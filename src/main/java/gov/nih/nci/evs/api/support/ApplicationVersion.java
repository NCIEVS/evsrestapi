package gov.nih.nci.evs.api.support;

/** Represents HomePageData. */
public class ApplicationVersion {

  /** The name. */
  private String name;

  /** The version. */
  private String version;

  /** The description. */
  private String description;

  /**
   * Returns the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Returns the version.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version.
   *
   * @param version the version
   */
  public void setVersion(final String version) {
    this.version = version;
  }

  /**
   * Returns the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the description
   */
  public void setDescription(final String description) {
    this.description = description;
  }
}
