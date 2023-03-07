
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * Integration tests for NCI loader.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class NciControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(NciControllerTests.class);

  /** The mvc. */
  @Autowired
  private MockMvc mvc;

  /** The test properties. */
  @Autowired
  TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /**
   * Sets the up.
   */
  @Before
  public void setUp() {
    /*
     * Configure the JacksonTester object
     */
    this.objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

  }

  @Test
  public void testNCITerminologyMonthly() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // test if mdr term exists
    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = mvc
        .perform(
            get(url).param("latest", "true").param("tag", "monthly").param("terminology", "ncit"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("ncit")).count())
        .isEqualTo(1);
    final Terminology ncit =
        terminologies.stream().filter(t -> t.getTerminology().equals("ncit")).findFirst().get();
    assertThat(ncit.getTerminology()).isEqualTo("ncit");
    assertThat(ncit.getMetadata().getUiLabel()).isEqualTo("NCI Thesaurus");
    assertThat(ncit.getName()).isEqualTo("NCI Thesaurus 21.06e");
    assertThat(ncit.getDescription()).isNotEmpty();

    // TODO: These need fixing
    assertThat(ncit.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(ncit.getMetadata().getSourceCt()).isGreaterThan(60);
    assertThat(ncit.getMetadata().getLicenseText()).isNull();
    assertThat(ncit.getDescription())
        .isEqualTo("NCI Thesaurus, a controlled vocabulary in support of NCI administrative and "
            + "scientific activities. Produced by the Enterprise Vocabulary System (EVS), "
            + "a project by the NCI Center for Biomedical Informatics and Information "
            + "Technology. National Cancer Institute, National Institutes of Health, "
            + "Bethesda, MD 20892, U.S.A.");

    assertThat(ncit.getLatest()).isTrue();
  }
  
  @Test
  public void testNCITerminologyWeekly() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // test if mdr term exists
    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = mvc
        .perform(
            get(url).param("latest", "true").param("tag", "weekly").param("terminology", "ncit"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("ncit")).count())
        .isEqualTo(1);
    final Terminology ncit =
        terminologies.stream().filter(t -> t.getTerminology().equals("ncit")).findFirst().get();
    assertThat(ncit.getTerminology()).isEqualTo("ncit");
    assertThat(ncit.getMetadata().getUiLabel()).isEqualTo("NCI Thesaurus");
    assertThat(ncit.getName()).isEqualTo("NCI Thesaurus 21.07a");
    assertThat(ncit.getDescription()).isNotEmpty();

    // TODO: These need fixing
    assertThat(ncit.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(ncit.getMetadata().getSourceCt()).isGreaterThan(60);
    assertThat(ncit.getMetadata().getLicenseText()).isNull();
    assertThat(ncit.getDescription())
        .isEqualTo("NCI Thesaurus, a controlled vocabulary in support of NCI administrative and "
            + "scientific activities. Produced by the Enterprise Vocabulary System (EVS), "
            + "a project by the NCI Center for Biomedical Informatics and Information "
            + "Technology. National Cancer Institute, National Institutes of Health, "
            + "Bethesda, MD 20892, U.S.A.");

    assertThat(ncit.getLatest()).isTrue();
  }
}
