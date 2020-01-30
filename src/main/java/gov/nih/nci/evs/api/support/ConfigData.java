package gov.nih.nci.evs.api.support;

import java.util.List;

import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;

public class ConfigData {
	private List<EvsProperty> properties;
	private EvsVersionInfo evsVersionInfo;
	private List<String> fullSynSources;
	
	public List<EvsProperty> getProperties() {
		return properties;
	}
	public void setProperties(final List<EvsProperty> properties) {
		this.properties = properties;
	}
	public EvsVersionInfo getEvsVersionInfo() {
		return evsVersionInfo;
	}
	public void setEvsVersionInfo(final EvsVersionInfo evsVersionInfo) {
		this.evsVersionInfo = evsVersionInfo;
	}
	public List<String> getFullSynSources() {
		return fullSynSources;
	}
	public void setFullSynSources(final List<String> fullSynSources) {
		this.fullSynSources = fullSynSources;
	}
	
	
	
	
	
	
	
}
