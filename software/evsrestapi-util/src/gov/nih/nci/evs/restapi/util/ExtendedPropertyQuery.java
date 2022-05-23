package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

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
import org.json.*;

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


public class ExtendedPropertyQuery {
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String username = null;
    String password = null;
    OWLSPARQLUtils owlSPARQLUtils = null;
    static Vector states = null;
    static int MAX_LENGTH = 7;

    static {
		states = new Vector();
		states.add("H"); //hierarchical
		states.add("S"); //subset
		states.add("R"); //role
		states.add("A"); //asociation
		states.add("P"); //property
		states.add("C"); //class
	}

    public ExtendedPropertyQuery(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
    	this.named_graph = named_graph;
    	this.username = username;
    	this.password = password;
        this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.owlSPARQLUtils.set_named_graph(named_graph);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

    public String getPropSelectStmt(Vector path) {
		StringBuffer buf = new StringBuffer();
		if (path.size() > 1) {
			buf.append("select distinct ?x_code ?x_label ?y_code ?y_label ?p_label ?p_value");
		} else {
			buf.append("select distinct ?y_code ?y_label ?p_label ?p_value");
		}
		String s = buf.toString();
		s = s.trim();
		return s;
	}

    public String getPropertyQuery(String named_graph, String code, Vector path) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        String selectStmt = getPropSelectStmt(path);
        buf.append(selectStmt).append("\n");
        buf.append("{").append("\n");
        buf.append("    graph <" + named_graph + ">").append("\n");
        buf.append("    {").append("\n");
        if (path.size() > 1) {
			buf.append("            ?x :NHC0 ?x_code .").append("\n");
			buf.append("            ?x rdfs:label ?x_label .").append("\n");
			if (code != null) {
				buf.append("            ?x :NHC0 \"" + code + "\"^^xsd:string .").append("\n");
			}
		}
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");

		for (int i=0; i<path.size(); i++) {
			String t = (String) path.elementAt(i);
			if (t.startsWith("H")) {
				buf.append("            ?y (rdfs:subClassOf|(owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first)) ?x . ").append("\n");

			} else if (t.startsWith("S")) {
				buf.append("            ?y ?p2 ?x .").append("\n");
				buf.append("            ?p2 rdfs:label ?p2_label .").append("\n");
				buf.append("            ?p2 rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");

			} else if (t.startsWith("R")) {
				buf.append("            ?x (rdfs:subClassOf|owl:equivalentClass|owl:unionOf/rdf:rest*/rdf:first|owl:intersectionOf/rdf:rest*/rdf:first)* ?rs .").append("\n");
				buf.append("            ?rs a owl:Restriction .").append("\n");
				buf.append("            ?rs owl:onProperty ?r .").append("\n");
				buf.append("            ?rs owl:someValuesFrom ?y .").append("\n");
				buf.append("            ?r :NHC0 ?r_code .").append("\n");
				buf.append("            ?r rdfs:label ?r_label .").append("\n");
				buf.append("            ?r :NHC0 \"" + t + "\"^^xsd:string .").append("\n");

			} else if (t.startsWith("A")) {
				buf.append("            ?a a owl:AnnotationProperty" + " .").append("\n");
				buf.append("            ?a :NHC0 ?a_code .").append("\n");
				buf.append("            ?a rdfs:label ?a_label .").append("\n");
				buf.append("            ?a :NHC0 \"" + t + "\"^^xsd:string .").append("\n");
				buf.append("            ?y ?a ?a_value" + " .").append("\n");

			} else if (t.startsWith("P")) {
				buf.append("            ?p a owl:AnnotationProperty" + " .").append("\n");
				buf.append("            ?p :NHC0 ?p_code .").append("\n");
				buf.append("            ?p rdfs:label ?p_label .").append("\n");
				buf.append("            ?p :NHC0 \"" + t + "\"^^xsd:string .").append("\n");
				buf.append("            ?y ?p ?p_value" + " .").append("\n");
			}
		}
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}

	public Vector executeQuery(String query) {
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		String code = args[4];

        ExtendedPropertyQuery test = new ExtendedPropertyQuery(serviceUrl, named_graph, username, password);
        /*
        String s = "A26|P106";
        String query = test.getPropertyQuery(named_graph, code, StringUtils.parseData(s, '|'));
        */
        code = "C12346";
        code = "C177536";
        //String s = "R82|P106";
        //String s = "H|P106";
        //String s = "S|P106";
        String s = "P372";
        String query = test.getPropertyQuery(named_graph, null, StringUtils.parseData(s, '|'));

        System.out.println(query);
        Vector v = test.executeQuery(query);
        Utils.dumpVector(s, v);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }
}
