package gov.nih.nci.evs.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.properties.TestProperties;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
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
   * Test replacements with a NCIt codes.
   *
   * @param code the code
   * @param expectedAction the expected action
   * @param expectedReplacementCode the expected replacement code
   * @throws Exception the exception
   */
  @ParameterizedTest
  @CsvFileSource(
      resources = "/historyControllerParamTestSamples/ncit_replacements_params.csv",
      numLinesToSkip = 1)
  public void testReplacementsWithNCItCodes(
      String code, String expectedAction, String expectedReplacementCode) throws Exception {
    // Arrange
    String url = baseUrl + "/history/ncit/" + code + "/replacements";
    log.info("Testing url - " + url);

    // Act
    MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    List<History> list =
        objectMapper.readValue(
            content,
            new TypeReference<List<History>>() {
              // n/a
            });

    // Assert
    assertNotNull(list);
    assertEquals(1, list.size());
    assertEquals(code, list.get(0).getCode());
    assertEquals(expectedAction, list.get(0).getAction());
    if (expectedReplacementCode != null) {
      assertEquals(expectedReplacementCode, list.get(0).getReplacementCode());
    }
  }

  /**
   * Test replacements with NCIm codes.
   *
   * @param code the code
   * @param expectedAction the expected action
   * @param expectedReplacementCode the expected replacement code
   * @throws Exception the exception
   */
  @ParameterizedTest
  @CsvFileSource(
      resources = "/historyControllerParamTestSamples/ncim_replacements_params.csv",
      numLinesToSkip = 1)
  public void testReplacementsWithNCImCodes(
      String code, String expectedAction, String expectedReplacementCode) throws Exception {
    // Arrange
    String url = baseUrl + "/history/ncim/" + code + "/replacements";
    log.info("Testing url - " + url);

    // Act
    MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    List<History> list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });

    // Assert
    assertNotNull(list);
    assertEquals(1, list.size());
    assertEquals(code, list.get(0).getCode());
    assertEquals(expectedAction, list.get(0).getAction());
    if (expectedReplacementCode != null) {
      assertEquals(expectedReplacementCode, list.get(0).getReplacementCode());
    }
  }

  /**
   * Test replacements list with a list of NCIt codes.
   *
   * @param codeList the code list
   * @param expectedAction the expected action
   * @param expectedReplacementCode the expected replacement code
   * @param size the size
   * @throws Exception the exception
   */
  @ParameterizedTest
  @CsvFileSource(
      resources = "/historyControllerParamTestSamples/ncit_replacementsList_params.csv",
      numLinesToSkip = 1)
  public void testReplacementsListWithNCItCodes(
      String codeList, String expectedAction, String expectedReplacementCode, int size)
      throws Exception {
    // Arrange
    List<String> codes = Arrays.asList(codeList.split(","));
    String url = baseUrl + "/history/ncit/replacements?list=" + codeList;
    log.info("Testing url - " + url);

    // Act
    MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info(" content = " + content);
    List<History> list =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<History>>() {
                  // n/a
                });

    // Assert
    assertNotNull(list);
    assertEquals(size, list.size());
    assertEquals(codes.get(0), list.get(0).getCode());
    assertEquals(expectedAction, list.get(0).getAction());
    if (expectedReplacementCode != null) {
      assertEquals(expectedReplacementCode, list.get(0).getReplacementCode());
    }
  }
}
