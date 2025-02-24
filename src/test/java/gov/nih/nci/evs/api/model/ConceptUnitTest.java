package gov.nih.nci.evs.api.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.CopyConstructorTester;
import gov.nih.nci.evs.api.EqualsHashcodeTester;
import gov.nih.nci.evs.api.GetterSetterTester;
import gov.nih.nci.evs.api.ProxyTester;
import gov.nih.nci.evs.api.SerializationTester;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Unit test for {@link Concept}. */
@ExtendWith(SpringExtension.class)
public class ConceptUnitTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(ConceptUnitTest.class);

  /** The model object to test. */
  private Concept object;

  /** The s 1. */
  private List<Synonym> s1;

  /** The s 2. */
  private List<Synonym> s2;

  /** The d 1. */
  private List<Definition> d1;

  /** The d 2. */
  private List<Definition> d2;

  /** The h 1. */
  private List<History> h1;

  /** The h 2. */
  private List<History> h2;

  /** The p 1. */
  private List<Property> p1;

  /** The p 2. */
  private List<Property> p2;

  /** The a 1. */
  private List<Association> a1;

  /** The a 2. */
  private List<Association> a2;

  /** The r 1. */
  private List<Role> r1;

  /** The r 2. */
  private List<Role> r2;

  /** The m 1. */
  private List<Mapping> m1;

  /** The m 2. */
  private List<Mapping> m2;

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @BeforeEach
  public void setup() throws Exception {
    object = new Concept();

    final ProxyTester tester1 = new ProxyTester(new Synonym());
    s1 = new ArrayList<>();
    s1.add((Synonym) tester1.createObject(1));
    s2 = new ArrayList<>();
    s2.add((Synonym) tester1.createObject(2));

    final ProxyTester tester2 = new ProxyTester(new Definition());
    d1 = new ArrayList<>();
    d1.add((Definition) tester2.createObject(1));
    d2 = new ArrayList<>();
    d2.add((Definition) tester2.createObject(2));

    final ProxyTester tester3 = new ProxyTester(new Property());
    p1 = new ArrayList<>();
    p1.add((Property) tester3.createObject(1));
    p2 = new ArrayList<>();
    p2.add((Property) tester3.createObject(2));

    final ProxyTester tester4 = new ProxyTester(new Association());
    a1 = new ArrayList<>();
    a1.add((Association) tester4.createObject(1));
    a2 = new ArrayList<>();
    a2.add((Association) tester4.createObject(2));

    final ProxyTester tester5 = new ProxyTester(new Role());
    r1 = new ArrayList<>();
    r1.add((Role) tester5.createObject(1));
    r2 = new ArrayList<>();
    r2.add((Role) tester5.createObject(2));

    final ProxyTester tester6 = new ProxyTester(new Mapping());
    m1 = new ArrayList<>();
    m1.add((Mapping) tester6.createObject(1));
    m2 = new ArrayList<>();
    m2.add((Mapping) tester6.createObject(2));

    final ProxyTester tester7 = new ProxyTester(new History());
    h1 = new ArrayList<>();
    h1.add((History) tester7.createObject(1));
    h2 = new ArrayList<>();
    h2.add((History) tester7.createObject(2));
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet() throws Exception {
    final GetterSetterTester tester = new GetterSetterTester(object);
    tester.proxy("synonyms", 1, s1);
    tester.proxy("synonyms", 2, s2);
    tester.proxy("definitions", 1, d1);
    tester.proxy("definitions", 2, d2);
    tester.proxy("history", 1, h1);
    tester.proxy("history", 2, h2);
    tester.proxy("propertys", 1, p1);
    tester.proxy("propertys", 2, p2);
    tester.proxy("associations", 1, a1);
    tester.proxy("associations", 2, a2);
    tester.proxy("roles", 1, r1);
    tester.proxy("roles", 2, r2);
    tester.proxy("maps", 1, m1);
    tester.proxy("maps", 2, m2);
    tester.proxy("extensions", 1, null);
    tester.proxy("extensions", 2, null);
    tester.proxy("paths", 1, null);
    tester.proxy("paths", 2, null);

    tester.test();
  }

  /**
   * Test equals and hashcode methods.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelEqualsHashcode() throws Exception {
    final EqualsHashcodeTester tester = new EqualsHashcodeTester(object);
    tester.include("name");
    tester.include("code");
    tester.include("terminology");
    tester.include("version");
    tester.include("level");

    tester.proxy("synonyms", 1, s1);
    tester.proxy("synonyms", 2, s2);
    tester.proxy("definitions", 1, d1);
    tester.proxy("definitions", 2, d2);
    tester.proxy("history", 1, h1);
    tester.proxy("history", 2, h2);
    tester.proxy("propertys", 1, p1);
    tester.proxy("propertys", 2, p2);
    tester.proxy("associations", 1, a1);
    tester.proxy("associations", 2, a2);
    tester.proxy("roles", 1, r1);
    tester.proxy("roles", 2, r2);
    tester.proxy("maps", 1, m1);
    tester.proxy("maps", 2, m2);
    tester.proxy("extensions", 1, null);
    tester.proxy("extensions", 2, null);
    tester.proxy("paths", 1, null);
    tester.proxy("paths", 2, null);

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

    tester.proxy("synonyms", 1, s1);
    tester.proxy("definitions", 1, d1);
    tester.proxy("history", 1, h1);
    tester.proxy("propertys", 1, p1);
    tester.proxy("associations", 1, a1);
    tester.proxy("roles", 1, r1);
    tester.proxy("maps", 1, m1);
    tester.proxy("extensions", 1, null);
    tester.proxy("paths", 1, null);

    assertTrue(tester.testCopyConstructor(Concept.class));
  }

  /**
   * Test model serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelSerialization() throws Exception {
    final SerializationTester tester = new SerializationTester(object);

    tester.proxy("synonyms", 1, s1);
    tester.proxy("definitions", 1, d1);
    tester.proxy("history", 1, h1);
    tester.proxy("propertys", 1, p1);
    tester.proxy("associations", 1, a1);
    tester.proxy("roles", 1, r1);
    tester.proxy("maps", 1, m1);
    tester.proxy("extensions", 1, null);
    tester.proxy("paths", 1, null);

    assertTrue(tester.testJsonSerialization());
  }
}
