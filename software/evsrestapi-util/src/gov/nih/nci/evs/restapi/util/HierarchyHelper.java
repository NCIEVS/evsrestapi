package gov.nih.nci.evs.restapi.util;


import java.io.*;
import java.util.*;


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


public class HierarchyHelper implements Serializable {
    private static String TYPE_ROOT = "TYPE_ROOT";
    private static String TYPE_LEAF = "TYPE_LEAF";

    private static boolean FORWARD = true;
    private static boolean BACKWARD = false;

    //private HashMap code2NameMap = null;
    private HashMap code2LabelMap = null;

    private Vector _PARENT_CHILDREN = null;
	private String _ROOTS = null;

	private Vector rel_vec = null;
	private Vector label_vec = null;

	private Vector roots = null;
	private Vector leaves = null;

	private HashSet root_set = null;
	private HashSet leaf_set = null;

	private HashMap _parent2childcodesMap = null;
	private HashMap _child2parentcodesMap = null;

	public static int FORMAT_PARENT_CHILD = 1;
	public static int FORMAT_CHILD_PARENT = 2;

	private int format = 0;
    private boolean show_code = true;

    private String INDENT = "";

    public HierarchyHelper() {
	}

    public HierarchyHelper(Vector v) {
        this.rel_vec = v;
        this.format = FORMAT_PARENT_CHILD;
        initialize(v, format);
	}


    public HierarchyHelper(Vector v, int format) {
		long ms = System.currentTimeMillis();
        this.rel_vec = v;
        this.format = format;
        initialize(v, format);
	}

	public void set_indent(String indent) {
		this.INDENT = indent;
	}


	public void set_show_code(boolean bool) {
		this.show_code = bool;
	}

	public HashMap get_parent2childcodesMap() {
		return _parent2childcodesMap;
	}

	public HashMap get_child2parentcodesMap() {
		return _child2parentcodesMap;
	}

	private HashMap getInverseHashMap(HashMap hmap) {
		HashMap inverse_hmap = new HashMap();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector v = (Vector) hmap.get(key);
			for (int k=0; k<v.size(); k++) {
				String value = (String) v.elementAt(k);
				Vector w = new Vector();
				if (inverse_hmap.containsKey(value)) {
					w = (Vector) inverse_hmap.get(value);
				}
				if (!w.contains(key)) {
					w.add(key);
				}
				inverse_hmap.put(value, w);
			}
		}
		return inverse_hmap;
	}

	private void initialize(Vector v, int format) {
		long ms0 = System.currentTimeMillis();
		long ms = System.currentTimeMillis();
        this._parent2childcodesMap = createparent2childcodesMap(v, format);
        this._child2parentcodesMap = getInverseHashMap(_parent2childcodesMap);
        //findRootAndLeafNodes();
		//System.out.println("createparent2childcodesMap run time (ms): " + (System.currentTimeMillis() - ms));
        //ms = System.currentTimeMillis();
        this.code2LabelMap = createCode2LabelMap(v, format);
		//System.out.println("createCode2LabelMap run time (ms): " + (System.currentTimeMillis() - ms));
		//System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms0));

        HashSet set_parent = new HashSet();
        HashSet set_child = new HashSet();
        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');

			String parent_code = null;
			String child_code = null;
			if (format == FORMAT_PARENT_CHILD) {
				if (u.size() == 2) {
					// parent_code|child_code
					parent_code = (String) u.elementAt(0);
					child_code = (String) u.elementAt(1);
				} else {
					// parent_label|parent_code|child_label|child_code
					parent_code = (String) u.elementAt(1);
					child_code = (String) u.elementAt(3);
				}
		    } else if (format == FORMAT_CHILD_PARENT) {
				if (u.size() == 2) {
					// child_code|parent_code
					parent_code = (String) u.elementAt(1);
					child_code = (String) u.elementAt(0);
				} else {
					// // child_label|child_code|parent_label|parent_code
					parent_code = (String) u.elementAt(3);
					child_code = (String) u.elementAt(1);
				}
			}

			set_parent.add(parent_code);
			set_child.add(child_code);
		}
		HashSet set1 = (HashSet) set_parent.clone();
		HashSet set2 = (HashSet) set_child.clone();
		set1.removeAll(set2);
		this.root_set = set1;
		set1 = (HashSet) set_parent.clone();
		set2 = (HashSet) set_child.clone();
		set2.removeAll(set1);
		this.leaf_set = set2;
        this.roots = Utils.hashSet2Vector(this.root_set);
        this.leaves = Utils.hashSet2Vector(this.leaf_set);
	}

	public Vector getRoots() {
		return this.roots;
	}

	public Vector getLeaves() {
		return this.leaves;
	}

	public Vector sortCodesByNames(Vector codes) {
		Vector names = new Vector();
		HashMap hmap = new HashMap();
		for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			String name = (String) code2LabelMap.get(code);
			names.add(name);
			hmap.put(name, code);
		}
		names = new SortUtils().quickSort(names);
		Vector u = new Vector();
		for (int i=0; i<names.size(); i++) {
			String name = (String) names.elementAt(i);
			String code = (String) hmap.get(name);
			u.add(code);
		}
	    return u;
	}

	public void findRootAndLeafNodes() {
        //roots = findRoots(_parent2childcodesMap);
        //leaves = findLeaves(_parent2childcodesMap);
        roots = sortCodesByNames(roots);
        leaves = sortCodesByNames(leaves);
	}

	public void load_parent_child_rel(String filename) {
		this.rel_vec = Utils.readFile(filename);
		System.out.println("parent_child_rel size: " + this.rel_vec.size());
	}

    public void set_rel_vec(Vector rel_vec) {
        this.rel_vec = rel_vec;
	}


    public void set_label_vec(Vector label_vec) {
        this.label_vec = label_vec;
        this.code2LabelMap = createCode2LabelMap(label_vec);
	}

	public HashMap createCode2LabelMap(Vector label_vec) {
		HashMap label_hmap = new HashMap();
		for (int i=0; i<label_vec.size(); i++) {
			String t = (String) label_vec.elementAt(i);
			Vector u = StringUtils.parseData(t);
			String code = (String) u.elementAt(0);
			String name = (String) u.elementAt(1);
			label_hmap.put(code, name);
		}
		return label_hmap;
	}

    public String getLabel(String code) {
	    if (this.code2LabelMap.containsKey(code)) {
			return (String) code2LabelMap.get(code);
		}
		return null;
	}


	public Vector findRoots(HashMap _parent2childcodesMap) {
		Vector parent_codes = new Vector();
		Vector child_codes = new Vector();

		Iterator it = _parent2childcodesMap.keySet().iterator();
		while (it.hasNext()) {
			String parent_code = (String) it.next();
			parent_codes.add(parent_code);
		}

		it = _parent2childcodesMap.keySet().iterator();
		while (it.hasNext()) {
			String parent_code = (String) it.next();
			Vector w = (Vector) _parent2childcodesMap.get(parent_code);
			for (int i=0; i<w.size(); i++) {
				String s = (String) w.elementAt(i);
				child_codes.add(s);
			}
		}
		Vector all_codes = new Vector();
		all_codes.addAll(parent_codes);
		all_codes.addAll(child_codes);
		Vector roots = new Vector();
		for (int i=0; i<all_codes.size(); i++) {
			String s = (String) all_codes.elementAt(i);
			if (parent_codes.contains(s) && !child_codes.contains(s)) {
				roots.add(s);
			}
		}
		return new SortUtils().quickSort(roots);
	}

	public Vector findLeaves(HashMap _parent2childcodesMap) {
		Vector parent_codes = new Vector();
		Vector child_codes = new Vector();

		Iterator it = _parent2childcodesMap.keySet().iterator();
		while (it.hasNext()) {
			String parent_code = (String) it.next();
			parent_codes.add(parent_code);
		}

		it = _parent2childcodesMap.keySet().iterator();
		while (it.hasNext()) {
			String parent_code = (String) it.next();
			Vector w = (Vector) _parent2childcodesMap.get(parent_code);
			for (int i=0; i<w.size(); i++) {
				String s = (String) w.elementAt(i);
				child_codes.add(s);
			}
		}
		Vector all_codes = new Vector();
		all_codes.addAll(parent_codes);
		all_codes.addAll(child_codes);
		Vector leaf_nodes = new Vector();
		for (int i=0; i<all_codes.size(); i++) {
			String s = (String) all_codes.elementAt(i);
			if (!parent_codes.contains(s) && child_codes.contains(s)) {
				leaf_nodes.add(s);
			}
		}
		return new SortUtils().quickSort(leaf_nodes);
	}



	private HashMap createparent2childcodesMap(Vector w) {
		return createparent2childcodesMap(w, FORMAT_PARENT_CHILD);
	}


	private HashMap createparent2childcodesMap(Vector w, int format) {
		HashMap parent2childcodesMap = new HashMap();
        for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t);
			String parent_code = null;
			String child_code = null;
			if (format == FORMAT_PARENT_CHILD) {
				if (u.size() == 2) {
					parent_code = (String) u.elementAt(0);
					child_code = (String) u.elementAt(1);
				} else {
					parent_code = (String) u.elementAt(1);
					child_code = (String) u.elementAt(3);
				}
		    } else {
				if (u.size() == 2) {
					parent_code = (String) u.elementAt(1);
					child_code = (String) u.elementAt(0);
				} else {
					parent_code = (String) u.elementAt(3);
					child_code = (String) u.elementAt(1);
				}
			}

			Vector v = new Vector();
			if (parent2childcodesMap.containsKey(parent_code)) {
				v = (Vector) parent2childcodesMap.get(parent_code);
			}
			if (!v.contains(child_code)) {
				v.add(child_code);
			}
			parent2childcodesMap.put(parent_code, v);
		}
		return parent2childcodesMap;
	}


///////////////////////////
	private HashMap createCode2LabelMap(Vector w, int format) {
		HashMap label_hmap = new HashMap();
        for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t);
			if (u.size() == 2) {
				String code = (String) u.elementAt(0);
				String label = (String) u.elementAt(1);
				label_hmap.put(code, label);

			} else {
				String parent_code = null;
				String child_code = null;
				String parent_label = null;
				String child_label = null;

				if (format == FORMAT_PARENT_CHILD) {
					parent_label = (String) u.elementAt(0);
					parent_code = (String) u.elementAt(1);
					child_label = (String) u.elementAt(2);
					child_code = (String) u.elementAt(3);
				} else {
					child_label = (String) u.elementAt(0);
					child_code = (String) u.elementAt(1);
					parent_label = (String) u.elementAt(2);
					parent_code = (String) u.elementAt(3);
				}
				label_hmap.put(parent_code, parent_label);
				label_hmap.put(child_code, child_label);
		    }
		}
		return label_hmap;
	}

////////////////////////////////////////////////////////////////////////////////////

	public Vector getSubclassCodes(String code) {
		if (!_parent2childcodesMap.containsKey(code)) return null;
		return (Vector) _parent2childcodesMap.get(code);
	}

	public Vector getSuperclassCodes(String code) {
		if (!_child2parentcodesMap.containsKey(code)) return null;
		return (Vector) _child2parentcodesMap.get(code);
	}

	public Vector mergeVector(Vector v, Vector u) {
		for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			if (!v.contains(t)) {
				v.add(t);
			}
		}
		return v;
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
		return getTransitiveClosure(code, true);
	}

  	public Vector getTransitiveClosure(String code, boolean traverseDown) {
		Vector w = new Vector();
		w.add(code);
		Vector v = new Vector();
		if (traverseDown) {
		    v = getSubclassCodes(code);
		} else {
			v = getSuperclassCodes(code);
		}
		if (v == null) return w;
		for (int i=0; i<v.size(); i++) {
			String child_code = (String) v.elementAt(i);
			Vector u = getTransitiveClosure(child_code, traverseDown);
			if (u != null && u.size() > 0) {
				w.addAll(u);
			}
		}
		w = removeDuplicates(w);
		return w;
	}

	public void printTree() {
		if (roots == null) {
			findRootAndLeafNodes();
		}
		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			printTree(root, 0);
		}
	}

	public void printTree(String code, int level) {
		String indent = INDENT;
		for (int i=0; i<level; i++) {
			indent = indent + "\t";
		}
		String label = getLabel(code);
		System.out.println(indent + label + " (" + code + ")");
		Vector child_codes = getSubclassCodes(code);
		if (child_codes != null && child_codes.size() > 0) {
			for (int i=0; i<child_codes.size(); i++) {
				String child_code = (String) child_codes.elementAt(i);
				printTree(child_code, level+1);
			}
		}
	}

	public void printTree(PrintWriter pw) {
		if (roots == null) {
			findRootAndLeafNodes();
		}
		Vector label_vec = new Vector();
		HashMap label2codeMap = new HashMap();
		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			String label = getLabel(root);
			label2codeMap.put(label, root);
			label_vec.add(label);
		}
		label_vec = new SortUtils().quickSort(label_vec);
		for (int i=0; i<label_vec.size(); i++) {
			String label = (String) label_vec.elementAt(i);
			String code = (String) label2codeMap.get(label);
			printTree(pw, code, 0);
		}
	}

	public void printTree(PrintWriter pw, String code, int level) {
		String indent = INDENT;
		for (int i=0; i<level; i++) {
			indent = indent + "\t";
		}
		String label = getLabel(code);
		if (show_code) {
			pw.println(indent + label + " (" + code + ")");
		} else {
			pw.println(indent + label);
		}

		Vector child_codes = getSubclassCodes(code);
        if (child_codes != null && child_codes.size() > 0) {
			Vector label_vec = new Vector();
			HashMap label2codeMap = new HashMap();
			for (int i=0; i<child_codes.size(); i++) {
				String root = (String) child_codes.elementAt(i);
				label = getLabel(root);
				label2codeMap.put(label, root);
				label_vec.add(label);
			}
			label_vec = new SortUtils().quickSort(label_vec);

			for (int i=0; i<label_vec.size(); i++) {
				label = (String) label_vec.elementAt(i);
				String child_code = (String) label2codeMap.get(label);
				printTree(pw, child_code, level+1);
			}
		}
	}

//////////////////////////////////////////////////////////////////////////////////////////

	public Vector exportTree() {
		return exportTree(new Vector());
	}

	public Vector exportTree(Vector v) {
		if (roots == null) {
			findRootAndLeafNodes();
		}
		Vector label_vec = new Vector();
		HashMap label2codeMap = new HashMap();
		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			String label = getLabel(root);
			label2codeMap.put(label, root);
			label_vec.add(label);
		}
		label_vec = new SortUtils().quickSort(label_vec);
		for (int i=0; i<label_vec.size(); i++) {
			String label = (String) label_vec.elementAt(i);
			String code = (String) label2codeMap.get(label);
			v = exportTree(v, code, 0);
			//v.addAll(w);
		}
		return v;
	}

	public Vector exportTree(Vector v, String code, int level) {
		String indent = INDENT;
		for (int i=0; i<level; i++) {
			indent = indent + "\t";
		}
		String label = getLabel(code);
		if (show_code) {
			v.add(indent + label + " (" + code + ")");
		} else {
			v.add(indent + label);
		}

		Vector child_codes = getSubclassCodes(code);
        if (child_codes != null && child_codes.size() > 0) {
			Vector label_vec = new Vector();
			HashMap label2codeMap = new HashMap();
			for (int i=0; i<child_codes.size(); i++) {
				String root = (String) child_codes.elementAt(i);
				label = getLabel(root);
				label2codeMap.put(label, root);
				label_vec.add(label);
			}
			label_vec = new SortUtils().quickSort(label_vec);

			for (int i=0; i<label_vec.size(); i++) {
				label = (String) label_vec.elementAt(i);
				String child_code = (String) label2codeMap.get(label);
				v = exportTree(v, child_code, level+1);
				//v.addAll(w);
			}
		}
		return v;
	}
//////////////////////////////////////////////////////////////////////////////////////////



	public static int TRAVERSE_UP = 1;
	public static int TRAVERSE_DOWN = 0;

	public void traverse(PrintWriter pw, String code) {
        traverse(pw, code, TRAVERSE_UP);
	}

	public void traverse(PrintWriter pw, String code, int direction) {
		Stack stack = new Stack();
		stack.push("0|"+code);
        while (!stack.isEmpty()) {
			String s = (String) stack.pop();
			Vector u = new Utils().parseData(s, '|');
			String level_str = (String) u.elementAt(0);
			int level = Integer.parseInt(level_str);
			String curr_code = (String) u.elementAt(1);

			String label = getLabel(curr_code);
			String indent = "";
			for (int i=0; i<level; i++) {
				indent = indent + "\t";
			}
			pw.println(indent + label + " (" + curr_code + ")");

            Vector w = null;
            if (direction == TRAVERSE_UP) {
            	w = getSuperclassCodes(curr_code);
		    } else {
				w = getSubclassCodes(curr_code);
			}

            if (w != null) {
				level = level + 1;
				for (int j=0; j<w.size(); j++) {
					String next_code = (String) w.elementAt(j);
					String t = "" + level + "|" + next_code;
					stack.push(t);
				}
		    }
		}
	}

	public void addRoot(String code, String label) {
		if (roots == null) {
			findRootAndLeafNodes();
		}
		if (roots.contains(code)) return;
		roots.add(code);
		code2LabelMap.put(code, label);
	}

	public HashMap getCode2LabelMap() {
		return this.code2LabelMap;
	}

	public Vector dumpTallies(HashMap hmap) {
		Vector v = new Vector();
		v.add("Root|Size");
		int count = 0;
		Vector keys = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		keys = new SortUtils().quickSort(keys);
		it = hmap.keySet().iterator();
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			Integer int_obj = (Integer) hmap.get(key);
			v.add(key + "|" + Integer.valueOf(int_obj));
			count = count + Integer.valueOf(int_obj);
		}
		v.add("Total|" + count);
		return v;
	}


   	public HashMap getBranchSizes() {
		HashMap hmap = new HashMap();
		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			String label = getLabel(root);
			String key = label + " (" + root + ")";
			Vector v = getTransitiveClosure(root, true);
			hmap.put(key, new Integer(v.size()));
		}

		return hmap;
	}

    public int get_transitive_closure(String code) {
		int count = 0;
		Vector v = null;
		Stack stack = new Stack();
		stack.push(code);
		while (!stack.isEmpty()) {
			String next_code = (String) stack.pop();
			count++;
			v = getSubclassCodes(next_code);
			if (v != null) {
				for (int i=0; i<v.size(); i++) {
					String child_code = (String) v.elementAt(i);
					stack.push(child_code);
				}
			}
		}
		return count;
	}

    public int get_transitive_closure_v2(String code) {
		int count = 0;
		Vector v = null;
		Stack stack = new Stack();
		stack.push(code);
		HashSet hset = new HashSet();

		while (!stack.isEmpty()) {
			String next_code = (String) stack.pop();
			if (!hset.contains(next_code)) {
				hset.add(next_code);
				count++;
			}
			v = getSubclassCodes(next_code);
			if (v != null) {
				for (int i=0; i<v.size(); i++) {
					String child_code = (String) v.elementAt(i);
					stack.push(child_code);
				}
			}
		}
		hset.clear();
		return count;
	}

    public Vector get_transitive_closure_v3(String code) {
		Vector w = new Vector();
		Vector v = null;
		Stack stack = new Stack();
		stack.push(code);
		w.add(code);
		HashSet hset = new HashSet();
		while (!stack.isEmpty()) {
			String next_code = (String) stack.pop();
			if (!hset.contains(next_code)) {
				hset.add(next_code);
				w.add(next_code);
			}
			v = getSubclassCodes(next_code);
			if (v != null) {
				for (int i=0; i<v.size(); i++) {
					String child_code = (String) v.elementAt(i);
					stack.push(child_code);
				}
			}
		}
		hset.clear();
		return w;
	}

    public void printPath2Roots(PrintWriter pw, String line) {
        Vector v = StringUtils.parseData(line, '|');
        String indent = "";
		for (int i=0; i<v.size(); i++) {
			String code = (String) v.elementAt(i);
			String label = getLabel(code);
			if (pw != null) {
				pw.println(indent + label + " (" + code + ")");
			} else {
				System.out.println(indent + label + " (" + code + ")");
			}
			indent = indent + "\t";
		}
	}

	public void path2Roots(PrintWriter pw, String code) {
		Stack stack = new Stack();
		stack.push(code);
		while (!stack.isEmpty()) {
            String line = (String) stack.pop();
            Vector u = StringUtils.parseData(line, '|');
            String next_code = (String) u.elementAt(u.size()-1);
            Vector v = getSuperclassCodes(next_code);
            if (v != null) {
                for (int i=0; i<v.size(); i++) {
					String sup = (String) v.elementAt(i);
					String nextLine = line + "|" + sup;
					stack.push(nextLine);
				}
			} else {
				printPath2Roots(pw, line);
			}
		}
	}

	public void path2Roots(String code) {
		path2Roots(null, code);
	}

    public static void main(String[] args) {
		Vector v = Utils.readFile("tvs_rel.txt");
		HierarchyHelper test = new HierarchyHelper(v, 1);
		Vector roots = test.getRoots();
		StringUtils.dumpVector("roots", roots);
		Vector leaves = test.getLeaves();
		StringUtils.dumpVector("leaves", leaves);

		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			Vector w = test.getTransitiveClosure(root);
			StringUtils.dumpVector("\n" + root, w);
		}
		test.printTree();
	}
}
