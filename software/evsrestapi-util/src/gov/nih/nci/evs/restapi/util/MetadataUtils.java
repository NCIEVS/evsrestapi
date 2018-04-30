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




public class MetadataUtils {
    String serviceUrl = null;
    String sparql_endpoint = null;
	String namedGraph = null;
	OWLSPARQLUtils owlSPARQLUtils = null;
	SPARQLUtilsClient client = null;
	HashMap nameVersion2NamedGraphMap = null;
	HashMap uriBaseHashMap = null;
	HashMap namedGraph2IdentfierHashMap = null;
	HashMap nameGraph2PredicateHashMap = null;

	public String verify_service_url(String serviceUrl) {
		int n = serviceUrl.indexOf("?");
        if (n != -1) {
			serviceUrl = serviceUrl.substring(0, n);
		}
		return serviceUrl;
	}

    public MetadataUtils() {

	}

    public  MetadataUtils(String serviceUrl) {
		this.serviceUrl = verify_service_url(serviceUrl);
		initialize();
    }

    public void initialize() {
		this.sparql_endpoint = serviceUrl;
		this.client = new SPARQLUtilsClient(sparql_endpoint);
		this.owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint + "?query=");
		this.nameVersion2NamedGraphMap = owlSPARQLUtils.getNameVersion2NamedGraphMap();
		this.nameGraph2PredicateHashMap = createNameGraph2PredicateHashMap();
    }

    public HashMap getNameVersion2NamedGraphMap() {
		return nameVersion2NamedGraphMap;
	}

    public void dumpNameVersion2NamedGraphMap() {
		if (nameVersion2NamedGraphMap == null) return;
		Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
		Vector versions = new Vector();
		while (it.hasNext()) {
			String nameVersion = (String) it.next();
			Vector u = StringUtils.parseData(nameVersion);
			String codingSchemeName = (String) u.elementAt(0);
			String version = (String) u.elementAt(1);
			//System.out.println(nameVersion + " --> " + version);
			Vector named_graphs = (Vector) nameVersion2NamedGraphMap.get(nameVersion);
			for (int i=0; i<named_graphs.size(); i++) {
				String named_graph = (String) named_graphs.elementAt(i);
				System.out.println(nameVersion + " --> " + named_graph);
			}
		}

	}

    public String getLatestVersion(String codingScheme) {
		if (nameVersion2NamedGraphMap == null) return null;
		Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
		Vector versions = new Vector();
		while (it.hasNext()) {
			String nameVersion = (String) it.next();
			Vector u = StringUtils.parseData(nameVersion);
			String codingSchemeName = (String) u.elementAt(0);
			if (codingSchemeName.compareTo(codingScheme) == 0) {
				String version = (String) u.elementAt(1);
				versions.add(version);
			}
		}
		versions = new SortUtils().quickSort(versions);
        return (String) versions.elementAt(versions.size()-1);
	}

    public String getNamedGraph(String codingScheme) {
		return getNamedGraph(codingScheme, null);
	}

    public String getNamedGraph(String codingScheme, String version) {
		if (version == null) {
			version = getLatestVersion(codingScheme);
		}
		String namedGraph = client.getNamedGraphByCodingSchemeAndVersion(codingScheme, version);
		return namedGraph;
	}

	public static String getLatestVersionOfCodingScheme(String serviceUrl, String codingScheme) {
		String sparql_endpoint = serviceUrl + "?query=";
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint);
		HashMap nameVersion2NamedGraphMap = owlSPARQLUtils.getNameVersion2NamedGraphMap();
		if (nameVersion2NamedGraphMap == null) return null;
		Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
		Vector versions = new Vector();
		while (it.hasNext()) {
			String nameVersion = (String) it.next();
			Vector u = StringUtils.parseData(nameVersion);
			String codingSchemeName = (String) u.elementAt(0);
			if (codingSchemeName.compareTo(codingScheme) == 0) {
				String version = (String) u.elementAt(1);
				versions.add(version);
			}
		}
		versions = new SortUtils().quickSort(versions);
        return (String) versions.elementAt(versions.size()-1);
	}

	public static String getNamedGraphOfCodingScheme(String serviceUrl, String codingScheme, String version) {
		SPARQLUtilsClient client = new SPARQLUtilsClient(serviceUrl);
		String namedGraph = client.getNamedGraphByCodingSchemeAndVersion(codingScheme, version);
		return namedGraph;
	}

	public String sparqlEndpoint2ServiceUrl(String sparql_endpoint) {
		int n = sparql_endpoint.lastIndexOf("?");
		if (n == -1) return sparql_endpoint;
		return sparql_endpoint.substring(0, n);
	}

	public String serviceUrl2SparqlEndpoint(String serviceUrl) {
		int n = serviceUrl.lastIndexOf("?");
		if (n != -1) return serviceUrl;
		return serviceUrl + "?query=";
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String searchIdentifier(Vector w) {
		if (w == null) return null;
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (u.size() == 3) {
				String t0 = (String) u.elementAt(0);
				String t1 = (String) u.elementAt(1);
				String t2 = (String) u.elementAt(2);
				String t2a = t2.replaceAll(":", "_");
				if (t0.endsWith(t2) || t0.endsWith(t2a) || t0.compareTo(t2) == 0 || t0.compareTo(t2a) == 0) {
					int n = t1.lastIndexOf("#");
					if (n != -1) {
						return t1.substring(n+1, t1.length());
					}
				}
			}
		}
		return null;
	}

	public String searchPreferredTermPredicate(Vector w) {
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String t0 = (String) u.elementAt(0);
			String t1 = (String) u.elementAt(1);
			t1 = t1.toLowerCase();
			if (t1.endsWith("preferred term") || t1.endsWith("preferred_term")) {
				return t1;
			}
		}
		return null;
	}


	public Vector removeDuplicates(Vector v) {
		if (v == null ) return null;
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (!w.contains(line)) {
				w.add(line);
			}
		}
		return w;
	}

	public String construct_generate_sample_owlclasses(String named_graph, int limit) {
		StringBuffer buf = new StringBuffer();
		buf.append("SELECT ?x ?y ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		//buf.append("?x a owl:Class .").append("\n");
		buf.append("?x ?y ?z").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("LIMIT " + limit).append("\n");
		return buf.toString();
	}

	public Vector generateSampleOWLClasses(String named_graph, int limit) {
		String query = construct_generate_sample_owlclasses(named_graph, limit);
		Vector v = owlSPARQLUtils.executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		v = removeDuplicates(v);
		return v;
	}

	public String construct_get_distinct_predicates(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT distinct ?y ?y_label").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?y_label").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
    }

	public Vector getDistinctPredicates(String named_graph) {
		String query = construct_get_distinct_predicates(named_graph);
		Vector v = owlSPARQLUtils.executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		v = removeDuplicates(v);
		return v;
	}

    public String searchURIBase(Vector v) {
		if (v == null) return null;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String uri = (String) u.elementAt(0);
			String id = (String) u.elementAt(1);
			id = id.toLowerCase();
			if (id.endsWith("preferred term") || id.endsWith("preferred_term")) {
				int n = uri.lastIndexOf("#");
				if (n == -1) {
					n = uri.lastIndexOf("/");
				}
				if (n != -1) {
					return uri.substring(0, n+1);
				}
				return uri;
			}
		}
		return null;
	}

//(119) http://purl.obolibrary.org/obo/GO_0000015|http://www.geneontology.org/formats/oboInOwl#id|GO:0000015

    public String searchURIBase2(Vector v) {
		if (v == null) return null;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String uri = (String) u.elementAt(0);
			String predicate = (String) u.elementAt(1);
			String label = (String) u.elementAt(2);
			String t2 = label.replaceAll(":", "_");
			if (uri.endsWith(t2) || uri.endsWith(label)) {
				int n = uri.lastIndexOf("#");
				if (n == -1) {
					n = uri.lastIndexOf("/");
				}
				if (n != -1) {
					return uri.substring(0, n+1);
				}
				return uri;
			}
		}
		return null;
	}

    public HashMap getNamegraph2IdentifierHashMap() {
		if (namedGraph2IdentfierHashMap != null) {
			return namedGraph2IdentfierHashMap;
		}
		namedGraph2IdentfierHashMap = new HashMap();
		Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
		String identifier = null;
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector graph_names = (Vector) nameVersion2NamedGraphMap.get(key);
			int limit = 1000;
			for (int k=0; k<graph_names.size(); k++) {
				String graph_name = (String) graph_names.elementAt(k);
				Vector w = generateSampleOWLClasses(graph_name, 1000);
				identifier = searchIdentifier(w);
				if (identifier != null) {
					namedGraph2IdentfierHashMap.put(key + "|" + graph_name, identifier);
				} else {
					w = getDistinctPredicates(graph_name);
					identifier = searchPreferredTermPredicate(w);
					if (identifier != null) {
						namedGraph2IdentfierHashMap.put(key + "|" + graph_name, identifier);
				    }
				}
			}
		}
		return namedGraph2IdentfierHashMap;
	}


    public HashMap getURIBaseHashMap() {
		if (uriBaseHashMap != null) {
			return uriBaseHashMap;
		}
		uriBaseHashMap = new HashMap();
        HashMap namedGraph2IdentfierHashMap = getNamegraph2IdentifierHashMap();
		Iterator it2 = namedGraph2IdentfierHashMap.keySet().iterator();
		while (it2.hasNext()) {
			String key = (String) it2.next();
			String identifier = (String) namedGraph2IdentfierHashMap.get(key);
			Vector u = StringUtils.parseData(key, '|');
			String graph_name = (String) u.elementAt(2);
			String id = identifier.toLowerCase();
			if (id.endsWith("preferred term") || id.endsWith("preferred_term")) {
				Vector w = getDistinctPredicates(graph_name);
				id = searchURIBase(w);
				uriBaseHashMap.put(key, id);
			} else {
				Vector w = generateSampleOWLClasses(graph_name, 1000);
				id = searchURIBase2(w);
				uriBaseHashMap.put(key, id);
			}
		}
        return uriBaseHashMap;
	}

    public String getURIBase(String named_graph) {
        Iterator it = uriBaseHashMap.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			Vector u = StringUtils.parseData(key, '|');
			String graph_name = (String) u.elementAt(2);
			if (graph_name.compareTo(named_graph) == 0) {
				return (String) uriBaseHashMap.get(key);
			}
		}
		return null;
	}

    public String getURI(String named_graph, String code) {
		String uriBase = getURIBase(named_graph);
		return uriBase + code;
	}

    public Vector getSupportedNamedGraphs() {
		Vector w = new Vector();
        HashMap uriBaseHashMap = getURIBaseHashMap();
		Iterator it = uriBaseHashMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector u = StringUtils.parseData(key, '|');
			w.add((String) u.elementAt(2));
		}
		w = new SortUtils().quickSort(w);
		return w;
	}

	public HashMap createNameGraph2PredicateHashMap() {
		HashMap hmap = new HashMap();
		Vector named_graphs = getSupportedNamedGraphs();
		StringUtils.dumpVector("SupportedNamedGraphs", named_graphs);
		for (int i=0; i<named_graphs.size(); i++) {
			String named_graph = (String) named_graphs.elementAt(i);
			HashMap predicate_hmap = createPredicateHashMap(named_graph);
			hmap.put(named_graph, predicate_hmap);
		}
		return hmap;
	}

	public HashMap createPredicateHashMap(String named_graph) {
		HashMap hmap = new HashMap();
		Vector w = getDistinctPredicates(named_graph);
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String key = (String) u.elementAt(0);
			String value = (String) u.elementAt(1);
			hmap.put(key, value);
		}
		return hmap;
	}

	public HashMap getPredicateHashMap(String named_graph) {
		return (HashMap) nameGraph2PredicateHashMap.get(named_graph);
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		System.out.println(serviceUrl);
		MetadataUtils test = new MetadataUtils(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		long ms = System.currentTimeMillis();
		String version = test.getLatestVersion(codingScheme);
		System.out.println(codingScheme);
		System.out.println(version);
		String named_graph = test.getNamedGraph(codingScheme);
		System.out.println(named_graph);

		test.dumpNameVersion2NamedGraphMap();

		String version_test = MetadataUtils.getLatestVersionOfCodingScheme(serviceUrl, codingScheme);
		System.out.println("getLatestVersionOfCodingScheme: " + version_test);

		String named_graph_test = MetadataUtils.getNamedGraphOfCodingScheme(serviceUrl, codingScheme, version_test);
		System.out.println("getNamedGraphOfCodingScheme: " + named_graph_test);

	}
}