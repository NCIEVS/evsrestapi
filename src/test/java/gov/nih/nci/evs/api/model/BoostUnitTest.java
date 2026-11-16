package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.Boost;
import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.query.HasAncestor;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link Boost}. */
public class BoostUnitTest {

  /** The children of Breast, boosted. */
  private static final String BOOSTED_CHILDREN_OF_BREAST_QUERY =
      """
      {"type": "Boost", "score": 2.0, "child":
        {"type": "HasAncestor", "target": "C12971", "maxLevel": 1}}
      """;

  /** Breast, the target of most hierarchy tests. */
  private static final String BREAST = "C12971";

  /** The max level that limits hierarchy queries to direct children or parents. */
  private static final int DIRECT_RELATIVES_MAX_LEVEL = 1;

  /**
   * Test Boost round trips unchanged through Jackson.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBoostRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(BOOSTED_CHILDREN_OF_BREAST_QUERY);
  }

  /**
   * Test Boost serializes to the expected JSON when built in Java: the Boost of the children of
   * Breast.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBoostSerializesToExpectedJson() throws Exception {
    final float score = 2.0f;
    final EvsQuery boostedChildrenOfBreast =
        new Boost(new HasAncestor(BREAST, DIRECT_RELATIVES_MAX_LEVEL), score);

    assertSerializesTo(boostedChildrenOfBreast, BOOSTED_CHILDREN_OF_BREAST_QUERY);
  }
}
