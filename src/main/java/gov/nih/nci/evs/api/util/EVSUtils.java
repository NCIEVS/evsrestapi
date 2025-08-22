package gov.nih.nci.evs.api.util;

import gov.nih.nci.evs.api.model.Axiom;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Qualifier;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.sparql.Bindings;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilities for handling EVS stuff. */
public class EVSUtils {

  /** The Constant log. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(EVSUtils.class);

  /**
   * Returns the qualifier name.
   *
   * @param qualifiers the qualifiers
   * @param code the code
   * @param uri the uri
   * @return the qualifier name
   */
  public static String getQualifierName(
      final List<Concept> qualifiers, final String code, final String uri) {
    final Optional<Concept> concept =
        qualifiers.stream()
            .filter(c -> c.getCode().equals(code) || uri.equals(c.getUri()))
            .findFirst();
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
    String rdfsLabel = null;
    for (final Property property : properties) {
      if (property.getCode() != null && property.getCode().equals(type)) {
        return property.getValue();
      }
      if ("rdfs:label".equals(property.getCode())) {
        rdfsLabel = property.getValue();
      }
    }

    return rdfsLabel;
  }

  /**
   * Returns the synonyms.
   *
   * @param terminology the terminology
   * @param properties the properties
   * @param axioms the axioms
   * @return the synonyms
   */
  public static List<Synonym> getSynonyms(
      Terminology terminology, List<Property> properties, List<Axiom> axioms) {
    final List<Synonym> results = new ArrayList<>();
    final Set<String> syCode = terminology.getMetadata().getSynonym();
    final Set<String> sySeen = new HashSet<>();

    // If 'axioms' is null here, it's likely because the "main" query didn't
    // finish
    for (Axiom axiom : axioms) {
      // log.info("AXIOM: {}", axiom);
      final String axiomCode = EVSUtils.getQualifiedCodeFromUri(axiom.getAnnotatedProperty());
      if (syCode.contains(axiomCode)) {
        Synonym synonym = new Synonym();
        // This shouldn't happen unless axiomCode and property codes are off

        // NOTE: this could be more efficient if we pass in a map of
        // synonym type metadata
        final Property typeProperty =
            properties.stream().filter(p -> p.getCode().equals(axiomCode)).findFirst().get();
        synonym.setTypeCode(typeProperty.getCode());
        synonym.setType(typeProperty.getType());

        if (synonym.getType() == null) {
          throw new RuntimeException("Unexpected missing name for synonym code = " + axiomCode);
        }
        synonym.setCode(axiomCode);
        synonym.setName(axiom.getAnnotatedTarget());
        synonym.setNormName(ConceptUtils.normalize(synonym.getName()));
        synonym.setTermType(axiom.getTermType());
        synonym.setSource(axiom.getTermSource());
        synonym.setCode(axiom.getSourceCode());
        synonym.setSubSource(axiom.getSubsourceName());

        // Add any qualifiers to the synonym
        synonym
            .getQualifiers()
            .addAll(EVSUtils.getQualifiers(synonym.getTypeCode(), synonym.getName(), axioms));

        // Record for later comparison
        sySeen.add(synonym.getType() + synonym.getName());

        // log.info(" ADD synonym = " + axiomCode + ", " + synonym);
        results.add(synonym);
      }
    }

    // Gather synonyms from properties
    for (final Property property : properties) {

      if (syCode.contains(property.getCode())
          && !sySeen.contains(property.getType() + property.getValue())) {
        // add synonym
        final Synonym synonym = new Synonym();
        synonym.setTypeCode(property.getCode());
        synonym.setType(property.getType());
        synonym.setName(property.getValue());
        synonym.setNormName(ConceptUtils.normalize(synonym.getName()));

        // Any synonyms found here won't have qualifiers

        sySeen.add(synonym.getType() + synonym.getName());
        results.add(synonym);

        continue;
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
  public static List<Definition> getDefinitions(
      Terminology terminology, List<Property> properties, List<Axiom> axioms) {
    final List<Definition> results = new ArrayList<>();
    final Set<String> defCodes = terminology.getMetadata().getDefinition();
    final Set<String> defSeen = new HashSet<>();

    // Check axioms for definitions
    for (final Axiom axiom : axioms) {
      final String axiomCode = EVSUtils.getQualifiedCodeFromUri(axiom.getAnnotatedProperty());
      if (defCodes.contains(axiomCode)) {
        Definition definition = new Definition();
        definition.setDefinition(axiom.getAnnotatedTarget());
        definition.setSource(axiom.getDefSource());
        // Only keep qualifiers without null types (this happened when loading
        // weird owl)
        definition
            .getQualifiers()
            .addAll(
                axiom.getQualifiers().stream()
                    .filter(q -> q.getType() != null)
                    .collect(Collectors.toList()));

        // NOTE: this could be more efficient if we pass in a map of
        // definition type metadata
        final Property typeProperty =
            properties.stream().filter(p -> p.getCode().equals(axiomCode)).findFirst().get();
        definition.setCode(typeProperty.getCode());
        definition.setType(typeProperty.getType());
        if (definition.getType() == null) {
          throw new RuntimeException("Unexpected missing name for definition code = " + axiomCode);
        }

        defSeen.add(definition.getType() + definition.getDefinition().trim());
        results.add(definition);
      }
    }

    // Check properties for definitions if axioms didn't produce them
    for (final Property property : properties) {
      if (defCodes.contains(property.getCode())
          && !defSeen.contains(property.getType() + property.getValue().trim())) {
        final Definition definition = new Definition();
        definition.setDefinition(property.getValue());
        definition.setCode(property.getCode());
        definition.setType(property.getType());

        // Definitions made here won't have qualifiers

        defSeen.add(definition.getType() + definition.getDefinition().trim());
        results.add(definition);
      }
    }

    // Special handling for "complex definition" (like NPO) definitions
    for (final Definition def : results) {
      if (def.getDefinition().startsWith("<ncicp:ComplexDefinition")
          && def.getDefinition().contains("ncicp:def-definition")) {

        // e.g. <ncicp:ComplexDefinition
        // xmlns:ncicp="http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#">
        // <ncicp:def-definition>For definition, please refer to Gene Ontology (v.
        // 1.1.2341)</ncicp:def-definition><ncicp:attr>Gene Ontology (v.
        // 1.1.2341)</ncicp:attr></ncicp:ComplexDefinition>

        // NOTE: there are other parts of complex definitions that we are not rendering
        // ncicp:Definition_Review_Date
        // ncicp:Definition_Reviewer_Name
        // ncicp:def-source

        // Extract the "attribution" parts
        // Pattern to find the content within <ncicp:attr> tags
        final Pattern attrPattern = Pattern.compile("<ncicp:attr>(.*?)</ncicp:attr>");
        final Matcher attrMatcher = attrPattern.matcher(def.getDefinition());

        // Iterate through the matches and extract the attribute values
        while (attrMatcher.find()) {
          final Qualifier qualifier = new Qualifier();
          qualifier.setType("attribution");
          qualifier.setValue(attrMatcher.group(1));
          def.getQualifiers().add(qualifier);
        }

        // Extract the "definition" part
        final String definition =
            def.getDefinition()
                .replaceFirst(".*<ncicp:def-definition>(.*)</ncicp:def-definition>.*", "$1");
        def.setDefinition(definition);
      }
    }
    return results;
  }

  /**
   * Returns the qualifiers.
   *
   * @param code the property code
   * @param value the annotated target
   * @param axioms the axioms
   * @return the qualifiers
   */
  public static List<Qualifier> getQualifiers(
      final String code, final String value, final List<Axiom> axioms) {
    for (final Axiom axiom : axioms) {
      if (axiom.getAnnotatedProperty().equals(code) && axiom.getAnnotatedTarget().equals(value)) {
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
  public static List<Mapping> getMapsTo(Terminology terminology, List<Axiom> axioms) {
    ArrayList<Mapping> results = new ArrayList<Mapping>();
    final String mapCode = terminology.getMetadata().getMap();
    for (Axiom axiom : axioms) {
      final String axiomCode = EVSUtils.getQualifiedCodeFromUri(axiom.getAnnotatedProperty());
      if (axiomCode.equals(mapCode)) {
        Mapping mapsTo = new Mapping();
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
  public static <T> List<T> asList(@SuppressWarnings("unchecked") final T... values) {
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
   * @param uri the property
   * @return the code from property
   */
  public static String getCodeFromUri(final String uri) {
    // Replace up to the last slash
    final String code = uri.replaceFirst(".*\\/", "");

    // If there's a #, replace everything up to it.
    if (code.contains("#")) {
      return code.replaceFirst(".*#", "");
    }

    return code;
  }

  /**
   * Returns the qualified code from uri.
   *
   * @param uri the uri
   * @return the qualified code from uri
   */
  public static String getQualifiedCodeFromUri(final String uri) {
    // Replace up to the last slash and fix rdfs
    String code = uri.replaceFirst(".*\\/", "").replaceFirst("rdf-schema", "rdfs");
    if (code.contains(".")) {
      // Remove up to the hash if the thing before is like "HGNC.owl"
      return code.replaceFirst(".*#", "");
    }
    // otherwise, use what's before the hash as a prefix
    return code.replaceFirst("#", ":");
  }

  /**
   * Returns the label from uri.
   *
   * @param uri the uri
   * @return the label from uri
   */
  public static String getLabelFromUri(final String uri) {
    // Only convert if it's an actual URI
    if (!uri.startsWith("http")) {
      return uri;
    }
    // Replace up to the last slash
    String code = uri.replaceFirst(".*\\/", "");
    // If there's a #, replace everything up to it.
    if (code.contains("#")) {
      code = code.replaceFirst(".*#", "");
    }
    return code;
  }

  /**
   * Returns the code.
   *
   * @param b the b
   * @return the code
   */
  public static String getPropertyCode(final Bindings b) {
    if (b.getPropertyCode() == null) {
      return EVSUtils.getQualifiedCodeFromUri(b.getProperty().getValue());
    } else {
      return b.getPropertyCode().getValue();
    }
  }

  /**
   * Returns the label.
   *
   * @param b the b
   * @return the label
   */
  public static String getPropertyLabel(final Bindings b) {
    if (b.getPropertyLabel() == null) {
      // Convert
      if (b.getProperty().getValue().startsWith("http://www.w3.org/2000/01/rdf-schema")) {
        return EVSUtils.getQualifiedCodeFromUri(b.getProperty().getValue());
      }
      return EVSUtils.getLabelFromUri(b.getProperty().getValue());
    } else {
      return b.getPropertyLabel().getValue();
    }
  }

  /**
   * Returns the relationship type.
   *
   * @param b the b
   * @return the relationship type
   */
  public static String getRelationshipType(final Bindings b) {
    if (b.getRelationshipLabel() == null) {
      if (b.getRelationship().getValue().startsWith("http://www.w3.org/2000/01/rdf-schema")) {
        return EVSUtils.getQualifiedCodeFromUri(b.getRelationship().getValue());
      }
      return EVSUtils.getLabelFromUri(b.getRelationship().getValue());
    } else {
      return b.getRelationshipLabel().getValue();
    }
  }

  /**
   * Returns the relationship code.
   *
   * @param b the b
   * @return the relationship code
   */
  public static String getRelationshipCode(final Bindings b) {
    if (b.getRelationshipCode() == null) {
      return EVSUtils.getQualifiedCodeFromUri(b.getRelationship().getValue());
    } else {
      return b.getRelationshipCode().getValue();
    }
  }

  /**
   * Returns the related concept label.
   *
   * @param b the b
   * @return the related concept label
   */
  public static String getRelatedConceptLabel(final Bindings b) {
    if (b.getRelatedConceptLabel() == null) {
      return EVSUtils.getLabelFromUri(b.getRelatedConcept().getValue());
    } else {
      return b.getRelatedConceptLabel().getValue();
    }
  }

  /**
   * Returns the related concept code.
   *
   * @param b the b
   * @return the related concept code
   */
  public static String getRelatedConceptCode(final Bindings b) {
    if (b.getRelatedConceptCode() == null) {
      return EVSUtils.getCodeFromUri(b.getRelatedConcept().getValue());
    } else {
      return b.getRelatedConceptCode().getValue();
    }
  }

  /**
   * Returns the parent label.
   *
   * @param b the b
   * @return the parent label
   */
  public static String getParentLabel(final Bindings b) {
    if (b.getParentLabel() == null) {
      return EVSUtils.getLabelFromUri(b.getParent().getValue());
    } else {
      return b.getParentLabel().getValue();
    }
  }

  /**
   * Returns the parent code.
   *
   * @param b the b
   * @return the parent code
   */
  public static String getParentCode(final Bindings b) {
    if (b.getParentCode() == null) {
      return EVSUtils.getCodeFromUri(b.getParent().getValue());
    } else {
      return b.getParentCode().getValue();
    }
  }

  /**
   * Returns the child label.
   *
   * @param b the b
   * @return the child label
   */
  public static String getChildLabel(final Bindings b) {
    if (b.getChildLabel() == null) {
      return EVSUtils.getLabelFromUri(b.getChild().getValue());
    } else {
      return b.getChildLabel().getValue();
    }
  }

  /**
   * Returns the child code.
   *
   * @param b the b
   * @return the child code
   */
  public static String getChildCode(final Bindings b) {
    if (b.getChildCode() == null) {
      return EVSUtils.getCodeFromUri(b.getChild().getValue());
    } else {
      return b.getChildCode().getValue();
    }
  }

  /**
   * Returns the concept label.
   *
   * @param b the b
   * @return the concept label
   */
  public static String getConceptLabel(final Bindings b) {
    if (b.getConceptLabel() == null) {
      return EVSUtils.getLabelFromUri(b.getConcept().getValue());
    } else {
      return b.getConceptLabel().getValue();
    }
  }

  /**
   * returns the read value from the given uri (local or repository file).
   *
   * @param uri the uri to read from
   * @param info the info
   * @return the value from file
   * @info the info needing to be read (mostly for error message specificity)
   */
  public static List<String> getValueFromFile(String uri, String info) throws Exception {
    try {
      try (final InputStream is = new URL(uri).openConnection().getInputStream()) {
        return IOUtils.readLines(is, "UTF-8");
      }
    } catch (final Throwable t) {
      try {
        // Try to open URI as a file
        final File file = new File(uri);
        return FileUtils.readLines(file, "UTF-8");
      } catch (Exception e2) {
        throw new Exception("Unable to get data from = " + uri);
      }
    }
  }
}
