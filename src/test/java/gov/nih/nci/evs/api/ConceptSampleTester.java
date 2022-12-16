
package gov.nih.nci.evs.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.controller.NCIControllerTests;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Terminology;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptSampleTester {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(NCIControllerTests.class);

    private String baseUrl = "/api/v1/concept/";

    private String termUrl = "/api/v1/metadata/terminologies/";

    private MockMvc testMvc;

    private Terminology terminology;

    private List<String> errors = new ArrayList<String>();

    public ConceptSampleTester() {
        // n/a
    }

    public void setTerminology(String term, MockMvc mvc) throws Exception {
        String url = termUrl + "?latest=true&terminology" + term;
        if (term.equals("ncit")) {
            url += "&tag=monthly";
        }

        MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        String content = result.getResponse().getContentAsString();
        log.info("  content = " + content);
        List<Terminology> list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
            // n/a
        });
        terminology = list.get(0);
    }

    public void performMetadataTests(String term, Map<String, List<SampleRecord>> sampleMap, MockMvc mvc)
            throws Exception {
        String url = baseUrl;
        MvcResult result = null;
        testMvc = mvc;
        String content = null;
        Concept concept;
        setTerminology(term, testMvc);
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

    public void performContentTests(String term, Map<String, List<SampleRecord>> sampleMap, MockMvc mvc)
            throws Exception {
        String url = baseUrl;
        MvcResult result = null;
        testMvc = mvc;
        String content = null;
        Concept concept;
        setTerminology(term, testMvc);
        for (Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
            url = baseUrl + term + "/" + entry.getKey() + "?include=full";
            log.info("Testing url - " + url);
            result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
            content = result.getResponse().getContentAsString();
            log.info(" content = " + content);
            concept = new ObjectMapper().readValue(content, Concept.class);
            assertThat(content).isNotNull();
            log.info(content);
            boolean error = false;
            for (SampleRecord sample : entry.getValue()) {
                final String key = sample.getKey();
                boolean check = false;
                if (key.startsWith("refs:subClassOf") && !key.contains("~")) {
                    check = checkParent(concept, sample);
                } else if (key.equals(terminology.getMetadata().getCode())) {
                    check = checkCode(concept, sample);
                } else if (key.equals(terminology.getMetadata().getPreferredName())) {
                    check = checkPreferredName(concept, sample);
                } else if (terminology.getMetadata().getSynonym().contains(key)) {
                    check = checkSynonym(concept, sample);
                } else if (terminology.getMetadata().getDefinition().contains(key)) {
                    check = checkDefinition(concept, sample);
                } else if ((key.startsWith("rdfs:subClassOf") || key.startsWith("owl:equivalentClass"))
                        && key.contains("~")) {
                    check = checkRole(concept, sample);
                } else if (key.startsWith("qualifier")) {
                    continue;
                }
            }
            if (errors.size() > 0) {
                log.error("SAMPLING ERRORS FOUND. SEE LOG BELOW");
                for (String err : errors) {
                    log.error("ERROR: " + err);
                }
            }
        }
    }

    public boolean checkParent(Concept concept, SampleRecord sample) {
        return concept.getParents().stream().filter(o -> o.getCode().equals(sample.getValue())).findFirst().isPresent();
    }

    public boolean checkCode(Concept concept, SampleRecord sample) {
        return sample.getKey().equals(terminology.getMetadata().getCode());
    }

    public boolean checkPreferredName(Concept concept, SampleRecord sample) {
        return sample.getKey().equals(terminology.getMetadata().getPreferredName());
    }

    public boolean checkSynonym(Concept concept, SampleRecord sample) {
        return concept.getSynonyms().stream().filter(o -> o.getName().equals(sample.getValue())).findFirst()
                .isPresent();
    }

    public boolean checkDefinition(Concept concept, SampleRecord sample) {
        return concept.getDefinitions().stream().filter(o -> o.getDefinition().equals(sample.getValue())).findFirst()
                .isPresent();
    }

    public boolean checkRole(Concept concept, SampleRecord sample) throws Exception {
        String url = "/api/v1/metadata/" + terminology.getTerminology() + "/role/" + sample.getKey().split("~")[1]
                + "?include=minimal";
        MvcResult result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        String content = result.getResponse().getContentAsString();
        log.info(" content = " + content);
        Concept minMatchedRole = new ObjectMapper().readValue(content, Concept.class);
        Role conceptMatchedRole = concept.getRoles().stream().filter(o -> o.getRelatedCode().equals(sample.getValue()))
                .findFirst().orElse(null);
        return conceptMatchedRole != null
                && minMatchedRole.getName() == conceptMatchedRole.getType();
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
