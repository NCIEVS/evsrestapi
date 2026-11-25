package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests how the OpenSearch service recognizes a search that failed because it broke one of
 * OpenSearch's limits, so it can report it to the client as a bad request with OpenSearch's reason.
 * The client wraps OpenSearch's reason in its own exceptions, so it may be on any exception in the
 * chain.
 */
public class OpenSearchLimitReasonTest {

  /** OpenSearch's reason for a query with too many clauses across its nested queries. */
  private static final String TOO_MANY_CLAUSES_REASON =
      "OpenSearch exception [type=too_many_nested_clauses, reason=Query contains too many nested"
          + " clauses; maxClauseCount is set to 1024]";

  /** OpenSearch's reason for paging past an index's result window. */
  private static final String RESULT_WINDOW_REASON =
      "OpenSearch exception [type=illegal_argument_exception, reason=Result window is too large,"
          + " from + size must be less than or equal to: [10000] but was [10010]]";

  /** The client's own message, which says the search failed but not why. */
  private static final String CLIENT_MESSAGE =
      "OpenSearch exception [type=search_phase_execution_exception, reason=all shards failed]";

  /** A clause limit reason on a cause of the client's exception is found. */
  @Test
  public void testTooManyClausesReasonOnCauseIsFound() {
    final RuntimeException error =
        new RuntimeException(CLIENT_MESSAGE, new RuntimeException(TOO_MANY_CLAUSES_REASON));

    assertEquals(TOO_MANY_CLAUSES_REASON, OpenSearchServiceImpl.getOpenSearchLimitReason(error));
  }

  /** A limit reason on a suppressed exception of the client's exception is found. */
  @Test
  public void testReasonOnSuppressedExceptionIsFound() {
    final RuntimeException error = new RuntimeException(CLIENT_MESSAGE);
    error.addSuppressed(new RuntimeException(TOO_MANY_CLAUSES_REASON));

    assertEquals(TOO_MANY_CLAUSES_REASON, OpenSearchServiceImpl.getOpenSearchLimitReason(error));
  }

  /** A result window reason is found. */
  @Test
  public void testResultWindowReasonIsFound() {
    final RuntimeException error =
        new RuntimeException(CLIENT_MESSAGE, new RuntimeException(RESULT_WINDOW_REASON));

    assertEquals(RESULT_WINDOW_REASON, OpenSearchServiceImpl.getOpenSearchLimitReason(error));
  }

  /** An error that isn't about a limit has no limit reason, so it stays a server error. */
  @Test
  public void testErrorWithoutLimitReasonHasNoReason() {
    final RuntimeException error =
        new RuntimeException(CLIENT_MESSAGE, new RuntimeException("connection refused"));

    assertNull(OpenSearchServiceImpl.getOpenSearchLimitReason(error));
  }
}
