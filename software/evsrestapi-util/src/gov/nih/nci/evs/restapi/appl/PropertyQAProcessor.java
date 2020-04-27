package gov.nih.nci.evs.restapi.appl;

import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

import java.io.*;
//import java.math.*;
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


public class PropertyQAProcessor {
    String named_graph = null;
    String serviceUrl = null;

    private OWLSPARQLUtils owlSPARQLUtils = null;
	private String username = null;
	private String password = null;
	private String namedGraph = null;
	private static String ncit_unii_txt_file = "ncit_unii.txt";
    private HashMap code2LabelMap = null;
    private HashMap code2PropertyMap = null;
    private String property_file = null;
    private HashMap ncit_code2PropertyMap = null;
    private Vector supported_properties = null;
    private HashMap supported_properties_hmap = null;
    private String propertyName = null;

	public PropertyQAProcessor() {

	}


	public PropertyQAProcessor(String serviceUrl, String namedGraph) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		System.out.println("serviceUrl: " + this.serviceUrl);
		System.out.println("namedGraph: " + this.namedGraph);
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		owlSPARQLUtils.set_named_graph(this.namedGraph);
		new MetadataUtils(serviceUrl).dumpNameVersion2NamedGraphMap();
		supported_properties = owlSPARQLUtils.getSupportedProperties(this.namedGraph);
		supported_properties_hmap = gov.nih.nci.evs.restapi.util.StringUtils.constructHashMap(supported_properties, 0, 1, '|');
    }

    private void initialize(String propertyName) {
        if (!supported_properties_hmap.containsKey(propertyName)) {
			System.out.println("Unidentifiable property name: " + propertyName + ". Program aborts.");
			System.exit(1);
		}
		this.propertyName = propertyName;
		property_file = propertyName + ".txt";
		code2LabelMap = new HashMap();
        code2PropertyMap = new HashMap();

        // single valued
        Vector v = null;
        if (!new File(property_file).exists()) {
			System.out.println(property_file + " does not exist.");
			Vector w = owlSPARQLUtils.findConceptsWithProperty(this.namedGraph, propertyName);
			Utils.saveToFile(property_file, w);
		}
		v = Utils.readFile(property_file);
        System.out.println("Number of records in " + property_file + ": " + v.size());
        code2LabelMap = gov.nih.nci.evs.restapi.util.StringUtils.constructHashMap(v, 1, 0, '|');
        // use multiple valued
        ncit_code2PropertyMap = gov.nih.nci.evs.restapi.util.StringUtils.constructMultiValuedHashMap(v, 1, 3, '|');

        Iterator it = ncit_code2PropertyMap.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			if (!code2LabelMap.containsKey(key)) {
				String value = (String) ncit_code2PropertyMap.get(key);
				code2LabelMap.put(key, value);
			}
		}

        System.out.println("Number of records in " + property_file + ": " + v.size());
    }

	public static int countDelimiters(String line, char delim) {
		int knt = 0;
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c == delim) knt++;
		}
		return knt;
	}

	public static boolean checkDelimitedFile(String filename, char delim) {
		Vector v = Utils.readFile(filename);
		String line0 = (String) v.elementAt(0);
		int count = countDelimiters(line0, delim);
		for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			int knt = countDelimiters(line, delim);
			if (knt != count) return false;
		}
		return true;
	}

    public void runQA(String excelfile) {
		String ch = scanCharInFile(excelfile);
		System.out.println("Suggested delimiter: " + ch);
		if (ch.length() == 1) {
			runQA(excelfile, ch.charAt(0));
		}
	}

    public void runQA(String excelfile, char delim) {
        Vector v = ExcelReader.toDelimited(excelfile, delim);
		String heading = (String) v.elementAt(0);
		Vector u = StringUtils.parseData(heading, delim);
		int property_col_index = -1;
		String target = propertyName.replace("_", " ");
		for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			if (t.indexOf(target) != -1) {
				property_col_index = i;
				break;
			}
		}

		int ncit_code_col_index = -1;
		for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			if (t.indexOf("NCIt Code") != -1) {
				ncit_code_col_index = i;
				break;
			}
		}
		System.out.println("ncit_code_col_index: " + ncit_code_col_index);
		System.out.println("property_col_index: " + property_col_index);
        HashMap excel_code2PropertyMap = gov.nih.nci.evs.restapi.util.StringUtils.constructHashMap(v, ncit_code_col_index, property_col_index, true, delim);
		Utils.dumpHashMap("excel_code2PropertyMap", excel_code2PropertyMap);
		reviewCode2PropertyMap(excel_code2PropertyMap);
	}

    public static String scanCharInFile(String filename) {
		System.out.println("Excel file: " + filename);
		String delim_str = "|$\t@%#";
		for (int i=0; i<delim_str.length(); i++) {
			char ch = delim_str.charAt(i);
			System.out.println("\tdelimiter: " + ch);
			Vector v = ExcelReader.toDelimited(filename, ch);
			String workfile = "tmp.txt";
			Utils.saveToFile(workfile, v);
			boolean bool_val = checkDelimitedFile(workfile, ch);
			if (bool_val) return "" + ch;
		}
		return "WARNING: No feasible delimiter found in " + delim_str;
	}

    public static boolean scanCharInFile(String filename, char ch) {
		String target = "" + ch;
		Vector v = Utils.readFile(filename);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.indexOf(target) != -1) {
				return true;
			}
		}
		return false;
	}

	public static Vector parseData(String t, String delimStr) {
		return StringUtils.parseData(t, delimStr);
	}

    public Vector getExcelDataMap(HashMap hmap) {
		Vector v = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String value = (String) hmap.get(key);
			String label = (String) code2LabelMap.get(key);
			v.add(key + "|" + label + "|" + value);
		}
		return v;
	}

	public void reviewCode2PropertyMap(HashMap hmap) {
		Vector warnings = new Vector();
		Vector v = getExcelDataMap(hmap);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			t = t.replace(" || ", "$");
			Vector u = StringUtils.parseData(t, '|');
			String ncit_code = (String) u.elementAt(0);
			String ncit_label = (String) code2LabelMap.get(ncit_code);

			String values = (String) u.elementAt(2);
			u = StringUtils.parseData(values, '$');
			u = new SortUtils().quickSort(u);
			Vector w = getNCItPropertyData(ncit_code);
			w = new SortUtils().quickSort(w);

			if (u == null && w == null) {
				warnings.add("WARNING: Type 1 error " + ncit_label + " (" + ncit_code + ")");
			} else if (u == null && w != null) {
				warnings.add("WARNING: Type 2 error " + ncit_label + " (" + ncit_code + ")");
			} else if (u != null && u.size() > 0 && w == null) {
				if (values != null && values.length() > 0) {
					for (int k=0; k<u.size(); k++) {
						String code = (String) u.elementAt(k);
						warnings.add("WARNING: Type 3 error " + ncit_label + " (" + ncit_code + ")");
						warnings.add("\t" + code);
					}
				}
			} else {

				if (w.size() != u.size()) {
					System.out.println(ncit_label + " (" + ncit_code + ")");
					Utils.dumpVector(ncit_label + " (" + ncit_code + ")", u);
					System.out.println("WARNING: the number of " + propertyName + " properties is different from whatg are in the NCIt.");
					Utils.dumpVector(ncit_label + " (" + ncit_code + ")", w);

					warnings.add(ncit_label + " (" + ncit_code + ")");
					warnings.add("WARNING: the number of " + propertyName + " properties is different from what are in the NCIt.");

				} else {
					boolean same_result = true;
					for (int k=0; k<w.size(); k++) {
						String value_1 = (String) u.elementAt(k);
						String value_2 = (String) w.elementAt(k);

						if (value_1.compareTo(value_2) != 0) {

							System.out.println("value_1(" + k + ") " + value_1);
							System.out.println("value_2(" + k + ") " + value_2);

							//System.out.println(ncit_label + " (" + ncit_code + ")");
							Utils.dumpVector(ncit_label + " (" + ncit_code + ")", u);
							System.out.println("\tWARNING: the values of " + propertyName + " are different from what's in the NCIt.");
							warnings.add(ncit_label + " (" + ncit_code + ")");
							warnings.add("WARNING: the values of " + propertyName + " are different from what's in the NCIt.");
						}
					}
				}
			}
		}
		if (warnings.size() == 0) {
			warnings.add("No " + propertyName + " error is detected.");
		}

		String property_name_lc = propertyName.toLowerCase();
		Utils.saveToFile("warnings_" + property_name_lc + "_" + StringUtils.getToday() + ".txt", warnings);
	}

    public Vector getNCItPropertyData(String code) {
		return (Vector) ncit_code2PropertyMap.get(code);
	}

	public Vector getSupportedProperties() {
		return owlSPARQLUtils.getSupportedProperties(this.namedGraph);
	}

    public static void main(String[] args) {

		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String propertyName = args[2];
		String excelfile = args[3];

		String filename = "GDC_Therapeutic_Agent_Terminology-Task-499_04-24-2020.txt";
		PropertyQAProcessor processor = new PropertyQAProcessor(serviceUrl, namedGraph);
		processor.initialize(propertyName);
        processor.runQA(excelfile);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

}

//C157712$GDC Therapeutic Agent Terminology$C136987$Anti-HER3 Antibody-drug Conjugate U3 1402$$An antibody-drug conjugate (ADC) composed of patritumab, a monoclonal antibody directed against the human epidermal growth factor receptor HER3 (ErbB3),linked to the topoisomerase I inhibitor DX 8951, a semisynthetic, water-soluble derivative of camptothecin, with potential antineoplastic activity. Upon administration of the anti-HER3 ADC U3 1402, the patritumab moiety targets and binds to HER3. After internalization, DX 8951 inhibits topoisomerase I activity by stabilizing the complex between topoisomerase I and DNA and inhibiting religation of DNA breaks, thereby inhibiting DNA replication and triggering apoptotic cell death. HER3, a member of the epidermal growth factor receptor (EGFR) family of receptor tyrosine kinases, is frequently overexpressed in tumors.$$
//Patritumab Deruxtecan|C136987|CAS_Registry|2227102-46-5
