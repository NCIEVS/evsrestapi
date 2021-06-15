package gov.nih.nci.evs.restapi.util;
import java.io.*;
import java.util.*;
import java.util.stream.*;

import java.text.*;
import java.nio.file.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class Text2CSV {

	public static String toCSV(String textfile, char delim) {
		int n = textfile.lastIndexOf(".");
		String csvfile = textfile.substring(0, n) + ".csv";
		Vector lines = Utils.readFile(textfile);
		Vector w = new Vector();
		try {
			for (int k=1; k<lines.size(); k++) {
				String line = (String) lines.elementAt(k);
				StringBuffer buf = new StringBuffer();
				Vector u = StringUtils.parseData(line, delim);
				for (int i=0; i<u.size(); i++) {
					String t = (String) u.elementAt(i);
					buf.append("\"" + t + "\"");
					if (i <u.size()-1) {
						buf.append(",");
					}
				}
				w.add(buf.toString());
			}
			Utils.saveToFile(csvfile, w);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return csvfile;
	}

	public static void main(String[] args) throws Exception {
		String textfile = args[0];//"FDA_Approved_Drug_Data.txt";
		char delimiter = '\t';
		try {
			String csvfile = toCSV(textfile, delimiter);
			System.out.println(csvfile + " generated.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

