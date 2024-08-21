package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.util.List;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** subset tests. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class HistoryControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(MetadataControllerTests.class);

  /** The mvc. */
  @Autowired private MockMvc mvc;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** The base url. */
  private String baseUrl = "";

  /** Sets the up. */
  @BeforeEach
  public void setUp() {

    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1";
  }

  /**
   * Test replacements.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReplacements() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<History> list = null;

    // NCIt "active"
    url = baseUrl + "/history/ncit/C3224/replacements";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).getAction()).isEqualTo("active");

    // NCIt "retire" - N/A only weekly will have these

    // NCIt "merge" - C13320
    url = baseUrl + "/history/ncit/C13320/replacements";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("C13320");
    assertThat(list.get(0).getAction()).isEqualTo("merge");
    assertThat(list.get(0).getReplacementCode()).isEqualTo("C12756");

    // NCIt "active" has merge but only with itself- C12756
    url = baseUrl + "/history/ncit/C12756/replacements";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("C12756");
    assertThat(list.get(0).getAction()).isEqualTo("active");

    // NCIt Active - C17610
    url = baseUrl + "/history/ncit/C17610/replacements";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("C17610");
    assertThat(list.get(0).getAction()).isEqualTo("active");

    // NCIt retire - C4631
    url = baseUrl + "/history/ncit/C4631/replacements";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("C4631");
    assertThat(list.get(0).getAction()).isEqualTo("retire");

    // NCIm DEL - C0278390
    url = baseUrl + "/history/ncim/C0278390/replacements";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("C0278390");
    assertThat(list.get(0).getAction()).isEqualTo("DEL");

    // NCIm SY - C0003962
    url = baseUrl + "/history/ncim/C0003962/replacements";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("C0003962");
    assertThat(list.get(0).getAction()).isEqualTo("SY");
    assertThat(list.get(0).getReplacementCode()).isEqualTo("C3554541");

    // NCIm SY - CL511651
    url = baseUrl + "/history/ncim/CL511651/replacements";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("CL511651");
    assertThat(list.get(0).getAction()).isEqualTo("SY");
    assertThat(list.get(0).getReplacementCode()).isEqualTo("C4273882");

    // NCIm Active - C3554541
    url = baseUrl + "/history/ncim/C3554541/replacements";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("C3554541");
    assertThat(list.get(0).getAction()).isEqualTo("active");
  }

  /**
   * Test replacements list.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReplacementsList() throws Exception {

    String url = null;
    MvcResult result = null;
    String content = null;
    List<History> list = null;

    // NCIt "active"
    url = baseUrl + "/history/ncit/replacements?list=C3224";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).getCode()).isEqualTo("C3224");
    assertThat(list.get(0).getAction()).isEqualTo("active");

    // NCIt "retire" - N/A only weekly will have these

    // NCIt "merge" - C13320
    url = baseUrl + "/history/ncit/replacements?list=C169836,C3224";
    log.info("Testing url - " + url);
    result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(0).getCode()).isEqualTo("C169836");
    assertThat(list.get(0).getAction()).isEqualTo("merge");
    assertThat(list.get(0).getReplacementCode()).isEqualTo("C120319");
  }
}
