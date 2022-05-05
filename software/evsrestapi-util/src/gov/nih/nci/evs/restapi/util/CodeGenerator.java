package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;


public class CodeGenerator {
    static String BASE_URL = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

    public CodeGenerator(String serviceUrl, String namedGraph, String username, String password) {
	}

    public static Vector generateCode(String queryfile) {
		int n = queryfile.lastIndexOf("_query");
		if(n ==  -1) {
			System.out.println("queryfile: " + queryfile);
			System.out.println("ERROR: Incorrect naming of query file (example: sparql_query.txt.)");
			return null;
		}
		String t = queryfile.substring(0, n);
		String methodSignature = "construct_get_" + t + "(String named_graph)";

		Vector v = Utils.readFile(queryfile);
		Vector w = new Vector();
		w.addAll(SPARQLQueryGenerator.createConstrcQueryMethod(methodSignature, v));
		w.add("\n");
		w.addAll(SPARQLQueryGenerator.createQueryMethod(methodSignature));
        //w.addAll(SPARQLQueryGenerator.findHardCodedVariables(v));
        return w;
	}


	public static void run(PrintWriter out, String queryfile, String className) {
		out.println("import gov.nih.nci.evs.restapi.util.*;");
		out.println("import gov.nih.nci.evs.restapi.bean.*;");
		out.println("import gov.nih.nci.evs.restapi.common.*;");
		out.println("");
		out.println("import java.io.*;");
		out.println("import java.io.BufferedReader;");
		out.println("import java.io.InputStream;");
		out.println("import java.io.InputStreamReader;");
		out.println("import java.net.*;");
		out.println("import java.net.HttpURLConnection;");
		out.println("import java.net.MalformedURLException;");
		out.println("import java.net.URL;");
		out.println("import java.net.URLConnection;");
		out.println("import java.net.URLEncoder;");
		out.println("import java.util.*;");
		out.println("import java.util.regex.*;");
		out.println("import org.json.*;");
		out.println("");
		out.println("/**");
		out.println(" * <!-- LICENSE_TEXT_START -->");
		out.println(" * Copyright 2008-2017 NGIS. This software was developed in conjunction");
		out.println(" * with the National Cancer Institute, and so to the extent government");
		out.println(" * employees are co-authors, any rights in such works shall be subject");
		out.println(" * to Title 17 of the United States Code, section 105.");
		out.println(" * Redistribution and use in source and binary forms, with or without");
		out.println(" * modification, are permitted provided that the following conditions");
		out.println(" * are met:");
		out.println(" *   1. Redistributions of source code must retain the above copyright");
		out.println(" *      notice, this list of conditions and the disclaimer of Article 3,");
		out.println(" *      below. Redistributions in binary form must reproduce the above");
		out.println(" *      copyright notice, this list of conditions and the following");
		out.println(" *      disclaimer in the documentation and/or other materials provided");
		out.println(" *      with the distribution.");
		out.println(" *   2. The end-user documentation included with the redistribution,");
		out.println(" *      if any, must include the following acknowledgment:");
		out.println(" *      \"This product includes software developed by NGIS and the National");
		out.println(" *      Cancer Institute.\"   If no such end-user documentation is to be");
		out.println(" *      included, this acknowledgment shall appear in the software itself,");
		out.println(" *      wherever such third-party acknowledgments normally appear.");
		out.println(" *   3. The names \"The National Cancer Institute\", \"NCI\" and \"NGIS\" must");
		out.println(" *      not be used to endorse or promote products derived from this software.");
		out.println(" *   4. This license does not authorize the incorporation of this software");
		out.println(" *      into any third party proprietary programs. This license does not");
		out.println(" *      authorize the recipient to use any trademarks owned by either NCI");
		out.println(" *      or NGIS");
		out.println(" *   5. THIS SOFTWARE IS PROVIDED \"AS IS,\" AND ANY EXPRESSED OR IMPLIED");
		out.println(" *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES");
		out.println(" *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE");
		out.println(" *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,");
		out.println(" *      NGIS, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,");
		out.println(" *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,");
		out.println(" *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;");
		out.println(" *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER");
		out.println(" *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT");
		out.println(" *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN");
		out.println(" *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
		out.println(" *      POSSIBILITY OF SUCH DAMAGE.");
		out.println(" * <!-- LICENSE_TEXT_END -->");
		out.println(" */");
		out.println("");
		out.println("/**");
		out.println(" * @author EVS Team");
		out.println(" * @version 1.0");
		out.println(" *");
		out.println(" * Modification history:");
		out.println(" *     Initial implementation kim.ong@ngc.com");
		out.println(" *");
		out.println(" */");
		out.println("");
		out.println("");
		out.println("public class " + className + " {");
		out.println("    JSONUtils jsonUtils = null;");
		out.println("    HTTPUtils httpUtils = null;");
		out.println("    String named_graph = null;");
		out.println("    String prefixes = null;");
		out.println("    String serviceUrl = null;");
		out.println("    String restURL = null;");
		out.println("    String named_graph_id = \":NHC0\";");
		out.println("    String BASE_URI = \"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl\";");
		out.println("");
		out.println("    ParserUtils parser = new ParserUtils();");
		out.println("    HashMap nameVersion2NamedGraphMap = null;");
		out.println("    HashMap ontologyUri2LabelMap = null;");
		out.println("    String version = null;");
		out.println("    String username = null;");
		out.println("    String password = null;");
		out.println("    OWLSPARQLUtils owlSPARQLUtils = null;");
		out.println("");
		out.println("    public " + className + "(String serviceUrl, String named_graph, String username, String password) {");
		out.println("		this.serviceUrl = serviceUrl;");
		out.println("    	this.named_graph = named_graph;");
		out.println("    	this.username = username;");
		out.println("    	this.password = password;");
		out.println("        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);");
		out.println("        this.owlSPARQLUtils.set_named_graph(named_graph);");
		out.println("    }");
		out.println("");
		out.println("    public OWLSPARQLUtils getOWLSPARQLUtils() {");
		out.println("		return this.owlSPARQLUtils;");
		out.println("	}");
        out.println("\n");

		Vector w = generateCode(queryfile);
		String invokingMethod = null;

		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			line = line.replace("namedGraph", "named_graph");
			line = line.replace(BASE_URL, "\" + named_graph + \"");
			out.println("\t" + line);

			if (line.indexOf("public Vector get") != -1) {
				invokingMethod = line;
				invokingMethod = invokingMethod.replace("public Vector", "Vector w = test.");
				invokingMethod = invokingMethod.replace("test. ", "test.");
				invokingMethod = invokingMethod.replace("(String ", "(");
				invokingMethod = invokingMethod.replace(" {", ";");
			}
		}

		out.println("");
		out.println("    public static void main(String[] args) {");
		out.println("		long ms = System.currentTimeMillis();");
		out.println("		String serviceUrl = args[0];");
		out.println("		String named_graph = args[1];");
		out.println("		String username = args[2];");
		out.println("		String password = args[3];");
		out.println("        " + className + " test = new " + className + "(serviceUrl, named_graph, username, password);");
		//out.println("        OWLSPARQLUtils owlSPARQLUtils = test.getOWLSPARQLUtils();");
		out.println("\t\t" + invokingMethod);
		out.println("\t\tUtils.saveToFile(\"results_" + queryfile + "\", w);");

		out.println("");
		out.println("    }");
		out.println("");
		out.println("}");
	}

    public static void main(String[] args) {
		String className = args[0];
		String queryfile = args[1];
		String outputfile = className + ".java";
		System.out.println(queryfile);
		int n = queryfile.lastIndexOf("_query");
		if(n ==  -1) {
			System.out.println("queryfile: " + queryfile);
			System.out.println("ERROR: Incorrect naming of query file (example: sparql_query.txt.)");
			System.exit(1);
		}

        long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			run(pw, queryfile, className);
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