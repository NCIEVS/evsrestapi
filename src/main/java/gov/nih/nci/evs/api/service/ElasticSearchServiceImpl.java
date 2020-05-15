package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.support.SearchCriteria;

@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(ElasticSearchServiceImpl.class);
  
  /** The Elasticsearch operations **/
  @Autowired
  ElasticsearchOperations operations;

  /**
   * search for the given search criteria
   * 
   * @param searchCriteria the search criteria
   * @return the result list with concepts
   */
  @Override
  public ConceptResultList search(SearchCriteria searchCriteria) throws IOException, HttpClientErrorException {
    int page = searchCriteria.getFromRecord() / searchCriteria.getPageSize();
    Pageable pageable = new EVSPageable(page, searchCriteria.getPageSize(), searchCriteria.getFromRecord());//PageRequest.of(page, searchCriteria.getPageSize());
    
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
    
    List<QueryBuilder> criteriaQueries = buildCriteriaQueries(searchCriteria);
    if (!CollectionUtils.isEmpty(criteriaQueries)) {
      for(QueryBuilder query: criteriaQueries) {
        boolQuery = boolQuery.must(query);
      }
    }
    
    //search query
    NativeSearchQueryBuilder searchQuery = new NativeSearchQueryBuilder()
      .withQuery(boolQuery)
      .withIndices(ElasticOperationsService.CONCEPT_INDEX)
      .withTypes(ElasticOperationsService.CONCEPT_TYPE)
      .withPageable(pageable);
    
    if ("highlights".equalsIgnoreCase(searchCriteria.getInclude())) {
      searchQuery = searchQuery.withHighlightFields(new HighlightBuilder.Field("*"));
    }
    
    //query on operations
    Page<Concept> resultPage = operations.queryForPage(searchQuery.build(), Concept.class, new EVSConceptResultMapper());
    
    logger.debug("result count: {}", resultPage.getTotalElements());
    
    ConceptResultList result = new ConceptResultList();
    result.setParameters(searchCriteria);
    result.setConcepts(resultPage.getContent());
    result.setTotal(Long.valueOf(resultPage.getTotalElements()).intValue());
    
    return result;
  }

  /**
   * update term based on type of search
   * 
   * @param term the term to be updated
   * @param type the type of search
   * @return the updated term
   */
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
  
  /**
   * append each token from a term with given modifier
   * 
   * @param term the term or phrase
   * @param modifier the modifier
   * @return the modified term or phrase
   */
  private String updateTokens(String term, Character modifier) {
    String[] tokens = term.split("\\s+");
    StringBuilder builder = new StringBuilder();
    
    for(String token: tokens) {
      if (builder.length() > 0) builder.append(" ");
      builder.append(token).append(modifier);
    }
    
    return builder.toString();
  }
  
  /**
   * builds list of queries for field specific criteria from the search criteria
   * 
   * @param searchCriteria the search criteria
   * @return list of nested queries
   */
  private List<QueryBuilder> buildCriteriaQueries(SearchCriteria searchCriteria) {
    List<QueryBuilder> queries = new ArrayList<>();
    
    //concept status
    QueryBuilder conceptStatusQuery = getPropertyTypeValueQueryBuilder(searchCriteria, "concept_status");
    if (conceptStatusQuery != null) {
      queries.add(conceptStatusQuery);
    }

    //contributing source
    QueryBuilder contributingSourceQuery = getPropertyTypeValueQueryBuilder(searchCriteria, "contributing_source");
    if (contributingSourceQuery != null) {
      queries.add(contributingSourceQuery);
    }

    //property
    QueryBuilder propertyQuery = getPropertyTypeCodeQueryBuilder(searchCriteria);
    if (propertyQuery != null) {
      queries.add(propertyQuery);
    }

    //synonym source
    QueryBuilder synonymSourceQuery = getSynonymSourceQueryBuilder(searchCriteria);
    if (synonymSourceQuery != null) {
      queries.add(synonymSourceQuery);
    }

    //definition source
    QueryBuilder definitionSourceQuery = getDefinitionSourceQueryBuilder(searchCriteria);
    if (definitionSourceQuery != null) {
      queries.add(definitionSourceQuery);
    }
    
    //synonym source
    QueryBuilder synonymTermGroupQuery = buildSynonymTermGroupQueryBuilder(searchCriteria);
    if (synonymTermGroupQuery != null) {
      queries.add(synonymTermGroupQuery);
    }
    
    return queries;
  }
  
  /**
   * builds nested query for property criteria on value field for given types
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getPropertyTypeValueQueryBuilder(SearchCriteria searchCriteria, String type) {
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
    
    //IN query on property.type
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    
    for(String property: searchCriteria.getProperty()) {
      inQuery = inQuery.should(QueryBuilders.matchQuery("properties.value", property));
    }
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.matchQuery("properties.type", type))
        .must(inQuery);
    
    //nested query on properties
    return QueryBuilders.nestedQuery("properties", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for property criteria on type field
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getPropertyTypeCodeQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getProperty())) return null;
    
    //IN query on property.type
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    
    for(String property: searchCriteria.getProperty()) {
      inQuery = inQuery
          .should(QueryBuilders.matchQuery("properties.type", property))
          .should(QueryBuilders.matchQuery("properties.code", property));
    }
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(inQuery);
    
    //nested query on properties
    return QueryBuilders.nestedQuery("properties", fieldBoolQuery, ScoreMode.Total);
  }
  
  /**
   * builds nested query for synonym source criteria
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getSynonymSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymSource())) return null;
    
    //IN query on synonym.source
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    
    for(String source: searchCriteria.getSynonymSource()) {
      inQuery = inQuery.should(QueryBuilders.matchQuery("synonyms.source", source));
    }
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(inQuery);
    
    //nested query on properties
    return QueryBuilders.nestedQuery("synonyms", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for definition source criteria
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getDefinitionSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getDefinitionSource())) return null;
    
    //IN query on definition.source
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    
    for(String source: searchCriteria.getDefinitionSource()) {
      inQuery = inQuery.should(QueryBuilders.matchQuery("definitions.source", source));
    }
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(inQuery);
    
    //nested query on properties
    return QueryBuilders.nestedQuery("definitions", fieldBoolQuery, ScoreMode.Total);
  }
  
  /**
   * builds nested query for synonym term group criteria
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder buildSynonymTermGroupQueryBuilder(SearchCriteria searchCriteria) {
    if (StringUtils.isEmpty(searchCriteria.getSynonymTermGroup())) return null;
    
    //bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.matchQuery("synonyms.termGroup", searchCriteria.getSynonymTermGroup()));
    
    //nested query on properties
    return QueryBuilders.nestedQuery("synonyms", fieldBoolQuery, ScoreMode.Total);
  }
  
  /**
   * Custom pageable implementation to support arbitrary offset (work in progress)
   * 
   * @author Arun
   *
   */
  private class EVSPageable extends PageRequest {
    private int offset;
    public EVSPageable(int page, int size, int offset){
      super(page, size, Sort.unsorted());
      this.offset=offset;
    }
    
    @Override
    public long getOffset(){
      logger.info("EVSPageable::getOffset() - {}", offset);
      return this.offset;
    }
    
    //this is a hack to work around spring data bug
    @Override
    public int getPageNumber() {
      return offset / super.getPageSize();
    }

    @Override
    public int getPageSize() {
      return super.getPageSize();
    }
  }
  
  /**
   * Custom concept result mapper to extract highlights from search hits
   * 
   * @author Arun
   *
   */
  private class EVSConceptResultMapper implements SearchResultMapper {
    
    private ObjectMapper mapper;
    
    public EVSConceptResultMapper() {
      this.mapper = new ObjectMapper();
    }
    
    @Override
    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
      if (response.getHits().getHits().length <= 0) {
        return new AggregatedPageImpl(Collections.emptyList(), pageable, 0L);
      }
      
      List<Concept> content = new ArrayList<Concept>();
      for (SearchHit searchHit : response.getHits()) {
        Concept concept = (Concept) mapSearchHit(searchHit, clazz);
        content.add(concept);
      }

      return new AggregatedPageImpl((List<T>) content, pageable, 
          response.getHits().getTotalHits(), response.getAggregations(), 
          response.getScrollId(), response.getHits().getMaxScore());
    }

    @Override
    public <T> T mapSearchHit(SearchHit searchHit, Class<T> clazz) {
      Concept concept = null;
      try {
        concept = mapper.readValue(searchHit.getSourceAsString(), Concept.class);

        Map<String, HighlightField> highlightMap = searchHit.getHighlightFields();
        for (String key : highlightMap.keySet()) {
          HighlightField field = highlightMap.get(key);
          for (Text text : field.getFragments()) {
            String highlight = text.string();
            concept.getHighlights().put(highlight.replaceAll("<em>", "").replaceAll("</em>", ""), highlight);
          }
        }

      } catch (JsonProcessingException e) {
        logger.error(e.getMessage(), e);
      }
      
      return (T) concept;
    }
  }
}
