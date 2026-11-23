package gov.nih.nci.evs.api.queryendpoint;

import static org.assertj.core.api.Assertions.assertThat;

import gov.nih.nci.evs.api.query.HasSynonym;
import org.junit.jupiter.api.Test;

/**
 * Tests the HasSynonym node, which matches concepts with a synonym matching the given name, source,
 * and term type, through the query endpoint.
 */
public class HasSynonymEndpointTest extends AbstractQueryEndpointTest {

  /** The NCI preferred term "melanoma". */
  private static final String MELANOMA_PREFERRED_TERM_QUERY =
      """
      {"type": "HasSynonym", "target": "melanoma", "source": "NCI", "termType": "PT"}
      """;

  /**
   * Test HasSynonym returns the concepts with a matching synonym: the NCI preferred term
   * "melanoma".
   *
   * @throws Exception the exception
   */
  @Test
  public void testHasSynonymReturnsMatchingConcepts() throws Exception {
    assertThat(fetchCodes(MELANOMA_PREFERRED_TERM_QUERY)).containsExactly(MELANOMA);
  }

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
