package gov.nih.nci.evs.api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utilities for handling the "include" flag, and converting EVSConcept to Concept. */
public final class JsonUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

  /** Instantiates a new json utils. */
  private JsonUtils() {
    // n/a
  }

  /**
   * Pretty print.
   *
   * @param json the json
   * @return the string
   * @throws Exception the exception
   */
  public static String prettyPrint(final String json) throws Exception {
    final ObjectMapper mapper = ThreadLocalMapper.get();
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(json));
  }

  /**
   * Pretty print.
   *
   * @param object the object
   * @return the string
   * @throws Exception the exception
   */
  public static String prettyPrint(final Object object) throws Exception {
    final ObjectMapper mapper = ThreadLocalMapper.get();
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
  }
}
