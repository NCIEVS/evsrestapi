package gov.nih.nci.evs.restapi.test;

import gov.nih.nci.evs.restapi.util.*;
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

public class TestOWLSPARQLUtils {
	OWLSPARQLUtils owlSPARQLUtils = null;
	String serviceUrl = null;
	String version = null;
	String named_graph = null;

    public TestOWLSPARQLUtils(String serviceUrl, String named_graph) {
		if (!serviceUrl.endsWith("?query=")) {
			serviceUrl = serviceUrl + "?query=";
		}
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
        this.named_graph = named_graph;
        owlSPARQLUtils.set_named_graph(this.named_graph);

		try {
			new MetadataUtils(serviceUrl).dumpNameVersion2NamedGraphMap();
		} catch (Exception ex) {
			System.out.println("\tWARNING: Exception: serviceUrl: " + serviceUrl);
		}

		version = owlSPARQLUtils.get_ontology_version(named_graph);

    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return owlSPARQLUtils;
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
		String named_graph = args[1]; //test.getNamedGraph(codingScheme);
		System.out.println(named_graph);
		ParserUtils parser = new ParserUtils();

		TestOWLSPARQLUtils analyzer = new TestOWLSPARQLUtils(serviceUrl, named_graph);

		OWLSPARQLUtils owlSPARQLUtils = analyzer.getOWLSPARQLUtils();
		String inputfile = args[2];
		Vector codes = Utils.readFile(inputfile);
		for (int i=0; i<codes.size(); i++) {
			String concept_code = (String) codes.elementAt(i);
			Vector w = owlSPARQLUtils.getLabelByCode(named_graph, concept_code);
			w = new ParserUtils().getResponseValues(w);
			String label = (String) w.elementAt(0);
			int j = i+1;
			System.out.println("(" + j + ") " + label + " (" + concept_code + ")");
			HashMap prop_map = owlSPARQLUtils.getPropertyHashMapByCode(named_graph, concept_code);
			Utils.dumpMultiValuedHashMap("prop_map", prop_map);

//	public Vector getAxiomsByCode(String named_graph, String code, String propertyName) {


			Vector axioms = owlSPARQLUtils.getAxiomsByCode(named_graph, concept_code, "FULL_SYN");
			Utils.dumpVector("axioms", axioms);

			Vector syns = parser.parseSynonymData(axioms);
			for (int k=0; k<syns.size(); k++) {
				Synonym syn = (Synonym) syns.get(k);
				System.out.println(syn.toJson());
			}

			//List list = parser.getSynonyms(axioms);

			//axioms = parser.getResponseValues(axioms);
			//if (axioms != null) Utils.dumpVector("axioms", axioms);
			/*
			Vector syns = parser.parseSynonymData(axioms);
            */
            /*
			for (int k=0; k<list.size(); k++) {
				//Synonym syn = (Synonym) axioms.get(k);
				//System.out.println(syn.toJson());
				String syn = (String) axioms.get(k);
				System.out.println(syn.toString());
			}
			*/


			Vector superconcept_vec = owlSPARQLUtils.getSuperclassesByCode(named_graph, concept_code);
			superconcept_vec = parser.getResponseValues(superconcept_vec);
			if (superconcept_vec != null) Utils.dumpVector("superconcept_vec", superconcept_vec);
			Vector subconcept_vec = owlSPARQLUtils.getSubclassesByCode(named_graph, concept_code);
			subconcept_vec = parser.getResponseValues(subconcept_vec);
			if (subconcept_vec != null) Utils.dumpVector("subconcept_vec", subconcept_vec);
			Vector role_vec = owlSPARQLUtils.getOutboundRolesByCode(named_graph, concept_code);
			role_vec = parser.getResponseValues(role_vec);
			if (role_vec != null) Utils.dumpVector("role_vec", role_vec);
			Vector inv_role_vec = owlSPARQLUtils.getInboundRolesByCode(named_graph, concept_code);
			inv_role_vec = parser.getResponseValues(inv_role_vec);
			if (inv_role_vec != null) Utils.dumpVector("inv_role_vec", inv_role_vec);
			Vector asso_vec = owlSPARQLUtils.getAssociationsByCode(named_graph, concept_code);
			asso_vec = parser.getResponseValues(asso_vec);
			if (asso_vec != null) Utils.dumpVector("asso_vec", asso_vec);
			Vector inv_asso_vec = owlSPARQLUtils.getInverseAssociationsByCode(named_graph, concept_code);
			inv_asso_vec = parser.getResponseValues(inv_asso_vec);
			if (inv_asso_vec != null) Utils.dumpVector("inv_asso_vec", inv_asso_vec);
		}
	}
}
