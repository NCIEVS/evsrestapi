package gov.nih.nci.evs.restapi.test;

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


public class TreeTraversal {
    String serviceUrl = null;
    String named_graph = null;
    String username = null;
    String password = null;
    OWLSPARQLUtils owlSPARQLUtils = null;
    MetadataUtils metadataUtils = null;
    String version = null;

    public TreeTraversal(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.username = username;
		this.password = password;
		this.metadataUtils = new MetadataUtils(serviceUrl, username, password);
        this.version = 	metadataUtils.getLatestVersion("NCI_Thesaurus");
        System.out.println(this.version);

		this.owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		this.owlSPARQLUtils.set_named_graph(named_graph);
    }

    public OWLSPARQLUtils getOWLSPARQLUtils() {
		return owlSPARQLUtils;
	}

	public Vector get_roots(String named_graph) {
		return this.owlSPARQLUtils.get_roots(named_graph);
	}


	public Vector getAncestors(String namedGraph, String root) {
		ParserUtils parser = new ParserUtils();
	    Vector w = new Vector();
	    Stack stack = new Stack();
	    stack.push(root + "|" + "0");
	    while (!stack.isEmpty()) {
			String codeAndLevel = (String) stack.pop();
			Vector u0 = StringUtils.parseData(codeAndLevel, '|');
			String code = (String) u0.elementAt(0);
			String level_str = (String) u0.elementAt(1);
			Integer int_obj = new Integer(Integer.parseInt(level_str));
			int k = Integer.valueOf(int_obj);
			w.add(codeAndLevel);
			Vector u = this.owlSPARQLUtils.getSuperclassesByCode(namedGraph, code);
			if (u != null && u.size() > 0) {
				int n = u.size()/2;
				for (int i=0; i<n; i++) {
					String s1 = (String) u.elementAt(i*2);
					String s2 = (String) u.elementAt(i*2+1);
					String t1 = parser.getValue(s1);
					String t2 = parser.getValue(s2);
					int k1 = k+1;
					stack.push(t2 + "|" + k1);
				}
			}
		}
		return w;
	}

    public String getIndentation(int level) {
	    StringBuffer buf = new StringBuffer();
	    for (int i=0; i<level; i++) {
			buf.append('\t');
		}
		return buf.toString();
	}

    public Vector printPath(Vector w) {
		Vector u = new Vector();
        for (int i=0; i<w.size(); i++) {
			String codeAndLevel = (String) w.elementAt(i);
			Vector u0 = StringUtils.parseData(codeAndLevel, '|');
			String code = (String) u0.elementAt(0);
			String level_str = (String) u0.elementAt(1);
			String label = owlSPARQLUtils.getLabel(code);
			u.add(getIndentation(Integer.parseInt(level_str)) + label + " (" + code + ")");
		}
		return u;
	}


    public void run(String root) {
		Vector v = new Vector();
		v.add("Service URL: " + serviceUrl);
		v.add("Last updated: " + StringUtils.getToday());
		v.add("Source: NCI Thesaurus, version " + version);

		v.add("\n");
		v.add("roots:");
	    Vector roots = get_roots(named_graph);
	    for (int i=0; i<roots.size(); i++) {
			String t = (String) roots.elementAt(i);
			int j = i+1;
			v.add("\t(" + j + ") " + t);
		}

	    HashSet hset = new HashSet();
	    for (int i=0; i<roots.size(); i++) {
			String t = (String) roots.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String root_code = (String) u.elementAt(1);
			if (!hset.contains(root_code)) {
				hset.add(root_code);
			}
		}

	    Vector w0 = new Vector();
	    HashSet ancestors = new HashSet();
	    Vector w = getAncestors(named_graph, root);
	    for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String code = (String) u.elementAt(0);

			if (code.compareTo(root) != 0) {
				if (!ancestors.contains(code)) {
					ancestors.add(code);
					String label = getOWLSPARQLUtils().getLabel(code);
					if (hset.contains(code)) {
						w0.add("\t" + label + " (" + code + ") "+ " (*)");
					} else {
						w0.add("\t" + label + " (" + code + ")");
					}
				}
			}
		}
		w0 = new SortUtils().quickSort(w0);
		v.add("\nAncestors of " + root + ":");
		v.addAll(w0);
        v.add("\nPaths to Roots:");
		v.addAll(printPath(w));
		Utils.saveToFile("paths_to_roots_" + root + ".txt", v);
	}


    public static void main(String[] args) {
	    String serviceUrl = args[0];
	    String named_graph = args[1];
	    String username = args[2];
	    String password = args[3];

	    TreeTraversal treeTraversal = new TreeTraversal(serviceUrl, named_graph, username, password);
	    String root = "C111076";
	    treeTraversal.run(root);
    }
}