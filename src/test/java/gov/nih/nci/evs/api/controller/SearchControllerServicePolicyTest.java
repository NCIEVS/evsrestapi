package gov.nih.nci.evs.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.GraphProperties;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.service.QueryBuilderService;
import gov.nih.nci.evs.api.util.RESTUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class SearchControllerServicePolicyTest {

  @Test
  void bothTextPlainRoutesRejectServiceBeforeForwarding() throws Exception {
    SearchController controller = new SearchController();
    TerminologyUtils terminologyUtils = mock(TerminologyUtils.class);
    OpensearchQueryService index = mock(OpensearchQueryService.class);
    QueryBuilderService queryBuilder = mock(QueryBuilderService.class);
    RESTUtils graphClient = mock(RESTUtils.class);
    Terminology terminology = mock(Terminology.class);
    when(terminologyUtils.getIndexedTerminology(eq("ncit"), eq(index), eq(true)))
        .thenReturn(terminology);
    when(terminology.getSource()).thenReturn("http://example.org/source");
    when(queryBuilder.prepSparql(eq(terminology), any(), any())).thenAnswer(i -> i.getArgument(1));
    controller.termUtils = terminologyUtils;
    controller.osQueryService = index;
    controller.queryBuilderService = queryBuilder;
    controller.graphProperties = mock(GraphProperties.class);
    ReflectionTestUtils.setField(controller, "restUtils", graphClient);

    for (String query :
        new String[] {
          "SELECT * WHERE { SERVICE <http://127.0.0.1:9999/canary> { ?s ?p ?o } }",
          "SELECT * WHERE { { SELECT * WHERE { SERVICE <http://127.0.0.1:9999/canary>"
              + " { ?s ?p ?o } } } }",
          "SELECT * WHERE { SERVICE SILENT <http://127.0.0.1:9999/canary> { ?s ?p ?o } }",
          "SELECT * WHERE { VALUES ?endpoint { <http://127.0.0.1:9999/canary> }"
              + " SERVICE ?endpoint { ?s ?p ?o } }",
          "SELECT * WHERE { SERVICE <http://127.0.0.1:9999/%63anary> { ?s ?p ?o } }"
        }) {
      ResponseStatusException raw =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.getSparqlBindings("ncit", null, null, false, query, null));
      assertEquals(HttpStatus.BAD_REQUEST, raw.getStatusCode());
      ResponseStatusException concept =
          assertThrows(
              ResponseStatusException.class,
              () ->
                  controller.searchSingleTerminologySparql("ncit", false, query, null, null, null));
      assertEquals(HttpStatus.BAD_REQUEST, concept.getStatusCode());
    }
    verifyNoInteractions(graphClient);
  }
}
