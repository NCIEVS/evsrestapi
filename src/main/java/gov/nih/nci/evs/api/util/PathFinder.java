
package gov.nih.nci.evs.api.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;

/**
 * The Class PathFinder.
 */
public class PathFinder {
  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(PathFinder.class);

  /** The hierarchy. */
  private HierarchyUtils hierarchy;

  /**
   * Instantiates an empty {@link PathFinder}.
   */
  public PathFinder() {
  }

  /**
   * Instantiates a {@link PathFinder} from the specified parameters.
   *
   * @param hierarchy the hierarchy
   */
  public PathFinder(HierarchyUtils hierarchy) {
    this.hierarchy = hierarchy;
  }

  /**
   * Parses the data.
   *
   * @param line the line
   * @param delimiter the delimiter
   * @return the vector
   */
  public static Vector<String> parseData(String line, char delimiter) {
    if (line == null)
      return null;
    Vector<String> w = new Vector<String>();
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < line.length(); i++) {
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

  /**
   * Returns the roots.
   *
   * @return the roots
   */
  public List<String> getRoots() {
    List<String> roots = this.hierarchy.getHierarchyRoots();
    for (String root : roots) {
      System.out.println(root);
    }
    return roots;
  }

  /**
   * Creates the path.
   *
   * @param path the path
   * @return the path
   */
  private Path createPath(String path) {
    final Path p = new Path();
    final List<ConceptMinimal> concepts = new ArrayList<>();
    final String[] codes = path.split("\\|");
    for (int i = 0; i < codes.length; i++) {
      final String name = hierarchy.getName(codes[i]);
      final ConceptMinimal concept = new ConceptMinimal(codes[i]);
      concept.setLevel(i);
      concept.setName(name);
      concept.setTerminology(hierarchy.getTerminology().getTerminology());
      concept.setVersion(hierarchy.getTerminology().getVersion());
      concepts.add(concept);
    }
    p.setDirection(1);
    p.setConcepts(concepts);
    return p;
  }

  /**
   * Find paths.
   *
   * @return the paths
   */
  public Paths findPaths() {
    Paths paths = new Paths();
    Deque<String> stack = new ArrayDeque<String>();
    List<String> roots = this.hierarchy.getHierarchyRoots();
    log.debug("    roots = " + roots.size());
    for (String root : roots) {
      stack.push(root);
    }
    int ct = 0;
    while (!stack.isEmpty()) {
      String path = stack.pop();
      String[] values = path.trim().split("\\|");
      List<String> elements = Arrays.asList(values);
      String lastCode = elements.get(elements.size() - 1);
      List<String> subclasses = hierarchy.getSubclassCodes(lastCode);
      if (subclasses == null) {
        paths.add(createPath(path));
      } else {
        for (String subclass : subclasses) {
          if (path.contains(subclass + "|")) {
            log.error("  unexpected cycle = " + path + ", " + subclass);
          }
          stack.push(path + "|" + subclass);
        }
      }
      if (++ct % 100000 == 0) {
        log.debug("    paths = " + ct);
      }
    }
    log.debug("    total paths = " + ct);

    return paths;
  }
  //
  // /**
  // * Find paths to roots.
  // *
  // * @param code the code
  // * @param hset the hset
  // * @return the paths
  // */
  // public Paths findPathsToRoots(String code, Set<> hset) {
  // Paths paths = new Paths();
  // Stack<String> stack = new Stack<>();
  // stack.push(code);
  // while (!stack.isEmpty()) {
  // String path = (String) stack.pop();
  // Vector<String> u = parseData(path, '|');
  // String last_code = (String) u.elementAt(u.size() - 1);
  // List<String> sups = hierarchy.getSuperclassCodes(last_code);
  // if (sups == null) {
  // paths.add(createPath(path));
  // } else {
  // Vector<String> w = new Vector<>();
  // for (int i = 0; i < sups.size(); i++) {
  // String sup = (String) sups.get(i);
  // if (!hset.contains(sup)) {
  // w.add(sup);
  // }
  // }
  // if (w.size() == 0) {
  // paths.add(createPath(path));
  // } else {
  // for (int k = 0; k < w.size(); k++) {
  // String s = (String) w.elementAt(k);
  // stack.push(path + "|" + s);
  // }
  // }
  // }
  // }
  // return paths;
  // }
}
