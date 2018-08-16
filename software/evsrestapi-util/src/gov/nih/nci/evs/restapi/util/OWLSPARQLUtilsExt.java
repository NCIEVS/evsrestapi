package gov.nih.nci.evs.restapi.util;

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


public class OWLSPARQLUtilsExt {

	public static String verifyServiceUrl(String serviceUrl) {
		if (serviceUrl.indexOf("?query=?query=") != -1) {
			serviceUrl = serviceUrl.replace("?query=?query=", "?query=");
		} else if (serviceUrl.indexOf("?") == -1) {
			serviceUrl = serviceUrl + "?query=";
		}
		return serviceUrl;
	}


	public static HashMap getNameVersion2EndpointNamedGraphMap(String[] sparql_endpoints) {
		HashMap hmap = new HashMap();
		for (int i=0; i<sparql_endpoints.length; i++) {
			String sparql_endpoint = sparql_endpoints[i];
			sparql_endpoint = verifyServiceUrl(sparql_endpoint);
			//OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint + "?query=");
			OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint);
			HashMap sub_hmap = null;
			try {
				sub_hmap = owlSPARQLUtils.getNameVersion2NamedGraphMap();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			if (sub_hmap != null) {
				Iterator it = sub_hmap.keySet().iterator();
				while (it.hasNext()) {
					String key = (String) it.next(); //NameVersion
					Vector values = (Vector) sub_hmap.get(key);//namedgraphs
					if (values != null) {
						for (int k=0; k<values.size(); k++) {
							String namedgraph = (String) values.elementAt(k);
							Vector w = new Vector();
							if (hmap.containsKey(key)) {
								w = (Vector) hmap.get(key);
							}
							if (!w.contains(sparql_endpoint + "|" + namedgraph)) {
								w.add(sparql_endpoint + "|" + namedgraph);
							}
							hmap.put(key, w);
						}
					}
				}
			} else {
				System.out.println("owlSPARQLUtils.getNameVersion2NamedGraphMap() returns NULL???");
			}
		}
		return hmap;
	}

	public static String getLatestVersion(HashMap nameVersion2EndpointNamedGraphMap, String codingSchemeName) {
		if (nameVersion2EndpointNamedGraphMap == null) return null;
		Vector v = new Vector();
		Iterator it = nameVersion2EndpointNamedGraphMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (key.startsWith(codingSchemeName + "|")) {
				v.add(key);
			}
		}
		if (v.size() > 0) {
			v = new SortUtils().quickSort(v);
		}

		if (v.size() >= 1) {
			String key = (String) v.elementAt(v.size()-1);
			Vector values = (Vector) nameVersion2EndpointNamedGraphMap.get(key);
			String value = null;
			if (values != null) {
				value = (String) values.elementAt(0);
			}
			Vector u = StringUtils.parseData(key, '|');
			//return key + "|" + value;
			return (String) u.elementAt(1);
	    } else {
			return null;
		}
	}

	public static Vector getSupportedCodingSchemesAndVersions(HashMap nameVersion2EndpointNamedGraphMap) {
		if (nameVersion2EndpointNamedGraphMap == null) return null;
		Vector v = new Vector();
		Iterator it = nameVersion2EndpointNamedGraphMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (!v.contains(key)) {
				v.add(key);
			}
		}
		v = new SortUtils().quickSort(v);
        return v;
	}

	public static Vector getSupportedCodingSchemes(HashMap nameVersion2EndpointNamedGraphMap) {
		if (nameVersion2EndpointNamedGraphMap == null) return null;
		Vector v = new Vector();
		Iterator it =
		nameVersion2EndpointNamedGraphMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector u = StringUtils.parseData(key, '|');
			String codingScheme = (String) u.elementAt(0);
			if (!v.contains(codingScheme)) {
				v.add(codingScheme);
			}
		}
		v = new SortUtils().quickSort(v);
        return v;
	}

	public static String getPrefix(Vector prefix_vec, String codingSchemeName) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<prefix_vec.size(); i++) {
			String t = (String) prefix_vec.elementAt(i);
			t = t.replaceAll("&lt;", "<");
			t = t.replaceAll("&gt;", ">");
			Vector u = StringUtils.parseData(t, '|');
			String key = (String) u.elementAt(0);
			if (key.compareTo("COMMON") == 0 || key.compareTo(codingSchemeName) == 0) {
				String value = (String) u.elementAt(1);
				buf.append(value).append("\n");
			}
		}
		return buf.toString();
	}

    public static HashMap getCodingSchemeNameVersion2OWLSPARQLUtilsMap(String[] sparql_endpoints) {
		return getCodingSchemeNameVersion2OWLSPARQLUtilsMap(sparql_endpoints, null);
	}

    public static HashMap getCodingSchemeNameVersion2OWLSPARQLUtilsMap(String[] sparql_endpoints, Vector prefix_vec) {
		HashMap OWLSPARQLUtilsMap = new HashMap();
		for (int i=0; i<sparql_endpoints.length; i++) {
			String sparql_endpoint = sparql_endpoints[i];
			System.out.println("SPARQL endpoint: " + sparql_endpoint);
			OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint);
			HashMap sub_hmap = null;
			try {
			    sub_hmap = owlSPARQLUtils.getNameVersion2NamedGraphMap();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			if (sub_hmap == null) {
				System.out.println("sub_hmap == null???");
			} else {
				Iterator it = sub_hmap.keySet().iterator();
				while (it.hasNext()) {
					String key = (String) it.next();
				    try {
						Vector u = StringUtils.parseData(key, '|');
						//NCI_Thesaurus|17.05e --> https://sparql-evs-stage.nci.nih.gov/sparql?query=|http://NCIt_Flattened
						String codingScheme = (String) u.elementAt(0);
						System.out.println("\tcodingScheme: " + codingScheme);
						String version = (String) u.elementAt(1);
						System.out.println("\tversion: " + version);
						Vector named_graphs = (Vector) sub_hmap.get(key);
						for (int k=0; k<named_graphs.size(); k++) {
							String named_graph = (String) named_graphs.elementAt(k);
							OWLSPARQLUtils sparql_util = new OWLSPARQLUtils(sparql_endpoint);
							sparql_util.set_named_graph(named_graph);
							String prefixes = null;
							if (prefix_vec != null) {
								prefixes = getPrefix(prefix_vec, codingScheme);
							} else {
								String uri = PrefixUtils.codingScheme2URI(codingScheme);
								prefixes = PrefixUtils.getPrefixes(uri);
							}
							sparql_util.set_prefixes(prefixes);
							String csnameversion = codingScheme + "|" + version;
							OWLSPARQLUtilsMap.put(csnameversion, sparql_util);

						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
		return OWLSPARQLUtilsMap;
	}


	public static void main(String[] args) {
		String[] sparql_endpoints = new String[1];
		sparql_endpoints[0] = args[0];//"https://sparql-evs-dev.nci.nih.gov/sparql";
		HashMap nameVersion2EndpointNamedGraphMap = OWLSPARQLUtilsExt.getNameVersion2EndpointNamedGraphMap(sparql_endpoints);
		Iterator it = nameVersion2EndpointNamedGraphMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			System.out.println("key: " + key);
			Vector values = (Vector) nameVersion2EndpointNamedGraphMap.get(key);
			for (int k=0; k<values.size(); k++) {
				String value = (String) values.elementAt(k);
				System.out.println("\tEndpointNamedGraph: " + value);
			}
		}
		String codingSchemeName = "NCI_Thesaurus";
		String version = OWLSPARQLUtilsExt.getLatestVersion(nameVersion2EndpointNamedGraphMap, codingSchemeName);
		System.out.println("\nVocabulary: " + codingSchemeName);
		System.out.println("Latest version: " + version);

		Vector w = OWLSPARQLUtilsExt.getSupportedCodingSchemes(nameVersion2EndpointNamedGraphMap);
		for (int k=0; k<w.size(); k++) {
			String value = (String) w.elementAt(k);
			System.out.println("SupportedCodingSchemes: " + value);
		}

		w = OWLSPARQLUtilsExt.getSupportedCodingSchemesAndVersions(nameVersion2EndpointNamedGraphMap);
		for (int k=0; k<w.size(); k++) {
			String value = (String) w.elementAt(k);
			System.out.println("SupportedCodingSchemesAndVersion: " + value);
		}

		HashMap codingSchemeNameVersion2OWLSPARQLUtilsMap = OWLSPARQLUtilsExt.getCodingSchemeNameVersion2OWLSPARQLUtilsMap(sparql_endpoints);
		Iterator it2 = codingSchemeNameVersion2OWLSPARQLUtilsMap.keySet().iterator();
		while (it2.hasNext()) {
			String key = (String) it2.next();
			System.out.println("\tcodingSchemeNameVersion2OWLSPARQLUtilsMap key: " + key);
			OWLSPARQLUtils owlSPARQLUtils = (OWLSPARQLUtils) codingSchemeNameVersion2OWLSPARQLUtilsMap.get(key);
            String named_graph = owlSPARQLUtils.get_named_graph();
			System.out.println("\tOWLSPARQLUtils named_graph: " + named_graph);
			Vector w2 = owlSPARQLUtils.getTripleCount(named_graph);
			StringUtils.dumpVector("count", w2);
		}
	}
}
