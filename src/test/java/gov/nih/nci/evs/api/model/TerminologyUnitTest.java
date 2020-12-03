
package gov.nih.nci.evs.api.model;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import gov.nih.nci.evs.api.CopyConstructorTester;
import gov.nih.nci.evs.api.EqualsHashcodeTester;
import gov.nih.nci.evs.api.GetterSetterTester;
import gov.nih.nci.evs.api.ProxyTester;
import gov.nih.nci.evs.api.SerializationTester;
import gov.nih.nci.evs.api.configuration.TestConfiguration;

/**
 * Unit test for {@link Terminology}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class TerminologyUnitTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(TerminologyUnitTest.class);

  /** The model object to test. */
  private Terminology object;

  /** The m 1. */
  private Map<String, String> m1;

  /** The m 2. */
  private Map<String, String> m2;

  /** The tm 1. */
  private TerminologyMetadata tm1;

  /** The tm 2. */
  private TerminologyMetadata tm2;

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    object = new Terminology();
    m1 = new HashMap<>();
    m1.put("1", "1");
    m2 = new HashMap<>();
    m2.put("2", "2");
    m2.put("3", "3");

    final ProxyTester tester = new ProxyTester(new TerminologyMetadata());
    tm1 = (TerminologyMetadata) tester.createObject(1);
    tm2 = (TerminologyMetadata) tester.createObject(2);
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet() throws Exception {
    final GetterSetterTester tester = new GetterSetterTester(object);
    tester.proxy(Map.class, 1, m1);
    tester.proxy(Map.class, 2, m2);
    tester.proxy(TerminologyMetadata.class, 1, tm1);
    tester.proxy(TerminologyMetadata.class, 2, tm2);

    tester.test();
  }

  /**
   * Test equals and hascode methods.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelEqualsHashcode() throws Exception {
    final EqualsHashcodeTester tester = new EqualsHashcodeTester(object);
    tester.include("terminology");
    tester.include("version");
    tester.include("date");
    tester.include("name");
    tester.include("description");
    tester.include("graph");
    tester.include("source");
    tester.include("terminologyVersion");
    tester.include("latest");
    tester.include("indexName");
    tester.include("objectIndexName");
    tester.include("sparqlFlag");

    tester.proxy(Map.class, 1, m1);
    tester.proxy(Map.class, 2, m2);
    tester.proxy(TerminologyMetadata.class, 1, tm1);
    tester.proxy(TerminologyMetadata.class, 2, tm2);

    assertTrue(tester.testIdentityFieldEquals());
    assertTrue(tester.testNonIdentityFieldEquals());
    assertTrue(tester.testIdentityFieldNotEquals());
    assertTrue(tester.testIdentityFieldHashcode());
    assertTrue(tester.testNonIdentityFieldHashcode());
    assertTrue(tester.testIdentityFieldDifferentHashcode());
  }

  /**
   * Test model copy.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelCopy() throws Exception {
    final CopyConstructorTester tester = new CopyConstructorTester(object);
    tester.proxy(Map.class, 1, m1);
    tester.proxy(TerminologyMetadata.class, 1, tm1);
    assertTrue(tester.testCopyConstructor(Terminology.class));
  }

  /**
   * Test model serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelSerialization() throws Exception {
    final SerializationTester tester = new SerializationTester(object);
    tester.proxy(Map.class, 1, m1);
    tester.proxy(TerminologyMetadata.class, 1, tm1);
    assertTrue(tester.testJsonSerialization());
  }
}
