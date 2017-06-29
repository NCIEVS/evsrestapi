package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nih.nci.evs.api.model.evs.EvsAdditionalProperty;
import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsDefinition;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsSynonym;

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
            "FULL_SYN", "Concept_Status", "Semantic_Type", "Concept_In_Subset"};
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
				synonym.setSubSourceName(axiom.getSubsourceName());
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
				definition.setSource(axiom.getDefSource());
				results.add(definition);
			}
		}
		return results;
	}
	
}
