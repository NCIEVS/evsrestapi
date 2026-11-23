package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;

import gov.nih.nci.evs.api.query.HasDescendantOrIs;
import org.junit.jupiter.api.Test;

/**
 * Tests the HasDescendantOrIs query node, which matches a target concept and its ancestors, through
 * the query endpoint.
 */
public class HasDescendantOrIsEndpointTest extends AbstractQueryEndpointTest {

  /** The query for Female Breast and its parent, with a max level of 1. */
  private static final String FEMALE_BREAST_AND_PARENT_QUERY =
      """
      {"type": "HasDescendantOrIs", "target": "C12851", "maxLevel": 1}
      """;

  /**
   * Test HasDescendantOrIs with a max level of 1 returns the target and its parent.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasDescendantOrIsReturnsTargetAndParent() throws Exception {
    assertThat(fetchCodes(FEMALE_BREAST_AND_PARENT_QUERY))
        .containsExactlyInAnyOrder(FEMALE_BREAST, BREAST);
  }

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
