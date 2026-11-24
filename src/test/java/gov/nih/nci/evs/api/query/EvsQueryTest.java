package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Unit tests for {@link EvsQuery}'s check that every terminology it searches has a terminology and
 * version. No OpenSearch or Spring: the check runs when a node is run, before it searches, so a
 * mocked {@link OpenSearchOperations} is enough.
 */
public class EvsQueryTest {

  /** Terminology of the made-up terminologies. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the made-up terminologies. */
  private static final String VERSION = "25.12e";

  /** Index name of the made-up terminologies. */
  private static final String INDEX_NAME = "concept_ncit_2512e";

  /** The page of results asked for; the mock ignores it. */
  private static final Pageable FIRST_PAGE = PageRequest.of(0, 10);

  /** The fields returned for each concept; the mock ignores them. */
  private static final IncludeParam MINIMAL_FIELDS = new IncludeParam("minimal");

  /** A complete terminology runs a node without failing. */
  @Test
  public void testCompleteTerminologyRunsNode() {
    final Terminology terminology =
        QueryTestTerminologies.getTerminology(TERMINOLOGY, VERSION, INDEX_NAME);
    final OpenSearchOperations operations = mock(OpenSearchOperations.class);
    final SearchHits<Concept> noHits = mock(SearchHits.class);
    when(noHits.getSearchHits()).thenReturn(List.of());
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenReturn(noHits);
    final EvsQuery conceptsWithSkinCode = new Exact("C12470");

    assertDoesNotThrow(
        () ->
            conceptsWithSkinCode.fetchConcepts(
                operations, List.of(terminology), FIRST_PAGE, MINIMAL_FIELDS));
  }

  /** A terminology without its terminology name fails when the node is run, naming the field. */
  @Test
  public void testTerminologyWithoutTerminologyThrowsErrorNamingIt() {
    final Terminology terminology =
        QueryTestTerminologies.getTerminology(null, VERSION, INDEX_NAME);

    assertMatchFailsNaming(terminology, "terminology");
  }

  /** A terminology without a version fails when the node is run, naming the field. */
  @Test
  public void testTerminologyWithoutVersionThrowsErrorNamingIt() {
    final Terminology terminology =
        QueryTestTerminologies.getTerminology(TERMINOLOGY, null, INDEX_NAME);

    assertMatchFailsNaming(terminology, "version");
  }

  // ---------------------------------------------------------------------------------------------
  // Depth: a query can nest at most EvsQuery.MAX_QUERY_DEPTH levels
  // ---------------------------------------------------------------------------------------------

  /** A leaf holds no other query, so its depth is 1. */
  @Test
  public void testLeafHasDepthOne() {
    final int leafDepth = 1;

    assertEquals(leafDepth, new Exact("C12470").getDepth());
  }

  /** A node's depth is one more than its deepest child's. */
  @Test
  public void testAndHasDepthOfDeepestChildPlusOne() {
    final EvsQuery skinAndNotSkin =
        new And(List.of(new Exact("C12470"), new Not(new Exact("C12470"))));
    final int depthOfNotAndExactPlusAnd = 3;

    assertEquals(depthOfNotAndExactPlusAnd, skinAndNotSkin.getDepth());
  }

  /**
   * Returns a leaf wrapped in Nots until the query is the given depth.
   *
   * @param depth the depth, at least 1
   * @return the query
   */
  private static EvsQuery getNotsAroundLeaf(final int depth) {
    EvsQuery query = new Exact("C12470");
    for (int level = 1; level < depth; level++) {
      query = new Not(query);
    }
    return query;
  }

  /**
   * Asserts that running a node over the terminology fails, and that the error names the missing
   * field.
   *
   * @param terminology the incomplete terminology
   * @param missingFieldName the name of the missing field
   */
  private static void assertMatchFailsNaming(
      final Terminology terminology, final String missingFieldName) {
    final OpenSearchOperations operations = mock(OpenSearchOperations.class);
    final EvsQuery conceptsWithSkinCode = new Exact("C12470");
    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                conceptsWithSkinCode.fetchConcepts(
                    operations, List.of(terminology), FIRST_PAGE, MINIMAL_FIELDS));

    assertTrue(
        error.getMessage().startsWith("Terminology has no " + missingFieldName + " "),
        error.getMessage());
  }
}
