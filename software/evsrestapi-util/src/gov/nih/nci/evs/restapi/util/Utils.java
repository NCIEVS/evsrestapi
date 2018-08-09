package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.text.*;
import java.util.*;


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


public class Utils {

    public static void dumpHashMap(String label, HashMap hmap) {
		System.out.println("\n" + label + ":");
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String nv = (String) it.next();
			System.out.println("\n");

			Vector v = (Vector) hmap.get(nv);
			for (int i=0; i<v.size(); i++) {
				String q = (String) v.elementAt(i);
				System.out.println(nv + " --> " + q);
			}
		}
		System.out.println("\n");
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

    public static void dumpArrayList(String label, ArrayList list) {
		System.out.println("\n" + label + ":");
		if (list.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			int j = i+1;
			System.out.println("\t(" + j + ") " + t);
		}
		System.out.println("\n");
	}

    public static void dumpList(String label, List list) {
		System.out.println("\n" + label + ":");
		if (list.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			int j = i+1;
			System.out.println("\t(" + j + ") " + t);
		}
		System.out.println("\n");
	}


	 public static String replaceFilename(String filename) {
	    return filename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
	 }

	 public static void saveToFile(String outputfile, String t) {
		 Vector v = new Vector();
		 v.add(t);
		 saveToFile(outputfile, v);
	 }

	 public static void saveToFile(String outputfile, Vector v) {
		outputfile = replaceFilename(outputfile);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			if (v != null && v.size() > 0) {
				for (int i=0; i<v.size(); i++) {
					String t = (String) v.elementAt(i);
					pw.println(t);
				}
		    }
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

	public static void saveToFile(PrintWriter pw, Vector v) {
		if (v != null && v.size() > 0) {
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
		}
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

	public static String createStaticVariable(Vector v, String variableName) {
		StringBuffer buf = new StringBuffer();
		buf.append("public static final String[] " + variableName + " = new String[] {").append("\n");
		for (int i=0; i<v.size(); i++) {
			int j = i+1;
			String t = (String) v.elementAt(i);
			buf.append("\"" + t + "\", ");
			if (j % 10 == 0) {
				buf.append("\n");
			}
		}
		String s = buf.toString();
		s = s.trim();
		s = s.substring(0, s.length()-1) + "};";
		return s;
	}

	public static HashSet vector2HashSet(Vector v) {
		if (v == null) return null;
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			hset.add(t);
		}
		return hset;
	}

	public static Vector hashSet2Vector(HashSet hset) {
		if (hset == null) return null;
		Vector v = new Vector();
		Iterator it = hset.iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			v.add(t);
		}
		v = new SortUtils().quickSort(v);
		return v;
	}

    public static String changeFileExtension(String filename, String ext) {
		int n = filename.lastIndexOf(".");
		if (n != -1) {
			return filename.substring(0, n) + "." + ext;
		}
		return filename;
	}

	public static HashMap getInverseHashMap(HashMap hmap) {
		HashMap inv_hmap = new HashMap();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String value = (String) hmap.get(key);
			Vector v = new Vector();
			if (inv_hmap.containsKey(value)) {
				v = (Vector) inv_hmap.get(value);
			}
			v.add(key);
			inv_hmap.put(value, v);
		}
		return inv_hmap;
	}

    public static Vector listFiles(String directory) {
		Vector w = new Vector();
		Collection<File> c = listFileTree(new File(directory));
		int k = 0;
		Iterator it = c.iterator();
		while (it.hasNext()) {
			File t = (File) it.next();
			k++;
			w.add(t.getName());
		}
		w = new SortUtils().quickSort(w);
		return w;
	}


	public static Collection<File> listFileTree(File dir) {
		Set<File> fileTree = new HashSet<File>();
		if(dir==null||dir.listFiles()==null){
			return fileTree;
		}
		for (File entry : dir.listFiles()) {
			if (entry.isFile()) fileTree.add(entry);
			else fileTree.addAll(listFileTree(entry));
		}
		return fileTree;
	}

    public static boolean checkIfFileExists(String filename) {
		String currentDir = System.getProperty("user.dir");
		File f = new File(currentDir + "\\" + filename);
		if(f.exists() && !f.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}

    public static HashMap createRelationshipHashMap(Vector v) {
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String src = (String) u.elementAt(0);
			String rel = (String) u.elementAt(1);
			String target = (String) u.elementAt(2);
			if (u.size() == 6) {
				src = (String) u.elementAt(1);
				rel = (String) u.elementAt(3);
				target = (String) u.elementAt(5);
			}
			HashMap sub_map = new HashMap();
			if (hmap.containsKey(src)) {
				sub_map = (HashMap) hmap.get(src);
			}
			Vector w = new Vector();
			if (sub_map.containsKey(rel)) {
				w = (Vector) sub_map.get(rel);
			}
			if (!w.contains(target)) {
				w.add(target);
			}
			sub_map.put(rel, w);
			hmap.put(src, sub_map);

		}
		return hmap;
	}

    public static HashMap createInverseRelationshipHashMap(Vector v) {
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String src = (String) u.elementAt(0);
			String rel = (String) u.elementAt(1);
			String target = (String) u.elementAt(2);
			if (u.size() == 6) {
				src = (String) u.elementAt(1);
				rel = (String) u.elementAt(3);
				target = (String) u.elementAt(5);
			}
			HashMap sub_map = new HashMap();
			if (hmap.containsKey(target)) {
				sub_map = (HashMap) hmap.get(target);
			}
			Vector w = new Vector();
			if (sub_map.containsKey(rel)) {
				w = (Vector) sub_map.get(rel);
			}
			if (!w.contains(src)) {
				w.add(src);
			}
			sub_map.put(rel, w);
			hmap.put(target, sub_map);
		}
		return hmap;
	}


    public static void generate_construct_statement(String method_name, String params, String filename) {
		Vector u = Utils.readFile(filename);
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		w.add("public String " + method_name + "(" + params + ") {");
		w.add("\tString prefixes = getPrefixes();");
		w.add("\tStringBuffer buf = new StringBuffer();");
		w.add("\tbuf.append(prefixes);");
		for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			t = StringUtils.trimLeadingBlanksOrTabs(t);
			if (!t.startsWith("#")) {
				t = StringUtils.escapeDoubleQuotes(t);
				t = "\"" + t + "\"";
				t = "buf.append(" + t + ").append(\"\\n\");";
				w.add("\t" + t);
			}
		}
		w.add("\treturn buf.toString();");
		w.add("}");
		StringUtils.dumpVector(w);
	}

	public static Table constructTable(String label, Vector heading_vec, Vector data_vec) {
        return Table.construct_table(label, heading_vec, data_vec);
	}

}
