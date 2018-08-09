package gov.nih.nci.evs.restapi.util; import gov.nih.nci.evs.restapi.appl.*; import gov.nih.nci.evs.restapi.bean.*;

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

	private static String CONTAINS = "contains";
	private static String EXACT_MATCH = "exactMatch";
	private static String STARTS_WITH = "startsWith";
	private static String ENDS_WITH = "startsWith";
    private static Vector supportedAssociations = null;

	public RelationSearchUtils(String serviceUrl) {
		super(serviceUrl);
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);

    }

	public RelationSearchUtils(String serviceUrl, String named_graph, Vector concept_in_subset_vec) {
		super(serviceUrl);
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);

		supportedAssociations = getSupportedAssociations();
		supportedAssociations = new ParserUtils().getValues(supportedAssociations, 0);

    }

    public void initialize(String named_graph) {
		long ms = System.currentTimeMillis();
		this.named_graph = named_graph;
		this.owlSPARQLUtils.set_named_graph(named_graph);
		this.set_named_graph(named_graph);

		supportedAssociations = getSupportedAssociations();
		supportedAssociations = new ParserUtils().getValues(supportedAssociations, 0);

		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

    public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;

		supportedAssociations = getSupportedAssociations();
		supportedAssociations = new ParserUtils().getValues(supportedAssociations, 0);

	}


	public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}


	public String construct_filter_stmt(String var, Vector properties) {
		StringBuffer buf = new StringBuffer();
		buf.append("FILTER (");
		for (int i=0; i<properties.size(); i++) {
			String property = (String) properties.elementAt(i);
			buf.append("(str(?" + var + ")=\"" + property + "\"^^xsd:string)");
			if (i < properties.size()-1) {
				buf.append(" || ");
			}
		}
		buf.append(")");
		return buf.toString();
    }


	public Vector postProcess(Vector w, String searchTarget, String algorithm, String searchString) {
		if (w == null) return null;
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
		Vector v = null;
		if (associationName != null) {
			if (supportedAssociations.contains(associationName)) {
				v = association_query(this.named_graph, associationName, searchString, algorithm, match_option);
			} else {
				v = role_query(this.named_graph, associationName, searchString, algorithm, match_option);
			}
		} else {
			v = association_query(this.named_graph, associationName, searchString, algorithm, match_option);
			if (v == null || v.size() == 0) {
				v = role_query(this.named_graph, associationName, searchString, algorithm, match_option);
			}
		}
		if (v == null || v.size() == 0) {
			return null;
		}
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
				/*
				to be implemented
				if (match_option == MATCH_SOURCE) {
					mc = new MatchedConcept(z_label, z_code, associationName, x_label);
				} else {
					mc = new MatchedConcept(x_label, x_code, associationName, z_label);
				}
				matchedConcepts.add(mc);
				*/

			}
		}
		matchedConcepts = sortMatchedConcepts(matchedConcepts, searchString);
		sr.setMatchedConcepts(matchedConcepts);
		return sr;
	}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Role search
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static int MATCH_SOURCE = 1;
	public static int MATCH_TARGET = 2;

/*
	public String apply_text_match(String var, String matchText, String algorithm) {
		matchText = matchText.toLowerCase();
		if (algorithm.compareTo(CONTAINS) == 0) {
			return "FILTER contains(lcase(str(?" + var + ")), \"" + matchText + "\")";
		} else if (algorithm.compareTo(EXACT_MATCH) == 0) {
			return "FILTER (lcase(str(?" + var + ")) = \"" + matchText + "\")";
		} else if (algorithm.compareTo(STARTS_WITH) == 0) {
			return "FILTER regex(?" + var + ", '^" + matchText + "', 'i')";
		} else if (algorithm.compareTo(ENDS_WITH) == 0) {
			return "FILTER regex(?" + var + ", '" + matchText + "^', 'i')";
		} else {
			matchText = matchText.replaceAll("%20", " ");
			matchText = createSearchString(matchText, algorithm);
			return"(?"+ var +") <tag:stardog:api:property:textMatch> (" + matchText + ").";
		}
		return matchText;
	}
*/

    public String apply_text_match(String var, String matchText, String algorithm) {
		return SPARQLUtils.apply_text_match(var, matchText, algorithm);
	}

    public String construct_role_query(String named_graph, String role, String matchText, String algorithm, int match_option) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		String filterStmt = null;
		if (match_option == MATCH_TARGET) {
			filterStmt = apply_text_match("y_label", matchText, algorithm);
	    } else {
			filterStmt = apply_text_match("x_label", matchText, algorithm);
		}
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code").append("\n");
		buf.append("{ ").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:subClassOf ?r .").append("\n");
		buf.append("?r a owl:Restriction .").append("\n");
		buf.append("?r owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");

			buf.append("		?r owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}	").append("\n");
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

			buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("UNION").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("?z0 a owl:Class .").append("\n");
		buf.append("?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("?z2 a owl:Restriction .").append("\n");
		buf.append("?z2 owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");

			buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("UNION").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
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

			buf.append("		?z4 owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector role_query(String named_graph, String role, String matchText, String algorithm, int match_option) {
		String query = construct_role_query(named_graph, role, matchText, algorithm, match_option);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
		} else {
			v = new Vector();
		}
		return new SortUtils().quickSort(v);
	}



    public String construct_role_query(String named_graph, String role, String matchText, String algorithm, int match_option, String propertyName) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		String filterStmt = null;
		if (match_option == MATCH_TARGET) {
			filterStmt = apply_text_match("y_prop_value", matchText, algorithm);
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ?y_prop_value").append("\n");
	    } else {
			filterStmt = apply_text_match("x_prop_value", matchText, algorithm);
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ?x_prop_value").append("\n");
		}

		buf.append("{ ").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");

		buf.append("?x ?x_prop ?x_prop_value .").append("\n");
		buf.append("?x_prop rdfs:label ?x_prop_label .").append("\n");
		buf.append("?x rdfs:label ?prop_label .").append("\n");

		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:subClassOf ?r .").append("\n");
		buf.append("?r a owl:Restriction .").append("\n");
		buf.append("?r owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");

			buf.append("		?r owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y ?y_prop ?y_prop_value .").append("\n");
			buf.append("		?y_prop rdfs:label ?y_prop_label .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");

			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

        if (match_option == MATCH_TARGET) {
			buf.append("FILTER (str(?y_prop_label)=\"" + propertyName + "\"^^xsd:string)").append("\n");
		} else {
			buf.append("FILTER (str(?x_prop_label)=\"" + propertyName + "\"^^xsd:string)").append("\n");
		}

		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}	").append("\n");
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

			buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y ?y_prop ?y_prop_value .").append("\n");
			buf.append("		?y_prop rdfs:label ?y_prop_label .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");


        if (match_option == MATCH_TARGET) {
			buf.append("FILTER (str(?y_prop_label)=\"" + propertyName + "\"^^xsd:string)").append("\n");
		} else {
			buf.append("FILTER (str(?x_prop_label)=\"" + propertyName + "\"^^xsd:string)").append("\n");
		}



		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("UNION").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("?z0 a owl:Class .").append("\n");
		buf.append("?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("?z2 a owl:Restriction .").append("\n");
		buf.append("?z2 owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");

			buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y ?y_prop ?y_prop_value .").append("\n");
			buf.append("		?y_prop rdfs:label ?y_prop_label .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");


        if (match_option == MATCH_TARGET) {
			buf.append("FILTER (str(?y_prop_label)=\"" + propertyName + "\"^^xsd:string)").append("\n");
		} else {
			buf.append("FILTER (str(?x_prop_label)=\"" + propertyName + "\"^^xsd:string)").append("\n");
		}


		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("UNION").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
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

			buf.append("		?z4 owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y ?y_prop ?y_prop_value .").append("\n");
			buf.append("		?y_prop rdfs:label ?y_prop_label .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

        if (match_option == MATCH_TARGET) {
			buf.append("FILTER (str(?y_prop_label)=\"" + propertyName + "\"^^xsd:string)").append("\n");
		} else {
			buf.append("FILTER (str(?x_prop_label)=\"" + propertyName + "\"^^xsd:string)").append("\n");
		}


		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector role_query(String named_graph, String role, String matchText, String algorithm, int match_option, String propertyName) {
		String query = construct_role_query(named_graph, role, matchText, algorithm, match_option, propertyName);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
		} else {
			v = new Vector();
		}
		return new SortUtils().quickSort(v);
	}


    public String construct_role_query(String named_graph, String role, String matchText, String algorithm, int match_option, Vector properties) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		String filterStmt = null;

		String prop_filter = null;
		if (match_option == MATCH_TARGET) {
			filterStmt = apply_text_match("y_prop_value", matchText, algorithm);
			prop_filter = construct_filter_stmt("y_prop_label", properties);
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ?y_prop_value").append("\n");
	    } else {
			filterStmt = apply_text_match("x_prop_value", matchText, algorithm);
			prop_filter = construct_filter_stmt("x_prop_label", properties);
			buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code ?x_prop_value").append("\n");
		}

		buf.append("{ ").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");

		buf.append("?x ?x_prop ?x_prop_value .").append("\n");
		buf.append("?x_prop rdfs:label ?x_prop_label .").append("\n");
		buf.append("?x rdfs:label ?prop_label .").append("\n");

		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x rdfs:subClassOf ?r .").append("\n");
		buf.append("?r a owl:Restriction .").append("\n");
		buf.append("?r owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");

			buf.append("		?r owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y ?y_prop ?y_prop_value .").append("\n");
			buf.append("		?y_prop rdfs:label ?y_prop_label .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");

			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

		buf.append(prop_filter).append("\n");
		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}	").append("\n");
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

			buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y ?y_prop ?y_prop_value .").append("\n");
			buf.append("		?y_prop rdfs:label ?y_prop_label .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

        buf.append(prop_filter).append("\n");
		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("UNION").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x owl:equivalentClass ?z0 .").append("\n");
		buf.append("?z0 a owl:Class .").append("\n");
		buf.append("?z0 owl:intersectionOf ?list .").append("\n");
		buf.append("?list rdf:rest*/rdf:first ?z2 .").append("\n");
		buf.append("?z2 a owl:Restriction .").append("\n");
		buf.append("?z2 owl:onProperty ?p .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");

			buf.append("		?z2 owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y ?y_prop ?y_prop_value .").append("\n");
			buf.append("		?y_prop rdfs:label ?y_prop_label .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

        buf.append(prop_filter).append("\n");
		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("UNION").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
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

			buf.append("		?z4 owl:someValuesFrom ?y .").append("\n");
			buf.append("		?y a owl:Class .").append("\n");
			buf.append("		?y ?y_prop ?y_prop_value .").append("\n");
			buf.append("		?y_prop rdfs:label ?y_prop_label .").append("\n");
			buf.append("		?y :NHC0 ?y_code .").append("\n");
			buf.append("		?y rdfs:label ?y_label").append("\n");
			buf.append(filterStmt).append("\n");

        buf.append(prop_filter).append("\n");
		if (role != null) {
			buf.append("FILTER (str(?p_label)=\"" + role + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		buf.append("} ").append("\n");
		return buf.toString();
	}

	public Vector role_query(String named_graph, String role, String matchText, String algorithm, int match_option, Vector properties) {
		String query = construct_role_query(named_graph, role, matchText, algorithm, match_option, properties);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
		} else {
			v = new Vector();
		}
		return new SortUtils().quickSort(v);
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Association search
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String construct_association_query(String named_graph, String association, String matchText, String algorithm, int match_option) {
        String prefixes = getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);

		String filterStmt = null;
		if (match_option == MATCH_TARGET) {
			filterStmt = apply_text_match("y_label", matchText, algorithm);
	    } else {
			filterStmt = apply_text_match("x_label", matchText, algorithm);
		}
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code").append("\n");
        buf.append("{").append("\n");
        buf.append("graph <" + named_graph + ">").append("\n");
        buf.append("{").append("\n");
        buf.append("?x a owl:Class .").append("\n");
        buf.append("?x rdfs:label ?x_label .").append("\n");
        buf.append("?x :NHC0 ?x_code .").append("\n");
        buf.append("?p a owl:AnnotationProperty .").append("\n");
        buf.append("?p rdfs:label ?p_label .").append("\n");
        if (association != null) {
        	buf.append("?p rdfs:label \"" + association + "\"^^xsd:string .").append("\n");
		}
        buf.append("?x ?p ?y .").append("\n");
        buf.append("?y a owl:Class .").append("\n");
        buf.append("?y rdfs:label ?y_label .").append("\n");
        buf.append("?y :NHC0 ?y_code .").append("\n");
        buf.append(filterStmt).append("\n");
        buf.append("}").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
    }

	public Vector association_query(String named_graph, String association, String matchText, String algorithm, int match_option) {
		String query = construct_association_query(named_graph, association, matchText, algorithm, match_option);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
		} else {
			v = new Vector();
		}
		return new SortUtils().quickSort(v);
	}


	public Vector getSupportedAssociations() {
        return owlSPARQLUtils.getSupportedAssociations(this.named_graph);
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

		named_graph = args[1];

		System.out.println("named_graph: " + named_graph);

		RelationSearchUtils searchUtils = new RelationSearchUtils(serviceUrl);
		searchUtils.set_named_graph(named_graph);

	    ms = System.currentTimeMillis();
	    Vector v = null;

	    System.out.println("Total search run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
