package gov.nih.nci.evs.restapi.util;

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


public class DelimitedDataExtractor {

	public static Vector extract(String filename, String delim_columns, char delim) {
		Vector w = StringUtils.parseData(delim_columns, delim);
		Vector columns = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			columns.add(Integer.valueOf(Integer.parseInt(line)));
		}
		return extract(filename, columns, delim);
	}

	public static Vector extract(String filename, Vector<Integer> columns, char delim) {
		Vector w = Utils.readFile(filename);
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, delim);
			StringBuffer buf = new StringBuffer();
			for (int k=0; k<columns.size(); k++) {
				Integer column_obj = (Integer) columns.elementAt(k);
				int column_int = Integer.parseInt(column_obj.toString());
				String t = (String) u.elementAt(column_int);
				buf.append(t).append(delim);
			}
			String s = buf.toString();
			s = s.substring(0, s.length()-1);
			v.add(s);
		}
		return v;
	}

	public static Vector extract(Vector data_vec, String delim_columns, char delim) {
		Vector w = StringUtils.parseData(delim_columns, delim);
		Vector columns = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			columns.add(Integer.valueOf(Integer.parseInt(line)));
		}
		return extract(data_vec, columns, delim);
	}

	public static Vector extract(Vector w, Vector<Integer> columns, char delim) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, delim);
			StringBuffer buf = new StringBuffer();
			for (int k=0; k<columns.size(); k++) {
				Integer column_obj = (Integer) columns.elementAt(k);
				int column_int = Integer.parseInt(column_obj.toString());
				String t = (String) u.elementAt(column_int);
				buf.append(t).append(delim);
			}
			String s = buf.toString();
			s = s.substring(0, s.length()-1);
			v.add(s);
		}
		return v;
	}

	public static void main(String[] args) throws Exception {
		String serviceUrl = args[0];
		String named_graph = args[1];
		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("named_graph: " + named_graph);
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
        owlSPARQLUtils.set_named_graph(named_graph);
        Vector v = owlSPARQLUtils.getDomainAndRangeData(named_graph);
        Vector trimmed_v = extract(v, "2|0|4", '|');
        Utils.saveToFile("trimmed_domain_range.txt", trimmed_v);
	}
}