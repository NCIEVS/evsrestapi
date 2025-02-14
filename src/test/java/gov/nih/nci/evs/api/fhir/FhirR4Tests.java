package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ValueSet;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * Class tests for FhirR4Tests. Tests the functionality of the FHIR R4
 * endpoints, CodeSystem, ValueSet, and ConceptMap. All passed ids MUST be
 * lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR4Tests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4Tests.class);

  /** The port. */
  @LocalServerPort
  private int port;

  /** The rest template. */
  @Autowired
  private TestRestTemplate restTemplate;

  /** The test properties. */
  @Autowired
  TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** local host prefix. */
  private final String localHost = "http://localhost:";

  /** Fhir url paths. */
  private final String fhirCSPath = "/fhir/r4/CodeSystem";

  /** The fhir VS path. */
  private final String fhirVSPath = "/fhir/r4/ValueSet";

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
   * Test code system search.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSearch() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Verify things about this one
    // {"resourceType":"CodeSystem","id":"ncit_21.06e","url":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","version":"21.06e","name":"NCI
    // Thesaurus
    // 21.06e","title":"ncit","status":"active","experimental":false,"publisher":"NCI","hierarchyMeaning":"is-a"}
    final Set<String> ids = new HashSet<>(Set.of("ncit_21.06e"));
    final Set<String> urls =
        new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"));

    // Assert
    assertFalse(codeSystems.isEmpty());
    for (Resource cs : codeSystems) {
      log.info("  code system = " + parser.encodeResourceToString(cs));
      CodeSystem css = (CodeSystem) cs;
      assertNotNull(css);
      assertEquals(ResourceType.CodeSystem, css.getResourceType());
      assertNotNull(css.getIdPart());
      assertNotNull(css.getPublisher());
      assertNotNull(css.getUrl());
      ids.remove(css.getIdPart());
      urls.remove(css.getUrl());
    }
    assertThat(ids).isEmpty();
    assertThat(urls).isEmpty();
  }

  /**
   * Test code system search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSearchWithParameters() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint)// .queryParam("date",
                                                                               // "ge2021-06")
        .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e").queryParam("title", "ncit");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, true); // Expecting results

    // Test 2: Invalid date
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("date", "ge2025-01") // Future
                                                                                           // date
        .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e").queryParam("title", "ncit");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false); // Expecting no results

    // Test 3: Invalid system
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("date", "ge2021-06")
        .queryParam("system", "http://invalid.system.url").queryParam("version", "21.06e")
        .queryParam("title", "ncit");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 4: Invalid version
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("date", "ge2021-06")
        .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "invalid_version").queryParam("title", "ncit");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);

    // Test 5: Invalid title
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("date", "ge2021-06")
        .queryParam("system", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e").queryParam("title", "invalid_title");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateCodeSystemResults(data, false);
  }

  /**
   * Validate code system results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  private void validateCodeSystemResults(Bundle data, boolean expectResults) {
    List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(codeSystems.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ncit_21.06e"));
      final Set<String> urls =
          new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"));

      for (Resource cs : codeSystems) {
        log.info(" code system = " + parser.encodeResourceToString(cs));
        CodeSystem css = (CodeSystem) cs;
        assertNotNull(css);
        assertEquals(ResourceType.CodeSystem, css.getResourceType());
        assertNotNull(css.getIdPart());
        assertNotNull(css.getPublisher());
        assertNotNull(css.getUrl());
        ids.remove(css.getIdPart());
        urls.remove(css.getUrl());
      }
      assertThat(ids).isEmpty();
      assertThat(urls).isEmpty();
    } else {
      assertTrue(data.getEntry().isEmpty() || codeSystems.isEmpty());
    }
  }

  /**
   * Test code system read.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemRead() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();

    // reassign content with the firstCodeSystemId
    content = this.restTemplate.getForObject(endpoint + "/" + firstCodeSystemId, String.class);
    CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert
    assertNotNull(codeSystem);
    assertEquals(ResourceType.CodeSystem, codeSystem.getResourceType());
    assertEquals(firstCodeSystemId, codeSystem.getIdPart());
    assertEquals(((CodeSystem) codeSystems.get(0)).getUrl(), codeSystem.getUrl());
    assertEquals(((CodeSystem) codeSystems.get(0)).getName(), codeSystem.getName());
    assertEquals(((CodeSystem) codeSystems.get(0)).getPublisher(), codeSystem.getPublisher());
  }

  /**
   * Test code system read static id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemReadStaticId() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;
    String codeSystemId = "umlssemnet_2023aa";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + codeSystemId, String.class);
    CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

    // Assert
    assertNotNull(codeSystem);
    assertEquals(ResourceType.CodeSystem, codeSystem.getResourceType());
    assertEquals(codeSystemId, codeSystem.getIdPart());
    assertEquals("http://www.nlm.nih.gov/research/umls/umlssemnet.owl", codeSystem.getUrl());
    assertEquals("UMLS Semantic Network 2023AA", codeSystem.getName());
    assertEquals("National Library of Medicine", codeSystem.getPublisher());
    assertEquals("umlssemnet", codeSystem.getTitle());
    assertEquals("active", codeSystem.getStatus().toCode());
    assertEquals("is-a", codeSystem.getHierarchyMeaning().toCode());
    assertEquals("CodeSystem", codeSystem.getResourceType().toString());
    assertEquals(false, codeSystem.getExperimental());
    assertEquals("2023AA", codeSystem.getVersion());
  }

  /**
   * Test code system read bad id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemReadBadId() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirCSPath;
    String invalidId = "invalid_id";
    String messageNotFound = "Code system not found = " + invalidId;
    String errorCode = "not-found";

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + invalidId, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));

  }

  /**
   * Test value set search.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearch() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirVSPath + "?_count=2000";

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    final Set<String> ids = new HashSet<>(Set.of("ncit_21.06e", "ncit_c61410"));
    final Set<String> urls =
        new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs",
            "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C61410"));

    // Assert
    assertFalse(valueSets.isEmpty());
    for (Resource vs : valueSets) {
      log.info("  value set = " + parser.encodeResourceToString(vs));
      ValueSet vss = (ValueSet) vs;
      assertNotNull(vss);
      assertEquals(ResourceType.ValueSet, vss.getResourceType());
      assertNotNull(vss.getIdPart());
      assertNotNull(vss.getPublisher());
      assertNotNull(vss.getUrl());
      ids.remove(vss.getIdPart());
      urls.remove(vss.getUrl());
    }
    assertTrue(ids.isEmpty());
    assertTrue(urls.isEmpty());
  }

  /**
   * Test value set read.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetRead() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Act
    String firstValueSetId = valueSets.get(0).getIdPart();
    // reassign content
    content = this.restTemplate.getForObject(endpoint + "/" + firstValueSetId, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertEquals(ResourceType.ValueSet, valueSet.getResourceType());
    assertEquals(firstValueSetId, valueSet.getIdPart());
    assertEquals(valueSet.getUrl(), ((ValueSet) valueSets.get(0)).getUrl());
    assertEquals(valueSet.getName(), ((ValueSet) valueSets.get(0)).getName());
    assertEquals(valueSet.getPublisher(), ((ValueSet) valueSets.get(0)).getPublisher());
  }

  /**
   * Test value set read static id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetReadStaticId() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    String valueSetId = "umlssemnet_2023aa";

    content = this.restTemplate.getForObject(endpoint + "/" + valueSetId, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertEquals(ResourceType.ValueSet, valueSet.getResourceType());
    assertEquals(valueSetId, valueSet.getIdPart());
    assertEquals("http://www.nlm.nih.gov/research/umls/umlssemnet.owl?fhir_vs", valueSet.getUrl());
    assertEquals("UMLS Semantic Network 2023AA", valueSet.getName());
    assertEquals("National Library of Medicine", valueSet.getPublisher());
    assertEquals("umlssemnet", valueSet.getTitle());
    assertEquals("active", valueSet.getStatus().toCode());
    assertEquals(false, valueSet.getExperimental());
    assertEquals("2023AA", valueSet.getVersion());
    assertEquals("UMLS Semantic Network", valueSet.getDescription());
  }

  /**
   * Test value set read bad id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetReadBadId() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    String invalidId = "invalid_id";
    String messageNotFound = "Value set not found = " + invalidId;
    String errorCode = "not-found";

    content = this.restTemplate.getForObject(endpoint + "/" + invalidId, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test value set read value set from code.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetReadCode() throws Exception {
    // Arrange
    String content;
    String code = "ncit_c129091";
    String name = "CDISC Questionnaire NCCN-FACT FBLSI-18 Version 2 Test Name Terminology";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C129091";
    String publisher = "NCI";
    String endpoint = localHost + port + fhirVSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint + "/" + code, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // Assert
    assertNotNull(valueSet);
    assertEquals(ResourceType.ValueSet, valueSet.getResourceType());
    assertEquals(code, valueSet.getIdPart());
    assertEquals(name, valueSet.getName());
    assertEquals(publisher, valueSet.getPublisher());
    assertEquals(url, valueSet.getUrl());
  }

  /**
   * Test concept map search.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapSearch() throws Exception {
    // Arrange
    String content = this.restTemplate.getForObject(localHost + port + fhirCMPath, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // Act
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Verify things about this one
    // {"resourceType":"ConceptMap","id":"ma_to_ncit_mapping_november2011","url":"http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping","version":"Aug2024","name":"NCIt_to_HGNC_Mapping","title":"NCIt_to_HGNC_Mapping","status":"active","experimental":false,"publisher":"NCI","group":[{"source":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","target":"http://www.genenames.org"}]}
    final Set<String> ids = new HashSet<>(Set.of("ma_to_ncit_mapping_november2011"));
    final Set<String> urls =
        new HashSet<>(Set.of("http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping"));

    // Assert
    assertFalse(conceptMaps.isEmpty());
    for (Resource cm : conceptMaps) {
      log.info("  concept map = " + parser.encodeResourceToString(cm));
      ConceptMap cmm = (ConceptMap) cm;
      assertNotNull(cmm);
      assertEquals(ResourceType.ConceptMap, cmm.getResourceType());
      assertNotNull(cmm.getIdPart());
      assertNotNull(cmm.getGroup());
      assertNotNull(cmm.getVersion());
      assertNotNull(cmm.getUrl());
      ids.remove(cmm.getIdPart());
      urls.remove(cmm.getUrl());
    }
    assertThat(ids).isEmpty();
    assertThat(urls).isEmpty();
  }

  /**
   * Test concept map read.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapRead() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCMPath;

    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Act
    String firstConceptMapId = conceptMaps.get(0).getIdPart();
    // reassign content
    content = this.restTemplate.getForObject(endpoint + "/" + firstConceptMapId, String.class);
    ConceptMap conceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert
    assertNotNull(conceptMap);
    assertEquals(ResourceType.ConceptMap, conceptMap.getResourceType());
    assertEquals(firstConceptMapId, conceptMap.getIdPart());
    assertEquals(conceptMap.getUrl(), ((ConceptMap) conceptMaps.get(0)).getUrl());
    assertEquals(conceptMap.getName(), ((ConceptMap) conceptMaps.get(0)).getName());
    assertEquals(conceptMap.getVersion(), ((ConceptMap) conceptMaps.get(0)).getVersion());
  }

  /**
   * Test concept map read static id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapReadStaticId() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCMPath;
    String conceptMapId = "icd10_to_meddra_mapping_july2023";

    // Act
    String content = this.restTemplate.getForObject(endpoint + "/" + conceptMapId, String.class);
    ConceptMap conceptMap = parser.parseResource(ConceptMap.class, content);

    // Assert
    assertNotNull(conceptMap);
    assertEquals(ResourceType.ConceptMap, conceptMap.getResourceType());
    assertEquals(conceptMapId, conceptMap.getIdPart());
    assertEquals(conceptMap.getUrl(),
        "http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping");
    assertEquals(conceptMap.getName(), "ICD10_to_MedDRA_Mapping");
    assertEquals(conceptMap.getVersion(), "July2023");
    assertEquals(conceptMap.getPublisher(), "World Health Organization");
    assertEquals(conceptMap.getStatus().toCode(), "active");
    assertEquals(conceptMap.getExperimental(), false);
    assertEquals(conceptMap.getSourceUriType().asStringValue(), "http://hl7.org/fhir/sid/icd-10");
    assertEquals(conceptMap.getTargetUriType().asStringValue(), "https://www.meddra.org");
    assertEquals(conceptMap.getTitle(), "ICD10_to_MedDRA_Mapping");
  }

  /**
   * Test concept map read bad id.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapReadBadId() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCMPath;
    String invalidId = "invalid_id";
    String messageNotFound = "Concept map not found = " + invalidId;
    String errorCode = "not-found";

    // Act
    String content = this.restTemplate.getForObject(endpoint + "/" + invalidId, String.class);
    OperationOutcome outcome = parser.parseResource(OperationOutcome.class, content);
    OperationOutcomeIssueComponent component = outcome.getIssueFirstRep();

    // Assert
    assertEquals(errorCode, component.getCode().toCode());
    assertEquals(messageNotFound, (component.getDiagnostics()));
  }

  /**
   * Test value set subset search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetSubsetSearchWithParameters() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
            .queryParam("_id", "ncit_c100110").queryParam("code", "C100110")
            .queryParam("name", "CDISC Questionnaire Terminology").queryParam("system", "ncit")
            .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
            .queryParam("version", "21.06e");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, true); // Expecting results

    // Test 2: Invalid ID
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "invalid_id").queryParam("code", "C100110")
        .queryParam("name", "CDISC Questionnaire Terminology").queryParam("system", "ncit")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
        .queryParam("version", "21.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 3: Invalid code
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "ncit_21.06e").queryParam("code", "INVALID_CODE")
        .queryParam("name", "CDISC Questionnaire Terminology").queryParam("system", "ncit")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
        .queryParam("version", "21.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 4: Invalid name
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "ncit_21.06e")// .queryParam("code", "C61410")
        .queryParam("name", "Invalid Name").queryParam("system", "ncit")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
        .queryParam("version", "21.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 5: Invalid system
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "ncit_21.06e").queryParam("code", "C100110")
        .queryParam("name", "CDISC Questionnaire Terminology")
        .queryParam("system", "invalid_system")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
        .queryParam("version", "21.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 6: Invalid URL
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "ncit_21.06e").queryParam("code", "C100110")
        .queryParam("name", "CDISC Questionnaire Terminology").queryParam("system", "ncit")
        .queryParam("url", "http://invalid.url").queryParam("version", "21.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);

    // Test 7: Invalid version
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "ncit_21.06e").queryParam("code", "C100110")
        .queryParam("name", "CDISC Questionnaire Terminology").queryParam("system", "ncit")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110")
        .queryParam("version", "invalid_version");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetSubsetResults(data, false);
  }

  /**
   * Test value set search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetSearchWithParameters() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_count", "2000").queryParam("_id", "ncit_21.06e")// .queryParam("code",
                                                                      // "C61410")
        .queryParam("name", "NCI Thesaurus 21.06e").queryParam("system", "ncit")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs")
        .queryParam("version", "21.06e");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, true); // Expecting results

    // Test 2: Invalid ID
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "invalid_id")// .queryParam("code", "C61410")
        .queryParam("name", "NCI Thesaurus 21.06e").queryParam("system", "ncit")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 3: Invalid code
    // builder =
    // UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
    // .queryParam("_id", "ncit_21.06e").queryParam("code", "INVALID_CODE")
    // .queryParam("name", "NCI Thesaurus 21.06e").queryParam("system", "ncit")
    // .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
    // .queryParam("version", "21.06e");
    //
    // content =
    // this.restTemplate.getForObject(builder.build().encode().toUri(),
    // String.class);
    // data = parser.parseResource(Bundle.class, content);
    // validateValueSetResults(data, false);

    // Test 4: Invalid name
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "ncit_21.06e")// .queryParam("code", "C61410")
        .queryParam("name", "Invalid Name").queryParam("system", "ncit")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 5: Invalid system
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "ncit_21.06e")// .queryParam("code", "C61410")
        .queryParam("name", "NCI Thesaurus 21.06e").queryParam("system", "invalid_system")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "21.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 6: Invalid URL
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "ncit_21.06e")// .queryParam("code", "C61410")
        .queryParam("name", "NCI Thesaurus 21.06e").queryParam("system", "ncit")
        .queryParam("url", "http://invalid.url").queryParam("version", "21.06e");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);

    // Test 7: Invalid version
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2000")
        .queryParam("_id", "ncit_21.06e")// .queryParam("code", "C61410")
        .queryParam("name", "NCI Thesaurus 21.06e").queryParam("system", "ncit")
        .queryParam("url", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl")
        .queryParam("version", "invalid_version");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateValueSetResults(data, false);
  }

  /**
   * Validate value set results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  // Helper method to validate results
  private void validateValueSetResults(Bundle data, boolean expectResults) {
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(valueSets.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ncit_21.06e"));
      final Set<String> urls =
          new HashSet<>(Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs"));

      for (Resource vs : valueSets) {
        log.info(" value set = " + parser.encodeResourceToString(vs));
        ValueSet vss = (ValueSet) vs;
        assertNotNull(vss);
        assertEquals(ResourceType.ValueSet, vss.getResourceType());
        assertNotNull(vss.getIdPart());
        assertNotNull(vss.getPublisher());
        assertNotNull(vss.getUrl());
        ids.remove(vss.getIdPart());
        urls.remove(vss.getUrl());
      }
      assertTrue(ids.isEmpty());
      assertTrue(urls.isEmpty());
    } else {
      assertTrue(data.getEntry().isEmpty() || valueSets.isEmpty());
    }
  }

  /**
   * Validate value set subset results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  private void validateValueSetSubsetResults(Bundle data, boolean expectResults) {
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(valueSets.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ncit_c100110"));
      final Set<String> urls = new HashSet<>(
          Set.of("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110"));

      for (Resource vs : valueSets) {
        log.info(" value set = " + parser.encodeResourceToString(vs));
        ValueSet vss = (ValueSet) vs;
        assertNotNull(vss);
        assertEquals(ResourceType.ValueSet, vss.getResourceType());
        assertNotNull(vss.getIdPart());
        assertNotNull(vss.getPublisher());
        assertNotNull(vss.getUrl());
        ids.remove(vss.getIdPart());
        urls.remove(vss.getUrl());
      }
      assertTrue(ids.isEmpty());
      assertTrue(urls.isEmpty());
    } else {
      assertTrue(data.getEntry().isEmpty() || valueSets.isEmpty());
    }
  }

  /**
   * Test concept map search with parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapSearchWithParameters() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCMPath;

    // Test 1: All valid parameters
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_id", "ma_to_ncit_mapping_november2011")// .queryParam("date",
                                                             // "2024-08")
        .queryParam("system", "MA_to_NCIt_Mapping")
        .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
        .queryParam("version", "November2011");

    // Test successful case with all parameters
    String content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, true); // Expecting results

    // Test 2: Invalid ID
    builder = UriComponentsBuilder.fromUriString(endpoint).queryParam("_id", "invalid_id")
        // .queryParam("date", "2024-08")
        .queryParam("system", "MA_to_NCIt_Mapping")
        .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
        .queryParam("version", "November2011");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 3: Invalid date
    builder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_id", "ma_to_ncit_mapping_november2011").queryParam("date", "2028-08")
        .queryParam("system", "MA_to_NCIt_Mapping")
        .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
        .queryParam("version", "November2011");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 4: Invalid system
    builder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_id", "ma_to_ncit_mapping_november2011")
        // .queryParam("date", "2024-08")
        .queryParam("system", "Invalid_System")
        .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
        .queryParam("version", "November2011");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 5: Invalid URL
    builder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_id", "ma_to_ncit_mapping_november2011")
        // .queryParam("date", "2024-08")
        .queryParam("system", "MA_to_NCIt_Mapping").queryParam("url", "http://invalid.url")
        .queryParam("version", "November2011");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);

    // Test 6: Invalid version
    builder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_id", "ma_to_ncit_mapping_november2011")
        // .queryParam("date", "2024-08")
        .queryParam("system", "MA_to_NCIt_Mapping")
        .queryParam("url", "http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping")
        .queryParam("version", "invalid_version");

    content = this.restTemplate.getForObject(builder.build().encode().toUri(), String.class);
    data = parser.parseResource(Bundle.class, content);
    validateConceptMapResults(data, false);
  }

  /**
   * Validate concept map results.
   *
   * @param data the data
   * @param expectResults the expect results
   */
  // Helper method to validate results
  private void validateConceptMapResults(Bundle data, boolean expectResults) {
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    if (expectResults) {
      assertFalse(conceptMaps.isEmpty());
      final Set<String> ids = new HashSet<>(Set.of("ma_to_ncit_mapping_november2011"));
      final Set<String> urls = new HashSet<>(
          Set.of("http://purl.obolibrary.org/obo/emap.owl?fhir_cm=MA_to_NCIt_Mapping"));

      for (Resource cm : conceptMaps) {
        log.info(" concept map = " + parser.encodeResourceToString(cm));
        ConceptMap cmm = (ConceptMap) cm;
        assertNotNull(cmm);
        assertEquals(ResourceType.ConceptMap, cmm.getResourceType());
        assertNotNull(cmm.getIdPart());
        assertNotNull(cmm.getGroup());
        assertNotNull(cmm.getVersion());
        assertNotNull(cmm.getUrl());
        ids.remove(cmm.getIdPart());
        urls.remove(cmm.getUrl());
      }
      assertThat(ids).isEmpty();
      assertThat(urls).isEmpty();
    } else {
      assertTrue(data.getEntry().isEmpty() || conceptMaps.isEmpty());
    }
  }

  /**
   * Test code system search pagination.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemSearchPagination() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCSPath;

    // Test 1: Get default results without pagination
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle defaultData = parser.parseResource(Bundle.class, content);
    List<Resource> defaultCodeSystems =
        defaultData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 2: Get first page (count=2)
    UriComponentsBuilder firstPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2");

    content =
        this.restTemplate.getForObject(firstPageBuilder.build().encode().toUri(), String.class);
    Bundle firstPageData = parser.parseResource(Bundle.class, content);
    List<Resource> firstPageSystems =
        firstPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 3: Get second page (count=2, offset=2)
    UriComponentsBuilder secondPageBuilder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_count", "2").queryParam("_offset", "2");

    content =
        this.restTemplate.getForObject(secondPageBuilder.build().encode().toUri(), String.class);
    Bundle secondPageData = parser.parseResource(Bundle.class, content);
    List<Resource> secondPageSystems =
        secondPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 4: Try to exceed maximum count (should return all)
    UriComponentsBuilder maxExceededBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "10000");

    content =
        this.restTemplate.getForObject(maxExceededBuilder.build().encode().toUri(), String.class);
    Bundle maxPageData = parser.parseResource(Bundle.class, content);
    List<Resource> maxPageSystems =
        maxPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Assertions for pagination
    assertEquals(2, firstPageSystems.size());
    assertEquals(2, secondPageSystems.size());
    assertEquals(4, firstPageSystems.size() + secondPageSystems.size());
    assertTrue(defaultCodeSystems.size() <= maxPageSystems.size());

    // Verify that concatenated pages equal first 4 of full results
    List<String> fourIds = defaultCodeSystems.subList(0, 4).stream()
        .map(resource -> ((CodeSystem) resource).getIdPart()).sorted().toList();

    List<String> paginatedIds = Stream.concat(firstPageSystems.stream(), secondPageSystems.stream())
        .map(resource -> ((CodeSystem) resource).getIdPart()).sorted().toList();

    assertEquals(fourIds, paginatedIds);

    // Verify content of individual resources
    for (Resource cs : defaultCodeSystems) {
      validateCodeSystemPagination((CodeSystem) cs);
    }
  }

  /**
   * Validate code system pagination.
   *
   * @param css the css
   */
  private void validateCodeSystemPagination(CodeSystem css) {
    assertNotNull(css);
    assertEquals(ResourceType.CodeSystem, css.getResourceType());
    assertNotNull(css.getIdPart());
    assertNotNull(css.getPublisher());
    assertNotNull(css.getUrl());

    // Log for debugging
    log.info(" code system = " + parser.encodeResourceToString(css));

    // Verify specific IDs and URLs if needed
    if (css.getIdPart().equals("ncit_21.06e")) {
      assertEquals("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl", css.getUrl());
    }
  }

  /**
   * Test value set search pagination.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetSearchPagination() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirVSPath;

    // Test 1: Get default results without pagination
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle defaultData = parser.parseResource(Bundle.class, content);
    List<Resource> defaultValueSets =
        defaultData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 2: Get first page (count=2)
    UriComponentsBuilder firstPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2");

    content =
        this.restTemplate.getForObject(firstPageBuilder.build().encode().toUri(), String.class);
    Bundle firstPageData = parser.parseResource(Bundle.class, content);
    List<Resource> firstPageSets =
        firstPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 3: Get second page (count=2, offset=2)
    UriComponentsBuilder secondPageBuilder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_count", "2").queryParam("_offset", "2");

    content =
        this.restTemplate.getForObject(secondPageBuilder.build().encode().toUri(), String.class);
    Bundle secondPageData = parser.parseResource(Bundle.class, content);
    List<Resource> secondPageSets =
        secondPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 4: Try to exceed maximum count (should return all)
    UriComponentsBuilder maxExceededBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "10000");

    content =
        this.restTemplate.getForObject(maxExceededBuilder.build().encode().toUri(), String.class);
    Bundle maxPageData = parser.parseResource(Bundle.class, content);
    List<Resource> maxPageSets =
        maxPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Assertions for pagination
    assertEquals(2, firstPageSets.size());
    assertEquals(2, secondPageSets.size());
    assertEquals(4, firstPageSets.size() + secondPageSets.size());
    assertTrue(defaultValueSets.size() <= maxPageSets.size());

    // Verify that concatenated pages equal first 4 of full results
    List<String> fourIds = defaultValueSets.subList(0, 4).stream()
        .map(resource -> ((ValueSet) resource).getIdPart()).sorted().toList();

    List<String> paginatedIds = Stream.concat(firstPageSets.stream(), secondPageSets.stream())
        .map(resource -> ((ValueSet) resource).getIdPart()).sorted().toList();

    assertEquals(fourIds, paginatedIds);

    // Verify content of individual resources
    for (Resource cs : defaultValueSets) {
      validateValueSetPagination((ValueSet) cs);
    }
  }

  /**
   * Validate value set pagination.
   *
   * @param vs the vs
   */
  private void validateValueSetPagination(ValueSet vs) {
    assertNotNull(vs);
    assertEquals(ResourceType.ValueSet, vs.getResourceType());
    assertNotNull(vs.getIdPart());
    assertNotNull(vs.getPublisher());
    assertNotNull(vs.getUrl());

    // Log for debugging
    log.info(" value set = " + parser.encodeResourceToString(vs));

    // Verify specific IDs and URLs if needed
    if (vs.getIdPart().equals("ncit_21.06e")) {
      assertEquals("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs", vs.getUrl());
    }
  }

  /**
   * Test concept map search pagination.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapSearchPagination() throws Exception {
    // Arrange
    String endpoint = localHost + port + fhirCMPath;

    // Test 1: Get default results without pagination
    String content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle defaultData = parser.parseResource(Bundle.class, content);
    List<Resource> defaultConceptMaps =
        defaultData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 2: Get first page (count=2)
    UriComponentsBuilder firstPageBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "2");

    content =
        this.restTemplate.getForObject(firstPageBuilder.build().encode().toUri(), String.class);
    Bundle firstPageData = parser.parseResource(Bundle.class, content);
    List<Resource> firstPageMaps =
        firstPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 3: Get second page (count=2, offset=2)
    UriComponentsBuilder secondPageBuilder = UriComponentsBuilder.fromUriString(endpoint)
        .queryParam("_count", "2").queryParam("_offset", "2");

    content =
        this.restTemplate.getForObject(secondPageBuilder.build().encode().toUri(), String.class);
    Bundle secondPageData = parser.parseResource(Bundle.class, content);
    List<Resource> secondPageMaps =
        secondPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Test 4: Try to exceed maximum count (should return all)
    UriComponentsBuilder maxExceededBuilder =
        UriComponentsBuilder.fromUriString(endpoint).queryParam("_count", "10000");

    content =
        this.restTemplate.getForObject(maxExceededBuilder.build().encode().toUri(), String.class);
    Bundle maxPageData = parser.parseResource(Bundle.class, content);
    List<Resource> maxPageMaps =
        maxPageData.getEntry().stream().map(BundleEntryComponent::getResource).toList();

    // Assertions for pagination
    assertEquals(2, firstPageMaps.size());
    assertEquals(2, secondPageMaps.size());
    assertEquals(4, firstPageMaps.size() + secondPageMaps.size());
    assertTrue(defaultConceptMaps.size() <= maxPageMaps.size());

    // Verify that concatenated pages equal first 4 of full results
    List<String> fourIds = defaultConceptMaps.subList(0, 4).stream()
        .map(resource -> ((ConceptMap) resource).getIdPart()).sorted().toList();

    List<String> paginatedIds = Stream.concat(firstPageMaps.stream(), secondPageMaps.stream())
        .map(resource -> ((ConceptMap) resource).getIdPart()).sorted().toList();

    assertEquals(fourIds, paginatedIds);

    // Verify content of individual resources
    for (Resource cs : defaultConceptMaps) {
      validateConceptMapPagination((ConceptMap) cs);
    }
  }

  /**
   * Validate concept map pagination.
   *
   * @param cm the cm
   */
  private void validateConceptMapPagination(ConceptMap cm) {
    assertNotNull(cm);
    assertEquals(ResourceType.ConceptMap, cm.getResourceType());
    assertNotNull(cm.getIdPart());
    assertNotNull(cm.getPublisher());
    assertNotNull(cm.getUrl());

    // Log for debugging
    log.info(" concept map = " + parser.encodeResourceToString(cm));

    // Verify specific IDs and URLs if needed
    if (cm.getIdPart().equals("icd10_to_meddra_mapping_july2023")) {
      assertEquals("http://hl7.org/fhir/sid/icd-10?fhir_cm=ICD10_to_MedDRA_Mapping", cm.getUrl());
    }
  }

}
