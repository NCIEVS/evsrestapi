package gov.nih.nci.evs.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Automates JUnit proxy object creation. */
public class ProxyTester {

  /** The logger. */
  private final Logger logger = LoggerFactory.getLogger(ProxyTester.class);

  /** Set of fields to exclude. */
  private Set<String> excludes = new TreeSet<String>();

  /** Set of fields to include. */
  private Set<String> includes = new TreeSet<String>();

  /** Class of object under test. */
  private Class<?> clazz;

  /** The proxy map. */
  private Map<Class<?>, Map<Integer, Object>> proxyMap = new HashMap<>();

  /** The field proxy map. */
  private Map<String, Map<Integer, Object>> fieldProxyMap = new HashMap<>();

  /**
   * Constructs a new tester for the specified class.
   *
   * @param obj Object to test.
   */
  public ProxyTester(final Object obj) {
    this.clazz = obj.getClass();
  }

  /**
   * Adds a field to the list of tested fields. If this method is called, the tester will not
   * attempt to list all the getters and setters on the object under test, and will instead simply
   * test all the fields in the include list.
   *
   * @param field Field name whose getter/setter should be tested.
   */
  public void include(final String field) {
    includes.add(field.toLowerCase());
  }

  /**
   * Adds a field to the list of excluded fields.
   *
   * @param field Field name to exclude from testing.
   */
  public void exclude(final String field) {
    excludes.add(field.toLowerCase());
  }

  /**
   * Proxy the specified object for the class type.
   *
   * @param clazz the class to match
   * @param i the i the initializer key
   * @param o the o the object
   */
  public void proxy(final Class<?> clazz, final int i, final Object o) {
    if (!proxyMap.containsKey(clazz)) {
      proxyMap.put(clazz, new HashMap<Integer, Object>());
    }
    final Map<Integer, Object> initializerMap = proxyMap.get(clazz);
    initializerMap.put(i, o);
  }

  /**
   * Proxy.
   *
   * @param field the field
   * @param i the i
   * @param o the o
   */
  public void proxy(final String field, final int i, final Object o) {
    if (!fieldProxyMap.containsKey(field.toLowerCase())) {
      fieldProxyMap.put(field.toLowerCase(), new HashMap<Integer, Object>());
    }
    final Map<Integer, Object> initializerMap = fieldProxyMap.get(field.toLowerCase());
    initializerMap.put(i, o);
  }

  /**
   * Walks through the methods in the class looking for getters and setters that are on our include
   * list (if any) and are not on our exclude list.
   *
   * @param initializer a value that when used produces certain field values
   * @return the object
   * @throws Exception the exception
   */
  public Object createObject(final int initializer) throws Exception {
    // Verify there is a no-argument constructor
    Object o = null;
    try {
      o = clazz.getConstructor().newInstance();
    } catch (final Exception e) {
      throw new Exception(
          "Class " + clazz + " unexpectedly does not have a no-argument constructor");
    }
    setFields(o, false, false, initializer);
    return o;
  }

  /**
   * Sets the fields.
   *
   * @param o the object
   * @param reverseIncludes the reverse includes
   * @param logField the log set
   * @param initializer the initializer
   * @throws Exception the exception
   */
  protected void setFields(
      final Object o, final boolean reverseIncludes, final boolean logField, final int initializer)
      throws Exception {
    final Set<String> fieldsSeen = new HashSet<>();
    final Method[] methods = clazz.getMethods();
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

      // indicate we've seen it
      fieldsSeen.add(fieldName.toLowerCase());

      /* Check the field name against our include/exclude list. */
      if (!includes.isEmpty() && !includes.contains(fieldName.toLowerCase()) && !reverseIncludes) {
        // skip if includes are explicit and none are listed
        continue;
      }

      if (!includes.isEmpty() && includes.contains(fieldName.toLowerCase()) && reverseIncludes) {
        // skip if includes are explicit and none are listed
        continue;
      }

      if (excludes.contains(fieldName.toLowerCase())) {
        // skip excludes always
        continue;
      }

      /* Is there a getter that returns the same type? */
      Method getter;
      try {
        getter = clazz.getMethod("get" + fieldName, new Class[] {});
        if (getter.getReturnType() != args[0]) {
          continue;
        }
      } catch (final NoSuchMethodException e) {
        try {
          getter = clazz.getMethod("is" + fieldName, new Class[] {});
          if (getter.getReturnType() != args[0]) {
            continue;
          }
        } catch (final NoSuchMethodException e2) {
          continue;
        }
      }
      if (logField) {
        logger.debug("  field = " + fieldName);
      }
      setField(o, fieldName, getter, m, args[0], initializer);
    }

    // If includes contains entries and not all have been seen - error
    if (!includes.isEmpty()) {
      final Set<String> notSeen = new HashSet<>();
      for (final String field : includes) {
        if (!fieldsSeen.contains(field.toLowerCase())) {
          notSeen.add(field);
        }
      }
      if (notSeen.size() > 0) {
        throw new Exception("Some included fields were not found: " + notSeen);
      }
    }

    if (!excludes.isEmpty()) {
      final Set<String> notSeen = new HashSet<>();
      for (final String field : excludes) {
        if (!fieldsSeen.contains(field.toLowerCase())) {
          notSeen.add(field);
        }
      }
      if (notSeen.size() > 0) {
        throw new Exception("Some excluded fields were not found: " + notSeen);
      }
    }
  }

  /** Dummy invocation handler for our proxy objects. */
  protected class DummyInvocationHandler implements InvocationHandler {

    /**
     * Invoke.
     *
     * @param o the o
     * @param m the m
     * @param a the a
     * @return object
     */
    @Override
    public Object invoke(final Object o, final Method m, final Object[] a) {
      return null;
    }
  }

  /**
   * Tests a single getter/setter pair using an argument of a particular type.
   *
   * @param o the o
   * @param fieldName the field name
   * @param get the get method
   * @param set the set method
   * @param argType the data type
   * @param initializer the initializer
   * @throws Exception the exception
   */
  protected void setField(
      final Object o,
      final String fieldName,
      final Method get,
      final Method set,
      final Class<?> argType,
      final int initializer)
      throws Exception {
    final Object proxy = makeProxy(fieldName, argType, initializer);

    // logger
    // .info(" " + set.getName() + " = " + proxy.toString());
    try {
      set.invoke(o, new Object[] {proxy});
    } catch (final InvocationTargetException e) {
      e.printStackTrace();
      throw new RuntimeException(
          "Setter "
              + set.getDeclaringClass().getName()
              + "."
              + set.getName()
              + " threw "
              + e.getTargetException().toString());
    } catch (final IllegalArgumentException e) {
      logger.debug("o=" + o.getClass().getName());
      logger.debug("proxy=" + proxy.getClass().getName());
      throw e;
    }
  }

  /**
   * Makes a proxy of a given class. If the class is an interface type, uses the standard JDK proxy
   * mechanism. If it's not, uses cglib. The use of cglib is via reflection so that cglib is not
   * required to use this library unless the caller actually needs to proxy a concrete class.
   *
   * @param fieldName the field name
   * @param type the type
   * @param initializer the initializer
   * @return a class of the specified type
   * @throws Exception the exception
   */
  @SuppressWarnings({"rawtypes", "deprecation"})
  protected Object makeProxy(final String fieldName, final Class<?> type, final int initializer)
      throws Exception {
    // Return field name proxies
    if (fieldProxyMap.containsKey(fieldName.toLowerCase())) {
      return fieldProxyMap.get(fieldName.toLowerCase()).get(initializer);
    }
    // Return anything passed in first
    if (proxyMap.containsKey(type)) {
      return proxyMap.get(type).get(initializer);
    }

    /* If it's a primitive type, just create it. */
    if (type == String.class) {
      return "" + initializer;
    }
    if (type == Date.class) {
      return new Date(10L + initializer);
    }
    if (type == Boolean.class || type == boolean.class) {
      return new Boolean((initializer & 1) == 0);
    }
    if (type == Integer.class || type == int.class) {
      return new Integer(initializer);
    }
    if (type == Long.class || type == long.class) {
      return new Long(initializer);
    }
    if (type == Double.class || type == double.class) {
      return new Double((initializer * 1.0) / 100);
    }
    if (type == Float.class || type == float.class) {
      return new Float((initializer * 1.0) / 100);
    }
    if (type == Character.class || type == char.class) {
      return new Character((char) ('a' + initializer));
    }
    if (type == BigDecimal.class) {
      return new BigDecimal(initializer);
    }
    if (type == Set.class) {
      final Set set = new HashSet();
      return set;
    }
    if (type == List.class) {
      final List list = new ArrayList();
      return list;
    }
    if (type == Map.class) {
      final Map map = new HashMap<>();
      return map;
    }
    if (type == BigInteger.class) {
      return new BigInteger("" + initializer);
    }
    if (type == Class.forName("[B")) {
      return new byte[1];
    }

    // JAVA5 - Comment out or remove the next two lines on older Java versions.
    if (type.isEnum()) {
      return makeEnum(type, initializer);
    }

    /* Use JDK dynamic proxy if the argument is an interface. */
    if (type.isInterface()) {
      return Proxy.newProxyInstance(
          type.getClassLoader(), new Class[] {type}, new DummyInvocationHandler());
    }

    /* Get the CGLib classes we need. */
    Class<?> enhancerClass = null;
    Class<?> callbackClass = null;
    Class<?> fixedValueClass = null;
    try {
      enhancerClass = Class.forName("net.sf.cglib.proxy.Enhancer");
      callbackClass = Class.forName("net.sf.cglib.proxy.Callback");
      fixedValueClass = Class.forName("net.sf.cglib.proxy.FixedValue");
    } catch (final ClassNotFoundException e) {
      throw new ClassNotFoundException(
          "Need cglib to make a dummy "
              + type.getName()
              + ". Make sure cglib.jar is on "
              + "your classpath.");
    }

    /* Make a dummy callback (proxies within proxies!) */
    Object callback;
    callback =
        Proxy.newProxyInstance(
            callbackClass.getClassLoader(),
            new Class[] {fixedValueClass},
            new DummyInvocationHandler());

    final Method createMethod =
        enhancerClass.getMethod("create", new Class[] {Class.class, callbackClass});
    return createMethod.invoke(null, new Object[] {type, callback});
  }

  /**
   * Returns an instance of an enum.
   *
   * <p>JAVA5 - Comment out or remove this method on older Java versions.
   *
   * @param clazz1 the class
   * @param initializer the initializer
   * @return an instance of an enum
   * @throws Exception the exception
   */
  private Object makeEnum(final Class<?> clazz1, final int initializer) throws Exception {
    final Method m = clazz1.getMethod("values", new Class[0]);
    final Object[] o = (Object[]) m.invoke(null, new Object[0]);
    return o[initializer];
  }

  /**
   * Returns the clazz.
   *
   * @return the clazz
   */
  public Class<?> getClazz() {
    return clazz;
  }

  /**
   * Sets the clazz.
   *
   * @param clazz the clazz
   */
  public void setClazz(final Class<?> clazz) {
    this.clazz = clazz;
  }

  /**
   * Returns the excludes.
   *
   * @return the excludes
   */
  public Set<String> getExcludes() {
    return excludes;
  }

  /**
   * Sets the excludes.
   *
   * @param excludes the excludes
   */
  public void setExcludes(final Set<String> excludes) {
    this.excludes = excludes;
  }

  /**
   * Returns the includes.
   *
   * @return the includes
   */
  public Set<String> getIncludes() {
    return includes;
  }

  /**
   * Sets the includes.
   *
   * @param includes the includes
   */
  public void setIncludes(final Set<String> includes) {
    this.includes = includes;
  }
}
