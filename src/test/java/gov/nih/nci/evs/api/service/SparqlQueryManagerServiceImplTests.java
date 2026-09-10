package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.LogicalDefinition;
import gov.nih.nci.evs.api.model.LogicalDefinition.Element;
import gov.nih.nci.evs.api.model.LogicalDefinition.Restriction;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Integration tests for SparqlQueryManagerImpl. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SparqlQueryManagerServiceImplTests {

  /** The logger. */
  // private static final Logger logger =
  // LoggerFactory.getLogger(SparqlQueryManagerImplTests.class);

  /** The mvc. */
  // @Autowired
  // private MockMvc mvc;

  @Autowired SparqlQueryManagerService sparqlQueryService;

  /** The opensearchquery service. */
  @Autowired OpensearchQueryService osQueryService;

  /** The term utils. */
  @Autowired TerminologyUtils termUtils;

  /** Sets the up. */
  @BeforeEach
  public void setUp() {
    // n/a
  }

  /**
   * NCIM terminology basic tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetDefinitionSources() throws Exception {
    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    final List<ConceptMinimal> list = sparqlQueryService.getDefinitionSources(term);

    assertTrue(list.stream().filter(c -> c.getCode().equals("BRIDG")).count() > 0);
    assertFalse(list.stream().filter(c -> c.getCode().equals("MSH2001")).count() > 0);
  }

  /**
   * Test get synonym sources.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetSynonymSources() throws Exception {

    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    final List<ConceptMinimal> list = sparqlQueryService.getSynonymSources(term);
    assertTrue(list.stream().filter(c -> c.getCode().equals("BRIDG")).count() > 0);
  }

  /** Test a simple equivalent-class definition from the current NCIt unit-test data. */
  @Test
  public void testSimpleLogicalDefinition() throws Exception {
    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    final LogicalDefinition definition = sparqlQueryService.getLogicalDefinition("C3224", term);

    assertNotNull(definition);
    assertEquals(2, definition.getParents().size());
    final Element abnormalCell =
        definition.getElements().stream()
            .filter(element -> "C12913".equals(element.getRangeCode()))
            .findFirst()
            .orElseThrow();
    assertTrue(
        abnormalCell.getRoles().stream()
            .anyMatch(
                role ->
                    "R105".equals(role.getRoleCode()) && "C36873".equals(role.getTargetCode())));
  }

  /** Test batched extraction used by the NCIt indexing pipeline. */
  @Test
  public void testLogicalDefinitionBatch() throws Exception {
    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    final Map<String, LogicalDefinition> definitions =
        sparqlQueryService.getLogicalDefinitions(List.of("C3224", "C37193", "C45231"), term);

    assertEquals(3, definitions.size());
    assertEquals(2, definitions.get("C3224").getParents().size());
    assertEquals(
        9,
        definitions.get("C37193").getElements().stream()
            .flatMap(element -> element.getRoleGroups().stream())
            .findFirst()
            .orElseThrow()
            .getRoleSets()
            .size());
    assertTrue(
        definitions.get("C45231").getElements().stream()
            .anyMatch(element -> "[Range Unspecified]".equals(element.getRange())));
  }

  /** Test a valid concept without an equivalent-class definition. */
  @Test
  public void testNoLogicalDefinition() throws Exception {
    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    assertTrue(sparqlQueryService.getLogicalDefinition("C1000", term) == null);
  }

  /** Test an equivalent-class definition containing parents but no restrictions. */
  @Test
  public void testParentsOnlyLogicalDefinition() throws Exception {
    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    final LogicalDefinition definition = sparqlQueryService.getLogicalDefinition("C102870", term);

    assertNotNull(definition);
    assertEquals(2, definition.getParents().size());
    assertTrue(definition.getElements().isEmpty());
  }

  /** Test an OR-of-AND role group from the current NCIt unit-test data. */
  @Test
  public void testComplexLogicalDefinition() throws Exception {
    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    final LogicalDefinition definition = sparqlQueryService.getLogicalDefinition("C37193", term);

    assertNotNull(definition);
    final Element molecularAbnormality =
        definition.getElements().stream()
            .filter(element -> "C3910".equals(element.getRangeCode()))
            .findFirst()
            .orElseThrow();
    assertEquals(1, molecularAbnormality.getRoleGroups().size());
    assertEquals(9, molecularAbnormality.getRoleGroups().get(0).getRoleSets().size());
    assertTrue(
        molecularAbnormality.getRoleGroups().get(0).getRoleSets().stream()
            .allMatch(roleSet -> roleSet.getRoles().size() == 2));
  }

  /** Test that incompatible group ranges remain represented in the fallback range bucket. */
  @Test
  public void testIncompatibleRangeLogicalDefinition() throws Exception {
    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    final LogicalDefinition definition = sparqlQueryService.getLogicalDefinition("C45231", term);

    assertNotNull(definition);
    final Element unspecified =
        definition.getElements().stream()
            .filter(element -> "[Range Unspecified]".equals(element.getRange()))
            .findFirst()
            .orElseThrow();
    assertEquals(1, unspecified.getRoleGroups().size());
    assertEquals(2, unspecified.getRoleGroups().get(0).getRoleSets().size());
    assertTrue(
        unspecified.getRoleGroups().get(0).getRoleSets().stream()
            .allMatch(roleSet -> roleSet.getRoles().size() == 2));
    final List<Restriction> groupedRoles =
        unspecified.getRoleGroups().get(0).getRoleSets().stream()
            .flatMap(roleSet -> roleSet.getRoles().stream())
            .toList();
    assertEquals(
        2, groupedRoles.stream().filter(role -> "R113".equals(role.getRoleCode())).count());
    assertTrue(
        groupedRoles.stream()
            .filter(role -> "R113".equals(role.getRoleCode()))
            .allMatch(
                role ->
                    "Abnormal Cell".equals(role.getRange())
                        && "C12913".equals(role.getRangeCode())
                        && role.getRangeUri().endsWith("#C12913")));
    assertEquals(
        2, groupedRoles.stream().filter(role -> "R116".equals(role.getRoleCode())).count());
    assertTrue(
        groupedRoles.stream()
            .filter(role -> "R116".equals(role.getRoleCode()))
            .allMatch(
                role ->
                    "Disease, Disorder or Finding".equals(role.getRange())
                        && "C7057".equals(role.getRangeCode())
                        && role.getRangeUri().endsWith("#C7057")));
    assertEquals(
        6, definition.getElements().stream().mapToInt(element -> element.getRoles().size()).sum());
  }

  /** Test that another mixed-range group preserves each restriction's tooltip range. */
  @Test
  public void testProgastrinRestrictionRanges() throws Exception {
    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    final LogicalDefinition definition = sparqlQueryService.getLogicalDefinition("C192657", term);

    assertNotNull(definition);
    final Element unspecified =
        definition.getElements().stream()
            .filter(element -> "[Range Unspecified]".equals(element.getRange()))
            .findFirst()
            .orElseThrow();
    final List<Restriction> groupedRoles =
        unspecified.getRoleGroups().get(0).getRoleSets().stream()
            .flatMap(roleSet -> roleSet.getRoles().stream())
            .toList();
    assertEquals(6, groupedRoles.size());
    assertTrue(groupedRoles.stream().allMatch(role -> role.getRange() != null));
    assertTrue(groupedRoles.stream().anyMatch(role -> "C20633".equals(role.getRangeCode())));
    assertTrue(groupedRoles.stream().anyMatch(role -> "C12219".equals(role.getRangeCode())));
    assertTrue(groupedRoles.stream().anyMatch(role -> "C17828".equals(role.getRangeCode())));
  }

  /** Test an OR expression whose alternatives are individual restrictions. */
  @Test
  public void testRestrictionUnionLogicalDefinition() throws Exception {
    final Terminology term = termUtils.getIndexedTerminology("ncit", osQueryService, true);
    final LogicalDefinition definition = sparqlQueryService.getLogicalDefinition("C5183", term);

    assertNotNull(definition);
    final Element gene =
        definition.getElements().stream()
            .filter(element -> "C16612".equals(element.getRangeCode()))
            .findFirst()
            .orElseThrow();
    assertEquals(1, gene.getRoleUnions().size());
    assertEquals(2, gene.getRoleUnions().get(0).getRoles().size());
    assertTrue(
        gene.getRoleUnions().get(0).getRoles().stream()
            .allMatch(
                role ->
                    "Gene".equals(role.getRange())
                        && "C16612".equals(role.getRangeCode())
                        && role.getRangeUri().endsWith("#C16612")));
  }
}
