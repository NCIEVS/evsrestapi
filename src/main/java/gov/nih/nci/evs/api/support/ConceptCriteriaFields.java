package gov.nih.nci.evs.api.support;

import java.util.List;

public class ConceptCriteriaFields {

	private List <String> concepts;
	private String db = "monthly";
	private String fmt = "byLabel";
	private List <String> properties;

	public List<String> getConcepts() {
		return concepts;
	}

	public void setConcepts(List<String> concepts) {
		this.concepts = concepts;
	}

	public String getDb() {
		return db;
	}

	public void setDb(String db) {
		this.db = db;
	}

	public String getFmt() {
		return fmt;
	}

	public void setFmt(String fmt) {
		this.fmt = fmt;
	}

	public List<String> getProperties() {
		return properties;
	}

	public void setProperties(List<String> properties) {
		this.properties = properties;
	}

}
