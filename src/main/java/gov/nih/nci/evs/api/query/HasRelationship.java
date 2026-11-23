package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches concepts with a relationship of the given type in one of their relationship lists,
 * optionally pointing to the given target, e.g. concepts whose primary anatomic site role points to
 * Skin. Roles and associations are both lists of entries pairing a type with a related concept, so
 * one node searches either: only the list differs. Each subclass says which list it searches
 * ({@link #getRelationshipListFieldName}): {@link HasRole} searches {@code roles}, {@link
 * HasAssociation} searches {@code associations}, and {@link HasInverseAssociation} searches {@code
 * inverseAssociations}.
 *
 * <p>The type and the target are each a name or code, equal to the entry's name or code: {@code new
 * HasRole("Disease_Has_Primary_Anatomic_Site", "C12470")}. Both are compared against values stored
 * on the entry itself, so no other search runs. The target name is analyzed text, so a name
 * compares against its individual lowercased words, not the whole name.
 *
 * <p>The type and target must match on the same entry. A concept with "primary site: Lung" and
 * "associated site: Skin" does not match "primary site: Skin".
 *
 * <p>Needs concept indexes mapped with the list as nested. On an index loaded before that mapping,
 * the search fails instead of silently returning nothing.
 */
public abstract class HasRelationship extends EvsQuery {

  /** The entry field holding the type's name, e.g. "Disease_Has_Primary_Anatomic_Site". */
  private static final String TYPE_NAME_FIELD = "type";

  /** The entry field holding the type's code, e.g. "R101". */
  private static final String TYPE_CODE_FIELD = "code";

  /** The entry field holding the name of the concept the entry points to. Analyzed text. */
  private static final String TARGET_NAME_FIELD = "relatedName";

  /** The entry field holding the code of the concept the entry points to. */
  private static final String TARGET_CODE_FIELD = "relatedCode";

  /** Joins the list's name and an entry field into the entry field's full name. */
  private static final String ENTRY_FIELD_SEPARATOR = ".";

  /**
   * The relationship's type name or code. The type is the kind of relationship the entry expresses,
   * e.g. "Disease_Has_Primary_Anatomic_Site" (code "R101"). A concept has many entries, each
   * pairing a type with a target concept, and the type says how the concept relates to that target.
   */
  private String relationshipType;

  /** The name or code of the concept the relationship points to, or null for any. */
  private String target;

  /** Instantiates an empty {@link HasRelationship}. */
  protected HasRelationship() {
    // n/a
  }

  /**
   * Instantiates a {@link HasRelationship} that matches a relationship of the given type, pointing
   * to any concept.
   *
   * @param relationshipType the relationship's type name or code
   */
  protected HasRelationship(final String relationshipType) {
    this.relationshipType = Objects.requireNonNull(relationshipType, "relationshipType");
    this.target = null;
  }

  /**
   * Instantiates a {@link HasRelationship} that matches a relationship of the given type pointing
   * to the target.
   *
   * @param relationshipType the relationship's type name or code
   * @param target the target's name or code
   */
  protected HasRelationship(final String relationshipType, final String target) {
    this.relationshipType = Objects.requireNonNull(relationshipType, "relationshipType");
    this.target = Objects.requireNonNull(target, "target");
  }

  /**
   * Returns the name of the concept document's list of relationships this node searches, e.g.
   * {@code roles}. OpenSearch calls it the nested path.
   *
   * <p>The list must be mapped as nested in {@code Concept}, or the search fails.
   *
   * @return the list's field name
   */
  protected abstract String getRelationshipListFieldName();

  /**
   * Returns the relationship's type name or code. Protected, so it isn't a JSON property: each
   * subclass exposes it under its own name, e.g. {@code role}, since {@code type} is the JSON
   * property naming the node.
   *
   * @return the type name or code
   */
  protected String getRelationshipType() {
    return relationshipType;
  }

  /**
   * Sets the relationship's type name or code.
   *
   * @param relationshipType the type name or code
   */
  protected void setRelationshipType(final String relationshipType) {
    this.relationshipType = relationshipType;
  }

  /**
   * Indicates whether the relationship must point to a specific target.
   *
   * @return whether there is a target
   */
  private boolean getHasTarget() {
    return target != null;
  }

  /**
   * Returns the target's name or code.
   *
   * @return the target name or code, or null for any target
   */
  public String getTarget() {
    return target;
  }

  /**
   * Sets the target's name or code.
   *
   * @param target the target name or code, or null for any target
   */
  public void setTarget(final String target) {
    this.target = target;
  }

  /**
   * Returns the OpenSearch query for this node: a concept matches when one of the entries in its
   * relationship list has this type and, if there is one, points to the target.
   *
   * @param operations the OpenSearch operations, for building the field matches
   * @param terminologies the terminologies being searched
   * @return the query builder
   */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    final BoolQueryBuilder entryConditions =
        QueryBuilders.boolQuery()
            .must(
                getEntryNameOrCodeQuery(
                    TYPE_NAME_FIELD, TYPE_CODE_FIELD, relationshipType, operations, terminologies));

    final boolean hasTarget = getHasTarget();
    if (hasTarget) {
      entryConditions.must(
          getEntryNameOrCodeQuery(
              TARGET_NAME_FIELD, TARGET_CODE_FIELD, target, operations, terminologies));
    }

    return getConceptsWithAnyEntryMatching(entryConditions);
  }

  /**
   * Returns the condition matching an entry whose name field or code field equals the name or code.
   *
   * @param nameEntryField the entry field holding the name
   * @param codeEntryField the entry field holding the code
   * @param nameOrCode the name or code
   * @param operations the OpenSearch operations, for building the field matches
   * @param terminologies the terminologies being searched
   * @return the condition
   */
  private QueryBuilder getEntryNameOrCodeQuery(
      final String nameEntryField,
      final String codeEntryField,
      final String nameOrCode,
      final OpenSearchOperations operations,
      final List<Terminology> terminologies) {
    return new NameOrCodeMatch(
            getEntryFieldName(nameEntryField),
            getEntryFieldName(codeEntryField),
            Set.of(nameOrCode))
        .fetchOpenSearchQuery(operations, terminologies);
  }

  /**
   * Returns the full name of a field on this node's entries: the list's name, a dot, and the field,
   * e.g. {@code roles.relatedCode}.
   *
   * @param entryField the field on one entry, e.g. {@code relatedCode}
   * @return the full field name
   */
  private String getEntryFieldName(final String entryField) {
    return getRelationshipListFieldName() + ENTRY_FIELD_SEPARATOR + entryField;
  }

  /**
   * Builds a query matching concepts that have at least one entry in this node's relationship list
   * satisfying all the given conditions. An entry is one item in the list, pairing a type with a
   * target.
   *
   * <p>The conditions must hold on the same entry, which is why this is a {@code nested} query.
   * Without it, OpenSearch flattens every entry's fields together, and "Lung Carcinoma" (primary
   * site Lung, associated site Skin) would match "primary site: Skin" by taking the type from one
   * entry and the target from the other.
   *
   * @param entryConditions the conditions one entry must meet
   * @return the query builder
   */
  private QueryBuilder getConceptsWithAnyEntryMatching(final QueryBuilder entryConditions) {
    return QueryBuilders.nestedQuery(
        getRelationshipListFieldName(), entryConditions, ScoreMode.None);
  }
}
