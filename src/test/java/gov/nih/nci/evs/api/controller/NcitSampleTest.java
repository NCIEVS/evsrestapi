package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.List;
import java.util.Set;
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
        ThreadLocalMapper.get()
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
    assertThat(ncit.getName()).isEqualTo("NCI Thesaurus 25.12e");
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
        ThreadLocalMapper.get()
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
    assertThat(ncit.getName()).isEqualTo("NCI Thesaurus 26.01a");
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
        ThreadLocalMapper.get()
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C111020");
    assertThat(
            concept.getRoles().stream().filter(r -> r.getType().equals("Disease_Is_Grade")).count())
        .isGreaterThan(0);
  }

  /**
   * Test role grouping.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRoleGrouping() throws Exception {
    String url = "/api/v1/concept/ncit/C37193?include=roles,properties,parents";
    log.info("Testing url - " + url);
    MvcResult result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    Concept concept = ThreadLocalMapper.get().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C37193");

    // Check for "simple" group (labels)
    assertThat(
            concept.getRoles().stream()
                .filter(r -> r.getGroup() != null && !r.getGroup().matches("\\d+"))
                .count())
        .isGreaterThan(0);

    // Check for numbered groups (complex groups)
    assertThat(
            concept.getRoles().stream()
                .filter(
                    r ->
                        r.getGroup() != null
                            && r.getGroup().matches("\\d+")
                            && Integer.parseInt(r.getGroup()) > 0)
                .count())
        .isGreaterThan(0);

    // Check for Logical_Definition property
    assertThat(
            concept.getProperties().stream()
                .filter(
                    p -> "Logical_Definition".equals(p.getType()) && "true".equals(p.getValue()))
                .count())
        .isEqualTo(1);

    // Check for defining parents
    assertThat(concept.getParents().stream().filter(p -> p.getDefining() != null && p.getDefining()).count())
        .isEqualTo(1);
    assertThat(concept.getParents().stream().filter(p -> p.getDefining() != null && p.getDefining()).findFirst().get().getCode())
        .isEqualTo("C3720");

    // Concept with roles but NO role groupings
    url = "/api/v1/concept/ncit/C10000?include=roles,properties,parents";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C10000");

    // has roles
    assertThat(concept.getRoles()).isNotEmpty();

    // no roles have a group value
    assertThat(concept.getRoles().stream().filter(r -> r.getGroup() != null).count()).isEqualTo(0);

    // no Logical_Definition property
    assertThat(
            concept.getProperties().stream()
                .filter(p -> "Logical_Definition".equals(p.getType()))
                .count())
        .isEqualTo(0);

    // Third case: C6627 has no groups but has 3 defining parents
    url = "/api/v1/concept/ncit/C6627?include=roles,properties,parents";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C6627");

    // no role groupings
    assertThat(concept.getRoles().stream().filter(r -> r.getGroup() != null).count()).isEqualTo(0);

    // 3 defining parents: C3549, C6624, C6594
    Set<String> definingParents = concept.getParents().stream()
        .filter(p -> p.getDefining() != null && p.getDefining())
        .map(Concept::getCode)
        .collect(java.util.stream.Collectors.toSet());
    assertThat(definingParents).containsExactlyInAnyOrder("C3549", "C6624", "C6594");
  }
}
