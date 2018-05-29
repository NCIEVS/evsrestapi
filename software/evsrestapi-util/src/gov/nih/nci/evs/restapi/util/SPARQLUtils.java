package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.appl.*;
import gov.nih.nci.evs.restapi.bean.*;

import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;
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

public class SPARQLUtils {

	private static String CONTAINS = "contains";
	private static String EXACT_MATCH = "exactMatch";
	private static String STARTS_WITH = "startsWith";
	private static String ENDS_WITH = "startsWith";
    public static String PHRASE = "Phrase";
    public static String FUZZY = "Fuzzy";
    public static String BOOLEAN_AND = "AND";

    private static Vector supportedAssociations = null;

	public static String escapeLuceneSpecialCharacters(String before) {
		String patternString = "([+:!~*?/\\-/{}\\[\\]\\(\\)\\^\\\"])";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(before);
		StringBuffer buf = new StringBuffer();
		while(matcher.find()) {
			matcher.appendReplacement(buf,
			before.substring(matcher.start(),matcher.start(1)) +
			"\\\\" + "\\\\" + matcher.group(1)
			+ before.substring(matcher.end(1),matcher.end()));
		}
		String after = matcher.appendTail(buf).toString();
		return after;
	}

	public static String createSearchString(String term, String algorithm) {
		if (term == null) return null;
		String searchTerm = term;
        if (algorithm.equalsIgnoreCase(CONTAINS)){
			searchTerm = "*" + term + "*";
		} else if (algorithm.equalsIgnoreCase(EXACT_MATCH)){
			searchTerm =term;
		} else if (algorithm.equalsIgnoreCase(STARTS_WITH)){
			searchTerm = term + "*";
		} else if (algorithm.equalsIgnoreCase(ENDS_WITH)){
			searchTerm = "*" + term;
		} else if (algorithm.equalsIgnoreCase(PHRASE)){
			searchTerm = "\"" + term + "\"";
		} else if (algorithm.equalsIgnoreCase(FUZZY)){
			searchTerm = term + "~";
		} else if (algorithm.equalsIgnoreCase(BOOLEAN_AND)){
			String[] terms = searchTerm.split(" ");
			List list = Arrays.asList(terms);
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<list.size(); i++) {
				String token = (String) list.get(i);
				buf = buf.append(token);
				if (i < list.size()-1) {
					buf.append(" AND ");
			    }
			}
			searchTerm = buf.toString();
		}
		searchTerm = escapeLuceneSpecialCharacters(searchTerm);
		return "'" + searchTerm + "'";
	}


	public static String apply_text_match(String var, String matchText, String algorithm) {
		matchText = matchText.toLowerCase();
		if (algorithm.compareTo(CONTAINS) == 0) {
			return "FILTER contains(lcase(str(?" + var + ")), \"" + matchText + "\")";
		} else if (algorithm.compareTo(EXACT_MATCH) == 0) {
			return "FILTER (lcase(str(?" + var + ")) = \"" + matchText + "\")";
		} else if (algorithm.compareTo(STARTS_WITH) == 0) {
			return "FILTER regex(?" + var + ", '^" + matchText + "', 'i')";
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			return "FILTER regex(?" + var + ", '" + matchText + "^', 'i')";
		} else {
			matchText = matchText.replaceAll("%20", " ");
			matchText = createSearchString(matchText, algorithm);
			return"(?"+ var +") <tag:stardog:api:property:textMatch> (" + matchText + ").";
		}
	}

	public static String construct_filter_stmt(String var, Vector properties) {
		return construct_filter_stmt(var, properties, false);
	}

	public static String construct_filter_stmt(String var, Vector properties, boolean negation) {
		StringBuffer buf = new StringBuffer();
		buf.append("FILTER (");
		if (negation) {
			buf.append("!(");
		}
		for (int i=0; i<properties.size(); i++) {
			String property = (String) properties.elementAt(i);
			buf.append("(str(?" + var + ")=\"" + property + "\"^^xsd:string)");
			if (i < properties.size()-1) {
				buf.append(" || ");
			}
		}
		if (negation) {
			buf.append(")");
		}
		buf.append(")");
		return buf.toString();
    }

    public static List<MatchedConcept> toMatchedConceptList(Vector v) {
        if (v == null) return null;
        List<MatchedConcept> list = new ArrayList<MatchedConcept>();
        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			String propertyName = (String) u.elementAt(2);
			String propertyValue = (String) u.elementAt(3);
			MatchedConcept mc = new MatchedConcept(label, code, propertyName, propertyValue, 0);
			list.add(mc);
		}
        return list;
	}


}