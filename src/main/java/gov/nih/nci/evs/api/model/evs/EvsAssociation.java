package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonView;

public class EvsAssociation {

	@JsonView(ConceptViews.FullClass.class)
	private String relationship;

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
