package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.HierarchyUtils;

@Service
public class ElasticObjectServiceImpl implements ElasticObjectService {

  @Autowired
  ElasticsearchOperations operations;

  @Override
  public HierarchyUtils getHierarchy(Terminology terminology) {
    Optional<ElasticObject> esObject = getElasticObject("hierarchy", terminology);
    return (esObject.isPresent()) ? esObject.get().getHierarchy() : null;
  }
  
  @Override
  public List<Concept> getQualifiers(Terminology terminology) {
    Optional<ElasticObject> esObject = getElasticObject("qualifiers", terminology);
    return (esObject.isPresent()) ? esObject.get().getConcepts() : null;
  }
  
  @Override
  public List<Concept> getProperties(Terminology terminology) {
    Optional<ElasticObject> esObject = getElasticObject("properties", terminology);
    return (esObject.isPresent()) ? esObject.get().getConcepts() : null;
  }

  @Override
  public List<Concept> getAssociations(Terminology terminology) {
    Optional<ElasticObject> esObject = getElasticObject("associations", terminology);
    return (esObject.isPresent()) ? esObject.get().getConcepts() : null;
  }
  
  @Override
  public List<Concept> getRoles(Terminology terminology) {
    Optional<ElasticObject> esObject = getElasticObject("roles", terminology);
    return (esObject.isPresent()) ? esObject.get().getConcepts() : null;
  }  
  
  @SuppressWarnings("unchecked")
  @Override
  public List<ConceptMinimal> getContributingSources(Terminology terminology)
      throws ClassNotFoundException, IOException {
    return getConceptMinimalList("contributing_sources", terminology);
  }

  @Override
  public List<ConceptMinimal> getSynonymSources(Terminology terminology) throws ClassNotFoundException, IOException {
    return getConceptMinimalList("synonym_sources", terminology);
  }

  private List<ConceptMinimal> getConceptMinimalList(String id, Terminology terminology)
      throws ClassNotFoundException, IOException {
    Optional<ElasticObject> esObject = getElasticObject(id, terminology);
    if (!esObject.isPresent()) {
      return Collections.<ConceptMinimal>emptyList();
    }

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
