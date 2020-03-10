
package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.ConfigData;
import gov.nih.nci.evs.api.util.ConceptUtils;
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

  /** The cache. */
  private static Map<String, List<Concept>> cache =
      new LinkedHashMap<String, List<Concept>>(1000 * 4 / 3, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, List<Concept>> eldest) {
          return size() > 1001;
        }
      };

  /** The Constant log. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(MetadataController.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /**
   * Returns the version info.
   *
   * @return the version info
   * @throws IOException Signals that an I/O exception has occurred.
   */

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
  public @ResponseBody ConfigData getApplicationMetadata() throws IOException {
    return sparqlQueryManagerService.getConfigurationData("monthly");
  }

  /**
   * Returns the version info.
   *
   * @return the version info
   * @throws IOException Signals that an I/O exception has occurred.
   */

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
  public @ResponseBody List<Terminology> getTerminologies() throws IOException {
    return TerminologyUtils.getTerminologies(sparqlQueryManagerService);
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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<EvsConcept> associations = sparqlQueryManagerService.getAllAssociations(dbType, ip);
    return ConceptUtils.applyIncludeAndList(associations, ip, list.orElse(null));
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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept =
          ConceptUtils.convertConcept(sparqlQueryManagerService.getEvsProperty(code, dbType, ip));
      if (concept == null || concept.getCode() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }
      return concept;
    }
    final List<Concept> list = getAssociations(terminology,
        Optional.ofNullable(include.orElse("summary")), Optional.ofNullable(code));
    if (list.size() > 0) {
      return list.get(0);
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");

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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<EvsConcept> roles = sparqlQueryManagerService.getAllRoles(dbType, ip);
    return ConceptUtils.applyIncludeAndList(roles, ip, list.orElse(null));
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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept =
          ConceptUtils.convertConcept(sparqlQueryManagerService.getEvsProperty(code, dbType, ip));
      if (concept == null || concept.getCode() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }
      return concept;
    }
    final List<Concept> list = getRoles(terminology, Optional.ofNullable(include.orElse("summary")),
        Optional.ofNullable(code));
    if (list.size() > 0) {
      return list.get(0);
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");

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
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return properties for (or leave blank for all)",
          required = false, dataType = "string", paramType = "query")
  })
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getProperties(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final Optional<String> list) throws Exception {

    // TODO: Arun - cache this in a sprint friendly way - and other metadata  calls too
    final String key = include.orElse("") + terminology;
    if (list.orElse("").isEmpty()) {
      if (cache.containsKey(key)) {
        return cache.get(key);
      }
    }

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<EvsConcept> properties = sparqlQueryManagerService.getAllProperties(dbType, ip);
    final List<Concept> results =
        ConceptUtils.applyIncludeAndList(properties, ip, list.orElse(null));
    if (list.orElse("").isEmpty()) {
      cache.put(key, results);
    }
    return results;

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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    if (ModelUtils.isCodeStyle(code)) {
      final Concept concept =
          ConceptUtils.convertConcept(sparqlQueryManagerService.getEvsProperty(code, dbType, ip));
      if (concept == null || concept.getCode() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }
      return concept;
    }

    final List<Concept> list =
        getProperties(terminology, Optional.of("minimal"), Optional.ofNullable(code));
    if (list.size() > 0) {
      return ConceptUtils.convertConcept(
          sparqlQueryManagerService.getEvsProperty(list.get(0).getCode(), dbType, ip));
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");

  }

  /**
   * Returns the concept statuses.
   *
   * @param terminology the terminology
   * @return the concept statuses
   * @throws Exception the exception
   */

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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    if (term.getTerminology().equals("ncit")) {
      return sparqlQueryManagerService.getConceptStatusForDocumentation();
    }

    throw new ResponseStatusException(HttpStatus.NOT_FOUND);

  }

  /**
   * Returns the contributing sources.
   *
   * @param terminology the terminology
   * @return the contributing sources
   * @throws Exception the exception
   */

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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    if (term.getTerminology().equals("ncit")) {
      return sparqlQueryManagerService.getContributingSourcesForDocumentation();
    }

    throw new ResponseStatusException(HttpStatus.NOT_FOUND);

  }

  /**
   * Returns the graph names. UNDOCUMENTED. BAC - "weekly" and "monthly" don't
   * really make sense here. This needs to be generalized for the
   * multi-environment scenario. And techically, this is probably for debugging
   * and may not really make sense as part of THIS API.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the graph names
   * @throws Exception the exception
   */
  // @RequestMapping(method = RequestMethod.GET, value =
  // "/metadata/{terminology}/graphNames", produces = "application/json")
  // public @ResponseBody List<String> getGraphNames(
  // @PathVariable(value = "terminology") final String terminology)
  // throws Exception {
  //
  // final Terminology term =
  // TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
  // final String dbType =
  // "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
  // List<String> graphNames =
  // sparqlQueryManagerService.getAllGraphNames(dbType);
  // return graphNames;
  // }

  /**
   * Returns the axiom qualifiers list. UNDOCUMENTED.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the axiom qualifiers list
   * @throws Exception the exception
   */

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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final String dbType = "true".equals(term.getTags().get("weekly")) ? "weekly" : "monthly";
    final IncludeParam ip = new IncludeParam("minimal");

    // Like "get properties", if it's "name style", we need to get all and then
    // find
    // this one.
    Concept concept = null;
    if (ModelUtils.isCodeStyle(code)) {
      concept =
          ConceptUtils.convertConcept(sparqlQueryManagerService.getEvsProperty(code, dbType, ip));
    }

    final List<Concept> list =
        getProperties(terminology, Optional.ofNullable("minimal"), Optional.ofNullable(code));
    if (list.size() > 0) {
      concept = list.get(0);
    }

    if (concept == null || concept.getCode() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
    }

    final List<String> propertyValues =
        sparqlQueryManagerService.getAxiomQualifiersList(concept.getCode(), dbType);
    return propertyValues;
  }
}
