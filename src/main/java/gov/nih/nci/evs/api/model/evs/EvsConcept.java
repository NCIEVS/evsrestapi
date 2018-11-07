package gov.nih.nci.evs.api.model.evs;

import java.util.List;
import java.util.Map;

public interface EvsConcept {

	public void setCode(String code);
	public String getCode();
	public void setLabel(String label);
	public String getLabel();
	public String getPreferredName();
	public void setPreferredName(String preferredName);
	public Map <String,List<String>> getProperties();
	
	public List<EvsSynonym> getSynonyms();
	public void setSynonyms(List<EvsSynonym> synonyms);
	public List<EvsDefinition> getDefinitions();
	public void setDefinitions(List<EvsDefinition> definitions);
	public List<EvsDefinition> getAltDefinitions();
	public void setAltDefinitions(List<EvsDefinition> definitions);
	public List<EvsRelatedConcept> getSubconcepts();
	public void setSubconcepts(List<EvsRelatedConcept> relatedConcepts);
	public List<EvsRelatedConcept> getSuperconcepts();
	public void setSuperconcepts(List<EvsRelatedConcept> relatedConcepts);
	
	public List<EvsAssociation> getAssociations();
	public void setAssociations(List<EvsAssociation> associations);
	public List<EvsAssociation> getInverseAssociations();
	public void setInverseAssociations(List<EvsAssociation> inverseAssociations);
	public List<EvsAssociation> getRoles();
	public void setRoles(List<EvsAssociation> roles);
	public List<EvsAssociation> getInverseRoles();
	public void setInverseRoles(List<EvsAssociation> inverseRoles);
	
	public List<EvsMapsTo> getMapsTo();
	public void setMapsTo(List<EvsMapsTo> mapsTo);
	public List<EvsGoAnnotation> getGoAnnotations();
	public void setGoAnnotations(List<EvsGoAnnotation> goAnnotations);
	public List<EvsAssociation> getDisjointWith();
	public void setDisjointWith(List<EvsAssociation> disjointWith);
}
