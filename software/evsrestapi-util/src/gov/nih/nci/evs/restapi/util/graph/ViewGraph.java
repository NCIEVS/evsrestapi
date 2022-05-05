package gov.nih.nci.evs.restapi.util.graph;
import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.util.*;
import java.util.Enumeration;
import java.util.Map.Entry;
import org.apache.commons.lang.*;
//import org.apache.log4j.*;
import org.apache.logging.log4j.*;

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


public class ViewGraph {

	String serviceUrl = null;
	String named_graph = null;
	String username = null;
	String password = null;
	String ncit_version = null;
	String version = null;
	MetadataUtils mdu = null;
	RelationshipData relationshipData = null;

    public static String NCI_THESAURUS = "NCI_Thesaurus";
    public static String scheme = NCI_THESAURUS;
    public static String namespace = "ncit";

    VisUtils visUtils = null;
    GraphReductionUtils graphReductionUtils = null;

	public ViewGraph(String serviceUrl, String named_graph, String username, String password) {
		System.out.println("Initialization ...");
		long ms = System.currentTimeMillis();
		mdu = new MetadataUtils(serviceUrl, username, password);
		//nameVersion2NamedGraphMap = mdu.getNameVersion2NamedGraphMap();
		this.named_graph = named_graph;//mdu.getNamedGraph(NCI_THESAURUS);
		this.ncit_version = mdu.getLatestVersion(NCI_THESAURUS);
		this.version = this.ncit_version;
		System.out.println("ncit_version: " + ncit_version);
		visUtils = new VisUtils(serviceUrl, named_graph, username, password);
		graphReductionUtils = new GraphReductionUtils();
	}

    public void generate(PrintWriter pw, String code) {
		generate(pw, code, null);
	}


	public int countEdges(HashMap relMap, String[] types) {
		if (relMap == null || types == null) return 0;
		int knt = 0;
		List typeList = Arrays.asList(types);
		for (int k=0; k<VisUtils.ALL_RELATIONSHIP_TYPES.length; k++) {
			String rel_type = (String) VisUtils.ALL_RELATIONSHIP_TYPES[k];
			if (typeList.contains(rel_type)) {
				List list = (ArrayList) relMap.get(rel_type);
				if (list != null) {
					knt = knt + list.size();
				}
			}
		}
        return knt;
	}

    public String verify_group_node_data(String node_id, String group_node_data) {
        int n = group_node_data.indexOf("Node ");
        int m = group_node_data.indexOf(" content:");
        String group_node_id = group_node_data.substring(n+5, m);
        String s1 = group_node_data.substring(0, n+5);
        String s2 = group_node_data.substring(m, group_node_data.length());
        return s1 + node_id + s2;
	}

    public void generate(PrintWriter out, String code, String type) {
	  HashMap hmap = visUtils.getRelationshipHashMap(code);
	  String focused_concept = scheme + "|" + version + "|" + namespace + "|" + code;

	  // compute nodes and edges using hmap
	  String[] types = null;
	  if (type == null || type.compareTo("ALL") == 0) {
		  types = VisUtils.ALL_RELATIONSHIP_TYPES;
	  } else {
		  types = new String[1];
		  types[0] = type;
	  }
	  int edge_count = countEdges(hmap, types);
	  Vector v = visUtils.generateGraphScriptVector(code, types, VisUtils.NODES_AND_EDGES, hmap);
      String nodes_and_edges = null;
      String group_node_data = "";
      String group_node_id = null;
      String group_node_data_2 = "";
      String group_node_id_2 = null;
      boolean direction = true;
      HashMap group_node_id2dataMap = new HashMap();

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
			  if (group_node_ids == null) return;
			  for (int k=0; k<group_node_ids.size(); k++) {
				  String node_id = (String) group_node_ids.elementAt(k);
				  String content = verify_group_node_data(node_id, group_node_data);
				  if (content.compareTo(group_node_data) != 0) {
					  group_node_data = content;
				  }
				  if (!group_node_id2dataMap.containsKey(node_id)) {
					  group_node_id2dataMap.put(node_id, group_node_data);
					  break;
				  }
			  }
			  nodes_and_edges =  GraphUtils.generateGraphScript(w);
			  v = (Vector) w.clone();
		  }
		  direction = false;
		  w = graphReductionUtils.reduce_graph(v, direction);
		  graph_reduced = graphReductionUtils.graph_reduced(v, w);
		  if (graph_reduced) {
			  group_node_data_2 = graphReductionUtils.get_removed_node_str(v, direction);
			  Vector group_node_ids = graphReductionUtils.get_group_node_ids(w);
			  if (group_node_ids == null) return;
			  for (int k=0; k<group_node_ids.size(); k++) {
				  String node_id = (String) group_node_ids.elementAt(k);
				  String content = verify_group_node_data(node_id, group_node_data_2);
				  if (content.compareTo(group_node_data_2) != 0) {
					  group_node_data_2 = content;
				  }
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
		  nodes_and_edges =  visUtils.generateGraphScript(code, types, VisUtils.NODES_AND_EDGES, hmap);
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
      out.println("  <script type=\"text/javascript\" src=\"https://nciterms65.nci.nih.gov/ncitbrowser/css/vis/vis.js\"></script>");
      out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"https://nciterms65.nci.nih.gov/ncitbrowser/css/vis/vis.css\" />");
      out.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"https://nciterms65.nci.nih.gov/ncitbrowser/css/styleSheet.css\" />");

      out.println("");
      out.println("  <script type=\"text/javascript\">");
      out.println("    var nodes = null;");
      out.println("    var edges = null;");
      out.println("    var network = null;");
      out.println("");

      out.println("    function reset_graph(id) {");
      out.println("        window.location.href=\"https://nciterms65.nci.nih.gov/ncitbrowser/ajax?action=reset_graph&id=\" + id;");
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

      if (type != null && type.endsWith("path")) {
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
      if (it == null) return;
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
      //out.println("<script>(function(i,s,o,g,r,a,m){i[\"GoogleAnalyticsObject\"]=r;i[r]=i[r]||function(){(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)})(window,document,\"script\",\"//www.google-analytics.com/analytics.js\",\"ga\");ga(\"create\", \"UA-150112876-1\", {\"cookieDomain\":\"auto\"});ga(\"send\", \"pageview\");</script>");

      out.println("</head>");
      out.println("");
      out.println("<body onload=\"draw();\">");

      out.println("<div style='clear:both;margin-top:-5px;padding:8px;height:32px;color:white;background-color:#C31F40'>");

      out.println("  <a href=\"http://www.cancer.gov\" target=\"_blank\">");
      out.println("    <img src=\"https://nciterms65.nci.nih.gov/ncitbrowser/images/banner-red.png\"");
      out.println("      width=\"955\" height=\"39\" border=\"0\"");
      out.println("      alt=\"National Cancer Institute\"/>");
      out.println("  </a>");

      out.println("</div>");
      out.println("<p></p>");

	  if (!graph_available) {
		  out.println("<p class=\"textbodyred\">&nbsp;No graph data is available.</p>");
	  }

      out.println("<form id=\"data\" method=\"post\" action=\"https://nciterms65.nci.nih.gov/ncitbrowser/ajax?action=view_graph\">");

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
			  if (type != null && type.compareTo(rel_type) == 0) {
				  out.println("  <option value=\"" + rel_type + "\" selected>" + option_label + "</option>");
			  } else {
				  out.println("  <option value=\"" + rel_type + "\">" + option_label + "</option>");
			  }
	      }
	  }

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

      if (type != null && type.endsWith("path")) {

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

		try {
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
   }

    public static void main(String [] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		String code = args[4];
        String outputfile = "view_graph_" + code + ".html";

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			ViewGraph viewGraph = new ViewGraph(serviceUrl, named_graph, username, password);
			viewGraph.generate(pw, code);

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
}