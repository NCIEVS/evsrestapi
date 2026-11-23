package gov.nih.nci.evs.api.controller;

import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Controller for /metadata endpoints. */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
public class BaseController {

  /**
   * How the include parameter works, for every endpoint that takes it. The API documentation lists
   * the values themselves, from {@link gov.nih.nci.evs.api.model.Include}.
   */
  protected static final String INCLUDE_DESCRIPTION =
      "Which fields to return for each concept, beyond its code, name, terminology, and status."
          + " Comma-separated; the response gets every field any value turns on. Shortcuts:"
          + " 'minimal' (no extra fields), 'summary' (synonyms, definitions, properties), 'full'"
          + " (every field except the large lists descendants, ancestors, paths, and extensions),"
          + " '*' (every field). <a"
          + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
          + " target='_blank'>See here for detailed information</a>.";

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

  /** The opensearch operations service. */
  @Autowired OpensearchOperationsService opensearchOperationsService;

  /**
   * Handle exception.
   *
   * @param e the e
   * @throws Exception the exception
   */
  public void handleException(final Exception e, String terminology) throws Exception {
    if (e instanceof ResponseStatusException) {
      throw e;
    }

    logger.error("Unexpected error", e);
    Audit.addAudit(
        opensearchOperationsService,
        "Exception",
        e.getStackTrace()[0].getClassName(),
        terminology,
        e.getMessage(),
        "ERROR");
    final String errorMessage =
        "An error occurred in the system. Please contact the NCI help desk.";
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
  }
}
