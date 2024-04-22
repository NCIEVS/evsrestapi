package gov.nih.nci.evs.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ApplicationTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ApplicationTests.class);

  @Autowired ApplicationContext context;

  @Test
  public void contextLoads() {
    assertThat(this.context).isNotNull();
    log.debug("context loaded successfully");
  }

  /**
   * Test version.
   *
   * @throws Exception the exception
   */
  @Test
  public void testVersion() throws Exception {
    // Verify that the build.gradle version matches the version in
    // VersionController

    final File bgf = new File("build.gradle");
    final File vcf =
        new File("src/main/java/gov/nih/nci/evs/api/controller/VersionController.java");
    assertTrue(bgf.exists());
    final String version =
        FileUtils.readLines(bgf, "UTF-8").stream()
            .filter(s -> s.matches("^version =.*"))
            .map(s -> s.replaceFirst("version = ", ""))
            .findFirst()
            .get();
    assertTrue(vcf.exists());

    final boolean flag =
        FileUtils.readLines(vcf, "UTF-8").stream()
                .filter(s -> s.contains("\"" + version + "\""))
                .count()
            > 0;
    if (!flag) {
      log.error(
          "Unexpected mismatch between build.gradle version and VersionController verion = "
              + version);
    }
    assertTrue(!flag);
  }
}
