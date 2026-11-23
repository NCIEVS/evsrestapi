package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.query.And;
import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.query.HasAncestor;
import gov.nih.nci.evs.api.query.HasAncestorOrIs;
import gov.nih.nci.evs.api.query.Not;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests Not, which leaves out the concepts its child matches, through the query endpoint. */
public class NotEndpointTest extends AbstractQueryEndpointTest {

  /** Breast and its children, without the children of Breast. */
  private static final String BREAST_WITHOUT_CHILDREN_QUERY =
      """
      {"type": "And", "children": [
        {"type": "HasAncestorOrIs", "target": "C12971", "maxLevel": 1},
        {"type": "Not", "child":
          {"type": "HasAncestor", "target": "C12971", "maxLevel": 1}}]}
      """;

  /**
   * Test Not leaves out the concepts its child matches: Breast and its children, without its
   * children, is Breast alone.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNotLeavesOutConceptsItsChildMatches() throws Exception {
    assertThat(fetchCodes(BREAST_WITHOUT_CHILDREN_QUERY)).containsExactly(BREAST);
  }

  /**
   * Test Not with a child given as a type name, not a query, is rejected as a bad request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNotWithChildGivenAsTypeNameReturnsBadRequest() throws Exception {
    final String childAsTypeNameQuery =
        """
        {"type": "Not", "child": "HasRole"}
        """;

    postQuery(childAsTypeNameQuery).andExpect(status().isBadRequest());
  }

  /**
   * Test Not round trips unchanged through Jackson.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNotRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(BREAST_WITHOUT_CHILDREN_QUERY);
  }

  /**
   * Test Not serializes to the expected JSON when built in Java: the Not of the children of Breast,
   * within an And with Breast and its children.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNotSerializesToExpectedJson() throws Exception {
    final EvsQuery breastWithoutChildren =
        new And(
            List.of(
                new HasAncestorOrIs(BREAST, DIRECT_RELATIVES_MAX_LEVEL),
                new Not(new HasAncestor(BREAST, DIRECT_RELATIVES_MAX_LEVEL))));

    assertSerializesTo(breastWithoutChildren, BREAST_WITHOUT_CHILDREN_QUERY);
  }
}
