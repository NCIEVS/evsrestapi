
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.evs.api.model.Axiom;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Qualifier;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.sparql.Bindings;

/**
 * Utilities for handling EVS stuff.
 */
public class EVSUtils {

  /** The Constant log. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(EVSUtils.class);

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
    for (final Property property : properties) {
      if (property.getCode() != null && property.getCode().equals(type)) {
        return property.getValue();
      }
    }
    return null;
  }

  /**
   * Returns the synonyms.
   *
   * @param terminology the terminology
   * @param axioms the axioms
   * @return the synonyms
   */
  public static List<Synonym> getSynonyms(Terminology terminology, List<Axiom> axioms) {
    final List<Synonym> results = new ArrayList<>();
    final Set<String> syCode = terminology.getMetadata().getSynonym();
    // If 'axioms' is null here, it's likely because the "main" query didn't
    // finish
    for (Axiom axiom : axioms) {
      final String axiomCode = axiom.getAnnotatedProperty();
      if (syCode.contains(axiomCode)) {
        Synonym synonym = new Synonym();
        synonym.setType(terminology.getMetadata().getPropertyName(axiomCode));
        if (synonym.getType() == null) {
          throw new RuntimeException("Unexpected missing name for synonym code = " + axiomCode);
        }
        synonym.setCode(axiomCode);
        synonym.setName(axiom.getAnnotatedTarget());
        synonym.setTermType(axiom.getTermType());
        synonym.setSource(axiom.getTermSource());
        synonym.setCode(axiom.getSourceCode());
        synonym.setSubSource(axiom.getSubsourceName());
        // log.info(" ADD synonym = " + axiomCode + ", " + synonym);
        results.add(synonym);
      }
    }
    return results;
  }

  /**
   * Returns the definitions.
   *
   * @param terminology the terminology
   * @param properties the properties
   * @param axioms the axioms
   * @return the definitions
   */
  public static List<Definition> getDefinitions(Terminology terminology, List<Property> properties,
    List<Axiom> axioms) {
    final ArrayList<Definition> results = new ArrayList<>();
    final Set<String> defCodes = terminology.getMetadata().getDefinition();
    // Check axioms for definitions
    for (final Axiom axiom : axioms) {
      final String axiomCode = axiom.getAnnotatedProperty();
      if (defCodes.contains(axiomCode)) {
        Definition definition = new Definition();
        definition.setDefinition(axiom.getAnnotatedTarget());
        definition.setSource(axiom.getDefSource());
        // Only keep qualifiers without null types (this happend when loading
        // weird owl)
        definition.getQualifiers().addAll(axiom.getQualifiers().stream()
            .filter(q -> q.getType() != null).collect(Collectors.toList()));
        definition.setType(terminology.getMetadata().getPropertyName(axiomCode));
        if (definition.getType() == null) {
          throw new RuntimeException("Unexpected missing name for definition code = " + axiomCode);
        }
        results.add(definition);
      }
    }

    // Check properties for definitions
    for (final Property property : properties) {
      if (defCodes.contains(property.getCode())) {
        Definition definition = new Definition();
        definition.setDefinition(property.getValue());
        definition.setType(terminology.getMetadata().getPropertyName(property.getCode()));

        // TODO: figure out how to get definition source and other qualifiers
        // from axioms?

        results.add(definition);
      }
    }
    return results;
  }

  /**
   * Returns the qualifiers.
   *
   * @param annotatedProperty the property code
   * @param annotatedTarget the annotated target
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
   * @param terminology the terminology
   * @param axioms the axioms
   * @return the maps to
   */
  public static List<Map> getMapsTo(Terminology terminology, List<Axiom> axioms) {
    ArrayList<Map> results = new ArrayList<Map>();
    final String mapCode = terminology.getMetadata().getMap();
    for (Axiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals(mapCode)) {
        Map mapsTo = new Map();
        mapsTo.setTargetName(axiom.getAnnotatedTarget());
        mapsTo.setType(axiom.getRelationshipToTarget());
        mapsTo.setTargetTermType(axiom.getTargetTermType());
        mapsTo.setTargetCode(axiom.getTargetCode());
        mapsTo.setTargetTerminology(axiom.getTargetTerminology());
        mapsTo.setTargetTerminologyVersion(axiom.getTargetTerminologyVersion());
        // log.info(" ADD map = " + mapsTo);
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
  public static <T> List<T> asList(@SuppressWarnings("unchecked")
  final T... values) {
    final List<T> list = new ArrayList<>(values.length);
    for (final T value : values) {
      if (value != null) {
        list.add(value);
      }
    }
    return list;
  }

  /**
   * Returns the code from property.
   *
   * @param property the property
   * @return the code from property
   */
  public static String getCodeFromProperty(final String property) {
    final String code = property.replaceFirst(".*\\/", "");
    if (code.contains("#")) {
      return code.replaceFirst(".*#", "");
    }
    return code;
  }

  /**
   * Returns the code.
   *
   * @param b the b
   * @return the code
   */
  public static String getCode(final Bindings b) {
    if (b.getPropertyCode() == null) {
      // Convert rdfs:label from the property type
      if (b.getProperty().getValue().equals("http://www.w3.org/2000/01/rdf-schema#label")) {
        return "rdfs:label";
      }
      return EVSUtils.getCodeFromProperty(b.getProperty().getValue());
    } else {
      return b.getPropertyCode().getValue();
    }
  }

}
