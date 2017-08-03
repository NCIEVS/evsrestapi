package gov.nih.nci.evs.api.maintype.util;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;

import gov.nih.nci.evs.api.maintype.bean.TreeItem;
import gov.nih.nci.evs.api.maintype.common.Constants;


/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2016 NGIS. This software was developed in conjunction
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


public class ASCIITreeUtils {

    public ASCIITreeUtils() {

	}

	public int getLevel(String t) {
		int level = 0;
		for (int i=0; i<t.length(); i++) {
			char c = t.charAt(i);
			if (c == '\t') {
				level++;
			} else {
				break;
			}
		}
		return level;
	}

    public String trimLabel(String label) {
		if (label.length() == 0) return label;
		char c = label.charAt(0);
		if (c == ' ' || c == '\t') {
			return trimLabel(label.substring(1, label.length()));
		}
		return label;
	}

	public String extractLabel(String line) {
        //Carcinoma (C2916)
        int n = line.lastIndexOf("(");
        if (n == -1) return line;
        String s = line.substring(0, n-1);
        return trimLabel(s);
	}

	public String extractCode(String line) {
        //Carcinoma (C2916)
        int n = line.lastIndexOf("(");
        if (n == -1) return null;
        String s = line.substring(n+1, line.length()-1);
        return s;
	}

    public int findMaximumLevel(String filename) {
		Vector v = Utils.readFile(filename);
		int maxLevel = 0;
		for (int i=0; i<v.size(); i++) {
			int j=i+1;
			String t = (String) v.elementAt(i);
			int n = getLevel(t);
			String level = new Integer(n).toString();
			if (maxLevel < n) {
				maxLevel = n;
			}
		}
		return maxLevel;
	}

    public int findMaximumLevel(Vector v) {
		int maxLevel = 0;
		for (int i=0; i<v.size(); i++) {
			int j=i+1;
			String t = (String) v.elementAt(i);
			int n = getLevel(t);
			String level = new Integer(n).toString();
			if (maxLevel < n) {
				maxLevel = n;
			}
		}
		return maxLevel;
	}


	public Vector getRoots(TreeItem ti) {
		Vector w = new Vector();
        for (String association : ti._assocToChildMap.keySet()) {
            List<TreeItem> children = ti._assocToChildMap.get(association);
            for (int i=0; i<children.size(); i++) {
				TreeItem childItem = (TreeItem) children.get(i);
                w.add(childItem);
			}
        }
        return w;
	}


	public TreeItem getChild(TreeItem ti, int index) {
        int n = 0;
        for (String association : ti._assocToChildMap.keySet()) {
            List<TreeItem> children = ti._assocToChildMap.get(association);
            for (int i=0; i<children.size(); i++) {
				TreeItem childItem = (TreeItem) children.get(i);
				n++;
				if (n == index) {
					return childItem;
				}
			}
        }
        return null;
	}


    public TreeItem createTreeItem(String filename) {
		int maxLevel = findMaximumLevel(filename);
		Vector v = Utils.readFile(filename);
		return createTreeItem(v, maxLevel);
	}

    public TreeItem createTreeItem(Vector parent_child_vec) {
		HierarchyHelper hh = new HierarchyHelper(parent_child_vec);
		Vector v = hh.exportTree();
		int maxLevel = findMaximumLevel(v);
		return createTreeItem(v, maxLevel);
	}

	public void dumpVector(String label, Vector v) {
		System.out.println("\n" + label);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			System.out.println(t);
		}
	}


    public void saveToFile(Vector v, String outputfile) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
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

    public TreeItem createTreeItem(Vector v, int maxLevel) {
		//saveToFile(v, "echo_tree.txt");
		String assocText = Constants.ASSOCIATION_NAME;
		int root_count = 0;
		TreeItem[] treeItem_array = new TreeItem[maxLevel+1];
		for (int k=0; k<treeItem_array.length; k++) {
			treeItem_array[k] = null;
		}

		TreeItem root = new TreeItem("<Root>", "Root node");
		root._expandable = false;
		int child_count = 0;
		try {
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				int level = getLevel(t);
				String code = extractCode(t);
				String name = extractLabel(t);

    			if (code == null || code.startsWith("R_")) {
					root_count++;
					code = "ROOT_" + root_count;

				}
				if (level == 0) {
					TreeItem item = treeItem_array[0];
					if (item != null) {
						root.addChild(Constants.ASSOCIATION_NAME, item);
						root._expandable = true;
					}
					TreeItem new_item = new TreeItem(code, name);
					new_item._id = "N_" + root_count;
					treeItem_array[0] = new_item;
					child_count = 0;

				} else {
                    TreeItem parent_ti = treeItem_array[level-1];
					TreeItem new_item = new TreeItem(code, name);
					String parent_id = parent_ti._id;
					child_count++;
					new_item._id = parent_id + "_" + child_count;
					parent_ti.addChild(Constants.ASSOCIATION_NAME, new_item);
					parent_ti._expandable = true;
					treeItem_array[level] = new_item;
				}
			}
			TreeItem item = treeItem_array[0];
			root.addChild(Constants.ASSOCIATION_NAME, item);
			root._expandable = true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return root;
	}


	public Vector exportTree(TreeItem root) {
		if (root == null) {
			return null;
		}
		Vector v = new Vector();
		String parent_code = root._code;
		String parent_label = root._text;
		List<TreeItem> children = root._assocToChildMap.get(Constants.ASSOCIATION_NAME);
		if (children != null && children.size() > 0) {
			TreeItem child_ti = null;
			for (int i=0; i<children.size(); i++) {
				try {
					child_ti = (TreeItem) children.get(i);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				String child_code = child_ti._code;
	            String child_label = child_ti._text;
	            String s = parent_label + "|" + parent_code + "|" + child_label + "|" + child_code;
				v.add(s);
				Vector u = exportTree(child_ti);
				v.addAll(u);
			}
		}
		return v;
	}




	public void exportTree(PrintWriter pw, TreeItem root) {
		if (root == null) return;
		String parent_code = root._code;
		String parent_label = root._text;
		List<TreeItem> children = root._assocToChildMap.get(Constants.ASSOCIATION_NAME);
		if (children != null && children.size() > 0) {
			List<TreeItem> new_children = new ArrayList<TreeItem>();
			for (int i=0; i<children.size(); i++) {
				TreeItem child_ti = (TreeItem) children.get(i);
				String child_code = child_ti._code;
				String child_label = child_ti._text;
				pw.println(parent_label + "|" + parent_code + "|" + child_label + "|" + child_code);
				exportTree(pw, child_ti);
			}
		}
	}

	public void exportTree(String outputfile, TreeItem root) {
		long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			exportTree(pw, root);

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


	private static String trim(String t) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<t.length(); i++) {
			char c = t.charAt(i);
			if (c != '\t') {
				buf.append(c);
			}
		}
		String s = buf.toString();
		s = s.trim();
		return s;
	}

	public Vector removeRootNode(Vector v) {
		if (v == null || v.size() == 0) return v;
		String line = (String) v.elementAt(0);
		if (line.compareTo("Root node (<Root>)") != 0) return v;
		Vector w = new Vector();
		for (int i=1; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			String s = t.substring(1, t.length());
			w.add(s);
		}
		return w;
	}


	public String getToday() {
		java.util.Date date = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
		return sdf.format(date);
	}

	public Vector getLeafNodes(String asciifile) {
		gov.nih.nci.evs.api.maintype.bean.TreeItem root = createTreeItem(asciifile);
		Vector parent_child_vec = exportTree(root);
		gov.nih.nci.evs.api.maintype.util.HierarchyHelper hh = new HierarchyHelper(parent_child_vec);
		hh.findRootAndLeafNodes();
		Vector leaves = hh.getLeaves();
		return leaves;
	}

    public Vector get_parent_child_vec(Vector ascii_tree_vec) {
		int maxLevel = findMaximumLevel(ascii_tree_vec);
		TreeItem ti = createTreeItem(ascii_tree_vec, maxLevel);
		Vector parent_child_vec = exportTree(ti);
		parent_child_vec.remove(0);
		return parent_child_vec;
	}


 	public static void main(String[] args) {
		ASCIITreeUtils utils = new ASCIITreeUtils();
		String asciitreefile = args[0];
		TreeItem root = utils.createTreeItem(asciitreefile);
		TreeItem.printTree(root, 0);
	}
}