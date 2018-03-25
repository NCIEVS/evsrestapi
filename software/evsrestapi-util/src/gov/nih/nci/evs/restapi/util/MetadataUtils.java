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




public class MetadataUtils {
    String serviceUrl = null;
    String sparql_endpoint = null;
	String namedGraph = null;
	OWLSPARQLUtils owlSPARQLUtils = null;
	SPARQLUtilsClient client = null;
	HashMap nameVersion2NamedGraphMap = null;


	public String verify_service_url(String serviceUrl) {
		int n = serviceUrl.indexOf("?");
        if (n != -1) {
			serviceUrl = serviceUrl.substring(0, n);
		}
		return serviceUrl;
	}


    public MetadataUtils() {

	}

    public  MetadataUtils(String serviceUrl) {
		this.serviceUrl = verify_service_url(serviceUrl);
		initialize();
    }

    public void initialize() {
		long ms = System.currentTimeMillis();
		this.sparql_endpoint = serviceUrl;
		this.client = new SPARQLUtilsClient(sparql_endpoint);
		this.owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint + "?query=");
		this.nameVersion2NamedGraphMap = owlSPARQLUtils.getNameVersion2NamedGraphMap();
		System.out.println("Total MetadataUtils initialization run time (ms): " + (System.currentTimeMillis() - ms));
    }

    public void dumpNameVersion2NamedGraphMap() {
		if (nameVersion2NamedGraphMap == null) return;
		Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
		Vector versions = new Vector();
		while (it.hasNext()) {
			String nameVersion = (String) it.next();
			Vector u = StringUtils.parseData(nameVersion);
			String codingSchemeName = (String) u.elementAt(0);
			String version = (String) u.elementAt(1);
			//System.out.println(nameVersion + " --> " + version);
			Vector named_graphs = (Vector) nameVersion2NamedGraphMap.get(nameVersion);
			for (int i=0; i<named_graphs.size(); i++) {
				String named_graph = (String) named_graphs.elementAt(i);
				System.out.println(nameVersion + " --> " + named_graph);
			}
		}

	}

    public String getLatestVersion(String codingScheme) {
		if (nameVersion2NamedGraphMap == null) return null;
		Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
		Vector versions = new Vector();
		while (it.hasNext()) {
			String nameVersion = (String) it.next();
			Vector u = StringUtils.parseData(nameVersion);
			String codingSchemeName = (String) u.elementAt(0);
			if (codingSchemeName.compareTo(codingScheme) == 0) {
				String version = (String) u.elementAt(1);
				versions.add(version);
			}
		}
		versions = new SortUtils().quickSort(versions);
        return (String) versions.elementAt(versions.size()-1);
	}

    public String getNamedGraph(String codingScheme) {
		return getNamedGraph(codingScheme, null);
	}

    public String getNamedGraph(String codingScheme, String version) {
		if (version == null) {
			version = getLatestVersion(codingScheme);
		}
		String namedGraph = client.getNamedGraphByCodingSchemeAndVersion(codingScheme, version);
		return namedGraph;
	}

	public static String getLatestVersionOfCodingScheme(String serviceUrl, String codingScheme) {
		String sparql_endpoint = serviceUrl + "?query=";
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint);
		HashMap nameVersion2NamedGraphMap = owlSPARQLUtils.getNameVersion2NamedGraphMap();
		if (nameVersion2NamedGraphMap == null) return null;
		Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
		Vector versions = new Vector();
		while (it.hasNext()) {
			String nameVersion = (String) it.next();
			Vector u = StringUtils.parseData(nameVersion);
			String codingSchemeName = (String) u.elementAt(0);
			if (codingSchemeName.compareTo(codingScheme) == 0) {
				String version = (String) u.elementAt(1);
				versions.add(version);
			}
		}
		versions = new SortUtils().quickSort(versions);
        return (String) versions.elementAt(versions.size()-1);
	}

	public static String getNamedGraphOfCodingScheme(String serviceUrl, String codingScheme, String version) {
		SPARQLUtilsClient client = new SPARQLUtilsClient(serviceUrl);
		String namedGraph = client.getNamedGraphByCodingSchemeAndVersion(codingScheme, version);
		return namedGraph;
	}

	public static String sparqlEndpoint2ServiceUrl(String sparql_endpoint) {
		int n = sparql_endpoint.lastIndexOf("?");
		if (n == -1) return sparql_endpoint;
		return sparql_endpoint.substring(0, n);
	}

	public static String serviceUrl2SparqlEndpoint(String serviceUrl) {
		int n = serviceUrl.lastIndexOf("?");
		if (n != -1) return serviceUrl;
		return serviceUrl + "?query=";
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		System.out.println(serviceUrl);
		MetadataUtils test = new MetadataUtils(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		long ms = System.currentTimeMillis();
		String version = test.getLatestVersion(codingScheme);
		System.out.println(codingScheme);
		System.out.println(version);
		String named_graph = test.getNamedGraph(codingScheme);
		System.out.println(named_graph);

		test.dumpNameVersion2NamedGraphMap();

		String version_test = MetadataUtils.getLatestVersionOfCodingScheme(serviceUrl, codingScheme);
		System.out.println("getLatestVersionOfCodingScheme: " + version_test);

		String named_graph_test = MetadataUtils.getNamedGraphOfCodingScheme(serviceUrl, codingScheme, version_test);
		System.out.println("getNamedGraphOfCodingScheme: " + named_graph_test);

	}
}