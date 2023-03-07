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


public class LogicalExpression {
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

    public LogicalExpression(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
    	this.named_graph = named_graph;
    	this.username = username;
    	this.password = password;
        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
        rangeHashMap = getRangeHashMap(named_graph);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

	public String construct_get_domain(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?r_code ?r_label ?r_domain_code ?r_domain_label").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + ">").append("\n");
        buf.append("    {").append("\n");
        buf.append("            ?r a owl:ObjectProperty .").append("\n");
        buf.append("            ?r rdfs:label ?r_label .").append("\n");
        buf.append("            ?r :NHC0 ?r_code .").append("\n");
        buf.append("            ?r rdfs:label ?r_label .").append("\n");
        buf.append("            ?r rdfs:domain ?r_domain .").append("\n");
        buf.append("            ?r_domain :NHC0 ?r_domain_code .").append("\n");
        buf.append("            ?r_domain rdfs:label ?r_domain_label .").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public HashMap getDomainHashMap(String named_graph) {
		Vector v = getDomain(named_graph);
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			hmap.put((String) u.elementAt(0), (String) u.elementAt(3));
		}
		return hmap;
	}

	public Vector getDomain(String named_graph) {
        String query = construct_get_domain(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public String construct_get_range(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        buf.append("select distinct ?r_code ?r_label ?r_range_code ?r_range_label").append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + ">").append("\n");
        buf.append("    {").append("\n");
        buf.append("            ?r a owl:ObjectProperty .").append("\n");
        buf.append("            ?r rdfs:label ?r_label .").append("\n");
        buf.append("            ?r :NHC0 ?r_code .").append("\n");
        buf.append("            ?r rdfs:label ?r_label .").append("\n");
        buf.append("            ?r rdfs:range ?r_range .").append("\n");
        buf.append("            ?r_range :NHC0 ?r_range_code .").append("\n");
        buf.append("            ?r_range rdfs:label ?r_range_label .").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector getRangeLabels(String named_graph) {
		Vector v = getRange(named_graph);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String range = (String) u.elementAt(3);
			if (!w.contains(range)) {
				w.add(range);
			}
		}
		return new SortUtils().quickSort(w);
	}

	public HashMap getRangeHashMap(String named_graph) {
		Vector v = getRange(named_graph);
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			hmap.put((String) u.elementAt(0), (String) u.elementAt(3));
		}
		return hmap;
	}

	public Vector getRange(String named_graph) {
        String query = construct_get_range(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

    public String path2SelectStmt(Vector path) {
		//path: <E, I, O, C>
		int e_count = 0;
		int i_count = 0;
		int u_count = 0;
		int c_count = 0;
		StringBuffer buf = new StringBuffer();
		buf.append("select distinct ?x_code ?x_label ");
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
			} else if (t.compareTo("C") == 0) {
				c_count++;
				buf.append("?c" + c_count + "_code").append(" ");
				buf.append("?c" + c_count + "_label").append(" ");
			} else if (t.compareTo("O") == 0) {
				//nothing
			}
		}
		String s = buf.toString();
		s = s.trim();
		return s;
	}

    public String path2Query(String named_graph, String code, Vector path) {
		int e_count = 0;
		int i_count = 0;
		int u_count = 0;
		int c_count = 0;
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        String selectStmt = path2SelectStmt(path);
        buf.append(selectStmt).append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + ">").append("\n");
        buf.append("    {").append("\n");
        buf.append("            ?x :NHC0 ?x_code .").append("\n");
        buf.append("            ?x rdfs:label ?x_label .").append("\n");
        buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");

		for (int i=0; i<path.size(); i++) {
			String t = (String) path.elementAt(i);
			if (t.compareTo("E") == 0) {
				e_count++;
				buf.append("            ?x owl:equivalentClass ?e" + e_count + " .").append("\n");
            // absorbing state
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
			} else if (t.compareTo("C") == 0) {
				//c_count++;
				buf.append("            ?c" + c_count + " a owl:Class .").append("\n");
				buf.append("            ?c" + c_count + " :NHC0 ?c" + c_count + "_code .").append("\n");
				buf.append("            ?c" + c_count + " rdfs:label ?c" + c_count + "_label .").append("\n");
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

	public static boolean isAbsorbingState(String t) {
		return absorbingStates.contains(t);
	}

    public HashMap getLogicalExpressionData(String named_graph, String code, Vector paths) {
		HashMap hmap = new HashMap();
        Stack stack = new Stack();

        for (int i=0; i<paths.size(); i++) {
			stack.push((String) paths.elementAt(i));
		}

        while (!stack.isEmpty()) {
			String line = (String) stack.pop();
			Vector path = StringUtils.parseData(line, '|');
			String query = path2Query(named_graph, code, path);

			System.out.println("\n" + path);
			System.out.println("\n" + query);

			Vector v = executeQuery(query);
			if (v != null && v.size() > 0) {
				hmap.put(line, v);
			}
		}
		return hmap;
    }

	public HashMap sortRestrictions(String path, Vector v) {
		HashMap hmap = new HashMap();
		if (v == null) return hmap;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (path.endsWith("U|O|R") || path.endsWith("I|O|R")) {
				String key = (String) u.elementAt(u.size()-5);
				String value = (String) u.elementAt(u.size()-4) + "|" +
							   (String) u.elementAt(u.size()-3) + "|" +
							   (String) u.elementAt(u.size()-2) + "|" +
				     		   (String) u.elementAt(u.size()-1);
				Vector w = new Vector();
				if (hmap.containsKey(key)) {
					w = (Vector) hmap.get(key);
				}
				w.add(value);
				hmap.put(key, w);
			} else if (path.endsWith("U|O|I|O|R")) {
				String key = (String) u.elementAt(u.size()-6) + "|" +
				             (String) u.elementAt(u.size()-5);
				String value = (String) u.elementAt(u.size()-4) + "|" +
							   (String) u.elementAt(u.size()-3) + "|" +
							   (String) u.elementAt(u.size()-2) + "|" +
				     		   (String) u.elementAt(u.size()-1);
				Vector w = new Vector();
				if (hmap.containsKey(key)) {
					w = (Vector) hmap.get(key);
				}
				w.add(value);
				hmap.put(key, w);
			}
		}
        return hmap;
    }

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		String code = args[4];
        LogicalExpression test = new LogicalExpression(serviceUrl, named_graph, username, password);
        Vector paths = new Vector();
        paths.add("E|I|O|C");
        paths.add("E|I|O|R");
        paths.add("E|I|O|U|O|R");
        paths.add("E|I|O|U|O|I|O|R");
        HashMap hmap = test.getLogicalExpressionData(named_graph, code, paths);
        Iterator it = hmap.keySet().iterator();
        while (it.hasNext()) {
			String path = (String) it.next();
			Vector v = (Vector) hmap.get(path);
			Utils.dumpVector(path, v);

			HashMap hmap2 = test.sortRestrictions(path, v);
			if (hmap2.keySet().size() > 0) {
				Iterator it2 = hmap2.keySet().iterator();
				while (it2.hasNext()) {
					String key = (String) it2.next();
					Vector values = (Vector) hmap2.get(key);
					Utils.dumpVector(key, values);
				}
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));

	}
}
