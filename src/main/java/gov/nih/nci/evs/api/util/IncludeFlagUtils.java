
package gov.nih.nci.evs.api.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsDefinition;
import gov.nih.nci.evs.api.model.evs.EvsSynonym;

/**
 * Utilities for handling the "include" flag, and converting EVSConcept to
 * Concept.
 */
public final class IncludeFlagUtils {

  /** The Constant logger. */
  private static final Logger logger =
      LoggerFactory.getLogger(IncludeFlagUtils.class);

  /**
   * Instantiates an empty {@link IncludeFlagUtils}.
   */
  private IncludeFlagUtils() {
    // n/a
  }

  /**
   * Apply include.
   *
   * @param evsConcepts the evs concepts
   * @param include the include
   * @param list the list
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> applyIncludeAndList(
    final List<EvsConcept> evsConcepts, final String include, final String list)
    throws Exception {
    final Set<String> codes = (list == null || list.isEmpty()) ? null
        : Arrays.stream(list.split(",")).collect(Collectors.toSet());

    return evsConcepts.stream().filter(ec -> codes == null
        || codes.contains(ec.getCode()) || codes.contains(ec.getLabel()))
        .map(ec -> {
          try {
            return IncludeFlagUtils.applyInclude(ec, include);
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
   * @param include the include
   * @return the concept
   * @throws Exception the exception
   */
  public static Concept applyInclude(final EvsConcept evsConcept,
    final String include) throws Exception {
    if (evsConcept == null) {
      return null;
    }
    final Concept concept = new Concept();
    final Set<String> includes = (include == null || include.isEmpty()) ? null
        : Arrays.stream(include.split(",")).collect(Collectors.toSet());

    // Apply minimal
    concept.setCode(evsConcept.getCode());
    concept.setName(evsConcept.getLabel());

    // If we're not in minimal mode, handle the other cases
    if (!"minimal".equals(include)) {

      // TODO: finish implementing include

      // Add synonyms
      if (includes == null || includes.contains("synonyms")
          || includes.contains("summary") || includes.contains("full")) {

        // Handle preferred name
        final Synonym pn = new Synonym();
        // TODO: NCI-specific, instead get this from somewhere
        pn.setType("Preferred_Name");
        pn.setName(evsConcept.getPreferredName());
        concept.getSynonyms().add(pn);

        // "Name" based properties
        for (final String key : evsConcept.getProperties().keySet()) {
          if (key.endsWith("_Name")) {
            for (final String value : evsConcept.getProperties().get(key)) {
              final Synonym name = new Synonym();
              name.setType(key);
              name.setName(value);
            }
          }
        }

        for (final EvsSynonym evsSy : evsConcept.getSynonyms()) {
          final Synonym sy = new Synonym(evsSy);
          // TODO: NCI-specific, instead get this from somewhere
          sy.setType("FULL_SYN");
          concept.getSynonyms().add(sy);
        }

      }

      // Add definitions
      if (includes == null || includes.contains("definitions")
          || includes.contains("summary") || includes.contains("full")) {

        for (final EvsDefinition evsDef : evsConcept.getDefinitions()) {
          final Definition def = new Definition(evsDef);
          // TODO: NCI-specific, instead get this from somewhere
          def.setType("DEFINITION");
          concept.getDefinitions().add(def);
        }

        for (final EvsDefinition evsDef : evsConcept.getAltDefinitions()) {
          final Definition def = new Definition(evsDef);
          // TODO: NCI-specific, instead get this from somewhere
          def.setType("ALT_DEFINITION");
          concept.getDefinitions().add(def);
        }

      }

      // TODO: continue with other parts

    }

    return concept;
  }
}
