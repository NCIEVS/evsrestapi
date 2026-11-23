package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * A tutorial for the logical queries: {@link And}, {@link Or}, {@link Not}, and the {@link Boost}
 * wrapper. They combine other queries into one OpenSearch query.
 *
 * <p>Every lesson runs against a small index of concepts, each with a neoplastic status, a cell
 * origin, or both, combines {@link HasProperty} leaves, and checks which names came back, or in
 * what order.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LogicalQueryTest {

  /** A throwaway index used only by this tutorial. */
  private static final IndexCoordinates INDEX = IndexCoordinates.of("evs_logical_query_tutorial");

  /** One page large enough to hold every indexed concept. */
  private static final Pageable ALL_NAMES = PageRequest.of(0, 100);

  /** The keyword field ranked ties are sorted by. Each concept's code is its name. */
  private static final String CODE_FIELD = "code";

  /** Logical queries don't filter by terminology. */
  private static final List<Terminology> TERMINOLOGIES = List.of();

  /** The property saying whether a concept is malignant or benign. */
  private static final String NEOPLASTIC_STATUS = "Neoplastic_Status";

  /** The property saying which kind of cell a concept comes from. */
  private static final String CELL_ORIGIN = "Cell_Origin";

  /** A {@link #NEOPLASTIC_STATUS} value. */
  private static final String MALIGNANT = "Malignant";

  /** A {@link #NEOPLASTIC_STATUS} value. */
  private static final String BENIGN = "Benign";

  /** A {@link #CELL_ORIGIN} value. */
  private static final String MELANOCYTIC = "Melanocytic";

  /** A {@link #CELL_ORIGIN} value. */
  private static final String EPITHELIAL = "Epithelial";

  /** The concepts in the index, by name, with their properties. Melanocyte has no status. */
  private static final Map<String, List<Property>> PROPERTIES_BY_NAME =
      Map.of(
          "Melanoma",
          List.of(
              new Property(NEOPLASTIC_STATUS, MALIGNANT), new Property(CELL_ORIGIN, MELANOCYTIC)),
          "Malignant Melanoma",
          List.of(
              new Property(NEOPLASTIC_STATUS, MALIGNANT), new Property(CELL_ORIGIN, MELANOCYTIC)),
          "Melanocyte",
          List.of(new Property(CELL_ORIGIN, MELANOCYTIC)),
          "Carcinoma",
          List.of(
              new Property(NEOPLASTIC_STATUS, MALIGNANT), new Property(CELL_ORIGIN, EPITHELIAL)),
          "Nevus",
          List.of(new Property(NEOPLASTIC_STATUS, BENIGN), new Property(CELL_ORIGIN, MELANOCYTIC)));

  /** The project's OpenSearch wrapper, which selects the correct operations bean. */
  @Autowired private OpensearchOperationsService operationsService;

  /** The OpenSearch operations, taken from {@link #operationsService}. */
  private OpenSearchOperations operations;

  /** Creates the index and loads {@link #PROPERTIES_BY_NAME}. */
  @BeforeAll
  public void setUpIndex() {
    operations = operationsService.getOpenSearchOperations();
    final IndexOperations indexOperations = operations.indexOps(INDEX);
    indexOperations.delete();
    indexOperations.create();
    indexOperations.putMapping(indexOperations.createMapping(Concept.class));

    final List<Concept> documents =
        PROPERTIES_BY_NAME.entrySet().stream()
            .map(entry -> getConcept(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    operations.save(documents, INDEX);
    indexOperations.refresh();
  }

  /** Removes the index. */
  @AfterAll
  public void tearDownIndex() {
    operations.indexOps(INDEX).delete();
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 1: And finds what every child finds.
  // ---------------------------------------------------------------------------------------------

  /** Malignant and melanocytic: Carcinoma is malignant but epithelial, Nevus is benign. */
  @Test
  public void testAndNeedsEveryChild() {
    final EvsQuery query =
        new And(
            List.of(
                new HasProperty(NEOPLASTIC_STATUS, MALIGNANT),
                new HasProperty(CELL_ORIGIN, MELANOCYTIC)));

    assertEquals(Set.of("Melanoma", "Malignant Melanoma"), fetchMatchingNames(query));
  }

  /** With no children there's nothing to fail, so everything matches. */
  @Test
  public void testAndWithoutChildrenMatchesEverything() {
    final EvsQuery query = new And(List.of());

    assertEquals(PROPERTIES_BY_NAME.keySet(), fetchMatchingNames(query));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: Or finds what any child finds.
  // ---------------------------------------------------------------------------------------------

  /** Either benign or epithelial. */
  @Test
  public void testOrNeedsAnyChild() {
    final EvsQuery query =
        new Or(
            List.of(
                new HasProperty(NEOPLASTIC_STATUS, BENIGN),
                new HasProperty(CELL_ORIGIN, EPITHELIAL)));

    assertEquals(Set.of("Carcinoma", "Nevus"), fetchMatchingNames(query));
  }

  /**
   * With no children there are no alternatives, so nothing matches. A list of alternatives that
   * came back empty must not widen a search to everything.
   */
  @Test
  public void testOrWithoutChildrenMatchesNothing() {
    final EvsQuery query = new Or(List.of());

    assertEquals(Set.of(), fetchMatchingNames(query));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: Not finds what its child doesn't.
  // ---------------------------------------------------------------------------------------------

  /** Every concept that isn't malignant, including Melanocyte, which has no status at all. */
  @Test
  public void testNotFindsWhatChildDoesNot() {
    final EvsQuery query = new Not(new HasProperty(NEOPLASTIC_STATUS, MALIGNANT));

    assertEquals(Set.of("Melanocyte", "Nevus"), fetchMatchingNames(query));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 4: Boost gives every match a fixed score, so boosted alternatives rank in tiers.
  // ---------------------------------------------------------------------------------------------

  /** The higher-boosted alternative ranks first, then the rest by name. */
  @Test
  public void testBoostRanksAlternativesInTiers() {
    final EvsQuery query = getEpithelialThenMelanocyticQuery();

    assertEquals(
        List.of("Carcinoma", "Malignant Melanoma", "Melanocyte", "Melanoma", "Nevus"),
        fetchRankedNames(query));
  }

  /** Not adds nothing to the score, so requiring it alongside the tiers leaves the order alone. */
  @Test
  public void testNotLeavesRankingUnchanged() {
    final EvsQuery query =
        new And(
            List.of(
                new Not(new HasProperty(NEOPLASTIC_STATUS, BENIGN)),
                getEpithelialThenMelanocyticQuery()));

    assertEquals(
        List.of("Carcinoma", "Malignant Melanoma", "Melanocyte", "Melanoma"),
        fetchRankedNames(query));
  }

  /**
   * Builds a query for epithelial concepts (score 2) or melanocytic concepts (score 1).
   *
   * @return the query
   */
  private EvsQuery getEpithelialThenMelanocyticQuery() {
    final float firstTierScore = 2.0f;
    final float secondTierScore = 1.0f;
    return new Or(
        List.of(
            new Boost(new HasProperty(CELL_ORIGIN, EPITHELIAL), firstTierScore),
            new Boost(new HasProperty(CELL_ORIGIN, MELANOCYTIC), secondTierScore)));
  }

  /**
   * Builds a tutorial concept whose code is its name.
   *
   * @param name the name, also used as the code
   * @param properties the properties
   * @return the concept
   */
  private static Concept getConcept(final String name, final List<Property> properties) {
    final Concept concept = new Concept(name);
    concept.setName(name);
    concept.setProperties(properties);
    return concept;
  }

  /**
   * Runs the query against the tutorial index.
   *
   * @param query the query to run
   * @return the names that matched
   */
  private Set<String> fetchMatchingNames(final EvsQuery query) {
    final NativeSearchQuery search =
        new NativeSearchQueryBuilder()
            .withQuery(query.fetchOpenSearchQuery(operations, TERMINOLOGIES))
            .withPageable(ALL_NAMES)
            .build();
    return operations.search(search, Concept.class, INDEX).stream()
        .map(SearchHit::getContent)
        .map(Concept::getName)
        .collect(Collectors.toSet());
  }

  /**
   * Runs the query against the tutorial index, best score first, then by name.
   *
   * @param query the query to run
   * @return the names that matched, in rank order
   */
  private List<String> fetchRankedNames(final EvsQuery query) {
    final NativeSearchQuery search =
        new NativeSearchQueryBuilder()
            .withQuery(query.fetchOpenSearchQuery(operations, TERMINOLOGIES))
            .withSorts(
                SortBuilders.scoreSort().order(SortOrder.DESC),
                SortBuilders.fieldSort(CODE_FIELD).order(SortOrder.ASC))
            .withPageable(ALL_NAMES)
            .build();
    return operations.search(search, Concept.class, INDEX).stream()
        .map(SearchHit::getContent)
        .map(Concept::getName)
        .collect(Collectors.toList());
  }
}
