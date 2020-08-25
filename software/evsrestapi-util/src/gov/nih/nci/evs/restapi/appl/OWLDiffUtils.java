package gov.nih.nci.evs.restapi.appl;

import gov.nih.nci.evs.restapi.util.*;
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
//import org.apache.commons.codec.binary.Base64;
//import org.json.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2020, MSC. This software was developed in conjunction
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


public class OWLDiffUtils {
    //OWLSPARQLUtils owlSPARQLUtils = null;
    //String serviceUrl = null;
    //String namedGraph = null;
    Vector properties = null;
    Vector qualifiers = null;
    HashMap qualifierMap = null;
    HashMap propertyMap = null;
    Vector propertyCountVec = null;
    String owlfile = null;
    Vector owl_vec = null;
    static PrintWriter pw = null;

    public OWLDiffUtils() {

    }

    public static boolean fileExists(String filename) {
		File file = new File(filename);
		return file.exists();
	}

	public static void dumpHashSet(HashSet hset) {
		Iterator it = hset.iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			System.out.println(t);
		}
	}

	public static Vector hashSet2Vector(HashSet hset) {
		Vector w = new Vector();
		Iterator it = hset.iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			w.add(t);
		}
		return w;
	}

    public static void compute(String owlfile1, String owlfile2) {
        long ms = System.currentTimeMillis();
        //String owlfile = "ThesaurusInferred_20.04d.owl";
        System.out.println("owlfile1: " + owlfile1);
        OWLScanner owlScanner = new OWLScanner(owlfile1);
        Vector w1 = owlScanner.getAllOWLClassHashCode();
        String hashfile_1 = "hash_" + owlfile1;
        Utils.saveToFile(hashfile_1, w1);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
        owlScanner.clear();

        System.out.println("owlfile2: " + owlfile2);
        ms = System.currentTimeMillis();
        owlScanner = new OWLScanner(owlfile2);
        Vector w2 = owlScanner.getAllOWLClassHashCode();
        String hashfile_2 = "hash_" + owlfile2;
        Utils.saveToFile(hashfile_2, w2);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
        owlScanner.clear();

        //w2 versus w1
        ms = System.currentTimeMillis();
        w2.removeAll(w1);
        w1.clear();
        String hash_2_rm_1 = "hash_2_rm_1.txt";
        Utils.saveToFile(hash_2_rm_1, w2);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));

        w1 = Utils.readFile(hashfile_1);
        w2 = Utils.readFile(hashfile_2);
        w1.removeAll(w2);
        w2.clear();
        String hash_1_rm_2 = "hash_1_rm_2.txt";
        Utils.saveToFile(hash_1_rm_2, w1);
        System.out.println("RM Total run time (ms): " + (System.currentTimeMillis() - ms));

        ms = System.currentTimeMillis();
        Vector hash_2_rm_1_vec = Utils.readFile(hash_2_rm_1);
        System.out.println("hash_2_rm_1_vec: " + hash_2_rm_1_vec.size());
        Vector codes = new Vector();
        for (int i=0; i<hash_2_rm_1_vec.size(); i++) {
			String line = (String) hash_2_rm_1_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			codes.add(code);
		}
        Vector class_vec = Utils.readFile(owlfile2);
        Vector w3 = new OWLScanner().getOWLClassDataByCode(class_vec, codes);
        String partial_owlfile2 = "partial_" + owlfile2;
        Utils.saveToFile(partial_owlfile2, w3);
        hash_2_rm_1_vec.clear();
        class_vec.clear();
        w3.clear();

        Vector hash_1_rm_2_vec = Utils.readFile(hash_1_rm_2);
        System.out.println("hash_1_rm_2_vec: " + hash_1_rm_2_vec.size());
        codes = new Vector();
        for (int i=0; i<hash_1_rm_2_vec.size(); i++) {
			String line = (String) hash_1_rm_2_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(1);
			codes.add(code);
		}
        class_vec = Utils.readFile(owlfile1);
        Vector w4 = new OWLScanner().getOWLClassDataByCode(class_vec, codes);
        String partial_owlfile1 = "partial_" + owlfile1;
        Utils.saveToFile(partial_owlfile1, w4);
        hash_1_rm_2_vec.clear();
        class_vec.clear();
        w4.clear();
        System.out.println("Extract Partial Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

    public static void compare(String owlfile1, String owlfile2, String partial_owlfile1, String partial_owlfile2) {
		OWLScanner owlScanner = new OWLScanner(owlfile1);
		HashMap code2LabelMap1 = (HashMap) owlScanner.getCode2LabelMap().clone();
		owlScanner.clear();
		System.out.println(code2LabelMap1.keySet().size());

		owlScanner = new OWLScanner(owlfile2);
		HashMap code2LabelMap2 = (HashMap) owlScanner.getCode2LabelMap().clone();
		owlScanner.clear();
		System.out.println(code2LabelMap2.keySet().size());

        HashMap id2DataHashMap1 = new OWLScanner().getOWLClassId2DataHashMap(Utils.readFile(partial_owlfile1));
        System.out.println("id2DataHashMap1: " + id2DataHashMap1.keySet().size());
        Vector classId_vec1 = new Vector();
        Iterator it = id2DataHashMap1.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			classId_vec1.add(key);
		}

        HashMap id2DataHashMap2 = new OWLScanner().getOWLClassId2DataHashMap(Utils.readFile(partial_owlfile2));
        System.out.println("id2DataHashMap2: " + id2DataHashMap2.keySet().size());
        Vector classId_vec2 = new Vector();
        Iterator it2 = id2DataHashMap2.keySet().iterator();
        while (it2.hasNext()) {
			String key = (String) it2.next();
			classId_vec2.add(key);
		}

		Vector v1 = (Vector) classId_vec1.clone();
		Vector v2 = (Vector) classId_vec2.clone();
		v1.removeAll(v2);

		Vector w1 = new Vector();
		for (int i=0; i<v1.size(); i++) {
			String t = (String) v1.elementAt(i);
			w1.add(t + "|" + (String) code2LabelMap1.get(t));
		}
		dumpVector("deleted|concept", w1);

		v1 = (Vector) classId_vec1.clone();
		v2 = (Vector) classId_vec2.clone();
		v2.removeAll(v1);
		Vector w2 = new Vector();
		for (int i=0; i<v2.size(); i++) {
			String t = (String) v2.elementAt(i);
			w2.add(t + "|" + (String) code2LabelMap2.get(t));
		}
		dumpVector("added|concept", w2);

		Vector v3 = (Vector) classId_vec2.clone();
		v3.removeAll(v2);

		// compare the modified concepts
		for (int k=0; k<v3.size(); k++) {
			String code = (String) v3.elementAt(k);
			w1 = (Vector) id2DataHashMap1.get(code);
			w2 = (Vector) id2DataHashMap2.get(code);
			compareClass(w1, w2);
		}
		id2DataHashMap1.clear();
		id2DataHashMap2.clear();
    }

	public static void dumpVector(String type, Vector v) {
		String property = null;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (type.indexOf("|axiom") != -1) {
				String s = getPropertyType(t);
				Vector u = StringUtils.parseData(type, '|');
				property = (String) u.elementAt(0) + "|" + s;
			} else {
				property = type;
			}
			pw.println(property + "|" + t);
		}
	}

    public static void compareVector(String type, Vector vec1, Vector vec2) {
		HashSet hset1 = new HashSet();
		for (int i=0; i<vec1.size(); i++) {
			String t = (String) vec1.elementAt(i);
			hset1.add(t);
		}
		HashSet hset2 = new HashSet();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			hset2.add(t);
		}
		Vector v1_minus_v2 = new Vector();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			if (!hset1.contains(t)) {
				v1_minus_v2.add(t);
			}
		}
		dumpVector("added" + "|" + type, v1_minus_v2);

		Vector v2_minus_v1 = new Vector();
		for (int i=0; i<vec1.size(); i++) {
			String t = (String) vec1.elementAt(i);
			if (!hset2.contains(t)) {
				v2_minus_v1.add(t);
			}
		}
		dumpVector("deleted" + "|" + type, v2_minus_v1);
	}

    public static Vector set_difference(Vector vec1, Vector vec2) {
		HashSet hset1 = new HashSet();
		for (int i=0; i<vec1.size(); i++) {
			String t = (String) vec1.elementAt(i);
			hset1.add(t);
		}
		HashSet hset2 = new HashSet();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			hset2.add(t);
		}
		Vector v1_minus_v2 = new Vector();
		for (int i=0; i<vec2.size(); i++) {
			String t = (String) vec2.elementAt(i);
			if (!hset1.contains(t)) {
				v1_minus_v2.add(t);
			}
		}
		return v1_minus_v2;
	}


	public static String getPropertyType(String tabDimitedAxiomData) {
		Vector u = StringUtils.parseData(tabDimitedAxiomData);
		String property_code = (String) u.elementAt(2);
		if (property_code.compareTo("P90") == 0) {
			return "FULL_SYN";
		} else if (property_code.compareTo("P375") == 0) {
			return "Maps_To";
		} else if (property_code.compareTo("P211") == 0) {
			return "GO_Annotation";
		} else if (property_code.compareTo("P97") == 0) {
			return "DEFINITION";
		} else if (property_code.compareTo("P325") == 0) {
			return "ALT_DEFINITION";
		} else {
			return "axiom";
		}
	}

    public static void compareClass(Vector v1, Vector v2) {
		OWLScanner owlScanner = new OWLScanner();
        String t1 = owlScanner.get_owl_class_hashcode(v1);
        String t2 = owlScanner.get_owl_class_hashcode(v2);

        Vector w1 = owlScanner.removeObjectValuedProperties(owlScanner.extract_properties(v1));
        Vector w2 = owlScanner.removeObjectValuedProperties(owlScanner.extract_properties(v2));
        compareVector("property", w1, w2);

        w1 = owlScanner.extract_superclasses(v1);
        w2 = owlScanner.extract_superclasses(v2);
        compareVector("superclass", w1, w2);

        w1 = new Vector();
        w2 = new Vector();
        Vector w3 = owlScanner.extract_owlrestrictions(v1);
        Vector w4 = owlScanner.extract_owlrestrictions(v2);

        for (int i=0; i<w3.size(); i++) {
            OWLRestriction r = (OWLRestriction) w3.elementAt(i);
            w1.add(r.toString());
		}
        for (int i=0; i<w4.size(); i++) {
            OWLRestriction r = (OWLRestriction) w4.elementAt(i);
            w2.add(r.toString());
		}
        compareVector("role", w1, w2);

        w1 = owlScanner.extract_associations(v1);
        w2 = owlScanner.extract_associations(v2);
        compareVector("association", w1, w2);


        w3 = owlScanner.extractPropertiesWithQualifiers(v1);
        w4 = owlScanner.extractPropertiesWithQualifiers(v2);

        w3 = owlScanner.axioms2Strings(w3);
        w4 = owlScanner.axioms2Strings(w4);

        Vector w5 = set_difference(w3, w4);
        dumpVector("added|axiom", w5);

        w5 = set_difference(w4, w3);
        dumpVector("deleted|axiom", w5);
	}


	public static String run(String owlfile1, String owlfile2) {
		int n1 = owlfile1.lastIndexOf(".");
		int n2 = owlfile2.lastIndexOf(".");
		String outputfile = owlfile1.substring(0, n1) + "_vs_" + owlfile2.substring(0, n2) + "_" + StringUtils.getToday() + ".txt";
        outputfile = outputfile.replace(" ", "_");
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			if (!(fileExists("partial_" + owlfile1) && fileExists("partial_" + owlfile1))) {
				System.out.println("Generating supporting data files...");
				compute(owlfile1, owlfile2);
			}
			System.out.println("Comparing " + owlfile1 + " and " + owlfile2 + " ... -- please wait.");
            compare(owlfile1, owlfile2, "partial_" + owlfile1, "partial_" + owlfile2);

		} catch (Exception ex) {
            ex.printStackTrace();
		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return outputfile;
	}

    public static void testCompareClass(Vector v1, Vector v2, String outputfile) {
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			OWLScanner owlScanner = new OWLScanner();
			String t1 = owlScanner.get_owl_class_hashcode(v1);
			String t2 = owlScanner.get_owl_class_hashcode(v2);

			Vector w1 = owlScanner.removeObjectValuedProperties(owlScanner.extract_properties(v1));
			Vector w2 = owlScanner.removeObjectValuedProperties(owlScanner.extract_properties(v2));

			compareVector("property", w1, w2);

			w1 = owlScanner.extract_superclasses(v1);
			w2 = owlScanner.extract_superclasses(v2);
			compareVector("superclass", w1, w2);

			w1 = new Vector();
			w2 = new Vector();
			Vector w3 = owlScanner.extract_owlrestrictions(v1);
			Vector w4 = owlScanner.extract_owlrestrictions(v2);

			for (int i=0; i<w3.size(); i++) {
				OWLRestriction r = (OWLRestriction) w3.elementAt(i);
				w1.add(r.toString());
			}
			for (int i=0; i<w4.size(); i++) {
				OWLRestriction r = (OWLRestriction) w4.elementAt(i);
				w2.add(r.toString());
			}
			compareVector("role", w1, w2);

			w1 = owlScanner.extract_associations(v1);
			w2 = owlScanner.extract_associations(v2);
			compareVector("association", w1, w2);

			w3 = owlScanner.extractPropertiesWithQualifiers(v1);
			w4 = owlScanner.extractPropertiesWithQualifiers(v2);

			w3 = owlScanner.axioms2Strings(w3);
			w4 = owlScanner.axioms2Strings(w4);

			Vector w5 = set_difference(w3, w4);
			dumpVector("added|axiom", w5);

			w5 = set_difference(w4, w3);
			dumpVector("deleted|axiom", w5);

		} catch (Exception ex) {
            ex.printStackTrace();
		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}


    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		//String owlfile1 = args[0];
		//String owlfile2 = args[1];
		//run(owlfile1, owlfile2);

		Vector v1 = Utils.readFile("C40074_v20.04d.owl");
		Vector v2 = Utils.readFile("C40074_v20.05d.owl");

		System.out.println("v1: " + v1.size());
		System.out.println("v2: " + v2.size());

		testCompareClass(v1, v2, "test.txt");
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

