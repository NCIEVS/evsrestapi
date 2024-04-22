package gov.nih.nci.evs.api.model;

import static org.junit.Assert.assertTrue;

import gov.nih.nci.evs.api.CopyConstructorTester;
import gov.nih.nci.evs.api.EqualsHashcodeTester;
import gov.nih.nci.evs.api.GetterSetterTester;
import gov.nih.nci.evs.api.SerializationTester;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/** Unit test for {@link TerminologyMetadata}. */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class TerminologyMetadataUnitTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(TerminologyMetadataUnitTest.class);

  /** The model object to test. */
  private TerminologyMetadata object;

  /** The m 1. */
  private Map<String, String> m1;

  /** The m 2. */
  private Map<String, String> m2;

  /** The s 1. */
  private Set<String> s1;

  /** The s 2. */
  private Set<String> s2;

  /** The l 1. */
  private List<String> l1;

  /** The s 2. */
  private List<String> l2;

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    object = new TerminologyMetadata();
    m1 = new HashMap<>();
    m1.put("1", "1");
    m2 = new HashMap<>();
    m2.put("2", null);
    s1 = new HashSet<>();
    s1.add("1");
    s2 = new HashSet<>();
    s2.add(null);
    l1 = new ArrayList<>();
    l1.add("1");
    l2 = new ArrayList<>();
    l2.add(null);
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet() throws Exception {
    final GetterSetterTester tester = new GetterSetterTester(object);
    tester.exclude("synonymTermGroup");
    tester.exclude("mapTargetTermGroup");
    tester.exclude("termGroups");
    tester.exclude("preferredTermGroups");
    tester.proxy(Map.class, 1, m1);
    tester.proxy(Map.class, 2, m2);
    tester.proxy("sourcesToRemove", 1, s1);
    tester.proxy("sourcesToRemove", 2, s2);
    tester.proxy("preferredTermTypes", 1, l1);
    tester.proxy("preferredTermTypes", 2, l2);

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
    tester.include("uiLabel");

    tester.proxy(Map.class, 1, m1);
    tester.proxy(Map.class, 2, m2);
    tester.proxy(Set.class, 1, s1);
    tester.proxy(Set.class, 2, s2);
    tester.proxy("preferredTermTypes", 1, l1);
    tester.proxy("preferredTermTypes", 2, l2);

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
    tester.proxy(Set.class, 1, s1);
    tester.proxy("preferredTermTypes", 1, l1);
    assertTrue(tester.testCopyConstructor(TerminologyMetadata.class));
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
    tester.proxy(Set.class, 1, s1);
    tester.proxy("preferredTermTypes", 1, l1);
    assertTrue(tester.testJsonSerialization());
  }
}
