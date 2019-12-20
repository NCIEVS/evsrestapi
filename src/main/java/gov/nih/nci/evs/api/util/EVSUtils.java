
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsDefinition;
import gov.nih.nci.evs.api.model.evs.EvsDefinitionByCode;
import gov.nih.nci.evs.api.model.evs.EvsDefinitionByLabel;
import gov.nih.nci.evs.api.model.evs.EvsGoAnnotation;
import gov.nih.nci.evs.api.model.evs.EvsGoAnnotationByCode;
import gov.nih.nci.evs.api.model.evs.EvsGoAnnotationByLabel;
import gov.nih.nci.evs.api.model.evs.EvsMapsTo;
import gov.nih.nci.evs.api.model.evs.EvsMapsToByCode;
import gov.nih.nci.evs.api.model.evs.EvsMapsToByLabel;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsSynonym;
import gov.nih.nci.evs.api.model.evs.EvsSynonymByCode;
import gov.nih.nci.evs.api.model.evs.EvsSynonymByLabel;

/**
 * The Class EVSUtils.
 */
public class EVSUtils {

  /**
   * Returns the property.
   *
   * @param code the code
   * @param properties the properties
   * @return the property
   */
  public static List<String> getProperty(String code,
    List<EvsProperty> properties) {
    ArrayList<String> results = new ArrayList<String>();
    for (EvsProperty property : properties) {
      if (property.getCode().equals(code)) {
        results.add(property.getValue());
      }
    }
    return results;
  }

  /**
   * Returns the concept code.
   *
   * @param properties the properties
   * @return the concept code
   */
  public static String getConceptCode(List<EvsProperty> properties) {
    List<String> results = getProperty("NHC0", properties);
    if (results.size() == 0) {
      return null;
    } else {
      return results.get(0);
    }
  }

  /**
   * Returns the preferred name.
   *
   * @param properties the properties
   * @return the preferred name
   */
  public static String getPreferredName(List<EvsProperty> properties) {
    List<String> results = getProperty("P108", properties);
    if (results.size() == 0) {
      return null;
    } else {
      return results.get(0);
    }
  }

  /** The common properties. */
  public static String[] COMMON_PROPERTIES = {
      "code", "label", "Preferred_Name", "DEFINITION", "ALT_DEFINITION",
      "FULL_SYN", "Maps_To", "GO_Annotation"
  };

  /**
   * Returns the additional properties by code.
   *
   * @param properties the properties
   * @return the additional properties by code
   */
  @SuppressWarnings("rawtypes")
  public static List<EvsProperty> getAdditionalPropertiesByCode(
    List<EvsProperty> properties) {
    List<EvsProperty> results = new ArrayList<EvsProperty>();
    List commonProperties = Arrays.asList(COMMON_PROPERTIES);
    for (EvsProperty property : properties) {
      if (!commonProperties.contains(property.getLabel())) {
        results.add(property);
      }
    }
    return results;
  }

  /**
   * Returns the synonyms.
   *
   * @param axioms the axioms
   * @param outputType the output type
   * @return the synonyms
   */
  public static List<EvsSynonym> getSynonyms(List<EvsAxiom> axioms,
    String outputType) {
    ArrayList<EvsSynonym> results = new ArrayList<EvsSynonym>();
    for (EvsAxiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P90")) {
        EvsSynonym synonym;
        if (outputType.equals("byLabel")) {
          synonym = new EvsSynonymByLabel();
        } else {
          synonym = new EvsSynonymByCode();
        }
        synonym.setCode("P90");
        synonym.setLabel(axiom.getAnnotatedTarget());
        synonym.setTermName(axiom.getAnnotatedTarget());
        synonym.setTermGroup(axiom.getTermGroup());
        synonym.setTermSource(axiom.getTermSource());
        synonym.setSourceCode(axiom.getSourceCode());
        synonym.setSubsourceName(axiom.getSubsourceName());
        results.add((EvsSynonym) synonym);
      }
    }
    return results;
  }

  /**
   * Returns the definitions.
   *
   * @param axioms the axioms
   * @param outputType the output type
   * @return the definitions
   */
  public static List<EvsDefinition> getDefinitions(List<EvsAxiom> axioms,
    String outputType) {
    ArrayList<EvsDefinition> results = new ArrayList<EvsDefinition>();
    for (EvsAxiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P97")) {
        EvsDefinition definition;
        if (outputType.equals("byLabel")) {
          definition = new EvsDefinitionByLabel();
        } else {
          definition = new EvsDefinitionByCode();
        }
        definition.setDefinition(axiom.getAnnotatedTarget());
        definition.setDefSource(axiom.getDefSource());
        definition.setAttr(axiom.getAttr());
        results.add(definition);
      }
    }
    return results;
  }

  /**
   * Returns the alt definitions.
   *
   * @param axioms the axioms
   * @param outputType the output type
   * @return the alt definitions
   */
  public static List<EvsDefinition> getAltDefinitions(List<EvsAxiom> axioms,
    String outputType) {
    ArrayList<EvsDefinition> results = new ArrayList<EvsDefinition>();
    for (EvsAxiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P325")) {
        EvsDefinition definition;
        if (outputType.equals("byLabel")) {
          definition = new EvsDefinitionByLabel();
        } else {
          definition = new EvsDefinitionByCode();
        }
        definition.setDefinition(axiom.getAnnotatedTarget());
        definition.setDefSource(axiom.getDefSource());
        definition.setAttr(axiom.getAttr());
        results.add(definition);
      }
    }
    return results;
  }

  /**
   * Returns the maps to.
   *
   * @param axioms the axioms
   * @param outputType the output type
   * @return the maps to
   */
  public static List<EvsMapsTo> getMapsTo(List<EvsAxiom> axioms,
    String outputType) {
    ArrayList<EvsMapsTo> results = new ArrayList<EvsMapsTo>();
    for (EvsAxiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P375")) {
        EvsMapsTo mapsTo;
        if (outputType.equals("byLabel")) {
          mapsTo = new EvsMapsToByLabel();
        } else {
          mapsTo = new EvsMapsToByCode();
        }
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

  /**
   * Returns the go annotations.
   *
   * @param axioms the axioms
   * @param outputType the output type
   * @return the go annotations
   */
  public static List<EvsGoAnnotation> getGoAnnotations(List<EvsAxiom> axioms,
    String outputType) {
    ArrayList<EvsGoAnnotation> results = new ArrayList<EvsGoAnnotation>();
    for (EvsAxiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P211")) {
        EvsGoAnnotation go;
        if (outputType.equals("byLabel")) {
          go = new EvsGoAnnotationByLabel();
        } else {
          go = new EvsGoAnnotationByCode();
        }
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

  /**
   * As list.
   *
   * @param <T> the
   * @param values the values
   * @return the list
   */
  public static <T> List<T> asList(
    @SuppressWarnings("unchecked") final T... values) {
    final List<T> list = new ArrayList<>(values.length);
    for (final T value : values) {
      if (value != null) {
        list.add(value);
      }
    }
    return list;
  }
}
