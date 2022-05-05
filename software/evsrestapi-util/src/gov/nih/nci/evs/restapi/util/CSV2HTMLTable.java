package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.util.*;

public class CSV2HTMLTable {

    public static Vector addHyperlinks(String datafile, int columnIndex, String url) {
		Vector w = new Vector();
		Vector v = Utils.readFile(datafile);
		w.add((String) v.elementAt(0));

		for (int i=1; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			StringBuffer buf = new StringBuffer();
			for (int j=0; j<u.size(); j++) {
				String value = (String) u.elementAt(j);
				if (j == columnIndex) {
					value = ExternalLinkPageGenerator.hyperlink(url, value);
				}
				buf.append(value).append("|");
			}
			String s = buf.toString();
			s = s.substring(0, s.length()-1);
			w.add(s);
		}
		return w;
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		String csvfile = args[4];

		boolean skip_heading = false;
		String delim = "|";
        Vector v = CSVFileReader.readCSV(csvfile, skip_heading, delim);
        int n = csvfile.lastIndexOf(".");
        String inputfile = csvfile.substring(0, n) + ".txt";

		if (args.length > 5) {
			String columnIndexStr = args[5];
			int columnIndex = Integer.parseInt(columnIndexStr);
			String url = args[6];
			Vector w = addHyperlinks(inputfile, columnIndex, url);
			Utils.saveToFile(inputfile, w);
		}

        Utils.saveToFile(inputfile, v);
		String outputfile = new HTMLTableDataConverter(serviceUrl, named_graph, username, password).convert(inputfile);
		System.out.println(outputfile + " generated.");
		v = Utils.readFile(outputfile);
		outputfile = new HTMLTable().generate(v);
		System.out.println(outputfile + " generated.");
	}
}