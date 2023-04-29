
package gov.nih.nci.evs.api.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.util.HistoryUtils;
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
 * Controller for /history endpoints.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Tag(name = "History endpoints")
public class HistoryController extends BaseController {

  /** Logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);

  /** The elastic query service. */
  @Autowired
  ElasticQueryService elasticQueryService;

  /** The term utils. */
  @Autowired
  TerminologyUtils termUtils;

  /**
   * Returns suggested replacements for a retired concept.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the replacement codes
   * @throws Exception the exception
   */
  @Operation(summary = "Gets suggested replacements for a specified terminology and retired code")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved the requested information"),
      @ApiResponse(responseCode = "400", description = "Bad request",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class))),
      @ApiResponse(responseCode = "417", description = "Expectation failed",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class)))
  })
  @Parameters({
      @Parameter(name = "terminology", description = "Terminology, e.g. 'ncit' or 'ncim'", required = true,
          schema = @Schema(implementation = String.class), example = "ncit"),
      @Parameter(name = "code",
          description = "Code in the specified terminology, e.g. "
              + "'C3910' for <i>ncit</i>.  This call is only meaningful for <i>ncit</i>.",
          required = true, schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/history/{terminology}/{code}/replacements",
      produces = "application/json")
  public @ResponseBody List<History> getReplacements(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {

    try {

      final Terminology term = termUtils.getTerminology(terminology, true);
      return HistoryUtils.getReplacements(term, elasticQueryService, code);

    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

  /**
   * Returns suggested replacements for retired concepts.
   *
   * @param terminology the terminology
   * @param list the list of codes
   * @return the replacement codes
   * @throws Exception the exception
   */
  @Operation(summary = "Gets suggested replacements for a specified terminology and a list of retired codes")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved the requested information"),
      @ApiResponse(responseCode = "400", description = "Bad request",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class))),
      @ApiResponse(responseCode = "417", description = "Expectation failed",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class)))

  })
  @Parameters({
      @Parameter(name = "terminology", description = "Terminology, e.g. 'ncit' or 'ncim'", required = true,
          schema = @Schema(implementation = String.class), example = "ncit"),
      @Parameter(name = "code",
          description = "Code in the specified terminology, e.g. "
              + "'C3910' for <i>ncit</i>.  This call is only meaningful for <i>ncit</i>.",
          required = true, schema = @Schema(implementation = String.class))
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/history/{terminology}/replacements",
      produces = "application/json")
  public @ResponseBody List<History> getReplacements(@PathVariable(value = "terminology")
  final String terminology, @RequestParam(required = true, name = "list")
  final List<String> list) throws Exception {

    try {

      final Terminology term = termUtils.getTerminology(terminology, true);
      return
          HistoryUtils.getReplacements(term, elasticQueryService, list);

    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

}
