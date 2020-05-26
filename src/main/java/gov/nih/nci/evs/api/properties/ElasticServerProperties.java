package gov.nih.nci.evs.api.properties;

public class ElasticServerProperties {
	
  /** The host **/
  private String host;
  
  /** The port **/
  private String port;
	
  /** The cluster name **/
  private String clusterName;
  
  /** The search url **/
  private String url;

  /**
   * Returns the host
   * 
   * @return the host
   */
	public String getHost() {
    return host;
  }

	/**
	 * Sets the host
	 * 
	 * @param host the host
	 */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets the port
   * 
   * @return the port
   */
  public String getPort() {
    return port;
  }

  /**
   * Sets the port
   * 
   * @param port the port
   */
  public void setPort(String port) {
    this.port = port;
  }

  /**
   * Gets the cluster name
   * 
   * @return the cluster name
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Sets the cluster name
   * 
   * @param clusterName the cluster name
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }
  
  /**
   * Returns the search url
   * 
   * @return
   */
  public String getUrl() {
		return url;
	}

  /**
   * Sets the search url
   * 
   * @param url the search url
   */
	public void setUrl(String url) {
		this.url = url;
	}


}
