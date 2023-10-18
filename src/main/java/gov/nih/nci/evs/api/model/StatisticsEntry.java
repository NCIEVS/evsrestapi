package gov.nih.nci.evs.api.model;

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
   * @param name the name
   */
  public StatisticsEntry(String code, String key, String value) {
    this.code = code;
    this.key = key;
    this.value = value;
  }

  /**
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * @param code the code to set
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * @param key the key to set
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * @param value the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }
}
