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
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
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
   * Helper method to create ValueSet with specific NCIt version.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param description the description
   * @return the value set
   */
  private ValueSet createNCITestValueSetWithVersion(
      String id, String name, String title, String description) {
    ValueSet inputValueSet = new ValueSet();
    inputValueSet.setId(id);
    inputValueSet.setUrl("http://example.org/fhir/ValueSet/" + id);
    inputValueSet.setVersion("1.0.0");
    inputValueSet.setName(name);
    inputValueSet.setTitle(title);
    inputValueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);
    inputValueSet.setDescription(description);

    // Build compose definition with NCI Thesaurus concepts using specific version
    ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
    ValueSet.ConceptSetComponent nciInclude = new ValueSet.ConceptSetComponent();
    nciInclude.setSystem("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    nciInclude.setVersion("24.03d"); // Specific NCIt version

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

    compose.addInclude(nciInclude);
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
   * Test value set expand with NCI thesaurus specific version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandWithNCIThesaurusSpecificVersion() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;

    // Create the ValueSet with specific NCIt version
    ValueSet inputValueSet =
        createNCITestValueSetWithVersion(
            "nci-version-test",
            "NCIVersionTest",
            "NCI Thesaurus Version Test",
            "Test ValueSet with specific NCIt version 24.03d");

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

    // Assert - Valid active concepts should be included
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

    // Assert - Check for version-specific behavior with inactive concept
    Optional<ValueSet.ValueSetExpansionContainsComponent> inactiveResult =
        contains.stream()
            .filter(
                comp ->
                    "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(comp.getSystem()))
            .filter(comp -> "C176707".equals(comp.getCode()))
            .findFirst();

    log.info(
        "  Inactive concept C176707 present in version-specific expansion: {}",
        inactiveResult.isPresent());

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

      log.debug("    Version 24.03d concept: {} - {}", concept.getCode(), concept.getDisplay());
    }

    // Assert - Should have the expected number of valid concepts
    assertTrue(expansion.getTotal() >= 2, "Should have at least 2 valid concepts");
    assertTrue(
        expansion.getTotal() <= 3,
        "Should have at most 3 concepts (depending on inactive handling)");

    // Assert - Check if expansion contains version information
    if (expansion.hasParameter()) {
      for (ValueSet.ValueSetExpansionParameterComponent param : expansion.getParameter()) {
        log.info("  Expansion parameter: {} = {}", param.getName(), param.getValue());

        // Check for version-related parameters
        if ("version".equals(param.getName()) || "system-version".equals(param.getName())) {
          String versionValue = param.getValue().toString();
          log.info("  Found version parameter: {}", versionValue);
          // Note: The exact version format may vary depending on implementation
        }
      }
    }

    log.info(
        "  NCI Thesaurus version-specific expansion completed with {} concepts",
        expansion.getTotal());

    // Note: This test validates that version-specific expansion works
    // The exact behavior may depend on whether version 24.03d is available
    // and how the implementation handles version-specific requests
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

    // Create the ValueSet with property '=' filter
    ValueSet inputValueSet =
        createNCITestValueSetWithPropertyEqualsFilter(
            "nci-property-equals-filter-test",
            "NCIPropertyEqualsFilterTest",
            "NCI Thesaurus Property Equals Filter Test",
            "Test ValueSet with '=' filter for Contributing_Source property");

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
    log.info("Found {} concepts in expansion", contains.size());

    // Assert - Should only contain concepts with Contributing_Source = "FDA"
    // We expect C48672 (Schedule I Substance) to be included since it has Contributing_Source = "FDA"
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
        "Disease or Disorder (C2991) should NOT be included as it does not have Contributing_Source = 'FDA'");
    log.info("C2991 (Disease or Disorder) correctly excluded - does not have Contributing_Source = 'FDA'");

    // Assert - Should have at least one concept with the matching property
    assertTrue(
        expansion.getTotal() >= 1,
        "Should find at least one concept with Contributing_Source = 'FDA', found: " + expansion.getTotal());
        
    log.info("Property equals filter test completed successfully with {} matching concepts", expansion.getTotal());
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
        "Controlled Substance (C48670) should NOT be included as it does not have Contributing_Source property");
    log.info("C48670 (Controlled Substance) correctly excluded - does not have Contributing_Source property");

    // Assert - Should have at least one concept with the property
    assertTrue(
        expansion.getTotal() >= 1,
        "Should find at least one concept with Contributing_Source property, found: " + expansion.getTotal());
        
    log.info("Property exists filter (value=true) test completed successfully with {} matching concepts", expansion.getTotal());
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
    log.info("Found {} concepts without Contributing_Source property", contains.size());

    // Assert - Should contain concepts that do NOT have Contributing_Source property
    // C48670 (Controlled Substance) should be included as it does not have Contributing_Source property
    Optional<ValueSet.ValueSetExpansionContainsComponent> controlledResult =
        contains.stream().filter(comp -> "C48670".equals(comp.getCode())).findFirst();

    assertTrue(
        controlledResult.isPresent(),
        "Controlled Substance (C48670) should be included as it does not have Contributing_Source property");
    log.info("C48670 (Controlled Substance) correctly included - does not have Contributing_Source property");

    // Assert - Should NOT contain concepts that HAVE Contributing_Source property
    // C48672 (Schedule I Substance) should not be included as it has Contributing_Source = "FDA"
    Optional<ValueSet.ValueSetExpansionContainsComponent> scheduleIResult =
        contains.stream().filter(comp -> "C48672".equals(comp.getCode())).findFirst();

    assertFalse(
        scheduleIResult.isPresent(),
        "Schedule I Substance (C48672) should NOT be included as it has Contributing_Source property");
    log.info("C48672 (Schedule I Substance) correctly excluded - has Contributing_Source property");

    // C2991 (Disease or Disorder) should not be included as it has Contributing_Source properties
    Optional<ValueSet.ValueSetExpansionContainsComponent> diseaseResult =
        contains.stream().filter(comp -> "C2991".equals(comp.getCode())).findFirst();

    assertFalse(
        diseaseResult.isPresent(),
        "Disease or Disorder (C2991) should NOT be included as it has Contributing_Source property");
    log.info("C2991 (Disease or Disorder) correctly excluded - has Contributing_Source property");

    // Assert - Should have at least one concept without the property
    assertTrue(
        expansion.getTotal() >= 1,
        "Should find at least one concept without Contributing_Source property, found: " + expansion.getTotal());
        
    log.info("Property exists filter (value=false) test completed successfully with {} matching concepts", expansion.getTotal());
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
   * @param shouldExist true to filter for concepts that HAVE the property, false to filter for concepts that do NOT have the property
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

    // C2991 (Disease or Disorder) - HAS Contributing_Source = "CDISC-GLOSS", "CTDC", "CTRP", "GDC", "NICHD"
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
}
