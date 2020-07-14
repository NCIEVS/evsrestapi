package gov.nih.nci.evs.api.model;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class ConceptNode {
  @Field(type = FieldType.Integer)
	private int idx;
  
  @Field(type = FieldType.Text)
	private String label;
  
  @Field(type = FieldType.Text)
	private String code;

	public ConceptNode() {
	}

	public ConceptNode(int idx, String label, String code) {

		this.idx = idx;
		this.label = label;
		this.code = code;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public int getIdx() {
		return this.idx;
	}

	public String getLabel() {
		return this.label;
	}

	public String getCode() {
		return this.code;
	}
}
