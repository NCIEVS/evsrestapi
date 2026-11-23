package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import java.util.Objects;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches property entries whose qualifiers don't meet the condition, e.g. a GO annotation with no
 * go-evi of "IDA". This is about one entry: a gene with one IDA annotation and one other annotation
 * still matches, through the other one. The concept-level {@code Not} would instead exclude every
 * gene with any IDA annotation. See {@link QualifierCondition}.
 */
public final class NoQualifiers implements QualifierCondition {

  /** The condition the entry must not meet. */
  private QualifierCondition condition;

  /** Instantiates an empty {@link NoQualifiers}. */
  public NoQualifiers() {
    // n/a
  }

  /**
   * Instantiates a {@link NoQualifiers}.
   *
   * @param condition the condition the entry must not meet
   */
  public NoQualifiers(final QualifierCondition condition) {
    this.condition = Objects.requireNonNull(condition, "condition");
  }

  /**
   * Returns the condition the entry must not meet.
   *
   * @return the condition
   */
  public QualifierCondition getCondition() {
    return condition;
  }

  /**
   * Sets the condition the entry must not meet.
   *
   * @param condition the condition
   */
  public void setCondition(final QualifierCondition condition) {
    this.condition = condition;
  }

  /* see superclass */
  @Override
  public QueryBuilder getPropertiesMatching(
      final String qualifierListField,
      final OpenSearchOperations operations,
      final List<Terminology> terminologies) {
    return QueryBuilders.boolQuery()
        .mustNot(condition.getPropertiesMatching(qualifierListField, operations, terminologies));
  }
}
