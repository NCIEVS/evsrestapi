package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests the HasSynonym node, which matches concepts with a synonym matching the given name, source,
 * and term type, through the query endpoint.
 */
public class SearchControllerHasSynonymTests extends AbstractSearchControllerQueryTests {

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
}
