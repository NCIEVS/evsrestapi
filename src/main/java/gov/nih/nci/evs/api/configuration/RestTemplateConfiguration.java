package gov.nih.nci.evs.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/** Rest template configuration. */
@Configuration
public class RestTemplateConfiguration {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfiguration.class);

  // See ElasticConfiguration instead
  // @Bean
  // public RestTemplate restTemplate(RestTemplateBuilder builder) {
  // logger.info(" XXX is this called?");
  // // Do any additional configuration here
  // return builder.build();
  // }
}
