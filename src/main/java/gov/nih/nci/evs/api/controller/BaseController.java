package gov.nih.nci.evs.api.controller;

import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.LoaderServiceImpl;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.util.Date;
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

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;

  /** The elastic query service. */
  @Autowired ElasticQueryService elasticQueryService;

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
    String functionName =
        StackWalker.getInstance().walk(s -> s.skip(1).findFirst()).get().getMethodName();
    final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
    String terminologyName = term != null ? term.getTerminology() : null;
    String terminologyVersion = term != null ? term.getVersion() : null;
    Audit audit =
        new Audit(
            "Exception",
            terminologyName,
            terminologyVersion,
            new Date(),
            functionName,
            e.getMessage(),
            "ERROR");
    LoaderServiceImpl.addAudit(audit);
    final String errorMessage =
        "An error occurred in the system. Please contact the NCI help desk.";
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
  }
}
