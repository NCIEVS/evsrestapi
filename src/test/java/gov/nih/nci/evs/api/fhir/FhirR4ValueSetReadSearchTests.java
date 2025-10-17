package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ValueSet;
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
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Class tests for FhirR4Tests. Tests the functionality of the FHIR R4 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR4ValueSetReadSearchTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4ValueSetReadSearchTests.class);

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** The fhir VS path. */
  private final String fhirVSPath = "/fhir/r4/ValueSet";

  /** The parser. */
  private static IParser parser;

  /** Sets the up once. */
  @BeforeAll
  public static void setUpOnce() {
    // Instantiate parser
    parser = FhirContext.forR4().newJsonParser();
  }

  /** Sets the up. */
  @BeforeEach
  public void setUp() {
    // The object mapper
    objectMapper = new ObjectMapper();
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
    final String endpoint = localHost + port + fhirVSPath + "?_count=2000";

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    final Set<String> ids = new HashSet<>(Set.of("ncit_25.06e", "ncit_c61410"));
    final Set<String> urls =
        new HashSet<>(
            Set.of(
                "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs",
                "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C61410"));

    // Assert
    assertFalse(valueSets.isEmpty());
    for (final Resource vs : valueSets) {
      log.info("  value set = " + parser.encodeResourceToString(vs));
      final ValueSet vss = (ValueSet) vs;
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
  }

  /**
   * Test value set read.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetRead() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Act
    final String firstValueSetId = valueSets.get(0).getIdPart();
    // reassign content
    content = this.restTemplate.getForObject(endpoint + "/" + firstValueSetId, String.class);
    final ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertEquals(ResourceType.ValueSet, valueSet.getResourceType());
    assertEquals(firstValueSetId, valueSet.getIdPart());
    assertEquals(valueSet.getUrl(), ((ValueSet) valueSets.get(0)).getUrl());
    assertEquals(valueSet.getName(), ((ValueSet) valueSets.get(0)).getName());
    assertEquals(valueSet.getPublisher(), ((ValueSet) valueSets.get(0)).getPublisher());
  }

  /**
   * Test value set read static id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetReadStaticId() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    final String valueSetId = "umlssemnet_2023aa";

    content = this.restTemplate.getForObject(endpoint + "/" + valueSetId, String.class);
    final ValueSet valueSet = parser.parseResource(ValueSet.class, content);

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
    final String endpoint = localHost + port + fhirVSPath;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    final String invalidId = "invalid_id";
    final String messageNotFound = "Value set not found = " + invalidId;
    final String errorCode = "not-found";

    content = this.restTemplate.getForObject(endpoint + "/" + invalidId, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
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
    final String code = "ncit_c129091";
    final String name = "CDISC Questionnaire NCCN-FACT FBLSI-18 Version 2 Test Name Terminology";
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C129091";
    final String publisher = "NCI";
    final String endpoint = localHost + port + fhirVSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + code, String.class);
    final ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertEquals(ResourceType.ValueSet, valueSet.getResourceType());
    assertEquals(code, valueSet.getIdPart());
    assertEquals(name, valueSet.getName());
    assertEquals(publisher, valueSet.getPublisher());
    assertEquals(url, valueSet.getUrl());
  }

  /**
   * Test value set subset search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetSubsetSearchWithParameters() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath;

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
    // Arrange
    final String endpoint = localHost + port + fhirVSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e") // .queryParam("code",
            // "C61410")
            .queryParam("name", "NCI Thesaurus 25.06e")
            .queryParam("title", "ncit")
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
            .queryParam("name", "NCI Thesaurus 25.06e")
            .queryParam("title", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 3: Invalid code
    // builder =
    // UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
    // .queryParam("_id", "ncit_25.06e").queryParam("code", "INVALID_CODE")
    // .queryParam("name", "NCI Thesaurus 25.06e").queryParam("title", "ncit")
    // .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
    // .queryParam("version", "25.06e");
    //
    // content =
    // this.restTemplate.getForObject(builder.build().encode().toUri(),
    // String.class);
    // data = parser.parseResource(Bundle.class, content);
    // validateValueSetResults(data, false);

    // Test 4: Invalid name
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e") // .queryParam("code", "C61410")
            .queryParam("name", "Invalid Name")
            .queryParam("title", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 5: Invalid title
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e") // .queryParam("code", "C61410")
            .queryParam("name", "NCI Thesaurus 25.06e")
            .queryParam("title", "invalid_title")
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
            .queryParam("name", "NCI Thesaurus 25.06e")
            .queryParam("title", "ncit")
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
            .queryParam("name", "NCI Thesaurus 25.06e")
            .queryParam("title", "ncit")
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
  private void validateValueSetResults(final Bundle data, final boolean expectResults) {
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(valueSets.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ncit_25.06e"));
      final Set<String> urls =
          new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs"));

      for (final Resource vs : valueSets) {
        log.info(" value set = " + parser.encodeResourceToString(vs));
        final ValueSet vss = (ValueSet) vs;
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

  private void validateCanmedValueSetResults(final Bundle data, final boolean expectResults) {
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(valueSets.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("canmed_202506"));
      final Set<String> urls = new HashSet<>(Set.of("http://seer.nci.nih.gov/CanMED.owl?fhir_vs"));

      for (final Resource vs : valueSets) {
        log.info(" value set = " + parser.encodeResourceToString(vs));
        final ValueSet vss = (ValueSet) vs;
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
   * Validate value set subset results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  private void validateValueSetSubsetResults(final Bundle data, final boolean expectResults) {
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(valueSets.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ncit_c100110"));
      final Set<String> urls =
          new HashSet<>(
              Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110"));

      for (final Resource vs : valueSets) {
        log.info(" value set = " + parser.encodeResourceToString(vs));
        final ValueSet vss = (ValueSet) vs;
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
    final String endpoint = localHost + port + fhirVSPath;

    // Test 1: Get default results without pagination
    String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle defaultData = parser.parseResource(Bundle.class, content);
    final List<Resource> defaultValueSets =
        defaultData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 2: Get first page (count=2)
    final UriComponentsBuilder firstPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2");

    content =
        this.restTemplate.getForObject(firstPageBuilder.build().encode().toUri(), String.class);
    final Bundle firstPageData = parser.parseResource(Bundle.class, content);
    final List<Resource> firstPageSets =
        firstPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 3: Get second page (count=2, offset=2)
    final UriComponentsBuilder secondPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2")
            .queryParam("_offset", "2");

    content =
        this.restTemplate.getForObject(secondPageBuilder.build().encode().toUri(), String.class);
    final Bundle secondPageData = parser.parseResource(Bundle.class, content);
    final List<Resource> secondPageSets =
        secondPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 4: Try to exceed maximum count (should return all)
    final UriComponentsBuilder maxExceededBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "10000");

    content =
        this.restTemplate.getForObject(maxExceededBuilder.build().encode().toUri(), String.class);
    final Bundle maxPageData = parser.parseResource(Bundle.class, content);
    final List<Resource> maxPageSets =
        maxPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Assertions for pagination
    assertEquals(2, firstPageSets.size());
    assertEquals(2, secondPageSets.size());
    assertEquals(4, firstPageSets.size() + secondPageSets.size());
    assertTrue(defaultValueSets.size() <= maxPageSets.size());

    // Verify that concatenated pages equal first 4 of full results
    final List<String> fourIds =
        defaultValueSets.subList(0, 4).stream()
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    final List<String> paginatedIds =
        Stream.concat(firstPageSets.stream(), secondPageSets.stream())
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    assertEquals(fourIds, paginatedIds);

    // Verify content of individual resources
    for (final Resource cs : defaultValueSets) {
      validateValueSetPagination((ValueSet) cs);
    }
  }

  /**
   * Validate value set pagination.
   *
   * @param vs the vs
   */
  private void validateValueSetPagination(final ValueSet vs) {
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
    final String endpoint = localHost + port + fhirVSPath;

    // Test 1: Get initial list of ValueSets
    String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> valueSets =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    final ValueSet firstValueSet = (ValueSet) valueSets.get(0);
    final String firstValueSetName = firstValueSet.getName();
    final String firstValueSetTitle = firstValueSet.getTitle();

    // Test 2: Basic name search (without modifier)
    final String basicNameUrl =
        endpoint + "?name=" + URLEncoder.encode(firstValueSetName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(basicNameUrl, String.class);
    final Bundle basicNameBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(basicNameBundle.getEntry());
    assertFalse(basicNameBundle.getEntry().isEmpty());
    final ValueSet basicMatchValueSet = (ValueSet) basicNameBundle.getEntry().get(0).getResource();
    assertEquals(firstValueSetName, basicMatchValueSet.getName());

    // Test 3: Exact match (case insensitive)
    final String upperCaseName = firstValueSetName.toUpperCase();
    final String exactMatchUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(upperCaseName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(exactMatchUrl, String.class);
    final Bundle exactMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(exactMatchBundle.getEntry());
    assertFalse(exactMatchBundle.getEntry().isEmpty());
    final ValueSet exactMatchValueSet = (ValueSet) exactMatchBundle.getEntry().get(0).getResource();
    assertTrue(exactMatchValueSet.getName().equalsIgnoreCase(upperCaseName));

    // Test 4: Contains search
    final String partialName = firstValueSetName.substring(1, firstValueSetName.length() - 1);
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
    final String namePrefix = firstValueSetName.substring(0, 3);
    final String startsWithUrl =
        endpoint + "?name:startsWith=" + URLEncoder.encode(namePrefix, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(startsWithUrl, String.class);
    final Bundle startsWithBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(startsWithBundle.getEntry());
    assertFalse(startsWithBundle.getEntry().isEmpty());
    final boolean foundStartsWithMatch =
        startsWithBundle.getEntry().stream()
            .map(entry -> ((ValueSet) entry.getResource()).getName())
            .anyMatch(name -> name.toLowerCase().startsWith(namePrefix.toLowerCase()));
    assertTrue(foundStartsWithMatch);

    // Test 6: Negative test - non-existent name
    final String nonExistentName = "NonExistentValueSet" + UUID.randomUUID();
    final String negativeTestUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(nonExistentName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(negativeTestUrl, String.class);
    final Bundle emptyBundle = parser.parseResource(Bundle.class, content);

    assertTrue(emptyBundle.getEntry() == null || emptyBundle.getEntry().isEmpty());

    // Test 7: Title exact match (case insensitive)
    final String upperCaseTitle = firstValueSetTitle.toUpperCase();
    final String titleExactUrl =
        endpoint + "?title:exact=" + URLEncoder.encode(upperCaseTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleExactUrl, String.class);
    final Bundle titleExactBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(titleExactBundle.getEntry());
    assertFalse(titleExactBundle.getEntry().isEmpty());
    final ValueSet titleExactMatch = (ValueSet) titleExactBundle.getEntry().get(0).getResource();
    assertTrue(titleExactMatch.getTitle().equalsIgnoreCase(upperCaseTitle));

    // Test 8: Title starts with search
    final String titlePrefix = firstValueSetTitle.substring(0, 3);
    final String titleStartsWithUrl =
        endpoint + "?title:startsWith=" + URLEncoder.encode(titlePrefix, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleStartsWithUrl, String.class);
    final Bundle titleStartsWithBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(titleStartsWithBundle.getEntry());
    assertFalse(titleStartsWithBundle.getEntry().isEmpty());
    final boolean foundTitleStartsWithMatch =
        titleStartsWithBundle.getEntry().stream()
            .map(entry -> ((ValueSet) entry.getResource()).getTitle())
            .anyMatch(title -> title.toLowerCase().startsWith(titlePrefix.toLowerCase()));
    assertTrue(foundTitleStartsWithMatch);

    // Test 9: Title contains search
    final String partialTitle = firstValueSetTitle.substring(1, firstValueSetTitle.length() - 1);
    final String titleContainsUrl =
        endpoint + "?title:contains=" + URLEncoder.encode(partialTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleContainsUrl, String.class);
    final Bundle titleContainsBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(titleContainsBundle.getEntry());
    assertFalse(titleContainsBundle.getEntry().isEmpty());
    final boolean foundTitleContainsMatch =
        titleContainsBundle.getEntry().stream()
            .map(entry -> ((ValueSet) entry.getResource()).getTitle())
            .anyMatch(title -> title.toLowerCase().contains(partialTitle.toLowerCase()));
    assertTrue(foundTitleContainsMatch);

    // Test 10: All parameters test (original test case)
    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2000")
            .queryParam("_id", "ncit_25.06e")
            .queryParam("name", "NCI Thesaurus 25.06e")
            .queryParam("title", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs")
            .queryParam("version", "25.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    final Bundle allParamsData = parser.parseResource(Bundle.class, content);
    validateVariantValueSetResults(allParamsData, true);

    // Test 11: Contains search
    final String cdiscName = "CDISC";
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
  private void validateVariantValueSetResults(final Bundle bundle, final boolean expectResults) {
    assertNotNull(bundle);
    if (expectResults) {
      assertFalse(bundle.getEntry() == null || bundle.getEntry().isEmpty());
      bundle
          .getEntry()
          .forEach(
              entry -> {
                final ValueSet vs = (ValueSet) entry.getResource();
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
    final String endpoint = localHost + port + fhirVSPath;

    // Act - First get list of ValueSets to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstValueSetId = valueSets.get(0).getIdPart();

    // Act - Get history for the first ValueSet
    final String historyEndpoint = endpoint + "/" + firstValueSetId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(historyBundle);
    assertEquals(ResourceType.Bundle, historyBundle.getResourceType());
    assertEquals(Bundle.BundleType.HISTORY, historyBundle.getType());
    assertFalse(historyBundle.getEntry().isEmpty());

    // Verify each entry in history is a ValueSet with the same ID
    for (final Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      assertNotNull(entry.getResource());
      assertEquals(ResourceType.ValueSet, entry.getResource().getResourceType());

      final ValueSet historyValueSet = (ValueSet) entry.getResource();
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
    final String endpoint = localHost + port + fhirVSPath;
    final String invalidId = "nonexistent-valueSet-id";
    final String historyEndpoint = endpoint + "/" + invalidId + "/_history";

    final String messageNotFound = "Value set not found = " + invalidId;
    final String errorCode = "not-found";

    // Act
    final String content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  @Test
  public void testValueSetVread() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirVSPath;

    // Act - First get list of ValueSets to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstValueSetId = valueSets.get(0).getIdPart();

    // Act - Get specific version (assuming version 1 exists)
    final String versionEndpoint = endpoint + "/" + firstValueSetId + "/_history/1";
    content = this.restTemplate.getForObject(versionEndpoint, String.class);
    final ValueSet versionedValueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(versionedValueSet);
    assertEquals(ResourceType.ValueSet, versionedValueSet.getResourceType());
    assertEquals(firstValueSetId, versionedValueSet.getIdPart());

    // Verify metadata
    assertNotNull(versionedValueSet.getMeta());
    assertNotNull(versionedValueSet.getMeta().getVersionId());
    assertNotNull(versionedValueSet.getMeta().getLastUpdated());

    // Compare with original ValueSet
    final ValueSet originalValueSet = (ValueSet) valueSets.get(0);
    assertEquals(originalValueSet.getUrl(), versionedValueSet.getUrl());
    assertEquals(originalValueSet.getName(), versionedValueSet.getName());
    assertEquals(originalValueSet.getPublisher(), versionedValueSet.getPublisher());
  }

  @Test
  public void testValueSetVreadNotFound() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath;
    final String invalidId = "nonexistent-valueSet-id";
    final String versionEndpoint = endpoint + "/" + invalidId + "/_history/1";

    // Act & Assert
    final String messageNotFound = "Value set version not found: nonexistent-valueSet-id version 1";
    final String errorCode = "not-found";

    // Act
    final String content = this.restTemplate.getForObject(versionEndpoint, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  @Test
  public void testValueSetVreadInvalidVersion() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirVSPath;

    // Act - First get list of ValueSets to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstValueSetId = valueSets.get(0).getIdPart();

    // Act & Assert - Try to get a version that doesn't exist
    final String invalidVersionEndpoint = endpoint + "/" + firstValueSetId + "/_history/999";
    final String messageNotFound =
        "Value set version not found: " + firstValueSetId + " version 999";
    final String errorCode = "not-found";

    // Act
    content = this.restTemplate.getForObject(invalidVersionEndpoint, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  @Test
  public void testValueSetHistoryMetadataConsistency() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirVSPath;

    // Act - Get a ValueSet and its history
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstValueSetId = valueSets.get(0).getIdPart();

    // Get history
    final String historyEndpoint = endpoint + "/" + firstValueSetId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get current version
    content = this.restTemplate.getForObject(endpoint + "/" + firstValueSetId, String.class);
    final ValueSet currentValueSet = parser.parseResource(ValueSet.class, content);

    // Assert - Verify history contains current version
    boolean foundCurrentVersion = false;
    for (final Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      final ValueSet historyVersion = (ValueSet) entry.getResource();
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
    final String endpoint = localHost + port + fhirVSPath;

    // Act - Get history first
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstValueSetId = valueSets.get(0).getIdPart();

    final String historyEndpoint = endpoint + "/" + firstValueSetId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get first version from history
    final ValueSet firstHistoryVersion = (ValueSet) historyBundle.getEntry().get(0).getResource();
    final String versionId = firstHistoryVersion.getMeta().getVersionId();

    // Act - Get the same version using vread
    final String vreadEndpoint = endpoint + "/" + firstValueSetId + "/_history/" + versionId;
    content = this.restTemplate.getForObject(vreadEndpoint, String.class);
    final ValueSet vreadValueSet = parser.parseResource(ValueSet.class, content);

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
    final String endpoint = localHost + port + fhirVSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://seer.nci.nih.gov/CanMED.owl?fhir_vs")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateCanmedValueSetResults(data, true); // Expecting results

    // Test 2: Invalid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2030-01") // Future date
            .queryParam("url", "http://seer.nci.nih.gov/CanMED.owl?fhir_vs")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCanmedValueSetResults(data, false); // Expecting no results

    // Test 3: Valid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "2025-06-01")
            .queryParam("url", "http://seer.nci.nih.gov/CanMED.owl?fhir_vs")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCanmedValueSetResults(data, true);

    // Test 4: Valid date range
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2023-11-01")
            .queryParam("url", "http://seer.nci.nih.gov/CanMED.owl?fhir_vs")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
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
    data = parser.parseResource(Bundle.class, content);
    validateCanmedValueSetResults(data, true);
  }

  @Test
  public void testValueSetSearchSortByName() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath + "?_sort=name&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ValueSet> valueSets =
        data.getEntry().stream()
            .map(entry -> (ValueSet) entry.getResource())
            .filter(vs -> vs.getName() != null)
            .toList();

    // Assert - verify ascending sort
    assertFalse(valueSets.isEmpty());
    for (int i = 1; i < valueSets.size(); i++) {
      final String previousName = valueSets.get(i - 1).getName().toLowerCase();
      final String currentName = valueSets.get(i).getName().toLowerCase();
      assertTrue(
          previousName.compareTo(currentName) <= 0,
          String.format(
              "ValueSets not sorted by name: '%s' should come before or equal to '%s'",
              previousName, currentName));
    }
  }

  @Test
  public void testValueSetSearchSortByNameDescending() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath + "?_sort=-name&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ValueSet> valueSets =
        data.getEntry().stream()
            .map(entry -> (ValueSet) entry.getResource())
            .filter(vs -> vs.getName() != null)
            .toList();

    // Assert - verify descending sort
    assertFalse(valueSets.isEmpty());
    for (int i = 1; i < valueSets.size(); i++) {
      final String previousName = valueSets.get(i - 1).getName().toLowerCase();
      final String currentName = valueSets.get(i).getName().toLowerCase();
      assertTrue(
          previousName.compareTo(currentName) >= 0,
          String.format(
              "ValueSets not sorted by name descending: '%s' should come after or equal to '%s'",
              previousName, currentName));
    }
  }

  @Test
  public void testValueSetSearchSortByTitle() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath + "?_sort=title&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ValueSet> valueSets =
        data.getEntry().stream()
            .map(entry -> (ValueSet) entry.getResource())
            .filter(vs -> vs.getTitle() != null)
            .toList();

    // Assert - verify ascending sort
    assertFalse(valueSets.isEmpty());
    for (int i = 1; i < valueSets.size(); i++) {
      final String previousTitle = valueSets.get(i - 1).getTitle().toLowerCase();
      final String currentTitle = valueSets.get(i).getTitle().toLowerCase();
      assertTrue(
          previousTitle.compareTo(currentTitle) <= 0,
          String.format(
              "ValueSets not sorted by title: '%s' should come before or equal to '%s'",
              previousTitle, currentTitle));
    }
  }

  @Test
  public void testValueSetSearchSortByTitleDescending() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath + "?_sort=-title&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ValueSet> valueSets =
        data.getEntry().stream()
            .map(entry -> (ValueSet) entry.getResource())
            .filter(vs -> vs.getTitle() != null)
            .toList();

    // Assert - verify descending sort
    assertFalse(valueSets.isEmpty());
    for (int i = 1; i < valueSets.size(); i++) {
      final String previousTitle = valueSets.get(i - 1).getTitle().toLowerCase();
      final String currentTitle = valueSets.get(i).getTitle().toLowerCase();
      assertTrue(
          previousTitle.compareTo(currentTitle) >= 0,
          String.format(
              "ValueSets not sorted by title descending: '%s' should come after or equal to '%s'",
              previousTitle, currentTitle));
    }
  }

  @Test
  public void testValueSetSearchSortByPublisher() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath + "?_sort=publisher&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ValueSet> valueSets =
        data.getEntry().stream()
            .map(entry -> (ValueSet) entry.getResource())
            .filter(vs -> vs.getPublisher() != null)
            .toList();

    // Assert - verify ascending sort
    assertFalse(valueSets.isEmpty());
    for (int i = 1; i < valueSets.size(); i++) {
      final String previousPublisher = valueSets.get(i - 1).getPublisher().toLowerCase();
      final String currentPublisher = valueSets.get(i).getPublisher().toLowerCase();
      assertTrue(
          previousPublisher.compareTo(currentPublisher) <= 0,
          String.format(
              "ValueSets not sorted by publisher: '%s' should come before or equal to '%s'",
              previousPublisher, currentPublisher));
    }
  }

  @Test
  public void testValueSetSearchSortInvalidField() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirVSPath + "?_sort=invalid_field";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);

    // Assert
    assertNotNull(outcome);
    assertEquals("invalid", outcome.getIssueFirstRep().getCode().toCode());
    assertTrue(outcome.getIssueFirstRep().getDiagnostics().contains("Unsupported sort field"));
  }
}
