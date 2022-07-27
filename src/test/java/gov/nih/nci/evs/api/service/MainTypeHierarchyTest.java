
package gov.nih.nci.evs.api.service;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.MainTypeHierarchy;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * Unit test for {@link MainTypeHierarchy}.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
// @RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration(classes = TestConfiguration.class)
public class MainTypeHierarchyTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(MainTypeHierarchyTest.class);

  /** The main type hierarchy. */
  @Autowired
  MainTypeHierarchy mainTypeHierarchy;

  /** The service. */
  @Autowired
  private SparqlQueryManagerService service;

  /** The term utils. */
  @Autowired
  TerminologyUtils termUtils;

  /**
   * Test.
   *
   * @throws Exception the exception
   */
  @Test
  public void test() throws Exception {

    log.info("  Get terminology = ncit");
    // Use true to get the metadata
    final Terminology terminology = termUtils.getTerminology("ncit", true);
    log.info("    terminology = " + terminology);
    log.info("  Get hierarchy");
    final HierarchyUtils hierarchy = service.getHierarchyUtils(terminology);
    log.info("  Initialize main type hierarchy");
    mainTypeHierarchy.initialize(terminology, hierarchy);

    // Check certain concepts
    for (final String code : new String[] {
        "C156722", "C6278", "C128245"
    }) {
      log.info("  Get concept and compute paths = " + code);
      final Concept concept = service.getConcept(code, terminology, new IncludeParam("full"));
      concept.setPaths(hierarchy.getPaths(concept.getCode(), terminology));

      final List<Paths> paths = mainTypeHierarchy.getMainMenuAncestors(concept);
      log.info("  Get main menu anc = " + paths);
      assertTrue(!paths.isEmpty());
    }
  }
}
