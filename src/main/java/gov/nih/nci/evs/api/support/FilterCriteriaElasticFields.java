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

	public void setHierarchySearch(final String hierarchySearch) {
		this.hierarchySearch = hierarchySearch;
	}

	private ArrayList<String> relationship;

	public String getRoleSearch() {
		return roleSearch;
	}

	public void setRoleSearch(final String roleSearch) {
		this.roleSearch = roleSearch;
	}

	public String getAssociationSearch() {
		return associationSearch;
	}

	public void setAssociationSearch(final String associationSearch) {
		this.associationSearch = associationSearch;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(final String format) {
		this.format = format;
	}

	public Integer getFromRecord() {
		return fromRecord;
	}

	public void setFromRecord(final Integer fromRecord) {
		this.fromRecord = fromRecord;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(final Integer pageSize) {
		this.pageSize = pageSize;
	}
	

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(final String term) {
		this.term = term;
	}

	public ArrayList<String> getProperty() {
		return property;
	}

	public void setProperty(final ArrayList<String> property) {
		this.property = property;
	}

	public ArrayList<String> getReturnProperties() {
		return returnProperties;
	}

	public void setReturnProperties(final ArrayList<String> returnProperties) {
		this.returnProperties = returnProperties;
	}

	public ArrayList<String> getSynonymSource() {
		return synonymSource;
	}

	public void setSynonymSource(final ArrayList<String> synonymSource) {
		this.synonymSource = synonymSource;
	}

	public ArrayList<String> getRelationship() {
		return relationship;
	}

	public void setRelationship(final ArrayList<String> relationship) {
		this.relationship = relationship;
	}

	public String getSynonymGroup() {
		return synonymGroup;
	}

	public void setSynonymGroup(final String synonymGroup) {
		this.synonymGroup = synonymGroup;
	}

	public ArrayList<String> getDefinitionSource() {
		return definitionSource;
	}

	public void setDefinitionSource(final ArrayList<String> definitionSource) {
		this.definitionSource = definitionSource;
	}

	public ArrayList<String> getConceptStatus() {
		return conceptStatus;
	}

	public void setConceptStatus(final ArrayList<String> conceptStatus) {
		this.conceptStatus = conceptStatus;
	}

	public ArrayList<String> getContributingSource() {
		return contributingSource;
	}

	public void setContributingSource(final ArrayList<String> contributingSource) {
		this.contributingSource = contributingSource;
	}

}
