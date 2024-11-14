package gov.nih.nci.evs.api;

import java.util.Map;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point for webapp. */
@SpringBootApplication(
    exclude = {
      // This is to avoid "Failed to configure a DataSource: 'url' attribute is not specified and no
      // embedded datasource could be configured" error
      // that arose when FHIR libraries were added
      DataSourceAutoConfiguration.class
    })
@EnableCaching
@EnableScheduling
public class Application extends SpringBootServletInitializer {

  // This is an invocation to allow @Request/ResponseBody to be used on maps
  static {
    SpringDocUtils.getConfig().removeRequestWrapperToIgnore(Map.class);
  }

  /* see superclass */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(Application.class);
  }

  /**
   * Application entry point.
   *
   * @param args the command line arguments
   */
  @SuppressWarnings("resource")
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
