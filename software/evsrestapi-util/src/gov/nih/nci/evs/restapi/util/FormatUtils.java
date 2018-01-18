package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

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

public class FormatUtils {
    ParserUtils parserUtils = null;

	public FormatUtils() {
        parserUtils = new ParserUtils();
	}

	public Vector toDelimited(Vector v) {
		return parserUtils.getResponseValues(v);
	}

	public static PrintWriter systemOut2PrintWriter() {
		return new PrintWriter(System.out, true);
	}

	public static Vector formatRelationships(Vector v, String label, String code) {
		if (v == null || v.size() == 0) return new Vector();
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String role_name = (String) u.elementAt(0);
			String role_target = (String) u.elementAt(1);
			String role_target_code = (String) u.elementAt(2);
			w.add("[" + label + " (" + code + ")] --> (" + role_name + ") --> [" + role_target + " (" + role_target_code + ")]");
		}
		return w;
	}

	public static Vector formatInverseRelationships(Vector v, String label, String code) {
		if (v == null || v.size() == 0) return new Vector();
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String role_source = (String) u.elementAt(0);
			String role_source_code = (String) u.elementAt(1);
			String role_name = (String) u.elementAt(2);
			w.add("[" + role_source + " (" + role_source_code + ")] --> (" + role_name + ") --> [" + label + " (" + code + ")]");
		}
		return w;
	}

	public static Vector formatSuperclasses(Vector v, String label, String code) {
		if (v == null || v.size() == 0) return new Vector();
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String role_name = "subClassOf";
			String role_target = (String) u.elementAt(0);
			String role_target_code = (String) u.elementAt(1);
			w.add("[" + label + " (" + code + ")] --> (" + role_name + ") --> [" + role_target + " (" + role_target_code + ")]");
		}
		return w;
	}

	public static Vector formatSubclasses(Vector v, String label, String code) {
		if (v == null || v.size() == 0) return new Vector();
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String role_source = (String) u.elementAt(0);
			String role_source_code = (String) u.elementAt(1);
			String role_name = "subClassOf";
			w.add("[" + role_source + " (" + role_source_code + ")] --> (" + role_name + ") --> [" + label + " (" + code + ")]");
		}
		return w;
	}

	//formatSynonyms
	public static String formatSynonyms(Vector v) {
		StringBuffer buf = new StringBuffer();
		if (v == null || v.size() == 0) return "";
		List list = new ParserUtils().getSynonyms(v);
		Synonym first_syn = (Synonym) list.get(0);
		String label = first_syn.getLabel();
		String code = first_syn.getCode();
		buf.append(label + " (" + code + ")").append("\n");
		buf.append("\tTerm\tSource\tType\tSource Code\tSubsource Name").append("\n");
		for (int i=0; i<list.size(); i++) {
			Synonym syn = (Synonym) list.get(i);
			String termName = syn.getTermName();
			if (termName == null) termName = "";
			String termSource = syn.getTermSource();
			if (termSource == null) termSource = "";
			String termGroup = syn.getTermGroup();
			if (termGroup == null) termGroup = "";
			String sourceCode = syn.getSourceCode();
			if (sourceCode == null) sourceCode = "";
			String subSourceName = syn.getSubSourceName();
			if (subSourceName == null) subSourceName = "";
			buf.append("\t" + termName + "\t" + termSource + "\t" + termGroup + "\t" + sourceCode + "\t" + subSourceName).append("\n");
		}
		return buf.toString();
	}
}