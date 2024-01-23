package gov.nih.nci.evs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticOperationsService;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

/**
 * Test class for ensure our field settings are being followed, as well as setting dynamic mapping to false
 * is working
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DynamicMappingTest {

    /** The Elasticsearch operations service instance *. */
    @Autowired
    ElasticOperationsService operationsService;

    String indexName = "testindex";


    /**
     * Test dynamic mapping is not enabled for the objects as well as Ignored values are being
     * ignore that are set in the Concept model.
     * Using concept C3224, map the object to the Concept model and index the concept.
     * @throws Exception
     */
    @Test
    public void testConceptDynamicMapping() throws Exception {
        // SETUP
        Concept concept = new ObjectMapper().readValue(IOUtils.toString(getClass().getClassLoader().getResource(
                "conceptTestDM.json")), Concept.class);

        // ACT
        boolean result = operationsService.createIndex(indexName, true);
        if (result) {
            operationsService
                    .getElasticsearchOperations()
                    .indexOps(IndexCoordinates.of(indexName))
                    .putMapping(Concept.class);
        }
        operationsService.index(concept, indexName, Concept.class);

        // ASSERT
        assertNotNull(concept);
    }
}
