package gov.nih.nci.evs.restapi.ui;

import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.ui.*;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

import org.json.*;

/*
   @author NGIS, Kim L. Ong
*/

public class GraphDrawer {

    public static int NODES_ONLY = 1;
    public static int EDGES_ONLY = 2;
    public static int NODES_AND_EDGES = 3;

    public static String ROOT = "<ROOT>";
    public static String PART_OF = "part_of";

    public static final String[] ALL_RELATIONSHIP_TYPES = {"type_superconcept",
                                                           "type_subconcept",
                                                           "type_role",
                                                           "type_inverse_role",
                                                           "type_association",
                                                           "type_inverse_association"};


    private String sparql_endpoint = null; //SparqlProperties.get_SPARQL_SERVICE()
    OWLSPARQLUtils owlSPARQLUtils = null;
    RelationshipHelper relationshipHelper = null;
    //gov.nih.nci.evs.restapi.ui.VisUtils visUtils = null;
    String named_graph = null;

    public GraphDrawer(String serviceUrl) {
		this.sparql_endpoint = serviceUrl + "?query=";
		System.out.println("Instantiating owlSPARQLUtils ...");
		this.owlSPARQLUtils = new OWLSPARQLUtils(sparql_endpoint, null, null);
		System.out.println("Instantiating relationshipHelper ...");
   	    relationshipHelper = new RelationshipHelper(serviceUrl);
   	    /*
   	    System.out.println("Instantiating visUtils ...");
   	    visUtils = new gov.nih.nci.evs.restapi.ui.VisUtils(sparql_endpoint);
   	    */
 		MetadataUtils test = new MetadataUtils(serviceUrl);
		String scheme = "NCI_Thesaurus";
		//long ms = System.currentTimeMillis();
		//String version = test.getLatestVersion(scheme);
		//System.out.println(scheme);
		//System.out.println(version);
		this.named_graph = test.getNamedGraph(scheme);
		owlSPARQLUtils.set_named_graph(this.named_graph);
	}

	public String get_named_graph() {
		return this.named_graph;
	}

	public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
		owlSPARQLUtils.set_named_graph(this.named_graph);
	}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String getLabel(String name, String code) {
		name = encode(name);
		StringBuffer buf = new StringBuffer();
		buf.append(name + " (" + code + ")");
		return buf.toString();
	}

    public String getLabel(String line) {
        Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line);
        String name = (String) u.elementAt(0);
        name = encode(name);
        String code = (String) u.elementAt(1);
        return getLabel(name, code);
	}

    public String getFieldValue(String line, int index) {
        Vector u = gov.nih.nci.evs.restapi.util.StringUtils.parseData(line);
        return (String) u.elementAt(index);
	}

	public String encode(String t) {
		if (t == null) return null;
		t = t.replaceAll("'", "\'");
		return t;
	}

    public String getEntityDescriptionByCode(String code) {
		System.out.println("named_graph: " + named_graph);
		System.out.println("code: " + code);
		Vector v = owlSPARQLUtils.getLabelByCode(named_graph, code);
		if (v == null || v.size() == 0) return null;
		v = new ParserUtils().getResponseValues(v);
		return (String) v.elementAt(0);
	}

    public String generateDiGraph(String scheme, String version, String namespace, String code) {
		boolean useNamespace = false;
		if (namespace != null) useNamespace = true;
		String name = "<NO DESCRIPTION>";
		String retstr = getEntityDescriptionByCode(code);
		if (retstr != null) {
			name = retstr;
		}
		name = encode(name);
		if (gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			namespace = "";
		}
		StringBuffer buf = new StringBuffer();
        buf.append("\ndigraph {").append("\n");
        buf.append("node [shape=oval fontsize=16]").append("\n");
        buf.append("edge [length=100, color=gray, fontcolor=black]").append("\n");

        String focused_node_label = "\"" + getLabel(name, code) + "\"" ;

        //RelationshipHelper relUtils = new RelationshipHelper(sparql_service);
        HashMap relMap = relationshipHelper.getRelationshipHashMap(scheme, version, code, namespace, useNamespace);

		String key = "type_superconcept";
		ArrayList list = (ArrayList) relMap.get(key);
		if (list != null) {
			for (int i=0; i<list.size(); i++) {
				String t = (String) list.get(i);
				String rel_node_label = "\"" + getLabel(t) + "\"" ; //getLabel(t);
				String rel_label = "is_a";
				buf.append(focused_node_label + " -> " + rel_node_label).append("\n");
				buf.append("[label=" + rel_label + "];").append("\n");
			}
		}

		key = "type_subconcept";
		list = (ArrayList) relMap.get(key);
		if (list != null) {
			for (int i=0; i<list.size(); i++) {
				String t = (String) list.get(i);
				String rel_node_label = "\"" + getLabel(t) + "\"" ; //getLabel(t);
				String rel_label = "inverse_is_a";
				buf.append(focused_node_label + " -> " + rel_node_label).append("\n");
				buf.append("[label=" + rel_label + "];").append("\n");
			}
		}

		key = "type_role";
		list = (ArrayList) relMap.get(key);
		if (list != null) {
			for (int i=0; i<list.size(); i++) {
				String t = (String) list.get(i);
				String rel_node_label =  "\"" + getLabel(getFieldValue(t, 1), getFieldValue(t, 2)) + "\"";
				String rel_label = getFieldValue(t, 0);
				buf.append(focused_node_label + " -> " + rel_node_label).append("\n");
				buf.append("[label=" + rel_label + "];").append("\n");
			}
		}

		key = "type_inverse_role";
		list = (ArrayList) relMap.get(key);
		if (list != null) {
			for (int i=0; i<list.size(); i++) {
				String t = (String) list.get(i);
				String rel_node_label =  "\"" + getLabel(getFieldValue(t, 1), getFieldValue(t, 2)) + "\"";
				String rel_label = getFieldValue(t, 0);
				buf.append(rel_node_label + " -> " + focused_node_label).append("\n");
				buf.append("[label=" + rel_label + "];").append("\n");
			}
		}

		key = "type_association";
		list = (ArrayList) relMap.get(key);
		if (list != null) {
			for (int i=0; i<list.size(); i++) {
				String t = (String) list.get(i);
				String rel_node_label =  "\"" + getLabel(getFieldValue(t, 1), getFieldValue(t, 2)) + "\"";
				String rel_label = getFieldValue(t, 0);
				buf.append(focused_node_label + " -> " + rel_node_label).append("\n");
				buf.append("[label=" + rel_label + "];").append("\n");
			}
		}

		key = "type_inverse_association";
		list = (ArrayList) relMap.get(key);
		if (list != null) {
			for (int i=0; i<list.size(); i++) {
				String t = (String) list.get(i);
				String rel_node_label =  "\"" + getLabel(getFieldValue(t, 1), getFieldValue(t, 2)) + "\"";
				String rel_label = getFieldValue(t, 0);
				buf.append(rel_node_label + " -> " + focused_node_label).append("\n");
				buf.append("[label=" + rel_label + "];").append("\n");
			}
		}

        buf.append(focused_node_label + " [").append("\n");
        buf.append("fontcolor=white,").append("\n");
        buf.append("color=red,").append("\n");
        buf.append("]").append("\n");
        buf.append("}").append("\n");
        return buf.toString();
	}


    public Vector generateGraphScriptVector(String scheme, String version, String namespace, String code, String[] types, int option, HashMap hmap) {
        if (types == null) {
			types = ALL_RELATIONSHIP_TYPES;
		}
        Vector graphData = generateGraphData(scheme, version, namespace, code, types, option, hmap);
        return GraphUtils.generateGraphScriptVector(graphData, option);
	}

	public Vector treeItem2GraphData(TreeItem root) {
	    Vector graphData = treeItem2GraphData(root, new Vector());
	    return graphData;
    }

	public Vector treeItem2GraphData(TreeItem ti, Vector v) {
		String focused_node_label = getLabel(ti._text, ti._code);
		for (String association : ti._assocToChildMap.keySet()) {
			List<TreeItem> children = ti._assocToChildMap.get(association);
			for (TreeItem childItem : children) {
				String code = childItem._code;
				String text = childItem._text;
				String rel_node_label = getLabel(text, code);
				v.add(focused_node_label + "|" + rel_node_label + "|" + association + "|7");
				v = treeItem2GraphData(childItem, v);
			}
		}
	    return v;
    }

    public Vector generateGraphData(String scheme, String version, String namespace, String code, String[] types, int option, HashMap hmap) {
		Vector graphData = new Vector();
		List typeList = null;
		if (types != null) {
			typeList = Arrays.asList(types);
		} else {
			typeList = new ArrayList();
			typeList.add("type_superconcept");
			typeList.add("type_subconcept");
    	}

		boolean useNamespace = true;
		if (gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			useNamespace = false;
		}

		String name = "<NO DESCRIPTION>";
		String retstr = getEntityDescriptionByCode(code);
		if (retstr != null) {
			name = retstr;
		}
		name = encode(name);

		if (gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			namespace = "";
		}
		if (!gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			useNamespace = true;
		}
        String focused_node_label = getLabel(name, code);

        HashMap relMap = null;
        if (hmap == null) {
			//RelationshipHelper relUtils = new RelationshipHelper(sparql_service);
			relMap = relationshipHelper.getRelationshipHashMap(scheme, version, code, namespace, useNamespace);
	    } else {
			relMap = hmap;
		}

        HashSet nodes = new HashSet();
        nodes.add(focused_node_label);

        ArrayList list = null;

		String key = null;

		key = "type_superconcept";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(t);
					if (!nodes.contains(rel_node_label)) {
						nodes.add(rel_node_label);
					}
				}
			}
	    }

		key = "type_subconcept";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(t);
					if (!nodes.contains(rel_node_label)) {
						nodes.add(rel_node_label);
					}
				}
			}
	    }

		key = "type_role";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
					if (!nodes.contains(rel_node_label)) {
						nodes.add(rel_node_label);
					}
				}
			}
	    }

		key = "type_inverse_role";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
					if (!nodes.contains(rel_node_label)) {
						nodes.add(rel_node_label);
					}
				}
			}
	    }

		key = "type_association";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
					if (!nodes.contains(rel_node_label)) {
						nodes.add(rel_node_label);
					}
				}
			}
	    }

		key = "type_inverse_association";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
					if (!nodes.contains(rel_node_label)) {
						nodes.add(rel_node_label);
					}
				}
			}
	    }

		Vector node_label_vec = new Vector();
		Iterator it = nodes.iterator();
		while (it.hasNext()) {
			String node_label = (String) it.next();
			node_label_vec.add(node_label);
		}

		key = "type_superconcept";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(t);
					String rel_label = "is_a";
					if (focused_node_label.compareTo(rel_node_label) != 0) {
						graphData.add(focused_node_label + "|" + rel_node_label + "|" + rel_label + "|1");
				    }
				}
			}
	    }

		key = "type_subconcept";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(t);
					String rel_label = "is_a";
					if (focused_node_label.compareTo(rel_node_label) != 0) {
						graphData.add(rel_node_label + "|" + focused_node_label + "|" + rel_label + "|2");
				    }
				}
			}
	    }

		key = "type_role";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
					String rel_label = getFieldValue(t, 0);
					graphData.add(focused_node_label + "|" + rel_node_label + "|" + rel_label + "|3");
				}
			}
	    }

		key = "type_inverse_role";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
					String rel_label = getFieldValue(t, 0);
					graphData.add(rel_node_label + "|" + focused_node_label + "|" +rel_label + "|4");
				}
			}
		}

		key = "type_association";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
					String rel_label = getFieldValue(t, 0);
					graphData.add(focused_node_label + "|" + rel_node_label + "|" + rel_label + "|5");
				}
			}
	    }

		key = "type_inverse_association";
		if (typeList.contains(key)) {
			list = (ArrayList) relMap.get(key);
			if (list != null) {
				for (int i=0; i<list.size(); i++) {
					String t = (String) list.get(i);
					String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
					String rel_label = getFieldValue(t, 0);
					graphData.add(rel_node_label + "|" + focused_node_label + "|" +rel_label + "|6");
				}
			}
		}
        return graphData;
	}

    public String generateGraphScript(String scheme, String version, String namespace, String code, String[] types, int option, HashMap hmap) {
        if (types == null) {
			types = ALL_RELATIONSHIP_TYPES;
		}
        Vector graphData = generateGraphData(scheme, version, namespace, code, types, option, hmap);
        return GraphUtils.generateGraphScript(graphData, option);
	}

    public String generateGraphScript(String scheme, String version, String namespace, String code, int option) {
        Vector graphData = generateGraphData(scheme, version, namespace, code, ALL_RELATIONSHIP_TYPES, option, null);
        return GraphUtils.generateGraphScript(graphData, option);
	}

    public String findCodeInGraph(String nodes_and_edges, String id) {
		String target = "{id: " + id + ", label:";
		int n = nodes_and_edges.indexOf(target);
		if (n == -1) return null;
		String t = nodes_and_edges.substring(n+target.length(), nodes_and_edges.length());
		target = ")'}";
		n = t.indexOf(target);
		t = t.substring(0, n);
		n = t.lastIndexOf("(");
		t = t.substring(n+1, t.length());
		return t;
	}

	public int countEdges(HashMap relMap, String[] types) {
		if (relMap == null || types == null) return 0;
		int knt = 0;
		List typeList = Arrays.asList(types);
		for (int k=0; k<VisualizationUtils.ALL_RELATIONSHIP_TYPES.length; k++) {
			String rel_type = (String) VisualizationUtils.ALL_RELATIONSHIP_TYPES[k];
			if (typeList.contains(rel_type)) {
				List list = (ArrayList) relMap.get(rel_type);
				if (list != null) {
					knt = knt + list.size();
				}
			}
		}
        return knt;
	}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void view_graph(PrintWriter out, String named_graph, String scheme, String version,
                           String namespace, String code, String type) {
		HttpServletRequest request = null;
		HttpServletResponse response = null;
        view_graph(out, request, response, named_graph, scheme, version, namespace, code, type);
	}


    public void view_graph(PrintWriter out, HttpServletRequest request, HttpServletResponse response, String named_graph, String scheme, String version,
                           String namespace, String code, String type) {
        view_graph(out, request, response, "sparql", named_graph, scheme, version,
                           namespace, code, type);

	}



    public void view_graph(PrintWriter out, HttpServletRequest request, HttpServletResponse response, String sparql, String named_graph, String scheme, String version,
                           String namespace, String code, String type) {
       	HashMap hmap = null;
       	if (out == null && response != null && request != null) {
			response.setContentType("text/html");
			hmap = (HashMap) request.getSession().getAttribute("RelationshipHashMap");
			if (hmap == null) {
				hmap = relationshipHelper.getRelationshipHashMap(scheme, version, code, namespace, true);
			}

			try {
			     out = response.getWriter();
			} catch (Exception ex) {
			     ex.printStackTrace();
			     return;
			}
		} else {
			hmap = relationshipHelper.getRelationshipHashMap(scheme, version, code, namespace, true);
		}

		// compute nodes and edges using hmap
		String[] types = null;
		if (type == null || type.compareTo("ALL") == 0) {
		    types = ALL_RELATIONSHIP_TYPES;
		} else {
		    types = new String[1];
		    types[0] = type;
		}

		int edge_count = countEdges(hmap, types);
		//relationshipHelper.dumpRelationshipHashmap(hmap);

		//System.out.println("edge_count: " + edge_count);
		Vector v = null;
		try {
			v = generateGraphScriptVector(scheme, version, namespace, code, types, VisUtils.NODES_AND_EDGES, hmap);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		String nodes_and_edges = null;
		String group_node_data = "";
		String group_node_id = null;
		String group_node_data_2 = "";
		String group_node_id_2 = null;
		boolean direction = true;
		HashMap group_node_id2dataMap = new HashMap();

		gov.nih.nci.evs.restapi.ui.GraphReductionUtils graphReductionUtils = new gov.nih.nci.evs.restapi.ui.GraphReductionUtils();
		int graph_size = graphReductionUtils.getNodeCount(v);

		//KLO, 02122016
		graphReductionUtils.initialize_group_node_id(graph_size);
		if (graph_size > graphReductionUtils.MINIMUM_REDUCED_GRAPH_SIZE) {

		  group_node_id = graphReductionUtils.getGroupNodeId(v);
		  int group_node_id_int = Integer.parseInt(group_node_id);
		  group_node_id_2 = new Integer(group_node_id_int+1).toString();
		  Vector w = graphReductionUtils.reduce_graph(v, direction);

		  boolean graph_reduced = graphReductionUtils.graph_reduced(v, w);
		  if (graph_reduced) {
			  group_node_data = graphReductionUtils.get_removed_node_str(v, direction);
			  Vector group_node_ids = graphReductionUtils.get_group_node_ids(w);
			  for (int k=0; k<group_node_ids.size(); k++) {
				  String node_id = (String) group_node_ids.elementAt(k);
				  if (!group_node_id2dataMap.containsKey(node_id)) {
					  group_node_id2dataMap.put(node_id, group_node_data);
					  break;
				  }
			  }

			  nodes_and_edges = GraphUtils.generateGraphScript(w);
			  v = (Vector) w.clone();
		  }


		  direction = false;
		  w = graphReductionUtils.reduce_graph(v, direction);
		  graph_reduced = graphReductionUtils.graph_reduced(v, w);
		  if (graph_reduced) {
			  group_node_data_2 = graphReductionUtils.get_removed_node_str(v, direction);
			  Vector group_node_ids = graphReductionUtils.get_group_node_ids(w);
			  for (int k=0; k<group_node_ids.size(); k++) {
				  String node_id = (String) group_node_ids.elementAt(k);
				  if (!group_node_id2dataMap.containsKey(node_id)) {
					  group_node_id2dataMap.put(node_id, group_node_data_2);
					  break;
				  }
			  }
			  nodes_and_edges =  GraphUtils.generateGraphScript(w);
			  v = (Vector) w.clone();
		  }
		}

		if (group_node_id2dataMap.keySet().size() == 0) {
		    nodes_and_edges = generateGraphScript(scheme, version, namespace, code, types, NODES_AND_EDGES, hmap);
		}

		Vector group_node_ids = graphReductionUtils.get_group_node_ids(v);
		boolean graph_available = true;
		if (nodes_and_edges.compareTo(GraphUtils.NO_DATA_AVAILABLE) == 0) {
		    graph_available = false;
		}

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
		if (edge_count > 50) {
		  out.println("      height: 800px;");
		} else {
		  out.println("      height: 600px;");
		}
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
		out.println("  <script type=\"text/javascript\" src=\"/" + sparql + "/css/vis/vis.js\"></script>");
		out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"/" + sparql + "/css/vis/vis.css\" />");
		out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"/" + sparql + "/css/styleSheet.css\" />");

		out.println("");
		out.println("  <script type=\"text/javascript\">");
		out.println("    var nodes = null;");
		out.println("    var edges = null;");
		out.println("    var network = null;");
		out.println("");

		out.println("    function reset_graph(id) {");
		out.println("        window.location.href=\"/" + sparql + "/ajax?action=reset_graph&id=\" + id;");
		out.println("    }");


		out.println("    function destroy() {");
		out.println("      if (network !== null) {");
		out.println("        network.destroy();");
		out.println("        network = null;");
		out.println("      }");
		out.println("    }");
		out.println("");

		out.println("    function draw() {");

		if (graph_available) {
		  out.println(nodes_and_edges);
		}

		out.println("      // create a network");
		out.println("      var container = document.getElementById('conceptnetwork');");
		out.println("      var data = {");
		out.println("        nodes: nodes,");
		out.println("        edges: edges");
		out.println("      };");

		if (type.endsWith("path")) {
		  out.println("            var directionInput = document.getElementById(\"direction\").value;");
		  out.println("            var options = {");
		  out.println("                layout: {");
		  out.println("                    hierarchical: {");
		  out.println("                        direction: directionInput");
		  out.println("                    }");
		  out.println("                }");
		  out.println("            };");


		} else {

		  out.println("      var options = {");
		  out.println("        interaction: {");
		  out.println("          navigationButtons: true,");
		  out.println("          keyboard: true");
		  out.println("        }");
		  out.println("      };");

		}

		out.println("      network = new vis.Network(container, data, options);");
		out.println("");
		out.println("      // add event listeners");


		out.println("      network.on('select', function(params) {");

		Iterator it = group_node_id2dataMap.keySet().iterator();
		while (it.hasNext()) {
		  String node_id = (String) it.next();
		  String node_data = (String) group_node_id2dataMap.get(node_id);
		  out.println("      if (params.nodes == '" + node_id + "') {");
		  out.println("         document.getElementById('selection').innerHTML = '" + node_data + "';");
		  out.println("      }");
		}

		out.println("      });");
		out.println("			network.on(\"doubleClick\", function (params) {");

		String node_id_1 = null;
		String node_id_2 = null;
		it = group_node_id2dataMap.keySet().iterator();
		int lcv = 0;
		while (it.hasNext()) {
		  String node_id = (String) it.next();
		  if (lcv == 0) {
			  node_id_1 = node_id;
		  } else if (lcv == 1) {
			  node_id_2 = node_id;
		  }
		  lcv++;
		}

		if (node_id_1 != null && node_id_2 != null) {
		  out.println("      if (params.nodes != '" + node_id_1 + "' && params.nodes != '" + node_id_2 + "') {");
		  out.println("				params.event = \"[original event]\";");
		  out.println("				var json = JSON.stringify(params, null, 4);");
		  out.println("				reset_graph(params.nodes);");
		  out.println("      }");
		} else if (node_id_1 != null && node_id_2 == null) {
		  out.println("      if (params.nodes != '" + node_id_1 + "') {");
		  out.println("				params.event = \"[original event]\";");
		  out.println("				var json = JSON.stringify(params, null, 4);");
		  out.println("				reset_graph(params.nodes);");
		  out.println("      }");
		} else if (node_id_2 != null && node_id_1 == null) {
		  out.println("      if (params.nodes != '" + node_id_2 + "') {");
		  out.println("				params.event = \"[original event]\";");
		  out.println("				var json = JSON.stringify(params, null, 4);");
		  out.println("				reset_graph(params.nodes);");
		  out.println("      }");
		} else if (node_id_2 == null && node_id_1 == null) {
		  out.println("				params.event = \"[original event]\";");
		  out.println("				var json = JSON.stringify(params, null, 4);");
		  out.println("				reset_graph(params.nodes);");
		}

		out.println("		    });");

		out.println("    }");
		out.println("  </script>");
		out.println("</head>");
		out.println("");
		out.println("<body onload=\"draw();\">");

		out.println("<div class=\"ncibanner\">");
		out.println("  <a href=\"http://www.cancer.gov\" target=\"_blank\">     ");
		out.println("    <img src=\"/" + sparql + "/images/logotype.gif\"");
		out.println("      width=\"556\" height=\"39\" border=\"0\"");
		out.println("      alt=\"National Cancer Institute\"/>");
		out.println("  </a>");
		out.println("  <a href=\"http://www.cancer.gov\" target=\"_blank\">     ");
		out.println("    <img src=\"/" + sparql + "/images/spacer.gif\"");
		out.println("      width=\"60\" height=\"39\" border=\"0\" ");
		out.println("      alt=\"National Cancer Institute\" class=\"print-header\"/>");
		out.println("  </a>");
		out.println("  <a href=\"http://www.nih.gov\" target=\"_blank\" >      ");
		out.println("    <img src=\"/" + sparql + "/images/tagline_nologo.gif\"");
		out.println("      width=\"219\" height=\"39\" border=\"0\"");
		out.println("      alt=\"U.S. National Institutes of Health\"/>");
		out.println("  </a>");
		out.println("  <a href=\"http://www.cancer.gov\" target=\"_blank\">      ");
		out.println("    <img src=\"/" + sparql + "/images/cancer-gov.gif\"");
		out.println("      width=\"125\" height=\"39\" border=\"0\"");
		out.println("      alt=\"www.cancer.gov\"/>");
		out.println("  </a>");
		out.println("</div>");
		out.println("<p></p>");

		if (!graph_available) {
		  out.println("<p class=\"textbodyred\">&nbsp;No graph data is available.</p>");
		}

		out.println("<form id=\"data\" method=\"post\" action=\"/" + sparql + "/ajax?action=view_graph\">");

		out.println("Relationships");
		out.println("<select name=\"type\" >");
		if (type == null || type.compareTo("ALL") == 0) {
		  out.println("  <option value=\"ALL\" selected>ALL</option>");
		} else {
		  out.println("  <option value=\"ALL\">ALL</option>");
		}
		String rel_type = null;
		String option_label = null;

		for (int k=0; k<VisUtils.ALL_RELATIONSHIP_TYPES.length; k++) {
		  rel_type = (String) VisUtils.ALL_RELATIONSHIP_TYPES[k];
		  List list = (List) hmap.get(rel_type);
		  if (list != null && list.size() > 0) {
			  option_label = VisUtils.getRelatinshipLabel(rel_type);
			  if (type.compareTo(rel_type) == 0) {
				  out.println("  <option value=\"" + rel_type + "\" selected>" + option_label + "</option>");
			  } else {
				  out.println("  <option value=\"" + rel_type + "\">" + option_label + "</option>");
			  }
		  }
		}
		/*
		boolean hasPartOf = new PartonomyUtils(SparqlProperties.get_SPARQL_SERVICE()).hasPartOfRelationships(hmap);
		if (hasPartOf) {
		  rel_type = "type_part_of";
		  option_label = VisUtils.getRelatinshipLabel(rel_type);
		  if (type.compareTo(rel_type) == 0) {
			  out.println("  <option value=\"" + rel_type + "\" selected>" + option_label + "</option>");
		  } else {
			  out.println("  <option value=\"" + rel_type + "\">" + option_label + "</option>");
		  }

		  rel_type = "type_part_of_path";
		  option_label = VisUtils.getRelatinshipLabel(rel_type);
		  if (type.compareTo(rel_type) == 0) {
			  out.println("  <option value=\"" + rel_type + "\" selected>" + option_label + "</option>");
		  } else {
			  out.println("  <option value=\"" + rel_type + "\">" + option_label + "</option>");
		  }
		}
		*/
		out.println("</select>");
		out.println("<input type=\"hidden\" id=\"scheme\" name=\"scheme\" value=\"" + scheme + "\" />");
		out.println("<input type=\"hidden\" id=\"version\" name=\"version\" value=\"" + version + "\" />");
		out.println("<input type=\"hidden\" id=\"ns\" name=\"ns\" value=\"" + namespace + "\" />");
		out.println("<input type=\"hidden\" id=\"code\" name=\"code\" value=\"" + code + "\" />");
		out.println("");
		out.println("&nbsp;&nbsp;");
		out.println("<input type=\"submit\" value=\"Refresh\"></input>");
		out.println("</form>");
		out.println("");

		if (type.endsWith("path")) {

		out.println("<p>");
		out.println("    <input type=\"button\" id=\"btn-UD\" value=\"Up-Down\">");
		out.println("    <input type=\"button\" id=\"btn-DU\" value=\"Down-Up\">");
		out.println("    <input type=\"button\" id=\"btn-LR\" value=\"Left-Right\">");
		out.println("    <input type=\"button\" id=\"btn-RL\" value=\"Right-Left\">");
		out.println("    <input type=\"hidden\" id='direction' value=\"UD\">");
		out.println("</p>");
		out.println("<script language=\"javascript\">");
		out.println("    var directionInput = document.getElementById(\"direction\");");
		out.println("    var btnUD = document.getElementById(\"btn-UD\");");
		out.println("    btnUD.onclick = function () {");
		out.println("        directionInput.value = \"UD\";");
		out.println("        draw();");
		out.println("    }");
		out.println("    var btnDU = document.getElementById(\"btn-DU\");");
		out.println("    btnDU.onclick = function () {");
		out.println("        directionInput.value = \"DU\";");
		out.println("        draw();");
		out.println("    };");
		out.println("    var btnLR = document.getElementById(\"btn-LR\");");
		out.println("    btnLR.onclick = function () {");
		out.println("        directionInput.value = \"LR\";");
		out.println("        draw();");
		out.println("    };");
		out.println("    var btnRL = document.getElementById(\"btn-RL\");");
		out.println("    btnRL.onclick = function () {");
		out.println("        directionInput.value = \"RL\";");
		out.println("        draw();");
		out.println("    };");
		out.println("</script>");
		}

		out.println("<div style=\"width: 800px; font-size:14px; text-align: justify;\">");
		out.println("</div>");
		out.println("");
		out.println("<div id=\"conceptnetwork\"></div>");
		out.println("");
		out.println("<p id=\"selection\"></p>");
		out.println("</body>");
		out.println("</html>");

		out.flush();

		if (response != null) {
			request.getSession().setAttribute("scheme", scheme);
			request.getSession().setAttribute("version", version);
			request.getSession().setAttribute("ns", namespace);
			request.getSession().setAttribute("code", code);
			request.getSession().setAttribute("nodes_and_edges", nodes_and_edges);
			request.getSession().setAttribute("RelationshipHashMap", hmap);
		}

		try {
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


    public static String endPoint2ServiceUrl(String sparql_endpoint) {
		int n = sparql_endpoint.indexOf("?");
		if (n == -1) return sparql_endpoint;
		return sparql_endpoint.substring(0, n);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		boolean testurl = false;
		String serviceUrl = args [0];
		String sparql_endpoint = serviceUrl + "?query=";

		serviceUrl = endPoint2ServiceUrl(sparql_endpoint);
		System.out.println(serviceUrl);
		if (testurl) {
			System.exit(0);
		}

        long ms = System.currentTimeMillis();

		GraphDrawer gd = new GraphDrawer(serviceUrl);

		MetadataUtils test = new MetadataUtils(serviceUrl);
		String scheme = "NCI_Thesaurus";

		String version = test.getLatestVersion(scheme);
		//System.out.println(scheme);
		//System.out.println(version);
		String named_graph = test.getNamedGraph(scheme);
		//System.out.println(named_graph);

		PrintWriter pw = null;
		String namespace = scheme;
		String code = "C12365";
		String type = "type_superconcept";
		String outputfile = "graph.html";

		try {
			pw = new PrintWriter(outputfile, "UTF-8");
        	gd.view_graph(pw, named_graph, scheme, version, namespace, code, type);

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
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}