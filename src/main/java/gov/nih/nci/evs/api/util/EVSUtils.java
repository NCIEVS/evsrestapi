
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import gov.nih.nci.evs.api.model.Axiom;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Qualifier;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;

/**
 * Utilities for handling EVS stuff.
 */
public class EVSUtils {

  /**
   * Returns the qualifier name.
   *
   * @param qualifiers the qualifiers
   * @param code the code
   * @return the qualifier name
   */
  public static String getQualifierName(final List<Concept> qualifiers, final String code) {
    final Optional<Concept> concept =
        qualifiers.stream().filter(c -> c.getCode().equals(code)).findFirst();
    if (concept.isPresent()) {
      return concept.get().getName();
    } else {
      return null;
    }
  }

  /**
   * Returns the property.
   *
   * @param type the type
   * @param properties the properties
   * @return the property
   */
  public static String getProperty(String type, List<Property> properties) {
    ArrayList<String> results = new ArrayList<String>();
    for (Property property : properties) {
      if (property.getType().equals(type)) {
        results.add(property.getValue());
      }
    }
    return results.isEmpty() ? null : results.get(0);
  }

  /**
   * Returns the common property names. These are property names that become
   * other model objects.
   *
   * @return the common property names
   */
  public static Set<String> getCommonPropertyNames(Terminology terminology) {
    // TODO: CONFIG
    return new HashSet<>(Arrays.asList(new String[] {
        "code", "name", "Preferred_Name", "DEFINITION", "ALT_DEFINITION", "FULL_SYN", "Maps_To"
    }));
  }

  /**
   * Returns the synonyms.
   *
   * @param axioms the axioms
   * @return the synonyms
   */
  public static List<Synonym> getSynonyms(List<Axiom> axioms) {
    final List<Synonym> results = new ArrayList<>();
    for (Axiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals("P90")) {
        Synonym synonym = new Synonym();
        // TODO: CONFIG
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
   * @return the definitions
   */
  public static List<Definition> getDefinitions(List<Axiom> axioms) {
    final ArrayList<Definition> results = new ArrayList<>();
    for (Axiom axiom : axioms) {
      // TODO: CONFIG
      if (axiom.getAnnotatedProperty().equals("P97")) {
        Definition definition = new Definition();
        definition.setDefinition(axiom.getAnnotatedTarget());
        definition.setSource(axiom.getDefSource());
        definition.getQualifiers().addAll(axiom.getQualifiers());
        results.add(definition);
      }
      // TODO: CONFIG
      else if (axiom.getAnnotatedProperty().equals("P325")) {
        Definition definition = new Definition();
        definition.setDefinition(axiom.getAnnotatedTarget());
        definition.setSource(axiom.getDefSource());
        definition.getQualifiers().addAll(axiom.getQualifiers());
        definition.setType("ALT_DEFINITION");
        results.add(definition);
      }
    }
    return results;
  }

  /**
   * Returns the qualifiers.
   *
   * @param annotatedProperty the property code
   * @param axioms the axioms
   * @return the qualifiers
   */
  public static List<Qualifier> getQualifiers(final String annotatedProperty,
    final String annotatedTarget, final List<Axiom> axioms) {
    for (final Axiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals(annotatedProperty)
          && axiom.getAnnotatedTarget().equals(annotatedTarget)) {
        return axiom.getQualifiers();
      }
    }
    return new ArrayList<>(0);
  }

  /**
   * Returns the maps to.
   *
   * @param axioms the axioms
   * @return the maps to
   */
  public static List<Map> getMapsTo(List<Axiom> axioms) {
    ArrayList<Map> results = new ArrayList<Map>();
    for (Axiom axiom : axioms) {
      // TODO: CONFIG
      if (axiom.getAnnotatedProperty().equals("P375")) {
        Map mapsTo = new Map();
        mapsTo.setTargetName(axiom.getAnnotatedTarget());
        mapsTo.setType(axiom.getRelationshipToTarget());
        mapsTo.setTargetTermGroup(axiom.getTargetTermType());
        mapsTo.setTargetCode(axiom.getTargetCode());
        mapsTo.setTargetTerminology(axiom.getTargetTerminology());
        mapsTo.setTargetTerminologyVersion(axiom.getTargetTerminologyVersion());
        results.add(mapsTo);
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
  public static <T> List<T> asList(@SuppressWarnings("unchecked") final T... values) {
    final List<T> list = new ArrayList<>(values.length);
    for (final T value : values) {
      if (value != null) {
        list.add(value);
      }
    }
    return list;
  }
}
