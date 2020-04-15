
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptNode;
import gov.nih.nci.evs.api.model.ConceptPath;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;

/**
 * Utilities for handling the "include" flag, and converting EVSConcept to
 * Concept.
 */
public final class ConceptUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(ConceptUtils.class);

  /**
   * Instantiates an empty {@link ConceptUtils}.
   */
  private ConceptUtils() {
    // n/a
  }

  /**
   * Apply highlights.
   *
   * @param concept the concept
   * @param highlights the highlights
   * @throws Exception the exception
   */
  public static void applyHighlights(final Concept concept,
    final java.util.Map<String, String> highlights) throws Exception {

    // concept
    if (highlights.containsKey(concept.getName())) {
      concept.setHighlight(highlights.get(concept.getName()));
    }

    // synonyms
    for (final Synonym sn : concept.getSynonyms()) {
      final String value = highlights.get(sn.getName());
      if (value != null) {
        sn.setHighlight(value);
      }
    }

    // definitions
    for (final Definition def : concept.getDefinitions()) {
      final String value = highlights.get(def.getDefinition());
      if (value != null) {
        def.setHighlight(value);
      }
    }

    // properties
    for (final Property prop : concept.getProperties()) {
      String value = highlights.get(prop.getValue());
      if (value != null) {
        prop.setHighlight(value);
      } else {
        value = highlights.get(prop.getType());
        if (value != null) {
          prop.setHighlight(value);
        }
      }
    }

    // TODO - when role search is supported
    // TODO - when association search is supported

  }

  /**
   * Apply include.
   *
   * @param evsConcepts the evs concepts
   * @param ip the ip
   * @param list the list
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> applyIncludeAndList(final List<Concept> evsConcepts,
    final IncludeParam ip, final String list) throws Exception {
    final Set<String> codes = (list == null || list.isEmpty()) ? null
        : Arrays.stream(list.split(",")).collect(Collectors.toSet());

    return evsConcepts.stream()
        .filter(ec -> codes == null || codes.contains(ec.getCode()) || codes.contains(ec.getName()))
        .collect(Collectors.toList());
  }

  /**
   * Convert concepts fy with include.
   *
   * @param service the service
   * @param ip the ip
   * @param terminology the terminology
   * @param concepts the concepts
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> convertConceptsWithInclude(final SparqlQueryManagerService service,
    final IncludeParam ip, final Terminology terminology, final List<Concept> concepts)
    throws Exception {

    // final List<Concept> concepts = convertConcepts(list);
    if (ip.hasAnyTrue()) {
      for (final Concept concept : concepts) {
        final Integer level = concept.getLevel();
        final Boolean leaf = concept.getLeaf();
        concept.populateFrom(service.getConcept(concept.getCode(), terminology, ip));
        concept.setLevel(level);
        concept.setLeaf(leaf);
      }
    }
    return concepts;
  }

  /**
   * Convert concepts from hierarchy.
   *
   * @param list the list
   * @return the list
   */
  public static List<Concept> convertConceptsFromHierarchy(final List<HierarchyNode> list) {
    if (list == null || list.isEmpty()) {
      return new ArrayList<>();
    }
    return list.stream().map(ea -> new Concept(ea)).collect(Collectors.toList());
  }

  /**
   * Convert concepts from hierarchy with include.
   *
   * @param service the service
   * @param ip the ip
   * @param terminology the terminology
   * @param list the list
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> convertConceptsFromHierarchyWithInclude(
    final SparqlQueryManagerService service, final IncludeParam ip, final Terminology terminology,
    final List<HierarchyNode> list) throws Exception {

    final List<Concept> concepts = convertConceptsFromHierarchy(list);
    if (ip.hasAnyTrue()) {
      for (final Concept concept : concepts) {
        final Integer level = concept.getLevel();
        final Boolean leaf = concept.getLeaf();
        concept.populateFrom(service.getConcept(concept.getCode(), terminology, ip));
        concept.setLevel(level);
        concept.setLeaf(leaf);
      }
    }
    return concepts;
  }

  /**
   * Convert paths.
   *
   * @param paths the paths
   * @param reverse the reverse
   * @return the list
   */
  public static List<ConceptPath> convertPaths(final Paths paths, final boolean reverse) {
    final List<ConceptPath> list = new ArrayList<>();
    if (paths == null || paths.getPaths() == null || paths.getPaths().isEmpty()) {
      return list;
    }
    for (final Path path : paths.getPaths()) {
      final ConceptPath concepts = new ConceptPath();
      for (final ConceptNode cn : path.getConcepts()) {
        final Concept concept = new Concept(cn);
        concepts.add(concept);
      }
      // Reverse if indicated
      if (reverse) {
        Collections.reverse(concepts);
      }
      for (int i = 0; i < concepts.size(); i++) {
        concepts.get(i).setLevel(i);
      }
      list.add(concepts);
    }
    return list;
  }

  /**
   * Convert paths with include.
   *
   * @param service the service
   * @param ip the ip
   * @param terminology the terminology
   * @param paths the paths
   * @param reverse the reverse
   * @return the list
   * @throws Exception the exception
   */
  public static List<ConceptPath> convertPathsWithInclude(final SparqlQueryManagerService service,
    final IncludeParam ip, final Terminology terminology, final Paths paths, final boolean reverse)
    throws Exception {

    final List<ConceptPath> list = convertPaths(paths, reverse);
    if (ip.hasAnyTrue()) {
      final java.util.Map<String, Concept> cache = new HashMap<>();
      for (final ConceptPath concepts : list) {
        for (final Concept concept : concepts) {
          final int level = concept.getLevel();
          if (cache.containsKey(concept.getCode())) {
            concept.populateFrom(cache.get(concept.getCode()));
          } else {
            concept.populateFrom(service.getConcept(concept.getCode(), terminology, ip));
            cache.put(concept.getCode(), concept);
          }
          concept.setLevel(level);
        }
      }
    }
    return list;
  }

  /**
   * As map.
   *
   * @param values the values
   * @return the map
   */
  public static Map<String, String> asMap(final String... values) {
    final Map<String, String> map = new HashMap<>();
    if (values.length % 2 != 0) {
      throw new RuntimeException("Unexpected odd number of parameters");
    }
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;

  }
}
