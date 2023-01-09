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


public class OWLScannerTest {
	public static String[] AXIOM_PROP_CODES = new String[] {
		"P325", "P97", "P90", "P211", "P375"};

	public static void extractHierarchicalRelationships(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		Vector w = new OWLScanner(owlfile).extractHierarchicalRelationships();
		String hierfile = "hier_" + owlfile + ".txt";
		Utils.saveToFile(hierfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractAxiomData(String[] args) {
		String owlfile = args[0];
		String prop_code = args[1];
		long ms = System.currentTimeMillis();
		String outputfile = prop_code + owlfile;
		Vector w = new OWLScanner(owlfile).extractAxiomData(prop_code);
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractOWLRestrictions(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		String outputfile = "role_" + owlfile;
		OWLScanner scanner = new OWLScanner(owlfile);
		Vector w = scanner.extractOWLRestrictions(scanner.get_owl_vec());
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractProperties(String[] args) {
		String owlfile = args[0];
		OWLScanner scanner = new OWLScanner(owlfile);
		String prop_code = null;
		if (args.length > 1) {
			prop_code = args[1];
			//OWLScanner.set_NAMESPACE(namespace);
		}
		long ms = System.currentTimeMillis();
		String outputfile = "properties_" + owlfile;
		Vector w = null;
		if (prop_code != null) {
			outputfile = prop_code + "_" + owlfile;
			w = scanner.extractProperties(scanner.get_owl_vec(), prop_code);
		} else {
			w = scanner.extractProperties(scanner.get_owl_vec());
		}
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractAssociations(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		String outputfile = "association_" + owlfile;
		OWLScanner scanner = new OWLScanner(owlfile);
		Vector w = scanner.extractAssociations(scanner.get_owl_vec());
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void extractRDFSLabels(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		String outputfile = "label_" + owlfile;
		OWLScanner scanner = new OWLScanner(owlfile);
		if (args.length > 1) {
			String namespace = args[1];
			OWLScanner.set_NAMESPACE(namespace);
		}
		Vector w = scanner.extractRDFSLabels(scanner.get_owl_vec());
		Utils.saveToFile(outputfile, w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void generate_FULL_SYN(String[] args) {
		String owlfile = args[0];
		long ms = System.currentTimeMillis();
		OWLScanner scanner = new OWLScanner(owlfile);
		scanner.generate_FULL_SYN();
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void test(String[] args) {
		long ms = System.currentTimeMillis();
		extractHierarchicalRelationships(args);
		extractAxiomData(args);
		extractOWLRestrictions(args);
		extractAssociations(args);
		extractProperties(args);
		extractRDFSLabels(args);
		generate_FULL_SYN(args);
		System.out.println("Grand total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void main(String[] args) {
		test(args);
	}
}

