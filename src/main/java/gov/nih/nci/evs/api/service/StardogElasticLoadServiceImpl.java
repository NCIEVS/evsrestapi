
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
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
public class StardogElasticLoadServiceImpl extends BaseLoaderService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(StardogElasticLoadServiceImpl.class);

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
  public int loadConcepts(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy, CommandLine cmd) throws IOException {

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

    return allConcepts.size();

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
      final List<Future<Void>> futures = new ArrayList<>();
      while (indexStart < indexTotal) {
        if (indexTotal - indexStart <= INDEX_BATCH_SIZE)
          indexEnd = indexTotal.intValue();

        futures.add(executor
            .submit(new ConceptLoadTask(concepts.subList(indexStart, indexEnd), start + indexStart,
                start + indexEnd, terminology.getIndexName(), latch, taskSize.intValue())));

        indexStart = indexEnd;
        indexEnd = indexEnd + INDEX_BATCH_SIZE;

      }
      // Look for exceptions
      for (final Future<Void> future : futures) {
        // This throws an exception if the callable had an issue
        future.get();
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

    ElasticObject hierarchyObject = new ElasticObject("hierarchy");
    hierarchyObject.setHierarchy(hierarchy);
    operationsService.index(hierarchyObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);
    logger.info("  Hierarchy loaded");

    List<ConceptMinimal> synonymSources = sparqlQueryManagerService.getSynonymSources(terminology);
    ElasticObject ssObject = new ElasticObject("synonym_sources");
    ssObject.setConceptMinimals(synonymSources);
    operationsService.index(ssObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);
    logger.info("  Synonym Sources loaded");

    List<Concept> qualifiers =
        sparqlQueryManagerService.getAllQualifiers(terminology, new IncludeParam("full"));
    ElasticObject conceptsObject = new ElasticObject("qualifiers");
    conceptsObject.setConcepts(qualifiers);
    operationsService.index(conceptsObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);
    logger.info("  Qualifiers loaded");

    List<Concept> properties =
        sparqlQueryManagerService.getAllProperties(terminology, new IncludeParam("full"));
    ElasticObject propertiesObject = new ElasticObject("properties");
    propertiesObject.setConcepts(properties);
    operationsService.index(propertiesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);
    logger.info("  Properties loaded");

    List<Concept> associations =
        sparqlQueryManagerService.getAllAssociations(terminology, new IncludeParam("full"));
    ElasticObject associationsObject = new ElasticObject("associations");
    associationsObject.setConcepts(associations);
    operationsService.index(associationsObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);
    logger.info("  Associations loaded");

    List<Concept> roles =
        sparqlQueryManagerService.getAllRoles(terminology, new IncludeParam("full"));
    ElasticObject rolesObject = new ElasticObject("roles");
    rolesObject.setConcepts(roles);
    operationsService.index(rolesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);
    logger.info("  Roles loaded");

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
     * @throws Exception the exception
     */
    @SuppressWarnings("rawtypes")
    public ConceptLoadTask(List concepts, int start, int end, String indexName,
        CountDownLatch latch, int taskSize) throws Exception {
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
        throw new Exception(e);
      } finally {
        concepts = null;
        latch.countDown();
      }

      return null;
    }
  }

  /* see superclass */
  @Override
  public Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config, CommandLine cmd)
    throws Exception {
    TerminologyUtils termUtils = app.getBean(TerminologyUtils.class);
    final Terminology term = termUtils.getTerminology(config.getTerminology(), false);

    // Attempt to read the config, if anything goes wrong
    // the config file is probably not there
    final String resource = "metadata/" + term.getTerminology() + ".json";
    try {
      TerminologyMetadata metadata = new ObjectMapper().readValue(
          IOUtils.toString(term.getClass().getClassLoader().getResourceAsStream(resource), "UTF-8"),
          TerminologyMetadata.class);
      term.setMetadata(metadata);
    } catch (Exception e) {
      throw new Exception("Unexpected error trying to load = " + resource, e);
    }
    return term;
  }

  /* see superclass */
  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term)
    throws JsonParseException, JsonMappingException, IOException {
    return sparqlQueryManagerService.getHierarchyUtils(term);
  }
}
