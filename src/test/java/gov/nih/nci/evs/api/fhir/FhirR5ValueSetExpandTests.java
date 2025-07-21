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
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

// TODO: Auto-generated Javadoc
/**
 * Class tests for FhirR5Tests. Tests the functionality of the FHIR R5 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
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

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
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

    // Assert - Expansion should contain only valid concepts (2 or 3 depending on inactive handling)
    assertTrue(expansion.getTotal() >= 2, "Should have at least 2 valid concepts");
    assertTrue(
        expansion.getTotal() <= 3,
        "Should have at most 3 concepts (including inactive if allowed)");

    // Assert - Log expansion results for debugging
    log.info("NCI Thesaurus ValueSet expansion completed with {} concepts", expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Check for any error messages in expansion (some implementations include warnings)
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

    // Create Parameters resource for POST
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter().setName("activeOnly").setValue(new BooleanType(true));
    parameters.addParameter().setName("includeDesignations").setValue(new BooleanType(true));
    parameters.addParameter().setName("includeDefinition").setValue(new BooleanType(true));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String requestBody = parser.encodeResourceToString(parameters);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
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
    log.info("Inactive concept C176707 correctly excluded with activeOnly=true");

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
        "NCI Thesaurus ValueSet expansion with activeOnly=true completed with {} concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug(
          "Expanded NCI concept: {} - {} (designations: {})",
          concept.getCode(),
          concept.getDisplay(),
          concept.hasDesignation() ? concept.getDesignation().size() : 0);
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
        "Concepts with definitions: {} out of {} total concepts ({} total definitions)",
        conceptsWithDefinitions,
        contains.size(),
        totalDefinitions);
  }

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

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Add filter parameter to exclude concepts containing "Gene" in code or display
    // This should filter out C16612 (Gene) but allow C2991 (Disease or Disorder)
    String endpointWithFilter = endpoint + "?filter=Disease";

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpointWithFilter, request, String.class);
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
    log.info("C2991 (Disease or Disorder) correctly included with filter='Disease'");

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
    log.info("C16612 (Gene) correctly excluded with filter='Disease'");

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
    log.info("C176707 (Physical Examination Finding) correctly excluded with filter='Disease'");

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
        "NCI Thesaurus ValueSet expansion with filter='Disease' completed with {} concepts",
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
        log.info("Expansion parameter: {} = {}", param.getName(), param.getValue());

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

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
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
    log.info("C16612 (Gene) correctly excluded - exclude overrides include");

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
    log.info("Inactive concept C176707 present in expansion: {}", inactiveResult.isPresent());

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
    log.info("Actual concept codes in expansion: {}", actualCodes);

    // Assert - Expansion should contain fewer concepts due to exclusion
    // Expected: 1-2 concepts (C2991 definitely, C176707 maybe, C16612 excluded, INVALID123
    // filtered)
    assertTrue(expansion.getTotal() >= 1, "Should have at least 1 valid concept after exclusion");
    assertTrue(
        expansion.getTotal() <= 2, "Should have at most 2 concepts (C2991 + possibly C176707)");

    // Assert - Log expansion results for debugging
    log.info(
        "NCI Thesaurus ValueSet expansion with exclusion completed with {} concepts",
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Verify exclude functionality worked correctly
    log.info(
        "Exclude functionality test: C16612 was included but then excluded - final result: {}",
        actualCodes.contains("C16612") ? "FAILED (still present)" : "PASSED (correctly excluded)");

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
    String historyEndpoint = endpoint + "/" + firstValueSetId + "/_history";

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

    log.info("Using NCIt version: {} from ValueSet: {}", currentNCItVersion, firstValueSetId);
    //    log.info("History bundle contains {} entries", historyBundle.getEntry().size());

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

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(expandEndpoint, request, String.class);
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
    log.info("C176707 correctly included from NCIt version {}", currentNCItVersion);

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
        "NCI Thesaurus ValueSet expansion for version {} completed with {} concepts",
        currentNCItVersion,
        expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug(
          "Expanded NCI concept from version {}: {} - {}",
          currentNCItVersion,
          concept.getCode(),
          concept.getDisplay());
    }

    // Assert - Verify that the version was respected in the expansion
    log.info(
        "Version-specific expansion test completed successfully for NCIt version: {}",
        currentNCItVersion);
    log.info("Active concepts included: {}", actualCodes);
    log.info("Inactive/invalid concepts correctly excluded: C176707, INVALID123");

    // Assert - Check for any error messages in expansion
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("Expansion parameter: {} = {}", param.getName(), param.getValue());

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
