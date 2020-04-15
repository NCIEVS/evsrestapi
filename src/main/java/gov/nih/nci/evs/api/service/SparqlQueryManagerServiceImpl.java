
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
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

  /** The self. */
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

    // NOTE: see TerminologyCacheLoader for other caching.

    // This file generation was for the "documentation files"
    // this is no longer part of the API, no need to do this
    // if (applicationProperties.getForceFileGeneration()) {
    // genDocumentationFiles();
    // }
  }

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
   * Returns terminology objects for all graphs loaded in db.
   *
   * @return the list of {@link Terminology} objects
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ParseException the parse exception
   */
  @Override
  @Cacheable(value = "terminology", key = "#root.methodName")
  public List<Terminology> getTerminologies()
    throws JsonParseException, JsonMappingException, IOException, ParseException {
    String queryPrefix = queryBuilderService.contructPrefix(null);
    String query = queryBuilderService.constructAllGraphsAndVersionsQuery();
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

  /**
   * Return the Version Information.
   *
   * @param terminology the terminology
   * @return VersionInfo instance.
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public Terminology getTerminology(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {

    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructVersionInfoQuery(terminology.getGraph());
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
   * Returns the concept label.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the concept label
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public String getConceptLabel(String conceptCode, Terminology terminology)
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
   * Returns the concept.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @param ip the ip
   * @return the concept
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public Concept getConcept(String conceptCode, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    return getConceptByType("concept", conceptCode, terminology, ip);
  }

  /**
   * Returns the maps to.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the maps to
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<gov.nih.nci.evs.api.model.Map> getMapsTo(String conceptCode, Terminology terminology)
    throws IOException {
    List<Axiom> axioms = getAxioms(conceptCode, terminology, true);
    return EVSUtils.getMapsTo(axioms);
  }

  /**
   * Returns the property.
   *
   * @param code the property code
   * @param terminology the terminology
   * @param ip the ip
   * @return the property
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public Concept getProperty(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    return getConceptByType("property", code, terminology, ip);
  }

  /**
   * Returns the qualifier.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the ip
   * @return the qualifier
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public Concept getQualifier(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    return getConceptByType("qualifier", code, terminology, ip);
  }

  /**
   * Returns the concept by type.
   *
   * @param type the type
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
    final List<Property> properties =
        conceptType.equals("concept") ? getConceptProperties(conceptCode, terminology)
            : getPropertyProperties(conceptCode, terminology);

    // minimal, always do these
    concept.setCode(EVSUtils.getProperty("code", properties));
    // TODO: CONFIG
    final String pn = EVSUtils.getProperty("Preferred_Name", properties);
    final String conceptLabel = getConceptLabel(conceptCode, terminology);

    if (!conceptType.equals("qualifier")) {
      concept.setName(conceptLabel);
    } else {
      // Handle case where preferred name and rdfs:label don't match (only for
      // qualifiers)
      if (conceptLabel != null && !conceptLabel.equals(pn) && conceptLabel.contains(" ")) {
        concept.setName(pn);
      } else {
        concept.setName(conceptLabel);
      }
    }

    if (ip.hasAnyTrue()) {

      // If loading a qualifier, don't look for additional qualifiers
      final List<Axiom> axioms =
          getAxioms(conceptCode, terminology, !conceptType.equals("qualifier"));
      log.info("XXX axioms = " + axioms);

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
        if (conceptType.equals("qualifier") && conceptLabel != null && !conceptLabel.equals(pn)
            && conceptLabel.contains(" ")) {
          final Synonym rdfsLabel = new Synonym();
          rdfsLabel.setTermGroup("rdfs:label");
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
            log.info("ZZZ HERE = " + property);
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
    String query = queryBuilderService.constructPropertyQuery(conceptCode, terminology.getGraph());
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
  public List<Property> getPropertyProperties(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {

    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructPropertyNoRestrictionsQuery(conceptCode,
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

  /**
   * Returns the subconcepts.
   *
   * @param conceptCode the concept code
   * @param terminology the terminology
   * @return the subconcepts
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Concept> getSubconcepts(String conceptCode, Terminology terminology)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query =
        queryBuilderService.constructSubconceptQuery(conceptCode, terminology.getGraph());
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
        queryBuilderService.constructSuperconceptQuery(conceptCode, terminology.getGraph());
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
        queryBuilderService.constructAssociationsQuery(conceptCode, terminology.getGraph());
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
    String query =
        queryBuilderService.constructInverseAssociationsQuery(conceptCode, terminology.getGraph());
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
        queryBuilderService.constructInverseRolesQuery(conceptCode, terminology.getGraph());
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
    String query = queryBuilderService.constructRolesQuery(conceptCode, terminology.getGraph());
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
        queryBuilderService.constructDisjointWithQuery(conceptCode, terminology.getGraph());
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
    String query = queryBuilderService.constructAxiomQuery(conceptCode, terminology.getGraph());
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
        case "P391":
          axiomObject.setSourceDate(value);
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
            log.info("XXX AXIOM = " + axiomObject.getAnnotatedProperty() + ", " + property + ", "
                + name + ", " + value);
            axiomObject.getQualifiers().add(new Qualifier(name, value));
          }
          break;

      }

    }
    axioms.add(axiomObject);
    return axioms;
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
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
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
  public List<Concept> getAllProperties(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructAllPropertiesQuery(terminology.getGraph());
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

  /**
   * Returns the all properties never used.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all properties never used
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Concept> getAllPropertiesNeverUsed(Terminology terminology, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructAllPropertiesNeverUsedQuery(terminology.getGraph());
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

  /**
   * Returns the all qualifiers.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the all qualifiers
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public List<Concept> getAllQualifiers(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructAllQualifiersQuery(terminology.getGraph());
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
  public List<Concept> getAllAssociations(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructAllAssociationsQuery(terminology.getGraph());
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
  public List<Concept> getAllRoles(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    String queryPrefix = queryBuilderService.contructPrefix(terminology.getSource());
    String query = queryBuilderService.constructAllRolesQuery(terminology.getGraph());
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

  /**
   * Returns the root nodes.
   *
   * @param terminology the terminology
   * @return the root nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
    return self.getHierarchyUtils(terminology).getChildNodes(parent, 0);
  }

  /**
   * Returns the child nodes.
   *
   * @param parent the parent
   * @param maxLevel the max level
   * @param terminology the terminology
   * @return the child nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    return self.getHierarchyUtils(terminology).getChildNodes(parent, maxLevel);
  }

  /**
   * Returns the all child nodes.
   *
   * @param parent the parent
   * @param terminology the terminology
   * @return the all child nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<String> getAllChildNodes(String parent, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    return self.getHierarchyUtils(terminology).getAllChildNodes(parent);
  }

  /**
   * Check path in hierarchy.
   *
   * @param code the code
   * @param node the node
   * @param path the path
   * @param terminology the terminology
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
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
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
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

  /**
   * Returns the path to root.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the path to root
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
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

  /**
   * Returns the path to parent.
   *
   * @param code the code
   * @param parentCode the parent code
   * @param terminology the terminology
   * @return the path to parent
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public ConfigData getConfigurationData(Terminology terminology)
    throws JsonMappingException, JsonProcessingException, IOException {

    ConfigData configData = new ConfigData();
    configData.setTerminology(terminology);

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
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
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
   * Get hiearchy for a given terminology.
   *
   * @param terminology the terminology
   * @return the hierarchy
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public HierarchyUtils getHierarchyUtils(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    List<String> parentchild = self.getHierarchy(terminology);
    return new HierarchyUtils(parentchild);
  }

  /**
   * Get paths.
   *
   * @param terminology the terminology
   * @return paths
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  @Cacheable(value = "terminology",
      key = "{#root.methodName, #terminology.getTerminologyVersion()}")
  public Paths getPaths(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    HierarchyUtils hierarchy = self.getHierarchyUtils(terminology);
    return new PathFinder(hierarchy).findPaths();
  }
}
