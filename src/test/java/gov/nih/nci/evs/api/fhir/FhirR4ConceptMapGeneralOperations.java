package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
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
 * Unit FhirR4Tests. Tests the functionality of the FHIR R4 endpoint ConceptMap specifically the
 * general operations.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FhirR4ConceptMapGeneralOperations {

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** Fhir url paths. */
  private final String fhirCMPath = "/fhir/r4/ConceptMap";

  /** The Parser. */
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
    // the object mapper
    ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  /**
   * Test concept map validate.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapValidate() throws Exception {
    // Arrange
    String content;
    String conceptMapId = "icd10_to_meddra_mapping_july2023";
    String endpoint = localHost + port + fhirCMPath;

    String messageNotFound =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/$validate] with parameters [[]]";
    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/"
            + conceptMapId
            + "/$validate] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/$validate", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));

    content =
        this.restTemplate.getForObject(endpoint + "/" + conceptMapId + "/$validate", String.class);
    outcome = parser.parseResource(OperationOutcome.class, content);
    component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test concept map meta.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapMeta() throws Exception {
    // Arrange
    String content;
    String conceptMapId = "icd10_to_meddra_mapping_july2023";
    String endpoint = localHost + port + fhirCMPath;

    String messageNotFound =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/$meta] with parameters [[]]";
    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/"
            + conceptMapId
            + "/$meta] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/$meta", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));

    // Act
    content =
        this.restTemplate.getForObject(endpoint + "/" + conceptMapId + "/$meta", String.class);
    outcome = parser.parseResource(OperationOutcome.class, content);
    component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test concept map graphql.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapGraphql() throws Exception {
    // Arrange
    String content;
    String conceptMapId = "icd10_to_meddra_mapping_july2023";
    String endpoint = localHost + port + fhirCMPath;

    String messageNotFound =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/$graphql] with parameters [[]]";
    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/"
            + conceptMapId
            + "/$graphql] with parameters [[]]";
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
        this.restTemplate.getForObject(endpoint + "/" + conceptMapId + "/$graphql", String.class);
    outcome = parser.parseResource(OperationOutcome.class, content);
    component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test concept map graph.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapGraph() throws Exception {
    // Arrange
    String content;
    String conceptMapId = "icd10_to_meddra_mapping_july2023";
    String endpoint = localHost + port + fhirCMPath;

    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/"
            + conceptMapId
            + "/$graph] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content =
        this.restTemplate.getForObject(endpoint + "/" + conceptMapId + "/$graph", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test concept map add.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapAdd() throws Exception {
    // Arrange
    String content;
    String conceptMapId = "icd10_to_meddra_mapping_july2023";
    String endpoint = localHost + port + fhirCMPath;

    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/"
            + conceptMapId
            + "/$add] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + conceptMapId + "/$add", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test concept map remove.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapRemove() throws Exception {
    // Arrange
    String content;
    String conceptMapId = "icd10_to_meddra_mapping_july2023";
    String endpoint = localHost + port + fhirCMPath;

    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/"
            + conceptMapId
            + "/$remove] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content =
        this.restTemplate.getForObject(endpoint + "/" + conceptMapId + "/$remove", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }

  /**
   * Test concept map filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapFilter() throws Exception {
    // Arrange
    String content;
    String conceptMapId = "icd10_to_meddra_mapping_july2023";
    String endpoint = localHost + port + fhirCMPath;

    String messageNotFoundWithId =
        "Invalid request: The FHIR endpoint on this server does not know how to handle GET"
            + " operation[ConceptMap/"
            + conceptMapId
            + "/$filter] with parameters [[]]";
    String errorCode = "not-supported";

    // Act
    content =
        this.restTemplate.getForObject(endpoint + "/" + conceptMapId + "/$filter", String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFoundWithId, (component.getDiagnostics()));
  }
}
