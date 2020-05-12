
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.util.EVSUtils;

/**
 * Integration tests for ContentController organized around proper handling of
 * property qualifiers. This is based on work from EVSRESTAPI-69.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class QualifierTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(QualifierTests.class);

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

  /** The meta base url. */
  private String metaBaseUrl = "";

  /**
   * Sets the up.
   */
  @Before
  public void setUp() {

    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1/concept";
    metaBaseUrl = "/api/v1/metadata";
  }

  /**
   * Test get concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAltDefinitionQualifiers() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C101669?include=definitions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C101669");
    assertThat(concept.getDefinitions().size()).isGreaterThan(0);
    assertThat(concept.getDefinitions().stream()
        .filter(d -> "ALT_DEFINITION".equals(d.getType()) && d.getQualifiers().size() > 0).count())
            .isGreaterThan(0);
    assertThat(concept.getDefinitions().stream()
        .filter(d -> "ALT_DEFINITION".equals(d.getType()) && d.getQualifiers().size() > 0)
        .flatMap(d -> d.getQualifiers().stream()).filter(q -> q.getType().equals("attribution")).count())
            .isGreaterThan(0);
  }

  /**
   * Test definition qualifiers.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDefinitionQualifiers() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C101046?include=definitions";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C101046");
    assertThat(concept.getDefinitions().size()).isGreaterThan(0);
    assertThat(concept.getDefinitions().stream()
        .filter(d -> !"ALT_DEFINITION".equals(d.getType()) && d.getQualifiers().size() > 0).count())
            .isGreaterThan(0);
    assertThat(concept.getDefinitions().stream()
        .filter(d -> !"ALT_DEFINITION".equals(d.getType()) && d.getQualifiers().size() > 0)
        .flatMap(d -> d.getQualifiers().stream()).filter(q -> q.getType().equals("attribution")).count())
            .isGreaterThan(0);
  }

  /**
   * Test full syn.
   *
   * @throws Exception the exception
   */
  @Test
  public void testFullSyn() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C100065?include=synonyms";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C100065");
    assertThat(concept.getSynonyms().size()).isGreaterThan(0);
    assertThat(concept.getSynonyms().stream().filter(s -> s.getSource() != null
        && s.getSubSource() != null && s.getCode() != null && s.getTermGroup() != null).count())
            .isGreaterThan(0);
  }

  /**
   * Test mappings.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMappings() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C101034?include=maps";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C101034");
    assertThat(concept.getMaps().size()).isGreaterThan(0);
    assertThat(concept.getMaps().stream()
        .filter(m -> m.getType() != null && m.getTargetCode() != null
            && m.getTargetTerminologyVersion() != null && m.getTargetTermGroup() != null
            && m.getTargetTerminology() != null)
        .count()).isGreaterThan(0);
  }

  /**
   * Test go annotation.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGoAnnotation() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C19799?include=properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C19799");
    assertThat(concept.getProperties().size()).isGreaterThan(0);
    assertThat(concept.getProperties().stream()
        .filter(p -> p.getType().contentEquals("GO_Annotation")).count()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().contentEquals("GO_Annotation"))
            .flatMap(p -> p.getQualifiers().stream())
            .filter(q -> q.getType().contentEquals("go-evi")).count()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().contentEquals("GO_Annotation"))
            .flatMap(p -> p.getQualifiers().stream())
            .filter(q -> q.getType().contentEquals("go-id")).count()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().contentEquals("GO_Annotation"))
            .flatMap(p -> p.getQualifiers().stream())
            .filter(q -> q.getType().contentEquals("go-source")).count()).isGreaterThan(0);
    assertThat(
        concept.getProperties().stream().filter(p -> p.getType().contentEquals("GO_Annotation"))
            .flatMap(p -> p.getQualifiers().stream())
            .filter(q -> q.getType().contentEquals("source-date")).count()).isGreaterThan(0);
  }

  /**
   * Test no property qualifier overlap.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNoPropertyQualifierOverlap() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // Get properties
    url = metaBaseUrl + "/ncit/properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final List<Concept> list1 =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list1).isNotEmpty();

    // Get qualifiers
    url = metaBaseUrl + "/ncit/qualifiers";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final List<Concept> list2 =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list2).isNotEmpty();

    // list1 and list2 should not have any codes in common
    final Set<String> codes1 = list1.stream().map(c -> c.getCode()).collect(Collectors.toSet());
    final Set<String> codes2 = list2.stream().map(c -> c.getCode()).collect(Collectors.toSet());
    assertThat(Sets.intersection(codes1, codes2).size()).isEqualTo(0);

    // list1 and list2 should not have any names in common
    final Set<String> names1 = list1.stream().map(c -> c.getName()).collect(Collectors.toSet());
    final Set<String> names2 = list2.stream().map(c -> c.getName()).collect(Collectors.toSet());
    assertThat(Sets.intersection(names1, names2).size()).isEqualTo(0);

  }

  /**
   * Test no property qualifier overlap via lookup.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNoPropertyQualifierOverlapViaLookup() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // Get properties
    url = metaBaseUrl + "/ncit/properties";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final List<Concept> list1 =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list1).isNotEmpty();

    // Get qualifiers
    url = metaBaseUrl + "/ncit/qualifiers";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final List<Concept> list2 =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list2).isNotEmpty();

    // Take the each property and look it up as a qualifier, expect 404
    for (final Concept property : list1) {
      url = metaBaseUrl + "/ncit/qualifier/" + property.getCode();
      log.info("Testing url - " + url);
      result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
      content = result.getResponse().getContentAsString();
      log.info("  content = " + content);
    }

    // Take the each qualifier and look it up as a property, expect 404
    for (final Concept qualifier : list2) {
      url = metaBaseUrl + "/ncit/property/" + qualifier.getCode();
      log.info("Testing url - " + url);
      result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
      content = result.getResponse().getContentAsString();
      log.info("  content = " + content);
    }
  }

  /**
   * Test property not used.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPropertyNotUsed() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    // P379, P377, P388, P365, P382, P392, P380
    url = metaBaseUrl + "/ncit/property/P379";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);

    url = metaBaseUrl + "/ncit/property/P380";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
  }

  /**
   * Test common property used.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCommonPropertyUsed() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    for (final String name : EVSUtils.getCommonPropertyNames(null)) {
      // skip label
      if (name.equals("label")) {
        continue;
      }
      // Try P98 - expect to not find it as a property
      url = metaBaseUrl + "/ncit/property/" + name;
      log.info("Testing url - " + url);
      result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
      content = result.getResponse().getContentAsString();
      log.info(" content = " + content);
      concept = new ObjectMapper().readValue(content, Concept.class);
      assertThat(concept).isNotNull();
    }

  }

  /**
   * Test qualifier different label pref name.
   *
   * @throws Exception the exception
   */
  @Test
  public void testQualifierAttr() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // attr
    url = metaBaseUrl + "/ncit/qualifier/attribution";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();

    // Assert that the "preferred name" synonym does match the concept name
    assertThat(
        concept.getSynonyms().stream().filter(c -> c.getType().equals("Preferred_Name")).count())
            .isGreaterThan(0);
    assertThat(concept.getSynonyms().stream().filter(c -> c.getType().equals("Preferred_Name"))
        .findFirst().get().getName()).isEqualTo("attribution");

    // Assert that an "rdfs:label" synonym exists and does not match
    assertThat(concept.getSynonyms().stream().filter(c -> c.getType().equals("rdfs:label")).count())
        .isGreaterThan(0);
    assertThat(concept.getSynonyms().stream().filter(c -> c.getType().equals("rdfs:label"))
        .findFirst().get().getName()).isEqualTo("attribution");

  }

  /**
   * Test all qualifiers can be individually resolved.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAllQualifiersCanBeIndividuallyResolved() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // Get qualifiers
    url = metaBaseUrl + "/ncit/qualifiers";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    final List<Concept> list =
        new ObjectMapper().readValue(content, new TypeReference<List<Concept>>() {
          // n/a
        });
    assertThat(list).isNotEmpty();

    // Take the each qualifier and look it up as a qualifier, expect 200
    for (final Concept qualifier : list) {
      url = metaBaseUrl + "/ncit/qualifier/" + qualifier.getCode();
      log.info("Testing url - " + url);
      result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    }
  }

}
