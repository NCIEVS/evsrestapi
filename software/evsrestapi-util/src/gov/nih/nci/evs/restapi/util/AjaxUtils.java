package gov.nih.nci.evs.restapi.util;
import java.io.*;
import java.text.*;
import java.util.*;

public class AjaxUtils {

	public AjaxUtils() {

	}

	//https://nciterms65.nci.nih.gov/ncitbrowser/ajax?action=build_tree&ontology_display_name=NCI_Thesaurus
	public void run(PrintWriter out, String title, String url) {
		out.println("<!DOCTYPE html>");
		out.println("<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\">");
		out.println("<head>");
		out.println("    <meta charset=\"utf-8\" />");
		out.println("    <title>" + title + "</title>");
		out.println("    <script>");
		out.println("    function get() {");
		out.println("		var ajax = new XMLHttpRequest();");
		out.println("		ajax.open(\"GET\", \"" + url + "\", true);");
		out.println("		ajax.send();");
		out.println("		ajax.onreadystatechange = function() {");
		out.println("			if (ajax.readyState == 4 && ajax.status == 200) {");
		out.println("				var data = ajax.responseText;");
		out.println("				alert(data);");
		out.println("			}");
		out.println("		}");
		out.println("    }");
		out.println("    </script>");
		out.println("</head>");
		out.println("<body onload=\"javascript:get()\">");
		out.println("    <div id=\"" + title + "\"></div>");
		out.println("</body>");
		out.println("</html>");
	}

	public void run(String title, String url, String outputfile) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			run(pw, title, url);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
        long ms = System.currentTimeMillis();
        String title = args[0];
        String url = args[1];
        AjaxUtils ajaxUtils = new AjaxUtils();
        String outputfile = title + ".html";
        outputfile = outputfile.replace(" ", "_");
        ajaxUtils.run(title, url, outputfile);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

}