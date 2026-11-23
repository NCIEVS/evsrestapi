package gov.nih.nci.evs.api.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tutorial for the hierarchy nodes {@link HasAncestor} and {@link HasAncestorOrIs}, run against two
 * throwaway indexes holding the same small hierarchy:
 *
 * <pre>
 * R
 * ├─ A
 * │  ├─ C
 * │  │  └─ E
 * │  │     └─ G
 * │  └─ D
 * │     └─ F      F has two parents, D and B
 * └─ B
 *    └─ F
 * </pre>
 *
 * <p>The <b>indexed</b> copy has {@code ancestors} and {@code ancestorDepths}, as reindexed
 * terminologies do. The <b>legacy</b> copy only has the stored {@code children} and depth-first
 * {@code descendants}, as terminologies indexed before those fields do, so it exercises the
 * fallback. Every lesson checks both copies give the same answer.
 *
 * <p>F is the polyhierarchy case: it is 2 below R through B, but a depth-first walk reaches it
 * through A and D first, at 3. Depth limits must use the shortest distance.
 *
 * <p>Runs against OpenSearch on port 9201.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HasHierarchyRelativeTest {

  /** The hierarchy: each code's children, in order. */
  private static final Map<String, List<String>> CHILDREN = new LinkedHashMap<>();

  static {
    CHILDREN.put("R", List.of("A", "B"));
    CHILDREN.put("A", List.of("C", "D"));
    CHILDREN.put("B", List.of("F"));
    CHILDREN.put("C", List.of("E"));
    CHILDREN.put("D", List.of("F"));
    CHILDREN.put("E", List.of("G"));
    CHILDREN.put("F", List.of());
    CHILDREN.put("G", List.of());
  }

  /** Terminology name shared by both test terminologies. */
  private static final String TERMINOLOGY_NAME = "unit";

  /** Version of the indexed terminology. */
  private static final String INDEXED_VERSION = "1";

  /** Version of the legacy terminology. */
  private static final String LEGACY_VERSION = "2";

  /** Index name of the indexed terminology. */
  private static final String INDEXED_INDEX_NAME = "evs_hierarchy_query_indexed";

  /** Index name of the legacy terminology. */
  private static final String LEGACY_INDEX_NAME = "evs_hierarchy_query_legacy";

  /** Hierarchy fields flag of the indexed terminology. */
  private static final Boolean INDEXED_HIERARCHY_FIELDS = true;

  /** Hierarchy fields flag of the legacy terminology, which predates the flag. */
  private static final Boolean LEGACY_HIERARCHY_FIELDS = null;

  /** Prefix of each concept's name. */
  private static final String CONCEPT_NAME_PREFIX = "Concept ";

  /** The first page of search results. */
  private static final int FIRST_PAGE = 0;

  /** More hits than the test hierarchy has concepts. */
  private static final int MAX_HITS = 100;

  /** Depth of a code's own children. */
  private static final int CHILD_LEVEL = 1;

  /** Level of a path's first step, the code itself. */
  private static final int PATH_START_LEVEL = 0;

  /** Distance of a target from itself. */
  private static final int TARGET_DISTANCE = 0;

  /** Direction of a path to root. */
  private static final int PATH_TO_ROOT_DIRECTION = 1;

  /** Terminology with ancestor fields indexed. */
  private Terminology indexed;

  /** Terminology indexed before ancestor fields existed. */
  private Terminology legacy;

  /** The project's OpenSearch wrapper, which selects the correct operations bean. */
  @Autowired private OpensearchOperationsService operationsService;

  /** The OpenSearch operations. */
  private OpenSearchOperations operations;

  /** Creates both indexes and loads the hierarchy into each. */
  @BeforeAll
  public void setUpIndexes() {
    operations = operationsService.getOpenSearchOperations();
    indexed = getTerminology(INDEXED_VERSION, INDEXED_INDEX_NAME, INDEXED_HIERARCHY_FIELDS);
    legacy = getTerminology(LEGACY_VERSION, LEGACY_INDEX_NAME, LEGACY_HIERARCHY_FIELDS);

    indexHierarchy(indexed, true);
    indexHierarchy(legacy, false);
  }

  /** Removes both indexes. */
  @AfterAll
  public void tearDownIndexes() {
    operations.indexOps(IndexCoordinates.of(indexed.getIndexName())).delete();
    operations.indexOps(IndexCoordinates.of(legacy.getIndexName())).delete();
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 1: HasAncestor matches everything strictly below the target.
  // ---------------------------------------------------------------------------------------------

  /** Everything below A, at any depth, and not A itself. */
  @Test
  public void testHasAncestorMatchesEverythingBelowTarget() {
    assertBoth(() -> new HasAncestor("A"), "C", "D", "E", "F", "G");
  }

  /** A leaf has nothing below it. */
  @Test
  public void testHasAncestorLeafMatchesNothing() {
    assertBoth(() -> new HasAncestor("G"));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: HasAncestorOrIs also matches the target.
  // ---------------------------------------------------------------------------------------------

  /** A and everything below A. */
  @Test
  public void testHasAncestorOrIsIncludesTargetAndEverythingBelow() {
    assertBoth(() -> new HasAncestorOrIs("A"), "A", "C", "D", "E", "F", "G");
  }

  /** A leaf matches only itself. */
  @Test
  public void testHasAncestorOrIsLeafMatchesItself() {
    assertBoth(() -> new HasAncestorOrIs("G"), "G");
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: maxLevel limits depth by shortest distance.
  // ---------------------------------------------------------------------------------------------

  /** maxLevel 1 is the direct children. */
  @Test
  public void testHasAncestorMaxLevelOneMatchesDirectChildren() {
    assertBoth(() -> new HasAncestor("A", 1), "C", "D");
  }

  /**
   * F is 2 below R through B, though a depth-first walk reaches it at 3 through A and D. maxLevel 2
   * must include it, and must leave out E (3 below R) and G (4).
   */
  @Test
  public void testHasAncestorMaxLevelTwoUsesShortestDistance() {
    assertBoth(() -> new HasAncestor("R", 2), "A", "B", "C", "D", "F");
  }

  /** With the target included, the target is depth 0 and always matches. */
  @Test
  public void testHasAncestorOrIsMaxLevelOneIncludesTargetAndChildren() {
    assertBoth(() -> new HasAncestorOrIs("A", 1), "A", "C", "D");
  }

  /** maxLevel must be at least 1. */
  @Test
  public void testHasAncestorMaxLevelZeroThrowsErrors() {
    final int invalidMaxLevel = 0;

    assertThrows(IllegalArgumentException.class, () -> new HasAncestor("A", invalidMaxLevel));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 4: hierarchy nodes compose.
  // ---------------------------------------------------------------------------------------------

  /** Composes inside a raw bool, as other nodes will: below A but not below C. */
  @Test
  public void testHasAncestorInBoolQueryExcludesMustNotBranch() {
    assertRawQueryBoth(
        t ->
            QueryBuilders.boolQuery()
                .must(new HasAncestor("A").fetchOpenSearchQuery(operations, t))
                .mustNot(new HasAncestor("C").fetchOpenSearchQuery(operations, t)),
        "C",
        "D",
        "F");
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 5: several terminologies at once, indexed and legacy mixed.
  // ---------------------------------------------------------------------------------------------

  /** Each terminology's targets match only in its own index, through its own path. */
  @Test
  public void testHasAncestorIndexedAndLegacyTogetherMatchesInEachIndex() {
    final List<Terminology> both = List.of(indexed, legacy);
    final HasAncestor query = new HasAncestor("A");
    final Map<String, Set<String>> byIndex =
        fetchCodesByIndex(both, query.fetchOpenSearchQuery(operations, both));
    final Set<String> expected = Set.of("C", "D", "E", "F", "G");

    assertThat(byIndex.get(indexed.getIndexName())).isEqualTo(expected);
    assertThat(byIndex.get(legacy.getIndexName())).isEqualTo(expected);
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 6: HasDescendant matches everything strictly above the target (the upward counterpart).
  // ---------------------------------------------------------------------------------------------

  /** Everything above G, and not G itself. */
  @Test
  public void testHasDescendantMatchesEverythingAboveTarget() {
    assertBoth(() -> new HasDescendant("G"), "E", "C", "A", "R");
  }

  /** F has two parents, so its ancestors come from both paths. */
  @Test
  public void testHasDescendantPolyhierarchyFollowsEveryPath() {
    assertBoth(() -> new HasDescendant("F"), "D", "B", "A", "R");
  }

  /** The root has nothing above it. */
  @Test
  public void testHasDescendantRootMatchesNothing() {
    assertBoth(() -> new HasDescendant("R"));
  }

  /** HasDescendantOrIs also matches the target. */
  @Test
  public void testHasDescendantOrIsIncludesTargetAndEverythingAbove() {
    assertBoth(() -> new HasDescendantOrIs("G"), "G", "E", "C", "A", "R");
  }

  /** The root matches only itself. */
  @Test
  public void testHasDescendantOrIsRootMatchesItself() {
    assertBoth(() -> new HasDescendantOrIs("R"), "R");
  }

  /** maxLevel 1 is the direct parents. */
  @Test
  public void testHasDescendantMaxLevelOneMatchesDirectParents() {
    assertBoth(() -> new HasDescendant("F", 1), "D", "B");
  }

  /** maxLevel 1 with the target included is the target and its direct parents. */
  @Test
  public void testHasDescendantOrIsMaxLevelOneIncludesTargetAndParents() {
    assertBoth(() -> new HasDescendantOrIs("F", 1), "F", "D", "B");
  }

  /**
   * R is 2 above F through B, though 3 through D and A. maxLevel 2 must include it, by shortest
   * distance.
   */
  @Test
  public void testHasDescendantMaxLevelTwoUsesShortestDistanceForPolyhierarchy() {
    assertBoth(() -> new HasDescendant("F", 2), "D", "B", "A", "R");
  }

  /** maxLevel 2 above G, which has a single path, stops at its parent and grandparent. */
  @Test
  public void testHasDescendantMaxLevelTwoStopsAtGrandparent() {
    assertBoth(() -> new HasDescendant("G", 2), "E", "C");
  }

  /** maxLevel must be at least 1. */
  @Test
  public void testHasDescendantMaxLevelZeroThrowsErrors() {
    final int invalidMaxLevel = 0;

    assertThrows(IllegalArgumentException.class, () -> new HasDescendant("G", invalidMaxLevel));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 7: upward nodes compose too.
  // ---------------------------------------------------------------------------------------------

  /** Common ancestors of G and F, composed in a raw bool. */
  @Test
  public void testHasDescendantInBoolQueryMatchesCommonAncestors() {
    assertRawQueryBoth(
        t ->
            QueryBuilders.boolQuery()
                .must(new HasDescendant("G").fetchOpenSearchQuery(operations, t))
                .must(new HasDescendant("F").fetchOpenSearchQuery(operations, t)),
        "A",
        "R");
  }

  /** Each terminology's targets match only in its own index. */
  @Test
  public void testHasDescendantIndexedAndLegacyTogetherMatchesInEachIndex() {
    final List<Terminology> both = List.of(indexed, legacy);
    final HasDescendant query = new HasDescendant("G");
    final Map<String, Set<String>> byIndex =
        fetchCodesByIndex(both, query.fetchOpenSearchQuery(operations, both));
    final Set<String> expected = Set.of("E", "C", "A", "R");

    assertThat(byIndex.get(indexed.getIndexName())).isEqualTo(expected);
    assertThat(byIndex.get(legacy.getIndexName())).isEqualTo(expected);
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  /**
   * Asserts the query matches exactly the expected codes in the indexed copy and in the legacy
   * copy.
   *
   * @param factory builds the query, which is run against each terminology
   * @param expected the expected codes
   */
  private void assertBoth(final Supplier<EvsQuery> factory, final String... expected) {
    assertRawQueryBoth(t -> factory.get().fetchOpenSearchQuery(operations, t), expected);
  }

  /**
   * Asserts the raw OpenSearch query matches exactly the expected codes in the indexed copy and in
   * the legacy copy.
   *
   * @param factory builds the raw query for one terminology
   * @param expected the expected codes
   */
  private void assertRawQueryBoth(
      final Function<List<Terminology>, QueryBuilder> factory, final String... expected) {
    for (final Terminology terminology : List.of(indexed, legacy)) {
      final List<Terminology> t = List.of(terminology);
      final Set<String> codes = fetchMatchedCodes(t, factory.apply(t));

      assertThat(codes).as(terminology.getIndexName()).containsExactlyInAnyOrder(expected);
    }
  }

  /**
   * Runs a query and returns the matching codes.
   *
   * @param terminologies the terminologies to search
   * @param query the query
   * @return the codes
   */
  private Set<String> fetchMatchedCodes(
      final List<Terminology> terminologies, final QueryBuilder query) {
    return fetchHits(terminologies, query).stream()
        .map(hit -> hit.getContent().getCode())
        .collect(Collectors.toSet());
  }

  /**
   * Runs a query and returns the matching codes grouped by the index they were found in.
   *
   * @param terminologies the terminologies to search
   * @param query the query
   * @return the codes by index name
   */
  private Map<String, Set<String>> fetchCodesByIndex(
      final List<Terminology> terminologies, final QueryBuilder query) {
    final Map<String, Set<String>> byIndex = new HashMap<>();
    for (final SearchHit<Concept> hit : fetchHits(terminologies, query)) {
      byIndex.computeIfAbsent(hit.getIndex(), k -> new TreeSet<>()).add(hit.getContent().getCode());
    }

    return byIndex;
  }

  /**
   * Runs a query and returns the hits.
   *
   * @param terminologies the terminologies to search
   * @param query the query
   * @return the hits
   */
  private List<SearchHit<Concept>> fetchHits(
      final List<Terminology> terminologies, final QueryBuilder query) {
    final IndexCoordinates indexes =
        IndexCoordinates.of(
            terminologies.stream().map(Terminology::getIndexName).toArray(String[]::new));

    return operations
        .search(
            new NativeSearchQueryBuilder()
                .withQuery(query)
                .withPageable(PageRequest.of(FIRST_PAGE, MAX_HITS))
                .build(),
            Concept.class,
            indexes)
        .getSearchHits();
  }

  /**
   * Returns a test terminology.
   *
   * @param version the version
   * @param indexName the index name
   * @param hierarchyFields the hierarchy fields flag
   * @return the terminology
   */
  private static Terminology getTerminology(
      final String version, final String indexName, final Boolean hierarchyFields) {
    final Terminology terminology = new Terminology();
    terminology.setTerminology(TERMINOLOGY_NAME);
    terminology.setVersion(version);
    terminology.setIndexName(indexName);

    final TerminologyMetadata metadata = new TerminologyMetadata();
    metadata.setHierarchy(true);
    metadata.setHierarchyFields(hierarchyFields);
    terminology.setMetadata(metadata);

    return terminology;
  }

  /**
   * Creates a terminology's index with the concept mapping and loads the hierarchy.
   *
   * @param terminology the terminology
   * @param withAncestorFields whether to set ancestors and ancestorDepths
   */
  private void indexHierarchy(final Terminology terminology, final boolean withAncestorFields) {
    final IndexCoordinates index = IndexCoordinates.of(terminology.getIndexName());
    final IndexOperations indexOperations = operations.indexOps(index);
    indexOperations.delete();
    indexOperations.create();
    indexOperations.putMapping(indexOperations.createMapping(Concept.class));

    final List<Concept> concepts = new ArrayList<>();
    for (final String code : CHILDREN.keySet()) {
      concepts.add(getConcept(terminology, code, withAncestorFields));
    }

    operations.save(concepts, index);
    indexOperations.refresh();
  }

  /**
   * Returns a concept as the loader stores it.
   *
   * @param terminology the terminology
   * @param code the code
   * @param withAncestorFields whether to set ancestors and ancestorDepths
   * @return the concept
   */
  private static Concept getConcept(
      final Terminology terminology, final String code, final boolean withAncestorFields) {
    final Concept concept =
        new Concept(terminology.getTerminology(), code, CONCEPT_NAME_PREFIX + code);
    concept.setVersion(terminology.getVersion());
    concept.setNormName(code.toLowerCase());
    concept.setChildren(CHILDREN.get(code).stream().map(Concept::new).collect(Collectors.toList()));
    concept.setDescendants(getDepthFirstDescendants(code));
    concept.setPaths(getPathsToRoot(code));
    if (!withAncestorFields) {
      return concept;
    }

    final Map<String, Integer> depths = getShortestAncestorDepths(code);
    concept.setAncestors(getAncestors(depths));
    concept.setAncestorDepths(getAncestorDepthTokens(depths));

    return concept;
  }

  /**
   * Returns the ancestors, sorted by code, each with its shortest depth as its level.
   *
   * @param depths the ancestors and their depths
   * @return the ancestors
   */
  private static List<Concept> getAncestors(final Map<String, Integer> depths) {
    final List<Concept> ancestors = new ArrayList<>();
    for (final String code : new TreeSet<>(depths.keySet())) {
      final Concept ancestor = new Concept(code);
      ancestor.setLevel(depths.get(code));
      ancestors.add(ancestor);
    }

    return ancestors;
  }

  /**
   * Returns the sorted ancestor depth tokens for a map of ancestor to depth.
   *
   * @param depths the ancestors and their depths
   * @return the tokens
   */
  private static List<String> getAncestorDepthTokens(final Map<String, Integer> depths) {
    return depths.entrySet().stream()
        .map(e -> e.getKey() + HasAncestor.DEPTH_SEPARATOR + e.getValue())
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * Returns a code's descendants as the loader stores them: depth-first, each at the depth the walk
   * first reached it.
   *
   * @param code the code
   * @return the descendants
   */
  private static List<Concept> getDepthFirstDescendants(final String code) {
    final Map<String, Integer> levels = new LinkedHashMap<>();
    walkDepthFirst(code, CHILD_LEVEL, levels);

    return levels.entrySet().stream()
        .map(e -> getDescendant(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  /**
   * Returns a descendant concept at a depth.
   *
   * @param code the code
   * @param level the depth
   * @return the descendant
   */
  private static Concept getDescendant(final String code, final int level) {
    final Concept descendant = new Concept(code);
    descendant.setLevel(level);

    return descendant;
  }

  /**
   * Walks the hierarchy depth-first, keeping the first depth each code is reached at.
   *
   * @param code the code to walk below
   * @param level the depth of its children
   * @param levels the codes reached so far, with their depths
   */
  private static void walkDepthFirst(
      final String code, final int level, final Map<String, Integer> levels) {
    for (final String child : CHILDREN.get(code)) {
      levels.putIfAbsent(child, level);
      walkDepthFirst(child, level + 1, levels);
    }
  }

  /**
   * Returns each code's parents, the inverse of {@link #CHILDREN}.
   *
   * @return the parents by code
   */
  private static Map<String, List<String>> getParentsByCode() {
    final Map<String, List<String>> parents = new HashMap<>();
    CHILDREN.forEach((parent, children) -> addParent(parents, parent, children));

    return parents;
  }

  /**
   * Records a parent against each of its children.
   *
   * @param parents the parents by code, updated
   * @param parent the parent
   * @param children the parent's children
   */
  private static void addParent(
      final Map<String, List<String>> parents, final String parent, final List<String> children) {
    children.forEach(child -> parents.computeIfAbsent(child, k -> new ArrayList<>()).add(parent));
  }

  /**
   * Returns each ancestor of a code with its shortest distance, by walking parents breadth-first.
   *
   * @param code the code
   * @return the ancestors and their depths
   */
  private static Map<String, Integer> getShortestAncestorDepths(final String code) {
    final Map<String, List<String>> parents = getParentsByCode();
    final Map<String, Integer> depths = new LinkedHashMap<>();
    final Deque<String> queue = new ArrayDeque<>(List.of(code));
    final Map<String, Integer> distance = new HashMap<>(Map.of(code, TARGET_DISTANCE));

    while (!queue.isEmpty()) {
      final String current = queue.removeFirst();
      final List<String> currentParents = parents.getOrDefault(current, List.of());
      visitParents(currentParents, distance.get(current), distance, depths, queue);
    }

    return depths;
  }

  /**
   * Visits the parents of a code in the breadth-first walk, recording the first distance each is
   * reached at.
   *
   * @param parents the parents of the current code
   * @param currentDistance the distance of the current code
   * @param distance the distances reached so far, updated
   * @param depths the ancestors and their depths, updated
   * @param queue the codes still to walk, updated
   */
  private static void visitParents(
      final List<String> parents,
      final int currentDistance,
      final Map<String, Integer> distance,
      final Map<String, Integer> depths,
      final Deque<String> queue) {
    for (final String parent : parents) {
      final boolean isAlreadyVisited = distance.containsKey(parent);
      if (isAlreadyVisited) {
        continue;
      }

      distance.put(parent, currentDistance + 1);
      depths.put(parent, currentDistance + 1);
      queue.addLast(parent);
    }
  }

  /**
   * Returns a code's paths to root as the loader stores them: each path starts at the code (level
   * 0) and ends at a root.
   *
   * @param code the code
   * @return the paths
   */
  private static Paths getPathsToRoot(final String code) {
    final List<List<String>> codePaths = new ArrayList<>();
    collectPaths(code, new ArrayList<>(List.of(code)), getParentsByCode(), codePaths);

    final Paths paths = new Paths();
    paths.setPaths(
        codePaths.stream().map(HasHierarchyRelativeTest::getPath).collect(Collectors.toList()));

    return paths;
  }

  /**
   * Returns a path whose steps are the codes in order, each at its level.
   *
   * @param codePath the codes, from the starting code up to a root
   * @return the path
   */
  private static Path getPath(final List<String> codePath) {
    final Path path = new Path();
    path.setDirection(PATH_TO_ROOT_DIRECTION);
    for (int level = PATH_START_LEVEL; level < codePath.size(); level++) {
      final ConceptMinimal step = new ConceptMinimal(codePath.get(level));
      step.setLevel(level);
      path.getConcepts().add(step);
    }

    return path;
  }

  /**
   * Collects every path from a code up to a root.
   *
   * @param code the current code
   * @param soFar the path so far, ending at the current code
   * @param parents each code's parents
   * @param codePaths the completed paths
   */
  private static void collectPaths(
      final String code,
      final List<String> soFar,
      final Map<String, List<String>> parents,
      final List<List<String>> codePaths) {
    final List<String> up = parents.getOrDefault(code, List.of());
    final boolean isRoot = up.isEmpty();
    if (isRoot) {
      codePaths.add(new ArrayList<>(soFar));
      return;
    }

    for (final String parent : up) {
      soFar.add(parent);
      collectPaths(parent, soFar, parents, codePaths);
      soFar.remove(soFar.size() - 1);
    }
  }
}
