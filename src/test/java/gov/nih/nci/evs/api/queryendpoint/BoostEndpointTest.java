package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.query.Boost;
import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.query.HasAncestor;
import org.junit.jupiter.api.Test;

/**
 * Tests Boost, which raises the score of the concepts its child matches without changing which
 * concepts match, through the query endpoint.
 */
public class BoostEndpointTest extends AbstractQueryEndpointTest {

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
