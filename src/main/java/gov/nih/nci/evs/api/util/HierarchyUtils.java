
package gov.nih.nci.evs.api.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;

/**
 * Hierarchy utilities.
 */
public class HierarchyUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(HierarchyUtils.class);

  /** The terminology. */
  @Transient
  private Terminology terminology;

  /** The parent 2 child. */
  // @Field(type = FieldType.Object)
  @Transient
  private Map<String, ArrayList<String>> parent2child = new HashMap<String, ArrayList<String>>();

  /** The child 2 parent. */
  // @Field(type = FieldType.Object)
  @Transient
  private Map<String, ArrayList<String>> child2parent = new HashMap<String, ArrayList<String>>();

  /** The code 2 label. */
  // @Field(type = FieldType.Text)
  @Transient
  private Map<String, String> code2label = new HashMap<String, String>();

  /** The label 2 code. */
  @Transient
  private Map<String, String> label2code = new HashMap<String, String>();

  /** The concepts. */
  @Transient
  private Set<String> concepts = new HashSet<String>();

  /** The parents. */
  @Transient
  private Set<String> parents = new HashSet<String>();

  /** The children. */
  @Transient
  private Set<String> children = new HashSet<String>();

  /**
   * The path map. NOTE: if we need paths for >1 terminology, this doesn't work.
   * Use a different HierarchyUtils.
   */
  @Transient
  private Map<String, Paths> pathsMap = new HashMap<>();

  /** The roots. */
  @Field(type = FieldType.Object)
  private Set<String> hierarchyRoots = null;

  /** The leaves. */
  // @Field(type = FieldType.Object)
  @Transient
  private Set<String> leaves = null;

  /**
   * Instantiates an empty {@link HierarchyUtils}.
   */
  public HierarchyUtils() {
  }

  /**
   * Instantiates an empty {@link HierarchyUtils}.
   *
   * @param terminology the terminology
   */
  public HierarchyUtils(final Terminology terminology) {
    this.terminology = terminology;
  }

  /**
   * Instantiates a {@link HierarchyUtils} from the specified parameters.
   *
   * @param terminology the terminology
   * @param parentchild the parentchild
   */
  public HierarchyUtils(final Terminology terminology, final List<String> parentchild) {
    this(terminology);
    initialize(parentchild);
  }

  /**
   * Initialize.
   *
   * @param parentchild the parentchild
   */
  public void initialize(List<String> parentchild) {
    /*
     * The parentchild string is expected to be in the order of parentCode,
     * parentLabel childCode, childLabel and Tab sepearated.
     */
    for (String str : parentchild) {
      String[] values = str.trim().split("\t");
      if (parent2child.containsKey(values[0])) {
        parent2child.get(values[0]).add(values[2]);
      } else {
        ArrayList<String> children = new ArrayList<String>();
        children.add(values[2]);
        parent2child.put(values[0], children);
      }

      if (child2parent.containsKey(values[2])) {
        child2parent.get(values[2]).add(values[0]);
      } else {
        ArrayList<String> parents = new ArrayList<String>();
        parents.add(values[0]);
        child2parent.put(values[2], parents);
      }

      if (!code2label.containsKey(values[0])) {
        code2label.put(values[0], values[1]);
      }
      if (!code2label.containsKey(values[2])) {
        code2label.put(values[2], values[3]);
      }
      if (!label2code.containsKey(values[1])) {
        code2label.put(values[1], values[0]);
      }
      if (!label2code.containsKey(values[3])) {
        code2label.put(values[3], values[2]);
      }

      /*
       * Keep Track of Parents and Children
       */
      parents.add(values[0]);
      children.add(values[2]);
      concepts.add(values[0]);
      concepts.add(values[2]);
    }

    hierarchyRoots = new HashSet<String>(parents);
    hierarchyRoots.removeAll(children);

    leaves = new HashSet<String>(children);
    leaves.removeAll(parents);
    // testLoading();
  }

  /**
   * Returns the transitive closure.
   *
   * @return the transitive closure
   */
  // public ArrayList<String> getTransitiveClosure(ArrayList<String> concepts,
  // String code,
  // Integer level) {
  // ArrayList<String> children = this.parent2child.get(code);
  // if (children == null || children.size() == 0) {
  // return concepts;
  // }
  // for (String child : children) {
  // String indent = "";
  // for (int i = 0; i < level; i++) {
  // indent = indent + " ";
  // }
  // System.out.println(indent + "Parent: " + code + ": " + code2label.get(code)
  // + " Child: "
  // + child + ": " + code2label.get(child) + " Level: " + level);
  // // ArrayList<String> newChildren =
  // getTransitiveClosure(concepts, child, level + 1);
  // }
  // return concepts;
  // }

  /**
   * Returns the roots.
   *
   * @return the roots
   */
  public ArrayList<String> getHierarchyRoots() {
    return new ArrayList<String>(this.hierarchyRoots);
  }

  /**
   * Returns the subclass codes.
   *
   * @param code the code
   * @return the subclass codes
   */
  public ArrayList<String> getSubclassCodes(String code) {
    if (this.parent2child.containsKey(code)) {
      return parent2child.get(code);
    }
    return null;
  }

  /**
   * Returns the superclass codes.
   *
   * @param code the code
   * @return the superclass codes
   */
  public ArrayList<String> getSuperclassCodes(String code) {
    if (!child2parent.containsKey(code)) {
      return null;
    }
    return child2parent.get(code);
  }

  /**
   * Returns the descendant nodes.
   *
   * @param code the code
   * @return the descendant nodes
   */
  public List<Concept> getDescendants(String code) {
    ArrayList<Concept> descendants = new ArrayList<Concept>();
    Map<String, Concept> descendantMap = new LinkedHashMap<>();

    getDescendantMapLevel(code, descendantMap, 1);

    descendants = new ArrayList<Concept>(descendantMap.values());
    Collections.sort(descendants, new Comparator<Concept>() {
      @Override
      public int compare(Concept c1, Concept c2) {
        if (c1.getLevel() == c2.getLevel()) {
          return c1.getName().compareTo(c2.getName());
        } else {
          return c1.getLevel() - c2.getLevel();
        }
      }
    });
    return descendants;
  }

  /**
   * Descendant nodes helper.
   *
   * @param code the code
   * @param descendantMap the descendant map
   * @param level the current level
   * @return void
   */
  public void getDescendantMapLevel(String code, Map<String, Concept> descendantMap, int level) {
    List<String> children = parent2child.get(code);
    if (children == null || children.size() == 0) {
      return;
    }
    for (String child : children) {
      if (descendantMap.get(child) == null) {
        Concept conc = new Concept(child);
        if (parent2child.containsKey(child))
          conc.setLeaf(false);
        else
          conc.setLeaf(true);
        conc.setLevel(level);
        conc.setName(code2label.get(child));
        descendantMap.put(child, conc);
      }
      getDescendantMapLevel(child, descendantMap, level + 1);
    }
  }

  /**
   * Returns the label.
   *
   * @param code the code
   * @return the label
   */
  public String getName(String code) {
    if (this.code2label.containsKey(code)) {
      return code2label.get(code);
    }
    return null;
  }

  /*
   * This section to support the Hierarchy Browser
   */

  /**
   * Returns the root nodes.
   *
   * @return the root nodes
   */
  public ArrayList<HierarchyNode> getRootNodes() {
    ArrayList<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
    for (String code : this.hierarchyRoots) {
      HierarchyNode node = new HierarchyNode(code, code2label.get(code), false);
      nodes.add(node);
    }
    nodes.sort(Comparator.comparing(HierarchyNode::getLabel));
    return nodes;
  }

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param maxLevel the max level
   * @return the child nodes
   */
  public ArrayList<HierarchyNode> getChildNodes(String parent, int maxLevel) {
    ArrayList<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
    ArrayList<String> children = this.parent2child.get(parent);
    if (children == null) {
      return nodes;
    }
    for (String code : children) {
      HierarchyNode node = new HierarchyNode(code, code2label.get(code), false);
      getChildNodesLevel(node, maxLevel, 0);
      nodes.add(node);
    }
    nodes.sort(Comparator.comparing(HierarchyNode::getLabel));
    return nodes;
  }

  /**
   * Returns the child nodes level.
   *
   * @param node the node
   * @param maxLevel the max level
   * @param level the level
   * @return the child nodes level
   */
  public void getChildNodesLevel(HierarchyNode node, int maxLevel, int level) {
    List<String> children = this.parent2child.get(node.getCode());
    node.setLevel(level);

    if (children == null || children.size() == 0) {
      node.setLeaf(true);
      return;
    } else {
      node.setLeaf(false);
    }
    if (level >= maxLevel) {
      return;
    }

    ArrayList<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
    level = level + 1;
    for (String code : children) {
      HierarchyNode newnode = new HierarchyNode(code, code2label.get(code), false);
      getChildNodesLevel(newnode, maxLevel, level);
      List<HierarchyNode> sortedChildren = newnode.getChildren();
      // Sort children if they exist
      if (sortedChildren != null && sortedChildren.size() > 0) {
        sortedChildren.sort(Comparator.comparing(HierarchyNode::getLabel));
      }

      newnode.setChildren(sortedChildren);
      nodes.add(newnode);
    }
    nodes.sort(Comparator.comparing(HierarchyNode::getLabel));
    node.setChildren(nodes);
  }

  /**
   * Returns the all child nodes recursive.
   *
   * @param code the code
   * @param childCodes the child codes
   * @return the all child nodes recursive
   */
  public void getAllChildNodesRecursive(String code, ArrayList<String> childCodes) {
    List<String> children = this.parent2child.get(code);
    if (children == null || children.size() == 0) {
      return;
    } else {
      for (String childCode : children) {
        childCodes.add(childCode);
        getAllChildNodesRecursive(childCode, childCodes);
      }
    }
  }

  /**
   * Returns the all child nodes.
   *
   * @param code the code
   * @return the all child nodes
   */
  public List<String> getAllChildNodes(String code) {
    ArrayList<String> childCodes = new ArrayList<String>();

    List<String> children = this.parent2child.get(code);
    if (children == null || children.size() == 0) {
      return childCodes;
    }

    for (String childCode : children) {
      childCodes.add(childCode);
      getAllChildNodesRecursive(childCode, childCodes);
    }

    return childCodes.stream().distinct().collect(Collectors.toList());
    // return childCodes;
  }

  /**
   * Returns the path map.
   *
   * @param terminology the terminology
   * @return the path map
   * @throws Exception the exception
   */
  public Map<String, Paths> getPathsMap(Terminology terminology) throws Exception {
    if (pathsMap.isEmpty()) {
      logger.info("XXX");
      final Paths allPaths = new PathFinder(this).findPaths();
      final Set<String> seen = new HashSet<>();
      for (final Path path : allPaths.getPaths()) {
        for (int i = 1; i < path.getConcepts().size(); i++) {
          final String key = path.getConcepts().get(i).getCode();
          final String ptr = String.join(".", path.getConcepts().subList(0, i + 1).stream()
              .map(c -> c.getCode()).collect(Collectors.toList()));
          if (seen.contains(ptr + key)) {
            continue;
          }
          seen.add(ptr + key);
          if (!pathsMap.containsKey(key)) {
            pathsMap.put(key, new Paths());
          }
          // Keep the path just down to the node itself
          // and reverse the concepts
          final Path copy = new Path();
          copy.setDirection(1);
          int level = 0;
          for (int j = i; j >= 0; j--) {
            final ConceptMinimal concept = new ConceptMinimal(path.getConcepts().get(j));
            concept.setLevel(level++);
            copy.getConcepts().add(concept);
          }
          pathsMap.get(key).getPaths().add(copy);
        }
      }
    }

    return pathsMap;
  }

  /**
   * Returns the paths.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the paths
   * @throws Exception the exception
   */
  public Paths getPaths(String code, Terminology terminology) throws Exception {
    return getPathsMap(terminology).get(code);
  }

  /**
   * Returns the paths map.
   *
   * @param codes the codes
   * @param terminology the terminology
   * @return the paths map
   * @throws Exception the exception
   */
  public Map<String, Paths> getPathsMap(List<String> codes, Terminology terminology)
    throws Exception {
    final Map<String, Paths> map = new HashMap<>();
    for (final String code : codes) {
      if (getPathsMap(terminology).containsKey(code)) {
        map.put(code, getPathsMap(terminology).get(code));
      }
    }
    return map;
  }

  /**
   * Returns the paths to parent.
   *
   * @param code the code
   * @param parentCode the parent code
   * @param terminology the terminology
   * @param paths the paths
   * @return the paths to parent
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Paths getPathsToParent(String code, String parentCode, Terminology terminology,
    final List<Path> paths) throws JsonParseException, JsonMappingException, IOException {
    Paths conceptPaths = new Paths();
    if (paths == null) {
      return conceptPaths;
    }
    for (Path path : paths) {
      int parentIndex = -1;
      int index = -1;
      for (int i = 0; i < path.getConcepts().size(); i++) {
        if (path.getConcepts().get(i).getCode().equals(parentCode)) {
          parentIndex = i;
        }
        if (path.getConcepts().get(i).getCode().equals(code)) {
          index = i;
        }
      }
      if (parentIndex != -1 && index != -1) {
        if (parentIndex >= index) {
          throw new IOException("Parent code is a child = " + code + ", " + parentCode);
        }
        final Path subpath = new Path();
        subpath.setDirection(1);
        for (int i = parentIndex; i < index; i++) {
          subpath.getConcepts().add(path.getConcepts().get(i));
        }
        conceptPaths.add(path);
      }
    }
    return conceptPaths;
  }

  /**
   * Returns the terminology.
   *
   * @return the terminology
   */
  public Terminology getTerminology() {
    return terminology;
  }

  /**
   * Sets the terminology.
   *
   * @param terminology the terminology
   */
  public void setTerminology(final Terminology terminology) {
    this.terminology = terminology;
  }
}
