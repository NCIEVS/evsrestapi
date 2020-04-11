
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nih.nci.evs.api.model.Axiom;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;

/**
 * Utilities for handling EVS stuff.
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
    List<Property> properties) {
    ArrayList<String> results = new ArrayList<String>();
    for (Property property : properties) {
      if (property.getType().equals(code)) {
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
  public static String getConceptCode(List<Property> properties) {
    List<String> results = getProperty("code", properties);
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
  public static String getPreferredName(List<Property> properties) {
    List<String> results = getProperty("Preferred_Name", properties);
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
  public static List<Property> getAdditionalPropertiesByCode(
    List<Property> properties) {
    List<Property> results = new ArrayList<Property>();
    List commonProperties = Arrays.asList(COMMON_PROPERTIES);
    for (Property property : properties) {
      if (!commonProperties.contains(property.getType())) {
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
  public static List<Synonym> getSynonyms(List<Axiom> axioms) {
    ArrayList<Synonym> results = new ArrayList<>();
    for (Axiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P90")) {
        Synonym synonym = new Synonym();
        synonym.setType("FULL_SYN");
        synonym.setCode("P90");
        synonym.setName(axiom.getAnnotatedTarget());
        synonym.setTermGroup(axiom.getTermGroup());
        synonym.setSource(axiom.getTermSource());
        synonym.setCode(axiom.getSourceCode());
        synonym.setSubSource(axiom.getSubsourceName());
        results.add(synonym);
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
  public static List<Definition> getDefinitions(List<Axiom> axioms) {
    ArrayList<Definition> results = new ArrayList<Definition>();
    for (Axiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P97")) {
        Definition definition = new Definition();
        definition.setDefinition(axiom.getAnnotatedTarget());
        definition.setSource(axiom.getDefSource());
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
  public static List<Definition> getAltDefinitions(List<Axiom> axioms) {
    ArrayList<Definition> results = new ArrayList<Definition>();
    for (Axiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P325")) {
        Definition definition = new Definition();
        definition.setDefinition(axiom.getAnnotatedTarget());
        definition.setSource(axiom.getDefSource());
        definition.setType("ALT_DEFINITION");
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
  public static List<Map> getMapsTo(List<Axiom> axioms) {
    ArrayList<Map> results = new ArrayList<Map>();
    for (Axiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P375")) {
        Map mapsTo = new Map();
        mapsTo.setTargetName(axiom.getAnnotatedTarget());
        mapsTo.setType(axiom.getRelationshipToTarget());
        mapsTo.setTargetTermGroup(axiom.getTargetTermType());
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
//  public static List<EvsGoAnnotation> getGoAnnotations(List<EvsAxiom> axioms,
//    String outputType) {
//    ArrayList<EvsGoAnnotation> results = new ArrayList<EvsGoAnnotation>();
//    for (EvsAxiom axiom : axioms) {
//      if (axiom.getAnnotatedProperty().equals("P211")) {
//        EvsGoAnnotation go;
//        if (outputType.equals("byLabel")) {
//          go = new EvsGoAnnotationByLabel();
//        } else {
//          go = new EvsGoAnnotationByCode();
//        }
//        go.setGoId(axiom.getGoId());
//        go.setGoTerm(axiom.getAnnotatedTarget());
//        go.setGoEvi(axiom.getGoEvi());
//        go.setGoSource(axiom.getGoSource());
//        go.setSourceDate(axiom.getSourceDate());
//        results.add(go);
//      }
//    }
//    return results;
//  }

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
