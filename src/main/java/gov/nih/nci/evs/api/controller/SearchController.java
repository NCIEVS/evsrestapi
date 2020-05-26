
package gov.nih.nci.evs.api.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.aop.RecordMetricSearch;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.SearchCriteriaWithoutTerminology;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.RESTUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The Class SearchController.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Search endpoint")
public class SearchController extends BaseController {

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

  /** The stardog properties. */
  @Autowired
  StardogProperties stardogProperties;

  /** The elastic search service. */
  @Autowired
  ElasticSearchService elasticSearchService;

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /** The metadata service *. */
  @Autowired
  MetadataService metadataService;

  /**
   * Search within a single terminology.
   *
   * @param terminology the terminology
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @return the string
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get concept search results for a specified terminology",
      response = ConceptResultList.class,
      notes = "Use cases for search range from very simple term searches, use of paging parameters, additional filters, searches properties, roles, and associations, and so on.  To further explore the range of search options, take a look at the <a href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>Github client SDK library created for the NCI EVS Rest API</a>.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology",
          value = "Comma-separated list of terminologies to search, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "term",
          value = "The term, phrase, or code to be searched, e.g. 'melanoma'", required = true,
          dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "type",
          value = "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.",
          required = false, dataType = "string", paramType = "query", defaultValue = "contains"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results",
          required = false, dataType = "string", paramType = "query", defaultValue = "0"),
      @ApiImplicitParam(name = "pageSize", value = "Max number of results to return",
          required = false, dataType = "string", paramType = "query", defaultValue = "10"),
      @ApiImplicitParam(name = "conceptStatus",
          value = "Comma-separated list of concept status values to restrict search results to. <a href='api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a list of NCI Thesaurus values.</a>",
          required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "property",
          value = "Comma-separated list of properties to search. e.g P107,P108. <a href='api/v1/metadata/ncit/properties' target='_blank'>Click here for a list of NCI Thesaurus properties.</a>.The properties can be specified as code or label",
          required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "definitionSource",
          value = "Comma-separated list of definition sources to restrict search results to.",
          required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymSource",
          value = "Comma-separated list of synonym sources to restrict search results to.",
          required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymTermGroup",
          value = "Single synonym term group value to restrict search results to. Must use with \"synonymSource\".",
          required = false, dataType = "string", paramType = "query", defaultValue = "")
      // These are commented out because they are currently not supported
      // @ApiImplicitParam(name = "inverse", value = "Used with \"associations\"
      // or \"roles\" when true to indicate that inverse associations or roles
      // should be searched", required = false, dataType = "string", paramType =
      // "query", defaultValue = "false"),
      // @ApiImplicitParam(name = "association", value = "Comma-separated list
      // of associations to search. e.g A10,A215. <a
      // href='api/v1/metadata/ncit/associations' target='_blank'>Click here for
      // a list of NCI Thesaurus associations.</a>. The associations can be
      // specified as code or label", required = false, dataType = "string",
      // paramType = "query", defaultValue = ""),
      // @ApiImplicitParam(name = "role", value = "Comma-separated list of roles
      // to search. e.g R15,R193. <a href='api/v1/metadata/ncit/roles'
      // target='_blank'>Click here for a list of NCI Thesaurus roles.</a>. The
      // roles can be specified as code or label", required = false, dataType =
      // "string", paramType = "query", defaultValue = "")
  })
  @RecordMetricSearch
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/search",
      produces = "application/json")
  public @ResponseBody ConceptResultList searchSingleTerminology(
    @PathVariable(value = "terminology") final String terminology,
    @ModelAttribute SearchCriteriaWithoutTerminology searchCriteria, BindingResult bindingResult)
    throws Exception {
    return search(new SearchCriteria(searchCriteria, terminology), bindingResult);
  }

  /**
   * Search.
   *
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @return the string
   * @throws Exception the exception
   */
  @ApiOperation(value = "Get concept search results", response = ConceptResultList.class,
      notes = "Use cases for search range from very simple term searches, use of paging parameters, additional filters, searches properties, roles, and associations, and so on.  To further explore the range of search options, take a look at the <a href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>Github client SDK library created for the NCI EVS Rest API</a>.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology",
          value = "Comma-separated list of terminologies to search, e.g. 'ncit'", required = true,
          dataType = "string", paramType = "query", defaultValue = "ncit"),
      @ApiImplicitParam(name = "term",
          value = "The term, phrase, or code to be searched, e.g. 'melanoma'", required = true,
          dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "type",
          value = "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.",
          required = false, dataType = "string", paramType = "query", defaultValue = "contains"),
      @ApiImplicitParam(name = "include",
          value = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'>See here "
              + "for detailed information</a>.",
          required = false, dataType = "string", paramType = "query", defaultValue = "minimal"),
      @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results",
          required = false, dataType = "string", paramType = "query", defaultValue = "0"),
      @ApiImplicitParam(name = "pageSize", value = "Max number of results to return",
          required = false, dataType = "string", paramType = "query", defaultValue = "10"),
      @ApiImplicitParam(name = "conceptStatus",
          value = "Comma-separated list of concept status values to restrict search results to. <a href='api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a list of NCI Thesaurus values.</a>",
          required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "property",
          value = "Comma-separated list of properties to search. e.g P107,P108. <a href='api/v1/metadata/ncit/properties' target='_blank'>Click here for a list of NCI Thesaurus properties.</a>.The properties can be specified as code or label",
          required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "definitionSource",
          value = "Comma-separated list of definition sources to restrict search results to.",
          required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymSource",
          value = "Comma-separated list of synonym sources to restrict search results to.",
          required = false, dataType = "string", paramType = "query", defaultValue = ""),
      @ApiImplicitParam(name = "synonymTermGroup",
          value = "Single synonym term group value to restrict search results to. Must use with \"synonymSource\".",
          required = false, dataType = "string", paramType = "query", defaultValue = ""),
      // These are commented out because they are currently not supported
      // @ApiImplicitParam(name = "inverse", value = "Used with \"associations\"
      // or \"roles\" when true to indicate that inverse associations or roles
      // should be searched", required = false, dataType = "string", paramType =
      // "query", defaultValue = "false"),
      // @ApiImplicitParam(name = "association", value = "Comma-separated list
      // of associations to search. e.g A10,A215. <a
      // href='api/v1/metadata/ncit/associations' target='_blank'>Click here for
      // a list of NCI Thesaurus associations.</a>. The associations can be
      // specified as code or label", required = false, dataType = "string",
      // paramType = "query", defaultValue = ""),
      // @ApiImplicitParam(name = "role", value = "Comma-separated list of roles
      // to search. e.g R15,R193. <a href='api/v1/metadata/ncit/roles'
      // target='_blank'>Click here for a list of NCI Thesaurus roles.</a>. The
      // roles can be specified as code or label", required = false, dataType =
      // "string", paramType = "query", defaultValue = "")
  })
  @RecordMetricSearch
  @RequestMapping(method = RequestMethod.GET, value = "/concept/search",
      produces = "application/json")
  public @ResponseBody ConceptResultList search(@ModelAttribute SearchCriteria searchCriteria,
    BindingResult bindingResult) throws Exception {

    // Check whether or not parameter binding was successful
    if (bindingResult.hasErrors()) {
      final List<FieldError> errors = bindingResult.getFieldErrors();
      final List<String> errorMessages = new ArrayList<>();
      for (final FieldError error : errors) {
        final String errorMessage = "ERROR " + bindingResult.getObjectName() + " = "
            + error.getField() + ", " + error.getCode();
        logger.error(errorMessage);
        errorMessages.add(errorMessage);
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("\n ", errorMessages));
    }

    final long startDate = System.currentTimeMillis();

    // Check search criteria for required fields
    if (!searchCriteria.checkRequiredFields()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Missing required field = " + searchCriteria.computeMissingRequiredFields());
    }

    final String queryTerm = RESTUtils.escapeLuceneSpecialCharacters(searchCriteria.getTerm());
    searchCriteria.setTerm(queryTerm);
    logger.debug("  Search = " + searchCriteria);

    if (searchCriteria.getTerminology().size() > 1) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Search currently supports only terminology ncit");
    } else if (searchCriteria.getTerminology().size() == 0) {
      // Implicitly use ncit
      searchCriteria.getTerminology().add("ncit");
      // throw new ResponseStatusException(HttpStatus.NOT_FOUND,
      // "Required parameter 'terminology' is missing");
    }

    try {
      final String terminology = searchCriteria.getTerminology().get(0);
      final Terminology term =
          TerminologyUtils.getTerminology(sparqlQueryManagerService, terminology);
      final IncludeParam ip = searchCriteria.computeIncludeParam();

      searchCriteria.validate(term, metadataService);
      final ConceptResultList results = elasticSearchService.search(searchCriteria);

      // Look up info for all the concepts

      final List<Concept> concepts = new ArrayList<>();
      for (final Concept result : results.getConcepts()) {
        final Concept concept = sparqlQueryManagerService.getConcept(result.getCode(), term, ip);
        ConceptUtils.applyHighlights(concept, result.getHighlights());
        // Clear highlights now that they have been applied
        concept.setHighlights(null);
        concepts.add(concept);
      }
      results.setConcepts(concepts);
      results.setTimeTaken(System.currentTimeMillis() - startDate);
      return results;
    } catch (Exception e) {
      // TODO: remove this once updated elasticsearch is in place.
      if (e.getMessage().contains("invalid value")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
      }
      handleException(e);
      return null;
    }

  }
}