
package gov.nih.nci.evs.api.properties;

/**
 * Application properties.
 */
public class ApplicationProperties {

  /** The context path. */
  private String contextPath;

  /** The generated file path. */
  private String generatedFilePath;

  /** The force file generation. */
  private Boolean forceFileGeneration;

  /** The metric log directory. */
  private String metricLogDirectory;

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
   * Returns the generated file path.
   *
   * @return the generated file path
   */
  public String getGeneratedFilePath() {
    return generatedFilePath;
  }

  /**
   * Sets the generated file path.
   *
   * @param generatedFilePath the generated file path
   */
  public void setGeneratedFilePath(String generatedFilePath) {
    this.generatedFilePath = generatedFilePath;
  }

  /**
   * Returns the force file generation.
   *
   * @return the force file generation
   */
  public Boolean getForceFileGeneration() {
    return forceFileGeneration;
  }

  /**
   * Sets the force file generation.
   *
   * @param forceFileGeneration the force file generation
   */
  public void setForceFileGeneration(Boolean forceFileGeneration) {
    this.forceFileGeneration = forceFileGeneration;
  }

  /**
   * Returns the metric log directory.
   *
   * @return the metric log directory
   */
  public String getMetricLogDirectory() {
    return metricLogDirectory;
  }

  /**
   * Sets the metric log directory.
   *
   * @param metricLogDirectory the metric log directory
   */
  public void setMetricLogDirectory(String metricLogDirectory) {
    this.metricLogDirectory = metricLogDirectory;
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
