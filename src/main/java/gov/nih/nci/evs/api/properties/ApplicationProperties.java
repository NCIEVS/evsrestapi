
package gov.nih.nci.evs.api.properties;

/**
 * Application properties.
 */
public class ApplicationProperties {

  /** The admin key. */
  private String adminKey;
  
  /** The context path. */
  private String contextPath;

  /** The metric log switch. */
  private Boolean metricsEnabled;

  /** The metrics dir */
  private String metricsDir;

  /**
   * Returns the admin key.
   *
   * @return the admin key
   */
  public String getAdminKey() {
    return adminKey;
  }

  /**
   * Sets the admin key.
   *
   * @param adminKey the admin key
   */
  public void setAdminKey(String adminKey) {
    this.adminKey = adminKey;
  }
  
  /**
   * Returns the context path.
   *
   * @return the context path
   */
  public String getContextPath() {
    return contextPath;
  }

  /**
   * Sets the context path.
   *
   * @param contextPath the context path
   */
  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  /**
   * Returns the metric log switch.
   *
   * @return the metric log switch
   */
  public Boolean getMetricsEnabled() {
    return metricsEnabled;
  }

  /**
   * Sets the metric log directory.
   *
   * @param metricsEnabled the metric log directory
   */
  public void setMetricsEnabled(Boolean metricsEnabled) {
    this.metricsEnabled = metricsEnabled;
  }

  /**
   * Returns the metric Dir.
   *
   * @return the metric Dir
   */
  public String getMetricsDir() {
    return metricsDir;
  }

  /**
   * Sets the metric directory.
   *
   * @param metricsDir the metric directory
   */
  public void setMetricsDir(String metricsDir) {
    this.metricsDir = metricsDir;
  }

}
