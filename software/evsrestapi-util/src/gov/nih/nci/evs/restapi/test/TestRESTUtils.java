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

public class TestRESTUtils {
	OWLSPARQLUtils owlSPARQLUtils = null;
	String serviceUrl = null;
	String version = null;
	String named_graph = null;
    public TestRESTUtils(String serviceUrl, String named_graph, String username, String password) {
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.named_graph = named_graph;
        owlSPARQLUtils.set_named_graph(this.named_graph);
		try {
			//new MetadataUtils(serviceUrl).dumpNameVersion2NamedGraphMap();
		} catch (Exception ex) {
			System.out.println("\tWARNING: Exception: serviceUrl: " + serviceUrl);
		}
		//version = owlSPARQLUtils.get_ontology_version(named_graph);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return owlSPARQLUtils;
	}


	public static void main(String[] args) {
		String serviceUrl = args[0];
		System.out.println(serviceUrl);
		//MetadataUtils metadataUtils = new MetadataUtils(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		long ms = System.currentTimeMillis();
		//String version = metadataUtils.getLatestVersion(codingScheme);
		System.out.println(codingScheme);
		//System.out.println(version);
		String named_graph = args[1];
		System.out.println(named_graph);
		ParserUtils parser = new ParserUtils();

		String username = args[2];
		String password = args[3];

		TestRESTUtils test = new TestRESTUtils(serviceUrl, named_graph, username, password);

		OWLSPARQLUtils owlSPARQLUtils = test.getOWLSPARQLUtils();
		String inputfile = args[4];

		System.out.println(serviceUrl);
		System.out.println(named_graph);
		System.out.println(username);
		System.out.println(password);
		System.out.println(inputfile);

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

			Vector axioms = owlSPARQLUtils.getAxiomsByCode(named_graph, concept_code, "FULL_SYN");
			//Utils.dumpVector("axioms", axioms);

            System.out.println("\nFULL_SYN");
			Vector syns = parser.parseSynonymData(axioms);
			for (int k=0; k<syns.size(); k++) {
				Synonym syn = (Synonym) syns.get(k);
				System.out.println(syn.toJson());
			}

            System.out.println("\nDEFINITION");
			Vector def_vec = owlSPARQLUtils.getDefinitions(named_graph, concept_code, "DEFINITION");
			for (int k2=0; k2<def_vec.size(); k2++) {
				Definition def = (Definition) def_vec.get(k2);
				System.out.println(def.toJson());
			}

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
