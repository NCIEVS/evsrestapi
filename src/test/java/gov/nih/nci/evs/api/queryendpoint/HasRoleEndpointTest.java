package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.query.HasRole;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the HasRole query node, which matches the concepts with a role to a target concept, through
 * the query endpoint.
 */
public class HasRoleEndpointTest extends AbstractQueryEndpointTest {

  /** The query for the concepts whose primary anatomic site is Breast. */
  private static final String PRIMARY_ANATOMIC_SITE_BREAST_QUERY =
      """
      {"type": "HasRole", "role": "R101", "target": "C12971"}
      """;

  /**
   * Test HasRole returns the concepts with the role to the target, e.g. Breast Carcinoma, whose
   * primary anatomic site is Breast, and that every concept returned has that role.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleReturnsConceptsWithTheRole() throws Exception {
    final String includeRoles = "minimal,roles";

    final List<Concept> concepts =
        fetchQueryResults(PRIMARY_ANATOMIC_SITE_BREAST_QUERY, includeRoles).getConcepts();

    assertThat(concepts).extracting(Concept::getCode).contains(BREAST_CARCINOMA);
    assertThat(concepts).allMatch(HasRoleEndpointTest::getHasPrimaryAnatomicSiteBreast);
  }

  /**
   * Test HasRole without a type is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithoutTypeReturnsBadRequest() throws Exception {
    final String withoutTypeQuery =
        """
        {"role": "R101"}
        """;

    postQuery(withoutTypeQuery).andExpect(status().isBadRequest());
  }

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
