
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
 * GO samples tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DuoSampleTest extends SampleTest {

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(DuoSampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired
  private MockMvc testMvc;

  @BeforeClass
  public static void setupClass() throws Exception {
    loadSamples("duo", "src/test/resources/samples/duo-samples.txt");
  }

  @Test
  public void testGOTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url).param("latest", "true").param("terminology", "duo"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
          // n/a
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("duo")).count())
        .isEqualTo(1);
    final Terminology duo =
        terminologies.stream().filter(t -> t.getTerminology().equals("duo")).findFirst().get();
    assertThat(duo.getTerminology()).isEqualTo("duo");
    assertThat(duo.getMetadata().getUiLabel()).isEqualTo("DUO: Data Use Ontology");
    assertThat(duo.getName()).isEqualTo(
        "DUO: Data Use Ontology http://purl.obolibrary.org/obo/duo/releases/2021-02-23/duo.owl");
    assertThat(duo.getDescription()).isNotEmpty();

    assertThat(duo.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(duo.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(duo.getMetadata().getLicenseText()).isNull();
    assertThat(duo.getDescription())
        .isEqualTo("DUO (Data Use Ontology) is an ontology which represent data use conditions.");

    assertThat(duo.getLatest()).isTrue();
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
    url = "/api/v1/concept/duo/DUO:0000001";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("DUO:0000001");
    assertThat(concept.getTerminology()).isEqualTo("duo");
    assertThat(concept.getActive()).isTrue();
  }

}