
package gov.nih.nci.evs.api.properties;

/**
 * Stardog properties.
 */
public class StardogProperties {

  /** The username. */
  private String username;

  /** The password. */
  private String password;

  /** The read timeout. */
  private long readTimeout;

  /** The connect timeout. */
  private long connectTimeout;

  /** The owlfile name. */
  private String owlfileName;

  /** The populate cache cron. */
  private String populateCacheCron;

  /** The force populate cache. */
  private String forcePopulateCache;

  /** The monthly query url. */
  private String monthlyQueryUrl;

  /** The weekly query url. */
  private String weeklyQueryUrl;

  /** The monthly graph name. */
  private String monthlyGraphName;

  /** The weekly graph name. */
  private String weeklyGraphName;

  /**
   * Returns the username.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username.
   *
   * @param username the username
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Returns the password.
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password.
   *
   * @param password the password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Returns the read timeout.
   *
   * @return the read timeout
   */
  public long getReadTimeout() {
    return readTimeout;
  }

  /**
   * Sets the read timeout.
   *
   * @param readTimeout the read timeout
   */
  public void setReadTimeout(long readTimeout) {
    this.readTimeout = readTimeout;
  }

  /**
   * Returns the connect timeout.
   *
   * @return the connect timeout
   */
  public long getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Sets the connect timeout.
   *
   * @param connectTimeout the connect timeout
   */
  public void setConnectTimeout(long connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  /**
   * Returns the owlfile name.
   *
   * @return the owlfile name
   */
  public String getOwlfileName() {
    return owlfileName;
  }

  /**
   * Sets the owlfile name.
   *
   * @param owlfileName the owlfile name
   */
  public void setOwlfileName(String owlfileName) {
    this.owlfileName = owlfileName;
  }

  /**
   * Returns the populate cache cron.
   *
   * @return the populate cache cron
   */
  public String getPopulateCacheCron() {
    return populateCacheCron;
  }

  /**
   * Sets the populate cache cron.
   *
   * @param populateCacheCron the populate cache cron
   */
  public void setPopulateCacheCron(String populateCacheCron) {
    this.populateCacheCron = populateCacheCron;
  }

  /**
   * Returns the force populate cache.
   *
   * @return the force populate cache
   */
  public String getForcePopulateCache() {
    return forcePopulateCache;
  }

  /**
   * Sets the force populate cache.
   *
   * @param forcePopulateCache the force populate cache
   */
  public void setForcePopulateCache(String forcePopulateCache) {
    this.forcePopulateCache = forcePopulateCache;
  }

  /**
   * Returns the monthly query url.
   *
   * @return the monthly query url
   */
  public String getMonthlyQueryUrl() {
    return monthlyQueryUrl;
  }

  /**
   * Sets the monthly query url.
   *
   * @param monthlyQueryUrl the monthly query url
   */
  public void setMonthlyQueryUrl(String monthlyQueryUrl) {
    this.monthlyQueryUrl = monthlyQueryUrl;
  }

  /**
   * Returns the weekly query url.
   *
   * @return the weekly query url
   */
  public String getWeeklyQueryUrl() {
    return weeklyQueryUrl;
  }

  /**
   * Sets the weekly query url.
   *
   * @param weeklyQueryUrl the weekly query url
   */
  public void setWeeklyQueryUrl(String weeklyQueryUrl) {
    this.weeklyQueryUrl = weeklyQueryUrl;
  }

  /**
   * Returns the monthly graph name.
   *
   * @return the monthly graph name
   */
  public String getMonthlyGraphName() {
    return monthlyGraphName;
  }

  /**
   * Sets the monthly graph name.
   *
   * @param monthlyGraphName the monthly graph name
   */
  public void setMonthlyGraphName(String monthlyGraphName) {
    this.monthlyGraphName = monthlyGraphName;
  }

  /**
   * Returns the weekly graph name.
   *
   * @return the weekly graph name
   */
  public String getWeeklyGraphName() {
    return weeklyGraphName;
  }

  /**
   * Sets the weekly graph name.
   *
   * @param weeklyGraphName the weekly graph name
   */
  public void setWeeklyGraphName(String weeklyGraphName) {
    this.weeklyGraphName = weeklyGraphName;
  }

}
