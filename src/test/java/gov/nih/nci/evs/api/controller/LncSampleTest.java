package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;

/**
 * NCIt samples test.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class LncSampleTest extends SampleTest {

    /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(LncSampleTest.class);

    /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
    @Autowired
    private MockMvc testMvc;
    
  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    Charset encode = StandardCharsets.US_ASCII;
    loadSamples("lnc", "src/test/resources/samples/lnc-samples.txt", encode);
  }
  
  /**
   * Test concept active status.
   *
   * @throws Exception the exception
   */
  @Test
  public void testActive() throws Exception {
      
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test active
    url = "/api/v1/concept/lnc/LA26702-3";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("LA26702-3");
    assertThat(concept.getTerminology()).isEqualTo("lnc");
    assertThat(concept.getActive()).isTrue();
    
    // Test inactive
    url = "/api/v1/concept/lnc/36926-4?include=full";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("36926-4");
    assertThat(concept.getTerminology()).isEqualTo("lnc");
    assertThat(concept.getActive()).isFalse();
    assertThat(concept.getConceptStatus()).isEqualTo("Retired_Concept");
   
    // test that "Retired_Concept" was added to the list of concept statuses
    url = "/api/v1/metadata/terminologies?terminology=lnc&latest=true";
    log.info("Testing url - " + url);

    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    List<Terminology> list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    Terminology term = list.get(0);
    assertThat(term.getMetadata().getConceptStatuses()).isNotEmpty();
    assertThat(term.getMetadata().getConceptStatuses()).contains("Retired_Concept");
  }

}
