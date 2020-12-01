package gov.nih.nci.evs.restapi.test;

import gov.nih.nci.evs.restapi.model.*;
import gov.nih.nci.evs.restapi.util.*;
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
import java.nio.charset.Charset;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class Test {

    public Test() {

    }

	public static void main(String[] args) {
		String outputfile = "EVSRESTAPI_Test" + ".txt";
		String terminology = args[0];
		String code = args[1];
        long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			HashMap hmap = EVSRESTAPIClient.EVSRESTAPI_URL_MAP;
			Iterator it = hmap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				String t0 = (String) hmap.get(key);
				String t = t0;
				t = t.replace("{terminology}", terminology);
				t = t.replace("{code}", code);
				if (key.compareTo("metadata_role_codeOrLabel") == 0) {
					t = t.replace("{codeOrLabel}", "R81");
				} else if (key.compareTo("metadata_association_codeOrLabel") == 0) {
					t = t.replace("{codeOrLabel}", "A10");
				} else if (key.compareTo("metadata_property_codeOrLabel") == 0) {
					t = t.replace("{codeOrLabel}", "P322");
				} else if (key.compareTo("metadata_qualifier_codeOrLabel") == 0) {
					t = t.replace("{codeOrLabel}", "P390");
				} else if (key.compareTo("metadata_values") == 0) {
					t = t.replace("{codeOrLabel}", "P383");
				} else if (key.compareTo("pathsToAncestor_ancestorCode") == 0) {
					t = t.replace("{ancestorCode}", "C7057");
				} else if (key.compareTo("concept_terminology") == 0) {
					t = t + "?list=C2291,C3224";
				}
				pw.println("\n" + t0);
				pw.println("Example: " + t);
				String json = EVSRESTAPIClient.getJson(t);
				pw.println(json);
			}
		} catch (Exception ex) {
            ex.printStackTrace();
		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

