package gov.nih.nci.evs.api.service;

import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

/** Rejects federated SERVICE clauses in client supplied SPARQL. */
public final class SparqlServicePolicy {

  private SparqlServicePolicy() {}

  /** Returns true when the parsed query contains a SERVICE clause at any depth. */
  public static boolean containsService(final String query) {
    final boolean[] found = {false};
    ElementWalker.walk(
        QueryFactory.create(query).getQueryPattern(),
        new ElementVisitorBase() {
          @Override
          public void visit(final ElementService service) {
            found[0] = true;
          }

          @Override
          public void visit(final ElementSubQuery subQuery) {
            ElementWalker.walk(subQuery.getQuery().getQueryPattern(), this);
          }
        });
    return found[0];
  }
}
