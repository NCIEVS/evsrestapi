package gov.nih.nci.evs.api.support;

import java.util.List;

import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Terminology;

public class ConfigData {
	private List<Property> properties;
	private Terminology terminology;
	private List<String> fullSynSources;
	
	public List<Property> getProperties() {
		return properties;
	}
	public void setProperties(final List<Property> properties) {
		this.properties = properties;
	}
	public Terminology getTerminology() {
		return terminology;
	}
	public void setTerminology(final Terminology terminology) {
		this.terminology = terminology;
	}
	public List<String> getFullSynSources() {
		return fullSynSources;
	}
	public void setFullSynSources(final List<String> fullSynSources) {
		this.fullSynSources = fullSynSources;
	}
}
