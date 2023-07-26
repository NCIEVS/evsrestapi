
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
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * subset tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SubsetControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MetadataControllerTests.class);

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

    baseUrl = "/api/v1/";
  }

  @Test
  public void testIncludeSubsets() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // minimal
    url = baseUrl + "subset/ncit/C116978?include=minimal";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("CTRP Agent Terminology");
    assertThat(concept.getCode()).isEqualTo("C116978");
    assertThat(concept.getLeaf()).isNotNull();
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    // DO NOT always include properties for "minimal"
    assertThat(concept.getProperties()).isEmpty();
    // Always include children, but no children
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();

    url = baseUrl + "subset/ncit/C116978?include=synonyms,properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("CTRP Agent Terminology");
    assertThat(concept.getCode()).isEqualTo("C116978");
    assertThat(concept.getLeaf()).isNotNull();
    assertThat(concept.getSynonyms()).isNotEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isNotEmpty();
    assertThat(concept.getChildren()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type")).count())
        .isGreaterThan(0);

    // /subset - minimal
    url = baseUrl + "subset/ncit?include=properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    List<Concept> list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    concept = list.get(0);
    assertThat(concept.getName()).isEqualTo("ACC/AHA EHR Terminology");
    assertThat(concept.getCode()).isEqualTo("C167405");
    assertThat(concept.getSynonyms()).isEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    // Always include properties
    assertThat(concept.getProperties()).isNotEmpty();
    // Children are set automatically (because this is a graph)
    assertThat(concept.getChildren()).isNotEmpty();
    // Check the first child also for just properties
    assertThat(concept.getChildren().get(0).getSynonyms()).isEmpty();
    assertThat(concept.getChildren().get(0).getProperties()).isNotEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type")).count())
        .isGreaterThan(0);

    // /subset - properties
    url = baseUrl + "subset/ncit?include=synonyms,properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    concept = list.get(0);
    assertThat(concept.getName()).isEqualTo("ACC/AHA EHR Terminology");
    assertThat(concept.getCode()).isEqualTo("C167405");
    assertThat(concept.getSynonyms()).isNotEmpty();
    assertThat(concept.getDefinitions()).isEmpty();
    assertThat(concept.getProperties()).isNotEmpty();
    // Children are set automatically (because this is a graph)
    assertThat(concept.getChildren()).isNotEmpty();
    // Check the first child also for just properties
    assertThat(concept.getChildren().get(0).getParents()).isEmpty();
    assertThat(concept.getChildren().get(0).getChildren()).isEmpty();
    assertThat(concept.getChildren().get(0).getSynonyms()).isNotEmpty();
    assertThat(concept.getChildren().get(0).getProperties()).isNotEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type")).count())
        .isGreaterThan(0);

  }

  /**
   * Test top level of subsets.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTopLevelSubsetSearch() throws Exception {
    String url = baseUrl + "subset/ncit?include=properties";
    MvcResult result = null;
    List<Concept> list = null;
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list != null && list.size() > 0).isTrue();
    // No subsets or children should have Publish_Value_Set set to something other than "Yes".
    assertThat(list.stream().flatMap(Concept::streamSelfAndChildren)
        .filter(c -> c.getProperties().stream()
            .filter(p -> p.getType().equals("Publish_Value_Set") && !p.getValue().equals("Yes"))
            .count() > 0)
        .count()).isEqualTo(0);

  }

  /**
   * Test specific subset search.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSpecificSubsetSearch() throws Exception {
    String url = baseUrl + "subset/ncit/C167405";
    MvcResult result = null;
    Concept list = null;
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, Concept.class);
    assertThat(list != null && list.getChildren().size() > 0).isTrue();
    // check that no subsetLink (no valid download)
    assertThat(list != null && list.getSubsetLink() == null).isTrue();

    // This value is specifically set in the unit test data via P374
    url = baseUrl + "subset/ncit/C100110?include=full";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, Concept.class);
    assertThat(list.getName()).isEqualTo("CDISC Questionnaire Terminology");
    assertThat(list.getCode()).isEqualTo("C100110");
    assertThat(list.getChildren()).isNotEmpty();
    assertThat(list.getDefinitions()).isNotEmpty();
    assertThat(list.getProperties()).isNotEmpty();
    assertThat(list.getLeaf()).isFalse();

    url = baseUrl + "subset/ncit/C116977";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, Concept.class);
    List<Property> sources = list.getProperties();
    for (Property source : sources) {
      if (source.getType() == "Contributing_Source") {
        assertThat(source.getValue() == "CTRP");
        break;
      }
    }

  }

}