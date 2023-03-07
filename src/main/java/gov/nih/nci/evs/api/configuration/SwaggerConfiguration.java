
package gov.nih.nci.evs.api.configuration;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gov.nih.nci.evs.api.controller.VersionController;
import gov.nih.nci.evs.api.support.ApplicationVersion;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger configuration.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

  /**
   * Api.
   *
   * @return the docket
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Bean
  public Docket api() throws IOException {
    // See
    // http://springfox.github.io/springfox/docs/current
    return new Docket(DocumentationType.SWAGGER_2)
        // Disable default responses (e.g. 401, 403)
        .useDefaultResponseMessages(false).select().apis(RequestHandlerSelectors.any())
        // .paths(Predicates.or(PathSelectors.ant("/api/v1/**"),
        // PathSelectors.ant("/version/**")))
        .paths(PathSelectors.regex("/(api/v1/.*|version)")).build().apiInfo(apiInfo());

  }

  /**
   * Api info.
   *
   * @return the api info
   * @throws IOException Signals that an I/O exception has occurred.
   */
  ApiInfo apiInfo() throws IOException {
    final ApplicationVersion data = new VersionController().getApplicationVersion();
    return new ApiInfoBuilder().title(data.getName()).description(data.getDescription()).license("").licenseUrl("")
        .termsOfServiceUrl("https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/ThesaurusTermsofUse.htm")
        .version(data.getVersion()).contact(new Contact("", "", "")).build();
  }

}
