package gov.nih.nci.evs.api.properties;

import java.util.List;
import java.util.Map;

public class ThesaurusProperties {
	
	private Map<String,String> roles;
	
	private Map<String,String> associations;
	
	private Map<String,String> conceptStatuses;
	
	private Map<String,String> contributingSources;
	
	private Map<String,String> returnFields;
	
	
	private Map<String,String>  propertyNotConsidered;
	
	//code to be removed 
	private Map<String,String> returnFieldsByLabel;
	
	private String[]  sourcesToBeRemoved;

	public Map<String,String> getRoles() {
		return roles;
	}

	public void setRoles(Map<String,String> roles) {
		this.roles = roles;
	}

	public Map<String,String> getAssociations() {
		return associations;
	}

	public void setAssociations(Map<String,String> associations) {
		this.associations = associations;
	}

	public Map<String,String> getConceptStatuses() {
		return conceptStatuses;
	}

	public void setConceptStatuses(Map<String,String> conceptStatuses) {
		this.conceptStatuses = conceptStatuses;
	}

	public Map<String,String> getReturnFields() {
		return returnFields;
	}

	public void setReturnFields(Map<String,String> returnFields) {
		this.returnFields = returnFields;
	}

	public Map<String,String> getReturnFieldsByLabel() {
		return returnFieldsByLabel;
	}

	public void setReturnFieldsByLabel(Map<String,String> returnFieldsByLabel) {
		this.returnFieldsByLabel = returnFieldsByLabel;
	}

	public Map<String,String> getContributingSources() {
		return contributingSources;
	}

	public void setContributingSources(Map<String,String> contributingSources) {
		this.contributingSources = contributingSources;
	}

	public Map<String,String> getPropertyNotConsidered() {
		return propertyNotConsidered;
	}

	public void setPropertyNotConsidered(Map<String,String> propertyNotConsidered) {
		this.propertyNotConsidered = propertyNotConsidered;
	}

	public String[]  getSourcesToBeRemoved() {
		return sourcesToBeRemoved;
	}

	public void setSourcesToBeRemoved(String[]  sourcesToBeRemoved) {
		this.sourcesToBeRemoved = sourcesToBeRemoved;
	}

}
