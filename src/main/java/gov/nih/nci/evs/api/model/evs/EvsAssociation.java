package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvsAssociation {
	private String relationship;
	private String relationshipCode;
	private String relatedConceptCode;
	private String relatedConceptLabel;

	public String getRelationship() {
		return relationship;
	}
	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}
	public String getRelationshipCode() {
		return relationshipCode;
	}
	public void setRelationshipCode(String relationshipCode) {
		this.relationshipCode = relationshipCode;
	}
	public String getRelatedConceptCode() {
		return relatedConceptCode;
	}
	public void setRelatedConceptCode(String relatedConceptCode) {
		this.relatedConceptCode = relatedConceptCode;
	}
	public String getRelatedConceptLabel() {
		return relatedConceptLabel;
	}
	public void setRelatedConceptLabel(String relatedConceptLabel) {
		this.relatedConceptLabel = relatedConceptLabel;
	}
}
