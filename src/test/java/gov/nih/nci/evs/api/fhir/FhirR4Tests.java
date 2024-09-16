package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.util.List;
import java.util.stream.Collectors;
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

/** Integration tests for FhirR4Tests. */
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

  /** local host prefix */
  private final String localHost = "http://localhost:";

  /** Fhir url paths */
  private final String fhirCSPath = "/fhir/r5/CodeSystem";

  private final String fhirVSPath = "/fhir/r5/ValueSet";
  private final String fhirCMPath = "/fhir/r5/ConceptMap";

  /** Sets the up. */
  @BeforeEach
  public void setUp() {

    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  /**
   * Test CodeSystem.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystem() throws Exception {
    String content = null;
    // Instantiate a new parser
    IParser parser = FhirContext.forR4().newJsonParser();

    // test search
    content =
        this.restTemplate.getForObject(
            "http://localhost:" + port + "/fhir/r4/CodeSystem", String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems =
        data.getEntry().stream()
            .map(BundleEntryComponent::getResource)
            .collect(Collectors.toList());
    assertThat(codeSystems.size()).isGreaterThan(0);
    for (Resource cs : codeSystems) {
      log.info("  code system = " + FhirContext.forR4().newJsonParser().encodeResourceToString(cs));
      CodeSystem css = (CodeSystem) cs;
      assertThat(css).isNotNull();
      assertThat(css.getResourceType().equals(ResourceType.CodeSystem));
      assertThat(css.getIdPart()).isNotNull();
      assertThat(css.getPublisher()).isNotNull();
      assertThat(css.getUrl()).isNotNull();
    }
    // test read
    String firstCodeSystemId = codeSystems.get(0).getIdPart();
    content =
        this.restTemplate.getForObject(
            "http://localhost:" + port + "/fhir/r4/CodeSystem/" + firstCodeSystemId, String.class);
    CodeSystem codeSystem = parser.parseResource(CodeSystem.class, content);
    assertThat(codeSystem).isNotNull();
    assertThat(codeSystem.getResourceType().equals(ResourceType.CodeSystem));
    assertThat(codeSystem.getIdPart()).isEqualTo(firstCodeSystemId);
    assertThat(codeSystem.getName()).isEqualTo(((CodeSystem) codeSystems.get(0)).getName());
    assertThat(codeSystem.getPublisher())
        .isEqualTo(((CodeSystem) codeSystems.get(0)).getPublisher());

    // test validate-code
    String activeCode = "T100";
    String activeID = "umlssemnet_2023AA";
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String messageNotFound =
        "The code does not exist for the supplied code system url and/or version";

    // active code
    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/$validate-code?url="
                + url
                + "&code="
                + activeCode,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("code").getValue()).getValue())
        .isEqualTo(activeCode);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);
    assertThat(((BooleanType) params.getParameter("active").getValue()).getValue()).isEqualTo(true);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/"
                + activeID
                + "/$validate-code?url="
                + url
                + "&code="
                + activeCode
                + "&display="
                + displayString,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("code").getValue()).getValue())
        .isEqualTo(activeCode);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);
    assertThat(((BooleanType) params.getParameter("active").getValue()).getValue()).isEqualTo(true);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/$validate-code?url="
                + url
                + "&code="
                + activeCode
                + "&display="
                + displayString,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("code").getValue()).getValue())
        .isEqualTo(activeCode);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);
    assertThat(((BooleanType) params.getParameter("active").getValue()).getValue()).isEqualTo(true);

    // code not found
    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/$validate-code?url="
                + url
                + "&code="
                + codeNotFound,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/$validate-code?url="
                + url
                + "&code="
                + codeNotFound
                + "&display="
                + displayString,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/"
                + activeID
                + "/$validate-code?url="
                + url
                + "&code="
                + codeNotFound
                + "&display="
                + displayString,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    // retired code
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String retiredId = "ncit_21.06e";
    String retiredName = "ABCB1 1 Allele";
    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/$validate-code?url="
                + retiredUrl
                + "&code="
                + retiredCode,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("code").getValue()).getValue())
        .isEqualTo(retiredCode);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(retiredName);
    assertThat(((BooleanType) params.getParameter("active").getValue()).getValue()).isEqualTo(true);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/$validate-code?url="
                + retiredUrl
                + "&code="
                + retiredCode
                + "&display="
                + retiredName,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("code").getValue()).getValue())
        .isEqualTo(retiredCode);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(retiredName);
    assertThat(((BooleanType) params.getParameter("active").getValue()).getValue()).isEqualTo(true);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/"
                + retiredId
                + "/$validate-code?url="
                + retiredUrl
                + "&code="
                + retiredCode
                + "&display="
                + retiredName,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("code").getValue()).getValue())
        .isEqualTo(retiredCode);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(retiredName);
    assertThat(((BooleanType) params.getParameter("active").getValue()).getValue()).isEqualTo(true);
  }

  /**
   * Test the CodeSystem rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testCodeSystemPostRejects() throws Exception {
    // ARRANGE
    ResponseEntity<String> content;
    String message = "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE;
    String endpoint = localHost + port + fhirCSPath + "/" + JpaConstants.OPERATION_VALIDATE_CODE;
    String parameters = "?code=" + null + "&system=" + null;

    // ACT
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);

    // ASSERT
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }

  @Test
  public void testCodeSystemBad() throws Exception {
    String content = null;
    // Instantiate a new parser
    IParser parser = FhirContext.forR4().newJsonParser();
    Parameters params = null;

    // test validate-code with "system" instead of URI (EVSRESTAPI-499)
    content =
        restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/CodeSystem/$validate-code?"
                + "code=C3224&system=http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo("Unable to find matching code system");
    assertThat(((UriType) params.getParameter("url").getValue()).getValue()).isEqualTo("<null>");
  }

  /**
   * Test ValueSet.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSet() throws Exception {
    String content = null;
    // Instantiate a new parser
    IParser parser = FhirContext.forR4().newJsonParser();

    // test search
    content =
        this.restTemplate.getForObject(
            "http://localhost:" + port + "/fhir/r4/ValueSet", String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets =
        data.getEntry().stream()
            .map(BundleEntryComponent::getResource)
            .collect(Collectors.toList());
    assertThat(valueSets.size()).isGreaterThan(0);
    for (Resource vs : valueSets) {
      log.info("  value set  = " + FhirContext.forR4().newJsonParser().encodeResourceToString(vs));
      ValueSet vss = (ValueSet) vs;
      assertThat(vss).isNotNull();
      assertThat(vss.getResourceType().equals(ResourceType.ValueSet));
      assertThat(vss.getIdPart()).isNotNull();
      assertThat(vss.getPublisher()).isNotNull();
      assertThat(vss.getUrl()).isNotNull();
    }
    // test read
    String firstValueSetId = valueSets.get(0).getIdPart();
    content =
        this.restTemplate.getForObject(
            "http://localhost:" + port + "/fhir/r4/ValueSet/" + firstValueSetId, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);
    assertThat(valueSet).isNotNull();
    assertThat(valueSet.getResourceType().equals(ResourceType.ValueSet));
    assertThat(valueSet.getIdPart()).isEqualTo(firstValueSetId);
    assertThat(valueSet.getName()).isEqualTo(((ValueSet) valueSets.get(0)).getName());
    assertThat(valueSet.getPublisher()).isEqualTo(((ValueSet) valueSets.get(0)).getPublisher());

    // test read for a value set with a code
    content =
        this.restTemplate.getForObject(
            "http://localhost:" + port + "/fhir/r4/ValueSet/ncit_C129091", String.class);
    valueSet = parser.parseResource(ValueSet.class, content);
    assertThat(valueSet).isNotNull();
    assertThat(valueSet.getResourceType().equals(ResourceType.ValueSet));
    assertThat(valueSet.getIdPart()).isEqualTo("ncit_C129091");
    assertThat(valueSet.getName())
        .isEqualTo("CDISC Questionnaire NCCN-FACT FBLSI-18 Version 2 Test Name Terminology");
    assertThat(valueSet.getPublisher()).isEqualTo("NCI");
    assertThat(valueSet.getUrl())
        .isEqualTo("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs=C129091");

    // test validate-code
    String activeCode = "T100";
    String activeID = "umlssemnet_2023AA";
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String messageNotFound = "The code '" + codeNotFound + "' was not found.";

    // active code
    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/ValueSet/$validate-code?url="
                + url
                + "&code="
                + activeCode,
            String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/ValueSet/"
                + activeID
                + "/$validate-code?url="
                + url
                + "&code="
                + activeCode
                + "&display="
                + displayString,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/ValueSet/$validate-code?url="
                + url
                + "&code="
                + activeCode
                + "&display="
                + displayString,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);

    // code not found
    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/ValueSet/$validate-code?url="
                + url
                + "&code="
                + codeNotFound,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/ValueSet/$validate-code?url="
                + url
                + "&code="
                + codeNotFound
                + "&display="
                + displayString,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/ValueSet/"
                + activeID
                + "/$validate-code?url="
                + url
                + "&code="
                + codeNotFound
                + "&display="
                + displayString,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    // retired code
    String retiredCode = "C45683";
    String retiredUrl = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String retiredId = "ncit_21.06e";
    String retiredName = "ABCB1 1 Allele";
    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/ValueSet/$validate-code?url="
                + retiredUrl
                + "&code="
                + retiredCode,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(retiredName);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/ValueSet/$validate-code?url="
                + retiredUrl
                + "&code="
                + retiredCode
                + "&display="
                + retiredName,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(retiredName);

    content =
        this.restTemplate.getForObject(
            "http://localhost:"
                + port
                + "/fhir/r4/ValueSet/"
                + retiredId
                + "/$validate-code?url="
                + retiredUrl
                + "&code="
                + retiredCode
                + "&display="
                + retiredName,
            String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(retiredName);
  }

  /**
   * Test the ValueSet rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testValueSetPostRejects() throws Exception {
    // ARRANGE
    ResponseEntity<String> content;
    String message = "POST method not supported for " + JpaConstants.OPERATION_EXPAND;

    String endpoint = localHost + port + fhirVSPath + "/" + JpaConstants.OPERATION_EXPAND;
    String parameters = "?code=" + null + "&system=" + null;

    // ACT
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);

    // ASSERT
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }

  /**
   * Test ConceptMap.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMap() throws Exception {
    String content = null;
    // Instantiate a new parser
    IParser parser = FhirContext.forR4().newJsonParser();

    // test search
    content =
        this.restTemplate.getForObject(
            "http://localhost:" + port + "/fhir/r4/ConceptMap", String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps =
        data.getEntry().stream()
            .map(BundleEntryComponent::getResource)
            .collect(Collectors.toList());
    assertThat(conceptMaps.size()).isGreaterThan(0);
    for (Resource cm : conceptMaps) {
      log.info("  concept map = " + FhirContext.forR4().newJsonParser().encodeResourceToString(cm));
      ConceptMap cmm = (ConceptMap) cm;
      assertThat(cmm).isNotNull();
      assertThat(cmm.getResourceType().equals(ResourceType.ConceptMap));
      assertThat(cmm.getIdPart()).isNotNull();
      assertThat(cmm.getGroup()).isNotNull();
      assertThat(cmm.getVersion()).isNotNull();
      assertThat(cmm.getUrl()).isNotNull();
    }
    // test read
    String firstConceptMapId = conceptMaps.get(0).getIdPart();
    content =
        this.restTemplate.getForObject(
            "http://localhost:" + port + "/fhir/r4/ConceptMap/" + firstConceptMapId, String.class);
    ConceptMap conceptMap = parser.parseResource(ConceptMap.class, content);
    assertThat(conceptMap).isNotNull();
    assertThat(conceptMap.getResourceType().equals(ResourceType.ConceptMap));
    assertThat(conceptMap.getIdPart()).isEqualTo(firstConceptMapId);
    assertThat(conceptMap.getName()).isEqualTo(((ConceptMap) conceptMaps.get(0)).getName());
    assertThat(conceptMap.getVersion()).isEqualTo(((ConceptMap) conceptMaps.get(0)).getVersion());
  }

  /**
   * Test the ValueSet rejects a post call when attempted.
   *
   * @throws Exception exception
   */
  @Test
  public void testConceptMapPostRejects() throws Exception {
    // ARRANGE
    ResponseEntity<String> content;
    String message = "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE;

    String endpoint = localHost + port + fhirCMPath + "/" + JpaConstants.OPERATION_TRANSLATE;
    String parameters = "?code=" + null + "&system=" + null;

    // ACT
    content = this.restTemplate.postForEntity(endpoint + parameters, null, String.class);

    // ASSERT
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, content.getStatusCode());
    assertNotNull(content.getBody());
    assertTrue(content.getBody().contains(message));
    assertTrue(content.getBody().contains("not supported"));
  }
}
