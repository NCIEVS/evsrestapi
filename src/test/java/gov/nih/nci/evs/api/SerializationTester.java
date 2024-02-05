package gov.nih.nci.evs.api;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Automates JUnit testing XML/JSON Serialization. */
public class SerializationTester extends ProxyTester {

  /** The logger. */
  private final Logger logger = LoggerFactory.getLogger(SerializationTester.class);

  /**
   * Constructs a new getter/setter tester to test objects of a particular class.
   *
   * @param obj Object to test.
   */
  public SerializationTester(final Object obj) {
    super(obj);
  }

  /**
   * Tests XML and JSON serialization for equality,.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testJsonSerialization() throws Exception {
    logger.debug("Test json serialization - " + getClazz().getName());
    final Object obj = createObject(1);
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_EMPTY);
    logger.debug(" " + obj);
    final String json = mapper.writeValueAsString(obj);
    logger.info("json = " + json);
    final Object obj3 = mapper.readValue(json, obj.getClass());

    logger.debug(" " + obj3);

    // If obj has an "id" field, compare the ids
    try {
      final Method method = obj.getClass().getMethod("getId", new Class<?>[] {});
      if (method != null && method.getReturnType() == Long.class) {

        final Long id1 = (Long) method.invoke(obj, new Object[] {});
        final Long id3 = (Long) method.invoke(obj3, new Object[] {});
        if (!id1.equals(id3)) {
          logger.debug("  id fields do not match " + id1 + ", " + id3);
          return false;
        }
      }
    } catch (final NoSuchMethodException e) {
      // this is OK
    }
    if (obj.equals(obj3)) {
      return true;
    } else {
      logger.info("obj = " + obj);
      logger.debug("json = " + json);
      logger.info("obj3 = " + obj3);
      return false;
    }
  }
}
