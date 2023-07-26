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

public class MainTypeHierarchyData {
	static String DISEASES_AND_DISORDERS_CODE = "C2991";
	static String CTS_API_Disease_Broad_Category_Terminology_Code = "C138189";
	static String CTS_API_Disease_Main_Type_Terminology_Code = "C138190";

    static String CTRP_BIOMARKER_TERMINOLOGY_CODE = "C142799";
    static String CTRP_REFERENCE_GENE_TERMINOLOGY_CODE = "C142801";

	String[] disease_main_types = null;
	String[] disease_broad_categories = null;

	String[] ctrp_biomarkers = null;
	String[] ctrp_reference_genes = null;

	HashSet main_type_set = null;
	HashSet ctrp_biomarker_set = null;
	HashSet ctrp_reference_gene_set = null;

	Vector<String> broad_category_vec = null;
	ArrayList<String> broad_category_list = null;
	OWLSPARQLUtils owlSPARQLUtils = null;
	HierarchyHelper hh = null;

	Vector<String> parent_child_vec = null;
	Vector<String> disease_is_stage_code_vec = null;
	Vector<String> disease_is_grade_code_vec = null;
	String serviceUrl = null;
	String version = null;
	String named_graph = null;

    public MainTypeHierarchyData(String serviceUrl, String named_graph, String username, String password) {
		owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, username, password);
        this.named_graph = named_graph;
        owlSPARQLUtils.set_named_graph(named_graph);
		this.disease_main_types = owlSPARQLUtils.get_concept_in_subset_codes(named_graph, CTS_API_Disease_Main_Type_Terminology_Code);
		System.out.println("disease_main_types: "+ disease_main_types.length);

		this.disease_broad_categories = owlSPARQLUtils.get_concept_in_subset_codes(named_graph, CTS_API_Disease_Broad_Category_Terminology_Code);
		System.out.println("disease_broad_categories: "+ disease_broad_categories.length);

		this.ctrp_biomarkers = owlSPARQLUtils.get_concept_in_subset_codes(named_graph, CTRP_BIOMARKER_TERMINOLOGY_CODE);
		System.out.println("ctrp_biomarkers: "+ ctrp_biomarkers.length);

		this.ctrp_reference_genes = owlSPARQLUtils.get_concept_in_subset_codes(named_graph, CTRP_REFERENCE_GENE_TERMINOLOGY_CODE);
		System.out.println("ctrp_reference_genes: "+ ctrp_reference_genes.length);

        main_type_set = new HashSet();
        for (int i=0; i<disease_main_types.length; i++) {
			String code = disease_main_types[i];
			main_type_set.add(code);
		}

        ctrp_biomarker_set = new HashSet();
        for (int i=0; i<ctrp_biomarkers.length; i++) {
			String code = ctrp_biomarkers[i];
			ctrp_biomarker_set.add(code);
		}

        ctrp_reference_gene_set = new HashSet();
        for (int i=0; i<ctrp_reference_genes.length; i++) {
			String code = ctrp_reference_genes[i];
			ctrp_reference_gene_set.add(code);
		}

		broad_category_list = new ArrayList<String>();
		broad_category_vec = new Vector();
        for (int i=0; i<disease_broad_categories.length; i++) {
			String code = disease_broad_categories[i];
			broad_category_list.add(code);
			broad_category_vec.add(code);
		}

		version = owlSPARQLUtils.get_ontology_version(named_graph);
		System.out.println("generate_parent_child_vec: " + named_graph);

		parent_child_vec = generate_parent_child_vec(named_graph);
		System.out.println("parent_child_vec: " + parent_child_vec.size());

		this.hh = new HierarchyHelper(parent_child_vec);
		disease_is_stage_code_vec = generateDiseaseIsStageSourceCodes(named_graph);
		System.out.println("disease_is_stage_code_vec: " + disease_is_stage_code_vec.size());

		disease_is_grade_code_vec = generateDiseaseIsGradeSourceCodes(named_graph);
		System.out.println("disease_is_grade_code_vec: " + disease_is_grade_code_vec.size());

    }

    public Vector get_parent_child_vec() {
		return parent_child_vec;
	}

    public Vector get_disease_is_stage_code_vec() {
		return disease_is_stage_code_vec;
	}

    public Vector get_disease_is_grade_code_vec() {
		return disease_is_grade_code_vec;
	}

    public String getVersion() {
		return this.version;
	}

    public String[] get_disease_main_types() {
		return this.disease_main_types;
	}

    public String[] get_disease_broad_categories() {
		return this.disease_broad_categories;
	}

	public HashSet get_main_type_set() {
		return this.main_type_set;
	}

	public HashSet get_ctrp_biomarker_set() {
		return this.ctrp_biomarker_set;
	}

	public HashSet get_ctrp_reference_gene_set() {
		return this.ctrp_reference_gene_set;
	}

	public ArrayList<String> get_broad_category_list() {
		return this.broad_category_list;
	}

	public Vector<String> get_broad_category_vec() {
		return this.broad_category_vec;
	}

	public Vector getHierarchicalRelationships(String named_graph) {
        String query = construct_get_hierarchical_relationships(named_graph);
        Vector v = owlSPARQLUtils.executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        v = new ParserUtils().getResponseValues(v);
        return new SortUtils().quickSort(v);
	}

	public Vector getHierarchicalRelationships(String named_graph, boolean raw_data) {
		Vector v = owlSPARQLUtils.executeQuery(construct_get_hierarchical_relationships(named_graph));
		if (raw_data) return v;
        v = new ParserUtils().getResponseValues(v);
        v = new SortUtils().quickSort(v);
        return v;
	}

	public String construct_get_hierarchical_relationships(String named_graph) {
        String prefixes = owlSPARQLUtils.getPrefixes();
        StringBuffer buf = new StringBuffer();
        buf.append(prefixes);
        //buf.append("select distinct ?x_label ?x_code ?y_label ?y_code").append("\n");
        //buf.append("select distinct ?x_code ?y_code").append("\n");
        buf.append("select distinct ?y_label ?y_code ?x_label ?x_code ").append("\n");
        buf.append("from <" + named_graph + ">").append("\n");
        buf.append("where  { ").append("\n");
        buf.append("    {").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?y a owl:Class .").append("\n");
        buf.append("                ?y :NHC0 ?y_code .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?x (rdfs:subClassOf|owl:equivalentClass/owl:intersectionOf/rdf:rest*/rdf:first) ?y .  ").append("\n");
        buf.append("    } UNION {").append("\n");
        buf.append("    ").append("\n");
        buf.append("                ?x a owl:Class .").append("\n");
        buf.append("                ?x :NHC0 ?x_code .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?x rdfs:label ?x_label .").append("\n");
        buf.append("").append("\n");
        buf.append("                ?y a owl:Class .").append("\n");
        buf.append("                ?y :NHC0 ?y_code .").append("\n");
        buf.append("                ?y rdfs:label ?y_label .").append("\n");
        buf.append("                ").append("\n");
        buf.append("                ?x (rdfs:subClassOf/owl:intersectionOf/rdf:rest*/rdf:first) ?y .  ").append("\n");
        buf.append("    }").append("\n");
        buf.append("}").append("\n");
        buf.append("").append("\n");
        return buf.toString();
	}

	public Vector<String> generate_parent_child_vec(String named_graph) {
		Vector parent_child_vec = null;
        File f = new File("parent_child.txt");
		if(f.exists() && !f.isDirectory()) {
		    parent_child_vec = Utils.readFile("parent_child.txt");
		} else {
			Vector parent_child_data = owlSPARQLUtils.getHierarchicalRelationships(named_graph);
			Utils.saveToFile("parent_child.txt", parent_child_vec);
		}
		return parent_child_vec;
	}

    public Vector<String> generateDiseaseIsStageSourceCodes(String named_graph) {
		Vector v1 = owlSPARQLUtils.getDiseaseIsStageSourceCodes(named_graph);
		//v1 = new ParserUtils().getResponseValues(v1);
		//v1 = new SortUtils().quickSort(v1);
		return v1;
	}

    public Vector<String> generateDiseaseIsGradeSourceCodes(String named_graph) {
		Vector v2 = owlSPARQLUtils.getDiseaseIsGradeSourceCodes(named_graph);
		//v2 = new ParserUtils().getResponseValues(v2);
		//v2 = new SortUtils().quickSort(v2);
		return v2;
	}

	public Vector<String> get_parent_child_vec(String named_graph) {
		if (this.named_graph.compareTo(named_graph) != 0) {
			return generate_parent_child_vec(named_graph);
		}
		return parent_child_vec;
	}

	public Vector<String> getDiseaseIsStageSourceCodes(String named_graph) {
		if (this.named_graph.compareTo(named_graph) != 0) {
			return getDiseaseIsStageSourceCodes(named_graph);
		}
		return disease_is_stage_code_vec;
	}

	public Vector<String> getDiseaseIsGradeSourceCodes(String named_graph) {
		if (this.named_graph.compareTo(named_graph) != 0) {
			return getDiseaseIsGradeSourceCodes(named_graph);
		}
		return disease_is_grade_code_vec;
	}

    public HashMap generateStageConceptHashMap(Vector stageConcepts) {
        HashMap stageConceptHashMap = new HashMap();
        for (int i=0; i<stageConcepts.size(); i++) {
			String code = (String) stageConcepts.elementAt(i);
			stageConceptHashMap.put(code, hh.getLabel(code));
		}
		return stageConceptHashMap;
	}

    public HashMap generateGradeConceptHashMap(Vector gradeConcepts) {
        HashMap gradeConceptHashMap = new HashMap();
        for (int i=0; i<gradeConcepts.size(); i++) {
			String code = (String) gradeConcepts.elementAt(i);
			gradeConceptHashMap.put(code, hh.getLabel(code));
		}
		return gradeConceptHashMap;
	}

	public static void main(String[] args) {
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];

		System.out.println(serviceUrl);
		MetadataUtils test = new MetadataUtils(serviceUrl, username, password);
		String codingScheme = "NCI_Thesaurus";
		long ms = System.currentTimeMillis();
		String version = test.getLatestVersion(codingScheme);
		System.out.println(codingScheme);
		System.out.println(version);
		//String named_graph = test.getNamedGraph(codingScheme);
		System.out.println(named_graph);

		MainTypeHierarchyData mthd = new MainTypeHierarchyData(serviceUrl, named_graph, username, password);
		version = mthd.getVersion();
		System.out.println("version " + version);
		Vector broad_category_vec = mthd.get_broad_category_vec();
		//StringUtils.dumpVector("broad_category_vec", broad_category_vec);
		HashSet main_type_set = mthd.get_main_type_set();
		Iterator it = main_type_set.iterator();
		int k = 0;
		while (it.hasNext()) {
			k++;
			String main_type = (String) it.next();
			System.out.println("(" + k + ") " + main_type);
		}

		Vector<String> v = mthd.get_parent_child_vec(named_graph);
		System.out.println("get_parent_child_vec: " + v.size());

		Vector v1 = mthd.getDiseaseIsStageSourceCodes(named_graph);
		StringUtils.dumpVector("getDiseaseIsStageSourceCodes", v1);

		Vector v2 = mthd.getDiseaseIsGradeSourceCodes(named_graph);
		StringUtils.dumpVector("getDiseaseIsGradeSourceCodes", v2);
	}
}