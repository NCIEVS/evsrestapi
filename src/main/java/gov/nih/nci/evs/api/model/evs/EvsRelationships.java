package gov.nih.nci.evs.api.model.evs;

import java.util.List;

public class EvsRelationships {
	private List <EvsRelatedConcept> superconcepts;
	private List <EvsRelatedConcept> subconcepts;
	private List <EvsAssociation> associations;
	private List <EvsAssociation> inverseAssociations;
	private List <EvsAssociation> roles;
	private List <EvsAssociation> inverseRoles;
	private List <EvsAssociation> disjointWith;

	public List<EvsRelatedConcept> getSuperconcepts() {
		return superconcepts;
	}
	public void setSuperconcepts(List<EvsRelatedConcept> superconcepts) {
		this.superconcepts = superconcepts;
	}
	public List<EvsRelatedConcept> getSubconcepts() {
		return subconcepts;
	}
	public void setSubconcepts(List<EvsRelatedConcept> subconcepts) {
		this.subconcepts = subconcepts;
	}
	public List<EvsAssociation> getAssociations() {
		return associations;
	}
	public void setAssociations(List<EvsAssociation> associations) {
		this.associations = associations;
	}
	public List<EvsAssociation> getInverseAssociations() {
		return inverseAssociations;
	}
	public void setInverseAssociations(List<EvsAssociation> inverseAssociations) {
		this.inverseAssociations = inverseAssociations;
	}
	public List<EvsAssociation> getRoles() {
		return roles;
	}
	public void setRoles(List<EvsAssociation> roles) {
		this.roles = roles;
	}
	public List<EvsAssociation> getInverseRoles() {
		return inverseRoles;
	}
	public void setInverseRoles(List<EvsAssociation> inverseRoles) {
		this.inverseRoles = inverseRoles;
	}
	public List<EvsAssociation> getDisjointWith() {
		return disjointWith;
	}
	public void setDisjointWith(List<EvsAssociation> disjointWith) {
		this.disjointWith = disjointWith;
	}
}
