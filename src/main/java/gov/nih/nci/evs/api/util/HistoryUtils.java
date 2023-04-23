
package gov.nih.nci.evs.api.util;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.model.BaseModel;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;

/**
 * Utilities for handling the history related tasks.
 */
public final class HistoryUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(HistoryUtils.class);
  
  /** The list of actions the show an NCIT code has been retired. */
  public static List<String> NCIT_RETIRED_ACTIONS = Arrays.asList("retire");

  /**
   * Instantiates an empty {@link HistoryUtils}.
   */
  private HistoryUtils() {
    // n/a
  }

  /**
   * Returns suggested replacements for a retired concept.
   *
   * @param terminology the terminology
   * @param service the service
   * @param code the code
   * @return the replacement codes
   * @throws Exception the exception
   */
  public static List<String> getReplacements(final Terminology terminology, final ElasticQueryService service, final String code) throws Exception {

      final Set<String> replacementCodes = new HashSet<>();
      boolean retired = false;
    
      final Optional<Concept> concept = service.getConcept(code, terminology, new IncludeParam("history"));
    
      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }
      
      for (final History history : concept.get().getHistory()) {
          
          if ((terminology.equals("ncit") && NCIT_RETIRED_ACTIONS.contains(history.getAction())) 
                  || terminology.equals("ncim")) {
              
              retired = true;
              
              if (history.getReplacementCode() != null && !history.getReplacementCode().isEmpty()) {
                  replacementCodes.add(history.getReplacementCode());
              }
          }
      }
    
      return new ArrayList<>(replacementCodes);
  }
  
  /**
   * Returns suggested replacements for a list of retired concepts.
   *
   * @param terminology the terminology
   * @param service the service
   * @param code the code
   * @return the replacement codes
   * @throws Exception the exception
   */
  public static Map<String, List<String>> getReplacements(final Terminology terminology, final ElasticQueryService service, final List<String> codes) throws Exception {

      final Map<String, List<String>> replacementMap = new HashMap<>();
      
      for (final String code : codes) {
          replacementMap.put(code, getReplacements(terminology, service, code));
      }
    
      return replacementMap;
  }
}
