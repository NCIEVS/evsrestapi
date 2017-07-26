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

public class MainTypeHierarchy {
	static String MAIN_TYPE_TREE = "main_type_tree";
	static String SUBTYPE_TREE = "subtype_tree";
	static String STAGE_TREE = "stage_tree";
	static String DISEASES_AND_DISORDERS_CODE = "C2991";
	static String NEOPLASM_CODE = "C3262";
	static Vector main_type_hierarchy_data = null;

	public static final String[] CTRP_MAIN_CANCER_TYPES = new String[] {
		"C4715", "C4536", "C35850", "C54293", "C9315", "C8990", "C9272", "C9466", "C3871", "C9465",
		"C9105", "C4855", "C4815", "C8054", "C4035", "C3879", "C4906", "C7569", "C3513", "C4911",
		"C3850", "C7927", "C3099", "C27814", "C4436", "C36077", "C35417", "C7109", "C3844", "C3908",
		"C7724", "C9330", "C2955", "C4910", "C9382", "C9291", "C4878", "C2926", "C4917", "C4872",
		"C4908", "C3867", "C7558", "C9039", "C3917", "C4866", "C9061", "C4863", "C40022", "C7352",
		"C9325", "C9385", "C6142", "C8993", "C4912", "C9106", "C4914", "C9312", "C9145", "C2946",
		"C9306", "C3158", "C3359", "C3194", "C3088", "C9087", "C3230", "C3059", "C3224", "C3510",
		"C8711", "C4817", "C3270", "C3790", "C9344", "C2947", "C3716", "C5669", "C3728", "C3267",
		"C6906", "C3403", "C3752", "C3753", "C2996", "C9309", "C3011", "C4290", "C9063", "C3422",
		"C2948", "C4699", "C3161", "C3172", "C3171", "C3174", "C3178", "C7539", "C3167", "C3163",
		"C9290", "C3247", "C4665", "C9349", "C3242", "C3208", "C9357", "C38661", "C3211", "C3773",
		"C7541", "C3411", "C3234", "C61574", "C2991", "C3262", "C2916", "C9118", "C3268", "C3264",
		"C3708", "C27134", "C3809", "C3058", "C3017"};

    Vector parent_child_vec = null;
    HierarchyHelper hh = null;
    HierarchyHelper mth_hh = null;
    HierarchyHelper mth_hh_without_categories = null;

    PathFinder pathFinder = null;

    String ctrp_classification_data = null;
    Vector main_types = null;
    Vector main_type_leaves = null;
    HashSet main_type_set = null;
    Vector subtype_terms = null;
    Vector stage_terms = null;

    HashSet subtype_set = null;
    HashSet stage_set = null;

    HashMap label2CodeMap = new HashMap();
    HashSet ctrp_codes = new HashSet();

    HashMap levelMap = null;
    HashMap stageConceptHashMap = null;
    HashMap gradeConceptHashMap = null;
    Vector category_vec = null;
    HashSet category_hset = null;

    public MainTypeHierarchy() {

    }

    public MainTypeHierarchy(String ncit_version, Vector parent_child_vec) {
		this.parent_child_vec = parent_child_vec;
		this.hh = new HierarchyHelper(parent_child_vec);
		main_type_set = new HashSet();
		for (int i=0; i<CTRP_MAIN_CANCER_TYPES.length; i++) {
			String t = CTRP_MAIN_CANCER_TYPES[i];
			main_type_set.add(t);
		}
		main_types = Utils.hashSet2Vector(main_type_set);
        main_type_hierarchy_data = generate_main_type_hierarchy();

        Vector mth_parent_child_vec = new ASCIITreeUtils().get_parent_child_vec(main_type_hierarchy_data);
        this.mth_hh = new HierarchyHelper(mth_parent_child_vec);
        this.pathFinder = new PathFinder(mth_hh, "NCI_Thesaurus", ncit_version);

        category_vec = get_category_vec();
        category_hset = Utils.vector2HashSet(category_vec);

        Vector mth_parent_child_vec_v2 = new Vector();
        for (int k=0; k<mth_parent_child_vec.size(); k++) {
			String t = (String) mth_parent_child_vec.elementAt(k);
			Vector u = StringUtils.parseData(t, '|');
			String parent_code = (String) u.elementAt(1);
			if (!category_hset.contains(parent_code)) {
				mth_parent_child_vec_v2.add(t);
			}
		}

		try {
			mth_hh_without_categories = new HierarchyHelper(mth_parent_child_vec_v2);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        levelMap = create_max_level_hashmap(main_type_hierarchy_data);

        Vector v = Utils.readFile("DISEASE_IS_STAGE.txt");
 		stageConceptHashMap = new ParserUtils().getCode2LabelHashMap(v);
	    System.out.println("Number of stage terms: " + stageConceptHashMap.keySet().size());

        v = Utils.readFile("DISEASE_IS_GRADE.txt");
		gradeConceptHashMap = new ParserUtils().getCode2LabelHashMap(v);
		System.out.println("Number of grade terms: " + gradeConceptHashMap.keySet().size());
    }

    public MainTypeHierarchy(String ncit_version, Vector parent_child_vec, HashSet main_type_set, Vector category_vec,
        HashMap stageConceptHashMap, HashMap gradeConceptHashMap) {
		this.parent_child_vec = parent_child_vec;
		this.hh = new HierarchyHelper(parent_child_vec);
        if (main_type_set == null || main_type_set.size() == 0) {
			this.main_type_set = new HashSet();
			for (int i=0; i<CTRP_MAIN_CANCER_TYPES.length; i++) {
				String t = CTRP_MAIN_CANCER_TYPES[i];
				this.main_type_set.add(t);
			}
		}
		main_types = Utils.hashSet2Vector(this.main_type_set);
        main_type_hierarchy_data = generate_main_type_hierarchy();
        Vector mth_parent_child_vec = new ASCIITreeUtils().get_parent_child_vec(main_type_hierarchy_data);
        this.mth_hh = new HierarchyHelper(mth_parent_child_vec);

        this.pathFinder = new PathFinder(mth_hh, "NCI_Thesaurus", ncit_version);
        this.category_vec = category_vec;
        if (category_vec == null) {
			this.category_vec = get_category_vec();
		}
        category_hset = Utils.vector2HashSet(this.category_vec);
        Vector mth_parent_child_vec_v2 = new Vector();
        for (int k=0; k<mth_parent_child_vec.size(); k++) {
			String t = (String) mth_parent_child_vec.elementAt(k);
			Vector u = StringUtils.parseData(t, '|');
			String parent_code = (String) u.elementAt(1);
			if (!category_hset.contains(parent_code)) {
				mth_parent_child_vec_v2.add(t);
			}
		}

		try {
			mth_hh_without_categories = new HierarchyHelper(mth_parent_child_vec_v2);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        levelMap = create_max_level_hashmap(main_type_hierarchy_data);
        this.stageConceptHashMap = stageConceptHashMap;
        this.gradeConceptHashMap = gradeConceptHashMap;
    }

    public Paths find_path_to_main_type_roots(String code) {
        return pathFinder.findPathsToRoots(code, category_hset);
	}

    public HashMap create_max_level_hashmap(Vector w) {
		HashMap hmap = create_level_hashmap(w);
		HashMap map = new HashMap();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector v = (Vector) hmap.get(key);
			int maxLevel = findMaximumLevel(v);
			map.put(key, new Integer(maxLevel));
		}
		return map;
	}

    public HashMap createMaxLevelHashMap(String asciitreefile) {
		HashMap hmap = createLevelHashMap(asciitreefile);
		HashMap map = new HashMap();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector v = (Vector) hmap.get(key);
			int maxLevel = findMaximumLevel(v);
			map.put(key, new Integer(maxLevel));
		}
		return map;
	}

    public boolean isMainType(String code) {
		return main_type_set.contains(code);
	}

    public boolean isDiseaseGrade(String code) {
		return gradeConceptHashMap.containsKey(code);
	}

    public boolean isDiseaseStage(String code) {
		return stageConceptHashMap.containsKey(code);
	}

    public boolean isSubtype(String code) {
		try {
			if (isDiseaseStage(code)) {
				String label = (String) stageConceptHashMap.get(code);
				label = label.toLowerCase();
				if (label.indexOf("stage") == -1) return true;
				return false;
			}
			if (isDiseaseGrade(code)) {
				return false;
			}
			if (isMainType(code)) {
				Vector sups = mth_hh_without_categories.getSuperclassCodes(code);
				if (sups != null && sups.size() > 0) {
					return true;
				} else {
					return false;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return true;
	}

    public Vector generate_main_type_hierarchy() {
		if (main_type_set == null) {
			return null;
		}
		return generate_embedded_hierarchy(DISEASES_AND_DISORDERS_CODE, main_type_set);
	}

	public Vector generate_embedded_hierarchy(String rootCode, HashSet nodeSet) {
		return generate_embedded_hierarchy(rootCode, nodeSet, true);
	}

	public Vector generate_embedded_hierarchy(String rootCode, HashSet nodeSet, boolean trim) {
		Vector v = generateEmbeddedHierarchy(rootCode, nodeSet, trim);
        HierarchyHelper hh = new HierarchyHelper(v);
        Vector w = hh.exportTree();
        w = new ASCIITreeUtils().removeRootNode(w);
        return w;
	}

    public void generateMainTypeHierarchy(String outputfile) {
		generateEmbeddedHierarchy(outputfile, DISEASES_AND_DISORDERS_CODE, main_type_set);
	}

	public void generateEmbeddedHierarchy(String treefile, String rootCode, HashSet nodeSet) {
		generateEmbeddedHierarchy(treefile, rootCode, nodeSet, true);
	}

	public void generateEmbeddedHierarchy(String treefile, String rootCode, HashSet nodeSet, boolean trim) {
		Vector v = generateEmbeddedHierarchy(rootCode, nodeSet, trim);
        HierarchyHelper hh = new HierarchyHelper(v);
        Vector w = hh.exportTree();
        //KLO
        w = new ASCIITreeUtils().removeRootNode(w);
        Utils.saveToFile(treefile, w);
	}

	public Vector createEmbeddedHierarchy(String rootCode, HashSet nodeSet) {
		return createEmbeddedHierarchy(rootCode, nodeSet, true);
	}

	public Vector createEmbeddedHierarchy(String rootCode, HashSet nodeSet, boolean trim) {
		Vector v = generateEmbeddedHierarchy(rootCode, nodeSet, trim);
        HierarchyHelper hh = new HierarchyHelper(v);
        Vector w = hh.exportTree();
        return w;
	}

	public TreeItem trimTree(TreeItem ti, int level, String code, int maxLevel) {
		if (level >= maxLevel) return ti;
        if (ti._code.compareTo(code) == 0 && level < maxLevel) {
			return null;
		}

		List<TreeItem> children = ti._assocToChildMap.get(Constants.ASSOCIATION_NAME);
		if (children != null && children.size() > 0) {
			List<TreeItem> new_children = new ArrayList<TreeItem>();
			for (int i=0; i<children.size(); i++) {
				TreeItem child_ti = (TreeItem) children.get(i);
				child_ti = trimTree(child_ti, level+1, code, maxLevel);
				if (child_ti != null) {
					if (child_ti._code.compareTo(code) != 0) {
						new_children.add(child_ti);
					} else {
					    if (child_ti._code.compareTo(code) == 0 && level >= maxLevel) {
							new_children.add(child_ti);
						}
					}
				}
			}
			ti._assocToChildMap.put(Constants.ASSOCIATION_NAME, new_children);
		}
		return ti;
	}

	public HashMap create_level_hashmap(Vector v) {
		ASCIITreeUtils utils = new ASCIITreeUtils();
	    HashMap hmap = new HashMap();
	    for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			int level = utils.getLevel(line);
			String code = utils.extractCode(line);
			Vector w = new Vector();
			if (hmap.containsKey(code)) {
				w = (Vector) hmap.get(code);
			}
			Integer int_obj = new Integer(level);
			if (!w.contains(int_obj)) {
				w.add(int_obj);
			}
			hmap.put(code, w);
		}
		return hmap;
	}

	public HashMap createLevelHashMap(String asciitreefile) {
		ASCIITreeUtils utils = new ASCIITreeUtils();
	    HashMap hmap = new HashMap();
	    Vector v = Utils.readFile(asciitreefile);
	    for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			int level = utils.getLevel(line);
			String code = utils.extractCode(line);
			Vector w = new Vector();
			if (hmap.containsKey(code)) {
				w = (Vector) hmap.get(code);
			}
			Integer int_obj = new Integer(level);
			if (!w.contains(int_obj)) {
				w.add(int_obj);
			}
			hmap.put(code, w);
		}
		return hmap;
	}

	public Vector findMultiples(HashMap hmap) {
		Vector w2 = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector w = (Vector) hmap.get(key);
			if (w.size() > 1) {
				int maxLevel = findMaximumLevel(w);
				w2.add(key + "|" + maxLevel);
			}
		}
		return w2;
	}

	public int findMaximumLevel(Vector v) {
		int max = -1;
		for (int i=0; i<v.size(); i++) {
			Integer int_obj = (Integer) v.elementAt(i);
			int value = Integer.parseInt(int_obj.toString());
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	public HashMap createLevelHashMap(Vector parent_child_vec) {
		HierarchyHelper hh = new HierarchyHelper(parent_child_vec);
		ASCIITreeUtils utils = new ASCIITreeUtils();
	    HashMap hmap = new HashMap();
	    Vector v = hh.exportTree();//(Vector) parent_child_vec.clone();
	    for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			int level = utils.getLevel(line);
			String code = utils.extractCode(line);
			Vector w = new Vector();
			if (hmap.containsKey(code)) {
				w = (Vector) hmap.get(code);
			}
			Integer int_obj = new Integer(level);
			if (!w.contains(int_obj)) {
				w.add(int_obj);
			}
			hmap.put(code, w);
		}
		return hmap;
	}

    public TreeItem trim_tree(Vector parent_child_vec) {
	    HashMap hmap = createLevelHashMap(parent_child_vec);
	    Vector multiples = findMultiples(hmap);
        TreeItem root = new ASCIITreeUtils().createTreeItem(parent_child_vec);
        for (int i=0; i<multiples.size(); i++) {
			String line = (String) multiples.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			String t2 = (String) u.elementAt(1);
			int maxLevel = Integer.parseInt(t2);
			root = trimTree(root, 1, code, maxLevel+1);
		}
		return root;
	}

    public Vector generateEmbeddedHierarchy(String rootCode, HashSet nodeSet, boolean trim) {

		if (nodeSet == null) {
			System.out.println("ERROR : nodeSet == null");
			return null;
		}

        Vector v = getEmbeddedHierarchy(rootCode, nodeSet);
		if (v.size() == 0) return v;
        if (!trim) {
			return v;
	    } else {
	    	TreeItem root = trim_tree(v);
	    	v = new ASCIITreeUtils().exportTree(root);
		}
		return v;
	}

	public Vector getEmbeddedHierarchy(String rootCode, HashSet nodeSet) {
        ParserUtils parser = new ParserUtils();
		Vector w = new Vector();
		HashSet visitedNodes = new HashSet();
		FirstInFirstOutQueue queue = new FirstInFirstOutQueue();
		queue.add("@|" + rootCode);
		while (!queue.isEmpty()) {
			String line = (String) queue.remove();
			Vector u = StringUtils.parseData(line, '|');
			String parentCode = parser.getValue((String) u.elementAt(0));
			String code = parser.getValue((String) u.elementAt(1));

			if (!visitedNodes.contains(parentCode)) {
				visitedNodes.add(parentCode);
			}
			if (!visitedNodes.contains(code)) {
				visitedNodes.add(code);
			}

			if (nodeSet.contains(parentCode) && nodeSet.contains(code)) {
				String parentLabel = hh.getLabel(parentCode);
				String childLabel = hh.getLabel(code);
				String record = parentLabel + "|" + parentCode
				              + "|" + childLabel + "|" + code;
				if (!w.contains(record)) {
					w.add(record);
				}

				if (visitedNodes.size() == nodeSet.size()) {
					break;
				}
				Vector v = hh.getSubclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.elementAt(j);
						queue.add(code + "|" + childCode);
					}
			    }
			} else if (nodeSet.contains(parentCode) && !nodeSet.contains(code)) {
				Vector v = hh.getSubclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.elementAt(j);
						queue.add(parentCode + "|" + childCode);
					}
				}
			} else if (!nodeSet.contains(parentCode)) {
				Vector v = hh.getSubclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.elementAt(j);
						queue.add(code + "|" + childCode);
					}
				}
			}
		}
        return w;
	}


	public Vector findCode2MainTypesTree(String rootCode) {
		return findCode2MainTypesTree(rootCode, main_type_set);
	}

	public Vector findMainTypeAncestors(String rootCode) {
		Vector w = new Vector();
		int maxLevel = -1;
	    Vector v = findCode2MainTypesTree(rootCode);
	    for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String parent_label = (String) u.elementAt(0);
			String parent_code = (String) u.elementAt(1);
			String child_label = (String) u.elementAt(2);
			String child_code = (String) u.elementAt(3);

			if (parent_code.compareTo(rootCode) != 0) {
				Integer n1 = (Integer) levelMap.get(parent_code);
			    int i1 = n1.intValue();
				if (i1 > maxLevel) {
					maxLevel = i1;
					w = new Vector();
					w.add(parent_label + "|" + parent_code + "|" + maxLevel);
				} else if (i1 == maxLevel) {
					String t = parent_label + "|" + parent_code + "|" + maxLevel;
					if (!w.contains(t)) {
						w.add(t);
					}
				}
			}

			if (child_code.compareTo(rootCode) != 0) {
				Integer n2 = (Integer) levelMap.get(child_code);
				int i2 = n2.intValue();
				if (i2 > maxLevel) {
					maxLevel = i2;
					w = new Vector();
					w.add(child_label + "|" + child_code + "|" + maxLevel);
				} else if (i2 == maxLevel) {
					String t = child_label + "|" + child_code + "|" + maxLevel;
					if (!w.contains(t)) {
						w.add(t);
					}
				}
		    }
		}
		return w;
	}

	public List getMainTypeAncestors(String code) {
        String label = hh.getLabel(code);
        if (label == null) return null;
		List list = new ArrayList();
	    Vector v = findMainTypeAncestors(code);
        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String ancestor_label = (String) u.elementAt(0);
			String ancestor_code = (String) u.elementAt(1);
			Paths paths = find_path_to_main_type_roots(ancestor_code);
			list.add(paths);
		}
		return list;
	}


	public void getCTRPResponse(PrintWriter pw, String code) {
        boolean isMainType = isMainType(code);
        boolean isSubtype = isSubtype(code);
        boolean isDiseaseStage = isDiseaseStage(code);
        boolean isDiseaseGrade = isDiseaseGrade(code);
        String label = hh.getLabel(code);
        if (label == null) return;
        pw.println("\n" + label + " (" + code + ")");
        pw.println("\tMain menu ancestor(s): ");
        Vector v = null;
        if (isSubtype(code)) {
        	v = findMainTypeAncestors(code);
			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				String ancestor_label = (String) u.elementAt(0);
				String ancestor_code = (String) u.elementAt(1);
				Paths paths = find_path_to_main_type_roots(ancestor_code);
				Vector w = PathFinder.formatPaths(paths);
				for (int k=0; k<w.size(); k++) {
					String t = (String) w.elementAt(k);
					pw.println("\t\t" + t);
				}
			}
		} else {
			pw.println("\t\t" + "None");
		}
		pw.println("\tIs a main type? " + isMainType);
		pw.println("\tIs a subype? " + isSubtype);
		pw.println("\tis disease stage? " + isDiseaseStage);
		pw.println("\tis disease grade? " + isDiseaseGrade);
  	}

  	public String getHeading() {
		StringBuffer buf = new StringBuffer();
		buf.append("Label").append("\t");
		buf.append("Code").append("\t");
		buf.append("Main Menu Ancestor(s)").append("\t");
		buf.append("Is Main Type?").append("\t");
		buf.append("Is Subtype?").append("\t");
		buf.append("Is Disease Stage?").append("\t");
		buf.append("Is Disease Grade?").append("\t");
		return buf.toString();
	}

	public void get_ctrp_response(PrintWriter pw, String code) {
		StringBuffer buf = new StringBuffer();
	    Vector v = findMainTypeAncestors(code);
        boolean isMainType = isMainType(code);
        boolean isSubtype = isSubtype(code);
        boolean isDiseaseStage = isDiseaseStage(code);
        boolean isDiseaseGrade = isDiseaseGrade(code);
        String label = hh.getLabel(code);
        if (label == null) {
			return;
		}

        buf.append(label).append("\t").append(code).append("\t");
        StringBuffer ancestors = new StringBuffer();
        if (v != null && v.size() > 0) {

			for (int i=0; i<v.size(); i++) {
				String line = (String) v.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				String ancestor_label = (String) u.elementAt(0);
				String ancestor_code = (String) u.elementAt(1);
				Paths paths = find_path_to_main_type_roots(ancestor_code);
				String t = PathFinder.format_paths(paths);
				ancestors.append(t);
				if (i<v.size()-1) {
					ancestors.append("@");
				}
			}
			buf.append(ancestors.toString()).append("\t");
		} else {
			buf.append("None").append("\t");
		}
		buf.append(isMainType).append("\t");
		buf.append(isSubtype).append("\t");
		buf.append(isDiseaseStage).append("\t");
		buf.append(isDiseaseGrade);
		String t = buf.toString();
		pw.println(t);
  	}

  	public Vector getTransitiveClosure(String code) {
		Vector w = new Vector();
		w.add(code);
		Vector v = hh.getSubclassCodes(code);
		if (v == null) return w;
		for (int i=0; i<v.size(); i++) {
			String child_code = (String) v.elementAt(i);
			Vector u = getTransitiveClosure(child_code);
			if (u != null && u.size() > 0) {
				w.addAll(u);
			}
		}
		w = removeDuplicates(w);
		return w;
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


	public Vector sortCodesByLabels(Vector codes) {
		Vector v = new Vector();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			String label = hh.getLabel(code);
			v.add(label + "|" + code);
		}
		v = new SortUtils().quickSort(v);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String code = (String) u.elementAt(1);
			w.add(code);
		}
		return w;
	}


  	public void run(Vector v, String outputfile, boolean flatFormat) {
		long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		Vector w = new Vector();
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			if (flatFormat) {
				pw.println(getHeading());
			}
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				String code = null;
				if (t.indexOf("\t") != -1) {
					Vector u = StringUtils.parseData(t, '\t');
					code = (String) u.elementAt(0);
			    } else {
					code = t;
				}
				w.add(code);
			}
			w = sortCodesByLabels(w);
			for (int i=0; i<w.size(); i++) {
				String code = (String) w.elementAt(i);
				if (flatFormat) {
					get_ctrp_response(pw, code);
				} else {
					getCTRPResponse(pw, code);
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
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

  	public void run(String ctrpdatafile, String outputfile, boolean flatFormat) {
		Vector v = Utils.readFile(ctrpdatafile);
		run(v, outputfile, flatFormat);
	}

    public void testOneCode(String code) {
		Vector v = new Vector();
		v.add(code);
		String outputfile = code + ".txt";
		boolean flatFormat = false;
        run(v, outputfile, flatFormat);
	}

	public Vector findCode2MainTypesTree(String rootCode, HashSet nodeSet) {
        ParserUtils parser = new ParserUtils();
		Vector w = new Vector();
		HashSet visitedNodes = new HashSet();
		FirstInFirstOutQueue queue = new FirstInFirstOutQueue();
		queue.add("@|" + rootCode);
		while (!queue.isEmpty()) {
			String line = (String) queue.remove();
			Vector u = StringUtils.parseData(line, '|');
			String parentCode = parser.getValue((String) u.elementAt(0));
			String code = parser.getValue((String) u.elementAt(1));

			if (!visitedNodes.contains(parentCode)) {
				visitedNodes.add(parentCode);
			}
			if (!visitedNodes.contains(code)) {
				visitedNodes.add(code);
			}

			if (nodeSet.contains(parentCode) && nodeSet.contains(code)) {
				String parentLabel = hh.getLabel(parentCode);
				String childLabel = hh.getLabel(code);
				String record = parentLabel + "|" + parentCode
				              + "|" + childLabel + "|" + code;
				if (!w.contains(record)) {
					w.add(record);
				}

				if (visitedNodes.size() == nodeSet.size()) {
					break;
				}
				Vector v = hh.getSuperclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.elementAt(j);
						queue.add(code + "|" + childCode);
					}
			    }
			} else if (nodeSet.contains(parentCode) && !nodeSet.contains(code)) {
				Vector v = hh.getSuperclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.elementAt(j);
						queue.add(parentCode + "|" + childCode);
					}
				}
			} else if (!nodeSet.contains(parentCode)) {
				Vector v = hh.getSuperclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.elementAt(j);
						queue.add(code + "|" + childCode);
					}
				}
			}
		}
		if (w.size() == 0) {
			String parentLabel = hh.getLabel(rootCode);
			String childLabel = hh.getLabel(DISEASES_AND_DISORDERS_CODE);
			String record = parentLabel + "|" + rootCode
						  + "|" + childLabel + "|" + DISEASES_AND_DISORDERS_CODE;
			w.add(record);
		}
        return w;
	}

/*
Disease or Disorder (C2991)
	Neoplasm (C3262)
		Carcinoma (C2916)
*/

    public String getLabel(String code) {
		return hh.getLabel(code);
	}

	public Vector get_category_vec() {
		Vector w = new Vector();
		w.add("C2991");
		w.add("C3262");
		w.add("C2916");
		return w;
	}

    public void dumpPaths(Paths paths) {
		pathFinder.dumpPaths(paths);
	}

	public String formatPaths(Paths paths) {
		StringBuffer buf = new StringBuffer();
		List list = paths.getPaths();
		for (int k=0; k<list.size(); k++) {
			Path path = (Path) list.get(k);
			List conceptList = path.getConcepts();
			for (int k2=0; k2<conceptList.size(); k2++) {
				Concept c = (Concept) conceptList.get(k2);
				String indent = "";
				for (int j=0; j<c.getIdx(); j++) {
					indent = indent + "\t";
				}
				buf.append(indent).append(c.getLabel() + " (" + c.getCode() + ")").append("\n");
			}
		}
		return buf.toString();
	}


    public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String parent_child_file = args[0];
		Vector parent_child_vec = Utils.readFile(parent_child_file);

        Vector v = Utils.readFile("DISEASE_IS_STAGE.txt");
 		HashMap stageConceptHashMap = new ParserUtils().getCode2LabelHashMap(v);

        v = Utils.readFile("DISEASE_IS_GRADE.txt");
		HashMap gradeConceptHashMap = new ParserUtils().getCode2LabelHashMap(v);

		MainTypeHierarchy test = new MainTypeHierarchy("17.06d", parent_child_vec, null, null,
		   stageConceptHashMap, gradeConceptHashMap);

        //MainTypeHierarchy test = new MainTypeHierarchy(parent_child_vec);
        Vector disease_codes = test.getTransitiveClosure(DISEASES_AND_DISORDERS_CODE);
        System.out.println("Number of disease_codes: " + disease_codes.size());
        Utils.saveToFile("disease_codes.txt", disease_codes);
        test.run(disease_codes, "disease_and_disorder_ctrp_response_v1.txt", true);
        test.run(disease_codes, "disease_and_disorder_ctrp_response_v2.txt", false);

        //Neoplasm (Code C3262)
        Vector neoplasm_codes = test.getTransitiveClosure(NEOPLASM_CODE);
        System.out.println("Number of neoplasm_codes: " + neoplasm_codes.size());
        Utils.saveToFile("neoplasm.txt", neoplasm_codes);
        test.run(neoplasm_codes, "neoplasm_ctrp_response_v1.txt", true);
        test.run(neoplasm_codes, "neoplasm_ctrp_response_v2.txt", false);

        System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

