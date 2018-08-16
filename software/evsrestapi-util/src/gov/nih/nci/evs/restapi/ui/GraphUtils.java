package gov.nih.nci.evs.restapi.ui;

import gov.nih.nci.evs.restapi.util.*;
import java.util.*;
import java.io.*;
import java.util.Map.Entry;

public class GraphUtils {
    public static final int NODES_ONLY = 1;
    public static final int EDGES_ONLY = 2;
    public static final int NODES_AND_EDGES = 3;

    public static final int NODE_COUNT_THRESHOLD = 25;
    public static final int MIN_EDGE_LENGTH = 200;
    public static final int EDGE_LENGTH = 400;

    public static final String NO_DATA_AVAILABLE = "No data available.";

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

    public static String generateGraphScript(Vector v) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			//t = encodeNodeLabel(t);
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
		while (it.hasNext()) {
			String node_label = (String) it.next();
			node_label_vec.add(node_label);
		}

		if (node_label_vec.size() == 0) {
			return NO_DATA_AVAILABLE;
		}

		node_label_vec = new SortUtils().quickSort(node_label_vec);
		int knt = 0;
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
		while (it.hasNext()) {
			String node_label = (String) it.next();
			//node_label = encodeNodeLabel(node_label);
			node_label_vec.add(node_label);
		}

		if (node_label_vec.size() == 0) {
			//return NO_DATA_AVAILABLE;
			return v;
		}

		node_label_vec = new SortUtils().quickSort(node_label_vec);
		int knt = 0;
		HashMap label2IdMap = new HashMap();
		for (int k=0; k<node_label_vec.size(); k++) {
			String node_label = (String) node_label_vec.elementAt(k);
			int j = k+1;
			String id = "" + j;
			label2IdMap.put(node_label, id);
		}

		StringBuffer buf = new StringBuffer();
        if (option == NODES_ONLY || option == NODES_AND_EDGES) {
			v.add("var nodes = [");
			for (int k=0; k<node_label_vec.size()-1; k++) {
				String node_label = (String) node_label_vec.elementAt(k);
				String id = (String) label2IdMap.get(node_label);
				/* testing
				if (node_label_vec.size() <= NODE_COUNT_THRESHOLD) {
					v.add("{id: " + id + ", label: '" + node_label + "'},");
				} else {
					v.add("{id: " + id + ", label: '" + node_label + "', shape: 'dot', size: 5},");
				}
				*/
				v.add("{id: " + id + ", label: '" + node_label + "'},");

			}
			String node_label = (String) node_label_vec.elementAt(node_label_vec.size()-1);
			String id = (String) label2IdMap.get(node_label);
			/*
			if (node_label_vec.size() <= NODE_COUNT_THRESHOLD) {
				v.add("{id: " + id + ", label: '" + node_label + "'}");
			} else {
				v.add("{id: " + id + ", label: '" + node_label + "', shape: 'dot', size: 5},");
			}
			*/
			v.add("{id: " + id + ", label: '" + node_label + "'}");
			v.add("];");
	    }

        if (option == EDGES_ONLY || option == NODES_AND_EDGES) {
			v.add("var edges = [");
			int count = 0;
			Iterator it2 = edge_map.keySet().iterator();
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


/*
    public static void main(String [] args) {
		Vector graphData = readFile("graph.txt");
        String t = generateGraphScript(graphData, NODES_ONLY);
        System.out.println("\n" + t);

        t = generateGraphScript(graphData, EDGES_ONLY);
        System.out.println("\n" + t);

        t = generateGraphScript(graphData, NODES_AND_EDGES);
        System.out.println("\n" + t);

	}
*/
}

