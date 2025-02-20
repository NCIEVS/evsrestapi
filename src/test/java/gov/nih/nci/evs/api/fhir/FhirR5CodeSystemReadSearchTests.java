package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.ResourceType;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit FhirR5Tests. Tests the functionality of the FHIR R5 endpoints,
 * CodeSystem, ValueSet, and ConceptMap. All passed ids MUST be lowercase, so
 * they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FhirR5CodeSystemReadSearchTests {
  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR5CodeSystemReadSearchTests.class);

  /** The port. */
  @LocalServerPort
  private int port;

  /** The rest template. */
  @Autowired
  private TestRestTemplate restTemplate;

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
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
        // "ge2021-06")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e").queryParam("title", "ncit").queryParam("publisher", "NCI")
        .queryParam("name", "NCI Thesaurus 21.06e");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, true); // Expecting results

    // Test 2: Invalid date, all other parameters valid
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("date", "ge2033-01") // Future
        // date
        // -
        // invalid
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e").queryParam("title", "ncit").queryParam("publisher", "NCI")
        .queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 3: Invalid url, all other parameters valid
    builder = UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
        // "ge2021-06")
        .queryParam("url", "http://invalid.system.url").queryParam("version", "21.06e")
        .queryParam("title", "ncit").queryParam("publisher", "NCI")
        .queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 4: Invalid version, all other parameters valid
    builder = UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
        // "ge2021-06")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "invalid_version").queryParam("title", "ncit")
        .queryParam("publisher", "NCI").queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 5: Invalid title, all other parameters valid
    builder = UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
        // "ge2021-06")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e").queryParam("title", "invalid_title")
        .queryParam("publisher", "NCI").queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 6: Invalid publisher, all other parameters valid
    builder = UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
        // "ge2021-06")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e").queryParam("title", "ncit")
        .queryParam("publisher", "InvalidPublisher").queryParam("name", "NCI Thesaurus");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 7: Invalid name, all other parameters valid
    builder = UriComponentsBuilder.fromUriString(endpoint) // .queryParam("date",
        // "ge2021-06")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e").queryParam("title", "ncit").queryParam("publisher", "NCI")
        .queryParam("name", "InvalidName");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 8: Invalid description, all other parameters valid
    // builder = UriComponentsBuilder.fromUriString(endpoint)//
    // .queryParam("date",
    // // "ge2021-06")
    // .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
    // .queryParam("version", "21.06e").queryParam("title",
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
    // .queryParam("version", "21.06e").queryParam("title",
    // "ncit").queryParam("publisher", "NCI")
    // .queryParam("name", "NCI Thesaurus");
    //
    // content =
    // this.restTemplate.getForObject(builder.build().encode().toUri(),
    // String.class);
    // data = parser.parseResource(Bundle.class, content);
    // validateCodeSystemResults(data, false);
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
    UriComponentsBuilder secondPageBuilder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_count", "2").queryParam("_offset", "2");

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
    List<String> fourIds = defaultCodeSystems.subList(0, 4).stream()
        .map(resource -> ((CodeSystem) resource).getIdPart()).sorted().toList();

    List<String> paginatedIds = Stream.concat(firstPageSystems.stream(), secondPageSystems.stream())
        .map(resource -> ((CodeSystem) resource).getIdPart()).sorted().toList();

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
}
