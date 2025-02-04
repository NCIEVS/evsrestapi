package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.TestProperties;
import org.hl7.fhir.r4.model.BooleanType;
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
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Class tests for FhirR4Tests. Tests the functionality of the FHIR R4 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR4CodeSystemValidateTests {

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
   * Test code system validate active code.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemValidateActiveCode() throws Exception {
    // Arrange
    String content;
    String activeCode = "T100";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
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
    String activeCode = "T100";
    String activeId = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode + "&display" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
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
    String messageNotFound =
        "The code does not exist for the supplied code system url and/or version";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + codeNotFound;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        messageNotFound, ((StringType) params.getParameter("message").getValue()).getValue());
  }

  /**
   * Test code system code not found and display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemCodeNotFoundAndDisplayString() throws Exception {
    // Arrange
    String content;
    String activeId = "umlssemnet_2023aa";
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String messageNotFound =
        "The code does not exist for the supplied code system url and/or version";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + codeNotFound + "&display" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
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
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String retiredName = "ABCB1 1 Allele";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + retiredUrl + "&code=" + retiredCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String retiredId = "ncit_21.06e";
    String retiredName = "ABCB1 1 Allele";
    String endpoint =
        localHost
            + port
            + fhirCSPath
            + "/"
            + retiredId
            + "/"
            + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + retiredUrl + "&code=" + retiredCode + "&display=" + retiredName;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String code = "C3224";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/TheBadTest.owl";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?code=" + code + "&url=" + url;
    String messageNotFound = "Unable to find matching code system";
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
   * Test code system bad.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemBadInstance() throws Exception {
    // Arrange
    String content;
    String code = "C3224";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/TheBadTest.owl";
    String activeId = "umlssemnet_2023aa";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?code=" + code + "&url=" + url;
    String messageNotFound = "Unable to find matching code system";
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
    String message = "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE;
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?code=" + null + "&system=" + null;

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
    String message = "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE;
    String activeId = "umlssemnet_2023aa";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?code=" + null + "&system=" + null;

    // Act
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);

    // Assert
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }
}
