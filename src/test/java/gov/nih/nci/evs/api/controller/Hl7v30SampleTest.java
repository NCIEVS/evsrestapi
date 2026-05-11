package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** HL7V3.0 samples test. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class Hl7v30SampleTest extends SampleTest {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(Hl7v30SampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc testMvc;

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeAll
  public static void setupClass() throws Exception {
    loadSamples("hl7v30", "src/test/resources/samples/hl7v30-samples.txt");
  }

  @Test
  public void testDuplicateCodeDisambiguation() throws Exception {

    String url = "/api/v1/concept/search?terminology=hl7v30&term=41&include=parents&pageSize=20";
    log.info("Testing url - " + url);
    MvcResult result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    gov.nih.nci.evs.api.model.ConceptResultList list =
        ThreadLocalMapper.get()
            .readValue(content, gov.nih.nci.evs.api.model.ConceptResultList.class);

    assertThat(list).isNotNull();
    assertThat(list.getConcepts()).isNotNull();
    assertThat(list.getConcepts().size()).isGreaterThanOrEqualTo(2);
    assertThat(list.getConcepts()).extracting(Concept::getCode).contains("41-11255", "41-11672");

    Concept typhoid =
        list.getConcepts().stream()
            .filter(concept -> concept.getCode().equals("41-11255"))
            .findFirst()
            .orElseThrow();
    Concept tribe =
        list.getConcepts().stream()
            .filter(concept -> concept.getCode().equals("41-11672"))
            .findFirst()
            .orElseThrow();

    assertThat(typhoid.getCode()).isEqualTo("41-11255");
    assertThat(typhoid.getTerminology()).isEqualTo("hl7v30");
    assertThat(typhoid.getName()).isEqualTo("typhoid, parenteral");
    assertThat(typhoid.getParents()).isNotEmpty();
    assertThat(typhoid.getParents().size()).isEqualTo(1);
    assertThat(typhoid.getParents().get(0).getCode()).startsWith("VaccineType");

    assertThat(tribe.getCode()).isEqualTo("41-11672");
    assertThat(tribe.getTerminology()).isEqualTo("hl7v30");
    assertThat(tribe.getName()).isEqualTo("Cheyenne and Arapaho Tribes, Oklahoma");
    assertThat(tribe.getParents()).isNotEmpty();
    assertThat(tribe.getParents().size()).isEqualTo(1);
    assertThat(tribe.getParents().get(0).getCode()).isEqualTo("_NativeEntityContiguous");

    url = "/api/v1/concept/hl7v30/41";
    log.info("Testing url - " + url);
    testMvc.perform(get(url)).andExpect(status().isNotFound());

    url = "/api/v1/concept/hl7v30/41-11255?include=definitions";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    Concept differentiatedConcept = ThreadLocalMapper.get().readValue(content, Concept.class);
    assertThat(differentiatedConcept.getDefinitions())
        .extracting(definition -> definition.getDefinition())
        .contains(
            "The code for this concept is reused across multiple contexts and so a differentiating"
                + " value has been used to make the code values unique.");
  }
}
