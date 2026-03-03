package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
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
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
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
public class FhirR4ConceptMapReadSearchTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4ConceptMapReadSearchTests.class);

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** The fhir CM path. */
  private final String fhirCMPath = "/fhir/r4/ConceptMap";

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
    // n/a
  }

  /**
   * Test concept map search.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapSearch() throws Exception {
    // Arrange
    final String content =
        this.restTemplate.getForObject(localHost + port + fhirCMPath, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);

    // Act
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Verify things about this one
    // {"resourceType":"ConceptMap","id":"ma_to_ncit_mapping_July2021","url":"http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping","version":"Aug2024","name":"NCIt_to_HGNC_Mapping","name":"NCIt_to_HGNC_Mapping","status":"active","experimental":false,"publisher":"NCI","group":[{"source":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","target":"http://www.genenames.org"}]}
    final Set<String> ids = new HashSet<>(Set.of("icd10_to_meddra_mapping_july2021"));
    final Set<String> urls =
        new HashSet<>(Set.of("http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping"));

    // Assert
    assertFalse(conceptMaps.isEmpty());
    for (final Resource cm : conceptMaps) {
      log.info("  concept map = " + parser.encodeResourceToString(cm));
      final ConceptMap cmm = (ConceptMap) cm;
      assertNotNull(cmm);
      assertEquals(ResourceType.ConceptMap, cmm.getResourceType());
      assertNotNull(cmm.getIdPart());
      assertNotNull(cmm.getGroup());
      assertNotNull(cmm.getVersion());
      assertNotNull(cmm.getUrl());
      ids.remove(cmm.getIdPart());
      urls.remove(cmm.getUrl());
    }
    assertThat(ids).isEmpty();
    assertThat(urls).isEmpty();
  }

  /**
   * Test concept map read.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapRead() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Act
    final String firstConceptMapId = conceptMaps.get(0).getIdPart();
    // reassign content
    content = this.restTemplate.getForObject(endpoint + "/" + firstConceptMapId, String.class);
    final ConceptMap conceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert
    assertNotNull(conceptMap);
    assertEquals(ResourceType.ConceptMap, conceptMap.getResourceType());
    assertEquals(firstConceptMapId, conceptMap.getIdPart());
    assertEquals(conceptMap.getUrl(), ((ConceptMap) conceptMaps.get(0)).getUrl());
    assertEquals(conceptMap.getName(), ((ConceptMap) conceptMaps.get(0)).getName());
    assertEquals(conceptMap.getVersion(), ((ConceptMap) conceptMaps.get(0)).getVersion());
  }

  /**
   * Test concept map read static id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapReadStaticId() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;
    final String conceptMapId = "icd10_to_meddra_mapping_july2021";

    // Act
    final String content =
        this.restTemplate.getForObject(endpoint + "/" + conceptMapId, String.class);
    final ConceptMap conceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert
    assertNotNull(conceptMap);
    assertEquals(ResourceType.ConceptMap, conceptMap.getResourceType());
    assertEquals(conceptMapId, conceptMap.getIdPart());
    assertEquals(
        conceptMap.getUrl(), "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping");
    assertEquals(conceptMap.getName(), "ICD10_to_MedDRA_Mapping");
    assertEquals(conceptMap.getVersion(), "July2021");
    assertEquals(conceptMap.getPublisher(), "World Health Organization");
    assertEquals(conceptMap.getStatus().toCode(), "active");
    assertEquals(conceptMap.getExperimental(), false);
    assertEquals(conceptMap.getSourceUriType().asStringValue(), "http://hl7.org/fhir/sid/icd-10");
    assertEquals(conceptMap.getTargetUriType().asStringValue(), "https://www.meddra.org");
    assertEquals(conceptMap.getTitle(), "ICD10_to_MedDRA_Mapping");
  }

  /**
   * Test concept map read bad id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapReadBadId() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;
    final String invalidId = "invalid_id";
    final String messageNotFound = "Concept map not found = " + invalidId;
    final String errorCode = "not-found";

    // Act
    final String content = this.restTemplate.getForObject(endpoint + "/" + invalidId, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test concept map search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapSearchWithParameters() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "icd10_to_meddra_mapping_july2021") // .queryParam("date",
            // "2024-08")
            .queryParam("name", "ICD10_to_MedDRA_Mapping")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, true); // Expecting results

    // Test 2: Invalid ID
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "invalid_id")
            // .queryParam("date", "2024-08")
            .queryParam("name", "ICD10_to_MedDRA_Mapping")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 3: Invalid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "icd10_to_meddra_mapping_july2021")
            .queryParam("date", "2028-08")
            .queryParam("name", "ICD10_to_MedDRA_Mapping")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 4: Invalid name
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "icd10_to_meddra_mapping_july2021")
            // .queryParam("date", "2024-08")
            .queryParam("name", "Invalid_Name")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 5: Invalid URL
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "icd10_to_meddra_mapping_july2021")
            // .queryParam("date", "2024-08")
            .queryParam("name", "ICD10_to_MedDRA_Mapping")
            .queryParam("url", "http://invalid.url")
            .queryParam("version", "July2021");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 6: Invalid version
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "icd10_to_meddra_mapping_july2021")
            // .queryParam("date", "2024-08")
            .queryParam("name", "ICD10_to_MedDRA_Mapping")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "invalid_version");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);
  }

  /**
   * Validate concept map results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  // Helper method to validate results
  private void validateConceptMapResults(final Bundle data, final boolean expectResults) {
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(conceptMaps.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("icd10_to_meddra_mapping_july2021"));
      final Set<String> urls =
          new HashSet<>(Set.of("http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping"));

      for (final Resource cm : conceptMaps) {
        log.info(" concept map = " + parser.encodeResourceToString(cm));
        final ConceptMap cmm = (ConceptMap) cm;
        assertNotNull(cmm);
        assertEquals(ResourceType.ConceptMap, cmm.getResourceType());
        assertNotNull(cmm.getIdPart());
        assertNotNull(cmm.getGroup());
        assertNotNull(cmm.getVersion());
        assertNotNull(cmm.getUrl());
        ids.remove(cmm.getIdPart());
        urls.remove(cmm.getUrl());
      }
      assertThat(ids).isEmpty();
      assertThat(urls).isEmpty();
    } else {
      assertTrue(data.getEntry().isEmpty() || conceptMaps.isEmpty());
    }
  }

  /**
   * Test concept map search pagination.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapSearchPagination() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    // Test 1: Get default results without pagination
    String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle defaultData = parser.parseResource(Bundle.class, content);
    final List<Resource> defaultConceptMaps =
        defaultData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 2: Get first page (count=2)
    final UriComponentsBuilder firstPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2");

    content =
        this.restTemplate.getForObject(firstPageBuilder.build().encode().toUri(), String.class);
    final Bundle firstPageData = parser.parseResource(Bundle.class, content);
    final List<Resource> firstPageMaps =
        firstPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 3: Get second page (count=2, offset=2)
    final UriComponentsBuilder secondPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2")
            .queryParam("_offset", "2");

    content =
        this.restTemplate.getForObject(secondPageBuilder.build().encode().toUri(), String.class);
    final Bundle secondPageData = parser.parseResource(Bundle.class, content);
    final List<Resource> secondPageMaps =
        secondPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 4: Try to exceed maximum count (should return all)
    final UriComponentsBuilder maxExceededBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "10000");

    content =
        this.restTemplate.getForObject(maxExceededBuilder.build().encode().toUri(), String.class);
    final Bundle maxPageData = parser.parseResource(Bundle.class, content);
    final List<Resource> maxPageMaps =
        maxPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Assertions for pagination
    assertEquals(2, firstPageMaps.size());
    assertEquals(2, secondPageMaps.size());
    assertEquals(4, firstPageMaps.size() + secondPageMaps.size());
    assertTrue(defaultConceptMaps.size() <= maxPageMaps.size());

    // Verify that concatenated pages equal first 4 of full results
    final List<String> fourIds =
        defaultConceptMaps.subList(0, 4).stream()
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    final List<String> paginatedIds =
        Stream.concat(firstPageMaps.stream(), secondPageMaps.stream())
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    assertEquals(fourIds, paginatedIds);

    // Verify content of individual resources
    for (final Resource cs : defaultConceptMaps) {
      validateConceptMapPagination((ConceptMap) cs);
    }
  }

  /**
   * Validate concept map pagination.
   *
   * @param cm the cm
   */
  private void validateConceptMapPagination(final ConceptMap cm) {
    assertNotNull(cm);
    assertEquals(ResourceType.ConceptMap, cm.getResourceType());
    assertNotNull(cm.getIdPart());
    assertNotNull(cm.getPublisher());
    assertNotNull(cm.getUrl());

    // Log for debugging
    log.info(" concept map = " + parser.encodeResourceToString(cm));

    // Verify specific IDs and URLs if needed
    if (cm.getIdPart().equals("icd10_to_meddra_mapping_july2021")) {
      assertEquals("http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping", cm.getUrl());
    }
  }

  /**
   * Test concept map search variants with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapSearchVariantsWithParameters() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    // Test 1: Get initial list of ConceptMaps
    String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    final ConceptMap firstConceptMap = (ConceptMap) conceptMaps.get(0);
    final String firstConceptMapName = firstConceptMap.getName();

    // Test 2: Basic name search (without modifier)
    final String basicNameUrl =
        endpoint + "?name=" + URLEncoder.encode(firstConceptMapName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(basicNameUrl, String.class);
    final Bundle basicNameBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(basicNameBundle.getEntry());
    assertFalse(basicNameBundle.getEntry().isEmpty());
    final ConceptMap basicMatchMap = (ConceptMap) basicNameBundle.getEntry().get(0).getResource();
    assertEquals(firstConceptMapName, basicMatchMap.getName());

    // Test 3: Exact match (case insensitive)
    final String upperCaseName = firstConceptMapName.toUpperCase();
    final String exactMatchUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(upperCaseName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(exactMatchUrl, String.class);
    final Bundle exactMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(exactMatchBundle.getEntry());
    assertFalse(exactMatchBundle.getEntry().isEmpty());
    final ConceptMap exactMatchMap = (ConceptMap) exactMatchBundle.getEntry().get(0).getResource();
    assertTrue(exactMatchMap.getName().equalsIgnoreCase(upperCaseName));

    // Test 4: Contains search
    final String partialName = firstConceptMapName.substring(1, firstConceptMapName.length() - 1);
    final String containsUrl =
        endpoint + "?name:contains=" + URLEncoder.encode(partialName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(containsUrl, String.class);
    final Bundle containsBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(containsBundle.getEntry());
    assertFalse(containsBundle.getEntry().isEmpty());
    final boolean foundContainsMatch =
        containsBundle.getEntry().stream()
            .map(entry -> ((ConceptMap) entry.getResource()).getName())
            .anyMatch(name -> name.toLowerCase().contains(partialName.toLowerCase()));
    assertTrue(foundContainsMatch);

    // Test 5: Starts with search
    final String namePrefix = firstConceptMapName.substring(0, 3);
    final String startsWithUrl =
        endpoint + "?name:startsWith=" + URLEncoder.encode(namePrefix, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(startsWithUrl, String.class);
    final Bundle startsWithBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(startsWithBundle.getEntry());
    assertFalse(startsWithBundle.getEntry().isEmpty());
    final boolean foundStartsWithMatch =
        startsWithBundle.getEntry().stream()
            .map(entry -> ((ConceptMap) entry.getResource()).getName())
            .anyMatch(name -> name.toLowerCase().startsWith(namePrefix.toLowerCase()));
    assertTrue(foundStartsWithMatch);

    // Test 6: Negative test - non-existent name
    final String nonExistentName = "NonExistentConceptMap" + UUID.randomUUID();
    final String negativeTestUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(nonExistentName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(negativeTestUrl, String.class);
    final Bundle emptyBundle = parser.parseResource(Bundle.class, content);

    assertTrue(emptyBundle.getEntry() == null || emptyBundle.getEntry().isEmpty());

    // Test 7: Mixed case search (to verify case insensitivity)
    final String mixedCaseName =
        firstConceptMapName.substring(0, firstConceptMapName.length() / 2).toLowerCase()
            + firstConceptMapName.substring(firstConceptMapName.length() / 2).toUpperCase();
    final String mixedCaseUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(mixedCaseName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(mixedCaseUrl, String.class);
    final Bundle mixedCaseBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(mixedCaseBundle.getEntry());
    assertFalse(mixedCaseBundle.getEntry().isEmpty());
    final ConceptMap mixedCaseMatch = (ConceptMap) mixedCaseBundle.getEntry().get(0).getResource();
    assertTrue(mixedCaseMatch.getName().equalsIgnoreCase(mixedCaseName));

    // Test 8: All parameters test (original test case)
    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "icd10_to_meddra_mapping_july2021")
            .queryParam("name", "ICD10_to_MedDRA_Mapping")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    final Bundle allParamsData = parser.parseResource(Bundle.class, content);
    validateVariantConceptMapResults(allParamsData, true);
  }

  /**
   * Validate variant concept map results.
   *
   * @param bundle the bundle
   * @param expectResults the expect results
   */
  private void validateVariantConceptMapResults(final Bundle bundle, final boolean expectResults) {
    assertNotNull(bundle);
    if (expectResults) {
      assertFalse(bundle.getEntry() == null || bundle.getEntry().isEmpty());
      bundle
          .getEntry()
          .forEach(
              entry -> {
                final ConceptMap cm = (ConceptMap) entry.getResource();
                assertNotNull(cm);
                assertEquals(ResourceType.ConceptMap, cm.getResourceType());
              });
    } else {
      assertTrue(bundle.getEntry() == null || bundle.getEntry().isEmpty());
    }
  }

  @Test
  public void testConceptMapHistory() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCMPath;

    // Act - First get list of ConceptMaps to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstConceptMapId = conceptMaps.get(0).getIdPart();

    // Act - Get history for the first ConceptMap
    final String historyEndpoint = endpoint + "/" + firstConceptMapId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(historyBundle);
    assertEquals(ResourceType.Bundle, historyBundle.getResourceType());
    assertEquals(Bundle.BundleType.HISTORY, historyBundle.getType());
    assertFalse(historyBundle.getEntry().isEmpty());

    // Verify each entry in history is a ConceptMap with the same ID
    for (final Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      assertNotNull(entry.getResource());
      assertEquals(ResourceType.ConceptMap, entry.getResource().getResourceType());

      final ConceptMap historyConceptMap = (ConceptMap) entry.getResource();
      assertEquals(firstConceptMapId, historyConceptMap.getIdPart());

      // Verify metadata is properly set
      assertNotNull(historyConceptMap.getMeta());
      assertNotNull(historyConceptMap.getMeta().getVersionId());
      assertNotNull(historyConceptMap.getMeta().getLastUpdated());
    }
  }

  @Test
  public void testConceptMapHistoryNotFound() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;
    final String invalidId = "nonexistent-conceptMap-id";
    final String historyEndpoint = endpoint + "/" + invalidId + "/_history";

    final String messageNotFound = "Concept map not found = " + invalidId;
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
  public void testConceptMapVread() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCMPath;

    // Act - First get list of ConceptMaps to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstConceptMapId = conceptMaps.get(0).getIdPart();

    // Act - Get specific version (assuming version 1 exists)
    final String versionEndpoint = endpoint + "/" + firstConceptMapId + "/_history/1";
    content = this.restTemplate.getForObject(versionEndpoint, String.class);
    final ConceptMap versionedConceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert
    assertNotNull(versionedConceptMap);
    assertEquals(ResourceType.ConceptMap, versionedConceptMap.getResourceType());
    assertEquals(firstConceptMapId, versionedConceptMap.getIdPart());

    // Verify metadata
    assertNotNull(versionedConceptMap.getMeta());
    assertNotNull(versionedConceptMap.getMeta().getVersionId());
    assertNotNull(versionedConceptMap.getMeta().getLastUpdated());

    // Compare with original ConceptMap
    final ConceptMap originalConceptMap = (ConceptMap) conceptMaps.get(0);
    assertEquals(originalConceptMap.getUrl(), versionedConceptMap.getUrl());
    assertEquals(originalConceptMap.getName(), versionedConceptMap.getName());
    assertEquals(originalConceptMap.getPublisher(), versionedConceptMap.getPublisher());
  }

  @Test
  public void testConceptMapVreadNotFound() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;
    final String invalidId = "nonexistent-conceptMap-id";
    final String versionEndpoint = endpoint + "/" + invalidId + "/_history/1";

    // Act & Assert
    final String messageNotFound =
        "Concept map version not found: nonexistent-conceptMap-id version 1";
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
  public void testConceptMapVreadInvalidVersion() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCMPath;

    // Act - First get list of ConceptMaps to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstConceptMapId = conceptMaps.get(0).getIdPart();

    // Act & Assert - Try to get a version that doesn't exist
    final String invalidVersionEndpoint = endpoint + "/" + firstConceptMapId + "/_history/999";
    final String messageNotFound =
        "Concept map version not found: " + firstConceptMapId + " version 999";
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
  public void testConceptMapHistoryMetadataConsistency() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCMPath;

    // Act - Get a ConceptMap and its history
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstConceptMapId = conceptMaps.get(0).getIdPart();

    // Get history
    final String historyEndpoint = endpoint + "/" + firstConceptMapId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get current version
    content = this.restTemplate.getForObject(endpoint + "/" + firstConceptMapId, String.class);
    final ConceptMap currentConceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert - Verify history contains current version
    boolean foundCurrentVersion = false;
    for (final Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      final ConceptMap historyVersion = (ConceptMap) entry.getResource();
      if (currentConceptMap.getUrl().equals(historyVersion.getUrl())
          && currentConceptMap.getName().equals(historyVersion.getName())) {
        foundCurrentVersion = true;
        break;
      }
    }
    Assertions.assertTrue(foundCurrentVersion, "History should contain the current version");
  }

  @Test
  public void testConceptMapVreadMatchesHistoryEntry() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCMPath;

    // Act - Get history first
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstConceptMapId = conceptMaps.get(0).getIdPart();

    final String historyEndpoint = endpoint + "/" + firstConceptMapId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get first version from history
    final ConceptMap firstHistoryVersion =
        (ConceptMap) historyBundle.getEntry().get(0).getResource();
    final String versionId = firstHistoryVersion.getMeta().getVersionId();

    // Act - Get the same version using vread
    final String vreadEndpoint = endpoint + "/" + firstConceptMapId + "/_history/" + versionId;
    content = this.restTemplate.getForObject(vreadEndpoint, String.class);
    final ConceptMap vreadConceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert - Both should be identical
    // assertEquals(firstHistoryVersion.getId(), vreadConceptMap.getId());
    assertEquals(firstHistoryVersion.getUrl(), vreadConceptMap.getUrl());
    assertEquals(firstHistoryVersion.getName(), vreadConceptMap.getName());
    assertEquals(firstHistoryVersion.getVersion(), vreadConceptMap.getVersion());
    assertEquals(
        firstHistoryVersion.getMeta().getVersionId(), vreadConceptMap.getMeta().getVersionId());
  }

  @Test
  public void testConceptMapSearchWithDateFocus() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            // .queryParam("_id", "icd10_to_meddra_mapping_july2021")
            .queryParam("name", "ICD10_to_MedDRA_Mapping")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    org.hl7.fhir.r4.model.Bundle data =
        parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, content);
    validateMaToNcitConceptMapResults(data, true); // Expecting results

    // Test 2: Invalid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2030-01") // Future date
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021")
            .queryParam("name", "ICD10_to_MedDRA_Mapping");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, content);
    validateMaToNcitConceptMapResults(data, false); // Expecting no results

    // Test 3: Valid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "2021-07-01")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021")
            .queryParam("name", "ICD10_to_MedDRA_Mapping");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, content);
    validateMaToNcitConceptMapResults(data, true);

    // Test 4: Valid date range
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2011-11-01")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021")
            .queryParam("name", "ICD10_to_MedDRA_Mapping");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r4.model.Bundle.class, content);
    validateMaToNcitConceptMapResults(data, true);

    // Test 5: Valid date range
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2011-11-01")
            .queryParam("date", "lt2030-08-01")
            .queryParam("url", "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping")
            .queryParam("version", "July2021")
            .queryParam("name", "ICD10_to_MedDRA_Mapping");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateMaToNcitConceptMapResults(data, true);
  }

  private void validateMaToNcitConceptMapResults(final Bundle data, final boolean expectResults) {
    final List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(conceptMaps.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("icd10_to_meddra_mapping_july2021"));
      final Set<String> urls =
          new HashSet<>(Set.of("http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping"));

      for (final Resource cm : conceptMaps) {
        log.info(" concept map = " + parser.encodeResourceToString(cm));
        final ConceptMap cmm = (ConceptMap) cm;
        assertNotNull(cmm);
        assertEquals(ResourceType.ConceptMap, cmm.getResourceType());
        assertNotNull(cmm.getIdPart());
        assertNotNull(cmm.getGroup());
        assertNotNull(cmm.getVersion());
        assertNotNull(cmm.getUrl());
        assertNotNull(cmm.getName());
        assertNotNull(cmm.getTitle());
        assertNotNull(cmm.getPublisher());
        assertNotNull(cmm.getStatus());
        assertNotNull(cmm.getExperimental());
        ids.remove(cmm.getIdPart());
        urls.remove(cmm.getUrl());
      }
      assertThat(ids).isEmpty();
      assertThat(urls).isEmpty();
    } else {
      assertTrue(data.getEntry().isEmpty() || conceptMaps.isEmpty());
    }
  }

  @Test
  public void testConceptMapSearchSortByName() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath + "?_sort=name&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ConceptMap> conceptMaps =
        data.getEntry().stream()
            .map(entry -> (ConceptMap) entry.getResource())
            .filter(cm -> cm.getName() != null)
            .toList();

    // Assert - verify ascending sort
    assertFalse(conceptMaps.isEmpty());
    for (int i = 1; i < conceptMaps.size(); i++) {
      final String previousName = conceptMaps.get(i - 1).getName().toLowerCase();
      final String currentName = conceptMaps.get(i).getName().toLowerCase();
      assertTrue(
          previousName.compareTo(currentName) <= 0,
          String.format(
              "ConceptMaps not sorted by name: '%s' should come before or equal to '%s'",
              previousName, currentName));
    }
  }

  @Test
  public void testConceptMapSearchSortByNameDescending() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath + "?_sort=-name&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ConceptMap> conceptMaps =
        data.getEntry().stream()
            .map(entry -> (ConceptMap) entry.getResource())
            .filter(cm -> cm.getName() != null)
            .toList();

    // Assert - verify descending sort
    assertFalse(conceptMaps.isEmpty());
    for (int i = 1; i < conceptMaps.size(); i++) {
      final String previousName = conceptMaps.get(i - 1).getName().toLowerCase();
      final String currentName = conceptMaps.get(i).getName().toLowerCase();
      assertTrue(
          previousName.compareTo(currentName) >= 0,
          String.format(
              "ConceptMaps not sorted by name descending: '%s' should come after or equal to '%s'",
              previousName, currentName));
    }
  }

  @Test
  public void testConceptMapSearchSortByTitle() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath + "?_sort=title&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ConceptMap> conceptMaps =
        data.getEntry().stream()
            .map(entry -> (ConceptMap) entry.getResource())
            .filter(cm -> cm.getTitle() != null)
            .toList();

    // Assert - verify ascending sort
    assertFalse(conceptMaps.isEmpty());
    for (int i = 1; i < conceptMaps.size(); i++) {
      final String previousTitle = conceptMaps.get(i - 1).getTitle().toLowerCase();
      final String currentTitle = conceptMaps.get(i).getTitle().toLowerCase();
      assertTrue(
          previousTitle.compareTo(currentTitle) <= 0,
          String.format(
              "ConceptMaps not sorted by title: '%s' should come before or equal to '%s'",
              previousTitle, currentTitle));
    }
  }

  @Test
  public void testConceptMapSearchSortByTitleDescending() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath + "?_sort=-title&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ConceptMap> conceptMaps =
        data.getEntry().stream()
            .map(entry -> (ConceptMap) entry.getResource())
            .filter(cm -> cm.getTitle() != null)
            .toList();

    // Assert - verify descending sort
    assertFalse(conceptMaps.isEmpty());
    for (int i = 1; i < conceptMaps.size(); i++) {
      final String previousTitle = conceptMaps.get(i - 1).getTitle().toLowerCase();
      final String currentTitle = conceptMaps.get(i).getTitle().toLowerCase();
      assertTrue(
          previousTitle.compareTo(currentTitle) >= 0,
          String.format(
              "ConceptMaps not sorted by title descending: '%s' should come after or equal to '%s'",
              previousTitle, currentTitle));
    }
  }

  @Test
  public void testConceptMapSearchSortByPublisher() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath + "?_sort=publisher&_count=100";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<ConceptMap> conceptMaps =
        data.getEntry().stream()
            .map(entry -> (ConceptMap) entry.getResource())
            .filter(cm -> cm.getPublisher() != null)
            .toList();

    // Assert - verify ascending sort
    assertFalse(conceptMaps.isEmpty());
    for (int i = 1; i < conceptMaps.size(); i++) {
      final String previousPublisher = conceptMaps.get(i - 1).getPublisher().toLowerCase();
      final String currentPublisher = conceptMaps.get(i).getPublisher().toLowerCase();
      assertTrue(
          previousPublisher.compareTo(currentPublisher) <= 0,
          String.format(
              "ConceptMaps not sorted by publisher: '%s' should come before or equal to '%s'",
              previousPublisher, currentPublisher));
    }
  }

  @Test
  public void testConceptMapSearchSortInvalidField() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath + "?_sort=invalid_field";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);

    // Assert
    assertNotNull(outcome);
    assertEquals("invalid", outcome.getIssueFirstRep().getCode().toCode());
    assertTrue(outcome.getIssueFirstRep().getDiagnostics().contains("Unsupported sort field"));
  }

  /**
   * Test concept map search with :missing modifier on string parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapSearchWithMissingModifierOnStringParams() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    // Test 1: Find ConceptMaps WITH a name (name:missing=false)
    String url = endpoint + "?name:missing=false&_count=100";
    String content = this.restTemplate.getForObject(url, String.class);
    Bundle bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should have a name
    assertNotNull(bundle);
    assertTrue(bundle.hasEntry(), "Should find ConceptMaps with names");
    for (BundleEntryComponent entry : bundle.getEntry()) {
      ConceptMap cm = (ConceptMap) entry.getResource();
      assertNotNull(cm.getName(), "ConceptMap should have a name when name:missing=false");
      assertFalse(cm.getName().isEmpty(), "ConceptMap name should not be empty");
    }

    // Test 2: Find ConceptMaps WITHOUT a name (name:missing=true)
    url = endpoint + "?name:missing=true&_count=100";
    content = this.restTemplate.getForObject(url, String.class);
    bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should NOT have a name
    assertNotNull(bundle);
    // Note: May be empty if all ConceptMaps have names
    for (BundleEntryComponent entry : bundle.getEntry()) {
      ConceptMap cm = (ConceptMap) entry.getResource();
      assertTrue(
          cm.getName() == null || cm.getName().isEmpty(),
          "ConceptMap should not have a name when name:missing=true");
    }
  }

  /**
   * Test concept map search with :missing modifier on URI parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapSearchWithUrlMissingModifier() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    // Test 1: Find ConceptMaps WITH a url (url:missing=false)
    String url = endpoint + "?url:missing=false&_count=100";
    String content = this.restTemplate.getForObject(url, String.class);
    Bundle bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should have a url
    assertNotNull(bundle);
    assertTrue(bundle.hasEntry(), "Should find ConceptMaps with urls");
    for (BundleEntryComponent entry : bundle.getEntry()) {
      ConceptMap cm = (ConceptMap) entry.getResource();
      assertNotNull(cm.getUrl(), "ConceptMap should have a url when url:missing=false");
      assertFalse(cm.getUrl().isEmpty(), "ConceptMap url should not be empty");
    }

    // Test 2: Find ConceptMaps WITHOUT a url (url:missing=true)
    url = endpoint + "?url:missing=true&_count=100";
    content = this.restTemplate.getForObject(url, String.class);
    bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should NOT have a url
    assertNotNull(bundle);
    // Note: May be empty if all ConceptMaps have urls
    for (BundleEntryComponent entry : bundle.getEntry()) {
      ConceptMap cm = (ConceptMap) entry.getResource();
      assertTrue(
          cm.getUrl() == null || cm.getUrl().isEmpty(),
          "ConceptMap should not have a url when url:missing=true");
    }
  }

  /**
   * Test concept map search with :contains modifier on URI parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapSearchWithUrlContainsModifier() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    // Test: Find ConceptMaps where URL contains "icd"
    String url = endpoint + "?url:contains=icd&_count=100";
    String content = this.restTemplate.getForObject(url, String.class);
    Bundle bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should have "icd" in their URL
    assertNotNull(bundle);
    if (bundle.hasEntry()) {
      for (BundleEntryComponent entry : bundle.getEntry()) {
        ConceptMap cm = (ConceptMap) entry.getResource();
        assertNotNull(cm.getUrl(), "ConceptMap should have a url");
        assertTrue(
            cm.getUrl().toLowerCase().contains("icd"),
            "ConceptMap url should contain 'icd': " + cm.getUrl());
      }
    }
  }

  /**
   * Test concept map search with :below modifier on URI parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapSearchWithUrlBelowModifier() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    // First, get a known ConceptMap with a URL
    String content = this.restTemplate.getForObject(endpoint + "?_count=1", String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    assertTrue(data.hasEntry(), "Should have at least one ConceptMap");
    ConceptMap firstCm = (ConceptMap) data.getEntry().get(0).getResource();
    String baseUrl = firstCm.getUrl();

    // Test: Find ConceptMaps where URL is at or below the base URL
    if (baseUrl != null && baseUrl.contains("/")) {
      // Get parent URL (remove last path segment)
      String parentUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/"));

      String url = endpoint + "?url:below=" + URLEncoder.encode(parentUrl, StandardCharsets.UTF_8);
      content = this.restTemplate.getForObject(url, String.class);
      Bundle bundle = parser.parseResource(Bundle.class, content);

      // Assert - all returned resources should have URL at or below parent URL
      assertNotNull(bundle);
      if (bundle.hasEntry()) {
        for (BundleEntryComponent entry : bundle.getEntry()) {
          ConceptMap cm = (ConceptMap) entry.getResource();
          assertNotNull(cm.getUrl(), "ConceptMap should have a url");
          assertTrue(
              cm.getUrl().startsWith(parentUrl) || cm.getUrl().equals(parentUrl),
              "ConceptMap url should be at or below '" + parentUrl + "': " + cm.getUrl());
        }
      }
    }
  }

  /**
   * Test concept map search with :above modifier on URI parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapSearchWithUrlAboveModifier() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCMPath;

    // First, get a known ConceptMap with a URL
    String content = this.restTemplate.getForObject(endpoint + "?_count=1", String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    assertTrue(data.hasEntry(), "Should have at least one ConceptMap");
    ConceptMap firstCm = (ConceptMap) data.getEntry().get(0).getResource();
    String childUrl = firstCm.getUrl();

    // Test: Find ConceptMaps where URL is at or above the child URL
    if (childUrl != null) {
      String url = endpoint + "?url:above=" + URLEncoder.encode(childUrl, StandardCharsets.UTF_8);
      content = this.restTemplate.getForObject(url, String.class);
      Bundle bundle = parser.parseResource(Bundle.class, content);

      // Assert - all returned resources should have URL at or above child URL
      assertNotNull(bundle);
      if (bundle.hasEntry()) {
        for (BundleEntryComponent entry : bundle.getEntry()) {
          ConceptMap cm = (ConceptMap) entry.getResource();
          assertNotNull(cm.getUrl(), "ConceptMap should have a url");
          assertTrue(
              childUrl.startsWith(cm.getUrl()) || childUrl.equals(cm.getUrl()),
              "Search url '"
                  + childUrl
                  + "' should be at or below ConceptMap url '"
                  + cm.getUrl()
                  + "'");
        }
      }
    }
  }
}
