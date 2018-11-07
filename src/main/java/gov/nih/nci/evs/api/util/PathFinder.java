package gov.nih.nci.evs.api.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import gov.nih.nci.evs.api.model.evs.ConceptNode;
import gov.nih.nci.evs.api.model.evs.Path;
import gov.nih.nci.evs.api.model.evs.Paths;

public class PathFinder {

	private HierarchyUtils hierarchy;

	public PathFinder() {
	}

	public PathFinder(HierarchyUtils hierarchy) {
		this.hierarchy = hierarchy;
	}
	
	public static Vector <String> parseData(String line, char delimiter) {
			if(line == null) return null;
			Vector <String> w = new Vector <String>();
			StringBuffer buf = new StringBuffer();
			for (int i=0; i<line.length(); i++) {
				char c = line.charAt(i);
				if (c == delimiter) {
					w.add(buf.toString());
					buf = new StringBuffer();
				} else {
					buf.append(c);
				}
			}
			w.add(buf.toString());
			return w;
		}

	public ArrayList<String> getRoots() {
		ArrayList<String> roots = this.hierarchy.getRoots();
		for (String root : roots) {
			System.out.println(root);
		}
		return roots;
	}

	public Path createPath(String path) {
		Path p = new Path();
		List<ConceptNode> concepts = new ArrayList<ConceptNode>();
		String[] codes = path.split("\\|");
		for (int i = 0; i < codes.length; i++) {
			String name = hierarchy.getLabel(codes[i]);
			ConceptNode concept = new ConceptNode(i, name, codes[i]);
			concepts.add(concept);
		}

		p.setDirection(1);
		p.setConcepts(concepts);
		return p;
	}

	public Paths findPaths() {
		Paths paths = new Paths();
		Deque<String> stack = new ArrayDeque<String>();
		ArrayList<String> roots = this.hierarchy.getRoots();
		for (String root : roots) {
			stack.push(root);
		}
		while (!stack.isEmpty()) {
			String path = stack.pop();
			String[] values = path.trim().split("\\|");
			List<String> elements = Arrays.asList(values);
			String lastCode = elements.get(elements.size() - 1);
			ArrayList<String> subclasses = hierarchy.getSubclassCodes(lastCode);
			if (subclasses == null) {
				paths.add(createPath(path));
			} else {
				for (String subclass: subclasses) {
					stack.push(path + "|" + subclass);
				}
			}
		}

		return paths;
	}

	
	public Paths findPathsToRoots(String code, HashSet hset) {
		Paths paths = new Paths();
		Stack stack = new Stack();
		stack.push(code);
		while (!stack.isEmpty()) {
			String path = (String) stack.pop();
			Vector u = parseData(path, '|');
			String last_code = (String) u.elementAt(u.size()-1);
			List sups = hierarchy.getSuperclassCodes(last_code);
			if (sups == null) {
				paths.add(createPath(path));
			} else {
				Vector w = new Vector();
				for (int i=0; i<sups.size(); i++) {
					String sup = (String) sups.get(i);
					if (!hset.contains(sup)) {
						w.add(sup);
					}
				}
				if (w.size() == 0) {
					paths.add(createPath(path));
				} else {
					for (int k=0; k<w.size(); k++) {
						String s = (String) w.elementAt(k);
						stack.push(path + "|" + s);
					}
				}
			}
		}
        return paths;
	}
}
