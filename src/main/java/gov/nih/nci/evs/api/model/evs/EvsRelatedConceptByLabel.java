package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EvsRelatedConceptByLabel implements EvsRelatedConcept {
	private String relatedConcept;

	@JsonProperty("code")
	private String code;

	@JsonProperty("label")
	private String label;

	@JsonIgnore
	public String getRelatedConcept() {
		return relatedConcept;
	}
	public void setRelatedConcept(String relatedConcept) {
		this.relatedConcept = relatedConcept;
	}
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

}