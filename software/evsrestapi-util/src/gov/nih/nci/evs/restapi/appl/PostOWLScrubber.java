package gov.nih.nci.evs.restapi.appl;

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


public class PostOWLScrubber {
    String owlfile = null;
    Vector owl_vec = null;
    Vector disjointwith_classes = null;
    HashMap subclassOfHashMap = null;

    static String NCIT_NAMESPACE_TARGET = "<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#";
    static String OWL_CLS_TARGET = NCIT_NAMESPACE_TARGET + "C"; //"<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C";
    static String rdfsSubClassOf = "rdfs:subClassOf";
    static String owlDisjointWith = "owl:disjointWith";
    static String open_class_tag = "<owl:Class ";
    //static String close_class_tag = "</owl:Class>";

    public PostOWLScrubber(String owlfile) {
        this.owlfile = owlfile;
        this.owl_vec = readFile(owlfile);
        disjointwith_classes = findConceptsWithDisjointWithRelationships();
        subclassOfHashMap = getSubclassOfHashMap(disjointwith_classes);
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

	public Vector isSubclassOf(Vector codes) {
		Vector v = new Vector();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			Vector w = isSubclassOf(code);
			v.addAll(w);
		}
		return v;
	}

	public Vector isSubclassOf(String code) {
		Vector v = new Vector();
		Vector w = getOWLClassDataByCode(code);
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			if (line.indexOf(rdfsSubClassOf) != -1) {
				int n = line.lastIndexOf("#");
				String t = line.substring(n+1, line.length());
				n = t.indexOf("\"");
				t = t.substring(0, n);
				v.add(code + "|" + t);
			}
		}
		return v;
	}

	public HashMap getSubclassOfHashMap(Vector codes) {
		Vector v = isSubclassOf(codes);
		boolean skipHeading = false;
		return constructHashMap(v, 0, 1, false);
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


	public Vector removeOwlDisjointWithFromClass(String code) {
		Vector v = getOWLClassDataByCode(code);
		return removeOwlDisjointWith(v);
	}

	public Vector removeOwlDisjointWith(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.indexOf(owlDisjointWith) == -1)
			    w.add(line);
		}
		return w;
	}


	public Vector addOwlDisjointWith(String code) {
		Vector v = getOWLClassDataByCode(code);
		v = removeOwlDisjointWith(v);
	    return addOwlDisjointWith(v, code);
	}


	public Vector addOwlDisjointWith(Vector v, String code) {
		String target = open_class_tag;
		boolean istart = false;
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.indexOf(target) != -1) {
				w.add(line);
				for (int j=0; j<disjointwith_classes.size(); j++) {
					String t = (String) disjointwith_classes.elementAt(j);
					if (t.compareTo(code) != 0) {
						String stmt = addOwlDisjointWithStmt(t);
						w.add("        " + stmt);
					}
				}
			} else {
				w.add(line);
			}
		}
		return w;
	}

	public String addOwlDisjointWithStmt(String code) {
		return "<owl:disjointWith rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#" + code + "\"/>";
	}

    public boolean hasOwlDisjointWith(Vector v) {
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.indexOf(owlDisjointWith) != -1) {
				return true;
			}
		}
		return false;
	}

    public String getClassLine(Vector v) {
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.indexOf("owl:Class") != -1) {
				return line;
			}
		}
		return null;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
	public Vector findConceptsWithDisjointWithRelationships() {
		Vector w = new Vector();
		int i = 0;
		String target = OWL_CLS_TARGET;
		boolean istart = false;
		Vector v = new Vector();
		String class_line = null;
		while (i < owl_vec.size()) {
			String line = (String) owl_vec.elementAt(i);
			if (line.indexOf(OWL_CLS_TARGET) != -1 && line.indexOf("enum") == -1) {
				if (v.size() > 0) {
					if (hasOwlDisjointWith(v)) {
						class_line = getClassLine(v);
						String code = getCode(class_line);
						w.add(code);
					}
				}
				v = new Vector();
				istart = true;
			} else if (istart && line.indexOf(OWL_CLS_TARGET) != -1 && line.indexOf("enum") == -1) {
                istart = false;
			}
			if (istart) {
				v.add(line);
			}
			i++;
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

	public void getModifiedClasses(PrintWriter pw) {
        for (int i=0; i<disjointwith_classes.size(); i++) {
			String code = (String) disjointwith_classes.elementAt(i);
		    Vector v = getOWLClassDataByCode(code);
		    int j = i+1;
		    dumpVector(pw, "(" + j + ") " + "Change from", v, true, false);
		    v = removeOwlDisjointWith(v);
		    if (!subclassOfHashMap.containsKey(code)) {
		   	    v = addOwlDisjointWith(v, code);
			}
		    dumpVector(pw, "To", v, true, false);
		}
	}


	public void getModifiedClasses(String outputfile) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
            getModifiedClasses(pw);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}



	public void run(String outputfile) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
            run(pw);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void run(PrintWriter pw) {
		int i = 0;
		String target = OWL_CLS_TARGET;
		boolean istart = false;
		Vector v = new Vector();
		String class_line = null;
		while (i < owl_vec.size()) {
			String line = (String) owl_vec.elementAt(i);
			if (line.indexOf(OWL_CLS_TARGET) != -1 && line.indexOf("enum") == -1) {
				if (v.size() > 0) {
					if (hasOwlDisjointWith(v)) {
						class_line = getClassLine(v);
						String code = getCode(class_line);
						v = removeOwlDisjointWith(v);
						if (!subclassOfHashMap.containsKey(code)) {
							v = addOwlDisjointWith(v, code);
						}
						for (int k=0; k<v.size(); k++) {
							String s = (String) v.elementAt(k);
							pw.println(s);
						}
					} else {
						for (int k=0; k<v.size(); k++) {
							String s = (String) v.elementAt(k);
							pw.println(s);
						}
					}
				}
				v = new Vector();
				istart = true;
			} else if (istart && line.indexOf(OWL_CLS_TARGET) != -1 && line.indexOf("enum") == -1) {
                istart = false;
			}
			if (istart) {
				v.add(line);
			} else {
				pw.println(line);
			}
			i++;
		}

		if (v.size() > 0) {
			if (hasOwlDisjointWith(v)) {
				class_line = getClassLine(v);
				String code = getCode(class_line);
				v = removeOwlDisjointWith(v);
				if (!subclassOfHashMap.containsKey(code)) {
					v = addOwlDisjointWith(v, code);
				}
				for (int k=0; k<v.size(); k++) {
					String s = (String) v.elementAt(k);
					pw.println(s);
				}
			} else {
				for (int k=0; k<v.size(); k++) {
					String s = (String) v.elementAt(k);
					pw.println(s);
				}
			}
		}
		printFootnote(pw);
	}

	private void printFootnote(PrintWriter pw) {
	    pw.println("<!-- owl:disjointWith modified by the PostOWLScrubber (version 1.0) on " + getToday() + " -->");
    }


    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
        String owlfile = args[0];
		PostOWLScrubber postOWLScrubber = new PostOWLScrubber(owlfile);
		int n = owlfile.lastIndexOf(".");
		String outputfile = owlfile.substring(0, n) + "_" + getToday() + ".owl";
		postOWLScrubber.run(outputfile);
		String changefile = "owlDisjointWith_mod_" + getToday() + ".txt";
		postOWLScrubber.getModifiedClasses(changefile);


/*
	(1) Abnormal Cell (C12913)
	(2) Activity (C43431)
	(3) Anatomic Structure, System, or Substance (C12219)
	(4) Biochemical Pathway (C20633)
	(5) Biological Process (C17828)
	(6) Chemotherapy Regimen or Agent Combination (C12218)
	(7) Conceptual Entity (C20181)
	(8) Diagnostic or Prognostic Factor (C20047)
	(9) Diagnostic, Therapeutic, or Research Equipment (C19238)
	(10) Disease, Disorder or Finding (C7057)
	(11) Drug, Food, Chemical or Biomedical Material (C1908)
	(12) Experimental Organism Anatomical Concept (C22188)
	(13) Experimental Organism Diagnosis (C22187)
	(14) Gene (C16612)
	(15) Gene Product (C26548)
	(16) Manufactured Object (C97325)
	(17) Molecular Abnormality (C3910)
	(18) Organism (C14250)
	(19) Property or Attribute (C20189)
	(20) Retired Concept (C28428)

*/
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }
}

