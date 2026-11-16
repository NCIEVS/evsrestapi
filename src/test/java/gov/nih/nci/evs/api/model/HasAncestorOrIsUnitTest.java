package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.HasAncestorOrIs;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasAncestorOrIs}. */
public class HasAncestorOrIsUnitTest {

  /** Breast and its descendants at most one level down, which are its children. */
  private static final String BREAST_AND_CHILDREN_QUERY =
      """
      {"type": "HasAncestorOrIs", "target": "C12971", "maxLevel": 1}
      """;

  /** Breast, the target of most hierarchy tests. */
  private static final String BREAST = "C12971";

  /** The max level that limits hierarchy queries to direct children or parents. */
  private static final int DIRECT_RELATIVES_MAX_LEVEL = 1;

  /**
   * Test HasAncestorOrIs round trips through Jackson unchanged.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorOrIsRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(BREAST_AND_CHILDREN_QUERY);
  }

  /**
   * Test HasAncestorOrIs serializes to the expected JSON, the wire format clients use.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorOrIsSerializesToExpectedJson() throws Exception {
    final HasAncestorOrIs breastAndChildren =
        new HasAncestorOrIs(BREAST, DIRECT_RELATIVES_MAX_LEVEL);

    assertSerializesTo(breastAndChildren, BREAST_AND_CHILDREN_QUERY);
  }
}
