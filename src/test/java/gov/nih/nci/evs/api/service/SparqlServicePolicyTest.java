package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SparqlServicePolicyTest {

  @Test
  void rejectsServiceAtAnyDepthAndWithAnyTarget() {
    String target = "<http://127.0.0.1:9999/canary>";
    assertTrue(
        SparqlServicePolicy.containsService(
            "SELECT * WHERE { SERVICE " + target + " { ?s ?p ?o } }"));
    assertTrue(
        SparqlServicePolicy.containsService(
            "SELECT * WHERE { { SELECT * WHERE { SERVICE " + target + " { ?s ?p ?o } } } }"));
    assertTrue(
        SparqlServicePolicy.containsService(
            "SELECT * WHERE { SERVICE SILENT " + target + " { ?s ?p ?o } }"));
    assertTrue(
        SparqlServicePolicy.containsService(
            "SELECT * WHERE { VALUES ?endpoint { "
                + target
                + " } SERVICE ?endpoint { ?s ?p ?o } }"));
  }

  @Test
  void allowsReadsAndServiceTextInCommentsOrLiterals() {
    assertFalse(SparqlServicePolicy.containsService("SELECT * WHERE { ?s ?p ?o } LIMIT 1"));
    assertFalse(
        SparqlServicePolicy.containsService(
            "SELECT * WHERE { ?s ?p \"SERVICE\" . # SERVICE <http://example.org>\n }"));
  }
}
