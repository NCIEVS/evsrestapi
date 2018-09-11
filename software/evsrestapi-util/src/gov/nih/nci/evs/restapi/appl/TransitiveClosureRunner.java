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

public class TransitiveClosureRunner {
	private MainTypeHierarchy mth = null;
	private HierarchyHelper hh = null;
	private Vector parent_child_vec = null;

    public TransitiveClosureRunner(MainTypeHierarchy mth) {
		this.mth = mth;
		this.hh = mth.getHierarchyHelper();
    }

    public HierarchyHelper getHierarchyHelper() {
		return hh;
	}

    public TransitiveClosureRunner(Vector parent_child_vec) {
		this.parent_child_vec = parent_child_vec;
		hh = new HierarchyHelper(parent_child_vec);
    }

	public Vector removeDuplicates(Vector codes) {
		HashSet hset = new HashSet();
		Vector w = new Vector();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			if (!hset.contains(code)) {
				hset.add(code);
				w.add(code);
			}
		}
		return w;
	}

  	public Vector getTransitiveClosure(String code) {
		Vector w = new Vector();
		String label = hh.getLabel(code);
		Vector v = hh.getSubclassCodes(code);
		if (v == null) return w;
		for (int i=0; i<v.size(); i++) {
			String child_code = (String) v.elementAt(i);
			String child_label = hh.getLabel(child_code);
			w.add(label + "|" + code + "|" + child_label + "|" + child_code);
			Vector u = getTransitiveClosure(child_code);
			if (u != null && u.size() > 0) {
				w.addAll(u);
			}
		}
		w = removeDuplicates(w);
		return w;
	}


    public Vector run(Vector codes) {
		Vector v = new Vector();
        for (int i=0; i<codes.size(); i++) {
			String code_root = (String) codes.elementAt(i);
			Vector w = getTransitiveClosure(code_root);
			v.addAll(w);
		}
		v = new SortUtils().quickSort(v);
        return v;
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		/*
		MetadataUtils test = new MetadataUtils(serviceUrl);
		String codingScheme = "NCI_Thesaurus";

		String version = test.getLatestVersion(codingScheme);
		System.out.println(codingScheme);
		System.out.println(version);
		String named_graph = test.getNamedGraph(codingScheme);
		System.out.println(named_graph);

		if (args.length > 1) {
        	named_graph = args[1];
		}
		*/

		/*
		String serviceUrl = args[0];
		System.out.println(serviceUrl);

		String named_graph = args[1];
		String inputfile = args[2];
		String outputfile = args[3];

		MainTypeHierarchyData mthd = new MainTypeHierarchyData(serviceUrl, named_graph);
		String ncit_version = mthd.getVersion();
		System.out.println("version " + ncit_version);
		Vector broad_category_vec = mthd.get_broad_category_vec();
		HashSet main_type_set = mthd.get_main_type_set();
		Vector<String> parent_child_vec = mthd.get_parent_child_vec(named_graph);
		Vector v1 = mthd.getDiseaseIsStageSourceCodes(named_graph);
		Vector v2 = mthd.getDiseaseIsGradeSourceCodes(named_graph);
        HashMap stageConceptHashMap = mthd.generateStageConceptHashMap(v1);
        HashMap gradeConceptHashMap = mthd.generateGradeConceptHashMap(v2);

	    HashSet ctrp_biomarker_set = mthd.get_ctrp_biomarker_set();
	    HashSet ctrp_reference_gene_set = mthd.get_ctrp_reference_gene_set();

        MainTypeHierarchy mth = new MainTypeHierarchy(ncit_version, parent_child_vec, main_type_set, broad_category_vec,
             stageConceptHashMap, gradeConceptHashMap, ctrp_biomarker_set, ctrp_reference_gene_set);

        TransitiveClosureRunner runner = new TransitiveClosureRunner(mth);
        */

        String parent_child_file = args[0];
        Vector parent_child_vec = Utils.readFile(parent_child_file);
        String root = args[1];
        Vector codes = new Vector();
        codes.add(root);
        TransitiveClosureRunner runner = new TransitiveClosureRunner(parent_child_vec);
        Vector v = runner.run(codes);
        String outputfile = "parent_child_" + root + ".txt";
        Utils.saveToFile(outputfile, v);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

