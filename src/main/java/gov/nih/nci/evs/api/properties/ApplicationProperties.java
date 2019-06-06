package gov.nih.nci.evs.api.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class ApplicationProperties {
	
	
	/** The logger. */
    private static final Logger log = LoggerFactory.getLogger(ApplicationProperties.class);

    private String contextPath;
    private String generatedFilePath;
    private Boolean forceFileGeneration;
    private String metricLogDirectory;

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}
    
	public String getGeneratedFilePath() {
		return generatedFilePath;
	}

	public void setGeneratedFilePath(String generatedFilePath) {
		this.generatedFilePath = generatedFilePath;
	}

	public Boolean getForceFileGeneration() {
		return forceFileGeneration;
	}

	public void setForceFileGeneration(Boolean forceFileGeneration) {
		this.forceFileGeneration = forceFileGeneration;
	}

	public String getMetricLogDirectory() {
		return metricLogDirectory;
	}

	public void setMetricLogDirectory(String metricLogDirectory) {
		this.metricLogDirectory = metricLogDirectory;
	}
		
		
}
