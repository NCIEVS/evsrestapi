package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.AssociationEntry;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Qualifier;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.MainTypeHierarchy;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.util.CollectionUtils;

/** The implementation for {@link ElasticLoadService}. */
// @Service
public abstract class AbstractStardogLoadServiceImpl extends BaseLoaderService {
  /** the logger *. */
  private static final Logger logger =
      LoggerFactory.getLogger(AbstractStardogLoadServiceImpl.class);

  /** constant value for mapping string */
  public static final String NCIT_MAPS_TO = "NCIt_Maps_To_";

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
  @Autowired Environment env;

  /** The Elasticsearch operations service instance *. */
  @Autowired ElasticOperationsService operationsService;

  /** The Elasticsearch query service instance *. */
  @Autowired ElasticQueryService esQueryService;

  /** The sparql query manager service. */
  @Autowired private SparqlQueryManagerService sparqlQueryManagerService;

  /** The stardog properties. */
  @Autowired StardogProperties stardogProperties;

  /** The main type hierarchy. */
  @Autowired MainTypeHierarchy mainTypeHierarchy;

  /** the sparql query service impl. */
  @Autowired private SparqlQueryManagerServiceImpl sparqlQueryManagerServiceImpl;

  /** The sparql query cache service */
  @Autowired SparqlQueryCacheService sparqlQueryCacheService;

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
  public int loadConcepts(
      ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
      throws Exception {

    logger.debug(
        "ElasticLoadServiceImpl::load() - index = {}, type = {}", terminology.getIndexName());

    boolean result =
        operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService
          .getOpenSearchOperations()
          .indexOps(IndexCoordinates.of(terminology.getIndexName()))
          .putMapping(Concept.class);
    }

    // Get complex roles and inverse roles
    try {
      logger.info("Load complex roles");
      hierarchy.setRoleMap(
          sparqlQueryManagerService.getComplexRolesForAllCodes(terminology, false));
      logger.info("Load complex inverse roles");
      hierarchy.setInverseRoleMap(
          sparqlQueryManagerService.getComplexRolesForAllCodes(terminology, true));
      logger.info("Load all associations");
      hierarchy.setAssociationMap(
          sparqlQueryManagerService.getAssociationsForAllCodes(terminology, false));
      logger.info("Load all inverse roles");
      hierarchy.setInverseAssociationMap(
          sparqlQueryManagerService.getAssociationsForAllCodes(terminology, true));
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
  private void loadConceptsRealTime(
      List<Concept> allConcepts, Terminology terminology, HierarchyUtils hierarchy)
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
        if (total - start <= DOWNLOAD_BATCH_SIZE) end = total.intValue();

        logger.info("  Processing {} to {}", start + 1, end);
        logger.info("    start reading {} to {}", start + 1, end);
        List<Concept> concepts =
            sparqlQueryManagerService.getConcepts(
                allConcepts.subList(start, end), terminology, hierarchy);
        logger.info("    finish reading {} to {}", start + 1, end);

        logger.info("    start computing extensions and history {} to {}", start + 1, end);
        concepts.stream()
            .forEach(
                c -> {
                  // logger.info(" concept = " + c.getCode() + " " + c.getName());
                  c.setExtensions(mainTypeHierarchy.getExtensions(c));
                  handleHistory(terminology, c);
                  if (c.getMaps().size() > 0 && c.getActive()) {
                    for (final Mapping map : c.getMaps()) {
                      final String mapterm = map.getTargetTerminology().split(" ")[0];
                      if (mapsets.containsKey(mapterm)) {
                        final Mapping copy = new Mapping(map);
                        copy.setSourceCode(c.getCode());
                        copy.setSourceName(c.getName());
                        copy.setSource(c.getTerminology());
                        copy.setSourceTerminology(c.getTerminology());
                        if (map.getTargetTerminology().split(" ").length > 1) {
                          copy.setTargetTerminology(mapterm);
                          copy.setTarget(mapterm);
                          copy.setTargetTerminologyVersion(
                              map.getTargetTerminology().split(" ")[1]);
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
          if (indexTotal - indexStart <= INDEX_BATCH_SIZE) indexEnd = indexTotal.intValue();

          futures.add(
              executor.submit(
                  new ConceptLoadTask(
                      concepts.subList(indexStart, indexEnd),
                      start + indexStart,
                      start + indexEnd,
                      terminology.getIndexName(),
                      latch,
                      taskSize.intValue())));

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
        // TEMP FIX FOR NOSUCHINDEXERROR THROWN BY DELETING A MAPPING INDEX
        try {
          operationsService.delete(
              ElasticOperationsService.MAPSET_INDEX, NCIT_MAPS_TO + mapset.getKey());
          operationsService.deleteQuery(
              "mapsetCode:" + NCIT_MAPS_TO + mapset.getKey(),
              ElasticOperationsService.MAPPINGS_INDEX);
        } catch (NoSuchIndexException e) {
          logger.warn("UNABLE TO DELETE INDEX: " + NCIT_MAPS_TO + mapset.getKey() + " NOT FOUND!");
        }
        Collections.sort(
            mapset.getValue().getMaps(),
            new Comparator<Mapping>() {
              @Override
              public int compare(final Mapping o1, final Mapping o2) {
                // Assume maps are not null
                return (o1.getSourceName()
                        + o1.getType()
                        + o1.getGroup()
                        + o1.getRank()
                        + o1.getTargetName())
                    .compareTo(
                        o2.getSourceName()
                            + o2.getType()
                            + o2.getGroup()
                            + o2.getRank()
                            + o2.getTargetName());
              }
            });
        logger.info(
            "    Index map = "
                + mapset.getValue().getName()
                + ", "
                + mapset.getValue().getMaps().size());
        operationsService.bulkIndex(
            mapset.getValue().getMaps(), ElasticOperationsService.MAPPINGS_INDEX, Mapping.class);
        mapset.getValue().setMaps(null);
        operationsService.index(
            mapset.getValue(), ElasticOperationsService.MAPSET_INDEX, Concept.class);
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
  public void loadObjects(
      ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
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

    if (terminology.getMetadata().getHierarchy() != null
        && terminology.getMetadata().getHierarchy()) {
      ElasticObject hierarchyObject = new ElasticObject("hierarchy");
      hierarchyObject.setHierarchy(hierarchy);
      operationsService.index(hierarchyObject, indexName, ElasticObject.class);
      logger.info("  Hierarchy loaded = " + hierarchy.getHierarchyRoots());
    } else {
      logger.info("  Hierarchy skipped");
    }

    List<ConceptMinimal> synonymSources = sparqlQueryManagerService.getSynonymSources(terminology);
    ElasticObject ssObject = new ElasticObject("synonym_sources");
    ssObject.setConceptMinimals(synonymSources);
    operationsService.index(ssObject, indexName, ElasticObject.class);
    logger.info("  Synonym Sources loaded");

    List<Concept> qualifiers =
        sparqlQueryManagerService.getAllQualifiersCache(terminology, new IncludeParam("full"));
    ElasticObject conceptsObject = new ElasticObject("qualifiers");
    conceptsObject.setConcepts(qualifiers);
    // Get qualifier values by code and by qualifier name
    final Map<String, Set<String>> qualMap = new HashMap<>();
    for (final Concept qualifier : qualifiers) {
      for (final String value :
          sparqlQueryManagerService.getQualifierValues(qualifier.getCode(), terminology)) {
        if (!qualMap.containsKey(qualifier.getCode())) {
          qualMap.put(qualifier.getCode(), new HashSet<>());
        }
        qualMap.get(qualifier.getCode()).add(value);
        if (!qualMap.containsKey(qualifier.getName())) {
          qualMap.put(qualifier.getName(), new HashSet<>());
        }
        qualMap.get(qualifier.getName()).add(value);
      }
    }
    ConceptUtils.limitQualMap(qualMap, 1000);
    conceptsObject.setMap(qualMap);
    operationsService.index(conceptsObject, indexName, ElasticObject.class);
    logger.info("  Qualifiers loaded");

    List<Concept> properties =
        sparqlQueryManagerService.getAllProperties(terminology, new IncludeParam("full"));
    ElasticObject propertiesObject = new ElasticObject("properties");
    propertiesObject.setConcepts(properties);
    operationsService.index(propertiesObject, indexName, ElasticObject.class);
    logger.info("  Properties loaded");

    List<Concept> associations =
        sparqlQueryManagerService.getAllAssociations(terminology, new IncludeParam("full"));
    ElasticObject associationsObject = new ElasticObject("associations");
    associationsObject.setConcepts(associations);
    operationsService.index(associationsObject, indexName, ElasticObject.class);
    logger.info("  Associations loaded");

    List<Concept> roles =
        sparqlQueryManagerService.getAllRoles(terminology, new IncludeParam("full"));
    ElasticObject rolesObject = new ElasticObject("roles");
    rolesObject.setConcepts(roles);
    operationsService.index(rolesObject, indexName, ElasticObject.class);
    logger.info("  Roles loaded");

    // synonymTypes
    List<Concept> synonymTypes =
        sparqlQueryManagerService.getAllSynonymTypes(terminology, new IncludeParam("full"));
    ElasticObject synonymTypesObject = new ElasticObject("synonymTypes");
    synonymTypesObject.setConcepts(synonymTypes);
    operationsService.index(synonymTypesObject, indexName, ElasticObject.class);
    logger.info("  Synonym Types loaded");

    // definitionTypes
    List<Concept> definitionTypes =
        sparqlQueryManagerService.getAllDefinitionTypes(terminology, new IncludeParam("full"));
    ElasticObject definitionTypesObject = new ElasticObject("definitionTypes");
    definitionTypesObject.setConcepts(definitionTypes);
    operationsService.index(definitionTypesObject, indexName, ElasticObject.class);
    logger.info("  Definition Types loaded");

    // subsets
    List<Concept> subsets = sparqlQueryManagerServiceImpl.getAllSubsets(terminology);
    if (terminology.getTerminology().equals("ncit")) {
      this.addExtraSubsets(subsets, terminology);
    }
    ElasticObject subsetsObject = new ElasticObject("subsets");
    subsetsObject.setConcepts(subsets);
    operationsService.index(subsetsObject, indexName, ElasticObject.class);
    logger.info("  Subsets loaded");
    List<AssociationEntry> entries = new ArrayList<>();
    // associationEntries
    for (Concept association : associations) {
      logger.info(association.getName());
      entries = new ArrayList<>();
      if (association.getName().equals("Concept_In_Subset")) continue;
      for (String conceptCode : hierarchy.getAssociationMap().keySet()) {
        List<Association> conceptAssociations = hierarchy.getAssociationMap().get(conceptCode);
        for (Association conceptAssociation : conceptAssociations) {
          if (association.getCode().equals(conceptAssociation.getCode())) {
            entries.add(
                convert(
                    terminology,
                    conceptCode,
                    hierarchy.getConceptNameFromCode(conceptCode),
                    conceptAssociation));
          }
        }
      }
      ElasticObject associationEntriesObject =
          new ElasticObject("associationEntries_" + association.getName());
      logger.info("    add associationEntries_" + association.getName() + " = " + entries.size());
      associationEntriesObject.setAssociationEntries(entries);
      operationsService.index(associationEntriesObject, indexName, ElasticObject.class);
    }
    logger.info("  Association Entries loaded");

    logger.info("Done loading Elastic Objects!");
  }

  public void addExtraSubsets(List<Concept> existingSubsets, Terminology terminology)
      throws Exception {

    String filePath =
        this.applicationProperties.getUnitTestData()
            + this.applicationProperties.getPediatricSubsetsXls();

    String url =
        this.applicationProperties.getFtpNeoplasmUrl()
            + this.applicationProperties.getPediatricSubsetsXls();
    // List to hold the sheet references
    List<Sheet> sheets = loadExcelSheets(url, filePath);

    for (Sheet sheet : sheets) {
      String subsetCode = sheet.getRow(1).getCell(1).getStringCellValue();
      // find the concept to add to the subsets list
      Concept newSubsetEntry = new Concept();
      try {
        newSubsetEntry =
            esQueryService
                .getConcept(subsetCode, terminology, new IncludeParam("full"))
                .orElseThrow();
      } catch (NoSuchElementException e) {
        logger.error("Subset " + subsetCode + " not found as a concept, skipping.");
        continue;
      }

      // get the subset and concept that the new subset is a part of i.e. pediatric subset part of
      // ncit subset
      Map<String, String> newSubsets = terminology.getMetadata().getExtraSubsets();
      Concept parentSubset = new Concept();
      try {
        parentSubset =
            existingSubsets.stream()
                .flatMap(Concept::streamSelfAndChildren)
                .filter(subset -> subset.getCode().equals(newSubsets.get(subsetCode)))
                .findFirst()
                .orElseThrow();
      } catch (NoSuchElementException e) {
        logger.error(
            "Parent Subset of "
                + subsetCode
                + ": "
                + newSubsets.get(subsetCode)
                + " not found, skipping.");
        continue;
      }

      boolean isFirstRow = true;
      for (Row row : sheet) {
        // skip labels row
        if (isFirstRow) {
          isFirstRow = false;
          continue;
        }
        Concept subsetConcept = new Concept();
        try {
          subsetConcept =
              esQueryService
                  .getConcept(
                      row.getCell(2).getStringCellValue(), terminology, new IncludeParam("full"))
                  .get();
        } catch (Exception e) {
          logger.error(
              "Concept "
                  + row.getCell(2).getStringCellValue()
                  + " not found for new subset "
                  + subsetCode
                  + ", skipping.");
          continue;
        }

        // make new computed association for the subset members
        final Association conceptAssoc = new Association();
        conceptAssoc.setType("Concept_In_Subset");
        conceptAssoc.setRelatedCode(newSubsetEntry.getCode());
        conceptAssoc.setRelatedName(newSubsetEntry.getName());

        final Qualifier conceptComputed = new Qualifier();
        conceptComputed.setValue("This is computed by the existing subset relationship");
        conceptComputed.setType("computed");
        conceptAssoc.getQualifiers().add(conceptComputed);

        subsetConcept.getAssociations().add(conceptAssoc);
        // edit codes for inverseAssociation
        Association inverseAssoc = new Association(conceptAssoc);
        inverseAssoc.setRelatedCode(subsetConcept.getCode());
        inverseAssoc.setRelatedName(subsetConcept.getName());
        newSubsetEntry.getInverseAssociations().add(inverseAssoc);
        // index subsetConcept
        operationsService.index(subsetConcept, terminology.getIndexName(), Concept.class);
      }
      // explicitly set leaf since it defaults to false
      newSubsetEntry.setLeaf(!newSubsets.containsValue(newSubsetEntry.getCode()));
      // add extra relevant properties to new subset
      newSubsetEntry.getProperties().add(new Property("Publish_Value_Set", "Yes"));
      newSubsetEntry.getProperties().add(new Property("EVSRESTAPI_Subset_Format", "NCI"));
      // index newSubsetEntry
      operationsService.index(newSubsetEntry, terminology.getIndexName(), Concept.class);
      // create new subset for parentSubset to add as child of existing subset
      Concept parentSubsetChild = new Concept(newSubsetEntry);
      // strip out everything the subset tree doesn't need
      ConceptUtils.applyInclude(parentSubsetChild, new IncludeParam("minimal,properties"));

      parentSubset.getChildren().add(parentSubsetChild);
      if (parentSubset.getProperties().stream()
          .noneMatch(property -> property.getType().equals("Publish_Value_Set"))) {
        parentSubset.getProperties().add(new Property("Publish_Value_Set", "Yes"));
      }
      parentSubset.getProperties().add(new Property("EVSRESTAPI_Subset_Format", "NCI"));
      // index parentSubset
      operationsService.index(parentSubset, terminology.getObjectIndexName(), Concept.class);
    }
    logger.info("Extra subsets added");
  }

  public static List<Sheet> loadExcelSheets(String url, String filepath) throws Exception {
    List<Sheet> sheets = new ArrayList<>();
    Workbook workbook = null;

    try {
      // Try downloading the file from the provided URL
      InputStream inputStream = new URL(url).openStream();
      workbook = new HSSFWorkbook(inputStream);
      System.out.println("Excel file successfully loaded from URL.");
    } catch (Exception e) {
      System.err.println("Failed to download Excel file from URL. Error: " + e.getMessage());
      try {
        // If download fails, fall back to the local backup file
        FileInputStream fileInputStream = new FileInputStream(filepath);
        workbook = new HSSFWorkbook(fileInputStream);
        System.out.println("Excel file successfully loaded from backup file.");
      } catch (Exception fileException) {
        logger.error(
            "Failed to load Excel file from backup file. Error: " + fileException.getMessage());
        throw new Exception(fileException); // throw an exception if all attempts fail
      }
    }

    // Extract sheets from the workbook
    if (workbook != null) {
      for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        sheets.add(workbook.getSheetAt(i));
      }
      try {
        workbook.close(); // Close the workbook to release resources
      } catch (IOException e) {
        logger.error("Failed to close the workbook. Error: " + e.getMessage());
        throw new IOException(e); // throw an exception if failed to close
      }
    }

    return sheets;
  }

  private AssociationEntry convert(
      Terminology terminology,
      String conceptCode,
      String conceptName,
      Association conceptAssociation) {
    AssociationEntry entry = new AssociationEntry();
    entry.setCode(conceptCode);
    entry.setAssociation(conceptAssociation.getType());
    entry.setRelatedCode(conceptAssociation.getRelatedCode());
    entry.setRelatedName(conceptAssociation.getRelatedName());
    entry.setVersion(terminology.getVersion());
    entry.setTerminology(terminology.getTerminology());
    entry.setName(conceptName);
    return entry;
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
    public ConceptLoadTask(
        List concepts, int start, int end, String indexName, CountDownLatch latch, int taskSize)
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
        operationsService.bulkIndex(concepts, indexName, Concept.class);
        int progress = (int) Math.floor((1.0 - 1.0 * latch.getCount() / taskSize) * 100);
        taskLogger.info(
            "    finish loading concepts: {} to {} ({}% complete)",
            startIndex + 1, endIndex, progress);
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
  public Terminology getTerminology(
      ApplicationContext app,
      ElasticLoadConfig config,
      String filepath,
      String terminology,
      boolean forceDelete)
      throws Exception {
    TerminologyUtils termUtils = app.getBean(TerminologyUtils.class);
    final Terminology term =
        termUtils.getTerminology(config.getTerminology(), sparqlQueryManagerService);

    // Attempt to read the config, if anything goes wrong
    // the config file is probably not there
    try {

      // Load from config
      final JsonNode node = getMetadataAsNode(terminology.toLowerCase());
      final TerminologyMetadata metadata =
          new ObjectMapper().treeToValue(node, TerminologyMetadata.class);

      // Set term name and description
      term.setName(metadata.getUiLabel() + " " + term.getVersion());
      if (term.getDescription() == null || term.getDescription().isEmpty()) {
        if (node.get("description") != null) {
          term.setDescription(node.get("description").asText());
        }
      }
      if (node.get("description") != null) {
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
        metadata.setConceptStatuses(
            sparqlQueryManagerService.getDistinctPropertyValues(term, metadata.getConceptStatus()));
        // IF this is just the single value "true", then instead set to
        // "Retired_Concept"
        if (metadata.getConceptStatuses().size() == 1
            && "true".equals(metadata.getConceptStatuses().get(0))) {
          metadata.getConceptStatuses().clear();
          metadata.getConceptStatuses().add("Retired_Concept");
        }
      }

      // Compute definition sources
      if (metadata.getDefinitionSource() != null) {
        metadata.setDefinitionSourceSet(
            sparqlQueryManagerService.getDefinitionSources(term).stream()
                .map(d -> d.getCode())
                .collect(Collectors.toSet()));
      }

      // Setup maps if this is a "monthly" version
      // Determine by checking against the stardog db we are loading from
      if (term.getTerminology() == "ncit"
          && stardogProperties.getDb().equals(term.getMetadata().getMonthlyDb())) {

        // setup mappings
        Concept ncitMapsToGdc = setupMap("GDC", term.getVersion());
        Concept ncitMapsToIcd10 = setupMap("ICD10", term.getVersion());
        Concept ncitMapsToIcd10cm = setupMap("ICD10CM", term.getVersion());
        Concept ncitMapsToIcd9cm = setupMap("ICD9CM", term.getVersion());
        Concept ncitMapsToIcdo3 = setupMap("ICDO3", term.getVersion());
        Concept ncitMapsToMeddra = setupMap("MedDRA", term.getVersion());
        mapsets.put("GDC", ncitMapsToGdc);
        mapsets.put("ICD10", ncitMapsToIcd10);
        mapsets.put("ICD10CM", ncitMapsToIcd10cm);
        mapsets.put("ICD9CM", ncitMapsToIcd9cm);
        mapsets.put("ICDO3", ncitMapsToIcdo3);
        mapsets.put("MedDRA", ncitMapsToMeddra);
        logger.info("mapsets = " + mapsets.size());
      }

    } catch (Exception e) {
      throw new Exception(
          "Unexpected error trying to load metadata = " + applicationProperties.getConfigBaseUri(),
          e);
    }

    // Compute tags because this is the new terminology
    // Do this AFTER setting terminology metadata, which is needed
    if (term.getMetadata().getMonthlyDb() != null) {
      termUtils.setTags(term, stardogProperties.getDb());
    }

    return term;
  }

  /**
   * Setup map.
   *
   * @param term the term
   * @param version the version
   * @return the concept
   */
  private Concept setupMap(String term, String version) {
    Concept map = new Concept();
    map.setCode(NCIT_MAPS_TO + term);
    map.setName(NCIT_MAPS_TO + term);
    map.setVersion(version);
    map.setActive(true);
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
      logger.info("Load ncit history");

      // Assume this file is checked. It's passed in by the reindex.sh
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")); ) {

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
      logger.info("    count = " + historyMap.size());
    } catch (Exception e) {
      throw new Exception(
          "Unable to load history file for " + terminology.getName() + ": " + filepath, e);
    }
  }

  /**
   * Handle history.
   *
   * @param terminology the terminology
   * @param concept the concept
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
    final HierarchyUtils hierarchy = sparqlQueryManagerService.getHierarchyUtilsCache(term);

    return hierarchy;
  }
}
