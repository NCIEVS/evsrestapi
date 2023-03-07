package gov.nih.nci.evs.restapi.util;
import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.Charset;

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@ngc.com
 *
 */

public class XMLEditor {

	public static void saveToFile(PrintWriter pw, Vector v) {
		if (v != null && v.size() > 0) {
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
		}
	}

	public static Vector readFile(String datafile) {
		Vector v = new Vector();
        try {
			File file = new File(datafile);
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedReader br = null;
			try {
				br = br = new BufferedReader(new InputStreamReader(bis));
			} catch (Exception ex) {
				return null;
			}

            while (true) {
                String line = br.readLine();
				if (line == null) {
					break;
				}
				v.add(line);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}

    public static void saveToFile(String outputfile, Vector v) {
        try {
            FileOutputStream output = new FileOutputStream(outputfile);
            for (int i=0; i<v.size(); i++) {
				String data = (String) v.elementAt(i);
				if (i < v.size()) {
					data = data + "\n";
				}
				byte[] array = data.getBytes();
				output.write(array);
			}
            output.close();
        } catch(Exception e) {
            e.getStackTrace();
        }
    }

	public static String extractTagValue(String line, String tag, String close_tag) {
		int n1 = line.indexOf(tag);
		int n2 = line.indexOf(close_tag);
		if (n1 == -1 || n2 == -1) return null;
		return line.substring(n1 + tag.length(), n2);
	}

    public static String getTag(String line) {
		String t = line;
		t = t.trim();
		if (t.startsWith("<") && t.endsWith(">")) {
			int n = t.indexOf(">");
			String s = t.substring(1, n);
			return s;
		}
		return null;
	}

	public static String replace(String t) {
		t = t.replace(">=", "&gt;=");
		t = t.replace("<=", "&lt;=");
		t = t.replace("<", "&lt;");
		t = t.replace(">", "&gt;");
		return t;
	}

	public static void run(String xmlfile, String outputfile) {
		Vector v = readFile(xmlfile);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			String s = getTag(line);
			String tag = "<" + s + ">";
			String close_tag = "</" + s + ">";
			if (s != null) {
				if (line.endsWith(close_tag)) {
					int indent = line.indexOf(tag);
					String value = extractTagValue(line, tag, close_tag);
					if (value != null) {
						w.add(line.substring(0, indent) + tag + replace(value) + close_tag);
					}
				} else {
					w.add(line);
				}
			} else {
				w.add(line);
			}
		}
		saveToFile(outputfile, w);
		System.out.println("Outputfile " + outputfile + " generated.");
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String xmlfile = args[0];
		String outputfile = args[1];
		run(xmlfile, outputfile);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

