package gov.nih.nci.evs.restapi.util;

//import gov.nih.nci.evs.restapi.config.*;

import gov.nih.nci.evs.reportwriter.formatter.*;

import gov.nih.nci.evs.restapi.appl.*;
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
import java.nio.file.*;
import java.util.*;

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
 *     Initial implementation kim.ong@nih.gov
 *
 */

public class RowProcessor {

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static HashMap generateRowMap(String compositeFile, int master_subset_code_column_number, int master_concept_code_column_number) {
		Vector v = Utils.readFile(compositeFile);
		String composite_header = (String) v.elementAt(0);

		Vector u = StringUtils.parseData(composite_header, '\t');
		String row_column_label = (String) u.elementAt(u.size()-1);
		row_column_label = row_column_label.toLowerCase();

		Vector keys = new Vector();
		HashMap hmap = new HashMap();

		for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			u = StringUtils.parseData(line, '\t');
			String s0 = (String) u.elementAt(master_subset_code_column_number);
			String s1 = (String) u.elementAt(master_concept_code_column_number);
			String row = (String) u.elementAt(u.size()-1);
			String key = s0 + "$" + s1;
			Vector w = new Vector();
			if (hmap.containsKey(key)) {
				w = (Vector) hmap.get(key);
			}
			w.add(row);
			hmap.put(key, w);
		}

		//Utils.dumpMultiValuedHashMap("RowMap", hmap);
		return hmap;
	}

    public static void generateExcel(String textfile) {
		System.out.println("textfile" + textfile);
		try {
			new AsciiToExcelFormatter().convert(textfile, "	", textfile.replace(".txt", ".xls"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    public static void appendRowNumbers(HashMap hmap, String projectFile,
    		                            int subset_code_column_number, int concept_code_column_number) {
		Vector v = Utils.readFile(projectFile);
        Vector v0 = new Vector();
        String header = (String) v.elementAt(0) + "\tRow";//PROJECT_HEADER;
		v0.add(header);

		int key_not_fount = 0;
		Vector key_not_found_vec = new Vector();

		for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '\t');
			String s0 = (String) u.elementAt(subset_code_column_number);
			String s1 = (String) u.elementAt(1);
			//String s2 = (String) u.elementAt(2);
			//String s3 = (String) u.elementAt(3);
			String s4 = (String) u.elementAt(concept_code_column_number);

			if (s1.compareTo("") != 0) {
				String key = s0 + "$" + s4;
				if (hmap.containsKey(key)) {
					Vector w = (Vector) hmap.get(key);
					for (int k=0; k<w.size(); k++) {
						String row = (String) w.elementAt(k);
						v0.add(line + "\t" + row);
					}
				} else {
					System.out.println("key not found: " + key);
					key_not_fount++;
					v0.add(line + "\t" + "" + "\t" + "");
					key_not_found_vec.add(line);
				}
			}
		}
		int m = v.size()-1;
		Utils.saveToFile("no_match_" + projectFile, key_not_found_vec);
		String outputfile = "row_" + projectFile;
		Utils.saveToFile(outputfile, v0);
		System.out.println("key_not_fount: " + key_not_fount + " out of " + m);
		String textfile = SortTextualData.sort(outputfile);
		generateExcel(textfile);
	}

	public static void run(String compositeFile,
	                       int master_subset_code_column_number,
	                       int master_concept_code_column_number,
	                       String projectFile,
	                       int subset_code_column_number,
	                       int concept_code_column_number
	) {
		HashMap hmap = generateRowMap(compositeFile, master_subset_code_column_number, master_concept_code_column_number);
		appendRowNumbers(hmap, projectFile, subset_code_column_number, concept_code_column_number);
	}

/*
	public static void main(String[] args) {
		//String owlfile = ConfigurationController.owlfile;
		String masterSubsetCodeColumnNumber = ConfigurationController.masterSubsetCodeColumnNumber;
		int master_subset_code_column_number = Integer.parseInt(masterSubsetCodeColumnNumber);
		System.out.println("master_subset_code_column_number: " + master_subset_code_column_number);

		String masterConceptCodeColumnNumber = ConfigurationController.masterConceptCodeColumnNumber;
		int master_concept_code_column_number = Integer.parseInt(masterConceptCodeColumnNumber);
		System.out.println("master_concept_code_column_number: " + master_concept_code_column_number);

		HashMap hmap = generateRowMap("composite.txt", master_subset_code_column_number, master_concept_code_column_number);

		String subsetCodeColumnNumber = ConfigurationController.subsetCodeColumnNumber;
		int subset_code_column_number = Integer.parseInt(subsetCodeColumnNumber);
		System.out.println("subset_code_column_number: " + subset_code_column_number);

		String conceptCodeColumnNumber = ConfigurationController.conceptCodeColumnNumber;
		int concept_code_column_number = Integer.parseInt(conceptCodeColumnNumber);
		System.out.println("concept_code_column_number: " + concept_code_column_number);

		String projectFile = ConfigurationController.projectFile; //"ACC-AHA_COVID-19_Terminology.txt";
		appendRowNumbers(hmap, projectFile, subset_code_column_number, concept_code_column_number);
	}
*/
}

