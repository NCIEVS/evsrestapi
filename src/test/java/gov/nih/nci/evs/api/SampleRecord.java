package gov.nih.nci.evs.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import gov.nih.nci.evs.api.model.BaseModel;

/** The Class SampleRecord. */
@JsonInclude(Include.NON_EMPTY)
public class SampleRecord extends BaseModel {

  /** The uri. */
  private String uri;

  /** The code. */
  private String code;

  /** The name. */
  private String key;

  /** The terminology. */
  private String value;

  /** Instantiates an empty {@link SampleRecord}. */
  public SampleRecord() {
    // n/a
  }

  /**
   * Instantiates a {@link SampleRecord} from the specified parameters.
   *
   * @param uri the uri
   * @param code the code
   * @param key the key
   * @param value the value
   */
  public SampleRecord(final String uri, final String code, final String key, final String value) {
    this.uri = uri;
    this.code = code;
    this.key = key;
    this.value = value;
  }

  /**
   * Returns the uri.
   *
   * @return the uri
   */
  @Override
  public String getUri() {
    return uri;
  }

  /**
   * Sets the uri.
   *
   * @param uri the uri to set
   */
  @Override
  public void setUri(String uri) {
    this.uri = uri;
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
}
