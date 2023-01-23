package gov.nih.nci.evs.restapi.appl;
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
 *     Initial implementation kim.ong@nih.gov
 *
 */


public class FlatFileGenerator {
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
    OWLSPARQLUtils owlSPARQLUtils = null;

    static String CONCEPT_MEMBESHIP_FILE = "concept_membership_results.txt";

    public FlatFileGenerator(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
    	this.named_graph = named_graph;
    	this.username = username;
    	this.password = password;
        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

	public String construct_get_class_uris(String named_graph) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_code ?x").append("\n");
		buf.append("{").append("\n");
		buf.append("	graph <" + named_graph + "> ").append("\n");
		buf.append("	{").append("\n");
		buf.append("				?x a owl:Class .").append("\n");
		buf.append("				?x :NHC0 ?x_code .").append("\n");
		buf.append("	}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getClassURIs(String named_graph) {
		String query = construct_get_class_uris(named_graph);

		System.out.println(query);

		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}


	public String construct_get_hierarchical_relationships(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_code ?y_code").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
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
        buf.append("    } ").append("\n");
        buf.append("}").append("\n");
        buf.append("").append("\n");
        return buf.toString();
	}

	public Vector getHierarchicalRelationships(String named_graph) {
        String query = construct_get_hierarchical_relationships(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_prop(String named_graph, String propertyCode) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("SELECT distinct ?x_code ?p_value").append("\n");
        buf.append("from <" + named_graph + "> ").append("\n");
        buf.append("where { ").append("\n");
        buf.append("                    ?x a owl:Class .").append("\n");
        buf.append("                    ?x :NHC0 ?x_code .").append("\n");
        //buf.append("                    ?x rdfs:label ?x_label .").append("\n");
        buf.append("                    ?p :NHC0 \"" + propertyCode + "\"^^xsd:string .").append("\n");
        //buf.append("                    ?p rdfs:label ?p_label .").append("\n");
        buf.append("                    ?x ?p ?p_value .").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getProp(String named_graph, String propertyCode) {
        String query = construct_get_prop(named_graph, propertyCode);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getSynonyms(String named_graph) {
        String propertyCode = "P90";
        return getProp(named_graph, propertyCode);
	}

	public String construct_get_labels(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_code ?x_label").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + "> ").append("\n");
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
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String getClassUri(String code) {
		return "<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#" + code + ">";
	}

	public String construct_get_concept_membership(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("").append("\n");
        buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + "> {").append("\n");
        buf.append("            ?x a owl:Class .").append("\n");
        buf.append("            ?x :NHC0 ?x_code .").append("\n");
        buf.append("            ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("            ?y a owl:Class .").append("\n");
        buf.append("            ?y :NHC0 ?y_code .").append("\n");
        buf.append("            ?y rdfs:label ?y_label .").append("\n");
        buf.append("").append("\n");
        buf.append("            ?x ?p ?y .").append("\n");
        buf.append("            ?p rdfs:label ?p_label .").append("\n");
        buf.append("            ?p rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
        buf.append("").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        buf.append("").append("\n");
        return buf.toString();
	}

    public HashMap createMultiValuedHashMap(Vector data) {
		HashMap hmap = new HashMap();
        for (int i=0; i<data.size(); i++) {
			String line = (String) data.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String key = (String) u.elementAt(0);
			String value = (String) u.elementAt(1);
			Vector w = new Vector();
			if (hmap.containsKey(key)) {
				w = (Vector) hmap.get(key);
			}
			if (!w.contains(value)) {
				w.add(value);
			}
			w = new SortUtils().quickSort(w);
			hmap.put(key, w);
		}
		return hmap;
	}

	public String getTabDelimitedValues(HashMap hmap, String key) {
		String values = "";
		if (hmap.containsKey(key)) {
			Vector w = (Vector) hmap.get(key);
			w = new SortUtils().quickSort(w);
			StringBuffer buf = new StringBuffer();
			for (int j=0; j<w.size(); j++) {
				buf.append((String) w.elementAt(j)).append("|");
			}
			String t = buf.toString();
			values = t.substring(0, t.length()-1);
		}
		return values;
	}


	public Vector getConceptMembership(String named_graph) {
        String query = construct_get_concept_membership(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String sortSynonyms(String synonyms, String pt) {
		Vector u = StringUtils.parseData(synonyms, '|');
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<u.size(); i++) {
			String t = (String)u .elementAt(i);
			if (t.compareTo(pt) == 0) {
				buf.append(t);
				break;
			}
		}
		for (int i=0; i<u.size(); i++) {
			String t = (String)u .elementAt(i);
			if (t.compareTo(pt) != 0) {
				buf.append("|");
				buf.append(t);
			}
		}
		return buf.toString();
    }

	public String generate() {
		Vector w = new Vector();
//Parents
		Vector v = getHierarchicalRelationships(named_graph);
		System.out.println("getHierarchicalRelationships: " + v.size());
		HashMap parentMap = createMultiValuedHashMap(v);
		v.clear();

//Synonyms
		v = getSynonyms(named_graph);
		System.out.println("getSynonyms: " + v.size());
		HashMap synonymMap = createMultiValuedHashMap(v);
		v.clear();

//DEFINITION
		v = getProp(named_graph, "P97");
		System.out.println("getProp(P97): " + v.size());
		HashMap defMap = createMultiValuedHashMap(v);
		v.clear();

//Preferred_Name
		v = getProp(named_graph, "P108");
		System.out.println("getProp(P108): " + v.size());
		HashMap ptMap = createMultiValuedHashMap(v);
		v.clear();

//Display_Name
		v = getProp(named_graph, "P107");
		System.out.println("getProp(P107): " + v.size());
		HashMap dnMap = createMultiValuedHashMap(v);
		v.clear();

//Cocept_Statu
		v = getProp(named_graph, "P310");
		System.out.println("getProp(P310): " + v.size());
		HashMap statusMap = createMultiValuedHashMap(v);
		v.clear();

//Semantic_Type
		v = getProp(named_graph, "P106");
		System.out.println("getProp(P106): " + v.size());
		HashMap styMap = createMultiValuedHashMap(v);
		v.clear();

        v = getLabels(named_graph);
        System.out.println("getLabels: " + v.size());

        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			String classUri = getClassUri(code);
			String parents = getTabDelimitedValues(parentMap, code);
			String synonms = getTabDelimitedValues(synonymMap, code);
			Vector pts = (Vector) ptMap.get(code);
			String pt = (String) pts.elementAt(0);
			synonms = sortSynonyms(synonms, pt);
            String defs = getTabDelimitedValues(defMap, code);
			String dns = getTabDelimitedValues(dnMap, code);
			String status = getTabDelimitedValues(statusMap, code);
			String sty = getTabDelimitedValues(styMap, code);
			w.add(code + "\t" + classUri + "\t" + parents + "\t" + synonms + "\t" + defs + "\t" + dns + "\t" + status + "\t" + sty);
		}
		v.clear();
		String outputfile = "Thesaurus.txt";
		Utils.saveToFile(outputfile, w);
		return outputfile;
	}
}
