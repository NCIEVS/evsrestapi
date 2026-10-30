package gov.nih.nci.evs.api.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.MapResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.SearchCriteriaWithoutTerminology;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.sparql.Bindings;
import gov.nih.nci.evs.api.model.sparql.Sparql;
import gov.nih.nci.evs.api.properties.GraphProperties;
import gov.nih.nci.evs.api.query.Typeahead;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.service.OpenSearchService;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.service.QueryBuilderService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.RESTUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Search controller. */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "Search endpoint")
public class SearchController extends BaseController {
  /** The default typeahead page size. */
  // Swagger requires default values to be a const. So we have to init this as a String and parse it
  // at run time
  private static final String DEFAULT_TYPEAHEAD_PAGE_SIZE = "10";

  /** The default typeahead fields: enough to show each suggestion, without highlights. */
  private static final String DEFAULT_TYPEAHEAD_INCLUDE = "minimal";

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

  /** The graph db properties. */
  @Autowired GraphProperties graphProperties;

  /** The opensearch search service. */
  @Autowired OpenSearchService openSearchService;

  /** The sparql query manager service. */
  @Autowired SparqlQueryManagerService sparqlQueryManagerService;

  /** The query builder service. */
  @Autowired QueryBuilderService queryBuilderService;

  /** The metadata service *. */
  @Autowired MetadataService metadataService;

  /** The es query service. */
  @Autowired OpensearchQueryService osQueryService;

  /** The term utils. */
  @Autowired TerminologyUtils termUtils;

  /** The rest utils. */
  private RESTUtils restUtils = null;

  /** The sparql timeout. */
  @Value("${nci.evs.sparql.timeoutSeconds}")
  private int sparqlTimeout;

  /**
   * Post init.
   *
   * @throws Exception the exception
   */
  @PostConstruct
  public void postInit() throws Exception {

    restUtils =
        new RESTUtils(
            graphProperties.getUsername(),
            graphProperties.getPassword(),
            graphProperties.getReadTimeout(),
            graphProperties.getConnectTimeout());
  }

  /**
   * Search within a single terminology.
   *
   * @param terminology the terminology
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @param license the license
   * @return the string
   * @throws Exception the exception
   */
  @Operation(
      summary = "Get concept search results for a specified terminology",
      description =
          "Use cases for search range from very simple term searches, use of paging parameters,"
              + " additional filters, searches properties, roles, and associations, and so on.  To"
              + " further explore the range of search options, take a look at the <a"
              + " href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>Github"
              + " client SDK library created for the NCI EVS Rest API</a>.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(name = "searchCriteria", hidden = true),
    @Parameter(
        name = "terminology",
        description =
            "Single terminology to search, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "term",
        description = "The term, phrase, or code to be searched, e.g. 'melanoma'",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "type",
        description =
            "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "contains"),
    @Parameter(
        name = "sort",
        description = "The search parameter to sort results by",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "ascending",
        description = "Sort ascending (if true) or descending (if false)",
        required = false,
        schema = @Schema(implementation = Boolean.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "10"),
    @Parameter(
        name = "conceptStatus",
        description =
            "Comma-separated list of concept status values to restrict search results by. <p><a"
                + " href='/api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>. This parameter is only meaningful for"
                + " <i>terminology=ncit</i>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "property",
        description =
            "Comma-separated list of properties to restrict search results by (see also"
                + " <i>value</i>). e.g.<ul><li>'P106,P322' for"
                + " <i>terminology=ncit</i></li><li>'COLOR,SHAPE' for"
                + " <i>terminology=ncim</i></li></ul><p><a href='/api/v1/metadata/ncit/properties'"
                + " target='_blank'>Click here for a list of NCI Thesaurus properties</a>.</p><p><a"
                + " href='/api/v1/metadata/ncim/properties' target='_blank'>Click here for a list"
                + " of NCI Metathesaurus properties</a>.</p> The properties can be specified as"
                + " code or name. NOTE: This feature works with <i>value</i> to find concepts"
                + " having one of the specified properties with an exact value matching the"
                + " <i>value</i> parameter.  Using a <i>term</i> will further restrict results to"
                + " those also matching the term.",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "value",
        description =
            "A property value to restrict search results by.  NOTE: This feature works with"
                + " <i>property</i> to find concepts having one of the specified properties with an"
                + " exact value matching this parameter.  Using a <i>term</i> will further restrict"
                + " results to those also matching the term.",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "definitionSource",
        description =
            "Comma-separated list of definition sources to restrict search results by. <p><a"
                + " href='/api/v1/metadata/ncit/definitionSources' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>.</p><p><a"
                + " href='/api/v1/metadata/ncim/definitionSources' target='_blank'>Click here for a"
                + " list of NCI Metathesaurus values</a>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "definitionType",
        description =
            "Comma-separated list of definition types to restrict search results by, e.g."
                + " 'DEFINITION,ALT_DEFINITION' for <i>terminology=ncit</i>. <p><a"
                + " href='/api/v1/metadata/ncit/definitionTypes' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>. This parameter is only meaningful for"
                + " <i>terminology=ncit</i>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "synonymSource",
        description =
            "Comma-separated list of synonym sources to restrict search results by. <p><a"
                + " href='/api/v1/metadata/ncit/synonymSources' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>.</p> <p><a"
                + " href='/api/v1/metadata/ncim/synonymSources' target='_blank'>Click here for a"
                + " list of NCI Metathesaurus values</a>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "synonymType",
        description =
            "Comma-separated list of synonym types to restrict search results by, e.g. "
                + "'FULL_SYN'. <p><a href='/api/v1/metadata/ncit/synonymTypes' target='_blank'>"
                + "Click here for a list of NCI Thesaurus values</a>. This parameter is only "
                + "meaningful for <i>terminology=ncit</i>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "synonymTermType",
        description =
            "Comma-separated list of synonym term type values to restrict search results by. "
                + "<p><a href='/api/v1/metadata/ncit/termTypes' target='_blank'>Click here for a "
                + "list of NCI Thesaurus values</a>.</p> "
                + "<p><a href='/api/v1/metadata/ncim/termTypes' target='_blank'>Click here for a "
                + "list of NCI Metathesaurus values</a>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "subset",
        description =
            "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'. The"
                + " value '*' can also be used to return results that participate in at least one"
                + " subset. This parameter is only meaningful for <i>terminology=ncit</i>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
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
  @GetMapping(value = "/concept/{terminology}/search", produces = "application/json")
  public @ResponseBody ConceptResultList searchSingleTerminology(
      @PathVariable(value = "terminology") final String terminology,
      @Parameter(hidden = true) @ModelAttribute SearchCriteriaWithoutTerminology searchCriteria,
      BindingResult bindingResult,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {
    final Terminology term = termUtils.getIndexedTerminology(terminology, osQueryService, true);
    termUtils.checkLicense(term, license);
    return search(new SearchCriteria(searchCriteria, terminology), bindingResult, license);
  }

  /**
   * Searches for concepts by keyword or ID number.
   *
   * @param incompleteTerm the partial term to search for
   * @param terminologies the terminologies to search, defaulting to ncit when omitted
   * @param pageSize the number of results per page
   * @param license the license key to restrict search results to a specific license
   * @return the list of matching concepts
   * @throws Exception if there is an error in the search process
   */
  @Operation(
      summary = "Get typeahead suggestions for a partially typed term",
      description =
          "Returns concepts from the requested terminologies (NCI Thesaurus by default) whose"
              + " preferred names contain every word typed so far, in any order, with the last word"
              + " allowed to be partial, or whose codes start with the typed text. Ignores case and"
              + " punctuation in names; codes are matched case sensitively. Leaves out retired"
              + " (inactive) concepts. A concept whose code is exactly the typed text comes first,"
              + " then"
              + " name matches, then concepts that only matched by code. Within each, general"
              + " concepts (those with more descendants) with short names come first, with a small"
              + " preference for names that start with the typed text. Intended for autocomplete"
              + " use cases; a blank term returns an empty result. Returns only the first page, and"
              + " the total counts matches only up to 10,000. Highlighting is optional: with"
              + " include=minimal,highlights, each concept's highlight is OpenSearch's highlight of"
              + " its name (or of its code, if only the code matched), which marks whole words,"
              + " e.g. \"<em>Lung</em> <em>Carcinoma</em>\" for \"lung ca\".")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Comma-separated list of terminologies to search, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "term",
        description = "The partially typed term or code to complete, e.g. 'melan'",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "melan"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return.",
        required = false,
        schema =
            @Schema(implementation = Integer.class, defaultValue = DEFAULT_TYPEAHEAD_PAGE_SIZE)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return, as for concept search. Comma-separated list of"
                + " any of the following values: minimal, summary, full, associations, children,"
                + " definitions, disjointWith, highlights, history, inverseAssociations,"
                + " inverseRoles, maps, parents, properties, roles, synonyms. Add 'highlights' to"
                + " mark the matched part of each suggestion. Typeahead runs on every keystroke, so"
                + " keep it small. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class, defaultValue = DEFAULT_TYPEAHEAD_INCLUDE),
        example = "minimal,highlights"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @GetMapping(value = "/concept/typeahead", produces = "application/json")
  public @ResponseBody ConceptResultList typeahead(
      @RequestParam(name = "term", required = false) final String incompleteTerm,
      @RequestParam(name = "terminology", required = false) final List<String> terminologies,
      @RequestParam(name = "pageSize", defaultValue = DEFAULT_TYPEAHEAD_PAGE_SIZE)
          final Integer pageSize,
      @RequestParam(name = "include", defaultValue = DEFAULT_TYPEAHEAD_INCLUDE)
          final String include,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    return typeaheadService(incompleteTerm, terminologies, license, pageSize, include);
  }

  /**
   * Finds concepts whose preferred names contain every word typed so far, in any order, with the
   * last word allowed to be partial, or whose codes start with the typed text. Ignores case and
   * punctuation in names (codes are case sensitive), and leaves out retired (inactive) concepts. A
   * concept whose code is exactly the typed text comes first, then name matches, then concepts that
   * only matched by code. Within each, general concepts with short names come first, with a small
   * preference for names that start with the typed text. Searches the requested terminologies,
   * defaulting to ncit when none are requested.
   *
   * @param incompleteTerm the partially typed term to complete
   * @param requestedTerminologies the names of the terminologies to search, or null for ncit
   * @param license the license key from the request header
   * @param pageSize the maximum number of suggestions to return
   * @param include the fields to return for each suggestion, and whether to highlight matches, or
   *     null for {@link #DEFAULT_TYPEAHEAD_INCLUDE}
   * @return the matching concepts
   * @throws Exception the exception
   */
  public ConceptResultList typeaheadService(
      final String incompleteTerm,
      final List<String> requestedTerminologies,
      final String license,
      final Integer pageSize,
      final String include)
      throws Exception {

    // Nothing typed yet (or only punctuation), so there is nothing to complete
    final boolean shouldReturnEmptyList = !Typeahead.getHasWordsToComplete(incompleteTerm);
    if (shouldReturnEmptyList) {
      final ConceptResultList results = new ConceptResultList();
      results.setTotal(0L);
      results.setTimeTaken(0L);
      return results;
    }

    // SearchCriteria carries the request parameters (term, page size, terminology, included
    // fields, and the echoed parameters in the response) and lets this reuse the existing
    // validation methods.
    final SearchCriteria searchCriteria =
        getTypeaheadSearchCriteria(incompleteTerm, requestedTerminologies, pageSize, include);

    final long timerStartDate = System.currentTimeMillis();

    checkSearchCriteria(searchCriteria);
    tryFallbackOnNCIT(searchCriteria);

    final List<Terminology> terminologies =
        parseAndValidateTerminologiesFromSearchCriteria(searchCriteria, license);

    final Typeahead typeahead = new Typeahead(terminologies, incompleteTerm);
    final ConceptResultList results =
        openSearchService.findTypeaheadConcepts(typeahead, searchCriteria);
    highlightConceptMatches(results);

    results.setTimeTaken(System.currentTimeMillis() - timerStartDate);
    return results;
  }

  /**
   * Builds the search criteria carrying the typeahead request parameters.
   *
   * @param incompleteTerm the partially typed term to complete
   * @param requestedTerminologies the names of the terminologies to search, or null to leave the
   *     terminology unset so the ncit fallback applies
   * @param pageSize the maximum number of suggestions to return, or null for the default
   * @param include the fields to return for each suggestion, or null for the default
   * @return the search criteria
   */
  private SearchCriteria getTypeaheadSearchCriteria(
      final String incompleteTerm,
      final List<String> requestedTerminologies,
      final Integer pageSize,
      final String include) {
    final SearchCriteria searchCriteria = new SearchCriteria();
    searchCriteria.setTerm(incompleteTerm);
    searchCriteria.setType("startsWith");
    final boolean hasRequestedInclude = include != null;
    searchCriteria.setInclude(hasRequestedInclude ? include : DEFAULT_TYPEAHEAD_INCLUDE);
    final boolean hasRequestedTerminologies = requestedTerminologies != null;
    if (hasRequestedTerminologies) {
      searchCriteria.setTerminology(requestedTerminologies);
    }
    if (pageSize != null) {
      searchCriteria.setPageSize(pageSize);
    }
    return searchCriteria;
  }

  /**
   * Search.
   *
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @param license the license
   * @return the string
   * @throws Exception the exception
   */
  @Operation(
      summary = "Get concept search results",
      description =
          "Use cases for search range from very simple term searches, use of paging "
              + "parameters, additional filters, searches properties, roles, and associations,"
              + " and so on.  To further explore the range of search options, take a look "
              + "at the <a href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>"
              + "Github client SDK library created for the NCI EVS Rest API</a>.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Resource not found",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(name = "searchCriteria", hidden = true),
    @Parameter(
        name = "terminology",
        description =
            "Comma-separated list of terminologies to search, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "term",
        description = "The term, phrase, or code to be searched, e.g. 'melanoma'",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "type",
        description =
            "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "contains"),
    @Parameter(
        name = "sort",
        description = "The search parameter to sort results by",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "ascending",
        description = "Sort ascending (if true) or descending (if false)",
        required = false,
        schema = @Schema(implementation = Boolean.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "10"),
    @Parameter(
        name = "conceptStatus",
        description =
            "Comma-separated list of concept status values to restrict search results by. <p><a"
                + " href='/api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>. This parameter is only meaningful for"
                + " <i>terminology=ncit</i>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "property",
        description =
            "Comma-separated list of properties to restrict search results by (see also"
                + " <i>value</i>). e.g.<ul><li>'P106,P322' for"
                + " <i>terminology=ncit</i></li><li>'COLOR,SHAPE' for"
                + " <i>terminology=ncim</i></li></ul><p><a href='/api/v1/metadata/ncit/properties'"
                + " target='_blank'>Click here for a list of NCI Thesaurus properties</a>.</p><p><a"
                + " href='/api/v1/metadata/ncim/properties' target='_blank'>Click here for a list"
                + " of NCI Metathesaurus properties</a>.</p> The properties can be specified as"
                + " code or name. NOTE: This feature works with <i>value</i> to find concepts"
                + " having one of the specified properties with an exact value matching the"
                + " <i>value</i> parameter.  Using a <i>term</i> will further restrict results to"
                + " those also matching the term.",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "value",
        description =
            "A property value to restrict search results by.  NOTE: This feature works with"
                + " <i>property</i> to find concepts having one of the specified properties with an"
                + " exact value matching this parameter.  Using a <i>term</i> will further restrict"
                + " results to those also matching the term.",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "definitionSource",
        description =
            "Comma-separated list of definition sources to restrict search results by. <p><a"
                + " href='/api/v1/metadata/ncit/definitionSources' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>.</p><p><a"
                + " href='/api/v1/metadata/ncim/definitionSources' target='_blank'>Click here for a"
                + " list of NCI Metathesaurus values</a>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "definitionType",
        description =
            "Comma-separated list of definition types to restrict search results by, e.g."
                + " 'DEFINITION,ALT_DEFINITION' for <i>terminology=ncit</i>. <p><a"
                + " href='/api/v1/metadata/ncit/definitionTypes' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>. This parameter is only meaningful for"
                + " <i>terminology=ncit</i>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "synonymSource",
        description =
            "Comma-separated list of synonym sources to restrict search results by. <p><a"
                + " href='/api/v1/metadata/ncit/synonymSources' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>.</p> <p><a"
                + " href='/api/v1/metadata/ncim/synonymSources' target='_blank'>Click here for a"
                + " list of NCI Metathesaurus values</a>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "synonymType",
        description =
            "Comma-separated list of synonym types to restrict search results by, e.g. "
                + "'FULL_SYN'. <p><a href='/api/v1/metadata/ncit/synonymTypes' target='_blank'>"
                + "Click here for a list of NCI Thesaurus values</a>. This parameter is only "
                + "meaningful for <i>terminology=ncit</i>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "synonymTermType",
        description =
            "Comma-separated list of synonym term type values to restrict search results by. "
                + "<p><a href='/api/v1/metadata/ncit/termTypes' target='_blank'>Click here for a "
                + "list of NCI Thesaurus values</a>.</p> "
                + "<p><a href='/api/v1/metadata/ncim/termTypes' target='_blank'>Click here for a "
                + "list of NCI Metathesaurus values</a>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "subset",
        description =
            "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'. The"
                + " value '*' can also be used to return results that participate in at least one"
                + " subset. This parameter is only meaningful for <i>terminology=ncit</i>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
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
  @GetMapping(value = "/concept/search", produces = "application/json")
  public @ResponseBody ConceptResultList search(
      @Parameter(hidden = true) @ModelAttribute SearchCriteria searchCriteria,
      BindingResult bindingResult,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    // Check whether or not parameter binding was successful
    errorOnInvalidBinding(bindingResult);
    final long timerStartDate = System.currentTimeMillis();
    checkSearchCriteria(searchCriteria);
    logger.debug("  Search = " + searchCriteria);

    tryFallbackOnNCIT(searchCriteria);

    try {
      final List<Terminology> terminologies =
          parseAndValidateTerminologiesFromSearchCriteria(searchCriteria, license);

      final ConceptResultList results =
          openSearchService.findConcepts(terminologies, searchCriteria);

      highlightConceptMatches(results);

      results.setTimeTaken(System.currentTimeMillis() - timerStartDate);
      return results;
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      handleException(e, null);
      return null;
    }
  }

  /**
   * Fails the request if the search criteria are missing required fields or have invalid
   * pagination.
   *
   * @param searchCriteria the search criteria
   * @throws Exception the exception
   */
  private void checkSearchCriteria(SearchCriteria searchCriteria) throws Exception {
    if (!searchCriteria.checkRequiredFields()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Missing required field = " + searchCriteria.computeMissingRequiredFields());
    }

    searchCriteria.checkPagination();
  }

  /**
   * Applies search highlights to each result concept, then clears the raw highlights so they are
   * not returned in the response.
   *
   * @param results the search results (modified in place)
   * @throws Exception the exception
   */
  private void highlightConceptMatches(final ConceptResultList results) throws Exception {
    for (final Concept result : results.getConcepts()) {
      ConceptUtils.applyHighlights(result, result.getHighlights());
      // Clear highlights now that they have been applied
      result.setHighlights(null);
    }
  }

  /**
   * Resolves each terminology named in the search criteria, checks the caller's license for it, and
   * validates the search criteria against it.
   *
   * @param searchCriteria the search criteria
   * @param license the license key from the request header
   * @return the resolved terminologies, in the order requested
   * @throws Exception if a terminology is not found, the license check fails, or the criteria are
   *     invalid for a terminology
   */
  private List<Terminology> parseAndValidateTerminologiesFromSearchCriteria(
      SearchCriteria searchCriteria, final String license) throws Exception {
    final List<Terminology> terminology = parseTerminologies(searchCriteria.getTerminology());

    checkLicenses(terminology, license);
    checkTermAgainstSearchCriteriaValidation(terminology, searchCriteria);

    return terminology;
  }

  private List<Terminology> parseTerminologies(List<String> terminologies) throws Exception {
    final List<Terminology> parsedTerminologies = new ArrayList<>();

    for (String terminology : terminologies) {
      final Terminology term = termUtils.getIndexedTerminology(terminology, osQueryService, true);
      parsedTerminologies.add(term);
    }

    return parsedTerminologies;
  }

  private void checkLicenses(List<Terminology> terminologies, final String license)
      throws Exception {
    for (final Terminology term : terminologies) {
      termUtils.checkLicense(term, license);
    }
  }

  private void checkTermAgainstSearchCriteriaValidation(
      List<Terminology> terminologies, SearchCriteria searchCriteria) throws Exception {
    for (final Terminology term : terminologies) {
      searchCriteria.validate(term, metadataService);
    }
  }

  /**
   * Defaults the search criteria terminology to ncit when none was specified.
   *
   * @param searchCriteria the search criteria (terminology may be modified)
   */
  private void tryFallbackOnNCIT(SearchCriteria searchCriteria) {
    if (searchCriteria.getTerminology().isEmpty()) {
      // Implicitly use ncit
      searchCriteria.getTerminology().add("ncit");
    }
  }

  /**
   * Fails the request if any request parameters could not be bound to the search criteria, such as
   * a non-numeric page size. Each binding error is logged.
   *
   * @param bindingResult the binding result
   * @throws ResponseStatusException with BAD_REQUEST listing each binding error
   */
  private void errorOnInvalidBinding(BindingResult bindingResult) throws ResponseStatusException {
    if (bindingResult.hasErrors()) {
      final List<FieldError> errors = bindingResult.getFieldErrors();
      final List<String> errorMessages = new ArrayList<>();
      for (final FieldError error : errors) {
        final String errorMessage =
            "ERROR "
                + bindingResult.getObjectName()
                + " = "
                + error.getField()
                + ", "
                + error.getCode();
        logger.error(errorMessage);
        errorMessages.add(errorMessage);
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("\n ", errorMessages));
    }
  }

  /**
   * Search within a single terminology by SPARQL query.
   *
   * @param terminology the terminology
   * @param include the include
   * @param query the SPARQL query to run
   * @param searchCriteria the filter criteria elastic fields
   * @param bindingResult the binding result
   * @param license the license
   * @return the string
   * @throws ResponseStatusException the response status exception
   * @throws Exception the exception
   */
  @Operation(
      summary = "Get concept search results for a specified terminology",
      description =
          "Use cases for search range from very simple term searches, use of paging parameters,"
              + " additional filters, searches properties, roles, and associations, and so on.  To"
              + " further explore the range of search options, take a look at the <a"
              + " href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>Github"
              + " client SDK library created for the NCI EVS Rest API</a>.",
      requestBody =
          @RequestBody(
              description =
                  "SPARQL query that returns ?code identifying a valid code in the specified"
                      + " terminology. <a"
                      + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/SPARQL.md'"
                      + " target='_blank'>See here for more information and examples of using"
                      + " SPARQL with EVSRESTAPI</a>.",
              required = true))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(name = "searchCriteria", hidden = true),
    @Parameter(
        name = "terminology",
        description =
            "Single terminology to search, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "term",
        description = "The term, phrase, or code to be searched, e.g. 'melanoma'",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "type",
        description =
            "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "contains"),
    @Parameter(
        name = "sort",
        description = "The search parameter to sort results by",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "ascending",
        description = "Sort ascending (if true) or descending (if false)",
        required = false,
        schema = @Schema(implementation = Boolean.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, history, inverseAssociations, inverseRoles, maps, parents,"
                + " properties, roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal"),
    @Parameter(
        name = "prefixes",
        description = "Use 'true' to use queries with declared prefixes",
        required = false,
        schema = @Schema(implementation = Boolean.class),
        example = "true"),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "10"),
    @Parameter(
        name = "conceptStatus",
        description =
            "Comma-separated list of concept status values to restrict search results by. <p><a"
                + " href='/api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>. This parameter is only meaningful for"
                + " <i>terminology=ncit</i>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "property",
        description =
            "Comma-separated list of properties to restrict search results by (see also"
                + " <i>value</i>). e.g.<ul><li>'P106,P322' for"
                + " <i>terminology=ncit</i></li><li>'COLOR,SHAPE' for"
                + " <i>terminology=ncim</i></li></ul><p><a href='/api/v1/metadata/ncit/properties'"
                + " target='_blank'>Click here for a list of NCI Thesaurus properties</a>.</p><p><a"
                + " href='/api/v1/metadata/ncim/properties' target='_blank'>Click here for a list"
                + " of NCI Metathesaurus properties</a>.</p> The properties can be specified as"
                + " code or name. NOTE: This feature works with <i>value</i> to find concepts"
                + " having one of the specified properties with an exact value matching the"
                + " <i>value</i> parameter.  Using a <i>term</i> will further restrict results to"
                + " those also matching the term.",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "value",
        description =
            "A property value to restrict search results by.  NOTE: This feature works with"
                + " <i>property</i> to find concepts having one of the specified properties with an"
                + " exact value matching this parameter.  Using a <i>term</i> will further restrict"
                + " results to those also matching the term.",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "definitionSource",
        description =
            "Comma-separated list of definition sources to restrict search results by. <p><a"
                + " href='/api/v1/metadata/ncit/definitionSources' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>.</p><p><a"
                + " href='/api/v1/metadata/ncim/definitionSources' target='_blank'>Click here for a"
                + " list of NCI Metathesaurus values</a>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "definitionType",
        description =
            "Comma-separated list of definition types to restrict search results by, e.g."
                + " 'DEFINITION,ALT_DEFINITION' for <i>terminology=ncit</i>. <p><a"
                + " href='/api/v1/metadata/ncit/definitionTypes' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>. This parameter is only meaningful for"
                + " <i>terminology=ncit</i>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "synonymSource",
        description =
            "Comma-separated list of synonym sources to restrict search results by. <p><a"
                + " href='/api/v1/metadata/ncit/synonymSources' target='_blank'>Click here for a"
                + " list of NCI Thesaurus values</a>.</p> <p><a"
                + " href='/api/v1/metadata/ncim/synonymSources' target='_blank'>Click here for a"
                + " list of NCI Metathesaurus values</a>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "synonymType",
        description =
            "Comma-separated list of synonym types to restrict search results by, e.g. "
                + "'FULL_SYN'. <p><a href='/api/v1/metadata/ncit/synonymTypes' target='_blank'>"
                + "Click here for a list of NCI Thesaurus values</a>. This parameter is only "
                + "meaningful for <i>terminology=ncit</i>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "synonymTermType",
        description =
            "Comma-separated list of synonym term type values to restrict search results by. "
                + "<p><a href='/api/v1/metadata/ncit/termTypes' target='_blank'>Click here for a "
                + "list of NCI Thesaurus values</a>.</p> "
                + "<p><a href='/api/v1/metadata/ncim/termTypes' target='_blank'>Click here for a "
                + "list of NCI Metathesaurus values</a>.</p>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "subset",
        description =
            "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'. The"
                + " value '*' can also be used to return results that participate in at least one"
                + " subset. This parameter is only meaningful for <i>terminology=ncit</i>",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
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
  @PostMapping(
      value = "/concept/{terminology}/search",
      consumes = "text/plain",
      produces = "application/json")
  public @ResponseBody ConceptResultList searchSingleTerminologySparql(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "prefixes") final Boolean prefixes,
      @org.springframework.web.bind.annotation.RequestBody final String query,
      @Parameter(hidden = true) @ModelAttribute SearchCriteriaWithoutTerminology searchCriteria,
      BindingResult bindingResult,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws ResponseStatusException, Exception {

    final Terminology term = termUtils.getIndexedTerminology(terminology, osQueryService, true);
    String res = null;

    if (query == null || query.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing SPARQL query");
    }

    if (term.getSource() == null) {
      throw new ResponseStatusException(
          HttpStatus.EXPECTATION_FAILED,
          "Terminology is not RDF-based and cannot be queried for sparql");
    }

    termUtils.checkLicense(term, license);
    final ObjectMapper mapper = ThreadLocalMapper.get();
    try {

      final String sparqlQuery = queryBuilderService.prepSparql(term, query, prefixes);
      // The following messages up "total" - so we need to either find it another way from
      // the sparql response OR we need to not limit this but do so with paging.
      //      sparqlQuery += " LIMIT 1000";

      // validate query
      res = restUtils.runSPARQL(sparqlQuery, graphProperties.getQueryUrl(), sparqlTimeout);

    } catch (final QueryException e) {
      final String errorMessage = extractErrorMessage(e.getMessage());
      throw new QueryException(
          "SPARQL query failed validation. Please review your query for syntax mistakes.\n"
              + errorMessage);

    } catch (final Exception e) {
      String errorMessage = extractErrorMessage(e.getMessage());

      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
    }

    try {

      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
      final Bindings[] bindings = sparqlResult.getResults().getBindings();

      if (bindings.length > 10000) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Too many sparql results (>10k).  Refine your query");
      }
      final List<String> codes = new ArrayList<>();

      for (final Bindings b : bindings) {

        if (b.getCode() == null || b.getCode().getValue().isEmpty()) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "SPARQL query must return a ?code parameter");
        }

        codes.add(b.getCode().getValue());
      }

      // Not sure this is necessary
      //      if (codes.size() == 0) {
      //        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SPARQL query returned no
      // codes");
      //
      //      } else if (codes.size() > 1000) {
      //        throw new ResponseStatusException(
      //            HttpStatus.BAD_REQUEST,
      //            "Maximum number of concepts to request at a time is 1000 = " + codes.size());
      //      }

      // Return empty list if no codes found
      if (codes.isEmpty()) {
        final ConceptResultList list = new ConceptResultList();
        list.setTotal(0L);
        list.setParameters(new SearchCriteria(searchCriteria, terminology));
        list.getParameters().setSparql(query);
        return list;
      }

      // Otherwise, continue and perform search
      searchCriteria.setCodeList(codes);
      final ConceptResultList list =
          search(new SearchCriteria(searchCriteria, terminology), bindingResult, license);
      list.getParameters().setSparql(query);
      return list;

    } catch (final Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * get SPARQL query bindings from a single terminology and query.
   *
   * @param terminology the terminology
   * @param fromRecord the from record
   * @param pageSize the page size
   * @param query the SPARQL query to run
   * @param license the license
   * @return the string
   * @throws ResponseStatusException the response status exception
   */
  @Operation(
      summary = "Get SPARQL query results",
      description = "Perform a SPARQL query for a specified terminology.",
      requestBody =
          @RequestBody(
              description =
                  "SPARQL query to execute on the graph for the specified terminology. <a"
                      + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/SPARQL.md'"
                      + " target='_blank'>See here for more information and examples of using"
                      + " SPARQL with EVSRESTAPI</a>.",
              required = true))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
    @ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(name = "searchCriteria", hidden = true),
    @Parameter(
        name = "terminology",
        description =
            "Single terminology to search, e.g. 'ncit' or 'ncim' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit"),
    @Parameter(
        name = "fromRecord",
        description = "Start index of the search results",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "0"),
    @Parameter(
        name = "pageSize",
        description = "Max number of results to return",
        required = false,
        schema = @Schema(implementation = Integer.class),
        example = "10"),
    @Parameter(
        name = "prefixes",
        description = "Use 'true' to use queries with declared prefixes",
        required = false,
        schema = @Schema(implementation = Boolean.class),
        example = "true"),
    @Parameter(
        name = "X-EVSRESTAPI-License-Key",
        description =
            "Required license information for restricted terminologies. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/LICENSE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @PostMapping(
      value = "/sparql/{terminology}",
      consumes = "text/plain",
      produces = "application/json")
  public @ResponseBody MapResultList getSparqlBindings(
      @PathVariable(value = "terminology") final String terminology,
      @RequestParam(required = false, name = "fromRecord") final Integer fromRecord,
      @RequestParam(required = false, name = "pageSize") final Integer pageSize,
      @RequestParam(required = false, name = "prefixes") final Boolean prefixes,
      @org.springframework.web.bind.annotation.RequestBody final String query,
      @RequestHeader(name = "X-EVSRESTAPI-License-Key", required = false) final String license)
      throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, osQueryService, true);
      if (term.getSource() == null) {
        throw new ResponseStatusException(
            HttpStatus.EXPECTATION_FAILED,
            "Terminology is not RDF-based and cannot be queried for sparql");
      }
      String res = null;
      termUtils.checkLicense(term, license);

      if (pageSize != null && (pageSize < 1 || pageSize > 1000)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Parameter 'pageSize' must be between 1 and 1000 = " + pageSize);
      }

      // If the query has a comment but no newlines, then we have a problem
      if (query.matches("(?i:.*SELECT.*#.*)") && !query.contains("\n")) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Query appears to contain a comment (#) without newline "
                + "characters (if using curl use --data-binary instead of -d)");
      }

      final ObjectMapper mapper = ThreadLocalMapper.get();
      String sparqlQuery = queryBuilderService.prepSparql(term, query, prefixes);

      // The following messages up "total" - so we need to either find it another way from
      // the sparql response OR we need to not limit this but do so with paging.
      //      sparqlQuery += " LIMIT 1000";

      // validate query
      res = restUtils.runSPARQL(sparqlQuery, graphProperties.getQueryUrl(), sparqlTimeout);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      JsonNode bindings = mapper.readTree(res).findValue("bindings");
      int total = bindings.size();
      List<Map<String, String>> results =
          getJsonSubset(
              bindings, fromRecord != null ? fromRecord : 0, pageSize != null ? pageSize : 10);
      MapResultList resultList = new MapResultList();
      resultList.setResults(results);
      resultList.setTotal((long) total);
      SearchCriteria sc = new SearchCriteria();
      sc.setPageSize(pageSize);
      sc.setFromRecord(fromRecord);
      sc.setSparql(query);
      resultList.setParameters(sc);
      return resultList;

    } catch (final QueryException e) {
      String errorMessage = extractErrorMessage(e.getMessage());

      throw new QueryException(
          "SPARQL query failed validation. Please review your query for syntax mistakes.\n"
              + errorMessage);
    } catch (final ResponseStatusException re) {
      handleException(re, terminology);
      return null;
    } catch (final Exception e) {
      String errorMessage = extractErrorMessage(e.getMessage());

      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
    }
  }

  @Operation(summary = "Get default prefixes used by SPARQL queries")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information",
        content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
        }),
    @ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class))),
    @ApiResponse(
        responseCode = "417",
        description = "Expectation failed",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
    @Parameter(
        name = "terminology",
        description =
            "Single terminology to find prefixes for, e.g. 'ncit' or 'hgnc' (<a"
                + " href=\"https://github.com/NCIEVS/evsrestapi-client-SDK/blob/main/doc/TERMINOLOGIES.md\">See"
                + " here for complete list</a>)",
        required = true,
        schema = @Schema(implementation = String.class),
        example = "ncit")
  })
  @RecordMetric
  @GetMapping(value = "/sparql/{terminology}/prefixes", produces = "application/json")
  public @ResponseBody ResponseEntity<String> getSparqlPrefixes(
      @PathVariable(value = "terminology") final String terminology) throws Exception {

    try {
      final Terminology term = termUtils.getIndexedTerminology(terminology, osQueryService, true);
      if (term.getSource() == null) {
        throw new ResponseStatusException(
            HttpStatus.EXPECTATION_FAILED,
            "Terminology is not RDF-based and cannot be queried for sparql");
      }
      final String prefixes = queryBuilderService.constructPrefix(term).replaceAll("\\r", "");
      return new ResponseEntity<>(
          ThreadLocalMapper.get().writeValueAsString(prefixes), new HttpHeaders(), HttpStatus.OK);

    } catch (final Exception e) {
      handleException(e, terminology);
      return null;
    }
  }

  /**
   * Returns the json subset.
   *
   * @param bindings the bindings
   * @param fromRecord the from record
   * @param pageSize the page size
   * @return the json subset
   */
  public List<Map<String, String>> getJsonSubset(JsonNode bindings, int fromRecord, int pageSize) {
    int endIndex = fromRecord + pageSize;
    int currentIndex = 0;
    List<Map<String, String>> results = new ArrayList<>();
    for (JsonNode node : bindings) {
      if (fromRecord <= currentIndex && currentIndex < endIndex) {
        Map<String, String> fields = new HashMap<>();
        Iterator<String> fieldNames = node.fieldNames();
        node.forEach(
            field -> {
              String currentField = fieldNames.next();
              fields.put(currentField, field.get("value").asText());
            });
        results.add(fields);

      } else if (currentIndex >= endIndex) {
        break;
      }
      currentIndex++;
    }
    return results;
  }

  /**
   * Extract error message.
   *
   * @param errorString the error string
   * @return the string
   */
  private static String extractErrorMessage(String errorString) {
    int startIndex = errorString.indexOf("Invalid SPARQL query");
    if (startIndex != -1) {
      String substring = errorString.substring(startIndex);
      return substring.replaceAll("\\\\\\\"", "\"");
    } else {
      return errorString.replaceAll("\\\\\\\"", "\"");
    }
  }
}
