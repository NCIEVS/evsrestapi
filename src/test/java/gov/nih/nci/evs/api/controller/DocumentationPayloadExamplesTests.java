
package gov.nih.nci.evs.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * Integration tests for generating documentation payload examples.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DocumentationPayloadExamplesTests {

  /** The logger. */
  private static final Logger log =
      LoggerFactory.getLogger(DocumentationPayloadExamplesTests.class);

  /** The mvc. */
  @Autowired
  private MockMvc mvc;

  /** The test properties. */
  @Autowired
  TestProperties testProperties;

  /** The urls. */
  private List<String> urls = new ArrayList<>();

  /**
   * Sets the up.
   */
  @Before
  public void setUp() {

    urls.add("/api/v1/metadata/terminologies");
    urls.add("/api/v1/concept/ncit/C3224?include=minimal");
    urls.add("/api/v1/concept/ncit/C3224?include=summary");
    urls.add(
        "/api/v1/concept/ncit/C3224?include=synonyms,children,maps,inverseAssociations");
    urls.add("/api/v1/concept/ncit/C3224/roles");
    urls.add("/api/v1/concept/ncit/C3224?include=full");
    urls.add("/api/v1/concept/ncit/C3224/pathsToRoot");
    urls.add("/api/v1/concept/ncit/C3224/children");
    urls.add("/api/v1/concept/ncit/C3224/descendants?maxLevel=3");
    urls.add("/api/v1/concept/ncit/roots");
    urls.add("/api/v1/metadata/ncit/associations?include=minimal");
    urls.add("/api/v1/metadata/ncit/association/A8");
    // urls.add(
    // "/api/v1/concept/ncit/search?term=melanoma&include=summary,highlights&fromRecord=0&pageSize=1");

  }

  /**
   * Test include.
   *
   * @throws Exception the exception
   */
  @Test
  public void testIncludeMinimal() throws Exception {

    MvcResult result = null;
    String content = null;
    // Concept concept = null;

    // Test with "minimal"
    for (final String url : urls) {
      log.info("Testing url - " + url);
      result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
      content = result.getResponse().getContentAsString();
      final ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(Include.NON_EMPTY);
      log.info(" content = " + mapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(mapper.readTree(content)));
    }
  }

}