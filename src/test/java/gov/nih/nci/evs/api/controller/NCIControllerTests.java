
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
public class NCIControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(NCIControllerTests.class);

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

  /**
   * NCI terminology basic tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNCITerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // test if nci term exists
    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("ncit")).count())
        .isEqualTo(2);
    final Terminology nci = terminologies.stream()
        .filter(t -> t.getTerminology().equals("ncit") && t.getLatest() != null && t.getLatest())
        .findFirst().get();
    assertThat(nci.getTerminology()).isEqualTo("ncit");
    assertThat(nci.getMetadata().getUiLabel()).isEqualTo("NCI Thesaurus");
    assertThat(nci.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(nci.getMetadata().getSourceCt()).isGreaterThan(1);
    assertThat(nci.getMetadata().getLicenseText()).isNull();
    assertThat(nci.getLatest()).isTrue();
  }

  /**
   * Test NCI terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNCITerminology2() throws Exception {
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
    assertThat(ncit.getMetadata().getSourceCt()).isEqualTo(15);
    assertThat(ncit.getMetadata().getLicenseText()).isNotNull();
    assertThat(ncit.getName())
        .isEqualTo("Medical Dictionary for Regulatory Activities Terminology (MedDRA), 23_1");
    assertThat(ncit.getDescription()).isEqualTo(";;MedDRA MSSO;;MedDRA [electronic resource]"
        + " : Medical Dictionary for Regulatory Activities Terminology;;;"
        + "Version 23.1;;MedDRA MSSO;;September, 2020;;;;MedDRA "
        + "[electronic resource] : Medical Dictionary for Regulatory Activities Terminology");
    ;
    assertThat(ncit.getLatest()).isTrue();
  }
}
