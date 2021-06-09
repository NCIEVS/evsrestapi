
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
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
 * Retrieves concepts from stardog and creates necessary index on Elasticsearch.
 *
 * @author Arun
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

  /** The client. */
  @Autowired
  private RestHighLevelClient client;

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
  @Override
  public void cleanStaleIndexes() throws Exception {
    List<IndexMetadata> iMetas = null;

    iMetas = termUtils.getStaleTerminologies();
    logger.info("Removing stale terminologies: " + iMetas);

    if (CollectionUtils.isEmpty(iMetas)) {
      return;
    }

    for (IndexMetadata iMeta : iMetas) {
      logger.info("stale term = " + iMeta.getTerminology());
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
   * @param term the term
   * @throws Exception the exception
   */
  @Override
  public void updateLatestFlag(Terminology term) throws Exception {
    // update latest flag
    logger.info("Updating latest flags on all metadata objects");
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);

    if (CollectionUtils.isEmpty(iMetas))
      return;

    // Make sure the new latest is set as latest and any others with matching
    // terminology are set to "false". Skip anything with non-matching
    // terminology.
    Terminology latest = termUtils.getLatestTerminology(true, term);
    boolean latestWeeklyOnly =
        !latest.getTags().containsKey("monthly") && latest.getTags().containsKey("weekly");

    for (IndexMetadata iMeta : iMetas) {
      boolean monthly = iMeta.getTerminology().getTags().containsKey("monthly");

      // only change latest flag of terminologies that match latest one
      if (iMeta.getTerminology().getTerminology().equals(latest.getTerminology())) {

        // If latest is a weekly update, remove the "weekly" tag from
        // the monthly, but leave "latest" flag unchanged
        if (latestWeeklyOnly && monthly && iMeta.getTerminology().getLatest()) {
          logger.info("  " + iMeta.getTerminologyVersion() + " = remove weekly tag");
          iMeta.getTerminology().getTags().remove("weekly");
        } else {
          boolean flag = iMeta.getTerminology().equals(latest);
          logger.info("  " + iMeta.getTerminologyVersion() + " = " + flag);
          iMeta.getTerminology().setLatest(flag);
        }
      } else {
        // If latest is a weekly update, remove the "weekly" tag from
        // the latest monthly, but leave "latest" flag unchanged
        if (latestWeeklyOnly && monthly && iMeta.getTerminology().getLatest()) {
          logger.info("  " + iMeta.getTerminologyVersion() + " = remove weekly tag");
          iMeta.getTerminology().getTags().remove("weekly");
        } else {
          logger.info("  " + iMeta.getTerminologyVersion() + " = unchanged");
        }
      }
    }

    operationsService.bulkIndex(iMetas, ElasticOperationsService.METADATA_INDEX,
        ElasticOperationsService.METADATA_TYPE, IndexMetadata.class);

  }

  /**
   * check load status.
   *
   * @param total the total
   * @param term the terminology object
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public void checkLoadStatus(int total, Terminology term) throws IOException {

    Long count = esQueryService.getCount(term);
    logger.info("Concepts count for index {} = {}", term.getIndexName(), count);
    boolean completed = (total == count.intValue());

    if (!completed) {
      logger.info("Concepts indexing not complete yet, waiting for completion..");
    }

    int attempts = 0;

    while (!completed && attempts < 30) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        logger.error("Error while checking load status: sleep interrupted - " + e.getMessage(), e);
        throw new IOException(e);
      }

      if (attempts == 15) {
        logger.info("Index completion is taking longer than expected..");
      }

      count = esQueryService.getCount(term);
      completed = (total == count.intValue());
      attempts++;
    }
  }

  /**
   * load index metadata.
   *
   * @param total the total
   * @param term the terminology object
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public void loadIndexMetadata(int total, Terminology term) throws IOException {
    IndexMetadata iMeta = new IndexMetadata();
    iMeta.setIndexName(term.getIndexName());
    iMeta.setTotalConcepts(total);
    iMeta.setCompleted(true); // won't make it this far if it isn't complete
    iMeta.setTerminology(term);

    operationsService.createIndex(ElasticOperationsService.METADATA_INDEX, false);

    operationsService.getElasticsearchOperations().putMapping(ElasticOperationsService.METADATA_INDEX,
          ElasticOperationsService.METADATA_TYPE, IndexMetadata.class);

    operationsService.index(iMeta, ElasticOperationsService.METADATA_INDEX,
        ElasticOperationsService.METADATA_TYPE, IndexMetadata.class);

    // This block is for debugging presence of the iMeta
    // try {
    // Thread.sleep(2000);
    // } catch (InterruptedException e) {
    // // n/a
    // }
    // List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);
    // for (IndexMetadata iMetaPostLoad : iMetas) {
    // logger.info("iMetaPostLoad = " + iMetaPostLoad);
    // }

  }

  /**
   * Find and delete terminology.
   *
   * @param ID the id
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InterruptedException the interrupted exception
   */
  protected void findAndDeleteTerminology(String ID) throws IOException, InterruptedException {
    DeleteRequest request = new DeleteRequest(ElasticOperationsService.METADATA_INDEX,
        ElasticOperationsService.METADATA_TYPE, ID);
    client.delete(request, RequestOptions.DEFAULT);

    // This block is for debugging presence of the iMeta still in the index
    // Thread.sleep(2000);
    // List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);
    // for (IndexMetadata iMetaPostDelete : iMetas) {
    // logger.info("iMetaPostDelete = " + iMetaPostDelete);
    // }
    return;

  }

}
