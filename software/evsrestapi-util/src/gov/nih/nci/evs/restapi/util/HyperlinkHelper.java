package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.util.*;

public class HyperlinkHelper {
	private static String HYPER_LINK = "https://nciterms65.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&code=";

	public static String toHyperlink(String code) {
		return toHyperlink(HYPER_LINK, code);
	}

	public static String toHyperlink(String hyperlink, String code) {
		StringBuffer buf = new StringBuffer();
		buf.append("<a ");
		buf.append("href=");
		buf.append("\"");
		buf.append(hyperlink + code);
		buf.append("\">");
		buf.append(code);
		buf.append("</a>");
		return buf.toString();
	}

	public static void addHyperlinks(String filename, String hyperlinkfile) {
	    Vector v = Utils.readFile(filename);
	    Vector v2 = Utils.readFile(hyperlinkfile);
	    String t = (String) v2.elementAt(0);
	    Vector hyperlinks = StringUtils.parseData(t, '|');
	    Vector w0 = new Vector();
	    w0.add((String) v.elementAt(0));
	    Vector w = new Vector();
	    for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			StringBuffer buf = new StringBuffer();
			Vector u = StringUtils.parseData(line, '|');
			for (int j=0; j<u.size(); j++) {
				buf.append(toHyperlink((String) hyperlinks.elementAt(j),
				                       (String) u.elementAt(j)));

				if (j < u.size()-1) {
				    buf.append("|");
				}
			}
			w.add(buf.toString());
		}
		w = new SortUtils().quickSort(w);
		w0.addAll(w);
		Utils.saveToFile(filename, w0);
	}


	public static void main(String[] args) {
		String code = "C12345";
		System.out.println(toHyperlink(code));
	}
}