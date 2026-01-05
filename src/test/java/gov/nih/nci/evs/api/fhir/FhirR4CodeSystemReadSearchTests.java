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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeSystem;
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
public class FhirR4CodeSystemReadSearchTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4CodeSystemReadSearchTests.class);

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** The fhir CM path. */
  private final String fhirCSPath = "/fhir/r4/CodeSystem";

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
   * Test code system read.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemRead() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();
    final String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // reassign content with the firstCodeSystemId
    content = this.restTemplate.getForObject(endpoint + "/" + firstCodeSystemId, String.class);
    final CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert
    assertNotNull(codeSystem);
    assertEquals(ResourceType.CodeSystem, codeSystem.getResourceType());
    assertEquals(firstCodeSystemId, codeSystem.getIdPart());
    assertEquals(((CodeSystem) codeSystems.get(0)).getUrl(), codeSystem.getUrl());
    assertEquals(((CodeSystem) codeSystems.get(0)).getName(), codeSystem.getName());
    assertEquals(((CodeSystem) codeSystems.get(0)).getPublisher(), codeSystem.getPublisher());
  }

  /**
   * Test code system search.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSearch() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Verify things about this one
    // {"resourceType":"CodeSystem","id":"ncit_25.06e","url":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","version":"25.06e","name":"NCI
    // Thesaurus
    // 25.06e","title":"ncit","status":"active","experimental":false,"publisher":"NCI","hierarchyMeaning":"is-a"}
    final Set<String> ids = new HashSet<>(Set.of("ncit_25.06e"));
    final Set<String> urls =
        new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"));

    // Assert
    assertFalse(codeSystems.isEmpty());
    for (final Resource cs : codeSystems) {
      log.info("  code system = " + parser.encodeResourceToString(cs));
      final CodeSystem css = (CodeSystem) cs;
      assertNotNull(css);
      assertEquals(ResourceType.CodeSystem, css.getResourceType());
      assertNotNull(css.getIdPart());
      assertNotNull(css.getPublisher());
      assertNotNull(css.getUrl());
      ids.remove(css.getIdPart());
      urls.remove(css.getUrl());
    }
    assertThat(ids).isEmpty();
    assertThat(urls).isEmpty();
  }

  /**
   * Test code system search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSearchWithParameters() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, true); // Expecting results

    // Test 2: Invalid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2035-01") // Future
            // date
            .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false); // Expecting no results

    // Test 3: Invalid system
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2021-06")
            .queryParam("system", "http://invalid.system.url")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 4: Invalid version
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2021-06")
            .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "invalid_version")
            .queryParam("title", "ncit");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 5: Invalid title
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2021-06")
            .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "invalid_title");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 6: url instead of system
    builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit");

    // Test successful case with all parameters
    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, true); // Expecting results

    // Test 7: url and system
    builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    final String messageNotFound = "Use one of 'url' or 'system' parameters.";
    final String errorCode = "invariant";

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test code system search with parameters - date related tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSearchWithDateFocus() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateCanmedCodeSystemResults(data, true); // Expecting results

    // Test 2: Invalid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2030-01") // Future date
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCanmedCodeSystemResults(data, false); // Expecting no results

    // Test 3: Valid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "2025-06-01")
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCanmedCodeSystemResults(data, true);

    // Test 4: Valid date range
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2023-11-01")
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCanmedCodeSystemResults(data, true);

    // Test 5: Valid date range
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2023-11-01")
            .queryParam("date", "lt2030-11-01")
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCanmedCodeSystemResults(data, true);
  }

  /**
   * Validate code system results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  private void validateCodeSystemResults(final Bundle data, final boolean expectResults) {
    final List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(codeSystems.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ncit_25.06e"));
      final Set<String> urls =
          new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"));

      for (final Resource cs : codeSystems) {
        log.info(" code system = " + parser.encodeResourceToString(cs));
        final CodeSystem css = (CodeSystem) cs;
        assertNotNull(css);
        assertEquals(ResourceType.CodeSystem, css.getResourceType());
        assertNotNull(css.getIdPart());
        assertNotNull(css.getPublisher());
        assertNotNull(css.getUrl());
        ids.remove(css.getIdPart());
        urls.remove(css.getUrl());
      }
      assertThat(ids).isEmpty();
      assertThat(urls).isEmpty();
    } else {
      assertTrue(data.getEntry().isEmpty() || codeSystems.isEmpty());
    }
  }

  /**
   * Validate canmed code system results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  private void validateCanmedCodeSystemResults(final Bundle data, final boolean expectResults) {
    final List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(codeSystems.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("canmed_202506"));
      final Set<String> urls = new HashSet<>(Set.of("http://seer.nci.nih.gov/CanMED.owl"));

      for (final Resource cs : codeSystems) {
        log.info(" code system = " + parser.encodeResourceToString(cs));
        final CodeSystem css = (CodeSystem) cs;
        assertNotNull(css);
        assertEquals(ResourceType.CodeSystem, css.getResourceType());
        assertNotNull(css.getIdPart());
        assertNotNull(css.getPublisher());
        assertNotNull(css.getUrl());
        ids.remove(css.getIdPart());
        urls.remove(css.getUrl());
      }
      assertThat(ids).isEmpty();
      assertThat(urls).isEmpty();
    } else {
      assertTrue(data.getEntry().isEmpty() || codeSystems.isEmpty());
    }
  }

  /**
   * Test code system read with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemReadWithParameters() throws Exception {
    // NOTE: implement for date when the CodeSystem data has dates
    // implement for url when the custom parameter is registered to the HAPI
    // FHIR server

    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;

    // Test 1: Get list of CodeSystems first
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();
    final String firstCodeSystemId = codeSystems.get(0).getIdPart();
    final CodeSystem firstCodeSystem = (CodeSystem) codeSystems.get(0);

    // Test 2: Basic read by ID
    content = this.restTemplate.getForObject(endpoint + "/" + firstCodeSystemId, String.class);
    final CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

    // Basic assertions
    assertNotNull(codeSystem);
    assertEquals(ResourceType.CodeSystem, codeSystem.getResourceType());
    assertEquals(firstCodeSystemId, codeSystem.getIdPart());
    assertEquals(firstCodeSystem.getUrl(), codeSystem.getUrl());
    assertEquals(firstCodeSystem.getName(), codeSystem.getName());
    assertEquals(firstCodeSystem.getPublisher(), codeSystem.getPublisher());

    // Test 3: Search using version only
    final String version = firstCodeSystem.getVersion();
    final String versionSearchEndpoint =
        endpoint
            + "?_id="
            + firstCodeSystemId
            + "&version="
            + URLEncoder.encode(version, StandardCharsets.UTF_8);

    content = this.restTemplate.getForObject(versionSearchEndpoint, String.class);
    final Bundle versionSearchResult = parser.parseResource(Bundle.class, content);

    assertNotNull(versionSearchResult);
    assertFalse(versionSearchResult.getEntry().isEmpty());
    final CodeSystem versionCodeSystem =
        (CodeSystem) versionSearchResult.getEntryFirstRep().getResource();
    assertEquals(firstCodeSystemId, versionCodeSystem.getIdPart());
    assertEquals(version, versionCodeSystem.getVersion());

    // Test 4: Search with non-existent version
    final String nonExistentVersionEndpoint =
        endpoint + "?_id=" + firstCodeSystemId + "&version=nonexistent";
    content = this.restTemplate.getForObject(nonExistentVersionEndpoint, String.class);
    final Bundle emptyVersionResult = parser.parseResource(Bundle.class, content);
    assertNotNull(emptyVersionResult);
    assertTrue(emptyVersionResult.getEntry().isEmpty());
  }

  /**
   * Test code system read static id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemReadStaticId() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;
    final String codeSystemId = "umlssemnet_2023aa";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId, String.class);
    final CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert
    assertNotNull(codeSystem);
    assertEquals(ResourceType.CodeSystem, codeSystem.getResourceType());
    assertEquals(codeSystemId, codeSystem.getIdPart());
    assertEquals("http://www.nlm.nih.gov/research/umls/umlssemnet.owl", codeSystem.getUrl());
    assertEquals("UMLS Semantic Network 2023AA", codeSystem.getName());
    assertEquals("National Library of Medicine", codeSystem.getPublisher());
    assertEquals("umlssemnet", codeSystem.getTitle());
    assertEquals("active", codeSystem.getStatus().toCode());
    assertEquals("is-a", codeSystem.getHierarchyMeaning().toCode());
    assertEquals("CodeSystem", codeSystem.getResourceType().toString());
    assertEquals(false, codeSystem.getExperimental());
    assertEquals("2023AA", codeSystem.getVersion());
  }

  /**
   * Test code system read bad id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemReadBadId() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;
    final String invalidId = "invalid_id";
    final String messageNotFound = "Code system not found = " + invalidId;
    final String errorCode = "not-found";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + invalidId, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test code system search pagination.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSearchPagination() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;

    // Test 1: Get default results without pagination
    String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle defaultData = parser.parseResource(Bundle.class, content);
    final List<Resource> defaultCodeSystems =
        defaultData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 2: Get first page (count=2)
    final UriComponentsBuilder firstPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2");

    content =
        this.restTemplate.getForObject(firstPageBuilder.build().encode().toUri(), String.class);
    final Bundle firstPageData = parser.parseResource(Bundle.class, content);
    final List<Resource> firstPageSystems =
        firstPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 3: Get second page (count=2, offset=2)
    final UriComponentsBuilder secondPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2")
            .queryParam("_offset", "2");

    content =
        this.restTemplate.getForObject(secondPageBuilder.build().encode().toUri(), String.class);
    final Bundle secondPageData = parser.parseResource(Bundle.class, content);
    final List<Resource> secondPageSystems =
        secondPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 4: Try to exceed maximum count (should return all)
    final UriComponentsBuilder maxExceededBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "10000");

    content =
        this.restTemplate.getForObject(maxExceededBuilder.build().encode().toUri(), String.class);
    final Bundle maxPageData = parser.parseResource(Bundle.class, content);
    final List<Resource> maxPageSystems =
        maxPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Assertions for pagination
    assertEquals(2, firstPageSystems.size());
    assertEquals(2, secondPageSystems.size());
    assertEquals(4, firstPageSystems.size() + secondPageSystems.size());
    assertTrue(defaultCodeSystems.size() <= maxPageSystems.size());

    // Verify that concatenated pages equal first 4 of full results
    final List<String> fourIds =
        defaultCodeSystems.subList(0, 4).stream()
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    final List<String> paginatedIds =
        Stream.concat(firstPageSystems.stream(), secondPageSystems.stream())
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    assertEquals(fourIds, paginatedIds);

    // Verify content of individual resources
    for (final Resource cs : defaultCodeSystems) {
      validateCodeSystemPagination((CodeSystem) cs);
    }
  }

  /**
   * Validate code system pagination.
   *
   * @param css the css
   */
  private void validateCodeSystemPagination(final CodeSystem css) {
    assertNotNull(css);
    assertEquals(ResourceType.CodeSystem, css.getResourceType());
    assertNotNull(css.getIdPart());
    assertNotNull(css.getPublisher());
    assertNotNull(css.getUrl());

    // Log for debugging
    log.info(" code system = " + parser.encodeResourceToString(css));

    // Verify specific IDs and URLs if needed
    if (css.getIdPart().equals("ncit_25.06e")) {
      assertEquals("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl", css.getUrl());
    }
  }

  /**
   * Test code system search variants with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSearchVariantsWithParameters() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;

    // Test 1: Get list of CodeSystems first
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    final CodeSystem firstCodeSystem = (CodeSystem) codeSystems.get(0);
    final String firstCodeSystemTitle = firstCodeSystem.getTitle();

    // Test 3: Search by title (exact match)
    final String titleExactUrl =
        endpoint
            + "?title:exact="
            + URLEncoder.encode(firstCodeSystemTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleExactUrl, String.class);
    final Bundle exactMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(exactMatchBundle.getEntry());
    assertFalse(exactMatchBundle.getEntry().isEmpty());
    final CodeSystem exactMatchSystem =
        (CodeSystem) exactMatchBundle.getEntry().get(0).getResource();
    assertEquals(firstCodeSystemTitle, exactMatchSystem.getTitle());

    // Test 4: Search by title (contains)
    final String partialTitle =
        firstCodeSystemTitle.substring(1, firstCodeSystemTitle.length() - 1);
    final String titleContainsUrl =
        endpoint + "?title:contains=" + URLEncoder.encode(partialTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleContainsUrl, String.class);
    final Bundle containsMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(containsMatchBundle.getEntry());
    assertFalse(containsMatchBundle.getEntry().isEmpty());
    final boolean foundMatch =
        containsMatchBundle.getEntry().stream()
            .map(entry -> ((CodeSystem) entry.getResource()).getTitle())
            .anyMatch(title -> title.contains(partialTitle));
    assertTrue(foundMatch);

    // Test 5: Search by title (startsWith)
    final String titlePrefix = firstCodeSystemTitle.substring(0, 3);
    final String titleStartsWithUrl =
        endpoint + "?title:startsWith=" + URLEncoder.encode(titlePrefix, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleStartsWithUrl, String.class);
    final Bundle startsWithMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(startsWithMatchBundle.getEntry());
    assertFalse(startsWithMatchBundle.getEntry().isEmpty());
    final boolean foundStartsWithMatch =
        startsWithMatchBundle.getEntry().stream()
            .map(entry -> ((CodeSystem) entry.getResource()).getTitle())
            .anyMatch(title -> title.startsWith(titlePrefix));
    assertTrue(foundStartsWithMatch);

    // Test 6: Negative test - non-existent title
    final String nonExistentTitle = "NonExistentCodeSystemTitle" + UUID.randomUUID();
    final String negativeTestUrl =
        endpoint + "?title:exact=" + URLEncoder.encode(nonExistentTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(negativeTestUrl, String.class);
    final Bundle emptyBundle = parser.parseResource(Bundle.class, content);

    assertTrue(emptyBundle.getEntry() == null || emptyBundle.getEntry().isEmpty());

    // Test 7: Case sensitivity test; exact ignores case
    final String upperCaseTitle = firstCodeSystemTitle.toUpperCase();
    final String caseSensitiveUrl =
        endpoint + "?title:exact=" + URLEncoder.encode(upperCaseTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(caseSensitiveUrl, String.class);
    final Bundle caseSensitiveBundle = parser.parseResource(Bundle.class, content);

    // Case-sensitive search returns otherwise exact match
    assertNotNull(caseSensitiveBundle.getEntry());
    assertFalse(caseSensitiveBundle.getEntry().isEmpty());
    final CodeSystem caseSensitiveMatchSystem =
        (CodeSystem) caseSensitiveBundle.getEntry().get(0).getResource();
    assertEquals(firstCodeSystemTitle, caseSensitiveMatchSystem.getTitle());
  }

  /**
   * Test code system history.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemHistory() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;

    // Act - First get list of CodeSystems to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // Act - Get history for the first CodeSystem
    final String historyEndpoint = endpoint + "/" + firstCodeSystemId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(historyBundle);
    assertEquals(ResourceType.Bundle, historyBundle.getResourceType());
    assertEquals(Bundle.BundleType.HISTORY, historyBundle.getType());
    assertFalse(historyBundle.getEntry().isEmpty());

    // Verify each entry in history is a CodeSystem with the same ID
    for (final Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      assertNotNull(entry.getResource());
      assertEquals(ResourceType.CodeSystem, entry.getResource().getResourceType());

      final CodeSystem historyCodeSystem = (CodeSystem) entry.getResource();
      assertEquals(firstCodeSystemId, historyCodeSystem.getIdPart());

      // Verify metadata is properly set
      assertNotNull(historyCodeSystem.getMeta());
      assertNotNull(historyCodeSystem.getMeta().getVersionId());
      assertNotNull(historyCodeSystem.getMeta().getLastUpdated());
    }
  }

  /**
   * Test code system history not found.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemHistoryNotFound() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;
    final String invalidId = "nonexistent-codesystem-id";
    final String historyEndpoint = endpoint + "/" + invalidId + "/_history";

    final String messageNotFound = "Code system not found = " + invalidId;
    final String errorCode = "not-found";

    // Act
    final String content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test code system vread.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemVread() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;

    // Act - First get list of CodeSystems to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // Act - Get specific version (assuming version 1 exists)
    final String versionEndpoint = endpoint + "/" + firstCodeSystemId + "/_history/1";
    content = this.restTemplate.getForObject(versionEndpoint, String.class);
    final CodeSystem versionedCodeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert
    assertNotNull(versionedCodeSystem);
    assertEquals(ResourceType.CodeSystem, versionedCodeSystem.getResourceType());
    assertEquals(firstCodeSystemId, versionedCodeSystem.getIdPart());

    // Verify metadata
    assertNotNull(versionedCodeSystem.getMeta());
    assertNotNull(versionedCodeSystem.getMeta().getVersionId());
    assertNotNull(versionedCodeSystem.getMeta().getLastUpdated());

    // Compare with original CodeSystem
    final CodeSystem originalCodeSystem = (CodeSystem) codeSystems.get(0);
    assertEquals(originalCodeSystem.getUrl(), versionedCodeSystem.getUrl());
    assertEquals(originalCodeSystem.getName(), versionedCodeSystem.getName());
    assertEquals(originalCodeSystem.getPublisher(), versionedCodeSystem.getPublisher());
  }

  /**
   * Test code system vread not found.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemVreadNotFound() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;
    final String invalidId = "nonexistent-codesystem-id";
    final String versionEndpoint = endpoint + "/" + invalidId + "/_history/1";

    // Act & Assert
    final String messageNotFound =
        "Code system version not found: nonexistent-codesystem-id version 1";
    final String errorCode = "not-found";

    // Act
    final String content = this.restTemplate.getForObject(versionEndpoint, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test code system vread invalid version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemVreadInvalidVersion() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;

    // Act - First get list of CodeSystems to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // Act & Assert - Try to get a version that doesn't exist
    final String invalidVersionEndpoint = endpoint + "/" + firstCodeSystemId + "/_history/999";
    final String messageNotFound =
        "Code system version not found: " + firstCodeSystemId + " version 999";
    final String errorCode = "not-found";

    // Act
    content = this.restTemplate.getForObject(invalidVersionEndpoint, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test code system history metadata consistency.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemHistoryMetadataConsistency() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;

    // Act - Get a CodeSystem and its history
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // Get history
    final String historyEndpoint = endpoint + "/" + firstCodeSystemId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get current version
    content = this.restTemplate.getForObject(endpoint + "/" + firstCodeSystemId, String.class);
    final CodeSystem currentCodeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert - Verify history contains current version
    boolean foundCurrentVersion = false;
    for (final Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      final CodeSystem historyVersion = (CodeSystem) entry.getResource();
      if (currentCodeSystem.getUrl().equals(historyVersion.getUrl())
          && currentCodeSystem.getName().equals(historyVersion.getName())) {
        foundCurrentVersion = true;
        break;
      }
    }

    assertTrue(foundCurrentVersion, "History should contain the current version");
  }

  /**
   * Test code system vread matches history entry.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemVreadMatchesHistoryEntry() throws Exception {
    // Arrange
    String content;
    final String endpoint = localHost + port + fhirCSPath;

    // Act - Get history first
    content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    final String firstCodeSystemId = codeSystems.get(0).getIdPart();

    final String historyEndpoint = endpoint + "/" + firstCodeSystemId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    final Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get first version from history
    final CodeSystem firstHistoryVersion =
        (CodeSystem) historyBundle.getEntry().get(0).getResource();
    final String versionId = firstHistoryVersion.getMeta().getVersionId();

    // Act - Get the same version using vread
    final String vreadEndpoint = endpoint + "/" + firstCodeSystemId + "/_history/" + versionId;
    content = this.restTemplate.getForObject(vreadEndpoint, String.class);
    final CodeSystem vreadCodeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert - Both should be identical
    // assertEquals(firstHistoryVersion.getId(), vreadCodeSystem.getId());
    assertEquals(firstHistoryVersion.getUrl(), vreadCodeSystem.getUrl());
    assertEquals(firstHistoryVersion.getName(), vreadCodeSystem.getName());
    assertEquals(firstHistoryVersion.getVersion(), vreadCodeSystem.getVersion());
    assertEquals(
        firstHistoryVersion.getMeta().getVersionId(), vreadCodeSystem.getMeta().getVersionId());
  }

  /**
   * Test code system search with sort by name.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchSortByName() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath + "?_sort=name";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by name in ascending order
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    String previousName = null;
    for (final Resource cs : codeSystems) {
      final CodeSystem css = (CodeSystem) cs;
      assertNotNull(css.getName());
      final String currentName = css.getName().toLowerCase();
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
   * Test code system search with sort by title descending.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchSortByTitleDescending() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath + "?_sort=-title";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by title in descending order
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    String previousTitle = null;
    for (final Resource cs : codeSystems) {
      final CodeSystem css = (CodeSystem) cs;
      assertNotNull(css.getTitle());
      final String currentTitle = css.getTitle().toLowerCase();
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
   * Test code system search with sort by publisher.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchSortByPublisher() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath + "?_sort=publisher";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by publisher in ascending order
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    String previousPublisher = null;
    for (final Resource cs : codeSystems) {
      final CodeSystem css = (CodeSystem) cs;
      assertNotNull(css.getPublisher());
      final String currentPublisher = css.getPublisher().toLowerCase();
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
   * Test code system search with sort by date.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchSortByDate() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath + "?_sort=date";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by date in ascending order
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    Date previousDate = null;
    for (final Resource cs : codeSystems) {
      final CodeSystem css = (CodeSystem) cs;
      final Date currentDate = css.getDate();
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
   * Test code system search with sort by URL.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchSortByUrl() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath + "?_sort=url";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by URL in ascending order
    final List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    String previousUrl = null;
    for (final Resource cs : codeSystems) {
      final CodeSystem css = (CodeSystem) cs;
      assertNotNull(css.getUrl());
      final String currentUrl = css.getUrl().toLowerCase();
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
   * Test code system search with invalid sort field.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchSortByInvalidField() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath + "?_sort=invalid_field";

    // Act
    final String content = this.restTemplate.getForObject(endpoint, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);

    // Assert
    assertNotNull(outcome);
    assertNotNull(outcome.getIssue());
    assertFalse(outcome.getIssue().isEmpty());

    final OperationOutcomeIssueComponent issue = outcome.getIssue().get(0);
    assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
    assertTrue(issue.getDiagnostics().contains("Unsupported sort field"));
  }

  /**
   * Test code system search with :missing modifier on string parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchWithMissingModifierOnStringParams() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;

    // Test 1: Find CodeSystems WITH a title (title:missing=false)
    String url = endpoint + "?title:missing=false&_count=100";
    String content = this.restTemplate.getForObject(url, String.class);
    Bundle bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should have a title
    assertNotNull(bundle);
    assertTrue(bundle.hasEntry(), "Should find CodeSystems with titles");
    for (BundleEntryComponent entry : bundle.getEntry()) {
      CodeSystem cs = (CodeSystem) entry.getResource();
      assertNotNull(cs.getTitle(), "CodeSystem should have a title when title:missing=false");
      assertFalse(cs.getTitle().isEmpty(), "CodeSystem title should not be empty");
    }

    // Test 2: Find CodeSystems WITHOUT a title (title:missing=true)
    url = endpoint + "?title:missing=true&_count=100";
    content = this.restTemplate.getForObject(url, String.class);
    bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should NOT have a title
    assertNotNull(bundle);
    // Note: May be empty if all CodeSystems have titles
    for (BundleEntryComponent entry : bundle.getEntry()) {
      CodeSystem cs = (CodeSystem) entry.getResource();
      assertTrue(
          cs.getTitle() == null || cs.getTitle().isEmpty(),
          "CodeSystem should not have a title when title:missing=true");
    }
  }

  /**
   * Test code system search with :missing modifier on URI parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchWithUrlMissingModifier() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;

    // Test 1: Find CodeSystems WITH a url (url:missing=false)
    String url = endpoint + "?url:missing=false&_count=100";
    String content = this.restTemplate.getForObject(url, String.class);
    Bundle bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should have a url
    assertNotNull(bundle);
    assertTrue(bundle.hasEntry(), "Should find CodeSystems with urls");
    for (BundleEntryComponent entry : bundle.getEntry()) {
      CodeSystem cs = (CodeSystem) entry.getResource();
      assertNotNull(cs.getUrl(), "CodeSystem should have a url when url:missing=false");
      assertFalse(cs.getUrl().isEmpty(), "CodeSystem url should not be empty");
    }

    // Test 2: Find CodeSystems WITHOUT a url (url:missing=true)
    url = endpoint + "?url:missing=true&_count=100";
    content = this.restTemplate.getForObject(url, String.class);
    bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should NOT have a url
    assertNotNull(bundle);
    // Note: May be empty if all CodeSystems have urls
    for (BundleEntryComponent entry : bundle.getEntry()) {
      CodeSystem cs = (CodeSystem) entry.getResource();
      assertTrue(
          cs.getUrl() == null || cs.getUrl().isEmpty(),
          "CodeSystem should not have a url when url:missing=true");
    }
  }

  /**
   * Test code system search with :contains modifier on URI parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchWithUrlContainsModifier() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;

    // Test: Find CodeSystems where URL contains "ncit"
    String url = endpoint + "?url:contains=ncit&_count=100";
    String content = this.restTemplate.getForObject(url, String.class);
    Bundle bundle = parser.parseResource(Bundle.class, content);

    // Assert - all returned resources should have "ncit" in their URL
    assertNotNull(bundle);
    if (bundle.hasEntry()) {
      for (BundleEntryComponent entry : bundle.getEntry()) {
        CodeSystem cs = (CodeSystem) entry.getResource();
        assertNotNull(cs.getUrl(), "CodeSystem should have a url");
        assertTrue(
            cs.getUrl().toLowerCase().contains("ncit"),
            "CodeSystem url should contain 'ncit': " + cs.getUrl());
      }
    }
  }

  /**
   * Test code system search with :below modifier on URI parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchWithUrlBelowModifier() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;

    // First, get a known CodeSystem with a URL
    String content = this.restTemplate.getForObject(endpoint + "?_count=1", String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    assertTrue(data.hasEntry(), "Should have at least one CodeSystem");
    CodeSystem firstCs = (CodeSystem) data.getEntry().get(0).getResource();
    String baseUrl = firstCs.getUrl();

    // Test: Find CodeSystems where URL is at or below the base URL
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
          CodeSystem cs = (CodeSystem) entry.getResource();
          assertNotNull(cs.getUrl(), "CodeSystem should have a url");
          assertTrue(
              cs.getUrl().startsWith(parentUrl) || cs.getUrl().equals(parentUrl),
              "CodeSystem url should be at or below '" + parentUrl + "': " + cs.getUrl());
        }
      }
    }
  }

  /**
   * Test code system search with :above modifier on URI parameters.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchWithUrlAboveModifier() throws Exception {
    // Arrange
    final String endpoint = localHost + port + fhirCSPath;

    // First, get a known CodeSystem with a URL
    String content = this.restTemplate.getForObject(endpoint + "?_count=1", String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    assertTrue(data.hasEntry(), "Should have at least one CodeSystem");
    CodeSystem firstCs = (CodeSystem) data.getEntry().get(0).getResource();
    String childUrl = firstCs.getUrl();

    // Test: Find CodeSystems where URL is at or above the child URL
    if (childUrl != null) {
      String url = endpoint + "?url:above=" + URLEncoder.encode(childUrl, StandardCharsets.UTF_8);
      content = this.restTemplate.getForObject(url, String.class);
      Bundle bundle = parser.parseResource(Bundle.class, content);

      // Assert - all returned resources should have URL at or above child URL
      assertNotNull(bundle);
      if (bundle.hasEntry()) {
        for (BundleEntryComponent entry : bundle.getEntry()) {
          CodeSystem cs = (CodeSystem) entry.getResource();
          assertNotNull(cs.getUrl(), "CodeSystem should have a url");
          assertTrue(
              childUrl.startsWith(cs.getUrl()) || childUrl.equals(cs.getUrl()),
              "Search url '"
                  + childUrl
                  + "' should be at or below CodeSystem url '"
                  + cs.getUrl()
                  + "'");
        }
      }
    }
  }
}
