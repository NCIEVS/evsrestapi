package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.HasInverseAssociation;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasInverseAssociation}. */
public class HasInverseAssociationUnitTest {

  /** The query for the subsets Breast Carcinoma is in. */
  private static final String SUBSETS_OF_BREAST_CARCINOMA_QUERY =
      """
      {"type": "HasInverseAssociation", "association": "A8", "target": "C4872"}
      """;

  /** Breast Carcinoma, whose primary anatomic site is Breast. */
  private static final String BREAST_CARCINOMA = "C4872";

  /** The association from a concept to a subset it's in (Concept_In_Subset). */
  private static final String IN_SUBSET_ASSOCIATION = "A8";

  /**
   * Test the HasInverseAssociation query reads from and writes back to the same JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasInverseAssociationRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(SUBSETS_OF_BREAST_CARCINOMA_QUERY);
  }

  /**
   * Test the HasInverseAssociation query built in Java is written as the expected JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasInverseAssociationSerializesToExpectedJson() throws Exception {
    final HasInverseAssociation subsetsOfBreastCarcinoma =
        new HasInverseAssociation(IN_SUBSET_ASSOCIATION, BREAST_CARCINOMA);

    assertSerializesTo(subsetsOfBreastCarcinoma, SUBSETS_OF_BREAST_CARCINOMA_QUERY);
  }
}
