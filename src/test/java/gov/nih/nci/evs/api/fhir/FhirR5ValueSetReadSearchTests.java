package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit FhirR5Tests. Tests the functionality of the FHIR R5 endpoints, CodeSystem, ValueSet, and
 * ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FhirR5ValueSetReadSearchTests {
  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR5ValueSetReadSearchTests.class);

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** The fhir VS path. */
  private final String fhirVSPath = "/fhir/r5/ValueSet";

  /** The Parser. */
  private static IParser parser;

  /** Sets the up once. */
  @BeforeAll
  public static void setUpOnce() {
    // Instantiate parser
    parser = FhirContext.forR5().newJsonParser();
  }

  /** Sets the up. */
  @BeforeEach
  public void setUp() {
    // the object mapper
    ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  /**
   * Test value set search.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearch() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirVSPath + "?_count=2000";

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Verify things about these
    // {"resourceType":"ValueSet","id":"ncit_25.06e","url":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","version":"25.06e","name":"NCI
    // Thesaurus
    // 25.06e","title":"ncit","status":"active","experimental":false,"publisher":"NCI","description":"NCI
    // Thesaurus, a controlled vocabulary in support of NCI administrative and
    // scientific activities. Produced by the Enterprise Vocabulary System
    // (EVS), a project by the NCI Center for Biomedical Informatics and
    // Information Technology. National Cancer Institute, National Institutes of
    // Health, Bethesda, MD 20892, U.S.A."}
    // {"resourceType":"ValueSet","id":"ncit_c100110","url":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110","identifier":[{"value":"C100110"}],"version":"25.06e","name":"CDISC
    // Questionnaire
    // Terminology","title":"ncit","status":"active","experimental":false,"publisher":"NCI","description":"Value
    // set representing the ncitsubsetC100110"}
    final Set<String> ids = new HashSet<>(Set.of("ncit_21.07a", "ncit_c61410"));
    final Set<String> urls =
        new HashSet<>(
            Set.of(
                "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs",
                "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C61410"));

    // Assert
    assertFalse(valueSets.isEmpty());
    for (Resource vs : valueSets) {
      log.info("  value set = " + parser.encodeResourceToString(vs));
      ValueSet vss = (ValueSet) vs;
      assertNotNull(vss);
      assertEquals(ResourceType.ValueSet, vss.getResourceType());
      assertNotNull(vss.getIdPart());
      assertNotNull(vss.getPublisher());
      assertNotNull(vss.getUrl());
      assertNotNull(vss.getName());
      assertNotNull(vss.getVersion());
      assertNotNull(vss.getTitle());
      assertNotNull(vss.getStatus());
      assertNotNull(vss.getExperimental());
      // assertNotNull(vss.getDescription()); zfa_2019-08-02 doesn't have a
      // description
      ids.remove(vss.getIdPart());
      urls.remove(vss.getUrl());
    }
    assertThat(ids).isEmpty();
    assertThat(urls).isEmpty();
  }

  /**
   * Test value set read.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetRead() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Act
    String firstValueSetId = valueSets.get(0).getIdPart();
    // reassign content
    content = this.restTemplate.getForObject(endpoint + "/" + firstValueSetId, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertEquals(ResourceType.ValueSet, valueSet.getResourceType());
    assertEquals(firstValueSetId, valueSet.getIdPart());
    assertEquals(valueSet.getUrl(), ((ValueSet) valueSets.get(0)).getUrl());
    assertEquals(valueSet.getName(), ((ValueSet) valueSets.get(0)).getName());
    assertEquals(valueSet.getVersion(), ((ValueSet) valueSets.get(0)).getVersion());
    assertEquals(valueSet.getTitle(), ((ValueSet) valueSets.get(0)).getTitle());
    assertEquals(valueSet.getDescription(), ((ValueSet) valueSets.get(0)).getDescription());
    assertEquals(valueSet.getStatus().toCode(), ((ValueSet) valueSets.get(0)).getStatus().toCode());
    assertEquals(valueSet.getExperimental(), ((ValueSet) valueSets.get(0)).getExperimental());
    assertEquals(valueSet.getPublisher(), ((ValueSet) valueSets.get(0)).getPublisher());
  }

  /**
   * Test value set read value set from code.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetReadCode() throws Exception {
    // Arrange
    String content;
    String code = "ncit_c129091";
    String name = "CDISC Questionnaire NCCN-FACT FBLSI-18 Version 2 Test Name Terminology";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C129091";
    String publisher = "NCI";
    String description = "Value set representing the ncitsubsetC129091";
    String title = "ncit";
    String status = "active";
    String experimental = "false";
    String endpoint = localHost + port + fhirVSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + code, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertEquals(ResourceType.ValueSet, valueSet.getResourceType());
    assertEquals(code, valueSet.getIdPart());
    assertEquals(name, valueSet.getName());
    assertEquals(publisher, valueSet.getPublisher());
    assertEquals(description, valueSet.getDescription());
    assertEquals(title, valueSet.getTitle());
    assertEquals(status, valueSet.getStatus().toCode());
    assertEquals(Boolean.parseBoolean(experimental), valueSet.getExperimental());
    assertEquals(url, valueSet.getUrl());
  }

  /**
   * Test value set read static id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetReadStaticId() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    String valueSetId = "umlssemnet_2023aa";

    content = this.restTemplate.getForObject(endpoint + "/" + valueSetId, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertEquals(ResourceType.ValueSet, valueSet.getResourceType());
    assertEquals(valueSetId, valueSet.getIdPart());
    assertEquals("http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs", valueSet.getUrl());
    assertEquals("UMLS Semantic Network 2023AA", valueSet.getName());
    assertEquals("National Library of Medicine", valueSet.getPublisher());
    assertEquals("umlssemnet", valueSet.getTitle());
    assertEquals("active", valueSet.getStatus().toCode());
    assertEquals(false, valueSet.getExperimental());
    assertEquals("2023AA", valueSet.getVersion());
    assertEquals("UMLS Semantic Network", valueSet.getDescription());
  }

  /**
   * Test value set read bad id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetReadBadId() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    String invalidId = "invalid_id";
    String messageNotFound = "Value set not found = " + invalidId;
    String errorCode = "not-found";

    content = this.restTemplate.getForObject(endpoint + "/" + invalidId, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test value set subset search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetSubsetSearchWithParameters() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_c100110")
            .queryParam("code", "C100110")
            .queryParam("name", "CDISC Questionnaire Terminology")
            .queryParam("title", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
            .queryParam("version", "25.06e");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, true); // Expecting results

    // Test 2: Invalid ID
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "invalid_id")
            .queryParam("code", "C100110")
            .queryParam("name", "CDISC Questionnaire Terminology")
            .queryParam("title", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 3: Invalid code
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e")
            .queryParam("code", "INVALID_CODE")
            .queryParam("name", "CDISC Questionnaire Terminology")
            .queryParam("title", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 4: Invalid name
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e") // .queryParam("code", "C61410")
            .queryParam("name", "Invalid Name")
            .queryParam("title", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 5: Invalid title
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e")
            .queryParam("code", "C100110")
            .queryParam("name", "CDISC Questionnaire Terminology")
            .queryParam("title", "invalid_title")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 6: Invalid URL
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e")
            .queryParam("code", "C100110")
            .queryParam("name", "CDISC Questionnaire Terminology")
            .queryParam("title", "ncit")
            .queryParam("url", "http://invalid.url")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 7: Invalid version
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e")
            .queryParam("code", "C100110")
            .queryParam("name", "CDISC Questionnaire Terminology")
            .queryParam("title", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
            .queryParam("version", "invalid_version");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);
  }

  /**
   * Test value set search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetSearchWithParameters() throws Exception {
    // TODO: currently data doesn't have the system data and only subsets have
    // the code
    // these tests should be amended when the data is updated

    // Arrange
    String endpoint = localHost + port + fhirVSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e") // .queryParam("code",
            // "C61410")
            .queryParam("name", "NCI Thesaurus 25.06e") // .queryParam("system",
            // "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs")
            .queryParam("version", "25.06e");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, true); // Expecting results

    // Test 2: Invalid ID
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "invalid_id") // .queryParam("code", "C61410")
            .queryParam("name", "NCI Thesaurus 25.06e") // .queryParam("system",
            // "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 3: Invalid code
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e")
            .queryParam("code", "INVALID_CODE")
            .queryParam("name", "NCI Thesaurus 25.06e") // .queryParam("system",
            // "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 4: Invalid name
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e") // .queryParam("code", "C61410")
            .queryParam("name", "Invalid Name") // .queryParam("system", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 5: Invalid system
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e") // .queryParam("code", "C61410")
            .queryParam("name", "NCI Thesaurus 25.06e") // .queryParam("system",
            // "invalid_system")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 6: Invalid URL
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e") // .queryParam("code", "C61410")
            .queryParam("name", "NCI Thesaurus 25.06e") // .queryParam("system",
            // "ncit")
            .queryParam("url", "http://invalid.url")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 7: Invalid version
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e") // .queryParam("code", "C61410")
            .queryParam("name", "NCI Thesaurus 25.06e") // .queryParam("system",
            // "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "invalid_version");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);
  }

  /**
   * Validate value set results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  // Helper method to validate results
  private void validateValueSetResults(Bundle data, boolean expectResults) {
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(valueSets.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ncit_25.06e"));
      final Set<String> urls =
          new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs"));

      for (Resource vs : valueSets) {
        log.info(" value set = " + parser.encodeResourceToString(vs));
        ValueSet vss = (ValueSet) vs;
        assertNotNull(vss);
        assertEquals(ResourceType.ValueSet, vss.getResourceType());
        assertNotNull(vss.getIdPart());
        assertNotNull(vss.getPublisher());
        assertNotNull(vss.getUrl());
        ids.remove(vss.getIdPart());
        urls.remove(vss.getUrl());
      }
      assertTrue(ids.isEmpty());
      assertTrue(urls.isEmpty());
    } else {
      assertTrue(data.getEntry().isEmpty() || valueSets.isEmpty());
    }
  }

  private void validateCanmedValueSetResults(
      org.hl7.fhir.r5.model.Bundle data, boolean expectResults) {
    List<org.hl7.fhir.r5.model.Resource> valueSets =
        data.getEntry().stream()
            .map(org.hl7.fhir.r5.model.Bundle.BundleEntryComponent::getResource)
            .toList();

    if (expectResults) {
      assertFalse(valueSets.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("canmed_202506"));
      final Set<String> urls = new HashSet<>(Set.of("http://seer.nci.nih.gov/CanMED.owl?fhir_vs"));

      for (org.hl7.fhir.r5.model.Resource vs : valueSets) {
        log.info(" value set = " + parser.encodeResourceToString(vs));
        org.hl7.fhir.r5.model.ValueSet vss = (org.hl7.fhir.r5.model.ValueSet) vs;
        assertNotNull(vss);
        assertEquals(org.hl7.fhir.r5.model.ResourceType.ValueSet, vss.getResourceType());
        assertNotNull(vss.getIdPart());
        assertNotNull(vss.getPublisher());
        assertNotNull(vss.getUrl());
        ids.remove(vss.getIdPart());
        urls.remove(vss.getUrl());
      }
      assertTrue(ids.isEmpty());
      assertTrue(urls.isEmpty());
    } else {
      assertTrue(data.getEntry().isEmpty() || valueSets.isEmpty());
    }
  }

  /**
   * Validate value set subset results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  private void validateValueSetSubsetResults(Bundle data, boolean expectResults) {
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(valueSets.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ncit_c100110"));
      final Set<String> urls =
          new HashSet<>(
              Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110"));

      for (Resource vs : valueSets) {
        log.info(" value set = " + parser.encodeResourceToString(vs));
        ValueSet vss = (ValueSet) vs;
        assertNotNull(vss);
        assertEquals(ResourceType.ValueSet, vss.getResourceType());
        assertNotNull(vss.getIdPart());
        assertNotNull(vss.getPublisher());
        assertNotNull(vss.getUrl());
        ids.remove(vss.getIdPart());
        urls.remove(vss.getUrl());
      }
      assertTrue(ids.isEmpty());
      assertTrue(urls.isEmpty());
    } else {
      assertTrue(data.getEntry().isEmpty() || valueSets.isEmpty());
    }
  }

  /**
   * Test value set search pagination.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetSearchPagination() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;

    // Test 1: Get default results without pagination
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle defaultData = parser.parseResource(Bundle.class, content);
    List<Resource> defaultValueSets =
        defaultData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 2: Get first page (count=2)
    UriComponentsBuilder firstPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2");

    content =
        this.restTemplate.getForObject(firstPageBuilder.build().encode().toUri(), String.class);
    Bundle firstPageData = parser.parseResource(Bundle.class, content);
    List<Resource> firstPageSets =
        firstPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 3: Get second page (count=2, offset=2)
    UriComponentsBuilder secondPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2")
            .queryParam("_offset", "2");

    content =
        this.restTemplate.getForObject(secondPageBuilder.build().encode().toUri(), String.class);
    Bundle secondPageData = parser.parseResource(Bundle.class, content);
    List<Resource> secondPageSets =
        secondPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 4: Try to exceed maximum count (should return all)
    UriComponentsBuilder maxExceededBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "10000");

    content =
        this.restTemplate.getForObject(maxExceededBuilder.build().encode().toUri(), String.class);
    Bundle maxPageData = parser.parseResource(Bundle.class, content);
    List<Resource> maxPageSets =
        maxPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Assertions for pagination
    assertEquals(2, firstPageSets.size());
    assertEquals(2, secondPageSets.size());
    assertEquals(4, firstPageSets.size() + secondPageSets.size());
    assertTrue(defaultValueSets.size() <= maxPageSets.size());

    // Verify that concatenated pages equal first 4 of full results
    List<String> fourIds =
        defaultValueSets.subList(0, 4).stream()
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    List<String> paginatedIds =
        Stream.concat(firstPageSets.stream(), secondPageSets.stream())
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    assertEquals(fourIds, paginatedIds);

    // Verify content of individual resources
    for (Resource cs : defaultValueSets) {
      validateValueSetPagination((ValueSet) cs);
    }
  }

  /**
   * Validate value set pagination.
   *
   * @param vs the vs
   */
  private void validateValueSetPagination(ValueSet vs) {
    assertNotNull(vs);
    assertEquals(ResourceType.ValueSet, vs.getResourceType());
    assertNotNull(vs.getIdPart());
    assertNotNull(vs.getPublisher());
    assertNotNull(vs.getUrl());

    // Log for debugging
    log.info(" value set = " + parser.encodeResourceToString(vs));

    // Verify specific IDs and URLs if needed
    if (vs.getIdPart().equals("ncit_25.06e")) {
      assertEquals("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs", vs.getUrl());
    }
  }

  /**
   * Test value set search variants with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetSearchVariantsWithParameters() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;

    // Test 1: Get initial list of ValueSets
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    ValueSet firstValueSet = (ValueSet) valueSets.get(0);
    String firstValueSetName = firstValueSet.getName();
    String firstValueSetTitle = firstValueSet.getTitle();

    // Test 2: Basic name search (without modifier)
    String basicNameUrl =
        endpoint + "?name=" + URLEncoder.encode(firstValueSetName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(basicNameUrl, String.class);
    Bundle basicNameBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(basicNameBundle.getEntry());
    assertFalse(basicNameBundle.getEntry().isEmpty());
    ValueSet basicMatchValueSet = (ValueSet) basicNameBundle.getEntry().get(0).getResource();
    assertEquals(firstValueSetName, basicMatchValueSet.getName());

    // Test 3: Exact match (case insensitive)
    String upperCaseName = firstValueSetName.toUpperCase();
    String exactMatchUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(upperCaseName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(exactMatchUrl, String.class);
    Bundle exactMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(exactMatchBundle.getEntry());
    assertFalse(exactMatchBundle.getEntry().isEmpty());
    ValueSet exactMatchValueSet = (ValueSet) exactMatchBundle.getEntry().get(0).getResource();
    assertTrue(exactMatchValueSet.getName().equalsIgnoreCase(upperCaseName));

    // Test 4: Contains search
    String partialName = firstValueSetName.substring(1, firstValueSetName.length() - 1);
    String containsUrl =
        endpoint + "?name:contains=" + URLEncoder.encode(partialName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(containsUrl, String.class);
    Bundle containsBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(containsBundle.getEntry());
    assertFalse(containsBundle.getEntry().isEmpty());
    boolean foundContainsMatch =
        containsBundle.getEntry().stream()
            .map(entry -> ((ValueSet) entry.getResource()).getName())
            .anyMatch(name -> name.toLowerCase().contains(partialName.toLowerCase()));
    assertTrue(foundContainsMatch);

    // Test 5: Starts with search
    String namePrefix = firstValueSetName.substring(0, 3);
    String startsWithUrl =
        endpoint + "?name:startsWith=" + URLEncoder.encode(namePrefix, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(startsWithUrl, String.class);
    Bundle startsWithBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(startsWithBundle.getEntry());
    assertFalse(startsWithBundle.getEntry().isEmpty());
    boolean foundStartsWithMatch =
        startsWithBundle.getEntry().stream()
            .map(entry -> ((ValueSet) entry.getResource()).getName())
            .anyMatch(name -> name.toLowerCase().startsWith(namePrefix.toLowerCase()));
    assertTrue(foundStartsWithMatch);

    // Test 6: Negative test - non-existent name
    String nonExistentName = "NonExistentValueSet" + UUID.randomUUID();
    String negativeTestUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(nonExistentName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(negativeTestUrl, String.class);
    Bundle emptyBundle = parser.parseResource(Bundle.class, content);

    assertTrue(emptyBundle.getEntry() == null || emptyBundle.getEntry().isEmpty());

    // Test 7: Title exact match (case insensitive)
    String upperCaseTitle = firstValueSetTitle.toUpperCase();
    String titleExactUrl =
        endpoint + "?title:exact=" + URLEncoder.encode(upperCaseTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleExactUrl, String.class);
    Bundle titleExactBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(titleExactBundle.getEntry());
    assertFalse(titleExactBundle.getEntry().isEmpty());
    ValueSet titleExactMatch = (ValueSet) titleExactBundle.getEntry().get(0).getResource();
    assertTrue(titleExactMatch.getTitle().equalsIgnoreCase(upperCaseTitle));

    // Test 8: Title starts with search
    String titlePrefix = firstValueSetTitle.substring(0, 3);
    String titleStartsWithUrl =
        endpoint + "?title:startsWith=" + URLEncoder.encode(titlePrefix, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleStartsWithUrl, String.class);
    Bundle titleStartsWithBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(titleStartsWithBundle.getEntry());
    assertFalse(titleStartsWithBundle.getEntry().isEmpty());
    boolean foundTitleStartsWithMatch =
        titleStartsWithBundle.getEntry().stream()
            .map(entry -> ((ValueSet) entry.getResource()).getTitle())
            .anyMatch(title -> title.toLowerCase().startsWith(titlePrefix.toLowerCase()));
    assertTrue(foundTitleStartsWithMatch);

    // Test 9: Title contains search
    String partialTitle = firstValueSetTitle.substring(1, firstValueSetTitle.length() - 1);
    String titleContainsUrl =
        endpoint + "?title:contains=" + URLEncoder.encode(partialTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleContainsUrl, String.class);
    Bundle titleContainsBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(titleContainsBundle.getEntry());
    assertFalse(titleContainsBundle.getEntry().isEmpty());
    boolean foundTitleContainsMatch =
        titleContainsBundle.getEntry().stream()
            .map(entry -> ((ValueSet) entry.getResource()).getTitle())
            .anyMatch(title -> title.toLowerCase().contains(partialTitle.toLowerCase()));
    assertTrue(foundTitleContainsMatch);

    // Test 10: All parameters test (original test case)
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e")
            .queryParam("name", "NCI Thesaurus 25.06e")
            .queryParam("title", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle allParamsData = parser.parseResource(Bundle.class, content);
    validateVariantValueSetResults(allParamsData, true);

    // Test 11: Contains search
    String cdiscName = "CDISC";
    containsUrl =
        endpoint + "?name:contains=" + URLEncoder.encode(cdiscName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(containsUrl, String.class);
    containsBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(containsBundle.getEntry());
    assertFalse(containsBundle.getEntry().isEmpty());
    foundContainsMatch =
        containsBundle.getEntry().stream()
            .map(entry -> ((ValueSet) entry.getResource()).getName())
            .anyMatch(name -> name.toLowerCase().contains(cdiscName.toLowerCase()));
    assertTrue(foundContainsMatch);
  }

  /**
   * Validate variant value set results.
   *
   * @param bundle the bundle
   * @param expectResults the expect results
   */
  private void validateVariantValueSetResults(Bundle bundle, boolean expectResults) {
    assertNotNull(bundle);
    if (expectResults) {
      assertFalse(bundle.getEntry() == null || bundle.getEntry().isEmpty());
      bundle
          .getEntry()
          .forEach(
              entry -> {
                ValueSet vs = (ValueSet) entry.getResource();
                assertNotNull(vs);
                assertEquals(ResourceType.ValueSet, vs.getResourceType());
              });
    } else {
      assertTrue(bundle.getEntry() == null || bundle.getEntry().isEmpty());
    }
  }

  @Test
  public void testCodeSystemHistory() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirVSPath;

    // Act - First get list of ValueSets to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstValueSetId = valueSets.get(0).getIdPart();

    // Act - Get history for the first ValueSet
    String historyEndpoint = endpoint + "/" + firstValueSetId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(historyBundle);
    assertEquals(ResourceType.Bundle, historyBundle.getResourceType());
    assertEquals(Bundle.BundleType.HISTORY, historyBundle.getType());
    assertFalse(historyBundle.getEntry().isEmpty());

    // Verify each entry in history is a ValueSet with the same ID
    for (Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      assertNotNull(entry.getResource());
      assertEquals(ResourceType.ValueSet, entry.getResource().getResourceType());

      ValueSet historyValueSet = (ValueSet) entry.getResource();
      assertEquals(firstValueSetId, historyValueSet.getIdPart());

      // Verify metadata is properly set
      assertNotNull(historyValueSet.getMeta());
      assertNotNull(historyValueSet.getMeta().getVersionId());
      assertNotNull(historyValueSet.getMeta().getLastUpdated());
    }
  }

  @Test
  public void testValueSetHistoryNotFound() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;
    String invalidId = "nonexistent-valueSet-id";
    String historyEndpoint = endpoint + "/" + invalidId + "/_history";

    String messageNotFound = "Value set not found = " + invalidId;
    String errorCode = "not-found";

    // Act
    String content = this.restTemplate.getForObject(historyEndpoint, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  @Test
  public void testValueSetVread() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirVSPath;

    // Act - First get list of ValueSets to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstValueSetId = valueSets.get(0).getIdPart();

    // Act - Get specific version (assuming version 1 exists)
    String versionEndpoint = endpoint + "/" + firstValueSetId + "/_history/1";
    content = this.restTemplate.getForObject(versionEndpoint, String.class);
    ValueSet versionedValueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(versionedValueSet);
    assertEquals(ResourceType.ValueSet, versionedValueSet.getResourceType());
    assertEquals(firstValueSetId, versionedValueSet.getIdPart());

    // Verify metadata
    assertNotNull(versionedValueSet.getMeta());
    assertNotNull(versionedValueSet.getMeta().getVersionId());
    assertNotNull(versionedValueSet.getMeta().getLastUpdated());

    // Compare with original ValueSet
    ValueSet originalValueSet = (ValueSet) valueSets.get(0);
    assertEquals(originalValueSet.getUrl(), versionedValueSet.getUrl());
    assertEquals(originalValueSet.getName(), versionedValueSet.getName());
    assertEquals(originalValueSet.getPublisher(), versionedValueSet.getPublisher());
  }

  @Test
  public void testValueSetVreadNotFound() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;
    String invalidId = "nonexistent-valueSet-id";
    String versionEndpoint = endpoint + "/" + invalidId + "/_history/1";

    // Act & Assert
    String messageNotFound = "Value set version not found: nonexistent-valueSet-id version 1";
    String errorCode = "not-found";

    // Act
    String content = this.restTemplate.getForObject(versionEndpoint, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  @Test
  public void testValueSetVreadInvalidVersion() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirVSPath;

    // Act - First get list of ValueSets to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstValueSetId = valueSets.get(0).getIdPart();

    // Act & Assert - Try to get a version that doesn't exist
    String invalidVersionEndpoint = endpoint + "/" + firstValueSetId + "/_history/999";
    String messageNotFound = "Value set version not found: " + firstValueSetId + " version 999";
    String errorCode = "not-found";

    // Act
    content = this.restTemplate.getForObject(invalidVersionEndpoint, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  @Test
  public void testValueSetHistoryMetadataConsistency() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirVSPath;

    // Act - Get a ValueSet and its history
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstValueSetId = valueSets.get(0).getIdPart();

    // Get history
    String historyEndpoint = endpoint + "/" + firstValueSetId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get current version
    content = this.restTemplate.getForObject(endpoint + "/" + firstValueSetId, String.class);
    ValueSet currentValueSet = parser.parseResource(ValueSet.class, content);

    // Assert - Verify history contains current version
    boolean foundCurrentVersion = false;
    for (Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      ValueSet historyVersion = (ValueSet) entry.getResource();
      if (currentValueSet.getUrl().equals(historyVersion.getUrl())
          && currentValueSet.getName().equals(historyVersion.getName())) {
        foundCurrentVersion = true;
        break;
      }
    }
    Assertions.assertTrue(foundCurrentVersion, "History should contain the current version");
  }

  @Test
  public void testValueSetVreadMatchesHistoryEntry() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirVSPath;

    // Act - Get history first
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstValueSetId = valueSets.get(0).getIdPart();

    String historyEndpoint = endpoint + "/" + firstValueSetId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get first version from history
    ValueSet firstHistoryVersion = (ValueSet) historyBundle.getEntry().get(0).getResource();
    String versionId = firstHistoryVersion.getMeta().getVersionId();

    // Act - Get the same version using vread
    String vreadEndpoint = endpoint + "/" + firstValueSetId + "/_history/" + versionId;
    content = this.restTemplate.getForObject(vreadEndpoint, String.class);
    ValueSet vreadValueSet = parser.parseResource(ValueSet.class, content);

    // Assert - Both should be identical
    // assertEquals(firstHistoryVersion.getId(), vreadValueSet.getId());
    assertEquals(firstHistoryVersion.getUrl(), vreadValueSet.getUrl());
    assertEquals(firstHistoryVersion.getName(), vreadValueSet.getName());
    assertEquals(firstHistoryVersion.getVersion(), vreadValueSet.getVersion());
    assertEquals(
        firstHistoryVersion.getMeta().getVersionId(), vreadValueSet.getMeta().getVersionId());
  }

  /**
   * Test value set search with parameters - date related tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetSearchWithDateFocus() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://seer.nci.nih.gov/CanMED.owl?fhir_vs")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    org.hl7.fhir.r5.model.Bundle data =
        parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
    validateCanmedValueSetResults(data, true); // Expecting results

    // Test 2: Invalid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2030-01") // Future date
            .queryParam("url", "http://seer.nci.nih.gov/CanMED.owl?fhir_vs")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
    validateCanmedValueSetResults(data, false); // Expecting no results

    // Test 3: Valid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "2025-06-01")
            .queryParam("url", "http://seer.nci.nih.gov/CanMED.owl?fhir_vs")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
    validateCanmedValueSetResults(data, true);

    // Test 4: Valid date range
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2023-11-01")
            .queryParam("url", "http://seer.nci.nih.gov/CanMED.owl?fhir_vs")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
    validateCanmedValueSetResults(data, true);

    // Test 5: Valid date range
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2023-11-01")
            .queryParam("date", "lt2030-11-01")
            .queryParam("url", "http://seer.nci.nih.gov/CanMED.owl?fhir_vs")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
    validateCanmedValueSetResults(data, true);
  }

  /**
   * Test value set search with sort by name.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearchSortByName() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "?_sort=name";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by name in ascending order
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(valueSets);

    String previousName = null;
    for (Resource vs : valueSets) {
      ValueSet vss = (ValueSet) vs;
      assertNotNull(vss.getName());
      String currentName = vss.getName().toLowerCase();
      if (previousName != null) {
        assertTrue(
            currentName.compareTo(previousName) >= 0,
            "Names should be in alphabetical order: '"
                + previousName
                + "' should come before '"
                + currentName
                + "'");
      }
      previousName = currentName;
    }
  }

  /**
   * Test value set search with sort by title descending.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearchSortByTitleDescending() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "?_sort=-title";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by title in descending order
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(valueSets);

    String previousTitle = null;
    for (Resource vs : valueSets) {
      ValueSet vss = (ValueSet) vs;
      assertNotNull(vss.getTitle());
      String currentTitle = vss.getTitle().toLowerCase();
      if (previousTitle != null) {
        assertTrue(
            currentTitle.compareTo(previousTitle) <= 0,
            "Titles should be in descending alphabetical order: '"
                + previousTitle
                + "' should come after '"
                + currentTitle
                + "'");
      }
      previousTitle = currentTitle;
    }
  }

  /**
   * Test value set search with sort by publisher.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearchSortByPublisher() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "?_sort=publisher";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by publisher in ascending order
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(valueSets);

    String previousPublisher = null;
    for (Resource vs : valueSets) {
      ValueSet vss = (ValueSet) vs;
      assertNotNull(vss.getPublisher());
      String currentPublisher = vss.getPublisher().toLowerCase();
      if (previousPublisher != null) {
        assertTrue(
            currentPublisher.compareTo(previousPublisher) >= 0,
            "Publishers should be in alphabetical order: '"
                + previousPublisher
                + "' should come before '"
                + currentPublisher
                + "'");
      }
      previousPublisher = currentPublisher;
    }
  }

  /**
   * Test value set search with sort by date.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearchSortByDate() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "?_sort=date";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by date in ascending order
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(valueSets);

    Date previousDate = null;
    for (Resource vs : valueSets) {
      ValueSet vss = (ValueSet) vs;
      Date currentDate = vss.getDate();
      if (previousDate != null && currentDate != null) {
        assertTrue(
            currentDate.compareTo(previousDate) >= 0,
            "Dates should be in chronological order: '"
                + previousDate
                + "' should come before '"
                + currentDate
                + "'");
      }
      if (currentDate != null) {
        previousDate = currentDate;
      }
    }
  }

  /**
   * Test value set search with sort by URL.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearchSortByUrl() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "?_sort=url";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by URL in ascending order
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(valueSets);

    String previousUrl = null;
    for (Resource vs : valueSets) {
      ValueSet vss = (ValueSet) vs;
      assertNotNull(vss.getUrl());
      String currentUrl = vss.getUrl().toLowerCase();
      if (previousUrl != null) {
        assertTrue(
            currentUrl.compareTo(previousUrl) >= 0,
            "URLs should be in alphabetical order: '"
                + previousUrl
                + "' should come before '"
                + currentUrl
                + "'");
      }
      previousUrl = currentUrl;
    }
  }

  /**
   * Test value set search with invalid sort field.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearchSortByInvalidField() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "?_sort=invalid_field";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);

    // Assert
    assertNotNull(outcome);
    assertNotNull(outcome.getIssue());
    assertFalse(outcome.getIssue().isEmpty());

    OperationOutcomeIssueComponent issue = outcome.getIssue().get(0);
    assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
    assertTrue(issue.getDiagnostics().contains("Unsupported sort field"));
  }
}
