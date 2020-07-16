package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.ConceptNode;
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.EVSConceptMultiGetResultMapper;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.PathUtils;

/**
 * The implementation for {@link ElasticQueryService}}
 * 
 * @author Arun
 *
 */
@Service
public class ElasticQueryServiceImpl implements ElasticQueryService {

  /** the logger **/
  private static final Logger logger = LoggerFactory.getLogger(ElasticQueryServiceImpl.class);
  
  /** the elasticsearch operations **/
  @Autowired
  ElasticsearchOperations operations;

  /** see superclass **/
  @Override
  public boolean checkConceptExists(String code, Terminology terminology) {
    logger.debug(String.format("checkConceptExists(%s)", code));
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("minimal"));
    return concept.isPresent();
  }
  
  /** see superclass **/
  @Override
  public Optional<Concept> getConcept(String code, Terminology terminology, IncludeParam ip) {
    logger.debug(String.format("getConcept(%s)", code));
    logger.debug("index: " + terminology.getIndexName());
    List<Concept> concepts = getConcepts(Arrays.asList(code), terminology, ip);
    logger.debug("concepts: " + concepts);
    if (CollectionUtils.isEmpty(concepts)) return Optional.empty();
    logger.debug("result size: " + concepts.size());
    return Optional.of(concepts.get(0));
  }
  
  /** see superclass **/
  @Override
  public List<Concept> getConcepts(Collection<String> codes, Terminology terminology, IncludeParam ip) {
    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withIds(codes)
        .withIndices(terminology.getIndexName())
        .withTypes(ElasticOperationsService.CONCEPT_TYPE)
        .build();

    List<Concept> concepts = operations.multiGet(
        query, Concept.class, new EVSConceptMultiGetResultMapper(ip));
    return concepts;
  }

  /** see superclass **/
  @Override
  public Map<String, Concept> getConceptsAsMap(Collection<String> codes, Terminology terminology, IncludeParam ip) {
    List<Concept> concepts = getConcepts(codes, terminology, ip);
    if (CollectionUtils.isEmpty(concepts)) return Collections.emptyMap();
    Map<String, Concept> result = new HashMap<>();
    concepts.stream().forEach(c -> result.put(c.getCode(), c));
    return result;
  }
  
  /** see superclass **/
  @Override
  public List<Concept> getSubclasses(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("children"));
    if (!concept.isPresent() || CollectionUtils.isEmpty(concept.get().getChildren())) {
      return Collections.emptyList();
    }
    
    return concept.get().getChildren();
  }

  /** see superclass **/
  @Override
  public List<Concept> getSuperclasses(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("parents"));
    if (!concept.isPresent() || CollectionUtils.isEmpty(concept.get().getParents())) {
      return Collections.emptyList();
    }
    
    return concept.get().getParents();
  }
  
  /** see superclass **/
  @Override
  public Optional<String> getLabel(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("minimal"));
    if (!concept.isPresent() || concept.get().getName() == null) return Optional.empty();
    return Optional.of(concept.get().getName());
  }
  
  /** see superclass **/
  @Override
  public List<HierarchyNode> getRootNodes(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    Optional<HierarchyUtils> hierarchy = getHierarchy(terminology);
    if (!hierarchy.isPresent()) return Collections.emptyList();
    ArrayList<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
    ArrayList<String> hierarchyRoots = hierarchy.get().getHierarchyRoots();
    List<Concept> concepts = getConcepts(hierarchyRoots, terminology, new IncludeParam("minimal"));
    for (Concept c: concepts) {
      HierarchyNode node = new HierarchyNode(c.getCode(), c.getName(), false);
      nodes.add(node);
    }
    nodes.sort(Comparator.comparing(HierarchyNode::getLabel));
    return nodes;
  }
  
  /** see superclass **/
  @Override
  public List<HierarchyNode> getChildNodes(String parent, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    return getChildNodes(parent, 0, terminology);
  }
  
  /** see superclass **/
  @Override
  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    ArrayList<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
    List<Concept> children = getSubclasses(parent, terminology);
    if (children == null) {
      return nodes;
    }
    for (Concept c : children) {
      HierarchyNode node = new HierarchyNode(c.getCode(), c.getName(), false);
      getChildNodesLevel(node, maxLevel, 0, terminology);
      nodes.add(node);
    }
    nodes.sort(Comparator.comparing(HierarchyNode::getLabel));
    return nodes;
  }
  
  /**
   * Returns the child nodes level.
   *
   * @param node the node
   * @param maxLevel the max level
   * @param level the level
   * @return the child nodes level
   */
  private void getChildNodesLevel(HierarchyNode node, int maxLevel, int level, Terminology terminology) {
    List<Concept> children = getSubclasses(node.getCode(), terminology);
    node.setLevel(level);

    if (children == null || children.size() == 0) {
      node.setLeaf(true);
      return;
    } else {
      node.setLeaf(false);
    }
    if (level >= maxLevel) {
      return;
    }

    ArrayList<HierarchyNode> nodes = new ArrayList<HierarchyNode>();
    level = level + 1;
    for (Concept c : children) {
      HierarchyNode newnode = new HierarchyNode(c.getCode(), c.getName(), false);
      getChildNodesLevel(newnode, maxLevel, level, terminology);
      List<HierarchyNode> sortedChildren = newnode.getChildren();
      // Sort children if they exist
      if (sortedChildren != null && sortedChildren.size() > 0) {
        sortedChildren.sort(Comparator.comparing(HierarchyNode::getLabel));
      }

      newnode.setChildren(sortedChildren);
      nodes.add(newnode);
    }
    nodes.sort(Comparator.comparing(HierarchyNode::getLabel));
    node.setChildren(nodes);
  }
  
  /** see superclass **/
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
   * @return the all child nodes recursive
   */
  private void getAllChildNodesRecursive(String code, ArrayList<String> childCodes, Terminology terminology) {
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
  
  /** see superclass **/
  @Override
  public List<HierarchyNode> getPathInHierarchy(String code, Terminology terminology) 
      throws JsonParseException, JsonMappingException, IOException {
      List<HierarchyNode> rootNodes = getRootNodes(terminology);
      Paths paths = getPathToRoot(code, terminology);
      
      //root hierarchy node map for quick look up
      Map<String, HierarchyNode> rootNodeMap = new HashMap<>(); 
      rootNodes.stream().forEach(n -> rootNodeMap.put(n.getCode(), n));
          
      //concepts map for all concepts in the paths
      Map<String, Concept> conceptMap = getConceptsInPaths(code, paths, terminology);
      
      //known hierarchy nodes map
      Map<String, HierarchyNode> knownNodeMap = new HashMap<>();
      
      for(Path path: paths.getPaths()) {
        List<ConceptNode> cNodes = path.getConcepts();
        if (CollectionUtils.isEmpty(cNodes) || cNodes.size() < 2) continue;
        ConceptNode rootConceptNode = cNodes.get(cNodes.size() - 1);

        HierarchyNode root = rootNodeMap.get(rootConceptNode.getCode());
        HierarchyNode previous = root;
        for(int j=cNodes.size()-2; j>=0; j--) {
          ConceptNode cNode = cNodes.get(j);
          Concept c = conceptMap.get(cNode.getCode());
          HierarchyNode current = getHierarchyNode(c, knownNodeMap);
          if (!previous.getChildren().stream().anyMatch(n -> n.getCode().equals(current.getCode()))) {
            previous.getChildren().add(current);
            previous.setExpanded(true);
          }
          previous = current;
        }
      }
      
      //set children for the concept
      Concept concept = conceptMap.get(code);
      HierarchyNode hNode = knownNodeMap.get(code);
      if(hNode != null) {
	      hNode.setHighlight(true);
	      hNode.setLeaf(concept.getLeaf());
	      List<Concept> children = concept.getChildren();
	      for(Concept child: children) {
	        //leaf property is not set as part of children; hence using concept from concept map for leaf property
          hNode.getChildren().add(new HierarchyNode(child.getCode(), child.getName(), conceptMap.get(child.getCode()).getLeaf()));
          hNode.setExpanded(true);
	      }
      }
      
      return rootNodes;
  }
  
  /**
   * Gets all concepts part of the paths including children for the given code
   * 
   * @param code
   * @param paths
   * @param terminology
   * @return
   */
  private Map<String, Concept> getConceptsInPaths(String code, Paths paths, Terminology terminology) {
    final Set<String> codes = new HashSet<>();
    for(Path path: paths.getPaths()) {
      for(int i=0; i<path.getConcepts().size(); i++) {
        ConceptNode node = path.getConcepts().get(i);
        codes.add(node.getCode());
      }
    }

    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("children"));
    if (concept.isPresent() && !concept.get().getLeaf()) {
      concept.get().getChildren().stream().forEach(c -> codes.add(c.getCode()));
    }
    
    return getConceptsAsMap(codes, terminology, new IncludeParam("children"));
  }
  
  private HierarchyNode getHierarchyNode(Concept concept, Map<String, HierarchyNode> knownNodeMap) {
    HierarchyNode hNode = null;
    if (knownNodeMap.containsKey(concept.getCode())) {
      hNode = knownNodeMap.get(concept.getCode());
    } else {
      hNode = new HierarchyNode(concept.getCode(), concept.getName(), concept.getLeaf());
      knownNodeMap.put(concept.getCode(), hNode);
    }
    
    return hNode;
  }
  
  /** see superclass **/
  @Override
  public Paths getPathToRoot(String code, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("full"));
    if (!concept.isPresent() || concept.get().getPaths() == null) return new Paths();
    return concept.get().getPaths();
  }
  
  /** see superclass **/
  @Override
  public Paths getPathToParent(String code, String parentCode, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    logger.debug(String.format("getPathToParent(%s, %s)", code, parentCode));
    Paths paths = getPathToRoot(code, terminology);
    logger.debug("paths: " + paths);
    Paths conceptPaths = new Paths();
    for (Path path : paths.getPaths()) {
      logger.debug("checking path: " + path);
      Boolean codeSW = false;
      Boolean parentSW = false;
      int idx = -1;
      List<ConceptNode> concepts = path.getConcepts();
      for (int i = 0; i < concepts.size(); i++) {
        ConceptNode concept = concepts.get(i);
        if (concept.getCode().equals(code)) {
          codeSW = true;
        }
        if (concept.getCode().equals(parentCode)) {
          parentSW = true;
          idx = concept.getIdx();
        }
      }
      if (codeSW && parentSW) {
        logger.debug("both codeSW and parentSW are TRUE");
        List<ConceptNode> trimed_concepts = new ArrayList<ConceptNode>();
        if (idx == -1) {
          idx = concepts.size() - 1;
        }
        logger.debug("idx: " + idx);
        for (int i = 0; i <= idx; i++) {
          ConceptNode c = new ConceptNode();
          c.setCode(concepts.get(i).getCode());
          c.setLabel(concepts.get(i).getLabel());
          c.setIdx(i);
          logger.debug("adding concept: " + c.getCode());
          trimed_concepts.add(c);
          if (c.getCode().equals(parentCode)) {
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

  /** see superclass **/
  @Override
  public Optional<HierarchyUtils> getHierarchy(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject("hierarchy", terminology);
    if (!esObject.isPresent()) return Optional.empty();
    
    return Optional.of(esObject.get().getHierarchy());
  }
  
  /** see superclass **/
  @Override
  public List<Concept> getQualifiers(Terminology terminology, IncludeParam ip) throws JsonMappingException, JsonProcessingException {
    return getConceptList("qualifiers", terminology, ip);
  }
  
  /** see superclass **/
  @Override
  public Optional<Concept> getQualifier(String code, Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonParseException, IOException {
    List<Concept> qualifiers = getQualifiers(terminology, ip);
    return qualifiers.stream().filter(q -> q.getCode().equals(code)).findFirst();
  }
  
  /** see superclass **/
  @Override
  public List<Concept> getProperties(Terminology terminology, IncludeParam ip) throws JsonMappingException, JsonProcessingException {
    return getConceptList("properties", terminology, ip);
  }

  /** see superclass **/
  @Override
  public Optional<Concept> getProperty(String code, Terminology terminology, IncludeParam ip)
      throws JsonMappingException, JsonParseException, IOException {
    List<Concept> properties = getProperties(terminology, ip);
    return properties.stream().filter(p -> p.getCode().equals(code)).findFirst();
  }
  
  /** see superclass **/
  @Override
  public List<Concept> getAssociations(Terminology terminology, IncludeParam ip) throws JsonMappingException, JsonProcessingException {
    return getConceptList("associations", terminology, ip);
  }
  
  /** see superclass **/
  @Override
  public Optional<Concept> getAssociation(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    List<Concept> associations = getAssociations(terminology, ip);
    return associations.stream().filter(a -> a.getCode().equals(code)).findFirst();
  }
  
  /** see superclass **/
  @Override
  public List<Concept> getRoles(Terminology terminology, IncludeParam ip) throws JsonMappingException, JsonProcessingException {
    return getConceptList("roles", terminology, ip);
  }  
  
  /** see superclass **/
  @Override
  public Optional<Concept> getRole(String code, Terminology terminology, IncludeParam ip)
    throws JsonMappingException, JsonParseException, IOException {
    List<Concept> roles = getRoles(terminology, ip);
    return roles.stream().filter(r -> r.getCode().equals(code)).findFirst();
  }
  
  /** see superclass **/
  @Override
  public List<ConceptMinimal> getContributingSources(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    return getConceptMinimalList("contributing_sources", terminology);
  }

  /** see superclass **/
  @Override
  public List<ConceptMinimal> getSynonymSources(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    return getConceptMinimalList("synonym_sources", terminology);
  }

  /**
   * Returns concept list wrapped by elasticsearch object
   * 
   * @param id the id of the elasticsearch object
   * @param terminology the terminology
   * @return the concept list identified by the id
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  private List<Concept> getConceptList(String id, Terminology terminology, IncludeParam ip) throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject(id, terminology);
    if (!esObject.isPresent()) {
      return Collections.<Concept>emptyList();
    }
    
    List<Concept> concepts = esObject.get().getConcepts();
    return ConceptUtils.applyInclude(concepts, ip);
  }
  
  /**
   * Returns concept minimal list wrapped by elasticsearch object
   * 
   * @param id the id of the elasticsearch object
   * @param terminology the terminology
   * @return the concept minimal list identified by the id
   * @throws JsonMappingException
   * @throws JsonProcessingException
   */
  private List<ConceptMinimal> getConceptMinimalList(String id, Terminology terminology)  throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject(id, terminology);
    if (!esObject.isPresent()) {
      return Collections.<ConceptMinimal>emptyList();
    }
    
    return esObject.get().getConceptMinimals();
  }
  
  /**
   * Returns the elasticsearch object
   * 
   * @param id the id of the elasticsearch object
   * @param terminology the terminology
   * @return the optional of elasticsearch object
   */
  private Optional<ElasticObject> getElasticObject(String id, Terminology terminology) {
    logger.info("getElasticObject({}, {})", id, terminology.getTerminology());
    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withFilter(QueryBuilders.termQuery("_id", id))
        .withIndices(terminology.getObjectIndexName())
        .withTypes(ElasticOperationsService.OBJECT_TYPE).build();
    
    List<ElasticObject> objects = operations.queryForList(query, ElasticObject.class);
    
    if (CollectionUtils.isEmpty(objects)) {
      return Optional.<ElasticObject>empty();
    }
    
    return Optional.of(objects.get(0));
  }
}
