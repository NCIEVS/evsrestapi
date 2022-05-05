package gov.nih.nci.evs.restapi.util;


import java.io.*;
import java.util.*;

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
 *     Initial implementation kim.ong@nih.gov
 *
 */

public class IndexUtils {
	String serviceUrl = null;
	String namedGraph = null;
	String username = null;
	String password = null;
	SPARQLSearchUtils searchUtils = null;

    public IndexUtils(String serviceUrl, String namedGraph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
        searchUtils = new SPARQLSearchUtils(serviceUrl, namedGraph, username, password);
	}

    public Vector toSentences(String narrative) {
		Vector w = new Vector();
		Pattern re = Pattern.compile("[^.!?\\s][^.!?]*(?:[.!?](?!['\"]?\\s|$)[^.!?]*)*[.!?]?['\"]?(?=\\s|$)", Pattern.MULTILINE | Pattern.COMMENTS);
		Matcher reMatcher = re.matcher(narrative);
		while (reMatcher.find()) {
			w.add(reMatcher.group());
		}
		return w;
	}

	public Vector indexNarrative(String narrative) {
		Vector w = toSentences(narrative);
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String sentence = (String) w.elementAt(i);
			Vector segments = toSegments(sentence);
			for (int j=0; j<segments.size(); j++) {
				String segment = (String) segments.elementAt(j);
				Vector u = searchUtils.indexTerm(segment);
				if (u != null && u.size() >0) {
					v.addAll(u);
				}
			}
		}
		return v;
	}

	public Vector toSegments(String sentence) {
		System.out.println(sentence);
		Vector v = new Vector();
		Vector w = SPARQLSearchUtils.term2Keywords(sentence);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<w.size(); i++) {
			String word = (String) w.elementAt(i);
			boolean isKeyword = searchUtils.isKeyword(word);
            boolean isFiller = searchUtils.isFiller(word);
            if (isKeyword && !isFiller) {
				buf.append(word).append(" ");
			} else if( !isKeyword) {
				String t = buf.toString();
				t = t.trim();
				v.add(t);
				buf = new StringBuffer();
			}
		}
		if (buf.length() > 0) {
			String t = buf.toString();
			t = t.trim();
			v.add(t);
		}
		return v;

	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String restURL = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
	    IndexUtils indexUtils = new IndexUtils(restURL, namedGraph, username, password);

		String narrative = "The most commonly reported side effects, which typically lasted several days, were pain at the injection site, tiredness, headache, muscle pain, chills, joint pain, swollen lymph nodes in the same arm as the injection, nausea and vomiting, and fever. Of note, more people experienced these side effects after the second dose than after the first dose, so it is important for vaccination providers and recipients to expect that there may be some side effects after either dose, but even more so after the second dose.";
		Vector w = indexUtils.indexNarrative(narrative);
		Utils.dumpVector(narrative, w);

    }

}


