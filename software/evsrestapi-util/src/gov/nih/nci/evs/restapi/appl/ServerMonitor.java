package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;
import gov.nih.nci.evs.restapi.config.*;

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
//import org.apache.commons.codec.binary.Base64;
import org.json.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2017 NGIS. This software was developed in conjunction
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
 *      "This product includes software developed by NGIS and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIS" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIS
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIS, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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
 *     Initial implementation kim.ong@ngc.com
 *
 */

public class ServerMonitor {
    public static String SERVER_STATUS = "server_status.txt";

	public static void query(String version) throws Exception {
		query("NCI_Thesaurus", version);
	}

	public static void query(String codingScheme, String version) throws Exception {
		if (version == null) {
			version = DateUtils.getNCItMonthlyVersion();
		}
		Vector v = Utils.readFile(SERVER_STATUS);
		Vector w = new Vector();
        String serviceUrl = null;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t.startsWith("(")) {
				int m = t.indexOf(")");
				serviceUrl = t.substring(m+2, t.length());
			} else {
				if (t.startsWith(codingScheme + "|" + version)) {
					w.add(serviceUrl);
					w.add("\t" + t);
				}
			}
		}
		if (w.size() > 0) {
			System.out.println(version + " available at");
			for (int i=0; i<w.size(); i++) {
				String t = (String) w.elementAt(i);
				System.out.println(t);
			}
		} else {
			System.out.println(version + " not available.");
		}
	}

	public static void main(String[] args) {
        long ms = System.currentTimeMillis();
        String outputfile = SERVER_STATUS;
		PrintWriter pw = null;

		HashMap hmap = new HashMap();
		Vector w = new Vector();

		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			String username = ConfigurationController.username;
			String password = ConfigurationController.password;
			String namedGraph = ConfigurationController.namedGraph;
			String serviceUrls = ConfigurationController.serviceUrls;
			Vector serviceUrl_vec = StringUtils.parseData(serviceUrls, '|');
			String codingScheme = "NCI_Thesaurus";
			pw.println("Default namedGraph: " + namedGraph);
			for (int i=0; i<serviceUrl_vec.size(); i++) {
				String serviceUrl = (String) serviceUrl_vec.elementAt(i);
				System.out.println("serviceUrl: " + serviceUrl);
				int j = i+1;
				pw.println("(" + j + ") " + serviceUrl);
				try {
					MetadataUtils test = new MetadataUtils(serviceUrl, username, password);
					String version = test.getLatestVersion(codingScheme);
					pw.println(codingScheme);
					pw.println("latest version: " + version);
					test.dumpNameVersion2NamedGraphMap(pw);
					HashMap nameVersion2NamedGraphMap = test.getNameVersion2NamedGraphMap();
					if (nameVersion2NamedGraphMap != null) {
						hmap.put(serviceUrl, nameVersion2NamedGraphMap);
					}

				} catch (Exception ex) {
					pw.println("Instantiation of MetadataUtils failed.");
				}
			}

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				pw.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			System.out.println(key);
			HashMap nameVersion2NamedGraphMap = (HashMap) hmap.get(key);
			Iterator it2 = nameVersion2NamedGraphMap.keySet().iterator();
			while (it2.hasNext()) {
				String key2 = (String) it2.next();
				Vector vec = (Vector) nameVersion2NamedGraphMap.get(key2);
				for (int i=0; i<vec.size(); i++) {
					String value = (String) vec.elementAt(i);
					System.out.println(key2 + " --> " + value);
					w.add(key + "|" + key2 + "|" + value);
				}
			}
		}
		Utils.saveToFile("stardog_server_status_" + StringUtils.getToday() + ".txt", w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

}

