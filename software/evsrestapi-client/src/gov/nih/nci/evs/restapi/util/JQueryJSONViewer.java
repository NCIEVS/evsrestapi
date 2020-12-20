package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.util.*;

import java.io.*;
import java.text.*;
import java.util.*;


public class JQueryJSONViewer {

   public JQueryJSONViewer() {

   }

   public static String readJSONFromFile(String filename) {
	   Vector v = Utils.readFile(filename);
	   StringBuffer buf = new StringBuffer();
	   for (int i=0; i<v.size(); i++) {
		   String line = (String) v.elementAt(i);
		   line = line.trim();
		   buf.append(line);
	   }
	   return buf.toString();
   }

   public static void generate(PrintWriter out, String title, String json) {
		out.println("<!doctype HTML>");
		out.println("<html>");
		out.println("  <head>");
		out.println("    <title>" + title + "</title>");
		out.println("    <meta charset=\"utf-8\" />");
		out.println("");
		out.println("    <script src=\"http://code.jquery.com/jquery-2.1.3.min.js\"></script>");
		out.println("    <script src=\"js/jquery.json-viewer.js\"></script>");
		out.println("    <link href=\"css/jquery.json-viewer.css\" type=\"text/css\" rel=\"stylesheet\" />");
		out.println("");
		out.println("    <style type=\"text/css\">");
		out.println("body {");
		out.println("  margin: 0 100px;");
		out.println("  font-family: sans-serif;");
		out.println("}");
		out.println("textarea#json-input {");
		out.println("  width: 100%;");
		out.println("  height: 200px;");
		out.println("}");
		out.println("pre#json-renderer {");
		out.println("  border: 1px solid #aaa;");
		out.println("  padding: 0.5em 1.5em;");
		out.println("}");
		out.println("    </style>");
		out.println("");
		out.println("    <script>");
		out.println("$(function() {");
		out.println("  $('#btn-json-viewer').click(function() {");
		out.println("    try {");
		out.println("      var input = eval('(' + $('#json-input').val() + ')');");
		out.println("    }");
		out.println("    catch (error) {");
		out.println("      return alert(\"Cannot eval JSON: \" + error);");
		out.println("    }");
		out.println("    var options = {");
		//out.println("      collapsed: $('#collapsed').is(':checked')");
		out.println("      collapsed: $('#collapsed').is(':checked'),");
		out.println("      withQuotes: $('#with-quotes').is(':checked')");
		out.println("    };");
		out.println("    $('#json-renderer').jsonViewer(input, options);");
		out.println("  });");
		out.println("");
		out.println("  // Display JSON sample on load");
		out.println("  $('#btn-json-viewer').click();");
		out.println("});");
		out.println("    </script>");
		out.println("  </head>");
		out.println("  <body>");
		out.println("    <h1><center>" + title + "</center></h1>");
		out.println("    <textarea id=\"json-input\" autocomplete=\"off\">");
		out.println(json);
		out.println("</textarea>");
		out.println("    <p>");
		out.println("      Options:");
		out.println("      <label><input type=\"checkbox\" id=\"collapsed\" />Collapse nodes</label>");
		//out.println("      <label><input type=\"checkbox\" id=\"with-quotes\" />Keys with quotes</label>");
		out.println("    </p>");
		//out.println("    <button id=\"btn-json-viewer\" title=\"run jsonViewer()\">Transform to HTML</button>");
		out.println("    <button id=\"btn-json-viewer\" title=\"run jsonViewer()\">Execute</button>");
		out.println("    <pre id=\"json-renderer\"></pre>");
		out.println("  </body>");
		out.println("</html>");
    }

    public static void generate_html(String json, String title) {
		long ms = System.currentTimeMillis();
		String outputfile = title + ".html";
		outputfile = outputfile.replace(" ","_");
		System.out.println("Generating " + outputfile + " ...");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			generate(pw, title, json);

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


    public static void generateHTML(String jsonfile) {
		long ms = System.currentTimeMillis();
		String json = readJSONFromFile(jsonfile);
		int n = jsonfile.lastIndexOf(".");
		String title = jsonfile.substring(0, n);
		int m = title.lastIndexOf("\\");
		if (m == -1) {
			m = title.lastIndexOf("/");
		}
		if (m != -1) {
			title = title.substring(m+1, title.length());
		}
		String outputfile = jsonfile.substring(0, n) + ".html";
		System.out.println("Generating " + outputfile + " ...");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			generate(pw, title, json);

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

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String jsonfile = args[0];
		generateHTML(jsonfile);
	}
}