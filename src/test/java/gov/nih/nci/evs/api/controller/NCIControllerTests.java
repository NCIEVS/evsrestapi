
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

}
