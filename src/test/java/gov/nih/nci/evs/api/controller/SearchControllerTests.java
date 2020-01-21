
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
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * The Class SearchControllerTests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SearchControllerTests {

  /** The logger. */
  private static final Logger log =
      LoggerFactory.getLogger(SearchControllerTests.class);

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
    result = this.mvc
        .perform(
            get(url).param("terminology", "ncit").param("term", "melanoma"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotNull();

    ConceptResultList list =
        new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(10);
    assertThat(list.getConcepts().get(0).getCode()).isEqualTo("C3224");
    assertThat(list.getConcepts().get(0).getName()).isEqualTo("Melanoma");
    assertThat(list.getConcepts().get(0).getSynonyms()).isEmpty();
    assertThat(list.getTotal()).isGreaterThan(100);
    assertThat(list.getParameters().getTerm()).isEqualTo("melanoma");
    assertThat(list.getParameters().getType()).isEqualTo("contains");
    assertThat(list.getParameters().getFromRecord()).isEqualTo(0);
    assertThat(list.getParameters().getPageSize()).isEqualTo(10);

    // Test a basic term search with single terminology form
    url = "/api/v1/concept/ncit/search";
    log.info("Testing url - " + url + "?terminology=ncit&term=melanoma");
    result = this.mvc.perform(get(url).param("term", "melanoma"))
        .andExpect(status().isOk()).andReturn();
    final String content2 = result.getResponse().getContentAsString();
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
    mvc.perform(
        get(url).param("terminology", "ncitXXX").param("term", "melanoma"))
        .andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Missing "term" parameter
    url = baseUrl;
    log.info("Testing url - " + url + "?terminology=ncit");
    mvc.perform(get(url).param("terminology", "ncit"))
        .andExpect(status().isBadRequest()).andReturn();
    // content is blank because of MockMvc

    // Invalid "include" parameter
    url = baseUrl;
    log.info(
        "Testing url - " + url + "?terminology=ncit&term=melanoma&include=XXX");
    mvc.perform(get(url).param("terminology", "ncit").param("term", "melanoma")
        .param("include", "XXX")).andExpect(status().isInternalServerError())
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
    log.info("Testing url - " + url
        + "?terminology=ncit&term=melanoma&include=synonyms");

    // Test a basic term search
    result = this.mvc
        .perform(get(url).param("terminology", "ncit").param("term", "melanoma")
            .param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotNull();

    ConceptResultList list =
        new ObjectMapper().readValue(content, ConceptResultList.class);
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
    log.info("Testing url - " + url
        + "?terminology=ncit&term=melanoma&include=synonyms");

    result = this.mvc
        .perform(get(url).param("terminology", "ncit").param("term", "melanoma")
            .param("include", "synonyms"))
        .andExpect(status().isOk()).andReturn();
    final String content2 = result.getResponse().getContentAsString();
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
    log.info(
        "Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=2");

    // Test a basic term search
    result =
        this.mvc
            .perform(get(url).param("terminology", "ncit")
                .param("term", "melanoma").param("pageSize", "2"))
            .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(2);
    // From 5 with page size of 9 (should match the last 9 records of
    // pageSize of 14
    url = baseUrl;
    log.info("Testing url - " + url
        + "?terminology=ncit&term=melanoma&pageSize=9&fromRecord=5");

    // Test a basic term search
    result = this.mvc
        .perform(get(url).param("terminology", "ncit").param("term", "melanoma")
            .param("pageSize", "9").param("fromRecord", "5"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(9);
    final List<Concept> cl1 = list.getConcepts();
    log.info(
        "Testing url - " + url + "?terminology=ncit&term=melanoma&pageSize=14");

    // Test a basic term search
    result =
        this.mvc
            .perform(get(url).param("terminology", "ncit")
                .param("term", "melanoma").param("pageSize", "14"))
            .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotNull();

    list = new ObjectMapper().readValue(content, ConceptResultList.class);
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isEqualTo(14);
    assertThat(list.getConcepts().subList(5, 14).toString())
        .isEqualTo(cl1.toString());
  }
  //
  // @Test
  // public void getSearchFormatClean() throws Exception {
  //
  //
  // String url = baseUrl;
  // log.info("Testing url - " + url);
  //
  //
  // MvcResult result =
  // mvc.perform(get(url).param("term","carcinoma").param("format","clean"))
  // .andExpect(status().isOk())
  // .andReturn();
  // String content = result.getResponse().getContentAsString();
  //
  // assertThat(content).isNotNull();
  //
  // JsonNode jsonNode = this.objectMapper.readTree(content);
  // JsonNode jsonNodeTotal = jsonNode.path("total");
  // assertThat(jsonNodeTotal.intValue() > 0).isTrue();
  // JsonNode jsonNodeSize = jsonNode.path("size");
  // assertThat(jsonNodeSize.intValue() > 0).isTrue();
  //
  // JsonNode jsonNodeHits = jsonNode.path("hits");
  // Iterator<JsonNode> iterator = jsonNodeHits.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode node = iterator.next();
  // node = node.path("Label");
  // log.info(node.textValue());
  // assertThat(node.textValue()).isEqualToIgnoringCase("Carcinoma");
  //
  //
  //
  //
  // log.info("Done Testing getSearchFormatClean ");
  //
  // }
  //
  //
  // @Test
  // public void getSearchTypeMatch() throws Exception {
  //
  //
  // String url = baseUrl;
  // log.info("Testing url - " + url);
  //
  //
  // MvcResult result = mvc.perform(get(url).param("term","Lung
  // Carcinoma").param("format","clean").param("type", "match"))
  // .andExpect(status().isOk())
  // .andReturn();
  // String content = result.getResponse().getContentAsString();
  //
  // assertThat(content).isNotNull();
  //
  // JsonNode jsonNode = this.objectMapper.readTree(content);
  // JsonNode jsonNodeTotal = jsonNode.path("total");
  // assertThat(jsonNodeTotal.intValue() > 0).isTrue();
  // JsonNode jsonNodeSize = jsonNode.path("size");
  // assertThat(jsonNodeSize.intValue() > 0).isTrue();
  //
  // JsonNode jsonNodeHits = jsonNode.path("hits");
  // Iterator<JsonNode> iterator = jsonNodeHits.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode node = iterator.next();
  // node = node.path("Label");
  // log.info(node.textValue());
  // assertThat(node.textValue()).isEqualToIgnoringCase("Lung Carcinoma");
  //
  //
  //
  //
  // log.info("Done Testing getSearchTypeMatch ");
  //
  // }
  //
  // @Test
  // public void getSearchTypeFuzzy() throws Exception {
  //
  //
  // String url = baseUrl;
  // log.info("Testing url - " + url);
  //
  //
  // MvcResult result =
  // mvc.perform(get(url).param("term","enzymi").param("format","clean").param("type",
  // "fuzzy"))
  // .andExpect(status().isOk())
  // .andReturn();
  // String content = result.getResponse().getContentAsString();
  //
  // assertThat(content).isNotNull();
  //
  // JsonNode jsonNode = this.objectMapper.readTree(content);
  // JsonNode jsonNodeTotal = jsonNode.path("total");
  // assertThat(jsonNodeTotal.intValue() > 0).isTrue();
  // JsonNode jsonNodeSize = jsonNode.path("size");
  // assertThat(jsonNodeSize.intValue() > 0).isTrue();
  //
  // JsonNode jsonNodeHits = jsonNode.path("hits");
  // Iterator<JsonNode> iterator = jsonNodeHits.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode node = iterator.next();
  // node = node.path("Label");
  // log.info(node.textValue());
  // assertThat(node.textValue()).isEqualToIgnoringCase("Enzyme");
  //
  //
  //
  //
  // log.info("Done Testing getSearchTypeFuzzy ");
  //
  // }
  //
  // @Test
  // public void getSearchProperty() throws Exception {
  //
  //
  // String url = baseUrl;
  // log.info("Testing url - " + url);
  //
  //
  // MvcResult result =
  // mvc.perform(get(url).param("term","XAV05295I5").param("format","cleanWithHighlights").param("property",
  // "fda_unii_code"))
  // .andExpect(status().isOk())
  // .andReturn();
  // String content = result.getResponse().getContentAsString();
  // log.info("content -" + content);
  // assertThat(content).isNotNull();
  //
  // JsonNode jsonNode = this.objectMapper.readTree(content);
  // JsonNode jsonNodeTotal = jsonNode.path("total");
  // assertThat(jsonNodeTotal.intValue() > 0).isTrue();
  // assertThat(jsonNodeTotal.intValue()).isEqualTo(1);
  //
  //
  // JsonNode jsonNodeHits = jsonNode.path("hits");
  // Iterator<JsonNode> iterator = jsonNodeHits.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode node = iterator.next();
  // JsonNode nodeLabel = node.path("Label");
  // log.info(nodeLabel.textValue());
  // assertThat(nodeLabel.textValue()).isEqualToIgnoringCase("Sivifene");
  // JsonNode nodeFDA = node.path("FDA_UNII_Code");
  // Iterator<JsonNode> iteratorFDA = nodeFDA.elements();
  // JsonNode nodeFDAValue = iteratorFDA.next();
  // assertThat(nodeFDAValue.textValue()).isEqualToIgnoringCase("XAV05295I5");
  //
  //
  //
  // log.info("Done Testing getSearchProperty ");
  //
  // }
  //
  //
  // @Test
  // public void getSearchReturnProperty() throws Exception {
  //
  //
  // String url = baseUrl;
  // log.info("Testing url - " + url);
  //
  //
  // MvcResult result =
  // mvc.perform(get(url).param("term","carcinoma").param("format","clean").param("returnProperties",
  // "roles"))
  // .andExpect(status().isOk())
  // .andReturn();
  // String content = result.getResponse().getContentAsString();
  // //log.info("content -" + content);
  // assertThat(content).isNotNull();
  //
  // JsonNode jsonNode = this.objectMapper.readTree(content);
  // JsonNode jsonNodeTotal = jsonNode.path("total");
  // assertThat(jsonNodeTotal.intValue() > 0).isTrue();
  //
  //
  //
  // JsonNode jsonNodeHits = jsonNode.path("hits");
  // Iterator<JsonNode> iterator = jsonNodeHits.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode node = iterator.next();
  //
  // JsonNode nodeRole = node.path("Role");
  // Iterator<JsonNode> iteratorRole = nodeRole.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode nodeDefintion = node.path("DEFINITION");
  // assertThat(nodeDefintion).isEmpty();
  //
  //
  //
  //
  //
  //
  // log.info("Done Testing getSearchReturnProperty ");
  //
  // }
  //
  // @Test
  // public void getSearchReturnPropertyWithDefinition() throws Exception {
  //
  //
  // String url = baseUrl;
  // log.info("Testing url - " + url);
  //
  //
  // MvcResult result =
  // mvc.perform(get(url).param("term","carcinoma").param("format","clean").param("returnProperties",
  // "roles,definition"))
  // .andExpect(status().isOk())
  // .andReturn();
  // String content = result.getResponse().getContentAsString();
  // //log.info("content -" + content);
  // assertThat(content).isNotNull();
  //
  // JsonNode jsonNode = this.objectMapper.readTree(content);
  // JsonNode jsonNodeTotal = jsonNode.path("total");
  // assertThat(jsonNodeTotal.intValue() > 0).isTrue();
  //
  //
  //
  // JsonNode jsonNodeHits = jsonNode.path("hits");
  // Iterator<JsonNode> iterator = jsonNodeHits.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode node = iterator.next();
  //
  // JsonNode nodeRole = node.path("Role");
  // Iterator<JsonNode> iteratorRole = nodeRole.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode nodeDefintion = node.path("DEFINITION");
  // assertThat(nodeDefintion).isNotEmpty();
  //
  //
  //
  //
  //
  //
  // log.info("Done Testing getSearchReturnPropertyWithDefinition ");
  //
  // }
  //
  // @Test
  // public void getSearchSynonym() throws Exception {
  //
  //
  // String url = baseUrl;
  // log.info("Testing url - " + url);
  //
  //
  // MvcResult result =
  // mvc.perform(get(url).param("term","dsDNA").param("format","clean").param("synonymSource",
  // "NCI"))
  // .andExpect(status().isOk())
  // .andReturn();
  // String content = result.getResponse().getContentAsString();
  // //log.info("content -" + content);
  // assertThat(content).isNotNull();
  //
  // JsonNode jsonNode = this.objectMapper.readTree(content);
  // JsonNode jsonNodeTotal = jsonNode.path("total");
  // assertThat(jsonNodeTotal.intValue() > 0).isTrue();
  // assertThat(jsonNodeTotal.intValue()).isEqualTo(2);
  //
  //
  // JsonNode jsonNodeHits = jsonNode.path("hits");
  // Iterator<JsonNode> iterator = jsonNodeHits.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode node = iterator.next();
  //
  // JsonNode nodeSynonym = node.path("FULL_SYN");
  // Iterator<JsonNode> iteratorSyn = nodeSynonym.elements();
  //
  // boolean found= false;
  // while (iteratorSyn.hasNext()) {
  // JsonNode syn = iteratorSyn.next();
  // JsonNode synTerm = syn.path("term-name");
  // if (synTerm.textValue().indexOf("dsDNA") > -1) {
  // JsonNode synSource = syn.path("term-source");
  // if (synSource.textValue().equalsIgnoreCase("NCI")) {
  // found = true;
  // break;
  // }
  // }
  // }
  //
  //
  // assertThat(found).isTrue();
  //
  //
  //
  //
  // log.info("Done Testing getSearchSynonym ");
  //
  // }
  //
  // @Test
  // public void getSearchFilter() throws Exception {
  //
  //
  // String url = baseUrl;
  // log.info("Testing url - " + url);
  //
  //
  // MvcResult result =
  // mvc.perform(get(url).param("term","melanoma").param("format","clean").param("conceptStatus",
  // "Retired_Concept"))
  // .andExpect(status().isOk())
  // .andReturn();
  // String content = result.getResponse().getContentAsString();
  // //log.info("content -" + content);
  // assertThat(content).isNotNull();
  //
  // JsonNode jsonNode = this.objectMapper.readTree(content);
  // JsonNode jsonNodeTotal = jsonNode.path("total");
  // assertThat(jsonNodeTotal.intValue() > 0).isTrue();
  // assertThat(jsonNodeTotal.intValue()).isEqualTo(13);
  //
  //
  // JsonNode jsonNodeHits = jsonNode.path("hits");
  // Iterator<JsonNode> iterator = jsonNodeHits.elements();
  // assertThat(iterator.hasNext()).isTrue();
  //
  // JsonNode node = iterator.next();
  //
  // JsonNode nodeStatus = node.path("Concept_Status");
  // Iterator<JsonNode> iteratorStatus = nodeStatus.elements();
  // assertThat(iteratorStatus.hasNext()).isTrue();
  //
  //
  // JsonNode nodeStatusRetired= iteratorStatus.next();
  //
  // assertThat(nodeStatusRetired.textValue().equalsIgnoreCase("Retired_Concept"));
  //
  //
  //
  //
  // log.info("Done Testing getSearchFilter ");
  //
  // }
}
