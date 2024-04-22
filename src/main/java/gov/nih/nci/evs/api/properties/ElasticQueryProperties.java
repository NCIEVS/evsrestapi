package gov.nih.nci.evs.api.properties;

import java.util.HashMap;

/** Elasticsearch query properties. */
public class ElasticQueryProperties {

  /** The shortsourcefields. */
  // source fields
  private String shortsourcefields;

  /** The defaultsourcefields. */
  private String defaultsourcefields;

  /** The associationsourcefields. */
  private String associationsourcefields;

  /** The definitionsourcefields. */
  private String definitionsourcefields;

  /** The exactstartswithfields. */
  // exact and starts with fields
  private String exactstartswithfields;

  /** The exactstartswithsynonymfields. */
  private String exactstartswithsynonymfields;

  /** The exactstartswithdefinitionfields. */
  private String exactstartswithdefinitionfields;

  /** The exactstartswithassociationfields. */
  private String exactstartswithassociationfields;

  /** The containsfields. */
  // contains fields
  private String containsfields;

  /** The containssynonymfields. */
  private String containssynonymfields;

  /** The containsdefinitionfields. */
  private String containsdefinitionfields;

  /** The containsassociationfields. */
  private String containsassociationfields;

  /** The andorfields. */
  // andor fields
  private String andorfields;

  /** The andorsynonymfields. */
  private String andorsynonymfields;

  /** The andordefinitionfields. */
  private String andordefinitionfields;

  /** The andorassociationfields. */
  private String andorassociationfields;

  /** The highlightexact. */
  // highlight fields for exact
  private String highlightexact;

  /** The highlightsynonymexact. */
  private String highlightsynonymexact;

  /** The highlightdefinitionexact. */
  private String highlightdefinitionexact;

  /** The highlightassociationexact. */
  private String highlightassociationexact;

  /** The highlightcontains. */
  // highlight fields for contains
  private String highlightcontains;

  /** The highlightsynonymcontains. */
  private String highlightsynonymcontains;

  /** The highlightdefinitioncontains. */
  private String highlightdefinitioncontains;

  /** The highlightassociationcontains. */
  private String highlightassociationcontains;

  /** The highlightandor. */
  // highlight fields for and or
  private String highlightandor;

  /** The highlightsynonymandor. */
  private String highlightsynonymandor;

  /** The highlightdefinitionandor. */
  private String highlightdefinitionandor;

  /** The highlightassociationandor. */
  private String highlightassociationandor;

  /** The main query. */
  // main query and main nested query
  private String mainQuery;

  /** The main nested query. */
  private String mainNestedQuery;

  /** The main nested with non nested query. */
  private String mainNestedWithNonNestedQuery;

  /** The main multiple nested query. */
  private String mainMultipleNestedQuery;

  /** The main query without highlights. */
  private String mainQueryWithoutHighlights;

  /** The main nested query without highlights. */
  private String mainNestedQueryWithoutHighlights;

  /** The main nested with non nested querywithout HL. */
  private String mainNestedWithNonNestedQuerywithoutHL;

  /** The main multiple nested query without highlights. */
  private String mainMultipleNestedQueryWithoutHighlights;

  /** The rescore query. */
  // rescore query
  private String rescoreQuery;

  /** The highlight tags. */
  // highlight tags
  private String highlightTags;

  /** The property to query. */
  // Property fields
  private HashMap<String, String> propertyToQuery;

  /** The property to query exact. */
  private HashMap<String, String> propertyToQueryExact;

  /** The property to query contains. */
  private HashMap<String, String> propertyToQueryContains;

  /**
   * Returns the exactstartswithassociationfields.
   *
   * @return the exactstartswithassociationfields
   */
  public String getExactstartswithassociationfields() {
    return exactstartswithassociationfields;
  }

  /**
   * Sets the exactstartswithassociationfields.
   *
   * @param exactstartswithassociationfields the exactstartswithassociationfields
   */
  public void setExactstartswithassociationfields(String exactstartswithassociationfields) {
    this.exactstartswithassociationfields = exactstartswithassociationfields;
  }

  /**
   * Returns the containsassociationfields.
   *
   * @return the containsassociationfields
   */
  public String getContainsassociationfields() {
    return containsassociationfields;
  }

  /**
   * Sets the containsassociationfields.
   *
   * @param containsassociationfields the containsassociationfields
   */
  public void setContainsassociationfields(String containsassociationfields) {
    this.containsassociationfields = containsassociationfields;
  }

  /**
   * Returns the andorassociationfields.
   *
   * @return the andorassociationfields
   */
  public String getAndorassociationfields() {
    return andorassociationfields;
  }

  /**
   * Sets the andorassociationfields.
   *
   * @param andorassociationfields the andorassociationfields
   */
  public void setAndorassociationfields(String andorassociationfields) {
    this.andorassociationfields = andorassociationfields;
  }

  /**
   * Returns the highlightassociationexact.
   *
   * @return the highlightassociationexact
   */
  public String getHighlightassociationexact() {
    return highlightassociationexact;
  }

  /**
   * Sets the highlightassociationexact.
   *
   * @param highlightassociationexact the highlightassociationexact
   */
  public void setHighlightassociationexact(String highlightassociationexact) {
    this.highlightassociationexact = highlightassociationexact;
  }

  /**
   * Returns the highlightassociationcontains.
   *
   * @return the highlightassociationcontains
   */
  public String getHighlightassociationcontains() {
    return highlightassociationcontains;
  }

  /**
   * Sets the highlightassociationcontains.
   *
   * @param highlightassociationcontains the highlightassociationcontains
   */
  public void setHighlightassociationcontains(String highlightassociationcontains) {
    this.highlightassociationcontains = highlightassociationcontains;
  }

  /**
   * Returns the highlightassociationandor.
   *
   * @return the highlightassociationandor
   */
  public String getHighlightassociationandor() {
    return highlightassociationandor;
  }

  /**
   * Sets the highlightassociationandor.
   *
   * @param highlightassociationandor the highlightassociationandor
   */
  public void setHighlightassociationandor(String highlightassociationandor) {
    this.highlightassociationandor = highlightassociationandor;
  }

  /**
   * Returns the highlightsynonymexact.
   *
   * @return the highlightsynonymexact
   */
  public String getHighlightsynonymexact() {
    return highlightsynonymexact;
  }

  /**
   * Sets the highlightsynonymexact.
   *
   * @param highlightsynonymexact the highlightsynonymexact
   */
  public void setHighlightsynonymexact(String highlightsynonymexact) {
    this.highlightsynonymexact = highlightsynonymexact;
  }

  /**
   * Returns the highlightsynonymcontains.
   *
   * @return the highlightsynonymcontains
   */
  public String getHighlightsynonymcontains() {
    return highlightsynonymcontains;
  }

  /**
   * Sets the highlightsynonymcontains.
   *
   * @param highlightsynonymcontains the highlightsynonymcontains
   */
  public void setHighlightsynonymcontains(String highlightsynonymcontains) {
    this.highlightsynonymcontains = highlightsynonymcontains;
  }

  /**
   * Returns the highlightsynonymandor.
   *
   * @return the highlightsynonymandor
   */
  public String getHighlightsynonymandor() {
    return highlightsynonymandor;
  }

  /**
   * Sets the highlightsynonymandor.
   *
   * @param highlightsynonymandor the highlightsynonymandor
   */
  public void setHighlightsynonymandor(String highlightsynonymandor) {
    this.highlightsynonymandor = highlightsynonymandor;
  }

  /**
   * Returns the exactstartswithsynonymfields.
   *
   * @return the exactstartswithsynonymfields
   */
  public String getExactstartswithsynonymfields() {
    return exactstartswithsynonymfields;
  }

  /**
   * Sets the exactstartswithsynonymfields.
   *
   * @param exactstartswithsynonymfields the exactstartswithsynonymfields
   */
  public void setExactstartswithsynonymfields(String exactstartswithsynonymfields) {
    this.exactstartswithsynonymfields = exactstartswithsynonymfields;
  }

  /**
   * Returns the containssynonymfields.
   *
   * @return the containssynonymfields
   */
  public String getContainssynonymfields() {
    return containssynonymfields;
  }

  /**
   * Sets the containssynonymfields.
   *
   * @param containssynonymfields the containssynonymfields
   */
  public void setContainssynonymfields(String containssynonymfields) {
    this.containssynonymfields = containssynonymfields;
  }

  /**
   * Returns the andorsynonymfields.
   *
   * @return the andorsynonymfields
   */
  public String getAndorsynonymfields() {
    return andorsynonymfields;
  }

  /**
   * Sets the andorsynonymfields.
   *
   * @param andorsynonymfields the andorsynonymfields
   */
  public void setAndorsynonymfields(String andorsynonymfields) {
    this.andorsynonymfields = andorsynonymfields;
  }

  /**
   * Returns the highlight tags.
   *
   * @return the highlight tags
   */
  public String getHighlightTags() {
    return highlightTags;
  }

  /**
   * Sets the highlight tags.
   *
   * @param highlightTags the highlight tags
   */
  public void setHighlightTags(String highlightTags) {
    this.highlightTags = highlightTags;
  }

  /**
   * Returns the highlightexact.
   *
   * @return the highlightexact
   */
  public String getHighlightexact() {
    return highlightexact;
  }

  /**
   * Sets the highlightexact.
   *
   * @param highlightexact the highlightexact
   */
  public void setHighlightexact(String highlightexact) {
    this.highlightexact = highlightexact;
  }

  /**
   * Returns the highlightcontains.
   *
   * @return the highlightcontains
   */
  public String getHighlightcontains() {
    return highlightcontains;
  }

  /**
   * Sets the highlightcontains.
   *
   * @param highlightcontains the highlightcontains
   */
  public void setHighlightcontains(String highlightcontains) {
    this.highlightcontains = highlightcontains;
  }

  /**
   * Returns the highlightandor.
   *
   * @return the highlightandor
   */
  public String getHighlightandor() {
    return highlightandor;
  }

  /**
   * Sets the highlightandor.
   *
   * @param highlightandor the highlightandor
   */
  public void setHighlightandor(String highlightandor) {
    this.highlightandor = highlightandor;
  }

  /**
   * Returns the main query.
   *
   * @return the main query
   */
  public String getMainQuery() {
    return mainQuery;
  }

  /**
   * Sets the main query.
   *
   * @param mainQuery the main query
   */
  public void setMainQuery(String mainQuery) {
    this.mainQuery = mainQuery;
  }

  /**
   * Returns the exactstartswithfields.
   *
   * @return the exactstartswithfields
   */
  public String getExactstartswithfields() {
    return exactstartswithfields;
  }

  /**
   * Sets the exactstartswithfields.
   *
   * @param exactstartswithfields the exactstartswithfields
   */
  public void setExactstartswithfields(String exactstartswithfields) {
    this.exactstartswithfields = exactstartswithfields;
  }

  /**
   * Returns the containsfields.
   *
   * @return the containsfields
   */
  public String getContainsfields() {
    return containsfields;
  }

  /**
   * Sets the containsfields.
   *
   * @param containsfields the containsfields
   */
  public void setContainsfields(String containsfields) {
    this.containsfields = containsfields;
  }

  /**
   * Returns the andorfields.
   *
   * @return the andorfields
   */
  public String getAndorfields() {
    return andorfields;
  }

  /**
   * Sets the andorfields.
   *
   * @param andorfields the andorfields
   */
  public void setAndorfields(String andorfields) {
    this.andorfields = andorfields;
  }

  /**
   * Returns the shortsourcefields.
   *
   * @return the shortsourcefields
   */
  public String getShortsourcefields() {
    return shortsourcefields;
  }

  /**
   * Sets the shortsourcefields.
   *
   * @param shortsourcefields the shortsourcefields
   */
  public void setShortsourcefields(String shortsourcefields) {
    this.shortsourcefields = shortsourcefields;
  }

  /**
   * Returns the defaultsourcefields.
   *
   * @return the defaultsourcefields
   */
  public String getDefaultsourcefields() {
    return defaultsourcefields;
  }

  /**
   * Sets the defaultsourcefields.
   *
   * @param defaultsourcefields the defaultsourcefields
   */
  public void setDefaultsourcefields(String defaultsourcefields) {
    this.defaultsourcefields = defaultsourcefields;
  }

  /**
   * Returns the main query without highlights.
   *
   * @return the main query without highlights
   */
  public String getMainQueryWithoutHighlights() {
    return mainQueryWithoutHighlights;
  }

  /**
   * Sets the main query without highlights.
   *
   * @param mainQueryWithoutHighlights the main query without highlights
   */
  public void setMainQueryWithoutHighlights(String mainQueryWithoutHighlights) {
    this.mainQueryWithoutHighlights = mainQueryWithoutHighlights;
  }

  /**
   * Returns the exactstartswithdefinitionfields.
   *
   * @return the exactstartswithdefinitionfields
   */
  public String getExactstartswithdefinitionfields() {
    return exactstartswithdefinitionfields;
  }

  /**
   * Sets the exactstartswithdefinitionfields.
   *
   * @param exactstartswithdefinitionfields the exactstartswithdefinitionfields
   */
  public void setExactstartswithdefinitionfields(String exactstartswithdefinitionfields) {
    this.exactstartswithdefinitionfields = exactstartswithdefinitionfields;
  }

  /**
   * Returns the containsdefinitionfields.
   *
   * @return the containsdefinitionfields
   */
  public String getContainsdefinitionfields() {
    return containsdefinitionfields;
  }

  /**
   * Sets the containsdefinitionfields.
   *
   * @param containsdefinitionfields the containsdefinitionfields
   */
  public void setContainsdefinitionfields(String containsdefinitionfields) {
    this.containsdefinitionfields = containsdefinitionfields;
  }

  /**
   * Returns the andordefinitionfields.
   *
   * @return the andordefinitionfields
   */
  public String getAndordefinitionfields() {
    return andordefinitionfields;
  }

  /**
   * Sets the andordefinitionfields.
   *
   * @param andordefinitionfields the andordefinitionfields
   */
  public void setAndordefinitionfields(String andordefinitionfields) {
    this.andordefinitionfields = andordefinitionfields;
  }

  /**
   * Returns the highlightdefinitionexact.
   *
   * @return the highlightdefinitionexact
   */
  public String getHighlightdefinitionexact() {
    return highlightdefinitionexact;
  }

  /**
   * Sets the highlightdefinitionexact.
   *
   * @param highlightdefinitionexact the highlightdefinitionexact
   */
  public void setHighlightdefinitionexact(String highlightdefinitionexact) {
    this.highlightdefinitionexact = highlightdefinitionexact;
  }

  /**
   * Returns the highlightdefinitioncontains.
   *
   * @return the highlightdefinitioncontains
   */
  public String getHighlightdefinitioncontains() {
    return highlightdefinitioncontains;
  }

  /**
   * Sets the highlightdefinitioncontains.
   *
   * @param highlightdefinitioncontains the highlightdefinitioncontains
   */
  public void setHighlightdefinitioncontains(String highlightdefinitioncontains) {
    this.highlightdefinitioncontains = highlightdefinitioncontains;
  }

  /**
   * Returns the highlightdefinitionandor.
   *
   * @return the highlightdefinitionandor
   */
  public String getHighlightdefinitionandor() {
    return highlightdefinitionandor;
  }

  /**
   * Sets the highlightdefinitionandor.
   *
   * @param highlightdefinitionandor the highlightdefinitionandor
   */
  public void setHighlightdefinitionandor(String highlightdefinitionandor) {
    this.highlightdefinitionandor = highlightdefinitionandor;
  }

  /**
   * Returns the main nested query.
   *
   * @return the main nested query
   */
  public String getMainNestedQuery() {
    return mainNestedQuery;
  }

  /**
   * Sets the main nested query.
   *
   * @param mainNestedQuery the main nested query
   */
  public void setMainNestedQuery(String mainNestedQuery) {
    this.mainNestedQuery = mainNestedQuery;
  }

  /**
   * Returns the main nested query without highlights.
   *
   * @return the main nested query without highlights
   */
  public String getMainNestedQueryWithoutHighlights() {
    return mainNestedQueryWithoutHighlights;
  }

  /**
   * Sets the main nested query without highlights.
   *
   * @param mainNestedQueryWithoutHighlights the main nested query without highlights
   */
  public void setMainNestedQueryWithoutHighlights(String mainNestedQueryWithoutHighlights) {
    this.mainNestedQueryWithoutHighlights = mainNestedQueryWithoutHighlights;
  }

  /**
   * Returns the associationsourcefields.
   *
   * @return the associationsourcefields
   */
  public String getAssociationsourcefields() {
    return associationsourcefields;
  }

  /**
   * Sets the associationsourcefields.
   *
   * @param associationsourcefields the associationsourcefields
   */
  public void setAssociationsourcefields(String associationsourcefields) {
    this.associationsourcefields = associationsourcefields;
  }

  /**
   * Returns the definitionsourcefields.
   *
   * @return the definitionsourcefields
   */
  public String getDefinitionsourcefields() {
    return definitionsourcefields;
  }

  /**
   * Sets the definitionsourcefields.
   *
   * @param definitionsourcefields the definitionsourcefields
   */
  public void setDefinitionsourcefields(String definitionsourcefields) {
    this.definitionsourcefields = definitionsourcefields;
  }

  /**
   * Returns the property to query.
   *
   * @return the property to query
   */
  public HashMap<String, String> getPropertyToQuery() {
    return propertyToQuery;
  }

  /**
   * Sets the property to query.
   *
   * @param propertyToQuery the property to query
   */
  public void setPropertyToQuery(HashMap<String, String> propertyToQuery) {
    this.propertyToQuery = propertyToQuery;
  }

  /**
   * Returns the property to query exact.
   *
   * @return the property to query exact
   */
  public HashMap<String, String> getPropertyToQueryExact() {
    return propertyToQueryExact;
  }

  /**
   * Sets the property to query exact.
   *
   * @param propertyToQueryExact the property to query exact
   */
  public void setPropertyToQueryExact(HashMap<String, String> propertyToQueryExact) {
    this.propertyToQueryExact = propertyToQueryExact;
  }

  /**
   * Returns the property to query contains.
   *
   * @return the property to query contains
   */
  public HashMap<String, String> getPropertyToQueryContains() {
    return propertyToQueryContains;
  }

  /**
   * Sets the property to query contains.
   *
   * @param propertyToQueryContains the property to query contains
   */
  public void setPropertyToQueryContains(HashMap<String, String> propertyToQueryContains) {
    this.propertyToQueryContains = propertyToQueryContains;
  }

  /**
   * Returns the main multiple nested query.
   *
   * @return the main multiple nested query
   */
  public String getMainMultipleNestedQuery() {
    return mainMultipleNestedQuery;
  }

  /**
   * Sets the main multiple nested query.
   *
   * @param mainMultipleNestedQuery the main multiple nested query
   */
  public void setMainMultipleNestedQuery(String mainMultipleNestedQuery) {
    this.mainMultipleNestedQuery = mainMultipleNestedQuery;
  }

  /**
   * Returns the main multiple nested query without highlights.
   *
   * @return the main multiple nested query without highlights
   */
  public String getMainMultipleNestedQueryWithoutHighlights() {
    return mainMultipleNestedQueryWithoutHighlights;
  }

  /**
   * Sets the main multiple nested query without highlights.
   *
   * @param mainMultipleNestedQueryWithoutHighlights the main multiple nested query without
   *     highlights
   */
  public void setMainMultipleNestedQueryWithoutHighlights(
      String mainMultipleNestedQueryWithoutHighlights) {
    this.mainMultipleNestedQueryWithoutHighlights = mainMultipleNestedQueryWithoutHighlights;
  }

  /**
   * Returns the rescore query.
   *
   * @return the rescore query
   */
  public String getRescoreQuery() {
    return rescoreQuery;
  }

  /**
   * Sets the rescore query.
   *
   * @param rescoreQuery the rescore query
   */
  public void setRescoreQuery(String rescoreQuery) {
    this.rescoreQuery = rescoreQuery;
  }

  /**
   * Returns the main nested with non nested query.
   *
   * @return the main nested with non nested query
   */
  public String getMainNestedWithNonNestedQuery() {
    return mainNestedWithNonNestedQuery;
  }

  /**
   * Sets the main nested with non nested query.
   *
   * @param mainNestedWithNonNestedQuery the main nested with non nested query
   */
  public void setMainNestedWithNonNestedQuery(String mainNestedWithNonNestedQuery) {
    this.mainNestedWithNonNestedQuery = mainNestedWithNonNestedQuery;
  }

  /**
   * Returns the main nested with non nested querywithout HL.
   *
   * @return the main nested with non nested querywithout HL
   */
  public String getMainNestedWithNonNestedQuerywithoutHL() {
    return mainNestedWithNonNestedQuerywithoutHL;
  }

  /**
   * Sets the main nested with non nested querywithout HL.
   *
   * @param mainNestedWithNonNestedQuerywithoutHL the main nested with non nested querywithout HL
   */
  public void setMainNestedWithNonNestedQuerywithoutHL(
      String mainNestedWithNonNestedQuerywithoutHL) {
    this.mainNestedWithNonNestedQuerywithoutHL = mainNestedWithNonNestedQuerywithoutHL;
  }
}
