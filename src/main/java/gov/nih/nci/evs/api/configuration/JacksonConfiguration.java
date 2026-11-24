package gov.nih.nci.evs.api.configuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
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

  /**
   * Preserve Jackson 2 serialization for application MVC endpoints. HAPI uses its own serializers.
   *
   * @param converters the HTTP message converters
   */
  @Override
  @SuppressWarnings("removal")
  public void extendMessageConverters(final List<HttpMessageConverter<?>> converters) {
    // Preserve raw byte responses while keeping Jackson 2's JSON string and DTO serialization.
    int jsonIndex = 0;
    for (int i = 0; i < converters.size(); i++) {
      if (converters.get(i) instanceof ByteArrayHttpMessageConverter) {
        jsonIndex = i + 1;
        break;
      }
    }
    converters.add(jsonIndex, new MappingJackson2HttpMessageConverter(objectMapper()));
  }
}
