package gov.nih.nci.evs.api.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * A tutorial for {@link HasRole}, which finds concepts by role type and role target.
 *
 * <p>Every lesson follows the same steps:
 *
 * <ol>
 *   <li>A small index holds the concepts in {@link #INDEXED_CONCEPTS}, mapped exactly like a real
 *       concept index, so the nested roles mapping is tested too.
 *   <li>The lesson builds a query, e.g. {@code getHasRole(PRIMARY_SITE, SKIN)}.
 *   <li>The query runs against the index, and the lesson checks which concept names came back.
 * </ol>
 *
 * <p>A role type is the role's exact name or code, and a role target is the target's exact name or
 * code.
 *
 * <p>The role names and codes are made up for the tutorial. They look like NCIt's but don't need to
 * match it.
 *
 * <p>TODO: add full integration tests against a real NCIt concept index once it's reindexed with
 * nested roles (local {@code concept_ncit_2512e} still has flattened roles, so {@link HasRole}
 * fails on it). Check the counts from the 2026-10-20 benchmark: name contains "cutaneous melanoma"
 * and primary site at or below C12219 finds 64; below Neoplasm (C3262) with primary site at or
 * below C12219 finds 13,335.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HasRoleTest {

  /** A throwaway index used only by this tutorial. */
  private static final String INDEX_NAME = "evs_has_role_tutorial";

  /** Terminology of every tutorial concept. */
  private static final String TERMINOLOGY = "ncit";

  /** Version of the concepts in the tutorial index. */
  private static final String VERSION = "25.12e";

  /** One page large enough to hold every indexed concept. */
  private static final Pageable ALL_CONCEPTS = PageRequest.of(0, 100);

  /** Role linking a disease to where it primarily occurs. */
  private static final String PRIMARY_SITE = "Disease_Has_Primary_Anatomic_Site";

  /** Code of {@link #PRIMARY_SITE}. */
  private static final String PRIMARY_SITE_CODE = "R101";

  /** Role linking a disease to a site it is associated with. */
  private static final String ASSOCIATED_SITE = "Disease_Has_Associated_Anatomic_Site";

  /** Code of {@link #ASSOCIATED_SITE}. */
  private static final String ASSOCIATED_SITE_CODE = "R100";

  /** Target concept: Skin. */
  private static final String SKIN = "C12470";

  /** Target concept: Lung. */
  private static final String LUNG = "C12468";

  /** Target concept: Liver. No indexed concept points to it. */
  private static final String LIVER = "C12392";

  /**
   * The concepts in the index. "Lung Carcinoma" has both sites, but Skin only through the
   * associated-site role, which lesson 1 relies on. "Lung Adenocarcinoma" has only a Lung site.
   * Skin, Lung, and Liver are role targets.
   */
  private static final List<Concept> INDEXED_CONCEPTS =
      List.of(
          getConcept(
              "C01", "Cutaneous Melanoma", getRole(PRIMARY_SITE, PRIMARY_SITE_CODE, SKIN, "Skin")),
          getConcept(
              "C02",
              "Lung Carcinoma",
              getRole(PRIMARY_SITE, PRIMARY_SITE_CODE, LUNG, "Lung"),
              getRole(ASSOCIATED_SITE, ASSOCIATED_SITE_CODE, SKIN, "Skin")),
          getConcept(
              "C03",
              "Basal Cell Carcinoma of Skin",
              getRole(PRIMARY_SITE, PRIMARY_SITE_CODE, SKIN, "Skin")),
          getConcept("C04", "Melanoma"),
          getConcept(
              "C07", "Lung Adenocarcinoma", getRole(PRIMARY_SITE, PRIMARY_SITE_CODE, LUNG, "Lung")),
          getConcept(SKIN, "Skin"),
          getConcept(LUNG, "Lung"),
          getConcept(LIVER, "Liver"));

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
    createIndex(INDEX_NAME, VERSION, INDEXED_CONCEPTS);

    terminologies =
        List.of(QueryTestTerminologies.getTerminology(TERMINOLOGY, VERSION, INDEX_NAME));
  }

  /** Removes the index. */
  @AfterAll
  public void tearDownIndex() {
    operations.indexOps(IndexCoordinates.of(INDEX_NAME)).delete();
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 1: HasRole finds concepts with a role of the given type pointing to the given target.
  // ---------------------------------------------------------------------------------------------

  /**
   * Both concepts whose primary site is Skin are found. "Lung Carcinoma" has a primary site and
   * points to Skin, but on two different roles, so it doesn't match.
   */
  @Test
  public void testRoleTypeAndTargetMatchOnSameRoleEntry() {
    final EvsQuery diseasesWithPrimarySiteSkin = getHasRole(PRIMARY_SITE, SKIN);

    assertEquals(
        Set.of("Cutaneous Melanoma", "Basal Cell Carcinoma of Skin"),
        fetchMatchingNames(diseasesWithPrimarySiteSkin));
  }

  /** Nothing has a primary site of Liver. */
  @Test
  public void testUnmatchedTargetFindsNothing() {
    final EvsQuery diseasesWithPrimarySiteLiver = getHasRole(PRIMARY_SITE, LIVER);

    assertEquals(Set.of(), fetchMatchingNames(diseasesWithPrimarySiteLiver));
  }

  /** The associated-site role reaches Skin only from "Lung Carcinoma". */
  @Test
  public void testOtherRoleTypeFindsItsOwnTargets() {
    final EvsQuery diseasesWithAssociatedSiteSkin = getHasRole(ASSOCIATED_SITE, SKIN);

    assertEquals(Set.of("Lung Carcinoma"), fetchMatchingNames(diseasesWithAssociatedSiteSkin));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 2: the target is optional.
  // ---------------------------------------------------------------------------------------------

  /** Without a target, every concept with a primary site is found, whatever it points to. */
  @Test
  public void testRoleTypeWithoutTargetFindsAnyTarget() {
    final EvsQuery diseasesWithAnyPrimarySite = new HasRole(PRIMARY_SITE);

    assertEquals(
        Set.of(
            "Cutaneous Melanoma",
            "Lung Carcinoma",
            "Basal Cell Carcinoma of Skin",
            "Lung Adenocarcinoma"),
        fetchMatchingNames(diseasesWithAnyPrimarySite));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 3: a role type is an exact name or code.
  // ---------------------------------------------------------------------------------------------

  /** The role's code finds the same concepts as its name. */
  @Test
  public void testRoleCodeMatchesLikeName() {
    final EvsQuery byName = getHasRole(PRIMARY_SITE, SKIN);
    final EvsQuery byCode = getHasRole(PRIMARY_SITE_CODE, SKIN);

    assertEquals(fetchMatchingNames(byName), fetchMatchingNames(byCode));
  }

  /** Part of a role name is not a role name. */
  @Test
  public void testPartialRoleNameFindsNothing() {
    final EvsQuery conceptsWithRoleNamedAnatomicSite = new HasRole("Anatomic_Site");

    assertEquals(Set.of(), fetchMatchingNames(conceptsWithRoleNamedAnatomicSite));
  }

  /** Role types are case sensitive. */
  @Test
  public void testRoleTypeIsCaseSensitive() {
    final EvsQuery diseasesWithLowercasePrimarySiteSkin =
        getHasRole(PRIMARY_SITE.toLowerCase(), SKIN);

    assertEquals(Set.of(), fetchMatchingNames(diseasesWithLowercasePrimarySiteSkin));
  }

  /** A target is compared against the role's target code. */
  @Test
  public void testTargetMatchedByCode() {
    final EvsQuery diseasesWithPrimarySiteCodeSkin = new HasRole(PRIMARY_SITE, SKIN);

    assertEquals(
        Set.of("Cutaneous Melanoma", "Basal Cell Carcinoma of Skin"),
        fetchMatchingNames(diseasesWithPrimarySiteCodeSkin));
  }

  /** A target that matches neither a role's target name nor its code finds nothing. */
  @Test
  public void testUnmatchedTargetNameOrCodeFindsNothing() {
    final EvsQuery diseasesWithPrimarySiteNamedExactlyLiver = new HasRole(PRIMARY_SITE, "Liver");

    assertEquals(Set.of(), fetchMatchingNames(diseasesWithPrimarySiteNamedExactlyLiver));
  }

  // ---------------------------------------------------------------------------------------------
  // Lesson 4: HasRole combines with other queries.
  // ---------------------------------------------------------------------------------------------

  /**
   * Primary site Lung, and associated site Skin. Unlike lesson 1, the two conditions are on
   * different roles of the same concept, combined with And.
   */
  @Test
  public void testCombinesWithAnotherHasRole() {
    final EvsQuery hasLungPrimarySite = getHasRole(PRIMARY_SITE, LUNG);
    final EvsQuery hasSkinAssociatedSite = getHasRole(ASSOCIATED_SITE, SKIN);
    final EvsQuery lungDiseasesWithSkinAssociatedSite =
        new And(List.of(hasLungPrimarySite, hasSkinAssociatedSite));

    assertEquals(Set.of("Lung Carcinoma"), fetchMatchingNames(lungDiseasesWithSkinAssociatedSite));
  }

  /** A role type and a target are required. */
  @Test
  public void testRejectsMissingValues() {
    final String missingValue = null;
    assertThrows(NullPointerException.class, () -> new HasRole(missingValue));
    assertThrows(NullPointerException.class, () -> new HasRole(PRIMARY_SITE, missingValue));
  }

  /**
   * Builds a {@link HasRole} whose target is one concept, given by its exact code.
   *
   * @param roleNameOrCode the role's exact name or code
   * @param targetCode the code of the concept the role must point to
   * @return the query
   */
  private HasRole getHasRole(final String roleNameOrCode, final String targetCode) {
    return new HasRole(roleNameOrCode, targetCode);
  }

  /**
   * Runs the query against the indexes it searches.
   *
   * @param query the query to run
   * @return the names of the matching concepts
   */
  private Set<String> fetchMatchingNames(final EvsQuery query) {
    return fetchMatchingNames(query, terminologies);
  }

  /**
   * Runs the query against the indexes of the given terminologies.
   *
   * @param query the query to run
   * @param searched the terminologies being searched
   * @return the names of the matching concepts
   */
  private Set<String> fetchMatchingNames(final EvsQuery query, final List<Terminology> searched) {
    return fetchMatchingNames(
        query.fetchOpenSearchQuery(operations, searched), EvsQuery.getIndexCoordinates(searched));
  }

  /**
   * Runs the OpenSearch query against the given indexes.
   *
   * @param query the query to run
   * @param indexes the indexes to search
   * @return the names of the matching concepts
   */
  private Set<String> fetchMatchingNames(final QueryBuilder query, final IndexCoordinates indexes) {
    final NativeSearchQuery search =
        new NativeSearchQueryBuilder().withQuery(query).withPageable(ALL_CONCEPTS).build();
    return operations.search(search, Concept.class, indexes).stream()
        .map(SearchHit::getContent)
        .map(Concept::getName)
        .collect(Collectors.toSet());
  }

  /**
   * Creates an index mapped like a concept index and loads the concepts into it, as one version of
   * the tutorial terminology.
   *
   * @param indexName the index name
   * @param version the version the concepts belong to
   * @param concepts the concepts to load
   */
  private void createIndex(
      final String indexName, final String version, final List<Concept> concepts) {
    for (final Concept concept : concepts) {
      concept.setTerminology(TERMINOLOGY);
      concept.setVersion(version);
    }
    final IndexCoordinates index = IndexCoordinates.of(indexName);
    final IndexOperations indexOperations = operations.indexOps(index);
    indexOperations.delete();
    indexOperations.create();
    indexOperations.putMapping(Concept.class);
    operations.save(concepts, index);
    indexOperations.refresh();
  }

  /**
   * Builds a concept with a normalized name and the given roles.
   *
   * @param code the concept code
   * @param name the preferred name
   * @param roles the concept's roles
   * @return the concept
   */
  private static Concept getConcept(final String code, final String name, final Role... roles) {
    final Concept concept = new Concept();
    concept.setCode(code);
    concept.setName(name);
    concept.setNormName(ConceptUtils.normalize(name));
    concept.setRoles(List.of(roles));
    return concept;
  }

  /**
   * Builds a role.
   *
   * @param type the role's name
   * @param code the role's code
   * @param relatedCode the code of the concept the role points to
   * @param relatedName the name of the concept the role points to
   * @return the role
   */
  private static Role getRole(
      final String type, final String code, final String relatedCode, final String relatedName) {
    final Role role = new Role();
    role.setType(type);
    role.setCode(code);
    role.setRelatedCode(relatedCode);
    role.setRelatedName(relatedName);
    return role;
  }
}
