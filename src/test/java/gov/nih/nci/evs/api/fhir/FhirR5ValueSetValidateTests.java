package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.TestProperties;
import org.hl7.fhir.r5.model.BooleanType;
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

/**
 * Class tests for FhirR5Tests. Tests the functionality of the FHIR R5 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR5ValueSetValidateTests {

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

  /** The fhir VS path. */
  private final String fhirVSPath = "/fhir/r5/ValueSet";

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
   * Test value set validate active code.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetValidateActiveCode() throws Exception {
    // Arrange
    String content;
    String activeCode = "T100";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String displayString = "Age Group";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
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
    String activeCode = "T100";
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String displayString = "Age Group";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode + "&display=" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
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
    String activeCode = "T100";
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String displayString = "INCORRECT";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode + "&display=" + displayString;
    String message =
        "The code '"
            + activeCode
            + "' was found in this value set, however the display '"
            + displayString
            + "' did not match any designations.";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String activeCode = "T100";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String displayString = "Age Group";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode + "&display=" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String messageNotFound = "The code '" + codeNotFound + "' was not found.";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
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
   * Test value set code not found for display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetCodeNotFoundAndDisplayString() throws Exception {
    // Arrange
    String content;
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String displayString = "Age Group";
    String messageNotFound = "The code '" + codeNotFound + "' was not found.";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + codeNotFound + "&display=" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String activeID = "umlssemnet_2023aa";
    String codeNotFound = "10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String displayString = "Age Group";
    String messageNotFound = "The code '" + codeNotFound + "' was not found.";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + codeNotFound + "&display=" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    String retiredName = "ABCB1 1 Allele";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + retiredUrl + "&code=" + retiredCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    String retiredName = "ABCB1 1 Allele";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + retiredUrl + "&code=" + retiredCode + "&display=" + retiredName;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

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
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    String retiredId = "ncit_21.06e";
    String retiredName = "ABCB1 1 Allele";
    String endpoint =
        localHost
            + port
            + fhirVSPath
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
    String message = "POST method not supported for " + JpaConstants.OPERATION_EXPAND;

    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
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
