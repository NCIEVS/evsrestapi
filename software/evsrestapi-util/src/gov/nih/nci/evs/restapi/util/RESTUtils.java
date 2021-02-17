package gov.nih.nci.evs.restapi.util;

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

public class RESTUtils {

	private static final Logger log = LoggerFactory.getLogger(RESTUtils.class);

	private String username = null;
	private String password = null;
	private Duration readTimeout;
	private Duration connectTimeout;

	public RESTUtils () {}

	public RESTUtils(long readTimeout, long connectTimeout) {
		this.readTimeout= Duration.ofSeconds(readTimeout);
		this.connectTimeout =  Duration.ofSeconds(connectTimeout);
	}

	public RESTUtils(String username, String password,long readTimeout, long connectTimeout) {
		this.username = username;
		this.password = password;
		this.readTimeout= Duration.ofSeconds(readTimeout);
		this.connectTimeout =  Duration.ofSeconds(connectTimeout);
	}

	public String runSPARQL(String query, String restURL) {
		RestTemplate restTemplate = null;
		if (username != null) {
			restTemplate = new RestTemplateBuilder().
												rootUri(restURL).
												basicAuthentication(username,password).
												setReadTimeout(readTimeout).
												setConnectTimeout(connectTimeout).
												build();
	    } else {
			restTemplate = new RestTemplateBuilder().
												rootUri(restURL).
												setReadTimeout(readTimeout).
												setConnectTimeout(connectTimeout).
												build();
		}

		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
		MultiValueMap <String,String> body = new LinkedMultiValueMap<String,String>();
		body.add("query", query);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(Arrays.asList(new MediaType("application","sparql-results+json")));
		HttpEntity<?> entity = new HttpEntity<Object>(body ,headers);
		String results = restTemplate.postForObject(restURL,entity,String.class);
		return results;
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String replaceAll(String s) {
		if (s == null) return null;
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c == '+') {
			    buf.append("%20");
			} else{
				buf.append(c);
			}
		}

		s = buf.toString();
		s = s.replaceAll("%28", "(");
		s = s.replaceAll("%29", ")");
		return s;
	}

    public static String encode(String query) {
		try {
			String retstr = String.format("%s", URLEncoder.encode(query, "UTF-8"));
			retstr = replaceAll(retstr);
			return retstr;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static String loadQuery(String filename, boolean encode) {
	    StringBuffer buf = new StringBuffer();
	    Vector v = readFile(filename);
	    for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (!t.startsWith("#")) {
			    buf.append(t);
			    if (!encode) {
					buf.append("\n");
				}
		    }
		}
		String query = buf.toString();
		if (encode) {
			query = encode(query);
		}
		return query;
	}

	public static String loadQuery(String filename) {
		return loadQuery(filename, true);
	}


	public static Vector readFile(String filename)
	{
		Vector v = new Vector();
		try {
            FileReader a = new FileReader(filename);
            BufferedReader br = new BufferedReader(a);
            String line;
            line = br.readLine();
            while(line != null){
                v.add(line);
                line = br.readLine();
            }
            br.close();
		} catch (Exception ex) {
            ex.printStackTrace();
		}
		return v;
	}

    public static void testServer(String serviceUrl) {
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl);
		Vector v = owlSPARQLUtils.get_ontology_info();
		Utils.dumpVector("get_ontology_info", v);
	}
}

