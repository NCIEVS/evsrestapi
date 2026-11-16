package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.HasDescendant;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasDescendant}. */
public class HasDescendantUnitTest {

  /** The query for the parent of Female Breast, with a max level of 1. */
  private static final String PARENT_OF_FEMALE_BREAST_QUERY =
      """
      {"type": "HasDescendant", "target": "C12851", "maxLevel": 1}
      """;

  /** The max level that limits hierarchy queries to direct children or parents. */
  private static final int DIRECT_RELATIVES_MAX_LEVEL = 1;

  /** Female Breast, whose only parent is Breast. */
  private static final String FEMALE_BREAST = "C12851";

  /**
   * Test the HasDescendant query reads from and writes back to the same JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasDescendantRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(PARENT_OF_FEMALE_BREAST_QUERY);
  }

  /**
   * Test the HasDescendant query built in Java is written as the expected JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasDescendantSerializesToExpectedJson() throws Exception {
    final HasDescendant parentOfFemaleBreast =
        new HasDescendant(FEMALE_BREAST, DIRECT_RELATIVES_MAX_LEVEL);

    assertSerializesTo(parentOfFemaleBreast, PARENT_OF_FEMALE_BREAST_QUERY);
  }
}
