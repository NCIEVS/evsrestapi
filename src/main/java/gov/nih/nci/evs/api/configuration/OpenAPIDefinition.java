
package gov.nih.nci.evs.api.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Open API Definition.
 */
@OpenAPIDefinition(info = @Info(title = "NCI EVS Rest API", version = "1.7.2.RELEASE",
    termsOfService = "https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/ThesaurusTermsofUse.htm",
    description = "<p><b>Government Funding Lapse</b></p>"
        + "<p>Because of a lapse in government funding, the information on this website may not be up to date, transactions submitted via the website may not be processed, and the agency may not be able to respond to inquiries until appropriations are enacted. \r\n"
        + "The NIH Clinical Center (the research hospital of NIH) is open. For more details about its operating status, please visit "
        + "<a href=\"https://www.cc.nih.gov/\">cc.nih.gov</a>. \r\n"
        + "Updates regarding government operating status and resumption of normal operations can be found at "
        + "<a href=\"https://www.opm.gov/\">OPM.gov</a>.</p>"
        + "Endpoints to support searching, metadata, and content retrieval for EVS terminologies. "
        + "To learn more about how to interact with this api, see the "
        + "<a href=\"https://github.com/NCIEVS/evsrestapi-client-SDK\">Github evsrestapi-client-SDK project.</a>",
    contact = @Contact(name = "NCI EVS", email = "NCIAppSupport@nih.gov")), servers = {
        @Server(url = "/")
})
class OpenAPIConfiguration {
  // n/a
}
