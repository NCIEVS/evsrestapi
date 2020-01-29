
package gov.nih.nci.evs.api.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.aop.RecordMetricDBFormat;
import gov.nih.nci.evs.api.support.HomePageData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Version controller.
 */
@RestController
@Api(tags = "Application version endpoint")
public class VersionController {

  /**
   * Returns the evs concept detail.
   *
   * @param response the response
   * @return the evs concept detail
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Get the application version information", response = String.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 401, message = "Not authorized to view this resource"),
      @ApiResponse(code = 403, message = "Access to resource is forbidden"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetricDBFormat
  @RequestMapping(method = RequestMethod.GET, value = "/version", produces = "application/json")
  public @ResponseBody HomePageData getApplicationVersion(
    HttpServletResponse response) throws IOException {
    HomePageData homePageData = new HomePageData();
    homePageData.setDescription("NCI EVS API");
    homePageData.setVersion("1.1.0-SNAPSHOT");
    return homePageData;
  }

}