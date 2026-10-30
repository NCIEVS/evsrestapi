package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EvsQuery}'s check that every terminology it searches has a terminology and
 * version. No OpenSearch or Spring: the check runs when a node is built.
 */
@DemonstratesFieldMatch
public class EvsQueryTest {

  /** Terminology of the made-up terminologies. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the made-up terminologies. */
  private static final String VERSION = "25.12e";

  /** Index name of the made-up terminologies. */
  private static final String INDEX_NAME = "concept_ncit_2512e";

  /** A complete terminology builds a node without failing. */
  @Test
  public void testCompleteTerminologyBuildsNode() {
    final Terminology terminology =
        QueryTestTerminologies.getTerminology(TERMINOLOGY, VERSION, INDEX_NAME);

    new FieldMatch(List.of(terminology), "code", new Exact("C12470"));
  }

  /** A terminology without its terminology name fails when the node is built, naming the field. */
  @Test
  public void testTerminologyWithoutTerminologyThrowsErrorNamingIt() {
    final Terminology terminology =
        QueryTestTerminologies.getTerminology(null, VERSION, INDEX_NAME);

    assertMatchFailsNaming(terminology, "terminology");
  }

  /** A terminology without a version fails when the node is built, naming the field. */
  @Test
  public void testTerminologyWithoutVersionThrowsErrorNamingIt() {
    final Terminology terminology =
        QueryTestTerminologies.getTerminology(TERMINOLOGY, null, INDEX_NAME);

    assertMatchFailsNaming(terminology, "version");
  }

  /**
   * Asserts that building a node over the terminology fails, and that the error names the missing
   * field.
   *
   * @param terminology the incomplete terminology
   * @param missingFieldName the name of the missing field
   */
  private static void assertMatchFailsNaming(
      final Terminology terminology, final String missingFieldName) {
    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> new FieldMatch(List.of(terminology), "code", new Exact("C12470")));

    assertTrue(
        error.getMessage().startsWith("Terminology has no " + missingFieldName + " "),
        error.getMessage());
  }
}
