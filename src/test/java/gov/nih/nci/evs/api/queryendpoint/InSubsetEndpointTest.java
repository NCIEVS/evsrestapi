package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.query.InSubset;
import org.junit.jupiter.api.Test;

/** Tests the InSubset node, which matches the members of a subset, through the query endpoint. */
public class InSubsetEndpointTest extends AbstractQueryEndpointTest {

  /** The members of the subset whose only member is Breast Carcinoma. */
  private static final String BREAST_CARCINOMA_SUBSET_MEMBERS_QUERY =
      """
      {"type": "InSubset", "target": "C103042"}
      """;

  /**
   * Test InSubset returns the members of the subset.
   *
   * @throws Exception the exception
   */
  @Test
  public void testInSubsetReturnsSubsetMembers() throws Exception {
    assertThat(fetchCodes(BREAST_CARCINOMA_SUBSET_MEMBERS_QUERY)).containsExactly(BREAST_CARCINOMA);
  }

  /**
   * Test InSubset with a constructor parameter name (subsetCode) instead of a property is rejected
   * as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testInSubsetWithConstructorParameterNameReturnsBadRequest() throws Exception {
    final String subsetCodeQuery =
        """
        {"type": "InSubset", "subsetCode": "C103042"}
        """;

    postQuery(subsetCodeQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test InSubset round trips through Jackson unchanged.
   *
   * @throws Exception the exception
   */
  @Test
  public void testInSubsetRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(BREAST_CARCINOMA_SUBSET_MEMBERS_QUERY);
  }

  /**
   * Test InSubset serializes to the expected JSON, the wire format clients use.
   *
   * @throws Exception the exception
   */
  @Test
  public void testInSubsetSerializesToExpectedJson() throws Exception {
    final InSubset breastCarcinomaSubsetMembers = new InSubset(BREAST_CARCINOMA_ONLY_SUBSET);

    assertSerializesTo(breastCarcinomaSubsetMembers, BREAST_CARCINOMA_SUBSET_MEMBERS_QUERY);
  }
}
