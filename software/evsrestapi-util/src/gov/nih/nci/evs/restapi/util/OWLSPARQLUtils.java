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


public class OWLSPARQLUtils {
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String restURL = null;
    String named_graph_id = ":NHC0";
    String BASE_URI = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

    ParserUtils parser = new ParserUtils();
    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String username = null;
    String password = null;

    public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

    public void setJSONUtils(JSONUtils jsonUtils) {
		this.jsonUtils = jsonUtils;
	}

    public void setHTTPUtils(HTTPUtils httpUtils) {
		this.httpUtils = httpUtils;
	}

	public void setOntologyUri2LabelMap(HashMap ontologyUri2LabelMap) {
		this.ontologyUri2LabelMap = ontologyUri2LabelMap;
	}

	public OWLSPARQLUtils() {
		this.httpUtils = new HTTPUtils();
        this.jsonUtils = new JSONUtils();
        this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
	}

	public String verifyServiceUrl(String serviceUrl) {
		if (serviceUrl.indexOf("?query=?query=") != -1) {
			int n = serviceUrl.lastIndexOf("?");
			serviceUrl = serviceUrl.substring(0, n);
		} else if (serviceUrl.indexOf("?") == -1) {
			serviceUrl = serviceUrl + "?query=";
		}
		return serviceUrl;
	}

	public OWLSPARQLUtils(String serviceUrl) {
		this.serviceUrl = verifyServiceUrl(serviceUrl);
		System.out.println(this.serviceUrl);
		this.httpUtils = new HTTPUtils(serviceUrl, null, null);
        this.jsonUtils = new JSONUtils();
        this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
    }

	public OWLSPARQLUtils(ServerInfo serverInfo) {
		this.serviceUrl = serverInfo.getServiceUrl();
		this.serviceUrl = verifyServiceUrl(serviceUrl);
		this.httpUtils = new HTTPUtils(serviceUrl, serverInfo.getUsername(), serverInfo.getPassword());
        this.jsonUtils = new JSONUtils();
        this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
	}

	public OWLSPARQLUtils(String serviceUrl, String username, String password) {
		this.serviceUrl = serviceUrl;//verifyServiceUrl(serviceUrl);
		this.restURL = serviceUrl;
		this.username = username;
		this.password = password;

		System.out.println(this.serviceUrl);
		this.httpUtils = new HTTPUtils(serviceUrl, username, password);
        this.jsonUtils = new JSONUtils();
        this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
    }

    HashMap createOntologyUri2LabelMap() {
		HashMap ontologyUri2LabelMap = new HashMap();
		ontologyUri2LabelMap.put(BASE_URI, "NCI_Thesaurus");
		ontologyUri2LabelMap.put("http://purl.obolibrary.org/obo/go.owl", "GO");
		ontologyUri2LabelMap.put("http://purl.obolibrary.org/obo/obi.owl", "obi");
		ontologyUri2LabelMap.put("http://purl.obolibrary.org/obo/chebi.owl", "chebi");
		ontologyUri2LabelMap.put("http://id.nlm.nih.gov/mesh/2017-11-07", "mesh");
		ontologyUri2LabelMap.put("http://cbiit.nci.nih.gov/caDSR/20171108", "caDSR");
		return ontologyUri2LabelMap;
	}

	public String get_named_graph() {
		return this.named_graph;
	}

    public void set_named_graph_id(String named_graph_id) {
		this.named_graph_id = named_graph_id;
	}

    public void set_version(String version) {
		this.version = version;
	}

    public String getServiceUrl() {
		return this.serviceUrl;
	}

    public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
	}

	public String get_version() {
		return this.version;
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

    public String getQuery(String query_file) {
		try {
			String query = httpUtils.loadQuery(query_file, false);
			return query;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}


	public String loadQuery(String query_file) {
		try {
			String query = httpUtils.loadQuery(query_file, false);
			return query;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

    public Vector execute(String query_file) {
		try {
			String query = httpUtils.loadQuery(query_file, false);
			return executeQuery(query);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

    public Vector executeQuery(String query) {
        Vector v = null;
        try {
			if (this.password == null) {
				String json = null;
				query = httpUtils.encode(query);
            	json = httpUtils.executeQuery(query);
            	v = new JSONUtils().parseJSON(json);
			} else {
				v = httpUtils.execute(this.serviceUrl, this.username, this.password, query, false); // no parser
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}

    public String getJSONResponseString(String query) {
        try {
			query = httpUtils.encode(query);
            String json = httpUtils.executeQuery(query);
			return json;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public String construct_get_ontology_info() {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("SELECT ?g ?x_version_info ?x_dc_date ?x_comment").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph ?g").append("\n");
		buf.append("    {").append("\n");

		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Ontology .").append("\n");
		buf.append("	    ?x owl:versionInfo ?x_version_info .").append("\n");
		buf.append("        OPTIONAL {").append("\n");
		buf.append("	        ?x dc:date ?x_dc_date .").append("\n");
		buf.append("	        ?x rdfs:comment ?x_comment").append("\n");
		buf.append("        }").append("\n");
		buf.append("    }").append("\n");
		buf.append("    UNION").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Ontology .").append("\n");
		buf.append("	    ?x owl:versionIRI ?x_version_info .").append("\n");
		buf.append("	    OPTIONAL {?x dc:date ?x_dc_date .}").append("\n");
		buf.append("	    ?x rdfs:comment ?x_comment").append("\n");
		buf.append("    }").append("\n");

        buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector get_ontology_info() {
		Vector v = executeQuery(construct_get_ontology_info());
		v = new ParserUtils().getResponseValues(v);
		return v;
	}


	public String construct_get_ontology_info(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("SELECT ?x_version_info ?x_dc_date ?x_comment").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");

		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Ontology .").append("\n");
		buf.append("	    ?x owl:versionInfo ?x_version_info .").append("\n");
		buf.append("        OPTIONAL {").append("\n");
		buf.append("	        ?x dc:date ?x_dc_date .").append("\n");
		buf.append("	        ?x rdfs:comment ?x_comment").append("\n");
		buf.append("        }").append("\n");
		buf.append("    }").append("\n");
		buf.append("    UNION").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Ontology .").append("\n");
		buf.append("	    ?x owl:versionIRI ?x_version_info .").append("\n");
		buf.append("	    OPTIONAL {?x dc:date ?x_dc_date .}").append("\n");
		buf.append("	    ?x rdfs:comment ?x_comment").append("\n");
		buf.append("    }").append("\n");

        buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector get_ontology_info(String named_graph) {
		Vector v = executeQuery(construct_get_ontology_info(named_graph));
		v = new ParserUtils().getResponseValues(v);
		return v;
	}



	public HashMap getGraphName2VersionHashMap() {

		HashMap hmap = new HashMap();
		Vector v = get_ontology_info();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String graph = (String) u.elementAt(0);
			String version = (String) u.elementAt(1);
			hmap.put(graph, version);
		}
        return hmap;
	}

	public String construct_get_named_graphs() {
		//String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		//buf.append(prefixes);
		buf.append("SELECT distinct ?g {").append("\n");
		buf.append("  { ?s ?p ?o } ").append("\n");
		buf.append("UNION ").append("\n");
		buf.append("    { graph ?g { ?s ?p ?o } }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getNamedGraph() {
		return executeQuery(construct_get_named_graphs());
	}

	public String construct_get_label_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select ?x_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class . ").append("\n");
		buf.append("	    ?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("	    ?x rdfs:label ?x_label    ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getLabelByCode(String named_graph, String code) {
		return executeQuery(construct_get_label_by_code(named_graph, code));
	}

	public Vector getInverseRolesByCode(String named_graph, String code) {
		return getInboundRolesByCode(named_graph, code);
	}

	public String construct_get_code_and_label(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select ?x_code ?x_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class . ").append("\n");
		buf.append("	    ?x :NHC0 ?x_code .").append("\n");
		buf.append("	    ?x rdfs:label ?x_label    ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getCodeAndLabel(String named_graph) {
		return executeQuery(construct_get_code_and_label(named_graph));
	}

	public String construct_get_class_counts() {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?g ( count(?class) as ?count ) ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph ?g { ?class a owl:Class }").append("\n");
		buf.append("}").append("\n");
		buf.append("group by ?g").append("\n");
		return buf.toString();
	}


	public Vector getClassCounts() {
		return executeQuery(construct_get_class_counts());
	}

	public String construct_get_class_count(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT (count(?class) as ?count) ").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("   	    ?class a owl:Class .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getClassCount(String named_graph) {
		return executeQuery(construct_get_class_count(named_graph));
	}

	public String construct_get_triple_count(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT (count(*) as ?count) ").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("   	    ?s ?p ?o .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getTripleCount(String named_graph) {
		return executeQuery(construct_get_triple_count(named_graph));
	}


    public int get_triple_count(String named_graph) {
		Vector w = getTripleCount(named_graph);
		w = new ParserUtils().getResponseValues(w);
		String count_str = (String) w.elementAt(0);
		return Integer.parseInt(count_str);
	}

	public Vector get_triple_counts(Vector named_graphs) {
		Vector v = new Vector();
        for (int i=0; i<named_graphs.size(); i++) {
			String named_graph = (String) named_graphs.elementAt(i);
			int count = get_triple_count(named_graph);
			v.add(named_graph + "|" + count);
		}
		return v;
	}

	public String construct_get_superclasses_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?z_label ?z_code WHERE").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("  {").append("\n");
		buf.append("  {").append("\n");
		buf.append("	  {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("	    ?x rdfs:subClassOf ?z .").append("\n");
		buf.append("	    ?z a owl:Class .").append("\n");
		buf.append("	    ?z rdfs:label ?z_label .").append("\n");
		buf.append("	    ?z :NHC0 ?z_code").append("\n");
		buf.append("	  }").append("\n");
		buf.append("	  FILTER (?x != ?z)").append("\n");
		buf.append("  }").append("\n");
		buf.append("  UNION").append("\n");
		buf.append("  {").append("\n");
		buf.append("	  {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("	    ?list rdf:rest*/rdf:first ?z .").append("\n");
		buf.append("	    ?z a owl:Class .").append("\n");
		buf.append("	    ?z rdfs:label ?z_label .").append("\n");
		buf.append("	    ?z :NHC0 ?z_code").append("\n");
		buf.append("	  }").append("\n");
		buf.append("	  FILTER (?x != ?z)").append("\n");
		buf.append("  }").append("\n");
		buf.append("  }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getSuperclassesByCode(String named_graph, String code) {
		return executeQuery(construct_get_superclasses_by_code(named_graph, code));
	}

	public String construct_get_subclasses_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?y_label ?y_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("  {").append("\n");
		buf.append("  {").append("\n");
		buf.append("	  {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("	    ?y rdfs:subClassOf ?x .").append("\n");
		buf.append("	    ?y a owl:Class .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");
		buf.append("	    ?y :NHC0 ?y_code .").append("\n");
		buf.append("	  }").append("\n");
		buf.append("	  FILTER (?x != ?y)").append("\n");
		buf.append("  }").append("\n");
		buf.append("  UNION").append("\n");
		buf.append("  {").append("\n");
		buf.append("	  {").append("\n");
		buf.append("	    ?y a owl:Class .").append("\n");
		buf.append("	    ?y :NHC0 ?y_code .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");

		buf.append("		?y owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");

		buf.append("	    ?list rdf:rest*/rdf:first ?x .").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("	  }").append("\n");
		buf.append("	  FILTER (?x != ?y)").append("\n");
		buf.append("  }").append("\n");
		buf.append("  }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

    public boolean isLeaf(String named_graph, String code) {
		Vector v = getSubclassesByCode(named_graph, code);
		if (v != null && v.size() > 0) return false;
	    return true;
	}

	public Vector getSubclassesByCode(String named_graph, String code) {
		return executeQuery(construct_get_subclasses_by_code(named_graph, code));
	}

	public Vector getRolesByCode(String named_graph, String code) {
		return getOutboundRolesByCode(named_graph, code);
	}

	public String construct_get_properties_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y rdfs:range ?y_range").append("\n");
		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?y_range)!=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getPropertiesByCode(String named_graph, String code) {
		return executeQuery(construct_get_properties_by_code(named_graph, code));
	}

	public String construct_get_ontology_version_info(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_version_info").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Ontology .").append("\n");
		buf.append("	    ?x owl:versionInfo ?x_version_info").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector getOntologyVersionInfo(String named_graph) {
		return executeQuery(construct_get_ontology_version_info(named_graph));
	}

	public String get_ontology_version(String named_graph) {
		try {
			Vector v = getOntologyVersionInfo(named_graph);
			if (v != null && v.size() > 0) {
				String version = (String) v.elementAt(0);
				version = new ParserUtils().getValue(version);
				return version;
			}
		} catch (Exception ex) {

		}
	    return null;
	}

	public String construct_get_synonyms(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?z_axiom ?x_label ?x_code ?term_name ?y ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x " + named_graph_id + " \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label " + " \"FULL_SYN\"^^xsd:string .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?term_name .").append("\n");
		buf.append("            ?z_axiom ?y ?z ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getComplexProperties(String named_graph, String code, String propertyName) {
		Vector v = getPropertyQualifiersByCode(named_graph, code);
		if (v == null) return null;
		v = new ParserUtils().filterPropertyQualifiers(v, propertyName);
		return v;
	}

	public Vector getSynonyms(String named_graph, String code) {
		//return getComplexProperties(named_graph, code, Constants.FULL_SYN);
		return getAxioms(named_graph, code, Constants.FULL_SYN);
	}

	public String construct_get_annotation_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?p_label ?p_code ").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?p a owl:AnnotationProperty .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getAnnotationProperties(String named_graph) {
		return executeQuery(construct_get_annotation_properties(named_graph));
	}

	public String construct_get_object_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?p_label ?p_code ").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?p a owl:ObjectProperty.").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getObjectProperties(String named_graph) {
		return executeQuery(construct_get_object_properties(named_graph));
	}

	public String construct_get_axioms_by_code(String named_graph, String code) {
		return construct_get_property_qualifiers_by_code(named_graph, code);
	}

	public String construct_get_property_qualifiers_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?z_axiom ?x_label ?x_code ?p_label ?p_code ?z_target ?y_label ?y_code ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    { ").append("\n");
		buf.append("    	{").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?z_axiom ?y ?z . ").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y rdfs:label ?y_label").append("\n");
		buf.append("        }").append("\n");
		buf.append("     }").append("\n");
		buf.append("}").append("\n");
		//buf.append("LIMIT " + Constants.DEFAULT_LIMIT).append("\n");
		return buf.toString();
	}

	public Vector getPropertyQualifiersByCode(String named_graph, String code) {
		Vector v = executeQuery(construct_get_property_qualifiers_by_code(named_graph, code));
		return v;
	}

	public String construct_get_inbound_roles_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    { ").append("\n");

		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("		?z2 a owl:Restriction .").append("\n");
		buf.append("		?z2 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	    }").append("\n");
		buf.append("	   UNION").append("\n");

		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");

		buf.append("		?x rdfs:subClassOf ?r .").append("\n");

		buf.append("		?r a owl:Restriction .").append("\n");
		buf.append("		?r owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?r owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> ").append("\n");
		buf.append("	   }").append("\n");

		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");

		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");

		buf.append("			?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("				?z2 a owl:Restriction .").append("\n");
		buf.append("				?z2 owl:onProperty ?p .").append("\n");
		buf.append("				?p rdfs:label ?p_label .").append("\n");
		buf.append("				?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("				?y a owl:Class .").append("\n");
		buf.append("				?y :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> ").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");

		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list1 .").append("\n");

		buf.append("			?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("			     ?z2 owl:unionOf ?list2 .").append("\n");
		buf.append("			     ?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("				 ?z3 owl:intersectionOf ?list3 .").append("\n");
		buf.append("				 ?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		buf.append("					?z4 a owl:Restriction .").append("\n");
		buf.append("					?z4 owl:onProperty ?p .").append("\n");
		buf.append("					?p rdfs:label ?p_label .").append("\n");
		buf.append("					?z4 owl:someValuesFrom ?y .").append("\n");
		buf.append("					?y a owl:Class .").append("\n");
		buf.append("					?y :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> ").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}


	public Vector getInboundRolesByCode(String named_graph, String code) {
		String query = construct_get_inbound_roles_by_code(named_graph, code);
		return executeQuery(construct_get_inbound_roles_by_code(named_graph, code));
	}

	public String construct_get_outbound_roles_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?p_label ?y_label ?y_code ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");

		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("		?z2 a owl:Restriction .").append("\n");
		buf.append("		?z2 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	    }").append("\n");
		buf.append("	   UNION").append("\n");

		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");

		buf.append("		?x rdfs:subClassOf ?r .").append("\n");

		buf.append("		?r a owl:Restriction .").append("\n");
		buf.append("		?r owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?r owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION ").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");

		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");

		buf.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("		?z2 a owl:Restriction .").append("\n");
		buf.append("		?z2 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");

		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list1 .").append("\n");

		buf.append("			?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("			     ?z2 owl:unionOf ?list2 .").append("\n");
		buf.append("			     ?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("				 ?z3 owl:intersectionOf ?list3 .").append("\n");
		buf.append("				 ?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		buf.append("					?z4 a owl:Restriction .").append("\n");
		buf.append("					?z4 owl:onProperty ?p .").append("\n");
		buf.append("					?p rdfs:label ?p_label .").append("\n");
		buf.append("					?z4 owl:someValuesFrom ?y .").append("\n");
		buf.append("					?y :NHC0 ?y_code .").append("\n");
		buf.append("					?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}


	public Vector getOutboundRolesByCode(String named_graph, String code) {
		String query = construct_get_outbound_roles_by_code(named_graph, code);
		return executeQuery(query);
	}

	public String construct_get_role_relationships(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		//buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?y_label ?y_code").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    { ").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?r .").append("\n");
		buf.append("		?r a owl:Restriction .").append("\n");
		buf.append("		?r owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?r owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("		?z2 a owl:Restriction .").append("\n");
		buf.append("		?z2 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z .").append("\n");
		buf.append("			?z owl:intersectionOf ?list .").append("\n");
		buf.append("			?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("				?z2 a owl:Restriction .").append("\n");
		buf.append("				?z2 owl:onProperty ?p .").append("\n");
		buf.append("				?p rdfs:label ?p_label .").append("\n");
		buf.append("				?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("				?y a owl:Class .").append("\n");
		buf.append("				?y :NHC0 ?y_code .").append("\n");
		buf.append("		        ?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z1 .").append("\n");
		buf.append("			?z1 owl:intersectionOf ?list1 .").append("\n");
		buf.append("			?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("			     ?z2 owl:unionOf ?list2 .").append("\n");
		buf.append("			     ?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("				 ?z3 owl:intersectionOf ?list3 .").append("\n");
		buf.append("				 ?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		buf.append("					?z4 a owl:Restriction .").append("\n");
		buf.append("					?z4 owl:onProperty ?p .").append("\n");
		buf.append("					?p rdfs:label ?p_label .").append("\n");
		buf.append("					?z4 owl:someValuesFrom ?y .").append("\n");
		buf.append("					?y a owl:Class .").append("\n");
		buf.append("					?y :NHC0 ?y_code .").append("\n");
		buf.append("					?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector getRoleRelationships(String named_graph) {
		String query = construct_get_role_relationships(named_graph);
		return executeQuery(query);
	}


	public String construct_get_inverse_associations_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label").append("\n");
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
		buf.append("	    ?z :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");
		buf.append("	    ?y :NHC0 ?y_code .").append("\n");
		buf.append("	    ?y rdfs:range ?y_range").append("\n");
		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?y_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getInverseAssociationsByCode(String named_graph, String code) {
		return executeQuery(construct_get_inverse_associations_by_code(named_graph, code));
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String construct_get_associations_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?y_label ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("	    ?y a owl:AnnotationProperty .").append("\n");
		buf.append("	    ?x ?y ?z .").append("\n");
		buf.append("	    ?z a owl:Class .").append("\n");
		buf.append("	    ?z rdfs:label ?z_label .").append("\n");
		buf.append("	    ?z :NHC0 ?z_code .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");
		buf.append("	    ?y :NHC0 ?y_code .").append("\n");
		buf.append("	    ?y rdfs:range ?y_range").append("\n");
		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?y_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAssociationsByCode(String named_graph, String code) {
		return executeQuery(construct_get_associations_by_code(named_graph, code));
	}

	public String construct_get_associations(String named_graph, String associationName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class .").append("\n");
		buf.append("	    ?x rdfs:label ?x_label .").append("\n");
		buf.append("	    ?x :NHC0 ?x_code .").append("\n");
		buf.append("	    ?y a owl:AnnotationProperty .").append("\n");
		buf.append("	    ?y rdfs:label ?y_label .").append("\n");
		buf.append("	    ?y  rdfs:label  \"" + associationName + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("	    ?x ?y ?z .").append("\n");
		buf.append("	    ?z a owl:Class .").append("\n");
		buf.append("	    ?z rdfs:label ?z_label .").append("\n");
		buf.append("	    ?z :NHC0 ?z_code .").append("\n");
		//buf.append("	    ?y rdfs:range ?y_range").append("\n");
		buf.append("    }").append("\n");
		//buf.append("    FILTER (str(?y_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAssociations(String named_graph, String associationName) {
		return executeQuery(construct_get_associations(named_graph, associationName));
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String construct_get_annotaion_properties_by_code(String named_graph, String code) {
		return construct_get_annotaion_properties_by_code(named_graph, code, null);
	}

	public String construct_get_annotaion_properties_by_code(String named_graph, String code, String associationName) {
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
        buf.append("").append("\n");
        buf.append("SELECT ?x_label ?y_label ?z").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("?x ?y ?z .").append("\n");
        buf.append("?y rdfs:label ?y_label .").append("\n");

        if (associationName !=null) {
			buf.append("?y rdfs:label \"" + associationName + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		}

        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getAnnotaionPropertiesByCode(String named_graph, String code) {
		String query = construct_get_annotaion_properties_by_code(named_graph, code);
		Vector v = executeQuery(query);
        v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public Vector getAnnotaionPropertiesByCode(String named_graph, String code, String assoicationName) {
		String query = construct_get_annotaion_properties_by_code(named_graph, code, assoicationName);
		Vector v = executeQuery(query);
        v = new ParserUtils().getResponseValues(v);
        return v;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Vector getDiseaseIsStageSourceCodes(String named_graph) {
		return executeQuery(construct_get_association_sources(named_graph, "Disease_Is_Stage", true));
	}

	public Vector getDiseaseIsGradeSourceCodes(String named_graph) {
		return executeQuery(construct_get_association_sources(named_graph, "Disease_Is_Grade", true));
	}

	public Vector getDiseaseIsStageSources(String named_graph, boolean code_only) {
		return executeQuery(construct_get_association_sources(named_graph, "Disease_Is_Stage", code_only));
	}

	public Vector getDiseaseIsGradeSources(String named_graph, boolean code_only) {
		return executeQuery(construct_get_association_sources(named_graph, "Disease_Is_Grade", code_only));
	}

	public Vector getAssociationSourceCodes(String named_graph, String associationName) {
		return executeQuery(construct_get_association_sources(named_graph, associationName, true));
	}

	public String construct_get_association_source_by_code(String named_graph, String associationName) {
		boolean code_only = true;
		return construct_get_association_sources(named_graph, associationName, code_only);
	}

	public String construct_get_association_sources(String named_graph, String associationName, boolean code_only) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (code_only) {
			buf.append("SELECT distinct ?x_code").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");

		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:subClassOf ?r .").append("\n");
		buf.append("		?r a owl:Restriction .").append("\n");
		buf.append("		?r owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	    UNION").append("\n");

		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("				?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("				?z2 a owl:Restriction .").append("\n");
		buf.append("				?z2 owl:onProperty ?p .").append("\n");
		buf.append("				?p rdfs:label ?p_label .").append("\n");
		buf.append("		FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");

		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x owl:equivalentClass ?z1 .").append("\n");
		buf.append("			?z1 owl:intersectionOf ?list1 .").append("\n");
		buf.append("			?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("			     ?z2 owl:unionOf ?list2 .").append("\n");
		buf.append("			     ?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("				 ?z3 owl:intersectionOf ?list3 .").append("\n");
		buf.append("				 ?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		buf.append("					?z4 a owl:Restriction .").append("\n");
		buf.append("					?z4 owl:onProperty ?p .").append("\n");
		buf.append("					?p rdfs:label ?p_label .").append("\n");
		buf.append("					FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector getAssociationSourcesAndTargets(String named_graph, String associationName, boolean code_only) {
		return executeQuery(construct_get_association_sources_and_targets(named_graph, associationName, code_only));
	}

	public String construct_get_association_sources_and_targets(String named_graph, String associationName, boolean code_only) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (code_only) {
			buf.append("SELECT distinct ?x_code ?y_code").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?y_label ?y_code").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");

        buf.append("        {");
        buf.append("        ?x a owl:Class .");
        buf.append("		?x :NHC0 ?x_code .").append("\n");
        buf.append("        ?x rdfs:label ?x_label .");
        buf.append("        ?x ?p ?y .");
        buf.append("        ?p rdfs:label ?p_label .");
        buf.append("        ?y a owl:Class .");
        buf.append("        ?y :NHC0 ?y_code .").append("\n");
        buf.append("        ?y rdfs:label ?y_label .");
		buf.append("		FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
        buf.append("        }").append("\n");
		buf.append("	    UNION").append("\n");

		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("		?z2 a owl:Restriction .").append("\n");
		buf.append("		?z2 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");

		buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");

		buf.append("		FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION ").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:subClassOf ?r .").append("\n");
		buf.append("		?r a owl:Restriction .").append("\n");
		buf.append("		?r owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");

		buf.append("		?r owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");


		buf.append("		FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }	").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x owl:equivalentClass ?z .").append("\n");
		buf.append("			?z owl:intersectionOf ?list .").append("\n");
		buf.append("			?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("				?z2 a owl:Restriction .").append("\n");
		buf.append("				?z2 owl:onProperty ?p .").append("\n");
		buf.append("				?p rdfs:label ?p_label .").append("\n");

		buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");


		buf.append("				FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x owl:equivalentClass ?z1 .").append("\n");
		buf.append("			?z1 owl:intersectionOf ?list1 .").append("\n");
		buf.append("			?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("			     ?z2 owl:unionOf ?list2 .").append("\n");
		buf.append("			     ?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("				 ?z3 owl:intersectionOf ?list3 .").append("\n");
		buf.append("				 ?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		buf.append("					?z4 a owl:Restriction .").append("\n");
		buf.append("					?z4 owl:onProperty ?p .").append("\n");
		buf.append("					?p rdfs:label ?p_label .").append("\n");

		buf.append("		?z4 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");



		buf.append("					FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public String construct_get_relationship_sources(String named_graph, String associationName, boolean code_only) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");

		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:subClassOf ?r .").append("\n");
		buf.append("		?r a owl:Restriction .").append("\n");
		buf.append("		?r owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }	").append("\n");
		buf.append("	   UNION ").append("\n");

		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("		?z2 a owl:Restriction .").append("\n");
		buf.append("		?z2 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("			?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("				?z2 a owl:Restriction .").append("\n");
		buf.append("				?z2 owl:onProperty ?p .").append("\n");
		buf.append("				?p rdfs:label ?p_label .").append("\n");
		buf.append("				FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list1 .").append("\n");
		buf.append("			?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("			     ?z2 owl:unionOf ?list2 .").append("\n");
		buf.append("			     ?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("				 ?z3 owl:intersectionOf ?list3 .").append("\n");
		buf.append("				 ?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		buf.append("					?z4 a owl:Restriction .").append("\n");
		buf.append("					?z4 owl:onProperty ?p .").append("\n");
		buf.append("					?p rdfs:label ?p_label .").append("\n");
		buf.append("					FILTER (str(?p_label)=\"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}




	public String construct_get_disjoint_with_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?y_label ?y_code ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:disjointWith ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	    }").append("\n");
		buf.append("	    ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getDisjointWithByCode(String named_graph, String code) {
		return executeQuery(construct_get_disjoint_with_by_code(named_graph, code));
	}

	public String construct_get_object_properties_domain_range(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_pt ?x_code ?x_domain_label ?x_range_label ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:ObjectProperty .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:domain ?x_domain .").append("\n");
		buf.append("		?x_domain rdfs:label ?x_domain_label .").append("\n");
		buf.append("		?x rdfs:range ?x_range .").append("\n");
		buf.append("		?x_range rdfs:label ?x_range_label .").append("\n");
		buf.append("        ?x :P108 ?x_pt .").append("\n");
		buf.append("	    }").append("\n");
		buf.append("	    ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getObjectPropertiesDomainRange(String named_graph) {
		Vector v = executeQuery(construct_get_object_properties_domain_range(named_graph));
		Vector w = new ParserUtils().getResponseValues(v);
		w = new SortUtils().quickSort(w);
		return w;
	}

	public String construct_get_object_valued_annotation_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("        ?x a owl:AnnotationProperty .").append("\n");
		buf.append("        ?x :NHC0 ?x_code .").append("\n");
		buf.append("        ?x rdfs:label ?x_label .").append("\n");
		buf.append("        ?x rdfs:range ?x_range").append("\n");
		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?x_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getObjectValuedAnnotationProperties(String named_graph) {
		Vector v = executeQuery(construct_get_object_valued_annotation_properties(named_graph));
		Vector w = new ParserUtils().getResponseValues(v);
		w = new SortUtils().quickSort(w);
		return w;
	}

/*
	public String construct_get_string_valued_annotation_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("      {").append("\n");
		buf.append("         {").append("\n");
		buf.append("           ?x a owl:AnnotationProperty .").append("\n");
		buf.append("           ?x :NHC0 ?x_code .").append("\n");
		buf.append("           ?x rdfs:label ?x_label .").append("\n");
		buf.append("           ?x rdfs:range ?x_range").append("\n");
		buf.append("         }").append("\n");
		buf.append("        FILTER (str(?x_range)=\"http://www.w3.org/2001/XMLSchema#string\")").append("\n");
		buf.append("      }").append("\n");
		buf.append("  	  UNION").append("\n");
		buf.append("      {").append("\n");
		buf.append("         {").append("\n");
		buf.append("           ?x a owl:AnnotationProperty .").append("\n");
		buf.append("           ?x :NHC0 ?x_code .").append("\n");
		buf.append("           ?x rdfs:label ?x_label .").append("\n");
		buf.append("           ?x rdfs:range ?x_range").append("\n");
		buf.append("         }").append("\n");
		buf.append("        FILTER (str(?x_range)=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#textArea\")").append("\n");
		buf.append("      }").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}
*/

	public String construct_get_string_valued_annotation_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?p_label ?p_code ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("      {").append("\n");
		buf.append("         {").append("\n");
		buf.append("           ?p a owl:AnnotationProperty .").append("\n");
		buf.append("           ?p :NHC0 ?p_code .").append("\n");
		buf.append("           ?p rdfs:label ?p_label .").append("\n");
		buf.append("         }").append("\n");
		buf.append("         MINUS").append("\n");
		buf.append("         {").append("\n");
		buf.append("           ?x ?p ?y .").append("\n");
		buf.append("           ?x a owl:Class .").append("\n");
		buf.append("           ?y a owl:Class .").append("\n");
		buf.append("         }").append("\n");
		buf.append("      }").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getStringValuedAnnotationProperties(String named_graph) {
		return executeQuery(construct_get_string_valued_annotation_properties(named_graph));
	}

	public Vector getSupportedProperties(String named_graph) {
		Vector v = getStringValuedAnnotationProperties(named_graph);
		Vector w = new ParserUtils().getResponseValues(v);
		w = new SortUtils().quickSort(w);
		return w;
	}

	public Vector getSupportedAssociations(String named_graph) {
		return getObjectValuedAnnotationProperties(named_graph);
	}

	public Vector getSupportedRoles(String named_graph) {
		Vector v = getObjectProperties(named_graph);
		Vector w = new ParserUtils().getResponseValues(v);
		w = new SortUtils().quickSort(w);
		return w;
	}

	public String construct_get_hierarchical_relationships(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?z_label ?z_code ?x_label ?x_code").append("\n");
		buf.append("{").append("\n");
		buf.append("  graph <" + named_graph + ">").append("\n");
		buf.append("  {").append("\n");
		buf.append("	  {").append("\n");
		buf.append("		  {").append("\n");
		buf.append("		    ?x a owl:Class .").append("\n");
		buf.append("		    ?x rdfs:label ?x_label .").append("\n");
		buf.append("		    ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("		    ?x rdfs:subClassOf ?z .").append("\n");
		buf.append("		    ?z a owl:Class .").append("\n");
		buf.append("		    ?z rdfs:label ?z_label .").append("\n");
		buf.append("		    ?z " + named_graph_id + " ?z_code").append("\n");
		buf.append("		  }").append("\n");
		buf.append("		  FILTER (?x != ?z)").append("\n");
		buf.append("	  }").append("\n");
		buf.append("  	  UNION").append("\n");
		buf.append("	  {").append("\n");
		buf.append("		  {").append("\n");
		buf.append("		    ?x a owl:Class .").append("\n");
		buf.append("		    ?x rdfs:label ?x_label .").append("\n");
		buf.append("		    ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("		    ?x owl:equivalentClass ?y .").append("\n");
		buf.append("		    ?y owl:intersectionOf ?list .").append("\n");
		buf.append("		    ?list rdf:rest*/rdf:first ?z .").append("\n");
		buf.append("		    ?z a owl:Class .").append("\n");
		buf.append("		    ?z rdfs:label ?z_label .").append("\n");
		buf.append("		    ?z " + named_graph_id + " ?z_code").append("\n");
		buf.append("		  }").append("\n");
		buf.append("		  FILTER (?x != ?z)").append("\n");
		buf.append("	  }").append("\n");
		buf.append("  }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getHierarchicalRelationships(String named_graph) {
		return executeQuery(construct_get_hierarchical_relationships(named_graph));
	}

	public Vector get_concepts_in_subset(String named_graph, String code) {
		Vector v = getConceptsInSubset(named_graph, code, false);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public Vector getConceptsInSubset(String named_graph, String code) {
		return getConceptsInSubset(named_graph, code, false);
	}

	public String construct_get_concepts_in_subset(String named_graph, String subset_code, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT ?x_code").append("\n");
		} else {
			buf.append("SELECT ?x_label ?x_code").append("\n");
	    }
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?z " + named_graph_id + " \"" + subset_code + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + "Concept_In_Subset" + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsInSubset(String named_graph, String code, boolean codeOnly) {
		return executeQuery(construct_get_concepts_in_subset(named_graph, code, codeOnly));
	}


	public String construct_get_subset_membership(String named_graph, String code, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT ?z_code").append("\n");
		} else {
			buf.append("SELECT ?z_label ?z_code").append("\n");
	    }
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?z " + named_graph_id + " ?z_code .").append("\n");
		buf.append("            ?z rdfs:label ?z_label .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + "Concept_In_Subset" + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getSubsetMembership(String named_graph, String code, boolean codeOnly) {
		return executeQuery(construct_get_subset_membership(named_graph, code, codeOnly));
	}


	public String construct_get_associated_concepts(String named_graph, String focus_code, String association, boolean sourceOf) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (sourceOf) {
			buf.append("SELECT distinct ?x_label ?x_code").append("\n");
		} else {
			buf.append("SELECT distinct ?z_label ?z_code").append("\n");
		}
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		if (sourceOf) {
		    buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		} else if (focus_code != null) {
			buf.append("            ?x " + named_graph_id + " \"" + focus_code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");

		if (sourceOf && focus_code != null) {
		    buf.append("            ?z " + named_graph_id + " \"" + focus_code + "\"^^xsd:string .").append("\n");
		} else {
			buf.append("            ?z " + named_graph_id + " ?z_code .").append("\n");
		}
		buf.append("            ?z rdfs:label ?z_label .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + association + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public String construct_get_associated_concepts(String named_graph, String association) {
        return construct_get_associated_concepts(named_graph, association, true);
	}

	public String construct_get_associated_concepts(String named_graph, String association, boolean sourceOf) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (sourceOf) {
			buf.append("SELECT distinct ?x_label ?x_code ?y_label ?z_label ?z_code").append("\n");
		} else {
			buf.append("SELECT distinct ?z_label ?z_code ?y_label ?x_label ?x_code").append("\n");
		}
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("            ?z " + named_graph_id + " ?z_code .").append("\n");
		buf.append("            ?z rdfs:label ?z_label .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + association + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public String construct_get_associated_concepts(String named_graph, String target_code, String association) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?z " + named_graph_id + " \"" + target_code + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + association + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAssociatedConcepts(String named_graph, String association) {
		return executeQuery(construct_get_associated_concepts(named_graph, association));
	}

	public Vector getAssociatedConcepts(String named_graph, String target_code, String association) {
		return executeQuery(construct_get_associated_concepts(named_graph, target_code, association));
	}

	public Vector getAssociatedConcepts(String named_graph, String target_code, String association, boolean sourceOf) {
		return executeQuery(construct_get_associated_concepts(named_graph, target_code, association, sourceOf));
	}

	public String[] get_concept_in_subset_codes(String named_graph, String subset_code) {
		Vector w = getConceptsInSubset(named_graph, subset_code);
		ParserUtils parser = new ParserUtils();
		w = parser.parse(w, 2);
		String[] array = new String[w.size()];
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String code = (String) u.elementAt(1);
			array[i] = code;
		}
		return array;
	}


    public HashMap getNameVersion2NamedGraphMap() {
		HashMap graphName2VersionHashMap = getGraphName2VersionHashMap();

		HashMap nameVersion2NamedGraphMap = new HashMap();
		ParserUtils parserUtils = new ParserUtils();
		//vocabulary name|vocabulary version|graph named
		Vector v = getAvailableOntologies();
		if (v != null && v.size() > 0) {
			for (int j=0; j<v.size(); j++) {
				String t = (String) v.elementAt(j);
				//System.out.println("getNameVersion2NamedGraphMap: " + t);
				String named_graph = parserUtils.getValue(t);
				//System.out.println("getNameVersion2NamedGraphMap named_graph: " + named_graph);
				String uri = getOntologyURI(named_graph);
				//System.out.println("getNameVersion2NamedGraphMap uri: " + uri);
		        String vocabulary = getOntologyName(uri);
		        //System.out.println("getNameVersion2NamedGraphMap vocabulary: " + vocabulary);
		        String version = "null";
		        Vector named_graph_vec = new Vector();
				try {
					if (vocabulary == null) {
						String s = named_graph;
						int n = s.lastIndexOf("#");
						if (n == -1) {
							n = s.lastIndexOf("/");
						}
						vocabulary = s.substring(n+1, s.length());

						//vocabulary = named_graph;
						version = "1.0";
						if (graphName2VersionHashMap.containsKey(named_graph)) {
							version= (String) graphName2VersionHashMap.get(named_graph);
						}
						//System.out.println("WARNING: getOntologyName(" + uri + ") returns null - need to update createOntologyUri2LabelMap method.");
					} else {
                        //System.out.println("vocabulary: " + vocabulary);
						Vector version_vec = getOntologyVersion(named_graph);
						if (version_vec != null && version_vec.size() > 0) {
							String first_line = (String) version_vec.elementAt(0);
							version = parserUtils.getValue(first_line);
						}
					}
					named_graph_vec = new Vector();
					if (nameVersion2NamedGraphMap.containsKey(vocabulary + "|" + version)) {
						named_graph_vec = (Vector) nameVersion2NamedGraphMap.get(vocabulary + "|" + version);
					}
					if (!named_graph_vec.contains(named_graph)) {
						named_graph_vec.add(named_graph);
					}
					nameVersion2NamedGraphMap.put(vocabulary + "|" + version, named_graph_vec);

				} catch (Exception ex) {
					//System.out.println("Exceptions thrown at getNameVersion2NamedGraphMap.");
				}
			}
		} else {
			System.out.println("getAvailableOntologies return null.");
		}
		return nameVersion2NamedGraphMap;
	}

    public String getOntologyURI(String named_graph) {
		Vector v = getOntology(named_graph);
		if (v == null) {
			//System.out.println("WARNING: getOntology " + named_graph + " returns null???");
			return named_graph;
		} else if (v.size() == 0) {
			//System.out.println("WARNING: getOntology " + named_graph + " returns an empty vector???");
			return named_graph;
		}

		String uri = new ParserUtils().getValue((String) v.elementAt(0));
		return uri;
	}

    public String getOntologyName(String uri) {
		return (String) ontologyUri2LabelMap.get(uri);
	}

	public Vector getAvailableOntologies() {
		ParserUtils parser = new ParserUtils();
		Vector v = new Vector();
		Vector namedgraphs = getNamedGraphs();
		if (namedgraphs != null) {
			for (int i=0; i<namedgraphs.size(); i++) {
				String named_graph = parser.getValue((String) namedgraphs.elementAt(i));
				//System.out.println("\t\t" + named_graph);
				Vector u1 = getOntology(named_graph);

				String ontology_name_line = named_graph;
				String ontology_name = named_graph;
				if (u1 != null && u1.size() > 0) {
					ontology_name_line = (String) u1.elementAt(0);
					ontology_name = parser.getValue(ontology_name_line);
				}
				Vector u2 = null;
				try {
					u2 = getOntologyVersion(named_graph);
				} catch (Exception ex) {
					//System.out.println("WARNING: Version not found -- " + named_graph);
				}

				if (u2 != null && u2.size() > 0) {
					v.add(ontology_name + "|" + parser.getValue((String) u2.elementAt(0))
					   + "|" + named_graph);
				} else {
					v.add(ontology_name + "|null" + "|" + named_graph);
				}
			}
		} else {
			System.out.println("getNamedGraphs returns null.");
		}
        return v;
	}

	public Vector getOntologyVersion(String named_graph) {
		for (int i=0; i<Constants.VERSION_PREDICATE.length; i++) {
			String predicate = Constants.VERSION_PREDICATE[i];
			String query = construct_get_ontology_version(named_graph, predicate);
			try {
				Vector v = executeQuery(query);
				if (v != null && v.size() > 0) return v;
			} catch (Exception ex) {

			}
		}
		return null;
	}

	public String construct_get_ontology(String named_graph) {
		//String prefixes = PrefixUtils.getPrefixes(null);
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		//buf.append(prefixes);
		buf.append("SELECT distinct ?x").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Ontology ").append("\n");
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getOntology(String named_graph) {
		return executeQuery(construct_get_ontology(named_graph));
	}


	public Vector getNamedGraphs() {
		String query = construct_get_named_graphs();
		return executeQuery(query);
	}

	public String construct_get_ontology_version(String named_graph, String predicate) {
		Vector v = getOntology(named_graph);
		String ontology_uri = parser.getValue((String) v.elementAt(0));
		String prefixes = PrefixUtils.getPrefixes(ontology_uri);

		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_version").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Ontology . ").append("\n");
		buf.append("		?x " + predicate + " ?x_version ").append("\n");
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


////////////////////////////////////////////////////////////////////////////////////////////////////////
// Utilities
////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Vector getTree(String named_graph, String code) {
		return getTree(named_graph, code, -1);
	}

	public Vector getTree(String named_graph, String code, int maxLevel) {
		ParserUtils parser = new ParserUtils();
		if (maxLevel == -1) maxLevel = 1000;
		Stack stack = new Stack();
		Vector v = getLabelByCode(named_graph, code);
		String line = (String) v.elementAt(0);
		String label = parser.getValue(line);

		stack.push(label + "|" + code + "|0");
		Vector w = new Vector();

		while (!stack.isEmpty()) {
			line = (String) stack.pop();
			Vector v2 = StringUtils.parseData(line, '|');
			String next_label = (String) v2.elementAt(0);
			String next_code = (String) v2.elementAt(1);
			String level_str = (String) v2.elementAt(2);
			int level = Integer.parseInt(level_str);
			if (level == maxLevel) break;
			int nextLevel = level + 1;
			Vector u = getSubclassesByCode(named_graph, next_code);
			Vector v3 = new Vector();
			int n = u.size()/2;
			for (int i=0; i<n; i++) {
				String s1 = (String) u.elementAt(i*2);
				String s2 = (String) u.elementAt(i*2+1);
				String t1 = parser.getValue(s1);
				String t2 = parser.getValue(s2);
				v3.add(next_label + "|" + next_code + "|" + t1 + "|" + t2);
				stack.push(t1 + "|" + t2 + "|" + nextLevel);
			}
			w.addAll(v3);
		}
		return w;
	}

	public boolean isFlatFormat(String named_graph) {
		Vector v = executeQuery(construct_get_restrictions(named_graph));
		if (v != null && v.size() > 0) return false;
		return true;
	}

	public String construct_get_restrictions(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x").append("\n");
		buf.append("{").append("\n");
		buf.append("  graph <" + named_graph + ">").append("\n");
		buf.append("  {").append("\n");
        buf.append("		?x a owl:Restriction").append("\n");
        buf.append("  }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getAssociations(String named_graph) {
		Vector v = executeQuery(construct_get_all_associations(named_graph));
		return v;
	}

	public String construct_get_all_associations(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes).append("\n");
		buf.append("SELECT ?x_code ?p_code ?y_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("                ?x a owl:Class .").append("\n");
		buf.append("                ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("                ?x ?p ?y .").append("\n");
		buf.append("                ?p " + named_graph_id + " ?p_code .").append("\n");
		buf.append("                ?y a owl:Class .").append("\n");
		buf.append("                ?y " + named_graph_id + " ?y_code").append("\n");
		buf.append("    }").append("\n");
		buf.append("}");
		return buf.toString();
	}


    public Vector filter_property_values(Vector v, String propertyName) {
		if (v == null || propertyName == null) return null;
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String propName = (String) u.elementAt(1);
			if (propName.compareTo(propertyName) == 0) {
				String propValue = (String) u.elementAt(2);
				if (!w.contains(propValue)) {
					w.add(propValue);
				}
			}
		}
		return w;
	}

	public Vector getValueSetURIsByMemberConceptCode(String named_graph, String code) {
		Vector v = getAssociationsByCode(named_graph, code);
		Vector uris = new Vector();
		ParserUtils parser = new ParserUtils();
		v = parser.getResponseValues(v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			String vs_code = parser.getValue(line);
			Vector w = getPropertiesByCode(named_graph, vs_code);
			w = new ParserUtils().getResponseValues(w);
			w = filter_property_values(w, "Contributing_Source");
			for (int k=0; k<w.size(); k++) {
				String contributing_source = (String) w.elementAt(k);
				String uri = Constants.VALUE_SET_URI_PREFIX + contributing_source + "/" + vs_code;
				if (!uris.contains(uri)) {
					uris.add(uri);
				}
			}
		}
		return uris;
	}

    public String getStringValuedVariableExpression(String varName) {
		return "\"" + varName + "\"^^<http://www.w3.org/2001/XMLSchema#string";
	}

	public String construct_get_concepts_with_annotation_property(String named_graph, String propertyName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + propertyName + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsWithAnnotationProperty(String named_graph, String propertyName) {
		String query = construct_get_concepts_with_annotation_property(named_graph, propertyName);
		Vector v = executeQuery(query);
		return v;
	}

	public Vector getValueSetHeaderConcepts(String named_graph) {
		return getConceptsWithAnnotationProperty(named_graph, "Publish_Value_Set");
	}

	public String construct_get_association_sources_and_targets(String named_graph, String association_name) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?z a owl:Class .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("            ?z rdfs:label ?z_label .").append("\n");
		buf.append("            ?z " + named_graph_id + " ?z_code .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + association_name + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAssociationSourcesAndTargets(String named_graph, String association_name) {
		return executeQuery(construct_get_association_sources_and_targets(named_graph, association_name));
	}

	public Vector getPermissibleValues(String inputfile) {
		Vector w = Utils.readFile(inputfile);
		return getPermissibleValues(w);
	}

	public Vector getPermissibleValues(Vector w) {
		//Vector w = Utils.readFile(inputfile);
        Vector v = new Vector();
        ParserUtils parser = new ParserUtils();
        for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			String value = parser.getValue(line);
            if (!v.contains(value)) {
				v.add(value);
			}
		}
        return new SortUtils().quickSort(v);
	}

	public HashMap getValueSetMembershipHashMap(String inputfile) {
		Vector w = Utils.readFile(inputfile);
		return getValueSetMembershipHashMap(w);
	}

	public HashMap getValueSetMembershipHashMap(Vector w) {
		//Vector w = Utils.readFile(inputfile);
		HashMap hmap = new HashMap();
		SortUtils utils = new SortUtils();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
            Vector u = StringUtils.parseData(line, '|');
            String label = (String) u.elementAt(0);
            String code = (String) u.elementAt(1);
            String vs_lable = (String) u.elementAt(3);
            String vs_code = (String) u.elementAt(4);
            Vector v = new Vector();
            if (hmap.containsKey(vs_code)) {
				v = (Vector) hmap.get(vs_code);
			}
			if (!v.contains(label + "|" + code)) {
				v.add(label + "|" + code);
			}
			hmap.put(code, v);
		}
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String code = (String) it.next();
			Vector v = (Vector) hmap.get(code);
			v = utils.quickSort(v);
			hmap.put(code, v);
		}
        return hmap;
	}

	public Vector getValueSetURIsByHeaderConceptCode(String named_graph, String vs_code) {
		Vector w = getPropertiesByCode(named_graph, vs_code);
		Vector uris = new Vector();
		w = new ParserUtils().getResponseValues(w);
		w = filter_property_values(w, "Contributing_Source");
		for (int k=0; k<w.size(); k++) {
			String contributing_source = (String) w.elementAt(k);
			String uri = Constants.VALUE_SET_URI_PREFIX + contributing_source + "/" + vs_code;
			if (!uris.contains(uri)) {
				uris.add(uri);
			}
		}
		return uris;
	}

    public Vector headerConceptCodes2ValueSetURIs(String named_graph, Vector v) {
        Vector vs_vec = new Vector();
        try {
			for (int k=0; k<v.size(); k++) {
				String vs_code = (String) v.elementAt(k);
				Vector w = getValueSetURIsByHeaderConceptCode(named_graph, vs_code);
				if (w != null && w.size() > 0) {
					vs_vec.addAll(w);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		vs_vec = new SortUtils().quickSort(vs_vec);
		return vs_vec;
	}

    public HashMap createHeaderConceptCode2ValueSetURIHashMap(String named_graph, Vector vs_code_vec) {
		if (vs_code_vec == null) return null;
        HashMap hmap = new HashMap();
        SortUtils utils = new SortUtils();
        try {
			for (int k=0; k<vs_code_vec.size(); k++) {
				String vs_code = (String) vs_code_vec.elementAt(k);
				Vector w = getValueSetURIsByHeaderConceptCode(named_graph, vs_code);
				if (w != null && w.size() > 0) {
					w = utils.quickSort(w);
					hmap.put(vs_code, w);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return hmap;
	}

	public HashMap createValueSetMembershipHashMap(Vector concept_in_subset_vec) {
		if (concept_in_subset_vec == null) return null;
		HashMap hmap = new HashMap();
		for (int k=0; k<concept_in_subset_vec.size(); k++) {
			String line = (String) concept_in_subset_vec.elementAt(k);
			Vector u = StringUtils.parseData(line, '|');
			//Knee Joint|C32898|Concept_In_Subset|CDISC SEND Terminology|C77526
			String member_code = (String) u.elementAt(1);
			String vs_code = (String) u.elementAt(4);
			Vector w = new Vector();
			if (hmap.containsKey(member_code)) {
				w = (Vector) hmap.get(member_code);
			}
			if (!w.contains(vs_code)) {
				w.add(vs_code);
			}
			hmap.put(member_code, w);
		}
		return hmap;
	}

	public String construct_get_valueset_metadata(String named_graph, String vs_code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?y_value").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x :NHC0 \"" + vs_code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("            ?x ?y ?y_value .").append("\n");
		buf.append("            ?y rdfs:label \"Contributing_Source\"^^<http://www.w3.org/2001/XMLSchema#string> ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getValueSetMetadata(String named_graph, String vs_code) {
		String query = construct_get_valueset_metadata(named_graph, vs_code);
		//System.out.println(query);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public gov.nih.nci.evs.restapi.bean.ValueSetDefinition createValueSetDefinition(String named_graph, String vs_code) {
		Vector v = getValueSetMetadata(named_graph, vs_code);
		return createValueSetDefinition(v);
	}

	public String getValueSetURIByHeaderConceptCode(String vs_code, String contributing_source) {
		String uri = Constants.VALUE_SET_URI_PREFIX + contributing_source + "/" + vs_code;
		return uri;
	}

	public gov.nih.nci.evs.restapi.bean.ValueSetDefinition createValueSetDefinition(Vector v) {
		String uri = null;
		String defaultCodingScheme = "NCI_Thesaurus";
		String conceptDomain = "Intellectual Property";
		String name = null;
		String code = null;
		//(1) SPL Color Terminology|C54453|Contributing_Source|FDA
        List sources = new ArrayList();
        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			name = (String) u.elementAt(0);
			code = (String) u.elementAt(1);
			String source =  (String) u.elementAt(3);
			if (!sources.contains(source)) {
				sources.add(source);
			}
		}

		String contributing_source = null;
		if (sources.size() > 0) {
			contributing_source = (String) sources.get(0);
		}

		uri = getValueSetURIByHeaderConceptCode(code, contributing_source);
		gov.nih.nci.evs.restapi.bean.ValueSetDefinition vsd = new gov.nih.nci.evs.restapi.bean.ValueSetDefinition(
			uri,
			defaultCodingScheme,
			conceptDomain,
			name,
			code,
			sources);

        return vsd;
	}

	public HashMap getPropertyHashMapByCode(Vector v) {
		HashMap hmap = new HashMap();
		SortUtils sortUtils = new SortUtils();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String propertyName = (String) u.elementAt(1);
			String propertyValue = (String) u.elementAt(2);
			Vector w = new Vector();
			if (hmap.containsKey(propertyName)) {
				w = (Vector) hmap.get(propertyName);
			}
			if (!w.contains(propertyValue)) {
				w.add(propertyValue);
				w = sortUtils.quickSort(w);
			}
			hmap.put(propertyName, w);
		}
		return hmap;
	}

	public HashMap getPropertyHashMapByCode(String named_graph, String code) {
		Vector v = getPropertiesByCode(named_graph, code);
		if (v == null) {
			return null;
		}
		v = new ParserUtils().getResponseValues(v);
		HashMap hmap = new HashMap();
		SortUtils sortUtils = new SortUtils();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String propertyName = (String) u.elementAt(1);
			String propertyValue = (String) u.elementAt(2);
			Vector w = new Vector();
			if (hmap.containsKey(propertyName)) {
				w = (Vector) hmap.get(propertyName);
			}
			if (!w.contains(propertyValue)) {
				w.add(propertyValue);
				w = sortUtils.quickSort(w);
			}
			hmap.put(propertyName, w);
		}
		return hmap;
	}

    public String getEntityDescriptionByCode(String named_graph, String code) {
		Vector v = getLabelByCode(named_graph, code);
		if (v == null || v.size() == 0) {
			return null;
		}
		v = new ParserUtils().getResponseValues(v);
		return (String) v.elementAt(0);
	}

////////////////////////////////////////////////////////////////////
	public Vector getSuperclassesByCode(String code) {
		Vector v = getSuperclassesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public Vector getSubclassesByCode(String code) {
		Vector v = getSubclassesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public Vector getOutboundRolesByCode(String code) {
		Vector v = getOutboundRolesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public Vector getInboundRolesByCode(String code) {
		Vector v = getInboundRolesByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public Vector getAssociationsByCode(String code) {
		Vector v = getAssociationsByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public Vector getInverseAssociationsByCode(String code) {
		Vector v = getInverseAssociationsByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_get_property_qualifier_values(String named_graph, String property_name, Vector inclusions, Vector exclusions) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?p_label ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label \"" + property_name + "\"^^xsd:string .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?axiom_target .").append("\n");
		buf.append("            ?z_axiom ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label").append("\n");
		buf.append("    }").append("\n");
		if (inclusions != null) {
			for (int i=0; i<inclusions.size(); i++) {
				String inclusion = (String) inclusions.elementAt(i);
				buf.append("    FILTER (str(?y_label) = \"" + inclusion + "\")").append("\n");
			}
		}
		if (exclusions != null) {
			for (int i=0; i<exclusions.size(); i++) {
				String exclusion = (String) exclusions.elementAt(i);
				buf.append("    FILTER (str(?y_label) != \"" + exclusion + "\")").append("\n");
			}
		}
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getPropertyQualifierValues(String named_graph, String property_name, Vector inclusions, Vector exclusions) {
	    String query = construct_get_property_qualifier_values(named_graph, property_name, inclusions, exclusions);
	    return executeQuery(query);
	}

	public String construct_get_predicate_labels(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?y ?y_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getPredicateLabels(String named_graph) {
	    String query = construct_get_predicate_labels(named_graph);
	    return executeQuery(query);
	}



	public String construct_get_properties_by_code(String named_graph, String code, boolean use_filter) {
		if (use_filter) {
			return construct_get_properties_by_code(named_graph, code);
		}
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?y ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("	graph <" + named_graph + "> {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x ?y ?z .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getPropertiesByCode(String named_graph, String code, boolean use_filter) {
	    String query = construct_get_properties_by_code(named_graph, code, use_filter);
	    return executeQuery(query);
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
	    String query = construct_get_owl_class_data(named_graph, identifier, code, ns, by_code);
	    return executeQuery(query);
	}

	public String generate_get_property_values_by_code(String named_graph, String code, String property_name) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label . ").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x :NHC0 \"" + code + "\"^^xsd:string .  ").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("?y rdfs:label \"" + property_name + "\"^^xsd:string  ").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getPropertyValuesByCode(String named_graph, String code, String property_name) {
	    String query = generate_get_property_values_by_code(named_graph, code, property_name);
	    return executeQuery(query);
	}


	public String generate_get_property_values(String named_graph, String property_name) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label . ").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("?y rdfs:label \"" + property_name + "\"^^xsd:string  ").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getPropertyValues(String named_graph, String property_name) {
	    String query = generate_get_property_values(named_graph, property_name);
	    return executeQuery(query);
	}

/*
	public String construct_get_axioms_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?z ?w_label ?w_value").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?y a owl:AnnotationProperty .").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("?z_axiom a owl:Axiom .").append("\n");
		buf.append("?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?z_axiom owl:annotatedTarget ?z .").append("\n");
		buf.append("?w rdfs:label ?w_label .").append("\n");
		buf.append("?z_axiom ?w ?w_value   ").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAxiomsByCode(String named_graph, String code) {
	    String query = construct_get_axioms_by_code(named_graph, code);
	    return executeQuery(query);
	}
*/

	public String construct_get_owl_class_data(String named_graph, String uri) {
		StringBuffer buf = new StringBuffer();
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT ?x_label ?y_label ?z ").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		//buf.append("?x " + identifier + " ?x_code .").append("\n");
		buf.append("}").append("\n");
		buf.append("FILTER (str(?x) = \"" + uri + "\"^^xsd:string)").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getOWLClassData(String named_graph, String uri) {
	    String query = construct_get_owl_class_data(named_graph, uri);
	    Vector v = executeQuery(query);
	    v = new ParserUtils().getResponseValues(v);
	    return v;
	}

////////////////////////////////////////////////////////////////////
	public String construct_get_label_by_uri(String named_graph, String uri) {
		StringBuffer buf = new StringBuffer();
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT ?x_label").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("}").append("\n");
		buf.append("FILTER (str(?x) = \"" + uri + "\"^^xsd:string)").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public String getLabel(String code) {
		return getLabel(this.named_graph, code);
	}

	public String getLabel(String named_graph, String code) {
		Vector labels = getLabelByCode(named_graph, code);
		if (labels == null || labels.size() == 0) return null;
		labels = new ParserUtils().getResponseValues(labels);
		String label = (String) labels.elementAt(0);
		return label;
	}

	public String getLabelByURI(String named_graph, String uri) {
	    String query = construct_get_label_by_uri(named_graph, uri);
	    Vector v = executeQuery(query);
	    v = new ParserUtils().getResponseValues(v);
	    return (String) v.elementAt(0);
	}

	public HashMap getStringValuedProperties(String named_graph, String uri_base, Vector data_vec) {
		if (data_vec == null) return null;
		HashMap hmap = new HashMap();
		for (int i=0; i<data_vec.size(); i++) {
			String line = (String) data_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String value = (String) u.elementAt(2);
			if (value.indexOf(uri_base) == -1) {
				String predicate_label = (String) u.elementAt(1);
				Vector v = new Vector();
				if (hmap.containsKey(predicate_label)) {
					v = (Vector) hmap.get(predicate_label);
				}
				if (!v.contains(value)) {
					v.add(value);
				}
				hmap.put(predicate_label, v);
			}
		}
		return hmap;
	}

    public String uri2Code(String uri) {
		int n = uri.lastIndexOf("#");
		if (n == -1) {
			n = uri.lastIndexOf("/");
		}
		return uri.substring(n+1, uri.length());
	}

	public HashMap getObjectValuedProperties(String named_graph, String uri_base, Vector data_vec) {
		if (data_vec == null) return null;
		HashMap hmap = new HashMap();
		//String uri_base = mdu.getURIBase(named_graph);
		for (int i=0; i<data_vec.size(); i++) {
			String line = (String) data_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String value = (String) u.elementAt(2);
			if (value.indexOf(uri_base) != -1) {
				String predicate_label = (String) u.elementAt(1);
				String label = getLabelByURI(named_graph, value);
				String code = uri2Code(value);
				String t = label + "|" + code;
				Vector v = new Vector();
				if (hmap.containsKey(predicate_label)) {
					v = (Vector) hmap.get(predicate_label);
				}
				if (!v.contains(t)) {
					v.add(t);
				}
				hmap.put(predicate_label, v);
			}
		}
		return hmap;
	}

	public String construct_get_inverse_relationships(String named_graph, String uri) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("SELECT ?x_label ?x ?y_label ?y").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("}").append("\n");
		buf.append("FILTER (str(?z) = \"" + uri + "\"^^xsd:string)").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getInverseRelationships(String named_graph, String uri) {
	    String query = construct_get_inverse_relationships(named_graph, uri);
	    Vector v = executeQuery(query);
	    v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String getMultipleValues(Vector w) {
		if (w == null) return null;
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			buf.append(t);
			if (i < w.size()-1) {
				buf.append("$");
			}
		}
		return buf.toString();
	}

    // v contains name|code
    // property_name example: Semantic_Type
	public Vector append_property_values(String named_graph, Vector v, String property_name) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			Vector w2 = getPropertyValuesByCode(named_graph, code, property_name);
			if (w2 != null && w2.size() > 0) {
				w2 = new ParserUtils().getResponseValues(w2);
			    Vector property_values = new Vector();
			    for (int k=0; k<w2.size(); k++) {
					String line2 = (String) w2.elementAt(k);
					Vector u2 = StringUtils.parseData(line2, '|');
					String value = (String) u2.elementAt(3);
					property_values.add(value);
				}
				if (property_values != null && property_values.size() > 0) {
					String property_value = (String) property_values.elementAt(0);
					w.add(line + "|" + property_value);
				} else {
					String values = getMultipleValues(property_values);
					w.add(line + "|" + values);
				}
			}
		}
        w = new SortUtils().quickSort(w);
        return w;
	}

	public String construct_get_sample_owlclass(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?p ?y ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("		?x ?p ?y ").append("\n");
		buf.append("	   }").append("\n");
        buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector getSampleOWLClass(String named_graph, String code) {
		String query = construct_get_sample_owlclass(named_graph, code);
		//System.out.println(query);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		return new ParserUtils().getResponseValues(v);
	}

	public String construct_get_simple_restrictions(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		//buf.append("SELECT ?x ?p ?y ").append("\n");
		buf.append("SELECT ?x_code ?p_code ?y_code ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		//buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x ?p ?y .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		//buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		//buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		FILTER regex(?p_code, '^r', 'i')").append("\n");
        buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		//buf.append("LIMIT 1000").append("\n");
		return buf.toString();
	}

	public Vector getSimpleRestrictions(String named_graph) {
		String query = construct_get_simple_restrictions(named_graph);
		//System.out.println(query);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		return new ParserUtils().getResponseValues(v);
	}


	public String construct_get_roles_1(String named_graph, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("		?z2 a owl:Restriction .").append("\n");
		buf.append("		?z2 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}


	public String construct_get_roles_2(String named_graph, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");

		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x rdfs:subClassOf ?r .").append("\n");
		buf.append("		?r a owl:Restriction .").append("\n");
		buf.append("		?r owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?r owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y rdfs:label ?y_label .").append("\n");
		buf.append("		?y :NHC0 ?y_code").append("\n");
		buf.append("	   }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public String construct_get_roles_3(String named_graph, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");

		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");

		buf.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("		?z2 a owl:Restriction .").append("\n");
		buf.append("		?z2 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?p :NHC0 ?p_code .").append("\n");
		buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public String construct_get_roles_4(String named_graph, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");

		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");

		buf.append("		?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");
		buf.append("		?z0 owl:intersectionOf ?list1 .").append("\n");

		buf.append("			?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("			     ?z2 owl:unionOf ?list2 .").append("\n");
		buf.append("			     ?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("				 ?z3 owl:intersectionOf ?list3 .").append("\n");
		buf.append("				 ?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		buf.append("					?z4 a owl:Restriction .").append("\n");
		buf.append("					?z4 owl:onProperty ?p .").append("\n");
		buf.append("					?p rdfs:label ?p_label .").append("\n");
		buf.append("					?p :NHC0 ?p_code .").append("\n");
		buf.append("					?z4 owl:someValuesFrom ?y .").append("\n");
		buf.append("					?y :NHC0 ?y_code .").append("\n");
		buf.append("					?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public String construct_get_roles_5(String named_graph, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_code ?p_code ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");

		buf.append("    {").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");

		buf.append("		?x owl:unionOf ?z0 .").append("\n");
		buf.append("		?z0 a owl:Class .").append("\n");

		buf.append("		?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("		?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("		?z2 a owl:Restriction .").append("\n");
		buf.append("		?z2 owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
//		buf.append("		?y :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
        buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");

		buf.append("	   }").append("\n");
		buf.append("    }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}


	public Vector getRestrictions(String named_graph) {
		return getRestrictions(named_graph, false);
	}

	public Vector getRestrictions(String named_graph, boolean codeOnly) {
		Vector w = new Vector();
		String query = construct_get_roles_1(named_graph, codeOnly);
		//System.out.println(query);
		Vector w1 = executeQuery(query);
		if (w1 != null && w1.size() > 0) {
			w1 = new ParserUtils().getResponseValues(w1);
			w.addAll(w1);
		}
		query = construct_get_roles_2(named_graph, codeOnly);
		//System.out.println(query);
		w1 = executeQuery(query);
		if (w1 != null && w1.size() > 0) {
			w1 = new ParserUtils().getResponseValues(w1);
			w.addAll(w1);
		}
		query = construct_get_roles_3(named_graph, codeOnly);
		//System.out.println(query);
		w1 = executeQuery(query);
		if (w1 != null && w1.size() > 0) {
			w1 = new ParserUtils().getResponseValues(w1);
			w.addAll(w1);
		}
		query = construct_get_roles_4(named_graph, codeOnly);
		//System.out.println(query);
		w1 = executeQuery(query);
		if (w1 != null && w1.size() > 0) {
			w1 = new ParserUtils().getResponseValues(w1);
			w.addAll(w1);
		}
		if (w.size() == 0) {
			w = getSimpleRestrictions(named_graph);
		}
		w = new SortUtils().quickSort(w);
		return w;
	}
    public String construct_get_domain_and_range_data(String name_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?x_label ?x_code ?x_domain_label ?x_domain_code ?x_range_label ?x_range_code ").append("\n");
        buf.append("{ ").append("\n");
        buf.append("graph <" + name_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("{").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("?x :NHC0 ?x_code .").append("\n");
        buf.append("?x rdfs:domain ?x_domain .").append("\n");
        buf.append("?x rdfs:range ?x_range .").append("\n");
        buf.append("?x_domain rdfs:label ?x_domain_label .").append("\n");
        buf.append("?x_domain :NHC0 ?x_domain_code .            ").append("\n");
        buf.append("?x_range rdfs:label ?x_range_label .").append("\n");
        buf.append("?x_range :NHC0 ?x_range_code                ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("} ").append("\n");
        buf.append("").append("\n");
        return buf.toString();
    }

	public Vector getDomainAndRangeData(String named_graph) {
	    String query = construct_get_domain_and_range_data(named_graph);
	    Vector v = executeQuery(query);
	    if (v == null) return null;
	    if (v.size() == 0) return v;
	    v = new ParserUtils().getResponseValues(v);
	    return new SortUtils().quickSort(v);
	}

	public Vector<ComplexProperty> getComplexProperties(String named_graph, String code) {
		Vector w = new Vector();
		Vector v = getPropertyQualifiersByCode(named_graph, code);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		List list = new ArrayList();
		String prev_axiom_id = "";
		String axiom_id = null;
		int i = 0;
		ComplexProperty prop = null;
		while (i<v.size()) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String z_axiom = (String) u.elementAt(0);
			String x_label = (String) u.elementAt(1);
			String x_code = (String) u.elementAt(2);
			String p_label = (String) u.elementAt(3);
			String p_code = (String) u.elementAt(4);
			String z_target = (String) u.elementAt(5);
			String y_label = (String) u.elementAt(6);
			String y_code = (String) u.elementAt(7);
			String z = (String) u.elementAt(8);

			axiom_id = z_axiom;
			if (axiom_id.compareTo(prev_axiom_id) != 0) {
				if (prop != null) {
					prop.setQualifiers(list);
					w.add(prop);
				}
				list = new ArrayList();
				prop = new ComplexProperty(p_label, z_target, null);
				prev_axiom_id = axiom_id;
				PropertyQualifier qual = new PropertyQualifier(y_label, z);
				list.add(qual);
			} else {
				PropertyQualifier qual = new PropertyQualifier(y_label, z);
				list.add(qual);
			}
			i++;
		}
		if (prop != null) {
			prop.setQualifiers(list);
			w.add(prop);
		}
		return w;
	}


	public String construct_simple_query(String named_graph, String code) {
			String prefixes = getPrefixes();
			StringBuffer buf = new StringBuffer();
			buf.append(prefixes);
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?y").append("\n");
			buf.append("{ ").append("\n");
			buf.append("graph <" + named_graph + ">").append("\n");
			buf.append("{").append("\n");
			buf.append("{").append("\n");
			buf.append("?x a owl:Class .").append("\n");
			buf.append("?x :NHC0 ?x_code .").append("\n");
			buf.append("?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			buf.append("?x rdfs:label ?x_label .").append("\n");
			buf.append("?x ?p ?y .").append("\n");
			buf.append("?p :NHC0 ?p_code .").append("\n");
			buf.append("?p rdfs:label ?p_label ").append("\n");
			buf.append("}").append("\n");
			buf.append("}").append("\n");
			buf.append("} ").append("\n");
			return buf.toString();
	}

	public Vector executeSimpleQuery(String named_graph, String code) {
	    String query = construct_simple_query(named_graph, code);
	    Vector v = executeQuery(query);
	    if (v == null) return null;
	    if (v.size() == 0) return v;
	    v = new ParserUtils().getResponseValues(v);
	    return new SortUtils().quickSort(v);
	}


	public String construct_inverse_simple_query(String named_graph, String code) {
			String prefixes = getPrefixes();
			StringBuffer buf = new StringBuffer();
			buf.append(prefixes);
			buf.append("SELECT distinct ?y_label ?y_code ?p_label ?p_code ?x_label ?x_code").append("\n");
			buf.append("{ ").append("\n");
			buf.append("graph <" + named_graph + ">").append("\n");
			buf.append("{").append("\n");
			buf.append("{").append("\n");
			buf.append("?x a owl:Class .").append("\n");
			buf.append("?x :NHC0 ?x_code .").append("\n");
			buf.append("?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			buf.append("?x rdfs:label ?x_label .").append("\n");
			buf.append("?y ?p ?x .").append("\n");
			buf.append("?y :NHC0 ?y_code .").append("\n");
			buf.append("?y rdfs:label ?y_label .").append("\n");
			buf.append("?p :NHC0 ?p_code .").append("\n");
			buf.append("?p rdfs:label ?p_label ").append("\n");
			buf.append("}").append("\n");
			buf.append("}").append("\n");
			buf.append("} ").append("\n");
			return buf.toString();
	}

	public Vector executeInverseSimpleQuery(String named_graph, String code) {
	    String query = construct_inverse_simple_query(named_graph, code);
	    Vector v = executeQuery(query);
	    if (v == null) return null;
	    if (v.size() == 0) return v;
	    v = new ParserUtils().getResponseValues(v);
	    return new SortUtils().quickSort(v);
	}

	public String construct_role_query(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?y_label ?y_code").append("\n");
		buf.append("{ ").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x ?p ?y .").append("\n");
		buf.append("?p :NHC0 ?p_code .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?y a owl:Class .").append("\n");
		buf.append("?y :NHC0 ?y_code .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("}").append("\n");
		buf.append("FILTER regex(?p_code, '^r', 'i')").append("\n");
		buf.append("}").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector getRoles(String named_graph) {
		return getRelationships(named_graph, TYPE_ROLE);
	}

	public static int TYPE_ROLE = 1;
	public static int TYPE_ASSOCIATION = 2;

	public String construct_relationship_query(String named_graph, int type) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?y_label ?y_code").append("\n");
		buf.append("{ ").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x ?p ?y .").append("\n");
		buf.append("?p :NHC0 ?p_code .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?y a owl:Class .").append("\n");
		buf.append("?y :NHC0 ?y_code .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("}").append("\n");
		if (type == TYPE_ROLE) {
			buf.append("FILTER regex(?p_code, '^r', 'i')").append("\n");
		} else {
			buf.append("FILTER regex(?p_code, '^a', 'i')").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector getRelationships(String named_graph, int type) {
	    String query = construct_relationship_query(named_graph, type);
	    Vector v = executeQuery(query);
	    if (v == null) return null;
	    if (v.size() == 0) return v;
	    v = new ParserUtils().getResponseValues(v);
	    return new SortUtils().quickSort(v);
	}
	public String construct_source_code_query(String named_graph, String source_code) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?x_label ?x_code").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + "> {").append("\n");
        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x :NHC0 ?x_code .").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("?z_axiom owl:annotatedSource ?x .").append("\n");
        buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
        buf.append("?p rdfs:label ?p_label .").append("\n");
        buf.append("?p rdfs:label \"FULL_SYN\"^^xsd:string .").append("\n");
        buf.append("?z_axiom owl:annotatedTarget ?axiom_target .").append("\n");
        buf.append("?y rdfs:label ?y_label .").append("\n");
        buf.append("?y rdfs:label \"Source Code\"^^xsd:string .").append("\n");
        buf.append("?z_axiom ?y \"" + source_code + "\"^^xsd:string ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector source_code_query(String named_graph, String source_code) {
		String query = construct_source_code_query(named_graph, source_code);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}


	public String construct_axiom_type_query(String named_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?p_label ?y_label ").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + "> {").append("\n");
        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x :NHC0 ?x_code .").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("?z_axiom owl:annotatedSource ?x .").append("\n");
        buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
        buf.append("?p rdfs:label ?p_label .").append("\n");
        buf.append("?z_axiom owl:annotatedTarget ?axiom_target .").append("\n");
        buf.append("?z_axiom ?y ?z .").append("\n");
        buf.append("?y rdfs:label ?y_label ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector axiom_type_query(String named_graph) {
		String query = construct_axiom_type_query(named_graph);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_axioms_by_code_query(String named_graph, String code) {
		return construct_axioms_by_code_query(named_graph, code, null);
	}


	public String construct_axioms_by_code_query(String named_graph, String code, String propertyName) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?x_label ?x_code ?z_axiom ?p_label ?axiom_target ?y_label ?z").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + "> {").append("\n");
        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x :NHC0 ?x_code .").append("\n");
        buf.append("?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("?z_axiom owl:annotatedSource ?x .").append("\n");
        buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
        buf.append("?p rdfs:label ?p_label .").append("\n");
        if (propertyName != null) {
			buf.append("?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
		}
        buf.append("?z_axiom owl:annotatedTarget ?axiom_target .").append("\n");
        buf.append("?z_axiom ?y ?z .").append("\n");
        buf.append("?y rdfs:label ?y_label ").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector axioms_by_code_query(String named_graph, String code) {
		return axioms_by_code_query(named_graph, code, null);
	}

	public Vector axioms_by_code_query(String named_graph, String code, String propertyName) {
		String query = construct_axioms_by_code_query(named_graph, code, propertyName);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

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
        buf.append("{").append("\n");
        buf.append("{").append("\n");
        buf.append("?x ?p ?y .").append("\n");
        buf.append("FILTER (contains(str(?y), \"" + code + "\") || contains(str(?x), \"" + code + "\"))").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("} ").append("\n");
        return buf.toString();
	}

	public Vector mapping_query(String named_graph, String code) {
		String query = construct_mapping_query(named_graph, code);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			Vector w = new Vector();
			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				String s = (String) u.elementAt(0);
				String o = (String) u.elementAt(2);
				if (s.endsWith(code) || o.endsWith(code)) {
					w.add(line);
				}
			}
			w = new SortUtils().quickSort(w);
			return w;
		}
		return null;
	}


	public String construct_mapping_query(String named_graph) {
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
        buf.append("?x ?p ?y .").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("} ").append("\n");
        return buf.toString();
	}

	public Vector mapping_query(String named_graph) {
		String query = construct_mapping_query(named_graph);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public static Vector formatOutput(Vector v) {
		return ParserUtils.formatOutput(v);
	}

    public void runQuery(String query_file) {
		/*
		int n = query_file.indexOf(".");
		String method_name = query_file.substring(0, n);
		String params = "String named_graph, String code";
        */
		String query = getQuery(query_file);
		System.out.println(query);
        /*
	    Utils.generate_construct_statement(method_name, params, query_file);

		String json = getJSONResponseString(query);
		System.out.println(json);
		*/
        Vector v = execute(query_file);
        v = formatOutput(v);
        //StringUtils.dumpVector("v", v);
        Utils.saveToFile("output_" + query_file, v);

	}

	public String construct_outbound_role_query(String named_graph, String roleName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:subClassOf ?z0 .").append("\n");
		buf.append("?z0 a owl:Class .").append("\n");
		buf.append("?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("?z2 a owl:Restriction .").append("\n");
		buf.append("?z2 owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("?y :NHC0 ?y_code .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("FILTER (str(?p_label) = \"" + roleName + "\")").append("\n");
		buf.append("}").append("\n");
		buf.append("UNION").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x rdfs:subClassOf ?r .").append("\n");
		buf.append("?r a owl:Restriction .").append("\n");
		buf.append("?r owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?r owl:someValuesFrom ?y .").append("\n");
		buf.append("?y a owl:Class .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("?y :NHC0 ?y_code .").append("\n");
		buf.append("FILTER (str(?p_label) = \"" + roleName + "\")").append("\n");
		buf.append("}").append("\n");
		buf.append("UNION ").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("?z0 a owl:Class .").append("\n");
		buf.append("?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("?z2 a owl:Restriction .").append("\n");
		buf.append("?z2 owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("?y :NHC0 ?y_code .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("FILTER (str(?p_label) = \"" + roleName + "\")").append("\n");
		buf.append("}").append("\n");
		buf.append("UNION").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .		").append("\n");
		buf.append("?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("?z0 a owl:Class .").append("\n");
		buf.append("?z0 owl:intersectionOf ?list1 .").append("\n");
		buf.append("?list1 rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("?z2 owl:unionOf ?list2 .").append("\n");
		buf.append("?list2 rdf:rest*/rdf:first ?z3 .").append("\n");
		buf.append("?z3 owl:intersectionOf ?list3 .").append("\n");
		buf.append("?list3 rdf:rest*/rdf:first ?z4 .").append("\n");
		buf.append("?z4 a owl:Restriction .").append("\n");
		buf.append("?z4 owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?z4 owl:someValuesFrom ?y .").append("\n");
		buf.append("?y :NHC0 ?y_code .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("FILTER (str(?p_label) = \"" + roleName + "\")").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector outbound_role_query(String named_graph, String propertyName) {
		String query = construct_outbound_role_query(named_graph, propertyName);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

    public Vector get_synonyms_query(String named_graph) {
		return get_property_query(named_graph, "FULL_SYN");
	}

	public String construct_get_property_query(String named_graph, String propertyName) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x ?p ?y .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?p rdfs:label \"" + propertyName + "\"").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector get_property_query(String named_graph, String propertyName) {
		String query = construct_get_property_query(named_graph, propertyName);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_get_association_query(String named_graph, String associationName) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?y a owl:Class .").append("\n");
		buf.append("?y :NHC0 ?y_code .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("?x ?p ?y .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?p rdfs:label \"" + associationName + "\"").append("\n");
		buf.append("FILTER (str(?p_label) = \"" + associationName + "\"^^xsd:string)").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");

		return buf.toString();
	}

	public Vector get_association_query(String named_graph, String associationName) {
		String query = construct_get_association_query(named_graph, associationName);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_source_code_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?z_target ?z1 ?z2").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?z_axiom a owl:Axiom .").append("\n");
		buf.append("?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("?p :NHC0 ?p_code .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?z_axiom ?y1 ?z1 .").append("\n");
		buf.append("?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("?z_axiom ?y2 ?z2 .").append("\n");
		buf.append("?y2 rdfs:label ?y2_label            ").append("\n");
		buf.append("FILTER (str(?y1_label) = \"Term Source\" && str(?y2_label) = \"Source Code\")").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector source_code_query(String named_graph) {
		String query = construct_source_code_query(named_graph);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_property_qualifier_query(String named_graph) {
        StringBuffer buf = new StringBuffer();
        buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
        buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
        buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
        buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
        buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
        buf.append("").append("\n");
        buf.append("SELECT distinct ?p_label ?y_label").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("{").append("\n");
        buf.append("?z_axiom a owl:Axiom .").append("\n");
        buf.append("?z_axiom owl:annotatedSource ?x .").append("\n");
        buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
        buf.append("?z_axiom owl:annotatedTarget ?z_target .").append("\n");
        buf.append("?p rdfs:label ?p_label .").append("\n");
        buf.append("?z_axiom ?y ?z .").append("\n");
        buf.append("?y rdfs:label ?y_label").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
    }

	public Vector property_qualifier_query(String named_graph) {
		String query = construct_property_qualifier_query(named_graph);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}


    public String construct_root_query(String named_graph) {
        StringBuffer buf = new StringBuffer();
        buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
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
        buf.append("select ?s_label ?s_code").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("?s a owl:Class .").append("\n");
        buf.append("?s rdfs:label ?s_label .").append("\n");
        buf.append("?s :NHC0 ?s_code . ").append("\n");
        buf.append("filter not exists { ?s rdfs:subClassOf|owl:equivalentClass ?o } ").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
    }

	public Vector get_roots(String named_graph) {
		Vector v = executeQuery(construct_root_query(named_graph));
		if (v == null) return null;
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

    public Vector getResponseValues(Vector v) {
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		v = new gov.nih.nci.evs.restapi.util.ParserUtils().getResponseValues(v);
		v = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(v);
		return v;
	}

	public String construct_axiom_query(String named_graph, String code) {
		String propertyName = null;
		return construct_axiom_query(named_graph, code, propertyName);
	}

	public String construct_axiom_query(String named_graph, String code, String propertyName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT ?z_axiom ?x_label ?x_code ?p_label ?z_target ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    { ").append("\n");
		buf.append("    	{").append("\n");
		buf.append("            ?z_axiom a owl:Axiom .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");

		if (propertyName != null) {
			buf.append("            ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            ?z_axiom ?y ?z . ").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");

		buf.append("        }").append("\n");
		buf.append("     }").append("\n");
		buf.append("}").append("\n");
		//buf.append("LIMIT " + Constants.DEFAULT_LIMIT).append("\n");
		return buf.toString();
	}

	public Vector getAxioms(String named_graph, String code, String propertyName) {
		String query = construct_axiom_query(named_graph, code, propertyName);
		Vector v = executeQuery(query);
		if (v != null) {
			v = new ParserUtils().getResponseValues(v);
		}
		return v;
	}

	public Vector getAxioms(String named_graph, String code) {
		Vector v = executeQuery(construct_axiom_query(named_graph, code));
		if (v != null) {
			v = new ParserUtils().getResponseValues(v);
		}
		return v;
	}

	public Vector getAxiomsByCode(String named_graph, String code, String propertyName) {
		String query = construct_axiom_query(named_graph, code, propertyName);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public Vector getAxiomsByCode(String named_graph, String code) {
		Vector v = executeQuery(construct_axiom_query(named_graph, code));
		v = new ParserUtils().getResponseValues(v);
		return v;
	}


	public String construct_get_axioms_by_code(String named_graph, String code, String prop_label) {
		return construct_get_property_qualifiers_by_code(named_graph, code, prop_label);
	}




	public String construct_get_property_qualifiers_by_code(String named_graph, String code, String prop_label) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?z_axiom ?x_label ?x_code ?p_label ?p_code ?z_target ?y_label ?y_code ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    { ").append("\n");
		buf.append("    	{").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		if (code != null) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?z_axiom ?y ?z . ").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y rdfs:label ?y_label").append("\n");
		buf.append("        }").append("\n");
		buf.append("     }").append("\n");
		buf.append("    FILTER (str(?p_label) = \"" + prop_label + "\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getPropertyQualifiersByCode(String named_graph, String code, String prop_label) {
		Vector v = executeQuery(construct_get_property_qualifiers_by_code(named_graph, code, prop_label));
		return v;
	}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*
bnode_07130346_a093_4c67_ad70_efd4d5bc5796_242621|Thorax|C12799|Maps_To|P375|Thorax, NOS|Target_Terminology|P396|GDC
bnode_07130346_a093_4c67_ad70_efd4d5bc5796_242621|Thorax|C12799|Maps_To|P375|Thorax, NOS|Target_Code|P395|SRB
bnode_07130346_a093_4c67_ad70_efd4d5bc5796_242618|Thorax|C12799|Maps_To|P375|Thorax|Target_Code|P395|BAS
bnode_07130346_a093_4c67_ad70_efd4d5bc5796_242618|Thorax|C12799|Maps_To|P375|Thorax|Target_Terminology|P396|GDC
*/

	public Vector getPropertyQualifiersByCode(String named_graph, String code, String prop_label, String prop_value) {
		Vector v = executeQuery(construct_get_property_qualifiers_by_code(named_graph, code, prop_label, prop_value));
		return v;
	}

	public String construct_get_property_qualifiers_by_code(String named_graph, String code, String prop_label, String prop_value) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?z_axiom ?x_label ?x_code ?p_label ?p_code ?z_target ?y_label ?y_code ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    { ").append("\n");
		buf.append("    	{").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		if (code != null) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?z_axiom ?y ?z . ").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y rdfs:label ?y_label").append("\n");
		buf.append("        }").append("\n");
		buf.append("     }").append("\n");
		buf.append("    FILTER (str(?p_label) = \"" + prop_label + "\" && str(?z) = \"" + prop_value + "\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public String construct_property_query(String named_graph, String property_code) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("").append("\n");
		buf.append("SELECT ?x_code ?a_code ?a_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?a a owl:AnnotationProperty .").append("\n");
		buf.append("?x ?a ?z .").append("\n");
		buf.append("?a :NHC0 ?a_code .").append("\n");
		buf.append("?a rdfs:label ?a_label ").append("\n");
		buf.append("}").append("\n");
		buf.append("FILTER (str(?a_code) = '" + property_code + "')").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsWithProperty(String named_graph, String prop_code) {
		String query = construct_property_query(named_graph, prop_code);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_concept_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> ").append("\n");
		buf.append("").append("\n");
		buf.append("SELECT ?x_code ?x_label").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label ").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAllConcepts(String named_graph, String prop_code) {
		String query = construct_concept_query(named_graph);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_association_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> ").append("\n");
		buf.append("").append("\n");
		buf.append("SELECT ?x_code ?x_label ?a_code ?a_label ?z_code ?z_label").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .        ").append("\n");
		buf.append("?a a owl:AnnotationProperty .").append("\n");
		buf.append("?a :NHC0 ?a_code .").append("\n");
		buf.append("?a rdfs:label ?a_label .   ").append("\n");
		buf.append("?x ?a ?z .").append("\n");
		buf.append("?z a owl:Class .").append("\n");
		buf.append("?z :NHC0 ?z_code .").append("\n");
		buf.append("?z rdfs:label ?z_label .        ").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAllConceptAssociations(String named_graph, String prop_code) {
		String query = construct_association_query(named_graph);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_object_property_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> ").append("\n");
		buf.append("").append("\n");
		buf.append("SELECT ?x_code ?x_label ?a_code ?a_label ?z_code ?z_label").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:label ?x_label .        ").append("\n");
		buf.append("?a a owl:ObjectProperty .").append("\n");
		buf.append("?a :NHC0 ?a_code .").append("\n");
		buf.append("?a rdfs:label ?a_label .   ").append("\n");
		buf.append("?x ?a ?z .").append("\n");
		buf.append("?z a owl:Class .").append("\n");
		buf.append("?z :NHC0 ?z_code .").append("\n");
		buf.append("?z rdfs:label ?z_label .        ").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAllConceptObjectProperties(String named_graph) {
		String query = construct_object_property_query(named_graph);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public Vector getAllConceptProperties(String named_graph) {
		int lcv = 0;
		Vector w = new Vector();
		Vector property_name_code_vec = getSupportedProperties(named_graph);
		for (int i=0; i<property_name_code_vec.size(); i++) {
			String property_name_code = (String) property_name_code_vec.elementAt(i);
			Vector u = StringUtils.parseData(property_name_code, "|");
			String property_name = (String) u.elementAt(0);
			lcv++;
			System.out.println("(" + lcv + ") " + property_name);
			String property_code = (String) u.elementAt(1);
			Vector v = null;
			try {
			    v = getConceptsWithProperty(named_graph, property_code);
			} catch (Exception ex) {

			}
			if (v != null) {
				w.addAll(v);
			}
		}
		return w;
	}


	public String construct_object_property_definition_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> ").append("\n");
		buf.append("").append("\n");
		buf.append("SELECT ?a_code ?a_label ?p_value").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?a a owl:ObjectProperty .").append("\n");
		buf.append("?a :NHC0 ?a_code .").append("\n");
		buf.append("?a rdfs:label ?a_label .  ").append("\n");
		buf.append("?a ?p ?p_value .").append("\n");
		buf.append("?p :NHC0 ?p_code").append("\n");
		buf.append("}").append("\n");
		buf.append("FILTER(str(?p_code) = 'P97')").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getObjectPropertyDefinitions(String named_graph) {
		String query = construct_object_property_definition_query(named_graph);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}


	public String construct_property_definition_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> ").append("\n");
		buf.append("").append("\n");
		buf.append("SELECT ?a_code ?a_label ?p_value").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?a a owl:AnnotationProperty .").append("\n");
		buf.append("?a :NHC0 ?a_code .").append("\n");
		buf.append("?a rdfs:label ?a_label .  ").append("\n");
		buf.append("?a ?p ?p_value .").append("\n");
		buf.append("?p :NHC0 ?p_code").append("\n");
		buf.append("}").append("\n");
		buf.append("FILTER(str(?p_code) = 'P97')").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAnnotationPropertyDefinitions(String named_graph) {
		String query = construct_property_definition_query(named_graph);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_supported_sources_query(String named_graph) {
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
		buf.append("SELECT distinct ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + "> {").append("\n");
		buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?p rdfs:label 'FULL_SYN'^^xsd:string .").append("\n");
		buf.append("?z_axiom ?y ?z .").append("\n");
		buf.append("?y rdfs:label 'Term Source'^^xsd:string ").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getSupportedSources(String named_graph) {
		String query = construct_supported_sources_query(named_graph);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}


	public String construct_supported_property_qualifiers(String named_graph, String property_name) {
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
		buf.append("SELECT distinct ?y_label").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + "> {").append("\n");
		buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?p rdfs:label '" + property_name + "'^^xsd:string .").append("\n");
		buf.append("?z_axiom ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getSupportedPropertyQualifiers(String named_graph, String property_name) {
		String query = construct_supported_property_qualifiers(named_graph, property_name);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_supported_property_qualifier_values(String named_graph, String property_name, String qualifier_name) {
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
		buf.append("SELECT distinct ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + "> {").append("\n");
		buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?p rdfs:label '" + property_name + "'^^xsd:string .").append("\n");
		buf.append("?z_axiom ?y ?z .").append("\n");
		buf.append("?y rdfs:label '" + qualifier_name + "'^^xsd:string .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		//buf.append("LIMIT 1000").append("\n");
		return buf.toString();
	}

	public Vector getSupportedPropertyQualifierValues(String named_graph, String property_name, String qualifier_name) {
		String query = construct_supported_property_qualifier_values(named_graph, property_name, qualifier_name);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}


/*
Source Code
Subsource Name
Term Source
Term Type
*/


	public String construct_supported_property_qualifiers(String named_graph) {
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
		buf.append("SELECT distinct ?property_name ?p_code ?qualifier_name ?y_code").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + "> {").append("\n");
		buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?p rdfs:label ?property_name .").append("\n");
		buf.append("?p :NHC0 ?p_code .").append("\n");
		buf.append("?z_axiom ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?qualifier_name .").append("\n");
		buf.append("?y :NHC0 ?y_code .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getSupportedPropertyQualifierValues(String named_graph) {
		String query = construct_supported_property_qualifiers(named_graph);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public Vector findConceptsWithPropertyMatching(String named_graph, String property_name, String property_value) {
	    String query = generate_find_concepts_with_property_matching(named_graph, property_name, property_value);
	    Vector v = executeQuery(query);
	    v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String generate_find_concepts_with_property_matching(String named_graph, String property_name, String property_value) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label . ").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("?y rdfs:label \"" + property_name + "\"^^xsd:string  ").append("\n");
		buf.append("}").append("\n");
		if (property_value != null) {
			buf.append("FILTER (str(?z) = \"" + property_value + "\"^^xsd:string)").append("\n");
	    }
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector findConceptsWithProperty(String named_graph, String property_name) {
	    String query = generate_find_concepts_with_property(named_graph, property_name);
	    Vector v = executeQuery(query);
	    v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String generate_find_concepts_with_property(String named_graph, String property_name) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label . ").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("?y rdfs:label \"" + property_name + "\"^^xsd:string  ").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector findMatchedConcepts(String named_graph, String target) {
	    String query = generate_find_matched_concepts(named_graph, target);
	    Vector v = executeQuery(query);
	    v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
	    return v;
	}

	public String generate_find_matched_concepts(String named_graph, String target) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label . ").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
        buf.append("FILTER (lcase(str(?x_label)) =  lcase(str(\"" + target + "\")))").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getOWLClasses(String named_graph) {
	    String query = generate_get_owl_classes(named_graph);
	    Vector v = executeQuery(query);
	    v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
	    return v;
	}

	public String generate_get_owl_classes(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label . ").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public String construct_get_property_qualifier_values_by_code(String named_graph, String code, String prop_label, String qualifier_label) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?z_axiom ?x_label ?x_code ?p_label ?p_code ?z_target ?y_label ?y_code ?z").append("\n");
		//buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?z_target ?y_label ?y_code ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    { ").append("\n");
		buf.append("    	{").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");

		if (code != null) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");

		if (prop_label != null) {
			buf.append("            ?p rdfs:label \"" + prop_label + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?z_axiom ?y ?z . ").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");

		if (qualifier_label != null) {
			buf.append("            ?y rdfs:label \"" + qualifier_label + "\"^^xsd:string .").append("\n");
		}

		buf.append("        }").append("\n");
		buf.append("     }").append("\n");
		//buf.append("    FILTER (str(?p_label) = \"" + prop_label + "\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getPropertyQualifierValuesByCode(String named_graph, String code, String prop_label, String qualifier_label) {
		String query = construct_get_property_qualifier_values_by_code(named_graph, code, prop_label, qualifier_label);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_get_axioms_with_qualifier_matching(String named_graph, String code, String prop_label, String qualifier_label, String qualifier_value) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?z_axiom ?x_label ?x_code ?p_label ?p_code ?z_target ?y_label ?y_code ?z").append("\n");
		//buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?z_target ?y_label ?y_code ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    { ").append("\n");
		buf.append("    	{").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");

		if (code != null) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");

		if (prop_label != null) {
			buf.append("            ?p rdfs:label \"" + prop_label + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?z_axiom ?y ?z . ").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");

		if (qualifier_label != null) {
			buf.append("            ?y rdfs:label \"" + qualifier_label + "\"^^xsd:string .").append("\n");
		}

		buf.append("        }").append("\n");
		buf.append("     }").append("\n");
		if (qualifier_value != null) {
			buf.append("    FILTER (str(?z) = \"" + qualifier_value + "\")").append("\n");
		}
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAxiomsWithQualifierMatching(String named_graph, String code, String prop_label, String qualifier_label, String qualifier_value) {
		String query = construct_get_axioms_with_qualifier_matching(named_graph, code, prop_label, qualifier_label, qualifier_value);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_get_disjoint(String named_graph, boolean forward) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (forward) {
			buf.append("SELECT distinct ?y_label ?y_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_label ?x_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:disjointWith ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	    }").append("\n");
		buf.append("	    ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getDisjoin(String named_graph, boolean forward) {
		Vector v = executeQuery(construct_get_disjoint(named_graph, forward));
		return new ParserUtils().getResponseValues(v);
	}


	public String construct_get_disjoint(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?y_label ?y_code ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("		?x owl:disjointWith ?y .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	    }").append("\n");
		buf.append("	    ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getDisjoin(String named_graph) {
		Vector v = executeQuery(construct_get_disjoint(named_graph));
		return new ParserUtils().getResponseValues(v);
	}

    public Vector executeQuery(String restURL, String username, String password, String query) {
		HTTPUtils httpUtils = new HTTPUtils(restURL, username, password);
		Vector v = null;
		try {
			String json = httpUtils.runSPARQL(query);
			v = new JSONUtils().parseJSON(json);
			v = new ParserUtils().getResponseValues(v);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}

	public String construct_get_class_identifiers(String named_graph, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?x_label ?x_code ").append("\n");
		} else {
			buf.append("SELECT distinct ?x_code ").append("\n");
		}
		buf.append("{ ").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 ?x_code .").append("\n");
		buf.append("		?x rdfs:label ?x_label .").append("\n");
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getClassIdentifiers(String named_graph) {
		boolean codeOnly = false;
		return getClassIdentifiers(named_graph, codeOnly);
	}

	public Vector getClassIdentifiers(String named_graph, boolean codeOnly) {
		Vector v = executeQuery(construct_get_class_identifiers(named_graph, codeOnly));
		v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

    public static void test1(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
        String named_graph = args[1];
        String queryfile = args[2];
        owlSPARQLUtils.set_named_graph(named_graph);
        Vector v = owlSPARQLUtils.getAllConceptProperties(named_graph);
        Utils.saveToFile("all_properties.txt", v);
        v = owlSPARQLUtils.getSupportedPropertyQualifierValues(named_graph);
        StringUtils.dumpVector(v);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }

	public static void test2(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		System.out.println(serviceUrl);
		//MetadataUtils metadataUtils = new MetadataUtils(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		//String version = metadataUtils.getLatestVersion(codingScheme);
		System.out.println(codingScheme);
		//System.out.println(version);
		String named_graph = args[1]; //test.getNamedGraph(codingScheme);
		System.out.println(named_graph);
		ParserUtils parser = new ParserUtils();

		String username = args[2]; //ConfigurationController.username;
		String password = args[3]; //ConfigurationController.password;

		System.out.println("username: " + username);
		System.out.println("password: " + password);

    	OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		String inputfile = args[4];
		Vector codes = Utils.readFile(inputfile);
		Utils.dumpVector("codes", codes);
		for (int i=0; i<codes.size(); i++) {
			String concept_code = (String) codes.elementAt(i);
			Vector w = owlSPARQLUtils.getLabelByCode(named_graph, concept_code);
			w = new ParserUtils().getResponseValues(w);
			String label = (String) w.elementAt(0);
			int j = i+1;
			System.out.println("(" + j + ") " + label + " (" + concept_code + ")");
			HashMap prop_map = owlSPARQLUtils.getPropertyHashMapByCode(named_graph, concept_code);
			Utils.dumpMultiValuedHashMap("prop_map", prop_map);
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}


    public Vector getDefinitions(String named_graph, String code, String prop_label) {
        Vector v = getPropertyQualifiersByCode(named_graph, code, prop_label);
        v = parser.getResponseValues(v);
		return parser.parseDefinitionData(v, prop_label);
	}

	public static void main(String[] args) {
		if (args.length == 2) {
			test1(args);
		} else {
			test2(args);
		}
	}
}
