
package gov.nih.nci.evs.api.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Property;
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
  private static final Logger logger = LoggerFactory.getLogger(AbstractStardogLoadServiceImpl.class);

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

  /** The name map. */
  private Map<String, String> nameMap = new HashMap<>();

  /** The history map. */
  private Map<String, List<Map<String, String>>> historyMap = new HashMap<>();

  /** The simple date format. */
  private SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd-MMM-yy");

  /** The simple date format. */
  private SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd");

  /** The mapsets. */
  Map<String, Concept> mapsets = new HashMap<>();

  /* see superclass */
  @Override
  public int loadConcepts(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
    throws IOException {

    logger.debug("ElasticLoadServiceImpl::load() - index = {}, type = {}", terminology.getIndexName(),
        ElasticOperationsService.CONCEPT_TYPE);

    boolean result = operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService.getElasticsearchOperations().putMapping(terminology.getIndexName(),
          ElasticOperationsService.CONCEPT_TYPE, Concept.class);
    }

    // Get complex roles and inverse roles
    try {
      logger.info("Load complex roles");
      hierarchy.setRoleMap(sparqlQueryManagerService.getComplexRolesForAllCodes(terminology, false));
      logger.info("Load complex inverse roles");
      hierarchy.setInverseRoleMap(sparqlQueryManagerService.getComplexRolesForAllCodes(terminology, true));
      logger.info("Load all associations");
      hierarchy.setAssociationMap(sparqlQueryManagerService.getAssociationsForAllCodes(terminology, false));
      logger.info("Load all inverse roles");
      hierarchy.setInverseAssociationMap(sparqlQueryManagerService.getAssociationsForAllCodes(terminology, true));
    } catch (Exception e1) {
      throw new IOException(e1);
    }

    logger.info("Getting all concepts without codes");
    List<Concept> concepts = sparqlQueryManagerService.getAllConceptsWithoutCode(terminology);
    int ct = concepts.size();

    try {
      logger.info("Loading concepts without codes");
      loadConceptsRealTime(concepts, terminology, hierarchy);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new IOException(e);
    }

    concepts = sparqlQueryManagerService.getAllConceptsWithCode(terminology);
    ct += concepts.size();

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
  private void loadConceptsRealTime(List<Concept> allConcepts, Terminology terminology, HierarchyUtils hierarchy)
    throws Exception {
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

    // build up a map of concept codes and names to use for history
    if (historyMap.size() > 0) {

      for (final Concept concept : allConcepts) {
        nameMap.put(concept.getCode(), concept.getName());
      }
    }

    CountDownLatch latch = new CountDownLatch(taskSize.intValue());
    ExecutorService executor = Executors.newFixedThreadPool(10);
    try {
      while (start < total) {
        if (total - start <= DOWNLOAD_BATCH_SIZE)
          end = total.intValue();

        logger.info("  Processing {} to {}", start + 1, end);
        logger.info("    start reading {} to {}", start + 1, end);
        List<Concept> concepts =
            sparqlQueryManagerService.getConcepts(allConcepts.subList(start, end), terminology, hierarchy);
        logger.info("    finish reading {} to {}", start + 1, end);

        logger.info("    start computing extensions and history {} to {}", start + 1, end);
        concepts.stream().forEach(c -> {
          // logger.info(" concept = " + c.getCode() + " " + c.getName());
          c.setExtensions(mainTypeHierarchy.getExtensions(c));
          handleHistory(terminology, c);
          if (c.getMaps().size() > 0) {
            for (final gov.nih.nci.evs.api.model.Map map : c.getMaps()) {
              final String mapterm = map.getTargetTerminology().split(" ")[0];
              if (mapsets.containsKey(mapterm)) {
                final gov.nih.nci.evs.api.model.Map copy = new gov.nih.nci.evs.api.model.Map(map);
                copy.setSourceCode(c.getCode());
                copy.setSourceName(c.getName());
                copy.setSourceTerminology(c.getTerminology());
                if (map.getTargetTerminology().split(" ").length > 1) {
                  copy.setTargetTerminology(mapterm);
                  copy.setTargetTerminologyVersion(map.getTargetTerminology().split(" ")[1]);
                }
                mapsets.get(mapterm).getMaps().add(copy);
              }
            }
          }
          // if (c.getExtensions() != null) {
          // logger.info(" extensions " + c.getCode() + " = " +
          // c.getExtensions());
          // }
        });
        logger.info("    finish computing extensions {} to {}", start + 1, end);

        int indexStart = 0;
        int indexEnd = INDEX_BATCH_SIZE;
        Double indexTotal = (double) concepts.size();
        final List<Future<Void>> futures = new ArrayList<>();
        while (indexStart < indexTotal) {
          if (indexTotal - indexStart <= INDEX_BATCH_SIZE)
            indexEnd = indexTotal.intValue();

          futures.add(executor.submit(new ConceptLoadTask(concepts.subList(indexStart, indexEnd), start + indexStart,
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

      // resetting and indexing mapsets
      for (Map.Entry<String, Concept> mapset : mapsets.entrySet()) {
        operationsService.delete(ElasticOperationsService.MAPPING_INDEX, ElasticOperationsService.CONCEPT_TYPE,
            "NCIt_Maps_To_" + mapset.getKey());
        Collections.sort(mapset.getValue().getMaps(), new Comparator<gov.nih.nci.evs.api.model.Map>() {
          @Override
          public int compare(final gov.nih.nci.evs.api.model.Map o1, final gov.nih.nci.evs.api.model.Map o2) {
            // Assume maps are not null
            return (o1.getSourceName() + o1.getType() + o1.getGroup() + o1.getRank() + o1.getTargetName())
                .compareTo(o2.getSourceName() + o2.getType() + o2.getGroup() + o2.getRank() + o2.getTargetName());
          }
        });
        logger.info("    Index map = " + mapset.getValue().getName() + ", " + mapset.getValue().getMaps().size());
        operationsService.index(mapset.getValue(), ElasticOperationsService.MAPPING_INDEX,
            ElasticOperationsService.CONCEPT_TYPE, Concept.class);
      }

    } catch (Exception e) {
      logger.info("  shutdown now");
      executor.shutdownNow();
      logger.info("  await termination");
      executor.awaitTermination(30, TimeUnit.SECONDS);
      throw e;
    }
    logger.info("Done loading concepts!");
  }

  /* see superclass */
  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
    throws Exception {
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

    if (terminology.getMetadata().getHierarchy() != null && terminology.getMetadata().getHierarchy()) {
      ElasticObject hierarchyObject = new ElasticObject("hierarchy");
      hierarchyObject.setHierarchy(hierarchy);
      operationsService.index(hierarchyObject, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
      logger.info("  Hierarchy loaded = " + hierarchy.getHierarchyRoots());
    } else {
      logger.info("  Hierarchy skipped");
    }

    List<ConceptMinimal> synonymSources = sparqlQueryManagerService.getSynonymSources(terminology);
    ElasticObject ssObject = new ElasticObject("synonym_sources");
    ssObject.setConceptMinimals(synonymSources);
    operationsService.index(ssObject, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
    logger.info("  Synonym Sources loaded");

    List<Concept> qualifiers = sparqlQueryManagerService.getAllQualifiers(terminology, new IncludeParam("full"));
    ElasticObject conceptsObject = new ElasticObject("qualifiers");
    conceptsObject.setConcepts(qualifiers);
    // Get qualifier values by code and by qualifier name
    final Map<String, Set<String>> map = new HashMap<>();
    for (final Concept qualifier : qualifiers) {
      for (final String value : sparqlQueryManagerService.getQualifierValues(qualifier.getCode(), terminology)) {
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
    operationsService.index(conceptsObject, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
    logger.info("  Qualifiers loaded");

    List<Concept> properties = sparqlQueryManagerService.getAllProperties(terminology, new IncludeParam("full"));
    ElasticObject propertiesObject = new ElasticObject("properties");
    propertiesObject.setConcepts(properties);
    operationsService.index(propertiesObject, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
    logger.info("  Properties loaded");

    List<Concept> associations = sparqlQueryManagerService.getAllAssociations(terminology, new IncludeParam("full"));
    ElasticObject associationsObject = new ElasticObject("associations");
    associationsObject.setConcepts(associations);
    operationsService.index(associationsObject, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
    logger.info("  Associations loaded");

    List<Concept> roles = sparqlQueryManagerService.getAllRoles(terminology, new IncludeParam("full"));
    ElasticObject rolesObject = new ElasticObject("roles");
    rolesObject.setConcepts(roles);
    operationsService.index(rolesObject, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
    logger.info("  Roles loaded");

    // synonymTypes
    List<Concept> synonymTypes = sparqlQueryManagerService.getAllSynonymTypes(terminology, new IncludeParam("full"));
    ElasticObject synonymTypesObject = new ElasticObject("synonymTypes");
    synonymTypesObject.setConcepts(synonymTypes);
    operationsService.index(synonymTypesObject, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
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
    subsetsObject.setConcepts(subsets);
    operationsService.index(subsetsObject, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
    logger.info("  Subsets loaded");

    // associationEntries
    for (Concept association : associations) {
      logger.info(association.getName());
      if (association.getName().equals("Concept_In_Subset"))
        continue;
      List<AssociationEntry> entries = sparqlQueryManagerService.getAssociationEntries(terminology, association);
      ElasticObject associationEntriesObject = new ElasticObject("associationEntries_" + association.getName());
      logger.info("    add associationEntries_" + association.getName() + " = " + entries.size());
      associationEntriesObject.setAssociationEntries(entries);
      operationsService.index(associationEntriesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
          ElasticObject.class);
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
    public ConceptLoadTask(List concepts, int start, int end, String indexName, CountDownLatch latch, int taskSize)
        throws Exception {
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
        operationsService.bulkIndex(concepts, indexName, ElasticOperationsService.CONCEPT_TYPE, Concept.class);
        int progress = (int) Math.floor((1.0 - 1.0 * latch.getCount() / taskSize) * 100);
        taskLogger.info("    finish loading concepts: {} to {} ({}% complete)", startIndex + 1, endIndex, progress);
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
  public Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config, String filepath,
    String terminology, boolean forceDelete) throws Exception {
    TerminologyUtils termUtils = app.getBean(TerminologyUtils.class);
    final Terminology term = termUtils.getTerminology(config.getTerminology(), false);

    // Attempt to read the config, if anything goes wrong
    // the config file is probably not there
    try {

      // Load from config
      final JsonNode node = getMetadataAsNode(terminology.toLowerCase());
      final TerminologyMetadata metadata = new ObjectMapper().treeToValue(node, TerminologyMetadata.class);

      // Set term name and description
      term.setName(metadata.getUiLabel() + " " + term.getVersion());
      if (term.getDescription() == null || term.getDescription().isEmpty()) {
        term.setDescription(node.get("description").asText());
      }

      // Set some flags
      metadata.setLoader("rdf");
      metadata.setSourceCt(metadata.getSources().size());
      metadata.setWelcomeText(getWelcomeText(terminology.toLowerCase()));
      term.setMetadata(metadata);
      loadHistory(term, filepath);

      // Compute concept statuses
      if (metadata.getConceptStatus() != null) {
        metadata
            .setConceptStatuses(sparqlQueryManagerService.getDistinctPropertyValues(term, metadata.getConceptStatus()));
        // IF this is just the single value "true", then instead set to
        // "Retired_Concept"
        if (metadata.getConceptStatuses().size() == 1 && "true".equals(metadata.getConceptStatuses().get(0))) {
          metadata.getConceptStatuses().clear();
          metadata.getConceptStatuses().add("Retired_Concept");
        }
      }

      // Compute definition sources
      if (metadata.getDefinitionSource() != null) {
        metadata.setDefinitionSourceSet(sparqlQueryManagerService.getDefinitionSources(term).stream()
            .map(d -> d.getCode()).collect(Collectors.toSet()));
      }

      // Setup maps if this is a "monthly" version
      // Determine by checking against the stardog db we are loading from
      if (term.getTerminology() == "ncit" && stardogProperties.getDb().equals(term.getMetadata().getMonthlyDb())) {

        // setup mappings
        Concept ncitMapsToGdc = setupMap("GDC", term.getTerminologyVersion());
        Concept ncitMapsToIcd10 = setupMap("ICD10", term.getTerminologyVersion());
        Concept ncitMapsToIcd10cm = setupMap("ICD10CM", term.getTerminologyVersion());
        Concept ncitMapsToIcd9cm = setupMap("ICD9CM", term.getTerminologyVersion());
        Concept ncitMapsToIcdo3 = setupMap("ICDO3", term.getTerminologyVersion());
        Concept ncitMapsToMeddra = setupMap("MedDRA", term.getTerminologyVersion());
        mapsets.put("GDC", ncitMapsToGdc);
        mapsets.put("ICD10", ncitMapsToIcd10);
        mapsets.put("ICD10CM", ncitMapsToIcd10cm);
        mapsets.put("ICD9CM", ncitMapsToIcd9cm);
        mapsets.put("ICDO3", ncitMapsToIcdo3);
        mapsets.put("MedDRA", ncitMapsToMeddra);
        logger.info("mapsets = " + mapsets.size());
      }

    } catch (Exception e) {
      throw new Exception("Unexpected error trying to load metadata = " + applicationProperties.getConfigBaseUri(), e);
    }

    // Compute tags because this is the new terminology
    // Do this AFTER setting terminology metadata, which is needed
    if (term.getMetadata().getMonthlyDb() != null) {
      termUtils.setTags(term, stardogProperties.getDb());
    }

    return term;
  }

  /**
   * 
   * @param term the terminology
   * @param version the version
   * @return
   */
  private Concept setupMap(String term, String version) {
    Concept map = new Concept();
    map.setCode("NCIt_Maps_To_" + term);
    map.setName("NCIt_Maps_To_" + term);
    map.setVersion(version);
    map.getProperties().add(new Property("downloadOnly", "true"));
    map.getProperties().add(new Property("mapsetLink", null));
    map.getProperties().add(new Property("loader", "AbstractStardogLoadServiceImpl"));
    return map;
  }

  /**
   * Load (and cache) the history info for all concepts.
   *
   * @param terminology the terminology
   * @param filepath the file path
   * @throws Exception the exception
   */
  private void loadHistory(final Terminology terminology, final String filepath) throws Exception {

    // Only load history for NCI Thesaurus
    if (!terminology.getTerminology().equals("ncit") || filepath == null || filepath.isEmpty()) {
      return;
    }

    try {

      File file = new File(filepath);
      logger.debug("Loading History for NCIT");

      // Assume this file is checked. It's passed in by the reindex.sh
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));) {

        String line = null;

        // CODE, NA, ACTION, DATE, REPLACEMENT CODE
        // Loop through lines until we reach "the end"
        while ((line = reader.readLine()) != null) {

          final String[] fields = line.split("\\|", -1);
          final String code = fields[0];
          final String action = fields[2];
          final String date = fields[3];
          final String replacementCode = fields[4];
          List<Map<String, String>> conceptHistory = new ArrayList<>();
          final Map<String, String> historyItem = new HashMap<>();

          if (historyMap.containsKey(code)) {
            conceptHistory = historyMap.get(code);
          }

          historyItem.put("action", action);
          historyItem.put("date", date);

          if (replacementCode != null && !replacementCode.equals("null")) {

            historyItem.put("replacementCode", replacementCode);

            // create history entry for the replacement concept if it isn't merging with itself
            if (!replacementCode.equals(code)) {

              List<Map<String, String>> replacementConceptHistory = new ArrayList<>();
              final Map<String, String> replacementHistoryItem = new HashMap<>(historyItem);

              if (historyMap.containsKey(replacementCode)) {
                replacementConceptHistory = historyMap.get(replacementCode);
              }

              replacementHistoryItem.put("code", code);
              replacementConceptHistory.add(replacementHistoryItem);
              historyMap.put(replacementCode, replacementConceptHistory);
            }
          }

          conceptHistory.add(historyItem);
          historyMap.put(code, conceptHistory);
        }
      }
    } catch (Exception e) {
      throw new Exception("Unable to load history file for " + terminology.getName() + ": " + filepath, e);
    }
  }

  /**
   * Handle history.
   *
   * @param terminology the terminology
   * @param Concept the concept
   * @throws Exception the exception
   */
  private void handleHistory(final Terminology terminology, final Concept concept) {

    try {

      List<Map<String, String>> conceptHistory = historyMap.get(concept.getCode());

      if (conceptHistory == null) {
        return;
      }

      for (final Map<String, String> historyItem : conceptHistory) {

        final History history = new History();

        // replacement concept history items will contain a key for code
        if (historyItem.containsKey("code")) {
          history.setCode(historyItem.get("code"));
        } else {
          history.setCode(concept.getCode());
        }

        history.setAction(historyItem.get("action"));

        final String date = outputDateFormat.format(inputDateFormat.parse(historyItem.get("date")));
        history.setDate(date);

        final String replacementCode = historyItem.get("replacementCode");

        if (replacementCode != null && replacementCode != "") {

          history.setReplacementCode(replacementCode);
          history.setReplacementName(nameMap.get(replacementCode));
        }

        concept.getHistory().add(history);
      }

    } catch (Exception e) {
      logger.error("Problem loading history for concept " + concept.getCode() + ".", e);
    }
  }

  /* see superclass */
  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term) throws Exception {
    final HierarchyUtils hierarchy = sparqlQueryManagerService.getHierarchyUtils(term);

    return hierarchy;
  }
}
