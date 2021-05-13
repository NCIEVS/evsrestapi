
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.AssociationEntry;
import gov.nih.nci.evs.api.model.AssociationEntryResultList;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.EVSConceptMultiGetResultMapper;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.support.es.IndexMetadata;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.PathUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * The implementation for {@link ElasticQueryService}}.
 *
 * @author Arun
 */
@Service
public class ElasticQueryServiceImpl implements ElasticQueryService {

  /** the logger *. */
  private static final Logger logger = LoggerFactory.getLogger(ElasticQueryServiceImpl.class);

  /** the elasticsearch operations *. */
  @Autowired
  ElasticsearchOperations operations;

  /** the term utils */
  @Autowired
  TerminologyUtils termUtils;

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @return true, if successful
   */
  @Override
  public boolean checkConceptExists(String code, Terminology terminology) {
    logger.debug(String.format("checkConceptExists(%s)", code));
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("minimal"));
    return concept.isPresent();
  }

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the ip
   * @return the concept
   */
  @Override
  public Optional<Concept> getConcept(String code, Terminology terminology, IncludeParam ip) {
    List<Concept> concepts = getConcepts(Arrays.asList(code), terminology, ip);
    if (CollectionUtils.isEmpty(concepts)) {
      return Optional.empty();
    }
    return Optional.of(concepts.get(0));
  }

  /**
   * see superclass *.
   *
   * @param codes the codes
   * @param terminology the terminology
   * @param ip the ip
   * @return the concepts
   */
  @Override
  public List<Concept> getConcepts(Collection<String> codes, Terminology terminology,
    IncludeParam ip) {
    NativeSearchQuery query =
        new NativeSearchQueryBuilder().withIds(codes).withIndices(terminology.getIndexName())
            .withTypes(ElasticOperationsService.CONCEPT_TYPE).build();

    List<Concept> concepts =
        operations.multiGet(query, Concept.class, new EVSConceptMultiGetResultMapper(ip));
    return concepts;
  }

  /**
   * see superclass *.
   *
   * @param codes the codes
   * @param terminology the terminology
   * @param ip the ip
   * @return the concepts as map
   */
  @Override
  public Map<String, Concept> getConceptsAsMap(Collection<String> codes, Terminology terminology,
    IncludeParam ip) {
    List<Concept> concepts = getConcepts(codes, terminology, ip);
    if (CollectionUtils.isEmpty(concepts))
      return Collections.emptyMap();
    Map<String, Concept> result = new HashMap<>();
    concepts.stream().forEach(c -> result.put(c.getCode(), c));
    return result;
  }

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the subclasses
   */
  @Override
  public List<Concept> getSubclasses(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("children"));
    if (!concept.isPresent() || CollectionUtils.isEmpty(concept.get().getChildren())) {
      return Collections.emptyList();
    }
    return concept.get().getChildren();
  }

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the descendants
   */
  @Override
  public List<Concept> getDescendants(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("descendants"));
    if (!concept.isPresent() || CollectionUtils.isEmpty(concept.get().getDescendants())) {
      return Collections.emptyList();
    }

    return concept.get().getDescendants();
  }

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the superclasses
   */
  @Override
  public List<Concept> getSuperclasses(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("parents"));
    if (!concept.isPresent() || CollectionUtils.isEmpty(concept.get().getParents())) {
      return Collections.emptyList();
    }

    return concept.get().getParents();
  }

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the label
   */
  @Override
  public Optional<String> getLabel(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("minimal"));
    if (!concept.isPresent() || concept.get().getName() == null)
      return Optional.empty();
    return Optional.of(concept.get().getName());
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the root nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<Concept> getRootNodes(Terminology terminology, IncludeParam ip)
    throws JsonParseException, JsonMappingException, IOException {
    Optional<HierarchyUtils> hierarchy = getHierarchyRoots(terminology);
    if (!hierarchy.isPresent())
      return Collections.emptyList();
    ArrayList<String> hierarchyRoots = hierarchy.get().getHierarchyRoots();
    List<Concept> concepts = getConcepts(hierarchyRoots, terminology, ip);
    concepts.sort(Comparator.comparing(Concept::getName));
    for (Concept c : concepts) {
      c.setLeaf(null);
    }
    return concepts;
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @return the root nodes
   * @throws JsonParseException the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public List<HierarchyNode> getRootNodesHierarchy(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    Optional<HierarchyUtils> hierarchy = getHierarchyRoots(terminology);
    if (!hierarchy.isPresent())
      return Collections.emptyList();
    ArrayList<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
    ArrayList<String> hierarchyRoots = hierarchy.get().getHierarchyRoots();
    List<Concept> concepts = getConcepts(hierarchyRoots, terminology, new IncludeParam("minimal"));
    for (Concept c : concepts) {
      HierarchyNode node = new HierarchyNode(c.getCode(), c.getName(), false);
      nodes.add(node);
    }
    nodes.sort(Comparator.comparing(HierarchyNode::getLabel));
    return nodes;
  }

  /**
   * see superclass *.
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
    ArrayList<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
    List<Concept> children = getSubclasses(parent, terminology);
    if (children == null) {
      return nodes;
    }

    for (Concept c : children) {
      HierarchyNode node = new HierarchyNode(c.getCode(), c.getName(), c.getLeaf());
      nodes.add(node);
    }
    nodes.sort(Comparator.comparing(HierarchyNode::getLabel));
    return nodes;
  }

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @return the all child nodes
   */
  @Override
  public List<String> getAllChildNodes(String code, Terminology terminology) {
    ArrayList<String> childCodes = new ArrayList<String>();

    List<Concept> children = getSubclasses(code, terminology);
    if (children == null || children.size() == 0) {
      return childCodes;
    }

    for (Concept c : children) {
      childCodes.add(c.getCode());
      getAllChildNodesRecursive(c.getCode(), childCodes, terminology);
    }

    return childCodes.stream().distinct().collect(Collectors.toList());
  }

  /**
   * Returns the all child nodes recursive.
   *
   * @param code the code
   * @param childCodes the child codes
   * @param terminology the terminology
   * @return the all child nodes recursive
   */
  private void getAllChildNodesRecursive(String code, ArrayList<String> childCodes,
    Terminology terminology) {
    List<Concept> children = getSubclasses(code, terminology);
    if (children == null || children.size() == 0) {
      return;
    } else {
      for (Concept c : children) {
        childCodes.add(c.getCode());
        getAllChildNodesRecursive(c.getCode(), childCodes, terminology);
      }
    }
  }

  /**
   * see superclass *.
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
    List<HierarchyNode> rootNodes = getRootNodesHierarchy(terminology);

    Paths paths = getPathToRoot(code, terminology);

    // root hierarchy node map for quick look up
    Map<String, HierarchyNode> rootNodeMap = new HashMap<>();
    rootNodes.stream().forEach(n -> rootNodeMap.put(n.getCode(), n));

    List<Path> ps = paths.getPaths();
    for (Path path : ps) {
      List<Concept> concepts = path.getConcepts();
      if (CollectionUtils.isEmpty(concepts) || concepts.size() < 2)
        continue;
      Concept rootConcept = concepts.get(concepts.size() - 1);

      HierarchyNode root = rootNodeMap.get(rootConcept.getCode());
      HierarchyNode previous = root;
      for (int j = concepts.size() - 2; j >= 0; j--) {
        Concept c = concepts.get(j);
        if (!previous.getChildren().stream().anyMatch(n -> n.getCode().equals(c.getCode()))) {
          List<HierarchyNode> children = getChildNodes(previous.getCode(), 0, terminology);
          for (HierarchyNode child : children) {
            child.setLevel(null);
            previous.getChildren().add(child);
          }
          previous.setExpanded(true);
        }
        previous = previous.getChildren().stream().filter(n -> n.getCode().equals(c.getCode()))
            .findFirst().orElse(null);
      }
    }

    return rootNodes;
  }

  /**
   * see superclass *.
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
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("paths"));
    if (!concept.isPresent() || concept.get().getPaths() == null)
      return new Paths();
    return concept.get().getPaths();
  }

  /**
   * see superclass *.
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
    logger.debug(String.format("getPathToParent(%s, %s)", code, parentCode));
    Paths paths = getPathToRoot(code, terminology);
    logger.debug("paths: " + paths);
    Paths conceptPaths = new Paths();
    Concept concept = new Concept();
    for (Path path : paths.getPaths()) {
      logger.debug("checking path: " + path);
      Boolean codeSW = false;
      Boolean parentSW = false;
      int idx = -1;
      List<Concept> concepts = path.getConcepts();
      for (int i = 0; i < concepts.size(); i++) {
        concept = concepts.get(i);
        if (concept.getCode().equals(code)) {
          codeSW = true;
        }
        if (concept.getCode().equals(parentCode)) {
          parentSW = true;
          idx = i;
        }
      }
      if (codeSW && parentSW) {
        logger.debug("both codeSW and parentSW are TRUE");
        List<Concept> trimed_concepts = new ArrayList<Concept>();
        if (idx == -1) {
          idx = concepts.size() - 1;
        }
        logger.debug("idx: " + idx);
        for (int i = 0; i <= idx; i++) {
          logger.debug("adding concept: " + concepts.get(i).getCode());
          trimed_concepts.add(concepts.get(i));
          if (concepts.get(i).getCode().equals(parentCode)) {
            break;
          }
        }
        logger.debug("trimmed concepts: " + trimed_concepts);
        conceptPaths.add(new Path(1, trimed_concepts));
      }
    }
    logger.debug("concept paths: " + conceptPaths);
    conceptPaths = PathUtils.removeDuplicatePaths(conceptPaths);
    logger.debug("concept paths after de-dupe: " + conceptPaths);
    return conceptPaths;
  }

  /**
   * see superclass *.
   * 
   * @param terminology the terminology
   * @return the concepts count
   */
  @Override
  public long getCount(Terminology terminology) {
    NativeSearchQuery query = new NativeSearchQueryBuilder().withIndices(terminology.getIndexName())
        .withTypes(ElasticOperationsService.CONCEPT_TYPE).build();

    return operations.count(query);
  }

  /**
   * see superclass *.
   * 
   * @param completedOnly boolean indicating to fetch metadata for complete
   *          indexes only
   * @return the list of {@link IndexMetadata} objects
   */
  @Override
  public List<IndexMetadata> getIndexMetadata(boolean completedOnly) {
    NativeSearchQueryBuilder queryBuilder =
        new NativeSearchQueryBuilder().withIndices(ElasticOperationsService.METADATA_INDEX)
            .withTypes(ElasticOperationsService.METADATA_TYPE);

    if (completedOnly) {
      queryBuilder = queryBuilder.withFilter(QueryBuilders.matchQuery("completed", true));
    }

    List<IndexMetadata> iMetas = operations.queryForList(queryBuilder.build(), IndexMetadata.class);
    return iMetas;
  }

  /**
   * see superclass *.
   * 
   * @param id the id of the {@link IndexMetadata} object
   */
  @Override
  public void deleteIndexMetadata(String id) {
    DeleteQuery delQuery = new DeleteQuery();
    delQuery.setQuery(QueryBuilders.idsQuery().addIds(id));
    delQuery.setIndex(ElasticOperationsService.METADATA_INDEX);
    delQuery.setType(ElasticOperationsService.METADATA_TYPE);

    operations.delete(delQuery);
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @return the hierarchy
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  @Override
  public Optional<HierarchyUtils> getHierarchyRoots(Terminology terminology)
    throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject("hierarchy", terminology);
    if (!esObject.isPresent())
      return Optional.empty();

    return Optional.of(esObject.get().getHierarchy());
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the qualifiers
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  @Override
  public List<Concept> getQualifiers(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonProcessingException {
    return getConceptList("qualifiers", terminology, ip);
  }

  /**
   * see superclass *.
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
  public Optional<Concept> getQualifier(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    List<Concept> qualifiers = getQualifiers(terminology, ip);
    return qualifiers.stream().filter(q -> q.getCode().equals(code)).findFirst();
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the properties
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  @Override
  public List<Concept> getProperties(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonProcessingException {
    return getConceptList("properties", terminology, ip);
  }

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the ip
   * @return the property
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public Optional<Concept> getProperty(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    List<Concept> properties = getProperties(terminology, ip);
    return properties.stream().filter(p -> p.getCode().equals(code)).findFirst();
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the associations
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  @Override
  public List<Concept> getAssociations(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonProcessingException {
    return getConceptList("associations", terminology, ip);
  }

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the ip
   * @return the association
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public Optional<Concept> getAssociation(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    List<Concept> associations = getAssociations(terminology, ip);
    return associations.stream().filter(a -> a.getCode().equals(code)).findFirst();
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @param fromRecord the starting record for the search
   * @param pageSize the size of pages in returned result
   * @return the association entry list
   * @throws Exception Signals that an exception has occurred.
   */
  public AssociationEntryResultList getAssociationEntries(String terminology, String label,
    int fromRecord, int pageSize) throws Exception {
    AssociationEntryResultList al = new AssociationEntryResultList();
    Optional<ElasticObject> esObject = getElasticObject("associationEntries_" + label,
        termUtils.getTerminology(terminology, true));
    // set params in object
    List<String> params =
        Arrays.asList(terminology, label, String.valueOf(fromRecord), String.valueOf(pageSize));
    SearchCriteria criteria = new SearchCriteria();
    criteria.setTerminology(params);
    al.setParameters(criteria);
    // check for results
    if (!esObject.isPresent()) {
      al.setTotal(0);
      return al;
    }
    List<AssociationEntry> list = esObject.get().getAssociationEntries();
    int from = Math.min(fromRecord, list.size());
    int to = Math.min(fromRecord + pageSize, list.size());
    // package up as AssociationEntryResultList
    al.setAssociationEntrys(list.subList(from, to));
    al.setTotal(list.size());
    logger.info("al = " + al);
    return al;
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @param ip the ip
   * @return the roles
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  @Override
  public List<Concept> getRoles(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonProcessingException {
    return getConceptList("roles", terminology, ip);
  }

  /**
   * see superclass *.
   *
   * @param code the code
   * @param terminology the terminology
   * @param ip the ip
   * @return the role
   * @throws JsonMappingException the json mapping exception
   * @throws JsonParseException the json parse exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  public Optional<Concept> getRole(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    List<Concept> roles = getRoles(terminology, ip);
    return roles.stream().filter(r -> r.getCode().equals(code)).findFirst();
  }

  /* see superclass */
  @Override
  public List<Concept> getSynonymTypes(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonProcessingException {
    return getConceptList("synonymTypes", terminology, ip);
  }

  /* see superclass */
  @Override
  public Optional<Concept> getSynonymType(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    List<Concept> synonymTypes = getSynonymTypes(terminology, ip);
    return synonymTypes.stream().filter(r -> r.getCode().equals(code)).findFirst();
  }

  /* see superclass */
  @Override
  public List<Concept> getDefinitionTypes(Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonProcessingException {
    return getConceptList("definitionTypes", terminology, ip);
  }

  /* see superclass */
  @Override
  public Optional<Concept> getDefinitionType(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    List<Concept> definitionTypes = getDefinitionTypes(terminology, ip);
    return definitionTypes.stream().filter(r -> r.getCode().equals(code)).findFirst();
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @return the contributing sources
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  @Override
  public List<ConceptMinimal> getContributingSources(Terminology terminology)
    throws JsonMappingException, JsonProcessingException {
    return getConceptMinimalList("contributing_sources", terminology);
  }

  /**
   * see superclass *.
   *
   * @param terminology the terminology
   * @return the synonym sources
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  @Override
  public List<ConceptMinimal> getSynonymSources(Terminology terminology)
    throws JsonMappingException, JsonProcessingException {
    return getConceptMinimalList("synonym_sources", terminology);
  }

  /**
   * Returns concept list wrapped by elasticsearch object.
   *
   * @param id the id of the elasticsearch object
   * @param terminology the terminology
   * @param ip the ip
   * @return the concept list identified by the id
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  private List<Concept> getConceptList(String id, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject(id, terminology);
    if (!esObject.isPresent()) {
      return Collections.<Concept> emptyList();
    }

    List<Concept> concepts = esObject.get().getConcepts();
    return ConceptUtils.applyInclude(concepts, ip);
  }

  /**
   * Returns concept minimal list wrapped by elasticsearch object.
   *
   * @param id the id of the elasticsearch object
   * @param terminology the terminology
   * @return the concept minimal list identified by the id
   * @throws JsonMappingException the json mapping exception
   * @throws JsonProcessingException the json processing exception
   */
  private List<ConceptMinimal> getConceptMinimalList(String id, Terminology terminology)
    throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject(id, terminology);
    if (!esObject.isPresent()) {
      return Collections.<ConceptMinimal> emptyList();
    }

    return esObject.get().getConceptMinimals();
  }

  /**
   * Returns the elasticsearch object.
   *
   * @param id the id of the elasticsearch object
   * @param terminology the terminology
   * @return the optional of elasticsearch object
   */
  private Optional<ElasticObject> getElasticObject(String id, Terminology terminology) {
    if (logger.isDebugEnabled()) {
      logger.debug("getElasticObject({}, {})", id, terminology.getTerminology());
    }

    NativeSearchQuery query =
        new NativeSearchQueryBuilder().withFilter(QueryBuilders.termQuery("_id", id))
            .withIndices(terminology.getObjectIndexName())
            .withTypes(ElasticOperationsService.OBJECT_TYPE).build();

    List<ElasticObject> objects = operations.queryForList(query, ElasticObject.class);

    if (CollectionUtils.isEmpty(objects)) {
      return Optional.<ElasticObject> empty();
    }

    return Optional.of(objects.get(0));
  }
}
