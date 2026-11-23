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

  // ---------------------------------------------------------------------------------------------
  // Behavior
  // ---------------------------------------------------------------------------------------------

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

  // ---------------------------------------------------------------------------------------------
  // maxLevel: optional, at least 1
  // ---------------------------------------------------------------------------------------------

  /**
   * Test no maxLevel is accepted: every level below the target.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorWithoutMaxLevelReturnsOk() throws Exception {
    final String withoutMaxLevelQuery =
        """
        {"type": "HasAncestor", "target": "C12971"}
        """;

    postQuery(withoutMaxLevelQuery).andExpect(status().isOk());
  }

  /**
   * Test the smallest maxLevel allowed, 1, is accepted.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorWithMaxLevelOneReturnsOk() throws Exception {
    postQuery(CHILDREN_OF_BREAST_QUERY).andExpect(status().isOk());
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

  /**
   * Test HasAncestor with a max level of 0, below the smallest allowed (1), is rejected as a bad
   * request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorWithMaxLevelZeroReturnsBadRequest() throws Exception {
    final String maxLevelZeroQuery =
        """
        {"type": "HasAncestor", "target": "C12971", "maxLevel": 0}
        """;

    postQuery(maxLevelZeroQuery).andExpect(status().isBadRequest());
  }

  // ---------------------------------------------------------------------------------------------
  // target: required, a concept's name or code
  // ---------------------------------------------------------------------------------------------

  /**
   * Test a target given by code is accepted.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorWithTargetByCodeReturnsOk() throws Exception {
    final String targetByCodeQuery =
        """
        {"type": "HasAncestor", "target": "C12971"}
        """;

    postQuery(targetByCodeQuery).andExpect(status().isOk());
  }

  /**
   * Test a target given by name is accepted.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorWithTargetByNameReturnsOk() throws Exception {
    final String targetByNameQuery =
        """
        {"type": "HasAncestor", "target": "Breast", "maxLevel": 1}
        """;

    postQuery(targetByNameQuery).andExpect(status().isOk());
  }

  /**
   * Test HasAncestor without a target is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorWithoutTargetReturnsBadRequest() throws Exception {
    final String withoutTargetQuery =
        """
        {"type": "HasAncestor", "maxLevel": 1}
        """;

    postQuery(withoutTargetQuery).andExpect(status().isBadRequest());
  }
}
