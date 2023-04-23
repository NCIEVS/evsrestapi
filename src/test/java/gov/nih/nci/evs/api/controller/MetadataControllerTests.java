
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.google.common.collect.Sets;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Map;
import gov.nih.nci.evs.api.model.MapResultList;
import gov.nih.nci.evs.api.model.Property;
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

    baseUrl = "/api/v1/metadata";
  }

  /**
   * Returns the associations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetTerminologies() throws Exception {

    String url = baseUrl + "/terminologies";
    log.info("Testing url - " + url);

    MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    List<Terminology> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    assertThat(list.stream().map(t -> t.getTerminology())).contains("ncit");
    assertThat(list.stream().map(t -> t.getTerminology())).contains("ncim");
    assertThat(list.stream().map(t -> t.getTerminology())).contains("mdr");

    url = baseUrl + "/terminologies?terminology=ncit&latest=true";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });
    for (Terminology term : list) {
      assertThat(term.getLatest() == true);
      assertThat(term.getTerminology() == "ncit");
    }

    url = baseUrl + "/terminologies?terminology=ncit&tags=monthly";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });
    for (Terminology term : list) {
      assertThat(term.getTags().get("monthly") == "true");
      assertThat(term.getTerminology() == "ncit");
    }

    url = baseUrl + "/terminologies?tags=monthly&latest=true";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });
    for (Terminology term : list) {
      assertThat(term.getTags().get("monthly") == "true");
      assertThat(term.getLatest() == true);
    }

    url = baseUrl + "/terminologies?terminology=ncit&tags=monthly&latest=true";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });
    for (Terminology term : list) {
      assertThat(term.getTags().get("monthly") == "true");
      assertThat(term.getLatest() == true);
      assertThat(term.getTerminology() == "ncit");
    }
  }

  /**
   * Test get terminology metadata.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetTerminologyMetadata() throws Exception {

    // Test NCIt
    String url = baseUrl + "/terminologies?terminology=ncit&tag=monthly&latest=true";
    log.info("Testing url - " + url);

    MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    List<Terminology> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    Terminology term = list.get(0);
    assertThat(term.getMetadata()).isNotNull();
    // term types are no longer returned with the terminology metadata
    // these are retrievable from the /termTypes metdata endpoint
    // assertThat(term.getMetadata().getTermTypes().size()).isGreaterThan(10);

    // Test NCIm
    url = baseUrl + "/terminologies?terminology=ncim&latest=true";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    term = list.get(0);
    assertThat(term.getMetadata()).isNotNull();
    // term types are no longer returned with the terminology metadata
    // these are retrievable from the /termTypes metdata endpoint
    // assertThat(term.getMetadata().getTermTypes().size()).isGreaterThan(10);

    // Test mdr
    url = baseUrl + "/terminologies?terminology=mdr&latest=true";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    term = list.get(0);
    assertThat(term.getMetadata()).isNotNull();
    // term types are no longer returned with the terminology metadata
    // these are retrievable from the /termTypes metdata endpoint
    // assertThat(term.getMetadata().getTermTypes().size()).isGreaterThan(5);

  }

  /**
   * Test get hierarchy flag.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetHierarchy() throws Exception {

    String url = baseUrl + "/terminologies?terminology=ncit&tag=monthly&latest=true";
    log.info("Testing url - " + url);

    MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    List<Terminology> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
          // n/a
        });

    for (Terminology term : list) {
      assertThat(term.getMetadata().getHierarchy() == true);
    }

    url = baseUrl + "/terminologies?terminology=ncim";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });

    for (Terminology term : list) {
      assertThat(term.getMetadata().getHierarchy() == null);
    }

    url = baseUrl + "/terminologies?terminology=mdr";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });

    for (Terminology term : list) {
      assertThat(term.getMetadata().getHierarchy() == true);
    }
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

    final MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    // Assert that no cases involve a concept with a name having a space
    assertThat(list.stream().filter(c -> c.getName().contains(" ")).count()).isEqualTo(0);

  }

  /**
   * Test get associations with list.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAssociationsWithList() throws Exception {

    // NOTE, this includes a middle association label that is bogus.
    final String url =
        baseUrl + "/ncit/associations?list=Concept_In_Subset,XYZ,A10&include=summary";
    log.info("Testing url - " + url);

    final MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
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

    // Test with "by code" with include=full
    url = baseUrl + "/ncit/association/A8?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getCode()).isEqualTo("A8");
    // Even full doesn't include descendants and paths
    assertThat(concept.getDescendants()).isEmpty();
    assertThat(concept.getPaths()).isNull();

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

    // Test with comma separated list
    url = baseUrl + "/ncit/association/A15,A12";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Test with comma separated list
    url = baseUrl + "/ncit/association/A15,Concept_In_Subset";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

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

    // Bad lookup (code style)
    url = baseUrl + "/ncit/association/A";
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

    // Bad lookup (not an association)
    url = baseUrl + "/ncit/association/P90";
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

    final MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    // Assert that no cases involve a concept with a name having a space
    assertThat(list.stream().filter(c -> c.getName().contains(" ")).count()).isEqualTo(0);

  }

  /**
   * Test get roles with list.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetRolesWithList() throws Exception {

    // NOTE, this includes a middle role label that is bogus.
    final String url = baseUrl + "/ncit/roles?list=Conceptual_Part_Of,XYZ,R123&include=summary";
    log.info("Testing url - " + url);

    final MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
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

    // Test with "by code", include=full
    url = baseUrl + "/ncit/role/R27?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getCode()).isEqualTo("R27");
    // Even full doesn't include descendants and paths
    assertThat(concept.getDescendants()).isEmpty();
    assertThat(concept.getPaths()).isNull();

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

    // Test with comma separated list
    url = baseUrl + "/ncit/role/R27,R100";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Test with comma separated list
    url = baseUrl + "/ncit/role/R100,Conceptual_Part_Of";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

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

    // Bad lookup (code style)
    url = baseUrl + "/ncit/role/R";
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

    // Bad lookup (not a role)
    url = baseUrl + "/ncit/role/P90";
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

    String url = baseUrl + "/ncit/properties";
    log.info("Testing url - " + url);

    MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    List<Concept> list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();

    // Assert that properties don't contain any "remodeled properties"
    url = baseUrl + "/terminologies";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    Terminology terminology =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
          // n/a
        }).stream().filter(t -> t.getTerminology().equals("ncit")).findFirst().get();
    assertThat(list.stream().filter(c -> terminology.getMetadata().isRemodeledProperty(c.getCode()))
        .count()).isEqualTo(0);

    // BAC: this is not true anymore because we've
    // Assert that no cases involve a concept with a name having a space
    // assertThat(list.stream().filter(c -> c.getName().contains("
    // ")).count()).isEqualTo(0);

    // In "minimal" mode (which is default), there shouldn't be any rdfs:labels
    assertThat(list.stream()
        .filter(
            c -> c.getSynonyms().stream().filter(s -> s.getType().equals("rdfs:label")).count() > 0)
        .count()).isEqualTo(0);

  }

  /**
   * Test get properties with list.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetPropertiesWithList() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;

    // NOTE, this includes a middle property label that is bogus.
    url = baseUrl + "/ncit/properties?list=Chemical_Formula,XYZ,P216&include=summary";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
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

    // Test with "by code", include=full
    url = baseUrl + "/ncit/property/P350?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getCode()).isEqualTo("P350");
    // Even full doesn't include descendants and paths
    assertThat(concept.getDescendants()).isEmpty();
    assertThat(concept.getPaths()).isNull();

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

    // Test with comma separated list
    url = baseUrl + "/ncit/property/P350,P106";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Test with comma separated list
    url = baseUrl + "/ncit/property/P350,OMIM_Number";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

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

    // P90 should no longer be a property
    url = baseUrl + "/ncit/property/P90";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // P97 should no longer be a property
    url = baseUrl + "/ncit/property/P97";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

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

    // Bad lookup (code style)
    url = baseUrl + "/ncit/property/P";
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

    // Lookup of an association instead of a property
    url = baseUrl + "/ncit/property/A8";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

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
    final List<String> list =
        new ObjectMapper().readValue(content, new TypeReference<List<String>>() {
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
   * Test synonym sources.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSynonymSources() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;

    // NCIt
    url = baseUrl + "/ncit/synonymSources";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    // NOTE: This may change in the future
    assertThat(list.size()).isGreaterThan(40);

    // Bad terminology
    url = baseUrl + "/ncitXXX/synonymSources";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }

  /**
   * Test definition sources.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDefinitionSources() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;

    // NCIt
    url = baseUrl + "/ncit/definitionSources";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();
    // This may change in the future, but it's a way of validating
    // "definition sources" as being different than "synonym sources"
    assertThat(list.size()).isLessThan(50);

    // Bad terminology
    url = baseUrl + "/ncitXXX/definitionSources";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }

  /**
   * Test term types.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTermTypes() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;

    // NCIt
    url = baseUrl + "/ncit/termTypes";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();

    // Bad terminology
    url = baseUrl + "/ncitXXX/termTypes";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }

  /**
   * Test property qualifiers.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPropertyQualifiers() throws Exception {

    String url = null;
    MvcResult result = null;
    Concept concept = null;
    String content = null;

    // Test with "by code"
    url = baseUrl + "/ncit/qualifier/P381";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("attribution");
    assertThat(concept.getCode()).isEqualTo("P381");
    assertThat(concept.getSynonyms()).isNotEmpty();

    // Test with "by code", full=include
    url = baseUrl + "/ncit/qualifier/P381?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getCode()).isEqualTo("P381");
    // Even full doesn't include descendants and paths
    assertThat(concept.getDescendants()).isEmpty();
    assertThat(concept.getPaths()).isNull();

    // P381
    url = baseUrl + "/ncit/qualifier/P381/values";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotEmpty();

    // attribution
    url = baseUrl + "/ncit/qualifier/attribution/values";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    // Bad terminology
    url = baseUrl + "/ncitXXX/qualifier/P381/values";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();

    // Bad property
    url = baseUrl + "/ncit/qualifier/P381XXX/values";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();

    // P383 should no longer be a property
    url = baseUrl + "/ncit/qualifier/P383";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

  }

  /**
   * Test synonym types.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSynonymTypes() throws Exception {

    String url = null;
    MvcResult result = null;
    Concept concept = null;
    String content = null;

    // Test with "by code"
    url = baseUrl + "/ncit/synonymType/P90";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("FULL_SYN");
    assertThat(concept.getCode()).isEqualTo("P90");
    assertThat(concept.getSynonyms()).isNotEmpty();

    // Test with "by code", full=include
    url = baseUrl + "/ncit/synonymType/P90?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getCode()).isEqualTo("P90");
    // Even full doesn't include descendants and paths
    assertThat(concept.getDescendants()).isEmpty();
    assertThat(concept.getPaths()).isNull();

    // Bad terminology
    url = baseUrl + "/ncitXXX/synonymType/P90";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();

    // Bad property
    url = baseUrl + "/ncit/synonymType/P90XXX";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();

  }

  /**
   * Test definition types.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDefinitionTypes() throws Exception {

    String url = null;
    MvcResult result = null;
    Concept concept = null;
    String content = null;

    // Test with "by code"
    url = baseUrl + "/ncit/definitionType/P97";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getName()).isEqualTo("DEFINITION");
    assertThat(concept.getCode()).isEqualTo("P97");
    assertThat(concept.getSynonyms()).isNotEmpty();

    // Test with "by code", full=include
    url = baseUrl + "/ncit/definitionType/P97?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getCode()).isEqualTo("P97");
    // Even full doesn't include descendants and paths
    assertThat(concept.getDescendants()).isEmpty();
    assertThat(concept.getPaths()).isNull();

    // Bad terminology
    url = baseUrl + "/ncitXXX/definitionType/P97";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();

    // Bad property
    url = baseUrl + "/ncit/synonymType/P97XXX";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();

  }

  /**
   * Test keep type: value out of qualifiers.
   *
   * @throws Exception the exception
   */
  @Test
  public void testQualifiersWithoutTypeValue() throws Exception {
    String url = null;
    MvcResult result = null;
    Concept concept = null;
    String content = null;

    // Test all
    url = baseUrl + "/ncit/qualifiers?include=minimal";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    List<Concept> list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });

    // test unpublished isn't in the list
    assertThat(list).isNotEmpty();
    List<String> unpublished = Arrays.asList("P379", "P380");
    List<String> properties = list.stream().map(Concept::getCode).collect(Collectors.toList());
    assertThat(Collections.disjoint(properties, unpublished));

    // Test with P389
    url = baseUrl + "/ncit/qualifiers?include=summary&list=P389";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    concept = list.get(0);
    for (Property prop : concept.getProperties()) {
      assertThat(!prop.getType().equals("value"));
    }

    // Test with P391
    url = baseUrl + "/ncit/qualifiers?include=summary&list=P391";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    concept = list.get(0);
    for (Property prop : concept.getProperties()) {
      assertThat(!prop.getType().equals("value"));
    }
  }

  @Test
  public void testMutuallyExclusive() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;

    // Properties
    url = baseUrl + "/ncit/properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    Set<String> properties =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        }).stream().map(c -> c.getCode()).collect(Collectors.toSet());

    // Qualifiers
    url = baseUrl + "/ncit/qualifiers";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    Set<String> qualifiers =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        }).stream().map(c -> c.getCode()).collect(Collectors.toSet());

    // Synonym Types
    url = baseUrl + "/ncit/synonymTypes";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    Set<String> synonymTypes =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        }).stream().map(c -> c.getCode()).collect(Collectors.toSet());

    // Definition Types
    url = baseUrl + "/ncit/definitionTypes";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    Set<String> definitionTypes =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        }).stream().map(c -> c.getCode()).collect(Collectors.toSet());

    // All sets are mutually exclusive with respect to each other.
    assertThat(Sets.intersection(properties, qualifiers)).isEmpty();
    assertThat(Sets.intersection(properties, synonymTypes)).isEmpty();
    assertThat(Sets.intersection(properties, definitionTypes)).isEmpty();
    assertThat(Sets.intersection(qualifiers, synonymTypes)).isEmpty();
    assertThat(Sets.intersection(qualifiers, definitionTypes)).isEmpty();
    assertThat(Sets.intersection(synonymTypes, definitionTypes)).isEmpty();

  }

  /**
   * Test top level of subsets.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTopLevelSubsetSearch() throws Exception {
    String url = baseUrl + "/ncit/subsets?include=properties";
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
    String url = baseUrl + "/ncit/subset/C167405";
    MvcResult result = null;
    Concept list = null;
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, Concept.class);
    assertThat(list != null && list.getChildren().size() > 0).isTrue();
    // check that no subsetLink (no valid download)
    assertThat(list.getSubsetLink() == null);

    // This value is specifically set in the unit test data via P374
    url = baseUrl + "/ncit/subset/C100110";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, Concept.class);
    // make sure there is a valid and correct subset link
    assertThat(list.getSubsetLink() != null && list.getSubsetLink().equals("CDISC/SDTM/"));

    url = baseUrl + "/ncit/subset/C116977";
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

  /**
   * Test terminology metadata calls.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTerminolgyMetadata() throws Exception {
    String url = baseUrl + "/terminologies";
    MvcResult result = null;
    List<Terminology> terminologies = null;
    log.info("Testing url - " + url);
    result =
        mvc.perform(get(url).param("terminology", "ncit")).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies != null && terminologies.size() == 2).isTrue();

    result =
        mvc.perform(get(url).param("terminology", "ncim")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies != null && terminologies.size() == 1).isTrue();

    result = mvc.perform(get(url).param("latest", "true")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(7);

    result = mvc.perform(get(url).param("latest", "false")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc.perform(get(url).param("tag", "monthly")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(1);

    result = mvc.perform(get(url).param("tag", "weekly")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(1);

    result = mvc.perform(get(url).param("terminology", "ncit").param("latest", "true"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(2);

    result = mvc.perform(get(url).param("terminology", "ncit").param("latest", "false"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualByComparingTo(0);

    result = mvc.perform(get(url).param("terminology", "ncit").param("tag", "monthly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(1);

    result = mvc.perform(get(url).param("terminology", "ncit").param("tag", "weekly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(1);

    result = mvc.perform(get(url).param("latest", "true").param("tag", "monthly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(1);

    result = mvc.perform(get(url).param("latest", "true").param("tag", "weekly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(1);

    result = mvc.perform(get(url).param("latest", "false").param("tag", "monthly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc.perform(get(url).param("latest", "false").param("tag", "weekly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc.perform(get(url).param("terminology", "ncim").param("latest", "true"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(1);

    result = mvc.perform(get(url).param("terminology", "ncim").param("latest", "false"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc.perform(get(url).param("terminology", "ncim").param("tag", "monthly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc.perform(get(url).param("terminology", "ncim").param("tag", "weekly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc
        .perform(
            get(url).param("terminology", "ncit").param("latest", "true").param("tag", "monthly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(1);

    result = mvc
        .perform(
            get(url).param("terminology", "ncit").param("latest", "true").param("tag", "weekly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(1);

    result = mvc
        .perform(
            get(url).param("terminology", "ncit").param("latest", "false").param("tag", "monthly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc
        .perform(
            get(url).param("terminology", "ncit").param("latest", "false").param("tag", "weekly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc
        .perform(
            get(url).param("terminology", "ncim").param("latest", "true").param("tag", "monthly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc
        .perform(
            get(url).param("terminology", "ncim").param("latest", "true").param("tag", "weekly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc
        .perform(
            get(url).param("terminology", "ncim").param("latest", "false").param("tag", "monthly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc
        .perform(
            get(url).param("terminology", "ncim").param("latest", "false").param("tag", "weekly"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(0);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologies = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
    });
    assertThat(terminologies).isNotNull();
    assertThat(terminologies.size()).isEqualTo(7);

  }

  /**
   * Test terminology versions
   *
   * @throws Exception the exception
   */
  @Test
  public void testTerminologyVersion() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    url = "/api/v1/metadata/";
    result =
        mvc.perform(get(url + "terminologies").param("terminology", "ncit").param("tag", "weekly"))
            .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    Terminology terminology =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
        }).get(0);
    String weeklyTerm = terminology.getTerminologyVersion();
    String baseWeeklyUrl = url + weeklyTerm;

    result = mvc.perform(get(baseWeeklyUrl + "/associations").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    List<Concept> metadataResults =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(metadataResults.get(0).getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/definitionType/P325").param("include", "summary"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    Concept metadataConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(metadataConcept.getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/definitionTypes").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataResults = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(metadataResults.get(0).getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/properties").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataResults = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(metadataResults.get(0).getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/property/P216").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(metadataConcept.getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/qualifier/P390").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(metadataConcept.getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/qualifiers").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataResults = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(metadataResults.get(0).getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/qualifier/P390").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(metadataConcept.getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/role/R123").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(metadataConcept.getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/roles").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataResults = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(metadataResults.get(0).getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/subset/C167405").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(metadataConcept.getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/subsets").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataResults = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(metadataResults.get(0).getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/synonymType/P90").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(metadataConcept.getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/synonymTypes").param("include", "minimal"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    metadataResults = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(metadataResults.get(0).getVersion() == terminology.getVersion());

  }

  /**
   * Test mappings
   *
   * @throws Exception the exception
   */
  @Test
  public void testMappings() throws Exception {
    MvcResult result = null;
    String content = null;
    String url = "/api/v1/metadata/";
    result = mvc.perform(get(url + "mapsets")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    List<Concept> metadataMappings =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });

    // test /mapsets
    assertThat(metadataMappings.size() > 0);
    assertThat(metadataMappings.stream().map(Concept::getName)
        .noneMatch(name -> name == null || name.isEmpty()));
    assertThat(metadataMappings.stream().map(Concept::getCode)
        .noneMatch(code -> code == null || code.isEmpty()));
    assertThat(metadataMappings.stream().map(Concept::getProperties)
        .noneMatch(properties -> properties == null || properties.isEmpty()));

    // test mapset/{code} - downloadOnly = false
    result =
        mvc.perform(get(url + "mapset/GO_to_NCIt_Mapping")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    Concept singleMetadataMap = new ObjectMapper().readValue(content, Concept.class);
    assertThat(singleMetadataMap != null);
    assertThat(singleMetadataMap.getName() != null
        && singleMetadataMap.getName().equals("GO_to_NCIt_Mapping"));
    assertThat(singleMetadataMap.getCode() != null
        && singleMetadataMap.getCode().equals("GO_to_NCIt_Mapping"));
    assertThat(singleMetadataMap.getProperties().stream()
        .filter(property -> "type".equals("downloadOnly") && "false".equals(property.getValue()))
        .findFirst());
    assertThat(singleMetadataMap.getProperties().stream()
        .filter(property -> "type".equals("welcomeText") && property.getValue() != null)
        .findFirst());

    // test mapset/{code} - downloadOnly = true, mapsetLink available
    result = mvc.perform(get(url + "mapset/ICDO_TO_NCI_TOPOGRAPHY")).andExpect(status().isOk())
        .andReturn();
    content = result.getResponse().getContentAsString();
    singleMetadataMap = new ObjectMapper().readValue(content, Concept.class);
    assertThat(singleMetadataMap != null);
    assertThat(singleMetadataMap.getName() != null
        && singleMetadataMap.getName().equals("ICDO_TO_NCI_TOPOGRAPHY"));
    assertThat(singleMetadataMap.getCode() != null
        && singleMetadataMap.getCode().equals("ICDO_TO_NCI_TOPOGRAPHY"));
    assertThat(singleMetadataMap.getProperties().stream()
        .filter(property -> "type".equals("downloadOnly") && "true".equals(property.getValue()))
        .findFirst());
    assertThat(
        singleMetadataMap
            .getProperties().stream().filter(property -> "type".equals("mapsetLink")
                && property.getValue() != null && property.getValue().endsWith(".txt"))
            .findFirst());

    // test mapset/{code} - downloadOnly = true, mapsetLink = null
    result =
        mvc.perform(get(url + "mapset/NCIt_Maps_To_GDC")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    singleMetadataMap = new ObjectMapper().readValue(content, Concept.class);
    assertThat(singleMetadataMap != null);
    assertThat(singleMetadataMap.getName() != null
        && singleMetadataMap.getName().equals("NCIt_Maps_To_GDC"));
    assertThat(singleMetadataMap.getCode() != null
        && singleMetadataMap.getCode().equals("NCIt_Maps_To_GDC"));
    assertThat(singleMetadataMap.getProperties().stream()
        .filter(property -> "type".equals("downloadOnly") && "true".equals(property.getValue()))
        .findFirst());
    assertThat(singleMetadataMap.getProperties().stream()
        .filter(property -> "type".equals("mapsetLink") && property.getValue().equals(null))
        .findFirst());

    // test mapset/{code}/mappings
    result = mvc.perform(get(url + "mapset/GO_to_NCIt_Mapping/maps")).andExpect(status().isOk())
        .andReturn();
    content = result.getResponse().getContentAsString();
    MapResultList mapList = new ObjectMapper().readValue(content, MapResultList.class);
    assertThat(mapList.getTotal() > 0);
    assertThat(mapList.getTotal().equals(mapList.getMaps().size()));
    Map tenFromZero = mapList.getMaps().get(9);

    // testing fromRecord and pageSize
    result = mvc.perform(get(url + "mapset/GO_to_NCIt_Mapping/maps?fromRecord=9&pageSize=23"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    mapList = new ObjectMapper().readValue(content, MapResultList.class);
    assertThat(mapList.getTotal() > 0);
    assertThat(mapList.getTotal().equals(mapList.getMaps().size()));
    assertThat(mapList.getTotal().equals(23));
    Map tenFromTen = mapList.getMaps().get(0);
    assertThat(tenFromTen.equals(tenFromZero));

  }

}
