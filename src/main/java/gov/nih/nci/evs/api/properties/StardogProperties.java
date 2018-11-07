package gov.nih.nci.evs.api.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class StardogProperties {
	
	
	/** The logger. */
    private static final Logger log = LoggerFactory.getLogger(StardogProperties.class);

    private String username;
    private String password;
    private long readTimeout;
    private long connectTimeout;
    private String owlfileName;
    private String populateCacheCron;
    private String forcePopulateCache;
    
    private String monthlyQueryUrl;
    private String weeklyQueryUrl;
    private String monthlyGraphName;
    private String weeklyGraphName;

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

	public long getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(long readTimeout) {
		this.readTimeout = readTimeout;
	}

	public long getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public String getOwlfileName() {
		return owlfileName;
	}

	public void setOwlfileName(String owlfileName) {
		this.owlfileName = owlfileName;
	}

	public String getPopulateCacheCron() {
		return populateCacheCron;
	}

	public void setPopulateCacheCron(String populateCacheCron) {
		this.populateCacheCron = populateCacheCron;
	}

	public String getForcePopulateCache() {
		return forcePopulateCache;
	}

	public void setForcePopulateCache(String forcePopulateCache) {
		this.forcePopulateCache = forcePopulateCache;
	}

	public String getMonthlyQueryUrl() {
		return monthlyQueryUrl;
	}

	public void setMonthlyQueryUrl(String monthlyQueryUrl) {
		this.monthlyQueryUrl = monthlyQueryUrl;
	}

	public String getWeeklyQueryUrl() {
		return weeklyQueryUrl;
	}

	public void setWeeklyQueryUrl(String weeklyQueryUrl) {
		this.weeklyQueryUrl = weeklyQueryUrl;
	}

	public String getMonthlyGraphName() {
		return monthlyGraphName;
	}

	public void setMonthlyGraphName(String monthlyGraphName) {
		this.monthlyGraphName = monthlyGraphName;
	}

	public String getWeeklyGraphName() {
		return weeklyGraphName;
	}

	public void setWeeklyGraphName(String weeklyGraphName) {
		this.weeklyGraphName = weeklyGraphName;
	}

	
}
