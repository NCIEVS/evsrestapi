package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
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
import gov.nih.nci.evs.api.repository.ConceptRepository;
import gov.nih.nci.evs.api.support.SearchCriteria;

@Service(value = "elasticSearchServiceImpl2")
public class ElasticSearchServiceImpl2 implements ElasticSearchService {

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(ElasticSearchServiceImpl2.class);
  
  /** The Elasticsearch operations **/
  @Autowired
  ElasticsearchOperations operations;
  
  @Autowired
  ConceptRepository conceptRepo;
  
  @Override
  public void initSettings() throws IOException, HttpClientErrorException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public ConceptResultList search(SearchCriteria searchCriteria) throws IOException, HttpClientErrorException {
    int page = searchCriteria.getFromRecord() / searchCriteria.getPageSize();
    Pageable pageable = PageRequest.of(page, searchCriteria.getPageSize());
    
    //query_string query
    logger.info("query string [{}]", searchCriteria.getTerm());
    
    QueryStringQueryBuilder queryBuilder = QueryBuilders.queryStringQuery(searchCriteria.getTerm())
        .type(Type.BEST_FIELDS)
        .defaultOperator(Operator.AND)
        .analyzeWildcard(true)
        .fuzziness(Fuzziness.ONE);
    
    SearchQuery query = new NativeSearchQueryBuilder()        
    .withQuery(
        new BoolQueryBuilder()
        .should(queryBuilder.boost(5f))
        .should(QueryBuilders.nestedQuery("properties", queryBuilder, ScoreMode.None))
        .should(QueryBuilders.nestedQuery("definitions", queryBuilder, ScoreMode.None))
        .should(QueryBuilders.nestedQuery("synonyms", queryBuilder, ScoreMode.None))
      )
    .withIndices(ElasticOperationsService.CONCEPT_INDEX)
    .withTypes(ElasticOperationsService.CONCEPT_TYPE)
    .withPageable(pageable)
    .build();
    
    //query on operations
    Page<Concept> resultPage = operations.queryForPage(query, Concept.class);

    logger.info("result count: {}", resultPage.getTotalElements());
    
    //query using repo
//    Page<Concept> resultPage = conceptRepo.search(searchCriteria.getTerm(), pageable);
    
//    logger.info("results: {}", resultPage.getContent());
    
    ConceptResultList result = new ConceptResultList();
    result.setConcepts(resultPage.getContent());
    result.setTotal(Long.valueOf(resultPage.getTotalElements()).intValue());
    
    return result;
  }

}
