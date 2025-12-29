package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.net.URI;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
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
 * Class tests for FhirR4Tests. Tests the functionality of the FHIR R4 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR4ValueSetValidateTests {

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** The test properties. */
  @Autowired TestProperties testProperties;

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
  public void setUp() {}

  /**
   * Test value set validate active code.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetValidateActiveCode() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String displayString = "Age Group";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + activeCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
  }

  @Test
  public void testValueSetValidateActiveImplicitCodeWithCoding() throws Exception {
    // Arrange
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String displayString = "Age Group";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;

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
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
  }

  @Test
  public void testValueSetValidateActiveInstanceCodeWithCoding() throws Exception {
    // Arrange
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String activeId = "umlssemnet_2023aa";
    final String displayString = "Age Group";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;

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
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
  }

  /**
   * Test value set validate active code parameter not supported.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetValidateActiveCodeParameterNotSupported() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters =
        "?url=" + url + "&code=" + activeCode + "&displayLanguage=not_supported";

    final String errorCode = "not-supported";
    final String messageNotSupported = "Input parameter 'displayLanguage' is not supported.";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }

  /**
   * Test value set validate active code invariant parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetValidateActiveCodeInvariantParameters() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    // String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?code=" + activeCode + "&displayLanguage=not_supported";

    final String errorCode = "invariant";
    final String messageInvariant =
        "Use of input parameter 'code' only allowed if 'system' or 'url' is also present.";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageInvariant, (component.getDiagnostics()));
  }

  /**
   * Test value set validate active id, active code and display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetValidateActiveIdAndActiveCodeAndDisplayString() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String displayString = "Age Group";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode + "&display=" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());

    parameters = "?code=" + activeCode + "&display" + displayString;

    // Act - Test 2 with no url
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());

    url = "invalid_url";
    parameters = "?url=" + url + "&code=" + activeCode + "&display" + displayString;
    final String messageNotFound =
        "Supplied url UriType[invalid_url] doesn't match the ValueSet retrieved by the id"
            + " ValueSet/umlssemnet_2023aa"
            + " http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String errorCode = "exception";

    // Act - Test 3 with invalid url
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);

    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test value set validate active id and active code and incorrect display string.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetValidateActiveIdAndActiveCodeAndIncorrectDisplayString()
      throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String activeID = "umlssemnet_2023aa";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String displayString = "INCORRECT";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + activeCode + "&display=" + displayString;
    final String message =
        "The code '"
            + activeCode
            + "' was found in this value set, however the display '"
            + displayString
            + "' did not match any designations.";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertNotEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertEquals(message, ((StringType) params.getParameter("message").getValue()).getValue());
  }

  /**
   * Test value set validate for active code and display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetValidateActiveCodeAndDisplayString() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String displayString = "Age Group";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + activeCode + "&display=" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
  }

  /**
   * Test value set validate code not found.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetCodeNotFound() throws Exception {
    // Arrange
    String content;
    final String codeNotFound = "T10";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String messageNotFound = "The code '" + codeNotFound + "' was not found.";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + codeNotFound;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        messageNotFound, ((StringType) params.getParameter("message").getValue()).getValue());
  }

  /**
   * Test value set code not found for display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetCodeNotFoundAndDisplayString() throws Exception {
    // Arrange
    String content;
    final String codeNotFound = "T10";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String displayString = "Age Group";
    final String messageNotFound = "The code '" + codeNotFound + "' was not found.";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + codeNotFound + "&display=" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        messageNotFound, ((StringType) params.getParameter("message").getValue()).getValue());
  }

  /**
   * Test value set code not found for activde Id and display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetCodeNotFoundActiveIdAndDisplayString() throws Exception {
    // Arrange
    String content;
    final String activeID = "umlssemnet_2023aa";
    final String codeNotFound = "10";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    final String displayString = "Age Group";
    final String messageNotFound = "The code '" + codeNotFound + "' was not found.";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + url + "&code=" + codeNotFound + "&display=" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        messageNotFound, ((StringType) params.getParameter("message").getValue()).getValue());
  }

  /**
   * Test value set for retired code.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetRetiredCode() throws Exception {
    // Arrange
    String content;
    final String retiredCode = "C45683";
    final String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    final String retiredName = "ABCB1 1 Allele";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters = "?url=" + retiredUrl + "&code=" + retiredCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
  }

  /**
   * Test value set for retired code and retired display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetRetiredCodeAndRetireDisplayString() throws Exception {
    // Arrange
    String content;
    final String retiredCode = "C45683";
    final String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    final String retiredName = "ABCB1 1 Allele";
    final String endpoint =
        localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    final String parameters =
        "?url=" + retiredUrl + "&code=" + retiredCode + "&display=" + retiredName;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
  }

  /**
   * Test value set for retired id, retired code and retired display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetRetiredIdRetiredCodeAndRetireDisplayString() throws Exception {
    // Arrange
    String content;
    final String retiredCode = "C45683";
    final String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    final String retiredId = "ncit_25.06e";
    final String retiredName = "ABCB1 1 Allele";
    final String endpoint =
        localHost
            + port
            + fhirVSPath
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
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
  }

  /**
   * Test the ValueSet rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetPostRejects() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    final String message = "POST method not supported for " + JpaConstants.OPERATION_EXPAND;

    final String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    final String parameters = "?code=" + null + "&system=" + null;

    // Act
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);

    // Assert
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }

  @Test
  public void testValueSetValidateImplicitCodeWithBothCodeAndCoding() throws Exception {
    // Arrange
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final Coding coding = new Coding(url, activeCode, null);

    final String messageNotSupported = "Use one of 'code' or 'coding' parameters.";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE);
    builder.queryParam("code", activeCode);
    builder.queryParam("url", url);
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
   * Test value set validate implicit code with no system.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetValidateImplicitCodeWithNoSystem() throws Exception {
    // Arrange
    final String activeCode = "T100";

    final String messageNotSupported =
        "Use of input parameter 'code' only allowed if 'system' or 'url' is also present.";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE);
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
