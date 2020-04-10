
package gov.nih.nci.evs.api.controller;

import java.util.ArrayList;
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

import gov.nih.nci.evs.api.aop.RecordMetricDBFormat;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.ConceptPath;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;
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
public class ConceptController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(ConceptController.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

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
  @RecordMetricDBFormat
  public @ResponseBody List<Concept> getConcepts(
    @PathVariable(value = "terminology") final String terminology,
    @RequestParam("include") final Optional<String> include,
    @RequestParam("list") final String list) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    final List<Concept> concepts = new ArrayList<>();
    for (final String code : list.split(",")) {
      final Concept concept = 
          sparqlQueryManagerService.getEvsConceptByCode(code, term, ip);
      if (concept != null && concept.getCode() != null) {
        concept.setTerminology(terminology);
        concepts.add(concept);
      }
    }
    return concepts;
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));

    final Concept concept = 
        sparqlQueryManagerService.getEvsConceptByCode(code, term, ip);

    if (concept == null || concept.getCode() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
    }
    concept.setTerminology(terminology);
    return concept;
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/associations",
      produces = "application/json")
  public @ResponseBody List<Association> getAssociations(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    final List<Association> list = sparqlQueryManagerService.getEvsAssociations(code, term);
    if (list == null || list.isEmpty()) {
      if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      } else {
        return new ArrayList<>();
      }
    }

    return list;
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET,
      value = "/concept/{terminology}/{code}/inverseAssociations", produces = "application/json")
  public @ResponseBody List<Association> getInverseAssociations(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    final List<Association> list =
        sparqlQueryManagerService.getEvsInverseAssociations(code, term);
    if (list == null || list.isEmpty()) {
      if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      } else {
        return new ArrayList<>();
      }
    }

    return list;
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/roles",
      produces = "application/json")
  public @ResponseBody List<Role> getRoles(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    final List<Role> list = sparqlQueryManagerService.getEvsRoles(code, term);
    if (list == null || list.isEmpty()) {
      if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      } else {
        return new ArrayList<>();
      }
    }

    return list;
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/inverseRoles",
      produces = "application/json")
  public @ResponseBody List<Role> getInverseRoles(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    final List<Role> list = sparqlQueryManagerService.getEvsInverseRoles(code, term);
    if (list == null || list.isEmpty()) {
      if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      } else {
        return new ArrayList<>();
      }
    }

    return list;
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/parents",
      produces = "application/json")
  public @ResponseBody List<Concept> getParents(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    final List<Concept> list =
        sparqlQueryManagerService.getEvsSuperconcepts(code, term);
    if (list == null || list.isEmpty()) {
      if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      } else {
        return new ArrayList<>();
      }
    }

    return list;
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/children",
      produces = "application/json")
  public @ResponseBody List<Concept> getChildren(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    final List<Concept> list =
        sparqlQueryManagerService.getEvsSubconcepts(code, term);
    if (list == null || list.isEmpty()) {
      if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      } else {
        return new ArrayList<>();
      }
    }

    return list;
  }

  /**
   * Returns the descendants.
   *
   * @param terminology the terminology
   * @param code the code
   * @param maxLevel the max level
   * @return the descendants
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get descendant concepts for the specified terminology and code",
      response = ConceptMinimal.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/descendants",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path"),
      @ApiImplicitParam(name = "maxLevel",
          value = "Maximum level of ancestors to include, if applicable", required = false,
          dataType = "string", paramType = "query")
  })
  public @ResponseBody List<Concept> getDescendants(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code,
    @RequestParam("maxLevel") final Optional<Integer> maxLevel) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    final List<HierarchyNode> list =
        sparqlQueryManagerService.getChildNodes(code, maxLevel.orElse(0), term);

    if (list == null || list.isEmpty()) {
      if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      } else {
        return new ArrayList<>();
      }
    }

    return ConceptUtils.convertConceptsFromHierarchy(list);
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3224'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/maps",
      produces = "application/json")
  public @ResponseBody List<Map> getMaps(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);

    final List<Map> list = sparqlQueryManagerService.getEvsMapsTo(code, term);
    if (list == null || list.isEmpty()) {
      if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      } else {
        return new ArrayList<>();
      }
    }

    return list;
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code in the specified terminology, e.g. 'C3910'",
          required = true, dataType = "string", paramType = "path")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/disjointWith",
      produces = "application/json")
  public @ResponseBody List<DisjointWith> getDisjointWith(
    @PathVariable(value = "terminology") final String terminology,
    @PathVariable(value = "code") final String code) throws Exception {

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam("disjointWith");

    final Concept concept = 
        sparqlQueryManagerService.getEvsConceptByCode(code, term, ip);

    if (concept == null || concept.getCode() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
    }
    return concept.getDisjointWith();
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    final List<HierarchyNode> list = sparqlQueryManagerService.getRootNodes(term);
    if (list == null || list.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "No roots for found for terminology = " + terminology);
    }
    return ConceptUtils.convertConceptsFromHierarchyWithInclude(sparqlQueryManagerService, ip,
        term, list);
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
      response = ConceptPath.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
    }
    final Paths paths = sparqlQueryManagerService.getPathToRoot(code, term);

    return ConceptUtils.convertPathsWithInclude(sparqlQueryManagerService, ip, term, paths, true);

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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
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
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    logger.debug("found terminology, calling for nodes..");
    final List<HierarchyNode> nodes = sparqlQueryManagerService.getPathInHierarchy(code, term);
    return nodes;
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
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
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
    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final List<HierarchyNode> nodes = sparqlQueryManagerService.getChildNodes(code, term);
    return nodes;
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
      response = ConceptPath.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
    }
    final Paths paths = sparqlQueryManagerService.getPathToRoot(code, term);
    return ConceptUtils.convertPathsWithInclude(sparqlQueryManagerService, ip, term, paths,
        false);

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
      response = ConceptPath.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
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

    final Terminology term =
        TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
    final IncludeParam ip = new IncludeParam(include.orElse(null));

    if (!sparqlQueryManagerService.checkConceptExists(code, term)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
    }
    final Paths paths = sparqlQueryManagerService.getPathToParent(code, ancestorCode, term);
    return ConceptUtils.convertPathsWithInclude(sparqlQueryManagerService, ip, term, paths,
        false);

  }
}
