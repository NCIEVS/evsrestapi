
package gov.nih.nci.evs.api.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson Configuration.
 */
@Configuration
public class JacksonConfiguration {

  /**
   * Object mapper.
   *
   * @return the object mapper
   */
  @Bean
  public ObjectMapper objectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_EMPTY);
    return mapper;
  }
}