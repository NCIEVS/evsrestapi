package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;
import java.io.*;
import java.text.*;
import java.util.*;


public class ASCII2HTMLTreeConverter {

	public static Vector getEmbeddedHierarchy(Vector parent_child_vec, String rootCode, HashSet nodeSet) {
		HierarchyHelper hh = new HierarchyHelper(parent_child_vec);
        ParserUtils parser = new ParserUtils();
		Vector w = new Vector();
		HashSet visitedNodes = new HashSet();
		FirstInFirstOutQueue queue = new FirstInFirstOutQueue();
		Vector topNodes = hh.getSubclassCodes(rootCode);
		for (int i=0; i<topNodes.size(); i++) {
			String topNode = (String) topNodes.elementAt(i);
			queue.add(rootCode + "|" + topNode);
		}
		while (!queue.isEmpty()) {
			String line = (String) queue.remove();
			Vector u = StringUtils.parseData(line, '|');
			String parentCode = parser.getValue((String) u.elementAt(0));
			String code = parser.getValue((String) u.elementAt(1));
			if (!visitedNodes.contains(parentCode)) {
				visitedNodes.add(parentCode);
			}
			if (!visitedNodes.contains(code)) {
				visitedNodes.add(code);
			}
			if (nodeSet.contains(parentCode) && nodeSet.contains(code)) {
				String parentLabel = hh.getLabel(parentCode);
				String childLabel = hh.getLabel(code);
				String record = parentLabel + "|" + parentCode
				              + "|" + childLabel + "|" + code;
				if (!w.contains(record)) {
					w.add(record);
				}
				Vector v = hh.getSubclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.elementAt(j);
						queue.add(code + "|" + childCode);
					}
			    }
			} else if (nodeSet.contains(parentCode) && !nodeSet.contains(code)) {
				Vector v = hh.getSubclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.elementAt(j);
						queue.add(parentCode + "|" + childCode);
					}
				}
			} else if (!nodeSet.contains(parentCode)) {
				Vector v = hh.getSubclassCodes(code);
				if (v != null) {
					for (int j=0; j<v.size(); j++) {
						String childCode = (String) v.elementAt(j);
						queue.add(code + "|" + childCode);
					}
				}
			}
		}
		w = new SortUtils().quickSort(w);
        return w;
	}

	public static void modifyHTMLFile(String htmlfile) {
		Vector v = Utils.readFile(htmlfile);
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			line = line.replace("/ncitbrowser/images", "images");
			line = line.replace("/ncitbrowser/js", "js");
			w.add(line);
		}
		Utils.saveToFile(htmlfile, w);
	}

    public static void generateHTMLTree(String asciitreefile, String title, String root) {
		Vector parent_child_vec = Utils.readFile(asciitreefile);
		parent_child_vec = new ASCIITreeUtils().get_parent_child_vec(parent_child_vec);
		String datafile = "flattened_" + asciitreefile;
		Utils.saveToFile(datafile, parent_child_vec);
		HTMLHierarchy.run(datafile, title, root);
    }

    public static void generateDynamicHTMLTree(String parent_child_file, String htmlfile) {
		Vector parent_child_vec = Utils.readFile(parent_child_file);
        TreeItem ti = new ASCIITreeUtils().createTreeItem(parent_child_vec);
        HashMap hmap = new HashMap();
        hmap.put("<Root>", ti);
        String url = "https://nciterms.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=NCI%20Thesaurus&code=";
        new SimpleTreeUtils().writeTree2HTML(hmap, url, htmlfile);
    }

    public static void main(String[] args) {
        String parent_child_file = args[0];
        Vector parent_child_vec = Utils.readFile(parent_child_file);
        String nodefile = args[1];
        Vector v = Utils.readFile(nodefile);
        HashSet nodeSet = new HashSet();
        for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (line.endsWith("Yes")) {
				nodeSet.add((String) u.elementAt(0));
			}
		}

		System.out.println("parent_child_vec: " + parent_child_vec.size());
		System.out.println("nodeSet: " + nodeSet.size());
		String rootCode = "C54443";
		nodeSet.add(rootCode);
		Vector w = getEmbeddedHierarchy(parent_child_vec, rootCode, nodeSet);
		Utils.saveToFile(rootCode + ".txt", w);
		String htmlfile = rootCode + ".html";
		generateDynamicHTMLTree(rootCode + ".txt", htmlfile);
		modifyHTMLFile(htmlfile);
	}
}