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
import org.hl7.fhir.r4.model.CodeSystem;
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
public class FhirR4CodeSystemReadSearchTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4CodeSystemReadSearchTests.class);

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
    // The object mapper
    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
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
    String endpoint = localHost + port + fhirCSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // reassign content with the firstCodeSystemId
    content = this.restTemplate.getForObject(endpoint + "/" + firstCodeSystemId, String.class);
    CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

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
    String endpoint = localHost + port + fhirCSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Verify things about this one
    // {"resourceType":"CodeSystem","id":"ncit_21.06e","url":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","version":"21.06e","name":"NCI
    // Thesaurus
    // 21.06e","title":"ncit","status":"active","experimental":false,"publisher":"NCI","hierarchyMeaning":"is-a"}
    final Set<String> ids = new HashSet<>(Set.of("ncit_21.06e"));
    final Set<String> urls =
        new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"));

    // Assert
    assertFalse(codeSystems.isEmpty());
    for (Resource cs : codeSystems) {
      log.info("  code system = " + parser.encodeResourceToString(cs));
      CodeSystem css = (CodeSystem) cs;
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
    String endpoint = localHost + port + fhirCSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "21.06e")
            .queryParam("title", "ncit");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, true); // Expecting results

    // Test 2: Invalid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2025-01") // Future
            // date
            .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "21.06e")
            .queryParam("title", "ncit");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false); // Expecting no results

    // Test 3: Invalid system
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2021-06")
            .queryParam("system", "http://invalid.system.url")
            .queryParam("version", "21.06e")
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
            .queryParam("version", "21.06e")
            .queryParam("title", "invalid_title");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 6: url instead of system
    builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "21.06e")
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
            .queryParam("version", "21.06e")
            .queryParam("title", "ncit");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    String messageNotFound = "Use one of 'url' or 'system' parameters.";
    String errorCode = "invariant";

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
    String endpoint = localHost + port + fhirCSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202311")
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
            .queryParam("version", "202311")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCanmedCodeSystemResults(data, false); // Expecting no results

    // Test 3: Valid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "2023-11-01")
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202311")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCanmedCodeSystemResults(data, true);

    // Test 4: Valid date range
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2023-11-01")
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202311")
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
            .queryParam("version", "202311")
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
  private void validateCodeSystemResults(Bundle data, boolean expectResults) {
    List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(codeSystems.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ncit_21.06e"));
      final Set<String> urls =
          new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"));

      for (Resource cs : codeSystems) {
        log.info(" code system = " + parser.encodeResourceToString(cs));
        CodeSystem css = (CodeSystem) cs;
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

  private void validateCanmedCodeSystemResults(Bundle data, boolean expectResults) {
    List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(codeSystems.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("canmed_202311"));
      final Set<String> urls = new HashSet<>(Set.of("http://seer.nci.nih.gov/CanMED.owl"));

      for (Resource cs : codeSystems) {
        log.info(" code system = " + parser.encodeResourceToString(cs));
        CodeSystem css = (CodeSystem) cs;
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
    // TODO: implement for date when the CodeSystem data has dates
    // implement for url when the custom parameter is registered to the HAPI
    // FHIR server

    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;

    // Test 1: Get list of CodeSystems first
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();
    CodeSystem firstCodeSystem = (CodeSystem) codeSystems.get(0);

    // Test 2: Basic read by ID
    content = this.restTemplate.getForObject(endpoint + "/" + firstCodeSystemId, String.class);
    CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

    // Basic assertions
    assertNotNull(codeSystem);
    assertEquals(ResourceType.CodeSystem, codeSystem.getResourceType());
    assertEquals(firstCodeSystemId, codeSystem.getIdPart());
    assertEquals(firstCodeSystem.getUrl(), codeSystem.getUrl());
    assertEquals(firstCodeSystem.getName(), codeSystem.getName());
    assertEquals(firstCodeSystem.getPublisher(), codeSystem.getPublisher());

    // Test 3: Search using version only
    String version = firstCodeSystem.getVersion();
    String versionSearchEndpoint =
        endpoint
            + "?_id="
            + firstCodeSystemId
            + "&version="
            + URLEncoder.encode(version, StandardCharsets.UTF_8);

    content = this.restTemplate.getForObject(versionSearchEndpoint, String.class);
    Bundle versionSearchResult = parser.parseResource(Bundle.class, content);

    assertNotNull(versionSearchResult);
    assertFalse(versionSearchResult.getEntry().isEmpty());
    CodeSystem versionCodeSystem =
        (CodeSystem) versionSearchResult.getEntryFirstRep().getResource();
    assertEquals(firstCodeSystemId, versionCodeSystem.getIdPart());
    assertEquals(version, versionCodeSystem.getVersion());

    // Test 4: Search with non-existent version
    String nonExistentVersionEndpoint =
        endpoint + "?_id=" + firstCodeSystemId + "&version=nonexistent";
    content = this.restTemplate.getForObject(nonExistentVersionEndpoint, String.class);
    Bundle emptyVersionResult = parser.parseResource(Bundle.class, content);
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
    String endpoint = localHost + port + fhirCSPath;
    String codeSystemId = "umlssemnet_2023aa";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId, String.class);
    CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

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
    String endpoint = localHost + port + fhirCSPath;
    String invalidId = "invalid_id";
    String messageNotFound = "Code system not found = " + invalidId;
    String errorCode = "not-found";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + invalidId, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    String endpoint = localHost + port + fhirCSPath;

    // Test 1: Get default results without pagination
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle defaultData = parser.parseResource(Bundle.class, content);
    List<Resource> defaultCodeSystems =
        defaultData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 2: Get first page (count=2)
    UriComponentsBuilder firstPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2");

    content =
        this.restTemplate.getForObject(firstPageBuilder.build().encode().toUri(), String.class);
    Bundle firstPageData = parser.parseResource(Bundle.class, content);
    List<Resource> firstPageSystems =
        firstPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 3: Get second page (count=2, offset=2)
    UriComponentsBuilder secondPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("_count", "2")
            .queryParam("_offset", "2");

    content =
        this.restTemplate.getForObject(secondPageBuilder.build().encode().toUri(), String.class);
    Bundle secondPageData = parser.parseResource(Bundle.class, content);
    List<Resource> secondPageSystems =
        secondPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 4: Try to exceed maximum count (should return all)
    UriComponentsBuilder maxExceededBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "10000");

    content =
        this.restTemplate.getForObject(maxExceededBuilder.build().encode().toUri(), String.class);
    Bundle maxPageData = parser.parseResource(Bundle.class, content);
    List<Resource> maxPageSystems =
        maxPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Assertions for pagination
    assertEquals(2, firstPageSystems.size());
    assertEquals(2, secondPageSystems.size());
    assertEquals(4, firstPageSystems.size() + secondPageSystems.size());
    assertTrue(defaultCodeSystems.size() <= maxPageSystems.size());

    // Verify that concatenated pages equal first 4 of full results
    List<String> fourIds =
        defaultCodeSystems.subList(0, 4).stream()
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    List<String> paginatedIds =
        Stream.concat(firstPageSystems.stream(), secondPageSystems.stream())
            .map(resource -> resource.getIdPart())
            .sorted()
            .toList();

    assertEquals(fourIds, paginatedIds);

    // Verify content of individual resources
    for (Resource cs : defaultCodeSystems) {
      validateCodeSystemPagination((CodeSystem) cs);
    }
  }

  /**
   * Validate code system pagination.
   *
   * @param css the css
   */
  private void validateCodeSystemPagination(CodeSystem css) {
    assertNotNull(css);
    assertEquals(ResourceType.CodeSystem, css.getResourceType());
    assertNotNull(css.getIdPart());
    assertNotNull(css.getPublisher());
    assertNotNull(css.getUrl());

    // Log for debugging
    log.info(" code system = " + parser.encodeResourceToString(css));

    // Verify specific IDs and URLs if needed
    if (css.getIdPart().equals("ncit_21.06e")) {
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
    String endpoint = localHost + port + fhirCSPath;

    // Test 1: Get list of CodeSystems first
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    CodeSystem firstCodeSystem = (CodeSystem) codeSystems.get(0);
    String firstCodeSystemTitle = firstCodeSystem.getTitle();

    // Test 3: Search by title (exact match)
    String titleExactUrl =
        endpoint
            + "?title:exact="
            + URLEncoder.encode(firstCodeSystemTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleExactUrl, String.class);
    Bundle exactMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(exactMatchBundle.getEntry());
    assertFalse(exactMatchBundle.getEntry().isEmpty());
    CodeSystem exactMatchSystem = (CodeSystem) exactMatchBundle.getEntry().get(0).getResource();
    assertEquals(firstCodeSystemTitle, exactMatchSystem.getTitle());

    // Test 4: Search by title (contains)
    String partialTitle = firstCodeSystemTitle.substring(1, firstCodeSystemTitle.length() - 1);
    String titleContainsUrl =
        endpoint + "?title:contains=" + URLEncoder.encode(partialTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleContainsUrl, String.class);
    Bundle containsMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(containsMatchBundle.getEntry());
    assertFalse(containsMatchBundle.getEntry().isEmpty());
    boolean foundMatch =
        containsMatchBundle.getEntry().stream()
            .map(entry -> ((CodeSystem) entry.getResource()).getTitle())
            .anyMatch(title -> title.contains(partialTitle));
    assertTrue(foundMatch);

    // Test 5: Search by title (startsWith)
    String titlePrefix = firstCodeSystemTitle.substring(0, 3);
    String titleStartsWithUrl =
        endpoint + "?title:startsWith=" + URLEncoder.encode(titlePrefix, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(titleStartsWithUrl, String.class);
    Bundle startsWithMatchBundle = parser.parseResource(Bundle.class, content);

    assertNotNull(startsWithMatchBundle.getEntry());
    assertFalse(startsWithMatchBundle.getEntry().isEmpty());
    boolean foundStartsWithMatch =
        startsWithMatchBundle.getEntry().stream()
            .map(entry -> ((CodeSystem) entry.getResource()).getTitle())
            .anyMatch(title -> title.startsWith(titlePrefix));
    assertTrue(foundStartsWithMatch);

    // Test 6: Negative test - non-existent title
    String nonExistentTitle = "NonExistentCodeSystemTitle" + UUID.randomUUID();
    String negativeTestUrl =
        endpoint + "?title:exact=" + URLEncoder.encode(nonExistentTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(negativeTestUrl, String.class);
    Bundle emptyBundle = parser.parseResource(Bundle.class, content);

    assertTrue(emptyBundle.getEntry() == null || emptyBundle.getEntry().isEmpty());

    // Test 7: Case sensitivity test; exact ignores case
    String upperCaseTitle = firstCodeSystemTitle.toUpperCase();
    String caseSensitiveUrl =
        endpoint + "?title:exact=" + URLEncoder.encode(upperCaseTitle, StandardCharsets.UTF_8);
    content = this.restTemplate.getForObject(caseSensitiveUrl, String.class);
    Bundle caseSensitiveBundle = parser.parseResource(Bundle.class, content);

    // Case-sensitive search returns otherwise exact match
    assertNotNull(caseSensitiveBundle.getEntry());
    assertFalse(caseSensitiveBundle.getEntry().isEmpty());
    CodeSystem caseSensitiveMatchSystem =
        (CodeSystem) caseSensitiveBundle.getEntry().get(0).getResource();
    assertEquals(firstCodeSystemTitle, caseSensitiveMatchSystem.getTitle());
  }

  @Test
  public void testCodeSystemHistory() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;

    // Act - First get list of CodeSystems to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // Act - Get history for the first CodeSystem
    String historyEndpoint = endpoint + "/" + firstCodeSystemId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(historyBundle);
    assertEquals(ResourceType.Bundle, historyBundle.getResourceType());
    assertEquals(Bundle.BundleType.HISTORY, historyBundle.getType());
    assertFalse(historyBundle.getEntry().isEmpty());

    // Verify each entry in history is a CodeSystem with the same ID
    for (Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      assertNotNull(entry.getResource());
      assertEquals(ResourceType.CodeSystem, entry.getResource().getResourceType());

      CodeSystem historyCodeSystem = (CodeSystem) entry.getResource();
      assertEquals(firstCodeSystemId, historyCodeSystem.getIdPart());

      // Verify metadata is properly set
      assertNotNull(historyCodeSystem.getMeta());
      assertNotNull(historyCodeSystem.getMeta().getVersionId());
      assertNotNull(historyCodeSystem.getMeta().getLastUpdated());
    }
  }

  @Test
  public void testCodeSystemHistoryNotFound() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCSPath;
    String invalidId = "nonexistent-codesystem-id";
    String historyEndpoint = endpoint + "/" + invalidId + "/_history";

    String messageNotFound = "Code system not found = " + invalidId;
    String errorCode = "not-found";

    // Act
    String content = this.restTemplate.getForObject(historyEndpoint, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  @Test
  public void testCodeSystemVread() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;

    // Act - First get list of CodeSystems to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // Act - Get specific version (assuming version 1 exists)
    String versionEndpoint = endpoint + "/" + firstCodeSystemId + "/_history/1";
    content = this.restTemplate.getForObject(versionEndpoint, String.class);
    CodeSystem versionedCodeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert
    assertNotNull(versionedCodeSystem);
    assertEquals(ResourceType.CodeSystem, versionedCodeSystem.getResourceType());
    assertEquals(firstCodeSystemId, versionedCodeSystem.getIdPart());

    // Verify metadata
    assertNotNull(versionedCodeSystem.getMeta());
    assertNotNull(versionedCodeSystem.getMeta().getVersionId());
    assertNotNull(versionedCodeSystem.getMeta().getLastUpdated());

    // Compare with original CodeSystem
    CodeSystem originalCodeSystem = (CodeSystem) codeSystems.get(0);
    assertEquals(originalCodeSystem.getUrl(), versionedCodeSystem.getUrl());
    assertEquals(originalCodeSystem.getName(), versionedCodeSystem.getName());
    assertEquals(originalCodeSystem.getPublisher(), versionedCodeSystem.getPublisher());
  }

  @Test
  public void testCodeSystemVreadNotFound() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCSPath;
    String invalidId = "nonexistent-codesystem-id";
    String versionEndpoint = endpoint + "/" + invalidId + "/_history/1";

    // Act & Assert
    String messageNotFound = "Code system version not found: nonexistent-codesystem-id version 1";
    String errorCode = "not-found";

    // Act
    String content = this.restTemplate.getForObject(versionEndpoint, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  @Test
  public void testCodeSystemVreadInvalidVersion() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;

    // Act - First get list of CodeSystems to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // Act & Assert - Try to get a version that doesn't exist
    String invalidVersionEndpoint = endpoint + "/" + firstCodeSystemId + "/_history/999";
    String messageNotFound = "Code system version not found: " + firstCodeSystemId + " version 999";
    String errorCode = "not-found";

    // Act
    content = this.restTemplate.getForObject(invalidVersionEndpoint, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  @Test
  public void testCodeSystemHistoryMetadataConsistency() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;

    // Act - Get a CodeSystem and its history
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // Get history
    String historyEndpoint = endpoint + "/" + firstCodeSystemId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get current version
    content = this.restTemplate.getForObject(endpoint + "/" + firstCodeSystemId, String.class);
    CodeSystem currentCodeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert - Verify history contains current version
    boolean foundCurrentVersion = false;
    for (Bundle.BundleEntryComponent entry : historyBundle.getEntry()) {
      CodeSystem historyVersion = (CodeSystem) entry.getResource();
      if (currentCodeSystem.getUrl().equals(historyVersion.getUrl())
          && currentCodeSystem.getName().equals(historyVersion.getName())) {
        foundCurrentVersion = true;
        break;
      }
    }
    Assertions.assertTrue(foundCurrentVersion, "History should contain the current version");
  }

  @Test
  public void testCodeSystemVreadMatchesHistoryEntry() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;

    // Act - Get history first
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();

    String historyEndpoint = endpoint + "/" + firstCodeSystemId + "/_history";
    content = this.restTemplate.getForObject(historyEndpoint, String.class);
    Bundle historyBundle = parser.parseResource(Bundle.class, content);

    // Get first version from history
    CodeSystem firstHistoryVersion = (CodeSystem) historyBundle.getEntry().get(0).getResource();
    String versionId = firstHistoryVersion.getMeta().getVersionId();

    // Act - Get the same version using vread
    String vreadEndpoint = endpoint + "/" + firstCodeSystemId + "/_history/" + versionId;
    content = this.restTemplate.getForObject(vreadEndpoint, String.class);
    CodeSystem vreadCodeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert - Both should be identical
    // assertEquals(firstHistoryVersion.getId(), vreadCodeSystem.getId());
    assertEquals(firstHistoryVersion.getUrl(), vreadCodeSystem.getUrl());
    assertEquals(firstHistoryVersion.getName(), vreadCodeSystem.getName());
    assertEquals(firstHistoryVersion.getVersion(), vreadCodeSystem.getVersion());
    assertEquals(
        firstHistoryVersion.getMeta().getVersionId(), vreadCodeSystem.getMeta().getVersionId());
  }
}
