package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class RestrictionPageGenerator extends HttpServlet {
	String HYPERLINK = "https://nciterms65.nci.nih.gov/ncitbrowser/pages/concept_details.jsf?dictionary=NCI_Thesaurus&type=terminology&key=null&b=1&n=0&vse=null&code=";

	public RestrictionPageGenerator() {

	}

	public void setHYPERLINK(String HYPERLINK) {
		this.HYPERLINK = HYPERLINK;
	}

	public String getHyperLink(String code) {
		if (HYPERLINK != null) {
			return HYPERLINK + code;
		} else {
			return null;
		}
	}

	public void generate(String title, Vector restriction_vec, String outputfile) {
		long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			generate(title, pw, restriction_vec);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}


	public void generate(String title, PrintWriter out, Vector restriction_vec) {
		out.println("<!doctype html>");
		out.println("<html lang=\"en\">");
		out.println("<head>");
		out.println("	<meta charset=\"utf-8\">");
		out.println("	<title>Biomarker Hierarchy (Last updated: 09-07-2020)</title>");
		out.println("	<link rel=\"stylesheet\" href=\"tree.css\">");
		out.println("	<script type=\"text/javascript\" src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js\"></script>");
		out.println("	<script type=\"text/javascript\" src=\"js/jquery.sapling.min.js\"></script>");
		out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"css/tree.css\">");
		out.println("	<script type=\"text/javascript\">");
		out.println("		$(document).ready(function() {");
		out.println("			$('#demoList').sapling();");
		out.println("		});");
		out.println("	</script>");
		out.println("</head>");
		out.println("<body>");
		out.println("	<h3>" + title + " (Last updated: " + StringUtils.getToday() + ")</h3>");
		out.println("	<hr>");
		out.println("	<ul id=\"demoList\">");
		out.println("		<ul>");

		for (int i=0; i<restriction_vec.size(); i++) {
			String line = (String) restriction_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String source_label = (String) u.elementAt(0);
			String source_code = (String) u.elementAt(1);
			String restriction = (String) u.elementAt(2);
			String target_label = (String) u.elementAt(3);
			String target_code = (String) u.elementAt(4);

            String hyperlink1 = getHyperLink(source_code);
            String hyperlink2 = getHyperLink(target_code);
            out.println("<li>");
            out.println("[" + source_label + " (<a href=\"" + hyperlink1 + "\">" + source_code + "</a>" + ")] -- (" + restriction + ")"
				    + " --> [" + target_label + " (<a href=\"" + hyperlink2 + "\">" + target_code + "</a>" + ")] ");
    		out.println("</li>");
	    }

		out.println("		</ul>");
		out.println("	</li>");
		out.println("	</ul>");
		out.println("</body>");
		out.println("</html>");
	}

    public static void main(String[] args) {
		String inputfile = args[0];
		String outputfile = args[1];
		RestrictionPageGenerator generator = new RestrictionPageGenerator();
		long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			Vector restriction_vec = Utils.readFile(inputfile);
			generator.generate("Restriction", pw, restriction_vec);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

}