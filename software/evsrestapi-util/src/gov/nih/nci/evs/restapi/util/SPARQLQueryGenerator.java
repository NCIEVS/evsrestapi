package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;
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
import java.text.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2020 MSC. This software was developed in conjunction
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
 *      "This product includes software developed by MSC and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "MSC" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or MSC
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      MSC, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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
 *     Initial implementation kim.ong@nih.gov
 *
 */


public class SPARQLQueryGenerator {
    private OWLSPARQLUtils owlSPARQLUtils = null;
	String serviceUrl = null;
	String namedGraph = null;
	String username = null;
	String password = null;

    static String returnChar = "\\n";

    public SPARQLQueryGenerator(String serviceUrl, String namedGraph, String username, String password) {
        owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        owlSPARQLUtils.set_named_graph(namedGraph);
    }

    public static String bufAppend(String str) {
        return "buf.append(\"" + str + "\"" + ").append(" + "\"" + returnChar + "\")" + ";";
    }

    public static Vector bufAppend(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String str = (String) v.elementAt(i);
			w.add(bufAppend(str));
		}
		return w;
	}

	public static void dumpVector(String methodSignature, Vector v) {
		System.out.println(methodSignature);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String str = (String) v.elementAt(i);
			System.out.println(str);
		}
	}

	public static Vector createConstrcQueryMethod(String methodSignature, Vector v) {
		Vector w = new Vector();
	    w.add("public String " + methodSignature + " {");
		w.add("\tString prefixes = owlSPARQLUtils.getPrefixes();");
		w.add("\tStringBuffer buf = new StringBuffer();");
		w.add("\tbuf.append(prefixes);");
		for (int i=0; i<v.size(); i++) {
			String str = (String) v.elementAt(i);

//buf.append("            ?a  :NHC0 ""A8""^^xsd:string .").append("\n");

			str = str.replace("\"", "\\\"");

			String t = str.toUpperCase();
			t = t.trim();

			if (!t.startsWith("PREFIX")) {
				w.add("\t" + bufAppend(str));
		    }
		}
		w.add("\treturn buf.toString();");
		w.add("}");
		return w;
	}

	public static Vector createQueryMethod(String constructQueryMethodSignature) {
		Vector w = new Vector();
		int n = constructQueryMethodSignature.lastIndexOf("(");
		String s1 = constructQueryMethodSignature.substring(0, n);
		Vector u = StringUtils.parseData(s1, '_');
		StringBuffer buf = new StringBuffer();
		String t = (String) u.elementAt(1);
		buf.append(t);
		for (int k=2; k<u.size(); k++) {
			t = (String) u.elementAt(k);
			String firstCh = t.substring(0, 1);
			firstCh = firstCh.toUpperCase();
			t = firstCh + t.substring(1, t.length());
			buf.append(t);
		}
		String methodSignature = buf.toString();
		String s2 = constructQueryMethodSignature.substring(n, constructQueryMethodSignature.length());
		w.add("public Vector " +  methodSignature + s2 + " {");
		constructQueryMethodSignature = constructQueryMethodSignature.replace("String ", "");

		w.add("\tString query = " + constructQueryMethodSignature + ";");
		w.add("\tVector v = owlSPARQLUtils.executeQuery(query);");
		w.add("\tif (v == null) return null;");
		w.add("\tif (v.size() == 0) return v;");
		w.add("\tv = new ParserUtils().getResponseValues(v);");
		w.add("\treturn new SortUtils().quickSort(v);");
		w.add("}");
		return w;
	}

    public static void generateCode(String queryfile, String methodSignature) {
		Vector v = Utils.readFile(queryfile);
		//String methodSignature = "construct_get_inbound_roles_by_code(String named_graph, String code)";
		dumpVector("\n", createConstrcQueryMethod(methodSignature, v));
		dumpVector("\n", createQueryMethod(methodSignature));
        Vector w = findHardCodedVariables(v);
		System.out.println("\nReminder: Need to substitute hard-coded variables.");
		dumpVector("Variables:", w);
	}

    public static Vector findHardCodedVariables(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.indexOf("^^") != -1 || t.indexOf("graph") != -1) {
				w.add(t);
			}
		}
		return w;
	}

	public static void test(String[] args) {
		long ms = System.currentTimeMillis();
		String restURL = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		String queryfile = args[4];
		System.out.println(queryfile);

		HTTPUtils util = new HTTPUtils();
		String query = util.loadQuery(queryfile, false);
		System.out.println(query);
		boolean parsevalues = true;
		Vector w = util.execute(restURL, username, password, query, parsevalues);
		Utils.saveToFile("ressults_" + queryfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));

	}


    public static void test2(String[] args) {
		long ms = System.currentTimeMillis();
        String owlfile = args[0];
        String type = args[1];
		OWLScanner scanner = new OWLScanner(owlfile);
		int n = owlfile.lastIndexOf(".");
		String outputfile = owlfile.substring(0, n) + "_" + StringUtils.getToday() + ".owl";
		Vector v = scanner.scanOwlTags();

		if (type.compareToIgnoreCase("roles") == 0) {
			v = scanner.fileterTagData(v, "owl:Restriction");
			for (int i=0; i<v.size()/2; i++) {
				int i1 = i+1;
				int j = i*2;
				String path = (String) v.elementAt(j);
				System.out.println("/*");
				System.out.println("\n(" + i1 + ") " + path);
				System.out.println("\n");
				scanner.printPath(path);
                System.out.println("*/");
				scanner.print_restriction_query(path, i1);
			}
		} else {
			Vector w = new Vector();
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				if (t.endsWith("|owl:Class")) {
					w.add(t);
				}
			}
			Utils.saveToFile("superclass_scanOwlTags", w);
			v = w;
			for (int i=0; i<v.size(); i++) {
				int i1 = i+1;
				String path = (String) v.elementAt(i);
				System.out.println("/*");
				System.out.println("\n(" + i1 + ") " + path);
				System.out.println("\n");
				scanner.printPath(path);
				System.out.println("*/");
				scanner.print_superclass_query(path, i1);
			}
		}
	}


    public static void main(String[] args) {
		String queryfile = args[0];
		System.out.println(queryfile);
		int n = queryfile.lastIndexOf("_query");
		if(n ==  -1) {
			System.out.println("queryfile: " + queryfile);
			System.out.println("ERROR: Incorrect naming of query file (example: sparql_query.txt.)");
			System.exit(1);
		}
		String t = queryfile.substring(0, n);
		String methodSignature = "construct_get_" + t + "(String named_graph)";
        generateCode(queryfile, methodSignature);
	}
}

