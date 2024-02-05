package gov.nih.nci.evs.api.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.AssociationEntry;
import gov.nih.nci.evs.api.model.Axiom;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
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
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.model.sparql.Bindings;
import gov.nih.nci.evs.api.model.sparql.Sparql;
import gov.nih.nci.evs.api.properties.ApplicationProperties;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.EVSUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.RESTUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Reference implementation of {@link SparqlQueryManagerService}. Includes hibernate tags for MEME
 * database.
 */
@Service
public class SparqlQueryManagerServiceImpl implements SparqlQueryManagerService {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(SparqlQueryManagerServiceImpl.class);

  /** The stardog properties. */
  @Autowired StardogProperties stardogProperties;

  /** The query builder service. */
  @Autowired QueryBuilderService queryBuilderService;

  /** The application properties. */
  @Autowired ApplicationProperties applicationProperties;

  /** The elastic search service. */
  @Autowired
  @org.springframework.beans.factory.annotation.Qualifier("elasticSearchServiceImpl") ElasticSearchService elasticSearchService;

  /** The elastic search service. */
  @Autowired ElasticQueryService elasticQueryService;

  /** The sparql query cache service */
  @Autowired SparqlQueryCacheService sparqlQueryCacheService;

  /** The utils. */
  @Autowired TerminologyUtils utils;

  /** The rest utils. */
  private RESTUtils restUtils = null;

  /**
   * Post init.
   *
   * @throws Exception the exception
   */
  @PostConstruct
  public void postInit() throws Exception {
    restUtils =
        new RESTUtils(
            stardogProperties.getUsername(),
            stardogProperties.getPassword(),
            stardogProperties.getReadTimeout(),
            stardogProperties.getConnectTimeout());

    // NOTE: see TerminologyCacheLoader for other caching.

    // This file generation was for the "documentation files"
    // this is no longer part of the API, no need to do this
    // if (applicationProperties.getForceFileGeneration()) {
    // genDocumentationFiles();
    // }
  }

  /* see superclass */
  @Override
  public String getNamedGraph(final Terminology terminology) {
    return terminology.getGraph();
  }

  /* see superclass */
  @Override
  public String getQueryURL() {
    return stardogProperties.getQueryUrl();
  }

  // Here check the qualified form as well as the URI

  // Here check the qualified form as well as the URI

  /* see superclass */
  @Override
  public List<String> getAllGraphNames() throws Exception {
    final List<String> graphNames = new ArrayList<>();
    final String queryPrefix = queryBuilderService.constructPrefix(null);
    final String query = queryBuilderService.constructGraphQuery("all.graph.names", null);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    log.debug("getAllGraphNames response - " + res);
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();

    for (final Bindings b : bindings) {
      final String graphName = b.getGraphName().getValue();
      log.debug("getAllGraphNames graphName - " + graphName);
      if (graphName != null && !graphName.equalsIgnoreCase("")) {
        graphNames.add(graphName);
      }
    }

    return graphNames;
  }

  /* see superclass */
  @Override
  public List<Terminology> getTerminologies(final String db) throws Exception, ParseException {
    final List<String> ignoreSources = getIgnoreSourceUrls();
    final String queryPrefix = queryBuilderService.constructPrefix(null);
    final String query =
        ignoreSources.isEmpty()
            ? queryBuilderService.constructGraphQuery("all.graphs.and.versions", ignoreSources)
            : queryBuilderService.constructGraphQuery(
                "all.graphs.and.versions.ignore.sources", ignoreSources);

    // NOTE: this is not a hardened approach
    final String queryURL = getQueryURL().replace(stardogProperties.getDb(), db);
    final String res = restUtils.runSPARQL(queryPrefix + query, queryURL);

    // if (log.isDebugEnabled()) {
    // log.debug("getTerminologies response - " + res);
    // }
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Terminology> termList = new ArrayList<>();
    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();

    for (final Bindings b : bindings) {
      final String graphName = b.getGraphName().getValue();
      if (graphName == null || graphName.equalsIgnoreCase("")) {
        continue;
      }
      final Terminology term = new Terminology();
      final String comment = (b.getComment() == null) ? "" : b.getComment().getValue();
      // String version = b.getVersion().getValue();
      term.setDescription(comment);
      // Extract a version if a owl:versionIRI was used
      term.setVersion(b.getVersion().getValue().replaceFirst(".*/([\\d-]+)/[a-zA-Z]+.owl", "$1"));
      // term.setName(TerminologyUtils.constructName(comment, version));
      term.setDate((b.getDate() == null) ? term.getVersion() : b.getDate().getValue());
      term.setGraph(graphName);
      term.setSource(b.getSource().getValue());
      term.setTerminology(getTerm(term.getSource()));
      termList.add(term);
    }

    if (CollectionUtils.isEmpty(termList)) {
      return Collections.<Terminology>emptyList();
    }

    final List<Terminology> results = new ArrayList<>();

    // Reverse sort by version
    Collections.sort(
        termList,
        new Comparator<Terminology>() {
          @Override
          public int compare(final Terminology o1, final Terminology o2) {
            return -1 * o1.getVersion().compareTo(o2.getVersion());
          }
        });

    for (int i = 0; i < termList.size(); i++) {
      final Terminology term = termList.get(i);

      // set latest tag for the most recent version
      term.setLatest(i == 0);
      results.add(term);
    }

    return results;
  }

  private String getTerm(String source) {
    String term = FilenameUtils.getBaseName(source);
    if (term.equals("Thesaurus")) {
      return "ncit";
    }
    return term.toLowerCase();
  }

  /* see superclass */
  @Override
  public Concept getConcept(
      final String conceptCode, final Terminology terminology, final IncludeParam ip)
      throws Exception {
    return getConceptByType("concept", conceptCode, terminology, ip);
  }

  /* see superclass */
  @Override
  public List<gov.nih.nci.evs.api.model.ConceptMap> getMapsTo(
      final String conceptCode, final Terminology terminology) throws Exception {
    final List<Axiom> axioms = getAxioms(conceptCode, terminology, true);
    return EVSUtils.getMapsTo(terminology, axioms);
  }

  /* see superclass */
  @Override
  public Concept getProperty(
      final String code, final Terminology terminology, final IncludeParam ip) throws Exception {
    return getConceptByType("property", code, terminology, ip);
  }

  /* see superclass */
  @Override
  public Concept getQualifier(
      final String code, final Terminology terminology, final IncludeParam ip) throws Exception {
    return getConceptByType("qualifier", code, terminology, ip);
  }

  /* see superclass */
  @Override
  public Concept getAssociation(
      final String code, final Terminology terminology, final IncludeParam ip) throws Exception {
    return getConceptByType("association", code, terminology, ip);
  }

  /* see superclass */
  @Override
  public Concept getRole(final String code, final Terminology terminology, final IncludeParam ip)
      throws Exception {
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
   * @throws Exception the exception
   */
  private Concept getConceptByType(
      final String conceptType,
      final String conceptCode,
      final Terminology terminology,
      final IncludeParam ip)
      throws Exception {
    final Concept concept = new Concept();
    concept.setTerminology(terminology.getTerminology());
    concept.setVersion(terminology.getVersion());
    concept.setActive(true);
    final List<Property> properties = getProperties(conceptCode, terminology);

    // always set code (if it's an rdf:about, strip after the #)
    concept.setCode(conceptCode);
    if (conceptCode.startsWith("http")) {
      // Not qualified code here because it should match "type" for metadata
      // or the actual code for a concept with no other info
      concept.setUri(conceptCode);
      if (conceptType.equals("concept")) {
        concept.setCode(EVSUtils.getCodeFromUri(conceptCode));
      } else {
        concept.setCode(EVSUtils.getQualifiedCodeFromUri(conceptCode));
      }
    }

    // Find preferred name (or rdfs:label if no pref name)
    String pn = EVSUtils.getProperty(terminology.getMetadata().getPreferredName(), properties);
    // Otherwise, compute a name from the code
    if (pn == null) {
      if (concept.getUri() != null) {
        pn = EVSUtils.getLabelFromUri(concept.getUri());
      } else {
        pn = concept.getCode();
      }
    }
    concept.setName(pn);

    // TODO: need to set leaf
    // concept.setLeaf(...

    if (ip.hasAnyTrue()) {

      // If loading a qualifier, don't look for additional qualifiers
      final List<Axiom> axioms =
          getAxioms(concept.getCode(), terminology, !conceptType.equals("qualifier"));

      // final Set<String> syNameType = new HashSet<>();

      if (ip.isSynonyms()) {

        final List<Synonym> synonyms = EVSUtils.getSynonyms(terminology, properties, axioms);
        // syNameType.addAll(
        // synonyms.stream().map(sy -> sy.getType() +
        // sy.getName()).collect(Collectors.toSet()));
        concept.getSynonyms().addAll(synonyms);

        // If no synonyms, we need to add at least one
        if (concept.getSynonyms().size() == 0) {
          final Synonym sy = new Synonym();
          // sy.setTypeCode("default");
          sy.setType("default");
          sy.setName(pn);
          sy.setNormName(ConceptUtils.normalize(sy.getName()));
          sy.setNormName(ConceptUtils.normalizeWithStemming(sy.getName()));
          concept.getSynonyms().add(sy);
          // syNameType.add("default" + pn);
        }
      }

      // Properties ending in "Name" are rendered as synonyms here.
      concept.setConceptStatus("DEFAULT");
      if (ip.isProperties()) {

        // Render synonym properties and normal properties
        for (final Property property : properties) {

          // Skip definition properties - load them as definitions
          if (terminology.getMetadata().getDefinition().contains(property.getCode())) {
            continue;
          }

          // Set concept status
          if (property.getCode().equals(terminology.getMetadata().getConceptStatus())) {
            // Set to retired if it matches config
            if (property.getValue().equals(terminology.getMetadata().getRetiredStatusValue())) {
              concept.setConceptStatus(property.getValue());
              concept.setActive(false);
            } else {
              concept.setConceptStatus(property.getValue());
            }
          }

          // Handle subset link (and continue)
          if (property.getCode().equals(terminology.getMetadata().getSubsetLink())) {
            // e.g.EVS/CDISC/SDTM/SDTM Terminology.xls|2:all
            String link = property.getValue().replaceFirst("EVS/", "").split("\\|")[0];
            // If there is a filename, remove it and just link to the directory
            // above
            if (link.contains(".")) {
              link = link.replaceFirst("(.*)/[^/]+\\.[^/]+", "$1");
            }
            if (!link.equals("null")) {
              // Prepend the subset prefix, remove the "EVS" part.
              concept.setSubsetLink(terminology.getMetadata().getSubsetPrefix() + link);
            }
            continue;
          }

          // Handle if not a remodeled property (e.g. sy or definition)
          if (ip.isProperties()
              && !terminology.getMetadata().isRemodeledProperty(property.getCode())) {
            // Add any qualifiers to the property
            property
                .getQualifiers()
                .addAll(EVSUtils.getQualifiers(property.getCode(), property.getValue(), axioms));
            // add property
            concept.getProperties().add(property);
          }
        }
      }

      if (ip.isDefinitions()) {
        concept.setDefinitions(EVSUtils.getDefinitions(terminology, properties, axioms));
      }

      if (ip.isChildren()) {
        concept.setChildren(getChildren(conceptCode, terminology));
      }

      if (ip.isParents()) {
        concept.setParents(getParents(conceptCode, terminology));
      }

      if (ip.isAssociations()) {
        concept.setAssociations(getAssociations(conceptCode, terminology));
      }

      if (ip.isInverseAssociations()) {
        concept.setInverseAssociations(getInverseAssociations(conceptCode, terminology));
      }

      // Only "concept" types have roles
      if (conceptType.equals("concept")) {
        if (ip.isRoles()) {
          concept.setRoles(getRoles(conceptCode, terminology));
        }

        if (ip.isInverseRoles()) {
          concept.setInverseRoles(getInverseRoles(conceptCode, terminology));
        }
      }

      if (ip.isMaps()) {
        concept.setMaps(EVSUtils.getMapsTo(terminology, axioms));
      }

      if (ip.isDisjointWith()) {
        concept.setDisjointWith(getDisjointWith(conceptCode, terminology));
      }

      // NOTE: "paths" and "descendants" are not read here

    }

    // Ensure that all list elements of the concept are in a natural sort
    // order
    concept.sortLists();

    return concept;
  }

  /* see superclass */
  @Override
  public List<Concept> getConcepts(
      final List<Concept> origConcepts,
      final Terminology terminology,
      final HierarchyUtils hierarchy)
      throws Exception {

    if (CollectionUtils.isEmpty(origConcepts)) {
      return Collections.<Concept>emptyList();
    }
    final List<Concept> concepts = new ArrayList<>();
    // Copy the original concepts to avoid keeping references around
    for (final Concept concept : origConcepts) {
      concepts.add(new Concept(concept));
    }
    final ExecutorService executor = Executors.newFixedThreadPool(4);
    final List<Exception> exceptions = new ArrayList<>();

    final List<String> conceptUris =
        concepts.stream()
            .filter(c -> c.getUri() != null)
            .map(c -> c.getUri())
            .collect(Collectors.toList());
    final List<String> conceptCodes =
        concepts.stream().map(c -> c.getCode()).collect(Collectors.toList());
    final Map<String, List<Property>> propertyMap = new HashMap<>();
    final Map<String, List<Axiom>> axiomMap = new HashMap<>();
    final Map<String, List<Concept>> childMap = getChildren(conceptCodes, terminology, hierarchy);
    final Map<String, List<Concept>> parentMap = getParents(conceptCodes, terminology, hierarchy);
    final Map<String, List<Association>> associationMap = hierarchy.getAssociationMap();
    final Map<String, List<Association>> inverseAssociationMap =
        hierarchy.getInverseAssociationMap();
    final Map<String, List<Role>> roleMap = new HashMap<>();
    final Map<String, List<Role>> inverseRoleMap = new HashMap<>();
    final Map<String, List<Role>> complexRoleMap = hierarchy.getRoleMap();
    final Map<String, List<Role>> complexInverseRoleMap = hierarchy.getInverseRoleMap();
    final Map<String, List<DisjointWith>> disjointWithMap = new HashMap<>();

    executor.submit(
        () -> {
          try {
            log.info("      start main");
            // send in URIs for about clause if available
            propertyMap.putAll(
                getProperties(conceptUris.isEmpty() ? conceptCodes : conceptUris, terminology));
            disjointWithMap.putAll(
                getDisjointWith(conceptUris.isEmpty() ? conceptCodes : conceptUris, terminology));
            log.info("      finish main");
          } catch (final Exception e) {
            log.error("Unexpected error on main", e);
            exceptions.add(e);
          }
        });

    executor.submit(
        () -> {
          try {
            log.info("      start roles");
            roleMap.putAll(
                getRoles(conceptUris.isEmpty() ? conceptCodes : conceptUris, terminology));
            log.info("      finish roles");
          } catch (final Exception e) {
            log.error("Unexpected error on roles", e);
            exceptions.add(e);
          }
        });

    executor.submit(
        () -> {
          try {
            log.info("      start inverse roles");
            inverseRoleMap.putAll(
                getInverseRoles(conceptUris.isEmpty() ? conceptCodes : conceptUris, terminology));
            log.info("      finish inverse roles");
          } catch (final Exception e) {
            log.error("Unexpected error on inverse roles", e);
            exceptions.add(e);
          }
        });

    executor.submit(
        () -> {
          try {
            log.info("      start axioms");
            axiomMap.putAll(
                getAxioms(conceptUris.isEmpty() ? conceptCodes : conceptUris, terminology, true));
            log.info("      finish axioms");
          } catch (final Exception e) {
            log.error("Unexpected error on axioms", e);
            exceptions.add(e);
          }
        });
    // Shutdown executor
    executor.shutdown();

    // Wait up to 10 min for processes to stop
    try {
      executor.awaitTermination(10, TimeUnit.MINUTES);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (axiomMap.isEmpty()) {
      // This likely occurs if the 10 minute awaitTermination isn't long enough
      log.warn(
          "Missing axioms, likely because awaitTermination was not long enough "
              + "(or there are no axioms).");
    }

    // Throw an
    if (!exceptions.isEmpty()) {
      throw new RuntimeException(exceptions.get(0));
    }
    ObjectMapper mapper = new ObjectMapper();
    for (final Concept concept : concepts) {
      final String conceptCode = concept.getCode();
      List<Property> properties =
          propertyMap.containsKey(conceptCode) ? propertyMap.get(conceptCode) : new ArrayList<>(0);
      if (properties.size() == 0 && concept.getUri() != null) {
        final String conceptUri = concept.getUri();
        properties =
            propertyMap.containsKey(conceptUri) ? propertyMap.get(conceptUri) : new ArrayList<>(0);
      }

      // minimal, always do these
      final String pn =
          EVSUtils.getProperty(terminology.getMetadata().getPreferredName(), properties);
      if (pn != null) {
        // Override concept name with PN if there is one
        concept.setName(pn);
      }
      // final String conceptLabel = getConceptLabel(conceptCode, terminology);
      // concept.setName(conceptLabel);
      concept.setNormName(ConceptUtils.normalize(pn));
      concept.setStemName(ConceptUtils.normalizeWithStemming(pn));

      // If loading a qualifier, don't look for additional qualifiers
      List<Axiom> axioms =
          axiomMap.get(conceptCode) == null ? new ArrayList<>(0) : axiomMap.get(conceptCode);
      if (axioms.size() == 0) {
        axioms =
            axiomMap.get(concept.getUri()) == null
                ? new ArrayList<>(0)
                : axiomMap.get(concept.getUri());
      }

      // adding all synonyms
      final List<Synonym> synonyms = EVSUtils.getSynonyms(terminology, properties, axioms);
      concept.getSynonyms().addAll(synonyms);
      if (synonyms.size() == 0) {
        final Synonym sy = new Synonym();
        sy.setType("default");
        sy.setName(concept.getName());
        sy.setNormName(ConceptUtils.normalize(sy.getName()));
        concept.getSynonyms().add(sy);
      }

      // Render synonym properties and normal properties
      concept.setConceptStatus("DEFAULT");
      for (final Property property : properties) {

        // Skip definitions rendered as properties
        if (terminology.getMetadata().getDefinition().contains(property.getCode())) {
          continue;
        }

        // Set concept status for retired concepts
        if (property.getCode().equals(terminology.getMetadata().getConceptStatus())) {
          // Set to retired if it matches config
          if (property.getValue().equals(terminology.getMetadata().getRetiredStatusValue())) {
            concept.setConceptStatus("Retired_Concept");
            concept.setActive(false);
          } else {
            concept.setConceptStatus(property.getValue());
          }
        }

        // Handle subset link (and continue)
        if (property.getCode().equals(terminology.getMetadata().getSubsetLink())) {
          // e.g. EVS/CDISC/SDTM/SDTM Terminology.xls|2:all => EVS/CDISC/SDTM/
          String link = property.getValue().replaceFirst("EVS/", "").split("\\|")[0];
          // If there is a filename, remove it and just link to the directory
          // above
          if (link.contains(".")) {
            link = link.replaceFirst("(.*)/[^/]+\\.[^/]+", "$1");
          }
          if (!link.equals("null")) {
            // Prepend the subset prefix, remove the "EVS" part.
            concept.setSubsetLink(terminology.getMetadata().getSubsetPrefix() + link);
          }
          continue;
        }

        // Handle if not a remodeled property (e.g. synonym or definition)
        if (!terminology.getMetadata().isRemodeledProperty(property.getCode())) {
          // Add any qualifiers to the property
          property
              .getQualifiers()
              .addAll(EVSUtils.getQualifiers(property.getCode(), property.getValue(), axioms));
          // add property
          concept.getProperties().add(property);
        }
      }

      concept.setDefinitions(EVSUtils.getDefinitions(terminology, properties, axioms));
      concept.setChildren(childMap.get(conceptCode));
      for (final Concept child : concept.getChildren()) {
        child.setLeaf(hierarchy.getChildNodes(child.getCode(), 1).isEmpty());
      }
      concept.setParents(parentMap.get(conceptCode));
      concept.setAssociations(associationMap.get(conceptCode));
      concept.setInverseAssociations(inverseAssociationMap.get(conceptCode));
      concept.setRoles(roleMap.get(conceptCode));
      // check uris
      if (concept.getRoles().size() == 0) {
        concept.setRoles(roleMap.get(concept.getUri()));
      }
      if (complexRoleMap.containsKey(conceptCode)) {
        concept.getRoles().addAll(complexRoleMap.get(conceptCode));
      }
      // check uris
      concept.setInverseRoles(inverseRoleMap.get(conceptCode));
      if (concept.getInverseRoles().size() == 0) {
        concept.setInverseRoles(inverseRoleMap.get(concept.getUri()));
      }
      if (complexInverseRoleMap.containsKey(conceptCode)) {
        concept.getInverseRoles().addAll(complexInverseRoleMap.get(conceptCode));
      }
      concept.setMaps(EVSUtils.getMapsTo(terminology, axioms));
      concept.setDisjointWith(disjointWithMap.get(conceptCode));
      if (concept.getDisjointWith().size() == 0) {
        // log.info(mapper.writeValueAsString(disjointWithMap.get(concept.getUri())));
        concept.setDisjointWith(disjointWithMap.get(concept.getUri()));
      }
      concept.setPaths(hierarchy.getPaths(terminology, conceptCode));
      concept.setDescendants(hierarchy.getDescendants(conceptCode));
      concept.setLeaf(concept.getChildren().isEmpty());

      // Ensure that all list elements of the concept are in a natural sort
      // order
      concept.sortLists();

      // Free memory in "all roles" maps
      roleMap.remove(concept.getCode());
      inverseRoleMap.remove(concept.getCode());
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<Property> getProperties(final String conceptCode, final Terminology terminology)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("properties", terminology, conceptCode);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Property> properties = new ArrayList<>();

    /*
     * Because the original SPARQL query that filtered out the Annotations was too slow, we will be
     * filtering them out in the post processing.
     */
    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {

      // OLD: skip properties without a code
      // if (b.getPropertyCode() != null &&
      // b.getPropertyCode().getValue().startsWith("A")) {
      // continue;
      // }
      final Property property = new Property();
      property.setCode(EVSUtils.getPropertyCode(b));
      property.setType(EVSUtils.getPropertyLabel(b));
      property.setValue(b.getPropertyValue().getValue());
      properties.add(property);
    }

    return properties;
  }

  /**
   * Returns the properties.
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @return the properties
   * @throws Exception the exception
   */
  private Map<String, List<Property>> getProperties(
      final List<String> conceptCodes, final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery("properties.batch", terminology, conceptCodes);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<Property>> resultMap = new HashMap<>();

    /*
     * Because the original SPARQL query that filtered out the Annotations was too slow, we will be
     * filtering them out in the post processing.
     */
    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final String conceptCode = b.getConceptCode().getValue();
      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }

      final Property property = new Property();
      property.setCode(EVSUtils.getPropertyCode(b));
      property.setType(EVSUtils.getPropertyLabel(b));
      property.setValue(b.getPropertyValue().getValue());
      resultMap.get(conceptCode).add(property);
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public List<Concept> getChildren(final String conceptCode, final Terminology terminology)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("children", terminology, conceptCode);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Concept> children = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Concept child = new Concept();
      child.setCode(EVSUtils.getChildCode(b));
      child.setName(EVSUtils.getChildLabel(b));
      children.add(child);
    }

    return children;
  }

  /**
   * Returns the children.
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @param hierarchy the hierarchy
   * @return the children
   * @throws Exception the exception
   */
  public Map<String, List<Concept>> getChildren(
      final List<String> conceptCodes,
      final Terminology terminology,
      final HierarchyUtils hierarchy)
      throws Exception {

    final Map<String, List<Concept>> resultMap = new HashMap<>();

    for (final String code : conceptCodes) {
      resultMap.put(code, new ArrayList<>());
      for (final HierarchyNode node : hierarchy.getChildNodes(code, 0)) {
        final Concept child = new Concept();
        child.setCode(node.getCode());
        child.setName(node.getLabel());
        resultMap.get(code).add(child);
      }
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public List<Concept> getParents(final String conceptCode, final Terminology terminology)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("parents", terminology, conceptCode);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Concept> parents = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Concept parent = new Concept();
      parent.setCode(EVSUtils.getParentCode(b));
      parent.setName(EVSUtils.getParentLabel(b));
      parents.add(parent);
    }

    return parents;
  }

  /**
   * Returns the parents.
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @param hierarchy the hierarchy
   * @return the parents
   * @throws Exception the exception
   */
  public Map<String, List<Concept>> getParents(
      final List<String> conceptCodes,
      final Terminology terminology,
      final HierarchyUtils hierarchy)
      throws Exception {

    final Map<String, List<Concept>> resultMap = new HashMap<>();

    for (final String code : conceptCodes) {
      resultMap.put(code, new ArrayList<>());
      for (final HierarchyNode node : hierarchy.getParentNodes(code)) {
        final Concept parent = new Concept();
        parent.setCode(node.getCode());
        parent.setName(node.getLabel());
        resultMap.get(code).add(parent);
      }
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public List<Association> getAssociations(final String conceptCode, final Terminology terminology)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructQuery("associations", terminology, conceptCode);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Association> associations = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Association association = new Association();
      association.setCode(EVSUtils.getRelationshipCode(b));
      association.setType(EVSUtils.getRelationshipType(b));
      association.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
      association.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
      associations.add(association);
    }

    return associations;
  }

  /**
   * Returns the associations.
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @return the associations
   * @throws Exception the exception
   */
  public Map<String, List<Association>> getAssociations(
      final List<String> conceptCodes, final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery("associations.batch", terminology, conceptCodes);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<Association>> resultMap = new HashMap<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final String conceptCode =
          b.getConceptCode() != null
              ? b.getConceptCode().getValue()
              : b.getRelatedConcept().getValue();

      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }

      final Association association = new Association();
      association.setCode(EVSUtils.getRelationshipCode(b));
      association.setType(EVSUtils.getRelationshipType(b));

      association.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
      association.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
      resultMap.get(conceptCode).add(association);
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public Map<String, List<Association>> getAssociationsForAllCodes(
      final Terminology terminology, final boolean inverse) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery("associations.all", terminology, new ArrayList<>());
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<Association>> resultMap = new HashMap<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {

      final String conceptCode =
          inverse ? EVSUtils.getRelatedConceptCode(b) : b.getConceptCode().getValue();

      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }

      final Association association = new Association();
      association.setCode(EVSUtils.getRelationshipCode(b));
      association.setType(EVSUtils.getRelationshipType(b));

      if (inverse) {
        association.setRelatedCode(b.getConceptCode().getValue());
        // assumes the query has ?concept or that label is guaranteed to be set
        association.setRelatedName(EVSUtils.getConceptLabel(b));
      } else {
        association.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
        association.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
      }
      resultMap.get(conceptCode).add(association);
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public List<Association> getInverseAssociations(
      final String conceptCode, final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructQuery("inverse.associations", terminology, conceptCode);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Association> associations = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Association association = new Association();
      association.setCode(EVSUtils.getRelationshipCode(b));
      association.setType(EVSUtils.getRelationshipType(b));
      association.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
      association.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
      associations.add(association);
    }

    return associations;
  }

  /**
   * Returns the inverse associations.
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @return the inverse associations
   * @throws Exception the exception
   */
  /* see superclass */
  public Map<String, List<Association>> getInverseAssociations(
      final List<String> conceptCodes, final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery(
            "inverse.associations.batch", terminology, conceptCodes);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<Association>> resultMap = new HashMap<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final String conceptCode =
          b.getConceptCode() != null
              ? b.getConceptCode().getValue()
              : b.getRelatedConcept().getValue();

      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }

      final Association association = new Association();
      association.setCode(EVSUtils.getRelationshipCode(b));
      association.setType(EVSUtils.getRelationshipType(b));
      association.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
      association.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
      resultMap.get(conceptCode).add(association);
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public List<Role> getInverseRoles(final String conceptCode, final Terminology terminology)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructQuery("inverse.roles", terminology, conceptCode);
    // log.info("INVERSE ROLE REPORT QUERY = {}", query);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Role> roles = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    final Set<String> seen = new HashSet<>();
    for (final Bindings b : bindings) {
      // log.info("BINDING = {}", b);
      final Role role = new Role();
      role.setCode(EVSUtils.getRelationshipCode(b));
      role.setType(EVSUtils.getRelationshipType(b));
      role.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
      role.setRelatedName(EVSUtils.getRelatedConceptLabel(b));

      // distinct roles only
      final String key = conceptCode + role.getCode() + role.getRelatedCode();
      if (!seen.contains(key)) {
        roles.add(role);
      }
      seen.add(key);
    }
    // log.info("ROLES = {}", roles);
    return roles;
  }

  /**
   * Returns the inverse roles.
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @return the inverse roles
   * @throws Exception the exception
   */
  /* see superclass */
  public Map<String, List<Role>> getInverseRoles(
      final List<String> conceptCodes, final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery("inverse.roles.batch", terminology, conceptCodes);

    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<Role>> resultMap = new HashMap<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    final Set<String> seen = new HashSet<>();
    for (final Bindings b : bindings) {
      final String conceptCode =
          b.getConceptCode() != null
              ? b.getConceptCode().getValue()
              : b.getRelatedConcept().getValue();

      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }

      final Role role = new Role();
      role.setCode(EVSUtils.getRelationshipCode(b));
      role.setType(EVSUtils.getRelationshipType(b));
      role.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
      role.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
      // distinct roles only
      final String key = conceptCode + role.getCode() + role.getRelatedCode();
      if (!seen.contains(key)) {
        resultMap.get(conceptCode).add(role);
      }
      seen.add(key);
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public List<Role> getRoles(final String conceptCode, final Terminology terminology)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("roles", terminology, conceptCode);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Role> roles = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    final Set<String> seen = new HashSet<>();
    for (final Bindings b : bindings) {
      final Role role = new Role();
      role.setCode(EVSUtils.getRelationshipCode(b));
      role.setType(EVSUtils.getRelationshipType(b));
      role.setRelatedCode(
          b.getRelatedConceptCode() != null
              ? b.getRelatedConceptCode().getValue()
              : b.getRelatedConcept().getValue());
      role.setRelatedName(b.getRelatedConceptLabel().getValue());
      // distinct roles only
      final String key = role.getCode() + role.getRelatedCode();
      if (!seen.contains(key)) {
        roles.add(role);
      }
      seen.add(key);
    }

    return roles;
  }

  /**
   * Returns the roles.
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @return the roles
   * @throws Exception the exception
   */
  public Map<String, List<Role>> getRoles(
      final List<String> conceptCodes, final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery("roles.batch", terminology, conceptCodes);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<Role>> resultMap = new HashMap<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    final Set<String> seen = new HashSet<>();
    for (final Bindings b : bindings) {
      final String conceptCode =
          b.getConceptCode() != null
              ? b.getConceptCode().getValue()
              : b.getRelatedConcept().getValue();

      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }

      final Role role = new Role();
      role.setCode(EVSUtils.getRelationshipCode(b));
      role.setType(EVSUtils.getRelationshipType(b));
      role.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
      role.setRelatedName(EVSUtils.getRelatedConceptLabel(b));

      // distinct roles only
      final String key = conceptCode + role.getCode() + role.getRelatedCode();
      if (!seen.contains(key)) {
        resultMap.get(conceptCode).add(role);
      }
      seen.add(key);
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public Map<String, List<Role>> getRolesForAllCodes(
      final Terminology terminology, boolean inverseFlag) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery("roles.all", terminology, new ArrayList<>());
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<Role>> resultMap =
        this.getComplexRolesForAllCodes(terminology, inverseFlag);

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    final Set<String> seen = new HashSet<>();
    for (final Bindings b : bindings) {

      final String conceptCode =
          inverseFlag ? b.getRelatedConceptCode().getValue() : b.getConceptCode().getValue();

      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }

      final Role role = new Role();
      role.setCode(EVSUtils.getRelationshipCode(b));
      role.setType(EVSUtils.getRelationshipType(b));
      if (inverseFlag) {
        // reverse code and related code
        role.setRelatedCode(b.getConceptCode().getValue());
        role.setRelatedName(EVSUtils.getConceptLabel(b));
        // distinct roles only
        final String key = conceptCode + role.getCode() + role.getRelatedCode();
        if (!seen.contains(key)) {
          resultMap.get(conceptCode).add(role);
        }
        seen.add(key);
      } else {
        role.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
        role.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
        // distinct roles only
        final String key = conceptCode + role.getCode() + role.getRelatedCode();
        if (!seen.contains(key)) {
          resultMap.get(conceptCode).add(role);
        }
        seen.add(key);
      }
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public Map<String, List<Role>> getComplexRolesForAllCodes(
      final Terminology terminology, boolean inverseFlag) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery(
            "roles.all.complex", terminology, new ArrayList<>());

    // escape hatch for terminologies without complex roles
    if (query.equals("SKIP")) {
      return new HashMap<>();
    }
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<Role>> resultMap = new HashMap<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    final Set<String> seen = new HashSet<>();
    for (final Bindings b : bindings) {

      final String conceptCode =
          inverseFlag ? b.getRelatedConceptCode().getValue() : b.getConceptCode().getValue();

      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }

      final Role role = new Role();
      role.setCode(EVSUtils.getRelationshipCode(b));
      role.setType(EVSUtils.getRelationshipType(b));
      if (inverseFlag) {
        // reverse code and related code
        role.setRelatedCode(b.getConceptCode().getValue());
        role.setRelatedName(EVSUtils.getConceptLabel(b));
        // distinct roles only
        final String key = conceptCode + role.getCode() + role.getRelatedCode();
        if (!seen.contains(key)) {
          resultMap.get(conceptCode).add(role);
        }
        seen.add(key);
      } else {
        role.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
        role.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
        // distinct roles only
        final String key = conceptCode + role.getCode() + role.getRelatedCode();
        if (!seen.contains(key)) {
          resultMap.get(conceptCode).add(role);
        }
        seen.add(key);
      }
    }

    return resultMap;
  }

  /* see superclass */
  @Override
  public List<DisjointWith> getDisjointWith(final String conceptCode, final Terminology terminology)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructQuery("disjoint.with", terminology, conceptCode);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<DisjointWith> disjointWithList = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final DisjointWith disjointWith = new DisjointWith();
      disjointWith.setType("disjointWith");
      disjointWith.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
      disjointWith.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
      disjointWithList.add(disjointWith);
    }

    return disjointWithList;
  }

  /**
   * Returns the disjoint with.
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @return the disjoint with
   * @throws Exception the exception
   */
  public Map<String, List<DisjointWith>> getDisjointWith(
      final List<String> conceptCodes, final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery("disjoint.with.batch", terminology, conceptCodes);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<DisjointWith>> resultMap = new HashMap<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final String conceptCode = b.getConceptCode().getValue();

      if (resultMap.get(conceptCode) == null) {
        resultMap.put(conceptCode, new ArrayList<>());
      }

      final DisjointWith disjointWith = new DisjointWith();
      disjointWith.setType("disjointWith");
      disjointWith.setRelatedCode(EVSUtils.getRelatedConceptCode(b));
      if (disjointWith.getRelatedCode().contains("anon-genid")) { // skip complex roles
        continue;
      }
      disjointWith.setRelatedName(EVSUtils.getRelatedConceptLabel(b));
      resultMap.get(conceptCode).add(disjointWith);
    }
    // log.info("RESULT MAP: {}", mapper.writeValueAsString(resultMap));
    return resultMap;
  }

  /* see superclass */
  @Override
  public List<Axiom> getAxioms(
      final String conceptCode, final Terminology terminology, final boolean qualifierFlag)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("axioms", terminology, conceptCode);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Axiom> axioms = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    if (bindings.length == 0) {
      return axioms;
    }
    Axiom axiomObject = new Axiom();
    String oldAxiom = null;
    for (final Bindings b : bindings) {
      final String axiom = b.getAxiom().getValue();
      final String propertyUri = b.getAxiomProperty().getValue();
      final String propertyCode = EVSUtils.getQualifiedCodeFromUri(propertyUri);
      final String value = b.getAxiomValue().getValue();
      // log.debug(" axiom = " + propertyUri + ", " + value + ", " + axiom);

      if (oldAxiom != null && !axiom.equals(oldAxiom)) {
        axioms.add(axiomObject);
        // log.debug(" ADD Axiom = " + axiomObject);
        axiomObject = new Axiom();
      }
      oldAxiom = axiom;

      setAxiomProperty(propertyCode, propertyUri, value, qualifierFlag, axiomObject, terminology);
    }
    // log.debug(" ADD Axiom = " + axiomObject);
    axioms.add(axiomObject);
    return axioms;
  }

  /**
   * Returns the axioms grouped by concept.
   *
   * @param conceptCodes the list of concept codes
   * @param terminology the terminology
   * @param qualifierFlag the qualifier flag
   * @return the axioms
   * @throws Exception the exception
   */
  private Map<String, List<Axiom>> getAxioms(
      final List<String> conceptCodes, final Terminology terminology, final boolean qualifierFlag)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructBatchQuery("axioms.batch", terminology, conceptCodes);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
    // log.info("AXIOM QUERY: {}", queryPrefix + query);
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final Map<String, List<Axiom>> resultMap = new HashMap<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    if (bindings.length == 0) {
      return Collections.<String, List<Axiom>>emptyMap();
    }

    String conceptCode = "";

    final Map<String, List<Bindings>> bindingsMap = new HashMap<>();
    boolean hasConceptCode = false;
    for (final Bindings b : bindings) {
      hasConceptCode = b.getConceptCode() != null;
      conceptCode =
          b.getConceptCode() != null ? b.getConceptCode().getValue() : b.getAxiom().getValue();
      if (bindingsMap.get(conceptCode) == null) {
        bindingsMap.put(conceptCode, new ArrayList<>());
      }

      bindingsMap.get(conceptCode).add(b);
    }

    for (final String code : bindingsMap.keySet()) {
      // log.info("CODE: {}", code);
      final List<Bindings> bindingsList = bindingsMap.get(code);
      // log.info("bindingsList: {}", bindingsList);
      final Map<String, Axiom> axiomMap = new HashMap<>();
      for (final Bindings b : bindingsList) {
        final String axiom = b.getAxiom().getValue();
        // Create the axiom the first time it's found, later instances of it
        // will just build/add more info
        if (!axiomMap.containsKey(axiom)) {
          axiomMap.put(axiom, new Axiom());
        }
        final Axiom axiomObject = axiomMap.get(axiom);
        final String propertyUri = b.getAxiomProperty().getValue();
        final String propertyCode = EVSUtils.getQualifiedCodeFromUri(propertyUri);
        final String value = b.getAxiomValue().getValue();

        setAxiomProperty(propertyCode, propertyUri, value, qualifierFlag, axiomObject, terminology);
      }
      for (final Axiom axiom : axiomMap.values()) {
        String realCode =
            hasConceptCode ? code : EVSUtils.getCodeFromUri(axiom.getAnnotatedSource());

        if (resultMap.get(realCode) == null) {
          resultMap.put(realCode, new ArrayList<>());
        }
        // log.debug(" ADD Axiom = " + axiom);
        resultMap.get(realCode).add(axiom);
      }
    }
    return resultMap;
  }

  /**
   * sets axiom property.
   *
   * @param propertyCode the property
   * @param propertyUri the property uri
   * @param value the value
   * @param qualifierFlag the qualifier flag
   * @param axiomObject the axiom object
   * @param terminology the terminology
   * @throws Exception the exception
   */
  private void setAxiomProperty(
      final String propertyCode,
      final String propertyUri,
      final String value,
      final boolean qualifierFlag,
      final Axiom axiomObject,
      final Terminology terminology)
      throws Exception {

    // Look at the qualified code form of the property for the switch
    // log.info("PROPERTY CODE: {}", propertyCode);
    switch (propertyCode) {
      case "owl:annotatedSource":
        // This is used when concepts don't have real codes
        axiomObject.setAnnotatedSource(value);
        break;
      case "owl:annotatedTarget":
        // use the actual value
        axiomObject.setAnnotatedTarget(value);
        // log.debug(" annotated target = " + value);
        break;
      case "owl:annotatedProperty":
        // Use the code value
        axiomObject.setAnnotatedProperty(EVSUtils.getQualifiedCodeFromUri(value));
        // log.debug(" annotated property = " + EVSUtils.getQualifiedCodeFromUri(value));
        break;
      default:

        // For "hasDbXref" if there is a url, just let that url be the value
        // this was done to handle situations where GO had blank "database_cross_reference"
        // qualifiers.
        final String labelValue =
            propertyCode.equals("oboInOwl:hasDbXref") ? value : EVSUtils.getLabelFromUri(value);

        // Skip the "type property
        if (propertyCode.contains("rdf-syntax-ns") && propertyCode.contains("type")) {
          return;
        }
        if (propertyCode.equals(terminology.getMetadata().getRelationshipToTarget())) {
          axiomObject.setRelationshipToTarget(labelValue);
        } else if (propertyCode.equals(terminology.getMetadata().getMapTarget())) {
          axiomObject.setTargetCode(labelValue);
        } else if (propertyCode.equals(terminology.getMetadata().getMapTargetTermType())) {
          axiomObject.setTargetTermType(labelValue);
        } else if (propertyCode.equals(terminology.getMetadata().getMapTargetTerminology())) {
          axiomObject.setTargetTerminology(labelValue);
        } else if (propertyCode.equals(
            terminology.getMetadata().getMapTargetTerminologyVersion())) {
          axiomObject.setTargetTerminologyVersion(labelValue);
        } else if (propertyCode.equals(terminology.getMetadata().getDefinitionSource())) {
          axiomObject.setDefSource(labelValue);
        } else if (propertyCode.equals(terminology.getMetadata().getSynonymCode())) {
          axiomObject.setSourceCode(labelValue);
          // log.debug(" sy code = " + labelValue);
        } else if (propertyCode.equals(terminology.getMetadata().getSynonymSubSource())) {
          axiomObject.setSubsourceName(labelValue);
          // log.debug(" sy subsource = " + labelValue);
        } else if (propertyCode.equals(terminology.getMetadata().getSynonymTermType())) {
          axiomObject.setTermType(labelValue);
          // log.debug(" sy termType = " + labelValue);
        } else if (propertyCode.equals(terminology.getMetadata().getSynonymSource())) {
          axiomObject.setTermSource(labelValue);
          // log.debug(" sy source = " + labelValue);
        } else if (qualifierFlag) {
          // Here check the qualified form as well as the URI

          final String name =
              EVSUtils.getQualifierName(
                  sparqlQueryCacheService.getAllQualifiers(
                      terminology, new IncludeParam("minimal"), restUtils, this),
                  propertyCode,
                  propertyUri);
          if (name != null) {
            axiomObject.getQualifiers().add(new Qualifier(name, labelValue));
          }
          // log.debug(" qualifier = " + name + ", " + labelValue + ", " +
          // propertyCode);
        } else {
          log.info("Unexpected property code = {}", propertyCode);
        }
        break;
    }
  }

  /**
   * Returns the main type hierarchy.
   *
   * @param terminology the terminology
   * @param mainTypeSet the main type set
   * @param broadCategorySet the broad category set
   * @param hierarchy the hierarchy
   * @return the main type hierarchy
   * @throws Exception the exception
   */
  @Override
  public Map<String, Paths> getMainTypeHierarchy(
      final Terminology terminology,
      final Set<String> mainTypeSet,
      final Set<String> broadCategorySet,
      final HierarchyUtils hierarchy)
      throws Exception {

    final Set<String> combined = Sets.union(mainTypeSet, broadCategorySet);
    // For each mainTypeSet concept find "shortest paths" to root
    final Map<String, Paths> map = new HashMap<>();
    for (final Map.Entry<String, Paths> entry :
        hierarchy.getPathsMap(terminology, new ArrayList<>(combined)).entrySet()) {
      final String code = entry.getKey();
      final Paths paths = entry.getValue();

      // Determine if paths go through C2991 "Diseases and Disorders"
      final boolean diseaseFlag =
          paths.getPaths().stream()
                  .flatMap(p -> p.getConcepts().stream())
                  .filter(c -> c.getCode().equals("C2991"))
                  .count()
              > 0;
      if (!diseaseFlag) {
        log.debug("  SKIP Main type hierarchy = " + code);
        // Leave the return value empty for this code
        continue;
      }

      log.debug("  Main type hierarchy = " + code);
      // for (final Path path : paths.getPaths()) {
      // log.debug(" path = "
      // + path.getConcepts().stream().map(c ->
      // c.getCode()).collect(Collectors.toList()));
      // }
      // Rewrite paths
      final Paths rewritePaths = new Paths();
      rewritePaths.setPaths(
          paths.getPaths().stream()
              .map(p -> p.rewritePath(combined, true))
              .collect(Collectors.toList()));
      // for (final Path path : rewritePaths.getPaths()) {
      // log.debug(" rewrite = "
      // + path.getConcepts().stream().map(c ->
      // c.getCode()).collect(Collectors.toList()));
      // }

      // Remove all but the longest paths
      final Paths longestPaths = new Paths();
      final int longest =
          rewritePaths.getPaths().stream()
              .map(p -> p.getConcepts().size())
              .max(Integer::compare)
              .get();
      // log.debug(" longest = " + longest);
      final Set<String> seen = new HashSet<>();
      longestPaths.setPaths(
          rewritePaths.getPaths().stream()
              .filter(p -> !seen.contains(p.getConcepts().toString()))
              .filter(p -> p.getConcepts().size() == longest)
              .peek(p -> seen.add(p.getConcepts().toString()))
              .collect(Collectors.toList()));
      for (final Path path : longestPaths.getPaths()) {
        log.debug(
            "    longest = "
                + path.getConcepts().stream().map(c -> c.getCode()).collect(Collectors.toList()));
      }
      // Save the pre-trimmed paths, this is the full hierarchy
      // Copy longest paths because the next section is going to trim them.
      map.put(code + "-FULL", new Paths(longestPaths));

      // Trim paths to remove broad category concepts if there are main type
      // concepts
      final Paths trimmedPaths = new Paths();
      for (final Path path : longestPaths.getPaths()) {

        // If the path contains main type concepts, remove broad category
        // concepts
        if (path.getConcepts().stream().filter(c -> mainTypeSet.contains(c.getCode())).count()
            > 0) {
          path.getConcepts().removeIf(c -> broadCategorySet.contains(c.getCode()));
        }
        // If there is more than one broad category concept, keep only the first
        if (path.getConcepts().stream().filter(c -> broadCategorySet.contains(c.getCode())).count()
            > 1) {
          path.getConcepts()
              .removeIf(c -> !c.getCode().equals(path.getConcepts().get(0).getCode()));
        }

        trimmedPaths.getPaths().add(path);
      }
      for (final Path path : trimmedPaths.getPaths()) {
        log.debug(
            "    trimmed = "
                + map.get(code + "-FULL").getPaths().get(0).getConcepts().size()
                + ", "
                + path.getConcepts().stream().map(c -> c.getCode()).collect(Collectors.toList()));
      }

      // Save the trimmed paths (this is where mma comes from)
      map.put(code, trimmedPaths);
    }

    return map;
  }

  /* see superclass */
  @Override
  public List<Concept> getAllProperties(final Terminology terminology, final IncludeParam ip)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("all.properties", terminology);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Property> properties = new ArrayList<>();
    final List<Concept> concepts = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Property property = new Property();
      // Add the "about" if there is no explicit property code
      if (b.getPropertyCode() == null) {
        property.setUri(b.getProperty().getValue());
      }
      property.setCode(EVSUtils.getPropertyCode(b));
      properties.add(property);
    }

    // BAC: remove this - something can be a qualifier AND a property
    // // Get all qualifier codes (in case there is overlap with properties)
    // final String query2 =
    // queryBuilderService.constructQuery("all.qualifiers", terminology);
    // final String res2 = restUtils.runSPARQL(queryPrefix + query2,
    // getQueryURL());
    // final Set<String> qualifiers = new HashSet<>();
    // final Sparql sparqlResult2 = mapper.readValue(res2, Sparql.class);
    // final Bindings[] bindings2 = sparqlResult2.getResults().getBindings();
    // for (final Bindings b : bindings2) {
    // // This query just looks up the codes
    // qualifiers.add(b.getProperty().getValue());
    // qualifiers.add(EVSUtils.getPropertyCode(b));
    // }

    // // Get all "properties never used"
    // final String query3 =
    // queryBuilderService.constructQuery("all.properties.never.used",
    // terminology);
    // final String res3 = restUtils.runSPARQL(queryPrefix + query3,
    // getQueryURL());
    // final Set<String> neverUsed = new HashSet<>();
    // final Sparql sparqlResult3 = mapper.readValue(res3, Sparql.class);
    // final Bindings[] bindings3 = sparqlResult3.getResults().getBindings();
    // for (final Bindings b : bindings3) {
    // neverUsed.add(b.getProperty().getValue());
    // neverUsed.add(EVSUtils.getPropertyCode(b));
    // }

    final TerminologyMetadata md = terminology.getMetadata();
    for (final Property property : properties) {

      // Send URI or code
      final Concept concept =
          getProperty(
              property.getUri() != null ? property.getUri() : property.getCode(), terminology, ip);
      if (md.isRemodeledProperty(property.getCode()) || md.isRemodeledProperty(property.getUri())) {
        concept.getProperties().add(new Property("remodeled", "true"));
        if (property.getCode() != null) {
          concept
              .getProperties()
              .add(
                  new Property(
                      "remodeledDescription",
                      "Remodeled as " + md.getRemodeledAsType(property, null, md)));
        }
      }
      concepts.add(concept);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<Concept> getRemodeledProperties(final Terminology terminology, final IncludeParam ip)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("all.properties", terminology);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Property> properties = new ArrayList<>();
    final List<Concept> concepts = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Property property = new Property();
      // Add the "about" if there is no explicit property code
      if (b.getPropertyCode() == null) {
        property.setUri(b.getProperty().getValue());
      }
      property.setCode(EVSUtils.getPropertyCode(b));
      properties.add(property);
    }

    final TerminologyMetadata md = terminology.getMetadata();
    for (final Property property : properties) {
      // Exclude properties that are redefined as synonyms or definitions
      // any qualifiers, and also properties defined in the OWL but never used
      if (md.isRemodeledProperty(property.getCode()) || md.isRemodeledProperty(property.getUri())) {

        // Send URI or code
        final Concept concept =
            getProperty(
                property.getUri() != null ? property.getUri() : property.getCode(),
                terminology,
                ip);
        concepts.add(concept);
      }
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<Concept> getNeverUsedProperties(final Terminology terminology, final IncludeParam ip)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructQuery("all.properties.never.used", terminology);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Property> properties = new ArrayList<>();
    final List<Concept> concepts = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Property property = new Property();
      // Add the "about" if there is no explicit property code
      if (b.getPropertyCode() == null) {
        property.setUri(b.getProperty().getValue());
      }
      property.setCode(EVSUtils.getPropertyCode(b));
      properties.add(property);
    }

    // Get all qualifier codes (in case there is overlap with properties)
    final String query2 = queryBuilderService.constructQuery("all.qualifiers", terminology);
    final String res2 = restUtils.runSPARQL(queryPrefix + query2, getQueryURL());
    final Set<String> qualifiers = new HashSet<>();
    final Sparql sparqlResult2 = mapper.readValue(res2, Sparql.class);
    final Bindings[] bindings2 = sparqlResult2.getResults().getBindings();
    for (final Bindings b : bindings2) {
      // This query just looks up the codes
      qualifiers.add(b.getProperty().getValue());
      qualifiers.add(EVSUtils.getPropertyCode(b));
    }

    final TerminologyMetadata md = terminology.getMetadata();
    for (final Property property : properties) {
      // Exclude properties that are redefined as synonyms or definitions
      // any qualifiers, and also properties defined in the OWL but never used
      if (md.isRemodeledProperty(property.getCode())
          || md.isRemodeledProperty(property.getUri())
          || qualifiers.contains(property.getCode())
          || qualifiers.contains(property.getUri())) {
        continue;
      }

      // Send URI or code
      final Concept concept =
          getProperty(
              property.getUri() != null ? property.getUri() : property.getCode(), terminology, ip);
      concepts.add(concept);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<String> getDistinctPropertyValues(
      final Terminology terminology, final String propertyCode) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final Map<String, String> values =
        ConceptUtils.asMap("propertyCode", propertyCode, "namedGraph", terminology.getGraph());
    final String query =
        queryBuilderService.constructQuery("distinct.property.values", terminology, values);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<String> propertyValues = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final String propertyValue = b.getPropertyValue().getValue();
      propertyValues.add(propertyValue);
    }

    return propertyValues;
  }

  /* see superclass */
  @Override
  public List<Concept> getRemodeledQualifiers(final Terminology terminology, final IncludeParam ip)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("all.qualifiers", terminology);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Qualifier> qualifiers = new ArrayList<>();
    final List<Concept> concepts = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Qualifier qualifier = new Qualifier();
      if (b.getPropertyCode() == null) {
        qualifier.setUri(b.getProperty().getValue());
      }
      qualifier.setCode(EVSUtils.getPropertyCode(b));
      qualifiers.add(qualifier);
    }

    final TerminologyMetadata md = terminology.getMetadata();
    for (final Qualifier qualifier : qualifiers) {
      // Exclude properties that are redefined as synonyms or definitions
      if (md.isRemodeledQualifier(qualifier.getCode())
          || md.isRemodeledQualifier(qualifier.getUri())
          || md.isUnpublished(qualifier.getCode())
          || md.isUnpublished(qualifier.getUri())) {
        // Send URI or code
        final Concept concept =
            getQualifier(
                qualifier.getUri() != null ? qualifier.getUri() : qualifier.getCode(),
                terminology,
                ip);
        concepts.add(concept);
      }
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<String> getQualifierValues(final String propertyCode, final Terminology terminology)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final Map<String, String> values =
        ConceptUtils.asMap("propertyCode", propertyCode, "namedGraph", terminology.getGraph());
    final String query = queryBuilderService.constructQuery("axiom.qualifier", terminology, values);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<String> propertyValues = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final String propertyValue = b.getPropertyValue().getValue();
      propertyValues.add(propertyValue);
    }

    return propertyValues;
  }

  /**
   * Returns the subset members.
   *
   * @param subsetCode the subset code
   * @param terminology the terminology
   * @return the subset members
   * @throws Exception the exception
   */
  @Override
  public List<Concept> getSubsetMembers(final String subsetCode, final Terminology terminology)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final Map<String, String> values =
        ConceptUtils.asMap(
            "codeCode",
            terminology.getMetadata().getCode(),
            "conceptCode",
            subsetCode,
            "namedGraph",
            terminology.getGraph(),
            "preferredNameCode",
            terminology.getMetadata().getPreferredName());
    final String query = queryBuilderService.constructQuery("subset", terminology, values);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Concept> subsetMembers = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final String code = b.getConceptCode().getValue();
      final String name = b.getConceptLabel().getValue();
      subsetMembers.add(new Concept(terminology.getTerminology(), code, name));
    }

    return subsetMembers;
  }

  /* see superclass */
  @Override
  public List<Concept> getAllAssociations(final Terminology terminology, final IncludeParam ip)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("all.associations", terminology);

    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Association> associations = new ArrayList<>();
    final List<Concept> concepts = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Association association = new Association();
      if (b.getPropertyCode() == null) {
        association.setUri(b.getProperty().getValue());
      }
      association.setCode(EVSUtils.getPropertyCode(b));
      associations.add(association);
    }

    for (final Association association : associations) {

      // Send URI or code
      final Concept concept =
          getAssociation(
              association.getUri() != null ? association.getUri() : association.getCode(),
              terminology,
              ip);
      concepts.add(concept);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<Concept> getAllRoles(final Terminology terminology, final IncludeParam ip)
      throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query = queryBuilderService.constructQuery("all.roles", terminology);
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<Role> roles = new ArrayList<>();
    final List<Concept> concepts = new ArrayList<>();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Role role = new Role();
      if (b.getPropertyCode() == null) {
        role.setUri(b.getProperty().getValue());
      }
      role.setCode(EVSUtils.getPropertyCode(b));
      roles.add(role);
    }

    for (final Role role : roles) {

      // Send URI or code
      final Concept concept =
          getRole(role.getUri() != null ? role.getUri() : role.getCode(), terminology, ip);
      concepts.add(concept);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<Concept> getAllSynonymTypes(final Terminology terminology, final IncludeParam ip)
      throws Exception {

    final List<Concept> properties = this.getRemodeledProperties(terminology, ip);
    final List<Concept> concepts = new ArrayList<>();
    final TerminologyMetadata md = terminology.getMetadata();
    for (final Concept property : properties) {
      if (md.getSynonym().contains(property.getCode())) {
        concepts.add(property);
      }
    }
    return concepts;
  }

  /* see superclass */
  @Override
  public List<Concept> getAllDefinitionTypes(final Terminology terminology, final IncludeParam ip)
      throws Exception {

    final List<Concept> properties = this.getRemodeledProperties(terminology, ip);
    final List<Concept> concepts = new ArrayList<>();
    final TerminologyMetadata md = terminology.getMetadata();
    for (final Concept property : properties) {
      if (md.getDefinition().contains(property.getCode())) {
        concepts.add(property);
      }
    }
    return concepts;
  }

  /* see superclass */
  @Override
  public List<HierarchyNode> getRootNodes(final Terminology terminology) throws Exception {
    return sparqlQueryCacheService.getHierarchyUtils(terminology, restUtils, this).getRootNodes();
  }

  /* see superclass */
  @Override
  public List<HierarchyNode> getChildNodes(final String parent, final Terminology terminology)
      throws Exception {
    return sparqlQueryCacheService
        .getHierarchyUtils(terminology, restUtils, this)
        .getChildNodes(parent, 0);
  }

  /* see superclass */
  @Override
  public List<HierarchyNode> getChildNodes(
      final String parent, final int maxLevel, final Terminology terminology) throws Exception {
    return sparqlQueryCacheService
        .getHierarchyUtils(terminology, restUtils, this)
        .getChildNodes(parent, maxLevel);
  }

  /* see superclass */
  @Override
  public List<String> getAllChildNodes(final String parent, final Terminology terminology)
      throws Exception {
    return sparqlQueryCacheService
        .getHierarchyUtils(terminology, restUtils, this)
        .getAllChildNodes(parent);
  }

  /* see superclass */
  @Override
  public void checkPathInHierarchy(
      final String code, final HierarchyNode node, final Path path, final Terminology terminology)
      throws Exception {

    // check for empty path
    if (path.getConcepts().size() == 0) {
      return;
    }

    // get path length
    final int end = path.getConcepts().size() - 1;

    // find the end (in this case top) of the path
    final ConceptMinimal concept = path.getConcepts().get(end);
    final List<HierarchyNode> children = getChildNodes(node.getCode(), 1, terminology);

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
      for (final HierarchyNode childNode : node.getChildren()) {
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
  public List<HierarchyNode> getPathInHierarchy(final String code, final Terminology terminology)
      throws Exception {
    final List<HierarchyNode> rootNodes = elasticQueryService.getRootNodesHierarchy(terminology);
    final Paths paths =
        sparqlQueryCacheService
            .getHierarchyUtils(terminology, restUtils, this)
            .getPaths(terminology, code);

    for (final HierarchyNode rootNode : rootNodes) {
      for (final Path path : paths.getPaths()) {
        checkPathInHierarchy(code, rootNode, path, terminology);
      }
    }

    return rootNodes;
  }

  /* see superclass */
  @Override
  public List<ConceptMinimal> getSynonymSources(final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);

    if (terminology.getMetadata().getSynonymSource() == null) {
      return new ArrayList<>(0);
    }

    final String query =
        queryBuilderService.constructQuery(
            "axiom.qualifier.values",
            terminology,
            ConceptUtils.asMap(
                "namedGraph",
                terminology.getGraph(),
                "propertyCode",
                terminology.getMetadata().getSynonymSource(),
                "conceptStatusCode",
                terminology.getMetadata().getConceptStatus(),
                "retiredStatusValue",
                terminology.getMetadata().getRetiredStatusValue()));
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<ConceptMinimal> sources = new ArrayList<>();
    final Map<String, String> map = terminology.getMetadata().getSources();
    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Concept concept = new Concept();
      concept.setTerminology(terminology.getTerminology());
      concept.setCode(b.getPropertyValue().getValue());
      if (map.containsKey(concept.getCode())) {
        concept.setName(map.get(concept.getCode()));
      } else if (concept.getCode().contains(" ")
          && map.containsKey(concept.getCode().substring(0, concept.getCode().indexOf(" ")))) {
        concept.setName(
            map.get(concept.getCode().substring(0, concept.getCode().indexOf(" ")))
                + ", version "
                + concept.getCode().substring(concept.getCode().indexOf(" ") + 1));
      }

      sources.add(concept);
    }

    return sources;
  }

  /* see superclass */
  @Override
  public List<ConceptMinimal> getTermTypes(final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructQuery(
            "axiom.qualifier.values",
            terminology,
            ConceptUtils.asMap(
                "namedGraph",
                terminology.getGraph(),
                "conceptCode",
                // Temporary legacy patch for termType/Type
                // Can be removed after 1.6.0 is deployed
                terminology.getMetadata().getSynonymTermType() != null
                    ? terminology.getMetadata().getSynonymTermType()
                    : "P383",
                "conceptStatusCode",
                terminology.getMetadata().getConceptStatus(),
                "retiredStatusValue",
                terminology.getMetadata().getRetiredStatusValue()));
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<ConceptMinimal> sources = new ArrayList<>();
    final Map<String, String> map = terminology.getMetadata().getTermTypes();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Concept concept = new Concept();
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
  public List<ConceptMinimal> getDefinitionSources(final Terminology terminology) throws Exception {
    if (terminology.getMetadata().getDefinitionSource() == null) {
      return new ArrayList<>(0);
    }

    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructQuery(
            "axiom.qualifier.values",
            terminology,
            ConceptUtils.asMap(
                "namedGraph",
                terminology.getGraph(),
                "propertyCode",
                terminology.getMetadata().getDefinitionSource(),
                "conceptStatusCode",
                terminology.getMetadata().getConceptStatus(),
                "retiredStatusValue",
                terminology.getMetadata().getRetiredStatusValue()));
    final String res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<ConceptMinimal> sources = new ArrayList<>();
    // Documentation on source definitions
    final Map<String, String> map = terminology.getMetadata().getSources();

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final Concept concept = new Concept();
      concept.setTerminology(terminology.getTerminology());
      concept.setCode(b.getPropertyValue().getValue());
      if (map.containsKey(concept.getCode())) {
        concept.setName(map.get(concept.getCode()));
      } else if (concept.getCode().contains(" ")
          && map.containsKey(concept.getCode().substring(0, concept.getCode().indexOf(" ")))) {
        concept.setName(
            map.get(concept.getCode().substring(0, concept.getCode().indexOf(" ")))
                + ", version "
                + concept.getCode().substring(concept.getCode().indexOf(" ") + 1));
      }
      sources.add(concept);
    }

    return sources;
  }

  /* see superclass */
  @Override
  public List<Concept> getAllConceptsWithCode(final Terminology terminology) throws IOException {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    log.debug("query prefix = {}", queryPrefix);
    final String query = queryBuilderService.constructQuery("all.concepts.with.code", terminology);
    log.debug("query = {}", query);
    log.debug("query url = {}", getQueryURL());
    String res = null;
    try {
      res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
    } catch (Exception e) {
      e.printStackTrace();
    }

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();

    final List<Concept> concepts = new ArrayList<>();
    for (final Bindings b : bindings) {

      // Skip anything without a concept code (it's not a class we care about)
      if (b.getConceptCode() == null) {
        throw new IOException("This should not happen, it is 'with code' only");
      }
      final Concept c = new Concept();
      // Code is guaranteed to be set here
      c.setCode(b.getConceptCode().getValue());
      c.setTerminology(terminology.getTerminology());
      c.setVersion(terminology.getVersion());
      // Use label if found, or compute from rdf:about otherwise
      c.setName(EVSUtils.getConceptLabel(b));
      c.setActive(true);
      concepts.add(c);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<Concept> getAllConceptsWithoutCode(final Terminology terminology) throws Exception {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    log.debug("query prefix = {}", queryPrefix);
    final String query =
        queryBuilderService.constructQuery("all.concepts.without.code", terminology);
    log.debug("query = {}", query);
    log.debug("query url = {}", getQueryURL());
    String res = null;
    try {
      res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
    } catch (Exception e) {
      e.printStackTrace();
    }

    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    final Sparql sparqlResult = mapper.readValue(res, Sparql.class);
    final Bindings[] bindings = sparqlResult.getResults().getBindings();

    final List<Concept> concepts = new ArrayList<>();
    for (final Bindings b : bindings) {

      final Concept c = new Concept();
      c.setUri(b.getConcept().getValue());
      c.setCode(EVSUtils.getCodeFromUri(b.getConcept().getValue()));
      c.setTerminology(terminology.getTerminology());
      c.setVersion(terminology.getVersion());
      // Use label if found, or compute from rdf:about otherwise
      c.setName(EVSUtils.getConceptLabel(b));
      c.setActive(true);
      concepts.add(c);
    }

    return concepts;
  }

  /* see superclass */
  @Override
  public List<Concept> getAllSubsets(final Terminology terminology) throws Exception {
    final List<Concept> subsets = new ArrayList<>();
    for (final String code : terminology.getMetadata().getSubset()) {
      final Concept concept =
          getConcept(code, terminology, new IncludeParam("minimal,children,properties"));

      getSubsetsHelper(concept, terminology, 0);
      subsets.add(concept);
    }
    return subsets.stream().flatMap(c -> c.getChildren().stream()).collect(Collectors.toList());
  }

  /**
   * Returns the subsets helper.
   *
   * @param concept the concept
   * @param terminology the terminology
   * @param level the level
   * @return the subsets helper
   * @throws Exception the exception
   */
  private void getSubsetsHelper(
      final Concept concept, final Terminology terminology, final int level) throws Exception {
    final List<Concept> children = new ArrayList<>();
    for (final Concept child : concept.getChildren()) {
      final Concept childFull =
          getConcept(child.getCode(), terminology, new IncludeParam("minimal,children,properties"));
      boolean valInSubset = false;
      boolean found = false;
      for (final Property prop : childFull.getProperties()) {
        if (prop.getType().equals("Publish_Value_Set")) {
          found = true;
          if (prop.getValue().equals("Yes")) {
            valInSubset = true;
            break;
          }
        }
      }
      // for terminologies without 'Publish_Value_Set' properties, show them also (future looking)
      if (found && !valInSubset) {
        continue;
      }
      childFull.setProperties(null);
      children.add(childFull);
      getSubsetsHelper(childFull, terminology, level + 1);
    }
    concept.setChildren(children);
  }

  /* see superclass */
  @Override
  public List<AssociationEntry> getAssociationEntries(
      final Terminology terminology, final Concept association) {
    final String queryPrefix = queryBuilderService.constructPrefix(terminology);
    final String query =
        queryBuilderService.constructQuery(
            "association.entries", terminology, association.getCode());
    String res = null;
    try {
      res = restUtils.runSPARQL(queryPrefix + query, getQueryURL());
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    final ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final List<AssociationEntry> entries = new ArrayList<>();
    Sparql sparqlResult = null;
    try {
      sparqlResult = mapper.readValue(res, Sparql.class);
    } catch (final Exception e) {
      log.error("Mapper could not read value in Association Entries");
      e.printStackTrace();
    }
    final Bindings[] bindings = sparqlResult.getResults().getBindings();
    for (final Bindings b : bindings) {
      final AssociationEntry entry = new AssociationEntry();
      entry.setTerminology(terminology.getTerminology());
      entry.setVersion(terminology.getVersion());
      entry.setAssociation(association.getName());
      entry.setCode(b.getConceptCode().getValue());
      entry.setName(b.getConceptLabel().getValue());
      entry.setRelatedCode(b.getRelatedConceptCode().getValue());
      entry.setRelatedName(b.getRelatedConceptLabel().getValue());
      entries.add(entry);
    }
    return entries;
  }

  /* see superclass */
  public HierarchyUtils getHierarchyUtilsCache(final Terminology terminology) throws Exception {
    return sparqlQueryCacheService.getHierarchyUtils(terminology, restUtils, this);
  }

  /* see superclass */
  public List<Concept> getAllQualifiersCache(Terminology terminology, IncludeParam ip)
      throws Exception {
    return sparqlQueryCacheService.getAllQualifiers(terminology, ip, restUtils, this);
  }

  private List<String> getIgnoreSourceUrls() {
    String uri = applicationProperties.getConfigBaseUri() + "/ignore-source.txt";
    if (StringUtils.isNotBlank(uri)) {
      log.info("Ignore source file URL:{}", uri);
      try {
        final URL url = new URL(uri);

        try (final InputStream is = url.openConnection().getInputStream()) {
          return IOUtils.readLines(is, "UTF-8");
        }
      } catch (Throwable t) {
        // Should not fail here if there are no ignore sources. Log and move on.
        log.warn("Error occurred when getting ignore sources", t);
      }
    }
    return Collections.emptyList();
  }
}
