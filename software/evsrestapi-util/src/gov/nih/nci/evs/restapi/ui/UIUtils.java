package gov.nih.nci.evs.restapi.ui;

import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;
import gov.nih.nci.evs.restapi.util.*;

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.codec.binary.Base64;
import org.json.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2017 NGIS. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by NGIS and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIS" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIS
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIS, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@ngc.com
 *
 */

public class UIUtils {

    public static String getConceptReportHyperlink(String applicationName, String named_graph, String name, String code) {
		StringBuffer buf = new StringBuffer();
		buf.append("<a href=\"/" + applicationName + "/ConceptReport.jsp?ng=" + named_graph + "&code=" + code + "\">").append("\n");
		buf.append(name).append("\n");
		buf.append("</a>").append("\n");
		return buf.toString();
    }


//sparql/ConceptReport.jsp
    public static String getHyperlink(String named_graph, String name, String code) {
		return getConceptReportHyperlink("sparqlncim", named_graph, name, code);
    }

    public static String getHyperlink(String jsp, String named_graph, String name, String code) {
		StringBuffer buf = new StringBuffer();
		buf.append("<a href=\"/"+ jsp + "?ng=" + named_graph + "&code=" + code + "\">").append("\n");
		buf.append(name).append("\n");
		buf.append("</a>").append("\n");
		return buf.toString();
    }


    public static void generateTestPage(String outputfile, String serviceUrl) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			generateTestPage(pw, serviceUrl);

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

	public static String generateCartContent(String applicationName, Vector label_vec, Vector semtype_vec) {
		StringBuffer buf = new StringBuffer();
		buf.append("      <table class=\"datatable_960\" summary=\"\" cellpadding=\"3\" cellspacing=\"0\" border=\"0\" width=\"100%\">").append("\n");
		buf.append("            <tr>").append("\n");
		buf.append("              <th class=\"dataTableHeader\" scope=\"col\" align=\"left\" width=\"20px\">&nbsp;</th>").append("\n");
		buf.append("              <th class=\"dataTableHeader\" scope=\"col\" align=\"left\">Concept</th>").append("\n");
		buf.append("              <th class=\"dataTableHeader\" scope=\"col\" align=\"left\">Semantic Type</th>").append("\n");
		buf.append("            </tr>").append("\n");
		for (int i=0; i<label_vec.size(); i++ ) {
			String label = (String) label_vec.elementAt(i);
			String semtype = (String) semtype_vec.elementAt(i);
			buf.append("            <tr class=\"dataRowDark\">").append("\n");
			buf.append("              <td><input id=\"cartFormId:checkboxId\" type=\"checkbox\" name=\"cartFormId:checkboxId\" onclick=\"submit()\" /></td>").append("\n");
			buf.append("              <td>").append("\n");
			buf.append("                   <label for=\"cartFormId:checkboxId\">" + label + "</label>").append("\n");
			buf.append("              </td>").append("\n");
			buf.append("              <td>" + semtype + "</td>").append("\n");
			buf.append("            </tr>").append("\n");
		}		buf.append("").append("\n");
		buf.append("      </table>").append("\n");
		//buf.append("      <input type=\"hidden\" name=\"javax.faces.ViewState\" value=\"j_id7:j_id8\" />").append("\n");
		//buf.append("</form>").append("\n");
        return buf.toString();
	}


    public static void generateTestPage(PrintWriter out, String serviceUrl) {
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<script src=\"js/jquery-3.3.1.min.js\"></script>");
		out.println("");
		out.println("<script>");
		out.println("");
		out.println("var EVSRESTAPIConfig = {");
		out.println("    serviceUrl: \"" + serviceUrl + "\"");
		out.println("};");
		out.println("");
		out.println("function showServiceURL() {");
		out.println("    var url = EVSRESTAPIConfig.serviceUrl;");
		out.println("    div = document.getElementById(\"serviceUrl\");");
		out.println("    div.outerHTML = \"<p></p>Service URL: \" + url;");
		out.println("}");
		out.println("");
		out.println("function concept_details() {");
		out.println("    var url = EVSRESTAPIConfig.serviceUrl;");
		out.println("    var str = document.getElementById(\"code\").value;");
		out.println("    var code = str;");
		out.println("    var n = str.indexOf(\"(\");");
		out.println("    if (n != -1) {");
		out.println("        code = str.substring(n+1, str.length-1);");
		out.println("    }");
		out.println("    window.open(url +code+\"/\");");
		out.println("}");
		out.println("</script>");
		out.println("");
		out.println("</head>");
		out.println("<body onload=javascript:showServiceURL()>");
		out.println("");
		out.println("<div id=\"serviceUrl\">");
		out.println("    <pre></pre>");
		out.println("</div>");
		out.println("<p></p>");
		out.println("Code: <input id=\"code\"></input>");
		out.println("<button onclick=javascript:concept_details()>Submit</button>");
		out.println("");
		out.println("</body>");
		out.println("</html>");
		out.println("");
	}

	public static StringBuffer treeItem2StringBuffer(String applicationName, gov.nih.nci.evs.restapi.bean.TreeItem ti) {
        HashSet _vocabularyNameSet = null;
        gov.nih.nci.evs.restapi.util.SimpleTreeUtils stu = new gov.nih.nci.evs.restapi.util.SimpleTreeUtils(_vocabularyNameSet);
        stu.setBasePath("/" + applicationName + "/");
		HashMap sourceValueSetTree = new HashMap();
		sourceValueSetTree.put("<Root>", ti);
		return stu.getValueSetTreeStringBuffer(sourceValueSetTree);
	}

}