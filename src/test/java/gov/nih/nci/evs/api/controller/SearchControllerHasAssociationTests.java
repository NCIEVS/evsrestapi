package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * Tests the HasAssociation query node, which matches the concepts with an association to a target
 * concept, through the query endpoint.
 */
public class SearchControllerHasAssociationTests extends AbstractSearchControllerQueryTests {

  /** The query for the members of the subset whose only member is Breast Carcinoma. */
  private static final String MEMBERS_OF_BREAST_CARCINOMA_ONLY_SUBSET_QUERY =
      """
      {"type": "HasAssociation", "association": "A8", "target": "C103042"}
      """;

  /**
   * Test HasAssociation returns the concepts with the association to the target: the members of a
   * subset whose only member is Breast Carcinoma.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAssociationReturnsConceptsWithTheAssociation() throws Exception {
    assertThat(fetchCodes(MEMBERS_OF_BREAST_CARCINOMA_ONLY_SUBSET_QUERY))
        .containsExactly(BREAST_CARCINOMA);
  }

  /**
   * Test HasAssociation without an association is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAssociationWithoutAssociationReturnsBadRequest() throws Exception {
    final String withoutAssociationQuery =
        """
        {"type": "HasAssociation", "target": "C103042"}
        """;

    postQuery(withoutAssociationQuery).andExpect(status().isBadRequest());
  }
}
