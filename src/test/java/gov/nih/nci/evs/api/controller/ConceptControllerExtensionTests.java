
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * Integration tests for MetadataController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptControllerExtensionTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ConceptControllerExtensionTests.class);

  /** The mvc. */
  @Autowired
  private MockMvc mvc;

  /** The test properties. */
  @Autowired
  TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** The base url. */
  private String baseUrl = "";

  /**
   * Sets the up.
   */
  @Before
  public void setUp() {

    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1/concept";
  }

  @Test
  public void testNotSubtypeStageOrGrade() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;
    String code = null;

    // Test positive assertion case
    code = "C100007";
    url = baseUrl + "/ncit/" + code + "?include=extensions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotEmpty();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    // Assertions about contents
    assertThat(concept).isNotNull();
    assertThat(concept.getExtensions()).isNotNull();
    assertThat(concept.getExtensions().getIsDiseaseStage()).isFalse();
    assertThat(concept.getExtensions().getIsDiseaseGrade()).isFalse();
    assertThat(concept.getExtensions().getIsMainType()).isFalse();
    assertThat(concept.getExtensions().getIsSubtype()).isFalse();
    assertThat(concept.getExtensions().getIsDisease()).isFalse();
    assertThat(concept.getExtensions().getIsBiomarker()).isFalse();
    assertThat(concept.getExtensions().getIsReferenceGene()).isFalse();
    assertThat(concept.getExtensions().getMainMenuAncestors()).isNull();

  }

  /**
   * Test is disease stage.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIsDiseaseStage() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;
    String code = null;

    // Test positive assertion case
    code = "C102845";
    url = baseUrl + "/ncit/" + code + "?include=extensions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotEmpty();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    // Assertions about contents
    assertThat(concept).isNotNull();
    assertThat(concept.getExtensions()).isNotNull();
    assertThat(concept.getExtensions().getIsDiseaseStage()).isTrue();

    // Test positive assertion case
    code = "C3224";
    url = baseUrl + "/ncit/" + code + "?include=extensions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotEmpty();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    // Assertions about contents
    assertThat(concept).isNotNull();
    assertThat(concept.getExtensions()).isNotNull();
    assertThat(concept.getExtensions().getIsDiseaseStage()).isFalse();

  }

}
