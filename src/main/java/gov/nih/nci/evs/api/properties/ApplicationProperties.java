
package gov.nih.nci.evs.api.properties;

/**
 * Application properties.
 */
public class ApplicationProperties {

  /** The context path. */
  private String contextPath;

  /** The metric log switch. */
  private Boolean metricsEnabled;

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

}
