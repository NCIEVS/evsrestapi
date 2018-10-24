package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nih.nci.evs.api.model.evs.EvsAdditionalProperty;
import gov.nih.nci.evs.api.model.evs.EvsAdditionalPropertyByCode;
import gov.nih.nci.evs.api.model.evs.EvsAdditionalPropertyByLabel;
import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsDefinition;
import gov.nih.nci.evs.api.model.evs.EvsDefinitionByCode;
import gov.nih.nci.evs.api.model.evs.EvsDefinitionByLabel;
import gov.nih.nci.evs.api.model.evs.EvsDefinitionFull;
import gov.nih.nci.evs.api.model.evs.EvsGoAnnotation;
import gov.nih.nci.evs.api.model.evs.EvsGoAnnotationByCode;
import gov.nih.nci.evs.api.model.evs.EvsMapsTo;
import gov.nih.nci.evs.api.model.evs.EvsMapsToByCode;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsSynonym;
import gov.nih.nci.evs.api.model.evs.EvsSynonymByCode;
import gov.nih.nci.evs.api.model.evs.EvsSynonymByLabel;
import gov.nih.nci.evs.api.model.evs.EvsSynonymFull;

public class EVSUtils {
	
	public EVSUtils() {}
	
	public static List <String> getProperty(String code, List <EvsProperty>properties) {
		ArrayList <String> results = new ArrayList<String>();
		for ( EvsProperty property: properties) {
			if (property.getCode().equals(code)) {
				results.add(property.getValue());
			}
		}
		return results;
	}

	public static String getConceptCode(List <EvsProperty>properties) {
		List <String> results =  getProperty("NHC0",properties);
		if (results.size() == 0) {
			return null;
		} else {
			return results.get(0);
		}
	} 
	
	public static String getDisplayName(List <EvsProperty>properties) {
		List <String> results =  getProperty("P107",properties);
		if (results.size() == 0) {
			return null;
		} else {
			return results.get(0);
		}
	}

	public static String getPreferredName(List <EvsProperty>properties) {
		List <String> results = getProperty("P108",properties);
		if (results.size() == 0) {
			return null;
		} else {
			return results.get(0);
		}
	}

	public static String getNeoplasticStatus(List <EvsProperty>properties) {
		List <String> results = getProperty("P363",properties);
		if (results.size() == 0) {
			return null;
		} else {
			return results.get(0);
		}
	}

	public static String[] COMMON_PROPERTIES = {"code", "label", "Preferred_Name", "Display_Name", "DEFINITION", "ALT_DEFINITION",
            "FULL_SYN", "Concept_Status", "Semantic_Type"};
	public static List <EvsAdditionalProperty> getAdditionalProperties(List <EvsProperty>properties) {
		List <EvsAdditionalProperty> results = new ArrayList<EvsAdditionalProperty>();
		List commonProperties = Arrays.asList(COMMON_PROPERTIES);
		for (EvsProperty property: properties) {
			if (!commonProperties.contains(property.getLabel())) {
				EvsAdditionalProperty additionalProperty = new EvsAdditionalProperty(property.getLabel(),property.getValue());
			    results.add(additionalProperty);
			}
		}
		return results;
	}

	public static String[] COMMON_PROPERTIES_FULL = {"code", "label", "Preferred_Name", "DEFINITION", "ALT_DEFINITION",
            "FULL_SYN", "Maps_To", "GO_Annotation"};
	public static List <EvsAdditionalProperty> getAdditionalPropertiesFull(List <EvsProperty>properties) {
		List <EvsAdditionalProperty> results = new ArrayList<EvsAdditionalProperty>();
		List commonProperties = Arrays.asList(COMMON_PROPERTIES_FULL);
		for (EvsProperty property: properties) {
			if (!commonProperties.contains(property.getLabel())) {
				EvsAdditionalProperty additionalProperty = new EvsAdditionalProperty(property.getLabel(),property.getValue());
			    results.add(additionalProperty);
			}
		}
		return results;
	}

	public static String[] COMMON_PROPERTIES_BY_LABEL = {"code", "label", "Preferred_Name", "DEFINITION", "ALT_DEFINITION",
            "FULL_SYN", "Maps_To", "GO_Annotation"};
	public static List <EvsAdditionalPropertyByLabel> getAdditionalPropertiesByLabel(List <EvsProperty>properties) {
		List <EvsAdditionalPropertyByLabel> results = new ArrayList<EvsAdditionalPropertyByLabel>();
		List commonProperties = Arrays.asList(COMMON_PROPERTIES_BY_LABEL);
		for (EvsProperty property: properties) {
			if (!commonProperties.contains(property.getLabel())) {
				EvsAdditionalPropertyByLabel additionalProperty = new EvsAdditionalPropertyByLabel(property.getLabel(),property.getValue());
			    results.add(additionalProperty);
			}
		}
		return results;
	}

	public static String[] COMMON_PROPERTIES_BY_CODE = {"code", "label", "Preferred_Name", "DEFINITION", "ALT_DEFINITION",
            "FULL_SYN", "Maps_To", "GO_Annotation"};
	public static List <EvsAdditionalPropertyByCode> getAdditionalPropertiesByCode(List <EvsProperty>properties) {
		List <EvsAdditionalPropertyByCode> results = new ArrayList<EvsAdditionalPropertyByCode>();
		List commonProperties = Arrays.asList(COMMON_PROPERTIES_BY_CODE);
		for (EvsProperty property: properties) {
			if (!commonProperties.contains(property.getLabel())) {
				EvsAdditionalPropertyByCode additionalProperty = new EvsAdditionalPropertyByCode(property.getCode(),property.getValue());
			    results.add(additionalProperty);
			}
		}
		return results;
	}



	public static String getDefinition(List <EvsProperty>properties) {
		List <String> results = getProperty("P97",properties);
		if (results.size() == 0) {
			return null;
		} else {
			return results.get(0);
		}
	}

	public static List <String> getSemanticType(List <EvsProperty>properties) {
		return getProperty("P106",properties);
	}

	public static List <String> getConceptStatus(List <EvsProperty>properties) {
		return getProperty("P310",properties);
	}


	public static List <EvsSynonym> getFullSynonym(List <EvsAxiom>axioms) {
		ArrayList <EvsSynonym> results = new ArrayList<EvsSynonym>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P90")) {
				EvsSynonym synonym = new EvsSynonym();
				synonym.setCode("P90");
				synonym.setLabel(axiom.getAnnotatedTarget());
				synonym.setTermName(axiom.getAnnotatedTarget());
				synonym.setTermGroup(axiom.getTermGroup());
				synonym.setTermSource(axiom.getTermSource());
				synonym.setSourceCode(axiom.getSourceCode());
				synonym.setSubsourceName(axiom.getSubsourceName());
				results.add(synonym);
			}
		}
		return results;
	}
	
	public static List <EvsSynonymFull> getSynonyms(List <EvsAxiom>axioms) {
		ArrayList <EvsSynonymFull> results = new ArrayList<EvsSynonymFull>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P90")) {
				EvsSynonymFull synonym = new EvsSynonymFull();
				synonym.setCode("P90");
				synonym.setLabel(axiom.getAnnotatedTarget());
				synonym.setTermName(axiom.getAnnotatedTarget());
				synonym.setTermGroup(axiom.getTermGroup());
				synonym.setTermSource(axiom.getTermSource());
				synonym.setSourceCode(axiom.getSourceCode());
				synonym.setSubsourceName(axiom.getSubsourceName());
				results.add(synonym);
			}
		}
		return results;
	}

	public static List <EvsSynonymByLabel> getSynonymsByLabel(List <EvsAxiom>axioms) {
		ArrayList <EvsSynonymByLabel> results = new ArrayList<EvsSynonymByLabel>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P90")) {
				EvsSynonymByLabel synonym = new EvsSynonymByLabel();
				synonym.setCode("P90");
				synonym.setLabel(axiom.getAnnotatedTarget());
				synonym.setTermName(axiom.getAnnotatedTarget());
				synonym.setTermGroup(axiom.getTermGroup());
				synonym.setTermSource(axiom.getTermSource());
				synonym.setSourceCode(axiom.getSourceCode());
				synonym.setSubsourceName(axiom.getSubsourceName());
				results.add(synonym);
			}
		}
		return results;
	}

	public static List <EvsSynonymByCode> getSynonymsByCode(List <EvsAxiom>axioms) {
		ArrayList <EvsSynonymByCode> results = new ArrayList<EvsSynonymByCode>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P90")) {
				EvsSynonymByCode synonym = new EvsSynonymByCode();
				synonym.setCode("P90");
				synonym.setLabel(axiom.getAnnotatedTarget());
				synonym.setTermName(axiom.getAnnotatedTarget());
				synonym.setTermGroup(axiom.getTermGroup());
				synonym.setTermSource(axiom.getTermSource());
				synonym.setSourceCode(axiom.getSourceCode());
				synonym.setSubsourceName(axiom.getSubsourceName());
				results.add(synonym);
			}
		}
		return results;
	}



	public static List <EvsDefinition> getFullDefinitions(List <EvsAxiom>axioms) {
		ArrayList <EvsDefinition> results = new ArrayList<EvsDefinition>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P97") || axiom.getAnnotatedProperty().equals("P325")) {
				EvsDefinition definition = new EvsDefinition();
				definition.setDescription(axiom.getAnnotatedTarget());
				definition.setDefSource(axiom.getDefSource());
				definition.setAttr(axiom.getAttr());
				results.add(definition);
			}
		}
		return results;
	}
	
	public static List <EvsDefinitionFull> getDefinitions(List <EvsAxiom>axioms) {
		ArrayList <EvsDefinitionFull> results = new ArrayList<EvsDefinitionFull>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P97")) {
				EvsDefinitionFull definition = new EvsDefinitionFull();
				definition.setDescription(axiom.getAnnotatedTarget());
				definition.setDefSource(axiom.getDefSource());
				definition.setAttr(axiom.getAttr());
				results.add(definition);
			}
		}
		return results;
	}

	public static List <EvsDefinitionByLabel> getDefinitionsByLabel(List <EvsAxiom>axioms) {
		ArrayList <EvsDefinitionByLabel> results = new ArrayList<EvsDefinitionByLabel>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P97")) {
				EvsDefinitionByLabel definition = new EvsDefinitionByLabel();
				definition.setDefinition(axiom.getAnnotatedTarget());
				definition.setDefSource(axiom.getDefSource());
				definition.setAttr(axiom.getAttr());
				results.add(definition);
			}
		}
		return results;
	}

	public static List <EvsDefinitionByCode> getDefinitionsByCode(List <EvsAxiom>axioms) {
		ArrayList <EvsDefinitionByCode> results = new ArrayList<EvsDefinitionByCode>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P97")) {
				EvsDefinitionByCode definition = new EvsDefinitionByCode();
				definition.setDefinition(axiom.getAnnotatedTarget());
				definition.setDefSource(axiom.getDefSource());
				definition.setAttr(axiom.getAttr());
				results.add(definition);
			}
		}
		return results;
	}

	public static List <EvsDefinitionByLabel> getAltDefinitionsByLabel(List <EvsAxiom>axioms) {
		ArrayList <EvsDefinitionByLabel> results = new ArrayList<EvsDefinitionByLabel>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P325")) {
				EvsDefinitionByLabel definition = new EvsDefinitionByLabel();
				definition.setDefinition(axiom.getAnnotatedTarget());
				definition.setDefSource(axiom.getDefSource());
				definition.setAttr(axiom.getAttr());
				results.add(definition);
			}
		}
		return results;
	}

	public static List <EvsDefinitionByCode> getAltDefinitionsByCode(List <EvsAxiom>axioms) {
		ArrayList <EvsDefinitionByCode> results = new ArrayList<EvsDefinitionByCode>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P325")) {
				EvsDefinitionByCode definition = new EvsDefinitionByCode();
				definition.setDefinition(axiom.getAnnotatedTarget());
				definition.setDefSource(axiom.getDefSource());
				definition.setAttr(axiom.getAttr());
				results.add(definition);
			}
		}
		return results;
	}

	public static List <EvsDefinitionFull> getAltDefinitions(List <EvsAxiom>axioms) {
		ArrayList <EvsDefinitionFull> results = new ArrayList<EvsDefinitionFull>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P325")) {
				EvsDefinitionFull definition = new EvsDefinitionFull();
				definition.setDescription(axiom.getAnnotatedTarget());
				definition.setDefSource(axiom.getDefSource());
				definition.setAttr(axiom.getAttr());
				results.add(definition);
			}
		}
		return results;
	}


	public static List <EvsMapsTo> getMapsTo(List <EvsAxiom>axioms) {
		ArrayList <EvsMapsTo> results = new ArrayList<EvsMapsTo>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P375")) {
				EvsMapsTo mapsTo = new EvsMapsTo();
				mapsTo.setAnnotatedTarget(axiom.getAnnotatedTarget());
				mapsTo.setRelationshipToTarget(axiom.getRelationshipToTarget());
				mapsTo.setTargetTermType(axiom.getTargetTermType());
				mapsTo.setTargetCode(axiom.getTargetCode());
				mapsTo.setTargetTerminology(axiom.getTargetTerminology());
				results.add(mapsTo);
			}
		}
		return results;
	}
	public static List <EvsMapsToByCode> getMapsToByCode(List <EvsAxiom>axioms) {
		ArrayList <EvsMapsToByCode> results = new ArrayList<EvsMapsToByCode>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P375")) {
				EvsMapsToByCode mapsTo = new EvsMapsToByCode();
				mapsTo.setAnnotatedTarget(axiom.getAnnotatedTarget());
				mapsTo.setRelationshipToTarget(axiom.getRelationshipToTarget());
				mapsTo.setTargetTermType(axiom.getTargetTermType());
				mapsTo.setTargetCode(axiom.getTargetCode());
				mapsTo.setTargetTerminology(axiom.getTargetTerminology());
				results.add(mapsTo);
			}
		}
		return results;
	}


	public static List <EvsGoAnnotation> getGoAnnotations(List <EvsAxiom>axioms) {
		ArrayList <EvsGoAnnotation> results = new ArrayList<EvsGoAnnotation>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P211")) {
				EvsGoAnnotation go = new EvsGoAnnotation();
				go.setGoId(axiom.getGoId());
				go.setGoTerm(axiom.getAnnotatedTarget());
				go.setGoEvi(axiom.getGoEvi());
				go.setGoSource(axiom.getGoSource());
				go.setSourceDate(axiom.getSourceDate());
				results.add(go);
			}
		}
		return results;
	}

	public static List <EvsGoAnnotationByCode> getGoAnnotationsByCode(List <EvsAxiom>axioms) {
		ArrayList <EvsGoAnnotationByCode> results = new ArrayList<EvsGoAnnotationByCode>();
		for ( EvsAxiom axiom: axioms) {
			if (axiom.getAnnotatedProperty().equals("P211")) {
				EvsGoAnnotationByCode go = new EvsGoAnnotationByCode();
				go.setGoId(axiom.getGoId());
				go.setGoTerm(axiom.getAnnotatedTarget());
				go.setGoEvi(axiom.getGoEvi());
				go.setGoSource(axiom.getGoSource());
				go.setSourceDate(axiom.getSourceDate());
				results.add(go);
			}
		}
		return results;
	}
}
