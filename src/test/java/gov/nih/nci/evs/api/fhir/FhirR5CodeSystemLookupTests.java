package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.TestProperties;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
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

/**
 * Class tests for FhirR5Tests. Tests the functionality of the FHIR R5 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR5CodeSystemLookupTests {

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

  /**
   * Test code system lookup code.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupCode() throws Exception {
    // Arrange
    String content;
    String activeCode = "T100";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String name = "UMLS Semantic Network 2023AA";
    String version = "2023AA";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?system=" + url + "&code=" + activeCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(name, ((StringType) params.getParameter("name").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertEquals(version, ((StringType) params.getParameter("version").getValue()).getValue());
  }

  /**
   * Test code system lookup code with coding.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupImplicitCodeWithCoding() throws Exception {
    // Arrange
    String activeCode = "T100";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String name = "UMLS Semantic Network 2023AA";
    String version = "2023AA";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;

    // Create the Coding object
    Coding coding = new Coding(url, activeCode, null);

    // Construct the GET request URI with the coding parameter
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("coding", coding.getSystem() + "|" + coding.getCode());

    URI getUri = builder.build().toUri();

    // Act
    String content = this.restTemplate.getForObject(getUri, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(name, ((StringType) params.getParameter("name").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertEquals(version, ((StringType) params.getParameter("version").getValue()).getValue());
  }


  @Test
  public void testCodeSystemLookupInstanceCodeWithCoding() throws Exception {
    // Arrange
    String activeCode = "T100";
    String activeId = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String name = "UMLS Semantic Network 2023AA";
    String version = "2023AA";
    String endpoint = localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_LOOKUP;

    // Create the Coding object
    Coding coding = new Coding(url, activeCode, null);

    // Construct the GET request URI with the coding parameter
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("coding", coding.getSystem() + "|" + coding.getCode());

    URI getUri = builder.build().toUri();

    // Act
    String content = this.restTemplate.getForObject(getUri, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(name, ((StringType) params.getParameter("name").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertEquals(version, ((StringType) params.getParameter("version").getValue()).getValue());
  }


  /**
   * Test code system lookup implicit parameter not supported.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupImplicitParameterNotSupported() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint =
        localHost + port + fhirCSPath  + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?url=" + url + "&displayLanguage=notfound";

    String messageNotSupported = "Input parameter 'displayLanguage' is not supported.";
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
   * Test code system lookup instance parameter not supported.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupInstanceParameterNotSupported() throws Exception {
    // Arrange
    String content;
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeID + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?url=" + url + "&displayLanguage=notfound";

    String messageNotSupported = "Input parameter 'displayLanguage' is not supported.";
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
   * Test code system lookup code and display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemLookupCodeDisplayString() throws Exception {
    // Arrange
    String content;
    String activeCode = "T100";
    String activeId = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String name = "UMLS Semantic Network 2023AA";
    String version = "2023AA";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?system=" + url + "&code=" + activeCode + "&display" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    // confirm code is active
    assertTrue(
        ((BooleanType) params.getParameter("property").getPart().get(1).getValue()).getValue());
    assertEquals(name, ((StringType) params.getParameter("name").getValue()).getValue());
    assertEquals(version, ((StringType) params.getParameter("version").getValue()).getValue());
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
    String messageNotFound = "Failed to lookup code";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?system=" + url + "&code=" + codeNotFound;
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
    String messageNotFound = "Failed to lookup code";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?system=" + url + "&code=" + codeNotFound + "$display" + displayString;
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
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?system=" + retiredUrl + "&code=" + retiredCode;

    final Set<String> sourceVersions = new HashSet<>(Set.of("21.06e", "21.07a"));
    String retiredName = "ABCB1 1 Allele";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
    assertThat(sourceVersions)
        .contains(((StringType) params.getParameter("version").getValue()).getValue());

    // get returned properties
    List<ParametersParameterComponent> properties =
        params.getParameter().stream()
            .filter(prop -> prop.getName().equals("property"))
            .collect(Collectors.toList());
    // for first (and only) property, get the Part that contains the value
    List<ParametersParameterComponent> parts =
        properties.get(0).getPart().stream()
            .filter(part -> part.getName().equals("value"))
            .collect(Collectors.toList());
    assertFalse(((BooleanType) parts.get(0).getValue()).getValue());
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
    String sourceName = "NCI Thesaurus 21.06e";
    String sourceVersion = "21.06e";
    String endpoint =
        localHost + port + fhirCSPath + "/" + retiredId + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters =
        "?system=" + retiredUrl + "&code=" + retiredCode + "&display=" + retiredName;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
    assertEquals(
        sourceVersion, ((StringType) params.getParameter("version").getValue()).getValue());
    assertEquals(sourceName, ((StringType) params.getParameter("name").getValue()).getValue());
    // get returned properties
    List<ParametersParameterComponent> properties =
        params.getParameter().stream()
            .filter(prop -> prop.getName().equals("property"))
            .collect(Collectors.toList());
    // for first (and only) property, get the Part that contains the value
    List<ParametersParameterComponent> parts =
        properties.get(0).getPart().stream()
            .filter(part -> part.getName().equals("value"))
            .collect(Collectors.toList());
    assertFalse(((BooleanType) parts.get(0).getValue()).getValue());
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
    String messageNotFound = "Unable to find matching code system";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?code=" + code + "&system=" + url;
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
    String messageNotFound = "Unable to find matching code system";
    String activeId = "umlssemnet_2023aa";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?code=" + code + "&system=" + url;
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
    String message = "POST method not supported for " + JpaConstants.OPERATION_LOOKUP;
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
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
    String message = "POST method not supported for " + JpaConstants.OPERATION_LOOKUP;
    String activeId = "umlssemnet_2023aa";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_LOOKUP;
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
