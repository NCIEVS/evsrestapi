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

//http://www.hccp.org/java-net-cookie-how-to.html
//https://www.mkyong.com/java/how-to-download-file-from-website-java-jsp/

public class DownloadPage {

	public static String SOURCE_HELP_PAGE_URL = "https://nciterms65.nci.nih.gov/ncitbrowser/pages/source_help_info.jsf?dictionary=NCI_Thesaurus";
	public static String TERM_TYPE_HELP_PAGE_URL = "https://nciterms65.nci.nih.gov/ncitbrowser/pages/term_type_help_info.jsf?dictionary=NCI_Thesaurus";

	public static String TERM_BROWSER_HOME_URL = "https://nciterms65.nci.nih.gov";

	public DownloadPage() {

	}

	public static void dumpVector(String label, Vector v) {
		System.out.println(label);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			System.out.println(t);
		}
	}
/*
    public static void download(String url, String filePath) {
		try {
			URL urlObj = new URL(url);
			URLConnection urlConnection = urlObj.openConnection();
			Charset charset = Charset.forName("UTF8");
			InputStreamReader stream = new InputStreamReader(urlConnection.getInputStream(), charset);
			BufferedReader reader = new BufferedReader(stream);
			StringBuffer responseBuffer = new StringBuffer();
			String line = null;
			while ((line = reader.readLine()) != null) {
				responseBuffer.append(line);
				responseBuffer.append("\n");
			}
			reader.close();
			System.out.println(responseBuffer.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
*/
    public static Vector download(String url) {
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

			//url = "https://google.com";
			//String filePath = "google.html";

			url = SOURCE_HELP_PAGE_URL;
			url = TERM_TYPE_HELP_PAGE_URL;
			String filePath = "help.html";

			//url = TERM_BROWSER_HOME_URL;
			//filePath = "home.html";

			Vector w = download(url);

			//v = WebPageRetrieval.download(url);
			dumpVector(url, w);

			//String retstr = WebPageRetrieval.executeQuery(new URL(url), null, null, "UTF8");
			//System.out.println(retstr);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
