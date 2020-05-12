
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

  /** The populate cache cron. */
  private String populateCacheCron;

  /** The force populate cache. */
  private String forcePopulateCache;

  /** The wait populate cache. */
  private String waitPopulateCache;

  /** The query url. */
  private String queryUrl;

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
   * Returns the wait populate cache.
   *
   * @return the wait populate cache
   */
  public String getWaitPopulateCache() {
    return waitPopulateCache;
  }

  /**
   * Sets the wait populate cache.
   *
   * @param waitPopulateCache the wait populate cache
   */
  public void setWaitPopulateCache(String waitPopulateCache) {
    this.waitPopulateCache = waitPopulateCache;
  }

  /**
   * Returns the query url.
   *
   * @return the query url
   */
  public String getQueryUrl() {
    return queryUrl;
  }

  /**
   * Sets the query url.
   *
   * @param queryUrl the query url
   */
  public void setQueryUrl(String queryUrl) {
    this.queryUrl = queryUrl;
  }

}
