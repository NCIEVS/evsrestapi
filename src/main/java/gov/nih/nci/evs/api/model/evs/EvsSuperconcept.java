package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

public class EvsSuperconcept {
	private String superclass;

	@JsonView(ConceptViews.FullClass.class)
	private String code;
	@JsonView(ConceptViews.FullClass.class)

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
