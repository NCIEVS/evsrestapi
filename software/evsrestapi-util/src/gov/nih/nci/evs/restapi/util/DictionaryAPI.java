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


public class DictionaryAPI {
    public static String DICTIONAY_API_URL = "https://api.dictionaryapi.dev/api/v2/entries/en_US/";

	public DictionaryAPI() {

	}

    public static Vector run(String word) {
		Vector w = null;
	    try {
			String url = DICTIONAY_API_URL + word;
			w = DownloadPage.download(url);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return w;
	}

	public static void main(String[] args) {
        String word = args[0];
	    Vector v = null;
	    try {
			Vector w = run(word);
			Utils.dumpVector(word, w);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
