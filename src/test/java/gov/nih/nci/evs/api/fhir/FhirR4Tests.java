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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Class tests for FhirR4Tests. Tests the functionality of the FHIR R4 endpoints, CodeSystem,
 * ValueSet, and ConceptMap. All passed ids MUST be lowercase, so they match our internally set id's
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR4Tests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4Tests.class);

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
    // {"resourceType":"CodeSystem","id":"ncit_21.06e","url":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","version":"21.06e","name":"NCI Thesaurus 21.06e","title":"ncit","status":"active","experimental":false,"publisher":"NCI","hierarchyMeaning":"is-a"}
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
   * Test code system validate active code.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemValidateActiveCode() throws Exception {
    // Arrange
    String content;
    String activeCode = "T100";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
  }

  /**
   * Test code system validate active code and display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemValidateActiveCodeDisplayString() throws Exception {
    // Arrange
    String content;
    String activeCode = "T100";
    String activeId = "umlssemnet_2023aa";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + activeCode + "&display" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
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
    String messageNotFound =
        "The code does not exist for the supplied code system url and/or version";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
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
   * Test code system code not found and display string.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemCodeNotFoundAndDisplayString() throws Exception {
    // Arrange
    String content;
    String activeId = "ulssemnet_2023aa";
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String messageNotFound = "Unable to find matching code system";
    String endpoint =
        localHost + port + fhirCSPath + "/" + activeId + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + url + "&code=" + codeNotFound + "$display" + displayString;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        messageNotFound, ((StringType) params.getParameter("message").getValue()).getValue());
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
    String retiredName = "ABCB1 1 Allele";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?url=" + retiredUrl + "&code=" + retiredCode;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(retiredCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
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
    String endpoint =
        localHost
            + port
            + fhirCSPath
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
    assertEquals(retiredCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
  }

  /**
   * Test code system bad.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemBad() throws Exception {
    // Arrange
    String content;
    String code = "C3224";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String message = "Unable to find matching code system";
    String isNull = "<null>";
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?code=" + code + "&system=" + url;

    // Act
    content = restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(message, ((StringType) params.getParameter("message").getValue()).getValue());
    assertEquals(isNull, ((UriType) params.getParameter("url").getValue()).getValue());
  }

  /**
   * Test the CodeSystem rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemPostRejects() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    String message = "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE;
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
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
   * Test value set search.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearch() throws Exception {
    // Arrange
    String content;
    String endpoint = localHost + port + fhirVSPath;

    // Act
    content = this.restTemplate.getForObject(endpoint, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // Verify things about these
    // {"resourceType":"ValueSet","id":"ncit_21.06e","url":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl","version":"21.06e","name":"NCI Thesaurus 21.06e","title":"ncit","status":"active","experimental":false,"publisher":"NCI","description":"NCI Thesaurus, a controlled vocabulary in support of NCI administrative and scientific activities. Produced by the Enterprise Vocabulary System (EVS), a project by the NCI Center for Biomedical Informatics and Information Technology. National Cancer Institute, National Institutes of Health, Bethesda, MD 20892, U.S.A."}
    // {"resourceType":"ValueSet","id":"ncit_c61410","url":"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C100110","identifier":[{"value":"C61410"}]...
    final Set<String> ids = new HashSet<>(Set.of("ncit_21.06e", "ncit_c61410"));
    final Set<String> urls =
        new HashSet<>(
            Set.of(
                "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs",
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
   * Test concept map translate with instance system; id, code, and system provided.
   *
   * @throws Exception throws exception when error occurs
   */
  @Test
  public void testConceptMapTranslateInstance() throws Exception {
    // Arrange
    String content;
    String code = "GO:0016887";
    String id = "go_to_ncit_mapping_february2020";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String endpoint =
        localHost + port + fhirCMPath + "/" + id + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
  }

  /**
   * Test concept map translate with instance system with reverse = true; id, code, system, and
   * display provided.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMaptTranslateInstanceWithReverse() throws Exception {
    // Arrange
    String content;
    String code = "C19939";
    String id = "go_to_ncit_mapping_february2020";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String reverse = "true";
    String endpoint =
        localHost + port + fhirCMPath + "/" + id + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system + "&reverse=" + reverse;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
    log.info("  translate params =\n" + parser.encodeResourceToString(params));

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
  }

  /**
   * Test concept map translate with implicit system; code and system provided.
   *
   * @throws Exception throws exception when error occurs
   */
  @Test
  public void testConceptMapTranslateImplicit() throws Exception {
    // Arrange
    String content;
    String code = "GO:0016887";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
    log.info("  translate params =\n" + parser.encodeResourceToString(params));

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
  }

  /**
   * Test concept map translate with implicit system with reverse = true; code and system provided.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapTranslateImplicitWithReverse() throws Exception {
    // Arrange
    String content;
    String code = "C19939";
    String system = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String reverse = "true";
    String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + code + "&system=" + system + "&reverse=" + reverse;

    // Act
    content = this.restTemplate.getForObject(endpoint + parameters, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // Assert
    assertNotNull(params);
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
  }

  /**
   * Test the ValueSet rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapPostRejects() throws Exception {
    // Arrange
    ResponseEntity<String> content;
    String message = "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE;

    String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
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
