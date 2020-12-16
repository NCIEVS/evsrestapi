
package gov.nih.nci.evs.api.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import gov.nih.nci.evs.api.aop.RecordMetric;
import gov.nih.nci.evs.api.support.ApplicationVersion;
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
   * @return the evs concept detail
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @ApiOperation(value = "Get the application version information", response = ApplicationVersion.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successfully retrieved the requested information"),
      @ApiResponse(code = 400, message = "Bad request"),
      @ApiResponse(code = 404, message = "Resource not found")
  })
  @RecordMetric
  @RequestMapping(method = RequestMethod.GET, value = "/version", produces = "application/json")
  public @ResponseBody ApplicationVersion getApplicationVersion() throws IOException {
    final ApplicationVersion homePageData = new ApplicationVersion();
    homePageData.setName("NCI EVS Rest API");
    homePageData.setDescription(
        "Endpoints to support searching, metadata, and content retrieval for EVS terminologies. "
            + "To learn more about how to interact with this api, see the  "
            + "<a href='https://github.com/NCIEVS/evsrestapi-client-SDK' "
            + "target='_blank'>Github evsrestapi-client-SDK project</a>.<br/><br/>"
            + "Terms of service:"
            + "<a href=\"https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/ThesaurusTermsofUse.htm\">"
            + "https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/ThesaurusTermsofUse.htm</a>");
    homePageData.setVersion("1.3.0.RELEASE");
    return homePageData;
  }

}