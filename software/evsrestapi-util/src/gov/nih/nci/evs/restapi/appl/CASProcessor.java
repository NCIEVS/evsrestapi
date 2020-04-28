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


public class CASProcessor {
    String named_graph = null;
    String serviceUrl = null;

    private OWLSPARQLUtils owlSPARQLUtils = null;
    private Vector unii_link = null;
	private Vector unii_data = null;
	private String username = null;
	private String password = null;
	private String namedGraph = null;
	private static String ncit_unii_txt_file = "ncit_unii.txt";
    private HashMap code2LabelMap = null;
    private HashMap code2CasRegistryMap = null;
    private HashMap code2UNIIMap = null;
    private String unii_file = "FDA_UNII_Code.txt";
    private String case_registry_file = "CAS_Registry.txt";
    private HashMap ncit_code2CasRegistryMap = null;
    private HashMap ncit_code2UNIIMap = null;

	public CASProcessor() {

	}


	public CASProcessor(String serviceUrl, String namedGraph) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		System.out.println("serviceUrl: " + this.serviceUrl);
		System.out.println("namedGraph: " + this.namedGraph);
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		owlSPARQLUtils.set_named_graph(this.namedGraph);
		new MetadataUtils(serviceUrl).dumpNameVersion2NamedGraphMap();

    }

    private void initialize() {
		code2LabelMap = new HashMap();
        code2CasRegistryMap = new HashMap();
        code2UNIIMap = new HashMap();
        // single valued
        Vector v = null;
        if (!new File(unii_file).exists()) {
			System.out.println(unii_file + " does not exist.");
			Vector w = owlSPARQLUtils.findConceptsWithProperty(this.namedGraph, "FDA_UNII_Code");
			Utils.saveToFile(unii_file, w);
		}
		v = Utils.readFile(unii_file);
        System.out.println("Number of records in " + unii_file + ": " + v.size());

        code2LabelMap = gov.nih.nci.evs.restapi.util.StringUtils.constructHashMap(v, 1, 0, '|');
        ncit_code2UNIIMap = gov.nih.nci.evs.restapi.util.StringUtils.constructHashMap(v, 1, 3, '|');

        // multiple valued

        if (!new File(case_registry_file).exists()) {
			System.out.println(case_registry_file + " does not exist.");
			Vector w = owlSPARQLUtils.findConceptsWithProperty(this.namedGraph, "CAS_Registry");
			Utils.saveToFile(case_registry_file, w);
		}
        v = Utils.readFile(case_registry_file);
        HashMap hmap = gov.nih.nci.evs.restapi.util.StringUtils.constructHashMap(v, 1, 0, '|');
        Iterator it = hmap.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			if (!code2LabelMap.containsKey(key)) {
				String value = (String) hmap.get(key);
				code2LabelMap.put(key, value);
			}
		}


		//String test_label = (String) code2LabelMap.get(test_code);
		//System.out.println("**********" + test_code + " -> " + test_label);


		//Utils.dumpHashMap("code2LabelMap", code2LabelMap);

        System.out.println("Number of records in " + case_registry_file + ": " + v.size());
        ncit_code2CasRegistryMap = gov.nih.nci.evs.restapi.util.StringUtils.constructMultiValuedHashMap(v, 1, 3, '|');

		System.out.println(unii_file + ": " + ncit_code2UNIIMap.keySet().size());
		System.out.println(case_registry_file + ": " + ncit_code2CasRegistryMap.keySet().size());


	}



    public void run() {
		String property_name = "CAS_Registry";
		String property_value = null;
		Vector v = owlSPARQLUtils.findConceptsWithPropertyMatching(this.namedGraph, property_name, property_value);
		v = new SortUtils().quickSort(v);
		Utils.saveToFile(property_name + ".txt", v);

		property_name = "FDA_UNII_Code";
		property_value = null;
		v = owlSPARQLUtils.findConceptsWithPropertyMatching(this.namedGraph, property_name, property_value);
		v = new SortUtils().quickSort(v);
		Utils.saveToFile(property_name + ".txt", v);
	}


/*
        (1) NCIt Subset Code
        (2) NCIt Subset Name
        (3) NCIt Code
        (4) NCIt PT
        (5) NCIt Synonyms
        (6) Definition
        (7) CAS Registry
        (8) FDA UNII Code
*/


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
		int registry_col_index = -1;
		for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			if (t.indexOf("CAS Registry") != -1) {
				registry_col_index = i;
				break;
			}
		}
		int fda_unii_code_col_index = -1;
		for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			if (t.indexOf("FDA UNII Code") != -1) {
				fda_unii_code_col_index = i;
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
		System.out.println("registry_col_index: " + registry_col_index);
		System.out.println("fda_unii_code_col_index: " + fda_unii_code_col_index);
        HashMap excel_code2CasRegistryMap = gov.nih.nci.evs.restapi.util.StringUtils.constructHashMap(v, ncit_code_col_index, registry_col_index, true, delim);
        HashMap excel_code2UNIIMap = gov.nih.nci.evs.restapi.util.StringUtils.constructHashMap(v, ncit_code_col_index, fda_unii_code_col_index, true, delim);
		reviewCode2RegistryMap(excel_code2CasRegistryMap);
		reviewCode2UNIIMap(code2UNIIMap);
	}

    public static String scanCharInFile(String filename) {
		String delim_str = "|$\t@%#";
		for (int i=0; i<delim_str.length(); i++) {
			char ch = delim_str.charAt(i);
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


/*
    private HashMap code2CasRegistryMap = null;
    private HashMap code2UNIIMap = null;
 */
    //CasRegistry is multi-valued


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

	public void reviewCode2UNIIMap(HashMap hmap) {
		Vector warnings = new Vector();
		Vector v = getExcelDataMap(hmap);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			t = t.replace(" || ", "$");
			Vector u = StringUtils.parseData(t, '|');
			String ncit_code = (String) u.elementAt(0);
			String ncit_label = (String) code2LabelMap.get(ncit_code);
			String value = (String) u.elementAt(2);
			String ncit_unii_code = (String) ncit_code2UNIIMap.get(ncit_code);
			if (value == null || value.length() == 0) {
				warnings.add("WARNING: missing FDA_UNII_Code - " + ncit_label + " (" + ncit_code + ")");
				warnings.add("\tFDA_UNII_Code: " + ncit_unii_code);
			} else if (value.compareTo(ncit_unii_code) != 0) {
				warnings.add("WARNING: miss matched FDA_UNII_Code - " + ncit_label + " (" + ncit_code + ")");
				warnings.add("\tFDA_UNII_Code: " + ncit_unii_code);
				warnings.add("\tExcel data: " + value);
			}
		}
		if (warnings.size() == 0) {
			warnings.add("No FDA UNII Code error is detected.");
		}
		Utils.saveToFile("warnings_fda_unii_" + StringUtils.getToday() + ".txt", warnings);
	}


	public void reviewCode2RegistryMap(HashMap hmap) {
		//Utils.dumpVector("excel_registry.txt", getExcelDataMap(hmap));
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
			Vector w = getNCItRegistryData(ncit_code);
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
					System.out.println("WARNING: the number of CAS Registry codes is different from what's in the NCIt.");
					Utils.dumpVector(ncit_label + " (" + ncit_code + ")", w);

					warnings.add(ncit_label + " (" + ncit_code + ")");
					warnings.add("WARNING: the number of CAS Registry codes is different from what's in the NCIt.");

				} else {
					boolean same_result = true;
					for (int k=0; k<w.size(); k++) {
						String value_1 = (String) u.elementAt(k);
						String value_2 = (String) w.elementAt(k);
						if (value_1.compareTo(value_2) != 0) {
							System.out.println(ncit_label + " (" + ncit_code + ")");
							Utils.dumpVector(ncit_label + " (" + ncit_code + ")", u);

							System.out.println("WARNING: the values of CAS Registry codes are different from what's in the NCIt.");
							Utils.dumpVector(ncit_label + " (" + ncit_code + ")", w);

							warnings.add(ncit_label + " (" + ncit_code + ")");
							warnings.add("WARNING: the values of CAS Registry codes are different from what's in the NCIt.");
						}
					}
				}
			}
		}
		if (warnings.size() == 0) {
			warnings.add("No CAS Registry code error is detected.");
		}
		Utils.saveToFile("warnings_cas_registry_" + StringUtils.getToday() + ".txt", warnings);
	}

    public Vector getNCItRegistryData(String code) {
		return (Vector) ncit_code2CasRegistryMap.get(code);
	}

	public Vector getSupportedProperties() {
		return owlSPARQLUtils.getSupportedProperties(this.namedGraph);
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String excelfile = args[2];
		String filename = "GDC_Therapeutic_Agent_Terminology-Task-499_04-24-2020.txt";
		CASProcessor processor = new CASProcessor(serviceUrl, namedGraph);
		processor.initialize();
        processor.runQA(excelfile);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

