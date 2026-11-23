package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import gov.nih.nci.evs.api.query.EvsQuery;
import gov.nih.nci.evs.api.support.es.OpenSearchLimits;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests that every limit below the query endpoint reaches the client as a bad request that says
 * which limit the query broke. A limit of a lower layer (OpenSearch, the JSON parser, the query
 * tree) limits the endpoint too, so each one is part of the endpoint's contract.
 *
 * <p>Each limit's exact edge is tested where the limit is owned (see {@code
 * SearchCriteriaUnitTest}, {@code EvsQueryTest}, {@code OpenSearchLimitReasonTest}, {@code
 * HasHierarchyRelativeLimitsTest}); these tests check the error the client gets. They go through
 * real HTTP rather than MockMvc, because the error message is added by the error controller, which
 * MockMvc skips.
 *
 * <p>Runs against the test OpenSearch with NCIt loaded.
 */
public class SearchControllerQueryLimitsTests extends AbstractSearchControllerQueryTests {

  /** A query matching a few NCIt concepts: those whose primary anatomic site is Breast. */
  private static final String LEAF_QUERY =
      "{\"type\": \"HasRole\", \"role\": \"R101\", \"target\": \"C12971\"}";

  /** The first record of a search, for tests that aren't about paging. */
  private static final int FIRST_RECORD = 0;

  /** The page size for tests that aren't about paging. */
  private static final int SMALL_PAGE_SIZE = 10;

  /** How deep the JSON parser lets a request body nest, by default. */
  private static final int JSON_MAX_NESTING_DEPTH = 1_000;

  // ---------------------------------------------------------------------------------------------
  // Result window: fromRecord + pageSize ≤ OpenSearchLimits.MAX_RESULT_WINDOW
  // ---------------------------------------------------------------------------------------------

  /** A page ending exactly at the result window is searched. */
  @Test
  public void testPageEndingAtResultWindowReturnsOk() {
    final int fromRecord = OpenSearchLimits.MAX_RESULT_WINDOW - SMALL_PAGE_SIZE;

    final ResponseEntity<String> response = exchangeQuery(LEAF_QUERY, fromRecord, SMALL_PAGE_SIZE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  /** A page ending past the result window is rejected as a bad request. */
  @Test
  public void testPageEndingPastResultWindowReturnsBadRequest() {
    final ResponseEntity<String> response = exchangePastResultWindow();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** A page ending past the result window gets an error naming the window. */
  @Test
  public void testPageEndingPastResultWindowErrorNamesLimit() {
    final String message = getErrorMessage(exchangePastResultWindow());

    assertThat(message).contains(String.valueOf(OpenSearchLimits.MAX_RESULT_WINDOW));
  }

  // ---------------------------------------------------------------------------------------------
  // Clauses: each And or Or can hold at most OpenSearchLimits.MAX_CLAUSE_COUNT children, translated
  // from OpenSearch. The limit counts per And or Or, not across the whole tree.
  // ---------------------------------------------------------------------------------------------

  /**
   * Test an Or with exactly as many children as OpenSearch allows clauses in one bool query is
   * searched.
   */
  @Test
  public void testOrWithMaxClauseCountChildrenReturnsOk() {
    final ResponseEntity<String> response =
        exchangeOrWithChildren(OpenSearchLimits.MAX_CLAUSE_COUNT);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  /**
   * Test the clause limit counts each Or separately: two nested Ors, each at the limit, hold twice
   * as many leaves as the limit and are still searched. OpenSearch 1.x (Lucene 8) counts clauses
   * per boolean query; Lucene 9 counts them across the whole query, so this fails if an OpenSearch
   * upgrade changes the limit to a total.
   */
  @Test
  public void testNestedOrsEachAtMaxClauseCountReturnOk() {
    final String orAtMaxClauseCount = getOrWithChildren(OpenSearchLimits.MAX_CLAUSE_COUNT);
    final String orOfOrsAtMaxClauseCount =
        "{\"type\": \"Or\", \"children\": ["
            + orAtMaxClauseCount
            + ", "
            + orAtMaxClauseCount
            + "]}";

    final ResponseEntity<String> response =
        exchangeQuery(orOfOrsAtMaxClauseCount, FIRST_RECORD, SMALL_PAGE_SIZE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  /** An Or with more children than OpenSearch allows clauses is rejected as a bad request. */
  @Test
  public void testOrWithTooManyChildrenReturnsBadRequest() {
    final ResponseEntity<String> response = exchangeTooManyClauses();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** An Or with too many children gets an error naming OpenSearch's clause limit. */
  @Test
  public void testOrWithTooManyChildrenErrorNamesLimit() {
    final String message = getErrorMessage(exchangeTooManyClauses());

    assertThat(message).contains(String.valueOf(OpenSearchLimits.MAX_CLAUSE_COUNT));
  }

  // ---------------------------------------------------------------------------------------------
  // Query depth: a query tree can nest at most EvsQuery.MAX_QUERY_DEPTH levels
  // ---------------------------------------------------------------------------------------------

  /** A query exactly at the depth limit is searched. */
  @Test
  public void testQueryAtMaxDepthReturnsOk() {
    final String atMaxDepthQuery = getNotsAroundLeaf(EvsQuery.MAX_QUERY_DEPTH);

    final ResponseEntity<String> response =
        exchangeQuery(atMaxDepthQuery, FIRST_RECORD, SMALL_PAGE_SIZE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  /** A query one level past the depth limit is rejected as a bad request. */
  @Test
  public void testQueryPastMaxDepthReturnsBadRequest() {
    final ResponseEntity<String> response = exchangePastMaxDepth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** A query past the depth limit gets an error naming the limit. */
  @Test
  public void testQueryPastMaxDepthErrorNamesLimit() {
    final String message = getErrorMessage(exchangePastMaxDepth());

    assertThat(message).contains("at most " + EvsQuery.MAX_QUERY_DEPTH);
  }

  // ---------------------------------------------------------------------------------------------
  // JSON nesting: the parser stops before the query depth check can run
  // ---------------------------------------------------------------------------------------------

  /**
   * Test a body nested exactly to the JSON parser's limit gets past the parser: it's rejected for
   * the query depth limit, which is stricter, not for JSON nesting.
   */
  @Test
  public void testBodyAtJsonNestingLimitPassesParser() {
    final String atJsonNestingLimitQuery = getNotsAroundLeaf(JSON_MAX_NESTING_DEPTH);

    final String message =
        getErrorMessage(exchangeQuery(atJsonNestingLimitQuery, FIRST_RECORD, SMALL_PAGE_SIZE));

    assertThat(message).contains("at most " + EvsQuery.MAX_QUERY_DEPTH).doesNotContain("nesting");
  }

  /** A body nested past the JSON parser's limit is rejected as a bad request. */
  @Test
  public void testBodyPastJsonNestingLimitReturnsBadRequest() {
    final ResponseEntity<String> response = exchangePastJsonNestingLimit();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /** A body nested past the JSON parser's limit gets an error naming the limit. */
  @Test
  public void testBodyPastJsonNestingLimitErrorNamesLimit() {
    final String message = getErrorMessage(exchangePastJsonNestingLimit());

    assertThat(message).contains(String.valueOf(JSON_MAX_NESTING_DEPTH));
  }

  // ---------------------------------------------------------------------------------------------
  // Validation failures: the error names the parameter or body field that failed
  // ---------------------------------------------------------------------------------------------

  /** A page size out of range gets an error naming the parameter. */
  @Test
  public void testPageSizeOutOfRangeErrorNamesParameter() {
    final int pageSizeOutOfRange = 0;

    final String message =
        getErrorMessage(exchangeQuery(LEAF_QUERY, FIRST_RECORD, pageSizeOutOfRange));

    assertThat(message).contains("pageSize");
  }

  /** A HasRole without a role gets an error naming the role field. */
  @Test
  public void testMissingRoleErrorNamesField() {
    final String withoutRoleQuery = "{\"type\": \"HasRole\", \"target\": \"C12971\"}";

    final String message =
        getErrorMessage(exchangeQuery(withoutRoleQuery, FIRST_RECORD, SMALL_PAGE_SIZE));

    assertThat(message).contains("role");
  }

  /** A HasRole without a role inside an And gets an error naming the field's path in the body. */
  @Test
  public void testMissingRoleInChildErrorNamesFieldPath() {
    final String childWithoutRoleQuery =
        "{\"type\": \"And\", \"children\": [{\"type\": \"HasRole\", \"target\": \"C12971\"}]}";

    final String message =
        getErrorMessage(exchangeQuery(childWithoutRoleQuery, FIRST_RECORD, SMALL_PAGE_SIZE));

    assertThat(message).contains("children[0].role");
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  /**
   * Posts a page that ends one record past the result window.
   *
   * @return the response
   */
  private ResponseEntity<String> exchangePastResultWindow() {
    final int fromRecord = OpenSearchLimits.MAX_RESULT_WINDOW - SMALL_PAGE_SIZE + 1;
    return exchangeQuery(LEAF_QUERY, fromRecord, SMALL_PAGE_SIZE);
  }

  /**
   * Posts an Or with one more child than OpenSearch allows clauses. Each child also builds clauses
   * of its own, so the query is well past the limit.
   *
   * @return the response
   */
  private ResponseEntity<String> exchangeTooManyClauses() {
    return exchangeOrWithChildren(OpenSearchLimits.MAX_CLAUSE_COUNT + 1);
  }

  /**
   * Posts an Or with the given number of children, each the leaf query.
   *
   * @param childCount the number of children
   * @return the response
   */
  private ResponseEntity<String> exchangeOrWithChildren(final int childCount) {
    return exchangeQuery(getOrWithChildren(childCount), FIRST_RECORD, SMALL_PAGE_SIZE);
  }

  /**
   * Returns an Or query with the given number of copies of the leaf query as children.
   *
   * @param childCount the number of children
   * @return the Or query as JSON
   */
  private static String getOrWithChildren(final int childCount) {
    final List<String> children = Collections.nCopies(childCount, LEAF_QUERY);
    return "{\"type\": \"Or\", \"children\": [" + String.join(", ", children) + "]}";
  }

  /**
   * Posts a query one level past the depth limit.
   *
   * @return the response
   */
  private ResponseEntity<String> exchangePastMaxDepth() {
    final String pastMaxDepthQuery = getNotsAroundLeaf(EvsQuery.MAX_QUERY_DEPTH + 1);
    return exchangeQuery(pastMaxDepthQuery, FIRST_RECORD, SMALL_PAGE_SIZE);
  }

  /**
   * Posts a query whose JSON nests one level past the parser's limit: each Not is one more object.
   *
   * @return the response
   */
  private ResponseEntity<String> exchangePastJsonNestingLimit() {
    final String pastJsonNestingLimitQuery = getNotsAroundLeaf(JSON_MAX_NESTING_DEPTH + 1);
    return exchangeQuery(pastJsonNestingLimitQuery, FIRST_RECORD, SMALL_PAGE_SIZE);
  }

  /**
   * Returns the JSON of the leaf query wrapped in Nots until the query is the given depth.
   *
   * @param depth the depth, at least 1
   * @return the query JSON
   */
  private static String getNotsAroundLeaf(final int depth) {
    final int notCount = depth - 1;
    final String notOpenings = "{\"type\": \"Not\", \"child\": ".repeat(notCount);
    final String notClosings = "}".repeat(notCount);
    return notOpenings + LEAF_QUERY + notClosings;
  }
}
