package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class FhirR4ConceptMapTranslateTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4ConceptMapTranslateTests.class);

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

  /** The fhir CM path. */
  private final String fhirCMPath = "/fhir/r4/ConceptMap";

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
   * Test concept map translate with instance system; id, code, and system provided.
   *
   * @throws Exception throws exception when error occurs
   */
  @Test
  public void testConceptMapTranslateInstance() throws Exception {
    // Arrange
    String content;
    String code = "GO:0016887";
    String id = "go_to_ncit_mapping_february2020";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String endpoint =
        localHost + port + fhirCMPath + "/" + id + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
  }

  /**
   * Test concept map translate instance parameter not found.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapTranslateInstanceParameterNotFound() throws Exception {
    // Arrange
    String content;
    String code = "GO:0016887";
    String id = "go_to_ncit_mapping_february2020";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String endpoint =
        localHost + port + fhirCMPath + "/" + id + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system + "&codableConcept=notfound";

    String messageNotSupported = "Input parameter 'codableConcept' is not supported";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }

  /**
   * Test concept map translate with instance system with reverse = true; id, code, system, and
   * display provided.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMaptTranslateInstanceWithReverse() throws Exception {
    // Arrange
    String content;
    String code = "C19939";
    String id = "go_to_ncit_mapping_february2020";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String reverse = "true";
    String endpoint =
        localHost + port + fhirCMPath + "/" + id + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system + "&reverse=" + reverse;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
    log.info("  translate params =\n" + parser.encodeResourceToString(params));

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
  }

  /**
   * Test concept map translate with implicit system; code and system provided.
   *
   * @throws Exception throws exception when error occurs
   */
  @Test
  public void testConceptMapTranslateImplicit() throws Exception {
    // Arrange
    String content;
    String code = "GO:0016887";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
    log.info("  translate params =\n" + parser.encodeResourceToString(params));

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
  }

  /**
   * Test concept map translate implicit parameter not found.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapTranslateImplicitParameterNotFound() throws Exception {
    // Arrange
    String content;
    String code = "GO:0016887";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system + "&codableConcept=notfound";

    String messageNotSupported = "Input parameter 'codableConcept' is not supported";
    String errorCode = "not-supported";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }

  /**
   * Test concept map translate with implicit system with reverse = true; code and system provided.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapTranslateImplicitWithReverse() throws Exception {
    // Arrange
    String content;
    String code = "C19939";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String reverse = "true";
    String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system + "&reverse=" + reverse;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
  }

  /**
   * Test the ConceptMap rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapPostRejectsImpicit() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    String message = "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE;

    String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
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
   * Test the ConceptMap rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapPostRejectsInstance() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    String message = "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE;
    String activeID = "umlssemnet_2023aa";
    String endpoint =
        localHost + port + fhirCMPath + "/" + activeID + "/" + JpaConstants.OPERATION_TRANSLATE;
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
