
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
 * The Class SearchControllerTests.
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

    // test NOCODES properly applying
    url = baseUrl + "/ncim/C0000985";
    log.info("Testing url - " + url + "?terminology=ncim&code=C0000985");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0000985");
    assertThat(concept.getName()).isEqualTo("Acetic Acids");
    assertThat(concept.getTerminology()).isEqualTo("ncim");
    assertThat(concept.getSynonyms().get(1).getCode()).isNull();

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
    url = baseUrl + "/ncim/C0030920";
    log.info("Testing url - " + url + "?terminology=ncim&code=C0030920");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0030920");
    assertThat(concept.getName()).isEqualTo("Peptic Ulcer");
    assertThat(concept.getDefinitions()).isNotNull();
    assertThat(concept.getDefinitions().size()).isEqualTo(8);
    assertThat(concept.getDefinitions().get(1).getDefinition())
        .isEqualTo("An ulcer of the gastrointestinal tract. [HPO:probinson]");
    assertThat(concept.getDefinitions().get(1).getSource()).isEqualTo("HPO");

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
    log.info("Testing url - " + url + "?terminology=ncim&code=C0000005");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0000005");
    assertThat(concept.getName()).isEqualTo("(131)I-Macroaggregated Albumin");
    assertThat(concept.getProperties().size()).isGreaterThan(1);
    assertThat(concept.getProperties().get(1).getType()).isEqualTo("Semantic_Type");
    assertThat(concept.getProperties().get(1).getValue()).isEqualTo("Pharmacologic Substance");

    // test random concept with property
    url = baseUrl + "/ncim/C0718043";
    log.info("Testing url - " + url + "?terminology=ncim&code=C0718043");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C0718043");
    assertThat(concept.getName()).isEqualTo("Sacrosidase");
    assertThat(concept.getProperties().size()).isGreaterThan(1);
    assertThat(concept.getProperties().get(1).getType()).isEqualTo("Semantic_Type");
    assertThat(concept.getProperties().get(1).getValue()).isEqualTo("Pharmacologic Substance");

    // test penultimate concept with property
    url = baseUrl + "/ncim/CL990131";
    log.info("Testing url - " + url + "?terminology=ncim&code=CL990131");
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("CL990131");
    assertThat(concept.getName()).isEqualTo("HUGO Gene Nomenclature Committee, 2019_03");
    assertThat(concept.getProperties().size()).isGreaterThan(0);
    assertThat(concept.getProperties().get(0).getType()).isEqualTo("Semantic_Type");
    assertThat(concept.getProperties().get(0).getValue()).isEqualTo("Intellectual Product");

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
    assertThat(concept.getProperties().size()).isEqualTo(1);
    assertThat(concept.getProperties().get(0).getType()).isEqualTo("Semantic_Type");
    assertThat(concept.getProperties().get(0).getValue()).isEqualTo("Intellectual Product");

  }

}
