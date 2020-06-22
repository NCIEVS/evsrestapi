package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
  public List<HierarchyNode> getChildNodes(String parent, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    Optional<HierarchyUtils> hierarchy = getHierarchy(terminology);
    if (!hierarchy.isPresent()) return Collections.emptyList();
    return hierarchy.get().getChildNodes(parent, 0);
  }
  
  @Override
  public List<HierarchyNode> getChildNodes(String parent, int maxLevel, Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    Optional<HierarchyUtils> hierarchy = getHierarchy(terminology);
    if (!hierarchy.isPresent()) return Collections.emptyList();
    return hierarchy.get().getChildNodes(parent, maxLevel);
  }
  
  @Override
  public List<HierarchyNode> getRootNodes(Terminology terminology)
    throws JsonParseException, JsonMappingException, IOException {
    Optional<HierarchyUtils> hierarchy = getHierarchy(terminology);
    if (!hierarchy.isPresent()) return Collections.emptyList();
    return hierarchy.get().getRootNodes();
  }
  
  @Override
  public Optional<HierarchyUtils> getHierarchy(Terminology terminology) throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject("hierarchy", terminology);
    if (!esObject.isPresent()) return Optional.empty();
    
    String data = esObject.get().getData();
    ObjectMapper mapper = new ObjectMapper();
    HierarchyUtils hierarchy = mapper.readValue(data, HierarchyUtils.class);
    
    return Optional.of(hierarchy);
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
    
    String data = esObject.get().getData();
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(data, new TypeReference<List<Concept>>(){});
  }
  
  private List<ConceptMinimal> getConceptMinimalList(String id, Terminology terminology)  throws JsonMappingException, JsonProcessingException {
    Optional<ElasticObject> esObject = getElasticObject(id, terminology);
    if (!esObject.isPresent()) {
      return Collections.<ConceptMinimal>emptyList();
    }
    
    String data = esObject.get().getData();
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(data, new TypeReference<List<ConceptMinimal>>(){});
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
  
  //-- private methods
}
