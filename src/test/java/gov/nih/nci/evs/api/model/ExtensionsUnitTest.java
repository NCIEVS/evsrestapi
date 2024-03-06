package gov.nih.nci.evs.api.model;

import static org.junit.Assert.assertTrue;

import gov.nih.nci.evs.api.CopyConstructorTester;
import gov.nih.nci.evs.api.EqualsHashcodeTester;
import gov.nih.nci.evs.api.GetterSetterTester;
import gov.nih.nci.evs.api.ProxyTester;
import gov.nih.nci.evs.api.SerializationTester;
import gov.nih.nci.evs.api.configuration.TestConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/** Unit test for {@link Concept}. */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ExtensionsUnitTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(ExtensionsUnitTest.class);

  /** The model object to test. */
  private Extensions object;

  /** The lp 1. */
  private List<Paths> lp1;

  /** The lp 2. */
  private List<Paths> lp2;

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    object = new Extensions();
    final ProxyTester tester = new ProxyTester(new Path());
    final Path p1 = (Path) tester.createObject(1);
    p1.getConcepts().add(new ConceptMinimal("1"));
    final Path p2 = (Path) tester.createObject(2);
    p2.getConcepts().add(new ConceptMinimal("2"));
    final Path p3 = (Path) tester.createObject(3);
    p3.getConcepts().add(new ConceptMinimal("3"));
    lp1 = new ArrayList<>();
    Paths ps1 = new Paths();
    ps1.getPaths().add(p1);
    lp1.add(ps1);
    lp2 = new ArrayList<>();
    Paths ps2 = new Paths();
    ps2.getPaths().add(p2);
    lp2.add(ps2);
    Paths ps3 = new Paths();
    ps3.getPaths().add(p3);
    lp2.add(ps3);
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet() throws Exception {
    final GetterSetterTester tester = new GetterSetterTester(object);
    tester.proxy("mainMenuAncestors", 1, lp1);
    tester.proxy("mainMenuAncestors", 2, lp2);
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
    tester.include("isDisease");
    tester.include("isDiseaseStage");
    tester.include("isDiseaseGrade");
    tester.include("isMaintype");
    tester.include("isSubtype");
    tester.include("isBiomarker");
    tester.include("isReferenceGene");
    tester.include("mainMenuAncestors");
    tester.proxy("mainMenuAncestors", 1, lp1);
    tester.proxy("mainMenuAncestors", 2, lp2);

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
    tester.proxy("mainMenuAncestors", 1, lp1);
    tester.proxy("mainMenuAncestors", 2, lp2);
    assertTrue(tester.testCopyConstructor(Extensions.class));
  }

  /**
   * Test model serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelSerialization() throws Exception {
    final SerializationTester tester = new SerializationTester(object);
    tester.proxy("mainMenuAncestors", 1, lp1);
    assertTrue(tester.testJsonSerialization());
  }
}
