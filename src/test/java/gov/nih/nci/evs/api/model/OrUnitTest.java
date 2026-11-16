package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.query.HasAncestor;
import gov.nih.nci.evs.api.query.HasDescendant;
import gov.nih.nci.evs.api.query.Or;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link Or}. */
public class OrUnitTest {

  /** The children of Breast or the parent of Female Breast, combined with Or. */
  private static final String CHILDREN_OR_PARENT_QUERY =
      """
      {"type": "Or", "children": [
        {"type": "HasAncestor", "target": "C12971", "maxLevel": 1},
        {"type": "HasDescendant", "target": "C12851", "maxLevel": 1}]}
      """;

  /** Breast, the target of most hierarchy tests. */
  private static final String BREAST = "C12971";

  /** The max level that limits hierarchy queries to direct children or parents. */
  private static final int DIRECT_RELATIVES_MAX_LEVEL = 1;

  /** Female Breast, whose only parent is Breast. */
  private static final String FEMALE_BREAST = "C12851";

  /**
   * Test Or round trips unchanged through Jackson.
   *
   * @throws Exception the exception
   */
  @Test
  public void testOrRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(CHILDREN_OR_PARENT_QUERY);
  }

  /**
   * Test Or serializes to the expected JSON when built in Java: the Or of the children of Breast
   * and the parent of Female Breast.
   *
   * @throws Exception the exception
   */
  @Test
  public void testOrSerializesToExpectedJson() throws Exception {
    final EvsQuery childrenOrParent =
        new Or(
            List.of(
                new HasAncestor(BREAST, DIRECT_RELATIVES_MAX_LEVEL),
                new HasDescendant(FEMALE_BREAST, DIRECT_RELATIVES_MAX_LEVEL)));

    assertSerializesTo(childrenOrParent, CHILDREN_OR_PARENT_QUERY);
  }
}
