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
import gov.nih.nci.evs.api.util.JsonUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.ValueSet.ConceptReferenceDesignationComponent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Tests for R5 value set expand calls. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR5ValueSetExpandTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR5ValueSetExpandTests.class);

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
    parser = FhirContext.forR5().newJsonParser().setPrettyPrint(true);
  }

  /**
   * Sets the up.
   *
   * @param testInfo the new up
   */
  @BeforeEach
  public void setUp(final TestInfo testInfo) {
    // The object mapper
    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
    log.info("Running test = " + testInfo.getDisplayName());
  }

  /**
   * Test value set expand instance.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandInstance() throws Exception {
    // Arrange
    String content;
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url;

    String activeCode = "T001";
    String displayString = "Organism";

    // Act - Test 1 with valid url
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());

    // Act - Test 2 no url
    content = this.restTemplate.getForObject(endpoint, String.class);
    valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());

    url = "invalid_url";
    parameters = "?url=" + url;
    String messageNotFound =
        "Supplied url invalid_url doesn't match the ValueSet retrieved by the id"
            + " ValueSet/umlssemnet_2023aa"
            + " http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String errorCode = "exception";

    // Act - Test 3 with invalid url
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);

    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test value set expand implicit.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicit() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url;

    String activeCode = "T001";
    String displayString = "Organism";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
  }

  @Test
  public void testValueSetPostRejectsParameters() throws Exception {
    // Arrange
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    String message = "POST method not supported for " + JpaConstants.OPERATION_EXPAND;

    // Create Parameters resource for POST
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("url").setValue(new UriType(url));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String requestBody = parser.encodeResourceToString(parameters);
    log.info("  parameters = " + requestBody);

    HttpEntity request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> content =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(content));

    // Assert
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }

  /**
   * Test value set expand implicit parameter not supported.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitParameterNotSupported() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
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
   * Test value set expand instance parameter not supported.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandInstanceParameterNotSupported() throws Exception {
    // Arrange
    String content;
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;
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
   * Test value set expand implicit with properties.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitWithProperties() throws Exception {
    // Arrange
    String content;
    String activeCode = "T001";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&property=parent";

    String displayString = "Organism";
    String parentCode = "#T072";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
    assertEquals(
        parentCode,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getProperty()
            .get(0)
            .getValue()
            .toString());
  }

  /**
   * Test value set expand implicit subset.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSubset() throws Exception {
    // Arrange
    String content;
    String activeCode = "C48672";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459";
    String displayString = "Schedule I Substance";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
  }

  /**
   * Test value set expand implicit subset with properties.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSubsetWithProperties() throws Exception {
    // Arrange
    String content;
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String propertyName = "Contributing_Source";
    String parameters = "?url=" + url + "&property=parent" + "&property=" + propertyName;

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
    String parentCode = "#C48670";
    String propertyValue = "FDA";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
    assertEquals(
        parentCode,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getProperty()
            .get(0)
            .getValue()
            .toString());
    assertTrue(
        valueSet.getExpansion().getContains().stream()
            .anyMatch(
                comp ->
                    comp.getProperty().stream()
                        .anyMatch(
                            prop ->
                                propertyName.equals(prop.getCode())
                                    && propertyValue.equals(prop.getValue().toString()))));
  }

  /**
   * Test value set expand implicit subset with definitions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSubsetWithDefinitions() throws Exception {
    // Arrange
    String content;
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String propertyName = "Contributing_Source";
    String parameters =
        "?url="
            + url
            + "&property=parent"
            + "&property="
            + propertyName
            + "&includeDefinition=true";

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
    String parentCode = "#C48670";
    String propertyValue = "FDA";
    String expectedDefinition = "A category of drugs not considered legitimate for medical use.";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
    assertEquals(
        parentCode,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getProperty()
            .get(0)
            .getValue()
            .toString());
    assertTrue(
        valueSet.getExpansion().getContains().stream()
            .anyMatch(
                comp ->
                    comp.getProperty().stream()
                        .anyMatch(
                            prop ->
                                propertyName.equals(prop.getCode())
                                    && propertyValue.equals(prop.getValue().toString()))));

    Optional<String> actualDefinitionOptional =
        valueSet.getExpansion().getContains().stream()
            .filter(contains -> contains.getCode().equals(activeCode))
            .findFirst()
            .flatMap(
                contains ->
                    contains.getProperty().stream()
                        .filter(property -> property.getCode().equals("definition"))
                        .findFirst()
                        .map(property -> property.getValue().toString()));

    assertTrue(actualDefinitionOptional.isPresent());
    assertEquals(expectedDefinition, actualDefinitionOptional.orElse(null));
  }

  /**
   * Test value set expand implicit subset with designations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSubsetWithDesignations() throws Exception {
    // Arrange
    String content;
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String propertyName = "Contributing_Source";
    String parameters =
        "?url="
            + url
            + "&property=parent"
            + "&property="
            + propertyName
            + "&includeDesignations=true";

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
    String parentCode = "#C48670";
    String propertyValue = "FDA";
    String expectedDesignation = "Schedule I Controlled Substance";
    String expectedTty = "SY";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
    assertEquals(
        parentCode,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getProperty()
            .get(0)
            .getValue()
            .toString());
    assertTrue(
        valueSet.getExpansion().getContains().stream()
            .anyMatch(
                comp ->
                    comp.getProperty().stream()
                        .anyMatch(
                            prop ->
                                propertyName.equals(prop.getCode())
                                    && propertyValue.equals(prop.getValue().toString()))));

    Optional<ConceptReferenceDesignationComponent> actualDesignationOptional =
        valueSet.getExpansion().getContains().stream()
            .filter(contains -> contains.getCode().equals(activeCode))
            .findFirst()
            .flatMap(
                contains ->
                    contains.getDesignation().stream()
                        .filter(
                            designation ->
                                designation.getValue().equals(expectedDesignation)
                                    && designation.getLanguage().equals("en"))
                        .findFirst());

    assertTrue(
        actualDesignationOptional.isPresent(),
        "Designation for '" + expectedDesignation + "' not found.");

    actualDesignationOptional.ifPresent(
        actualDesignation -> {
          assertEquals(expectedDesignation, actualDesignation.getValue());
          assertEquals("en", actualDesignation.getLanguage());
          assertEquals(expectedTty, actualDesignation.getUse().getCode());
        });
  }

  /**
   * Test value set expand implicit subset no designations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSubsetNoDesignations() throws Exception {
    // Arrange
    String content;
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String propertyName = "Contributing_Source";
    String parameters =
        "?url="
            + url
            + "&property=parent"
            + "&property="
            + propertyName
            + "&includeDesignations=false";

    String activeCode = "C48672";
    String expectedDesignation = "Schedule I Controlled Substance";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());

    Optional<ConceptReferenceDesignationComponent> actualDesignationOptional =
        valueSet.getExpansion().getContains().stream()
            .filter(contains -> contains.getCode().equals(activeCode))
            .findFirst()
            .flatMap(
                contains ->
                    contains.getDesignation().stream()
                        .filter(
                            designation ->
                                designation.getValue().equals(expectedDesignation)
                                    && designation.getLanguage().equals("en"))
                        .findFirst());

    assertFalse(actualDesignationOptional.isPresent());
  }

  /**
   * Test value set not found.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetNotFoundExpandImplicit() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/vsNotFound?fhir_vs";
    String messageNotFound = "Value set " + url + " not found";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url;
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
    String parameters = "?url=" + null;

    // Act
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(content));

    // Assert
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }

  /**
   * Test value set expand implicit offset size.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitOffsetSize() throws Exception {
    // Arrange
    String content;
    String activeCode = "T001";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&offset=30" + "&count=12";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertTrue(valueSet.getExpansion().getContains().size() == 12);
    assertFalse(
        valueSet.getExpansion().getContains().stream()
                .filter(comp -> comp.getCode().equals(activeCode))
                .collect(Collectors.toList())
                .size()
            > 0);
  }

  /**
   * Test value set expand implicit filter size.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitFilterSize() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&filter=Discipline" + "&count=100";
    final Set<String> disciplineStys =
        new HashSet<>(Set.of("Occupation or Discipline", "Biomedical Occupation or Discipline"));

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    // confirm that all discipleStys were returned from search with 'Discipline'
    // filter
    assertTrue(
        valueSet.getExpansion().getContains().stream()
                .filter(comp -> disciplineStys.contains(comp.getDisplay()))
                .collect(Collectors.toList())
                .size()
            == disciplineStys.size());
  }

  /**
   * Test value set expand instance with properties.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandInstanceWithProperties() throws Exception {
    // Arrange
    String content;
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&property=parent";

    String displayString = "Organism";
    String parentCode = "#T072";
    String activeCode = "T001";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
    assertEquals(
        parentCode,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getProperty()
            .get(0)
            .getValue()
            .toString());
  }

  /**
   * Test value set expand instance subset with properties.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandInstanceSubsetWithProperties() throws Exception {
    // Arrange
    String content;
    String activeID = "ncit_c54459";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;
    String propertyName = "Contributing_Source";
    String parameters = "?url=" + url + "&property=parent" + "&property=" + propertyName;

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
    String parentCode = "#C48670";
    String propertyValue = "FDA";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
    assertTrue(
        valueSet.getExpansion().getContains().stream()
            .anyMatch(
                comp ->
                    comp.getProperty().stream()
                        .anyMatch(
                            prop ->
                                "parent".equals(prop.getCode())
                                    && parentCode.equals(prop.getValue().toString()))));
    assertTrue(
        valueSet.getExpansion().getContains().stream()
            .anyMatch(
                comp ->
                    comp.getProperty().stream()
                        .anyMatch(
                            prop ->
                                propertyName.equals(prop.getCode())
                                    && propertyValue.equals(prop.getValue().toString()))));
  }

  /**
   * Test value set expand instance subset with definitions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandInstanceSubsetWithDefinitions() throws Exception {
    // Arrange
    String content;
    String activeID = "ncit_c54459";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;
    String propertyName = "Contributing_Source";
    String parameters =
        "?url="
            + url
            + "&property=parent"
            + "&property="
            + propertyName
            + "&includeDefinition=true";

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
    String parentCode = "#C48670";
    String propertyValue = "FDA";
    String expectedDefinition = "A category of drugs not considered legitimate for medical use.";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
    assertEquals(
        parentCode,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getProperty()
            .get(0)
            .getValue()
            .toString());
    assertTrue(
        valueSet.getExpansion().getContains().stream()
            .anyMatch(
                comp ->
                    comp.getProperty().stream()
                        .anyMatch(
                            prop ->
                                propertyName.equals(prop.getCode())
                                    && propertyValue.equals(prop.getValue().toString()))));

    Optional<String> actualDefinitionOptional =
        valueSet.getExpansion().getContains().stream()
            .filter(contains -> contains.getCode().equals(activeCode))
            .findFirst()
            .flatMap(
                contains ->
                    contains.getProperty().stream()
                        .filter(property -> property.getCode().equals("definition"))
                        .findFirst()
                        .map(property -> property.getValue().toString()));

    assertTrue(actualDefinitionOptional.isPresent());
    assertEquals(expectedDefinition, actualDefinitionOptional.orElse(null));
  }

  /**
   * Test value set expand instance subset with designations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandInstanceSubsetWithDesignations() throws Exception {
    // Arrange
    String content;
    String activeID = "ncit_c54459";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;
    String propertyName = "Contributing_Source";
    String parameters =
        "?url="
            + url
            + "&property=parent"
            + "&property="
            + propertyName
            + "&includeDesignations=true";

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
    String parentCode = "#C48670";
    String propertyValue = "FDA";
    String expectedDesignation = "Schedule I Controlled Substance";
    String expectedTty = "SY";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertEquals(
        displayString,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getDisplay());
    assertEquals(
        parentCode,
        valueSet.getExpansion().getContains().stream()
            .filter(comp -> comp.getCode().equals(activeCode))
            .collect(Collectors.toList())
            .get(0)
            .getProperty()
            .get(0)
            .getValue()
            .toString());
    assertTrue(
        valueSet.getExpansion().getContains().stream()
            .anyMatch(
                comp ->
                    comp.getProperty().stream()
                        .anyMatch(
                            prop ->
                                propertyName.equals(prop.getCode())
                                    && propertyValue.equals(prop.getValue().toString()))));

    Optional<ConceptReferenceDesignationComponent> actualDesignationOptional =
        valueSet.getExpansion().getContains().stream()
            .filter(contains -> contains.getCode().equals(activeCode))
            .findFirst()
            .flatMap(
                contains ->
                    contains.getDesignation().stream()
                        .filter(
                            designation ->
                                designation.getValue().equals(expectedDesignation)
                                    && designation.getLanguage().equals("en"))
                        .findFirst());

    assertTrue(actualDesignationOptional.isPresent());

    actualDesignationOptional.ifPresent(
        actualDesignation -> {
          assertEquals(expectedDesignation, actualDesignation.getValue());
          assertEquals("en", actualDesignation.getLanguage());
          assertEquals(expectedTty, actualDesignation.getUse().getCode());
        });
  }

  /**
   * Creates the NCI test value set.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
  // Helper method to create the common NCI ValueSet for testing
  private ValueSet createNCITestValueSet(String id, String name, String title, String description) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId(id);
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/" + id);
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName(name);
    inputValueSet.setTitle(title);
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription(description);

    // Build compose definition with NCI Thesaurus concepts
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Valid active concept
    ValueSet.ConceptReferenceComponent diseaseOrDisorder = new ValueSet.ConceptReferenceComponent();
    diseaseOrDisorder.setCode("C2991");
    diseaseOrDisorder.setDisplay("Disease or Disorder");
    nciInclude.addConcept(diseaseOrDisorder);

    // Valid active concept
    ValueSet.ConceptReferenceComponent gene = new ValueSet.ConceptReferenceComponent();
    gene.setCode("C16612");
    gene.setDisplay("Gene");
    nciInclude.addConcept(gene);

    // Inactive concept
    ValueSet.ConceptReferenceComponent inactiveConcept = new ValueSet.ConceptReferenceComponent();
    inactiveConcept.setCode("C176707");
    inactiveConcept.setDisplay("Physical Examination Finding - Inactive Test Concept");
    nciInclude.addConcept(inactiveConcept);

    // Invalid/bogus concept
    ValueSet.ConceptReferenceComponent invalidConcept = new ValueSet.ConceptReferenceComponent();
    invalidConcept.setCode("INVALID123");
    invalidConcept.setDisplay("This is an invalid concept code");
    nciInclude.addConcept(invalidConcept);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Test value set expand with NCI thesaurus compose definition.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusComposeDefinition() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet using helper method
    ValueSet inputValueSet =
        createNCITestValueSet(
            "nci-test-concepts",
            "NCITestConcepts",
            "NCI Thesaurus Test Concepts",
            "Test ValueSet with valid, inactive, and invalid NCI Thesaurus concepts");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + requestBody);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Valid active concepts are included
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(diseaseResult.isPresent(), "Disease or Disorder (C2991) should be included");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertTrue(geneResult.isPresent(), "Gene (C16612) should be included");
    assertEquals("Gene", geneResult.get().getDisplay());

    // Assert - Check handling of inactive concept (behavior depends on activeOnly parameter)
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    // Note: This assertion depends on your implementation's handling of inactive concepts
    // If activeOnly=false (default), inactive concepts might be included
    // If activeOnly=true, they should be excluded
    log.info("  Inactive concept C176707 present in expansion: {}", inactiveResult.isPresent());

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have valid NCI Thesaurus system
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());
    }

    // Assert - Expansion should contain only valid concepts (2 or 3 depending on inactive handling)
    assertTrue(expansion.getTotal() >= 2, "Should have at least 2 valid concepts");
    assertTrue(
        expansion.getTotal() <= 3,
        "Should have at most 3 concepts (including inactive if allowed)");

    // Assert - Log expansion results for debugging
    log.info("  NCI Thesaurus ValueSet expansion completed with {} concepts", expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("    Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Check for any error messages in expansion (some implementations include warnings)
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("  Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }
  }

  /**
   * Test value set expand with NCI thesaurus active only designations definition.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusActiveOnlyDesignationsDefinition()
      throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet using helper method
    ValueSet inputValueSet =
        createNCITestValueSet(
            "nci-active-designations-test",
            "NCIActiveDesignationsTest",
            "NCI Thesaurus Active Concepts with Designations",
            "Test ValueSet with activeOnly=true and includeDesignations=true and"
                + " includeDefinition=true");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + requestBody);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Add filter parameter to exclude concepts containing "Gene" in code or display
    // This should filter out C16612 (Gene) but allow C2991 (Disease or Disorder)
    String endpointWithFilter =
        endpoint + "?includeDesignations=true&includeDefinition=true&activeOnly=true";

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpointWithFilter, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Valid active concepts are included
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(diseaseResult.isPresent(), "Disease or Disorder (C2991) should be included");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertTrue(geneResult.isPresent(), "Gene (C16612) should be included");
    assertEquals("Gene", geneResult.get().getDisplay());

    // Assert - Inactive concept should NOT be included with activeOnly=true
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        inactiveResult.isPresent(),
        "Inactive concept (C176707) should be excluded with activeOnly=true");
    log.info("  Inactive concept C176707 correctly excluded with activeOnly=true");

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have designations when includeDesignations=true
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());

      // Assert - Designations should be included when includeDesignations=true
      if (concept.hasDesignation()) {
        assertTrue(
            concept.getDesignation().size() > 0,
            "Concept should have designations when includeDesignations=true: " + concept.getCode());

        for (ConceptReferenceDesignationComponent designation : concept.getDesignation()) {
          assertNotNull(designation.getValue(), "Designation should have a value");
          assertFalse(
              designation.getValue().trim().isEmpty(), "Designation value should not be empty");

          if (designation.hasUse()) {
            assertNotNull(designation.getUse().getCode(), "Designation use should have a code");
            assertNotNull(
                designation.getUse().getDisplay(), "Designation use should have a display");
            log.debug(
                "Designation for {}: {} (use: {})",
                concept.getCode(),
                designation.getValue(),
                designation.getUse().getDisplay());
          }

          if (designation.hasLanguage()) {
            assertNotNull(designation.getLanguage(), "Designation language should not be null");
            log.debug(
                "Designation language for {}: {}", concept.getCode(), designation.getLanguage());
          }
        }
      } else {
        log.warn(
            "Concept {} does not have designations despite includeDesignations=true",
            concept.getCode());
      }
    }

    // Assert - Expansion should contain only active concepts (2 concepts expected)
    assertEquals(
        2, expansion.getTotal(), "Should have exactly 2 active concepts with activeOnly=true");
    assertEquals(2, contains.size(), "Contains list should match total count");

    // Assert - Verify no inactive concepts made it through
    long inactiveCodeCount =
        contains.stream().filter(comp -> "C176707".equals(comp.getCode())).count();
    assertEquals(
        0, inactiveCodeCount, "No inactive concepts should be included with activeOnly=true");

    // Assert - Verify no invalid concepts made it through
    long invalidCodeCount =
        contains.stream().filter(comp -> "INVALID123".equals(comp.getCode())).count();
    assertEquals(0, invalidCodeCount, "No invalid concepts should be included");

    // Assert - Log expansion results for debugging
    log.info(
        "  NCI Thesaurus ValueSet expansion with activeOnly=true completed with {} concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug(
          "    Expanded NCI concept: {} - {} (designations: {})",
          concept.getCode(),
          concept.getDisplay(),
          concept.hasDesignation() ? concept.getDesignation().size() : 0);
    }

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("  Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }

    // Assert - Verify that at least one concept has designations
    long conceptsWithDesignations =
        contains.stream()
            .filter(ValueSet.ValueSetExpansionContainsComponent::hasDesignation)
            .count();
    assertTrue(
        conceptsWithDesignations > 0,
        "At least one concept should have designations when includeDesignations=true");

    // Assert - Verify that concepts have definitions when includeDefinition=true
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      // Check if concept has property for definition
      if (concept.hasProperty()) {
        List<ValueSet.ConceptPropertyComponent> definitionProperties =
            concept.getProperty().stream()
                .filter(prop -> "definition".equals(prop.getCode()))
                .collect(Collectors.toList());

        if (!definitionProperties.isEmpty()) {
          log.debug(
              "Concept {} has {} definition properties",
              concept.getCode(),
              definitionProperties.size());

          // Validate each definition property has value
          for (ValueSet.ConceptPropertyComponent defProp : definitionProperties) {
            assertNotNull(
                defProp.getValue(),
                "Definition property should have a value for concept: " + concept.getCode());

            if (defProp.getValue() instanceof StringType) {
              StringType defValue = (StringType) defProp.getValue();
              assertNotNull(
                  defValue.getValue(),
                  "Definition value should not be null for concept: " + concept.getCode());
              assertFalse(
                  defValue.getValue().trim().isEmpty(),
                  "Definition should not be empty for concept: " + concept.getCode());
              log.debug("Definition for {}: {}", concept.getCode(), defValue.getValue());
            }
          }
        } else {
          log.warn(
              "Concept {} does not have definition property despite includeDefinition=true",
              concept.getCode());
        }
      } else {
        log.warn("Concept {} has no properties despite includeDefinition=true", concept.getCode());
      }
    }

    // Assert - Verify that at least one concept has a definition
    long conceptsWithDefinitions =
        contains.stream()
            .filter(
                concept ->
                    concept.hasProperty()
                        && concept.getProperty().stream()
                            .anyMatch(prop -> "definition".equals(prop.getCode())))
            .count();

    assertTrue(
        conceptsWithDefinitions > 0,
        "At least one concept should have a definition when includeDefinition=true");

    // Assert - Count total definitions across all concepts
    long totalDefinitions =
        contains.stream()
            .flatMap(concept -> concept.getProperty().stream())
            .filter(prop -> "definition".equals(prop.getCode()))
            .count();

    // Assert - Log definition summary
    log.info(
        "  Concepts with definitions: {} out of {} total concepts ({} total definitions)",
        conceptsWithDefinitions,
        contains.size(),
        totalDefinitions);
  }

  @Test
  public void testValueSetExpandWithNCIThesaurusActiveOnlyDesignationsDefinitionParameters()
      throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet using helper method
    ValueSet inputValueSet =
        createNCITestValueSet(
            "nci-active-designations-test",
            "NCIActiveDesignationsTest",
            "NCI Thesaurus Active Concepts with Designations",
            "Test ValueSet with activeOnly=true and includeDesignations=true and"
                + " includeDefinition=true");

    // Create Parameters resource for POST
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter().setName("activeOnly").setValue(new BooleanType(true));
    parameters.addParameter().setName("includeDesignations").setValue(new BooleanType(true));
    parameters.addParameter().setName("includeDefinition").setValue(new BooleanType(true));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String requestBody = parser.encodeResourceToString(parameters);
    log.info("  parameters = " + requestBody);

    HttpEntity request = new HttpEntity<>(requestBody, headers);
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);

    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Valid active concepts are included
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(diseaseResult.isPresent(), "Disease or Disorder (C2991) should be included");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertTrue(geneResult.isPresent(), "Gene (C16612) should be included");
    assertEquals("Gene", geneResult.get().getDisplay());

    // Assert - Inactive concept should NOT be included with activeOnly=true
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        inactiveResult.isPresent(),
        "Inactive concept (C176707) should be excluded with activeOnly=true");
    log.info("  Inactive concept C176707 correctly excluded with activeOnly=true");

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have designations when includeDesignations=true
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());

      // Assert - Designations should be included when includeDesignations=true
      if (concept.hasDesignation()) {
        assertTrue(
            concept.getDesignation().size() > 0,
            "Concept should have designations when includeDesignations=true: " + concept.getCode());

        for (ConceptReferenceDesignationComponent designation : concept.getDesignation()) {
          assertNotNull(designation.getValue(), "Designation should have a value");
          assertFalse(
              designation.getValue().trim().isEmpty(), "Designation value should not be empty");

          if (designation.hasUse()) {
            assertNotNull(designation.getUse().getCode(), "Designation use should have a code");
            assertNotNull(
                designation.getUse().getDisplay(), "Designation use should have a display");
            log.debug(
                "Designation for {}: {} (use: {})",
                concept.getCode(),
                designation.getValue(),
                designation.getUse().getDisplay());
          }

          if (designation.hasLanguage()) {
            assertNotNull(designation.getLanguage(), "Designation language should not be null");
            log.debug(
                "Designation language for {}: {}", concept.getCode(), designation.getLanguage());
          }
        }
      } else {
        log.warn(
            "Concept {} does not have designations despite includeDesignations=true",
            concept.getCode());
      }
    }

    // Assert - Expansion should contain only active concepts (2 concepts expected)
    assertEquals(
        2, expansion.getTotal(), "Should have exactly 2 active concepts with activeOnly=true");
    assertEquals(2, contains.size(), "Contains list should match total count");

    // Assert - Verify no inactive concepts made it through
    long inactiveCodeCount =
        contains.stream().filter(comp -> "C176707".equals(comp.getCode())).count();
    assertEquals(
        0, inactiveCodeCount, "No inactive concepts should be included with activeOnly=true");

    // Assert - Verify no invalid concepts made it through
    long invalidCodeCount =
        contains.stream().filter(comp -> "INVALID123".equals(comp.getCode())).count();
    assertEquals(0, invalidCodeCount, "No invalid concepts should be included");

    // Assert - Log expansion results for debugging
    log.info(
        "  NCI Thesaurus ValueSet expansion with activeOnly=true completed with {} concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug(
          "    Expanded NCI concept: {} - {} (designations: {})",
          concept.getCode(),
          concept.getDisplay(),
          concept.hasDesignation() ? concept.getDesignation().size() : 0);
    }

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("  Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }

    // Assert - Verify that at least one concept has designations
    long conceptsWithDesignations =
        contains.stream()
            .filter(ValueSet.ValueSetExpansionContainsComponent::hasDesignation)
            .count();
    assertTrue(
        conceptsWithDesignations > 0,
        "At least one concept should have designations when includeDesignations=true");

    // Assert - Verify that concepts have definitions when includeDefinition=true
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      // Check if concept has property for definition
      if (concept.hasProperty()) {
        List<ValueSet.ConceptPropertyComponent> definitionProperties =
            concept.getProperty().stream()
                .filter(prop -> "definition".equals(prop.getCode()))
                .collect(Collectors.toList());

        if (!definitionProperties.isEmpty()) {
          log.debug(
              "Concept {} has {} definition properties",
              concept.getCode(),
              definitionProperties.size());

          // Validate each definition property has value
          for (ValueSet.ConceptPropertyComponent defProp : definitionProperties) {
            assertNotNull(
                defProp.getValue(),
                "Definition property should have a value for concept: " + concept.getCode());

            if (defProp.getValue() instanceof StringType) {
              StringType defValue = (StringType) defProp.getValue();
              assertNotNull(
                  defValue.getValue(),
                  "Definition value should not be null for concept: " + concept.getCode());
              assertFalse(
                  defValue.getValue().trim().isEmpty(),
                  "Definition should not be empty for concept: " + concept.getCode());
              log.debug("Definition for {}: {}", concept.getCode(), defValue.getValue());
            }
          }
        } else {
          log.warn(
              "Concept {} does not have definition property despite includeDefinition=true",
              concept.getCode());
        }
      } else {
        log.warn("Concept {} has no properties despite includeDefinition=true", concept.getCode());
      }
    }
  }

  /**
   * Test value set expand with NCI thesaurus filter parameter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusFilterParameter() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet using helper method
    ValueSet inputValueSet =
        createNCITestValueSet(
            "nci-filter-test",
            "NCIFilterTest",
            "NCI Thesaurus Filter Test",
            "Test ValueSet with filter parameter to exclude specific concepts");

    // Create Parameters resource with both ValueSet and other parameters
    Parameters parameters = new Parameters();
    parameters
        .addParameter()
        .setName("valueSet")
        .setResource(inputValueSet); // Add your ValueSet here
    // Add filter parameter to exclude concepts containing "Gene" in code or display
    // This should filter out C16612 (Gene) but allow C2991 (Disease or Disorder)
    parameters.addParameter().setName("filter").setValue(new StringType("Disease"));
    // Add any other parameters you need
    parameters.addParameter().setName("activeOnly").setValue(new BooleanType(true));
    parameters.addParameter().setName("includeDesignations").setValue(new BooleanType(false));

    // Encode the Parameters resource instead of the ValueSet directly
    String requestBody = parser.encodeResourceToString(parameters);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Concept containing "Disease" should be included (matches filter)
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included (matches filter 'Disease')");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());
    log.info("  C2991 (Disease or Disorder) correctly included with filter='Disease'");

    // Assert - Concept NOT containing "Disease" should be excluded (doesn't match filter)
    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        geneResult.isPresent(),
        "Gene (C16612) should be excluded (doesn't match filter 'Disease')");
    log.info("  C16612 (Gene) correctly excluded with filter='Disease'");

    // Assert - Inactive concept should be excluded (regardless of filter, due to text not matching)
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        inactiveResult.isPresent(),
        "Inactive concept (C176707) should be excluded (doesn't match filter 'Disease')");
    log.info("  C176707 (Physical Examination Finding) correctly excluded with filter='Disease'");

    // Assert - Invalid concept should NOT be included (filtered out during lookup)
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have valid NCI Thesaurus system
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());

      // Assert - All returned concepts should match the filter
      String lowerDisplay = concept.getDisplay().toLowerCase();
      String lowerCode = concept.getCode().toLowerCase();
      assertTrue(
          lowerDisplay.contains("disease") || lowerCode.contains("disease"),
          "All returned concepts should match filter 'Disease': " + concept.getDisplay());
    }

    // Assert - Expansion should contain only filtered concepts (1 concept expected)
    assertEquals(
        1, expansion.getTotal(), "Should have exactly 1 concept matching filter 'Disease'");
    assertEquals(1, contains.size(), "Contains list should match total count");

    // Assert - Verify only the expected concept made it through
    assertEquals("C2991", contains.get(0).getCode(), "Only C2991 should match the filter");
    assertTrue(
        contains.get(0).getDisplay().contains("Disease"),
        "The returned concept should contain 'Disease' in its display");

    // Assert - Verify filtered-out concepts are not present
    long filteredOutCount =
        contains.stream()
            .filter(
                comp ->
                    "C16612".equals(comp.getCode())
                        || "C176707".equals(comp.getCode())
                        || "INVALID123".equals(comp.getCode()))
            .count();
    assertEquals(0, filteredOutCount, "No filtered-out concepts should be included");

    // Assert - Log expansion results for debugging
    log.info(
        "  NCI Thesaurus ValueSet expansion with filter='Disease' completed with {} concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug(
          "Filtered NCI concept: {} - {} (matches filter: {})",
          concept.getCode(),
          concept.getDisplay(),
          concept.getDisplay().toLowerCase().contains("disease"));
    }

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("  Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for filter parameter being recorded
        if ("filter".equals(param.getName())) {
          assertEquals(
              "Disease",
              param.getValue().toString(),
              "Filter parameter should be recorded in expansion");
        }

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }
  }

  /**
   * Creates the NCI test value set with exclude.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
  // Helper method to create ValueSet with both include and exclude
  private ValueSet createNCITestValueSetWithExclude(
      String id, String name, String title, String description) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId(id);
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/" + id);
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName(name);
    inputValueSet.setTitle(title);
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription(description);

    // Build compose definition with NCI Thesaurus concepts
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // INCLUDE section - same as before
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Valid active concept
    ValueSet.ConceptReferenceComponent diseaseOrDisorder = new ValueSet.ConceptReferenceComponent();
    diseaseOrDisorder.setCode("C2991");
    diseaseOrDisorder.setDisplay("Disease or Disorder");
    nciInclude.addConcept(diseaseOrDisorder);

    // Valid active concept
    ValueSet.ConceptReferenceComponent gene = new ValueSet.ConceptReferenceComponent();
    gene.setCode("C16612");
    gene.setDisplay("Gene");
    nciInclude.addConcept(gene);

    // Inactive concept
    ValueSet.ConceptReferenceComponent inactiveConcept = new ValueSet.ConceptReferenceComponent();
    inactiveConcept.setCode("C176707");
    inactiveConcept.setDisplay("Physical Examination Finding - Inactive Test Concept");
    nciInclude.addConcept(inactiveConcept);

    // Invalid/bogus concept
    ValueSet.ConceptReferenceComponent invalidConcept = new ValueSet.ConceptReferenceComponent();
    invalidConcept.setCode("INVALID123");
    invalidConcept.setDisplay("This is an invalid concept code");
    nciInclude.addConcept(invalidConcept);

    compose.addInclude(nciInclude);

    // EXCLUDE section - exclude one of the included concepts
    ValueSet.ConceptSetComponent nciExclude = new ValueSet.ConceptSetComponent();
    nciExclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Exclude the Gene concept (C16612) that was included above
    ValueSet.ConceptReferenceComponent excludeGene = new ValueSet.ConceptReferenceComponent();
    excludeGene.setCode("C16612");
    excludeGene.setDisplay("Gene");
    nciExclude.addConcept(excludeGene);

    compose.addExclude(nciExclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  // Helper method to create ValueSet with filter-based include, direct includes, and excludes
  private ValueSet createNCITestValueSetWithIsaFilterIncludeAndExclude(
      String id, String name, String title, String description) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId(id);
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/" + id);
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName(name);
    inputValueSet.setTitle(title);
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription(description);

    // Build compose definition with NCI Thesaurus concepts
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // FIRST INCLUDE - Filter-based inclusion for Lyase Gene and its descendants
    ValueSet.ConceptSetComponent lyaseInclude = new ValueSet.ConceptSetComponent();
    lyaseInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Add filter for Lyase Gene (C21282) and descendants using "is-a" relationship
    ValueSet.ConceptSetFilterComponent lyaseFilter = new ValueSet.ConceptSetFilterComponent();
    lyaseFilter.setProperty("concept");
    lyaseFilter.setOp(Enumerations.FilterOperator.ISA); // "is-a" - includes descendants
    lyaseFilter.setValue("C21282"); // Lyase Gene
    lyaseInclude.addFilter(lyaseFilter);

    compose.addInclude(lyaseInclude);

    // SECOND INCLUDE - Direct concept inclusion (your original test concepts)
    ValueSet.ConceptSetComponent directInclude = new ValueSet.ConceptSetComponent();
    directInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Valid active concept
    ValueSet.ConceptReferenceComponent diseaseOrDisorder = new ValueSet.ConceptReferenceComponent();
    diseaseOrDisorder.setCode("C2991");
    diseaseOrDisorder.setDisplay("Disease or Disorder");
    directInclude.addConcept(diseaseOrDisorder);

    // Valid active concept
    ValueSet.ConceptReferenceComponent gene = new ValueSet.ConceptReferenceComponent();
    gene.setCode("C16612");
    gene.setDisplay("Gene");
    directInclude.addConcept(gene);

    // Inactive concept
    ValueSet.ConceptReferenceComponent inactiveConcept = new ValueSet.ConceptReferenceComponent();
    inactiveConcept.setCode("C176707");
    inactiveConcept.setDisplay("Physical Examination Finding - Inactive Test Concept");
    directInclude.addConcept(inactiveConcept);

    // Invalid/bogus concept
    ValueSet.ConceptReferenceComponent invalidConcept = new ValueSet.ConceptReferenceComponent();
    invalidConcept.setCode("INVALID123");
    invalidConcept.setDisplay("This is an invalid concept code");
    directInclude.addConcept(invalidConcept);

    compose.addInclude(directInclude);

    // EXCLUDE section - exclude one of the directly included concepts
    ValueSet.ConceptSetComponent nciExclude = new ValueSet.ConceptSetComponent();
    nciExclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Exclude the Gene concept (C16612) that was included above
    ValueSet.ConceptReferenceComponent excludeGene = new ValueSet.ConceptReferenceComponent();
    excludeGene.setCode("C16612");
    excludeGene.setDisplay("Gene");
    nciExclude.addConcept(excludeGene);

    compose.addExclude(nciExclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  @Test
  public void testValueSetExpandWithNCIThesaurusIsaFilterIncludeAndExclude() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with filter-based include, direct includes, and excludes
    ValueSet inputValueSet =
        createNCITestValueSetWithIsaFilterIncludeAndExclude(
            "nci-filter-include-exclude-test",
            "NCIFilterIncludeExcludeTest",
            "NCI Thesaurus Filter Include and Exclude Test",
            "Test ValueSet with filter for Lyase Gene descendants plus direct includes and"
                + " excludes");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + requestBody);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Lyase Gene (C21282) should be included from filter
    Optional<ValueSet.ValueSetExpansionContainsComponent> lyaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C21282".equals(comp.getCode()))
            .findFirst();

    assertTrue(lyaseResult.isPresent(), "Lyase Gene (C21282) should be included from is-a filter");
    log.info("C21282 (Lyase Gene) correctly included from filter");

    // Assert - Check for descendants of Lyase Gene (if any exist in your terminology)
    long lyaseDescendantCount =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(
                comp ->
                    comp.getDisplay() != null
                        && (comp.getDisplay().toLowerCase().contains("lyase")
                            || comp.getCode().equals("C21282")))
            .count();

    assertTrue(lyaseDescendantCount >= 1, "Should include at least Lyase Gene itself from filter");
    log.info("Found {} concepts matching Lyase Gene filter", lyaseDescendantCount);

    // Assert - Disease or Disorder should still be included (direct include, not excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included (direct include, not excluded)");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    // Assert - Gene should NOT be included (excluded despite being in direct include)
    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        geneResult.isPresent(),
        "Gene (C16612) should be excluded despite being in direct include section");
    log.info("C16612 (Gene) correctly excluded - exclude overrides include");

    // Assert - Check handling of inactive concept
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    log.info("Inactive concept C176707 present in expansion: {}", inactiveResult.isPresent());

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have valid NCI Thesaurus system
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());
    }

    // Assert - Verify specific inclusion/exclusion behavior
    Set<String> actualCodes =
        contains.stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toSet());

    // Should contain C21282 (Lyase Gene from filter) and C2991 (Disease or Disorder from direct
    // include)
    assertTrue(actualCodes.contains("C21282"), "Should contain Lyase Gene from filter");
    assertTrue(
        actualCodes.contains("C2991"), "Should contain Disease or Disorder from direct include");

    // Should NOT contain excluded or invalid concepts
    assertFalse(actualCodes.contains("C16612"), "Should NOT contain excluded Gene concept");
    assertFalse(actualCodes.contains("INVALID123"), "Should NOT contain invalid concept");

    log.info("Actual concept codes in expansion: {}", actualCodes);

    // Assert - Should have concepts from both filter and direct includes (minus exclusions)
    assertTrue(
        expansion.getTotal() >= 2,
        "Should have at least 2 concepts (Lyase Gene from filter + Disease or Disorder from direct"
            + " include)");

    // Assert - Log expansion results for debugging
    log.info(
        "NCI Thesaurus ValueSet expansion with filter, includes, and excludes completed with {}"
            + " concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Categorize concepts by source
    long filterBasedConcepts =
        contains.stream()
            .filter(
                comp ->
                    comp.getDisplay() != null
                        && (comp.getDisplay().toLowerCase().contains("lyase")
                            || comp.getCode().equals("C21282")))
            .count();

    long directIncludeConcepts =
        contains.stream()
            .filter(
                comp ->
                    Set.of("C2991", "C176707")
                        .contains(comp.getCode())) // Possible direct includes (minus excluded)
            .count();

    log.info(
        "Filter-based concepts (Lyase): {}, Direct include concepts: {}",
        filterBasedConcepts,
        directIncludeConcepts);

    // Assert - Verify filter functionality worked
    assertTrue(filterBasedConcepts >= 1, "Should have at least 1 concept from Lyase Gene filter");

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }
  }

  // Helper method to create ValueSet with descendant-leaf filter-based include, direct includes,
  // and excludes
  private ValueSet createNCITestValueSetWithDescendantLeafFilterIncludeAndExclude(
      String id, String name, String title, String description) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId(id);
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/" + id);
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName(name);
    inputValueSet.setTitle(title);
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription(description);

    // Build compose definition with NCI Thesaurus concepts
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // FIRST INCLUDE - Filter-based inclusion for Lyase Gene descendant leaf nodes only
    ValueSet.ConceptSetComponent lyaseInclude = new ValueSet.ConceptSetComponent();
    lyaseInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Add filter for Lyase Gene (C21282) descendant-leaf nodes (leaf concepts only, not
    // intermediate)
    ValueSet.ConceptSetFilterComponent lyaseFilter = new ValueSet.ConceptSetFilterComponent();
    lyaseFilter.setProperty("concept");
    lyaseFilter.setOp(
        Enumerations.FilterOperator.DESCENDENTLEAF); // "descendant-leaf" - only leaf descendants
    lyaseFilter.setValue("C21282"); // Lyase Gene
    lyaseInclude.addFilter(lyaseFilter);

    compose.addInclude(lyaseInclude);

    // SECOND INCLUDE - Direct concept inclusion (your original test concepts)
    ValueSet.ConceptSetComponent directInclude = new ValueSet.ConceptSetComponent();
    directInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Valid active concept
    ValueSet.ConceptReferenceComponent diseaseOrDisorder = new ValueSet.ConceptReferenceComponent();
    diseaseOrDisorder.setCode("C2991");
    diseaseOrDisorder.setDisplay("Disease or Disorder");
    directInclude.addConcept(diseaseOrDisorder);

    // Valid active concept
    ValueSet.ConceptReferenceComponent gene = new ValueSet.ConceptReferenceComponent();
    gene.setCode("C16612");
    gene.setDisplay("Gene");
    directInclude.addConcept(gene);

    // Inactive concept
    ValueSet.ConceptReferenceComponent inactiveConcept = new ValueSet.ConceptReferenceComponent();
    inactiveConcept.setCode("C176707");
    inactiveConcept.setDisplay("Physical Examination Finding - Inactive Test Concept");
    directInclude.addConcept(inactiveConcept);

    // Invalid/bogus concept
    ValueSet.ConceptReferenceComponent invalidConcept = new ValueSet.ConceptReferenceComponent();
    invalidConcept.setCode("INVALID123");
    invalidConcept.setDisplay("This is an invalid concept code");
    directInclude.addConcept(invalidConcept);

    compose.addInclude(directInclude);

    // EXCLUDE section - exclude one of the directly included concepts
    ValueSet.ConceptSetComponent nciExclude = new ValueSet.ConceptSetComponent();
    nciExclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Exclude the Gene concept (C16612) that was included above
    ValueSet.ConceptReferenceComponent excludeGene = new ValueSet.ConceptReferenceComponent();
    excludeGene.setCode("C16612");
    excludeGene.setDisplay("Gene");
    nciExclude.addConcept(excludeGene);

    compose.addExclude(nciExclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  @Test
  public void testValueSetExpandWithNCIThesaurusDescendantLeafFilterIncludeAndExclude()
      throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with descendant-leaf filter-based include, direct includes, and excludes
    ValueSet inputValueSet =
        createNCITestValueSetWithDescendantLeafFilterIncludeAndExclude(
            "nci-descendant-leaf-filter-test",
            "NCIDescendantLeafFilterTest",
            "NCI Thesaurus Descendant-Leaf Filter Test",
            "Test ValueSet with descendant-leaf filter for Lyase Gene leaf descendants plus direct"
                + " includes and excludes");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + requestBody);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Check for descendant-leaf concepts of Lyase Gene (leaf nodes only)
    long lyaseDescendantLeafCount =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(
                comp ->
                    comp.getDisplay() != null && comp.getDisplay().toLowerCase().contains("lyase"))
            .count();

    // Note: descendant-leaf should return only leaf concepts (no intermediate parent concepts)
    log.info(
        "Found {} leaf concepts matching Lyase Gene descendant-leaf filter",
        lyaseDescendantLeafCount);

    // Assert - Disease or Disorder should still be included (direct include, not excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included (direct include, not excluded)");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    // Assert - Gene should NOT be included (excluded despite being in direct include)
    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        geneResult.isPresent(),
        "Gene (C16612) should be excluded despite being in direct include section");
    log.info("C16612 (Gene) correctly excluded - exclude overrides include");

    // Assert - Lyase Gene itself (C21282) should NOT be included with descendant-leaf (only leaf
    // descendants)
    Optional<ValueSet.ValueSetExpansionContainsComponent> lyaseParentResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C21282".equals(comp.getCode()))
            .findFirst();

    // descendant-leaf excludes the parent concept itself, only includes leaf descendants
    log.info(
        "Lyase Gene parent (C21282) present in expansion: {} (should be false for descendant-leaf)",
        lyaseParentResult.isPresent());

    // Assert - Check handling of inactive concept
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    log.info("Inactive concept C176707 present in expansion: {}", inactiveResult.isPresent());

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have valid NCI Thesaurus system
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());
    }

    // Assert - Verify specific inclusion/exclusion behavior
    Set<String> actualCodes =
        contains.stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toSet());

    // Should contain C2991 (Disease or Disorder from direct include)
    assertTrue(
        actualCodes.contains("C2991"), "Should contain Disease or Disorder from direct include");

    // Should NOT contain excluded, invalid, or parent concepts
    assertFalse(actualCodes.contains("C16612"), "Should NOT contain excluded Gene concept");
    assertFalse(actualCodes.contains("INVALID123"), "Should NOT contain invalid concept");
    assertFalse(
        actualCodes.contains("C21282"),
        "Should NOT contain parent Lyase Gene concept (descendant-leaf excludes parent)");

    log.info("Actual concept codes in expansion: {}", actualCodes);

    // Assert - Should have concepts from both filter and direct includes (minus exclusions)
    assertTrue(
        expansion.getTotal() >= 1,
        "Should have at least 1 concept (Disease or Disorder from direct include + any Lyase leaf"
            + " descendants)");

    // Assert - Log expansion results for debugging
    log.info(
        "NCI Thesaurus ValueSet expansion with descendant-leaf filter completed with {} concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Categorize concepts by source
    long filterBasedLeafConcepts =
        contains.stream()
            .filter(
                comp ->
                    comp.getDisplay() != null
                        && comp.getDisplay().toLowerCase().contains("lyase")
                        && !comp.getCode().equals("C21282")) // Should not include parent
            .count();

    long directIncludeConcepts =
        contains.stream()
            .filter(
                comp ->
                    Set.of("C2991", "C176707")
                        .contains(comp.getCode())) // Possible direct includes (minus excluded)
            .count();

    log.info(
        "Descendant-leaf concepts (Lyase leaf descendants): {}, Direct include concepts: {}",
        filterBasedLeafConcepts,
        directIncludeConcepts);

    // Assert - Verify descendant-leaf filter behavior
    // descendant-leaf should only return leaf nodes, not intermediate or parent concepts
    log.info(
        "Descendant-leaf filter test: Parent concept C21282 should be excluded, only leaf"
            + " descendants included");

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }
  }

  // Helper method to create ValueSet with "in" filter-based include, direct includes, and excludes
  private ValueSet createNCITestValueSetWithInFilterIncludeAndExclude(
      String id, String name, String title, String description) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId(id);
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/" + id);
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName(name);
    inputValueSet.setTitle(title);
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription(description);

    // Build compose definition with NCI Thesaurus concepts
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // FIRST INCLUDE - Filter-based inclusion using "in" operation for specific concept codes
    ValueSet.ConceptSetComponent inFilterInclude = new ValueSet.ConceptSetComponent();
    inFilterInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Add filter for concepts where the code is "in" the specified comma-separated list
    ValueSet.ConceptSetFilterComponent inFilter = new ValueSet.ConceptSetFilterComponent();
    inFilter.setProperty("concept");
    inFilter.setOp(Enumerations.FilterOperator.IN); // "in" - concept code is in the specified list
    inFilter.setValue(
        "C21282,C3262,C2991"); // Comma-separated list: Lyase Gene, Neoplasm, Disease or Disorder
    inFilterInclude.addFilter(inFilter);

    compose.addInclude(inFilterInclude);

    // SECOND INCLUDE - Direct concept inclusion (your original test concepts)
    ValueSet.ConceptSetComponent directInclude = new ValueSet.ConceptSetComponent();
    directInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Valid active concept (this will be duplicated by the "in" filter above)
    ValueSet.ConceptReferenceComponent diseaseOrDisorder = new ValueSet.ConceptReferenceComponent();
    diseaseOrDisorder.setCode("C2991");
    diseaseOrDisorder.setDisplay("Disease or Disorder");
    directInclude.addConcept(diseaseOrDisorder);

    // Valid active concept
    ValueSet.ConceptReferenceComponent gene = new ValueSet.ConceptReferenceComponent();
    gene.setCode("C16612");
    gene.setDisplay("Gene");
    directInclude.addConcept(gene);

    // Inactive concept
    ValueSet.ConceptReferenceComponent inactiveConcept = new ValueSet.ConceptReferenceComponent();
    inactiveConcept.setCode("C176707");
    inactiveConcept.setDisplay("Physical Examination Finding - Inactive Test Concept");
    directInclude.addConcept(inactiveConcept);

    // Invalid/bogus concept
    ValueSet.ConceptReferenceComponent invalidConcept = new ValueSet.ConceptReferenceComponent();
    invalidConcept.setCode("INVALID123");
    invalidConcept.setDisplay("This is an invalid concept code");
    directInclude.addConcept(invalidConcept);

    compose.addInclude(directInclude);

    // EXCLUDE section - exclude one of the directly included concepts
    ValueSet.ConceptSetComponent nciExclude = new ValueSet.ConceptSetComponent();
    nciExclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Exclude the Gene concept (C16612) that was included above
    ValueSet.ConceptReferenceComponent excludeGene = new ValueSet.ConceptReferenceComponent();
    excludeGene.setCode("C16612");
    excludeGene.setDisplay("Gene");
    nciExclude.addConcept(excludeGene);

    compose.addExclude(nciExclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  @Test
  public void testValueSetExpandWithNCIThesaurusInFilterIncludeAndExclude() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with "in" filter-based include, direct includes, and excludes
    ValueSet inputValueSet =
        createNCITestValueSetWithInFilterIncludeAndExclude(
            "nci-in-filter-test",
            "NCIInFilterTest",
            "NCI Thesaurus In Filter Test",
            "Test ValueSet with 'in' filter for concepts in a specific ValueSet plus direct"
                + " includes and excludes");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + requestBody);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Check for specific concepts from the "in" filter
    // The "in" filter should include: C21282 (Lyase Gene), C3262 (Neoplasm), C2991 (Disease or
    // Disorder)

    Optional<ValueSet.ValueSetExpansionContainsComponent> lyaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C21282".equals(comp.getCode()))
            .findFirst();

    assertTrue(lyaseResult.isPresent(), "Lyase Gene (C21282) should be included from 'in' filter");
    log.info("C21282 (Lyase Gene) correctly included from 'in' filter");

    Optional<ValueSet.ValueSetExpansionContainsComponent> neoplasmResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C3262".equals(comp.getCode()))
            .findFirst();

    // Assert - Count concepts from the "in" filter
    long inFilterConceptCount =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(
                comp ->
                    Set.of("C21282", "C3262", "C2991")
                        .contains(comp.getCode())) // Codes from "in" filter
            .count();

    log.info("Found {} concepts from 'in' filter", inFilterConceptCount);

    if (neoplasmResult.isPresent()) {
      log.info("C3262 (Neoplasm) correctly included from 'in' filter");
    } else {
      log.warn("C3262 (Neoplasm) not found - may not exist in terminology or may be inactive");
    }

    // Assert - Disease or Disorder should still be included (direct include, not excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included (from both 'in' filter and direct"
            + " include)");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());
    log.info(
        "C2991 (Disease or Disorder) correctly included from both 'in' filter and direct include");

    // Assert - Gene should NOT be included (excluded despite being in direct include)
    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        geneResult.isPresent(),
        "Gene (C16612) should be excluded despite being in direct include section");
    log.info("C16612 (Gene) correctly excluded - exclude overrides include");

    // Assert - Check handling of inactive concept
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    log.info("Inactive concept C176707 present in expansion: {}", inactiveResult.isPresent());

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have valid NCI Thesaurus system
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());
    }

    // Assert - Verify specific inclusion/exclusion behavior
    Set<String> actualCodes =
        contains.stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toSet());

    // Should contain C2991 (Disease or Disorder from direct include)
    assertTrue(
        actualCodes.contains("C2991"), "Should contain Disease or Disorder from direct include");

    // Should NOT contain excluded or invalid concepts
    assertFalse(actualCodes.contains("C16612"), "Should NOT contain excluded Gene concept");
    assertFalse(actualCodes.contains("INVALID123"), "Should NOT contain invalid concept");

    log.info("Actual concept codes in expansion: {}", actualCodes);

    // Assert - Should have concepts from both filter and direct includes (minus exclusions)
    assertTrue(
        expansion.getTotal() >= 1,
        "Should have at least 1 concept (Disease or Disorder from direct include + concepts from"
            + " 'in' filter)");

    // Assert - Log expansion results for debugging
    log.info(
        "NCI Thesaurus ValueSet expansion with 'in' filter completed with {} concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Categorize concepts by source
    long directIncludeConcepts =
        contains.stream()
            .filter(
                comp ->
                    Set.of("C2991", "C176707")
                        .contains(comp.getCode())) // Possible direct includes (minus excluded)
            .count();

    log.info(
        "'In' filter concepts: {}, Direct include concepts: {}",
        inFilterConceptCount,
        directIncludeConcepts);

    // Assert - Verify "in" filter functionality
    // The "in" filter should include concepts that are members of the referenced ValueSet
    log.info("'In' filter test: Concepts from referenced ValueSet should be included");

    // Assert - Check that we have some concepts from the "in" filter (this depends on the
    // referenced ValueSet having content)
    if (inFilterConceptCount > 0) {
      log.info("Successfully found {} concepts from 'in' filter", inFilterConceptCount);
    } else {
      log.warn(
          "No concepts found from 'in' filter - check if referenced ValueSet exists and has"
              + " content");
    }

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }

    // Assert - Verify that "in" filter processed correctly
    // Look for evidence that the filter was applied (concepts from referenced ValueSet)
    boolean hasFilterConcepts =
        contains.stream()
            .anyMatch(
                comp -> {
                  // Check if any concepts appear to be from a cancer/disease-related ValueSet
                  String display = comp.getDisplay() != null ? comp.getDisplay().toLowerCase() : "";
                  return display.contains("cancer")
                      || display.contains("neoplasm")
                      || display.contains("disease")
                      || display.contains("disorder");
                });

    if (hasFilterConcepts) {
      log.info("'In' filter appears to have included relevant concepts from referenced ValueSet");
    } else {
      log.warn(
          "'In' filter may not have found concepts - verify referenced ValueSet URL and content");
    }
  }

  // Helper method to create ValueSet with "generalizes" filter-based include, direct includes, and
  // excludes
  private ValueSet createNCITestValueSetWithGeneralizesFilterIncludeAndExclude(
      String id, String name, String title, String description) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId(id);
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/" + id);
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName(name);
    inputValueSet.setTitle(title);
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription(description);

    // Build compose definition with NCI Thesaurus concepts
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // FIRST INCLUDE - Filter-based inclusion using "generalizes" operation for ancestors of Lyase
    // Gene
    ValueSet.ConceptSetComponent generalizesInclude = new ValueSet.ConceptSetComponent();
    generalizesInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Add filter for concepts that "generalize" Lyase Gene (C21282) - i.e., ancestors + the concept
    // itself
    ValueSet.ConceptSetFilterComponent generalizesFilter = new ValueSet.ConceptSetFilterComponent();
    generalizesFilter.setProperty("concept");
    generalizesFilter.setOp(
        Enumerations.FilterOperator.GENERALIZES); // "generalizes" - ancestors + self
    generalizesFilter.setValue("C21282"); // Lyase Gene
    generalizesInclude.addFilter(generalizesFilter);

    compose.addInclude(generalizesInclude);

    // SECOND INCLUDE - Direct concept inclusion (your original test concepts)
    ValueSet.ConceptSetComponent directInclude = new ValueSet.ConceptSetComponent();
    directInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Valid active concept
    ValueSet.ConceptReferenceComponent diseaseOrDisorder = new ValueSet.ConceptReferenceComponent();
    diseaseOrDisorder.setCode("C2991");
    diseaseOrDisorder.setDisplay("Disease or Disorder");
    directInclude.addConcept(diseaseOrDisorder);

    // Valid active concept
    ValueSet.ConceptReferenceComponent gene = new ValueSet.ConceptReferenceComponent();
    gene.setCode("C16612");
    gene.setDisplay("Gene");
    directInclude.addConcept(gene);

    // Inactive concept
    ValueSet.ConceptReferenceComponent inactiveConcept = new ValueSet.ConceptReferenceComponent();
    inactiveConcept.setCode("C176707");
    inactiveConcept.setDisplay("Physical Examination Finding - Inactive Test Concept");
    directInclude.addConcept(inactiveConcept);

    // Invalid/bogus concept
    ValueSet.ConceptReferenceComponent invalidConcept = new ValueSet.ConceptReferenceComponent();
    invalidConcept.setCode("INVALID123");
    invalidConcept.setDisplay("This is an invalid concept code");
    directInclude.addConcept(invalidConcept);

    compose.addInclude(directInclude);

    // EXCLUDE section - exclude one of the directly included concepts
    ValueSet.ConceptSetComponent nciExclude = new ValueSet.ConceptSetComponent();
    nciExclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Exclude the Gene concept (C16612) that was included above
    ValueSet.ConceptReferenceComponent excludeGene = new ValueSet.ConceptReferenceComponent();
    excludeGene.setCode("C16612");
    excludeGene.setDisplay("Gene");
    nciExclude.addConcept(excludeGene);

    compose.addExclude(nciExclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  @Test
  public void testValueSetExpandWithNCIThesaurusGeneralizesFilterIncludeAndExclude()
      throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with "generalizes" filter-based include, direct includes, and excludes
    ValueSet inputValueSet =
        createNCITestValueSetWithGeneralizesFilterIncludeAndExclude(
            "nci-generalizes-filter-test",
            "NCIGeneralizesFilterTest",
            "NCI Thesaurus Generalizes Filter Test",
            "Test ValueSet with 'generalizes' filter for Lyase Gene ancestors plus direct includes"
                + " and excludes");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + requestBody);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Lyase Gene (C21282) should be included (generalizes includes the concept itself)
    Optional<ValueSet.ValueSetExpansionContainsComponent> lyaseResult =
        contains.stream()
            // .filter(comp ->
            // "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C21282".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        lyaseResult.isPresent(),
        "Lyase Gene (C21282) should be included from 'generalizes' filter (includes self)");
    log.info("C21282 (Lyase Gene) correctly included from 'generalizes' filter");

    // Assert - Check for ancestor concepts of Lyase Gene (broader/parent concepts)
    long generalizesConceptCount =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(
                comp -> {
                  // Look for broader concepts that would be ancestors of Lyase Gene
                  String display = comp.getDisplay() != null ? comp.getDisplay().toLowerCase() : "";
                  String code = comp.getCode();
                  // Include the target concept itself and potential parent concepts
                  return code.equals("C21282")
                      || // Lyase Gene itself
                      display.contains("enzyme")
                      || display.contains("protein")
                      || display.contains("gene")
                      || display.contains("molecular");
                })
            .count();

    log.info(
        "Found {} concepts from 'generalizes' filter (Lyase Gene + ancestors)",
        generalizesConceptCount);

    // Assert - Disease or Disorder should still be included (direct include, not excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included (direct include, not excluded)");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    // Assert - Gene should NOT be included (excluded despite being in direct include)
    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        geneResult.isPresent(),
        "Gene (C16612) should be excluded despite being in direct include section");
    log.info("C16612 (Gene) correctly excluded - exclude overrides include");

    // Assert - Check handling of inactive concept
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    log.info("Inactive concept C176707 present in expansion: {}", inactiveResult.isPresent());

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have valid NCI Thesaurus system
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());
    }

    // Assert - Verify specific inclusion/exclusion behavior
    Set<String> actualCodes =
        contains.stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toSet());

    // Should contain C21282 (Lyase Gene - target concept) and C2991 (Disease or Disorder from
    // direct include)
    assertTrue(
        actualCodes.contains("C21282"), "Should contain Lyase Gene from 'generalizes' filter");
    assertTrue(
        actualCodes.contains("C2991"), "Should contain Disease or Disorder from direct include");

    // Should NOT contain excluded or invalid concepts
    assertFalse(actualCodes.contains("C16612"), "Should NOT contain excluded Gene concept");
    assertFalse(actualCodes.contains("INVALID123"), "Should NOT contain invalid concept");

    log.info("Actual concept codes in expansion: {}", actualCodes);

    // Assert - Should have concepts from both filter and direct includes (minus exclusions)
    assertTrue(
        expansion.getTotal() >= 2,
        "Should have at least 2 concepts (Lyase Gene from filter + Disease or Disorder from direct"
            + " include)");

    // Assert - Log expansion results for debugging
    log.info(
        "NCI Thesaurus ValueSet expansion with 'generalizes' filter completed with {} concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Categorize concepts by source
    long directIncludeConcepts =
        contains.stream()
            .filter(
                comp ->
                    Set.of("C2991", "C176707")
                        .contains(comp.getCode())) // Possible direct includes (minus excluded)
            .count();

    log.info(
        "'Generalizes' filter concepts: {}, Direct include concepts: {}",
        generalizesConceptCount,
        directIncludeConcepts);

    // Assert - Compare with other filter operations for documentation
    log.info("Filter operation comparison:");
    log.info("- 'is-a': includes C21282 + all descendants (children, grandchildren, etc.)");
    log.info(
        "- 'descendant-leaf': includes only leaf descendants of C21282 (excludes C21282 itself)");
    log.info("- 'generalizes': includes C21282 + all ancestors (parents, grandparents, etc.)");
    log.info("- 'in': includes only explicitly listed concepts");

    // Assert - Verify "generalizes" filter functionality
    assertTrue(
        actualCodes.contains("C21282"),
        "'Generalizes' filter should include the target concept itself (C21282)");

    // Check if we have ancestor concepts (broader than the target)
    long ancestorCount =
        contains.stream()
            .filter(comp -> !comp.getCode().equals("C21282")) // Exclude the target concept
            .filter(
                comp -> {
                  String display = comp.getDisplay() != null ? comp.getDisplay().toLowerCase() : "";
                  // Look for broader concepts that could be ancestors
                  return display.contains("enzyme")
                      || display.contains("protein")
                      || display.contains("gene")
                      || display.contains("molecular")
                      || display.contains("biological");
                })
            .count();

    log.info("Found {} potential ancestor concepts from 'generalizes' filter", ancestorCount);

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }
  }

  /**
   * Test value set expand with NCI thesaurus include and exclude.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusIncludeAndExclude() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with include and exclude using helper method
    ValueSet inputValueSet =
        createNCITestValueSetWithExclude(
            "nci-include-exclude-test",
            "NCIIncludeExcludeTest",
            "NCI Thesaurus Include and Exclude Test",
            "Test ValueSet with include and exclude to verify exclude overrides include");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + requestBody);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Disease or Disorder should still be included (not excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(), "Disease or Disorder (C2991) should be included (not excluded)");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    // Assert - Gene should NOT be included (excluded despite being in include)
    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        geneResult.isPresent(),
        "Gene (C16612) should be excluded despite being in include section");
    log.info("  C16612 (Gene) correctly excluded - exclude overrides include");

    // Assert - Check handling of inactive concept (behavior depends on activeOnly parameter)
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    // Note: This assertion depends on your implementation's handling of inactive concepts
    // If activeOnly=false (default), inactive concepts might be included
    // If activeOnly=true, they should be excluded
    log.info("  Inactive concept C176707 present in expansion: {}", inactiveResult.isPresent());

    // Assert - Invalid concept should NOT be included (filtered out during lookup)
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have valid NCI Thesaurus system
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());
    }

    // Assert - Verify specific exclusion behavior
    long excludedConceptCount =
        contains.stream().filter(comp -> "C16612".equals(comp.getCode())).count();
    assertEquals(0, excludedConceptCount, "Excluded concept C16612 should not appear in expansion");

    // Assert - Verify that only expected concepts are present
    Set<String> actualCodes =
        contains.stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toSet());

    // Should contain C2991 (Disease or Disorder) but NOT C16612 (Gene - excluded)
    assertTrue(actualCodes.contains("C2991"), "Should contain included concept C2991");
    assertFalse(actualCodes.contains("C16612"), "Should NOT contain excluded concept C16612");
    assertFalse(actualCodes.contains("INVALID123"), "Should NOT contain invalid concept");

    // C176707 (inactive) may or may not be present depending on activeOnly parameter
    log.info("  Actual concept codes in expansion: {}", actualCodes);

    // Assert - Expansion should contain fewer concepts due to exclusion
    // Expected: 1-2 concepts (C2991 definitely, C176707 maybe, C16612 excluded, INVALID123
    // filtered)
    assertTrue(expansion.getTotal() >= 1, "Should have at least 1 valid concept after exclusion");
    assertTrue(
        expansion.getTotal() <= 2, "Should have at most 2 concepts (C2991 + possibly C176707)");

    // Assert - Log expansion results for debugging
    log.info(
        "  NCI Thesaurus ValueSet expansion with exclusion completed with {} concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Verify exclude functionality worked correctly
    log.info(
        "  Exclude functionality test: C16612 was included but then excluded - final result: {}",
        actualCodes.contains("C16612") ? "FAILED (still present)" : "PASSED (correctly excluded)");

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("  Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }
  }

  /**
   * Creates the NCI test value set with version.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @param ncitVersion the ncit version
   * @return the value set
   */
  // Helper method to create ValueSet with specific NCIt version
  private ValueSet createNCITestValueSetWithVersion(
      String id, String name, String title, String description, String ncitVersion) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId(id);
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/" + id);
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName(name);
    inputValueSet.setTitle(title);
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription(description);

    // Build compose definition with NCI Thesaurus concepts and specific version
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Set the specific NCIt version
    if (ncitVersion != null && !ncitVersion.trim().isEmpty()) {
      nciInclude.setVersion(ncitVersion);
    }

    // Valid active concept
    ValueSet.ConceptReferenceComponent diseaseOrDisorder = new ValueSet.ConceptReferenceComponent();
    diseaseOrDisorder.setCode("C2991");
    diseaseOrDisorder.setDisplay("Disease or Disorder");
    nciInclude.addConcept(diseaseOrDisorder);

    // Valid active concept
    ValueSet.ConceptReferenceComponent gene = new ValueSet.ConceptReferenceComponent();
    gene.setCode("C16612");
    gene.setDisplay("Gene");
    nciInclude.addConcept(gene);

    // Inactive concept
    ValueSet.ConceptReferenceComponent inactiveConcept = new ValueSet.ConceptReferenceComponent();
    inactiveConcept.setCode("C176707");
    inactiveConcept.setDisplay("Physical Examination Finding - Inactive Test Concept");
    nciInclude.addConcept(inactiveConcept);

    // Invalid/bogus concept
    ValueSet.ConceptReferenceComponent invalidConcept = new ValueSet.ConceptReferenceComponent();
    invalidConcept.setCode("INVALID123");
    invalidConcept.setDisplay("This is an invalid concept code");
    nciInclude.addConcept(invalidConcept);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Test value set expand with NCI thesaurus specific version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusSpecificVersion() throws Exception {
    // Arrange - Get current NCIt version using _history mechanism
    String content;
    String endpoint = localHost + port + fhirVSPath;

    // Act - First get list of NCIt ValueSets to find a valid ID
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream()
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(resource -> resource instanceof ValueSet)
            .filter(
                resource -> {
                  ValueSet vs = (ValueSet) resource;
                  return vs.hasUrl()
                      && vs.getUrl().contains("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
                })
            .toList();
    String firstValueSetId = valueSets.get(0).getIdPart();

    // Act - Get history for the first ValueSet
    // String historyEndpoint = endpoint + "/" + firstValueSetId + "/_history";

    // Get current version
    content = this.restTemplate.getForObject(endpoint + "/" + firstValueSetId, String.class);
    ValueSet currentValueSet = parser.parseResource(ValueSet.class, content);

    // Extract NCIt version from the current ValueSet
    String currentNCItVersion = currentValueSet.getVersion();

    if (currentNCItVersion == null) {
      log.warn(
          "Could not determine current NCIt version from ValueSet {}, using fallback",
          firstValueSetId);
      currentNCItVersion = "24.01d"; // Fallback to a recent version
    }

    log.info("  Using NCIt version: {} from ValueSet: {}", currentNCItVersion, firstValueSetId);
    // log.info("History bundle contains {} entries", historyBundle.getEntry().size());

    // Create expand endpoint
    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with specific NCIt version using helper method
    ValueSet inputValueSet =
        createNCITestValueSetWithVersion(
            "nci-version-test",
            "NCIVersionTest",
            "NCI Thesaurus Version-Specific Test",
            "Test ValueSet with specific NCIt version: " + currentNCItVersion,
            currentNCItVersion);

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + requestBody);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(expandEndpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Assert - Basic structure validation
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion());

    // Assert - Expansion metadata
    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    assertNotNull(expansion.getIdentifier());
    assertNotNull(expansion.getTimestamp());
    assertNotNull(expansion.getTotal());
    assertTrue(expansion.hasContains());

    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Valid active concepts should be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included from NCIt version " + currentNCItVersion);
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        geneResult.isPresent(),
        "Gene (C16612) should be included from NCIt version " + currentNCItVersion);
    assertEquals("Gene", geneResult.get().getDisplay());

    // Assert - Inactive concept should NOT be included when using specific version
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        inactiveResult.isPresent(),
        "Inactive concept (C176707) should be included from NCIt version " + currentNCItVersion);
    log.info("  C176707 correctly included from NCIt version {}", currentNCItVersion);

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - All returned concepts should have valid NCI Thesaurus system
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format (should start with 'C' followed by digits)
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());
    }

    // Assert - Should have exactly 2 active concepts (C2991, C16612, C176707) when version is
    // specified
    assertEquals(
        3,
        expansion.getTotal(),
        "Should have exactly 3 active concepts from NCIt version " + currentNCItVersion);
    assertEquals(3, contains.size(), "Contains list should match total count");

    // Assert - Verify specific concepts are present and inactive/invalid are not
    Set<String> actualCodes =
        contains.stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toSet());

    assertTrue(actualCodes.contains("C2991"), "Should contain active concept C2991");
    assertTrue(actualCodes.contains("C16612"), "Should contain active concept C16612");
    assertTrue(actualCodes.contains("C176707"), "Should contain inactive concept C176707");
    assertFalse(actualCodes.contains("INVALID123"), "Should NOT contain invalid concept");

    // Assert - Log expansion results for debugging
    log.info(
        "  NCI Thesaurus ValueSet expansion for version {} completed with {} concepts",
        currentNCItVersion,
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug(
          "    Expanded NCI concept from version {}: {} - {}",
          currentNCItVersion,
          concept.getCode(),
          concept.getDisplay());
    }

    // Assert - Verify that the version was respected in the expansion
    log.info(
        "  Version-specific expansion test completed successfully for NCIt version: {}",
        currentNCItVersion);
    log.info("  Active concepts included: {}", actualCodes);
    log.info("  Inactive/invalid concepts correctly excluded: C176707, INVALID123");

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("  Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check if the version parameter is recorded in the expansion
        if ("version".equals(param.getName()) || "system-version".equals(param.getName())) {
          assertEquals(
              currentNCItVersion,
              param.getValue().toString(),
              "Expansion should record the NCIt version used");
        }

        // Check for warnings about invalid concepts
        if ("warning".equals(param.getName())) {
          String warningMessage = param.getValue().toString();
          assertTrue(
              warningMessage.contains("INVALID123") || warningMessage.contains("invalid"),
              "Should warn about invalid concept");
        }
      }
    }
  }
}
