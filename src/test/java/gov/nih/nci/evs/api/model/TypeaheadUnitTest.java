package gov.nih.nci.evs.api.model;

import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertRoundTripsUnchanged;
import static gov.nih.nci.evs.api.EvsQuerySerializationTester.assertSerializesTo;

import gov.nih.nci.evs.api.query.Typeahead;
import org.junit.jupiter.api.Test;

/** Unit test for the JSON wire format of {@link Typeahead}. */
public class TypeaheadUnitTest {

  /** The typeahead for the term "melanoma". */
  private static final String MELANOMA_TYPEAHEAD_QUERY =
      """
      {"type": "Typeahead", "term": "melanoma"}
      """;

  /**
   * Test Typeahead round trips through Jackson unchanged.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadRoundTripsUnchanged() throws Exception {
    assertRoundTripsUnchanged(MELANOMA_TYPEAHEAD_QUERY);
  }

  /**
   * Test Typeahead serializes to the expected JSON, the wire format clients use.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadSerializesToExpectedJson() throws Exception {
    final Typeahead melanomaTypeahead = new Typeahead("melanoma");

    assertSerializesTo(melanomaTypeahead, MELANOMA_TYPEAHEAD_QUERY);
  }
}
