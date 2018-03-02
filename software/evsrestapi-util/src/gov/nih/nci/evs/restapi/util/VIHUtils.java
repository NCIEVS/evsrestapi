package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;

import java.io.*;
import java.util.*;

public class VIHUtils {
    static String HAS_CHILD = "has_child";
    private String coding_scheme = null;
    private Vector parent_child_vec = null;
    private HashSet _leaf_set = null;
    private String _roots = null;
    private HierarchyHelper hh = null;
	private Vector roots = null;
	private Vector leaves = null;

	HashSet root_set = null;
	HashSet leaf_set = null;


    public void set_roots(String roots) {
        this._roots = roots;
    }

    public VIHUtils() {

	}

    public void set_leaf_set(HashSet hset) {
		this._leaf_set = hset;
	}

    public void setparent_child_vec(Vector v) {
		this.parent_child_vec = v;
	}

    public void setCodingScheme(String coding_scheme) {
		this.coding_scheme = coding_scheme;
	}

    public VIHUtils(Vector parent_child_vec) {
		long ms = System.currentTimeMillis();
        this.parent_child_vec = parent_child_vec;
        this.hh = new HierarchyHelper(parent_child_vec);

		this.roots = hh.getRoots();//.hashSet2Vector(this.root_set);
		this.leaves = hh.getLeaves();//Utils.hashSet2Vector(this.leaf_set);

		System.out.println("Number of roots: " + this.roots.size());
		System.out.println("Number of leaves: " + this.leaves.size());
        System.out.println("Total VIHUtils initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

    public boolean isLeaf(String code) {
		return leaves.contains(code);
	}

    public Vector<TreeItem> getChildNodes(TreeItem parent_ti) {
		if (parent_ti == null) return null;
		String focus_code = parent_ti._code;
		Vector w = getSubclassesByCode(focus_code);
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t);
			String cs_name = coding_scheme;
			String cs_version = null;
			String cs_ns = cs_name;
			String code = (String) u.elementAt(1);
			String name = (String) u.elementAt(0);
			boolean is_expandable = true;
			TreeItem child = new TreeItem(code, name, cs_ns, null);
			child._expandable = !isLeaf(code);
			child._auis = code + "|" + parent_ti._auis;
			v.add(child);
		}
		return v;
	}

	public String getEntityDescriptionByCode(String code) {
		return hh.getLabel(code);
	}

    public TreeItem getRootTreeItem() {
		String assocText = HAS_CHILD;
		Vector v = this.roots;
		TreeItem root = new TreeItem("Root", "<Root>");
		root._auis = "root";
		try {
			for (int i=0; i<v.size(); i++) {
				String code = (String) v.elementAt(i);
				String name = getEntityDescriptionByCode(code);
				TreeItem child = new TreeItem(code, name, null, null);
				child._auis = code;//root._auis + "_" + i;
				child._expandable = true;
				root.addChild(assocText, child);
			}
			List<TreeItem> children = root._assocToChildMap.get(assocText);
			new SortUtils().quickSort(children);
			root._assocToChildMap.put(HAS_CHILD, children);

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return root;
	}


	public static void dumpHashMap(String label, HashMap map) {
		System.out.println("\n" + label);
		if (map == null) return;
		Vector key_vec = new Vector();
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			key_vec.add(key);
		}
		key_vec = new SortUtils().quickSort(key_vec);
        for (int k=0; k<key_vec.size(); k++) {
			String key = (String) key_vec.elementAt(k);
			Object obj = map.get(key);
			if (obj instanceof Vector) {
				Vector v = (Vector) obj;
				for (int j=0; j<v.size(); j++) {
					String value = (String) v.elementAt(j);
					System.out.println(key + " --> " + value);
				}
			} else if (obj instanceof String) {
				String value = (String) obj;
				System.out.println(key + " --> " + value);
			}
		}
	}


    public HashMap getSuperclassTransitiveClosure(String code) {
		return getSuperclassTransitiveClosure(new HashMap(), code);
	}

    public HashMap getSuperclassTransitiveClosure(HashMap map, String code) {
		if (map == null) {
			map = new HashMap();
		}
		Vector v = hh.getSuperclassCodes(code);
		if (v == null || v.size() == 0) return map;
		for (int i=0; i<v.size(); i++) {
			String parent_code = (String) v.elementAt(i);
			Vector w = (Vector) map.get(code);
			if (w == null) {
				w = new Vector();
			}
			if (!w.contains(parent_code)) {
				w.add(parent_code);
			}
			w = new SortUtils().quickSort(w);
			map.put(code, w);
			map = getSuperclassTransitiveClosure(map, parent_code);
		}
		return map;
	}

    public HashMap getInverseHashMap(HashMap map) {
		HashMap inv_map = new HashMap();
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Object obj = map.get(key);
			if (obj instanceof Vector) {
				Vector v = (Vector) map.get(key);
				for (int i=0; i<v.size(); i++) {
					String t = (String) v.elementAt(i);
					Vector w = new Vector();
					if (inv_map.containsKey(t)) {
						w = (Vector) inv_map.get(t);
					}
					w.add(key);
					inv_map.put(t, w);
				}
			} else if (obj instanceof String) {
				String t = (String) map.get(key);
				inv_map.put(t, key);
			}
		}
		return inv_map;
	}

	public TreeItem searchTreeItem(TreeItem ti, String auis) {
        if (ti._auis.compareTo(auis) == 0) {
			return ti;
		}
		for (String assoc : ti._assocToChildMap.keySet()) {
			List<TreeItem> children = ti._assocToChildMap.get(assoc);
			for (int i=0; i<children.size(); i++) {
				TreeItem child = (TreeItem) children.get(i);
				TreeItem ti_next = searchTreeItem(child, auis);
				if (ti_next != null) return ti_next;
			}
		}
		return null;
	}

	public Vector findPathsToRoots(String code) {
	    HashMap hmap = getSuperclassTransitiveClosure(code);
	    Stack st = new Stack();
	    st.push(code);
	    Vector paths = new Vector();
	    while (!st.empty()) {
			String path = (String) st.pop();
			Vector u = StringUtils.parseData(path, '|');
			String next_code = (String) u.elementAt(u.size()-1);
			if (hmap.containsKey(next_code)) {
				Vector w = (Vector) hmap.get(next_code);
				for (int k=0; k<w.size(); k++) {
					String parent_node = (String) w.elementAt(k);
					String new_path = path + "|" + parent_node;
					st.push(new_path);
				}
			} else {
				paths.add(path);
			}
		}
        return paths;
    }

    public String findParentTreePosition(String path, String code) {
		String tree_position = "";
		path = path + "|";
		int n = path.indexOf(code + "|");
		if (n != -1) {
			tree_position = path.substring(n+code.length()+1, path.length());
			if (tree_position.endsWith("|")) {
				tree_position = tree_position.substring(0, tree_position.length()-1);
			}
		}
        return tree_position;
	}

	public TreeItem buildViewInHierarchyTree(String code) {
		TreeItem root_node = getRootTreeItem();
		Vector paths = findPathsToRoots(code);
		for (int k=0; k<paths.size(); k++) {
			String path = (String) paths.elementAt(k);
			Vector nodes = StringUtils.parseData(path);
			for (int i=0; i<nodes.size()-1; i++) {
				int j = nodes.size()-i-2;
				String path_node_id = (String) nodes.elementAt(j);
				String parent_tree_position = findParentTreePosition(path, path_node_id);
				TreeItem parent_node = searchTreeItem(root_node, parent_tree_position);
				if (parent_node != null) {
					String child_id = path_node_id;
					if (child_id.compareTo(code) != 0) {
						String child_name = getEntityDescriptionByCode(child_id);
						TreeItem child = new TreeItem(child_id, child_name, null, null);
						child._auis = child_id + "|" + parent_tree_position;
						parent_node._expandable = true;
						parent_node.addChild(HAS_CHILD, child);
				    } else {
						Vector<TreeItem> children = getChildNodes(parent_node);
                        for (int k2=0; k2<children.size(); k2++) {
							TreeItem child = (TreeItem) children.elementAt(k2);
							parent_node.addChild(HAS_CHILD, child);
							parent_node._expandable = true;
						}
					}
				}
			}
		}
		return root_node;
	}

    public Vector getSuperclassesByCode(String child_code) {
		Vector u = new Vector();
		Vector codes = hh.getSuperclassCodes(child_code);
		if (codes != null) {
			for (int i=0; i<codes.size(); i++) {
				String code = (String) codes.elementAt(i);
				String label = hh.getLabel(code);
				u.add(label + "|" + label);
			}
		}
		return u;
	}

    public Vector getSubclassesByCode(String parent_code) {
		Vector u = new Vector();
		Vector codes = hh.getSubclassCodes(parent_code);
		if (codes != null) {
			for (int i=0; i<codes.size(); i++) {
				String code = (String) codes.elementAt(i);
				String label = hh.getLabel(code);
				u.add(label + "|" + label);
			}
		}
		return u;
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		Vector parent_child_vec = Utils.readFile("parent_child.txt");
		VIHUtils vihUtils = new VIHUtils(parent_child_vec);
        String code = "C12365";
        TreeItem ti = vihUtils.buildViewInHierarchyTree(code);
        TreeItem.printTree(ti, 0, false);
        System.out.println("\n");
        TreeItem.printTree(ti, 0, true);
	}
}
