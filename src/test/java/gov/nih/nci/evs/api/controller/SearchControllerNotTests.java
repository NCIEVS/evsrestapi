package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** Tests Not, which leaves out the concepts its child matches, through the query endpoint. */
public class SearchControllerNotTests extends AbstractSearchControllerQueryTests {

  /** Breast and its children, without the children of Breast. */
  private static final String BREAST_WITHOUT_CHILDREN_QUERY =
      """
      {"type": "And", "children": [
        {"type": "Or", "children": [
          {"type": "Exact", "term": "C12971"},
          {"type": "HasAncestor", "target": "C12971", "maxLevel": 1}]},
        {"type": "Not", "child":
          {"type": "HasAncestor", "target": "C12971", "maxLevel": 1}}]}
      """;

  /**
   * Test Not leaves out the concepts its child matches: Breast and its children, without its
   * children, is Breast alone.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNotLeavesOutConceptsItsChildMatches() throws Exception {
    assertThat(fetchCodes(BREAST_WITHOUT_CHILDREN_QUERY)).containsExactly(BREAST);
  }

  /**
   * Test Not with a child given as a type name, not a query, is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNotWithChildGivenAsTypeNameReturnsBadRequest() throws Exception {
    final String childAsTypeNameQuery =
        """
        {"type": "Not", "child": "HasRole"}
        """;

    postQuery(childAsTypeNameQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test Not without a child is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNotWithoutChildReturnsBadRequest() throws Exception {
    final String withoutChildQuery =
        """
        {"type": "Not"}
        """;

    postQuery(withoutChildQuery).andExpect(status().isBadRequest());
  }
}
