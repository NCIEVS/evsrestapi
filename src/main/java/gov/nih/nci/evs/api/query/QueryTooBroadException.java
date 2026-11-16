package gov.nih.nci.evs.api.query;

/**
 * Thrown when a query matches too much to run, e.g. more codes than one {@code terms} query
 * accepts. The client can narrow the query, so the query endpoint reports it as a bad request.
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
}
