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
    OWLSPARQLUtils owlSPARQLUtils = null;
    HierarchyHelper hh = null;
    Vector main_types = null;
    String named_graph = null;

    public TreeTraversalUtils(Vector parent_child_vec, Vector main_types) {
		this.main_types = main_types;
		this.hh = new HierarchyHelper(parent_child_vec);
    }

    public TreeTraversalUtils(String serviceUrl, String named_graph, String username, String password, Vector parent_child_vec, Vector main_types) {
		this.main_types = main_types;
		this.hh = new HierarchyHelper(parent_child_vec);

		this.named_graph = named_graph;
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		if (named_graph != null) {
			owlSPARQLUtils.set_named_graph(named_graph);
		}
	}

	public Vector formatRoles(String label, String code, Vector roles, boolean outbound) {
		Vector v = new Vector();
		for (int i=0; i<roles.size(); i++) {
			String line = (String) roles.elementAt(i);

			if (outbound) {
				line = label + "|" + code + "|" + line;
			} else {
				//Stage I Choroidal and Ciliary Body Melanoma AJCC v8|C140660|Disease_Has_Primary_Anatomic_Site
				//System.out.println(line);
				line = line + "|" + label + "|" + code;
			}
			line = StringUtils.formatAssociation(line);
			v.add(line);
		}
		return new SortUtils().quickSort(v);
	}


	public Vector getRolesByCode(String code) {
		String label = hh.getLabel(code);
		Vector w = new  Vector();
		boolean outbound = true;
		Vector v = owlSPARQLUtils.getInboundRolesByCode(named_graph, code);
		v = new ParserUtils().getResponseValues(v);
		if (v != null && v.size() > 0) {
			v = formatRoles(label, code, v, false);
			v = new SortUtils().quickSort(v);
			w.addAll(v);
		}

		v = owlSPARQLUtils.getOutboundRolesByCode(named_graph, code);
		v = new ParserUtils().getResponseValues(v);
		if (v != null && v.size() > 0) {
			v = formatRoles(label, code, v, true);
			v = new SortUtils().quickSort(v);
			w.addAll(v);
		}
		return w;
	}

	public void dumpVector(String indentation, Vector v) {
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			t = indentation + t;
			System.out.println(t);
		}
	}


    public String getIndentation(int level) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<level; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}

/*
	public Vector getLocality(String code) {
		Vector w = new  Vector();
		boolean outbound = true;
		Vector v = owlSPARQLUtils.getAssociatedConcepts(named_graph, code, null, outbound);
		v = new ParserUtils().getResponseValues(v);
		if (v != null && v.size() > 0) {
			w.addAll(v);
		}
		outbound = false;
		v = owlSPARQLUtils.getAssociatedConcepts(named_graph, code, null, outbound);
		v = new ParserUtils().getResponseValues(v);
		if (v != null && v.size() > 0) {
			w.addAll(v);
		}
		return w;
	}
*/

	public void run(String root) {
		run(root, false);
	}

    public void run(String root, boolean show_locality) {
		Stack stack = new Stack();
		int level = 0;
		stack.push(root + "|" + level);
		while (!stack.isEmpty()) {
			String line = (String) stack.pop();
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			String level_str = (String) u.elementAt(1);
			int next_level = Integer.parseInt(level_str);
			String label = hh.getLabel(code);
			String indentation = getIndentation(next_level);
			if (main_types != null && main_types.contains(code)) {
				System.out.println(indentation + label + " (" + code + ") (*)");
			} else {
				System.out.println(indentation + label + " (" + code + ")");
			}

			if (show_locality) {
				Vector v = getRolesByCode(code);
                dumpVector(indentation, v);
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
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];

		String parent_child_file = args[4];
		Vector main_types = null;

		String code = args[5];
		Vector parent_child_vec = Utils.readFile(parent_child_file);
		//Vector main_types = Utils.readFile("main_types.txt");

		new TreeTraversalUtils(serviceUrl, named_graph, username, password, parent_child_vec, main_types).run(code, true);

		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

