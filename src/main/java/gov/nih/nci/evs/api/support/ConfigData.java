package gov.nih.nci.evs.api.support;

import java.util.List;

import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.VersionInfo;

public class ConfigData {
	private List<Property> properties;
	private VersionInfo evsVersionInfo;
	private List<String> fullSynSources;
	
	public List<Property> getProperties() {
		return properties;
	}
	public void setProperties(final List<Property> properties) {
		this.properties = properties;
	}
	public VersionInfo getEvsVersionInfo() {
		return evsVersionInfo;
	}
	public void setEvsVersionInfo(final VersionInfo evsVersionInfo) {
		this.evsVersionInfo = evsVersionInfo;
	}
	public List<String> getFullSynSources() {
		return fullSynSources;
	}
	public void setFullSynSources(final List<String> fullSynSources) {
		this.fullSynSources = fullSynSources;
	}
	
	
	
	
	
	
	
}
