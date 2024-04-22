package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.properties.TestProperties;
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

/** Integration tests for MetadataController for "include" parameter. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MetadataControllerIncludeTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MetadataControllerIncludeTests.class);

  /** The mvc. */
  @Autowired private MockMvc mvc;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** The base url. */
  private String baseUrl = "";

  /** Sets the up. */
  @Before
  public void setUp() {

    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1/metadata";
  }

  /**
   * Test include.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeMinimal() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "minimal"
    url = baseUrl + "/ncit/association/A8?include=minimal";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    // Assertions about contents
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
  }

  /**
   * Test include summary.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeSummary() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;
    // Test with "summary"
    url = baseUrl + "/ncit/association/A8?include=summary";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isNotEmpty();
    assertThat(concept.getDefinitions()).isNotEmpty();
    assertThat(concept.getProperties()).isNotEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
    // Test for Preferred_Name and FULL_SYN
    assertThat(
            concept.getSynonyms().stream()
                .filter(s -> s.getType().equals("Preferred_Name"))
                .count())
        .isGreaterThan(0);
    assertThat(concept.getSynonyms().stream().filter(s -> s.getType().equals("FULL_SYN")).count())
        .isGreaterThan(0);
    // Test properties are "by label"
    assertThat(
            concept.getProperties().stream()
                .filter(p -> p.getType().equals("Semantic_Type"))
                .count())
        .isGreaterThan(0);

    // Test "summary" is equal to "synonyms,definitions,properties"
    url = baseUrl + "/ncit/association/A8?include=synonyms,definitions,properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content2 = result.getResponse().getContentAsString();
    log.info(" content2 = " + content2);
    assertThat(content).isEqualTo(content);
  }

  /**
   * Test include full.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeFull() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "full" - same results as summary, actually
    url = baseUrl + "/ncit/association/A8?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isNotEmpty();
    assertThat(concept.getDefinitions()).isNotEmpty();
    assertThat(concept.getProperties()).isNotEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
    // Test for Preferred_Name and FULL_SYN
    assertThat(
            concept.getSynonyms().stream()
                .filter(s -> "Preferred_Name".equals(s.getType()))
                .count())
        .isGreaterThan(0);
    assertThat(concept.getSynonyms().stream().filter(s -> "FULL_SYN".equals(s.getType())).count())
        .isGreaterThan(0);
    // Test properties are "by label"
    assertThat(
            concept.getProperties().stream()
                .filter(p -> "Semantic_Type".equals(p.getType()))
                .count())
        .isGreaterThan(0);
  }

  /**
   * Test include synonyms.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeSynonyms() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url = baseUrl + "/ncit/association/A8?include=synonyms";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isNotEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
    // Test for Preferred_Name and FULL_SYN
    assertThat(
            concept.getSynonyms().stream()
                .filter(s -> s.getType().equals("Preferred_Name"))
                .count())
        .isGreaterThan(0);
    assertThat(concept.getSynonyms().stream().filter(s -> s.getType().equals("FULL_SYN")).count())
        .isGreaterThan(0);
  }

  /**
   * Test include synonyms.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeDefinitions() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url = baseUrl + "/ncit/association/A8?include=definitions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isNotEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
  }

  /**
   * Test include properties.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeProperties() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url = baseUrl + "/ncit/association/A8?include=properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isNotEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
    // Test properties are "by label"
    assertThat(
            concept.getProperties().stream()
                .filter(p -> p.getType().equals("Semantic_Type"))
                .count())
        .isGreaterThan(0);
  }

  /**
   * Test include other empty parts.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeOtherEmptyParts() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url =
        baseUrl
            + "/ncit/association/A8?include=children,parents,associations,inverseAssociations,roles,inverseRoles,maps,disjointWith";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
  }

  /**
   * Test include synonyms and definitions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeSynonymsAndDefinitions() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url = baseUrl + "/ncit/association/A8?include=synonyms,definitions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isNotEmpty();
    assertThat(concept.getDefinitions()).isNotEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
  }
}
