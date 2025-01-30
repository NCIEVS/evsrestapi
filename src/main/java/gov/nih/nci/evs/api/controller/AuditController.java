package gov.nih.nci.evs.api.controller;

import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.service.ElasticQueryServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Controller for /audit endpoints. */
@RestController
@RequestMapping("${nci.evs.application.contextPath}/audit")
@Tag(name = "Audit endpoints")
public class AuditController {

  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

  /** The elastic query service. */
  @Autowired private ElasticQueryServiceImpl elasticQueryService;

  /**
   * Returns all audit records.
   *
   * @return the list of audit records
   * @throws Exception if retrieval fails
   */
  @Operation(summary = "Gets all audit records.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit records"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  @GetMapping(produces = "application/json")
  public @ResponseBody List<Audit> getAllAudits(
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(defaultValue = "false", name = "ascending", required = false) Boolean ascending)
      throws Exception {
    SearchCriteria searchCriteria = new SearchCriteria();
    if (sort != null) {
      searchCriteria.setSort(sort);
      if (ascending != null) {
        searchCriteria.setAscending(ascending);
      }
    }
    return elasticQueryService.getAllAudits(searchCriteria);
  }

  /**
   * Returns audit records filtered by terminology.
   *
   * @param terminology the terminology to filter by
   * @return the list of audit records matching the terminology
   * @throws Exception if retrieval fails
   */
  @Operation(summary = "Gets audit records filtered by terminology.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit records"),
    @ApiResponse(responseCode = "400", description = "Bad request"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  @GetMapping(value = "/terminology/{terminology}", produces = "application/json")
  public @ResponseBody List<Audit> getAuditsByTerminology(
      @PathVariable("terminology") @Parameter(description = "Terminology to filter by")
          String terminology)
      throws Exception {
    return elasticQueryService.getAuditsByTerminology(terminology);
  }

  /**
   * Returns audit records filtered by type
   *
   * @param type the type to filter by
   * @return the list of audit records matching the type
   * @throws Exception if retrieval fails
   */
  @Operation(summary = "Gets audit records filtered by type.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit records"),
    @ApiResponse(responseCode = "400", description = "Bad request"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  @GetMapping(value = "/type/{type}", produces = "application/json")
  public @ResponseBody List<Audit> getAuditsByType(
      @PathVariable("type") @Parameter(description = "Type to filter by") String type)
      throws Exception {
    return elasticQueryService.getAuditsByType(type);
  }
}
