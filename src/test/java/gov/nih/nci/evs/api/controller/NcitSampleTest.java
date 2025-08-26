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
public class NcitSampleTest extends SampleTest {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(NcitSampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc testMvc;

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeAll
  public static void setupClass() throws Exception {
    loadSamples("ncit", "src/test/resources/samples/ncit-samples.txt");
  }

  /**
   * Test NCI terminology monthly.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNCITerminologyMonthly() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // test if mdr term exists
    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result =
        testMvc
            .perform(
                get(url)
                    .param("latest", "true")
                    .param("tag", "monthly")
                    .param("terminology", "ncit"))
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
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("ncit")).count())
        .isEqualTo(1);
    final Terminology ncit =
        terminologies.stream().filter(t -> t.getTerminology().equals("ncit")).findFirst().get();
    assertThat(ncit.getTerminology()).isEqualTo("ncit");
    assertThat(ncit.getMetadata().getUiLabel()).isEqualTo("NCI Thesaurus");
    assertThat(ncit.getName()).isEqualTo("NCI Thesaurus 25.06e");
    assertThat(ncit.getDescription()).isNotEmpty();

    assertThat(ncit.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(ncit.getMetadata().getSourceCt()).isGreaterThan(60);
    assertThat(ncit.getMetadata().getLicenseText()).isNull();
    assertThat(ncit.getDescription())
        .isEqualTo(
            "NCI Thesaurus, a controlled vocabulary in support of NCI administrative and "
                + "scientific activities. Produced by the Enterprise Vocabulary System (EVS), "
                + "a project by the NCI Center for Biomedical Informatics and Information "
                + "Technology. National Cancer Institute, National Institutes of Health, "
                + "Bethesda, MD 20892, U.S.A.");

    assertThat(ncit.getLatest()).isTrue();
  }

  /**
   * Test NCI terminology weekly.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNCITerminologyWeekly() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // test if mdr term exists
    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result =
        testMvc
            .perform(
                get(url)
                    .param("latest", "true")
                    .param("tag", "weekly")
                    .param("terminology", "ncit"))
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
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("ncit")).count())
        .isEqualTo(1);
    final Terminology ncit =
        terminologies.stream().filter(t -> t.getTerminology().equals("ncit")).findFirst().get();
    assertThat(ncit.getTerminology()).isEqualTo("ncit");
    assertThat(ncit.getMetadata().getUiLabel()).isEqualTo("NCI Thesaurus");
    assertThat(ncit.getName()).isEqualTo("NCI Thesaurus 25.07b");
    assertThat(ncit.getDescription()).isNotEmpty();

    assertThat(ncit.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(ncit.getMetadata().getSourceCt()).isGreaterThan(60);
    assertThat(ncit.getMetadata().getLicenseText()).isNull();
    assertThat(ncit.getDescription())
        .isEqualTo(
            "NCI Thesaurus, a controlled vocabulary in support of NCI administrative and "
                + "scientific activities. Produced by the Enterprise Vocabulary System (EVS), "
                + "a project by the NCI Center for Biomedical Informatics and Information "
                + "Technology. National Cancer Institute, National Institutes of Health, "
                + "Bethesda, MD 20892, U.S.A.");

    assertThat(ncit.getLatest()).isTrue();
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
    url = "/api/v1/concept/ncit/C12756?include=full";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C12756");
    assertThat(concept.getTerminology()).isEqualTo("ncit");
    assertThat(concept.getActive()).isTrue();
    assertThat(concept.getParents()).isNotEmpty();
    assertThat(concept.getParents().get(0).getActive()).isNull();

    // Test inactive
    url = "/api/v1/concept/ncit/C4631?include=full";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C4631");
    assertThat(concept.getTerminology()).isEqualTo("ncit");
    assertThat(concept.getActive()).isFalse();
    assertThat(concept.getConceptStatus()).isEqualTo("Retired_Concept");

    // test that "Retired_Concept" was added to the list of concept statuses
    url = "/api/v1/metadata/terminologies?terminology=ncit&latest=true";
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

  /**
   * Test complexrole.
   *
   * @throws Exception the exception
   */
  @Test
  public void testComplexrole() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // This concept has a particular "complex" role - verify that it's present
    // If not there may be a problem in sparql-queries.properties for roles.all.complex
    url = "/api/v1/concept/ncit/C111020?include=roles";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C111020");
    assertThat(
            concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count())
        .isGreaterThan(0);
  }
}
