package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * Tests the HasAncestor node, which matches the descendants of a target concept, through the query
 * endpoint.
 */
public class SearchControllerHasAncestorTests extends AbstractSearchControllerQueryTests {

  /** The descendants of Breast at most one level down, which are its children. */
  private static final String CHILDREN_OF_BREAST_QUERY =
      """
      {"type": "HasAncestor", "target": "C12971", "maxLevel": 1}
      """;

  /**
   * Test HasAncestor with a max level of 1 returns the target's children, without the target.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorReturnsChildren() throws Exception {
    assertThat(fetchCodes(CHILDREN_OF_BREAST_QUERY))
        .containsExactlyInAnyOrderElementsOf(BREAST_CHILDREN);
  }

  /**
   * Test HasAncestor with a max level that is not a number is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorWithNonNumericMaxLevelReturnsBadRequest() throws Exception {
    final String nonNumericMaxLevelQuery =
        """
        {"type": "HasAncestor", "target": "C12971", "maxLevel": "one"}
        """;

    postQuery(nonNumericMaxLevelQuery).andExpect(status().isBadRequest());
  }
}
