package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvsAssociation {

	@JsonView(ConceptViews.FullClass.class)
	private String relationship;

	@JsonView(ConceptViews.FullClass.class)
	private String relationshipCode;

	@JsonView(ConceptViews.FullClass.class)
	private String relatedConceptCode;
	
	@JsonView(ConceptViews.FullClass.class)
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
