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
    String named_graph_id = ":NHC0";

	public OWLSPARQLUtils() {
		this.httpUtils = new HTTPUtils();
        this.jsonUtils = new JSONUtils();
	}

	public OWLSPARQLUtils(String serviceUrl) {
		this.serviceUrl = serviceUrl;
		this.httpUtils = new HTTPUtils(serviceUrl, null, null);
        this.jsonUtils = new JSONUtils();
    }

	public OWLSPARQLUtils(ServerInfo serverInfo) {
		this.serviceUrl = serverInfo.getServiceUrl();
		this.httpUtils = new HTTPUtils(serviceUrl, serverInfo.getUsername(), serverInfo.getPassword());
        this.jsonUtils = new JSONUtils();
	}


	public OWLSPARQLUtils(String serviceUrl, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.httpUtils = new HTTPUtils(serviceUrl, username, password);
        this.jsonUtils = new JSONUtils();
    }

    public void set_named_graph_id(String named_graph_id) {
		this.named_graph_id = named_graph_id;
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
			query = httpUtils.encode(query);
            String json = httpUtils.executeQuery(query);
			v = new JSONUtils().parseJSON(json);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}


	public String construct_get_ontology_info(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_version_info ?x_dc_date ?x_comment").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("	    ?x a owl:Ontology .").append("\n");
		buf.append("	    ?x owl:versionInfo ?x_version_info .").append("\n");
		buf.append("	    ?x dc:date ?x_dc_date .").append("\n");
		buf.append("	    ?x rdfs:comment ?x_comment").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getOntologyInfo(String named_graph) {
		return executeQuery(construct_get_ontology_info(named_graph));
	}


	public String construct_get_named_graphs() {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
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

	public Vector getOntologyInfo() {
		Vector name_graph_vec = getNamedGraph();
		Vector v = new Vector();
		ParserUtils parser = new ParserUtils();
		if (name_graph_vec == null || name_graph_vec.size() == 0) return v;
		for (int i=0; i<name_graph_vec.size(); i++) {
			String line = (String) name_graph_vec.elementAt(i);
            String named_graph = parser.getValue(line);
			Vector u = getOntologyInfo(named_graph);
			String version = parser.getValue((String) u.elementAt(0));
			String dc_date = parser.getValue((String) u.elementAt(1));
			String comment = parser.getValue((String) u.elementAt(2));
			v.add(named_graph + "|" + version + "|" + dc_date + "|" + comment);
		}
		return v;
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
		buf.append("limit 100").append("\n");
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
		buf.append("	    ?x owl:equivalentClass ?y .").append("\n");
		buf.append("	    ?y owl:intersectionOf ?list .").append("\n");
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
		buf.append("	    ?y owl:equivalentClass ?z .").append("\n");
		buf.append("	    ?z owl:intersectionOf ?list .").append("\n");
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

/*
	public String construct_get_synonyms(String named_graph, String code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("").append("\n");
		buf.append("SELECT distinct ?z_axiom ?x_label ?x_code ?term_name ?y ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?z_axiom a owl:Axiom  .").append("\n");
		buf.append("            ?z_axiom owl:annotatedSource ?x .").append("\n");
		buf.append("            ?z_axiom owl:annotatedProperty ?p .").append("\n");
		buf.append("            ?p :NHC0 \"P90\"^^xsd:string .").append("\n");
		buf.append("	    ?z_axiom owl:annotatedTarget ?term_name .").append("\n");
		buf.append("	    ?z_axiom ?y ?z ").append("\n");
		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?y) = \"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#term-group\"").append("\n");
		buf.append("         || str(?y) = \"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#term-source\"").append("\n");
		buf.append("         || str(?y) = \"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P385\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}
*/
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


	public Vector getSynonyms(String named_graph, String code) {
		Vector v = getPropertyQualifiersByCode(named_graph, code);
		v = new ParserUtils().filterPropertyQualifiers(v, Constants.FULL_SYN);
		return v;
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
		buf.append("").append("\n");
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
		buf.append("	   }").append("\n");
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
		buf.append("	   }	").append("\n");
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
		buf.append("				?y :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> ").append("\n");
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
		buf.append("					?y :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> ").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}


	public Vector getInboundRolesByCode(String named_graph, String code) {
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
		buf.append("	   }").append("\n");
		buf.append("	   UNION ").append("\n");
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
		buf.append("	   }	").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
		buf.append("		?x owl:equivalentClass ?z .").append("\n");
		buf.append("			?z owl:intersectionOf ?list .").append("\n");
		buf.append("			?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("				?z2 a owl:Restriction .").append("\n");
		buf.append("				?z2 owl:onProperty ?p .").append("\n");
		buf.append("				?p rdfs:label ?p_label .").append("\n");
		buf.append("				?z2 owl:someValuesFrom ?y .").append("\n");
		buf.append("				?y :NHC0 ?y_code .").append("\n");
		buf.append("				?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("	   UNION").append("\n");
		buf.append("	   {").append("\n");
		buf.append("		?x a owl:Class .").append("\n");
		buf.append("		?x :NHC0 \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .").append("\n");
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
		buf.append("					?y :NHC0 ?y_code .").append("\n");
		buf.append("					?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}


	public Vector getOutboundRolesByCode(String named_graph, String code) {
		return executeQuery(construct_get_outbound_roles_by_code(named_graph, code));
	}

	public String construct_get_role_relationships(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?p_code ?y_label ?y_code").append("\n");
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
		buf.append("		?x rdfs:subClassOf ?r .").append("\n");
		buf.append("		?r a owl:Restriction .").append("\n");
		buf.append("		?r owl:onProperty ?p .").append("\n");
		buf.append("		?p rdfs:label ?p_label .").append("\n");
		buf.append("		?r owl:someValuesFrom ?y .").append("\n");
		buf.append("		?y a owl:Class .").append("\n");
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	   }	").append("\n");
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
		buf.append("		?y :NHC0 ?y_code .").append("\n");
		buf.append("		?y rdfs:label ?y_label").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector getRoleRelationships(String named_graph) {
		return executeQuery(construct_get_role_relationships(named_graph));
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

	public String construct_get_disease_is_stage_source_codes(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code").append("\n");
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
		buf.append("		FILTER (str(?p_label)=\"Disease_Is_Stage\"^^xsd:string)").append("\n");
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
		buf.append("		FILTER (str(?p_label)=\"Disease_Is_Stage\"^^xsd:string)").append("\n");
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
		buf.append("				FILTER (str(?p_label)=\"Disease_Is_Stage\"^^xsd:string)").append("\n");
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
		buf.append("					FILTER (str(?p_label)=\"Disease_Is_Stage\"^^xsd:string)").append("\n");
		buf.append("	   }").append("\n");
		buf.append("   }").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}


	public Vector getDiseaseIsStageSourceCodes(String named_graph) {
		return executeQuery(construct_get_association_source_codes(named_graph, "Disease_Is_Stage"));
	}

	public Vector getDiseaseIsGradeSourceCodes(String named_graph) {
		return executeQuery(construct_get_association_source_codes(named_graph, "Disease_Is_Grade"));
	}

	public Vector getAssociationSourceCodes(String named_graph, String associationName) {
		return executeQuery(construct_get_association_source_codes(named_graph, associationName));
	}

	public String construct_get_association_source_codes(String named_graph, String associationName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code").append("\n");
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
		buf.append("		FILTER (str(?p_label)=\"Disease_Is_Stage\"^^xsd:string)").append("\n");
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
		return executeQuery(construct_get_object_properties_domain_range(named_graph));
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
		return executeQuery(construct_get_object_valued_annotation_properties(named_graph));
	}

	public String construct_get_string_valued_annotation_properties(String named_graph) {
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
		buf.append("    FILTER (str(?x_range)=\"http://www.w3.org/2001/XMLSchema#string\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getStringValuedAnnotationProperties(String named_graph) {
		return executeQuery(construct_get_string_valued_annotation_properties(named_graph));
	}

	public Vector getSupportedProperties(String named_graph) {
		return getStringValuedAnnotationProperties(named_graph);
	}

	public Vector getSupportedAssociations(String named_graph) {
		return getObjectValuedAnnotationProperties(named_graph);
	}

	public Vector getSupportedRoles(String named_graph) {
		return getObjectProperties(named_graph);
	}

	public String construct_get_hierarchical_relationships(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?z_label ?z_code ?x_label ?x_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
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

    public static void main(String[] args) {

    }
}
