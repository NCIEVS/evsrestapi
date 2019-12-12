
package gov.nih.nci.evs.api.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.aop.RecordMetricDB;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.util.EVSUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Controller for /metadata endpoints.
 */
@RestController
@RequestMapping("${nci.evs.application.contextPath}")
@Api(tags = "Metadata endpoints")
public class MetadataController {

  /** The Constant log. */
  @SuppressWarnings("unused")
  private static final Logger logger =
      LoggerFactory.getLogger(MetadataController.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  /**
   * Returns the version info.
   *
   * @return the version info
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Gets terminologies loaded into the API", response = EvsVersionInfo.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "db", value = "Specify either 'monthly' or 'weekly', if not specified defaults to 'monthly'", required = false, dataType = "string", paramType = "query")
  })
  @RecordMetricDB
  @RequestMapping(method = RequestMethod.GET, value = "/metadata/terminologies", produces = "application/json")
  public @ResponseBody List<Terminology> getVersionInfo() throws IOException {

    final Terminology monthlyNcit =
        new Terminology(sparqlQueryManagerService.getEvsVersionInfo("monthly"));
    monthlyNcit.getTags().put("monthly", "true");

    final Terminology weeklyNcit =
        new Terminology(sparqlQueryManagerService.getEvsVersionInfo("weekly"));

    if (monthlyNcit.equals(weeklyNcit)) {
      monthlyNcit.setLatest(true);
      return EVSUtils.asList(monthlyNcit);
    } else {
      weeklyNcit.getTags().put("weekly", "true");
      weeklyNcit.setLatest(true);
      return EVSUtils.asList(monthlyNcit, weeklyNcit);
    }
  }

}
