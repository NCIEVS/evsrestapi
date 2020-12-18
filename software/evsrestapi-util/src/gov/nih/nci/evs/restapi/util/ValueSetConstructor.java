package gov.nih.nci.evs.restapi.util;
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
import org.json.*;

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
 *      or MSC
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

public class ValueSetConstructor {
    String serviceUrl = null;
    String namedGraph = null;
    String username = null;
    String password = null;

    HashMap nameVersion2NamedGraphMap = null;
    String version = null;

    OWLSPARQLUtils owlSPARQLUtils = null;
    ParserUtils parser = null;

    public ValueSetConstructor(String serviceUrl, String namedGraph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.namedGraph = namedGraph;
		this.username = username;
		this.password = password;
		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        owlSPARQLUtils.set_named_graph(namedGraph);
        parser = new ParserUtils();
	}

    public void checkValueSetMemberShip(Vector codes, String header_code, String code) {
		boolean bool_val = codes.contains(code);
		System.out.println("Is concept " + code + " in subset " + header_code + "? " + bool_val);
	}

	public void test_concept_in_subset(String header_code, Vector codes) {
        Vector w = owlSPARQLUtils.get_concepts_in_subset(namedGraph, header_code);
        Vector v = new Vector();
        for (int i=0; i<w.size(); i++) {
			String s = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(s, '|');
			v.add((String) u.elementAt(1));
		}
		if (codes != null && codes.size() > 0) {
			for (int i=0; i<codes.size(); i++) {
				String code = (String) codes.elementAt(i);
				checkValueSetMemberShip(v, header_code, code);
			}
		}
	}

    public void dumpMetadata() {
        HashMap hmap = owlSPARQLUtils.getNameVersion2NamedGraphMap();
        Iterator it = hmap.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			Vector values = (Vector) hmap.get(key);
			Utils.dumpVector(key, values);
		}
	}

	public Vector generate_concept_in_subset(String root) {
	    Vector w = new Vector();
	    Stack stack = new Stack();
	    stack.push(root);
	    while (!stack.isEmpty()) {
			String code = (String) stack.pop();
			Vector v = owlSPARQLUtils.get_concepts_in_subset(this.namedGraph, code);
			w.addAll(v);
			Vector subs = owlSPARQLUtils.getSubclassesByCode(code);
			if (subs != null && subs.size() > 0) {
				for (int k=0; k<subs.size(); k++) {
					String sub = (String) subs.elementAt(k);
					stack.push(sub);
				}
			}
		}
		return w;
	}


	public Vector generate_concept_in_subset_by_tree(String parent_child_file) { //header_code = "C120166";
	Vector w = new Vector();
	    Vector lines = Utils.readFile(parent_child_file);
	    HashSet hset = new HashSet();
	    for (int i=0; i<lines.size(); i++) {
			String line = (String) lines.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String parent_code = (String) u.elementAt(1);
			String child_code = (String) u.elementAt(3);
			if (!hset.contains(parent_code)) {
				hset.add(parent_code);
				Vector v = owlSPARQLUtils.get_concepts_in_subset(this.namedGraph, parent_code);
				if (v != null && v.size() > 0) {
					w.addAll(v);
				}
			}

			if (!hset.contains(child_code)) {
				hset.add(child_code);
				Vector v = owlSPARQLUtils.get_concepts_in_subset(this.namedGraph, child_code);
				if (v != null && v.size() > 0) {
					w.addAll(v);
				}
			}
		}
		return w;
	}

	public Vector generate_concept_in_subset_by_descendants(Vector decendants) {
	    Vector w = new Vector();
	    HashSet hset = new HashSet();
	    for (int i=0; i<decendants.size(); i++) {
			String code = (String) decendants.elementAt(i);
			if (!hset.contains(code)) {
				hset.add(code);
				Vector v = owlSPARQLUtils.get_concepts_in_subset(this.namedGraph, code);
				if (v != null && v.size() > 0) {
					w.addAll(v);
				}
			}
		}
		return w;
	}

	public Vector traverseUp(String root) {
	    Vector w = new Vector();
	    Stack stack = new Stack();
	    stack.push(root);
	    while (!stack.isEmpty()) {
			String code = (String) stack.pop();
			Vector v = owlSPARQLUtils.getSuperclassesByCode(code);
			if (v != null && v.size() > 0) {
			    w.addAll(v);
				System.out.println(code + " has " + v.size() + " superclasses.");
				for (int k=0; k<v.size(); k++) {
					String sup = (String) v.elementAt(k);
					stack.push(sup);
				}
			} else {
				System.out.println(code + " has NO superclasses.");
			}
		}
		return w;
	}

	public String getLabel(String code) {
		Vector v = owlSPARQLUtils.getLabelByCode(this.namedGraph, code);
		String line = (String) v.elementAt(0);
		String label = parser.getValue(line);
		return label;
	}

	public Vector getTree(String code, int maxLevel) {
		return owlSPARQLUtils.getTree(this.namedGraph, code, maxLevel);
	}

	public Vector getDescendants(String root) {
	    Vector w = new Vector();
	    Stack stack = new Stack();
	    stack.push(root);
	    while (!stack.isEmpty()) {
			String code = (String) stack.pop();
			w.add(code);
			Vector u = owlSPARQLUtils.getSubclassesByCode(this.namedGraph, code);
			if (u != null && u.size() > 0) {
				int n = u.size()/2;
				for (int i=0; i<n; i++) {
					String s1 = (String) u.elementAt(i*2);
					String s2 = (String) u.elementAt(i*2+1);
					String t1 = parser.getValue(s1);
					String t2 = parser.getValue(s2);
					stack.push(t2);
				}
			}
		}
		return w;
	}

	public Vector getAncestors(String root) {
		ParserUtils parser = new ParserUtils();
	    Vector w = new Vector();
	    Stack stack = new Stack();
	    stack.push(root);
	    while (!stack.isEmpty()) {
			String code = (String) stack.pop();
			w.add(code);
			Vector u = owlSPARQLUtils.getSuperclassesByCode(this.namedGraph, code);
			if (u != null && u.size() > 0) {
				int n = u.size()/2;
				for (int i=0; i<n; i++) {
					String s1 = (String) u.elementAt(i*2);
					String s2 = (String) u.elementAt(i*2+1);
					String t1 = parser.getValue(s1);
					String t2 = parser.getValue(s2);
					stack.push(t2);
				}
			}
		}
		return w;
	}

    public void printTree(String parent_child_file) {
		Vector v = Utils.readFile(parent_child_file);
		HierarchyHelper hh = new HierarchyHelper(v, 1);
		hh.printTree();
	}


    public void checkValueSetMemberShip(String valuesetfile, String header_code, Vector codes) {
		Vector w = Utils.readFile(valuesetfile);
		Vector v = new Vector();
        for (int i=0; i<w.size(); i++) {
			String s = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(s, '|');
			v.add((String) u.elementAt(1));
		}

		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
		    boolean bool_val = v.contains(code);
		    System.out.println("Is concept " + code + " in subset " + header_code + "? " + bool_val);
		}
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String namedGraph = args[1];
		String username = args[2];
		String password = args[3];
	    ValueSetConstructor test = new ValueSetConstructor(serviceUrl, namedGraph, username, password);

        long ms = System.currentTimeMillis();
        Vector w = test.getAncestors("C100144");
        Utils.saveToFile("C100144_sups.txt", w);
        for (int i=0; i<w.size(); i++) {
			String code = (String) w.elementAt(i);
			String label = (String) test.getLabel(code);
			System.out.println(label + " (" + code + ")");
		}
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
