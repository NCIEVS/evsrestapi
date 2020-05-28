package gov.nih.nci.evs.api.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.ElasticObject;

public class ElasticObjectServiceImpl implements ElasticObjectService {

  @Autowired
  ElasticsearchOperations operations;
  
  @Override
  public List<ConceptMinimal> getContributingSources(Terminology terminology) {
    
    NativeSearchQuery query = new NativeSearchQueryBuilder()
      .withIds(Arrays.asList("contributing_sources"))
      .withIndices(terminology.getObjectIndexName())
      .withTypes(ElasticOperationsService.OBJECT_TYPE)
      .build();
    
    List<ElasticObject> objects = operations.queryForList(query, ElasticObject.class);
    
    if (CollectionUtils.isEmpty(objects)) return Collections.<ConceptMinimal>emptyList();
    
    return (List<ConceptMinimal>)objects.get(0).getObject();
  }

}
