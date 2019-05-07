package gov.nih.nci.evs.api.support;

import java.util.List;

import gov.nih.nci.evs.api.model.evs.EvsProperty;

public class ConfigData {
	private List<EvsProperty> properties;
	private List<String> defintionSources;
	private List<String> synonymSources;
	private List<String> synonymGroups;
	
	public List<EvsProperty> getProperties() {
		return properties;
	}
	public void setProperties(List<EvsProperty> properties) {
		this.properties = properties;
	}
	public List<String> getDefintionSources() {
		return defintionSources;
	}
	public void setDefintionSources(List<String> defintionSources) {
		this.defintionSources = defintionSources;
	}
	public List<String> getSynonymSources() {
		return synonymSources;
	}
	public void setSynonymSources(List<String> synonymSources) {
		this.synonymSources = synonymSources;
	}
	public List<String> getSynonymGroups() {
		return synonymGroups;
	}
	public void setSynonymGroups(List<String> synonymGroups) {
		this.synonymGroups = synonymGroups;
	}
	
	
	
	
}
