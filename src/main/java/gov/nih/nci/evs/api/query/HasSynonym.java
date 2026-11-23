package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.ConceptUtils;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches concepts that have a synonym with the given name, optionally from a given source, term
 * type, and sub-source, e.g. concepts with a CDISC preferred term (PT) named "melanoma". The name,
 * source, term type, and sub-source must all match on the same synonym entry: a concept with a
 * CDISC synonym and a different PT synonym doesn't match "CDISC PT".
 *
 * <p>NCIt models a synonym as a FULL_SYN property whose source and term type are qualifiers. The
 * loader remodels them into the concept's {@code synonyms} list, with the qualifiers as fields, so
 * this node searches that list rather than {@link HasProperty}'s {@code properties}.
 *
 * <p>The name is compared to the synonym's normalized name: lowercased, with punctuation turned
 * into spaces and leading punctuation removed ({@link ConceptUtils#normalize}). The name is
 * normalized the same way when the query is built, so {@code "MELANOMA, MALIGNANT"} finds the
 * synonym "Melanoma, Malignant". Matching therefore ignores case and punctuation, and surrounding
 * spaces are trimmed. Source, term type, and sub-source are compared as given. To search synonyms
 * by part of a name, use a term search such as {@link Contains}.
 *
 * <p>Combine synonym nodes with the concept-level {@code And}/{@code Or}/{@code Not}: {@code Or} of
 * two nodes is a synonym matching either, {@code And} is a concept having both (possibly on
 * different synonyms), and {@code Not} is a concept with no matching synonym. Conditions that must
 * hold together on one synonym belong in one node. OR or NOT between fields of one synonym (e.g. "a
 * CDISC synonym that isn't a PT") isn't supported yet: see {@link QualifierCondition} for how that
 * would be modeled.
 */
public final class HasSynonym extends EvsQuery {

  /** The concept document's list of synonyms, mapped as nested so synonyms can be searched. */
  private static final String SYNONYMS_FIELD = "synonyms";

  /** A synonym entry's normalized name, e.g. "melanoma malignant". */
  private static final String NORMALIZED_NAME_FIELD = SYNONYMS_FIELD + ".normName";

  /** A synonym entry's source, e.g. "CDISC". */
  private static final String SOURCE_FIELD = SYNONYMS_FIELD + ".source";

  /** A synonym entry's term type, e.g. "PT". */
  private static final String TERM_TYPE_FIELD = SYNONYMS_FIELD + ".termType";

  /** A synonym entry's sub-source, e.g. "NCI-GLOSS". */
  private static final String SUB_SOURCE_FIELD = SYNONYMS_FIELD + ".subSource";

  /**
   * The synonym's name, as given, or null to match any name. It is normalized when the query is
   * built, so clients can send the name as written.
   */
  private String target;

  /** The synonym's source, or null to match any source. */
  private String source;

  /** The synonym's term type, or null to match any term type. */
  private String termType;

  /** The synonym's sub-source, or null to match any sub-source. */
  private String subSource;

  /** Instantiates an empty {@link HasSynonym}. */
  public HasSynonym() {
    // n/a
  }

  /**
   * Instantiates a {@link HasSynonym} that matches a synonym with the given name, from any source.
   *
   * @param target the synonym's name
   */
  public HasSynonym(final String target) {
    this(Objects.requireNonNull(target, "target"), null, null, null);
  }

  /**
   * Instantiates a {@link HasSynonym} that matches a synonym with the given name, source, and term
   * type, from any sub-source.
   *
   * @param target the synonym's name, or null for any name
   * @param source the synonym's source, or null for any source
   * @param termType the synonym's term type, or null for any term type
   */
  public HasSynonym(final String target, final String source, final String termType) {
    this(target, source, termType, null);
  }

  /**
   * Instantiates a {@link HasSynonym} that matches a synonym with the given name, source, term
   * type, and sub-source.
   *
   * @param target the synonym's name, or null for any name
   * @param source the synonym's source, or null for any source
   * @param termType the synonym's term type, or null for any term type
   * @param subSource the synonym's sub-source, or null for any sub-source
   */
  public HasSynonym(
      final String target, final String source, final String termType, final String subSource) {
    this.target = target;
    this.source = source;
    this.termType = termType;
    this.subSource = subSource;
  }

  /**
   * Returns the synonym's name, as given.
   *
   * @return the name, or null to match any name
   */
  public String getTarget() {
    return target;
  }

  /**
   * Sets the synonym's name, as given.
   *
   * @param target the name, or null to match any name
   */
  public void setTarget(final String target) {
    this.target = target;
  }

  /**
   * Returns the synonym's source.
   *
   * @return the source, or null to match any source
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the synonym's source.
   *
   * @param source the source, or null to match any source
   */
  public void setSource(final String source) {
    this.source = source;
  }

  /**
   * Returns the synonym's term type.
   *
   * @return the term type, or null to match any term type
   */
  public String getTermType() {
    return termType;
  }

  /**
   * Sets the synonym's term type.
   *
   * @param termType the term type, or null to match any term type
   */
  public void setTermType(final String termType) {
    this.termType = termType;
  }

  /**
   * Returns the synonym's sub-source.
   *
   * @return the sub-source, or null to match any sub-source
   */
  public String getSubSource() {
    return subSource;
  }

  /**
   * Sets the synonym's sub-source.
   *
   * @param subSource the sub-source, or null to match any sub-source
   */
  public void setSubSource(final String subSource) {
    this.subSource = subSource;
  }

  /**
   * Returns the name normalized the way synonym names were when they were indexed, to compare
   * against the normalized name field.
   *
   * @return the normalized name, or null to match any name
   */
  private String getNormalizedName() {
    final boolean hasName = target != null;
    return hasName ? ConceptUtils.normalize(target) : null;
  }

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    final BoolQueryBuilder entryConditions = QueryBuilders.boolQuery();
    addFieldCondition(
        entryConditions, NORMALIZED_NAME_FIELD, getNormalizedName(), operations, terminologies);
    addFieldCondition(entryConditions, SOURCE_FIELD, source, operations, terminologies);
    addFieldCondition(entryConditions, TERM_TYPE_FIELD, termType, operations, terminologies);
    addFieldCondition(entryConditions, SUB_SOURCE_FIELD, subSource, operations, terminologies);

    return QueryBuilders.nestedQuery(SYNONYMS_FIELD, entryConditions, ScoreMode.None);
  }

  /**
   * Requires the synonym entry's field to equal the value, when there is one. With no conditions at
   * all, the nested query matches concepts with any synonym.
   *
   * @param entryConditions the conditions one synonym entry must meet, added to
   * @param field the synonym entry's field
   * @param value the value the field must equal, or null to leave the field unconstrained
   * @param operations the OpenSearch operations
   * @param terminologies the terminologies being searched
   */
  private static void addFieldCondition(
      final BoolQueryBuilder entryConditions,
      final String field,
      final String value,
      final OpenSearchOperations operations,
      final List<Terminology> terminologies) {
    final boolean hasValue = value != null;
    if (hasValue) {
      entryConditions.filter(
          new MatchAnyOf(field, Set.of(value)).fetchOpenSearchQuery(operations, terminologies));
    }
  }
}
