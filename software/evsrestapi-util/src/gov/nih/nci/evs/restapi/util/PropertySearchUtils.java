package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.appl.*;
import gov.nih.nci.evs.restapi.bean.*;

import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;
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
import org.apache.commons.codec.binary.Base64;
import org.json.*;

import org.apache.commons.text.similarity.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

public class PropertySearchUtils extends SPARQLSearchUtils {
	private OWLSPARQLUtils owlSPARQLUtils = null;
	private String named_graph = null;

	private static String CONTAINS = "contains";
	private static String EXACT_MATCH = "exactMatch";
	private static String STARTS_WITH = "startsWith";

	public PropertySearchUtils(String serviceUrl) {
		super(serviceUrl);
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
    }

    public void initialize(String named_graph) {
		long ms = System.currentTimeMillis();
		this.named_graph = named_graph;
		this.owlSPARQLUtils.set_named_graph(named_graph);
		this.set_named_graph(named_graph);
		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

    public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
		this.owlSPARQLUtils.set_named_graph(named_graph);
	}

	public OWLSPARQLUtils getOWLSPARQLUtils() {
		return this.owlSPARQLUtils;
	}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Property search
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String construct_property_query(String named_graph, String property, String matchText, String algorithm) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		String filterStmt = null;
		filterStmt = SPARQLUtils.apply_text_match("y", matchText, algorithm);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y").append("\n");
		buf.append("{ ").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x ?p ?y .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append(filterStmt).append("\n");
		if (property != null) {
			buf.append("FILTER (str(?p_label)=\"" + property + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


    public String construct_property_query(String named_graph, Vector properties, String matchText, String algorithm) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		String filterStmt = null;
		filterStmt = SPARQLUtils.apply_text_match("y", matchText, algorithm);
		buf.append("SELECT distinct ?x_label ?x_code ?p_label ?y").append("\n");
		buf.append("{ ").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x a owl:Class .").append("\n");
		buf.append("?x rdfs:label ?x_label .").append("\n");
		buf.append("?x :NHC0 ?x_code .").append("\n");
		buf.append("?x ?p ?y .").append("\n");
		buf.append("?p rdfs:label ?p_label .").append("\n");
		buf.append(filterStmt).append("\n");
		if (properties != null) {
			String propertyFilterStmt = SPARQLUtils.construct_filter_stmt("p_label", properties, true);
			buf.append(propertyFilterStmt).append("\n");
		}
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector property_query(String named_graph, String property, String matchText, String algorithm) {
		String query = construct_property_query(named_graph, property, matchText, algorithm);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
		} else {
			v = new Vector();
		}
		return new SortUtils().quickSort(v);
	}

	public Vector property_query(String named_graph, Vector properties, String matchText, String algorithm) {
		String query = construct_property_query(named_graph, properties, matchText, algorithm);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
		} else {
			v = new Vector();
		}
		return new SortUtils().quickSort(v);
	}

    public List<MatchedConcept> searchByProperty(String named_graph, Vector properties, String matchText, String algorithm) {
        Vector v = property_query(named_graph, properties, matchText, algorithm);
        return SPARQLUtils.toMatchedConceptList(v);
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		System.out.println(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		MetadataUtils test = new MetadataUtils(serviceUrl);
		String version = test.getLatestVersion(codingScheme);
		String named_graph = test.getNamedGraph(codingScheme);
		System.out.println(named_graph);
		serviceUrl = serviceUrl + "?query=";

		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("codingScheme: " + codingScheme);
		System.out.println("version: " + version);

		named_graph = args[1];

		System.out.println("named_graph: " + named_graph);

		PropertySearchUtils searchUtils = new PropertySearchUtils(serviceUrl);
		searchUtils.set_named_graph(named_graph);

	    ms = System.currentTimeMillis();
	    Vector v = null;

	    String property = "FULL_SYN";
	    String matchText = "cell aging";
	    String algorithm = "contains";
	    algorithm = "exactMatch";

        Vector properties = new Vector();
        properties.add("Preferred_Name");
        properties.add("Display_Name");
        properties.add("FULL_SYN");
        properties.add("label");
        properties.add("Legacy_Concept_Name");

	    String q = searchUtils.construct_property_query(named_graph, properties, matchText, algorithm);
	    System.out.println(q);
	    v = searchUtils.property_query(named_graph, properties, matchText, algorithm);
	    StringUtils.dumpVector(matchText, v);

	    System.out.println("Total search run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
