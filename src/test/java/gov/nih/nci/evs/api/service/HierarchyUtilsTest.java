
package gov.nih.nci.evs.api.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.configuration.TestConfiguration;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.HierarchyUtils;

/**
 * Unit test for {@link HierarchyUtils}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class HierarchyUtilsTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(HierarchyUtilsTest.class);

  /**
   * Test model serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelSerialization() throws Exception {
    String s = "{\"hierarchyRoots\": [\"10000647\",\"10017886\"]}";
    new ObjectMapper().readValue(s, HierarchyUtils.class);

    s = "{\"name\": \"hierarchy\",\"hierarchy\": {\"hierarchyRoots\": [\"10000647\",\"10030209\"]},\"paths\": null,\"concepts\": [],\"conceptMinimals\": [],\"associationEntries\": null}";
    new ObjectMapper().readValue(s, ElasticObject.class);

  }
}
