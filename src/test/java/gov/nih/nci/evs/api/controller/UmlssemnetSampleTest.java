
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Umlssemnet samples tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class UmlssemnetSampleTest extends SampleTest {

  /**
   * Setup class.
   *
   */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(UmlssemnetSampleTest.class);

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
    loadSamples("umlssemnet", "src/test/resources/samples/umlssemnet-samples.txt");
  }

  /**
   * Test HGNC terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHGNCTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url).param("latest", "true").param("terminology", "umlssemnet")).andExpect(status().isOk())
        .andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("umlssemnet")).count()).isEqualTo(1);
    final Terminology terminology = terminologies.stream().filter(t -> t.getTerminology().equals("umlssemnet")).findFirst().get();
    assertThat(terminology.getTerminology()).isEqualTo("umlssemnet");
    assertThat(terminology.getMetadata().getUiLabel()).isEqualTo("UMLS Semantic Network");
    assertThat(terminology.getName()).isEqualTo("UMLS Semantic Network 2023AA");
    assertThat(terminology.getDescription()).isNotEmpty();

    assertThat(terminology.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(terminology.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(terminology.getMetadata().getLicenseText()).isEqualTo("Government information at NLM Web sites is in the public domain. " 
        + "Public domain information may be freely distributed and copied, but it is requested that in any subsequent use the " 
        + "National Library of Medicine (NLM) be given appropriate acknowledgement as specified at " 
        + "https://lhncbc.nlm.nih.gov/semanticnetwork/terms.html");
    assertThat(terminology.getDescription()).isEqualTo("UMLS Semantic Network");

    assertThat(terminology.getLatest()).isTrue();
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
    url = "/api/v1/concept/umlssemnet/T001";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("T001");
    assertThat(concept.getTerminology()).isEqualTo("umlssemnet");
    assertThat(concept.getActive()).isTrue();
  }
}