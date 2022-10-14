package gov.nih.nci.evs.restapi.util;

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
import java.nio.charset.Charset;

import java.time.Duration;
/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2020 MSC. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
6 * Redistribution and use in source and binary forms, with or without
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


public class SPARQL4OBO {
    OWLSPARQLUtils owlSPARQLUtils = null;
    String BASE_URI = "http://purl.obolibrary.org/obo/go.owl";

    public String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String restURL = null;

    String version = null;
    String username = null;
    String password = null;

    public SPARQL4OBO(String restURL, String namedGraph, String username, String password) {
	    owlSPARQLUtils = new OWLSPARQLUtils(restURL, username, password);
	    owlSPARQLUtils.set_named_graph(namedGraph);
	}

	public String construct_get_prop(String named_graph) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?p ?p_label ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where { ").append("\n");
		buf.append("                    ?p a owl:AnnotationProperty .").append("\n");
		buf.append("OPTIONAL {").append("\n");
		buf.append("                ?p rdfs:label ?p_label").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getProp(String named_graph) {
		String query = construct_get_prop(named_graph);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}


    public String getPrefixes() {
		//if (prefixes != null) return prefixes;
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>").append("\n");
		buf.append("PREFIX obo1:<http://purl.obolibrary.org/obo/>").append("\n");
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


	public String construct_get_superconcepts(String named_graph, String code) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?x_label ?x_id ?y_label ?y_id ").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("                ?x oboInOwl:id ?x_id .").append("\n");
        buf.append("                ?x oboInOwl:id \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?y a owl:Class .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ?y oboInOwl:id ?y_id .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?x rdfs:subClassOf ?y").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getSuperconcepts(String named_graph, String code) {
        String query = construct_get_superconcepts(named_graph, code);

        System.out.println(query);

        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_subconcepts(String named_graph, String code) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?x_label ?x_id ?y_label ?y_id ").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("                ?x oboInOwl:id ?x_id .").append("\n");
        buf.append("                ?x oboInOwl:id \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?y a owl:Class .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ?y oboInOwl:id ?y_id .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?y rdfs:subClassOf ?x").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getSubconcepts(String named_graph, String code) {
        String query = construct_get_subconcepts(named_graph, code);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_axiom(String named_graph, String code) { //GO:0018849
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select ?a1 ?x_id ?x_label ?a1_target ?p ?q1_label ?q1_value").append("\n");
        buf.append("").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x oboInOwl:id ?x_id .").append("\n");
        buf.append("                ?x oboInOwl:id \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?p a owl:AnnotationProperty .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append(" ").append("\n");
        buf.append("                ?a1 a owl:Axiom .").append("\n");
        buf.append("                ?a1 owl:annotatedSource ?x .").append("\n");
        buf.append("                ?a1 owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?a1 owl:annotatedTarget ?a1_target .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
        buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
        buf.append("").append("\n");
        buf.append("}").append("\n");
        buf.append("").append("\n");
        return buf.toString();
	}


	public Vector getAxiom(String named_graph, String code) {
        String query = construct_get_axiom(named_graph, code);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_roles(String named_graph, String code) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_id ?x_label ?p_label ?y_id ?y_label").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where {").append("\n");
        buf.append("            ?x a owl:Class .").append("\n");
        buf.append("            ?x oboInOwl:id ?x_id .").append("\n");
        buf.append("            ?x oboInOwl:id \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("            ?x rdfs:label ?x_label .").append("\n");
        buf.append("            ").append("\n");
        buf.append("            ?p rdfs:label ?p_label .").append("\n");
        buf.append("            ?x (rdfs:subClassOf|owl:equivalentClass|owl:unionOf/rdf:rest*/rdf:first|owl:intersectionOf/rdf:rest*/rdf:first)* ?rs .").append("\n");
        buf.append("            ?rs a owl:Restriction .").append("\n");
        buf.append("            ?rs owl:onProperty ?p .").append("\n");
        buf.append("            ?rs owl:someValuesFrom ?y .            ").append("\n");
        buf.append("            ?y oboInOwl:id ?y_id .").append("\n");
        buf.append("            ?y rdfs:label ?y_label .            ").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getRoles(String named_graph, String code) {
        String query = construct_get_roles(named_graph, code);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_object_prop(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?p_label ?id ?shorthand ?xref ?ns ?inv").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where { ").append("\n");
        buf.append("    ?p a owl:ObjectProperty .").append("\n");
        buf.append("    ?p oboInOwl:hasDbXref ?xref .").append("\n");
        buf.append("    ?p oboInOwl:hasOBONamespace ?ns .").append("\n");
        buf.append("    ?p oboInOwl:id ?id .").append("\n");
        buf.append("    ?p oboInOwl:shorthand ?shorthand .").append("\n");
        buf.append("    ?p rdfs:label ?p_label .").append("\n");
        buf.append("").append("\n");
        buf.append("OPTIONAL {").append("\n");
        buf.append("    ?p owl:inverseOf ?inv").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getObjectProp(String named_graph) {
        String query = construct_get_object_prop(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_id(String named_graph) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_label ?x_id").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where {").append("\n");
        buf.append("            ?x a owl:Class .").append("\n");

        if (named_graph.indexOf("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus") == -1) {
            buf.append("            ?x oboInOwl:id ?x_id .").append("\n");
        } else {
			buf.append("            ?x :NHC0 ?x_id .").append("\n");
		}
        buf.append("            ?x rdfs:label ?x_label .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getId(String named_graph) {
		//set_BASE_URI(named_graph);
        String query = construct_get_id(named_graph);
        System.out.println(query);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}



	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String restURL = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];

	    SPARQL4OBO goRunner = new SPARQL4OBO(restURL, namedGraph, username, password);

	    /*
	    Vector v = goRunner.getProp(namedGraph);
	    v = new SortUtils().quickSort(v);
        Utils.dumpVector("Go properties", v);

        String code = "GO:0003899";
	    v = goRunner.getSuperconcepts(namedGraph, code);
	    v = new SortUtils().quickSort(v);
        Utils.dumpVector("Go superconcepts", v);

	    v = goRunner.getSubconcepts(namedGraph, code);
	    v = new SortUtils().quickSort(v);
        Utils.dumpVector("Go subconcepts", v);

        code = "GO:0018849";
	    v = goRunner.getAxiom(namedGraph, code);
	    v = new SortUtils().quickSort(v);
        Utils.dumpVector("Go getAxiom", v);

        code = "GO:0021546";
	    v = goRunner.getRoles(namedGraph, code);
	    v = new SortUtils().quickSort(v);
        Utils.dumpVector("Go getRoles", v);

	    v = goRunner.getObjectProp(namedGraph);
	    v = new SortUtils().quickSort(v);
        Utils.dumpVector("Go getObjectProp", v);
        */

        namedGraph = "http://GO_monthly";
	    Vector v = goRunner.getId(namedGraph);
	    //v = new SortUtils().quickSort(v);
        //Utils.dumpVector("go_IDs", v);

        namedGraph = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus22.08e.owl";
	    v = goRunner.getId(namedGraph);
	    v = new SortUtils().quickSort(v);
        Utils.dumpVector("ncit_IDs", v);



	}


}
