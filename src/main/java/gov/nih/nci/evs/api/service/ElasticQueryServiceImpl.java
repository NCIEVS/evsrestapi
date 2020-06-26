package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
import com.fasterxml.jackson.databind.ObjectMapper;

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
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.PathUtils;

@Service
public class ElasticQueryServiceImpl implements ElasticQueryService {

  private static final Logger logger = LoggerFactory.getLogger(ElasticQueryServiceImpl.class);
  
  @Autowired
  ElasticsearchOperations operations;
  
  @Override
  public boolean checkConceptExists(String code, Terminology terminology) {
    logger.debug(String.format("checkConceptExists(%s)", code));
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("minimal"));
    return concept.isPresent();
  }
  
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
  
  @Override
  public List<Concept> getConcepts(List<String> codes, Terminology terminology, IncludeParam ip) {
    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withIds(codes)
        .withIndices(terminology.getIndexName())
        .withTypes(ElasticOperationsService.CONCEPT_TYPE)
        .build();

    List<Concept> concepts = operations.multiGet(
        query, Concept.class, new EVSConceptMultiGetResultMapper(ip));
    return concepts;
  }

  @Override
  public Map<String, Concept> getConceptsAsMap(List<String> codes, Terminology terminology, IncludeParam ip) {
    List<Concept> concepts = getConcepts(codes, terminology, ip);
    if (CollectionUtils.isEmpty(concepts)) return Collections.emptyMap();
    Map<String, Concept> result = new HashMap<>();
    concepts.stream().forEach(c -> result.put(c.getCode(), c));
    return result;
  }
  
  @Override
  public List<Concept> getSubclasses(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("children"));
    if (!concept.isPresent() || CollectionUtils.isEmpty(concept.get().getChildren())) {
      return Collections.emptyList();
    }
    
    return concept.get().getChildren();
  }

  @Override
  public List<Concept> getSuperclasses(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("parents"));
    if (!concept.isPresent() || CollectionUtils.isEmpty(concept.get().getParents())) {
      return Collections.emptyList();
    }
    
    return concept.get().getParents();
  }
  
  @Override
  public Optional<String> getLabel(String code, Terminology terminology) {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("minimal"));
    if (!concept.isPresent() || concept.get().getName() == null) return Optional.empty();
    return Optional.of(concept.get().getName());
  }
  
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
  
//  @Override
//  public Map<String, String> getCodeLabelMap(List<String> codes, Terminology terminology) {
//    List<Concept> concepts = getConcepts(codes, terminology, new IncludeParam("minimal"));
//    if (CollectionUtils.isEmpty(concepts)) {
//      return Collections.emptyMap();
//    }
//    
//    final Map<String, String> result = new HashMap<>();
//    concepts.stream().forEach(c -> result.put(c.getCode(), c.getName()));
//    return result;
//  }
  
  @Override
  public List<HierarchyNode> getChildNodes(String parent, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    return getChildNodes(parent, 0, terminology);
  }
  
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
  
  @Override
  public Paths getPathToRoot(String code, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    Optional<Concept> concept = getConcept(code, terminology, new IncludeParam("full"));
    if (!concept.isPresent() || StringUtils.isEmpty(concept.get().getPaths())) return new Paths();
    return new ObjectMapper().readValue(concept.get().getPaths(), Paths.class);
  }
  
  @Override
  public Paths getPathToParent(String code, String parentCode, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    logger.info(String.format("getPathToParent(%s, %s)", code, parentCode));
    Paths paths = getPathToRoot(code, terminology);
    logger.info("paths: " + paths);
    Paths conceptPaths = new Paths();
    for (Path path : paths.getPaths()) {
      logger.info("checking path: " + path);
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
        logger.info("both codeSW and parentSW are TRUE");
        List<ConceptNode> trimed_concepts = new ArrayList<ConceptNode>();
        if (idx == -1) {
          idx = concepts.size() - 1;
        }
        logger.info("idx: " + idx);
//        int j = 0;
        for (int i = 0; i <= idx; i++) {
          ConceptNode c = new ConceptNode();
          c.setCode(concepts.get(i).getCode());
          c.setLabel(concepts.get(i).getLabel());
          c.setIdx(i);
          logger.info("adding concept: " + c.getCode());
          trimed_concepts.add(c);
          if (c.getCode().equals(parentCode)) {
            break;
          }
        }
        logger.info("trimmed concepts: " + trimed_concepts);
        conceptPaths.add(new Path(1, trimed_concepts));
      }
    }
    logger.info("concept paths: " + conceptPaths);
    conceptPaths = PathUtils.removeDuplicatePaths(conceptPaths);
    logger.info("concept paths after de-dupe: " + conceptPaths);
    return conceptPaths;
  }
  
  @Override
  public Optional<HierarchyUtils> getHierarchy(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject("hierarchy", terminology);
    if (!esObject.isPresent()) return Optional.empty();
    
//    String data = esObject.get().getData();
//    ObjectMapper mapper = new ObjectMapper();
//    HierarchyUtils hierarchy = mapper.readValue(data, HierarchyUtils.class);
    
    return Optional.of(esObject.get().getHierarchy());
  }
  
  @Override
  public List<Concept> getQualifiers(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    return getConceptList("qualifiers", terminology);
  }
  
  @Override
  public List<Concept> getProperties(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    return getConceptList("properties", terminology);
  }

  @Override
  public List<Concept> getAssociations(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    return getConceptList("associations", terminology);
  }
  
  @Override
  public List<Concept> getRoles(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    return getConceptList("roles", terminology);
  }  
  
  @SuppressWarnings("unchecked")
  @Override
  public List<ConceptMinimal> getContributingSources(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    return getConceptMinimalList("contributing_sources", terminology);
  }

  @Override
  public List<ConceptMinimal> getSynonymSources(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    return getConceptMinimalList("synonym_sources", terminology);
  }

  private List<Concept> getConceptList(String id, Terminology terminology) throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject(id, terminology);
    if (!esObject.isPresent()) {
      return Collections.<Concept>emptyList();
    }
    
//    String data = esObject.get().getData();
//    ObjectMapper mapper = new ObjectMapper();
//    return mapper.readValue(data, new TypeReference<List<Concept>>(){});

    return esObject.get().getConcepts();
  }
  
  private List<ConceptMinimal> getConceptMinimalList(String id, Terminology terminology)  throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject(id, terminology);
    if (!esObject.isPresent()) {
      return Collections.<ConceptMinimal>emptyList();
    }
    
//    String data = esObject.get().getData();
//    ObjectMapper mapper = new ObjectMapper();
//    return mapper.readValue(data, new TypeReference<List<ConceptMinimal>>(){});
    return esObject.get().getConceptMinimals();
  }
  
  private Optional<ElasticObject> getElasticObject(String id, Terminology terminology) {
    NativeSearchQuery query = new NativeSearchQueryBuilder().withIds(Arrays.asList(id))
        .withIndices(terminology.getObjectIndexName()).withTypes(ElasticOperationsService.OBJECT_TYPE).build();

    List<ElasticObject> objects = operations.queryForList(query, ElasticObject.class);
    
    if (CollectionUtils.isEmpty(objects)) {
      return Optional.<ElasticObject>empty();
    }
    
    return Optional.of(objects.get(0));
  }
}
