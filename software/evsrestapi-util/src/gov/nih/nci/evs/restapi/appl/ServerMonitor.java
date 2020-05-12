package gov.nih.nci.evs.restapi.appl;

import gov.nih.nci.evs.restapi.util.*;
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

public class ServerMonitor {

    static Vector serviceUrl_vec = null;
	static Duration readTimeout;
	static Duration connectTimeout;
	static String SERVICE_URL_DATA_FILE = "service_url_data.txt";
	static {
		serviceUrl_vec = Utils.readFile(SERVICE_URL_DATA_FILE);
	}

	private static final Logger log = LoggerFactory.getLogger(ServerMonitor.class);

	public ServerMonitor () {}

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

    public static void main(String[] args) {
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		testMetadataUtils(username, password);
	}
}
