package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests the HasInverseRole query node, which matches the concepts a target concept has a role to,
 * through the query endpoint.
 */
public class SearchControllerHasInverseRoleTests extends AbstractSearchControllerQueryTests {

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
}
