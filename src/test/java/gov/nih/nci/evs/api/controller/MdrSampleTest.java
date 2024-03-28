
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.properties.ApplicationProperties;

/**
 * NCIt samples test.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MdrSampleTest extends SampleTest {

  /** The application properties. */
  @Autowired
  ApplicationProperties applicationProperties;

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MdrSampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired
  private MockMvc testMvc;

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    loadSamples("mdr", "src/test/resources/samples/mdr-samples.txt");
  }

  /**
   * Test concept active status.
   *
   * @throws Exception the exception
   */
  @Test
  public void testActive() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;

    // Test active
    url = "/api/v1/concept/mdr/10062368";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url).header("X-EVSRESTAPI-License-Key", applicationProperties.getUiLicense()))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("10062368");
    assertThat(concept.getTerminology()).isEqualTo("mdr");
    assertThat(concept.getActive()).isTrue();

    // Test inactive
    url = "/api/v1/concept/mdr/10002614?include=full";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url).header("X-EVSRESTAPI-License-Key", applicationProperties.getUiLicense()))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    concept = new ObjectMapper().readValue(content, Concept.class);
    assertThat(concept).isNotNull();
    assertThat(concept.getCode()).isEqualTo("10002614");
    assertThat(concept.getTerminology()).isEqualTo("mdr");
    assertThat(concept.getActive()).isFalse();
    assertThat(concept.getConceptStatus()).isEqualTo("Retired_Concept");

    // test that "Retired_Concept" was added to the list of concept statuses
    url = "/api/v1/metadata/terminologies?terminology=mdr&latest=true";
    log.info("Testing url - " + url);

    result = testMvc.perform(get(url).header("X-EVSRESTAPI-License-Key", applicationProperties.getUiLicense()))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    List<Terminology> list = new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
      // n/a
    });
    assertThat(list).isNotEmpty();
    Terminology term = list.get(0);
    assertThat(term.getMetadata().getConceptStatuses()).isNotEmpty();
    assertThat(term.getMetadata().getConceptStatuses()).contains("Retired_Concept");
  }

  /**
   * MDR terminology basic tests.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMDRTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    // test if mdr term exists
    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
          // n/a
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("mdr")).count()).isEqualTo(1);
    final Terminology mdr = terminologies.stream().filter(t -> t.getTerminology().equals("mdr")).findFirst().get();
    assertThat(mdr.getTerminology()).isEqualTo("mdr");
    assertThat(mdr.getMetadata().getUiLabel()).isEqualTo("MedDRA: Medical Dictionary for Regulatory Activities");
    assertThat(mdr.getName()).isEqualTo("MedDRA: Medical Dictionary for Regulatory Activities 23_1");
    assertThat(mdr.getDescription()).isNotEmpty();
    assertThat(mdr.getMetadata().getLoader()).isEqualTo("rrf");
    assertThat(mdr.getMetadata().getSourceCt()).isEqualTo(1);
    assertThat(mdr.getMetadata().getLicenseText()).isNotNull();
    assertThat(mdr.getDescription()).isEqualTo(
        ";;MedDRA MSSO;;MedDRA [electronic resource]" + " : Medical Dictionary for Regulatory Activities Terminology;;;"
            + "Version 23.1;;MedDRA MSSO;;September, 2020;;;;MedDRA "
            + "[electronic resource] : Medical Dictionary for Regulatory Activities Terminology");

    assertThat(mdr.getLatest()).isTrue();

    // Load from config
    final JsonNode node = getMetadataAsNode("mdr");
    final TerminologyMetadata metadata = new ObjectMapper().treeToValue(node, TerminologyMetadata.class);
    log.info("XXX2 = " + metadata.getLicenseCheck());
  }

  /**
   * Returns the metadata as node.
   *
   * @param terminology the terminology
   * @return the metadata as node
   * @throws Exception the exception
   */
  public JsonNode getMetadataAsNode(final String terminology) throws Exception {
    final String uri = applicationProperties.getConfigBaseUri() + "/" + terminology + ".json";
    final URL url = new URL(uri);
    try (final InputStream is = url.openConnection().getInputStream()) {
      return new ObjectMapper().readTree(IOUtils.toString(is, "UTF-8"));
    }
  }
}
