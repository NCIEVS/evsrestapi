package gov.nih.nci.evs.api.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Vector;

import gov.nih.nci.evs.api.model.evs.Concept;
import gov.nih.nci.evs.api.model.evs.Path;
import gov.nih.nci.evs.api.model.evs.Paths;

public class PathFinder {

	private HierarchyUtils hierarchy;

	public PathFinder() {
	}

	public PathFinder(HierarchyUtils hierarchy) {
		this.hierarchy = hierarchy;
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
		List<Concept> concepts = new ArrayList<Concept>();
		String[] codes = path.split("\\|");
		for (int i = 0; i < codes.length; i++) {
			String name = hierarchy.getLabel(codes[i]);
			Concept concept = new Concept(i, name, codes[i]);
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

}
