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
    gov.nih.nci.evs.restapi.util.JSONUtils jsonUtils = null;
    gov.nih.nci.evs.restapi.util.HTTPUtils httpUtils = null;
    public String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String restURL = null;
    String named_graph_id = ":NHC0";

    String NCIT_URI = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String BASE_URI = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

    static String DEFAULT_URL = "http://localhost:3030/ncit/query";

    gov.nih.nci.evs.restapi.util.ParserUtils parser = new ParserUtils();
    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String username = null;
    String password = null;

    private HashMap propertyCode2labelHashMap = null;

    public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

    public void setJSONUtils(JSONUtils jsonUtils) {
		this.jsonUtils = jsonUtils;
	}

    public void setHTTPUtils(gov.nih.nci.evs.restapi.util.HTTPUtils httpUtils) {
		this.httpUtils = httpUtils;
	}

	public void setOntologyUri2LabelMap(HashMap ontologyUri2LabelMap) {
		this.ontologyUri2LabelMap = ontologyUri2LabelMap;
	}

	public OWLSPARQLUtils() {
		this.httpUtils = new gov.nih.nci.evs.restapi.util.HTTPUtils();
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
		//this.serviceUrl = verifyServiceUrl(serviceUrl);
		this.serviceUrl = serviceUrl;
		this.restURL = this.serviceUrl;
		System.out.println(this.serviceUrl);
		this.httpUtils = new gov.nih.nci.evs.restapi.util.HTTPUtils(serviceUrl, null, null);
        this.jsonUtils = new JSONUtils();
        this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
    }

	public OWLSPARQLUtils(ServerInfo serverInfo) {
		this.serviceUrl = serverInfo.getServiceUrl();
		this.serviceUrl = verifyServiceUrl(serviceUrl);
		this.restURL = this.serviceUrl;
		this.httpUtils = new gov.nih.nci.evs.restapi.util.HTTPUtils(serviceUrl, serverInfo.getUsername(), serverInfo.getPassword());
        this.jsonUtils = new JSONUtils();
        this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
	}

	public OWLSPARQLUtils(String serviceUrl, String username, String password) {
		this.serviceUrl = serviceUrl;//verifyServiceUrl(serviceUrl);
		this.restURL = serviceUrl;
		this.username = username;
		this.password = password;

		System.out.println(this.serviceUrl);
		this.httpUtils = new gov.nih.nci.evs.restapi.util.HTTPUtils(serviceUrl, username, password);
        this.jsonUtils = new gov.nih.nci.evs.restapi.util.JSONUtils();
        this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
    }

	public OWLSPARQLUtils(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.restURL = serviceUrl;
		this.username = username;
		this.password = password;

		this.httpUtils = new gov.nih.nci.evs.restapi.util.HTTPUtils(serviceUrl, username, password);
        this.jsonUtils = new JSONUtils();
        this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
    }


    public HashMap createOntologyUri2LabelMap() {
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
		propertyCode2labelHashMap = new HashMap();
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
		buf.append("PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>").append("\n");
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
		Vector w = null;
		try {
			//String query = httpUtils.loadQuery(query_file, false);
			//return executeQuery(query);
			String query = httpUtils.loadQuery(query_file, false);
			String json = new HTTPUtils(this.restURL).runSPARQL(query, restURL);
			System.out.println(json);
			//gov.nih.nci.evs.reportwriter.core.util.JSONUtils jsonUtils = new gov.nih.nci.evs.reportwriter.core.util.JSONUtils();
			//gov.nih.nci.evs.restapi.util.JSONUtils jsonUtils = new gov.nih.nci.evs.restapi.util.JSONUtils();
			JSONUtils jsonUtils = new JSONUtils();
			w = jsonUtils.parseJSON(json);
			w = jsonUtils.getResponseValues(w);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return w;
	}

    public Vector executeQuery(String query) {
		//from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>
		if (this.named_graph != null) {
			query = query.replaceAll("from <" + NCIT_URI + ">", "from <" + this.named_graph + ">");
    	}
        Vector v = null;
        try {
			if (this.password == null) {
				//System.out.println("this.restURL : " + this.restURL);
				//System.out.println("query:\n " + query);
				String json = new HTTPUtils(this.restURL).runSPARQL(query, this.restURL);
				//System.out.println(json);
				//gov.nih.nci.evs.reportwriter.core.util.JSONUtils jsonUtils = new gov.nih.nci.evs.reportwriter.core.util.JSONUtils();
				//gov.nih.nci.evs.restapi.util.JSONUtils jsonUtils = new gov.nih.nci.evs.restapi.util.JSONUtils();
				JSONUtils jsonUtils = new JSONUtils();
				v = jsonUtils.parseJSON(json);
				v = jsonUtils.getResponseValues(v);

			} else {
				gov.nih.nci.evs.restapi.util.RESTUtils restUtils = new gov.nih.nci.evs.restapi.util.RESTUtils(this.username, this.password, 1500000, 1500000);
				String response = restUtils.runSPARQL(query, serviceUrl);
				v = new gov.nih.nci.evs.restapi.util.JSONUtils().parseJSON(response);
				v = jsonUtils.getResponseValues(v);
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
		buf.append("PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>").append("\n");
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
		// v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public Vector get_ontology_info(String named_graph) {
		Vector v = executeQuery(construct_get_ontology_info(named_graph));
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
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
	    }
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

/*
	public Vector get_ontology_info(String named_graph) {
		Vector v = executeQuery(construct_get_ontology_info(named_graph));
		// v = new ParserUtils().getResponseValues(v);
		return v;
	}
*/


	public HashMap getGraphName2VersionHashMap() {
		HashMap hmap = new HashMap();
		Vector v = get_ontology_info();
		if (v == null) {
		    System.out.println("ERROR: get_ontology_info returns null." );
		    return null;
		}

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
		buf.append("SELECT distinct ?g {").append("\n");
		buf.append("  { ?s ?p ?o } ").append("\n");
		buf.append("UNION ").append("\n");
		buf.append("    { graph ?g { ?s ?p ?o } }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getNamedGraph() {
        Vector v = executeQuery(construct_get_named_graphs());
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_label_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select ?x_label").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class . ").append("\n");
		buf.append("	    ?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("	    ?x rdfs:label ?x_label    ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getLabelByCode(String code) {
        Vector v = executeQuery(construct_get_label_by_code(this.named_graph, code));
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getLabelByCode(String named_graph, String code) {
        Vector v = executeQuery(construct_get_label_by_code(named_graph, code));
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getInverseRolesByCode(String named_graph, String code) {
		return getInverseRoles(named_graph, code);
	}

	public String construct_get_code_and_label(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select ?x_code ?x_label").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class . ").append("\n");
		buf.append("	    ?x :NHC0 ?x_code .").append("\n");
		buf.append("	    ?x rdfs:label ?x_label    ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getCodeAndLabel(String named_graph) {
        Vector v = executeQuery(construct_get_code_and_label(named_graph));
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
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
        Vector v = executeQuery(construct_get_class_counts());
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_class_count(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT (count(?class) as ?count) ").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("   	    ?class a owl:Class .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getClassCount(String named_graph) {
        Vector v = executeQuery(construct_get_class_count(named_graph));
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_triple_count(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT (count(*) as ?count) ").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("   	    ?s ?p ?o .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getTripleCount(String named_graph) {
        Vector v = executeQuery(construct_get_triple_count(named_graph));
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
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

////////////////////////////////////////////////////////////////////////////////////////////////
	public String construct_get_superclasses_by_code(String named_graph, String code) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?y_label ?y_code").append("\n");
        buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
        buf.append("            {").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("                ?y :NHC0 ?y_code .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("                ?x rdfs:subClassOf ?y . ").append("\n");
        buf.append("            } union {").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("                ?y :NHC0 ?y_code .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("                ?x rdfs:subClassOf ?rs . ").append("\n");
        buf.append("                ?rs a owl:Restriction .  ").append("\n");
        buf.append("            ?rs owl:onProperty ?p .").append("\n");
        buf.append("                ?rs owl:someValuesFrom ?y .  ").append("\n");
        buf.append("            }").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
    }

	public Vector getSuperclassesByCode(String named_graph, String code) {
        String query = construct_get_superclasses_by_code(named_graph, code);
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_subclasses_by_code(String named_graph, String code) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_label ?x_code").append("\n");
        buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
        buf.append("            {").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("                ?y :NHC0 ?y_code .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ?y :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("                ?x rdfs:subClassOf ?y . ").append("\n");
        buf.append("            } union {").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("                ?y :NHC0 ?y_code .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ?y :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("                ?x rdfs:subClassOf ?rs . ").append("\n");
        buf.append("                ?rs a owl:Restriction .  ").append("\n");
        buf.append("                ?rs owl:onProperty ?p .").append("\n");
        buf.append("                ?rs owl:someValuesFrom ?y .  ").append("\n");
        buf.append("            }").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
    }

	public Vector getSubclassesByCode(String named_graph, String code) {
        String query = construct_get_subclasses_by_code(named_graph, code);
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}
/////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isLeaf(String named_graph, String code) {
		Vector v = getSubclassesByCode(named_graph, code);
		if (v != null && v.size() > 0) return false;
	    return true;
	}

/*
	public Vector getSubclassesByCode(String named_graph, String code) {
		return getSubclasses(named_graph, code);
	}
*/
	public Vector getRolesByCode(String named_graph, String code) {
		return getRoles(named_graph, code);
	}


	public boolean hasRole(String code, String roleName) {
		return hasRole(this.named_graph, code, roleName);
	}

	public boolean hasRole(String named_graph, String code, String roleName) {
		Vector v = getRoles(named_graph, code);
		if (v == null) return false;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String p_label = (String) u.elementAt(3);
			if (p_label.compareTo(roleName) == 0) {
				return true;
			}
		}
		return false;
	}

	public String construct_get_properties_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_code ?x_label ?y_code ?y_label ?z").append("\n");
		//buf.append("SELECT ?x_label ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("	        ?x :NHC0 ?x_code .").append("\n");
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
	    String query = construct_get_properties_by_code(named_graph, code);
	    Vector v = executeQuery(query);
	    if (v == null) return null;
		//// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}


	public String construct_get_properties_by_code(String named_graph, String code, String propertyName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_code ?x_label ?y_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
        if (propertyName != null) {
			buf.append("            ?y rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
		}
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getPropertiesByCode(String named_graph, String code, String propertyName) {
		String query = construct_get_properties_by_code(named_graph, code, propertyName);
		Vector v = executeQuery(query);
		//// v = new ParserUtils().getResponseValues(v);
		return v;
	}


	public String construct_get_ontology_version_info(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_version_info").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Ontology .").append("\n");
		buf.append("	    ?x owl:versionInfo ?x_version_info").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector getOntologyVersionInfo(String named_graph) {
        Vector v = executeQuery(construct_get_ontology_version_info(named_graph));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
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
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
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
		buf.append("ORDER BY ?z_axiom").append("\n");
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("            ?p a owl:AnnotationProperty .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label ").append("\n");
		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getAnnotationProperties(String named_graph) {
        Vector v = executeQuery(construct_get_annotation_properties(named_graph));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_object_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?p_label ?p_code ").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("            ?p a owl:ObjectProperty.").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label ").append("\n");
		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}

		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getObjectProperties(String named_graph) {
        Vector v = executeQuery(construct_get_object_properties(named_graph));
        if (v == null) return null;
        if (v.size() == 0) return v;
        return v;
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
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> ").append("\n");
		}
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
		buf.append("ORDER BY ?z_axiom").append("\n");
		return buf.toString();
	}

	public Vector getPropertyQualifiersByCode(String named_graph, String code) {
		Vector v = executeQuery(construct_get_property_qualifiers_by_code(named_graph, code));
		return v;
	}

	public Vector getInboundRolesByCode(String named_graph, String code) {
        return getInverseRoles(named_graph, code);
	}

	public Vector getOutboundRolesByCode(String named_graph, String code) {
        return getRoles(named_graph, code);
	}

	public Vector getRoleRelationships(String named_graph) {
		return getRoles(named_graph);
	}

	public String construct_get_inverse_associations_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
        Vector v = executeQuery(construct_get_inverse_associations_by_code(named_graph, code));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        //return new SortUtils().quickSort(v);
        return v;
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String construct_get_associations_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?y_label ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> ").append("\n");
		}
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
        Vector v = executeQuery(construct_get_associations_by_code(named_graph, code));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_associations(String named_graph, String associationName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> ").append("\n");
		}

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
        Vector v = executeQuery(construct_get_associations(named_graph, associationName));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String construct_get_annotaion_properties_by_code(String named_graph, String code) {
		return construct_get_annotaion_properties_by_code(named_graph, code, null);
	}

	public String construct_get_annotaion_properties_by_code(String named_graph, String code, String associationName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
        buf.append("SELECT ?x_label ?y_label ?z").append("\n");
        buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> ").append("\n");
		}

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
        //// v = new ParserUtils().getResponseValues(v);
        return v;
	}

	public Vector getAnnotaionPropertiesByCode(String named_graph, String code, String associationName) {
		String query = construct_get_annotaion_properties_by_code(named_graph, code, associationName);
		Vector v = executeQuery(query);
        //// v = new ParserUtils().getResponseValues(v);
        return v;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Vector getDiseaseIsStageSourceCodes(String named_graph) {
        Vector v = executeQuery(construct_get_association_sources(named_graph, "Disease_Is_Stage", true));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getDiseaseIsGradeSourceCodes(String named_graph) {
        Vector v = executeQuery(construct_get_association_sources(named_graph, "Disease_Is_Grade", true));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getDiseaseIsStageSources(String named_graph, boolean code_only) {
        Vector v = executeQuery(construct_get_association_sources(named_graph, "Disease_Is_Stage", code_only));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getDiseaseIsGradeSources(String named_graph, boolean code_only) {
        Vector v = executeQuery(construct_get_association_sources(named_graph, "Disease_Is_Grade", code_only));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getAssociationSourceCodes(String named_graph, String associationName) {
        Vector v = executeQuery(construct_get_association_sources(named_graph, associationName, true));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> ").append("\n");
		}

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


	public String construct_get_relationship_sources(String named_graph, String associationName, boolean code_only) {
		return construct_get_association_sources(named_graph, associationName, code_only);
	}

	public String construct_get_disjoint_with_by_code(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?y_label ?y_code ").append("\n");
		buf.append("{ ").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> ").append("\n");
		}

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
        Vector v = executeQuery(construct_get_disjoint_with_by_code(named_graph, code));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_object_properties_domain_range(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_pt ?x_code ?x_domain_label ?x_range_label ").append("\n");
		buf.append("{ ").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> ").append("\n");
		}

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
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
		String query = construct_get_object_valued_annotation_properties(named_graph);
		Vector v = executeQuery(query);
		Vector w = new ParserUtils().getResponseValues(v);
		w = new SortUtils().quickSort(w);
		return w;
	}

	public String construct_get_string_valued_annotation_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?p_label ?p_code ").append("\n");
		buf.append("{ ").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}


		buf.append("    {").append("\n");
		buf.append("      {").append("\n");
		buf.append("         {").append("\n");
		buf.append("           ?p a owl:AnnotationProperty .").append("\n");
		buf.append("           ?p :NHC0 ?p_code .").append("\n");
		buf.append("           ?p rdfs:label ?p_label .").append("\n");
        buf.append("         }").append("\n");
		buf.append("         MINUS").append("\n");
		buf.append("         {").append("\n");
		buf.append("           ?p a owl:AnnotationProperty .").append("\n");
		buf.append("           ?p rdfs:range xsd:anyURI .").append("\n");
		buf.append("         }").append("\n");
		buf.append("      }").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getStringValuedAnnotationProperties(String named_graph) {
        Vector v = executeQuery(construct_get_string_valued_annotation_properties(named_graph));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        v = new SortUtils().quickSort(v);
        return v;
	}

	public Vector getSupportedProperties(String named_graph) {
		Vector v = getStringValuedAnnotationProperties(named_graph);
		return v;
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



	public Vector get_concepts_in_subset(String named_graph, String code) {
		Vector v = getConceptsInSubset(named_graph, code, false);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		//// v = new ParserUtils().getResponseValues(v);
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
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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

	public Vector getPublishedValueSets(String named_graph) {
		return getPublishedValueSets(named_graph, true);
	}


	public Vector getPublishedValueSets(String named_graph, boolean codeOnly) {
		String query = construct_get_published_value_sets(named_graph, codeOnly);
		Vector v = executeQuery(query);
		//// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_get_published_value_sets(String named_graph, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT distinct ?z_code").append("\n");
		} else{
			buf.append("SELECT distinct ?z_label ?z_code").append("\n");
		}
  		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + "Concept_In_Subset" + "\"^^xsd:string .").append("\n");

		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?z rdfs:label ?z_label .").append("\n");
		buf.append("            ?z " + named_graph_id + " ?z_code .").append("\n");

		buf.append("            ?p a owl:AnnotationProperty .").append("\n");
		buf.append("            ?p rdfs:label " + "\"" + "Publish_Value_Set" + "\"^^xsd:string .").append("\n");
		buf.append("            ?z ?p " + "\"" + "Yes" + "\"^^xsd:string .").append("\n");

		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsInSubset(String named_graph, String code, boolean codeOnly) {
        Vector v = executeQuery(construct_get_concepts_in_subset(named_graph, code, codeOnly));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}


	public String construct_get_subset_membership(String named_graph, String code, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT ?x_code").append("\n");
		} else {
			buf.append("SELECT ?x_label ?x_code").append("\n");
	    }
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?z rdfs:label ?z_label .").append("\n");
		buf.append("            ?z " + named_graph_id + " \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + "Concept_In_Subset" + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getSubsetMembership(String named_graph, String code, boolean codeOnly) {
		Vector w = executeQuery(construct_get_subset_membership(named_graph, code, codeOnly));
		//return new ParserUtils().getResponseValues(w);
		return w;
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
        Vector v = executeQuery(construct_get_associated_concepts(named_graph, association));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getAssociatedConcepts(String named_graph, String target_code, String association) {
        Vector v = executeQuery(construct_get_associated_concepts(named_graph, target_code, association));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getAssociatedConcepts(String named_graph, String target_code, String association, boolean outbound) {
        Vector v = executeQuery(construct_get_associated_concepts(named_graph, target_code, association, outbound));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

/*
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
*/

	public String[] get_concept_in_subset_codes(String named_graph, String subset_code) {
		Vector w = getConceptsInSubset(named_graph, subset_code);
		String[] array = new String[w.size()];
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String code = (String) u.elementAt(0);
			if (u.size() == 2) {
				code = (String) u.elementAt(1);
			}
			array[i] = code;
		}
		return array;
	}

    public HashMap getNameVersion2NamedGraphMap() {
		HashMap graphName2VersionHashMap = getGraphName2VersionHashMap();

		HashMap nameVersion2NamedGraphMap = new HashMap();
		ParserUtils parserUtils = new ParserUtils();
		//vocabulary name|vocabulary version|graph named

		System.out.println("getAvailableOntologies ...");
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
		System.out.println("getNamedGraphs ...");
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
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("SELECT distinct ?x").append("\n");
		buf.append("{ ").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("	    {").append("\n");
		buf.append("		?x a owl:Ontology ").append("\n");
		buf.append("	    }").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getOntology(String named_graph) {
        Vector v = executeQuery(construct_get_ontology(named_graph));
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}


	public Vector getNamedGraphs() {
		String query = construct_get_named_graphs();
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        //// v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_ontology_version(String named_graph, String predicate) {
		Vector v = getOntology(named_graph);
		String ontology_uri = parser.getValue((String) v.elementAt(0));
		String prefixes = PrefixUtils.getPrefixes(ontology_uri);

		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_version").append("\n");
		buf.append("{ ").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
			Vector u2 = getSubclassesByCode(named_graph, next_code);
			Vector v3 = new Vector();
			for (int i=0; i<u2.size(); i++) {
				String t = (String) u2.elementAt(i);
				Vector u3 = StringUtils.parseData(t, '|');
				String t1 = (String) u3.elementAt(0);
				String t2 = (String) u3.elementAt(1);
				v3.add(next_label + "|" + next_code + "|" + t1 + "|" + t2);
				stack.push(t1 + "|" + t2 + "|" + nextLevel);
			}
			/*
			int n = u.size()/2;
			for (int i=0; i<n; i++) {
				String s1 = (String) u.elementAt(i*2);
				String s2 = (String) u.elementAt(i*2+1);
				String t1 = parser.getValue(s1);
				String t2 = parser.getValue(s2);
				v3.add(next_label + "|" + next_code + "|" + t1 + "|" + t2);
				stack.push(t1 + "|" + t2 + "|" + nextLevel);
			}
			*/

			w.addAll(v3);
		}
		return w;
	}

	public boolean isFlatFormat(String named_graph) {
		Vector v = executeQuery(construct_test_get_restrictions(named_graph));
		if (v != null && v.size() > 0) return false;
		return true;
	}

	public String construct_test_get_restrictions(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}


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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		if (propertyName != null) {
			buf.append("            ?y rdfs:label " + "\"" + propertyName + "\"^^xsd:string ").append("\n");
	    }
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		Vector v = executeQuery(query);
		//// v = new ParserUtils().getResponseValues(v);
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
		//// v = new ParserUtils().getResponseValues(v);
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
		//// v = new ParserUtils().getResponseValues(v);
		return (String) v.elementAt(0);
	}

////////////////////////////////////////////////////////////////////

	public Vector getSuperclassesByCode(String code) {
		return getSuperclassesByCode(named_graph, code);
	}

	public Vector getSubclassesByCode(String code) {
		return getSubclassesByCode(named_graph, code);
	}

	public Vector getOutboundRolesByCode(String code) {
		return getRoles(named_graph, code);
	}

	public Vector getInboundRolesByCode(String code) {
		return getInverseRoles(named_graph, code);
	}

	public Vector getAssociationsByCode(String code) {
		Vector v = getAssociationsByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public Vector getInverseAssociationsByCode(String code) {
		Vector v = getInverseAssociationsByCode(named_graph, code);
		if (v == null) return null;
		if (v.size() == 0) return new Vector();
		// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_get_property_qualifier_values(String named_graph, String property_name, Vector inclusions, Vector exclusions) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?p_label ?y_label ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_predicate_labels(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?y ?y_label").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getPredicateLabels(String named_graph) {
	    String query = construct_get_predicate_labels(named_graph);
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
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
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
	    Vector v = executeQuery(query);
	    if (v == null) return null;
		// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_get_owl_class_data(String named_graph, String identifier, String code, String ns, boolean by_code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z ").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String generate_get_property_values_by_code(String named_graph, String code, String property_name) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}


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
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}


	public String generate_get_property_values(String named_graph, String property_name) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
        Vector v = executeQuery(query);
        //Utils.dumpVector(property_name, v);
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_owl_class_data(String named_graph, String uri) {
		StringBuffer buf = new StringBuffer();
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT ?x_label ?y_label ?z ").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
	    // v = new ParserUtils().getResponseValues(v);
	    return v;
	}

////////////////////////////////////////////////////////////////////
	public String construct_get_label_by_uri(String named_graph, String uri) {
		StringBuffer buf = new StringBuffer();
        buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT ?x_label").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
	    // v = new ParserUtils().getResponseValues(v);
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
        buf.append(prefixes);
		buf.append("SELECT ?x_label ?x ?y_label ?y").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
	    // v = new ParserUtils().getResponseValues(v);
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
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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


    public String construct_get_domain_and_range_data(String name_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?x_label ?x_code ?x_domain_label ?x_domain_code ?x_range_label ?x_range_code ").append("\n");
        buf.append("{ ").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
	    // v = new ParserUtils().getResponseValues(v);
	    return new SortUtils().quickSort(v);
	}

	public Vector<ComplexProperty> getComplexProperties(String named_graph, String code) {
		Vector w = new Vector();
		Vector v = getPropertyQualifiersByCode(named_graph, code);
		if (v == null || v.size() == 0) return null;
		// v = new ParserUtils().getResponseValues(v);
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
			if (named_graph != null) {
				buf.append("    graph <" + named_graph + ">").append("\n");
			}

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
	    // v = new ParserUtils().getResponseValues(v);
	    return new SortUtils().quickSort(v);
	}


	public String construct_inverse_simple_query(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?y_label ?y_code ?p_label ?p_code ?x_label ?x_code").append("\n");
		buf.append("{ ").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
	    // v = new ParserUtils().getResponseValues(v);
	    return new SortUtils().quickSort(v);
	}

	public static int TYPE_ROLE = 1;
	public static int TYPE_ASSOCIATION = 2;

	public String construct_relationship_query(String named_graph, int type) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?y_label ?y_code").append("\n");
		buf.append("{ ").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
	    // v = new ParserUtils().getResponseValues(v);
	    return new SortUtils().quickSort(v);
	}
	public String construct_source_code_query(String named_graph, String source_code) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?x_label ?x_code").append("\n");
        buf.append("{").append("\n");
        //buf.append("graph <" + named_graph + "> {").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}
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

		if (named_graph != null) {
			buf.append("}").append("\n");
		}

        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector source_code_query(String named_graph, String source_code) {
		String query = construct_source_code_query(named_graph, source_code);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			// v = new ParserUtils().getResponseValues(v);
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
        //buf.append("graph <" + named_graph + "> {").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x :NHC0 ?x_code .").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("?z_axiom owl:annotatedSource ?x .").append("\n");
        buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
        buf.append("?p rdfs:label ?p_label .").append("\n");
        buf.append("?z_axiom owl:annotatedTarget ?axiom_target .").append("\n");
        buf.append("?z_axiom ?y ?z .").append("\n");
        buf.append("?y rdfs:label ?y_label ").append("\n");

		if (named_graph != null) {
			buf.append("}").append("\n");
		}

        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector axiom_type_query(String named_graph) {
		String query = construct_axiom_type_query(named_graph);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			// v = new ParserUtils().getResponseValues(v);
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
 		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

        //buf.append("graph <" + named_graph + "> {").append("\n");
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

		if (named_graph != null) {
			buf.append("}").append("\n");
		}

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
			// v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_mapping_query(String named_graph, String code) {
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
        buf.append("SELECT distinct ?x ?p ?y").append("\n");
        buf.append("{ ").append("\n");
 		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> ").append("\n");
		}

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
			// v = new ParserUtils().getResponseValues(v);
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
		buf.append(prefixes);
        buf.append("SELECT distinct ?x ?p ?y").append("\n");
        buf.append("{ ").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
			// v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public static Vector formatOutput(Vector v) {
		return ParserUtils.formatOutput(v);
	}

    public void runQuery(String query_file) {
		String query = getQuery(query_file);
        Vector v = execute(query_file);
        v = formatOutput(v);
        Utils.saveToFile("output_" + query_file, v);
	}

	public Vector outbound_role_query(String named_graph, String propertyName) {
		return getRoleSourcesAndTargets(named_graph, propertyName);
	}

    public Vector get_synonyms_query(String named_graph) {
		return get_property_query(named_graph, "FULL_SYN");
	}

	public String construct_get_property_query(String named_graph, String propertyName) {
		StringBuffer buf = new StringBuffer();
		String prefixes = getPrefixes();
        buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
			// v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_get_association_query(String named_graph, String associationName) {
		StringBuffer buf = new StringBuffer();
        buf.append(prefixes);

		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
			// v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_source_code_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?z_target ?z1 ?z2").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
			// v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_property_qualifier_query(String named_graph) {
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);

        buf.append("").append("\n");
        buf.append("SELECT distinct ?p_label ?y_label").append("\n");
        buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
			// v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}


    public String construct_root_query(String named_graph) {
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select ?s_label ?s_code").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
        //buf.append("from <" + named_graph + ">").append("\n");
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
		// v = new ParserUtils().getResponseValues(v);
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    { ").append("\n");
		buf.append("    	{").append("\n");
		buf.append("            ?z_axiom a owl:Axiom .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");

		if (code != null) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}

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
		return buf.toString();
	}

	public Vector getAxioms(String named_graph, String code, String propertyName) {
		String query = construct_axiom_query(named_graph, code, propertyName);
		Vector v = executeQuery(query);
		if (v != null) {
			// v = new ParserUtils().getResponseValues(v);
		}
		return v;
	}

	public Vector getAxioms(String named_graph, String code) {
		Vector v = executeQuery(construct_axiom_query(named_graph, code));
		if (v != null) {
			// v = new ParserUtils().getResponseValues(v);
		}
		return v;
	}

	public Vector getAxiomsByCode(String named_graph, String code, String propertyName) {
		String query = construct_axiom_query(named_graph, code, propertyName);
		Vector v = executeQuery(query);
		// v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public Vector getAxiomsByCode(String named_graph, String code) {
		Vector v = executeQuery(construct_axiom_query(named_graph, code));
		// v = new ParserUtils().getResponseValues(v);
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		buf.append("ORDER BY ?z_axiom").append("\n");
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
		buf.append("ORDER BY ?z_axiom").append("\n");
		return buf.toString();
	}


	public String construct_property_query(String named_graph, String property_code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ? x_code ?a_label ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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

	public Vector getConceptsContainingProperty(String named_graph, String prop_label) {
		String prop_code = null;
		Vector properties = getSupportedProperties(named_graph);
        for (int i=0; i<properties.size(); i++) {
			String str = (String) properties.elementAt(i);
			Vector u = StringUtils.parseData(str, '|');
			String label = (String) u.elementAt(0);
			if (label.compareTo(prop_label) == 0) {
				prop_code = (String) u.elementAt(1);
			    break;
			}
		}
		Vector w = getConceptsWithProperty(named_graph, prop_code);
		w = gov.nih.nci.evs.restapi.util.SortUtils.multiValuedSortBy(w, 1);
        return w;
	}


	public Vector getConceptsWithProperty(String named_graph, String prop_code) {
		String query = construct_property_query(named_graph, prop_code);
		Vector v = executeQuery(query);
		// v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_concept_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT ?x_code ?x_label").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		// v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_association_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT ?x_code ?x_label ?a_code ?a_label ?z_code ?z_label").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		// v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_object_property_query(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_code ?x_label ?a_code ?a_label ?z_code ?z_label").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		// v = new ParserUtils().getResponseValues(v);
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
		String prefixes = getPrefixes();
		buf.append(prefixes);
		buf.append("SELECT ?a_code ?a_label ?p_value").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		// v = new ParserUtils().getResponseValues(v);
		return v;
	}


	public String construct_property_definition_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		String prefixes = getPrefixes();
		buf.append(prefixes);
		buf.append("SELECT ?a_code ?a_label ?p_value").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
		// v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_supported_sources_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		String prefixes = getPrefixes();
		buf.append(prefixes);
		buf.append("SELECT distinct ?y_label ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}


	public String construct_supported_property_qualifiers(String named_graph, String property_name) {
		StringBuffer buf = new StringBuffer();
		String prefixes = getPrefixes();
		buf.append(prefixes);
		buf.append("SELECT distinct ?y_label").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?p rdfs:label '" + property_name + "'^^xsd:string .").append("\n");
		buf.append("?z_axiom ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");

		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getSupportedPropertyQualifiers(String named_graph, String property_name) {
		String query = construct_supported_property_qualifiers(named_graph, property_name);
		Vector v = executeQuery(query);
		// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_supported_property_qualifier_values(String named_graph, String property_name, String qualifier_name) {
		StringBuffer buf = new StringBuffer();
		String prefixes = getPrefixes();
		buf.append(prefixes);
		buf.append("SELECT distinct ?z").append("\n");
		buf.append("{").append("\n");
		//buf.append("graph <" + named_graph + "> {").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?p rdfs:label '" + property_name + "'^^xsd:string .").append("\n");
		buf.append("?z_axiom ?y ?z .").append("\n");
		buf.append("?y rdfs:label '" + qualifier_name + "'^^xsd:string .").append("\n");
		//buf.append("}").append("\n");
		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}
		buf.append("}").append("\n");
		//buf.append("LIMIT 1000").append("\n");
		return buf.toString();
	}

	public Vector getSupportedPropertyQualifierValues(String named_graph, String property_name, String qualifier_name) {
		String query = construct_supported_property_qualifier_values(named_graph, property_name, qualifier_name);
		Vector v = executeQuery(query);
		// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public String construct_supported_property_qualifiers(String named_graph) {
		StringBuffer buf = new StringBuffer();
		String prefixes = getPrefixes();
		buf.append(prefixes);
		buf.append("SELECT distinct ?property_name ?p_code ?qualifier_name ?y_code").append("\n");
		buf.append("{").append("\n");
		//buf.append("graph <" + named_graph + "> {").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append("?p rdfs:label ?property_name .").append("\n");
		buf.append("?p :NHC0 ?p_code .").append("\n");
		buf.append("?z_axiom ?y ?z .").append("\n");
		buf.append("?y rdfs:label ?qualifier_name .").append("\n");
		buf.append("?y :NHC0 ?y_code .").append("\n");
		//buf.append("}").append("\n");
		if (named_graph != null) {
			buf.append("    }").append("\n");
     	        }
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getSupportedPropertyQualifierValues(String named_graph) {
		String query = construct_supported_property_qualifiers(named_graph);
		Vector v = executeQuery(query);
		// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

	public Vector findConceptsWithPropertyMatching(String named_graph, String property_name, String property_value) {
	    String query = generate_find_concepts_with_property_matching(named_graph, property_name, property_value);
	    Vector v = executeQuery(query);
	    // v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String generate_find_concepts_with_property_matching(String named_graph, String property_name, String property_value) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_code ?x_label ?y_label ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

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
	    // v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String generate_find_concepts_with_property(String named_graph, String property_name) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y_label ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
	    // v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
	    return v;
	}

	public String generate_find_matched_concepts(String named_graph, String target) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
	    // v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
	    return v;
	}

	public String generate_get_owl_classes(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		buf.append("SELECT f ?x_label ?x_code ?p_label ?p_code ?z_target ?y_label ?y_code ?z").append("\n");
		//buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?z_target ?y_label ?y_code ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		buf.append("ORDER BY ?z_axiom").append("\n");
		return buf.toString();
	}

	public Vector getPropertyQualifierValuesByCode(String named_graph, String code, String prop_label, String qualifier_label) {
		String query = construct_get_property_qualifier_values_by_code(named_graph, code, prop_label, qualifier_label);
		Vector v = executeQuery(query);
		// v = new ParserUtils().getResponseValues(v);
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		buf.append("ORDER BY ?z_axiom").append("\n");
		return buf.toString();
	}

	public Vector getAxiomsWithQualifierMatching(String named_graph, String code, String prop_label, String qualifier_label, String qualifier_value) {
		String query = construct_get_axioms_with_qualifier_matching(named_graph, code, prop_label, qualifier_label, qualifier_value);
		Vector v = executeQuery(query);
		// v = new ParserUtils().getResponseValues(v);
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
			// v = new ParserUtils().getResponseValues(v);
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

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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
		// v = new ParserUtils().getResponseValues(v);
		v = new SortUtils().quickSort(v);
		return v;
	}

    public static void test1(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
        String named_graph = args[1];
        //String queryfile = args[2];
        owlSPARQLUtils.set_named_graph(named_graph);
        Vector v = owlSPARQLUtils.getAllConceptProperties(named_graph);
        Utils.saveToFile("all_properties.txt", v);
        v = owlSPARQLUtils.getSupportedPropertyQualifierValues(named_graph);
        StringUtils.dumpVector(v);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }



    public Vector getDefinitions(String named_graph, String code, String prop_label) {
        Vector v = getPropertyQualifiersByCode(named_graph, code, prop_label);
        v = parser.getResponseValues(v);
		return parser.parseDefinitionData(v, prop_label);
	}

	public String construct_get_code_by_label(String named_graph, String label) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select ?x_code").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Class . ").append("\n");
		buf.append("	    ?x :NHC0 ?x_code .  ").append("\n");
		buf.append("	    ?x rdfs:label \"" + label + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getCodeByLabel(String named_graph, String label) {
        Vector v = executeQuery(construct_get_code_by_label(named_graph, label));
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}



	public String construct_axiom_query(String named_graph, String code, String propertyName, String qualifierName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT ?z_axiom ?x_label ?x_code ?p_label ?z_target ?y_label ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    { ").append("\n");
		buf.append("    	{").append("\n");
		buf.append("            ?z_axiom a owl:Axiom .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");

		if (code != null) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");

		if (propertyName != null) {
			buf.append("            ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            ?z_axiom ?y ?z . ").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");

		if (qualifierName != null) {
			buf.append("            ?y rdfs:label \"" + qualifierName + "\"^^xsd:string .").append("\n");
		}

		buf.append("        }").append("\n");
		buf.append("     }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAxioms(String named_graph, String code, String propertyName, String qualfierName) {
		String query = construct_axiom_query(named_graph, code, propertyName, qualfierName);
		Vector v = executeQuery(query);
		if (v != null) {
			// v = new ParserUtils().getResponseValues(v);
		}
		return v;
	}


	public String construct_get_associated_concepts(String named_graph, String code, String association, boolean outbound) {
		String named_graph_id = ":NHC0";
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?y_label ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("            ?z " + named_graph_id + " ?z_code .").append("\n");
		buf.append("            ?z rdfs:label ?z_label .").append("\n");

		if (code != null) {
			if (outbound) {
				buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			} else {
				buf.append("            ?z :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			}
		}

		if (association != null) {
			buf.append("            ?y rdfs:label " + "\"" + association + "\"^^xsd:string ").append("\n");
		}
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getValueSet(String named_graph, String vs_header_concept_code) {
		boolean formatted = true;
		return getValueSet(named_graph, vs_header_concept_code, formatted);
	}

	public Vector getValueSet(String named_graph, String vs_header_concept_code, boolean formatted) {
		String association = "Concept_In_Subset";
		boolean outbound = false;
		Vector v = getAssociatedConcepts(named_graph, vs_header_concept_code, association, outbound);
		// v = new ParserUtils().getResponseValues(v);
        Vector w = new Vector();
 	    for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (formatted) {
				line = gov.nih.nci.evs.restapi.util.StringUtils.formatAssociation(line);
			}
			w.add(line);
		}
 	    return w;
	}

	public String generate_get_distinct_property_values(String named_graph, String property_name) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?z").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
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

	public Vector getDistinctPropertyValues(String named_graph, String property_name) {
	    String query = generate_get_distinct_property_values(named_graph, property_name);
	    Vector v = executeQuery(query);
	    // v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
	    return v;
	}

	public String construct_get_concepts_with_properties(String named_graph, Vector prop_codes) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		StringBuffer selectStmt = new StringBuffer();
		selectStmt.append("SELECT ?x_code ?x_label ");
		for (int i=0; i<prop_codes.size(); i++) {
			int j = i+1;
		    selectStmt.append("?p" + j + "_code ?p" + j + "_label ?y" + j + "");
		    if (i<prop_codes.size()-1) {
				selectStmt.append(" ");
			}
		}
		String s = selectStmt.toString();
		buf.append(s);
		buf.append("{ ");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("	{");
		buf.append("		?x a owl:Class .");
		buf.append("                ?x :NHC0 ?x_code .");
		buf.append("                ?x rdfs:label ?x_label .");

		for (int i=0; i<prop_codes.size(); i++) {
			int j = i+1;
			String prop_code = (String) prop_codes.elementAt(i);

			buf.append("		?x ?p" + j + " ?y" + j + " .");
			buf.append("                ?p" + j + " :NHC0 \"" + prop_code + "\"^^xsd:string .");
			buf.append("                ?p" + j + " :NHC0 ?p" + j + "_code .");
			buf.append("                ?p" + j + " rdfs:label ?p" + j + "_label .");
		}

		buf.append("        }");
		buf.append("}");
		return buf.toString();
	}

	public Vector getConceptsWithProperties(String named_graph, Vector prop_codes) {
	    String query = construct_get_concepts_with_properties(named_graph, prop_codes);
	    Vector v = executeQuery(query);
	    // v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
	    return v;
	}

	public String construct_get_property_qualifiers(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);

        buf.append("select distinct ?p_label ?p_code ?q_label ?q_code").append("\n");
        buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}
		buf.append("                ?a a owl:Axiom .").append("\n");
		buf.append("                ?a owl:annotatedProperty ?p .").append("\n");
		buf.append("                ?p rdfs:label ?p_label .").append("\n");
		buf.append("                ?p :NHC0 ?p_code .").append("\n");
		buf.append("                ?a ?q ?q_value .").append("\n");
		buf.append("                ?q rdfs:label ?q_label .").append("\n");
		buf.append("                ?q :NHC0 ?q_code .").append("\n");

		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getSupportedPropertyQualifiers(String named_graph) {
	    String query = construct_get_property_qualifiers(named_graph);
	    Vector v = executeQuery(query);
	    // v = new ParserUtils().getResponseValues(v);
	    v = new SortUtils().quickSort(v);
	    return v;
	}

    public Vector get_subclasses_by_code(String namedGraph, String code) {
		Vector w = new Vector();
		Vector u = getSubclassesByCode(namedGraph, code);
		if (u != null && u.size() > 0) {
			int n = u.size()/2;
			for (int i=0; i<n; i++) {
				String s1 = (String) u.elementAt(i*2);
				String s2 = (String) u.elementAt(i*2+1);
				String t1 = parser.getValue(s1);
				String t2 = parser.getValue(s2);
				w.add(t1 + "|" + t2);
			}
		}
		return w;
	}

////////////////////getTransitiveClosure//////////////////////////////////////////////////////////////////

	public Vector getTree(String root) {
		return getDescendants(this.named_graph, root, true);
	}

	public Vector getDescendants(String root) {
		return getDescendants(root, false);
	}

	public Vector getDescendants(String root, boolean includeRoot) {
		return getDescendants(this.named_graph, root, includeRoot);
	}

	public Vector getDescendants(String namedGraph, String root, boolean includeRoot) {
	    Vector w = new Vector();
	    Stack stack = new Stack();
	    stack.push(root);
	    while (!stack.isEmpty()) {
			String code = (String) stack.pop();
			w.add(code);
			Vector v = getSubclassesByCode(namedGraph, code);
			if (v != null && v.size() > 0) {
				for (int i=0; i<v.size(); i++) {
					String line = (String) v.elementAt(i);
					Vector u = StringUtils.parseData(line, '|');
					String s1 = (String) u.elementAt(0);
					String s2 = (String) u.elementAt(1);
					stack.push(s2);
				}
			}
		}
		if (!includeRoot) {
			w.remove(0);
		}
		return w;
	}

	public Vector getAncestors(String root) {
		return getAncestors(root, false);
	}

	public Vector getAncestors(String root, boolean includeRoot) {
		return getAncestors(this.named_graph, root, includeRoot);
	}

	public Vector getAncestors(String namedGraph, String root, boolean includeRoot) {
	    Vector w = new Vector();
	    Stack stack = new Stack();
	    stack.push(root);
	    while (!stack.isEmpty()) {
			String code = (String) stack.pop();
			w.add(code);
			Vector v = getSuperclassesByCode(namedGraph, code);
			if (v != null && v.size() > 0) {
				for (int i=0; i<v.size(); i++) {
					String line = (String) v.elementAt(i);
					Vector u = StringUtils.parseData(line, '|');
					String s1 = (String) u.elementAt(0);
					String s2 = (String) u.elementAt(1);
					stack.push(s2);
				}
			}
		}
		if (!includeRoot) {
			w.remove(0);
		}
		return w;
	}

	public Vector removeDuplicates(Vector codes) {
		HashSet hset = new HashSet();
		Vector w = new Vector();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			if (!hset.contains(code)) {
				hset.add(code);
				w.add(code);
			}
		}
		return w;
	}

  	public Vector getTransitiveClosure(String root) {
		return getTransitiveClosure(this.named_graph, root);
	}

  	public Vector getTransitiveClosure(String namedGraph, String root) {
		return getTransitiveClosure(namedGraph, root, true);
	}

  	public Vector getTransitiveClosure(String namedGraph, String root, boolean traverseDown) {
	    Vector w = new Vector();
	    Vector v = new Vector();
	    Stack stack = new Stack();
	    String label = getLabel(root);
	    stack.push(label + "|" + root);
	    while (!stack.isEmpty()) {
			String line = (String) stack.pop();
			w.add(line);
			Vector u = StringUtils.parseData(line, '|');
			String s1 = (String) u.elementAt(0);
			String s2 = (String) u.elementAt(1);
			if (traverseDown) {
				v = getSubclassesByCode(namedGraph, s2);
			} else {
				v = getSuperclassesByCode(namedGraph, s2);
			}
			if (v != null && v.size() > 0) {
				for (int i=0; i<v.size(); i++) {
					line = (String) v.elementAt(i);
					stack.push(line);
				}
			}
		}
		w = removeDuplicates(w);
		return w;
	}

    public void printTree(String parent_child_file) {
		Vector v = Utils.readFile(parent_child_file);
		HierarchyHelper hh = new HierarchyHelper(v, 1);
		hh.printTree();
	}

    public void printTree(Vector parent_child_vec) {
		HierarchyHelper hh = new HierarchyHelper(parent_child_vec, 1);
		hh.printTree();
	}

	public String construct_get_concepts_with_properties(String named_graph, String code, Vector propertyLabels) {
		String named_graph_id = ":NHC0";
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		StringBuffer select_buf = new StringBuffer();
		buf.append("SELECT distinct ?x_code ?x_label");
		for (int i=0; i<propertyLabels.size(); i++) {
			int j = i+1;
			String propertyLabel = (String) propertyLabels.elementAt(i);
            select_buf.append(" " + "?y" + j + "_label");
            select_buf.append(" " + "?y" + j + "_value");
        }
        String select_stmt = select_buf.toString();
		buf.append(select_stmt).append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("            ?x a owl:Class .").append("\n");

		if (code != null) {
			buf.append("            ?x " + named_graph_id + " \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");

		for (int i=0; i<propertyLabels.size(); i++) {
			int j = i+1;
			String propertyLabel = (String) propertyLabels.elementAt(i);

            buf.append("            ?y" + j + " rdfs:label " + "?y" + j + "_label .").append("\n");
            buf.append("            ?y" + j + " rdfs:label " + " \"" + propertyLabel + "\"^^xsd:string .").append("\n");
            buf.append("            ?x " + "?y" + j + " ?y" + j + "_value .").append("\n");
        }
		//buf.append("    }").append("\n");
		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}

		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsWithProperties(String named_graph, String code, Vector propertyLabels) {
	    String query = construct_get_concepts_with_properties(named_graph, code, propertyLabels);
	    Vector v = executeQuery(query);
	    // v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String getPropertyLabel(String code) {
		String label = (String) propertyCode2labelHashMap.get(code);
		return label;
	}

	public String construct_get_known_property_qualifier_values(String named_graph, String code, String propertyLabel,
	              String qualifierCode, String qualifierValue, String qualifier_code) {
		String named_graph_id = ":NHC0";
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		StringBuffer select_buf = new StringBuffer();

        if (qualifierCode != null && qualifierValue != null) {
			if (code == null) {
				select_buf.append("SELECT distinct ?y_label ?y_code ?y_value ?y2_label ?y2_code ?y2_value");
			} else {
				select_buf.append("SELECT distinct ?x_code ?x_label ?y_label ?y_code ?y_value ?y2_label ?y2_code ?y2_value");
			}
	    } else {
			if (code == null) {
				select_buf.append("SELECT distinct ?y2_label ?y2_code ?y2_value");
			} else {
				select_buf.append("SELECT distinct ?x_code ?x_label ?y2_label ?y2_code ?y2_value");
			}
		}

        String select_stmt = select_buf.toString();
		buf.append(select_stmt).append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");

		if (code != null) {
			buf.append("            ?x " + named_graph_id + " \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label " + "\"" + propertyLabel + "\"^^xsd:string .").append("\n");
		buf.append("            ?z_axiom ?y ?z .").append("\n");

        if (qualifierCode != null && qualifierValue != null) {
			buf.append("            ?y" + " :NHC0 \"" + qualifierCode + "\"^^xsd:string .").append("\n");
			buf.append("            ?y" + " :NHC0 ?y_code .").append("\n");
			buf.append("            ?y" + " rdfs:label ?y_label .").append("\n");

			buf.append("            ?z_axiom ?y \"" + qualifierValue + "\"^^xsd:string .").append("\n");
			buf.append("            ?z_axiom ?y ?y_value .").append("\n");
		}

		buf.append("            ?y2" + " :NHC0 \"" + qualifier_code + "\"^^xsd:string .").append("\n");
		buf.append("            ?y2" + " :NHC0 ?y2_code .").append("\n");
		buf.append("            ?y2" + " rdfs:label ?y2_label .").append("\n");
		buf.append("            ?z_axiom ?y2 ?y2_value ").append("\n");


		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}

		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getKnownPropertyQualifierValues(String named_graph, String code, String propertyLabel,
	              String qualifierCode, String qualifierValue, String qualifier_code) {

	    String query = construct_get_known_property_qualifier_values(named_graph, code, propertyLabel,
	              qualifierCode, qualifierValue, qualifier_code);
	    Vector v = executeQuery(query);
	    // v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String construct_get_concepts_with_association_and_properties_matching(
		String named_graph,
	    String associationLabel,
	    String associationTargetCode,
	    String propertyLabel,
	    String property2Label, //FULL_SYN
	    Vector property2QualifierCodes) {

		String named_graph_id = ":NHC0";
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		StringBuffer select_buf = new StringBuffer();
        select_buf.append("select distinct ?a_value_label ?a_value_code ?x_label ?x_code ?p_label ?p_code ?p_value ?p2_label ?p2_code ?term_name");

        for (int i=0; i<property2QualifierCodes.size(); i++) {
			int j = i+1;
			select_buf.append(" ?y" + j + "_code ?y" + j + "_label ?y" + j + "_value");
		}

        String select_stmt = select_buf.toString();
		buf.append(select_stmt).append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}


		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?x ?p ?p_value .").append("\n");
		buf.append("            ?p" + " :NHC0 ?p_code .").append("\n");
		buf.append("            ?p" + " rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label " + "\"" + propertyLabel + "\"^^xsd:string .").append("\n");
		buf.append("            ?x ?a ?a_value .").append("\n");
		buf.append("            ?a_value" + " :NHC0 ?a_value_code .").append("\n");
		buf.append("            ?a_value rdfs:label ?a_value_label .").append("\n");
		buf.append("            ?a_value" + " :NHC0 \"" + associationTargetCode + "\"^^xsd:string .").append("\n");
		buf.append("            ?x ?p2 ?p2_value .").append("\n");
		buf.append("            ?p2" + " :NHC0 ?p2_code .").append("\n");
		buf.append("            ?p2" + " rdfs:label ?p2_label .").append("\n");
		buf.append("            ?p2 rdfs:label " + "\"" + property2Label + "\"^^xsd:string .").append("\n");

		buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p2 .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?term_name .").append("\n");

		for (int i=0; i<property2QualifierCodes.size(); i++) {
			int j = i+1;
			String property2QualifierCode = (String) property2QualifierCodes.elementAt(i);
			buf.append("            ?z_axiom ?y" + j + " ?y" + j + "_value .").append("\n");
			buf.append("            ?y" + j + " :NHC0 ?y" + j + "_code .").append("\n");
			buf.append("            ?y" + j + " :NHC0 \"" + property2QualifierCode + "\"^^xsd:string .").append("\n");
			buf.append("            ?y" + j + " rdfs:label " + "?y" + j +"_label .").append("\n");
		}

		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsWithAssociationAndPropertiesMatching(
		String named_graph,
	    String associationLabel,
	    String associationTargetCode,
	    String propertyLabel,
	    String property2Label,
	    Vector property2QualifierCodes) {
	    String query = construct_get_concepts_with_association_and_properties_matching(
			named_graph,
			associationLabel,
			associationTargetCode,
			propertyLabel, //Contributing_Source
			property2Label, //FULL_SYN
			property2QualifierCodes);
	    Vector v = executeQuery(query);
	    // v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String construct_get_concepts_with_property_and_qualifiers_matching(String named_graph, String code, String propertyLabel,
	              Vector qualifierCodes, Vector qualifierValues) {
		String named_graph_id = ":NHC0";
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		StringBuffer select_buf = new StringBuffer();
		buf.append("SELECT distinct ?x_code ?term_name ?p_label");
		for (int i=0; i<qualifierCodes.size(); i++) {
			int j = i+1;
			String qualifierCode = (String) qualifierCodes.elementAt(i);
			String qualifierValue = (String) qualifierValues.elementAt(i);
            select_buf.append(" " + "?y" + j + "_code");
            select_buf.append(" " + "?y" + j + "_value");
        }
        String select_stmt = select_buf.toString();
		buf.append(select_stmt).append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}


		buf.append("            ?x a owl:Class .").append("\n");

		if (code != null) {
			buf.append("            ?x " + named_graph_id + " \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label " + " \"" + propertyLabel + "\"^^xsd:string .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?term_name .").append("\n");
		buf.append("            ?z_axiom ?y ?z .").append("\n");
		for (int i=0; i<qualifierCodes.size(); i++) {
			int j = i+1;
			String qualifierCode = (String) qualifierCodes.elementAt(i);
			String qualifierValue = (String) qualifierValues.elementAt(i);
			buf.append("            ?z_axiom ?y" + j + " \"" + qualifierValue + "\"^^xsd:string .").append("\n");
			buf.append("            ?z_axiom ?y" + j + " ?y" + j + "_value .").append("\n");
			buf.append("            ?y" + j + " :NHC0 \"" + qualifierCode + "\"^^xsd:string .").append("\n");
			buf.append("            ?y" + j + " :NHC0 " + "?y" + j + "_code .").append("\n");
		}
		if (named_graph != null) {
			buf.append("    }").append("\n");
        }
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsWithPropertyAndQualifiersMatching(String named_graph, String code, String propertyLabel,
	              Vector qualifierCodes, Vector qualifierValues) {
	    String query = construct_get_concepts_with_property_and_qualifiers_matching(named_graph, code, propertyLabel,
	                   qualifierCodes, qualifierValues);
	    Vector v = executeQuery(query);
	    // v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String construct_get_valueset_concepts_with_properties_matching(
		String named_graph,
		String headerConceptCode,
		String propertyLabel,
		Vector qualifierCodes
		) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		StringBuffer select_buf = new StringBuffer();
		select_buf.append("SELECT distinct ?x_code ?x_label ?y_code ?y_label ?p1_label ?p1_value ");

		if (qualifierCodes != null) {
			select_buf.append("?p2_label ?term_name");
			for (int i=0; i<qualifierCodes.size(); i++) {
				int j = i+1;
				select_buf.append(" ?y" + j + "_code ?y" + j + "_value");
			}
		}
		String select_stmt = select_buf.toString();
        buf.append(select_stmt).append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 \"" + headerConceptCode + "\"^^xsd:string .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y ?a ?x .").append("\n");
		buf.append("            ?a :NHC0 \"A8\"^^xsd:string .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y ?p1 ?p1_value .").append("\n");
		buf.append("            ?p1 rdfs:label ?p1_label .").append("\n");
		buf.append("            ?p1 rdfs:label \"" + propertyLabel + "\"^^xsd:string .").append("\n");
		buf.append("").append("\n");

		if (qualifierCodes != null) {
			buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
			buf.append("            ?z_axiom owl:annotatedSource ?y .").append("\n");
			buf.append("            ?z_axiom owl:annotatedProperty ?p2 .").append("\n");
			buf.append("            ?p2 rdfs:label ?p2_label .").append("\n");
			buf.append("            ?p2 rdfs:label \"FULL_SYN\"^^xsd:string .").append("\n");
			buf.append("            ?z_axiom owl:annotatedTarget ?term_name .").append("\n");
			buf.append("          ").append("\n");
			for (int i=0; i<qualifierCodes.size(); i++) {
				int j = i+1;
				String qualifierCode = (String) qualifierCodes.elementAt(i);
				buf.append("            ?z_axiom ?y" + j + " ?y" + j + "_value .").append("\n");
				buf.append("            ?y" + j + " :NHC0 \"" + qualifierCode + "\"^^xsd:string .").append("\n");
				buf.append("            ?y" + j + " :NHC0 ?y" + j + "_code .").append("\n");
			}
		}

		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}

		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getValuesetConceptsWithPropertiesMatching(
		String named_graph,
		String headerConceptCode,
		String propertyLabel,
		Vector qualifierCodes) {
	    String query = construct_get_valueset_concepts_with_properties_matching(named_graph, headerConceptCode, propertyLabel, qualifierCodes);
	    Vector v = executeQuery(query);
	    // v = new ParserUtils().getResponseValues(v);
	    return v;
	}

	public String construct_get_value_set_contributing_sources(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?y_code ?y_label ?p1_code ?p1_label ?p1_value").append("\n");
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y ?p0 \"Yes\"^^xsd:string .").append("\n");
		buf.append("            ?p0 rdfs:label ?p0_label .").append("\n");
		buf.append("            ?p0 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x ?a ?y .").append("\n");
		buf.append("            ?a :NHC0 \"A8\"^^xsd:string .").append("\n");
		buf.append(" ").append("\n");
		buf.append("            ?y ?p1 ?p1_value .").append("\n");
		buf.append("            ?p1 rdfs:label ?p1_label .").append("\n");
		buf.append("            ?p1 rdfs:label \"Contributing_Source\"^^xsd:string .").append("\n");

		if (named_graph != null) {
			buf.append("    }").append("\n");
        }

		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getValueSetContributingSources(String named_graph) {
		String query = construct_get_value_set_contributing_sources(named_graph);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_association_sources_and_targets(String named_graph, String associationName, boolean code_only) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (code_only) {
			buf.append("SELECT distinct ?x_code ?a_label ?y_code ").append("\n");
		} else {
		    buf.append("SELECT distinct ?x_label ?x_code ?a_label ?y_label ?y_code ").append("\n");
		}

		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x ?a ?y .").append("\n");
		buf.append("            ?a rdfs:label \"" + associationName + "\"^^xsd:string .").append("\n");
		buf.append("            ?a rdfs:label ?a_label .").append("\n");

		if (named_graph != null) {
			buf.append("    }").append("\n");
        }

		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector getAssociationSourcesAndTargets(String named_graph, String associationName) {
		boolean code_only = false;
		return getAssociationSourcesAndTargets(named_graph, associationName, code_only);
	}

	public Vector getAssociationSourcesAndTargets(String named_graph, String associationName, boolean code_only) {
		String query = construct_get_association_sources_and_targets(named_graph, associationName, code_only);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_ontology_data(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT ?o ?date ?version ?lang ?comment").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("").append("\n");
		buf.append("            ?o a owl:Ontology .").append("\n");
		buf.append("            ?o dc:date ?date .").append("\n");
		buf.append("            ?o owl:versionInfo ?version .").append("\n");
		buf.append("            ?o protege:defaultLanguage ?lang .").append("\n");
		buf.append("            ?o rdfs:comment ?comment .").append("\n");

		if (named_graph != null) {
			buf.append("    }").append("\n");
        }

		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("    ").append("\n");
		return buf.toString();
	}


	public Vector getOntologyData(String named_graph) {
		String query = construct_get_ontology_data(named_graph);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_property_values(String named_graph, String propertyName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?p_label ?p_value").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?p a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?p ?p_value .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");

		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}

		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getAvailablePropertyValues(String named_graph, String propertyName) {
		String query = construct_get_property_values(named_graph, propertyName);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_axioms(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?z_axiom ?x_code ?x_label ?p_code ?p_label ?z_target ?q_code ?q_label ?q_value").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x  :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            ?z_axiom ?q ?q_value .").append("\n");
		buf.append("            ?q rdfs:label ?q_label .").append("\n");
		buf.append("            ?q :NHC0 ?q_code .").append("\n");

		if (named_graph != null) {
			buf.append("    }").append("\n");
     	}

		buf.append("}").append("\n");
		buf.append("ORDER BY ?z_axiom").append("\n");
		return buf.toString();
	}


	public Vector get_axioms_by_code(String named_graph, String code) {
		String query = construct_get_axioms(named_graph, code);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}


	public String construct_get_axiom_annotated_properties(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?p_code ?p_label ").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");

		if (named_graph != null) {
			buf.append("    }").append("\n");
        }

		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector get_axiom_annotated_properties(String named_graph) {
		String query = construct_get_axiom_annotated_properties(named_graph);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_association(String named_graph, String code, String propertyName, boolean outbound) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?y_label ?y_code").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}


		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		if (outbound) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		if (!outbound) {
			buf.append("            ?y :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
	    }
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?p a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?p ?y .").append("\n");
		buf.append("            ?p :NHC0 ?p_code .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		if (propertyName != null) {
			buf.append("            ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
		}

		if (named_graph != null) {
			buf.append("    }").append("\n");
        }
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getAssociation(String named_graph, String code, String propertyName, boolean outbound) {
		String query = construct_get_association(named_graph, code, propertyName, outbound);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}



	public String construct_get_presentations(String named_graph, String code, Vector propertyNames) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?x_code ?x_label ?z_target").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + "> {").append("\n");
		}

		buf.append("            {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");

		if (code != null) {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label \"FULL_SYN\"^^xsd:string .").append("\n");
		buf.append("            ?z_axiom owl:annotatedTarget ?z_target .").append("\n");
		buf.append("            }").append("\n");

		for (int i=0; i<propertyNames.size(); i++) {
			String propertyName = (String) propertyNames.elementAt(i);
			buf.append("            UNION ").append("\n");
			buf.append("            {").append("\n");
			buf.append("            ?x a owl:Class .").append("\n");
			buf.append("            ?x :NHC0 ?x_code .").append("\n");
			if (code != null) {
				buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			}
			buf.append("            ?x rdfs:label ?x_label .").append("\n");
			buf.append("            ").append("\n");
			buf.append("            ?x ?p ?z_target.").append("\n");
			buf.append("            ?p rdfs:label ?p_label .").append("\n");
			buf.append("            ?p rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
			buf.append("            }").append("\n");
		}
		if (named_graph != null) {
			buf.append("    }").append("\n");
        }

		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getPresentations(String named_graph, String code, Vector propertyNames) {
        String query = construct_get_presentations(named_graph, code, propertyNames);
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}


	public String construct_get_value_set_data(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?y_label ?y_code ?p0_label ?x_label ?x_code ?p1_label ?p1_value ?p2_label ?p2_value ").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

		buf.append("where  { ").append("\n");
		buf.append("                ?y a owl:Class .").append("\n");
		buf.append("                ?y :NHC0 ?y_code .").append("\n");
		buf.append("                ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p0 a owl:AnnotationProperty .").append("\n");
		buf.append("                ?p0 :NHC0 ?p0_code .").append("\n");
		buf.append("                ?p0 rdfs:label ?p0_label .").append("\n");
		buf.append("                ?p0 rdfs:label \"Concept_In_Subset\"^^xsd:string .  ").append("\n");
		buf.append(" ").append("\n");
		buf.append("                ?x a owl:Class .").append("\n");
		buf.append("                ?x :NHC0 ?x_code .").append("\n");
		buf.append("                ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?y ?p0 ?x .   ").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p1 a owl:AnnotationProperty .").append("\n");
		buf.append("                ?p1 :NHC0 ?p1_code .").append("\n");
		buf.append("                ?p1 rdfs:label ?p1_label .").append("\n");
		buf.append("                ?p1 rdfs:label \"Contributing_Source\"^^xsd:string .").append("\n");

		buf.append("                OPTIONAL {").append("\n");
		buf.append("                ?x  ?p1 ?p1_value .").append("\n");
		buf.append("                }").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
		buf.append("                ?p2 :NHC0 ?p2_code .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("                ?p2 rdfs:label \"Publish_Value_Set\"^^xsd:string .").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?x ?p2 ?p2_value .").append("\n");
		buf.append("                ?x ?p2 \"Yes\"^^xsd:string .").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getValueSetData(String named_graph) {
		Vector v = executeQuery(construct_get_value_set_data(named_graph));
		// v = new ParserUtils().getResponseValues(v);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.endsWith("null|Yes")) {
				Vector u = StringUtils.parseData(line, '|');
				String name = (String) u.elementAt(0);
				String code = (String) u.elementAt(1);
				String Concept_In_Subset = (String) u.elementAt(2);
				String subset_name = (String) u.elementAt(3);
				String subset_code = (String) u.elementAt(4);
				String Contributing_Source = (String) u.elementAt(5);
				String Publish_Value_Set = (String) u.elementAt(6);
				String Contributing_Source_value = (String) u.elementAt(7);
				String Publish_Value_Set_value = (String) u.elementAt(8);
				String t = name + "|" + code + "|" + Concept_In_Subset + "|" + subset_name + "|" + subset_code
				    + "|Contributing_Source|No External Source|"
				    + Publish_Value_Set + "|" + Publish_Value_Set_value;
				w.add(t);
			} else {
				w.add(line);
			}
		}
		return w;
	}

	public String construct_get_datatypes(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select ?dt ?element ?elementType ").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("where  { ").append("\n");
		buf.append("   ?dt a rdfs:Datatype ;").append("\n");
		buf.append("   owl:oneOf/rdf:rest*/rdf:first ?element .").append("\n");
		buf.append("   bind(datatype(?element) as ?elementType)").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

    public Vector getDatatypes(String named_graph) {
	    String query = construct_get_datatypes(named_graph);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_cdisc_submission_value(String named_graph, String subsetCode, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x2_code ?x2_label ?x1_code ?x1_label ?a1_target ?a2_target ?q7_value").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("where  { ").append("\n");
		buf.append("            	?x1 a owl:Class .").append("\n");
		buf.append("            	?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("            	?x1 :NHC0 \"" + subsetCode + "\"^^xsd:string .").append("\n");
		buf.append("            	?x1 rdfs:label ?x1_label .").append("\n");
		buf.append("           	").append("\n");
		buf.append("                ?x2 a owl:Class .").append("\n");
		buf.append("             	?x2 :NHC0 ?x2_code .").append("\n");
		buf.append("             	?x2 :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            	?x2 rdfs:label ?x2_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p4 .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a1_target .").append("\n");
		buf.append("                ?p4 :NHC0 \"P90\"^^xsd:string .").append("\n");
		buf.append("                ?a1 ?q3 \"NCI\"^^xsd:string .").append("\n");
		buf.append("                ?q3 :NHC0 \"P384\"^^xsd:string .").append("\n");
		buf.append("                ?a1 ?q4 \"AB\"^^xsd:string .").append("\n");
		buf.append("                ?q4 :NHC0 \"P383\"^^xsd:string .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?a2 a owl:Axiom .").append("\n");
		buf.append("                ?a2 owl:annotatedSource ?x2 .").append("\n");
		buf.append("                ?a2 owl:annotatedProperty ?p5 .").append("\n");
		buf.append("                ?a2 owl:annotatedTarget ?a2_target .").append("\n");
		buf.append("                ?p5 :NHC0 \"P90\"^^xsd:string .").append("\n");
		buf.append("                ?a2 ?q5 \"CDISC\"^^xsd:string .").append("\n");
		buf.append("                ?q5 :NHC0 \"P384\"^^xsd:string .").append("\n");
		buf.append("                ?a2 ?q6 \"PT\"^^xsd:string .").append("\n");
		buf.append("                ?q6 :NHC0 \"P383\"^^xsd:string .").append("\n");
		buf.append("                ?a2 ?q7 ?q7_value .").append("\n");
		buf.append("                ?q7 :NHC0 \"P385\"^^xsd:string .   ").append("\n");
		buf.append("                ").append("\n");
		buf.append("                FILTER(str(?a1_target) = str(?q7_value))").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getCdiscSubmissionValue(String named_graph, String subsetCode, String code) {
		String query = construct_get_cdisc_submission_value(named_graph, subsetCode, code);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_subclasses(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("      ?x :NHC0 ?x_code .").append("\n");
		buf.append("      ?x rdfs:label ?x_label .").append("\n");
		buf.append("      ?y :NHC0 ?y_code .").append("\n");
		buf.append("      ?y rdfs:label ?y_label .").append("\n");
		buf.append("      ?y :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("      ?x (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?y . ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getSubclasses(String named_graph, String code) {
		String query = construct_get_subclasses(named_graph, code);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_superclasses(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?y_label ?y_code ").append("\n");
		buf.append("{").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("    {").append("\n");
		buf.append("      ?x :NHC0 ?x_code .").append("\n");
		buf.append("      ?x rdfs:label ?x_label .").append("\n");
		buf.append("      ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("      ?y :NHC0 ?y_code .").append("\n");
		buf.append("      ?y rdfs:label ?y_label .").append("\n");
		buf.append("      ?x (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?y . ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getSuperclasses(String named_graph, String code) {
		String query = construct_get_superclasses(named_graph, code);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_hierarchical_relationships(String named_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        //buf.append("select distinct ?x_label ?x_code ?y_label ?y_code").append("\n");
        //buf.append("select distinct ?x_code ?y_code").append("\n");
        buf.append("select distinct ?y_label ?y_code ?x_label ?x_code ").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

        buf.append("where  { ").append("\n");
        buf.append("    {").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?y a owl:Class .").append("\n");
        buf.append("                ?y :NHC0 ?y_code .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?x (rdfs:subClassOf|owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first) ?y .  ").append("\n");
        buf.append("    } UNION {").append("\n");
        buf.append("    ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?y a owl:Class .").append("\n");
        buf.append("                ?y :NHC0 ?y_code .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?x (rdfs:subClassOf/owl:intersectionOf/rdf:rest*/rdf:first) ?y .  ").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        buf.append("").append("\n");
        return buf.toString();
	}

	public String construct_get_semantictypes(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select ?element ").append("\n");
		//buf.append("from <" + named_graph + ">").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

		buf.append("where  { ").append("\n");
		buf.append("   ?dt a rdfs:Datatype .").append("\n");
		buf.append("   ?dt ?x ?x_value .").append("\n");
		buf.append("   ?x_value ?p ?e .").append("\n");
		buf.append("   ?x_value ?p \"Acquired Abnormality\" .").append("\n");
        buf.append("   ?dt owl:oneOf/rdf:rest*/rdf:first ?element .").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

    public Vector getSemanticTypes(String named_graph) {
	    String query = construct_get_semantictypes(named_graph);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_axiom_qualifier_values(String named_graph, String propertyCode, String qualifierCode) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?p2_label ?q1_label ?q1_value").append("\n");
		//buf.append("from <" + named_graph + ">").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("where  { ").append("\n");
		buf.append("                    ?x1 a owl:Class .").append("\n");
		buf.append("                    ?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("                    ?x1 rdfs:label ?x1_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a2_target .").append("\n");
		buf.append("                ?p2 :NHC0 \"" + propertyCode + "\"^^xsd:string .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("                ?q1 :NHC0 \"" + qualifierCode + "\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAxiomQualifierValues(String named_graph, String propertyCode, String qualifierCode) {
		String query = construct_get_axiom_qualifier_values(named_graph, propertyCode, qualifierCode);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_target_terminology(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?p2_label ?q1_label ?q1_value ?q2_label ?q2_value").append("\n");
		//buf.append("from <" + named_graph + ">").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("where  { ").append("\n");
		buf.append("                    ?x1 a owl:Class .").append("\n");
		buf.append("                    ?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("                    ?x1 rdfs:label ?x1_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a2_target .").append("\n");
		buf.append("                ?p2 :NHC0 \"P375\"^^xsd:string .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("                ?q1 :NHC0 \"P396\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("").append("\n");
		buf.append("OPTIONAL {").append("\n");
		buf.append("                ?p3 rdfs:label ?p3_label .").append("\n");
		buf.append("                ?q2 :NHC0 \"P397\"^^xsd:string .").append("\n");
		buf.append("                ?q3 rdfs:label ?q3_label .").append("\n");
		buf.append("                ?a1 ?q2 ?q2_value .").append("\n");
		buf.append("}                             ").append("\n");
		buf.append("").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getTargetTerminology(String named_graph) {
		String query = construct_get_target_terminology(named_graph);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_maps_to(String named_graph, String terminology, String terminologyVersion) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (terminologyVersion != null) {
			buf.append("select distinct ?x1_label ?x1_code ?p2_label ?a2_target ?q1_label ?q1_value ?q2_label ?q2_value ?q3_label ?q3_value ?q4_label ?q4_value ?q5_label ?q5_value").append("\n");
		} else {
			buf.append("select distinct ?x1_label ?x1_code ?p2_label ?a2_target ?q1_label ?q1_value ?q2_label ?q2_value ?q3_label ?q3_value ?q4_label ?q4_value").append("\n");
		}
		//buf.append("from <" + named_graph + ">").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
		buf.append("where  {").append("\n");
		buf.append("                ?x1 a owl:Class .").append("\n");
		buf.append("                ?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("                ?x1 rdfs:label ?x1_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a2_target .").append("\n");
		buf.append("                ?p2 :NHC0 \"P375\"^^xsd:string .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q1 :NHC0 \"P393\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q2 :NHC0 \"P394\"^^xsd:string .").append("\n");
		buf.append("                ?q2 rdfs:label ?q2_label .").append("\n");
		buf.append("                ?a1 ?q2 ?q2_value .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q3 :NHC0 \"P395\"^^xsd:string .").append("\n");
		buf.append("                ?q3 rdfs:label ?q3_label .").append("\n");
		buf.append("                ?a1 ?q3 ?q3_value .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q4 :NHC0 \"P396\"^^xsd:string .").append("\n");
		buf.append("                ?q4 rdfs:label ?q4_label .").append("\n");
		buf.append("                ?a1 ?q4 \"" + terminology + "\"^^xsd:string .").append("\n");
		buf.append("                ?a1 ?q4 ?q4_value .").append("\n");
		buf.append("").append("\n");
		if (terminologyVersion != null) {
			buf.append("                ?q5 :NHC0 \"P397\"^^xsd:string .").append("\n");
			buf.append("                ?q5 rdfs:label ?q5_label .").append("\n");
			buf.append("                ?a1 ?q5 \"" + terminologyVersion + "\"^^xsd:string .").append("\n");
			buf.append("                ?a1 ?q5 ?q5_value .").append("\n");
	    }
		buf.append("").append("\n");
		buf.append("}").append("\n");

		return buf.toString();
	}

	public Vector getMapsTo(String named_graph, String terminology, String terminologyVersion) {
		String query = construct_get_maps_to(named_graph, terminology, terminologyVersion);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

    public String getLatestVersionOfCodingScheme(String codingScheme) {
		/*
		HashMap nameVersion2NamedGraphMap = getNameVersion2NamedGraphMap();
		if (nameVersion2NamedGraphMap == null) return null;
		Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
		Vector versions = new Vector();
		while (it.hasNext()) {
			String nameVersion = (String) it.next();
			System.out.println(nameVersion);
			Vector u = StringUtils.parseData(nameVersion);
			String codingSchemeName = (String) u.elementAt(0);
			if (codingSchemeName.compareTo(codingScheme) == 0) {
				String version = (String) u.elementAt(1);
				versions.add(version);
			}
		}
		versions = new SortUtils().quickSort(versions);
        return (String) versions.elementAt(versions.size()-1);
        */
        return null;
	}

	public Vector getRestrictions(String named_graph) {
		return getRoles(named_graph, null);
	}

	public Vector getRoles(String named_graph) {
		return getRoles(named_graph, null);
	}

	public Vector getRoles(String named_graph, String code, boolean outbound) {
		if (outbound) {
			return getRoles(named_graph, code);
		} else {
			return getInverseRoles(named_graph, code);
		}
	}

	public String construct_get_axiom_data(String named_graph, String propertyCode) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?a1 ?x1_label ?x1_code ?p2_label ?p2_code ?a1_target ?q1_label ?q1_code ?q1_value").append("\n");
        //buf.append("from <" + named_graph + ">").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
        buf.append("where  { ").append("\n");
        buf.append("                ?x1 a owl:Class .").append("\n");
        buf.append("                ?x1 :NHC0 ?x1_code .").append("\n");
        buf.append("                ?x1 rdfs:label ?x1_label .").append("\n");
        buf.append("                ?a1 a owl:Axiom .").append("\n");
        buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
        buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
        buf.append("                ?a1 owl:annotatedTarget ?a1_target .").append("\n");
        buf.append("                ?p2 :NHC0 ?p2_code .").append("\n");
        buf.append("                ?p2 :NHC0 \"" + propertyCode + "\"^^xsd:string .").append("\n");
        buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
        buf.append("                ?q1 :NHC0 ?q1_code .").append("\n");
        buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
        buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getAxiomData(String named_graph, String propertyCode) {
        String query = construct_get_axiom_data(named_graph, propertyCode);
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getHierarchicalRelationships(String named_graph) {
        String query = construct_get_hierarchical_relationships(named_graph);
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        // v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getHierarchicalRelationships(String named_graph, boolean raw_data) {
		Vector v = executeQuery(construct_get_hierarchical_relationships(named_graph));
		if (raw_data) return v;
        // v = new ParserUtils().getResponseValues(v);
        v = new SortUtils().quickSort(v);
        return v;
	}


	public String construct_get_role_sources_and_targets(String named_graph, String associationName, boolean code_only) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (code_only) {
			buf.append("SELECT distinct ?x_code ?p_label ?y_code ").append("\n");
		} else {
		    buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
		}

        //buf.append("    graph <" + named_graph + "> ").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
        buf.append("    {").append("\n");
        buf.append("            ?x :NHC0 ?x_code .").append("\n");
        buf.append("            ?x rdfs:label ?x_label .").append("\n");
        buf.append("            ?y :NHC0 ?y_code .").append("\n");
        buf.append("            ?y rdfs:label ?y_label .").append("\n");
        buf.append("            ?p :NHC0 ?p_code .").append("\n");
        buf.append("            ?p rdfs:label ?p_label .").append("\n");
        buf.append("            ?x (rdfs:subClassOf|owl:equivalentClass|owl:unionOf/rdf:rest*/rdf:first|owl:intersectionOf/rdf:rest*/rdf:first)* ?rs .  ").append("\n");
        buf.append("            ?rs a owl:Restriction .").append("\n");
        buf.append("            ?rs owl:onProperty ?p .").append("\n");
        buf.append("            ?rs owl:someValuesFrom ?y .").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getRoleSourcesAndTargets(String named_graph, String associationName) {
		boolean code_only = false;
		return getRoleSourcesAndTargets(named_graph, associationName, code_only);
	}

	public Vector getRoleSourcesAndTargets(String named_graph, String associationName, boolean code_only) {
		String query = construct_get_role_sources_and_targets(named_graph, associationName, code_only);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		// v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_simple_roles(String named_graph, String code) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_code ?x_label ?p_code ?p_label ?y_code ?y_label").append("\n");
        buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
        buf.append("    {").append("\n");
        buf.append("            ?x :NHC0 ?x_code .").append("\n");
        if (code != null) {
        	buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
        buf.append("            ?x rdfs:label ?x_label .").append("\n");
        buf.append("            ?y :NHC0 ?y_code .").append("\n");
        buf.append("            ?y rdfs:label ?y_label .").append("\n");
        buf.append("            ?p :NHC0 ?p_code .").append("\n");
        buf.append("            ?p rdfs:label ?p_label .").append("\n");
        buf.append("            ?x rdfs:subClassOf ?rs .  ").append("\n");
        buf.append("            ?rs a owl:Restriction .").append("\n");
        buf.append("            ?rs owl:onProperty ?p .").append("\n");
        buf.append("            ?rs owl:someValuesFrom ?y .").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        buf.append("").append("\n");
        return buf.toString();
	}

	public String construct_get_labels(String named_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_code ?x_label").append("\n");
        buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}
        buf.append("    {").append("\n");
        buf.append("            {").append("\n");
        buf.append("            ?x a owl:Class .").append("\n");
        buf.append("            ?x :NHC0 ?x_code .").append("\n");
        buf.append("            ?x rdfs:label ?x_label .").append("\n");
        buf.append("            }").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getLabels(String named_graph) {
        String query = construct_get_labels(named_graph);
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        return new SortUtils().quickSort(v);
	}


	public String construct_get_roles(String named_graph, String code) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT ?relationship ?relationshipCode ?relationshipLabel ?relatedConcept ?relatedConceptLabel ?relatedConceptCode  ").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

        buf.append("  { ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      ?x_concept (owl:equivalentClass|rdfs:subClassOf) ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 ?relatedConceptCode . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("    UNION ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      ?x_concept owl:equivalentClass ?z . ").append("\n");
        buf.append("      ?z (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 ?relatedConceptCode . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("    UNION ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      ?x_concept rdfs:subClassOf ?z . ").append("\n");
        buf.append("      ?z (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 ?relatedConceptCode . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("    UNION ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      ?x_concept owl:equivalentClass ?z . ").append("\n");
        buf.append("      ?z (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/ ").append("\n");
        buf.append("        ( (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first | ").append("\n");
        buf.append("          (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/(owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first ").append("\n");
        buf.append("        ) ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 ?relatedConceptCode . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("    UNION ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      ?x_concept rdfs:subClassOf ?z . ").append("\n");
        buf.append("      ?z (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/ ").append("\n");
        buf.append("        ( (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first | ").append("\n");
        buf.append("          (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/(owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first ").append("\n");
        buf.append("        ) ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 ?relatedConceptCode . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("  } ").append("\n");
        String query = buf.toString();
        return query;
	}

	public Vector getRoles(String named_graph, String code) {
        String query = construct_get_roles(named_graph, code);
        Vector v = HTTPUtils.runQuery(restURL, username, password, query);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_inv_roles(String named_graph, String code) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT ?x_concept ?conceptCode ?conceptLabel ?relationship ?relationshipCode ?relationshipLabel   ").append("\n");

		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

        buf.append("  { ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 ?conceptCode . ").append("\n");
        buf.append("      ?x_concept rdfs:label ?conceptLabel . ").append("\n");
        buf.append("      ?x_concept (owl:equivalentClass|rdfs:subClassOf) ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("    UNION ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 ?conceptCode . ").append("\n");
        buf.append("      ?x_concept rdfs:label ?conceptLabel .").append("\n");
        buf.append("      ?x_concept owl:equivalentClass ?z . ").append("\n");
        buf.append("      ?z (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("    UNION ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 ?conceptCode . ").append("\n");
        buf.append("      ?x_concept rdfs:label ?conceptLabel .").append("\n");
        buf.append("      ?x_concept rdfs:subClassOf ?z . ").append("\n");
        buf.append("      ?z (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("    UNION ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 ?conceptCode . ").append("\n");
        buf.append("      ?x_concept rdfs:label ?conceptLabel .").append("\n");
        buf.append("      ?x_concept owl:equivalentClass ?z . ").append("\n");
        buf.append("      ?z (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/ ").append("\n");
        buf.append("        ( (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first | ").append("\n");
        buf.append("          (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/(owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first ").append("\n");
        buf.append("        ) ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("    UNION ").append("\n");
        buf.append("    { ").append("\n");
        buf.append("      ?x_concept a owl:Class . ").append("\n");
        buf.append("      ?x_concept :NHC0 ?conceptCode . ").append("\n");
        buf.append("      ?x_concept rdfs:label ?conceptLabel .").append("\n");
        buf.append("      ?x_concept rdfs:subClassOf ?z . ").append("\n");
        buf.append("      ?z (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/ ").append("\n");
        buf.append("        ( (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first | ").append("\n");
        buf.append("          (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/(owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first ").append("\n");
        buf.append("        ) ?rs . ").append("\n");
        buf.append("      ?rs a owl:Restriction . ").append("\n");
        buf.append("      ?rs owl:onProperty ?relationship . ").append("\n");
        buf.append("      ?rs owl:someValuesFrom ?relatedConcept . ").append("\n");
        buf.append("      ?relatedConcept a owl:Class . ").append("\n");
        buf.append("      OPTIONAL { ?relatedConcept rdfs:label ?relatedConceptLabel } . ").append("\n");
        buf.append("      ?relatedConcept :NHC0 \"" + code + "\" . ").append("\n");
        buf.append("      OPTIONAL { ?relationship rdfs:label ?relationshipLabel } . ").append("\n");
        buf.append("      OPTIONAL { ?relationship :NHC0 ?relationshipCode } . ").append("\n");
        buf.append("    } ").append("\n");
        buf.append("  } ").append("\n");
        String query = buf.toString();
        return query;
	}

	public Vector getInverseRoles(String named_graph, String code) {
        String query = construct_get_inv_roles(named_graph, code);
        Vector v = HTTPUtils.runQuery(restURL, username, password, query);
        return new SortUtils().quickSort(v);
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String restURL = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
	    OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(restURL, username, password);
	    owlSPARQLUtils.set_named_graph(namedGraph);
        String code = "C96255";
	    boolean isStage = owlSPARQLUtils.hasRole(namedGraph, code, "Disease_Is_Stage");
        System.out.println("isStage? " + code + ": " + isStage);
		//FIGO Stage IIIA2 Ovarian Cancer (Code C128091)
        code = "C128091";
	    isStage = owlSPARQLUtils.hasRole(namedGraph, code, "Disease_Is_Stage");
        System.out.println("isStage? " + code + ": " + isStage);
    }
}

