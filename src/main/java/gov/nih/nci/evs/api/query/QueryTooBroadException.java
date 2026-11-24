package gov.nih.nci.evs.api.query;

/**
 * Thrown when a query is too big to run: it matches more codes than one {@code terms} query
 * accepts, holds more clauses than OpenSearch allows, or nests too deeply. The client can narrow
 * the query, so the query endpoint reports it as a bad request.
 */
public class QueryTooBroadException extends IllegalArgumentException {

  /** The serial version UID. */
  private static final long serialVersionUID = 1L;

  /**
   * Instantiates a {@link QueryTooBroadException}.
   *
   * @param message what was too broad
   */
  public QueryTooBroadException(final String message) {
    super(message);
  }

  /**
   * Instantiates a {@link QueryTooBroadException} for a limit OpenSearch reported.
   *
   * @param message what was too broad
   * @param cause the error OpenSearch reported
   */
  public QueryTooBroadException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
