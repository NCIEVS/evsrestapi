package gov.nih.nci.evs.api.configuration;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.service.ElasticLoadService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.HierarchyUtils;

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

  /** The stardog properties. */
  @Autowired
  StardogProperties stardogProperties;

  /** The sparql query manager service. */
  @Autowired
  SparqlQueryManagerService sparqlQueryManagerService;

  @Autowired
  ElasticLoadService loadService;
  
  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    log.debug("onApplicationEvent() = " + event);

    final ExecutorService executorService = Executors.newFixedThreadPool(4);

    final ObjectMapper mapper = new ObjectMapper();
    
    try {
      final List<Terminology> terminologies = sparqlQueryManagerService.getTerminologies();
      if (CollectionUtils.isEmpty(terminologies))
        return;

      for (final Terminology terminology : terminologies) {
        log.info("Start populating cache - " + terminology.getTerminologyVersion());

        executorService.execute(new Runnable() {
          public void run() {
            try {
              log.info("  get hierarchy ");
              HierarchyUtils hierarchy = sparqlQueryManagerService.getHierarchyUtils(terminology);
              ElasticObject hierarchyObject = new ElasticObject("hierarchy");
              hierarchyObject.setData(mapper.writeValueAsString(hierarchy));
              loadService.loadObject(hierarchyObject, terminology);
              log.info("    done hierarchy ");

              log.info("  find paths ");
              Paths paths = sparqlQueryManagerService.getPaths(terminology);
//              ElasticObject pathsObject = new ElasticObject("paths");
//              pathsObject.setPaths(paths);
//              loadService.loadObject(pathsObject, terminology);
              log.info("    done paths ");

              log.info("  get synonym sources ");
              List<ConceptMinimal> synonymSources = sparqlQueryManagerService.getSynonymSources(terminology);
              ElasticObject ssObject = new ElasticObject("synonym_sources");
              ssObject.setData(mapper.writeValueAsString(synonymSources));
              loadService.loadObject(ssObject, terminology);
              log.info("    done synonym sources ");
              
            } catch (IOException e) {
              log.error("Unexpected error caching = " + terminology, e);
            }

          }
        });

        // If "force pouplate cache" is set, then cache all the metadata
        // for both "minimal" and "summary"
        if ("Y".equals(stardogProperties.getForcePopulateCache())) {
          executorService.execute(new Runnable() {
            public void run() {
              try {

                log.info("  get qualifiers ");
//                sparqlQueryManagerService.getAllQualifiers(terminology,
//                    new IncludeParam("minimal"));
//                log.info("    done qualifiers minimal");
                List<Concept> qualifiers = sparqlQueryManagerService.getAllQualifiers(terminology,
                    new IncludeParam("summary"));
                ElasticObject conceptsObject = new ElasticObject("qualifiers");
                conceptsObject.setData(mapper.writeValueAsString(qualifiers));
                loadService.loadObject(conceptsObject, terminology);
                log.info("    done qualifiers summary");

              } catch (IOException e) {
                log.error("Unexpected error caching2 = " + terminology, e);
              }

            }
          });

          executorService.execute(new Runnable() {
            public void run() {
              try {

                log.info("  get properties ");
//                sparqlQueryManagerService.getAllProperties(terminology,
//                    new IncludeParam("minimal"));
//                log.info("    done properties minimal");
                List<Concept> properties = sparqlQueryManagerService.getAllProperties(terminology,
                    new IncludeParam("summary"));
                ElasticObject propertiesObject = new ElasticObject("properties");
                propertiesObject.setData(mapper.writeValueAsString(properties));
                loadService.loadObject(propertiesObject, terminology);
                log.info("    done properties summary");

              } catch (IOException e) {
                log.error("Unexpected error caching2 = " + terminology, e);
              }

            }
          });

          executorService.execute(new Runnable() {
            public void run() {
              try {
                log.info("  get associations ");
//                sparqlQueryManagerService.getAllAssociations(terminology,
//                    new IncludeParam("minimal"));
//                log.info("    done associations minimal");
                List<Concept> associations = sparqlQueryManagerService.getAllAssociations(terminology,
                    new IncludeParam("summary"));
                ElasticObject associationsObject = new ElasticObject("associations");
                associationsObject.setData(mapper.writeValueAsString(associations));
                loadService.loadObject(associationsObject, terminology);
                log.info("    done associations summary");

              } catch (IOException e) {
                log.error("Unexpected error caching3 = " + terminology, e);
              }

            }
          });

          executorService.execute(new Runnable() {
            public void run() {
              try {
                log.info("  get roles ");
//                sparqlQueryManagerService.getAllRoles(terminology, new IncludeParam("minimal"));
//                log.info("    done roles minimal");
                List<Concept> roles = sparqlQueryManagerService.getAllRoles(terminology, new IncludeParam("summary"));
                ElasticObject rolesObject = new ElasticObject("roles");
                rolesObject.setData(mapper.writeValueAsString(roles));
                loadService.loadObject(rolesObject, terminology);
                log.info("    done roles summary");

              } catch (IOException e) {
                log.error("Unexpected error caching = " + terminology, e);
              }

            }
          });

        }
      }
      executorService.shutdown();

      // if indicated, wait for cache population to complete
      if ("Y".equals(stardogProperties.getWaitPopulateCache())) {
        log.info("  waiting to finish");
        boolean done = false;
        int ct = 0;
        while (!done) {
          done = executorService.awaitTermination(30, TimeUnit.SECONDS);
          ct += 30;
          log.info("    wait time = " + ct);
        }
        log.info("  done populating cache");
      }

    } catch (Exception e) {
      log.error("Unexpected error caching data", e);
    }
  }

}
