package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.MainTypeHierarchy;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Unit test for {@link MainTypeHierarchy}. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
// @RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration(classes = TestConfiguration.class)
public class MainTypeHierarchyTest {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MainTypeHierarchyTest.class);

  /** The main type hierarchy. */
  @Autowired MainTypeHierarchy mainTypeHierarchy;

  /** The service. */
  @Autowired private SparqlQueryManagerService sparqlQueryService;

  /** The elasticquery service. */
  @Autowired ElasticQueryService esQueryService;

  /** The term utils. */
  @Autowired TerminologyUtils termUtils;

  /**
   * Test.
   *
   * @throws Exception the exception
   */
  @Test
  public void test() throws Exception {

    log.info("  Get terminology = ncit");
    // Use true to get the metadata
    final Terminology terminology = termUtils.getIndexedTerminology("ncit", esQueryService);
    log.info("    terminology = " + terminology);
    log.info("  Get hierarchy");
    final HierarchyUtils hierarchy = sparqlQueryService.getHierarchyUtilsCache(terminology);
    log.info("  Initialize main type hierarchy");
    mainTypeHierarchy.initialize(terminology, hierarchy);

    // Check certain concepts
    for (final String code : new String[] {"C156722", "C6278", "C128245"}) {
      log.info("  Get concept and compute paths = " + code);
      final Concept concept =
          sparqlQueryService.getConcept(code, terminology, new IncludeParam("full"));
      concept.setPaths(hierarchy.getPaths(terminology, concept.getCode()));

      final List<Paths> paths = mainTypeHierarchy.getMainMenuAncestors(concept);
      log.info("  Get main menu anc = " + paths);
      assertTrue(!paths.isEmpty());
    }
  }
}
