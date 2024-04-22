package gov.nih.nci.evs.api.properties;

/** The Class ElasticServerProperties. */
public class ElasticServerProperties {

  /** The host *. */
  private String host;

  /** The port *. */
  private String port;

  /** The scheme. */
  private String scheme;

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
   * Gets the port.
   *
   * @return the port
   */
  public String getPort() {
    return port;
  }

  /**
   * Sets the port.
   *
   * @param port the port
   */
  public void setPort(String port) {
    this.port = port;
  }

  /**
   * Returns the scheme.
   *
   * @return the scheme
   */
  public String getScheme() {
    return scheme;
  }

  /**
   * Sets the scheme.
   *
   * @param scheme the scheme
   */
  public void setScheme(String scheme) {
    this.scheme = scheme;
  }
}
