
package gov.nih.nci.evs.api.controller;

import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * Integration tests for MetadataController.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptControllerExtensionTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ConceptControllerExtensionTests.class);

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

    objectMapper = new ObjectMapper();
    JacksonTester.initFields(this, objectMapper);

    baseUrl = "/api/v1/concept";
  }

  /**
   * Test broad category set.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBroadCategorySet() throws Exception {
    testHelper("extensions-test-broad-category-set.txt",
        "extensions-test-broad-category-result.txt");
  }

  /**
   * Test main type set.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMainTypeSet() throws Exception {
    testHelper("extensions-test-main-type-set.txt", "extensions-test-main-type-result.txt");
  }

  /**
   * Test targeted set.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTargetedSet() throws Exception {
    testHelper("extensions-test-targeted-set.txt", "extensions-test-targeted-result.txt");
  }

  /**
   * Test full set. This doesn't need to be kept around as an actively running
   * test The other tests are sufficient.
   * @throws Exception the exception
   */
  // @Test
  public void testFullSet() throws Exception {
    testHelper("extensions-test-full-set.txt", "extensions-test-full-result.txt");
  }

  /**
   * Test helper.
   *
   * @param codeSetPath the code set path
   * @param resultPath the result path
   * @throws Exception the exception
   */
  public void testHelper(final String codeSetPath, final String resultPath) throws Exception {
    String url = null;
    MvcResult result = null;
    String content = null;
    Concept concept = null;
    final Map<String, String> map = new HashMap<>();
    final ObjectMapper mapper = new ObjectMapper();
    int ct = 0;
    for (final String code : IOUtils
        .readLines(getClass().getClassLoader().getResourceAsStream(codeSetPath), "UTF-8")) {
      url = baseUrl + "/ncit/" + code + "?include=extensions";
      result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
      content = result.getResponse().getContentAsString();
      concept = new ObjectMapper().readValue(content, Concept.class);
      // For comparing main menu ancestors, null all concept fields in paths
      // except for the codes
      concept.getExtensions().getMainMenuAncestors().stream().flatMap(p -> p.getPaths().stream())
          .flatMap(p -> p.getConcepts().stream()).peek(c -> c.setName(null))
          .peek(c -> c.setTerminology(null)).peek(c -> c.setLevel(null))
          .peek(c -> c.setVersion(null)).count();
      final String pretty = mapper.writerWithDefaultPrettyPrinter()
          .writeValueAsString(concept.getExtensions()).replaceAll("\r", "") + "\n";
      map.put(code, pretty);
      if (++ct % 1000 == 0) {
        log.info("  count = " + ct);
      }
    }

    // Test the result
    compare(map, getResults(resultPath));
  }

  /**
   * Compare.
   *
   * @param map the map
   * @param cmpMap the cmp map
   * @throws Exception the exception
   */
  private void compare(final Map<String, String> map, Map<String, String> cmpMap) throws Exception {
    boolean error = false;
    if (map.size() != cmpMap.size()) {
      fail("Map size does not match comparison map size = " + map.size() + ", " + cmpMap.size());
    }
    int matchCt = 0;
    int mismatchCt = 0;
    for (final String code : map.keySet()) {
      final ObjectMapper mapper = new ObjectMapper();
      final JsonNode node = mapper.readTree(map.get(code));
      final JsonNode cmpNode = mapper.readTree(cmpMap.get(code));
      final StringBuilder sb = new StringBuilder();
      sb.append("\n");
      boolean match = true;
      for (final String field : new String[] {
          "isDisease", "isDiseaseGrade", "isDiseaseStage", "isMainType", "isSubtype", "isBiomarker",
          "isReferenceGene"
      }) {
        final String val = node.get(field) == null ? "null" : node.get(field).asText();
        final String cmpVal = cmpNode.get(field) == null ? "null" : cmpNode.get(field).asText();
        if (!val.equals(cmpVal)) {
          sb.append("  " + field + " = " + val + ", " + cmpVal);
          match = false;
        }
        if (node.get(field) == null) {
          sb.append("  " + field + " = val IS NULL, " + cmpVal);
          match = false;
        }
      }

      final String field = "mainMenuAncestors";
      final String val = node.get(field) == null ? "null" : node.get(field).asText();
      final String cmpVal = cmpNode.get(field) == null ? "null" : cmpNode.get(field).asText();
      if (!val.equals(cmpVal)) {
        final String pretty = mapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(node.get("mainMenuAncestors"));
        final String cmpPretty = mapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(cmpNode.get("mainMenuAncestors"));
        sb.append("  MISMATCH mainMenuAncestors = \n").append("<<< map\n").append(pretty)
            .append(">>> cmpMap\n").append(cmpPretty).append("\n");
        match = false;
      }
      if (!match) {
        log.error("  MISMATCH = " + code);
        mismatchCt++;
        error = true;
        log.error(sb.toString());
      } else {
        log.info("  MATCH = " + code);
        matchCt++;
      }
    }
    log.error("  MISMATCH CT = " + mismatchCt);
    log.error("  MATCH CT = " + matchCt);
    if (error) {
      fail("Unexpected mismatches, see log");
    }
  }

  /**
   * Returns the results.
   *
   * @param path the path
   * @return the results
   * @throws Exception the exception
   */
  private Map<String, String> getResults(final String path) throws Exception {
    final Map<String, String> map = new HashMap<>();
    final List<String> lines =
        IOUtils.readLines(getClass().getClassLoader().getResourceAsStream(path), "UTF-8");
    String code = null;
    StringBuilder result = new StringBuilder();
    for (final String line : lines) {
      if (line.matches("^CONCEPT .*")) {
        // Add prior result
        if (code != null) {
          map.put(code, result.toString());
          result = new StringBuilder();
        }
        code = line.replaceFirst("CONCEPT ", "");
        continue;
      }
      result.append(line).append("\n");
    }
    // Put the final case
    map.put(code, result.toString());

    return map;
  }

  // For testing computation of extensions - no longer needed
  // @Test
  // public void testExtensions() throws Exception {
  // String url = null;
  // MvcResult result = null;
  // String content = null;
  // Concept concept = null;
  //
  // // Test a few codes to make sure they're not null
  // for (final String code : new String[] {
  // "C7280"
  // }) {
  // url = "/api/v1/extensions/" + code;
  //
  // log.info("Testing url - " + url);
  // result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
  // content = result.getResponse().getContentAsString();
  // log.info(" content = " + content);
  // concept = new ObjectMapper().readValue(content, Concept.class);
  // log.info(" extensions = " + concept.getExtensions());
  // assertThat(concept).isNotNull();
  // }
  //
  // }

  // TO USE THE ABOVE TEST
  // put this code back at the end of concept controller
  //
  // @Autowired
  // MainTypeHierarchy mainTypeHierarchy;
  //
  // @RequestMapping(method = RequestMethod.GET, value = "/extensions/{code}",
  // produces = "application/json")
  // @ApiIgnore
  // public @ResponseBody Concept calculateExtensions(@PathVariable(value =
  // "code")
  // final String code) throws Exception {
  // try {
  // final Terminology term = termUtils.getTerminology("ncit", true);
  // mainTypeHierarchy.initialize(term);
  // final IncludeParam ip = new IncludeParam("full");
  // final Concept concept = sparqlQueryManagerService.getConcept(code, term,
  // ip);
  // concept.setPaths(sparqlQueryManagerService.getPathToRoot(code, term));
  // concept.setExtensions(mainTypeHierarchy.getExtensions(concept));
  // return concept;
  // } catch (Exception e) {
  // handleException(e);
  // return null;
  // }
  // }
}
