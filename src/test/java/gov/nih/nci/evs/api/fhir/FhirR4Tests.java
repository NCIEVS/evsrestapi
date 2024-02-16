package gov.nih.nci.evs.api.fhir;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * Integration tests for FhirR4Tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirR4Tests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(FhirR4Tests.class);

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  /** The test properties. */
  @Autowired
  TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** The base url. */
  private String baseUrl = "";

  /**
   * Sets the up.
   */
  @Before
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
    content = this.restTemplate.getForObject("http://localhost:" + port + "/fhir/r4/CodeSystem",
        String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> codeSystems = data.getEntry().stream().map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(codeSystems.size()).isGreaterThan(0);
    for (Resource cs : codeSystems) {
      CodeSystem css = (CodeSystem) cs;
      assertThat(css).isNotNull();
      assertThat(css.getResourceType().equals(ResourceType.CodeSystem));
      assertThat(css.getIdPart()).isNotNull();
      assertThat(css.getPublisher()).isNotNull();
      assertThat(css.getUrl()).isNotNull();
    }
    // test read
    String firstCodeSystemId = codeSystems.get(0).getIdPart();
    content = this.restTemplate.getForObject(
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
    String messageNotFound = "The code does not exist for the supplied code system and/or version";

    // active code
    content = this.restTemplate.getForObject("http://localhost:" + port
        + "/fhir/r4/CodeSystem/$validate-code?url=" + url + "&code=" + activeCode, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("code").getValue()).getValue())
        .isEqualTo(activeCode);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);

    content = this.restTemplate.getForObject(
        "http://localhost:" + port + "/fhir/r4/CodeSystem/" + activeID + "/$validate-code?url="
            + url + "&code=" + activeCode + "&display=" + displayString,
        String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("code").getValue()).getValue())
        .isEqualTo(activeCode);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);

    content = this.restTemplate
        .getForObject("http://localhost:" + port + "/fhir/r4/CodeSystem/$validate-code?url=" + url
            + "&code=" + activeCode + "&display=" + displayString, String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("code").getValue()).getValue())
        .isEqualTo(activeCode);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);

    // code not found
    content = this.restTemplate.getForObject("http://localhost:" + port
        + "/fhir/r4/CodeSystem/$validate-code?url=" + url + "&code=" + codeNotFound, String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    content = this.restTemplate
        .getForObject("http://localhost:" + port + "/fhir/r4/CodeSystem/$validate-code?url=" + url
            + "&code=" + codeNotFound + "&display=" + displayString, String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    content = this.restTemplate.getForObject(
        "http://localhost:" + port + "/fhir/r4/CodeSystem/" + activeID + "/$validate-code?url="
            + url + "&code=" + codeNotFound + "&display=" + displayString,
        String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);
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
    content = this.restTemplate.getForObject("http://localhost:" + port + "/fhir/r4/ValueSet",
        String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> valueSets = data.getEntry().stream().map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(valueSets.size()).isGreaterThan(0);
    for (Resource vs : valueSets) {
      ValueSet vss = (ValueSet) vs;
      assertThat(vss).isNotNull();
      assertThat(vss.getResourceType().equals(ResourceType.ValueSet));
      assertThat(vss.getIdPart()).isNotNull();
      assertThat(vss.getPublisher()).isNotNull();
      assertThat(vss.getUrl()).isNotNull();
    }
    // test read
    String firstValueSetId = valueSets.get(0).getIdPart();
    content = this.restTemplate.getForObject(
        "http://localhost:" + port + "/fhir/r4/ValueSet/" + firstValueSetId, String.class);
    ValueSet valueSet = parser.parseResource(ValueSet.class, content);
    assertThat(valueSet).isNotNull();
    assertThat(valueSet.getResourceType().equals(ResourceType.ValueSet));
    assertThat(valueSet.getIdPart()).isEqualTo(firstValueSetId);
    assertThat(valueSet.getName()).isEqualTo(((ValueSet) valueSets.get(0)).getName());
    assertThat(valueSet.getPublisher()).isEqualTo(((ValueSet) valueSets.get(0)).getPublisher());

    // test validate-code
    String activeCode = "T100";
    String activeID = "umlssemnet_2023AA";
    String codeNotFound = "T10";
    String url = "http://www.nlm.nih.gov/research/umls/umlssemnet.owl";
    String displayString = "Age Group";
    String messageNotFound = "The code '" + codeNotFound + "' was not found.";

    // active code
    content = this.restTemplate.getForObject("http://localhost:" + port
        + "/fhir/r4/ValueSet/$validate-code?url=" + url + "&code=" + activeCode, String.class);
    Parameters params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);

    content = this.restTemplate.getForObject(
        "http://localhost:" + port + "/fhir/r4/ValueSet/" + activeID + "/$validate-code?url=" + url
            + "&code=" + activeCode + "&display=" + displayString,
        String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);

    content = this.restTemplate
        .getForObject("http://localhost:" + port + "/fhir/r4/ValueSet/$validate-code?url=" + url
            + "&code=" + activeCode + "&display=" + displayString, String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue()).isEqualTo(true);
    assertThat(((StringType) params.getParameter("display").getValue()).getValue())
        .isEqualTo(displayString);

    // code not found
    content = this.restTemplate.getForObject("http://localhost:" + port
        + "/fhir/r4/ValueSet/$validate-code?url=" + url + "&code=" + codeNotFound, String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    content = this.restTemplate
        .getForObject("http://localhost:" + port + "/fhir/r4/ValueSet/$validate-code?url=" + url
            + "&code=" + codeNotFound + "&display=" + displayString, String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

    content = this.restTemplate.getForObject(
        "http://localhost:" + port + "/fhir/r4/ValueSet/" + activeID + "/$validate-code?url=" + url
            + "&code=" + codeNotFound + "&display=" + displayString,
        String.class);
    params = parser.parseResource(Parameters.class, content);
    assertThat(((BooleanType) params.getParameter("result").getValue()).getValue())
        .isEqualTo(false);
    assertThat(((StringType) params.getParameter("message").getValue()).getValue())
        .isEqualTo(messageNotFound);

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
    content = this.restTemplate.getForObject("http://localhost:" + port + "/fhir/r4/ConceptMap",
        String.class);
    Bundle data = parser.parseResource(Bundle.class, content);
    List<Resource> conceptMaps = data.getEntry().stream().map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(conceptMaps.size()).isGreaterThan(0);
    for (Resource cm : conceptMaps) {
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
    content = this.restTemplate.getForObject(
        "http://localhost:" + port + "/fhir/r4/ConceptMap/" + firstConceptMapId, String.class);
    ConceptMap conceptMap = parser.parseResource(ConceptMap.class, content);
    assertThat(conceptMap).isNotNull();
    assertThat(conceptMap.getResourceType().equals(ResourceType.ConceptMap));
    assertThat(conceptMap.getIdPart()).isEqualTo(firstConceptMapId);
    assertThat(conceptMap.getName()).isEqualTo(((ConceptMap) conceptMaps.get(0)).getName());
    assertThat(conceptMap.getVersion()).isEqualTo(((ConceptMap) conceptMaps.get(0)).getVersion());
  }
}
