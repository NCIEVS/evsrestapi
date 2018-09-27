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
public class EvsConceptFull {
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("Code")
	private String code;


	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("Label")
	private String label;

	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("Preferred_Name")
	private String preferredName;

	private Map <String,List<String>> properties = new HashMap <String,List<String>>();
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonProperty("FULL_SYN")
	private List <EvsSynonymFull> synonyms;
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("DEFINITION")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsDefinitionFull> definitions;

	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("ALT_DEFINITION")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsDefinitionFull> altDefinitions;
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("Subconcept")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsSubconcept> subconcepts;

	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("Superconcept")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsSuperconcept> superconcepts;
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("Association")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> associations;
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("InverseAssocation")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> inverseAssociations;

	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("Role")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> roles;
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("InverseRole")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> inverseRoles;	
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("Maps_To")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsMapsTo> mapsTo;
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("GO_Annotation")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsGoAnnotation> goAnnotations;	
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("DisjointWith")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAssociation> disjointWith;	
	
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List <EvsAdditionalProperty> additionalProperties;
	
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

	public List<EvsSynonymFull> getSynonyms() {
		return synonyms;
	}
	public void setSynonyms(List<EvsSynonymFull> synonyms) {
		this.synonyms = synonyms;
	}
	
	public List <EvsDefinitionFull> getDefinitions() {
		return definitions;
	}
	public void setDefinitions(List<EvsDefinitionFull> definitions) {
		this.definitions = definitions;
	}

	public List <EvsDefinitionFull> getAltDefinitions() {
		return altDefinitions;
	}
	public void setAltDefinitions(List<EvsDefinitionFull> altDefinitions) {
		this.altDefinitions = altDefinitions;
	}
	
	public List<EvsSubconcept> getSubconcepts() {
		return subconcepts;
	}
	public void setSubconcepts(List<EvsSubconcept> subconcepts) {
		this.subconcepts = subconcepts;
	}

	public List<EvsSuperconcept> getSuperconcepts() {
		return superconcepts;
	}
	public void setSuperconcepts(List<EvsSuperconcept> superconcepts) {
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
	
	public List <EvsAdditionalProperty> getAdditionalProperties() {
		return additionalProperties;
	}
	public void setAdditionalProperties(List <EvsAdditionalProperty> additionalProperties) {
		this.additionalProperties = additionalProperties;
	}
}
