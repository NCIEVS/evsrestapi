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
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;

public class TreeTraversalUtils {

    HierarchyHelper hh = null;
    Vector main_types = null;

    public TreeTraversalUtils(Vector parent_child_vec, Vector main_types) {
		this.main_types = main_types;
		this.hh = new HierarchyHelper(parent_child_vec);
    }

    String getIndentation(int level) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<level; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}

    public void run(String root) {
		Stack stack = new Stack();
		int level = 0;
		stack.push(root + "|" + level);
		while (!stack.isEmpty()) {
			String line = (String) stack.pop();
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			String level_str = (String) u.elementAt(1);
			int next_level = Integer.parseInt(level_str);
			if (main_types.contains(code)) {
				System.out.println(getIndentation(next_level) + hh.getLabel(code) + " (" + code + ") (*)");
			} else {
				System.out.println(getIndentation(next_level) + hh.getLabel(code) + " (" + code + ")");
			}
			next_level = next_level + 1;

			Vector superclass_codes = hh.getSuperclassCodes(code);
			if (superclass_codes != null) {
				for (int k=0; k<superclass_codes.size(); k++) {
					String child_code = (String) superclass_codes.elementAt(k);
					stack.push(child_code + "|" + next_level);
				}
			}
		}
	}


	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String parent_child_file = args[0];
		String code = args[1];
		Vector parent_child_vec = Utils.readFile(parent_child_file);
		Vector main_types = Utils.readFile("main_types.txt");
		new TreeTraversalUtils(parent_child_vec, main_types).run(code);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

