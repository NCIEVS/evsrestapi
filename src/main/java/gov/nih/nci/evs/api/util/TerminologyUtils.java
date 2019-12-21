
package gov.nih.nci.evs.api.util;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;

/**
 * Utilities for handling the "include" flag, and converting EVSConcept to
 * Concept.
 */
public final class TerminologyUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger =
      LoggerFactory.getLogger(TerminologyUtils.class);

  @Autowired
  static SparqlQueryManagerService sparqlQueryManagerService;

  /**
   * Instantiates an empty {@link TerminologyUtils}.
   */
  private TerminologyUtils() {
    // n/a
  }

  /**
   * Returns the terminologies.
   *
   * @return the terminologies
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static List<Terminology> getTerminologies() throws IOException {
    // TODO: Next step for this is to use approaches in MetadataUtils
    // to discover the ontologies loaded into stardog (via sparql).
    final Terminology monthlyNcit =
        new Terminology(sparqlQueryManagerService.getEvsVersionInfo("monthly"));
    monthlyNcit.getTags().put("monthly", "true");

    final Terminology weeklyNcit =
        new Terminology(sparqlQueryManagerService.getEvsVersionInfo("weekly"));

    // If these are equal, there's only one version, return it
    if (monthlyNcit.equals(weeklyNcit)) {
      // Monthly is the latest published version
      monthlyNcit.setLatest(true);
      return EVSUtils.asList(monthlyNcit);
    }
    // Otherwise, there are two versions
    else {
      // Monthly is the latest published version
      monthlyNcit.setLatest(true);
      weeklyNcit.getTags().put("weekly", "true");
      weeklyNcit.setLatest(false);
      return EVSUtils.asList(monthlyNcit, weeklyNcit);
    }
  }

  /**
   * Returns the terminology.
   *
   * @param terminology the terminology
   * @return the terminology
   * @throws Exception the exception
   */
  public static Terminology getTerminology(final String terminology)
    throws Exception {
    for (final Terminology t : getTerminologies()) {
      if (t.getTerminology().equals(terminology) && t.getLatest() != null
          && t.getLatest()) {
        return t;
      } else if (t.getTerminologyVersion().equals(terminology)) {
        return t;
      }
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
        terminology + " not found");
  }

}
