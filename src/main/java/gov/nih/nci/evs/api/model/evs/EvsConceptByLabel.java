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
public class EvsConceptByLabel {
	
	@JsonProperty("Code")
	private String code;


	@JsonProperty("Label")
	private String label;
	
	@JsonProperty("Preferred_Name")
	private String preferredName;

	private Map <String,List<String>> properties = new HashMap <String,List<String>>();
	
	@JsonProperty("FULL_SYN")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)	
	private List <EvsSynonymByLabel> synonyms;
	
	@JsonProperty("DEFINITION")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsDefinitionByLabel> definitions;
	
	@JsonProperty("ALT_DEFINITION")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsDefinitionByLabel> altDefinitions;
	
	@JsonProperty("Subconcept")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsSubconceptByLabel> subconcepts;

	@JsonProperty("Superconcept")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsSuperconceptByLabel> superconcepts;
	
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
	
	@JsonProperty("Maps_To")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsMapsTo> mapsTo;
	
	@JsonProperty("GO_Annotation")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsGoAnnotation> goAnnotations;	
	
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
	
	public List<EvsSynonymByLabel> getSynonyms() {
		return synonyms;
	}
	public void setSynonyms(List<EvsSynonymByLabel> synonyms) {
		this.synonyms = synonyms;
	}
	
	public List <EvsDefinitionByLabel> getDefinitions() {
		return definitions;
	}
	public void setDefinitions(List<EvsDefinitionByLabel> definitions) {
		this.definitions = definitions;
	}
	
	public List <EvsDefinitionByLabel> getAltDefinitions() {
		return altDefinitions;
	}
	public void setAltDefinitions(List<EvsDefinitionByLabel> altDefinitions) {
		this.altDefinitions = altDefinitions;
	}	
	public List<EvsSubconceptByLabel> getSubconcepts() {
		return subconcepts;
	}
	public void setSubconcepts(List<EvsSubconceptByLabel> subconcepts) {
		this.subconcepts = subconcepts;
	}

	public List<EvsSuperconceptByLabel> getSuperconcepts() {
		return superconcepts;
	}
	public void setSuperconcepts(List<EvsSuperconceptByLabel> superconcepts) {
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
	
	public List<EvsMapsTo> getMapsTo() {
		return mapsTo;
	}
	public void setMapsTo(List<EvsMapsTo> mapsTo) {
		this.mapsTo = mapsTo;
	}

	public List<EvsGoAnnotation> getGoAnnotations() {
		return goAnnotations;
	}
	public void setGoAnnotations(List<EvsGoAnnotation> goAnnotations) {
		this.goAnnotations = goAnnotations;
	}
	
	public List<EvsAssociation> getDisjointWith() {
		return disjointWith;
	}
	public void setDisjointWith(List<EvsAssociation> disjointWith) {
		this.disjointWith = disjointWith;
	}
	
}
