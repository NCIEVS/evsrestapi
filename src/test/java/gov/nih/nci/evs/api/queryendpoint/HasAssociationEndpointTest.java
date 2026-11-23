package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;

import gov.nih.nci.evs.api.query.HasAssociation;
import org.junit.jupiter.api.Test;

/**
 * Tests the HasAssociation query node, which matches the concepts with an association to a target
 * concept, through the query endpoint.
 */
public class HasAssociationEndpointTest extends AbstractQueryEndpointTest {

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
   * Test the HasAssociation query reads from and writes back to the same JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAssociationRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(MEMBERS_OF_BREAST_CARCINOMA_ONLY_SUBSET_QUERY);
  }

  /**
   * Test the HasAssociation query built in Java is written as the expected JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasAssociationSerializesToExpectedJson() throws Exception {
    final HasAssociation membersOfBreastCarcinomaOnlySubset =
        new HasAssociation(IN_SUBSET_ASSOCIATION, BREAST_CARCINOMA_ONLY_SUBSET);

    assertSerializesTo(
        membersOfBreastCarcinomaOnlySubset, MEMBERS_OF_BREAST_CARCINOMA_ONLY_SUBSET_QUERY);
  }
}
