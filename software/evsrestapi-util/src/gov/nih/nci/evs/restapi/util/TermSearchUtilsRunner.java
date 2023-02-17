package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.config.*;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

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

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright Copyright 2020 MSC.. This software was developed in conjunction
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
 *     Initial implementation kim.ong@ngc.com
 *
 */


public class TermSearchUtilsRunner {

	public TermSearchUtilsRunner() {

	}

/*
Term	NCIt Codes	NCIt PTs
Right	Right|C25228$Tumor Laterality Right|C160199
Midline	Midline|C81170$Tumor Laterality Midline|C162614
*/

	public static void reformatMatches(String filename) {
		int knt = 0;
        Vector v = Utils.readFile(filename);
        Vector w = new Vector();
        w.add((String) v.elementAt(0));
        for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '\t');
			String nomatch = (String) u.elementAt(u.size()-1);

			if (nomatch.compareTo("No match") != 0) {
				Vector u2 = StringUtils.parseData(nomatch, '$');
				StringBuffer pts_buf = new StringBuffer();
				StringBuffer codes_buf = new StringBuffer();
				HashSet hset = new HashSet();
				for (int j=0; j<u2.size(); j++) {
					String s = (String) u2.elementAt(j);
					Vector u3 = StringUtils.parseData(s, '|');
					if (!hset.contains((String) u3.elementAt(0))) {
						hset.add((String) u3.elementAt(0));
						pts_buf.append((String) u3.elementAt(0)).append("|");
						codes_buf.append((String) u3.elementAt(1)).append("|");
					}
				}
				String pts = pts_buf.toString();
				pts = pts.substring(0, pts.length()-1);
				String codes = codes_buf.toString();
				codes = codes.substring(0, codes.length()-1);

				int m = line.lastIndexOf("\t");
				line = line.substring(0, m);
                w.add(line + "\t" + codes + "\t" + pts);
                System.out.println(line + "\t" + codes + "\t" + pts);
                knt++;

			} else {
				w.add(line + "\t");
			}
		}
		Utils.saveToFile("reformatted_" + filename, w);
		int n = v.size()-1;
		System.out.println("" + knt + " out of " + n + " matches.");
	}

    public static HashMap createCode2LabelMap() {
		HashMap hmap = new HashMap();
		Vector v = Utils.readFile("FULL_SYN.txt");
		for (int i=1; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			hmap.put(code, label);
		}
		return hmap;
	}

    public static HashMap createCode2NCIPTMap() {
		HashMap hmap = new HashMap();
		Vector v = Utils.readFile("FULL_SYN.txt");
		for (int i=1; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String code = (String) u.elementAt(1);
			if (t.indexOf("PT|NCI|") != -1 || t.indexOf("HD|NCI|") != -1) {
				String pt = (String) u.elementAt(3);
				hmap.put(code, pt);
			}
		}
		return hmap;
	}

    public static void main(String [] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = ConfigurationController.serviceUrl;
		String named_graph = ConfigurationController.namedGraph;
		String username = ConfigurationController.username;
		String password = ConfigurationController.password;
		String vbtfiles = ConfigurationController.termfiles;
		String term_file_heading = ConfigurationController.term_file_heading;
		String term_file_delim = ConfigurationController.term_file_delim;
		String match_file_heading = ConfigurationController.match_file_heading;
		String term_column = ConfigurationController.term_column;

		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("named_graph: " + named_graph);
		System.out.println("vbtfiles: " + vbtfiles);
		System.out.println("term_file_heading: " + term_file_heading);
		System.out.println("term_file_delim: " + term_file_delim);
		System.out.println("match_file_heading: " + match_file_heading);
		System.out.println("term_column: " + term_column);

		Vector vbt_files = StringUtils.parseData(vbtfiles, '$');
		Utils.dumpVector(vbtfiles, vbt_files);

        TermSearchUtils termSearchUtils = new TermSearchUtils(serviceUrl, named_graph, username, password);
        termSearchUtils.initialize();

        int vbt_col = Integer.parseInt(term_column);
		char delim = term_file_delim.charAt(0);
        boolean heading = false;
        term_file_heading = term_file_heading.trim();
        if (term_file_heading.length() > 0) {
			heading = true;
		}
		HashMap code2LabelMap = new HashMap();
		HashMap code2NCIPTMap = new HashMap();
		boolean use_pt = false;
		if (match_file_heading.indexOf("PT") != -1) {
			use_pt = true;;
			code2NCIPTMap = createCode2NCIPTMap();
		} else {
			code2LabelMap = createCode2LabelMap();
		}

        for (int i=0; i<vbt_files.size(); i++) {
            String vbtfile = (String) vbt_files.elementAt(i);
            System.out.println("Processing " + vbtfile);
			Vector vbts = Utils.readFile(vbtfile);
			Vector w = new Vector();

			int istart = 0;
			if (heading) {
				istart = 1;
				String firstLine = (String) vbts.elementAt(0);
				if (firstLine.endsWith("\t")) {
					firstLine = firstLine.substring(0, firstLine.length()-1);
				}
				//firstLine = firstLine.replace("" + delim, "\t");
				w.add(firstLine + "\t" + match_file_heading);
			}	else {
				w.add("Term\t" + match_file_heading);
			}

			for (int j=istart; j<vbts.size(); j++) {
				String line = (String) vbts.elementAt(j);
				if (line.endsWith("\t")) {
					line = line.substring(0, line.length()-1);
				}
				Vector u = StringUtils.parseData(line, delim);
				String vbt = (String) u.elementAt(vbt_col);
				String s = vbt;
				String retstr = termSearchUtils.matchTerm(s);

				if (retstr.length() == 0) {
					w.add(line + "\t" + "No match");
				} else {
					HashSet codeHash = new HashSet();
					StringBuffer buf = new StringBuffer();
					Vector u1 = StringUtils.parseData(retstr, '$');
					for (int k=0; k<u1.size(); k++) {
						String match = (String) u1.elementAt(k);
						Vector u2 = StringUtils.parseData(match, '|');
						String t1 = (String) u2.elementAt(0);
						String t2 = (String) u2.elementAt(1);
						if (!codeHash.contains(t2)) {
							codeHash.add(t2);
							if (use_pt) {
								buf.append((String) code2NCIPTMap.get(t2)).append("|").append(t2).append("$");
							} else {
								buf.append((String) code2LabelMap.get(t2)).append("|").append(t2).append("$");
							}
						}
					}
					String t3 = buf.toString();
					t3 = t3.substring(0, t3.length()-1);
					w.add(line + "\t" + t3);
				}
			}
			String outputfile = "result_" + vbtfile;
		    Utils.saveToFile(outputfile, w);
		    reformatMatches(outputfile);
		}
	}
}