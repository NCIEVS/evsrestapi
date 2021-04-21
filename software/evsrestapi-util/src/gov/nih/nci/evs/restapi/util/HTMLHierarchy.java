package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.util.*;


public class HTMLHierarchy {
	HashMap code2NodeMap = new HashMap();
    HierarchyHelper hh = null;

    public String title = "NCI Thesaurus";

    String datafile = null;

    public HTMLHierarchy(String datafile) {
        this.datafile = datafile;
        initialize();
	}

    public void initialize() {
		Vector parent_child_vec = Utils.readFile(datafile);
		System.out.println("parent_child_vec: " + parent_child_vec.size());

		hh = new HierarchyHelper(parent_child_vec);
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
			int j = labels.size()-i-1;
			String label = (String) labels.elementAt(j);
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


	public void writeNode(PrintWriter out, String code) {
		String label = (String) hh.getLabel(code);
		String link_code = HyperlinkHelper.toHyperlink(code);
		String display = label + " (" + link_code + ")";
		out.println("		<ul>");
		out.println("			<li data-jstree='{ \"opened\" : false }'>" + display);

		Vector subs = hh.getSubclassCodes(code);
		if (subs != null) {
			//subs = sortByLabel(subs);
			for (int j=0; j<subs.size(); j++) {
				String sub = (String) subs.elementAt(j);
				writeNode(out, sub);
			}
		}
		out.println("			</li>");
		out.println("		</ul>");
	}

	public void write_data(PrintWriter out, String title, String root) {
		Stack stack = new Stack();
		if (root != null) {
			stack.push(root);
		} else {
			Vector codes = hh.getRoots();
			codes = sortByLabel(codes);

			for (int i=0; i<codes.size(); i++) {
				int k = codes.size()-i-1;
				String code = (String) codes.elementAt(k);
				stack.push(code);
			}
	    }

		while (!stack.isEmpty()) {
            String code = (String) stack.pop();
            writeNode(out, code);
		}
	}

    public void run(PrintWriter out, String title, String root) {
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
		/*
		out.println("	// inline data demo");
		out.println("	$('#data').jstree({");
		out.println("		'core' : {");
		out.println("			'data' : [");
		out.println("				{ \"text\" : \"Root node\", \"children\" : [");
		out.println("						{ \"text\" : \"Child node 1\" },");
		out.println("						{ \"text\" : \"Child node 2\" }");
		out.println("				]}");
		out.println("			]");
		out.println("		}");
		out.println("	});");
		out.println("");
		*/

		out.println("	</script>");
		out.println("</body>");
		out.println("</html>");
    }

    public static void run(String datafile, String title, String root) {
        long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		int n = datafile.lastIndexOf(".");
		String outputfile = datafile.substring(0, n) + ".html";
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
            HTMLHierarchy htmlHierarchy = new HTMLHierarchy(datafile);
            htmlHierarchy.run(pw, title, root);

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

    public static void main(String[] args) {
		String datafile = args[0];
		String title = args[1];
		String root = args[2];
		HTMLHierarchy.run(datafile, title, root);
	}
}