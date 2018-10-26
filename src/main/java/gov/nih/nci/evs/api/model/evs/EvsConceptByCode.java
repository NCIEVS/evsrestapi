package gov.nih.nci.evs.api.model.evs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "code", "label", "preferredName", "properties", "synonyms",
	"definitions", "altDefinitions", "subconcepts", "superconcepts", 
	"associations", "inverseAssociations", 
	"roles", "inverseRoles",
	"mapsTo", "goAnnotations", "disjointWith"
	})
public class EvsConceptByCode {
	
	@JsonProperty("NCH0")
	private String code;

	@JsonProperty("rdfs:label")
	private String label;
	
	@JsonProperty("P108")
	private String preferredName;

	private Map <String,List<String>> properties = new HashMap <String,List<String>>();
	
	@JsonProperty("P90")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsSynonymByCode> synonyms;
	
	@JsonProperty("P97")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsDefinitionByCode> definitions;

	@JsonProperty("P325")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsDefinitionByCode> altDefinitions;
	
	@JsonProperty("Subconcept")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsSubconceptByCode> subconcepts;

	@JsonProperty("Superconcept")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsSuperconceptByCode> superconcepts;
	
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
	
	@JsonProperty("P375")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsMapsToByCode> mapsTo;
	
	@JsonProperty("P211")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsGoAnnotationByCode> goAnnotations;	
	
	@JsonProperty("DisjointWith")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> disjointWith;	

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
	
	public String getPreferredName() {
		return preferredName;
	}
	public void setPreferredName(String preferredName) {
		this.preferredName = preferredName;
	}

	@JsonAnyGetter
	public Map <String,List<String>> getProperties() {
		return properties;
	};

	public List<EvsSynonymByCode> getSynonyms() {
		return synonyms;
	}
	public void setSynonyms(List<EvsSynonymByCode> synonyms) {
		this.synonyms = synonyms;
	}
	
	public List <EvsDefinitionByCode> getDefinitions() {
		return definitions;
	}
	public void setDefinitions(List<EvsDefinitionByCode> definitions) {
		this.definitions = definitions;
	}

	public List <EvsDefinitionByCode> getAltDefinitions() {
		return altDefinitions;
	}
	public void setAltDefinitions(List<EvsDefinitionByCode> altDefinitions) {
		this.altDefinitions = altDefinitions;
	}
	
	public List<EvsSubconceptByCode> getSubconcepts() {
		return subconcepts;
	}
	public void setSubconcepts(List<EvsSubconceptByCode> subconcepts) {
		this.subconcepts = subconcepts;
	}

	public List<EvsSuperconceptByCode> getSuperconcepts() {
		return superconcepts;
	}
	public void setSuperconcepts(List<EvsSuperconceptByCode> superconcepts) {
		this.superconcepts = superconcepts;
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
	
	public List<EvsMapsToByCode> getMapsTo() {
		return mapsTo;
	}
	public void setMapsTo(List<EvsMapsToByCode> mapsTo) {
		this.mapsTo = mapsTo;
	}
	
	public List<EvsGoAnnotationByCode> getGoAnnotations() {
		return goAnnotations;
	}
	public void setGoAnnotations(List<EvsGoAnnotationByCode> goAnnotations) {
		this.goAnnotations = goAnnotations;
	}
	
	public List<EvsAssociation> getDisjointWith() {
		return disjointWith;
	}
	public void setDisjointWith(List<EvsAssociation> disjointWith) {
		this.disjointWith = disjointWith;
	}
}

