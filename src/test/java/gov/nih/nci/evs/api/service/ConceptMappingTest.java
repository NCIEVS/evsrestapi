package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.configuration.EVSElasticsearchRestTemplate;
import gov.nih.nci.evs.api.model.Concept;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Unit test for accessing the concept mapping. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ConceptMappingTest {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(ConceptMappingTest.class);

  /** The Elasticsearch operations service instance *. */
  @Autowired ElasticOperationsService operationsService;

  /** The template. */
  @Autowired EVSElasticsearchRestTemplate template;

  /**
   * Test concept mapping.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapping() throws Exception {
    final Document doc =
        operationsService.getElasticsearchOperations().indexOps(Concept.class).createMapping();
    logger.info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(doc));
  }
}
