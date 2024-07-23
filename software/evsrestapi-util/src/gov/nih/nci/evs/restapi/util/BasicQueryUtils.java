package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;
import gov.nih.nci.evs.restapi.config.*;

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


public class BasicQueryUtils {
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String username = null;
    String password = null;
    OWLSPARQLUtils owlSPARQLUtils = null;

    public BasicQueryUtils(String serviceUrl, String named_graph, String username, String password) {
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
		return owlSPARQLUtils.getPrefixes();
	}

	public Vector executeQuery(String query) {
		return owlSPARQLUtils.executeQuery(query);
	}

	public String construct_get_hierarchical_relationships(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?y_label ?y_code ?x_label ?x_code").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + ">").append("\n");
        buf.append("    {").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?y a owl:Class .").append("\n");
        buf.append("                ?y :NHC0 ?y_code .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?x (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?y .").append("\n");
        buf.append("    } ").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getHierarchicalRelationships(String named_graph) {
        String query = construct_get_hierarchical_relationships(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        return new SortUtils().quickSort(v);
	}

	public String construct_get_subsets(String named_graph, String code) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("        ?x a owl:Class .  ").append("\n");
        buf.append("    	?x :NHC0 ?x_code .").append("\n");
        buf.append("    	?x rdfs:label ?x_label . ").append("\n");
        buf.append("        ?p a owl:AnnotationProperty .       ").append("\n");
        buf.append("        ?p :NHC0 \"A8\"^^xsd:string .").append("\n");
        buf.append("        ?p rdfs:label ?p_label . ").append("\n");
        buf.append("        ?y a owl:Class .  ").append("\n");
        buf.append("   	    ?y :NHC0 ?y_code .").append("\n");
        if (code != null) {
        	buf.append("        ?y :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
        buf.append("    	?y rdfs:label ?y_label . ").append("\n");
        buf.append("        ?x ?p ?y").append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getSubsets(String named_graph, String code) {
        String query = construct_get_subsets(named_graph, code);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        return new SortUtils().quickSort(v);
	}

	public Vector getSubsets(String named_graph) {
		return getSubsets(named_graph, null);
	}

	public String construct_get_published_subset(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT ?x_label ?x_code ?p_label ?p_value").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + ">").append("\n");
        buf.append("   {").append("\n");
        buf.append("        ?x a owl:Class .  ").append("\n");
        buf.append("        ?x rdfs:label ?x_label .").append("\n");
        buf.append("    	?x :NHC0 ?x_code .").append("\n");
        buf.append("        ?p a owl:AnnotationProperty .      ").append("\n");
        buf.append("        ?p rdfs:label ?p_label . ").append("\n");
        buf.append("        ?p rdfs:label \"Publish_Value_Set\"^^xsd:string . ").append("\n");
        buf.append("        ?x ?p ?p_value").append("\n");
        buf.append("   }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getPublishedSubset(String named_graph) {
        String query = construct_get_published_subset(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        return new SortUtils().quickSort(v);
	}

	public static void main(String[] args) {
		String serviceUrl = ConfigurationController.serviceUrl;
		String namedGraph = ConfigurationController.namedGraph;
		String username = ConfigurationController.username;
		String password = ConfigurationController.password;
        BasicQueryUtils generator = new BasicQueryUtils(serviceUrl, namedGraph, username, password);

	}
}