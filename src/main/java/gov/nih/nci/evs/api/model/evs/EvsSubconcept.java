package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class EvsSubconcept {
	private String subclass;
	private String code;
	private String label;

	@JsonIgnore
	public String getSubclass() {
		return subclass;
	}
	public void setSubclass(String subclass) {
		this.subclass = subclass;
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
