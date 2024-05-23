package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Terminology;
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

/** GO samples tests. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MouseAnatomySampleTest extends SampleTest {

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MouseAnatomySampleTest.class);

  /** The test mvc. Used by CheckZzz methods to avoid taking as a param. */
  @Autowired private MockMvc testMvc;

  @BeforeClass
  public static void setupClass() throws Exception {
    loadSamples("ma", "src/test/resources/samples/ma-samples.txt");
  }

  /**
   * Test terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTerminology() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);
    result =
        testMvc
            .perform(get(url).param("latest", "true").param("terminology", "ma"))
            .andExpect(status().isOk())
            .andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);

    final List<Terminology> terminologies =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Terminology>>() {
                  // n/a
                });
    assertThat(terminologies.size()).isGreaterThan(0);
    assertThat(terminologies.stream().filter(t -> t.getTerminology().equals("ma")).count())
        .isEqualTo(1);
    final Terminology ma =
        terminologies.stream().filter(t -> t.getTerminology().equals("ma")).findFirst().get();
    assertThat(ma.getTerminology()).isEqualTo("ma");
    assertThat(ma.getMetadata().getUiLabel())
        .isEqualTo("Mouse Anatomy: Anatomical Dictionary for the Adult Mouse");
    assertThat(ma.getName())
        .isEqualTo("Mouse Anatomy: Anatomical Dictionary for the Adult Mouse 2022-07-11");

    assertThat(ma.getDescription()).isNull();
    assertThat(ma.getMetadata().getLoader()).isEqualTo("rdf");
    assertThat(ma.getMetadata().getSourceCt()).isEqualTo(0);
    assertThat(ma.getMetadata().getLicenseText()).isNull();

    assertThat(ma.getLatest()).isTrue();
  }
}
