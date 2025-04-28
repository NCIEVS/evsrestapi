package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Unit FhirR5Tests. Tests the functionality of the FHIR R5 endpoint ValueSet specifically the
 * general operations.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FhirR5ValueSetGeneralOperations {

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** Fhir url paths. */
  private final String fhirVSPath = "/fhir/r5/ValueSet";

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
   * Test value set validate.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetValidate() throws Exception {
    // Arrange
    String content;
    String valueSetId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirVSPath;

    String messageNotFound =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/$validate] with parameters [[]]";
    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/umlssemnet_2023aa/$validate] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/$validate", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));

    content =
        this.restTemplate.getForObject(endpoint + "/" + valueSetId + "/$validate", String.class);
    outcome = parser.parseResource(OperationOutcome.class, content);
    component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test value set meta.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetMeta() throws Exception {
    // Arrange
    String content;
    String valueSetId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirVSPath;

    String messageNotFound =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/$meta] with parameters [[]]";
    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/umlssemnet_2023aa/$meta] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/$meta", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + valueSetId + "/$meta", String.class);
    outcome = parser.parseResource(OperationOutcome.class, content);
    component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test value set graphql.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetGraphql() throws Exception {
    // Arrange
    String content;
    String valueSetId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirVSPath;

    String messageNotFound =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/$graphql] with parameters [[]]";
    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/umlssemnet_2023aa/$graphql] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/$graphql", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));

    // Act
    content =
        this.restTemplate.getForObject(endpoint + "/" + valueSetId + "/$graphql", String.class);
    outcome = parser.parseResource(OperationOutcome.class, content);
    component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test value set graph.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetGraph() throws Exception {
    // Arrange
    String content;
    String valueSetId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirVSPath;

    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/umlssemnet_2023aa/$graph] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + valueSetId + "/$graph", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test value set add.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetAdd() throws Exception {
    // Arrange
    String content;
    String valueSetId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirVSPath;

    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/umlssemnet_2023aa/$add] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + valueSetId + "/$add", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test value set remove.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetRemove() throws Exception {
    // Arrange
    String content;
    String valueSetId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirVSPath;

    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/umlssemnet_2023aa/$remove] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content =
        this.restTemplate.getForObject(endpoint + "/" + valueSetId + "/$remove", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test value set filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetFilter() throws Exception {
    // Arrange
    String content;
    String valueSetId = "umlssemnet_2023aa";
    String endpoint = localHost + port + fhirVSPath;

    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ValueSet/umlssemnet_2023aa/$filter] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content =
        this.restTemplate.getForObject(endpoint + "/" + valueSetId + "/$filter", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }
}
