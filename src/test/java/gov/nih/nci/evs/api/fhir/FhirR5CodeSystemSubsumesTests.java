package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.TestProperties;
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
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

// TODO: Auto-generated Javadoc
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

  /** The object mapper. */
  private ObjectMapper objectMapper;

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
    // The object mapper
    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  // TODO: test invalid parameter such as url instead of system

  /**
   * Test code system lookup code.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSubsumedByImplicit() throws Exception {
    // Arrange
    String content;
    String activeCodeB = "448772000";
    String activeCodeA = "271860004";
    String url = "http://snomed.info/sct";
    String outcome = "subsumed-by";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?system=" + url + "&codeA=" + activeCodeA + "&codeB=" + activeCodeB;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String activeCodeA = "271860004";
    String url = "http://snomed.info/sct";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?system=" + url + "&codeA=" + activeCodeA;
    String errorCode = "exception";
    String messageNotFound = "No codeB parameter provided in request";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    String activeCodeA = "448772000";
    String activeCodeB = "271860004";
    String url = "http://snomed.info/sct";
    String outcome = "subsumes";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?system=" + url + "&codeA=" + activeCodeA + "&codeB=" + activeCodeB;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String url = "http://snomed.info/sct";
    String activeCodeA = "448772000";
    String activeCodeB = "271860004";
    String outcome = "subsumes";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;

    // Create Coding objects
    Coding codingA = new Coding(url, activeCodeA, null);
    Coding codingB = new Coding(url, activeCodeB, null);

    // Construct the GET request URI with codingA and codingB parameters
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("codingA", codingA.getSystem() + "|" + codingA.getCode());
    builder.queryParam("codingB", codingB.getSystem() + "|" + codingB.getCode());

    URI getUri = builder.build().toUri();

    // Act
    String content = this.restTemplate.getForObject(getUri, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(outcome, ((StringType) params.getParameter("outcome").getValue()).getValue());
  }

  @Test
  public void testCodeSystemSubsumesInstanceWithCoding() throws Exception {
    // Arrange
    String url = "http://snomed.info/sct";
    String activeId = "snomedct_us_2020_09_01";
    String activeCodeA = "448772000";
    String activeCodeB = "271860004";
    String outcome = "subsumes";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_SUBSUMES;

    // Create Coding objects
    Coding codingA = new Coding(url, activeCodeA, null);
    Coding codingB = new Coding(url, activeCodeB, null);

    // Construct the GET request URI with codingA and codingB parameters
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("codingA", codingA.getSystem() + "|" + codingA.getCode());
    builder.queryParam("codingB", codingB.getSystem() + "|" + codingB.getCode());

    URI getUri = builder.build().toUri();

    // Act
    String content = this.restTemplate.getForObject(getUri, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String activeCodeB = "448772000";
    String activeCodeA = "271860004";
    String activeId = "snomedct_us_2020_09_01";
    String url = "http://snomed.info/sct";
    String outcome = "subsumed-by";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?system=" + url + "&codeA=" + activeCodeA + "&codeB=" + activeCodeB;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String messageNotFound = "Failed to check if A subsumes B";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?system=" + url + "&codeA=" + codeNotFound + "&codeB=" + codeNotFound;
    String errorCode = "exception";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    String retiredCodeB = "C45683";
    String activeCodeA = "C17966";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String outcome = "no-subsumption-relationship";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?system=" + url + "&codeA=" + activeCodeA + "&codeB=" + retiredCodeB;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String codeA = "2222222222222222222";
    String codeB = "3333333333333333333";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/TheBadTest.owl";
    String messageNotFound = "Unable to find matching code system";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?codeA=" + codeA + "&codeB=" + codeB + "&system=" + url;
    String errorCode = "not-found";

    // Act
    content = restTemplate.getForObject(endpoint + parameters, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    String codeA = "2222222222222222222";
    String codeB = "3333333333333333333";
    String activeId = "snomedct_us_2020_09_01";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/TheBadTest.owl";
    String messageNotFound = "Unable to find matching code system";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?codeA=" + codeA + "&codeB=" + codeB + "&system=" + url;
    String errorCode = "not-found";

    // Act
    content = restTemplate.getForObject(endpoint + parameters, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    String message = "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES;
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?codeA=" + null + "&codeB=" + null + "&system=" + null;

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
    String message = "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES;
    String activeId = "snomedct_us_2020_09_01";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_SUBSUMES;
    String parameters = "?codeA=" + null + "&codeB=" + null + "&system=" + null;

    // Act
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);

    // Assert
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }
}
