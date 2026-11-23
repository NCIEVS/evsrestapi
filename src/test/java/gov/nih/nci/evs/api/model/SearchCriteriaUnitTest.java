package gov.nih.nci.evs.api.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.CopyConstructorTester;
import gov.nih.nci.evs.api.GetterSetterTester;
import gov.nih.nci.evs.api.SerializationTester;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

/** Unit test for {@link SearchCriteria}, including hierarchy constraint parameters. */
@ExtendWith(SpringExtension.class)
public class SearchCriteriaUnitTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(SearchCriteriaUnitTest.class);

  /** The model object to test. */
  private SearchCriteria object;

  /** The l 1. */
  private List<String> l1;

  /** The l 2. */
  private List<String> l2;

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @BeforeEach
  public void setup() throws Exception {
    object = new SearchCriteria();
    l1 = new ArrayList<>(Arrays.asList("C3262"));
    l2 = new ArrayList<>(Arrays.asList("C4872", "C3224"));
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet() throws Exception {
    final GetterSetterTester tester = new GetterSetterTester(object);
    proxyLists(tester);
    tester.test();
  }

  /**
   * Test that the hierarchy fields participate in equals and hashCode.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHierarchyFieldsEqualsHashcode() throws Exception {
    final SearchCriteria a = hierarchyCriteria();
    final SearchCriteria b = hierarchyCriteria();
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());

    b.setDescendantOf(new ArrayList<>(l2));
    assertNotEquals(a, b);
    assertNotEquals(a.hashCode(), b.hashCode());

    final SearchCriteria c = hierarchyCriteria();
    c.setNotDescendantOf(new ArrayList<>(l1));
    assertNotEquals(a, c);

    final SearchCriteria d = hierarchyCriteria();
    d.setAncestorOf("C3224");
    assertNotEquals(a, d);

    final SearchCriteria e = hierarchyCriteria();
    e.setMaxLevel(3);
    assertNotEquals(a, e);

    final SearchCriteria f = hierarchyCriteria();
    f.setIncludeSelf(true);
    assertNotEquals(a, f);
  }

  /**
   * Test model copy.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelCopy() throws Exception {
    final CopyConstructorTester tester = new CopyConstructorTester(object);
    proxyLists(tester);
    // uri and ct (from BaseModel) have never been copied by populateFrom; not relevant to criteria
    tester.exclude("uri");
    tester.exclude("ct");
    assertTrue(tester.testCopyConstructor(SearchCriteria.class));

    // Explicitly confirm hierarchy fields survive both copy paths
    final SearchCriteria source = hierarchyCriteria();
    final SearchCriteria copy = new SearchCriteria(source);
    assertEquals(source, copy);
    assertEquals(l1, copy.getDescendantOf());
    assertEquals(2, copy.getMaxLevel());
    assertFalse(copy.getIncludeSelf());
    final SearchCriteria fromWithout = new SearchCriteria(hierarchyCriteria(), "ncit");
    assertEquals(l1, fromWithout.getDescendantOf());
    assertEquals(2, fromWithout.getMaxLevel());
    assertFalse(fromWithout.getIncludeSelf());
  }

  /**
   * Test model serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelSerialization() throws Exception {
    final SerializationTester tester = new SerializationTester(object);
    proxyLists(tester);
    assertTrue(tester.testJsonSerialization());
  }

  /**
   * Test hasHierarchyConstraints.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasHierarchyConstraints() throws Exception {
    assertFalse(new SearchCriteria().hasHierarchyConstraints());

    final SearchCriteria sc = new SearchCriteria();
    sc.setMaxLevel(2);
    sc.setIncludeSelf(false);
    assertFalse(sc.hasHierarchyConstraints());

    sc.setAncestorOf("  ");
    assertFalse(sc.hasHierarchyConstraints());

    sc.setAncestorOf("C4872");
    assertTrue(sc.hasHierarchyConstraints());

    final SearchCriteria sc2 = new SearchCriteria();
    sc2.setNotDescendantOf(new ArrayList<>(l1));
    assertTrue(sc2.hasHierarchyConstraints());
  }

  /**
   * Test that valid hierarchy parameters pass validation.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValidateHierarchyValid() throws Exception {
    final SearchCriteria sc = criteria("ncit");
    sc.setDescendantOf(new ArrayList<>(l2));
    sc.setNotDescendantOf(new ArrayList<>(l1));
    sc.setAncestorOf("C4872");
    sc.setMaxLevel(1);
    sc.setIncludeSelf(false);
    assertDoesNotThrow(() -> sc.validate(terminology("ncit", true), null));

    // No hierarchy parameters is fine even without a hierarchy
    assertDoesNotThrow(() -> criteria("ncim").validate(terminology("ncim", false), null));
  }

  /**
   * Test that hierarchy parameters require exactly one terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValidateMultipleTerminologies() throws Exception {
    final SearchCriteria sc = criteria("ncit", "go");
    sc.setDescendantOf(new ArrayList<>(l1));
    assertBadRequest(
        () -> sc.validate(terminology("ncit", true), null), "require exactly one terminology");

    // Multiple terminologies without hierarchy parameters remain valid
    assertDoesNotThrow(() -> criteria("ncit", "go").validate(terminology("ncit", true), null));
  }

  /**
   * Test that a terminology without a hierarchy rejects hierarchy parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValidateNoHierarchy() throws Exception {
    for (final String param : Arrays.asList("descendantOf", "notDescendantOf", "ancestorOf")) {
      final SearchCriteria sc = criteria("ncim");
      if (param.equals("descendantOf")) {
        sc.setDescendantOf(new ArrayList<>(l1));
      } else if (param.equals("notDescendantOf")) {
        sc.setNotDescendantOf(new ArrayList<>(l1));
      } else {
        sc.setAncestorOf("C0006826");
      }
      assertBadRequest(
          () -> sc.validate(terminology("ncim", false), null),
          "'ncim' does not support hierarchy parameters");
    }

    // Null hierarchy flag is treated as no hierarchy
    final SearchCriteria sc = criteria("x");
    sc.setDescendantOf(new ArrayList<>(l1));
    assertBadRequest(() -> sc.validate(terminology("x", null), null), "does not support hierarchy");
  }

  /**
   * Test maxLevel validation.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValidateMaxLevel() throws Exception {
    for (final int level : new int[] {0, -1}) {
      final SearchCriteria sc = criteria("ncit");
      sc.setDescendantOf(new ArrayList<>(l1));
      sc.setMaxLevel(level);
      assertBadRequest(() -> sc.validate(terminology("ncit", true), null), "must be >= 1");
    }

    final SearchCriteria noAnchor = criteria("ncit");
    noAnchor.setMaxLevel(2);
    assertBadRequest(
        () -> noAnchor.validate(terminology("ncit", true), null), "requires 'descendantOf'");

    // maxLevel with only ancestorOf is still missing descendantOf
    final SearchCriteria ancestorOnly = criteria("ncit");
    ancestorOnly.setAncestorOf("C4872");
    ancestorOnly.setMaxLevel(2);
    assertBadRequest(
        () -> ancestorOnly.validate(terminology("ncit", true), null), "requires 'descendantOf'");
  }

  /**
   * Test the code count limit and blank codes.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValidateCodeLists() throws Exception {
    final int max = SearchCriteriaWithoutTerminology.MAX_HIERARCHY_CODES;
    final List<String> atMax = new ArrayList<>(Collections.nCopies(max, "C3262"));
    final List<String> overMax = new ArrayList<>(Collections.nCopies(max + 1, "C3262"));

    final SearchCriteria ok = criteria("ncit");
    ok.setDescendantOf(atMax);
    assertDoesNotThrow(() -> ok.validate(terminology("ncit", true), null));

    final SearchCriteria tooMany = criteria("ncit");
    tooMany.setDescendantOf(overMax);
    assertBadRequest(
        () -> tooMany.validate(terminology("ncit", true), null), "at most " + max + " codes");

    final SearchCriteria tooManyNot = criteria("ncit");
    tooManyNot.setNotDescendantOf(overMax);
    assertBadRequest(
        () -> tooManyNot.validate(terminology("ncit", true), null), "'notDescendantOf'");

    final SearchCriteria blank = criteria("ncit");
    blank.setDescendantOf(new ArrayList<>(Arrays.asList("C3262", " ")));
    assertBadRequest(
        () -> blank.validate(terminology("ncit", true), null),
        "'descendantOf' has an invalid value");
  }

  /**
   * Proxy list fields for the shared testers.
   *
   * @param tester the tester
   */
  private void proxyLists(final gov.nih.nci.evs.api.ProxyTester tester) {
    for (final String field :
        Arrays.asList(
            "conceptStatus",
            "property",
            "synonymSource",
            "synonymType",
            "definitionSource",
            "definitionType",
            "synonymTermType",
            "subset",
            "codeList",
            "descendantOf",
            "notDescendantOf",
            "terminology")) {
      tester.proxy(field, 1, new ArrayList<>(l1));
      tester.proxy(field, 2, new ArrayList<>(l2));
    }
  }

  /**
   * Returns criteria with hierarchy fields set.
   *
   * @return the search criteria
   */
  private SearchCriteria hierarchyCriteria() {
    final SearchCriteria sc = new SearchCriteria();
    sc.setDescendantOf(new ArrayList<>(l1));
    sc.setMaxLevel(2);
    sc.setIncludeSelf(false);
    return sc;
  }

  /**
   * Returns criteria for the given terminologies.
   *
   * @param terminologies the terminologies
   * @return the search criteria
   */
  private SearchCriteria criteria(final String... terminologies) {
    final SearchCriteria sc = new SearchCriteria();
    sc.setTerminology(new ArrayList<>(Arrays.asList(terminologies)));
    return sc;
  }

  /**
   * Returns a terminology with the given hierarchy flag.
   *
   * @param name the terminology name
   * @param hierarchy the hierarchy flag
   * @return the terminology
   */
  private Terminology terminology(final String name, final Boolean hierarchy) {
    final Terminology terminology = new Terminology();
    terminology.setTerminology(name);
    final TerminologyMetadata metadata = new TerminologyMetadata();
    metadata.setHierarchy(hierarchy);
    terminology.setMetadata(metadata);
    return terminology;
  }

  /**
   * Assert that the call fails with 400 and a message containing the text.
   *
   * @param call the call
   * @param text the expected message text
   */
  private void assertBadRequest(
      final org.junit.jupiter.api.function.Executable call, final String text) {
    final ResponseStatusException e = assertThrows(ResponseStatusException.class, call);
    assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    assertTrue(
        e.getReason() != null && e.getReason().contains(text),
        "expected message containing [" + text + "] but was [" + e.getReason() + "]");
  }
}
