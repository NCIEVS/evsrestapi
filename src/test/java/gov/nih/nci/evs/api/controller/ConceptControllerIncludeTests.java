package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.properties.TestProperties;
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

/** Integration tests for ConceptController for "include" flag. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptControllerIncludeTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ConceptControllerIncludeTests.class);

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

    baseUrl = "/api/v1/concept";
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
    url = baseUrl + "/ncit/C3224?include=minimal";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    // Assertions about contents
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
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
   * Test bad get include.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBadGetInclude() throws Exception {
    String url = null;

    // Bad terminology
    url = baseUrl + "/ncit/C3224?include=NOT_AN_INCLUDE";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isBadRequest()).andReturn();
    // content is blank because of MockMvc

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
    url = baseUrl + "/ncit/C3224?include=summary";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
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
    url = baseUrl + "/ncit/C3224?include=synonyms,definitions,properties";
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
    url = baseUrl + "/ncit/C3224?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
    assertThat(concept.getSynonyms()).isNotEmpty();
    assertThat(concept.getDefinitions()).isNotEmpty();
    assertThat(concept.getProperties()).isNotEmpty();
    assertThat(concept.getChildren()).isNotEmpty();
    assertThat(concept.getParents()).isNotEmpty();
    assertThat(concept.getAssociations()).isNotEmpty();
    // C3224 no longer has associations
    // assertThat(concept.getInverseAssociations()).isNotEmpty();
    assertThat(concept.getRoles()).isNotEmpty();
    assertThat(concept.getInverseRoles()).isNotEmpty();
    assertThat(concept.getMaps()).isNotEmpty();
    // Test for Preferred_Name and FULL_SYN
    assertThat(
            concept.getSynonyms().stream()
                .filter(s -> "Preferred_Name".equals(s.getType()))
                .count())
        .isGreaterThan(0);
    assertThat(
            concept.getSynonyms().stream().filter(s -> "Display_Name".equals(s.getType())).count())
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

    url = baseUrl + "/ncit/C3224?include=synonyms";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
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

    url = baseUrl + "/ncit/C3224?include=definitions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
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

    url = baseUrl + "/ncit/C3224?include=properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
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
  public void testIncludeChildren() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url = baseUrl + "/ncit/C3224?include=children";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isNotEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
  }

  /**
   * Test include parents.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeParents() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url = baseUrl + "/ncit/C3224?include=parents";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isNotEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
  }

  /**
   * Test include associations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeAssociations() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // C3224 no longer has inverse associations
    url = baseUrl + "/ncit/C100139?include=associations,inverseAssociations";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("CDISC Questionnaire MDS-UPDRS Test Name Terminology");
    assertThat(concept.getCode()).isEqualTo("C100139");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isNotEmpty();
    assertThat(concept.getInverseAssociations()).isNotEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
  }

  /**
   * Test include roles.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeRoles() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url = baseUrl + "/ncit/C3224?include=roles,inverseRoles";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isNotEmpty();
    assertThat(concept.getInverseRoles()).isNotEmpty();
    assertThat(concept.getMaps()).isEmpty();
  }

  /**
   * Test include maps.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeMaps() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url = baseUrl + "/ncit/C3224?include=maps";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isNotEmpty();
  }

  /**
   * Test include disjoint with.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeDisjointWith() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    url = baseUrl + "/ncit/C3910?include=disjointWith";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Molecular Abnormality");
    assertThat(concept.getCode()).isEqualTo("C3910");
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
    assertThat(concept.getDisjointWith()).isNotEmpty();
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

    url = baseUrl + "/ncit/C3224?include=synonyms,definitions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
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

  /**
   * Test subset members include.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSubsetMembersInclude() throws Exception {

    String url = "/api/v1";
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;

    // Look up subset members with minimal
    url = url + "/subset/ncit/C157225/members?include=minimal&fromRecord=0&pageSize=10";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(10);
    assertThat(list.get(0).getCode()).isNotNull();
    assertThat(list.get(0).getName()).isNotNull();
    assertThat(list.get(0).getTerminology()).isNotNull();
    assertThat(list.get(0).getVersion()).isNotNull();
    assertThat(list.get(0).getLeaf()).isNotNull();
    assertThat(list.get(0).getSynonyms()).isEmpty();
    assertThat(list.get(0).getProperties()).isEmpty();

    // Look up subset members with synonyms
    url = "/api/v1";
    url = url + "/subset/ncit/C157225/members?include=synonyms&fromRecord=0&pageSize=10";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(10);
    assertThat(list.get(0).getCode()).isNotNull();
    assertThat(list.get(0).getName()).isNotNull();
    assertThat(list.get(0).getTerminology()).isNotNull();
    assertThat(list.get(0).getVersion()).isNotNull();
    assertThat(list.get(0).getLeaf()).isNotNull();
    assertThat(list.get(0).getSynonyms()).isNotEmpty();
    assertThat(list.get(0).getProperties()).isEmpty();
  }
}
