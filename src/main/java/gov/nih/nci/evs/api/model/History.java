
package gov.nih.nci.evs.api.model;

import java.util.Objects;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a synonym of a concept.
 */
@Schema(description = "Represents a history record, generally for a retired concept")
@JsonInclude(Include.NON_EMPTY)
public class History extends BaseModel implements Comparable<History> {

  /** The concept code. */
  @Field(type = FieldType.Keyword)
  private String code;

  /** The concept name. */
  @Field(type = FieldType.Keyword)
  private String name;

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
    name = other.getName();
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
  @Schema(description = "Indicates the history action, e.g. 'merge', 'active', 'retire', 'SY', 'RB', etc.")
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
  @Schema(description = "Code for this history record")
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
  @Schema(description = "Last known preferred name for the code")
  public String getName() {
    return name;
  }

  /**
   * Sets the concept name.
   *
   * @param name the concept name
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Returns the date.
   *
   * @return the date
   */
  @Schema(description = "Date of the history record")
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
  @Schema(description = "Code replacing this code")
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
  @Schema(description = "Preferred name of the code replacing this code")
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
    return Objects.hash(code, name, action, date, replacementCode, replacementName);
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

    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
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
    return (code + action + date + replacementCode)
        .compareTo(o.getCode() + o.getAction() + o.getDate() + o.getReplacementCode());
  }

}
