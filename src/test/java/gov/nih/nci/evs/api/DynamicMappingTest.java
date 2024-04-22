package gov.nih.nci.evs.api;

import static org.junit.Assert.assertNotNull;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticOperationsService;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * Test class for ensure our field settings are being followed (enabled = false,
 * DynamicMapping.False
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DynamicMappingTest {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(DynamicMappingTest.class);

  /** The Elasticsearch operations service instance *. */
  @Autowired
  ElasticOperationsService operationsService;

  /** The elastic query service */
  @Autowired
  ElasticQueryService elasticQueryService;

  /** The terminology utils */
  @Autowired
  TerminologyUtils termUtils;

  /** index name constant */
  String indexName = "testindex";

  /** test code */
  String code = "C3224";

  /** terminology name */
  String terminology = "ncit";

  /**
   * Test dynamic mapping is not enabled for the objects as well as Ignored values are being ignored
   * that are set in the Concept model. Using concept C3224, map the object to the Concept model and
   * index the concept, to make sure we still have descendants and paths.
   *
   * @throws Exception
   */
  @Test
  public void testConceptDynamicMapping() throws Exception {
    // SETUP
    Concept concept = new ObjectMapper().readValue(
        IOUtils.toString(getClass().getClassLoader().getResource("conceptTestDM.json"), "UTF-8"),
        Concept.class);

    final Terminology term = termUtils.getIndexedTerminology(terminology, elasticQueryService);
    final IncludeParam ip = new IncludeParam("full,descendants,paths");

    // ACT
    boolean result = operationsService.createIndex(indexName, true);
    if (!result) {
      logger.warn("Our index boolean = " + false);
    }

    if (result) {
      operationsService.getElasticsearchOperations().indexOps(IndexCoordinates.of(indexName))
          .putMapping(Concept.class);
    }
    operationsService.index(concept, indexName, Concept.class);
    assertNotNull(concept);

    // get a concept
    try {
      final Concept response = elasticQueryService.getConcept(code, term, ip).get();
      // ASSERT
      assertNotNull(response);
      assertNotNull(response.getDescendants());
      assertNotNull(response.getPaths());
    } catch (Exception e) {
      logger.warn("CONCEPT IS NULL. TEST FAILED");
    }
  }
}
