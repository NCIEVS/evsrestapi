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

public class EmbeddedHierarchy {
	static final String ROOT_NODE = "Root node";
	static final String ROOT_NODE_CODE = "<Root>";

	static final String UNUSED_SUBSET_CONCEPT_CODE = "C103175";
	static final String TERMINOLOGY_SUBSET_CODE = "C54443";

	static final String parent_child_file = "parent_child.txt";
	static final String vs_header_concept_file = "vs_header_concepts.txt";

	int multiple_count = 0;

    Vector parent_child_vec = null;
    HashSet node_set = null;

    HierarchyHelper hh = null;
    HierarchyHelper eh_hh = null;

    PathFinder pathFinder = null;

    HashMap label2CodeMap = new HashMap();

    HashMap levelMap = null;

    HashMap embeddedHierarchyCode2LabelHashMap = null;

    static int TRAVERSE_UP = 1;
    static int TRAVERSE_DOWN = 2;

    public EmbeddedHierarchy() {

    }

    public EmbeddedHierarchy(Vector parent_child_vec) {
		this.parent_child_vec = parent_child_vec;
		this.hh = new HierarchyHelper(parent_child_vec);
	}

	public void set_embedded_hierarchy(HierarchyHelper hh) {
		this.eh_hh = hh;
	}

    public HashMap createEmbeddedHierarchyCode2LabelHashMap(Vector v) {
		//GAIA Terminology|C125481|GAIA Preeclampsia Level of Diagnostic Certainty Terminology|C126860
		//CDISC Questionnaire Terminology|C100110|CDISC Questionnaire ADAS-Cog CDISC Version Test Code Terminology|C100132
		HashMap hmap = new HashMap();
        for (int k=0; k<v.size(); k++) {
			String t = (String) v.elementAt(k);
			Vector u = StringUtils.parseData(t, '|');
			String parent_label = (String) u.elementAt(0);
			String parent_code = (String) u.elementAt(1);
			String child_label = (String) u.elementAt(2);
			String child_code = (String) u.elementAt(3);
			hmap.put(parent_code, parent_label);
			hmap.put(child_code, child_label);
		}
		return hmap;
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

    public String getLabel(String code) {
		return hh.getLabel(code);
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

    public HierarchyHelper getHierarchyHelper() {
		return this.hh;
	}

	public HashMap getLevelMap() {
   	    return this.levelMap;
	}

	public void printTree(Vector v) {
		HierarchyHelper hh = new HierarchyHelper(v);
		hh.printTree();
	}

    public HashSet getPublishedValueSetHeaderConceptCodes(Vector v) {
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			//SPL Shape Terminology|C54454|Publish_Value_Set|Yes
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

	public Vector getSuperclassCodes(String code) {
		return hh.getSuperclassCodes(code);
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
		w = new SortUtils().quickSort(w);
        return w;
	}

    public Vector identifyOrphanTerminologySubsets(Vector orphan_codes) {
		if (orphan_codes == null) return null;
		Vector w = new Vector();
        for (int k=0; k<orphan_codes.size(); k++) {
			String orphan_code = (String) orphan_codes.elementAt(k);
			String orphan_label = getLabel(orphan_code);
			Vector superclasses = getSuperclassCodes(orphan_code);
			if (superclasses.contains(UNUSED_SUBSET_CONCEPT_CODE)) {
				System.out.println(orphan_label + " (" + orphan_code + ")" + " is unused.");

			} else {
				Vector superclass_label_and_code_vec = new Vector();
				for (int j=0; j<superclasses.size(); j++) {
					String t = (String) superclasses.elementAt(j);
					String t_label = getLabel(t);
					superclass_label_and_code_vec.add(t_label + " (" + t + ")");
				}
				if (superclasses.contains(TERMINOLOGY_SUBSET_CODE)) {
					w.add(orphan_label + "|" + orphan_code);
				}
			}
		}
		return w;
	}

    public Vector identifyRootTerminologySubsets(Vector parent_child_vec) {
		HashSet set_parent = new HashSet();
		HashSet set_child = new HashSet();
		for (int i=0; i<parent_child_vec.size(); i++) {
			String line = (String) parent_child_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String parent_code = (String) u.elementAt(1);
			String child_code = (String) u.elementAt(3);
			set_parent.add(parent_code);
			set_child.add(child_code);
		}
		HashSet set1 = (HashSet) set_parent.clone();
		HashSet set2 = (HashSet) set_child.clone();
		set1.removeAll(set2);
		return Utils.hashSet2Vector(set1);
	}

	public Vector generateEmbeddedHierarchyParentChildData(Vector embedded_hierarchy_parent_child_vec, HashSet nodeSet) {
		Vector v = (Vector) embedded_hierarchy_parent_child_vec.clone();

        HashMap code2LableMap = createEmbeddedHierarchyCode2LabelHashMap(v);
        Iterator it = code2LableMap.keySet().iterator();
        Vector w = new Vector();
        while (it.hasNext()) {
			String node = (String) it.next();
			String node_label = getLabel(node);
			w.add(node_label + " (" + node + ")");
		}
        it = nodeSet.iterator();
        int lcv = 0;
        Vector orphans = new Vector();
        Vector orphan_codes = new Vector();
        while (it.hasNext()) {
			lcv++;
			String node = (String) it.next();
			if (!code2LableMap.containsKey(node)) {
				String node_label = getLabel(node);
				orphans.add(node_label + " (" + node + ")");
				orphan_codes.add(node);
			}
		}
		//StringUtils.dumpVector("orphans", orphans);
		Vector orphan_vs = identifyOrphanTerminologySubsets(orphan_codes);
		Vector root_vs = identifyRootTerminologySubsets(v);
/*
		for (int k=0; k<orphan_vs.size(); k++) {
			String t = (String) orphan_vs.elementAt(k);
			v.add(ROOT_NODE + "|" + ROOT_NODE_CODE + "|" + hh.getLabel(t) + "|" + t);
		}
		for (int k=0; k<root_vs.size(); k++) {
			String t = (String) root_vs.elementAt(k);
			v.add(ROOT_NODE + "|" + ROOT_NODE_CODE + "|" + hh.getLabel(t) + "|" + t);
		}
*/
		for (int k=0; k<orphan_vs.size(); k++) {
			String t = (String) orphan_vs.elementAt(k);
			v.add(ROOT_NODE + "|" + ROOT_NODE_CODE + "|" + t);
			System.out.println("(orphan_vs) " + ROOT_NODE + "|" + ROOT_NODE_CODE + "|" + t);
		}
		for (int k=0; k<root_vs.size(); k++) {
			String t = (String) root_vs.elementAt(k);
			v.add(ROOT_NODE + "|" + ROOT_NODE_CODE + "|" + hh.getLabel(t) + "|" + t);
			System.out.println("(root_vs) " + ROOT_NODE + "|" + ROOT_NODE_CODE + "|" + hh.getLabel(t) + "|" + t);
		}

        v = new SortUtils().quickSort(v);
		return v;
	}

	public void generateEmbeddedHierarchyFile(String outputfile, Vector eh_vec) {
		System.out.println("eh_vec.size(): " + eh_vec.size());
		eh_hh = new HierarchyHelper(eh_vec);
		if (outputfile == null) outputfile = "vs_ascii_tree.txt";
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			eh_hh.printTree(pw);

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public String getIndentation(int level) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<level; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}


	public void traverseEmbeddedHierarchy(String code) {
		traverseEmbeddedHierarchy(code, TRAVERSE_UP, 0);
	}

	public void traverseEmbeddedHierarchy(String code, int direction) {
		traverseEmbeddedHierarchy(code, direction, 0);
	}


	public void traverseEmbeddedHierarchy(String code, int direction, int level) {
		if (eh_hh == null) return;
		String label = eh_hh.getLabel(code);
		//System.out.println(getIndentation(level) + label + " (" + code + ")");
        Vector codes = null;
        if (direction == TRAVERSE_UP) {
			codes = eh_hh.getSuperclassCodes(code);
		} else {
			codes = eh_hh.getSubclassCodes(code);
		}
        if (codes == null || codes.size() == 0) return;
		for (int j=0; j<codes.size(); j++) {
			String concept_code = (String) codes.elementAt(j);
			traverseEmbeddedHierarchy(concept_code, direction, level+1);
		}
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		Vector parent_child_vec = Utils.readFile(parent_child_file);
        EmbeddedHierarchy eh = new EmbeddedHierarchy(parent_child_vec);
        String rootCode = Constants.TERMINOLOGY_SUBSET_CODE;
        String label = eh.getLabel(rootCode);
        System.out.println("Root: " + label + " (" + rootCode + ")");
        Vector vs_header_concept_vec = Utils.readFile(vs_header_concept_file);
        HashSet nodeSet = eh.getPublishedValueSetHeaderConceptCodes(vs_header_concept_vec);
        Vector v = eh.getEmbeddedHierarchy(rootCode, nodeSet);
        //StringUtils.dumpVector("EmbeddedHierarchy", v);

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

		System.out.println("(*) Number of published value sets: " + nodeSet.size());

        it = nodeSet.iterator();
        int lcv = 0;
        Vector orphans = new Vector();
        Vector orphan_codes = new Vector();
        while (it.hasNext()) {
			lcv++;
			String node = (String) it.next();
			System.out.println("(" + lcv + ") " + node);
			if (!code2LableMap.containsKey(node)) {
				String node_label = eh.getLabel(node);
				orphans.add(node_label + " (" + node + ")");
				orphan_codes.add(node);
				//System.out.println("Orphan node: " + node_label + " (" + node + ")");
			}
		}
		//StringUtils.dumpVector("orphans", orphans);
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
				//StringUtils.dumpVector("superclasses of " + superclass_label + " (" + orphan_code + ")", superclass_label_and_code_vec);
			}
		}

		Vector orphanTerminologySubsets = eh.identifyOrphanTerminologySubsets(orphan_codes);
		//StringUtils.dumpVector("orphanTerminologySubsets", orphanTerminologySubsets);
        Vector roots = eh.identifyRootTerminologySubsets(embedded_hierarchy_parent_child_vec);
        //StringUtils.dumpVector("roots", roots);
        Vector eh_vec = eh.generateEmbeddedHierarchyParentChildData(embedded_hierarchy_parent_child_vec, nodeSet);
        //StringUtils.dumpVector("eh_vec", eh_vec);
        eh.generateEmbeddedHierarchyFile("test_vh_ascii_tree.txt", eh_vec);

        HierarchyHelper hierarchyHelper = new HierarchyHelper(eh_vec);
        eh.set_embedded_hierarchy(hierarchyHelper);

        eh.traverseEmbeddedHierarchy("C74456");
        System.out.println("\n");
        eh.traverseEmbeddedHierarchy("C99074");
        System.out.println("\n");
        eh.traverseEmbeddedHierarchy("C99073");

		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
