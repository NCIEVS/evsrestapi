
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Axiom;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.ConceptNode;
import gov.nih.nci.evs.api.model.DisjointWith;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Qualifier;
import gov.nih.nci.evs.api.model.Role;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.sparql.Bindings;
import gov.nih.nci.evs.api.model.sparql.Sparql;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.properties.ThesaurusProperties;
import gov.nih.nci.evs.api.util.ConceptUtils;
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

//  @Autowired
//  ElasticObjectService esObjectService;
  
  /** The elastic search service. */
  @Autowired
  @org.springframework.beans.factory.annotation.Qualifier("elasticSearchServiceImpl")
  ElasticSearchService elasticSearchService;

  /** The rest utils. */
  private RESTUtils restUtils = null;

  /** The self. */
  @Resource
  private SparqlQueryManagerService self;

  @Autowired
  ElasticQueryService esQueryService;
  
  /**
   * Post init.
   *
   * @throws Exception the exception
   */
  @PostConstruct
  public void postInit() throws Exception {
    restUtils = new RESTUtils(stardogProperties.getUsername(), stardogProperties.getPassword(),
        stardogProperties.getReadTimeout(), stardogProperties.getConnectTimeout());

    // NOTE: see TerminologyCacheLoader for other caching.

    // This file generation was for the "documentation files"
    // this is no longer part of the API, no need to do this
    // if (applicationProperties.getForceFileGeneration()) {
    // genDocumentationFiles();
    // }
  }

  /* see superclass */
  @Override
  public String getNamedGraph(Terminology terminology) {
    return terminology.getGraph();
  }

  /* see superclass */
  @Override
  public String getQueryURL() {
    return stardogProperties.getQueryUrl();
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology", key = "#root.methodName")
  public List<String> getAllGraphNames()
    throws JsonParseException, JsonMappingException, IOException {
    List<String> graphNames = new ArrayList<String>();
    String queryPrefix = queryBuilderService.contructPrefix(null);
    String query = queryBuilderService.constructQuery("all.graph.names", new HashMap<>());
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

  /* see superclass */
  @Override
  @Cacheable(value = "terminology", key = "#root.methodName")
  public List<Terminology> getTerminologies()
    throws JsonParseException, JsonMappingException, IOException, ParseException {
    String queryPrefix = queryBuilderService.contructPrefix(null);
    String query = queryBuilderService.constructQuery("all.graphs.and.versions", new HashMap<>());
    String queryURL = getQueryURL();
    String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    if (log.isDebugEnabled())
      log.debug("getTerminologies response - " + res);
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<Terminology> termList = new ArrayList<>();
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();

    for (Bindings b : bindings) {
      String graphName = b.getGraphName().getValue();
      if (graphName == null || graphName.equalsIgnoreCase(""))
        continue;
      Terminology term = new Terminology();
      String comment = (b.getComment() == null) ? "" : b.getComment().getValue();
      String version = b.getVersion().getValue();
      term.setDescription(comment);
      term.setVersion(b.getVersion().getValue());
      term.setName(TerminologyUtils.constructName(comment, version));
      term.setDate((b.getDate() == null) ? null : b.getDate().getValue());
      term.setGraph(graphName);
      term.setSource(b.getSource().getValue());
      termList.add(term);
    }

    if (CollectionUtils.isEmpty(termList))
      return Collections.<Terminology> emptyList();

    final DateFormat fmt = new SimpleDateFormat("MMMM dd, yyyy");
    final List<Terminology> results = new ArrayList<>();

    Collections.sort(termList, new Comparator<Terminology>() {
      @Override
      public int compare(Terminology o1, Terminology o2) {
        return -1 * o1.getVersion().compareTo(o2.getVersion());
      }
    });

    for (int i = 0; i < termList.size(); i++) {
      final Terminology term = termList.get(i);
      setTags(term, fmt);

      // set latest tag for the most recent version
      term.setLatest(i == 0);

      // temporary code -- enable date logic in getTerminologyForVersionInfo
      if (i == 0) {
        term.getTags().put("monthly", "true");
      } else {
        term.getTags().put("weekly", "true");
      }

      results.add(term);
    }

    return results;
  }

  /**
   * Sets monthly/weekly tags based on date on the given terminology object.
   *
   * @param terminology the terminology
   * @param fmt the date format
   * @throws ParseException the parse exception
   */
  private void setTags(final Terminology terminology, final DateFormat fmt) throws ParseException {
    final Date d = fmt.parse(terminology.getDate());
    Calendar cal = GregorianCalendar.getInstance();
    cal.setTime(d);

    // Count days of week; for NCI, this should be max Mondays in month
    int maxDayOfWeek = cal.getActualMaximum(Calendar.DAY_OF_WEEK_IN_MONTH);

    String version = terminology.getVersion();
    char weekIndicator = version.charAt(version.length() - 1);

    boolean monthly = false;

    switch (weekIndicator) {
      case 'e':
        monthly = true;// has to be monthly version
        break;
      case 'd':// monthly version, if month has only 4 days of week (for ex:
               // Monday) only
        if (maxDayOfWeek == 4)
          monthly = true;
        break;
      default:// case a,b,c
        break;
    }

    if (monthly)
      terminology.getTags().put("monthly", "true");
    else
      terminology.getTags().put("weekly", "true");
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public Terminology getTerminology(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {

    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("version.info", terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Terminology term = new Terminology();
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String comment = (b.getComment() == null) ? "" : b.getComment().getValue();
      String version = b.getVersion().getValue();
      term.setVersion(version);
      term.setDate(b.getDate().getValue());
      term.setDescription(comment);
      term.setGraph(terminology.getGraph());
      term.setName(TerminologyUtils.constructName(comment, version));
    }
    return term;
  }

  /**
   * Returns the returns the class counts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the returns the class counts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  // public Long getGetClassCounts(Terminology terminology)
  // throws JsonMappingException, JsonParseException, IOException {
  // String queryPrefix =
  // queryBuilderService.contructPrefix(terminology.getSource());
  // String query =
  // queryBuilderService.constructClassCountsQuery(terminology.getGraph());
  // String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
  //
  // ObjectMapper mapper = new ObjectMapper();
  // mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  // String count = "0";
  // Sparql sparqlResult = mapper.readValue(res, Sparql.class);
  // Bindings[] bindings = sparqlResult.getResults().getBindings();
  // if (bindings.length == 1) {
  // count = bindings[0].getCount().getValue();
  // }
  // return Long.parseLong(count);
  // }

  @Override
  public boolean checkConceptExists(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructQuery("concept.label", conceptCode, terminology.getGraph());
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

  /* see superclass */
  @Override
  public String getConceptLabel(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructQuery("concept.label", conceptCode, terminology.getGraph());
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

  /* see superclass */
  @Override
  public Concept getConcept(String conceptCode, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    return getConceptByType("concept", conceptCode, terminology, ip);
  }

  /* see superclass */
  @Override
  public List<gov.nih.nci.evs.api.model.Map> getMapsTo(String conceptCode, Terminology terminology)
    throws IOException {
    List<Axiom> axioms = getAxioms(conceptCode, terminology, true);
    return EVSUtils.getMapsTo(axioms);
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #code, #terminology.getTerminologyVersion(), #ip.toString()}")
  public Concept getProperty(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    return getConceptByType("property", code, terminology, ip);
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #code, #terminology.getTerminologyVersion(), #ip.toString()}")
  public Concept getQualifier(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    return getConceptByType("qualifier", code, terminology, ip);
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #code, #terminology.getTerminologyVersion(), #ip.toString()}")
  public Concept getAssociation(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    return getConceptByType("association", code, terminology, ip);
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #code, #terminology.getTerminologyVersion(), #ip.toString()}")
  public Concept getRole(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    return getConceptByType("role", code, terminology, ip);
  }

  /**
   * Returns the concept by type.
   *
   * @param conceptType the concept type
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the concept by type
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private Concept getConceptByType(final String conceptType, String conceptCode,
    Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    final Concept concept = new Concept();
    concept.setTerminology(terminology.getTerminology());
    concept.setVersion(terminology.getVersion());

    final List<Property> properties =
        conceptType.equals("concept") ? getConceptProperties(conceptCode, terminology)
            : getMetadataProperties(conceptCode, terminology);
    
    // minimal, always do these
    concept.setCode(EVSUtils.getProperty("code", properties));
    // TODO: CONFIG
    final String pn = EVSUtils.getProperty("Preferred_Name", properties);
    final String conceptLabel = getConceptLabel(conceptCode, terminology);

    if (!conceptType.equals("qualifier") && !conceptType.equals("property")) {
      concept.setName(conceptLabel);
    } else {
      // Handle case where preferred name and rdfs:label don't match (only for
      // qualifiers)
      if (conceptLabel != null && !conceptLabel.equals(pn)) {
        concept.setName(pn);
      } else {
        concept.setName(conceptLabel);
      }
    }

    if (ip.hasAnyTrue()) {

      // If loading a qualifier, don't look for additional qualifiers
      final List<Axiom> axioms =
          getAxioms(concept.getCode(), terminology, !conceptType.equals("qualifier"));

      if (ip.isSynonyms()) {
        // Set the preferred name if including synonyms
        final Synonym pnSynonym = new Synonym();
        // TODO: CONFIG
        pnSynonym.setType("Preferred_Name");
        pnSynonym.setName(pn);
        concept.getSynonyms().add(pnSynonym);

        concept.getSynonyms().addAll(EVSUtils.getSynonyms(axioms));

        // If we're using preferred name instead of the label above,
        // then we need to add an "rdfs:label" synonym here.
        if ((conceptType.equals("qualifier") || conceptType.equals("property"))
            && conceptLabel != null && !conceptLabel.equals(pn)) {
          final Synonym rdfsLabel = new Synonym();
          rdfsLabel.setType("rdfs:label");
          rdfsLabel.setName(conceptLabel);
          concept.getSynonyms().add(rdfsLabel);
        }
      }

      // Properties ending in "Name" are rendered as synonyms here.
      if (ip.isSynonyms() || ip.isProperties()) {

        final Set<String> commonProperties = EVSUtils.getCommonPropertyNames(terminology);
        for (Property property : properties) {
          final String type = property.getType();
          if (commonProperties.contains(type)) {
            continue;
          }

          if (ip.isSynonyms() && type.endsWith("_Name")) {
            // add synonym
            final Synonym synonym = new Synonym();
            synonym.setType(type);
            synonym.setName(pn);
            concept.getSynonyms().add(synonym);
          }

          if (ip.isProperties() && !type.endsWith("_Name")) {
            // Add any qualifiers to the property
            property.getQualifiers()
                .addAll(EVSUtils.getQualifiers(property.getCode(), property.getValue(), axioms));
            // add property
            concept.getProperties().add(property);
          }
        }
      }

      if (ip.isDefinitions()) {
        concept.setDefinitions(EVSUtils.getDefinitions(axioms));
      }

      if (ip.isChildren()) {
        concept.setChildren(getSubconcepts(conceptCode, terminology));
      }

      if (ip.isParents()) {
        concept.setParents(getSuperconcepts(conceptCode, terminology));
      }

      if (ip.isAssociations()) {
        concept.setAssociations(getAssociations(conceptCode, terminology));
      }

      if (ip.isInverseAssociations()) {
        concept.setInverseAssociations(getInverseAssociations(conceptCode, terminology));
      }

      if (ip.isRoles()) {
        concept.setRoles(getRoles(conceptCode, terminology));
      }

      if (ip.isInverseRoles()) {
        concept.setInverseRoles(getInverseRoles(conceptCode, terminology));
      }

      if (ip.isMaps()) {
        concept.setMaps(EVSUtils.getMapsTo(axioms));
      }

      if (ip.isDisjointWith()) {
        concept.setDisjointWith(getDisjointWith(conceptCode, terminology));
      }
    }
    
    return concept;
  }

  @Override
  public List<Concept> getConcepts(List<String> conceptCodes, Terminology terminology) throws IOException {
    if (CollectionUtils.isEmpty(conceptCodes)) return Collections.<Concept>emptyList();
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query = queryBuilderService.constructBatchQuery("concepts.batch", terminology.getGraph(), inClause);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();

    List<Concept> concepts = new ArrayList<>();
    for(Bindings b: bindings) {
      if (b.getConceptCode() == null) continue; 
      Concept c = new Concept();
      c.setCode(b.getConceptCode().getValue());
      c.setTerminology(terminology.getTerminology());
      c.setName(b.getConceptLabel().getValue());
      concepts.add(c);
    }

    Map<String, List<Property>> propertyMap = getProperties(conceptCodes, terminology);
    Map<String, List<Axiom>> axiomMap = getAxioms(conceptCodes, terminology, true);
    Map<String, List<Concept>> subConceptMap = getSubconcepts(conceptCodes, terminology);
    Map<String, List<Concept>> superConceptMap = getSuperconcepts(conceptCodes, terminology);
    Map<String, List<Association>> associationMap = getAssociations(conceptCodes, terminology);
    Map<String, List<Association>> inverseAssociationMap = getInverseAssociations(conceptCodes, terminology);
    Map<String, List<Role>> roleMap = getRoles(conceptCodes, terminology);
    Map<String, List<Role>> inverseRoleMap = getInverseRoles(conceptCodes, terminology);
    Map<String, List<DisjointWith>> disjointWithMap = getDisjointWith(conceptCodes, terminology);
    
    for(Concept concept: concepts) {
      String conceptCode = concept.getCode();
      List<Property> properties = propertyMap.get(conceptCode);
      
      // minimal, always do these
      concept.setCode(EVSUtils.getProperty("code", properties));
      // TODO: CONFIG
      final String pn = EVSUtils.getProperty("Preferred_Name", properties);
      final String conceptLabel = getConceptLabel(conceptCode, terminology);
      concept.setName(conceptLabel);

      // If loading a qualifier, don't look for additional qualifiers
      final List<Axiom> axioms = axiomMap.get(conceptCode);

      // Set the preferred name if including synonyms
      final Synonym pnSynonym = new Synonym();
      // TODO: CONFIG
      
      //adding preferred name synonym
      pnSynonym.setType("Preferred_Name");
      pnSynonym.setName(pn);
      concept.getSynonyms().add(pnSynonym);

      //adding all synonyms
      concept.getSynonyms().addAll(EVSUtils.getSynonyms(axioms));

      // Properties ending in "Name" are rendered as synonyms here.
      final Set<String> commonProperties = EVSUtils.getCommonPropertyNames(terminology);
      for (Property property : properties) {
        final String type = property.getType();
        if (commonProperties.contains(type)) {
          continue;
        }

        if (type.endsWith("_Name")) {
          // add synonym
          final Synonym synonym = new Synonym();
          synonym.setType(type);
          synonym.setName(pn);
          concept.getSynonyms().add(synonym);
        }

        if (!type.endsWith("_Name")) {
          // Add any qualifiers to the property
          property.getQualifiers()
              .addAll(EVSUtils.getQualifiers(property.getCode(), property.getValue(), axioms));
          // add property
          concept.getProperties().add(property);
        }
      }

      concept.setDefinitions(EVSUtils.getDefinitions(axioms));
      concept.setChildren(subConceptMap.get(conceptCode));
      concept.setParents(superConceptMap.get(conceptCode));
      concept.setAssociations(associationMap.get(conceptCode));
      concept.setInverseAssociations(inverseAssociationMap.get(conceptCode));
      concept.setRoles(roleMap.get(conceptCode));
      concept.setInverseRoles(inverseRoleMap.get(conceptCode));
      concept.setMaps(EVSUtils.getMapsTo(axioms));
      concept.setDisjointWith(disjointWithMap.get(conceptCode));
//      Paths paths = getPathToRoot(conceptCode, terminology);
//      concept.setPaths(new ObjectMapper().writeValueAsString(paths));
    }
    
    return concepts;
  }
  
  /**
   * Returns the properties.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Property> getConceptProperties(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructQuery("property", conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<Property> properties = new ArrayList<Property>();

    /*
     * Because the original SPARQL query that filtered out the Annotations was
     * too slow, we will be filtering them out in the post processing.
     */
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      if (b.getPropertyCode() == null) {
        Property property = new Property();
        if (b.getPropertyCode() != null) {
          property.setCode(b.getPropertyCode().getValue());
        }
        property.setType(b.getPropertyLabel().getValue());
        property.setValue(b.getPropertyValue().getValue());
        properties.add(property);
      } else {
        if (!b.getPropertyCode().getValue().startsWith("A")) {
          Property property = new Property();
          if (b.getPropertyCode() != null) {
            property.setCode(b.getPropertyCode().getValue());
          }
          property.setType(b.getPropertyLabel().getValue());
          property.setValue(b.getPropertyValue().getValue());
          properties.add(property);
        }
      }
    }

    return properties;
  }

  /**
   * Returns the properties grouped by concept
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @return the properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private Map<String, List<Property>> getProperties(List<String> conceptCodes, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query =
        queryBuilderService.constructBatchQuery("properties.batch", terminology.getGraph(), inClause);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Map<String, List<Property>> resultMap = new HashMap<>();

    /*
     * Because the original SPARQL query that filtered out the Annotations was
     * too slow, we will be filtering them out in the post processing.
     */
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String conceptCode = b.getConceptCode().getValue();
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<Property>());
      }
      
      if (b.getPropertyCode() == null) {
        Property property = new Property();
        if (b.getPropertyCode() != null) {
          property.setCode(b.getPropertyCode().getValue());
        }
        property.setType(b.getPropertyLabel().getValue());
        property.setValue(b.getPropertyValue().getValue());
        resultMap.get(conceptCode).add(property);
      } else {
        if (!b.getPropertyCode().getValue().startsWith("A")) {
          Property property = new Property();
          if (b.getPropertyCode() != null) {
            property.setCode(b.getPropertyCode().getValue());
          }
          property.setType(b.getPropertyLabel().getValue());
          property.setValue(b.getPropertyValue().getValue());
          resultMap.get(conceptCode).add(property);
        }
      }
    }

    return resultMap;
  }
  
  /**
   * Returns the properties no restrictions.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the properties no restrictions
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Property> getMetadataProperties(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {

    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("property.no.restrictions", conceptCode,
        terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<Property> properties = new ArrayList<Property>();

    /*
     * Because the original SPARQL query that filtered out the Annotations was
     * too slow, we will be filtering them out in the post processing.
     */
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Property property = new Property();
      if (b.getPropertyCode() != null) {
        property.setCode(b.getPropertyCode().getValue());
      }
      if (b.getPropertyLabel() == null) {
        property.setType(b.getProperty().getValue());
      } else {
        property.setType(b.getPropertyLabel().getValue());
      }
      property.setValue(b.getPropertyValue().getValue());
      properties.add(property);

    }

    return properties;
  }

  /* see superclass */
  @Override
  public List<Concept> getSubconcepts(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructQuery("subconcept", conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<Concept> subclasses = new ArrayList<Concept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Concept subclass = new Concept();
      subclass.setName(b.getSubclassLabel().getValue());
      subclass.setCode(b.getSubclassCode().getValue());
      subclasses.add(subclass);
    }

    return subclasses;
  }

  /* see superclass */
  public Map<String, List<Concept>> getSubconcepts(List<String> conceptCodes, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query =
        queryBuilderService.constructBatchQuery("subconcepts.batch", terminology.getGraph(), inClause);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Map<String, List<Concept>> resultMap = new HashMap<>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String conceptCode = b.getConceptCode().getValue();
      
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }
      
      Concept subclass = new Concept();
      subclass.setName(b.getSubclassLabel().getValue());
      subclass.setCode(b.getSubclassCode().getValue());
      resultMap.get(conceptCode).add(subclass);
    }

    return resultMap;
  }
  
  /**
   * Returns the superconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the superconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Concept> getSuperconcepts(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructQuery("superconcept", conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<Concept> superclasses = new ArrayList<Concept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Concept superclass = new Concept();
      superclass.setName(b.getSuperclassLabel().getValue());
      superclass.setCode(b.getSuperclassCode().getValue());
      superclasses.add(superclass);
    }

    return superclasses;
  }

  /* see superclass */
  public Map<String, List<Concept>> getSuperconcepts(List<String> conceptCodes, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query =
        queryBuilderService.constructBatchQuery("superconcepts.batch", terminology.getGraph(), inClause);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
  
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Map<String, List<Concept>> resultMap = new HashMap<>();
  
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String conceptCode = b.getConceptCode().getValue();
      
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }
      
      Concept superclass = new Concept();
      superclass.setName(b.getSuperclassLabel().getValue());
      superclass.setCode(b.getSuperclassCode().getValue());
      resultMap.get(conceptCode).add(superclass);
    }
  
    return resultMap;
  }
  
  /**
   * Returns the associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Association> getAssociations(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructQuery("associations", conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<Association> associations = new ArrayList<Association>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Association association = new Association();
      association.setType(b.getRelationship().getValue());
      association.setRelatedCode(b.getRelatedConceptCode().getValue());
      association.setRelatedName(b.getRelatedConceptLabel().getValue());
      associations.add(association);
    }

    return associations;
  }

  /* see superclass */
  public Map<String, List<Association>> getAssociations(List<String> conceptCodes, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query =
        queryBuilderService.constructBatchQuery("associations.batch", terminology.getGraph(), inClause);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Map<String, List<Association>> resultMap = new HashMap<>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String conceptCode = b.getConceptCode().getValue();
      
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }
      
      Association association = new Association();
      association.setType(b.getRelationship().getValue());
      association.setRelatedCode(b.getRelatedConceptCode().getValue());
      association.setRelatedName(b.getRelatedConceptLabel().getValue());
      resultMap.get(conceptCode).add(association);
    }

    return resultMap;
  }
  
  /**
   * Returns the inverse associations.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the inverse associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Association> getInverseAssociations(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("inverse.associations", conceptCode,
        terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<Association> associations = new ArrayList<Association>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Association association = new Association();
      association.setType(b.getRelationship().getValue());
      association.setRelatedCode(b.getRelatedConceptCode().getValue());
      association.setRelatedName(b.getRelatedConceptLabel().getValue());
      associations.add(association);
    }

    return associations;
  }

  /* see superclass */
  public Map<String, List<Association>> getInverseAssociations(List<String> conceptCodes, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query = queryBuilderService.constructBatchQuery("inverse.associations.batch", 
        terminology.getGraph(), inClause);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Map<String, List<Association>> resultMap = new HashMap<>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String conceptCode = b.getConceptCode().getValue();
      
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }
      
      Association association = new Association();
      association.setType(b.getRelationship().getValue());
      association.setRelatedCode(b.getRelatedConceptCode().getValue());
      association.setRelatedName(b.getRelatedConceptLabel().getValue());
      resultMap.get(conceptCode).add(association);
    }

    return resultMap;
  }
  
  /**
   * Returns the inverse roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the inverse roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Role> getInverseRoles(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructQuery("inverse.roles", conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<Role> roles = new ArrayList<Role>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Role role = new Role();
      role.setType(b.getRelationship().getValue());
      role.setRelatedCode(b.getRelatedConceptCode().getValue());
      role.setRelatedName(b.getRelatedConceptLabel().getValue());
      roles.add(role);
    }

    return roles;
  }

  /* see superclass */
  public Map<String, List<Role>> getInverseRoles(List<String> conceptCodes, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query =
        queryBuilderService.constructBatchQuery("inverse.roles.batch", terminology.getGraph(), inClause);
    if (log.isDebugEnabled()) log.debug("query: " + query);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
    
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Map<String, List<Role>> resultMap = new HashMap<>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String conceptCode = b.getConceptCode().getValue();
      
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }
      
      Role role = new Role();
      role.setType(b.getRelationship().getValue());
      role.setRelatedCode(b.getRelatedConceptCode().getValue());
      role.setRelatedName(b.getRelatedConceptLabel().getValue());
      resultMap.get(conceptCode).add(role);
    }

    return resultMap;
  }
  
  /**
   * Returns the roles.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Role> getRoles(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("roles", conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<Role> roles = new ArrayList<Role>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Role role = new Role();
      role.setType(b.getRelationship().getValue());
      role.setRelatedCode(b.getRelatedConceptCode().getValue());
      role.setRelatedName(b.getRelatedConceptLabel().getValue());

      roles.add(role);
    }

    return roles;
  }

  /* see superclass */
  public Map<String, List<Role>> getRoles(List<String> conceptCodes, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query = queryBuilderService.constructBatchQuery("roles.batch", terminology.getGraph(), inClause);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Map<String, List<Role>> resultMap = new HashMap<>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String conceptCode = b.getConceptCode().getValue();
      
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }
      
      Role role = new Role();
      role.setType(b.getRelationship().getValue());
      role.setRelatedCode(b.getRelatedConceptCode().getValue());
      role.setRelatedName(b.getRelatedConceptLabel().getValue());

      resultMap.get(conceptCode).add(role);
    }

    return resultMap;
  }
  
  /**
   * Returns the disjoint with.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the disjoint with
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<DisjointWith> getDisjointWith(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructQuery("disjoint.with", conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<DisjointWith> disjointWithList = new ArrayList<DisjointWith>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      DisjointWith disjointWith = new DisjointWith();
      disjointWith.setType(b.getRelationship().getValue());
      disjointWith.setRelatedCode(b.getRelatedConceptCode().getValue());
      disjointWith.setRelatedName(b.getRelatedConceptLabel().getValue());
      disjointWithList.add(disjointWith);
    }

    return disjointWithList;
  }

  /* see superclass */
  public Map<String, List<DisjointWith>> getDisjointWith(List<String> conceptCodes, Terminology terminology)
      throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query =
        queryBuilderService.constructBatchQuery("disjoint.with.batch", terminology.getGraph(), inClause);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Map<String, List<DisjointWith>> resultMap = new HashMap<>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      String conceptCode = b.getConceptCode().getValue();
      
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }        
      
      DisjointWith disjointWith = new DisjointWith();
      disjointWith.setType(b.getRelationship().getValue());
      disjointWith.setRelatedCode(b.getRelatedConceptCode().getValue());
      disjointWith.setRelatedName(b.getRelatedConceptLabel().getValue());
      resultMap.get(conceptCode).add(disjointWith);
    }

    return resultMap;
  }
  
  /**
   * Returns the axioms.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param qualifierFlag the qualifier flag
   * @return the axioms
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Axiom> getAxioms(String conceptCode, Terminology terminology, boolean qualifierFlag)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("axiom", conceptCode, terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<Axiom> axioms = new ArrayList<Axiom>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    if (bindings.length == 0) {
      return axioms;
    }
    Axiom axiomObject = new Axiom();
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
        axioms.add(axiomObject);
        axiomObject = new Axiom();
      }
      sw = true;
      oldAxiom = axiom;

      //TODO: call new function
      setAxiomProperty(property, value, qualifierFlag, axiomObject, terminology);
    }
    axioms.add(axiomObject);
    return axioms;
  }

  /**
   * Returns the axioms grouped by concept
   *
   * @param conceptCodes the list of concept codes
   * @param terminology the terminology
   * @param qualifierFlag the qualifier flag
   * @return the axioms
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */  
  private Map<String, List<Axiom>> getAxioms(List<String> conceptCodes, Terminology terminology, boolean qualifierFlag)
      throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String inClause = getInClause(conceptCodes);
    String query = queryBuilderService.constructBatchQuery("axioms.batch", terminology.getGraph(), inClause);
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Map<String, List<Axiom>> resultMap = new HashMap<>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    if (bindings.length == 0) {
      return Collections.<String, List<Axiom>>emptyMap();
    }
    Axiom axiomObject = new Axiom();
    Boolean sw = false;
    String oldAxiom = "";
    String conceptCode = "";
    for (Bindings b : bindings) {
      conceptCode = b.getConceptCode().getValue();
      
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<Axiom>());
      }
      
      String axiom = b.getAxiom().getValue();
      String property = b.getAxiomProperty().getValue().split("#")[1];
      String value = b.getAxiomValue().getValue();
      if (value.contains("#")) {
        value = value.split("#")[1];
      }

      if (sw && !axiom.equals(oldAxiom)) {
        resultMap.get(conceptCode).add(axiomObject);
        axiomObject = new Axiom();
      }
      sw = true;
      oldAxiom = axiom;

      setAxiomProperty(property, value, qualifierFlag, axiomObject, terminology);
    }
    resultMap.get(conceptCode).add(axiomObject);
    return resultMap;
  }
  
  /**
   * sets axiom property
   * 
   * @param property the property
   * @param value the value
   * @param qualifierFlag the qualifier flag
   * @param axiomObject the axiom object
   * @param terminology the terminology
   * @throws IOException
   */
  private void setAxiomProperty(String property, String value, boolean qualifierFlag, 
      Axiom axiomObject, Terminology terminology) throws IOException {
    // TODO: CONFIG
    switch (property) {
      case "annotatedSource":
        axiomObject.setAnnotatedSource(value);
        break;
      case "annotatedTarget":
        axiomObject.setAnnotatedTarget(value);
        break;
      case "annotatedProperty":
        axiomObject.setAnnotatedProperty(value);
        break;
      case "type":
        axiomObject.setType(value);
        break;
      case "P393":
        axiomObject.setRelationshipToTarget(value);
        break;
      case "P395":
        axiomObject.setTargetCode(value);
        break;
      case "P394":
        axiomObject.setTargetTermType(value);
        break;
      case "P396":
        axiomObject.setTargetTerminology(value);
        break;
      case "P397":
        axiomObject.setTargetTerminologyVersion(value);
        break;
      case "P378":
        axiomObject.setDefSource(value);
        break;
      case "P385":
        axiomObject.setSourceCode(value);
        break;
      case "P386":
        axiomObject.setSubsourceName(value);
        break;
      case "P383":
        axiomObject.setTermGroup(value);
        break;
      case "P384":
        axiomObject.setTermSource(value);
        break;
      case "term-source":
        axiomObject.setTermSource(value);
        break;
      case "term-group":
        axiomObject.setTermGroup(value);
        break;
      default:
        if (qualifierFlag) {
          final String name = EVSUtils.getQualifierName(
              self.getAllQualifiers(terminology, new IncludeParam("minimal")), property);
          axiomObject.getQualifiers().add(new Qualifier(name, value));
        }
        break;
    }
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public ArrayList<String> getHierarchy(Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    ArrayList<String> parentchild = new ArrayList<String>();
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("hierarchy", terminology.getGraph());
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

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion(), #ip.toString()}")
  public List<Concept> getAllProperties(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("all.properties", terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> properties = new ArrayList<String>();
    ArrayList<Concept> concepts = new ArrayList<Concept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      properties.add(b.getPropertyCode().getValue());
    }

    for (String code : properties) {
      Concept concept = getProperty(code, terminology, ip);
      concepts.add(concept);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<String> getDistinctPropertyValues(Terminology terminology, String propertyCode)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    Map<String, String> values =
        ConceptUtils.asMap("propertyCode", propertyCode, "namedGraph", terminology.getGraph());
    String query = queryBuilderService.constructQuery("distinct.property.values", values);
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

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion(), #ip.toString()}")
  public List<Concept> getAllPropertiesNeverUsed(Terminology terminology, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructQuery("all.propertiesNeverUsed", terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> properties = new ArrayList<String>();
    ArrayList<Concept> concepts = new ArrayList<Concept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      properties.add(b.getPropertyCode().getValue());
    }

    for (String code : properties) {
      Concept concept = getProperty(code, terminology, ip);
      concepts.add(concept);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion(), #ip.toString()}")
  public List<Concept> getAllQualifiers(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("all.qualifiers", terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> properties = new ArrayList<String>();
    ArrayList<Concept> concepts = new ArrayList<Concept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      properties.add(b.getPropertyCode().getValue());
    }

    for (String code : properties) {
      Concept concept = getQualifier(code, terminology, ip);
      concepts.add(concept);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<String> getQualifierValues(String propertyCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    Map<String, String> values =
        ConceptUtils.asMap("propertyCode", propertyCode, "namedGraph", terminology.getGraph());
    String query = queryBuilderService.constructQuery("axiom.qualifier", values);
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

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion(), #ip.toString()}")
  public List<Concept> getAllAssociations(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("all.associations", terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> properties = new ArrayList<String>();
    ArrayList<Concept> concepts = new ArrayList<Concept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      properties.add(b.getPropertyCode().getValue());
    }

    for (String code : properties) {
      Concept concept = getProperty(code, terminology, ip);
      concepts.add(concept);

    }

    return concepts;
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion(), #ip.toString()}")
  public List<Concept> getAllRoles(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructQuery("all.roles", terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ArrayList<String> properties = new ArrayList<String>();
    ArrayList<Concept> concepts = new ArrayList<Concept>();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      properties.add(b.getPropertyCode().getValue());
    }

    for (String code : properties) {
      Concept concept = null;
      concept = getProperty(code, terminology, ip);
      concepts.add(concept);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<HierarchyNode> getRootNodes(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    return self.getHierarchyUtils(terminology).getRootNodes();
  }

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the child nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<HierarchyNode> getChildNodes(String parent, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    return esQueryService.getChildNodes(parent, 0, terminology);
  }

  /* see superclass */
  @Override
  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    return esQueryService.getChildNodes(parent, maxLevel, terminology);
  }

  /* see superclass */
  @Override
  public List<String> getAllChildNodes(String parent, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    return esQueryService.getAllChildNodes(parent, terminology);
  }

  /* see superclass */
  @Override
  public void checkPathInHierarchy(String code, HierarchyNode node, Path path,
    Terminology terminology) throws JsonParseException, JsonMappingException, IOException {

    // check for empty path
    if (path.getConcepts().size() == 0) {
      return;
    }
    
    // get path length
    int end = path.getConcepts().size() - 1;

    // find the end (in this case top) of the path
    ConceptNode concept = path.getConcepts().get(end);
    List<HierarchyNode> children = getChildNodes(node.getCode(), 1, terminology);

    // attach children to node if necessary
    if (node.getChildren().size() == 0) {
      node.setChildren(children);
    }

    // is this the top level node containing the term in question
    if (concept.getCode().equals(node.getCode())) {

      // is this the term itself
      if (node.getCode().equals(code)) {
        node.setHighlight(true);
        return;
      }
      node.setExpanded(true);
      if (path.getConcepts() != null && !path.getConcepts().isEmpty()) { 
        path.getConcepts().remove(path.getConcepts().size() - 1);
      }

      // recursively check its children until we find the term
      for (HierarchyNode childNode : node.getChildren()) { 
        checkPathInHierarchy(code, childNode, path, terminology);
      }
    }

    // this node does not contain the term
    else {
    	node.setChildren(null); // we don't care about its children
    }
    	
  }

  /* see superclass */
  @Override
  public List<HierarchyNode> getPathInHierarchy(String code, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    List<HierarchyNode> rootNodes = getRootNodes(terminology);
    Paths paths = getPathToRoot(code, terminology);

    for (HierarchyNode rootNode : rootNodes) {
      for (Path path : paths.getPaths()) {
        checkPathInHierarchy(code, rootNode, path, terminology);
      }
    }

    return rootNodes;
  }

  /* see superclass */
  @Override
  public Paths getPathToRoot(String code, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
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

  /* see superclass */
  @Override
  public Paths getPathToParent(String code, String parentCode, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
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

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public HierarchyUtils getHierarchyUtils(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    List<String> parentchild = self.getHierarchy(terminology);
    return new HierarchyUtils(parentchild);
  }

  /* see superclass */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public Paths getPaths(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    HierarchyUtils hierarchy = self.getHierarchyUtils(terminology);
    return new PathFinder(hierarchy).findPaths();
  }

  /* see superclass */
  @Override
  public List<ConceptMinimal> getSynonymSources(Terminology terminology)
    throws JsonMappingException, JsonProcessingException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    // TODO: CONFIG
    String query = queryBuilderService.constructQuery("axiom.qualifier.values", "P384",
        terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<ConceptMinimal> sources = new ArrayList<>();
    final Map<String, String> map = thesaurusProperties.getSources();
    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Concept concept = new Concept();
      concept.setTerminology(terminology.getTerminology());
      concept.setCode(b.getPropertyValue().getValue());
      if (map.containsKey(concept.getCode())) {
        concept.setName(map.get(concept.getCode()));
      } else if (concept.getCode().contains(" ")
          && map.containsKey(concept.getCode().substring(0, concept.getCode().indexOf(" ")))) {
        concept.setName(map.get(concept.getCode().substring(0, concept.getCode().indexOf(" ")))
            + ", version " + concept.getCode().substring(concept.getCode().indexOf(" ") + 1));
      }

      sources.add(concept);
    }

    return sources;
  }

  /* see superclass */
  @Override
  public List<ConceptMinimal> getTermTypes(Terminology terminology)
    throws JsonMappingException, JsonProcessingException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    // TODO: CONFIG
    String query = queryBuilderService.constructQuery("axiom.qualifier.values", "P383",
        terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<ConceptMinimal> sources = new ArrayList<>();
    final Map<String, String> map = thesaurusProperties.getTermTypes();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Concept concept = new Concept();
      concept.setTerminology(terminology.getTerminology());
      concept.setCode(b.getPropertyValue().getValue());
      if (map.containsKey(concept.getCode())) {
        concept.setName(map.get(concept.getCode()));
        if (concept.getName().startsWith("*")) {
          concept.setCode(concept.getCode() + "*");
          concept.setName(concept.getName().substring(1));
        }
      }
      sources.add(concept);
    }

    return sources;
  }

 

  /* see superclass */
  @Override
  public List<ConceptMinimal> getDefinitionSources(Terminology terminology)
    throws JsonMappingException, JsonProcessingException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    // TODO: CONFIG
    String query = queryBuilderService.constructQuery("axiom.qualifier.values", "P378",
        terminology.getGraph());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<ConceptMinimal> sources = new ArrayList<>();
    // Documentation on source definitions
    final Map<String, String> map = thesaurusProperties.getSources();

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (Bindings b : bindings) {
      Concept concept = new Concept();
      concept.setTerminology(terminology.getTerminology());
      concept.setCode(b.getPropertyValue().getValue());
      if (map.containsKey(concept.getCode())) {
        concept.setName(map.get(concept.getCode()));
      } else if (concept.getCode().contains(" ")
          && map.containsKey(concept.getCode().substring(0, concept.getCode().indexOf(" ")))) {
        concept.setName(map.get(concept.getCode().substring(0, concept.getCode().indexOf(" ")))
            + ", version " + concept.getCode().substring(concept.getCode().indexOf(" ") + 1));
      }
      sources.add(concept);
    }

    return sources;
  }

  /**
   * gets all concepts (minimal)
   * 
   * @param terminology the terminology
   * @return list of concept objects
   */
  @Override
  public List<Concept> getAllConcepts(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    log.debug("query prefix = {}", queryPrefix);
    String query = queryBuilderService.constructQuery("all.concepts", terminology.getGraph());
    log.debug("query = {}", query);
    log.debug("query url = {}", getQueryURL());
    String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
    
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    Bindings[] bindings = sparqlResult.getResults().getBindings();

    List<Concept> concepts = new ArrayList<>();
    for(Bindings b: bindings) {
      if (b.getConceptCode() == null) continue; 
      Concept c = new Concept();
      c.setCode(b.getConceptCode().getValue());
      c.setTerminology(terminology.getTerminology());
      c.setName(b.getConceptLabel().getValue());
      concepts.add(c);
    }
    
    return concepts;
  }

  /**
   * prepares "in" clause to be used sparql queries for given values
   * 
   * @param values
   * @return
   */
  private String getInClause(List<String> values) {
    return new StringBuilder()
      .append("'")
      .append(String.join("', '", values))
      .append("'")
      .toString();
  }
  
}
