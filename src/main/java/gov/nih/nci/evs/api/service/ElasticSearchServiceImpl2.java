package gov.nih.nci.evs.api.service;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
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

    QueryStringQueryBuilder queryStringQueryBuilder = 
        QueryBuilders.queryStringQuery(updateTermForType(searchCriteria.getTerm(), searchCriteria.getType()));
    
    boolean isWildcard = "startswith".equalsIgnoreCase(searchCriteria.getType());
    
    if ("fuzzy".equalsIgnoreCase(searchCriteria.getType())) {
      queryStringQueryBuilder = queryStringQueryBuilder.fuzziness(Fuzziness.ONE);
    } else if (isWildcard) {
      queryStringQueryBuilder = queryStringQueryBuilder.analyzeWildcard(true);
    }
    
    if ("AND".equalsIgnoreCase(searchCriteria.getType()) || isWildcard) {
      queryStringQueryBuilder = queryStringQueryBuilder.defaultOperator(Operator.AND);
    }
    
    queryStringQueryBuilder = queryStringQueryBuilder.type(Type.BEST_FIELDS);
    
    SearchQuery query = new NativeSearchQueryBuilder()
    .withQuery(
        new BoolQueryBuilder()
        .should(queryStringQueryBuilder.boost(10f))
        .should(QueryBuilders.nestedQuery("properties", queryStringQueryBuilder, ScoreMode.Total).boost(2f))
        .should(QueryBuilders.nestedQuery("definitions", queryStringQueryBuilder, ScoreMode.Total).boost(4f))
        .should(QueryBuilders.nestedQuery("synonyms", queryStringQueryBuilder, ScoreMode.Total).boost(8f))
      )
    .withIndices(ElasticOperationsService.CONCEPT_INDEX)
    .withTypes(ElasticOperationsService.CONCEPT_TYPE)
    .withPageable(pageable)
    .build();
    
    //query on operations
    Page<Concept> resultPage = operations.queryForPage(query, Concept.class);

    logger.debug("result count: {}", resultPage.getTotalElements());
    
    ConceptResultList result = new ConceptResultList();
    result.setConcepts(resultPage.getContent());
    result.setTotal(Long.valueOf(resultPage.getTotalElements()).intValue());
    
    return result;
  }

  private String updateTermForType(String term, String type) {
    if (StringUtils.isBlank(term) || StringUtils.isBlank(type)) return term;
    String result = null;
    
    switch (type.toLowerCase()) {
      case "fuzzy":
        result = updateTokens(term, '~');
        break;
      case "startswith":
        result = term + "*";
        break;
      case "phrase":
        result = "\"" + term + "\"";
        break;
      default:
        result = term;
        break;
    }

    logger.info("updated term - {}", result);
    
    return result;
  }
  
  private String updateTokens(String term, Character modifier) {
    String[] tokens = term.split("\\s+");
    StringBuilder builder = new StringBuilder();
    
    for(String token: tokens) {
      if (builder.length() > 0) builder.append(" ");
      builder.append(token).append(modifier);
    }
    
    return builder.toString();
  } 
}
