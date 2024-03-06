package gov.nih.nci.evs.api;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Automates JUnit testing of equals and hashcode methods. */
public class EqualsHashcodeTester extends ProxyTester {

  /** The logger. */
  private final Logger logger = LoggerFactory.getLogger(EqualsHashcodeTester.class);

  /**
   * Constructs a new getter/setter tester to test objects of a particular class.
   *
   * @param obj Object to test.
   */
  public EqualsHashcodeTester(final Object obj) {
    super(obj);
  }

  /**
   * Creates two objects with the same field values and verifies they are equal.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testIdentityFieldEquals() throws Exception {
    logger.debug("Test identity field equals - " + getClazz().getName());
    final Object o1 = createObject(1);
    final Object o2 = createObject(1);
    if (o1.equals(o2)) {
      return true;
    } else {
      logger.info("o1 = " + o1.hashCode() + ", " + o1);
      logger.info("o2 = " + o2.hashCode() + ", " + o2);
      return false;
    }
  }

  /**
   * Creates two objects and verifies for each that changing the non-included or excluded fields
   * makes no difference to equality testing.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testNonIdentityFieldEquals() throws Exception {
    logger.debug("Test non identity field equals - " + getClazz().getName());
    final Object o1 = createObject(1);
    final Object o2 = createObject(1);
    setFields(o2, true, true, 2);
    if (o1.equals(o2)) {
      return true;
    } else {
      logger.info("o1 = " + o1.hashCode() + ", " + o1);
      logger.info("o2 = " + o2.hashCode() + ", " + o2);
      return false;
    }
  }

  /**
   * Creates two objects and verifies that any difference in identity fields produces inequality.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testIdentityFieldNotEquals() throws Exception {
    logger.debug("Test identity field not equals - " + getClazz().getName());

    // Create an object
    final Object o1 = createObject(1);

    final Method[] methods = getClazz().getMethods();
    for (int i = 0; i < methods.length; i++) {

      /* We're looking for single-argument setters. */
      final Method m = methods[i];
      if (!m.getName().startsWith("set")) {
        continue;
      }

      final String fieldName = m.getName().substring(3);
      final Class<?>[] args = m.getParameterTypes();
      if (args.length != 1) {
        continue;
      }

      /* Check the field name against our include/exclude list. */
      if (!getIncludes().isEmpty() && !getIncludes().contains(fieldName.toLowerCase())) {
        continue;
      }
      if (getExcludes().contains(fieldName.toLowerCase())) {
        continue;
      }

      /* Is there a getter that returns the same type? */
      Method getter;
      try {
        getter = getClazz().getMethod("get" + fieldName, new Class[] {});
        if (getter.getReturnType() != args[0]) {
          continue;
        }
      } catch (final NoSuchMethodException e) {
        try {
          getter = getClazz().getMethod("is" + fieldName, new Class[] {});
          if (getter.getReturnType() != args[0]) {
            continue;
          }
        } catch (final NoSuchMethodException e2) {
          continue;
        }
      }

      // Create second object each time, so we can compare resetting each field
      // value
      final Object o2 = createObject(1);
      // Change the field (use an initializer of 2).
      setField(o2, fieldName, getter, m, args[0], 2);

      if (o1.equals(o2)) {
        // if equals, fail here
        logger.info("  o1 = " + o1.hashCode() + ", " + o1);
        logger.info("  o2 = " + o2.hashCode() + ", " + o2);
        throw new Exception("Equality did not change when field " + fieldName + " was changed");
      }
    }
    return true;
  }

  /**
   * Creates two objects with the same field values and verifies they have equal hashcodes.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testIdentityFieldHashcode() throws Exception {
    logger.debug("Test identity field hashcode - " + getClazz().getName());
    final Object o1 = createObject(1);
    final Object o2 = createObject(1);
    return o1.hashCode() == o2.hashCode();
  }

  /**
   * Creates two objects and verifies for each that changing the non-included or excluded fields
   * does not affect the hashcode.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testNonIdentityFieldHashcode() throws Exception {
    logger.debug("Test non identity field hashcode - " + getClazz().getName());
    final Object o1 = createObject(1);
    final Object o2 = createObject(1);
    setFields(o2, true, true, 2);
    if (o1.hashCode() != o2.hashCode()) {
      logger.info("o1 = " + o1.hashCode() + ", " + o1);
      logger.info("o2 = " + o2.hashCode() + ", " + o2);
    }
    return o1.hashCode() == o2.hashCode();
  }

  /**
   * Creates two objects and verifies that any difference in identity fields produces different
   * hashcodes.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testIdentityFieldDifferentHashcode() throws Exception {
    logger.debug("Test identity field different hashcode - " + getClazz().getName());

    // Create an object
    final Object o1 = createObject(1);

    final Method[] methods = getClazz().getMethods();
    for (int i = 0; i < methods.length; i++) {

      /* We're looking for single-argument setters. */
      final Method m = methods[i];
      if (!m.getName().startsWith("set")) {
        continue;
      }

      final String fieldName = m.getName().substring(3);
      final Class<?>[] args = m.getParameterTypes();
      if (args.length != 1) {
        continue;
      }

      /* Check the field name against our include/exclude list. */
      if (!getIncludes().isEmpty() && !getIncludes().contains(fieldName.toLowerCase())) {
        continue;
      }
      if (getExcludes().contains(fieldName.toLowerCase())) {
        continue;
      }

      /* Is there a getter that returns the same type? */
      Method getter;
      try {
        getter = getClazz().getMethod("get" + fieldName, new Class[] {});
        if (getter.getReturnType() != args[0]) {
          continue;
        }
      } catch (final NoSuchMethodException e) {
        try {
          getter = getClazz().getMethod("is" + fieldName, new Class[] {});
          if (getter.getReturnType() != args[0]) {
            continue;
          }
        } catch (final NoSuchMethodException e2) {
          continue;
        }
      }

      // Create second object each time, so we can compare resetting each field
      // value
      final Object o2 = createObject(1);
      logger.debug("  field = " + fieldName);

      // Change the field (use an initializer of 2).
      setField(o2, fieldName, getter, m, args[0], 2);

      if (o1.hashCode() == o2.hashCode()) {
        // if equals, fail here
        logger.info("  o1 = " + o1.hashCode() + ", " + o1);
        logger.info("  o2 = " + o2.hashCode() + ", " + o2);
        throw new Exception("Hashcode did not change when field " + fieldName + " was changed");
      }
    }
    return true;
  }
}
