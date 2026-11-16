package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.HasAssociation;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasAssociation}. */
public class HasAssociationUnitTest {

  /** The query for the members of the subset whose only member is Breast Carcinoma. */
  private static final String MEMBERS_OF_BREAST_CARCINOMA_ONLY_SUBSET_QUERY =
      """
      {"type": "HasAssociation", "association": "A8", "target": "C103042"}
      """;

  /** The association from a concept to a subset it's in (Concept_In_Subset). */
  private static final String IN_SUBSET_ASSOCIATION = "A8";

  /** A subset whose only member is Breast Carcinoma. */
  private static final String BREAST_CARCINOMA_ONLY_SUBSET = "C103042";

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
