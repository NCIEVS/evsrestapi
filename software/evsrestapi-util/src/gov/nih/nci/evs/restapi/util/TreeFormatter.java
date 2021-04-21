package gov.nih.nci.evs.restapi.util;
import java.io.*;
import java.util.*;

public class TreeFormatter {

	HashMap code2NodeMap = new HashMap();
    HierarchyHelper hh = null;

	public TreeFormatter() {

	}

	public static String remoteTabs(String line) {
		while (line.startsWith("\t")) {
			line = line.substring(1, line.length());
		}
		return line;
	}

	public static String toDelimited(String line) {
		line = remoteTabs(line);
		int n = line.indexOf("(");
		String label = line.substring(0, n-1);
		String code = line.substring(n+1, line.length()-1);
		String t = label + "|" + code;
		return t;
	}


	public static void flattenASCIITree(String asciitree) {
		Vector v = Utils.readFile(asciitree);
		int maxDepth = -1;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			int n = 0;
			while (line.startsWith("\t")) {
				n++;
				line = line.substring(1, line.length());
			}
			if (n > maxDepth) {
				maxDepth = n;
			}
		}
		List<String> list = new ArrayList<String>();
		for (int i=0; i<=maxDepth; i++) {
			list.add("");
		}
        Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.length() > 0) {
				int n = 0;
				while (line.startsWith("\t")) {
					n++;
					line = line.substring(1, line.length());
				}
				line = toDelimited(line);
				list.set(n, line);
				if (n > 0) {
					String sup = list.get(n-1);
					w.add(sup + "|" + line);
				}
	    	}
		}
        Utils.saveToFile("parent_child_" + asciitree, w);
	}

	public static void main(String[] args) {
		String asciitree = args[0];
		flattenASCIITree(asciitree);
	}

}

