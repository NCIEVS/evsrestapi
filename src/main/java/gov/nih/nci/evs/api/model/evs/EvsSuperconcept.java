package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EvsSuperconcept {
	private String superclass;
	private String code;
	private String label;

	@JsonIgnore
	public String getSuperclass() {
		return superclass;
	}
	public void setSuperclass(String superclass) {
		this.superclass = superclass;
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
