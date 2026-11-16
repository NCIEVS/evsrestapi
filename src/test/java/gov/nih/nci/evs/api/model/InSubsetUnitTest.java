package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.InSubset;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link InSubset}. */
public class InSubsetUnitTest {

  /** The members of the subset whose only member is Breast Carcinoma. */
  private static final String BREAST_CARCINOMA_SUBSET_MEMBERS_QUERY =
      """
      {"type": "InSubset", "target": "C103042"}
      """;

  /** A subset whose only member is Breast Carcinoma. */
  private static final String BREAST_CARCINOMA_ONLY_SUBSET = "C103042";

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
