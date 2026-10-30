package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.opensearch.common.unit.Fuzziness;
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
 * A tutorial for the value matches: {@link Exact}, {@link StartsWith}, {@link Contains}, and {@link
 * Fuzzy}, plus the {@link CaseInsensitive} wrapper. Each lesson matches the name field with a
 * {@link FieldMatch}.
 *
 * <p>Every lesson follows the same steps:
 *
 * <ol>
 *   <li>A small index holds the names in {@link #INDEXED_NAMES}, stored in a keyword field so each
 *       name is matched as one whole, case-preserved value.
 *   <li>The lesson builds a match, e.g. {@code new Contains("lanoma")}.
 *   <li>The match runs against the index's name field, and the lesson checks which names came back.
 * </ol>
 *
 * <p>Field matches don't filter by terminology, so every lesson passes an empty list.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ValueMatchTest {

  /** A throwaway index used only by this tutorial. */
  private static final IndexCoordinates INDEX = IndexCoordinates.of("evs_value_match_tutorial");

  /** One page large enough to hold every indexed name. */
  private static final Pageable ALL_NAMES = PageRequest.of(0, 100);

  /** The keyword field every lesson searches. */
  private static final String NAME_FIELD = "name";

  /** Field matches don't filter by terminology. */
  private static final List<Terminology> TERMINOLOGIES = List.of();

  /** The names in the index. Note the two spellings of melanoma that differ only by case. */
  private static final List<String> INDEXED_NAMES =
      List.of(
          "Melanoma",
          "melanoma",
          "Malignant Melanoma",
          "Melanocyte",
          "Carcinoma",
          "Stage 1*",
          "NEVUS");

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
    indexOperations.putMapping(indexOperations.createMapping(IndexedName.class));

    final List<IndexedName> documents =
        INDEXED_NAMES.stream().map(IndexedName::new).collect(Collectors.toList());
    operations.save(documents, INDEX);
    indexOperations.refresh();
  }

  /** Removes the index. */
  @AfterAll
  public void tearDownIndex() {
    operations.indexOps(INDEX).delete();
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 1: Exact finds values that are exactly equal to the term.
  // ---------------------------------------------------------------------------------------------

  /** Exact returns only the value that is identical to the term. */
  @Test
  public void testExactFindsIdenticalValue() {
    final ValueMatch match = new Exact("Melanoma");

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(match));
  }

  /** Exact is case sensitive: "melanoma" and "Melanoma" are different values. */
  @Test
  public void testExactIsCaseSensitive() {
    final ValueMatch match = new Exact("melanoma");

    assertEquals(Set.of("melanoma"), fetchMatchingNames(match));
  }

  /** Exact never matches part of a value. "Melan" is not equal to anything in the index. */
  @Test
  public void testExactIgnoresPartialValues() {
    final ValueMatch match = new Exact("Melan");

    assertEquals(Set.of(), fetchMatchingNames(match));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: StartsWith finds values that begin with the term.
  // ---------------------------------------------------------------------------------------------

  /**
   * StartsWith matches the beginning of the whole value, not of each word. "Malignant Melanoma"
   * contains a word starting with "Melano", but the value itself starts with "Malignant".
   */
  @Test
  public void testStartsWithFindsValuesWithPrefix() {
    final ValueMatch match = new StartsWith("Melano");

    assertEquals(Set.of("Melanoma", "Melanocyte"), fetchMatchingNames(match));
  }

  /** StartsWith is case sensitive. */
  @Test
  public void testStartsWithIsCaseSensitive() {
    final ValueMatch match = new StartsWith("melano");

    assertEquals(Set.of("melanoma"), fetchMatchingNames(match));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: Contains finds values that include the term anywhere.
  // ---------------------------------------------------------------------------------------------

  /** Contains matches the term at the start, middle, or end of a value. */
  @Test
  public void testContainsFindsTermAnywhere() {
    final ValueMatch match = new Contains("lanoma");

    assertEquals(Set.of("Melanoma", "melanoma", "Malignant Melanoma"), fetchMatchingNames(match));
  }

  /** Contains is case sensitive, so the lowercase "melanoma" is not included. */
  @Test
  public void testContainsIsCaseSensitive() {
    final ValueMatch match = new Contains("Melanoma");

    assertEquals(Set.of("Melanoma", "Malignant Melanoma"), fetchMatchingNames(match));
  }

  /**
   * Contains treats every character in the term literally. Searching for "*" finds only the value
   * that has an asterisk in it, rather than treating it as a wildcard that matches everything.
   */
  @Test
  public void testContainsTreatsWildcardCharactersLiterally() {
    final ValueMatch match = new Contains("*");

    assertEquals(Set.of("Stage 1*"), fetchMatchingNames(match));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 4: Fuzzy finds values that are within a few typos of the term.
  // ---------------------------------------------------------------------------------------------

  /**
   * Fuzzy tolerates typos. "Carcinomma" is one extra letter away from "Carcinoma". By default the
   * number of allowed typos grows with the length of the term.
   */
  @Test
  public void testFuzzyToleratesTypos() {
    final ValueMatch match = new Fuzzy("Carcinomma");

    assertEquals(Set.of("Carcinoma"), fetchMatchingNames(match));
  }

  /**
   * Fuzzy rejects values that are too different. "Melan" is three letters short of "Melanoma", more
   * than the one typo allowed for a five-letter term.
   */
  @Test
  public void testFuzzyRejectsDistantValues() {
    final ValueMatch match = new Fuzzy("Melan");

    assertEquals(Set.of(), fetchMatchingNames(match));
  }

  /**
   * The number of allowed typos can be set explicitly, and a change of case counts as a typo.
   * "Melanomma" is one typo from "Melanoma" but two from "melanoma", so allowing exactly one typo
   * finds only "Melanoma".
   */
  @Test
  public void testFuzzyWithExplicitTypoLimit() {
    final ValueMatch match = new Fuzzy("Melanomma", Fuzziness.ONE);

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(match));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 5: CaseInsensitive wraps a match so that uppercase and lowercase letters match each
  // other. It works with Exact, StartsWith, and Contains. Wrapping a Fuzzy does not compile.
  // ---------------------------------------------------------------------------------------------

  /** A case-insensitive Exact finds every value equal to the term, whatever its case. */
  @Test
  public void testCaseInsensitiveExact() {
    final ValueMatch match = new CaseInsensitive(new Exact("MELANOMA"));

    assertEquals(Set.of("Melanoma", "melanoma"), fetchMatchingNames(match));
  }

  /** A case-insensitive StartsWith finds every value beginning with the term, whatever its case. */
  @Test
  public void testCaseInsensitiveStartsWith() {
    final ValueMatch match = new CaseInsensitive(new StartsWith("melano"));

    assertEquals(Set.of("Melanoma", "melanoma", "Melanocyte"), fetchMatchingNames(match));
  }

  /** A case-insensitive Contains finds every value including the term, whatever its case. */
  @Test
  public void testCaseInsensitiveContains() {
    final ValueMatch match = new CaseInsensitive(new Contains("MELANOMA"));

    assertEquals(Set.of("Melanoma", "melanoma", "Malignant Melanoma"), fetchMatchingNames(match));
  }

  /**
   * Wrapping a match does not change it. The original match is still case sensitive and can be used
   * on its own.
   */
  @Test
  public void testCaseInsensitiveLeavesWrappedMatchUnchanged() {
    final Exact caseSensitiveMatch = new Exact("melanoma");
    final ValueMatch caseInsensitiveMatch = new CaseInsensitive(caseSensitiveMatch);

    assertEquals(Set.of("melanoma", "Melanoma"), fetchMatchingNames(caseInsensitiveMatch));
    assertEquals(Set.of("melanoma"), fetchMatchingNames(caseSensitiveMatch));
  }

  /**
   * Ignoring case only adds matches. A lowercase value ("melanoma") needs no change of case to
   * match itself, so it still matches when case is ignored.
   */
  @Test
  public void testIgnoringCaseKeepsLowercaseMatches() {
    final String lowercaseValue = "melanoma";

    assertIgnoringCaseKeepsMatches(new Exact(lowercaseValue), lowercaseValue);
    assertIgnoringCaseKeepsMatches(new StartsWith(lowercaseValue), lowercaseValue);
    assertIgnoringCaseKeepsMatches(new Contains(lowercaseValue), lowercaseValue);
  }

  /**
   * Ignoring case only adds matches. An uppercase value ("NEVUS") needs no change of case to match
   * itself, so it still matches when case is ignored.
   */
  @Test
  public void testIgnoringCaseKeepsUppercaseMatches() {
    final String uppercaseValue = "NEVUS";

    assertIgnoringCaseKeepsMatches(new Exact(uppercaseValue), uppercaseValue);
    assertIgnoringCaseKeepsMatches(new StartsWith(uppercaseValue), uppercaseValue);
    assertIgnoringCaseKeepsMatches(new Contains(uppercaseValue), uppercaseValue);
  }

  /**
   * Asserts that the match finds the indexed value, and that wrapping it in {@link CaseInsensitive}
   * still finds everything the match finds on its own.
   *
   * @param match a match whose term is the indexed value
   * @param indexedValue the indexed value
   */
  private void assertIgnoringCaseKeepsMatches(
      final SupportsIgnoringCase match, final String indexedValue) {
    final Set<String> caseSensitiveNames = fetchMatchingNames(match);
    final Set<String> caseInsensitiveNames = fetchMatchingNames(new CaseInsensitive(match));

    assertTrue(caseSensitiveNames.contains(indexedValue));
    assertTrue(caseInsensitiveNames.containsAll(caseSensitiveNames));
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
    return operations.search(search, IndexedName.class, INDEX).stream()
        .map(SearchHit::getContent)
        .map(IndexedName::getName)
        .collect(Collectors.toSet());
  }

  /** A document in the tutorial index: one name stored as a whole, case-preserved value. */
  public static class IndexedName {

    /** The id, which is the name itself. */
    @Id private String id;

    /** The name. */
    @Field(type = FieldType.Keyword)
    private String name;

    /** Instantiates an empty {@link IndexedName}. */
    public IndexedName() {
      // n/a
    }

    /**
     * Instantiates an {@link IndexedName}.
     *
     * @param name the name
     */
    public IndexedName(final String name) {
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
