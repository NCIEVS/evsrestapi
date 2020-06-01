package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.ElasticObjectUtils;

@Service
public class ElasticObjectServiceImpl implements ElasticObjectService {

  @Autowired
  ElasticsearchOperations operations;

  @SuppressWarnings("unchecked")
  @Override
  public List<ConceptMinimal> getContributingSources(Terminology terminology)
      throws ClassNotFoundException, IOException {
    return getConceptMinimalListObject("contributing_sources", terminology);
  }

  @Override
  public List<ConceptMinimal> getSynonymSources(Terminology terminology) throws ClassNotFoundException, IOException {
    return getConceptMinimalListObject("synonym_sources", terminology);
  }

  private List<ConceptMinimal> getConceptMinimalListObject(String id, Terminology terminology)
      throws ClassNotFoundException, IOException {
    NativeSearchQuery query = new NativeSearchQueryBuilder().withIds(Arrays.asList(id))
        .withIndices(terminology.getObjectIndexName()).withTypes(ElasticOperationsService.OBJECT_TYPE).build();

    List<ElasticObject> objects = operations.queryForList(query, ElasticObject.class);
    if (CollectionUtils.isEmpty(objects)) {
      return Collections.<ConceptMinimal>emptyList();
    }

    byte[] data = objects.get(0).getData();

    return ElasticObjectUtils.deserializeConceptMinimalList(data);
  }
}
