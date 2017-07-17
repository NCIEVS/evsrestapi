package gov.nih.nci.evs.api.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Data;

@Data
public class StardogProperties {
	
	
	/** The logger. */
    private static final Logger log = LoggerFactory.getLogger(StardogProperties.class);

    private String url;
    private String username;
    private String password;
    private String queryUrl;
    private int readTimeout;
    private int connectTimeout;
    private String graphName;
    private String owlfileName;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getQueryUrl() {
		return queryUrl;
	}

	public void setQueryUrl(String queryUrl) {
		this.queryUrl = queryUrl;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public String getGraphName() {
		return graphName;
	}

	public void setGraphName(String graphName) {
		this.graphName = graphName;
	}

	public String getOwlfileName() {
		return owlfileName;
	}

	public void setOwlfileName(String owlfileName) {
		this.owlfileName = owlfileName;
	}

	
}
