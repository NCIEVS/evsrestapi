
package gov.nih.nci.evs.api.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.evs.EvsConcept;

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
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> applyInclude(final List<EvsConcept> evsConcepts,
    final String include) throws Exception {
    return evsConcepts.stream().map(ec -> {
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
    final Concept concept = new Concept();
    final Set<String> includes = (include == null || include.isEmpty()) ? null
        : Arrays.stream(include.split(",")).collect(Collectors.toSet());

    // Apply minimal
    concept.setCode(evsConcept.getCode());
    concept.setName(evsConcept.getLabel());

    // If we're not in minimal mode, handle the other cases
    if (!"minimal".equals(include)) {

      // Add synonyms
      if (includes == null || includes.contains("synonyms")
          || includes.contains("summary") || includes.contains("full")) {
        // "preferred name"
        // any properties ending in "name"
        // evsConcept.getSynonyms();
      }

      // Add definitions

    }

    return concept;
  }
}
