package gov.nih.nci.evs.api.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Jackson Configuration. */
@Configuration
public class JacksonConfiguration implements WebMvcConfigurer {

  /**
   * Object mapper.
   *
   * @return the object mapper
   */
  @Bean
  public ObjectMapper objectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setDefaultPropertyInclusion(Include.NON_EMPTY);
    return mapper;
  }

  /** Prefer Jackson 2 while application and HAPI HTTP types still use Jackson 2 classes. */
  @Override
  @SuppressWarnings("removal")
  public void extendMessageConverters(final List<HttpMessageConverter<?>> converters) {
    converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper()));
  }
}
