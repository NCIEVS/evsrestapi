package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import gov.nih.nci.evs.api.util.ConceptUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.data.core.OpenSearchOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * A tutorial for {@link Typeahead} ranking, using concepts built to isolate one ranking rule each.
 *
 * <p>Every lesson follows the same steps:
 *
 * <ol>
 *   <li>Small indexes hold the concepts in {@link #INDEXED_CONCEPTS}, {@link
 *       #OTHER_INDEXED_CONCEPTS}, and {@link #LEGACY_INDEXED_CONCEPTS}, mapped exactly like a real
 *       concept index. Each concept gets a normalized name and a chosen number of descendants,
 *       which are the inputs to the ranking.
 *   <li>The lesson runs a typeahead for what the user has typed so far.
 *   <li>The lesson checks the order the matches came back in.
 * </ol>
 *
 * <p>Where a lesson checks an order, the concepts' codes are chosen so that the default code-order
 * tie-break would give a different order. A lesson only passes if its ranking rule works.
 *
 * <p>This test makes its own indexes, so it does not depend on the concept indexes being reindexed
 * or on what the NCI Thesaurus currently contains.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TypeaheadTest {

  /** A throwaway index used by most lessons. */
  private static final String INDEX_NAME = "evs_typeahead_tutorial";

  /** A second throwaway index, standing in for another terminology. */
  private static final String OTHER_INDEX_NAME = "evs_typeahead_tutorial_other";

  /** A throwaway index mapped like the indexes loaded before descendant counts were stored. */
  private static final String LEGACY_INDEX_NAME = "evs_typeahead_tutorial_legacy";

  /** The version of every tutorial terminology. Query nodes need one, but typeahead ignores it. */
  private static final String VERSION = "25.12e";

  /** The field that indexes loaded before descendant counts were stored don't have. */
  private static final String DESCENDANT_COUNT_FIELD = "descendantCount";

  /** One page large enough to hold every indexed concept. */
  private static final Pageable ALL_CONCEPTS = PageRequest.of(0, 100);

  /** The fields to return for each hit. */
  private static final IncludeParam MINIMAL_FIELDS = new IncludeParam("minimal");

  /** The fields to return for each hit, plus OpenSearch's highlights of the matched fields. */
  private static final IncludeParam MINIMAL_FIELDS_AND_HIGHLIGHTS =
      new IncludeParam("minimal,highlights");

  /** Opens a highlighted part. */
  private static final String START = "<em>";

  /** Closes a highlighted part. */
  private static final String END = "</em>";

  /** Reports a match by its name. */
  private static final Function<Concept, String> NAME = Concept::getName;

  /** Reports a match by its code. */
  private static final Function<Concept, String> CODE = Concept::getCode;

  /** Reports a match by its terminology and name, e.g. "other: Retinoblastoma". */
  private static final Function<Concept, String> TERMINOLOGY_AND_NAME =
      concept -> concept.getTerminology() + ": " + concept.getName();

  /**
   * The concepts in the main index. Concepts that share a lesson share a typed word that no other
   * concept in the index has, so each lesson only sees its own concepts.
   */
  private static final List<IndexedConcept> INDEXED_CONCEPTS =
      List.of(
          // Lesson 1: more descendants rank first
          new IndexedConcept("C02", "Lung Carcinoma", 4),
          new IndexedConcept("C01", "Lung Cancer", 0),
          // Lesson 2: fewer words rank first
          new IndexedConcept("C04", "Melanoma", 0),
          new IndexedConcept("C03", "Melanoma of Skin", 0),
          // Lesson 3: names starting with the typed text rank first
          new IndexedConcept("C05", "Blue Nevus", 0),
          new IndexedConcept("C06", "Nevus Spilus", 0),
          new IndexedConcept("C15", "Carcinoma, Basal Cell", 0),
          new IndexedConcept("C16", "Basal Cell Carcinoma", 0),
          // Lesson 4: enough descendants outweigh the starts-with bonus, but only on a log scale
          new IndexedConcept("C07", "Lymphoblastic Lymphoma", 0),
          new IndexedConcept("C08", "Acute Lymphoblastic Leukemia", 20),
          new IndexedConcept("C13", "Diffuse Astrocytic and Oligodendroglial Glioma", 100_000),
          new IndexedConcept("C14", "Glioma", 1_000),
          // Lesson 5: only active concepts, matched on their preferred names
          new IndexedConcept("C17", "Hemangioma of Liver", 0).retire(),
          new IndexedConcept("C18", "Hemangioma", 0),
          new IndexedConcept("C27", "Lymphangioma", 0).clearActiveFlag(),
          new IndexedConcept("C19", "Myocardial Infarction", 0).addSynonym("Heart Attack"),
          // Lesson 6: codes complete too
          new IndexedConcept("C9001", "Fibroma", 0),
          new IndexedConcept("C90011", "Desmoplastic Fibroma", 5),
          new IndexedConcept("C26", "C9 Deficiency", 0),
          // Lesson 7: several terminologies rank together
          new IndexedConcept("C20", "Retinoblastoma", 0),
          new IndexedConcept("C25", "Wilms Tumor", 0),
          // Lesson 9: edge cases
          IndexedConcept.withoutNormalizedName("C09", "Sarcoma"),
          new IndexedConcept("C10", "Kaposi Sarcoma", 0),
          new IndexedConcept("C12", "Adenoma", 0),
          new IndexedConcept("C11", "Adenoma", 0));

  /** The concepts in the other terminology's index, all for lesson 7. */
  private static final List<IndexedConcept> OTHER_INDEXED_CONCEPTS =
      List.of(
          new IndexedConcept("C21", "Retinoblastoma", 5),
          new IndexedConcept("C22", "Trilateral Retinoblastoma", 0),
          new IndexedConcept("C24", "Wilms Tumor", 0));

  /** The concepts in the legacy index, all for lesson 8. */
  private static final List<IndexedConcept> LEGACY_INDEXED_CONCEPTS =
      List.of(
          IndexedConcept.withLeafDescendants("C02", "Lung Carcinoma", 4),
          IndexedConcept.withLeafDescendants("C01", "Lung Cancer", 0));

  /** The project's OpenSearch wrapper, which selects the correct operations bean. */
  @Autowired private OpensearchOperationsService operationsService;

  /** The OpenSearch operations, taken from {@link #operationsService}. */
  private OpenSearchOperations operations;

  /** The terminology whose concept index is {@link #INDEX_NAME}. */
  private Terminology terminology;

  /** The terminology whose concept index is {@link #OTHER_INDEX_NAME}. */
  private Terminology otherTerminology;

  /** The terminology whose concept index is {@link #LEGACY_INDEX_NAME}. */
  private Terminology legacyTerminology;

  /** Creates the indexes, mapped like concept indexes, and loads their concepts. */
  @BeforeAll
  public void setUpIndexes() {
    operations = operationsService.getOpenSearchOperations();
    final Document conceptMapping =
        operations.indexOps(IndexCoordinates.of(INDEX_NAME)).createMapping(Concept.class);

    terminology = createTerminologyIndex(INDEX_NAME, conceptMapping, INDEXED_CONCEPTS);
    otherTerminology =
        createTerminologyIndex(OTHER_INDEX_NAME, conceptMapping, OTHER_INDEXED_CONCEPTS);
    legacyTerminology =
        createTerminologyIndex(
            LEGACY_INDEX_NAME, getMappingWithoutDescendantCount(), LEGACY_INDEXED_CONCEPTS);
  }

  /** Removes the indexes. */
  @AfterAll
  public void tearDownIndexes() {
    for (final String indexName : List.of(INDEX_NAME, OTHER_INDEX_NAME, LEGACY_INDEX_NAME)) {
      operations.indexOps(IndexCoordinates.of(indexName)).delete();
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 0: typeahead needs at least one word to complete.
  // ---------------------------------------------------------------------------------------------

  /** Null, blank, and punctuation-only text has no words. */
  @Test
  public void testTextWithoutWordsHasNoWords() {
    for (final String incompleteTerm : new String[] {null, "", "   ", "---", " , . "}) {
      assertFalse(Typeahead.getHasWordsToComplete(incompleteTerm), "[" + incompleteTerm + "]");
    }
  }

  /** A partial word counts as a word, even with punctuation around it. */
  @Test
  public void testPartialWordHasWords() {
    for (final String incompleteTerm : new String[] {"m", "melan", " -melan- "}) {
      assertTrue(Typeahead.getHasWordsToComplete(incompleteTerm), "[" + incompleteTerm + "]");
    }
  }

  /** A typeahead can't be built for text without words. */
  @Test
  public void testTypeaheadRejectsTextWithoutWords() {
    assertThrows(IllegalArgumentException.class, () -> new Typeahead(List.of(terminology), "---"));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 1: general concepts (more descendants) rank first.
  // ---------------------------------------------------------------------------------------------

  /** Both names have two words and start with "lung ca", so the one with descendants wins. */
  @Test
  public void testMoreDescendantsRankFirst() {
    assertEquals(List.of("Lung Carcinoma", "Lung Cancer"), fetchRankedNames("lung ca"));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: short names rank first.
  // ---------------------------------------------------------------------------------------------

  /** Neither concept has descendants and both start with "melan", so the shorter name wins. */
  @Test
  public void testFewerWordsRankFirst() {
    assertEquals(List.of("Melanoma", "Melanoma of Skin"), fetchRankedNames("melan"));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: names starting with the typed text get a small bonus.
  // ---------------------------------------------------------------------------------------------

  /** Same descendants and word count, so the name starting with "nevus" wins. */
  @Test
  public void testStartsWithRanksFirst() {
    assertEquals(List.of("Nevus Spilus", "Blue Nevus"), fetchRankedNames("nevus"));
  }

  /**
   * The starts-with check compares normalized text, so case and punctuation in what was typed or in
   * the name don't matter. "BASAL-CELL" starts "Basal Cell Carcinoma" but not "Carcinoma, Basal
   * Cell".
   */
  @Test
  public void testStartsWithIgnoresCaseAndPunctuation() {
    assertEquals(
        List.of("Basal Cell Carcinoma", "Carcinoma, Basal Cell"), fetchRankedNames("BASAL-CELL"));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 4: descendants count on a log scale.
  // ---------------------------------------------------------------------------------------------

  /**
   * The starts-with bonus is small. A much more general concept that only contains the typed word
   * still ranks above a name that starts with it, even with an extra word.
   */
  @Test
  public void testDescendantsOutweighStartsWith() {
    assertEquals(
        List.of("Acute Lymphoblastic Leukemia", "Lymphoblastic Lymphoma"),
        fetchRankedNames("lymphoblastic"));
  }

  /**
   * On a log scale, 100 times as many descendants is worth only about 4.6 words. So a concept with
   * a huge hierarchy and a much longer name doesn't bury a general concept with a short name.
   * Counted directly, 100,000 descendants would outrank everything.
   */
  @Test
  public void testHugeHierarchyDoesNotBuryShortName() {
    assertEquals(
        List.of("Glioma", "Diffuse Astrocytic and Oligodendroglial Glioma"),
        fetchRankedNames("glioma"));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 5: only active concepts are suggested, matched on their preferred names.
  // ---------------------------------------------------------------------------------------------

  /** Retired concepts can't be used, so they aren't suggested. */
  @Test
  public void testRetiredConceptsAreNotSuggested() {
    assertEquals(List.of("Hemangioma"), fetchRankedNames("hemangioma"));
  }

  /**
   * A concept without an active flag is still suggested, so terminologies that don't set the flag
   * still complete.
   */
  @Test
  public void testConceptsWithoutActiveFlagAreSuggested() {
    assertEquals(List.of("Lymphangioma"), fetchRankedNames("lymphang"));
  }

  /**
   * Typeahead completes preferred names. Synonyms are for search, so "heart att" doesn't find
   * "Myocardial Infarction" even though "Heart Attack" is one of its synonyms.
   */
  @Test
  public void testSynonymsDoNotMatch() {
    assertEquals(List.of("Myocardial Infarction"), fetchRankedNames("myocardial inf"));
    assertEquals(List.of(), fetchRankedNames("heart att"));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 6: codes complete as well as names.
  // ---------------------------------------------------------------------------------------------

  /** A partial code finds the concepts whose codes start with it. */
  @Test
  public void testPartialCodeCompletes() {
    assertEquals(
        Set.of("C9001", "C90011"), Set.copyOf(fetchRanked(List.of(terminology), "C900", CODE)));
  }

  /** Codes are matched case sensitively, so a lowercase code finds nothing. */
  @Test
  public void testPartialCodeIsCaseSensitive() {
    assertEquals(List.of(), fetchRanked(List.of(terminology), "c900", CODE));
  }

  /**
   * A whole code ranks its concept first, ahead of longer codes that start with it, even more
   * general ones.
   */
  @Test
  public void testWholeCodeRanksFirst() {
    assertEquals(List.of("C9001", "C90011"), fetchRanked(List.of(terminology), "C9001", CODE));
  }

  /**
   * Name matches rank above concepts that only matched by code. "C9" starts "C9 Deficiency" and the
   * codes C9001 and C90011. C90011 would win on descendants, but it only matched by code.
   */
  @Test
  public void testNameMatchesRankAboveCodeOnlyMatches() {
    assertEquals(List.of("C26", "C90011", "C9001"), fetchRanked(List.of(terminology), "C9", CODE));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 7: matches from several terminologies are ranked together.
  // ---------------------------------------------------------------------------------------------

  /** Matches are ranked together, not grouped by terminology. */
  @Test
  public void testTerminologiesRankTogether() {
    assertEquals(
        List.of(
            "other: Retinoblastoma",
            "tutorial: Retinoblastoma",
            "other: Trilateral Retinoblastoma"),
        fetchRanked(
            List.of(terminology, otherTerminology), "retinoblastoma", TERMINOLOGY_AND_NAME));
  }

  /** Matches from different terminologies that rank the same come back in index name order. */
  @Test
  public void testEqualRanksAcrossTerminologiesFallBackToIndexOrder() {
    assertEquals(
        List.of("tutorial: Wilms Tumor", "other: Wilms Tumor"),
        fetchRanked(List.of(otherTerminology, terminology), "wilms", TERMINOLOGY_AND_NAME));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 8: indexes loaded before descendant counts were stored.
  // ---------------------------------------------------------------------------------------------

  /**
   * An index loaded before descendant counts were stored has no descendant count field. Ranking
   * counts each concept's descendants' leaf flags instead, so more descendants still rank first.
   */
  @Test
  public void testLegacyIndexCountsDescendantLeafFlags() {
    assertEquals(
        List.of("Lung Carcinoma", "Lung Cancer"),
        fetchRanked(List.of(legacyTerminology), "lung ca", NAME));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 9: edge cases.
  // ---------------------------------------------------------------------------------------------

  /** A concept without a normalized name can't be ranked, so it comes last. */
  @Test
  public void testMissingNormalizedNameRanksLast() {
    assertEquals(List.of("Kaposi Sarcoma", "Sarcoma"), fetchRankedNames("sarcoma"));
  }

  /** Concepts that rank the same come back in code order, so results are stable. */
  @Test
  public void testEqualRanksFallBackToCodeOrder() {
    assertEquals(List.of("C11", "C12"), fetchRanked(List.of(terminology), "adenoma", CODE));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 10: highlights come from OpenSearch.
  // ---------------------------------------------------------------------------------------------

  /** With highlights included, OpenSearch marks the matched words of the name. */
  @Test
  public void testHighlightsComeFromOpenSearch() {
    final Typeahead typeahead = new Typeahead(List.of(terminology), "lung carc");
    final SearchHit<Concept> lungCarcinoma =
        typeahead.fetchConcepts(operations, ALL_CONCEPTS, MINIMAL_FIELDS_AND_HIGHLIGHTS).stream()
            .filter(hit -> hit.getContent().getCode().equals("C02"))
            .findFirst()
            .orElseThrow();

    final boolean hasMarkedName =
        lungCarcinoma.getHighlightFields().values().stream()
            .flatMap(List::stream)
            .anyMatch(highlight -> highlight.contains(START + "Lung" + END));
    assertTrue(hasMarkedName);
  }

  /**
   * Runs a typeahead against the main tutorial index.
   *
   * @param incompleteTerm the text typed so far
   * @return the names of the matches, best first
   */
  private List<String> fetchRankedNames(final String incompleteTerm) {
    return fetchRanked(List.of(terminology), incompleteTerm, NAME);
  }

  /**
   * Runs a typeahead against the tutorial indexes of the given terminologies.
   *
   * @param terminologies the terminologies to search
   * @param incompleteTerm the text typed so far
   * @param getValue extracts the value to report from each match
   * @return the values of the matches, best first
   */
  private List<String> fetchRanked(
      final List<Terminology> terminologies,
      final String incompleteTerm,
      final Function<Concept, String> getValue) {
    final Typeahead typeahead = new Typeahead(terminologies, incompleteTerm);
    return typeahead.fetchConcepts(operations, ALL_CONCEPTS, MINIMAL_FIELDS).stream()
        .map(SearchHit::getContent)
        .map(getValue)
        .collect(Collectors.toList());
  }

  /**
   * Creates an index and loads concepts into it. Each concept's terminology is named after the
   * index, e.g. "other" for {@link #OTHER_INDEX_NAME}.
   *
   * @param indexName the index name
   * @param mapping the mapping to create the index with
   * @param indexedConcepts the concepts to load
   * @return the terminology whose concept index this is
   */
  private Terminology createTerminologyIndex(
      final String indexName, final Document mapping, final List<IndexedConcept> indexedConcepts) {
    final String terminologyName = indexName.replaceFirst("^evs_typeahead_(tutorial_)?", "");
    final IndexCoordinates index = IndexCoordinates.of(indexName);
    final IndexOperations indexOperations = operations.indexOps(index);
    indexOperations.delete();
    indexOperations.create();
    indexOperations.putMapping(mapping);

    final List<Concept> documents =
        indexedConcepts.stream().map(IndexedConcept::getConcept).collect(Collectors.toList());
    documents.forEach(document -> document.setTerminology(terminologyName));
    operations.save(documents, index);
    indexOperations.refresh();

    return QueryTestTerminologies.getTerminology(terminologyName, VERSION, indexName);
  }

  /**
   * Builds the concept mapping as it was before descendant counts were stored.
   *
   * @return the mapping without the descendant count field
   */
  @SuppressWarnings("unchecked")
  private Document getMappingWithoutDescendantCount() {
    final Document mapping =
        operations.indexOps(IndexCoordinates.of(LEGACY_INDEX_NAME)).createMapping(Concept.class);
    ((Map<String, Object>) mapping.get("properties")).remove(DESCENDANT_COUNT_FIELD);
    return mapping;
  }

  /** A concept in a tutorial index, with the inputs the ranking reads. */
  private static final class IndexedConcept {

    /** The concept to index. */
    private final Concept concept;

    /**
     * Instantiates an active {@link IndexedConcept} with a normalized name.
     *
     * @param code the code
     * @param name the preferred name
     * @param descendantCount the number of descendants
     */
    IndexedConcept(final String code, final String name, final int descendantCount) {
      this(code, name, ConceptUtils.normalize(name));
      concept.setDescendantCount(descendantCount);
    }

    /**
     * Instantiates an active {@link IndexedConcept} without a descendant count.
     *
     * @param code the code
     * @param name the preferred name
     * @param normalizedName the normalized name, or null for none
     */
    private IndexedConcept(final String code, final String name, final String normalizedName) {
      concept = new Concept();
      concept.setCode(code);
      concept.setName(name);
      concept.setNormName(normalizedName);
      concept.setActive(true);
    }

    /**
     * Instantiates an {@link IndexedConcept} without a normalized name and without descendants.
     *
     * @param code the code
     * @param name the preferred name
     * @return the indexed concept
     */
    static IndexedConcept withoutNormalizedName(final String code, final String name) {
      final IndexedConcept indexedConcept = new IndexedConcept(code, name, null);
      indexedConcept.concept.setDescendantCount(0);
      return indexedConcept;
    }

    /**
     * Instantiates an {@link IndexedConcept} the way concepts were loaded before descendant counts
     * were stored: with its descendants listed and no count.
     *
     * @param code the code
     * @param name the preferred name
     * @param descendantCount the number of descendants to list
     * @return the indexed concept
     */
    static IndexedConcept withLeafDescendants(
        final String code, final String name, final int descendantCount) {
      final IndexedConcept indexedConcept =
          new IndexedConcept(code, name, ConceptUtils.normalize(name));
      final List<Concept> descendants = new ArrayList<>();
      for (int i = 0; i < descendantCount; i++) {
        final Concept descendant = new Concept(code + "-" + i);
        descendant.setLeaf(true);
        descendants.add(descendant);
      }
      indexedConcept.concept.setDescendants(descendants);
      return indexedConcept;
    }

    /**
     * Marks the concept retired.
     *
     * @return this
     */
    IndexedConcept retire() {
      concept.setActive(false);
      return this;
    }

    /**
     * Removes the concept's active flag, like a terminology that doesn't set one.
     *
     * @return this
     */
    IndexedConcept clearActiveFlag() {
      concept.setActive(null);
      return this;
    }

    /**
     * Adds a synonym to the concept.
     *
     * @param synonymName the synonym
     * @return this
     */
    IndexedConcept addSynonym(final String synonymName) {
      final Synonym synonym = new Synonym();
      synonym.setName(synonymName);
      concept.getSynonyms().add(synonym);
      return this;
    }

    /**
     * Returns the concept to index.
     *
     * @return the concept
     */
    Concept getConcept() {
      return concept;
    }
  }
}
