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


public class MappingUtils {
/*
    static String[] VARS = new String[] {"sourceCode",
                                         "sourceName",
                                         "sourceCodingScheme",
                                         "sourceCodingSchemeVersion",
                                         "sourceCodingSchemeNamespace",
                                         "targetCodingScheme",
                                         "associationName",
                                         "rel",
                                         "mapRank",
                                         "targetCode",
                                         "targetName",
                                         "targetCodingScheme",
                                         "targetCodingSchemeVersion",
                                         "targetCodingSchemeNamespace"};

	static VAR_HASHMAP = null;

	static {
		VAR_HASHMAP = new HashMap();
		for (int j=0; j<VARS.length; j++) {
			String var = VARS[j];
			VAR_HASHMAP.put(var, var);
		}
	}
*/
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;

    ParserUtils parser = new ParserUtils();
    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String ns = null;

    public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

    public void setJSONUtils(JSONUtils jsonUtils) {
		this.jsonUtils = jsonUtils;
	}

    public void setNs(String ns) {
		this.ns = ns;
	}

    public void setHTTPUtils(HTTPUtils httpUtils) {
		this.httpUtils = httpUtils;
	}

	public void setOntologyUri2LabelMap(HashMap ontologyUri2LabelMap) {
		this.ontologyUri2LabelMap = ontologyUri2LabelMap;
	}

	public MappingUtils() {
		this.httpUtils = new HTTPUtils();
        this.jsonUtils = new JSONUtils();
        //this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
	}

	public String verifyServiceUrl(String serviceUrl) {
		if (serviceUrl.indexOf("?query=?query=") != -1) {
			int n = serviceUrl.lastIndexOf("?");
			serviceUrl = serviceUrl.substring(0, n);
		} else if (serviceUrl.indexOf("?") == -1) {
			serviceUrl = serviceUrl + "?query=";
		}
		return serviceUrl;
	}

	public MappingUtils(String serviceUrl) {
		this.serviceUrl = verifyServiceUrl(serviceUrl);
		this.httpUtils = new HTTPUtils(serviceUrl, null, null);
        this.jsonUtils = new JSONUtils();
        //this.ontologyUri2LabelMap = createOntologyUri2LabelMap();
    }

    public Vector executeQuery(String query) {
        Vector v = null;
        try {
			query = httpUtils.encode(query);
            String json = httpUtils.executeQuery(query);
			v = new JSONUtils().parseJSON(json);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}
//GO:0050975
	public String construct_mapentry_query(String named_graph, String id) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<" + named_graph + ">").append("\n");
		buf.append("PREFIX base:<" + named_graph + ">").append("\n");
		buf.append("PREFIX Thesaurus:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("SELECT ?x ?y ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <" + named_graph + ">").append("\n");
		buf.append("{").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		if (id != null) {
			buf.append("FILTER(str(?x) = \"" + id + "\"^^xsd:string)").append("\n");
		}
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector get_mapentry(String named_graph, String id) {
		String query = construct_mapentry_query(named_graph, id);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			return v;
		}
		return null;
	}

	public String construct_mapentry_id_query(String named_graph, String key, String value) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<" + named_graph + ">").append("\n");
		buf.append("PREFIX base:<" + named_graph + ">").append("\n");
		buf.append("PREFIX Thesaurus:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("SELECT ?x").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <http://gov.nih.nci.evs/mapping/go_to_ncit_mapping_1.1>").append("\n");
		buf.append("{").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("FILTER(str(?z) = \"" + value + "\"^^xsd:string && endsWith(str(?y), \"" +key + "\"^^xsd:string))").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector get_mapentry_id_by_source_code(String named_graph, String code) {
		String query = construct_mapentry_id_query(named_graph, "sourceCode", code);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}


	public Vector get_mapentry_id_by_target_code(String named_graph, String code) {
		String query = construct_mapentry_id_query(named_graph, "targetCode", code);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public String construct_mapentry_ids_query(String named_graph) {
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<" + named_graph + ">").append("\n");
		buf.append("PREFIX base:<" + named_graph + ">").append("\n");
		buf.append("PREFIX Thesaurus:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		buf.append("SELECT distinct ?z").append("\n");
		buf.append("{").append("\n");
		buf.append("graph <http://gov.nih.nci.evs/mapping/go_to_ncit_mapping_1.1>").append("\n");
		buf.append("{").append("\n");
		buf.append("?x ?y ?z .").append("\n");
		buf.append("FILTER(endsWith(str(?y), \"sourceCode\"^^xsd:string))").append("\n");
		buf.append("}").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector get_mapentry_ids(String named_graph) {
		String query = construct_mapentry_ids_query(named_graph);
		Vector v = executeQuery(query);
		if (v != null && v.size() > 0) {
			v = new ParserUtils().getResponseValues(v);
			v = new SortUtils().quickSort(v);
			return v;
		}
		return null;
	}

	public Vector trim_namespace(Vector v, String ns) {
        Vector w = new Vector();
        for (int i=0; i<v.size(); i++) {
		    String line = (String) v.elementAt(i);

		    System.out.println(line);

		    Vector u = StringUtils.parseData(line, '|');
		    String x = (String) u.elementAt(0);
		    String y = (String) u.elementAt(1);
		    String z = (String) u.elementAt(2);
		    if (x.startsWith(ns)) {
				x = x.substring(ns.length(), x.length());
			}
		    if (y.startsWith(ns)) {
				y = y.substring(ns.length(), y.length());
			}
			w.add(x + "|" + y + "|" + z);
		}
		return w;
	}

    public MapEntry constructMapEntry(Vector v) {
		String sourceCode = null;
		String sourceName = null;
		String sourceCodingScheme = null;
		String sourceCodingSchemeVersion = null;
		String sourceCodingSchemeNamespace = null;
		String associationName = null;
		String rel = null;
		String mapRank = null;
		String targetCode = null;
		String targetName = null;
		String targetCodingScheme = null;
		String targetCodingSchemeVersion = null;
		String targetCodingSchemeNamespace = null;

		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String y = (String) u.elementAt(1);
			String z = (String) u.elementAt(2);
			if (y.compareTo("sourceCode") == 0) {
				sourceCode = z;
			} else if (y.compareTo("sourceName") == 0) {
				sourceName = z;
			} else if (y.compareTo("sourceCodingScheme") == 0) {
				sourceCodingScheme = z;
			} else if (y.compareTo("sourceCodingSchemeVersion") == 0) {
				sourceCodingSchemeVersion = z;
			} else if (y.compareTo("sourceCodingSchemeNamespace") == 0) {
				sourceCodingSchemeNamespace = z;
			} else if (y.compareTo("associationName") == 0) {
				associationName = z;
			} else if (y.compareTo("rel") == 0) {
				rel = z;
			} else if (y.compareTo("mapRank") == 0) {
				mapRank = z;
			} else if (y.compareTo("targetCode") == 0) {
				targetCode = z;
			} else if (y.compareTo("targetName") == 0) {
				targetName = z;
			} else if (y.compareTo("targetCodingScheme") == 0) {
				targetCodingScheme = z;
			} else if (y.compareTo("targetCodingSchemeVersion") == 0) {
				targetCodingSchemeVersion = z;
			} else if (y.compareTo("targetCodingSchemeNamespace") == 0) {
				targetCodingSchemeNamespace = z;
			}
		}
	    return new MapEntry(
			sourceCode,
			sourceName,
			sourceCodingScheme,
			sourceCodingSchemeVersion,
			sourceCodingSchemeNamespace,
			associationName,
			rel,
			mapRank,
			targetCode,
			targetName,
			targetCodingScheme,
			targetCodingSchemeVersion,
			targetCodingSchemeNamespace);
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		MappingUtils mappingUtils = new MappingUtils(serviceUrl);
        String named_graph = args[1];
        String sourcecode = args[2];
        Vector ids = mappingUtils.get_mapentry_id_by_source_code(named_graph, sourcecode);

        for (int k=0; k<ids.size(); k++) {
			String id = (String) ids.elementAt(k);
			System.out.println(id);

			Vector v = mappingUtils.get_mapentry(named_graph, id);
			v = mappingUtils.trim_namespace(v, named_graph);
			StringUtils.dumpVector("v", v);

			MapEntry entry = mappingUtils.constructMapEntry(v);
			System.out.println(entry.toXML());
			System.out.println(entry.toJson());
		}
        /*
        Vector w = mappingUtils.get_mapentry_ids(named_graph);
        StringUtils.dumpVector("w", w);
        */
	}

}