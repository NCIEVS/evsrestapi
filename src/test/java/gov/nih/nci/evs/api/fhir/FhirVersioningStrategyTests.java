package gov.nih.nci.evs.api.fhir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.nih.nci.evs.api.properties.TestProperties;

/** FHIR version strategy tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class FhirVersioningStrategyTests {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(FhirVersioningStrategyTests.class);

  /** The port. */
  @LocalServerPort private int port;

  /** The rest template. */
  @Autowired private TestRestTemplate restTemplate;

  /** The test properties. */
  @SuppressWarnings("unused")
  @Autowired
  private TestProperties testProperties;

  /** The local host. */
  private final String localHost = "http://localhost:";

  /** The parser R 4. */
  private static IParser parserR4;

  /** The parser R 5. */
  private static IParser parserR5;

  /** Sets the up once. */
  @BeforeAll
  public static void setUpOnce() {
    parserR4 = FhirContext.forR4().newJsonParser();
    parserR5 = FhirContext.forR5().newJsonParser();
  }

  /**
   * Test code system lookup no version R 4.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupNoVersionR4() throws Exception {
    String url = null;
    String code = null;
    String endpoint = null;
    String content = null;
    org.hl7.fhir.r4.model.Parameters params = null;

    // NCI should be "latest monthly" not just "latest"
    url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    code = "C3224";
    endpoint = localHost + port + "/fhir/r4/CodeSystem/$lookup?system=" + url + "&code=" + code;
    content = this.restTemplate.getForObject(endpoint, String.class);
    // log.info("  content = " + content);
    params = parserR4.parseResource(org.hl7.fhir.r4.model.Parameters.class, content);
    assertNotNull(params.getParameter("version"));
    assertEquals("25.12e", params.getParameter("version").getValue().toString());

    // MDR should be "latest" because monthly doesn't matter
    url = "https://www.meddra.org";
    code = "10008906";
    endpoint = localHost + port + "/fhir/r4/CodeSystem/$lookup?system=" + url + "&code=" + code;
    content = this.restTemplate.getForObject(endpoint, String.class);
    // log.info("  content = " + content);
    params = parserR4.parseResource(org.hl7.fhir.r4.model.Parameters.class, content);
    assertNotNull(params.getParameter("version"));
    assertEquals("28_0", params.getParameter("version").getValue().toString());
  }

  /**
   * Test value set expand no version R 4.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandNoVersionR4() throws Exception {

    String url = null;
    String endpoint = null;
    String content = null;
    org.hl7.fhir.r4.model.ValueSet vs = null;

    // We expect "latest monthly" for "ncit"
    url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    endpoint = localHost + port + "/fhir/r4/ValueSet/$expand?url=" + url;
    content = this.restTemplate.getForObject(endpoint, String.class);

    // log.info("  content = " + content);
    vs = parserR4.parseResource(org.hl7.fhir.r4.model.ValueSet.class, content);
    assertNotNull(vs.getVersion());
    assertEquals("25.12e", vs.getVersion());

    // We expect "latest" for "mdr"
    url = "https://www.meddra.org?fhir_vs";
    endpoint = localHost + port + "/fhir/r4/ValueSet/$expand?url=" + url;
    content = this.restTemplate.getForObject(endpoint, String.class);

    // log.info("  content = " + content);
    vs = parserR4.parseResource(org.hl7.fhir.r4.model.ValueSet.class, content);
    assertNotNull(vs.getVersion());
    assertEquals("28_0", vs.getVersion());
  }

  /**
   * Test concept map translate no version R 4.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapTranslateNoVersionR4() throws Exception {
    String url = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String system = "http://purl.obolibrary.org/obo/go.owl";
    String code = "GO:0016887";
    String endpoint =
        localHost
            + port
            + "/fhir/r4/ConceptMap/$translate?url="
            + url
            + "&system="
            + system
            + "&code="
            + code;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    log.info("  content = " + content);
    org.hl7.fhir.r4.model.Parameters params =
        parserR4.parseResource(org.hl7.fhir.r4.model.Parameters.class, content);
    assertNotNull(params.getParameter("result"));
    assertTrue(
        ((org.hl7.fhir.r4.model.BooleanType) params.getParameter("result").getValue())
            .booleanValue());
  }

  /**
   * Test code system lookup no version R 5.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCodeSystemLookupNoVersionR5() throws Exception {
    String url = null;
    String code = null;
    String endpoint = null;
    String content = null;
    org.hl7.fhir.r5.model.Parameters params = null;

    // NCI should be "latest monthly" not just "latest"
    url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    code = "C3224";
    endpoint = localHost + port + "/fhir/r5/CodeSystem/$lookup?system=" + url + "&code=" + code;
    content = this.restTemplate.getForObject(endpoint, String.class);
    log.info("  content = " + content);
    params = parserR5.parseResource(org.hl7.fhir.r5.model.Parameters.class, content);
    assertNotNull(params.getParameter("version"));
    assertEquals("25.12e", params.getParameter("version").getValue().toString());

    // MDR should be "latest" because monthly doesn't matter
    url = "https://www.meddra.org";
    code = "10008906";
    endpoint = localHost + port + "/fhir/r5/CodeSystem/$lookup?system=" + url + "&code=" + code;
    content = this.restTemplate.getForObject(endpoint, String.class);
    log.info("  content = " + content);
    params = parserR5.parseResource(org.hl7.fhir.r5.model.Parameters.class, content);
    assertNotNull(params.getParameter("version"));
    assertEquals("28_0", params.getParameter("version").getValue().toString());
  }

  /**
   * Test value set expand no version R 5.
   *
   * @throws Exception the exception
   */
  @Test
  public void testValueSetExpandNoVersionR5() throws Exception {

    String url = null;
    String endpoint = null;
    String content = null;
    org.hl7.fhir.r5.model.ValueSet vs = null;

    // We expect "latest monthly" for "ncit"
    url = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl?fhir_vs";
    endpoint = localHost + port + "/fhir/r5/ValueSet/$expand?url=" + url;
    content = this.restTemplate.getForObject(endpoint, String.class);

    // log.info("  content = " + content);
    vs = parserR5.parseResource(org.hl7.fhir.r5.model.ValueSet.class, content);
    assertNotNull(vs.getVersion());
    assertEquals("25.12e", vs.getVersion());

    // We expect "latest" for "mdr"
    url = "https://www.meddra.org?fhir_vs";
    endpoint = localHost + port + "/fhir/r5/ValueSet/$expand?url=" + url;
    content = this.restTemplate.getForObject(endpoint, String.class);

    // log.info("  content = " + content);
    vs = parserR5.parseResource(org.hl7.fhir.r5.model.ValueSet.class, content);
    assertNotNull(vs.getVersion());
    assertEquals("28_0", vs.getVersion());
  }

  /**
   * Test concept map translate no version R 5.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConceptMapTranslateNoVersionR5() throws Exception {
    String url = "http://purl.obolibrary.org/obo/go.owl?fhir_cm=GO_to_NCIt_Mapping";
    String system = "http://purl.obolibrary.org/obo/go.owl";
    String code = "GO:0016887";
    String endpoint =
        localHost
            + port
            + "/fhir/r5/ConceptMap/$translate?url="
            + url
            + "&system="
            + system
            + "&sourceCode="
            + code;
    String content = this.restTemplate.getForObject(endpoint, String.class);
    log.info("  content = " + content);
    org.hl7.fhir.r5.model.Parameters params =
        parserR5.parseResource(org.hl7.fhir.r5.model.Parameters.class, content);
    assertNotNull(params.getParameter("result"));
    assertTrue(
        ((org.hl7.fhir.r5.model.BooleanType) params.getParameter("result").getValue())
            .booleanValue());
  }
}
