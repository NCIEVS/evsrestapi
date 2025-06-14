package gov.nih.nci.evs.api.service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.MappingResultList;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.support.es.EVSPageable;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.NestedQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.QueryStringQueryBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Reference implementation of {@link OpenSearchService}. Includes hibernate tags for MEME database.
 */
@Service
public class OpenSearchServiceImpl implements OpenSearchService {

  /** The Constant log. */
  private static final Logger logger = LoggerFactory.getLogger(OpenSearchServiceImpl.class);

  /** The Opensearch operations service *. */
  @Autowired OpensearchOperationsService esOperationsService;

  /** The Opensearch operations *. */
  @Autowired OpenSearchOperations operations;

  /** The Opensearch query service *. */
  @Autowired OpensearchQueryService osQueryService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;

  /**
   * search for the given search criteria.
   *
   * @param terminologies the terminologies
   * @param searchCriteria the search criteria
   * @return the result list with concepts
   * @throws Exception the exception
   */
  @Override
  public ConceptResultList findConcepts(
      List<Terminology> terminologies, SearchCriteria searchCriteria) throws Exception {
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

    if ("".equals(term) && searchCriteria.getCodeList().isEmpty()) {
      // don't create anything
    } else {
      boolQuery.must(getTermQuery(searchCriteria, term));
    }

    // Append concept status clause. Boosts active to the top of the list, and maintains inactive at
    // the bottom
    BoolQueryBuilder activeQuery = new BoolQueryBuilder();
    activeQuery.should(QueryBuilders.matchQuery("active", true).boost(100000f));
    activeQuery.should(QueryBuilders.matchQuery("active", false).boost(0.0001f));
    boolQuery.must(activeQuery);

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

    // Get include param
    final IncludeParam include = new IncludeParam(searchCriteria.getInclude());

    // build final search query
    final NativeSearchQueryBuilder searchQuery =
        new NativeSearchQueryBuilder()
            .withQuery(boolQuery)
            .withPageable(pageable)
            .withSourceFilter(
                new FetchSourceFilter(include.getIncludedFields(), include.getExcludedFields()));

    // avoid setting min score
    // .withMinScore(0.01f);

    if (searchCriteria.getInclude().toLowerCase().contains("highlights")) {
      searchQuery.withHighlightFields(new HighlightBuilder.Field("*"));
    }

    // Sort by score, but then by code
    if (searchCriteria.getSort() != null) {
      // Default is ascending if not specified
      if (searchCriteria.getAscending() == null || searchCriteria.getAscending()) {
        searchQuery.withSorts(
            SortBuilders.fieldSort(searchCriteria.getSort()).order(SortOrder.ASC));
      } else {
        searchQuery.withSorts(
            SortBuilders.fieldSort(searchCriteria.getSort()).order(SortOrder.DESC));
      }

    } else {
      searchQuery
          .withSorts(SortBuilders.scoreSort())
          .withSorts(SortBuilders.fieldSort("code").order(SortOrder.ASC));
    }

    // query on operations
    final SearchHits<Concept> hits =
        operations.search(
            searchQuery.build(),
            Concept.class,
            IndexCoordinates.of(buildIndicesArray(searchCriteria)));

    logger.debug("result count: {}", hits.getTotalHits());

    final ConceptResultList result = new ConceptResultList();

    if (hits.getTotalHits() >= 10000) {
      result.setTotal(
          operations.count(
              searchQuery.build(),
              Concept.class,
              IndexCoordinates.of(buildIndicesArray(searchCriteria))));
    } else {
      result.setTotal(hits.getTotalHits());
    }

    result.setParameters(searchCriteria);

    if (fromOffset == 0) {
      result.setConcepts(
          hits.stream()
              .peek(
                  h -> {
                    if (Concept.class.isAssignableFrom(h.getContent().getClass())) {
                      Map<String, List<String>> highlightMap = h.getHighlightFields();
                      // searchQuery.withHighlightFields(new HighlightBuilder.Field("*"));
                      for (String field : highlightMap.keySet()) {
                        // Avoid highlights from "terminology" match
                        if (field.equals("terminology")) {
                          continue;
                        }
                        for (String text : highlightMap.get(field)) {
                          h.getContent()
                              .getHighlights()
                              .put(text.replaceAll("<em>", "").replaceAll("</em>", ""), text);
                        }
                      }
                    }
                  })
              .map(SearchHit::getContent)
              .collect(Collectors.toList()));
    } else {
      final List<Concept> results =
          hits.stream().map(SearchHit::getContent).collect(Collectors.toList());
      if (fromIndex >= results.size()) {
        result.setConcepts(new ArrayList<>());
      } else {
        result.setConcepts(results.subList(fromIndex, Math.min(toIndex, results.size())));
      }
    }
    return result;
  }

  /**
   * Find the concept mappings based on a provided query and search criteria.
   *
   * @param query the query string to search for
   * @param searchCriteria the search criteria
   * @return the result list with concepts
   */
  @Override
  public MappingResultList findConceptMappings(String query, SearchCriteria searchCriteria) {
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

    // build search query from string query
    final NativeSearchQueryBuilder searchQuery =
        new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.queryStringQuery(query))
            .withPageable(pageable);

    if (searchCriteria.getSort() != null) {
      // Default is ascending if not specified
      if (searchCriteria.getAscending() == null || searchCriteria.getAscending()) {
        searchQuery.withSorts(
            SortBuilders.fieldSort(searchCriteria.getSort()).order(SortOrder.ASC));
      } else {
        searchQuery.withSorts(
            SortBuilders.fieldSort(searchCriteria.getSort()).order(SortOrder.DESC));
      }
    }

    if (searchCriteria.getSort() != null) {
      // get sortField, start with default
      String sortField = searchCriteria.getSort();
      if (searchCriteria.getAscending() == null || searchCriteria.getAscending()) {
        searchQuery
            .withSorts(SortBuilders.scoreSort())
            .withSorts(SortBuilders.fieldSort(sortField).order(SortOrder.ASC));
      } else {
        searchQuery
            .withSorts(SortBuilders.scoreSort())
            .withSorts(SortBuilders.fieldSort(sortField).order(SortOrder.DESC));
      }

    } else {
      // default to sortKey
      if (searchCriteria.getAscending() == null || searchCriteria.getAscending()) {
        searchQuery
            .withSorts(SortBuilders.scoreSort())
            .withSorts(SortBuilders.fieldSort("sortKey").order(SortOrder.ASC));
      } else {
        searchQuery
            .withSorts(SortBuilders.scoreSort())
            .withSorts(SortBuilders.fieldSort("sortKey").order(SortOrder.DESC));
      }
    }

    // query on operations
    final SearchHits<Mapping> hits =
        operations.search(
            searchQuery.build(),
            Mapping.class,
            IndexCoordinates.of(OpensearchOperationsService.MAPPINGS_INDEX));

    logger.debug("result count: {}", hits.getTotalHits());

    final MappingResultList result = new MappingResultList();

    if (hits.getTotalHits() >= 10000) {
      result.setTotal(
          operations.count(
              searchQuery.build(),
              Concept.class,
              IndexCoordinates.of(OpensearchOperationsService.MAPPINGS_INDEX)));
    } else {
      result.setTotal(hits.getTotalHits());
    }

    result.setParameters(searchCriteria);

    if (fromOffset == 0) {
      result.setMaps(hits.stream().map(SearchHit::getContent).collect(Collectors.toList()));
    } else {
      final List<Mapping> results =
          hits.stream().map(SearchHit::getContent).collect(Collectors.toList());
      if (fromIndex >= results.size()) {
        result.setMaps(new ArrayList<>());
      } else {
        result.setMaps(results.subList(fromIndex, Math.min(toIndex, results.size())));
      }
    }
    return result;
  }

  /**
   * Returns the term query.
   *
   * @param searchCriteria the search criteria
   * @param term the term
   * @return the term query
   * @throws Exception the exception
   */
  private BoolQueryBuilder getTermQuery(final SearchCriteria searchCriteria, final String term)
      throws Exception {

    final String type = searchCriteria.getType();

    switch (type) {
      case "contains":
      case "phrase":
      case "OR":
        return getContainsQuery(searchCriteria, term, false, false);
      case "match":
        return getMatchQuery(term, false);
      case "startsWith":
        return getMatchQuery(term, true);
      case "fuzzy":
        return getContainsQuery(searchCriteria, term, true, false);
      case "AND":
        return getContainsQuery(searchCriteria, term, false, true);
      default:
        // Default to contains query
        return getContainsQuery(searchCriteria, term, false, false);
    }
  }

  /**
   * Returns the term query for mappings based on concepts.
   *
   * @param conceptCodes the concept codes
   * @param terminology the terminology
   * @return the mappings
   */
  @Override
  public List<Mapping> getConceptMappings(List<String> conceptCodes, String terminology) {
    // must match mapsetCode

    BoolQueryBuilder termQuery = new BoolQueryBuilder();
    if (null != terminology) {
      termQuery =
          new BoolQueryBuilder()
              .must(QueryBuilders.queryStringQuery(terminology).field("source").field("target"));
    }
    // search term processing
    if (conceptCodes != null && conceptCodes.size() > 0) {
      conceptCodes.replaceAll(code -> escape(code));

      termQuery.must(
          QueryBuilders.queryStringQuery(String.join(" OR ", conceptCodes)).field("sourceCode"));
    }

    final NativeSearchQueryBuilder searchQuery =
        new NativeSearchQueryBuilder().withQuery(termQuery);
    final SearchHits<Mapping> hits =
        operations.search(
            searchQuery.build(),
            Mapping.class,
            IndexCoordinates.of(OpensearchOperationsService.MAPPINGS_INDEX));

    logger.debug("result count: {}", hits.getTotalHits());

    final MappingResultList result = new MappingResultList();

    result.setTotal(hits.getTotalHits());

    final List<Mapping> mappings =
        hits.stream().map(SearchHit::getContent).collect(Collectors.toList());

    return mappings;
  }

  /**
   * Returns the contains query.
   *
   * @param searchCriteria the search criteria
   * @param term the term
   * @param fuzzyFlag the fuzzy flag
   * @param andFlag the and flag
   * @return the contains query
   * @throws Exception the exception
   */
  private BoolQueryBuilder getContainsQuery(
      final SearchCriteria searchCriteria, final String term, boolean fuzzyFlag, boolean andFlag)
      throws Exception {

    // Generate variants to search with
    final String type = searchCriteria.getType();
    final String normTerm = ConceptUtils.normalize(term);
    final String stemTerm = ConceptUtils.normalizeWithStemming(term);
    final String fixTerm = updateTermForType(term, type);
    final String fixNormTerm = updateTermForType(normTerm, type);
    final String codeTerm = term.toUpperCase();
    final boolean hasCodeList = !searchCriteria.getCodeList().isEmpty();

    // Unable to do an exact match on "name" as it is a "text" field
    // final MatchQueryBuilder nameQuery = QueryBuilders.matchQuery("name",
    // exactTerm).boost(50f);

    // Exact match queries
    final MatchQueryBuilder normNameQuery =
        QueryBuilders.matchQuery("normName", normTerm).boost(40f);
    final NestedQueryBuilder nestedSynonymNormNameQuery =
        QueryBuilders.nestedQuery(
            "synonyms",
            QueryBuilders.matchQuery("synonyms.normName", normTerm).boost(39f),
            ScoreMode.Max);

    // Boosting matches with words next to each other
    final QueryStringQueryBuilder phraseNormNameQuery =
        QueryBuilders.queryStringQuery("\"" + term + "\"").field("name").boost(30f);
    final NestedQueryBuilder nestedSynonymPhraseNormNameQuery =
        QueryBuilders.nestedQuery(
            "synonyms",
            QueryBuilders.queryStringQuery("\"" + term + "\"").field("synonyms.name").boost(29f),
            ScoreMode.Max);

    // Word queries
    final QueryStringQueryBuilder fixNameQuery =
        QueryBuilders.queryStringQuery(fixTerm).field("name").boost(20f);
    final QueryStringQueryBuilder fixNormNameQuery =
        QueryBuilders.queryStringQuery(fixNormTerm).field("normName").boost(20f);
    final QueryStringQueryBuilder stemNameQuery =
        QueryBuilders.queryStringQuery(stemTerm).field("stemName").boost(15f);

    final QueryStringQueryBuilder synonymFixNameAndQuery =
        QueryBuilders.queryStringQuery(String.join(" AND ", normTerm.split("\\s+")))
            .field("synonyms.name")
            .defaultOperator(Operator.AND)
            .boost(15f);
    final NestedQueryBuilder nestedSynonymFixNameAndQuery =
        QueryBuilders.nestedQuery("synonyms", synonymFixNameAndQuery, ScoreMode.Max);
    final QueryStringQueryBuilder synonymFixNormNameQuery =
        QueryBuilders.queryStringQuery(fixNormTerm).field("synonyms.normName").boost(10f);
    final QueryStringQueryBuilder synonymStemNameQuery =
        QueryBuilders.queryStringQuery(stemTerm).field("synonyms.stemName").boost(7f);
    final NestedQueryBuilder nestedSynonymFixNameQuery =
        QueryBuilders.nestedQuery("synonyms", synonymFixNormNameQuery, ScoreMode.Max);
    final NestedQueryBuilder nestedSynonymStemNameQuery =
        QueryBuilders.nestedQuery("synonyms", synonymStemNameQuery, ScoreMode.Max);

    // Definition query
    final QueryStringQueryBuilder definitionQuery =
        QueryBuilders.queryStringQuery(fixNormTerm).field("definitions.definition");
    final NestedQueryBuilder nestedDefinitionQuery =
        QueryBuilders.nestedQuery("definitions", definitionQuery, ScoreMode.Max);

    String codeList = codeTerm;

    if (hasCodeList) {
      codeList = String.join(" OR ", searchCriteria.getCodeList());
    }

    // Code queries
    final QueryStringQueryBuilder codeQuery =
        QueryBuilders.queryStringQuery(codeList).field("code").boost(50f);
    final NestedQueryBuilder synonymCodeQuery =
        QueryBuilders.nestedQuery(
                "synonyms",
                QueryBuilders.queryStringQuery(codeList).field("synonyms.code"),
                ScoreMode.Max)
            .boost(50f);

    // Name queries

    // -- fuzzy case
    if ("fuzzy".equalsIgnoreCase(type)) {
      fixNameQuery.fuzziness(Fuzziness.ONE);
      fixNormNameQuery.fuzziness(Fuzziness.ONE);
      stemNameQuery.fuzziness(Fuzziness.ONE);
      synonymFixNameAndQuery.fuzziness(Fuzziness.ONE);
      synonymFixNormNameQuery.fuzziness(Fuzziness.ONE);
      synonymStemNameQuery.fuzziness(Fuzziness.ONE);
      definitionQuery.fuzziness(Fuzziness.ONE);
    } else {
      fixNameQuery.fuzziness(Fuzziness.ZERO);
      fixNormNameQuery.fuzziness(Fuzziness.ZERO);
      stemNameQuery.fuzziness(Fuzziness.ZERO);
      synonymFixNameAndQuery.fuzziness(Fuzziness.ZERO);
      synonymFixNormNameQuery.fuzziness(Fuzziness.ZERO);
      synonymStemNameQuery.fuzziness(Fuzziness.ZERO);
      definitionQuery.fuzziness(Fuzziness.ZERO);
    }

    // -- wildcard search is assumed to be a term search or phrase search
    if ("AND".equalsIgnoreCase(type)) {
      fixNameQuery.defaultOperator(Operator.AND);
      fixNormNameQuery.defaultOperator(Operator.AND);
      stemNameQuery.defaultOperator(Operator.AND);
      synonymFixNormNameQuery.defaultOperator(Operator.AND);
      synonymStemNameQuery.defaultOperator(Operator.AND);
      definitionQuery.defaultOperator(Operator.AND);
    }

    // Set "best fields" and fields
    // synonymFixNameQuery.type(Type.BEST_FIELDS);
    // definitionQuery.type(Type.BEST_FIELDS);

    // prepare bool query
    BoolQueryBuilder termQuery = new BoolQueryBuilder();

    // Avoid searching codes with spaces in search query
    if (!term.contains(" ") || hasCodeList) {
      // Code match
      if (hasCodeList) {
        BoolQueryBuilder codeQueries =
            QueryBuilders.boolQuery().should(codeQuery).should(synonymCodeQuery);
        termQuery.must(codeQueries);
      } else {
        termQuery.should(codeQuery).should(synonymCodeQuery);
      }
    }

    // contains, and/or
    if (!"phrase".equals(type.toLowerCase())) {
      termQuery

          // Text query on "name"
          // .should(nameQuery)

          // Text queries on "norm name" and synonym "norm name"
          .should(normNameQuery)
          .should(nestedSynonymNormNameQuery);
    }

    // Use phrase queries with higher boost than fixname queries
    termQuery.should(phraseNormNameQuery).should(nestedSynonymPhraseNormNameQuery);

    // contains, and/or
    if (!"phrase".equals(type.toLowerCase())) {
      termQuery

          // Text queries on "name" and "norm name" and "stem name" using fix names
          .should(fixNameQuery)
          .should(fixNormNameQuery)
          .should(stemNameQuery)

          // Text query on synonym "name" and "stem name" using fix query
          .should(nestedSynonymFixNameAndQuery)
          .should(nestedSynonymFixNameQuery)
          .should(nestedSynonymStemNameQuery)

          // definition match
          .should(nestedDefinitionQuery);
    }
    return (term.isBlank()) ? termQuery : termQuery.minimumShouldMatch(1);
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
            .should(
                QueryBuilders.queryStringQuery(exactNormTerm)
                    .field("normName")
                    .analyzeWildcard(startsWithFlag)
                    .boost(20f))
            // exact "synonyms.normName" match
            .should(
                QueryBuilders.nestedQuery(
                    "synonyms",
                    QueryBuilders.queryStringQuery(exactNormTerm)
                        .field("synonyms.normName")
                        .boost(20f)
                        .analyzeWildcard(startsWithFlag),
                    ScoreMode.Max))
            // exact match to code (uppercase the code)
            .should(
                QueryBuilders.queryStringQuery(codeTerm)
                    .field("code")
                    .analyzeWildcard(startsWithFlag)
                    .boost(20f));

    // If startsWith flag, boost exact matches to the top
    if (startsWithFlag) {
      termQuery =
          termQuery
              .should(
                  QueryBuilders.matchQuery("normName", ConceptUtils.normalize(exactTerm))
                      .boost(40f))
              .should(
                  QueryBuilders.queryStringQuery(exactTerm.toUpperCase()).field("code").boost(40f));
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
   * update term based on type of search.
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

    return result;
  }

  /**
   * append each token from a term with given modifier.
   *
   * @param term the term or phrase
   * @param modifier the modifier
   * @return the modified term or phrase
   */
  private String updateTokens(String term, Character modifier) {
    String[] tokens = term.split("\\s+");
    StringBuilder builder = new StringBuilder();

    for (String token : tokens) {
      if (builder.length() > 0) builder.append(" ");
      builder.append(token).append(modifier);
    }

    return builder.toString();
  }

  /**
   * builds terminology query based on input search criteria.
   *
   * @param terminologies list of terminologies
   * @return the terminology query builder
   */
  private QueryBuilder getTerminologyQuery(final List<Terminology> terminologies) {
    if (CollectionUtils.isEmpty(terminologies)) {
      return null;
    }

    BoolQueryBuilder terminologyQuery = QueryBuilders.boolQuery();

    if (terminologies.size() == 1) {
      terminologyQuery =
          terminologyQuery.must(
              QueryBuilders.matchQuery("terminology", terminologies.get(0).getTerminology()));
    } else {
      for (Terminology terminology : terminologies) {
        terminologyQuery =
            terminologyQuery.should(
                QueryBuilders.matchQuery("terminology", terminology.getTerminology()));
      }
    }

    return terminologyQuery;
  }

  /**
   * builds list of queries for field specific criteria from the search criteria.
   *
   * @param terminologies the terminologies
   * @param searchCriteria the search criteria
   * @return list of nested queries
   */
  private List<QueryBuilder> getCriteriaQueries(
      List<Terminology> terminologies, SearchCriteria searchCriteria) {
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

    QueryBuilder subsetQuery = getSubsetValueQueryBuilder(terminologies, searchCriteria);
    if (subsetQuery != null) {
      queries.add(subsetQuery);
    }

    return queries;
  }

  /**
   * builds nested query for property criteria on value field for given types.
   *
   * @param searchCriteria the search criteria
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
    BoolQueryBuilder fieldBoolQuery =
        QueryBuilders.boolQuery()
            .must(QueryBuilders.matchQuery("properties.type", "Concept_Status"))
            .must(inQuery);

    // nested query on properties
    return QueryBuilders.nestedQuery("properties", fieldBoolQuery, ScoreMode.Total);
  }

  /**
   * builds nested query for subset criteria on value field for given types.
   *
   * @param term the term
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getSubsetValueQueryBuilder(
      final List<Terminology> terminologies, SearchCriteria searchCriteria) {
    if (searchCriteria.getSubset().size() == 0) return null;

    List<String> subsets = searchCriteria.getSubset();
    BoolQueryBuilder subsetListQuery = QueryBuilders.boolQuery();

    if (subsets.size() == 1) {
      subsetListQuery =
          subsetListQuery.must(
              QueryBuilders.matchQuery("associations.relatedCode", subsets.get(0)));

      // If there is one subset code and one terminology, do the CDISC grouper thing
      if (terminologies.size() == 1) {
        final Optional<Concept> concept =
            osQueryService.getConcept(
                subsets.get(0), terminologies.get(0), new IncludeParam("synonyms"));
        // For CDISC groupers, only keep members matching CDISC in name
        if (concept.isPresent() && ConceptUtils.isCdiscGrouper(concept.get())) {
          subsetListQuery.must(
              QueryBuilders.queryStringQuery("CDISC*").field("name").analyzeWildcard(true));
        }
      }
    } else {
      for (String subset : subsets) {
        subsetListQuery =
            subsetListQuery.should(QueryBuilders.matchQuery("associations.relatedCode", subset));
      }
    }

    BoolQueryBuilder subsetQuery =
        QueryBuilders.boolQuery()
            .must(QueryBuilders.matchQuery("associations.type", "Concept_In_Subset"));

    if (!(searchCriteria.getSubset().size() == 1
        && searchCriteria.getSubset().get(0).contentEquals("*"))) {
      subsetQuery.must(subsetListQuery);
    }
    return subsetQuery;
  }

  /**
   * builds nested query for property criteria on type or code field.
   *
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getPropertyValueQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getProperty())) return null;

    boolean hasValue = !StringUtils.isBlank(searchCriteria.getValue());

    // Match value (optional)
    MatchQueryBuilder valueQuery = null;
    if (hasValue) {
      valueQuery = QueryBuilders.matchQuery("properties.value", searchCriteria.getValue());
    }

    BoolQueryBuilder typeQuery = QueryBuilders.boolQuery();
    // Iterate through properties and build up parts
    for (String property : searchCriteria.getProperty()) {

      typeQuery =
          typeQuery
              .should(QueryBuilders.matchQuery("properties.type", property))
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
   * builds nested query for synonym source criteria.
   *
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getSynonymSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymSource())) return null;

    // IN query on synonym.source

    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getSynonymSource().size() == 1) {
      fieldBoolQuery =
          fieldBoolQuery.must(
              QueryBuilders.matchQuery(
                  "synonyms.source", searchCriteria.getSynonymSource().get(0)));
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
  private QueryBuilder getSynonymTypeQueryBuilder(
      List<Terminology> terminologies, SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymType())) return null;

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
   * builds nested query for definition source criteria.
   *
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getDefinitionSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getDefinitionSource())) return null;

    // IN query on definition.source
    BoolQueryBuilder inQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getDefinitionSource().size() == 1) {
      inQuery =
          inQuery.must(
              QueryBuilders.matchQuery(
                  "definitions.source", searchCriteria.getDefinitionSource().get(0)));
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
   * @param terminologies the terminologies
   * @param searchCriteria the search criteria
   * @return the definition type query builder
   */
  private QueryBuilder getDefinitionTypeQueryBuilder(
      List<Terminology> terminologies, SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getDefinitionType())) return null;

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
   * builds nested query for synonym term type criteria.
   *
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getSynonymTermTypeQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymTermType())) return null;

    // bool query to match synonym.termType
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getSynonymTermType().size() == 1) {
      fieldBoolQuery =
          fieldBoolQuery.must(
              QueryBuilders.matchQuery(
                  "synonyms.termType", searchCriteria.getSynonymTermType().get(0)));
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
   * builds nested query for synonym term type + source criteria.
   *
   * @param searchCriteria the search criteria
   * @return the nested query
   */
  private QueryBuilder getSynonymTermTypeAndSourceQueryBuilder(SearchCriteria searchCriteria) {
    if (CollectionUtils.isEmpty(searchCriteria.getSynonymTermType())) return null;

    // bool query to match synonym.termType
    BoolQueryBuilder fieldBoolQuery = QueryBuilders.boolQuery();

    if (searchCriteria.getSynonymTermType().size() == 1) {
      fieldBoolQuery =
          fieldBoolQuery.must(
              QueryBuilders.matchQuery(
                  "synonyms.termType", searchCriteria.getSynonymTermType().get(0)));
    } else {
      // IN query on synonym.termType
      BoolQueryBuilder inQuery = QueryBuilders.boolQuery();
      for (String source : searchCriteria.getSynonymTermType()) {
        inQuery = inQuery.should(QueryBuilders.matchQuery("synonyms.termType", source));
      }
      fieldBoolQuery = fieldBoolQuery.must(inQuery);
    }

    if (searchCriteria.getSynonymSource().size() == 1) {
      fieldBoolQuery =
          fieldBoolQuery.must(
              QueryBuilders.matchQuery(
                  "synonyms.source", searchCriteria.getSynonymSource().get(0)));
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
   * build array of index names.
   *
   * @param searchCriteria the search criteria
   * @return the array of index names
   * @throws Exception the exception
   */
  private String[] buildIndicesArray(SearchCriteria searchCriteria) throws Exception {
    List<String> terminologies = searchCriteria.getTerminology();

    if (CollectionUtils.isEmpty(searchCriteria.getTerminology())) {
      return new String[] {"_all"};
    }

    String[] indices = new String[terminologies.size()];
    // TODO: add getTerminologies call to avoid looping
    for (int i = 0; i < terminologies.size(); i++) {
      indices[i] =
          termUtils
              .getIndexedTerminology(terminologies.get(i), osQueryService, true)
              .getIndexName();
    }
    // logger.info("indices array: " + Arrays.asList(indices));
    return indices;
  }
}
