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

  /** The sdk base uri. */
  private String sdkBaseUri;

  /** The ui license - passed by UI to the backend via X-EVSRESAPI-License-Key. */
  private String uiLicense;

  /** The UnitTestData folder. */
  private String unitTestData;

  /** Childhood Neoplasm Subsets xls file. */
  private String childhoodNeoplasmSubsetsXls;

  /** FTP neoplasm site. */
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
   * Gets the sdk base uri.
   *
   * @return the sdk base uri
   */
  public String getSdkBaseUri() {
    return sdkBaseUri;
  }

  /**
   * Sets the sdk base uri.
   *
   * @param sdkBaseUri the new sdk base uri
   */
  public void setSdkBaseUri(final String sdkBaseUri) {
    this.sdkBaseUri = sdkBaseUri;
  }

  /**
   * Gets the childhood neoplasm subsets xls.
   *
   * @return the childhood neoplasm subsets xls
   */
  public String getChildhoodNeoplasmSubsetsXls() {
    return childhoodNeoplasmSubsetsXls;
  }

  /**
   * Sets the childhood neoplasm subsets xls.
   *
   * @param childhoodNeoplasmSubsetsXls the new childhood neoplasm subsets xls
   */
  public void setChildhoodNeoplasmSubsetsXls(final String childhoodNeoplasmSubsetsXls) {
    this.childhoodNeoplasmSubsetsXls = childhoodNeoplasmSubsetsXls;
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
   * @param ftpNeoplasmUrl the new ftp neoplasm url
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
   * @return the recaptcha site key
   */
  public String getRecaptchaSiteKey() {
    return recaptchaSiteKey;
  }

  /**
   * Set the recaptcha site key.
   *
   * @param recaptchaSiteKey the new recaptcha site key
   */
  public void setRecaptchaSiteKey(String recaptchaSiteKey) {
    this.recaptchaSiteKey = recaptchaSiteKey;
  }
}
