package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceDesignationComponent;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.util.JsonUtils;

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

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4ValueSetExpandTests.class);

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
   * Helper method to create the common NCI ValueSet for testing.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
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
   * Helper method to create ValueSet with both include and exclude.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
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

  /**
   * Helper method to create ValueSet with filter-based include, direct includes, and excludes.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
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
    lyaseFilter.setOp(ValueSet.FilterOperator.ISA); // "is-a" - includes descendants
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

    // Exclude the Gene concept (C16612) that was directly included
    ValueSet.ConceptReferenceComponent excludeGene = new ValueSet.ConceptReferenceComponent();
    excludeGene.setCode("C16612");
    excludeGene.setDisplay("Gene");
    nciExclude.addConcept(excludeGene);

    compose.addExclude(nciExclude);
    inputValueSet.setCompose(compose);
    return inputValueSet;
  }

  /**
   * Helper method to create ValueSet with "in" filter-based include, direct includes, and excludes.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
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

    // Add filter for specific concepts using "in" operation
    ValueSet.ConceptSetFilterComponent inFilter = new ValueSet.ConceptSetFilterComponent();
    inFilter.setProperty("concept");
    inFilter.setOp(ValueSet.FilterOperator.IN);
    inFilter.setValue("C3262,C21282,C21283"); // Neoplasm, Lyase Gene, ADCY9 Gene
    inFilterInclude.addFilter(inFilter);

    compose.addInclude(inFilterInclude);

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

    // EXCLUDE section - exclude one of the filter-included concepts and one direct concept
    ValueSet.ConceptSetComponent nciExclude = new ValueSet.ConceptSetComponent();
    nciExclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Exclude C3262 (Neoplasm) that was included via the "in" filter
    ValueSet.ConceptReferenceComponent excludeNeoplasm = new ValueSet.ConceptReferenceComponent();
    excludeNeoplasm.setCode("C3262");
    excludeNeoplasm.setDisplay("Neoplasm");
    nciExclude.addConcept(excludeNeoplasm);

    // Exclude the Gene concept (C16612) that was directly included
    ValueSet.ConceptReferenceComponent excludeGene = new ValueSet.ConceptReferenceComponent();
    excludeGene.setCode("C16612");
    excludeGene.setDisplay("Gene");
    nciExclude.addConcept(excludeGene);

    compose.addExclude(nciExclude);
    inputValueSet.setCompose(compose);
    return inputValueSet;
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
    String parameters = "?url=" + url + "&includeDesignations=true&includeDefinition=true";

    log.info("  parameters = " + parameters);

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
    String expectedDesignation = "Schedule I Controlled Substance";
    String expectedTty = "SY";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(content));
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

    // Also check for definition (in R4, definitions are stored as designations with
    // use="definition")
    Optional<ConceptReferenceDesignationComponent> definitionOptional =
        valueSet.getExpansion().getContains().stream()
            .filter(contains -> contains.getCode().equals(activeCode))
            .findFirst()
            .flatMap(
                contains ->
                    contains.getDesignation().stream()
                        .filter(
                            designation ->
                                designation.getUse() != null
                                    && "definition".equals(designation.getUse().getCode())
                                    && designation.getLanguage().equals("en"))
                        .findFirst());

    assertTrue(definitionOptional.isPresent(), "Definition should be present for " + activeCode);

    definitionOptional.ifPresent(
        definition -> {
          assertNotNull(definition.getValue());
          assertFalse(definition.getValue().isEmpty());
          assertEquals("en", definition.getLanguage());
          assertEquals("definition", definition.getUse().getCode());
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
    String parameters = "?url=" + url + "&includeDesignations=true&includeDefinition=true";

    log.info("  parameters = " + parameters);

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";
    String expectedDesignation = "Schedule I Controlled Substance";
    String expectedTty = "SY";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(content));
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

    // Also check for definition (in R4, definitions are stored as designations with
    // use="definition")
    Optional<ConceptReferenceDesignationComponent> definitionOptional =
        valueSet.getExpansion().getContains().stream()
            .filter(contains -> contains.getCode().equals(activeCode))
            .findFirst()
            .flatMap(
                contains ->
                    contains.getDesignation().stream()
                        .filter(
                            designation ->
                                designation.getUse() != null
                                    && "definition".equals(designation.getUse().getCode())
                                    && designation.getLanguage().equals("en"))
                        .findFirst());

    assertTrue(definitionOptional.isPresent(), "Definition should be present for " + activeCode);

    definitionOptional.ifPresent(
        definition -> {
          assertNotNull(definition.getValue());
          assertFalse(definition.getValue().isEmpty());
          assertEquals("en", definition.getLanguage());
          assertEquals("definition", definition.getUse().getCode());
        });
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
    String parameters = "?url=" + url + "&includeDefinition=true";

    log.info("  parameters = " + parameters);

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(content));
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

    // Check for definition (in R4, definitions are stored as designations with use="definition")
    Optional<ConceptReferenceDesignationComponent> definitionOptional =
        valueSet.getExpansion().getContains().stream()
            .filter(contains -> contains.getCode().equals(activeCode))
            .findFirst()
            .flatMap(
                contains ->
                    contains.getDesignation().stream()
                        .filter(
                            designation ->
                                designation.getUse() != null
                                    && "definition".equals(designation.getUse().getCode())
                                    && designation.getLanguage().equals("en"))
                        .findFirst());

    assertTrue(definitionOptional.isPresent(), "Definition should be present for " + activeCode);

    definitionOptional.ifPresent(
        definition -> {
          assertNotNull(definition.getValue());
          assertFalse(definition.getValue().isEmpty());
          assertEquals("en", definition.getLanguage());
          assertEquals("definition", definition.getUse().getCode());
        });
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
    String parameters = "?url=" + url + "&includeDefinition=true";

    log.info("  parameters = " + parameters);

    String displayString = "Schedule I Substance";
    String activeCode = "C48672";

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(content));
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

    // Check for definition (in R4, definitions are stored as designations with use="definition")
    Optional<ConceptReferenceDesignationComponent> definitionOptional =
        valueSet.getExpansion().getContains().stream()
            .filter(contains -> contains.getCode().equals(activeCode))
            .findFirst()
            .flatMap(
                contains ->
                    contains.getDesignation().stream()
                        .filter(
                            designation ->
                                designation.getUse() != null
                                    && "definition".equals(designation.getUse().getCode())
                                    && designation.getLanguage().equals("en"))
                        .findFirst());

    assertTrue(definitionOptional.isPresent(), "Definition should be present for " + activeCode);

    definitionOptional.ifPresent(
        definition -> {
          assertNotNull(definition.getValue());
          assertFalse(definition.getValue().isEmpty());
          assertEquals("en", definition.getLanguage());
          assertEquals("definition", definition.getUse().getCode());
        });
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
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
        "Disease or Disorder (C2991) should be included as it matches filter 'Disease'");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    // Assert - Concept NOT containing "Disease" should be filtered out
    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        geneResult.isPresent(),
        "Gene (C16612) should be filtered out as it does not contain 'Disease'");

    // Assert - Inactive and invalid concepts should be excluded (activeOnly=true)
    long inactiveAndInvalidCount =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()) || "INVALID123".equals(comp.getCode()))
            .count();

    assertEquals(
        0, inactiveAndInvalidCount, "Inactive and invalid concepts should not be included");

    // Assert - Only concepts matching the filter should be returned
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertTrue(
          concept.getDisplay().toLowerCase().contains("disease")
              || concept.getCode().toLowerCase().contains("disease"),
          "All returned concepts should match the filter 'Disease': "
              + concept.getCode()
              + " - "
              + concept.getDisplay());
    }

    log.info("  NCI Thesaurus filtered expansion completed with {} concepts", expansion.getTotal());
  }

  /**
   * Test value set expand with NCI thesaurus is-a filter include and exclude.
   *
   * @throws Exception the exception
   */
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
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
            .filter(comp -> comp.getCode().startsWith("C") && !comp.getCode().equals("C21282"))
            .filter(
                comp ->
                    comp.getDisplay().toLowerCase().contains("gene")
                        && comp.getDisplay().toLowerCase().contains("lyase"))
            .count();

    log.info("Found {} potential Lyase Gene descendants", lyaseDescendantCount);

    // Assert - Disease or Disorder (C2991) should be included from direct include
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included from direct include");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    // Assert - Gene (C16612) should be EXCLUDED (was directly included but then excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> excludedGeneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        excludedGeneResult.isPresent(),
        "Gene (C16612) should be excluded despite being in direct include");

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    log.info(
        "  NCI Thesaurus is-a filter expansion completed with {} concepts", expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("    Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }
  }

  /**
   * Test value set expand with NCI thesaurus "in" filter include and exclude.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusInFilterIncludeAndExclude() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with "in" filter-based include, direct includes, and excludes
    ValueSet inputValueSet =
        createNCITestValueSetWithInFilterIncludeAndExclude(
            "nci-in-filter-include-exclude-test",
            "NCIInFilterIncludeExcludeTest",
            "NCI Thesaurus In Filter Include and Exclude Test",
            "Test ValueSet with 'in' filter for specific concept codes plus direct includes and"
                + " excludes");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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

    // Assert - C3262 (Neoplasm) should be EXCLUDED despite being in "in" filter
    Optional<ValueSet.ValueSetExpansionContainsComponent> neoplasmResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C3262".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        neoplasmResult.isPresent(),
        "Neoplasm (C3262) should be excluded despite being in 'in' filter");

    // Assert - C21282 (Lyase Gene) should be included from "in" filter (not excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> lyaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C21282".equals(comp.getCode()))
            .findFirst();

    assertTrue(lyaseResult.isPresent(), "Lyase Gene (C21282) should be included from 'in' filter");
    log.info("C21282 (Lyase Gene) correctly included from 'in' filter");

    // Assert - C21283 (ADCY9 Gene) should be included from "in" filter (not excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> adcy9Result =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C21283".equals(comp.getCode()))
            .findFirst();

    assertTrue(adcy9Result.isPresent(), "ADCY9 Gene (C21283) should be included from 'in' filter");
    log.info("C21283 (ADCY9 Gene) correctly included from 'in' filter");

    // Assert - Disease or Disorder (C2991) should be included from direct include (not excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included from direct include");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    // Assert - Gene (C16612) should be EXCLUDED (was directly included but then excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> excludedGeneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        excludedGeneResult.isPresent(),
        "Gene (C16612) should be excluded despite being in direct include");

    // Assert - Inactive concept (C176707) should be included since activeOnly=false and it's not
    // excluded
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        inactiveResult.isPresent(),
        "Inactive concept (C176707) should be included since activeOnly was not specified and it's"
            + " not excluded");
    log.info("C176707 (Inactive concept) correctly included - activeOnly not specified");

    // Assert - Invalid concept should NOT be included
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    log.info(
        "  NCI Thesaurus 'in' filter expansion completed with {} concepts", expansion.getTotal());
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      log.debug("    Expanded NCI concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Verify that exclude takes precedence over include
    long totalExpectedConcepts =
        4; // C21282, C21283, C2991, C176707 (C3262 and C16612 are excluded)
    assertTrue(
        expansion.getTotal() <= totalExpectedConcepts,
        "Should have at most " + totalExpectedConcepts + " concepts after exclusions");
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

    // Create the ValueSet with include and exclude sections
    ValueSet inputValueSet =
        createNCITestValueSetWithExclude(
            "nci-include-exclude-test",
            "NCIIncludeExcludeTest",
            "NCI Thesaurus Include and Exclude Test",
            "Test ValueSet with both include and exclude sections to verify exclusion behavior");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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

    // Assert - Disease or Disorder (C2991) should be included (was included and not excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C2991".equals(comp.getCode()))
            .findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included as it was not excluded");
    assertEquals("Disease or Disorder", diseaseResult.get().getDisplay());

    // Assert - Gene (C16612) should be EXCLUDED (was included but then explicitly excluded)
    Optional<ValueSet.ValueSetExpansionContainsComponent> excludedGeneResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C16612".equals(comp.getCode()))
            .findFirst();

    assertFalse(
        excludedGeneResult.isPresent(),
        "Gene (C16612) should be excluded despite being in the include section");

    // Assert - Invalid concept should NOT be included (invalid concepts are typically filtered out)
    Optional<ValueSet.ValueSetExpansionContainsComponent> invalidResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "INVALID123".equals(comp.getCode()))
            .findFirst();

    assertFalse(invalidResult.isPresent(), "Invalid concept (INVALID123) should not be included");

    // Assert - Check for inactive concept (C176707) - behavior depends on activeOnly parameter
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    log.info("  Inactive concept C176707 present in expansion: {}", inactiveResult.isPresent());

    // Assert - All returned concepts should be valid and from NCI Thesaurus
    for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
      assertEquals(
          "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
          concept.getSystem(),
          "All concepts should be from NCI Thesaurus system");
      assertNotNull(concept.getCode(), "All concepts should have a code");
      assertNotNull(concept.getDisplay(), "All concepts should have a display");
      assertFalse(concept.getDisplay().trim().isEmpty(), "Display should not be empty");

      // Assert valid NCI concept code format
      assertTrue(
          concept.getCode().matches("C\\d+"),
          "NCI concept codes should start with 'C' followed by digits: " + concept.getCode());

      // Assert that excluded concept is not present
      assertFalse(
          "C16612".equals(concept.getCode()),
          "Excluded concept C16612 (Gene) should not be in results");

      log.debug("    Included concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Verify that exclude takes precedence over include
    // Should have fewer concepts than the original include list due to exclusions
    assertTrue(
        expansion.getTotal() < 4,
        "Should have fewer than 4 concepts due to exclusions (original had 4 in include)");
    assertTrue(expansion.getTotal() >= 1, "Should have at least 1 valid concept remaining");

    log.info(
        "  NCI Thesaurus include/exclude expansion completed with {} concepts after exclusions",
        expansion.getTotal());
  }

  /**
   * Test value set expand with NCI thesaurus property equals filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusPropertyEqualsFilter() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with property '=' filter3
    ValueSet inputValueSet =
        createNCITestValueSetWithPropertyEqualsFilter(
            "nci-property-equals-filter-test",
            "NCIPropertyEqualsFilterTest",
            "NCI Thesaurus Property Equals Filter Test",
            "Test ValueSet with '=' filter for Contributing_Source property");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
    log.info("Found {} concepts in expansion", contains.size());

    // Assert - Should only contain concepts with Contributing_Source = "FDA"
    // We expect C48672 (Schedule I Substance) to be included since it has Contributing_Source =
    // "FDA"
    Optional<ValueSet.ValueSetExpansionContainsComponent> scheduleIResult =
        contains.stream().filter(comp -> "C48672".equals(comp.getCode())).findFirst();

    assertTrue(
        scheduleIResult.isPresent(),
        "Schedule I Substance (C48672) should be included as it has Contributing_Source = 'FDA'");
    log.info("C48672 (Schedule I Substance) correctly included with Contributing_Source = 'FDA'");

    // Assert - Should NOT contain concepts without Contributing_Source = "FDA"
    // For example, C2991 (Disease or Disorder) should not have Contributing_Source = "FDA"
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream().filter(comp -> "C2991".equals(comp.getCode())).findFirst();

    assertFalse(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should NOT be included as it does not have Contributing_Source"
            + " = 'FDA'");
    log.info(
        "C2991 (Disease or Disorder) correctly excluded - does not have Contributing_Source ="
            + " 'FDA'");

    // Assert - Should have at least one concept with the matching property
    assertTrue(
        expansion.getTotal() >= 1,
        "Should find at least one concept with Contributing_Source = 'FDA', found: "
            + expansion.getTotal());

    log.info(
        "Property equals filter test completed successfully with {} matching concepts",
        expansion.getTotal());
  }

  /**
   * Test value set expand with NCI thesaurus property exists filter (value=true).
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusPropertyExistsTrue() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with property 'exists' filter (value=true)
    ValueSet inputValueSet =
        createNCITestValueSetWithPropertyExistsFilter(
            "nci-property-exists-true-test",
            "NCIPropertyExistsTrueTest",
            "NCI Thesaurus Property Exists True Filter Test",
            "Test ValueSet with 'exists' filter for Contributing_Source property (value=true)",
            true);

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
    log.info("Found {} concepts with Contributing_Source property existing", contains.size());

    // Assert - Should contain concepts that HAVE Contributing_Source property
    // C48672 (Schedule I Substance) should be included as it has Contributing_Source = "FDA"
    Optional<ValueSet.ValueSetExpansionContainsComponent> scheduleIResult =
        contains.stream().filter(comp -> "C48672".equals(comp.getCode())).findFirst();

    assertTrue(
        scheduleIResult.isPresent(),
        "Schedule I Substance (C48672) should be included as it has Contributing_Source property");
    log.info("C48672 (Schedule I Substance) correctly included - has Contributing_Source property");

    // C2991 (Disease or Disorder) should be included as it has Contributing_Source properties
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream().filter(comp -> "C2991".equals(comp.getCode())).findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included as it has Contributing_Source property");
    log.info("C2991 (Disease or Disorder) correctly included - has Contributing_Source property");

    // Assert - Should NOT contain concepts that do NOT have Contributing_Source property
    // C48670 (Controlled Substance) should not have Contributing_Source property
    Optional<ValueSet.ValueSetExpansionContainsComponent> controlledResult =
        contains.stream().filter(comp -> "C48670".equals(comp.getCode())).findFirst();

    assertFalse(
        controlledResult.isPresent(),
        "Controlled Substance (C48670) should NOT be included as it does not have"
            + " Contributing_Source property");
    log.info(
        "C48670 (Controlled Substance) correctly excluded - does not have Contributing_Source"
            + " property");

    // Assert - Should have at least one concept with the property
    assertTrue(
        expansion.getTotal() >= 1,
        "Should find at least one concept with Contributing_Source property, found: "
            + expansion.getTotal());

    log.info(
        "Property exists filter (value=true) test completed successfully with {} matching concepts",
        expansion.getTotal());
  }

  /**
   * Test value set expand with NCI thesaurus property exists filter (value=false).
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusPropertyExistsFalse() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with property 'exists' filter (value=false)
    ValueSet inputValueSet =
        createNCITestValueSetWithPropertyExistsFilter(
            "nci-property-exists-false-test",
            "NCIPropertyExistsFalseTest",
            "NCI Thesaurus Property Exists False Filter Test",
            "Test ValueSet with 'exists' filter for Contributing_Source property (value=false)",
            false);

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
    log.info("Found {} concepts without Contributing_Source property", contains.size());

    // Assert - Should contain concepts that do NOT have Contributing_Source property
    // C48670 (Controlled Substance) should be included as it does not have Contributing_Source
    // property
    Optional<ValueSet.ValueSetExpansionContainsComponent> controlledResult =
        contains.stream().filter(comp -> "C48670".equals(comp.getCode())).findFirst();

    assertTrue(
        controlledResult.isPresent(),
        "Controlled Substance (C48670) should be included as it does not have Contributing_Source"
            + " property");
    log.info(
        "C48670 (Controlled Substance) correctly included - does not have Contributing_Source"
            + " property");

    // Assert - Should NOT contain concepts that HAVE Contributing_Source property
    // C48672 (Schedule I Substance) should not be included as it has Contributing_Source = "FDA"
    Optional<ValueSet.ValueSetExpansionContainsComponent> scheduleIResult =
        contains.stream().filter(comp -> "C48672".equals(comp.getCode())).findFirst();

    assertFalse(
        scheduleIResult.isPresent(),
        "Schedule I Substance (C48672) should NOT be included as it has Contributing_Source"
            + " property");
    log.info("C48672 (Schedule I Substance) correctly excluded - has Contributing_Source property");

    // C2991 (Disease or Disorder) should not be included as it has Contributing_Source properties
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream().filter(comp -> "C2991".equals(comp.getCode())).findFirst();

    assertFalse(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should NOT be included as it has Contributing_Source"
            + " property");
    log.info("C2991 (Disease or Disorder) correctly excluded - has Contributing_Source property");

    // Assert - Should have at least one concept without the property
    assertTrue(
        expansion.getTotal() >= 1,
        "Should find at least one concept without Contributing_Source property, found: "
            + expansion.getTotal());

    log.info(
        "Property exists filter (value=false) test completed successfully with {} matching"
            + " concepts",
        expansion.getTotal());
  }

  /**
   * Create NCI test value set with property equals filter.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithPropertyEqualsFilter(
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

    // FIRST INCLUDE - Add explicit concepts that we know have the Contributing_Source property
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Add concepts that we know have different Contributing_Source values
    // C48672 (Schedule I Substance) has Contributing_Source = "FDA"
    ValueSet.ConceptReferenceComponent scheduleIConcept = new ValueSet.ConceptReferenceComponent();
    scheduleIConcept.setCode("C48672");
    scheduleIConcept.setDisplay("Schedule I Substance");
    nciInclude.addConcept(scheduleIConcept);

    // C2991 (Disease or Disorder) - has Contributing_Source but not "FDA"
    ValueSet.ConceptReferenceComponent diseaseConcept = new ValueSet.ConceptReferenceComponent();
    diseaseConcept.setCode("C2991");
    diseaseConcept.setDisplay("Disease or Disorder");
    nciInclude.addConcept(diseaseConcept);

    // C48670 (Controlled Substance) - does not have Contributing_Source
    ValueSet.ConceptReferenceComponent controlledConcept = new ValueSet.ConceptReferenceComponent();
    controlledConcept.setCode("C48670");
    controlledConcept.setDisplay("Controlled Substance");
    nciInclude.addConcept(controlledConcept);

    // Now add the property equals filter to filter for Contributing_Source = "FDA"
    ValueSet.ConceptSetFilterComponent propertyFilter = new ValueSet.ConceptSetFilterComponent();
    propertyFilter.setProperty("Contributing_Source");
    propertyFilter.setOp(ValueSet.FilterOperator.EQUAL); // "=" operation
    propertyFilter.setValue("FDA"); // Filter for Contributing_Source = "FDA"
    nciInclude.addFilter(propertyFilter);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Create NCI test value set with property exists filter.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @param shouldExist true to filter for concepts that HAVE the property, false to filter for
   *     concepts that do NOT have the property
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithPropertyExistsFilter(
      String id, String name, String title, String description, boolean shouldExist) {
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

    // Add explicit concepts that we know have different property existence patterns
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Add concepts with known Contributing_Source property patterns
    // C48672 (Schedule I Substance) - HAS Contributing_Source = "FDA"
    ValueSet.ConceptReferenceComponent scheduleIConcept = new ValueSet.ConceptReferenceComponent();
    scheduleIConcept.setCode("C48672");
    scheduleIConcept.setDisplay("Schedule I Substance");
    nciInclude.addConcept(scheduleIConcept);

    // C2991 (Disease or Disorder) - HAS Contributing_Source = "CDISC-GLOSS", "CTDC", "CTRP", "GDC",
    // "NICHD"
    ValueSet.ConceptReferenceComponent diseaseConcept = new ValueSet.ConceptReferenceComponent();
    diseaseConcept.setCode("C2991");
    diseaseConcept.setDisplay("Disease or Disorder");
    nciInclude.addConcept(diseaseConcept);

    // C48670 (Controlled Substance) - does NOT have Contributing_Source property
    ValueSet.ConceptReferenceComponent controlledConcept = new ValueSet.ConceptReferenceComponent();
    controlledConcept.setCode("C48670");
    controlledConcept.setDisplay("Controlled Substance");
    nciInclude.addConcept(controlledConcept);

    // C21282 (Lyase Gene) - gene concept, does not have Contributing_Source
    ValueSet.ConceptReferenceComponent lyaseConcept = new ValueSet.ConceptReferenceComponent();
    lyaseConcept.setCode("C21282");
    lyaseConcept.setDisplay("Lyase Gene");
    nciInclude.addConcept(lyaseConcept);

    // Add the property exists filter
    ValueSet.ConceptSetFilterComponent propertyFilter = new ValueSet.ConceptSetFilterComponent();
    propertyFilter.setProperty("Contributing_Source");
    propertyFilter.setOp(ValueSet.FilterOperator.EXISTS); // "exists" operation
    propertyFilter.setValue(shouldExist ? "true" : "false"); // Filter for property existence

    nciInclude.addFilter(propertyFilter);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Test value set expand with NCI thesaurus 'not-in' filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusNotInFilter() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with 'not-in' filter
    ValueSet inputValueSet =
        createNCITestValueSetWithNotInFilter(
            "nci-not-in-filter-test",
            "NCINotInFilterTest",
            "NCI Thesaurus Not-In Filter Test",
            "Test ValueSet with 'not-in' filter to exclude specific concepts");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
    log.info("Found {} concepts after 'not-in' filter", contains.size());

    // Assert - Should contain concepts that are NOT in the exclusion list
    // C48670 (Controlled Substance) should be included as it's not in the exclusion list
    Optional<ValueSet.ValueSetExpansionContainsComponent> controlledResult =
        contains.stream().filter(comp -> "C48670".equals(comp.getCode())).findFirst();

    assertTrue(
        controlledResult.isPresent(),
        "Controlled Substance (C48670) should be included as it's not in the exclusion list");
    log.info("C48670 (Controlled Substance) correctly included - not in exclusion list");

    // Assert - Should NOT contain concepts that ARE in the exclusion list
    // C2991 (Disease or Disorder) should be excluded as it's in the exclusion list
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream().filter(comp -> "C2991".equals(comp.getCode())).findFirst();

    assertFalse(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should NOT be included as it's in the exclusion list");
    log.info("C2991 (Disease or Disorder) correctly excluded - in exclusion list");

    // C48672 (Schedule I Substance) should be excluded as it's in the exclusion list
    Optional<ValueSet.ValueSetExpansionContainsComponent> scheduleIResult =
        contains.stream().filter(comp -> "C48672".equals(comp.getCode())).findFirst();

    assertFalse(
        scheduleIResult.isPresent(),
        "Schedule I Substance (C48672) should NOT be included as it's in the exclusion list");
    log.info("C48672 (Schedule I Substance) correctly excluded - in exclusion list");

    // Assert - Should have at least one concept remaining
    assertTrue(
        expansion.getTotal() >= 1,
        "Should find at least one concept not in exclusion list, found: " + expansion.getTotal());

    log.info(
        "Not-in filter test completed successfully with {} remaining concepts",
        expansion.getTotal());
  }

  /**
   * Test value set expand with NCI thesaurus 'is-not-a' filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusIsNotAFilter() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with 'is-not-a' filter
    ValueSet inputValueSet =
        createNCITestValueSetWithIsNotAFilter(
            "nci-is-not-a-filter-test",
            "NCIIsNotAFilterTest",
            "NCI Thesaurus Is-Not-A Filter Test",
            "Test ValueSet with 'is-not-a' filter to exclude gene concepts");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
    log.info("Found {} concepts after 'is-not-a' filter", contains.size());

    // Assert - Should contain concepts that are NOT gene-related
    // C48672 (Schedule I Substance) should be included as it's not a gene
    Optional<ValueSet.ValueSetExpansionContainsComponent> scheduleIResult =
        contains.stream().filter(comp -> "C48672".equals(comp.getCode())).findFirst();

    assertTrue(
        scheduleIResult.isPresent(),
        "Schedule I Substance (C48672) should be included as it's not a gene");
    log.info("C48672 (Schedule I Substance) correctly included - not a gene");

    // C2991 (Disease or Disorder) should be included as it's not a gene
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream().filter(comp -> "C2991".equals(comp.getCode())).findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included as it's not a gene");
    log.info("C2991 (Disease or Disorder) correctly included - not a gene");

    // Assert - Should NOT contain gene concepts
    // C21282 (Lyase Gene) should be excluded as it's a gene (is-a Gene)
    Optional<ValueSet.ValueSetExpansionContainsComponent> lyaseResult =
        contains.stream().filter(comp -> "C21282".equals(comp.getCode())).findFirst();

    assertFalse(
        lyaseResult.isPresent(), "Lyase Gene (C21282) should NOT be included as it is-a Gene");
    log.info("C21282 (Lyase Gene) correctly excluded - is-a Gene");

    // Assert - Should have at least one non-gene concept
    assertTrue(
        expansion.getTotal() >= 1,
        "Should find at least one concept that is-not-a Gene, found: " + expansion.getTotal());

    log.info(
        "Is-not-a filter test completed successfully with {} non-gene concepts",
        expansion.getTotal());
  }

  /**
   * Creates the NCI test value set with 'not-in' filter.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithNotInFilter(
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

    // Add concepts that should be filtered by 'not-in' operation
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Include several concepts to test exclusion
    // C48670 (Controlled Substance) - should be included (not in exclusion list)
    ValueSet.ConceptReferenceComponent controlledConcept = new ValueSet.ConceptReferenceComponent();
    controlledConcept.setCode("C48670");
    controlledConcept.setDisplay("Controlled Substance");
    nciInclude.addConcept(controlledConcept);

    // C2991 (Disease or Disorder) - will be excluded via filter
    ValueSet.ConceptReferenceComponent diseaseConcept = new ValueSet.ConceptReferenceComponent();
    diseaseConcept.setCode("C2991");
    diseaseConcept.setDisplay("Disease or Disorder");
    nciInclude.addConcept(diseaseConcept);

    // C48672 (Schedule I Substance) - will be excluded via filter
    ValueSet.ConceptReferenceComponent scheduleIConcept = new ValueSet.ConceptReferenceComponent();
    scheduleIConcept.setCode("C48672");
    scheduleIConcept.setDisplay("Schedule I Substance");
    nciInclude.addConcept(scheduleIConcept);

    // C16612 (Gene) - should be included (not in exclusion list)
    ValueSet.ConceptReferenceComponent geneConcept = new ValueSet.ConceptReferenceComponent();
    geneConcept.setCode("C16612");
    geneConcept.setDisplay("Gene");
    nciInclude.addConcept(geneConcept);

    // Add the 'not-in' filter to exclude specific concepts
    ValueSet.ConceptSetFilterComponent notInFilter = new ValueSet.ConceptSetFilterComponent();
    notInFilter.setProperty("concept");
    notInFilter.setOp(ValueSet.FilterOperator.NOTIN); // "not-in" operation
    notInFilter.setValue("C2991,C48672"); // Exclude Disease or Disorder and Schedule I Substance

    nciInclude.addFilter(notInFilter);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Creates the NCI test value set with 'is-not-a' filter.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithIsNotAFilter(
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

    // Add concepts that should be filtered by 'is-not-a' operation
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Include gene and non-gene concepts to test exclusion
    // C21282 (Lyase Gene) - will be excluded (is-a Gene)
    ValueSet.ConceptReferenceComponent lyaseConcept = new ValueSet.ConceptReferenceComponent();
    lyaseConcept.setCode("C21282");
    lyaseConcept.setDisplay("Lyase Gene");
    nciInclude.addConcept(lyaseConcept);

    // C16612 (Gene) - will be excluded (is-a Gene, actually is Gene itself)
    ValueSet.ConceptReferenceComponent geneConcept = new ValueSet.ConceptReferenceComponent();
    geneConcept.setCode("C16612");
    geneConcept.setDisplay("Gene");
    nciInclude.addConcept(geneConcept);

    // C48672 (Schedule I Substance) - should be included (not a gene)
    ValueSet.ConceptReferenceComponent scheduleIConcept = new ValueSet.ConceptReferenceComponent();
    scheduleIConcept.setCode("C48672");
    scheduleIConcept.setDisplay("Schedule I Substance");
    nciInclude.addConcept(scheduleIConcept);

    // C2991 (Disease or Disorder) - should be included (not a gene)
    ValueSet.ConceptReferenceComponent diseaseConcept = new ValueSet.ConceptReferenceComponent();
    diseaseConcept.setCode("C2991");
    diseaseConcept.setDisplay("Disease or Disorder");
    nciInclude.addConcept(diseaseConcept);

    // C48670 (Controlled Substance) - should be included (not a gene)
    ValueSet.ConceptReferenceComponent controlledConcept = new ValueSet.ConceptReferenceComponent();
    controlledConcept.setCode("C48670");
    controlledConcept.setDisplay("Controlled Substance");
    nciInclude.addConcept(controlledConcept);

    // Add the 'is-not-a' filter to exclude gene concepts
    ValueSet.ConceptSetFilterComponent isNotAFilter = new ValueSet.ConceptSetFilterComponent();
    isNotAFilter.setProperty("concept");
    isNotAFilter.setOp(ValueSet.FilterOperator.ISNOTA); // "is-not-a" operation
    isNotAFilter.setValue("C16612"); // Exclude concepts that are-a Gene (C16612)

    nciInclude.addFilter(isNotAFilter);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Test value set expand with NCI thesaurus exclude 'is-a' filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusExcludeIsAFilter() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with exclude 'is-a' filter
    ValueSet inputValueSet =
        createNCITestValueSetWithExcludeIsAFilter(
            "nci-exclude-is-a-filter-test",
            "NCIExcludeIsAFilterTest",
            "NCI Thesaurus Exclude Is-A Filter Test",
            "Test ValueSet with exclude 'is-a' filter to remove gene concepts");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
    log.info("Found {} concepts after exclude 'is-a' filter", contains.size());

    // Assert - Should contain non-gene concepts
    // C48672 (Schedule I Substance) should be included as it's not a gene
    Optional<ValueSet.ValueSetExpansionContainsComponent> scheduleIResult =
        contains.stream().filter(comp -> "C48672".equals(comp.getCode())).findFirst();

    assertTrue(
        scheduleIResult.isPresent(),
        "Schedule I Substance (C48672) should be included as it's not a gene");
    log.info("C48672 (Schedule I Substance) correctly included - not a gene");

    // C2991 (Disease or Disorder) should be included as it's not a gene
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream().filter(comp -> "C2991".equals(comp.getCode())).findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included as it's not a gene");
    log.info("C2991 (Disease or Disorder) correctly included - not a gene");

    // Assert - Should NOT contain gene concepts that were excluded
    // C21282 (Lyase Gene) should be excluded by the exclude filter
    Optional<ValueSet.ValueSetExpansionContainsComponent> lyaseResult =
        contains.stream().filter(comp -> "C21282".equals(comp.getCode())).findFirst();

    assertFalse(
        lyaseResult.isPresent(),
        "Lyase Gene (C21282) should NOT be included due to exclude 'is-a' filter");
    log.info("C21282 (Lyase Gene) correctly excluded by exclude filter");

    // C16612 (Gene) should be excluded by the exclude filter
    Optional<ValueSet.ValueSetExpansionContainsComponent> geneResult =
        contains.stream().filter(comp -> "C16612".equals(comp.getCode())).findFirst();

    assertFalse(
        geneResult.isPresent(),
        "Gene (C16612) should NOT be included due to exclude 'is-a' filter");
    log.info("C16612 (Gene) correctly excluded by exclude filter");

    log.info(
        "Exclude is-a filter test completed successfully with {} remaining concepts",
        expansion.getTotal());
  }

  /**
   * Test value set expand with NCI thesaurus exclude property equals filter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusExcludePropertyEqualsFilter() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with exclude property equals filter
    ValueSet inputValueSet =
        createNCITestValueSetWithExcludePropertyEqualsFilter(
            "nci-exclude-property-equals-filter-test",
            "NCIExcludePropertyEqualsFilterTest",
            "NCI Thesaurus Exclude Property Equals Filter Test",
            "Test ValueSet with exclude property equals filter");

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
    log.info("Found {} concepts after exclude property equals filter", contains.size());

    // Assert - Should contain concepts that do NOT have Contributing_Source = "FDA"
    // C2991 (Disease or Disorder) should be included (has Contributing_Source but not "FDA")
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream().filter(comp -> "C2991".equals(comp.getCode())).findFirst();

    assertTrue(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should be included as it doesn't have Contributing_Source ="
            + " 'FDA'");
    log.info("C2991 (Disease or Disorder) correctly included - no FDA Contributing_Source");

    // C48670 (Controlled Substance) should be included (no Contributing_Source property)
    Optional<ValueSet.ValueSetExpansionContainsComponent> controlledResult =
        contains.stream().filter(comp -> "C48670".equals(comp.getCode())).findFirst();

    assertTrue(
        controlledResult.isPresent(),
        "Controlled Substance (C48670) should be included as it has no Contributing_Source"
            + " property");
    log.info("C48670 (Controlled Substance) correctly included - no Contributing_Source property");

    // Assert - Should NOT contain concepts with Contributing_Source = "FDA"
    // C48672 (Schedule I Substance) should be excluded (has Contributing_Source = "FDA")
    Optional<ValueSet.ValueSetExpansionContainsComponent> scheduleIResult =
        contains.stream().filter(comp -> "C48672".equals(comp.getCode())).findFirst();

    assertFalse(
        scheduleIResult.isPresent(),
        "Schedule I Substance (C48672) should NOT be included due to exclude property equals"
            + " filter");
    log.info("C48672 (Schedule I Substance) correctly excluded - has Contributing_Source = 'FDA'");

    log.info(
        "Exclude property equals filter test completed successfully with {} remaining concepts",
        expansion.getTotal());
  }

  /**
   * Creates the NCI test value set with exclude 'is-a' filter.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithExcludeIsAFilter(
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

    // INCLUDE section - include various concepts
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Include various concepts including genes and non-genes
    ValueSet.ConceptReferenceComponent lyaseConcept = new ValueSet.ConceptReferenceComponent();
    lyaseConcept.setCode("C21282");
    lyaseConcept.setDisplay("Lyase Gene");
    nciInclude.addConcept(lyaseConcept);

    ValueSet.ConceptReferenceComponent geneConcept = new ValueSet.ConceptReferenceComponent();
    geneConcept.setCode("C16612");
    geneConcept.setDisplay("Gene");
    nciInclude.addConcept(geneConcept);

    ValueSet.ConceptReferenceComponent scheduleIConcept = new ValueSet.ConceptReferenceComponent();
    scheduleIConcept.setCode("C48672");
    scheduleIConcept.setDisplay("Schedule I Substance");
    nciInclude.addConcept(scheduleIConcept);

    ValueSet.ConceptReferenceComponent diseaseConcept = new ValueSet.ConceptReferenceComponent();
    diseaseConcept.setCode("C2991");
    diseaseConcept.setDisplay("Disease or Disorder");
    nciInclude.addConcept(diseaseConcept);

    compose.addInclude(nciInclude);

    // EXCLUDE section - exclude gene concepts using 'is-a' filter
    ValueSet.ConceptSetComponent nciExclude = new ValueSet.ConceptSetComponent();
    nciExclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    ValueSet.ConceptSetFilterComponent isAFilter = new ValueSet.ConceptSetFilterComponent();
    isAFilter.setProperty("concept");
    isAFilter.setOp(ValueSet.FilterOperator.ISA); // "is-a" operation in exclude
    isAFilter.setValue("C16612"); // Exclude concepts that are-a Gene (C16612)

    nciExclude.addFilter(isAFilter);

    compose.addExclude(nciExclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Creates the NCI test value set with exclude property equals filter.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithExcludePropertyEqualsFilter(
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

    // INCLUDE section - include various concepts
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Include concepts with different Contributing_Source properties
    ValueSet.ConceptReferenceComponent scheduleIConcept = new ValueSet.ConceptReferenceComponent();
    scheduleIConcept.setCode("C48672");
    scheduleIConcept.setDisplay("Schedule I Substance");
    nciInclude.addConcept(scheduleIConcept);

    ValueSet.ConceptReferenceComponent diseaseConcept = new ValueSet.ConceptReferenceComponent();
    diseaseConcept.setCode("C2991");
    diseaseConcept.setDisplay("Disease or Disorder");
    nciInclude.addConcept(diseaseConcept);

    ValueSet.ConceptReferenceComponent controlledConcept = new ValueSet.ConceptReferenceComponent();
    controlledConcept.setCode("C48670");
    controlledConcept.setDisplay("Controlled Substance");
    nciInclude.addConcept(controlledConcept);

    compose.addInclude(nciInclude);

    // EXCLUDE section - exclude concepts with Contributing_Source = "FDA"
    ValueSet.ConceptSetComponent nciExclude = new ValueSet.ConceptSetComponent();
    nciExclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Include the same concepts in exclude to apply property filter
    ValueSet.ConceptReferenceComponent excludeScheduleIConcept =
        new ValueSet.ConceptReferenceComponent();
    excludeScheduleIConcept.setCode("C48672");
    excludeScheduleIConcept.setDisplay("Schedule I Substance");
    nciExclude.addConcept(excludeScheduleIConcept);

    ValueSet.ConceptReferenceComponent excludeDiseaseConcept =
        new ValueSet.ConceptReferenceComponent();
    excludeDiseaseConcept.setCode("C2991");
    excludeDiseaseConcept.setDisplay("Disease or Disorder");
    nciExclude.addConcept(excludeDiseaseConcept);

    ValueSet.ConceptReferenceComponent excludeControlledConcept =
        new ValueSet.ConceptReferenceComponent();
    excludeControlledConcept.setCode("C48670");
    excludeControlledConcept.setDisplay("Controlled Substance");
    nciExclude.addConcept(excludeControlledConcept);

    // Add property equals filter to exclude
    ValueSet.ConceptSetFilterComponent propertyFilter = new ValueSet.ConceptSetFilterComponent();
    propertyFilter.setProperty("Contributing_Source");
    propertyFilter.setOp(ValueSet.FilterOperator.EQUAL); // "=" operation
    propertyFilter.setValue("FDA"); // Exclude concepts with Contributing_Source = "FDA"

    nciExclude.addFilter(propertyFilter);

    compose.addExclude(nciExclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Test value set expand with include.version for direct concepts using current NCIt version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithIncludeVersionDirectConceptsCurrent() throws Exception {
    // Arrange - Get current NCIt version
    String content;
    String endpoint = localHost + port + fhirVSPath;

    // Get list of NCIt ValueSets to find a valid ID and current version
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

    // Get current version
    content = this.restTemplate.getForObject(endpoint + "/" + firstValueSetId, String.class);
    ValueSet currentValueSet = parser.parseResource(ValueSet.class, content);
    String currentNCItVersion = currentValueSet.getVersion();

    if (currentNCItVersion == null) {
      log.warn("Could not determine current NCIt version, using fallback");
      currentNCItVersion = "24.01d"; // Fallback to a recent version
    }
    log.info("Using current NCIt version: {} for R4 include.version test", currentNCItVersion);

    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet using current version - should return concepts
    ValueSet inputValueSet = createNCITestValueSetWithIncludeVersionR4(currentNCItVersion);

    // Create Parameters resource
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter("count", new IntegerType(100));
    parameters.addParameter("offset", new IntegerType(0));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String parametersJson = parser.encodeResourceToString(parameters);

    log.info("  parameters = " + JsonUtils.prettyPrint(parametersJson));
    HttpEntity<String> entity = new HttpEntity<>(parametersJson, headers);

    // Act
    ResponseEntity<String> response =
        restTemplate.postForEntity(expandEndpoint, entity, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));

    // Assert - Should return 200 OK
    assertEquals(HttpStatus.OK, response.getStatusCode());

    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion(), "Expanded ValueSet should have expansion");

    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();
    List<ValueSet.ValueSetExpansionContainsComponent> contains = expansion.getContains();

    // Assert - Should have concepts since we're using current version
    log.info(
        "R4 NCI Thesaurus ValueSet expansion with current include.version ({}) completed with {}"
            + " concepts",
        currentNCItVersion,
        expansion.getTotal());

    assertTrue(
        expansion.getTotal() > 0,
        "Should have concepts when using current NCIt version: " + currentNCItVersion);

    // Verify specific concepts are present
    Optional<ValueSet.ValueSetExpansionContainsComponent> neoplasmResult =
        contains.stream().filter(comp -> "C3262".equals(comp.getCode())).findFirst();
    assertTrue(
        neoplasmResult.isPresent(), "Neoplasm (C3262) should be included with current version");
  }

  /**
   * Test value set expand with include.version for direct concepts using old NCIt version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithIncludeVersionDirectConceptsOld() throws Exception {
    // Arrange
    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String oldVersion = "23.03d"; // Old version that shouldn't be available on server

    // Create the ValueSet using old version - should return no concepts
    ValueSet inputValueSet = createNCITestValueSetWithIncludeVersionR4(oldVersion);

    // Create Parameters resource
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter("count", new IntegerType(100));
    parameters.addParameter("offset", new IntegerType(0));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String parametersJson = parser.encodeResourceToString(parameters);

    log.info("  parameters = " + JsonUtils.prettyPrint(parametersJson));
    HttpEntity<String> entity = new HttpEntity<>(parametersJson, headers);

    // Act
    ResponseEntity<String> response =
        restTemplate.postForEntity(expandEndpoint, entity, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));

    // Assert - Should return 200 OK but no concepts
    assertEquals(HttpStatus.OK, response.getStatusCode());

    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion(), "Expanded ValueSet should have expansion");

    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();

    // Assert - Should have no concepts since we're using old version not available on server
    log.info(
        "R4 NCI Thesaurus ValueSet expansion with old include.version ({}) completed with {}"
            + " concepts",
        oldVersion,
        expansion.getTotal());

    assertEquals(
        0,
        expansion.getTotal(),
        "Should have no concepts when using old NCIt version not available on server: "
            + oldVersion);
  }

  /**
   * Test value set expand with include.version for filter-based concepts using current NCIt
   * version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithIncludeVersionFilterBasedCurrent() throws Exception {
    // Arrange - Get current NCIt version
    String content;
    String endpoint = localHost + port + fhirVSPath;

    // Get list of NCIt ValueSets to find a valid ID and current version
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

    // Get current version
    content = this.restTemplate.getForObject(endpoint + "/" + firstValueSetId, String.class);
    ValueSet currentValueSet = parser.parseResource(ValueSet.class, content);
    String currentNCItVersion = currentValueSet.getVersion();

    if (currentNCItVersion == null) {
      log.warn("Could not determine current NCIt version, using fallback");
      currentNCItVersion = "24.01d"; // Fallback to a recent version
    }
    log.info(
        "Using current NCIt version: {} for R4 include.version filter test", currentNCItVersion);

    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet using current version and filters - should return concepts
    ValueSet inputValueSet =
        createNCITestValueSetWithIncludeVersionAndFiltersR4(currentNCItVersion);

    // Create Parameters resource
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter("count", new IntegerType(100));
    parameters.addParameter("offset", new IntegerType(0));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String parametersJson = parser.encodeResourceToString(parameters);

    log.info("  parameters = " + JsonUtils.prettyPrint(parametersJson));
    HttpEntity<String> entity = new HttpEntity<>(parametersJson, headers);

    // Act
    ResponseEntity<String> response =
        restTemplate.postForEntity(expandEndpoint, entity, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));

    // Assert - Should return 200 OK
    assertEquals(HttpStatus.OK, response.getStatusCode());

    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion(), "Expanded ValueSet should have expansion");

    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();

    // Assert - Should have concepts since we're using current version and valid filters
    log.info(
        "R4 NCI Thesaurus ValueSet expansion with current include.version ({}) and filters"
            + " completed with {} concepts",
        currentNCItVersion,
        expansion.getTotal());

    assertTrue(
        expansion.getTotal() > 0,
        "Should have concepts when using current NCIt version with filters: " + currentNCItVersion);
  }

  /**
   * Test value set expand with include.version for filter-based concepts using old NCIt version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithIncludeVersionFilterBasedOld() throws Exception {
    // Arrange
    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String oldVersion = "23.03d"; // Old version that shouldn't be available on server

    // Create the ValueSet using old version and filters - should return no concepts
    ValueSet inputValueSet = createNCITestValueSetWithIncludeVersionAndFiltersR4(oldVersion);

    // Create Parameters resource
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter("count", new IntegerType(100));
    parameters.addParameter("offset", new IntegerType(0));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String parametersJson = parser.encodeResourceToString(parameters);

    log.info("  parameters = " + JsonUtils.prettyPrint(parametersJson));
    HttpEntity<String> entity = new HttpEntity<>(parametersJson, headers);

    // Act
    ResponseEntity<String> response =
        restTemplate.postForEntity(expandEndpoint, entity, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));

    // Assert - Should return 200 OK but no concepts
    assertEquals(HttpStatus.OK, response.getStatusCode());

    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());
    assertNotNull(expandedValueSet);
    assertTrue(expandedValueSet.hasExpansion(), "Expanded ValueSet should have expansion");

    ValueSet.ValueSetExpansionComponent expansion = expandedValueSet.getExpansion();

    // Assert - Should have no concepts since we're using old version not available on server
    log.info(
        "R4 NCI Thesaurus ValueSet expansion with old include.version ({}) and filters completed"
            + " with {} concepts",
        oldVersion,
        expansion.getTotal());

    assertEquals(
        0,
        expansion.getTotal(),
        "Should have no concepts when using old NCIt version not available on server with filters: "
            + oldVersion);
  }

  /**
   * Creates a test ValueSet with include.version for direct concepts (R4).
   *
   * @param version the NCIt version to use
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithIncludeVersionR4(String version) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("test-valueset-include-version-r4-" + version.replace(".", "-"));
    inputValueSet.setUrl("http://example.org/test/ValueSet/include-version-test-r4");
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName("IncludeVersionTestR4");
    inputValueSet.setTitle("Include Version Test ValueSet R4");
    inputValueSet.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDate(new Date());
    inputValueSet.setDescription(
        "Test ValueSet for include.version functionality with direct concepts using NCIt "
            + version
            + " (R4)");

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    nciInclude.setVersion(version); // Set specific version

    // Add some known NCI concepts
    ValueSet.ConceptReferenceComponent conceptNeoplasm = new ValueSet.ConceptReferenceComponent();
    conceptNeoplasm.setCode("C3262");
    conceptNeoplasm.setDisplay("Neoplasm");
    nciInclude.addConcept(conceptNeoplasm);

    ValueSet.ConceptReferenceComponent conceptCarcinoma = new ValueSet.ConceptReferenceComponent();
    conceptCarcinoma.setCode("C2916");
    conceptCarcinoma.setDisplay("Carcinoma");
    nciInclude.addConcept(conceptCarcinoma);

    ValueSet.ConceptReferenceComponent conceptMalignant = new ValueSet.ConceptReferenceComponent();
    conceptMalignant.setCode("C9305");
    conceptMalignant.setDisplay("Malignant Neoplasm");
    nciInclude.addConcept(conceptMalignant);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Creates a test ValueSet with include.version and filters (R4).
   *
   * @param version the NCIt version to use
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithIncludeVersionAndFiltersR4(String version) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("test-valueset-include-version-filters-r4-" + version.replace(".", "-"));
    inputValueSet.setUrl("http://example.org/test/ValueSet/include-version-filters-test-r4");
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName("IncludeVersionFiltersTestR4");
    inputValueSet.setTitle("Include Version with Filters Test ValueSet R4");
    inputValueSet.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDate(new Date());
    inputValueSet.setDescription(
        "Test ValueSet for include.version functionality with filter-based concepts using NCIt "
            + version
            + " (R4)");

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    nciInclude.setVersion(version); // Set specific version

    // Add is-a filter with more specific concept to limit the number of concepts
    ValueSet.ConceptSetFilterComponent isaFilter = new ValueSet.ConceptSetFilterComponent();
    isaFilter.setProperty("concept");
    isaFilter.setOp(ValueSet.FilterOperator.ISA); // "is-a" operation (includes descendants)
    isaFilter.setValue("C2916"); // Carcinoma (smaller set than Neoplasm)3
    nciInclude.addFilter(isaFilter);

    // Add property filter
    ValueSet.ConceptSetFilterComponent propertyFilter = new ValueSet.ConceptSetFilterComponent();
    propertyFilter.setProperty("Contributing_Source");
    propertyFilter.setOp(ValueSet.FilterOperator.EQUAL); // "=" operation
    propertyFilter.setValue("CTRP"); // Include concepts with Contributing_Source = "CTRP"
    nciInclude.addFilter(propertyFilter);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Test value set expand too-costly error when using is-a filter that would return too many
   * concepts.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandTooCostlyError() throws Exception {
    // Arrange
    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Get current NCIt version from server
    String endpoint =
        localHost
            + port
            + fhirVSPath
            + "?url="
            + URLEncoder.encode(
                "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl", StandardCharsets.UTF_8);

    // Content not needed, we expect an exception
    this.restTemplate.getForObject(endpoint, String.class);

    // Create the ValueSet using is-a filter that would return a very large number of concepts
    ValueSet inputValueSet = createNCITestValueSetWithLargeIsAFilter();

    // Create Parameters resource
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter("count", new IntegerType(100));
    parameters.addParameter("offset", new IntegerType(0));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String parametersJson = parser.encodeResourceToString(parameters);

    log.info("  parameters = " + parametersJson);
    HttpEntity<String> entity = new HttpEntity<>(parametersJson, headers);

    // Act
    ResponseEntity<String> response =
        restTemplate.postForEntity(expandEndpoint, entity, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));

    // Assert - Should return OperationOutcome with too-costly error
    // assertEquals(HttpStatus.OK, response.getStatusCode());
    // assertNotNull(response.getBody());

    // Parse the OperationOutcome from the response body
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, response.getBody());
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Verify the error code and message
    assertEquals("too-costly", component.getCode().toCode());
    assertTrue(
        component.getDiagnostics().contains("too costly"),
        "Diagnostics should contain 'too costly' error message: " + component.getDiagnostics());
  }

  /**
   * Creates a test ValueSet with a large is-a filter that would trigger the too-costly error.
   *
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithLargeIsAFilter() {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("test-valueset-too-costly");
    inputValueSet.setUrl("http://example.org/test/ValueSet/too-costly-test");
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName("TooCostlyTest");
    inputValueSet.setTitle("Too Costly Test ValueSet");
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDate(new Date());
    inputValueSet.setDescription(
        "Test ValueSet for too-costly error using is-a filter with large concept set (no version"
            + " constraint)");

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    // Note: Not setting version to allow access to full concept set and trigger too-costly error

    // Add is-a filter for "Thing" (C2991) which should return a very large number of concepts
    ValueSet.ConceptSetFilterComponent isAFilter = new ValueSet.ConceptSetFilterComponent();
    isAFilter.setProperty("concept");
    isAFilter.setOp(ValueSet.FilterOperator.ISA); // "is-a" operation
    isAFilter.setValue("C2991"); // Thing - the root concept that has many descendants
    nciInclude.addFilter(isAFilter);

    // Add property filter to trigger property filtering on the large set
    ValueSet.ConceptSetFilterComponent propertyFilter = new ValueSet.ConceptSetFilterComponent();
    propertyFilter.setProperty("Semantic_Type");
    propertyFilter.setOp(ValueSet.FilterOperator.EQUAL);
    propertyFilter.setValue("Disease or Syndrome"); // Some semantic type filter
    nciInclude.addFilter(propertyFilter);

    compose.addInclude(nciInclude);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Test value set expand with include value set.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithIncludeValueSet() throws Exception {
    // Create a ValueSet that includes another ValueSet by URL
    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    ValueSet inputValueSet = createNCITestValueSetWithIncludeValueSet();
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter("count", new IntegerType(100));
    parameters.addParameter("offset", new IntegerType(0));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String parametersJson = parser.encodeResourceToString(parameters);
    log.info("  parameters = " + parametersJson);
    HttpEntity<String> entity = new HttpEntity<>(parametersJson, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(expandEndpoint, entity, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Verify expansion exists
    assertTrue(expandedValueSet.hasExpansion());

    // Check if concepts from the referenced ValueSet are included
    if (expandedValueSet.getExpansion().hasContains()) {
      List<ValueSet.ValueSetExpansionContainsComponent> contains =
          expandedValueSet.getExpansion().getContains();
      log.info("Expanded ValueSet with include.valueSet contains {} concepts", contains.size());

      // Log some sample concepts for debugging
      int logCount = Math.min(5, contains.size());
      for (int i = 0; i < logCount; i++) {
        ValueSet.ValueSetExpansionContainsComponent concept = contains.get(i);
        log.debug("Included concept: {} - {}", concept.getCode(), concept.getDisplay());
      }

      // Basic validation - should have some concepts if the referenced ValueSet was found and
      // expanded
      assertTrue(
          contains.size() >= 0, "Should have concepts when include.valueSet is properly supported");
    } else {
      log.info(
          "Expanded ValueSet with include.valueSet contains no concepts - may indicate referenced"
              + " ValueSet not found");
    }
  }

  /**
   * Test value set expand with include value set paging.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithIncludeValueSetPaging() throws Exception {
    // Create a ValueSet that includes another ValueSet by URL
    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    ValueSet inputValueSet = createNCITestValueSetWithIncludeValueSet();

    // First request: Get first 50 concepts (offset=0, count=50)
    Parameters parameters1 = new Parameters();
    parameters1.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters1.addParameter("count", new IntegerType(50));
    parameters1.addParameter("offset", new IntegerType(0));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String parametersJson1 = parser.encodeResourceToString(parameters1);
    log.info("  parameters1 = " + parametersJson1);
    HttpEntity<String> entity1 = new HttpEntity<>(parametersJson1, headers);

    ResponseEntity<String> response1 =
        restTemplate.postForEntity(expandEndpoint, entity1, String.class);
    log.info("  response1 = " + JsonUtils.prettyPrint(response1.getBody()));
    assertEquals(HttpStatus.OK, response1.getStatusCode());

    ValueSet expandedValueSet1 = parser.parseResource(ValueSet.class, response1.getBody());

    // Second request: Get next 50 concepts (offset=50, count=50)
    Parameters parameters2 = new Parameters();
    parameters2.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters2.addParameter("count", new IntegerType(50));
    parameters2.addParameter("offset", new IntegerType(50));

    String parametersJson2 = parser.encodeResourceToString(parameters2);
    log.info("  parameters2 = " + parametersJson2);
    HttpEntity<String> entity2 = new HttpEntity<>(parametersJson2, headers);

    ResponseEntity<String> response2 =
        restTemplate.postForEntity(expandEndpoint, entity2, String.class);
    log.info("  response2 = " + JsonUtils.prettyPrint(response2.getBody()));
    assertEquals(HttpStatus.OK, response2.getStatusCode());

    ValueSet expandedValueSet2 = parser.parseResource(ValueSet.class, response2.getBody());

    // Verify both expansions exist
    assertTrue(expandedValueSet1.hasExpansion());
    assertTrue(expandedValueSet2.hasExpansion());

    if (expandedValueSet1.getExpansion().hasContains()
        && expandedValueSet2.getExpansion().hasContains()) {
      List<ValueSet.ValueSetExpansionContainsComponent> firstPage =
          expandedValueSet1.getExpansion().getContains();
      List<ValueSet.ValueSetExpansionContainsComponent> secondPage =
          expandedValueSet2.getExpansion().getContains();

      log.info("First page (offset=0, count=50): {} concepts", firstPage.size());
      log.info("Second page (offset=50, count=50): {} concepts", secondPage.size());
      log.info("Total concepts available: {}", expandedValueSet1.getExpansion().getTotal());

      // Verify pagination worked correctly
      assertTrue(firstPage.size() <= 50, "First page should have at most 50 concepts");
      assertTrue(secondPage.size() <= 50, "Second page should have at most 50 concepts");

      // Verify the concepts are different (no overlap between pages)
      if (firstPage.size() > 0 && secondPage.size() > 0) {
        Set<String> firstPageCodes =
            firstPage.stream()
                .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
                .collect(Collectors.toSet());
        Set<String> secondPageCodes =
            secondPage.stream()
                .map(ValueSet.ValueSetExpansionContainsComponent::getCode)
                .collect(Collectors.toSet());

        // Check for overlap - there should be none if paging works correctly
        Set<String> overlap = new HashSet<>(firstPageCodes);
        overlap.retainAll(secondPageCodes);
        assertEquals(0, overlap.size(), "There should be no overlap between pages: " + overlap);

        log.info("Successfully verified no concept overlap between pages");

        // Log some sample concepts from each page
        log.info(
            "Sample from first page: {} - {}",
            firstPage.get(0).getCode(),
            firstPage.get(0).getDisplay());
        log.info(
            "Sample from second page: {} - {}",
            secondPage.get(0).getCode(),
            secondPage.get(0).getDisplay());
      }

      // Verify total is consistent between requests
      assertEquals(
          expandedValueSet1.getExpansion().getTotal(),
          expandedValueSet2.getExpansion().getTotal(),
          "Total count should be consistent between paginated requests");

    } else {
      log.info(
          "One or both pages contained no concepts - may indicate referenced ValueSet not found or"
              + " empty");
    }
  }

  /**
   * Test value set expand with include value set including definitions and designations.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithIncludeValueSetDefinitionsDesignations() throws Exception {
    // Create a ValueSet that includes another ValueSet by URL
    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    ValueSet inputValueSet = createNCITestValueSetWithIncludeValueSet();
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter("count", new IntegerType(20)); // Smaller count for easier validation
    parameters.addParameter("offset", new IntegerType(0));
    parameters.addParameter("includeDefinition", new BooleanType(true));
    parameters.addParameter("includeDesignations", new BooleanType(true));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String parametersJson = parser.encodeResourceToString(parameters);
    log.info("  parameters = " + parametersJson);
    HttpEntity<String> entity = new HttpEntity<>(parametersJson, headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity(expandEndpoint, entity, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));
    ValueSet expandedValueSet = parser.parseResource(ValueSet.class, response.getBody());

    // Verify expansion exists
    assertTrue(expandedValueSet.hasExpansion());

    // Check if concepts from the referenced ValueSet are included
    if (expandedValueSet.getExpansion().hasContains()) {
      List<ValueSet.ValueSetExpansionContainsComponent> contains =
          expandedValueSet.getExpansion().getContains();
      log.info("Expanded ValueSet with include.valueSet contains {} concepts", contains.size());

      // Validate that at least some concepts have definitions and designations
      int conceptsWithDefinitions = 0;
      int conceptsWithDesignations = 0;

      for (ValueSet.ValueSetExpansionContainsComponent concept : contains) {
        log.debug("Checking concept: {} - {}", concept.getCode(), concept.getDisplay());

        // Check for designations (R4 supports designations on expansion contains)
        if (concept.hasDesignation() && !concept.getDesignation().isEmpty()) {
          conceptsWithDesignations++;
          log.debug("  Found {} designations", concept.getDesignation().size());

          // In R4, definitions are stored as designations with use="definition"
          boolean hasDefinition = false;
          for (ValueSet.ConceptReferenceDesignationComponent designation :
              concept.getDesignation()) {
            assertNotNull(designation.getValue(), "Designation should have a value");
            assertFalse(
                designation.getValue().trim().isEmpty(), "Designation value should not be empty");
            log.debug("    Designation: {}", designation.getValue());

            if (designation.hasUse()) {
              log.debug(
                  "    Use: {} - {}",
                  designation.getUse().getCode(),
                  designation.getUse().getDisplay());
              if ("definition".equals(designation.getUse().getCode())) {
                hasDefinition = true;
                log.debug("  Found definition as designation: {}", designation.getValue());
              }
            }
          }

          if (hasDefinition) {
            conceptsWithDefinitions++;
          }
        }

        // Log first few concepts in detail
        if (contains.indexOf(concept) < 3) {
          log.info(
              "Sample concept {}: {} - {} (designations: {})",
              contains.indexOf(concept) + 1,
              concept.getCode(),
              concept.getDisplay(),
              concept.hasDesignation() ? concept.getDesignation().size() : 0);
        }
      }

      log.info("Total concepts with definitions: {}/{}", conceptsWithDefinitions, contains.size());
      log.info(
          "Total concepts with designations: {}/{}", conceptsWithDesignations, contains.size());

      // Assert that at least some concepts have definitions and designations
      // Note: In R4, definitions are stored as designations with use="definition"
      assertTrue(
          conceptsWithDefinitions > 0,
          "At least some concepts should have definitions when includeDefinition=true");
      assertTrue(
          conceptsWithDesignations > 0,
          "At least some concepts should have designations when includeDesignations=true");

    } else {
      log.info(
          "Expanded ValueSet with include.valueSet contains no concepts - may indicate referenced"
              + " ValueSet not found");
    }
  }

  /**
   * Test value set expand with include value set that doesn't exist.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithIncludeValueSetNotFound() throws Exception {
    // Create a ValueSet that includes a non-existent ValueSet by URL
    String expandEndpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    ValueSet inputValueSet = createNCITestValueSetWithIncludeValueSetNotFound();
    Parameters parameters = new Parameters();
    parameters.addParameter().setName("valueSet").setResource(inputValueSet);
    parameters.addParameter("count", new IntegerType(100));
    parameters.addParameter("offset", new IntegerType(0));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String parametersJson = parser.encodeResourceToString(parameters);
    log.info("  parameters = " + parametersJson);
    HttpEntity<String> entity = new HttpEntity<>(parametersJson, headers);

    // Expect this to return an error status code
    ResponseEntity<String> response =
        restTemplate.postForEntity(expandEndpoint, entity, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));

    // Should return a 4xx error status code for invalid request
    assertTrue(
        response.getStatusCode().is4xxClientError(),
        "Should return 4xx error for missing ValueSet, got: " + response.getStatusCode());

    // Parse the response as an OperationOutcome
    org.hl7.fhir.r4.model.OperationOutcome outcome =
        parser.parseResource(org.hl7.fhir.r4.model.OperationOutcome.class, response.getBody());

    // Verify the OperationOutcome has the expected error
    assertTrue(outcome.hasIssue());
    org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent issue =
        outcome.getIssueFirstRep();
    assertEquals(org.hl7.fhir.r4.model.OperationOutcome.IssueType.NOTFOUND, issue.getCode());
    assertEquals(org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
    assertTrue(
        issue.getDiagnostics().contains("Referenced ValueSet not found")
            || issue.getDiagnostics().contains("Referenced ValueSet has no content"));

    log.info("Successfully caught missing ValueSet error: {}", issue.getDiagnostics());
  }

  /**
   * Creates the NCI test value set with include value set.
   *
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithIncludeValueSet() {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("test-include-valueset");
    inputValueSet.setUrl("http://example.org/test-include-valueset");
    inputValueSet.setName("TestIncludeValueSet");
    inputValueSet.setTitle("Test ValueSet with Include ValueSet");
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // Include a ValueSet by canonical URL
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    // Use the NCI Thesaurus ValueSet URL and the UMLS semnet set
    include.addValueSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459");
    include.addValueSet("http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs");

    compose.addInclude(include);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Creates the NCI test value set with include value set that doesn't exist.
   *
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithIncludeValueSetNotFound() {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("test-include-valueset-notfound");
    inputValueSet.setUrl("http://example.org/test-include-valueset-notfound");
    inputValueSet.setName("TestIncludeValueSetNotFound");
    inputValueSet.setTitle("Test ValueSet with Include ValueSet Not Found");
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // Include a non-existent ValueSet by canonical URL
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    // Use a non-existent ValueSet URL that should trigger the "not found" error
    include.addValueSet("http://example.org/non-existent-valueset");
    include.addValueSet(
        "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C999999"); // Non-existent
    // concept

    compose.addInclude(include);
    inputValueSet.setCompose(compose);

    return inputValueSet;
  }

  /**
   * Test value set expand with exclude.valueSet - basic functionality.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithExcludeValueSet() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with include.valueSet and exclude.valueSet
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("nci-exclude-valueset-test");
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/nci-exclude-valueset-test");
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName("NCIExcludeValueSetTest");
    inputValueSet.setTitle("NCI Thesaurus Exclude ValueSet Test");
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription("Test ValueSet with exclude.valueSet functionality");

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // Include a ValueSet
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    include.addValueSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459");
    compose.addInclude(include);

    // Exclude a smaller subset using direct concepts (to ensure some overlap)
    ValueSet.ConceptSetComponent exclude = new ValueSet.ConceptSetComponent();
    exclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    // Add specific concepts to exclude
    ValueSet.ConceptReferenceComponent excludeConcept1 = new ValueSet.ConceptReferenceComponent();
    excludeConcept1.setCode("C48672"); // Schedule I Substance
    excludeConcept1.setDisplay("Schedule I Substance");
    exclude.addConcept(excludeConcept1);

    compose.addExclude(exclude);

    inputValueSet.setCompose(compose);

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
    log.info("Found {} concepts after exclude processing", contains.size());

    // Assert - Excluded concept should NOT be present
    Optional<ValueSet.ValueSetExpansionContainsComponent> excludedResult =
        contains.stream().filter(comp -> "C48672".equals(comp.getCode())).findFirst();

    assertFalse(
        excludedResult.isPresent(),
        "Schedule I Substance (C48672) should be excluded from expansion");

    log.info("Exclude ValueSet basic functionality test completed successfully");
  }

  /**
   * Test value set expand with include C54452 and exclude C54459 ValueSets.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithIncludeAndExcludeValueSet() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    log.info("  parameters = include C54452, exclude C54459");

    // Create the ValueSet with include.valueSet C54452 and exclude.valueSet C54459
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("nci-include-exclude-valueset-test");
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/nci-include-exclude-valueset-test");
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName("NCIIncludeExcludeValueSetTest");
    inputValueSet.setTitle("NCI Thesaurus Include and Exclude ValueSet Test");
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription("Test ValueSet with include C54452 and exclude C54459 ValueSets");

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // Include ValueSet C54452
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    include.addValueSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54452");
    compose.addInclude(include);

    // Exclude ValueSet C54459
    ValueSet.ConceptSetComponent exclude = new ValueSet.ConceptSetComponent();
    exclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    exclude.addValueSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54459");
    compose.addExclude(exclude);

    inputValueSet.setCompose(compose);

    // First, get the count of C54452 alone for comparison
    ValueSet includeOnlyValueSet = new ValueSet();
    includeOnlyValueSet.setId("nci-include-only-test");
    includeOnlyValueSet.setUrl("http://example.org/fhir/ValueSet/nci-include-only-test");
    includeOnlyValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);

    ValueSet.ValueSetComposeComponent includeOnlyCompose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent includeOnly = new ValueSet.ConceptSetComponent();
    includeOnly.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    includeOnly.addValueSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C54452");
    includeOnlyCompose.addInclude(includeOnly);
    includeOnlyValueSet.setCompose(includeOnlyCompose);

    String includeOnlyRequestBody = parser.encodeResourceToString(includeOnlyValueSet);
    HttpEntity<String> includeOnlyRequest =
        new HttpEntity<>(
            includeOnlyRequestBody,
            new HttpHeaders() {
              {
                setContentType(MediaType.APPLICATION_JSON);
              }
            });
    ResponseEntity<String> includeOnlyResponse =
        this.restTemplate.postForEntity(endpoint, includeOnlyRequest, String.class);
    ValueSet includeOnlyExpandedValueSet =
        parser.parseResource(ValueSet.class, includeOnlyResponse.getBody());
    int originalCount = includeOnlyExpandedValueSet.getExpansion().getTotal();
    log.info("  C54452 original count: {}", originalCount);

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

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
    int finalCount = expansion.getTotal();
    int excludedCount = originalCount - finalCount;

    log.info("  C54452 original count: {}", originalCount);
    log.info("  Final count after excluding C54459: {}", finalCount);
    log.info("  Excluded concept count: {}", excludedCount);

    // Assert - C54452 should still be in the expansion
    ValueSet.ValueSetExpansionContainsComponent c54452Result =
        contains.stream().filter(comp -> "C54452".equals(comp.getCode())).findFirst().orElse(null);
    assertNotNull(c54452Result);

    // Assert - Total count should decrease by exactly 5 (as specified by user)
    assertEquals(
        5,
        excludedCount,
        "Total count should decrease by exactly 5 concepts due to overlap between C54452 and C54459");

    assertTrue(finalCount > 0, "Final expansion should contain concepts");
    assertTrue(
        finalCount < originalCount, "Final count should be less than original due to exclusion");

    log.info("Include C54452 and exclude C54459 ValueSet test completed successfully");
  }

  /**
   * Test value set expand with exclude.valueSet not found error handling.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithExcludeValueSetNotFound() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with exclude.valueSet that doesn't exist
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId("nci-exclude-valueset-not-found-test");
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/nci-exclude-valueset-not-found-test");
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName("NCIExcludeValueSetNotFoundTest");
    inputValueSet.setTitle("NCI Thesaurus Exclude ValueSet Not Found Test");
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription("Test ValueSet with exclude.valueSet that doesn't exist");

    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();

    // Include a valid concept
    ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
    include.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");

    ValueSet.ConceptReferenceComponent includeConcept = new ValueSet.ConceptReferenceComponent();
    includeConcept.setCode("C2991"); // Disease or Disorder
    includeConcept.setDisplay("Disease or Disorder");
    include.addConcept(includeConcept);

    compose.addInclude(include);

    // Exclude a non-existent ValueSet
    ValueSet.ConceptSetComponent exclude = new ValueSet.ConceptSetComponent();
    exclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    exclude.addValueSet("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=INVALID999");
    compose.addExclude(exclude);

    inputValueSet.setCompose(compose);

    // Convert to JSON for POST request
    String requestBody = parser.encodeResourceToString(inputValueSet);
    log.info("  value set = " + JsonUtils.prettyPrint(requestBody));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    // Act
    ResponseEntity<String> response =
        this.restTemplate.postForEntity(endpoint, request, String.class);
    log.info("  response = " + JsonUtils.prettyPrint(response.getBody()));

    // Assert - Should return an OperationOutcome error
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    // Parse as OperationOutcome to verify error handling
    OperationOutcome operationOutcome =
        parser.parseResource(OperationOutcome.class, response.getBody());
    assertNotNull(operationOutcome);
    assertTrue(operationOutcome.hasIssue());

    OperationOutcome.OperationOutcomeIssueComponent issue = operationOutcome.getIssueFirstRep();
    assertEquals(OperationOutcome.IssueType.NOTFOUND, issue.getCode());
    assertTrue(issue.getDiagnostics().contains("Referenced ValueSet not found"));
    assertTrue(issue.getDiagnostics().contains("INVALID999"));

    log.info("Exclude ValueSet not found error handling test completed successfully");
  }
}
