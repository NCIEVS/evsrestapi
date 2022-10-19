
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * Integration tests for MDR loader.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MDRControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MDRControllerTests.class);

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
    /*
     * Configure the JacksonTester object
     */
    this.objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1/concept";
    // baseUrlMetadata = "/api/v1/metadata";
  }

  /**
   * MDR terminology basic tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMDRTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // test if mdr term exists
    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("mdr")).count())
        .isEqualTo(1);
    final Terminology mdr =
        terminologies.stream().filter(t -> t.getTerminology().equals("mdr")).findFirst().get();
    assertThat(mdr.getTerminology()).isEqualTo("mdr");
    assertThat(mdr.getMetadata().getUiLabel()).isEqualTo("MedDRA");
    assertThat(mdr.getName()).isEqualTo("MedDRA 23_1");
    assertThat(mdr.getDescription()).isNotEmpty();
    assertThat(mdr.getMetadata().getLoader()).isEqualTo("rrf");
    assertThat(mdr.getMetadata().getSourceCt()).isEqualTo(1);
    assertThat(mdr.getMetadata().getLicenseText()).isNotNull();
    assertThat(mdr.getName())
        .isEqualTo("Medical Dictionary for Regulatory Activities Terminology (MedDRA), 23_1");
    assertThat(mdr.getDescription()).isEqualTo(";;MedDRA MSSO;;MedDRA [electronic resource]"
        + " : Medical Dictionary for Regulatory Activities Terminology;;;"
        + "Version 23.1;;MedDRA MSSO;;September, 2020;;;;MedDRA "
        + "[electronic resource] : Medical Dictionary for Regulatory Activities Terminology");
    ;
    assertThat(mdr.getLatest()).isTrue();
  }

  /**
   * MRCONSO basic tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMRCONSO() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Random MDR code
    url = baseUrl + "/mdr/10009802";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("10009802");
    assertThat(concept.getName()).isEqualTo("Coagulopathy");
    assertThat(concept.getTerminology()).isEqualTo("mdr");
    assertThat(concept.getSynonyms().size()).isEqualTo(2);
  }

  /**
   * MRDEF basic tests. N/A - no MDR definitions
   *
   * @throws Exception the exception
   */
  // @Test
  public void testMRDEF() throws Exception {
    // n/a
  }

  /**
   * MRDEF basic tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMRSTY() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Random MDR code
    url = baseUrl + "/mdr/10009802";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getProperties().size()).isGreaterThan(1);
    assertThat(concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type"))
        .findFirst().orElse(null)).isNotNull();
    assertThat(concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type"))
        .findFirst().get().getValue()).isEqualTo("Disease or Syndrome");

  }

  /**
   * Test MRREL.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMRREL() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Check associations
    url = baseUrl + "/mdr/10009802?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("10009802");
    assertThat(concept.getAssociations().size()).isEqualTo(8);
    assertThat(concept.getAssociations().stream().filter(a -> a.getRelatedCode().equals("10013206"))
        .findFirst().orElse(null)).isNotNull();
    assertThat(concept.getAssociations().stream().filter(a -> a.getRelatedCode().equals("10013206"))
        .findFirst().get().getType()).isEqualTo("RQ");

    assertThat(concept.getInverseAssociations().size()).isEqualTo(8);
    assertThat(concept.getInverseAssociations().stream()
        .filter(a -> a.getRelatedCode().equals("10064732")).findFirst().orElse(null)).isNotNull();
    assertThat(
        concept.getInverseAssociations().stream().filter(a -> a.getRelatedCode().equals("10064732"))
            .findFirst().get().getQualifiers().get(0).getValue().equals("classified_as"));

    assertThat(concept.getChildren().size()).isEqualTo(8);
    assertThat(concept.getChildren().get(0).getTerminology()).isNull();
    assertThat(concept.getChildren().get(0).getVersion()).isNull();
    assertThat(concept.getChildren().get(0).getLeaf()).isNotNull();
    assertThat(concept.getChildren().stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .contains("10048498");

    assertThat(concept.getParents().size()).isEqualTo(1);
    assertThat(concept.getParents().get(0).getTerminology()).isNull();
    assertThat(concept.getParents().get(0).getVersion()).isNull();
    assertThat(concept.getParents().get(0).getLeaf()).isNull();
    assertThat(concept.getParents().stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .contains("10053567");

  }

  /**
   * MRDEF basic tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMRSAT() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // random concept in MRSAT
    url = baseUrl + "/mdr/10009802";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("10009802");
    assertThat(concept.getProperties().size()).isGreaterThan(1);
    assertThat(concept.getProperties().stream()
        .filter(p -> p.getType().equals("PRIMARY_SOC") && p.getValue().equals("10005329")).count())
            .isEqualTo(1);

    // SMQ concept in MRSAT - 10036030
    url = baseUrl + "/mdr/10036030?include=associations,inverseAssociations";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("10036030");
    assertThat(concept.getAssociations().size()).isGreaterThan(0);
    assertThat(concept.getAssociations().stream().filter(a -> a.getQualifiers().size() > 1).count())
        .isEqualTo(1);
    assertThat(concept.getAssociations().stream().filter(a -> a.getQualifiers().size() > 1)
        .flatMap(a -> a.getQualifiers().stream()).filter(q -> q.getType().equals("SMQ_TERM_CAT"))
        .count()).isEqualTo(1);

    assertThat(concept.getInverseAssociations().size()).isGreaterThan(0);
    assertThat(
        concept.getInverseAssociations().stream().filter(a -> a.getQualifiers().size() > 1).count())
            .isEqualTo(1);
    assertThat(concept.getInverseAssociations().stream().filter(a -> a.getQualifiers().size() > 1)
        .flatMap(a -> a.getQualifiers().stream()).filter(q -> q.getType().equals("SMQ_TERM_CAT"))
        .count()).isEqualTo(1);

  }

  /**
   * Test mdr search contains.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMdrSearchContains() throws Exception {
    String url = "/api/v1/concept/search";
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Valid test
    log.info("Testing url - " + url
        + "?terminology=mdr&fromRecord=0&include=synonyms&pageSize=100&term=Coagulation&type=contains");
    result = mvc
        .perform(get(url).param("terminology", "mdr").param("term", "Coagulation")
            .param("pageSize", "100").param("type", "contains").param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();

    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list).isNotNull();
    // Look for the Aspirin CUI.
    assertThat(list.getConcepts().stream().filter(c -> c.getCode().equals("10009729"))
        .collect(Collectors.toList()).size()).isEqualTo(1);

    // Test searching "Hepatitis, non-infectious (SMQ)"
    log.info("Testing url - " + url
        + "?terminology=mdr&fromRecord=0&include=synonyms&pageSize=100&term=Hepatitis, non-infectious (SMQ)&type=contains");
    result = mvc
        .perform(get(url).param("terminology", "mdr").param("term", "Hepatitis, non-infectious (SMQ)")
            .param("pageSize", "100").param("type", "contains").param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();
    // Just checking that searching with parentheses doesn't cause an error

    // Test searching "Hepatitis, non-infectious (SMQ)"
    log.info("Testing url - " + url
        + "?terminology=mdr&fromRecord=0&include=synonyms&pageSize=100&term=Hepatitis, non-infectious (SMQ)&type=match");
    result = mvc
        .perform(get(url).param("terminology", "mdr").param("term", "Hepatitis, non-infectious (SMQ)")
            .param("pageSize", "100").param("type", "match").param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();
    // Just checking that searching with parentheses doesn't cause an error
}

  /**
   * Test mdr metadata.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMdrMetadata() throws Exception {

    String base = "/api/v1/metadata/mdr";
    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;

    // Handle associations
    url = base + "/associations";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isNotNull();
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("RQ");
    // assertThat(list.stream().map(c ->
    // c.getCode()).collect(Collectors.toSet())).contains("RB");
    // assertThat(list.stream().map(c ->
    // c.getCode()).collect(Collectors.toSet())).contains("RN");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("SY");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("BRO");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("BRB");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("BRN");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("XR");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("AQ");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("QB");

    // Handle concept statuses
    url = base + "/conceptStatuses";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isEmpty();

    // Handle definitionSources
    url = base + "/definitionSources";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isEmpty();

    // Handle definitionTypes
    url = base + "/definitionTypes";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list.size()).isEqualTo(0);
    // No definitions in MDR
    // assertThat(list.get(0).getCode()).isEqualTo("DEFINITION");

    // Handle properties
    url = base + "/properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .contains("Semantic_Type");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .contains("PT_IN_VERSION");

    // Handle qualifiers
    url = base + "/qualifiers";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    // NOTE: some quaifiers are not actually used
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("AUI1");
    // assertThat(list.stream().map(c ->
    // c.getCode()).collect(Collectors.toSet())).contains("STYPE1");
    // assertThat(list.stream().map(c ->
    // c.getCode()).collect(Collectors.toSet())).contains("AUI2");
    // assertThat(list.stream().map(c ->
    // c.getCode()).collect(Collectors.toSet())).contains("STYPE2");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("RELA");

    // Faked example of SMQ qualifier
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .contains("SMQ_TERM_CAT");

    // assertThat(list.stream().map(c ->
    // c.getCode()).collect(Collectors.toSet()))
    // .contains("SUPPRESS");

    // Handle subsets - n/a
    url = base + "/subsets";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isEmpty();

    // Handle synonymSources - n/a - handled inline
    url = base + "/synonymSources";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("MDR");

    // Handle synonymTypes
    url = base + "/synonymTypes";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(0).getCode()).isEqualTo("Preferred_Name");
    assertThat(list.get(1).getCode()).isEqualTo("Synonym");

    // Handle termTypes - n/a - handled inline
    url = base + "/termTypes";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(10);
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("LLT");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("PT");

  }

  /**
   * Test qualifier values.
   *
   * @throws Exception the exception
   */
  @Test
  public void testQualifierValues() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<String> list = null;

    // NCIM qualifier values
    url = "/api/v1/metadata/mdr/qualifier/SMQ_TERM_CAT/values";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<String>>() {
      // n/a
    });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);

  }
  /**
   * Test subree.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSubree() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;
    List<HierarchyNode> list2 = null;

    // test /roots
    url = baseUrl + "/mdr/roots";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    // Some things show up as roots because of nature of the data
    assertThat(list.size()).isGreaterThan(0);

    // test /descendants
    url = baseUrl + "/mdr/10053567/descendants";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();

    // test /subtree
    url = baseUrl + "/mdr/10053567/subtree";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list2 = new ObjectMapper().readValue(content, new TypeReference<List<HierarchyNode>>() {
      // n/a
    });
    assertThat(list2).isNotEmpty();

    // test /subtree/children
    url = baseUrl + "/mdr/10053567/subtree/children";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list2 = new ObjectMapper().readValue(content, new TypeReference<List<HierarchyNode>>() {
      // n/a
    });
    assertThat(list2).isNotEmpty();

  }

  /**
   * Test paths.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPaths() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    List<List<Concept>> list = null;
    List<Concept> roots = null;
    List<Concept> concepts = null;

    // test /roots
    url = baseUrl + "/mdr/roots";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    roots = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });

    // test /pathsToRoot
    url = baseUrl + "/mdr/10009727/pathsToRoot";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<List<Concept>>>() {
      // n/a
    });
    assertThat(list.size()).isGreaterThan(0);
    concepts = list.get(0);
    assertThat(concepts.size()).isGreaterThan(1);
    assertThat(concepts.get(0).getLevel()).isEqualTo(0);
    assertThat(concepts.get(0).getCode()).isEqualTo("10009727");
    assertThat(concepts.get(concepts.size() - 1).getLevel()).isEqualTo(concepts.size() - 1);
    final String rootCode = concepts.get(concepts.size() - 1).getCode();
    assertThat(roots.stream().filter(c -> c.getCode().equals(rootCode)).count()).isGreaterThan(0);

    // test /pathsFromRoot
    url = baseUrl + "/mdr/10009727/pathsFromRoot";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<List<Concept>>>() {
      // n/a
    });
    assertThat(list.size()).isGreaterThan(0);
    concepts = list.get(0);
    assertThat(concepts.size()).isGreaterThan(1);
    assertThat(concepts.get(0).getLevel()).isEqualTo(0);
    assertThat(concepts.get(concepts.size() - 1).getCode()).isEqualTo("10009727");
    assertThat(concepts.get(concepts.size() - 1).getLevel()).isEqualTo(concepts.size() - 1);
    assertThat(roots.stream().filter(c -> c.getCode().equals(rootCode)).count()).isGreaterThan(0);

    // test /pathsToAncestor
    url = baseUrl + "/mdr/10009727/pathsToAncestor/10009802";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<List<Concept>>>() {
      // n/a
    });
    assertThat(list.size()).isGreaterThan(0);
    concepts = list.get(0);
    assertThat(concepts.size()).isEqualTo(2);
    assertThat(concepts.get(0).getCode()).isEqualTo("10009727");
    assertThat(concepts.get(0).getLevel()).isEqualTo(0);
    assertThat(concepts.get(1).getCode()).isEqualTo("10009802");
    assertThat(concepts.get(1).getLevel()).isEqualTo(1);

    // test /pathsToAncestor - all the way up
    url = baseUrl + "/mdr/10009727/pathsToAncestor/10005329";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<List<Concept>>>() {
      // n/a
    });
    assertThat(list.size()).isGreaterThan(0);
    concepts = list.get(0);
    assertThat(concepts.size()).isGreaterThan(2);
    assertThat(concepts.get(0).getCode()).isEqualTo("10009727");
    assertThat(concepts.get(0).getLevel()).isEqualTo(0);
    assertThat(concepts.get(concepts.size() - 1).getCode()).isEqualTo("10005329");
    assertThat(concepts.get(concepts.size() - 1).getLevel()).isEqualTo(concepts.size() - 1);

  }
}
