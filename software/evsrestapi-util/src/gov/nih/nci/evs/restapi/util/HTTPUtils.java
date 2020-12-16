package gov.nih.nci.evs.restapi.util;

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
import java.nio.charset.Charset;

import java.time.Duration;
/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2020 MSC. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by MSC and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "MSC" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or MSC
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      MSC, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@nih.gov
 *
 */


public class HTTPUtils {
    public static String DEFAULT_ACCEPT_FORMAT = "application/json";
    public static String SPARQL_JSON_ACCEPT_FORMAT = "application/sparql-results+json";

    private String serviceUrl = null;
    private String username = null;
    private String password = null;

	private long readTimeout;
	private long connectTimeout;

	private Duration readTimeoutDuration;
	private Duration connectTimeoutDuration;


	private String restURL = null;

    public HTTPUtils() {

	}

    public HTTPUtils(String serviceUrl) {
		//serviceUrl = verifyServiceUrl(serviceUrl);
		this.serviceUrl = serviceUrl;
	}

	public String verifyServiceUrl(String serviceUrl) {
		if (serviceUrl.indexOf("?query=?query=") != -1) {
			int n = serviceUrl.lastIndexOf("?");
			serviceUrl = serviceUrl.substring(0, n);
		} else if (serviceUrl.indexOf("?") == -1) {
			serviceUrl = serviceUrl + "?query=";
		}
		return serviceUrl;
	}

    public HTTPUtils(String restURL, String username, String password) {
		this.restURL = restURL;
		this.serviceUrl = restURL; //verifyServiceUrl(restURL);
		this.serviceUrl = serviceUrl;
		this.username = username;
		this.password = password;
	}

	public HTTPUtils(String username, String password, long readTimeout, long connectTimeout) {
		this.username = username;
		this.password = password;

		this.readTimeout= readTimeout;
		this.connectTimeout =  connectTimeout;

		this.readTimeoutDuration = Duration.ofSeconds(readTimeout);
		this.connectTimeoutDuration = Duration.ofSeconds(connectTimeout);
    }

	public HTTPUtils(String username, String password) {
		this.username = username;
		this.password = password;
		this.readTimeoutDuration = Duration.ofSeconds(600000);
		this.connectTimeoutDuration =  Duration.ofSeconds(600000);
    }

	public String runSPARQL(String query) {
		if (restURL == null) {
			return null;
		}
		return new RESTUtils(username, password, readTimeout, connectTimeout).runSPARQL(query, restURL);
	}

	public String runSPARQL(String query, String restURL) {
		return new RESTUtils(username, password, readTimeout, connectTimeout).runSPARQL(query, restURL);
	}


	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getServiceUrl() {
		return this.serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public String getRestURL() {
		return this.restURL;
	}

	public void setRestURL(String restURL) {
		this.restURL = restURL;
	}

	public String loadQuery(String filename, boolean encode) {
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

	public String loadQuery(String filename) {
		return loadQuery(filename, true);
	}


	public Vector readFile(String filename)
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

    public String replaceAll(String s) {
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

    public String encode(String query) {
		try {
			String retstr = String.format("%s", URLEncoder.encode(query, "UTF-8"));
			retstr = replaceAll(retstr);
			return retstr;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public URL getPageURL(String query) {
		try {
			String t = this.serviceUrl + query;
			URL url = new URL(t);
			return url;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public URL getPageURL(String serviceUrl, String query) {
		try {
			String t = serviceUrl + query;
			URL url = new URL(t);
			return url;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}


	public String executeQuery(String query) throws Exception {
		return executeQuery(getPageURL(query), this.username, this.password, SPARQL_JSON_ACCEPT_FORMAT);
	}


	public String executeQuery(String page_url, String acceptFormat) throws Exception {
        URL url = new URL(page_url);
	    return executeQuery(url, null, null, acceptFormat);
    }

	public String executeQuery(String query, String username, String password, String acceptFormat) throws Exception {
        URL url = getPageURL(serviceUrl, query);
	    return executeQuery(url, username, password, acceptFormat);
    }

	public String executeQuery(URL url, String username, String password, String acceptFormat) throws Exception {
        if (url == null) return null;
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty("Accept", acceptFormat);

		if (username != null && password != null) {
			String authenticationStr = username + ":" + password;
			String encodedAuthenticationStr  = new String(Base64.encodeBase64(authenticationStr.getBytes()));
			urlConnection.setRequestProperty("Authorization", "Basic " + encodedAuthenticationStr);
		}

		Charset charset = Charset.forName("UTF8");
		InputStreamReader stream = new InputStreamReader(urlConnection.getInputStream(), charset);
		BufferedReader reader = new BufferedReader(stream);
		StringBuffer responseBuffer = new StringBuffer();
		String line = null;
		while ((line = reader.readLine())!=null) {
			responseBuffer.append(line);
			responseBuffer.append("\n");
		}
		reader.close();
		return responseBuffer.toString();
	}

    public Vector execute(String restURL, String username, String password, String query) {
		boolean parsevalues = true;
		return execute(restURL, username, password, query, parsevalues);
	}

    public Vector execute(String restURL, String username, String password, String query, boolean parsevalues) {
		HTTPUtils httpUtils = new HTTPUtils(restURL, username, password);
		Vector v = null;
		try {
			String json = httpUtils.runSPARQL(query);
			v = new JSONUtils().parseJSON(json);
			if (parsevalues) {
				v = new ParserUtils().getResponseValues(v);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String restURL = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
		String queryfile = args[4];
		HTTPUtils util = new HTTPUtils();
		String query = util.loadQuery(queryfile, false);
		boolean parsevalues = true;
		Vector w = util.execute(restURL, username, password, query, parsevalues);
		Utils.dumpVector(queryfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));

	}

}






