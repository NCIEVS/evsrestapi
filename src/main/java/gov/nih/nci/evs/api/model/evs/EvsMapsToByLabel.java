package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "annotatedTarget", "relationshipToTarget", "targetTermType", "targetCode", "targetTerminology"})
public class EvsMapsToByLabel implements EvsMapsTo{
	@JsonProperty("annotatedTarget")
	private String annotatedTarget;

	@JsonProperty("Relationship_to_Target")
	private String relationshipToTarget;

	@JsonProperty("Target_Term_Type")
	private String targetTermType;

	@JsonProperty("Target_Code")
	private String targetCode;

	@JsonProperty("Target_Terminology")
	private String targetTerminology;

	public String getAnnotatedTarget() {
		return annotatedTarget;
	}
	public void setAnnotatedTarget(String annotatedTarget) {
		this.annotatedTarget = annotatedTarget;
	}
	public String getRelationshipToTarget() {
		return relationshipToTarget;
	}
	public void setRelationshipToTarget(String relationshipToTarget) {
		this.relationshipToTarget = relationshipToTarget;
	}
	public String getTargetTermType() {
		return targetTermType;
	}
	public void setTargetTermType(String targetTermType) {
		this.targetTermType = targetTermType;
	}
	public String getTargetCode() {
		return targetCode;
	}
	public void setTargetCode(String targetCode) {
		this.targetCode = targetCode;
	}
	public String getTargetTerminology() {
		return targetTerminology;
	}
	public void setTargetTerminology(String targetTerminology) {
		this.targetTerminology = targetTerminology;
	}
}