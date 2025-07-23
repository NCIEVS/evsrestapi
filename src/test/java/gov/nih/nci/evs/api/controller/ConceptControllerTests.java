package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.AssociationEntry;
import gov.nih.nci.evs.api.model.AssociationEntryResultList;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;

/** Integration tests for ConceptController. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ConceptControllerTests.class);

  /** The sparql query manager service. */
  @Autowired SparqlQueryManagerService sparqlQueryManagerService;

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

  /** Sets the up. */
  @BeforeEach
  public void setUp() {

    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1/concept";
  }

  @Test
  /**
   * test get trailing slash 404
   *
   * @throws Exception
   */
  public void testGetTrailingSlashNcit() throws Exception {
    String url = baseUrl + "/ncit/C3224/";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
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
    assertThat(concept.getTerminology()).isEqualTo("ncit");

    // Check hidden fields of concept (prove that WriteOnlyProperty) works
    assertThat(concept.getNormName()).isNull();
    assertThat(concept.getStemName()).isNull();
    assertThat(concept.getSynonyms().stream().filter(s -> s.getNormName() != null).count())
        .isEqualTo(0);
    assertThat(concept.getSynonyms().stream().filter(s -> s.getStemName() != null).count())
        .isEqualTo(0);
  }

  /**
   * Test get full concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptFull() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with "by code"
    url = baseUrl + "/ncit/C3224?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("C3224");
    assertThat(concept.getName()).isEqualTo("Melanoma");
    assertThat(concept.getTerminology()).isEqualTo("ncit");

    // Even full doesn't include descendants and paths
    assertThat(concept.getDescendants()).isEmpty();
    assertThat(concept.getPaths()).isNull();

    // check that normName, stemName, and property codes are not showing up in searches, as
    // is intended
    assertThat(concept.getNormName()).isNull();
    assertThat(concept.getStemName()).isNull();
    assertThat(concept.getSynonyms().stream().filter(s -> s.getNormName() != null).count())
        .isEqualTo(0);
    assertThat(concept.getSynonyms().stream().filter(s -> s.getStemName() != null).count())
        .isEqualTo(0);
    assertThat(concept.getAssociations().size()).isGreaterThan(0);
    assertThat(concept.getRoles().size()).isGreaterThan(0);
  }

  /**
   * Test get concept with extra mapping maps.
   *
   * @throws Exception the exception
   */
  @Test
  @Disabled("The extra mappings are blocked off until we figure out versioning")
  public void testGetConceptExtraMappings() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // TODO: if there was a case of a map that had a P375 in the owl (mapsTo) and ALSO
    // was represented in an explicit viewable map - then we could test that concept
    // details properly de-duplicates that.  But we don't have any such data at the moment.

    //

    // Verify this concept has a "maps to" to an HGNC code
    // This comes from the independent NCI-HGNC map distributed by Liz
    // and isn't in the owl file itself.
    url = baseUrl + "/ncit/C143031?include=full";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getMaps().size()).isGreaterThan(0);
    assertThat(
            concept.getMaps().stream()
                .filter(
                    m ->
                        m.getMapsetCode() != null
                            && m.getMapsetCode().equals("NCIt_to_HGNC_Mapping")
                            && m.getSourceCode().equals("C143031")
                            && m.getTargetCode().equals("HGNC:24086"))
                .count())
        .isEqualTo(1);
  }

  /**
   * Test bad get concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBadGetConcept() throws Exception {
    String url = null;

    // Test lookup of >500 codes
    url =
        baseUrl
            + "/ncit?list="
            + IntStream.range(1, 1002).mapToObj(String::valueOf).collect(Collectors.joining(","));
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isBadRequest()).andReturn();
    // content is blank because of MockMvc

    // Bad terminology
    url = baseUrl + "/ncitXXX/C3224";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup
    url = baseUrl + "/ncit/NOTACODE";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup - associations
    url = baseUrl + "/ncit/NOTACODE/associations";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup - children
    url = baseUrl + "/ncit/NOTACODE/children";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup - descendants
    url = baseUrl + "/ncit/NOTACODE/descendants";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup - disjointWith
    url = baseUrl + "/ncit/NOTACODE/disjointWith";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup - inverseAssociations
    url = baseUrl + "/ncit/NOTACODE/inverseAssociations";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup - inverseRoles
    url = baseUrl + "/ncit/NOTACODE/inverseRoles";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup - maps
    url = baseUrl + "/ncit/NOTACODE/maps";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup - parents
    url = baseUrl + "/ncit/NOTACODE/parents";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

    // Bad lookup - roles
    url = baseUrl + "/ncit/NOTACODE/roles";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
    // content is blank because of MockMvc

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

    final MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    final List<Concept> list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(0).getTerminology()).isEqualTo("ncit");
    assertThat(list.get(0).getSynonyms()).isNotEmpty();
    assertThat(list.get(0).getDefinitions()).isNotEmpty();
  }

  /**
   * Test get concepts with list with license restriction.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptsWithListWithLicenseRestriction() throws Exception {

    MvcResult result = null;
    String content = null;
    String url = null;
    List<Concept> list = null;

    // Try MDR with 10 codes
    // grep MDR MRCONSO.RRF | cut -d\| -f 14
    url =
        baseUrl
            + "/mdr?list=10021428,10021994,10036030,10009729,10066874,10017885,10053567,10015389,"
            + "10030182,10017924&include=minimal";
    log.info("Testing url - " + url);

    result =
        mvc.perform(get(url).header("X-EVSRESTAPI-License-Key", appProperties.getUiLicense()))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list).isNotEmpty();
    // 2 codes must be
    assertThat(list.size()).isEqualTo(10);
    assertThat(list.get(0).getTerminology()).isEqualTo("mdr");
    assertThat(list.get(0).getSynonyms()).isEmpty();
    assertThat(list.get(0).getDefinitions()).isEmpty();

    url =
        baseUrl
            + "/mdr?list=10021428,10021994,10036030,10009729,10066874,10017885,10053567,10015389,"
            + "10030182,10017924,10017884&include=minimal";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isForbidden()).andReturn();
  }

  /**
   * Test get concept associations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptAssociations() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Association> list = null;

    // NOTE, this includes a middle concept code that is bougs
    url = baseUrl + "/ncit/C3224/associations";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Association>>() {
                  // n/a
                });
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(5);

    // Test case without associations
    url = baseUrl + "/ncit/C2291/associations";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Association>>() {
                  // n/a
                });
    assertThat(list).isEmpty();
  }

  /**
   * Test get concept inverse associations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptInverseAssociations() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Association> list = null;

    url = baseUrl + "/ncit/C100139/inverseAssociations";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Association>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();

    // Test case without inverse associations
    // C3224 no longer has inverse associations
    url = baseUrl + "/ncit/C2291/inverseAssociations";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Association>>() {
                  // n/a
                });
    assertThat(list).isEmpty();
  }

  /**
   * Test get concept roles.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptRoles() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Role> list = null;

    // NOTE, this includes a middle concept code that is bougs
    url = baseUrl + "/ncit/C3224/roles";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Role>>() {
                  // n/a
                });
    assertThat(list).isNotEmpty();

    // Test case without roles
    url = baseUrl + "/ncit/C2291/roles";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Role>>() {
                  // n/a
                });
    assertThat(list).isEmpty();
  }

  /**
   * Test get concept inverse roles.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptInverseRoles() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Role> list = null;

    // NOTE, this includes a middle concept code that is bougs
    url = baseUrl + "/ncit/C3224/inverseRoles";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Role>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();

    // Test case without inverse roles
    url = baseUrl + "/ncit/C2291/inverseRoles";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Role>>() {
                  // n/a
                });
    assertThat(list).isEmpty();
  }

  /**
   * Test get concept parents.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptParents() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;

    // NOTE, this includes a middle concept code that is bougs
    url = baseUrl + "/ncit/C3224/parents";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();

    // Test case without parents
    url = baseUrl + "/ncit/C12913/parents";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list).isEmpty();
  }

  /**
   * Test get concept children.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptChildren() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> list = null;

    // NOTE, this includes a middle concept code that is bougs
    url = baseUrl + "/ncit/C3224/children";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(5);

    // Test case without children
    url = baseUrl + "/ncit/C2291/children";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list).isEmpty();
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
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    Predicate<Concept> byLevel = concept -> concept.getLevel() > 0;
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(5);
    // preserve level
    assertThat(list.stream().filter(byLevel).collect(Collectors.toList()).size() > 0);

    // Test fromRecord and pageSize
    url = baseUrl + "/ncit/C7058/descendants?fromRecord=2&pageSize=5";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    // check pageSize
    assertThat(list.size()).isEqualTo(5);
    // preserve level
    assertThat(list.stream().filter(byLevel).collect(Collectors.toList())).isNotEmpty();

    // Test fromRecord and pageSize with larger values
    url = baseUrl + "/ncit/C7057/descendants?fromRecord=200&pageSize=500";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    // check pageSize
    assertThat(list.size()).isEqualTo(500);
    // preserve level
    assertThat(list.stream().filter(byLevel).collect(Collectors.toList())).isNotEmpty();

    // Test pageSize with BIG value
    url = baseUrl + "/ncit/C7057/descendants?pageSize=10000";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    // check pageSize
    assertThat(list.size()).isEqualTo(10000);
    // preserve level
    assertThat(list.stream().filter(byLevel).collect(Collectors.toList())).isNotEmpty();

    // Test case without descendants
    url = baseUrl + "/ncit/C2291/descendants";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list).isEmpty();

    // Test case with descendants < pageSize but non-zero
    url = baseUrl + "/ncit/C2323/descendants?pageSize=10";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(list).isNotEmpty();
    assertThat(list.size() < 10);

    // Test case with maxLevel
    url = baseUrl + "/ncit/C3510/descendants?maxLevel=2";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    byLevel = concept -> concept.getLevel() > 2;
    assertThat(list).isNotEmpty();
    int size = list.size();
    assertThat(list.size()).isGreaterThan(5);
    // preserve level
    assertThat(list.stream().filter(byLevel).collect(Collectors.toList()).isEmpty());
    byLevel = concept -> concept.getLevel() <= 2;
    assertThat(list.stream().filter(byLevel).collect(Collectors.toList()).size()).isEqualTo(size);
  }

  /**
   * Test get concept maps.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptMaps() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<Mapping> list = null;

    // NOTE, this includes a middle concept code that is bougs
    url = baseUrl + "/ncit/C3224/maps";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Mapping>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();

    // Test case without maps
    url = baseUrl + "/ncit/C2291/maps";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Mapping>>() {
                  // n/a
                });
    assertThat(list).isEmpty();

    // Test case with duplicate maps (should only have one)
    url = baseUrl + "/ncit/C957?include=maps";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    Concept concept =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<Concept>() {
                  // n/a
                });
    assertThat(concept).isNotNull();
    assertThat(concept.getMaps().size()).isEqualTo(1);

    // Test case with duplicate maps (should have two, one regular and one mapping map)
    url = baseUrl + "/ncit?include=maps&list=C957,C49172";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    List<Concept> conceptList =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(conceptList).isNotNull();
    assertThat(conceptList.size()).isEqualTo(2);
    assertThat(conceptList.get(0).getMaps().get(0).getMapsetCode()).isNull();
    assertThat(conceptList.get(1).getMaps().get(0).getMapsetCode()).isNull();
    assertThat(conceptList.get(0).getMaps().get(0).getTargetName())
        .isNotEqualTo(conceptList.get(1).getMaps().get(0).getTargetName());

    // Test that map adding works with concept list
    url = baseUrl + "/ncit/C49172?include=maps";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    concept =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<Concept>() {
                  // n/a
                });
    assertThat(concept).isNotNull();
    assertThat(concept.getMaps().size()).isEqualTo(1);
    assertThat(concept.getMaps().get(0).getMapsetCode()).isNull();
    assertThat(concept.getMaps().get(0).getTargetName()).isEqualTo("11C Topotecan");

    // Test case without maps
    url = baseUrl + "/ncit/C2291/maps";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Mapping>>() {
                  // n/a
                });
    assertThat(list).isEmpty();
  }

  /**
   * Test get concept history.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptHistory() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // NOTE, this is a concept that has replaced other concepts and is retired itself
    url = baseUrl + "/ncit/C14615/history";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    log.info("  list = " + concept.getHistory().size());
    assertThat(concept.getHistory()).isNotEmpty();

    boolean foundRetiredThis = false;
    boolean foundReplacedAnother = false;
    boolean foundModify = false;

    for (final History history : concept.getHistory()) {

      if (history.getAction().equals("retire") && history.getCode().equals("C14615")) {
        foundRetiredThis = true;

      } else if (history.getAction().equals("modify") && history.getCode().equals("C14615")) {
        foundModify = true;

      } else if (history.getAction().equals("retire")
          && !history.getCode().equals("C14615")
          && history.getReplacementCode().equals("C14615")) {
        foundReplacedAnother = true;
      }
    }

    assertThat(foundRetiredThis && foundReplacedAnother && foundModify).isTrue();

    // NOTE, this is a concept that should not have history
    url = baseUrl + "/ncim/C0005768/history";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    log.info("  list = " + concept.getHistory().size());
    assertThat(concept.getHistory()).isEmpty();
  }

  /**
   * Test get concept disjoint with.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetConceptDisjointWith() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<DisjointWith> list = null;

    // NOTE, this includes a middle concept code that is bougs
    url = baseUrl + "/ncit/C3910/disjointWith";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<DisjointWith>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.stream().filter(d -> d.getRelatedCode().equals("C7057")).count())
        .isGreaterThan(0);

    // Test case without disjointWith
    url = baseUrl + "/ncit/C2291/disjointWith";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<DisjointWith>>() {
                  // n/a
                });
    assertThat(list).isEmpty();
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
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).getSynonyms()).isEmpty();
    assertThat(list.get(0).getCode()).isNotEmpty();
    assertThat(list.get(0).getName()).isNotEmpty();
    assertThat(list.get(0).getTerminology()).isNotEmpty();
    assertThat(list.get(0).getVersion()).isNotEmpty();
    assertThat(list.get(0).getLeaf()).isEqualTo(false);

    url = baseUrl + "/ncit/roots?include=summary";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).getSynonyms().get(0)).isNotNull();

    url = baseUrl + "/ncit/roots?include=full";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).getSynonyms().get(0)).isNotNull();

    // Even full doesn't include descendants and paths
    assertThat(list.get(0).getDescendants()).isEmpty();
    assertThat(list.get(0).getPaths()).isNull();

    // check that normName and property codes are not showing up in searches, as
    // is intended
    assertThat(list.get(0).getNormName()).isNull();
    assertThat(list.get(0).getSynonyms().get(0).getNormName()).isNull();

    // check for a couple things that should only show up in full
    assertThat(list.get(0).getInverseAssociations().get(0)).isNotNull();
    assertThat(list.get(0).getChildren().get(0)).isNotNull();
    assertThat(list.get(0).getDisjointWith().get(0)).isNotNull();
  }

  /**
   * Test get subtree.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetSubtree() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    List<HierarchyNode> list = null;

    url = baseUrl + "/ncit/C40335/subtree";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<HierarchyNode>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(5);
    // check that subtree is properly expanded
    assertThat(list.stream().filter(n -> n.getExpanded() != null && n.getExpanded()).count())
        .isGreaterThan(0);
    // something should have children
    assertThat(list.stream().filter(c -> c.getChildren().size() > 0).count()).isGreaterThan(0);
    // none should have "level" set
    assertThat(list.stream().filter(c -> c.getLevel() != null).count()).isEqualTo(0);
    // children should not have "level" set
    assertThat(
            list.stream()
                .flatMap(c -> c.getChildren().stream())
                .filter(c -> c.getLevel() != null)
                .count())
        .isEqualTo(0);
    // there should be a leaf node in the hierarchy
    assertThat(hasLeafNode(list)).isTrue();
    // something should have grand children
    assertThat(
            list.stream()
                .filter(
                    c ->
                        c.getChildren().size() > 0
                            && c.getChildren().stream()
                                    .filter(c2 -> c2.getChildren().size() > 0)
                                    .count()
                                > 0)
                .count())
        .isGreaterThan(0);

    // Test case with bad terminology
    url = baseUrl + "/test/C2291/subtree";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound());

    // Test case with bad code
    url = baseUrl + "/ncit/BADCODE/subtree";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound());

    // Test subtree children (see what it looks like)
    url = baseUrl + "/ncit/C7058/subtree/children";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<HierarchyNode>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(5);
    // none should have "level" set
    assertThat(list.stream().filter(c -> c.getLevel() != null).count()).isEqualTo(0);

    // Test case with top level term
    url = baseUrl + "/ncit/C12913/subtree";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isOk());

    // Test case with bad terminology
    url = baseUrl + "/test/C2291/subtree/children";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound());

    // Test case with bad code
    url = baseUrl + "/ncit/BADCODE/subtree/children";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound());
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
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isEmpty();
    assertThat(list.get(0).get(0).getName()).isNotEmpty();
    assertThat(list.get(0).get(0).getTerminology()).isNotEmpty();
    assertThat(list.get(0).get(0).getVersion()).isNotEmpty();
    assertThat(list.get(0).get(0).getLeaf()).isEqualTo(false);

    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C7057");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode()).isEqualTo("C3224");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

    url = baseUrl + "/ncit/C3224/pathsFromRoot?include=summary";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isNotEmpty();
    assertThat(list.get(0).get(0).getName()).isNotEmpty();
    assertThat(list.get(0).get(0).getTerminology()).isNotEmpty();
    assertThat(list.get(0).get(0).getVersion()).isNotEmpty();
    assertThat(list.get(0).get(0).getLeaf()).isEqualTo(false);

    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C7057");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode()).isEqualTo("C3224");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

    url = baseUrl + "/ncit/C3224/pathsFromRoot?include=minimal";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());

    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getTerminology()).isNotNull();
    assertThat(list.get(0).get(0).getVersion()).isNotNull();

    // C162271 - test large case
    url = baseUrl + "/ncit/C162271/pathsFromRoot";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(700);

    // C162271 - test large case
    url = baseUrl + "/ncit/C162271/pathsFromRoot?fromRecord=0&pageSize=10";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(10);
    String origContent = content;

    // C162271 - test large case
    url = baseUrl + "/ncit/C162271/pathsFromRoot?fromRecord=10&pageSize=10";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(10);
    assertThat(content).isNotEqualTo(origContent);

    // C98767 - test case with paging (for something with only 1)
    url = baseUrl + "/ncit/C98767/pathsFromRoot?fromRecord=0&include=minimal&pageSize=100";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list.size()).isEqualTo(1);

    // C4872 - test case with paging
    url = baseUrl + "/ncit/C4872/pathsFromRoot?fromRecord=0&include=minimal&pageSize=2";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list.size()).isEqualTo(2);

    // C4872 - test case with paging
    url = baseUrl + "/ncit/C4872/pathsFromRoot?fromRecord=0&include=minimal&pageSize=10";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list.size()).isGreaterThan(2);
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

    // C3224
    url = baseUrl + "/ncit/C3224/pathsToRoot";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());

    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isEmpty();
    assertThat(list.get(0).get(0).getName()).isNotEmpty();
    assertThat(list.get(0).get(0).getTerminology()).isNotEmpty();
    assertThat(list.get(0).get(0).getVersion()).isNotEmpty();
    assertThat(list.get(0).get(0).getLeaf()).isEqualTo(false);

    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode()).isEqualTo("C7057");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

    url = baseUrl + "/ncit/C3224/pathsToRoot?include=summary";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isNotEmpty();
    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode()).isEqualTo("C7057");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

    url = baseUrl + "/ncit/C3224/pathsToRoot?include=minimal";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getTerminology()).isNotNull();
    assertThat(list.get(0).get(0).getVersion()).isNotNull();

    // C162271 - test large case
    url = baseUrl + "/ncit/C162271/pathsToRoot";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isGreaterThan(700);

    // C162271 - test large case
    url = baseUrl + "/ncit/C162271/pathsToRoot?fromRecord=0&pageSize=10";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(10);
    String origContent = content;

    // C162271 - test large case
    url = baseUrl + "/ncit/C162271/pathsToRoot?fromRecord=10&pageSize=10";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.size()).isEqualTo(10);
    assertThat(content).isNotEqualTo(origContent);

    // C98767 - test case with paging (for something with only 1)
    url = baseUrl + "/ncit/C98767/pathsToRoot?fromRecord=0&include=minimal&pageSize=100";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list.size()).isEqualTo(1);

    // C4872 - test case with paging
    url = baseUrl + "/ncit/C4872/pathsToRoot?fromRecord=0&include=minimal&pageSize=2";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list.size()).isEqualTo(2);

    // C4872 - test case with paging
    url = baseUrl + "/ncit/C4872/pathsToRoot?fromRecord=0&include=minimal&pageSize=100";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    // log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list.size()).isGreaterThan(2);
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
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    log.info("  list values = " + list);

    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isEmpty();
    assertThat(list.get(0).get(0).getName()).isNotEmpty();
    assertThat(list.get(0).get(0).getTerminology()).isNotEmpty();
    assertThat(list.get(0).get(0).getVersion()).isNotEmpty();
    assertThat(list.get(0).get(0).getLeaf()).isEqualTo(false);

    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode()).isEqualTo("C2991");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

    url = baseUrl + "/ncit/C3224/pathsToAncestor/C2991?include=summary";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });
    log.info("  list = " + list.size());
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getSynonyms()).isNotEmpty();
    // Assert that the first element is a "root" - e.g. C7057
    assertThat(list.get(0).get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).get(list.get(0).size() - 1).getCode()).isEqualTo("C2991");
    // Assert that numbers count in order, starting at 1 and ending in legnth
    assertThat(list.get(0).get(0).getLevel()).isEqualTo(0);
    assertThat(list.get(0).get(list.get(0).size() - 1).getLevel())
        .isEqualTo(list.get(0).size() - 1);

    url = baseUrl + "/ncit/C3224/pathsToAncestor/C3224?include=minimal";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });

    // check format for ancestor of self
    assertThat(list).isNotEmpty();
    assertThat(list.get(0).get(0).getTerminology()).isNotNull();
    assertThat(list.get(0).get(0).getVersion()).isNotNull();

    assertThat(list.size()).isEqualTo(1); // single path
    assertThat(list.get(0).size()).isEqualTo(1); // single element in path
    assertThat(list.get(0).get(0).getLevel() == 0);

    // Test case with paging
    url = baseUrl + "/ncit/C98767/pathsToAncestor/C49237?fromRecord=0&include=minimal&pageSize=100";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });

    // check format for ancestor of self
    assertThat(list.size()).isEqualTo(1);

    // Test case with paging (2 results)
    url = baseUrl + "/ncit/C4872/pathsToAncestor/C7057?fromRecord=0&include=minimal&pageSize=2";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });

    // check format for ancestor of self
    assertThat(list.size()).isEqualTo(2);

    // Test case with paging (2 results)
    url = baseUrl + "/ncit/C4872/pathsToAncestor/C7057?fromRecord=0&include=minimal&pageSize=100";
    log.info("Testing url - " + url);

    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<List<Concept>>>() {
                  // n/a
                });

    // check format for ancestor of self
    assertThat(list.size()).isGreaterThan(2);
  }

  /**
   * Test that we don't have erroneous definitions or synonyms.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCheckDefsAndSynsAreRight() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test with C3224 synonyms
    url = baseUrl + "/ncit/C3224";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    for (Definition def : concept.getDefinitions()) {
      assertFalse(def.getDefinition().contains("nephron"));
    }

    // Test with C36716 synonyms
    url = baseUrl + "/ncit/C36716";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    for (Synonym syn : concept.getSynonyms()) {
      assertFalse(syn.getName().contains("nephron"));
    }

    // Test with C100808 definitions
    url = baseUrl + "/ncit/C100808";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    for (Definition def : concept.getDefinitions()) {
      assertFalse(def.getDefinition().contains("arrhythmia"));
    }
  }

  /**
   * Test association entries API call.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAssociationEntries() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    AssociationEntryResultList resultList = null;

    // Test with valid association label
    url = baseUrl + "/ncit/associations/Has_Target";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    resultList = new ObjectMapper().readValue(content, AssociationEntryResultList.class);
    assertThat(resultList).isNotNull();
    assertThat(resultList.getTimeTaken()).isGreaterThan(0);
    assertThat(resultList.getTotal()).isGreaterThan(2500);
    assertThat(resultList.getParameters().getTerminology()).contains("Has_Target");
    for (AssociationEntry assoc : resultList.getAssociationEntries()) {
      assertThat(assoc.getAssociation().equals("Has_Target"));
    }

    // Test with valid code -> label
    url = baseUrl + "/ncit/associations/A7";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    resultList = new ObjectMapper().readValue(content, AssociationEntryResultList.class);
    assertThat(resultList).isNotNull();
    assertThat(resultList.getTimeTaken()).isGreaterThan(0);
    assertThat(resultList.getTotal()).isGreaterThan(2500);
    assertThat(resultList.getParameters().getTerminology()).contains("Has_Target");
    for (AssociationEntry assoc : resultList.getAssociationEntries()) {
      assertThat(assoc.getAssociation().equals("Has_Target"));
    }

    // Test for relatively valid counts
    url = baseUrl + "/ncit/associations/A5";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    resultList = new ObjectMapper().readValue(content, AssociationEntryResultList.class);
    assertThat(resultList).isNotNull();
    assertThat(resultList.getTimeTaken()).isGreaterThan(0);
    assertThat(resultList.getTotal()).isLessThan(3000);
    assertThat(resultList.getParameters().getTerminology()).contains("Has_Salt_Form");
    for (AssociationEntry assoc : resultList.getAssociationEntries()) {
      assertThat(assoc.getAssociation().equals("Has_Salt_Form"));
    }

    // Test that concept subset is properly 404'd
    url = baseUrl + "/ncit/associations/A8";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();

    // Test pageSize
    url = baseUrl + "/ncit/associations/Has_Target?pageSize=12";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    resultList = new ObjectMapper().readValue(content, AssociationEntryResultList.class);
    assertThat(resultList).isNotNull();
    assertThat(resultList.getTotal()).isGreaterThan(0);
    assertThat(resultList.getParameters().getTerminology()).contains("12");
    assertThat(resultList.getAssociationEntries().size()).isEqualTo(12);

    // Test fromRecord
    url = baseUrl + "/ncit/associations/Has_Target?fromRecord=1";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    resultList = new ObjectMapper().readValue(content, AssociationEntryResultList.class);
    assertThat(resultList).isNotNull();
    assertThat(resultList.getTotal()).isGreaterThan(0);
    assertThat(resultList.getParameters().getTerminology()).contains("1");
    assertThat(resultList.getAssociationEntries().get(0).getCode() == "C125718");
    assertThat(resultList.getAssociationEntries().get(0).getRelatedCode() == "C128784");
  }

  /**
   * Checks if hierarchy has a leaf node anywhere in the hierarchy.
   *
   * @param list list of hierarchy nodes
   * @return boolean true if hierarchy has a leaf node, else false
   */
  private boolean hasLeafNode(List<HierarchyNode> list) {
    if (CollectionUtils.isEmpty(list)) return false;
    for (HierarchyNode node : list) {
      if (node.getLeaf() != null && node.getLeaf()) return true;
      if (!CollectionUtils.isEmpty(node.getChildren())) {
        if (hasLeafNode(node.getChildren())) return true;
      }
    }

    return false;
  }

  /**
   * Test getting subsets members for code and terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSubsetMembers() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    List<Concept> subsetMembers;

    url = baseUrl + "/ncit/subsetMembers/C157225";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    subsetMembers =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(subsetMembers.size()).isGreaterThan(1);

    url = baseUrl + "/ncit/subsetMembers/C157225?pageSize=10";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    subsetMembers =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });

    assertThat(subsetMembers.size()).isEqualTo(10);

    url = baseUrl + "/ncit/subsetMembers/C1111?pageSize=10";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    subsetMembers =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });

    assertThat(subsetMembers.size()).isEqualTo(0);
  }

  /**
   * Test terminology versions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTerminologyVersion() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    url = "/api/v1/metadata/terminologies";
    result =
        mvc.perform(get(url).param("terminology", "ncit").param("tag", "weekly"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    Terminology terminology =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Terminology>>() {
                  // n/a
                })
            .get(0);
    String weeklyTerm = terminology.getTerminologyVersion();
    String baseWeeklyUrl = baseUrl + "/" + weeklyTerm;

    result =
        mvc.perform(get(baseWeeklyUrl).param("list", "C3224"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    List<Concept> conceptResults =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(conceptResults.get(0).getVersion() == terminology.getVersion());

    result = mvc.perform(get(baseWeeklyUrl + "/C3224")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    Concept concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept.getVersion() == terminology.getVersion());

    result =
        mvc.perform(get(baseWeeklyUrl + "/C3224/children")).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    conceptResults =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Concept>>() {
                  // n/a
                });
    assertThat(conceptResults.get(0).getVersion() == terminology.getVersion());
  }

  /**
   * Test get terminology concepts.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetTerminologyConcepts() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    url = "/api/v1/concept/radlex/codes";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();

    // very small terminology
    ArrayList<String> terminologyCodes =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<ArrayList<String>>() {
                  // n/a
                });
    assertThat(terminologyCodes.size()).isGreaterThan(50);

    // small terminology
    url = "/api/v1/concept/ctcae5/codes";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologyCodes =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<ArrayList<String>>() {
                  // n/a
                });

    assertThat(terminologyCodes.size()).isGreaterThan(4000);

    // medium terminology
    url = "/api/v1/concept/go/codes";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologyCodes =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<ArrayList<String>>() {
                  // n/a
                });

    assertThat(terminologyCodes.size()).isGreaterThan(40000);

    // large terminology
    url = "/api/v1/concept/ncit/codes";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    terminologyCodes =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<ArrayList<String>>() {
                  // n/a
                });

    assertThat(terminologyCodes.size()).isGreaterThan(150000);
  }

  /**
   * Test that code values are present for Synonym, Definition, Property, Role/inverse,
   * Association/inverse, and Qualifier with include=full.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeValuesPresentInFullInclude() throws Exception {
    String url;
    MvcResult result;
    String content;
    Concept concept;

    // C101669 has synonyms, definitions
    url = baseUrl + "/ncit/C101669?include=full";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    concept = new ObjectMapper().readValue(content, Concept.class);

    assertThat(concept.getSynonyms().get(0).getTypeCode() != null).isTrue();
    assertThat(concept.getDefinitions().get(0).getCode() != null).isTrue();
    assertThat(concept.getProperties().get(0).getCode() != null).isTrue();
    assertThat(concept.getAssociations().get(0).getCode() != null).isTrue();

    // C3224 lacks qualifiers, but should have code for other types
    url = baseUrl + "/ncit/C3224?include=full";
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    concept = new ObjectMapper().readValue(content, Concept.class);

    assertThat(concept.getSynonyms().get(0).getTypeCode() != null).isTrue();
    assertThat(concept.getDefinitions().get(0).getCode() != null).isTrue();
    assertThat(concept.getProperties().get(0).getCode() != null).isTrue();
    assertThat(concept.getRoles().get(0).getCode() != null).isTrue();
    assertThat(concept.getInverseRoles().get(0).getCode() != null).isTrue();
    assertThat(concept.getAssociations().get(0).getCode() != null).isTrue();
    assertThat(concept.getInverseAssociations().get(0).getCode() != null)
        .isTrue();
  }
}
