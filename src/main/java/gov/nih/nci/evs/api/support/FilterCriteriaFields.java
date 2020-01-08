package gov.nih.nci.evs.api.support;

public class FilterCriteriaFields {
	
	private String limit;
	private String type;
	private String term;
	private String property;
	
	public String getLimit() {
		return limit;
	}
	public void setLimit(final String limit) {
		this.limit = limit;
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
	public String getProperty() {
		return property;
	}
	public void setProperty(final String property) {
		this.property = property;
	}

}
