package gov.nih.nci.evs.api.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
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
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * The implementation for {@link ElasticLoadService}
 * 
 * @author Arun
 *
 */
@Service
public class ElasticLoadServiceImpl implements ElasticLoadService {

  private static final Logger logger = LoggerFactory.getLogger(ElasticLoadServiceImpl.class);

  /** the concepts download location **/
  @Value("${nci.evs.bulkload.conceptsDir}")
  private String CONCEPTS_OUT_DIR;

  /** the lock file name **/
  @Value("${nci.evs.bulkload.lockFile}")
  private String LOCK_FILE;

  /** download batch size **/
  @Value("${nci.evs.bulkload.downloadBatchSize}")
  private int DOWNLOAD_BATCH_SIZE;

  /** index batch size **/
  @Value("${nci.evs.bulkload.indexBatchSize}")
  private int INDEX_BATCH_SIZE;

  /** the environment **/
  @Autowired
  Environment env;
  
  /** The Elasticsearch operations service instance **/
  @Autowired
  ElasticOperationsService operationsService;

  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService sparqlQueryManagerService;

  @Override
  
  /**
   * load elasticsearch index based on config options
   * 
   * @param config the config object with options
   * @param terminology the terminology object
   */
  public void load(ElasticLoadConfig config, Terminology terminology) throws IOException {

    logger.debug("ElasticLoadServiceImpl::load() - index = {}, type = {}", terminology.getIndexName(), 
        ElasticOperationsService.CONCEPT_TYPE);
    
    boolean result = operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService.getElasticsearchOperations().putMapping(terminology.getIndexName(), ElasticOperationsService.CONCEPT_TYPE, Concept.class);
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
   * download concepts
   * 
   * @param allConcepts all concepts to download
   * @param terminology the terminology
   * @param location the location to download
   * @throws IOException the io exception
   */
  private void downloadConcepts(List<Concept> allConcepts, Terminology terminology, String location) throws IOException {
    if (CollectionUtils.isEmpty(allConcepts))
      return;
    logger.debug("Downloading concepts");
    int start = 0;
    int end = DOWNLOAD_BATCH_SIZE;

    int total = allConcepts.size();
    logger.info("   total concepts to download: {}", total);

    while (start < total) {
      if (total - start <= DOWNLOAD_BATCH_SIZE)
        end = total;

      logger.info("  Processing {} to {}", start + 1, end);

      List<String> conceptCodes = allConcepts.subList(start, end).stream().map(c -> c.getCode())
          .collect(Collectors.toList());
      List<Concept> concepts = sparqlQueryManagerService.getConcepts(conceptCodes, terminology);

      for (Concept concept : concepts) {
        String json = concept.toString();
        String filePath = location + concept.getCode() + ".json";
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
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
   * load concepts from files in batches
   * 
   * @param location the location where files are to be read from
   * @param terminology the terminology
   * @throws IOException the io exception
   * @throws InterruptedException the interruped exception
   */
  private void loadConceptsFromFiles(String location, Terminology terminology) throws IOException, InterruptedException {
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

    logger.info("   total concepts to load: {}", jsonFiles.length);
    
    Double total = (double) jsonFiles.length;

    int start = 0;
    int end = INDEX_BATCH_SIZE;

    Double taskSize = Math.ceil(total / INDEX_BATCH_SIZE);

    logger.debug("   task size: {}", taskSize.intValue());

    CountDownLatch latch = new CountDownLatch(taskSize.intValue());
    ExecutorService executor = Executors.newFixedThreadPool(10);

    while (start < total) {
      if (total - start <= INDEX_BATCH_SIZE)
        end = total.intValue();

      logger.debug("   processing {} to {}", start, end);

      File[] files = Arrays.copyOfRange(jsonFiles, start, end);
      List<Concept> concepts = new ArrayList<>(files.length);
      ObjectMapper mapper = new ObjectMapper();
      for (File file : files) {
        byte[] data = Files.readAllBytes(Paths.get(file.toURI()));
        String strData = new String(data, "Windows-1252");
        Concept concept = mapper.readValue(strData.getBytes("UTF-8"), Concept.class);
        concepts.add(concept);
      }

      executor.submit(new LoadTask(concepts, start, end, terminology.getIndexName(), latch, taskSize.intValue()));

      start = end;
      end = end + INDEX_BATCH_SIZE;
    }

    latch.await();

    executor.shutdown();
  }

  /**
   * load concepts directly from stardog in batches
   * 
   * @param allConcepts all concepts to load
   * @param terminology the terminology
   * @throws IOException the io exception
   * @throws InterruptedException the interrupted exception
   */
  private void loadConceptsRealTime(List<Concept> allConcepts, Terminology terminology)
      throws IOException, InterruptedException {
    logger.debug("loading concepts real time");

    if (CollectionUtils.isEmpty(allConcepts)) {
      logger.warn("Unable to load. No concepts found!");
      return;
    }

    logger.info("   total concepts to load: {}", allConcepts.size());
    
    Double total = (double) allConcepts.size();

    int start = 0;
    int end = INDEX_BATCH_SIZE;

    Double taskSize = Math.ceil(total / INDEX_BATCH_SIZE);

    logger.debug("   task size: {}", taskSize.intValue());

    CountDownLatch latch = new CountDownLatch(taskSize.intValue());
    ExecutorService executor = Executors.newFixedThreadPool(10);

    while (start < total) {
      if (total - start <= INDEX_BATCH_SIZE)
        end = total.intValue();

      logger.debug("   processing {} to {}", start, end);
      List<String> conceptCodes = allConcepts.subList(start, end).stream().map(c -> c.getCode())
          .collect(Collectors.toList());
      List<Concept> concepts = sparqlQueryManagerService.getConcepts(conceptCodes, terminology);

      executor.submit(new LoadTask(concepts, start, end, terminology.getIndexName(), latch, taskSize.intValue()));

      start = end;
      end = end + INDEX_BATCH_SIZE;
    }

    latch.await();

    executor.shutdown();
  }

  /**
   * Task to load a batch of concepts to elasticsearch
   * 
   * @author Arun
   *
   */
  private class LoadTask implements Callable<Void> {
    /** the logger **/
    private final Logger taskLogger = LoggerFactory.getLogger(LoadTask.class);
    
    /** the concepts **/
    private List<Concept> concepts;
    
    /** start index for the task **/
    private int startIndex;
    
    /** end index for the task **/
    private int endIndex;
    
    /** the index name **/
    private String indexName;
    
    /**  the count down latch **/
    private CountDownLatch latch;
    
    /** the task size **/
    private int taskSize;

    public LoadTask(List<Concept> concepts, int start, int end, String indexName, CountDownLatch latch, int taskSize) {
      this.concepts = concepts;
      this.startIndex = start;
      this.endIndex = end;
      this.indexName = indexName;
      this.latch = latch;
      this.taskSize = taskSize;
    }

    @Override
    public Void call() {
      try {
        operationsService.loadConcepts(concepts, indexName, ElasticOperationsService.CONCEPT_TYPE);
        int progress = (int)Math.floor((1.0 - 1.0*latch.getCount()/taskSize)*100);
        taskLogger.info("   loaded concepts: {} to {} ({}% complete)", startIndex, endIndex, progress);
      } catch (IOException e) {
        taskLogger.error("   error loading concepts: {} to {}", startIndex, endIndex);
        taskLogger.error(e.getMessage(), e);
      } finally {
        latch.countDown();
      }

      return null;
    }
  }

  /** the main method to trigger elasticsearch load via command line **/
  public static void main(String[] args) {
    Options options = prepareOptions();
    CommandLine cmd;
    try {
      cmd = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      logger.error("{}; Try -h or --help to learn more about command line options available.", e.getMessage());
      return;
    }
    
    if (cmd.hasOption('h')) {
      printHelp(options);
      return;
    }

    ApplicationContext app = SpringApplication.run(Application.class, new String[0]);
    
    try {
      ElasticLoadServiceImpl loadService = app.getBean(ElasticLoadServiceImpl.class);//get the bean by type
      ElasticLoadConfig config = buildConfig(cmd, loadService.CONCEPTS_OUT_DIR);
      
      if (StringUtils.isBlank(config.getTerminology())) {
        logger.error("Terminology (-t or --terminology) is required! Try -h or --help to learn more about command line options available.");
        return;
      }

      Terminology term = TerminologyUtils.getTerminology(loadService.sparqlQueryManagerService, config.getTerminology());
      loadService.load(config, term);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    } finally {
      SpringApplication.exit(app);
    }
  }
  
  /**
   * prepare command line options available
   * 
   * @return
   */
  private static Options prepareOptions() {
    Options options = new Options();

    options.addOption("d", "downloadOnly", false, "Download concepts and skip elasticsearch load.");
    options.addOption("f", "forceDeleteIndex", false, "Force delete index if index already exists.");
    options.addOption("h", "help", false, "Show this help information and exit.");
    options.addOption("l", "location", true, "The folder location (ex: /tmp/) to use for download. Overrides the configuration in application.yml file. Will be used only if download is required.");
    options.addOption("r", "realTime", false, "Load elasticsearch in real-time by fetching concepts from stardog. Skips downloading to folder. Ignores --location (-l), --downloadOnly (-d), --skipDownload (-s) options.");
    options.addOption("s", "skipDownload", false, "Load elasticsearch from folder without download.");
    options.addOption("t", "terminology", true, "The terminology (ex: ncit_20.02d) to load.");
    
    return options;
  }
  
  /**
   * print command line help information
   * 
   * @param options the options available
   */
  private static void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("java -jar $DIR/evsrestapi-1.1.1.RELEASE.jar", options);
    return;
  }
  
  /**
   * build config object from command line options
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
