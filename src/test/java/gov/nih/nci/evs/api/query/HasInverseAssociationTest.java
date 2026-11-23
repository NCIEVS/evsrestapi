package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import gov.nih.nci.evs.api.util.ConceptUtils;
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
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * A tutorial for {@link HasInverseAssociation}, which finds concepts by the associations other
 * concepts hold that point to them. Read {@link HasAssociationTest} first: type and target matching
 * work the same way, so this tutorial mirrors its lessons from the other end, then covers what only
 * the inverse direction has.
 *
 * <p>An inverse association is an association seen from the concept it points to. When Cutaneous
 * Melanoma's associations include "Concept_In_Subset: CTRP Disease Terminology", the subset's
 * inverse associations include "Concept_In_Subset: Cutaneous Melanoma". The type stays the same,
 * and the inverse association's target is the concept holding the association, here called its
 * source. So {@code getHasInverseAssociation(CUTANEOUS_MELANOMA)} finds the subsets Cutaneous
 * Melanoma is in.
 *
 * <p>Every lesson follows the same steps:
 *
 * <ol>
 *   <li>A small index holds the concepts in {@link #INDEXED_CONCEPTS}, mapped exactly like a real
 *       concept index, so the nested inverse associations mapping is tested too. Each concept's
 *       inverse associations are built from the other concepts' associations, the way the loader
 *       builds them.
 *   <li>The lesson builds a query, e.g. the subsets Cutaneous Melanoma is in.
 *   <li>The query runs against the index, and the lesson checks which concept names came back.
 * </ol>
 *
 * <p>The association names and codes are made up for the tutorial. They look like NCIt's but don't
 * need to match it.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HasInverseAssociationTest {

  /** A throwaway index used only by this tutorial. */
  private static final String INDEX_NAME = "evs_has_inverse_association_tutorial";

  /** Terminology of every tutorial concept. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the concepts in the tutorial index. */
  private static final String VERSION = "25.12e";

  /** One page large enough to hold every indexed concept. */
  private static final Pageable ALL_CONCEPTS = PageRequest.of(0, 100);

  /** Association linking a concept to a subset it belongs to. */
  private static final String CONCEPT_IN_SUBSET = "Concept_In_Subset";

  /** Code of {@link #CONCEPT_IN_SUBSET}. */
  private static final String CONCEPT_IN_SUBSET_CODE = "A8";

  /** Role linking a disease to where it primarily occurs. */
  private static final String PRIMARY_SITE = "Disease_Has_Primary_Anatomic_Site";

  /** Code of {@link #PRIMARY_SITE}. */
  private static final String PRIMARY_SITE_CODE = "R101";

  /** Disease: Cutaneous Melanoma, in {@link #CTRP_DISEASES}, primary site Skin. */
  private static final String CUTANEOUS_MELANOMA = "C01";

  /** Disease: Lung Carcinoma, in {@link #CELLOSAURUS_DISEASES}, primary site Lung. */
  private static final String LUNG_CARCINOMA = "C02";

  /** Disease: Basal Cell Carcinoma of Skin, in {@link #CTRP_DISEASES}, primary site Skin. */
  private static final String BASAL_CELL_CARCINOMA = "C03";

  /** Subset: the CTRP Terminology, which holds other subsets. */
  private static final String CTRP_TERMINOLOGY = "C116978";

  /** Subset: CTRP's disease terms, itself in {@link #CTRP_TERMINOLOGY}. */
  private static final String CTRP_DISEASES = "C116977";

  /** Subset: Cellosaurus's disease terms. In no other subset. */
  private static final String CELLOSAURUS_DISEASES = "C165258";

  /** Made-up subset of anatomic sites. Holds Skin, and is in no other subset. */
  private static final String SITES_OF_INTEREST = "C99002";

  /** Anatomic site: Skin, in {@link #SITES_OF_INTEREST}. */
  private static final String SKIN = "C12470";

  /** Anatomic site: Lung. In no subset. */
  private static final String LUNG = "C12468";

  /**
   * The concepts in the index: {@link HasAssociationTest}'s concepts, plus Basal Cell Carcinoma of
   * Skin, so the CTRP disease subset has two members. Their associations are written out, and their
   * inverse associations are built from those associations. That gives the CTRP disease subset
   * inverse associations from Cutaneous Melanoma and Basal Cell Carcinoma of Skin, the Cellosaurus
   * subset one from Lung Carcinoma, the CTRP Terminology one from the CTRP disease subset, and
   * Anatomic Sites of Interest one from Skin.
   */
  private static final List<Concept> INDEXED_CONCEPTS =
      getConceptsWithInverseAssociations(
          List.of(
              getConcept(
                  CUTANEOUS_MELANOMA,
                  "Cutaneous Melanoma",
                  List.of(getPrimarySite(SKIN, "Skin")),
                  List.of(getSubsetMembership(CTRP_DISEASES, "CTRP Disease Terminology"))),
              getConcept(
                  LUNG_CARCINOMA,
                  "Lung Carcinoma",
                  List.of(getPrimarySite(LUNG, "Lung")),
                  List.of(
                      getSubsetMembership(
                          CELLOSAURUS_DISEASES, "Cellosaurus Disease Terminology"))),
              getConcept(
                  BASAL_CELL_CARCINOMA,
                  "Basal Cell Carcinoma of Skin",
                  List.of(getPrimarySite(SKIN, "Skin")),
                  List.of(getSubsetMembership(CTRP_DISEASES, "CTRP Disease Terminology"))),
              getConcept(
                  CTRP_DISEASES,
                  "CTRP Disease Terminology",
                  List.of(),
                  List.of(getSubsetMembership(CTRP_TERMINOLOGY, "CTRP Terminology"))),
              getConcept(
                  SKIN,
                  "Skin",
                  List.of(),
                  List.of(getSubsetMembership(SITES_OF_INTEREST, "Anatomic Sites of Interest"))),
              getConcept(LUNG, "Lung", List.of(), List.of()),
              getConcept(CTRP_TERMINOLOGY, "CTRP Terminology", List.of(), List.of()),
              getConcept(
                  CELLOSAURUS_DISEASES, "Cellosaurus Disease Terminology", List.of(), List.of()),
              getConcept(SITES_OF_INTEREST, "Anatomic Sites of Interest", List.of(), List.of())));

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
  // Lesson 1: HasInverseAssociation finds the concepts an association of the given type points to.
  // ---------------------------------------------------------------------------------------------

  /**
   * Without a source, every subset with any member is found. The members hold Concept_In_Subset
   * associations, but those are their associations, not inverse associations, so they don't match.
   */
  @Test
  public void testAssociationTypeWithoutSourceFindsAnyConceptPointedTo() {
    final EvsQuery subsetsWithAnyMember = new HasInverseAssociation(CONCEPT_IN_SUBSET);

    assertEquals(
        Set.of(
            "CTRP Disease Terminology",
            "CTRP Terminology",
            "Cellosaurus Disease Terminology",
            "Anatomic Sites of Interest"),
        fetchMatchingNames(subsetsWithAnyMember));
  }

  /** The association's code finds the same concepts as its name. */
  @Test
  public void testAssociationCodeMatchesLikeName() {
    final EvsQuery subsetsWithAnyMemberByName = new HasInverseAssociation(CONCEPT_IN_SUBSET);
    final EvsQuery subsetsWithAnyMemberByCode = new HasInverseAssociation(CONCEPT_IN_SUBSET_CODE);

    assertEquals(
        fetchMatchingNames(subsetsWithAnyMemberByName),
        fetchMatchingNames(subsetsWithAnyMemberByCode));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: the source is a name or code, as the target is for HasAssociation.
  // ---------------------------------------------------------------------------------------------

  /** A source is compared against the source's code: the subsets Cutaneous Melanoma is in. */
  @Test
  public void testSourceMatchedByCode() {
    final EvsQuery subsetsContainingCutaneousMelanoma =
        getHasInverseAssociation(CUTANEOUS_MELANOMA);

    assertEquals(
        Set.of("CTRP Disease Terminology"), fetchMatchingNames(subsetsContainingCutaneousMelanoma));
  }

  /** Lung holds no associations, so no subset contains it. */
  @Test
  public void testSourceWithoutAssociationsFindsNothing() {
    final EvsQuery subsetsContainingLung = getHasInverseAssociation(LUNG);

    assertEquals(Set.of(), fetchMatchingNames(subsetsContainingLung));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: inverse associations, associations, and roles are separate lists, but compose.
  // ---------------------------------------------------------------------------------------------

  /** HasInverseAssociation doesn't search roles: no inverse association is named like a role. */
  @Test
  public void testRoleTypeFindsNoInverseAssociations() {
    final EvsQuery conceptsPointedToByPrimarySiteAssociation =
        new HasInverseAssociation(PRIMARY_SITE);

    assertEquals(Set.of(), fetchMatchingNames(conceptsPointedToByPrimarySiteAssociation));
  }

  /**
   * HasAssociation doesn't follow inverse associations: no concept is in Cutaneous Melanoma, so
   * asking for concepts in it finds nothing, though the CTRP disease subset has an inverse
   * association from it.
   */
  @Test
  public void testAssociationDoesNotSearchInverseAssociations() {
    final EvsQuery conceptsInCutaneousMelanoma =
        new HasAssociation(CONCEPT_IN_SUBSET, CUTANEOUS_MELANOMA);

    assertEquals(Set.of(), fetchMatchingNames(conceptsInCutaneousMelanoma));
  }

  /**
   * HasInverseAssociation doesn't search associations: Anatomic Sites of Interest holds no
   * associations, so nothing has an inverse association from it, though Skin's association points
   * to it.
   */
  @Test
  public void testInverseAssociationDoesNotSearchAssociations() {
    final EvsQuery subsetsContainingSitesOfInterest = getHasInverseAssociation(SITES_OF_INTEREST);

    assertEquals(Set.of(), fetchMatchingNames(subsetsContainingSitesOfInterest));
  }

  /**
   * Subsets with any member that are themselves in the CTRP Terminology: CTRP Disease Terminology.
   * One condition is on the concept's inverse associations, the other on its associations. Combined
   * with a plain bool query until the And node exists.
   */
  @Test
  public void testCombinesWithHasAssociation() {
    final EvsQuery hasAnyMember = new HasInverseAssociation(CONCEPT_IN_SUBSET);
    final EvsQuery isInCtrpTerminology = new HasAssociation(CONCEPT_IN_SUBSET, CTRP_TERMINOLOGY);
    final QueryBuilder nonEmptySubsetsInCtrpTerminology =
        QueryBuilders.boolQuery()
            .must(hasAnyMember.fetchOpenSearchQuery(operations, terminologies))
            .must(isInCtrpTerminology.fetchOpenSearchQuery(operations, terminologies));

    assertEquals(
        Set.of("CTRP Disease Terminology"), fetchMatchingNames(nonEmptySubsetsInCtrpTerminology));
  }

  /** An association type is required. */
  @Test
  public void testMissingAssociationTypeThrowsErrors() {
    assertThrows(NullPointerException.class, () -> new HasInverseAssociation(null));
  }

  /**
   * Builds a {@link HasInverseAssociation} finding the subsets one concept is in, given by its
   * exact code.
   *
   * @param sourceCode the code of the concept holding the subset associations
   * @return the query
   */
  private HasInverseAssociation getHasInverseAssociation(final String sourceCode) {
    return new HasInverseAssociation(CONCEPT_IN_SUBSET, sourceCode);
  }

  /**
   * Runs the query against the tutorial index.
   *
   * @param query the query to run
   * @return the names of the matching concepts
   */
  private Set<String> fetchMatchingNames(final EvsQuery query) {
    return fetchMatchingNames(query.fetchOpenSearchQuery(operations, terminologies));
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
   * Gives each concept the inverse associations of the associations pointing to it, the way the
   * loader does: the same type and code, pointing back to the concept holding the association.
   *
   * @param concepts the concepts, with their associations set
   * @return the same concepts, with their inverse associations set
   */
  private static List<Concept> getConceptsWithInverseAssociations(final List<Concept> concepts) {
    final Map<String, Concept> conceptsByCode =
        concepts.stream().collect(Collectors.toMap(Concept::getCode, Function.identity()));
    for (final Concept source : concepts) {
      addInverseAssociationsOf(source, conceptsByCode);
    }
    return concepts;
  }

  /**
   * Adds an inverse association to each indexed concept the source's associations point to.
   * Associations pointing to a concept that isn't indexed have nowhere to go.
   *
   * @param source the concept holding the associations
   * @param conceptsByCode the indexed concepts
   */
  private static void addInverseAssociationsOf(
      final Concept source, final Map<String, Concept> conceptsByCode) {
    for (final Association association : source.getAssociations()) {
      final Concept target = conceptsByCode.get(association.getRelatedCode());
      final boolean isTargetIndexed = target != null;
      if (isTargetIndexed) {
        target
            .getInverseAssociations()
            .add(
                getAssociation(
                    association.getType(),
                    association.getCode(),
                    source.getCode(),
                    source.getName()));
      }
    }
  }

  /**
   * Builds a concept with a normalized name, roles, and associations.
   *
   * @param code the concept code
   * @param name the preferred name
   * @param roles the concept's roles
   * @param associations the concept's associations
   * @return the concept
   */
  private static Concept getConcept(
      final String code,
      final String name,
      final List<Role> roles,
      final List<Association> associations) {
    final Concept concept = new Concept();
    concept.setCode(code);
    concept.setName(name);
    concept.setNormName(ConceptUtils.normalize(name));
    concept.setRoles(roles);
    concept.setAssociations(associations);
    return concept;
  }

  /**
   * Builds a primary site role.
   *
   * @param siteCode the code of the site
   * @param siteName the name of the site
   * @return the role
   */
  private static Role getPrimarySite(final String siteCode, final String siteName) {
    final Role role = new Role();
    role.setType(PRIMARY_SITE);
    role.setCode(PRIMARY_SITE_CODE);
    role.setRelatedCode(siteCode);
    role.setRelatedName(siteName);
    return role;
  }

  /**
   * Builds an association placing a concept in a subset.
   *
   * @param subsetCode the code of the subset
   * @param subsetName the name of the subset
   * @return the association
   */
  private static Association getSubsetMembership(final String subsetCode, final String subsetName) {
    return getAssociation(CONCEPT_IN_SUBSET, CONCEPT_IN_SUBSET_CODE, subsetCode, subsetName);
  }

  /**
   * Builds an association or inverse association.
   *
   * @param type the association's name
   * @param code the association's code
   * @param relatedCode the code of the concept at the other end
   * @param relatedName the name of the concept at the other end
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
