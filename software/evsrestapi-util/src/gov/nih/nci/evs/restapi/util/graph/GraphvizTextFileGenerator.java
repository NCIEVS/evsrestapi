package gov.nih.nci.evs.restapi.util.graph;

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


public class GraphvizTextFileGenerator {

    public GraphvizTextFileGenerator() {

	}

	public static Vector readFile(String filename)
	{
		return GraphGenerator.readFile(filename);
	}

    public void generateGraphvizTextFile(String relationshipFile, String outputfile) {
         PrintWriter pw = null;
         Vector rel_vec = readFile(relationshipFile);
         // graph is not tree
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

			HashSet hset = new HashSet();
			HashMap hmap = new HashMap();

			HashSet rel_hset = new HashSet();

			for (int i=0; i<rel_vec.size(); i++) {
				String t = (String) rel_vec.elementAt(i);
				Vector u = GraphGenerator.parseData(t);
				String label_1 = (String) u.elementAt(0);
				String code_1 = (String) u.elementAt(1);
				String rel = (String) u.elementAt(2);
				String label_2 = (String) u.elementAt(3);
				String code_2 = (String) u.elementAt(4);
				if (!hset.contains(label_1)) {
					hset.add(label_1);
					int node_id = hset.size();
					String color = GraphGenerator.generateColor();
					pw.println("node_" + node_id + "[label=\"" + label_1 + "\", fontcolor=white, shape=box, style=filled, fillcolor=" + color + "];");
					hmap.put(label_1, "node_" + node_id);
				}
				if (!hset.contains(label_2)) {
					hset.add(label_2);
					int node_id = hset.size();
					String color = GraphGenerator.generateColor();
					pw.println("node_" + node_id + "[label=\"" + label_2 + "\", fontcolor=white, shape=box, style=filled, fillcolor=" + color + "];");
					hmap.put(label_2, "node_" + node_id);
				}
			}

			for (int i=0; i<rel_vec.size(); i++) {
				String t = (String) rel_vec.elementAt(i);
				if (!rel_hset.contains(t)) {
					Vector u = GraphGenerator.parseData(t);
					String label_1 = (String) u.elementAt(0);
					String code_1 = (String) u.elementAt(1);
					String rel = (String) u.elementAt(2);
					String label_2 = (String) u.elementAt(3);
					String code_2 = (String) u.elementAt(4);
					String node_1 = (String) hmap.get(label_1);
					String node_2 = (String) hmap.get(label_2);
					String color = GraphGenerator.generateColor();
                    pw.println(node_1 + " -> " + node_2 + " [label=\"" + rel + "\" " + "fontcolor=" + color + ", color=" + color + "];");
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

	public static void main(String[] args) throws Exception {
		String relationship_file = args[0];
		String outputfile = args[1];
        new GraphvizTextFileGenerator().generateGraphvizTextFile(relationship_file, outputfile);
        String datafile = outputfile;
        String format = "png";
        GraphGenerator gg = new GraphGenerator();
		String dotFormat = gg.toString(gg.readFile(datafile));
        int n = datafile.lastIndexOf(".");
        outputfile = datafile.substring(0, n);
		gg.createDotGraph(dotFormat, outputfile, format);
	}



}