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

/** HGNC samples tests. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class HgncSampleTest extends SampleTest {

  /** Setup class. */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(HgncSampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc testMvc;

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    Charset encode = StandardCharsets.US_ASCII;
    loadSamples("hgnc", "src/test/resources/samples/hgnc-samples.txt");
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
    result =
        testMvc
            .perform(get(url).param("latest", "true").param("terminology", "hgnc"))
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
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("hgnc")).count())
        .isEqualTo(1);
    final Terminology hgnc =
        terminologies.stream().filter(t -> t.getTerminology().equals("hgnc")).findFirst().get();
    assertThat(hgnc.getTerminology()).isEqualTo("hgnc");
    assertThat(hgnc.getMetadata().getUiLabel()).isEqualTo("HGNC: HUGO Gene Nomenclature Committee");
    assertThat(hgnc.getName()).isEqualTo("HGNC: HUGO Gene Nomenclature Committee 202209");
    assertThat(hgnc.getDescription()).isNotEmpty();

    assertThat(hgnc.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(hgnc.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(hgnc.getMetadata().getLicenseText()).isNull();
    assertThat(hgnc.getDescription())
        .isEqualTo(
            "HGNC, published by the HUGO Gene Nomenclature Committee"
                + " at the European Bioinformatics Institute.");

    assertThat(hgnc.getLatest()).isTrue();
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
    url = "/api/v1/concept/hgnc/HGNC:11231";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("HGNC:11231");
    assertThat(concept.getTerminology()).isEqualTo("hgnc");
    assertThat(concept.getActive()).isTrue();
  }
}
