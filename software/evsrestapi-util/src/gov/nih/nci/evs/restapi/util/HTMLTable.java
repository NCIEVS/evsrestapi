package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.util.*;

public class HTMLTable {

	public static void printBanner(PrintWriter out) {
		out.println("<div>");
		out.println("  <img");
		out.println("      src=\"https://nciterms.nci.nih.gov/ncitbrowser/images/evs-logo-swapped.gif\"");
		out.println("      alt=\"EVS Logo\"");
		out.println("      width=\"100%\"");
		out.println("      height=\"26\"");
		out.println("      border=\"0\"");
		out.println("      usemap=\"#external-evs\"");
		out.println("  />");
		out.println("  <map id=\"external-evs\" name=\"external-evs\">");
		out.println("    <area");
		out.println("        shape=\"rect\"");
		out.println("        coords=\"0,0,140,26\"");
		out.println("        href=\"/ncitbrowser/start.jsf\"");
		out.println("        target=\"_self\"");
		out.println("        alt=\"NCI Term Browser\"");
		out.println("    />");
		out.println("    <area");
		out.println("        shape=\"rect\"");
		out.println("        coords=\"520,0,941,26\"");
		out.println("        href=\"http://evs.nci.nih.gov/\"");
		out.println("        target=\"_blank\"");
		out.println("        alt=\"Enterprise Vocabulary Services\"");
		out.println("    />");
		out.println("  </map>");
		out.println("</div>");
	}

    public static void printHeader(PrintWriter out, String pageTitle) {
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
		out.println("<html xmlns:c=\"http://java.sun.com/jsp/jstl/core\">");
		out.println("<head>");
		out.println("<title>NCIt Properties and Relationships</title>");
		out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
		out.println("<style>");
		out.println("table {");
		out.println("    border-collapse: collapse;");
		out.println("}");
		out.println("table, td, th {");
		out.println("    border: 1px solid black;");
		out.println("}");
		out.println("</style>");
		out.println("</head>");
		out.println("");
		out.println("<body>");
		out.println("");
		out.println("");
		out.println(" <!-- nci banner<div class=\"ncibanner\"> -->");
		out.println("<div style='clear:both;margin-top:-5px;padding:8px;height:32px;color:white;background-color:#C31F40'>");
		out.println("  <a href=\"http://www.cancer.gov\" target=\"_blank\">");
		out.println("    <img");
		out.println("        src=\"https://nciterms.nci.nih.gov/ncitbrowser/images/banner-red.png\"");
		//out.println("        src=\"https://nciterms.nci.nih.gov/ncitbrowser/images/nci-banner-1.gif\"");
		out.println("        width=\"955\"");
		out.println("        height=\"39\"");
		out.println("        border=\"0\"");
		out.println("        alt=\"National Cancer Institute\"");
		out.println("    />");
		out.println("  </a>");
		out.println("</div>");
		out.println("<!-- end nci banner -->");
		out.println("");

		printBanner(out);

		out.println("");
		out.println("");
		out.println("<center>");
		out.println("<h1>" + pageTitle + "</h1>");
		out.println("<p>");
		String today = StringUtils.getToday("MM-dd-yyyy");
		out.println("<h2>" + today + "</h2>");
		out.println("</p>");
		out.println("</center>");
	}

    public static void printTable(PrintWriter out,
        String tableLabel,
        Vector th_vec,
        Vector data_vec) {
		out.println("");
		out.println("<div>");
		out.println("<center>");
		out.println("<h2>" + tableLabel + "</h2>");
		out.println("<table>");
        out.println("<tr>");
		for (int i=0; i<th_vec.size(); i++) {
			String th = (String) th_vec.elementAt(i);
			out.println("<th>");
			out.println(th);
			out.println("</th>");
		}
		out.println("</tr>");

		for (int i=0; i<data_vec.size(); i++) {
			String data = (String) data_vec.elementAt(i);
			out.println("<tr>");
			Vector u = StringUtils.parseData(data, '|');
			for (int j=0; j<u.size(); j++) {
				String value = (String) u.elementAt(j);
				out.println("<td>");
				out.println(value);
				out.println("</td>");
			}
			out.println("</tr>");
		}
		out.println("</table>");
		out.println("</div>");
		out.println("");
	}

    public static void printFooter(PrintWriter out) {
		out.println("</div>");
		out.println("");
		out.println("");
		out.println("</center>");
		out.println("</body>");
		out.println("</html>");
    }

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
	    OWLSPARQLUtils test = new OWLSPARQLUtils(serviceUrl, username, password);
        test.set_named_graph(namedGraph);

        String pageTitle = "NCIt_Properties_and_Relationships";
        String outputfile = pageTitle + ".html";
        pageTitle = pageTitle.replace("_", " ");
        long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		Vector th_vec = new Vector();
		th_vec.add("Name");
		th_vec.add("Code");
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
            printHeader(pw, pageTitle);
			Vector v = test.getSupportedProperties(namedGraph);
			String tableLabel = "Supported Properties";
            printTable(pw, tableLabel, th_vec, v);

	        v = test.getSupportedRoles(namedGraph);
 			tableLabel = "Supported Roles";
            printTable(pw, tableLabel, th_vec, v);

	        v = test.getSupportedAssociations(namedGraph);
 			tableLabel = "Supported Associations";
            printTable(pw, tableLabel, th_vec, v);

            th_vec = new Vector();
			th_vec.add("Property Name");
			th_vec.add("Property Code");
			th_vec.add("Qualifier Name");
			th_vec.add("Qualifier Code");

	        v = test.getSupportedPropertyQualifiers(namedGraph);
 			tableLabel = "Supported Property Qualifiers";
            printTable(pw, tableLabel, th_vec, v);

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