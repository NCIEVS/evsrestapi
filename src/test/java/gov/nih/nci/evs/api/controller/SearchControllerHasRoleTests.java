package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.model.Concept;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the HasRole query node, which matches the concepts with a role to a target concept, through
 * the query endpoint.
 *
 * <p>The NCIt tests check that results contain a known concept and that every result has the role.
 * The evstest tests expect exact results, since the test terminology's data doesn't change; each
 * one follows an example or limit in the HasRole section of the agent spec. evstest's roles:
 *
 * <pre>
 * Concept                                  R101 primary site   R102 associated site
 * FK1000  Cutaneous Melanoma               Skin
 * FK10000 Superficial Spreading Melanoma   Skin                Lung
 * FK10001 Nodular Melanoma                 Skin
 * FK1010  Lung Carcinoma                   Lung                Skin
 * FK1011  Skin Carcinoma                   Skin
 * FK200   Pneumonia                        Lung
 * </pre>
 */
public class SearchControllerHasRoleTests extends AbstractSearchControllerQueryTests {

  /** The query for the concepts whose primary anatomic site is Breast. */
  private static final String PRIMARY_ANATOMIC_SITE_BREAST_QUERY =
      """
      {"type": "HasRole", "role": "R101", "target": "C12971"}
      """;

  /** evstest's Cutaneous Melanoma, primary site Skin. */
  private static final String CUTANEOUS_MELANOMA = "FK1000";

  /** evstest's Superficial Spreading Melanoma, primary site Skin, associated site Lung. */
  private static final String SUPERFICIAL_SPREADING_MELANOMA = "FK10000";

  /** evstest's Nodular Melanoma, primary site Skin. */
  private static final String NODULAR_MELANOMA = "FK10001";

  /** evstest's Lung Carcinoma, primary site Lung, associated site Skin. */
  private static final String LUNG_CARCINOMA = "FK1010";

  /** evstest's Skin Carcinoma, primary site Skin. */
  private static final String SKIN_CARCINOMA = "FK1011";

  /** evstest's Pneumonia, primary site Lung. */
  private static final String PNEUMONIA = "FK200";

  /** The evstest concepts whose primary site is Skin. */
  private static final List<String> PRIMARY_SITE_SKIN =
      List.of(CUTANEOUS_MELANOMA, SUPERFICIAL_SPREADING_MELANOMA, NODULAR_MELANOMA, SKIN_CARCINOMA);

  /** The evstest concepts with any primary site. */
  private static final List<String> ANY_PRIMARY_SITE =
      List.of(
          CUTANEOUS_MELANOMA,
          SUPERFICIAL_SPREADING_MELANOMA,
          NODULAR_MELANOMA,
          LUNG_CARCINOMA,
          SKIN_CARCINOMA,
          PNEUMONIA);

  /** The evstest query for the concepts whose primary site is Skin, by codes. */
  private static final String PRIMARY_SITE_SKIN_BY_CODE_QUERY =
      """
      {"type": "HasRole", "role": "R101", "target": "FK21"}
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
    assertThat(concepts).allMatch(SearchControllerHasRoleTests::getHasPrimaryAnatomicSiteBreast);
  }

  // ---------------------------------------------------------------------------------------------
  // evstest: exact results
  // ---------------------------------------------------------------------------------------------

  /**
   * Test HasRole by role code and target code returns exactly the concepts with that role to that
   * target.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleByCodeReturnsConceptsWithRoleToTarget() throws Exception {
    assertThat(fetchCodes(EVSTEST, PRIMARY_SITE_SKIN_BY_CODE_QUERY))
        .containsExactlyInAnyOrderElementsOf(PRIMARY_SITE_SKIN);
  }

  /**
   * Test HasRole by role name and a one-word lowercase target name returns the same concepts as by
   * codes.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleByNameReturnsSameConceptsAsByCode() throws Exception {
    final String byNameQuery =
        """
        {"type": "HasRole", "role": "Disease_Has_Primary_Anatomic_Site", "target": "skin"}
        """;

    assertThat(fetchCodes(EVSTEST, byNameQuery))
        .containsExactlyInAnyOrderElementsOf(PRIMARY_SITE_SKIN);
  }

  /**
   * Test HasRole without a target returns every concept with the role, whatever it points to.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithoutTargetReturnsEveryConceptWithRole() throws Exception {
    final String anyPrimarySiteQuery =
        """
        {"type": "HasRole", "role": "R101"}
        """;

    assertThat(fetchCodes(EVSTEST, anyPrimarySiteQuery))
        .containsExactlyInAnyOrderElementsOf(ANY_PRIMARY_SITE);
  }

  /**
   * Test HasRole leaves out a concept with the role and the target on different roles: Lung
   * Carcinoma's primary site is Lung, and it points to Skin only through its associated site.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleLeavesOutRoleAndTargetOnDifferentRoles() throws Exception {
    assertThat(fetchCodes(EVSTEST, PRIMARY_SITE_SKIN_BY_CODE_QUERY)).doesNotContain(LUNG_CARCINOMA);
  }

  /**
   * Test HasRoles in an Or return the concepts with either target.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRolesInOrReturnConceptsWithEitherTarget() throws Exception {
    final String primarySiteSkinOrLungQuery =
        """
        {"type": "Or", "children": [
          {"type": "HasRole", "role": "R101", "target": "FK21"},
          {"type": "HasRole", "role": "R101", "target": "FK22"}]}
        """;

    assertThat(fetchCodes(EVSTEST, primarySiteSkinOrLungQuery))
        .containsExactlyInAnyOrderElementsOf(ANY_PRIMARY_SITE);
  }

  /**
   * Test HasRole in an And with HasAncestor returns only the concepts matching both: the melanomas
   * whose primary site is Skin.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleInAndWithHasAncestorReturnsConceptsMatchingBoth() throws Exception {
    final String skinMelanomasQuery =
        """
        {"type": "And", "children": [
          {"type": "HasRole", "role": "R101", "target": "FK21"},
          {"type": "HasAncestor", "target": "FK100"}]}
        """;

    assertThat(fetchCodes(EVSTEST, skinMelanomasQuery))
        .containsExactlyInAnyOrder(
            CUTANEOUS_MELANOMA, SUPERFICIAL_SPREADING_MELANOMA, NODULAR_MELANOMA);
  }

  // ---------------------------------------------------------------------------------------------
  // evstest: inputs that aren't errors but match nothing
  // ---------------------------------------------------------------------------------------------

  /**
   * Test HasRole with a role that doesn't exist returns no concepts, not an error.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithUnknownRoleReturnsNoConcepts() throws Exception {
    final String unknownRoleQuery =
        """
        {"type": "HasRole", "role": "R999"}
        """;

    assertThat(fetchCodes(EVSTEST, unknownRoleQuery)).isEmpty();
  }

  /**
   * Test HasRole with a role name in the wrong case returns no concepts: role names are compared
   * case-sensitively.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithRoleNameInWrongCaseReturnsNoConcepts() throws Exception {
    final String wrongCaseRoleQuery =
        """
        {"type": "HasRole", "role": "disease_has_primary_anatomic_site"}
        """;

    assertThat(fetchCodes(EVSTEST, wrongCaseRoleQuery)).isEmpty();
  }

  /**
   * Test HasRole with a capitalized target name returns no concepts: a target name only matches one
   * lowercase word of the name. A known limit, listed in the spec's open questions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithCapitalizedTargetNameReturnsNoConcepts() throws Exception {
    final String capitalizedTargetQuery =
        """
        {"type": "HasRole", "role": "R101", "target": "Skin"}
        """;

    assertThat(fetchCodes(EVSTEST, capitalizedTargetQuery)).isEmpty();
  }

  /**
   * Test HasRole with an association code as the role returns no concepts: associations are
   * searched with HasAssociation. Lung Carcinoma has association A2.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithAssociationCodeReturnsNoConcepts() throws Exception {
    final String associationAsRoleQuery =
        """
        {"type": "HasRole", "role": "A2"}
        """;

    assertThat(fetchCodes(EVSTEST, associationAsRoleQuery)).isEmpty();
  }

  // ---------------------------------------------------------------------------------------------
  // Rejected
  // ---------------------------------------------------------------------------------------------

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
   * Test HasRole without a role is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithoutRoleReturnsBadRequest() throws Exception {
    final String withoutRoleQuery =
        """
        {"type": "HasRole", "target": "C12971"}
        """;

    postQuery(withoutRoleQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test HasRole with a null role is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithNullRoleReturnsBadRequest() throws Exception {
    final String nullRoleQuery =
        """
        {"type": "HasRole", "role": null, "target": "C12971"}
        """;

    postQuery(nullRoleQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test HasRole with the target given as a query instead of a name or code is rejected as a bad
   * request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithTargetGivenAsQueryReturnsBadRequest() throws Exception {
    final String targetAsQueryQuery =
        """
        {"type": "HasRole", "role": "R101", "target": {"type": "Exact", "term": "C12971"}}
        """;

    postQuery(targetAsQueryQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test HasRole with a field it doesn't have is rejected as a bad request, e.g. a role source.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithUnknownFieldReturnsBadRequest() throws Exception {
    final String unknownFieldQuery =
        """
        {"type": "HasRole", "role": "R101", "source": "NCI"}
        """;

    postQuery(unknownFieldQuery).andExpect(status().isBadRequest());
  }
}
