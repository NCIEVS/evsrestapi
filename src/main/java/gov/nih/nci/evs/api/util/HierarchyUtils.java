
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.Terminology;

/**
 * Hierarchy utilities.
 */
public class HierarchyUtils {

  private static final Logger logger = LoggerFactory.getLogger(HierarchyUtils.class);
  
  /** The parent 2 child. */
//  @Field(type = FieldType.Object)
  @Transient
  private HashMap<String, ArrayList<String>> parent2child =
      new HashMap<String, ArrayList<String>>();

  /** The child 2 parent. */
//  @Field(type = FieldType.Object)
  @Transient
  private HashMap<String, ArrayList<String>> child2parent =
      new HashMap<String, ArrayList<String>>();

  /** The code 2 label. */
//  @Field(type = FieldType.Text)
  @Transient
  private HashMap<String, String> code2label = new HashMap<String, String>();

  /** The label 2 code. */
  @Transient
  private HashMap<String, String> label2code = new HashMap<String, String>();

  /** The concepts. */
  @Transient
  private HashSet<String> concepts = new HashSet<String>();

  /** The parents. */
  @Transient
  private HashSet<String> parents = new HashSet<String>();

  /** The children. */
  @Transient
  private HashSet<String> children = new HashSet<String>();

  /** The roots. */
  @Field(type = FieldType.Object)
  private HashSet<String> hierarchyRoots = null;

  /** The leaves. */
  @Field(type = FieldType.Object)
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

    hierarchyRoots = new HashSet<String>(parents);
    hierarchyRoots.removeAll(children);

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
//  public ArrayList<String> getTransitiveClosure(ArrayList<String> concepts, String code,
//    Integer level) {
//    ArrayList<String> children = this.parent2child.get(code);
//    if (children == null || children.size() == 0) {
//      return concepts;
//    }
//    for (String child : children) {
//      String indent = "";
//      for (int i = 0; i < level; i++) {
//        indent = indent + "    ";
//      }
//      System.out.println(indent + "Parent: " + code + ": " + code2label.get(code) + "  Child: "
//          + child + ": " + code2label.get(child) + "  Level: " + level);
//      // ArrayList<String> newChildren =
//      getTransitiveClosure(concepts, child, level + 1);
//    }
//    return concepts;
//  }

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
   * @param parent the parent
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
	    if (c1.getLevel() == c2.getLevel()) { return c1.getName().compareTo(c2.getName()); }
	    else { return c1.getLevel() - c2.getLevel(); }
	}});
  return descendants;
  }
  
  /**
   * Descendant nodes helper.
   *
   * @param concept the concept
   * @param descendantMap the descendant map
   * @param level the current level
   * @param term the terminology
   * @return void
   */
  public void getDescendantMapLevel(String code, Map<String, Concept> descendantMap, int level) {
	List<String> children = parent2child.get(code);
	if (children == null || children.size() == 0) {
		return;
    }
	for (String child : children) {
      if(descendantMap.get(child) == null) {
    	  Concept conc = new Concept(child);
    	  if(parent2child.containsKey(child))
    		  conc.setLeaf(false);
    	  else
    		  conc.setLeaf(true);
          conc.setLevel(level);
          conc.setName(code2label.get(child));
    	  descendantMap.put(child, conc);
      }
      getDescendantMapLevel(child, descendantMap, level+1);
    }
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
  
}
