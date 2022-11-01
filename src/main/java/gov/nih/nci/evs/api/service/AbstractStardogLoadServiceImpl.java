
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.AssociationEntry;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.MainTypeHierarchy;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * The implementation for {@link ElasticLoadService}.
 */
// @Service
public abstract class AbstractStardogLoadServiceImpl extends BaseLoaderService {
  /** the logger *. */
  private static final Logger logger =
      LoggerFactory.getLogger(AbstractStardogLoadServiceImpl.class);

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

  /** The stardog properties. */
  @Autowired
  StardogProperties stardogProperties;

  /** The main type hierarchy. */
  @Autowired
  MainTypeHierarchy mainTypeHierarchy;

  /** the sparql query service impl. */
  @Autowired
  private SparqlQueryManagerServiceImpl sparqlQueryManagerServiceImpl;

  /* see superclass */
  @Override
  public int loadConcepts(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws IOException {

    logger.debug("ElasticLoadServiceImpl::load() - index = {}, type = {}",
        terminology.getIndexName(), ElasticOperationsService.CONCEPT_TYPE);

    boolean result =
        operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService.getElasticsearchOperations().putMapping(terminology.getIndexName(),
          ElasticOperationsService.CONCEPT_TYPE, Concept.class);
    }

    logger.info("Getting all concepts without codes");
    List<Concept> concepts = sparqlQueryManagerService.getAllConceptsWithoutCode(terminology);
    int ct = concepts.size();

    // For loading these concepts, use "rdfs:about" as the #{codeCode}
    final String codeCode = terminology.getMetadata().getCode();
    terminology.getMetadata().setCode("rdfs:about");

    try {
      logger.info("Loading concepts without codes");
      loadConceptsRealTime(concepts, terminology, hierarchy);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new IOException(e);
    }

    concepts = sparqlQueryManagerService.getAllConceptsWithCode(terminology);
    ct += concepts.size();

    // Restore the #{codeCode} here
    terminology.getMetadata().setCode(codeCode);

    try {
      // download concepts and upload to es in real time
      logger.info("Loading concepts with codes");
      loadConceptsRealTime(concepts, terminology, hierarchy);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new IOException(e);
    }

    return ct;

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

    logger.info("  Initialize main type hierarchy");
    mainTypeHierarchy.initialize(terminology, hierarchy);

    logger.info("  Total concepts to load: {}", allConcepts.size());

    Double total = (double) allConcepts.size();

    int start = 0;
    int end = DOWNLOAD_BATCH_SIZE;

    Double taskSize = Math.ceil(total / INDEX_BATCH_SIZE);

    CountDownLatch latch = new CountDownLatch(taskSize.intValue());
    ExecutorService executor = Executors.newFixedThreadPool(10);
    try {
      while (start < total) {
        if (total - start <= DOWNLOAD_BATCH_SIZE)
          end = total.intValue();

        logger.info("  Processing {} to {}", start + 1, end);
        logger.info("    start reading {} to {}", start + 1, end);
        List<Concept> concepts = sparqlQueryManagerService
            .getConcepts(allConcepts.subList(start, end), terminology, hierarchy);
        logger.info("    finish reading {} to {}", start + 1, end);

        logger.info("    start computing extensions {} to {}", start + 1, end);
        concepts.stream()
            // .peek(c -> logger.info(" concept = " + c.getCode() + " " +
            // c.getName()))
            .peek(c -> c.setExtensions(mainTypeHierarchy.getExtensions(c)))
            // .peek(c -> logger.info(" extensions = " + c.getExtensions()))
            .count();
        logger.info("    finish computing extensions {} to {}", start + 1, end);

        int indexStart = 0;
        int indexEnd = INDEX_BATCH_SIZE;
        Double indexTotal = (double) concepts.size();
        final List<Future<Void>> futures = new ArrayList<>();
        while (indexStart < indexTotal) {
          if (indexTotal - indexStart <= INDEX_BATCH_SIZE)
            indexEnd = indexTotal.intValue();

          futures.add(executor.submit(
              new ConceptLoadTask(concepts.subList(indexStart, indexEnd), start + indexStart,
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

    } catch (Exception e) {
      logger.info("  shutdown now");
      executor.shutdownNow();
      logger.info("  await termination");
      executor.awaitTermination(30, TimeUnit.SECONDS);
      throw e;
    }
    logger.info("Done loading concepts!");
  }

  /**
   * add subset links to subset hierarchy.
   *
   * @param subset the subset
   * @param subsetLinks the subset links
   * @param subsetPrefix the subset prefix
   */
  private void addSubsetLinks(Concept subset, Map<String, String> subsetLinks,
    String subsetPrefix) {
    if (subsetLinks.containsKey(subset.getCode())) {
      subset.setSubsetLink(subsetPrefix + subsetLinks.get(subset.getCode()));
    }
    for (Concept child : subset.getChildren()) {
      addSubsetLinks(child, subsetLinks, subsetPrefix);
    }
  }

  /* see superclass */
  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws Exception {
    String indexName = terminology.getObjectIndexName();
    logger.info("Loading Elastic Objects");
    logger.debug("object index name: {}", indexName);
    boolean result = operationsService.createIndex(indexName, config.isForceDeleteIndex());
    logger.debug("index result: {}", result);

    // No need to put mapping directly, we really just need to index the id
    // if (result) {
    // logger.debug("put mapping");
    // operationsService.getElasticsearchOperations().putMapping(terminology.getIndexName(),
    // ElasticOperationsService.OBJECT_TYPE, ElasticObjectMapping.class);
    // }

    if (terminology.getMetadata().getHierarchy() != null
        && terminology.getMetadata().getHierarchy()) {
      ElasticObject hierarchyObject = new ElasticObject("hierarchy");
      hierarchyObject.setHierarchy(hierarchy);
      operationsService.index(hierarchyObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
      logger.info("  Hierarchy loaded");
    } else {
      logger.info("  Hierarchy skipped");
    }

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
    // Get qualifier values by code and by qualifier name
    final Map<String, Set<String>> map = new HashMap<>();
    for (final Concept qualifier : qualifiers) {
      for (final String value : sparqlQueryManagerService.getQualifierValues(qualifier.getCode(),
          terminology)) {
        if (!map.containsKey(qualifier.getCode())) {
          map.put(qualifier.getCode(), new HashSet<>());
        }
        map.get(qualifier.getCode()).add(value);
        if (!map.containsKey(qualifier.getName())) {
          map.put(qualifier.getName(), new HashSet<>());
        }
        map.get(qualifier.getName()).add(value);
      }
    }
    conceptsObject.setMap(map);
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

    // synonymTypes
    List<Concept> synonymTypes =
        sparqlQueryManagerService.getAllSynonymTypes(terminology, new IncludeParam("full"));
    ElasticObject synonymTypesObject = new ElasticObject("synonymTypes");
    synonymTypesObject.setConcepts(synonymTypes);
    operationsService.index(synonymTypesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);
    logger.info("  Synonym Types loaded");

    // definitionTypes
    List<Concept> definitionTypes =
        sparqlQueryManagerService.getAllDefinitionTypes(terminology, new IncludeParam("full"));
    ElasticObject definitionTypesObject = new ElasticObject("definitionTypes");
    definitionTypesObject.setConcepts(definitionTypes);
    operationsService.index(definitionTypesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);
    logger.info("  Definition Types loaded");

    // subsets
    List<Concept> subsets = sparqlQueryManagerServiceImpl.getAllSubsets(terminology);
    ElasticObject subsetsObject = new ElasticObject("subsets");
    for (Concept subset : subsets)
      addSubsetLinks(subset, terminology.getMetadata().getSubsetLinks(),
          terminology.getMetadata().getSubsetPrefix());
    subsetsObject.setConcepts(subsets);
    operationsService.index(subsetsObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);
    logger.info("  Subsets loaded");

    // associationEntries
    for (Concept association : associations) {
      logger.info(association.getName());
      if (association.getName().equals("Concept_In_Subset"))
        continue;
      List<AssociationEntry> entries =
          sparqlQueryManagerService.getAssociationEntries(terminology, association);
      ElasticObject associationEntriesObject =
          new ElasticObject("associationEntries_" + association.getName());
      logger.info("    add associationEntries_" + association.getName() + " = " + entries.size());
      associationEntriesObject.setAssociationEntries(entries);
      operationsService.index(associationEntriesObject, indexName,
          ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
    }
    logger.info("  Association Entries loaded");

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
  public Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config,
    String filepath, String terminology, boolean forceDelete) throws Exception {
    TerminologyUtils termUtils = app.getBean(TerminologyUtils.class);
    final Terminology term = termUtils.getTerminology(config.getTerminology(), false);

    // Attempt to read the config, if anything goes wrong
    // the config file is probably not there
    final String resource = "metadata/" + term.getTerminology() + ".json";
    try {
      // Load from file
      final JsonNode node = new ObjectMapper().readTree(IOUtils
          .toString(term.getClass().getClassLoader().getResourceAsStream(resource), "UTF-8"));
      TerminologyMetadata metadata =
          new ObjectMapper().treeToValue(node, TerminologyMetadata.class);

      // Set term name and description
      term.setName(metadata.getUiLabel() + " " + term.getVersion());
      if (term.getDescription() == null) {
        term.setDescription(node.get("description").asText());
      }

      // Set some flags
      metadata.setLoader("rdf");
      metadata.setSourceCt(metadata.getSources().size());
      final String welcomeResource = "metadata/" + term.getTerminology() + ".html";
      try {
        String welcomeText = IOUtils.toString(
            term.getClass().getClassLoader().getResourceAsStream(welcomeResource), "UTF-8");
        metadata.setWelcomeText(welcomeText);
      } catch (Exception e) {
        throw new Exception("Unexpected error trying to load = " + welcomeResource, e);
      }
      term.setMetadata(metadata);

      // Compute concept statuses
      if (metadata.getConceptStatus() != null) {
        metadata.setConceptStatuses(
            sparqlQueryManagerService.getDistinctPropertyValues(term, metadata.getConceptStatus()));
      }

      // Compute definition sources
      if (metadata.getDefinitionSource() != null) {
        metadata.setDefinitionSourceSet(sparqlQueryManagerService.getDefinitionSources(term)
            .stream().map(d -> d.getCode()).collect(Collectors.toSet()));
      }

    } catch (Exception e) {
      throw new Exception("Unexpected error trying to load = " + resource, e);
    }

    // Compute tags because this is the new terminology
    // Do this AFTER setting terminology metadata, which is needed
    if (term.getMetadata().getMonthlyDb() != null) {
      termUtils.setTags(term, stardogProperties.getDb());
    }

    return term;
  }

  /* see superclass */
  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term) throws Exception {
    return sparqlQueryManagerService.getHierarchyUtils(term);
  }
}
