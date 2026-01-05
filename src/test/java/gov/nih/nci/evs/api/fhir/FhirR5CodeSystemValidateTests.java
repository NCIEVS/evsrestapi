package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.net.URI;
import org.hl7.fhir.r5.model.BooleanType;
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
public class FhirR5CodeSystemValidateTests {

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
   * Test code system validate active code.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemValidateActiveCode() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + activeCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
  }

  /**
   * Test code system validate code with coding.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemValidateImplicitCodeWithCoding() throws Exception {
    // Arrange
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String version = "2023AA";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;

    // Create the Coding object
    final Coding coding = new Coding(url, activeCode, null);

    // Construct the GET request URI with the coding parameter
    final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("coding", coding.getSystem() + "|" + coding.getCode());

    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
    assertEquals(version, ((StringType) params.getParameter("version").getValue()).getValue());
  }

  /**
   * Test code system validate instance code with coding.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemValidateInstanceCodeWithCoding() throws Exception {
    // Arrange
    final String activeCode = "T100";
    final String activeId = "umlssemnet_2023aa";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String version = "2023AA";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;

    // Create the Coding object
    final Coding coding = new Coding(url, activeCode, null);

    // Construct the GET request URI with the coding parameter
    final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("coding", coding.getSystem() + "|" + coding.getCode());

    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
    assertEquals(version, ((StringType) params.getParameter("version").getValue()).getValue());
  }

  /**
   * Test code system validate active code implicit parameter not supported.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemValidateActiveCodeImplicitParameterNotSupported() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + activeCode + "&displayLanguage=notfound";

    final String messageNotSupported = "Input parameter 'displayLanguage' is not supported.";
    final String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }

  /**
   * Test code system validate active code instance parameter not supported.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemValidateActiveCodeInstanceParameterNotSupported() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String activeId = "umlssemnet_2023aa";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + activeCode + "&displayLanguage=notfound";

    final String messageNotSupported = "Input parameter 'displayLanguage' is not supported.";
    final String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }

  /**
   * Test code system validate active code and display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemValidateActiveCodeDisplayString() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String activeId = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode + "&display" + displayString;

    // Act - Test 1 with appropriate URL
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
    parameters = "?code=" + activeCode + "&display" + displayString;

    // Act - Test 2 with no url
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());

    url = "invalid_url";
    parameters = "?url=" + url + "&code=" + activeCode + "&display" + displayString;
    final String messageNotFound =
        "Supplied url or system UriType[invalid_url] doesn't match the CodeSystem retrieved by the"
            + " id CodeSystem/umlssemnet_2023aa"
            + " http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String errorCode = "exception";

    // Act - Test 3 with invalid url
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);

    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
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
    final String messageNotFound =
        "The code does not exist for the supplied code system url and/or version";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + codeNotFound;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        messageNotFound, ((StringType) params.getParameter("message").getValue()).getValue());
  }

  /** Test code system code not found and display string. */
  @Test
  public void testCodeSystemCodeNotFoundAndDisplayString() {
    // Arrange
    String content;
    final String activeId = "umlssemnet_2023aa";
    final String codeNotFound = "T10";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String messageNotFound =
        "The code does not exist for the supplied code system url and/or version";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + codeNotFound + "&display" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        messageNotFound, ((StringType) params.getParameter("message").getValue()).getValue());
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
    final String retiredCode = "C45683";
    final String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    final String retiredName = "ABCB1 1 Allele";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + retiredUrl + "&code=" + retiredCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(retiredCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
  }

  /**
   * Test code system retired code and retired name.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemRetiredCodeAndRetiredName() throws Exception {
    // Arrange
    String content;
    final String retiredCode = "C45683";
    final String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    final String retiredId = "ncit_25.06e";
    final String retiredName = "ABCB1 1 Allele";
    final String endpoint =
        localHost
            + port
            + fhirCSPath
            + "/"
            + retiredId
            + "/"
            + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters =
        "?url=" + retiredUrl + "&code=" + retiredCode + "&display=" + retiredName;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(retiredCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
  }

  /**
   * Test code system bad.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemBadImplicit() throws Exception {
    // Arrange
    String content;
    final String code = "C3224";
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/TheBadTest.owl";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?code=" + code + "&url=" + url;
    final String messageNotFound = "Unable to find matching code system";
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
    final String message = "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE;
    final String endpoint =
        localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?code=" + null + "&system=" + null;

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
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemPostRejectsInstance() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    final String message = "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE;
    final String activeId = "umlssemnet_2023aa";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?code=" + null + "&system=" + null;

    // Act
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);

    // Assert
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }

  /**
   * Test code system validate implicit code with both code and coding.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemValidateImplicitCodeWithBothCodeAndCoding() throws Exception {
    // Arrange
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final Coding coding = new Coding(url, activeCode, null);

    final String messageNotSupported = "Must use one of 'code' or 'coding' parameters";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE);
    builder.queryParam("code", activeCode);
    builder.queryParam("system", url);
    builder.queryParam("coding", coding.getSystem() + "|" + coding.getCode());
    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }

  /**
   * Test code system validate implicit code with no system.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemValidateImplicitCodeWithNoSystem() throws Exception {
    // Arrange
    final String activeCode = "T100";

    final String messageNotSupported =
        "Input parameter 'code' can only be used in conjunction with parameter 'url'.";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE);
    builder.queryParam("code", activeCode);
    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }
}
