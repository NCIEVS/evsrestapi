package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EvsSynonym {
	private String code;
	private String label;
	private String termName;
	private String termGroup;
	private String termSource;
	private String sourceCode;
	private String subsourceName;

	@JsonIgnore
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
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
