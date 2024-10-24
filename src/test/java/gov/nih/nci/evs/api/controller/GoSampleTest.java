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
public class GoSampleTest extends SampleTest {

  /** Setup class. */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(GoSampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc testMvc;

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeAll
  public static void setupClass() throws Exception {
    loadSamples("go", "src/test/resources/samples/go-samples.txt");
  }

  /**
   * Test GO terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGOTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result =
        testMvc
            .perform(get(url).param("latest", "true").param("terminology", "go"))
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
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("go")).count())
        .isEqualTo(1);
    final Terminology go =
        terminologies.stream().filter(t -> t.getTerminology().equals("go")).findFirst().get();
    assertThat(go.getTerminology()).isEqualTo("go");
    assertThat(go.getMetadata().getUiLabel()).isEqualTo("GO: Gene Ontology");
    assertThat(go.getName()).isEqualTo("GO: Gene Ontology 2022-07-01");
    assertThat(go.getDescription()).isNotEmpty();

    assertThat(go.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(go.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(go.getMetadata().getLicenseText()).isNull();
    assertThat(go.getDescription())
        .isEqualTo(
            "The Gene Ontology (GO) provides a framework and set"
                + " of concepts for describing the functions of gene products from all organisms.");

    assertThat(go.getLatest()).isTrue();
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
    url = "/api/v1/concept/go/GO:0002451";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("GO:0002451");
    assertThat(concept.getTerminology()).isEqualTo("go");
    assertThat(concept.getActive()).isTrue();
  }

  /**
   * Test part of parent.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPartOfParent() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // This concept has a particular "complex" role - verify that it's present
    // If not there may be a problem in sparql-queries.properties for roles.all.complex
    url = "/api/v1/concept/go/GO:0000015?include=parents";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("GO:0000015");
    assertThat(concept.getParents().stream().filter(p -> p.getCode().equals("GO:0005829")).count())
        .isGreaterThan(0);
  }

  /**
   * Test obsolete concept. This is to confirm that owl:deprecated=true are still not being loded.
   *
   * @throws Exception the exception
   */
  @Test
  public void testObsoleteConcept() throws Exception {
    String url = null;

    // This concept has a particular "complex" role - verify that it's present
    // If not there may be a problem in sparql-queries.properties for roles.all.complex
    url = "/api/v1/concept/go/GO_0000004";
    log.info("Testing url - " + url);
    testMvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
  }
}
