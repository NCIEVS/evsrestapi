
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
  private String about;

  /**
   * Instantiates an empty {@link BaseModel}.
   */
  public BaseModel() {
    // n/a
  }

  /**
   * Returns the about.
   *
   * @return the about
   */
  public String getAbout() {
    return about;
  }

  /**
   * Sets the about.
   *
   * @param about the about
   */
  public void setAbout(final String about) {
    this.about = about;
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
