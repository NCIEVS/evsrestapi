package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.util.*;
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
		String label = null;
		String owlannotatedSource_value = null;
		String owlannotatedProperty_value = null;
		String owlannotatedTarget_value = null;
		String qualify_data = null;

		Vector w = new Vector();
		int i = 0;
		String target = open_tag;
		boolean istart = false;
		Vector v = new Vector();
		String class_line = null;
		StringBuffer buf = new StringBuffer();
		boolean owlannotatedTarget_start = false;

		int class_knt = 0;

		while (i < owl_vec.size()) {
			String line = (String) owl_vec.elementAt(i);
			if (line.indexOf(OWL_CLS_TARGET) != -1 && line.indexOf("enum") == -1) {
				class_knt++;
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
						OWLAxiom owl_Axiom = new OWLAxiom(class_knt,
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

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
        String owlfile = args[0];
		OWLScanner scanner = new OWLScanner(owlfile);
		int n = owlfile.lastIndexOf(".");
		String outputfile = owlfile.substring(0, n) + "_" + getToday() + ".owl";
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
        Vector v = scanner.scanOwlTags();
        v = scanner.fileterTagData(v, "owl:intersectionOf");
        Utils.dumpVector("fileterTagData", v);
        scanner.printPaths(v);
    }
}


