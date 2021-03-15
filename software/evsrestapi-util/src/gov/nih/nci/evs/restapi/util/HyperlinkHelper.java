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

	public static void main(String[] args) {
		String code = "C12345";
		System.out.println(toHyperlink(code));
	}
}