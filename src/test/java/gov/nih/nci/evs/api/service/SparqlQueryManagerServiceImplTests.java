package gov.nih.nci.evs.api.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

/** Integration tests for SparqlQueryManagerImpl. */
@RunWith(SpringRunner.class)
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

  /** The elasticquery service. */
  @Autowired ElasticQueryService esQueryService;

  /** The term utils. */
  @Autowired TerminologyUtils termUtils;

  /** Sets the up. */
  @Before
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
    final Terminology term = termUtils.getIndexedTerminology("ncit", esQueryService);
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

    final Terminology term = termUtils.getIndexedTerminology("ncit", esQueryService);
    final List<ConceptMinimal> list = sparqlQueryService.getSynonymSources(term);
    assertTrue(list.stream().filter(c -> c.getCode().equals("BRIDG")).count() > 0);
  }
}
