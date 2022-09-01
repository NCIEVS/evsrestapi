
package gov.nih.nci.evs.api.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.AssociationEntryResultList;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.MetadataService;
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
  @Autowired
  ElasticQueryService elasticQueryService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired
  TerminologyUtils termUtils;

  /** The metadata service. */
  /* The metadata service */
  @Autowired
  MetadataService metadataService;

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
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List (comma-separated) of codes to return concepts for, e.g."
              + "<ul><li>'C2291,C3224' for <i>ncit</i></li>"
              + "<li>'C0010137,C0025202' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getConcepts(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("include")
  final Optional<String> include, @RequestParam("list")
  final String list) throws Exception {
    try {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse("summary"));

      final String[] codes = list.split(",");
      // Impose a maximum number at a time
      if (codes.length > 1000) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Maximum number of concepts to request at a time is 1000 = " + codes.length);
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
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g." + "<ul><li>'C3224' for <i>ncit</i></li>"
              + "<li>'C0025202' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the "
              + "following values: minimal, summary, full, associations, children, definitions, "
              + "disjointWith, inverseAssociations, inverseRoles, maps, parents, properties, "
              + "roles, synonyms. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
              + "master/doc/INCLUDE.md' target='_blank'>See here for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "summary")
  })
  public @ResponseBody Concept getConcept(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {
    try {
      final Terminology term = termUtils.getTerminology(terminology, true);
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
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. <ul><li>'C3224' for <i>ncit</i></li>"
              + "<li>'C0025202' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/associations",
      produces = "application/json")
  public @ResponseBody List<Association> getAssociations(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {

    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

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
   * Returns the association entries.
   *
   * @param terminology the terminology
   * @param codeOrLabel the code or label
   * @param fromRecord the from record
   * @param pageSize the page size
   * @return the association antries
   * @throws Exception the exception
   */
  @ApiOperation(
      value = "Get the association entries for the specified terminology and code. Associations used to define subset membership are not resolved by this call",
      response = AssociationEntryResultList.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "codeOrLabel",
          value = "Code/label in the specified terminology, e.g. "
              + "'A5' or 'Has_Salt_Form' for <i>ncit</i>."
              + " This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results",
          required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "0"),
      @ApiImplicitParam(name = "pageSize", value = "Max number of results to return",
          required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "10")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/concept/{terminology}/associations/{codeOrLabel}", produces = "application/json")
  public @ResponseBody AssociationEntryResultList getAssociationEntries(
    @PathVariable(value = "terminology")
    final String terminology, @PathVariable(value = "codeOrLabel")
    String codeOrLabel, @RequestParam("fromRecord")
    Optional<Integer> fromRecord, @RequestParam("pageSize")
    Optional<Integer> pageSize) throws Exception {
    // Get the association "label"
    Long startTime = System.currentTimeMillis();

    Optional<Concept> association =
        metadataService.getAssociation(terminology, codeOrLabel, Optional.ofNullable("minimal"));
    if (!association.isPresent())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Association " + codeOrLabel + " not found");
    String label = association.get().getName();
    String code = association.get().getCode();
    if (termUtils.getTerminology(terminology, true).getMetadata().getSubsetMember()
        .contains(code)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Associations used to define subset membership are not resolved by this call");
    }
    // Getting the list
    AssociationEntryResultList list = metadataService.getAssociationEntries(terminology, label,
        fromRecord.orElse(0), pageSize.orElse(10));
    list.setTimeTaken(System.currentTimeMillis() - startTime);

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
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g." + "<ul><li>'C3224' for <i>ncit</i></li>"
              + "<li>'C0025202' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET,
      value = "/concept/{terminology}/{code}/inverseAssociations", produces = "application/json")
  public @ResponseBody List<Association> getInverseAssociations(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {
    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

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
   * Returns the subsets.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the subsets
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get subset members for the specified terminology and code",
      response = Association.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'", required = true,
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code", value = "Code for a subset concept in the specified "
          + "terminology, e.g. 'C157225' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results",
          required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "0"),
      @ApiImplicitParam(name = "pageSize", value = "Max number of results to return",
          required = false, dataTypeClass = String.class, paramType = "query"),
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/subsetMembers/{code}",
      produces = "application/json")
  public @ResponseBody List<Concept> getSubsetMembers(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("fromRecord")
  final Optional<Integer> fromRecord, @RequestParam(value = "pageSize")
  final Optional<Integer> pageSize, @PathVariable(value = "code")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {
    try {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse("minimal"));

      final Optional<Concept> concept =
          elasticQueryService.getConcept(code, term, new IncludeParam("inverseAssociations"));

      if (!concept.isPresent()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      List<Concept> subsets = new ArrayList<>();
      int associationListSize = concept.get().getInverseAssociations().size();

      if (associationListSize > 0) {
        final int fromIndex = fromRecord.orElse(0);
        final int toIndex =
            Math.min(pageSize.orElse(associationListSize) + fromIndex, associationListSize);
        for (Association assn : concept.get().getInverseAssociations().subList(fromIndex,
            toIndex)) {
          final Concept member =
              elasticQueryService.getConcept(assn.getRelatedCode(), term, ip).orElse(null);
          if (member != null) {
            subsets.add(member);
          } else {
            logger.warn("Unexpected subset member that could not be loaded as a concept = "
                + term.getTerminology() + ", " + assn.getRelatedCode());
          }
        }
      }

      return subsets;
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
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/roles",
      produces = "application/json")
  public @ResponseBody List<Role> getRoles(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {

    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

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
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3224' for <i>ncit</i>.  This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/inverseRoles",
      produces = "application/json")
  public @ResponseBody List<Role> getInverseRoles(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {

    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

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
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "<ul><li>'C3224' for <i>ncit</i></li><li>'C0025202' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/parents",
      produces = "application/json")
  public @ResponseBody List<Concept> getParents(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {

    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

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
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "<ul><li>'C3224' for <i>ncit</i></li><li>'C0025202' for <i>ncim</i></li></ul>",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/children",
      produces = "application/json")
  public @ResponseBody List<Concept> getChildren(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {

    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

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
   * @param maxLevel the max level
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
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit''", required = true,
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results",
          required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "0"),
      @ApiImplicitParam(name = "pageSize", value = "Max number of results to return",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "10000"),
      @ApiImplicitParam(name = "maxLevel", value = "Max level of results to return",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "10000")
  })
  public @ResponseBody List<Concept> getDescendants(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code, @RequestParam("fromRecord")
  final Optional<Integer> fromRecord, @RequestParam("pageSize")
  final Optional<Integer> pageSize, @RequestParam("maxLevel")
  final Optional<Integer> maxLevel) throws Exception {
    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, code + " not found");
      }

      final List<Concept> baseList =
          new ArrayList<Concept>(elasticQueryService.getDescendants(code, term));
      Predicate<Concept> byLevel = concept -> concept.getLevel() <= maxLevel.orElse(10000);
      final List<Concept> list = baseList.stream().filter(byLevel).collect(Collectors.toList());

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
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/maps",
      produces = "application/json")
  public @ResponseBody List<Map> getMaps(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {

    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

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
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3910' for <i>ncit</i>.  This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/{code}/disjointWith",
      produces = "application/json")
  public @ResponseBody List<DisjointWith> getDisjointWith(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {
    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

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
      @ApiImplicitParam(name = "terminology",
          value = "Terminology, e.g. 'ncit'.  This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal")
  })
  public @ResponseBody List<Concept> getRoots(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("include")
  final Optional<String> include) throws Exception {

    try {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      final List<Concept> list = elasticQueryService.getRootNodes(term, ip);
      if (list == null || list.isEmpty()) {
        return new ArrayList<>();
      }
      // "leaf" should be set to false for all roots
      for (final Concept c : list) {
        c.setLeaf(false);
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
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal")
  })
  public @ResponseBody List<List<Concept>> getPathsFromRoot(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {

    try {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final Paths paths = elasticQueryService.getPathsToRoot(code, term);

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
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  public @ResponseBody List<HierarchyNode> getSubtree(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {

    try {
      final Terminology term = termUtils.getTerminology(terminology, true);

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
//      final List<HierarchyNode> nodes = elasticQueryService.getPathInHierarchy(code, term);
//      return nodes;

      List<HierarchyNode> rootNodes = elasticQueryService.getRootNodesHierarchy(term);
      Paths paths = elasticQueryService.getPathsToRoot(code, term);

      // root hierarchy node map for quick look up
      HashMap<String, HierarchyNode> rootNodeMap = new HashMap<>();
      rootNodes.stream().forEach(n -> rootNodeMap.put(n.getCode(), n));

      List<Path> ps = paths.getPaths();
      for (Path path : ps) {
        final List<ConceptMinimal> concepts = path.getConcepts();
        if (CollectionUtils.isEmpty(concepts) || concepts.size() < 2) {
          continue;
        }
        final ConceptMinimal rootConcept = concepts.get(concepts.size() - 1);

        final HierarchyNode root = rootNodeMap.get(rootConcept.getCode());
        HierarchyNode previous = root;
        for (int j = concepts.size() - 2; j >= 0; j--) {
          ConceptMinimal c = concepts.get(j);
          if (!previous.getChildren().stream().anyMatch(n -> n.getCode().equals(c.getCode()))) {
            List<HierarchyNode> children = elasticQueryService.getChildNodes(previous.getCode(), 0, term);
            for (HierarchyNode child : children) {
              child.setLevel(null);
              previous.getChildren().add(child);
            }
            previous.setExpanded(true);
          }
          previous = previous.getChildren().stream().filter(n -> n.getCode().equals(c.getCode()))
              .findFirst().orElse(null);
        }
      }
      return rootNodes;
      
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
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. 'C3224' for <i>ncit</i>. "
              + "This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  public @ResponseBody List<HierarchyNode> getSubtreeChildren(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {
    try {
      final Terminology term = termUtils.getTerminology(terminology, true);
      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      // If terminology is "ncim", there are no child nodes
      if ("ncim".equals(terminology)) {
        return new ArrayList<>();
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
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal")
  })
  public @ResponseBody List<List<Concept>> getPathsToRoot(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code, @RequestParam("include")
  final Optional<String> include) throws Exception {
    try {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final Paths paths = elasticQueryService.getPathsToRoot(code, term);
      logger.info("XXX paths = " + paths.getPaths().size());
      logger.info("XXX ip = " + ip);
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
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g."
              + " 'C3224' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "ancestorCode",
          value = "Ancestor code of the other specified code, e.g. "
              + "'C2991' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal")
  })
  public @ResponseBody List<List<Concept>> getPathsToAncestor(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code, @PathVariable(value = "ancestorCode")
  final String ancestorCode, @RequestParam("include")
  final Optional<String> include) throws Exception {
    try {
      final Terminology term = termUtils.getTerminology(terminology, true);
      final IncludeParam ip = new IncludeParam(include.orElse(null));

      if (!elasticQueryService.checkConceptExists(code, term)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found = " + code);
      }
      final Paths paths = elasticQueryService.getPathsToParent(code, ancestorCode, term);
      return ConceptUtils.convertPathsWithInclude(elasticQueryService, ip, term, paths, false);

    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

}
