package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.MapResultList;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Integration tests for SearchController. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SearchControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(SearchControllerTests.class);

  /** The mvc. */
  @Autowired private MockMvc mvc;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The application properties. */
  @Autowired ApplicationProperties appProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** The base url. */
  private String baseUrl = "";

  /** The base url. */
  private String baseUrlNoTerm = "";

  /** Sets the up. */
  @Before
  public void setUp() {
    /*
     * Configure the JacksonTester object
     */
    this.objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1/concept/search";
    baseUrlNoTerm = "/api/v1/concept";
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
    result =
        this.mvc
            .perform(get(url).param("terminology", "ncit").param("term", "melanoma"))
            .andExpect(status().isOk())
            .andReturn();
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

    assertThat(content).isEqualToIgnoringCase(content2);
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
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("include", "highlights"))
            .andExpect(status().isOk())
            .andReturn();
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
    result =
        this.mvc
            .perform(get(url).param("term", "melanoma").param("include", "highlights"))
            .andExpect(status().isOk())
            .andReturn();
    String content2 = result.getResponse().getContentAsString();

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualToIgnoringCase(content2);

    // Test an NCIM search for C192 -> expect synonym to be highlighted
    url = "/api/v1/concept/ncim/search";
    log.info("Testing url - " + url + "?term=C192&include=synonyms,highlights");
    result =
        this.mvc
            .perform(get(url).param("term", "C192").param("include", "synonyms,highlights"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isEqualTo(1);
    assertThat(
            list.getConcepts().get(0).getSynonyms().stream()
                .filter(s -> s.getHighlight() != null)
                .count())
        .isGreaterThan(0);
    assertThat(
            list.getConcepts().get(0).getSynonyms().stream()
                .filter(s -> s.getHighlight() != null && s.getHighlight().contains("<em>C192"))
                .count())
        .isGreaterThan(0);

    // Test an NCIt search for 10053571 -> expect synonym to be highlighted
    url = "/api/v1/concept/ncit/search";
    log.info("Testing url - " + url + "?term=10053571&include=synonyms,highlights");
    result =
        this.mvc
            .perform(get(url).param("term", "10053571").param("include", "synonyms,highlights"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isEqualTo(1);
    assertThat(
            list.getConcepts().get(0).getSynonyms().stream()
                .filter(s -> s.getHighlight() != null)
                .count())
        .isEqualTo(1);
    assertThat(
            list.getConcepts().get(0).getSynonyms().stream()
                .filter(s -> s.getHighlight() != null && s.getHighlight().contains("<em>10053571"))
                .count())
        .isEqualTo(1);
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
        .andExpect(status().isNotFound())
        .andReturn();
    // content is blank because of MockMvc

    // Invalid "include" parameter
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&include=XYZ");
    mvc.perform(
            get(url).param("terminology", "ncit").param("term", "melanoma").param("include", "XYZ"))
        .andExpect(status().isBadRequest())
        .andReturn();
    // content is blank because of MockMvc

    // Page size too big
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=blood&pageSize=1001");
    mvc.perform(
            get(url).param("terminology", "ncit").param("term", "blood").param("pageSize", "1001"))
        .andExpect(status().isBadRequest())
        .andReturn();
    // content is blank because of MockMvc

    // Page size too big - 2
    url = baseUrl;
    log.info("Testing url - /api/v1/concept/ncit/search?term=blood&pageSize=1001");
    mvc.perform(get("/api/v1/concept/ncit/search").param("term", "blood").param("pageSize", "1001"))
        .andExpect(status().isBadRequest())
        .andReturn();
    // content is blank because of MockMvc

    // Page size too small
    url = baseUrl;
    log.info("Testing url - /api/v1/concept/ncit/search?term=blood&pageSize=0");
    mvc.perform(get("/api/v1/concept/ncit/search").param("term", "blood").param("pageSize", "0"))
        .andExpect(status().isBadRequest())
        .andReturn();
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
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
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

    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    String content2 = result.getResponse().getContentAsString();

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualToIgnoringCase(content2);
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
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("pageSize", "2"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(2);

    // fromRecord not matching start of page
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&fromRecord=6&pageSize=10");

    // Try from records within page size
    for (final String fr : new String[] {"1", "9", "6"}) {
      result =
          mvc.perform(
                  get(url)
                      .param("terminology", "ncit")
                      .param("term", "melanoma")
                      .param("fromRecord", fr)
                      .param("pageSize", "10"))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      log.info("  content = " + content);
      assertThat(content).isNotNull();
      list = new ObjectMapper().readValue(content, ConceptResultList.class);
      assertThat(list.getConcepts().size()).isEqualTo(10);
    }
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("fromRecord", "16")
                    .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    ConceptResultList list2 = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list2.getConcepts().size()).isEqualTo(10);
    // should be a different code starting on 16
    assertThat(list.getConcepts().get(0).getCode())
        .isNotEqualTo(list2.getConcepts().get(0).getCode());

    // From record beyond last result
    // fromRecord=12&pageSize=10&term=C12913&type=startsWith
    url = baseUrl;
    log.info(
        "Testing url - "
            + url
            + "?terminology=ncit&fromRecord=12&pageSize=10&term=C12913&type=startsWith");

    // Test a basic term search
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "C12913")
                    .param("pageSize", "10")
                    .param("fromRecord", "12")
                    .param("type", "startsWith"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts()).isEmpty();

    // From 5 with page size of 5 (should match the last 9 records of
    // pageSize of 10
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=5&fromRecord=5");

    // Test a basic term search
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("pageSize", "5")
                    .param("fromRecord", "5"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(5);
    final List<Concept> cl1 = list.getConcepts();
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=10");

    // Test a basic term search
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(10);
    assertThat(list.getConcepts().subList(5, 10).toString()).isEqualTo(cl1.toString());

    // Bad page size = -1, 0
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=-1");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("pageSize", "-1"))
            .andExpect(status().isBadRequest())
            .andReturn();
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=0");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("pageSize", "0"))
            .andExpect(status().isBadRequest())
            .andReturn();

    // Bad page size = 1001
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=1001");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("pageSize", "1001"))
            .andExpect(status().isBadRequest())
            .andReturn();

    // Bad from record = -1
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&fromRecord=-1");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("fromRecord", "-1"))
            .andExpect(status().isBadRequest())
            .andReturn();

    // Bad page size = not a number
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=ABC");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("pageSize", "ABC"))
            .andExpect(status().isBadRequest())
            .andReturn();

    // Bad from record = not a number
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&formRecord=ABC");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("fromRecord", "ABC"))
            .andExpect(status().isBadRequest())
            .andReturn();
  }

  /**
   * Test page size license restricted.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPageSizeLicenseRestricted() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Legal page size
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=mdr&term=cancer&pageSize=10");
    result =
        this.mvc
            .perform(
                get(url)
                    .header("X-EVSRESTAPI-License-Key", appProperties.getUiLicense())
                    .param("terminology", "mdr")
                    .param("term", "cancer")
                    .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // Over limit
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=mdr&term=cancer&pageSize=11");
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "mdr")
                    .param("term", "cancer")
                    .param("pageSize", "11"))
            .andExpect(status().isForbidden())
            .andReturn();

    // Legal page size
    url = "/api/v1/concept/mdr/search";
    log.info("Testing url - /api/v1/concept/mdr/search?term=cancer&pageSize=10");
    result =
        this.mvc
            .perform(
                get(url)
                    .header("X-EVSRESTAPI-License-Key", appProperties.getUiLicense())
                    .param("term", "cancer")
                    .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // Over limit
    url = "/api/v1/concept/mdr/search";
    log.info("Testing url - /api/v1/concept/mdr/search?term=cancer&pageSize=11");
    result =
        this.mvc
            .perform(get(url).param("term", "cancer").param("pageSize", "11"))
            .andExpect(status().isForbidden())
            .andReturn();
  }

  /**
   * Returns the search property.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchProperty() throws Exception {

    String url = baseUrlNoTerm;
    MvcResult result = null;
    String content = null;
    String content2 = null;
    ConceptResultList list = null;

    // check that search requires exact

    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("include", "properties")
                    .param("term", "XAV05295I5")
                    .param("property", "FDA"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() == 0);

    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("include", "properties")
                    .param("term", "XAV0")
                    .param("property", "FDA_UNII_Code"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() == 0);

    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("include", "properties")
                    .param("value", "XAV0")
                    .param("property", "P999"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() == 0);

    log.info(
        "Testing url - "
            + url
            + "?terminology=ncit&value=XAV05295I5&property=FDA_UNII_Code&include=properties");

    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("include", "properties")
                    .param("value", "XAV05295I5")
                    .param("property", "FDA_UNII_Code"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(1);
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Sivifene");
    assertThat(
            list.getConcepts().get(0).getProperties().stream()
                .filter(p -> p.getType().equals("FDA_UNII_Code"))
                .count())
        .isGreaterThan(0);
    // All results an FDA_UNII_Code property with the specified value
    assertThat(
            list.getConcepts().stream()
                .filter(
                    c ->
                        c.getProperties().stream()
                                .filter(
                                    p ->
                                        p.getType().equals("FDA_UNII_Code")
                                            && p.getValue().equals("XAV05295I5"))
                                .count()
                            > 0)
                .count())
        .isEqualTo(list.getConcepts().size());

    // Test with single terminology form
    url = "/api/v1/concept/ncit/search";
    log.info(
        "Testing url - " + url + "?value=XAV05295I5&property=FDA_UNII_Code&include=properties");

    result =
        this.mvc
            .perform(
                get(url)
                    .param("include", "properties")
                    .param("value", "XAV05295I5")
                    .param("property", "FDA_UNII_Code"))
            .andExpect(status().isOk())
            .andReturn();
    content2 = result.getResponse().getContentAsString();
    log.info("content2 -" + content2);

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualToIgnoringCase(content2);

    // With property code also - P319
    url = baseUrlNoTerm;
    log.info(
        "Testing url - "
            + url
            + "/ncit/search"
            + "?value=XAV05295I5&property=P319&include=properties");

    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("include", "properties")
                    .param("value", "XAV05295I5")
                    .param("property", "P319"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(1);
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Sivifene");
    assertThat(
            list.getConcepts().get(0).getProperties().stream()
                .filter(p -> p.getType().equals("FDA_UNII_Code"))
                .count())
        .isGreaterThan(0);
    assertThat(
            list.getConcepts().get(0).getProperties().stream()
                .filter(p -> p.getType().equals("FDA_UNII_Code"))
                .findFirst()
                .get()
                .getValue())
        .isEqualTo("XAV05295I5");

    // Test with single terminology form
    url = "/api/v1/concept/ncit/search";
    log.info("Testing url - " + url + "?value=XAV05295I5&property=P319&include=properties");

    result =
        this.mvc
            .perform(
                get(url)
                    .param("include", "properties")
                    .param("value", "XAV05295I5")
                    .param("property", "P319"))
            .andExpect(status().isOk())
            .andReturn();
    content2 = result.getResponse().getContentAsString();
    log.info("content2 -" + content2);

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualToIgnoringCase(content2);

    // BAD property type
    url = baseUrlNoTerm;
    log.info(
        "Testing url - "
            + url
            + "/ncit/search"
            + "?terminology=ncit&value=XAV05295I5&property=P999999");

    result =
        mvc.perform(
                get(url + "/ncit/search").param("value", "XAV05295I5").param("property", "P999999"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() == 0);

    // Test with single terminology form
    url = "/api/v1/concept/ncit/search";
    log.info("Testing url - " + url + "?value=XAV05295I5&property=P999999");

    result =
        this.mvc
            .perform(get(url).param("value", "XAV05295I5").param("property", "P999999"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() == 0);
    log.info("Done Testing testSearchProperty ");

    // search by property and term
    // property=FDA_UNII_CODE and term=Toluene
    log.info("Testing url - " + url + "?terminology=ncit&property=FDA_UNII_Code&term=Toluene");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("property", "FDA_UNII_Code")
                    .param("include", "properties,synonyms,definitions")
                    .param("term", "Toluene"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();
    // all should have a name containing "Toluene" and have an FDA_UNII_CODE
    assertThat(
            list.getConcepts().stream()
                .filter(
                    c ->
                        c.getProperties().stream()
                                        .filter(p -> p.getType().equals("FDA_UNII_Code"))
                                        .count()
                                    > 0
                                && (c.getName().toLowerCase().contains("toluene")
                                    || c.getSynonyms().stream()
                                            .filter(
                                                s -> s.getName().toLowerCase().contains("toluene"))
                                            .count()
                                        > 0)
                            || c.getDefinitions().stream()
                                    .filter(
                                        s -> s.getDefinition().toLowerCase().contains("toluene"))
                                    .count()
                                > 0)
                .count())
        .isEqualTo(list.getConcepts().size());
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

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "enzymi")
                    .param("type", "fuzzy"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getName()).containsIgnoringCase("enzyme");

    // Exact search
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&type=exact");

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("type", "exact"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getName()).isEqualToIgnoringCase("melanoma");

    // Phrase search
    log.info("Testing url - " + url + "?terminology=ncit&term=malignant%20melanoma&type=phrase");

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "malignant melanoma")
                    .param("type", "phrase"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // Match search
    log.info("Testing url - " + url + "?terminology=ncit&term=Lung%20Carcinoma&type=match");

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "Lung Carcinoma")
                    .param("type", "match"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // startsWith search
    log.info("Testing url - " + url + "?terminology=ncit&term=enzyme&type=startsWith");

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "enzyme")
                    .param("type", "startsWith"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // AND search
    log.info("Testing url - " + url + "?terminology=ncit&term=lentiginous%20melanoma&type=AND");

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "lentiginous melanoma")
                    .param("type", "AND"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // OR search
    log.info("Testing url - " + url + "?terminology=ncit&term=lentiginous%20melanoma&type=OR");

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "lentiginous melanoma")
                    .param("type", "OR"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    // Bad type
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma&type=exact2");

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("type", "exact2"))
            .andExpect(status().isBadRequest())
            .andReturn();

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

    // Obsolete_Concept
    log.info(
        "Testing url - " + url + "?terminology=ncit&term=melanoma&conceptStatus=Obsolete_Concept");

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("conceptStatus", "Obsolete_Concept")
                    .param("include", "properties"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    // Verify property of "Obsolete_Concept on each results
    assertThat(
            list.getConcepts().stream()
                .flatMap(c -> c.getProperties().stream())
                .filter(
                    p ->
                        p.getType().contentEquals("Concept_Status")
                            && p.getValue().contentEquals("Obsolete_Concept"))
                .count())
        .isEqualTo(list.getConcepts().size());

    // Provisional_Concept
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "Lymphoid")
                    .param("conceptStatus", "Provisional_Concept")
                    .param("include", "properties"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    // Verify property of "Obsolete_Concept on each results
    assertThat(
            list.getConcepts().stream()
                .flatMap(c -> c.getProperties().stream())
                .filter(
                    p ->
                        p.getType().contentEquals("Concept_Status")
                            && p.getValue().contentEquals("Provisional_Concept"))
                .count())
        .isEqualTo(list.getConcepts().size());

    // Header_Concept
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "blood")
                    .param("conceptStatus", "Header_Concept")
                    .param("include", "properties"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    // Verify property of "Obsolete_Concept on each results
    assertThat(
            list.getConcepts().stream()
                .flatMap(c -> c.getProperties().stream())
                .filter(
                    p ->
                        p.getType().contentEquals("Concept_Status")
                            && p.getValue().contentEquals("Header_Concept"))
                .count())
        .isEqualTo(list.getConcepts().size());

    // Bad value
    log.info("Testing url - " + url + "?terminology=ncit&term=crop&conceptStatus=Bad_Value");

    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "crop")
                    .param("conceptStatus", "Bad_Value"))
            .andExpect(status().isBadRequest())
            .andReturn();

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

    // incomplete search
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&synonymSource=NCIT");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "dsDNA")
                    .param("synonymSource", "NCIT"))
            .andExpect(status().isBadRequest())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content.length() == 0); // incomplete query fails

    // Test synonymSource
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&synonymSource=CDISC");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "dsDNA")
                    .param("synonymSource", "CDISC")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    boolean found = false;
    for (final Synonym syn : list.getConcepts().get(0).getSynonyms()) {
      if (syn.getName().contains("dsDNA")
          && syn.getSource() != null
          && syn.getSource().equals("CDISC")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();

    // Test synonymSource+SynonymTermType
    log.info(
        "Testing url - "
            + url
            + "?terminology=ncit&term=dsDNA&synonymSource=NCI&synonymTermType=SY");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "dsDNA")
                    .param("synonymSource", "NCI")
                    .param("synonymTermType", "SY")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    found = false;
    // check concept synonyms for valid synonym
    for (final Synonym syn : list.getConcepts().get(0).getSynonyms()) {
      if (syn.getName().contains("dsDNA")
          && syn.getSource().equals("CDISC")
          && syn.getTermType().equals("SY")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();

    // Bad value test - just no results found in this case.
    // Valid test
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&synonymSource=Bad_Value");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "dsDNA")
                    .param("synonymSource", "Bad_Value"))
            .andExpect(status().isBadRequest())
            .andReturn();

    log.info("Done Testing testSynonymSource");
  }

  /**
   * Test synonym termType.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSynonymTermType() throws Exception {

    String url = baseUrlNoTerm;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // incomplete search, no termTypes matching SY
    log.info("Testing url - " + url + "/ncit/search" + "?term=dsDNA&synonymTermType=S");
    result =
        mvc.perform(get(url + "/ncit/search").param("term", "dsDNA").param("synonymTermType", "S"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal() == 0);

    // Test single SynonymTermType
    log.info("Testing url - " + url + "/ncit/search" + "?term=dsDNA&synonymTermType=SY");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("term", "dsDNA")
                    .param("synonymTermType", "SY")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);

    boolean found = false;
    for (final Synonym syn : list.getConcepts().get(0).getSynonyms()) {
      if (syn.getName().contains("dsDNA") && syn.getTermType().equals("SY")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();

    // Test multiple SynonymTermType
    log.info("Testing url - " + url + "/ncit/search" + "?term=dsDNA&synonymTermType=DN,SY");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("term", "dsDNA")
                    .param("synonymTermType", "DN,SY")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
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
          if (!found) found = "SY".equals(syn.getTermType());
          if (!found1) found1 = "PT".equals(syn.getTermType());
          if (found && found1) break;
        }
      }
    }

    log.info("found = {}, found1 = {}", found, found1);
    assertThat(found && found1).isTrue();

    // Test synonymSource + synonymTermType
    // ?include=summary&pageSize=100&synonymSource=CDISC&synonymTermType=SY&term=blood
    log.info(
        "Testing url - "
            + url
            + "/ncit/search"
            + "?include=summary&pageSize=1000&synonymSource=CDISC&synonymTermType=SY&term=blood");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("term", "blood")
                    .param("synonymSource", "CDISC")
                    .param("pageSize", "1000")
                    .param("synonymTermType", "SY")
                    .param("include", "summary"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    // Verify that each concept contains a CDISC/SY synonym
    assertThat(
            list.getConcepts().stream()
                .filter(
                    c ->
                        c.getSynonyms().stream()
                                .filter(
                                    s ->
                                        "SY".equals(s.getTermType())
                                            && "CDISC".equals(s.getSource()))
                                .count()
                            > 0)
                .count())
        .isEqualTo(list.getConcepts().size());

    // Test synonymSource + synonymTermType without a term
    log.info(
        "Testing url - "
            + url
            + "/ncit/search"
            + "?include=synonyms&pageSize=10&synonymSource=CDISC&synonymTermType=SY");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("synonymSource", "CDISC")
                    .param("pageSize", "10")
                    .param("synonymTermType", "SY")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    // Verify that each concept contains a CDISC/SY synonym
    assertThat(
            list.getConcepts().stream()
                .filter(
                    c ->
                        c.getSynonyms().stream()
                                .filter(
                                    s ->
                                        "SY".equals(s.getTermType())
                                            && "CDISC".equals(s.getSource()))
                                .count()
                            > 0)
                .count())
        .isEqualTo(list.getConcepts().size());

    log.info("Done Testing testSynonymTermType");
  }

  /**
   * Test synonym type.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSynonymType() throws Exception {

    String url = baseUrlNoTerm;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // synonymType=P90
    log.info("Testing url - " + url + "/ncit/search" + "?synonymType=P90&include=synonyms");
    result =
        mvc.perform(
                get(url + "/ncit/search").param("synonymType", "P90").param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);

    boolean found = false;
    for (final Synonym syn : list.getConcepts().get(0).getSynonyms()) {
      if (syn.getType().equals("FULL_SYN")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();

    // synonymType=FULL_SYN
    log.info("Testing url - " + url + "?synonymType=FULL_SYN&include=synonyms");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("synonymType", "FULL_SYN")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);

    for (final Synonym syn : list.getConcepts().get(0).getSynonyms()) {
      if (syn.getType().equals("FULL_SYN")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();
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
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "melanoma")
                    .param("definitionSource", "NCI")
                    .param("include", "definitions"))
            .andExpect(status().isOk())
            .andReturn();
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
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&definitionSource=Bad_Value");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "dsDNA")
                    .param("definitionSource", "Bad_Value"))
            .andExpect(status().isBadRequest())
            .andReturn();

    // incomplete match
    log.info("Testing url - " + url + "?terminology=ncit&term=dsDNA&definitionSource=NC");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "dsDNA")
                    .param("definitionSource", "NC"))
            .andExpect(status().isBadRequest())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content.length() == 0); // incomplete query fails

    log.info("Done Testing testDefinitionSource ");
  }

  /**
   * Test definition type.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDefinitionType() throws Exception {

    String url = baseUrlNoTerm;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // definitionType=P325
    log.info("Testing url - " + url + "/ncit/search" + "?definitionType=P325&include=definitions");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("definitionType", "P325")
                    .param("include", "definitions"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);

    boolean found = false;
    for (final Definition def : list.getConcepts().get(0).getDefinitions()) {
      if (def.getType().equals("ALT_DEFINITION")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();

    // definitionType=ALT_DEFINITION
    log.info(
        "Testing url - "
            + url
            + "/ncit/search"
            + "?definitionType=ALT_DEFINITION&include=definitions");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("definitionType", "ALT_DEFINITION")
                    .param("include", "definitions"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);

    for (final Definition def : list.getConcepts().get(0).getDefinitions()) {
      if (def.getType().equals("ALT_DEFINITION")) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();
  }

  /**
   * Test search contains.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchContains() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Valid test
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms&pageSize=100&term=braf&type=contains");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "braf")
                    .param("pageSize", "100")
                    .param("type", "contains")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();

    log.info("Testing url - " + url + "?fromRecord=0&include=synonyms&pageSize=100&term=braf");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "braf")
                    .param("include", "synonyms")
                    .param("pageSize", "100"))
            .andExpect(status().isOk())
            .andReturn();

    // check that no type = contains
    String content2 = result.getResponse().getContentAsString();

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualToIgnoringCase(content2);

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    List<Concept> conceptList = list.getConcepts();
    boolean currentExact = true;
    for (Concept concept : conceptList) {
      // found match
      if (concept.getName().equalsIgnoreCase("braf")
          || !concept.getSynonyms().stream()
              .filter(p -> p.getName().equalsIgnoreCase("braf"))
              .collect(Collectors.toList())
              .isEmpty()) {
        // check still in front
        if (!currentExact)
          // exact matches not inorder
          fail("Exact Matches not in order");
      } else
        // should be at end of exact matches
        currentExact = false;
    }

    // phrase term check
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms&pageSize=100&term=malignant%20bone%20neoplasm&type=contains");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "malignant bone neoplasm")
                    .param("pageSize", "100")
                    .param("type", "contains")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();

    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms&pageSize=100&term=malignant%20bone%20neoplasm");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "malignant bone neoplasm")
                    .param("include", "synonyms")
                    .param("pageSize", "100"))
            .andExpect(status().isOk())
            .andReturn();

    // check that no type = contains
    content2 = result.getResponse().getContentAsString();

    // removing timeTaken key from json before comparison
    content = removeTimeTaken(content);
    content2 = removeTimeTaken(content2);

    assertThat(content).isEqualToIgnoringCase(content2);

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    currentExact = true;
    for (Concept concept : conceptList) {
      if (concept.getName().equalsIgnoreCase("braf")
          || !concept.getSynonyms().stream()
              .filter(p -> p.getName().toLowerCase().equalsIgnoreCase("braf"))
              .collect(Collectors.toList())
              .isEmpty()) { // found
        // match
        if (!currentExact) // check still in front
        fail("Exact Matches not in order"); // exact matches not in
        // order
      } else currentExact = false; // should be at end of exact matches
    }
  }

  /**
   * Test search contains coronary vein.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchContainsCoronaryVein() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Find corona
    log.info("Testing url - " + url + "?include=synonyms&term=corona&type=contains&pageSize=50");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "corona")
                    .param("type", "contains")
                    .param("pageSize", "50")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    assertThat(content).isNotEmpty();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    // The first one should contain the word "corona" (e.g. crown, corona
    // dentist)
    assertThat(
            list.getConcepts().get(0).getSynonyms().stream()
                .filter(s -> s.getName().toLowerCase().contains("corona"))
                .count())
        .isGreaterThan(0);
    assertThat(
            list.getConcepts().stream()
                .filter(
                    c ->
                        c.getSynonyms().stream()
                                .filter(s -> s.getName().toLowerCase().contains("coronaviridae"))
                                .count()
                            > 0)
                .count())
        .isGreaterThan(0);

    // Find coronary vein
    for (final String coronaryVein : new String[] {"corona vei", "coron vein", "cor vei"}) {
      log.info(
          "Testing url - "
              + url
              + "?include=synonyms&term="
              + coronaryVein
              + "&type=contains&pageSize=50");
      result =
          mvc.perform(
                  get(url)
                      .param("terminology", "ncit")
                      .param("term", coronaryVein)
                      .param("type", "contains")
                      .param("include", "synonyms")
                      .param("pageSize", "50"))
              .andExpect(status().isOk())
              .andReturn();
      content = result.getResponse().getContentAsString();
      log.info("  content = " + content);

      assertThat(content).isNotEmpty();
      list = new ObjectMapper().readValue(content, ConceptResultList.class);
      // The first one should contain the word "corona" (e.g. crown, corona
      // dentist)
      assertThat(
              list.getConcepts().stream()
                  .filter(
                      c ->
                          c.getSynonyms().stream()
                                  .filter(s -> s.getName().toLowerCase().contains("coronary vein"))
                                  .count()
                              > 0)
                  .count())
          .isGreaterThan(0);
    }
  }

  /**
   * Test search phrase.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchPhrase() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Valid test
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=malignant%20bone%20neoplasm&type=phrase");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "malignant bone neoplasm")
                    .param("pageSize", "100")
                    .param("type", "phrase")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    List<Concept> conceptList = list.getConcepts();
    boolean currentExact = true;
    for (Concept concept : conceptList) {
      if (concept.getName().equalsIgnoreCase("malignant bone neoplasm")
          || !concept.getSynonyms().stream()
              .filter(p -> p.getName().toLowerCase().equalsIgnoreCase("malignant bone neoplasm"))
              .collect(Collectors.toList())
              .isEmpty()
          || !concept.getProperties().stream()
              .filter(p -> p.getValue().toLowerCase().equalsIgnoreCase("malignant bone neoplasm"))
              .collect(Collectors.toList())
              .isEmpty()
          || !concept.getDefinitions().stream()
              .filter(
                  p -> p.getDefinition().toLowerCase().equalsIgnoreCase("malignant bone neoplasm"))
              .collect(Collectors.toList())
              .isEmpty()) { // found
        // match
        if (!currentExact) // check still in front
        fail("Exact Matches not in order");
      } else currentExact = false; // should be at end of exact matches
    }

    // another valid test for bug fixing
    log.info("Testing url - " + url + "?terminology=ncit&term=corona&type=phrase");
    result =
        mvc.perform(get(url + "?terminology=ncit&term=corona&type=phrase"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    assertThat(conceptList.size()).isEqualTo(1);
  }

  /**
   * Test search starts with.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchStartsWith() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Valid test
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=braf&type=startsWith");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "braf")
                    .param("pageSize", "100")
                    .param("type", "startsWith")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    List<Concept> conceptList = list.getConcepts();
    for (Concept concept : conceptList) {
      assertTrue(
          concept.getName().toLowerCase().startsWith("braf")
              || !concept.getSynonyms().stream()
                  .filter(p -> p.getName().toLowerCase().startsWith("braf"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getProperties().stream()
                  .filter(p -> p.getValue().toLowerCase().startsWith("braf"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getDefinitions().stream()
                  .filter(p -> p.getDefinition().toLowerCase().startsWith("braf"))
                  .collect(Collectors.toList())
                  .isEmpty());
    }

    // phrase term check
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=malignant%20bone%20neoplasm&type=startsWith");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "malignant bone neoplasm")
                    .param("pageSize", "100")
                    .param("type", "startsWith")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    assertTrue(conceptList.get(0).getName().equalsIgnoreCase("malignant bone neoplasm"));
    for (Concept concept : conceptList) {
      assertTrue(
          concept.getName().toLowerCase().startsWith("malignant bone neoplasm")
              || !concept.getSynonyms().stream()
                  .filter(p -> p.getName().toLowerCase().startsWith("malignant bone neoplasm"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getProperties().stream()
                  .filter(p -> p.getValue().toLowerCase().startsWith("malignant bone neoplasm"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getDefinitions().stream()
                  .filter(
                      p -> p.getDefinition().toLowerCase().startsWith("malignant bone neoplasm"))
                  .collect(Collectors.toList())
                  .isEmpty());
    }

    // exact matches come first
    log.info("Testing url - " + url + "?include=minimal&term=blood&type=startsWith");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "blood")
                    .param("type", "startsWith")
                    .param("include", "minimal"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    assertTrue(conceptList.get(0).getName().equalsIgnoreCase("blood"));

    log.info("Testing url - " + url + "?include=minimal&term=cold&type=startsWith");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "cold")
                    .param("type", "startsWith")
                    .param("include", "minimal"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    assertTrue(conceptList.get(0).getName().equalsIgnoreCase("cold"));

    log.info("Testing url - " + url + "?include=minimal&term=bone&type=startsWith");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "cold")
                    .param("type", "startsWith")
                    .param("include", "minimal"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    assertTrue(conceptList.get(0).getName().equalsIgnoreCase("cold"));

    log.info("Testing url - " + url + "?include=minimal&term=malignant%20neoplasm&type=startsWith");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "malignant neoplasm")
                    .param("type", "startsWith")
                    .param("pageSize", "100")
                    .param("fromRecord", "0")
                    .param("include", "minimal"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    for (Concept conc : conceptList) {
      log.info(conc.getName());
    }
    assertTrue(conceptList.get(0).getName().equalsIgnoreCase("malignant neoplasm"));

    log.info("Testing url - " + url + "?include=minimal&term=Malignant%20Neoplasm&type=startsWith");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "Malignant Neoplasm")
                    .param("type", "startsWith")
                    .param("pageSize", "100")
                    .param("fromRecord", "0")
                    .param("include", "minimal"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    for (Concept conc : conceptList) {
      log.info(conc.getName());
    }
    assertTrue(conceptList.get(0).getName().equalsIgnoreCase("malignant neoplasm"));
  }

  /**
   * Test search AND.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchAND() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // phrase term check
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties&pageSize=100&term=malignant%20bone%20neoplasm&type=AND");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "malignant bone neoplasm")
                    .param("pageSize", "100")
                    .param("type", "AND")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    List<Concept> conceptList = list.getConcepts();
    for (Concept concept : conceptList) {
      assertTrue(
          (concept.getName().toLowerCase().contains("malignant")
                  && concept.getName().toLowerCase().contains("bone")
                  && concept.getName().toLowerCase().contains("neoplasm"))
              || !concept.getSynonyms().stream()
                  .filter(p -> p.getName().toLowerCase().contains("malignant"))
                  .filter(p -> p.getName().toLowerCase().contains("bone"))
                  .filter(p -> p.getName().toLowerCase().contains("neoplasm"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getProperties().stream()
                  .filter(p -> p.getValue().toLowerCase().contains("malignant"))
                  .filter(p -> p.getValue().toLowerCase().contains("bone"))
                  .filter(p -> p.getValue().toLowerCase().contains("neoplasm"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getDefinitions().stream()
                  .filter(p -> p.getDefinition().toLowerCase().contains("malignant"))
                  .filter(p -> p.getDefinition().toLowerCase().contains("bone"))
                  .filter(p -> p.getDefinition().toLowerCase().contains("neoplasm"))
                  .collect(Collectors.toList())
                  .isEmpty());
    }
  }

  /**
   * Test search OR.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchOR() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Valid test
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=braf&type=OR");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "braf")
                    .param("pageSize", "100")
                    .param("type", "OR")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    List<Concept> conceptList = list.getConcepts();
    for (Concept concept : conceptList) {
      assertTrue(
          (concept.getName().toLowerCase().contains("braf"))
              || !concept.getSynonyms().stream()
                  .filter(p -> p.getName().toLowerCase().contains("braf"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getProperties().stream()
                  .filter(p -> p.getValue().toLowerCase().contains("braf"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getDefinitions().stream()
                  .filter(p -> p.getDefinition().toLowerCase().contains("braf"))
                  .collect(Collectors.toList())
                  .isEmpty());
    }

    // phrase term check
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=malignant%20bone%20neoplasm&type=OR");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "malignant bone neoplasm")
                    .param("pageSize", "100")
                    .param("type", "OR")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    for (Concept concept : conceptList) {
      boolean found = false;
      if (concept.getName().toLowerCase().contains("malignant")
          || concept.getName().toLowerCase().contains("bone")
          || concept.getName().toLowerCase().contains("neoplasm")) continue;
      for (Synonym syn : concept.getSynonyms()) {
        if (syn.getName().toLowerCase().contains("malignant")
            || syn.getName().toLowerCase().contains("bone")
            || syn.getName().toLowerCase().contains("neoplasm")) {
          found = true;
        }
      }
      if (found) continue;
      for (Property prop : concept.getProperties()) {
        if (prop.getValue().toLowerCase().contains("malignant")
            || prop.getValue().toLowerCase().contains("bone")
            || prop.getValue().toLowerCase().contains("neoplasm")) {
          found = true;
        }
      }
      if (found) continue;
      for (Definition def : concept.getDefinitions()) {
        if (def.getDefinition().toLowerCase().contains("malignant")
            || def.getDefinition().toLowerCase().contains("bone")
            || def.getDefinition().toLowerCase().contains("neoplasm")) {
          found = true;
        }
      }
      if (!found) assertThat(found);
    }
  }

  /**
   * Test search exact.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchExact() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Valid test
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=braf&type=match");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "braf")
                    .param("pageSize", "100")
                    .param("type", "match")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    List<Concept> conceptList = list.getConcepts();
    for (Concept concept : conceptList) {
      assertTrue(
          concept.getName().toLowerCase().equalsIgnoreCase("braf")
              || !concept.getSynonyms().stream()
                  .filter(p -> p.getName().equalsIgnoreCase("braf"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getProperties().stream()
                  .filter(p -> p.getValue().equalsIgnoreCase("braf"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getDefinitions().stream()
                  .filter(p -> p.getDefinition().equalsIgnoreCase("braf"))
                  .collect(Collectors.toList())
                  .isEmpty());
    }

    // phrase term check
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=malignant%20bone%20neoplasm&type=match");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "malignant bone neoplasm")
                    .param("pageSize", "100")
                    .param("type", "match")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    conceptList = list.getConcepts();
    for (Concept concept : conceptList) {
      assertTrue(
          concept.getName().toLowerCase().equalsIgnoreCase("malignant bone neoplasm")
              || !concept.getSynonyms().stream()
                  .filter(p -> p.getName().equalsIgnoreCase("malignant bone neoplasm"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getProperties().stream()
                  .filter(p -> p.getValue().equalsIgnoreCase("malignant bone neoplasm"))
                  .collect(Collectors.toList())
                  .isEmpty()
              || !concept.getDefinitions().stream()
                  .filter(p -> p.getDefinition().equalsIgnoreCase("malignant bone neoplasm"))
                  .collect(Collectors.toList())
                  .isEmpty());
    }
  }

  /**
   * Test search code.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchCode() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    ConceptResultList list = null;
    ConceptResultList list2 = null;
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=braf");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "braf")
                    .param("pageSize", "100")
                    .param("type", "contains")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);

    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=braf");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "braf")
                    .param("type", "contains")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    list2 =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);

    assertThat(list.getConcepts().get(0).equals(list2.getConcepts().get(0))); // make
    // sure
    // code
    // matches
    // name
    // for
    // first
    // result
    // at
    // least
  }

  /**
   * Test search fuzzy.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchFuzzy() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    ConceptResultList list = null;
    ConceptResultList list2 = null;
    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=braf&type=fuzzy");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "braf")
                    .param("pageSize", "100")
                    .param("type", "contains")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);

    log.info(
        "Testing url - "
            + url
            + "?fromRecord=0&include=synonyms,properties,definitions&pageSize=100&term=braf");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "braf")
                    .param("pageSize", "100")
                    .param("type", "contains")
                    .param("include", "synonyms,properties,definitions"))
            .andExpect(status().isOk())
            .andReturn();

    list2 =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);

    assertThat(list.getTotal() > list2.getTotal()); // should be more in
    // fuzzy
    // search
  }

  /**
   * Test search by concept code.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByConceptCode() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit&term=C3224");

    // Test a basic term search
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "C3224")
                    .param("include", "highlights"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    ConceptResultList list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getTerminology()).isEqualTo("ncit");
    assertThat(list.getConcepts().get(0).getHighlight()).isNotEmpty();

    // Test fuzzy
    result =
        this.mvc
            .perform(
                get(url).param("terminology", "ncit").param("term", "C3224").param("type", "fuzzy"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(1);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getTerminology()).isEqualTo("ncit");

    // Test startsWith
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "C3224")
                    .param("type", "startsWith"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getTerminology()).isEqualTo("ncit");

    // Test exact
    result =
        this.mvc
            .perform(
                get(url).param("terminology", "ncit").param("term", "C3224").param("type", "exact"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getTerminology()).isEqualTo("ncit");

    // Test contains
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "C3224")
                    .param("type", "contains"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getTerminology()).isEqualTo("ncit");

    // Test AND
    result =
        this.mvc
            .perform(
                get(url).param("terminology", "ncit").param("term", "C3224").param("type", "AND"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getTerminology()).isEqualTo("ncit");

    // Test OR
    result =
        this.mvc
            .perform(
                get(url).param("terminology", "ncit").param("term", "C3224").param("type", "OR"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getTerminology()).isEqualTo("ncit");

    // Test phrase
    result =
        this.mvc
            .perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "C3224")
                    .param("type", "phrase"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getTerminology()).isEqualTo("ncit");
  }

  /**
   * Test search blank.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchBlank() throws Exception {
    String url = baseUrlNoTerm;
    MvcResult result = null;
    ConceptResultList list = null;

    // no params (should return all concepts)
    log.info("Testing url - " + url + "/ncit/search");
    result = mvc.perform(get(url + "/ncit/search")).andExpect(status().isOk()).andReturn();
    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();

    // search by synonymSource
    log.info("Testing url - " + url + "/ncit/search" + "?synonymSource=GDC&terminology=ncit");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("synonymSource", "GDC")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();
    for (final Concept conc : list.getConcepts()) { // test that have match
      // to
      // synonymSource = GDC
      boolean found = false;
      for (Synonym syn : conc.getSynonyms()) {
        if (syn.getSource() != null && syn.getSource().equals("GDC")) {
          found = true;
          break;
        }
      }
      assertThat(found).isTrue();
    }

    // search by synonymSource + synonymTermType
    log.info("Testing url - " + url + "/ncit/search" + "?synonymSource=GDC&terminology=ncit");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("synonymSource", "GDC")
                    .param("synonymTermType", "SY")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();
    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();
    for (final Concept conc : list.getConcepts()) { // test that have match
      // to
      // synonymSource = GDC
      boolean foundBoth = false;
      for (Synonym syn : conc.getSynonyms()) {
        if (syn.getSource() != null
            && syn.getSource().equals("GDC")
            && syn.getTermType() != null
            && syn.getTermType().equals("SY")) {
          foundBoth = true;
          break;
        }
      }
      assertThat(foundBoth).isTrue();
    }

    // search by concept status
    log.info("Testing url - " + url + "/ncit/search" + "?conceptStatus=Obsolete_Concept");
    result =
        mvc.perform(get(url + "/ncit/search").param("conceptStatus", "Obsolete_Concept"))
            .andExpect(status().isOk())
            .andReturn();
    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();

    // search by definition source
    log.info("Testing url - " + url + "?definitionSource=CDISC&include=definitions");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("definitionSource", "CDISC")
                    .param("include", "definitions"))
            .andExpect(status().isOk())
            .andReturn();
    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();
    for (final Concept conc : list.getConcepts()) { // test that have match
      // to
      // synonymSource = GDC
      boolean found = false;
      for (Definition def : conc.getDefinitions()) {
        if (def.getSource() != null && def.getSource().equals("CDISC")) {
          found = true;
          break;
        }
      }
      assertThat(found).isTrue();
    }

    // search by property
    log.info("Testing url - " + url + "?property=FDA_UNII_Code");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("property", "FDA_UNII_Code")
                    .param("include", "properties"))
            .andExpect(status().isOk())
            .andReturn();
    String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();
    // test that have match to synonymSource = GDC
    for (final Concept conc : list.getConcepts()) {
      boolean found = false;
      for (Property prop : conc.getProperties()) {
        if (prop.getType() != null && prop.getType().equals("FDA_UNII_Code")) {
          found = true;
          break;
        }
      }
      assertThat(found).isTrue();
    }
    // NOT all should have a name containing "Toluene" (see testSearchProperty)
    assertThat(
            list.getConcepts().stream()
                .filter(
                    c ->
                        (c.getName().toLowerCase().contains("toluene")
                            || c.getSynonyms().stream()
                                    .filter(s -> s.getName().toLowerCase().contains("toluene"))
                                    .count()
                                > 0))
                .count())
        .isNotEqualTo(list.getConcepts().size());
  }

  /**
   * Test bad search term.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBadSearchTerm() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    ConceptResultList list = null;

    for (final String type :
        new String[] {"contains", "fuzzy", "phrase", "match", "startsWith", "AND", "OR"}) {
      // Expect 0 results, no error
      log.info("Testing url - " + url + "?term=C)%26ghd&type=" + type);
      result =
          mvc.perform(
                  get(url)
                      .param("terminology", "ncit")
                      .param("term", "C)%26ghd")
                      .param("type", type))
              .andExpect(status().isOk())
              .andReturn();
      String content = result.getResponse().getContentAsString();
      log.info("  content = " + content);
      assertThat(content).isNotNull();
      list = new ObjectMapper().readValue(content, ConceptResultList.class);
      if (type.equals("contains") || type.equals("OR") || type.equals("fuzzy")) {
        assertThat(list.getConcepts()).isNotEmpty();
      } else {
        assertThat(list.getConcepts()).isEmpty();
      }
    }

    for (final String type :
        new String[] {"contains", "fuzzy", "phrase", "match", "startsWith", "AND", "OR"}) {
      // Expect 0 results, no error
      log.info("Testing url - " + url + "?term=C)%26ghd+melanoma&type=" + type);
      result =
          mvc.perform(
                  get(url)
                      .param("terminology", "ncit")
                      .param("term", "C)%26ghd melanoma")
                      .param("type", type))
              .andExpect(status().isOk())
              .andReturn();
      String content = result.getResponse().getContentAsString();
      log.info("  content = " + content);
      assertThat(content).isNotNull();
      list = new ObjectMapper().readValue(content, ConceptResultList.class);
      // if (type.equals("contains") || type.equals("OR")) {
      // assertThat(list.getConcepts()).isNotEmpty();
      // } else {
      // assertThat(list.getConcepts()).isEmpty();
      // }

    }
  }

  /**
   * Test search of subset only.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchBySubsetOnly() throws Exception {
    String url = baseUrlNoTerm;
    MvcResult result = null;
    ConceptResultList list = null;
    log.info(
        "Testing url - "
            + url
            + "/ncit/search"
            + "?include=associations&subset=C167405&terminology=ncit");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("subset", "C167405")
                    .param("include", "associations"))
            .andExpect(status().isOk())
            .andReturn();
    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();
    boolean found = false;
    for (Concept conc : list.getConcepts()) {
      found = false;
      for (Association assoc : conc.getAssociations()) {
        if (assoc.getRelatedCode().equals("C167405")) {
          found = true;
          break;
        }
      }
      assertThat(found).isTrue();
    }

    log.info(
        "Testing url - "
            + url
            + "/ncit/search"
            + "?include=associations&subset=*&terminology=ncit");
    result =
        mvc.perform(get(url + "/ncit/search").param("subset", "*").param("include", "associations"))
            .andExpect(status().isOk())
            .andReturn();
    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();
  }

  /**
   * Test that search deboosts retired concepts.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchDeboostRetired() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    ConceptResultList list = null;
    log.info("Testing url - " + url + "?term=grey&terminology=ncit&include=properties");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "grey")
                    .param("include", "properties"))
            .andExpect(status().isOk())
            .andReturn();
    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    log.info("  list = " + list);
    assertThat(list.getConcepts() != null && list.getConcepts().size() > 0).isTrue();
    boolean found = false;
    for (Concept conc : list.getConcepts()) {
      for (Property prop : conc.getProperties()) {
        if (prop.getValue() != null && prop.getValue().equals("Retired_Concept")) {
          found = true;
          break;
        }
      }

      assertThat(found).isFalse();
    }
  }

  /**
   * Test search retired concepts still returns the retired concept. Ensure our deboost logic isn't
   * affecting the search results.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchRetiredConcepts() throws Exception {
    // SETUP
    String url = baseUrl;
    MvcResult result = null;
    ConceptResultList list = null;
    log.info("Testing url - " + url + "?term=Jax Grey Lethal Mutn&terminology=ncit");

    // ACT & ASSERT
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "Jax Grey Lethal Mutn")
                    .param("type", "match"))
            .andExpect(status().isOk())
            .andReturn();
    list =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    log.info("  list = " + list);
    assertThat(list.getConcepts() != null && !list.getConcepts().isEmpty()).isTrue();
    assertThat(list.getConcepts().get(0).getConceptStatus()).isEqualTo("Retired_Concept");
  }

  /**
   * Test ncit browser match.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBrowserMatch() throws Exception {

    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Browser match on bone cancer (ncit)
    log.info("Testing url - " + url + "?terminology=ncit&term=bone+cancer&type=contains");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "bone cancer")
                    .param("type", "contains"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    // test first result is Malignant Bone Neoplasm
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Malignant Bone Neoplasm");

    // Browser match on bone cancer (ncim)
    log.info("Testing url - " + url + "?terminology=ncim&term=digestive+cancer&type=contains");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncim")
                    .param("term", "digestive cancer")
                    .param("type", "contains"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    // test first result is Malignant Digestive System Neoplasm
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C0685938");
  }

  /**
   * Test search quality.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchQuality() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Search for "mg/dL", the first result should be C67015
    log.info(
        "Testing url - " + url + "?terminology=ncit&term=mg%2Fdl&type=contains&include=minimal");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "mg/dL")
                    .param("type", "contains"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts().size()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C67015");

    // Search for "double lymphoma" - everything with "double" and "lymphoma"
    // should come
    // before anything that doesn't have both.
    log.info(
        "Testing url - "
            + url
            + "?terminology=ncit&pageSize=100&term=double+lymphoma&type=contains&include=synonyms");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("pageSize", "100")
                    .param("term", "double lymphoma")
                    .param("type", "contains")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    boolean currentExact = true;
    boolean foundC125904 = false;
    int ct = 0;
    for (Concept concept : list.getConcepts()) {
      // found match
      if ((concept.getName().toLowerCase().contains("double")
              && concept.getName().toLowerCase().contains("lymphoma"))
          || !concept.getSynonyms().stream()
              .filter(
                  s ->
                      s.getName().toLowerCase().contains("double")
                          && s.getName().toLowerCase().contains("lymphoma"))
              .collect(Collectors.toList())
              .isEmpty()) {
        // check still in front
        if (!currentExact)
          // exact matches not inorder
          fail("Matches with both 'double' and 'lymphoma' not before others");
      } else {
        // should be at end of exact matches
        currentExact = false;
      }

      // also verify these two concepts are in the top 5 results
      // C125904 double hit lymphoma
      if (concept.getCode().equals("C125904") && ct < 5) {
        foundC125904 = true;
      }
    }

    // Verify C125904 in top 5 results
    if (!foundC125904) {
      fail("C125904 (double hit lymphoma) not found in first 5 results");
    }

    // Search for "breast cancer" - things with "breast cancer" together should
    // show up before separated
    log.info(
        "Testing url - "
            + url
            + "?terminology=ncit&pageSize=100&term=breast+cancer&type=contains&include=synonyms");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("pageSize", "100")
                    .param("term", "breast cancer")
                    .param("type", "contains")
                    .param("include", "synonyms"))
            .andExpect(status().isOk())
            .andReturn();

    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    currentExact = true;
    ct = 0;
    boolean foundC12959 = false;
    boolean foundC142819 = false;
    for (Concept concept : list.getConcepts()) {
      // found match
      if ((concept.getName().toLowerCase().contains("breast cancer"))
          || !concept.getSynonyms().stream()
              .filter(s -> s.getName().toLowerCase().contains("breast cancer"))
              .collect(Collectors.toList())
              .isEmpty()) {
        // check still in front
        if (!currentExact)
          // exact matches not inorder
          fail("Matches with both 'breast cancer' not before others");
      } else {
        // should be at end of exact matches
        currentExact = false;
      }
      ct++;

      // also verify these two concepts are in the top 5 results
      // C12959 Breast Cancer Cell Breast Cancer Cell
      if (concept.getCode().equals("C12959") && ct < 5) {
        foundC12959 = true;
      }
      // C142819 Breast Cancer Locator
      if (concept.getCode().equals("C142819") && ct < 5) {
        foundC142819 = true;
      }
    }

    if (!foundC12959) {
      fail("C12959 (Breast Cancer Cell Breast Cancer Cell) not found in first 5 results");
    }
    if (!foundC142819) {
      fail("C142819 (Breast Cancer Locator) not found in first 5 results");
    }
  }

  /**
   * Test search weekly.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchWeekly() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // Basic search using the weekly version of NCIt
    log.info("Testing url - " + url + "?terminology=ncit_21.07a&term=cancer");
    result =
        mvc.perform(get(url).param("terminology", "ncit_21.07a").param("term", "cancer"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal()).isGreaterThan(0);
  }

  /**
   * Test search with sort.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchWithSort() throws Exception {
    String url = baseUrlNoTerm;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;
    List<String> values = null;
    List<String> sortedValues = null;

    // Sort members of C128784 ascending by code
    log.info("Testing url - " + url + "/ncit/search" + "?subset=C128784&sort=code&ascending=true");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("subset", "C128784")
                    .param("sort", "code")
                    .param("ascending", "true"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    values = list.getConcepts().stream().map(c -> c.getCode()).collect(Collectors.toList());
    sortedValues =
        list.getConcepts().stream().map(c -> c.getCode()).sorted().collect(Collectors.toList());
    assertThat(values).isEqualTo(sortedValues);

    // Sort members of C128784 descending by code
    log.info("Testing url - " + url + "/ncit/search" + "?subset=C128784&sort=code&ascending=false");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("subset", "C128784")
                    .param("sort", "code")
                    .param("ascending", "false"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    values = list.getConcepts().stream().map(c -> c.getCode()).collect(Collectors.toList());
    sortedValues =
        list.getConcepts().stream()
            .map(c -> c.getCode())
            .sorted((a, b) -> b.toLowerCase().compareTo(a.toLowerCase()))
            .collect(Collectors.toList());
    assertThat(values).isEqualTo(sortedValues);

    // Sort members of C128784 ascending by norm name
    log.info(
        "Testing url - " + url + "/ncit/search" + "?subset=C128784&sort=normName&ascending=true");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("subset", "C128784")
                    .param("sort", "normName")
                    .param("ascending", "true"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    values = list.getConcepts().stream().map(c -> c.getName()).collect(Collectors.toList());
    sortedValues =
        list.getConcepts().stream().map(c -> c.getName()).sorted().collect(Collectors.toList());
    assertThat(values).isEqualTo(sortedValues);

    // Sort members of C128784 ascending by norm name
    log.info(
        "Testing url - " + url + "/ncit/search" + "?subset=C128784&sort=normName&ascending=false");
    result =
        mvc.perform(
                get(url + "/ncit/search")
                    .param("subset", "C128784")
                    .param("sort", "normName")
                    .param("ascending", "false"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    values = list.getConcepts().stream().map(c -> c.getName()).collect(Collectors.toList());
    sortedValues =
        list.getConcepts().stream()
            .map(c -> c.getName())
            .sorted((a, b) -> b.toLowerCase().compareTo(a.toLowerCase()))
            .collect(Collectors.toList());
    assertThat(values).isEqualTo(sortedValues);
  }

  /**
   * Test search with stemming.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchWithStemming() throws Exception {
    String url = baseUrl;
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;
    List<String> names = null;

    // check stem in partial
    log.info("Testing url - " + url + "?terminology=ncit&term=All%20Site&type=AND");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "All Site")
                    .param("type", "AND"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    names = list.getConcepts().stream().map(c -> c.getName()).collect(Collectors.toList());
    assert (names.contains("All Sites"));

    // check stem in full
    log.info("Testing url - " + url + "?terminology=ncit&term=All%20Sites&type=AND");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "All Sites")
                    .param("type", "AND"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    names = list.getConcepts().stream().map(c -> c.getName()).collect(Collectors.toList());
    assert (names.contains("All Sites"));

    // check contains
    log.info("Testing url - " + url + "?terminology=ncit&term=cancerous");
    result =
        mvc.perform(get(url).param("terminology", "ncit").param("term", "cancerous"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 1000);

    // check another contains
    log.info("Testing url - " + url + "?terminology=ncit&term=cancerous%20sites");
    result =
        mvc.perform(get(url).param("terminology", "ncit").param("term", "cancerous sites"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 5000);

    // check a third contains
    log.info("Testing url - " + url + "?terminology=ncit&term=subsets%20displays");
    result =
        mvc.perform(get(url).param("terminology", "ncit").param("term", "subsets displays"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 100);

    // check match
    log.info("Testing url - " + url + "?terminology=ncit&term=connecting%20tissue&type=match");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "connecting tissue")
                    .param("type", "match"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal().equals(0L));

    // check startsWith
    log.info("Testing url - " + url + "?terminology=ncit&term=connecting%20tissue&type=startsWith");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "connecting tissue")
                    .param("type", "startsWith"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal().equals(0L));

    // check phrase
    log.info("Testing url - " + url + "?terminology=ncit&term=connecting%20tissue&type=phrase");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "connecting tissue")
                    .param("type", "phrase"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal().equals(0L));

    // check AND
    log.info("Testing url - " + url + "?terminology=ncit&term=connecting%20tissue&type=AND");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "connecting tissue")
                    .param("type", "AND"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 50);

    // check another AND
    log.info("Testing url - " + url + "?terminology=ncit&term=polyp%%20%site&type=AND");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "polyp site")
                    .param("type", "AND"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 0);

    // check a third AND
    log.info("Testing url - " + url + "?terminology=ncit&term=subsets%20terminology&type=AND");
    result =
        mvc.perform(
                get(url)
                    .param("terminology", "ncit")
                    .param("term", "subsets terminology")
                    .param("type", "AND"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 0);
  }

  /**
   * Test search with sparql.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchWithSparql() throws Exception {
    String url = "/api/v1/concept/ncit/search/";
    MvcResult result = null;
    String content = null;
    ConceptResultList list = null;

    // check basic query
    String query =
        "SELECT ?code\n"
            + "{ GRAPH <http://NCI_T_monthly> \n"
            + "  { \n"
            + "    ?x a owl:Class . \n"
            + "    ?x :NHC0 ?code .\n"
            + "    ?x :P108 \"Melanoma\"\n"
            + "  } \n"
            + "}";

    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");

    // check basic query with pre-formed prefix
    query =
        "PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n"
            + "PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n"
            + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX dc:<http://purl.org/dc/elements/1.1/>\n"
            + "PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>\n"
            + "PREFIX xml:<http://www.w3.org/2001/XMLSchema#>\n"
            + "SELECT ?code\n"
            + "{ GRAPH <http://NCI_T_monthly>\n"
            + "{\n"
            + "  ?x a owl:Class .\n"
            + "  ?x :NHC0 ?code .\n"
            + "  ?x :P108 \"Melanoma\"\n"
            + "}\n"
            + "}";

    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");

    // check query with malformed graph
    query =
        "SELECT ?code\n"
            + "{ GRAPH <http://blablablabla> \n"
            + "  { \n"
            + "    ?x a owl:Class . \n"
            + "    ?x :NHC0 ?code .\n"
            + "    ?x :P108 \"Melanoma\"\n"
            + "  } \n"
            + "}";

    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");

    // check another valid query
    query =
        "SELECT ?code\n"
            + "{ GRAPH <http://NCI_T_monthly> \n"
            + "  { \n"
            + "    ?x a owl:Class . \n"
            + "    ?x :NHC0 ?code .\n"
            + "    ?x :P108 \"Melanoma Pathway\"\n"
            + "  } \n"
            + "}";
    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C91477");

    // check a query with a term
    query =
        "SELECT ?code {\n"
            + "  GRAPH <http://NCI_T_monthly> {\n"
            + "    ?x a owl:Class .\n"
            + "    ?x :NHC0 ?code .\n"
            + "    ?x :P108 ?label .\n"
            + "    FILTER(CONTAINS(?label, \"Melanoma\"))\n"
            + "  }\n"
            + "}";
    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal&term=Theraccine");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains")
                    .param("term", "Theraccine"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal()).isGreaterThan(0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C1830");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma Theraccine");
    assertThat(list.getParameters().getCodeList().contains("C1830"));

    // check another query with a term
    query =
        "SELECT ?code {\n"
            + "  GRAPH <http://NCI_T_monthly> {\n"
            + "    ?x a owl:Class .\n"
            + "    ?x :NHC0 ?code .\n"
            + "    ?x :P108 ?label .\n"
            + "    FILTER(CONTAINS(?label, \"Cancer\"))\n"
            + "  }\n"
            + "}";
    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal&term=Liver");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains")
                    .param("term", "Liver"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getTotal()).isGreaterThan(0);
    for (Concept conc : list.getConcepts()) {
      Boolean name = conc.getName().toLowerCase().contains("liver");
      Boolean definitions =
          conc.getDefinitions().stream()
              .anyMatch(definition -> definition.getDefinition().toLowerCase().contains("liver"));
      Boolean synonyms =
          conc.getSynonyms().stream()
              .anyMatch(synonym -> synonym.getName().toLowerCase().contains("liver"));
      log.info(conc.getCode() + " " + conc.getName());
      assertThat(name || definitions || synonyms).isTrue();
      assertThat(list.getParameters().getCodeList().contains(conc.getCode()));
    }

    // check non-existent term
    query =
        "SELECT ?code\n"
            + "{ GRAPH <http://NCI_T_monthly> \n"
            + "  { \n"
            + "    ?x a owl:Class . \n"
            + "    ?x :NHC0 ?code .\n"
            + "    ?x :P108 \"ZZZZZ\"\n"
            + "  } \n"
            + "}";
    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains"))
            .andExpect(status().isBadRequest())
            .andReturn();

    // check query with malformed prefix
    query =
        "PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n"
            + "PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n"
            + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX dc:<http://purl.org/dc/elements/1.1/>\n"
            + "PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>\n"
            + "PREFIX xml:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX xml:<http://www.w3.org/2001/XMLSchema#>\n"
            + "SELECT ?code\n"
            + "{ GRAPH <http://NCI_T_monthly>\n"
            + "{\n"
            + "  ?x a owl:Class .\n"
            + "  ?x :NHC0 ?code .\n"
            + "  ?x :P108 \"Melanoma\"\n"
            + "}\n"
            + "}";
    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains"))
            .andExpect(status().isBadRequest())
            .andReturn();
    assertThat(result.getResponse().getErrorMessage()).isNotNull();
    content = result.getResponse().getErrorMessage();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
    assertThat(
            content.contains("Invalid SPARQL query: Multiple prefix declarations for prefix 'xml'"))
        .isTrue();

    // check query with a query that fails initial validation
    query =
        "PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n"
            + "PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n"
            + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX dc:<http://purl.org/dc/elements/1.1/>\n"
            + "PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>\n"
            + " xml:<http://www.w3.org/2001/XMLSchema#>\n"
            + "SELECT ?code\n"
            + "{ GRAPH <http://NCI_T_monthly>\n"
            + "{\n"
            + "  ?x a owl:Class .\n"
            + "  ?x :NHC0 ?code .\n"
            + "  ?x :P108 \"Melanoma\"\n"
            + "}\n"
            + "}";
    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal");

    final String exceptionUrl = new String(url);
    final String exceptionQuery = new String(query);
    assertThrows(
        AssertionError.class,
        () -> {
          MvcResult resultException =
              mvc.perform(
                      MockMvcRequestBuilders.post(exceptionUrl)
                          .param("query", exceptionQuery)
                          .param("include", "minimal")
                          .param("type", "contains"))
                  .andExpect(status().is5xxServerError())
                  .andReturn();
          assertThat(resultException.getResponse().getErrorMessage()).isNotNull();
          String contentException = resultException.getResponse().getErrorMessage();
          log.info("  content = " + contentException);
          assertThat(contentException).isNotNull();
          assertThat(contentException.contains("SPARQL query failed validation:")).isTrue();
        });

    url = "/api/v1/concept/umlssemnet/search/";
    // check a valid query in another terminology (with malformed graph)
    query =
        "SELECT ?code\n"
            + "{ GRAPH <http://blablabla> \n"
            + "  { \n"
            + "    ?x a owl:Class . \n"
            + "    ?x :Code ?code .\n"
            + "    ?x :Preferred_Name \"Behavior\"\n"
            + "  } \n"
            + "}";
    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assert (list.getTotal() > 0);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("T053");

    // check query that takes too long
    query =
        "PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#> \n"
            + "PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>\n"
            + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX dc:<http://purl.org/dc/elements/1.1/>\n"
            + "PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>\n"
            + "PREFIX xml:<http://www.w3.org/2001/XMLSchema#>\n"
            + "SELECT DISTINCT ?code\n"
            + "{ GRAPH <#{namedGraph}> \n"
            + "  { \n"
            + "      ?relatedConcept a owl:Class .\n"
            + "      ?relatedConcept (owl:equivalentClass|rdfs:subClassOf) ?z .\n"
            + "      ?z (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/\n"
            + "        ( owl:unionOf/rdf:rest*/rdf:first |\n"
            + "         "
            + " (owl:unionOf|owl:intersectionOf)/rdf:rest*/rdf:first/owl:unionOf/rdf:rest*/rdf:first"
            + " \n"
            + "        ) ?rs . \n"
            + "      ?rs a owl:Restriction . \n"
            + "      ?rs owl:onProperty ?relationship . \n"
            + "      ?rs owl:someValuesFrom ?x_concept . \n"
            + "      ?x_concept a owl:Class . \n"
            + "      ?x_concept :NHC0 ?code . \n"
            + "      ?relatedConcept :NHC0 ?relatedConceptCode\n"
            + "  } \n"
            + "}";
    log.info(
        "Testing url - "
            + url
            + "?query="
            + query
            + "&terminology=ncit&type=contains&include=minimal");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("include", "minimal")
                    .param("type", "contains"))
            .andExpect(status().isBadRequest())
            .andReturn();
    assertThat(result.getResponse().getErrorMessage()).isNotNull();
    content = result.getResponse().getErrorMessage();
    log.info("  content = " + content);
    assertThat(content).isNotNull();
  }

  /**
   * Test sparql.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSparql() throws Exception {
    String url = "/api/v1/sparql/ncit";
    MvcResult result = null;
    String content = null;
    final ObjectMapper mapper = new ObjectMapper();
    String query =
        "SELECT ?code ?x { GRAPH <http://NCI_T_monthly> { ?x a owl:Class . ?x :NHC0 ?code . } }";
    log.info(
        "Testing url - " + url + "?query=" + query + "&terminology=ncit&fromRecord=0&pageSize=10");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("fromRecord", "0")
                    .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andReturn();
    MapResultList results =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), MapResultList.class);
    assertThat(results.getParameters().getPageSize()).isEqualTo(10);
    assertThat(results.getResults().size()).isEqualTo(results.getParameters().getPageSize());
    Map<String, String> node = results.getResults().get(1);

    // verify fromRecord and pageSize
    query =
        "SELECT ?code ?x { GRAPH <http://NCI_T_monthly> { ?x a owl:Class . ?x :NHC0 ?code . } }";
    log.info(
        "Testing url - " + url + "?query=" + query + "&terminology=ncit&fromRecord=1&pageSize=5");
    result =
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .param("query", query)
                    .param("fromRecord", "1")
                    .param("pageSize", "5"))
            .andExpect(status().isOk())
            .andReturn();
    results =
        new ObjectMapper()
            .readValue(result.getResponse().getContentAsString(), MapResultList.class);
    assertThat(results.getParameters().getPageSize()).isEqualTo(5);
    assertThat(results.getResults().size()).isEqualTo(results.getParameters().getPageSize());
    assertThat(results.getResults().get(0)).isEqualTo(node);
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
