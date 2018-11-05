package gov.nih.nci.evs.api.properties;

import java.util.Map;

public class ThesaurusProperties {
	
	private Map<String,String> roles;
	
	private Map<String,String> associations;
	
	private Map<String,String> conceptStatuses;
	
	private Map<String,String> returnFields;
	
	//code to be removed 
	private Map<String,String> returnFieldsByLabel;

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

}
