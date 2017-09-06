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

/*
CTS-API Disease Broad Category Terminology (C138189)
CTS-API Disease Main Type Terminology (C138190)
*/

public class MainTypeHierarchyData {
	//static String CTS_API_Disease_Category_Terminology_Code = "C00000";
	static String CTS_API_Disease_Broad_Category_Terminology_Code = "C138189";
	static String CTS_API_Disease_Main_Type_Terminology_Code = "C138190";

	String[] disease_main_types = null;
	String[] disease_broad_categories = null;
	HashSet main_type_set = null;
	Vector<String> broad_category_vec = null;
	ArrayList<String> broad_category_list = null;

	String serviceUrl = null;
	String version = null;

    public MainTypeHierarchyData(String serviceUrl, String named_graph) {
		OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
		this.disease_main_types = owlSPARQLUtils.get_concept_in_subset_codes(named_graph, CTS_API_Disease_Main_Type_Terminology_Code);
		this.disease_broad_categories = owlSPARQLUtils.get_concept_in_subset_codes(named_graph, CTS_API_Disease_Broad_Category_Terminology_Code);
        main_type_set = new HashSet();
        for (int i=0; i<disease_main_types.length; i++) {
			String code = disease_main_types[i];
			main_type_set.add(code);
		}
		broad_category_list = new ArrayList<String>();
		broad_category_vec = new Vector();
        for (int i=0; i<disease_broad_categories.length; i++) {
			String code = disease_broad_categories[i];
			broad_category_list.add(code);
			broad_category_vec.add(code);
		}
		version = owlSPARQLUtils.get_ontology_version(named_graph);
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

	public ArrayList<String> get_broad_category_list() {
		return this.broad_category_list;
	}

	public Vector<String> get_broad_category_vec() {
		return this.broad_category_vec;
	}
}