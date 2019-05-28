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

import java.util.StringTokenizer;

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


public class GeneralizedQueryUtils {
	OWLSPARQLUtils owlSPARQLUtils = null;
	String serviceUrl = null;
	String named_graph = null;
	String ncit_version = null;
	String sparql_endpoint = null;
	HashMap nameVersion2NamedGraphMap = null;
	gov.nih.nci.evs.restapi.util.MetadataUtils mdu = null;
	HashMap uriBaseHashMap = null;
	HashMap nameGraph2PredicateHashMap = null;
	Vector supportedNamedGraphs = null;

	SPARQLSearchUtils sparqlSearchUtils = null;
	//ConceptDetailsPageGenerator cdpg = null;

    static int FORWARD = 1;
    static int BACKWARD = 2;

    HierarchyHelper hh = null;

    HashMap namedGraph2UIDPredicateHashMap = null;
    HashMap basePrefixUIDHashMap = null;
    static String PARENT_CHILD_FILE = "parent_child.txt";

    static String[] COMMON_PREFIXES = new String[] {
		"PREFIX xml:<http://www.w3.org/XML/1998/namespace>",
		"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
		"PREFIX owl:<http://www.w3.org/2002/07/owl#>",
		"PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>",
		"PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>",
		"PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>",
        "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>"
	};

	public GeneralizedQueryUtils(String serviceUrl, HashMap nameVersion2NamedGraphMap, HashMap basePrefixUIDHashMap) {
		this.serviceUrl = serviceUrl;
		this.nameVersion2NamedGraphMap = nameVersion2NamedGraphMap;
		this.basePrefixUIDHashMap = basePrefixUIDHashMap;
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
	}

	public String getServiceUrl() {
		return this.serviceUrl;
	}

	public HashMap getNameVersion2NamedGraphMap() {
		return this.nameVersion2NamedGraphMap;
	}

	public HashMap getBasePrefixUIDHashMap() {
		return this.basePrefixUIDHashMap;
	}

	public gov.nih.nci.evs.restapi.util.MetadataUtils getMetadataUrils() {
		return this.mdu;
	}

	public GeneralizedQueryUtils(String serviceUrl) {
		long ms = System.currentTimeMillis();

		int n = serviceUrl.indexOf("?");
		if (n != -1) {
			serviceUrl = serviceUrl.substring(0, n);
		}
		this.serviceUrl = serviceUrl;
		this.sparql_endpoint = serviceUrl + "?query=";

		mdu = new gov.nih.nci.evs.restapi.util.MetadataUtils(serviceUrl);
		nameVersion2NamedGraphMap = mdu.getNameVersion2NamedGraphMap();
		uriBaseHashMap = mdu.getURIBaseHashMap();

		this.supportedNamedGraphs = mdu.getSupportedNamedGraphs();

		this.named_graph = mdu.getNamedGraph(gov.nih.nci.evs.restapi.common.Constants.NCI_THESAURUS);
		this.ncit_version = mdu.getLatestVersion(gov.nih.nci.evs.restapi.common.Constants.NCI_THESAURUS);

		System.out.println("sparql_endpoint: " + sparql_endpoint);
		System.out.println("named_graph: " + named_graph);
		System.out.println("ncit_version: " + ncit_version);

		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		this.sparqlSearchUtils = new SPARQLSearchUtils(serviceUrl);

        if (FileUtils.fileExists(PARENT_CHILD_FILE)) {
			Vector parent_child_vec = Utils.readFile(PARENT_CHILD_FILE);
			hh = new HierarchyHelper(parent_child_vec);
		}
		namedGraph2UIDPredicateHashMap = createNamedGraph2UIDPredicateHashMap();
		basePrefixUIDHashMap = createBasePrefixUIDHashMap();
		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public MetadataUtils getMetadataUtils() {
		return this.mdu;
	}


    public HashMap createNamedGraph2UIDPredicateHashMap() {
		HashMap namedGraph2UIDPredicateHashMap = new HashMap();;
        HashMap hmap = getNameVersion2NamedGraphMap();
        Iterator it = hmap.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			Vector ng_vec = (Vector) hmap.get(key);
			for (int j=0; j<ng_vec.size(); j++) {
				String ng = (String) ng_vec.elementAt(j);
				String p = getNamedGraphUIDPredicate(ng);
				namedGraph2UIDPredicateHashMap.put(ng, p);
			}
		}
		return namedGraph2UIDPredicateHashMap;
	}

	public String get_named_graph_predicate(String named_graph) {
		return (String) namedGraph2UIDPredicateHashMap.get(named_graph);
	}

	public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
	}

	public Vector getAnnotationProperties(String named_graph) {
		return this.owlSPARQLUtils.getAnnotationProperties(named_graph);
	}

	public Vector getSupportedNamedGraphs() {
		return this.supportedNamedGraphs;
	}

	public static Vector formatOutput(Vector v) {
		return ParserUtils.formatOutput(v);
	}

    public static String trimLeadingBlanksOrTabs(String line) {
		return StringUtils.trimLeadingBlanksOrTabs(line);
	}

	public static String escapeDoubleQuotes(String line) {
		return StringUtils.escapeDoubleQuotes(line);
	}

    public static void generate_construct_statement(String method_name, String params, String filename) {
		Utils.generate_construct_statement(method_name, params, filename);
	}

	public String getURIBase(String named_graph) {
		return mdu.getURIBase(named_graph);
	}

    public void runQuery(String query_file) {
		int n = query_file.indexOf(".");
		String method_name = query_file.substring(0, n);
		String params = "String named_graph, String code";

		String query = owlSPARQLUtils.getQuery(query_file);

	    Utils.generate_construct_statement(method_name, params, query_file);

		//String json = owlSPARQLUtils.getJSONResponseString(query);
		//System.out.println(json);
        Vector v = owlSPARQLUtils.execute(query_file);
        v = formatOutput(v);
        StringUtils.dumpVector("v", v);
	}

	public OWLSPARQLUtils getOWLSPARQLUtils() {
		return owlSPARQLUtils;
	}

	public Vector getSupportedProperties() {
        return owlSPARQLUtils.getSupportedProperties(named_graph);
	}

	public Vector getSupportedAssociations() {
        return owlSPARQLUtils.getSupportedAssociations(named_graph);
	}


	public String construct_get_owl_class_data(String named_graph, String identifier, String code, String ns, boolean by_code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z ").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("?x " + identifier + " ?x_code .").append("\n");
		if (by_code) {
			buf.append("?x " + identifier + " \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		}
		buf.append("}").append("\n");
		if (!by_code) {
			buf.append("FILTER (str(?x) = \"" + ns + code + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getOWLClassData(String named_graph, String identifier, String code, String ns, boolean by_code) {
		String q = owlSPARQLUtils.construct_get_owl_class_data(named_graph, identifier, code, ns, by_code);
        //System.out.println(q);
		Vector v = owlSPARQLUtils.getOWLClassData(named_graph, identifier, code, ns, by_code);
		return v;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String construct_simple_tuple_query(String named_graph, int limit) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT distinct ?x ?p ?y").append("\n");
		buf.append("{ ").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("{").append("\n");
		//buf.append("?x a owl:Class .").append("\n");
		buf.append("?x ?p ?y ").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("} ").append("\n");
		buf.append("").append("\n");
		buf.append("LIMIT " + limit).append("\n");
		return buf.toString();
	}

	public Vector simple_tuple_query(String named_graph, int limit) {
		String query = construct_simple_tuple_query(named_graph, limit);

//System.out.println(query);


		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}


	public String findNamedGraphIdentifierLine(Vector v) {
		if (v == null) {
			return null;
		}
		String id_line = null;
		int n0 = 0;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector w = StringUtils.parseData(line, '|');
			String subject = (String) w.elementAt(0);
			String predict = (String) w.elementAt(1);
			if (!predict.endsWith("label") && !predict.endsWith("type") && !predict.endsWith("P108")) {
				String object = (String) w.elementAt(2);
				int n = 0;
				int len_1 = subject.length();
				int len_2 = object.length();
				int min = len_1;
				if (len_2 < len_1) min = len_2;
				for (int j=0; j<min; j++) {
					char c1 = subject.charAt(len_1-j-1);
					char c2 = object.charAt(len_2-j-1);
					if (c1 == c2) {
						n++;
					} else {
						break;
					}
				}
				if (n > n0 && n > 1) {
					id_line = line;
					n0 = n;
				}
			}
		}
		return id_line;
	}

    public String getNamedGraphBasePrefix(String line) {
		Vector u = StringUtils.parseData(line, '|');
		String t = (String) u.elementAt(1);
		int n = t.lastIndexOf("#");
		if (n == -1) {
			n = t.lastIndexOf("/");
		}
		return "<" + t.substring(0, n+1) + ">";
	}

    public String getNamedGraphUniqueIdentifier(String line) {
		Vector u = StringUtils.parseData(line, '|');
		String t = (String) u.elementAt(1);
		int n = t.lastIndexOf("#");
		if (n == -1) {
			n = t.lastIndexOf("/");
		}
		return t.substring(n+1, t.length());
	}

	public String getNamespace(String ng) {
		Vector w = simple_tuple_query(ng, 1000);

		String id_line = findNamedGraphIdentifierLine(w);
		Vector u = StringUtils.parseData(id_line, '|');
		String t = (String) u.elementAt(0);
		int n = t.lastIndexOf("#");
		if (n == -1) {
			n = t.lastIndexOf("/");
		}
		//return t.substring(n+1, t.length());
		return t.substring(0, n+1);
	}

    public String getNamedGraphUIDPredicate(String named_graph) {
		String s = getNamedGraphBasePrefixAndUniqueIdentifier(named_graph);
		Vector u = StringUtils.parseData(s);
		String basePrefix = (String) u.elementAt(0);
		String uid = (String) u.elementAt(1);
		//System.out.println("\nnamed_graph: " + named_graph);
		//System.out.println("\tbasePrefix: " + basePrefix);
		//System.out.println("\tid: " + uid);
		return basePrefix.substring(1, basePrefix.length()-1) + uid;
	}


	public String getNamedGraphBasePrefixAndUniqueIdentifier(String ng) {
		Vector w = simple_tuple_query(ng, 1000);
		String id_line = findNamedGraphIdentifierLine(w);
		return getNamedGraphBasePrefixAndUniqueIdentifier(ng, id_line);
	}

	public String getNamedGraphBasePrefixAndUniqueIdentifier(String named_graph, String line) {
		if (named_graph.compareTo("http://purl.obolibrary.org/obo/obi/2017-09-03/obi.owl") == 0) {
			return "<http://purl.obolibrary.org/obo/>|IAO_0000111";
		}
		String basePrefix = getNamedGraphBasePrefix(line);
		String uid = getNamedGraphUniqueIdentifier(line);
		return basePrefix + "|" + uid;
	}


    public String generate_sample_id(String named_graph, String basePrefix, String uid) {
		int knt = 0;
		Vector w = simple_tuple_query(named_graph, 1000);
		String target = get_named_graph_predicate(named_graph);

		int rand = 500;//new RandomVariateGenerator().uniform(0, 999);
		String line = (String) w.elementAt(rand);
		Vector u = StringUtils.parseData(line, '|');
		String s = (String) u.elementAt(0);
		String p = (String) u.elementAt(1);
		String o = (String) u.elementAt(2);
		return o;

	}


    public String construct_class_uri_query(String named_graph, String code) {
		//String p = getNamedGraphUIDPredicate(named_graph);
		String p = get_named_graph_predicate(named_graph);
		StringBuffer buf = new StringBuffer();

        buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
        buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
        buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");

        buf.append("SELECT distinct ?x").append("\n");
        buf.append("{ ").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("{").append("\n");
        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x ?p ?y ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("FILTER ((str(?p) = \"" + p + "\"^^xsd:string) && (str(?y) = \"" + code + "\"^^xsd:string))").append("\n");
        buf.append("} ").append("\n");
        return buf.toString();
    }


	public String get_class_URI(String named_graph, String code) {
		String query = construct_class_uri_query(named_graph, code);
		System.out.println(query);

		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return (String) v.elementAt(0);
		}
		return null;
	}


    public String construct_class_query_by_cui(String named_graph, String cui) {
		//String cui = get_class_URI(named_graph, code);
		System.out.println("cui: " + cui);
		StringBuffer buf = new StringBuffer();
        buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
        buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
        buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("SELECT distinct ?x ?p ?y").append("\n");
        buf.append("{ ").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("{").append("\n");
        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x ?p ?y ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("FILTER (str(?x) = \"" + cui + "\"^^xsd:string)").append("\n");
        buf.append("} ").append("\n");
        return buf.toString();
    }

	public Vector class_query(String named_graph, String code) {
		String query = construct_class_query_by_cui(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}


    public String construct_class_query_by_id(String named_graph, String id) {
		String cui = get_class_URI(named_graph, id);
		System.out.println("cui: " + cui);
		StringBuffer buf = new StringBuffer();
        buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
        buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
        buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("SELECT distinct ?x ?p ?y").append("\n");
        buf.append("{ ").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("{").append("\n");
        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x ?p ?y ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("FILTER (str(?x) = \"" + cui + "\"^^xsd:string)").append("\n");
        buf.append("} ").append("\n");
        return buf.toString();
    }

	public Vector class_query_by_id(String named_graph, String id) {
		String query = construct_class_query_by_id(named_graph, id);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

    public String construct_class_query_by_code(String named_graph, String code) {
		StringBuffer buf = new StringBuffer();
        String line = (String) basePrefixUIDHashMap.get(named_graph);
        Vector u = StringUtils.parseData(line, '|');
        String basePrefix = (String) u.elementAt(0);
        String uid = (String) u.elementAt(1);
		buf.append("PREFIX :" + basePrefix).append("\n");
        buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
        buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
        buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("SELECT distinct ?x ?p ?y").append("\n");
        buf.append("{ ").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("{").append("\n");
        buf.append("?x :" + uid + " ?x_code .").append("\n");
        buf.append("?x :" + uid + " \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("?x ?p ?y ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("} ").append("\n");
        return buf.toString();
    }

	public Vector class_query_by_code(String named_graph, String code) {
		String query = construct_class_query_by_code(named_graph, code);

		//System.out.println(query);

		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}


	public String getPrefixes() {
		return owlSPARQLUtils.getPrefixes();
	}

    public String construct_annotation_property_query(String named_graph) {
        StringBuffer buf = new StringBuffer();
        buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
        buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
        buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("").append("\n");
        buf.append("SELECT distinct ?p ?prop ?prop_value").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + "> {").append("\n");
        buf.append("?p a owl:AnnotationProperty .").append("\n");
        buf.append("?p ?prop ?prop_value ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
    }

	public Vector annotation_property_query(String named_graph) {
		String query = construct_annotation_property_query(named_graph);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

/*
	(73) http://purl.obolibrary.org/obo/IAO_0000115|http://www.w3.org/1999/02/22-rdf-syntax-ns#type|http://www.w3.org/2002/07/owl#AnnotationProperty
	(74) http://purl.obolibrary.org/obo/IAO_0000115|http://www.w3.org/2000/01/rdf-schema#label|definition
	(75) http://purl.obolibrary.org/obo/IAO_0000231|http://www.w3.org/1999/02/22-rdf-syntax-ns#type|http://www.w3.org/2002/07/owl#AnnotationProperty
	(76) http://purl.obolibrary.org/obo/IAO_0000425|http://www.w3.org/1999/02/22-rdf-syntax-ns#type|http://www.w3.org/2002/07/owl#AnnotationProperty
	(77) http://purl.obolibrary.org/obo/IAO_0000425|http://www.w3.org/2000/01/rdf-schema#label|expand assertion to
*/

	public HashMap createPropertyHashMap(String named_graph) {
		Vector w = annotation_property_query(named_graph);
		HashMap hmap = new HashMap();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String key = (String) u.elementAt(0);
			String prop_name = (String) u.elementAt(1);
			String prop_value = (String) u.elementAt(2);
			HashMap submap = new HashMap();
			if (hmap.containsKey(key)) {
				submap = (HashMap) hmap.get(key);
			}
			submap.put(prop_name, prop_value);
			hmap.put(key, submap);
		}
		return hmap;
	}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String construct_class_id_query(String named_graph, int limit) {
        HashMap hmap = getBasePrefixUIDHashMap();
		String value = (String) hmap.get(named_graph);
		Vector u = StringUtils.parseData(value);
		String basePrefix = (String) u.elementAt(0);
		String uid = (String) u.elementAt(1);

		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :" + basePrefix).append("\n");
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_code").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + "> {").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :" + uid + " ?x_code .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("LIMIT " + limit).append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector class_id_query(String named_graph, int limit) {
		String query = construct_class_id_query(named_graph, limit);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

    public HashMap createBasePrefixUIDHashMap(){
        HashMap hmap = getNameVersion2NamedGraphMap();
        Iterator it = hmap.keySet().iterator();
        HashMap basePrefixUIDHashMap = new HashMap();
        while (it.hasNext()) {
			String key = (String) it.next();
			Vector ng_vec = (Vector) hmap.get(key);
			for (int j=0; j<ng_vec.size(); j++) {
				String ng = (String) ng_vec.elementAt(j);
				Vector w = simple_tuple_query(ng, 1000);
				String id_line = findNamedGraphIdentifierLine(w);
				String s = getNamedGraphBasePrefixAndUniqueIdentifier(ng, id_line);
				Vector u = StringUtils.parseData(s);
				String basePrefix = (String) u.elementAt(0);
				String uid = (String) u.elementAt(1);
				if (basePrefix.endsWith("oboInOwl#>")) {
					uid = "id";
				}
				basePrefixUIDHashMap.put(ng, basePrefix + "|" + uid);
			}
		}
		return basePrefixUIDHashMap;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String construct_mapping_query(String named_graph, String code) {
		StringBuffer buf = new StringBuffer();
        buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
        buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
        buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("SELECT distinct ?x ?p ?y").append("\n");
        buf.append("{ ").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        //buf.append("{").append("\n");
        buf.append("{").append("\n");
        buf.append("?x ?p ?y .").append("\n");
        if (code != null) {
        	buf.append("FILTER (contains(str(?y), \"" + code + "\") || contains(str(?x), \"" + code + "\"))").append("\n");
		}
        //buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector mapping_query(String named_graph) {
		return mapping_query(named_graph, null);
	}


	public Vector mapping_query(String named_graph, String code) {
		String query = construct_mapping_query(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			Vector w = new Vector();
			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				String s = (String) u.elementAt(0);
				String o = (String) u.elementAt(2);
				if (code != null) {
					if (s.endsWith(code) || o.endsWith(code)) {
						w.add(line);
					}
				} else {
					w.add(line);
				}
			}
			w = new SortUtils().quickSort(w);
			return w;
		}
		return null;
	}

	public Vector format_class_data(Vector w) {
		Vector v = new Vector();
		if (w == null) return v;
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String s = (String) u.elementAt(0);
			String p = (String) u.elementAt(1);
			String o = (String) u.elementAt(2);
			int n = p.lastIndexOf("#");
			if (n == -1) {
				n = p.lastIndexOf("/");
			}
			String label = p.substring(n+1, p.length());
			System.out.println(label + ": " + o);
			v.add(s + "|" + label + "|" + o);
		}
		return new SortUtils().quickSort(v);
	}


    public String construct_inverse_class_query_by_code(String named_graph, String code) {
		StringBuffer buf = new StringBuffer();
        String line = (String) basePrefixUIDHashMap.get(named_graph);
        Vector u = StringUtils.parseData(line, '|');
        String basePrefix = (String) u.elementAt(0);
        String uid = (String) u.elementAt(1);
		buf.append("PREFIX :" + basePrefix).append("\n");
        buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
        buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
        buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");

        buf.append("SELECT distinct ?x ?p ?y").append("\n");

        buf.append("{ ").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("{").append("\n");
        buf.append("?y :" + uid + " ?y_code .").append("\n");
        buf.append("?y :" + uid + " \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("?x ?p ?y ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("} ").append("\n");
        return buf.toString();
    }

	public Vector inverse_class_query_by_code(String named_graph, String code) {
		String query = construct_inverse_class_query_by_code(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_search_by_code(String code) {
        StringBuffer buf = new StringBuffer();
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("SELECT distinct ?g ?x ?p ?z ").append("\n");
        buf.append("{").append("\n");
        buf.append("graph ?g {").append("\n");
        buf.append("?x ?p \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("?x ?p ?z").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector search_by_code(String code) {
		String query = construct_search_by_code(code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		} else {
			System.out.println("executeQuery returns NULL");
		}
		return null;
	}


	public String construct_search_by_property(String matchText, int maxReturn) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("SELECT distinct ?g ?x ?p ?y").append("\n");
		buf.append("{").append("\n");
		buf.append("graph ?g").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x ?p ?y .").append("\n");
		buf.append("FILTER contains(lcase(str(?y)), \"" + matchText + "\")").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("LIMIT " + maxReturn).append("\n");
		return buf.toString();
	}

	public Vector search_by_property(String mathText, int maxReturn) {
		String query = construct_search_by_property(mathText, maxReturn);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		} else {
			System.out.println("executeQuery returns NULL");
		}
		return null;
	}


	public String construct_superclass_query(String named_graph, String code) {
        HashMap hmap = getBasePrefixUIDHashMap();
		String value = (String) hmap.get(named_graph);
		Vector u = StringUtils.parseData(value);
		String basePrefix = (String) u.elementAt(0);
		String uid = (String) u.elementAt(1);

        StringBuffer buf = new StringBuffer();
        buf.append("PREFIX :" + basePrefix).append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("SELECT ?y_label ?y_code").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        //buf.append("?x a owl:Class .").append("\n");
        buf.append("?x :" + uid + " \"" + code + "\"^^xsd:string .").append("\n");

        buf.append("?x rdfs:subClassOf ?y .").append("\n");

        //buf.append("?y a owl:Class .").append("\n");
        buf.append("?y :" + uid + " ?y_code .").append("\n");
        buf.append("?y rdfs:label ?y_label .").append("\n");

        buf.append("}").append("\n");
        buf.append("}").append("\n");

        return buf.toString();
	}

	public Vector get_superclasses(String named_graph, String code) {
		String query = construct_superclass_query(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		} else {
			System.out.println("executeQuery returns NULL");
		}
		return null;
	}


	public String construct_subclass_query(String named_graph, String code) {
        HashMap hmap = getBasePrefixUIDHashMap();
		String value = (String) hmap.get(named_graph);
		Vector u = StringUtils.parseData(value);
		String basePrefix = (String) u.elementAt(0);
		String uid = (String) u.elementAt(1);

        StringBuffer buf = new StringBuffer();
        buf.append("PREFIX :" + basePrefix).append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("SELECT ?x_label ?x_code").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        //buf.append("?x a owl:Class .").append("\n");
        buf.append("?x :" + uid + " ?x_code .").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");

        //buf.append("?y a owl:Class .").append("\n");
        buf.append("?y :" + uid + " ?y_code .").append("\n");
        buf.append("?y :" + uid + " \"" + code + "\"^^xsd:string .").append("\n");

        buf.append("?x rdfs:subClassOf ?y .").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector get_subclasses(String named_graph, String code) {
		String query = construct_subclass_query(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		} else {
			System.out.println("executeQuery returns NULL");
		}
		return null;
	}

	public String construct_label_query(String named_graph, String code) {
        HashMap hmap = getBasePrefixUIDHashMap();
		String value = (String) hmap.get(named_graph);
		Vector u = StringUtils.parseData(value);
		String basePrefix = (String) u.elementAt(0);
		String uid = (String) u.elementAt(1);

        StringBuffer buf = new StringBuffer();
        buf.append("PREFIX :" + basePrefix).append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("SELECT ?x_label").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        //buf.append("?x a owl:Class .").append("\n");
        buf.append("?x :" + uid + " ?x_code .").append("\n");
        buf.append("?x :" + uid + " \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public String get_label(String named_graph, String code) {
		String query = construct_label_query(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return (String) v.elementAt(0);
		} else {
			System.out.println("executeQuery returns NULL");
		}
		return null;
	}

	public String construct_hierarchical_relationship_query(String named_graph, int maxReturn) {

        HashMap hmap = getBasePrefixUIDHashMap();
		String value = (String) hmap.get(named_graph);
		Vector u = StringUtils.parseData(value);
		String basePrefix = (String) u.elementAt(0);
		String uid = (String) u.elementAt(1);

        StringBuffer buf = new StringBuffer();
        buf.append("PREFIX :" + basePrefix).append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("SELECT ?y_label ?y_code ?x_label ?x_code").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("?x :" + uid + " ?x_code .").append("\n");
        buf.append("?y a owl:Class .").append("\n");
        buf.append("?y rdfs:label ?y_label .").append("\n");
        buf.append("?y :" + uid + " ?y_code .").append("\n");
        buf.append("?x rdfs:subClassOf ?y .").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        if (maxReturn != -1) {
        	buf.append("LIMIT " + maxReturn).append("\n");
		}
        return buf.toString();
	}

	public Vector get_hierarchical_relationships(String named_graph, int maxReturn) {
		String query = construct_hierarchical_relationship_query(named_graph, maxReturn);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		} else {
			System.out.println("executeQuery returns NULL");
		}
		return null;
	}


	public Vector get_hierarchical_relationships(String named_graph) {
		return get_hierarchical_relationships(named_graph, -1);
	}


    public String formatLabelCodeString(String line) {
		//Eye Part|C13019
		int n = line.indexOf("|");
		String label = line.substring(0, n);
		String code = line.substring(n+1, line.length());
		return label + " (" + code + ")";
	}

	public String construct_search_by_name_query(String matchText, int maxReturn) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT distinct ?g ?x ?x_label").append("\n");
		buf.append("{").append("\n");
		buf.append("graph ?g").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		String matchText_lc = matchText.toLowerCase();
		buf.append("FILTER contains(lcase(str(?x_label)), \"" + matchText_lc + "\")").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		if (maxReturn != -1 && maxReturn > 0) {
			buf.append("LIMIT " + maxReturn).append("\n");
		}
		return buf.toString();
	}

	public Vector search_by_name(String matchText, int maxReturn) {
		String query = construct_search_by_name_query(matchText, maxReturn);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		} else {
			System.out.println("executeQuery returns NULL");
		}
		return null;
	}

	public String construct_search_by_name_query(String named_graph, String matchText, int maxReturn) {
        HashMap hmap = getBasePrefixUIDHashMap();
		String value = (String) hmap.get(named_graph);
		Vector u = StringUtils.parseData(value);
		String basePrefix = (String) u.elementAt(0);
		String uid = (String) u.elementAt(1);

		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :" + basePrefix).append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :" + uid + " ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");

		String matchText_lc = matchText.toLowerCase();
		buf.append("FILTER contains(lcase(str(?x_label)), \"" + matchText_lc + "\")").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		if (maxReturn != -1 && maxReturn > 0) {
			buf.append("LIMIT " + maxReturn).append("\n");
		}
		return buf.toString();
	}

	public Vector search_by_name(String named_graph, String matchText, int maxReturn) {
		String query = construct_search_by_name_query(named_graph, matchText, maxReturn);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		} else {
			System.out.println("executeQuery returns NULL");
		}
		return null;
	}


	public String construct_search_by_code(String named_graph, String code) {
        HashMap hmap = getBasePrefixUIDHashMap();
		String value = (String) hmap.get(named_graph);
		Vector u = StringUtils.parseData(value);
		String basePrefix = (String) u.elementAt(0);
		String uid = (String) u.elementAt(1);

		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :" + basePrefix).append("\n");
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("SELECT distinct ?x_label ?x_code ").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("?x :" + uid + " ?x_code .").append("\n");
        buf.append("?x :" + uid + " \"" + code + "\"^^xsd:string").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector search_by_code(String named_graph, String code) {
		String query = construct_search_by_code(named_graph, code);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		} else {
			System.out.println("executeQuery returns NULL");
		}
		return null;
	}

    public static void main(String[] args) {
		String serviceUrl = args[0];
		System.out.println(serviceUrl);
		GeneralizedQueryUtils test = new GeneralizedQueryUtils(serviceUrl+"?query=");
        long ms = System.currentTimeMillis();

		String named_graph = args[1];
		named_graph = "http://ncicb.nci.nih.gov/trix/NCIt-HGNC";
		/*
		test.set_named_graph(named_graph);

		String queryfile = args[2];
		System.out.println(queryfile);
		//test.runQuery(queryfile);

		String code = "2002093";
		named_graph = "http://cbiit.nci.nih.gov/caDSR";
		String label = test.get_label(named_graph, code);

		System.out.println(named_graph);
		System.out.println(code);

		System.out.println("label: " + label);
		*/
		Vector w = test.mapping_query(named_graph);
		if (w != null) {
			System.out.println(w.size());
		}
    }
}
