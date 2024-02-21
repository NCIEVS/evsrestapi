
package gov.nih.nci.evs.api.model;

import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the base superclass for other model objects.
 */
public class BaseModel {

  /**
   * The rdf:about - used for situations where there is not a defined code property.
   */
  @Transient
  private String uri;

  /** The ct - only used for limit calls. */
  private Integer ct;

  /**
   * Instantiates an empty {@link BaseModel}.
   */
  public BaseModel() {
    // n/a
  }

  /**
   * Instantiates a {@link BaseModel} from the specified parameters.
   *
   * @param other the other
   */
  public BaseModel(final BaseModel other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final BaseModel other) {
    uri = other.getUri();
    ct = other.getCt();
  }

  /**
   * Returns the uri.
   *
   * @return the uri
   */
  @Schema(description = "URI for this element in an rdf-based source file")
  public String getUri() {
    return uri;
  }

  /**
   * Sets the uri.
   *
   * @param uri the uri
   */
  public void setUri(final String uri) {
    this.uri = uri;
  }

  /**
   * Returns the ct.
   *
   * @return the ct
   */
  @Schema(description = "Used to indicate the total amount of data in cases where a limit is being applied")
  public Integer getCt() {
    return ct;
  }

  /**
   * Sets the ct.
   *
   * @param ct the ct
   */
  public void setCt(Integer ct) {
    this.ct = ct;
  }

  /* see superclass */
  @Override
  public String toString() {
    try {
      return new ObjectMapper().writeValueAsString(this);
    } catch (final Exception e) {
      return e.getMessage();
    }
  }

}
