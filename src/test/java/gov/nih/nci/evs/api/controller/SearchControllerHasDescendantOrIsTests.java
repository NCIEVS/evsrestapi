package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests the HasDescendantOrIs query node, which matches a target concept and its ancestors, through
 * the query endpoint.
 */
public class SearchControllerHasDescendantOrIsTests extends AbstractSearchControllerQueryTests {

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
}
