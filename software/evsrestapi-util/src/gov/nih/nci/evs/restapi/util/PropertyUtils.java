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
import org.apache.commons.codec.binary.Base64;
import org.json.*;

import java.util.StringTokenizer;

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


public class PropertyUtils {
	OWLSPARQLUtils owlSPARQLUtils = null;
	String serviceUrl = null;
	String named_graph = null;
	String ncit_version = null;
	String sparql_endpoint = null;
	static String CTS_API_Disease_Main_Type_Terminology_Code = "C138190";
	HashMap nameVersion2NamedGraphMap = null;
	MetadataUtils mdu = null;
	HashMap uriBaseHashMap = null;
	HashMap nameGraph2PredicateHashMap = null;
	Vector supportedNamedGraphs = null;

	public String[] PRESENTATIONS = new String[] {"Display_Name", "FULL_SYN", "Legacy_Concept_Name", "Preferred_Name"};
	public String[] DEFINITIONS = new String[] {"ALT_DEFINITION", "DEFINITION"};
    public String[] COMMENTS = new String[] {"DesignNote"};
    public String[] COMPLEX_PROPERTIES = new String[] {"ALT_DEFINITION", "DEFINITION", "FULL_SYN"};
    public List     PRESENTATION_LIST = Arrays.asList(PRESENTATIONS);
    public List     DEFINITION_LIST = Arrays.asList(DEFINITIONS);
    public List     COMMENT_LIST = Arrays.asList(COMMENTS);
    public List     COMPLEX_PROPERTY_LIST = Arrays.asList(COMPLEX_PROPERTIES);
    public Vector all_properties = null;

	public PropertyUtils(String serviceUrl) {
		System.out.println("Initialization ...");
		long ms = System.currentTimeMillis();
		this.serviceUrl = serviceUrl;
		this.sparql_endpoint = serviceUrl + "?query=";
		mdu = new MetadataUtils(serviceUrl);
		nameVersion2NamedGraphMap = mdu.getNameVersion2NamedGraphMap();
		uriBaseHashMap = mdu.getURIBaseHashMap();
		this.supportedNamedGraphs = mdu.getSupportedNamedGraphs();
		this.named_graph = mdu.getNamedGraph(Constants.NCI_THESAURUS);
		this.ncit_version = mdu.getLatestVersion(Constants.NCI_THESAURUS);
		System.out.println("sparql_endpoint: " + sparql_endpoint);
		System.out.println("named_graph: " + named_graph);
		System.out.println("ncit_version: " + ncit_version);
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		this.owlSPARQLUtils = owlSPARQLUtils;
		all_properties = getSupportedProperties();
		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
	}

	public Vector getAnnotationProperties(String named_graph) {
		return this.owlSPARQLUtils.getAnnotationProperties(named_graph);
	}

	public Vector getSupportedNamedGraphs() {
		return this.supportedNamedGraphs;
	}

	public MetadataUtils getMetadataUtils() {
		return this.mdu;
	}

	public HashMap getNameVersion2NamedGraphMap() {
        return this.nameVersion2NamedGraphMap;
	}

	public static Vector formatOutput(Vector v) {
		return ParserUtils.formatOutput(v);
	}

    public static String trimLeadingBlanksOrTabs(String line) {
		return StringUtils.trimLeadingBlanksOrTabs(line);
	}

	public static String escapeDoubleQuotes(String line) {
		return StringUtils.escapeDoubleQuotes(line);
	}

    public static void generate_construct_statement(String method_name, String params, String filename) {
		Utils.generate_construct_statement(method_name, params, filename);
	}

	public String getURIBase(String named_graph) {
		return mdu.getURIBase(named_graph);
	}

    public void runQuery(String query_file) {
		int n = query_file.indexOf(".");
		String method_name = query_file.substring(0, n);
		String params = "String named_graph, String code";

		String query = owlSPARQLUtils.getQuery(query_file);
		System.out.println(query);

	    Utils.generate_construct_statement(method_name, params, query_file);

		String json = owlSPARQLUtils.getJSONResponseString(query);
		System.out.println(json);
        Vector v = owlSPARQLUtils.execute(query_file);
        v = formatOutput(v);
        StringUtils.dumpVector("v", v);
	}

	public OWLSPARQLUtils getOWLSPARQLUtils() {
		return owlSPARQLUtils;
	}

	public Vector getSupportedProperties() {
        return owlSPARQLUtils.getSupportedProperties(named_graph);
	}

	public Vector getSupportedAssociations() {
        return owlSPARQLUtils.getSupportedAssociations(named_graph);
	}

	public boolean isPresentation(String propertyName) {
		return PRESENTATION_LIST.contains(propertyName);
	}

	public boolean isDefinition(String propertyName) {
		return DEFINITION_LIST.contains(propertyName);
	}

	public boolean isComment(String propertyName) {
		return COMMENT_LIST.contains(propertyName);
	}

	public boolean isGeneric(String propertyName) {
		if (!PRESENTATION_LIST.contains(propertyName) && !DEFINITION_LIST.contains(propertyName) && !COMMENT_LIST.contains(propertyName)) {
			return true;
		}
		return false;
	}

	public Vector getAllProperties() {
		return all_properties;
	}

	public void set_presentations(String[] presentations) {
		if (presentations == null) return;
		this.PRESENTATIONS = presentations;
		this.PRESENTATION_LIST = Arrays.asList(presentations);
	}

	public void set_definitions(String[] definitions) {
		if (definitions == null) return;
		this.DEFINITIONS = definitions;
		this.DEFINITION_LIST = Arrays.asList(definitions);
	}

	public void set_comments(String[] comments) {
		if (comments == null) return;
		this.COMMENTS = comments;
		this.COMMENT_LIST = Arrays.asList(comments);
	}

    public static void main(String[] args) {
		String serviceUrl = args[0];
		PropertyUtils test = new PropertyUtils(serviceUrl+"?query=");
		String named_graph = args[1];
		test.set_named_graph(named_graph);
		String propertyName = "Display_Name";
		System.out.println(propertyName + " isPresentation? " + test.isPresentation(propertyName));
		System.out.println(propertyName + " isDefinition? " + test.isDefinition(propertyName));
		System.out.println(propertyName + " isComment? " + test.isComment(propertyName));
		System.out.println(propertyName + " isGeneric? " + test.isGeneric(propertyName));
	}
}
