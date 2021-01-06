package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.util.*;

public class HTMLTableDataConverter {
	String serviceUrl = null;
	String named_graph = null;
	String username = null;
	String password = null;
	String ncit_version = null;

	static String NC_THESAURUS = "NCI_Thesaurus";
	MetadataUtils metadataUtils = null;

	public HTMLTableDataConverter() {

	}

	public HTMLTableDataConverter(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.username = username;
		this.password = password;
		metadataUtils = new MetadataUtils(serviceUrl, username, password);
		this.ncit_version = metadataUtils.getVocabularyVersion(named_graph);
		System.out.println(this.ncit_version);
	}

	public static boolean isCode(String t) {
		if (t == null) return false;
		if (t.length() == 0) return false;
		char c = t.charAt(0);
		if (c != 'C') return false;
		String s = t.substring(1, t.length());
		try {
			int value = Integer.parseInt(s);
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	public void convert(String inputfile, String outputfile, String title, String table,
	    Vector th_vec) {
		Vector v = Utils.readFile(inputfile);

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			pw.println("<title>" + title);
			pw.println("<table>" + table);
			for (int i=0; i<th_vec.size(); i++) {
				String th = (String) th_vec.elementAt(i);
				pw.println("<th>" + th);
			}
			pw.println("<data>");
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				Vector u = StringUtils.parseData(t, '|');
				StringBuffer buf = new StringBuffer();
				for (int j=0; j<u.size(); j++) {
				    String s = (String) u.elementAt(j);
				    boolean bool = isCode(s);
				    if (bool) {
						s = HyperlinkHelper.toHyperlink(s);
					}
					buf = buf.append(s);
					if (j < u.size()-1) {
						buf.append("|");
					}
				}
				pw.println(buf.toString());
			}
			pw.println("</data>");
			pw.println("</table>");
			pw.println("<footer>(Source; NCI Thesaurus, version " + ncit_version + ")");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				pw.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void convert(String inputfile) {
		int n = inputfile.lastIndexOf(".");
		String title = inputfile.substring(0, n);
		String table = title;
		Vector v = Utils.readFile(inputfile);
		String firstLine = (String) v.elementAt(0);
		Vector u = StringUtils.parseData(firstLine, '|');
		int numberOfColumns = u.size();
		Vector th_vec = new Vector();
		for (int i=0; i<numberOfColumns; i++) {
			th_vec.add("Column " + i);
		}
	    convert(inputfile, "tabledata_" + inputfile,
			title, table,
			th_vec);
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		String inputfile = args[4];
		new HTMLTableDataConverter(serviceUrl, named_graph, username, password).convert(inputfile);
	}
}