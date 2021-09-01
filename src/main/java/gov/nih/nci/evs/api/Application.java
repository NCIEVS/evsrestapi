
package gov.nih.nci.evs.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for webapp.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class Application extends SpringBootServletInitializer {

  /**
   * Configure.
   *
   * @param application the application
   * @return the spring application builder
   */
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(Application.class);
  }

  /**
   * Application entry point.
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
