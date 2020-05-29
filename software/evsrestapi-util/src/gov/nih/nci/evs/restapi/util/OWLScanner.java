package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;
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
import java.text.*;

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


public class OWLScanner {
    String owlfile = null;
    Vector owl_vec = null;
    static String NCIT_NAMESPACE_TARGET = "<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#";
    static String OWL_CLS_TARGET = NCIT_NAMESPACE_TARGET + "C"; //"<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C";
	static String open_tag = "<owl:Axiom>";
	static String close_tag = "</owl:Axiom>";
	static String owlannotatedSource = "owl:annotatedSource";
	static String owlannotatedProperty = "<owl:annotatedProperty";
	static String owlannotatedTarget_open = "owl:annotatedTarget";
	static String owlannotatedTarget_close = "</owl:annotatedTarget>";
	static String pt_code = "P108";
	static String pt_tag_open = "<P108>";

    public OWLScanner() {

    }

    public OWLScanner(String owlfile) {
        this.owlfile = owlfile;
        this.owl_vec = readFile(owlfile);
    }

    public Vector get_owl_vec() {
		return this.owl_vec;
	}

	public static String getToday() {
		return getToday("MM-dd-yyyy");
	}

	public static String getToday(String format) {
		java.util.Date date = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

    public static HashMap constructHashMap(Vector v, int index_key, int index_value, boolean skip_heading) {
        HashMap hmap = new HashMap();
        int istart = 0;
        if (skip_heading) istart = 1;
        for (int i=istart; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String key = (String) u.elementAt(index_key);
			String value = (String) u.elementAt(index_value);
			hmap.put(key, value);
		}
		return hmap;
	}

	public static Vector readFile(String filename)
	{
		Vector v = new Vector();
		try {
			BufferedReader in = new BufferedReader(
			   new InputStreamReader(
						  new FileInputStream(filename), "UTF8"));
			String str;
			while ((str = in.readLine()) != null) {
				v.add(str);
			}
            in.close();
		} catch (Exception ex) {
            ex.printStackTrace();
		}
		return v;
	}

    public static void dumpVector(String label, Vector v) {
		System.out.println("\n" + label + ":");
		if (v.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			System.out.println("\t(" + j + ") " + t);
		}
		System.out.println("\n");
	}

    public static void dumpVector(String label, Vector v, boolean display_label, boolean display_index) {
		if (display_label) {
			System.out.println("\n" + label + ":");
		}
		if (v.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			if (display_index) {
				System.out.println("\t(" + j + ") " + t);
			} else {
				System.out.println("\t" + t);
			}
		}
		System.out.println("\n");
	}

    public static void dumpVector(PrintWriter pw, String label, Vector v) {
		pw.println("\n" + label + ":");
		if (v.size() == 0) {
			pw.println("\tNone");
			return;
		}
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			pw.println("\t(" + j + ") " + t);
		}
		pw.println("\n");
	}

    public static void dumpVector(PrintWriter pw, String label, Vector v, boolean display_label, boolean display_index) {
		if (display_label) {
			pw.println("\n" + label + ":");
		}
		if (v.size() == 0) {
			pw.println("\tNone");
			return;
		}
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			if (display_index) {
				pw.println("\t(" + j + ") " + t);
			} else {
				pw.println("\t" + t);
			}
		}
		pw.println("\n");
	}

    public static Vector parseData(String line, char delimiter) {
		if(line == null) return null;
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c == delimiter) {
				w.add(buf.toString());
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		w.add(buf.toString());
		return w;
	}

    public boolean fileExists(String filename) {
		File file = new File(filename);
		return file.exists();
	}

	public Vector getOWLClasses() {
		Vector w = new Vector();
		for (int i=0; i<owl_vec.size(); i++) {
			String line = (String) owl_vec.elementAt(i);
			if (line.indexOf(OWL_CLS_TARGET) != -1 && line.indexOf("enum") == -1) {
				w.add(line);
			}
		}
		return w;
	}

	public Vector getOWLClassDataByCode(String code) {
		Vector w = new Vector();
		String target = NCIT_NAMESPACE_TARGET + code + " -->";
		boolean istart = false;
		for (int i=0; i<owl_vec.size(); i++) {
			String line = (String) owl_vec.elementAt(i);
			if (line.indexOf(target) != -1) {
				istart = true;
			} else if (istart && line.indexOf(OWL_CLS_TARGET) != -1) {
                istart = false;
			}
			if (istart) {
				w.add(line);
			}
		}
		return w;
	}

	public Vector getOWLClassDataByCode(Vector codes) {
		Vector v = new Vector();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			Vector w = getOWLClassDataByCode(code);
			v.addAll(w);
		}
		return v;
	}

    public String getCode(String line) {
		int n = line.lastIndexOf("#");
		String t = line.substring(n+1, line.length());
		n = t.indexOf("\"");
		t = t.substring(0, n);
		return t;
	}

	public String extractCode(String line) {
		int n = line.lastIndexOf("#");
		line = line.substring(n+1, line.length()-3);
        return line;
    }

	public String extractAnnotatedTarget(String line) {
		int n = line.indexOf(owlannotatedTarget_open);
		line = line.substring(n+owlannotatedTarget_open.length()+1, line.length());
		n = line.lastIndexOf(owlannotatedTarget_close);
		line = line.substring(0, n);
        return line.trim();
    }

	public String extractQualifier(String line) {
		int n = line.indexOf(">");
        return line.substring(1, n);
    }
    //<P378>CDISC</P378>
	public String extractQualifierValue(String line) {
		int n = line.indexOf(">");
		line = line.substring(n+1, line.length());
		n = line.lastIndexOf("<");
		line = line.substring(0, n);
        return line.trim();
    }


	private void printFootnote(PrintWriter pw) {
	    pw.println("<!-- owl:disjointWith modified by the OWLScanner (version 1.0) on " + getToday() + " -->");
    }

    public Vector parseSynonymData(Vector axiom_data) {
		return new ParserUtils().parseSynonymData(axiom_data);
	}

	public int findMaxLevel() {
		return findMaxLevel(owl_vec);
	}

    public String getPath(String[] nodes, int curr_level) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<curr_level; i++) {
			buf.append(nodes[i]).append("|");
		}
		String t = buf.toString();
		return t.substring(0, t.length()-1);
	}

	public String getIndentation(int level) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<level; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}

	public void printPath(String path) {
		Vector u = StringUtils.parseData(path, '|');
		for (int i=0; i<u.size(); i++) {
			String node = (String) u.elementAt(i);
			System.out.println(getIndentation(i) + node);
		}
	}


	public void printPaths(Vector paths) {
		for (int i=0; i<paths.size(); i++) {
			String path = (String) paths.elementAt(i);
			System.out.println("\n");
			printPath(path);
		}
	}

	public int count(String target) {
		int knt = 0;
		int i = 0;
		while (i < owl_vec.size()) {
			String line = (String) owl_vec.elementAt(i);
			line = line.trim();
			if (line.indexOf(target) != -1) {
				knt++;
			}
			i++;
		}
		return knt;
	}

	public int findMaxIndentation(Vector v) {
		int max = -1;
		for (int i=0; i<v.size(); i++) {
			String str = (String) v.elementAt(i);
			int knt = countIndentation(str);
			if (knt > max) {
				max = knt;
			}
		}
        return max;
	}


	public int findMaxLevel(Vector v) {
		int max = -1;
		for (int i=0; i<v.size(); i++) {
			String str = (String) v.elementAt(i);
			int knt = findLevel(str);
			if (knt > max) {
				max = knt;
			}
		}
        return max;
	}

	public int findLevel(String str) {
		int knt = countIndentation(str);
		if (knt == -1) return knt;
		return knt/4;
	}


	public int countIndentation(String str) {
		String owl_open = "<owl:";
		String owl_close = "</owl:>";

		String owlClass_open = "<owl:Class";
		String owlClass_close = "</owl:Class>";

		String rdfssubClassOf_open = "<rdfs:subClassOf";
		String rdfssubClassOf_close = "</rdfs:subClassOf>";


		String t = str;
		t = t.trim();
		if (!t.startsWith(owl_open) && !t.startsWith(rdfssubClassOf_open) ) {
			return -1;
		}
		int k = 0;
		for (int i=0; i<str.length(); i++) {
			char c = str.charAt(i);
			if (c == ' ') {
				k++;
			} else {
				return k;
			}
		}
		return -1;
	}

	public String[] initializeNodes(int n) {
		return new String[n];
	}

	public String getFirstToken(String str) {
		StringBuffer buf = new StringBuffer();
		str = str.trim();
		for (int i=0; i<str.length(); i++) {
			char c = str.charAt(i);
			if (c == ' ') {
				return str.substring(0, i);
			}
		}
		return str;
	}

    public String getOpenTag(String str) {
		if (str.indexOf("<owl:annotatedTarget>") != -1) {
			return "owl:annotatedTarget";
		}
	    String t = str;
	    t = t.trim();
	    t = getFirstToken(t);

		if (t.startsWith("<")) {
			t = t.substring(1, t.length());
		}
		if (t.endsWith(">")) {
			t = t.substring(0, t.length()-1);
		}
		return t;
	}

    public Vector scanAxioms() {
 		return scanAxioms(owl_vec);
 	}

    public Vector scanAxioms(Vector data_vec) {
		String label = null;
		String owlannotatedSource_value = null;
		String owlannotatedProperty_value = null;
		String owlannotatedTarget_value = null;
		String qualify_data = null;

		String axiom_open_tag = "<owl:Axiom>";

		Vector w = new Vector();
		int i = 0;
		String target = open_tag;
		boolean istart = false;
		Vector v = new Vector();
		String class_line = null;

		StringBuffer buf = new StringBuffer();
		boolean owlannotatedTarget_start = false;

		int class_knt = 0;
		int axiom_knt = 0;

		while (i < data_vec.size()) {
			String line = (String) data_vec.elementAt(i);
			if (line.indexOf(OWL_CLS_TARGET) != -1 && line.indexOf("enum") == -1) {
				class_knt++;
			} else if (line.indexOf(axiom_open_tag) != -1) {
				axiom_knt++;
			}

			if (line.indexOf(pt_tag_open) != -1) {
				label = extractQualifierValue(line);
			}

			line = line.trim();
			//line = line.trim();
			if (line.indexOf(open_tag) != -1) {
				v = new Vector();
				istart = true;
			} else if (istart && line.indexOf(close_tag) != -1) {
				//// process data collection
                w.addAll(v);
                v = new Vector();
                istart = false;
			}  else if (istart) {
				if (line.indexOf(owlannotatedSource) != -1) {
					owlannotatedSource_value = line;
				} else if (line.indexOf(owlannotatedProperty) != -1) {
					owlannotatedProperty_value = line;
				} else if (line.indexOf(owlannotatedTarget_open) != -1) {
					buf.append(line);
					if (line.indexOf(owlannotatedTarget_close) != -1) {
						owlannotatedTarget_start = false;
						owlannotatedTarget_value = buf.toString();
					} else {
						owlannotatedTarget_start = false;
						if (line.indexOf(owlannotatedTarget_close) != -1) {
							owlannotatedTarget_value = line;
						} else {
							owlannotatedTarget_start = true;
						}
					}

				} else {
					if (owlannotatedTarget_start) {
						buf.append(" ").append(line);

						if (line.indexOf(owlannotatedTarget_close) != -1) {
							owlannotatedTarget_start = false;
							owlannotatedTarget_value = buf.toString();
						}

					} else {
						owlannotatedTarget_start = false;
						qualify_data = line;
						OWLAxiom owl_Axiom = new OWLAxiom(axiom_knt,
						                         label,
							                     extractCode(owlannotatedSource_value),
												 extractCode(owlannotatedProperty_value),
												 extractAnnotatedTarget(owlannotatedTarget_value),
												 extractQualifier(qualify_data),
												 extractQualifierValue(qualify_data));
						v.add(owl_Axiom);
						buf = new StringBuffer();
						owlannotatedTarget_start = false;
					}
				}
			}
			i++;
		}
		w.addAll(v);
		return w;
	}


	public Vector scanOwlTags() {
		int maxLevel = findMaxLevel();
		String parent = null;
		String child = null;
		Vector w = new Vector();
		int i = 0;

		String path = null;
		Vector paths = new Vector();
		String[] nodes = initializeNodes(maxLevel+1);

		int prevLevel = -1;
		int currLevel = -1;

		while (i < owl_vec.size()) {
			String line = (String) owl_vec.elementAt(i);
			int level = findLevel(line);
			if (level == 1) {
				nodes = initializeNodes(maxLevel);
				nodes[level-1] = getOpenTag(line);
				prevLevel = -1;
				currLevel = level;
			} else if (level > 1) {
				nodes[level-1] = getOpenTag(line);
				String path_str = getPath(nodes, currLevel);
				currLevel = level;
				prevLevel = currLevel;
				String tag = getOpenTag(line);
				nodes[level-1] = getOpenTag(line);
				path_str = getPath(nodes, currLevel);
				if (!w.contains(path_str)) {
					w.add(path_str);
				}
			}
			i++;
		}

		String path_str = getPath(nodes, currLevel);
		if (!w.contains(path_str)) {
			w.add(path_str);
		}

        w = new SortUtils().quickSort(w);
        return w;
	}

	public Vector fileterTagData(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			boolean retval = false;
			String t = (String) v.elementAt(i);
			for (int j=0; j<v.size(); j++) {
				String s = (String) v.elementAt(j);
				if (s.compareTo(t) != 0) {
					if (s.indexOf(t) != -1) {
						retval = true;
						break;
					}
				}
			}
			if (!retval) {
				w.add(t);
			}
		}
		return w;
	}

	public Vector fileterTagData(Vector v, String restriction) {
		Vector w = fileterTagData(v);
		Vector v2 = new Vector();
		for (int j=0; j<w.size(); j++) {
			String s = (String) w.elementAt(j);
			if (s.indexOf(restriction) != -1) {
				v2.add(s);
			}
		}
		return v2;
	}


	public void print_restriction_query(String path, int method_index) {
		Vector v = StringUtils.parseData(path, '|');
		int z_index = 0;
		System.out.println("\n");
		System.out.println("	public String construct_get_roles_" + method_index + "(String named_graph, String code, boolean outbound, boolean codeOnly) {");
		System.out.println("		String prefixes = getPrefixes();");
		System.out.println("		StringBuffer buf = new StringBuffer();");
		System.out.println("		buf.append(prefixes);");
		System.out.println("		if (codeOnly) {");
		System.out.println("			buf.append(\"SELECT distinct ?x_code ?p_code ?y_code \").append(\"\\n\");");
		System.out.println("		} else {");
		System.out.println("			buf.append(\"SELECT distinct ?x_label ?x_code ?p_label ?y_label ?y_code \").append(\"\\n\");");
		System.out.println("		}");
		System.out.println("		buf.append(\"{ \").append(\"\\n\");");

		System.out.println("		buf.append(\"    graph <\" + named_graph + \">\").append(\"\\n\");");
		System.out.println("		buf.append(\"    {\").append(\"\\n\");");
		System.out.println("		buf.append(\"	   {\").append(\"\\n\");");
		for (int i=0; i<v.size(); i++) {
			String node = (String) v.elementAt(i);
			if (node.compareTo("owl:Class") == 0) {

				if (z_index == 0) {
					System.out.println("		buf.append(\"		?x :NHC0 ?x_code .\").append(\"\\n\");");
					System.out.println("		buf.append(\"		?x rdfs:label ?x_label .\").append(\"\\n\");");
				} else {
					String z2 = "z" + z_index;
					//z_index++;
					System.out.println("		buf.append(\"		?" + z2 + " a owl:Class .\").append(\"\\n\");");
				}

			} else if (node.compareTo("rdfs:subClassOf") == 0) {
				if (z_index == 0) {
					z_index++;
					String z2 = "z" + z_index;
					System.out.println("		buf.append(\"		?x" + " rdfs:subClassOf ?" + z2 + " .\").append(\"\\n\");");
				} else {
					String z1 = "z" + z_index;
					z_index++;
					String z2 = "z" + z_index;
					z_index++;
					System.out.println("		buf.append(\"		?" + z1 + " rdfs:subClassOf ?" + z2 + " .\").append(\"\\n\");");
				}

			} else if (node.compareTo("owl:equivalentClass") == 0) {
				if (z_index == 0) {
					z_index++;
					String z2 = "z" + z_index;
					System.out.println("		buf.append(\"		?x" + " owl:equivalentClass ?" + z2 + " .\").append(\"\\n\");");
				} else {
					String z1 = "z" + z_index;
					z_index++;
					String z2 = "z" + z_index;
					z_index++;
					System.out.println("		buf.append(\"		?" + z1 + " owl:equivalentClass ?" + z2 + " .\").append(\"\\n\");");
				}


			} else if (node.compareTo("owl:intersectionOf") == 0) {
				String z1 = "z" + z_index;
				z_index++;
				String z2 = "z" + z_index;
				z_index++;
				String z3 = "z" + z_index;

				System.out.println("		buf.append(\"		?" + z1 + " owl:intersectionOf ?" + z2 + " .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + z2 + " rdf:rest*/rdf:first ?" + z3 + " .\").append(\"\\n\");");

			} else if (node.compareTo("owl:unionOf") == 0) {
				String z1 = "z" + z_index;
				z_index++;
				String z2 = "z" + z_index;
				z_index++;
				String z3 = "z" + z_index;

				System.out.println("		buf.append(\"		?" + z1 + " owl:unionOf ?" + z2 + " .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + z2 + " rdf:rest*/rdf:first ?" + z3 + " .\").append(\"\\n\");");

			} else if (node.compareTo("owl:Restriction") == 0) {

				String restriction_id = "z" + z_index;
				String prop_id = "p";
				String y_id = "y";
				System.out.println("		buf.append(\"		?" + restriction_id + " a owl:Restriction .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + restriction_id + " owl:onProperty ?" + prop_id + " .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + prop_id + " rdfs:label ?" + prop_id + "_label .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + prop_id + " :NHC0 ?" + prop_id + "_code .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + restriction_id + " owl:someValuesFrom ?" + y_id + " .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + y_id + " a owl:Class .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + y_id + " rdfs:label ?" + y_id + "_label .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + y_id + " :NHC0 ?" + y_id + "_code .\").append(\"\\n\");");
			}
   	    }
		System.out.println("		if (code != null && code.compareTo(\"null\") != 0 && outbound) {");
		System.out.println("		    buf.append(\"		?x :NHC0 \\" + "\"" + "\"+ code +\"" + "\\\"^^xsd:string\").append(\"\\n\");");
		System.out.println("		} else if (code != null && code.compareTo(\"null\") != 0 && !outbound) {");
		System.out.println("		    buf.append(\"		?y :NHC0 \\" + "\"" + "\"+ code +\"" + "\\\"^^xsd:string\").append(\"\\n\");");
		System.out.println("		}");

		System.out.println("		buf.append(\"	    }\").append(\"\\n\");");
		System.out.println("		buf.append(\"    }\").append(\"\\n\");");
		System.out.println("		buf.append(\"} \").append(\"\\n\");");
		System.out.println("		return buf.toString();");
		System.out.println("	}");
	}


	public void print_superclass_query(String path, int method_index) {
		Vector v = StringUtils.parseData(path, '|');
		int z_index = 0;
		System.out.println("\n");
		System.out.println("	public String construct_get_superclass_" + method_index + "(String named_graph, String code, boolean outbound, boolean codeOnly) {");
		System.out.println("		String prefixes = getPrefixes();");
		System.out.println("		StringBuffer buf = new StringBuffer();");
		System.out.println("		buf.append(prefixes);");
		System.out.println("		if (codeOnly) {");
		System.out.println("			buf.append(\"SELECT distinct ?y_code ?x_code \").append(\"\\n\");");
		System.out.println("		} else {");
		System.out.println("			buf.append(\"SELECT distinct ?y_label ?y_code ?x_label ?x_code \").append(\"\\n\");");
		System.out.println("		}");
		System.out.println("		buf.append(\"{ \").append(\"\\n\");");

		System.out.println("		buf.append(\"    graph <\" + named_graph + \">\").append(\"\\n\");");
		System.out.println("		buf.append(\"    {\").append(\"\\n\");");
		System.out.println("		buf.append(\"	   {\").append(\"\\n\");");
		for (int i=0; i<v.size(); i++) {
			String node = (String) v.elementAt(i);
			if (node.compareTo("owl:Class") == 0) {

				if (z_index == 0) {
					System.out.println("		buf.append(\"		?x :NHC0 ?x_code .\").append(\"\\n\");");
					System.out.println("		buf.append(\"		?x rdfs:label ?x_label .\").append(\"\\n\");");
				} else {
					String z2 = "z" + z_index;
					//z_index++;
					System.out.println("		buf.append(\"		?" + z2 + " a owl:Class .\").append(\"\\n\");");
				}

			} else if (node.compareTo("rdfs:subClassOf") == 0) {
				if (z_index == 0) {
					z_index++;
					String z2 = "z" + z_index;
					System.out.println("		buf.append(\"		?x" + " rdfs:subClassOf ?" + z2 + " .\").append(\"\\n\");");
				} else {
					String z1 = "z" + z_index;
					z_index++;
					String z2 = "z" + z_index;
					z_index++;
					System.out.println("		buf.append(\"		?" + z1 + " rdfs:subClassOf ?" + z2 + " .\").append(\"\\n\");");
				}

			} else if (node.compareTo("owl:equivalentClass") == 0) {
				if (z_index == 0) {
					z_index++;
					String z2 = "z" + z_index;
					System.out.println("		buf.append(\"		?x" + " owl:equivalentClass ?" + z2 + " .\").append(\"\\n\");");
				} else {
					String z1 = "z" + z_index;
					z_index++;
					String z2 = "z" + z_index;
					z_index++;
					System.out.println("		buf.append(\"		?" + z1 + " owl:equivalentClass ?" + z2 + " .\").append(\"\\n\");");
				}


			} else if (node.compareTo("owl:intersectionOf") == 0) {
				String z1 = "z" + z_index;
				z_index++;
				String z2 = "z" + z_index;
				z_index++;
				String z3 = "z" + z_index;

				System.out.println("		buf.append(\"		?" + z1 + " owl:intersectionOf ?" + z2 + " .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + z2 + " rdf:rest*/rdf:first ?" + z3 + " .\").append(\"\\n\");");

			} else if (node.compareTo("owl:unionOf") == 0) {
				String z1 = "z" + z_index;
				z_index++;
				String z2 = "z" + z_index;
				z_index++;
				String z3 = "z" + z_index;

				System.out.println("		buf.append(\"		?" + z1 + " owl:unionOf ?" + z2 + " .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + z2 + " rdf:rest*/rdf:first ?" + z3 + " .\").append(\"\\n\");");

			} else if (node.compareTo("owl:Restriction") == 0) {

				String restriction_id = "z" + z_index;
				String prop_id = "p";
				String y_id = "y";
				System.out.println("		buf.append(\"		?" + restriction_id + " a owl:Restriction .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + restriction_id + " owl:onProperty ?" + prop_id + " .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + prop_id + " rdfs:label ?" + prop_id + "_label .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + prop_id + " :NHC0 ?" + prop_id + "_code .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + restriction_id + " owl:someValuesFrom ?" + y_id + " .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + y_id + " a owl:Class .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + y_id + " rdfs:label ?" + y_id + "_label .\").append(\"\\n\");");
				System.out.println("		buf.append(\"		?" + y_id + " :NHC0 ?" + y_id + "_code .\").append(\"\\n\");");
			}
   	    }

  	    String z_final = "z" + z_index;

		System.out.println("		buf.append(\"		?" + z_final + " :NHC0 ?y_code .\").append(\"\\n\");");
		System.out.println("		buf.append(\"		?" + z_final + " rdfs:label ?y_label .\").append(\"\\n\");");

		System.out.println("		if (code != null && code.compareTo(\"null\") != 0 && outbound) {");
		System.out.println("		    buf.append(\"		?x :NHC0 \\" + "\"" + "\"+ code +\"" + "\\\"^^xsd:string\").append(\"\\n\");");
		System.out.println("		} else if (code != null && code.compareTo(\"null\") != 0 && !outbound) {");
		System.out.println("		    buf.append(\"		?" + z_final + " :NHC0 \\" + "\"" + "\"+ code +\"" + "\\\"^^xsd:string\").append(\"\\n\");");
		System.out.println("		}");

		System.out.println("		buf.append(\"	    }\").append(\"\\n\");");
		System.out.println("		buf.append(\"    }\").append(\"\\n\");");
		System.out.println("		buf.append(\"} \").append(\"\\n\");");
		System.out.println("		return buf.toString();");
		System.out.println("	}");
	}

    public Vector partitionClassData(Vector v) {
		String label = null;
		String owlannotatedSource_value = null;
		String owlannotatedProperty_value = null;
		String owlannotatedTarget_value = null;
		String qualify_data = null;

		String axiom_open_tag = "<owl:Axiom>";
		String axiom_close_tag = "</owl:Axiom>";

		Vector w = new Vector();
		Vector axiom_data = new Vector();
		int i = 0;
		boolean istart = false;
		String class_line = null;
		int axiom_knt = 0;

		StringBuffer buf = new StringBuffer();
		boolean owlannotatedTarget_start = false;
		while (i < v.size()) {
			String line = (String) v.elementAt(i);
	        if (line.indexOf(axiom_open_tag) != -1) {
				if (axiom_knt == 0) {
					w.add(axiom_data);
					axiom_data = new Vector();
				}
				istart = true;
				axiom_data.add(line);
				axiom_knt++;
	        } else if (line.indexOf(axiom_close_tag) != -1) {
				axiom_data.add(line);
				w.add(axiom_data);
				axiom_data = new Vector();
			} else if (istart) {
				axiom_data.add(line);
			}
			if (axiom_knt == 0) {
				axiom_data.add(line);
			}
			i++;
		}
		return w;
	}

	public void dumpClassData(Vector class_data) {
		Vector v = partitionClassData(class_data);
		Vector cls_data = (Vector) v.elementAt(0);
		Utils.dumpVector("class", cls_data);
		for (int i=1; i<v.size(); i++) {
			Vector axiom_data = (Vector) v.elementAt(i);
			Utils.dumpVector("axiom_" + i, axiom_data);
		}
	}


    public Vector extractOWLRestrictions(Vector class_vec) {
//    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C4910 -->
        Vector w = new Vector();
        boolean istart = false;
        boolean restriction_starg = false;
        String classId = null;
        OWLRestriction r = null;
        String onProperty = null;
        String someValueFrom = null;
        HashSet hset = new HashSet();
        for (int i=0; i<class_vec.size(); i++) {
			String t = (String) class_vec.elementAt(i);
			if (t.indexOf("<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#") != -1 && t.endsWith("-->")) {
				//System.out.println(t);

				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
				System.out.println("extractOWLRestrictions: " + classId);
				r = null;
				//istart = false;

				istart = true;

			}
			if (istart) {
				t = t.trim();
				if (t.indexOf("<owl:Restriction>") != -1) {
					restriction_starg = true;
					r = new OWLRestriction();
					r.setClassId(classId);
				} else if (r != null) {
					t = t.trim();
					if (t.startsWith("<owl:onProperty")) {
						//<owl:onProperty rdf:resource="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#R101"/>
						int n = t.lastIndexOf("#");
						t = t.substring(n, t.length());
						n = t.lastIndexOf("\"");
						onProperty = t.substring(1, n);
						r.setOnProperty(onProperty);
					}
					if (t.startsWith("<owl:someValuesFrom")) {
						// <owl:someValuesFrom rdf:resource="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C12382"/>
						int n = t.lastIndexOf("#");
						t = t.substring(n, t.length());
						n = t.lastIndexOf("\"");
						someValueFrom = t.substring(1, n);
						r.setSomeValuesFrom(someValueFrom);
						if (!hset.contains(r.toString())) {
							hset.add(r.toString());
							w.add(r);
						}
						r = null;
					}
				}
		    }
		}
		return w;
	}

/*
C4910|<A8 rdf:resource="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C165258"/>
C4910|<NHC0>C4910</NHC0>
*/
    public String parseProperty(String t) {
		t = t.trim();
		if (t.indexOf("rdf:resource") != -1) {
			int n = t.indexOf("rdf:resource");
			String propertyCode = t.substring(1, n-1);
		    int m = t.lastIndexOf("#");
			String s = t.substring(m, t.length());
			m = s.indexOf("\"");
			String target = s.substring(1, m);
			return propertyCode + "|" + target;
		} else {
			int n = t.indexOf(">");
			String propertyCode = t.substring(1, n);

			String end_tag = "</" + propertyCode + ">";
			if(!t.endsWith(end_tag)) {
				return("ERROR parsing " + t);
			}
			String s = t.substring(n, t.length());
			int m = s.lastIndexOf("<");
			String propertyValue = s.substring(1, m);
        	return propertyCode + "|" + propertyValue;
		}
	}


    public Vector extractProperties(Vector class_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;

        for (int i=0; i<class_vec.size(); i++) {
			String t = (String) class_vec.elementAt(i);
			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}
			if (t.indexOf("<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#") != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
				if (istart0) {
					istart = true;
				}
			}
			if (istart) {
				t = t.trim();
				if (t.startsWith("<") && t.indexOf("rdf:resource=") != -1 && t.indexOf("owl:") == -1 && t.indexOf("rdfs:subClassOf") == -1) {
					int n = t.indexOf(">");
                    if (n != -1) {
						String s = t.substring(1, n-1);
						w.add(classId + "|" + parseProperty(t));
					}
				} else if (t.startsWith("<") && t.indexOf("rdf:resource=") == -1 && t.indexOf("owl:") == -1 && t.indexOf("rdfs:subClassOf") == -1
				    && t.indexOf("rdf:Description") == -1 && t.indexOf("rdfs:subClassOf") == -1) {
					int n = t.indexOf(">");
                    if (n != -1) {
						String s = t.substring(1, n-1);
						w.add(classId + "|" + parseProperty(t));
					}
				}
		    }
		}
		return w;
	}


    public Vector extractSuperclasses(Vector class_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;

        for (int i=0; i<class_vec.size(); i++) {
			String t = (String) class_vec.elementAt(i);
			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}
			if (t.indexOf("<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#") != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
				if (istart0) {
					istart = true;
				}
			}
			//<rdfs:subClassOf rdf:resource="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C35844"/>
			if (istart) {
				t = t.trim();
				if (t.startsWith("<rdfs:subClassOf rdf:resource=") && t.endsWith("/>")) {
					int n = t.lastIndexOf("#");
				    t = t.substring(n, t.length());
					n = t.indexOf("\"");
					t = t.substring(1, n);
					w.add(classId + "|" + t);
				//<rdf:Description rdf:about="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C3161"/>
				} else if (t.startsWith("<rdf:Description rdf:about=") && t.endsWith("/>")) {
					int n = t.lastIndexOf("#");
				    t = t.substring(n, t.length());
					n = t.indexOf("\"");
					t = t.substring(1, n);
					w.add(classId + "|" + t);
				}
		    }
		}
		return w;
	}

    public Vector extractRDFSLabels(Vector class_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;

        for (int i=0; i<class_vec.size(); i++) {
			String t = (String) class_vec.elementAt(i);
			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}
			if (t.indexOf("<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#") != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
				if (istart0) {
					istart = true;
				}
			}
			//<rdfs:label>LEC-5 Standard Version - Unwanted Sex Experience: Happened</rdfs:label>
			if (istart) {
				t = t.trim();
				if (t.startsWith("<rdfs:label>") && t.endsWith("</rdfs:label>")) {
					int n = t.lastIndexOf("</rdfs:label>");
				    t = t.substring("<rdfs:label>".length(), n);
					w.add(classId + "|" + t);
				}
		    }
		}
		return w;
	}

	public static Vector hashSet2Vector(HashSet hset) {
		Vector w = new Vector();
		Iterator it = hset.iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			w.add(t);
		}
		return w;
	}

    public Vector getRootCodes() {
		Vector w = extractSuperclasses(this.owl_vec);
        HashSet child_codes = new HashSet();
        HashSet parent_codes = new HashSet();
        for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String child_code = (String) u.elementAt(0);
			String parent_code = (String) u.elementAt(1);
			child_codes.add(child_code);
			parent_codes.add(parent_code);
		}
		parent_codes.removeAll(child_codes);
		return hashSet2Vector(parent_codes);
	}

    public Vector extractAnnotationProperties(Vector owl_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;
        String code = null;
        String label = null;
        String p108 = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("// Annotation properties") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}
			if (t.indexOf("<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#") != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
            } else {
				t = t.trim();
				if (t.startsWith("<owl:AnnotationProperty rdf:about=")) {
                    istart = true;
				}
				if (istart) {
					if (t.startsWith("<NHC0>") && t.endsWith("</NHC0>")) {
						int n = t.lastIndexOf("</NHC0>");
						code = t.substring("<NHC0>".length(), n);
					}
					if (t.startsWith("<rdfs:label>") && t.endsWith("</rdfs:label>")) {
						int n = t.lastIndexOf("</rdfs:label>");
						label = t.substring("<rdfs:label>".length(), n);
					}
					if (t.startsWith("<P108>") && t.endsWith("</P108>")) {
						int n = t.lastIndexOf("</P108>");
						p108 = t.substring("<P108>".length(), n);
					}
				}
				if (t.compareTo("</owl:AnnotationProperty>") == 0 && code != null) {
					w.add(code + "|" + label + "|" + p108);
					code = null;
					label = null;
					p108 = null;
					istart = false;
				}

			}
		}
		return new SortUtils().quickSort(w);
	}

    public Vector extractAssociations(Vector class_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;

        for (int i=0; i<class_vec.size(); i++) {
			String t = (String) class_vec.elementAt(i);
			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}
			if (t.indexOf("<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#") != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
				if (istart0) {
					istart = true;
				}
			}
			if (istart) {
				String s = t.trim();
				if (s.indexOf("rdf:resource=") != -1 && s.startsWith("<A")) {
					int n = s.indexOf(" ");
					String a = s.substring(1, n);
					w.add(classId + "|" + a + "|" + extractCode(s));
				}
		    }
		}
		return w;
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		/*
        String owlfile = args[0];
		OWLScanner scanner = new OWLScanner(owlfile);
		int n = owlfile.lastIndexOf(".");
		String outputfile = owlfile.substring(0, n) + "_" + getToday() + ".owl";
		*/
		/*
		Vector w = scanner.scanAxioms();
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			OWLAxiom axiom = (OWLAxiom) w.elementAt(i);
			//v.add(axiom.toJson());
			v.add(axiom.toString());
		}
		Utils.saveToFile(outputfile, v);
		Vector synoym_vec = scanner.parseSynonymData(v);
		v = new Vector();
		for (int i=0; i<synoym_vec.size(); i++) {
			Synonym syn = (Synonym) synoym_vec.elementAt(i);
			v.add(syn.toJson());
		}
		String syn_file = "syn_" + owlfile.substring(0, n) + "_" + getToday() + ".owl";
		Utils.saveToFile(syn_file, v);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
		*/
        //Vector v = scanner.scanOwlTags();
        //v = scanner.fileterTagData(v, "owl:Restriction");
        //Utils.dumpVector("fileterTagData", v);
        //scanner.printPaths(v);

        Vector class_vec = Utils.readFile("ThesaurusInferred_forTS.owl");
        Vector w = new OWLScanner().extractProperties(class_vec);
        for (int i=0; i<w.size(); i++) {
			String p = (String) w.elementAt(i);
			System.out.println(p);
		}



    }
}


