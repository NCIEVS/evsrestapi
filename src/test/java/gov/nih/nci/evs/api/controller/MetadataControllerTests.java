
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

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * Integration tests for MetadataController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MetadataControllerTests {

  /** The logger. */
  private static final Logger log =
      LoggerFactory.getLogger(MetadataControllerTests.class);

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

    baseUrl = "/api/v1/metadata";
  }

  /**
   * Returns the associations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetTerminologies() throws Exception {

    final String url = baseUrl + "/terminologies";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Terminology> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Terminology>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).getTerminology()).isEqualTo("ncit");
  }

  /**
   * Test get associations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAssociations() throws Exception {

    final String url = baseUrl + "/ncit/associations";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();

  }

  /**
   * Test get associations with list.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAssociationsWithList() throws Exception {

    // NOTE, this includes a middle association label that is bogus.
    final String url = baseUrl
        + "/ncit/associations?list=Concept_In_Subset,XYZ,A10&include=summary";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(0).getSynonyms()).isNotEmpty();
    assertThat(list.get(0).getDefinitions()).isNotEmpty();

  }

  /**
   * Test get association.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAssociation() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/association/A8";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isNotEmpty();

    // Test with "by label"
    url = baseUrl + "/ncit/association/Concept_In_Subset";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Concept_In_Subset");
    assertThat(concept.getCode()).isEqualTo("A8");
    assertThat(concept.getSynonyms()).isNotEmpty();

  }

  /**
   * Test bad get association.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBadGetAssociation() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // Bad terminology
    url = baseUrl + "/ncitXXX/association/A8";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Bad lookup (code style)
    url = baseUrl + "/ncit/association/P123456";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Bad lookup (label style)
    url = baseUrl + "/ncit/association/Concept_Out_Of_Subset";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }

  /**
   * Test get roles.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetRoles() throws Exception {

    final String url = baseUrl + "/ncit/roles";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();

  }

  /**
   * Test get roles with list.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetRolesWithList() throws Exception {

    // NOTE, this includes a middle role label that is bogus.
    final String url = baseUrl
        + "/ncit/roles?list=Conceptual_Part_Of,XYZ,R123&include=summary";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(0).getSynonyms()).isNotEmpty();
    assertThat(list.get(0).getDefinitions()).isNotEmpty();

  }

  /**
   * Test get role.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetRole() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/role/R27";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Conceptual_Part_Of");
    assertThat(concept.getCode()).isEqualTo("R27");
    assertThat(concept.getSynonyms()).isNotEmpty();

    // Test with "by label"
    url = baseUrl + "/ncit/role/Conceptual_Part_Of";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Conceptual_Part_Of");
    assertThat(concept.getCode()).isEqualTo("R27");
    assertThat(concept.getSynonyms()).isNotEmpty();

  }

  /**
   * Test bad get role.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBadGetRole() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // Bad terminology
    url = baseUrl + "/ncitXXX/role/R27";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Bad lookup (code style)
    url = baseUrl + "/ncit/role/A123456";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Bad lookup (label style)
    url = baseUrl + "/ncit/role/Not_A_Role";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }

  /**
   * Test get properties.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetProperties() throws Exception {

    final String url = baseUrl + "/ncit/properties";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();

  }

  /**
   * Test get properties with list.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetPropertiesWithList() throws Exception {

    // NOTE, this includes a middle property label that is bogus.
    final String url = baseUrl
        + "/ncit/properties?list=Chemical_Formula,XYZ,P90&include=summary";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(0).getSynonyms()).isNotEmpty();
    assertThat(list.get(0).getDefinitions()).isNotEmpty();

  }

  /**
   * Test get property.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetProperty() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/property/P350";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Chemical_Formula");
    assertThat(concept.getCode()).isEqualTo("P350");
    assertThat(concept.getSynonyms()).isNotEmpty();

    // Test with "by label"
    url = baseUrl + "/ncit/property/Chemical_Formula";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("Chemical_Formula");
    assertThat(concept.getCode()).isEqualTo("P350");
    assertThat(concept.getSynonyms()).isNotEmpty();

  }

  /**
   * Test bad get property.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBadGetProperty() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // Bad terminology
    url = baseUrl + "/ncitXXX/property/P350";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Bad lookup (code style)
    url = baseUrl + "/ncit/property/R123456";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Bad lookup (label style)
    url = baseUrl + "/ncit/property/Not_A_Property";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }

  /**
   * Test concept statuses.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptStatuses() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;

    // Bad terminology
    url = baseUrl + "/ncit/conceptStatuses";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<String> list = new ObjectMapper().readValue(content,
        new TypeReference<List<String>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();

    // Bad terminology
    url = baseUrl + "/ncitXXX/conceptStatuses";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }

  /**
   * Test contributing sources.
   *
   * @throws Exception the exception
   */
  @Test
  public void testContributingSources() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;

    // Bad terminology
    url = baseUrl + "/ncit/contributingSources";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<String> list = new ObjectMapper().readValue(content,
        new TypeReference<List<String>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();

    // Bad terminology
    url = baseUrl + "/ncitXXX/contributingSources";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }
}
