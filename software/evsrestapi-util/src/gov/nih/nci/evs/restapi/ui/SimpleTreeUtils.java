package gov.nih.nci.evs.restapi.ui;

import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.util.*;
import java.math.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008,2009 NGIT. This software was developed in conjunction
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
 *      "This product includes software developed by NGIT and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIT" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIT
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIT, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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
 * SimpleSearchUtils (uses LexEVSAPI 6.1 SearchExtension)
 *
 * @author kimong
 *
 */


public class SimpleTreeUtils {
    int node_counter = 0;
    static int MAXIMUM = 5;
    static int MINIMUM = 0;
    static String ASSOCIATION = "has_sub";
    static String TAB = "";
    private String URL = null;
    private int tabindex = 0;
    private String basePath = "/sparql/";
    private HashMap checkboxid2NodeIdMap = null;

    private String focusNodeId = null;
    private boolean checkAll = false;

    private Vector selected_nodes = null;
    private Set vocabularyNameSet = null;

    public SimpleTreeUtils(Set vocabularyNameSet) {
        this.checkboxid2NodeIdMap = new HashMap();
        this.vocabularyNameSet = vocabularyNameSet;
    }

    public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

    public void setVocabularyNameSet(Set vocabularyNameSet) {
		this.vocabularyNameSet = vocabularyNameSet;
	}

	public boolean isFormalName(String name) {
		if (vocabularyNameSet != null && vocabularyNameSet.contains(name)) return true;
		return false;
	}

    public void setFocusNodeId(String focusNodeId) {
		this.focusNodeId = focusNodeId;
	}

    public void setCheckAll(boolean checkAll) {
		this.checkAll = checkAll;
	}

	public String getCheckBoxStatus() {
		if (this.checkAll) {
			return "checked";
		}
		return "";
	}

	public String getCheckBoxStatus(String node_id) {
		if (this.selected_nodes == null) {
			return getCheckBoxStatus();
		} else {
			if (this.selected_nodes.contains(node_id)) {
				return "checked";
			}
		}
		return "";
	}


	public void setSelectedNodes(Vector selected_nodes) {
		this.selected_nodes = selected_nodes;
	}


    public void setUrl(String url) {
		this.URL = url;
	}

    private String getIndentation(int level) {
		if (level <= 0) return "";
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<level; i++) {
			buf.append(TAB);
		}
		return buf.toString();
	}


    //////////////////////////////////////////////////////////////////////////////////////////

    private boolean hasSub(TreeItem tree_node) {
		if (getChildrenCount(tree_node) > 0) return true;
		return false;
	}

    private int getChildrenCount(TreeItem tree_node) {
		int knt = 0;
		for (String asso_name : tree_node._assocToChildMap.keySet()) {
			List<TreeItem> cs_vs_children = tree_node._assocToChildMap.get(asso_name);
			knt = knt + cs_vs_children.size();
		}
		return knt;
	}

    private Vector getChildren(TreeItem tree_node) {
		Vector sub_vec = new Vector();
		for (String asso_name : tree_node._assocToChildMap.keySet()) {
			List<TreeItem> cs_vs_children = tree_node._assocToChildMap.get(asso_name);
			for (TreeItem child_item : cs_vs_children) {
				sub_vec.add(child_item);
			}
		}
		return sub_vec;
	}

    public StringBuffer getValueSetTreeStringBuffer(HashMap tree_map) {
		if (tree_map == null) return null;

		TreeItem root = (TreeItem) tree_map.get("<Root>");
		int knt = 0;
		for (String asso_name : root._assocToChildMap.keySet()) {
			List<TreeItem> cs_vs_children = root._assocToChildMap.get(asso_name);
			for (TreeItem child_item : cs_vs_children) {
				knt++;
				assignNodeId(null, child_item, knt);
			}
		}


		Vector v = treeHashMap2StringBuffer(tree_map);
		if (v == null) return null;
		StringBuffer buf = new StringBuffer();
		if (v.size() == 0) {
		    return buf;
		}

		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			buf.append(t);
		}
        return buf;
	}


    public Vector treeItem2StringBuffer(TreeItem ti) {
		Vector buf_vec = new Vector();
        printTree(buf_vec, ti);
		return buf_vec;
	}


    public Vector treeHashMap2StringBuffer(HashMap tree_map) {
		TreeItem root = (TreeItem) tree_map.get("<Root>");
		return treeItem2StringBuffer(root);
	}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void printFormHeader(PrintWriter out) {
      //out.println("	    <font face=\"verdana\" size=2px>");
      out.println("	    <font face=\"verdana\" size=1.7px>");
      out.println("	    <form>");
    }

	public void printFormHeader(Vector v) {
      v.add("	    <font face=\"verdana\" size=2px>");
      //v.add("	    <form>");
    }

	public void printPageHeader(PrintWriter out) {
      out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
      out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
      out.println("	<head>");
      out.println("		<meta name=\"description\" content=\"A Tree from HTML Lists\">");
      out.println("		<meta http-equiv=\"pragma\" content=\"nocache\">");
      out.println("		<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
      out.println("		<title>Value Set Hierarchy</title>");

      out.println("	</head>");
      out.println("	<body>");

	  out.println("	<script type=\"text/javascript\" src=\"" + basePath + "js/event_simulate.js\"></script>");
	  out.println("	<script type=\"text/javascript\" src=\"" + basePath + "js/value_set_tree_navigation.js\"></script>");

      out.println("	    <font face=\"verdana\" size=2px>");
      out.println("	    <form>");
  }


//Expand all Collapse all Check all Uncheck all
  public void printSelectAllOrNoneLinks(PrintWriter out) {
      out.println("");
      out.println("<div id=\"expandcontractdiv\">");
      out.println("<a href=\"#\" onclick=\"expand_all();\" " + getTabIndex() + " >Expand all</a>");
      out.println("&nbsp;");
      out.println("<a href=\"#\" onclick=\"collapse_all();\" " + getTabIndex() + ">Collapse all</a>");
      out.println("&nbsp;");
      out.println("<a href=\"#\" onclick=\"select_all();\" " + getTabIndex() + ">Check all</a>");
      out.println("&nbsp;");
      out.println("<a href=\"#\" onclick=\"select_none();\" " + getTabIndex() + ">Uncheck all</a>");
      out.println("</div>");
      out.println("");
  }

  public String getTabIndex() {
	  tabindex++;
	  return "tabindex=\"" + tabindex + "\"";
  }

  public static void printFormFooter(PrintWriter out) {
	  out.println("	    </form>");
	  out.println("	    </font>");
  }


  public static void printFormFooter(Vector v) {
	  v.add("	    </form>");
	  v.add("	    </font>");
  }



  public static void printPageFooter(PrintWriter out) {
	  out.println("	    </form>");
	  out.println("	    </font>");
      out.println("	</body>");
      out.println("</html>");
  }

  public HashMap getCheckboxid2NodeIdMap() {
	  return checkboxid2NodeIdMap;
  }

  public void printNode(PrintWriter pw, TreeItem ti, int level) {
	  boolean expandAll = true;
	  printNode(pw, ti, level, expandAll);
 }


  public void printNode(PrintWriter pw, TreeItem ti, int level, boolean expandAll) {
	    if (ti == null) return;
	    String display_text = ti._text;
		String indentation = getIndentation(level);
		pw.println(indentation + "<li>");
		String checkbox_id = ti._id;
		if (checkbox_id == null) {
			checkbox_id = ti._code;
		}
		checkboxid2NodeIdMap.put(checkbox_id, ti._code);
		String img_id = "IMG_" + checkbox_id;
		if (ti._expandable) {
			String div_id = "DIV_" + checkbox_id;
 			pw.println("<img src=\"" + basePath + "images/minus.gif\" id=\"" + img_id + "\" alt=\"show_hide\" onclick=\"show_hide('" + div_id + "');\" "
 			    + getTabIndex()
 			    + ">"
 			    + "<input type=\"checkbox\" id=\"" + checkbox_id + "\" name=\"" + ti._code + "\"  onclick=\"updateCheckbox('" + checkbox_id + "'); return false; \""
 			    + " " + getTabIndex()
 			    + " " + getCheckBoxStatus(ti._code)
 			    + ">"
 			    + getHyperLink(ti));

            if (expandAll) {
				pw.println(indentation + "<div id=\"" +  div_id   + "\">");
				pw.println(indentation + "<ul>");

				for (String asso_name : ti._assocToChildMap.keySet()) {
					List<TreeItem> cs_vs_children = ti._assocToChildMap.get(asso_name);
					for (TreeItem child_item : cs_vs_children) {
						printNode(pw, child_item, level+1, expandAll);
						pw.flush();
					}
				}
				pw.println(indentation + "</ul>");
				pw.println(indentation + "</div>");
		    }

		    pw.flush();
	    } else {
			pw.println("<img src=\"" + basePath + "images/dot.gif\" id=\"" + img_id + "\" alt=\"show_hide\" "
			+ ">"
			+ "<input type=\"checkbox\" id=\"" + checkbox_id + "\" name=\"" + ti._code + "\"  onclick=\"updateCheckbox('" + checkbox_id + "'); return false; \""
			+ " " + getTabIndex()
			+ " " + getCheckBoxStatus(ti._code)
			+ ">"
			+ getHyperLink(ti));
		}
		pw.println(indentation + "</li>");
		pw.flush();
	}

  public void printNode(Vector v, TreeItem ti, int level) {
	  boolean expandAll = true;
	  printNode(v, ti, level, expandAll);
  }

  public void printNode(Vector v, TreeItem ti, int level, boolean expandAll) {
	    String display_text = ti._text;
		String indentation = getIndentation(level);
		v.add(indentation + "<li>");
		String checkbox_id = ti._id;
		if (checkbox_id == null) {
			checkbox_id = ti._code;
		}
		checkboxid2NodeIdMap.put(checkbox_id, ti._code);

		String img_id = "IMG_" + checkbox_id;

		if (ti._expandable) {
			String div_id = "DIV_" + checkbox_id;
 			v.add("<img src=\"" + basePath + "images/minus.gif\" id=\"" + img_id + "\" alt=\"show_hide\" onclick=\"show_hide('" + div_id + "');\" "
 			    + getTabIndex()
 			    + ">"
 			    + "<input type=\"checkbox\" id=\"" + checkbox_id + "\" name=\"" + ti._code + "\"  onclick=\"updateCheckbox('" + checkbox_id + "'); return false; \""
 			    + " " + getTabIndex()
 			    + ">"
 			    + getHyperLink(ti));

            if (expandAll) {
				v.add(indentation + "<div id=\"" +  div_id   + "\">");
				v.add(indentation + "<ul>");

				for (String asso_name : ti._assocToChildMap.keySet()) {
					List<TreeItem> cs_vs_children = ti._assocToChildMap.get(asso_name);
					for (TreeItem child_item : cs_vs_children) {
						printNode(v, child_item, level+1);
						//pw.flush();
					}
				}
				v.add(indentation + "</ul>");
				v.add(indentation + "</div>");
		    }
		    //pw.flush();
	    } else {
			v.add("<img src=\"" + basePath + "images/dot.gif\" id=\"" + img_id + "\" alt=\"show_hide\" "
			+ ">"
			+ "<input type=\"checkbox\" id=\"" + checkbox_id + "\" name=\"" + ti._code + "\"  onclick=\"updateCheckbox('" + checkbox_id + "'); return false; \""
			+ " " + getTabIndex()
			+ ">"
			+ getHyperLink(ti));
		}
		v.add(indentation + "</li>");
	}


    // Make root node clickable:
	private String getHyperLink(TreeItem ti) {
		//if (ti._code.startsWith("TVS_")) {
		//	return ti._text;
		//}

		//if (!ti._code.startsWith("http:")) {
		//	return ti._text;
		//}

		//if (DataUtils.getFormalName(ti._code) != null || DataUtils.getFormalName(ti._text) != null) {
		if (isFormalName(ti._code) || isFormalName(ti._text)) {
			return ti._text;
		}

		if (focusNodeId != null && ti._code.compareTo(focusNodeId) == 0) {
			return ti._text;
		}

	    return "<a href=\"#\" onclick=\"onValueSetNodeClicked('" + ti._code + "');return false;\" " + getTabIndex() + ">" + ti._text + "</a>";
    }


// Collapse nodes at initialization.
    public void printTree(PrintWriter pw, HashMap tree_map) {
		boolean expandAll = true;
		printTree(pw, tree_map, expandAll);
	}


    public void printTree(PrintWriter pw, HashMap tree_map, boolean expandAll) {
		if (tree_map == null) return;
		//checkboxid2NodeIdMap = new HashMap();

		TreeItem root = (TreeItem) tree_map.get("<Root>");
		if (root == null) {
			System.out.println("ERROR in printTree -- root is null.");
			return;
		}

		if (root._assocToChildMap == null) {
			System.out.println("ERROR in printTree -- root._assocToChildMap is null.");
			return;
		}

		int knt = 0;
		for (String asso_name : root._assocToChildMap.keySet()) {
			List<TreeItem> cs_vs_children = root._assocToChildMap.get(asso_name);
			for (TreeItem child_item : cs_vs_children) {
				if (child_item != null) {
				    knt++;
					assignNodeId(null, child_item, knt);
				}
			}
		}
		printTree(pw, root, expandAll);
	}

    public void printTree(Vector v, HashMap tree_map) {
         boolean expandAll = true;
         printTree(v, tree_map, expandAll);
	}

    public void printTree(Vector v, HashMap tree_map, boolean expandAll) {
		if (tree_map == null) return;
		TreeItem root = (TreeItem) tree_map.get("<Root>");
		int knt = 0;
		for (String asso_name : root._assocToChildMap.keySet()) {
			List<TreeItem> cs_vs_children = root._assocToChildMap.get(asso_name);
			for (TreeItem child_item : cs_vs_children) {
				knt++;
				assignNodeId(null, child_item, knt);
			}
		}
		v = new Vector();
		printTree(v, root, expandAll);
	}

	public void printTree(PrintWriter pw, TreeItem root) {
		boolean expandAll = true;
		printTree(pw, root, true);
	}

    public void printTree(PrintWriter pw, TreeItem root, boolean expandAll) {
		if (root == null) return;
		pw.println("<ul>");
		for (String asso_name : root._assocToChildMap.keySet()) {
			List<TreeItem> cs_vs_children = root._assocToChildMap.get(asso_name);
			for (TreeItem child_item : cs_vs_children) {
				printNode(pw, child_item, 0, expandAll);
			}
		}
        pw.println("</ul>");
	}


    public void printTree(TreeItem root) {
        boolean expandAll = true;
        printTree(root, expandAll);
	}

    public void printTree(TreeItem root, boolean expandAll) {
		PrintWriter pw = new PrintWriter(System.out);
        printTree(pw, root, expandAll);
	}

    public void printTree(Vector v, TreeItem root) {
        boolean expandAll = true;
        printTree(v, root, expandAll);
	}

    public void printTree(Vector v, TreeItem root, boolean expandAll) {
		if (root == null) return;
		v.add("<ul>");
		for (String asso_name : root._assocToChildMap.keySet()) {
			List<TreeItem> cs_vs_children = root._assocToChildMap.get(asso_name);
			for (TreeItem child_item : cs_vs_children) {
				printNode(v, child_item, 0, expandAll);
			}
		}
        v.add("</ul>");
	}


///////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void assignNodeId(String parent_id, TreeItem ti, int index) {
		String id = null;
		if (parent_id == null) {
			id = "N_" + index;
		} else {
			id = parent_id + "_" + index;
		}
		ti.setId(id);

		int lcv = 0;
		for (String asso_name : ti._assocToChildMap.keySet()) {
			List<TreeItem> cs_vs_children = ti._assocToChildMap.get(asso_name);
			for (TreeItem child_item : cs_vs_children) {
				lcv++;
				assignNodeId(id, child_item, lcv);
			}
		}
	}


	public void assignNodeId(TreeItem root) {
		node_counter = 0;
		if (root._text.compareTo("<root>") == 0) {
			int knt = 0;
			for (String asso_name : root._assocToChildMap.keySet()) {
				List<TreeItem> cs_vs_children = root._assocToChildMap.get(asso_name);
				for (TreeItem child_item : cs_vs_children) {
					knt++;
					assignNodeId(null, child_item, knt);
				}
			}
		} else {
			assignNodeId(null, root, 1);
		}
	}

    public void writeTree2HTML(HashMap hmap, String basePath, String htmlfile) {
		setBasePath(basePath);
		tabindex = 0;
        TreeItem root = (TreeItem) hmap.get("<Root>");
		int knt = 0;
		for (String asso_name : root._assocToChildMap.keySet()) {
			List<TreeItem> cs_vs_children = root._assocToChildMap.get(asso_name);
			for (TreeItem child_item : cs_vs_children) {
				knt++;
				assignNodeId(null, child_item, knt);
			}
		}

        try {
			PrintWriter pw = new PrintWriter(htmlfile, "UTF-8");
			printPageHeader(pw);
			printSelectAllOrNoneLinks(pw);
			printTree(pw, root);
			printPageFooter(pw);
			pw.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

    public static String getValueSetTreeKey(String uri, String name) {
		return uri + "$" + name;
	}

    public static HashMap createValueSetTreeKey2TreeItemMap(TreeItem ti) {
		return createValueSetTreeKey2TreeItemMap(ti, new HashMap());
	}

    private static HashMap createValueSetTreeKey2TreeItemMap(TreeItem ti, HashMap map) {
        String key = ti._code + "$" + ti._text;
        map.put(key, ti);
		for (String association : ti._assocToChildMap.keySet()) {
			 List<TreeItem> children = ti._assocToChildMap.get(association);
			 for (TreeItem childItem : children) {
				 map = createValueSetTreeKey2TreeItemMap(childItem, map);
			 }
		}
		return map;
    }
}

