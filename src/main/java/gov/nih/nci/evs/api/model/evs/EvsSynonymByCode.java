package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "termName", "termGroup", "termSource", "sourceCode", "subsourceName" })
public class EvsSynonymByCode {
	private String code;
	private String label;
	
	@JsonProperty("P382")
	private String termName;

	@JsonProperty("P383")
	private String termGroup;

	@JsonProperty("P384")
	private String termSource;

	@JsonProperty("P385")
	private String sourceCode;

	@JsonProperty("P386")
	private String subsourceName;

	@JsonIgnore
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	@JsonIgnore
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getTermName() {
		return termName;
	}
	public void setTermName(String termName) {
		this.termName = termName;
	}
	public String getTermGroup() {
		return termGroup;
	}
	public void setTermGroup(String termGroup) {
		this.termGroup = termGroup;
	}
	public String getTermSource() {
		return termSource;
	}
	public void setTermSource(String termSource) {
		this.termSource = termSource;
	}
	public String getSourceCode() {
		return sourceCode;
	}
	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}
	public String getSubsourceName() {
		return subsourceName;
	}
	public void setSubsourceName(String subsourceName) {
		this.subsourceName = subsourceName;
	}
}