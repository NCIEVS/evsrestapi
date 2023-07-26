package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.util.*;


public class ValueSetHTMLHierarchy {
	HashMap code2NodeMap = new HashMap();
    HierarchyHelper hh = null;
    static String a8File = "A8.txt";
    Vector a8_vec = null;
    String title = null;

    String parent_child_file = "parent_child.txt";
    Vector parent_child_vec = null;
    HashMap subset_hmap = null;
    String owlfile = null;
    String root = null;

    public ValueSetHTMLHierarchy(String owlfile, String root) {
        this.owlfile = owlfile;
        this.root = root;
        initialize();
	}

    public void initialize() {
		OWLScanner owlscanner = new OWLScanner(owlfile);
		parent_child_vec = owlscanner.extractHierarchicalRelationships();
		gov.nih.nci.evs.restapi.util.Utils.saveToFile("parent_child.txt", parent_child_vec);
		String associationCode = "A8";
		a8_vec = owlscanner.extractAssociations(owlscanner.get_owl_vec(), associationCode);
		gov.nih.nci.evs.restapi.util.Utils.saveToFile(a8File, a8_vec);

		Vector parent_child_vec = Utils.readFile(parent_child_file);
		a8_vec = Utils.readFile(a8File);
		System.out.println("parent_child_vec: " + parent_child_vec.size());

		hh = new HierarchyHelper(parent_child_vec);
		subset_hmap = create_subset_hmap();
	}

	public Vector sortByLabel(Vector codes) {
		HashMap label2CodeMap = new HashMap();
		Vector w = new Vector();
		Vector labels = new Vector();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			String label = (String) hh.getLabel(code);
			labels.add(label);
		    label2CodeMap.put(label, code);
		}
		labels = new SortUtils().quickSort(labels);
		for (int i=0; i<labels.size(); i++) {
			//int j = labels.size()-i-1;
			//String label = (String) labels.elementAt(j);
			String label = (String) labels.elementAt(i);
			String code = (String) label2CodeMap.get(label);
			w.add(code);
		}
		return w;
	}

    public void setTitle(String title) {
		this.title = title;
	}

    public void writeData(PrintWriter out) {
		out.println("	<h1>" + title + "</h1>");
		out.println("	<div id=\"html\" class=\"demo\">");


		out.println("		<ul>");
		out.println("			<li data-jstree='{ \"opened\" : true }'>NCI Thesaurus");
		out.println("				<ul>");
		out.println("					<li data-jstree='{ \"selected\" : true }'>Child node 1</li>");
		out.println("					<li>Child node 2</li>");
		out.println("				</ul>");
		out.println("			</li>");
		out.println("		</ul>");


		out.println("	</div>");
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


	public HashMap create_subset_hmap() {
		HashMap hmap = new HashMap();
		for (int i=0; i<a8_vec.size(); i++) {
			String line = (String) a8_vec.elementAt(i);
			Vector u = parseData(line, '|');
			String member_code = (String) u.elementAt(0);
			String subset_code = (String) u.elementAt(2);
			Vector w = new Vector();
			if (hmap.containsKey(subset_code)) {
				w = (Vector) hmap.get(subset_code);
			}
			w.add(member_code);
			hmap.put(subset_code, w);
		}
		return hmap;
	}

/*
	public void writeSubNodes(PrintWriter out, String code) {
		String label = (String) hh.getLabel(code);
		String link_code = HyperlinkHelper.toHyperlink(code);
		String display = label + " (" + link_code + ")";
		out.println("		<ul>");
		out.println("			<li data-jstree='{ \"opened\" : false }'>" + display);

		Vector subs = (Vector) subset_hmap.get(code);
		if (subs != null) {
			subs = sortByLabel(subs);
			for (int j=0; j<subs.size(); j++) {
				String sub = (String) subs.elementAt(j);
				writeSubNodes(out, sub);
			}
		}

		out.println("			</li>");
		out.println("		</ul>");
	}
*/

	public void writeSubNodes(PrintWriter out, String code) {
		//String label = (String) hh.getLabel(code);
		//String link_code = HyperlinkHelper.toHyperlink(code);
		//String display = label + " (" + link_code + ")";
		out.println("		<ul>");
		//out.println("			<li data-jstree='{ \"opened\" : false }'>" + display);

		Vector subs = (Vector) subset_hmap.get(code);
		if (subs != null) {
			subs = sortByLabel(subs);
			for (int j=0; j<subs.size(); j++) {
				String sub = (String) subs.elementAt(j);
				String label = (String) hh.getLabel(sub);
				String link_code = HyperlinkHelper.toHyperlink(sub);
				String display = label + " (" + link_code + ")";
				out.println("			<li data-jstree='{ \"opened\" : false }'>" + display);
		        out.println("			</li>");

				//writeSubNodes(out, sub);
			}
		}

		//out.println("			</li>");
		out.println("		</ul>");
	}

	public void write_data(PrintWriter out, String title, String root) {
		/*
		Stack stack = new Stack();
		stack.push(root);
		while (!stack.isEmpty()) {
            String code = (String) stack.pop();
            writeNode(out, code);
		}
		*/
		writeNode(out, root);
	}

    public Vector getSubsAndMembers(String c) {
		Vector w = new Vector();
		Vector children = hh.getSubclassCodes(c);
		if (children != null && children.size() > 0) {
			w.addAll(children);
		}
		Vector subsets = (Vector) subset_hmap.get(c);
		if (subsets != null && subsets.size() > 0) {
			//w.addAll(subsets);
			for (int i=0; i<subsets.size(); i++) {
				String s = (String) subsets.elementAt(i);
				if (!w.contains(s)) {
					w.add(s);
				}
			}
		}
		return w;
	}
/*
	public void writeNode0(PrintWriter out, String code) {
		String label = (String) hh.getLabel(code);
		String link_code = HyperlinkHelper.toHyperlink(code);
		String display = label + " (" + link_code + ")";
		out.println("		<ul>");
		out.println("			<li data-jstree='{ \"opened\" : false }'>" + display);

		//writeSubNodes(out, code);
		Vector w = getSubsAndMembers(code);
		for (int i=0; i<w.size(); i++) {
			String s = (String) w.elementAt(i);
			Vector w1 = getSubsAndMembers(s);
			if (w1.size() > 0) {
				writeNode(out, s);
			}
		}
		out.println("			</li>");
		out.println("		</ul>");
	}
*/

	public void writeNode(PrintWriter out, String code) {
		String label = (String) hh.getLabel(code);
		String link_code = HyperlinkHelper.toHyperlink(code);
		String display = label + " (" + link_code + ")";
		out.println("		<ul>");
		out.println("			<li data-jstree='{ \"opened\" : false }'>" + display);
		Vector w = getSubsAndMembers(code);

		if (w != null && w.size() > 0) {
			w = sortByLabel(w);
			//out.println("		<ul>");
			for (int i=0; i<w.size(); i++) {
				String t = (String) w.elementAt(i);
				Vector subsets = (Vector) subset_hmap.get(t);
				if (subsets != null && subsets.size() > 0) {
					writeNode(out, t);
				} else {
					label = (String) hh.getLabel(t);
					link_code = HyperlinkHelper.toHyperlink(t);
					display = label + " (" + link_code + ")";
					out.println("		<ul>");
					out.println("			<li data-jstree='{ \"opened\" : false }'>" + display);
					out.println("		</ul>");
				}
			}
			//out.println("		</ul>");
		}
		out.println("			</li>");
		out.println("		</ul>");
	}


    public void run(PrintWriter out) {
		out.println("<!DOCTYPE html>");
		out.println("<html lang=\"en\">");
		out.println("<head>");
		out.println("	<meta charset=\"UTF-8\">");
		out.println("	<title>NCI Thesaurus</title>");
		out.println("	<style>");
		out.println("	html { margin:0; padding:0; font-size:62.5%; }");
		out.println("	body { max-width:800px; min-width:300px; margin:0 auto; padding:20px 10px; font-size:14px; font-size:1.4em; }");
		out.println("	h1 { font-size:1.8em; }");
		out.println("	.demo { overflow:auto; border:1px solid silver; min-height:100px; }");
		out.println("	</style>");
		out.println("	<link rel=\"stylesheet\" href=\"style.min.css\" />");
		out.println("");
		out.println("</head>");
		out.println("<body>");

		String title = (String) hh.getLabel(root);
		write_data(out, title, root);

		out.println("");
		out.println("");
		out.println("	<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js\"></script>");
		out.println("	<script src=\"jstree.min.js\"></script>");
		out.println("");
		out.println("	<script>");
		out.println("	// html demo");
		out.println("	$('#html').jstree();");
		out.println("");

		out.println("	</script>");
		out.println("</body>");
		out.println("</html>");
    }

    public void run() {
        long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		String htmlfile = this.root + ".html";
		try {
			pw = new PrintWriter(htmlfile, "UTF-8");
            run(pw);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + htmlfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

    public static void main(String[] args) {
		String owlfile = args[0];
		String root = args[1];
		ValueSetHTMLHierarchy valueSetHTMLHierarchy = new ValueSetHTMLHierarchy(owlfile, root);
		valueSetHTMLHierarchy.run();
	}
}