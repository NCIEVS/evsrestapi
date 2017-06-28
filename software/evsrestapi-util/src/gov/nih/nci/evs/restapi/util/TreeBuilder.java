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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.codec.binary.Base64;
import org.json.*;


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


public class TreeBuilder {
    private JSONUtils jsonUtils = null;
	private Vector<ServerInfo> serverInfo_vec = null;

	private OWLSPARQLUtils sparqlUtils = null;
	private String named_graph = "http://NCIt";

	private String username = null;
	private String password = null;

	public TreeBuilder() {

	}

	public TreeBuilder(OWLSPARQLUtils owlSPARQLUtils) {
        jsonUtils = new JSONUtils();
        sparqlUtils = owlSPARQLUtils;
	}

	public TreeBuilder(String serviceUrl, String username, String password) {
        jsonUtils = new JSONUtils();
        sparqlUtils = new OWLSPARQLUtils(serviceUrl, username, password);
	}

	public TreeBuilder(String serviceUrl) {
        jsonUtils = new JSONUtils();
        sparqlUtils = new OWLSPARQLUtils(serviceUrl, null, null);
	}

	public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
	}

	public static void printQuery(int k, String label, String query) {
		int n = label.indexOf("_");
		if (n != -1) {
			label = label.substring(n+1, label.length());
		}
		System.out.println("\n\nExample " + k + ": " + label + "\n");
		System.out.println(query);
	}

	public static void saveToFile(String label, String query) {
		int n = label.indexOf("_");
		if (n != -1) {
			label = label.substring(n+1, label.length());
		}

        PrintWriter pw = null;
        String outputfile = label + ".txt";
        try {
 			pw = new PrintWriter(outputfile, "UTF-8");
 			pw.println(query);

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

	public static File[] listFileInDirectory(String directoryName) {
		return IOUtils.listFileInDirectory(directoryName);
	}

    public Vector getFilesInFolder(String folderName) {
		Vector v = new Vector();
		File folder = new File(folderName);
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				v.add(listOfFiles[i].getAbsolutePath());
			}
		}
		return v;
	}

    public Vector parseData(String line, char delimiter) {
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

    public Vector getSubclassCodes(String code) {
		Vector v = sparqlUtils.getSubclassesByCode(named_graph, code);
        Vector w = new Vector();
        int n = v.size() / 2;
        for (int i=0; i<n; i++) {
			String t0 = (String) v.elementAt(2*i);
			Vector u = parseData(t0, '|');
			String label = (String) u.elementAt(2);
			String t1 = (String) v.elementAt(2*i+1);
			u = parseData(t1, '|');
			String cd = (String) u.elementAt(2);
			w.add(cd + "|" + label);
		}
        return w;
	}

    public Vector getSuperclassCodes(String code) {
		Vector v = sparqlUtils.getSuperclassesByCode(named_graph, code);
        Vector w = new Vector();
        int n = v.size() / 2;
        for (int i=0; i<n; i++) {
			String t0 = (String) v.elementAt(2*i);
			Vector u = parseData(t0, '|');
			String label = (String) u.elementAt(2);
			String t1 = (String) v.elementAt(2*i+1);
			u = parseData(t1, '|');
			String cd = (String) u.elementAt(2);
			w.add(cd + "|" + label);
		}
        return w;
	}

    public String getLabel(String code) {
        Vector v = sparqlUtils.getLabelByCode(named_graph, code);
        //x_label|literal|Heart Failure
        String t = (String) v.elementAt(0);
        Vector w = parseData(t, '|');
        return (String) w.elementAt(2);
    }

//////////////////////////////////////////////////////////////////////
	public int getLevel(String line) {
		int level = 0;
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c != '\t') return level;
			level++;
		}
		return level;
	}

	public int getMaxLevel(Vector v) {
		int maxLevel = 0;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			int level = getLevel(line);
			if (level > maxLevel) {
				maxLevel = level;
			}
		}
		return maxLevel;
	}

	public String getDisplayLabel(String t) {
		int level = getLevel(t);
		return t.substring(level, t.length());
	}

	public String getText(String line) {
		String t = getDisplayLabel(line);
		int n = t.indexOf("(");
		t = t.substring(0, n);
		t = t.trim();
		return t;
	}


	public String getCode(String line) {
		String t = getDisplayLabel(line);
		int n = t.indexOf("(");
		t = t.substring(n+1, t.length()-1);
		t = t.trim();
		return t;
	}

    public TreeItem asciiTree2TreeItem(Vector v) {
		return createTreeItem(v);
	}

    public TreeItem createTreeItem(Vector v) {
		int root_count = 0;
		HashMap hmap = new HashMap();
		String assocText = Constants.INVERSE_IS_A;

		TreeItem root_ti = new TreeItem("@", "root");
		int maxLevel = getMaxLevel(v);
		TreeItem[] nodes = new TreeItem[maxLevel+1];
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			String key = getDisplayLabel(t);
			TreeItem child_ti = null;
			if (hmap.containsKey(t)) {
				child_ti = (TreeItem) hmap.get(key);
			} else {
				child_ti = new TreeItem(getCode(t), getText(t));
				hmap.put(key, child_ti);
			}
			int level = getLevel(t);
			nodes[level] = child_ti;
			if (level == 0) {
				root_ti.addChild(assocText, child_ti);
				root_ti._expandable = true;
			} else {
				TreeItem parent_ti = nodes[level-1];
				parent_ti.addChild(assocText, child_ti);
				parent_ti._expandable = true;
			}
		}
		return root_ti;
	}
//////////////////////////////////////////////////////////////////////


	public Vector generateTreeData(String code) {
		return generateTreeData(code, Constants.TRAVERSE_DOWN);
	}

	public Vector generateTreeData(String code, int direction) {
		Stack stack = new Stack();
		String label = getLabel(code);

		String parent_code = "@";
		String parent_label = "root";

		stack.push("0|" + parent_label + "|" + parent_code + "|" + label + "|" + code);
		Vector w = new Vector();
        while (!stack.isEmpty()) {
			String s = (String) stack.pop();
			Vector u = parseData(s, '|');
			String s0 = (String) u.elementAt(0);
			int level = Integer.parseInt(s0);
			String s1 = (String) u.elementAt(1);
			String s2 = (String) u.elementAt(2);
			String s3 = (String) u.elementAt(3);
			String s4 = (String) u.elementAt(4);

			String indent = "";
			for (int i=0; i<level; i++) {
				indent = indent + "\t";
			}
			String line = s1 + "|" + s2 + "|" + s3 + "|" + s4;
			if (!w.contains(line)) {
				w.add(line);
			}
            u = null;
            if (direction == Constants.TRAVERSE_UP) {
            	u = getSuperclassCodes(s4);
		    } else {
				u = getSubclassCodes(s4);
			}

            if (u != null) {
				level = level + 1;
				for (int j=0; j<u.size(); j++) {
					String t3 = (String) u.elementAt(j);
					Vector u2 = parseData(t3, '|');
					String next_code = (String) u2.elementAt(0);
					String next_label = (String) u2.elementAt(1);
					String t = "" + level + "|" + s3 + "|" + s4 + "|" + next_label + "|" + next_code;
					stack.push(t);
				}
		    }
		}
		return w;
	}

	public void traverse(PrintWriter pw, String code, int direction) {
		Stack stack = new Stack();
		String label = getLabel(code);
		stack.push("0|"+ code + "|" + label);
        while (!stack.isEmpty()) {
			String s = (String) stack.pop();
			Vector u = parseData(s, '|');
			String level_str = (String) u.elementAt(0);
			int level = Integer.parseInt(level_str);
			String curr_code = (String) u.elementAt(1);
			label = (String) u.elementAt(2);
			String indent = "";
			for (int i=0; i<level; i++) {
				indent = indent + "\t";
			}
			pw.println(indent + label + " (" + curr_code + ")");

            Vector w = null;
            if (direction == Constants.TRAVERSE_UP) {
            	w = getSuperclassCodes(curr_code);
		    } else {
				w = getSubclassCodes(curr_code);
			}
            if (w != null) {
				level = level + 1;
				for (int j=0; j<w.size(); j++) {
					String next_code = (String) w.elementAt(j);
					String t = "" + level + "|" + next_code;
					stack.push(t);
				}
		    }
		}
	}

    public void dumpVector(String label, Vector v) {
		PrintWriter pw = null;
		dumpVector(pw, label, v);
	}


    public void dumpVector(PrintWriter pw, String label, Vector v) {
		if (pw == null) {
			pw = new PrintWriter(System.out);
		}
		pw.println("\n" + label);
		int j = 0;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			j = i+1;
			pw.println("(" + j + ") " + t);
		}
		if (v.size() == 0) {
			pw.println("\tNone.");
		}
	}

    public static Links loadTreeData(String filename) {
		List a = new ArrayList();
		Utils utils = new Utils();
		Vector v = new HTTPUtils().readFile(filename);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			Vector u = utils.parseData(t, '|');
			String s1 = (String) u.elementAt(0);
			String s2 = (String) u.elementAt(1);
			String s3 = (String) u.elementAt(2);
			String s4 = (String) u.elementAt(3);
			Link link = new Link(j, s2, s1, s4, s3);
			a.add(link);
		}
		Links links = new Links();
		links.setLinks(a);
		return links;
	}

	public Paths getPaths(String code) {
		int direction = Constants.TRAVERSE_UP;
		return getPaths(code, direction);
	}

	public Paths getPaths(String code, int direction) {
		Vector u = generateTreeData(code, direction);
		Vector v = new Vector();
		for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			if (t.indexOf("@") == -1) {
				v.add(t);
			}
		}
		PathFinder pathFinder = new PathFinder(v);
		Paths paths = pathFinder.findPaths();
		return paths;
	}


    public static void main(String[] args) {
		String serviceUrl = "http://ncidb-d174-v.nci.nih.gov/sparql1?query=";
		String username = null;
		String password = null;
		String named_graph = "http://NCIt";
		String code = "C86567"; //Lifting
		String outputfile = "tree.txt";

//E:\jdk1.8.0_45\bin\java -d64 -Xms512m -Xmx4g -classpath %CLASSPATH% TreeBuilder "http://ncidb-d174-v.nci.nih.gov/sparql1?query=" triplereadonly triplereadonly "http://NCIt" C86567 tree.txt
        if (args.length == 6) {
			serviceUrl = args[0];
			username = args[1];
			password = args[2];
			named_graph = args[3];
			code = args[4];
			outputfile = args[5];
		}
		/*
        String directory = "queries";
        */
        int direction = Constants.TRAVERSE_DOWN;
        TreeBuilder treeBuilder = new TreeBuilder(serviceUrl);

        PrintWriter pw = null;
        try {
 			pw = new PrintWriter(outputfile, "UTF-8");
            treeBuilder.traverse(pw, code, direction);
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
}
