package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.HasInverseRole;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasInverseRole}. */
public class HasInverseRoleUnitTest {

  /** The query for the primary anatomic site of Breast Carcinoma. */
  private static final String PRIMARY_ANATOMIC_SITE_OF_BREAST_CARCINOMA_QUERY =
      """
      {"type": "HasInverseRole", "role": "R101", "target": "C4872"}
      """;

  /** Breast Carcinoma, whose primary anatomic site is Breast. */
  private static final String BREAST_CARCINOMA = "C4872";

  /** The role from a disease to its primary anatomic site (Disease_Has_Primary_Anatomic_Site). */
  private static final String PRIMARY_ANATOMIC_SITE_ROLE = "R101";

  /**
   * Test the HasInverseRole query reads from and writes back to the same JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasInverseRoleRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(PRIMARY_ANATOMIC_SITE_OF_BREAST_CARCINOMA_QUERY);
  }

  /**
   * Test the HasInverseRole query built in Java is written as the expected JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasInverseRoleSerializesToExpectedJson() throws Exception {
    final HasInverseRole primaryAnatomicSiteOfBreastCarcinoma =
        new HasInverseRole(PRIMARY_ANATOMIC_SITE_ROLE, BREAST_CARCINOMA);

    assertSerializesTo(
        primaryAnatomicSiteOfBreastCarcinoma, PRIMARY_ANATOMIC_SITE_OF_BREAST_CARCINOMA_QUERY);
  }
}
