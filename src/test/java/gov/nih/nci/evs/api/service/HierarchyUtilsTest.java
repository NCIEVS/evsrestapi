package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.support.es.OpensearchObject;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.Arrays;
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
}
