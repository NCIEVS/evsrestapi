package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.appl.*;
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

public class SPARQLSearchUtils extends OWLSPARQLUtils {
	static String[] PRESENTATIONS = new String[] {"Preferred_Name", "Display_Name", "FULL_SYN", "Legacy_Concept_Name", "label"};
	static List PRESENTATION_LIST = Arrays.asList(PRESENTATIONS);

    static String EXACT_MATCH = "exactMatch";
    static String STARTS_WITH = "startsWith";
    static String ENDS_WITH = "endsWith";
    static String CONTAINS = "contains";
    static String PHRASE = "Phrase";
    static String FUZZY = "Fuzzy";
    static String BOOLEAN_AND = "AND";

    static String NAMES = "names";
    static String PROPERTIES = "properties";
    static String ASSOCIATIONS = "associations";

    static String[] ALGORITHMS = new String[] {EXACT_MATCH, STARTS_WITH, ENDS_WITH, CONTAINS, PHRASE, FUZZY, BOOLEAN_AND};
    static String[] TARGETS = new String[] {NAMES, PROPERTIES, ASSOCIATIONS};

	public SPARQLSearchUtils() {
		super();
	}

	public SPARQLSearchUtils(String serviceUrl) {
		super(serviceUrl);
    }

	public SPARQLSearchUtils(ServerInfo serverInfo) {
		super(serverInfo);
	}

	public SPARQLSearchUtils(String serviceUrl, String username, String password) {
		super(serviceUrl, username, password);
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
		boolean matchPropertyValue = true;
		return assignScores(matchedConcepts, searchString, matchPropertyValue);
	}

	public List assignScores(List matchedConcepts, String searchString, boolean matchPropertyValue) {
		LevenshteinDistance ld = new LevenshteinDistance(null);
		searchString = searchString.toLowerCase();
		for (int i=0; i<matchedConcepts.size(); i++) {
            MatchedConcept mc_i = (MatchedConcept) matchedConcepts.get(i);
            Integer score = null;
            if (matchPropertyValue) {
                score = ld.apply(searchString, mc_i.getPropertyValue().toLowerCase());
			} else {
                score = ld.apply(searchString, mc_i.getLabel().toLowerCase());
			}
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
		matchedConcepts = assignScores(matchedConcepts, searchString, false);
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
			String p_value = (String) u.elementAt(0);
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
			} else {
				boolean bool = match(algorithm, searchString, p_value);
				if (bool) {
					if (!v.contains(line)) {
						v.add(line);
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
		searchTerm = escapeLuceneSpecialCharacters(searchTerm);
		return "'" + searchTerm + "'";
	}

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

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public SearchResult search(String named_graph, String propertyName, String searchString, String searchTarget, String algorithm) {
		long ms = System.currentTimeMillis();
		long ms0 = ms;
		Vector v = null;
		try {
			if (searchTarget.compareTo(ASSOCIATIONS) == 0) {
				v = searchAssociationTargets(named_graph, propertyName, searchString, algorithm);
			} else {
				v = searchAnnotationProperties(named_graph, propertyName, searchString, algorithm);
			}
		} catch (Exception ex) {
			System.out.println("search method throws exception???");
		}
		ms = System.currentTimeMillis();
		if (v == null) return null;
		String x_label = null;
		String x_code = null;
		String p_label = null;
		String p_value = null;
		String vs_code = null;

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
                if (searchTarget.compareTo(ASSOCIATIONS) == 0) {
					vs_code = (String) u.elementAt(4);
					p_value = p_value + " (" + vs_code + ")";
				}

				MatchedConcept mc = new MatchedConcept(x_label, x_code, p_label, p_value);
				matchedConcepts.add(mc);
			}
		}
		ms = System.currentTimeMillis();
		matchedConcepts = sortMatchedConcepts(matchedConcepts, searchString);
		sr.setMatchedConcepts(matchedConcepts);
		return sr;
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  named_graph, propertyName, searchString, EXACT_MATCH);
	public String construct_search_annotation_properties(String named_graph, String propertyName, String searchString, String algorithm) {
		searchString = searchString.replaceAll("%20", " ");
		searchString = searchString.toLowerCase();
		searchString = createSearchString(searchString, algorithm);
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x rdfs:label ?x_label .").append("\n");
		buf.append("	    ?x :NHC0 ?x_code .").append("\n");
		buf.append("	    ?y a owl:AnnotationProperty .").append("\n");
		buf.append("	    ?x ?y ?z .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");
		if (propertyName != null) {
			buf.append("	    ?y rdfs:label " + "\"" + propertyName + "\"^^xsd:string .").append("\n");
		}
		buf.append("	    (?x_label) <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector searchAnnotationProperties(String named_graph, String propertyName, String searchString, String algorithm) {
		String query = construct_search_annotation_properties(named_graph, propertyName, searchString, algorithm);
		System.out.println(query);
		Vector v = null;
		try {
			v = executeQuery(query);
		} catch (Exception ex) {
			System.out.println("searchAnnotationProperties throws exception???");
		}

		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			String searchTarget = NAMES;
			if (!PRESENTATION_LIST.contains(propertyName)) {
					searchTarget = PROPERTIES;
			}
			v = postProcess(v, searchTarget, algorithm, searchString);
		}
		return v;
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String construct_search_association_targets(String named_graph, String associationName, String searchString, String algorithm) {
		searchString = searchString.replaceAll("%20", " ");
		searchString = searchString.toLowerCase();
		searchString = createSearchString(searchString, algorithm);
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?y_label ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x rdfs:label ?x_label .").append("\n");
		buf.append("	    ?x :NHC0 ?x_code .").append("\n");
		buf.append("	    ?y a owl:AnnotationProperty .").append("\n");
		buf.append("	    ?x ?y ?z .").append("\n");
		buf.append("	    ?z a owl:Class .").append("\n");
		buf.append("	    ?z rdfs:label ?z_label .").append("\n");
		buf.append("	    ?z :NHC0 ?z_code .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");
		buf.append("	    ?y rdfs:label " + "\"" + associationName + "\"^^xsd:string .").append("\n");
		buf.append("	    ?y :NHC0 ?y_code .").append("\n");
		buf.append("	    ?y rdfs:range ?y_range .").append("\n");
		buf.append("	    (?x_label) <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");
		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?y_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector searchAssociationTargets(String named_graph, String associationName, String searchString, String algorithm) {
		String query = construct_search_association_targets(named_graph, associationName, searchString, algorithm);
		System.out.println(query);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		if (v.size() > 0) {
			String searchTarget = ASSOCIATIONS;
			v = postProcess(v, searchTarget, algorithm, searchString);
	    }
		return v;
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();

		String serviceUrl = args[0];
		System.out.println(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		MetadataUtils test = new MetadataUtils(serviceUrl);
		String version = test.getLatestVersion(codingScheme);
		String named_graph = test.getNamedGraph(codingScheme);

		serviceUrl = serviceUrl + "?query=";

		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("codingScheme: " + codingScheme);
		System.out.println("version: " + version);
		System.out.println("named_graph: " + named_graph);

		//named_graph = "http://NCIt";

		SPARQLSearchUtils searchUtils = new SPARQLSearchUtils(serviceUrl, null, null);
		searchUtils.set_named_graph(named_graph);
/*
        String associationName = "Concept_In_Subset";
        String searchString = "Parietal Lobe";

        searchString = "Red";
        String searchTarget = ASSOCIATIONS;
        String algorithm = EXACT_MATCH;
        Vector v = searchUtils.searchAssociationTargets(named_graph, associationName, searchString, algorithm);
        if (v != null) {
			StringUtils.dumpVector("v", v);
		}

        //searchUtils.runSearch("Red");
        String propertyName = "FULL_SYN";
        v = searchUtils.searchAnnotationProperties(named_graph, propertyName, searchString, EXACT_MATCH);
        if (v != null) {
			StringUtils.dumpVector("v", v);
		}
*/
//        String queryfile = "get_valueset_metadata.txt";
//        Vector v = searchUtils.runTest(queryfile);
        gov.nih.nci.evs.restapi.bean.ValueSetDefinition vsd = searchUtils.createValueSetDefinition(named_graph, "C54453");
        System.out.println(vsd.toJson());

	}
}
