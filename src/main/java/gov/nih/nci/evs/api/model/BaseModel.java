
package gov.nih.nci.evs.api.model;

import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the base superclass for other model objects.
 */
public class BaseModel {

  /**
   * The rdf:about - used for situations where there is not a defined code
   * property.
   */
  @Transient
  private String uri;

  /**
   * Instantiates an empty {@link BaseModel}.
   */
  public BaseModel() {
    // n/a
  }

  /**
   * Returns the uri.
   *
   * @return the uri
   */
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
