package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Property;
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
 * A tutorial for combining {@link HasProperty} and {@link HasSynonym} with the concept-level {@link
 * And}, {@link Or}, and {@link Not}. Each node keeps its own conditions on one entry; the logical
 * nodes combine whole concepts, so two nodes joined by {@code And} may match on different entries,
 * or different lists.
 *
 * <p>Every lesson runs against a small index of the concepts in {@link #INDEXED_CONCEPTS}, mapped
 * exactly like a real concept index, and checks which concept names came back. The data looks like
 * NCIt's but is made up.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CombiningPropertiesAndSynonymsTest {

  /** A throwaway index used only by this tutorial. */
  private static final String INDEX_NAME = "evs_combining_properties_and_synonyms_tutorial";

  /** Terminology of every tutorial concept. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the concepts in the tutorial index. */
  private static final String VERSION = "25.12e";

  /** One page large enough to hold every indexed concept. */
  private static final Pageable ALL_CONCEPTS = PageRequest.of(0, 100);

  /** Property saying whether a neoplasm is benign or malignant. */
  private static final String NEOPLASTIC_STATUS = "Neoplastic_Status";

  /** Property holding a concept's ICD-O-3 code. */
  private static final String ICD_O_3_CODE = "ICD-O-3_Code";

  /** A neoplastic status value. */
  private static final String MALIGNANT = "Malignant";

  /** A neoplastic status value. */
  private static final String BENIGN = "Benign";

  /** Synonym source: NCI. */
  private static final String NCI = "NCI";

  /** Synonym source: CDISC. */
  private static final String CDISC = "CDISC";

  /** Synonym source: FDA. */
  private static final String FDA = "FDA";

  /** Term type: preferred term. */
  private static final String PREFERRED_TERM = "PT";

  /** Term type: synonym. */
  private static final String SYNONYM = "SY";

  /**
   * The concepts in the index. "Melanoma" and "Lung Carcinoma" are Malignant, "Nevus" is Benign.
   * Only "Lung Carcinoma" has an ICD-O-3 code. "Melanoma" has a CDISC PT and an NCI SY, but no
   * CDISC SY. "Nevus" has a CDISC PT, "Lung Carcinoma" an FDA PT, and "Skin" has no properties or
   * synonyms.
   */
  private static final List<Concept> INDEXED_CONCEPTS =
      List.of(
          getConcept(
              "C01",
              "Melanoma",
              List.of(getProperty(NEOPLASTIC_STATUS, MALIGNANT)),
              List.of(
                  getSynonym("MELANOMA, MALIGNANT", CDISC, PREFERRED_TERM),
                  getSynonym("Malignant Melanoma", NCI, SYNONYM))),
          getConcept(
              "C02",
              "Nevus",
              List.of(getProperty(NEOPLASTIC_STATUS, BENIGN)),
              List.of(getSynonym("NEVUS", CDISC, PREFERRED_TERM))),
          getConcept(
              "C03",
              "Lung Carcinoma",
              List.of(
                  getProperty(NEOPLASTIC_STATUS, MALIGNANT), getProperty(ICD_O_3_CODE, "8046/3")),
              List.of(
                  getSynonym("Lung Carcinoma", NCI, PREFERRED_TERM),
                  getSynonym("CARCINOMA OF LUNG", FDA, PREFERRED_TERM))),
          getConcept("C04", "Skin", List.of(), List.of()));

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
  // Lesson 1: And, Or, and Not combine whole concepts.
  // ---------------------------------------------------------------------------------------------

  /** And of two properties needs both: only "Lung Carcinoma" is Malignant with an ICD-O-3 code. */
  @Test
  public void testAndOfPropertiesRequiresEachProperty() {
    final EvsQuery malignantWithIcdO3Code =
        new And(
            List.of(new HasProperty(NEOPLASTIC_STATUS, MALIGNANT), new HasProperty(ICD_O_3_CODE)));

    assertEquals(Set.of("Lung Carcinoma"), fetchMatchingNames(malignantWithIcdO3Code));
  }

  /** Or of two synonym nodes finds a synonym from either source. */
  @Test
  public void testOrOfSynonymsFindsSynonymFromEitherSource() {
    final EvsQuery withCdiscOrFdaSynonym =
        new Or(List.of(new HasSynonym(null, CDISC, null), new HasSynonym(null, FDA, null)));

    assertEquals(
        Set.of("Melanoma", "Nevus", "Lung Carcinoma"), fetchMatchingNames(withCdiscOrFdaSynonym));
  }

  /**
   * Not of a synonym node finds concepts with no such synonym, including "Skin", which has none.
   */
  @Test
  public void testNotOfSynonymFindsConceptsWithoutMatchingSynonym() {
    final EvsQuery withoutCdiscPt = new Not(new HasSynonym(null, CDISC, PREFERRED_TERM));

    assertEquals(Set.of("Lung Carcinoma", "Skin"), fetchMatchingNames(withoutCdiscPt));
  }

  /** And combines a synonym and a property: Malignant concepts with a CDISC PT. */
  @Test
  public void testAndOfSynonymAndPropertyCombinesLists() {
    final EvsQuery malignantWithCdiscPt =
        new And(
            List.of(
                new HasSynonym(null, CDISC, PREFERRED_TERM),
                new HasProperty(NEOPLASTIC_STATUS, MALIGNANT)));

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(malignantWithCdiscPt));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: conditions that must share an entry belong in one node.
  // ---------------------------------------------------------------------------------------------

  /**
   * And of two synonym nodes may match different synonyms: "Melanoma" has a CDISC synonym (a PT)
   * and an SY synonym (from NCI), so it matches.
   */
  @Test
  public void testAndOfSynonymsMayMatchDifferentSynonyms() {
    final EvsQuery withCdiscSynonymAndSySynonym =
        new And(List.of(new HasSynonym(null, CDISC, null), new HasSynonym(null, null, SYNONYM)));

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(withCdiscSynonymAndSySynonym));
  }

  /** One synonym node keeps its fields on one synonym: no concept has a CDISC SY. */
  @Test
  public void testSingleSynonymNodeKeepsFieldsOnOneSynonym() {
    final EvsQuery withCdiscSy = new HasSynonym(null, CDISC, SYNONYM);

    assertEquals(Set.of(), fetchMatchingNames(withCdiscSy));
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
   * Builds a concept with a normalized name, properties, and synonyms.
   *
   * @param code the concept code
   * @param name the preferred name
   * @param properties the concept's properties
   * @param synonyms the concept's synonyms
   * @return the concept
   */
  private static Concept getConcept(
      final String code,
      final String name,
      final List<Property> properties,
      final List<Synonym> synonyms) {
    final Concept concept = new Concept();
    concept.setCode(code);
    concept.setName(name);
    concept.setNormName(ConceptUtils.normalize(name));
    concept.setProperties(properties);
    concept.setSynonyms(synonyms);
    return concept;
  }

  /**
   * Builds a property without qualifiers.
   *
   * @param type the property type's name
   * @param value the property's value
   * @return the property
   */
  private static Property getProperty(final String type, final String value) {
    final Property property = new Property();
    property.setType(type);
    property.setValue(value);
    return property;
  }

  /**
   * Builds a FULL_SYN synonym, normalized as the loader does.
   *
   * @param name the synonym's name
   * @param source the synonym's source
   * @param termType the synonym's term type
   * @return the synonym
   */
  private static Synonym getSynonym(final String name, final String source, final String termType) {
    final Synonym synonym = new Synonym();
    synonym.setName(name);
    synonym.setNormName(ConceptUtils.normalize(name));
    synonym.setSource(source);
    synonym.setTermType(termType);
    synonym.setType("FULL_SYN");
    synonym.setTypeCode("P90");
    return synonym;
  }
}
