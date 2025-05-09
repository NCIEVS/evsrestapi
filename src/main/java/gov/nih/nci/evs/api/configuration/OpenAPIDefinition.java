package gov.nih.nci.evs.api.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

/** Open API Definition. */
@OpenAPIDefinition(
    info =
        @Info(
            title = "NCI EVS Rest API",
            version = "2.2.0.RELEASE",
            termsOfService = "https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/ThesaurusTermsofUse.htm",
            description =
                "Endpoints to support searching, metadata, and content retrieval for EVS"
                    + " terminologies. To learn more about how to interact with this api, see the"
                    + " <a href=\"https://github.com/NCIEVS/evsrestapi-client-SDK\">Github"
                    + " evsrestapi-client-SDK project.</a>",
            contact = @Contact(name = "NCI EVS", email = "NCIAppSupport@nih.gov")),
    servers = {@Server(url = "/")})
class OpenAPIConfiguration {
  // n/a
}
