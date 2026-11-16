package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.And;
import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.query.HasAncestor;
import gov.nih.nci.evs.api.query.StartsWith;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link And}. */
public class AndUnitTest {

  /** The children of Breast with a name or synonym starting "left", combined with And. */
  private static final String LEFT_CHILD_OF_BREAST_QUERY =
      """
      {"type": "And", "children": [
        {"type": "HasAncestor", "target": "C12971", "maxLevel": 1},
        {"type": "StartsWith", "term": "left"}]}
      """;

  /** Breast, the target of most hierarchy tests. */
  private static final String BREAST = "C12971";

  /** The max level that limits hierarchy queries to direct children or parents. */
  private static final int DIRECT_RELATIVES_MAX_LEVEL = 1;

  /**
   * Test And round trips unchanged through Jackson.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAndRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(LEFT_CHILD_OF_BREAST_QUERY);
  }

  /**
   * Test And serializes to the expected JSON when built in Java: the And of the children of Breast
   * and the names or synonyms starting "left".
   *
   * @throws Exception the exception
   */
  @Test
  public void testAndSerializesToExpectedJson() throws Exception {
    final EvsQuery leftChildOfBreast =
        new And(
            List.of(new HasAncestor(BREAST, DIRECT_RELATIVES_MAX_LEVEL), new StartsWith("left")));

    assertSerializesTo(leftChildOfBreast, LEFT_CHILD_OF_BREAST_QUERY);
  }
}
