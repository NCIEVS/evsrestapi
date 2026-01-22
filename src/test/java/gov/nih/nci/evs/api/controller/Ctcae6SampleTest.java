package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
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

/** CTCAE6 samples tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class Ctcae6SampleTest extends SampleTest {

  /** Setup class. */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(Ctcae6SampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc testMvc;

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeAll
  public static void setupClass() throws Exception {
    loadSamples("ctcae6", "src/test/resources/samples/ctcae6-samples.txt");
  }

  /**
   * Test CTCAE6 terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCTCAE6Terminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result =
        testMvc
            .perform(get(url).param("latest", "true").param("terminology", "ctcae6"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        ThreadLocalMapper.get()
            .readValue(
                content,
                new TypeReference<List<Terminology>>() {
                  // n/a
                });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("ctcae6")).count())
        .isEqualTo(1);
    final Terminology terminology =
        terminologies.stream().filter(t -> t.getTerminology().equals("ctcae6")).findFirst().get();
    assertThat(terminology.getTerminology()).isEqualTo("ctcae6");
    assertThat(terminology.getMetadata().getUiLabel())
        .isEqualTo("CTCAE 6: Common Terminology Criteria for Adverse Events Version 6");
    assertThat(terminology.getName()).isEqualTo("CTCAE 6 6.0");
    assertThat(terminology.getDescription()).isNotEmpty();

    assertThat(terminology.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(terminology.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(terminology.getMetadata().getLicenseText()).isEqualTo(null);
    assertThat(terminology.getDescription().trim())
        .isEqualTo("Common Terminology Criteria for Adverse Events");

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
    url = "/api/v1/concept/ctcae6/C221229";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C221229");
    assertThat(concept.getName())
        .isEqualTo("Grade 4 Immune effector cell-associated neurotoxicity syndrome");
    assertThat(concept.getTerminology()).isEqualTo("ctcae6");
    assertThat(concept.getActive()).isTrue();
  }
}
