package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.appl.*;

import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;
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

import org.apache.commons.text.similarity.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


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

public class ValueSetSearchUtils extends SPARQLSearchUtils {
	private OWLSPARQLUtils owlSPARQLUtils = null;
	//private SPARQLSearchUtils sparqlSearchUtils = null;
	private String named_graph = null;
	private HashMap valueSetMembershipHashMap = null;
	private Vector concept_in_subset_vec = null;
	private HashMap valueSetURIHashMap = null;
	private Vector vs_code_vec = null;

	public ValueSetSearchUtils(String serviceUrl) {
		super(serviceUrl);
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
    }

	public ValueSetSearchUtils(String serviceUrl, OWLSPARQLUtils owlSPARQLUtils) {
		this.serviceUrl = serviceUrl;
		this.owlSPARQLUtils = owlSPARQLUtils;
    }

	public ValueSetSearchUtils(String serviceUrl, String named_graph, Vector concept_in_subset_vec) {
		super(serviceUrl);
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		initialize(named_graph, concept_in_subset_vec);
    }

    public void set_concept_in_subset_vec(Vector concept_in_subset_vec) {
		this.concept_in_subset_vec = concept_in_subset_vec;
    }

    public void initialize(String named_graph, Vector concept_in_subset_vec) {
		long ms = System.currentTimeMillis();
		this.named_graph = named_graph;
		this.owlSPARQLUtils.set_named_graph(named_graph);
		this.set_named_graph(named_graph);
		this.concept_in_subset_vec = concept_in_subset_vec;

		System.out.println("concept_in_subset_vec: " + concept_in_subset_vec.size());

		this.valueSetMembershipHashMap = this.owlSPARQLUtils.createValueSetMembershipHashMap(concept_in_subset_vec);
		this.vs_code_vec = this.owlSPARQLUtils.getPermissibleValues(concept_in_subset_vec);

		System.out.println("vs_code_vec: " + vs_code_vec.size());

		this.valueSetURIHashMap = owlSPARQLUtils.createHeaderConceptCode2ValueSetURIHashMap(named_graph, vs_code_vec);
		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public HashMap getvalueSetMembershipHashMap() {
		return this.valueSetMembershipHashMap;
	}

	public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

	//public String getNamedGraph() {
	//	return named_graph;
	//}

    public Vector searchValueSetURIs(String named_graph, String propertyName, String searchString, String searchTarget, String algorithm) {
		Vector w = new Vector();
		SearchResult sr = search(named_graph, propertyName, searchString, searchTarget, algorithm);
		sr = restrictToValueSets(sr);
		Vector uris = searchResult2ValueSetURIs(sr);
		for (int k2=0; k2<uris.size(); k2++) {
			String uri = (String) uris.elementAt(k2);
			if (!w.contains(uri)) {
				w.add(uri);
			}
		}
		return new SortUtils().quickSort(w);
	}

    public void runSearch(String searchString, String outputfile) {
		Vector results = new Vector();
		int k = 0;
		String searchTarget = TARGETS[0];

/*
        for (int j=0; j<ALGORITHMS.length; j++) {
			k++;
			String algorithm = ALGORITHMS[j];
			String propertyName = "FULL_SYN";
            results.add("\n(" + k + ") propertyName: " + propertyName + "; searchString: " + searchString + "; target: " + searchTarget + "; algorithm: " + algorithm);
			try {
				SearchResult sr = search(named_graph, propertyName, searchString, searchTarget, algorithm);
				if (sr != null) {
					sr = restrictToValueSets(sr);
					results.add(sr.toJson());
					Vector uris = searchResult2ValueSetURIs(sr);
					results.add("Value sets: (" + algorithm + ")");
					for (int k2=0; k2<uris.size(); k2++) {
						String uri = (String) uris.elementAt(k2);
						results.add("\t" + uri);
					}
				}
			} catch (Exception ex) {
				System.out.println("Searching on " + propertyName + " using " + algorithm + " throws exception???");
			}
		}
*/

		searchTarget = TARGETS[2];
		for (int j=0; j<ALGORITHMS.length; j++) {
			k++;
			String algorithm = ALGORITHMS[j];
			String associationName = "Concept_In_Subset";
			SearchResult sr = search(named_graph, associationName, searchString, searchTarget, algorithm);
			sr = restrictToValueSets(sr);
			results.add("\n(" + k + ") associationName: " + associationName + "; searchString: " + searchString + "; target: " + searchTarget + "; algorithm: " + algorithm);
			results.add(sr.toJson());
			Vector uris = searchResult2ValueSetURIs(sr);
			results.add("Value sets: (" + algorithm + ")");
			for (int k2=0; k2<uris.size(); k2++) {
				String uri = (String) uris.elementAt(k2);
				results.add("\t" + uri);
			}
		}

		if (results != null) {
			if (outputfile == null) {
				outputfile = searchString;
				outputfile = outputfile.replaceAll(" ", "_");
				outputfile = outputfile.replaceAll(":", "_");
				outputfile = outputfile + "_" + StringUtils.getToday() + ".txt";
			}
        	Utils.saveToFile(outputfile, results);
		}
	}

	public SearchResult restrictToValueSets(SearchResult sr) {
		List list = new ArrayList();
		List matchedConcepts = sr.getMatchedConcepts();
		for (int i=0; i<matchedConcepts.size(); i++) {
			MatchedConcept mc = (MatchedConcept) matchedConcepts.get(i);
            if (valueSetMembershipHashMap.containsKey(mc.getCode())) {
				list.add(mc);
			}
		}
		SearchResult new_sr = new SearchResult();
		new_sr.setMatchedConcepts(list);
		return new_sr;
	}

	public Vector mergeVectors(Vector v1, Vector v2) {
		if (v1 == null || v2 == null) return null;
		for (int i=0; i<v2.size(); i++) {
			String t = (String) v2.elementAt(i);
			if (!v1.contains(t)) {
				v1.add(t);
			}
		}
		return v1;
	}

	public Vector searchResult2ValueSetURIs(SearchResult sr) {
		Vector v = new Vector();
		List matchedConcepts = sr.getMatchedConcepts();
		for (int i=0; i<matchedConcepts.size(); i++) {
			MatchedConcept mc = (MatchedConcept) matchedConcepts.get(i);
			String code = mc.getCode();
            if (valueSetMembershipHashMap.containsKey(code)) {
				Vector w = (Vector) valueSetMembershipHashMap.get(code);
				v.addAll(w);
			}
		}
		Vector uris = new Vector();
		for (int i=0; i<v.size(); i++) {
			String vs_code = (String) v.elementAt(i);
			Vector vs_uri_vec = (Vector) valueSetURIHashMap.get(vs_code);
			if (vs_uri_vec != null) {
				uris = mergeVectors(uris, vs_uri_vec);
			}
		}
		uris = new SortUtils().quickSort(uris);
		return uris;
	}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void apply_text_match(StringBuffer buf, String param, String searchString, String algorithm) {
		if (algorithm.compareTo("exactMatch") == 0) {
			buf.append("FILTER (lcase(str(?" + param + ")) = \"" + searchString + "\")").append("\n");
		} else if (algorithm.compareTo("startsWith") == 0) {
			buf.append("FILTER (regex(str(?"+ param +"),'^" + searchString + "','i'))").append("\n");
		} else if (algorithm.compareTo("endsWith") == 0) {
			buf.append("FILTER (regex(str(?"+ param +"),'" + searchString + "^','i'))").append("\n");
		} else {
			searchString = searchString.replaceAll("%20", " ");
			searchString = searchString.toLowerCase();
			searchString = SPARQLSearchUtils.createSearchString(searchString, algorithm);
			buf.append("(?"+ param +") <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");
		}
	}

	public String construct_search_value_sets(String named_graph, String searchString, String algorithm) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?y_label ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("?y rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
		buf.append("?y rdfs:label ?y_label .").append("\n");
		buf.append("?z rdfs:label ?z_label .").append("\n");
		buf.append("?z :NHC0 ?z_code .").append("\n");

		apply_text_match(buf, "x_label", searchString, algorithm);

		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector searchValueSets(String named_graph, String matchText, String algorithm) {
	    String query = construct_search_value_sets(named_graph, matchText, algorithm);
	    Vector v = owlSPARQLUtils.executeQuery(query);
	    if (v == null) return null;
	    if (v.size() == 0) return v;
	    return new ParserUtils().getResponseValues(v);
	}

	public String construct_get_value_set_metadata(String named_graph, String header_concept_code) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("SELECT distinct ?x_label ?x_code ?y1_label ?z1 ?y2_label ?z2 ").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x ?y1 ?z1 .").append("\n");
		buf.append("?y1 rdfs:label ?y1_label .").append("\n");
		buf.append("?y1 rdfs:label \"DEFINITION\"^^xsd:string .").append("\n");
		buf.append("?x ?y2 ?z2 .").append("\n");
		buf.append("?y2 rdfs:label \"Contributing_Source\"^^xsd:string .").append("\n");
		buf.append("?y2 rdfs:label ?y2_label .").append("\n");
		buf.append("FILTER (str(?x_code) = \"" + header_concept_code + "\"^^xsd:string)").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getValueSetMetadata(String named_graph, String header_concept_code) {
	    String query = construct_get_value_set_metadata(named_graph, header_concept_code);
	    Vector v = owlSPARQLUtils.executeQuery(query);
	    if (v == null) return null;
	    if (v.size() == 0) return v;
	    return new ParserUtils().getResponseValues(v);
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();

		String serviceUrl = args[0];
		System.out.println(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		MetadataUtils test = new MetadataUtils(serviceUrl);
		String version = test.getLatestVersion(codingScheme);
		String named_graph = test.getNamedGraph(codingScheme);

		System.out.println(named_graph);
		serviceUrl = serviceUrl + "?query=";

		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("codingScheme: " + codingScheme);
		System.out.println("version: " + version);
		System.out.println("named_graph: " + named_graph);

		String concept_in_subset_file = "Concept_In_Subset.txt";
		Vector cis_vec = Utils.readFile(concept_in_subset_file);
		ValueSetSearchUtils searchUtils = new ValueSetSearchUtils(serviceUrl, named_graph, cis_vec);
	    String outputfile = "search_" + StringUtils.getToday() + ".txt";
	    String searchString = "red";

	    ms = System.currentTimeMillis();
	    searchUtils.runSearch(searchString, outputfile);
	    System.out.println("Total search run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
