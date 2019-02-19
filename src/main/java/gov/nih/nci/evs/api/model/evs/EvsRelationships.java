package gov.nih.nci.evs.api.model.evs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EvsRelationships {
	@JsonProperty("Superconcept")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsRelatedConcept> superconcepts;
	
	@JsonProperty("Subconcept")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsRelatedConcept> subconcepts;
	
	@JsonProperty("Association")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> associations;
	
	@JsonProperty("InverseAssociation")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> inverseAssociations;
	
	@JsonProperty("Role")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> roles;
	
	@JsonProperty("InverseRole")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> inverseRoles;
	
	@JsonProperty("DisjointWith")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
