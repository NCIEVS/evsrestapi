package gov.nih.nci.evs.restapi.util.graph;

import gov.nih.nci.evs.restapi.util.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;


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
 * @author EVS Team
 * @version 1.0
 *
 *          Modification history Initial implementation kim.ong@ngc.com
 *
 */


public class GraphUtils {
    public static final int NODES_ONLY = 1;
    public static final int EDGES_ONLY = 2;
    public static final int NODES_AND_EDGES = 3;

    public static final int NODE_COUNT_THRESHOLD = 25;
    public static final int MIN_EDGE_LENGTH = 200;
    public static final int EDGE_LENGTH = 400;

    public static final String NO_DATA_AVAILABLE = "No data available.";

    public static String generateGraphScript(Vector v) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			buf.append(t);
			if (i < v.size()-1) {
				buf.append("\n");
			}
		}
		return buf.toString();
	}

	public static String generateGraphScript(HashSet nodes, HashMap edge_map, int option) {
		Vector node_label_vec = new Vector();
		Iterator it = nodes.iterator();
		if (it == null) return null;
		while (it.hasNext()) {
			String node_label = (String) it.next();
			node_label_vec.add(node_label);
		}

		if (node_label_vec.size() == 0) {
			return NO_DATA_AVAILABLE;
		}

		node_label_vec = new SortUtils().quickSort(node_label_vec);
		HashMap label2IdMap = new HashMap();
		for (int k=0; k<node_label_vec.size(); k++) {
			String node_label = (String) node_label_vec.elementAt(k);
			int j = k+1;
			String id = "" + j;
			label2IdMap.put(node_label, id);
		}

		StringBuffer buf = new StringBuffer();
        if (option == NODES_ONLY || option == NODES_AND_EDGES) {
			buf.append("var nodes = [").append("\n");
			for (int k=0; k<node_label_vec.size()-1; k++) {
				String node_label = (String) node_label_vec.elementAt(k);
				String id = (String) label2IdMap.get(node_label);
				if (node_label_vec.size() <= NODE_COUNT_THRESHOLD) {
					buf.append("{id: " + id + ", label: '" + node_label + "'},").append("\n");
				} else {
					buf.append("{id: " + id + ", label: '" + node_label + "', shape: 'dot', size: 5},").append("\n");
				}
			}
			String node_label = (String) node_label_vec.elementAt(node_label_vec.size()-1);
			String id = (String) label2IdMap.get(node_label);
			if (node_label_vec.size() <= NODE_COUNT_THRESHOLD) {
				buf.append("{id: " + id + ", label: '" + node_label + "'}").append("\n");
			} else {
				buf.append("{id: " + id + ", label: '" + node_label + "', shape: 'dot', size: 5},").append("\n");
			}

			buf.append("];").append("\n");
	    }

        if (option == EDGES_ONLY || option == NODES_AND_EDGES) {
			buf.append("var edges = [").append("\n");
			int count = 0;
			Iterator it2 = edge_map.keySet().iterator();
			if (it2 == null) return null;
			while (it2.hasNext()) {
				String key = (String) it2.next();
				Vector w = (Vector) edge_map.get(key);
				count = count + w.size();
			}

			int m = 0;
			it2 = edge_map.keySet().iterator();
			while (it2.hasNext()) {
				String key = (String) it2.next();
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(key, "$");
				String source = (String) u.elementAt(0);
				String target = (String) u.elementAt(1);

				String from_id = (String) label2IdMap.get(source);
				String to_id = (String) label2IdMap.get(target);

				Vector w = (Vector) edge_map.get(key);
				if (w == null) return null;
				for (int k=0; k<w.size(); k++) {
					String edge = (String) w.elementAt(k);
					m++;
					if (node_label_vec.size() <= NODE_COUNT_THRESHOLD) {
					    if (m < count) {
						    buf.append("{from: " + from_id + ", to: " + to_id + ", arrows:'to', label: '" + edge + "', length: " + MIN_EDGE_LENGTH + "}, ").append("\n");
					    } else {
						    buf.append("{from: " + from_id + ", to: " + to_id + ", arrows:'to', label: '" + edge + "', length: " + MIN_EDGE_LENGTH + "} ").append("\n");
					    }
					} else {
					    if (m < count) {
						    buf.append("{from: " + from_id + ", to: " + to_id + ", arrows:'to', label: '" + edge + "', length: " + EDGE_LENGTH + "}, ").append("\n");
					    } else {
						    buf.append("{from: " + from_id + ", to: " + to_id + ", arrows:'to', label: '" + edge + "', length: " + EDGE_LENGTH + "} ").append("\n");
					    }
					}
				}
			}
			buf.append("];").append("\n");
		}
		return buf.toString();
	}

	public static String encodeNodeLabel(String node_label) {
        String s = node_label.replace("'", "\\'");
        return s;
	}

	public static Vector generateGraphScriptVector(HashSet nodes, HashMap edge_map, int option) {
		Vector v = new Vector();
		Vector node_label_vec = new Vector();
		Iterator it = nodes.iterator();
		if (it == null) return null;
		while (it.hasNext()) {
			String node_label = (String) it.next();
			node_label_vec.add(node_label);
		}

		if (node_label_vec.size() == 0) {
			return v;
		}

		node_label_vec = new SortUtils().quickSort(node_label_vec);
		HashMap label2IdMap = new HashMap();
		for (int k=0; k<node_label_vec.size(); k++) {
			String node_label = (String) node_label_vec.elementAt(k);
			int j = k+1;
			String id = "" + j;
			label2IdMap.put(node_label, id);
		}

        if (option == NODES_ONLY || option == NODES_AND_EDGES) {
			v.add("var nodes = [");
			for (int k=0; k<node_label_vec.size()-1; k++) {
				String node_label = (String) node_label_vec.elementAt(k);
				String id = (String) label2IdMap.get(node_label);
				v.add("{id: " + id + ", label: '" + node_label + "'},");

			}
			String node_label = (String) node_label_vec.elementAt(node_label_vec.size()-1);
			String id = (String) label2IdMap.get(node_label);
			v.add("{id: " + id + ", label: '" + node_label + "'}");
			v.add("];");
	    }

        if (option == EDGES_ONLY || option == NODES_AND_EDGES) {
			v.add("var edges = [");
			int count = 0;
			Iterator it2 = edge_map.keySet().iterator();
			if (it2 == null) return null;
			while (it2.hasNext()) {
				String key = (String) it2.next();
				Vector w = (Vector) edge_map.get(key);
				count = count + w.size();
			}

			int m = 0;
			it2 = edge_map.keySet().iterator();
			while (it2.hasNext()) {
				String key = (String) it2.next();
				Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(key, "$");
				String source = (String) u.elementAt(0);
				String target = (String) u.elementAt(1);

				String from_id = (String) label2IdMap.get(source);
				String to_id = (String) label2IdMap.get(target);

				Vector w = (Vector) edge_map.get(key);
				if (w == null) return null;
				for (int k=0; k<w.size(); k++) {
					String edge = (String) w.elementAt(k);
					m++;
					if (node_label_vec.size() <= NODE_COUNT_THRESHOLD) {
					    if (m < count) {
						    v.add("{from: " + from_id + ", to: " + to_id + ", arrows:'to', label: '" + edge + "', length: " + MIN_EDGE_LENGTH + "}, ");
					    } else {
						    v.add("{from: " + from_id + ", to: " + to_id + ", arrows:'to', label: '" + edge + "', length: " + MIN_EDGE_LENGTH + "} ");
					    }
					} else {
					    if (m < count) {
						    v.add("{from: " + from_id + ", to: " + to_id + ", arrows:'to', label: '" + edge + "', length: " + EDGE_LENGTH + "}, ");
					    } else {
						    v.add("{from: " + from_id + ", to: " + to_id + ", arrows:'to', label: '" + edge + "', length: " + EDGE_LENGTH + "} ");
					    }
					}
				}
			}
			v.add("];");
		}
		return v;
	}


    public static String generateGraphScript(Vector graphData, int option) {
		if (graphData == null) return null;

		HashSet nodes = new HashSet();
		HashMap edge_map = new HashMap();
        for (int i=0; i<graphData.size(); i++) {
			String t = (String) graphData.elementAt(i);
			Vector w = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t);
			String source = (String) w.elementAt(0);
			String target = (String) w.elementAt(1);

			source = encodeNodeLabel(source);
			target = encodeNodeLabel(target);

			String relationship = (String) w.elementAt(2);
			if (!nodes.contains(source)) {
				nodes.add(source);
			}
			if (!nodes.contains(target)) {
				nodes.add(target);
			}
			String key = source + "$" + target;
			Vector v = (Vector) edge_map.get(key);
			if (v == null) {
				v = new Vector();
			}
			v.add(relationship);
			edge_map.put(key, v);
		}
		return generateGraphScript(nodes, edge_map, option);
    }

    public static Vector generateGraphScriptVector(Vector graphData, int option) {
		if (graphData == null) return null;
		HashSet nodes = new HashSet();
		HashMap edge_map = new HashMap();
        for (int i=0; i<graphData.size(); i++) {
			String t = (String) graphData.elementAt(i);
			Vector w = gov.nih.nci.evs.restapi.util.StringUtils.parseData(t);
			String source = (String) w.elementAt(0);
			String target = (String) w.elementAt(1);
			String relationship = (String) w.elementAt(2);
			if (!nodes.contains(source)) {
				nodes.add(source);
			}
			if (!nodes.contains(target)) {
				nodes.add(target);
			}
			String key = source + "$" + target;
			Vector v = (Vector) edge_map.get(key);
			if (v == null) {
				v = new Vector();
			}
			v.add(relationship);
			edge_map.put(key, v);
		}
		return generateGraphScriptVector(nodes, edge_map, option);
    }

}

