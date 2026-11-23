package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches documents whose field equals any of a set of values (case sensitive, whole value), e.g.
 * concepts whose {@code ancestors} include any of several codes. One OpenSearch {@code terms} query
 * instead of a {@code should} clause per value, which would hit OpenSearch's clause limit for large
 * sets.
 *
 * <p>OpenSearch accepts at most {@value #MAX_VALUES} values in one {@code terms} query by default,
 * so the constructor rejects larger sets. Callers that build the set from other concepts (see
 * {@link HasHierarchyRelative#fetchTargetCodes}) check the size first to report a clearer error.
 */
class MatchAnyOf extends EvsQuery {

  /** OpenSearch's default limit on values in one {@code terms} query. */
  static final int MAX_VALUES = 65_536;

  /** The field to match against. */
  private String field;

  /** The values, any of which the field may equal. Sorted, so built queries are repeatable. */
  private Set<String> values;

  /** Instantiates an empty {@link MatchAnyOf}. */
  public MatchAnyOf() {
    // n/a
  }

  /**
   * Instantiates a {@link MatchAnyOf}.
   *
   * @param field the field to match against
   * @param values the values, any of which the field may equal
   */
  public MatchAnyOf(final String field, final Collection<String> values) {
    this.field = Objects.requireNonNull(field, "field");
    this.values = new TreeSet<>(Objects.requireNonNull(values, "values"));

    final boolean hasTooManyValues = this.values.size() > MAX_VALUES;
    if (hasTooManyValues) {
      throw new IllegalArgumentException(
          "Too many values for one terms query on '"
              + field
              + "': "
              + this.values.size()
              + " > "
              + MAX_VALUES);
    }
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
   * Sets the field to match against.
   *
   * @param field the field
   */
  public void setField(final String field) {
    this.field = field;
  }

  /**
   * Returns the values, any of which the field may equal.
   *
   * @return the values, sorted
   */
  public Set<String> getValues() {
    if (values == null) {
      values = new TreeSet<>();
    }
    return values;
  }

  /**
   * Sets the values, any of which the field may equal.
   *
   * @param values the values
   */
  public void setValues(final Set<String> values) {
    this.values = values;
  }

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    return QueryBuilders.termsQuery(field, values);
  }
}
