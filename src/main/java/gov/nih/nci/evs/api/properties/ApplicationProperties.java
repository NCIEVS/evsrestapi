package gov.nih.nci.evs.api.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class ApplicationProperties {
	
	
	/** The logger. */
    private static final Logger log = LoggerFactory.getLogger(ApplicationProperties.class);

    private String contextPath;

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}
    
	
		
}
