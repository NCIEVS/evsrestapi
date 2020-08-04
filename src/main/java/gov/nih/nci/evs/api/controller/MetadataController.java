
package gov.nih.nci.evs.api.controller;

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

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
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
public class MetadataController extends BaseController {

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
   * Returns the terminologies.
   *
   * @return the terminologies
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all available terminologies", response = Terminology.class,
      responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/terminologies",
      produces = "application/json")
  public @ResponseBody List<Terminology> getTerminologies() throws Exception {
    try {
      return sparqlQueryManagerService.getTerminologies();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
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
      @ApiResponse(code = 400, message = "Bad request"),
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
  @RecordMetric
  public @ResponseBody List<Concept> getAssociations(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {
    try {
      return metadataService.getAssociations(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
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
  @ApiOperation(value = "Get the association for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Association code (or name), e.g. 'A10' or 'Has_CDRH_Parent'", required = true,
          dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/association/{codeOrName}", produces = "application/json")
  public @ResponseBody Concept getAssociation(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrName") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {
    try {

      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Association " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getAssociation(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Association " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
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
  @RecordMetric
  public @ResponseBody List<Concept> getRoles(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {
    try {
      return metadataService.getRoles(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
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
  @ApiOperation(value = "Get the role for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Role code (or name), e.g. 'R123' or 'Chemotherapy_Regimen_Has_Component'",
          required = true, dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/role/{codeOrName}",
      produces = "application/json")
  public @ResponseBody Concept getRole(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrName") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {
    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getRole(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
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
  @ApiOperation(
      value = "Get all properties (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
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
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return properties for (or leave blank for all)",
          required = false, dataType = "string", paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getProperties(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {

    try {
      return metadataService.getProperties(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the qualifiers.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the qualifiers
   * @throws Exception the exception
   */
  @ApiOperation(
      value = "Get all qualifiers (properties on properties) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/qualifiers",
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
          value = "List of codes or labels to return qualifiers for (or leave blank for all)",
          required = false, dataType = "string", paramType = "query")

  })
  @RecordMetric
  public @ResponseBody List<Concept> getQualifiers(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {
    try {
      return metadataService.getQualifiers(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the qualifier.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the qualifier
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the qualifier for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Qualifier code (or name), e.g. 'P390' or 'go-source'", required = true,
          dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/qualifier/{codeOrName}", produces = "application/json")
  public @ResponseBody Concept getQualifier(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrName") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {

    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualifier " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getQualifier(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualifier " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the term types.
   *
   * @param terminology the terminology
   * @return the term types
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all term types for the specified terminology",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/termTypes",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit")
  })
  @RecordMetric
  public @ResponseBody List<ConceptMinimal> getTermTypes(
    @PathVariable(value = "terminology") final String terminology) throws Exception {
    try {
      return metadataService.getTermTypes(terminology);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
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
  @ApiOperation(value = "Get the property for the specified terminology and code/name",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Property code (or name), e.g. 'P90' or 'FULL_SYN'", required = true,
          dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/property/{codeOrName}", produces = "application/json")
  public @ResponseBody Concept getProperty(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrName") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {

    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property " + code + " not found");
      }

      Optional<Concept> concept = metadataService.getProperty(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property " + code + " not found");

      return concept.get();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
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
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/conceptStatuses",
      produces = "application/json")
  public @ResponseBody List<String> getConceptStatuses(
    @PathVariable(value = "terminology") final String terminology) throws Exception {
    try {
      Optional<List<String>> result = metadataService.getConceptStatuses(terminology);
      if (!result.isPresent()) {
        // this should never happen
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      }
      return result.get();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the definition sources.
   *
   * @param terminology the terminology
   * @return the definition sources
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all definition sources for the specified terminology",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/definitionSources",
      produces = "application/json")
  public @ResponseBody List<ConceptMinimal> getDefinitionSources(
    @PathVariable(value = "terminology") final String terminology) throws Exception {
    try {
      return metadataService.getDefinitionSources(terminology);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the synonym sources.
   *
   * @param terminology the terminology
   * @return the synonym sources
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get all synonym sources for the specified terminology",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/{terminology}/synonymSources",
      produces = "application/json")
  public @ResponseBody List<ConceptMinimal> getSynonymSources(
    @PathVariable(value = "terminology") final String terminology) throws Exception {
    try {
      return metadataService.getSynonymSources(terminology);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the axiom qualifiers list.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the axiom qualifiers list
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get qualifier values for the specified terminology and code/name",
      response = String.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrName",
          value = "Qualifier code (or name), e.g. 'P383' or 'term-group'", required = true,
          dataType = "string", paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/metadata/{terminology}/qualifier/{codeOrName}/values",
      produces = "application/json")
  public @ResponseBody List<String> getQualifierValues(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "codeOrName") final String code) throws Exception {
    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualifier " + code + " not found");
      }

      Optional<List<String>> result = metadataService.getQualifierValues(terminology, code);
      if (!result.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Qualifier " + code + " not found");

      return result.get();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }
}
