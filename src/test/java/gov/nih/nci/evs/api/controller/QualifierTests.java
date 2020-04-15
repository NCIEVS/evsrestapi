
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
 * Integration tests for ContentController organized around proper handling of
 * property qualifiers. This is based on work from EVSRESTAPI-69.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class QualifierTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(QualifierTests.class);

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

  /**
   * Test get concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAltDefinitionQualifiers() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C101669?include=definitions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C101669");
    assertThat(concept.getDefinitions().size()).isGreaterThan(0);
    assertThat(concept.getDefinitions().stream()
        .filter(d -> "ALT_DEFINITION".equals(d.getType()) && d.getQualifiers().size() > 0).count())
            .isGreaterThan(0);
    assertThat(concept.getDefinitions().stream()
        .filter(d -> "ALT_DEFINITION".equals(d.getType()) && d.getQualifiers().size() > 0)
        .flatMap(d -> d.getQualifiers().stream()).filter(q -> q.getType().equals("attribution"))
        .count()).isGreaterThan(0);
  }

  /**
   * Test definition qualifiers.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDefinitionQualifiers() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C101046?include=definitions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C101046");
    assertThat(concept.getDefinitions().size()).isGreaterThan(0);
    assertThat(concept.getDefinitions().stream()
        .filter(d -> !"ALT_DEFINITION".equals(d.getType()) && d.getQualifiers().size() > 0).count())
            .isGreaterThan(0);
    assertThat(concept.getDefinitions().stream()
        .filter(d -> !"ALT_DEFINITION".equals(d.getType()) && d.getQualifiers().size() > 0)
        .flatMap(d -> d.getQualifiers().stream()).filter(q -> q.getType().equals("attribution"))
        .count()).isGreaterThan(0);
  }

  /**
   * Test full syn.
   *
   * @throws Exception the exception
   */
  @Test
  public void testFullSyn() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C100065?include=synonyms";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C100065");
    assertThat(concept.getSynonyms().size()).isGreaterThan(0);
    assertThat(concept.getSynonyms().stream().filter(s -> s.getSource() != null
        && s.getSubSource() != null && s.getCode() != null && s.getTermGroup() != null).count())
            .isGreaterThan(0);
  }

  @Test
  public void testMappings() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C101034?include=maps";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C101034");
    assertThat(concept.getMaps().size()).isGreaterThan(0);
    assertThat(concept.getMaps().stream()
        .filter(m -> m.getType() != null && m.getTargetCode() != null
            && m.getTargetTerminologyVersion() != null && m.getTargetTermGroup() != null
            && m.getTargetTerminology() != null)
        .count()).isGreaterThan(0);
  }

  /**
   * Test go annotation.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGoAnnotation() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C19799?include=properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C19799");
    assertThat(concept.getProperties().size()).isGreaterThan(0);
    assertThat(concept.getProperties().stream()
        .filter(p -> p.getType().contentEquals("GO_Annotation")).count()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().contentEquals("GO_Annotation"))
            .flatMap(p -> p.getQualifiers().stream())
            .filter(q -> q.getType().contentEquals("go-evi")).count()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().contentEquals("GO_Annotation"))
            .flatMap(p -> p.getQualifiers().stream())
            .filter(q -> q.getType().contentEquals("go-id")).count()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().contentEquals("GO_Annotation"))
            .flatMap(p -> p.getQualifiers().stream())
            .filter(q -> q.getType().contentEquals("go-source")).count()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().contentEquals("GO_Annotation"))
            .flatMap(p -> p.getQualifiers().stream())
            .filter(q -> q.getType().contentEquals("source-date")).count()).isGreaterThan(0);
  }
  
  // TODO: test that /properties doesn't return overlap with /qualifiers (and vice versa)
  
  // TODO: test that /property doesn't resolve qualifiers and vice versa.
 
  // TODO: test that /properties doesn't return the propertyNotConsidered (and get rid of it)

  // TODO: test that /properties DOES return the "common" properties  -we'll deal with these later
  // EVSUtils.getCommonPropertyNames()

  // TODO: test "attribution/P381" with "summary" mode - verify concept name Is "attr"
  // and synonyms includes an rdfs:label with "attribution"
  
  // TODO: for each qualifier returned by getQualifiers, ensure that a callback based on the name resolves properly.
  
}
