package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.HasRole;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasRole}. */
public class HasRoleUnitTest {

  /** The query for the concepts whose primary anatomic site is Breast. */
  private static final String PRIMARY_ANATOMIC_SITE_BREAST_QUERY =
      """
      {"type": "HasRole", "role": "R101", "target": "C12971"}
      """;

  /** Breast, the target of most hierarchy tests. */
  private static final String BREAST = "C12971";

  /** The role from a disease to its primary anatomic site (Disease_Has_Primary_Anatomic_Site). */
  private static final String PRIMARY_ANATOMIC_SITE_ROLE = "R101";

  /**
   * Test the HasRole query reads from and writes back to the same JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(PRIMARY_ANATOMIC_SITE_BREAST_QUERY);
  }

  /**
   * Test the HasRole query built in Java is written as the expected JSON.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleSerializesToExpectedJson() throws Exception {
    final HasRole primaryAnatomicSiteBreast = new HasRole(PRIMARY_ANATOMIC_SITE_ROLE, BREAST);

    assertSerializesTo(primaryAnatomicSiteBreast, PRIMARY_ANATOMIC_SITE_BREAST_QUERY);
  }
}
