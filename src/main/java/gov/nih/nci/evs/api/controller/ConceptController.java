
package gov.nih.nci.evs.api.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.aop.RecordMetricDBFormat;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.IncludeFlagUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Controller for /concept endpoints.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Concept endpoints")
public class ConceptController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger =
      LoggerFactory.getLogger(ConceptController.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /**
   * Returns the concept.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the concept
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the specified concept", response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}", produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  public @ResponseBody Concept getConcept(
    @PathVariable(value = "terminology") String terminology,
    @PathVariable(value = "code") String code,
    @RequestParam("include") Optional<String> include) throws Exception {

    final Terminology term = TerminologyUtils.getTerminology(terminology);
    final String dbType =
        "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";

    final Concept concept = IncludeFlagUtils.applyInclude(
        sparqlQueryManagerService.getEvsConceptByCode(code, dbType),
        include.orElse("summary"));
    if (concept == null || concept.getCode() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          code + " not found");
    }
    return concept;
  }

}
