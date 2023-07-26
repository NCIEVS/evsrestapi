
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Terminology;

/**
 * GO samples tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class GoSampleTest extends SampleTest {

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(GoSampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired
  private MockMvc testMvc;

  @BeforeClass
  public static void setupClass() throws Exception {
    loadSamples("go", "src/test/resources/samples/go-samples.txt");
  }

  @Test
  public void testGOTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result = testMvc.perform(get(url).param("latest", "true").param("terminology", "go"))
        .andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper().readValue(content, new TypeReference<List<Terminology>>() {
        });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("go")).count())
        .isEqualTo(1);
    final Terminology go =
        terminologies.stream().filter(t -> t.getTerminology().equals("go")).findFirst().get();
    assertThat(go.getTerminology()).isEqualTo("go");
    assertThat(go.getMetadata().getUiLabel()).isEqualTo("GO: Gene Ontology");
    assertThat(go.getName()).isEqualTo("GO: Gene Ontology 2022-07-01");
    assertThat(go.getDescription()).isNotEmpty();

    assertThat(go.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(go.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(go.getMetadata().getLicenseText()).isNull();
    assertThat(go.getDescription()).isEqualTo("The Gene Ontology (GO) provides a framework and set"
        + " of concepts for describing the functions of gene products from all organisms.");

    assertThat(go.getLatest()).isTrue();
  }

}