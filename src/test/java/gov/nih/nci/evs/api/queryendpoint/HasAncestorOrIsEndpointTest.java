package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.query.HasAncestorOrIs;
import org.junit.jupiter.api.Test;

/**
 * Tests the HasAncestorOrIs node, which matches a target concept and its descendants, through the
 * query endpoint.
 */
public class HasAncestorOrIsEndpointTest extends AbstractQueryEndpointTest {

  /** Breast and its descendants at most one level down, which are its children. */
  private static final String BREAST_AND_CHILDREN_QUERY =
      """
      {"type": "HasAncestorOrIs", "target": "C12971", "maxLevel": 1}
      """;

  /**
   * Test HasAncestorOrIs with a max level of 1 returns the target and its children.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorOrIsReturnsTargetAndChildren() throws Exception {
    assertThat(fetchCodes(BREAST_AND_CHILDREN_QUERY))
        .containsExactlyInAnyOrderElementsOf(getWithBreast(BREAST_CHILDREN));
  }

  /**
   * Test HasAncestorOrIs with a misspelled node type (HasAncestorOrSelf) is rejected as a bad
   * request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorOrIsWithMisspelledNodeTypeReturnsBadRequest() throws Exception {
    final String misspelledNodeTypeQuery =
        """
        {"type": "HasAncestorOrSelf", "target": "C12971"}
        """;

    postQuery(misspelledNodeTypeQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test HasAncestorOrIs round trips through Jackson unchanged.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorOrIsRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(BREAST_AND_CHILDREN_QUERY);
  }

  /**
   * Test HasAncestorOrIs serializes to the expected JSON, the wire format clients use.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAncestorOrIsSerializesToExpectedJson() throws Exception {
    final HasAncestorOrIs breastAndChildren =
        new HasAncestorOrIs(BREAST, DIRECT_RELATIVES_MAX_LEVEL);

    assertSerializesTo(breastAndChildren, BREAST_AND_CHILDREN_QUERY);
  }
}
