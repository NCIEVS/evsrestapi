package gov.nih.nci.evs.restapi.test;

import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;
import gov.nih.nci.evs.restapi.util.*;

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
import java.util.Random;

public class Test {
	OWLSPARQLUtils owlSPARQLUtils = null;
	String serviceUrl = null;
	String named_graph = null;
	String version = null;
	String codingScheme = "NCI_Thesaurus";
	String sparql_endpoint = null;

	public Test(String serviceUrl) {
        this.serviceUrl = serviceUrl;
		this.sparql_endpoint = this.serviceUrl + "?query=";
		this.owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint);
	}

	public HashMap getPropertyHashMapByCode(String named_graph, String code) {
		HashMap hmap = owlSPARQLUtils.getPropertyHashMapByCode(named_graph, code);
		return hmap;
	}

    OWLSPARQLUtils getOWLSPARQLUtils() {
		return owlSPARQLUtils;
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];

		System.out.println(serviceUrl);
		MetadataUtils metadataUtils = new MetadataUtils(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		long ms = System.currentTimeMillis();
		String version = metadataUtils.getLatestVersion(codingScheme);
		System.out.println(codingScheme);
		System.out.println(version);

		String named_graph = metadataUtils.getNamedGraph(codingScheme);
		System.out.println(named_graph);

		Test test = new Test(serviceUrl);

        Vector w = test.getOWLSPARQLUtils().getObjectProperties(named_graph);
		StringUtils.dumpVector("w", w);

		String code = "C12356";
		HashMap hmap = test.getOWLSPARQLUtils().getPropertyHashMapByCode(named_graph, code);
		StringUtils.dumpHashMap("getPropertyHashMapByCode", hmap);
    }
}