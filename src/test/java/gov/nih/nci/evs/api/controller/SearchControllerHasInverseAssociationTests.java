package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests the HasInverseAssociation query node, which matches the concepts a target concept has an
 * association to, through the query endpoint.
 */
public class SearchControllerHasInverseAssociationTests extends AbstractSearchControllerQueryTests {

  /** The query for the subsets Breast Carcinoma is in. */
  private static final String SUBSETS_OF_BREAST_CARCINOMA_QUERY =
      """
      {"type": "HasInverseAssociation", "association": "A8", "target": "C4872"}
      """;

  /**
   * Test HasInverseAssociation returns the concepts the target has the association to: the 18
   * subsets Breast Carcinoma is in.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasInverseAssociationReturnsConceptsTheTargetPointsTo() throws Exception {
    assertThat(fetchCodes(SUBSETS_OF_BREAST_CARCINOMA_QUERY))
        .containsExactlyInAnyOrder(
            "C102905", "C103042", "C116977", "C118168", "C118464", "C126659", "C138188", "C138190",
            "C156952", "C156966", "C157711", "C158035", "C158520", "C165258", "C177537", "C189762",
            "C54450", "C90259");
  }
}
