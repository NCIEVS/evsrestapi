
package gov.nih.nci.evs.api.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.evs.ConceptNode;
import gov.nih.nci.evs.api.model.evs.EvsAssociation;
import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsConceptByCode;
import gov.nih.nci.evs.api.model.evs.EvsConceptByLabel;
import gov.nih.nci.evs.api.model.evs.EvsMapsTo;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsRelatedConcept;
import gov.nih.nci.evs.api.model.evs.EvsRelatedConceptByCode;
import gov.nih.nci.evs.api.model.evs.EvsRelatedConceptByLabel;
import gov.nih.nci.evs.api.model.evs.EvsRelationships;
import gov.nih.nci.evs.api.model.evs.EvsSynonym;
import gov.nih.nci.evs.api.model.evs.EvsSynonymByLabel;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Path;
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.model.sparql.Bindings;
import gov.nih.nci.evs.api.model.sparql.Sparql;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.properties.ThesaurusProperties;
import gov.nih.nci.evs.api.support.ConfigData;
import gov.nih.nci.evs.api.util.EVSUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.PathFinder;
import gov.nih.nci.evs.api.util.RESTUtils;

@Service
public class SparqlQueryManagerServiceImpl implements SparqlQueryManagerService {
  private static final Logger log = LoggerFactory.getLogger(SparqlQueryManagerServiceImpl.class);

  @Autowired
  StardogProperties stardogProperties;

  @Autowired
  QueryBuilderService queryBuilderService;

  @Autowired
  private ThesaurusProperties thesaurusProperties;

  @Autowired
  ApplicationProperties applicationProperties;

  private RESTUtils restUtils = null;

  private HashMap<String, String> propertyNotConsidered;

  private Long classCountMonthly;

  private Long classCountWeekly;

  private HierarchyUtils hierarchyMonthly = null;

  private HierarchyUtils hierarchyWeekly = null;

  private Paths pathsMonthly = null;

  private Paths pathsWeekly = null;

  private List<String> uniqueSourcesMonthly = null;

  private List<String> uniqueSourcesWeekly = null;

  @PostConstruct
  public void postInit() throws IOException {
    propertyNotConsidered =
        (HashMap<String, String>) thesaurusProperties.getPropertyNotConsidered();
    restUtils = new RESTUtils(stardogProperties.getUsername(), stardogProperties.getPassword(),
        stardogProperties.getReadTimeout(), stardogProperties.getConnectTimeout());
    populateCache();
    // This file generation was for the "documentation files"
    // this is no longer part of the API, no need to do this
    // if (applicationProperties.getForceFileGeneration()) {
    // genDocumentationFiles();
    // }
  }

  @Scheduled(cron = "${nci.evs.stardog.populateCacheCron}")
  public void callCronJob() throws IOException {
    LocalDateTime currentTime = LocalDateTime.now();
    log.info("callCronJob at " + currentTime);
    Long classCountMonthlyNow = getGetClassCounts("monthly");
    Long classCountWeeklyNow = getGetClassCounts("weekly");

    log.info("Current class monthly count: " + classCountMonthlyNow);
    log.info("Previous class monthly count: " + classCountMonthly);
    log.info("Current class weekly count: " + classCountWeeklyNow);
    log.info("Previous class weekly count: " + classCountWeekly);

    log.info("ForcePopulateCache " + stardogProperties.getForcePopulateCache());
    if ((classCountMonthlyNow.longValue() != classCountMonthly.longValue())
        || (classCountWeeklyNow.longValue() != classCountWeekly.longValue())
        || stardogProperties.getForcePopulateCache().equalsIgnoreCase("Y")) {
      log.info("****Repopulating cache***");
      populateCache();
      genDocumentationFiles();
      log.info("****Repopulating cache done***");
    }
  }

  public void populateCache() throws IOException {
    LocalDateTime currentTime = LocalDateTime.now();
    log.info("Start populating cache");
    classCountMonthly = getGetClassCounts("monthly");
    log.info("  class count monthly = " + classCountMonthly);

    log.info("  get hierarchy = monthly");
    List<String> parentchildMonthly = getHierarchy("monthly");
    hierarchyMonthly = new HierarchyUtils(parentchildMonthly);
    log.info("  find paths = monthly");
    PathFinder pathFinder = new PathFinder(hierarchyMonthly);
    pathsMonthly = pathFinder.findPaths();
    uniqueSourcesMonthly = getUniqueSourcesList("monthly");

    // Weekly
    if (getNamedGraph("weekly").equals(getNamedGraph("monthly"))) {
      log.info("  copy weekly to monthly = " + classCountMonthly);
      classCountWeekly = classCountMonthly;
      hierarchyWeekly = hierarchyMonthly;
      pathsWeekly = pathsMonthly;
      uniqueSourcesWeekly = uniqueSourcesMonthly;
    } else {
      classCountWeekly = getGetClassCounts("weekly");
      log.info("  class count weekly = " + classCountMonthly);
      log.info("  get hierarchy = weekly");
      List<String> parentchildWeekly = getHierarchy("weekly");
      hierarchyWeekly = new HierarchyUtils(parentchildWeekly);
      log.info("  find paths = weekly");
      pathFinder = new PathFinder(hierarchyWeekly);
      pathsWeekly = pathFinder.findPaths();
      uniqueSourcesWeekly = getUniqueSourcesList("weekly");
    }

    log.info("Done populating the cache");

  }

  public void genDocumentationFiles() throws IOException {
    String baseDirectory = applicationProperties.getGeneratedFilePath();
    ObjectMapper mapper = new ObjectMapper();
    List<EvsConcept> properties = getAllProperties("weekly", "byLabel");
    try {
      mapper.writerWithDefaultPrettyPrinter()
          .writeValue(new File(baseDirectory + "properties.json"), properties);
    } catch (IOException e) {
      e.printStackTrace();
    }
    List<EvsConcept> roles = getAllRoles("weekly", "byLabel");
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File(baseDirectory + "roles.json"),
          roles);
    } catch (IOException e) {
      e.printStackTrace();
    }
    List<EvsConcept> associations = getAllAssociations("weekly", "byLabel");
    try {
      mapper.writerWithDefaultPrettyPrinter()
          .writeValue(new File(baseDirectory + "associations.json"), associations);
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<String> conceptProperties =
        Arrays.asList("Code", "Label", "Subconcept", "Publish_Value_Set");
    EvsConcept parent = getEvsConceptByLabelProperties("C54443", "monthly", conceptProperties);

    List<HierarchyNode> nodes = getValueSetHierarchy(parent, conceptProperties);
    try {
      mapper.writerWithDefaultPrettyPrinter()
          .writeValue(new File(baseDirectory + "ValueSetHierarchy.json"), nodes);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private List<HierarchyNode> getValueSetHierarchy(EvsConcept parent,
    List<String> conceptProperties) throws IOException {
    List<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
    for (EvsRelatedConcept child : parent.getSubconcepts()) {
      EvsConcept childConcept =
          getEvsConceptByLabelProperties(child.getCode(), "monthly", conceptProperties);
      HierarchyNode node = new HierarchyNode();
      node.setCode(childConcept.getCode());
      node.setLabel(childConcept.getLabel());
      if (childConcept.getProperties().containsKey("Publish_Value_Set")) {
        if (childConcept.getProperties().get("Publish_Value_Set").get(0).equals("Yes")) {
          if (childConcept.getSubconcepts() != null) {
            List<HierarchyNode> children = getValueSetHierarchy(childConcept, conceptProperties);
            if (children.size() > 0) {
              node.setLeaf(false);
              node.setChildren(children);
            } else {
              node.setLeaf(true);
            }
          } else {
            node.setLeaf(true);
          }
          nodes.add(node);
        }
      }
    }
    return nodes;
  }

  public String getNamedGraph(String dbType) {
    if (dbType.equals("monthly")) {
      return stardogProperties.getMonthlyGraphName();
    } else {
      return stardogProperties.getWeeklyGraphName();
    }
  }

  public String getQueryURL(String dbType) {
    if (dbType.equals("monthly")) {
      return stardogProperties.getMonthlyQueryUrl();
    } else {
      return stardogProperties.getWeeklyQueryUrl();
    }
  }

  public List<String> getAllGraphNames(String dbType)
    throws JsonParseException, JsonMappingException, IOException {
    List<String> graphNames = new ArrayList<String>();
    String queryPrefix = queryBuilderService.contructPrefix();
    String query = queryBuilderService.constructAllGraphNamesQuery();
    String queryURL = getQueryURL(dbType);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    log.debug("getAllGraphNames response - " + res);
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    boolean conceptExists = false;
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();

    for (Bindings b : bindings) {
      String graphName = b.getGraphName().getValue();
      log.debug("getAllGraphNames graphName - " + graphName);
      if (graphName != null && !graphName.equalsIgnoreCase("")) {
        graphNames.add(graphName);
      }
    }

    return graphNames;
  }

  /**
   * Return the EvsVersion Information
   * 
   * @return EvsVersionInfo instance.
   */
  public EvsVersionInfo getEvsVersionInfo(String dbType)
    throws JsonParseException, JsonMappingException, IOException {

    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructVersionInfoQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    EvsVersionInfo evsVersionInfo = new EvsVersionInfo();
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      evsVersionInfo.setVersion(b.getVersion().getValue());
      evsVersionInfo.setDate(b.getDate().getValue());
      evsVersionInfo.setComment(b.getComment().getValue());
      evsVersionInfo.setGraph(namedGraph);
    }
    return evsVersionInfo;
  }

  public Long getGetClassCounts(String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructClassCountsQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    String count = "0";
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    if (bindings.length == 1) {
      count = bindings[0].getCount().getValue();
    }
    return Long.parseLong(count);
  }

  public boolean checkConceptExists(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructConceptLabelQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    boolean conceptExists = false;
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();

    for (Bindings b : bindings) {
      String conceptLabel = b.getConceptLabel().getValue();
      if (conceptLabel != null && !conceptLabel.equalsIgnoreCase("")) {
        conceptExists = true;
      }
    }

    return conceptExists;
  }

  public String getEvsConceptLabel(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructConceptLabelQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    String conceptLabel = null;
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    if (bindings.length == 1) {
      conceptLabel = bindings[0].getConceptLabel().getValue();
    }

    return conceptLabel;
  }

  public EvsConcept getEvsConceptByLabel(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByLabel evsConcept = new EvsConceptByLabel();
    getConcept(evsConcept, conceptCode, dbType, "byLabel");
    return evsConcept;
  }

  public EvsConcept getEvsConceptByCode(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByCode evsConcept = new EvsConceptByCode();
    getConcept(evsConcept, conceptCode, dbType, "byCode");
    return evsConcept;
  }

  public EvsConcept getEvsConceptByCode(String conceptCode, String dbType, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByCode evsConcept = new EvsConceptByCode();
    getConcept(evsConcept, conceptCode, dbType, ip);
    return evsConcept;
  }

  public EvsConcept getEvsConceptByLabelShort(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByLabel evsConcept = new EvsConceptByLabel();
    getConceptShort(evsConcept, conceptCode, dbType, "byLabel");
    return evsConcept;
  }

  public EvsConcept getEvsConceptByCodeShort(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByCode evsConcept = new EvsConceptByCode();
    getConceptShort(evsConcept, conceptCode, dbType, "byCode");
    return evsConcept;
  }

  public EvsConcept getEvsConceptByLabelProperties(String conceptCode, String dbType,
    List<String> propertyList) throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByLabel evsConcept = new EvsConceptByLabel();
    getConceptProperties(evsConcept, conceptCode, dbType, "byLabel", propertyList);
    return evsConcept;
  }

  public EvsConcept getEvsConceptByCodeProperties(String conceptCode, String dbType,
    List<String> propertyList) throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByCode evsConcept = new EvsConceptByCode();
    getConceptProperties(evsConcept, conceptCode, dbType, "byLabel", propertyList);
    return evsConcept;
  }

  public void getConceptProperties(EvsConcept evsConcept, String conceptCode, String dbType,
    String outputType, List<String> propertyList) throws IOException {
    evsConcept.setCode(conceptCode);

    List<EvsProperty> properties = getEvsProperties(conceptCode, dbType);
    if (propertyList.contains("Label") || propertyList.contains("rdfs:label")) {
      evsConcept.setLabel(getEvsConceptLabel(conceptCode, dbType));
    }
    if (propertyList.contains("Code") || propertyList.contains("NCH0")) {
      evsConcept.setCode(EVSUtils.getConceptCode(properties));
    }
    if (propertyList.contains("Preferred_Name") || propertyList.contains("P108")) {
      evsConcept.setPreferredName(EVSUtils.getPreferredName(properties));
    }

    /*
     * Load Additional Properties
     */
    Map<String, List<String>> allProperties = evsConcept.getProperties();
    List<EvsProperty> additionalProperties = EVSUtils.getAdditionalPropertiesByCode(properties);
    if (outputType.equals("byLabel")) {
      for (EvsProperty property : additionalProperties) {
        if (propertyList.contains(property.getLabel())
            || propertyList.contains(property.getCode())) {
          String label = property.getLabel();
          String value = property.getValue();
          if (allProperties.containsKey(label)) {
            allProperties.get(label).add(value);
          } else {
            allProperties.put(label, new ArrayList<String>());
            allProperties.get(label).add(value);
          }
        }
      }
    } else {
      for (EvsProperty property : additionalProperties) {
        if (propertyList.contains(property.getLabel())
            || propertyList.contains(property.getCode())) {
          String code = property.getCode();
          String value = property.getValue();
          if (allProperties.containsKey(code)) {
            allProperties.get(code).add(value);
          } else {
            allProperties.put(code, new ArrayList<String>());
            allProperties.get(code).add(value);
          }
        }
      }
    }

    List<EvsAxiom> axioms = getEvsAxioms(conceptCode, dbType);
    if (propertyList.contains("FULL_SYN") || propertyList.contains("P90")) {
      evsConcept.setSynonyms(EVSUtils.getSynonyms(axioms, outputType));
    }
    if (propertyList.contains("DEFINITION") || propertyList.contains("P97")) {
      evsConcept.setDefinitions(EVSUtils.getDefinitions(axioms, outputType));
    }
    if (propertyList.contains("ALT_DEFINITION") || propertyList.contains("P325")) {
      evsConcept.setAltDefinitions(EVSUtils.getAltDefinitions(axioms, outputType));
    }
    if (propertyList.contains("Subconcept")) {
      evsConcept.setSubconcepts(getEvsSubconcepts(conceptCode, dbType, outputType));
    }
    if (propertyList.contains("Superconcept")) {
      evsConcept.setSuperconcepts(getEvsSuperconcepts(conceptCode, dbType, outputType));
    }
    if (propertyList.contains("Association")) {
      evsConcept.setAssociations(getEvsAssociations(conceptCode, dbType));
    }
    if (propertyList.contains("InverseAssociation")) {
      evsConcept.setInverseAssociations(getEvsInverseAssociations(conceptCode, dbType));
    }
    if (propertyList.contains("Role")) {
      evsConcept.setRoles(getEvsRoles(conceptCode, dbType));
    }
    if (propertyList.contains("InverseRole")) {
      evsConcept.setInverseRoles(getEvsInverseRoles(conceptCode, dbType));
    }
    if (propertyList.contains("Maps_To") || propertyList.contains("P375")) {
      evsConcept.setMapsTo(EVSUtils.getMapsTo(axioms, outputType));
    }
    if (propertyList.contains("GO_Annotation") || propertyList.contains("P211")) {
      evsConcept.setGoAnnotations(EVSUtils.getGoAnnotations(axioms, outputType));
    }
    if (propertyList.contains("DisjointWith")) {
      evsConcept.setDisjointWith(getEvsDisjointWith(conceptCode, dbType));
    }
  }

  public void getConcept(EvsConcept evsConcept, String conceptCode, String dbType,
    String outputType) throws IOException {
    evsConcept.setCode(conceptCode);

    List<EvsProperty> properties = getEvsProperties(conceptCode, dbType);
    evsConcept.setLabel(getEvsConceptLabel(conceptCode, dbType));
    evsConcept.setCode(EVSUtils.getConceptCode(properties));
    evsConcept.setPreferredName(EVSUtils.getPreferredName(properties));

    /*
     * Load Additional Properties
     */
    Map<String, List<String>> allProperties = evsConcept.getProperties();
    List<EvsProperty> additionalProperties = EVSUtils.getAdditionalPropertiesByCode(properties);
    if (outputType.equals("byLabel")) {
      for (EvsProperty property : additionalProperties) {
        String label = property.getLabel();
        String value = property.getValue();
        if (allProperties.containsKey(label)) {
          allProperties.get(label).add(value);
        } else {
          allProperties.put(label, new ArrayList<String>());
          allProperties.get(label).add(value);
        }
      }
    } else {
      for (EvsProperty property : additionalProperties) {
        String code = property.getCode();
        String value = property.getValue();
        if (allProperties.containsKey(code)) {
          allProperties.get(code).add(value);
        } else {
          allProperties.put(code, new ArrayList<String>());
          allProperties.get(code).add(value);
        }
      }
    }

    List<EvsAxiom> axioms = getEvsAxioms(conceptCode, dbType);
    evsConcept.setSynonyms(EVSUtils.getSynonyms(axioms, outputType));
    evsConcept.setDefinitions(EVSUtils.getDefinitions(axioms, outputType));
    evsConcept.setAltDefinitions(EVSUtils.getAltDefinitions(axioms, outputType));
    evsConcept.setSubconcepts(getEvsSubconcepts(conceptCode, dbType, outputType));
    evsConcept.setSuperconcepts(getEvsSuperconcepts(conceptCode, dbType, outputType));
    log.info("ZZZ2 - currently not reading associations/roles for testing perf");
    // evsConcept.setAssociations(getEvsAssociations(conceptCode, dbType));
    // evsConcept
    // .setInverseAssociations(getEvsInverseAssociations(conceptCode, dbType));
    // evsConcept.setRoles(getEvsRoles(conceptCode, dbType));
    // evsConcept.setInverseRoles(getEvsInverseRoles(conceptCode, dbType));
    evsConcept.setMapsTo(EVSUtils.getMapsTo(axioms, outputType));
    evsConcept.setGoAnnotations(EVSUtils.getGoAnnotations(axioms, outputType));
    evsConcept.setDisjointWith(getEvsDisjointWith(conceptCode, dbType));
  }

  public void getConcept(EvsConcept evsConcept, String conceptCode, String dbType, IncludeParam ip)
    throws IOException {
    evsConcept.setCode(conceptCode);

    List<EvsProperty> properties = getEvsProperties(conceptCode, dbType);

    // minimal, always do these
    evsConcept.setLabel(getEvsConceptLabel(conceptCode, dbType));
    evsConcept.setCode(EVSUtils.getConceptCode(properties));

    // This becomes a synonym
    if (ip.isSynonyms()) {
      evsConcept.setPreferredName(EVSUtils.getPreferredName(properties));
    }

    // Properties ending in "Name" are rendered as synonyms here.
    if (ip.isSynonyms() || ip.isProperties()) {
      Map<String, List<String>> allProperties = evsConcept.getProperties();
      List<EvsProperty> additionalProperties = EVSUtils.getAdditionalPropertiesByCode(properties);
      for (EvsProperty property : additionalProperties) {
        String label = property.getLabel();
        if ((label.endsWith("_Name") && ip.isSynonyms())
            || (!label.endsWith("_Name") && ip.isProperties())) {
          String value = property.getValue();
          if (allProperties.containsKey(label)) {
            allProperties.get(label).add(value);
          } else {
            allProperties.put(label, new ArrayList<String>());
            allProperties.get(label).add(value);
          }
        }
      }
    }
    final String outputType = "byLabel";

    if (ip.hasAnyTrue()) {
      final List<EvsAxiom> axioms = getEvsAxioms(conceptCode, dbType);
      if (ip.isSynonyms()) {
        evsConcept.setSynonyms(EVSUtils.getSynonyms(axioms, outputType));
      }

      if (ip.isDefinitions()) {
        evsConcept.setDefinitions(EVSUtils.getDefinitions(axioms, outputType));
        evsConcept.setAltDefinitions(EVSUtils.getAltDefinitions(axioms, outputType));
      }
      if (ip.isChildren()) {
        evsConcept.setSubconcepts(getEvsSubconcepts(conceptCode, dbType, outputType));
      }

      if (ip.isParents()) {
        evsConcept.setSuperconcepts(getEvsSuperconcepts(conceptCode, dbType, outputType));
      }

      if (ip.isAssociations()) {
        evsConcept.setAssociations(getEvsAssociations(conceptCode, dbType));
      }

      if (ip.isInverseAssociations()) {
        evsConcept.setInverseAssociations(getEvsInverseAssociations(conceptCode, dbType));
      }

      if (ip.isRoles()) {
        evsConcept.setRoles(getEvsRoles(conceptCode, dbType));
      }

      if (ip.isInverseRoles()) {
        evsConcept.setInverseRoles(getEvsInverseRoles(conceptCode, dbType));
      }

      if (ip.isMaps()) {
        evsConcept.setMapsTo(EVSUtils.getMapsTo(axioms, outputType));
      }

      // evsConcept
      // .setGoAnnotations(EVSUtils.getGoAnnotations(axioms, outputType));

      if (ip.isDisjointWith()) {
        evsConcept.setDisjointWith(getEvsDisjointWith(conceptCode, dbType));
      }
    }
  }

  public void getConceptShort(EvsConcept evsConcept, String conceptCode, String dbType,
    String outputType) throws IOException {
    evsConcept.setCode(conceptCode);

    List<EvsProperty> properties = getEvsProperties(conceptCode, dbType);
    evsConcept.setLabel(getEvsConceptLabel(conceptCode, dbType));
    evsConcept.setCode(EVSUtils.getConceptCode(properties));
    evsConcept.setPreferredName(EVSUtils.getPreferredName(properties));

    /*
     * Load Additional Properties
     */
    Map<String, List<String>> allProperties = evsConcept.getProperties();
    List<EvsProperty> additionalProperties = EVSUtils.getAdditionalPropertiesByCode(properties);
    if (outputType.equals("byLabel")) {
      for (EvsProperty property : additionalProperties) {
        String label = property.getLabel();
        String value = property.getValue();
        if (allProperties.containsKey(label)) {
          allProperties.get(label).add(value);
        } else {
          allProperties.put(label, new ArrayList<String>());
          allProperties.get(label).add(value);
        }
      }
    } else {
      for (EvsProperty property : additionalProperties) {
        String code = property.getCode();
        String value = property.getValue();
        if (allProperties.containsKey(code)) {
          allProperties.get(code).add(value);
        } else {
          allProperties.put(code, new ArrayList<String>());
          allProperties.get(code).add(value);
        }
      }
    }

    List<EvsAxiom> axioms = getEvsAxioms(conceptCode, dbType);
    evsConcept.setSynonyms(EVSUtils.getSynonyms(axioms, outputType));
    evsConcept.setDefinitions(EVSUtils.getDefinitions(axioms, outputType));
    evsConcept.setAltDefinitions(EVSUtils.getAltDefinitions(axioms, outputType));
    evsConcept.setMapsTo(EVSUtils.getMapsTo(axioms, outputType));
    evsConcept.setGoAnnotations(EVSUtils.getGoAnnotations(axioms, outputType));
    evsConcept.setDisjointWith(getEvsDisjointWith(conceptCode, dbType));
  }

  public List<EvsMapsTo> getEvsMapsTo(String conceptCode, String dbType) throws IOException {
    List<EvsAxiom> axioms = getEvsAxioms(conceptCode, dbType);
    return EVSUtils.getMapsTo(axioms, "byCode");
  }

  public EvsConcept getEvsPropertyByLabel(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByLabel evsConcept = new EvsConceptByLabel();
    getProperty(evsConcept, conceptCode, dbType, "byLabel");
    return evsConcept;
  }

  public EvsConcept getEvsPropertyByCode(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByCode evsConcept = new EvsConceptByCode();
    getProperty(evsConcept, conceptCode, dbType, "byCode");
    return evsConcept;
  }

  public EvsConcept getEvsProperty(String conceptCode, String dbType, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByLabel evsConcept = new EvsConceptByLabel();
    getProperty(evsConcept, conceptCode, dbType, ip);
    return evsConcept;
  }

  public void getProperty(EvsConcept evsConcept, String conceptCode, String dbType,
    String outputType) throws IOException {
    evsConcept.setCode(conceptCode);

    List<EvsProperty> properties = getEvsPropertiesNoRestrictions(conceptCode, dbType);
    evsConcept.setLabel(getEvsConceptLabel(conceptCode, dbType));
    evsConcept.setCode(EVSUtils.getConceptCode(properties));
    evsConcept.setPreferredName(EVSUtils.getPreferredName(properties));

    /*
     * Load Additional Properties
     */
    Map<String, List<String>> allProperties = evsConcept.getProperties();
    List<EvsProperty> additionalProperties = EVSUtils.getAdditionalPropertiesByCode(properties);
    if (outputType.equals("byLabel")) {
      for (EvsProperty property : additionalProperties) {
        String label = property.getLabel();
        String value = property.getValue();
        if (allProperties.containsKey(label)) {
          allProperties.get(label).add(value);
        } else {
          allProperties.put(label, new ArrayList<String>());
          allProperties.get(label).add(value);
        }
      }
    } else {
      for (EvsProperty property : additionalProperties) {
        String code = property.getCode();
        String value = property.getValue();
        if (allProperties.containsKey(code)) {
          allProperties.get(code).add(value);
        } else {
          allProperties.put(code, new ArrayList<String>());
          allProperties.get(code).add(value);
        }
      }
    }

    List<EvsAxiom> axioms = getEvsAxioms(conceptCode, dbType);
    evsConcept.setSynonyms(EVSUtils.getSynonyms(axioms, outputType));
    evsConcept.setDefinitions(EVSUtils.getDefinitions(axioms, outputType));
    evsConcept.setAltDefinitions(EVSUtils.getAltDefinitions(axioms, outputType));
    evsConcept.setSubconcepts(getEvsSubconcepts(conceptCode, dbType, outputType));
    evsConcept.setSuperconcepts(getEvsSuperconcepts(conceptCode, dbType, outputType));
    log.info("ZZZ - currently not reading associations/roles for testing perf");
    // evsConcept.setAssociations(getEvsAssociations(conceptCode, dbType));
    // evsConcept
    // .setInverseAssociations(getEvsInverseAssociations(conceptCode, dbType));
    // evsConcept.setRoles(getEvsRoles(conceptCode, dbType));
    // evsConcept.setInverseRoles(getEvsInverseRoles(conceptCode, dbType));
    evsConcept.setMapsTo(EVSUtils.getMapsTo(axioms, outputType));
    evsConcept.setGoAnnotations(EVSUtils.getGoAnnotations(axioms, outputType));
    evsConcept.setDisjointWith(getEvsDisjointWith(conceptCode, dbType));
  }

  public void getProperty(EvsConcept evsConcept, String conceptCode, String dbType, IncludeParam ip)
    throws IOException {
    evsConcept.setCode(conceptCode);

    List<EvsProperty> properties = getEvsPropertiesNoRestrictions(conceptCode, dbType);

    // minimal, always do these
    evsConcept.setCode(EVSUtils.getConceptCode(properties));

    final String pn = EVSUtils.getPreferredName(properties);
    final String conceptLabel = getEvsConceptLabel(conceptCode, dbType);
    // log.info("conceptLabel - " + conceptLabel);
    // If the label has a space and the preferred name doesn't,
    // and they don't match, use the preferred name
    // This is to handle the NCIt properties like "term-group" and "def-source"
    // that otherwise wind up with concept names having a space - awkward for
    // the callback
    // This condition ONLY applies to properties in NCIt, but should be
    // double-checked for other terminologies
    if (conceptLabel != null && !conceptLabel.equals(pn) && conceptLabel.contains(" ")) {
      evsConcept.setLabel(pn);
    } else {
      evsConcept.setLabel(conceptLabel);
    }
    // Set the preferred name if including synonyms
    if (ip.isSynonyms()) {
      evsConcept.setPreferredName(pn);
    }

    // Properties ending in "Name" are rendered as synonyms here.
    if (ip.isSynonyms() || ip.isProperties()) {

      Map<String, List<String>> allProperties = evsConcept.getProperties();
      List<EvsProperty> additionalProperties = EVSUtils.getAdditionalPropertiesByCode(properties);

      for (EvsProperty property : additionalProperties) {
        String label = property.getLabel();
        if ((label.endsWith("_Name") && ip.isSynonyms())
            || (!label.endsWith("_Name") && ip.isProperties())) {
          String value = property.getValue();
          if (allProperties.containsKey(label)) {
            allProperties.get(label).add(value);
          } else {
            allProperties.put(label, new ArrayList<String>());
            allProperties.get(label).add(value);
          }
        }
      }

    }
    final String outputType = "byLabel";

    if (ip.hasAnyTrue()) {
      List<EvsAxiom> axioms = getEvsAxioms(conceptCode, dbType);

      if (ip.isSynonyms()) {
        evsConcept.setSynonyms(EVSUtils.getSynonyms(axioms, outputType));
        // If we're using preferred name instead of the label above,
        // then we need to add an "rdfs:label" synonym here.
        if (conceptLabel != null && !conceptLabel.equals(pn) && conceptLabel.contains(" ")) {
          final EvsSynonym rdfsLabel = new EvsSynonymByLabel();
          rdfsLabel.setTermGroup("rdfs:label");
          rdfsLabel.setLabel(conceptLabel);
          evsConcept.getSynonyms().add(rdfsLabel);
        }
      }

      if (ip.isDefinitions()) {
        evsConcept.setDefinitions(EVSUtils.getDefinitions(axioms, outputType));
        evsConcept.setAltDefinitions(EVSUtils.getAltDefinitions(axioms, outputType));
      }

      if (ip.isChildren()) {
        evsConcept.setSubconcepts(getEvsSubconcepts(conceptCode, dbType, outputType));
      }

      if (ip.isParents()) {
        evsConcept.setSuperconcepts(getEvsSuperconcepts(conceptCode, dbType, outputType));
      }

      if (ip.isAssociations()) {
        evsConcept.setAssociations(getEvsAssociations(conceptCode, dbType));
      }

      if (ip.isInverseAssociations()) {
        evsConcept.setInverseAssociations(getEvsInverseAssociations(conceptCode, dbType));
      }

      if (ip.isRoles()) {
        evsConcept.setRoles(getEvsRoles(conceptCode, dbType));
      }

      if (ip.isInverseRoles()) {
        evsConcept.setInverseRoles(getEvsInverseRoles(conceptCode, dbType));
      }

      if (ip.isMaps()) {
        evsConcept.setMapsTo(EVSUtils.getMapsTo(axioms, outputType));
      }
      // evsConcept
      // .setGoAnnotations(EVSUtils.getGoAnnotations(axioms, outputType));
      if (ip.isDisjointWith()) {
        evsConcept.setDisjointWith(getEvsDisjointWith(conceptCode, dbType));
      }
    }
  }

  public List<EvsProperty> getEvsProperties(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructPropertyQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsProperty> evsProperties = new ArrayList<EvsProperty>();

    /*
     * Because the original SPARQL query that filtered out the Annotations was
     * too slow, we will be filtering them out in the post processing.
     */
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      if (b.getPropertyCode() == null) {
        EvsProperty evsProperty = new EvsProperty();
        evsProperty.setCode("");
        evsProperty.setLabel(b.getPropertyLabel().getValue());
        evsProperty.setValue(b.getPropertyValue().getValue());
        evsProperties.add(evsProperty);
      } else {
        if (!b.getPropertyCode().getValue().startsWith("A")) {
          EvsProperty evsProperty = new EvsProperty();
          evsProperty.setCode(b.getPropertyCode().getValue());
          evsProperty.setLabel(b.getPropertyLabel().getValue());
          evsProperty.setValue(b.getPropertyValue().getValue());
          evsProperties.add(evsProperty);
        }
      }
    }

    return evsProperties;
  }

  public List<EvsProperty> getEvsPropertiesNoRestrictions(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {

    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query =
        queryBuilderService.constructPropertyNoRestrictionsQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsProperty> evsProperties = new ArrayList<EvsProperty>();

    /*
     * Because the original SPARQL query that filtered out the Annotations was
     * too slow, we will be filtering them out in the post processing.
     */
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      EvsProperty evsProperty = new EvsProperty();
      if (b.getPropertyCode() == null) {
        evsProperty.setCode(b.getProperty().getValue());
      } else {
        evsProperty.setCode(b.getPropertyCode().getValue());
      }
      if (b.getPropertyLabel() == null) {
        evsProperty.setLabel(b.getProperty().getValue());
      } else {
        evsProperty.setLabel(b.getPropertyLabel().getValue());
      }
      evsProperty.setValue(b.getPropertyValue().getValue());
      evsProperties.add(evsProperty);

    }

    return evsProperties;
  }

  @Override
  public List<EvsRelatedConcept> getEvsSubconcepts(String conceptCode, String dbType,
    String outputType) throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructSubconceptQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsRelatedConcept> evsSubclasses = new ArrayList<EvsRelatedConcept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      EvsRelatedConcept evsSubclass;
      if (outputType.equals("byLabel")) {
        evsSubclass = new EvsRelatedConceptByLabel();
      } else {
        evsSubclass = new EvsRelatedConceptByCode();
      }
      evsSubclass.setRelatedConcept(b.getSubclass().getValue());
      evsSubclass.setLabel(b.getSubclassLabel().getValue());
      evsSubclass.setCode(b.getSubclassCode().getValue());
      evsSubclasses.add(evsSubclass);
    }

    return evsSubclasses;
  }

  @Override
  public List<EvsRelatedConcept> getEvsSuperconcepts(String conceptCode, String dbType,
    String outputType) throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructSuperconceptQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsRelatedConcept> evsSuperclasses = new ArrayList<EvsRelatedConcept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      EvsRelatedConcept evsSuperclass;
      if (outputType.equals("byLabel")) {
        evsSuperclass = new EvsRelatedConceptByLabel();
      } else {
        evsSuperclass = new EvsRelatedConceptByCode();
      }
      evsSuperclass.setRelatedConcept(b.getSuperclass().getValue());
      evsSuperclass.setLabel(b.getSuperclassLabel().getValue());
      evsSuperclass.setCode(b.getSuperclassCode().getValue());
      evsSuperclasses.add(evsSuperclass);
    }

    return evsSuperclasses;
  }

  @Override
  public List<EvsAssociation> getEvsAssociations(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAssociationsQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsAssociation> evsAssociations = new ArrayList<EvsAssociation>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      EvsAssociation evsAssociation = new EvsAssociation();
      evsAssociation.setRelationship(b.getRelationship().getValue());
      evsAssociation.setRelationshipCode(b.getRelationshipCode().getValue());
      evsAssociation.setRelatedConceptCode(b.getRelatedConceptCode().getValue());
      evsAssociation.setRelatedConceptLabel(b.getRelatedConceptLabel().getValue());
      evsAssociations.add(evsAssociation);
    }

    return evsAssociations;
  }

  public List<EvsAssociation> getEvsInverseAssociations(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructInverseAssociationsQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsAssociation> evsAssociations = new ArrayList<EvsAssociation>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      EvsAssociation evsAssociation = new EvsAssociation();
      evsAssociation.setRelationship(b.getRelationship().getValue());
      evsAssociation.setRelationshipCode(b.getRelationshipCode().getValue());
      evsAssociation.setRelatedConceptCode(b.getRelatedConceptCode().getValue());
      evsAssociation.setRelatedConceptLabel(b.getRelatedConceptLabel().getValue());
      evsAssociations.add(evsAssociation);
    }

    return evsAssociations;
  }

  public List<EvsAssociation> getEvsInverseRoles(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructInverseRolesQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsAssociation> evsAssociations = new ArrayList<EvsAssociation>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      EvsAssociation evsAssociation = new EvsAssociation();
      evsAssociation.setRelationship(b.getRelationship().getValue());
      evsAssociation.setRelationshipCode(b.getRelationshipCode().getValue());
      evsAssociation.setRelatedConceptCode(b.getRelatedConceptCode().getValue());
      evsAssociation.setRelatedConceptLabel(b.getRelatedConceptLabel().getValue());
      evsAssociations.add(evsAssociation);
    }

    return evsAssociations;
  }

  public List<EvsAssociation> getEvsRoles(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructRolesQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsAssociation> evsAssociations = new ArrayList<EvsAssociation>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      EvsAssociation evsAssociation = new EvsAssociation();
      evsAssociation.setRelationship(b.getRelationship().getValue());
      evsAssociation.setRelationshipCode(b.getRelationshipCode().getValue());
      evsAssociation.setRelatedConceptCode(b.getRelatedConceptCode().getValue());
      evsAssociation.setRelatedConceptLabel(b.getRelatedConceptLabel().getValue());
      evsAssociations.add(evsAssociation);
    }

    return evsAssociations;
  }

  public EvsRelationships getEvsRelationships(String conceptCode, String dbType, String outputType)
    throws JsonMappingException, JsonParseException, IOException {
    EvsRelationships relationships = new EvsRelationships();
    relationships.setSubconcepts(getEvsSubconcepts(conceptCode, dbType, outputType));
    relationships.setSuperconcepts(getEvsSuperconcepts(conceptCode, dbType, outputType));
    relationships.setAssociations(getEvsAssociations(conceptCode, dbType));
    relationships.setInverseAssociations(getEvsInverseAssociations(conceptCode, dbType));
    relationships.setRoles(getEvsRoles(conceptCode, dbType));
    relationships.setInverseRoles(getEvsInverseRoles(conceptCode, dbType));
    relationships.setDisjointWith(getEvsDisjointWith(conceptCode, dbType));

    return relationships;
  }

  public List<EvsAssociation> getEvsDisjointWith(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructDisjointWithQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsAssociation> evsAssociations = new ArrayList<EvsAssociation>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      EvsAssociation evsAssociation = new EvsAssociation();
      evsAssociation.setRelationship(b.getRelationship().getValue());
      evsAssociation.setRelatedConceptCode(b.getRelatedConceptCode().getValue());
      evsAssociation.setRelatedConceptLabel(b.getRelatedConceptLabel().getValue());
      evsAssociations.add(evsAssociation);
    }

    return evsAssociations;
  }

  public List<EvsAxiom> getEvsAxioms(String conceptCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAxiomQuery(conceptCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsAxiom> evsAxioms = new ArrayList<EvsAxiom>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    if (bindings.length == 0) {
      return evsAxioms;
    }
    EvsAxiom evsAxiom = new EvsAxiom();
    Boolean sw = false;
    String oldAxiom = "";
    for (Bindings b : bindings) {
      String axiom = b.getAxiom().getValue();
      String property = b.getAxiomProperty().getValue().split("#")[1];
      String value = b.getAxiomValue().getValue();
      if (value.contains("#")) {
        value = value.split("#")[1];
      }

      if (sw && !axiom.equals(oldAxiom)) {
        evsAxioms.add(evsAxiom);
        evsAxiom = new EvsAxiom();
      }
      sw = true;
      oldAxiom = axiom;

      switch (property) {
        case "annotatedSource":
          evsAxiom.setAnnotatedSource(value);
          break;
        case "annotatedTarget":
          evsAxiom.setAnnotatedTarget(value);
          break;
        case "annotatedProperty":
          evsAxiom.setAnnotatedProperty(value);
          break;
        case "type":
          evsAxiom.setType(value);
          break;
        case "P380":
          evsAxiom.setDefinitionReviewDate(value);
          break;
        case "P379":
          evsAxiom.setDefinitionReviewerName(value);
          break;
        case "P393":
          evsAxiom.setRelationshipToTarget(value);
          break;
        case "P395":
          evsAxiom.setTargetCode(value);
          break;
        case "P394":
          evsAxiom.setTargetTermType(value);
          break;
        case "P396":
          evsAxiom.setTargetTerminology(value);
          break;
        case "P381":
          evsAxiom.setAttr(value);
          break;
        case "P378":
          evsAxiom.setDefSource(value);
          break;
        case "P389":
          evsAxiom.setGoEvi(value);
          break;
        case "P387":
          evsAxiom.setGoId(value);
          break;
        case "P390":
          evsAxiom.setGoSource(value);
          break;
        case "P385":
          evsAxiom.setSourceCode(value);
          break;
        case "P391":
          evsAxiom.setSourceDate(value);
          break;
        case "P386":
          evsAxiom.setSubsourceName(value);
          break;
        case "P383":
          evsAxiom.setTermGroup(value);
          break;
        case "P384":
          evsAxiom.setTermSource(value);
          break;
        case "term-source":
          evsAxiom.setTermSource(value);
          break;
        case "term-group":
          evsAxiom.setTermGroup(value);
          break;
        default:
          break;

      }

    }
    evsAxioms.add(evsAxiom);
    return evsAxioms;
  }

  public ArrayList<String> getHierarchy(String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    ArrayList<String> parentchild = new ArrayList<String>();
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructHierarchyQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      StringBuffer str = new StringBuffer();
      str.append(b.getParentCode().getValue());
      str.append("\t");
      str.append(b.getParentLabel().getValue());
      str.append("\t");
      str.append(b.getChildCode().getValue());
      str.append("\t");
      str.append(b.getChildLabel().getValue());
      str.append("\n");
      parentchild.add(str.toString());
    }

    return parentchild;
  }

  /*
   * Documentation Section
   */

  public List<String> getAllPropertiesForDocumentation(String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllPropertiesQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> evsProperties = new ArrayList<String>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      if (!propertyNotConsidered.containsKey(b.getPropertyCode().getValue())) {
        String property = b.getPropertyCode().getValue() + " : " + b.getPropertyLabel().getValue();
        evsProperties.add(property);
      }
    }

    evsProperties.add("Superconcept : Superconcept");
    evsProperties.add("Subconcept : Subconcept");
    evsProperties.add("Association : Association");
    evsProperties.add("InverseAssociation : InverseAssociation");
    evsProperties.add("Role : Role");
    evsProperties.add("InverseRole : InverseRole");

    return evsProperties;
  }

  public List<EvsConcept> getAllProperties(String dbType, String fmt)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllPropertiesQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> evsProperties = new ArrayList<String>();
    ArrayList<EvsConcept> evsConcepts = new ArrayList<EvsConcept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      evsProperties.add(b.getPropertyCode().getValue());
    }

    for (String code : evsProperties) {
      EvsConcept concept = null;
      if (fmt.equals("byLabel")) {
        concept = getEvsPropertyByLabel(code, dbType);
      } else {
        concept = getEvsPropertyByCode(code, dbType);
      }
      evsConcepts.add(concept);

    }

    return evsConcepts;
  }

  public List<EvsConcept> getAllProperties(String dbType, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllPropertiesQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> evsProperties = new ArrayList<String>();
    ArrayList<EvsConcept> evsConcepts = new ArrayList<EvsConcept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      evsProperties.add(b.getPropertyCode().getValue());
    }

    for (String code : evsProperties) {
      EvsConcept concept = null;
      concept = getEvsProperty(code, dbType, ip);
      evsConcepts.add(concept);
    }

    return evsConcepts;
  }

  public List<EvsProperty> getAllPropertiesList(String dbType, String fmt)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllPropertiesQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<EvsProperty> evsProperties = new ArrayList<EvsProperty>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      if (b.getPropertyCode() == null) {
        EvsProperty evsProperty = new EvsProperty();
        evsProperty.setCode("");
        evsProperty.setLabel(b.getPropertyLabel().getValue());
        evsProperty.setValue(b.getPropertyValue().getValue());
        evsProperties.add(evsProperty);
      } else {
        if (!b.getPropertyCode().getValue().startsWith("A")) {
          EvsProperty evsProperty = new EvsProperty();
          evsProperty.setCode(b.getPropertyCode().getValue());
          evsProperty.setLabel(b.getPropertyLabel().getValue());
          evsProperty.setValue(b.getPropertyValue().getValue());
          evsProperties.add(evsProperty);
        }
      }
    }

    return evsProperties;

  }

  public List<String> getAxiomQualifiersList(String propertyCode, String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAxiomQualifierQuery(propertyCode, namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> propertyValues = new ArrayList<String>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String propertyValue = b.getPropertyValue().getValue();
      propertyValues.add(propertyValue);
    }

    return propertyValues;
  }

  public List<String> getAllAssociationsForDocumentation(String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllAssociationsQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> evsProperties = new ArrayList<String>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String property = b.getPropertyCode().getValue() + " : " + b.getPropertyLabel().getValue();
      evsProperties.add(property);
    }

    return evsProperties;
  }

  public List<EvsConcept> getAllAssociations(String dbType, String fmt)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllAssociationsQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> evsProperties = new ArrayList<String>();
    ArrayList<EvsConcept> evsConcepts = new ArrayList<EvsConcept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      evsProperties.add(b.getPropertyCode().getValue());
    }

    for (String code : evsProperties) {
      EvsConcept concept = null;
      if (fmt.equals("byLabel")) {
        concept = getEvsPropertyByLabel(code, dbType);
      } else {
        concept = getEvsPropertyByCode(code, dbType);
      }
      evsConcepts.add(concept);

    }

    return evsConcepts;
  }

  public List<EvsConcept> getAllAssociations(String dbType, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllAssociationsQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> evsProperties = new ArrayList<String>();
    ArrayList<EvsConcept> evsConcepts = new ArrayList<EvsConcept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      evsProperties.add(b.getPropertyCode().getValue());
    }

    for (String code : evsProperties) {
      EvsConcept concept = null;
      concept = getEvsProperty(code, dbType, ip);
      evsConcepts.add(concept);

    }

    return evsConcepts;
  }

  public List<String> getAllRolesForDocumentation(String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllRolesQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> evsProperties = new ArrayList<String>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String property = b.getPropertyCode().getValue() + " : " + b.getPropertyLabel().getValue();
      evsProperties.add(property);
    }

    return evsProperties;
  }

  public List<EvsConcept> getAllRoles(String dbType, String fmt)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllRolesQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> evsProperties = new ArrayList<String>();
    ArrayList<EvsConcept> evsConcepts = new ArrayList<EvsConcept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      evsProperties.add(b.getPropertyCode().getValue());
    }

    for (String code : evsProperties) {
      EvsConcept concept = null;
      if (fmt.equals("byLabel")) {
        concept = getEvsPropertyByLabel(code, dbType);
      } else {
        concept = getEvsPropertyByCode(code, dbType);
      }
      evsConcepts.add(concept);

    }

    return evsConcepts;
  }

  public List<EvsConcept> getAllRoles(String dbType, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructAllRolesQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> evsProperties = new ArrayList<String>();
    ArrayList<EvsConcept> evsConcepts = new ArrayList<EvsConcept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      evsProperties.add(b.getPropertyCode().getValue());
    }

    for (String code : evsProperties) {
      EvsConcept concept = null;
      concept = getEvsProperty(code, dbType, ip);
      evsConcepts.add(concept);
    }

    return evsConcepts;
  }

  /*
   * This section supports the Hierarchy Browser
   */

  public List<HierarchyNode> getRootNodes(String dbType) {
    if (dbType.equals("monthly")) {
      return hierarchyMonthly.getRootNodes();
    } else {
      return hierarchyWeekly.getRootNodes();
    }
  }

  public List<HierarchyNode> getChildNodes(String parent, String dbType) {
    if (dbType.equals("monthly")) {
      return hierarchyMonthly.getChildNodes(parent, 0);
    } else {
      return hierarchyWeekly.getChildNodes(parent, 0);
    }
  }

  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, String dbType) {
    if (dbType.equals("monthly")) {
      return hierarchyMonthly.getChildNodes(parent, maxLevel);
    } else {
      return hierarchyWeekly.getChildNodes(parent, maxLevel);
    }
  }

  public List<String> getAllChildNodes(String parent, String dbType) {
    if (dbType.equals("monthly")) {
      return hierarchyMonthly.getAllChildNodes(parent);
    } else {
      return hierarchyWeekly.getAllChildNodes(parent);
    }
  }

  public void checkPathInHierarchy(String code, HierarchyNode node, Path path, String dbType) {
    if (path.getConcepts().size() == 0) {
      return;
    }
    int end = path.getConcepts().size() - 1;
    ConceptNode concept = path.getConcepts().get(end);
    List<HierarchyNode> children = null;
    if (dbType.equals("monthly")) {
      children = hierarchyMonthly.getChildNodes(node.getCode(), 1);
    } else {
      children = hierarchyWeekly.getChildNodes(node.getCode(), 1);
    }
    if (node.getChildren().size() == 0) {
      node.setChildren(children);
    }
    if (concept.getCode().equals(node.getCode())) {
      if (node.getCode().equals(code)) {
        node.setHighlight(true);
        return;
      }
      node.setExpanded(true);
      if (path.getConcepts() != null && !path.getConcepts().isEmpty()) {
        path.getConcepts().remove(path.getConcepts().size() - 1);
      }
      for (HierarchyNode childNode : node.getChildren()) {
        checkPathInHierarchy(code, childNode, path, dbType);
      }
    }
  }

  public List<HierarchyNode> getPathInHierarchy(String code, String dbType) {
    List<HierarchyNode> rootNodes = null;
    if (dbType.equals("monthly")) {
      rootNodes = hierarchyMonthly.getRootNodes();
    } else {
      rootNodes = hierarchyWeekly.getRootNodes();
    }
    Paths paths = getPathToRoot(code, dbType);

    for (HierarchyNode rootNode : rootNodes) {
      for (Path path : paths.getPaths()) {
        checkPathInHierarchy(code, rootNode, path, dbType);
      }
    }

    return rootNodes;
  }

  public Paths getPathToRoot(String code, String dbType) {
    List<Path> paths = null;
    if (dbType.equals("monthly")) {
      paths = pathsMonthly.getPaths();
    } else {
      paths = pathsWeekly.getPaths();
    }
    Paths conceptPaths = new Paths();
    for (Path path : paths) {
      Boolean sw = false;
      int idx = -1;
      List<ConceptNode> concepts = path.getConcepts();
      for (int i = 0; i < concepts.size(); i++) {
        ConceptNode concept = concepts.get(i);
        if (concept.getCode().equals(code)) {
          sw = true;
          idx = concept.getIdx();
        }
      }
      if (sw) {
        List<ConceptNode> trimed_concepts = new ArrayList<ConceptNode>();
        if (idx == -1) {
          idx = concepts.size() - 1;
        }
        int j = 0;
        for (int i = idx; i >= 0; i--) {
          ConceptNode c = new ConceptNode();
          c.setCode(concepts.get(i).getCode());
          c.setLabel(concepts.get(i).getLabel());
          c.setIdx(j);
          j++;
          trimed_concepts.add(c);
        }
        conceptPaths.add(new Path(1, trimed_concepts));
      }
    }
    conceptPaths = removeDuplicatePaths(conceptPaths);
    return conceptPaths;
  }

  public Paths getPathToParent(String code, String parentCode, String dbType) {
    List<Path> paths = null;
    if (dbType.equals("monthly")) {
      paths = pathsMonthly.getPaths();
    } else {
      paths = pathsWeekly.getPaths();
    }
    Paths conceptPaths = new Paths();
    for (Path path : paths) {
      Boolean codeSW = false;
      Boolean parentSW = false;
      int idx = -1;
      List<ConceptNode> concepts = path.getConcepts();
      for (int i = 0; i < concepts.size(); i++) {
        ConceptNode concept = concepts.get(i);
        if (concept.getCode().equals(code)) {
          codeSW = true;
          idx = concept.getIdx();
        }
        if (concept.getCode().equals(parentCode)) {
          parentSW = true;
        }
      }
      if (codeSW && parentSW) {
        List<ConceptNode> trimed_concepts = new ArrayList<ConceptNode>();
        if (idx == -1) {
          idx = concepts.size() - 1;
        }
        int j = 0;
        for (int i = idx; i >= 0; i--) {
          ConceptNode c = new ConceptNode();
          c.setCode(concepts.get(i).getCode());
          c.setLabel(concepts.get(i).getLabel());
          c.setIdx(j);
          c.setIdx(j);
          j++;
          trimed_concepts.add(c);
          if (c.getCode().equals(parentCode)) {
            break;
          }
        }
        conceptPaths.add(new Path(1, trimed_concepts));
      }
    }
    conceptPaths = removeDuplicatePaths(conceptPaths);
    return conceptPaths;
  }

  private Paths removeDuplicatePaths(Paths paths) {
    Paths uniquePaths = new Paths();
    HashSet<String> seenPaths = new HashSet<String>();
    for (Path path : paths.getPaths()) {
      StringBuffer strPath = new StringBuffer();
      for (ConceptNode concept : path.getConcepts()) {
        strPath.append(concept.getCode());
        strPath.append("|");
      }
      String pathString = strPath.toString();
      if (!seenPaths.contains(pathString)) {
        seenPaths.add(pathString);
        uniquePaths.add(path);
      }
    }

    return uniquePaths;
  }

  private List<Paths> removeDuplicatePathsList(List<Paths> paths) {
    List<Paths> uniquePaths = new ArrayList<Paths>();
    for (Paths newPaths : paths) {
      Paths newUniquePaths = new Paths();
      HashSet<String> seenPaths = new HashSet<String>();
      for (Path path : newPaths.getPaths()) {
        StringBuffer strPath = new StringBuffer();
        for (ConceptNode concept : path.getConcepts()) {
          strPath.append(concept.getCode());
          strPath.append("|");
        }
        String pathString = strPath.toString();
        if (!seenPaths.contains(pathString)) {
          seenPaths.add(pathString);
          newUniquePaths.add(path);
        }
      }
      uniquePaths.add(newUniquePaths);
    }

    return uniquePaths;
  }

  public List<String> getContributingSourcesForDocumentation()
    throws JsonMappingException, JsonParseException, IOException {
    Map<String, String> contributingSources = thesaurusProperties.getContributingSources();
    ArrayList<String> sources = new ArrayList<String>();

    for (String name : contributingSources.keySet()) {
      // search for value
      String value = contributingSources.get(name);
      // String conSource = name + " : " + value;
      // sources.add(conSource);
      sources.add(value);
    }

    return sources;

  }

  public List<String> getConceptStatusForDocumentation()
    throws JsonMappingException, JsonParseException, IOException {
    Map<String, String> conceptStatuses = thesaurusProperties.getConceptStatuses();
    ArrayList<String> statues = new ArrayList<String>();

    for (String name : conceptStatuses.keySet()) {
      // search for value
      String value = conceptStatuses.get(name);
      // String conSource = name + " : " + value;
      // sources.add(conSource);
      statues.add(value);
    }

    return statues;

  }

  public ConfigData getConfigurationData(String dbType)
    throws JsonMappingException, JsonProcessingException, IOException {

    ConfigData configData = new ConfigData();
    configData.setEvsVersionInfo(this.getEvsVersionInfo("monthly"));

    /*
     * //List<EvsProperty> properties = this.getAllPropertiesList("monthly",
     * "byLabel"); //configData.setProperties(properties);
     * 
     * List<String> termSources = getAxiomQualifiersList("P384", "monthly");
     * List<String> subsourceSources = getAxiomQualifiersList("P386",
     * "monthly"); String[] sourceToBeRemoved =
     * thesaurusProperties.getSourcesToBeRemoved(); List<String>
     * sourceToBeRemovedList = Arrays.asList( sourceToBeRemoved );
     * subsourceSources.removeAll(sourceToBeRemovedList);
     * termSources.removeAll(sourceToBeRemovedList); List<String> uniqueSources
     * = Stream.concat(termSources.stream(),subsourceSources.stream()) .map(x ->
     * x) .distinct() .collect(Collectors.toList());
     * Collections.sort(uniqueSources);
     * configData.setFullSynSources(uniqueSources);
     */

    String[] sourceToBeRemoved = thesaurusProperties.getSourcesToBeRemoved();
    List<String> sourceToBeRemovedList = Arrays.asList(sourceToBeRemoved);
    List<String> sources = uniqueSourcesMonthly;
    sources.removeAll(sourceToBeRemovedList);
    configData.setFullSynSources(sources);

    return configData;
  }

  public List<String> getUniqueSourcesList(String dbType)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix();
    String namedGraph = getNamedGraph(dbType);
    String queryURL = getQueryURL(dbType);
    String query = queryBuilderService.constructUniqueSourcesQuery(namedGraph);
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> sources = new ArrayList<String>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String source = b.getPropertyValue().getValue();
      sources.add(source);
    }

    return sources;
  }

}
