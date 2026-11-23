package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.query.Typeahead;
import org.junit.jupiter.api.Test;

/**
 * Tests the Typeahead node, which completes an incomplete term to the best matching concepts,
 * through the query endpoint.
 */
public class TypeaheadEndpointTest extends AbstractQueryEndpointTest {

  /** The typeahead for the term "melanoma". */
  private static final String MELANOMA_TYPEAHEAD_QUERY =
      """
      {"type": "Typeahead", "term": "melanoma"}
      """;

  /**
   * Test Typeahead returns Melanoma first for "melanoma".
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadReturnsBestMatchFirst() throws Exception {
    assertThat(fetchCodes(MELANOMA_TYPEAHEAD_QUERY)).first().isEqualTo(MELANOMA);
  }

  /**
   * Test Typeahead with a misspelled property (incompleteTerm, not term) is rejected as a bad
   * request.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTypeaheadWithMisspelledPropertyReturnsBadRequest() throws Exception {
    final String incompleteTermInsteadOfTermQuery =
        """
        {"type": "Typeahead", "incompleteTerm": "melanoma"}
        """;

    postQuery(incompleteTermInsteadOfTermQuery).andExpect(status().isBadRequest());
  }

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
