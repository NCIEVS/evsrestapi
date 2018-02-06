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


public class ConceptDetailsBatchRunner {
    JSONUtils jsonUtils = null;
    HTTPUtils httpUtils = null;
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String named_graph_id = ":NHC0";
    String base_uri = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    TreeBuilder treeBuilder = null;//new TreeBuilder(this);
    ParserUtils parser = new ParserUtils();
    Vector parent_child_vec = null;
    gov.nih.nci.evs.restapi.util.OWLSPARQLUtils owlSPARQLUtils = null;
    MainTypeHierarchy mth = null;
    ExportUtils exportUtils = null;
    String ncit_version = "17.07c";
	HashSet main_type_set = null;
	Vector category_vec = null;

	public ConceptDetailsBatchRunner(String serviceUrl, String username, String password,
	    String named_graph,
	    Vector parent_child_vec,
        HashMap stageConceptHashMap,
        HashMap gradeConceptHashMap) {

		this.parent_child_vec = parent_child_vec;
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.owlSPARQLUtils = new gov.nih.nci.evs.restapi.util.OWLSPARQLUtils(serviceUrl, username, password);

		Vector v = this.owlSPARQLUtils.getOntologyVersionInfo(named_graph);
		this.ncit_version = new ParserUtils().getValue((String) v.elementAt(0));

 		MainTypeHierarchyData mthd = new MainTypeHierarchyData(serviceUrl, named_graph);

		main_type_set = mthd.get_main_type_set();
		System.out.println("main_type_set: " + main_type_set.size());
		category_vec = mthd.get_broad_category_vec();
		System.out.println("category_vec: " + category_vec.size());
		Utils.dumpVector("category_vec", category_vec);

		String version = mthd.getVersion();
		System.out.println("version: " + version);

System.out.println("*** Instantiating MainTypeHierarchy " + parent_child_vec.size());

		this.mth = new MainTypeHierarchy(
            ncit_version,
            parent_child_vec,
            main_type_set,
            category_vec,
            stageConceptHashMap,
            gradeConceptHashMap);

		this.httpUtils = new HTTPUtils(serviceUrl, username, password);
        this.jsonUtils = new JSONUtils();
        this.treeBuilder = new TreeBuilder(owlSPARQLUtils);
        this.exportUtils = new ExportUtils(owlSPARQLUtils);
    }

    public HashSet combineMainTypesAndCategories() {
		HashSet hset = main_type_set;
        for (int i=0; i<category_vec.size(); i++) {
			String code = (String) category_vec.elementAt(i);
			hset.add(code);
		}
		return hset;
	}

	public ConceptDetails createConceptDetails(String named_graph, String code) {
        List mainMenuAncestors = mth.getMainMenuAncestors(code);
        Boolean isMainType = new Boolean(mth.isMainType(code));
        Boolean isSubtype = new Boolean(mth.isSubtype(code));
        Boolean isDiseaseStage = new Boolean(mth.isDiseaseStage(code));
        Boolean isDiseaseGrade = new Boolean(mth.isDiseaseGrade(code));
        Boolean isDisease = new Boolean(mth.isDisease(code));
        ConceptDetails cd = exportUtils.buildConceptDetails(named_graph, code,
			mainMenuAncestors,
			isMainType,
			isSubtype,
			isDiseaseStage,
			isDiseaseGrade,
			isDisease);
        return cd;
	}

	public void run(String named_graph, Vector codes) {
		String today = StringUtils.getToday();
        String outputfile = "ConceptDetails_" + today + ".txt";
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			for (int i=0; i<codes.size(); i++) {
				String code = (String) codes.elementAt(i);

				System.out.println(code);

				int j = i+1;

				String label = mth.getLabel(code);

				if (label == null) {
					System.out.println("Label for " + code + " not found???");
				}

				pw.println("(" + j + ") " + label + " (" + code + ")");
				System.out.println("(" + j + ") " + label + " (" + code + ")");
				ConceptDetails cd = createConceptDetails(named_graph, code);
				String json = cd.toJson();
				pw.println(json);
				pw.println("\n");
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

    public void generateMainTypeHierarchy() {
		MainTypeHierarchy.save_main_type_hierarchy();
		this.mth.generate_main_type_label_code_file(combineMainTypesAndCategories());
	}

    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];//"https://sparql-evs-dev.nci.nih.gov/ctrp/?query=";//args[0];
		String named_graph = args[1]; //"http://NCIt_Flattened";
		/*

		String serviceUrl = args[0];
		String username = args[1];
		String password = args[2];
		String named_graph = args[3];
		String parent_child_file = args[4];
		*/
		String parent_child_file = "parent_child.txt";
        Vector parent_child_vec = Utils.readFile(parent_child_file);
        System.out.println("parent_child_vec: " + parent_child_vec.size());

        System.out.println("serviceUrl: " + serviceUrl);
        System.out.println("named_graph: " + named_graph);

        OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
        /*
		String serviceUrl = args[0];
		String username = args[1];
		String password = args[2];
		String named_graph = args[3];
		String parent_child_file = args[4];
        Vector parent_child_vec = Utils.readFile(parent_child_file);

        System.out.println("serviceUrl: " + serviceUrl);
        OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
        */

        Vector disease_is_stage_vec = null;
        String stage_file = "DISEASE_IS_STAGE.txt";
		File f = new File(stage_file);
		if(f.exists() && !f.isDirectory()) {
			disease_is_stage_vec = Utils.readFile(stage_file);
			System.out.println(stage_file + " exists.");
		}  else {
			String association_name = "Disease_Is_Stage";
			disease_is_stage_vec = owlSPARQLUtils.getAssociationSourceCodes(named_graph, association_name);
			Utils.saveToFile(stage_file, disease_is_stage_vec);
		}
		HashMap stageConceptHashMap = new ParserUtils().getCode2LabelHashMap(disease_is_stage_vec);
		System.out.println("Number of stage terms: " + stageConceptHashMap.keySet().size());

        String grade_file = "DISEASE_IS_GRADE.txt";
        Vector disease_is_grade_vec = null;
		f = new File(grade_file);
		if(f.exists() && !f.isDirectory()) {
			disease_is_grade_vec = Utils.readFile(grade_file);
			System.out.println(grade_file + " exists.");
		}  else {
			String association_name = "Disease_Is_Grade";
			disease_is_grade_vec = owlSPARQLUtils.getAssociationSourceCodes(named_graph, association_name);
			Utils.saveToFile(grade_file, disease_is_grade_vec);
		}
		HashMap gradeConceptHashMap = new ParserUtils().getCode2LabelHashMap(disease_is_grade_vec);
		System.out.println("Number of grade terms: " + gradeConceptHashMap.keySet().size());

        System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
        ms = System.currentTimeMillis();

System.out.println("serviceUrl: " + serviceUrl);
System.out.println("named_graph: " + named_graph);
System.out.println("parent_child_vec: " + parent_child_vec.size());
System.out.println("stageConceptHashMap: " + stageConceptHashMap.keySet().size());
System.out.println("gradeConceptHashMap: " + gradeConceptHashMap.keySet().size());


	    ConceptDetailsBatchRunner cdbr = new ConceptDetailsBatchRunner(serviceUrl, null, null, named_graph,
	        parent_child_vec,
            stageConceptHashMap,
            gradeConceptHashMap);

        Vector codes = new Vector();
        /*
        codes.add("C3058");
        codes.add("C125890");
        codes.add("C2924");
        codes.add("C4896");
        codes.add("C54705");
        codes.add("C7057");
        codes.add("C2991");
        codes.add("C3262");
        codes.add("C4897");
        codes.add("C7834");
        codes.add("C48232");
        */
        codes.add("C123181");

        cdbr.run(named_graph, codes);

        cdbr.generateMainTypeHierarchy();
        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));

    }
}
