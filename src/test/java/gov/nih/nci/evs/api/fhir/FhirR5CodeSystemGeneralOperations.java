package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit FhirR5Tests. Tests the functionality of the FHIR R5 endpoint CodeSystem specifically the 
 * general operations.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FhirR5CodeSystemGeneralOperations {

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
   * Test code system validate.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemValidate() throws Exception {
    // Arrange
    String content;
    String codeSystemId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirCSPath;

    String messageNotFound = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/$validate] with parameters [[]]";
    String messageNotFoundWithId = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/umlssemnet_2023aa/$validate] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/$validate", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
    
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId + "/$validate", String.class);
    outcome = parser.parseResource(OperationOutcome.class, content);
    component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test code system meta.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemMeta() throws Exception {
    // Arrange
    String content;
    String codeSystemId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirCSPath;

    String messageNotFound = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/$meta] with parameters [[]]";
    String messageNotFoundWithId = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/umlssemnet_2023aa/$meta] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/$meta", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
    
    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId + "/$meta", String.class);
    outcome = parser.parseResource(OperationOutcome.class, content);
    component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
 
  }

  /**
   * Test code system graphql.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemGraphql() throws Exception {
    // Arrange
    String content;
    String codeSystemId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirCSPath;

    String messageNotFound = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/$graphql] with parameters [[]]";
    String messageNotFoundWithId = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/umlssemnet_2023aa/$graphql] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/$graphql", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
    
    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId + "/$graphql", String.class);
    outcome = parser.parseResource(OperationOutcome.class, content);
    component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }
  
  /**
   * Test code system graph.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemGraph() throws Exception {
    // Arrange
    String content;
    String codeSystemId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirCSPath;

    String messageNotFoundWithId = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/umlssemnet_2023aa/$graph] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId + "/$graph", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
   
  }
  
  /**
   * Test code system add.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemAdd() throws Exception {
    // Arrange
    String content;
    String codeSystemId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirCSPath;

    String messageNotFoundWithId = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/umlssemnet_2023aa/$add] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId + "/$add", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
   
  }
  
  /**
   * Test code system remove.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemRemove() throws Exception {
    // Arrange
    String content;
    String codeSystemId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirCSPath;

    String messageNotFoundWithId = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/umlssemnet_2023aa/$remove] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId + "/$remove", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
   
  }
  
  /**
   * Test code system filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemFilter() throws Exception {
    // Arrange
    String content;
    String codeSystemId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirCSPath;

    String messageNotFoundWithId = "Invalid request: The FHIR endpoint on this server does not know how to handle GET operation[CodeSystem/umlssemnet_2023aa/$filter] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId + "/$filter", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
   
  }
}
