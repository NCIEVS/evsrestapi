package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.LogicalDefinition;
import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

/** Integration tests for ConceptController for "include" flag. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptControllerIncludeTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ConceptControllerIncludeTests.class);

  /** The mvc. */
  @Autowired private MockMvc mvc;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The base url. */
  private String baseUrl = "";

  /** Sets the up. */
  @BeforeEach
  public void setUp() {

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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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

  /** Test logical definitions through both supported API forms. */
  @Test
  public void testLogicalDefinition() throws Exception {
    MvcResult result =
        mvc.perform(get(baseUrl + "/ncit/C3224?include=logicalDefinition"))
            .andExpect(status().isOk())
            .andReturn();
    Concept concept =
        ThreadLocalMapper.get().readValue(result.getResponse().getContentAsString(), Concept.class);
    assertThat(concept.getLogicalDefinition()).isNotNull();
    assertThat(concept.getLogicalDefinition().getCode()).isEqualTo("C3224");

    result =
        mvc.perform(get(baseUrl + "/ncit/C3224/logicalDefinition"))
            .andExpect(status().isOk())
            .andReturn();
    LogicalDefinition definition =
        ThreadLocalMapper.get()
            .readValue(result.getResponse().getContentAsString(), LogicalDefinition.class);
    assertThat(definition.getCode()).isEqualTo("C3224");
    assertThat(definition.getParents()).hasSize(2);
    assertThat(definition.getElements()).hasSize(1);
    assertThat(definition.getElements().get(0).getRoles().get(0).getRangeCode())
        .isEqualTo("C12913");

    result =
        mvc.perform(get(baseUrl + "/ncit/C3224?include=synonyms,logicalDefinition"))
            .andExpect(status().isOk())
            .andReturn();
    concept =
        ThreadLocalMapper.get().readValue(result.getResponse().getContentAsString(), Concept.class);
    assertThat(concept.getSynonyms()).isNotEmpty();
    assertThat(concept.getLogicalDefinition()).isNotNull();
  }

  /** Test logical-definition inclusion for a mixed batch of concepts. */
  @Test
  public void testLogicalDefinitionIncludeForConceptList() throws Exception {
    final MvcResult result =
        mvc.perform(get(baseUrl + "/ncit?list=C3224,C1000&include=logicalDefinition"))
            .andExpect(status().isOk())
            .andReturn();
    final JsonNode concepts =
        ThreadLocalMapper.get().readTree(result.getResponse().getContentAsString());
    assertThat(concepts.isArray()).isTrue();
    assertThat(concepts).hasSize(2);

    JsonNode c3224 = null;
    JsonNode c1000 = null;
    for (final JsonNode concept : concepts) {
      if ("C3224".equals(concept.path("code").asText())) {
        c3224 = concept;
      } else if ("C1000".equals(concept.path("code").asText())) {
        c1000 = concept;
      }
    }
    assertThat(c3224).isNotNull();
    assertThat(c3224.path("logicalDefinition").path("code").asText()).isEqualTo("C3224");
    assertThat(c1000).isNotNull();
    assertThat(c1000.has("logicalDefinition")).isTrue();
    assertThat(c1000.get("logicalDefinition").isNull()).isTrue();
  }

  /** Test explicit null and unsupported-terminology logical-definition responses. */
  @Test
  public void testLogicalDefinitionEdgeCases() throws Exception {
    MvcResult result =
        mvc.perform(get(baseUrl + "/ncit/C1000?include=logicalDefinition"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode json = ThreadLocalMapper.get().readTree(result.getResponse().getContentAsString());
    assertThat(json.has("logicalDefinition")).isTrue();
    assertThat(json.get("logicalDefinition").isNull()).isTrue();

    result =
        mvc.perform(get(baseUrl + "/ncit/C1000/logicalDefinition"))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(result.getResponse().getContentAsString()).isEqualTo("null");

    mvc.perform(get(baseUrl + "/ncim/C0025202/logicalDefinition"))
        .andExpect(status().isNotFound())
        .andExpect(status().reason("Logical definitions are currently supported for NCIt only."));

    mvc.perform(get(baseUrl + "/ncit/BADCODE/logicalDefinition"))
        .andExpect(status().isNotFound())
        .andExpect(status().reason("BADCODE not found"));
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getCode()).isEqualTo("C3224");
    assertThat(concept.getSynonyms()).isNotEmpty();
    assertThat(concept.getDefinitions()).isNotEmpty();
    assertThat(concept.getProperties()).isNotEmpty();
    assertThat(concept.getChildren()).isNotEmpty();
    assertThat(concept.getParents()).isNotEmpty();
    assertThat(concept.getAssociations()).isNotEmpty();
    // check that normName and stemName are not showing up in searches, as is intended
    assertThat(concept.getNormName()).isNull();
    assertThat(concept.getStemName()).isNull();
    assertThat(concept.getSynonyms().get(0).getNormName()).isNull();
    assertThat(concept.getSynonyms().get(0).getStemName()).isNull();
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    // check that normName and stemName are not showing up in searches, as is intended
    assertThat(concept.getNormName()).isNull();
    assertThat(concept.getStemName()).isNull();
    assertThat(concept.getSynonyms().get(0).getNormName()).isNull();
    assertThat(concept.getSynonyms().get(0).getStemName()).isNull();
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
    concept = ThreadLocalMapper.get().readValue(content, Concept.class);
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
        ThreadLocalMapper.get()
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
        ThreadLocalMapper.get()
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
