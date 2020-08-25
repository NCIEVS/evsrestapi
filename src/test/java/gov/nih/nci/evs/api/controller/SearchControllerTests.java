
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

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * The Class SearchControllerTests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SearchControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(SearchControllerTests.class);

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

    baseUrl = "/api/v1/concept/search";
  }

  /**
   * Returns the search simple.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchTerm() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma");

    // Test a basic term search
    result = this.mvc.perform(get(url).param("terminology", "ncit").param("term", "melanoma"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    ConceptResultList list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(10);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getTerminology()).isEqualTo("ncit");
    assertThat(list.getConcepts().get(0).getSynonyms()).isEmpty();
    assertThat(list.getTotal()).isGreaterThan(100);
    assertThat(list.getParameters().getTerm()).isEqualTo("melanoma");
    assertThat(list.getParameters().getType()).isEqualTo("contains");
    assertThat(list.getParameters().getFromRecord()).isEqualTo(0);
    assertThat(list.getParameters().getPageSize()).isEqualTo(10);
    assertThat(list.getTimeTaken()).isNotNull();
    assertThat(list.getTimeTaken().longValue()).isGreaterThan(0);

    // Test a basic term search with single terminology form
    url = "/api/v1/concept/ncit/search";
    log.info("Testing url - " + url + "?term=melanoma");
    result =
        this.mvc.perform(get(url).param("term", "melanoma")).andExpect(status().isOk()).andReturn();
    String content2 = result.getResponse().getContentAsString();

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualTo(content2);

  }

  /**
   * Test highlight.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHighlight() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&include=highlights");

    // Test a basic term search
    result = this.mvc.perform(get(url).param("terminology", "ncit").param("term", "melanoma")
        .param("include", "highlights")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    ConceptResultList list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(10);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getHighlight()).isEqualTo("<em>Melanoma</em>");
    assertThat(list.getTotal()).isGreaterThan(100);
    assertThat(list.getParameters().getTerm()).isEqualTo("melanoma");
    assertThat(list.getParameters().getType()).isEqualTo("contains");
    assertThat(list.getParameters().getFromRecord()).isEqualTo(0);
    assertThat(list.getParameters().getPageSize()).isEqualTo(10);

    // Test a basic term search with single terminology form
    url = "/api/v1/concept/ncit/search";
    log.info("Testing url - " + url + "?term=melanoma&include=highlights");
    result = this.mvc.perform(get(url).param("term", "melanoma").param("include", "highlights"))
        .andExpect(status().isOk()).andReturn();
    String content2 = result.getResponse().getContentAsString();

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualTo(content2);

  }

  /**
   * Test bad search.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBadSearch() throws Exception {
    String url = null;

    // Bad terminology
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncitXXX&term=melanoma");
    mvc.perform(get(url).param("terminology", "ncitXXX").param("term", "melanoma"))
        .andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Missing "term" parameter
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit");
    mvc.perform(get(url).param("terminology", "ncit")).andExpect(status().isBadRequest())
        .andReturn();
    // content is blank because of MockMvc

    // Missing "terminology" parameter
    url = baseUrl;
    log.info("Testing url - " + url + "?term=melanoma");
    mvc.perform(get(url).param("melanoma", "melanoma")).andExpect(status().isBadRequest())
        .andReturn();
    // content is blank because of MockMvc

    // Invalid "include" parameter
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&include=XXX");
    mvc.perform(
        get(url).param("terminology", "ncit").param("term", "melanoma").param("include", "XXX"))
        .andExpect(status().isBadRequest()).andReturn();
    // content is blank because of MockMvc

    // Page size too big
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=blood&pageSize=101");
    mvc.perform(
        get(url).param("terminology", "ncit").param("term", "blood").param("pageSize", "101"))
        .andExpect(status().isBadRequest()).andReturn();
    // content is blank because of MockMvc

    // Page size too big - 2
    url = baseUrl;
    log.info("Testing url - /api/v1/concept/ncit/search?term=blood&pageSize=101");
    mvc.perform(get("/api/v1/concept/ncit/search").param("term", "blood").param("pageSize", "101"))
        .andExpect(status().isBadRequest()).andReturn();
    // content is blank because of MockMvc

    // Page size too small
    url = baseUrl;
    log.info("Testing url - /api/v1/concept/ncit/search?term=blood&pageSize=0");
    mvc.perform(get("/api/v1/concept/ncit/search").param("term", "blood").param("pageSize", "0"))
        .andExpect(status().isBadRequest()).andReturn();
    // content is blank because of MockMvc

  }

  /**
   * Test search include.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchInclude() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&include=synonyms");

    // Test a basic term search
    result = this.mvc.perform(get(url).param("terminology", "ncit").param("term", "melanoma")
        .param("include", "synonyms")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    ConceptResultList list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(10);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getSynonyms()).isNotEmpty();
    assertThat(list.getTotal()).isGreaterThan(100);
    assertThat(list.getParameters().getTerm()).isEqualTo("melanoma");
    assertThat(list.getParameters().getType()).isEqualTo("contains");
    assertThat(list.getParameters().getFromRecord()).isEqualTo(0);
    assertThat(list.getParameters().getPageSize()).isEqualTo(10);

    // Test a basic term search with single terminology form
    url = "/api/v1/concept/ncit/search";
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&include=synonyms");

    result = this.mvc.perform(get(url).param("terminology", "ncit").param("term", "melanoma")
        .param("include", "synonyms")).andExpect(status().isOk()).andReturn();
    String content2 = result.getResponse().getContentAsString();

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualTo(content2);

  }

  /**
   * Test page size from page.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPageSizeFromRecord() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;
    // Page size smaller than default
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=2");

    // Test a basic term search
    result = this.mvc
        .perform(
            get(url).param("terminology", "ncit").param("term", "melanoma").param("pageSize", "2"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(2);

    // bad from record - fromRecord should be the first element of page
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&fromRecord=6");

    result = mvc.perform(
        get(url).param("terminology", "ncit").param("term", "melanoma").param("fromRecord", "6"))
        .andExpect(status().isBadRequest()).andReturn();

    // From 5 with page size of 5 (should match the last 9 records of
    // pageSize of 10
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=5&fromRecord=5");

    // Test a basic term search
    result = this.mvc
        .perform(get(url).param("terminology", "ncit").param("term", "melanoma")
            .param("pageSize", "5").param("fromRecord", "5"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(5);
    final List<Concept> cl1 = list.getConcepts();
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=10");

    // Test a basic term search
    result = this.mvc
        .perform(
            get(url).param("terminology", "ncit").param("term", "melanoma").param("pageSize", "10"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(10);
    assertThat(list.getConcepts().subList(5, 10).toString()).isEqualTo(cl1.toString());

    // Bad page size = -1
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=-1");
    result = mvc
        .perform(
            get(url).param("terminology", "ncit").param("term", "melanoma").param("pageSize", "-1"))
        .andExpect(status().isBadRequest()).andReturn();

    // Bad page size = 1001
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=1001");
    result = mvc.perform(
        get(url).param("terminology", "ncit").param("term", "melanoma").param("pageSize", "1001"))
        .andExpect(status().isBadRequest()).andReturn();

    // Bad from record = -1
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&fromRecord=-1");
    result = mvc.perform(
        get(url).param("terminology", "ncit").param("term", "melanoma").param("fromRecord", "-1"))
        .andExpect(status().isBadRequest()).andReturn();

    // Bad page size = not a number
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=ABC");
    result = mvc.perform(
        get(url).param("terminology", "ncit").param("term", "melanoma").param("pageSize", "ABC"))
        .andExpect(status().isBadRequest()).andReturn();

    // Bad from record = not a number
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&formRecord=ABC");
    result = mvc.perform(
        get(url).param("terminology", "ncit").param("term", "melanoma").param("fromRecord", "ABC"))
        .andExpect(status().isBadRequest()).andReturn();

  }

  /**
   * Returns the search property.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchProperty() throws Exception {

    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    String content2 = null;
    ConceptResultList list = null;

    log.info("Testing url - " + url
        + "?terminology=ncit&term=XAV05295I5&property=fda_unii_code&include=properties");

    result = mvc
        .perform(get(url).param("terminology", "ncit").param("include", "properties")
            .param("term", "XAV05295I5").param("property", "fda_unii_code"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(1);
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Sivifene");
    assertThat(list.getConcepts().get(0).getProperties().stream()
        .filter(p -> p.getType().equals("FDA_UNII_Code")).count()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getProperties().stream()
        .filter(p -> p.getType().equals("FDA_UNII_Code")).findFirst().get().getValue())
            .isEqualTo("XAV05295I5");

    // Test with single terminology form
    url = "/api/v1/concept/ncit/search";
    log.info("Testing url - " + url + "?term=XAV05295I5&property=fda_unii_code&include=properties");

    result = this.mvc.perform(get(url).param("include", "properties").param("term", "XAV05295I5")
        .param("property", "fda_unii_code")).andExpect(status().isOk()).andReturn();
    content2 = result.getResponse().getContentAsString();
    log.info("content2 -" + content2);

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualTo(content2);

    // With property code also - P319
    url = baseUrl;
    log.info("Testing url - " + url
        + "?terminology=ncit&term=XAV05295I5&property=P319&include=properties");

    result = mvc
        .perform(get(url).param("terminology", "ncit").param("include", "properties")
            .param("term", "XAV05295I5").param("property", "P319"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(1);
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Sivifene");
    assertThat(list.getConcepts().get(0).getProperties().stream()
        .filter(p -> p.getType().equals("FDA_UNII_Code")).count()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getProperties().stream()
        .filter(p -> p.getType().equals("FDA_UNII_Code")).findFirst().get().getValue())
            .isEqualTo("XAV05295I5");

    // Test with single terminology form
    url = "/api/v1/concept/ncit/search";
    log.info("Testing url - " + url + "?term=XAV05295I5&property=P319&include=properties");

    result = this.mvc.perform(get(url).param("include", "properties").param("term", "XAV05295I5")
        .param("property", "P319")).andExpect(status().isOk()).andReturn();
    content2 = result.getResponse().getContentAsString();
    log.info("content2 -" + content2);

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualTo(content2);

    // BAD property type
    // url = baseUrl;
    // log.info("Testing url - " + url +
    // "?terminology=ncit&term=XAV05295I5&property=P999999");
    //
    // result = mvc.perform(get(url).param("terminology", "ncit").param("term",
    // "XAV05295I5")
    // .param("property",
    // "P999999")).andExpect(status().isBadRequest()).andReturn();
    //
    // // Test with single terminology form
    // url = "/api/v1/concept/ncit/search";
    // log.info("Testing url - " + url + "?term=XAV05295I5&property=P999999");
    //
    // result = this.mvc.perform(get(url).param("term",
    // "XAV05295I5").param("property", "P999999"))
    // .andExpect(status().isBadRequest()).andReturn();
    log.info("Done Testing testSearchProperty ");
  }

  /**
   * Test search type fuzzy.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchType() throws Exception {

    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Fuzzy search
    log.info("Testing url - " + url + "?terminology=ncit&term=enzymi&type=fuzzy");

    result = mvc
        .perform(
            get(url).param("terminology", "ncit").param("term", "enzymi").param("type", "fuzzy"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getName()).containsIgnoringCase("enzyme");

    // Exact search
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&type=exact");

    result = mvc
        .perform(
            get(url).param("terminology", "ncit").param("term", "melanoma").param("type", "exact"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getName()).isEqualToIgnoringCase("melanoma");

    // Phrase search
    log.info("Testing url - " + url + "?terminology=ncit&term=malignant%20melanoma&type=phrase");

    result = mvc.perform(get(url).param("terminology", "ncit").param("term", "malignant melanoma")
        .param("type", "phrase")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // Match search
    log.info("Testing url - " + url + "?terminology=ncit&term=Lung%20Carcinoma&type=match");

    result = mvc.perform(get(url).param("terminology", "ncit").param("term", "Lung Carcinoma")
        .param("type", "match")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // startsWith search
    log.info("Testing url - " + url + "?terminology=ncit&term=enzyme&type=startsWith");

    result = mvc.perform(
        get(url).param("terminology", "ncit").param("term", "enzyme").param("type", "startsWith"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // AND search
    log.info("Testing url - " + url + "?terminology=ncit&term=lentiginous%20melanoma&type=AND");

    result = mvc.perform(get(url).param("terminology", "ncit").param("term", "lentiginous melanoma")
        .param("type", "AND")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // OR search
    log.info("Testing url - " + url + "?terminology=ncit&term=lentiginous%20melanoma&type=OR");

    result = mvc.perform(get(url).param("terminology", "ncit").param("term", "lentiginous melanoma")
        .param("type", "OR")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // Bad type
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&type=exact2");

    result = mvc
        .perform(
            get(url).param("terminology", "ncit").param("term", "melanoma").param("type", "exact2"))
        .andExpect(status().isBadRequest()).andReturn();

    log.info("Done Testing testSearchType");

  }

  /**
   * Test search type fuzzy.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptStatus() throws Exception {

    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Retired_Concept
    log.info("Testing url - " + url + "?terminology=ncit&term=crop&conceptStatus=Obsolete_Concept");

    result = mvc.perform(get(url).param("terminology", "ncit").param("term", "melanoma")
        .param("conceptStatus", "Obsolete_Concept")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // Bad value
    log.info("Testing url - " + url + "?terminology=ncit&term=crop&conceptStatus=Bad_Value");

    result = mvc.perform(get(url).param("terminology", "ncit").param("term", "crop")
        .param("conceptStatus", "Bad_Value")).andExpect(status().isBadRequest()).andReturn();

    log.info("Done Testing testSearchType");

  }

  /**
   * Test synonym source.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSynonymSource() throws Exception {

    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Test synonymSource
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&synonymSource=NCI");
    result = mvc
        .perform(get(url).param("terminology", "ncit").param("term", "dsDNA")
            .param("synonymSource", "CDISC").param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    boolean found = false;
    for (final Synonym syn : list.getConcepts().get(0).getSynonyms()) {
      if (syn.getName().contains("dsDNA") && syn.getSource().equals("CDISC")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();

    // Test synonymSource+SynonymTermGroup
    log.info("Testing url - " + url
        + "?terminology=ncit&term=dsDNA&synonymSource=NCI&synonymTermGroup=SY");
    result = mvc.perform(get(url).param("terminology", "ncit").param("term", "dsDNA")
        .param("synonymSource", "NCI").param("synonymTermGroup", "SY").param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    found = false;
    for (final Synonym syn : list.getConcepts().get(0).getSynonyms()) {
      if (syn.getName().contains("dsDNA") && syn.getSource().equals("CDISC")
          && syn.getTermGroup().equals("SY")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();

    // Bad value test - just no results found in this case.
    // Valid test
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&synonymSource=Bad_Value");
    result = mvc.perform(get(url).param("terminology", "ncit").param("term", "dsDNA")
        .param("synonymSource", "Bad_Value")).andExpect(status().isBadRequest()).andReturn();

    log.info("Done Testing testSynonymSource");

  }

  /**
   * Test synonym termGroup.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSynonymTermGroup() throws Exception {

    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Test single SynonymTermGroup
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&synonymTermGroup=SY");
    result = mvc
        .perform(get(url).param("terminology", "ncit").param("term", "dsDNA")
            .param("synonymTermGroup", "SY").param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    boolean found = false;
    for (final Synonym syn : list.getConcepts().get(0).getSynonyms()) {
      if (syn.getName().contains("dsDNA") && syn.getTermGroup().equals("SY")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();

    // Test multiple SynonymTermGroup
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&synonymTermGroup=DN,SY&");
    result = mvc
        .perform(get(url).param("terminology", "ncit").param("term", "dsDNA")
            .param("synonymTermGroup", "DN,SY").param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    found = false;
    boolean found1 = false;
    for (final Concept concept : list.getConcepts()) {
      for (final Synonym syn : concept.getSynonyms()) {
        if (syn.getName().contains("DNA")) {
          if (!found)
            found = "SY".equals(syn.getTermGroup());
          if (!found1)
            found1 = "PT".equals(syn.getTermGroup());
          if (found && found1)
            break;
        }
      }
    }

    log.info("found = {}, found1 = {}", found, found1);
    assertThat(found && found1).isTrue();

    log.info("Done Testing testSynonymTermGroup");

  }

  /**
   * Test definition source.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDefinitionSource() throws Exception {

    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Valid test
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&definitionSource=NCI");
    result = mvc
        .perform(get(url).param("terminology", "ncit").param("term", "melanoma")
            .param("definitionSource", "NCI").param("include", "definitions"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    boolean found = false;
    for (final Definition def : list.getConcepts().get(0).getDefinitions()) {
      if (def.getDefinition().contains("melanoma") && def.getSource().equals("NCI")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();

    // Bad value test - just no results found in this case.
    // Valid test
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&definitionSource=Bad_Value");
    result = mvc.perform(get(url).param("terminology", "ncit").param("term", "dsDNA")
        .param("definitionSource", "Bad_Value")).andExpect(status().isBadRequest()).andReturn();

    log.info("Done Testing testDefinitionSource ");

  }

  /**
   * Removes the time taken.
   *
   * @param response the response
   * @return the string
   */
  private String removeTimeTaken(String response) {
    return response.replaceAll("\"timeTaken\"\\s*:\\s*\\d+\\s*,", "");
  }
}
