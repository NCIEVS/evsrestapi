package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

/** A machine-readable representation of an OWL equivalent-class logical definition. */
@Schema(description = "Machine-readable OWL equivalent-class logical definition")
@JsonInclude(Include.NON_NULL)
public class LogicalDefinition {

  private String code;
  private String label;
  private List<Parent> parents = new ArrayList<>();
  private List<Element> elements = new ArrayList<>();

  public String getCode() {
    return code;
  }

  public void setCode(final String code) {
    this.code = code;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  public List<Parent> getParents() {
    return parents;
  }

  public void setParents(final List<Parent> parents) {
    this.parents = parents;
  }

  public List<Element> getElements() {
    return elements;
  }

  public void setElements(final List<Element> elements) {
    this.elements = elements;
  }

  /** A named class in the top-level equivalent-class intersection. */
  @JsonInclude(Include.NON_NULL)
  public static class Parent {
    private Integer idx;
    private String code;
    private String label;

    public Parent() {
      // n/a
    }

    public Parent(final String code, final String label) {
      this.code = code;
      this.label = label;
    }

    public Integer getIdx() {
      return idx;
    }

    public void setIdx(final Integer idx) {
      this.idx = idx;
    }

    public String getCode() {
      return code;
    }

    public void setCode(final String code) {
      this.code = code;
    }

    public String getLabel() {
      return label;
    }

    public void setLabel(final String label) {
      this.label = label;
    }
  }

  /** Logical-definition content sharing a role range. */
  @JsonInclude(Include.NON_NULL)
  public static class Element {
    private String range;
    private String rangeCode;
    private String rangeUri;
    private List<Restriction> roles = new ArrayList<>();
    private List<RoleUnion> roleUnions = new ArrayList<>();
    private List<RoleGroup> roleGroups = new ArrayList<>();

    public String getRange() {
      return range;
    }

    public void setRange(final String range) {
      this.range = range;
    }

    public String getRangeCode() {
      return rangeCode;
    }

    public void setRangeCode(final String rangeCode) {
      this.rangeCode = rangeCode;
    }

    public String getRangeUri() {
      return rangeUri;
    }

    public void setRangeUri(final String rangeUri) {
      this.rangeUri = rangeUri;
    }

    public List<Restriction> getRoles() {
      return roles;
    }

    public void setRoles(final List<Restriction> roles) {
      this.roles = roles;
    }

    public List<RoleUnion> getRoleUnions() {
      return roleUnions;
    }

    public void setRoleUnions(final List<RoleUnion> roleUnions) {
      this.roleUnions = roleUnions;
    }

    public List<RoleGroup> getRoleGroups() {
      return roleGroups;
    }

    public void setRoleGroups(final List<RoleGroup> roleGroups) {
      this.roleGroups = roleGroups;
    }
  }

  /** An OWL some-values-from restriction. */
  @JsonInclude(Include.NON_NULL)
  public static class Restriction {
    private String sourceCode;
    private String sourceLabel;
    private String roleCode;
    private String roleLabel;
    private String targetCode;
    private String targetLabel;
    private String range;
    private String rangeCode;
    private String rangeUri;

    public String getSourceCode() {
      return sourceCode;
    }

    public void setSourceCode(final String sourceCode) {
      this.sourceCode = sourceCode;
    }

    public String getSourceLabel() {
      return sourceLabel;
    }

    public void setSourceLabel(final String sourceLabel) {
      this.sourceLabel = sourceLabel;
    }

    public String getRoleCode() {
      return roleCode;
    }

    public void setRoleCode(final String roleCode) {
      this.roleCode = roleCode;
    }

    public String getRoleLabel() {
      return roleLabel;
    }

    public void setRoleLabel(final String roleLabel) {
      this.roleLabel = roleLabel;
    }

    public String getTargetCode() {
      return targetCode;
    }

    public void setTargetCode(final String targetCode) {
      this.targetCode = targetCode;
    }

    public String getTargetLabel() {
      return targetLabel;
    }

    public void setTargetLabel(final String targetLabel) {
      this.targetLabel = targetLabel;
    }

    public String getRange() {
      return range;
    }

    public void setRange(final String range) {
      this.range = range;
    }

    public String getRangeCode() {
      return rangeCode;
    }

    public void setRangeCode(final String rangeCode) {
      this.rangeCode = rangeCode;
    }

    public String getRangeUri() {
      return rangeUri;
    }

    public void setRangeUri(final String rangeUri) {
      this.rangeUri = rangeUri;
    }
  }

  /** An OR expression whose alternatives are individual restrictions. */
  @JsonInclude(Include.NON_NULL)
  public static class RoleUnion {
    private List<Restriction> roles = new ArrayList<>();

    public List<Restriction> getRoles() {
      return roles;
    }

    public void setRoles(final List<Restriction> roles) {
      this.roles = roles;
    }
  }

  /** An OR expression whose alternatives are AND sets of restrictions. */
  @JsonInclude(Include.NON_NULL)
  public static class RoleGroup {
    private List<RoleSet> roleSets = new ArrayList<>();

    public List<RoleSet> getRoleSets() {
      return roleSets;
    }

    public void setRoleSets(final List<RoleSet> roleSets) {
      this.roleSets = roleSets;
    }
  }

  /** An AND set within a role group. */
  @JsonInclude(Include.NON_NULL)
  public static class RoleSet {
    private List<Restriction> roles = new ArrayList<>();

    public List<Restriction> getRoles() {
      return roles;
    }

    public void setRoles(final List<Restriction> roles) {
      this.roles = roles;
    }
  }
}
