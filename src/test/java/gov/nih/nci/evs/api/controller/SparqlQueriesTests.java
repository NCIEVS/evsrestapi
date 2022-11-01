
package gov.nih.nci.evs.api.controller;

import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.elasticsearch.common.util.set.Sets;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for SearchController.
 */
@RunWith(SpringRunner.class)
// @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SparqlQueriesTests {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(SparqlQueriesTests.class);

  /** The prop. */
  private static Properties prop = null;

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setup() throws Exception {
    prop = new Properties();
    prop.load(
        SparqlQueriesTests.class.getClassLoader().getResourceAsStream("sparql-queries.properties"));
  }

  /**
   * Test query fields.
   */
  @Test
  public void testQueryFields() {

    for (final Map.Entry<Object, Object> entry : prop.entrySet()) {
      final String key = entry.getKey().toString();
      final String query = entry.getValue().toString();

      // Pull the select line
      final String select = query.replaceFirst(".*(SELECT .*) GRAPH.*", "$1");
      final String graph = query.replaceFirst(".*(GRAPH.*)", "$1");

      final Set<String> selectMatches = new HashSet<>();
      final Matcher m1 = Pattern.compile("\\?[A-Za-z]+").matcher(select);
      while (m1.find()) {
        selectMatches.add(m1.group());
      }
      final Set<String> graphMatches = new HashSet<>();
      final Matcher m2 = Pattern.compile("\\?[A-Za-z]+").matcher(graph);
      while (m2.find()) {
        graphMatches.add(m2.group());
      }

      // Find things referenced in "graph" that are not in "select"
      final Set<String> selectNotGraph = Sets.difference(selectMatches, graphMatches);
      final Set<String> graphNotSelect = Sets.difference(graphMatches, selectMatches).stream()
          .filter(s -> !s.matches("\\?[xyz].*")).collect(Collectors.toSet());
      if (selectNotGraph.size() > 0) {
        logger.info("  FOUND select field not in graph = " + key);
        logger.info("    ERROR = " + selectNotGraph);
      }
      if (graphNotSelect.size() > 0) {
        logger.info("  FOUND graph field not in select = " + key);
        logger.info("    ERROR = " + graphNotSelect);
      }

    }

  }

  /**
   * In general, rdfs:label should not be used in queires.
   */
  @Test
  public void testRdfsLabel() {

    final Set<String> exceptions = Set.of("subset");
    for (final Map.Entry<Object, Object> entry : prop.entrySet()) {
      final String key = entry.getKey().toString();
      final String query = entry.getValue().toString();

      if (exceptions.contains(key)) {
        continue;
      }

      if (query.contains("rdfs:label")) {
        logger.info("  FOUND rdfs:label = " + key);
        fail("Unexpected use of 'rdfs:label' in query = " + key);
      }
    }
  }

  /**
   * Test that #{preferredNameCode} is always optional.
   */
  @Test
  public void testPreferredNameCode() {

    // This is an exception because it's always NCIt
    final Set<String> exceptions = Set.of("subset");
    boolean found = false;
    boolean error = false;
    for (final Map.Entry<Object, Object> entry : prop.entrySet()) {
      final String key = entry.getKey().toString();
      final String query = entry.getValue().toString();

      if (exceptions.contains(key)) {
        continue;
      }

      // Split on " . "
      for (final String line : query.split(" \\. ")) {

        final boolean pnc = line.contains("preferredNameCode");
        final boolean matches =
            line.matches("OPTIONAL \\{ .*#\\{preferredNameCode\\} .*Label.*\\}.*");

        if (pnc) {
          logger.info("  FOUND #{preferredNameCode} = " + key + ", " + line);
          found = true;
        }
        if (pnc && !matches) {
          logger.info("    ERROR #{preferredNameCode} = " + key + ", " + line);
          error = true;
        }
      }
    }
    if (!found) {
      fail("Unexpectedly did not find #{preferredNameCode} in any queries");
    }
    if (error) {
      fail("Unexpected use of '#{preferredNameCode}' - see log");
    }
  }

  /**
   * Test code code after preferred name code.
   */
  @Test
  public void testCodeCodeAfterPreferredNameCode() {

    // This is an exception because it's always NCIt
    final Set<String> exceptions = Set.of("associations");

    boolean error = false;
    for (final Map.Entry<Object, Object> entry : prop.entrySet()) {
      final String key = entry.getKey().toString();
      final String query = entry.getValue().toString();

      if (exceptions.contains(key)) {
        continue;
      }

      // If query contains "#{preferredNameCode} ?XXXLabel"
      // It should on the next line contain "#{codeCode} ?XXXCode"
      boolean flag = false;
      String prefix = null;
      for (final String line : query.split(" \\. ")) {

        final String expected = "#{codeCode} ?" + prefix + "Code";
        if (flag && !line.contains(expected)) {
          logger.info("    ERROR '" + expected + "' not found = " + key + ", " + line);
          error = true;
        }
        flag = line.matches(".*#\\{preferredNameCode\\} \\?[A-Za-z]+Label.*");
        prefix = line.replaceFirst(".*#\\{preferredNameCode\\} \\?([A-Za-z]+)Label.*", "$1");
        if (flag) {
          logger.info("  FOUND '#{preferredNameCode} ?" + prefix + "Label' = " + key + ", " + line);
        }
      }
    }
    if (error) {
      fail("Unexpected use of '#{codeCode} after #{preferredNameCode}' - see log");
    }
  }
}
