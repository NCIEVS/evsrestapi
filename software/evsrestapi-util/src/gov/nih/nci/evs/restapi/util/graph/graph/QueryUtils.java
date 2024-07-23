package gov.nih.nci.evs.restapi.util.graph;

import gov.nih.nci.evs.restapi.util.*;
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
import org.apache.commons.text.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2020 MSC. This software was developed in conjunction
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
 *     Initial implementation kim.ong@nih.gov
 *
 */


public class QueryUtils {
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String username = null;
    String password = null;
    OWLSPARQLUtils owlSPARQLUtils = null;

    public QueryUtils(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
    	this.named_graph = named_graph;
    	this.username = username;
    	this.password = password;
    	initialize();
    }

    public void initialize() {
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		owlSPARQLUtils.set_named_graph(named_graph);
	}

	public String getPrefixes() {
		String s = owlSPARQLUtils.getPrefixes();
		System.out.println(s);
		return s;
	}

	public Vector executeQuery(String query) {
		return owlSPARQLUtils.executeQuery(query);
	}

	public String get_associations(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_code ?x_label ?a1_label ?y1_code ?y1_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <${named_graph}>").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 "+"${code}"+"^^xsd:string .").append("\n");
		buf.append("            ?a1 a owl:AnnotationProperty .").append("\n");
		buf.append("            ?a1 rdfs:label ?a1_label .").append("\n");
		buf.append("            ?x ?a1 ?y1  .").append("\n");
		buf.append("            ?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("            ?y1 :NHC0 ?y1_code .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return substitute(buf.toString(), named_graph, code);
	}

	public String get_inverse_associations(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_code ?x_label ?a1_label ?y1_code ?y1_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <${named_graph}>").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 "+"${code}"+"^^xsd:string .").append("\n");
		buf.append("            ?a1 a owl:AnnotationProperty .").append("\n");
		buf.append("            ?a1 rdfs:label ?a1_label .").append("\n");
		buf.append("            ?y1 ?a1 ?x .").append("\n");
		buf.append("            ?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("            ?y1 :NHC0 ?y1_code .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return substitute(buf.toString(), named_graph, code);
	}

	public String get_roles(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_code ?x_label ?r1_label ?y1_code ?y1_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <${named_graph}>").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 "+"${code}"+"^^xsd:string .").append("\n");
		buf.append("            ?x (rdfs:subClassOf|owl:equivalentClass|owl:unionOf/rdf:rest*/rdf:first|owl:intersectionOf/rdf:rest*/rdf:first)* ?rs .").append("\n");
		buf.append("            ?rs a owl:Restriction .").append("\n");
		buf.append("            ?rs owl:onProperty ?r1 .").append("\n");
		buf.append("            ?rs owl:someValuesFrom ?y1 .").append("\n");
		buf.append("            ?r1 rdfs:label ?r1_label .").append("\n");
		buf.append("            ?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("            ?y1 :NHC0 ?y1_code .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return substitute(buf.toString(), named_graph, code);
	}

	public String get_inverse_roles(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_code ?x_label ?r1_label ?y1_code ?y1_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <${named_graph}>").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 "+"${code}"+"^^xsd:string .").append("\n");
		buf.append("            ?y1 (rdfs:subClassOf|owl:equivalentClass|owl:unionOf/rdf:rest*/rdf:first|owl:intersectionOf/rdf:rest*/rdf:first)* ?rs .").append("\n");
		buf.append("            ?rs a owl:Restriction .").append("\n");
		buf.append("            ?rs owl:onProperty ?r1 .").append("\n");
		buf.append("            ?rs owl:someValuesFrom ?x .").append("\n");
		buf.append("            ?r1 rdfs:label ?r1_label .").append("\n");
		buf.append("            ?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("            ?y1 :NHC0 ?y1_code .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return substitute(buf.toString(), named_graph, code);
	}

	public String get_roots(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_code ?x_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <${named_graph}>").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 "+"${code}"+"^^xsd:string .").append("\n");
		buf.append("            filter not exists { ?x rdfs:subClassOf|owl:equivalentClass ?y .}").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return substitute(buf.toString(), named_graph, code);
	}

	public String get_subset(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_code ?x_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <${named_graph}>").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 "+"${code}"+"^^xsd:string .").append("\n");
		buf.append("            ?a1 a owl:AnnotationProperty .").append("\n");
		buf.append("            ?a1 rdfs:label ?a1_label .").append("\n");
		buf.append("            ?y1 ?a1 ?x .").append("\n");
		buf.append("            ?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("            ?y1 :NHC0 ?y1_code .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return substitute(buf.toString(), named_graph, code);
	}

	public String get_parents(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_code ?x_label ?y1_code ?y1_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <${named_graph}>").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 "+"${code}"+"^^xsd:string .").append("\n");
		buf.append("            ?x (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?y1 .").append("\n");
		buf.append("            ?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("            ?y1 :NHC0 ?y1_code .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return substitute(buf.toString(), named_graph, code);
	}

	public String get_children(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_code ?x_label ?y1_code ?y1_label").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <${named_graph}>").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 "+"${code}"+"^^xsd:string .").append("\n");
		buf.append("            ?y1 (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?x .").append("\n");
		buf.append("            ?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("            ?y1 :NHC0 ?y1_code .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return substitute(buf.toString(), named_graph, code);
	}

	public String construct_get_version(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select ?x_code ?x_label").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + ">").append("\n");
        buf.append("    {").append("\n");
        buf.append("            ?x_code a owl:Ontology .").append("\n");
        buf.append("            ?x owl:versionInfo ?x_label").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public String getVersion(String named_graph) {
        String query = construct_get_version(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return null;
        String t = (String) v.elementAt(0);
        Vector u = StringUtils.parseData(t, '|');
        return (String) u.elementAt(1);
	}

    public static String substitute(String templateString, String named_graph, String code) {
		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("named_graph", named_graph);
		valuesMap.put("code", "\"" + code + "\"");
		StringSubstitutor sub = new StringSubstitutor(valuesMap);
		String resolvedString = sub.replace(templateString);
		return resolvedString;
	}

	public Vector getParents(String code) {
        String query = get_parents(named_graph, code);
        Vector v = executeQuery(query);
        return v;
	}

	public Vector getChildren(String code) {
        String query = get_children(named_graph, code);
        Vector v = executeQuery(query);
        return v;
	}

	public Vector getRoles(String code) {
        String query = get_roles(named_graph, code);
        Vector v = executeQuery(query);
        return v;
	}

	public Vector getInverseRoles(String code) {
        String s = "r";
        String query = get_inverse_roles(named_graph, code);
        Vector v = executeQuery(query);
        return v;
	}

	public Vector getAssociations(String code) {
        String s = "A";
        String query = get_associations(named_graph, code);
        Vector v = executeQuery(query);
        return v;
	}

	public Vector getInverseAssociations(String code) {
        String s = "a";
        String query = get_inverse_associations(named_graph, code);
        Vector v = executeQuery(query);
        return v;
	}




    public HashMap createRelationshipHashMap(String code) {
		Vector superclasses;
		Vector subclasses;
		Vector roles;
		Vector inverseRoles;
		Vector associations;
		Vector inverseAssociations;

		HashMap relMap = new HashMap();
		List list = new ArrayList();
		Vector v = getParents(code);
        if (v != null) {
			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
				String concept_label = (String) u.elementAt(3);
				String concept_code = (String) u.elementAt(2);
				list.add(concept_label + "|" + concept_code);
			}
		}
		relMap.put("type_superconcept", list);
		list = new ArrayList();
		v = getChildren(code);
		if (v != null) {
			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
				String concept_label = (String) u.elementAt(3);
				String concept_code = (String) u.elementAt(2);
				list.add(concept_label + "|" + concept_code);
			}
		}
		relMap.put("type_subconcept", list);
		list = new ArrayList();
        v = getRoles(code);
        if (v != null) {
			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
				String property_name = (String) u.elementAt(2);
				String target_concept_label = (String) u.elementAt(4);
				String target_concept_code = (String) u.elementAt(3);
				list.add(property_name + "|" + target_concept_label + "|" + target_concept_code);
			}
		}
		relMap.put("type_role", list);

		list = new ArrayList();
        v = getInverseRoles(code);
        if (v != null) {
			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
				String property_name = (String) u.elementAt(2);
				String source_concept_label = (String) u.elementAt(4);
				String source_concept_code = (String) u.elementAt(3);
				list.add(property_name + "|" + source_concept_label + "|" + source_concept_code);
			}
		}
		relMap.put("type_inverse_role", list);

		list = new ArrayList();
        v = getAssociations(code);
        if (v != null) {
			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
				String property_name = (String) u.elementAt(2);
				String target_concept_label = (String) u.elementAt(4);
				String target_concept_code = (String) u.elementAt(3);
				list.add(property_name + "|" + target_concept_label + "|" + target_concept_code);
			}
		}
		relMap.put("type_association", list);

		list = new ArrayList();
        v = getInverseAssociations(code);
        if (v != null) {
			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
				String property_name = (String) u.elementAt(2);
				String source_concept_label = (String) u.elementAt(4);
				String source_concept_code = (String) u.elementAt(3);
				list.add(property_name + "|" + source_concept_label + "|" + source_concept_code);
			}
		}
		relMap.put("type_inverse_association", list);
        return relMap;
	}

	public static void dumpRelationshipHashMap(HashMap hmap) {
        Iterator it = hmap.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			ArrayList list = (ArrayList) hmap.get(key);
			Utils.dumpArrayList(key, list);
		}
	}

/*
	public static void main(String[] args) {
		String serviceUrl = ConfigurationController.serviceUrl;
		String namedGraph = ConfigurationController.namedGraph;
		String username = ConfigurationController.username;
		String password = ConfigurationController.password;
		String code = args[0];
		QueryUtils utils = new QueryUtils(serviceUrl, namedGraph, username, password);
		HashMap hmap = utils.createRelationshipHashMap(code);
		dumpRelationshipHashMap(hmap);
	}
	*/
}
