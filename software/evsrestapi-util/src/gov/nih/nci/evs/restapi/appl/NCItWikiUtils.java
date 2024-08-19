package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.config.*;

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


public class NCItWikiUtils {
	String restURL = null;
	String namedGraph = null;
	String username = null;
	String password = null;
	MetadataUtils metadataUtils = null;
	OWLSPARQLUtils owlSPARQLUtils = null;
	HashSet retired_concepts = new HashSet();

    public NCItWikiUtils(String restURL, String namedGraph, String username, String password) {
		this.restURL = restURL;
		this.namedGraph = namedGraph;
		this.username = username;
		this.password = password;

System.out.println(	"restURL: " + restURL);
System.out.println(	"namedGraph: " + namedGraph);
System.out.println(	"username: " + username);
System.out.println(	"password: " + password);

		metadataUtils = new MetadataUtils(restURL, username, password);
		owlSPARQLUtils = new OWLSPARQLUtils(restURL, username, password);
		owlSPARQLUtils.set_named_graph(namedGraph);

		String ncit_version = getVersion();
		System.out.println(ncit_version);

	    String property_name = "Concept_Status";
	    String property_value = "Retired_Concept";
		Vector w = findConceptsWithPropertyMatching(namedGraph, property_name, property_value);
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			retired_concepts.add(code);
		}
	}

    public Vector findConceptsWithPropertyMatching(String named_graph, String property_name, String property_value) {
		Vector w = owlSPARQLUtils.findConceptsWithPropertyMatching(named_graph, property_name, property_value);
		Utils.saveToFile("Retired_Concept.txt", w);
		return w;
	}


	public String cnstruct_get_axiom_data(String named_graph) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?a_target ?q1_label ?q1_value ?q2_label ?q2_value").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            	?x a owl:Class .").append("\n");
		buf.append("            	?x :NHC0 ?x_code .").append("\n");
		buf.append("            	?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
		buf.append(" ").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?p2 :NHC0 \"P90\"^^xsd:string .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a_target .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q1 :NHC0 \"P384\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q2 :NHC0 \"P383\"^^xsd:string .  ").append("\n");
		buf.append("                ?q2 rdfs:label ?q2_label . ").append("\n");
		buf.append("                ?a1 ?q2 ?q2_value .").append("\n");
		buf.append("").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector getAxiomData(String named_graph) {
		String query = cnstruct_get_axiom_data(named_graph);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		//v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}


	public String construct_get_contributing_source(String named_graph) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x1_label ?x1_code ?p_label ?p_value ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            	?x1 a owl:Class .").append("\n");
		buf.append("            	?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("             	?x1 rdfs:label ?x1_label .").append("\n");
		buf.append(" ").append("\n");
		buf.append("                ?x1 ?p ?p_value .").append("\n");
		buf.append("                ?p :NHC0 ?p_code .").append("\n");
		buf.append("                ?p :NHC0 \"P322\"^^xsd:string .").append("\n");
		buf.append("                ?p rdfs:label ?p_label .").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getContributingSource(String named_graph) {

		System.out.println("************************ getContributingSource ");
		String query = construct_get_contributing_source(named_graph);
		Vector v = owlSPARQLUtils.executeQuery(query);

		if (v == null) {
			//System.out.println("************************ v=null??? ");
			return null;
		}
		//System.out.println("************************ v: " + v.size());
		//Utils.dumpVector("getContributingSource", v);

		if (v.size() == 0) return v;
		//????????????????????????????????????????????????????????????????
		//v = new ParserUtils().getResponseValues(v);

		Utils.saveToFile("debug_cs.txt", v);

		return new SortUtils().quickSort(v);
	}

	public String getVersion() {
		Vector v = owlSPARQLUtils.get_ontology_info(namedGraph);
		Utils.dumpVector("get_ncit_version", v);
		String line = (String) v.elementAt(0);
		Vector u = StringUtils.parseData(line, '|');
		String ncit_version = (String) u.elementAt(0);
		System.out.println(ncit_version);
		return ncit_version;
	}

//Activity|C43431|Contributing_Source|BRIDG
	public Vector removedRetired(Vector w) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			if (!retired_concepts.contains(code)) {
				v.add(line);
			}
		}
		return v;
	}

    public static void runPieChart(String cmd) {
		try {
			Runtime run = Runtime.getRuntime();
			Process pr = run.exec(cmd);
			pr.waitFor();
			BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = "";
			while ((line=buf.readLine())!=null) {
				System.out.println(line);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

    public static void runBatchFile(String cmd) {
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(cmd);
	    try {
			Process process = processBuilder.start();
			StringBuilder output = new StringBuilder();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}
			int exitVal = process.waitFor();
			if (exitVal == 0) {
				System.out.println("Success!");
				System.out.println(output);
				System.exit(0);
			} else {

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main1(String[] args) {
		long ms = System.currentTimeMillis();
		String restURL = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];

		NCItWikiUtils ncitWikiUtils = new NCItWikiUtils(restURL, namedGraph, username, password);

        //Vector w = ncitWikiUtils.getAxiomData(namedGraph);
        //w = ncitWikiUtils.removedRetired(w);
        //Utils.saveToFile("data.txt", w);
        Vector w = ncitWikiUtils.getContributingSource(namedGraph);
        //Utils.saveToFile("cs_data_0.txt", w);
        w = ncitWikiUtils.removedRetired(w);
        Utils.saveToFile("cs_data.txt", w);

        String version = ncitWikiUtils.getVersion();
		System.out.println("NCIt version: " + version);

		ApachePoiPieChartCS.generatePieChart(version);
		//ApachePoiBarChart.generateBarChart(version);

	}

	public static void main(String[] args) {
		System.out.println("************************** NCItWikiUtils **************************");
		long ms = System.currentTimeMillis();
		/*
		String restURL = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		*/

		String restURL = ConfigurationController.serviceUrl;
		String namedGraph = ConfigurationController.namedGraph;
		String username = ConfigurationController.username;
		String password = ConfigurationController.password;

		NCItWikiUtils ncitWikiUtils = new NCItWikiUtils(restURL, namedGraph, username, password);

        Vector w = ncitWikiUtils.getContributingSource(namedGraph);
        w = ncitWikiUtils.removedRetired(w);
        Utils.saveToFile("cs_data.txt", w);

        System.out.println("************************** getVersion **************************");
        String version = ncitWikiUtils.getVersion();
		System.out.println("NCIt version: " + version);
		ApachePoiPieChartCS.generatePieChart(version);
		//ApachePoiBarChart.generateBarChart(version);
	}

}