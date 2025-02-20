package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.properties.TestProperties;
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
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** subset tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SubsetControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MetadataControllerTests.class);

  /** The mvc. */
  @Autowired private MockMvc mvc;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** The base url. */
  private String baseUrl = "";

  /** Sets the up. */
  @BeforeEach
  public void setUp() {

    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1/";
  }

  @Test
  /**
   * test get trailing slash 404
   *
   * @throws Exception
   */
  public void testGetTrailingSlashSubset() throws Exception {
    String url = baseUrl + "/subset/ncit/C116978/?include=minimal";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
  }

  /**
   * Test include subsets.
   *
   * @throws Exception the exception
   */
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
            concept.getProperties().stream()
                .filter(p -> p.getType().equals("Semantic_Type"))
                .count())
        .isGreaterThan(0);

    // /subset - properties
    url = baseUrl + "subset/ncit/C167405?include=properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
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
    assertThat(concept.getChildren().get(0).getProperties()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
    assertThat(
            concept.getProperties().stream()
                .filter(p -> p.getType().equals("Semantic_Type"))
                .count())
        .isGreaterThan(0);

    // /subset - synonyms,properties
    url = baseUrl + "subset/ncit/C167405?include=synonyms,properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
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
    assertThat(concept.getChildren().get(0).getSynonyms()).isEmpty();
    assertThat(concept.getChildren().get(0).getProperties()).isEmpty();
    assertThat(concept.getParents()).isEmpty();
    assertThat(concept.getAssociations()).isEmpty();
    assertThat(concept.getInverseAssociations()).isEmpty();
    assertThat(concept.getRoles()).isEmpty();
    assertThat(concept.getInverseRoles()).isEmpty();
    assertThat(concept.getMaps()).isEmpty();
    assertThat(
            concept.getProperties().stream()
                .filter(p -> p.getType().equals("Semantic_Type"))
                .count())
        .isGreaterThan(0);
  }

  /**
   * Test top level of subsets.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("null")
  @Test
  public void testTopLevelSubsetSearch() throws Exception {
    String url = baseUrl + "subset/ncit?include=properties";
    MvcResult result = null;
    List<Concept> list = null;
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list != null).isTrue();
    assertThat(list.size() > 0).isTrue();
    // No subsets or children should have Publish_Value_Set set to something other than "Yes".
    assertThat(
            list.stream()
                .flatMap(Concept::streamSelfAndChildren)
                .filter(
                    c ->
                        c.getProperties().stream()
                                .filter(
                                    p ->
                                        p.getType().equals("Publish_Value_Set")
                                            && !p.getValue().equals("Yes"))
                                .count()
                            > 0)
                .count())
        .isEqualTo(0);
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

  /**
   * Test subset members.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSubsetMembers() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;

    // C81224 - 27 members, no properties
    url = baseUrl + "subset/ncit/C81224/members";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isEqualTo(27);
    assertThat(list.stream().flatMap(c -> c.getProperties().stream()).count()).isEqualTo(0);

    // C81224 - 29 members, no properties
    url = baseUrl + "subset/ncit/C81224/members";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isEqualTo(27);
    assertThat(list.stream().flatMap(c -> c.getProperties().stream()).count()).isEqualTo(0);

    // C81222 - CDISC ADaM terminology
    // contains C81226
    // does not contain C82867
    url = baseUrl + "subset/ncit/C81222/members";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isEqualTo(12);
    assertThat(list.stream().flatMap(c -> c.getProperties().stream()).count()).isEqualTo(0);
    assertThat(list.stream().filter(c -> c.getCode().equals("C81226")).count()).isGreaterThan(0);
    assertThat(list.stream().filter(c -> c.getCode().equals("C82867")).count()).isEqualTo(0);
    // All ADAM members start with "CDISC ..."
    assertThat(list.stream().filter(c -> !c.getName().startsWith("CDISC")).count()).isEqualTo(0);

    url = baseUrl + "subset/ncit/C81222/members?fromRecord=0&pageSize=5";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isEqualTo(5);
    // All ADAM members start with "CDISC ..."
    assertThat(list.stream().filter(c -> !c.getName().startsWith("CDISC")).count()).isEqualTo(0);

    //    C61410 - Clinical Data Interchange Standards Consortium Terminology
    // Has many fewer entries than 30k (which is what is in prod as of 202412)
    url = baseUrl + "subset/ncit/C61410/members";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isGreaterThan(0);
  }

  /**
   * Test subset via concept controller.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSubsetMembersViaConceptController() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;

    // C81224 - 27 members, no properties
    url = baseUrl + "concept/ncit/subsetMembers/C81224";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isEqualTo(27);
    assertThat(list.stream().flatMap(c -> c.getProperties().stream()).count()).isEqualTo(0);

    // C81224 - 29 members, no properties
    url = baseUrl + "concept/ncit/subsetMembers/C81224";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isEqualTo(27);
    assertThat(list.stream().flatMap(c -> c.getProperties().stream()).count()).isEqualTo(0);

    // C81222 - CDISC ADaM terminology
    // contains C81226
    // does not contain C82867
    url = baseUrl + "concept/ncit/subsetMembers/C81222";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isEqualTo(12);
    assertThat(list.stream().flatMap(c -> c.getProperties().stream()).count()).isEqualTo(0);
    assertThat(list.stream().filter(c -> c.getCode().equals("C81226")).count()).isGreaterThan(0);
    assertThat(list.stream().filter(c -> c.getCode().equals("C82867")).count()).isEqualTo(0);
    // All ADAM members start with "CDISC ..."
    assertThat(list.stream().filter(c -> !c.getName().startsWith("CDISC")).count()).isEqualTo(0);

    url = baseUrl + "concept/ncit/subsetMembers/C81222?fromRecord=0&pageSize=5";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isEqualTo(5);
    // All ADAM members start with "CDISC ..."
    assertThat(list.stream().filter(c -> !c.getName().startsWith("CDISC")).count()).isEqualTo(0);

    //    C61410 - Clinical Data Interchange Standards Consortium Terminology
    // Has many fewer entries than 30k (which is what is in prod as of 202412)
    url = baseUrl + "concept/ncit/subsetMembers/C61410";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    assertThat(content).isNotNull();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list.size()).isGreaterThan(0);
  }

  /**
   * Test Pediatric Subsets.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPediatricSubsets() throws Exception {

    // C143048 - NCIt subset, ancestor of pediatric subsets
    String url = baseUrl + "subset/ncit/C143048";
    MvcResult result = null;
    Concept subsetConcept = null;
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    subsetConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(subsetConcept).isNotNull();
    assertThat(subsetConcept.getCode()).isEqualTo("C143048");
    assertThat(subsetConcept.getChildren()).isNotEmpty();

    Concept childhoodNeoplasmSubset =
        subsetConcept.getChildren().stream()
            .filter(c -> c.getCode().equals("C6283"))
            .findFirst()
            .orElse(null);
    assertThat(childhoodNeoplasmSubset).isNotNull();
    assertThat(childhoodNeoplasmSubset.getName()).isEqualTo("Childhood Neoplasm");
    assertThat(childhoodNeoplasmSubset.getChildren()).isNotEmpty();

    url = baseUrl + "concept/ncit/C6283?include=inverseAssociations";
    subsetConcept = null;
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    subsetConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(subsetConcept).isNotNull();
    assertThat(subsetConcept.getCode()).isEqualTo("C6283");
    assertThat(subsetConcept.getName()).isEqualTo("Childhood Neoplasm");
    assertThat(subsetConcept.getInverseAssociations()).isNotEmpty();
    assertThat(
            subsetConcept.getInverseAssociations().stream()
                .allMatch(assoc -> assoc.getQualifiers().size() > 0))
        .isTrue();

    url = baseUrl + "subset/ncit/C6283?include=properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    subsetConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(subsetConcept).isNotNull();
    assertThat(subsetConcept.getProperties()).isNotEmpty();
    assertThat(
            subsetConcept.getProperties().stream()
                .filter(p -> p.getType().equals("Publish_Value_Set") && p.getValue().equals("Yes"))
                .count())
        .isGreaterThan(0);
    assertThat(
            subsetConcept.getProperties().stream()
                .filter(
                    p ->
                        p.getType().equals("EVSRESTAPI_Subset_Format")
                            && p.getValue().equals("NCI"))
                .count())
        .isGreaterThan(0);

    Concept childhoodMalignantNeoplasmSubset =
        childhoodNeoplasmSubset.getChildren().stream()
            .filter(c -> c.getCode().equals("C4005"))
            .findFirst()
            .orElse(null);
    assertThat(childhoodMalignantNeoplasmSubset).isNotNull();
    assertThat(childhoodMalignantNeoplasmSubset.getName())
        .isEqualTo("Childhood Malignant Neoplasm");

    url = baseUrl + "concept/ncit/C4005?include=inverseAssociations";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    subsetConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(subsetConcept).isNotNull();
    assertThat(subsetConcept.getCode()).isEqualTo("C4005");
    assertThat(subsetConcept.getName()).isEqualTo("Childhood Malignant Neoplasm");
    assertThat(subsetConcept.getInverseAssociations()).isNotEmpty();
    assertThat(
            subsetConcept.getInverseAssociations().stream()
                .allMatch(assoc -> assoc.getQualifiers().size() > 0))
        .isTrue();

    url = baseUrl + "subset/ncit/C4005?include=properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    subsetConcept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(subsetConcept).isNotNull();
    assertThat(subsetConcept.getProperties()).isNotEmpty();
    assertThat(
            subsetConcept.getProperties().stream()
                .filter(p -> p.getType().equals("Publish_Value_Set") && p.getValue().equals("Yes"))
                .count())
        .isGreaterThan(0);
    assertThat(
            subsetConcept.getProperties().stream()
                .filter(
                    p ->
                        p.getType().equals("EVSRESTAPI_Subset_Format")
                            && p.getValue().equals("NCI"))
                .count())
        .isGreaterThan(0);
  }
}
