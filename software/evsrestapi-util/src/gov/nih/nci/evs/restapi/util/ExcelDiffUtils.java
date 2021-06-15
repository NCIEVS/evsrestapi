package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;
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
import java.text.*;

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


public class ExcelDiffUtils {
    public String datafile1 = null;
    public String datafile2 = null;
    Vector headings = null;

    public ExcelDiffUtils(String datafile1, String datafile2) {
		this.datafile1 = datafile1;
		this.datafile2 = datafile2;
	}

	public Vector split(String line) {
		return StringUtils.parseData(line, '\t');
	}

	public Vector split(String line, char delim) {
		return StringUtils.parseData(line, delim);
	}

	public void run(PrintWriter pw) {
		Vector v1 = Utils.readFile(datafile1);
		String heading = (String) v1.elementAt(0);
		headings = split(heading);
		HashSet codes_1 = new HashSet();
		HashMap hmap_1 = new HashMap();
		for (int i=1; i<v1.size(); i++) {
			String line = (String) v1.elementAt(i);
			line = line.trim();
			if (line.length() >  0) {
				Vector u = split(line, '\t');
				String code = (String) u.elementAt(2);
				if (!codes_1.contains(code)) {
					codes_1.add(code);
				}
				HashMap hmap = new HashMap();
				for (int j=0; j<u.size(); j++) {
					String value = (String) u.elementAt(j);
					hmap.put((String) headings.elementAt(j), value);
				}
				hmap_1.put(code, hmap);
			}
		}

		Vector v2 = Utils.readFile(datafile2);
		heading = (String) v2.elementAt(0);
		heading = heading.trim();
		headings = split(heading);

		HashSet codes_2 = new HashSet();
		HashMap hmap_2 = new HashMap();
		for (int i=1; i<v2.size(); i++) {
			String line = (String) v2.elementAt(i);
			line = line.trim();
			if (line.length() > 0) {
				Vector u = split(line, '\t');
				String code = (String) u.elementAt(2);
				if (!codes_2.contains(code)) {
					codes_2.add(code);
				}
				HashMap hmap = new HashMap();
				for (int j=0; j<u.size(); j++) {
					String value = (String) u.elementAt(j);
					hmap.put((String) headings.elementAt(j), value);
				}
				hmap_2.put(code, hmap);
			}
		}

        pw.println("\nOld file " + datafile1);
        pw.println("New file " + datafile2);
        pw.println("\n");
        dumpVector(pw, "headings", headings);
        pw.println("\nEdit actions:");
		compare(pw, hmap_2, hmap_1);
	}

	public void dumpVector(PrintWriter pw, String label, Vector v) {
	    pw.println(label);
	    for (int i=0; i<v.size(); i++) {
			int j = i+1;
			pw.println("(" + j + ") " + (String) v.elementAt(i));
		}
	}

    public void dumpHashMap(HashMap hmap) {
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String code = (String) it.next();
			System.out.println(code);
		    HashMap map = (HashMap) hmap.get(code);
			Iterator it1 = map.keySet().iterator();
			while (it1.hasNext()) {
				String key = (String) it1.next();
				String value = (String) map.get(key);
				System.out.println("\t" + key + " --> " + value);
			}
		}
	}

	public void compareRow(PrintWriter pw, String code, HashMap hmap_1, HashMap hmap_2) {
		Iterator it1 = hmap_1.keySet().iterator();
		while (it1.hasNext()) {
			String key = (String) it1.next();
			String value_1 = (String) hmap_1.get(key);
			String value_2 = (String) hmap_2.get(key);
			if (value_1.compareTo(value_2) != 0) {
				pw.println("added|" + code + "|" + key + "|" + value_1);
				pw.println("deleted|" + code + "|" + key + "|" + value_2);
			}
		}
	}

	public void compare(PrintWriter pw, HashMap hmap_1, HashMap hmap_2) {
		//hmap_1: New, hmap_2: Old
		Iterator it1 = hmap_1.keySet().iterator();

		//In New, but not in Old
		while (it1.hasNext()) {
			String code = (String) it1.next();
			if (!hmap_2.containsKey(code)) {
				pw.println("added|" + code);
			}
		}
		//In Old, but not in New
        Iterator it2 = hmap_2.keySet().iterator();
		while (it2.hasNext()) {
			String code = (String) it2.next();
			if (!hmap_1.containsKey(code)) {
				pw.println("deleted|" + code);
			}
		}

		it2 = hmap_2.keySet().iterator();
		while (it2.hasNext()) {
			String code = (String) it2.next();
			////In both Old and New
			if (hmap_1.containsKey(code) && hmap_2.containsKey(code)) {
				HashMap map_1 = (HashMap) hmap_2.get(code);
				HashMap map_2 = (HashMap) hmap_1.get(code);
                compareRow(pw, code, map_1, map_2);
			}
		}
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String datafile1 = args[0];
		String datafile2 = args[1];
		ExcelDiffUtils pdfAnalyzer = new ExcelDiffUtils(datafile1, datafile2);

		PrintWriter pw = null;
		int n1 = datafile1.lastIndexOf(".");
		int n2 = datafile2.lastIndexOf(".");
		String outputfile = datafile1.substring(0, n1) + "_" + datafile1.substring(0, n2) + ".txt";
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
            pdfAnalyzer.run(pw);

		} catch (Exception ex) {

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