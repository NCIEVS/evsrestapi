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
import java.util.regex.*;
import java.nio.charset.Charset;

import java.time.Duration;
/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2020 MSC. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
6 * Redistribution and use in source and binary forms, with or without
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


public class ExternalLinkPageGenerator {
	static String NCI_BROWSER_PROPERTYES_XML = "NCItBrowserProperties.xml";
	String restURL = null;
	String namedGraph = null;
	String username = null;
	String password = null;
	OWLSPARQLUtils owlSPARQLUtils = null;
	HashMap propertyCode2labelHashMap = null;

    public ExternalLinkPageGenerator(String restURL, String namedGraph, String username, String password) {
		this.restURL = restURL;
		this.namedGraph = namedGraph;
		this.username = username;
		this.password = password;
		owlSPARQLUtils = new OWLSPARQLUtils(restURL, username, password);
		owlSPARQLUtils.set_named_graph(namedGraph);

		propertyCode2labelHashMap = new HashMap();
		Vector supportedProperties = owlSPARQLUtils.getSupportedProperties(namedGraph);
		for (int i=0; i<supportedProperties.size(); i++) {
			String supportedProperty = (String) supportedProperties.elementAt(i);
			Vector u = StringUtils.parseData(supportedProperty, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			propertyCode2labelHashMap.put(code, label);
		}
	}

	public String getPropertyLabel(String code) {
		return (String) propertyCode2labelHashMap.get(code);
	}

	public Vector executeQuery(String query) {
		return owlSPARQLUtils.executeQuery(query);
	}

	public String construct_get_concepts_with_property(String named_graph, String propertyCode) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT ?x_label ?x_code ?y1 ").append("\n");
		buf.append("{ ").append("\n");
		buf.append("    GRAPH <" + named_graph + "> ").append("\n");
		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("                ?x :NHC0 ?x_code .").append("\n");
		buf.append("                ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x ?p1 ?y1 .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p1 :NHC0 \"" + propertyCode + "\"^^xsd:string .").append("\n");
		buf.append("                ?p1 :NHC0 ?p1_code .").append("\n");
		buf.append("                ?p1 rdfs:label ?p1_label .").append("\n");
		buf.append("").append("\n");
		buf.append("        }").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector getConceptsWithProperty(String named_graph, String propertyCode) {
		String query = construct_get_concepts_with_property(named_graph, propertyCode);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

    public static String hyperlink(String url, String code) {
		return "<a href=\"" + url + code + "\">" + code + "</a>";
	}

    public void run(String propertyCode) {
		run(NCI_BROWSER_PROPERTYES_XML, propertyCode);
	}

	public String external_hyperlink_url = null;

	public String get_external_hyperlink_url() {
		return external_hyperlink_url;
	}

	public void set_external_hyperlink_url(String external_hyperlink_url) {
		System.out.println("external_hyperlink_url: " + external_hyperlink_url);
		this.external_hyperlink_url = external_hyperlink_url;
	}

    public void run(String xmlfile, String propertyCode) {
		String propertyName = getPropertyLabel(propertyCode);
		String url = getUri(propertyName);

        Vector w = getConceptsWithProperty(namedGraph, propertyCode);
        Vector w1 = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			w1.add((String) u.elementAt(0) + "|" + (String) u.elementAt(1) + "|"
			    + hyperlink(url, (String) u.elementAt(2)));
		}
        Vector w0 = new Vector();
        w0.add("Label|Code|" + propertyName);
        w0.addAll(w1);
        Utils.saveToFile(propertyName + ".txt", w0);
        String outputfile = new HTMLTableDataConverter(restURL, namedGraph, username, password).convert(propertyName + ".txt");
		Vector v = Utils.readFile(outputfile);
		outputfile = new HTMLTable().generate(v);
		System.out.println(outputfile + " generated.");
	}

	//NCI_BROWSER_PROPERTYES_XML
	public String getUri(String propertyName) {
		if (external_hyperlink_url != null) {
			return external_hyperlink_url;
		}
		return getUri(NCI_BROWSER_PROPERTYES_XML, propertyName);
	}

	public String getUri(String xmlfile, String propertyName) {
		String mod_propertyName = propertyName.replace("_", " ");
        PropertyFileParser parser = new PropertyFileParser(xmlfile);
        parser.run();
        Iterator it = parser.getDisplayItemList().iterator();
        if (it == null) return null;
        while (it.hasNext()) {
            DisplayItem item = (DisplayItem) it.next();
			String s = item.getItemLabel().replace("_", " ");
            if (item.getItemLabel().compareTo(propertyName) == 0 || s.compareTo(propertyName) == 0 ||
                item.getItemLabel().compareTo(mod_propertyName) == 0 || s.compareTo(mod_propertyName) == 0) {
				return item.getUrl();
			}
        }
        return null;
	}


	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String restURL = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		String propertyCode = args[4];
        ExternalLinkPageGenerator generator = new ExternalLinkPageGenerator(
			restURL, namedGraph, username, password);

		//String propertyCode = "P93";
        //String xmlfile = "NCItBrowserProperties.xml";
        if (args.length == 6) {
			generator.set_external_hyperlink_url(args[5]);
		}
		generator.run(propertyCode);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

}