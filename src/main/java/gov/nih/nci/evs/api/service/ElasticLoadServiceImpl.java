
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.evs.api.Application;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * The implementation for {@link ElasticLoadService}.
 *
 * @author Arun
 */
@Service
public class ElasticLoadServiceImpl implements ElasticLoadService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(ElasticLoadServiceImpl.class);

  /** the concepts download location *. */
  @Value("${nci.evs.bulkload.conceptsDir}")
  private String CONCEPTS_OUT_DIR;

  /** the lock file name *. */
  @Value("${nci.evs.bulkload.lockFile}")
  private String LOCK_FILE;

  /** download batch size *. */
  @Value("${nci.evs.bulkload.downloadBatchSize}")
  private int DOWNLOAD_BATCH_SIZE;

  /** index batch size *. */
  @Value("${nci.evs.bulkload.indexBatchSize}")
  private int INDEX_BATCH_SIZE;

  /** the environment *. */
  @Autowired
  Environment env;

  /** The Elasticsearch operations service instance *. */
  @Autowired
  ElasticOperationsService operationsService;

  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService sparqlQueryManagerService;

  /** The elasticsearch query service *. */
  @Autowired
  private ElasticQueryService esQueryService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired
  private TerminologyUtils termUtils;

  /* see superclass */
  @Override
  public void loadConcepts(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException {

    logger.debug("ElasticLoadServiceImpl::load() - index = {}, type = {}",
        terminology.getIndexName(), ElasticOperationsService.CONCEPT_TYPE);

    boolean result =
        operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService.getElasticsearchOperations().putMapping(terminology.getIndexName(),
          ElasticOperationsService.CONCEPT_TYPE, Concept.class);
    }

    logger.info("Getting all concepts");
    List<Concept> allConcepts = sparqlQueryManagerService.getAllConcepts(terminology);

	try {
	  // download concepts and upload to es in real time
	  logger.info("Loading in real time");
	  loadConceptsRealTime(allConcepts, terminology, hierarchy);
	} catch (Exception e) {
	  logger.error(e.getMessage(), e);
	  throw new IOException(e);
	}
	
	// compare count of concepts loaded and index metadata object
	checkLoadStatusandIndexMetadata(allConcepts.size(), terminology);
	
	return;

  }

  /**
   * load concepts directly from stardog in batches.
   *
   * @param allConcepts all concepts to load
   * @param terminology the terminology
   * @param hierarchy the hierarchy
   * @throws Exception the exception
   */
  private void loadConceptsRealTime(List<Concept> allConcepts, Terminology terminology,
    HierarchyUtils hierarchy) throws Exception {
    logger.info("  download batch size = " + DOWNLOAD_BATCH_SIZE);
    logger.info("  index batch size = " + INDEX_BATCH_SIZE);

    // Check assumptions
    if (DOWNLOAD_BATCH_SIZE < INDEX_BATCH_SIZE) {
      throw new Exception("The download batch size must not be less than the index batch size");
    }

    if (CollectionUtils.isEmpty(allConcepts)) {
      logger.warn("Unable to load. No concepts found!");
      return;
    }

    logger.info("  Total concepts to load: {}", allConcepts.size());

    Double total = (double) allConcepts.size();

    int start = 0;
    int end = DOWNLOAD_BATCH_SIZE;

    Double taskSize = Math.ceil(total / INDEX_BATCH_SIZE);

    CountDownLatch latch = new CountDownLatch(taskSize.intValue());
    ExecutorService executor = Executors.newFixedThreadPool(10);

    while (start < total) {
      if (total - start <= DOWNLOAD_BATCH_SIZE)
        end = total.intValue();

      logger.info("  Processing {} to {}", start + 1, end);
      logger.info("    start reading {} to {}", start + 1, end);
      List<Concept> concepts = sparqlQueryManagerService
          .getConcepts(allConcepts.subList(start, end), terminology, hierarchy);
      logger.info("    finish reading {} to {}", start + 1, end);

      int indexStart = 0;
      int indexEnd = INDEX_BATCH_SIZE;
      Double indexTotal = (double) concepts.size();
      while (indexStart < indexTotal) {
        if (indexTotal - indexStart <= INDEX_BATCH_SIZE)
          indexEnd = indexTotal.intValue();

        executor
            .submit(new ConceptLoadTask(concepts.subList(indexStart, indexEnd), start + indexStart,
                start + indexEnd, terminology.getIndexName(), latch, taskSize.intValue()));

        indexStart = indexEnd;
        indexEnd = indexEnd + INDEX_BATCH_SIZE;

      }
      start = end;
      end = end + DOWNLOAD_BATCH_SIZE;
    }

    latch.await();

    logger.info("  shutdown");
    executor.shutdown();
    logger.info("  await termination");
    executor.awaitTermination(30, TimeUnit.SECONDS);
    logger.info("Done loading concepts!");
  }

  /* see superclass */
  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException {
    String indexName = terminology.getObjectIndexName();
    logger.info("Loading Elastic Objects");
    logger.debug("object index name: {}", indexName);
    boolean result = operationsService.createIndex(indexName, config.isForceDeleteIndex());
    logger.debug("index result: {}", result);

    try {
      ElasticObject hierarchyObject = new ElasticObject("hierarchy");
      hierarchyObject.setHierarchy(hierarchy);
      operationsService.index(hierarchyObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("  Hierarchy loaded");
    } catch (IOException e) {
      logger.error("  Error loading Elastic Object: Hierarchy", e);
      throw new IOException(e);
    }

    try {
      List<ConceptMinimal> synonymSources =
          sparqlQueryManagerService.getSynonymSources(terminology);
      ElasticObject ssObject = new ElasticObject("synonym_sources");
      ssObject.setConceptMinimals(synonymSources);
      operationsService.index(ssObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("  Synonym Sources loaded");
    } catch (IOException e) {
      logger.error("  Error loading Elastic Object: Synonym Sources", e);
      throw new IOException(e);
    }

    try {
      List<Concept> qualifiers =
          sparqlQueryManagerService.getAllQualifiers(terminology, new IncludeParam("full"));
      ElasticObject conceptsObject = new ElasticObject("qualifiers");
      conceptsObject.setConcepts(qualifiers);
      operationsService.index(conceptsObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("  Qualifiers loaded");
    } catch (IOException e) {
      logger.error("  Error loading Elastic Object: Qualifiers", e);
      throw new IOException(e);
    }

    try {
      List<Concept> properties =
          sparqlQueryManagerService.getAllProperties(terminology, new IncludeParam("full"));
      ElasticObject propertiesObject = new ElasticObject("properties");
      propertiesObject.setConcepts(properties);
      operationsService.index(propertiesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("  Properties loaded");
    } catch (IOException e) {
      logger.error("  Error loading Elastic Object: Properties", e);
      throw new IOException(e);
    }

    try {
      List<Concept> associations =
          sparqlQueryManagerService.getAllAssociations(terminology, new IncludeParam("full"));
      ElasticObject associationsObject = new ElasticObject("associations");
      associationsObject.setConcepts(associations);
      operationsService.index(associationsObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("  Associations loaded");
    } catch (IOException e) {
      logger.error("  Error loading Elastic Object: Associations", e);
      throw new IOException(e);
    }

    try {
      List<Concept> roles =
          sparqlQueryManagerService.getAllRoles(terminology, new IncludeParam("full"));
      ElasticObject rolesObject = new ElasticObject("roles");
      rolesObject.setConcepts(roles);
      operationsService.index(rolesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("  Roles loaded");
    } catch (IOException e) {
      logger.error("  Error loading Elastic Object: Roles", e);
      throw new IOException(e);
    }

    logger.info("Done loading Elastic Objects!");
  }

  /**
   * Check load statusand index metadata.
   *
   * @param total the total
   * @param terminology the terminology
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void checkLoadStatusandIndexMetadata(int total, Terminology terminology)
    throws IOException {
    Long count = esQueryService.getCount(terminology);
    logger.info("Concepts count for index {} = {}", terminology.getIndexName(), count);
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

      count = esQueryService.getCount(terminology);
      completed = (total == count.intValue());
      attempts++;
    }

    logger.info("Indexing metadata object with completed flag: {}", completed);

    IndexMetadata iMeta = new IndexMetadata();
    iMeta.setIndexName(terminology.getIndexName());
    iMeta.setTotalConcepts(total);
    iMeta.setCompleted(completed);
    iMeta.setTerminology(terminology);

    operationsService.index(iMeta, ElasticOperationsService.METADATA_INDEX,
        ElasticOperationsService.METADATA_TYPE, IndexMetadata.class);
  }

  /**
   * Clean stale indexes.
   *
   * @throws Exception the exception
   */
  private void cleanStaleIndexes() throws Exception {
    List<IndexMetadata> iMetas = null;
    try {
      iMetas = termUtils.getStaleTerminologies();
    } catch (Exception e) {
      logger.error("Error while cleaning stale terminologies: " + e.getMessage());
      throw new Exception(e);
    }

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
  private void updateLatestFlag() throws Exception {
    // update latest flag
    logger.info("Updating latest flags on all metadata objects");
    List<IndexMetadata> iMetas = esQueryService.getIndexMetadata(true);

    if (CollectionUtils.isEmpty(iMetas))
      return;

    try {
      Terminology latest = termUtils.getLatestTerminology(false);
      for (IndexMetadata iMeta : iMetas) {
        if (iMeta.getTerminology() != null) {
          iMeta.getTerminology()
              .setLatest(iMeta.getTerminology().getIndexName().equals(latest.getIndexName()));
        }
      }

      operationsService.bulkIndex(iMetas, ElasticOperationsService.METADATA_INDEX,
          ElasticOperationsService.METADATA_TYPE, IndexMetadata.class);
    } catch (Exception e) {
      logger.error("Error while updating latest flags: " + e.getMessage(), e);
      throw new Exception(e);
    }
  }

  /**
   * Task to load a batch of concepts to elasticsearch.
   *
   * @author Arun
   */
  private class ConceptLoadTask implements Callable<Void> {

    /** the logger *. */
    private final Logger taskLogger = LoggerFactory.getLogger(ConceptLoadTask.class);

    /** the concepts *. */
    @SuppressWarnings("rawtypes")
    private List concepts;

    /** start index for the task *. */
    private int startIndex;

    /** end index for the task *. */
    private int endIndex;

    /** the index name *. */
    private String indexName;

    /** the count down latch *. */
    private CountDownLatch latch;

    /** the task size *. */
    private int taskSize;

    /**
     * Instantiates a {@link ConceptLoadTask} from the specified parameters.
     *
     * @param concepts the concepts
     * @param start the start
     * @param end the end
     * @param indexName the index name
     * @param latch the latch
     * @param taskSize the task size
     */
    @SuppressWarnings("rawtypes")
    public ConceptLoadTask(List concepts, int start, int end, String indexName,
        CountDownLatch latch, int taskSize) {
      this.concepts = concepts;
      this.startIndex = start;
      this.endIndex = end;
      this.indexName = indexName;
      this.latch = latch;
      this.taskSize = taskSize;
    }

    /* see superclass */
    @Override
    public Void call() throws Exception {
      try {
        taskLogger.info("    start loading concepts: {} to {}", startIndex + 1, endIndex);
        operationsService.bulkIndex(concepts, indexName, ElasticOperationsService.CONCEPT_TYPE,
            Concept.class);
        int progress = (int) Math.floor((1.0 - 1.0 * latch.getCount() / taskSize) * 100);
        taskLogger.info("    finish loading concepts: {} to {} ({}% complete)", startIndex + 1,
            endIndex, progress);
      } catch (Throwable e) {
        taskLogger.error("  Error loading concepts: {} to {}", startIndex + 1, endIndex);
        taskLogger.error(e.getMessage(), e);
        throw new Exception(e);
      } finally {
        concepts = null;
        latch.countDown();
      }

      return null;
    }
  }

  /**
   * the main method to trigger elasticsearch load via command line *.
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    Options options = prepareOptions();
    CommandLine cmd;
    try {
      cmd = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      logger.error("{}; Try -h or --help to learn more about command line options available.",
          e.getMessage());
      return;
    }

    if (cmd.hasOption('h')) {
      printHelp(options);
      return;
    }

    ApplicationContext app = SpringApplication.run(Application.class, new String[0]);

    try {
      // get the bean by type
      ElasticLoadServiceImpl loadService = app.getBean(ElasticLoadServiceImpl.class);

      ElasticLoadConfig config = buildConfig(cmd, loadService.CONCEPTS_OUT_DIR);

      if (StringUtils.isBlank(config.getTerminology())) {
        logger.error(
            "Terminology (-t or --terminology) is required! Try -h or --help to learn more about command line options available.");
        return;
      }

      TerminologyUtils termUtils = app.getBean(TerminologyUtils.class);
      Terminology term = termUtils.getTerminology(config.getTerminology(), false);
      HierarchyUtils hierarchy = loadService.sparqlQueryManagerService.getHierarchyUtils(term);
      loadService.loadConcepts(config, term, hierarchy);
      loadService.loadObjects(config, term, hierarchy);
      loadService.cleanStaleIndexes();
      loadService.updateLatestFlag();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    } finally {
      SpringApplication.exit(app);
    }
  }

  /**
   * prepare command line options available.
   *
   * @return the options
   */
  private static Options prepareOptions() {
    Options options = new Options();

    options.addOption("f", "forceDeleteIndex", false,
        "Force delete index if index already exists.");
    options.addOption("h", "help", false, "Show this help information and exit.");
    options.addOption("r", "realTime", false,
        "Load elasticsearch in real-time by fetching concepts from stardog. Skips downloading to folder. Ignores --location (-l), --downloadOnly (-d), --skipDownload (-s) options.");
    options.addOption("t", "terminology", true, "The terminology (ex: ncit_20.02d) to load.");

    return options;
  }

  /**
   * print command line help information.
   *
   * @param options the options available
   */
  private static void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("java -jar $DIR/evsrestapi-*.jar", options);
    return;
  }

  /**
   * build config object from command line options.
   *
   * @param cmd the command line object
   * @param defaultLocation the default download location to use
   * @return the config object
   */
  private static ElasticLoadConfig buildConfig(CommandLine cmd, String defaultLocation) {
    ElasticLoadConfig config = new ElasticLoadConfig();

    config.setTerminology(cmd.getOptionValue('t'));
    config.setDownloadOnly(cmd.hasOption('d'));
    config.setSkipDownload(cmd.hasOption('s'));
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

    return config;
  }
}
