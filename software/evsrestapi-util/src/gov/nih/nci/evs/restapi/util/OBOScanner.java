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


public class OBOScanner {
    String owlfile = null;
    Vector owl_vec = null;
    static String NAMESPACE = "http://purl.obolibrary.org/obo";
    static String NAMESPACE_TARGET = "<!-- " + NAMESPACE;
    static String OWL_CLS_TARGET = NAMESPACE_TARGET;

    static String OWL_ANNOTATION_PROPERTY_TARGET = NAMESPACE_TARGET + "A";

	static String open_tag = "<owl:Axiom>";
	static String close_tag = "</owl:Axiom>";
	static String owlannotatedSource = "owl:annotatedSource";
	static String owlannotatedProperty = "<owl:annotatedProperty";
	static String owlannotatedTarget_open = "owl:annotatedTarget";
	static String owlannotatedTarget_close = "</owl:annotatedTarget>";
	//static String pt_code = "P108";
	//static String pt_tag_open = "<P108>";

	//static String SUBSET_MEMBERSHIP = "<A8 rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#CCODE\"/>";

	public HashMap code2LabelMap = null;

    public OBOScanner() {

    }

    public static void set_NAMESPACE(String namespace) {
		NAMESPACE = namespace;
		NAMESPACE_TARGET = "<!-- " + NAMESPACE;
	}

    public OBOScanner(String owlfile) {
        this.owlfile = owlfile;
        this.owl_vec = readFile(owlfile);
        Vector label_data = extractRDFSLabels(owl_vec);
        code2LabelMap = new HashMap();
        for (int i=0; i<label_data.size(); i++) {
			String t = (String) label_data.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String code = (String) u.elementAt(0);
			String label = (String) u.elementAt(1);
			code2LabelMap.put(code, label);
		}
    }

    public Vector get_owl_vec() {
		return this.owl_vec;
	}

    public HashMap getCode2LabelMap() {
		return this.code2LabelMap;
	}

    public String getLabel(String code) {
		return (String) code2LabelMap.get(code);
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
			if (line.indexOf("enum") == -1) {
				w.add(line);
			}
		}
		return w;
	}

	public Vector getOWLClassDataByCode(String code) {
		Vector w = new Vector();
		String target = NAMESPACE_TARGET + code + " -->";
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
		Vector targets = new Vector();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			targets.add(NAMESPACE_TARGET + code + " -->");
		}
		Vector w = new Vector();
		boolean istart = false;
		for (int i=0; i<owl_vec.size(); i++) {
			String line = (String) owl_vec.elementAt(i);

			if (line.indexOf("General axioms") != -1) break;
			while (!line.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				line = line + " " + nextLine;
			}


			String line_trimmed = line.trim();
			if (targets.contains(line_trimmed)) {
				istart = true;
			} else if (istart && line.indexOf(OWL_CLS_TARGET) != -1) {
                istart = false;
			} else {
				if (line_trimmed.startsWith(OWL_CLS_TARGET) && !targets.contains(line_trimmed)) {
					istart = false;
				}
			}
			if (istart) {
				w.add(line);
			}
		}
		return w;
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
	    pw.println("<!-- owl:disjointWith modified by the OBOScanner (version 1.0) on " + getToday() + " -->");
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
			if (line.indexOf("enum") == -1) {
				class_knt++;
			} else if (line.indexOf(axiom_open_tag) != -1) {
				axiom_knt++;
			}
/*
			if (line.indexOf(pt_tag_open) != -1) {
				label = extractQualifierValue(line);
			}
*/
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
						v.add(owl_Axiom.toString());
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


    public Vector extractOWLRestrictions(Vector owl_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean restriction_start = false;
        String classId = null;
        OWLRestriction r = null;
        String onProperty = null;
        String someValueFrom = null;
        HashSet hset = new HashSet();
        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}

			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {

				int n = t.lastIndexOf("#");
				if (n == -1) {
					n = t.lastIndexOf("/");
				}
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);

/*
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
*/

				r = null;
				//istart = false;
				istart = true;

			}
			if (istart) {
				t = t.trim();
				if (t.indexOf("<owl:Restriction>") != -1) {
					restriction_start = true;
					r = new OWLRestriction();
					r.setClassId(classId);
				} else if (r != null) {
					t = t.trim();
					if (t.startsWith("<owl:onProperty")) {
						//<owl:onProperty rdf:resource="http://purl.obolibrary.org/obo/RO_0000053"/>
						int n = t.indexOf(NAMESPACE);
						onProperty = t.substring(n + NAMESPACE.length() + 1, t.length()-3);
						r.setOnProperty(onProperty);
					}
					//<owl:someValuesFrom rdf:resource="http://purl.obolibrary.org/obo/PATO_0002124"/>

/*
<owl:someValuesFrom>
	<owl:Class>
		<owl:complementOf rdf:resource="http://purl.obolibrary.org/obo/NCBITaxon_50557"/>
	</owl:Class>
</owl:someValuesFrom>
*/
                    if (t.startsWith("<owl:someValuesFrom>")) {
						i++;
						i++;
						t = (String) owl_vec.elementAt(i);
                        t = t.trim();

						int n = t.indexOf(NAMESPACE);
						//System.out.println(t);
						//System.out.println(n);
						someValueFrom = t.substring(n + NAMESPACE.length() + 1, t.length()-3);
						r.setSomeValuesFrom(someValueFrom);

						if (!hset.contains(r.toString())) {
							hset.add(r.toString());
							w.add(r.toString());
						} else {
							//System.out.println("\tWARNING: Duplicate " + r.toString());
						}
						r = null;

					} else if (t.startsWith("<owl:someValuesFrom")) {
						int n = t.indexOf(NAMESPACE);
						//System.out.println(t);
						//System.out.println(n);
						someValueFrom = t.substring(n + NAMESPACE.length() + 1, t.length()-3);
						r.setSomeValuesFrom(someValueFrom);

						if (!hset.contains(r.toString())) {
							hset.add(r.toString());
							w.add(r.toString());
						} else {
							//System.out.println("\tWARNING: Duplicate " + r.toString());
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


    public Vector extractProperties(Vector owl_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;
        boolean switch_off = false;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}

			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}

			if (t.indexOf("<owl:Axiom>") != -1) {
				switch_off = true;
			}
			if (t.indexOf("</owl:Axiom>") != -1) {
				switch_off = false;
			}

			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				if (n == -1) {
					n = t.lastIndexOf("/");
				}

				if (n != -1) {
					t = t.substring(n, t.length());
					n = t.lastIndexOf(" ");
					classId = t.substring(1, n);
					if (istart0) {
						istart = true;
					}
				}
			}
			if (istart) {
				t = t.trim();
				if (t.startsWith("<") && t.indexOf("rdf:resource=") != -1 && t.indexOf("owl:") == -1 && t.indexOf("rdfs:subClassOf") == -1 && t.indexOf("rdfs:isDefinedBy") == -1) {
					int n = t.indexOf(">");
                    if (n != -1) {
						if (!switch_off) {
							w.add(classId + "|" + parseProperty(t));
					    }
					}

				} else if (t.startsWith("<") && t.indexOf("rdf:resource=") == -1 && t.indexOf("owl:") == -1 && t.indexOf("rdfs:subClassOf") == -1
				    && t.indexOf("rdf:Description") == -1 && t.indexOf("rdfs:subClassOf") == -1) {
					int n = t.indexOf(">");
                    if (n != -1) {
						//String s = t.substring(1, n-1);
						if (!switch_off) {
						    w.add(classId + "|" + parseProperty(t));
						}
					}
				}
		    }
		}
		return w;
	}

    public Vector extractProperties(Vector owl_vec, String propertyCode) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;
        boolean switch_off = false;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}

			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}

			if (t.indexOf("<owl:Axiom>") != -1) {
				switch_off = true;
			}
			if (t.indexOf("</owl:Axiom>") != -1) {
				switch_off = false;
			}

			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
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
						//String s = t.substring(1, n-1);
						if (!switch_off) {
							if (propertyCode != null) {
								String s = parseProperty(t);
								if (s.startsWith(propertyCode+ "|")) {
									w.add(classId + "|" + s);
								}
							} else {
								w.add(classId + "|" + parseProperty(t));
							}
					    }
					}


				} else if (t.startsWith("<") && t.indexOf("rdf:resource=") == -1 && t.indexOf("owl:") == -1 && t.indexOf("rdfs:subClassOf") == -1
				    && t.indexOf("rdf:Description") == -1 && t.indexOf("rdfs:subClassOf") == -1) {
					int n = t.indexOf(">");
                    if (n != -1) {
						if (!switch_off) {
							if (propertyCode != null) {
								String s = parseProperty(t);
								if (s.startsWith(propertyCode+ "|")) {
									w.add(classId + "|" + s);
								}
							} else {
								w.add(classId + "|" + parseProperty(t));
							}
						}
					}
				}
		    }
		}
		return w;
	}

    public Vector extractSuperclasses(Vector owl_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}

			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				if (n == -1) {
					n = t.lastIndexOf("/");
				}

				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
				if (istart0) {
					istart = true;
				}
			}
			if (istart) {
				t = t.trim();
				if (t.startsWith("<rdfs:subClassOf rdf:resource=") && t.endsWith("/>")) {
//<rdfs:subClassOf rdf:resource="http://purl.obolibrary.org/obo/SO_0001217"/>
					int n = t.indexOf(NAMESPACE);
					t = t.substring(n + NAMESPACE.length() + 1, t.length()-3);
					w.add(classId + "|" + t);
				} else if (t.startsWith("<rdf:Description rdf:about=") && t.endsWith("/>")) {
					int n = t.indexOf(NAMESPACE);
					t = t.substring(n + NAMESPACE.length() + 1, t.length()-3);
					w.add(classId + "|" + t);
				}
		    }
		}
		return w;
	}

    public Vector extractRDFSLabels(Vector owl_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;
        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
		    if (t.indexOf("// Annotations") != -1) {
				break;

			} else {
				if (t.indexOf("// Classes") != -1) {
					istart0 = true;
				}

				if (t.indexOf("</rdf:RDF>") != -1) {
					break;
				}
				if (t.indexOf("<!-- http://") != -1 && t.endsWith("-->")) {
					int n = t.lastIndexOf("#");
					if (n == -1) {
						n = t.lastIndexOf("/");
					}
					t = t.substring(n, t.length());
					n = t.lastIndexOf(" ");
					classId = t.substring(1, n);
					if (istart0) {
						istart = true;
					}
				}
				if (istart) {
					t = t.trim();

					if (t.startsWith("<rdfs:label>") && t.endsWith("</rdfs:label>")) {
						int n = t.lastIndexOf("</rdfs:label>");
						t = t.substring("<rdfs:label>".length(), n);
						w.add(classId + "|" + t);
					} else if (t.startsWith("<rdfs:label") && t.endsWith("</rdfs:label>")) {
						int n = t.lastIndexOf("</rdfs:label>");
						t = t.substring(0, n);
						n = t.lastIndexOf(">");
						t = t.substring(n+1, t.length());
						w.add(classId + "|" + t);
					}
				}
			}
		}
		return w;
	}

    public String extractRDFSLabel(Vector owl_vec) {
        Vector w = new Vector();
        String classId = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
			t = t.trim();
			if (t.startsWith("<rdfs:label>") && t.endsWith("</rdfs:label>")) {
				int n = t.lastIndexOf("</rdfs:label>");
				t = t.substring("<rdfs:label>".length(), n);
				return t;
			}
		}
		return null;
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

    public Vector extractOWLDisjointWith(Vector owl_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);

				System.out.println(classId);
				//if (istart0) {
					istart = true;
				//}
			}
			if (istart) {
				t = t.trim();
				if (t.startsWith("<") && t.indexOf("owl:disjointWith") != -1) {
					int n = t.lastIndexOf("#");
					t = t.substring(n, t.length());
					n = t.lastIndexOf("\"");
					String id = t.substring(1, n);
					w.add(classId + "|" + id);
				}
		    }
		}
		return w;
	}

    public Vector extractSuperclasses() {
		return extractSuperclasses(owl_vec);
	}

    public Vector extractHierarchicalRelationships() {
		Vector w = extractHierarchicalRelationships(this.owl_vec);
		return new SortUtils().quickSort(w);
	}

    public Vector extractHierarchicalRelationships(Vector owl_vec) {
		Vector v = extractSuperclasses(owl_vec);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String code_1 = (String) u.elementAt(0);
			String code_2 = (String) u.elementAt(1);
			String label_1 = getLabel(code_1);
			String label_2 = getLabel(code_2);
			w.add(label_2 + "|" + code_2 + "|" + label_1 + "|" + code_1);
		}
		return new SortUtils().quickSort(w);
	}

    public Vector extractAllDisjointClasses(Vector owl_vec) {
        Vector w = new Vector();
        boolean istart = false;
        String classId = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
			if (t.indexOf("<owl:AllDisjointClasses") != -1) {
				istart = true;
			}

			if (istart) {
				w.add(t);
			}

		    if (istart && t.indexOf("</owl:AllDisjointClasses>") != -1) {
				break;
			}
		}
		return w;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String extractClassId(String line) {
        String t = line;
        String classId = null;
		if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
			int n = t.lastIndexOf("#");
			t = t.substring(n, t.length());
			n = t.lastIndexOf(" ");
			classId = t.substring(1, n);
		}
		return classId;
	}


    public String getOWLClassHashCode(Vector v) {
		String classId = null;
		String label = null;
		int hashcode = 0;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			String line = t.trim();
			if (line.length() > 0) {
				hashcode = hashcode + line.hashCode();
			}
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				classId = extractClassId(t);
			}
			String t3 = t.trim();
			if (t3.startsWith("<rdfs:label>") && t3.endsWith("</rdfs:label>")) {
				int n = t3.lastIndexOf("</rdfs:label>");
				label = t3.substring("<rdfs:label>".length(), n);
		    }
		}
		return label + "|" + classId + "|" + hashcode;
	}

    public Vector getAllOWLClassHashCode() {
        return getAllOWLClassHashCode(this.owl_vec);
    }

    public Vector getAllOWLClassHashCode(Vector owl_vec) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;
        Vector v = new Vector();
        String label = null;

        boolean startPrint = false;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
			String t_save = t;
			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				if (istart0 && v.size() > 0) {
					String hc = getOWLClassHashCode(v);
					//System.out.println(hc);
					w.add(hc);
					v = new Vector();
				}
				if (istart0) {
					istart = true;
				}
			}
			if (istart) {
				v.add(t);
		    }
		}
		if (v.size() > 0) {
			String hc = getOWLClassHashCode(v);
			//System.out.println(hc);
			w.add(hc);
		}
		return w;//new SortUtils().quickSort(w);
	}

	public void clear() {
		this.code2LabelMap.clear();
		this.owl_vec.clear();
	}

    public Vector getOWLClassDataByCode(Vector owl_vec, Vector codes) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        String classId = null;
        Vector v = new Vector();
        String label = null;

        boolean startPrint = false;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
			String t_save = t;
			if (t.indexOf("// Classes") != -1) {
				istart0 = true;
			}
		    if (t.indexOf("</rdf:RDF>") != -1) {
				break;
			}
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				if (istart0 && v.size() > 0) {
					if (classId != null && codes.contains(classId)) {
						w.addAll(v);
					}

					classId = extractClassId(t);
					v = new Vector();
				}
				if (istart0) {
					istart = true;
				}
			}
			if (istart) {
				v.add(t);
		    }
		}
		if (v.size() > 0) {
			if (classId != null && codes.contains(classId)) {
				w.addAll(v);
			}
		}
		return w;
	}

    public HashMap getOWLClassId2DataHashMap(Vector owl_vec) {
		int knt = 0;
		HashMap hmap = new HashMap();
        String classId = null;
        Vector v = new Vector();
        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				knt++;
				if (classId != null) {
					hmap.put(classId, v);
				}
				classId = extractClassId(t);
				v = new Vector();
				v.add(t);
			} else if (classId != null) {
				v.add(t);
			}
		}
		hmap.put(classId, v);
		System.out.println("knt: " + knt);
		return hmap;
	}

    public Vector extract_properties(Vector owl_vec) {
        int m = 0;
        Vector w = new Vector();
        String classId = null;
        boolean start = false;
        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
		    if (t.indexOf("// Annotations") != -1) {
				break;
			}


			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				start = true;
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
			}

			t = t.trim();
			if (t.startsWith("<owl:Class")) {
				m = m + 1;
			} else if (t.startsWith("</owl:Class")) {
				m = m - 1;
				if (m == 0) break;
			}

			if (t.startsWith("<") && t.indexOf("rdf:resource=") != -1 && t.indexOf("owl:") == -1 && t.indexOf("rdfs:subClassOf") == -1) {
				int n = t.indexOf(">");
				if (n != -1) {
					w.add(classId + "|" + parseProperty(t));
				}
			} else if (t.startsWith("<") && t.indexOf("rdf:resource=") == -1 && t.indexOf("owl:") == -1 && t.indexOf("rdfs:subClassOf") == -1
				&& t.indexOf("rdf:Description") == -1 && t.indexOf("rdfs:subClassOf") == -1) {
				int n = t.indexOf(">");
				if (n != -1) {
					w.add(classId + "|" + parseProperty(t));
				}
			}
		}
		return w;
	}

    public String get_owl_class_hashcode(Vector v) {
		String classId = null;
		int hashcode = 0;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			String line = t.trim();
			if (line.length() > 0) {
				hashcode = hashcode + line.hashCode();
			}
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				classId = extractClassId(t);
			}
		}
		return classId + "|" + hashcode;
	}

    public Vector extract_superclasses(Vector owl_vec) {
        Vector w = new Vector();
        String classId = null;
        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
			}
			//<rdfs:subClassOf rdf:resource="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C35844"/>
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
		return w;
	}

    public static void compareVector(Vector vec1, Vector vec2) {
		HashSet hset1 = new HashSet();
		for (int i=0; i<vec1.size(); i++) {
			String t = (String) vec1.elementAt(i);
			hset1.add(t);
		}
		HashSet hset2 = new HashSet();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			hset2.add(t);
		}
		Vector v1_minus_v2 = new Vector();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			if (!hset1.contains(t)) {
				v1_minus_v2.add(t);
			}
		}
		Utils.dumpVector("Deleted", v1_minus_v2);

		Vector v2_minus_v1 = new Vector();
		for (int i=0; i<vec1.size(); i++) {
			String t = (String) vec1.elementAt(i);
			if (!hset2.contains(t)) {
				v2_minus_v1.add(t);
			}
		}
		Utils.dumpVector("Added", v2_minus_v1);
	}

    public Vector extract_owlrestrictions(Vector owl_vec) {
        Vector w = new Vector();
        String classId = null;
        boolean restriction_start = false;
        OWLRestriction r = null;
        String onProperty = null;
        String someValueFrom = null;
        HashSet hset = new HashSet();
        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
				r = null;
			}
			t = t.trim();
			if (t.indexOf("<owl:Restriction>") != -1) {
				restriction_start = true;
				r = new OWLRestriction();
				r.setClassId(classId);
			} else if (r != null) {
				t = t.trim();
				if (t.startsWith("<owl:onProperty")) {
					int n = t.lastIndexOf("#");
					t = t.substring(n, t.length());
					n = t.lastIndexOf("\"");
					onProperty = t.substring(1, n);
					r.setOnProperty(onProperty);
				}
				if (t.startsWith("<owl:someValuesFrom")) {
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
		return w;
	}

    public Vector extract_axioms(Vector data_vec) {
		String label = null;
		String owlannotatedSource_value = null;
		String owlannotatedProperty_value = null;
		String owlannotatedTarget_value = null;
		String qualify_data = null;

		String axiom_open_tag = "<owl:Axiom>";

		Vector w = new Vector();
		int i = 0;
		String target = open_tag;
		Vector v = new Vector();
		String class_line = null;

		StringBuffer buf = new StringBuffer();
		boolean owlannotatedTarget_start = false;

		int class_knt = 0;
		int axiom_knt = 0;
		boolean istart = false;

		while (i < data_vec.size()) {
			String line = (String) data_vec.elementAt(i);

		    if (line.indexOf("// Annotations") != -1) {
				break;
			}

			if (line.indexOf("enum") == -1) {
				class_knt++;
			} else if (line.indexOf(axiom_open_tag) != -1) {
				axiom_knt++;
			}
/*
			if (line.indexOf(pt_tag_open) != -1) {
				label = extractQualifierValue(line);
			}
*/
			line = line.trim();
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

    public Vector extract_associations() {
		return extract_associations(this.owl_vec);
	}


    public Vector extract_associations(Vector owl_vec) {
        Vector w = new Vector();
        String classId = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
			}
			String s = t.trim();
			if (s.indexOf("rdf:resource=") != -1 && s.startsWith("<A")) {
				int n = s.indexOf(" ");
				String a = s.substring(1, n);
				w.add(classId + "|" + a + "|" + extractCode(s));
			}
		}
		return w;
	}

    public Vector axioms2Strings(Vector v) {
		Vector u = new Vector();
		for (int i=0; i<v.size(); i++) {
			Object obj = v.elementAt(i);
			if (obj instanceof Synonym) {
				Synonym syn = (Synonym) obj;
				u.add(syn.toString());
			} else if (obj instanceof Definition) {
				Definition def = (Definition) obj;
				u.add(def.toString());
			} else if (obj instanceof AltDefinition) {
				AltDefinition def = (AltDefinition) obj;
				u.add(def.toString());
			} else if (obj instanceof GoAnnotation) {
				GoAnnotation go = (GoAnnotation) obj;
				u.add(go.toString());
			} else if (obj instanceof MapToEntry) {
				MapToEntry entry = (MapToEntry) obj;
				u.add(entry.toString());
			}
		}
		return u;
	}

	public static Vector removeObjectValuedProperties(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String property = (String) u.elementAt(1);
			if (!property.startsWith("A")) {
				w.add(t);
			}
		}
		return w;
	}

    public Vector extractEnum(Vector owl_vec, String type) {
        Vector w = new Vector();
        boolean istart = false;
        String classId = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
		    if (t.indexOf("// Annotations") != -1) {
				break;
			}


			if (t.indexOf(NAMESPACE_TARGET + type + "-enum -->") != -1) {
				istart = true;
			}
			if (istart && t.indexOf("</rdfs:Datatype>") != -1) {
				istart = false;
				break;
			}
			if (istart && t.indexOf("<rdf:first>") != -1) {
				t = t.trim();
				int n = t.lastIndexOf("</rdf:first>");
				t = t.substring("<rdf:first>".length(), n);
				w.add(t);
			}
		}
		return new SortUtils().quickSort(w);
	}

    public Vector extractDeprecatedObjects(Vector owl_vec) {
        Vector w = new Vector();
		String classId = null;
		for (int i=0; i<owl_vec.size(); i++) {
			String line = (String) owl_vec.elementAt(i);

			if (line.indexOf("General axioms") != -1) break;
			while (!line.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				line = line + " " + nextLine;
			}

		    if (line.indexOf("// Annotations") != -1) {
				break;
			}

			line = line.trim();
			if (line.endsWith(" -->")) {
			    int n = line.lastIndexOf("#");
				classId = line.substring(n+1, line.length()-4);
			} else {
				if (line.indexOf("<owl:deprecated") != -1 && line.indexOf(">true<") != -1) {
					if (classId != null) {
						w.add(classId);
					}
					classId = null;
				}
			}
		}
		return w;
	}

    public Vector filterAxiomData(Vector axiom_data, String prop_code) {
		Vector w = new Vector();
		for (int i=0; i<axiom_data.size(); i++) {
			String t = (String) axiom_data.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
		    String propertyCode = (String) u.elementAt(3); // e.g., P90
		    if (propertyCode.compareTo(prop_code) == 0) {
				w.add(t);
			}
		}
		return w;
	}

	public String extractVersion() {
		return extractVersion(this.owl_vec);
	}

	public String extractVersion(Vector owl_vec) {
		String version = null;
		String tag = "<owl:versionInfo>";
		for (int i=0; i<owl_vec.size(); i++) {
			String line = (String) owl_vec.elementAt(i);
			version = extractTagValue(line, tag);
			if (version != null) {
				break;
			}
		}
		return version;
	}

	public String extractTagValue(String line, String tag) {
		String t = line;
		t = t.trim();
		int n = t.indexOf(tag);
		if (n == -1) return null;
		t = t.substring(n+tag.length(), t.length());
		n = t.indexOf("<");
		if (n == -1) return null;
		t = t.substring(0, n);
		t = t.trim();
		if (t.startsWith(">")) {
			return t.substring(1, t.length());
		}
		return t;
	}

    public boolean isNull(String str) {
		if (str == null) return true;
		if (str.compareTo("null") == 0) return true;
		return false;
	}

    public Vector extractAxiomData() {
		String open_tag = "<owl:Axiom>";
		String close_tag = "</owl:Axiom>";
        Vector w = new Vector();
        Vector w1 = new Vector();
        boolean istart = false;
        StringBuffer qualifier_buf = new StringBuffer();
        String source = null;
        String property = null;
        String target = null;
        String qualifiers = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);

			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}

			t = t.trim();

		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

			if (!istart && t.indexOf(open_tag) != -1) {
				istart = true;

				w1 = new Vector();
				qualifier_buf = new StringBuffer();
				source = null;
				property = null;
				target = null;

			}

			if (istart && t.indexOf(close_tag) != -1) {

				qualifiers = "";
				if (source != null) {
					if (qualifier_buf != null) {
						qualifiers = qualifier_buf.toString();
						qualifiers = qualifiers.substring(0, qualifiers.length()-1);
					}
					w.add(source + "|" + property + "|" + target + "|" + qualifiers);
				}


				if (w1.size() > 0) {
					//w.addAll(w1);
				}
				istart = false;
				w1 = null;
			}

			if (istart) {
				//owl:annotatedSource
				if (t.indexOf("owl:annotatedSource") != -1) {
					w1.add(t);
					source = extract_rdf_resource(t);
					w1.add("SOURCE: " + source);
				} else if (t.indexOf("owl:annotatedProperty") != -1) {
					w1.add(t);
					w1.add("PROPERTY: " + extract_rdf_resource(t));
					property = extract_rdf_resource(t);
				} else if (t.indexOf("owl:annotatedTarget") != -1) {
					target = extract_tag_value(t, "owl:annotatedTarget");
					w1.add("TARGET: " + target);
					w1.add(t);

				} else if (t.indexOf(open_tag) == -1) {
					w1.add(t);
					//<oboInOwl:hasDbXref rdf:resource="http://www.ncbi.nlm.nih.gov/pubmed/20973947"/>
					if (t.indexOf("rdf:resource") != -1) {
						int m = t.indexOf(" ");
						if (m != -1) {
							//System.out.println(m);
							//w1.add(m);
							String qualifier_name = t.substring(1, m);
							String qualifier_value = extract_rdf_resource(t);
							String qualifier = qualifier_name + "$" + qualifier_value;
							w1.add("QUALIFIER: " +qualifier);
							qualifier_buf.append(qualifier).append("|");
						} else {
							w1.add("QUALIFIER: " +t);
						}
					} else if (t.indexOf("rdf:datatype") != -1) {
						int m = t.indexOf(" ");
						if (m != -1) {
							String qualifier_name = t.substring(1, m);
							String qualifier_value = extract_rdf_datatype(t);
							String qualifier = qualifier_name + "$" + qualifier_value;
							w1.add("QUALIFIER: " +qualifier);
							qualifier_buf.append(qualifier).append("|");
						} else {
							w1.add("QUALIFIER: " +t);
						}

					} else {
						int m = t.indexOf(">");
						String tag = t.substring(1, m);
						String value = extract_tag_value(t, tag);
						if (value != null) {
							if (value.startsWith(">")) {
								value = value.substring(1, value.length());
							}
							w1.add("QUALIFIER: " + tag + "$" + value);
							qualifier_buf.append(tag + "$" + value).append("|");
						} else {
							w1.add("QUALIFIER: " +t);
						}
					}
				}
			}
		}


		if (source != null) {
			if (qualifier_buf != null) {
				qualifiers = qualifier_buf.toString();
				qualifiers = qualifiers.substring(0, qualifiers.length()-1);
			}
			w.add(source + "|" + property + "|" + target + "|" + qualifiers);
		}

        return w;
	}

	public static String extract_tag_value(String line, String tag) {
		String t0 = line;
		String t = t0.trim();
		int n = t.indexOf(tag);
		if (n == -1) return null;
		t = t.substring(n+tag.length(), t.length());
		n = t.indexOf("<");
		if (n == -1) return null;
		t = t.substring(1, n);
		t = t.trim();
		return t;
	}

	public String extract_rdf_datatype(String line) {
        int n1 = line.indexOf("\">");
        int n2 = line.lastIndexOf("<");
        return line.substring(n1+2, n2);
	}

	public String extract_rdf_resource(String line) {
        int n = line.indexOf("rdf:resource");
        if (n == -1) return null;
        return line.substring(n+"rdf:resource".length()+2, line.length()-3);
	}

    public static Vector split(String t) {
		return StringUtils.parseData(t, '|');
	}

    public static Vector split(String t, char ch) {
		return StringUtils.parseData(t, ch);
	}

    public Vector extractPropertyData(String prop_code) {
        Vector w = new Vector();
        boolean istart = false;
        String classId = null;
        String prop_value = null;
        w = new Vector();
        StringBuffer buf = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);

			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

			t = t.trim();
			if (t.indexOf("// Classes") != -1) {
				istart = true;
			}

			if (istart) {
			    if (t.indexOf("<" + prop_code + ">") != -1) {
					while (t.indexOf("</" + prop_code + ">") == -1) {
						i++;
						String s = (String) owl_vec.elementAt(i);
						t = t.substring(0, t.length()-1) + s;
					}
					try {
						String retstr = parseProperty(t);
						Vector u = split(retstr);
						prop_value = (String) u.elementAt(1);
						buf.append("|" + prop_value);
					} catch (Exception ex) {
						System.out.println("Error parsing: " + t);
					}

				} else if (t.startsWith("<owl:Class ")) {

					if (buf != null){
						String str = buf.toString();
						Vector u = split(str);
						if (u.size() > 1) {
							if (!w.contains(str)) w.add(str);
						}
					}

					buf = new StringBuffer();
					int m = t.lastIndexOf("#");
					String value = t.substring(m+1, t.length()-2);
					classId = value;
					buf.append(classId);
				}
			}
        }
        return w;
    }


    public Vector extractNonRetiredConcepts(Vector owl_vec, HashSet retiredConcepts) {
        Vector w = new Vector();
        boolean istart = false;
        boolean istart0 = false;
        boolean toSave = true;
        String classId = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
			String t0 = t;
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				int n = t.lastIndexOf("#");
				t = t.substring(n, t.length());
				n = t.lastIndexOf(" ");
				classId = t.substring(1, n);
				if (retiredConcepts.contains(classId)) {
					toSave = false;
				} else {
					toSave = true;
				}
			}

			if (toSave) {
				w.add(t0);
			}
		}
		return w;
	}

	// <owl:AnnotationProperty rdf:about="http://purl.obolibrary.org/obo/BFO_0000179"/>
    public Vector extractAnnotationProperties() {
        Vector w = new Vector();
        boolean istart = false;
        String classId = null;
        w = new Vector();

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);

			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}

		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

			t = t.trim();
			if (t.startsWith("<owl:AnnotationProperty")) {
				int n1 = t.indexOf("\"");
				int n2 = t.lastIndexOf("\"");
				String s = t.substring(n1+1, n2);
				w.add(s);
			}
		}
		return w;
	}

	public Vector prefixOboInOwlId(String id, Vector v) {
		Vector w = new Vector();
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			w.add(id + "|" + parse_rdf_resource(t));
		}
		return w;
	}

	//<terms:contributor rdf:resource="https://orcid.org/0000-0002-6601-2165"/>
	public String parse_rdf_resource(String line) {
		int n = line.indexOf(" ");
		String key = line.substring(1, n);
		String value = extract_rdf_resource(line);
		return key + "|" + value;
	}

	public Vector prefixOboInOwlId(String id, Vector v, boolean parserdfresource) {
		Vector w = new Vector();
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (parserdfresource) {
				w.add(id + "|" + parse_rdf_resource(t));
			} else {
				w.add(id + "|" + xml2Delimited(t));
		    }
		}
		return w;
	}

    public Vector extractProperties() {
        Vector w = new Vector();
        Vector w1 = new Vector();
        boolean istart = false;
        String id = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);

			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}

			String t0 = t;
			t = t.trim();

		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

		    if (t.indexOf("<owl:Axiom>") != -1) {
				istart = false;
			}

			if (t.indexOf("<oboInOwl:id>") != -1) {
				id = extractTagValue(t, "<oboInOwl:id>");
			}

			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				istart = true;
				w1 = new Vector();
			} else if (t0.compareTo("    </owl:Class>") == 0) {
				if (w1 != null && w1.size() > 0) {
					w.addAll(prefixOboInOwlId(id, w1, false));
					//w.addAll(w1);
				}
				istart = false;
				w1 = null;
			}

			if (istart) {
				if (t.indexOf("rdf:about") == -1 && t.indexOf("rdf:resource") == -1 && t.indexOf("rdfs:subClassOf") == -1 && t.indexOf("owl:onProperty") == -1 && t.indexOf("owl:someValuesFrom") == -1) {
					int n = t.indexOf(">");
					if (n != -1 && n != t.length()-1) {
						w1.add(t);
					}
				}
			}
		}
		if (w1 != null && w1.size() > 0) {
			w.addAll(prefixOboInOwlId(id, w1, false));
		}

        return w;
	}



    public Vector extractAssociations() {
        Vector w = new Vector();
        Vector w1 = new Vector();
        boolean istart = false;
        String id = null;

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);
			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}


			String t0 = t;
			t = t.trim();

		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

		    if (t.indexOf("<owl:Axiom>") != -1) {
				istart = false;
			}

			if (t.indexOf("<oboInOwl:id>") != -1) {
				id = extractTagValue(t, "<oboInOwl:id>");
			}

			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				istart = true;
				w1 = new Vector();
			} else if (t0.compareTo("    </owl:Class>") == 0) {
				if (w1 != null && w1.size() > 0) {
					w.addAll(prefixOboInOwlId(id, w1));
				}
				istart = false;
				w1 = null;
			}

			if (istart) {
				if (t.indexOf("rdf:resource") != -1 && t.indexOf("rdfs:subClassOf") == -1 && t.indexOf("owl:onProperty") == -1 && t.indexOf("owl:someValuesFrom") == -1) {
					w1.add(t);
				}
			}
		}
		if (w1 != null && w1.size() > 0) {
			w.addAll(prefixOboInOwlId(id, w1));
		}

        return w;
	}

	public String xml2Delimited(String t) {
		int n1 = t.indexOf(">");
		String tag = t.substring(1, n1);
		String value = extractTagValue(t, tag);
		return tag + "|" + value;
	}

    public Vector extractObjectProperties() {
        Vector w = new Vector();
        boolean istart = false;
        String classId = null;
        w = new Vector();

        for (int i=0; i<owl_vec.size(); i++) {
			String t = (String) owl_vec.elementAt(i);

			if (t.indexOf("General axioms") != -1) break;
			while (!t.endsWith(">") && i < owl_vec.size()-1) {
				i++;
				String nextLine = (String) owl_vec.elementAt(i);
				nextLine = nextLine.trim();
				t = t + " " + nextLine;
			}
		    if (t.indexOf("// Annotations") != -1) {
				break;
			}

			t = t.trim();
			if (t.startsWith("<owl:ObjectProperty")) {
				int n1 = t.indexOf("\"");
				int n2 = t.lastIndexOf("\"");
				String s = t.substring(n1+1, n2);
				w.add(s);
			}
		}
		return w;
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
        String owlfile = args[0];
		OBOScanner scanner = new OBOScanner(owlfile);
		//scanner.generate_FULL_SYN();

		String t = "<oboInOwl:hasOBONamespace>uberon</oboInOwl:hasOBONamespace>";
		System.out.println(scanner.xml2Delimited(t));


	}
}


