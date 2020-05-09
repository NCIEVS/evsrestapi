package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
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
import org.springframework.util.CollectionUtils;
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
    
    logger.info("query string [{}]", searchCriteria.getTerm());

    //prepare query_string query builder
    QueryStringQueryBuilder queryStringQueryBuilder = 
        QueryBuilders.queryStringQuery(updateTermForType(searchCriteria.getTerm(), searchCriteria.getType()));
    
    boolean isWildcard = "startswith".equalsIgnoreCase(searchCriteria.getType());
    
    //-- fuzzy and wildcard - both cannot be used for same query 
    if ("fuzzy".equalsIgnoreCase(searchCriteria.getType())) {
      queryStringQueryBuilder = queryStringQueryBuilder.fuzziness(Fuzziness.ONE);
    } else if (isWildcard) {
      queryStringQueryBuilder = queryStringQueryBuilder.analyzeWildcard(true);
    }

    //-- wildcard search is assumed to be a term search or phrase search
    if ("AND".equalsIgnoreCase(searchCriteria.getType()) || isWildcard) {
      queryStringQueryBuilder = queryStringQueryBuilder.defaultOperator(Operator.AND);
    }
    
    queryStringQueryBuilder = queryStringQueryBuilder.type(Type.BEST_FIELDS);

    //prepare bool query
    BoolQueryBuilder boolQuery = new BoolQueryBuilder()
    .should(queryStringQueryBuilder.boost(10f))
    .should(QueryBuilders.nestedQuery("properties", queryStringQueryBuilder, ScoreMode.Total).boost(2f))
    .should(QueryBuilders.nestedQuery("definitions", queryStringQueryBuilder, ScoreMode.Total).boost(4f))
    .should(QueryBuilders.nestedQuery("synonyms", queryStringQueryBuilder, ScoreMode.Total).boost(8f));
    
    QueryBuilder criteriaQuery = prepareCriteriaQueryBuilder(searchCriteria);
    if (criteriaQuery != null) {
      boolQuery = boolQuery.must(criteriaQuery);
    }
    
    SearchQuery query = new NativeSearchQueryBuilder()
    .withQuery(boolQuery)
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
  
  private QueryBuilder prepareCriteriaQueryBuilder(SearchCriteria searchCriteria) {
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    boolean hasCondition = false;
    
    //concept status
    QueryBuilder conceptStatusQuery = getPropertyValueQueryBuilder(searchCriteria, "concept_status");
    if (conceptStatusQuery != null) {
      boolQuery = boolQuery.must(conceptStatusQuery);
      hasCondition = true;
    }

    //contributing source
    QueryBuilder contributingSourceQuery = getPropertyValueQueryBuilder(searchCriteria, "contributing_source");
    if (contributingSourceQuery != null) {
      boolQuery = boolQuery.must(contributingSourceQuery);
      hasCondition = true;
    }

    //property
    QueryBuilder propertyQuery = getPropertyTypeQueryBuilder(searchCriteria);
    if (propertyQuery != null) {
      boolQuery = boolQuery.must(propertyQuery);
      hasCondition = true;
    }

    //synonym source
    QueryBuilder synonymSourceQuery = getSynonymSourceQueryBuilder(searchCriteria);
    if (synonymSourceQuery != null) {
      boolQuery = boolQuery.must(synonymSourceQuery);
      hasCondition = true;
    }

    //definition source
    QueryBuilder definitionSourceQuery = getDefinitionSourceQueryBuilder(searchCriteria);
    if (definitionSourceQuery != null) {
      boolQuery = boolQuery.must(definitionSourceQuery);
      hasCondition = true;
    }
    
    //synonym source
    QueryBuilder synonymTermGroupQuery = getSynonymTermGroupQueryBuilder(searchCriteria);
    if (synonymTermGroupQuery != null) {
      boolQuery = boolQuery.must(synonymTermGroupQuery);
      hasCondition = true;
    }
    
    return hasCondition ? boolQuery : null;//TODO
  }
  
  private QueryBuilder getPropertyValueQueryBuilder(SearchCriteria searchCriteria, String type) {
    List<String> values = null;
    switch (type) {
      case "concept_status":
        values = searchCriteria.getConceptStatus();
        break;
      case "contributing_source":
        values = searchCriteria.getContributingSource();
        break;
      default:
        break;
    }
    
    if (CollectionUtils.isEmpty(values)) return null;
    
    //IN query on property.value
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    
    for(String value: values) {
      inQuery = inQuery.should(QueryBuilders.matchQuery("value", value));
    }
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.matchQuery("type", type))
        .must(inQuery);
    
    //nested query on properties
    return QueryBuilders.nestedQuery("properties", fieldBoolQuery, ScoreMode.Total);
  }

  private QueryBuilder getPropertyTypeQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getProperty())) return null;
    
    //IN query on property.value
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    
    for(String property: searchCriteria.getProperty()) {
      inQuery = inQuery.should(QueryBuilders.matchQuery("type", property));
    }
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(inQuery);
    
    //nested query on properties
    return QueryBuilders.nestedQuery("properties", fieldBoolQuery, ScoreMode.Total);
  }
  
  private QueryBuilder getSynonymSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymSource())) return null;
    
    //IN query on synonym.source
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    
    for(String source: searchCriteria.getSynonymSource()) {
      inQuery = inQuery.should(QueryBuilders.matchQuery("source", source));
    }
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(inQuery);
    
    //nested query on properties
    return QueryBuilders.nestedQuery("synonyms", fieldBoolQuery, ScoreMode.Total);
  }

  private QueryBuilder getDefinitionSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getDefinitionSource())) return null;
    
    //IN query on definition.source
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    
    for(String source: searchCriteria.getDefinitionSource()) {
      inQuery = inQuery.should(QueryBuilders.matchQuery("source", source));
    }
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(inQuery);
    
    //nested query on properties
    return QueryBuilders.nestedQuery("definitions", fieldBoolQuery, ScoreMode.Total);
  }
  
  private QueryBuilder getSynonymTermGroupQueryBuilder(SearchCriteria searchCriteria) {
    if (StringUtils.isEmpty(searchCriteria.getSynonymTermGroup())) return null;
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.matchQuery("termGroup", searchCriteria.getSynonymTermGroup()));
    
    //nested query on properties
    return QueryBuilders.nestedQuery("synonyms", fieldBoolQuery, ScoreMode.Total);
  }
}
