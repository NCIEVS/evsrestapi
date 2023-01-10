
package gov.nih.nci.evs.api.controller;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * ChEBI samples tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ChebiSampleTest extends SampleTest {

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    loadSamples("chebi", "src/test/resources/samples/chebi-samples.txt");
  }

}