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
import java.nio.file.*;

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
 *     Initial implementation kim.ong@ngc.com
 *
 */


public class ValueSetUtils {
	static final String UNUSED_SUBSET_CONCEPT_CODE = "C103175";
	static String PUBLISH_VALUE_SET = "Publish_Value_Set";

	private OWLSPARQLUtils owlSPARQLUtils = null;
	private Vector annotation_property_vec = null;
	private Vector parent_child_vec = null;
	private Vector concept_in_subset_vec = null;
	private Vector vs_header_concept_vec = null;
	private MetadataUtils mdu = null;
	private String named_graph = null;

	private String version = null;
	private String serviceUrl = null;
	private String username = null;
	private String password = null;

	private String sparql_endpoint = null;

	public static String PARENT_CHILD_FILE = "parent_child.txt";
	public static String CONCEPT_IN_SUBSET_FILE = "concept_in_subset.txt";
	public static String VS_HEADER_CONCEPT_FILE = "vs_header_concepts.txt";
	public static String ANNOTATION_PROPERTY_FILE = "annotation_properties.txt";
	public static String EMBEDDED_VALUE_SET_HIERARCHY_FILE = "embedded_value_set_hierarchy.txt";
    public static String TERMINOLOGY_SUBSET_CODE = "C54443"; //Terminology Subset (Code C54443)
    public static String NCI_THESAURUS = "NCI_Thesaurus";
	public static String CONCEPT_IN_SUBSET = "Concept_In_Subset";
	public static String CONTRIBUTING_SOURCE = "Contributing_Source";

	private ValueSetSearchUtils searchUtils = null;

	private RelationSearchUtils relSearchUtils = null;
	private HashMap valueSet2ContributingSourcesHashMap = null;

	private static String data_directory = null;
	Vector embedded_value_set_hierarchy_vec = null;
	TreeItem assertedValueSetTree = null;

    public ValueSetUtils() {

	}

    public ValueSetUtils(String serviceUrl, String named_graph,  String username, String password) {
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.username = username;
		this.password = password;

		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
		owlSPARQLUtils.set_named_graph(named_graph);

		this.sparql_endpoint = serviceUrl + "?query=";

		System.out.println(serviceUrl);

		System.out.println("Instantiating MetadataUtils ...");
		mdu = new MetadataUtils(serviceUrl, username, password);
		version = mdu.getLatestVersion(NCI_THESAURUS);
		System.out.println("\tnamed_graph: " + this.named_graph);
		System.out.println("\tgetLatestVersion: " + version);

        System.out.println("NameVersion2NamedGraphMap ... ");
		mdu.dumpNameVersion2NamedGraphMap();
    }

    public void set_data_directory(String data_directory) {
		this.data_directory = data_directory;
	}

    public static String get_data_directory() {
		return data_directory;
	}

	 public void saveToFile(String outputfile, Vector v) {
		if (this.data_directory != null) {
			outputfile = this.data_directory + File.separator + outputfile;
		}
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			if (v != null && v.size() > 0) {
				for (int i=0; i<v.size(); i++) {
					String t = (String) v.elementAt(i);
					pw.println(t);
				}
		    }
		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public Vector readFile(String filename) {
		if (this.data_directory != null) {
			filename = this.data_directory + File.separator + filename;
		}
		return Utils.readFile(filename);
	}

	public static boolean checkIfFileExists(String filename) {
		File file = null;
		if (data_directory != null) {
			file = new File(data_directory + File.separator + filename);
		} else {
			file = new File(filename);
		}
		return file.exists();
	}

    public void initialize() {
		long ms_0 = System.currentTimeMillis();
        long ms = System.currentTimeMillis();

System.out.println("Step 1: " + ANNOTATION_PROPERTY_FILE);
        boolean annotation_property_file_exists = checkIfFileExists(ANNOTATION_PROPERTY_FILE);
        annotation_property_vec = null;
        if (!annotation_property_file_exists) {
			System.out.println("Generating " + ANNOTATION_PROPERTY_FILE + "...");
			annotation_property_vec = getAnnotationProperties();
			saveToFile(ANNOTATION_PROPERTY_FILE, annotation_property_vec);
			System.out.println(ANNOTATION_PROPERTY_FILE + " size: " + annotation_property_vec.size());
	    } else {
			System.out.println("Loading " + ANNOTATION_PROPERTY_FILE + "...");
			annotation_property_vec = readFile(ANNOTATION_PROPERTY_FILE);
		}
		System.out.println("Total processing " + ANNOTATION_PROPERTY_FILE + " run time (ms): " + (System.currentTimeMillis() - ms));

System.out.println("Step 2: " + PARENT_CHILD_FILE);
        ms = System.currentTimeMillis();
        boolean parent_child_file_exists = checkIfFileExists(PARENT_CHILD_FILE);
        parent_child_vec = null;
        if (!parent_child_file_exists) {
			System.out.println("Generating " + PARENT_CHILD_FILE + "...");
			parent_child_vec = generate_parent_child_vec();
			saveToFile(PARENT_CHILD_FILE, parent_child_vec);
			System.out.println(PARENT_CHILD_FILE + " size: " + parent_child_vec.size());
	    } else {
			System.out.println("Loading " + PARENT_CHILD_FILE + "...");
			parent_child_vec = readFile(PARENT_CHILD_FILE);
		}
		System.out.println("Total processing " + PARENT_CHILD_FILE + " run time (ms): " + (System.currentTimeMillis() - ms));

System.out.println("Step 3: " + VS_HEADER_CONCEPT_FILE);
        ms = System.currentTimeMillis();
        boolean vs_header_concept_file_exists = checkIfFileExists(VS_HEADER_CONCEPT_FILE);
        vs_header_concept_vec = null;
        if (!vs_header_concept_file_exists) {
			System.out.println("Generating " + VS_HEADER_CONCEPT_FILE + "...");
			vs_header_concept_vec = generate_value_set_header_concept_vec();
			saveToFile(VS_HEADER_CONCEPT_FILE, vs_header_concept_vec);
			System.out.println(VS_HEADER_CONCEPT_FILE + " size: " + vs_header_concept_vec.size());
	    } else {
			System.out.println("Loading " + VS_HEADER_CONCEPT_FILE + "...");
			vs_header_concept_vec = readFile(VS_HEADER_CONCEPT_FILE);
		}
		System.out.println("Total processing " + VS_HEADER_CONCEPT_FILE + " run time (ms): " + (System.currentTimeMillis() - ms));

System.out.println("Step 4: " + EMBEDDED_VALUE_SET_HIERARCHY_FILE);
        ms = System.currentTimeMillis();
        embedded_value_set_hierarchy_vec = null;
        boolean embedded_value_set_hierarchy_file_exists = checkIfFileExists(EMBEDDED_VALUE_SET_HIERARCHY_FILE);
		if (!embedded_value_set_hierarchy_file_exists) {
			embedded_value_set_hierarchy_vec = generate_embedded_value_set_hierarchy_vec(parent_child_vec,
				vs_header_concept_vec);
			saveToFile(EMBEDDED_VALUE_SET_HIERARCHY_FILE, embedded_value_set_hierarchy_vec);
		} else {
			System.out.println("Loading " + EMBEDDED_VALUE_SET_HIERARCHY_FILE + "...");
			embedded_value_set_hierarchy_vec = readFile(EMBEDDED_VALUE_SET_HIERARCHY_FILE);
		}
		System.out.println("Total processing " + EMBEDDED_VALUE_SET_HIERARCHY_FILE + " run time (ms): " + (System.currentTimeMillis() - ms));

System.out.println("Step 5: generating AssertedValueSetTree ...");
        assertedValueSetTree = generateAssertedValueSetTree(embedded_value_set_hierarchy_vec);
		System.out.println("Total processing run time (ms): " + (System.currentTimeMillis() - ms_0));
	}

	public TreeItem getAssertedValueSetTree() {
		return this.assertedValueSetTree;
	}

    public void setValueSet2ContributingSourcesHashMap(Vector u) {
		valueSet2ContributingSourcesHashMap = createValueSet2ContributingSourcesHashMap(u);
	}

	public RelationSearchUtils getRelationSearchUtils() {
		return this.relSearchUtils;
	}

	public Vector identifyOrphanNodes(Vector w) {
		Vector parent_nodes = new Vector();
		Vector child_nodes = new Vector();
		Vector orphan_nodes = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String parent_label = (String) u.elementAt(0);
			String parent_code = (String) u.elementAt(1);
			String child_label = (String) u.elementAt(2);
			String child_code = (String) u.elementAt(3);
			String parent = parent_label + "|" + parent_code;
			if (!parent_nodes.contains(parent)) {
				parent_nodes.add(parent);
			}
			String child = child_label + "|" + child_code;
			if (!child_nodes.contains(child)) {
				child_nodes.add(child);
			}
		}
		for (int i=0; i<parent_nodes.size(); i++) {
			String parent = (String) parent_nodes.elementAt(i);
			if (!child_nodes.contains(parent)) {
				if (!orphan_nodes.contains(parent)) {
					orphan_nodes.add(parent);
				}
			}
		}
		return orphan_nodes;
	}

	public HashSet vector2HashSet(Vector v) {
		HashSet hset = new HashSet();
		for (int i=0;i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			hset.add(t);
		}
		return hset;
	}

    public void contructSourceAssertedTree(String outputfile) {
		long ms = System.currentTimeMillis();
		Vector parent_child_vec = Utils.readFile(PARENT_CHILD_FILE);
        EmbeddedHierarchy eh = new EmbeddedHierarchy(parent_child_vec);
        String rootCode = TERMINOLOGY_SUBSET_CODE;
        String label = eh.getLabel(rootCode);
        System.out.println("Root: " + label + " (" + rootCode + ")");
        Vector vs_header_concept_vec = Utils.readFile(VS_HEADER_CONCEPT_FILE);

        //HashSet nodeSet = eh.getPublishedValueSetHeaderConceptCodes(vs_header_concept_vec);
        HashSet nodeSet = vector2HashSet(vs_header_concept_vec);


        Vector v = eh.getEmbeddedHierarchy(rootCode, nodeSet);

        Vector embedded_hierarchy_parent_child_vec = v;
        Utils.saveToFile("embedded_hierarchy" + "_" + rootCode + ".txt", v);
        HashMap code2LableMap = eh.createEmbeddedHierarchyCode2LabelHashMap(v);
        Iterator it = code2LableMap.keySet().iterator();
        Vector w = new Vector();
        while (it.hasNext()) {
			String node = (String) it.next();
			String node_label = eh.getLabel(node);
			w.add(node_label + " (" + node + ")");
		}
		Utils.saveToFile("code2LableMap" + "_" + rootCode + ".txt", w);
        it = nodeSet.iterator();
        int lcv = 0;
        Vector orphans = new Vector();
        Vector orphan_codes = new Vector();
        while (it.hasNext()) {
			lcv++;
			String node = (String) it.next();
			if (!code2LableMap.containsKey(node)) {
				String node_label = eh.getLabel(node);
				orphans.add(node_label + " (" + node + ")");
				orphan_codes.add(node);
			}
		}
		Utils.saveToFile("orphans" + "_" + rootCode + ".txt", orphans);

        for (int k=0; k<orphan_codes.size(); k++) {
			String orphan_code = (String) orphan_codes.elementAt(k);
			String superclass_label = eh.getLabel(orphan_code);
			Vector superclasses = eh.getSuperclassCodes(orphan_code);
			if (superclasses.contains(UNUSED_SUBSET_CONCEPT_CODE)) {
				System.out.println(superclass_label + " (" + orphan_code + ")" + " is unused.");
			} else {
				Vector superclass_label_and_code_vec = new Vector();
				for (int j=0; j<superclasses.size(); j++) {
					String t = (String) superclasses.elementAt(j);
					String t_label = eh.getLabel(t);
					superclass_label_and_code_vec.add(t_label + " (" + t + ")");
				}
			}
		}

		Vector orphanTerminologySubsets = eh.identifyOrphanTerminologySubsets(orphan_codes);
        Vector roots = eh.identifyRootTerminologySubsets(embedded_hierarchy_parent_child_vec);
        Vector eh_vec = eh.generateEmbeddedHierarchyParentChildData(embedded_hierarchy_parent_child_vec, nodeSet);
        Utils.saveToFile("embedded_value_set_hierarchy.txt", eh_vec);

        eh.generateEmbeddedHierarchyFile(outputfile, eh_vec);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public gov.nih.nci.evs.restapi.bean.ValueSetDefinition getValueSetDefinition(String vs_code) {
        return this.owlSPARQLUtils.createValueSetDefinition(named_graph, vs_code);
	}

	public SearchResult search(String associationName, String searchString, String searchTarget, String algorithm) {
	    return searchUtils.search(named_graph, associationName, searchString, searchTarget, algorithm);
	}

	public Vector execute(String queryfile) {
		long ms = System.currentTimeMillis();
		Vector v = this.owlSPARQLUtils.execute(queryfile);
		System.out.println("Total execute run time (ms): " + (System.currentTimeMillis() - ms));
		return v;
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


    public Vector getSuperclassesByCode(String code) {
		return this.owlSPARQLUtils.getSuperclassesByCode(named_graph, code);
	}

    public Vector getAnnotationProperties() {
		String query = this.owlSPARQLUtils.construct_get_annotation_properties(named_graph);
		Vector properties = this.owlSPARQLUtils.getAnnotationProperties(named_graph);
		properties = new ParserUtils().getResponseValues(properties);
		properties = new SortUtils().quickSort(properties);
		return properties;
	}

    // find header concepts with a Published_Value_Set property.
    public Vector getConceptsWithAnnotationProperty(String propertyName) {
		String query = this.owlSPARQLUtils.construct_get_concepts_with_annotation_property(named_graph, propertyName);
		Vector concepts = this.owlSPARQLUtils.getConceptsWithAnnotationProperty(named_graph, propertyName);
        if (concepts == null) return null;
        if (concepts.size() == 0) return new Vector();
		concepts = new ParserUtils().getResponseValues(concepts);
		concepts = new SortUtils().quickSort(concepts);
		return concepts;
	}

    public Vector getPermissibleValues(String filename) {
		Vector v = Utils.readFile(filename);
		v = this.owlSPARQLUtils.getPermissibleValues(v);
		return v;
	}

	public HashMap createValueSet2ContributingSourcesHashMap(Vector v) {
		//CDISC SDTM Cardiac Procedure Indication Terminology|C101859|CDISC
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code  = (String) u.elementAt(1);
			String source  = (String) u.elementAt(2);
			if (label.endsWith("Terminology")) {
				Vector w = new Vector();
				if (hmap.containsKey(code)) {
					w = (Vector) hmap.get(code);
				}
				if (!w.contains(source)) {
					w.add(source);
				}
				hmap.put(code, w);
				if (w.size() > 1) {
					System.out.println(label);
				}
			}
		}
		return hmap;
	}

	//FDA Structured Product Labeling Terminology|C54452|SPL Shape Terminology|C54454
    public void reviewVSParentChildRelationships(Vector v) {
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String parent_label = (String) u.elementAt(0);
			String parent_code  = (String) u.elementAt(1);
			String child_label  = (String) u.elementAt(2);
			String child_code  = (String) u.elementAt(3);
			//parent source must be in child source list
			Vector parent_sources = (Vector) valueSet2ContributingSourcesHashMap.get(parent_code);
			Vector child_sources = (Vector) valueSet2ContributingSourcesHashMap.get(child_code);

			if (parent_sources != null && child_sources != null) {
				if (parent_sources.size() > 1) {
					System.out.println("WARNING: " + line);
					System.out.println("\tparent_sources.size() > 1");
				}
				String parent_source = (String) parent_sources.elementAt(0);
				if (!child_sources.contains(parent_source)) {
					System.out.println("WARNING: " + line);
					System.out.println("\tparent source: " + parent_source);
					System.out.println("\tchild sources:");
					for (int k=0; k<child_sources.size(); k++) {
						String src = (String) child_sources.elementAt(k);
						System.out.println("\t\t" + src);
					}
				}
			}
    	}
	}

    public HashSet getPublishedValueSetHeaderConceptCodes(Vector v) {
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			String property = (String) u.elementAt(2);
			String yes_no = (String) u.elementAt(3);
			if (yes_no.compareTo("Yes") == 0) {
				if (!hset.contains(code)) {
					hset.add(code);
				}
			}
		}
		return hset;
	}

	public Vector hashSet2Vector(HashSet hset) {
		Vector keys = new Vector();
		Iterator it = hset.iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		return keys;
	}

	public Vector getConceptInSubset(String code) {
		Vector concepts = this.owlSPARQLUtils.getInverseAssociationsByCode(named_graph, code);
		concepts = new ParserUtils().getResponseValues(concepts);
		concepts = new SortUtils().quickSort(concepts);
		return concepts;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Vector generate_parent_child_vec() {
		//owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl + "?query=", null, null);
		Vector parent_child_vec = owlSPARQLUtils.getHierarchicalRelationships(named_graph);
		parent_child_vec = new ParserUtils().getResponseValues(parent_child_vec);
		parent_child_vec = new SortUtils().quickSort(parent_child_vec);
		return parent_child_vec;
	}

	public Vector generate_concept_in_subset_vec() {
		//OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl + "?query=", null, null);
		concept_in_subset_vec = owlSPARQLUtils.getAssociationSourcesAndTargets(named_graph, CONCEPT_IN_SUBSET);
		concept_in_subset_vec = new ParserUtils().getResponseValues(concept_in_subset_vec);
		concept_in_subset_vec = new SortUtils().quickSort(concept_in_subset_vec);
		return concept_in_subset_vec;
	}

	public Vector generate_value_set_header_concept_vec() {
		Vector vs_header_concept_vec = owlSPARQLUtils.getPublishedValueSets(named_graph, true);
		return vs_header_concept_vec;
	}

    public Vector generate_embedded_value_set_hierarchy_vec() {
		return generate_embedded_value_set_hierarchy_vec(null, null);
	}

	public Vector get_parent_child_vec() {
		return parent_child_vec;
	}

	public Vector get_vs_header_concept_vec() {
		return vs_header_concept_vec;
	}

    public Vector generate_embedded_value_set_hierarchy_vec(Vector parent_child_vec, Vector vs_header_concept_vec) {
		long ms = System.currentTimeMillis();
		if (parent_child_vec == null) {
			parent_child_vec = generate_parent_child_vec();
		}
		if (vs_header_concept_vec == null) {
			vs_header_concept_vec = generate_value_set_header_concept_vec();
		}
        EmbeddedHierarchy eh = new EmbeddedHierarchy(parent_child_vec);
        String rootCode = TERMINOLOGY_SUBSET_CODE;
        String label = eh.getLabel(rootCode);

        //HashSet nodeSet = eh.getPublishedValueSetHeaderConceptCodes(vs_header_concept_vec);
        HashSet nodeSet = vector2HashSet(vs_header_concept_vec);
        Vector v = eh.getEmbeddedHierarchy(rootCode, nodeSet);
        Vector embedded_hierarchy_parent_child_vec = v;
        HashMap code2LableMap = eh.createEmbeddedHierarchyCode2LabelHashMap(v);
        Iterator it = nodeSet.iterator();
        int lcv = 0;
        Vector orphans = new Vector();
        Vector orphan_codes = new Vector();
        while (it.hasNext()) {
			lcv++;
			String node = (String) it.next();
			if (!code2LableMap.containsKey(node)) {
				String node_label = eh.getLabel(node);
				orphans.add(node_label + " (" + node + ")");
				orphan_codes.add(node);
			}
		}
        for (int k=0; k<orphan_codes.size(); k++) {
			String orphan_code = (String) orphan_codes.elementAt(k);
			String superclass_label = eh.getLabel(orphan_code);
			Vector superclasses = eh.getSuperclassCodes(orphan_code);
			if (superclasses.contains(UNUSED_SUBSET_CONCEPT_CODE)) {
				//System.out.println(superclass_label + " (" + orphan_code + ")" + " is unused.");
			} else {
				Vector superclass_label_and_code_vec = new Vector();
				for (int j=0; j<superclasses.size(); j++) {
					String t = (String) superclasses.elementAt(j);
					String t_label = eh.getLabel(t);
					superclass_label_and_code_vec.add(t_label + " (" + t + ")");
				}
			}
		}
		Vector orphanTerminologySubsets = eh.identifyOrphanTerminologySubsets(orphan_codes);
        Vector roots = eh.identifyRootTerminologySubsets(embedded_hierarchy_parent_child_vec);
        Vector eh_vec = eh.generateEmbeddedHierarchyParentChildData(embedded_hierarchy_parent_child_vec, nodeSet);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
		return eh_vec;
	}

    public static TreeItem createTreeItem(Vector parent_child_vec) {
		return new ASCIITreeUtils().createTreeItem(parent_child_vec);
	}

    public static String getAssociation(TreeItem ti) {
		Iterator it = ti._assocToChildMap.keySet().iterator();
		return (String) it.next();
	}

    public static TreeItem removeSuperRootNode(TreeItem ti) {
		if (ti._code.compareTo("<Root>") != 0) return ti;
		List<TreeItem> children = ti._assocToChildMap.get(getAssociation(ti));
		return (TreeItem) children.get(0);
	}

	public static TreeItem generateAssertedValueSetTree(Vector embedded_value_set_hierarchy_vec) {
		TreeItem ti = createTreeItem(embedded_value_set_hierarchy_vec);
		ti = removeSuperRootNode(ti);
		return ti;
	}

	public String treeItem2Json(TreeItem ti) {
		return JSON2TreeItem.treeItem2Json(ti);
	}

	public void run() {
		Vector embedded_value_set_hierarchy_vec = generate_embedded_value_set_hierarchy_vec(get_parent_child_vec(),
		    get_vs_header_concept_vec());
		saveToFile("embedded_value_set_hierarchy_vec_" + ".txt", embedded_value_set_hierarchy_vec);
		TreeItem ti = generateAssertedValueSetTree(embedded_value_set_hierarchy_vec);
		TreeItem.printTree(ti, 0, false); // print code first = false
	}

    public TreeItem loadTree(String filename) {
		if (data_directory != null) {
			filename = this.data_directory + File.separator + filename;
		}
		Vector embedded_value_set_hierarchy_vec = Utils.readFile(filename);
		TreeItem ti = generateAssertedValueSetTree(embedded_value_set_hierarchy_vec);
		return ti;
	}

	public TreeItem searchTree(TreeItem ti, String text) {
		if (ti._text.compareTo(text) == 0) return ti;
		TreeItem node = null;
        for (String association : ti._assocToChildMap.keySet()) {
            List<TreeItem> children = ti._assocToChildMap.get(association);
            if (children != null) {
				for (int i=0; i<children.size(); i++) {
					TreeItem childItem = (TreeItem) children.get(i);
					node = searchTree(childItem, text);
					if (node != null) {
						return node;
					}
				}
			}
        }
        return null;
	}



 	public void testSearchTree() {
		String text = "FDA Structured Product Labeling Terminology";
		String filename = "embedded_value_set_hierarchy_vec_05-11-2018.txt";
        TreeItem ti = loadTree(filename);
        TreeItem tree_branch = searchTree(ti, text);
        if (tree_branch != null) {
       		TreeItem.printTree(tree_branch, 0, false);
	    }
	}

 	public void testSerialization() {
		String filename = "embedded_value_set_hierarchy_vec_05-11-2018.txt";
        TreeItem ti = loadTree(filename);
        String outputfile = "valuesettree.ser";
        SerializationUtils.serialize(ti, outputfile);
        ti = SerializationUtils.deserialize(outputfile);
        TreeItem.printTree(ti, 0, false);
	}



    // Step 1:
 	public TreeItem deserializeValueSetTree(String filename) {
        boolean fileexists = checkIfFileExists(filename);
        if (!fileexists) return null;
        // Convert serialized valueset tree to TreeItem
        TreeItem ti = SerializationUtils.deserialize(filename);
        return ti;
	}

    // Step 2:
	public StringBuffer treeItem2StringBuffer(TreeItem ti) {
        HashSet _vocabularyNameSet = null;
        SimpleTreeUtils stu = new SimpleTreeUtils(_vocabularyNameSet);
		HashMap sourceValueSetTree = new HashMap();
		sourceValueSetTree.put("<Root>", ti);
		return stu.getValueSetTreeStringBuffer(sourceValueSetTree);
	}

/*
	public HashSet vector2HashSet(Vector v) {
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (!hset.contains(t)) {
				hset.add(t);
			}
		}
		return hset;
	}
*/

    public Vector generate_embedded_value_set_hierarchy_vec(Vector parent_child_vec, String rootCode, Vector vs_header_concept_vec) {
		long ms = System.currentTimeMillis();
        EmbeddedHierarchy eh = new EmbeddedHierarchy(parent_child_vec);
        //String label = eh.getLabel(rootCode);
        HashSet nodeSet = vector2HashSet(vs_header_concept_vec);
        Vector v = eh.getEmbeddedHierarchy(rootCode, nodeSet);
        Vector embedded_hierarchy_parent_child_vec = v;
        HashMap code2LableMap = eh.createEmbeddedHierarchyCode2LabelHashMap(v);

        Iterator it = nodeSet.iterator();
        int lcv = 0;
        Vector orphans = new Vector();
        Vector orphan_codes = new Vector();
        while (it.hasNext()) {
			lcv++;
			String node = (String) it.next();
			if (!code2LableMap.containsKey(node)) {
				String node_label = eh.getLabel(node);
				orphans.add(node_label + " (" + node + ")");
				orphan_codes.add(node);
			}
		}
        for (int k=0; k<orphan_codes.size(); k++) {
			String orphan_code = (String) orphan_codes.elementAt(k);
			String superclass_label = eh.getLabel(orphan_code);
			Vector superclasses = eh.getSuperclassCodes(orphan_code);
			if (superclasses.contains(UNUSED_SUBSET_CONCEPT_CODE)) {
				//System.out.println(superclass_label + " (" + orphan_code + ")" + " is unused.");
			} else {
				Vector superclass_label_and_code_vec = new Vector();
				for (int j=0; j<superclasses.size(); j++) {
					String t = (String) superclasses.elementAt(j);
					String t_label = eh.getLabel(t);
					superclass_label_and_code_vec.add(t_label + " (" + t + ")");
				}
			}
		}
		Vector orphanTerminologySubsets = eh.identifyOrphanTerminologySubsets(orphan_codes);
        Vector roots = eh.identifyRootTerminologySubsets(embedded_hierarchy_parent_child_vec);
        Vector eh_vec = eh.generateEmbeddedHierarchyParentChildData(embedded_hierarchy_parent_child_vec, nodeSet);
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
		return eh_vec;
	}

    public static String getCurrentWorkingDrectory() {
        return java.nio.file.Paths.get(".").toAbsolutePath().normalize().toString();
	}

    public static void createCurrentWorkingDrectory(String subdirname) {
		String currentWorkingDir = getCurrentWorkingDrectory();
		File theDir = new File(currentWorkingDir + File.separator + subdirname);
		if (!theDir.exists()){
			theDir.mkdirs();
		}
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		System.out.println("serviceUrl: " + serviceUrl);
		System.out.println("named_graph: " + named_graph);
        ValueSetUtils vsu = new ValueSetUtils(serviceUrl, named_graph, username, password);
	    vsu.initialize();
	    TreeItem assertedValueSetTree = vsu.getAssertedValueSetTree();
	    TreeItem.printTree(assertedValueSetTree, 0, false); // print code first = false
	}
}
