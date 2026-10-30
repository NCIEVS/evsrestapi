package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import java.util.List;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * A tutorial for {@link SearchAsYouType}, using the typeahead examples the search was tuned for.
 *
 * <p>Every lesson follows the same steps:
 *
 * <ol>
 *   <li>A small index holds the names in {@link #INDEXED_NAMES}, in a field mapped as {@code
 *       search_as_you_type}. That mapping makes OpenSearch also index word pairs, word triples, and
 *       word prefixes for each name.
 *   <li>The lesson builds a match for what the user has typed so far, e.g. {@code new
 *       SearchAsYouType("chronic granu")}.
 *   <li>The match runs against the index's name field, and the lesson checks which names came back.
 * </ol>
 *
 * <p>This test makes its own index, so it does not depend on the concept indexes being reindexed.
 * Field matches don't filter by terminology, so every lesson passes an empty list.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchAsYouTypeTest {

  /** A throwaway index used only by this tutorial. */
  private static final IndexCoordinates INDEX =
      IndexCoordinates.of("evs_search_as_you_type_tutorial");

  /** One page large enough to hold every indexed name. */
  private static final Pageable ALL_NAMES = PageRequest.of(0, 100);

  /** The search_as_you_type field every lesson searches. */
  private static final String NAME_FIELD = "name";

  /** Field matches don't filter by terminology. */
  private static final List<Terminology> TERMINOLOGIES = List.of();

  /** The names in the index: the expected results, plus look-alikes that share some words. */
  private static final List<String> INDEXED_NAMES =
      List.of(
          "Chronic granulomatous disease",
          "Chronic granulocytic leukemia",
          "Chronic kidney disease",
          "Subependymal giant cell astrocytoma",
          "Giant cell arteritis",
          "Acute lymphoblastic leukemia",
          "Lymphoblastic lymphoma",
          "Non-Hodgkin lymphoma",
          "Hodgkin lymphoma",
          "Melanoma");

  /** The project's OpenSearch wrapper, which selects the correct operations bean. */
  @Autowired private OpensearchOperationsService operationsService;

  /** The OpenSearch operations, taken from {@link #operationsService}. */
  private OpenSearchOperations operations;

  /** Creates the index and loads {@link #INDEXED_NAMES}. */
  @BeforeAll
  public void setUpIndex() {
    operations = operationsService.getOpenSearchOperations();
    final IndexOperations indexOperations = operations.indexOps(INDEX);
    indexOperations.delete();
    indexOperations.create();
    indexOperations.putMapping(indexOperations.createMapping(TypedName.class));

    final List<TypedName> documents =
        INDEXED_NAMES.stream().map(TypedName::new).collect(Collectors.toList());
    operations.save(documents, INDEX);
    indexOperations.refresh();
  }

  /** Removes the index. */
  @AfterAll
  public void tearDownIndex() {
    operations.indexOps(INDEX).delete();
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 1: the last word may be partial, because the user is still typing it.
  // ---------------------------------------------------------------------------------------------

  /** "chronic granu" finds both names with a word starting with "granu", and not other chronics. */
  @Test
  public void testLastWordMayBePartial() {
    final ValueMatch match = new SearchAsYouType("chronic granu");

    assertEquals(
        Set.of("Chronic granulomatous disease", "Chronic granulocytic leukemia"),
        fetchMatchingNames(match));
  }

  /** A single partial word works the same way. */
  @Test
  public void testSingleWordMayBePartial() {
    final ValueMatch match = new SearchAsYouType("lymphobl");

    assertEquals(
        Set.of("Acute lymphoblastic leukemia", "Lymphoblastic lymphoma"),
        fetchMatchingNames(match));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: every word must appear, but in any order and anywhere in the name.
  // ---------------------------------------------------------------------------------------------

  /** "giant subep" finds the name even though the words are reversed in it. */
  @Test
  public void testWordsMayBeInAnyOrder() {
    final ValueMatch match = new SearchAsYouType("giant subep");

    assertEquals(Set.of("Subependymal giant cell astrocytoma"), fetchMatchingNames(match));
  }

  /**
   * "giant astro" needs both words, so "Giant cell arteritis" is not included even though it starts
   * with "giant".
   */
  @Test
  public void testEveryWordMustAppear() {
    final ValueMatch match = new SearchAsYouType("giant astro");

    assertEquals(Set.of("Subependymal giant cell astrocytoma"), fetchMatchingNames(match));
  }

  /** A word in the middle of a name matches as well as one at the start. */
  @Test
  public void testWordMayBeMidName() {
    final ValueMatch match = new SearchAsYouType("lymphoblastic");

    assertEquals(
        Set.of("Acute lymphoblastic leukemia", "Lymphoblastic lymphoma"),
        fetchMatchingNames(match));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: limits of search-as-you-type.
  // ---------------------------------------------------------------------------------------------

  /**
   * Only the last word may be partial. "chro" is not a whole word in any name, so "chro granu"
   * finds nothing.
   */
  @Test
  public void testEarlierWordsMustBeWhole() {
    final ValueMatch match = new SearchAsYouType("chro granu");

    assertEquals(Set.of(), fetchMatchingNames(match));
  }

  /** Words match from their start only, so "noma" does not find "Melanoma". */
  @Test
  public void testNoMatchInsideAWord() {
    final ValueMatch match = new SearchAsYouType("noma");

    assertEquals(Set.of(), fetchMatchingNames(match));
  }

  /** Case is ignored, because the field's analyzer lowercases both the names and the term. */
  @Test
  public void testCaseIsIgnored() {
    final ValueMatch match = new SearchAsYouType("GIANT SUBEP");

    assertEquals(Set.of("Subependymal giant cell astrocytoma"), fetchMatchingNames(match));
  }

  /**
   * Punctuation is ignored, because the field's analyzer splits words on it. "non-hodgkin" and "non
   * hodgkin" both find "Non-Hodgkin lymphoma", and neither finds "Hodgkin lymphoma".
   */
  @Test
  public void testPunctuationIsIgnored() {
    for (final String incompleteTerm : new String[] {"non-hodgkin", "non hodgkin", "non-hodg"}) {
      final ValueMatch match = new SearchAsYouType(incompleteTerm);

      assertEquals(Set.of("Non-Hodgkin lymphoma"), fetchMatchingNames(match), incompleteTerm);
    }
  }

  /**
   * Runs the match against the name field of the tutorial index.
   *
   * @param match how the name must match
   * @return the names that matched
   */
  @DemonstratesFieldMatch
  private Set<String> fetchMatchingNames(final ValueMatch match) {
    final EvsQuery namesMatching = new FieldMatch(TERMINOLOGIES, NAME_FIELD, match);
    final NativeSearchQuery search =
        new NativeSearchQueryBuilder()
            .withQuery(namesMatching.fetchOpenSearchQuery(operations))
            .withPageable(ALL_NAMES)
            .build();
    return operations.search(search, TypedName.class, INDEX).stream()
        .map(SearchHit::getContent)
        .map(TypedName::getName)
        .collect(Collectors.toSet());
  }

  /** A document in the tutorial index: one name in a search_as_you_type field. */
  public static class TypedName {

    /** The id, which is the name itself. */
    @Id private String id;

    /** The name. */
    @Field(type = FieldType.Search_As_You_Type)
    private String name;

    /** Instantiates an empty {@link TypedName}. */
    public TypedName() {
      // n/a
    }

    /**
     * Instantiates a {@link TypedName}.
     *
     * @param name the name
     */
    public TypedName(final String name) {
      this.id = name;
      this.name = name;
    }

    /**
     * Returns the id.
     *
     * @return the id
     */
    public String getId() {
      return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id
     */
    public void setId(final String id) {
      this.id = id;
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name
     */
    public void setName(final String name) {
      this.name = name;
    }
  }
}
