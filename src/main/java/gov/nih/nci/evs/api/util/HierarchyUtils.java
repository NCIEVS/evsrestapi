
package gov.nih.nci.evs.api.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Role;
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
  private Map<String, List<String>> parent2child = new HashMap<>();

  /** The child 2 parent. */
  // @Field(type = FieldType.Object)
  @Transient
  private Map<String, List<String>> child2parent = new HashMap<>();

  /** The code 2 label. */
  // @Field(type = FieldType.Text)
  @Transient
  private Map<String, String> code2label = new HashMap<>();

  /** The parents. */
  @Transient
  private Set<String> parents = new HashSet<String>();

  /** The children. */
  @Transient
  private Set<String> children = new HashSet<String>();

  /** The association map. */
  @Transient
  private Map<String, List<Association>> associationMap = new HashMap<>(10000);

  /** The inverse association map. */
  @Transient
  private Map<String, List<Association>> inverseAssociationMap = new HashMap<>(10000);

  /** The role map. */
  @Transient
  private Map<String, List<Role>> roleMap = new HashMap<>(10000);

  /** The inverse roles. */
  @Transient
  private Map<String, List<Role>> inverseRoleMap = new HashMap<>(10000);

  /**
   * The path map. NOTE: if we need paths for >1 terminology, this doesn't work.
   * Use a different HierarchyUtils.
   */
  @Transient
  private Map<String, Set<String>> pathsMap = new HashMap<>();

  /** The roots. */
  @Field(type = FieldType.Object)
  private Set<String> hierarchyRoots = null;

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
        List<String> children = new ArrayList<>();
        children.add(values[2]);
        parent2child.put(values[0], children);
      }

      if (child2parent.containsKey(values[2])) {
        child2parent.get(values[2]).add(values[0]);
      } else {
        List<String> parents = new ArrayList<>();
        parents.add(values[0]);
        child2parent.put(values[2], parents);
      }

      if (!code2label.containsKey(values[0])) {
        code2label.put(values[0], values[1]);
      }
      if (!code2label.containsKey(values[2])) {
        code2label.put(values[2], values[3]);
      }

      /*
       * Keep Track of Parents and Children
       */
      parents.add(values[0]);
      children.add(values[2]);
    }

    hierarchyRoots = new HashSet<String>(parents);
    hierarchyRoots.removeAll(children);

  }

  /**
   * Returns the roots.
   *
   * @return the roots
   */
  public List<String> getHierarchyRoots() {
    return new ArrayList<>(hierarchyRoots);
  }

  /**
   * Returns the subclass codes.
   *
   * @param code the code
   * @return the subclass codes
   */
  public List<String> getSubclassCodes(String code) {
    if (parent2child.containsKey(code)) {
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
  public List<String> getSuperclassCodes(String code) {
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
    List<Concept> descendants = new ArrayList<>();
    Map<String, Concept> descendantMap = new LinkedHashMap<>();

    getDescendantMapLevel(code, descendantMap, 1);

    descendants = new ArrayList<>(descendantMap.values());
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
    if (code2label.containsKey(code)) {
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
  public List<HierarchyNode> getRootNodes() {
    List<HierarchyNode> nodes = new ArrayList<>();
    for (String code : hierarchyRoots) {
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
  public List<HierarchyNode> getChildNodes(final String parent, final int maxLevel) {
    final List<HierarchyNode> nodes = new ArrayList<>();
    final List<String> children = parent2child.get(parent);
    if (children == null) {
      return nodes;
    }
    for (final String code : children) {
      final HierarchyNode node =
          new HierarchyNode(code, code2label.get(code), parent2child.get(code) != null);
      getChildNodesLevel(node, maxLevel, 0);
      nodes.add(node);
    }
    nodes.sort(Comparator.comparing(HierarchyNode::getLabel));
    return nodes;
  }

  /**
   * Returns the parent nodes.
   *
   * @param child the child
   * @return the parent nodes
   */
  public List<HierarchyNode> getParentNodes(final String child) {
    final List<HierarchyNode> nodes = new ArrayList<>();
    final List<String> parents = child2parent.get(child);
    if (parents == null) {
      return nodes;
    }
    for (final String code : parents) {
      final HierarchyNode node =
          new HierarchyNode(code, code2label.get(code), parent2child.get(code) != null);
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
    List<String> children = parent2child.get(node.getCode());
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

    List<HierarchyNode> nodes = new ArrayList<>();
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
  public void getAllChildNodesRecursive(String code, List<String> childCodes) {
    List<String> children = parent2child.get(code);
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
    List<String> childCodes = new ArrayList<>();

    List<String> children = parent2child.get(code);
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
  public Map<String, Set<String>> getPathsMap(final Terminology terminology) throws Exception {
    if (pathsMap.isEmpty() && terminology.getMetadata().getHierarchy() != null
        && terminology.getMetadata().getHierarchy()) {

      // This finds paths for leaf nodes, and we need to turn into full paths
      // for each code. Write to a file because this can be a lot of data.
      final Set<String> paths = findPaths();
      final File file = File.createTempFile("tmp", "txt");
      logger.info("    write file");
      FileUtils.writeLines(file, "UTF-8", paths, "\n", false);
      paths.clear();
      logger.info("    start build paths map");
      try (final BufferedReader in = new BufferedReader(new FileReader(file))) {
        int partCt = 0;
        // Go from the end so we can remove entries as we work through
        String path = null;
        while ((path = in.readLine()) != null) {
          final List<String> parts = Arrays.asList(path.split("\\|"));
          for (int i = 1; i < parts.size(); i++) {
            partCt++;
            final String key = parts.get(i);
            final String ptr = String.join("|", parts.subList(0, i + 1));

            if (!pathsMap.containsKey(key)) {
              // Keep set size to a minimum as most things have small numbers of
              // paths
              pathsMap.put(key, new HashSet<>(5));
            }

            if (!pathsMap.get(key).contains(ptr)) {
              partCt++;
              pathsMap.get(key).add(ptr);
            }
          }
        }
        logger.debug("    total paths map = " + pathsMap.size() + ", " + partCt);
        FileUtils.deleteQuietly(file);
      }
    }

    return pathsMap;
  }

  /**
   * Find paths.
   *
   * @return the list
   */
  private Set<String> findPaths() {
    final Set<String> paths = new HashSet<>();
    final Deque<String> stack = new ArrayDeque<String>();
    final List<String> roots = getHierarchyRoots();
    logger.debug("    roots = " + roots.size());
    for (final String root : roots) {
      stack.push(root);
    }
    int ct = 0;
    while (!stack.isEmpty()) {
      final String path = stack.pop();
      final String[] values = path.trim().split("\\|");
      final List<String> elements = Arrays.asList(values);
      final String lastCode = elements.get(elements.size() - 1);
      final List<String> subclasses = getSubclassCodes(lastCode);
      if (subclasses == null) {
        paths.add(path);
      } else {
        for (final String subclass : subclasses) {
          if (path.contains("|" + subclass + "|")) {
            logger.error("  unexpected cycle = " + path + ", " + subclass);
          }
          stack.push(path + "|" + subclass);
        }
      }
      if (++ct % 100000 == 0) {
        logger.debug("    paths = " + ct);
      }
    }
    logger.debug("    total paths = " + ct);

    return paths;
  }

  /**
   * Returns the code with min paths.
   *
   * @param terminology the terminology
   * @return the code with min paths
   * @throws Exception the exception
   */
  public String getCodeWithMinPaths(Terminology terminology) throws Exception {
    final Map<String, Set<String>> paths = getPathsMap(terminology);
    int min = 100000;
    String code = null;
    for (final Map.Entry<String, Set<String>> entry : paths.entrySet()) {
      if (entry.getValue().size() < min) {
        min = entry.getValue().size();
        code = entry.getKey();
      }
    }
    return code;
  }

  /**
   * Returns the code with max paths.
   *
   * @param terminology the terminology
   * @return the code with max paths
   * @throws Exception the exception
   */
  public String getCodeWithMaxPaths(Terminology terminology) throws Exception {
    final Map<String, Set<String>> paths = getPathsMap(terminology);
    int max = 0;
    String code = null;
    for (final Map.Entry<String, Set<String>> entry : paths.entrySet()) {
      if (entry.getValue().size() > max) {
        max = entry.getValue().size();
        code = entry.getKey();
      }
    }
    return code;
  }

  /**
   * Returns the code with max children.
   *
   * @param terminology the terminology
   * @return the code with max children
   * @throws Exception the exception
   */
  public String getCodeWithMaxChildren(Terminology terminology) throws Exception {
    int max = 0;
    String code = null;
    for (final Map.Entry<String, List<String>> entry : parent2child.entrySet()) {
      if (entry.getValue().size() > max) {
        max = entry.getValue().size();
        code = entry.getKey();
      }
    }
    return code;
  }

  /**
   * Returns the paths.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the paths
   * @throws Exception the exception
   */
  public Paths getPaths(Terminology terminology, String code) throws Exception {
    // Handle things without paths
    if (!getPathsMap(terminology).containsKey(code)) {
      return null;
    }

    final Paths paths = new Paths();

    // Sort in code-path order
    for (final String pathstr : getPathsMap(terminology).get(code).stream().sorted()
        .collect(Collectors.toList())) {
      final Path path = new Path();
      path.setDirection(1);
      int level = 0;
      final String[] parts = pathstr.split("\\|");
      for (int i = parts.length - 1; i >= 0; i--) {
        final ConceptMinimal concept = new ConceptMinimal();
        concept.setCode(parts[i]);
        concept.setName(code2label.get(parts[i]));
        concept.setLevel(level++);
        path.getConcepts().add(concept);
      }
      paths.getPaths().add(path);
    }
    return paths;
  }

  /**
   * Returns the paths map.
   *
   * @param terminology the terminology
   * @param codes the codes
   * @return the paths map
   * @throws Exception the exception
   */
  public Map<String, Paths> getPathsMap(Terminology terminology, List<String> codes)
    throws Exception {
    final Map<String, Paths> map = new HashMap<>();
    for (final String code : codes) {
      map.put(code, getPaths(terminology, code));
    }
    return map;
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

  /**
   * Indicates whether or not leaf is the case.
   *
   * @param code the code
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isLeaf(final String code) {
    return parent2child.get(code) == null;
  }

  /**
   * Returns the roles.
   *
   * @return the roles
   */
  public Map<String, List<Role>> getRoleMap() {
    return roleMap;
  }

  /**
   * Sets the roles.
   *
   * @param roleMap the roles
   */
  public void setRoleMap(Map<String, List<Role>> roleMap) {
    this.roleMap = roleMap;
  }

  /**
   * Returns the inverse roles.
   *
   * @return the inverse roles
   */
  public Map<String, List<Role>> getInverseRoleMap() {
    return inverseRoleMap;
  }

  /**
   * Sets the inverse roles.
   *
   * @param inverseRoleMap the inverse roles
   */
  public void setInverseRoleMap(Map<String, List<Role>> inverseRoleMap) {
    this.inverseRoleMap = inverseRoleMap;
  }

  /**
   * Returns the association map.
   *
   * @return the association map
   */
  public Map<String, List<Association>> getAssociationMap() {
    return associationMap;
  }

  /**
   * Sets the association map.
   *
   * @param associationMap the association map
   */
  public void setAssociationMap(Map<String, List<Association>> associationMap) {
    this.associationMap = associationMap;
  }

  /**
   * Returns the inverse association map.
   *
   * @return the inverse association map
   */
  public Map<String, List<Association>> getInverseAssociationMap() {
    return inverseAssociationMap;
  }

  /**
   * Sets the inverse association map.
   *
   * @param inverseAssociationMap the inverse association map
   */
  public void setInverseAssociationMap(Map<String, List<Association>> inverseAssociationMap) {
    this.inverseAssociationMap = inverseAssociationMap;
  }
}
