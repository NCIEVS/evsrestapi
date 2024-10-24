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

/** GO samples tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class NdfrtSampleTest extends SampleTest {

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ObiSampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc testMvc;

  @BeforeAll
  public static void setupClass() throws Exception {
    loadSamples("ndfrt", "src/test/resources/samples/ndfrt-samples.txt");
  }

  @Test
  public void testNDFRTTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result =
        testMvc
            .perform(get(url).param("latest", "true").param("terminology", "ndfrt"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Terminology>>() {
                  // n/a
                });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("ndfrt")).count())
        .isEqualTo(1);
    final Terminology ndfrt =
        terminologies.stream().filter(t -> t.getTerminology().equals("ndfrt")).findFirst().get();
    assertThat(ndfrt.getTerminology()).isEqualTo("ndfrt");
    assertThat(ndfrt.getMetadata().getUiLabel())
        .isEqualTo("NDF-RT: National Drug File Reference Terminology");
    assertThat(ndfrt.getName())
        .isEqualTo("NDF-RT: National Drug File Reference Terminology 2018.02.05");
    assertThat(ndfrt.getDescription()).isNotEmpty();

    assertThat(ndfrt.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(ndfrt.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(ndfrt.getMetadata().getLicenseText()).isNull();
    assertThat(ndfrt.getDescription())
        .isEqualTo(
            "National Drug File Reference Terminology (NDF-RT) is produced by "
                + "the Veterans Health Administration (VHA) as an extension of the "
                + "VHA National Drug File (VANDF) formulary.");

    assertThat(ndfrt.getLatest()).isTrue();
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
    url = "/api/v1/concept/ndfrt/N0000000004";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("N0000000004");
    assertThat(concept.getTerminology()).isEqualTo("ndfrt");
    assertThat(concept.getActive()).isTrue();
  }
}
