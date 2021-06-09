
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
import org.springframework.util.CollectionUtils;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
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

  public static final String PUNCTUATION_REGEX =
      "[ \\t\\-\\(\\{\\[\\)\\}\\]_!@#%&\\*\\\\:;\\\"',\\.\\?\\/~\\+=\\|<>$`^]";

  /**
   * Normalize.
   *
   * @param value the value
   * @return the string
   */
  public static String normalize(final String value) {
    if (value == null) {
      return null;
    }
    return value.replaceFirst("^[^\\p{IsAlphabetic}\\p{IsDigit}]*", "").toLowerCase()
        .replaceAll(PUNCTUATION_REGEX, " ").replaceAll("\\s+", " ").trim();
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
    } else if (highlights.containsKey(concept.getCode())) {
      concept.setHighlight(highlights.get(concept.getCode()));
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
   * @param concepts the list of concepts
   * @param ip the include param
   * @return the result concepts
   */
  public static List<Concept> applyInclude(final List<Concept> concepts, final IncludeParam ip) {
    if (CollectionUtils.isEmpty(concepts))
      return Collections.emptyList();

    final List<Concept> result = new ArrayList<>(concepts.size());

    for (Concept concept : concepts) {
      Concept newConcept = new Concept();
      newConcept.setCode(concept.getCode());
      newConcept.setName(concept.getName());
      newConcept.setTerminology(concept.getTerminology());
      newConcept.setVersion(concept.getVersion());
      newConcept.setLeaf(concept.getLeaf());

      if (ip.isSynonyms()) {
        newConcept.setSynonyms(concept.getSynonyms());
      }
      if (ip.isDefinitions()) {
        newConcept.setDefinitions(concept.getDefinitions());
      }
      if (ip.isProperties()) {
        newConcept.setProperties(concept.getProperties());
      }
      if (ip.isChildren()) {
        newConcept.setChildren(concept.getChildren());
      }
      if (ip.isParents()) {
        newConcept.setParents(concept.getParents());
      }
      if (ip.isAssociations()) {
        newConcept.setAssociations(concept.getAssociations());
      }
      if (ip.isInverseAssociations()) {
        newConcept.setInverseAssociations(concept.getInverseAssociations());
      }
      if (ip.isRoles()) {
        newConcept.setRoles(concept.getRoles());
      }
      if (ip.isInverseRoles()) {
        newConcept.setInverseRoles(concept.getInverseRoles());
      }
      if (ip.isDisjointWith()) {
        newConcept.setDisjointWith(concept.getDisjointWith());
      }
      if (ip.isMaps()) {
        newConcept.setMaps(concept.getMaps());
      }
      if (ip.isPaths()) {
        newConcept.setPaths(concept.getPaths());
      }
      if (ip.isExtensions()) {
        newConcept.setExtensions(concept.getExtensions());
      }
      if (ip.isDescendants()) {
        newConcept.setDescendants(concept.getDescendants());
      }
      if (ip.isSubsetLink()) {
        log.info("value set link = " + concept.getSubsetLink());
        newConcept.setSubsetLink(concept.getSubsetLink());
      }

      result.add(newConcept);
    }

    return result;
  }

  /**
   * Apply include.
   *
   * @param concepts the list of concepts
   * @param ip the include param
   * @return the result concepts
   */
  public static void applyInclude(Concept concept, final IncludeParam ip) {

    if (concept == null)
      return;

    if (!ip.isSynonyms()) {
      concept.setSynonyms(null);
    }
    if (!ip.isDefinitions()) {
      concept.setDefinitions(null);
    }
    if (!ip.isProperties()) {
      concept.setProperties(null);
    }
    if (!ip.isChildren()) {
      concept.setChildren(null);
    }
    if (!ip.isParents()) {
      concept.setParents(null);
    }
    if (!ip.isAssociations()) {
      concept.setAssociations(null);
    }
    if (!ip.isInverseAssociations()) {
      concept.setInverseAssociations(null);
    }
    if (!ip.isRoles()) {
      concept.setRoles(null);
    }
    if (!ip.isInverseRoles()) {
      concept.setInverseRoles(null);
    }
    if (!ip.isDisjointWith()) {
      concept.setDisjointWith(null);
    }
    if (!ip.isMaps()) {
      concept.setMaps(null);
    }
    if (!ip.isPaths()) {
      concept.setPaths(null);
    }
    if (!ip.isDescendants()) {
      concept.setDescendants(null);
    }

  }

  /**
   * Apply list.
   *
   * @param concepts the evs concepts
   * @param ip the ip
   * @param list the list
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> applyList(final List<Concept> concepts, final IncludeParam ip,
    final String list) throws Exception {
    final Set<String> codes = (list == null || list.isEmpty()) ? null
        : Arrays.stream(list.split(",")).collect(Collectors.toSet());

    return concepts.stream()
        .filter(c -> codes == null || codes.contains(c.getCode()) || codes.contains(c.getName()))
        .collect(Collectors.toList());
  }

  /**
   * Apply include with children.
   *
   * @param concepts the evs concepts
   * @param ip the ip
   * @param list the list
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> applyListWithChildren(final List<Concept> concepts,
    final IncludeParam ip, final String list) throws Exception {
    final Set<String> codes = (list == null || list.isEmpty()) ? null
        : Arrays.stream(list.split(",")).collect(Collectors.toSet());

    return concepts.stream().flatMap(Concept::streamSelfAndChildren)
        .filter(c -> codes == null || codes.contains(c.getCode()) || codes.contains(c.getName()))
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
   * Convert paths.
   *
   * @param paths the paths
   * @param reverse the reverse
   * @return the list
   */
  public static List<List<Concept>> convertPaths(final Paths paths, final boolean reverse) {
    final List<List<Concept>> list = new ArrayList<>();
    if (paths == null || paths.getPaths() == null || paths.getPaths().isEmpty()) {
      return list;
    }
    for (final Path path : paths.getPaths()) {
      final List<Concept> concepts = new ArrayList<Concept>();
      for (final Concept cn : path.getConcepts()) {
        concepts.add(cn);
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
  public static List<List<Concept>> convertPathsWithInclude(final ElasticQueryService service,
    final IncludeParam ip, final Terminology terminology, final Paths paths, final boolean reverse)
    throws Exception {

    final List<List<Concept>> list = convertPaths(paths, reverse);
    // final java.util.Map<String, Concept> cache = new HashMap<>();
    for (final List<Concept> concepts : list) {
      List<String> codes = concepts.stream().map(c -> c.getCode()).collect(Collectors.toList());
      Map<String, Concept> conceptMap = service.getConceptsAsMap(codes, terminology, ip);
      for (final Concept concept : concepts) {
        final int level = concept.getLevel();
        // if (cache.containsKey(concept.getCode())) {
        // concept.populateFrom(cache.get(concept.getCode()));
        // } else {
        concept.populateFrom(conceptMap.get(concept.getCode()));
        // cache.put(concept.getCode(), concept);
        // }
        concept.setLevel(level);
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
