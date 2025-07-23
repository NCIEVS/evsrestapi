package gov.nih.nci.evs.api.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.TimeZone;

/** Supply object mapper per thread. */
public class ThreadLocalMapper {

  /** The Constant mapper. */
  private static final ThreadLocal<ObjectMapper> mapper =
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
    return mapper;
  }

  /**
   * Gets the.
   *
   * @return the object mapper
   */
  public static ObjectMapper get() {
    return mapper.get();
  }
}
