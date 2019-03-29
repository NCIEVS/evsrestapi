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

import gov.nih.nci.evs.restapi.util.*;

//http://www.hccp.org/java-net-cookie-how-to.html
//https://www.mkyong.com/java/how-to-download-file-from-website-java-jsp/

public class MetadataExtractor {

	public static String SOURCE_HELP_PAGE_URL = "https://nciterms65.nci.nih.gov/ncitbrowser/pages/source_help_info.jsf?dictionary=NCI_Thesaurus";
	public static String TERM_TYPE_HELP_PAGE_URL = "https://nciterms65.nci.nih.gov/ncitbrowser/pages/term_type_help_info.jsf?dictionary=NCI_Thesaurus";

	public static String TERM_BROWSER_HOME_URL = "https://nciterms65.nci.nih.gov";

	public MetadataExtractor() {

	}

	public static HashMap extractData(String url) {
		Vector w = DownloadPage.download(url);
		HashMap hmap = new HashMap();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			line = line.trim();
			if (line.length() > 0) {
				if (line.startsWith("<tr class=\"dataRow")) {
					String s = (String) w.elementAt(i+1);
					String t = (String) w.elementAt(i+2);
					int n = s.indexOf(">");
					s = s.substring(n+1, s.length());
					n = s.lastIndexOf("<");
					s = s.substring(0, n);

					n = t.lastIndexOf("<");
					t = t.substring(0, n);
					n = t.lastIndexOf(">");
					t = t.substring(n+1, t.length());

					//System.out.println(s + "-->" + t);
					if (!(s.compareTo("Name") == 0 && t.compareTo("Description") == 0)) {
						if (!(s.compareTo("Source") == 0 && t.compareTo("Description") == 0)) {
							s = s.replaceAll("&#45;", "-");
							hmap.put(s, t);
						}
					}
				}
			}
		}
		return hmap;
	}

	public static void dumpVector(String label, Vector v) {
		System.out.println(label);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			System.out.println(t);
		}
	}

	public static void dumpHashMap(String label, HashMap hmap) {
		System.out.println(label);
		Vector v = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			v.add(key);
		}
		v = new SortUtils().quickSort(v);
		for (int i=0; i<v.size(); i++) {
			String key = (String) v.elementAt(i);
			String value = (String) hmap.get(key);
			System.out.println(key + " -> " + value);
		}
	}

    public static Vector download(String url, String filePath) {
		Vector w = new Vector();
		try {
			URL urlObj = new URL(url);
			URLConnection urlConnection = urlObj.openConnection();
			Charset charset = Charset.forName("UTF8");
			InputStreamReader stream = new InputStreamReader(urlConnection.getInputStream(), charset);
			BufferedReader reader = new BufferedReader(stream);
			StringBuffer responseBuffer = new StringBuffer();
			String line = null;
			while ((line = reader.readLine()) != null) {
				w.add(line);
			}
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return w;
	}

	public static void main(String[] args) {
		String url = SOURCE_HELP_PAGE_URL;
	    Vector v = null;
	    try {
			url = SOURCE_HELP_PAGE_URL;
			HashMap hmap = extractData(SOURCE_HELP_PAGE_URL);
			dumpHashMap("\nTerm Source", hmap);

			url = TERM_TYPE_HELP_PAGE_URL;
			hmap = extractData(TERM_TYPE_HELP_PAGE_URL);
			dumpHashMap("\nTerm Type", hmap);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
