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
import java.nio.charset.Charset;

import java.time.Duration;
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


public class ConceptHelper {
	String serviceUrl = null;
	String namedGraph = null;
	String username = null;
	String password = null;
	OWLSPARQLUtils owlSPARQLUtils = null;

    public ConceptHelper(String serviceUrl,
                          String namedGraph,
                          String username,
                          String password) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		this.username = username;
		this.password = password;
	    this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
	    this.owlSPARQLUtils.set_named_graph(namedGraph);
	}

	public String construct_get_label(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code").append("\n");
		buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("                ?x a owl:Class .").append("\n");
		buf.append("                ?x :NHC0 ?x_code .").append("\n");
		if (code != null) {
			buf.append("                ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}
		buf.append("                ?x rdfs:label ?x_label .").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector getLabel(String named_graph, String code) {
        String query = construct_get_label(named_graph, code);
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
        buf.append("select distinct ?x_code ?x_label ?p_code ?p_label ?y_code ?y_label").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + "> ").append("\n");
        buf.append("    {").append("\n");
        buf.append("            ?x :NHC0 ?x_code .").append("\n");
        buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
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


	public Vector getRoles(String named_graph, String code) {
        String query = construct_get_roles(named_graph, code);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_proproperties(String named_graph, String code) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?x1_code ?x1_label ?p_label ?p_value").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where { ").append("\n");
        buf.append("                    ?x1 a owl:Class .").append("\n");
        buf.append("                    ?x1 :NHC0 ?x1_code .").append("\n");
        buf.append("                ?x1 :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("                    ?x1 rdfs:label ?x1_label .").append("\n");
        buf.append("                    ?p rdfs:label ?p_label .").append("\n");
        buf.append("                    ?x1 ?p ?p_value .               ").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


	public Vector getProperties(String named_graph, String code) {
        String query = construct_get_proproperties(named_graph, code);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_subclasses(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> ").append("\n");
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
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_superclasses(String named_graph, String code) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?y_label ?y_code ").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + "> ").append("\n");
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
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_axioms(String named_graph, String code) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?a1 ?x1_label ?x1_code ?p_label ?a1_target ?q1_label ?q1_value").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("                    ?x1 a owl:Class .").append("\n");
        buf.append("                    ?x1 :NHC0 ?x1_code .").append("\n");
        buf.append("                ?x1 :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
        buf.append("                    ?x1 rdfs:label ?x1_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?a1 a owl:Axiom .").append("\n");
        buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
        buf.append("                ?a1 owl:annotatedProperty ?p .").append("\n");
        buf.append("                ?a1 owl:annotatedTarget ?a1_target .").append("\n");
        buf.append("                ?p :NHC0 ?p_code .").append("\n");
        buf.append("                ?p rdfs:label ?p_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?q1 :NHC0 ?q1_code .").append("\n");
        buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
        buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
        buf.append("").append("\n");
        buf.append("                             ").append("\n");
        buf.append("").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getAxioms(String named_graph, String code) {
        String query = construct_get_axioms(named_graph, code);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getAxiomData(Vector w, String prop_label) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String propLabel = (String) u.elementAt(3);
			if (propLabel.compareTo(prop_label) == 0) {
				v.add(line);
			}
		}
		return v;
	}

	public void getConceptHelper(String named_graph, String code) {
        Vector w = getLabel(named_graph, code);
        Utils.dumpVector("getLabel", w);

        w = getSubclasses(named_graph, code);
        Utils.dumpVector("getSubclasses", w);

        w = getSuperclasses(named_graph, code);
        Utils.dumpVector("getSuperclasses", w);

        w = getProperties(named_graph, code);
        Utils.dumpVector("getProperties", w);

        w = getRoles(named_graph, code);
        Utils.dumpVector("getRoles", w);

        w = getAxioms(named_graph, code);
        Utils.dumpVector("getAxioms", w);

        List list = new AxiomUtils().getSynonyms(getAxiomData(w, "FULL_SYN"));
        for (int i=0; i<list.size(); i++) {
			Synonym syn = (Synonym) list.get(i);
			System.out.println(syn.toJson());
		}

        list = new AxiomUtils().getDefinitions(getAxiomData(w, "DEFINITION"));
        for (int i=0; i<list.size(); i++) {
			Definition def = (Definition) list.get(i);
			System.out.println(def.toJson());
		}

        list = new AxiomUtils().getDefinitions(getAxiomData(w, "ALT_DEFINITION"));
        for (int i=0; i<list.size(); i++) {
			AltDefinition def = (AltDefinition) list.get(i);
			System.out.println(def.toJson());
		}

        list = new AxiomUtils().getMapsTos(getAxiomData(w, "Maps_To"));
        for (int i=0; i<list.size(); i++) {
			MapToEntry mapsTo = (MapToEntry) list.get(i);
			System.out.println(mapsTo.toJson());
		}
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		ConceptHelper cd = new ConceptHelper(serviceUrl, namedGraph, username, password);
		String code = args[4];
		cd.getConceptHelper(namedGraph, code);
	}

}