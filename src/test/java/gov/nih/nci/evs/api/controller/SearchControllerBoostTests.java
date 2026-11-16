package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * Tests Boost, which raises the score of the concepts its child matches without changing which
 * concepts match, through the query endpoint.
 */
public class SearchControllerBoostTests extends AbstractSearchControllerQueryTests {

  /** The children of Breast, boosted. */
  private static final String BOOSTED_CHILDREN_OF_BREAST_QUERY =
      """
      {"type": "Boost", "score": 2.0, "child":
        {"type": "HasAncestor", "target": "C12971", "maxLevel": 1}}
      """;

  /**
   * Test Boost returns the same concepts as its child.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBoostReturnsConceptsItsChildMatches() throws Exception {
    assertThat(fetchCodes(BOOSTED_CHILDREN_OF_BREAST_QUERY))
        .containsExactlyInAnyOrderElementsOf(BREAST_CHILDREN);
  }

  /**
   * Test Boost with a score that is not a number is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBoostWithNonNumericScoreReturnsBadRequest() throws Exception {
    final String nonNumericScoreQuery =
        """
        {"type": "Boost", "score": "high", "child":
          {"type": "HasRole", "role": "R101"}}
        """;

    postQuery(nonNumericScoreQuery).andExpect(status().isBadRequest());
  }
}
