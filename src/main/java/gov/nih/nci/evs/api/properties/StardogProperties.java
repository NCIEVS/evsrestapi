
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

  /** The host *. */
  private String host;

  /** The port *. */
  private String port;

  /** The db. */
  private String db;

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
   * Returns the host.
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets the host.
   *
   * @param host the host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Returns the port.
   *
   * @return the port
   */
  public String getPort() {
    return port;
  }

  /**
   * Set the port.
   *
   * @param port the port
   */
  public void setPort(String port) {
    this.port = port;
  }

  /**
   * Returns the db.
   *
   * @return the db
   */
  public String getDb() {
    return db;
  }

  /**
   * Sets the db.
   *
   * @param db the db
   */
  public void setDb(String db) {
    this.db = db;
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
