
package gov.nih.nci.evs.api.model;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

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
 * Unit test for {@link ConceptResultList}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ConceptResultListUnitTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log =
      LoggerFactory.getLogger(ConceptResultListUnitTest.class);

  /** The model object to test. */
  private ConceptResultList object;

  /** The s 1. */
  private List<Synonym> s1;

  /** The s 2. */
  private List<Synonym> s2;

  /** The d 1. */
  private List<Definition> d1;

  /** The d 2. */
  private List<Definition> d2;

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
  private List<Map> m1;

  /** The m 2. */
  private List<Map> m2;

  /** The c 1. */
  private List<Concept> c1;

  /** The c 2. */
  private List<Concept> c2;

  /** The sc 1. */
  private SearchCriteria sc1;

  /** The sc 2. */
  private SearchCriteria sc2;

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    object = new ConceptResultList();

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

    final ProxyTester tester6 = new ProxyTester(new Map());
    m1 = new ArrayList<>();
    m1.add((Map) tester6.createObject(1));
    m2 = new ArrayList<>();
    m2.add((Map) tester6.createObject(2));

    final ProxyTester tester7 = new ProxyTester(new SearchCriteria());
    sc1 = (SearchCriteria) tester7.createObject(1);
    sc2 = (SearchCriteria) tester7.createObject(2);

    final ProxyTester tester8 = new ProxyTester(new Concept());
    tester8.proxy("synonyms", 1, s1);
    tester8.proxy("synonyms", 2, s2);
    tester8.proxy("definitions", 1, d1);
    tester8.proxy("definitions", 2, d2);
    tester8.proxy("propertys", 1, p1);
    tester8.proxy("propertys", 2, p2);
    tester8.proxy("associations", 1, a1);
    tester8.proxy("associations", 2, a2);
    tester8.proxy("roles", 1, r1);
    tester8.proxy("roles", 2, r2);
    tester8.proxy("maps", 1, m1);
    tester8.proxy("maps", 2, m2);
    c1 = new ArrayList<>();
    c1.add((Concept) tester8.createObject(1));
    c2 = new ArrayList<>();
    c2.add((Concept) tester8.createObject(1));
    c2.add((Concept) tester8.createObject(2));
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet() throws Exception {
    final GetterSetterTester tester = new GetterSetterTester(object);
    tester.proxy("concepts", 1, c1);
    tester.proxy("concepts", 2, c2);
    tester.proxy(SearchCriteria.class, 1, sc1);
    tester.proxy(SearchCriteria.class, 2, sc2);

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
    tester.include("total");
    tester.include("parameters");
    
    tester.proxy("concepts", 1, c1);
    tester.proxy("concepts", 2, c2);
    tester.proxy(SearchCriteria.class, 1, sc1);
    tester.proxy(SearchCriteria.class, 2, sc2);

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
    tester.proxy("concepts", 1, c1);
    tester.proxy(SearchCriteria.class, 1, sc1);

    assertTrue(tester.testCopyConstructor(ConceptResultList.class));
  }

  /**
   * Test model serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelSerialization() throws Exception {
    final SerializationTester tester = new SerializationTester(object);
    tester.proxy("concepts", 1, c1);
    tester.proxy(SearchCriteria.class, 1, sc1);

    assertTrue(tester.testJsonSerialization());
  }
}
