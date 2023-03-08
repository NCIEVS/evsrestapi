package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;

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


public class SemanticTypeQA {
    String owlfile = null;
    OWLScanner scanner = null;
    static String DISEASE_ROOT = "C2991"; //Disease or Disorder (Code C2991)
    static String FINDING_ROOT = "C3367";
    static String SEMANTIC_TYPE_PROP_CODE = "P106";

    HierarchyHelper hh = null;
    Vector parent_child_vec = null;
    Vector semantic_type_vec = null;
    HashMap semantic_type_hashmap = null;
    Vector roots = null;

    public SemanticTypeQA(String owlfile) {
		long ms = System.currentTimeMillis();
		this.owlfile = owlfile;
		scanner = new OWLScanner(owlfile);
		parent_child_vec = scanner.extractHierarchicalRelationships();

		//C100003|P106|Therapeutic or Preventive Procedure
		semantic_type_vec  = scanner.extractProperties(scanner.get_owl_vec(), SEMANTIC_TYPE_PROP_CODE);
		semantic_type_hashmap = new HashMap();
		for (int i=0; i<semantic_type_vec.size(); i++) {
			String line = (String) semantic_type_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			Vector w = new Vector();
			String code = (String) u.elementAt(0);
			if (semantic_type_hashmap.containsKey(code)) {
				w = (Vector) semantic_type_hashmap.get(code);
			}
			String type = (String) u.elementAt(2);
			if (!w.contains(type)) {
				w.add(type);
			}
			semantic_type_hashmap.put(code, w);
		}
		hh = new HierarchyHelper(parent_child_vec);
		roots = hh.getRoots();
		System.out.println("Number of roots: " + roots.size());
		Vector w1 = new Vector();
		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			String label = hh.getLabel(root);
			w1.add(label + " (" + root + ")");
		}
		w1 = new SortUtils().quickSort(w1);
		Utils.dumpVector("roots", w1);

		System.out.println("Total initialiation run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public void runQA() {
		long ms = System.currentTimeMillis();
		Vector w = hh.getTransitiveClosure(FINDING_ROOT);
        //PrintWriter writer = new PrintWriter(System.out);
        int warning_count = 0;
        String finding_label = hh.getLabel(FINDING_ROOT);

		System.out.println("\n***********************************************************");
		System.out.println("Branch: " + finding_label + " (" + FINDING_ROOT + ")");
		System.out.println("\n***********************************************************");

		String branch_id = "the branch " + finding_label + " (" + FINDING_ROOT + ")";

		for (int i=0; i<w.size(); i++) {
			int j = i+1;
			String code = (String) w.elementAt(i);
			String label = scanner.getLabel(code);
			Vector types = (Vector) semantic_type_hashmap.get(code);
			if (!types.contains("Finding") && !types.contains("Laboratory or Test Result") && !types.contains("Sign or Symptom") ) {
				System.out.println("\n" + label + " (" + code + ")");

				Utils.dumpVector("\tSemantic_Types", types);

				System.out.println("\tWARNING: " + label + " (" + code + ")" + " is in " + branch_id + " but does not have a Finding semantic type.");
				//hh.path2Roots(writer, code);
				warning_count++;
			}
		}

		Utils.saveToFile("finding.txt", w);
		System.out.println("\nwarning_count: " + warning_count);

		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			String label = hh.getLabel(root);

			if (root.compareTo("C7057") != 0) { //C7057

				int root_warning_count = 0;

				System.out.println("\n***********************************************************");
				System.out.println("Branch: " + label + " (" + root + ")");
				System.out.println("\n***********************************************************");
				branch_id = "the branch " + label + " (" + root + ")";


				w = hh.getTransitiveClosure(root);
				root_warning_count = 0;
				for (int k=0; k<w.size(); k++) {
					int j = i+1;
					String code = (String) w.elementAt(k);
					label = scanner.getLabel(code);

					Vector types = (Vector) semantic_type_hashmap.get(code);

					if (types.contains("Finding") || types.contains("Laboratory or Test Result") || types.contains("Sign or Symptom") ) {
						System.out.println("\n" + label + " (" + code + ")");
						Utils.dumpVector("\tSemantic_Types", types);
						System.out.println("\tWARNING: " + label + " (" + code + ") " + " is in " + branch_id + " but has a Finding semantic type.");
						root_warning_count++;
					}
				}

                System.out.println("\n\twarning_count: " + root_warning_count);
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public void run() {
		Vector w1 = hh.getTransitiveClosure(DISEASE_ROOT);
		Vector w2 = hh.getTransitiveClosure(FINDING_ROOT);
		Vector w3 = new Vector();
		for (int i=0; i<w1.size(); i++) {
			String code = (String) w1.elementAt(i);
			if (w2.contains(code)) {
				String label = hh.getLabel(code);
				w3.add(label +  " (" + code + ")");
				Vector types = (Vector) semantic_type_hashmap.get(code);
				System.out.println(label +  " (" + code + ")");
				Utils.dumpVector("\tSemantic Types", types);
			}
		}
		w3 = new SortUtils().quickSort(w3);
		Utils.dumpVector("In_Both_Branches.txt", w3);
		Utils.saveToFile("In_Both_Branches.txt", w3);
	}

    public static void main(String[] args) {
		String owlfile = args[0];
		SemanticTypeQA test = new SemanticTypeQA(owlfile);
		test.run();
	}
}