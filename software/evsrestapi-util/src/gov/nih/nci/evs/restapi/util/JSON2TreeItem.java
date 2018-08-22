package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.json.simple.JSONValue;

import java.io.*;
import java.util.*;

//http://code.google.com/p/json-simple/wiki/DecodingExamples

public class JSON2TreeItem {

	public static final String ONTOLOGY_NODE_CHILD_COUNT = "ontology_node_child_count";

	/** The Constant ONTOLOGY_NODE_ID. */
	public static final String ONTOLOGY_NODE_ID = "ontology_node_id";

	/** The Constant ONTOLOGY_NODE_NS. */
	public static final String ONTOLOGY_NODE_NS = "ontology_node_ns";

	/** The Constant ONTOLOGY_NODE_NAME. */
	public static final String ONTOLOGY_NODE_NAME = "ontology_node_name";

	/** The Constant CHILDREN_NODES. */
	public static final String CHILDREN_NODES = "children_nodes";

	/** The Constant NODES. */
	public static final String NODES = "nodes";

	/** The Constant PAGE. */
	public static final String PAGE = "page";

	/** The MA x_ children. */
	private int MAX_CHILDREN = 5;

	/** The SUBCHILDRE n_ levels. */
	private int SUBCHILDREN_LEVELS = 1;

	/** The MOR e_ childre n_ indicator. */
	private static String MORE_CHILDREN_INDICATOR = "...";
	private static String assocText = "CHD";

	public JSON2TreeItem() {

	}

	public static TreeItem JSONObject2TreeItem(JSONObject jsonObj) {
		JSONParser parser = new JSONParser();
		TreeItem ti = new TreeItem("", "");
		Iterator it = jsonObj.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			Object obj = jsonObj.get(key);
			String value = null;
			if (obj instanceof String) {
				value = obj.toString();
				if (value != null) {
					if (value.startsWith("\"")) {
						value = value.substring(1, value.length());
					}
					if (value.endsWith("\"")) {
						value = value.substring(0, value.length()-1);
					}
				}

			} else if (obj instanceof Double || obj instanceof Float || obj instanceof Number) {
				value = JSONValue.toJSONString(jsonObj.get(key));
			}

			if (key.compareTo(ONTOLOGY_NODE_ID) == 0) {
				ti._code = value;

			} else if (key.compareTo(ONTOLOGY_NODE_NS) == 0) {
				ti._ns = value;

			} else if (key.compareTo(ONTOLOGY_NODE_NAME) == 0) {
				ti._text = value;

			} else if (key.compareTo(ONTOLOGY_NODE_CHILD_COUNT) == 0) {
				int count = Integer.parseInt(value);
				ti._expandable = false;
				if (count > 0) {
					ti._expandable = true;
				}

			} else if (key.compareTo(PAGE) == 0) {

			} else if (key.compareTo(CHILDREN_NODES) == 0) {
				value = JSONValue.toJSONString(jsonObj.get(key));
				if (value.compareTo("[]") != 0) {
					try {
						Object children_node_obj = parser.parse(value);
						JSONArray array=(JSONArray) children_node_obj;

						for (int i=0; i<array.size(); i++) {
							JSONObject object = (JSONObject) JSONValue.parse((array.get(i)).toString());
							TreeItem child_ti = JSONObject2TreeItem(object);

							ti.addChild(assocText, child_ti);
							ti._expandable = true;
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
		return ti;
	}


    public static TreeItem json2TreeItem(String json_string) {
		TreeItem ti = new TreeItem("<Root>", "Root node");
		ti._expandable = false;
		try {
			JSONParser parser=new JSONParser();
			String assocText = "has_child";
			Object obj = parser.parse(json_string);
			JSONArray array=(JSONArray)obj;

			for (int i=0; i<array.size(); i++) {
				JSONObject object = (JSONObject) JSONValue.parse((array.get(i)).toString());
				TreeItem child_ti = JSONObject2TreeItem(object);
				ti.addChild(assocText, child_ti);
				ti._expandable = true;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return ti;
	}

    public static String replace(String s, String replace, String with) {
		String t = s;
		int n = t.indexOf(replace);
		while (n != -1) {
			String s1 = t.substring(0, n);
			String s2 = t.substring(n+replace.length(), t.length());
			t = s1 + with + s2;
			n = t.indexOf(replace);
		}
		return t;
	}

    public static String treeItem2Json(TreeItem ti) {
		StringBuffer buf = new StringBuffer();
		for (String association : ti._assocToChildMap.keySet()) {
			List<TreeItem> children = ti._assocToChildMap.get(association);
			new SortUtils().quickSort(children);
			for (int i=0; i<children.size(); i++) {
				TreeItem childItem = (TreeItem) children.get(i);
				String s = getTreeItemInJson(childItem);
				buf.append(s);
			}
		}
		buf.append("]");
		String t = buf.toString();
		t = replace(t, "}[{", "},{");
		return t;
	}

    public static boolean hasChildren(TreeItem ti) {
		int knt = 0;
		for (String association : ti._assocToChildMap.keySet()) {
			List<TreeItem> children = ti._assocToChildMap.get(association);
			knt = knt + children.size();
		}
		if (knt > 0) return true;
		return false;
	}

    public static String getTreeItemInJson(TreeItem ti) {
        StringBuffer buf = new StringBuffer();
	    String _code = ti._code;
	    String _text = ti._text;
	    String _ns = ti._ns;
		if (hasChildren(ti)) {
            buf.append("[{\"children_nodes\":");
	        for (String association : ti._assocToChildMap.keySet()) {
		        List<TreeItem> children = ti._assocToChildMap.get(association);
		        new SortUtils().quickSort(children);
		        for (int i=0; i<children.size(); i++) {
		            TreeItem childItem = (TreeItem) children.get(i);
		            String s = getTreeItemInJson(childItem);
		            buf.append(s);
	            }
			}
			buf.append("],");
	    }
	    else {
        	buf.append("[{\"children_nodes\":[],");
	    }
        if (_text.compareTo("...") == 0) {
			buf.append("\"page\":1,");
		}
        if (ti._expandable) {
            buf.append("\"ontology_node_child_count\":1,");
        } else {
            buf.append("\"ontology_node_child_count\":0,");
        }
        buf.append("\"ontology_node_id\":\"" + _code + "\",");
        if (_text.compareTo("...") != 0) {
        	buf.append("\"ontology_node_ns\":\"" + _ns + "\",");
	    }
        buf.append("\"ontology_node_name\":\"" + _text + "\"}");
        String retstr = buf.toString();
        return retstr;
    }
}

