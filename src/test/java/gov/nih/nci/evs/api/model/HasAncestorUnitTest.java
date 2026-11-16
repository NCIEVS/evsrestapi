package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.HasAncestor;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasAncestor}. */
public class HasAncestorUnitTest {

  /** The descendants of Breast at most one level down, which are its children. */
  private static final String CHILDREN_OF_BREAST_QUERY =
      """
      {"type": "HasAncestor", "target": "C12971", "maxLevel": 1}
      """;

  /** Breast, the target of most hierarchy tests. */
  private static final String BREAST = "C12971";

  /** The max level that limits hierarchy queries to direct children or parents. */
  private static final int DIRECT_RELATIVES_MAX_LEVEL = 1;

  /**
   * Test HasAncestor round trips through Jackson unchanged.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(CHILDREN_OF_BREAST_QUERY);
  }

  /**
   * Test HasAncestor serializes to the expected JSON, the wire format clients use.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorSerializesToExpectedJson() throws Exception {
    final HasAncestor childrenOfBreast = new HasAncestor(BREAST, DIRECT_RELATIVES_MAX_LEVEL);

    assertSerializesTo(childrenOfBreast, CHILDREN_OF_BREAST_QUERY);
  }
}
