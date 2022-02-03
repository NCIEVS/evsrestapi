
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
 * Integration tests for NCIm loader.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class NCIMControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(NCIMControllerTests.class);

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
   * NCIM terminology basic tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNCIMTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // test if ncim term exists
    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("ncim")).count())
        .isEqualTo(1);
    final Terminology ncim =
        terminologies.stream().filter(t -> t.getTerminology().equals("ncim")).findFirst().get();
    assertThat(ncim.getTerminology()).isEqualTo("ncim");
    assertThat(ncim.getName()).isEqualTo("NCIMTH");
    assertThat(ncim.getDescription())
        .isEqualTo("NCI Metathesaurus. Bethesda, MD: National Cancer Institute.");
    assertThat(ncim.getLatest()).isTrue();
    assertThat(ncim.getMetadata().getUiLabel()).isEqualTo("NCI Metathesaurus");
    assertThat(ncim.getMetadata().getLoader()).isEqualTo("rrf");
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

    // first concept in MRCONSO
    url = baseUrl + "/ncim/C0000005";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0000005");
    assertThat(concept.getName()).isEqualTo("(131)I-Macroaggregated Albumin");
    assertThat(concept.getTerminology()).isEqualTo("ncim");

    // last concept in MRCONSO
    url = baseUrl + "/ncim/CL990362";
    log.info("Testing url - " + url + "?terminology=ncim&code=CL990362");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CL990362");
    assertThat(concept.getName()).isEqualTo("Foundational Model of Anatomy Ontology, 4_15");
    assertThat(concept.getTerminology()).isEqualTo("ncim");

    // test concept with only NOCODE atoms
    url = baseUrl + "/ncim/C0738563";
    log.info("Testing url - " + url + "?terminology=ncim&code=C0738563");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0738563");
    assertThat(concept.getName()).isEqualTo("Entire subregion of abdomen");
    assertThat(concept.getTerminology()).isEqualTo("ncim");
    assertThat(concept.getSynonyms().get(0).getCode()).isNull();

  }

  /**
   * MRDEF basic tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMRDEF() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // first concept in MRCONSO, no definitions
    url = baseUrl + "/ncim/C0000005";
    log.info("Testing url - " + url + "?terminology=ncim&code=C0000005");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0000005");
    assertThat(concept.getName()).isEqualTo("(131)I-Macroaggregated Albumin");
    assertThat(concept.getDefinitions()).isEmpty();

    // last concept in MRCONSO with definition
    url = baseUrl + "/ncim/CL977629";
    log.info("Testing url - " + url + "?terminology=ncim&code=CL977629");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CL977629");
    assertThat(concept.getName()).isEqualTo("Concurrent Disease Type");
    assertThat(concept.getDefinitions()).isNotNull();
    assertThat(concept.getDefinitions().size()).isEqualTo(1);
    assertThat(concept.getDefinitions().get(0).getDefinition())
        .isEqualTo("The classification and naming of comorbid conditions.");
    assertThat(concept.getDefinitions().get(0).getSource()).isEqualTo("NCI");

    // last concept in MRCONSO, no definitions
    url = baseUrl + "/ncim/CL990362";
    log.info("Testing url - " + url + "?terminology=ncim&code=CL990362");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CL990362");
    assertThat(concept.getName()).isEqualTo("Foundational Model of Anatomy Ontology, 4_15");
    assertThat(concept.getDefinitions()).isEmpty();

    // test random concept with definition
    url = baseUrl + "/ncim/C0030274";
    log.info("Testing url - " + url + "?terminology=ncim&code=C0030274");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0030274");
    assertThat(concept.getName()).isEqualTo("Pancreas");
    assertThat(concept.getDefinitions()).isNotNull();
    assertThat(concept.getDefinitions().size()).isEqualTo(8);
    assertThat(concept.getDefinitions().get(0).getDefinition()).startsWith(
        "A digestive organ in the abdomen that has both endocrine and exocrine functions.");
    assertThat(concept.getDefinitions().get(1).getSource()).isEqualTo("NCI-GLOSS");

    // test random concept with no definition
    url = baseUrl + "/ncim/C0426679";
    log.info("Testing url - " + url + "?terminology=ncim&code=C0426679");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0426679");
    assertThat(concept.getName()).isEqualTo("Puddle sign");
    assertThat(concept.getDefinitions()).isEmpty();

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

    // first concept in MRCONSO, three properties
    url = baseUrl + "/ncim/C0000005";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0000005");
    assertThat(concept.getName()).isEqualTo("(131)I-Macroaggregated Albumin");
    assertThat(concept.getProperties().size()).isGreaterThan(1);
    assertThat(concept.getProperties().get(1).getType()).isEqualTo("Semantic_Type");
    assertThat(concept.getProperties().get(1).getValue())
        .isEqualTo("Amino Acid, Peptide, or Protein");

    // test random concept with property
    url = baseUrl + "/ncim/C0718043";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0718043");
    assertThat(concept.getName()).isEqualTo("Sacrosidase");
    assertThat(concept.getProperties().size()).isGreaterThan(1);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type")).count())
            .isGreaterThan(0);
    assertThat(concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type"))
        .findFirst().get().getValue()).isEqualTo("Amino Acid, Peptide, or Protein");

    // test penultimate concept with property
    url = baseUrl + "/ncim/CL988042";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CL988042");
    assertThat(concept.getName()).isEqualTo(
        "Guidance for drainage+placement of drainage catheter^WO contrast:Find:Pt:Abdomen:Doc:CT");
    assertThat(concept.getProperties().size()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type")).count())
            .isGreaterThan(0);
    assertThat(concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type"))
        .findFirst().get().getValue()).isEqualTo("Clinical Attribute");


    // last concept in MRCONSO with one property
    url = baseUrl + "/ncim/CL990362";
    log.info("Testing url - " + url + "?terminology=ncim&code=CL990362");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CL990362");
    assertThat(concept.getName()).isEqualTo("Foundational Model of Anatomy Ontology, 4_15");
    assertThat(concept.getProperties().size()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type")).count())
            .isGreaterThan(0);
    assertThat(concept.getProperties().stream().filter(p -> p.getType().equals("Semantic_Type"))
        .findFirst().get().getValue()).isEqualTo("Intellectual Product");

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
    url = baseUrl + "/ncim/C0000005?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0000005");
    assertThat(concept.getAssociations().size()).isEqualTo(1);
    assertThat(concept.getAssociations().get(0).getType()).isEqualTo("RN");
    assertThat(concept.getAssociations().get(0).getRelatedCode()).isEqualTo("C0036775");
    assertThat(concept.getInverseAssociations().size()).isEqualTo(1);
    assertThat(concept.getInverseAssociations().get(0).getType()).isEqualTo("RB");
    assertThat(concept.getInverseAssociations().get(0).getRelatedCode()).isEqualTo("C0036775");

    // Check parents/children
    url = baseUrl + "/ncim/C0242354?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0242354");
    assertThat(concept.getChildren().size()).isEqualTo(4);
    assertThat(concept.getChildren().stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .contains("CL979355");
    assertThat(concept.getParents().size()).isEqualTo(2);
    assertThat(concept.getParents().stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .contains("C0012634");

  }

  /**
   * Test MRREL 2.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMRREL2() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // MRREL entry with parents and children
    url = baseUrl + "/ncim/CL979355?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CL979355");
    assertThat(concept.getParents().size()).isGreaterThan(0);
    // Verify that parents are "isa" and not "inverse isa"
    assertThat(concept.getParents().stream()
        .filter(
            r -> !r.getQualifiers().isEmpty() && r.getQualifiers().get(0).getValue().equals("isa"))
        .count() > 0);
    // Verify that children are "inverse isa" and not "isa"
    assertThat(concept.getChildren().size()).isGreaterThan(0);
    assertThat(concept.getChildren().stream().filter(r -> !r.getQualifiers().isEmpty()
        && r.getQualifiers().get(0).getValue().equals("inverse_isa")).count() > 0);

    // Read something with associations and inverse associations
    url = baseUrl + "/ncim/C0000726?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0000726");

    // C0000726|A13537807|AUI|RO|CL565855|A15706523|AUI|analyzes|R123761621||LNC|LNC|||N||
    // CL565855|A15706523|AUI|RO|C0000726|A13537807|AUI|analyzed_by|R123761622||LNC|LNC|||N||
    assertThat(concept.getAssociations().size()).isGreaterThan(0);
    assertThat(concept.getAssociations().stream().filter(r -> !r.getQualifiers().isEmpty()
        && r.getQualifiers().get(0).getValue().equals("analyzed_by")).count() > 0);
    assertThat(concept.getInverseAssociations().stream().filter(
        r -> !r.getQualifiers().isEmpty() && r.getQualifiers().get(0).getValue().equals("analyzes"))
        .count() > 0);

    assertThat(concept.getInverseAssociations().size()).isGreaterThan(0);

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

    // first concept in MRSAT
    url = baseUrl + "/ncim/C0000052";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0000052");
    assertThat(concept.getName()).isEqualTo("1,4-alpha-Glucan Branching Enzyme");
    assertThat(concept.getProperties().size()).isGreaterThan(1);
    assertThat(concept.getProperties().stream()
        .filter(p -> p.getType().equals("DEFINITION_STATUS_ID")
            && p.getSource().equals("SNOMEDCT_US") && p.getValue().equals("900000000000074008"))
        .count()).isEqualTo(1);

    // random concept in MRSAT
    url = baseUrl + "/ncim/C0436993";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0436993");
    assertThat(concept.getName()).isEqualTo("On examination - abdominal mass - regular shape");
    assertThat(concept.getProperties().size()).isGreaterThan(1);
    assertThat(concept.getProperties().stream()
        .filter(p -> p.getType().equals("DEFINITION_STATUS_ID")
            && p.getSource().equals("SNOMEDCT_US") && p.getValue().equals("900000000000074008"))
        .count()).isEqualTo(1);

    // last concept in MRSTY/MRSAT
    // TODO: we're ignoring AUI attributes so for the moment this turns up
    // nothing
    // url = baseUrl + "/ncim/CL988043";
    // log.info("Testing url - " + url + "?terminology=ncim&code=CL988043");
    // result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    // content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    // concept = new ObjectMapper().readValue(content, Concept.class);
    // assertThat(concept).isNotNull();
    // assertThat(concept.getCode()).isEqualTo("CL988043");
    // assertThat(concept.getName()).isEqualTo(
    // "Guidance for drainage+placement of drainage catheter^W contrast
    // IV:Find:Pt:Abdomen:Doc:CT");
    // assertThat(concept.getProperties().size()).isGreaterThan(0);
    // assertThat(
    // concept.getProperties().stream()
    // .filter(p -> p.getType().equals("IMAGING_DOCUMENT_VALUE_SET")
    // && p.getSource().equals("LNC") && p.getValue().equals("TRUE"))
    // .count() > 0).isTrue();

  }

  /**
   * Test ncim search contains.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNcimSearchContains() throws Exception {
    String url = "/api/v1/concept/search";
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Valid test
    log.info("Testing url - " + url
        + "?fromRecord=0&include=synonyms&pageSize=100&term=aspirin&type=contains");
    result = mvc
        .perform(get(url).param("terminology", "ncim").param("term", "aspirin")
            .param("pageSize", "100").param("type", "contains").param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();

    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list).isNotNull();
    // Look for the Aspirin CUI.
    assertThat(list.getConcepts().stream().filter(c -> c.getCode().equals("C0004057"))
        .collect(Collectors.toList()).size()).isEqualTo(1);

  }

  /**
   * Test ncim metadata.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNcimMetadata() throws Exception {

    String base = "/api/v1/metadata/ncim";
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
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("RO");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("RB");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("RN");
    // assertThat(list.stream().map(c ->
    // c.getCode()).collect(Collectors.toSet())).contains("RQ");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("SY");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("BRO");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("BRN");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet()))
        .doesNotContain("BRB");
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
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(10);
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("CDISC");

    // Handle definitionTypes
    url = base + "/definitionTypes";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("DEFINITION");

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
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("SCORE");

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
    // assertThat(list.stream().map(c ->
    // c.getCode()).collect(Collectors.toSet())).contains("RG");
    // assertThat(list.stream().map(c ->
    // c.getCode()).collect(Collectors.toSet())).contains("DIR");
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
    assertThat(list.size()).isGreaterThan(20);
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("MSH");

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
    assertThat(list.size()).isGreaterThan(30);
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("AB");
    assertThat(list.stream().map(c -> c.getCode()).collect(Collectors.toSet())).contains("PT");

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
    url = baseUrl + "/ncim/roots";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isEmpty();

    // test /descendants
    url = baseUrl + "/ncim/C0004057/descendants";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
      // n/a
    });
    assertThat(list).isEmpty();

    // test /subtree
    url = baseUrl + "/ncim/C0004057/subtree";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list2 = new ObjectMapper().readValue(content, new TypeReference<List<HierarchyNode>>() {
      // n/a
    });
    assertThat(list2).isEmpty();

    // test /subtree/children - C0242354
    url = baseUrl + "/ncim/C0242354/subtree/children";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list2 = new ObjectMapper().readValue(content, new TypeReference<List<HierarchyNode>>() {
      // n/a
    });
    assertThat(list2).isEmpty();

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

    // test /pathsToRoot
    url = baseUrl + "/ncim/C0242354/pathsToRoot";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<List<Concept>>>() {
      // n/a
    });
    assertThat(list).isEmpty();

    // test /pathsFromRoot
    url = baseUrl + "/ncim/C0242354/pathsFromRoot";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<List<Concept>>>() {
      // n/a
    });
    assertThat(list).isEmpty();

    // test /pathsToAncestor
    url = baseUrl + "/ncim/C0242354/pathsToAncestor/C0000005";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list = new ObjectMapper().readValue(content, new TypeReference<List<List<Concept>>>() {
      // n/a
    });
    assertThat(list).isEmpty();
  }

}
