package gov.nih.nci.evs.restapi.util.graph;

import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.util.*;
import java.text.DecimalFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;


public class GraphGenerator {

	static Vector COLORS = null;
	static {
		COLORS = new Vector();
		COLORS.add("black");
		COLORS.add("blue");
		COLORS.add("blueviolet");
		COLORS.add("brown");
		COLORS.add("brown4");
		COLORS.add("burlywood");
		COLORS.add("cadetblue4");
		COLORS.add("chocolate4");
		COLORS.add("coral3");
		COLORS.add("cyan4");
		COLORS.add("crimson");
		COLORS.add("darkgreen");
		COLORS.add("darkolivegreen");
		COLORS.add("darkgoldenrod");
		COLORS.add("darkorchid");
		COLORS.add("darklategray");
		COLORS.add("darkviolet");
		COLORS.add("deeppink3");
		COLORS.add("firebrick2");
		COLORS.add("forestgreen");
		COLORS.add("gold");
		COLORS.add("grey6");
		COLORS.add("green3");
		COLORS.add("green4");
		COLORS.add("lawngreen");
		COLORS.add("lightblue");
		COLORS.add("lightslateblue");
		COLORS.add("limegreen");
		COLORS.add("maroon");
		COLORS.add("magenta");
		COLORS.add("maroon");
		COLORS.add("midnightblue");
		COLORS.add("navyblue");
		COLORS.add("orange");
		COLORS.add("orangered");
		COLORS.add("orangered4");
		COLORS.add("orchid");
		COLORS.add("palevioletred");
		COLORS.add("pink");
		COLORS.add("purple");
		COLORS.add("red");
		COLORS.add("saddlebrown");
		COLORS.add("sienna");
		COLORS.add("slateblue");
		COLORS.add("snow");
		COLORS.add("springgreen");
		COLORS.add("tan");
		COLORS.add("turquoise3");
		COLORS.add("tomato");
		COLORS.add("violet");
		COLORS.add("wheat");
		COLORS.add("yellowgreen");
	}

    public GraphGenerator() {

	}

	public static String generateColor() {
		int n = new RandomVariateGenerator().uniform(0, COLORS.size()-1);
		return (String) COLORS.elementAt(n);
	}

	public static void createDotGraph(String dotFormat,String fileName, String type)
	{
	    GraphViz gv=new GraphViz();
	    gv.addln(gv.start_graph());
	    gv.add(dotFormat);
	    gv.addln(gv.end_graph());
	    gv.decreaseDpi();
	    gv.decreaseDpi();
	    String fName = fileName+"."+ type;
	    System.out.println("fName: " + fName);
	    File out = new File(fName);
	    gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );
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

	public static String toString(Vector v) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			t = t.trim();
			buf.append(t).append(" ");
		}
		String s = buf.toString();
		return s;
	}

/*
p_label|literal|Gene_Is_Element_In_Pathway
p_domain_label|literal|Gene
p_range_label|literal|Biochemical Pathway
*/
	public static Vector getNodes(String filename) {
		//graviz_tbox_data.txt
		Vector w = new Vector();
		Vector v = readFile(filename);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.startsWith("p_domain_label") || t.startsWith("p_range_label")) {
				int n = t.lastIndexOf("|");
				String entity = t.substring(n+1, t.length());
				if (!w.contains(entity)) {
					w.add(entity);
				}
			}
		}
		w = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(w);
		return w;
	}

	public static Vector getEdgeLabels(String filename) {
		//graviz_tbox_data.txt
		Vector w = new Vector();
		Vector v = readFile(filename);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.startsWith("p_label")) {
				int n = t.lastIndexOf("|");
				String entity = t.substring(n+1, t.length());
				if (!w.contains(entity)) {
					w.add(entity);
				}
			}
		}
		w = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(w);
		return w;
	}

	public static Vector getEdges(String filename) {
		//graviz_tbox_data.txt
		Vector w = new Vector();
		Vector v = readFile(filename);
		String role = null;
		String domain = null;
		String range = null;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.startsWith("p_label")) {
				int n = t.lastIndexOf("|");
				role = t.substring(n+1, t.length());
			} if (t.startsWith("p_domain_label")) {
				int n = t.lastIndexOf("|");
				domain = t.substring(n+1, t.length());
			} if (t.startsWith("p_range_label")) {
				int n = t.lastIndexOf("|");
				range = t.substring(n+1, t.length());
				w.add(role + "|" + domain + "|" + range);
			}
		}
		w = new gov.nih.nci.evs.restapi.util.SortUtils().quickSort(w);
		return w;
	}



    public static Vector<String> parseData(String line) {
		if (line == null) return null;
        String tab = "|";
        return parseData(line, tab);
    }

    public static Vector<String> parseData(String line, String tab) {
		if (line == null) return null;
        Vector data_vec = new Vector();
        StringTokenizer st = new StringTokenizer(line, tab);
        while (st.hasMoreTokens()) {
            String value = st.nextToken();
            if (value.compareTo("null") == 0)
                value = "";
            data_vec.add(value);
        }
        return data_vec;
    }

    public static void generateGraph(PrintWriter pw, String inputfile, Vector node_vec) {
		HashMap label2IdMap = new HashMap();
		HashMap id2LabelMap = new HashMap();
		HashMap id2ColorMap = new HashMap();
		Vector w = getNodes(inputfile);
		int knt = 0;
		for (int i=0; i<w.size(); i++) {
			knt++;
			String label = (String) w.elementAt(i);
			String id = "node_" + knt;
			label2IdMap.put(label, id);
			id2LabelMap.put(id, label);
			String color = generateColor();

			if (node_vec == null) {
				pw.println(id + " [label=\"" + label + "\", fontcolor=white, shape=box, style=filled, fillcolor=" + color + "];");
			} else {
				if (node_vec.contains(label)) {
					pw.println(id + " [label=\"" + label + "\", fontcolor=white, shape=box, style=filled, fillcolor=" + color + "];");
				}
			}
			id2ColorMap.put(id, color);
		}

		w = getEdges(inputfile);
		HashMap nodePair2EdgeMap = new HashMap();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = parseData(t);
			String role = (String) u.elementAt(0);
			String domain = (String) u.elementAt(1);
			String range = (String) u.elementAt(2);
			String domain_id = (String) label2IdMap.get(domain);
			String range_id = (String) label2IdMap.get(range);
			String key = domain_id + "|" + range_id;
			String roles = (String) nodePair2EdgeMap.get(key);
			if (roles == null) {
				roles = role;
			} else {
				roles = roles + "\\n" + role;
			}
			nodePair2EdgeMap.put(key, roles);
		}

		Iterator it = nodePair2EdgeMap.keySet().iterator();
		while (it.hasNext()) {
			String nodePair = (String) it.next();
			Vector u = parseData(nodePair);
			String domain_id = (String) u.elementAt(0);
			String range_id = (String) u.elementAt(1);
			String color = (String) id2ColorMap.get(domain_id);
			String role = (String) nodePair2EdgeMap.get(nodePair);

			String domain_label = (String) id2LabelMap.get(domain_id);
			String range_label = (String) id2LabelMap.get(range_id);
			if (node_vec == null) {
				pw.println(domain_id + " -> " + range_id
				   + " [label=" + "\"" + role + "\"" + " fontcolor=" + color + ", color=" + color + "];");
			} else {
				if (node_vec.contains(domain_label) && node_vec.contains(range_label)) {
					pw.println(domain_id + " -> " + range_id
					   + " [label=" + "\"" + role + "\"" + " fontcolor=" + color + ", color=" + color + "];");
				}
			}
		}
    }

    public static void generateGraph(PrintWriter pw, Vector nodes, Vector edges, Vector node_vec) {
		HashMap label2IdMap = new HashMap();
		HashMap id2LabelMap = new HashMap();
		HashMap id2ColorMap = new HashMap();
		Vector w = nodes;
		int knt = 0;
		for (int i=0; i<w.size(); i++) {
			knt++;
			String label = (String) w.elementAt(i);
			String id = "node_" + knt;
			label2IdMap.put(label, id);
			id2LabelMap.put(id, label);
			String color = generateColor();

			if (node_vec == null) {
				pw.println(id + " [label=\"" + label + "\", fontcolor=white, shape=box, style=filled, fillcolor=" + color + "];");
			} else {
				if (node_vec.contains(label)) {
					pw.println(id + " [label=\"" + label + "\", fontcolor=white, shape=box, style=filled, fillcolor=" + color + "];");
				}
			}
			id2ColorMap.put(id, color);
		}

		w = edges;
		HashMap nodePair2EdgeMap = new HashMap();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = parseData(t);
			String role = (String) u.elementAt(0);
			String domain = (String) u.elementAt(1);
			String range = (String) u.elementAt(2);
			String domain_id = (String) label2IdMap.get(domain);
			String range_id = (String) label2IdMap.get(range);
			//String color = (String) id2ColorMap.get(domain_id);
			String key = domain_id + "|" + range_id;
			String roles = (String) nodePair2EdgeMap.get(key);
			if (roles == null) {
				roles = role;
			} else {
				roles = roles + "\\n" + role;
			}
			nodePair2EdgeMap.put(key, roles);
		}

		Iterator it = nodePair2EdgeMap.keySet().iterator();
		while (it.hasNext()) {
			String nodePair = (String) it.next();
			Vector u = parseData(nodePair);
			String domain_id = (String) u.elementAt(0);
			String range_id = (String) u.elementAt(1);
			String color = (String) id2ColorMap.get(domain_id);
			String role = (String) nodePair2EdgeMap.get(nodePair);

			String domain_label = (String) id2LabelMap.get(domain_id);
			String range_label = (String) id2LabelMap.get(range_id);
			if (node_vec == null) {
				pw.println(domain_id + " -> " + range_id
				   + " [label=" + "\"" + role + "\"" + " fontcolor=" + color + ", color=" + color + "];");
			} else {
				if (node_vec.contains(domain_label) && node_vec.contains(range_label)) {
					pw.println(domain_id + " -> " + range_id
					   + " [label=" + "\"" + role + "\"" + " fontcolor=" + color + ", color=" + color + "];");
				}
			}
		}
    }


	public static void generateGraphvizDataFile(String inputfile, String outputfile) {
		generateGraphvizDataFile(inputfile, outputfile, null);
	}


	public static void generateGraphvizDataFile(String inputfile, String outputfile, String nodefile) {
         PrintWriter pw = null;
         Vector node_vec = null;
         if (nodefile != null) {
			 node_vec = readFile(nodefile);
		 }
         try {
 			pw = new PrintWriter(outputfile, "UTF-8");
			pw.println("ranksep = \"5.0\";");
			pw.println("ratio=fill;");
			pw.println("overlap=scale;");
			pw.println("concentrate=true;");
			pw.println("ratio=auto;");
			pw.println("overlap = false;");
			pw.println("splines = true;");
			pw.println("node [style=filled,color=lightblue,shape=box];");
			pw.println("style=filled;");
			pw.println("color=lightgrey;");
			pw.println("nodesep=1.0;");

            generateGraph(pw, inputfile, node_vec);
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

	public static void generateGraphvizDataFile(Vector nodes, Vector edges, Vector node_vec, String outputfile) {
         PrintWriter pw = null;
         try {
 			pw = new PrintWriter(outputfile, "UTF-8");
			pw.println("ranksep = \"5.0\";");
			pw.println("ratio=fill;");
			pw.println("overlap=scale;");
			pw.println("concentrate=true;");
			pw.println("ratio=auto;");
			pw.println("overlap = false;");
			pw.println("splines = true;");
			pw.println("node [style=filled,color=lightblue,shape=box];");
			pw.println("style=filled;");
			pw.println("color=lightgrey;");
			pw.println("nodesep=1.0;");

            generateGraph(pw, nodes, edges, node_vec);
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

//gif, svg, png
	public static void main(String[] args) throws Exception {
        boolean generate_graph_file = true;
		String tbox_file = "graphviz_tbox_data.txt";
		String datafile = args[0];
		String format = args[1];
		String subgraphfile = null;
		if (args.length > 2) {
			subgraphfile = args[2];
		}
		if (generate_graph_file) {
			generateGraphvizDataFile(tbox_file, datafile, subgraphfile);
		}

		String dotFormat = toString(readFile(datafile));
        int n = datafile.lastIndexOf(".");
        String outputfile = datafile.substring(0, n);
		System.out.println(dotFormat);
		createDotGraph(dotFormat, outputfile, format);
	}

}