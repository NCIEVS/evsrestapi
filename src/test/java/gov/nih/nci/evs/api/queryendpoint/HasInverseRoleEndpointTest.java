package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;

import gov.nih.nci.evs.api.query.HasInverseRole;
import org.junit.jupiter.api.Test;

/**
 * Tests the HasInverseRole query node, which matches the concepts a target concept has a role to,
 * through the query endpoint.
 */
public class HasInverseRoleEndpointTest extends AbstractQueryEndpointTest {

  /** The query for the primary anatomic site of Breast Carcinoma. */
  private static final String PRIMARY_ANATOMIC_SITE_OF_BREAST_CARCINOMA_QUERY =
      """
      {"type": "HasInverseRole", "role": "R101", "target": "C4872"}
      """;

  /**
   * Test HasInverseRole returns the concepts the target has the role to: the primary anatomic site
   * of Breast Carcinoma, which is Breast.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasInverseRoleReturnsConceptsTheTargetPointsTo() throws Exception {
    assertThat(fetchCodes(PRIMARY_ANATOMIC_SITE_OF_BREAST_CARCINOMA_QUERY)).containsExactly(BREAST);
  }

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
