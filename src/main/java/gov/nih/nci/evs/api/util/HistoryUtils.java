package gov.nih.nci.evs.api.util;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Utilities for handling the history related tasks. */
public final class HistoryUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(HistoryUtils.class);

  /** The list of actions the show an NCIT code has been retired. */
  public static List<String> RETIRED_ACTIONS = Arrays.asList("retire", "DEL");

  /** Instantiates an empty {@link HistoryUtils}. */
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
  public static List<History> getReplacements(
      final Terminology terminology, final ElasticQueryService service, final String code)
      throws Exception {

    final List<History> replacements = new ArrayList<>();

    final Optional<Concept> concept =
        service.getConcept(code, terminology, new IncludeParam("history"));

    // Ignore bad codes
    if (!concept.isPresent()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
    }

    for (final History history : concept.get().getHistory()) {

      // If there is a "retired" entry for the concept being looked up we are done - but only
      // include these if
      // there are no other replacement entries or the "retired" entry has a replacement listed
      if (RETIRED_ACTIONS.contains(history.getAction())
          && history.getCode().equals(code)
          && (replacements.isEmpty()
              || (history.getReplacementCode() != null
                  && !history.getReplacementCode().isEmpty()))) {

        history.setName(concept.get().getName());
        replacements.add(history);
        return replacements;
      } else if (history.getReplacementCode() != null
          && !history.getReplacementCode().isEmpty()
          && history.getCode().equals(code)
          && !history.getReplacementCode().equals(code)) {

        history.setName(concept.get().getName());
        replacements.add(history);
      }
    }

    // If there is no history at this point, we have an active concept
    if (replacements.isEmpty()) {
      final History active = new History();
      active.setCode(concept.get().getCode());
      active.setName(concept.get().getName());
      active.setAction("active");
      replacements.add(active);
    }

    return replacements;
  }

  /**
   * Returns suggested replacements for a list of retired concepts.
   *
   * @param terminology the terminology
   * @param service the service
   * @param codes the codes
   * @return the replacement codes
   * @throws Exception the exception
   */
  public static List<History> getReplacements(
      final Terminology terminology, final ElasticQueryService service, final List<String> codes)
      throws Exception {

    final List<History> replacements = new ArrayList<>();

    for (final String code : codes) {
      replacements.addAll(getReplacements(terminology, service, code));
    }

    return replacements;
  }
}
