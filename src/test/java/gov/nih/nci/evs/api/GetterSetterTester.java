
package gov.nih.nci.evs.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automates JUnit testing of simple getter/setter methods.
 * 
 * <p>
 * It may be used in exclusive or inclusive mode. In exclusive mode, which is
 * the default, all JavaBeans properties (getter/setter method pairs with
 * matching names) are tested unless they are excluded beforehand. For example:
 * 
 * <pre>
 * MyClass objectToTest = new MyClass();
 * GetterSetterTester gst = new GetterSetterTester(objectToTest);
 * gst.exclude(&quot;complexProperty&quot;);
 * gst.exclude(&quot;anotherProperty&quot;);
 * gst.test();
 * </pre>
 * 
 * The following property types are supported:
 * 
 * <ul>
 * <li>All Java primitive types.
 * <li>Interfaces.
 * <li>All non-final classes if <a href="http://cglib.sourceforge.net">cglib</a>
 * is on your classpath -- this uses cglib even when a no-argument constructor
 * is available because a constructor might have side effects that you wouldn.t
 * want to trigger in a unit test.
 * <li>Java 5 enums.
 * </ul>
 * 
 * <p>
 * Properties whose types are classes declared <code>final</code> are not
 * supported; neither are non-primitive, non-interface properties if you don't
 * have cglib.
 * 
 * <p>
 * Copyright (c) 2005, Steven Grimm.<br>
 * This software may be used for any purpose, commercial or noncommercial, so
 * long as this copyright notice is retained. If you make improvements to the
 * code, you're encouraged (but not required) to send them to me so I can make
 * them available to others. For updates, please check
 * <a href="http://www.plaintivemewling.com/?p=34">here</a>.
 * 
 * @author Steven Grimm, koreth@midwinter.com
 * @version 1.0 (2005/11/08).
 */
public class GetterSetterTester extends ProxyTester {
  /** The logger. */
  private final Logger logger = LoggerFactory.getLogger(GetterSetterTester.class);

  /** Object under test. */
  private Object obj;

  /** If true, output trace information. */
  private boolean verbose = false;

  /**
   * Constructs a new getter/setter tester to test objects of a particular
   * class.
   * 
   * @param obj Object to test.
   */
  public GetterSetterTester(final Object obj) {
    super(obj);
    this.obj = obj;
  }

  /**
   * Sets the verbosity flag.
   * @param verbose the verbose flag
   * @return this
   */
  public GetterSetterTester setVerbose(final boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  /**
   * Walks through the methods in the class looking for getters and setters that
   * are on our include list (if any) and are not on our exclude list.
   *
   * @throws Exception the exception
   */
  public void test() throws Exception {
    final Method[] methods = getClazz().getMethods();
    final Object emptyObj = obj.getClass().getDeclaredConstructor().newInstance();

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
        // Enforce that collection fields don't return null
        if (Collection.class.isAssignableFrom(getter.getReturnType())) {
          final Object value = getter.invoke(emptyObj, new Object[] {});
          if (value == null) {
            logger.error("  " + getter.getName() + " returns null instead of empty collection");
            throw new Exception(getter.getName() + " returns null instead of empty collection");
          }
        }
      } catch (final NoSuchMethodException e) {
        try {
          getter = getClazz().getMethod("is" + fieldName, new Class[] {});
          if (getter.getReturnType() != args[0]) {
            continue;
          }
        } catch (final NoSuchMethodException e2) {
          throw new Exception("Set method does not have corresponding get method: " + m.getName());
        }
      }

      logger.debug("  field = " + fieldName);
      testGetterSetter(fieldName, getter, m, args[0]);
    }
  }

  /**
   * Tests a single getter/setter pair using an argument of a particular type.
   *
   * @param field the field
   * @param get the get method
   * @param set the set method
   * @param argType the data type
   * @throws Exception the exception
   */
  private void testGetterSetter(final String field, final Method get, final Method set,
    final Class<?> argType) throws Exception {
    if (this.verbose) {
      logger.debug("Testing " + get.getDeclaringClass().getName() + "." + get.getName());
    }
    final Object proxy = makeProxy(field, argType, 1);
    try {
      set.invoke(this.obj, new Object[] {
          proxy
      });
    } catch (final InvocationTargetException e) {
      e.printStackTrace();
      throw new RuntimeException("Setter " + set.getDeclaringClass().getName() + "." + set.getName()
          + " threw " + e.getTargetException().toString());
    }

    Object getResult;
    try {
      getResult = get.invoke(this.obj, new Object[] {});
    } catch (final InvocationTargetException e) {
      throw new RuntimeException("Getter " + get.getDeclaringClass().getName() + "." + set.getName()
          + " threw " + e.getTargetException().toString());
    }

    if (getResult == proxy || proxy.equals(getResult)) {
      return;
    }
    throw new RuntimeException("Getter " + get.getName() + " did not return value from setter: "
        + proxy + ", " + getResult);
  }

}
