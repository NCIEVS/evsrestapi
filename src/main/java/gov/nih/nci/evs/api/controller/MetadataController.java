
package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.List;

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

import gov.nih.nci.evs.api.aop.RecordMetricDB;
import gov.nih.nci.evs.api.aop.RecordMetricDBFormat;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.EVSUtils;
import gov.nih.nci.evs.api.util.IncludeFlagUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Controller for /metadata endpoints.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Metadata endpoints")
public class MetadataController {

  /** The Constant log. */
  @SuppressWarnings("unused")
  private static final Logger logger =
      LoggerFactory.getLogger(MetadataController.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /**
   * Returns the version info.
   *
   * @return the version info
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Gets terminologies loaded into the API", response = EvsVersionInfo.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'", required = false, dataType = "string", paramType = "query")
  })
  @RecordMetricDB
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/terminologies", produces = "application/json")
  public @ResponseBody List<Terminology> getTerminologies() throws IOException {

    // TODO: Next step for this is to use approaches in MetadataUtils
    // to discover the ontologies loaded into stardog (via sparql).
    final Terminology monthlyNcit =
        new Terminology(sparqlQueryManagerService.getEvsVersionInfo("monthly"));
    monthlyNcit.getTags().put("monthly", "true");
    // Monthly is the latest published version
    monthlyNcit.setLatest(true);

    final Terminology weeklyNcit =
        new Terminology(sparqlQueryManagerService.getEvsVersionInfo("weekly"));

    // If these are equal, there's only one version, return it
    if (monthlyNcit.equals(weeklyNcit)) {
      return EVSUtils.asList(monthlyNcit);
    }
    // Otherwise, there are two versions
    else {
      weeklyNcit.getTags().put("weekly", "true");
      weeklyNcit.setLatest(false);
      return EVSUtils.asList(monthlyNcit, weeklyNcit);
    }
  }

  /**
   * Returns the terminology.
   *
   * @param terminology the terminology
   * @return the terminology
   * @throws Exception the exception
   */
  private Terminology getTerminology(final String terminology)
    throws Exception {
    for (final Terminology t : getTerminologies()) {
      if (t.getName().equals(terminology) && t.getLatest() != null
          && t.getLatest()) {
        return t;
      } else if (t.getTerminologyVersion().equals(terminology)) {
        return t;
      }
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  }

  /**
   * Returns the associations.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the associations
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get a all associations, or those specified by list parameter", response = EvsConcept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/{terminology}/metadata/associations", produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "list", value = "List of codes or labels to return associations for", required = false, dataType = "string", paramType = "query")
  })
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getAssociations(
    @PathVariable(value = "terminology") String terminology,
    @RequestParam("include") String include, @RequestParam("list") String list)
    throws Exception {

    Terminology term = getTerminology(terminology);
    final List<EvsConcept> associations =
        sparqlQueryManagerService.getAllAssociations(
            "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly",
            "byLabel");
    return IncludeFlagUtils.applyInclude(associations, include);

  }
}
