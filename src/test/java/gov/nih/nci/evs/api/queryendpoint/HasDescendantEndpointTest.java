package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;

import gov.nih.nci.evs.api.query.HasDescendant;
import org.junit.jupiter.api.Test;

/**
 * Tests the HasDescendant query node, which matches the ancestors of a target concept, through the
 * query endpoint.
 */
public class HasDescendantEndpointTest extends AbstractQueryEndpointTest {

  /** The query for the parent of Female Breast, with a max level of 1. */
  private static final String PARENT_OF_FEMALE_BREAST_QUERY =
      """
      {"type": "HasDescendant", "target": "C12851", "maxLevel": 1}
      """;

  /**
   * Test HasDescendant with a max level of 1 returns the target's parent, without the target.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasDescendantReturnsParent() throws Exception {
    assertThat(fetchCodes(PARENT_OF_FEMALE_BREAST_QUERY)).containsExactly(BREAST);
  }

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
