package gov.nih.nci.evs.api.query;

import java.util.Objects;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Matches values within a small edit distance of the term (case sensitive). By default the allowed
 * distance scales with term length: exact for 1-2 characters, 1 edit for 3-5, 2 edits beyond.
 *
 * @param term the term the value must approximately equal
 * @param fuzziness the allowed edit distance
 */
public record Fuzzy(String term, Fuzziness fuzziness) implements ValueMatch {

  /**
   * Instantiates a {@link Fuzzy} with an explicit edit distance.
   *
   * @param term the term the value must approximately equal
   * @param fuzziness the allowed edit distance
   */
  public Fuzzy {
    Objects.requireNonNull(term, "term");
    Objects.requireNonNull(fuzziness, "fuzziness");
  }

  /**
   * Instantiates a {@link Fuzzy} with an edit distance scaled to the term length.
   *
   * @param term the term the value must approximately equal
   */
  public Fuzzy(final String term) {
    this(term, Fuzziness.AUTO);
  }

  /* see superclass */
  @Override
  public QueryBuilder getQueryBuilder(final String field) {
    return QueryBuilders.fuzzyQuery(field, term).fuzziness(fuzziness);
  }
}
