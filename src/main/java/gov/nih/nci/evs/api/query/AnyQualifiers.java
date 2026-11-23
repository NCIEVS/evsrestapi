package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches property entries whose qualifiers meet at least one condition. No conditions matches
 * nothing, as for the concept-level {@code Or}, so an empty list of alternatives doesn't widen a
 * search to every property. See {@link QualifierCondition} for why this isn't {@code Or}.
 */
public final class AnyQualifiers implements QualifierCondition {

  /** One of the conditions must hold. */
  private static final int MINIMUM_MATCHING_CONDITIONS = 1;

  /** The alternative conditions. */
  private List<QualifierCondition> conditions;

  /** Instantiates an empty {@link AnyQualifiers}. */
  public AnyQualifiers() {
    // n/a
  }

  /**
   * Instantiates an {@link AnyQualifiers}.
   *
   * @param conditions the alternative conditions
   */
  public AnyQualifiers(final List<QualifierCondition> conditions) {
    this.conditions = new ArrayList<>(Objects.requireNonNull(conditions, "conditions"));
  }

  /**
   * Returns the alternative conditions.
   *
   * @return the conditions
   */
  public List<QualifierCondition> getConditions() {
    if (conditions == null) {
      conditions = new ArrayList<>();
    }
    return conditions;
  }

  /**
   * Sets the alternative conditions.
   *
   * @param conditions the conditions
   */
  public void setConditions(final List<QualifierCondition> conditions) {
    this.conditions = conditions;
  }

  /* see superclass */
  @Override
  public QueryBuilder getPropertiesMatching(
      final String qualifierListField,
      final OpenSearchOperations operations,
      final List<Terminology> terminologies) {
    final boolean hasAlternatives = !conditions.isEmpty();
    if (!hasAlternatives) {
      return QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery());
    }

    final BoolQueryBuilder anyConditionQuery =
        QueryBuilders.boolQuery().minimumShouldMatch(MINIMUM_MATCHING_CONDITIONS);
    for (final QualifierCondition condition : conditions) {
      anyConditionQuery.should(
          condition.getPropertiesMatching(qualifierListField, operations, terminologies));
    }
    return anyConditionQuery;
  }
}
