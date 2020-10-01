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
 * Copyright Copyright 2020 MSC.. This software was developed in conjunction
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
 *      "This product includes software developed by MSC and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "MSC" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or MSC
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      MSC, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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


public class TermSearchUtils {
	String restricted_value_set_code = null;
	OWLSPARQLUtils owlSPARQLUtils = null;
	AxiomUtils axiomUtils = null;

	String serviceUrl = null;
	String namedGraph = null;
	HashMap termMap = null;
	HashMap obsoleteConceptMap = null;
	Vector full_syn_vec = null;
	String full_syn_file = "FULL_SYN.txt";
	boolean exclude_retired = true;

	Vector restricted_codes = null;

	public TermSearchUtils(String serviceUrl, String namedGraph) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		this.axiomUtils = new AxiomUtils(serviceUrl, null, null);
	}

	public TermSearchUtils(String serviceUrl, String namedGraph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		this.axiomUtils = new AxiomUtils(serviceUrl, username, password);
	}

	public void set_restricted_value_set_code(String code) {
		this.restricted_value_set_code = code;
	}

    public List getSynonyms(String named_graph, String code) {
	    return axiomUtils.getSynonyms(named_graph, code, "FULL_SYN");
	}

/*
{
  "code": "C82358",
  "label": "Adecatumumab",
  "termName": "ADECATUMUMAB",
  "termGroup": "PT",
  "termSource": "FDA",
  "sourceCode": "000705ZASD",
  "subSourceName": "UNII"
}
*/
    public String findFirstMatchedConceptCode(String term) {
		String term_lc = term.toLowerCase();
		if (!termMap.containsKey(term_lc)) {
			return "";
		}
		Vector w = (Vector) termMap.get(term_lc);
		StringBuffer buf = new StringBuffer();
		HashSet hset = new HashSet();
		int knt = 0;
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			for (int j=0; j<u.size(); j++) {
				String name = (String) u.elementAt(0);
				String code = (String) u.elementAt(1);
				String key = name + "|" + code;
				if (!hset.contains(key)) {
					hset.add(key);
					return code;
				}
			}
		}
		return "";
	}

	public String getMatchedSynonymNames(String named_graph, String code, Synonym pattern) {
		StringBuffer buf = new StringBuffer();
		List list = getSynonyms(named_graph, code);
		for (int i=0; i<list.size(); i++) {
			Synonym syn = (Synonym) list.get(i);
            boolean matched = true;
			if (pattern.getTermGroup() != null) {
				if (syn.getTermGroup() == null || syn.getTermGroup().compareTo(pattern.getTermGroup()) != 0) {
					matched = false;
				}
			}
			if (pattern.getTermSource() != null) {
				if (syn.getTermSource() == null || syn.getTermSource().compareTo(pattern.getTermSource()) != 0) {
					matched = false;
				}
			}
			if (pattern.getSourceCode() != null) {
				if (syn.getSourceCode() == null || syn.getSourceCode().compareTo(pattern.getSourceCode()) != 0) {
					matched = false;
				}
			}
			if (pattern.getSubSourceName() != null) {
				if (syn.getSubSourceName() == null || syn.getSubSourceName().compareTo(pattern.getSubSourceName()) != 0) {
					matched = false;
				}
			}
			if (matched) {
				buf.append(syn.getTermName());
			}

		}
		String t = buf.toString();
        if (t.endsWith("$")) {
			t = t.substring(0, t.length()-1);
		}
		return t;
	}

	public String getFDAPT(String code) {
		return getFDAPT(namedGraph, code);
	}

	public String getFDAPT(String named_graph, String code) {
		List list = getSynonyms(named_graph, code);
		for (int i=0; i<list.size(); i++) {
			Synonym syn = (Synonym) list.get(i);
			if ((syn.getTermGroup() != null &&  syn.getTermGroup().equals("PT")) &&
			    (syn.getTermSource() != null && syn.getTermSource().equals("FDA")) &&
			    (syn.getSubSourceName() != null && syn.getSubSourceName().equals("UNII"))) {
				return syn.getTermName();
			}
		}
        return null;
	}

	public Vector get_concepts_in_subset(String code) {
		return owlSPARQLUtils.get_concepts_in_subset(this.namedGraph, code);
	}

	public Vector getMemberConceptsCodes(String code) {
        Vector w = owlSPARQLUtils.get_concepts_in_subset(this.namedGraph, code);
        Vector v = new Vector();
        for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String s = (String) u.elementAt(1);
			v.add(s);
		}
		return v;
	}

	public void set_exclude_retired(boolean exclude_retired) {
		this.exclude_retired = exclude_retired;
	}

	public Vector retrievePropertyData(String propertyName) {
		Vector v = owlSPARQLUtils.get_property_query(this.namedGraph, propertyName);
        return v;
	}

    public Vector get_full_syn_vec() {
		if (full_syn_vec == null) {
			full_syn_vec = retrievePropertyData("FULL_SYN");
		}
		return full_syn_vec;
	}


	public void initialize() {
		obsoleteConceptMap = createObsoleteConceptMap();
		if (restricted_value_set_code != null) {
			restricted_codes = getMemberConceptsCodes(restricted_value_set_code);
		}
		//restricted_codes = getMemberConceptsCodes(FDA_Established_Names_and_Unique_Ingredient_Identifier_Codes_Terminology_Code);
		Vector w = retrievePropertyData("FULL_SYN");
		Vector full_syn_vec = new Vector();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String code = (String) u.elementAt(1);
			if (restricted_codes != null && restricted_codes.contains(code)) {
				full_syn_vec.add(t);
			}
		}
		termMap = createTermMap(full_syn_vec);
	}

	public Vector get_property_query(String propertyName) {
		return owlSPARQLUtils.get_property_query(this.namedGraph, propertyName);
	}

	public HashMap createTermMap(Vector v) {
		HashMap termMap = new HashMap();
/*
Fluorodopa F 18|C95766|FULL_SYN|FLUORODOPA F-18
Fluorodopa F 18|C95766|FULL_SYN|L-6-(18F)Fluoro-DOPA
*/
        //Vector v = Utils.readFile(filename);
		for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, "|");
			for (int j=0; j<u.size(); j++) {
				String label = (String) u.elementAt(0);
				String code = (String) u.elementAt(1);
				if (!exclude_retired) { // include every term
					String term = (String) u.elementAt(3);
					String term_lc = term.toLowerCase();
					Vector w = new Vector();
					if (termMap.containsKey(term_lc)) {
						w = (Vector) termMap.get(term_lc);
					}
					if (!w.contains(line)) {
						w.add(line);
					}
					termMap.put(term_lc, w);
				} else if (!obsoleteConceptMap.containsKey(code)) { // only include active terms
					String term = (String) u.elementAt(3);
					String term_lc = term.toLowerCase();
					Vector w = new Vector();
					if (termMap.containsKey(term_lc)) {
						w = (Vector) termMap.get(term_lc);
					}
					if (!w.contains(line)) {
						w.add(line);
					}
					termMap.put(term_lc, w);
				}
			}
		}
		return termMap;
	}


    public String removeSuffix(String str) {
		if (str.endsWith("[USAN]")) {
			int n = str.lastIndexOf("[USAN]");
			str = str.substring(0, n-1);
		}

		if (str.endsWith("[INN]")) {
			int n = str.lastIndexOf("[INN]");
			str = str.substring(0, n-1);
		}
		str = str.trim();
		return str;
	}

	public void match(String datafile) {
		Vector output_vec = new Vector();
        Vector v = Utils.readFile(datafile);
		for (int i=0; i<v.size(); i++) {
			int j = i+1;
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, "\t");
			String term0 = (String) u.elementAt(0);

			Vector tmp = new Vector();
			tmp.add("(" + j + ") " + term0);
			output_vec.addAll(tmp);

			String term = removeSuffix(term0);
			String term_lc = term.toLowerCase();
			if (termMap.containsKey(term_lc)) {
				Vector w = (Vector) termMap.get(term_lc);
				output_vec.addAll(w);
                Utils.dumpVector("(" + j + ") " + term0 , w);
			} else {
				System.out.println("(" + j + ") " + term0);
				System.out.println("\tNo match");
				tmp = new Vector();
				tmp.add("\tNo match");
				output_vec.addAll(tmp);
			}
		}
		int n = datafile.lastIndexOf(datafile);
		String outputfile = datafile.substring(0, n) + "_" + StringUtils.getToday() + ".txt";
		Utils.saveToFile(outputfile, output_vec);
	}

    public String findMatchedConcepts(String term) {
		String term_lc = term.toLowerCase();
		if (!termMap.containsKey(term_lc)) {
			return "";
		}
		Vector w = (Vector) termMap.get(term_lc);
		StringBuffer buf = new StringBuffer();
		HashSet hset = new HashSet();
		int knt = 0;
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			for (int j=0; j<u.size(); j++) {
				String name = (String) u.elementAt(0);
				String code = (String) u.elementAt(1);
				String key = name + "|" + code;
				if (!hset.contains(key)) {
					hset.add(key);
					buf.append(name + " (" + code + ")").append(";");
					knt++;
				}
			}
		}
		String t = buf.toString();
		while (t.endsWith(";")) {
			t = t.substring(0, t.length()-1);
		}
		return t;
	}


    public String findMatchedConceptCodes(String term) {
		String term_lc = term.toLowerCase();
		if (!termMap.containsKey(term_lc)) {
			return "";
		}
		Vector w = (Vector) termMap.get(term_lc);
		StringBuffer buf = new StringBuffer();
		HashSet hset = new HashSet();
		int knt = 0;
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			for (int j=0; j<u.size(); j++) {
				String name = (String) u.elementAt(0);
				String code = (String) u.elementAt(1);
				String key = name + "|" + code;
				if (!hset.contains(key)) {
					hset.add(key);
					buf.append(code).append("|");
					knt++;
				}
			}
		}
		String t = buf.toString();
		while (t.endsWith("|")) {
			t = t.substring(0, t.length()-1);
		}
		return t;
	}



	public Vector retrieveRetiredConceptData() {
		String property_name = "Concept_Status";
		String property_value = "Obsolete_Concept";
		Vector v = owlSPARQLUtils.findConceptsWithPropertyMatching(this.namedGraph, property_name, property_value);
		property_value = "Retired_Concept";
		Vector v2 = owlSPARQLUtils.findConceptsWithPropertyMatching(this.namedGraph, property_name, property_value);
		v.addAll(v2);
        return v;
	}


	public boolean isRetired(String code) {
		if (obsoleteConceptMap == null) {
			obsoleteConceptMap = createObsoleteConceptMap();
		}
		if (obsoleteConceptMap.containsKey(code)) {
			return true;
		}
		return false;
	}


	public HashMap createObsoleteConceptMap() {
		HashMap hmap = new HashMap();
		Vector w = retrieveRetiredConceptData();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			for (int j=0; j<u.size(); j++) {
				String name = (String) u.elementAt(0);
				String code = (String) u.elementAt(1);
				hmap.put(code, name);
			}
		}
		return hmap;
	}

	public Vector findConceptsWithPropertyMatching(String property_name, String property_value) {
		return owlSPARQLUtils.findConceptsWithPropertyMatching(this.namedGraph, property_name, property_value);
	}
}