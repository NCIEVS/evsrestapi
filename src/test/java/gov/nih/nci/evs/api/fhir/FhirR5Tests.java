package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.ResourceType;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FhirR5Tests {
  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR5Tests.class);

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** local host prefix */
  private final String localHost = "http://localhost:";

  /** Fhir url paths */
  private final String fhirCSPath = "/fhir/r5/CodeSystem";

  private final String fhirVSPath = "/fhir/r5/ValueSet";
  private final String fhirCMPath = "/fhir/r5/ConceptMap";

  /** The Parser */
  private IParser parser;

  /** Sets the up. */
  @BeforeEach
  public void setUp() {
    /** The object mapper. */
    ObjectMapper objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
    // Instantiate a new parser
    parser = FhirContext.forR5().newJsonParser();
  }

  /**
   * Test code system search.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemSearch() throws Exception {
    // ARRANGE
    String content = this.restTemplate.getForObject(localHost + port + fhirCSPath, String.class);

    // ACT
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // ASSERT
    assertFalse(codeSystems.isEmpty());
    for (Resource cs : codeSystems) {
      log.info("  code system = " + FhirContext.forR5().newJsonParser().encodeResourceToString(cs));
      CodeSystem css = (CodeSystem) cs;
      assertNotNull(css);
      assertEquals(ResourceType.CodeSystem, css.getResourceType());
      assertNotNull(css.getIdPart());
      assertNotNull(css.getPublisher());
      assertNotNull(css.getUrl());
    }
  }

  /**
   * Test code system read.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemRead() throws Exception {
    // ARRANGE
    String content = this.restTemplate.getForObject(localHost + port + fhirCSPath, String.class);
    ;

    // ACT
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
    String firstCodeSystemId = codeSystems.get(0).getIdPart();
    // reassign content with firstCodeSystemId
    content =
        this.restTemplate.getForObject(
            localHost + port + fhirCSPath + "/" + firstCodeSystemId, String.class);
    CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);

    // ASSERT
    assertNotNull(codeSystem);
    assertEquals(ResourceType.CodeSystem, codeSystem.getResourceType());
    assertEquals(firstCodeSystemId, codeSystem.getIdPart());
    assertEquals(((CodeSystem) codeSystems.get(0)).getName(), codeSystem.getName());
    assertEquals(((CodeSystem) codeSystems.get(0)).getPublisher(), codeSystem.getPublisher());
  }

  /**
   * Test code system validate active code.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemValidateActiveCode() throws Exception {
    // ARRANGE
    String content;
    String activeCode = "T100";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirCSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + activeCode,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String activeCode = "T100";
    String activeId = "umlssemnet_2023AA";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirCSPath
                + "/"
                + activeId
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + activeCode
                + "&display="
                + displayString,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(activeCode, ((StringType) params.getParameter("code").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("dispaly").getValue()).getValue());
    assertTrue(((BooleanType) params.getParameter("active").getValue()).getValue());
  }

  /**
   * Test code system validate code not found.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemCodeNotFound() throws Exception {
    // ARRANGE
    String content;
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String messageNotFound =
        "The code does not exist for the supplied code system url and/or version";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirCSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + codeNotFound,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String activeId = "ulssemnet_2023AA";
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String messageNotFound = "Unable to find matching code syste";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirCSPath
                + "/"
                + activeId
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + codeNotFound
                + "&display="
                + displayString,
            String.class);
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
    // ARRANGE
    String content;
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String retiredName = "ABCB1 1 Allele";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirCSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + retiredUrl
                + "&code="
                + retiredCode,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String retiredId = "ncit_21.06e";
    String retiredName = "ABCB1 1 Allele";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirCSPath
                + "/"
                + retiredId
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + retiredUrl
                + "&code="
                + retiredCode
                + "&display="
                + retiredName,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String code = "C3224";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String message = "Unable to find matching code system";
    String isNull = "<null>";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirCSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?code="
                + code
                + "&system="
                + url,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
    assertFalse(((BooleanType) params.getParameter("results").getValue()).getValue());
    assertEquals(message, ((StringType) params.getParameter("message").getValue()).getValue());
    assertEquals(isNull, ((UriType) params.getParameter("url").getValue()).getValue());
  }

  /**
   * Test value set search.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetSearch() throws Exception {
    // ARRANGE
    String content;

    // ACT
    content = this.restTemplate.getForObject(localHost + port + fhirVSPath, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // ASSERT
    assertFalse(valueSets.isEmpty());
    for (Resource vs : valueSets) {
      log.info("  value set = " + FhirContext.forR5().newJsonParser().encodeResourceToString(vs));
      ValueSet vss = (ValueSet) vs;
      assertNotNull(vss);
      assertEquals(ResourceType.ValueSet, vss.getResourceType());
      assertNotNull(vss.getIdPart());
      assertNotNull(vss.getPublisher());
      assertNotNull(vss.getUrl());
    }
  }

  /**
   * Test value set read.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetRead() throws Exception {
    // ARRANGE
    String content = this.restTemplate.getForObject(localHost + port + fhirVSPath, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // ACT
    String firstValueSetId = valueSets.get(0).getIdPart();
    // reassign content
    content =
        this.restTemplate.getForObject(
            localHost + port + fhirVSPath + "/" + firstValueSetId, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // ASSERT
    assertNotNull(valueSet);
    assertEquals(ResourceType.ValueSet, valueSet.getResourceType());
    assertEquals(firstValueSetId, valueSet.getIdPart());
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
    // ARRANGE
    String content;
    String code = "ncit_C129091";
    String name = "CDISC Questionnaire NCCN-FACT FBLSI-18 Version 2 Test Name Terminology";
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C129091";
    String publisher = "NCI";

    // ACT
    content =
        this.restTemplate.getForObject(localHost + port + fhirVSPath + "/" + code, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String activeCode = "T100";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirVSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + activeCode,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        displayString, ((StringType) params.getParameter("display").getValue()).getValue());
  }

  /**
   * Test value set validate active id, active code and display string
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetValidateActiveIdAndActiveCodeAndDisplayString() throws Exception {
    // ARRANGE
    String content;
    String activeCode = "T100";
    String activeID = "umlssemnet_2023AA";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirVSPath
                + "/"
                + activeID
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + activeCode
                + "&display="
                + displayString,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String activeCode = "T100";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirVSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + activeCode
                + "&display="
                + displayString,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String messageNotFound = "The code '" + codeNotFound + "' was not found.";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirVSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + codeNotFound,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String messageNotFound = "The code '" + codeNotFound + "' was not found.";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirVSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + codeNotFound
                + "&display="
                + displayString,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
    assertFalse(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(
        messageNotFound, ((StringType) params.getParameter("message").getValue()).getValue());
  }

  /**
   * Test value set code not found for activde Id and display string
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetCodeNotFoundActiveIdAndDisplayString() throws Exception {
    // ARRANGE
    String content;
    String activeID = "umlssemnet_2023AA";
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String messageNotFound = "The code '" + codeNotFound + "' was not found.";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirVSPath
                + "/"
                + activeID
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + url
                + "&code="
                + codeNotFound
                + "&display="
                + displayString,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String retiredName = "ABCB1 1 Allele";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirVSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + retiredUrl
                + "&code="
                + retiredCode,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String retiredName = "ABCB1 1 Allele";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirVSPath
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + retiredUrl
                + "&code="
                + retiredCode
                + "&display="
                + retiredName,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
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
    // ARRANGE
    String content;
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String retiredId = "ncit_21.06e";
    String retiredName = "ABCB1 1 Allele";

    // ACT
    content =
        this.restTemplate.getForObject(
            localHost
                + port
                + fhirVSPath
                + "/"
                + retiredId
                + "/"
                + JpaConstants.OPERATION_VALIDATE_CODE
                + "?url="
                + retiredUrl
                + "&code="
                + retiredCode
                + "&display="
                + retiredName,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);

    // ASSERT
    assertTrue(((BooleanType) params.getParameter("result").getValue()).getValue());
    assertEquals(retiredName, ((StringType) params.getParameter("display").getValue()).getValue());
  }

  /**
   * Test concept map search.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapSearch() throws Exception {
    // ARRANGE
    String content = this.restTemplate.getForObject(localHost + port + fhirCMPath, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);

    // ACT
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // ASSERT
    assertFalse(conceptMaps.isEmpty());
    for (Resource cm : conceptMaps) {
      log.info("  concept map = " + FhirContext.forR5().newJsonParser().encodeResourceToString(cm));
      ConceptMap cmm = (ConceptMap) cm;
      assertNotNull(cmm);
      assertEquals(ResourceType.ConceptMap, cmm.getResourceType());
      assertNotNull(cmm.getIdPart());
      assertNotNull(cmm.getGroup());
      assertNotNull(cmm.getVersion());
      assertNotNull(cmm.getUrl());
    }
  }

  /**
   * Test concept map read.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapRead() throws Exception {
    // ARRANGE
    String content = this.restTemplate.getForObject(localHost + port + fhirCMPath, String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    // ACT
    String firstConceptMapId = conceptMaps.get(0).getIdPart();
    // reassign content
    content =
        this.restTemplate.getForObject(
            localHost + port + fhirCMPath + "/" + firstConceptMapId, String.class);
    ConceptMap conceptMap = parser.parseResource(ConceptMap.class, content);

    // ASSERT
    assertNotNull(conceptMap);
    assertEquals(ResourceType.ConceptMap, conceptMap.getResourceType());
    assertEquals(firstConceptMapId, conceptMap.getIdPart());
    assertEquals(conceptMap.getName(), ((ConceptMap) conceptMaps.get(0)).getName());
    assertEquals(conceptMap.getVersion(), ((ConceptMap) conceptMaps.get(0)).getVersion());
  }
}
