package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import java.util.List;
import java.util.stream.Collectors;
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
public class ObibSampleTest extends SampleTest {

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
    loadSamples("obib", "src/test/resources/samples/obib-samples.txt");
  }

  @Test
  public void testDUOTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result =
        testMvc
            .perform(get(url).param("latest", "true").param("terminology", "obib"))
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
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("obib")).count())
        .isEqualTo(1);
    final Terminology obib =
        terminologies.stream().filter(t -> t.getTerminology().equals("obib")).findFirst().get();
    assertThat(obib.getTerminology()).isEqualTo("obib");
    assertThat(obib.getMetadata().getUiLabel()).isEqualTo("OBIB: The ontology for Biobanking");
    assertThat(obib.getName()).isEqualTo("OBIB: The ontology for Biobanking 2021-11-12");
    assertThat(obib.getDescription()).isNotEmpty();

    assertThat(obib.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(obib.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(obib.getMetadata().getLicenseText()).isNull();
    assertThat(obib.getDescription())
        .isEqualTo(
            "OBIB: The ontology for Biobanking (OBIB) is an ontology built for annotation and"
                + " modeling of biobank repository and biobanking administration.");

    assertThat(obib.getLatest()).isTrue();
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
    url = "/api/v1/concept/obib/APOLLO_SV_00000032";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("APOLLO_SV_00000032");
    assertThat(concept.getTerminology()).isEqualTo("obib");
    assertThat(concept.getActive()).isTrue();
  }

  /**
   * Test children not duplicated.
   *
   * @throws Exception the exception
   */
  @Test
  public void testChildrenNotDuplicated() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/concept/obib/CL_0000000";
    log.info("Testing url - " + url);
    result =
        testMvc
            .perform(get(url).param("include", "children"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final Concept concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getChildren()).isNotEmpty();
    // Verify child codes are unique
    assertThat(concept.getChildren().size())
        .isEqualTo(
            concept.getChildren().stream()
                .map(c -> c.getCode())
                .collect(Collectors.toSet())
                .size());
  }

  /**
   * Test obsolete children.
   *
   * @throws Exception the exception
   */
  @Test
  public void testObsoleteChildren() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Get the "ObsoleteClass" code,
    url = "/api/v1/concept/obib/ObsoleteClass?include=children";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("ObsoleteClass");
    assertThat(concept.getChildren()).isNotEmpty();

    // Pick and load a child - it should exist
    final String obsoleteCode = concept.getChildren().get(0).getCode();
    url = "/api/v1/concept/obib/" + concept.getChildren().get(0).getCode();
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo(obsoleteCode);
  }
}
