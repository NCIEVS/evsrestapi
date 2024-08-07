package gov.nih.nci.evs.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in a hierarchy and is directly compatible with the primeng "TreeNode"
 * structure.
 */
@Schema(description = "Represents a node in a subtree rendering of the hierarchy")
public class HierarchyNode extends BaseModel {

  /** The code. */
  private String code;

  /** The label. */
  private String label;

  /** The level. */
  private Integer level;

  /** The leaf. */
  private Boolean leaf;

  /** The expanded. */
  private Boolean expanded;

  /** The highlight. */
  private Boolean highlight;

  /** The children. */
  private List<HierarchyNode> children = null;

  /** Instantiates an empty {@link HierarchyNode}. */
  public HierarchyNode() {
    // n/a
  }

  /**
   * Instantiates a {@link HierarchyNode} from the specified parameters.
   *
   * @param code the code
   * @param label the label
   * @param leaf the leaf
   */
  public HierarchyNode(String code, String label, Boolean leaf) {
    this.code = code;
    this.label = label;
    this.leaf = leaf;
  }

  /**
   * Instantiates a {@link HierarchyNode} from the specified parameters.
   *
   * @param code the code
   * @param label the label
   * @param children the children
   * @param leaf the leaf
   */
  public HierarchyNode(String code, String label, List<HierarchyNode> children, Boolean leaf) {
    this.code = code;
    this.label = label;
    this.leaf = leaf;
    this.children = children;
  }

  /**
   * Returns the code.
   *
   * @return the code
   */
  @Schema(description = "Code of the hierarchy node")
  public String getCode() {
    return code;
  }

  /**
   * Sets the code.
   *
   * @param code the code
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Returns the label.
   *
   * @return the label
   */
  @Schema(description = "Code label for the hierarchy node")
  public String getLabel() {
    return label;
  }

  /**
   * Sets the label.
   *
   * @param label the label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Returns the leaf.
   *
   * @return the leaf
   */
  @Schema(description = "Indicates whether the code has children")
  public Boolean getLeaf() {
    return leaf;
  }

  /**
   * Sets the leaf.
   *
   * @param leaf the leaf
   */
  public void setLeaf(Boolean leaf) {
    this.leaf = leaf;
  }

  /**
   * Returns the level.
   *
   * @return the level
   */
  @Schema(description = "Indicates level of depth in the (respective) hierarchy")
  public Integer getLevel() {
    return level;
  }

  /**
   * Sets the level.
   *
   * @param level the level
   */
  public void setLevel(Integer level) {
    this.level = level;
  }

  /**
   * Returns the expanded.
   *
   * @return the expanded
   */
  @Schema(description = "Indicates whether the node has been expanded")
  public Boolean getExpanded() {
    return expanded;
  }

  /**
   * Sets the expanded.
   *
   * @param expanded the expanded
   */
  public void setExpanded(Boolean expanded) {
    this.expanded = expanded;
  }

  /**
   * Returns the highlight.
   *
   * @return the highlight
   */
  @Schema(hidden = true)
  public Boolean getHighlight() {
    return highlight;
  }

  /**
   * Sets the highlight.
   *
   * @param highlight the highlight
   */
  public void setHighlight(Boolean highlight) {
    this.highlight = highlight;
  }

  /**
   * Returns the children.
   *
   * @return the children
   */
  @Schema(description = "Child nodes")
  public List<HierarchyNode> getChildren() {
    if (children == null) {
      children = new ArrayList<>();
    }
    return children;
  }

  /**
   * Sets the children.
   *
   * @param children the children
   */
  public void setChildren(List<HierarchyNode> children) {
    this.children = children;
  }
}
