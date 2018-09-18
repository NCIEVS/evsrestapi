package gov.nih.nci.evs.api.support;

import java.util.ArrayList;

public class FilterCriteriaElasticFields {

	private String type;
	private String term;
	private ArrayList<String> property;
	private Integer fromRecord;
	private Integer pageSize;
	private ArrayList<String> returnProperties;
	private String biomarker;
	private String disease;
	private String synonymSource;
	private String synonymGroup;
	private String associationSearch;
	private String definitionSource;
	private ArrayList<String> relationship;
	
	public String getAssociationSearch() {
		return associationSearch;
	}

	public void setAssociationSearch(String associationSearch) {
		this.associationSearch = associationSearch;
	}

	

	private String format;

	

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

	public String getBiomarker() {
		return biomarker;
	}

	public void setBiomarker(String biomarker) {
		this.biomarker = biomarker;
	}

	public String getDisease() {
		return disease;
	}

	public void setDisease(String disease) {
		this.disease = disease;
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

	public String getSynonymSource() {
		return synonymSource;
	}

	public void setSynonymSource(String synonymSource) {
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

	public String getDefinitionSource() {
		return definitionSource;
	}

	public void setDefinitionSource(String definitionSource) {
		this.definitionSource = definitionSource;
	}

	

}
