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
 * A tutorial for {@link HasAssociation}, which finds concepts by association type and association
 * target. It works like {@link HasRole} (see {@link HasRoleTest} for type and target matching in
 * depth), but searches a concept's associations instead of its roles.
 *
 * <p>Every lesson follows the same steps:
 *
 * <ol>
 *   <li>A small index holds the concepts in {@link #INDEXED_CONCEPTS}, mapped exactly like a real
 *       concept index, so the nested associations mapping is tested too.
 *   <li>The lesson builds a query, e.g. concepts in the CTRP Disease Terminology subset.
 *   <li>The query runs against the index, and the lesson checks which concept names came back.
 * </ol>
 *
 * <p>The association names and codes are made up for the tutorial. They look like NCIt's but don't
 * need to match it.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HasAssociationTest {

  /** A throwaway index used only by this tutorial. */
  private static final String INDEX_NAME = "evs_has_association_tutorial";

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

  /** Subset: the CTRP Terminology, which holds other subsets. */
  private static final String CTRP_TERMINOLOGY = "C116978";

  /** Subset: CTRP's disease terms, itself in {@link #CTRP_TERMINOLOGY}. */
  private static final String CTRP_DISEASES = "C116977";

  /** Subset: Cellosaurus's disease terms. In no other subset. */
  private static final String CELLOSAURUS_DISEASES = "C165258";

  /** Made-up subset of anatomic sites. */
  private static final String SITES_OF_INTEREST = "C99002";

  /** Anatomic site: Skin, in {@link #SITES_OF_INTEREST}. */
  private static final String SKIN = "C12470";

  /** Anatomic site: Lung. In no subset. */
  private static final String LUNG = "C12468";

  /**
   * The concepts in the index. Cutaneous Melanoma and Lung Carcinoma are each in a disease subset,
   * and the CTRP disease subset is itself in the CTRP Terminology. Skin is in a subset of sites;
   * Lung isn't. The diseases also have primary site roles, so lessons can combine roles and
   * associations.
   */
  private static final List<Concept> INDEXED_CONCEPTS =
      List.of(
          getConcept(
              "C01",
              "Cutaneous Melanoma",
              List.of(getRole(SKIN, "Skin")),
              List.of(getSubsetMembership(CTRP_DISEASES, "CTRP Disease Terminology"))),
          getConcept(
              "C02",
              "Lung Carcinoma",
              List.of(getRole(LUNG, "Lung")),
              List.of(
                  getSubsetMembership(CELLOSAURUS_DISEASES, "Cellosaurus Disease Terminology"))),
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
          getConcept(LUNG, "Lung", List.of(), List.of()));

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
  // Lesson 1: HasAssociation finds concepts with an association of the given type.
  // ---------------------------------------------------------------------------------------------

  /** Without a target, every concept in any subset is found, whatever the subset. */
  @Test
  public void testAssociationTypeWithoutTargetFindsAnyTarget() {
    final EvsQuery conceptsInAnySubset = new HasAssociation(CONCEPT_IN_SUBSET);

    assertEquals(
        Set.of("Cutaneous Melanoma", "Lung Carcinoma", "CTRP Disease Terminology", "Skin"),
        fetchMatchingNames(conceptsInAnySubset));
  }

  /** The association's code finds the same concepts as its name. */
  @Test
  public void testAssociationCodeMatchesLikeName() {
    final EvsQuery conceptsInAnySubsetByName = new HasAssociation(CONCEPT_IN_SUBSET);
    final EvsQuery conceptsInAnySubsetByCode = new HasAssociation(CONCEPT_IN_SUBSET_CODE);

    assertEquals(
        fetchMatchingNames(conceptsInAnySubsetByName),
        fetchMatchingNames(conceptsInAnySubsetByCode));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: the target is a name or code, as for roles.
  // ---------------------------------------------------------------------------------------------

  /** A target is compared against the association's target code. */
  @Test
  public void testTargetMatchedByCode() {
    final EvsQuery conceptsInCtrpDiseases = new HasAssociation(CONCEPT_IN_SUBSET, CTRP_DISEASES);

    assertEquals(Set.of("Cutaneous Melanoma"), fetchMatchingNames(conceptsInCtrpDiseases));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: associations and roles are separate lists, but compose.
  // ---------------------------------------------------------------------------------------------

  /** HasAssociation doesn't search roles: no concept has an association named like a role. */
  @Test
  public void testRoleTypeFindsNoAssociations() {
    final EvsQuery conceptsWithPrimarySiteAssociation = new HasAssociation(PRIMARY_SITE);

    assertEquals(Set.of(), fetchMatchingNames(conceptsWithPrimarySiteAssociation));
  }

  /** HasRole doesn't search associations: no concept has a role named like an association. */
  @Test
  public void testAssociationTypeFindsNoRoles() {
    final EvsQuery conceptsWithSubsetRole = new HasRole(CONCEPT_IN_SUBSET);

    assertEquals(Set.of(), fetchMatchingNames(conceptsWithSubsetRole));
  }

  /** An association type is required. */
  @Test
  public void testRejectsMissingType() {
    assertThrows(NullPointerException.class, () -> new HasAssociation(null));
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
  private static Role getRole(final String siteCode, final String siteName) {
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
    final Association association = new Association();
    association.setType(CONCEPT_IN_SUBSET);
    association.setCode(CONCEPT_IN_SUBSET_CODE);
    association.setRelatedCode(subsetCode);
    association.setRelatedName(subsetName);
    return association;
  }
}
