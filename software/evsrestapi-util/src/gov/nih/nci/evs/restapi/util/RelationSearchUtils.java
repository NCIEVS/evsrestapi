package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.appl.*;
import gov.nih.nci.evs.restapi.bean.*;

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

public class RelationSearchUtils extends SPARQLSearchUtils {
	private OWLSPARQLUtils owlSPARQLUtils = null;
	private String named_graph = null;


	public RelationSearchUtils(String serviceUrl) {
		super(serviceUrl);
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
    }

	public RelationSearchUtils(String serviceUrl, String named_graph, Vector concept_in_subset_vec) {
		super(serviceUrl);
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
    }

    public void initialize(String named_graph) {
		long ms = System.currentTimeMillis();
		this.named_graph = named_graph;
		this.owlSPARQLUtils.set_named_graph(named_graph);
		this.set_named_graph(named_graph);
		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

    public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
	}


	public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

	public Vector postProcess(Vector w, String searchTarget, String algorithm, String searchString) {
		searchString = searchString.toLowerCase();
        searchString = searchString.trim();
		Vector v = new Vector();

		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String p_label = (String) u.elementAt(2);
			String p_value = (String) u.elementAt(0);
			p_value = p_value.toLowerCase();

			if (searchTarget.compareTo(NAMES) == 0) {
				if (PRESENTATION_LIST.contains(p_label)) {
					if (match(algorithm, searchString, p_value)) {
					    if (!v.contains(line)) {
							v.add(line);
						}
					}
				}
			} else if (searchTarget.compareTo(PROPERTIES) == 0) {
				if (!PRESENTATION_LIST.contains(p_label)) {
					if (match(algorithm, searchString, p_value)) {
					    if (!v.contains(line)) {
	                        v.add(line);
						}
					}
				}
			} else {
				boolean bool = match(algorithm, searchString, p_value);
				if (bool) {
					if (!v.contains(line)) {
						v.add(line);
					}
				}
			}
		}
		return v;
	}

	public String construct_search_by_relationship(String associationName, String searchString, String algorithm, int match_option) {
		searchString = searchString.replaceAll("%20", " ");
		searchString = searchString.toLowerCase();
		searchString = createSearchString(searchString, algorithm);
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?y_label ?z_label ?z_code").append("\n");
		buf.append("{").append("\n");
		buf.append("    graph <http://NCIt_Flattened>").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ").append("\n");
		buf.append("            ?z a owl:Class .").append("\n");
		buf.append("            ?z rdfs:label ?z_label .").append("\n");
		buf.append("            ?z :NHC0 ?z_code .").append("\n");
		buf.append("            ").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");

		if (associationName != null) {
			buf.append("            ?y rdfs:label \"" + associationName + "\"^^xsd:string .").append("\n");
			buf.append("            ").append("\n");
	    }

		buf.append("            ?y rdfs:range ?y_range .").append("\n");
		if (match_option == MATCH_SOURCE) {
			buf.append("            (?x_label) <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");
		} else {
			buf.append("            (?z_label) <tag:stardog:api:property:textMatch> (" + searchString + ").").append("\n");
		}

		buf.append("    }").append("\n");
		buf.append("    FILTER (str(?y_range)=\"http://www.w3.org/2001/XMLSchema#anyURI\")").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector searchByRelationship(String associationName, String searchString, String algorithm, int match_option) {
		String query = construct_search_by_relationship(associationName, searchString, algorithm, match_option);
		System.out.println(query);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		if (v.size() > 0) {
			String searchTarget = ASSOCIATIONS;
			v = postProcess(v, searchTarget, algorithm, searchString);
	    }
		return v;
	}


	public String construct_get_concepts_related_to_annotation_property(String propertyName, boolean source) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (source) {
			buf.append("SELECT distinct ?x_label ?x_code").append("\n");
		} else {
			buf.append("SELECT distinct ?z_label ?z_code").append("\n");
		}
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?z a owl:Class .").append("\n");
		buf.append("            ?z rdfs:label ?z_label .").append("\n");
		buf.append("            ?z :NHC0 ?z_code .").append("\n");
		buf.append("            ?y rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getConceptsRelatedToAnnotationProperty(String propertyName, boolean source) {
		String query = construct_get_concepts_related_to_annotation_property(propertyName, source);
		//System.out.println(query);
		Vector v = executeQuery(query);
		v = new ParserUtils().getResponseValues(v);
		return v;
	}

	public String construct_get_annotation_property_values(String code, String propertyName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (code == null) {
			buf.append("SELECT distinct ?x_label ?x_code ?z").append("\n");
		} else  {
			buf.append("SELECT distinct ?z").append("\n");
		}
		buf.append("{").append("\n");
		buf.append("    graph <" + named_graph + ">").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");

		if (code == null) {
			buf.append("            ?x :NHC0 ?x_code .").append("\n");
		} else {
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
		}

		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?y rdfs:label \"" + propertyName + "\"^^xsd:string .").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAnnotationPropertyValues(String propertyName) {
		return getAnnotationPropertyValues(null, propertyName);
	}

	public Vector getAnnotationPropertyValues(String code, String propertyName) {
		String query = construct_get_annotation_property_values(code, propertyName);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
		} else {
			v = new Vector();
		}
		return v;
	}

	public SearchResult search(String associationName, String searchString, String algorithm, int match_option) {
        Vector v = searchByRelationship(associationName, searchString, algorithm, match_option);
		SearchResult sr = new SearchResult();
		List matchedConcepts = new ArrayList();
		Vector w = new Vector();
		HashSet hset = new HashSet();
		//buf.append("SELECT distinct ?x_label ?x_code ?y_label ?z_label ?z_code").append("\n");
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			line = line.trim();
			if (!hset.contains(line)) {
				Vector u = StringUtils.parseData(line, '|');
				String x_label = (String) u.elementAt(0);
				String x_code = (String) u.elementAt(1);
				String y_label = (String) u.elementAt(2);
				String z_label = (String) u.elementAt(3);
				String z_code = (String) u.elementAt(4);

				MatchedConcept mc = null;
				if (match_option == MATCH_SOURCE) {
					mc = new MatchedConcept(z_label, z_code, associationName, x_label);
				} else {
					mc = new MatchedConcept(x_label, x_code, associationName, z_label);
				}
				matchedConcepts.add(mc);
			}
		}
		matchedConcepts = sortMatchedConcepts(matchedConcepts, searchString);
		sr.setMatchedConcepts(matchedConcepts);
		return sr;
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

		//public void set_named_graph(String named_graph) {
		serviceUrl = serviceUrl + "?query=";

		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("codingScheme: " + codingScheme);
		System.out.println("version: " + version);
		System.out.println("named_graph: " + named_graph);

		RelationSearchUtils searchUtils = new RelationSearchUtils(serviceUrl);
		searchUtils.set_named_graph(named_graph);

	    ms = System.currentTimeMillis();
	    Vector v = null;
/*
        String associationName = null;//"Concept_In_Subset";
        String searchString = "Red";
        String algorithm = "exactMatch";
        int search_option = MATCH_SOURCE;
        v = searchUtils.searchByRelationship(associationName, searchString, algorithm, search_option);
        StringUtils.dumpVector("v", v);

        System.out.println("\n");

        searchString = "SPL Color Terminology";
        algorithm = BOOLEAN_AND;
        search_option = MATCH_TARGET;
        v = searchUtils.searchByRelationship(associationName, searchString, algorithm, search_option);
        StringUtils.dumpVector("v", v);

*/

        String queryfile = args[1];
        v = searchUtils.execute(queryfile);
        StringUtils.dumpVector("v", v);


	    System.out.println("Total search run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
