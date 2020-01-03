
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

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * Integration tests for MetadataController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptControllerTests {

  /** The logger. */
  private static final Logger log =
      LoggerFactory.getLogger(ConceptControllerTests.class);

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
  public void testGetConcept() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C3224";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C3224");
    assertThat(concept.getName()).isEqualTo("Melanoma");

  }

  /**
   * Test bad get concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBadGetConcept() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // Bad terminology
    url = baseUrl + "/ncitXXX/C3224";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Bad lookup
    url = baseUrl + "/ncit/NOTACODE";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }

  /**
   * Test get concepts with list.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptsWithList() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    final String url = baseUrl + "/ncit?list=C3224,XYZ,C131506&include=summary";
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
   * Test get concept associations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptAssociations() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    final String url = baseUrl + "/ncit/C3224/associations";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Association> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Association>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(5);
  }

  /**
   * Test get concept inverse associations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptInverseAssociations() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    final String url = baseUrl + "/ncit/C3224/inverseAssociations";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Association> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Association>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
  }

  /**
   * Test get concept roles.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptRoles() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    final String url = baseUrl + "/ncit/C3224/roles";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Role> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Role>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
  }

  /**
   * Test get concept inverse roles.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptInverseRoles() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    final String url = baseUrl + "/ncit/C3224/inverseRoles";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Role> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Role>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
  }

  /**
   * Test get concept parents.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptParents() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    final String url = baseUrl + "/ncit/C3224/parents";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
  }

  /**
   * Test get concept children.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptChildren() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    final String url = baseUrl + "/ncit/C3224/children";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(5);
  }

  /**
   * Test get concept descendants.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptDescendants() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;

    // NOTE, this includes a middle concept code that is bougs
    url = baseUrl + "/ncit/C7058/descendants";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(5);
    // nothing should have children
    assertThat(list.stream().filter(c -> c.getChildren().size() > 0).count())
        .isEqualTo(0);
    // nothing should be a leaf node
    assertThat(
        list.stream().filter(c -> c.getLeaf() != null && c.getLeaf()).count())
            .isEqualTo(0);

    url = baseUrl + "/ncit/C7058/descendants?maxLevel=2";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(5);
    // something should have children
    assertThat(list.stream().filter(c -> c.getChildren().size() > 0).count())
        .isGreaterThan(0);
    // something should have grand children
    assertThat(list.stream()
        .filter(c -> c.getChildren().size() > 0 && c.getChildren().stream()
            .filter(c2 -> c2.getChildren().size() > 0).count() > 0)
        .count()).isGreaterThan(0);
    // grand children should NOT have children
    assertThat(list.stream()
        .filter(c -> c.getChildren().size() > 0 && c.getChildren().stream()
            .filter(c2 -> c2.getChildren().size() > 0 && c2.getChildren()
                .stream().filter(c3 -> c3.getChildren().size() > 0).count() > 0)
            .count() > 0)
        .count()).isEqualTo(0);
    // Some grand children should be leaf nodes
    assertThat(list.stream().filter(c -> c.getChildren().size() > 0 && c
        .getChildren().stream()
        .filter(c2 -> c2.getChildren().size() > 0 && c2.getChildren().stream()
            .filter(c3 -> c3.getLeaf() != null && c3.getLeaf()).count() > 0)
        .count() > 0).count()).isGreaterThan(0);
  }

  /**
   * Test get concept maps.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptMaps() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    final String url = baseUrl + "/ncit/C3224/maps";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Map> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Map>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();

  }

  /**
   * Test get concept disjoint with.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptDisjointWith() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    final String url = baseUrl + "/ncit/C3910/disjointWith";
    log.info("Testing url - " + url);

    final MvcResult result =
        mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<DisjointWith> list = new ObjectMapper().readValue(content,
        new TypeReference<List<DisjointWith>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(
        list.stream().filter(d -> d.getRelatedCode().equals("C7057")).count())
            .isGreaterThan(0);

  }

  /**
   * Test get roots.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetRoots() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;

    url = baseUrl + "/ncit/roots";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).getSynonyms()).isEmpty();

    url = baseUrl + "/ncit/roots?include=summary";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<Concept>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).getSynonyms()).isNotEmpty();
  }

  /**
   * Test get paths from root.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetPathsFromRoot() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    String url = null;
    MvcResult result = null;
    String content = null;
    List<List<Concept>> list = null;

    url = baseUrl + "/ncit/C3224/pathsFromRoot";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<List<Concept>>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isEmpty();
    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C7057");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode())
        .isEqualTo("C3224");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

    url = baseUrl + "/ncit/C3224/pathsFromRoot?include=summary";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<List<Concept>>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isNotEmpty();
    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C7057");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode())
        .isEqualTo("C3224");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

  }

  /**
   * Test get paths to root.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetPathsToRoot() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    String url = null;
    MvcResult result = null;
    String content = null;
    List<List<Concept>> list = null;

    url = baseUrl + "/ncit/C3224/pathsToRoot";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<List<Concept>>>() {
          // n/a
        });
    log.info("  list = " + list.size());

    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isEmpty();
    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode())
        .isEqualTo("C7057");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

    url = baseUrl + "/ncit/C3224/pathsToRoot?include=summary";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<List<Concept>>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isNotEmpty();
    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode())
        .isEqualTo("C7057");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

  }

  /**
   * Test get paths to ancestor.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetPathsToAncestor() throws Exception {

    // NOTE, this includes a middle concept code that is bougs
    String url = null;
    MvcResult result = null;
    String content = null;
    List<List<Concept>> list = null;

    url = baseUrl + "/ncit/C3224/pathsToAncestor/C2991";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<List<Concept>>>() {
          // n/a
        });
    log.info("  list = " + list.size());

    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isEmpty();
    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode())
        .isEqualTo("C2991");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

    url = baseUrl + "/ncit/C3224/pathsToAncestor/C2991?include=summary";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content,
        new TypeReference<List<List<Concept>>>() {
          // n/a
        });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isNotEmpty();
    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode())
        .isEqualTo("C2991");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

  }

  // TODO: would be nice to have a really comprehensive "include" parameter test
  // -> maybe in its own class
}
