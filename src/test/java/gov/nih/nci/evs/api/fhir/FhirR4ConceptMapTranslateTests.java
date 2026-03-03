package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.net.URI;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
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
import org.springframework.web.util.UriComponentsBuilder;

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
    final String code = "GO:0016887";
    final String id = "go_to_ncit_mapping_february2020";
    final String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    final String endpoint =
        localHost + port + fhirCMPath + "/" + id + "/" + JpaConstants.OPERATION_TRANSLATE;
    final String parameters = "?code=" + code + "&system=" + system;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

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
    final String code = "GO:0016887";
    final String id = "go_to_ncit_mapping_february2020";
    final String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    final String endpoint =
        localHost + port + fhirCMPath + "/" + id + "/" + JpaConstants.OPERATION_TRANSLATE;
    final String parameters = "?code=" + code + "&system=" + system + "&codableConcept=notfound";

    final String messageNotSupported = "Input parameter 'codableConcept' is not supported.";
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
   * Test concept map translate with instance system with reverse = true; id, code, system, and
   * display provided.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMaptTranslateInstanceWithReverse() throws Exception {
    // Arrange
    String content;
    final String code = "C19939";
    final String id = "go_to_ncit_mapping_february2020";
    final String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    final String reverse = "true";
    final String endpoint =
        localHost + port + fhirCMPath + "/" + id + "/" + JpaConstants.OPERATION_TRANSLATE;
    final String parameters = "?code=" + code + "&system=" + system + "&reverse=" + reverse;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);
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
    final String code = "GO:0016887";
    final String targetCode = "C19939";
    final String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    final String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    final String parameters = "?code=" + code + "&system=" + system;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);
    log.info("  translate params =\n" + parser.encodeResourceToString(params));

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    final Parameters.ParametersParameterComponent matchParam = params.getParameter("match");
    assertNotNull(matchParam, "Match parameter should be present");

    final Coding coding =
        (Coding)
            matchParam.getPart().stream()
                .filter(part -> "concept".equals(part.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Concept part not found"))
                .getValue();

    assertEquals(targetCode, coding.getCode());
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
    final String code = "GO:0016887";
    final String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    final String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    final String parameters = "?code=" + code + "&system=" + system + "&codableConcept=notfound";

    final String messageNotSupported = "Input parameter 'codableConcept' is not supported.";
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
   * Test concept map translate with implicit system with reverse = true; code and system provided.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapTranslateImplicitWithReverse() throws Exception {
    // Arrange
    String content;
    final String code = "C19939";
    final String sourceCode = "GO:0016887";
    final String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    final String reverse = "true";
    final String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    final String parameters = "?code=" + code + "&system=" + system + "&reverse=" + reverse;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    final Parameters.ParametersParameterComponent matchParam = params.getParameter("match");
    assertNotNull(matchParam, "Match parameter should be present");

    final Coding coding =
        (Coding)
            matchParam.getPart().stream()
                .filter(part -> "concept".equals(part.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Concept part not found"))
                .getValue();

    assertEquals(sourceCode, coding.getCode());
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
    final String message = "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE;

    final String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
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
   * Test the ConceptMap rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapPostRejectsInstance() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    final String message = "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE;
    final String activeID = "umlssemnet_2023aa";
    final String endpoint =
        localHost + port + fhirCMPath + "/" + activeID + "/" + JpaConstants.OPERATION_TRANSLATE;
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
   * Test concept map translate instance with source coding.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapTranslateInstanceWithSourceCoding() throws Exception {
    // Arrange
    final String code = "GO:0016887";
    final String id = "go_to_ncit_mapping_february2020";
    final String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    final String endpoint =
        localHost + port + fhirCMPath + "/" + id + "/" + JpaConstants.OPERATION_TRANSLATE;

    // Create the Coding object
    final Coding sourceCoding = new Coding(system, code, null);

    // Construct the GET request URI with the sourceCoding parameter
    final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("coding", sourceCoding.getSystem() + "|" + sourceCoding.getCode());

    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
  }

  /**
   * Test concept map translate implicit with source coding.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapTranslateImplicitWithSourceCoding() throws Exception {
    // Arrange
    final String code = "GO:0016887";
    final String targetCode = "C19939";
    final String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    final String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;

    // Create the Coding object
    final Coding sourceCoding = new Coding(system, code, null);

    // Construct the GET request URI with the sourceCoding parameter
    final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("coding", sourceCoding.getSystem() + "|" + sourceCoding.getCode());

    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    final Parameters.ParametersParameterComponent matchParam = params.getParameter("match");
    assertNotNull(matchParam, "Match parameter should be present");

    final Coding coding =
        (Coding)
            matchParam.getPart().stream()
                .filter(part -> "concept".equals(part.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Concept part not found"))
                .getValue();

    assertEquals(targetCode, coding.getCode());
  }

  @Test
  public void testConceptMapTranslateImplicitCodeWithBothCodeAndCoding() throws Exception {
    // Arrange
    String content;
    final String code = "GO:0016887";
    final String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    final String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    final Coding coding = new Coding(system, code, null);

    final String messageNotSupported = "Use one of 'code' or 'coding' parameters.";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("code", code);
    builder.queryParam("system", system);
    builder.queryParam("coding", coding.getSystem() + "|" + coding.getCode());
    final URI getUri = builder.build().toUri();

    // Act
    content = this.restTemplate.getForObject(getUri, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }

  /**
   * Test concept map translate implicit code with no system.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapTranslateImplicitCodeWithNoSystem() throws Exception {
    // Arrange
    final String code = "GO:0016887";

    final String messageNotSupported =
        "Input parameter 'code' can only be used in conjunction with parameter 'system'.";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE);
    builder.queryParam("code", code);
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
