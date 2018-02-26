package gov.nih.nci.evs.restapi.util;
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
import org.apache.commons.codec.binary.Base64;
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


public class ValueSetUtils {
	private static String TERMINOLOGY_SUBSET_CODE = "C54443";
	private OWLSPARQLUtils owlSPARQLUtils = null;
	private Vector parent_child_vec = null;
	private Vector concept_in_subset_vec = null;
	private MetadataUtils mdu = null;
	private String named_graph = null;
	private static String CONCEPT_IN_SUBSET = "Concept_In_Subset";
	private static String NCI_THESAURUS = "NCI_Thesaurus";
	private String version = null;
	private String serviceUrl = null;
	private String sparql_endpoint = null;

	private static String parent_child_file = "parent_child.txt";
	private static String concept_in_subset_file = "concept_in_subset.txt";
	private ValueSetSearchUtils searchUtils = null;//new ValueSetSearchUtils(serviceUrl, named_graph, cis_vec);

    public ValueSetUtils(String serviceUrl) {
		this.serviceUrl = serviceUrl;
		this.sparql_endpoint = serviceUrl;
        this.owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint + "?query=");
		initialize();
    }

    public ValueSetUtils(String serviceUrl, Vector parent_child_vec, Vector concept_in_subset_vec) {
		this.sparql_endpoint = serviceUrl;
		this.owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint + "?query=");
		this.parent_child_vec = parent_child_vec;
    	this.concept_in_subset_vec = concept_in_subset_vec;

    	System.out.println("initialize...");
    	initialize();
    }

    public void initialize() {
		long ms = System.currentTimeMillis();
		System.out.println(sparql_endpoint);
		mdu = new MetadataUtils(sparql_endpoint);
		named_graph = mdu.getNamedGraph(NCI_THESAURUS);
		version = mdu.getLatestVersion(NCI_THESAURUS);
		this.owlSPARQLUtils.set_named_graph(named_graph);
		if (parent_child_vec == null) {
			File file = new File(parent_child_file);
			boolean exists = file.exists();
			if (exists) {
				System.out.println("Loading parent_child_vec...");
				parent_child_vec = Utils.readFile("parent_child.txt");
			} else {
				System.out.println("Generating parent_child_vec...");
				owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl + "?query=", null, null);
				parent_child_vec = owlSPARQLUtils.getHierarchicalRelationships(named_graph);
			}
		}

        if (concept_in_subset_vec == null) {
			File file = new File(concept_in_subset_file);
			//String association_name = "Concept_In_Subset";
			boolean exists = file.exists();
			if (exists) {
				System.out.println("Loading concept_in_subset_vec...");
				concept_in_subset_vec = Utils.readFile(concept_in_subset_file);
			} else {
				System.out.println("Generating concept_in_subset_vec...");
				OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl + "?query=", null, null);
				concept_in_subset_vec = owlSPARQLUtils.getAssociationSourcesAndTargets(named_graph, CONCEPT_IN_SUBSET);
			}
		}
		searchUtils = new ValueSetSearchUtils(serviceUrl + "?query=", named_graph, concept_in_subset_vec);
		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

    public void contructSourceAssertedTree() {
		long ms = System.currentTimeMillis();
		String rootCode = TERMINOLOGY_SUBSET_CODE;
        MainTypeHierarchy mth = new MainTypeHierarchy(parent_child_vec);
        Vector v = this.owlSPARQLUtils.getPermissibleValues(concept_in_subset_vec);
        HashSet nodeSet = Utils.vector2HashSet(v);
        Vector parent_child_vs = mth.generate_embedded_hierarchy(rootCode, nodeSet);
        Utils.saveToFile("ascii_vs_tree.txt", parent_child_vs);
        Vector vs_parent_child_vec = new ASCIITreeUtils().get_parent_child_vec(parent_child_vs);
        Utils.saveToFile("vs_parent_child_vec.txt", vs_parent_child_vec);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public gov.nih.nci.evs.restapi.bean.ValueSetDefinition getValueSetDefinition(String vs_code) {
        return this.owlSPARQLUtils.createValueSetDefinition(named_graph, vs_code);
	}

	public SearchResult search(String associationName, String searchString, String searchTarget, String algorithm) {
	    return searchUtils.search(named_graph, associationName, searchString, searchTarget, algorithm);
	}

	public gov.nih.nci.evs.restapi.bean.ResolvedValueSet resolveValueSet(String vs_code) {
		gov.nih.nci.evs.restapi.bean.ValueSetDefinition vsd = getValueSetDefinition(vs_code);
		gov.nih.nci.evs.restapi.bean.ResolvedValueSet rvs = new gov.nih.nci.evs.restapi.bean.ResolvedValueSet();
		List list = new ArrayList();
		int idx = 0;
		Vector w = new Vector();
		for (int i=0; i<concept_in_subset_vec.size(); i++) {
			String line = (String) concept_in_subset_vec.elementAt(i);
			if (line.endsWith("|" + vs_code)) {
				w.add(line);
			}
		}

		w = new SortUtils().quickSort(w);
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			if (line.endsWith("|" + vs_code)) {
				Vector u = StringUtils.parseData(line, '|');
				String label = (String) u.elementAt(0);
				String code = (String) u.elementAt(1);
				idx++;
				Concept c = new Concept(idx, NCI_THESAURUS, version, label, code);
                list.add(c);
			}
		}

        rvs = new gov.nih.nci.evs.restapi.bean.ResolvedValueSet(vsd.getUri(), vsd.getName(), list);
		return rvs;
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		//ValueSetUtils vsu = new ValueSetUtils(serviceUrl, parent_child_vec, concept_in_subset_vec);
		ValueSetUtils vsu = new ValueSetUtils(serviceUrl);
        vsu.contructSourceAssertedTree();

        //SPL Color Terminology (Code C54453)
        String vs_code = "C54453";
        gov.nih.nci.evs.restapi.bean.ResolvedValueSet rvs = vsu.resolveValueSet(vs_code);
        String rvs_str = rvs.toJson();
        Vector w1 = new Vector();
        w1.add(rvs_str);
        Utils.saveToFile("rvs_" + vs_code + StringUtils.getToday() + ".txt", w1);
	}
}