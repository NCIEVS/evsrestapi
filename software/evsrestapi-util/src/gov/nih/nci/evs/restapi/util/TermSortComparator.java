package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;
import java.io.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.codec.binary.Base64;
import org.json.*;

import org.apache.commons.text.similarity.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2017 NGIS. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by NGIS and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIS" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIS
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIS, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@ngc.com
 *
 */


public class TermSortComparator implements Comparator<Object> {
	LevenshteinDistance ld = null;
	String matchText = null;

    public TermSortComparator(String matchText) {
		ld = new LevenshteinDistance(null);
		this.matchText = matchText.toLowerCase();
    }

    private String getKey(Object c) {
        if (c instanceof MatchedConcept) {
            MatchedConcept mc = (MatchedConcept) c;
            String value = mc.getPropertyValue();
			return value;
		}
        return "NULL";
    }

    private int getScore(Object c) {
        if (c instanceof MatchedConcept) {
            MatchedConcept mc = (MatchedConcept) c;
            int value = mc.getScore();
			return value;
		}
        return -1;
    }

    private String getSecondaryKey(Object c) {
        if (c instanceof MatchedConcept) {
            MatchedConcept mc = (MatchedConcept) c;
            String type = mc.getTermType();
 			return type;
 		}
        return "NULL";
    }

    public int compare(Object object1, Object object2) {
        // case insensitive sort
        /*
        String key1 = getKey(object1);
        String key2 = getKey(object2);

        if (key1 == null || key2 == null)
            return 0;

        key1 = getKey(object1).toLowerCase();
        key2 = getKey(object2).toLowerCase();
        */

        int score1 = getScore(object1);
        int score2 = getScore(object2);

        if (score1 < score2) return -1;
        if (score1 > score2) return 1;
        return break_tie(object1, object2);
    }

    public int break_tie(Object object1, Object object2) {
        // case insensitive sort
        String key1 = getSecondaryKey(object1);
        String key2 = getSecondaryKey(object2);

        if (key1 == null || key2 == null)
            return 0;

        int score1 = 0;
        int score2 = 0;

        if (key1.compareTo("PT") == 0) {
			score1 = 3;
		} else if (key1.compareTo("SY") == 0) {
			score1 = 2;
		} else {
			score1 = 1;
		}
        if (key2.compareTo("PT") == 0) {
			score2 = 3;
		} else if (key2.compareTo("SY") == 0) {
			score2 = 2;
		} else {
			score2 = 1;
		}
        if (score1 < score2) return 1;
        if (score1 > score2) return -1;

        key1 = getKey(object1).toLowerCase();
        key2 = getKey(object2).toLowerCase();
        return key1.compareTo(key2);
    }
}
