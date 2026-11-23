package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.IdsQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Unit tests for the limits and batching of the hierarchy query nodes: sets too large for one
 * {@code terms} query are rejected with a clear message, missing arguments fail fast, and lookups
 * of many anchors are split into batches. The anchors of the stored hierarchy lookups are the
 * hierarchy nodes' targets.
 *
 * <p>No OpenSearch or Spring: a mocked {@link OpenSearchOperations} returns made-up concepts. That
 * makes it cheap to return tens of thousands of descendants or ancestors, which {@link
 * HasHierarchyRelativeTest} can't index.
 */
public class HasHierarchyRelativeLimitsTest {

  /** Index name of the made-up terminology. */
  private static final String INDEX_NAME = "concept_limits_test";

  /** Terminology of the made-up concepts. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the made-up concepts. */
  private static final String VERSION = "1";

  /** Field name used when building a {@link MatchAnyOf}. */
  private static final String ANCESTORS_FIELD = "ancestors.code";

  /** One more than fits in a {@code terms} query. */
  private static final int TOO_MANY = MatchAnyOf.MAX_VALUES + 1;

  /** A max level of one: direct children or parents. */
  private static final int ONE_LEVEL = 1;

  /** Code of the target in the descendant tests. */
  private static final String DESCENDANT_TARGET_CODE = "R";

  /** Code of the target in the ancestor tests. */
  private static final String ANCESTOR_TARGET_CODE = "G";

  /** Code of the concept the mocked target code lookup matches. */
  private static final String MATCHED_TARGET_CODE = "target";

  /** Code of the single target in the depth token tests. */
  private static final String DEPTH_TARGET_CODE = "C1";

  /** Max level in the one-target depth token test. */
  private static final int ONE_TARGET_MAX_LEVEL = 2;

  /** Max level whose targets times levels exactly reach the limit. */
  private static final int LEVELS_AT_LIMIT = 4;

  /** Targets that alone are over the limit, with a max level of one. */
  private static final int TARGETS_OVER_LIMIT = 70_000;

  /** Targets that are over the limit only once multiplied by {@link #LEVELS_OVER_LIMIT}. */
  private static final int TARGETS_UNDER_LIMIT = 10_000;

  /** Max level that pushes {@link #TARGETS_UNDER_LIMIT} targets over the limit. */
  private static final int LEVELS_OVER_LIMIT = 7;

  /**
   * A max level far enough above the target to reach every one of its {@link #TOO_MANY} ancestors.
   */
  private static final int LEVELS_REACHING_EVERY_ANCESTOR = TOO_MANY;

  /** The message fragment naming the max level of an over-limit set of depth tokens. */
  private static final String MAX_LEVEL_MESSAGE = "maxLevel " + LEVELS_OVER_LIMIT;

  /** The argument named when the target is null. */
  private static final String TARGET_ARGUMENT = "target";

  /** Message fragment telling the caller to reindex. */
  private static final String REINDEX_MESSAGE = "reindex the terminology";

  /** Message fragment of the too-many-ancestors error. */
  private static final String TOO_MANY_ANCESTORS_MESSAGE = "Too many ancestors";

  /** Size of the last, partial lookup in the batching tests. */
  private static final int PARTIAL_LOOKUP_SIZE = 500;

  /** Anchors in the batching tests: two full lookups and one partial. */
  private static final int MANY_ANCHORS =
      2 * StoredHierarchyFallback.CONCEPTS_PER_LOOKUP + PARTIAL_LOOKUP_SIZE;

  /** Lookups expected for {@link #MANY_ANCHORS} anchors. */
  private static final int EXPECTED_LOOKUP_COUNT = 3;

  /** Ids per lookup expected for {@link #MANY_ANCHORS} anchors. */
  private static final List<Integer> EXPECTED_LOOKUP_SIZES =
      List.of(
          StoredHierarchyFallback.CONCEPTS_PER_LOOKUP,
          StoredHierarchyFallback.CONCEPTS_PER_LOOKUP,
          PARTIAL_LOOKUP_SIZE);

  /** Code of the first generated anchor. */
  private static final String FIRST_ANCHOR_CODE = "C0";

  /** Code of the last generated anchor. */
  private static final String LAST_ANCHOR_CODE = "C" + (MANY_ANCHORS - 1);

  /** Suffix of the made-up child of each anchor. */
  private static final String CHILD_SUFFIX = "-child";

  /** Suffix of the made-up parent of each anchor. */
  private static final String PARENT_SUFFIX = "-parent";

  /** A terminology indexed without ancestor fields, so {@link HasAncestor} uses the fallback. */
  private Terminology terminology;

  /** The terminology searched, as a list. */
  private List<Terminology> terminologies;

  /** Mocked OpenSearch operations, stubbed per test. */
  private OpenSearchOperations operations;

  /** Builds the terminology and a fresh mock. */
  @BeforeEach
  public void setUp() {
    terminology = new Terminology();
    terminology.setTerminology(TERMINOLOGY);
    terminology.setVersion(VERSION);
    terminology.setIndexName(INDEX_NAME);
    terminologies = List.of(terminology);
    operations = mock(OpenSearchOperations.class);
  }

  /** MatchAnyOf with exactly the limit of values is accepted. */
  @Test
  public void testMatchAnyOfAtLimitIsAccepted() {
    assertDoesNotThrow(() -> new MatchAnyOf(ANCESTORS_FIELD, getCodes(MatchAnyOf.MAX_VALUES)));
  }

  /** MatchAnyOf with one more value than the limit throws an error naming the field and count. */
  @Test
  public void testMatchAnyOfOverLimitThrowsErrorNamingFieldAndCount() {
    final IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> new MatchAnyOf(ANCESTORS_FIELD, getCodes(TOO_MANY)));

    assertTrue(e.getMessage().contains("'" + ANCESTORS_FIELD + "'"), e.getMessage());
    assertTrue(e.getMessage().contains(String.valueOf(TOO_MANY)), e.getMessage());
  }

  /** One target gives one depth token per level up to the max level. */
  @Test
  public void testDepthTokensForOneTargetGivesOneTokenPerDepth() {
    final List<String> expectedTokens =
        List.of(
            DEPTH_TARGET_CODE + HasAncestor.DEPTH_SEPARATOR + 1,
            DEPTH_TARGET_CODE + HasAncestor.DEPTH_SEPARATOR + 2);

    assertEquals(
        expectedTokens,
        HasAncestor.getDepthTokens(List.of(DEPTH_TARGET_CODE), ONE_TARGET_MAX_LEVEL));
  }

  /** Depth tokens that exactly reach the limit are accepted. */
  @Test
  public void testDepthTokensAtLimitAreAccepted() {
    final int targetsAtLimit = MatchAnyOf.MAX_VALUES / LEVELS_AT_LIMIT;

    assertEquals(
        MatchAnyOf.MAX_VALUES,
        HasAncestor.getDepthTokens(getCodes(targetsAtLimit), LEVELS_AT_LIMIT).size());
  }

  /** Too many targets for the limit, even at a max level of one, throws an error. */
  @Test
  public void testDepthTokensWithTooManyTargetsThrowsError() {
    assertThrows(
        IllegalArgumentException.class,
        () -> HasAncestor.getDepthTokens(getCodes(TARGETS_OVER_LIMIT), ONE_LEVEL));
  }

  /** Targets times max level over the limit throws an error naming the max level. */
  @Test
  public void testDepthTokensOverLimitThroughMaxLevelThrowsErrorNamingMaxLevel() {
    final IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> HasAncestor.getDepthTokens(getCodes(TARGETS_UNDER_LIMIT), LEVELS_OVER_LIMIT));

    assertTrue(e.getMessage().contains(MAX_LEVEL_MESSAGE), e.getMessage());
  }

  /** A null target on HasAncestor throws an error naming target. */
  @Test
  public void testHasAncestorWithNullTargetThrowsErrorNamingTarget() {
    final NullPointerException e =
        assertThrows(NullPointerException.class, () -> new HasAncestor((String) null));

    assertEquals(TARGET_ARGUMENT, e.getMessage());
  }

  /** A null target on HasDescendantOrIs throws an error naming target. */
  @Test
  public void testHasDescendantOrIsWithNullTargetThrowsErrorNamingTarget() {
    final NullPointerException e =
        assertThrows(
            NullPointerException.class, () -> new HasDescendantOrIs((String) null, ONE_LEVEL));

    assertEquals(TARGET_ARGUMENT, e.getMessage());
  }

  /** A null target on HasDescendant throws an error naming target. */
  @Test
  public void testHasDescendantWithNullTargetThrowsErrorNamingTarget() {
    final NullPointerException e =
        assertThrows(NullPointerException.class, () -> new HasDescendant((String) null));

    assertEquals(TARGET_ARGUMENT, e.getMessage());
  }

  /** A null target on HasAncestorOrIs throws an error naming target. */
  @Test
  public void testHasAncestorOrIsWithNullTargetThrowsErrorNamingTarget() {
    final NullPointerException e =
        assertThrows(
            NullPointerException.class, () -> new HasAncestorOrIs((String) null, ONE_LEVEL));

    assertEquals(TARGET_ARGUMENT, e.getMessage());
  }

  /**
   * On an index without ancestor fields, a branch with more descendants than fit in one query
   * throws an error telling the caller to reindex.
   */
  @Test
  public void testFallbackWithTooManyDescendantsThrowsErrorAskingToReindex() {
    stubSearch(
        target -> {
          final Concept concept = getConcept(target);
          concept.setDescendants(getCodes(TOO_MANY).stream().map(Concept::new).toList());
          return concept;
        });

    final IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new HasAncestor(DESCENDANT_TARGET_CODE)
                    .fetchOpenSearchQuery(operations, terminologies));

    assertTrue(e.getMessage().contains(REINDEX_MESSAGE), e.getMessage());
  }

  /** The same limit applies to the fallback's depth-limited walk over children. */
  @Test
  public void testFallbackWithMaxLevelAndTooManyChildrenThrowsError() {
    stubSearch(
        target -> {
          final Concept concept = getConcept(target);
          concept.setChildren(getCodes(TOO_MANY).stream().map(Concept::new).toList());
          return concept;
        });

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new HasAncestor(DESCENDANT_TARGET_CODE, ONE_LEVEL)
                .fetchOpenSearchQuery(operations, terminologies));
  }

  /**
   * A level-limited HasDescendant whose target has more ancestors within the limit than fit in one
   * query throws an error. Without a limit, HasDescendant matches on the stored descendants and
   * reads no paths, so only a limited one can hit this.
   */
  @Test
  public void testHasDescendantMaxLevelWithTooManyAncestorsThrowsErrorNamingTooManyAncestors() {
    stubSearch(target -> getConceptWithAncestorCount(target, TOO_MANY));

    final IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new HasDescendant(ANCESTOR_TARGET_CODE, LEVELS_REACHING_EVERY_ANCESTOR)
                    .fetchOpenSearchQuery(operations, terminologies));

    assertTrue(e.getMessage().contains(TOO_MANY_ANCESTORS_MESSAGE), e.getMessage());
  }

  /** The fallback finds the descendants of every one of many anchors. */
  @Test
  public void testFallbackWithManyAnchorsFindsEveryAnchorsDescendants() {
    stubOneChildPerAnchor();

    final Set<String> found =
        StoredHierarchyFallback.fetchDescendantCodes(
            operations, terminology, getCodes(MANY_ANCHORS), null);

    assertEquals(MANY_ANCHORS, found.size());
    assertTrue(
        found.containsAll(
            Set.of(FIRST_ANCHOR_CODE + CHILD_SUFFIX, LAST_ANCHOR_CODE + CHILD_SUFFIX)));
  }

  /** The fallback reads many anchors' descendants in lookups of at most CONCEPTS_PER_LOOKUP. */
  @Test
  public void testFallbackWithManyAnchorsLooksUpInBatchesOfConceptsPerLookup() {
    stubOneChildPerAnchor();

    StoredHierarchyFallback.fetchDescendantCodes(
        operations, terminology, getCodes(MANY_ANCHORS), null);

    assertEquals(EXPECTED_LOOKUP_SIZES, getLookupSizes(EXPECTED_LOOKUP_COUNT));
  }

  /** Stored paths for many anchors give the ancestors of every one of them. */
  @Test
  public void testStoredPathsWithManyAnchorsFindsEveryAnchorsAncestors() {
    stubOneParentPerAnchor();

    final Set<String> found =
        StoredPaths.fetchAncestorCodes(operations, terminology, getCodes(MANY_ANCHORS), null);

    assertEquals(MANY_ANCHORS, found.size());
    assertTrue(
        found.containsAll(
            Set.of(FIRST_ANCHOR_CODE + PARENT_SUFFIX, LAST_ANCHOR_CODE + PARENT_SUFFIX)));
  }

  /** Stored paths for many anchors are read in lookups of at most CONCEPTS_PER_LOOKUP. */
  @Test
  public void testStoredPathsWithManyAnchorsLooksUpInBatchesOfConceptsPerLookup() {
    stubOneParentPerAnchor();

    StoredPaths.fetchAncestorCodes(operations, terminology, getCodes(MANY_ANCHORS), null);

    assertEquals(EXPECTED_LOOKUP_SIZES, getLookupSizes(EXPECTED_LOOKUP_COUNT));
  }

  /** Stubs every anchor to have exactly one made-up child. */
  private void stubOneChildPerAnchor() {
    stubSearch(
        anchor -> {
          final Concept concept = getConcept(anchor);
          concept.setDescendants(List.of(new Concept(anchor + CHILD_SUFFIX)));
          return concept;
        });
  }

  /** Stubs every anchor to have one stored path to a single made-up parent. */
  private void stubOneParentPerAnchor() {
    stubSearch(
        anchor -> {
          final Concept concept = getConcept(anchor);
          final List<ConceptMinimal> steps =
              List.of(getPathStep(anchor, 0), getPathStep(anchor + PARENT_SUFFIX, 1));
          concept.setPaths(new Paths(List.of(new Path(1, steps))));
          return concept;
        });
  }

  /**
   * Stubs the mock: a lookup by ids returns one concept per id, built by {@code byCode}; any other
   * search (the target code lookup, {@link HasHierarchyRelative#fetchTargetCodes}) returns one page
   * with the matched target.
   *
   * @param byCode builds the stored concept for a code
   */
  private void stubSearch(final Function<String, Concept> byCode) {
    when(operations.search(any(Query.class), eq(Concept.class), any(IndexCoordinates.class)))
        .thenAnswer(
            invocation -> {
              final NativeSearchQuery search = invocation.getArgument(0);

              // The target code lookup matched the target by code; the mock ignores which code
              if (!(search.getQuery() instanceof IdsQueryBuilder ids)) {
                return getPage(List.of(getConcept(MATCHED_TARGET_CODE)));
              }

              return getPage(ids.ids().stream().map(byCode).toList());
            });
  }

  /**
   * Returns the number of ids in each lookup by ids, in order, after checking how many ran.
   *
   * @param expectedLookups how many lookups by ids should have run
   * @return the number of ids in each
   */
  private List<Integer> getLookupSizes(final int expectedLookups) {
    final ArgumentCaptor<Query> searches = ArgumentCaptor.forClass(Query.class);
    verify(operations, times(expectedLookups))
        .search(searches.capture(), eq(Concept.class), any(IndexCoordinates.class));

    return searches.getAllValues().stream()
        .map(search -> ((IdsQueryBuilder) ((NativeSearchQuery) search).getQuery()).ids().size())
        .toList();
  }

  /**
   * Returns made-up codes C0, C1, and so on.
   *
   * @param count how many
   * @return the codes
   */
  private static Set<String> getCodes(final int count) {
    final Set<String> codes = new HashSet<>();
    for (int i = 0; i < count; i++) {
      codes.add("C" + i);
    }
    return codes;
  }

  /**
   * Builds a concept in the made-up terminology version.
   *
   * @param code the code
   * @return the concept
   */
  private static Concept getConcept(final String code) {
    final Concept concept = new Concept(code);
    concept.setTerminology(TERMINOLOGY);
    concept.setVersion(VERSION);
    return concept;
  }

  /**
   * Builds a concept with one stored path to root that holds the given number of ancestors.
   *
   * @param code the concept's code, the first step of the path
   * @param ancestorCount how many ancestors the path holds after the concept itself
   * @return the concept
   */
  private static Concept getConceptWithAncestorCount(final String code, final int ancestorCount) {
    final List<ConceptMinimal> steps = new ArrayList<>();
    steps.add(getPathStep(code, 0));
    int level = 1;
    for (final String ancestorCode : getCodes(ancestorCount)) {
      steps.add(getPathStep(ancestorCode, level++));
    }

    final Concept concept = getConcept(code);
    concept.setPaths(new Paths(List.of(new Path(1, steps))));
    return concept;
  }

  /**
   * Builds one step of a path to root.
   *
   * @param code the step's code
   * @param level its distance from the path's first concept
   * @return the step
   */
  private static ConceptMinimal getPathStep(final String code, final int level) {
    final ConceptMinimal step = new ConceptMinimal(code);
    step.setLevel(level);
    return step;
  }

  /**
   * Builds one page of hits holding the given concepts, sorted by code like the real target code
   * page search.
   *
   * @param concepts the concepts
   * @return the page
   */
  private static SearchHits<Concept> getPage(final Collection<Concept> concepts) {
    final List<SearchHit<Concept>> hits = new ArrayList<>();
    for (final Concept concept : concepts) {
      final Object[] sortValues = {concept.getCode()};
      hits.add(
          new SearchHit<>(
              INDEX_NAME,
              concept.getCode(),
              null,
              0f,
              sortValues,
              Map.of(),
              Map.of(),
              null,
              null,
              List.of(),
              concept));
    }

    return new SearchHitsImpl<>(
        hits.size(),
        TotalHitsRelation.EQUAL_TO,
        0f,
        Duration.ZERO,
        null,
        null,
        hits,
        null,
        null,
        null);
  }
}
