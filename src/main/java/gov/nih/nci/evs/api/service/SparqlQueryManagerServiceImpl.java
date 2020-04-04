
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
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
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * Reference implementation of {@link SparqlQueryManagerService}. Includes
 * hibernate tags for MEME database.
 */
@Service
public class SparqlQueryManagerServiceImpl implements SparqlQueryManagerService {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(SparqlQueryManagerServiceImpl.class);

  /** The stardog properties. */
  @Autowired
  StardogProperties stardogProperties;

  /** The query builder service. */
  @Autowired
  QueryBuilderService queryBuilderService;

  /** The thesaurus properties. */
  @Autowired
  private ThesaurusProperties thesaurusProperties;

  /** The application properties. */
  @Autowired
  ApplicationProperties applicationProperties;

  /** The elastic search service. */
  @Autowired
  ElasticSearchService elasticSearchService;

  /** The rest utils. */
  private RESTUtils restUtils = null;
  
  @Resource
  private SparqlQueryManagerService self;
  
  /**
   * Post init.
   *
   * @throws Exception the exception
   */
  @PostConstruct
  public void postInit() throws Exception {
    restUtils = new RESTUtils(stardogProperties.getUsername(), stardogProperties.getPassword(),
        stardogProperties.getReadTimeout(), stardogProperties.getConnectTimeout());

    log.info("  configure elasticsearch");
    elasticSearchService.initSettings();

    // This file generation was for the "documentation files"
    // this is no longer part of the API, no need to do this
    // if (applicationProperties.getForceFileGeneration()) {
    // genDocumentationFiles();
    // }
  }

  /**
   * Call cron job.
   *
   * @param terminologies the terminologies
   * @throws Exception 
   */
  // @Scheduled(cron = "${nci.evs.stardog.populateCacheCron}")
  // public void callCronJob() throws Exception {
  // LocalDateTime currentTime = LocalDateTime.now();
  // log.info("callCronJob at " + currentTime);
  //
  // List<Terminology> terminologies = TerminologyUtils.getTerminologies(this);
  //
  // log.info("ForcePopulateCache " +
  // stardogProperties.getForcePopulateCache());
  // boolean forcePopulateCache =
  // stardogProperties.getForcePopulateCache().equalsIgnoreCase("Y");
  //
  // for(Terminology terminology: terminologies) {
  // Long classCountNow = getGetClassCounts(terminology);
  //
  // if (classCountNow.longValue() !=
  // classCountMap.get(terminology.getTerminologyVersion())
  // || forcePopulateCache) {
  // log.info("****Repopulating cache***");
  // populateCache(terminology);
  // // genDocumentationFiles();
  // log.info("****Repopulating cache done***");
  // }
  // }
  // }

  /**
   * Returns the value set hierarchy.
   *
   * @param terminology the terminology
   * @return the value set hierarchy
   */
  // @SuppressWarnings("unused")
  // private List<HierarchyNode> getValueSetHierarchy(EvsConcept parent,
  // List<String> conceptProperties) throws IOException {
  // List<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
  // for (EvsRelatedConcept child : parent.getSubconcepts()) {
  // EvsConcept childConcept =
  // getEvsConceptByLabelProperties(child.getCode(), "monthly",
  // conceptProperties);
  // HierarchyNode node = new HierarchyNode();
  // node.setCode(childConcept.getCode());
  // node.setLabel(childConcept.getLabel());
  // if (childConcept.getProperties().containsKey("Publish_Value_Set")) {
  // if
  // (childConcept.getProperties().get("Publish_Value_Set").get(0).equals("Yes"))
  // {
  // if (childConcept.getSubconcepts() != null) {
  // List<HierarchyNode> children = getValueSetHierarchy(childConcept,
  // conceptProperties);
  // if (children.size() > 0) {
  // node.setLeaf(false);
  // node.setChildren(children);
  // } else {
  // node.setLeaf(true);
  // }
  // } else {
  // node.setLeaf(true);
  // }
  // nodes.add(node);
  // }
  // }
  // }
  // return nodes;
  // }

  /**
   * Returns the named graph.
   *
   * @param terminology the terminology
   * @return the named graph
   */
  @Override
  public String getNamedGraph(Terminology terminology) {
    return terminology.getGraph();
  }

  /**
   * Returns the query URL.
   *
   * @return the query URL
   */
  @Override
  public String getQueryURL() {
    return stardogProperties.getQueryUrl();
  }

  /**
   * Returns the all graph names.
   *
   * @return the all graph names
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology", key = "#root.methodName")
  public List<String> getAllGraphNames()
    throws JsonParseException, JsonMappingException, IOException {
    List<String> graphNames = new ArrayList<String>();
    String queryPrefix = queryBuilderService.contructPrefix(null);
    String query = queryBuilderService.constructAllGraphNamesQuery();
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    log.debug("getAllGraphNames response - " + res);
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
   * Returns EvsVersion Information objects for all graphs loaded in db.
   *
   * @return the list of {@link EvsVersionInfo} objects
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology", key = "#root.methodName")
  public List<EvsVersionInfo> getEvsVersionInfoList()
    throws JsonParseException, JsonMappingException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(null);
    String query = queryBuilderService.constructAllGraphsAndVersionsQuery();
    String queryURL = getQueryURL();
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    if (log.isDebugEnabled())
      log.debug("getEvsVersionInfoList response - " + res);
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<EvsVersionInfo> evsVersionInfoList = new ArrayList<>();
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();

    for (Bindings b : bindings) {
      String graphName = b.getGraphName().getValue();
      if (graphName == null || graphName.equalsIgnoreCase(""))
        continue;
      EvsVersionInfo evsVersionInfo = new EvsVersionInfo();
      evsVersionInfo.setVersion(b.getVersion().getValue());
      evsVersionInfo.setDate((b.getDate() == null) ? null : b.getDate().getValue());
      evsVersionInfo.setComment((b.getComment() == null) ? "" : b.getComment().getValue());
      evsVersionInfo.setGraph(graphName);
      evsVersionInfo.setSource(b.getSource().getValue());
      evsVersionInfoList.add(evsVersionInfo);
    }

    return evsVersionInfoList;
  }

  /**
   * Return the EvsVersion Information.
   *
   * @param terminology the terminology
   * @return EvsVersionInfo instance.
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology", key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public EvsVersionInfo getEvsVersionInfo(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {

    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructVersionInfoQuery(terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    EvsVersionInfo evsVersionInfo = new EvsVersionInfo();
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      evsVersionInfo.setVersion(b.getVersion().getValue());
      evsVersionInfo.setDate(b.getDate().getValue());
      evsVersionInfo.setComment(b.getComment().getValue());
      evsVersionInfo.setGraph(terminology.getGraph());
    }
    return evsVersionInfo;
  }

  /**
   * Returns the returns the class counts.
   *
   * @param terminology the terminology
   * @return the returns the class counts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
//  public Long getGetClassCounts(Terminology terminology)
//    throws JsonMappingException, JsonParseException, IOException {
//    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
//    String query = queryBuilderService.constructClassCountsQuery(terminology.getGraph());
//    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
//
//    ObjectMapper mapper = new ObjectMapper();
//    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//    String count = "0";
//    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
//    Bindings[] bindings = sparqlResult.getResults().getBindings();
//    if (bindings.length == 1) {
//      count = bindings[0].getCount().getValue();
//    }
//    return Long.parseLong(count);
//  }

  /**
   * Check concept exists.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return true, if successful
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public boolean checkConceptExists(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructConceptLabelQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs concept label.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs concept label
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public String getEvsConceptLabel(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructConceptLabelQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs concept by code.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the evs concept by code
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public EvsConcept getEvsConceptByCode(String conceptCode, Terminology terminology,
    IncludeParam ip) throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByCode evsConcept = new EvsConceptByCode();
    getConcept(evsConcept, conceptCode, terminology, ip);
    return evsConcept;
  }

  /**
   * Returns the evs concept by label properties.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param propertyList the property list
   * @return the evs concept by label properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public EvsConcept getEvsConceptByLabelProperties(String conceptCode, Terminology terminology,
    List<String> propertyList) throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByLabel evsConcept = new EvsConceptByLabel();
    getConceptProperties(evsConcept, conceptCode, terminology, "byLabel", propertyList);
    return evsConcept;
  }

  /**
   * Returns the concept properties.
   *
   * @param evsConcept the evs concept
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param outputType the output type
   * @param propertyList the property list
   * @return the concept properties
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public void getConceptProperties(EvsConcept evsConcept, String conceptCode,
    Terminology terminology, String outputType, List<String> propertyList) throws IOException {
    evsConcept.setCode(conceptCode);

    List<EvsProperty> properties = getEvsProperties(conceptCode, terminology);
    if (propertyList.contains("Label") || propertyList.contains("rdfs:label")) {
      evsConcept.setLabel(getEvsConceptLabel(conceptCode, terminology));
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

    List<EvsAxiom> axioms = getEvsAxioms(conceptCode, terminology);
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
      evsConcept.setSubconcepts(getEvsSubconcepts(conceptCode, terminology, outputType));
    }
    if (propertyList.contains("Superconcept")) {
      evsConcept.setSuperconcepts(getEvsSuperconcepts(conceptCode, terminology, outputType));
    }
    if (propertyList.contains("Association")) {
      evsConcept.setAssociations(getEvsAssociations(conceptCode, terminology));
    }
    if (propertyList.contains("InverseAssociation")) {
      evsConcept.setInverseAssociations(getEvsInverseAssociations(conceptCode, terminology));
    }
    if (propertyList.contains("Role")) {
      evsConcept.setRoles(getEvsRoles(conceptCode, terminology));
    }
    if (propertyList.contains("InverseRole")) {
      evsConcept.setInverseRoles(getEvsInverseRoles(conceptCode, terminology));
    }
    if (propertyList.contains("Maps_To") || propertyList.contains("P375")) {
      evsConcept.setMapsTo(EVSUtils.getMapsTo(axioms, outputType));
    }
    if (propertyList.contains("GO_Annotation") || propertyList.contains("P211")) {
      evsConcept.setGoAnnotations(EVSUtils.getGoAnnotations(axioms, outputType));
    }
    if (propertyList.contains("DisjointWith")) {
      evsConcept.setDisjointWith(getEvsDisjointWith(conceptCode, terminology));
    }
  }

  /**
   * Returns the concept.
   *
   * @param evsConcept the evs concept
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the concept
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public void getConcept(EvsConcept evsConcept, String conceptCode, Terminology terminology,
    IncludeParam ip) throws IOException {
    evsConcept.setCode(conceptCode);

    List<EvsProperty> properties = getEvsProperties(conceptCode, terminology);

    // minimal, always do these
    evsConcept.setLabel(getEvsConceptLabel(conceptCode, terminology));
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
      final List<EvsAxiom> axioms = getEvsAxioms(conceptCode, terminology);
      if (ip.isSynonyms()) {
        evsConcept.setSynonyms(EVSUtils.getSynonyms(axioms, outputType));
      }

      if (ip.isDefinitions()) {
        evsConcept.setDefinitions(EVSUtils.getDefinitions(axioms, outputType));
        evsConcept.setAltDefinitions(EVSUtils.getAltDefinitions(axioms, outputType));
      }
      if (ip.isChildren()) {
        evsConcept.setSubconcepts(getEvsSubconcepts(conceptCode, terminology, outputType));
      }

      if (ip.isParents()) {
        evsConcept.setSuperconcepts(getEvsSuperconcepts(conceptCode, terminology, outputType));
      }

      if (ip.isAssociations()) {
        evsConcept.setAssociations(getEvsAssociations(conceptCode, terminology));
      }

      if (ip.isInverseAssociations()) {
        evsConcept.setInverseAssociations(getEvsInverseAssociations(conceptCode, terminology));
      }

      if (ip.isRoles()) {
        evsConcept.setRoles(getEvsRoles(conceptCode, terminology));
      }

      if (ip.isInverseRoles()) {
        evsConcept.setInverseRoles(getEvsInverseRoles(conceptCode, terminology));
      }

      if (ip.isMaps()) {
        evsConcept.setMapsTo(EVSUtils.getMapsTo(axioms, outputType));
      }

      // evsConcept
      // .setGoAnnotations(EVSUtils.getGoAnnotations(axioms, outputType));

      if (ip.isDisjointWith()) {
        evsConcept.setDisjointWith(getEvsDisjointWith(conceptCode, terminology));
      }
    }
  }

  /**
   * Returns the evs maps to.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs maps to
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsMapsTo> getEvsMapsTo(String conceptCode, Terminology terminology)
    throws IOException {
    List<EvsAxiom> axioms = getEvsAxioms(conceptCode, terminology);
    return EVSUtils.getMapsTo(axioms, "byCode");
  }

  /**
   * Returns the evs property.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the evs property
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public EvsConcept getEvsProperty(String conceptCode, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    EvsConceptByLabel evsConcept = new EvsConceptByLabel();
    getProperty(evsConcept, conceptCode, terminology, ip);
    return evsConcept;
  }

  /**
   * Returns the property.
   *
   * @param evsConcept the evs concept
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the property
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public void getProperty(EvsConcept evsConcept, String conceptCode, Terminology terminology,
    IncludeParam ip) throws IOException {
    evsConcept.setCode(conceptCode);

    List<EvsProperty> properties = getEvsPropertiesNoRestrictions(conceptCode, terminology);

    // minimal, always do these
    evsConcept.setCode(EVSUtils.getConceptCode(properties));

    final String pn = EVSUtils.getPreferredName(properties);
    final String conceptLabel = getEvsConceptLabel(conceptCode, terminology);
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
      List<EvsAxiom> axioms = getEvsAxioms(conceptCode, terminology);

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
        evsConcept.setSubconcepts(getEvsSubconcepts(conceptCode, terminology, outputType));
      }

      if (ip.isParents()) {
        evsConcept.setSuperconcepts(getEvsSuperconcepts(conceptCode, terminology, outputType));
      }

      if (ip.isAssociations()) {
        evsConcept.setAssociations(getEvsAssociations(conceptCode, terminology));
      }

      if (ip.isInverseAssociations()) {
        evsConcept.setInverseAssociations(getEvsInverseAssociations(conceptCode, terminology));
      }

      if (ip.isRoles()) {
        evsConcept.setRoles(getEvsRoles(conceptCode, terminology));
      }

      if (ip.isInverseRoles()) {
        evsConcept.setInverseRoles(getEvsInverseRoles(conceptCode, terminology));
      }

      if (ip.isMaps()) {
        evsConcept.setMapsTo(EVSUtils.getMapsTo(axioms, outputType));
      }
      // evsConcept
      // .setGoAnnotations(EVSUtils.getGoAnnotations(axioms, outputType));
      if (ip.isDisjointWith()) {
        evsConcept.setDisjointWith(getEvsDisjointWith(conceptCode, terminology));
      }
    }
  }

  /**
   * Returns the evs properties.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsProperty> getEvsProperties(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructPropertyQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs properties no restrictions.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs properties no restrictions
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsProperty> getEvsPropertiesNoRestrictions(String conceptCode,
    Terminology terminology) throws JsonMappingException, JsonParseException, IOException {

    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructPropertyNoRestrictionsQuery(conceptCode,
        terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs subconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param outputType the output type
   * @return the evs subconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsRelatedConcept> getEvsSubconcepts(String conceptCode, Terminology terminology,
    String outputType) throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructSubconceptQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs superconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param outputType the output type
   * @return the evs superconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsRelatedConcept> getEvsSuperconcepts(String conceptCode, Terminology terminology,
    String outputType) throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructSuperconceptQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsAssociation> getEvsAssociations(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructAssociationsQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs inverse associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs inverse associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsAssociation> getEvsInverseAssociations(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructInverseAssociationsQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs inverse roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs inverse roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsAssociation> getEvsInverseRoles(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructInverseRolesQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsAssociation> getEvsRoles(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructRolesQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs disjoint with.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs disjoint with
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsAssociation> getEvsDisjointWith(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructDisjointWithQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the evs axioms.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the evs axioms
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsAxiom> getEvsAxioms(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructAxiomQuery(conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the hierarchy.
   *
   * @param terminology the terminology
   * @return the hierarchy
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology", key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public ArrayList<String> getHierarchy(Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    ArrayList<String> parentchild = new ArrayList<String>();
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructHierarchyQuery(terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the all properties.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsConcept> getAllProperties(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructAllPropertiesQuery(terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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
      concept = getEvsProperty(code, terminology, ip);
      evsConcepts.add(concept);
    }

    return evsConcepts;
  }

  /**
   * Returns the axiom qualifiers list.
   *
   * @param propertyCode the property code
   * @param terminology the terminology
   * @return the axiom qualifiers list
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<String> getAxiomQualifiersList(String propertyCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructAxiomQualifierQuery(propertyCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Returns the all associations.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsConcept> getAllAssociations(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructAllAssociationsQuery(terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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
      concept = getEvsProperty(code, terminology, ip);
      evsConcepts.add(concept);

    }

    return evsConcepts;
  }

  /**
   * Returns the all roles.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<EvsConcept> getAllRoles(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructAllRolesQuery(terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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
      concept = getEvsProperty(code, terminology, ip);
      evsConcepts.add(concept);
    }

    return evsConcepts;
  }

  /**
   * Returns the root nodes.
   *
   * @param terminology the terminology
   * @return the root nodes
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  @Override
  public List<HierarchyNode> getRootNodes(Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    return self.getHierarchyUtils(terminology).getRootNodes();
  }

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the child nodes
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  @Override
  public List<HierarchyNode> getChildNodes(String parent, Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    return self.getHierarchyUtils(terminology).getChildNodes(parent, 0);
  }

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param maxLevel the max level
   * @param terminology the terminology
   * @return the child nodes
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  @Override
  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    return self.getHierarchyUtils(terminology).getChildNodes(parent, maxLevel);
  }

  /**
   * Returns the all child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the all child nodes
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  @Override
  public List<String> getAllChildNodes(String parent, Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    return self.getHierarchyUtils(terminology).getAllChildNodes(parent);
  }

  /**
   * Check path in hierarchy.
   *
   * @param code the code
   * @param node the node
   * @param path the path
   * @param terminology the terminology
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  @Override
  public void checkPathInHierarchy(String code, HierarchyNode node, Path path,
    Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    if (path.getConcepts().size() == 0) {
      return;
    }
    int end = path.getConcepts().size() - 1;
    ConceptNode concept = path.getConcepts().get(end);
    List<HierarchyNode> children = getChildNodes(node.getCode(), 1, terminology);
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
        checkPathInHierarchy(code, childNode, path, terminology);
      }
    }
  }

  /**
   * Returns the path in hierarchy.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the path in hierarchy
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  @Override
  public List<HierarchyNode> getPathInHierarchy(String code, Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    List<HierarchyNode> rootNodes = getRootNodes(terminology);
    Paths paths = getPathToRoot(code, terminology);
    
    for (HierarchyNode rootNode : rootNodes) {
      for (Path path : paths.getPaths()) {
        checkPathInHierarchy(code, rootNode, path, terminology);
      }
    }

    return rootNodes;
  }

  /**
   * Returns the path to root.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the path to root
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  @Override
  public Paths getPathToRoot(String code, Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    List<Path> paths = self.getPaths(terminology).getPaths();
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

  /**
   * Returns the path to parent.
   *
   * @param code the code
   * @param parentCode the parent code
   * @param terminology the terminology
   * @return the path to parent
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  @Override
  public Paths getPathToParent(String code, String parentCode, Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    List<Path> paths = self.getPaths(terminology).getPaths();
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

  /**
   * Removes the duplicate paths.
   *
   * @param paths the paths
   * @return the paths
   */
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

  /**
   * Returns the configuration data.
   *
   * @param terminology the terminology
   * @return the configuration data
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology", key = "{#root.methodName, #terminology.getTerminologyVersion()}")  
  public ConfigData getConfigurationData(Terminology terminology)
    throws JsonMappingException, JsonProcessingException, IOException {

    ConfigData configData = new ConfigData();
    configData.setEvsVersionInfo(self.getEvsVersionInfo(terminology));

    String[] sourceToBeRemoved = thesaurusProperties.getSourcesToBeRemoved();
    List<String> sourceToBeRemovedList = Arrays.asList(sourceToBeRemoved);
    List<String> sources = self.getUniqueSourcesList(terminology);
    sources.removeAll(sourceToBeRemovedList);
    configData.setFullSynSources(sources);

    return configData;
  }

  /**
   * Returns the unique sources list.
   *
   * @param terminology the terminology
   * @return the unique sources list
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology", key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public List<String> getUniqueSourcesList(Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructUniqueSourcesQuery(terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

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

  /**
   * Get hiearchy for a given terminology
   * 
   * @param terminology the terminology
   * @return the hierarchy
   * @throws IOException 
   * @throws JsonMappingException 
   * @throws JsonParseException 
   */
  @Override
  @Cacheable(value = "terminology", key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public HierarchyUtils getHierarchyUtils(Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    List<String> parentchild = self.getHierarchy(terminology);
    return new HierarchyUtils(parentchild);
  }

  /**
   * Get paths
   * 
   * @param terminology the terminology
   * @return paths
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  @Override
  @Cacheable(value = "terminology", key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public Paths getPaths(Terminology terminology) throws JsonParseException, JsonMappingException, IOException {
    HierarchyUtils hierarchy = self.getHierarchyUtils(terminology);
    return new PathFinder(hierarchy).findPaths();
  }
  
  /**
   * Returns the terminologies.
   *
   * @return the terminologies
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology", key = "#root.methodName")
  public List<Terminology> getTerminologies() throws Exception {
    return TerminologyUtils.getTerminologies(this);
  }
}
