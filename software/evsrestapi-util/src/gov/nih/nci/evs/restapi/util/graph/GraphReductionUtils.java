package gov.nih.nci.evs.restapi.util.graph;

import gov.nih.nci.evs.restapi.util.*;
import java.io.*;
import java.util.*;


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


public class GraphReductionUtils {
	static String POP_UP_MSG = "Click to view node content.";
    Vector data_vec = null;
    HashMap sourceEdge2TargetsMap = null;
    HashMap targetEdge2SourcesMap = null;
    public int MINIMUM_REDUCED_GRAPH_SIZE = 25;
    private int MINIMUM_GRAPH_NODE_ID = 1000;

    int group_node_id = 0;

    static boolean FORWARD = true;

    public GraphReductionUtils() {

	}

	public void initialize_group_node_id(int n) {
		MINIMUM_GRAPH_NODE_ID = n;
	}

	private int getGroupNodeID() {
		MINIMUM_GRAPH_NODE_ID++;
		return MINIMUM_GRAPH_NODE_ID;
	}

    public GraphReductionUtils(Vector data_vec) {
		this.data_vec = data_vec;
		sourceEdge2TargetsMap = createSourceEdge2TargetsMap(data_vec);
		targetEdge2SourcesMap = getInverseHashMap(sourceEdge2TargetsMap);
	}


    public HashMap getInverseHashMap(HashMap hmap) {
		HashMap inv_hmap = new HashMap();
        Iterator it = hmap.keySet().iterator();
        if (it == null) return null;
        while (it.hasNext()) {
			String key = (String) it.next();
			Vector node_edge = StringUtils.parseData(key);
			String node = (String) node_edge.elementAt(0);
			String edge = (String) node_edge.elementAt(1);
			Vector w = (Vector) hmap.get(key);
			if (w == null) return null;
			for (int i=0; i<w.size(); i++) {
				String value = (String) w.elementAt(i);
				String value_edge = value + "|" + edge;
				Vector u = new Vector();
				if (inv_hmap.containsKey(value_edge)) {
					u = (Vector) inv_hmap.get(value_edge);
				}
				if (!u.contains(node)) {
					u.add(node);
				}
				inv_hmap.put(value_edge, u);
			}
		}
		return inv_hmap;
	}



    public HashMap getSourceEdge2TargetsMap() {
		return this.sourceEdge2TargetsMap;
	}

    public HashMap getTargetEdge2SourcesMap() {
		return this.targetEdge2SourcesMap;
	}

    public Vector insert_group_node(Vector v, String nodeId, String nodeLabel) {
        Vector w = new Vector();
        String new_node =
        "{id: " + nodeId + ", font: {size:12, color:'red', face:'sans', background:'white'}, label: '" + nodeLabel + "', shape: 'dot', size:8, title: '" + POP_UP_MSG + "', color: {background:'cyan', border:'blue',highlight:{background:'red',border:'blue'},hover:{background:'white',border:'red'}}},";
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t == null) return null;
			if (t.compareTo("var nodes = [") == 0) {
				w.add(t);
				w.add(new_node);
			} else {
				w.add(t);
			}
		}
		return w;
	}

    public Vector insert_edge_to_group_node(Vector v, String sourceNodeId, String groupNodeId, String edgeLabel) {
		return insert_edge_to_group_node(v, sourceNodeId, groupNodeId, edgeLabel, true);
	}

    public Vector insert_edge_to_group_node(Vector v, String sourceNodeId, String groupNodeId, String edgeLabel, boolean forward) {
        Vector w = new Vector();
        String new_edge = "";

        if (forward) {
			new_edge =
			"{from: " + sourceNodeId + ", to: " + groupNodeId + ", arrows:'to', width: 1, label:'" + edgeLabel + "'},";
	    } else {
			new_edge =
			"{from: " + groupNodeId + ", to: " + sourceNodeId + ", arrows:'to', width: 1, label:'" + edgeLabel + "'},";
		}

        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t == null) return null;
			if (t.compareTo("var edges = [") == 0) {
				w.add(t);
				w.add(new_edge);
			} else {
				w.add(t);
			}
		}
		return w;
	}

    public int getNodeCount(Vector v) {
		int count = 0;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
            if (isNode(line)) {
				count++;
			}
		}
		return count;
	}

    public Vector removeNodes(Vector v, Vector nodes_to_remove) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
            if (isNode(line)) {
				String nodeId = getNodeId(line);
				if (!nodes_to_remove.contains(nodeId)) {
					w.add(line);
				}
			} else {
				w.add(line);
			}
		}
        return w;
	}

    public Vector removeEdges(Vector v, Vector nodes_to_remove) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
            if (isEdge(line)) {
				String source = getSourceId(line);
				String target = getTargetId(line);
				//String edge = getEdgeLabel(line);
				if (!(nodes_to_remove.contains(source) || nodes_to_remove.contains(target))) {
					w.add(line);
				}
			} else {
				w.add(line);
			}
		}
        return w;
	}

    public Vector insert_edge_from_group_node(Vector v, String targetNodeId, String groupNodeId, String edgeLabel) {
        Vector w = new Vector();
        String new_edge =
        "{from: " + groupNodeId + ", to: " + targetNodeId + ", arrows:'to', width: 1, label:'" + edgeLabel + "'},";
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			if (t == null) return null;
			if (t.compareTo("var edges = [") == 0) {
				w.add(t);
				w.add(new_edge);
			} else {
				w.add(t);
			}
		}
		return w;
	}

	public String getHighestFreqSourceEdge(HashMap hmap) {
		int max = -1;
		String max_key = null;
        Iterator it = hmap.keySet().iterator();
        if (it == null) return null;
        while (it.hasNext()) {
			String key = (String) it.next();
			Vector w = (Vector) hmap.get(key);
			if (w == null) return null;
			if (w.size() > max) {
				max = w.size();
				max_key = key;
			}
		}
		return max_key;
    }

   	public String getHighestFreqTargetEdge(HashMap targetEdge2SourcesMap) {
		return getHighestFreqSourceEdge(targetEdge2SourcesMap);
	}


	public int getHighestFreqSourceEdgeCount(HashMap sourceEdge2TargetsMap) {
		int max = -1;
		String max_key = null;
        Iterator it = sourceEdge2TargetsMap.keySet().iterator();
        if (it == null) return -1;
        while (it.hasNext()) {
			String key = (String) it.next();
			Vector w = (Vector) sourceEdge2TargetsMap.get(key);
			if (w == null) return -1;
			if (w.size() > max) {
				max = w.size();
				max_key = key;
			}
		}
		return max;
    }

	public int getHighestFreqTargetEdgeCount(HashMap targetEdge2SourcesMap) {
		return getHighestFreqSourceEdgeCount(targetEdge2SourcesMap);
	}

	public Vector getCandidateTargetNodesToRemove(HashMap sourceEdge2TargetsMap) {
		String key = getHighestFreqSourceEdge(sourceEdge2TargetsMap);
		return (Vector) sourceEdge2TargetsMap.get(key);
	}

	public Vector getCandidateSourceNodesToRemove(HashMap targetEdge2SourcesMap) {
		return getCandidateTargetNodesToRemove(targetEdge2SourcesMap);
	}

    public boolean isSourceNodeRemovable(Vector v, String targetEdge, String nodeId) {
		Vector u = StringUtils.parseData(targetEdge);
		String max_id = (String) u.elementAt(0);
		String max_edge = (String) u.elementAt(1);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
            if (isEdge(line)) {
				String source = getSourceId(line);
				String target = getTargetId(line);
				String edge = getEdgeLabel(line);

				if (edge.compareTo(max_edge) != 0) {
				    return false;
				} else if (target.compareTo(nodeId) == 0) {
					return false;
				} else if (source.compareTo(nodeId) == 0 && target.compareTo(max_id) != 0) {
					return false;
				}
			}
		}
		return true;
	}

    public boolean isTargetNodeRemovable(Vector v, String sourceEdge, String nodeId) {
		Vector u = StringUtils.parseData(sourceEdge);

		//44|Inv_Gene_Involved_In_Pathogenesis_Of_Disease nodeId

		String max_id = (String) u.elementAt(0);
		String max_edge = (String) u.elementAt(1);

		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
            if (isEdge(line)) {
				String source = getSourceId(line);
				String target = getTargetId(line);
				String edge = getEdgeLabel(line);
				if (edge == null) return false;
				if (source.compareTo(max_id) == 0 && edge.compareTo(max_edge) != 0 && target.compareTo(nodeId) == 0 ) {
				    return false;
				} else if (source.compareTo(max_id) != 0 && edge.compareTo(max_edge) == 0 && target.compareTo(nodeId) == 0) {
					return false;
				} else if (target.compareTo(max_id) != 0 && edge.compareTo(max_edge) == 0 && source.compareTo(nodeId) == 0) {
					return false;
				}
			}
		}
		return true;
	}

	public Vector getTargetNodesToRemove(Vector v, String sourceEdge, Vector candidates) {
        Vector w = new Vector();
		for (int i=0; i<candidates.size(); i++) {
			String nodeId = (String) candidates.elementAt(i);
			boolean retval = isTargetNodeRemovable(v, sourceEdge, nodeId);
			if (retval) {
				w.add(nodeId);
			}
		}
 		return w;
	}

	public Vector getSourceNodesToRemove(Vector v, String targetEdge, Vector candidates) {
        Vector w = new Vector();
		for (int i=0; i<candidates.size(); i++) {
			String nodeId = (String) candidates.elementAt(i);
			boolean retval = isSourceNodeRemovable(v, targetEdge, nodeId);
			if (retval) {
				w.add(nodeId);
			}
		}
 		return w;
	}

	public Vector addGroupNode(Vector v) {
		return null;
	}

	public Vector addEdgeFromGroupNode(Vector v) {
		return null;
	}

	public Vector addEdgeToGroupNode(Vector v) {
		return null;
	}

	public HashMap createNodeId2LabelMap(Vector v) {
		HashMap map = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (isNode(line)) {
				String node_id = getNodeId(line);
				String node_label = getNodeLabel(line);
				map.put(node_id, node_label);
			}
		}
		return map;
	}



	public HashMap createSourceEdge2TargetsMap(Vector v) {
		HashMap map = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
            if (isEdge(line)) {
				String source = getSourceId(line);
				String target = getTargetId(line);
				String edgeLabel = getEdgeLabel(line);
				String key = source + "|" + edgeLabel;
				Vector w = new Vector();
				if (map.containsKey(key)) {
					w = (Vector) map.get(key);
				}
				if (!w.contains(target)) {
					w.add(target);
				}
				map.put(key, w);
			}
		}
		return map;

	}

	public HashMap createTargetEdge2SourcesMap(Vector v) {
		HashMap map = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
            if (isEdge(line)) {
				String source = getSourceId(line);
				String target = getTargetId(line);
				String edgeLabel = getEdgeLabel(line);
				String key = target + "|" + edgeLabel;
				Vector w = new Vector();
				if (map.containsKey(key)) {
					w = (Vector) map.get(key);
				}
				if (!w.contains(source)) {
					w.add(source);
				}
				map.put(key, w);
			}
		}
		return map;
	}

/*


    public Vector<String> StringUtils.parseData(String line) {
		if (line == null) return null;
        String tab = "|";
        return StringUtils.parseData(line, tab);
    }

    public Vector<String> StringUtils.parseData(String line, String tab) {
		if (line == null) return null;
        Vector data_vec = new Vector();
        StringTokenizer st = new StringTokenizer(line, tab);
        if (st == null) return null;
        while (st.hasMoreTokens()) {
            String value = st.nextToken();
            data_vec.add(value);
        }
        return data_vec;
    }
*/

	public static Vector readFile(String filename)
	{
		Vector v = new Vector();
		try {

            FileReader a = new FileReader(filename);
            BufferedReader br = new BufferedReader(a);
            String line;
            line = br.readLine();
            while(line != null){
                v.add(line);
                line = br.readLine();
            }
            br.close();
		} catch (Exception ex) {
            ex.printStackTrace();
		}
		return v;
	}

	//{id: 1, label: 'Adenocarcinoma of the Gastroesophageal Junction (C9296)', shape: 'dot', size: 5},
    public boolean isNode(String line) {
		if (line.indexOf("id:") != -1 && line.indexOf("label:") != -1) {
			return true;
		}
		return false;
	}

    //{from: 44, to: 33, arrows:'to', label: 'Gene_Involved_In_Pathogenesis_Of_Disease', length: 400},
    public boolean isEdge(String line) {
		if (line.indexOf("from:") != -1 && line.indexOf("to:") != -1) {
			return true;
		}
		return false;
	}

	//{id: 1, label: 'Adenocarcinoma of the Gastroesophageal Junction (C9296)', shape: 'dot', size: 5},
	public String getNodeId(String line) {
		int n = line.indexOf("id:");
		String t = line.substring(n+3, line.length());
		n = t.indexOf(",");
		t = t.substring(1, n);
		return t;
	}

	//{id: 1, label: 'Adenocarcinoma of the Gastroesophageal Junction (C9296)', shape: 'dot', size: 5},
	public String getNodeLabel(String line) {
		int n = line.indexOf("label:");
		String t = line.substring(n+6, line.length());
		n = t.indexOf(",");
		t = t.substring(2, n-1);
		return t;
	}

//    //{from: 44, to: 33, arrows:'to', label: 'Gene_Involved_In_Pathogenesis_Of_Disease', length: 400},
    public String getSourceId(String line) {
		int n = line.indexOf("from:");
		String t = line.substring(n+5, line.length());
		n = t.indexOf(",");
		t = t.substring(1, n);
		return t;
	}

    public String getTargetId(String line) {
		int n = line.indexOf("to:");
		String t = line.substring(n+3, line.length());
		n = t.indexOf(",");
		t = t.substring(1, n);
		return t;
	}

	public String getEdgeLabel(String line) {
		int n = line.indexOf("label:");
		String t = line.substring(n+6, line.length());
		n = t.indexOf(",");
		t = t.substring(2, n-1);
		return t;
	}

	public void dumpVector(Vector v) {
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			System.out.println(t);
		}
	}


	public void dumpVector(String label, Vector v) {
		System.out.println(label);
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			if (label.length() > 0) {
				System.out.println("(" + j + ")" + t);
			} else {
				System.out.println(t);
			}
		}
	}

	public void dumpHashMap(String label, HashMap hmap) {
		if (hmap == null) return;
		System.out.println(label);
		Iterator it = hmap.keySet().iterator();
		if (it == null) return;
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector v = (Vector) hmap.get(key);
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				buf.append(t);
				if (i <v.size()-1) buf.append(", ");
			}
			System.out.println(key + " --> " + buf.toString());
		}
	}

	public Vector extractNodeIDs(Vector v) {
	    Vector w = new Vector();
	    for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int n = t.indexOf("'");
			t = t.substring(n+1, t.length());
			n = t.indexOf("'");
			t = t.substring(0, n);
			w.add(t);
		}
		return w;
	}


	public Vector vec_difference(Vector u, Vector v) {
	    Vector w = new Vector();
	    for (int i=0; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			if (!v.contains(t)) {
				w.add(t);
			}
		}
		return w;
	}

	public String get_removed_node_str(Vector removed_node_ids, String groupNodeId) {
		StringBuffer buf = new StringBuffer();
		buf.append("<table>");
		String groupNodeContent = "Node " + groupNodeId + " content:";
		buf.append("<tr><td align=\"left\">");
		buf.append("<b>" + groupNodeContent + "</b>");
		buf.append("</td></tr>");
		int j = 0;
		for (int i=0; i<removed_node_ids.size(); i++) {
			j++;
			String t = (String) removed_node_ids.elementAt(i);
			buf.append("<tr><td align=\"left\">");
			buf.append("(" + j + ") " + t);
			buf.append("</td></tr>");
		}
		buf.append("</table>");
		return buf.toString();
	}

/*
    //PrintWriter out = response.getWriter();
	public void generateResponse(PrintWriter out, Vector v, String group_node_id, String group_node_data) {
      out.println("<!doctype html>");
      out.println("<html>");
      out.println("<head>");
      out.println("  <title>View Graph</title>");
      out.println("");
      out.println("  <style type=\"text/css\">");
      out.println("    body {");
      out.println("      font: 10pt sans;");
      out.println("    }");
      out.println("    #conceptnetwork {");
      out.println("      width: 1200px;");
      out.println("      height: 600px;");
      out.println("      border: 1px solid lightgray;");
      out.println("    }");
      out.println("    table.legend_table {");
      out.println("      border-collapse: collapse;");
      out.println("    }");
      out.println("    table.legend_table td,");
      out.println("    table.legend_table th {");
      out.println("      border: 1px solid #d3d3d3;");
      out.println("      padding: 10px;");
      out.println("    }");
      out.println("");
      out.println("    table.legend_table td {");
      out.println("      text-align: center;");
      out.println("      width:110px;");
      out.println("    }");
      out.println("  </style>");
      out.println("");
      out.println("  <script type=\"text/javascript\" src=\"ncitbrowser/css/vis/vis.js\"></script>");
      out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"ncitbrowser/css/vis/vis.css\" />");
      out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"ncitbrowser/css/styleSheet.css\" />");
      out.println("");
      out.println("  <script type=\"text/javascript\">");
      out.println("    var nodes = null;");
      out.println("    var edges = null;");
      out.println("    var network = null;");
      out.println("");
      out.println("    function destroy() {");
      out.println("      if (network !== null) {");
      out.println("        network.destroy();");
      out.println("        network = null;");
      out.println("      }");
      out.println("    }");
      out.println("");
      out.println("    function draw() {");

      for (int i=0; i<v.size(); i++) {
		  String t = (String) v.elementAt(i);
		  out.println(t);
	  }
      out.println("      // create a network");
      out.println("      var container = document.getElementById('conceptnetwork');");
      out.println("      var data = {");
      out.println("        nodes: nodes,");
      out.println("        edges: edges");
      out.println("      };");
      out.println("      var options = {");
      out.println("        interaction: {");
      out.println("          navigationButtons: true,");
      out.println("          keyboard: true");
      out.println("        }");
      out.println("      };");
      out.println("      network = new vis.Network(container, data, options);");
      out.println("");
      out.println("      // add event listeners");
      out.println("      network.on('select', function(params) {");

      out.println("      if (params.nodes == '" + group_node_id + "') {");
	  out.println("         document.getElementById('selection').innerHTML = '" + group_node_data + "';");
	  out.println("      }");
      out.println("      });");
      out.println("}");
      out.println("  </script>");
      out.println("</head>");
      out.println("");
      out.println("<body onload=\"draw();\">");
      out.println("<div class=\"ncibanner\">");
      out.println("  <a href=\"http://www.cancer.gov\" target=\"_blank\">");
      out.println("    <img src=\"ncitbrowser/images/logotype.gif\"");
      out.println("      width=\"556\" height=\"39\" border=\"0\"");
      out.println("      alt=\"National Cancer Institute\"/>");
      out.println("  </a>");
      out.println("  <a href=\"http://www.cancer.gov\" target=\"_blank\">");
      out.println("    <img src=\"ncitbrowser/images/spacer.gif\"");
      out.println("      width=\"60\" height=\"39\" border=\"0\"");
      out.println("      alt=\"National Cancer Institute\" class=\"print-header\"/>");
      out.println("  </a>");
      out.println("  <a href=\"http://www.nih.gov\" target=\"_blank\" >");
      out.println("    <img src=\"ncitbrowser/images/tagline_nologo.gif\"");
      out.println("      width=\"219\" height=\"39\" border=\"0\"");
      out.println("      alt=\"U.S. National Institutes of Health\"/>");
      out.println("  </a>");
      out.println("  <a href=\"http://www.cancer.gov\" target=\"_blank\">");
      out.println("    <img src=\"ncitbrowser/images/cancer-gov.gif\"");
      out.println("      width=\"125\" height=\"39\" border=\"0\"");
      out.println("      alt=\"www.cancer.gov\"/>");
      out.println("  </a>");
      out.println("</div>");
      out.println("<p></p>");
      out.println("<form id=\"data\" method=\"post\" action=\"ncitbrowser/ajax?action=view_graph\">");
      out.println("Relationships");
      out.println("<select name=\"type\" >");
      out.println("  <option value=\"ALL\" selected>ALL</option>");
      out.println("  <option value=\"type_superconcept\">superconcept</option>");
      out.println("  <option value=\"type_role\">role</option>");
      out.println("  <option value=\"type_inverse_role\">inverse_role</option>");
      out.println("  <option value=\"type_association\">association</option>");
      out.println("  <option value=\"type_part_of\">part_of</option>");
      out.println("  <option value=\"type_part_of_path\">part_of_path</option>");
      out.println("</select>");
      out.println("<input type=\"hidden\" id=\"scheme\" name=\"scheme\" value=\"NCI_Thesaurus\" />");
      out.println("<input type=\"hidden\" id=\"version\" name=\"version\" value=\"15.07d\" />");
      out.println("<input type=\"hidden\" id=\"ns\" name=\"ns\" value=\"NCI_Thesaurus\" />");
      out.println("<input type=\"hidden\" id=\"code\" name=\"code\" value=\"C12809\" />");
      out.println("");
      out.println("&nbsp;&nbsp;");
      out.println("<input type=\"submit\" value=\"Refresh\"></input>");
      out.println("</form>");
      out.println("");
      out.println("<div style=\"width: 800px; font-size:14px; text-align: justify;\">");
      out.println("</div>");
      out.println("");
      out.println("<div id=\"conceptnetwork\"></div>");
      out.println("");
      out.println("<p id=\"selection\"></p>");
      out.println("</body>");
      out.println("</html>");
   }
*/
    public Vector get_nodes_to_remove(Vector v, HashMap hmap) {
		//int source_max = getHighestFreqSourceEdgeCount(hmap);
		String max_source_edge_str = getHighestFreqSourceEdge(hmap);
		Vector v1 = getCandidateTargetNodesToRemove(hmap);
		//dumpVector("CandidateTargetNodesToRemove:", v1);
		Vector nodes_to_remove = getTargetNodesToRemove(v, max_source_edge_str, v1);
		return nodes_to_remove;
	}


	public String get_removed_node_str(Vector v, HashMap hmap) {
		int n1 = getNodeCount(v);
		String groupNodeId = new Integer(n1+1).toString();
		Vector w = null;
		String removed_nodes_str = "";
		Vector nodes_to_remove = get_nodes_to_remove(v, hmap);
		if (nodes_to_remove.size() == 0) return removed_nodes_str;
		//String max_source_edge_str =  getHighestFreqSourceEdge(hmap);
		w = removeNodes(v, nodes_to_remove);
		Vector removed_nodes = vec_difference(v, w);
		Vector removed_node_ids = extractNodeIDs(removed_nodes);
		removed_nodes_str = get_removed_node_str(removed_node_ids, groupNodeId);
		return removed_nodes_str;
	}

	public Vector reduceGraph(Vector v, HashMap hmap) {
		return reduceGraph(v, hmap, true);
	}

	public Vector reduceGraph(Vector v, HashMap hmap, boolean forward) {

		//int n1 = getNodeCount(v);
		//String groupNodeId = new Integer(n1+1).toString();
		int n1 = getGroupNodeID();
		String groupNodeId = new Integer(n1).toString();
		String groupNodeLabel = "Node " + groupNodeId;
		Vector w = null;
		try {
			Vector nodes_to_remove = get_nodes_to_remove(v, hmap);
			String removed_nodes_str = null;
			if (nodes_to_remove.size() > 0) {
				String max_source_edge_str =  getHighestFreqSourceEdge(hmap);
				w = removeNodes(v, nodes_to_remove);
				Vector removed_nodes = vec_difference(v, w);
				Vector removed_node_ids = extractNodeIDs(removed_nodes);
				removed_nodes_str = get_removed_node_str(removed_node_ids, groupNodeId);
				w = removeEdges(w, nodes_to_remove);
				Vector u = StringUtils.parseData(max_source_edge_str);
				String max_node = (String) u.elementAt(0);
				String max_edge = (String) u.elementAt(1);
				String sourceNodeId = max_node;
				w = insert_group_node(w, groupNodeId, groupNodeLabel);
				w = insert_edge_to_group_node(w, sourceNodeId, groupNodeId, max_edge, forward);
				return w;
		    }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}

	public String getGroupNodeId(Vector v) {
		int n1 = getNodeCount(v);
		return new Integer(n1+1).toString();
	}

	public Vector reduce_graph(Vector v, boolean direction) {
		int n = getNodeCount(v);
		HashMap hmap = createSourceEdge2TargetsMap(v);
		if (!direction) {
			hmap = getInverseHashMap(hmap);
		}
		return reduceGraph(v, hmap, direction);
	}

	public boolean graph_reduced(Vector v, Vector w) {
		int n1 = getNodeCount(v);
		int n2 = getNodeCount(w);
		if (n1 > n2) {
			return true;
		}
		return false;
	}

	public String get_removed_node_str(Vector v, boolean direction) {
		HashMap hmap = createSourceEdge2TargetsMap(v);
		if (!direction) {
			hmap = getInverseHashMap(hmap);
		}
		return get_removed_node_str(v, hmap);
	}

	public Vector get_group_node_ids(Vector v) {
		Vector w = new Vector();
		if (v == null) return w;
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			t = t.trim();
			if (t.startsWith("{id:")) {
				int n = t.indexOf("label:");
				String s = t.substring(n, t.length());
				n = s.indexOf(",");
				if (n != -1) {
					s = s.substring(0, n-1);
					n = s.indexOf("'");
					if (n != -1) {
						s = s.substring(n+1, s.length());
						if (s.startsWith("Node")) {
							n = s.lastIndexOf(" ");
							s = s.substring(n+1, s.length());
							w.add(s);
						}
				    }
			    }
			}
		}
		return w;
	}

/*
    public void test(String inputfile, String outputfile) {
		PrintWriter pw = null;
		Vector v = readFile(inputfile);
		int n1 = getNodeCount(v);
		String groupNodeId = new Integer(n1+1).toString();
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			HashMap hmap = createSourceEdge2TargetsMap(v);
			Vector w = reduceGraph(v, hmap, true);
			int n2 = getNodeCount(w);
			boolean graph_reduced = false;
			if (n1 > n2 * 2) {
				graph_reduced = true;
				String removed_nodes_str = get_removed_node_str(v, hmap);
				generateResponse(pw, w, groupNodeId, removed_nodes_str);
				System.out.println("Outputfile " + outputfile + " generated.");
			} else {
				System.out.println("No reduction at the forward direction.");
				System.out.println("Attempting the reverse direction...");
				hmap = getInverseHashMap(hmap);
				w = reduceGraph(v, hmap, false);
				n2 = getNodeCount(w);
				if (n1 > n2 * 2) {
					graph_reduced = true;
					String removed_nodes_str = get_removed_node_str(v, hmap);

					System.out.println("********** removed_nodes_str: " + removed_nodes_str);

					generateResponse(pw, w, groupNodeId, removed_nodes_str);
					System.out.println("Outputfile " + outputfile + " generated.");
				}
			}
			if (!graph_reduced) {
				System.out.println("No graph reduction is made.");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				pw.close();
			} catch (Exception e) {
                e.printStackTrace();
			}
		}
    }

	public static void main(String args[])
	{
		String filename = args[0];
		Vector v = readFile(filename);
		GraphReductionUtils util = new GraphReductionUtils(v);

		//test.reduceGraph(v);
		int n = filename.lastIndexOf(".");

		String outputfile = filename.substring(0, n) + ".html";
		System.out.println("outputfile " + outputfile);
		//test.reduceGraph2(v, outputfile);

		util.test(filename, outputfile);

	}
*/
}

