package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.support.SearchCriteria;

@Service(value = "elasticSearchServiceImpl2")
public class ElasticSearchServiceImpl2 implements ElasticSearchService {

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(ElasticSearchServiceImpl2.class);
  
  /** The Elasticsearch operations **/
  @Autowired
  ElasticsearchOperations operations;
  
  @Override
  public void initSettings() throws IOException, HttpClientErrorException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ConceptResultList search(SearchCriteria searchCriteria) throws IOException, HttpClientErrorException {
    int page = searchCriteria.getFromRecord() / searchCriteria.getPageSize();
    Pageable pageable = PageRequest.of(page, searchCriteria.getPageSize());
    
    SearchQuery query = new NativeSearchQueryBuilder()
        .withQuery(
            QueryBuilders.multiMatchQuery(searchCriteria.getTerm())
            .field("code", 2f).field("name", 1.5f)
            .field("synonyms.name", 1f).field("synonyms.type", 1f).field("synonyms.source", 1f)
            .field("definitions.definition", 1f).field("definitions.type", 1f).field("definitions.source", 1f)
            .field("properties.type", 1f).field("properties.value", 1f)
            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
          )
        .withIndices(ElasticOperationsService.CONCEPT_INDEX)
        .withTypes(ElasticOperationsService.CONCEPT_TYPE)
        .withPageable(pageable)
        .build();
    
    Page<Concept> resultPage = operations.queryForPage(query, Concept.class);
    
//    logger.info("results: {}", resultPage.getContent());
    
    ConceptResultList result = new ConceptResultList();
    result.setConcepts(resultPage.getContent());
    result.setTotal(Long.valueOf(resultPage.getTotalElements()).intValue());
    
    return result;
  }

}
