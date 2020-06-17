
package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.properties.ElasticQueryProperties;
import gov.nih.nci.evs.api.properties.ThesaurusProperties;

/**
 * Reference implementation of {@link ElasticQueryBuilder}. Includes hibernate
 * tags for MEME database.
 */
@Service
public class ElasticQueryBuilderImpl implements ElasticQueryBuilder {

  /** The Constant log. */
  private static final Logger log = LoggerFactory.getLogger(ElasticQueryBuilderImpl.class);

  /** The return field map. */
  @SuppressWarnings("unused")
  private HashMap<String, String> returnFieldMap;

  /** The property to query. */
  private HashMap<String, String> propertyToQuery;

  /** The property to query exact. */
  private HashMap<String, String> propertyToQueryExact;

  /** The property to query contains. */
  private HashMap<String, String> propertyToQueryContains;

  /** The association to query. */
  // private HashMap<String, String> associationToQuery;

  /** The role to query. */
  // private HashMap<String, String> roleToQuery;

  /** The elastic query properties. */
  @Autowired
  private ElasticQueryProperties elasticQueryProperties;

  /** The thesaurus properties. */
  @Autowired
  private ThesaurusProperties thesaurusProperties;

  /**
   * The sparql query manager service.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  // @Autowired
  // private SparqlQueryManagerService sparqlQueryManagerService;

  /**
   * Post init.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @PostConstruct
  public void postInit() throws IOException {

    // roleToQuery = (HashMap<String, String>) thesaurusProperties.getRoles();
    // associationToQuery = (HashMap<String, String>)
    // thesaurusProperties.getAssociations();
    returnFieldMap = (HashMap<String, String>) thesaurusProperties.getReturnFields();
    propertyToQuery = (HashMap<String, String>) elasticQueryProperties.getPropertyToQuery();
    propertyToQueryExact =
        (HashMap<String, String>) elasticQueryProperties.getPropertyToQueryExact();
    propertyToQueryContains =
        (HashMap<String, String>) elasticQueryProperties.getPropertyToQueryContains();
  }

  /**
   * Construct synonym source.
   *
   * @param synonymSources the synonym sources
   * @param synonymGroup the synonym group
   * @return the string
   */
  private String constructSynonymSource(List<String> synonymSources, final String synonymGroup) {
    String synonymSourceStr = "";

    /*
     * if (synonymSources != null && synonymSources.size() > 0) {
     * //synonymSourceStr = synonymSourceStr +
     * ",{ \"match\" : {\"FULL_SYN.term-source\" : \"" + synonymSource // +
     * "\"} }"; synonymSourceStr = synonymSourceStr +
     * ",{\"terms\":{\"FULL_SYN.term-source\": [" ; for (String synonymSource:
     * synonymSources ) { synonymSourceStr = synonymSourceStr + "\"" +
     * synonymSource + "\","; } synonymSourceStr = synonymSourceStr.substring(0,
     * synonymSourceStr.length() - 1); synonymSourceStr = synonymSourceStr +
     * "]}}"; }
     */

    if (synonymSources != null && synonymSources.size() > 0) {
      synonymSourceStr = synonymSourceStr + ",{\"bool\": {";
      synonymSourceStr = synonymSourceStr + "\"should\": [";
      synonymSourceStr = synonymSourceStr + "{\"terms\":{\"FULL_SYN.term-source\": [";
      for (String synonymSource : synonymSources) {
        synonymSourceStr = synonymSourceStr + "\"" + synonymSource + "\",";
      }
      synonymSourceStr = synonymSourceStr.substring(0, synonymSourceStr.length() - 1);
      synonymSourceStr = synonymSourceStr + "]}}";
      synonymSourceStr = synonymSourceStr + ",{\"terms\":{\"FULL_SYN.subsource-name\": [";
      for (String synonymSource : synonymSources) {
        synonymSourceStr = synonymSourceStr + "\"" + synonymSource + "\",";
      }
      synonymSourceStr = synonymSourceStr.substring(0, synonymSourceStr.length() - 1);
      synonymSourceStr = synonymSourceStr + "]}}";
      synonymSourceStr = synonymSourceStr + "]}}";

    }

    if (synonymGroup != null && !synonymGroup.equalsIgnoreCase("")) {
      synonymSourceStr = synonymSourceStr + ",{ \"match\" : {\"FULL_SYN.term-group\" : \""
          + synonymGroup + "\"} }";
    }

    return synonymSourceStr;

  }

  /**
   * Construct defintion source.
   *
   * @param definitionSources the definition sources
   * @return the string
   */
  private String constructDefintionSource(List<String> definitionSources) {
    String definitionSourceStr = "";

    // if (definitionSource != null && !definitionSource.equalsIgnoreCase("")) {
    // definitionSourceStr = definitionSourceStr + ",{ \"match\" :
    // {\"DEFINITION.def-source\" : \"" + definitionSource
    // + "\"} }";
    // }
    if (definitionSources != null && definitionSources.size() > 0) {

      definitionSourceStr = definitionSourceStr + ",{\"terms\":{\"DEFINITION.def-source\": [";
      for (String definitionSource : definitionSources) {
        definitionSourceStr = definitionSourceStr + "\"" + definitionSource + "\",";
      }
      definitionSourceStr = definitionSourceStr.substring(0, definitionSourceStr.length() - 1);
      definitionSourceStr = definitionSourceStr + "]}}";
    }

    return definitionSourceStr;

  }

  /**
   * Construct alt defintion source.
   *
   * @param definitionSources the definition sources
   * @return the string
   */
  private String constructAltDefintionSource(List<String> definitionSources) {
    String definitionSourceStr = "";

    // if (definitionSource != null && !definitionSource.equalsIgnoreCase("")) {
    // definitionSourceStr = definitionSourceStr + ",{ \"match\" :
    // {\"ALT_DEFINITION.def-source\" : \"" + definitionSource
    // + "\"} }";
    // }
    if (definitionSources != null && definitionSources.size() > 0) {

      definitionSourceStr = definitionSourceStr + ",{\"terms\":{\"ALT_DEFINITION.def-source\": [";
      for (String definitionSource : definitionSources) {
        definitionSourceStr = definitionSourceStr + "\"" + definitionSource + "\",";
      }
      definitionSourceStr = definitionSourceStr.substring(0, definitionSourceStr.length() - 1);
      definitionSourceStr = definitionSourceStr + "]}}";
    }

    return definitionSourceStr;

  }

  /**
   * Construct filter query.
   *
   * @param conceptStatuses the concept statuses
   * @param contributingSources the contributing sources
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private String constructFilterQuery(List<String> conceptStatuses,
    List<String> contributingSources) throws IOException {

    String filter = "";
    boolean contributingSourceFilter = false;
    boolean conceptStatusFilter = false;
    boolean hierarchySearch = false;

    if (conceptStatuses != null && conceptStatuses.size() > 0) {

      for (String conceptStatus : conceptStatuses) {
        conceptStatus = conceptStatus.toLowerCase();
        if (conceptStatus == null) {
          throw new IOException(
              "Invalid Parameter value for conceptStatus field. Rejected value - " + conceptStatus);
        }
      }
      conceptStatusFilter = true;
    }

    if (contributingSources != null && contributingSources.size() > 0) {

      for (String contributingSource : contributingSources) {
        contributingSource = contributingSource.toLowerCase();
        if (contributingSource == null) {
          throw new IOException(
              "Invalid Parameter value for contributingSource field. Rejected value - "
                  + contributingSource);
        }
      }
      contributingSourceFilter = true;
    }

    if (contributingSourceFilter || conceptStatusFilter || hierarchySearch) {
      filter = filter + ",\n";
      filter = filter + "\"filter\":\n";
      filter = filter + "[\n";
      // filter = filter + "\"bool\":{\n";
      // filter = filter + "\"should\":[\n";

      if (conceptStatusFilter) {
        if (conceptStatuses.size() == 1) {
          filter = filter + "{\"term\":{\"Concept_Status\":\"" + conceptStatuses.get(0) + "\"}},\n";
        } else {
          filter = filter + "{\"terms\":{\"Concept_Status\": [";
          for (String conceptStatus : conceptStatuses) {
            filter = filter + "\"" + conceptStatus + "\",";
          }
          filter = filter.substring(0, filter.length() - 1);
          filter = filter + "]}},\n";
        }
      }

      // if (contributingSourceFilter) {
      // for (String contributingSource: contributingSources ) {
      // filter = filter + "{\"term\":{\"Contributing_Source\":\"" +
      // contributingSource + "\"}},\n";
      // }
      // }

      if (contributingSourceFilter) {
        if (contributingSources.size() == 1) {
          filter = filter + "{\"term\":{\"Contributing_Source\":\"" + contributingSources.get(0)
              + "\"}},\n";
        } else {
          filter = filter + "{\"terms\":{\"Contributing_Source\": [";
          for (String contributingSource : contributingSources) {
            filter = filter + "\"" + contributingSource + "\",";
          }
          filter = filter.substring(0, filter.length() - 1);
          filter = filter + "]}},\n";
        }
      }

      filter = filter.substring(0, filter.length() - 2);
      filter = filter + "]\n";

    }
    return filter;
  }

  /**
   * Returns the main query.
   *
   * @param highlightFlag the highlight flag
   * @return the main query
   */
  private String getMainQuery(final boolean highlightFlag) {
    if (highlightFlag) {
      return elasticQueryProperties.getMainQuery();
    }
    return elasticQueryProperties.getMainQueryWithoutHighlights();
  }

  /**
   * Returns the main nested query.
   *
   * @param highlightFlag the highlight flag
   * @return the main nested query
   */
  private String getMainNestedQuery(final boolean highlightFlag) {
    if (highlightFlag) {
      return elasticQueryProperties.getMainNestedQuery();
    }
    return elasticQueryProperties.getMainNestedQueryWithoutHighlights();
  }

  /**
   * Returns the main multiple nested query.
   *
   * @param highlightFlag the highlight flag
   * @return the main multiple nested query
   */
  private String getMainMultipleNestedQuery(final boolean highlightFlag) {
    if (highlightFlag) {
      return elasticQueryProperties.getMainMultipleNestedQuery();
    }
    return elasticQueryProperties.getMainMultipleNestedQueryWithoutHighlights();
  }

  /**
   * Returns the main nested with non nested query.
   *
   * @param highlightFlag the highlight flag
   * @return the main nested with non nested query
   */
  @SuppressWarnings("unused")
  private String getMainNestedWithNonNestedQuery(final boolean highlightFlag) {
    if (highlightFlag) {
      return elasticQueryProperties.getMainNestedWithNonNestedQuery();
    }
    return elasticQueryProperties.getMainNestedWithNonNestedQuerywithoutHL();
  }

  /**
   * Construct query.
   *
   * @param searchCriteria the search criteria
   * @return the string
   * @throws IOException Signals that an I/O exception has occurred.
   */
  /* see superclass */
  public String constructQuery(SearchCriteria searchCriteria) throws IOException {
    Map<String, String> valuesMap = new HashMap<>();

    // setting defaults - already handeld by SearchCriteria

    // NO need to worry about "display" because we'll handle "include" at the
    // top level

    // TODO:

    // //association
    // if (searchCritiera.getAssociationSearch() != null) {
    // if (!(searchCritiera.getAssociationSearch().equalsIgnoreCase("source")
    // || searchCritiera.getAssociationSearch().equalsIgnoreCase("target"))) {
    // throw new IOException("Invalid Parameter value for
    // associationSearch -"
    // + searchCritiera.getAssociationSearch() + ". The valid values are
    // source,target.");
    //
    // }
    // if (searchCritiera.getAssociationSearch().equalsIgnoreCase("source")){
    // relation = "Association";
    // }else {
    // relation = "InverseAssociation";
    // }
    // associationSearch = true;
    // }
    // //role
    // if (searchCritiera.getRoleSearch() != null) {
    // if (!(searchCritiera.getRoleSearch().equalsIgnoreCase("source")
    // || searchCritiera.getRoleSearch().equalsIgnoreCase("target"))) {
    // throw new IOException("Invalid Parameter value for
    // roleSearch -"
    // + searchCritiera.getRoleSearch() + ". The valid values are
    // source,target.");
    //
    // }
    // if (searchCritiera.getRoleSearch().equalsIgnoreCase("source")){
    // relation = "Role";
    // }else {
    // relation = "InverseRole";
    // }
    // roleSearch = true;
    // }

    final String returnFields = elasticQueryProperties.getShortsourcefields();

    valuesMap.put("sourcefields", returnFields);
    valuesMap.put("fromRecord", searchCriteria.getFromRecord().toString());
    valuesMap.put("pageSize", searchCriteria.getPageSize().toString());

    // *******term and operator replace*************
    String term = searchCriteria.getTerm();
    String operator = "";
    String rescore = "";
    if (searchCriteria.getType() != null) {
      if (searchCriteria.getType() == "OR") {
        operator = "";
      }
      if (searchCriteria.getType().equalsIgnoreCase("AND")) {
        if (!searchCriteria.getSynonymSource().isEmpty()
            // || !searchCriteria.getAssociation().isEmpty()
            // || !searchCriteria.getRole().isEmpty()
            || !searchCriteria.getDefinitionSource().isEmpty()) {
          operator = "\"operator\":\"and\",\n";
        } else {
          operator = "\"default_operator\":\"AND\",\n";
        }
      }
      if (searchCriteria.getType().equalsIgnoreCase("phrase")) {
        operator = "\"type\":\"phrase_prefix\",\n";
      }
      if (searchCriteria.getType().equalsIgnoreCase("contains")) {
        operator = "";
      }
      if (searchCriteria.getType().equalsIgnoreCase("match")) {
        operator = "";
      }
      if (searchCriteria.getType().equalsIgnoreCase("fuzzy")) {
        operator = "";
        term = term + "~";
      }
      if (searchCriteria.getType().equalsIgnoreCase("startswith")) {

        operator = "\"type\":\"phrase_prefix\",\n";
        rescore = elasticQueryProperties.getRescoreQuery();
      }
    } else {
      operator = "";
    }
    valuesMap.put("rescore", rescore);
    valuesMap.put("operator", operator);
    valuesMap.put("searchterm", term);

    // **********fields to search in replace***************
    String fields = "";
    if (!searchCriteria.getSynonymSource().isEmpty()) {
      // any getProperty field will be ignored
      if ((searchCriteria.getType().equalsIgnoreCase("startswith"))
          || (searchCriteria.getType().equalsIgnoreCase("match"))) {
        fields = elasticQueryProperties.getExactstartswithsynonymfields();
      } else if (searchCriteria.getType().equalsIgnoreCase("contains")) {
        fields = elasticQueryProperties.getContainssynonymfields();

      } else {
        fields = elasticQueryProperties.getAndorsynonymfields();
      }

      // } else if (!searchCriteria.getAssociation().isEmpty()
      // || !searchCriteria.getRole().isEmpty()) {
      // // any getProperty field will be ignored
      // if ((searchCriteria.getType().equalsIgnoreCase("startswith"))
      // || (searchCriteria.getType().equalsIgnoreCase("match"))) {
      // fields = elasticQueryProperties.getExactstartswithassociationfields();
      // } else if (searchCriteria.getType().equalsIgnoreCase("contains")) {
      // fields = elasticQueryProperties.getContainsassociationfields();
      //
      // } else {
      // fields = elasticQueryProperties.getAndorassociationfields();
      // }

      // fields = fields.replace("${relation}", relation);

    } else if (searchCriteria.getDefinitionSource().size() > 0) {
      // any getProperty field will be ignored
      if ((searchCriteria.getType().equalsIgnoreCase("startswith"))
          || (searchCriteria.getType().equalsIgnoreCase("match"))) {
        fields = elasticQueryProperties.getExactstartswithdefinitionfields();
      } else if (searchCriteria.getType().equalsIgnoreCase("contains")) {
        fields = elasticQueryProperties.getContainsdefinitionfields();

      } else {
        fields = elasticQueryProperties.getAndordefinitionfields();
      }
    } else {
      if (searchCriteria.getProperty() != null && searchCriteria.getProperty().size() > 0) {
        List<String> properties = searchCriteria.getProperty();
        if (properties != null && properties.size() > 0) {
          fields = "[\n";
          for (String property : properties) {
            String value = "";
            if ((searchCriteria.getType().equalsIgnoreCase("startswith"))
                || (searchCriteria.getType().equalsIgnoreCase("match"))) {
              value = this.propertyToQueryExact.get(property.toLowerCase());

            } else if (searchCriteria.getType().equalsIgnoreCase("contains")) {
              value = this.propertyToQueryContains.get(property.toLowerCase());
            } else {
              value = this.propertyToQuery.get(property.toLowerCase());
            }
            if (value == null) {
              // INstead of throwing an exception here, just add a worthless
              // clause
              // throw new IOException(
              // "Parameter 'property' has an invalid value = " + property);
              value = "\"" + property.toLowerCase() + "^10\",";
            }

            fields = fields + value;
          }
          fields = fields.substring(0, fields.length() - 1);
          fields = fields + "]\n";

        }
      } else if ((searchCriteria.getType().equalsIgnoreCase("startswith"))
          || (searchCriteria.getType().equalsIgnoreCase("match"))) {
        fields = elasticQueryProperties.getExactstartswithfields();
      } else if (searchCriteria.getType().equalsIgnoreCase("contains")) {
        fields = elasticQueryProperties.getContainsfields();

      } else {
        fields = elasticQueryProperties.getAndorfields();
      }
    }
    valuesMap.put("fields", fields);

    // ***********replace highlight fields************************
    String highlightFields = "";
    if (searchCriteria.getSynonymSource().size() > 0) {
      if (searchCriteria.getType().equalsIgnoreCase("startswith")
          || searchCriteria.getType().equalsIgnoreCase("match")) {
        highlightFields = elasticQueryProperties.getHighlightsynonymexact();

      } else if (searchCriteria.getType().equalsIgnoreCase("contains")) {
        highlightFields = elasticQueryProperties.getHighlightsynonymcontains();

      } else {
        highlightFields = elasticQueryProperties.getHighlightsynonymandor();
      }
      // } else if (searchCriteria.getAssociation().size() > 0
      // || searchCriteria.getRole().size() > 0) {
      // if (searchCriteria.getType().equalsIgnoreCase("startswith")
      // || searchCriteria.getType().equalsIgnoreCase("match")) {
      // highlightFields =
      // elasticQueryProperties.getHighlightassociationexact();
      //
      // } else if (searchCriteria.getType().equalsIgnoreCase("contains")) {
      // highlightFields =
      // elasticQueryProperties.getHighlightassociationcontains();
      //
      // } else {
      // highlightFields =
      // elasticQueryProperties.getHighlightassociationandor();
      // }

      // TODO:
      // highlightFields = highlightFields.replace("${relation}", relation);

    } else if (searchCriteria.getDefinitionSource().size() > 0) {
      if (searchCriteria.getType().equalsIgnoreCase("startswith")
          || searchCriteria.getType().equalsIgnoreCase("match")) {
        highlightFields = elasticQueryProperties.getHighlightdefinitionexact();

      } else if (searchCriteria.getType().equalsIgnoreCase("contains")) {
        highlightFields = elasticQueryProperties.getHighlightdefinitioncontains();

      } else {
        highlightFields = elasticQueryProperties.getHighlightdefinitionandor();
      }

    } else {
      if (searchCriteria.getType().equalsIgnoreCase("startswith")
          || searchCriteria.getType().equalsIgnoreCase("match")) {
        highlightFields = elasticQueryProperties.getHighlightexact();

      } else if (searchCriteria.getType().equalsIgnoreCase("contains")) {
        highlightFields = elasticQueryProperties.getHighlightcontains();

      } else {
        highlightFields = elasticQueryProperties.getHighlightandor();
      }
    }
    valuesMap.put("highlightFields", highlightFields);
    valuesMap.put("highlightTags", "");

    // TODO
    // // get association relationship
    // if (associationSearch || roleSearch) {
    // String associationRelationship =
    // constructAssociationRelationship(searchCriteria, relation);
    // valuesMap.put("searchFilter", associationRelationship);
    //
    // valuesMap.put("nestedPath", relation);
    // }

    // ***************synonym source***********
    if (searchCriteria.getSynonymSource().size() > 0) {
      String synonymSourceStr = constructSynonymSource(searchCriteria.getSynonymSource(),
          searchCriteria.getSynonymTermGroup());

      valuesMap.put("searchFilter", synonymSourceStr);
      valuesMap.put("nestedPath", "FULL_SYN");

    }

    // ***************definition source***********
    if (searchCriteria.getDefinitionSource().size() > 0) {
      String definitionSourceStr = constructDefintionSource(searchCriteria.getDefinitionSource());
      valuesMap.put("searchFilter1", definitionSourceStr);
      definitionSourceStr = constructAltDefintionSource(searchCriteria.getDefinitionSource());
      valuesMap.put("searchFilter2", definitionSourceStr);
      valuesMap.put("nestedPath1", "DEFINITION");
      valuesMap.put("nestedPath2", "ALT_DEFINITION");
    }

    // **********************filter replace********************
    if (!(searchCriteria.getSynonymSource().size() > 0
    // || searchCriteria.getAssociation().size() > 0
    // || searchCriteria.getRole().size() > 0
    )) {
      String filter = this.constructFilterQuery(searchCriteria.getConceptStatus(),
          searchCriteria.getSynonymSource());
      valuesMap.put("filter", filter);
    }

    // *********get main query
    String templateString = "";
    final boolean highlightFlag = searchCriteria.computeIncludeParam().isHighlights();
    if (searchCriteria.getDefinitionSource().size() > 0) {
      templateString = getMainMultipleNestedQuery(highlightFlag);
    } else if (searchCriteria.getSynonymSource().size() > 0
    // || searchCriteria.getAssociation().size() > 0
    // || searchCriteria.getRole().size() > 0
    ) {
      templateString = getMainNestedQuery(highlightFlag);
    } else {
      templateString = getMainQuery(highlightFlag);
    }

    // replace values
    StrSubstitutor sub = new StrSubstitutor(valuesMap);
    String resolvedString = sub.replace(templateString);

    log.debug("query string - " + resolvedString);
    return resolvedString;
  }

}
