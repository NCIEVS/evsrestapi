package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
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

/** ChEBI samples tests. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ChebiSampleTest extends SampleTest {

  /** Setup class. */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ChebiSampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc testMvc;

  @BeforeClass
  public static void setupClass() throws Exception {
    Charset encode = StandardCharsets.US_ASCII;
    loadSamples("chebi", "src/test/resources/samples/chebi-samples.txt", encode);
  }

  /**
   * Test CHEBI terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCHEBITerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result =
        testMvc
            .perform(get(url).param("latest", "true").param("terminology", "chebi"))
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
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("chebi")).count())
        .isEqualTo(1);
    final Terminology chebi =
        terminologies.stream().filter(t -> t.getTerminology().equals("chebi")).findFirst().get();
    assertThat(chebi.getTerminology()).isEqualTo("chebi");
    assertThat(chebi.getMetadata().getUiLabel())
        .isEqualTo("ChEBI: Chemical Entities of Biological Interest");
    assertThat(chebi.getName()).isEqualTo("ChEBI: Chemical Entities of Biological Interest 213");
    assertThat(chebi.getDescription()).isNotEmpty();

    assertThat(chebi.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(chebi.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(chebi.getMetadata().getLicenseText()).isNull();
    assertThat(chebi.getDescription())
        .isEqualTo(
            "Chemical Entities of Biological Interest (ChEBI) is a freely available dictionary"
                + " of molecular entities focused on 'small' chemical compounds.");

    assertThat(chebi.getLatest()).isTrue();
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
    url = "/api/v1/concept/chebi/CHEBI:104926";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CHEBI:104926");
    assertThat(concept.getTerminology()).isEqualTo("chebi");
    assertThat(concept.getActive()).isTrue();
  }
}
