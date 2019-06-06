package gov.nih.nci.evs.api.support;

import java.util.ArrayList;

public class FilterCriteriaElasticFields {

	private String type;
	private String term;
	private ArrayList<String> property;
	private Integer fromRecord;
	private Integer pageSize;
	private ArrayList<String> returnProperties;
	private ArrayList<String> conceptStatus;
	private ArrayList<String> contributingSource;
	private ArrayList<String> synonymSource;
	private String synonymGroup;
	private String associationSearch;
	private String roleSearch;
	private String format;
	private ArrayList<String> definitionSource;
	private String hierarchySearch;
	
	public String getHierarchySearch() {
		return hierarchySearch;
	}

	public void setHierarchySearch(String hierarchySearch) {
		this.hierarchySearch = hierarchySearch;
	}

	private ArrayList<String> relationship;

	public String getRoleSearch() {
		return roleSearch;
	}

	public void setRoleSearch(String roleSearch) {
		this.roleSearch = roleSearch;
	}

	public String getAssociationSearch() {
		return associationSearch;
	}

	public void setAssociationSearch(String associationSearch) {
		this.associationSearch = associationSearch;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public Integer getFromRecord() {
		return fromRecord;
	}

	public void setFromRecord(Integer fromRecord) {
		this.fromRecord = fromRecord;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}
	

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public ArrayList<String> getProperty() {
		return property;
	}

	public void setProperty(ArrayList<String> property) {
		this.property = property;
	}

	public ArrayList<String> getReturnProperties() {
		return returnProperties;
	}

	public void setReturnProperties(ArrayList<String> returnProperties) {
		this.returnProperties = returnProperties;
	}

	public ArrayList<String> getSynonymSource() {
		return synonymSource;
	}

	public void setSynonymSource(ArrayList<String> synonymSource) {
		this.synonymSource = synonymSource;
	}

	public ArrayList<String> getRelationship() {
		return relationship;
	}

	public void setRelationship(ArrayList<String> relationship) {
		this.relationship = relationship;
	}

	public String getSynonymGroup() {
		return synonymGroup;
	}

	public void setSynonymGroup(String synonymGroup) {
		this.synonymGroup = synonymGroup;
	}

	public ArrayList<String> getDefinitionSource() {
		return definitionSource;
	}

	public void setDefinitionSource(ArrayList<String> definitionSource) {
		this.definitionSource = definitionSource;
	}

	public ArrayList<String> getConceptStatus() {
		return conceptStatus;
	}

	public void setConceptStatus(ArrayList<String> conceptStatus) {
		this.conceptStatus = conceptStatus;
	}

	public ArrayList<String> getContributingSource() {
		return contributingSource;
	}

	public void setContributingSource(ArrayList<String> contributingSource) {
		this.contributingSource = contributingSource;
	}

}
