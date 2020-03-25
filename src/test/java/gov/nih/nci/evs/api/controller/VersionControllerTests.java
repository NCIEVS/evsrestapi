
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.support.ApplicationVersion;

/**
 * The Class VersionControllerTests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class VersionControllerTests {

  /** The logger. */
  private static final Logger log =
      LoggerFactory.getLogger(VersionControllerTests.class);

  /** The mvc. */
  @Autowired
  private MockMvc mvc;

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
    /*
     * Configure the JacksonTester object
     */
    this.objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  /**
   * Test version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testVersion() throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;

    url = baseUrl + "/version";
    log.info("Testing url - " + url);

    // Test a basic term search
    result = this.mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    assertThat(content).isNotEmpty();
    final ApplicationVersion data =
        new ObjectMapper().readValue(content, ApplicationVersion.class);
    final String buildGradleVersion = getBuildGradleVersion();
    assertThat(data.getVersion()).isEqualTo(buildGradleVersion);

  }

  /**
   * Returns the builds the gradle version.
   *
   * @return the builds the gradle version
   * @throws Exception the exception
   */
  private String getBuildGradleVersion() throws Exception {
    final LineIterator iter = FileUtils.lineIterator(new File("build.gradle"), "UTF-8");

    while (iter.hasNext()) {
      final String line = iter.next();
      if (line.matches("^version\\s+=\\s+\".*")) {
        return line.replaceFirst("version\\s+=\\s+", "").replaceAll("\"", "");
      }
    }

    return null;
  }

}
