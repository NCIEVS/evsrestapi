package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** NCIt samples test. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class NcimSampleTest extends SampleTest {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(NcimSampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc testMvc;

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeAll
  public static void setupClass() throws Exception {
    loadSamples("ncim", "src/test/resources/samples/ncim-samples.txt");
  }

  /**
   * Test get concept with mappings.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptWithMappings() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = "/api/v1/concept/ncim/CL547438?include=maps";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CL547438");
    assertThat(concept.getTerminology()).isEqualTo("ncim");
    assertThat(concept.getMaps().size()).isEqualTo(2);
    assertThat(concept.getMaps().get(0).getTargetCode()).isEqualTo("S39.7");
    assertThat(concept.getMaps().get(0).getGroup()).isEqualTo("1");
    assertThat(concept.getMaps().get(0).getRank()).isEqualTo("1");
    assertThat(concept.getMaps().get(0).getRule()).isEqualTo("TRUE");
    assertThat(concept.getMaps().get(1).getTargetCode()).isEqualTo("S39.91X?");
    assertThat(concept.getMaps().get(1).getGroup()).isEqualTo("1");
    assertThat(concept.getMaps().get(1).getRank()).isEqualTo("1");
    assertThat(concept.getMaps().get(1).getRule()).isEqualTo("TRUE");
    // TODO: test what the maps are
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
    url = "/api/v1/concept/ncim/CL547438";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CL547438");
    assertThat(concept.getTerminology()).isEqualTo("ncim");
    assertThat(concept.getActive()).isTrue();

    // Test inactive
    url = "/api/v1/concept/ncim/C0278390?include=full";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0278390");
    assertThat(concept.getTerminology()).isEqualTo("ncim");
    assertThat(concept.getActive()).isFalse();
    assertThat(concept.getConceptStatus()).isEqualTo("Retired_Concept");

    // test that "Retired_Concept" was added to the list of concept statuses
    url = "/api/v1/metadata/terminologies?terminology=ncim&latest=true";
    log.info("Testing url - " + url);

    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    List<Terminology> list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Terminology>>() {
                  // n/a
                });
    assertThat(list).isNotEmpty();
    Terminology term = list.get(0);
    assertThat(term.getMetadata().getConceptStatuses()).isNotEmpty();
    assertThat(term.getMetadata().getConceptStatuses()).contains("Retired_Concept");
  }
}
