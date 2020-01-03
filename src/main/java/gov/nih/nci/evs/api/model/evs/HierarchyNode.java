package gov.nih.nci.evs.api.model.evs;

import java.util.ArrayList;
import java.util.List;

public class HierarchyNode {
  private String code;

  private String label;

  private Integer level;

  private Boolean leaf;

  private Boolean expanded;

  private Boolean highlight;

  private List<HierarchyNode> children = new ArrayList<HierarchyNode>(0);

  public HierarchyNode() {
  };

  public HierarchyNode(String code, String label, Boolean leaf) {
    this.code = code;
    this.label = label;
    this.leaf = leaf;
  };

  public HierarchyNode(String code, String label, List<HierarchyNode> children,
      Boolean leaf) {
    this.code = code;
    this.label = label;
    this.leaf = leaf;
    this.children = children;
  };

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Boolean getLeaf() {
    return leaf;
  }

  public void setLeaf(Boolean leaf) {
    this.leaf = leaf;
  }

  public Integer getLevel() {
    return level;
  }

  public void setLevel(Integer level) {
    this.level = level;
  }

  public Boolean getExpanded() {
    return expanded;
  }

  public void setExpanded(Boolean expanded) {
    this.expanded = expanded;
  }

  public Boolean getHighlight() {
    return highlight;
  }

  public void setHighlight(Boolean highlight) {
    this.highlight = highlight;
  }

  public List<HierarchyNode> getChildren() {
    return children;
  }

  public void setChildren(List<HierarchyNode> children) {
    this.children = children;
  }
}