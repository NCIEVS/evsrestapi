package gov.nih.nci.evs.api.query;

import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.ConceptUtils;
import java.util.List;
import java.util.Map;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptType;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.sort.ScriptSortBuilder;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

/**
 * Completes a partially typed term against concept preferred names and codes.
 *
 * <ul>
 *   <li>Matching is {@link SearchAsYouType} on the preferred name: every typed word must appear, in
 *       any order, with only the last word allowed to be partial. Case and punctuation are ignored.
 *   <li>A partial code matches the start of a code, case sensitively, so "C322" finds C3224 and
 *       C32240 but "c322" finds neither.
 *   <li>Retired (inactive) concepts are left out, since they can't be used.
 *   <li>Ranking comes in tiers: a concept whose code is exactly what was typed, then name matches,
 *       then concepts that only matched by code. Every NCIt code starts with "C", so without the
 *       tiers, typing "c" or "c3" would bury names under codes. Within a tier, general concepts
 *       (those with more descendants) with short names come first, with a small preference for
 *       names that start with the typed text. See {@link #getSorts()}.
 *   <li>Run it with {@link #fetchConcepts}, which applies the ranking.
 * </ul>
 */
public class Typeahead extends EvsQuery {

  /** The search_as_you_type subfield of preferred names, which typeahead matches against. */
  private static final String NAME_TYPEAHEAD_FIELD =
      "name." + ConceptMinimal.SEARCH_AS_YOU_TYPE_SUFFIX;

  /** The preferred name field, whose text highlights show what matched. */
  private static final String NAME_FIELD = "name";

  /** Boolean field that is false for retired concepts, which typeahead leaves out. */
  private static final String ACTIVE_FIELD = "active";

  /** Keyword field holding the code, which a typed code is matched against. */
  private static final String CODE_FIELD = "code";

  /** Keyword field holding the normalized preferred name, used to rank typeahead matches. */
  private static final String NORMALIZED_NAME_FIELD = "normName";

  /**
   * Bonus for names that start with the typed text. Small enough that a much more general concept
   * whose name only contains the typed words, like "Acute Lymphoblastic Leukemia" for
   * "lymphoblastic", can still rank above it.
   */
  private static final double STARTS_WITH_BONUS = 1.0;

  /**
   * How many descendants one extra word in a name is worth when ranking typeahead matches. With a
   * penalty of 1, a name with one more word needs about e (2.7) times as many descendants to rank
   * the same.
   */
  private static final double WORD_PENALTY = 1.0;

  /** Field holding each concept's number of descendants, stored when concepts are loaded. */
  private static final String DESCENDANT_COUNT_FIELD = "descendantCount";

  /**
   * Field counted in place of {@link #DESCENDANT_COUNT_FIELD} on indexes loaded before it existed.
   * Every descendant has a leaf flag, and boolean doc values keep duplicates, so the number of leaf
   * values is the number of descendants.
   */
  private static final String DESCENDANT_LEAF_FIELD = "descendants.leaf";

  /**
   * Query score added when a concept's code is exactly what was typed. Above every other score, so
   * the whole code comes first. See {@link #fetchOpenSearchQuery}.
   */
  private static final float WHOLE_CODE_SCORE = 2.0f;

  /** Query score added when a concept's name matched. See {@link #fetchOpenSearchQuery}. */
  private static final float NAME_MATCH_SCORE = 1.0f;

  /**
   * Query score added when a concept's code starts with what was typed. Nothing, so concepts that
   * only matched by code rank below name matches. See {@link #fetchOpenSearchQuery}.
   */
  private static final float CODE_MATCH_SCORE = 0.0f;

  /**
   * Ranks general concepts with short names first: log(1 + descendant count), minus the word
   * penalty for each word in the normalized name, plus the starts-with bonus when the normalized
   * name starts with the typed text. The log keeps concepts with huge hierarchies from outranking
   * everything. A concept without a normalized name sorts last, and one without a descendant count
   * counts as having none.
   *
   * <p>TODO: Drop the {@link #DESCENDANT_LEAF_FIELD} fallback once every index has been reloaded
   * with {@link #DESCENDANT_COUNT_FIELD}.
   */
  private static final String GENERAL_SHORT_NAME_FIRST_SOURCE =
      """
      boolean hasNormalizedName = doc['%1$s'].size() != 0;
      if (!hasNormalizedName) {
        return -Double.MAX_VALUE;
      }
      String name = doc['%1$s'].value;

      int wordCount = 1;
      for (int i = 0; i < name.length(); i++) {
        boolean isWordBreak = name.charAt(i) == (char) ' ';
        if (isWordBreak) {
          wordCount++;
        }
      }

      boolean isNameStartingWithIncompleteTerm = name.startsWith(params.normalizedIncompleteTerm);
      double startsWithBonus = isNameStartingWithIncompleteTerm ? params.startsWithBonus : 0;

      boolean hasDescendantCountField = doc.containsKey('%2$s');
      boolean hasStoredDescendantCount = hasDescendantCountField && doc['%2$s'].size() != 0;
      int descendantCount = 0;
      if (hasStoredDescendantCount) {
        descendantCount = (int) doc['%2$s'].value;
      } else if (!hasDescendantCountField) {
        descendantCount = doc['%3$s'].size();
      }

      return Math.log(1 + descendantCount) - params.wordPenalty * wordCount + startsWithBonus;
      """
          .formatted(NORMALIZED_NAME_FIELD, DESCENDANT_COUNT_FIELD, DESCENDANT_LEAF_FIELD);

  /** The text typed so far, as entered. The search_as_you_type analyzer handles normalization. */
  private final String incompleteTerm;

  /**
   * Instantiates a {@link Typeahead}.
   *
   * @param terminologies the terminologies being searched
   * @param incompleteTerm the text typed so far; must contain at least one word (see {@link
   *     #getHasWordsToComplete(String)})
   */
  public Typeahead(final List<Terminology> terminologies, final String incompleteTerm) {
    super(terminologies);
    final boolean hasWordsToComplete = getHasWordsToComplete(incompleteTerm);
    if (!hasWordsToComplete) {
      throw new IllegalArgumentException("Typeahead term has no words: " + incompleteTerm);
    }
    this.incompleteTerm = incompleteTerm;
  }

  /**
   * Indicates whether the typed text has anything to complete. Null, blank, and punctuation-only
   * text has no words.
   *
   * @param incompleteTerm the text typed so far
   * @return whether the text has at least one word
   */
  public static boolean getHasWordsToComplete(final String incompleteTerm) {
    final boolean hasText = incompleteTerm != null;
    return hasText && !ConceptUtils.normalize(incompleteTerm).isEmpty();
  }

  /**
   * Returns the text typed so far.
   *
   * @return the incomplete term
   */
  public String getIncompleteTerm() {
    return incompleteTerm;
  }

  /**
   * Matches concepts whose preferred name or code completes the typed text. The score sorts matches
   * into tiers, because each clause adds a constant score: {@link #WHOLE_CODE_SCORE} for a whole
   * typed code, {@link #NAME_MATCH_SCORE} for a name match, and {@link #CODE_MATCH_SCORE} for a
   * partial code. So a whole code scores at least 2, a name match 1, and a code-only match 0.
   *
   * <p>Concepts marked inactive are left out. Concepts without an active flag are kept, so
   * terminologies that don't set it still complete.
   *
   * @param operations the OpenSearch operations
   * @return the query builder
   */
  @Override
  public QueryBuilder fetchOpenSearchQuery(final OpenSearchOperations operations) {
    // Codes aren't normalized, so only surrounding whitespace is dropped before comparing
    final String typedCode = incompleteTerm.trim();
    final QueryBuilder hasNameCompletingTypedWords =
        new FieldMatch(terminologies, NAME_TYPEAHEAD_FIELD, new SearchAsYouType(incompleteTerm))
            .fetchOpenSearchQuery(operations);
    // Codes are always case sensitive, so they're completed with a prefix on the code keyword,
    // not a search_as_you_type field, which lowercases what it compares
    final QueryBuilder hasCodeStartingWithTypedCode =
        new FieldMatch(terminologies, CODE_FIELD, new StartsWith(typedCode))
            .fetchOpenSearchQuery(operations);
    final QueryBuilder hasTypedCode =
        new FieldMatch(terminologies, CODE_FIELD, new Exact(typedCode))
            .fetchOpenSearchQuery(operations);
    final String inactiveFlag = Boolean.toString(false);
    final QueryBuilder isInactive =
        new FieldMatch(terminologies, ACTIVE_FIELD, new Exact(inactiveFlag))
            .fetchOpenSearchQuery(operations);

    // Confirm that the ordering has not been messed up
    assert WHOLE_CODE_SCORE > NAME_MATCH_SCORE && NAME_MATCH_SCORE > CODE_MATCH_SCORE;

    final List<QueryBuilder> completionTiers =
        List.of(
            getScoredAs(hasTypedCode, WHOLE_CODE_SCORE),
            getScoredAs(hasNameCompletingTypedWords, NAME_MATCH_SCORE),
            getScoredAs(hasCodeStartingWithTypedCode, CODE_MATCH_SCORE));
    return getMatchingAnyExcept(completionTiers, isInactive);
  }

  /**
   * Returns a query that matches what the query matches, with a constant score instead of its own.
   * Raw OpenSearch until the logical operator nodes land: this becomes a {@code Boost} node.
   *
   * @param query the query to score
   * @param score the score of every match
   * @return the query builder
   */
  private static QueryBuilder getScoredAs(final QueryBuilder query, final float score) {
    return QueryBuilders.constantScoreQuery(query).boost(score);
  }

  /**
   * Returns a query that matches concepts matching at least one of the queries and not the excluded
   * one, scored as the sum of the queries it matches. Raw OpenSearch until the logical operator
   * nodes land: this becomes {@code And(Or(anyOf...), Not(excluded))}.
   *
   * @param anyOf the queries, at least one of which must match
   * @param excluded the query that must not match
   * @return the query builder
   */
  private static QueryBuilder getMatchingAnyExcept(
      final List<QueryBuilder> anyOf, final QueryBuilder excluded) {
    final int atLeastOneMatches = 1;
    final BoolQueryBuilder matchingAnyExcept =
        QueryBuilders.boolQuery().mustNot(excluded).minimumShouldMatch(atLeastOneMatches);
    anyOf.forEach(matchingAnyExcept::should);
    return matchingAnyExcept;
  }

  /**
   * Highlights the matched words of the preferred name, and the code when it matched.
   *
   * <p>The name is highlighted on its main text field with a {@code match_bool_prefix} query: the
   * typed words, with the last one as a prefix. Highlighting the {@code search_as_you_type}
   * subfield the query actually matched doesn't work well: OpenSearch never marks the partial last
   * word there (so "melan" highlights nothing), and its prefix subfield marks whole word groups.
   * OpenSearch still marks whole words, e.g. {@code <em>Lung</em> <em>Carcinoma</em>} for "lung
   * ca".
   *
   * @return the name and code highlight fields
   */
  @Override
  protected List<HighlightBuilder.Field> getHighlightFields() {
    final QueryBuilder typedWordsQuery =
        QueryBuilders.matchBoolPrefixQuery(NAME_FIELD, incompleteTerm);
    return List.of(
        new HighlightBuilder.Field(NAME_FIELD).highlightQuery(typedWordsQuery),
        new HighlightBuilder.Field(CODE_FIELD));
  }

  /**
   * Orders matches best first: by tier, which is the query score (see {@link
   * #fetchOpenSearchQuery}), then general concepts with short names first within a tier (see {@link
   * #GENERAL_SHORT_NAME_FIRST_SOURCE}). Ties fall through to the default tie-break in {@link
   * EvsQuery#fetchConcepts}.
   *
   * @return the tier sort, then the ranking sort
   */
  @Override
  protected List<SortBuilder<?>> getSorts() {
    return List.of(
        SortBuilders.scoreSort().order(SortOrder.DESC),
        SortBuilders.scriptSort(getRankingScript(), ScriptSortBuilder.ScriptSortType.NUMBER)
            .order(SortOrder.DESC));
  }

  /**
   * Builds the script that ranks typeahead matches within a tier. See {@link
   * #GENERAL_SHORT_NAME_FIRST_SOURCE}.
   *
   * @return the ranking script
   */
  private Script getRankingScript() {
    // Normalized like NORMALIZED_NAME_FIELD so the starts-with comparison lines up
    final String normalizedIncompleteTerm = ConceptUtils.normalize(incompleteTerm);
    return new Script(
        ScriptType.INLINE,
        Script.DEFAULT_SCRIPT_LANG,
        GENERAL_SHORT_NAME_FIRST_SOURCE,
        Map.of(
            "normalizedIncompleteTerm", normalizedIncompleteTerm,
            "wordPenalty", WORD_PENALTY,
            "startsWithBonus", STARTS_WITH_BONUS));
  }
}
