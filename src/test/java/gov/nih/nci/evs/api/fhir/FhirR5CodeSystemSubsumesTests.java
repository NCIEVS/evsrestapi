package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.net.URI;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.StringType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Class tests for FhirR5Tests. Tests the functionality of the FHIR R5 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR5CodeSystemSubsumesTests {

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** Fhir url paths. */
  private final String fhirCSPath = "/fhir/r5/CodeSystem";

  /** The parser. */
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
    // n/a
  }

  /**
   * Test code system lookup code.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSubsumedByImplicit() throws Exception {
    // Arrange
    String content;
    final String activeCodeB = "448772000";
    final String activeCodeA = "271860004";
    final String url = "http://snomed.info/sct";
    final String outcome = "subsumed-by";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters = "?system=" + url + "&codeA=" + activeCodeA + "&codeB=" + activeCodeB;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(outcome, ((StringType) params.getParameter("outcome").getValue()).getValue());
  }

  /**
   * Test code system missing input.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemMissingInput() throws Exception {
    // Arrange
    String content;
    final String activeCodeA = "271860004";
    final String url = "http://snomed.info/sct";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters = "?system=" + url + "&codeA=" + activeCodeA;
    final String errorCode = "exception";
    final String messageNotFound = "No codeB parameter provided in request";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test code system subsumes implicit.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSubsumesImplicit() throws Exception {
    // Arrange
    String content;
    final String activeCodeA = "448772000";
    final String activeCodeB = "271860004";
    final String url = "http://snomed.info/sct";
    final String outcome = "subsumes";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters = "?system=" + url + "&codeA=" + activeCodeA + "&codeB=" + activeCodeB;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(outcome, ((StringType) params.getParameter("outcome").getValue()).getValue());
  }

  /**
   * Test code system subsumes coding.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSubsumesImplicitWithCoding() throws Exception {
    // Arrange
    final String url = "http://snomed.info/sct";
    final String activeCodeA = "448772000";
    final String activeCodeB = "271860004";
    final String outcome = "subsumes";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;

    // Create Coding objects
    final Coding codingA = new Coding(url, activeCodeA, null);
    final Coding codingB = new Coding(url, activeCodeB, null);

    // Construct the GET request URI with codingA and codingB parameters
    final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("codingA", codingA.getSystem() + "|" + codingA.getCode());
    builder.queryParam("codingB", codingB.getSystem() + "|" + codingB.getCode());

    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(outcome, ((StringType) params.getParameter("outcome").getValue()).getValue());
  }

  @Test
  public void testCodeSystemSubsumesInstanceWithCoding() throws Exception {
    // Arrange
    final String url = "http://snomed.info/sct";
    final String activeId = "snomedct_us_2020_09_01";
    final String activeCodeA = "448772000";
    final String activeCodeB = "271860004";
    final String outcome = "subsumes";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_SUBSUMES;

    // Create Coding objects
    final Coding codingA = new Coding(url, activeCodeA, null);
    final Coding codingB = new Coding(url, activeCodeB, null);

    // Construct the GET request URI with codingA and codingB parameters
    final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("codingA", codingA.getSystem() + "|" + codingA.getCode());
    builder.queryParam("codingB", codingB.getSystem() + "|" + codingB.getCode());

    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(outcome, ((StringType) params.getParameter("outcome").getValue()).getValue());
  }

  /**
   * Test code system lookup code and display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSubsumesInstance() throws Exception {
    // Arrange
    String content;
    final String activeCodeB = "448772000";
    final String activeCodeA = "271860004";
    final String activeId = "snomedct_us_2020_09_01";
    final String url = "http://snomed.info/sct";
    final String outcome = "subsumed-by";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters = "?system=" + url + "&codeA=" + activeCodeA + "&codeB=" + activeCodeB;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(outcome, ((StringType) params.getParameter("outcome").getValue()).getValue());
  }

  /**
   * Test code system validate code not found.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemCodeNotFound() throws Exception {
    // Arrange
    String content;
    final String codeNotFound = "T10";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String messageNotFound = "Failed to check if A subsumes B";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters =
        "?system=" + url + "&codeA=" + codeNotFound + "&codeB=" + codeNotFound;
    final String errorCode = "exception";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test code system retired code.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemRetiredCode() throws Exception {
    // Arrange
    String content;
    final String retiredCodeB = "C45683";
    final String activeCodeA = "C17966";
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    final String outcome = "no-subsumption-relationship";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters = "?system=" + url + "&codeA=" + activeCodeA + "&codeB=" + retiredCodeB;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(outcome, ((StringType) params.getParameter("outcome").getValue()).getValue());
  }

  /**
   * Test code system bad implicit.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemBadImplicit() throws Exception {
    // Arrange
    String content;
    final String codeA = "2222222222222222222";
    final String codeB = "3333333333333333333";
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/TheBadTest.owl";
    final String messageNotFound = "Unable to find matching code system";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters = "?codeA=" + codeA + "&codeB=" + codeB + "&system=" + url;
    final String errorCode = "not-found";

    // Act
    content = restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test code system bad instance.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemBadInstance() throws Exception {
    // Arrange
    String content;
    final String codeA = "2222222222222222222";
    final String codeB = "3333333333333333333";
    final String activeId = "snomedct_us_2020_09_01";
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/TheBadTest.owl";
    final String messageNotFound = "Unable to find matching code system";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters = "?codeA=" + codeA + "&codeB=" + codeB + "&system=" + url;
    final String errorCode = "not-found";

    // Act
    content = restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test the CodeSystem rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemPostRejectsImplicit() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    final String message = "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES;
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters = "?codeA=" + null + "&codeB=" + null + "&system=" + null;

    // Act
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);

    // Assert
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }

  /**
   * Test the CodeSystem rejects a post call when attempted.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemPostRejectsInstance() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    final String message = "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES;
    final String activeId = "snomedct_us_2020_09_01";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_SUBSUMES;
    final String parameters = "?codeA=" + null + "&codeB=" + null + "&system=" + null;

    // Act
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);

    // Assert
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }

  /**
   * Test code system subsumes implicit with both code and coding get.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSubsumesImplicitWithBothCodeAndCodingGet() throws Exception {
    // Arrange
    final String url = "http://snomed.info/sct";
    final String activeCodeA = "448772000";
    final String activeCodeB = "271860004";
    final Coding codingA = new Coding(url, activeCodeA, null);

    final String messageNotSupported = "Must use one of 'codingA' or 'codeA' parameters";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES);
    builder.queryParam("codeA", activeCodeA);
    builder.queryParam("system", url);
    builder.queryParam("codingA", codingA.getSystem() + "|" + codingA.getCode());
    builder.queryParam("codeB", activeCodeB);
    builder.queryParam("systemB", url);
    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, component.getDiagnostics());
  }

  /**
   * Test code system subsumes implicit with code A no system.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSubsumesImplicitWithCodeANoSystem() throws Exception {
    // Arrange
    final String activeCodeA = "448772000";
    final String activeCodeB = "271860004";
    final String url = "http://snomed.info/sct";

    final String messageNotSupported =
        "Input parameter 'codeA' can only be used in conjunction with parameter 'system'.";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES);
    builder.queryParam("codeA", activeCodeA);
    builder.queryParam("codeB", activeCodeB);
    builder.queryParam("systemB", url);
    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, component.getDiagnostics());
  }

  /**
   * Test subsumes operation with url parameter instead of system (implicit).
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSubsumesImplicitWithUrlInsteadOfSystem() throws Exception {
    // Arrange
    final String activeCodeA = "448772000";
    final String activeCodeB = "271860004";
    final String url = "http://snomed.info/sct";

    final String messageNotSupported =
        "Input parameter 'codeA' can only be used in conjunction with parameter 'system'.";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES);
    builder.queryParam("codeA", activeCodeA);
    builder.queryParam("codeB", activeCodeB);
    builder.queryParam("url", url); // Using 'url' instead of 'system'
    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, component.getDiagnostics());
  }

  /**
   * Test subsumes operation with url parameter instead of system (instance).
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSubsumesInstanceWithUrlInsteadOfSystem() throws Exception {
    // Arrange
    final String activeCodeA = "448772000";
    final String activeCodeB = "271860004";
    final String activeId = "snomedct_us_2025_03_01";
    final String url = "http://snomed.info/sct";

    final String messageNotSupported =
        "Input parameter 'codeA' can only be used in conjunction with parameter 'system'.";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_SUBSUMES);
    builder.queryParam("codeA", activeCodeA);
    builder.queryParam("codeB", activeCodeB);
    builder.queryParam("url", url); // Using 'url' instead of 'system'
    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, component.getDiagnostics());
  }
}
