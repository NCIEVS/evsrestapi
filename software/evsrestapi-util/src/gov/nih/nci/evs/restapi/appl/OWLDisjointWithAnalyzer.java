package gov.nih.nci.evs.restapi.appl;

import gov.nih.nci.evs.restapi.util.*;

import java.io.*;
import java.util.*;


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


public class OWLDisjointWithAnalyzer {

    HierarchyHelper hh = null;
    static int CODE_ONLY = 1;
    static int LABEL_AND_CODE = 2;
    static int LABEL_WITH_CODE_IN_BRACKET = 3;


    public OWLDisjointWithAnalyzer() {
	}

    public OWLDisjointWithAnalyzer(Vector v) {
        hh = new HierarchyHelper(v);
	}

	public HierarchyHelper getHierarchyHelper() {
		return this.hh;
	}

	public Vector getRoots(int format) {
		Vector roots = hh.getRoots();
		Vector w = new Vector();
		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			if (format == CODE_ONLY) {
				w.add(root);
			} else {
				String label = hh.getLabel(root);
				if (format == LABEL_AND_CODE) {
					w.add(label + "|" + root);
				} else {
					w.add(label + " (" + root + ")");
				}
			}
		}
		if (format != CODE_ONLY) {
			w = new SortUtils().quickSort(w);
		}
		return w;
	}

	public String getLabel(String code) {
	    return hh.getLabel(code);
	}


	public Vector getTransitiveClosure(String root) {
		return hh.getTransitiveClosure(root);
	}

    public static Vector test(String owlfile) {
		Vector output_vec = new Vector();
		Vector branch_vec = new Vector();
		branch_vec.add("NCI Thesaurus source file: " + owlfile);
		OWLScanner owlscanner = new OWLScanner(owlfile); //"20.05d_disjoint-test-export_200601.owl"
        Vector parent_child_vec = owlscanner.extractHierarchicalRelationships();
        Utils.saveToFile("parent_child.txt", parent_child_vec);
		Vector v = Utils.readFile("parent_child.txt");
		OWLDisjointWithAnalyzer test = new OWLDisjointWithAnalyzer(v);
        int total = 0;
		Vector roots = test.getRoots(CODE_ONLY);
		Vector class_vec = new Vector();
		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			Vector w = test.getTransitiveClosure(root);
			total = total + w.size();
			System.out.println(test.getLabel(root) + " (" + root + "): " + w.size());
			branch_vec.add(test.getLabel(root) + " (" + root + "): " + w.size());
		}
		System.out.println("Total: " + total);
		branch_vec.add("Total: " + total);
		Vector classes = owlscanner.getOWLClasses();
		System.out.println("Total owl classes: " + classes.size());
		branch_vec.add("Total owl classes: " + classes.size());

		Vector class_data = owlscanner.getOWLClassDataByCode(roots);
		Vector disjointWith_vec = owlscanner.extractOWLDisjointWith(class_data);
		for (int i=0; i<disjointWith_vec.size(); i++) {
			String t = (String) disjointWith_vec.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String c1 = (String) u.elementAt(0);
			String c2 = (String) u.elementAt(1);
			String label_1 = test.getLabel(c1);
			String label_2 = test.getLabel(c2);
			output_vec.add(label_1 + " (" + c1 + ") --> [" + "owl:disjointWith" + "} --> " +  label_2 + " (" + c2 + ")");
		}
		output_vec = new SortUtils().quickSort(output_vec);
		branch_vec.addAll(output_vec);
		return branch_vec;
	}

    public static void main(String[] args) {
		String owlfile = "20.05d_disjoint-test-export_200601.owl";
        Vector output_vec = test(owlfile);
        Utils.saveToFile("output_vec_1.txt", output_vec);

 		owlfile = "ThesaurusInferred_forTS.owl";
        output_vec = test(owlfile);
        Utils.saveToFile("output_vec_2.txt", output_vec);

    }

}

