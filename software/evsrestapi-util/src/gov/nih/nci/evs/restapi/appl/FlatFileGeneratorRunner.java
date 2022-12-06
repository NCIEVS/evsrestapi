package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;

import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.Charset;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2022 Guidehouse. This software was developed in conjunction
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
 *      "This product includes software developed by Guidehouse and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "Guidehouse" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or Guidehouse
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


public class FlatFileGeneratorRunner {
    static String CONCEPT_MEMBESHIP_FILE = "concept_in_subset_data.txt";
	public static void run(String restURL, String named_graph, String username, String password) {
		System.out.println("Generating NCI Thesaurus flat file. Please wait ...");

        FlatFileGenerator test = new FlatFileGenerator(restURL, named_graph, username, password);
		String flatfile = test.generate();

        //System.out.println("getConceptMembership ...");
        Vector v = test.getConceptMembership(named_graph);
        //Utils.saveToFile(CONCEPT_MEMBESHIP_FILE, v);

		//System.out.println("Merging " + CONCEPT_MEMBESHIP_FILE + " with " + flatfile + " ...");
		HashMap code2LabelMap = new HashMap();

		//Cyclin D|C104194|Concept_In_Subset|CTRP Biomarker Terminology|C142799
        HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);

			Vector w = new Vector();
			if (hmap.containsKey(code)) {
				w = (Vector) hmap.get(code);
			}
			String targetName = (String) u.elementAt(3);
			String targetCode = (String) u.elementAt(4);
			code2LabelMap.put(targetCode, targetName);
			if (!w.contains(targetCode)) {
				w.add(targetCode);
			}
			hmap.put(code, w);
		}

		v.clear();
		v = Utils.readFile(flatfile);
		Vector v2 = new Vector();
		HashSet hset = new HashSet();
		//C100000	<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C100000>	C99521	Percutaneous Coronary Intervention for ST Elevation Myocardial Infarction-Stable-Over 12 Hours From Symptom Onset|PERCUTANEOUS CORONARY INTERVENTION (PCI) FOR ST ELEVATION MYOCARDIAL INFARCTION (STEMI) (STABLE, >12 HRS FROM SYMPTOM ONSET)	A percutaneous coronary intervention is necessary for a myocardial infarction that presents with ST segment elevation and the subject does not have recurrent or persistent symptoms, symptoms of heart failure or ventricular arrhythmia. The presentation is past twelve hours since onset of symptoms. (ACC)			Therapeutic or Preventive Procedure
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '\t');
		    String code = (String) u.elementAt(0);
		    hset.add(code);
		    String cis = "";
		    if (hmap.containsKey(code)) {
				StringBuffer buf = new StringBuffer();
				Vector w = (Vector) hmap.get(code);
				for (int j=0; j<w.size(); j++) {
					String targetCode = (String) w.elementAt(j);
					String targetName = (String) code2LabelMap.get(targetCode);
					//buf.append("Concept_In_Subset"+"|").append(targetName+"|").append(targetCode+"|").append("ncit").append("$");
					buf.append(targetName).append("|");
				}
				String s = buf.toString();
				cis = s.substring(0, s.length()-1);
			}
			t = t + "\t" + cis;
			v2.add(t);
		}
		System.out.println("Number of code in the " + flatfile + ": " + hset.size());

		int n = flatfile.lastIndexOf(".");
		String outputfile = flatfile.substring(0, n) + "_v2" + ".txt";
		Utils.saveToFile(outputfile, v2);
		System.out.println("File generated as " + outputfile + ".");

//      QA:
        HashSet missing_codes = new HashSet();
        Iterator it = code2LabelMap.keySet().iterator();
        while (it.hasNext()) {
			String code = (String) it.next();
			if (!hset.contains(code)) {
				missing_codes.add(code);
			}
		}
		if (missing_codes.size() > 0) {
			System.out.println("\nWARNING: Codes in " + CONCEPT_MEMBESHIP_FILE + " not found in " + flatfile + ".");
			it = missing_codes.iterator();
			int lcv = 0;
			while (it.hasNext()) {
				lcv++;
				String code = (String) it.next();
				String label = (String) code2LabelMap.get(code);
				System.out.println("(" + lcv + ") " + label + " (" + code + ")");
			}
		} else {
			System.out.println("\nAll codes in " + CONCEPT_MEMBESHIP_FILE + " are found in " + flatfile + ".");
		}
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String restURL = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		run(restURL, named_graph, username, password);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

