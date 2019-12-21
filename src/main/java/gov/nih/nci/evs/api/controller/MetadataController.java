
package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.List;
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

import gov.nih.nci.evs.api.aop.RecordMetricDB;
import gov.nih.nci.evs.api.aop.RecordMetricDBFormat;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.IncludeFlagUtils;
import gov.nih.nci.evs.api.util.ModelUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
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

    return TerminologyUtils.getTerminologies();

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
  @ApiOperation(value = "Get a all associations, or those specified by list parameter", response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/associations", produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "list", value = "List of codes or labels to return associations for", required = false, dataType = "string", paramType = "query")
  })
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getAssociations(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {

    final Terminology term = TerminologyUtils.getTerminology(terminology);
    final String dbType =
        "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";

    final List<EvsConcept> associations =
        sparqlQueryManagerService.getAllAssociations(dbType, "byLabel");
    return IncludeFlagUtils.applyIncludeAndList(associations,
        include.orElse("minimal"), list.orElse(null));
  }

  /**
   * Returns the association.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the association
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the specified association code or label", response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/association/{codeOrLabel}", produces = "application/json")
  public @ResponseBody Concept getAssociation(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrLabel") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {

    final Terminology term = TerminologyUtils.getTerminology(terminology);
    final String dbType =
        "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept = IncludeFlagUtils.applyInclude(
          sparqlQueryManagerService.getEvsPropertyByCode(code, dbType),
          include.orElse("summary"));
      if (concept == null || concept.getCode() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            code + " not found");
      }
      return concept;
    }
    final List<Concept> list = getAssociations(terminology,
        Optional.ofNullable(include.orElse("summary")),
        Optional.ofNullable(code));
    if (list.size() > 0) {
      return list.get(0);
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
        code + " not found");

  }

  /**
   * Returns the roles.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the roles
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all roles, or those specified by list parameter", response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/roles", produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "list", value = "List of codes or labels to return roles for", required = false, dataType = "string", paramType = "query")
  })
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getRoles(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {

    final Terminology term = TerminologyUtils.getTerminology(terminology);
    final String dbType =
        "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";

    final List<EvsConcept> roles =
        sparqlQueryManagerService.getAllRoles(dbType, "byLabel");
    return IncludeFlagUtils.applyIncludeAndList(roles,
        include.orElse("minimal"), list.orElse(null));
  }

  /**
   * Returns the role.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the role
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the specified role code or label", response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/role/{codeOrLabel}", produces = "application/json")
  public @ResponseBody Concept getRole(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrLabel") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {

    final Terminology term = TerminologyUtils.getTerminology(terminology);
    final String dbType =
        "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept = IncludeFlagUtils.applyInclude(
          sparqlQueryManagerService.getEvsPropertyByCode(code, dbType),
          include.orElse("summary"));
      if (concept == null || concept.getCode() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            code + " not found");
      }
      return concept;
    }
    final List<Concept> list =
        getRoles(terminology, Optional.ofNullable(include.orElse("summary")),
            Optional.ofNullable(code));
    if (list.size() > 0) {
      return list.get(0);
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
        code + " not found");

  }

  /**
   * Returns the properties.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the properties
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all properties, or those specified by list parameter", response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/properties", produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "list", value = "List of codes or labels to return properties for", required = false, dataType = "string", paramType = "query")
  })
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getProperties(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {

    final Terminology term = TerminologyUtils.getTerminology(terminology);
    final String dbType =
        "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";

    final List<EvsConcept> properties =
        sparqlQueryManagerService.getAllProperties(dbType, "byLabel");
    return IncludeFlagUtils.applyIncludeAndList(properties,
        include.orElse("minimal"), list.orElse(null));
  }

  /**
   * Returns the property.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the property
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the specified property code or label", response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "include", value = "Indicator of how much data to return", required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/property/{codeOrLabel}", produces = "application/json")
  public @ResponseBody Concept getProperty(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrLabel") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {

    final Terminology term = TerminologyUtils.getTerminology(terminology);
    final String dbType =
        "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept = IncludeFlagUtils.applyInclude(
          sparqlQueryManagerService.getEvsPropertyByCode(code, dbType),
          include.orElse("summary"));
      if (concept == null || concept.getCode() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            code + " not found");
      }
      return concept;
    }
    final List<Concept> list = getProperties(terminology,
        Optional.ofNullable(include.orElse("summary")),
        Optional.ofNullable(code));
    if (list.size() > 0) {
      return list.get(0);
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
        code + " not found");

  }

  /**
   * Returns the concept statuses.
   *
   * @param terminology the terminology
   * @return the concept statuses
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Get the concept status values", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/conceptStatuses", produces = "application/json")
  public @ResponseBody List<String> getConceptStatuses(
    @PathVariable(value = "terminology") final String terminology)
    throws IOException {

    if (terminology.equals("ncit")) {
      return sparqlQueryManagerService.getConceptStatusForDocumentation();
    }

    throw new ResponseStatusException(HttpStatus.NOT_FOUND);

  }

  /**
   * Returns the contributing sources.
   *
   * @param terminology the terminology
   * @return the contributing sources
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Get the contributing sources", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/contributingSources", produces = "application/json")
  public @ResponseBody List<String> getContributingSources(
    @PathVariable(value = "terminology") final String terminology)
    throws IOException {

    if (terminology.equals("ncit")) {
      return sparqlQueryManagerService.getContributingSourcesForDocumentation();
    }

    throw new ResponseStatusException(HttpStatus.NOT_FOUND);

  }
}
