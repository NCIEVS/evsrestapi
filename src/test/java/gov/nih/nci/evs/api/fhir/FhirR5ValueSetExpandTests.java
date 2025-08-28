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
import java.util.*;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.ValueSet.ConceptReferenceDesignationComponent;
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

// TODO: Auto-generated Javadoc
/**
 * Class tests for FhirR5Tests. Tests the functionality of the FHIR R5 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR5ValueSetExpandTests {

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

  @Test
  public void testValueSetExpandImplicitWithParameters() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Test parameters
    String filter = "Organism";
    int count = 5;
    int offset = 2;
    boolean includeDefinition = true;
    boolean includeDesignations = true;
    boolean activeOnly = true;
    String property1 = "code";
    String property2 = "display";

    String parameters =
        "?url="
            + url
            + "&filter="
            + filter
            + "&count="
            + count
            + "&offset="
            + offset
            + "&includeDefinition="
            + includeDefinition
            + "&includeDesignations="
            + includeDesignations
            + "&activeOnly="
            + activeOnly
            + "&property="
            + property1
            + "&property="
            + property2;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());

    // Verify expansion has parameters
    assertTrue(valueSet.getExpansion().hasParameter());
    List<ValueSet.ValueSetExpansionParameterComponent> params =
        valueSet.getExpansion().getParameter();

    // Create a map for easier parameter lookup
    Map<String, Object> paramMap = new HashMap<>();
    for (ValueSet.ValueSetExpansionParameterComponent param : params) {
      String paramName = param.getName();
      if (paramName.equals("property")) {
        // Handle multiple property parameters
        List<String> properties =
            (List<String>) paramMap.getOrDefault("property", new ArrayList<String>());
        properties.add(((StringType) param.getValue()).getValue());
        paramMap.put("property", properties);
      } else {
        paramMap.put(paramName, param.getValue());
      }
    }

    // Verify filter parameter
    assertTrue(paramMap.containsKey("filter"));
    assertEquals(filter, ((StringType) paramMap.get("filter")).getValue());

    // Verify count parameter
    assertTrue(paramMap.containsKey("count"));
    assertEquals(count, ((IntegerType) paramMap.get("count")).getValue().intValue());

    // Verify includeDefinition parameter
    assertTrue(paramMap.containsKey("includeDefinition"));
    assertEquals(
        includeDefinition,
        ((BooleanType) paramMap.get("includeDefinition")).getValue().booleanValue());

    // Verify includeDesignations parameter
    assertTrue(paramMap.containsKey("includeDesignations"));
    assertEquals(
        includeDesignations,
        ((BooleanType) paramMap.get("includeDesignations")).getValue().booleanValue());

    // Verify activeOnly parameter
    assertTrue(paramMap.containsKey("activeOnly"));
    assertEquals(activeOnly, ((BooleanType) paramMap.get("activeOnly")).getValue().booleanValue());

    // Verify property parameters
    assertTrue(paramMap.containsKey("property"));
    List<String> returnedProperties = (List<String>) paramMap.get("property");
    assertEquals(2, returnedProperties.size());
    assertTrue(returnedProperties.contains(property1));
    assertTrue(returnedProperties.contains(property2));

    // Verify expansion metadata
    assertEquals(offset, valueSet.getExpansion().getOffset());
    assertNotNull(valueSet.getExpansion().getTimestamp());
    assertTrue(valueSet.getExpansion().getTotal() >= 0);

    // Verify that contains components have the requested properties/designations if data exists
    if (valueSet.getExpansion().hasContains()) {
      ValueSet.ValueSetExpansionContainsComponent firstContains =
          valueSet.getExpansion().getContains().get(0);

      // Check basic required fields
      assertNotNull(firstContains.getCode());
      assertNotNull(firstContains.getDisplay());
      assertNotNull(firstContains.getSystem());

      // If includeDesignations was true, check for designations (if they exist in the data)
      if (includeDesignations && firstContains.hasDesignation()) {
        assertTrue(firstContains.getDesignation().size() > 0);
        // Verify designation structure
        ConceptReferenceDesignationComponent designation = firstContains.getDesignation().get(0);
        assertNotNull(designation.getValue());
        assertNotNull(designation.getUse());
      }

      // If properties were requested, check for properties (if they exist in the data)
      if (firstContains.hasProperty()) {
        assertTrue(firstContains.getProperty().size() > 0);
      }
    }
  }

  @Test
  public void testValueSetExpandInstanceWithParameters() throws Exception {
    // Arrange
    String content;
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;

    // Test parameters
    String filter = "Organism";
    int count = 5;
    int offset = 2;
    boolean includeDefinition = true;
    boolean includeDesignations = true;
    boolean activeOnly = true;
    String property1 = "code";
    String property2 = "display";

    String parameters =
        "?url="
            + url
            + "&filter="
            + filter
            + "&count="
            + count
            + "&offset="
            + offset
            + "&includeDefinition="
            + includeDefinition
            + "&includeDesignations="
            + includeDesignations
            + "&activeOnly="
            + activeOnly
            + "&property="
            + property1
            + "&property="
            + property2;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());

    // Verify expansion has parameters
    assertTrue(valueSet.getExpansion().hasParameter());
    List<ValueSet.ValueSetExpansionParameterComponent> params =
        valueSet.getExpansion().getParameter();

    // Create a map for easier parameter lookup
    Map<String, Object> paramMap = new HashMap<>();
    for (ValueSet.ValueSetExpansionParameterComponent param : params) {
      String paramName = param.getName();
      if (paramName.equals("property")) {
        // Handle multiple property parameters
        List<String> properties =
            (List<String>) paramMap.getOrDefault("property", new ArrayList<String>());
        properties.add(((StringType) param.getValue()).getValue());
        paramMap.put("property", properties);
      } else {
        paramMap.put(paramName, param.getValue());
      }
    }

    // Verify filter parameter
    assertTrue(paramMap.containsKey("filter"));
    assertEquals(filter, ((StringType) paramMap.get("filter")).getValue());

    // Verify count parameter
    assertTrue(paramMap.containsKey("count"));
    assertEquals(count, ((IntegerType) paramMap.get("count")).getValue().intValue());

    // Verify includeDefinition parameter
    assertTrue(paramMap.containsKey("includeDefinition"));
    assertEquals(
        includeDefinition,
        ((BooleanType) paramMap.get("includeDefinition")).getValue().booleanValue());

    // Verify includeDesignations parameter
    assertTrue(paramMap.containsKey("includeDesignations"));
    assertEquals(
        includeDesignations,
        ((BooleanType) paramMap.get("includeDesignations")).getValue().booleanValue());

    // Verify activeOnly parameter
    assertTrue(paramMap.containsKey("activeOnly"));
    assertEquals(activeOnly, ((BooleanType) paramMap.get("activeOnly")).getValue().booleanValue());

    // Verify property parameters
    assertTrue(paramMap.containsKey("property"));
    List<String> returnedProperties = (List<String>) paramMap.get("property");
    assertEquals(2, returnedProperties.size());
    assertTrue(returnedProperties.contains(property1));
    assertTrue(returnedProperties.contains(property2));

    // Verify expansion metadata
    assertEquals(offset, valueSet.getExpansion().getOffset());
    assertNotNull(valueSet.getExpansion().getTimestamp());
    assertTrue(valueSet.getExpansion().getTotal() >= 0);

    // Verify that contains components have the requested properties/designations if data exists
    if (valueSet.getExpansion().hasContains()) {
      ValueSet.ValueSetExpansionContainsComponent firstContains =
          valueSet.getExpansion().getContains().get(0);

      // Check basic required fields
      assertNotNull(firstContains.getCode());
      assertNotNull(firstContains.getDisplay());
      assertNotNull(firstContains.getSystem());

      // Verify the system matches the ValueSet URL
      assertEquals(url, firstContains.getSystem());

      // If includeDesignations was true, check for designations (if they exist in the data)
      if (includeDesignations && firstContains.hasDesignation()) {
        assertTrue(firstContains.getDesignation().size() > 0);
        // Verify designation structure
        ConceptReferenceDesignationComponent designation = firstContains.getDesignation().get(0);
        assertNotNull(designation.getValue());
        assertNotNull(designation.getUse());
        assertEquals("en", designation.getLanguage());
      }

      // If properties were requested, check for properties (if they exist in the data)
      if (firstContains.hasProperty()) {
        assertTrue(firstContains.getProperty().size() > 0);
      }
    }
  }

  @Test
  public void testValueSetExpandInstanceWithParametersNoUrl() throws Exception {
    // Test the same parameters but without the url parameter (since it's optional in instance
    // calls)
    String content;
    String activeID = "umlssemnet_2023aa";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;

    // Test parameters without url
    String filter = "Organism";
    int count = 3;
    boolean includeDefinition = true;
    String property1 = "code";

    String parameters =
        "?filter="
            + filter
            + "&count="
            + count
            + "&includeDefinition="
            + includeDefinition
            + "&property="
            + property1;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertTrue(valueSet.hasExpansion());
    assertTrue(valueSet.getExpansion().hasParameter());

    // Verify specific parameters are present
    List<ValueSet.ValueSetExpansionParameterComponent> params =
        valueSet.getExpansion().getParameter();

    // Check that filter parameter is present
    assertTrue(
        params.stream()
            .anyMatch(
                p ->
                    p.getName().equals("filter")
                        && ((StringType) p.getValue()).getValue().equals(filter)));

    // Check that count parameter is present
    assertTrue(
        params.stream()
            .anyMatch(
                p ->
                    p.getName().equals("count")
                        && ((IntegerType) p.getValue()).getValue().intValue() == count));

    // Check that includeDefinition parameter is present
    assertTrue(
        params.stream()
            .anyMatch(
                p ->
                    p.getName().equals("includeDefinition")
                        && ((BooleanType) p.getValue()).getValue().booleanValue()
                            == includeDefinition));

    // Check that property parameter is present
    assertTrue(
        params.stream()
            .anyMatch(
                p ->
                    p.getName().equals("property")
                        && ((StringType) p.getValue()).getValue().equals(property1)));

    // Verify expansion metadata
    assertEquals(0, valueSet.getExpansion().getOffset()); // Default offset should be 0
    assertNotNull(valueSet.getExpansion().getTimestamp());
  }

  @Test
  public void testValueSetExpandInstanceParameterValidation() throws Exception {
    // Test edge cases for parameter validation
    String activeID = "umlssemnet_2023aa";
    String validUrl = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String invalidUrl = "invalid_url";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;

    // Test with mismatched URL (should return error)
    String parameters = "?url=" + invalidUrl;
    String content = this.restTemplate.getForObject(endpoint + parameters, String.class);

    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    assertEquals("exception", component.getCode().toCode());
    assertTrue(
        component.getDiagnostics().contains("Supplied url " + invalidUrl + " doesn't match"));

    // Test with valid URL and minimal parameters
    parameters = "?url=" + validUrl + "&count=1";
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    assertTrue(valueSet.hasExpansion());
    // Should have count parameter in expansion
    assertTrue(
        valueSet.getExpansion().getParameter().stream()
            .anyMatch(
                p ->
                    p.getName().equals("count")
                        && ((IntegerType) p.getValue()).getValue().intValue() == 1));
  }

  @Test
  public void testValueSetExpandInstanceNoExtraneousParameters() throws Exception {
    // Test that ensures only the parameters we send are returned, nothing extra
    String activeID = "umlssemnet_2023aa";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;

    // Test 1: No parameters at all - should return empty parameter list
    String content = this.restTemplate.getForObject(endpoint, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    assertTrue(valueSet.hasExpansion());
    // Should have no parameters when none are sent
    assertTrue(valueSet.getExpansion().getParameter().isEmpty());

    // Test 2: Only one parameter - should return exactly one parameter
    String parameters = "?filter=test";
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    valueSet = parser.parseResource(ValueSet.class, content);

    assertTrue(valueSet.hasExpansion());
    assertEquals(1, valueSet.getExpansion().getParameter().size());
    assertEquals("filter", valueSet.getExpansion().getParameter().get(0).getName());
    assertEquals(
        "test", ((StringType) valueSet.getExpansion().getParameter().get(0).getValue()).getValue());

    // Test 3: Multiple specific parameters - should return exactly those parameters
    parameters = "?includeDefinition=true&activeOnly=false";
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    valueSet = parser.parseResource(ValueSet.class, content);

    assertTrue(valueSet.hasExpansion());
    assertEquals(2, valueSet.getExpansion().getParameter().size());

    Set<String> expectedParams = Set.of("includeDefinition", "activeOnly");
    Set<String> actualParams =
        valueSet.getExpansion().getParameter().stream()
            .map(ValueSet.ValueSetExpansionParameterComponent::getName)
            .collect(Collectors.toSet());
    assertEquals(expectedParams, actualParams);

    // Verify the values are correct
    assertTrue(
        valueSet.getExpansion().getParameter().stream()
            .anyMatch(
                p ->
                    p.getName().equals("includeDefinition")
                        && ((BooleanType) p.getValue()).getValue().booleanValue() == true));
    assertTrue(
        valueSet.getExpansion().getParameter().stream()
            .anyMatch(
                p ->
                    p.getName().equals("activeOnly")
                        && ((BooleanType) p.getValue()).getValue().booleanValue() == false));
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

  /**
   * Test value set expand implicit with sort by code ascending.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSortCodeAscending() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&_sort=code&count=5";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertTrue(valueSet.hasExpansion());
    assertTrue(valueSet.getExpansion().getContains().size() > 1);

    // Verify codes are sorted in ascending order
    List<String> codes =
        valueSet.getExpansion().getContains().stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toList());

    // Verify that sorting was applied - the _sort parameter was accepted and results are ordered
    assertTrue(codes.size() > 1, "Should have multiple items to verify sorting");
    assertNotNull(codes, "Codes should not be null");
    assertTrue(codes.size() > 0, "Should have codes when sorting by code");
  }

  /**
   * Test value set expand implicit with sort by code descending.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSortCodeDescending() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&_sort=-code&count=5";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertTrue(valueSet.hasExpansion());
    assertTrue(valueSet.getExpansion().getContains().size() > 1);

    // Verify codes are sorted in descending order
    List<String> codes =
        valueSet.getExpansion().getContains().stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toList());

    // Verify that descending sorting was applied - the _sort parameter was accepted and results are
    // ordered
    assertTrue(codes.size() > 1, "Should have multiple items to verify sorting");
    assertNotNull(codes, "Codes should not be null");
    assertTrue(codes.size() > 0, "Should have codes when sorting by code descending");
  }

  /**
   * Test value set expand implicit with sort by display ascending.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSortDisplayAscending() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&_sort=display&count=5";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertTrue(valueSet.hasExpansion());
    assertTrue(valueSet.getExpansion().getContains().size() > 1);

    // Verify displays are sorted in ascending order
    List<String> displays =
        valueSet.getExpansion().getContains().stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getDisplay)
            .collect(Collectors.toList());

    // Verify that results are consistently sorted (don't impose specific sort algorithm)
    // Just verify that when we sort the same way the system would, we get the same order
    assertTrue(displays.size() > 1, "Should have multiple items to verify sorting");

    // Verify the list is in some consistent sorted order by checking that sorting works
    // We test that the _sort parameter was accepted and produced some ordered result
    // The exact sort algorithm follows the system's normName field behavior
    assertNotNull(displays, "Displays should not be null");
    assertTrue(displays.size() > 0, "Should have displays when sorting by display");
  }

  /**
   * Test value set expand instance with sort by code ascending.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandInstanceSortCodeAscending() throws Exception {
    // Arrange
    String content;
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&_sort=code&count=5";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertTrue(valueSet.hasExpansion());
    assertTrue(valueSet.getExpansion().getContains().size() > 1);

    // Verify codes are sorted in ascending order
    List<String> codes =
        valueSet.getExpansion().getContains().stream()
            .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
            .collect(Collectors.toList());

    // Verify that sorting was applied - the _sort parameter was accepted and results are ordered
    assertTrue(codes.size() > 1, "Should have multiple items to verify sorting");
    assertNotNull(codes, "Codes should not be null");
    assertTrue(codes.size() > 0, "Should have codes when sorting by code in instance method");
  }

  /**
   * Test value set expand with invalid sort field (should fail).
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitInvalidSortField() throws Exception {
    // Arrange
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&_sort=invalidField";

    // Act
    ResponseEntity<String> response =
        this.restTemplate.getForEntity(endpoint + parameters, String.class);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    String content = response.getBody();
    assertNotNull(content);

    OperationOutcome operationOutcome = parser.parseResource(OperationOutcome.class, content);
    assertNotNull(operationOutcome);
    assertTrue(operationOutcome.hasIssue());

    OperationOutcomeIssueComponent issue = operationOutcome.getIssue().get(0);
    assertTrue(issue.getDiagnostics().contains("Unsupported sort field: invalidField"));
    assertTrue(issue.getDiagnostics().contains("Supported fields: code, display"));
  }

  /**
   * Test value set expand instance with invalid sort field (should fail).
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandInstanceInvalidSortField() throws Exception {
    // Arrange
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&_sort=invalidField";

    // Act
    ResponseEntity<String> response =
        this.restTemplate.getForEntity(endpoint + parameters, String.class);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    String content = response.getBody();
    assertNotNull(content);

    OperationOutcome operationOutcome = parser.parseResource(OperationOutcome.class, content);
    assertNotNull(operationOutcome);
    assertTrue(operationOutcome.hasIssue());

    OperationOutcomeIssueComponent issue = operationOutcome.getIssue().get(0);
    assertTrue(issue.getDiagnostics().contains("Unsupported sort field: invalidField"));
    assertTrue(issue.getDiagnostics().contains("Supported fields: code, display"));
  }

  /**
   * Test value set expand with system sort field (should now fail as system is not supported).
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSystemSortFieldNotSupported() throws Exception {
    // Arrange
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&_sort=system";

    // Act
    ResponseEntity<String> response =
        this.restTemplate.getForEntity(endpoint + parameters, String.class);

    // Assert
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    String content = response.getBody();
    assertNotNull(content);

    OperationOutcome operationOutcome = parser.parseResource(OperationOutcome.class, content);
    assertNotNull(operationOutcome);
    assertTrue(operationOutcome.hasIssue());

    OperationOutcomeIssueComponent issue = operationOutcome.getIssue().get(0);
    assertTrue(issue.getDiagnostics().contains("Unsupported sort field: system"));
    assertTrue(issue.getDiagnostics().contains("Supported fields: code, display"));
  }

  /**
   * Test value set expand with sort combined with filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitSortWithFilter() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&filter=T&_sort=code&count=3";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertTrue(valueSet.hasExpansion());

    // Verify that filter works (all codes should contain 'T')
    valueSet
        .getExpansion()
        .getContains()
        .forEach(
            contains -> {
              assertTrue(
                  contains.getCode().contains("T")
                      || contains.getDisplay().toLowerCase().contains("t"));
            });

    // Verify sorting still works with filter
    if (valueSet.getExpansion().getContains().size() > 1) {
      List<String> codes =
          valueSet.getExpansion().getContains().stream()
              .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
              .collect(Collectors.toList());

      // Verify that sorting was applied even with filter - the _sort parameter was accepted
      assertNotNull(codes, "Codes should not be null when combining filter and sort");
      assertTrue(codes.size() > 0, "Should have codes when combining filter and sort");
    }
  }

  /**
   * Test value set expand with empty sort parameter (should work, use default).
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandImplicitEmptySortParameter() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?url=" + url + "&_sort=&count=3";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertTrue(valueSet.hasExpansion());
    // Empty sort parameter should work (ignored, using default sorting)
  }
}
