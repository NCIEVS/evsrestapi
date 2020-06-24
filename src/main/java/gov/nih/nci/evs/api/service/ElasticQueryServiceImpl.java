package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import gov.nih.nci.evs.api.model.HierarchyNode;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.EVSConceptMultiGetResultMapper;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.HierarchyUtils;

@Service
public class ElasticQueryServiceImpl implements ElasticQueryService {

  private static final Logger logger = LoggerFactory.getLogger(ElasticQueryServiceImpl.class);
  
  @Autowired
  ElasticsearchOperations operations;
  
  @Override
  public boolean checkConceptExists(String code, Terminology terminology) {
    logger.debug(String.format("checkConceptExists(%s)", code));
    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .withIds(Arrays.asList(code))
        .withIndices(terminology.getIndexName())
        .withTypes(ElasticOperationsService.CONCEPT_TYPE)
        .build();

    long count = operations.count(query, Concept.class);
    logger.info("count: " + count);
    return (count > 0);
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
  
  /**
   * Find paths.
   *
   * @return the paths
   * @throws JsonProcessingException 
   * @throws JsonMappingException 
   */
//  @Override
//  public Paths findPaths(Terminology terminology) throws JsonMappingException, JsonProcessingException {
//    Paths paths = new Paths();
//    Deque<String> stack = new ArrayDeque<String>();
//    Optional<HierarchyUtils> hierarchy = getHierarchy(terminology);
//    if (!hierarchy.isPresent()) return null;
//    ArrayList<String> roots = hierarchy.get().getHierarchyRoots();
//    for (String root : roots) {
//      stack.push(root);
//    }
//    while (!stack.isEmpty()) {
//      String path = stack.pop();
//      String[] values = path.trim().split("\\|");
//      List<String> elements = Arrays.asList(values);
//      String lastCode = elements.get(elements.size() - 1);
//      List<Concept> subclasses = getSubclasses(lastCode, terminology);
//      if (subclasses == null || CollectionUtils.isEmpty(subclasses)) {
//        paths.add(createPath(path, terminology));
//      } else {
//        for (Concept subclass : subclasses) {
//          stack.push(path + "|" + subclass.getCode());
//        }
//      }
//    }
//
//    return paths;
//  }
//
//  /**
//   * Creates the path.
//   *
//   * @param path the path
//   * @return the path
//   */
//  private Path createPath(String path, Terminology terminology) {
//    Path p = new Path();
//    List<ConceptNode> conceptNodes = new ArrayList<ConceptNode>();
//    String[] codes = path.split("\\|");
//    List<String> codeList = Arrays.asList(codes);
//    List<Concept> concepts = getConcepts(codeList, terminology, new IncludeParam("minimal"));
//    for (int i = 0; i < concepts.size(); i++) {
//      ConceptNode concept = new ConceptNode(i, concepts.get(i).getName(), concepts.get(i).getCode());
//      conceptNodes.add(concept);
//    }
//
//    p.setDirection(1);
//    p.setConcepts(conceptNodes);
//    return p;
//  }
  
  /**
   * Find paths to roots.
   *
   * @param code the code
   * @param hset the hset
   * @return the paths
   */
//  public Paths findPathsToRoots(String code, HashSet<String> hset, Terminology terminology) {
//    Paths paths = new Paths();
//    Stack<String> stack = new Stack<>();
//    stack.push(code);
//    while (!stack.isEmpty()) {
//      String path = (String) stack.pop();
//      Vector<String> u = PathFinder.parseData(path, '|');
//      String lastCode = (String) u.elementAt(u.size() - 1);
//      List<Concept> sups = getSuperclasses(lastCode, terminology);
//      if (sups == null) {
//        paths.add(createPath(path, terminology));
//      } else {
//        Vector<String> w = new Vector<>();
//        for (int i = 0; i < sups.size(); i++) {
//          String sup = sups.get(i).getCode();
//          if (!hset.contains(sup)) {
//            w.add(sup);
//          }
//        }
//        if (w.size() == 0) {
//          paths.add(createPath(path, terminology));
//        } else {
//          for (int k = 0; k < w.size(); k++) {
//            String s = (String) w.elementAt(k);
//            stack.push(path + "|" + s);
//          }
//        }
//      }
//    }
//    return paths;
//  }
  
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
