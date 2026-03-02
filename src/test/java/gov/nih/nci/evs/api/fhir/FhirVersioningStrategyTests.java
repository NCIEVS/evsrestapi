package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.nih.nci.evs.api.properties.TestProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirVersioningStrategyTests {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @SuppressWarnings("unused")
  @Autowired
  private TestProperties testProperties;

  private final String localHost = "http://localhost:";

  private static IParser parserR4;
  private static IParser parserR5;

  @BeforeAll
  public static void setUpOnce() {
    parserR4 = FhirContext.forR4().newJsonParser();
    parserR5 = FhirContext.forR5().newJsonParser();
  }

  @Test
  public void testCodeSystemLookupNoVersionR4() throws Exception {
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String code = "C3224";
    String endpoint = localHost + port + "/fhir/r4/CodeSystem/$lookup?system=" + url + "&code=" + code;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    org.hl7.fhir.r4.model.Parameters params = parserR4.parseResource(org.hl7.fhir.r4.model.Parameters.class, content);
    assertNotNull(params.getParameter("version"));
  }

  @Test
  public void testValueSetExpandNoVersionR4() throws Exception {
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    String endpoint = localHost + port + "/fhir/r4/ValueSet/$expand?url=" + url;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    org.hl7.fhir.r4.model.ValueSet vs = parserR4.parseResource(org.hl7.fhir.r4.model.ValueSet.class, content);
    assertNotNull(vs.getVersion());
  }

  @Test
  public void testConceptMapTranslateNoVersionR4() throws Exception {
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_cm=GO_to_NCIt_Mapping";
    String system = "http://purl.obolibrary.org/obo/go.owl";
    String code = "GO:0000021";
    String endpoint = localHost + port + "/fhir/r4/ConceptMap/$translate?url=" + url + "&system=" + system + "&code=" + code;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    org.hl7.fhir.r4.model.Parameters params = parserR4.parseResource(org.hl7.fhir.r4.model.Parameters.class, content);
    assertNotNull(params.getParameter("result"));
  }

  @Test
  public void testCodeSystemLookupNoVersionR5() throws Exception {
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String code = "C3224";
    String endpoint = localHost + port + "/fhir/r5/CodeSystem/$lookup?system=" + url + "&code=" + code;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    org.hl7.fhir.r5.model.Parameters params = parserR5.parseResource(org.hl7.fhir.r5.model.Parameters.class, content);
    assertNotNull(params.getParameter("version"));
  }

  @Test
  public void testValueSetExpandNoVersionR5() throws Exception {
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    String endpoint = localHost + port + "/fhir/r5/ValueSet/$expand?url=" + url;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    org.hl7.fhir.r5.model.ValueSet vs = parserR5.parseResource(org.hl7.fhir.r5.model.ValueSet.class, content);
    assertNotNull(vs.getVersion());
  }

  @Test
  public void testConceptMapTranslateNoVersionR5() throws Exception {
    String url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_cm=GO_to_NCIt_Mapping";
    String system = "http://purl.obolibrary.org/obo/go.owl";
    String code = "GO:0000021";
    String endpoint = localHost + port + "/fhir/r5/ConceptMap/$translate?url=" + url + "&system=" + system + "&sourceCode=" + code;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    org.hl7.fhir.r5.model.Parameters params = parserR5.parseResource(org.hl7.fhir.r5.model.Parameters.class, content);
    assertNotNull(params.getParameter("result"));
  }
}
