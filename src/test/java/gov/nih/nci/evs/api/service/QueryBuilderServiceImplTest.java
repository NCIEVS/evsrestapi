package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.GraphProperties;
import gov.nih.nci.evs.api.util.RESTUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Unit test for {@link QueryBuilderServiceImpl}. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class QueryBuilderServiceImplTest {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(QueryBuilderServiceImplTest.class);

  /** The service. */
  @Autowired QueryBuilderService service;

  /** The graph properties. */
  @Autowired GraphProperties graphProperties;

  /** The term utils. */
  @Autowired TerminologyUtils termUtils;

  /** The es query service. */
  @Autowired ElasticQueryService esQueryService;

  /** The rest utils. */
  private RESTUtils restUtils = null;

  /**
   * Post init.
   *
   * @throws Exception the exception
   */
  @PostConstruct
  public void postInit() throws Exception {
    restUtils =
        new RESTUtils(
            graphProperties.getUsername(),
            graphProperties.getPassword(),
            graphProperties.getReadTimeout(),
            graphProperties.getConnectTimeout());
  }

  /**
   * Test prep sparql.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPrepSparql() throws Exception {

    log.info("  get ncit terminology");
    Terminology ncit = termUtils.getIndexedTerminology("ncit", esQueryService);
    log.info("    terminology = " + ncit);

    for (final String query :
        new String[] {
          // Simple
          "SELECT ?code { ?x a owl:Class . ?x :NHC0 ?code .?x :P108 \"Melanoma\" }",
          // Simple with WHERE
          "SELECT ?code WHERE { ?x a owl:Class . ?x :NHC0 ?code .?x :P108 \"Melanoma\" }",
          // Spacing variation
          "SELECT?code{ ?x a owl:Class . ?x :NHC0 ?code .?x :P108 \"Melanoma\"}",
          // Simple with GRAPH
          "SELECT ?code { GRAPH <http://NCI_T_monthly> { ?x a owl:Class .  ?x :NHC0 ?code . ?x"
              + " :P108 \"Melanoma\" }  }",
          // Simple with GRAPH spacing variation
          "SELECT?code{GRAPH<http://NCI_T_monthly>{?x a owl:Class .  ?x :NHC0 ?code . ?x :P108"
              + " \"Melanoma\"}}",
          // Simple with GRAPH with a newline
          "SELECT ?code {\n"
              + " GRAPH <http://NCI_T_monthly> { ?x a owl:Class .  ?x :NHC0 ?code . ?x :P108"
              + " \"Melanoma\" }  }",
          // Simple with WHERE and GRAPH with a newline
          "SELECT ?code WHERE {\n"
              + " GRAPH <http://NCI_T_monthly> { ?x a owl:Class .  ?x :NHC0 ?code . ?x :P108"
              + " \"Melanoma\" }  }",
          // Simple with GRAPH with prefixes (and newline)
          "PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#> \n"
              + "SELECT ?code {GRAPH <http://NCI_T_monthly> { ?x a owl:Class .  ?x :NHC0 ?code . ?x"
              + " :P108 \"Melanoma\" }  }",
          // Multiple fields
          "SELECT ?x ?code {\n"
              + " GRAPH <http://NCI_T_monthly> { ?x a owl:Class .  ?x :NHC0 ?code . ?x :P108"
              + " \"Melanoma\" }  }",
        }) {
      log.info("  query = \n  " + query);
      final String query2 = service.prepSparql(ncit, query);

      log.info("  clean = \n  " + query2);

      // we should have a graph declaration in the query
      assertTrue(query2.toLowerCase().contains("graph"));

      // Test that this query runs
      restUtils.runSPARQL(query2, graphProperties.getQueryUrl(), 30000);
    }
  }

  /**
   * Test prep bad sparql.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPrepBadSparql() throws Exception {

    log.info("  get ncit terminology");
    Terminology ncit = termUtils.getIndexedTerminology("ncit", esQueryService);
    log.info("    terminology = " + ncit);

    for (final String query :
        new String[] {
          // Extra }
          "SELECT ?code { ?x a owl:Class . ?x :NHC0 ?code .?x :P108 } \"Melanoma\" }",
          // Comma in columns
          "SELECT ?x, ?code WHERE { ?x a owl:Class . ?x :NHC0 ?code .?x :P108 \"Melanoma\" }",
          //           Too long (too many results)
          //          "SELECT?code { ?x a owl:Class . ?x :NHC0 ?code . ?x rdfs:subClassOf ?y . ?y
          // :NHC0 ?ycode . }"
        }) {
      log.info("  query = \n  " + query);
      final String query2 = service.prepSparql(ncit, query);

      log.info("  clean = \n  " + query2);

      // we should have a graph declaration in the query
      assertTrue(query2.toLowerCase().contains("graph"));

      // Test that this query runs
      try {
        restUtils.runSPARQL(query2, graphProperties.getQueryUrl(), 30000);
        fail("Unexpected success");
      } catch (Exception e) {
        // n/a
      }
    }
  }
}
