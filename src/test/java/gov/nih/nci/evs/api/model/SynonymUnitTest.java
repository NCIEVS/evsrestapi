package gov.nih.nci.evs.api.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.CopyConstructorTester;
import gov.nih.nci.evs.api.EqualsHashcodeTester;
import gov.nih.nci.evs.api.GetterSetterTester;
import gov.nih.nci.evs.api.ProxyTester;
import gov.nih.nci.evs.api.SerializationTester;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Unit test for {@link Synonym}. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class SynonymUnitTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(SynonymUnitTest.class);

  /** The model object to test. */
  private Synonym object;

  /** The q 1. */
  private List<Qualifier> q1;

  /** The q 2. */
  private List<Qualifier> q2;

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @BeforeEach
  public void setup() throws Exception {
    object = new Synonym();
    final ProxyTester tester = new ProxyTester(new Qualifier());
    q1 = new ArrayList<>();
    q1.add((Qualifier) tester.createObject(1));
    q2 = new ArrayList<>();
    q2.add((Qualifier) tester.createObject(2));
    q2.add((Qualifier) tester.createObject(3));
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet() throws Exception {
    final GetterSetterTester tester = new GetterSetterTester(object);
    tester.exclude("termgroup");
    tester.proxy("qualifiers", 1, q1);
    tester.proxy("qualifiers", 2, q2);
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
    tester.proxy("qualifiers", 1, q1);
    tester.proxy("qualifiers", 2, q2);
    tester.include("name");
    tester.include("termType");
    tester.include("type");
    tester.include("source");
    tester.include("code");
    tester.include("active");
    tester.include("subSource");

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
    tester.proxy("qualifiers", 1, q1);
    tester.proxy("qualifiers", 2, q2);
    assertTrue(tester.testCopyConstructor(Synonym.class));
  }

  /**
   * Test model serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelSerialization() throws Exception {
    final SerializationTester tester = new SerializationTester(object);
    tester.proxy("qualifiers", 1, q1);
    assertTrue(tester.testJsonSerialization());
  }
}
