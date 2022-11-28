
package gov.nih.nci.evs.api.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
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
import gov.nih.nci.evs.api.model.Terminology;
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
  public ConceptResultList search(List<Terminology> terminologies, SearchCriteria searchCriteria)
    throws Exception {
    int page = searchCriteria.getFromRecord() / searchCriteria.getPageSize();
    // PageRequest.of(page, searchCriteria.getPageSize());

    // handle fromRecord offset from pageSize (e.g. fromRecord=6, pageSize=10)
    final int fromOffset = searchCriteria.getFromRecord() % searchCriteria.getPageSize();
    final int esFromRecord = searchCriteria.getFromRecord() - fromOffset;
    final int esPageSize =
        fromOffset == 0 ? searchCriteria.getPageSize() : (searchCriteria.getPageSize() * 2);
    final int fromIndex = searchCriteria.getFromRecord() % searchCriteria.getPageSize();
    final int toIndex = fromIndex + searchCriteria.getPageSize();

    final Pageable pageable = new EVSPageable(page, esPageSize, esFromRecord);

    // Escape the term in case it has special characters
    final String term = escape(searchCriteria.getTerm());
    logger.debug("query string [{}]", term);

    final BoolQueryBuilder boolQuery = new BoolQueryBuilder();

    if ("".equals(term)) {
      // don't create anything
    } else {
      boolQuery.must(getTermQuery(searchCriteria.getType(), term));
    }

    // Append concept status clause
    boolQuery.should(QueryBuilders.matchQuery("conceptStatus", "Retired_Concept").boost(-200f));

    // append terminology query
    final QueryBuilder terminologyQuery = getTerminologyQuery(terminologies);
    if (terminologyQuery != null) {
      boolQuery.must(terminologyQuery);
    }

    // append criteria queries
    final List<QueryBuilder> criteriaQueries = getCriteriaQueries(terminologies, searchCriteria);
    if (!CollectionUtils.isEmpty(criteriaQueries)) {
      for (final QueryBuilder criteriaQuery : criteriaQueries) {
        boolQuery.must(criteriaQuery);
      }
    }

    // build final search query
    final NativeSearchQueryBuilder searchQuery = new NativeSearchQueryBuilder().withQuery(boolQuery)
        .withIndices(buildIndicesArray(searchCriteria))
        .withTypes(ElasticOperationsService.CONCEPT_TYPE).withPageable(pageable);
    // .withSourceFilter(new FetchSourceFilter(new String[] {
    // "name", "code", "leaf", "terminology", "version"
    // }, null));

    // avoid setting min score
    // .withMinScore(0.01f);

    if (searchCriteria.getInclude().toLowerCase().contains("highlights")) {
      searchQuery.withHighlightFields(new HighlightBuilder.Field("*"));
    }

    // Sort by score, but then by code
    if (searchCriteria.getSort() != null) {
      // Default is ascending if not specified
      if (searchCriteria.getAscending() == null || searchCriteria.getAscending()) {
        searchQuery.withSort(SortBuilders.fieldSort(searchCriteria.getSort()).order(SortOrder.ASC));
      } else {
        searchQuery
            .withSort(SortBuilders.fieldSort(searchCriteria.getSort()).order(SortOrder.DESC));
      }

    } else {
      searchQuery.withSort(SortBuilders.scoreSort())
          .withSort(SortBuilders.fieldSort("code").order(SortOrder.ASC));
    }

    // query on operations
    final Page<Concept> resultPage = operations.queryForPage(searchQuery.build(), Concept.class,
        new EVSConceptResultMapper(searchCriteria.computeIncludeParam()));

    logger.debug("result count: {}", resultPage.getTotalElements());

    final ConceptResultList result = new ConceptResultList();
    result.setParameters(searchCriteria);
    if (fromOffset == 0) {
      result.setConcepts(resultPage.getContent());
    } else {
      // Handle non-aligned paging (e.g fromRecord=6, pageSize=10)
      final List<Concept> results = resultPage.getContent();
      // if fromIndex is beyond the end of results, bail
      if (fromIndex >= results.size()) {
        result.setConcepts(new ArrayList<>());
      } else {
        result.setConcepts(results.subList(fromIndex, Math.min(toIndex, results.size())));
      }
    }
    result.setTotal(Long.valueOf(resultPage.getTotalElements()).intValue());

    return result;
  }

  /**
   * Returns the term query.
   *
   * @param type the type
   * @param term the term
   * @return the term query
   * @throws Exception the exception
   */
  private BoolQueryBuilder getTermQuery(final String type, final String term) throws Exception {
    switch (type) {
      case "contains":
      case "phrase":
      case "OR":
        return getContainsQuery(type, term, false, false);
      case "match":
        return getMatchQuery(term, false);
      case "startsWith":
        return getMatchQuery(term, true);
      case "fuzzy":
        return getContainsQuery(type, term, true, false);
      case "AND":
        return getContainsQuery(type, term, false, true);
      default:
        // Default to contains query
        return getContainsQuery(type, term, false, false);
    }
  }

  /**
   * Returns the contains query.
   *
   * @param type the type
   * @param term the term
   * @param fuzzyFlag the fuzzy flag
   * @param andFlag the and flag
   * @return the contains query
   * @throws Exception the exception
   */
  private BoolQueryBuilder getContainsQuery(final String type, final String term, boolean fuzzyFlag,
    boolean andFlag) throws Exception {

    // Normalized term (no punctuation)
    final String normTerm = ConceptUtils.normalize(term);
    final String fixterm = updateTermForType(term, type);
    final String fixNormTerm = updateTermForType(normTerm, type);
    final String codeTerm = term.toUpperCase();

    // prepare query_string query builder
    final QueryStringQueryBuilder synonymNamesQuery =
        QueryBuilders.queryStringQuery(fixNormTerm).field("synonyms.name", 10f);
    final QueryStringQueryBuilder definitionQuery =
        QueryBuilders.queryStringQuery(fixNormTerm).field("definitions.definition");
    // name is Text indexed - for looser matching
    final QueryStringQueryBuilder nameQuery1 =
        QueryBuilders.queryStringQuery(fixterm).field("name", 20f);
    final QueryStringQueryBuilder nameQuery2 =
        QueryBuilders.queryStringQuery(fixNormTerm).field("name", 20f);

    // -- fuzzy case
    if ("fuzzy".equalsIgnoreCase(type)) {
      synonymNamesQuery.fuzziness(Fuzziness.ONE);
      definitionQuery.fuzziness(Fuzziness.ONE);
      nameQuery1.fuzziness(Fuzziness.ONE);
      nameQuery2.fuzziness(Fuzziness.ONE);
    }

    // -- wildcard search is assumed to be a term search or phrase search
    if ("AND".equalsIgnoreCase(type)) {
      synonymNamesQuery.defaultOperator(Operator.AND);
      definitionQuery.defaultOperator(Operator.AND);
      nameQuery1.defaultOperator(Operator.AND);
      nameQuery2.defaultOperator(Operator.AND);
    }

    // Set "best fields" and fields
    synonymNamesQuery.type(Type.BEST_FIELDS);
    definitionQuery.type(Type.BEST_FIELDS);

    final QueryStringQueryBuilder codeQuery =
        QueryBuilders.queryStringQuery(codeTerm).field("code", 20f);

    // prepare bool query
    BoolQueryBuilder termQuery = new BoolQueryBuilder();

    // For "contains", also search without wildcards, boost higher
    // if ("contains".equalsIgnoreCase(type)) {
    // termQuery
    // // Boost name matches
    // .should(QueryBuilders.queryStringQuery(term).field("name", 30f))
    // // Boost synonym name matches
    // .should(QueryBuilders.nestedQuery("synonyms",
    // QueryBuilders.queryStringQuery(term).field("synonyms.name", 25f),
    // ScoreMode.Max));
    // }

    termQuery
        // Boost exact matches on norm name
        .should(QueryBuilders.matchQuery("normName", normTerm).boost(40f))
        .should(QueryBuilders.nestedQuery("synonyms",
            QueryBuilders.matchQuery("synonyms.normName", normTerm).boost(40f), ScoreMode.Max))

        // Name query
        .should(nameQuery1).should(nameQuery2);

    // // If not phrase query, do a name search also on the fixed norm name.
    // if (!"phrase".equalsIgnoreCase(type)) {
    // termQuery.should(nameQuery2)
    // .should(QueryBuilders.nestedQuery("synonyms", nameQuery4,
    // ScoreMode.Max));
    // }

    termQuery
        // Code match
        .should(codeQuery)
        .should(QueryBuilders.nestedQuery("synonyms",
            QueryBuilders.queryStringQuery(codeTerm).field("synonyms.code", 20f), ScoreMode.Max))
        // Synonyms generally 10f
        .should(QueryBuilders.nestedQuery("synonyms", synonymNamesQuery, ScoreMode.Max))
        // definitions generally 1f
        .should(QueryBuilders.nestedQuery("definitions", definitionQuery, ScoreMode.Max));

    return termQuery;
  }

  /**
   * Returns the match query.
   *
   * @param term the term
   * @param startsWithFlag the starts with flag
   * @return the match query
   * @throws Exception the exception
   */
  private BoolQueryBuilder getMatchQuery(final String term, final boolean startsWithFlag)
    throws Exception {

    // Normalized term with escaped spaces for exact matching
    final String normTerm = escape(ConceptUtils.normalize(term)) + (startsWithFlag ? "*" : "");
    final String codeTerm = term.toUpperCase() + (startsWithFlag ? "*" : "");
    final String exactTerm = term.replace(" ", "\\ ");
    final String exactNormTerm = normTerm.replace(" ", "\\ ");

    // "Exact" clauses (with normTerm)
    // Only search name, code, and synonyms
    BoolQueryBuilder termQuery =
        // One of the following things
        new BoolQueryBuilder()
            // exact "normName" match
            .should(QueryBuilders.queryStringQuery(exactNormTerm).field("normName")
                .analyzeWildcard(startsWithFlag).boost(20f))
            // exact "synonyms.normName" match
            .should(QueryBuilders.nestedQuery("synonyms",
                QueryBuilders.queryStringQuery(exactNormTerm).field("synonyms.normName").boost(20f)
                    .analyzeWildcard(startsWithFlag),
                ScoreMode.Max))
            // exact match to code (uppercase the code)
            .should(QueryBuilders.queryStringQuery(codeTerm).field("code")
                .analyzeWildcard(startsWithFlag).boost(20f));

    // If startsWith flag, boost exact matches to the top
    if (startsWithFlag) {
      termQuery = termQuery
          .should(
              QueryBuilders.matchQuery("normName", ConceptUtils.normalize(exactTerm)).boost(40f))
          .should(QueryBuilders.queryStringQuery(exactTerm.toUpperCase()).field("code").boost(40f));
    }
    return termQuery;
  }

  /**
   * Escape.
   *
   * @param s the s
   * @return the string
   */
  public static String escape(String s) {
    if (s == null) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      // These characters are part of the query syntax and must be escaped
      if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
          || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
          || c == '*' || c == '?' || c == '|' || c == '&' || c == '/') {
        sb.append('\\');
      }
      sb.append(c);
    }

    // Escape "and", "or", and "not" - escape each char of the word
    final String q1 = sb.toString();
    final StringBuilder sb2 = new StringBuilder();
    boolean first = true;
    for (final String word : q1.split(" ")) {
      if (!first) {
        sb2.append(" ");
      }
      first = false;
      if (word.toLowerCase().matches("(and|or|not)")) {
        for (final String c : word.split("")) {
          sb2.append("\\").append(c);
        }
      } else {
        sb2.append(word);
      }
    }
    return sb2.toString();
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
      case "contains":
        result = updateTokens(term, '*');
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
  private QueryBuilder getTerminologyQuery(final List<Terminology> terminologies) {
    if (CollectionUtils.isEmpty(terminologies)) {
      return null;
    }

    BoolQueryBuilder terminologyQuery = QueryBuilders.boolQuery();

    if (terminologies.size() == 1) {
      terminologyQuery = terminologyQuery
          .must(QueryBuilders.matchQuery("terminology", terminologies.get(0).getTerminology()));
    } else {
      for (Terminology terminology : terminologies) {
        terminologyQuery = terminologyQuery
            .should(QueryBuilders.matchQuery("terminology", terminology.getTerminology()));
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
  private List<QueryBuilder> getCriteriaQueries(List<Terminology> terminologies,
    SearchCriteria searchCriteria) {
    List<QueryBuilder> queries = new ArrayList<>();

    // concept status
    QueryBuilder conceptStatusQuery = getConceptStatusQueryBuilder(searchCriteria);
    if (conceptStatusQuery != null) {
      queries.add(conceptStatusQuery);
    }

    // property
    QueryBuilder propertyQuery = getPropertyValueQueryBuilder(searchCriteria);
    if (propertyQuery != null) {
      queries.add(propertyQuery);
    }

    // synonym source
    QueryBuilder synonymSourceQuery = getSynonymSourceQueryBuilder(searchCriteria);

    // synonym termType
    QueryBuilder synonymTermTypeQuery = getSynonymTermTypeQueryBuilder(searchCriteria);

    if (synonymSourceQuery != null && synonymTermTypeQuery != null) {
      // synonym termType + source search clause
      QueryBuilder synonymTermTypeAndSourceQuery =
          getSynonymTermTypeAndSourceQueryBuilder(searchCriteria);
      queries.add(synonymTermTypeAndSourceQuery);
    } else if (synonymSourceQuery != null) {
      queries.add(synonymSourceQuery);
    } else if (synonymTermTypeQuery != null) {
      queries.add(synonymTermTypeQuery);
    }

    // synonym type
    QueryBuilder synonymTypeQuery = getSynonymTypeQueryBuilder(terminologies, searchCriteria);
    if (synonymTypeQuery != null) {
      queries.add(synonymTypeQuery);
    }

    // definition source
    QueryBuilder definitionSourceQuery = getDefinitionSourceQueryBuilder(searchCriteria);
    if (definitionSourceQuery != null) {
      queries.add(definitionSourceQuery);
    }

    // definition type
    QueryBuilder definitionTypeQuery = getDefinitionTypeQueryBuilder(terminologies, searchCriteria);
    if (definitionTypeQuery != null) {
      queries.add(definitionTypeQuery);
    }

    QueryBuilder subsetQuery = getSubsetValueQueryBuilder(searchCriteria);
    if (subsetQuery != null) {
      queries.add(subsetQuery);
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
  private QueryBuilder getConceptStatusQueryBuilder(SearchCriteria searchCriteria) {
    List<String> values = searchCriteria.getConceptStatus();

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

    // TODO: this shouldn't be hardcoded, but need to read from the particular
    // terminology
    // we are searching, or otherwise make this a top-level field of "Concept"
    // bool query to match property.type and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.matchQuery("properties.type", "Concept_Status")).must(inQuery);

    // nested query on properties
    return QueryBuilders.nestedQuery("properties", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for subset criteria on value field for given types.
   *
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getSubsetValueQueryBuilder(SearchCriteria searchCriteria) {
    if (searchCriteria.getSubset().size() == 0)
      return null;

    List<String> subsets = searchCriteria.getSubset();
    BoolQueryBuilder subsetListQuery = QueryBuilders.boolQuery();

    if (subsets.size() == 1) {
      subsetListQuery = subsetListQuery
          .must(QueryBuilders.matchQuery("associations.relatedCode", subsets.get(0)));
    } else {
      for (String subset : subsets) {
        subsetListQuery =
            subsetListQuery.should(QueryBuilders.matchQuery("associations.relatedCode", subset));
      }
    }

    BoolQueryBuilder subsetQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.matchQuery("associations.type", "Concept_In_Subset"))
        .must(subsetListQuery);

    return QueryBuilders.nestedQuery("associations", subsetQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for property criteria on type or code field
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getPropertyValueQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getProperty()))
      return null;

    boolean hasValue = !StringUtils.isBlank(searchCriteria.getValue());

    // Match value (optional)
    MatchQueryBuilder valueQuery = null;
    if (hasValue) {
      valueQuery = QueryBuilders.matchQuery("properties.value", searchCriteria.getValue());
    }

    BoolQueryBuilder typeQuery = QueryBuilders.boolQuery();
    // Iterate through properties and build up parts
    for (String property : searchCriteria.getProperty()) {

      typeQuery = typeQuery.should(QueryBuilders.matchQuery("properties.type", property))
          .should(QueryBuilders.matchQuery("properties.code", property));

    }

    // bool query to match (property.type or property.code) and property.value
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery().must(typeQuery);
    if (hasValue) {
      fieldBoolQuery.must(valueQuery);
    }

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
   * Returns the synonym type query builder.
   *
   * @param terminologies the terminologies
   * @param searchCriteria the search criteria
   * @return the synonym type query builder
   */
  private QueryBuilder getSynonymTypeQueryBuilder(List<Terminology> terminologies,
    SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymType()))
      return null;

    // bool query to match synonym.type
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();

    // IN query on synonym.type
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    for (String type : searchCriteria.getSynonymType()) {
      inQuery = inQuery.should(QueryBuilders.matchQuery("synonyms.type", type));
      inQuery = inQuery.should(QueryBuilders.matchQuery("synonyms.typeCode", type));
      // ONLY search on synonym type
      // for (final Terminology terminology : terminologies) {
      // if (terminology.getMetadata().getPropertyName(source) != null) {
      // inQuery = inQuery.should(QueryBuilders.matchQuery("synonyms.type",
      // terminology.getMetadata().getPropertyName(source)));
      // }
      // }
    }
    fieldBoolQuery = fieldBoolQuery.must(inQuery);

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
   * Returns the definition type query builder.
   *
   * @param searchCriteria the search criteria
   * @return the definition type query builder
   */
  private QueryBuilder getDefinitionTypeQueryBuilder(List<Terminology> terminologies,
    SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getDefinitionType()))
      return null;

    // bool query to match definition.type
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();

    // IN query on definition.type
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
    for (String type : searchCriteria.getDefinitionType()) {
      inQuery = inQuery.should(QueryBuilders.matchQuery("definitions.type", type));
      inQuery = inQuery.should(QueryBuilders.matchQuery("definitions.code", type));
      // Only search on definition type
      // for (final Terminology terminology : terminologies) {
      // if (terminology.getMetadata().getPropertyName(type) != null) {
      // inQuery = inQuery.should(QueryBuilders.matchQuery("definitions.type",
      // terminology.getMetadata().getPropertyName(type)));
      // }
      // }
    }
    fieldBoolQuery = fieldBoolQuery.must(inQuery);

    // nested query on properties
    return QueryBuilders.nestedQuery("definitions", fieldBoolQuery, ScoreMode.Total);

  }

  /**
   * builds nested query for synonym term type criteria
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getSynonymTermTypeQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymTermType()))
      return null;

    // bool query to match synonym.termType
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getSynonymTermType().size() == 1) {
      fieldBoolQuery = fieldBoolQuery.must(QueryBuilders.matchQuery("synonyms.termType",
          searchCriteria.getSynonymTermType().get(0)));
    } else {
      // IN query on synonym.termType
      BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
      for (String source : searchCriteria.getSynonymTermType()) {
        inQuery = inQuery.should(QueryBuilders.matchQuery("synonyms.termType", source));
      }
      fieldBoolQuery = fieldBoolQuery.must(inQuery);
    }

    // nested query on properties
    return QueryBuilders.nestedQuery("synonyms", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for synonym term type + source criteria
   * 
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getSynonymTermTypeAndSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymTermType()))
      return null;

    // bool query to match synonym.termType
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getSynonymTermType().size() == 1) {
      fieldBoolQuery = fieldBoolQuery.must(QueryBuilders.matchQuery("synonyms.termType",
          searchCriteria.getSynonymTermType().get(0)));
    } else {
      // IN query on synonym.termType
      BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
      for (String source : searchCriteria.getSynonymTermType()) {
        inQuery = inQuery.should(QueryBuilders.matchQuery("synonyms.termType", source));
      }
      fieldBoolQuery = fieldBoolQuery.must(inQuery);
    }

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
    // logger.info("indices array: " + Arrays.asList(indices));
    return indices;
  }

}
