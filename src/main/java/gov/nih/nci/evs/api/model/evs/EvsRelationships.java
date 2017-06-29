package gov.nih.nci.evs.api.model.evs;

import java.util.List;

public class EvsRelationships {
	private List <EvsSuperconcept> superconcepts;
	private List <EvsSubconcept> subconcepts;
	private List <EvsAssociation> associations;
	private List <EvsAssociation> inverseAssociations;
	private List <EvsAssociation> roles;
	private List <EvsAssociation> inverseRoles;
	public List<EvsSuperconcept> getSuperconcepts() {
		return superconcepts;
	}
	public void setSuperconcepts(List<EvsSuperconcept> superconcepts) {
		this.superconcepts = superconcepts;
	}
	public List<EvsSubconcept> getSubconcepts() {
		return subconcepts;
	}
	public void setSubconcepts(List<EvsSubconcept> subconcepts) {
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

}
