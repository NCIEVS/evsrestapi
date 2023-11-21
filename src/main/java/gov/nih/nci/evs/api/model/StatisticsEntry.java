
package gov.nih.nci.evs.api.model;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents an etry in a stats table.
 */
@Schema(description = "Represents an entry in a statistics table")
public class StatisticsEntry {
  /** The code. */
  private String code;

  /** The name. */
  private String key;

  /** The terminology. */
  private String value;

  /**
   * Instantiates an empty {@link StatisticsEntry}.
   */
  public StatisticsEntry() {
    // n/a
  }

  /**
   * Instantiates a {@link StatisticsEntry} from the specified parameters.
   *
   * @param other the other
   */
  public StatisticsEntry(StatisticsEntry other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final StatisticsEntry other) {
    code = other.getCode();
    key = other.getKey();
    value = other.getValue();
  }

  /**
   * Instantiates a {@link StatisticsEntry} from the specified parameters.
   *
   * @param code the code
   * @param key the key
   * @param value the value
   */
  public StatisticsEntry(String code, String key, String value) {
    this.code = code;
    this.key = key;
    this.value = value;
  }

  /**
   * Returns the code.
   *
   * @return the code
   */
  @Schema(description = "Code for what this statistic is measuring")
  public String getCode() {
    return code;
  }

  /**
   * Sets the code.
   *
   * @param code the code to set
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Returns the key.
   *
   * @return the key
   */
  @Schema(description = "Key defining an additional element to the statistic")
  public String getKey() {
    return key;
  }

  /**
   * Sets the key.
   *
   * @param key the key to set
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Returns the value.
   *
   * @return the value
   */
  @Schema(description = "Value of the statistic")
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param value the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    return Objects.hash(code, key, value);
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    StatisticsEntry other = (StatisticsEntry) obj;
    return Objects.equals(code, other.code) && Objects.equals(key, other.key) && Objects.equals(value, other.value);
  }

}
