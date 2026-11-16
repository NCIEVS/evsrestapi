package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.And;
import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.query.HasAncestor;
import gov.nih.nci.evs.api.query.HasAncestorOrIs;
import gov.nih.nci.evs.api.query.Not;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link Not}. */
public class NotUnitTest {

  /** Breast and its children, without the children of Breast. */
  private static final String BREAST_WITHOUT_CHILDREN_QUERY =
      """
      {"type": "And", "children": [
        {"type": "HasAncestorOrIs", "target": "C12971", "maxLevel": 1},
        {"type": "Not", "child":
          {"type": "HasAncestor", "target": "C12971", "maxLevel": 1}}]}
      """;

  /** Breast, the target of most hierarchy tests. */
  private static final String BREAST = "C12971";

  /** The max level that limits hierarchy queries to direct children or parents. */
  private static final int DIRECT_RELATIVES_MAX_LEVEL = 1;

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
