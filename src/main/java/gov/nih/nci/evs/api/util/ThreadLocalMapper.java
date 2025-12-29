package gov.nih.nci.evs.api.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.TimeZone;

/** Supply object mapper per thread. */
public final class ThreadLocalMapper {

  /** Instantiates a new thread local mapper. */
  private ThreadLocalMapper() {
    // n/a
  }

  /** The Constant mapper. */
  private static final ThreadLocal<ObjectMapper> MAPPER =
      ThreadLocal.withInitial(ThreadLocalMapper::newMapper);

  /**
   * New mapper.
   *
   * @return the object mapper
   */
  public static ObjectMapper newMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_EMPTY);
    mapper
        .findAndRegisterModules()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.setTimeZone(TimeZone.getDefault());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  /**
   * Gets the.
   *
   * @return the object mapper
   */
  public static ObjectMapper get() {
    return MAPPER.get();
  }
}
