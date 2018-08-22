package gov.nih.nci.evs.restapi.ui;

import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;
import gov.nih.nci.evs.restapi.util.*;

import java.util.*;
import java.io.*;
import java.util.Map.Entry;
//import org.apache.log4j.*;

public class VisUtils {
    //private static Logger _logger = Logger.getLogger(VisualizationUtils.class);

    private OWLSPARQLUtils owlSPARQLUtils = null;//(String sparql_service) {
    private String sparql_service = null;
    private String prefixes = null;

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
    public static HashMap RELATIONSHIP_LABEL_MAP;
    String named_graph = null;

    static {
		RELATIONSHIP_LABEL_MAP = new HashMap();
		RELATIONSHIP_LABEL_MAP.put("ALL", "ALL");
		RELATIONSHIP_LABEL_MAP.put("type_superconcept", "Superconcept");
		RELATIONSHIP_LABEL_MAP.put("type_subconcept", "Subconcept");
		RELATIONSHIP_LABEL_MAP.put("type_role", "Role");
		RELATIONSHIP_LABEL_MAP.put("type_inverse_role", "Inverse Role");
		RELATIONSHIP_LABEL_MAP.put("type_association", "Association");
		RELATIONSHIP_LABEL_MAP.put("type_inverse_association", "Inverse Association");
		RELATIONSHIP_LABEL_MAP.put("type_part_of", "Part Of");
		RELATIONSHIP_LABEL_MAP.put("type_part_of_path", "Part Of (Path to Roots)");
	};

	public static String getRelatinshipLabel(String option_label) {
	    if (!RELATIONSHIP_LABEL_MAP.containsKey(option_label)) return option_label;
	    return (String) RELATIONSHIP_LABEL_MAP.get(option_label);
	}

	public VisUtils() {

	}

	public VisUtils(String sparql_service) {
        this.sparql_service = sparql_service;
        owlSPARQLUtils = new OWLSPARQLUtils(sparql_service);
        String serviceUrl = sparql_service;
        int n = sparql_service.lastIndexOf("?");
        if (n != -1) {
			serviceUrl = sparql_service.substring(0, n);
		}

		System.out.println("serviceUrl: " + serviceUrl);

		MetadataUtils metadataUtils = new MetadataUtils(serviceUrl);
		String codingScheme = "NCI_Thesaurus";
		long ms = System.currentTimeMillis();
		String version = metadataUtils.getLatestVersion(codingScheme);
		System.out.println(codingScheme);
		System.out.println(version);
		this.named_graph = metadataUtils.getNamedGraph(codingScheme);


	}

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
		Vector v = owlSPARQLUtils.getLabelByCode(named_graph, code);
		v = new ParserUtils().getResponseValues(v);
		return (String) v.elementAt(0);
	}

    public String generateDiGraph(String scheme, String version, String namespace, String code) {
		boolean useNamespace = false;
		if (namespace != null) useNamespace = true;
		String name = "<NO DESCRIPTION>";
		String retstr = getEntityDescriptionByCode(code);
		/*
		Vector v = owlSPARQLUtils.getLabelByCode(named_graph, code);
		v = new ParserUtils().getResponseValues(v);
		String retstr = (String) v.elementAt(0);
		*/

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

        RelationshipHelper relUtils = new RelationshipHelper(sparql_service);
        HashMap relMap = relUtils.getRelationshipHashMap(scheme, version, code, namespace, useNamespace);

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


    public String generateGraphScript(String scheme, String version, String namespace, String code) {
		return generateGraphScript(scheme, version, namespace, code, null);
	}


    public String generateGraphScript(String scheme, String version, String namespace, String code, String[] types) {
		return generateGraphScript(scheme, version, namespace, code, types, NODES_AND_EDGES, null);
	}



    public String generateGraphScript(String scheme, String version, String namespace, String code, String[] types, int option, HashMap hmap) {
        if (types == null) {
			types = ALL_RELATIONSHIP_TYPES;
		}
        Vector graphData = generateGraphData(scheme, version, namespace, code, types, option, hmap);
        return GraphUtils.generateGraphScript(graphData, option);
	}


    public Vector generateGraphScriptVector(String scheme, String version, String namespace, String code, String[] types, int option, HashMap hmap) {
        if (types == null) {
			types = ALL_RELATIONSHIP_TYPES;
		}
		System.out.println("generateGraphData ...");
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

/*
    public Vector generatePartonomyGraphData(String scheme, String version, String namespace, String code, String type, int option, HashMap hmap) {
		Vector graphData = new Vector();
		boolean useNamespace = true;
		if (gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			useNamespace = false;
		}

		String name = "<NO DESCRIPTION>";
		String retstr = owlSPARQLUtils.getEntityDescriptionByCode(code);
		if (retstr != null) {
			name = retstr;
		}
		name = encode(name);
		if (gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			namespace = "";
		}

		if (gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			namespace = "";
		}
		if (!gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			useNamespace = true;
		}
        String focused_node_label = getLabel(name, code);

        HashMap relMap = null;
        if (hmap == null) {
			RelationshipHelper relUtils = new RelationshipHelper(sparql_service);
			relMap = relUtils.getRelationshipHashMap(scheme, version, code, namespace, useNamespace);
	    } else {
			relMap = hmap;
		}

        HashSet nodes = new HashSet();
        nodes.add(focused_node_label);

        PartonomyUtils partUtils = new PartonomyUtils(sparql_service);

        if (type.compareTo("type_part_of") == 0) {
			ArrayList list = null;

			List part_of_list = partUtils.getPartOfData(relMap);
			for (int i=0; i<part_of_list.size(); i++) {
				String t = (String) part_of_list.get(i);
				String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
				String rel_label = getFieldValue(t, 0);
				graphData.add(focused_node_label + "|" + rel_node_label + "|" + rel_label + "|7");
			}

			List has_part_list = partUtils.getHasPartData(relMap);
			for (int i=0; i<has_part_list.size(); i++) {
				String t = (String) has_part_list.get(i);
				String rel_node_label = getLabel(getFieldValue(t, 1), getFieldValue(t, 2));
				String rel_label = getFieldValue(t, 0);
				graphData.add(rel_node_label + "|" + focused_node_label + "|" + rel_label + "|7");
			}

		} else if (type.compareTo("type_part_of_path") == 0) {
            HashMap map = partUtils.getPathsToRoots(scheme, version, code, namespace, PART_OF);
            if (map == null) return null;
            TreeItem root = (TreeItem) map.get(ROOT);
            graphData = treeItem2GraphData(root);

		}
		return graphData;
	}

*/

    public Vector generateGraphData(String scheme, String version, String namespace, String code, String[] types, int option, HashMap hmap) {
/*
		if (types != null && types.length == 1) {
			String type = types[0];

			if (type.compareTo("type_part_of") == 0 || type.compareTo("type_part_of_path") == 0) {
				return generatePartonomyGraphData(scheme, version, namespace, code, type, option, hmap);
			}
		}
*/

System.out.println("generateGraphData...");

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

System.out.println("useNamespace..." + useNamespace);

		String name = "<NO DESCRIPTION>";
		String retstr = getEntityDescriptionByCode(code);
		if (retstr != null) {
			name = retstr;
			System.out.println(retstr);
		}
		name = encode(name);
		System.out.println(name);

		if (gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			namespace = "";
		}
		if (!gov.nih.nci.evs.restapi.util.StringUtils.isNullOrBlank(namespace)) {
			useNamespace = true;
		}
        String focused_node_label = getLabel(name, code);
        System.out.println("focused_node_label: " + focused_node_label);

        HashMap relMap = null;
        if (hmap == null) {
			RelationshipHelper relUtils = new RelationshipHelper(sparql_service);
			relMap = relUtils.getRelationshipHashMap(scheme, version, code, namespace, useNamespace);
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

    public String generateGraphScript(String scheme, String version, String namespace, String code, int option) {
        Vector graphData = generateGraphData(scheme, version, namespace, code, ALL_RELATIONSHIP_TYPES, option, null);

        System.out.println("GraphUils.generateGraphScript ...");
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

}


