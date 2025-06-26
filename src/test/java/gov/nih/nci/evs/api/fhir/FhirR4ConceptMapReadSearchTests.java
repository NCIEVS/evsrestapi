package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
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
public class FhirR4ConceptMapReadSearchTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4ConceptMapReadSearchTests.class);

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
    // The object mapper
    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  /**
   * Test concept map search.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapSearch() throws Exception {
    // Arrange
    String content = this.restTemplate.getForObject(localHost + port + fhirCMPath, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Act
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Verify things about this one
    // {"resourceType":"ConceptMap","id":"ma_to_ncit_mapping_november2011","url":"http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping","version":"Aug2024","name":"NCIt_to_HGNC_Mapping","title":"NCIt_to_HGNC_Mapping","status":"active","experimental":false,"publisher":"NCI","group":[{"source":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","target":"http://www.genenames.org"}]}
    final Set<String> ids = new HashSet<>(Set.of("ma_to_ncit_mapping_november2011"));
    final Set<String> urls =
        new HashSet<>(Set.of("http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping"));

    // Assert
    assertFalse(conceptMaps.isEmpty());
    for (Resource cm : conceptMaps) {
      log.info("  concept map = " + parser.encodeResourceToString(cm));
      ConceptMap cmm = (ConceptMap) cm;
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
    String endpoint = localHost + port + fhirCMPath;

    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Act
    String firstConceptMapId = conceptMaps.get(0).getIdPart();
    // reassign content
    content = this.restTemplate.getForObject(endpoint + "/" + firstConceptMapId, String.class);
    ConceptMap conceptMap = parser.parseResource(ConceptMap.class, content);

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
    String endpoint = localHost + port + fhirCMPath;
    String conceptMapId = "icd10_to_meddra_mapping_july2021";

    // Act
    String content = this.restTemplate.getForObject(endpoint + "/" + conceptMapId, String.class);
    ConceptMap conceptMap = parser.parseResource(ConceptMap.class, content);

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
    String endpoint = localHost + port + fhirCMPath;
    String invalidId = "invalid_id";
    String messageNotFound = "Concept map not found = " + invalidId;
    String errorCode = "not-found";

    // Act
    String content = this.restTemplate.getForObject(endpoint + "/" + invalidId, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    String endpoint = localHost + port + fhirCMPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "ma_to_ncit_mapping_november2011") // .queryParam("date",
            // "2024-08")
            .queryParam("name", "MA_to_NCIt_Mapping")
            .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
            .queryParam("version", "November2011");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, true); // Expecting results

    // Test 2: Invalid ID
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "invalid_id")
            // .queryParam("date", "2024-08")
            .queryParam("name", "MA_to_NCIt_Mapping")
            .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
            .queryParam("version", "November2011");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 3: Invalid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "ma_to_ncit_mapping_november2011")
            .queryParam("date", "2028-08")
            .queryParam("name", "MA_to_NCIt_Mapping")
            .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
            .queryParam("version", "November2011");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 4: Invalid name
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "ma_to_ncit_mapping_november2011")
            // .queryParam("date", "2024-08")
            .queryParam("name", "Invalid_Name")
            .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
            .queryParam("version", "November2011");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 5: Invalid URL
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "ma_to_ncit_mapping_november2011")
            // .queryParam("date", "2024-08")
            .queryParam("name", "MA_to_NCIt_Mapping")
            .queryParam("url", "http://invalid.url")
            .queryParam("version", "November2011");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 6: Invalid version
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "ma_to_ncit_mapping_november2011")
            // .queryParam("date", "2024-08")
            .queryParam("name", "MA_to_NCIt_Mapping")
            .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
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
  private void validateConceptMapResults(Bundle data, boolean expectResults) {
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(conceptMaps.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ma_to_ncit_mapping_november2011"));
      final Set<String> urls =
          new HashSet<>(
              Set.of("http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping"));

      for (Resource cm : conceptMaps) {
        log.info(" concept map = " + parser.encodeResourceToString(cm));
        ConceptMap cmm = (ConceptMap) cm;
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
    String endpoint = localHost + port + fhirCMPath;

    // Test 1: Get default results without pagination
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle defaultData = parser.parseResource(Bundle.class, content);
    List<Resource> defaultConceptMaps =
        defaultData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 2: Get first page (count=2)
    UriComponentsBuilder firstPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2");

    content =
        this.restTemplate.getForObject(firstPageBuilder.build().encode().toUri(), String.class);
    Bundle firstPageData = parser.parseResource(Bundle.class, content);
    List<Resource> firstPageMaps =
        firstPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 3: Get second page (count=2, offset=2)
    UriComponentsBuilder secondPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2")
            .queryParam("_offset", "2");

    content =
        this.restTemplate.getForObject(secondPageBuilder.build().encode().toUri(), String.class);
    Bundle secondPageData = parser.parseResource(Bundle.class, content);
    List<Resource> secondPageMaps =
        secondPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 4: Try to exceed maximum count (should return all)
    UriComponentsBuilder maxExceededBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "10000");

    content =
        this.restTemplate.getForObject(maxExceededBuilder.build().encode().toUri(), String.class);
    Bundle maxPageData = parser.parseResource(Bundle.class, content);
    List<Resource> maxPageMaps =
        maxPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Assertions for pagination
    assertEquals(2, firstPageMaps.size());
    assertEquals(2, secondPageMaps.size());
    assertEquals(4, firstPageMaps.size() + secondPageMaps.size());
    assertTrue(defaultConceptMaps.size() <= maxPageMaps.size());

    // Verify that concatenated pages equal first 4 of full results
    List<String> fourIds =
        defaultConceptMaps.subList(0, 4).stream()
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    List<String> paginatedIds =
        Stream.concat(firstPageMaps.stream(), secondPageMaps.stream())
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    assertEquals(fourIds, paginatedIds);

    // Verify content of individual resources
    for (Resource cs : defaultConceptMaps) {
      validateConceptMapPagination((ConceptMap) cs);
    }
  }

  /**
   * Validate concept map pagination.
   *
   * @param cm the cm
   */
  private void validateConceptMapPagination(ConceptMap cm) {
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
    String endpoint = localHost + port + fhirCMPath;

    // Test 1: Get initial list of ConceptMaps
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    ConceptMap firstConceptMap = (ConceptMap) conceptMaps.get(0);
    String firstConceptMapName = firstConceptMap.getName();

    // Test 2: Basic name search (without modifier)
    String basicNameUrl =
        endpoint + "?name=" + URLEncoder.encode(firstConceptMapName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(basicNameUrl, String.class);
    Bundle basicNameBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(basicNameBundle.getEntry());
    assertFalse(basicNameBundle.getEntry().isEmpty());
    ConceptMap basicMatchMap = (ConceptMap) basicNameBundle.getEntry().get(0).getResource();
    assertEquals(firstConceptMapName, basicMatchMap.getName());

    // Test 3: Exact match (case insensitive)
    String upperCaseName = firstConceptMapName.toUpperCase();
    String exactMatchUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(upperCaseName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(exactMatchUrl, String.class);
    Bundle exactMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(exactMatchBundle.getEntry());
    assertFalse(exactMatchBundle.getEntry().isEmpty());
    ConceptMap exactMatchMap = (ConceptMap) exactMatchBundle.getEntry().get(0).getResource();
    assertTrue(exactMatchMap.getName().equalsIgnoreCase(upperCaseName));

    // Test 4: Contains search
    String partialName = firstConceptMapName.substring(1, firstConceptMapName.length() - 1);
    String containsUrl =
        endpoint + "?name:contains=" + URLEncoder.encode(partialName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(containsUrl, String.class);
    Bundle containsBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(containsBundle.getEntry());
    assertFalse(containsBundle.getEntry().isEmpty());
    boolean foundContainsMatch =
        containsBundle.getEntry().stream()
            .map(entry -> ((ConceptMap) entry.getResource()).getName())
            .anyMatch(name -> name.toLowerCase().contains(partialName.toLowerCase()));
    assertTrue(foundContainsMatch);

    // Test 5: Starts with search
    String namePrefix = firstConceptMapName.substring(0, 3);
    String startsWithUrl =
        endpoint + "?name:startsWith=" + URLEncoder.encode(namePrefix, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(startsWithUrl, String.class);
    Bundle startsWithBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(startsWithBundle.getEntry());
    assertFalse(startsWithBundle.getEntry().isEmpty());
    boolean foundStartsWithMatch =
        startsWithBundle.getEntry().stream()
            .map(entry -> ((ConceptMap) entry.getResource()).getName())
            .anyMatch(name -> name.toLowerCase().startsWith(namePrefix.toLowerCase()));
    assertTrue(foundStartsWithMatch);

    // Test 6: Negative test - non-existent name
    String nonExistentName = "NonExistentConceptMap" + UUID.randomUUID();
    String negativeTestUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(nonExistentName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(negativeTestUrl, String.class);
    Bundle emptyBundle = parser.parseResource(Bundle.class, content);

    assertTrue(emptyBundle.getEntry() == null || emptyBundle.getEntry().isEmpty());

    // Test 7: Mixed case search (to verify case insensitivity)
    String mixedCaseName =
        firstConceptMapName.substring(0, firstConceptMapName.length() / 2).toLowerCase()
            + firstConceptMapName.substring(firstConceptMapName.length() / 2).toUpperCase();
    String mixedCaseUrl =
        endpoint + "?name:exact=" + URLEncoder.encode(mixedCaseName, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(mixedCaseUrl, String.class);
    Bundle mixedCaseBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(mixedCaseBundle.getEntry());
    assertFalse(mixedCaseBundle.getEntry().isEmpty());
    ConceptMap mixedCaseMatch = (ConceptMap) mixedCaseBundle.getEntry().get(0).getResource();
    assertTrue(mixedCaseMatch.getName().equalsIgnoreCase(mixedCaseName));

    // Test 8: All parameters test (original test case)
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_id", "ma_to_ncit_mapping_november2011")
            .queryParam("name", "MA_to_NCIt_Mapping")
            .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
            .queryParam("version", "November2011");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle allParamsData = parser.parseResource(Bundle.class, content);
    validateVariantConceptMapResults(allParamsData, true);
  }

  /**
   * Validate variant concept map results.
   *
   * @param bundle the bundle
   * @param expectResults the expect results
   */
  private void validateVariantConceptMapResults(Bundle bundle, boolean expectResults) {
    assertNotNull(bundle);
    if (expectResults) {
      assertFalse(bundle.getEntry() == null || bundle.getEntry().isEmpty());
      bundle
          .getEntry()
          .forEach(
              entry -> {
                ConceptMap cm = (ConceptMap) entry.getResource();
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
    String endpoint = localHost + port + fhirCMPath;

    // Act - First get list of ConceptMaps to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstConceptMapId = conceptMaps.get(0).getIdPart();

    // Act - Get history for the first ConceptMap
    String historyEndpoint = endpoint + "/" + firstConceptMapId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(historyBundle);
    assertEquals(ResourceType.Bundle, historyBundle.getResourceType());
    assertEquals(Bundle.BundleType.HISTORY, historyBundle.getType());
    assertFalse(historyBundle.getEntry().isEmpty());

    // Verify each entry in history is a ConceptMap with the same ID
    for (Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      assertNotNull(entry.getResource());
      assertEquals(ResourceType.ConceptMap, entry.getResource().getResourceType());

      ConceptMap historyConceptMap = (ConceptMap) entry.getResource();
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
    String endpoint = localHost + port + fhirCMPath;
    String invalidId = "nonexistent-conceptMap-id";
    String historyEndpoint = endpoint + "/" + invalidId + "/_history";

    String messageNotFound = "Concept map not found = " + invalidId;
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
  public void testConceptMapVread() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCMPath;

    // Act - First get list of ConceptMaps to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstConceptMapId = conceptMaps.get(0).getIdPart();

    // Act - Get specific version (assuming version 1 exists)
    String versionEndpoint = endpoint + "/" + firstConceptMapId + "/_history/1";
    content = this.restTemplate.getForObject(versionEndpoint, String.class);
    ConceptMap versionedConceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert
    assertNotNull(versionedConceptMap);
    assertEquals(ResourceType.ConceptMap, versionedConceptMap.getResourceType());
    assertEquals(firstConceptMapId, versionedConceptMap.getIdPart());

    // Verify metadata
    assertNotNull(versionedConceptMap.getMeta());
    assertNotNull(versionedConceptMap.getMeta().getVersionId());
    assertNotNull(versionedConceptMap.getMeta().getLastUpdated());

    // Compare with original ConceptMap
    ConceptMap originalConceptMap = (ConceptMap) conceptMaps.get(0);
    assertEquals(originalConceptMap.getUrl(), versionedConceptMap.getUrl());
    assertEquals(originalConceptMap.getName(), versionedConceptMap.getName());
    assertEquals(originalConceptMap.getPublisher(), versionedConceptMap.getPublisher());
  }

  @Test
  public void testConceptMapVreadNotFound() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCMPath;
    String invalidId = "nonexistent-conceptMap-id";
    String versionEndpoint = endpoint + "/" + invalidId + "/_history/1";

    // Act & Assert
    String messageNotFound = "Concept map version not found: nonexistent-conceptMap-id version 1";
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
  public void testConceptMapVreadInvalidVersion() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCMPath;

    // Act - First get list of ConceptMaps to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstConceptMapId = conceptMaps.get(0).getIdPart();

    // Act & Assert - Try to get a version that doesn't exist
    String invalidVersionEndpoint = endpoint + "/" + firstConceptMapId + "/_history/999";
    String messageNotFound = "Concept map version not found: " + firstConceptMapId + " version 999";
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
  public void testConceptMapHistoryMetadataConsistency() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCMPath;

    // Act - Get a ConceptMap and its history
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstConceptMapId = conceptMaps.get(0).getIdPart();

    // Get history
    String historyEndpoint = endpoint + "/" + firstConceptMapId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get current version
    content = this.restTemplate.getForObject(endpoint + "/" + firstConceptMapId, String.class);
    ConceptMap currentConceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert - Verify history contains current version
    boolean foundCurrentVersion = false;
    for (Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      ConceptMap historyVersion = (ConceptMap) entry.getResource();
      if (currentConceptMap.getUrl().equals(historyVersion.getUrl())
          && currentConceptMap.getName().equals(historyVersion.getName())) {
        foundCurrentVersion = true;
        break;
      }
    }
    assertTrue(foundCurrentVersion, "History should contain the current version");
  }

  @Test
  public void testConceptMapVreadMatchesHistoryEntry() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCMPath;

    // Act - Get history first
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstConceptMapId = conceptMaps.get(0).getIdPart();

    String historyEndpoint = endpoint + "/" + firstConceptMapId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get first version from history
    ConceptMap firstHistoryVersion = (ConceptMap) historyBundle.getEntry().get(0).getResource();
    String versionId = firstHistoryVersion.getMeta().getVersionId();

    // Act - Get the same version using vread
    String vreadEndpoint = endpoint + "/" + firstConceptMapId + "/_history/" + versionId;
    content = this.restTemplate.getForObject(vreadEndpoint, String.class);
    ConceptMap vreadConceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert - Both should be identical
    // assertEquals(firstHistoryVersion.getId(), vreadConceptMap.getId());
    assertEquals(firstHistoryVersion.getUrl(), vreadConceptMap.getUrl());
    assertEquals(firstHistoryVersion.getName(), vreadConceptMap.getName());
    assertEquals(firstHistoryVersion.getVersion(), vreadConceptMap.getVersion());
    assertEquals(
        firstHistoryVersion.getMeta().getVersionId(), vreadConceptMap.getMeta().getVersionId());
  }
}
