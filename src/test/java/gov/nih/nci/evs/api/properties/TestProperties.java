package gov.nih.nci.evs.api.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "test", ignoreUnknownFields = false)
public class TestProperties {

    private String conceptCodeList;

	public String getConceptCodeList() {
		return conceptCodeList;
	}

	public void setConceptCodeList(String conceptCodeList) {
		this.conceptCodeList = conceptCodeList;
	}
   

    
    

}
