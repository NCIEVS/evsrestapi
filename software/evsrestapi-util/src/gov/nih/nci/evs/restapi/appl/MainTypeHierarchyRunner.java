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

public class MainTypeHierarchyRunner {

	public static void countColumn(String filename, int column) {
		int knt = 0;

		Vector v = gov.nih.nci.evs.restapi.util.Utils.readFile(filename);
		int total = v.size()-1;

		String firstLine = (String) v.elementAt(0);
		Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(firstLine, '\t');
		String columnLabel = (String) u.elementAt(column);

		for (int i=1; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line, '\t');
			String value = (String) u.elementAt(column);
			if (value.compareTo("true") == 0) {
				//System.out.println(line);
				knt++;
			}
		}
		System.out.println(columnLabel + " " + knt + " of " + total);
	}

	public static void main(String[] args) {
/*
		String serviceUrl = ConfigurationController.serviceUrl;
		String named_graph = ConfigurationController.namedGraph;
		String username = ConfigurationController.username;
		String password = ConfigurationController.password;
*/
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

		String ncit_version = mthd.getVersion();
		System.out.println("version " + ncit_version);
		Vector broad_category_vec = mthd.get_broad_category_vec();
		//StringUtils.dumpVector("broad_category_vec", broad_category_vec);
		HashSet main_type_set = mthd.get_main_type_set();
		Vector<String> parent_child_vec = mthd.get_parent_child_vec(named_graph);

		System.out.println(	"parent_child_vec: " + parent_child_vec.size());

		Vector v1 = mthd.getDiseaseIsStageSourceCodes(named_graph);
		Vector v2 = mthd.getDiseaseIsGradeSourceCodes(named_graph);
        HashMap stageConceptHashMap = mthd.generateStageConceptHashMap(v1);
        HashMap gradeConceptHashMap = mthd.generateGradeConceptHashMap(v2);

	    HashSet ctrp_biomarker_set = mthd.get_ctrp_biomarker_set();
	    HashSet ctrp_reference_gene_set = mthd.get_ctrp_reference_gene_set();


System.out.println("====================== Instantiating MainTypeHierarchy ======================");

        MainTypeHierarchy mth = new MainTypeHierarchy(ncit_version, parent_child_vec, main_type_set, broad_category_vec,
             stageConceptHashMap, gradeConceptHashMap, ctrp_biomarker_set, ctrp_reference_gene_set);
System.out.println("main_type_set: " +  main_type_set.size());

        Vector mth_vec = mth.generate_main_type_hierarchy();
        Utils.saveToFile("MainTypeHierarchy_" + StringUtils.getToday() + ".txt", mth_vec);

        String outputfile = mth.DISEASES_AND_DISORDERS_CODE + ".txt";
        try {
			Vector codes = mth.getTransitiveClosure(mth.DISEASES_AND_DISORDERS_CODE);
			boolean flatFormat = true;
			mth.run(codes, outputfile, flatFormat);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

}

