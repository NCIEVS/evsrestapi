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
	/*
	public static String TERMINOLOGY_SUBSET_CODE = "C54443";
	public static String CONCEPT_IN_SUBSET = "Concept_In_Subset";
	public static String CONTRIBUTING_SOURCE = "Contributing_Source";
	public static String NCI_THESAURUS = "NCI_Thesaurus";
	*/

	private OWLSPARQLUtils owlSPARQLUtils = null;
	private Vector parent_child_vec = null;
	private Vector concept_in_subset_vec = null;
	private MetadataUtils mdu = null;
	private String named_graph = null;


	private String version = null;
	private String serviceUrl = null;
	private String sparql_endpoint = null;

	private static String parent_child_file = "parent_child.txt";
	private static String concept_in_subset_file = "concept_in_subset.txt";
	private ValueSetSearchUtils searchUtils = null;//new ValueSetSearchUtils(serviceUrl, named_graph, cis_vec);

	private RelationSearchUtils relSearchUtils = null;
	private HashMap valueSet2ContributingSourcesHashMap = null;


    public ValueSetUtils(String serviceUrl) {
		this.serviceUrl = serviceUrl;
		this.sparql_endpoint = serviceUrl;
        this.owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint + "?query=");
        this.relSearchUtils = new RelationSearchUtils(sparql_endpoint + "?query=");
		initialize();
    }

    public ValueSetUtils(String serviceUrl, Vector parent_child_vec, Vector concept_in_subset_vec) {
		this.sparql_endpoint = serviceUrl;
		this.owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint + "?query=");
		this.relSearchUtils = new RelationSearchUtils(sparql_endpoint + "?query=");
		this.parent_child_vec = parent_child_vec;
    	this.concept_in_subset_vec = concept_in_subset_vec;
    	System.out.println("initialize...");
    	initialize();
    }

    public void setValueSet2ContributingSourcesHashMap(Vector u) {
		//Vector u = Utils.readFile(contributingSrcFile);
		valueSet2ContributingSourcesHashMap = createValueSet2ContributingSourcesHashMap(u);
	}

    public void initialize() {
		long ms = System.currentTimeMillis();
		System.out.println(sparql_endpoint);
		mdu = new MetadataUtils(sparql_endpoint);
		named_graph = mdu.getNamedGraph(Constants.NCI_THESAURUS);
		version = mdu.getLatestVersion(Constants.NCI_THESAURUS);
		System.out.println("named_graph: " + named_graph);
		System.out.println("version: " + version);
		this.owlSPARQLUtils.set_named_graph(named_graph);
		this.relSearchUtils.set_named_graph(named_graph);
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
				concept_in_subset_vec = owlSPARQLUtils.getAssociationSourcesAndTargets(named_graph, Constants.CONCEPT_IN_SUBSET);
			}
		}
		searchUtils = new ValueSetSearchUtils(serviceUrl + "?query=", named_graph, concept_in_subset_vec);
		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public RelationSearchUtils getRelationSearchUtils() {
		return this.relSearchUtils;
	}

    public void contructSourceAssertedTree() {
		long ms = System.currentTimeMillis();
		String rootCode = Constants.TERMINOLOGY_SUBSET_CODE;
        MainTypeHierarchy mth = new MainTypeHierarchy(parent_child_vec);
        // To be modified -- restricted to concepts with a Published_Value_Set property
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
				Concept c = new Concept(idx, Constants.NCI_THESAURUS, version, label, code);
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
		System.out.println(query);

		Vector properties = this.owlSPARQLUtils.getAnnotationProperties(named_graph);
		properties = new ParserUtils().getResponseValues(properties);
		properties = new SortUtils().quickSort(properties);
		return properties;

		//return new Vector();
	}

    // find header concepts with a Published_Value_Set property.
    public Vector getConceptsWithAnnotationProperty(String propertyName) {
		Vector concepts = this.owlSPARQLUtils.getConceptsWithAnnotationProperty(named_graph, propertyName);
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



	public void runVSAnalyzer() {
		Vector properties = getAnnotationProperties();
		StringUtils.dumpVector("properties", properties);

		RelationSearchUtils relSearchUtils = getRelationSearchUtils();

        String contributingSource = Constants.CONTRIBUTING_SOURCE;
        String propertyName = Constants.CONCEPT_IN_SUBSET;
		//Vector v = relSearchUtils.getAnnotationPropertyValues(CONTRIBUTING_SOURCE);
		//Utils.saveToFile(CONTRIBUTING_SOURCE + "_" + StringUtils.getToday() + ".txt", v);

        String contributingSrcFile = Constants.CONTRIBUTING_SOURCE + "_" + StringUtils.getToday() + ".txt";
		Vector w = getPermissibleValues(contributingSrcFile);
		StringUtils.dumpVector("w", w);

		Vector u = Utils.readFile(contributingSrcFile);
		setValueSet2ContributingSourcesHashMap(u);
		u = Utils.readFile("vs_parent_child_vec.txt");
		reviewVSParentChildRelationships(u);

        contructSourceAssertedTree();

        //SPL Color Terminology (Code C54453)
        String vs_code = "C54453";
        gov.nih.nci.evs.restapi.bean.ResolvedValueSet rvs = resolveValueSet(vs_code);
        String rvs_str = rvs.toJson();
        Vector w1 = new Vector();
        w1.add(rvs_str);
        Utils.saveToFile("rvs_" + vs_code + StringUtils.getToday() + ".txt", w1);

        boolean source = false;
        Vector v = relSearchUtils.getConceptsRelatedToAnnotationProperty(propertyName, source);
        //StringUtils.dumpVector("v", v);

        Vector concepts_w_multiple_sources = new Vector();

        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			u = StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			w = relSearchUtils.getAnnotationPropertyValues(code, contributingSource);
			if (w != null && w.size() > 1) {
				for (int k=0; k<w.size(); k++) {
					String src = (String) w.elementAt(k);
					concepts_w_multiple_sources.add(label + "|" + code + "|" + src);
				}
			}
		}
		StringUtils.dumpVector("\nconcepts_w_multiple_sources", concepts_w_multiple_sources);
		Vector superclasses = new Vector();
		for (int k=0; k<concepts_w_multiple_sources.size(); k++) {
			String line = (String) concepts_w_multiple_sources.elementAt(k);
			//(1) CDISC SDTM Anatomical Location Terminology|C74456|NICHD
			u = StringUtils.parseData(line, '|');
			String label = (String) u.elementAt(0);
			String code = (String) u.elementAt(1);
			w = getSuperclassesByCode(code);
			w = new ParserUtils().getResponseValues(w);
			//StringUtils.dumpVector("\nSuperconcept of " + label + " (" + code + ")", w);
			for (int j=0; j<w.size(); j++) {
				String t = (String) w.elementAt(j);
				if (!superclasses.contains(t)) {
					superclasses.add(t);
				}
			}
		}
		StringUtils.dumpVector("\nsuperclasses of concepts_w_multiple_sources", superclasses);
		for (int k=0; k<superclasses.size(); k++) {
			String line = (String) superclasses.elementAt(k);
			u = StringUtils.parseData(line, '|');
			String concept_label = (String) u.elementAt(0);
			String concept_code = (String) u.elementAt(1);
			w = relSearchUtils.getAnnotationPropertyValues(concept_code, contributingSource);
			if (w != null && w.size() > 0) {
				StringUtils.dumpVector(concept_label + " (" + concept_code + ")", w);
			}
		}

		System.out.println("\n");

		for (int k=0; k<concepts_w_multiple_sources.size(); k++) {
			String line = (String) concepts_w_multiple_sources.elementAt(k);
			//(1) CDISC SDTM Anatomical Location Terminology|C74456
			u = StringUtils.parseData(line, '|');
			String concept_label = (String) u.elementAt(0);
			String concept_code = (String) u.elementAt(1);
			w = relSearchUtils.getAnnotationPropertyValues(concept_code, contributingSource);
			if (w != null && w.size() > 0) {
				StringUtils.dumpVector(concept_label + " (" + concept_code + ")", w);
			}
		}
	}



	public static void main(String[] args) {
		String serviceUrl = args[0];
		ValueSetUtils vsu = new ValueSetUtils(serviceUrl);
		//vsu.runVSAnalyzer();

		Vector properties = vsu.getAnnotationProperties();
		StringUtils.dumpVector("properties", properties);

		//Vector properties = vsu.execute("get_annotation_properties.txt");
        //StringUtils.dumpVector("properties", properties);


	}

}

/*

CDISC SDTM Anatomical Location Terminology (C74456):
	(1) NICHD
	(2) CDISC

CDISC SDTM Directionality Terminology (C99074):
	(1) NICHD
	(2) CDISC

CDISC SDTM Laterality Terminology (C99073):
	(1) NICHD
	(2) CDISC

C74456:
        (1) CDISC SDTM Terminology|C66830
        (2) CDISC SEND Terminology|C77526
        (3) Pediatric Terminology|C90259
C99074:
        (1) CDISC SDTM Terminology|C66830
        (2) CDISC SEND Terminology|C77526
        (3) Pediatric Terminology|C90259
C99073:
        (1) CDISC SDTM Terminology|C66830
        (2) CDISC SEND Terminology|C77526
        (3) Pediatric Terminology|C90259


CDISC SDTM Terminology (C66830):
        (1) CDISC
CDISC SEND Terminology (C77526):
        (1) CDISC
Pediatric Terminology (C90259):
        (1) NICHD


I believe so yes; if the NICHD nodes are present in the Pediatric branch,
then they do not need to be present in the SDTM or SEND branches (currently in both).

I see there is also duplication of these 3 SDTM codelists (LOC, LAT, DIR) in the Pediatric branch
(but without the CDISC and NICHD suffixes on the codelist names) so think there is maybe something funky going on
(see screen shot 2 in the attached).

        (1) ACC
        (2) BRIDG
        (3) CareLex
        (4) CDISC
        (5) CDISC-GLOSS
        (6) CRCH
        (7) CTCAE
        (8) CTEP
        (9) CTRP
        (10) FDA
        (11) GAIA
        (12) HL7
        (13) ICH
        (14) MedDRA
        (15) NCCN
        (16) NCPDP
        (17) NICHD
        (18) PI-RADS
        (19) UCUM

CDISC SDTM Anatomical Location Terminology
CDISC SDTM Directionality Terminology
CDISC SDTM Laterality Terminology

WARNING: Clinical Data Interchange Standards Consortium Terminology|C61410|CDISC Glossary Terminology|C67497
        CDISC
        child sources:
                CDISC-GLOSS

*/
