package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.appl.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import java.text.*;
import java.nio.file.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class MappingDataUtils {
	String serviceUrl = null;
	String namedGraph = null;
	String username = null;
	String password = null;
	OWLSPARQLUtils owlSPARQLUtils = null;

    public MappingDataUtils(String serviceUrl, String namedGraph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		this.username = username;
		this.password = password;
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		owlSPARQLUtils.set_named_graph(namedGraph);
	}

	public String construct_get_axiom_target(String named_graph) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x1_label ?x1_code ?a2_target ").append("\n");
		buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("                    ?x1 a owl:Class .").append("\n");
		buf.append("                    ?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("                    ?x1 rdfs:label ?x1_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a2_target .").append("\n");
		buf.append("                ?p2 :NHC0 \"P90\"^^xsd:string .").append("\n");
		buf.append("                ?q1 :NHC0 \"P384\"^^xsd:string .").append("\n");
		buf.append("                ?a1 ?q1 \"NCI\"^^xsd:string .").append("\n");
		buf.append("                ?q2 :NHC0 \"P383\"^^xsd:string .   ").append("\n");
		buf.append("                ?a1 ?q2 \"PT\"^^xsd:string .").append("\n");
		buf.append("                             ").append("\n");
		buf.append("").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getAxiomTarget(String named_graph) {
		String query = construct_get_axiom_target(named_graph);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_axiom_qualifier_values(String named_graph, String propertyCode, String qualifierCode) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?p2_label ?q1_label ?q1_value").append("\n");
		buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("                    ?x1 a owl:Class .").append("\n");
		buf.append("                    ?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("                    ?x1 rdfs:label ?x1_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a2_target .").append("\n");
		buf.append("                ?p2 :NHC0 \"" + propertyCode + "\"^^xsd:string .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("                ?q1 :NHC0 \"" + qualifierCode + "\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public Vector getAxiomQualifierValues(String named_graph, String propertyCode, String qualifierCode) {
		String query = construct_get_axiom_qualifier_values(named_graph, propertyCode, qualifierCode);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_target_terminology(String named_graph) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?p2_label ?q1_label ?q1_value ?q2_label ?q2_value").append("\n");
		buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("                    ?x1 a owl:Class .").append("\n");
		buf.append("                    ?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("                    ?x1 rdfs:label ?x1_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a2_target .").append("\n");
		buf.append("                ?p2 :NHC0 \"P375\"^^xsd:string .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("                ?q1 :NHC0 \"P396\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("").append("\n");
		buf.append("OPTIONAL {").append("\n");
		buf.append("                ?p3 rdfs:label ?p3_label .").append("\n");
		buf.append("                ?q2 :NHC0 \"P397\"^^xsd:string .").append("\n");
		buf.append("                ?q3 rdfs:label ?q3_label .").append("\n");
		buf.append("                ?a1 ?q2 ?q2_value .").append("\n");
		buf.append("}                             ").append("\n");
		buf.append("").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}


	public Vector getTargetTerminology(String named_graph) {
		String query = construct_get_target_terminology(named_graph);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

	public String construct_get_maps_to(String named_graph, String terminology, String terminologyVersion) {
		String prefixes = owlSPARQLUtils.getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (terminologyVersion != null) {
			buf.append("select distinct ?x1_label ?x1_code ?p2_label ?a2_target ?q1_label ?q1_value ?q2_label ?q2_value ?q3_label ?q3_value ?q4_label ?q4_value ?q5_label ?q5_value").append("\n");
		} else {
			buf.append("select distinct ?x1_label ?x1_code ?p2_label ?a2_target ?q1_label ?q1_value ?q2_label ?q2_value ?q3_label ?q3_value ?q4_label ?q4_value").append("\n");
		}
		buf.append("from <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("where  {").append("\n");
		buf.append("                ?x1 a owl:Class .").append("\n");
		buf.append("                ?x1 :NHC0 ?x1_code .").append("\n");
		buf.append("                ?x1 rdfs:label ?x1_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x1 .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a2_target .").append("\n");
		buf.append("                ?p2 :NHC0 \"P375\"^^xsd:string .").append("\n");
		buf.append("                ?p2 rdfs:label ?p2_label .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q1 :NHC0 \"P393\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q2 :NHC0 \"P394\"^^xsd:string .").append("\n");
		buf.append("                ?q2 rdfs:label ?q2_label .").append("\n");
		buf.append("                ?a1 ?q2 ?q2_value .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q3 :NHC0 \"P395\"^^xsd:string .").append("\n");
		buf.append("                ?q3 rdfs:label ?q3_label .").append("\n");
		buf.append("                ?a1 ?q3 ?q3_value .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q4 :NHC0 \"P396\"^^xsd:string .").append("\n");
		buf.append("                ?q4 rdfs:label ?q4_label .").append("\n");
		buf.append("                ?a1 ?q4 \"" + terminology + "\"^^xsd:string .").append("\n");
		buf.append("                ?a1 ?q4 ?q4_value .").append("\n");
		buf.append("").append("\n");
		if (terminologyVersion != null) {
			buf.append("                ?q5 :NHC0 \"P397\"^^xsd:string .").append("\n");
			buf.append("                ?q5 rdfs:label ?q5_label .").append("\n");
			buf.append("                ?a1 ?q5 \"" + terminologyVersion + "\"^^xsd:string .").append("\n");
			buf.append("                ?a1 ?q5 ?q5_value .").append("\n");
	    }
		buf.append("").append("\n");
		buf.append("}").append("\n");

		return buf.toString();
	}

	public Vector getMapsTo(String named_graph, String terminology, String terminologyVersion) {
		String query = construct_get_maps_to(named_graph, terminology, terminologyVersion);
		Vector v = owlSPARQLUtils.executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		v = new ParserUtils().getResponseValues(v);
		return new SortUtils().quickSort(v);
	}

    public String getLatestVersionOfCodingScheme(String codingScheme) {
		HashMap nameVersion2NamedGraphMap = owlSPARQLUtils.getNameVersion2NamedGraphMap();
		if (nameVersion2NamedGraphMap == null) return null;
		Iterator it = nameVersion2NamedGraphMap.keySet().iterator();
		Vector versions = new Vector();
		while (it.hasNext()) {
			String nameVersion = (String) it.next();
			System.out.println(nameVersion);
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

//C154919,"Bronchopulmonary Dysplasia, AE",NCI_Thesaurus,21.11e,Has Synonym,LLT,"Bronchopulmonary dysplasia",10006475,MedDRA,"22.0"
    public String getHeading() {
		StringBuffer buf = new StringBuffer();
		buf.append("Source Code").append(",");
		buf.append("Source Name").append(",");
		buf.append("Source Terminology").append(",");
		buf.append("Source Terminology Version").append(",");
		buf.append("Relationship to Target").append(",");
		buf.append("Target Term Type").append(",");
		buf.append("Target Name").append(",");
		buf.append("Target Code").append(",");
		buf.append("Target Terminology").append(",");
		buf.append("Target Terminology Version");
        return buf.toString();
	}

    public Vector formatMapsTo(Vector v) {
		String sourceCode = null;
		String sourceName = null;
		String sourceCodingScheme = null;
		String sourceCodingSchemeVersion = null;
		String associationName = null;
		String relationshipToTarget = null;
		String targetTermType = null;
		String targetCode = null;
		String targetName = null;
		String targetCodingScheme = null;
		String targetCodingSchemeVersion = null;
        Vector w = new Vector();
        w.add(getHeading());
        String version = getLatestVersionOfCodingScheme("NCI_Thesaurus");
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			StringBuffer buf = new StringBuffer();
			sourceName = (String) u.elementAt(0);
			sourceName = "\"" + sourceName + "\"";

			targetName = (String) u.elementAt(3);
			targetName = "\"" + targetName + "\"";

			buf.append((String) u.elementAt(1)).append(","); //code
			buf.append(sourceName).append(","); //name
			buf.append("NCI_Thesaurus").append(",");
			buf.append(version).append(",");
			buf.append((String) u.elementAt(5)).append(",");
			buf.append((String) u.elementAt(7)).append(",");

			buf.append(targetName).append(",");

			buf.append((String) u.elementAt(9)).append(",");
			buf.append((String) u.elementAt(11));
			if (u.size() > 12) {
				targetCodingSchemeVersion = (String) u.elementAt(13);
				targetCodingSchemeVersion = "\"" + targetCodingSchemeVersion + "\"";
				buf.append(",");
				buf.append(targetCodingSchemeVersion);
			}
			w.add(buf.toString());
		}
		return w;
	}


    public Vector run(String terminology, String terminologyVersion) {
        Vector w = getMapsTo(namedGraph, terminology, terminologyVersion);
        w = formatMapsTo(w);
        return w;
    }

	public static void main(String[] args) throws Exception {
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
        long ms = System.currentTimeMillis();
        MappingDataUtils utils = new MappingDataUtils(serviceUrl, named_graph, username, password);
        String terminology = "MedDRA";
        String terminologyVersion = "22.0";
        Vector w = utils.run(terminology, terminologyVersion);
        String filename = "NCIt_to_" + terminology + "_mapping_" + terminologyVersion + ".csv";
        Utils.saveToFile(filename, w);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
