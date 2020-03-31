
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
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.ConfigData;
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
  private static final Logger logger = LoggerFactory.getLogger(MetadataController.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /** The metadata service. */
  @Autowired
  MetadataService metadataService;

  /**
   * Returns the application metadata.
   *
   * @return the application metadata
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Get application metadata for evs-explore application",
      response = ConfigData.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDB
  @RequestMapping(method = RequestMethod.GET, value = "/metadata", produces = "application/json")
  public @ResponseBody ConfigData getApplicationMetadata() throws Exception {
    return metadataService.getApplicationMetadata();
  }

  /**
   * Returns the terminologies.
   *
   * @return the terminologies
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Get all available terminologies", response = Terminology.class,
      responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDB
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/terminologies",
      produces = "application/json")
  public @ResponseBody List<Terminology> getTerminologies() throws Exception {
    return metadataService.getTerminologies();
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
  @ApiOperation(
      value = "Get all associations (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/associations",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return associations for (or leave blank for all)",
          required = false, dataType = "string", paramType = "query")
  })
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getAssociations(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {

    return metadataService.getAssociations(terminology, include, list);
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
  @ApiOperation(value = "Get the association for the specified terminology and code/label",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrLabel",
          value = "Association code (or label), e.g. 'A10' or 'Has_CDRH_Parent'", required = true,
          dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/association/{codeOrLabel}", produces = "application/json")
  public @ResponseBody Concept getAssociation(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrLabel") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {

    Optional<Concept> concept = metadataService.getAssociation(terminology, code, include);
    if (!concept.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");

    return concept.get();
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
  @ApiOperation(
      value = "Get all roles (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/roles",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return roles for (or leave blank for all)",
          required = false, dataType = "string", paramType = "query")
  })
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getRoles(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {

    return metadataService.getRoles(terminology, include, list);
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
  @ApiOperation(value = "Get the role for the specified terminology and code/label",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrLabel",
          value = "Role code (or label), e.g. 'R123' or 'Chemotherapy_Regimen_Has_Component'",
          required = true, dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/role/{codeOrLabel}",
      produces = "application/json")
  public @ResponseBody Concept getRole(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrLabel") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {

    Optional<Concept> concept = metadataService.getRole(terminology, code, include);
    if (!concept.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");

    return concept.get();
  }

  /**
   * Returns the properties.
   *
   * @param terminology the terminology
   * @param include the include
   * @param forDocumentation the for documentation
   * @param list the list
   * @return the properties
   * @throws Exception the exception
   */
  @ApiOperation(
      value = "Get all properties (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/properties",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "forDocumentation",
          value = "Special param for documentation mode to remove the specially handled properties",
          required = false, dataType = "string", paramType = "query"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return properties for (or leave blank for all)",
          required = false, dataType = "string", paramType = "query")

  })
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getProperties(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("forDocumentation") final Optional<Boolean> forDocumentation,
    @RequestParam("list") final Optional<String> list) throws Exception {

    return metadataService.getProperties(terminology, include, forDocumentation.orElse(false),
        list);
  }

  /**
   * Returns the term types.
   *
   * @param terminology the terminology
   * @return the term types
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all term types for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/termTypes",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit")
  })
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getTermTypes(
    @PathVariable(value = "terminology") final String terminology) throws Exception {

    return metadataService.getTermTypes(terminology);
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
  @ApiOperation(value = "Get the property for the specified terminology and code/label",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrLabel",
          value = "Property code (or label), e.g. 'P90' or 'FULL_SYN'", required = true,
          dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/property/{codeOrLabel}", produces = "application/json")
  public @ResponseBody Concept getProperty(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrLabel") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {

    Optional<Concept> concept = metadataService.getProperty(terminology, code, include);
    if (!concept.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");

    return concept.get();
  }

  /**
   * Returns the concept statuses.
   *
   * @param terminology the terminology
   * @return the concept statuses
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all concept status values for the specified terminology",
      response = String.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/conceptStatuses",
      produces = "application/json")
  public @ResponseBody List<String> getConceptStatuses(
    @PathVariable(value = "terminology") final String terminology) throws Exception {

    Optional<List<String>> result = metadataService.getConceptStatuses(terminology);
    if (!result.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    return result.get();
  }

  /**
   * Returns the contributing sources.
   *
   * @param terminology the terminology
   * @return the contributing sources
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all contributing sources for the specified terminology",
      response = String.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/contributingSources",
      produces = "application/json")
  public @ResponseBody List<String> getContributingSources(
    @PathVariable(value = "terminology") final String terminology) throws Exception {

    Optional<List<String>> result = metadataService.getContributingSources(terminology);
    if (!result.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    return result.get();
  }

  /**
   * Returns the axiom qualifiers list.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the axiom qualifiers list
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get axiom qualifiers for the specified property", response = String.class,
      responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrLabel",
          value = "Property code (or label), e.g. 'P383' or 'term-group'", required = true,
          dataType = "string", paramType = "path")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/property/{codeOrLabel}/axiomQualifiers",
      produces = "application/json")
  public @ResponseBody List<String> getAxiomQualifiersList(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrLabel") final String code) throws Exception {

    Optional<List<String>> result = metadataService.getAxiomQualifiersList(terminology, code);
    if (!result.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);

    return result.get();
  }
}
