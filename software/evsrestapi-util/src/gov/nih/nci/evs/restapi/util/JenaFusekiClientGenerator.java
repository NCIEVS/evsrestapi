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
import java.nio.charset.Charset;

import java.time.Duration;

public class JenaFusekiClientGenerator {
    static String returnChar = "\\n";
	public JenaFusekiClientGenerator() {

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

	public static Vector createConstrcQueryMethod(String methodSignature, Vector v) {
		Vector w = new Vector();
	    w.add("public String " + methodSignature + " {");
		w.add("\tString prefixes = owlSPARQLUtils.getPrefixes();");
		w.add("\tStringBuffer buf = new StringBuffer();");
		w.add("\tbuf.append(prefixes);");
		for (int i=0; i<v.size(); i++) {
			String str = (String) v.elementAt(i);
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
		w.add("\tVector v = HTTPUtils.executeQuery(query);");
		w.add("\tif (v == null) return null;");
		w.add("\tif (v.size() == 0) return v;");
		w.add("\tv = new ParserUtils().getResponseValues(v);");
		w.add("\treturn new SortUtils().quickSort(v);");
		w.add("}");
		return w;
	}

    public static void generateCode(String queryfile, String methodSignature) {
		Vector v = Utils.readFile(queryfile);
		Utils.dumpVector("\n", createConstrcQueryMethod(methodSignature, v));
		Utils.dumpVector("\n", createQueryMethod(methodSignature));
        Vector w = findHardCodedVariables(v);
		System.out.println("\nReminder: Need to substitute hard-coded variables.");
		Utils.dumpVector("Variables:", w);
	}

    public static Vector listFilesInDirectory(String dirName) {
		Vector v = new Vector();
		if (dirName == null) {
			dirName = System.getProperty("user.dir");;
		}
        File f = new File(dirName);
        String[] pathnames = f.list();
        for (String pathname : pathnames) {
            System.out.println(pathname);
            v.add(pathname);
        }
        return v;
	}

	public static void writeJavaSource(PrintWriter out, String className) {
		out.println("import gov.nih.nci.evs.restapi.util.*;");
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
		out.println("import org.apache.commons.codec.binary.Base64;");
		out.println("import java.nio.charset.Charset;");
		out.println("");
		out.println("import java.time.Duration;");
		out.println("/**");
		out.println("* <!-- LICENSE_TEXT_START -->");
		out.println(" * Copyright 2020 MSC. This software was developed in conjunction");
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
		out.println(" *      \"This product includes software developed by MSC and the National");
		out.println(" *      Cancer Institute.\"   If no such end-user documentation is to be");
		out.println(" *      included, this acknowledgment shall appear in the software itself,");
		out.println(" *      wherever such third-party acknowledgments normally appear.");
		out.println(" *   3. The names \"The National Cancer Institute\", \"NCI\" and \"MSC\" must");
		out.println(" *      not be used to endorse or promote products derived from this software.");
		out.println(" *   4. This license does not authorize the incorporation of this software");
		out.println(" *      into any third party proprietary programs. This license does not");
		out.println(" *      authorize the recipient to use any trademarks owned by either NCI");
		out.println(" *      or MSC");
		out.println(" *   5. THIS SOFTWARE IS PROVIDED \"AS IS,\" AND ANY EXPRESSED OR IMPLIED");
		out.println(" *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES");
		out.println(" *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE");
		out.println(" *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,");
		out.println(" *      MSC, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,");
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
		out.println(" *     Initial implementation kim.ong@nih.gov");
		out.println(" *");
		out.println(" */");
		out.println("");
		out.println("");
		out.println("public class " + className + " {");
		out.println("	String restURL = null;");
		out.println("	String username = null;");
		out.println("	String password = null;");
		out.println("");
		out.println("	static String DEFAULT_URL = \"http://localhost:3030/ncit/query\";");
		out.println("");
		out.println("	public " + className + "(String restURL,");
		out.println("	                    String username,");
		out.println("	                    String password) {");
		out.println("	    this.restURL = restURL;");
		out.println("	    this.username = username;");
		out.println("	    this.password = password;");
		out.println("	}");
		out.println("");
		out.println("    public String getPrefixes() {");
		out.println("		StringBuffer buf = new StringBuffer();");
		out.println("		buf.append(\"PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX xml:<http://www.w3.org/XML/1998/namespace>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX owl:<http://www.w3.org/2002/07/owl#>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX ncicp:<http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#>\").append(" + "\"" + returnChar + "\");");
		out.println("		buf.append(\"PREFIX dc:<http://purl.org/dc/elements/1.1/>\").append(" + "\"" + returnChar + "\");");
		out.println("		return buf.toString();");
		out.println("	}");
		out.println("");
	}

	public static String getMethodName(String queryfile) {
		int n = queryfile.lastIndexOf("_query");
		String t = queryfile.substring(0, n);
		String firstChar = "" + t.charAt(0);
		String methodName = firstChar.toUpperCase() + t.substring(1, t.length());
		return methodName;
	}

	public static void writeMethod(PrintWriter out, String restURL, String username, String password, String dirName, String queryfile) {
	    int n = queryfile.lastIndexOf("_query");
		String t = queryfile.substring(0, n);
		String firstChar = "" + t.charAt(0);
		String methodName = firstChar.toUpperCase() + t.substring(1, t.length());

		String methodSignature = "construct_get_" + t + "()";
		out.println("	public String " + methodSignature + " {");
		out.println("        String prefixes = getPrefixes();");
		out.println("        StringBuffer buf = new StringBuffer();");
		out.println("        buf.append(prefixes);");

		Vector v = Utils.readFile(dirName + File.separator + queryfile);
		v = replaceDoubleQuotes(v);
		v = bufAppend(v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			out.println("        " + line);
		}
		out.println("        String query = buf.toString();");
		out.println("        return query;");
		out.println("	}");
		out.println("");
		out.println("	public Vector get" + methodName + "(String restURL) {");
		out.println("        String query = " + methodSignature + ";");
		out.println("        if (restURL == null) {");
		out.println("			restURL = DEFAULT_URL;");
		out.println("		}");
		out.println("        Vector v = HTTPUtils.runQuery(restURL, username, password, query);");
		out.println("        return new SortUtils().quickSort(v);");
		out.println("	}");
		out.println("");
	}

	public static Vector writeMethods(PrintWriter out, String restURL, String username, String password, String dirName, Vector queryFiles) {
        Vector w = new Vector();
        for (int i=0; i<queryFiles.size(); i++) {
		    String queryfile = (String) queryFiles.elementAt(i);
		    System.out.println("**************** queryfile " + queryfile);
		    boolean bool = testQuery(restURL, username, password, dirName + File.separator + queryfile);
		    if (bool) {
				w.add(queryfile);
		    	writeMethod(out, restURL, username, password, dirName, queryfile);
			}
		}
		return w;
	}


	public static void writeMainMethod(PrintWriter out, String className, String dirName, Vector passed_queries) {
		out.println("	public static void main(String[] args) {");
		out.println("		String restURL = args[0];");
		out.println("		String username = args[1];");
		out.println("		if (username.compareTo(\"null\") == 0) {");
		out.println("			username = null;");
		out.println("		}");
		out.println("		String password = args[2];");
		out.println("		if (password.compareTo(\"null\") == 0) {");
		out.println("			password = null;");
		out.println("		}");
		out.println("	    " + className + " util = new " + className + "(restURL,");
		out.println("	                 username,");
		out.println("	                 password);");
		out.println("	    Vector w = null;");

        Vector queryFiles = listFilesInDirectory(dirName);
        for (int i=0; i<queryFiles.size(); i++) {
		    String queryfile = (String) queryFiles.elementAt(i);
		    if (passed_queries.contains(queryfile)) {
				out.println("	    w = util.get" + getMethodName(queryfile) + "(restURL);");
				out.println("	    Utils.dumpVector(\"w\", w);");
			}
		}
		out.println("    }");
		out.println("}");
	}

    public static boolean testQuery(String restURL, String username, String password, String queryfile) {
		String query = HTTPUtils.loadQuery(queryfile);
		System.out.println("*********************" + queryfile);
        System.out.println("*********************\n" + query);
		Vector v = HTTPUtils.runQuery(restURL, username, password, query);
		if (v == null) {
			return false;
		}
		Utils.dumpVector(queryfile, v);
		return true;
	}

	public static void test(String[] args) {
		long ms = System.currentTimeMillis();
		String className = args[0];
		String restURL = args[1];
		String dirName = args[2];
		String username = null;
		String password = null;
        Vector queryFiles = listFilesInDirectory(dirName);
        for (int i=0; i<queryFiles.size(); i++) {
		    String queryfile = (String) queryFiles.elementAt(i);
		    boolean bool = testQuery(restURL, username, password, queryfile);
		    System.out.println(queryfile + ": " + bool);
		}
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static String replaceDoubleQuotes(String s) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"') {
				buf.append("\\");
			}
			buf.append(c);
		}
		return buf.toString();
	}

	public static Vector replaceDoubleQuotes(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
		    String s = (String) v.elementAt(i);
		    w.add(replaceDoubleQuotes(s));
		}
		return w;
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String className = args[0];
		String restURL = args[1];
		String dirName = args[2];
		Vector queryFiles = listFilesInDirectory(dirName);
		String username = null;
		String password = null;
		try {
			PrintWriter out = new PrintWriter(className + ".java");
			writeJavaSource(out, className);
			Vector passed_queries = writeMethods(out, restURL, username, password, dirName, queryFiles);
			writeMainMethod(out, className, dirName, passed_queries);
			out.close();
			System.out.println(className + ".java" + " generated.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void main1(String[] args) {
		String s = "?y :NHC0 \"C178725\"^^xsd:string .";
		System.out.println(s);
		String t = replaceDoubleQuotes(s);
		System.out.println(t);
	}


}