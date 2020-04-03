
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import gov.nih.nci.evs.api.model.evs.HierarchyNode;

/**
 * Hierarchy utilities.
 */
public class HierarchyUtils {

  /** The parent 2 child. */
  private HashMap<String, ArrayList<String>> parent2child =
      new HashMap<String, ArrayList<String>>();

  /** The child 2 parent. */
  private HashMap<String, ArrayList<String>> child2parent =
      new HashMap<String, ArrayList<String>>();

  /** The code 2 label. */
  private HashMap<String, String> code2label = new HashMap<String, String>();

  /** The label 2 code. */
  private HashMap<String, String> label2code = new HashMap<String, String>();

  /** The concepts. */
  private HashSet<String> concepts = new HashSet<String>();

  /** The parents. */
  private HashSet<String> parents = new HashSet<String>();

  /** The children. */
  private HashSet<String> children = new HashSet<String>();

  /** The roots. */
  private HashSet<String> roots = null;

  /** The leaves. */
  private HashSet<String> leaves = null;

  /**
   * Instantiates an empty {@link HierarchyUtils}.
   */
  public HierarchyUtils() {
  }

  /**
   * Instantiates a {@link HierarchyUtils} from the specified parameters.
   *
   * @param parentchild the parentchild
   */
  public HierarchyUtils(List<String> parentchild) {
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

    roots = new HashSet<String>(parents);
    roots.removeAll(children);

    leaves = new HashSet<String>(children);
    leaves.removeAll(parents);
    // testLoading();
  }

  /**
   * Returns the transitive closure.
   *
   * @param concepts the concepts
   * @param code the code
   * @param level the level
   * @return the transitive closure
   */
  public ArrayList<String> getTransitiveClosure(ArrayList<String> concepts, String code,
    Integer level) {
    ArrayList<String> children = this.parent2child.get(code);
    if (children == null || children.size() == 0) {
      return concepts;
    }
    for (String child : children) {
      String indent = "";
      for (int i = 0; i < level; i++) {
        indent = indent + "    ";
      }
      System.out.println(indent + "Parent: " + code + ": " + code2label.get(code) + "  Child: "
          + child + ": " + code2label.get(child) + "  Level: " + level);
      // ArrayList<String> newChildren =
      getTransitiveClosure(concepts, child, level + 1);
    }
    return concepts;
  }

  /**
   * Returns the roots.
   *
   * @return the roots
   */
  public ArrayList<String> getRoots() {
    return new ArrayList<String>(this.roots);
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
   * Returns the label.
   *
   * @param code the code
   * @return the label
   */
  public String getLabel(String code) {
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
    for (String code : this.roots) {
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
}
