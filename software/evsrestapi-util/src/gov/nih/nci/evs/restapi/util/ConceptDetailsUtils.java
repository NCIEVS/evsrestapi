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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.codec.binary.Base64;
import java.nio.charset.Charset;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class ConceptDetailsUtils {

    static Vector serviceUrl_vec = null;
	static Duration readTimeout;
	static Duration connectTimeout;
	static String SERVICE_URL_DATA_FILE = "service_url_data.txt";

	String username = null;
	String password = null;
	static {
		serviceUrl_vec = Utils.readFile(SERVICE_URL_DATA_FILE);
	}

	private static final Logger log = LoggerFactory.getLogger(ConceptDetailsUtils.class);

	public ConceptDetailsUtils(String username, String password) {
		this.username = username;
		this.password = password;
	}

    public static void testMetadataUtils(int i, String username, String password) {
		String serviceUrl = (String) serviceUrl_vec.elementAt(i);
		new MetadataUtils(serviceUrl, username, password).dumpNameVersion2NamedGraphMap();
	}

    public static void testMetadataUtils(String username, String password) {
		testMetadataUtils(serviceUrl_vec, username, password);
    }

    public static void testMetadataUtils(Vector serviceUrl_vec, String username, String password) {
		Vector w = new Vector();
		Vector warning_vec = new Vector();
    	for (int i=0; i<serviceUrl_vec.size(); i++) {
			String serviceUrl = (String) serviceUrl_vec.elementAt(i);
			int j = i+1;
		    System.out.println("\n(" + j + ") " + serviceUrl);
		    try {
				if (serviceUrl.indexOf("?") != -1) {
					new MetadataUtils(serviceUrl).dumpNameVersion2NamedGraphMap();
					w.add(serviceUrl);
				} else {
					new MetadataUtils(serviceUrl, username, password).dumpNameVersion2NamedGraphMap();
					w.add(serviceUrl);
				}
			} catch (Exception ex) {
				System.out.println("\tWARNING: Exception: serviceUrl: " + serviceUrl);
				warning_vec.add(serviceUrl);
			}
		}
		System.out.println("\nTest Results:");
		Utils.dumpVector("Service URLs", serviceUrl_vec);

		System.out.println("\nThe following serviceURL return data successfully:");
		Utils.dumpVector("Service URLs", w);

		System.out.println("\nThe following serviceURL throw exceptions:");
		Utils.dumpVector("Service URLs", warning_vec);
	}


    public static boolean testMetadataUtils(String serviceUrl) {
		Vector w = new Vector();
		Vector warning_vec = new Vector();
	    System.out.println("\n" + serviceUrl);
		try {
			new MetadataUtils(serviceUrl).dumpNameVersion2NamedGraphMap();
			return true;
		} catch (Exception ex) {
			System.out.println("\tWARNING: Exception: serviceUrl: " + serviceUrl);
			return false;
		}
	}

    public static boolean testMetadataUtils(String serviceUrl, String username, String password) {
		Vector w = new Vector();
		Vector warning_vec = new Vector();
	    System.out.println("\n" + serviceUrl);
		try {
			new MetadataUtils(serviceUrl, username, password).dumpNameVersion2NamedGraphMap();
			return true;
		} catch (Exception ex) {
			System.out.println("\tWARNING: Exception: serviceUrl: " + serviceUrl);
			return false;
		}
	}

//NCI_Thesaurus|20.05a --> http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl
    public Vector searchAvailableServiceUrl(String codingScheme, String version) {
        Vector w = new Vector();
        String target = codingScheme + "|" + version;
    	for (int i=0; i<serviceUrl_vec.size(); i++) {
			String serviceUrl = (String) serviceUrl_vec.elementAt(i);
			int j = i+1;
		    try {
				MetadataUtils mdu = null;
				if (serviceUrl.indexOf("?") != -1) {
					mdu = new MetadataUtils(serviceUrl);
				} else {
					mdu = new MetadataUtils(serviceUrl, username, password);
				}
				HashMap hmap = mdu.getNameVersion2NamedGraphMap();
				if (hmap.containsKey(target)) {
					w.add(serviceUrl);
				}
			} catch (Exception ex) {

			}
		}
		return w;
	}


    public void getConceptDetailsUtils(String serviceUrl, String username, String password, String concept_code) {
		String named_graph = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
		OWLSPARQLUtils owlSPARQLUtils = null;
		ParserUtils parser = new ParserUtils();
		if (serviceUrl.indexOf("?") != -1) {
			owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		} else {
			owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		}

		Vector w = owlSPARQLUtils.getLabelByCode(named_graph, concept_code);
		w = new ParserUtils().getResponseValues(w);
		String label = (String) w.elementAt(0);
		System.out.println(label + " (" + concept_code + ")");
		HashMap prop_map = owlSPARQLUtils.getPropertyHashMapByCode(named_graph, concept_code);
		Utils.dumpMultiValuedHashMap("prop_map", prop_map);

		Vector axioms = owlSPARQLUtils.getAxiomsByCode(named_graph, concept_code, "FULL_SYN");
		Utils.dumpVector("axioms", axioms);

		Vector syns = parser.parseSynonymData(axioms);
		for (int k=0; k<syns.size(); k++) {
			Synonym syn = (Synonym) syns.get(k);
			System.out.println(syn.toJson());
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


    public void getConceptDetails(String codingScheme, String version, String concept_code) {
        Vector serviceUrl_vec = searchAvailableServiceUrl(codingScheme, version);
        if (serviceUrl_vec == null || serviceUrl_vec.size() == 0) return;
        String serviceUrl = (String) serviceUrl_vec.elementAt(0);
        getConceptDetailsUtils(serviceUrl, this.username, this.password, concept_code);
	}



    public static void main(String[] args) {
		String username = args[0];
		String password = args[1];
		String version = args[2];
		String concept_code = args[3];
		ConceptDetailsUtils cdu = new ConceptDetailsUtils(username, password);
		String codingScheme = "NCI_Thesaurus";
		Vector v = cdu.searchAvailableServiceUrl(codingScheme, version);
		Utils.dumpVector("Service URLs supporting " + version, v);
		cdu.getConceptDetails(codingScheme, version, concept_code);
	}
}
