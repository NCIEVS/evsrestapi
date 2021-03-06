package gov.nih.nci.evs.api.util.ext;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

@SuppressWarnings({
    "unchecked", "rawtypes"
})
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

  HierarchyHelper hh = null;

  Vector<String> parent_child_vec = null;

  Vector<String> disease_is_stage_code_vec = null;

  Vector<String> disease_is_grade_code_vec = null;

  String serviceUrl = null;

  String version = null;

  String named_graph = null;

  // Vector getConceptsInSubset(String named_graph, String code)

  public MainTypeHierarchyData(String serviceUrl, String named_graph) {
    if (!serviceUrl.endsWith("?query=")) {
      serviceUrl = serviceUrl + "?query=";
    }
    this.named_graph = named_graph;
//    this.disease_broad_categories = owlSPARQLUtils.get_concept_in_subset_codes(named_graph,
  //      CTS_API_Disease_Broad_Category_Terminology_Code);

    main_type_set = new HashSet();
    for (int i = 0; i < disease_main_types.length; i++) {
      String code = disease_main_types[i];
      main_type_set.add(code);
    }

    ctrp_biomarker_set = new HashSet();
    for (int i = 0; i < ctrp_biomarkers.length; i++) {
      String code = ctrp_biomarkers[i];
      ctrp_biomarker_set.add(code);
    }

    ctrp_reference_gene_set = new HashSet();
    for (int i = 0; i < ctrp_reference_genes.length; i++) {
      String code = ctrp_reference_genes[i];
      ctrp_reference_gene_set.add(code);
    }

    broad_category_list = new ArrayList<String>();
    broad_category_vec = new Vector();
    for (int i = 0; i < disease_broad_categories.length; i++) {
      String code = disease_broad_categories[i];
      broad_category_list.add(code);
      broad_category_vec.add(code);
    }
    parent_child_vec = generate_parent_child_vec(named_graph);
    this.hh = new HierarchyHelper(parent_child_vec);
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

  public Vector<String> generate_parent_child_vec(String named_graph) {
    Vector parent_child_vec = null;
    File f = new File("parent_child.txt");
    if (f.exists() && !f.isDirectory()) {
      parent_child_vec = Utils.readFile("parent_child.txt");

    } else {
      System.out.println("Generating parent_child.txt...");
      Vector parent_child_data = getHierarchicalRelationships(named_graph);
      parent_child_vec = new ParserUtils().getResponseValues(parent_child_data);
      parent_child_vec = new SortUtils().quickSort(parent_child_vec);
      Utils.saveToFile("parent_child.txt", parent_child_vec);
    }
    return parent_child_vec;
  }

  public Vector getHierarchicalRelationships(String named_graph) {
    // TODO: for this and others, just use sparqlQueryManagerImpl
    return executeQuery(construct_get_hierarchical_relationships(named_graph));
  }

  public String construct_get_hierarchical_relationships(String named_graph) {
    String prefixes = getPrefixes();
    String named_graph_id = ":NHC0";
    StringBuffer buf = new StringBuffer();
    buf.append(prefixes);
    buf.append("SELECT ?z_label ?z_code ?x_label ?x_code").append("\n");
    buf.append("{").append("\n");
    buf.append("  graph <" + named_graph + ">").append("\n");
    buf.append("  {").append("\n");
    buf.append("      {").append("\n");
    buf.append("          {").append("\n");
    buf.append("            ?x a owl:Class .").append("\n");
    buf.append("            ?x rdfs:label ?x_label .").append("\n");
    buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
    buf.append("            ?x rdfs:subClassOf ?z .").append("\n");
    buf.append("            ?z a owl:Class .").append("\n");
    buf.append("            ?z rdfs:label ?z_label .").append("\n");
    buf.append("            ?z " + named_graph_id + " ?z_code").append("\n");
    buf.append("          }").append("\n");
    buf.append("          FILTER (?x != ?z)").append("\n");
    buf.append("      }").append("\n");
    buf.append("      UNION").append("\n");
    buf.append("      {").append("\n");
    buf.append("          {").append("\n");
    buf.append("            ?x a owl:Class .").append("\n");
    buf.append("            ?x rdfs:label ?x_label .").append("\n");
    buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
    buf.append("            ?x owl:equivalentClass ?y .").append("\n");
    buf.append("            ?y owl:intersectionOf ?list .").append("\n");
    buf.append("            ?list rdf:rest*/rdf:first ?z .").append("\n");
    buf.append("            ?z a owl:Class .").append("\n");
    buf.append("            ?z rdfs:label ?z_label .").append("\n");
    buf.append("            ?z " + named_graph_id + " ?z_code").append("\n");
    buf.append("          }").append("\n");
    buf.append("          FILTER (?x != ?z)").append("\n");
    buf.append("      }").append("\n");
    buf.append("  }").append("\n");
    buf.append("}").append("\n");
    return buf.toString();
  }

  public String getPrefixes() {
    StringBuffer buf = new StringBuffer();
    buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
    buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
    buf.append("PREFIX Thesaurus:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>")
        .append("\n");
    buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
    buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
    buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
    buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
    buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
    buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
    buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
    buf.append("PREFIX ncicp:<http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#>")
        .append("\n");
    buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
    return buf.toString();
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
    for (int i = 0; i < stageConcepts.size(); i++) {
      String code = (String) stageConcepts.elementAt(i);
      stageConceptHashMap.put(code, hh.getLabel(code));
    }
    return stageConceptHashMap;
  }

  public HashMap generateGradeConceptHashMap(Vector gradeConcepts) {
    HashMap gradeConceptHashMap = new HashMap();
    for (int i = 0; i < gradeConcepts.size(); i++) {
      String code = (String) gradeConcepts.elementAt(i);
      gradeConceptHashMap.put(code, hh.getLabel(code));
    }
    return gradeConceptHashMap;
  }

}