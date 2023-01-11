package gov.nih.nci.evs.restapi.test;
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


public class OBOScannerTest {

	public static void extractHierarchicalRelationships(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		Vector w = new OBOScanner(owlfile).extractHierarchicalRelationships();
		String hierfile = "hier_" + owlfile + ".txt";
		Utils.saveToFile(hierfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractAxiomData(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		String outputfile = "axiom_" + owlfile;
		Vector w = new OBOScanner(owlfile).extractAxiomData();
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractOWLRestrictions(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		String outputfile = "role_" + owlfile;
		OBOScanner scanner = new OBOScanner(owlfile);
		Vector w = scanner.extractOWLRestrictions(scanner.get_owl_vec());
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractProperties(String[] args) {
		String owlfile = args[0];
		OBOScanner scanner = new OBOScanner(owlfile);

		long ms = System.currentTimeMillis();
		String outputfile = "properties_" + owlfile;
		Vector w = null;
		w = scanner.extractProperties();
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractAssociations(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		String outputfile = "association_" + owlfile;
		OBOScanner scanner = new OBOScanner(owlfile);
		Vector w = scanner.extractAssociations();
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractRDFSLabels(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		String outputfile = "label_" + owlfile;
		OBOScanner scanner = new OBOScanner(owlfile);
		if (args.length > 1) {
			String namespace = args[1];
			OBOScanner.set_NAMESPACE(namespace);
		}
		Vector w = scanner.extractRDFSLabels(scanner.get_owl_vec());
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}


	public static void extractSynonymsFromAxiomData(String filename){
		Vector v = Utils.readFile(filename);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.indexOf("Synonym|") != -1) {
				Vector u = StringUtils.parseData(line, '|');
				w.add((String) u.elementAt(0) + "|" + (String) u.elementAt(2));
			}
		}
		Utils.saveToFile("synonym_" + filename, w);
	}

	public static HashMap createCode2NamespaceMap(String filename){
		Vector v = Utils.readFile(filename);
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			//http://purl.obolibrary.org/obo/UBERON_8440064|SPLC (sensu Mustela putorius furo)
			Vector u = StringUtils.parseData(line, '|');
			String t = (String) u.elementAt(0);
			//http://purl.obolibrary.org/obo/UBERON_8440064
			int n = t.lastIndexOf("/");
			String ns = t.substring(0, n);
			String code = t.substring(n+1, t.length());
			hmap.put(code, ns);

			if (i < 10) {
				System.out.println(code + " --> " + ns);
			}
		}
		return hmap;
	}

	public static void extractAnnotationProperties(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		String outputfile = "annotation_properties_" + owlfile;
		OBOScanner scanner = new OBOScanner(owlfile);
		Vector w = scanner.extractAnnotationProperties();
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void generateTermFile(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		String outputfile = "term_" + owlfile;
		OBOScanner scanner = new OBOScanner(owlfile);
		Vector v = scanner.extractAxiomData();
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.indexOf("Synonym|") != -1) {
				Vector u = StringUtils.parseData(line, '|');
				w.add((String) u.elementAt(0) + "|" + (String) u.elementAt(2));
			}
		}

		Vector w1 = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String t = (String) u.elementAt(0);
			int n = t.lastIndexOf("/");
			String ns = t.substring(0, n);
			String code = t.substring(n+1, t.length());
			w1.add(code + "|" + (String) u.elementAt(1));
		}

		Vector w2 = scanner.extractRDFSLabels(scanner.get_owl_vec());
		w1.addAll(w2);
		w1 = new SortUtils().quickSort(w1);
		w = new Vector();
		w.add("Code\tTerm");
		HashSet hset = new HashSet();
		for (int i=0; i<w1.size(); i++) {
			String line = (String) w1.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String s = (String) u.elementAt(0) + "\t" + (String) u.elementAt(1);
			if (!hset.contains(s)) {
				hset.add(s);
			    w.add(s);
			}
		}
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void test(String[] args) {
		long ms = System.currentTimeMillis();
		//extractRDFSLabels(args);
		//extractHierarchicalRelationships(args);
        //extractOWLRestrictions(args);
        //extractAxiomData(args);
        //extractAnnotationProperties(args);
        //extractProperties(args);
        //extractAssociations(args);
        //extractAxiomData(args);
        generateTermFile(args);
		System.out.println("Grand total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void main(String[] args) {
		test(args);
	}
}

