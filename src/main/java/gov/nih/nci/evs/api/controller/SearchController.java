
package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.SearchCriteriaWithoutTerminology;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.sparql.Bindings;
import gov.nih.nci.evs.api.model.sparql.Sparql;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.service.QueryBuilderService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.EVSUtils;
import gov.nih.nci.evs.api.util.RESTUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Search controller.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "Search endpoint")
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
  
  /** The query builder service. */
  @Autowired
  QueryBuilderService queryBuilderService;

  /** The metadata service *. */
  @Autowired
  MetadataService metadataService;

  /** The es query service. */
  /* The elasticsearch query service */
  @Autowired
  ElasticQueryService esQueryService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired
  TerminologyUtils termUtils;
  
  /** The rest utils. */
  private RESTUtils restUtils = null;
  
  /**
   * Post init.
   *
   * @throws Exception the exception
   */
  @PostConstruct
  public void postInit() throws Exception {
    
    restUtils = new RESTUtils(stardogProperties.getUsername(), stardogProperties.getPassword(),
        stardogProperties.getReadTimeout(), stardogProperties.getConnectTimeout());
  }

  /**
   * Search within a single terminology.
   *
   * @param terminology the terminology
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @return the string
   * @throws Exception the exception
   */
  @Operation(summary = "Get concept search results for a specified terminology",
      description = "Use cases for search range from very simple term searches, use of paging parameters, additional filters, searches properties, roles, and associations, and so on.  To further explore the range of search options, take a look at the <a href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>Github client SDK library created for the NCI EVS Rest API</a>.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved the requested information"),
      @ApiResponse(responseCode = "400", description = "Bad request",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class))),
      @ApiResponse(responseCode = "417", description = "Expectation failed",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
      @Parameter(name = "searchCriteria", hidden = true),
      @Parameter(name = "terminology",
          description = "Single terminology to search, e.g. 'ncit' or 'ncim'"
              + " (<a href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/TERMINOLOGIES.md\">"
              + "See here for complete list</a>)",
          required = true, schema = @Schema(implementation = String.class), example = "ncit"),
      @Parameter(name = "term", description = "The term, phrase, or code to be searched, e.g. 'melanoma'",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "type",
          description = "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.",
          required = false, schema = @Schema(implementation = String.class), example = "contains"),
      @Parameter(name = "sort", description = "The search parameter to sort results by", required = false,
          schema = @Schema(implementation = String.class)),
      @Parameter(name = "ascending", description = "Sort ascending (if true) or descending (if false)",
          required = false, schema = @Schema(implementation = Boolean.class)),
      @Parameter(name = "include",
          description = "Indicator of how much data to return. Comma-separated list of any of the "
              + "following values: minimal, summary, full, associations, children, definitions,"
              + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents, properties, "
              + "roles, synonyms. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
              + "master/doc/INCLUDE.md' target='_blank'>See here for detailed information</a>.",
          required = false, schema = @Schema(implementation = String.class), example = "minimal"),
      @Parameter(name = "fromRecord", description = "Start index of the search results", required = false,
          schema = @Schema(implementation = Integer.class), example = "0"),
      @Parameter(name = "pageSize", description = "Max number of results to return", required = false,
          schema = @Schema(implementation = Integer.class), example = "10"),
      @Parameter(name = "conceptStatus",
          description = "Comma-separated list of concept status values to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>terminology=ncit</i>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "property",
          description = "Comma-separated list of properties to restrict search results by (see also <i>value</i>). e.g."
              + "<ul><li>'P106,P322' for <i>terminology=ncit</i></li>"
              + "<li>'COLOR,SHAPE' for <i>terminology=ncim</i></li></ul>"
              + "<p><a href='/api/v1/metadata/ncit/properties' target='_blank'>Click here for a "
              + "list of NCI Thesaurus properties</a>.</p>"
              + "<p><a href='/api/v1/metadata/ncim/properties' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus properties</a>.</p> " + "The properties can be specified as code or name. "
              + "NOTE: This feature works with <i>value</i> to find concepts having one of the specified "
              + "properties with an exact value matching the <i>value</i> parameter.  Using a <i>term</i> "
              + "will further restrict results to those also matching the term.",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "value",
          description = "A property value to restrict search results by.  "
              + "NOTE: This feature works with <i>property</i> to find concepts having one of the specified "
              + "properties with an exact value matching this parameter.  Using a <i>term</i> "
              + "will further restrict results to those also matching the term.",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "definitionSource",
          description = "Comma-separated list of definition sources to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/definitionSources' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>.</p>"
              + "<p><a href='/api/v1/metadata/ncim/definitionSources' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus values</a>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "definitionType",
          description = "Comma-separated list of definition types to restrict search results by, e.g. "
              + "'DEFINITION,ALT_DEFINITION' for <i>terminology=ncit</i>. "
              + "<p><a href='/api/v1/metadata/ncit/definitionTypes' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>terminology=ncit</i>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "synonymSource",
          description = "Comma-separated list of synonym sources to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/synonymSources' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>.</p> "
              + "<p><a href='/api/v1/metadata/ncim/synonymSources' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus values</a>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "synonymType",
          description = "Comma-separated list of synonym types to restrict search results by, e.g. "
              + "'FULL_SYN'. <p><a href='/api/v1/metadata/ncit/synonymTypes' target='_blank'>"
              + "Click here for a list of NCI Thesaurus values</a>. This parameter is only "
              + "meaningful for <i>terminology=ncit</i>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "synonymTermType",
          description = "Comma-separated list of synonym term type values to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/termTypes' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>.</p> "
              + "<p><a href='/api/v1/metadata/ncim/termTypes' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus values</a>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "subset",
          description = "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'."
              + " The value '*' can also be used to return results that participate in at least one subset."
              + " This parameter is only meaningful for <i>terminology=ncit</i>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "X-EVSRESTAPI-License-Key",
      description = "Required license information for restricted terminologies. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
          + "master/doc/LICENSE.md' target='_blank'>See here for detailed information</a>.",
      required = false, schema = @Schema(implementation = String.class))
  // These are commented out because they are currently not supported
  // @Parameter(name = "inverse", description = "Used with \"associations\"
  // or \"roles\" when true to indicate that inverse associations or roles
  // should be searched", required = false, schema = @Schema(implementation = String.class),
  // paramType =
  // "query", example = "false"),
  // @Parameter(name = "association", description = "Comma-separated list
  // of associations to search. e.g A10,A215. <a
  // href='/api/v1/metadata/ncit/associations' target='_blank'>Click here for
  // a list of NCI Thesaurus associations</a>. The associations can be
  // specified as code or name", required = false, schema = @Schema(implementation =
  // String.class),
  // paramType = "query"),
  // @Parameter(name = "role", description = "Comma-separated list of roles
  // to search. e.g R15,R193. <a href='/api/v1/metadata/ncit/roles'
  // target='_blank'>Click here for a list of NCI Thesaurus roles</a>. The
  // roles can be specified as code or name", required = false, dataTypeClass =
  // String.class)
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/search", produces = "application/json")
  public @ResponseBody ConceptResultList searchSingleTerminology(@PathVariable(value = "terminology")
  final String terminology, @ModelAttribute
  SearchCriteriaWithoutTerminology searchCriteria, BindingResult bindingResult, @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false)
  final String license) throws Exception {
    final Terminology term = termUtils.getTerminology(terminology, true);
    termUtils.checkLicense(term, license);
    return search(new SearchCriteria(searchCriteria, terminology), bindingResult, license);
  }

  /**
   * Search.
   *
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @return the string
   * @throws Exception the exception
   */
  @Operation(summary = "Get concept search results",
      description = "Use cases for search range from very simple term searches, use of paging "
          + "parameters, additional filters, searches properties, roles, and associations,"
          + " and so on.  To further explore the range of search options, take a look "
          + "at the <a href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>"
          + "Github client SDK library created for the NCI EVS Rest API</a>.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved the requested information"),
      @ApiResponse(responseCode = "400", description = "Bad request",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class))),
      @ApiResponse(responseCode = "404", description = "Resource not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class))),
      @ApiResponse(responseCode = "417", description = "Expectation failed",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
      @Parameter(name = "searchCriteria", hidden = true),
      @Parameter(name = "terminology",
          description = "Comma-separated list of terminologies to search, e.g. 'ncit' or 'ncim'"
              + " (<a href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/TERMINOLOGIES.md\">"
              + "See here for complete list</a>)",
          required = false, schema = @Schema(implementation = String.class), example = "ncit"),
      @Parameter(name = "term", description = "The term, phrase, or code to be searched, e.g. 'melanoma'",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "type",
          description = "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.",
          required = false, schema = @Schema(implementation = String.class), example = "contains"),
      @Parameter(name = "sort", description = "The search parameter to sort results by", required = false,
          schema = @Schema(implementation = String.class)),
      @Parameter(name = "ascending", description = "Sort ascending (if true) or descending (if false)",
          required = false, schema = @Schema(implementation = Boolean.class)),
      @Parameter(name = "include",
          description = "Indicator of how much data to return. Comma-separated list of any of the "
              + "following values: minimal, summary, full, associations, children, definitions,"
              + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents, properties, "
              + "roles, synonyms. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
              + "master/doc/INCLUDE.md' target='_blank'>See here for detailed information</a>.",
          required = false, schema = @Schema(implementation = String.class), example = "minimal"),
      @Parameter(name = "fromRecord", description = "Start index of the search results", required = false,
          schema = @Schema(implementation = Integer.class), example = "0"),
      @Parameter(name = "pageSize", description = "Max number of results to return", required = false,
          schema = @Schema(implementation = Integer.class), example = "10"),
      @Parameter(name = "conceptStatus",
          description = "Comma-separated list of concept status values to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>terminology=ncit</i>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "property",
          description = "Comma-separated list of properties to restrict search results by (see also <i>value</i>). e.g."
              + "<ul><li>'P106,P322' for <i>terminology=ncit</i></li>"
              + "<li>'COLOR,SHAPE' for <i>terminology=ncim</i></li></ul>"
              + "<p><a href='/api/v1/metadata/ncit/properties' target='_blank'>Click here for a "
              + "list of NCI Thesaurus properties</a>.</p>"
              + "<p><a href='/api/v1/metadata/ncim/properties' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus properties</a>.</p> " + "The properties can be specified as code or name. "
              + "NOTE: This feature works with <i>value</i> to find concepts having one of the specified "
              + "properties with an exact value matching the <i>value</i> parameter.  Using a <i>term</i> "
              + "will further restrict results to those also matching the term.",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "value",
          description = "A property value to restrict search results by.  "
              + "NOTE: This feature works with <i>property</i> to find concepts having one of the specified "
              + "properties with an exact value matching this parameter.  Using a <i>term</i> "
              + "will further restrict results to those also matching the term.",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "definitionSource",
          description = "Comma-separated list of definition sources to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/definitionSources' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>.</p>"
              + "<p><a href='/api/v1/metadata/ncim/definitionSources' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus values</a>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "definitionType",
          description = "Comma-separated list of definition types to restrict search results by, e.g. "
              + "'DEFINITION,ALT_DEFINITION' for <i>terminology=ncit</i>. "
              + "<p><a href='/api/v1/metadata/ncit/definitionTypes' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>terminology=ncit</i>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "synonymSource",
          description = "Comma-separated list of synonym sources to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/synonymSources' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>.</p> "
              + "<p><a href='/api/v1/metadata/ncim/synonymSources' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus values</a>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "synonymType",
          description = "Comma-separated list of synonym types to restrict search results by, e.g. "
              + "'FULL_SYN'. <p><a href='/api/v1/metadata/ncit/synonymTypes' target='_blank'>"
              + "Click here for a list of NCI Thesaurus values</a>. This parameter is only "
              + "meaningful for <i>terminology=ncit</i>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "synonymTermType",
          description = "Comma-separated list of synonym term type values to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/termTypes' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>.</p> "
              + "<p><a href='/api/v1/metadata/ncim/termTypes' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus values</a>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "subset",
          description = "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'."
              + " The value '*' can also be used to return results that participate in at least one subset."
              + " This parameter is only meaningful for <i>terminology=ncit</i>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "X-EVSRESTAPI-License-Key",
      description = "Required license information for restricted terminologies. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
          + "master/doc/LICENSE.md' target='_blank'>See here for detailed information</a>.",
      required = false, schema = @Schema(implementation = String.class))
  // These are commented out because they are currently not supported
  // @Parameter(name = "inverse", value = "Used with \"associations\"
  // or \"roles\" when true to indicate that inverse associations or roles
  // should be searched", required = false, schema = @Schema(implementation = String.class),
  // paramType =
  // "query", example = "false"),
  // @Parameter(name = "association", value = "Comma-separated list
  // of associations to search. e.g A10,A215. <a
  // href='/api/v1/metadata/ncit/associations' target='_blank'>Click here for
  // a list of NCI Thesaurus associations</a>. The associations can be
  // specified as code or name", required = false, schema = @Schema(implementation =
  // String.class),
  // paramType = "query"),
  // @Parameter(name = "role", value = "Comma-separated list of roles
  // to search. e.g R15,R193. <a href='/api/v1/metadata/ncit/roles'
  // target='_blank'>Click here for a list of NCI Thesaurus roles</a>. The
  // roles can be specified as code or name", required = false, dataTypeClass =
  // String.class)
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/search", produces = "application/json")
  public @ResponseBody ConceptResultList search(@ModelAttribute
  SearchCriteria searchCriteria, BindingResult bindingResult, @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false)
  final String license) throws Exception {

    // Check whether or not parameter binding was successful
    if (bindingResult.hasErrors()) {
      final List<FieldError> errors = bindingResult.getFieldErrors();
      final List<String> errorMessages = new ArrayList<>();
      for (final FieldError error : errors) {
        final String errorMessage =
            "ERROR " + bindingResult.getObjectName() + " = " + error.getField() + ", " + error.getCode();
        logger.error(errorMessage);
        errorMessages.add(errorMessage);
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("\n ", errorMessages));
    }

    final long startDate = System.currentTimeMillis();

    // Check search criteria for required fields
    
      if (!searchCriteria.checkRequiredFields()) { throw new
      ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required field = " +
      searchCriteria.computeMissingRequiredFields()); }
     

    searchCriteria.checkPagination();
    logger.debug("  Search = " + searchCriteria);

    if (searchCriteria.getTerminology().size() == 0) {
      // Implicitly use ncit
      searchCriteria.getTerminology().add("ncit");
    }

    try {
      final List<Terminology> terminologies = new ArrayList<>();
      for (String terminology : searchCriteria.getTerminology()) {
        final Terminology term = termUtils.getTerminology(terminology, true);
        termUtils.checkLicense(term, license);
        searchCriteria.validate(term, metadataService);
        terminologies.add(term);
      }

      final ConceptResultList results = elasticSearchService.search(terminologies, searchCriteria);

      // Look up info for all the concepts
      for (final Concept result : results.getConcepts()) {
        ConceptUtils.applyHighlights(result, result.getHighlights());
        // Clear highlights now that they have been applied
        result.setHighlights(null);
      }

      results.setTimeTaken(System.currentTimeMillis() - startDate);
      return results;
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      handleException(e);
      return null;
    }

  }
  
  
  /**
   * Search within a single terminology by SPARQL query.
   *
   * @param terminology the terminology
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @return the string
   * @throws ResponseStatusException 
   * @throws Exception the exception
   */
  @Operation(summary = "Get concept search results for a specified terminology",
      description = "Use cases for search range from very simple term searches, use of paging parameters, additional filters, searches properties, roles, and associations, and so on.  To further explore the range of search options, take a look at the <a href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>Github client SDK library created for the NCI EVS Rest API</a>.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved the requested information"),
      @ApiResponse(responseCode = "400", description = "Bad request",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class))),
      @ApiResponse(responseCode = "417", description = "Expectation failed",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
      @Parameter(name = "searchCriteria", hidden = true),
      @Parameter(name = "query", description = "The SPARQL query to run"),
      @Parameter(name = "terminology",
          description = "Single terminology to search, e.g. 'ncit' or 'ncim'"
              + " (<a href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/TERMINOLOGIES.md\">"
              + "See here for complete list</a>)",
          required = true, schema = @Schema(implementation = String.class), example = "ncit"),
      @Parameter(name = "term", description = "The term, phrase, or code to be searched, e.g. 'melanoma'",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "type",
          description = "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.",
          required = false, schema = @Schema(implementation = String.class), example = "contains"),
      @Parameter(name = "sort", description = "The search parameter to sort results by", required = false,
          schema = @Schema(implementation = String.class)),
      @Parameter(name = "ascending", description = "Sort ascending (if true) or descending (if false)",
          required = false, schema = @Schema(implementation = Boolean.class)),
      @Parameter(name = "include",
          description = "Indicator of how much data to return. Comma-separated list of any of the "
              + "following values: minimal, summary, full, associations, children, definitions,"
              + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents, properties, "
              + "roles, synonyms. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
              + "master/doc/INCLUDE.md' target='_blank'>See here for detailed information</a>.",
          required = false, schema = @Schema(implementation = String.class), example = "minimal"),
      @Parameter(name = "fromRecord", description = "Start index of the search results", required = false,
          schema = @Schema(implementation = Integer.class), example = "0"),
      @Parameter(name = "pageSize", description = "Max number of results to return", required = false,
          schema = @Schema(implementation = Integer.class), example = "10"),
      @Parameter(name = "conceptStatus",
          description = "Comma-separated list of concept status values to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>terminology=ncit</i>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "property",
          description = "Comma-separated list of properties to restrict search results by (see also <i>value</i>). e.g."
              + "<ul><li>'P106,P322' for <i>terminology=ncit</i></li>"
              + "<li>'COLOR,SHAPE' for <i>terminology=ncim</i></li></ul>"
              + "<p><a href='/api/v1/metadata/ncit/properties' target='_blank'>Click here for a "
              + "list of NCI Thesaurus properties</a>.</p>"
              + "<p><a href='/api/v1/metadata/ncim/properties' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus properties</a>.</p> " + "The properties can be specified as code or name. "
              + "NOTE: This feature works with <i>value</i> to find concepts having one of the specified "
              + "properties with an exact value matching the <i>value</i> parameter.  Using a <i>term</i> "
              + "will further restrict results to those also matching the term.",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "value",
          description = "A property value to restrict search results by.  "
              + "NOTE: This feature works with <i>property</i> to find concepts having one of the specified "
              + "properties with an exact value matching this parameter.  Using a <i>term</i> "
              + "will further restrict results to those also matching the term.",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "definitionSource",
          description = "Comma-separated list of definition sources to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/definitionSources' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>.</p>"
              + "<p><a href='/api/v1/metadata/ncim/definitionSources' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus values</a>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "definitionType",
          description = "Comma-separated list of definition types to restrict search results by, e.g. "
              + "'DEFINITION,ALT_DEFINITION' for <i>terminology=ncit</i>. "
              + "<p><a href='/api/v1/metadata/ncit/definitionTypes' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>terminology=ncit</i>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "synonymSource",
          description = "Comma-separated list of synonym sources to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/synonymSources' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>.</p> "
              + "<p><a href='/api/v1/metadata/ncim/synonymSources' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus values</a>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "synonymType",
          description = "Comma-separated list of synonym types to restrict search results by, e.g. "
              + "'FULL_SYN'. <p><a href='/api/v1/metadata/ncit/synonymTypes' target='_blank'>"
              + "Click here for a list of NCI Thesaurus values</a>. This parameter is only "
              + "meaningful for <i>terminology=ncit</i>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "synonymTermType",
          description = "Comma-separated list of synonym term type values to restrict search results by. "
              + "<p><a href='/api/v1/metadata/ncit/termTypes' target='_blank'>Click here for a "
              + "list of NCI Thesaurus values</a>.</p> "
              + "<p><a href='/api/v1/metadata/ncim/termTypes' target='_blank'>Click here for a "
              + "list of NCI Metathesaurus values</a>.</p>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "subset",
          description = "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'."
              + " The value '*' can also be used to return results that participate in at least one subset."
              + " This parameter is only meaningful for <i>terminology=ncit</i>",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "X-EVSRESTAPI-License-Key",
      description = "Required license information for restricted terminologies. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
          + "master/doc/LICENSE.md' target='_blank'>See here for detailed information</a>.",
      required = false, schema = @Schema(implementation = String.class))
  // These are commented out because they are currently not supported
  // @Parameter(name = "inverse", description = "Used with \"associations\"
  // or \"roles\" when true to indicate that inverse associations or roles
  // should be searched", required = false, schema = @Schema(implementation = String.class),
  // paramType =
  // "query", example = "false"),
  // @Parameter(name = "association", description = "Comma-separated list
  // of associations to search. e.g A10,A215. <a
  // href='/api/v1/metadata/ncit/associations' target='_blank'>Click here for
  // a list of NCI Thesaurus associations</a>. The associations can be
  // specified as code or name", required = false, schema = @Schema(implementation =
  // String.class),
  // paramType = "query"),
  // @Parameter(name = "role", description = "Comma-separated list of roles
  // to search. e.g R15,R193. <a href='/api/v1/metadata/ncit/roles'
  // target='_blank'>Click here for a list of NCI Thesaurus roles</a>. The
  // roles can be specified as code or name", required = false, dataTypeClass =
  // String.class)
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/search/sparql", produces = "application/json")
  public @ResponseBody ConceptResultList searchSingleTerminologySparql(
    @PathVariable(value = "terminology") final String terminology, 
    @RequestParam(required = true, name = "query") final String query, 
    @ModelAttribute SearchCriteriaWithoutTerminology searchCriteria, 
    @RequestParam(required = false, name = "include") final Optional<String> include, 
    BindingResult bindingResult, 
    @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license) 
  throws ResponseStatusException, Exception {
      
    final Terminology term = termUtils.getTerminology(terminology, true);
    final IncludeParam ip = new IncludeParam(include.orElse("summary"));
    String res = null;
    termUtils.checkLicense(term, license);
    
    try {
      
      final String queryPrefix = queryBuilderService.constructPrefix(term);
      String sparqlQuery = query.replaceAll("\\{\s*GRAPH(\s*<.*>\s*|\s*)\\{", "{ GRAPH <" + term.getGraph() + "> {");
      sparqlQuery += " LIMIT 1000";
      res = restUtils.runSPARQL(queryPrefix + sparqlQuery, stardogProperties.getQueryUrl());
      
    } catch (final Exception e) {
      
      final String replaceString = "{\"message\":\"com.complexible.stardog.plan.eval.ExecutionException:";
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage().replace(replaceString, "\""));
    }
    
    try {
      
      final ObjectMapper mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  
      final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
      final Bindings[] bindings = sparqlResult.getResults().getBindings();
      final List<String> codes = new ArrayList<>();
      
      for (final Bindings b : bindings) {
        
        if (b.getConceptCode() == null || b.getConceptCode().getValue().isEmpty()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "SPARQL query must return concept codes");
        }
        
        codes.add(b.getConceptCode().getValue());
      }
      
      if (codes.size() == 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "SPARQL query returned no codes");
        
      } else if (codes.size() > 1000) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Maximum number of concepts to request at a time is 1000 = " + codes.size());
      }
      
      searchCriteria.setCodeList(codes);
  
      return search(new SearchCriteria(searchCriteria, terminology), bindingResult, license);
      
    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }
}