
package gov.nih.nci.evs.api.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.Application;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.support.es.ElasticObject;
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

  /* see superclass */
  @Override
  public void loadConcepts(ElasticLoadConfig config, Terminology terminology) throws IOException {

    logger.debug("ElasticLoadServiceImpl::load() - index = {}, type = {}",
        terminology.getIndexName(), ElasticOperationsService.CONCEPT_TYPE);

    boolean result =
        operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService.getElasticsearchOperations().putMapping(terminology.getIndexName(),
          ElasticOperationsService.CONCEPT_TYPE, Concept.class);
    }

    if (config.isRealTime()) {
      List<Concept> allConcepts = sparqlQueryManagerService.getAllConcepts(terminology);

      try {
        // download concepts and upload to es in real time
        logger.info("Loading in real time");
        loadConceptsRealTime(allConcepts, terminology);
      } catch (InterruptedException e) {
        logger.error(e.getMessage(), e);
      }

      return;
    }

    // get all concepts
    if (!config.isSkipDownload()) {
      logger.info("Getting all concepts");
      List<Concept> allConcepts = sparqlQueryManagerService.getAllConcepts(terminology);

      // download files from stardog in batches
      downloadConcepts(allConcepts, terminology, config.getLocation());
    }

    if (config.isDownloadOnly()) {
      logger.info("DownloadOnly option is True. Skipping load process.");
      return;
    }

    // load concepts to es in batches
    try {
      logger.info("Loading concepts from files");
      loadConceptsFromFiles(config.getLocation(), terminology);
    } catch (InterruptedException e) {
      logger.error(e.getMessage(), e);
    }

    return;
  }

  /**
   * download concepts.
   *
   * @param allConcepts all concepts to download
   * @param terminology the terminology
   * @param location the location to download
   * @throws IOException the io exception
   */
  private void downloadConcepts(List<Concept> allConcepts, Terminology terminology, String location)
    throws IOException {
    if (CollectionUtils.isEmpty(allConcepts))
      return;
    logger.debug("Downloading concepts");
    int start = 0;
    int end = DOWNLOAD_BATCH_SIZE;

    int total = allConcepts.size();
    logger.info("   Total concepts to download: {}", total);

    while (start < total) {
      if (total - start <= DOWNLOAD_BATCH_SIZE)
        end = total;

      logger.info("   Processing {} to {}", start + 1, end);

      List<String> conceptCodes = allConcepts.subList(start, end).stream().map(c -> c.getCode())
          .collect(Collectors.toList());
      List<Concept> concepts = sparqlQueryManagerService.getConcepts(conceptCodes, terminology);

      for (Concept concept : concepts) {
        String json = concept.toString();
        String filePath = location + concept.getCode() + ".json";
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));
        writer.write(json);
        writer.close();
      }

      start = end;
      end = end + DOWNLOAD_BATCH_SIZE;
    }

    Files.createFile(FileSystems.getDefault().getPath(location, LOCK_FILE));
    logger.info("Download process complete!");
  }

  /**
   * load concepts from files in batches.
   *
   * @param location the location where files are to be read from
   * @param terminology the terminology
   * @throws IOException the io exception
   * @throws InterruptedException the interruped exception
   */
  private void loadConceptsFromFiles(String location, Terminology terminology)
    throws IOException, InterruptedException {
    logger.debug("Loading concepts from files");
    File conceptsDir = new File(location);
    File lockFile = new File(location + LOCK_FILE);

    if (!conceptsDir.exists() || !lockFile.exists()) {
      logger.warn("Unable to load concepts. Lock file is missing!");
      return;
    }

    File[] jsonFiles = conceptsDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".json");
      }
    });

    if (jsonFiles == null || jsonFiles.length == 0) {
      logger.warn("No JSON files found to load!");
      return;
    }

    logger.info("   Total concepts to load: {}", jsonFiles.length);

    Double total = (double) jsonFiles.length;

    int start = 0;
    int end = INDEX_BATCH_SIZE;

    Double taskSize = Math.ceil(total / INDEX_BATCH_SIZE);

    logger.debug("   Task size: {}", taskSize.intValue());

    CountDownLatch latch = new CountDownLatch(taskSize.intValue());
    ExecutorService executor = Executors.newFixedThreadPool(10);

    while (start < total) {
      if (total - start <= INDEX_BATCH_SIZE)
        end = total.intValue();

      logger.debug("   Processing {} to {}", start, end);

      File[] files = Arrays.copyOfRange(jsonFiles, start, end);
      List<Concept> concepts = new ArrayList<>(files.length);
      ObjectMapper mapper = new ObjectMapper();
      for (File file : files) {
        byte[] data = Files.readAllBytes(Paths.get(file.toURI()));
        String strData = new String(data, "UTF-8");
        Concept concept = mapper.readValue(strData.getBytes("UTF-8"), Concept.class);
        concepts.add(concept);
      }

      executor.submit(new ConceptLoadTask(concepts, start, end, terminology.getIndexName(), latch,
          taskSize.intValue()));

      start = end;
      end = end + INDEX_BATCH_SIZE;
    }

    latch.await();

    executor.shutdown();

    logger.debug("Done loading concepts!");
  }

  /**
   * load concepts directly from stardog in batches.
   *
   * @param allConcepts all concepts to load
   * @param terminology the terminology
   * @throws IOException the io exception
   * @throws InterruptedException the interrupted exception
   */
  private void loadConceptsRealTime(List<Concept> allConcepts, Terminology terminology)
    throws IOException, InterruptedException {
    logger.info("  index batch size = " + INDEX_BATCH_SIZE);

    if (CollectionUtils.isEmpty(allConcepts)) {
      logger.warn("Unable to load. No concepts found!");
      return;
    }

    logger.info("   Total concepts to load: {}", allConcepts.size());

    Double total = (double) allConcepts.size();

    int start = 0;
    int end = INDEX_BATCH_SIZE;

    Double taskSize = Math.ceil(total / INDEX_BATCH_SIZE);

    logger.debug("   Task size: {}", taskSize.intValue());

    CountDownLatch latch = new CountDownLatch(taskSize.intValue());
    ExecutorService executor = Executors.newFixedThreadPool(10);

    while (start < total) {
      if (total - start <= INDEX_BATCH_SIZE)
        end = total.intValue();

      logger.debug("   Processing {} to {}", start, end);
      List<String> conceptCodes = allConcepts.subList(start, end).stream().map(c -> c.getCode())
          .collect(Collectors.toList());
      List<Concept> concepts = sparqlQueryManagerService.getConcepts(conceptCodes, terminology);

      executor.submit(new ConceptLoadTask(concepts, start, end, terminology.getIndexName(), latch,
          taskSize.intValue()));

      start = end;
      end = end + INDEX_BATCH_SIZE;
    }

    latch.await();

    executor.shutdown();

    logger.debug("Done loading concepts!");
  }

  /* see superclass */
  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology) throws IOException {
    String indexName = terminology.getObjectIndexName();
    logger.info("Loading Elastic Objects");
    logger.debug("object index name: {}", indexName);
    boolean result = operationsService.createIndex(indexName, config.isForceDeleteIndex());
    logger.debug("index result: {}", result);

    try {
      HierarchyUtils hierarchy = sparqlQueryManagerService.getHierarchyUtils(terminology);
      ElasticObject hierarchyObject = new ElasticObject("hierarchy");
      hierarchyObject.setHierarchy(hierarchy);
      operationsService.index(hierarchyObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("   Hierarchy loaded");
    } catch (IOException e) {
      logger.error("   Error loading Elastic Object: Hierarchy");
    }

    try {
      List<ConceptMinimal> synonymSources =
          sparqlQueryManagerService.getSynonymSources(terminology);
      ElasticObject ssObject = new ElasticObject("synonym_sources");
      ssObject.setConceptMinimals(synonymSources);
      operationsService.index(ssObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("   Synonym Sources loaded");
    } catch (IOException e) {
      logger.error("   Error loading Elastic Object: Synonym Sources");
    }

    try {
      List<Concept> qualifiers =
          sparqlQueryManagerService.getAllQualifiers(terminology, new IncludeParam("full"));
      ElasticObject conceptsObject = new ElasticObject("qualifiers");
      conceptsObject.setConcepts(qualifiers);
      operationsService.index(conceptsObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("   Qualifiers loaded");
    } catch (IOException e) {
      logger.error("   Error loading Elastic Object: Qualifiers");
    }

    try {
      List<Concept> properties =
          sparqlQueryManagerService.getAllProperties(terminology, new IncludeParam("full"));
      ElasticObject propertiesObject = new ElasticObject("properties");
      propertiesObject.setConcepts(properties);
      operationsService.index(propertiesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("   Properties loaded");
    } catch (IOException e) {
      logger.error("   Error loading Elastic Object: Properties");
    }

    try {
      List<Concept> associations =
          sparqlQueryManagerService.getAllAssociations(terminology, new IncludeParam("full"));
      ElasticObject associationsObject = new ElasticObject("associations");
      associationsObject.setConcepts(associations);
      operationsService.index(associationsObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("   Associations loaded");
    } catch (IOException e) {
      logger.error("   Error loading Elastic Object: Associations");
    }

    try {
      List<Concept> roles =
          sparqlQueryManagerService.getAllRoles(terminology, new IncludeParam("full"));
      ElasticObject rolesObject = new ElasticObject("roles");
      rolesObject.setConcepts(roles);
      operationsService.index(rolesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("   Roles loaded");
    } catch (IOException e) {
      logger.error("   Error loading Elastic Object: Roles");
    }

    logger.info("Done loading Elastic Objects!");
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
    @SuppressWarnings("unchecked")
    @Override
    public Void call() {
      try {
        operationsService.bulkIndex(concepts, indexName, ElasticOperationsService.CONCEPT_TYPE,
            Concept.class);
        int progress = (int) Math.floor((1.0 - 1.0 * latch.getCount() / taskSize) * 100);
        taskLogger.info("   Loaded concepts: {} to {} ({}% complete)", startIndex, endIndex,
            progress);
      } catch (IOException e) {
        taskLogger.error("   Error loading concepts: {} to {}", startIndex, endIndex);
        taskLogger.error(e.getMessage(), e);
      } finally {
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
      ElasticLoadServiceImpl loadService = app.getBean(ElasticLoadServiceImpl.class);// get
                                                                                     // the
                                                                                     // bean
                                                                                     // by
                                                                                     // type
      ElasticLoadConfig config = buildConfig(cmd, loadService.CONCEPTS_OUT_DIR);

      if (StringUtils.isBlank(config.getTerminology())) {
        logger.error(
            "Terminology (-t or --terminology) is required! Try -h or --help to learn more about command line options available.");
        return;
      }

      Terminology term = TerminologyUtils.getTerminology(loadService.sparqlQueryManagerService,
          config.getTerminology());
      loadService.loadConcepts(config, term);
      loadService.loadObjects(config, term);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
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

    options.addOption("d", "downloadOnly", false, "Download concepts and skip elasticsearch load.");
    options.addOption("f", "forceDeleteIndex", false,
        "Force delete index if index already exists.");
    options.addOption("h", "help", false, "Show this help information and exit.");
    options.addOption("l", "location", true,
        "The folder location (ex: /tmp/) to use for download. Overrides the configuration in application.yml file. Will be used only if download is required.");
    options.addOption("r", "realTime", false,
        "Load elasticsearch in real-time by fetching concepts from stardog. Skips downloading to folder. Ignores --location (-l), --downloadOnly (-d), --skipDownload (-s) options.");
    options.addOption("s", "skipDownload", false,
        "Load elasticsearch from folder without download.");
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
