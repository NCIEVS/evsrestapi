package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.HasDescendantOrIs;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasDescendantOrIs}. */
public class HasDescendantOrIsUnitTest {

  /** The query for Female Breast and its parent, with a max level of 1. */
  private static final String FEMALE_BREAST_AND_PARENT_QUERY =
      """
      {"type": "HasDescendantOrIs", "target": "C12851", "maxLevel": 1}
      """;

  /** The max level that limits hierarchy queries to direct children or parents. */
  private static final int DIRECT_RELATIVES_MAX_LEVEL = 1;

  /** Female Breast, whose only parent is Breast. */
  private static final String FEMALE_BREAST = "C12851";

  /**
   * Test the HasDescendantOrIs query reads from and writes back to the same JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasDescendantOrIsRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(FEMALE_BREAST_AND_PARENT_QUERY);
  }

  /**
   * Test the HasDescendantOrIs query built in Java is written as the expected JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasDescendantOrIsSerializesToExpectedJson() throws Exception {
    final HasDescendantOrIs femaleBreastAndParent =
        new HasDescendantOrIs(FEMALE_BREAST, DIRECT_RELATIVES_MAX_LEVEL);

    assertSerializesTo(femaleBreastAndParent, FEMALE_BREAST_AND_PARENT_QUERY);
  }
}
