package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
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
import org.opensearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * A tutorial for {@link InSubset}, which finds the members of subsets, and for combining subsets.
 *
 * <p>Every lesson follows the same steps:
 *
 * <ol>
 *   <li>A small index holds the concepts in {@link #INDEXED_CONCEPTS}, mapped exactly like a real
 *       concept index. Members point to their subsets with Concept_In_Subset associations, as in
 *       NCIt.
 *   <li>The lesson builds a query, e.g. {@code new InSubset(SDTM)}.
 *   <li>The query runs against the index, and the lesson checks which concept names came back.
 * </ol>
 *
 * <p>The subset codes and names are NCIt's, but the members are made up for the tutorial.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InSubsetTest {

  /** A throwaway index used only by this tutorial. */
  private static final String INDEX_NAME = "evs_in_subset_tutorial";

  /** Terminology of every tutorial concept. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the tutorial concepts. */
  private static final String VERSION = "25.12e";

  /** One page large enough to hold every indexed concept. */
  private static final Pageable ALL_CONCEPTS = PageRequest.of(0, 100);

  /** The association marking subset membership. */
  private static final String CONCEPT_IN_SUBSET = "Concept_In_Subset";

  /** Code of {@link #CONCEPT_IN_SUBSET}, which NCIt's metadata names as its membership type. */
  private static final String CONCEPT_IN_SUBSET_CODE = "A8";

  /** An association that isn't membership. */
  private static final String RELATED_TO = "Related_To";

  /** Code of {@link #RELATED_TO}. */
  private static final String RELATED_TO_CODE = "A99";

  /** A CDISC subset. */
  private static final String SDTM = "C66830";

  /** Name of {@link #SDTM}. */
  private static final String SDTM_NAME = "CDISC SDTM Terminology";

  /** A CDISC subset. */
  private static final String SEND = "C77526";

  /** Name of {@link #SEND}. */
  private static final String SEND_NAME = "CDISC SEND Terminology";

  /** A subset outside CDISC. */
  private static final String CELLOSAURUS = "C165258";

  /** Name of {@link #CELLOSAURUS}. */
  private static final String CELLOSAURUS_NAME = "Cellosaurus Disease Terminology";

  /** A subset with no members. */
  private static final String EMPTY_SUBSET = "C99999";

  /**
   * The concepts in the index. The subsets are concepts too. "Tumor Finding" points to SDTM, but
   * not as a member.
   */
  private static final List<Concept> INDEXED_CONCEPTS =
      List.of(
          getConcept(
              "C01",
              "Melanoma",
              getMembership(SDTM, SDTM_NAME),
              getMembership(CELLOSAURUS, CELLOSAURUS_NAME)),
          getConcept(
              "C02", "Body Weight", getMembership(SDTM, SDTM_NAME), getMembership(SEND, SEND_NAME)),
          getConcept("C03", "Nevus", getMembership(CELLOSAURUS, CELLOSAURUS_NAME)),
          getConcept("C04", "Skin"),
          getConcept(
              "C05", "Tumor Finding", getAssociation(RELATED_TO, RELATED_TO_CODE, SDTM, SDTM_NAME)),
          getConcept(SDTM, SDTM_NAME),
          getConcept(SEND, SEND_NAME),
          getConcept(CELLOSAURUS, CELLOSAURUS_NAME),
          getConcept(EMPTY_SUBSET, "Empty Subset"));

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
    createIndex();

    terminologies = List.of(getTerminology(Set.of(CONCEPT_IN_SUBSET_CODE)));
  }

  /** Removes the index. */
  @AfterAll
  public void tearDownIndex() {
    operations.indexOps(IndexCoordinates.of(INDEX_NAME)).delete();
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 1: InSubset finds a subset's members.
  // ---------------------------------------------------------------------------------------------

  /** Both SDTM members are found. "Tumor Finding" points to SDTM, but isn't a member. */
  @Test
  public void testInSubsetFindsOnlyMembers() {
    final EvsQuery sdtmMembers = new InSubset(SDTM);

    assertEquals(Set.of("Melanoma", "Body Weight"), fetchMatchingNames(sdtmMembers));
  }

  /** A subset with no members finds nothing. */
  @Test
  public void testEmptySubsetFindsNothing() {
    final EvsQuery emptySubsetMembers = new InSubset(EMPTY_SUBSET);

    assertEquals(Set.of(), fetchMatchingNames(emptySubsetMembers));
  }

  /**
   * Known limitation: a subset's whole multi-word name never matches exactly, because the target's
   * name is stored as separate words. Use the subset's code.
   */
  @Test
  public void testWholeMultiWordSubsetNameFindsNothing() {
    final EvsQuery cellosaurusMembersByWholeName = new InSubset(CELLOSAURUS_NAME);

    assertEquals(Set.of(), fetchMatchingNames(cellosaurusMembersByWholeName));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: combinations of subsets.
  // ---------------------------------------------------------------------------------------------

  /** In any of several subsets: an InSubset per subset, combined with Or. */
  @Test
  public void testInAnyOfSubsetsWithOrFindsMembersOfEach() {
    final EvsQuery sdtmOrCellosaurusMembers =
        new Or(List.of(new InSubset(SDTM), new InSubset(CELLOSAURUS)));

    assertEquals(
        Set.of("Melanoma", "Body Weight", "Nevus"), fetchMatchingNames(sdtmOrCellosaurusMembers));
  }

  /**
   * In every one of several subsets: an InSubset per subset, combined with And. Each membership is
   * its own association, so combining per concept is correct here.
   */
  @Test
  public void testInAllOfSubsets() {
    final EvsQuery sdtmAndCellosaurusMembers =
        new And(List.of(new InSubset(SDTM), new InSubset(CELLOSAURUS)));

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(sdtmAndCellosaurusMembers));
  }

  /** In one subset but not another: And with a Not. */
  @Test
  public void testInOneSubsetButNotAnother() {
    final EvsQuery sdtmMembersOutsideSend =
        new And(List.of(new InSubset(SDTM), new Not(new InSubset(SEND))));

    assertEquals(Set.of("Melanoma"), fetchMatchingNames(sdtmMembersOutsideSend));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: membership is the terminology's own association.
  // ---------------------------------------------------------------------------------------------

  /**
   * The membership association comes from the terminology's metadata. If it named Related_To
   * instead, only "Tumor Finding" would be an SDTM member.
   */
  @Test
  public void testMembershipTypeComesFromMetadata() {
    final List<Terminology> relatedToAsMembership = List.of(getTerminology(Set.of(RELATED_TO)));
    final EvsQuery sdtmMembers = new InSubset(SDTM);

    assertEquals(Set.of("Tumor Finding"), fetchMatchingNames(sdtmMembers, relatedToAsMembership));
  }

  /** A terminology without a membership association has no subsets to search. */
  @Test
  public void testTerminologyWithoutMembershipTypeIsRejected() {
    final List<Terminology> withoutMembershipType = List.of(getTerminology(Set.of()));

    final EvsQuery sdtmMembers = new InSubset(SDTM);

    assertThrows(
        IllegalArgumentException.class,
        () -> sdtmMembers.fetchOpenSearchQuery(operations, withoutMembershipType));
  }

  /** A subset is required. */
  @Test
  public void testRejectsMissingSubset() {
    final String missingCode = null;
    assertThrows(NullPointerException.class, () -> new InSubset(missingCode));
  }

  /**
   * Runs the query against the index it searches.
   *
   * @param query the query to run
   * @return the names of the matching concepts
   */
  private Set<String> fetchMatchingNames(final EvsQuery query) {
    return fetchMatchingNames(query, terminologies);
  }

  /**
   * Runs the query against the tutorial index, searching the given terminologies.
   *
   * @param query the query to run
   * @param searched the terminologies being searched
   * @return the names of the matching concepts
   */
  private Set<String> fetchMatchingNames(final EvsQuery query, final List<Terminology> searched) {
    return fetchMatchingNames(query.fetchOpenSearchQuery(operations, searched));
  }

  /**
   * Runs the OpenSearch query against the tutorial index.
   *
   * @param query the query to run
   * @return the names of the matching concepts
   */
  private Set<String> fetchMatchingNames(final QueryBuilder query) {
    final NativeSearchQuery search =
        new NativeSearchQueryBuilder().withQuery(query).withPageable(ALL_CONCEPTS).build();
    return operations.search(search, Concept.class, IndexCoordinates.of(INDEX_NAME)).stream()
        .map(SearchHit::getContent)
        .map(Concept::getName)
        .collect(Collectors.toSet());
  }

  /**
   * Builds the tutorial terminology.
   *
   * @param membershipTypes the association names or codes marking subset membership
   * @return the terminology
   */
  private static Terminology getTerminology(final Set<String> membershipTypes) {
    final Terminology terminology =
        QueryTestTerminologies.getTerminology(TERMINOLOGY, VERSION, INDEX_NAME);
    final TerminologyMetadata metadata = new TerminologyMetadata();
    metadata.setSubsetMember(membershipTypes);
    terminology.setMetadata(metadata);
    return terminology;
  }

  /** Creates the index mapped like a concept index and loads the concepts into it. */
  private void createIndex() {
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
  }

  /**
   * Builds a concept with a normalized name and the given associations.
   *
   * @param code the concept code
   * @param name the preferred name
   * @param associations the concept's associations
   * @return the concept
   */
  private static Concept getConcept(
      final String code, final String name, final Association... associations) {
    final Concept concept = new Concept();
    concept.setCode(code);
    concept.setName(name);
    concept.setNormName(ConceptUtils.normalize(name));
    concept.setAssociations(List.of(associations));
    return concept;
  }

  /**
   * Builds a membership in a subset.
   *
   * @param subsetCode the subset's code
   * @param subsetName the subset's name
   * @return the association
   */
  private static Association getMembership(final String subsetCode, final String subsetName) {
    return getAssociation(CONCEPT_IN_SUBSET, CONCEPT_IN_SUBSET_CODE, subsetCode, subsetName);
  }

  /**
   * Builds an association.
   *
   * @param type the association's name
   * @param code the association's code
   * @param relatedCode the code of the concept the association points to
   * @param relatedName the name of the concept the association points to
   * @return the association
   */
  private static Association getAssociation(
      final String type, final String code, final String relatedCode, final String relatedName) {
    final Association association = new Association();
    association.setType(type);
    association.setCode(code);
    association.setRelatedCode(relatedCode);
    association.setRelatedName(relatedName);
    return association;
  }
}
