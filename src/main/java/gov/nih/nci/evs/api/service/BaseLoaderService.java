package gov.nih.nci.evs.api.service;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * The service to load concepts to Elasticsearch
 * 
 * Retrieves concepts from stardog and creates necessary index on Elasticsearch
 * 
 * @author Arun
 *
 */
public abstract class BaseLoaderService implements ElasticLoadService {
	
  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(BaseLoaderService.class);
  
  /** The Elasticsearch operations service instance *. */
  @Autowired
  ElasticOperationsService operationsService;

  /** The elasticsearch query service *. */
  @Autowired
  private ElasticQueryService esQueryService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired
  private TerminologyUtils termUtils;
  
  /**
   * build config object from command line options.
   *
   * @param cmd the command line object
   * @param defaultLocation the default download location to use
   * @return the config object
   */
  public static ElasticLoadConfig buildConfig(CommandLine cmd, String defaultLocation) {
    ElasticLoadConfig config = new ElasticLoadConfig();

    config.setTerminology(cmd.getOptionValue('t'));
    config.setRealTime(cmd.hasOption('r'));
    config.setForceDeleteIndex(cmd.hasOption('f'));
    if (cmd.hasOption('l')) {
      String location = cmd.getOptionValue('l');
      if (StringUtils.isBlank(location)) {
        logger.error("Location is empty!");

      }
      if (!location.endsWith("/")) {
        location += "/";
      }
      logger.info("location - {}", location);
      config.setLocation(location);
    } else {
      config.setLocation(defaultLocation);
    }
    if (cmd.hasOption('d')) {
        String location = cmd.getOptionValue('d');
        if (StringUtils.isBlank(location)) {
          logger.error("Location is empty!");

        }
        if (!location.endsWith("/")) {
          location += "/";
        }
        logger.info("location - {}", location);
        config.setLocation(location);
      } else {
        config.setLocation(defaultLocation);
      }

    return config;
  }
  
  /**
   * Clean stale indexes.
   *
   * @throws Exception the exception
   */
  public void cleanStaleIndexes() throws Exception {
    List<IndexMetadata> iMetas = null;
    iMetas = termUtils.getStaleTerminologies();

    if (CollectionUtils.isEmpty(iMetas))
      return;

    logger.info("Removing stale terminologies: " + iMetas);

    for (IndexMetadata iMeta : iMetas) {
      String indexName = iMeta.getIndexName();
      String objectIndexName = iMeta.getObjectIndexName();

      // objectIndexName will be NULL if terminology object is not part of
      // IndexMetadata
      // temporarily required to accommodate change in IndexMetadata object
      if (objectIndexName == null) {
        objectIndexName = "evs_object_" + indexName.replace("concept_", "");
      }

      // delete objects index
      boolean result = operationsService.deleteIndex(objectIndexName);

      if (!result) {
        logger.warn("Deleting objects index {} failed!", objectIndexName);
        continue;
      }

      // delete concepts index
      result = operationsService.deleteIndex(indexName);

      if (!result) {
        logger.warn("Deleting concepts index {} failed!", indexName);
        continue;
      }

      // delete metadata object
      esQueryService.deleteIndexMetadata(indexName);
    }
  }

  /**
   * Update latest flag.
   *
   * @throws Exception the exception
   */
  public void updateLatestFlag() throws Exception {
    // update latest flag
    logger.info("Updating latest flags on all metadata objects");
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);

    if (CollectionUtils.isEmpty(iMetas))
      return;

    Terminology latest = termUtils.getLatestTerminology(false);
    for (IndexMetadata iMeta : iMetas) {
      if (iMeta.getTerminology() != null) {
        iMeta.getTerminology()
            .setLatest(iMeta.getTerminology().getIndexName().equals(latest.getIndexName()));
      }
    }

    operationsService.bulkIndex(iMetas, ElasticOperationsService.METADATA_INDEX,
        ElasticOperationsService.METADATA_TYPE, IndexMetadata.class);

  }

}
