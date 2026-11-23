package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.support.es.OpensearchObject;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Unit test for {@link HierarchyUtils}. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class HierarchyUtilsTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(HierarchyUtilsTest.class);

  /**
   * Test model serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelSerialization() throws Exception {
    String s = "{\"hierarchyRoots\": [\"10000647\",\"10017886\"]}";
    ThreadLocalMapper.get().readValue(s, HierarchyUtils.class);

    s =
        "{\"name\": \"hierarchy\",\"hierarchy\": {\"hierarchyRoots\":"
            + " [\"10000647\",\"10030209\"]},\"paths\": null,\"concepts\": [],\"conceptMinimals\":"
            + " [],\"associationEntries\": null}";
    ThreadLocalMapper.get().readValue(s, OpensearchObject.class);
  }

  /**
   * Test max parent and child code statistics.
   *
   * @throws Exception the exception
   */
  @Test
  public void testParentAndChildCounts() throws Exception {
    final HierarchyUtils hierarchy =
        new HierarchyUtils(
            null,
            Arrays.asList(
                "A\tRoot A\tB\tChild B",
                "A\tRoot A\tC\tChild C",
                "D\tRoot D\tC\tChild C",
                "E\tRoot E\tC\tChild C"));

    assertEquals("A", hierarchy.getCodeWithMaxChildren(null));
    assertEquals(2, hierarchy.getChildCount("A"));
    assertEquals("C", hierarchy.getCodeWithMaxParents(null));
    assertEquals(3, hierarchy.getParentCount("C"));
  }

  /**
   * Test hierarchy path statistics.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPathStats() throws Exception {
    final Terminology terminology = new Terminology();
    terminology.setTerminology("unit");
    final TerminologyMetadata metadata = new TerminologyMetadata();
    metadata.setHierarchy(true);
    terminology.setMetadata(metadata);

    final HierarchyUtils hierarchy =
        new HierarchyUtils(
            terminology,
            Arrays.asList(
                "A\tRoot A\tB\tChild B",
                "A\tRoot A\tC\tChild C",
                "B\tChild B\tD\tChild D",
                "C\tChild C\tD\tChild D",
                "C\tChild C\tE\tChild E",
                "D\tChild D\tF\tChild F",
                "E\tChild E\tF\tChild F"));

    final HierarchyUtils.PathStats stats = hierarchy.getPathStats(terminology);
    assertEquals(5, stats.getCodeCount());
    assertEquals(8, stats.getTreePositionCount());
    assertEquals(1, stats.getMinPathCount());
    assertEquals("F", stats.getMaxPathsCode());
    assertEquals(3, stats.getMaxPathCount());
    assertEquals("F", hierarchy.getCodeWithMaxPaths(terminology));

    hierarchy.getPathsMap(terminology).remove("F");
    assertEquals("F", hierarchy.getCodeWithMaxPaths(terminology));
    assertEquals(3, hierarchy.getPathStats(terminology).getMaxPathCount());

    hierarchy.clearPathsMap(terminology);
    assertEquals("F", hierarchy.getCodeWithMaxPaths(terminology));
    assertTrue(hierarchy.getPathStats(terminology).getTreePositionCount() > 0);
  }

  /**
   * Test ancestors for a concept with a single path to root.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplyAncestorsSinglePath() throws Exception {
    final Concept concept = new Concept("X");
    concept.setPaths(paths(path("X", "P", "Root")));

    HierarchyUtils.applyAncestors(concept);

    // Only the depths come from the paths; the ancestors come from getAncestors
    assertTrue(concept.getAncestors().isEmpty());
    assertEquals(Arrays.asList("P|1", "Root|2"), concept.getAncestorDepths());
  }

  /**
   * Test ancestors under polyhierarchy, using real paths computed by {@link HierarchyUtils}. F has
   * paths F-D-B-A, F-D-C-A, and F-E-C-A, so each ancestor keeps its shortest depth.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplyAncestorsPolyhierarchy() throws Exception {
    final Terminology terminology = new Terminology();
    terminology.setTerminology("unit");
    final TerminologyMetadata metadata = new TerminologyMetadata();
    metadata.setHierarchy(true);
    terminology.setMetadata(metadata);
    final HierarchyUtils hierarchy =
        new HierarchyUtils(
            terminology,
            Arrays.asList(
                "A\tRoot A\tB\tChild B",
                "A\tRoot A\tC\tChild C",
                "B\tChild B\tD\tChild D",
                "C\tChild C\tD\tChild D",
                "C\tChild C\tE\tChild E",
                "D\tChild D\tF\tChild F",
                "E\tChild E\tF\tChild F"));

    final Concept concept = new Concept("F");
    concept.setPaths(hierarchy.getPaths(terminology, "F"));
    assertEquals(3, concept.getPaths().getPaths().size());

    HierarchyUtils.applyAncestors(concept);

    assertEquals(Arrays.asList("A|3", "B|2", "C|2", "D|1", "E|1"), concept.getAncestorDepths());
  }

  /**
   * Test that a root concept has no ancestors.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplyAncestorsRootConcept() throws Exception {
    final Concept concept = new Concept("Root");
    concept.setPaths(paths(path("Root")));

    HierarchyUtils.applyAncestors(concept);

    assertTrue(concept.getAncestors().isEmpty());
    assertTrue(concept.getAncestorDepths().isEmpty());
  }

  /**
   * Test that concepts without paths are left unchanged.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplyAncestorsNoPaths() throws Exception {
    final Concept noPaths = new Concept("X");
    HierarchyUtils.applyAncestors(noPaths);
    assertTrue(noPaths.getAncestors().isEmpty());
    assertTrue(noPaths.getAncestorDepths().isEmpty());

    final Concept emptyPaths = new Concept("X");
    emptyPaths.setPaths(new Paths());
    HierarchyUtils.applyAncestors(emptyPaths);
    assertTrue(emptyPaths.getAncestors().isEmpty());
    assertTrue(emptyPaths.getAncestorDepths().isEmpty());
  }

  /**
   * Test codes that contain colons, as in GO.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplyAncestorsColonCodes() throws Exception {
    final Concept concept = new Concept("GO:0000003");
    concept.setPaths(paths(path("GO:0000003", "GO:0000002", "GO:0008150")));

    HierarchyUtils.applyAncestors(concept);

    assertEquals(Arrays.asList("GO:0000002|1", "GO:0008150|2"), concept.getAncestorDepths());
    final String token = concept.getAncestorDepths().get(1);
    final int sep = token.lastIndexOf('|');
    assertEquals("GO:0008150", token.substring(0, sep));
    assertEquals("2", token.substring(sep + 1));
  }

  /**
   * Test that results are sorted and identical across repeated runs.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplyAncestorsDeterministic() throws Exception {
    final Concept concept = new Concept("X");
    concept.setPaths(paths(path("X", "Z", "M", "Root"), path("X", "B", "Root")));

    HierarchyUtils.applyAncestors(concept);
    final List<String> first = new ArrayList<>(concept.getAncestorDepths());
    HierarchyUtils.applyAncestors(concept);

    assertEquals(first, concept.getAncestorDepths());
    assertEquals(Arrays.asList("B|1", "M|2", "Root|2", "Z|1"), concept.getAncestorDepths());
  }

  /**
   * Test ancestors along a single path: each with its level, name and leaf flag.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAncestorsSinglePath() throws Exception {
    final List<Concept> ancestors = branchingHierarchy().getAncestors("G");

    assertEquals(Arrays.asList("E|1", "C|2", "A|3", "R|4"), codeLevels(ancestors));
    assertEquals(
        Arrays.asList("Name E", "Name C", "Name A", "Name R"),
        ancestors.stream().map(Concept::getName).toList());
    assertTrue(ancestors.stream().noneMatch(Concept::getLeaf));
  }

  /**
   * Test ancestors under polyhierarchy. F has parents B and D, so R is two levels up through B and
   * three through D and A: it is listed once, at its shortest distance. Results are sorted by level
   * and then name.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAncestorsPolyhierarchy() throws Exception {
    final List<Concept> ancestors = branchingHierarchy().getAncestors("F");

    assertEquals(Arrays.asList("B|1", "D|1", "A|2", "R|2"), codeLevels(ancestors));
  }

  /**
   * Test that a root concept and an unknown code have no ancestors.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAncestorsRootAndUnknownCode() throws Exception {
    final HierarchyUtils hierarchy = branchingHierarchy();

    assertTrue(hierarchy.getAncestors("R").isEmpty());
    assertTrue(hierarchy.getAncestors("UNKNOWN").isEmpty());
  }

  /**
   * Test that a concept listed as its own parent is skipped.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAncestorsSkipsSelfAsParent() throws Exception {
    final HierarchyUtils hierarchy =
        new HierarchyUtils(null, Arrays.asList("X\tName X\tX\tName X", "P\tName P\tX\tName X"));

    assertEquals(Arrays.asList("P|1"), codeLevels(hierarchy.getAncestors("X")));
  }

  /**
   * Builds the hierarchy R-A, R-B, A-C, A-D, B-F, C-E, D-F, E-G (parent-child).
   *
   * @return the hierarchy
   */
  private HierarchyUtils branchingHierarchy() {
    return new HierarchyUtils(
        null,
        Arrays.asList(
            "R\tName R\tA\tName A",
            "R\tName R\tB\tName B",
            "A\tName A\tC\tName C",
            "A\tName A\tD\tName D",
            "B\tName B\tF\tName F",
            "C\tName C\tE\tName E",
            "D\tName D\tF\tName F",
            "E\tName E\tG\tName G"));
  }

  /**
   * Returns each concept as "code|level", in order.
   *
   * @param concepts the concepts
   * @return the code and level tokens
   */
  private List<String> codeLevels(final List<Concept> concepts) {
    return concepts.stream().map(c -> c.getCode() + "|" + c.getLevel()).toList();
  }

  /**
   * Builds a path from the concept up to the root, with levels starting at 0.
   *
   * @param codes the codes, starting with the concept itself
   * @return the path
   */
  private Path path(final String... codes) {
    final Path path = new Path();
    path.setDirection(1);
    for (int i = 0; i < codes.length; i++) {
      final ConceptMinimal concept = new ConceptMinimal(codes[i]);
      concept.setLevel(i);
      path.getConcepts().add(concept);
    }
    return path;
  }

  /**
   * Builds a paths object.
   *
   * @param pathList the paths
   * @return the paths
   */
  private Paths paths(final Path... pathList) {
    final Paths paths = new Paths();
    paths.setPaths(new ArrayList<>(Arrays.asList(pathList)));
    return paths;
  }
}
