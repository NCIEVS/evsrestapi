
package gov.nih.nci.evs.api.model;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

/**
 * Represents a synonym of a concept.
 */
@JsonInclude(Include.NON_EMPTY)
public class History extends BaseModel implements Comparable<History> {

  /** The concept code. */
  @Field(type = FieldType.Keyword)
  private String code;
  
  /** The concept name. */
  @Field(type = FieldType.Keyword)
  private String conceptName;

  /** The action. */
  @Field(type = FieldType.Keyword)
  private String action;
  
  /** The date. */
  @Field(type = FieldType.Keyword)
  private String date;

  /** The code of the replacement concept. */
  @Field(type = FieldType.Keyword)
  private String replacementCode;

  /** The name of the replacement concept. */
  @Field(type = FieldType.Text)
  private String replacementName;
  
  
  /**
   * Instantiates an empty {@link History}.
   */
  public History() {
    // n/a
  }

  /**
   * Instantiates a {@link History} from the specified parameters.
   *
   * @param other the other
   */
  public History(final History other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final History other) {
    super.populateFrom(other);
    code = other.getCode();
    conceptName = other.getConceptName();
    action = other.getAction();
    date = other.getDate();
    replacementCode = other.getReplacementCode();
    replacementName = other.getReplacementName();
  }

  /**
   * Returns the action.
   *
   * @return the action
   */
  public String getAction() {
    return action;
  }

  /**
   * Sets the action.
   *
   * @param action the action
   */
  public void setAction(final String action) {
    this.action = action;
  }

  /**
   * Returns the code.
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Sets the code.
   *
   * @param code the code
   */
  public void setCode(final String code) {
    this.code = code;
  }
  
  /**
   * Returns the concept name.
   *
   * @return the concept name
   */
  public String getConceptName() {
    return conceptName;
  }

  /**
   * Sets the concept name.
   *
   * @param conceptName the concept name
   */
  public void setConceptName(final String conceptName) {
    this.conceptName = conceptName;
  }

  /**
   * Returns the date.
   *
   * @return the date
   */
  public String getDate() {
    return date;
  }

  /**
   * Sets the date.
   *
   * @param date the date
   */
  public void setDate(final String date) {
    this.date = date;
  }
  
  /**
   * Returns the code of the replacement concept.
   *
   * @return the code of the replacement concept
   */
  public String getReplacementCode() {
    return replacementCode;
  }

  /**
   * Sets the code of the replacement concept.
   *
   * @param replacementCode the code of the replacement concept
   */
  public void setReplacementCode(final String replacementCode) {
    this.replacementCode = replacementCode;
  }
  
  /**
   * Returns the name of the replacement concept.
   *
   * @return the name of the replacement concept
   */
  public String getReplacementName() {
    return replacementName;
  }

  /**
   * Sets the name of the replacement concept.
   *
   * @param replacementName the name of the replacement concept
   */
  public void setReplacementName(final String replacementName) {
    this.replacementName = replacementName;
  }

  /**
   * Hash code.
   *
   * @return the int
   */
  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((code == null) ? 0 : code.hashCode());
    result = prime * result + ((conceptName == null) ? 0 : conceptName.hashCode());
    result = prime * result + ((action == null) ? 0 : action.hashCode());
    result = prime * result + ((date == null) ? 0 : date.hashCode());
    result = prime * result + ((replacementCode == null) ? 0 : replacementCode.hashCode());
    result = prime * result + ((replacementName == null) ? 0 : replacementName.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
   */
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
    
    final History other = (History) obj;
    
    if (code == null) {
      if (other.code != null) {
        return false;
      }
    } else if (!code.equals(other.code)) {
      return false;
    }

    if (conceptName == null) {
      if (other.conceptName != null) {
        return false;
      }
    } else if (!conceptName.equals(other.conceptName)) {
      return false;
    }

    if (action == null) {
      if (other.action != null) {
        return false;
      }
    } else if (!action.equals(other.action)) {
      return false;
    }

    if (date == null) {
      if (other.date != null) {
        return false;
      }
    } else if (!date.equals(other.date)) {
      return false;
    }

    if (replacementCode == null) {
      if (other.replacementCode != null) {
        return false;
      }
    } else if (!replacementCode.equals(other.replacementCode)) {
      return false;
    }

    if (replacementName == null) {
      if (other.replacementName != null) {
        return false;
      }
    } else if (!replacementName.equals(other.replacementName)) {
      return false;
    }
    return true;
  }

  /* see superclass */
  @Override
  public int compareTo(History o) {
    return (code + action + date + replacementCode).compareTo(o.getCode() + o.getAction() + o.getDate() + o.getReplacementCode());
  }

}
