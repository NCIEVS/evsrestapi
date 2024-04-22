
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

/** CANMED samples tests. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class CanmedSampleTest extends SampleTest {

  /** Setup class. */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(CanmedSampleTest.class);

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
    loadSamples("canmed", "src/test/resources/samples/canmed-samples.txt");
  }

  /**
   * Test CANMED terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCANMEDTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url).param("latest", "true").param("terminology", "canmed"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
          // n/a
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("canmed")).count())
        .isEqualTo(1);
    final Terminology terminology =
        terminologies.stream().filter(t -> t.getTerminology().equals("canmed")).findFirst().get();
    assertThat(terminology.getTerminology()).isEqualTo("canmed");
    assertThat(terminology.getMetadata().getUiLabel()).isEqualTo("CanMED");
    assertThat(terminology.getName()).isEqualTo("CanMED 202311");
    assertThat(terminology.getDescription()).isNotEmpty();

    assertThat(terminology.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(terminology.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(terminology.getMetadata().getLicenseText()).isEqualTo(null);
    assertThat(terminology.getDescription())
        .isEqualTo("Cancer Medications Enquiry Database (CanMED)");

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
    url = "/api/v1/concept/canmed/NDC_16729-0131-30";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("NDC_16729-0131-30");
    assertThat(concept.getName()).isEqualTo("FLUDARABINE 25.0 mg/mL");
    assertThat(concept.getTerminology()).isEqualTo("canmed");
    assertThat(concept.getActive()).isTrue();
  }
}
