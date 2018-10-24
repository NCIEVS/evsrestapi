package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "annotatedTarget", "relationshipToTarget", "targetTermType",
	"targetCode", "targetTerminology"})
public class EvsMapsToByCode {
	@JsonProperty("annotatedTarget")
	private String annotatedTarget;

	@JsonProperty("P393")
	private String relationshipToTarget;

	@JsonProperty("P394")
	private String targetTermType;

	@JsonProperty("P395")
	private String targetCode;

	@JsonProperty("P396")
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