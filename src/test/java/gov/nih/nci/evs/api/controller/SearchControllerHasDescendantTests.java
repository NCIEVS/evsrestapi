package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * Tests the HasDescendant query node, which matches the ancestors of a target concept, through the
 * query endpoint.
 */
public class SearchControllerHasDescendantTests extends AbstractSearchControllerQueryTests {

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
   * Test HasDescendant without a target is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasDescendantWithoutTargetReturnsBadRequest() throws Exception {
    final String withoutTargetQuery =
        """
        {"type": "HasDescendant"}
        """;

    postQuery(withoutTargetQuery).andExpect(status().isBadRequest());
  }
}
