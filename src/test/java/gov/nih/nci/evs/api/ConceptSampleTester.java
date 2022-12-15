
package gov.nih.nci.evs.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.controller.NCIControllerTests;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptSampleTester {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(NCIControllerTests.class);

    private String baseUrl = "/api/v1/concept/";

    public ConceptSampleTester() {
        // n/a
    }

    public void performMetadataTests(Terminology terminology, Map<String, List<SampleRecord>> sampleMap, MockMvc mvc)
            throws Exception {
        String term = terminology.getTerminology();
        String url = baseUrl;
        MvcResult result = null;
        String content = null;
        Concept concept;
        for (Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
            url = baseUrl + term + "/" + entry.getKey() + "?include=full";
            log.info("Testing url - " + url);
            result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
            content = result.getResponse().getContentAsString();
            log.info(" content = " + content);
            concept = new ObjectMapper().readValue(content, Concept.class);
            assertThat(content).isNotNull();
            log.info(content);
            for (SampleRecord sample : entry.getValue()) {
                ;
            }
        }
    }

    public void performContentTests(Terminology terminology, Map<String, List<SampleRecord>> sampleMap, MockMvc mvc)
            throws Exception {
        String term = terminology.getTerminology();
        String url = baseUrl;
        MvcResult result = null;
        String content = null;
        Concept concept;
        for (Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
            url = baseUrl + term + "/" + entry.getKey() + "?include=full";
            log.info("Testing url - " + url);
            result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
            content = result.getResponse().getContentAsString();
            log.info(" content = " + content);
            concept = new ObjectMapper().readValue(content, Concept.class);
            assertThat(content).isNotNull();
            log.info(content);
            for (SampleRecord sample : entry.getValue()) {
                ;
            }
        }
    }

    public void performPathsSubtreeAndRootsTests(Terminology terminology, Map<String, List<SampleRecord>> sampleMap) {

    }

    public void performSearchTests(Terminology terminology, Map<String, List<SampleRecord>> sampleMap) {

    }

    public void performSubsetsTests(Terminology terminology, Map<String, List<SampleRecord>> sampleMap) {

    }

    public void performAssociationTests(Terminology terminology, Map<String, List<SampleRecord>> sampleMap) {

    }

}
