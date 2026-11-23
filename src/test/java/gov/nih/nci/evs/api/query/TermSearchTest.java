package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import gov.nih.nci.evs.api.support.es.EVSPageable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * A tutorial for the term searches: {@link Exact}, {@link StartsWith}, {@link Contains}, and {@link
 * Fuzzy}. Each is a concept-level node that mirrors the legacy {@code GET /concept/search} with the
 * matching {@code type} ({@code match}, {@code startsWith}, {@code contains}, {@code fuzzy}): it
 * searches a concept's name, code, and synonyms, and ranks the results by relevance.
 *
 * <p>Every lesson follows the same steps:
 *
 * <ol>
 *   <li>The lesson builds a term search, e.g. {@code new Contains("melanoma")}.
 *   <li>The search runs against the test OpenSearch with NCIt loaded, as the concept search tests
 *       do.
 *   <li>The lesson checks which concepts came back, or which came first, as {@code
 *       SearchControllerTests} does for the same search.
 * </ol>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TermSearchTest {

  /** Terminology every lesson searches. */
  private static final String TERMINOLOGY = "ncit";

  /** The NCIt version {@code ncit} resolves to in the test data (the latest monthly). */
  private static final String VERSION = "25.12e";

  /** The concept index of {@link #VERSION}. */
  private static final String INDEX_NAME = "concept_ncit_2512e";

  /** One page as large as OpenSearch's default result window, enough to hold every match. */
  private static final Pageable ALL_MATCHES = PageRequest.of(0, 10_000);

  /** The fields returned for each match. */
  private static final IncludeParam MINIMAL_FIELDS = new IncludeParam("minimal");

  /** Melanoma, the only concept with an NCI preferred term normalized to "melanoma". */
  private static final String MELANOMA = "C3224";

  /** The project's OpenSearch wrapper, which selects the correct operations bean. */
  @Autowired private OpensearchOperationsService operationsService;

  /** The OpenSearch operations, taken from {@link #operationsService}. */
  private OpenSearchOperations operations;

  /** The terminology every lesson searches. */
  private List<Terminology> terminologies;

  /** Takes the operations and builds the NCIt terminology. */
  @BeforeAll
  public void setUpTerminology() {
    operations = operationsService.getOpenSearchOperations();
    terminologies =
        List.of(QueryTestTerminologies.getTerminology(TERMINOLOGY, VERSION, INDEX_NAME));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 1: Exact finds concepts whose whole name or code is the term.
  // ---------------------------------------------------------------------------------------------

  /** Exact ranks the concept whose name is the term first, ignoring case, like type=exact. */
  @Test
  public void testExactRanksConceptWithIdenticalNameFirst() {
    final TermSearch search = new Exact("melanoma");

    assertEquals("melanoma", fetchMatchingConcepts(search).get(0).getName().toLowerCase());
  }

  /** Exact finds a concept by its code. */
  @Test
  public void testExactFindsConceptByCode() {
    final TermSearch search = new Exact(MELANOMA);

    assertEquals(MELANOMA, fetchMatchingConcepts(search).get(0).getCode());
  }

  /** Exact ignores the case of a code, because the term is uppercased before matching codes. */
  @Test
  public void testExactFindsConceptByLowercaseCode() {
    final TermSearch search = new Exact(MELANOMA.toLowerCase());

    assertEquals(MELANOMA, fetchMatchingConcepts(search).get(0).getCode());
  }

  /** Exact ignores case, because names are compared normalized: "MELANOMA" finds Melanoma. */
  @Test
  public void testExactIgnoresCase() {
    final TermSearch search = new Exact("MELANOMA");

    assertTrue(fetchMatchingCodes(search).contains(MELANOMA));
  }

  /** Exact never matches part of a name. "Melan" is not Melanoma's whole name. */
  @Test
  public void testExactIgnoresPartialNames() {
    final TermSearch search = new Exact("Melan");

    assertFalse(fetchMatchingCodes(search).contains(MELANOMA));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: StartsWith finds concepts whose name or code begins with the term.
  // ---------------------------------------------------------------------------------------------

  /** StartsWith finds a concept whose name begins with the term. */
  @Test
  public void testStartsWithFindsConceptWithNamePrefix() {
    final TermSearch search = new StartsWith("Melano");

    assertTrue(fetchMatchingCodes(search).contains(MELANOMA));
  }

  /** StartsWith finds a concept whose code begins with the term. */
  @Test
  public void testStartsWithFindsConceptWithCodePrefix() {
    final TermSearch search = new StartsWith("C322");

    assertTrue(fetchMatchingCodes(search).contains(MELANOMA));
  }

  /** StartsWith ignores case, because names are compared normalized. */
  @Test
  public void testStartsWithIgnoresCase() {
    final TermSearch search = new StartsWith("MELANO");

    assertTrue(fetchMatchingCodes(search).contains(MELANOMA));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: Contains finds concepts whose name includes the term anywhere, or whose code is it.
  // ---------------------------------------------------------------------------------------------

  /** Contains ranks the concept whose name is the term first, like the default search type. */
  @Test
  public void testContainsRanksConceptWithIdenticalNameFirst() {
    final TermSearch search = new Contains("melanoma");

    assertEquals(MELANOMA, fetchMatchingConcepts(search).get(0).getCode());
  }

  /**
   * Contains matches the term at the start of a name or later in it: "melanoma" finds Melanoma
   * itself and concepts with "melanoma" after their first word.
   */
  @Test
  public void testContainsFindsTermAnywhereInName() {
    final TermSearch search = new Contains("melanoma");

    final List<Concept> matches = fetchMatchingConcepts(search);

    final boolean hasMelanoma = matches.stream().map(Concept::getCode).anyMatch(MELANOMA::equals);
    final boolean hasTermAfterFirstWord =
        matches.stream()
            .map(Concept::getName)
            .anyMatch(name -> name.toLowerCase().contains(" melanoma"));
    assertTrue(hasMelanoma);
    assertTrue(hasTermAfterFirstWord);
  }

  /** Contains finds a concept by its code and ranks it first, like the legacy code search. */
  @Test
  public void testContainsRanksConceptWithCodeFirst() {
    final TermSearch search = new Contains(MELANOMA);

    assertEquals(MELANOMA, fetchMatchingConcepts(search).get(0).getCode());
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 4: Fuzzy finds concepts whose name is within a typo of the term.
  // ---------------------------------------------------------------------------------------------

  /** Fuzzy tolerates typos: "enzymi" ranks a name containing "enzyme" first, like type=fuzzy. */
  @Test
  public void testFuzzyRanksTypoCorrectedNameFirst() {
    final TermSearch search = new Fuzzy("enzymi");

    final String firstName = fetchMatchingConcepts(search).get(0).getName();
    assertTrue(firstName.toLowerCase().contains("enzyme"));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 5: A page can start at any record, not only at a multiple of its size.
  // ---------------------------------------------------------------------------------------------

  /**
   * A page from a record that is not a multiple of the page size starts at that record: records
   * 6-15 of the results, the same records a page from the start returns at those positions.
   */
  @Test
  public void testPageFromUnalignedRecordStartsAtThatRecord() {
    final TermSearch search = new Contains("melanoma");
    final int pageSize = 10;
    final int fromRecord = 6;
    final Pageable pageFromRecord = EVSPageable.fromRecord(fromRecord, pageSize);
    final Pageable pageThroughRecords = PageRequest.of(0, fromRecord + pageSize);

    final List<Concept> concepts = fetchMatchingConcepts(search, pageFromRecord);
    final List<Concept> conceptsFromStart = fetchMatchingConcepts(search, pageThroughRecords);

    assertEquals(
        getCodes(conceptsFromStart.subList(fromRecord, fromRecord + pageSize)), getCodes(concepts));
  }

  /**
   * Returns the codes of the concepts, in order.
   *
   * @param concepts the concepts
   * @return the codes
   */
  private static List<String> getCodes(final List<Concept> concepts) {
    return concepts.stream().map(Concept::getCode).toList();
  }

  /**
   * Runs the term search against NCIt.
   *
   * @param search the term search
   * @return the codes of the concepts that matched
   */
  private Set<String> fetchMatchingCodes(final TermSearch search) {
    return fetchMatchingConcepts(search).stream().map(Concept::getCode).collect(Collectors.toSet());
  }

  /**
   * Runs the term search against NCIt.
   *
   * @param search the term search
   * @return the concepts that matched, most relevant first
   */
  private List<Concept> fetchMatchingConcepts(final TermSearch search) {
    return fetchMatchingConcepts(search, ALL_MATCHES);
  }

  /**
   * Runs the term search against NCIt and returns one page of the results.
   *
   * @param search the term search
   * @param page the page to return
   * @return the concepts on the page, most relevant first
   */
  private List<Concept> fetchMatchingConcepts(final TermSearch search, final Pageable page) {
    return search.fetchConcepts(operations, terminologies, page, MINIMAL_FIELDS).stream()
        .map(SearchHit::getContent)
        .toList();
  }
}
