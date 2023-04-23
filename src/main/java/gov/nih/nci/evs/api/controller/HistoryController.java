
package gov.nih.nci.evs.api.controller;

import java.util.List;
import java.util.Map;

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
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.util.HistoryUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Controller for /history endpoints.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "History endpoints")
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
  @ApiOperation(value = "Gets suggested replacements for a specified terminology and retired code",
      response = List.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'", required = true,
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3910' for <i>ncit</i>.  This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/history/{terminology}/{code}/replacements",
      produces = "application/json")
  public @ResponseBody List<String> getReplacements(@PathVariable(value = "terminology")
  final String terminology, @PathVariable(value = "code")
  final String code) throws Exception {

    try {

      final Terminology term = termUtils.getTerminology(terminology, true);
      final List<String> replacementCodes = HistoryUtils.getReplacements(term, elasticQueryService, code);

      return replacementCodes;

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
  @ApiOperation(value = "Gets suggested replacements for a specified terminology and a list of retired codes",
      response = Map.class, responseContainer = "List")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"), @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "terminology", value = "Terminology, e.g. 'ncit' or 'ncim'", required = true,
          dataTypeClass = String.class, paramType = "path", defaultValue = "ncit"),
      @ApiImplicitParam(name = "code",
          value = "Code in the specified terminology, e.g. "
              + "'C3910' for <i>ncit</i>.  This call is only meaningful for <i>ncit</i>.",
          required = true, dataTypeClass = String.class, paramType = "path")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/history/{terminology}/replacements",
      produces = "application/json")
  public @ResponseBody Map<String, List<String>> getReplacements(@PathVariable(value = "terminology")
  final String terminology, @RequestParam(required = true, name = "list")
  final List<String> list) throws Exception {

    try {

      final Terminology term = termUtils.getTerminology(terminology, true);
      final Map<String, List<String>> replacementCodes = HistoryUtils.getReplacements(term, elasticQueryService, list);

      return replacementCodes;

    } catch (final Exception e) {
      handleException(e);
      return null;
    }
  }

}
