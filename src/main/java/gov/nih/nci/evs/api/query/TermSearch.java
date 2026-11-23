package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpenSearchServiceImpl;
import java.util.List;
import java.util.Objects;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

/**
 * Searches concepts for a term the way the legacy {@code GET /concept/search} does: the term is
 * matched against each concept's name, code, and synonyms, and results are ranked by relevance with
 * active concepts first. Each subclass is one legacy search {@code type}.
 */
public abstract class TermSearch extends EvsQuery {

  /** The field flagging whether a concept is active. */
  private static final String ACTIVE_FIELD = "active";

  /** Boost that lifts active concepts above inactive ones, as the legacy search does. */
  private static final float ACTIVE_BOOST = 100000f;

  /** Boost that keeps inactive concepts at the bottom, as the legacy search does. */
  private static final float INACTIVE_BOOST = 0.0001f;

  /** The term to search for. */
  private String term;

  /** Instantiates an empty {@link TermSearch}. */
  protected TermSearch() {
    // n/a
  }

  /**
   * Instantiates a {@link TermSearch}.
   *
   * @param term the term to search for
   */
  protected TermSearch(final String term) {
    this.term = Objects.requireNonNull(term, "term");
  }

  /**
   * Returns the term to search for.
   *
   * @return the term
   */
  public String getTerm() {
    return term;
  }

  /**
   * Sets the term to search for.
   *
   * @param term the term
   */
  public void setTerm(final String term) {
    this.term = term;
  }

  /**
   * Returns the legacy search's query for the term.
   *
   * @param escapedTerm the term with query string special characters escaped
   * @return the query builder
   * @throws Exception the exception the legacy query builders declare
   */
  protected abstract QueryBuilder getTermQuery(String escapedTerm) throws Exception;

  /* see superclass */
  @Override
  public QueryBuilder fetchOpenSearchQuery(
      final OpenSearchOperations operations, final List<Terminology> terminologies) {
    final String escapedTerm = OpenSearchServiceImpl.escape(term);
    final QueryBuilder termQuery = fetchTermQuery(escapedTerm);

    final QueryBuilder activeFirstQuery =
        QueryBuilders.boolQuery()
            .should(QueryBuilders.matchQuery(ACTIVE_FIELD, true).boost(ACTIVE_BOOST))
            .should(QueryBuilders.matchQuery(ACTIVE_FIELD, false).boost(INACTIVE_BOOST));

    return QueryBuilders.boolQuery().must(termQuery).must(activeFirstQuery);
  }

  /**
   * Returns the legacy search's query for the term. The legacy query builders declare {@code throws
   * Exception} without throwing a checked exception, so it's rethrown unchecked here.
   *
   * @param escapedTerm the term with query string special characters escaped
   * @return the query builder
   */
  private QueryBuilder fetchTermQuery(final String escapedTerm) {
    try {
      return getTermQuery(escapedTerm);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** Ranks the most relevant concepts first, as the legacy search does. */
  @Override
  protected List<SortBuilder<?>> getSorts() {
    return List.of(SortBuilders.scoreSort().order(SortOrder.DESC));
  }
}
