package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import java.util.Objects;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;

/**
 * A leaf query: matches concepts whose field matches the value match, e.g. concepts whose {@code
 * code} is exactly "C12470". The {@link ValueMatch} says how the value is compared; this node only
 * says which field it's compared against.
 *
 * <p>For developers building new query nodes only: a node uses it inside {@code
 * fetchOpenSearchQuery} to match the fields it owns. It is not part of the API. API users can't
 * name it outside this package, and tests may only use it inside a class, method, or field marked
 * {@code @DemonstratesFieldMatch} (enforced by {@code FieldMatchUsageSourceCodeTest}), so tests
 * that show how to use the API stick to the public nodes.
 */
final class FieldMatch extends EvsQuery {

  /** The field to match against. */
  private final String field;

  /** How the field's value must match. */
  private final ValueMatch match;

  /**
   * Instantiates a {@link FieldMatch}.
   *
   * @param terminologies the terminologies being searched
   * @param field the field to match against
   * @param match how the field's value must match
   */
  public FieldMatch(
      final List<Terminology> terminologies, final String field, final ValueMatch match) {
    super(terminologies);
    this.field = Objects.requireNonNull(field, "field");
    this.match = Objects.requireNonNull(match, "match");
  }

  /**
   * Returns the field to match against.
   *
   * @return the field
   */
  public String getField() {
    return field;
  }

  /**
   * Returns how the field's value must match.
   *
   * @return the match
   */
  public ValueMatch getMatch() {
    return match;
  }

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(final OpenSearchOperations operations) {
    return match.getQueryBuilder(field);
  }
}
