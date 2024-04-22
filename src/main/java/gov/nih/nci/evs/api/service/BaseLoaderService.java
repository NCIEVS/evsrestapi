package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.util.CollectionUtils;

/**
 * The service to load concepts to Elasticsearch
 *
 * <p>Retrieves concepts from stardog and creates necessary index on Elasticsearch.
 *
 * @author Arun
 */
public abstract class BaseLoaderService implements ElasticLoadService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(BaseLoaderService.class);

  /** the metrics db path. */
  @Autowired ApplicationProperties applicationProperties;

  /** The Elasticsearch operations service instance *. */
  @Autowired ElasticOperationsService operationsService;

  /** The elasticsearch query service *. */
  @Autowired private ElasticQueryService esQueryService;

  /** The sparql query service *. */
  @Autowired private SparqlQueryManagerService sparqlQueryManagerService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired private TerminologyUtils termUtils;

  /** The client. */
  @Autowired private RestHighLevelClient client;

  /** The dbs. */
  @Value("${nci.evs.bulkload.stardogDbs}")
  private String dbs;

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
   * Initialize the service to ensure indexes are created before getting Terminologies
   *
   * @throws Exception
   */
  @Override
  public void initialize() throws IOException {
    try {
      // Create the metadata index if it doesn't exist
      boolean createdIndex =
          operationsService.createIndex(ElasticOperationsService.METADATA_INDEX, false);
      if (createdIndex) {
        operationsService
            .getElasticsearchOperations()
            .indexOps(IndexCoordinates.of(ElasticOperationsService.METADATA_INDEX))
            .putMapping(IndexMetadata.class);
      }

      // Create the mapping index if it doesn't exist
      boolean createdMapset =
          operationsService.createIndex(ElasticOperationsService.MAPPING_INDEX, false);
      if (createdMapset) {
        operationsService
            .getElasticsearchOperations()
            .indexOps(IndexCoordinates.of(ElasticOperationsService.MAPPING_INDEX))
            .putMapping(Concept.class);
      }
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new IOException(e);
    }
  }

  /**
   * Clean stale indexes.
   *
   * @param terminology the terminology
   * @throws Exception the exception
   */
  @Override
  public void cleanStaleIndexes(final Terminology terminology) throws Exception {

    List<IndexMetadata> iMetas =
        termUtils.getStaleStardogTerminologies(
            Arrays.asList(dbs.split(",")), terminology, sparqlQueryManagerService, esQueryService);
    if (CollectionUtils.isEmpty(iMetas)) {
      logger.info("NO stale terminologies to remove");
      return;
    }

    logger.info("Removing stale terminologies");
    for (IndexMetadata iMeta : iMetas) {

      logger.info("stale terminology = " + iMeta.getTerminology().getTerminologyVersion());
      String indexName = iMeta.getIndexName();
      String objectIndexName = iMeta.getObjectIndexName();

      // objectIndexName will be NULL if terminology object is not part of
      // IndexMetadata temporarily required to accommodate change in
      // IndexMetadata object
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
      operationsService.deleteIndexMetadata(indexName);
    }
  }

  /**
   * Update latest flag.
   *
   * @param terminology the terminology
   * @throws Exception the exception
   */
  @Override
  public void updateLatestFlag(final Terminology terminology) throws Exception {
    // update latest flag
    logger.info("Updating latest flags on all metadata objects");
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);

    if (CollectionUtils.isEmpty(iMetas)) {
      return;
    }

    // Copy to allow modification
    iMetas = new ArrayList<>(iMetas);
    // Reverse sort (latest versions first)
    Collections.sort(
        iMetas,
        new Comparator<IndexMetadata>() {
          @Override
          public int compare(IndexMetadata o1, IndexMetadata o2) {
            return -1
                * o1.getTerminology().getVersion().compareTo(o2.getTerminology().getVersion());
          }
        });
    logger.info(
        "  iMetas = "
            + iMetas.stream()
                .map(i -> i.getTerminology().getTerminologyVersion())
                .collect(Collectors.toList()));
    boolean latestMonthlyFound = false;
    boolean latestWeeklyFound = false;
    boolean latestFound = false;

    for (final IndexMetadata iMeta : iMetas) {

      // Skip terminologies for a different loader
      if (!iMeta.getTerminology().getTerminology().equals(terminology.getTerminology())) {
        continue;
      }

      final boolean monthly = iMeta.getTerminology().getTags().containsKey("monthly");
      final boolean weekly = iMeta.getTerminology().getTags().containsKey("weekly");

      // Set latest monthly
      if (!latestMonthlyFound && monthly) {
        // Remove weekly tag if another thing was the latest weekly
        if (latestWeeklyFound) {
          logger.info("  " + iMeta.getTerminologyVersion() + " = remove weekly tag");
          iMeta.getTerminology().getTags().remove("weekly");
        } else {
          logger.info("  " + iMeta.getTerminologyVersion() + " = add weekly tag");
          iMeta.getTerminology().getTags().put("weekly", "true");
        }
        logger.info("  " + iMeta.getTerminologyVersion() + " = latest true");
        iMeta.getTerminology().setLatest(true);
        latestMonthlyFound = true;
        latestWeeklyFound = true;
      }
      // Set latest weekly
      else if (!latestWeeklyFound && weekly) {
        logger.info("  " + iMeta.getTerminologyVersion() + " = latest true");
        iMeta.getTerminology().setLatest(true);
        latestWeeklyFound = true;
      }
      // Else if we're dealing with non-monthly/non-weekly and latest hasn't
      // been found
      else if (!latestFound && !monthly && !weekly) {
        logger.info("  " + iMeta.getTerminologyVersion() + " = latest true");
        iMeta.getTerminology().setLatest(true);
        latestFound = true;
      }
      // Else change nothing
      else {
        logger.info("  " + iMeta.getTerminologyVersion() + " = latest false");
        iMeta.getTerminology().setLatest(false);
      }

      // see if Concept Statuses needs to be updated
      if (!iMeta
          .getTerminology()
          .getMetadata()
          .getConceptStatuses()
          .equals(terminology.getMetadata().getConceptStatuses())) {

        iMeta
            .getTerminology()
            .getMetadata()
            .setConceptStatuses(terminology.getMetadata().getConceptStatuses());
      }
    }

    operationsService.bulkIndex(
        iMetas, ElasticOperationsService.METADATA_INDEX, IndexMetadata.class);
  }

  /**
   * check load status.
   *
   * @param total the iMeta.getTerminology().setLatest(true); total
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
    // won't make it this far if it isn't complete
    iMeta.setCompleted(true);
    logger.info("  ADD terminology = " + iMeta);
    iMeta.setTerminology(term);

    // create the metadata and mappings index
    initialize();

    // Non-blocking index approach
    // operationsService.index(iMeta, ElasticOperationsService.METADATA_INDEX,
    // ElasticOperationsService.METADATA_TYPE, IndexMetadata.class);

    // Make sure this blocks before proceeding
    operationsService.bulkIndexAndWait(
        Arrays.asList(iMeta), ElasticOperationsService.METADATA_INDEX, IndexMetadata.class);

    // This block is for debugging presence of the iMeta
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);
    for (IndexMetadata iMetaPostLoad : iMetas) {
      logger.info("iMetaPostLoad (true) = " + iMetaPostLoad);
    }

    // This block is for debugging presence of the iMeta
    iMetas = esQueryService.getIndexMetadata(false);
    for (IndexMetadata iMetaPostLoad : iMetas) {
      logger.info("iMetaPostLoad (false) = " + iMetaPostLoad);
    }
  }

  /**
   * Find and delete terminology.
   *
   * @param ID the id
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InterruptedException the interrupted exception
   */
  protected void findAndDeleteTerminology(String ID) throws IOException, InterruptedException {
    DeleteRequest request = new DeleteRequest(ElasticOperationsService.METADATA_INDEX, ID);
    client.delete(request, RequestOptions.DEFAULT);

    // This block is for debugging presence of the iMeta still in the index
    // Thread.sleep(2000);
    // List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);
    // for (IndexMetadata iMetaPostDelete : iMetas) {
    // logger.info("iMetaPostDelete = " + iMetaPostDelete);
    // }
    return;
  }

  /**
   * Take care of all needed steps to set a concept to inactive.
   *
   * @param term the Terminology
   * @param concept the concept
   */
  protected void setConceptInactive(final Terminology term, final Concept concept) {

    concept.setActive(false);
    concept.setConceptStatus("Retired_Concept");

    if (!term.getMetadata().getConceptStatuses().contains("Retired_Concept")) {
      term.getMetadata().getConceptStatuses().add("Retired_Concept");
    }
  }

  /**
   * Returns the metadata.
   *
   * @param terminology the terminology
   * @return the metadata
   * @throws Exception the exception
   */
  public TerminologyMetadata getMetadata(final String terminology) throws Exception {
    return new ObjectMapper()
        .treeToValue(getMetadataAsNode(terminology), TerminologyMetadata.class);
  }

  /**
   * Returns the metadata as node.
   *
   * @param terminology the terminology
   * @return the metadata as node
   * @throws Exception the exception
   */
  public JsonNode getMetadataAsNode(final String terminology) throws Exception {
    // Read from the configured URI where this data lives
    // If terminology is {term}_{version} -> strip the version
    final String uri =
        applicationProperties.getConfigBaseUri()
            + "/"
            + termUtils.getTerminologyName(terminology)
            + ".json";
    logger.info("  get config for " + terminology + " = " + uri);
    final URL url = new URL(uri);

    try (final InputStream is = url.openConnection().getInputStream()) {
      return new ObjectMapper().readTree(IOUtils.toString(is, "UTF-8"));
    }
  }

  /**
   * Returns the welcome text.
   *
   * @param terminology the terminology
   * @return the welcome text
   * @throws Exception the exception
   */
  public String getWelcomeText(final String terminology) throws Exception {
    // Read from the configured URI where this data lives
    // If terminology is {term}_{version} -> strip the version
    final String uri =
        applicationProperties.getConfigBaseUri()
            + "/"
            + termUtils.getTerminologyName(terminology)
            + ".html";
    logger.info("  get welcome text for " + terminology + " = " + uri);
    try (final InputStream is = new URL(uri).openConnection().getInputStream()) {
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    }
  }
}
