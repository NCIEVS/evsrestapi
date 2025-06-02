package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.AssociationEntry;
import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.History;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Qualifier;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.properties.GraphProperties;
import gov.nih.nci.evs.api.support.es.OpensearchLoadConfig;
import gov.nih.nci.evs.api.support.es.OpensearchObject;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.MainTypeHierarchy;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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

/** The implementation for {@link OpensearchLoadService}. */
// @Service
public abstract class AbstractGraphLoadServiceImpl extends BaseLoaderService {
  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(AbstractGraphLoadServiceImpl.class);

  /** constant value for mapping string. */
  public static final String NCIT_MAPS_TO = "NCIt_Maps_To_";

  /** the concepts download location *. */
  @Value("${nci.evs.bulkload.historyDir}")
  private String HISTORY_DIR;

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

  /** The Opensearch operations service instance *. */
  @Autowired OpensearchOperationsService operationsService;

  /** The Opensearch query service instance *. */
  @Autowired OpensearchQueryService opensearchQueryService;

  /** The sparql query manager service. */
  @Autowired private SparqlQueryManagerService sparqlQueryManagerService;

  /** The graph db properties. */
  @Autowired GraphProperties graphProperties;

  /** The main type hierarchy. */
  @Autowired MainTypeHierarchy mainTypeHierarchy;

  /** the sparql query service impl. */
  @Autowired private SparqlQueryManagerServiceImpl sparqlQueryManagerServiceImpl;

  /** The sparql query cache service. */
  @Autowired SparqlQueryCacheService sparqlQueryCacheService;

  /** The name map. */
  private Map<String, String> nameMap = new HashMap<>();

  /** The simple date format. */
  private SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd-MMM-yy");

  /** The simple date format. */
  private SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd");

  /** The mapsets. */
  Map<String, Concept> mapsets = new HashMap<>();

  /* see superclass */
  @Override
  public int loadConcepts(
      OpensearchLoadConfig config,
      Terminology terminology,
      HierarchyUtils hierarchy,
      Map<String, List<Map<String, String>>> historyMap)
      throws Exception {

    logger.debug(
        "OpensearchLoadServiceImpl::load() - index = {}, type = {}", terminology.getIndexName());

    boolean result =
        operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService
          .getOpenSearchOperations()
          .indexOps(IndexCoordinates.of(terminology.getIndexName()))
          .putMapping(Concept.class);
    }

    // Get complex roles and inverse roles
    logger.info("Load complex roles");
    hierarchy.setRoleMap(sparqlQueryManagerService.getComplexRolesForAllCodes(terminology, false));
    logger.info("Load complex inverse roles");
    hierarchy.setInverseRoleMap(
        sparqlQueryManagerService.getComplexRolesForAllCodes(terminology, true));
    logger.info("Load all associations");
    hierarchy.setAssociationMap(
        sparqlQueryManagerService.getAssociationsForAllCodes(terminology, false));
    logger.info("Load all inverse roles");
    hierarchy.setInverseAssociationMap(
        sparqlQueryManagerService.getAssociationsForAllCodes(terminology, true));

    logger.info("Getting all concepts without codes");
    List<Concept> concepts = sparqlQueryManagerService.getAllConceptsWithoutCode(terminology);
    int ct = concepts.size();

    logger.info("Loading concepts without codes");
    loadConceptsRealTime(concepts, terminology, hierarchy, historyMap);

    concepts = sparqlQueryManagerService.getAllConceptsWithCode(terminology);
    ct += concepts.size();

    // download concepts and upload to es in real time
    logger.info("Loading concepts with codes");
    loadConceptsRealTime(concepts, terminology, hierarchy, historyMap);

    return ct;
  }

  /**
   * load concepts directly from graph db in batches.
   *
   * @param allConcepts all concepts to load
   * @param terminology the terminology
   * @param hierarchy the hierarchy
   * @throws Exception the exception
   */
  private void loadConceptsRealTime(
      List<Concept> allConcepts,
      Terminology terminology,
      HierarchyUtils hierarchy,
      Map<String, List<Map<String, String>>> historyMap)
      throws Exception {
    logger.info("  download batch size = " + DOWNLOAD_BATCH_SIZE);
    logger.info("  index batch size = " + INDEX_BATCH_SIZE);

    // Check assumptions
    if (DOWNLOAD_BATCH_SIZE < INDEX_BATCH_SIZE) {
      throw new Exception("The download batch size must not be less than the index batch size");
    }

    if (CollectionUtils.isEmpty(allConcepts)) {
      logger.warn("Unable to load. No concepts found!");
      Audit.addAudit(
          operationsService,
          "No concepts found",
          "loadConceptsRealTime",
          terminology.getTerminology(),
          "No concepts found!",
          "WARN");
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
                  try {
                    handleHistory(terminology, c, historyMap);
                  } catch (Exception e) {
                    logger.error("Error handling history for concept " + c.getCode(), e);
                    // needs an extra try-catching because we're in a forEach
                    try {
                      Audit.addAudit(
                          operationsService,
                          "Exception",
                          "loadConceptsRealTime",
                          terminology.getTerminology(),
                          "Error handling history for concept " + c.getCode(),
                          "ERROR");
                    } catch (Exception e1) {
                      logger.error(e1.getMessage(), e1);
                    }
                  }
                  // Collect maps for NCIt mapsets
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
                        copy.setMapsetCode(mapsets.get(mapterm).getCode());
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
              OpensearchOperationsService.MAPSET_INDEX, NCIT_MAPS_TO + mapset.getKey());
          operationsService.deleteQuery(
              "mapsetCode:" + NCIT_MAPS_TO + mapset.getKey(),
              OpensearchOperationsService.MAPPINGS_INDEX);
        } catch (NoSuchIndexException e) {
          logger.warn("UNABLE TO DELETE INDEX: " + NCIT_MAPS_TO + mapset.getKey() + " NOT FOUND!");
          Audit.addAudit(
              operationsService,
              "NoSuchIndexException",
              "loadConceptsRealTime",
              terminology.getTerminology(),
              "UNABLE TO DELETE INDEX: " + NCIT_MAPS_TO + mapset.getKey() + " NOT FOUND!",
              "WARN");
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
            mapset.getValue().getMaps(), OpensearchOperationsService.MAPPINGS_INDEX, Mapping.class);
        mapset.getValue().setMaps(null);
        operationsService.index(
            mapset.getValue(), OpensearchOperationsService.MAPSET_INDEX, Concept.class);
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
      OpensearchLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
      throws Exception {
    String indexName = terminology.getObjectIndexName();
    logger.info("Loading Opensearch Objects");
    logger.debug("object index name: {}", indexName);
    boolean result = operationsService.createIndex(indexName, config.isForceDeleteIndex());
    logger.debug("index result: {}", result);

    // No need to put mapping directly, we really just need to index the id
    // if (result) {
    // logger.debug("put mapping");
    // operationsService.getElasticsearchOperations().putMapping(terminology.getIndexName(),
    // OpensearchOperationsService.OBJECT_TYPE, OpensearchObjectMapping.class);
    // }

    if (terminology.getMetadata().getHierarchy() != null
        && terminology.getMetadata().getHierarchy()) {
      OpensearchObject hierarchyObject = new OpensearchObject("hierarchy");
      hierarchyObject.setHierarchy(hierarchy);
      operationsService.index(hierarchyObject, indexName, OpensearchObject.class);
      logger.info("  Hierarchy loaded = " + hierarchy.getHierarchyRoots());
    } else {
      logger.info("  Hierarchy skipped");
    }

    List<ConceptMinimal> synonymSources = sparqlQueryManagerService.getSynonymSources(terminology);
    OpensearchObject ssObject = new OpensearchObject("synonym_sources");
    ssObject.setConceptMinimals(synonymSources);
    operationsService.index(ssObject, indexName, OpensearchObject.class);
    logger.info("  Synonym Sources loaded");

    List<Concept> qualifiers =
        sparqlQueryManagerService.getAllQualifiersCache(terminology, new IncludeParam("full"));
    OpensearchObject conceptsObject = new OpensearchObject("qualifiers");
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
    operationsService.index(conceptsObject, indexName, OpensearchObject.class);
    logger.info("  Qualifiers loaded");

    List<Concept> properties =
        sparqlQueryManagerService.getAllProperties(terminology, new IncludeParam("full"));
    OpensearchObject propertiesObject = new OpensearchObject("properties");
    propertiesObject.setConcepts(properties);
    operationsService.index(propertiesObject, indexName, OpensearchObject.class);
    logger.info("  Properties loaded");

    List<Concept> associations =
        sparqlQueryManagerService.getAllAssociations(terminology, new IncludeParam("full"));
    OpensearchObject associationsObject = new OpensearchObject("associations");
    associationsObject.setConcepts(associations);
    operationsService.index(associationsObject, indexName, OpensearchObject.class);
    logger.info("  Associations loaded");

    List<Concept> roles =
        sparqlQueryManagerService.getAllRoles(terminology, new IncludeParam("full"));
    OpensearchObject rolesObject = new OpensearchObject("roles");
    rolesObject.setConcepts(roles);
    operationsService.index(rolesObject, indexName, OpensearchObject.class);
    logger.info("  Roles loaded");

    // synonymTypes
    List<Concept> synonymTypes =
        sparqlQueryManagerService.getAllSynonymTypes(terminology, new IncludeParam("full"));
    OpensearchObject synonymTypesObject = new OpensearchObject("synonymTypes");
    synonymTypesObject.setConcepts(synonymTypes);
    operationsService.index(synonymTypesObject, indexName, OpensearchObject.class);
    logger.info("  Synonym Types loaded");

    // definitionTypes
    List<Concept> definitionTypes =
        sparqlQueryManagerService.getAllDefinitionTypes(terminology, new IncludeParam("full"));
    OpensearchObject definitionTypesObject = new OpensearchObject("definitionTypes");
    definitionTypesObject.setConcepts(definitionTypes);
    operationsService.index(definitionTypesObject, indexName, OpensearchObject.class);
    logger.info("  Definition Types loaded");

    // subsets
    List<Concept> subsets = sparqlQueryManagerServiceImpl.getAllSubsets(terminology);
    // Handle the pediatric neoplasm "extra" subset data for NCIt
    if (terminology.getTerminology().equals("ncit")) {
      this.addExtraSubsets(subsets, terminology);
    }
    OpensearchObject subsetsObject = new OpensearchObject("subsets");
    subsetsObject.setConcepts(subsets);
    operationsService.index(subsetsObject, indexName, OpensearchObject.class);
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
      OpensearchObject associationEntriesObject =
          new OpensearchObject("associationEntries_" + association.getName());
      logger.info("    add associationEntries_" + association.getName() + " = " + entries.size());
      associationEntriesObject.setAssociationEntries(entries);
      operationsService.index(associationEntriesObject, indexName, OpensearchObject.class);
    }
    logger.info("  Association Entries loaded");

    logger.info("Done loading Opensearch Objects!");
  }

  /**
   * Adds the extra subsets.
   *
   * @param existingSubsets the existing subsets
   * @param terminology the terminology
   * @throws Exception the exception
   */
  public void addExtraSubsets(List<Concept> existingSubsets, Terminology terminology)
      throws Exception {

    String filePath =
        this.applicationProperties.getUnitTestData()
            + this.applicationProperties.getChildhoodNeoplasmSubsetsXls();

    String url =
        this.applicationProperties.getFtpNeoplasmUrl()
            + this.applicationProperties.getChildhoodNeoplasmSubsetsXls();
    // List to hold the sheet references
    List<Sheet> sheets = loadExcelSheets(url, filePath);

    for (Sheet sheet : sheets) {
      String subsetCode = sheet.getRow(1).getCell(1).getStringCellValue();
      // find the concept to add to the subsets list
      Concept subsetConcept = new Concept();
      try {
        // We can't use "full" here or we wind up losing "extensions" and "paths"
        // So we use the "everything" mode
        subsetConcept =
            opensearchQueryService
                .getConcept(subsetCode, terminology, new IncludeParam("*"))
                .orElseThrow();
      } catch (NoSuchElementException e) {
        logger.warn("Subset " + subsetCode + " not found as a concept, skipping.");
        Audit.addAudit(
            operationsService,
            "NoSuchElementException",
            "addExtraSubsets",
            terminology.getTerminology(),
            "Subset " + subsetCode + " not found as a concept, skipping.",
            "WARN");
        // We don't want to fail here but keep going
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
        logger.warn(
            "Parent Subset of "
                + subsetCode
                + ": "
                + newSubsets.get(subsetCode)
                + " not found, skipping.");
        Audit.addAudit(
            operationsService,
            "NoSuchElementException",
            "addExtraSubsets",
            terminology.getTerminology(),
            "Parent Subset of "
                + subsetCode
                + ": "
                + newSubsets.get(subsetCode)
                + " not found, skipping.",
            "WARN");
        continue;
      }

      boolean isFirstRow = true;
      List<String> missingConcepts = new ArrayList<>();
      for (Row row : sheet) {
        // skip labels row
        if (isFirstRow) {
          isFirstRow = false;
          continue;
        }
        Concept subsetMember = new Concept();
        try {
          // We can't use "full" here or we wind up losing "extensions" and "paths"
          // So we use the "everything" mode
          subsetMember =
              opensearchQueryService
                  .getConcept(
                      row.getCell(2).getStringCellValue(), terminology, new IncludeParam("*"))
                  .get();
        } catch (Exception e) {
          logger.warn(
              "Concept "
                  + row.getCell(2).getStringCellValue()
                  + " not found for new subset "
                  + subsetCode
                  + ", skipping.");
          missingConcepts.add(row.getCell(2).getStringCellValue());
          continue;
        }

        // make new computed association for the subset members
        final Association conceptAssoc = new Association();
        conceptAssoc.setType("Concept_In_Subset");
        conceptAssoc.setRelatedCode(subsetConcept.getCode());
        conceptAssoc.setRelatedName(subsetConcept.getName());

        final Qualifier conceptComputed = new Qualifier();
        conceptComputed.setValue("This is computed by the existing subset relationship");
        conceptComputed.setType("computed");
        conceptAssoc.getQualifiers().add(conceptComputed);

        // handle the case where the code is itself a member in its own subset
        if (subsetMember.getCode().equals(subsetConcept.getCode())) {
          subsetConcept.getAssociations().add(conceptAssoc);
        } else {
          subsetMember.getAssociations().add(conceptAssoc);
        }
        // edit codes for inverseAssociation
        Association inverseAssoc = new Association(conceptAssoc);
        inverseAssoc.setRelatedCode(subsetMember.getCode());
        inverseAssoc.setRelatedName(subsetMember.getName());
        subsetConcept.getInverseAssociations().add(inverseAssoc);
        // index subsetMember (but only if the member is not also the subset concept itself)
        if (!subsetMember.getCode().equals(subsetConcept.getCode())) {
          operationsService.index(subsetMember, terminology.getIndexName(), Concept.class);
        }
      }
      if (missingConcepts.size() > 0) {
        Audit.addAudit(
            operationsService,
            "Missing Subset Concepts",
            "addExtraSubsets",
            terminology.getTerminology(),
            "Concepts " + missingConcepts + " not found for new subset " + subsetCode + ".",
            "WARN");
      }
      // explicitly set leaf since it defaults to false
      subsetConcept.setLeaf(!newSubsets.containsValue(subsetConcept.getCode()));
      // add extra relevant properties to new subset
      subsetConcept.getProperties().add(new Property("Publish_Value_Set", "Yes"));
      subsetConcept.getProperties().add(new Property("EVSRESTAPI_Subset_Format", "NCI"));
      subsetConcept.setSubsetLink(url);

      // index subsetConcept
      operationsService.index(subsetConcept, terminology.getIndexName(), Concept.class);
      // create new subset for parentSubset to add as child of existing subset
      Concept parentSubsetChild = new Concept(subsetConcept);
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

  /**
   * Load excel sheets.
   *
   * @param url the url
   * @param filepath the filepath
   * @return the list
   * @throws Exception the exception
   */
  @SuppressWarnings("resource")
  public static List<Sheet> loadExcelSheets(String url, String filepath) throws Exception {
    List<Sheet> sheets = new ArrayList<>();
    Workbook workbook = null;

    try (InputStream inputStream = new URL(url).openStream(); ) {
      // Try downloading the file from the provided URL
      workbook = new HSSFWorkbook(inputStream);
      logger.info("Excel file successfully loaded from URL.");
    } catch (Exception e) {
      logger.error("Failed to download Excel file from URL. Error: " + e.getMessage());
      try (final FileInputStream fileInputStream = new FileInputStream(filepath)) {
        // If download fails, fall back to the local backup file
        workbook = new HSSFWorkbook(fileInputStream);
        logger.info("Excel file successfully loaded from backup file.");
      } catch (Exception fileException) {
        logger.error(
            "Failed to load Excel file from backup file. Error: " + fileException.getMessage());
        // throw an exception if all attempts fail
        throw fileException;
      }
    }

    // Extract sheets from the workbook
    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
      sheets.add(workbook.getSheetAt(i));
    }
    // Close the workbook to release resources
    workbook.close();

    return sheets;
  }

  /**
   * Convert.
   *
   * @param terminology the terminology
   * @param conceptCode the concept code
   * @param conceptName the concept name
   * @param conceptAssociation the concept association
   * @return the association entry
   */
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
   * Task to load a batch of concepts to Opensearch.
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
        taskLogger.error("Unexpected error loading concepts", e);
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
      OpensearchLoadConfig config,
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
      // Determine by checking against the graph db we are loading from
      if (term.getTerminology() == "ncit"
          && graphProperties.getDb().equals(term.getMetadata().getMonthlyDb())) {

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
      termUtils.setTags(term, graphProperties.getDb());
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
    map.getProperties().add(new Property("loader", "AbstractGraphLoadServiceImpl"));
    return map;
  }

  /**
   * Handle history.
   *
   * @param terminology the terminology
   * @param concept the concept
   * @throws Exception the exception
   */
  private void handleHistory(
      final Terminology terminology,
      final Concept concept,
      Map<String, List<Map<String, String>>> historyMap)
      throws Exception {

    List<Map<String, String>> conceptHistory = historyMap.get(concept.getCode());

    if (conceptHistory == null || conceptHistory.isEmpty()) {
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
  }

  /**
   * Update historyMap.
   *
   * @param terminology the terminology
   * @param oldFilepath the old file path
   * @param newFilepath the new file path
   * @throws Exception the exception
   */
  public Map<String, List<Map<String, String>>> updateHistoryMap(
      final Terminology terminology, final String filepath) throws Exception {

    if (!terminology.getTerminology().equals("ncit")) {
      return new HashMap<>();
    }
    if (filepath == null || filepath.isEmpty()) {
      logger.warn("File path is null or empty, returning empty history map.");
      Audit.addAudit(
          operationsService,
          "FilePathException",
          "updateHistoryMap",
          terminology.getTerminology(),
          "File path is null or empty.",
          "WARN");
      return new HashMap<>();
    }
    Map<String, List<Map<String, String>>> historyMap = new HashMap<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(new FileInputStream(filepath), "UTF-8")); ) {
      String line = null;
      historyMap = processHistoryMap(line, reader);
    } catch (IOException e) {
      logger.error("Error reading history file: " + filepath, e);
      Audit.addAudit(
          operationsService,
          "IOException",
          "updateHistoryMap",
          terminology.getTerminology(),
          "Error reading history file: " + filepath,
          "ERROR");
      throw new Exception("Error reading history file: " + filepath, e);
    }
    String historyVersion = filepath.split("cumulative_history_")[1].split("\\.txt")[0];
    terminology.getMetadata().setHistoryVersion(historyVersion);
    return historyMap;
  }

  /** process history map. */
  public Map<String, List<Map<String, String>>> processHistoryMap(
      String line, BufferedReader reader) throws Exception {
    // CODE, NA, ACTION, DATE, REPLACEMENT CODE
    // Loop through lines until we reach "the end"
    Map<String, List<Map<String, String>>> historyMap = new HashMap<>();

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
    return historyMap;
  }

  /**
   * update history
   *
   * @param terminology the terminology
   * @param file the file path
   * @throws Exception the exception
   */
  public void updateHistory(
      final Terminology terminology,
      Map<String, List<Map<String, String>>> historyMap,
      String newHistoryVersion)
      throws Exception {

    // Update history for "ncit" monthly when it gets revisitied to double check latest versions
    // Skip this for other terminologies, for cases where an updated cumulative history file has
    // already been processed
    // or in cases where the cumulative history for this version is unable to be found
    if (!terminology.getTerminology().equals("ncit")
        || historyMap == null
        || historyMap.size() == 0
        || newHistoryVersion.equals(terminology.getVersion())
        || terminology.getMetadata().getHistoryVersion().equals(terminology.getVersion())) {
      return;
    }

    Date startOfUpdateScope = parseVersion(terminology.getVersion());
    Map<String, List<Map<String, String>>> historyMapUpdate = new HashMap<>();
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");

    // Loop through the historyMap and check if the date is within the update scope
    for (final String code : historyMap.keySet()) {
      for (final Map<String, String> map : historyMap.get(code)) {
        if (sdf.parse(map.get("date")).after(startOfUpdateScope)) {
          // If the date is within the scope, add it to historyMapUpdate and break off
          historyMapUpdate.put(code, historyMap.get(code));
          break;
        }
      }
    }

    IncludeParam ip = new IncludeParam("*");
    for (Map.Entry<String, List<Map<String, String>>> entry : historyMapUpdate.entrySet()) {
      Concept concept = opensearchQueryService.getConcept(entry.getKey(), terminology, ip).get();
      handleHistory(terminology, concept, historyMap);
      // index the concept with the history
      operationsService.index(concept, terminology.getIndexName(), Concept.class);
    }
    terminology.getMetadata().setHistoryVersion(newHistoryVersion);
  }

  public Date parseVersion(String version) {
    int year = 2000 + Integer.parseInt(version.substring(0, 2));
    int month = Integer.parseInt(version.substring(3, 5));

    Calendar cal = Calendar.getInstance();
    // set to first of the month to ignore date
    cal.set(year, month, 1);
    // go back 2 months to cover everything
    cal.add(Calendar.MONTH, -2);

    return cal.getTime();
  }

  /* see superclass */
  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term) throws Exception {
    final HierarchyUtils hierarchy = sparqlQueryManagerService.getHierarchyUtilsCache(term);

    return hierarchy;
  }
}
