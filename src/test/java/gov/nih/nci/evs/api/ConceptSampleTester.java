
package gov.nih.nci.evs.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.Terminology;

public class ConceptSampleTester {

    /** The mvc. */
    @Autowired
    private MockMvc mvc;

    /** The logger. */

    private String baseUrl = "/api/v1/concept/search";

    public ConceptSampleTester() {
        // n/a
    }

    public void performMetadataTests(Terminology terminology, Map<String, List<SampleRecord>> sampleMap)
            throws Exception {
        String url = baseUrl;
        MvcResult result = null;
        String content = null;
        for (Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
            result = this.mvc
                    .perform(get(url).param("terminology", "ncit")
                            .param("code", entry.getKey())
                            .param("include", "full")
                            .param("type", "exact"))
                    .andExpect(status().isOk()).andReturn();
            content = result.getResponse().getContentAsString();
            assertThat(content).isNotNull();
            Concept conc = new ObjectMapper().readValue(content, ConceptResultList.class).getConcepts().get(0);

            for (SampleRecord sample : entry.getValue()) {
                ;
            }
        }
    }

    public void performContentTests(Terminology terminology, Map<String, List<SampleRecord>> sampleMap)
            throws Exception {
        String url = baseUrl;
        MvcResult result = null;
        String content = null;
        for (Entry<String, List<SampleRecord>> entry : sampleMap.entrySet()) {
            result = this.mvc
                    .perform(get(url).param("terminology", "ncit")
                            .param("code", entry.getKey())
                            .param("include", "full")
                            .param("type", "exact"))
                    .andExpect(status().isOk()).andReturn();
            content = result.getResponse().getContentAsString();
            assertThat(content).isNotNull();
            Concept conc = new ObjectMapper().readValue(content, ConceptResultList.class).getConcepts().get(0);

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
