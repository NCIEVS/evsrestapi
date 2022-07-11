package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import java.nio.charset.Charset;


public class ValueSetSearchUtils {
	private OWLSPARQLUtils owlSPARQLUtils = null;
	private String serviceUrl = null;
	private String named_graph = null;
	private String username = null;
	private String password = null;

	public static String EXACT_MATCH = "exactMatch";
	public static String STARTSWITH = "startsWith";
	public static String CONTAINS = "contains";

	public static String[] ALGORITHMS = new String[] {EXACT_MATCH, STARTSWITH, CONTAINS};

	public ValueSetSearchUtils(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.username = username;
		this.password = password;
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		this.owlSPARQLUtils.set_named_graph(named_graph);
	}

	public String construct_get_valueset_code_search(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            	?x a owl:Class .").append("\n");
		buf.append("            	?x :NHC0 ?x_code .").append("\n");
		buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            	?x rdfs:label ?x_label .").append("\n");
		buf.append("            	").append("\n");
		buf.append("            	?y a owl:Class .").append("\n");
		buf.append("            	?y :NHC0 ?y_code .").append("\n");
		buf.append("            	?y rdfs:label ?y_label .").append("\n");
		buf.append("            	").append("\n");
		buf.append("            	?x ?p1 ?y .").append("\n");
		buf.append("                ?p1 rdfs:label ?p1_label .").append("\n");
		buf.append("                ?p1 rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?y ?p2 \"Yes\"^^xsd:string .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("                ?p2 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n");
		buf.append("                ").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector searchByCode(String named_graph, String code) {
		String query = construct_get_valueset_code_search(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		HashSet hset = new HashSet();
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (!hset.contains(line)) {
				hset.add(line);
				w.add(line);
			}
		}
		return w;
	}

	public String construct_get_valueset_search(String named_graph, String term, String algorithm) {
		term = term.toLowerCase();
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?p1_label ?y_label ?y_code ?p_label ?p_value").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            	?x a owl:Class .").append("\n");
		buf.append("            	?x :NHC0 ?x_code .").append("\n");
		buf.append("            	?x rdfs:label ?x_label .").append("\n");
		buf.append("            	").append("\n");
		buf.append("            	?y a owl:Class .").append("\n");
		buf.append("            	?y :NHC0 ?y_code .").append("\n");
		buf.append("            	?y rdfs:label ?y_label .").append("\n");
		buf.append("            	").append("\n");
		buf.append("            	?x ?p1 ?y .").append("\n");
		buf.append("                ?p1 rdfs:label ?p1_label .").append("\n");
		buf.append("                ?p1 rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?y ?p2 \"Yes\"^^xsd:string .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("                ?p2 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n");
		buf.append("                ").append("\n");

		buf.append("                ?x ?p ?p_value .").append("\n");
		buf.append("                ?p rdfs:label ?p_label .").append("\n");
		/*
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a1_target .").append("\n");
		buf.append("                ?p rdfs:label ?p_label .    ").append("\n");
		buf.append("                ?p rdfs:label \"FULL_SYN\"^^xsd:string .").append("\n");
		buf.append("                ").append("\n");
		*/

		if (algorithm.compareTo(EXACT_MATCH) == 0) {
			buf.append("                FILTER(lcase(str(?p_value)) = \"" + term + "\"^^xsd:string)").append("\n");
		} else if (algorithm.compareTo(STARTSWITH) == 0) {
			buf.append("                FILTER(strStarts(lcase(str(?p_value)), \"" + term + "\"^^xsd:string))").append("\n");
		} else if (algorithm.compareTo(CONTAINS) == 0) {
			buf.append("                FILTER(contains(lcase(str(?p_value)), \"" + term + "\"^^xsd:string))").append("\n");
		}
		buf.append("                ").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector search(String named_graph, String term, String algorithm) {
		String query = construct_get_valueset_search(named_graph, term, algorithm);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		HashSet hset = new HashSet();
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (!hset.contains(line)) {
				hset.add(line);
				w.add(line);
			}
		}
		return w;
	}

	public String construct_get_value_set_metadata(String named_graph, String header_concept_code) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?y1_label ?z1 ?y2_label ?z2 ").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x ?y1 ?z1 .").append("\n");
		buf.append("?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("?y1 rdfs:label \"DEFINITION\"^^xsd:string .").append("\n");
		buf.append("?x ?y2 ?z2 .").append("\n");
		buf.append("?y2 rdfs:label \"Contributing_Source\"^^xsd:string .").append("\n");
		buf.append("?y2 rdfs:label ?y2_label .").append("\n");
		buf.append("FILTER (str(?x_code) = \"" + header_concept_code + "\"^^xsd:string)").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getValueSetMetadata(String named_graph, String header_concept_code) {
	    String query = construct_get_value_set_metadata(named_graph, header_concept_code);
	    Vector v = owlSPARQLUtils.executeQuery(query);
	    if (v == null) return null;
	    if (v.size() == 0) return v;
	    return new ParserUtils().getResponseValues(v);
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];

		String term = "blue";
		String code = "C48333";
		ValueSetSearchUtils utils = new ValueSetSearchUtils(serviceUrl, namedGraph, username, password);

		for (int i=0; i<ALGORITHMS.length; i++) {
			String algorithm = ALGORITHMS[i];
			Vector w = utils.search(namedGraph, term, algorithm);
			Utils.dumpVector(algorithm + " name " + term, w);
		}

		Vector w = utils.searchByCode(namedGraph, code);
        Utils.dumpVector("EXACT_MATCH code " + code, w);
	}

}

