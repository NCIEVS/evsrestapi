package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import gov.nih.nci.evs.api.maintype.bean.TreeItem;
import gov.nih.nci.evs.api.maintype.common.Constants;
import gov.nih.nci.evs.api.maintype.util.ASCIITreeUtils;
import gov.nih.nci.evs.api.maintype.util.FirstInFirstOutQueue;
import gov.nih.nci.evs.api.maintype.util.HierarchyHelper;
import gov.nih.nci.evs.api.maintype.util.ParserUtils;
import gov.nih.nci.evs.api.maintype.util.StringUtils;
import gov.nih.nci.evs.api.model.evs.Paths;

public class MainTypeHierarchyUtils {
	static String DISEASES_AND_DISORDERS_CODE = "C2991";
	static String NEOPLASM_CODE = "C3262";
	
	private HashSet <String> mainTypeSet = null;
	private List <String> mainTypes = null;
	//private List <String> parentchild = null;
	private HashMap<String,String> diseaseStageConcepts = null;
	private HashMap<String,String> diseaseGradeConcepts = null;
	private List <String> categoryList = null;
	private HashSet<String> categoryHash = null;
	private Vector <String> mainTypeHierarchyData = null;
	private HashMap levelMap = null;

	private HierarchyUtils hh = null;
    private HierarchyUtils mth_hh = null;
    private HierarchyUtils mth_hh_without_categories = null;	
    
    private PathFinder pathFinder = null;

	public MainTypeHierarchyUtils() {}
	
	public MainTypeHierarchyUtils(List <String> parentchild, HashSet mainTypeSet, ArrayList <String>categoryList,
	        HashMap <String,String> diseaseStageConcepts, HashMap <String,String> diseaseGradeConcepts) {
		
		//this.parentchild = parentchild;
		this.mainTypeSet = mainTypeSet;
		this.categoryList = categoryList;
		this.diseaseStageConcepts = diseaseStageConcepts;
		this.diseaseGradeConcepts = diseaseGradeConcepts;
		this.hh = new HierarchyUtils(parentchild);	
		this.mainTypes = new ArrayList <String>(mainTypeSet);
		
		mainTypeHierarchyData = generateMainTypeHierarchy(DISEASES_AND_DISORDERS_CODE,mainTypeSet,true);
		
		Vector <String> mthParentChildVec = new ASCIITreeUtils().get_parent_child_vec(mainTypeHierarchyData);
		List <String> mthParentChildList = new ArrayList <String>();
		for (String line: mthParentChildVec) {
			String [] values = line.split("\\|");
			String newLine = values[1] + "\t" + values[0] + "\t" + values[3] + "\t" + values[2];
			mthParentChildList.add(newLine);
		}
		this.mth_hh = new HierarchyUtils(mthParentChildList);
		this.pathFinder = new PathFinder(mth_hh);
		
		
		categoryHash = new HashSet <String>();
		for (String category: categoryList) {
			categoryHash.add(category);
		}
		
		Vector <String> mth_parent_child_vec_v2 = new <String> Vector();
		for (int k=0; k<mthParentChildVec.size(); k++) {
			String t = (String) mthParentChildVec.elementAt(k);
			Vector u = StringUtils.parseData(t, '|');
			String parent_code = (String) u.elementAt(1);
			if (!categoryHash.contains(parent_code)) {
				mth_parent_child_vec_v2.add(t);
			}
		}
		List <String> mthParentChildListV2 = new ArrayList <String>();
		for (String line: mth_parent_child_vec_v2) {
			String [] values = line.split("\\|");
			String newLine = values[1] + "\t" + values[0] + "\t" + values[3] + "\t" + values[2];
			mthParentChildListV2.add(newLine);
		}
		this.mth_hh_without_categories = new HierarchyUtils(mthParentChildListV2);
		levelMap = create_max_level_hashmap(mainTypeHierarchyData);

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
	
	public Vector <String> generateMainTypeHierarchy(String rootCode, HashSet nodeSet, boolean trim) {
		Vector <String> embeddedHierarchy = generateEmbeddedHierarchy(rootCode,nodeSet,trim);
		TreeItem root = trim_tree(embeddedHierarchy);
		Vector v =  new ASCIITreeUtils().exportTree(root);
		HierarchyHelper hh = new HierarchyHelper(v);
		Vector w = hh.exportTree();
		return new ASCIITreeUtils().removeRootNode(w);
	}
	
	public Vector <String> generateEmbeddedHierarchy(String rootCode, HashSet nodeSet, boolean trim) {
		Vector <String> w = new Vector <String>();
        ParserUtils parser = new ParserUtils();
		HashSet <String> visitedNodes = new HashSet <String>();
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
				ArrayList <String> v = hh.getSubclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.get(j);
						queue.add(code + "|" + childCode);
					}
			    }
			} else if (nodeSet.contains(parentCode) && !nodeSet.contains(code)) {
				ArrayList <String> v = hh.getSubclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.get(j);
						queue.add(parentCode + "|" + childCode);
					}
				}
			} else if (!nodeSet.contains(parentCode)) {
				ArrayList <String> v = hh.getSubclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.get(j);
						queue.add(code + "|" + childCode);
					}
				}
			}
		}
        return w;
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
	
	public List<Paths> getMainMenuAncestors(String code) {
		if (!isSubtype(code) && !isDiseaseStage(code) && !isDiseaseGrade(code)) {
			return null;
		}
        String label = hh.getLabel(code);
        if (label == null) return null;
		List list = new ArrayList();
	    Vector v = findMainMenuAncestors(code);
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
	
	public Vector findCode2MainTypesTree(String rootCode) {
		return findCode2MainTypesTree(rootCode, mainTypeSet);
	}
	
	public Paths find_path_to_main_type_roots(String code) {
	        return pathFinder.findPathsToRoots(code, categoryHash);
	}
	
	public Vector findMainMenuAncestors(String rootCode) {
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
				List <String> v = hh.getSuperclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.get(j);
						queue.add(code + "|" + childCode);
					}
			    }
			} else if (nodeSet.contains(parentCode) && !nodeSet.contains(code)) {
				List <String> v = hh.getSuperclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.get(j);
						queue.add(parentCode + "|" + childCode);
					}
				}
			} else if (!nodeSet.contains(parentCode)) {
				List <String> v = hh.getSuperclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.get(j);
						queue.add(code + "|" + childCode);
					}
				}
			}
		}
		if (w.size() == 0) {
			if (visitedNodes.contains(DISEASES_AND_DISORDERS_CODE)) {
				String parentLabel = hh.getLabel(rootCode);
				String childLabel = hh.getLabel(DISEASES_AND_DISORDERS_CODE);
				String record = parentLabel + "|" + rootCode
							  + "|" + childLabel + "|" + DISEASES_AND_DISORDERS_CODE;
				w.add(record);
			}			
		}
        return w;
	}	
	
	
	
    public boolean isMainType(String code) {
		return mainTypeSet.contains(code);
	}

    public boolean isDiseaseGrade(String code) {
		return diseaseGradeConcepts.containsKey(code);
	}

    public boolean isDiseaseStage(String code) {
		return diseaseStageConcepts.containsKey(code);
	}
    
    public boolean isDisease(String code) {
  		Vector v = findMainMenuAncestors(code);
  		if (v == null || v.size() == 0) {
  			return false;
  		}
  		return true;
  	}
    
    public boolean isNotDisease(String code) {
 		Vector v = findMainMenuAncestors(code);
 		if (v == null || v.size() == 0) {
 			return true;
 		}
 		return false;
 	}
    

    /*
    public boolean isSubtype(String code) {
		try {
			if (isDiseaseStage(code)) {
				String label = (String) hh.getLabel(code);
				label = label.toLowerCase();
				if (label.indexOf("stage") == -1) return true;
				return false;
			}
			if (isDiseaseGrade(code)) {
				return false;
			}
			if (isMainType(code)) {
				List <String> sups = mth_hh_without_categories.getSuperclassCodes(code);
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
	*/

    public boolean isSubtype(String code) {
 		try {
 			if (isNotDisease(code)) return false;

 			if (isDiseaseStage(code)) {
 				String label = (String) diseaseStageConcepts.get(code);
 				label = label.toLowerCase();
 				if (label.indexOf("stage") == -1) return true;
 				return false;
 			}
 			/*
 			if (isDiseaseGrade(code)) {
 				return false;
 			}
 			*/
 			if (isMainType(code)) {
 				List <String> sups = mth_hh_without_categories.getSuperclassCodes(code);
 				if (sups != null && sups.size() > 0) {
 					return true;
 				} else {
 					return false;
 				}
 			}

 			if (isDiseaseGrade(code)) {
 				String label = (String) diseaseGradeConcepts.get(code);
 				label = label.toLowerCase();
 				if (label.indexOf("grade") == -1) {
 					return true;
 				}
 				return false;
 			}

 		} catch (Exception ex) {
 			ex.printStackTrace();
 		}
 		return true;
 	}
	
}
	