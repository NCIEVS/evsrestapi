package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import gov.nih.nci.evs.api.support.es.OpensearchLoadConfig;
import gov.nih.nci.evs.api.util.EVSUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * The service to load concepts to Opensearch
 *
 * <p>Retrieves concepts from graph db and creates necessary index on Opensearch.
 *
 * @author Arun
 */
public abstract class BaseLoaderService implements OpensearchLoadService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(BaseLoaderService.class);

  /** the metrics db path. */
  @Autowired ApplicationProperties applicationProperties;

  /** The Opensearch operations service instance *. */
  @Autowired OpensearchOperationsService operationsService;

  /** The Opensearch query service *. */
  @Autowired private OpensearchQueryService osQueryService;

  /** The sparql query service *. */
  @Autowired private SparqlQueryManagerService sparqlQueryManagerService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired private TerminologyUtils termUtils;

  /** The client. */
  @Autowired private RestHighLevelClient client;

  /** The dbs. */
  @Value("${nci.evs.bulkload.graphDbs}")
  private String dbs;

  /**
   * build config object from command line options.
   *
   * @param cmd the command line object
   * @param defaultLocation the default download location to use
   * @return the config object
   */
  public static OpensearchLoadConfig buildConfig(CommandLine cmd, String defaultLocation) {
    OpensearchLoadConfig config = new OpensearchLoadConfig();

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
   * Initialize the service to ensure indexes are created before getting Terminologies.
   *
   * @throws Exception the exception
   */
  @Override
  public void initialize() throws Exception {
    // Create the metadata index if it doesn't exist
    boolean createdIndex =
        operationsService.createIndex(OpensearchOperationsService.METADATA_INDEX, false);
    if (createdIndex) {
      operationsService
          .getOpenSearchOperations()
          .indexOps(IndexCoordinates.of(OpensearchOperationsService.METADATA_INDEX))
          .putMapping(IndexMetadata.class);
    }

    // Create the mapping indexes if they don't exist
    boolean createdMapset =
        operationsService.createIndex(OpensearchOperationsService.MAPSET_INDEX, false);
    operationsService.createIndex(OpensearchOperationsService.MAPPINGS_INDEX, false);
    if (createdMapset) {
      operationsService
          .getOpenSearchOperations()
          .indexOps(IndexCoordinates.of(OpensearchOperationsService.MAPSET_INDEX))
          .putMapping(Concept.class);
      operationsService
          .getOpenSearchOperations()
          .indexOps(IndexCoordinates.of(OpensearchOperationsService.MAPPINGS_INDEX))
          .putMapping(Mapping.class);
    }

    // create the audit index if it doesn't exist
    boolean createdAudit =
        operationsService.createIndex(OpensearchOperationsService.AUDIT_INDEX, false);
    if (createdAudit) {
      operationsService
          .getOpenSearchOperations()
          .indexOps(IndexCoordinates.of(OpensearchOperationsService.AUDIT_INDEX))
          .putMapping(Audit.class);
    }
  }

  /**
   * Clean stale indexes.
   *
   * @param terminology the terminology
   * @return the sets the
   * @throws Exception the exception
   */
  @Override
  public Set<String> cleanStaleIndexes(final Terminology terminology) throws Exception {

    List<IndexMetadata> iMetas =
        termUtils.getStaleGraphTerminologies(
            Arrays.asList(dbs.split(",")), terminology, sparqlQueryManagerService, osQueryService);
    if (CollectionUtils.isEmpty(iMetas)) {
      logger.info("NO stale terminologies to remove");
      return new HashSet<>(0);
    }

    logger.info("Removing stale terminologies");
    final Set<String> removed = new HashSet<>();
    for (IndexMetadata iMeta : iMetas) {

      logger.info("  REMOVE stale terminology = " + iMeta.getTerminology().getTerminologyVersion());
      String indexName = iMeta.getIndexName();
      String objectIndexName = iMeta.getObjectIndexName();

      // objectIndexName will be NULL if terminology object is not part of
      // IndexMetadata temporarily required to accommodate change in
      // IndexMetadata object
      if (objectIndexName == null) {
        objectIndexName = "evs_object_" + indexName.replace("concept_", "");
      }

      // delete objects index
      logger.info("    REMOVE " + objectIndexName);
      boolean result = operationsService.deleteIndex(objectIndexName);

      if (!result) {
        logger.warn("Deleting objects index {} failed!", objectIndexName);
        Audit.addAudit(
            operationsService,
            "DeleteIndexFailed",
            "cleanStaleIndexes",
            terminology.getTerminology(),
            "Deleting objects index " + objectIndexName + " failed!",
            "WARN");
        continue;
      }

      // delete concepts index
      logger.info("    REMOVE " + indexName);
      result = operationsService.deleteIndex(indexName);

      if (!result) {
        logger.warn("Deleting concepts index {} failed!", indexName);
        Audit.addAudit(
            operationsService,
            "WARN",
            "cleanStaleIndexes",
            terminology.getTerminology(),
            "Deleting concepts index " + objectIndexName + " failed!",
            "WARN");
        continue;
      }

      // delete metadata object
      logger.info("    REMOVE evs_metadata " + indexName);
      String id = operationsService.deleteIndexMetadata(indexName);
      logger.info("      id = " + id);

      removed.add(iMeta.getTerminologyVersion());
    }
    return removed;
  }

  /**
   * Update latest flag.
   *
   * @param terminology the terminology
   * @param removed the removed
   * @throws Exception the exception
   */
  @Override
  public void updateLatestFlag(final Terminology terminology, final Set<String> removed)
      throws Exception {
    // update latest flag
    logger.info("Updating latest flags on all metadata objects");
    List<IndexMetadata> iMetas = osQueryService.getIndexMetadata(true);

    // If certain terminology/version combinations were removed, skip them here
    iMetas.removeIf(m -> removed.contains(m.getTerminologyVersion()));
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
        iMetas, OpensearchOperationsService.METADATA_INDEX, IndexMetadata.class);
  }

  /**
   * check load status.
   *
   * @param total the iMeta.getTerminology().setLatest(true); total
   * @param term the terminology object
   * @throws Exception the exception
   */
  @Override
  public void checkLoadStatus(int total, Terminology term) throws Exception {

    Long count = osQueryService.getCount(term);
    logger.info("Concepts count for index {} = {}", term.getIndexName(), count);
    boolean completed = (total == count.intValue());

    if (!completed) {
      logger.info("Concepts indexing not complete yet, waiting for completion..");
    }

    int attempts = 0;

    while (!completed && attempts < 30) {
      Thread.sleep(2000);

      if (attempts == 15) {
        logger.info("Index completion is taking longer than expected..");
      }

      count = osQueryService.getCount(term);
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
   * @throws Exception the exception
   */
  @Override
  public void loadIndexMetadata(int total, Terminology term) throws IOException, Exception {
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
    // operationsService.index(iMeta, OpensearchOperationsService.METADATA_INDEX,
    // OpensearchOperationsService.METADATA_TYPE, IndexMetadata.class);

    // Make sure this blocks before proceeding
    operationsService.bulkIndexAndWait(
        Arrays.asList(iMeta), OpensearchOperationsService.METADATA_INDEX, IndexMetadata.class);

    // This block is for debugging presence of the iMeta
    List<IndexMetadata> iMetas = osQueryService.getIndexMetadata(true);
    final List<String> completed =
        iMetas.stream().map(t -> t.getTerminologyVersion()).sorted().collect(Collectors.toList());
    logger.info("  Completed terminologies = " + completed);

    logger.info(
        "  NOT Completed terminologies = "
            + iMetas.stream()
                .map(t -> t.getTerminologyVersion())
                .filter(t -> !completed.contains(t))
                .sorted()
                .collect(Collectors.toList()));
  }

  /**
   * Find and delete terminology.
   *
   * @param ID the id
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InterruptedException the interrupted exception
   */
  protected void findAndDeleteTerminology(String ID) throws IOException, InterruptedException {
    DeleteRequest request = new DeleteRequest(OpensearchOperationsService.METADATA_INDEX, ID);
    client.delete(request, RequestOptions.DEFAULT);

    // This block is for debugging presence of the iMeta still in the index
    // Thread.sleep(2000);
    // List<IndexMetadata> iMetas = osQueryService.getIndexMetadata(true);
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
    return new ObjectMapper()
        .readTree(StringUtils.join(EVSUtils.getValueFromFile(uri, "metadata info"), '\n'));
  }

  /**
   * Returns the metadata as node from an explicitly local filepath.
   *
   * @param terminology the terminology
   * @return the metadata as node
   * @throws Exception the exception
   */
  public JsonNode getMetadataAsNodeLocal(final String terminology) throws Exception {
    // Read from the configured URI where this data lives
    // If terminology is {term}_{version} -> strip the version
    final String uri = "src/test/resources/" + termUtils.getTerminologyName(terminology) + ".json";
    logger.info("  get config for " + terminology + " = " + uri);
    return new ObjectMapper()
        .readTree(StringUtils.join(EVSUtils.getValueFromFile(uri, "metadata info"), '\n'));
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
    return StringUtils.join(EVSUtils.getValueFromFile(uri, "welcome text"), '\n');
  }

  /* see superclass */
  @Override
  public Map<String, List<Map<String, String>>> updateHistoryMap(
      final Terminology terminology, final String filepath) throws Exception {
    logger.info("History map updating not implemented for " + terminology.getTerminology());
    return Collections.emptyMap();
  }

  /* see superclass */
  @Override
  public void updateHistory(
      final Terminology terminology,
      Map<String, List<Map<String, String>>> historyMap,
      String filepath)
      throws Exception {
    logger.info("History updating not implemented for " + terminology.getTerminology());
  }
}
