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
 * Matches property entries whose qualifiers meet every condition, all on that same entry. No
 * conditions requires nothing, so every entry matches. See {@link QualifierCondition} for why this
 * isn't the concept-level {@code And}.
 */
public final class AllQualifiers implements QualifierCondition {

  /** The conditions that must all hold. */
  private List<QualifierCondition> conditions;

  /** Instantiates an empty {@link AllQualifiers}. */
  public AllQualifiers() {
    // n/a
  }

  /**
   * Instantiates an {@link AllQualifiers}.
   *
   * @param conditions the conditions that must all hold
   */
  public AllQualifiers(final List<QualifierCondition> conditions) {
    this.conditions = new ArrayList<>(Objects.requireNonNull(conditions, "conditions"));
  }

  /**
   * Returns the conditions that must all hold.
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
   * Sets the conditions that must all hold.
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
    final BoolQueryBuilder allConditionsQuery = QueryBuilders.boolQuery();
    for (final QualifierCondition condition : conditions) {
      allConditionsQuery.must(
          condition.getPropertiesMatching(qualifierListField, operations, terminologies));
    }
    return allConditionsQuery;
  }
}
