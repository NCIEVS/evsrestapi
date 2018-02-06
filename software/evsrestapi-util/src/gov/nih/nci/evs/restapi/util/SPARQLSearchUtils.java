package gov.nih.nci.evs.restapi.util;

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


public class SPARQLSearchUtils {
	static String[] PRESENTATIONS = new String[] {"Preferred_Name", "Display_Name", "FULL_SYN", "Legacy_Concept_Name", "label"};
	static List PRESENTATION_LIST = Arrays.asList(PRESENTATIONS);

    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;

    static String EXACT_MATCH = "exactMatch";
    static String STARTS_WITH = "startsWith";
    static String ENDS_WITH = "endsWith";
    static String CONTAINS = "contains";
    static String PHRASE = "Phrase";
    static String FUZZY = "Fuzzy";
    static String BOOLEAN_AND = "AND";

    static String NAMES = "names";
    static String PROPERTIES = "properties";

    static String[] ALGORITHMS = new String[] {EXACT_MATCH, STARTS_WITH, ENDS_WITH, CONTAINS, PHRASE, FUZZY, BOOLEAN_AND};
    static String[] TARGETS = new String[] {NAMES, PROPERTIES};



	public SPARQLSearchUtils() {
		this.httpUtils = new HTTPUtils();
        this.jsonUtils = new JSONUtils();
	}

	public SPARQLSearchUtils(String serviceUrl) {
		this.serviceUrl = serviceUrl;
		this.httpUtils = new HTTPUtils(serviceUrl, null, null);
        this.jsonUtils = new JSONUtils();
    }

	public SPARQLSearchUtils(ServerInfo serverInfo) {
		this.serviceUrl = serverInfo.getServiceUrl();
		this.httpUtils = new HTTPUtils(serviceUrl, serverInfo.getUsername(), serverInfo.getPassword());
        this.jsonUtils = new JSONUtils();
	}


	public SPARQLSearchUtils(String serviceUrl, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.httpUtils = new HTTPUtils(serviceUrl, username, password);
        this.jsonUtils = new JSONUtils();
    }

    public String getServiceUrl() {
		return this.serviceUrl;
	}

    public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
	}

	public void set_prefixes(String prefixes) {
		this.prefixes = prefixes;
	}

    public String getPrefixes() {
		if (prefixes != null) return prefixes;
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX Thesaurus:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX ncicp:<http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		return buf.toString();
	}

    public Vector executeQuery(String query) {
        Vector v = null;
        try {
			query = httpUtils.encode(query);
            String json = httpUtils.executeQuery(query);
			json = httpUtils.executeQuery(query);
			v = new JSONUtils().parseJSON(json);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}


    public String getMatchFilter(String var, String algorithm, String searchString) {
		if (algorithm.compareTo(EXACT_MATCH) == 0) {
			return "    FILTER (lcase(?" + var + ") = \"" + searchString + "\")";
		} else if (algorithm.compareTo(STARTS_WITH) == 0) {
			return "    FILTER (STRSTARTS(lcase(?" + var + "), \"" + searchString + "\"))";
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			return "    FILTER (STRENDS(lcase(?" + var + "), \"" + searchString + "\"))";
		} else if (algorithm.compareTo(CONTAINS) == 0) {
			return "    FILTER (CONTAINS(lcase(?" + var + "), \"" + searchString + "\"))";
		}
		return "";
	}

	public List assignScores(List matchedConcepts, String searchString) {
		LevenshteinDistance ld = new LevenshteinDistance(null);
		searchString = searchString.toLowerCase();
		for (int i=0; i<matchedConcepts.size(); i++) {
            MatchedConcept mc_i = (MatchedConcept) matchedConcepts.get(i);
            Integer score = ld.apply(searchString, mc_i.getPropertyValue().toLowerCase());
            mc_i.setScore(Integer.valueOf(score));
		}
		return matchedConcepts;
	}

    public String getKey(int score) {
		String base = "000000";
		String score_str = new Integer(score).toString();
		int len = score_str.length();
		return base.substring(0, base.length()-len) + score_str;
	}



	public List sortMatchedConcepts(List matchedConcepts, String searchString) {
		matchedConcepts = assignScores(matchedConcepts, searchString);
		if (matchedConcepts == null) return null;
		if (matchedConcepts.size() <= 1) return matchedConcepts;
		Vector w = new Vector();
		for (int i=1; i<matchedConcepts.size(); i++) {
			MatchedConcept mc = (MatchedConcept) matchedConcepts.get(i);
			String key = "" + getKey(mc.getScore())  + "|" + mc.getScore()  + "|" + mc.getLabel() + "|" + mc.getCode() + "|" + mc.getPropertyName()
			   + "|" + mc.getPropertyValue();
			w.add(key);
		}

		matchedConcepts = new ArrayList();
		w = new SortUtils().quickSort(w);
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String score_str = (String) u.elementAt(1);
			int score = Integer.parseInt(score_str);
			String x_label = (String) u.elementAt(2);
			String x_code = (String) u.elementAt(3);
			String p_label = (String) u.elementAt(4);
			String p_value = (String) u.elementAt(5);
			MatchedConcept mc = new MatchedConcept(x_label, x_code, p_label, p_value, score);
			matchedConcepts.add(mc);
		}
		return matchedConcepts;

	}

	public void dumpMatchedConcept(MatchedConcept mc) {
		StringBuffer buf = new StringBuffer();
		 buf.append("\n" + mc.getLabel() + "\n\t" + mc.getCode()
		 + "\n\t" + mc.getPropertyName()
		 + "\n\t" + mc.getPropertyValue()
		 + "\n\t" + mc.getScore()
		 );
	    System.out.println(buf.toString());
	}

	public SearchResult search(String named_graph, String searchString, String searchTarget, String algorithm) {
		long ms = System.currentTimeMillis();
		long ms0 = ms;
		Vector v = searchAnnotationProperties(named_graph, searchString, searchTarget, algorithm);
		ms = System.currentTimeMillis();
		if (v == null) return null;
		String x_label = null;
		String x_code = null;
		String p_label = null;
		String p_value = null;

		SearchResult sr = new SearchResult();
		List matchedConcepts = new ArrayList();
		Vector w = new Vector();
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			line = line.trim();
			if (!hset.contains(line)) {
				Vector u = StringUtils.parseData(line, '|');
				x_label = (String) u.elementAt(0);
				x_code = (String) u.elementAt(1);
				p_label = (String) u.elementAt(2);
				p_value = (String) u.elementAt(3);
				MatchedConcept mc = new MatchedConcept(x_label, x_code, p_label, p_value);
				matchedConcepts.add(mc);
			}
		}
		ms = System.currentTimeMillis();
		matchedConcepts = sortMatchedConcepts(matchedConcepts, searchString);
		//System.out.println("Total sort matchedConcepts run time (ms): " + (System.currentTimeMillis() - ms));
		sr.setMatchedConcepts(matchedConcepts);
		//System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms0));
		return sr;
	}


    public Vector tokenize(String str) {
		Vector v = new Vector();
		StringTokenizer st = new StringTokenizer(str);
		while (st.hasMoreTokens()) {
		    v.add(st.nextToken());
		}
		return v;
	}

	public String createSearchString(String str) {
		str = str.toLowerCase();
		Vector v = tokenize(str);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String token = (String) v.elementAt(i);
            buf.append(token);
			if (i < v.size()-1) {
				buf.append(" AND ");
			}
		}
		return "'" + buf.toString() + "'" ;
	}

	public String construct_search_annotation_properties(String named_graph, String searchString) {
		String prefixes = getPrefixes();
		searchString = searchString.replaceAll("%20", " ");
		searchString = searchString.toLowerCase();
		Vector u = tokenize(searchString);
		searchString = createSearchString(searchString);
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		//buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_value ?score").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_value").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?p a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?p ?p_value .").append("\n");
		buf.append("            ?p rdfs:label ?p_label . ").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
       	//buf.append("            (?p_value ?score) <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");
       	buf.append("            (?p_value) <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

    public Vector searchAnnotationProperties(String named_graph, String searchString, String searchTarget, String algorithm) {
		String query = construct_search_annotation_properties(named_graph, searchString);
		Vector v = executeQuery(query);
		if (v.size() > 0) {
			v = new ParserUtils().parse(v);
			v = postProcess(v, searchTarget, algorithm, searchString);
	    }
	    return v;
	}


	public boolean match(String algorithm, String searchString, String p_value) {
		if (algorithm.compareTo(EXACT_MATCH) == 0) {
			if (p_value.compareToIgnoreCase(searchString) == 0) {
				return true;
			}
		} else if (algorithm.compareTo(STARTS_WITH) == 0) {
			if (p_value.startsWith(searchString)) {
				return true;
			}
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			if (p_value.endsWith(searchString)) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}


	public Vector postProcess(Vector w, String searchTarget, String algorithm, String searchString) {
		searchString = searchString.toLowerCase();
        searchString = searchString.trim();
		Vector v = new Vector();

		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String p_label = (String) u.elementAt(2);
			String p_value = (String) u.elementAt(3);
			p_value = p_value.toLowerCase();

			if (searchTarget.compareTo(NAMES) == 0) {
				if (PRESENTATION_LIST.contains(p_label)) {
					if (match(algorithm, searchString, p_value)) {
					    if (!v.contains(line)) {
							v.add(line);
						}
					}
				}
			} else if (searchTarget.compareTo(PROPERTIES) == 0) {
				if (!PRESENTATION_LIST.contains(p_label)) {
					if (match(algorithm, searchString, p_value)) {
					    if (!v.contains(line)) {
	                        v.add(line);
						}
					}
				}
			}
		}
		return v;
	}

	public static String marshalSearchResult(SearchResult sr) throws Exception {
		XStream xstream_xml = new XStream(new DomDriver());
        String xml = Constants.XML_DECLARATION + "\n" + xstream_xml.toXML(sr);
        return xml;
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
		return searchTerm;
	}


	public static void main(String[] args) {
		String serviceUrl = args[0];
		String named_graph = args[1];
		long ms = System.currentTimeMillis();
		SPARQLSearchUtils searchUtils = new SPARQLSearchUtils(serviceUrl, null, null);
	    String searchString = "cell aging";
        Vector results = new Vector();
        int k = 0;

	    for (int i=0; i<TARGETS.length; i++) {
			String searchTarget = TARGETS[i];
			for (int j=0; j<ALGORITHMS.length; j++) {
				k++;
				String algorithm = ALGORITHMS[j];
	            SearchResult sr = searchUtils.search(named_graph, searchString, searchTarget, algorithm);
                results.add("\n(" + k + ") searchString: " + searchString + ", target: " + searchTarget + ", algorithm: " + algorithm);
                results.add(sr.toJson());
			}
		}
        Utils.saveToFile("test_resutls.txt", results);
	}
}

/*
wildcards: ? and *
fuzzy: ~ and ~ with similarity weights (e.g. foo~0.8)
proximities: "semantic web"~5
term boosting
booleans: OR, AND, NOT, +, and `-.
grouping
*/