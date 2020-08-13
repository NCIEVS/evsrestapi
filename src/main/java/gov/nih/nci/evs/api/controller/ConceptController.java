
package gov.nih.nci.evs.api.controller;

import java.util.ArrayList;
import java.util.Arrays;
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
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.ConceptUtils;
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
public class ConceptController extends BaseController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(ConceptController.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /** The elastic query service. */
  /* The elasticsearch query service */
  @Autowired
  ElasticQueryService elasticQueryService;

  /**
   * Returns the associations.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the associations
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get concepts specified by list parameter", response = Concept.class,
      responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}",
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
          value = "List (comma-separated) of codes to return concepts for, e.g. C2291,C3224.",
          required = true, dataType = "string", paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getConcepts(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final String list) throws Exception {
    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));

      final String[] codes = list.split(",");
      // Impose a maximum number at a time
      if (codes.length > 500) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Maximum number of concepts to request at a time is 500 = " + codes.length);
      }

      final List<Concept> concepts =
          elasticQueryService.getConcepts(Arrays.asList(codes), term, ip);
      return concepts;
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the concept.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the concept
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the concept for the specified terminology and code",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "summary")
  })
  public @ResponseBody Concept getConcept(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {
    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));

      Optional<Concept> concept = elasticQueryService.getConcept(code, term, ip);

      if (!concept.isPresent() || concept.get().getCode() == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }
      return concept.get();
    } catch (Exception e) {
      handleException(e);
      return null;
    }

  }

  /**
   * Returns the associations.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the associations
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the associations for the specified terminology and code",
      response = Association.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/associations",
      produces = "application/json")
  public @ResponseBody List<Association> getAssociations(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("associations"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getAssociations();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the inverse associations.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the inverse associations
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get inverse associations for the specified terminology and code",
      response = Association.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/concept/{terminology}/{code}/inverseAssociations", produces = "application/json")
  public @ResponseBody List<Association> getInverseAssociations(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {
    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("inverseAssociations"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getInverseAssociations();
    } catch (Exception e) {
      handleException(e);
      return null;
    }

  }

  /**
   * Returns the roles.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the roles
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get roles for the specified terminology and code", response = Role.class,
      responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/roles",
      produces = "application/json")
  public @ResponseBody List<Role> getRoles(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("roles"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getRoles();
    } catch (Exception e) {
      handleException(e);
      return null;
    }

  }

  /**
   * Returns the inverse roles.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the inverse roles
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get inverse roles for the specified terminology and code",
      response = Role.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/inverseRoles",
      produces = "application/json")
  public @ResponseBody List<Role> getInverseRoles(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("inverseRoles"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getInverseRoles();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the parents.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the parents
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get parent concepts for the specified terminology and code",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/parents",
      produces = "application/json")
  public @ResponseBody List<Concept> getParents(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("parents"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getParents();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the children.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the children
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get child concepts for the specified terminology and code",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/children",
      produces = "application/json")
  public @ResponseBody List<Concept> getChildren(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("children"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getChildren();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the descendants.
   *
   * @param terminology the terminology
   * @param code the code
   * @param fromRecord the from record
   * @param pageSize the page size
   * @return the descendants
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get descendant concepts for the specified terminology and code",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/descendants",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results",
          required = false, dataType = "string", paramType = "query", defaultValue = "0"),
      @ApiImplicitParam(name = "pageSize", value = "Max number of results to return",
          required = false, dataType = "string", paramType = "query", defaultValue = "10000")
  })
  public @ResponseBody List<Concept> getDescendants(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code,
    @RequestParam("fromRecord") final Optional<Integer> fromRecord,
    @RequestParam("pageSize") final Optional<Integer> pageSize) throws Exception {
    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      final List<Concept> list =
          new ArrayList<Concept>(elasticQueryService.getDescendants(code, term));

      int fromIndex = fromRecord.orElse(0);
      // Use a large default page size
      int toIndex = fromIndex + pageSize.orElse(50000);
      if (toIndex >= list.size()) {
        toIndex = list.size();
      }
      // If requesting beyond end of the list, just return empty
      if (fromRecord.orElse(0) > list.size()) {
        return new ArrayList<>();
      }
      // empty list
      if (list.size() == 0) {
        return list;
      }

      return list.subList(fromIndex, toIndex);

    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the maps.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the maps
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get maps for the specified terminology and code", response = Map.class,
      responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/maps",
      produces = "application/json")
  public @ResponseBody List<Map> getMaps(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("maps"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getMaps();
    } catch (Exception e) {
      handleException(e);
      return null;
    }

  }

  /**
   * Returns the disjoint with.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the disjoint with
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get \"disjoint with\" info for the specified terminology and code",
      response = DisjointWith.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3910'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/disjointWith",
      produces = "application/json")
  public @ResponseBody List<DisjointWith> getDisjointWith(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {
    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("disjointWith"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      return concept.get().getDisjointWith();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the roots.
   *
   * @param terminology the terminology
   * @param include the include
   * @return the roots
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get root concepts for the specified terminology", response = Concept.class,
      responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/roots",
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
          required = false, dataType = "string", paramType = "query", defaultValue = "minimal")
  })
  public @ResponseBody List<Concept> getRoots(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include) throws Exception {

    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      final List<Concept> list = elasticQueryService.getRootNodes(term);
      if (list == null || list.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No roots for found for terminology = " + terminology);
      }
      return list;
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the paths from root.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the paths from root
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get paths from the hierarchy root to the specified concept",
      response = List.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/pathsFromRoot",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "minimal")
  })
  public @ResponseBody List<List<Concept>> getPathsFromRoot(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {

    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final Paths paths = elasticQueryService.getPathToRoot(code, term);

      return ConceptUtils.convertPathsWithInclude(elasticQueryService, ip, term, paths, true);
    } catch (Exception e) {
      handleException(e);
      return null;
    }

  }

  /**
   * Returns the subtree.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the subtree
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the entire subtree from the root node to the specified code",
      response = HierarchyNode.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/subtree",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  public @ResponseBody List<HierarchyNode> getSubtree(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final List<HierarchyNode> nodes = elasticQueryService.getPathInHierarchy(code, term);

      return nodes;
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the subtree children.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the subtree children
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the entire subtree from the root node to the specified code",
      response = HierarchyNode.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/concept/{terminology}/{code}/subtree/children", produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  public @ResponseBody List<HierarchyNode> getSubtreeChildren(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {
    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final List<HierarchyNode> nodes = elasticQueryService.getChildNodes(code, 0, term);
      nodes.stream().peek(n -> n.setLevel(null)).count();
      return nodes;
    } catch (Exception e) {
      handleException(e);
      return null;
    }

  }

  /**
   * Returns the paths to root.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the paths to root
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get paths to the hierarchy root from the specified code",
      response = List.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/pathsToRoot",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "minimal")
  })
  public @ResponseBody List<List<Concept>> getPathsToRoot(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code,
    @RequestParam("include") final Optional<String> include) throws Exception {
    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final Paths paths = elasticQueryService.getPathToRoot(code, term);
      return ConceptUtils.convertPathsWithInclude(elasticQueryService, ip, term, paths, false);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the paths to ancestor.
   *
   * @param terminology the terminology
   * @param code the code
   * @param ancestorCode the ancestor code
   * @param include the include
   * @return the paths to ancestor
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get paths from the specified code to the specified ancestor code",
      response = List.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/concept/{terminology}/{code}/pathsToAncestor/{ancestorCode}",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "ancestorCode",
          value = "Ancestor code of the other specified code, e.g. 'C2991'", required = true,
          dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "minimal")
  })
  public @ResponseBody List<List<Concept>> getPathsToAncestor(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code,
    @PathVariable(value = "ancestorCode") final String ancestorCode,
    @RequestParam("include") final Optional<String> include) throws Exception {
    try {
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final Paths paths = elasticQueryService.getPathToParent(code, ancestorCode, term);
      return ConceptUtils.convertPathsWithInclude(elasticQueryService, ip, term, paths, false);

    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

}
