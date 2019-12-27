
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.evs.ConceptNode;
import gov.nih.nci.evs.api.model.evs.EvsAssociation;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsDefinition;
import gov.nih.nci.evs.api.model.evs.EvsMapsTo;
import gov.nih.nci.evs.api.model.evs.EvsRelatedConcept;
import gov.nih.nci.evs.api.model.evs.EvsSynonym;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Path;
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;

/**
 * Utilities for handling the "include" flag, and converting EVSConcept to
 * Concept.
 */
public final class ConceptUtils {

  /** The Constant logger. */
  private static final Logger logger =
      LoggerFactory.getLogger(ConceptUtils.class);

  /**
   * Instantiates an empty {@link ConceptUtils}.
   */
  private ConceptUtils() {
    // n/a
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
  public static List<Concept> applyIncludeAndList(
    final List<EvsConcept> evsConcepts, final IncludeParam ip,
    final String list) throws Exception {
    final Set<String> codes = (list == null || list.isEmpty()) ? null
        : Arrays.stream(list.split(",")).collect(Collectors.toSet());

    return evsConcepts.stream().filter(ec -> codes == null
        || codes.contains(ec.getCode()) || codes.contains(ec.getLabel()))
        .map(ec -> {
          try {
            return ConceptUtils.convertConcept(ec);
          } catch (Exception e) {
            logger.error("Unexpected failure converting to concept", e);
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toList());
  }

  /**
   * Apply include.
   *
   * @param evsConcept the evs concept
   * @return the concept
   * @throws Exception the exception
   */
  public static Concept convertConcept(final EvsConcept evsConcept)
    throws Exception {
    if (evsConcept == null) {
      return null;
    }
    final Concept concept = new Concept();

    // Apply minimal always
    concept.setCode(evsConcept.getCode());
    concept.setName(evsConcept.getLabel());

    // Handle preferred name
    if (evsConcept.getPreferredName() != null) {
      final Synonym pn = new Synonym();
      // TODO: NCI-specific, instead get this from somewhere
      pn.setType("Preferred_Name");
      pn.setName(evsConcept.getPreferredName());
      concept.getSynonyms().add(pn);
    }

    // "Name" based properties
    if (evsConcept.getProperties() != null) {
      for (final String key : evsConcept.getProperties().keySet()) {
        if (key.endsWith("_Name")) {
          for (final String value : evsConcept.getProperties().get(key)) {
            final Synonym name = new Synonym();
            name.setType(key);
            name.setName(value);
            concept.getSynonyms().add(name);
          }
        }
      }
    }

    // Synonyms
    if (evsConcept.getSynonyms() != null) {
      for (final EvsSynonym evsSy : evsConcept.getSynonyms()) {
        final Synonym sy = new Synonym(evsSy);
        // TODO: NCI-specific, instead get this from somewhere
        sy.setType("FULL_SYN");
        concept.getSynonyms().add(sy);
      }
    }

    // Add definitions
    if (evsConcept.getDefinitions() != null) {

      for (final EvsDefinition evsDef : evsConcept.getDefinitions()) {
        final Definition def = new Definition(evsDef);
        // TODO: NCI-specific, instead get this from somewhere
        def.setType("DEFINITION");
        concept.getDefinitions().add(def);
      }
    }
    if (evsConcept.getAltDefinitions() != null) {

      for (final EvsDefinition evsDef : evsConcept.getAltDefinitions()) {
        final Definition def = new Definition(evsDef);
        // TODO: NCI-specific, instead get this from somewhere
        def.setType("ALT_DEFINITION");
        concept.getDefinitions().add(def);
      }

    }

    // Add properties
    if (evsConcept.getProperties() != null) {
      for (final String key : evsConcept.getProperties().keySet()) {
        if (!key.endsWith("_Name")) {
          for (final String value : evsConcept.getProperties().get(key)) {
            final Property property = new Property();
            property.setType(key);
            property.setValue(value);
            concept.getProperties().add(property);
          }
        }
      }
    }

    // Add children
    if (evsConcept.getSubconcepts() != null) {
      for (final EvsRelatedConcept ec : evsConcept.getSubconcepts()) {
        concept.getChildren().add(new Concept(ec));
      }
    }

    // Add parents
    if (evsConcept.getSuperconcepts() != null) {
      for (final EvsRelatedConcept ec : evsConcept.getSuperconcepts()) {
        concept.getParents().add(new Concept(ec));
      }
    }

    // Add associations
    if (evsConcept.getAssociations() != null) {
      for (final EvsAssociation ea : evsConcept.getAssociations()) {
        concept.getAssociations().add(new Association(ea));
      }
    }

    // Add inverse associations
    if (evsConcept.getInverseAssociations() != null) {
      for (final EvsAssociation ea : evsConcept.getInverseAssociations()) {
        concept.getInverseAssociations().add(new Association(ea));
      }
    }

    // Add roles
    if (evsConcept.getRoles() != null) {
      for (final EvsAssociation ea : evsConcept.getRoles()) {
        concept.getRoles().add(new Role(ea));
      }
    }

    // Add inverse roles
    if (evsConcept.getInverseRoles() != null) {
      for (final EvsAssociation ea : evsConcept.getInverseRoles()) {
        concept.getInverseRoles().add(new Role(ea));
      }
    }

    // Add maps
    if (evsConcept.getMapsTo() != null) {
      for (final EvsMapsTo em : evsConcept.getMapsTo()) {
        concept.getMaps().add(new Map(em));
      }
    }

    // Add disjoint
    if (evsConcept.getDisjointWith() != null) {
      for (final EvsAssociation ea : evsConcept.getDisjointWith()) {
        concept.getDisjointWith().add(new DisjointWith(ea));
      }
    }

    return concept;
  }

  /**
   * Convert associations.
   *
   * @param list the list
   * @return the list
   */
  public static List<Association> convertAssociations(
    final List<EvsAssociation> list) {
    if (list == null || list.isEmpty()) {
      return new ArrayList<>();
    }
    return list.stream().map(ea -> new Association(ea))
        .collect(Collectors.toList());
  }

  /**
   * Convert roles.
   *
   * @param list the list
   * @return the list
   */
  public static List<Role> convertRoles(final List<EvsAssociation> list) {
    if (list == null || list.isEmpty()) {
      return new ArrayList<>();
    }
    return list.stream().map(ea -> new Role(ea)).collect(Collectors.toList());
  }

  /**
   * Convert maps.
   *
   * @param list the list
   * @return the list
   */
  public static List<Map> convertMaps(final List<EvsMapsTo> list) {
    if (list == null || list.isEmpty()) {
      return new ArrayList<>();
    }
    return list.stream().map(ea -> new Map(ea)).collect(Collectors.toList());
  }

  /**
   * Convert concepts.
   *
   * @param list the list
   * @return the list
   */
  public static List<Concept> convertConcepts(
    final List<EvsRelatedConcept> list) {
    if (list == null || list.isEmpty()) {
      return new ArrayList<>();
    }
    return list.stream().map(ea -> new Concept(ea))
        .collect(Collectors.toList());
  }

  /**
   * Convert concepts fy with include.
   *
   * @param service the service
   * @param ip the ip
   * @param dbType the db type
   * @param list the list
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> convertConceptsWithInclude(
    final SparqlQueryManagerService service, final IncludeParam ip,
    final String dbType, final List<EvsRelatedConcept> list) throws Exception {

    final List<Concept> concepts = convertConcepts(list);
    if (ip.hasAnyTrue()) {
      for (final Concept concept : concepts) {
        final Integer level = concept.getLevel();
        final Boolean leaf = concept.getLeaf();
        concept.populateFrom(ConceptUtils.convertConcept(
            service.getEvsConceptByCode(concept.getCode(), dbType, ip)));
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
  public static List<Concept> convertConceptsFromHierarchy(
    final List<HierarchyNode> list) {
    if (list == null || list.isEmpty()) {
      return new ArrayList<>();
    }
    return list.stream().map(ea -> new Concept(ea))
        .collect(Collectors.toList());
  }

  /**
   * Convert concepts from hierarchy with include.
   *
   * @param service the service
   * @param ip the ip
   * @param dbType the db type
   * @param list the list
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> convertConceptsFromHierarchyWithInclude(
    final SparqlQueryManagerService service, final IncludeParam ip,
    final String dbType, final List<HierarchyNode> list) throws Exception {

    final List<Concept> concepts = convertConceptsFromHierarchy(list);
    if (ip.hasAnyTrue()) {
      for (final Concept concept : concepts) {
        final Integer level = concept.getLevel();
        final Boolean leaf = concept.getLeaf();
        concept.populateFrom(ConceptUtils.convertConcept(
            service.getEvsConceptByCode(concept.getCode(), dbType, ip)));
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
  public static List<List<Concept>> convertPaths(final Paths paths,
    final boolean reverse) {
    final List<List<Concept>> list = new ArrayList<>();
    if (paths == null || paths.getPaths() == null
        || paths.getPaths().isEmpty()) {
      return list;
    }
    for (final Path path : paths.getPaths()) {
      final List<Concept> concepts = new ArrayList<>();
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
   * @param dbType the db type
   * @param paths the paths
   * @param reverse the reverse
   * @return the list
   * @throws Exception the exception
   */
  public static List<List<Concept>> convertPathsWithInclude(
    final SparqlQueryManagerService service, final IncludeParam ip,
    final String dbType, final Paths paths, final boolean reverse)
    throws Exception {

    final List<List<Concept>> list = convertPaths(paths, reverse);
    if (ip.hasAnyTrue()) {
      final java.util.Map<String, Concept> cache = new HashMap<>();
      for (final List<Concept> concepts : list) {
        for (final Concept concept : concepts) {
          final int level = concept.getLevel();
          if (cache.containsKey(concept.getCode())) {
            concept.populateFrom(cache.get(concept.getCode()));
          } else {
            concept.populateFrom(ConceptUtils.convertConcept(
                service.getEvsConceptByCode(concept.getCode(), dbType, ip)));
            cache.put(concept.getCode(), concept);
          }
          concept.setLevel(level);
        }
      }
    }
    return list;
  }

}
