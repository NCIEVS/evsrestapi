
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.es.IndexMetadata;

/**
 * Utilities for handling the "include" flag, and converting EVSConcept to
 * Concept.
 */
@Component
public final class TerminologyUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(TerminologyUtils.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /* The elasticsearch query service */
  @Autowired
  ElasticQueryService esQueryService;
  
  /**
   * Returns all terminologies.
   * 
   * @return the terminologies
   * @throws Exception Signals that an exception has occurred.
   */
//  public List<Terminology> getTerminologies() throws Exception {
//    return sparqlQueryManagerService.getTerminologies();
//  }

  /**
   * Returns terminologies loaded to elasticsearch.
   * 
   * @return the terminologies
   * @throws Exception Signals that an exception has occurred.
   */
  public List<Terminology> getAvailableTerminologies() throws Exception {
    //get index metadata for terminologies completely loaded in es
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);
    if (CollectionUtils.isEmpty(iMetas)) return Collections.emptyList();
    
    //get all terminologies and organize in a map by terminologyVersion as key
    List<Terminology> terminologies = sparqlQueryManagerService.getTerminologies();
    final Map<String, Terminology> termMap = new HashMap<>();
    terminologies.stream().forEach(t -> termMap.putIfAbsent(t.getTerminologyVersion(), t));
    
    //collect only terminologies loaded in es
    return iMetas.stream()
        .map(m -> termMap.get(m.getTerminologyVersion()))
        .collect(Collectors.toList());
  }
  
  /**
   * Returns the terminology.
   *
   * @param sparqlQueryManagerService the sparql query manager service
   * @param terminology the terminology
   * @return the terminology
   * @throws Exception the exception
   */
  public Terminology getTerminology(final String terminology)
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
  public Terminology getLatestTerminology()
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
