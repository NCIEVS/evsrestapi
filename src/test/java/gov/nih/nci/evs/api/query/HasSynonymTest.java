package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import gov.nih.nci.evs.api.util.ConceptUtils;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * A tutorial for {@link HasSynonym}, which finds concepts by a synonym's name, source, term type,
 * and sub-source, all on the same synonym.
 *
 * <p>Every lesson follows the same steps:
 *
 * <ol>
 *   <li>A small index holds the concepts in {@link #INDEXED_CONCEPTS}, mapped exactly like a real
 *       concept index, so the nested synonyms mapping is tested too.
 *   <li>The lesson builds a query, e.g. concepts with a CDISC synonym named "Malignant Melanoma".
 *   <li>The query runs against the index, and the lesson checks which concept names came back.
 * </ol>
 *
 * <p>The synonyms look like NCIt's but are made up for the tutorial.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HasSynonymTest {

  /** A throwaway index used only by this tutorial. */
  private static final String INDEX_NAME = "evs_has_synonym_tutorial";

  /** Terminology of every tutorial concept. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the concepts in the tutorial index. */
  private static final String VERSION = "25.12e";

  /** One page large enough to hold every indexed concept. */
  private static final Pageable ALL_CONCEPTS = PageRequest.of(0, 100);

  /** Synonym source: NCI. */
  private static final String NCI = "NCI";

  /** Synonym source: CDISC. */
  private static final String CDISC = "CDISC";

  /** Term type: preferred term. */
  private static final String PREFERRED_TERM = "PT";

  /** Term type: synonym. */
  private static final String SYNONYM = "SY";

  /** A CDISC sub-source. */
  private static final String SDTM = "SDTM";

  /**
   * The concepts in the index. "Melanoma" has a CDISC PT and a CDISC SY. "Nevus" has a CDISC
   * synonym and an SY synonym, but not a CDISC SY, which lesson 3 relies on. "Lung Carcinoma"'s
   * CDISC PT has a sub-source. "Skin" has no synonyms.
   */
  private static final List<Concept> INDEXED_CONCEPTS =
      List.of(
          getConcept(
              "C01",
              "Melanoma",
              getSynonym("Melanoma", NCI, PREFERRED_TERM, null),
              getSynonym("MELANOMA, MALIGNANT", CDISC, PREFERRED_TERM, null),
              getSynonym("Malignant Melanoma", CDISC, SYNONYM, null)),
          getConcept(
              "C02",
              "Nevus",
              getSynonym("NEVUS", CDISC, PREFERRED_TERM, null),
              getSynonym("Melanocytic Nevus", NCI, SYNONYM, null)),
          getConcept(
              "C03",
              "Lung Carcinoma",
              getSynonym("Lung Carcinoma", NCI, PREFERRED_TERM, null),
              getSynonym("CARCINOMA OF LUNG", CDISC, PREFERRED_TERM, SDTM)),
          getConcept("C04", "Skin"));

  /** The project's OpenSearch wrapper, which selects the correct operations bean. */
  @Autowired private OpensearchOperationsService operationsService;

  /** The OpenSearch operations, taken from {@link #operationsService}. */
  private OpenSearchOperations operations;

  /** The terminology whose concept index is the tutorial index. */
  private List<Terminology> terminologies;

  /** Creates the index, mapped like a concept index, and loads its concepts. */
  @BeforeAll
  public void setUpIndex() {
    operations = operationsService.getOpenSearchOperations();
    for (final Concept concept : INDEXED_CONCEPTS) {
      concept.setTerminology(TERMINOLOGY);
      concept.setVersion(VERSION);
    }

    final IndexCoordinates index = IndexCoordinates.of(INDEX_NAME);
    final IndexOperations indexOperations = operations.indexOps(index);
    indexOperations.delete();
    indexOperations.create();
    indexOperations.putMapping(Concept.class);
    operations.save(INDEXED_CONCEPTS, index);
    indexOperations.refresh();

    terminologies =
        List.of(QueryTestTerminologies.getTerminology(TERMINOLOGY, VERSION, INDEX_NAME));
  }

  /** Removes the index. */
  @AfterAll
  public void tearDownIndex() {
    operations.indexOps(IndexCoordinates.of(INDEX_NAME)).delete();
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 1: HasSynonym finds concepts by a synonym's name.
  // ---------------------------------------------------------------------------------------------

  /** A name finds the concept whose synonym it is, not just its preferred name. */
  @Test
  public void testSynonymNameFindsConcept() {
    final EvsQuery conceptsCalledMalignantMelanoma = new HasSynonym("Malignant Melanoma");

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(conceptsCalledMalignantMelanoma));
  }

  /**
   * Names are compared normalized, so case and punctuation don't matter: "melanoma malignant" finds
   * "MELANOMA, MALIGNANT".
   */
  @Test
  public void testSynonymNameIgnoresCaseAndPunctuation() {
    final EvsQuery conceptsCalledMelanomaMalignant = new HasSynonym("melanoma malignant");

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(conceptsCalledMelanomaMalignant));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: source, term type, and sub-source narrow which synonyms count.
  // ---------------------------------------------------------------------------------------------

  /** Without a name, every concept with a synonym from the source matches. */
  @Test
  public void testSourceWithoutNameFindsAnySynonymFromSource() {
    final EvsQuery conceptsWithCdiscSynonym = new HasSynonym(null, CDISC, null);

    assertEquals(
        Set.of("Melanoma", "Nevus", "Lung Carcinoma"),
        fetchMatchingNames(conceptsWithCdiscSynonym));
  }

  /** A sub-source picks out synonyms from part of a source. */
  @Test
  public void testSubSourceFindsSynonymsFromSubSource() {
    final EvsQuery conceptsWithSdtmSynonym = new HasSynonym(null, CDISC, null, SDTM);

    assertEquals(Set.of("Lung Carcinoma"), fetchMatchingNames(conceptsWithSdtmSynonym));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: every field must match on the same synonym.
  // ---------------------------------------------------------------------------------------------

  /**
   * Only "Melanoma" has a CDISC SY. "Nevus" has a CDISC synonym and an SY synonym, but they're two
   * different synonyms, so it doesn't match.
   */
  @Test
  public void testSourceAndTermTypeMatchOnSameSynonymEntry() {
    final EvsQuery conceptsWithCdiscSy = new HasSynonym(null, CDISC, SYNONYM);

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(conceptsWithCdiscSy));
  }

  /**
   * The name and term type match on the same synonym too: "Malignant Melanoma" is an SY, so asking
   * for it as a PT finds nothing, though "Melanoma" has PT synonyms.
   */
  @Test
  public void testNameAndTermTypeMatchOnSameSynonymEntry() {
    final EvsQuery conceptsWithMalignantMelanomaPt =
        new HasSynonym("Malignant Melanoma", null, PREFERRED_TERM);

    assertEquals(Set.of(), fetchMatchingNames(conceptsWithMalignantMelanomaPt));
  }

  /**
   * Name, source, and term type together: "MELANOMA, MALIGNANT" is a CDISC PT. Typed with different
   * case and punctuation, it still finds that synonym.
   */
  @Test
  public void testNameSourceAndTermTypeMatchOnSameSynonymEntry() {
    final EvsQuery conceptsWithCdiscPtMelanomaMalignant =
        new HasSynonym("Melanoma Malignant", CDISC, PREFERRED_TERM);

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(conceptsWithCdiscPtMelanomaMalignant));
  }

  /** With no conditions, every concept with any synonym matches; "Skin" has none. */
  @Test
  public void testNoConditionsFindsEveryConceptWithSynonym() {
    final EvsQuery conceptsWithAnySynonym = new HasSynonym(null, null, null);

    assertEquals(
        Set.of("Melanoma", "Nevus", "Lung Carcinoma"), fetchMatchingNames(conceptsWithAnySynonym));
  }

  /** Unlike the name, the source is compared as given: "cdisc" isn't "CDISC". */
  @Test
  public void testSourceIsComparedAsGiven() {
    final EvsQuery conceptsWithLowercaseCdiscSynonym = new HasSynonym(null, "cdisc", null);

    assertEquals(Set.of(), fetchMatchingNames(conceptsWithLowercaseCdiscSynonym));
  }

  /** A name is required when it's the only condition. */
  @Test
  public void testRejectsMissingName() {
    final String missingName = null;
    assertThrows(NullPointerException.class, () -> new HasSynonym(missingName));
  }

  /**
   * Runs the query against the tutorial index.
   *
   * @param query the query to run
   * @return the names of the matching concepts
   */
  private Set<String> fetchMatchingNames(final EvsQuery query) {
    final NativeSearchQuery search =
        new NativeSearchQueryBuilder()
            .withQuery(query.fetchOpenSearchQuery(operations, terminologies))
            .withPageable(ALL_CONCEPTS)
            .build();
    return operations
        .search(search, Concept.class, EvsQuery.getIndexCoordinates(terminologies))
        .stream()
        .map(SearchHit::getContent)
        .map(Concept::getName)
        .collect(Collectors.toSet());
  }

  /**
   * Builds a concept with a normalized name and synonyms.
   *
   * @param code the concept code
   * @param name the preferred name
   * @param synonyms the concept's synonyms
   * @return the concept
   */
  private static Concept getConcept(
      final String code, final String name, final Synonym... synonyms) {
    final Concept concept = new Concept();
    concept.setCode(code);
    concept.setName(name);
    concept.setNormName(ConceptUtils.normalize(name));
    concept.setSynonyms(List.of(synonyms));
    return concept;
  }

  /**
   * Builds a FULL_SYN synonym, normalized as the loader does.
   *
   * @param name the synonym's name
   * @param source the synonym's source
   * @param termType the synonym's term type
   * @param subSource the synonym's sub-source, or null for none
   * @return the synonym
   */
  private static Synonym getSynonym(
      final String name, final String source, final String termType, final String subSource) {
    final Synonym synonym = new Synonym();
    synonym.setName(name);
    synonym.setNormName(ConceptUtils.normalize(name));
    synonym.setSource(source);
    synonym.setTermType(termType);
    synonym.setSubSource(subSource);
    synonym.setType("FULL_SYN");
    synonym.setTypeCode("P90");
    return synonym;
  }
}
