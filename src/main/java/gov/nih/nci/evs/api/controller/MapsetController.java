package gov.nih.nci.evs.api.controller;

import static gov.nih.nci.evs.api.service.ElasticSearchServiceImpl.escape;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.MappingResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Controller for /mapset endpoints. */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "Mapset endpoints")
public class MapsetController extends BaseController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(MapsetController.class);

  /** The metadata service. */
  @Autowired MetadataService metadataService;

  /** The elasticquery service. */
  @Autowired ElasticQueryService esQueryService;

  /** The elasticsearch service. */
  @Autowired ElasticSearchService esSearchService;

  /** The term utils. */
  @Autowired TerminologyUtils termUtils;

  /**
   * Returns the mapsets.
   *
   * @param include the include
   * @return the mapsets
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get all mapsets (no terminology parameter is needed as mapsets connect codes "
              + "in one terminology to another)")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information")
  })
  @Parameters({
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal")
  })
  @RecordMetric
  @GetMapping(value = "/mapset", produces = "application/json")
  public @ResponseBody List<Concept> getMapsets(
      @RequestParam(required = false, name = "include") final Optional<String> include)
      throws Exception {
    try {
      final IncludeParam ip = new IncludeParam(include.orElse("minimal"));
      return esQueryService.getMapsets(ip);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the mapsets.
   *
   * @param code the code
   * @param include the include
   * @return the mapsets
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get the mapset for the specified code (no terminology parameter is"
              + " needed as mapsets connect codes in one terminology to another)")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
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
    @Parameter(
        name = "code",
        description = "Mapset code",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "include",
        description =
            "Indicator of how much data to return. Comma-separated list of any of the following"
                + " values: minimal, summary, full, associations, children, definitions,"
                + " disjointWith, inverseAssociations, inverseRoles, maps, parents, properties,"
                + " roles, synonyms. <a"
                + " href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md'"
                + " target='_blank'>See here for detailed information</a>.",
        required = false,
        schema = @Schema(implementation = String.class),
        example = "minimal")
  })
  @RecordMetric
  @GetMapping(value = "/mapset/{code}", produces = "application/json")
  public @ResponseBody Concept getMapsetByCode(
      @PathVariable(value = "code") final String code,
      @RequestParam(required = false, name = "include") final Optional<String> include)
      throws Exception {
    try {
      final IncludeParam ip = new IncludeParam(include.orElse("minimal"));
      List<Concept> results = esQueryService.getMapset(code, ip);
      if (results.size() > 0) {
        return results.get(0);
      } else {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Mapset not found for code = " + code);
      }
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns the mapset maps.
   *
   * @param code the code
   * @param searchCriteria the search criteria
   * @return the mapsets
   * @throws Exception the exception
   */
  @Operation(
      summary =
          "Get the maps for the mapset specified by the code (no terminology "
              + "parameter is needed as mapsets connect codes in one terminology to another)")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved the requested information"),
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
        name = "code",
        description = "Mapset code",
        required = true,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "term",
        description = "The term, phrase, or code to be searched, e.g. 'melanoma'",
        required = false,
        schema = @Schema(implementation = String.class)),
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
        name = "sort",
        description = "The search parameter to sort results by",
        required = false,
        schema = @Schema(implementation = String.class)),
    @Parameter(
        name = "ascending",
        description = "Sort ascending (if true) or descending (if false)",
        required = false,
        schema = @Schema(implementation = Boolean.class))
  })
  @RecordMetric
  @GetMapping(value = "/mapset/{code}/maps", produces = "application/json")
  public @ResponseBody MappingResultList getMapsetMappingsByCode(
      @PathVariable(value = "code") final String code, SearchCriteria searchCriteria)
      throws Exception {
    try {
      // default index 0 and page size 10
      // Check search criteria for required fields

      if (!searchCriteria.checkRequiredFields()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Missing required field = " + searchCriteria.computeMissingRequiredFields());
      }

      searchCriteria.checkPagination();
      // pre-process certain sorts
      if ("targetName".equals(searchCriteria.getSort())
          || "sourceName".equals(searchCriteria.getSort())) {
        searchCriteria.setSort(searchCriteria.getSort() + ".keyword");
      }
      logger.debug("  Search = " + searchCriteria);

      // Build the query for finding concept mappings
      String query = buildQueryString(code, searchCriteria.getTerm());

      logger.debug("  Query = " + query);

      return esSearchService.findConceptMappings(query, searchCriteria);
    } catch (Exception e) {
      handleException(e);
      return null;
    }
  }

  private String buildQueryString(String code, String term) throws Exception {
    if (code == null) {
      throw new Exception("Code is required");
    }
    List<String> termClauses = new ArrayList<>();
    List<String> queryClauses = new ArrayList<>();
    queryClauses.add("mapsetCode:\"" + escape(code) + "\"");

    // Mapset searches on sourceName, sourceCode, targetName, targetCode, so we set the term to each
    // of these values
    if (term != null) {
      termClauses.add("sourceName:\"" + escape(term) + "\"");
      termClauses.add("sourceCode:\"" + escape(term) + "\"");
      termClauses.add("targetName:\"" + escape(term) + "\"");
      termClauses.add("targetCode:\"" + escape(term) + "\"");
      queryClauses.add(ConceptUtils.composeQuery("OR", termClauses));
    }
    return ConceptUtils.composeQuery("AND", queryClauses);
  }
}
