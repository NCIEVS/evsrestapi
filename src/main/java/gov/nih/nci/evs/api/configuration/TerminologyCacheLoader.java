package gov.nih.nci.evs.api.configuration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;

/**
 * Terminology cache loader to load spring cache by making calls to
 * {@code SparqlQueryManagerService} when application is ready
 * 
 * 
 * @author Arun
 *
 */
@Component
public class TerminologyCacheLoader implements ApplicationListener<ApplicationReadyEvent> {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(TerminologyCacheLoader.class);

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    log.debug("onApplicationEvent() = " + event);
    try {
      List<Terminology> terminologies = sparqlQueryManagerService.getTerminologies();
      if (CollectionUtils.isEmpty(terminologies))
        return;
      for (Terminology terminology : terminologies) {
        log.info("Start populating cache - " + terminology.getTerminologyVersion());

        // results from following calls are auto cached using ehcache managed by
        // spring
        log.info("  get hierarchy ");
        sparqlQueryManagerService.getHierarchyUtils(terminology);

        log.info("  find paths ");
        sparqlQueryManagerService.getPaths(terminology);

        log.info("  get unique sources ");
        sparqlQueryManagerService.getUniqueSourcesList(terminology);

        log.info("  get qualifiers ");
        sparqlQueryManagerService.getAllQualifiers(terminology, new IncludeParam("minimal"));

        log.info("Done populating cache - " + terminology.getTerminologyVersion());
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
