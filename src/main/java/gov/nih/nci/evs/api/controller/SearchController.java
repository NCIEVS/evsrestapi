
package gov.nih.nci.evs.api.controller;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.SearchCriteriaWithoutTerminology;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.ElasticSearchService;
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
 * The Class SearchController.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Search endpoint")
public class SearchController extends BaseController {

    /** The Constant log. */
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    public final int MAX_EXPORT_CONCEPTS = 150000;

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

    /* The elasticsearch query service */
    @Autowired
    ElasticQueryService esQueryService;

    /* The terminology utils */
    @Autowired
    TerminologyUtils termUtils;

    /**
     * Search within a single terminology.
     *
     * @param terminology    the terminology
     * @param searchCriteria the filter criteria elastic fields
     * @param bindingResult  the binding result
     * @return the string
     * @throws Exception the exception
     */
    @ApiOperation(value = "Get concept search results for a specified terminology", response = ConceptResultList.class, notes = "Use cases for search range from very simple term searches, use of paging parameters, additional filters, searches properties, roles, and associations, and so on.  To further explore the range of search options, take a look at the <a href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>Github client SDK library created for the NCI EVS Rest API</a>.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 404, message = "Resource not found")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "terminology", value = "Single terminology to search, e.g. 'ncit' or 'ncim'", required = true, dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
            @ApiImplicitParam(name = "term", value = "The term, phrase, or code to be searched, e.g. 'melanoma'", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "type", value = "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "contains"),
            @ApiImplicitParam(name = "include", value = "Indicator of how much data to return. Comma-separated list of any of the "
                    + "following values: minimal, summary, full, associations, children, definitions,"
                    + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties, "
                    + "roles, synonyms. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
                    + "master/doc/INCLUDE.md' target='_blank'>See here for detailed information</a>.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "minimal"),
            @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results", required = false, dataTypeClass = String.class, paramType = "query", example = "0", defaultValue = "0"),
            @ApiImplicitParam(name = "pageSize", value = "Max number of results to return", required = false, dataTypeClass = String.class, paramType = "query", example = "10", defaultValue = "10"),
            @ApiImplicitParam(name = "conceptStatus", value = "Comma-separated list of concept status values to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>ncit</i>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "property", value = "Comma-separated list of properties to restrict search results by (see also <i>value</i>). e.g."
                    + "<ul><li>'P106,P322' for <i>ncit</i></li>"
                    + "<li>'COLOR,SHAPE' for <i>ncim</i></li></ul>"
                    + "<p><a href='api/v1/metadata/ncit/properties' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus properties</a>.</p>"
                    + "<p><a href='api/v1/metadata/ncim/properties' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus properties</a>.</p> "
                    + "The properties can be specified as code or name. "
                    + "NOTE: This feature works with <i>value</i> to find concepts having one of the specified "
                    + "properties with an exact value matching the <i>value</i> parameter.  Using a <i>term</i> "
                    + "will further restrict results to those also matching the term.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "value", value = "A property value to restrict search results by.  "
                    + "NOTE: This feature works with <i>property</i> to find concepts having one of the specified "
                    + "properties with an exact value matching this parameter.  Using a <i>term</i> "
                    + "will further restrict results to those also matching the term.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "definitionSource", value = "Comma-separated list of definition sources to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/definitionSources' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>.</p>"
                    + "<p><a href='api/v1/metadata/ncim/definitionSources' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus values</a>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "definitionType", value = "Comma-separated list of definition types to restrict search results by, e.g. "
                    + "'DEFINITION,ALT_DEFINITION' for <i>ncit</i>. "
                    + "<p><a href='api/v1/metadata/ncit/definitionTypes' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>ncit</i>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "synonymSource", value = "Comma-separated list of synonym sources to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/synonymSources' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>.</p> "
                    + "<p><a href='api/v1/metadata/ncim/synonymSources' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus values</a>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "synonymType", value = "Comma-separated list of synonym types to restrict search results by, e.g. "
                    + "'FULL_SYN'. <p><a href='api/v1/metadata/ncit/synonymTypes' target='_blank'>"
                    + "Click here for a list of NCI Thesaurus values</a>. This parameter is only "
                    + "meaningful for <i>ncit</i>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "synonymTermType", value = "Single synonym term type value to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/termTypes' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>.</p> "
                    + "<p><a href='api/v1/metadata/ncim/termTypes' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus values</a>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "subset", value = "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'."
                    + " This parameter is only meaningful for <i>ncit</i>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "")
            // These are commented out because they are currently not supported
            // @ApiImplicitParam(name = "inverse", value = "Used with \"associations\"
            // or \"roles\" when true to indicate that inverse associations or roles
            // should be searched", required = false, dataTypeClass = String.class,
            // paramType =
            // "query", defaultValue = "false"),
            // @ApiImplicitParam(name = "association", value = "Comma-separated list
            // of associations to search. e.g A10,A215. <a
            // href='api/v1/metadata/ncit/associations' target='_blank'>Click here for
            // a list of NCI Thesaurus associations</a>. The associations can be
            // specified as code or name", required = false, dataTypeClass = String.class,
            // paramType = "query", defaultValue = ""),
            // @ApiImplicitParam(name = "role", value = "Comma-separated list of roles
            // to search. e.g R15,R193. <a href='api/v1/metadata/ncit/roles'
            // target='_blank'>Click here for a list of NCI Thesaurus roles</a>. The
            // roles can be specified as code or name", required = false, dataTypeClass =
            // String.class, paramType = "query", defaultValue = "")
    })
    @RecordMetric
    @RequestMapping(method = RequestMethod.GET, value = "/concept/{terminology}/search", produces = "application/json")
    public @ResponseBody ConceptResultList searchSingleTerminology(
            @PathVariable(value = "terminology") final String terminology,
            @ModelAttribute SearchCriteriaWithoutTerminology searchCriteria, BindingResult bindingResult)
            throws Exception {
        return search(new SearchCriteria(searchCriteria, terminology), bindingResult, false);
    }

    /**
     * Search.
     *
     * @param searchCriteria the filter criteria elastic fields
     * @param bindingResult  the binding result
     * @return the string
     * @throws Exception the exception
     */
    @ApiOperation(value = "Get concept search results", response = ConceptResultList.class, notes = "Use cases for search range from very simple term searches, use of paging "
            + "parameters, additional filters, searches properties, roles, and associations,"
            + " and so on.  To further explore the range of search options, take a look "
            + "at the <a href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>"
            + "Github client SDK library created for the NCI EVS Rest API</a>.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 404, message = "Resource not found")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "terminology", value = "Comma-separated list of terminologies to search, e.g. 'ncit' or 'ncim'", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "ncit"),
            @ApiImplicitParam(name = "term", value = "The term, phrase, or code to be searched, e.g. 'melanoma'", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "type", value = "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "contains"),
            @ApiImplicitParam(name = "include", value = "Indicator of how much data to return. Comma-separated list of any of the "
                    + "following values: minimal, summary, full, associations, children, definitions,"
                    + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties, "
                    + "roles, synonyms. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
                    + "master/doc/INCLUDE.md' target='_blank'>See here for detailed information</a>.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "minimal"),
            @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results", required = false, dataTypeClass = String.class, paramType = "query", example = "0", defaultValue = "0"),
            @ApiImplicitParam(name = "pageSize", value = "Max number of results to return", required = false, dataTypeClass = String.class, paramType = "query", example = "0", defaultValue = "10"),
            @ApiImplicitParam(name = "conceptStatus", value = "Comma-separated list of concept status values to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>ncit</i>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "property", value = "Comma-separated list of properties to restrict search results by (see also <i>value</i>). e.g."
                    + "<ul><li>'P106,P322' for <i>ncit</i></li>"
                    + "<li>'COLOR,SHAPE' for <i>ncim</i></li></ul>"
                    + "<p><a href='api/v1/metadata/ncit/properties' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus properties</a>.</p>"
                    + "<p><a href='api/v1/metadata/ncim/properties' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus properties</a>.</p> "
                    + "The properties can be specified as code or name. "
                    + "NOTE: This feature works with <i>value</i> to find concepts having one of the specified "
                    + "properties with an exact value matching the <i>value</i> parameter.  Using a <i>term</i> "
                    + "will further restrict results to those also matching the term.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "value", value = "A property value to restrict search results by.  "
                    + "NOTE: This feature works with <i>property</i> to find concepts having one of the specified "
                    + "properties with an exact value matching this parameter.  Using a <i>term</i> "
                    + "will further restrict results to those also matching the term.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "definitionSource", value = "Comma-separated list of definition sources to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/definitionSources' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>.</p>"
                    + "<p><a href='api/v1/metadata/ncim/definitionSources' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus values</a>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "definitionType", value = "Comma-separated list of definition types to restrict search results by, e.g. "
                    + "'DEFINITION,ALT_DEFINITION' for <i>ncit</i>. "
                    + "<p><a href='api/v1/metadata/ncit/definitionTypes' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>ncit</i>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "synonymSource", value = "Comma-separated list of synonym sources to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/synonymSources' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>.</p> "
                    + "<p><a href='api/v1/metadata/ncim/synonymSources' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus values</a>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "synonymType", value = "Comma-separated list of synonym types to restrict search results by, e.g. "
                    + "'FULL_SYN'. <p><a href='api/v1/metadata/ncit/synonymTypes' target='_blank'>"
                    + "Click here for a list of NCI Thesaurus values</a>. This parameter is only "
                    + "meaningful for <i>ncit</i>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "synonymTermType", value = "Single synonym term type value to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/termTypes' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>.</p> "
                    + "<p><a href='api/v1/metadata/ncim/termTypes' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus values</a>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "subset", value = "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'."
                    + " This parameter is only meaningful for <i>ncit</i>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "")
            // These are commented out because they are currently not supported
            // @ApiImplicitParam(name = "inverse", value = "Used with \"associations\"
            // or \"roles\" when true to indicate that inverse associations or roles
            // should be searched", required = false, dataTypeClass = String.class,
            // paramType =
            // "query", defaultValue = "false"),
            // @ApiImplicitParam(name = "association", value = "Comma-separated list
            // of associations to search. e.g A10,A215. <a
            // href='api/v1/metadata/ncit/associations' target='_blank'>Click here for
            // a list of NCI Thesaurus associations</a>. The associations can be
            // specified as code or name", required = false, dataTypeClass = String.class,
            // paramType = "query", defaultValue = ""),
            // @ApiImplicitParam(name = "role", value = "Comma-separated list of roles
            // to search. e.g R15,R193. <a href='api/v1/metadata/ncit/roles'
            // target='_blank'>Click here for a list of NCI Thesaurus roles</a>. The
            // roles can be specified as code or name", required = false, dataTypeClass =
            // String.class, paramType = "query", defaultValue = "")
    })
    @RecordMetric
    @RequestMapping(method = RequestMethod.GET, value = "/concept/search", produces = "application/json")
    public @ResponseBody ConceptResultList search(@ModelAttribute SearchCriteria searchCriteria,
            BindingResult bindingResult, boolean export) throws Exception {

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
        /*
         * if (!searchCriteria.checkRequiredFields()) { throw new
         * ResponseStatusException(HttpStatus.BAD_REQUEST,
         * "Missing required field = " +
         * searchCriteria.computeMissingRequiredFields()); }
         */

        if (export == false)
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
                searchCriteria.validate(term, metadataService, export);
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
     * Export search results.
     *
     * @param searchCriteria the filter criteria elastic fields
     * @param bindingResult  the binding result
     * @return the string
     * @throws Exception the exception
     */
    @ApiOperation(value = "Get exported search results", response = byte[].class, notes = "Use cases for search range from very simple term searches, use of paging "
            + "parameters, additional filters, searches properties, roles, and associations,"
            + " and so on.  To further explore the range of search options, take a look "
            + "at the <a href='https://github.com/NCIEVS/evsrestapi-client-SDK' target='_blank'>"
            + "Github client SDK library created for the NCI EVS Rest API</a>.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
            @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 404, message = "Resource not found")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "terminology", value = "Comma-separated list of terminologies to search, e.g. 'ncit' or 'ncim'", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "ncit"),
            @ApiImplicitParam(name = "term", value = "The term, phrase, or code to be searched, e.g. 'melanoma'", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "type", value = "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "contains"),
            @ApiImplicitParam(name = "sortBy", value = "The search parameter to sort results by", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "descending", value = "Sort by descending (if true) or ascending (if false)", required = false, dataTypeClass = Boolean.class, paramType = "query"),
            @ApiImplicitParam(name = "include", value = "Indicator of how much data to return. Comma-separated list of any of the "
                    + "following values: minimal, summary, full, associations, children, definitions,"
                    + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties, "
                    + "roles, synonyms. <a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/"
                    + "master/doc/INCLUDE.md' target='_blank'>See here for detailed information</a>.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "minimal"),
            @ApiImplicitParam(name = "fromRecord", value = "Start index of the search results", required = false, dataTypeClass = String.class, paramType = "query", example = "0", defaultValue = "0"),
            @ApiImplicitParam(name = "pageSize", value = "Max number of results to return", required = false, dataTypeClass = String.class, paramType = "query", example = "0", defaultValue = "10"),
            @ApiImplicitParam(name = "conceptStatus", value = "Comma-separated list of concept status values to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/conceptStatuses' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>ncit</i>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "property", value = "Comma-separated list of properties to restrict search results by (see also <i>value</i>). e.g."
                    + "<ul><li>'P106,P322' for <i>ncit</i></li>"
                    + "<li>'COLOR,SHAPE' for <i>ncim</i></li></ul>"
                    + "<p><a href='api/v1/metadata/ncit/properties' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus properties</a>.</p>"
                    + "<p><a href='api/v1/metadata/ncim/properties' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus properties</a>.</p> "
                    + "The properties can be specified as code or name. "
                    + "NOTE: This feature works with <i>value</i> to find concepts having one of the specified "
                    + "properties with an exact value matching the <i>value</i> parameter.  Using a <i>term</i> "
                    + "will further restrict results to those also matching the term.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "value", value = "A property value to restrict search results by.  "
                    + "NOTE: This feature works with <i>property</i> to find concepts having one of the specified "
                    + "properties with an exact value matching this parameter.  Using a <i>term</i> "
                    + "will further restrict results to those also matching the term.", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "definitionSource", value = "Comma-separated list of definition sources to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/definitionSources' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>.</p>"
                    + "<p><a href='api/v1/metadata/ncim/definitionSources' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus values</a>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "definitionType", value = "Comma-separated list of definition types to restrict search results by, e.g. "
                    + "'DEFINITION,ALT_DEFINITION' for <i>ncit</i>. "
                    + "<p><a href='api/v1/metadata/ncit/definitionTypes' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>. This parameter is only meaningful for <i>ncit</i>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "synonymSource", value = "Comma-separated list of synonym sources to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/synonymSources' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>.</p> "
                    + "<p><a href='api/v1/metadata/ncim/synonymSources' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus values</a>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "synonymType", value = "Comma-separated list of synonym types to restrict search results by, e.g. "
                    + "'FULL_SYN'. <p><a href='api/v1/metadata/ncit/synonymTypes' target='_blank'>"
                    + "Click here for a list of NCI Thesaurus values</a>. This parameter is only "
                    + "meaningful for <i>ncit</i>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "synonymTermType", value = "Single synonym term type value to restrict search results by. "
                    + "<p><a href='api/v1/metadata/ncit/termTypes' target='_blank'>Click here for a "
                    + "list of NCI Thesaurus values</a>.</p> "
                    + "<p><a href='api/v1/metadata/ncim/termTypes' target='_blank'>Click here for a "
                    + "list of NCI Metathesaurus values</a>.</p>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = ""),
            @ApiImplicitParam(name = "subset", value = "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'."
                    + " This parameter is only meaningful for <i>ncit</i>", required = false, dataTypeClass = String.class, paramType = "query", defaultValue = "")
    })
    @RecordMetric
    @RequestMapping(method = RequestMethod.GET, value = "/concept/export", produces = "application/octet-stream")
    public @ResponseBody ResponseEntity<byte[]> export(@ModelAttribute SearchCriteria searchCriteria,
            BindingResult bindingResult) throws Exception {
        String exportText = String.join("\t", "Code", "Preferred Name", "Synonyms", "Definitions\n");
        ConceptResultList results = this.search(searchCriteria, bindingResult, true);
        exportText += this.exportFormatter(results);
        int totalRecords = results.getTotal();
        if (totalRecords > 10000) {
            for (int i = 10000; i < Math.min(totalRecords, MAX_EXPORT_CONCEPTS); i += 10000) {
                searchCriteria.setFromRecord(i);
                results = this.search(searchCriteria, bindingResult, true);
                exportText += this.exportFormatter(results);
            }
        }
        byte[] exportData = exportText.getBytes(StandardCharsets.UTF_8);
        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(searchCriteria.getTerm() + "." + Clock.systemUTC().instant() + "." + ".tsv").build());
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(exportData.length);
        return ResponseEntity.ok().headers(httpHeaders).body(exportData);

    }

    String exportFormatter(ConceptResultList results) {
        // format all the text in here
        String toJoin = "";
        for (Concept conc : results.getConcepts()) {
            toJoin += conc.getCode() + "\t";
            toJoin += conc.getName() + "\t";

            String synonymString = "";
            if (conc.getSynonyms().size() > 0) {
                synonymString += "\"";
                Set<String> uniqueSynonyms = new HashSet<>(conc.getSynonyms().size());
                conc.getSynonyms().removeIf(p -> !uniqueSynonyms.add(p.getName()));
                for (String name : uniqueSynonyms) {
                    synonymString += name + "\n";
                }
                // remove last newline
                synonymString = synonymString.substring(0, synonymString.length() - 1) + "\"";
            }
            synonymString += "\t";
            toJoin += synonymString;

            String defString = "";
            if (conc.getDefinitions().size() > 0) {
                defString += "\"";
                for (Definition def : conc.getDefinitions()) {
                    defString += def.getSource() + ": " + def.getDefinition() + "\n";
                }
                // remove last newline
                defString = defString.substring(0, defString.length() - 1) + "\"";
            }
            defString += "\n";
            toJoin += defString;
        }
        return toJoin;
    }

}