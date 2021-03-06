package gov.nih.nci.evs.api.util.ext;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.util.PathFinder;
import net.sf.cglib.core.Constants;

@SuppressWarnings({
    "unchecked", "rawtypes"
})
public class MainTypeHierarchy {
  public int multiple_count = 0;

  static String MAIN_TYPE_TREE = "main_type_tree";

  static String SUBTYPE_TREE = "subtype_tree";

  static String STAGE_TREE = "stage_tree";

  static String DISEASE_DISORDER_OR_FINDING_CODE = "C7057";

  static String FINDING_CODE = "C3367";

  static String DISEASES_AND_DISORDERS_CODE = "C2991";

  static String NEOPLASM_CODE = "C3262";

  static Vector main_type_hierarchy_data = null;

  static String RECURRENT = "recurrent";

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

  HashSet subtype_set = null;

  HashMap label2CodeMap = new HashMap();

  HashSet ctrp_codes = new HashSet();

  HashMap levelMap = null;

  HashMap stageConceptHashMap = null;

  HashMap gradeConceptHashMap = null;

  Vector category_vec = null;

  HashSet category_hset = null;

  static String NCI_THESAURUS = "NCI_Thesaurus";

  public MainTypeHierarchy() {

  }

  public MainTypeHierarchy(Vector parent_child_vec) {
    this.parent_child_vec = parent_child_vec;
    this.hh = new HierarchyHelper(parent_child_vec);
  }

  public MainTypeHierarchy(String ncit_version, Vector parent_child_vec, HashSet main_type_set,
      Vector category_vec, Vector stageConcepts, Vector gradeConcepts, HashSet ctrp_biomarker_set,
      HashSet ctrp_reference_gene_set) {

    this.parent_child_vec = parent_child_vec;
    this.hh = new HierarchyHelper(parent_child_vec);
    this.main_type_set = main_type_set;
    this.category_vec = category_vec;
    category_hset = Utils.vector2HashSet(category_vec);

    for (int i = 0; i < category_vec.size(); i++) {
      String code = (String) category_vec.elementAt(i);
      this.main_type_set.add(code);
    }

    main_types = Utils.hashSet2Vector(main_type_set);
    main_type_hierarchy_data = generate_main_type_hierarchy();
    Vector mth_parent_child_vec =
        new ASCIITreeUtils().get_parent_child_vec(main_type_hierarchy_data);
    this.mth_hh = new HierarchyHelper(mth_parent_child_vec);

    this.pathFinder = new PathFinder(mth_hh, NCI_THESAURUS, ncit_version);

    Vector mth_parent_child_vec_v2 = new Vector();
    for (int k = 0; k < mth_parent_child_vec.size(); k++) {
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

  public MainTypeHierarchy(String ncit_version, Vector parent_child_vec, HashSet main_type_set,
      Vector category_vec, HashMap stageConceptHashMap, HashMap gradeConceptHashMap,
      HashSet ctrp_biomarker_set, HashSet ctrp_reference_gene_set) {
    this.parent_child_vec = parent_child_vec;
    this.hh = new HierarchyHelper(parent_child_vec);
    this.main_type_set = main_type_set;
    this.category_vec = category_vec;
    category_hset = Utils.vector2HashSet(category_vec);

    for (int i = 0; i < category_vec.size(); i++) {
      String code = (String) category_vec.elementAt(i);
      this.main_type_set.add(code);
    }

    main_types = Utils.hashSet2Vector(main_type_set);

    main_type_hierarchy_data = generate_main_type_hierarchy();
    Vector mth_parent_child_vec =
        new ASCIITreeUtils().get_parent_child_vec(main_type_hierarchy_data);
    this.mth_hh = new HierarchyHelper(mth_parent_child_vec);

    this.pathFinder = new PathFinder(mth_hh, NCI_THESAURUS, ncit_version);

    Vector mth_parent_child_vec_v2 = new Vector();
    for (int k = 0; k < mth_parent_child_vec.size(); k++) {
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
    final Paths paths = new Paths();
    // TODO: get codes and make paths here
    paths.setPaths(pathFinder.getRoots());
    return paths;
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



  public boolean isNotDisease(String code) {
    if (code.compareTo(DISEASE_DISORDER_OR_FINDING_CODE) == 0)
      return false;
    if (code.compareTo(DISEASES_AND_DISORDERS_CODE) == 0)
      return false;
    if (category_vec.contains(code))
      return false;

    Vector v = findMainMenuAncestors(code);
    if (v == null || v.size() == 0) {
      return true;
    }
    return false;
  }

  public boolean isDisease(String code) {
    return !isNotDisease(code);
  }

  public boolean isSubtype(String code) {
    try {
      if (code.compareTo(DISEASE_DISORDER_OR_FINDING_CODE) == 0)
        return false;
      if (isNotDisease(code)) {
        return false;
      }
      // 09062017
      String label = hh.getLabel(code);
      label = label.toLowerCase();
      if (label.startsWith(RECURRENT))
        return false;

      if (isDiseaseStage(code)) {
        String name = (String) stageConceptHashMap.get(code);
        name = name.toLowerCase();
        if (name.indexOf("stage") == -1)
          return true;
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

      if (isDiseaseGrade(code)) {
        String name = (String) gradeConceptHashMap.get(code);
        name = name.toLowerCase();
        if (name.indexOf("grade") == -1) {
          return true;
        }
        return false;
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

  public void generateEmbeddedHierarchy(String treefile, String rootCode, HashSet nodeSet,
    boolean trim) {
    Vector v = generateEmbeddedHierarchy(rootCode, nodeSet, trim);
    HierarchyHelper hh = new HierarchyHelper(v);
    Vector w = hh.exportTree();
    // KLO
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
    if (level >= maxLevel)
      return ti;
    if (ti._code.compareTo(code) == 0 && level < maxLevel) {
      return null;
    }

    List<TreeItem> children = ti._assocToChildMap.get(Constants.ASSOCIATION_NAME);
    if (children != null && children.size() > 0) {
      List<TreeItem> new_children = new ArrayList<TreeItem>();
      for (int i = 0; i < children.size(); i++) {
        TreeItem child_ti = (TreeItem) children.get(i);
        child_ti = trimTree(child_ti, level + 1, code, maxLevel);
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
    for (int i = 0; i < v.size(); i++) {
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
    for (int i = 0; i < v.size(); i++) {
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
    for (int i = 0; i < v.size(); i++) {
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
    Vector v = hh.exportTree();// (Vector) parent_child_vec.clone();
    for (int i = 0; i < v.size(); i++) {
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
    for (int i = 0; i < multiples.size(); i++) {
      String line = (String) multiples.elementAt(i);
      Vector u = StringUtils.parseData(line, '|');
      String code = (String) u.elementAt(0);
      String t2 = (String) u.elementAt(1);
      int maxLevel = Integer.parseInt(t2);
      root = trimTree(root, 1, code, maxLevel + 1);
    }
    return root;
  }

  public Vector generateEmbeddedHierarchy(String rootCode, HashSet nodeSet, boolean trim) {

    if (nodeSet == null) {
      System.out.println("ERROR : nodeSet == null");
      return null;
    }

    Vector v = getEmbeddedHierarchy(rootCode, nodeSet);
    if (v.size() == 0)
      return v;
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
        String record = parentLabel + "|" + parentCode + "|" + childLabel + "|" + code;
        if (!w.contains(record)) {
          w.add(record);
        }

        if (visitedNodes.size() == nodeSet.size()) {
          break;
        }
        Vector v = hh.getSubclassCodes(code);
        if (v != null) {
          for (int j = 0; j < v.size(); j++) {
            String childCode = (String) v.elementAt(j);
            queue.add(code + "|" + childCode);
          }
        }
      } else if (nodeSet.contains(parentCode) && !nodeSet.contains(code)) {
        Vector v = hh.getSubclassCodes(code);
        if (v != null) {
          for (int j = 0; j < v.size(); j++) {
            String childCode = (String) v.elementAt(j);
            queue.add(parentCode + "|" + childCode);
          }
        }
      } else if (!nodeSet.contains(parentCode)) {
        Vector v = hh.getSubclassCodes(code);
        if (v != null) {
          for (int j = 0; j < v.size(); j++) {
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

  public Vector findMainMenuAncestors(String rootCode) {
    Vector w = new Vector();
    int maxLevel = -1;
    Vector v = findCode2MainTypesTree(rootCode);
    for (int i = 0; i < v.size(); i++) {
      String line = (String) v.elementAt(i);
      Vector u = StringUtils.parseData(line, '|');
      String parent_label = (String) u.elementAt(0);
      String parent_code = (String) u.elementAt(1);
      String child_label = (String) u.elementAt(2);
      String child_code = (String) u.elementAt(3);

      if (parent_code.compareTo(rootCode) != 0) {
        Integer n1 = (Integer) levelMap.get(parent_code);
        if (n1 != null) {
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
      }

      if (child_code.compareTo(rootCode) != 0) {
        Integer n2 = (Integer) levelMap.get(child_code);
        if (n2 != null) {
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
    }
    return w;
  }

  public List getMainMenuAncestors(String code) {
    if (!isSubtype(code) && !isDiseaseStage(code) && !isDiseaseGrade(code)) {
      return null;
    }
    String label = hh.getLabel(code);
    if (label == null)
      return null;
    List list = new ArrayList();
    Vector v = findMainMenuAncestors(code);
    for (int i = 0; i < v.size(); i++) {
      String line = (String) v.elementAt(i);
      Vector u = StringUtils.parseData(line, '|');
      String ancestor_label = (String) u.elementAt(0);
      String ancestor_code = (String) u.elementAt(1);
      Paths paths = find_path_to_main_type_roots(ancestor_code);
      list.add(paths);
    }
    // KLO, 09182017
    list = selectPaths(list);
    return list;
  }

  public void getCTRPResponse(PrintWriter pw, Vector codes) {
    if (codes == null)
      return;
    for (int i = 0; i < codes.size(); i++) {
      String code = (String) codes.elementAt(i);
      getCTRPResponse(pw, code);
    }
  }

  public void getCTRPResponse(PrintWriter pw, String code) {
    boolean isMainType = isMainType(code);
    boolean isSubtype = isSubtype(code);
    boolean isDiseaseStage = isDiseaseStage(code);
    boolean isDiseaseGrade = isDiseaseGrade(code);
    boolean isDisease = isDisease(code);
    String label = hh.getLabel(code);
    if (label == null)
      return;
    pw.println("\n" + label + " (" + code + ")");
    pw.println("\tMain menu ancestor(s): ");
    Vector v = null;
    if (isSubtype(code) || isDiseaseStage(code) || isDiseaseGrade(code)) {
      v = findMainMenuAncestors(code);
      if (v != null && v.size() > 0) {
        for (int i = 0; i < v.size(); i++) {
          String line = (String) v.elementAt(i);
          Vector u = StringUtils.parseData(line, '|');
          String ancestor_label = (String) u.elementAt(0);
          String ancestor_code = (String) u.elementAt(1);
          Paths paths = find_path_to_main_type_roots(ancestor_code);
          Vector w = PathFinder.formatPaths(paths);
          for (int k = 0; k < w.size(); k++) {
            String t = (String) w.elementAt(k);
            pw.println("\t\t" + t);
          }
        }
      }
    } else {
      pw.println("\t\t" + "None");
    }
    pw.println("\tIs a main type? " + isMainType);
    pw.println("\tIs a subype? " + isSubtype);
    pw.println("\tIs disease stage? " + isDiseaseStage);
    pw.println("\tIs disease grade? " + isDiseaseGrade);
    pw.println("\tIs disease? " + isDisease);
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
    buf.append("Is Disease?").append("\t");
    return buf.toString();
  }

  public String format_paths(Paths paths) {
    StringBuffer buf = new StringBuffer();
    List list = paths.getPaths();
    for (int k = 0; k < list.size(); k++) {
      Path path = (Path) list.get(k);
      List conceptList = path.getConcepts();
      for (int k2 = 0; k2 < conceptList.size(); k2++) {
        Concept c = (Concept) conceptList.get(k2);
        buf.append(c.getLabel() + "|" + c.getCode());
        if (k2 < conceptList.size() - 1) {
          buf.append("|");
        }
      }
      if (k < list.size() - 1) {
        buf.append("$");
      }
    }
    return buf.toString();
  }

  public void get_ctrp_response(PrintWriter pw, String code) {
    StringBuffer buf = new StringBuffer();
    Vector v = findMainMenuAncestors(code);
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
      for (int i = 0; i < v.size(); i++) {
        String line = (String) v.elementAt(i);
        Vector u = StringUtils.parseData(line, '|');
        String ancestor_label = (String) u.elementAt(0);
        String ancestor_code = (String) u.elementAt(1);
        Paths paths = find_path_to_main_type_roots(ancestor_code);
        // String t = PathFinder.format_paths(paths);
        String t = format_paths(paths);
        ancestors.append(t);
        if (i < v.size() - 1) {
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
    if (v == null)
      return w;
    for (int i = 0; i < v.size(); i++) {
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
    for (int i = 0; i < codes.size(); i++) {
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
    for (int i = 0; i < codes.size(); i++) {
      String code = (String) codes.elementAt(i);
      String label = hh.getLabel(code);
      v.add(label + "|" + code);
    }
    v = new SortUtils().quickSort(v);
    Vector w = new Vector();
    for (int i = 0; i < v.size(); i++) {
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
      for (int i = 0; i < v.size(); i++) {
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
      for (int i = 0; i < w.size(); i++) {
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
        String record = parentLabel + "|" + parentCode + "|" + childLabel + "|" + code;
        if (!w.contains(record)) {
          w.add(record);
        }

        if (visitedNodes.size() == nodeSet.size()) {
          break;
        }
        Vector v = hh.getSuperclassCodes(code);
        if (v != null) {
          for (int j = 0; j < v.size(); j++) {
            String childCode = (String) v.elementAt(j);
            queue.add(code + "|" + childCode);
          }
        }
      } else if (nodeSet.contains(parentCode) && !nodeSet.contains(code)) {
        Vector v = hh.getSuperclassCodes(code);
        if (v != null) {
          for (int j = 0; j < v.size(); j++) {
            String childCode = (String) v.elementAt(j);
            queue.add(parentCode + "|" + childCode);
          }
        }
      } else if (!nodeSet.contains(parentCode)) {
        Vector v = hh.getSuperclassCodes(code);
        if (v != null) {
          for (int j = 0; j < v.size(); j++) {
            String childCode = (String) v.elementAt(j);
            queue.add(code + "|" + childCode);
          }
        }
      }
    }

    if (w.size() == 0) {
      if (visitedNodes.contains(DISEASES_AND_DISORDERS_CODE)) {
        String parentLabel = hh.getLabel(rootCode);
        String childLabel = hh.getLabel(DISEASES_AND_DISORDERS_CODE);
        String record =
            parentLabel + "|" + rootCode + "|" + childLabel + "|" + DISEASES_AND_DISORDERS_CODE;
        w.add(record);
      }
    }
    return w;
  }

  /*
   * Disease or Disorder (C2991) Neoplasm (C3262) Carcinoma (C2916)
   */

  public String getLabel(String code) {
    return hh.getLabel(code);
  }

  public String formatPaths(Paths paths) {
    StringBuffer buf = new StringBuffer();
    List list = paths.getPaths();
    for (int k = 0; k < list.size(); k++) {
      Path path = (Path) list.get(k);
      List conceptList = path.getConcepts();
      for (int k2 = 0; k2 < conceptList.size(); k2++) {
        Concept c = (Concept) conceptList.get(k2);
        String indent = "";
        for (int j = 0; j < c.getIdx(); j++) {
          indent = indent + "\t";
        }
        buf.append(indent).append(c.getLabel() + " (" + c.getCode() + ")").append("\n");
      }
    }
    return buf.toString();
  }

  public boolean isSubtype2(String code) {
    String name = getLabel(code);
    System.out.println("\n" + name + " (" + code + ")");
    try {
      if (code.compareTo(DISEASE_DISORDER_OR_FINDING_CODE) == 0)
        return false;
      System.out.println("\t(1)" + " isNotDisease? " + isNotDisease(code));
      if (isNotDisease(code)) {
        System.out.println("\t(2)" + code + " " + isNotDisease(code));
        return false;
      }

      System.out.println("\t(3)" + " isDiseaseStage? " + isDiseaseStage(code));
      if (isDiseaseStage(code)) {
        System.out.println("\t(4)" + " isDiseaseStage " + isDiseaseStage(code));

        String label = (String) stageConceptHashMap.get(code);
        label = label.toLowerCase();
        if (label.indexOf("stage") == -1) {
          System.out.println("\t(5)" + " isSubtype? true (does not contains the word stage.)");
          return true;
        }
        System.out.println("\t(6)" + " isSubtype? false -- contains the word stage.");
        return false;
      }

      System.out.println("\t(8)" + " isDiseaseGrade " + isDiseaseGrade(code));

      System.out.println("\t(9)" + " isMainType? " + isMainType(code));
      if (isMainType(code)) {
        Vector sups = mth_hh_without_categories.getSuperclassCodes(code);
        if (sups != null && sups.size() > 0) {

          System.out.println("\t(10) sups != null && sups.size() > 0 -- isSubtype: true");
          return true;
        } else {

          System.out.println("\t(11) sups == null -- isSubtype: false");
          return false;
        }
      }

      System.out.println("\t(12)" + " isDiseaseGrade? " + isDiseaseGrade(code));
      if (isDiseaseGrade(code)) {
        System.out.println("\t(13)" + " isDiseaseGrade " + isDiseaseGrade(code));
        String label = name;
        label = label.toLowerCase();
        if (label.indexOf("grade") == -1) {
          System.out.println("\t(14)" + " isSubtype? true (does not contains the word grade.)");
          return true;
        }
        System.out.println("\t(15)" + " isSubtype? false -- contains the word grade.");
        return false;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    System.out.println("\t(16)" + " isSubtype: true");
    return true;
  }

  public static void save_main_type_hierarchy() {
    String today = StringUtils.getToday();
    Utils.saveToFile("main_type_hierarchy_" + today + ".txt", main_type_hierarchy_data);
  }

  public void generate_main_type_label_code_file(HashSet main_type_set) {
    Vector w = new Vector();
    Iterator it = main_type_set.iterator();
    while (it.hasNext()) {
      String code = (String) it.next();
      String label = hh.getLabel(code);
      w.add(label + "|" + code);
    }

    w = new SortUtils().quickSort(w);
    String today = StringUtils.getToday();
    Utils.saveToFile("main_type_label_and_code_" + today + ".txt", w);
  }

  public static void generate_data_files(String serviceUrl, String named_graph) {
    OWLSPARQLUtils owlSPARQLUtils = new OWLSPARQLUtils(serviceUrl, null, null);
    System.out.println("getHierarchicalRelationships ...");
    Vector v = owlSPARQLUtils.getHierarchicalRelationships(named_graph);
    Vector parent_child_vec = new ParserUtils().toDelimited(v, 4, '|');
    String today = StringUtils.getToday();
    Utils.saveToFile("parent_child_" + today + ".txt", parent_child_vec);

  }

  public void testIsSubtype(Vector codes) {
    for (int i = 0; i < codes.size(); i++) {
      String code = (String) codes.elementAt(i);
      testIsSubtype(code);
    }
  }

  public void testIsSubtype(String code) {
    String label = getLabel(code);
    System.out.println("\n\n" + label + " (" + code + ")");
    boolean isSubtype = isSubtype(code);
    System.out.println("\tisSubtype: " + isSubtype);
    isSubtype2(code);
  }

  public List selectPaths(List list) {
    if (list == null)
      return null;
    if (list.size() <= 1)
      return list;

    HashMap hmap = new HashMap();
    int path_to_category = 0;
    int path_to_main_type = 0;

    for (int i = 0; i < list.size(); i++) {
      Paths paths = (Paths) list.get(i);
      List path_list = paths.getPaths();
      for (int j = 0; j < path_list.size(); j++) {
        Path path = (Path) path_list.get(j);
        List concepts = path.getConcepts();
        int length = concepts.size();
        Concept c0 = (Concept) concepts.get(0);
        Concept c1 = (Concept) concepts.get(length - 1);
        String code_0 = c0.getCode();
        String code_1 = c1.getCode();
        hmap.put(code_0 + "|" + code_1, path);
        if (category_vec.contains(code_1)) {
          path_to_category++;
        } else {
          path_to_main_type++;
        }
      }
    }
    HashMap hmap2 = new HashMap();
    int path_count = hmap.keySet().size();
    if (path_to_main_type > 0) {
      // remove all path_to_category
      Iterator it = hmap.keySet().iterator();
      while (it.hasNext()) {
        String key = (String) it.next();
        Vector u = StringUtils.parseData(key);
        String code_1 = (String) u.elementAt(1);
        if (!category_vec.contains(code_1)) {
          hmap2.put(key, (Path) hmap.get(key));
        }
      }
    }
    HashMap hmap3 = new HashMap();
    // code_0 --> List<Path>
    Iterator it = hmap2.keySet().iterator();
    while (it.hasNext()) {
      String key = (String) it.next();
      Vector u = StringUtils.parseData(key);
      String code_0 = (String) u.elementAt(0);
      Path path = (Path) hmap2.get(key);
      List path_list = new ArrayList();
      if (hmap3.containsKey(code_0)) {
        path_list = (List) hmap3.get(code_0);
      }
      path_list.add(path);
      hmap3.put(code_0, path_list);
    }
    List paths_list = new ArrayList();
    it = hmap3.keySet().iterator();
    while (it.hasNext()) {
      String code_0 = (String) it.next();
      List path_list = (List) hmap3.get(code_0);
      Paths paths = new Paths(path_list);
      paths_list.add(paths);
    }
    return paths_list;
  }

  public static String dumpMainTypeHierarchyCodes() {
    Vector codes = new Vector();
    Vector w = (Vector) main_type_hierarchy_data.clone();
    Vector v = new Vector();
    for (int i = 0; i < w.size(); i++) {
      String t = (String) w.elementAt(i);
      t = t.trim();
      v.add(t);
    }
    v = new SortUtils().quickSort(v);
    for (int i = 0; i < v.size(); i++) {
      String line = (String) v.elementAt(i);
      int n = line.lastIndexOf("(");
      String code = line.substring(n + 1, line.length() - 1);
      if (!codes.contains(code)) {
        codes.add(code);
      }
    }
    String today = StringUtils.getToday();
    String outputfile = "main_type_code_" + today + ".txt";
    Utils.saveToFile(outputfile, codes);
    return outputfile;
  }

  public void searchMultiplePaths(PrintWriter pw, Vector codes) {
    if (codes == null)
      return;
    multiple_count = 0;
    for (int i = 0; i < codes.size(); i++) {
      String code = (String) codes.elementAt(i);
      searchMultiplePaths(pw, code);
    }
  }

  public void searchMultiplePaths(PrintWriter pw, String code) {
    if (!isMultiple(code))
      return;
    multiple_count++;
    getCTRPResponse(pw, code);
  }

  public boolean isMultiple(String code) {
    Vector v = findMainMenuAncestors(code);
    if (v.size() < 2)
      return false;
    Vector w = new Vector();
    if (isSubtype(code) || isDiseaseStage(code) || isDiseaseGrade(code)) {
      if (v != null && v.size() > 0) {
        for (int i = 0; i < v.size(); i++) {
          String line = (String) v.elementAt(i);
          Vector u = StringUtils.parseData(line, '|');
          String ancestor_label = (String) u.elementAt(0);
          String ancestor_code = (String) u.elementAt(1);
          Paths paths = find_path_to_main_type_roots(ancestor_code);
          List list = paths.getPaths();
          for (int k = 0; k < list.size(); k++) {
            Path path = (Path) list.get(k);
            List concepts = path.getConcepts();
            Concept c = (Concept) concepts.get(concepts.size() - 1);
            if (w.size() == 0) {
              w.add(c.getCode());
            } else {
              if (!w.contains(c.getCode())) {
                return true;
              } else {
                w.add(c.getCode());
              }
            }
          }
        }
      }
    }
    return false;
  }

  public HierarchyHelper getHierarchyHelper() {
    return this.hh;
  }

  public HierarchyHelper getMTHHierarchyHelper() {
    return this.mth_hh;
  }

  public HashMap getLevelMap() {
    return this.levelMap;
  }

  public void printTree(Vector v) {
    HierarchyHelper hh = new HierarchyHelper(v);
    hh.printTree();
  }

  public void traverseUp(String rootCode) {
    traverseUp(rootCode, 0);
  }

  public String getIndentation(int level) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < level; i++) {
      buf.append("\t");
    }
    return buf.toString();
  }

  public void traverseUp(String code, int level) {
    String label = hh.getLabel(code);
    if (main_type_set.contains(code)) {
      System.out.println(getIndentation(level) + label + " (" + code + ") (*)");
    } else {
      System.out.println(getIndentation(level) + label + " (" + code + ")");
    }
    Vector superconcepts = hh.getSuperclassCodes(code);
    if (superconcepts == null)
      return;
    for (int i = 0; i < superconcepts.size(); i++) {
      String superconceptcode = (String) superconcepts.elementAt(i);
      traverseUp(superconceptcode, level + 1);
    }
  }

  public static void main(String[] args) {
    /*
     * String serviceUrl = args[0]; String named_graph = args[1];
     */

    String serviceUrl = args[0];
    System.out.println(serviceUrl);
    MetadataUtils test = new MetadataUtils(serviceUrl);
    String codingScheme = "NCI_Thesaurus";
    long ms = System.currentTimeMillis();
    String version = test.getLatestVersion(codingScheme);
    System.out.println(codingScheme);
    System.out.println(version);
    String named_graph = test.getNamedGraph(codingScheme);
    System.out.println(named_graph);

    MainTypeHierarchyData mthd = new MainTypeHierarchyData(serviceUrl, named_graph);
    String ncit_version = mthd.getVersion();
    System.out.println("version " + ncit_version);
    Vector broad_category_vec = mthd.get_broad_category_vec();
    // StringUtils.dumpVector("broad_category_vec", broad_category_vec);
    HashSet main_type_set = mthd.get_main_type_set();
    Vector<String> parent_child_vec = mthd.get_parent_child_vec(named_graph);
    Vector v1 = mthd.getDiseaseIsStageSourceCodes(named_graph);
    Vector v2 = mthd.getDiseaseIsGradeSourceCodes(named_graph);
    HashMap stageConceptHashMap = mthd.generateStageConceptHashMap(v1);
    HashMap gradeConceptHashMap = mthd.generateGradeConceptHashMap(v2);

    HashSet ctrp_biomarker_set = mthd.get_ctrp_biomarker_set();
    HashSet ctrp_reference_gene_set = mthd.get_ctrp_reference_gene_set();

    MainTypeHierarchy mth =
        new MainTypeHierarchy(ncit_version, parent_child_vec, main_type_set, broad_category_vec,
            stageConceptHashMap, gradeConceptHashMap, ctrp_biomarker_set, ctrp_reference_gene_set);
    Vector mth_vec = mth.generate_main_type_hierarchy();
    Utils.saveToFile("MainTypeHierarchy_" + StringUtils.getToday() + ".txt", mth_vec);

    // AIDS-Related Primary Central Nervous System Lymphoma (C8284)
    String code = "C8284";
    System.out.println("get_ctrp_response: " + code);
    Vector v = mth.findMainMenuAncestors(code);
    StringUtils.dumpVector("findMainMenuAncestors", v);

    String outputfile = mth.DISEASES_AND_DISORDERS_CODE + "_multiple.txt";
    PrintWriter pw = null;
    try {
      pw = new PrintWriter(outputfile, "UTF-8");
      Vector codes = mth.getTransitiveClosure(mth.DISEASES_AND_DISORDERS_CODE);
      mth.searchMultiplePaths(pw, codes);
    } catch (Exception ex) {

    } finally {
      try {
        pw.close();
        System.out.println("Output file " + outputfile + " generated.");
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    System.out.println("Multiple cases: " + mth.multiple_count);
    System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
  }
}
