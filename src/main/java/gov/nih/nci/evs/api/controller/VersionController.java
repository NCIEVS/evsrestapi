
package gov.nih.nci.evs.api.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.support.ApplicationVersion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Version controller.
 */
@RestController
@Tag(name = "Application version endpoint")
public class VersionController extends BaseController {

  /**
   * Returns the evs concept detail.
   *
   * @return the evs concept detail
   * @throws IOException Signals that an I/O exception has occurred.
   */

  @Operation(summary = "Get the application version information")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved the requested information"),
      @ApiResponse(responseCode = "400", description = "Bad request",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class))),
      @ApiResponse(responseCode = "404", description = "Resource not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = RestException.class)))
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/version", produces = "application/json")
  public @ResponseBody ApplicationVersion getApplicationVersion() throws IOException {
    final ApplicationVersion homePageData = new ApplicationVersion();
    homePageData.setName("NCI EVS Rest API");
    homePageData
        .setDescription("Endpoints to support searching, metadata, and content retrieval for EVS terminologies. "
            + "To learn more about how to interact with this api, see the  "
            + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK' "
            + "target='_blank'>Github evsrestapi-client-SDK project</a>.<br/><br/>");
    homePageData.setVersion("1.9.0.RELEASE");
    return homePageData;
  }

}
