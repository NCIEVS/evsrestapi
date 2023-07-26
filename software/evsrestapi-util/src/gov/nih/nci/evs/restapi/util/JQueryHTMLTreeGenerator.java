package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.appl.*;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;


public class JQueryHTMLTreeGenerator {
	HierarchyHelper hh = null;
	Vector parent_child_vec = null;
	String TREE_VARIABLE_NAME = "demoList";
	String HYPERLINK = "https://nciterms.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&type=terminology&key=null&b=1&n=0&vse=null&code=";

	String fontColor = "red";
	HashSet nodeSet = null;

	public JQueryHTMLTreeGenerator(Vector parent_child_vec) {
		this.parent_child_vec = parent_child_vec;
		hh = new HierarchyHelper(parent_child_vec);
		hh.findRootAndLeafNodes();
		Vector roots = hh.getRoots();
    }

	public HierarchyHelper getHierarchyHelper() {
		return this.hh;
	}

	public void set_tree_variable_name(String name) {
		TREE_VARIABLE_NAME = name;
	}

	public void setHYPERLINK(String HYPERLINK) {
		this.HYPERLINK = HYPERLINK;
	}

	public void setFontColor(String fontColor) {
		this.fontColor = fontColor;
	}

	public void setNodeSet(HashSet nodeSet) {
		this.nodeSet = nodeSet;
	}

	public String getHyperLink(String code) {
		if (HYPERLINK != null) {
			return HYPERLINK + code;
		} else {
			return null;
		}
	}

    public String encode(String t) {
		if (t == null) return null;
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<t.length(); i++) {
			char c = t.charAt(i);
			if (c > 126) {
				buf.append(" ");
			} else {
				String s = "" + c;
				if (s.compareTo("'") == 0) {
					buf.append("\\'");
				} else {
					buf.append(s);
				}
			}
		}
		return buf.toString();
	}

    public void writeHeader(PrintWriter out, String title) {
      out.println("<!doctype html>");
      out.println("<html lang=\"en\">");
      out.println("<head>");
      out.println("	<meta charset=\"utf-8\">");
      out.println("	<title>" + title + "</title>");
      out.println("	<link rel=\"stylesheet\" href=\"tree.css\">");
      writeFunction(out);
      out.println("</head>");
	}


    public void writeFunction(PrintWriter out) {
		out.println("	<script type=\"text/javascript\" src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js\"></script>");
		out.println("	<script type=\"text/javascript\" src=\"js/jquery.sapling.min.js\"></script>");
		out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"css/tree.css\">");
		out.println("	<script type=\"text/javascript\">");
		out.println("		$(document).ready(function() {");
		out.println("			$('#demoList').sapling();");
		out.println("		});");
		out.println("	</script>");
    }


	public String getIndentation(int level) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<level; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}


    public void writeTree(PrintWriter out) {
		out.println("\t<ul id=\"" + TREE_VARIABLE_NAME + "\">");
		Vector roots = hh.getRoots();
		roots = sortByLabel(roots);
		for (int i=0; i<roots.size(); i++) {
			String code = (String) roots.elementAt(i);
			if (code.compareTo("<Root>") == 0) {
				String indent = getIndentation(1);
				Vector subs = hh.getSubclassCodes(code);
				if (subs != null && subs.size() > 0) {
					out.println(indent + "\t<ul>");
					subs = sortByLabel(subs);
					for (int j=0; j<subs.size(); j++) {
						String sub = (String) subs.elementAt(j);
						writeNode(out, sub, 2);
					}
					out.println(indent + "\t</ul>");
				}
				out.println(indent + "</li>");

			} else {
				writeNode(out, code, 2);
			}

		}
		out.println("\t</ul>");
	}

	public void writeNode(PrintWriter out, String code, int level) {
		String indent = getIndentation(level);
		String label = hh.getLabel(code);
		label = encode(label);
		String text = label + " (" + code + ")";
		String hyperlink = getHyperLink(code);

		if (nodeSet != null && nodeSet.contains(code)) {
			if (hyperlink != null) {
				out.println(indent + "<li><font color=\"" + fontColor + "\"><a href=\"" + hyperlink + "\">" + label + "</font></a>");
			} else {
				out.println(indent + "<li><font color=\"" + fontColor + "\">" + label + "</font>");
			}
		} else {
			if (hyperlink != null) {
				out.println(indent + "<li><a href=\"" + hyperlink + "\">" + label + "</a>");
			} else {
				out.println(indent + "<li>" + label);
			}
		}

		Vector subs = hh.getSubclassCodes(code);
		if (subs != null && subs.size() > 0) {
			out.println(indent + "\t<ul>");
			subs = sortByLabel(subs);
			for (int i=0; i<subs.size(); i++) {
				String sub = (String) subs.elementAt(i);
				writeNode(out, sub, level+1);
			}
			out.println(indent + "\t</ul>");
		}
		out.println(indent + "</li>");
	}

	public void writeBody(PrintWriter out, String title) {
		out.println("<body>");
		out.println("	<h3>" + title + "</h3>");
		out.println("	<hr>");
		writeTree(out);
		out.println("</body>");
	}

	public void writeFooter(PrintWriter out) {
		out.println("</html>");
	}

	public Vector sortByLabel(Vector codes) {
		if (codes == null || codes.size()<=1) return codes;
		Vector w = new Vector();
		HashMap hmap = new HashMap();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			String label = hh.getLabel(code);
			hmap.put(label, code);
			w.add(label);
		}
		w = new SortUtils().quickSort(w);
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String label = (String) w.elementAt(i);
			String code = (String) hmap.get(label);
			v.add(code);
		}
		return v;
	}

    public void generate(PrintWriter out, String title) {
        writeHeader(out, title);
        writeBody(out, title);
        writeFooter(out);
	}

	public void generate(String outputfile, String title) {
		long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			generate(pw, title);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static Vector filterRoots(Vector parent_child_vec) {
		Vector w = new Vector();
		for (int i=0; i<parent_child_vec.size(); i++) {
			String line = (String) parent_child_vec.elementAt(i);
			if (line.indexOf("|<Root>|") == -1) {
				w.add(line);
			}
		}
		return w;
	}


	public static void main(String[] args) {
		/*
		String filename = args[0];
		System.out.println(filename);
		Vector parent_child_vec = Utils.readFile(filename);
		parent_child_vec = filterRoots(parent_child_vec);

		JQueryHTMLTreeGenerator generator = new JQueryHTMLTreeGenerator(parent_child_vec);

		String hyperlinkUrl = "https://nciterms65.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&type=terminology&key=null&b=1&n=0&vse=null&code=";
        generator.setHYPERLINK(hyperlinkUrl);

	    String outputfile = "disease_tree.html";
	    String title = "Test HTML Tree";
		generator.generate(outputfile, title);
		*/
        long ms = System.currentTimeMillis();
        String parent_child_file = args[0];
        Vector parent_child_vec = Utils.readFile(parent_child_file);
        String root = args[1];
        Vector codes = new Vector();
        codes.add(root);
        TransitiveClosureRunner runner = new TransitiveClosureRunner(parent_child_vec);
        Vector v = runner.run(codes);
        String outputfile = "parent_child_" + root + ".txt";
        Utils.saveToFile(outputfile, v);

		parent_child_vec = filterRoots(v);
		JQueryHTMLTreeGenerator generator = new JQueryHTMLTreeGenerator(parent_child_vec);
		String hyperlinkUrl = "https://nciterms65.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&type=terminology&key=null&b=1&n=0&vse=null&code=";
        //generator.setHYPERLINK(hyperlinkUrl);
        generator.setHYPERLINK(null);

	    outputfile = root + "_tree.html";
	    String label = runner.getHierarchyHelper().getLabel(root);
	    String title = label + " (" + root + ")" + " Hierarchy";
	    Vector mth_nodes = Utils.readFile("main_types.txt");
	    HashSet nodeSet = Utils.vector2HashSet(mth_nodes);
	    generator.setNodeSet(nodeSet);
		generator.generate(outputfile, title);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

}




