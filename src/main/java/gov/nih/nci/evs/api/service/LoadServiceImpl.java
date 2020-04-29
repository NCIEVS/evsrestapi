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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.LoadConfig;

/**
 * The implementation for {@link LoadService}
 * 
 * @author Arun
 *
 */
@Service
public class LoadServiceImpl implements LoadService {

  private static final Logger logger = LoggerFactory.getLogger(LoadServiceImpl.class);
  
  @Value("${nci.evs.bulkload.conceptsDir}")
  private String CONCEPTS_OUT_DIR;
  
  @Value("${nci.evs.bulkload.lockFile}")
  private String LOCK_FILE;
  
  @Value("${nci.evs.bulkload.downloadBatchSize}")
  private int DOWNLOAD_BATCH_SIZE;
  
  @Value("${nci.evs.bulkload.indexBatchSize}")
  private int INDEX_BATCH_SIZE;
  
  /** The Elasticsearch operations service instance **/
  @Autowired
  ElasticOperationsService esOperations;
  
  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService sparqlQueryManagerService;
  
  @Override
  public void load(LoadConfig config, Terminology terminology) throws IOException {

    if (config.isRealTime()) {
      List<Concept> allConcepts = sparqlQueryManagerService.getAllConcepts(terminology);
      
      try {
        //download concepts and upload to es in real time
        logger.info("Loading in real time");
        loadConceptsRealTime(allConcepts, terminology);
      } catch (InterruptedException e) {
        logger.error(e.getMessage(), e);
      }
      
      return;
    }
    
    //get all concepts
    if (!config.isSkipDownload()) {
      logger.info("Getting all concepts");
      List<Concept> allConcepts = sparqlQueryManagerService.getAllConcepts(terminology);
      
      if (allConcepts != null) logger.info("all concepts size is {}", allConcepts.size());
      
      //download files from stardog in batches
      downloadConcepts(allConcepts, terminology);
    }
    
    if (config.isDownloadOnly()) {
      logger.info("downloadOnly option is True. Skipping load process.");
      return;
    }
    
    //load concepts to es in batches
    try {
      logger.info("loading concepts from files");
      loadConceptsFromFiles();
    } catch (InterruptedException e) {
      logger.error(e.getMessage(), e);
    }
    
    return;
  }

  //download files from stardog in batches
  private void downloadConcepts(List<Concept> allConcepts, Terminology terminology) throws IOException {
    if (CollectionUtils.isEmpty(allConcepts)) return;
    logger.info("Downloading concepts");
    int start = 0;
    int end = DOWNLOAD_BATCH_SIZE;
    
    int total = allConcepts.size();
    logger.info("  Total concepts: {}", total);
    
    
    while(start < total) {
      if (total - start <= DOWNLOAD_BATCH_SIZE) end = total;
      
      logger.info("  Processing {} to {}", start+1, end);
      
      List<String> conceptCodes = allConcepts.subList(start, end).stream().map(c -> c.getCode()).collect(Collectors.toList());
      List<Concept> concepts = sparqlQueryManagerService.getConcepts(conceptCodes, terminology);
      
      for(Concept concept: concepts) {
        String json = concept.toString();
        String filePath = CONCEPTS_OUT_DIR + concept.getCode() + ".json";
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(json);
        writer.close();
      }
      
      start = end;
      end = end + DOWNLOAD_BATCH_SIZE;
    }
    
    Files.createFile(FileSystems.getDefault().getPath(CONCEPTS_OUT_DIR, LOCK_FILE));
    logger.info("Download process complete!");
  }
  
  //load concepts to es in batches - from files
  private void loadConceptsFromFiles() throws IOException, InterruptedException {
    logger.info("Loading concepts from files");
    File conceptsDir = new File(CONCEPTS_OUT_DIR);
    File lockFile = new File(CONCEPTS_OUT_DIR + LOCK_FILE);
    
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
    
    Double total = (double)jsonFiles.length;
    
    int start = 0;
    int end = INDEX_BATCH_SIZE;

    Double taskSize = Math.ceil(total/INDEX_BATCH_SIZE);  
    
    logger.info("   task size: {}", taskSize);
    
    CountDownLatch latch = new CountDownLatch(taskSize.intValue());
    ExecutorService executor = Executors.newFixedThreadPool(10);
    
    while(start < total) {
      if (total - start <= INDEX_BATCH_SIZE) end = total.intValue();
      
      logger.info("   processing {} to {}", start, end);
      
      File[] files = Arrays.copyOfRange(jsonFiles, start, end);
      List<Concept> concepts = new ArrayList<>(files.length);
      ObjectMapper mapper = new ObjectMapper();
      for(File file: files) {
        byte[] data = Files.readAllBytes(Paths.get(file.toURI()));
        String strData = new String(data , "Windows-1252");
        Concept concept = mapper.readValue(strData.getBytes("UTF-8"), Concept.class);
        concepts.add(concept);
      }
      
      executor.submit(new LoadTask(concepts, start, end, latch));
      
      start = end;
      end = end + INDEX_BATCH_SIZE;
    }
    
    latch.await();
    
    executor.shutdown();
  }
  
  //load concepts to es in batches -- in real time
  private void loadConceptsRealTime(List<Concept> allConcepts, Terminology terminology) throws IOException, InterruptedException {
    logger.info("loading concepts real time");
    
    if (CollectionUtils.isEmpty(allConcepts)) {
      logger.warn("Unable to load. No concepts found!");
      return;
    }
    
    Double total = (double)allConcepts.size();
    
    int start = 0;
    int end = INDEX_BATCH_SIZE;

    Double taskSize = Math.ceil(total/INDEX_BATCH_SIZE);  
    
    logger.info("   task size: {}", taskSize);
    
    CountDownLatch latch = new CountDownLatch(taskSize.intValue());
    ExecutorService executor = Executors.newFixedThreadPool(10);
    
    while(start < total) {
      if (total - start <= INDEX_BATCH_SIZE) end = total.intValue();
      
      logger.info("   processing {} to {}", start, end);
      
      List<String> conceptCodes = allConcepts.subList(start, end).stream().map(c -> c.getCode()).collect(Collectors.toList());
      List<Concept> concepts = sparqlQueryManagerService.getConcepts(conceptCodes, terminology);
      
      executor.submit(new LoadTask(concepts, start, end, latch));
      
      start = end;
      end = end + INDEX_BATCH_SIZE;
    }
    
    latch.await();
    
    executor.shutdown();
  }
  
  private class LoadTask implements Callable<Void> {
    
    private List<Concept> concepts;
    private int startIndex;
    private int endIndex;
    private CountDownLatch latch;
    
    public LoadTask(List<Concept> concepts, int start, int end, CountDownLatch latch) {
      this.concepts = concepts;
      this.startIndex = start;
      this.endIndex = end;
      this.latch = latch;
    }
    
    @Override
    public Void call() {
      try {
        esOperations.loadConcepts(concepts, ElasticOperationsService.CONCEPT_INDEX, ElasticOperationsService.CONCEPT_TYPE, null);
      } catch (IOException e) {
        logger.error("Error loading concepts: {} to {}", startIndex, endIndex);
        logger.error(e.getMessage(), e);
      } finally {
        latch.countDown();
        logger.info("   latch count: {}", latch.getCount());
      }
      
      return null;
    }
  }

}
