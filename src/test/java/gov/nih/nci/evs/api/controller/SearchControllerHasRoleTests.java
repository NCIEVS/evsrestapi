package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Role;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the HasRole query node, which matches the concepts with a role to a target concept, through
 * the query endpoint.
 *
 * <p>The tests check what holds as the terminology's data changes: results contain a known concept,
 * every result has the role, names find what codes find, and leaving out the target never finds
 * fewer. The terminology, role, and target they search are the constants below.
 */
public class SearchControllerHasRoleTests extends AbstractSearchControllerQueryTests {

  /** The terminology searched. */
  private static final String SEARCHED_TERMINOLOGY = "ncit";

  /** The role's code: Disease_Has_Primary_Anatomic_Site. */
  private static final String ROLE_CODE = "R101";

  /** The role's name, as the terminology spells it. */
  private static final String ROLE_NAME = "Disease_Has_Primary_Anatomic_Site";

  /** The role's name in the wrong case. */
  private static final String ROLE_NAME_IN_WRONG_CASE = "disease_has_primary_anatomic_site";

  /** The target's code: Breast. */
  private static final String TARGET_CODE = "C12971";

  /** The target's name, one lowercase word of it. */
  private static final String TARGET_NAME = "breast";

  /** A concept with the role to the target: Breast Carcinoma. */
  private static final String CONCEPT_WITH_ROLE_TO_TARGET = "C4872";

  /** An association code of the terminology, which isn't a role: Concept_In_Subset. */
  private static final String ASSOCIATION_CODE = "A8";

  /** A role code the terminology doesn't have. */
  private static final String UNKNOWN_ROLE_CODE = "R999";

  /** The include value that returns each concept's roles. */
  private static final String ROLES_INCLUDE = "roles";

  /** The query for the concepts with the role to the target, by codes. */
  private static final String BY_CODE_QUERY = getHasRoleQuery(ROLE_CODE, TARGET_CODE);

  // ---------------------------------------------------------------------------------------------
  // role and target: a role of the terminology, by code or name; target optional
  // ---------------------------------------------------------------------------------------------

  /**
   * Test HasRole by role code and target code returns a known concept with that role to that
   * target, and that every concept returned has it.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleByCodeReturnsConceptsWithRoleToTarget() throws Exception {
    final List<Concept> concepts =
        fetchQueryResults(SEARCHED_TERMINOLOGY, BY_CODE_QUERY, ROLES_INCLUDE).getConcepts();

    assertThat(concepts).extracting(Concept::getCode).contains(CONCEPT_WITH_ROLE_TO_TARGET);
    assertThat(concepts).allMatch(SearchControllerHasRoleTests::getHasRoleToTarget);
  }

  /**
   * Test HasRole by role name and a one-word lowercase target name returns every concept found by
   * codes. It can return more: a target name matches any one word of a target's name, e.g. "breast"
   * also matches Male Breast.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleByNameReturnsEveryConceptFoundByCode() throws Exception {
    final String byNameQuery = getHasRoleQuery(ROLE_NAME, TARGET_NAME);

    final List<String> byNameCodes = fetchCodes(SEARCHED_TERMINOLOGY, byNameQuery);
    final List<String> byCodeCodes = fetchCodes(SEARCHED_TERMINOLOGY, BY_CODE_QUERY);

    assertThat(byCodeCodes).isNotEmpty();
    assertThat(byNameCodes).containsAll(byCodeCodes);
  }

  /**
   * Test HasRole without a target returns at least as many concepts as with one: the target only
   * narrows the role.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithoutTargetReturnsEveryConceptWithRole() throws Exception {
    final String withoutTargetQuery = getHasRoleQuery(ROLE_CODE, null);

    final long withoutTargetTotal =
        fetchQueryResults(SEARCHED_TERMINOLOGY, withoutTargetQuery, ROLES_INCLUDE).getTotal();
    final long withTargetTotal =
        fetchQueryResults(SEARCHED_TERMINOLOGY, BY_CODE_QUERY, ROLES_INCLUDE).getTotal();

    assertThat(withTargetTotal).isPositive();
    assertThat(withoutTargetTotal).isGreaterThanOrEqualTo(withTargetTotal);
  }

  // ---------------------------------------------------------------------------------------------
  // role values that aren't errors but match nothing
  // ---------------------------------------------------------------------------------------------

  /**
   * Test HasRole with a role that doesn't exist returns no concepts, not an error.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithUnknownRoleReturnsNoConcepts() throws Exception {
    final String unknownRoleQuery = getHasRoleQuery(UNKNOWN_ROLE_CODE, null);

    assertThat(fetchCodes(SEARCHED_TERMINOLOGY, unknownRoleQuery)).isEmpty();
  }

  /**
   * Test HasRole with a role name in the wrong case returns no concepts: role names are compared
   * case-sensitively.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithRoleNameInWrongCaseReturnsNoConcepts() throws Exception {
    final String wrongCaseRoleQuery = getHasRoleQuery(ROLE_NAME_IN_WRONG_CASE, null);

    assertThat(fetchCodes(SEARCHED_TERMINOLOGY, wrongCaseRoleQuery)).isEmpty();
  }

  /**
   * Test HasRole with an association code as the role returns no concepts: associations are
   * searched with HasAssociation.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasRoleWithAssociationCodeReturnsNoConcepts() throws Exception {
    final String associationAsRoleQuery = getHasRoleQuery(ASSOCIATION_CODE, null);

    assertThat(fetchCodes(SEARCHED_TERMINOLOGY, associationAsRoleQuery)).isEmpty();
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

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  /**
   * Returns a HasRole query as JSON.
   *
   * @param role the role code or name
   * @param target the target code or name, or null for any target
   * @return the query JSON
   */
  private static String getHasRoleQuery(final String role, final String target) {
    final boolean hasTarget = target != null;
    final String targetField = hasTarget ? ", \"target\": \"" + target + "\"" : "";
    return "{\"type\": \"HasRole\", \"role\": \"" + role + "\"" + targetField + "}";
  }

  /**
   * Returns whether the concept has the role to the target.
   *
   * @param concept the concept, with its roles
   * @return whether it has the role to the target
   */
  private static boolean getHasRoleToTarget(final Concept concept) {
    return concept.getRoles().stream().anyMatch(SearchControllerHasRoleTests::getIsRoleToTarget);
  }

  /**
   * Returns whether the role is the searched role, pointing to the target.
   *
   * @param role the role
   * @return whether it's the role to the target
   */
  private static boolean getIsRoleToTarget(final Role role) {
    return ROLE_CODE.equals(role.getCode()) && TARGET_CODE.equals(role.getRelatedCode());
  }
}
