package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.CodeType;
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
  public void setUp() {}

  /**
   * Test code system lookup code.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupCode() throws Exception {
    // Arrange
    String content;
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String name = "UMLS Semantic Network 2023AA";
    final String version = "2023AA";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    final String parameters = "?system=" + url + "&code=" + activeCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

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
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String name = "UMLS Semantic Network 2023AA";
    final String version = "2023AA";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;

    // Create the Coding object
    final Coding coding = new Coding(url, activeCode, null);

    // Construct the GET request URI with the coding parameter
    final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("coding", coding.getSystem() + "|" + coding.getCode());

    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(name, ((StringType) params.getParameter("name").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertEquals(version, ((StringType) params.getParameter("version").getValue()).getValue());
  }

  @Test
  public void testCodeSystemLookupInstanceCodeWithCoding() throws Exception {
    // Arrange
    final String activeCode = "T100";
    final String activeId = "umlssemnet_2023aa";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String name = "UMLS Semantic Network 2023AA";
    final String version = "2023AA";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_LOOKUP;

    // Create the Coding object
    final Coding coding = new Coding(url, activeCode, null);

    // Construct the GET request URI with the coding parameter
    final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
    builder.queryParam("coding", coding.getSystem() + "|" + coding.getCode());

    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(name, ((StringType) params.getParameter("name").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertEquals(version, ((StringType) params.getParameter("version").getValue()).getValue());
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
    final String activeCode = "T100";
    final String activeId = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String name = "UMLS Semantic Network 2023AA";
    final String version = "2023AA";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_LOOKUP;
    String parameters = "?system=" + url + "&code=" + activeCode + "&display" + displayString;

    // Act - Test 1 system/url matches that of id
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

    parameters = "?code=" + activeCode + "&display" + displayString;

    // Act - Test 2 no url
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    // confirm code is active
    assertTrue(
        ((BooleanType) params.getParameter("property").getPart().get(1).getValue()).getValue());
    assertEquals(name, ((StringType) params.getParameter("name").getValue()).getValue());
    assertEquals(version, ((StringType) params.getParameter("version").getValue()).getValue());

    url = "invalid_url";
    parameters = "?system=" + url + "&code=" + activeCode + "&display" + displayString;
    final String messageNotFound =
        "Supplied url or system UriType[invalid_url] doesn't match the CodeSystem retrieved by the"
            + " id CodeSystem/umlssemnet_2023aa"
            + " http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String errorCode = "exception";

    // Act - Test 3 with invalid url
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);

    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
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
    final String codeNotFound = "T10";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String messageNotFound = "Failed to lookup code";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    final String parameters = "?system=" + url + "&code=" + codeNotFound;
    final String errorCode = "exception";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    final String activeId = "umlssemnet_2023aa";
    final String codeNotFound = "T10";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String displayString = "Age Group";
    final String messageNotFound = "Failed to lookup code";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_LOOKUP;
    final String parameters =
        "?system=" + url + "&code=" + codeNotFound + "$display" + displayString;
    final String errorCode = "exception";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    final String retiredCode = "C45683";
    final String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    final String parameters = "?system=" + retiredUrl + "&code=" + retiredCode;

    final Set<String> sourceVersions = new HashSet<>(Set.of("25.06e", "25.07b"));
    final String retiredName = "ABCB1 1 Allele";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
    assertThat(sourceVersions)
        .contains(((StringType) params.getParameter("version").getValue()).getValue());

    // get returned properties
    final List<ParametersParameterComponent> properties =
        params.getParameter().stream()
            .filter(prop -> prop.getName().equals("property"))
            .collect(Collectors.toList());
    // for first (and only) property, get the Part that contains the value
    final List<ParametersParameterComponent> parts =
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
    final String retiredCode = "C45683";
    final String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    final String retiredId = "ncit_25.06e";
    final String retiredName = "ABCB1 1 Allele";
    final String sourceName = "NCI Thesaurus 25.06e";
    final String sourceVersion = "25.06e";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + retiredId + "/" + JpaConstants.OPERATION_LOOKUP;
    final String parameters =
        "?system=" + retiredUrl + "&code=" + retiredCode + "&display=" + retiredName;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
    assertEquals(
        sourceVersion, ((StringType) params.getParameter("version").getValue()).getValue());
    assertEquals(sourceName, ((StringType) params.getParameter("name").getValue()).getValue());
    // get returned properties
    final List<ParametersParameterComponent> properties =
        params.getParameter().stream()
            .filter(prop -> prop.getName().equals("property"))
            .collect(Collectors.toList());
    // for first (and only) property, get the Part that contains the value
    final List<ParametersParameterComponent> parts =
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
    final String code = "C3224";
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/TheBadTest.owl";
    final String messageNotFound = "Unable to find matching code system";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    final String parameters = "?code=" + code + "&system=" + url;
    final String errorCode = "not-found";

    // Act
    content = restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

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
    final String message = "POST method not supported for " + JpaConstants.OPERATION_LOOKUP;
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
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
   * Test the CodeSystem rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemPostRejectsInstance() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    final String message = "POST method not supported for " + JpaConstants.OPERATION_LOOKUP;
    final String activeId = "umlssemnet_2023aa";
    final String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_LOOKUP;
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
   * Test code system lookup implicit code with both code and coding.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupImplicitCodeWithBothCodeAndCoding() throws Exception {
    // Arrange
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final Coding coding = new Coding(url, activeCode, null);

    final String messageNotSupported = "Must use one of 'code' or 'coding' parameters";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP);
    builder.queryParam("code", activeCode);
    builder.queryParam("system", url);
    builder.queryParam("coding", coding.getSystem() + "|" + coding.getCode());
    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }

  /**
   * Test code system lookup implicit code with no system.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupImplicitCodeWithNoSystem() throws Exception {
    // Arrange
    final String activeCode = "T100";

    final String messageNotSupported =
        "Input parameter 'code' can only be used in conjunction with parameter 'system'.";
    final String errorCode = "invariant";

    final UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(
            localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP);
    builder.queryParam("code", activeCode);
    final URI getUri = builder.build().toUri();

    // Act
    final String content = this.restTemplate.getForObject(getUri, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotSupported, (component.getDiagnostics()));
  }

  /**
   * Test code system lookup returns all properties when no property parameter specified.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupReturnsAllProperties() throws Exception {
    // Arrange
    final String activeCode = "C3224";
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    final String parameters = "?system=" + url + "&code=" + activeCode;

    // Act
    final String content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert - verify that properties are returned
    final List<ParametersParameterComponent> properties =
        params.getParameter().stream()
            .filter(param -> param.getName().equals("property"))
            .collect(Collectors.toList());
    assertFalse(properties.isEmpty(), "Properties should be returned");

    // Verify that standard properties are included
    final Set<String> propertyNames =
        properties.stream()
            .flatMap(prop -> prop.getPart().stream())
            .filter(part -> part.getName().equals("code"))
            .map(part -> ((CodeType) part.getValue()).getValue())
            .collect(Collectors.toSet());
    assertTrue(propertyNames.contains("active"), "Should include 'active' property");
  }

  /**
   * Test code system lookup with specific property filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupWithSpecificProperty() throws Exception {
    // Arrange
    final String activeCode = "C3224";
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    final String parameters = "?system=" + url + "&code=" + activeCode + "&property=Semantic_Type";

    // Act
    final String content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final Parameters params = parser.parseResource(Parameters.class, content);

    // Assert - verify standard fields are returned
    assertNotNull(params.getParameter("name"), "Should return 'name' field");
    assertNotNull(params.getParameter("display"), "Should return 'display' field");

    // Verify that properties are filtered
    final List<ParametersParameterComponent> properties =
        params.getParameter().stream()
            .filter(param -> param.getName().equals("property"))
            .collect(Collectors.toList());
    assertFalse(properties.isEmpty(), "Should have properties");

    // Verify that hardcoded properties are always included
    final Set<String> propertyNames =
        properties.stream()
            .flatMap(prop -> prop.getPart().stream())
            .filter(part -> part.getName().equals("code"))
            .map(part -> ((CodeType) part.getValue()).getValue())
            .collect(Collectors.toSet());
    assertTrue(
        propertyNames.contains("active"),
        "Should include hardcoded 'active' property even with filter");
  }

  /**
   * Test code system lookup with property parameter filters hardcoded properties correctly.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupPropertyParameterFiltersHardcodedProperties() throws Exception {
    // Test 1: Request only parent property
    final String activeCode = "T100";
    final String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;

    String parameters = "?system=" + url + "&code=" + activeCode + "&property=parent";
    String content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    List<ParametersParameterComponent> properties =
        params.getParameter().stream()
            .filter(p -> p.getName().equals("property"))
            .collect(Collectors.toList());

    // Verify active is present (always included)
    boolean hasActive =
        properties.stream()
            .anyMatch(
                prop ->
                    prop.getPart().stream()
                        .anyMatch(
                            part ->
                                part.getName().equals("code")
                                    && ((CodeType) part.getValue()).getValue().equals("active")));
    assertTrue(hasActive, "Should always have 'active' property");

    // Verify parent is present (requested)
    boolean hasParent =
        properties.stream()
            .anyMatch(
                prop ->
                    prop.getPart().stream()
                        .anyMatch(
                            part ->
                                part.getName().equals("code")
                                    && ((CodeType) part.getValue()).getValue().equals("parent")));
    assertTrue(hasParent, "Should have 'parent' property when requested");

    // Verify child is NOT present (not requested)
    boolean hasChild =
        properties.stream()
            .anyMatch(
                prop ->
                    prop.getPart().stream()
                        .anyMatch(
                            part ->
                                part.getName().equals("code")
                                    && ((CodeType) part.getValue()).getValue().equals("child")));
    assertFalse(hasChild, "Should NOT have 'child' property when not requested");

    // Test 2: Request only active property
    parameters = "?system=" + url + "&code=" + activeCode + "&property=active";
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    params = parser.parseResource(Parameters.class, content);

    properties =
        params.getParameter().stream()
            .filter(p -> p.getName().equals("property"))
            .collect(Collectors.toList());

    // Should only have active, not parent or child
    hasActive =
        properties.stream()
            .anyMatch(
                prop ->
                    prop.getPart().stream()
                        .anyMatch(
                            part ->
                                part.getName().equals("code")
                                    && ((CodeType) part.getValue()).getValue().equals("active")));
    assertTrue(hasActive, "Should have 'active' property");

    hasParent =
        properties.stream()
            .anyMatch(
                prop ->
                    prop.getPart().stream()
                        .anyMatch(
                            part ->
                                part.getName().equals("code")
                                    && ((CodeType) part.getValue()).getValue().equals("parent")));
    assertFalse(hasParent, "Should NOT have 'parent' when not requested");

    hasChild =
        properties.stream()
            .anyMatch(
                prop ->
                    prop.getPart().stream()
                        .anyMatch(
                            part ->
                                part.getName().equals("code")
                                    && ((CodeType) part.getValue()).getValue().equals("child")));
    assertFalse(hasChild, "Should NOT have 'child' when not requested");
  }

  /**
   * Test code system lookup displayLanguage parameter not supported.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupDisplayLanguageNotSupported() throws Exception {
    // Arrange
    final String activeCode = "C3224";
    final String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    final String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_LOOKUP;
    final String parameters = "?system=" + url + "&code=" + activeCode + "&displayLanguage=en";

    // Act
    String content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    final OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    final OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals("not-supported", component.getCode().toCode());
    assertTrue(component.getDiagnostics().contains("displayLanguage"));
  }
}
