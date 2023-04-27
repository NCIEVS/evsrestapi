
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

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Controller for /subset endpoints.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Subset endpoints")
public class SubsetController extends BaseController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(SubsetController.class);

  /** The elastic query service. */
  @Autowired
  ElasticQueryService elasticQueryService;

  /** The metadata service. */
  @Autowired
  MetadataService metadataService;

  /** The term utils. */
  @Autowired
  TerminologyUtils termUtils;

  /**
   * Returns the subsets.
   *
   * @param terminology the terminology
   * @param include the include
   * @param list the list
   * @return the properties
   * @throws Exception the exception
   */
  @ApiOperation(
      value = "Get all subsets (or those specified by list parameter) for the specified terminology",
      response = Concept.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request")
  })
  @RequestMapping(method = RequestMethod.GET, value = "/subset/{terminology}",
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
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "list",
          value = "List of codes or labels to return subsets for (or leave blank for all).  If invalid values are passed, the result will simply include no entries for those invalid values.",
          required = false, dataTypeClass = String.class, paramType = "query")
  })
  @RecordMetric
  public @ResponseBody List<Concept> getSubsets(@PathVariable(value = "terminology")
  final String terminology, @RequestParam(required = false, name = "include")
  final Optional<String> include, @RequestParam(required = false, name = "list")
  final Optional<String> list) throws Exception {
    try {
      return metadataService.getSubsets(terminology, include, list);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the subset.
   *
   * @param terminology the terminology
   * @param code the code
   * @param include the include
   * @return the subset
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get the subset for the specified terminology and code",
      response = Concept.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 417, message = "Unexpected duplicate found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit'.", required = true,
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Subset code, e.g. 'C116978' for <i>ncit</i>. This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data tc return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "summary")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/subset/{terminology}/{code}",
      produces = "application/json")
  public @ResponseBody Concept getSubset(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code, @RequestParam(required = false, name = "include")
  final Optional<String> include) throws Exception {
    try {
      // If the code contains a comma, just bail
      if (code.contains(",")) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subset " + code + " not found");
      }
      Optional<Concept> concept = metadataService.getSubset(terminology, code, include);
      if (!concept.isPresent())
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subset " + code + " not found");
      return concept.get();
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the subsets.
   *
   * @param terminology the terminology
   * @param fromRecord the from record
   * @param pageSize the page size
   * @param code the code
   * @param include the include
   * @return the subsets
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get subset members for the specified terminology and code. "
      + "Concept subset endpoints will be deprecated in v2 in favor of top level subset endpoints.",
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
              + "minimal, summary, full, associations, children, definitions, disjointWith, history, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, dataTypeClass = String.class, paramType = "query",
          defaultValue = "minimal"),
      @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results",
          required = false, dataTypeClass = Integer.class, paramType = "query", defaultValue = "0",
          example = "0"),
      @ApiImplicitParam(name = "pageSize", value = "Max number of results to return",
          required = false, dataTypeClass = Integer.class, paramType = "query", example = "10000"),
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/subset/{terminology}/{code}/members",
      produces = "application/json")
  public @ResponseBody List<Concept> getSubsetMembers(@PathVariable(value = "terminology")
  final String terminology, @RequestParam("fromRecord")
  final Optional<Integer> fromRecord, @RequestParam(required = false, value = "pageSize")
  final Optional<Integer> pageSize, @PathVariable(required = false, value = "code")
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

      final List<Concept> subsets = new ArrayList<>();
      final int associationListSize = concept.get().getInverseAssociations().size();

      if (associationListSize > 0) {
        final int fromIndex = fromRecord.orElse(0);
        final int toIndex =
            Math.min(pageSize.orElse(associationListSize) + fromIndex, associationListSize);
        for (final Association assn : concept.get().getInverseAssociations().subList(fromIndex,
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
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

}
