package gov.nih.nci.evs.api.properties;

import org.springframework.beans.factory.annotation.Value;

/** Application properties. */
public class ApplicationProperties {

  /** The admin key. */
  private String adminKey;

  /** The context path. */
  private String contextPath;

  /** The metric log switch. */
  private Boolean metricsEnabled;

  /** The metrics dir. */
  private String metricsDir;

  /** The config base Uri. */
  private String configBaseUri;

  /** The ui license - passed by UI to the backend via X-EVSRESAPI-License-Key. */
  private String uiLicense;

  /** The UnitTestData folder */
  private String unitTestData;

  /** Pediatric Subsets xls file */
  private String pediatricSubsetsXls;

  /** FTP neoplasm site */
  private String ftpNeoplasmUrl;

  /** The recaptcha secret key. */
  @Value("${google.recaptcha.site.key}")
  private String recaptchaSiteKey;

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
  public void setAdminKey(final String adminKey) {
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
  public void setContextPath(final String contextPath) {
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
  public void setMetricsEnabled(final Boolean metricsEnabled) {
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
  public void setMetricsDir(final String metricsDir) {
    this.metricsDir = metricsDir;
  }

  /**
   * Returns the config base uri.
   *
   * @return the config base uri
   */
  public String getConfigBaseUri() {
    return configBaseUri;
  }

  /**
   * Sets the config base uri.
   *
   * @param configBaseUri the config base uri
   */
  public void setConfigBaseUri(final String configBaseUri) {
    this.configBaseUri = configBaseUri;
  }

  /**
   * Returns the pediatricSubsetsXls.
   *
   * @return the pediatricSubsetsXls
   */
  public String getPediatricSubsetsXls() {
    return pediatricSubsetsXls;
  }

  /**
   * Sets the pediatricSubsetsXls.
   *
   * @param uiLicense the pediatricSubsetsXls
   */
  public void setPediatricSubsetsXls(final String pediatricSubsetsXls) {
    this.pediatricSubsetsXls = pediatricSubsetsXls;
  }

  /**
   * Returns the unitTestData.
   *
   * @return the unitTestData
   */
  public String getUnitTestData() {
    return unitTestData;
  }

  /**
   * Sets the unitTestData.
   *
   * @param unitTestData the unitTestData
   */
  public void setUnitTestData(final String unitTestData) {
    this.unitTestData = unitTestData;
  }

  /**
   * Returns the ftpNeoplasmUrl.
   *
   * @return the ftpNeoplasmUrl
   */
  public String getFtpNeoplasmUrl() {
    return ftpNeoplasmUrl;
  }

  /**
   * Sets the ftpNeoplasmUrl.
   *
   * @param uiLicense the ftpNeoplasmUrl
   */
  public void setFtpNeoplasmUrl(final String ftpNeoplasmUrl) {
    this.ftpNeoplasmUrl = ftpNeoplasmUrl;
  }

  /**
   * Returns the ui license.
   *
   * @return the ui license
   */
  public String getUiLicense() {
    return uiLicense;
  }

  /**
   * Sets the ui license.
   *
   * @param uiLicense the ui license
   */
  public void setUiLicense(final String uiLicense) {
    this.uiLicense = uiLicense;
  }

  /**
   * Get the recaptcha site key.
   *
   * @return
   */
  public String getRecaptchaSiteKey() {
    return recaptchaSiteKey;
  }

  /**
   * Set the recaptcha site key.
   *
   * @param recaptchaSiteKey
   */
  public void setRecaptchaSiteKey(String recaptchaSiteKey) {
    this.recaptchaSiteKey = recaptchaSiteKey;
  }
}
