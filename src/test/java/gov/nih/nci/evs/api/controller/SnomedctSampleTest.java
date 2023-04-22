package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;

/**
 * NCIt samples test.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SnomedctSampleTest extends SampleTest {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(SnomedctSampleTest.class);

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
    loadSamples("snomedct", "src/test/resources/samples/snomedct-samples.txt");
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
    url = "/api/v1/concept/snomedct/36138009?include=maps";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("36138009");
    assertThat(concept.getTerminology()).isEqualTo("snomedct");
    assertThat(concept.getMaps().size()).isEqualTo(2);
    assertThat(concept.getMaps().get(0).getTargetCode()).isEqualTo("D84.8");
    assertThat(concept.getMaps().get(0).getGroup()).isEqualTo("1");
    assertThat(concept.getMaps().get(0).getRank()).isEqualTo("1");
    assertThat(concept.getMaps().get(0).getRule()).isEqualTo("TRUE");
    assertThat(concept.getMaps().get(1).getTargetCode()).isEqualTo("D84.9");
    assertThat(concept.getMaps().get(1).getGroup()).isEqualTo("1");
    assertThat(concept.getMaps().get(1).getRank()).isEqualTo("1");
    assertThat(concept.getMaps().get(1).getRule()).isEqualTo("TRUE");

    // TODO: test what the maps are
  }
}
