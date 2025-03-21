package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.support.ApplicationVersion;

/** Integration tests for VersionController. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class VersionControllerTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(VersionControllerTests.class);

  /** The mvc. */
  @Autowired private MockMvc mvc;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The object mapper. */
  private ObjectMapper objectMapper;

  /** The base url. */
  private String baseUrl = "/api/v1";

  /** Sets the up. */
  @BeforeEach
  public void setUp() {
    /*
     * Configure the JacksonTester object
     */
    this.objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);
  }

  /**
   * Test get trailing slash version.
   *
   * @throws Exception the exception
   */
  @Test
  /**
   * test get trailing slash 404
   *
   * @throws Exception
   */
  public void testGetTrailingSlashVersion() throws Exception {
    String url = baseUrl + "/version/";
    log.info("Testing url - " + url);
    mvc.perform(get(url)).andExpect(status().isNotFound()).andReturn();
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
    final ApplicationVersion data = new ObjectMapper().readValue(content, ApplicationVersion.class);
    final String buildGradleVersion = getBuildGradleVersion();
    assertThat(data.getVersion()).isEqualTo(buildGradleVersion);
  }

  /**
   * Returns the builds the gradle version.
   *
   * @return the builds the gradle version
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
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

  /**
   * Returns the builds the gradle version.
   *
   * @return the builds the gradle version
   * @throws Exception the exception
   */
  @Test
  public void testGetSwaggerUIVersion() throws Exception {

    // Get swagger-ui version
    String swaggerUiVersion = null;

    // Load .jar file from classpath
    final String classpath = System.getProperty("java.class.path");
    final String[] classPathValues = classpath.split(File.pathSeparator);
    for (final String cp : classPathValues) {
      if (cp.contains("org.webjars") && cp.contains("swagger-ui")) {
        if (swaggerUiVersion != null) {
          fail("Unexpected multiple swagger ui versions = " + swaggerUiVersion + ", " + cp);
        }
        swaggerUiVersion =
            classpath
                .replaceFirst(".*org.webjars.*swagger-ui.([\\d\\.]+).*", "$1")
                .replaceFirst("\\.$", "");
      }
    }

    if (swaggerUiVersion == null || swaggerUiVersion.isEmpty()) {
      throw new IllegalStateException("org.webjars.*swagger-ui not found in classpath");
    }
    log.info("  swaggerUiVersion = " + swaggerUiVersion);

    // Get path to version
    String path = "src/main/resources/META-INF/resources/webjars/swagger-ui/" + swaggerUiVersion;

    // Check if the path exists and the versions match
    assertThat(Files.exists(Paths.get(path)))
        .withFailMessage(
            "The Swagger UI version does not match the folder structure: expected version %s at"
                + " path %s",
            swaggerUiVersion, path)
        .isTrue();

    // Verify that "pom.properties" and "pom.xml" reference this same version
    final String v = swaggerUiVersion;
    if (FileUtils.readLines(
                new File("src/main/resources/META-INF/maven/org.webjars/swagger-ui/pom.properties"),
                "UTF-8")
            .stream()
            .filter(s -> s.contains(v))
            .count()
        == 0) {
      fail(
          "src/main/resources/META-INF/maven/org.webjars/swagger-ui/pom.properties"
              + " does not contain the expected swagger version");
    }

    if (FileUtils.readLines(
                new File("src/main/resources/META-INF/maven/org.webjars/swagger-ui/pom.xml"),
                "UTF-8")
            .stream()
            .filter(s -> s.contains(v))
            .count()
        == 0) {
      fail(
          "src/main/resources/META-INF/maven/org.webjars/swagger-ui/pom.properties"
              + " does not contain the expected swagger version");
    }
    if (FileUtils.readLines(new File("src/main/resources/META-INF/MANIFEST.MF"), "UTF-8").stream()
            .filter(s -> s.contains(v))
            .count()
        == 0) {
      fail(
          "src/main/resources/META-INF/MANIFEST.MF"
              + " does not contain the expected swagger version");
    }
  }
}
