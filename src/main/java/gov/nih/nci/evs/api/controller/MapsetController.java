
package gov.nih.nci.evs.api.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.MapResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.service.ElasticQueryService;
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

/**
 * Controller for /mapset endpoints.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "Mapset endpoints")
public class MapsetController extends BaseController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(MapsetController.class);

  /** The metadata service. */
  @Autowired
  MetadataService metadataService;

  /** The elasticquery service. */
  @Autowired
  ElasticQueryService esQueryService;

  /** The term utils. */
  @Autowired
  TerminologyUtils termUtils;

  /**
   * Returns the mapsets.
   *
   * @param include the include
   * @return the mapsets
   * @throws Exception the exception
   */
  @Operation(
      summary = "Get all mapsets (no terminology parameter is needed as mapsets connect codes "
          + "in one terminology to another)")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Successfully retrieved the requested information")
  })
  @Parameters({
      @Parameter(name = "include",
          description = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, schema = @Schema(implementation = String.class), example = "minimal")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/mapset", produces = "application/json")
  public @ResponseBody List<Concept> getMapsets(@RequestParam(required = false, name = "include")
  final Optional<String> include) throws Exception {
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
  @Operation(summary = "Get the mapset for the specified code (no terminology parameter is"
      + " needed as mapsets connect codes in one terminology to another)")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Successfully retrieved the requested information"),
      @ApiResponse(responseCode = "404", description = "Resource not found",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RestException.class))),
      @ApiResponse(responseCode = "417", description = "Expectation failed",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
      @Parameter(name = "code", description = "Mapset code", required = true,
          schema = @Schema(implementation = String.class)),
      @Parameter(name = "include",
          description = "Indicator of how much data to return. Comma-separated list of any of the following values: "
              + "minimal, summary, full, associations, children, definitions, disjointWith, inverseAssociations, "
              + "inverseRoles, maps, parents, properties, roles, synonyms. "
              + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK/blob/master/doc/INCLUDE.md' target='_blank'>See here "
              + "for detailed information</a>.",
          required = false, schema = @Schema(implementation = String.class), example = "minimal")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/mapset/{code}",
      produces = "application/json")
  public @ResponseBody Concept getMapsetByCode(@PathVariable(value = "code")
  final String code, @RequestParam(required = false, name = "include")
  final Optional<String> include) throws Exception {
    try {
      final IncludeParam ip = new IncludeParam(include.orElse("minimal"));
      List<Concept> results = esQueryService.getMapset(code, ip);
      if (results.size() > 0) {
        return results.get(0);
      } else {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Mapset not found for code = " + code);
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
   * @param fromRecord the from record
   * @param pageSize the page size
   * @param term the term
   * @param ascending the ascending
   * @param sort the sort
   * @return the mapsets
   * @throws Exception the exception
   */
  @Operation(summary = "Get the maps for the mapset specified by the code (no terminology "
      + "parameter is needed as mapsets connect codes in one terminology to another)")
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Successfully retrieved the requested information"),
      @ApiResponse(responseCode = "404", description = "Resource not found",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RestException.class))),
      @ApiResponse(responseCode = "417", description = "Expectation failed",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
      @Parameter(name = "code", description = "Mapset code", required = true,
          schema = @Schema(implementation = String.class)),
      @Parameter(name = "fromRecord", description = "Start index of the search results",
          required = false, schema = @Schema(implementation = Integer.class), example = "0"),
      @Parameter(name = "pageSize", description = "Max number of results to return",
          required = false, schema = @Schema(implementation = Integer.class), example = "10"),
      @Parameter(name = "sort", description = "The search parameter to sort results by",
          required = false, schema = @Schema(implementation = String.class)),
      @Parameter(name = "ascending",
          description = "Sort ascending (if true) or descending (if false)", required = false,
          schema = @Schema(implementation = Boolean.class))
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/mapset/{code}/maps",
      produces = "application/json")
  public @ResponseBody MapResultList getMapsetMappingsByCode(@PathVariable(value = "code")
  final String code, @RequestParam(required = false, name = "fromRecord")
  final Optional<Integer> fromRecord, @RequestParam(required = false, name = "pageSize")
  final Optional<Integer> pageSize, @RequestParam(required = false, name = "term")
  final Optional<String> term, @RequestParam(required = false, name = "ascending")
  final Optional<Boolean> ascending, @RequestParam(required = false, name = "sort")
  final Optional<String> sort) throws Exception {
    try {
      // default index 0 and page size 10
      final Integer fromRecordParam = fromRecord.orElse(0);
      final Integer pageSizeParam = pageSize.orElse(10);
      final IncludeParam ip = new IncludeParam("maps");
      List<Map> maps = new ArrayList<Map>();
      List<Concept> results = esQueryService.getMapset(code, ip);
      if (results.size() > 0) {
        maps = results.get(0).getMaps();
      } else {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Mapset not found for code = " + code);
      }
      if (term.isPresent()) {
        final String t = term.get().trim().toLowerCase();
        final List<String> words = ConceptUtils.wordind(t);
        // Check single words
        if (words.size() == 1 || ConceptUtils.isCode(t)) {
          maps = maps.stream().filter(m ->
          // Code match
          m.getSourceCode().toLowerCase().contains(t) || m.getTargetCode().toLowerCase().contains(t)
              ||
              // Lowercase word match (starting on a word boundary)
              m.getSourceName().toLowerCase().matches("^" + Pattern.quote(t) + ".*")
              || m.getSourceName().toLowerCase().matches(".*\\b" + Pattern.quote(t) + ".*")
              || m.getTargetName().toLowerCase().matches("^" + Pattern.quote(t) + ".*")
              || m.getTargetName().toLowerCase().matches(".*\\b" + Pattern.quote(t) + ".*"))
              .collect(Collectors.toList());
        }
        // Check multiple words (make sure both are in the source OR both are in the target)
        else if (words.size() > 1) {
          maps = maps.stream().filter(m -> {
            boolean sourceFlag = true;
            boolean targetFlag = true;
            for (final String word : words) {
              if (!(
              // Lowercase word match (starting on a word boundary)
              m.getSourceName().toLowerCase().matches("^" + Pattern.quote(word) + ".*") || m
                  .getSourceName().toLowerCase().matches(".*\\b" + Pattern.quote(word) + ".*"))) {
                sourceFlag = false;
              }
              if (!(
              // Lowercase word match (starting on a word boundary)
              m.getTargetName().toLowerCase().matches("^" + Pattern.quote(word) + ".*") || m
                  .getTargetName().toLowerCase().matches(".*\\b" + Pattern.quote(word) + ".*"))) {
                targetFlag = false;
              }
            }
            return sourceFlag || targetFlag;
          }).collect(Collectors.toList());
        }
      }
      final Integer mapLength = maps.size();
      final MapResultList list = new MapResultList();
      list.setTotal(mapLength);
      if (sort.isPresent()) {
        if (sort.get().equals("sourceName")) {
          maps.sort(Comparator.comparing(Map::getSourceName));

        } else if (sort.get().equals("targetName")) {
          maps.sort(Comparator.comparing(Map::getTargetName));
        } else if (sort.get().equals("sourceCode")) {
          maps.sort(Comparator.comparing(Map::getSourceCode));
        } else if (sort.get().equals("targetCode")) {
          maps.sort(Comparator.comparing(Map::getTargetCode));
        }
        if (ascending.isPresent() && !ascending.get()) {
          Collections.reverse(maps);
        }
        list.setMaps(maps);
      }
      // Get this page if we haven't gone over the end
      if (fromRecordParam < mapLength) {
        // on subList "toIndex" don't go past the end
        list.setMaps(
            maps.subList(fromRecordParam, Math.min(mapLength, fromRecordParam + pageSizeParam)));
      } else {
        list.setTotal(0);
        list.setMaps(new ArrayList<Map>());
      }
      final SearchCriteria criteria = new SearchCriteria();
      criteria.setInclude(null);
      criteria.setType(null);
      if (fromRecord.isPresent()) {
        criteria.setFromRecord(fromRecord.get());
        list.setParameters(criteria);
      }
      if (pageSize.isPresent()) {
        criteria.setPageSize(pageSize.get());
        list.setParameters(criteria);
      }

      return list;
    } catch (Exception e) {
      handleException(e);
      return null;
    }

  }
}
