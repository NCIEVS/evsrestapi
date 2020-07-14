
package gov.nih.nci.evs.api.util;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;

/**
 * Utilities for handling the "include" flag, and converting EVSConcept to
 * Concept.
 */
public final class TerminologyUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(TerminologyUtils.class);

  /**
   * Instantiates an empty {@link TerminologyUtils}.
   */
  private TerminologyUtils() {
    // n/a
  }

  /**
   * Returns the terminologies.
   * 
   * Note: To be used only once, during initialization. Subsequently calls to get terminologies
   * should use {@code SparqlQueryManagerService.getTerminologies()}
   *
   * @param sparqlQueryManagerService the sparql query manager service
   *
   * @return the terminologies
   * @throws Exception Signals that an exception has occurred.
   */
  public static List<Terminology> getTerminologies(final SparqlQueryManagerService sparqlQueryManagerService) throws Exception {
    return sparqlQueryManagerService.getTerminologies();
  }

  /**
   * Returns the terminology.
   *
   * @param sparqlQueryManagerService the sparql query manager service
   * @param terminology the terminology
   * @return the terminology
   * @throws Exception the exception
   */
  public static Terminology getTerminology(
    final SparqlQueryManagerService sparqlQueryManagerService, final String terminology)
    throws Exception {
    for (final Terminology t : sparqlQueryManagerService.getTerminologies()) {
      if (t.getTerminology().equals(terminology) && t.getLatest() != null && t.getLatest()) {
        return t;
      } else if (t.getTerminologyVersion().equals(terminology)) {
        return t;
      }
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, terminology + " not found");
  }

  /**
   * Returns the latest terminology.
   *
   * @param sparqlQueryManagerService the sparql query manager service
   * @return the terminology
   * @throws Exception the exception
   */
  public static Terminology getLatestTerminology(
    final SparqlQueryManagerService sparqlQueryManagerService)
    throws Exception {
    
    List<Terminology> terminologies = sparqlQueryManagerService.getTerminologies();
    if (CollectionUtils.isEmpty(terminologies))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No terminology found!");
    
    Optional<Terminology> latest = terminologies.stream().filter(t -> t.getLatest()).findFirst();
    if (!latest.isPresent()) 
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No terminology found with latest flag!");
    
    return latest.get();
  }
  
  /**
   * As set.
   *
   * @param <T> the
   * @param values the values
   * @return the sets the
   */
  public static <T> Set<T> asSet(@SuppressWarnings("unchecked") final T... values) {
    final Set<T> set = new HashSet<>(values.length);
    for (final T value : values) {
      if (value != null) {
        set.add(value);
      }
    }
    return set;
  }

  /**
   * Construct name for terminology using comment and version
   * 
   * @param comment the terminology comment
   * @param version the terminology version
   * @return
   */
  public static String constructName(String comment, String version) {
    return comment.substring(0, comment.indexOf(",")) + " " + version;
  }

}
