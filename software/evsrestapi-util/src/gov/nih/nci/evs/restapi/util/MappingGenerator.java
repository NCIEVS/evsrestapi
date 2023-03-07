package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.config.*;
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
 *     Initial implementation kim.ong@nih.gov
 *
 */


/*
mapping_source_shortname=ncit
mapping_target_shortname=chebi
mapping_source_graphname=http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus23.02b.owl
mapping_target_graphname=http://purl.obolibrary.org/obo/chebi/220/chebi.owl
mapping_datafile=NCIt-ChEBI_Mapping.txt
mapping_filename=NCIt_to_ChEBI_Mapping
mapping_source_id=:NHC0
mapping_target_id=oboInOwl:id

source_ns=http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus#
target_ns=http://purl.obolibrary.org/obo/

source_coding_scheme=NCI_Thesaurus
source_coding_scheme_version=23.02b
target_coding_scheme=ChEBI
target_coding_scheme_version=1.0
mapping_name=NCIt_to_ChEBI_Mapping
mapping_version=1.0
ontology_display_label=NCIt to ChEBI Mapping
ontology_version_info=1.0
ontology_release_date=2022-03-28
ontology_description=>This is a unidirectional mapping from NCI Thesaurus, version 23.02b, to ChEBI vocabulary, version 1.0.
*/



public class MappingGenerator {
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String restURL = null;
    String named_graph_id = ":NHC0";
    String BASE_URI = "" + named_graph + "";

    static final String MAPPING_NAME = "mapping_name";
    static final String MAPPING_VERSION = "ontology_version_info";
    static final String RELEASE_DATE = "ontology_release_date";
    static final String MAPPING_DESCRIPTION = "ontology_description";
    static final String MAPPING_DISPLAY_NAME = "ontology_display_label";

    static String SOURCE_NS = "";
    static String TARGET_NS = "";

    ParserUtils parser = new ParserUtils();
    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String username = null;
    String password = null;
    OWLSPARQLUtils owlSPARQLUtils = null;

    public MappingGenerator(String serviceUrl, String named_graph, String username, String password) {
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

//oboInOwl:id
	public String construct_get_label(String named_graph, String id) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("                ?x a owl:Class .").append("\n");
		buf.append("                ?x " + id + " ?x_code .").append("\n");
		buf.append("                ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("}").append("\n");
		buf.append("order by ?x_label").append("\n");
		buf.append(" ").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}

	public Vector getLabel(String named_graph, String id) {
		String query = construct_get_label(named_graph, id);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public static HashMap createCode2LabelMap(Vector v) {
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			hmap.put(code, label);
		}
		return hmap;
	}

    public static void run(Vector v, String outputfile) {
		String SOURCE_NS = ConfigurationController.source_ns;
		String TARGET_NS = ConfigurationController.target_ns;
		MappingGenerator.setSOURCE_NS(SOURCE_NS);
		MappingGenerator.setTARGET_NS(TARGET_NS);

		System.out.println("SOURCE_NS: " + SOURCE_NS);
		System.out.println("TARGET_NS: " + TARGET_NS);
        long ms = System.currentTimeMillis();
        HashMap metadataHashMap = new HashMap();

        metadataHashMap.put(MAPPING_NAME, ConfigurationController.mapping_name);
        metadataHashMap.put(MAPPING_VERSION, ConfigurationController.ontology_version_info);
        metadataHashMap.put(RELEASE_DATE, ConfigurationController.ontology_release_date);
        metadataHashMap.put(MAPPING_DESCRIPTION, ConfigurationController.ontology_description);

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			writeMetadata(pw, metadataHashMap);
			writeAnnotationProperties(pw);
			writeSourceConcepts(pw, v);
			writeTargetConcepts(pw, v);
			writeFooter(pw);

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
				System.out.println("Graph name: " + "http://ncicb.nci.nih.gov/xml/owl/EVS/" + (String) metadataHashMap.get(MAPPING_NAME) + ".owl");

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void writeMetadata(PrintWriter out, HashMap hmap) {
		String mappingName=(String) hmap.get(MAPPING_NAME);
		String mappingVersion=(String) hmap.get(MAPPING_VERSION);
		String releaseDate=(String) hmap.get(RELEASE_DATE);
		String mappingDescription=(String) hmap.get(MAPPING_DESCRIPTION);

		out.println("<?xml version=\"1.0\"?>");
		out.println("<rdf:RDF xmlns=\"http://ncicb.nci.nih.gov/xml/owl/EVS/" + mappingName + ".owl#\"");
		out.println("     xml:base=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl\"");
		out.println("     xmlns:ncit=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl\"");
		out.println("     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
		out.println("     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"");
		out.println("     xmlns:oboInOwl=\"http://www.geneontology.org/formats/oboInOwl#\"");
		//out.println("     xmlns:Thesaurus=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#\"");
		out.println("     xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"");
		//out.println("     xmlns:protege=\"http://protege.stanford.edu/plugins/owl/protege#\"");
		out.println("     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
		out.println("     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
		out.println("     xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
		out.println("     xmlns:obo=\"http://purl.obolibrary.org/obo/\">");
		out.println("     xmlns:go=\"http://purl.obolibrary.org/obo/go#\"");
		out.println("");

		out.println("    <owl:Ontology rdf:about=\"" + mappingName + "\">");
		out.println("        <owl:versionInfo>" + mappingVersion + "</owl:versionInfo>");
		out.println("        <protege:defaultLanguage>en</protege:defaultLanguage>");
		out.println("        <dc:date>" + releaseDate + "</dc:date>");
		out.println("        <rdfs:comment>" + mappingDescription + "</rdfs:comment>");
		out.println("    </owl:Ontology>");
		out.println("    ");
	}


	public static void writeAnnotationProperties(PrintWriter out) {
		if (!isMappingRankApplicable()) {
			return;
		}

		out.println("    <!-- ");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("    //");
		out.println("    // Annotation properties");
		out.println("    //");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("     -->");
		out.println("     ");
		out.println("    <!-- http://LexGrid.org/schema/2010/01/LexGrid/naming#P001 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://http://LexGrid.org/schema/2010/01/LexGrid/naming#P001\">");
		out.println("        <ncit:NHC0>P001</ncit:NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>Map Rank</ncit:P108>");
		out.println("        <rdfs:label>score</rdfs:label>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    ");
	}

	public static void writeSourceConcepts(PrintWriter out, Vector mappingData) {
		out.println("    <!-- ");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("    //");
		out.println("    // Classes - Source Terminology");
		out.println("    //");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("     -->");
		out.println("");

        for (int i=1; i<mappingData.size(); i++) {
			String line = (String) mappingData.elementAt(i);

			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String sourceCode = (String) u.elementAt(0);
			String sourceName = (String) u.elementAt(1);
			String sourceCodingScheme = (String) u.elementAt(2);

			out.println("");
			out.println("    <!-- http://" + SOURCE_NS + sourceCode + " -->");
            out.println("");
			out.println("    <owl:Class rdf:about=\"" + SOURCE_NS + sourceCode + "\">");
			out.println("        <rdfs:label>" + sourceName + "</rdfs:label>");
			out.println("        <ncit:P375>" + sourceName + "</ncit:P375>");
            out.println("    </owl:Class>");

			writeAxiom(out, line);

			out.println("");
		}
	}


	public static void writeTargetConcepts(PrintWriter out, Vector mappingData) {
		out.println("    <!-- ");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("    //");
		out.println("    // Classes - Target Terminology");
		out.println("    //");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("     -->");
		out.println("");

        for (int i=1; i<mappingData.size(); i++) {
			String line = (String) mappingData.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '|');
			String targetCode = (String) u.elementAt(4);
			String targetName = (String) u.elementAt(5);
			String targetCodingScheme = (String) u.elementAt(6);
			String targetCodingSchemeVersion = (String) u.elementAt(7);

			out.println("");
			out.println("    <!-- http://" + TARGET_NS + targetCode + " -->");

			out.println("    <owl:Class rdf:about=\"" + TARGET_NS + targetCode + "\">");
			out.println("        <rdfs:label>" + targetName + "</rdfs:label>");
			out.println("    </owl:Class>");
			out.println("");
		}
	}

	public static boolean isMappingRankApplicable() {
		/*
		String value = (String) metadataHashMap.get(MAPPING_RANK_APPLICABLE);
		if (value == null) {
			return false;
		}
		if (value.compareTo("true") == 0) {
			return true;
		}
		*/
		return false;
	}

/*
	public static void createMetadataHashMap(String metadataXML) {
		metadataHashMap = new HashMap();
		// Instantiate the Factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try (InputStream is = XmlDomParser.readXmlFileIntoInputStream(metadataXML)) {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);

			for (int i=0; i<METADATA.length; i++) {
				String nodeName = METADATA[i];
				Node node = XmlDomParser.searchNode(doc.getChildNodes(), nodeName);
				if (node != null) {
					metadataHashMap.put(nodeName, node.getTextContent());
					System.out.println(nodeName + ":" + node.getTextContent());
				}
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
*/
/*
    public static void run(String metadataXML, String mappingdata, String sourceNS, String targetNS) {
		SOURCE_NS = sourceNS;
		TARGET_NS = targetNS;
		createMetadataHashMap(metadataXML);
		int n = mappingdata.lastIndexOf(".");
		String outputfile = mappingdata.substring(0, n) + "_" + (String) metadataHashMap.get(MAPPING_VERSION) + ".owl";
		Vector v = readFile(mappingdata);
        long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			writeMetadata(pw, metadataHashMap);
			writeAnnotationProperties(pw);
			writeSourceConcepts(pw, v);
			writeTargetConcepts(pw, v);
			writeFooter(pw);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
				System.out.println("Graph name: " + "http://ncicb.nci.nih.gov/xml/owl/EVS/" + (String) metadataHashMap.get(MAPPING_NAME) + ".owl");

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
*/
    public static void writeAxiom(PrintWriter out, String data) {
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(data, '|');
		String source_Code = (String) u.elementAt(0);
		String source_Name = (String) u.elementAt(1);
		String sourceCodingScheme = (String) u.elementAt(2);
		String sourceCodingSchemeVersion = (String) u.elementAt(3);
		String target_Term_Type = "SY";
		String target_Code = (String) u.elementAt(4);
		String target_Name = (String) u.elementAt(5);
		String target_Coding_Scheme = (String) u.elementAt(6);
		String target_Terminology_Version = (String) u.elementAt(7);

		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"" + SOURCE_NS + source_Code + "\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P375\"/>");
		out.println("        <owl:annotatedTarget>" + source_Name + "</owl:annotatedTarget>");
		out.println("        <ncit:P393>Has Synonym</ncit:P393>");

		out.println("        <ncit:P394>" + target_Term_Type + "</ncit:P394>");
		out.println("        <ncit:P395>" + target_Code + "</ncit:P395>");
		out.println("        <ncit:P396>" + target_Coding_Scheme + "</ncit:P396>");
		out.println("        <ncit:P397>" + target_Terminology_Version + "</ncit:P397>");
		out.println("    </owl:Axiom>");
	}

    public static void writeFooter(PrintWriter out) {
		out.println("    ");
		out.println("</rdf:RDF>");
		out.println("");
		out.println("<!-- Generated by MappingGenerator (" + gov.nih.nci.evs.restapi.util.StringUtils.getToday() + ") -->");
		out.println("    ");
	}

	public static void setSOURCE_NS(String source_NS) {
		SOURCE_NS = source_NS;
	}

	public static void setTARGET_NS(String target_NS) {
		TARGET_NS = target_NS;
	}

	public static Vector run() {
	    String mapping_source_shortname = ConfigurationController.mapping_source_shortname;
	    String mapping_target_shortname = ConfigurationController.mapping_target_shortname;
	    String mapping_source_graphname = ConfigurationController.mapping_source_graphname;
	    String mapping_target_graphname = ConfigurationController.mapping_target_graphname;
	    String mapping_datafile = ConfigurationController.mapping_datafile;
        String mapping_filename = ConfigurationController.mapping_filename;
        String serviceUrl = ConfigurationController.serviceUrl;
        String username = ConfigurationController.username;
        String password = ConfigurationController.password;

        String mapping_source_id = ConfigurationController.mapping_source_id;
        String mapping_target_id = ConfigurationController.mapping_target_id;

		String source_coding_scheme = ConfigurationController.source_coding_scheme;
		String source_coding_scheme_version = ConfigurationController.source_coding_scheme_version;
		String target_coding_scheme =  ConfigurationController.target_coding_scheme;
		String target_coding_scheme_version = ConfigurationController.target_coding_scheme_version;
		String mapping_version = ConfigurationController.mapping_version;

        MappingGenerator test = new MappingGenerator(serviceUrl, mapping_source_graphname, username, password);
		Vector v = test.getLabel(mapping_source_graphname, mapping_source_id);
		HashMap hmap1 = createCode2LabelMap(v);

		Utils.saveToFile(mapping_source_shortname + ".txt", v);

        test = new MappingGenerator(serviceUrl, mapping_target_graphname, username, password);
		v = test.getLabel(mapping_target_graphname, mapping_target_id);
		Utils.saveToFile(mapping_target_shortname + ".txt", v);
		HashMap hmap2 = createCode2LabelMap(v);

		v = new Vector();
		Vector w = Utils.readFile(mapping_datafile);
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '\t');
			String sourceCode = (String) u.elementAt(0);
			String targetCode = (String) u.elementAt(1);
			v.add(sourceCode + "|" +
			     (String) hmap1.get(sourceCode) + "|" +
			      source_coding_scheme + "|" +
			      source_coding_scheme_version + "|" +
	              targetCode + "|" +
			     (String) hmap2.get(targetCode) + "|" +
			      target_coding_scheme + "|" +
			      target_coding_scheme_version);
	    }
        return v;

	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
        Vector v = run();
        String outputfile = ConfigurationController.mapping_filename + "_" +
            ConfigurationController.ontology_version_info + ".owl";

        run(v, outputfile);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
    }
}

