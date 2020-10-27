
package gov.nih.nci.evs.api.service;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.support.es.EVSConceptResultMapper;
import gov.nih.nci.evs.api.support.es.EVSPageable;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;

@Service
public class ElasticSearchServiceImpl implements ElasticSearchService {

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(ElasticSearchServiceImpl.class);

  /** The Elasticsearch operations **/
  @Autowired
  ElasticsearchOperations operations;

  /* The terminology utils */
  @Autowired
  TerminologyUtils termUtils;

  /**
   * search for the given search criteria.
   *
   * @param searchCriteria the search criteria
   * @return the result list with concepts
   * @throws Exception the exception
   */
  @Override
  public ConceptResultList search(SearchCriteria searchCriteria) throws Exception {
    int page = searchCriteria.getFromRecord() / searchCriteria.getPageSize();
    // PageRequest.of(page, searchCriteria.getPageSize());
    Pageable pageable =
        new EVSPageable(page, searchCriteria.getPageSize(), searchCriteria.getFromRecord());

    logger.debug("query string [{}]", searchCriteria.getTerm());

    BoolQueryBuilder boolQuery = new BoolQueryBuilder();

    boolean blankTermFlag = "".equalsIgnoreCase(searchCriteria.getTerm());
    boolean startsWithFlag = "startsWith".equalsIgnoreCase(searchCriteria.getType());
    boolean matchFlag = "match".equalsIgnoreCase(searchCriteria.getType());

    if (blankTermFlag) {
      // don't create anything
    } else if (matchFlag || startsWithFlag) {
      // only search normName in concept/synonym or value in property
      final String normTerm =
          ConceptUtils.normalize(searchCriteria.getTerm()).replaceAll(" ", "\\\\ ")
              + (startsWithFlag ? "*" : "");

      BoolQueryBuilder boolQuery2 =
          new BoolQueryBuilder()
              .should(QueryBuilders
                  .queryStringQuery("normName:" + normTerm).analyzeWildcard(true).boost(20f))
              .should(QueryBuilders
                  .queryStringQuery("code:" + searchCriteria.getTerm().replaceAll(" ", "\\\\ "))
                  .analyzeWildcard(true).boost(20f))
              .should(
                  QueryBuilders.nestedQuery("properties",
                      QueryBuilders.queryStringQuery("properties.value:" + normTerm)
                          .analyzeWildcard(true),
                      ScoreMode.Max).boost(5f))
              .should(QueryBuilders.nestedQuery("synonyms", QueryBuilders
                  .queryStringQuery("synonyms.normName:" + normTerm).analyzeWildcard(true),
                  ScoreMode.Max).boost(20f));

      if (startsWithFlag) {
        // Boost exact name match to top of list
        boolQuery2 = boolQuery2.should(QueryBuilders
            .matchQuery("normName", ConceptUtils.normalize(searchCriteria.getTerm())).boost(40f))
            .should(QueryBuilders
                .queryStringQuery(
                    "code:" + searchCriteria.getTerm().toUpperCase().replaceAll(" ", "\\\\ ") + "*")
                .analyzeWildcard(true).boost(15f));
      }
      boolQuery.must(boolQuery2);
    } else {
      // prepare query_string query builder
      QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders
          .queryStringQuery(updateTermForType(searchCriteria.getTerm(), searchCriteria.getType()));

      // -- fuzzy and wildcard - both cannot be used for same query
      if ("fuzzy".equalsIgnoreCase(searchCriteria.getType())) {
        queryStringQueryBuilder = queryStringQueryBuilder.fuzziness(Fuzziness.ONE);
      }

      // -- wildcard search is assumed to be a term search or phrase search
      if ("AND".equalsIgnoreCase(searchCriteria.getType())) {
        queryStringQueryBuilder = queryStringQueryBuilder.defaultOperator(Operator.AND);
      }

      queryStringQueryBuilder = queryStringQueryBuilder.type(Type.BEST_FIELDS);

      // prepare bool query
      BoolQueryBuilder boolQuery2 = new BoolQueryBuilder()
          .should(QueryBuilders.queryStringQuery(queryStringQueryBuilder.queryString())
              .field("name", 2f).boost(10f)
              .defaultOperator(queryStringQueryBuilder.defaultOperator()))
          .should(QueryBuilders.queryStringQuery(queryStringQueryBuilder.queryString())
              .field("code", 2f).boost(10f)
              .defaultOperator(queryStringQueryBuilder.defaultOperator()))
          .should(QueryBuilders.nestedQuery("properties", queryStringQueryBuilder, ScoreMode.Max)
              .boost(5f))
          .should(QueryBuilders.nestedQuery("synonyms", queryStringQueryBuilder, ScoreMode.Max)
              .boost(20f))
          .should(QueryBuilders.nestedQuery("definitions", queryStringQueryBuilder, ScoreMode.Max));

      boolQuery.must(boolQuery2);
    }

    // append terminology query
    QueryBuilder terminologyQuery = buildTerminologyQuery(searchCriteria);
    if (terminologyQuery != null) {
      boolQuery = boolQuery.must(terminologyQuery);
    }

    // append criteria queries
    List<QueryBuilder> criteriaQueries = buildCriteriaQueries(searchCriteria);
    if (!CollectionUtils.isEmpty(criteriaQueries)) {
      for (QueryBuilder criteriaQuery : criteriaQueries) {
        boolQuery = boolQuery.must(criteriaQuery);
      }
    }

    // build final search query
    NativeSearchQueryBuilder searchQuery = new NativeSearchQueryBuilder().withQuery(boolQuery)
        .withIndices(buildIndicesArray(searchCriteria))
        .withTypes(ElasticOperationsService.CONCEPT_TYPE).withPageable(pageable);
    // avoid setting min score
    // .withMinScore(0.01f);

    if (searchCriteria.getInclude().toLowerCase().contains("highlights")) {
      searchQuery = searchQuery.withHighlightFields(new HighlightBuilder.Field("*"));
    }

    // query on operations
    Page<Concept> resultPage = operations.queryForPage(searchQuery.build(), Concept.class,
        new EVSConceptResultMapper(searchCriteria.computeIncludeParam()));

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
    if (StringUtils.isBlank(term) || StringUtils.isBlank(type))
      return term;
    String result = null;

    switch (type.toLowerCase()) {
      case "fuzzy":
        result = updateTokens(term, '~');
        break;
      case "phrase":
        result = "\"" + term + "\"";
        break;
      default:
        result = term;
        break;
    }

    logger.debug("updated term: {}", result);

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

    for (String token : tokens) {
      if (builder.length() > 0)
        builder.append(" ");
      builder.append(token).append(modifier);
    }

    return builder.toString();
  }

  /**
   * builds terminology query based on input search criteria
   * 
   * @param searchCriteria
   * @return the terminology query builder
   */
  private QueryBuilder buildTerminologyQuery(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getTerminology()))
      return null;

    BoolQueryBuilder terminologyQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getTerminology().size() == 1) {
      terminologyQuery = terminologyQuery
          .must(QueryBuilders.matchQuery("terminology", searchCriteria.getTerminology().get(0)));
    } else {
      for (String terminology : searchCriteria.getTerminology()) {
        terminologyQuery =
            terminologyQuery.should(QueryBuilders.matchQuery("terminology", terminology));
      }
    }

    return terminologyQuery;
  }

  /**
   * builds list of queries for field specific criteria from the search criteria
   * 
   * @param searchCriteria the search criteria
   * @return list of nested queries
   */
  private List<QueryBuilder> buildCriteriaQueries(SearchCriteria searchCriteria) {
    List<QueryBuilder> queries = new ArrayList<>();

    // concept status
    QueryBuilder conceptStatusQuery =
        getPropertyTypeValueQueryBuilder(searchCriteria, "Concept_Status");
    if (conceptStatusQuery != null) {
      queries.add(conceptStatusQuery);
    }

    // property
    QueryBuilder propertyQuery = getPropertyTypeCodeQueryBuilder(searchCriteria);
    if (propertyQuery != null) {
      queries.add(propertyQuery);
    }

    // synonym source
    QueryBuilder synonymSourceQuery = getSynonymSourceQueryBuilder(searchCriteria);
    if (synonymSourceQuery != null) {
      queries.add(synonymSourceQuery);
    }

    // definition source
    QueryBuilder definitionSourceQuery = getDefinitionSourceQueryBuilder(searchCriteria);
    if (definitionSourceQuery != null) {
      queries.add(definitionSourceQuery);
    }

    // synonym termGroup
    QueryBuilder synonymTermGroupQuery = buildSynonymTermGroupQueryBuilder(searchCriteria);
    if (synonymTermGroupQuery != null) {
      queries.add(synonymTermGroupQuery);
    }

    return queries;
  }

  /**
   * builds nested query for property criteria on value field for given types.
   *
   * @param searchCriteria the search criteria
   * @param type the type
   * @return the nested query
   */
  private QueryBuilder getPropertyTypeValueQueryBuilder(SearchCriteria searchCriteria,
    String type) {
    List<String> values = null;
    switch (type.toLowerCase()) {
      case "concept_status":
        values = searchCriteria.getConceptStatus();
        break;
      default:
        values = searchCriteria.getProperty();
        break;
    }

    // If there are no values, bail
    if (CollectionUtils.isEmpty(values)) {
      return null;
    }

    // IN query on property.value
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();

    if (values.size() == 1) {
      inQuery = inQuery.must(QueryBuilders.matchQuery("properties.value", values.get(0)));
    } else {
      for (String property : values) {
        inQuery = inQuery.should(QueryBuilders.matchQuery("properties.value", property));
      }
    }

    // bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.matchQuery("properties.type", type)).must(inQuery);

    // nested query on properties
    return QueryBuilders.nestedQuery("properties", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for property criteria on type or code field
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getPropertyTypeCodeQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getProperty()))
      return null;

    boolean hasTerm = !StringUtils.isBlank(searchCriteria.getTerm());

    // IN query on property.type or property.code
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getProperty().size() == 1) {
      inQuery = inQuery.must(QueryBuilders.boolQuery()
          .should(QueryBuilders.matchQuery("properties.type", searchCriteria.getProperty().get(0)))
          .should(
              QueryBuilders.matchQuery("properties.code", searchCriteria.getProperty().get(0))));

      if (hasTerm) {
        inQuery =
            inQuery.must(QueryBuilders.matchQuery("properties.value", searchCriteria.getTerm()));
      }
    } else {
      for (String property : searchCriteria.getProperty()) {
        inQuery = inQuery.should(
            QueryBuilders.boolQuery().should(QueryBuilders.matchQuery("properties.type", property))
                .should(QueryBuilders.matchQuery("properties.code", property)));

        if (hasTerm) {
          inQuery = inQuery
              .should(QueryBuilders.matchQuery("properties.value", searchCriteria.getTerm()));
        }
      }
    }

    // bool query to match (property.type or property.code) and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery().must(inQuery);

    // nested query on properties
    return QueryBuilders.nestedQuery("properties", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for synonym source criteria
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getSynonymSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymSource()))
      return null;

    // IN query on synonym.source

    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getSynonymSource().size() == 1) {
      fieldBoolQuery = fieldBoolQuery.must(
          QueryBuilders.matchQuery("synonyms.source", searchCriteria.getSynonymSource().get(0)));
    } else {
      BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
      for (String source : searchCriteria.getSynonymSource()) {
        inQuery = inQuery.should(QueryBuilders.matchQuery("synonyms.source", source));
      }
      fieldBoolQuery = fieldBoolQuery.must(inQuery);
    }

    // nested query on properties
    return QueryBuilders.nestedQuery("synonyms", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for definition source criteria
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getDefinitionSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getDefinitionSource()))
      return null;

    // IN query on definition.source
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getDefinitionSource().size() == 1) {
      inQuery = inQuery.must(QueryBuilders.matchQuery("definitions.source",
          searchCriteria.getDefinitionSource().get(0)));
    } else {
      for (String source : searchCriteria.getDefinitionSource()) {
        inQuery = inQuery.should(QueryBuilders.matchQuery("definitions.source", source));
      }
    }

    // bool query to match definition.source
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery().must(inQuery);

    // nested query on properties
    return QueryBuilders.nestedQuery("definitions", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for synonym term group criteria
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder buildSynonymTermGroupQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymTermGroup()))
      return null;

    // bool query to match synonym.termGroup
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getSynonymTermGroup().size() == 1) {
      fieldBoolQuery = fieldBoolQuery.must(QueryBuilders.matchQuery("synonyms.termGroup",
          searchCriteria.getSynonymTermGroup().get(0)));
    } else {
      // IN query on synonym.termGroup
      BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
      for (String source : searchCriteria.getSynonymTermGroup()) {
        inQuery = inQuery.should(QueryBuilders.matchQuery("synonyms.termGroup", source));
      }
      fieldBoolQuery = fieldBoolQuery.must(inQuery);
    }

    // nested query on properties
    return QueryBuilders.nestedQuery("synonyms", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * build array of index names
   * 
   * @param searchCriteria the search criteria
   * @return the array of index names
   * @throws Exception the exception
   */
  private String[] buildIndicesArray(SearchCriteria searchCriteria) throws Exception {
    List<String> terminologies = searchCriteria.getTerminology();

    if (CollectionUtils.isEmpty(searchCriteria.getTerminology())) {
      return new String[] {
          "_all"
      };
    }

    String[] indices = new String[terminologies.size()];
    // TODO: add getTerminologies call to avoid looping
    for (int i = 0; i < terminologies.size(); i++) {
      indices[i] = termUtils.getTerminology(terminologies.get(i), true).getIndexName();
    }
    logger.info("indices array: " + Arrays.asList(indices));
    return indices;
  }

}
