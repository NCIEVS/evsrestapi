package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.HasSynonym;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link HasSynonym}. */
public class HasSynonymUnitTest {

  /** The NCI preferred term "melanoma". */
  private static final String MELANOMA_PREFERRED_TERM_QUERY =
      """
      {"type": "HasSynonym", "target": "melanoma", "source": "NCI", "termType": "PT"}
      """;

  /**
   * Test HasSynonym round trips through Jackson unchanged.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasSynonymRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(MELANOMA_PREFERRED_TERM_QUERY);
  }

  /**
   * Test HasSynonym serializes to the expected JSON, the wire format clients use.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasSynonymSerializesToExpectedJson() throws Exception {
    final HasSynonym melanomaPreferredTerm = new HasSynonym("melanoma", "NCI", "PT");

    assertSerializesTo(melanomaPreferredTerm, MELANOMA_PREFERRED_TERM_QUERY);
  }
}
