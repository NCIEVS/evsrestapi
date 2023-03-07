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


public class QueryBuilder {
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String username = null;
    String password = null;
    OWLSPARQLUtils owlSPARQLUtils = null;
    static HashMap transitionMatrix = null;
    static Vector absorbingStates = null;
    static int MAX_LENGTH = 10;
    HashMap rangeHashMap = null;

	public HashMap getRangeHashMap() {
		return this.rangeHashMap;
	}

    public QueryBuilder(String serviceUrl, String named_graph, String username, String password) {
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

    public String path2SelectStmt(Vector path) {
		//path: <E, I, O, C>
		int e_count = 0;
		int i_count = 0;
		int u_count = 0;
		int c_count = 0;
		int a_count = 0;
		int o_count = 0;
		int p_count = 0;
		int r_count = 0;
		StringBuffer buf = new StringBuffer();
		buf.append("select distinct ");
		for (int i=0; i<path.size(); i++) {
			String t = (String) path.elementAt(i);
			if (t.compareTo("R") == 0) {
				buf.append("?p_label ?p_code ?y_code ?y_label").append(" ");
			} else if (t.compareTo("E") == 0) {
				e_count++;
				buf.append("?e" + e_count).append(" ");
			} else if (t.compareTo("I") == 0) {
				i_count++;
				buf.append("?i" + i_count).append(" ");
			} else if (t.compareTo("U") == 0) {
				u_count++;
				buf.append("?u" + u_count).append(" ");
			} else if (t.compareTo("Y") == 0) {
				c_count++;
				buf.append("?c" + c_count + "_code").append(" ");
				buf.append("?c" + c_count + "_label").append(" ");

			} else if (t.compareTo("O") == 0) {
				//nothing
			} else if (t.startsWith("V")) {
				p_count++;
				buf.append("?p" + p_count + "_label").append(" ");
				c_count++;
				buf.append("?c" + c_count + "_code").append(" ");
				buf.append("?c" + c_count + "_label").append(" ");
			} else if (t.compareTo("a") == 0) { // Axiom
				a_count++;
				buf.append("?a" + a_count + "_target").append(" ");

			} else if (t.startsWith("C")) {
				c_count++;
				buf.append("?c" + c_count + "_code").append(" ");
				buf.append("?c" + c_count + "_label").append(" ");

			} else if (t.startsWith("A")) { // association
			    Vector u = StringUtils.parseData(t, '$');
				o_count++;
				buf.append("?o" + o_count + "_label").append(" ");
				c_count++;
				buf.append("?c" + c_count + "_code").append(" ");
				buf.append("?c" + c_count + "_label").append(" ");

			} else if (t.startsWith("P")) {
				p_count++;
				buf.append("?p" + p_count + "_label").append(" ");
				buf.append("?p" + p_count + "_value").append(" ");
			} else if (t.compareTo("sub") == 0) {
				c_count++;
				buf.append("?c" + c_count + "_code").append(" ");
				buf.append("?c" + c_count + "_label").append(" ");
			} else if (t.compareTo("sup") == 0) {
				c_count++;
				buf.append("?c" + c_count + "_code").append(" ");
				buf.append("?c" + c_count + "_label").append(" ");
			} else if (t.startsWith("r")) { // role relationship
				p_count++;
				buf.append("?p" + p_count + "_label").append(" ");
				c_count++;
				buf.append("?c" + c_count + "_code").append(" ");
				buf.append("?c" + c_count + "_label").append(" ");
			}
		}
		String s = buf.toString();
		s = s.trim();
		return s;
	}

    public String path2Query(String named_graph, Vector path) {
		int e_count = 0;
		int i_count = 0;
		int u_count = 0;
		int c_count = 0;
		int v_count = 0; // value set
		int p_count = 0;
		int a_count = 0;
		int o_count = 0;
		int q_count = 0;
		int r_count = 0;

		StringBuffer b = new StringBuffer();
		for (int i=0; i<path.size(); i++) {
			String s = (String) path.elementAt(i);
			b.append(s).append('|');
		}
		String path_str = b.toString();
		path_str = path_str.substring(0, path_str.length());
        if (path_str.startsWith("C|r$C")) {
			int n = path_str.indexOf("$");
			String code = path_str.substring(n+1, path_str.length()-1);
			return construct_get_inverse_roles(named_graph, code);
		}

        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        String selectStmt = path2SelectStmt(path);
        buf.append(selectStmt).append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + ">").append("\n");
        buf.append("    {").append("\n");
/*
        if (path.contains("E")) {
			buf.append("            ?x :NHC0 ?x_code .").append("\n");
			buf.append("            ?x rdfs:label ?x_label .").append("\n");
			buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
	    }
*/



		for (int i=0; i<path.size(); i++) {
			String t = (String) path.elementAt(i);
			if (t.compareTo("E") == 0) {
				e_count++;
				buf.append("            ?x owl:equivalentClass ?e" + e_count + " .").append("\n");

			} else if (t.compareTo("R") == 0) {
				buf.append("            ?rs a owl:Restriction"  + " .").append("\n");
				buf.append("            ?rs owl:onProperty ?p"  + " .").append("\n");
				buf.append("            ?p :NHC0 ?p_code"  + " .").append("\n");
				buf.append("            ?p rdfs:label ?p_label"  + " .").append("\n");
				buf.append("            ?rs owl:someValuesFrom ?y"  + " .").append("\n");
				buf.append("            ?y :NHC0 ?y_code .").append("\n");
				buf.append("            ?y rdfs:label ?y_label .").append("\n");

			} else if (t.compareTo("I") == 0) {
				String s = (String) path.elementAt(i-1);
				s = s.toLowerCase();

				i_count++;
				int s_count = e_count;
				if (s.compareTo("e") == 0) s_count = e_count;
				if (s.compareTo("u") == 0) s_count = u_count;
				if (s.compareTo("i") == 0) s_count = i_count;
				if (s.compareTo("o") == 0) s_count = i_count-1;

				if (s.compareTo("o") == 0) {
					buf.append("            ?i" + s_count + " owl:intersectionOf ?i" + i_count + " .").append("\n");
				} else {
					buf.append("            ?" + s + s_count + " owl:intersectionOf ?i" + i_count + " .").append("\n");
			    }

			} else if (t.compareTo("U") == 0) {
				String s = (String) path.elementAt(i-1);
				s = s.toLowerCase();

				u_count++;
				int s_count = e_count;
				if (s.compareTo("e") == 0) s_count = e_count;
				if (s.compareTo("u") == 0) s_count = u_count;
				if (s.compareTo("i") == 0) s_count = i_count;
				if (s.compareTo("o") == 0) s_count = u_count-1;

				if (s.compareTo("o") == 0) {
					buf.append("            ?u" + s_count + " owl:unionOf ?u" + u_count + " .").append("\n");
				} else {
					buf.append("            ?" + s + s_count + " owl:unionOf ?u" + u_count + " .").append("\n");
			    }
			} else if (t.startsWith("C")) {
				c_count++;
				buf.append("            ?c" + c_count + " a owl:Class .").append("\n");
				buf.append("            ?c" + c_count + " :NHC0 ?c" + c_count + "_code .").append("\n");
				buf.append("            ?c" + c_count + " rdfs:label ?c" + c_count + "_label .").append("\n");

				Vector u = StringUtils.parseData(t, '$');
				if (u.size() > 1) {
					String id = (String) u.elementAt(1);
				    buf.append("            ?c" + c_count + " :NHC0 \"" + id + "\"^^xsd:string .").append("\n");
				}
			} else if (t.compareTo("O") == 0) {
				String s = (String) path.elementAt(i-1);
				s = s.toLowerCase();
				int s_count = e_count;
				if (s.compareTo("e") == 0) s_count = e_count;
				if (s.compareTo("u") == 0) s_count = u_count;
				if (s.compareTo("i") == 0) s_count = i_count;

				t = (String) path.elementAt(i+1);
				if (t.compareTo("C") == 0) {
					c_count++;
					buf.append("            ?" + s + s_count + " rdf:rest*/rdf:first ?c" + c_count + " .").append("\n");
				} else if (t.compareTo("R") == 0) {
					buf.append("            ?" + s + s_count + " rdf:rest*/rdf:first ?rs" + " .").append("\n");
				} else if (t.compareTo("U") == 0) {
					u_count++;
					buf.append("            ?" + s + s_count + " rdf:rest*/rdf:first ?u" + u_count + " .").append("\n");
				} else if (t.compareTo("I") == 0) {
					i_count++;
					buf.append("            ?" + s + s_count + " rdf:rest*/rdf:first ?i" + i_count + " .").append("\n");
				}
			} else if (t.startsWith("V")) {
				int n = c_count;
				c_count++;
				buf.append("            ?c" + c_count + " a owl:Class .").append("\n");
				buf.append("            ?c" + c_count + " :NHC0 ?c" + c_count + "_code .").append("\n");
				buf.append("            ?c" + c_count + " rdfs:label ?c" + c_count + "_label .").append("\n");

				p_count++;
				buf.append("            ?c" + n + " ?p" + p_count + " ?c" + c_count + " .").append("\n");
				buf.append("            ?p" + p_count + " rdfs:label ?p" + p_count + "_label .").append("\n");
				buf.append("            ?p" + p_count + " rdfs:label " + "\"Concept_In_Subset\"^^xsd:string .").append("\n");

				Vector u = StringUtils.parseData(t, '$');
				if (u.size() > 1) {
					String id = (String) u.elementAt(1);
				    buf.append("            ?c" + c_count + " :NHC0 \"" + id + "\"^^xsd:string .").append("\n");
				}

			} else if (t.startsWith("A")) { //object-valued property / association
			    Vector u = StringUtils.parseData(t, '$');
				o_count++;
				int n = c_count;
				c_count++;
				String prop_name = null;
				String id = null;
				if (u.size() > 0) {
					prop_name = (String) u.elementAt(1);
				}
				if (u.size() > 1) {
					id = (String) u.elementAt(2);
				}

				if (u.size() == 1) {
					buf.append("            ?c" + n + " ?o" + o_count + " ?c" + c_count + " .").append("\n");
				} else if (u.size() == 2) {
					buf.append("            ?c" + c_count + " ?o" + o_count + " ?c" + n + " .").append("\n");
				}

				buf.append("            ?c" + c_count + " :NHC0 ?c" + c_count + "_code .").append("\n");

				if (id != null) {
					buf.append("            ?c" + c_count + " :NHC0 \"" + id + "\"^^xsd:string .").append("\n");
				}

				buf.append("            ?c" + c_count + " rdfs:label ?c" + c_count + "_label .").append("\n");
				buf.append("            ?o" + o_count + " rdfs:label ?o" + o_count + "_label .").append("\n");

				if (prop_name != null) {
					buf.append("            ?o" + o_count + " rdfs:label " + "\"" + prop_name + "\"^^xsd:string .").append("\n");
			    }

			} else if (t.compareTo("a") == 0) { // axiom
				p_count++;
				a_count++;
			    buf.append("            ?p" + p_count + " a owl:AnnotationProperty .").append("\n");
			    buf.append("            ?p" + p_count + " rdfs:label ?p" + p_count + "_label .").append("\n");
			    buf.append("            ?p" + p_count + " :NHC0 \"P90\"^^xsd:string .").append("\n");
			    buf.append("            ?a" + a_count + " a owl:Axiom .").append("\n");
			    buf.append("            ?a" + a_count + " owl:annotatedSource ?c" + c_count + " .").append("\n");
			    buf.append("            ?a" + a_count + " owl:annotatedTarget ?a" + a_count + "_target .	").append("\n");

			} else if (t.startsWith("P")) {
				p_count++;
				buf.append("            ?c" + c_count + " ?p" + p_count + " ?p" + p_count + "_value .").append("\n");
				buf.append("            ?p" + p_count + " rdfs:label ?p" + p_count + "_label .").append("\n");

				Vector u = StringUtils.parseData(t, '$');
				if (u.size() > 1) {
					String prop_name = (String) u.elementAt(1);
				    buf.append("            ?p" + p_count + " rdfs:label " + "\"" + prop_name + "\"^^xsd:string .").append("\n");
				}
			} else if (t.startsWith("Q")) {
				q_count++;
			    buf.append("                ?q" + q_count + " :NHC0 ?q" + q_count + "_code .").append("\n");
			    buf.append("                ?q" + q_count + " rdfs:label ?q" + q_count + "_label .").append("\n");
			    buf.append("                ?a" + a_count + " ?q" + q_count + " ?q" + q_count + "_value .").append("\n");

				Vector u = StringUtils.parseData(t, '$');
				if (u.size() == 2) {
					String qualifier_code = (String) u.elementAt(1);
				    buf.append("                ?q" + q_count + " :NHC0 \"" + qualifier_code + "\"^^xsd:string .").append("\n");
				} else if (u.size() == 3) {
					String qualifier_code = (String) u.elementAt(1);
					String qualifier_value = (String) u.elementAt(2);
				    buf.append("                ?q" + q_count + " :NHC0 \"" + qualifier_code + "\"^^xsd:string .").append("\n");
				    buf.append("                ?a" + a_count + " ?q" + q_count + "\"" + qualifier_value + "\"^^xsd:string .").append("\n");
				}
            } else if (t.compareTo("sub") == 0) {
				int n = c_count;
				c_count++;
				buf.append("            ?c" + c_count + " :NHC0 ?c" + c_count + "_code .").append("\n");
				buf.append("            ?c" + c_count + " rdfs:label ?c" + c_count + "_label .").append("\n");
				buf.append("            ?c" + c_count + " (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?c" + n + ". ").append("\n");
            } else if (t.compareTo("sup") == 0) {
				int n = c_count;
				c_count++;
				buf.append("            ?c" + c_count + " :NHC0 ?c" + c_count + "_code .").append("\n");
				buf.append("            ?c" + c_count + " rdfs:label ?c" + c_count + "_label .").append("\n");
				buf.append("            ?c" + n + " (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?c" + c_count + ". ").append("\n");
            } else if (t.startsWith("r")) {
			    Vector u = StringUtils.parseData(t, '$');
				String role_name = null;
				String id = null;
				if (u.size() > 1) {
					id = (String) u.elementAt(1);
				}
				if (u.size() > 2) {
					role_name = (String) u.elementAt(2);
				}
				int n = c_count;
				c_count++;
				buf.append("            ?c" + c_count + " :NHC0 ?c" + c_count + "_code .").append("\n");
				buf.append("            ?c" + c_count + " rdfs:label ?c" + c_count + "_label .").append("\n");
				if (id != null) {
					buf.append("            ?c" + c_count + " :NHC0 \"" + id + "\"^^xsd:string .").append("\n");
				}
				p_count++;
				buf.append("            ?p" + p_count + " :NHC0 ?p" + p_count + "_code .").append("\n");
				buf.append("            ?p" + p_count + " rdfs:label ?p" + p_count + "_label .").append("\n");
				if (role_name != null) {
					buf.append("            ?p" + p_count + " rdfs:label \"" + role_name + "\"^^xsd:string .").append("\n");
				}
				r_count++;
				if (id == null) {
					buf.append("            ?c" + n + " (rdfs:subClassOf|owl:equivalentClass|owl:unionOf/rdf:rest*/rdf:first|owl:intersectionOf/rdf:rest*/rdf:first)* ?rs" + r_count + " .  ").append("\n");
					buf.append("            ?rs" + r_count + " a owl:Restriction .").append("\n");
					buf.append("            ?rs" + r_count + " owl:onProperty ?p" + p_count + " .").append("\n");
					buf.append("            ?rs" + r_count + " owl:someValuesFrom ?c" + c_count + " .").append("\n");
				}
		    }
		}
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector executeQuery(String query) {
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

    public HashMap getQueryBuilderData(String named_graph, Vector paths) {
		HashMap hmap = new HashMap();
        Stack stack = new Stack();

        for (int i=0; i<paths.size(); i++) {
			stack.push((String) paths.elementAt(i));
		}

        while (!stack.isEmpty()) {
			String line = (String) stack.pop();
			Vector path = StringUtils.parseData(line, '|');
			String query = path2Query(named_graph, path);

			System.out.println("\n" + path);
			System.out.println("\n" + query);

			Vector v = executeQuery(query);
			if (v != null && v.size() > 0) {
				hmap.put(line, v);
			}
		}
		return hmap;
    }

	public String construct_get_inverse_roles(String named_graph, String code) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?x_code ?x_label ?p_code ?p_label ?y_code ?y_label").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + "> ").append("\n");
        buf.append("    {").append("\n");
        buf.append("            ?x :NHC0 ?x_code .").append("\n");
        buf.append("            ?x rdfs:label ?x_label .").append("\n");
        buf.append("            ?y :NHC0 ?y_code .").append("\n");
        buf.append("            ?y :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
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
}