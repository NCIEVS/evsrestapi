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
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceDesignationComponent;
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
 * Class tests for FhirR4Tests. Tests the functionality of the FHIR R4 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR4ValueSetExpandTests {

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
  private final String fhirVSPath = "/fhir/r4/ValueSet";

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
   * Test value set expand instance.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandInstance() throws Exception {
    // Arrange
    String content;
    String activeCode = "T001";
    String activeID = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String displayString = "Organism";
    String endpoint =
        localHost + port + fhirVSPath + "/" + activeID + "/" + JpaConstants.OPERATION_EXPAND;
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
    OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    String activeCode = "T001";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String displayString = "Organism";
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

  @Test
  public void testValueSetExpandImplicitWithParameters() throws Exception {
    // Arrange
    String content;
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Test parameters (R4 supports fewer parameters than R5)
    String filter = "Organism";
    int count = 5;
    int offset = 2;
    boolean includeDesignations = true;
    boolean activeOnly = true;

    String parameters =
        "?url="
            + url
            + "&filter="
            + filter
            + "&count="
            + count
            + "&offset="
            + offset
            + "&includeDesignations="
            + includeDesignations
            + "&activeOnly="
            + activeOnly;

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
      paramMap.put(param.getName(), param.getValue());
    }

    // Verify filter parameter
    assertTrue(paramMap.containsKey("filter"));
    assertEquals(filter, ((StringType) paramMap.get("filter")).getValue());

    // Verify count parameter
    assertTrue(paramMap.containsKey("count"));
    assertEquals(count, ((IntegerType) paramMap.get("count")).getValue().intValue());

    // Verify includeDesignations parameter
    assertTrue(paramMap.containsKey("includeDesignations"));
    assertEquals(
        includeDesignations,
        ((BooleanType) paramMap.get("includeDesignations")).getValue().booleanValue());

    // Verify activeOnly parameter
    assertTrue(paramMap.containsKey("activeOnly"));
    assertEquals(activeOnly, ((BooleanType) paramMap.get("activeOnly")).getValue().booleanValue());

    // Verify no extraneous parameters are present - only the ones we sent should be returned
    Set<String> expectedParamNames =
        Set.of("filter", "count", "includeDesignations", "activeOnly", "offset");
    Set<String> actualParamNames = paramMap.keySet();
    assertEquals(expectedParamNames, actualParamNames);

    // Verify expansion metadata
    assertEquals(offset, valueSet.getExpansion().getOffset());
    assertNotNull(valueSet.getExpansion().getTimestamp());
    assertTrue(valueSet.getExpansion().getTotal() >= 0);

    // Verify that contains components have the requested designations if data exists
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
        assertEquals("en", designation.getLanguage());
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

    // Test parameters for R4 instance method
    String filter = "Organism";
    int count = 5;
    int offset = 2; // Note: R4 instance method uses @Offset/@Count annotations
    boolean includeDesignations = true;
    boolean activeOnly = true;

    String parameters =
        "?url="
            + url
            + "&filter="
            + filter
            + "&count="
            + count
            + "&offset="
            + offset
            + "&includeDesignations="
            + includeDesignations
            + "&activeOnly="
            + activeOnly;

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
      paramMap.put(param.getName(), param.getValue());
    }

    // Verify filter parameter
    assertTrue(paramMap.containsKey("filter"));
    assertEquals(filter, ((StringType) paramMap.get("filter")).getValue());

    assertTrue(paramMap.containsKey("count"));
    assertEquals(count, ((IntegerType) paramMap.get("count")).getValue().intValue());

    // Verify includeDesignations parameter
    assertTrue(paramMap.containsKey("includeDesignations"));
    assertEquals(
        includeDesignations,
        ((BooleanType) paramMap.get("includeDesignations")).getValue().booleanValue());

    // Verify activeOnly parameter
    assertTrue(paramMap.containsKey("activeOnly"));
    assertEquals(activeOnly, ((BooleanType) paramMap.get("activeOnly")).getValue().booleanValue());

    // Verify no extraneous parameters are present
    Set<String> expectedParamNames =
        Set.of("filter", "count", "includeDesignations", "activeOnly", "offset");
    Set<String> actualParamNames = paramMap.keySet();
    assertEquals(expectedParamNames, actualParamNames);

    // Verify expansion metadata
    assertEquals(offset, valueSet.getExpansion().getOffset());
    assertNotNull(valueSet.getExpansion().getTimestamp());
    assertTrue(valueSet.getExpansion().getTotal() >= 0);

    // Verify that contains components have the requested designations if data exists
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
    boolean includeDesignations = true;

    String parameters =
        "?filter=" + filter + "&count=" + count + "&includeDesignations=" + includeDesignations;

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

    // Check that includeDesignations parameter is present
    assertTrue(
        params.stream()
            .anyMatch(
                p ->
                    p.getName().equals("includeDesignations")
                        && ((BooleanType) p.getValue()).getValue().booleanValue()
                            == includeDesignations));

    // Verify no extraneous parameters - only the ones we sent should be returned
    Set<String> expectedParamNames = Set.of("filter", "count", "includeDesignations");
    Set<String> actualParamNames =
        params.stream()
            .map(ValueSet.ValueSetExpansionParameterComponent::getName)
            .collect(Collectors.toSet());
    assertEquals(expectedParamNames, actualParamNames);

    // Verify expansion metadata
    assertEquals(0, valueSet.getExpansion().getOffset()); // Default offset should be 0
    assertNotNull(valueSet.getExpansion().getTimestamp());
  }

  @Test
  public void testValueSetExpandImplicitNoExtraneousParameters() throws Exception {
    // Test that ensures only the parameters we send are returned, nothing extra
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs";
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Test 1: Only required url parameter - should return empty parameter list
    String parameters = "?url=" + url;
    String content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    assertTrue(valueSet.hasExpansion());
    // Should have no parameters when only required url is sent
    assertTrue(valueSet.getExpansion().getParameter().isEmpty());

    // Test 2: Only one optional parameter - should return exactly one parameter
    parameters = "?url=" + url + "&filter=test";
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    valueSet = parser.parseResource(ValueSet.class, content);

    assertTrue(valueSet.hasExpansion());
    assertEquals(1, valueSet.getExpansion().getParameter().size());
    assertEquals("filter", valueSet.getExpansion().getParameter().get(0).getName());
    assertEquals(
        "test", ((StringType) valueSet.getExpansion().getParameter().get(0).getValue()).getValue());

    // Test 3: Multiple specific parameters - should return exactly those parameters
    parameters = "?url=" + url + "&includeDesignations=true&activeOnly=false";
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    valueSet = parser.parseResource(ValueSet.class, content);

    assertTrue(valueSet.hasExpansion());
    assertEquals(2, valueSet.getExpansion().getParameter().size());

    Set<String> expectedParams = Set.of("includeDesignations", "activeOnly");
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
                    p.getName().equals("includeDesignations")
                        && ((BooleanType) p.getValue()).getValue().booleanValue() == true));
    assertTrue(
        valueSet.getExpansion().getParameter().stream()
            .anyMatch(
                p ->
                    p.getName().equals("activeOnly")
                        && ((BooleanType) p.getValue()).getValue().booleanValue() == false));
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
    parameters = "?includeDesignations=true&activeOnly=false";
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    valueSet = parser.parseResource(ValueSet.class, content);

    assertTrue(valueSet.hasExpansion());
    assertEquals(2, valueSet.getExpansion().getParameter().size());

    Set<String> expectedParams = Set.of("includeDesignations", "activeOnly");
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
                    p.getName().equals("includeDesignations")
                        && ((BooleanType) p.getValue()).getValue().booleanValue() == true));
    assertTrue(
        valueSet.getExpansion().getParameter().stream()
            .anyMatch(
                p ->
                    p.getName().equals("activeOnly")
                        && ((BooleanType) p.getValue()).getValue().booleanValue() == false));
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
    OperationOutcome.OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    assertEquals("exception", component.getCode().toCode());
    assertTrue(
        component.getDiagnostics().contains("Supplied url " + invalidUrl + " doesn't match"));

    parameters = "?url=" + validUrl + "&count=1";
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    assertTrue(valueSet.hasExpansion());
    assertTrue(
        valueSet.getExpansion().getParameter().stream()
            .anyMatch(
                p ->
                    p.getName().equals("count")
                        && ((IntegerType) p.getValue()).getValue().intValue() == 1));

    // Verify no extraneous parameters - only count should be returned since that's all we sent
    Set<String> expectedParamNames = Set.of("count");
    Set<String> actualParamNames =
        valueSet.getExpansion().getParameter().stream()
            .map(ValueSet.ValueSetExpansionParameterComponent::getName)
            .collect(Collectors.toSet());
    assertEquals(expectedParamNames, actualParamNames);
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
    // confirm that all discipleStys were returned from search with 'Discipline' filter
    assertTrue(
        valueSet.getExpansion().getContains().stream()
                .filter(comp -> disciplineStys.contains(comp.getDisplay()))
                .collect(Collectors.toList())
                .size()
            == disciplineStys.size());
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
    String parameters = "?url=" + url + "&includeDesignations=true";

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
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
    String parameters = "?url=" + url + "&includeDesignations=true";

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
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
}
