
package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a map to a concept in another terminology.
 */
@JsonInclude(Include.NON_EMPTY)
public class Map extends BaseModel implements Comparable<Map> {

  /** The source. */
  private String source;

  /** The source name. */
  private String sourceName;

  /** The source term type. */
  private String sourceTermType;

  /** The source code. */
  private String sourceCode;

  /** The source terminology. */
  private String sourceTerminology;

  /** The type. */
  private String type;

  /** The rank. */
  private String rank;

  /** The group. */
  private String group;

  /** The rank. */
  private String rank;

  /** The rule. */
  private String rule;

  /** The target name. */
  private String targetName;

  /** The target term type. */
  private String targetTermType;

  /** The target code. */
  private String targetCode;

  /** The target terminology. */
  private String targetTerminology;

  /** The target terminology version. */
  private String targetTerminologyVersion;

  /**
   * Instantiates an empty {@link Map}.
   */
  public Map() {
    // n/a
  }

  /**
   * Instantiates a {@link Map} from the specified parameters.
   *
   * @param other the other
   */
  public Map(final Map other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Map other) {
    super.populateFrom(other);
    type = other.getType();
    group = other.getGroup();
    rank = other.getRank();
    rule = other.getRule();
    targetName = other.getTargetName();
    targetTermType = other.getTargetTermType();
    sourceCode = other.getSourceCode();
    sourceTerminology = other.getSourceTerminology();
    targetCode = other.getTargetCode();
    targetTerminology = other.getTargetTerminology();
    targetTerminologyVersion = other.getTargetTerminologyVersion();
  }

  /**
   * @return the source
   */
  public String getSource() {
    return source;
  }

  /**
   * @param source the source to set
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * @return the sourceName
   */
  public String getSourceName() {
    return sourceName;
  }

  /**
   * @param sourceName the sourceName to set
   */
  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  /**
   * @return the sourceTermType
   */
  public String getSourceTermType() {
    return sourceTermType;
  }

  /**
   * @param sourceTermType the sourceTermType to set
   */
  public void setSourceTermType(String sourceTermType) {
    this.sourceTermType = sourceTermType;
  }

  /**
   * Returns the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the type
   */
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * Returns the group.
   *
   * @return the group
   */
  public String getGroup() {
    return group;
  }

  /**
   * Sets the group.
   *
   * @param group the group
   */
  public void setGroup(final String group) {
    this.group = group;
  }

  /**
   * Returns the rank.
   *
   * @return the rank
   */
  public String getRank() {
    return rank;
  }

  /**
   * Sets the rank.
   *
   * @param rank the rank
   */
  public void setRank(final String rank) {
    this.rank = rank;
  }

  /**
   * Returns the rule.
   *
   * @return the rule
   */
  public String getRule() {
    return rule;
  }

  /**
   * Sets the rule.
   *
   * @param rule the rule
   */
  public void setRule(final String rule) {
    this.rule = rule;
  }

  /**
   * Returns the target name.
   *
   * @return the target name
   */
  public String getTargetName() {
    return targetName;
  }

  /**
   * Sets the target name.
   *
   * @param targetName the target name
   */
  public void setTargetName(final String targetName) {
    this.targetName = targetName;
  }

  /**
   * Returns the target term type.
   *
   * @return the target term type
   */
  public String getTargetTermType() {
    return targetTermType;
  }

  /**
   * Sets the target term type.
   *
   * @param targetTermType the target term type
   */
  public void setTargetTermType(final String targetTermType) {
    this.targetTermType = targetTermType;
  }

  /**
   * Sets the target term group. This is a bridge to support naming convention normalization.
   *
   * @param targetTermGroup the target term group
   */
  public void setTargetTermGroup(final String targetTermGroup) {
    this.targetTermType = targetTermGroup;
  }

  /**
   * Returns the target code.
   *
   * @return the target code
   */
  public String getTargetCode() {
    return targetCode;
  }

  /**
   * Sets the target code.
   *
   * @param targetCode the target code
   */
  public void setTargetCode(final String targetCode) {
    this.targetCode = targetCode;
  }

  /**
   * Returns the source code.
   *
   * @return the source code
   */
  public String getSourceCode() {
    return sourceCode;
  }

  /**
   * Sets the source code.
   *
   * @param sourceCode the source code
   */
  public void setSourceCode(final String sourceCode) {
    this.sourceCode = sourceCode;
  }

  /**
   * Returns the source terminology.
   *
   * @return the source terminology
   */
  public String getSourceTerminology() {
    return sourceTerminology;
  }

  /**
   * Sets the source terminology.
   *
   * @param sourceTerminology the source terminology
   */
  public void setSourceTerminology(final String sourceTerminology) {
    this.sourceTerminology = sourceTerminology;
  }

  /**
   * Returns the target terminology.
   *
   * @return the target terminology
   */
  public String getTargetTerminology() {
    return targetTerminology;
  }

  /**
   * Sets the target terminology.
   *
   * @param targetTerminology the target terminology
   */
  public void setTargetTerminology(final String targetTerminology) {
    this.targetTerminology = targetTerminology;
  }

  /**
   * Returns the target terminology version.
   *
   * @return the target terminology version
   */
  public String getTargetTerminologyVersion() {
    return targetTerminologyVersion;
  }

  /**
   * Sets the target terminology version.
   *
   * @param targetTerminologyVersion the target terminology version
   */
  public void setTargetTerminologyVersion(final String targetTerminologyVersion) {
    this.targetTerminologyVersion = targetTerminologyVersion;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((sourceCode == null) ? 0 : sourceCode.hashCode());
    result = prime * result + ((sourceTerminology == null) ? 0 : sourceTerminology.hashCode());
    result = prime * result + ((targetCode == null) ? 0 : targetCode.hashCode());
    result = prime * result + ((targetName == null) ? 0 : targetName.hashCode());
    result = prime * result + ((targetTermType == null) ? 0 : targetTermType.hashCode());
    result = prime * result + ((targetTerminology == null) ? 0 : targetTerminology.hashCode());
    result = prime * result + ((targetTerminologyVersion == null) ? 0 : targetTerminologyVersion.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((group == null) ? 0 : group.hashCode());
    result = prime * result + ((rank == null) ? 0 : rank.hashCode());
    result = prime * result + ((rule == null) ? 0 : rule.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Map other = (Map) obj;
    if (sourceCode == null) {
      if (other.sourceCode != null) {
        return false;
      }
    } else if (!sourceCode.equals(other.sourceCode)) {
      return false;
    }
    if (sourceTerminology == null) {
      if (other.sourceTerminology != null) {
        return false;
      }
    } else if (!sourceTerminology.equals(other.sourceTerminology)) {
      return false;
    }
    if (targetCode == null) {
      if (other.targetCode != null) {
        return false;
      }
    } else if (!targetCode.equals(other.targetCode)) {
      return false;
    }
    if (targetName == null) {
      if (other.targetName != null) {
        return false;
      }
    } else if (!targetName.equals(other.targetName)) {
      return false;
    }
    if (targetTermType == null) {
      if (other.targetTermType != null) {
        return false;
      }
    } else if (!targetTermType.equals(other.targetTermType)) {
      return false;
    }
    if (targetTerminology == null) {
      if (other.targetTerminology != null) {
        return false;
      }
    } else if (!targetTerminology.equals(other.targetTerminology)) {
      return false;
    }
    if (targetTerminologyVersion == null) {
      if (other.targetTerminologyVersion != null) {
        return false;
      }
    } else if (!targetTerminologyVersion.equals(other.targetTerminologyVersion)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    if (group == null) {
      if (other.group != null) {
        return false;
      }
    } else if (!group.equals(other.group)) {
      return false;
    }
    if (rank == null) {
      if (other.rank != null) {
        return false;
      }
    } else if (!rank.equals(other.rank)) {
      return false;
    }
    if (rule == null) {
      if (other.rule != null) {
        return false;
      }
    } else if (!rule.equals(other.rule)) {
      return false;
    }
    return true;
  }

  /* see superclass */
  @Override
  public int compareTo(Map o) {
    return (sourceCode + sourceTerminology + group + rank + targetName + targetCode)
        .compareToIgnoreCase(o.getSourceCode() + o.getSourceTerminology() + o.getGroup() + o.getRank()
            + o.getTargetName() + o.getTargetCode());
  }

}
