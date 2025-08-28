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
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.ResourceType;
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
class FhirR5CodeSystemReadSearchTests {
  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR5CodeSystemReadSearchTests.class);

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** Fhir url paths. */
  private final String fhirCSPath = "/fhir/r5/CodeSystem";

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
   * Test code system search.
   *
   * @throws Exception exception
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
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Verify things about this one
    // {"resourceType":"CodeSystem","id":"ncit_25.06e","url":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","version":"25.06e","name":"NCI
    // Thesaurus
    // 25.06e","title":"ncit","status":"active","experimental":false,"publisher":"NCI","hierarchyMeaning":"is-a"}
    final Set<String> ids = new HashSet<>(Set.of("ncit_25.06e"));
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
      assertNotNull(css.getName());
      assertNotNull(css.getVersion());
      assertNotNull(css.getTitle());
      ids.remove(css.getIdPart());
      urls.remove(css.getUrl());
    }
    assertThat(ids).isEmpty();
    assertThat(urls).isEmpty();
  }

  /**
   * Test code system read.
   *
   * @throws Exception exception
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
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // reassign content with firstCodeSystemId
    content = this.restTemplate.getForObject(endpoint + "/" + firstCodeSystemId, String.class);
    CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert
    assertNotNull(codeSystem);
    assertEquals(ResourceType.CodeSystem, codeSystem.getResourceType());
    assertEquals(firstCodeSystemId, codeSystem.getIdPart());
    assertEquals(((CodeSystem) codeSystems.get(0)).getUrl(), codeSystem.getUrl());
    assertEquals(((CodeSystem) codeSystems.get(0)).getVersion(), codeSystem.getVersion());
    assertEquals(((CodeSystem) codeSystems.get(0)).getTitle(), codeSystem.getTitle());
    assertEquals(((CodeSystem) codeSystems.get(0)).getName(), codeSystem.getName());
    assertEquals(((CodeSystem) codeSystems.get(0)).getPublisher(), codeSystem.getPublisher());
  }

  /**
   * Test code system search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSearchWithParameters() throws Exception {

    // TODO: currently data doesn't have the code, description or date data
    // these tests should be amended when the data is updated

    // Arrange
    String endpoint = localHost + port + fhirCSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit")
            .queryParam("publisher", "NCI")
            .queryParam("name", "NCI Thesaurus 25.06e");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, true); // Expecting results

    // Test 2: Invalid date, all other parameters valid
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2033-01") // Future
            // date
            // -
            // invalid
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit")
            .queryParam("publisher", "NCI")
            .queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 3: Invalid url, all other parameters valid
    builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://invalid.system.url")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit")
            .queryParam("publisher", "NCI")
            .queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 4: Invalid version, all other parameters valid
    builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "invalid_version")
            .queryParam("title", "ncit")
            .queryParam("publisher", "NCI")
            .queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 5: Invalid title, all other parameters valid
    builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "invalid_title")
            .queryParam("publisher", "NCI")
            .queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 6: Invalid publisher, all other parameters valid
    builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit")
            .queryParam("publisher", "InvalidPublisher")
            .queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 7: Invalid name, all other parameters valid
    builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit")
            .queryParam("publisher", "NCI")
            .queryParam("name", "InvalidName");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 8: Invalid description, all other parameters valid
    // builder = UriComponentsBuilder.fromUriString(endpoint)//
    // .queryParam("date",
    // // "ge2021-06")
    // .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
    // .queryParam("version", "25.06e").queryParam("title",
    // "ncit").queryParam("publisher", "NCI")
    // .queryParam("name", "NCI Thesaurus");
    //
    // content =
    // this.restTemplate.getForObject(builder.build().encode().toUri(),
    // String.class);
    // data = parser.parseResource(Bundle.class, content);
    // validateCodeSystemResults(data, false);
    //
    // // Test 9: Invalid code, all other parameters valid
    // builder = UriComponentsBuilder.fromUriString(endpoint)//
    // .queryParam("date",
    // // "ge2021-06")
    // .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
    // .queryParam("version", "25.06e").queryParam("title",
    // "ncit").queryParam("publisher", "NCI")
    // .queryParam("name", "NCI Thesaurus");
    //
    // content =
    // this.restTemplate.getForObject(builder.build().encode().toUri(),
    // String.class);
    // data = parser.parseResource(Bundle.class, content);
    // validateCodeSystemResults(data, false);

    // Test 6: system instead of url
    builder =
        UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
            // "ge2021-06")
            .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
            .queryParam("version", "25.06e")
            .queryParam("title", "ncit");

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
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    String messageNotFound = "Must use one of 'url' or 'system' parameters";
    String errorCode = "invariant";

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
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
      final Set<String> ids = new HashSet<>(Set.of("ncit_25.06e"));
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
    OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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

  /**
   * Test code system search with parameters - date range related.
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
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    org.hl7.fhir.r5.model.Bundle data =
        parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
    validateCanmedCodeSystemResults(data, true); // Expecting results

    // Test 2: Invalid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2030-01") // Future date
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
    validateCanmedCodeSystemResults(data, false); // Expecting no results

    // Test 3: Valid date
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "2025-06-01")
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
    validateCanmedCodeSystemResults(data, true);

    // Test 4: Valid date range
    builder =
        UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("date", "ge2023-11-01")
            .queryParam("system", "http://seer.nci.nih.gov/CanMED.owl")
            .queryParam("version", "202506")
            .queryParam("title", "canmed");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
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
    data = parser.parseResource(org.hl7.fhir.r5.model.Bundle.class, content);
    validateCanmedCodeSystemResults(data, true);
  }

  private void validateCanmedCodeSystemResults(
      org.hl7.fhir.r5.model.Bundle data, boolean expectResults) {
    List<org.hl7.fhir.r5.model.Resource> codeSystems =
        data.getEntry().stream()
            .map(org.hl7.fhir.r5.model.Bundle.BundleEntryComponent::getResource)
            .toList();

    if (expectResults) {
      assertFalse(codeSystems.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("canmed_202506"));
      final Set<String> urls = new HashSet<>(Set.of("http://seer.nci.nih.gov/CanMED.owl"));

      for (org.hl7.fhir.r5.model.Resource cs : codeSystems) {
        log.info(" code system = " + parser.encodeResourceToString(cs));
        org.hl7.fhir.r5.model.CodeSystem css = (org.hl7.fhir.r5.model.CodeSystem) cs;
        assertNotNull(css);
        assertEquals(org.hl7.fhir.r5.model.ResourceType.CodeSystem, css.getResourceType());
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
   * Test code system search with sort by name.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearchSortByName() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCSPath + "?_sort=name";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by name in ascending order
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    String previousName = null;
    for (Resource cs : codeSystems) {
      CodeSystem css = (CodeSystem) cs;
      assertNotNull(css.getName());
      String currentName = css.getName().toLowerCase();
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
    String endpoint = localHost + port + fhirCSPath + "?_sort=-title";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by title in descending order
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    String previousTitle = null;
    for (Resource cs : codeSystems) {
      CodeSystem css = (CodeSystem) cs;
      assertNotNull(css.getTitle());
      String currentTitle = css.getTitle().toLowerCase();
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
    String endpoint = localHost + port + fhirCSPath + "?_sort=publisher";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by publisher in ascending order
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    String previousPublisher = null;
    for (Resource cs : codeSystems) {
      CodeSystem css = (CodeSystem) cs;
      assertNotNull(css.getPublisher());
      String currentPublisher = css.getPublisher().toLowerCase();
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
    String endpoint = localHost + port + fhirCSPath + "?_sort=date";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by date in ascending order
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    Date previousDate = null;
    for (Resource cs : codeSystems) {
      CodeSystem css = (CodeSystem) cs;
      Date currentDate = css.getDate();
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
    String endpoint = localHost + port + fhirCSPath + "?_sort=url";

    // Act
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Assert
    assertNotNull(data);
    assertFalse(data.getEntry().isEmpty());

    // Verify that results are sorted by URL in ascending order
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    assertNotNull(codeSystems);

    String previousUrl = null;
    for (Resource cs : codeSystems) {
      CodeSystem css = (CodeSystem) cs;
      assertNotNull(css.getUrl());
      String currentUrl = css.getUrl().toLowerCase();
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
    String endpoint = localHost + port + fhirCSPath + "?_sort=invalid_field";

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
