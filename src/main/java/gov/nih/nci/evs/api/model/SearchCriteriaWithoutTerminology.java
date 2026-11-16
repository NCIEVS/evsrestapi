package gov.nih.nci.evs.api.model;

import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Search criteria object for /concept/search implementation without a terminology field. */
@Schema(description = "Criteria for a search or find operation (except for a terminology aspect)")
public class SearchCriteriaWithoutTerminology extends BaseModel {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger =
      LoggerFactory.getLogger(SearchCriteriaWithoutTerminology.class);

  /** The maximum number of codes allowed in a hierarchy code list parameter. */
  public static final int MAX_HIERARCHY_CODES = 100;

  /** The term. */
  private String term;

  /** The type. */
  private String type = "contains";

  /** The include. */
  private String include = "minimal";

  /** The sort. */
  private String sort;

  /** Ascending/Descending. */
  private Boolean ascending = null;

  /** The from record. */
  private Integer fromRecord = 0;

  /** The page size. */
  private Integer pageSize = 10;

  /** The concept status. */
  private List<String> conceptStatus;

  /** The property. */
  private List<String> property;

  /** The value. */
  private String value;

  /** The synonym source. */
  private List<String> synonymSource;

  /** The synonym type. */
  private List<String> synonymType;

  /** The definition source. */
  private List<String> definitionSource;

  /** The definition type. */
  private List<String> definitionType;

  /** The synonym term type. */
  private List<String> synonymTermType;

  /** The subset group. */
  private List<String> subset;

  /** The code list. */
  private List<String> codeList;

  /** The codes whose descendants results are restricted to. */
  private List<String> descendantOf;

  /** The codes whose descendants are excluded from results. */
  private List<String> notDescendantOf;

  /** The code whose ancestors results are restricted to. */
  private String ancestorOf;

  /** The code whose subtree (the code and its descendants) results must be in. */
  private String ancestor;

  /** The code whose ancestors, or the code itself, results must be. */
  private String descendant;

  /** The maximum depth below descendantOf codes. */
  private Integer maxLevel;

  /** Whether hierarchy anchor codes match themselves (default true). */
  private Boolean includeSelf;

  /** The inverse. */
  // private Boolean inverse = null;

  /** The association. */
  // private List<String> association;

  /** The role. */
  // private List<String> role;

  /** Instantiates an empty {@link SearchCriteriaWithoutTerminology}. */
  public SearchCriteriaWithoutTerminology() {
    // n/a
  }

  /**
   * Instantiates a {@link SearchCriteriaWithoutTerminology} from the specified parameters.
   *
   * @param other the other
   */
  public SearchCriteriaWithoutTerminology(final SearchCriteriaWithoutTerminology other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final SearchCriteriaWithoutTerminology other) {
    // association = new ArrayList<>(other.getAssociation());
    conceptStatus = new ArrayList<>(other.getConceptStatus());
    definitionSource = new ArrayList<>(other.getDefinitionSource());
    definitionType = new ArrayList<>(other.getDefinitionType());
    fromRecord = other.getFromRecord();
    include = other.getInclude();
    sort = other.getSort();
    ascending = other.getAscending();
    // inverse = other.getInverse();
    pageSize = other.getPageSize();
    property = new ArrayList<>(other.getProperty());
    value = other.getValue();

    // role = new ArrayList<>(other.getRole());
    synonymSource = new ArrayList<>(other.getSynonymSource());
    synonymType = new ArrayList<>(other.getSynonymType());
    synonymTermType = new ArrayList<>(other.getSynonymTermType());
    term = other.getTerm();
    type = other.getType();
    subset = new ArrayList<>(other.getSubset());
    codeList = new ArrayList<>(other.getCodeList());
    descendantOf = new ArrayList<>(other.getDescendantOf());
    notDescendantOf = new ArrayList<>(other.getNotDescendantOf());
    ancestorOf = other.getAncestorOf();
    ancestor = other.getAncestor();
    descendant = other.getDescendant();
    maxLevel = other.getMaxLevel();
    includeSelf = other.getIncludeSelf();
  }

  /**
   * Returns the term.
   *
   * @return the term
   */
  @Schema(description = "Search term, e.g. 'heart', or 'C3224'")
  public String getTerm() {
    return term;
  }

  /**
   * Sets the term.
   *
   * @param term the term
   */
  public void setTerm(final String term) {
    this.term = term;
  }

  /**
   * Returns the type.
   *
   * @return the type
   */
  @Schema(
      description = "The match type, one of: contains, match, startsWith, phrase, AND, OR, fuzzy")
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the type
   */
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * Returns the include.
   *
   * @return the include
   */
  @Schema(
      description =
          "Include parameter value, e.g. 'minimal', 'summary', 'synonyms,properties,children'")
  public String getInclude() {
    return include;
  }

  /**
   * Sets the include.
   *
   * @param include the include
   */
  public void setInclude(final String include) {
    this.include = include;
  }

  /**
   * Returns the sort by.
   *
   * @return the sort
   */
  @Schema(description = "Sort field")
  public String getSort() {
    return sort;
  }

  /**
   * Sets the sort by.
   *
   * @param sort the sort to set
   */
  public void setSort(final String sort) {
    this.sort = sort;
  }

  /**
   * Returns the ascending.
   *
   * @return the ascending
   */
  @Schema(
      description =
          "Indicates whether sort is ascending (true), descending (false), or not specified (null)")
  public Boolean getAscending() {
    return ascending;
  }

  /**
   * Sets the ascending.
   *
   * @param aescending the ascending
   */
  public void setAscending(final Boolean aescending) {
    this.ascending = aescending;
  }

  /**
   * Returns the from record.
   *
   * @return the from record
   */
  @Schema(description = "Start index of the search results")
  public Integer getFromRecord() {
    return fromRecord;
  }

  /**
   * Sets the from record.
   *
   * @param fromRecord the from record
   */
  public void setFromRecord(final Integer fromRecord) {
    this.fromRecord = fromRecord;
  }

  /**
   * Returns the page size.
   *
   * @return the page size
   */
  @Schema(description = "Max number of results to return")
  public Integer getPageSize() {
    return pageSize;
  }

  /**
   * Sets the page size.
   *
   * @param pageSize the page size
   */
  public void setPageSize(final Integer pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * Returns the concept status.
   *
   * @return the concept status
   */
  @Schema(
      description = "Comma-separated list of concept status values to restrict search results by")
  public List<String> getConceptStatus() {
    if (conceptStatus == null) {
      conceptStatus = new ArrayList<>();
    }
    return conceptStatus;
  }

  /**
   * Sets the concept status.
   *
   * @param conceptStatus the concept status
   */
  public void setConceptStatus(final List<String> conceptStatus) {
    this.conceptStatus = conceptStatus;
  }

  /**
   * Returns the property.
   *
   * @return the property
   */
  @Schema(description = "Comma-separated list of properties to restrict search results by")
  public List<String> getProperty() {
    if (property == null) {
      property = new ArrayList<>();
    }
    return property;
  }

  /**
   * Sets the property.
   *
   * @param property the property
   */
  public void setProperty(final List<String> property) {
    this.property = property;
  }

  /**
   * Returns the value.
   *
   * @return the value
   */
  @Schema(description = "A property value to restrict search results by")
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param value the value
   */
  public void setValue(final String value) {
    this.value = value;
  }

  /**
   * Returns the synonym source.
   *
   * @return the synonym source
   */
  @Schema(description = "Comma-separated list of synonym sources to restrict search results by")
  public List<String> getSynonymSource() {
    if (synonymSource == null) {
      synonymSource = new ArrayList<>();
    }
    return synonymSource;
  }

  /**
   * Sets the synonym source.
   *
   * @param synonymSource the synonym source
   */
  public void setSynonymSource(final List<String> synonymSource) {
    this.synonymSource = synonymSource;
  }

  /**
   * Returns the synonym type.
   *
   * @return the synonym type
   */
  @Schema(description = "Comma-separated list of synonym types to restrict search results by")
  public List<String> getSynonymType() {
    if (synonymType == null) {
      synonymType = new ArrayList<>();
    }
    return synonymType;
  }

  /**
   * Sets the synonym type.
   *
   * @param synonymType the synonym type
   */
  public void setSynonymType(final List<String> synonymType) {
    this.synonymType = synonymType;
  }

  /**
   * Returns the definition source.
   *
   * @return the definition source
   */
  @Schema(description = "Comma-separated list of definition sources to restrict search results by")
  public List<String> getDefinitionSource() {
    if (definitionSource == null) {
      definitionSource = new ArrayList<>();
    }
    return definitionSource;
  }

  /**
   * Sets the definition source.
   *
   * @param definitionSource the definition source
   */
  public void setDefinitionSource(final List<String> definitionSource) {
    this.definitionSource = definitionSource;
  }

  /**
   * Returns the definition type.
   *
   * @return the definition type
   */
  @Schema(description = "Comma-separated list of definition types to restrict search results by")
  public List<String> getDefinitionType() {
    if (definitionType == null) {
      definitionType = new ArrayList<>();
    }
    return definitionType;
  }

  /**
   * Sets the definition type.
   *
   * @param definitionType the definition type
   */
  public void setDefinitionType(final List<String> definitionType) {
    this.definitionType = definitionType;
  }

  /**
   * Returns the synonym term type.
   *
   * @return the synonym term type
   */
  @Schema(description = "Comma-separated list of synonym term types to restrict search results by")
  public List<String> getSynonymTermType() {
    if (synonymTermType == null) {
      synonymTermType = new ArrayList<>();
    }
    return synonymTermType;
  }

  /**
   * Sets the synonym term type.
   *
   * @param synonymTermType the synonym term type
   */
  public void setSynonymTermType(final List<String> synonymTermType) {
    this.synonymTermType = synonymTermType;
  }

  /**
   * Returns the inverse.
   *
   * @return the inverse
   */
  // public Boolean getInverse() {
  // return inverse;
  // }

  /**
   * Sets the inverse.
   *
   * @param inverse the inverse
   */
  // public void setInverse(final Boolean inverse) {
  // this.inverse = inverse;
  // }

  /**
   * Returns the association.
   *
   * @return the association
   */
  // public List<String> getAssociation() {
  // if (association == null) {
  // association = new ArrayList<>();
  // }
  // return association;
  // }

  /**
   * Sets the association.
   *
   * @param association the association
   */
  // public void setAssociation(final List<String> association) {
  // this.association = association;
  // }

  /**
   * Returns the role.
   *
   * @return the role
   */
  // public List<String> getRole() {
  // if (role == null) {
  // role = new ArrayList<>();
  // }
  // return role;
  // }

  /**
   * Sets the role.
   *
   * @param role the role
   */
  // public void setRole(final List<String> role) {
  // this.role = role;
  // }

  /**
   * @return the
   */
  @Schema(
      description = "Comma-separated list of subsets to restrict search results by, e.g. 'C157225'")
  public List<String> getSubset() {
    if (subset == null) {
      subset = new ArrayList<>();
    }
    return subset;
  }

  /**
   * Sets the subset.
   *
   * @param subset the subset
   */
  public void setSubset(List<String> subset) {
    this.subset = subset;
  }

  /**
   * Returns the code list.
   *
   * @return the list of codes to limit the search to
   */
  @Schema(
      description =
          "Comma-separated list of concept codes to restrict search results by, e.g. 'C157225'")
  public List<String> getCodeList() {
    if (codeList == null) {
      codeList = new ArrayList<>();
    }
    return codeList;
  }

  /**
   * Sets the list of codes to limit the search to.
   *
   * @param codeList the list of codes to limit the search to
   */
  public void setCodeList(List<String> codeList) {
    this.codeList = codeList;
  }

  /**
   * Returns the codes whose descendants results are restricted to.
   *
   * @return the descendant-of codes
   */
  @Schema(
      description =
          "Comma-separated list of concept codes; results are restricted to descendants of any of"
              + " them, e.g. 'C3262'")
  public List<String> getDescendantOf() {
    if (descendantOf == null) {
      descendantOf = new ArrayList<>();
    }
    return descendantOf;
  }

  /**
   * Sets the codes whose descendants results are restricted to.
   *
   * @param descendantOf the descendant-of codes
   */
  public void setDescendantOf(final List<String> descendantOf) {
    this.descendantOf = descendantOf;
  }

  /**
   * Returns the codes whose descendants are excluded from results.
   *
   * @return the not-descendant-of codes
   */
  @Schema(
      description =
          "Comma-separated list of concept codes; descendants of any of them are excluded from"
              + " results, e.g. 'C4872'")
  public List<String> getNotDescendantOf() {
    if (notDescendantOf == null) {
      notDescendantOf = new ArrayList<>();
    }
    return notDescendantOf;
  }

  /**
   * Sets the codes whose descendants are excluded from results.
   *
   * @param notDescendantOf the not-descendant-of codes
   */
  public void setNotDescendantOf(final List<String> notDescendantOf) {
    this.notDescendantOf = notDescendantOf;
  }

  /**
   * Returns the code whose ancestors results are restricted to.
   *
   * @return the ancestor-of code
   */
  @Schema(description = "Concept code; results are restricted to ancestors of it, e.g. 'C4872'")
  public String getAncestorOf() {
    return ancestorOf;
  }

  /**
   * Sets the code whose ancestors results are restricted to.
   *
   * @param ancestorOf the ancestor-of code
   */
  public void setAncestorOf(final String ancestorOf) {
    this.ancestorOf = ancestorOf;
  }

  /**
   * Returns the code whose subtree results must be in (the code itself is included).
   *
   * @return the ancestor code
   */
  @Schema(
      description =
          "Concept code; results are restricted to its subtree: the code itself and the concepts"
              + " that have it as an ancestor, e.g. 'C4872'")
  public String getAncestor() {
    return ancestor;
  }

  /**
   * Sets the code results must have as an ancestor.
   *
   * @param ancestor the ancestor code
   */
  public void setAncestor(final String ancestor) {
    this.ancestor = ancestor;
  }

  /**
   * Returns the code whose ancestors results must be (the code itself is included).
   *
   * @return the descendant code
   */
  @Schema(
      description =
          "Concept code; results are restricted to the code itself and the concepts that have it"
              + " as a descendant, e.g. 'C4872'")
  public String getDescendant() {
    return descendant;
  }

  /**
   * Sets the code results must have as a descendant.
   *
   * @param descendant the descendant code
   */
  public void setDescendant(final String descendant) {
    this.descendant = descendant;
  }

  /**
   * Returns the maximum depth below descendantOf codes.
   *
   * @return the max level
   */
  @Schema(
      description =
          "Maximum depth below the descendantOf codes, using the shortest path; 1 means direct"
              + " children")
  public Integer getMaxLevel() {
    return maxLevel;
  }

  /**
   * Sets the maximum depth below descendantOf codes.
   *
   * @param maxLevel the max level
   */
  public void setMaxLevel(final Integer maxLevel) {
    this.maxLevel = maxLevel;
  }

  /**
   * Returns whether hierarchy anchor codes match themselves. Null means true.
   *
   * @return the include self flag
   */
  @Schema(
      description =
          "Whether the descendantOf, notDescendantOf, and ancestorOf codes themselves count as"
              + " members of their branch (default true)")
  public Boolean getIncludeSelf() {
    return includeSelf;
  }

  /**
   * Sets whether hierarchy anchor codes match themselves.
   *
   * @param includeSelf the include self flag
   */
  public void setIncludeSelf(final Boolean includeSelf) {
    this.includeSelf = includeSelf;
  }

  /**
   * Indicates whether any hierarchy constraint is set.
   *
   * @return true, if descendantOf, notDescendantOf, ancestorOf, ancestor, or descendant is set
   */
  public boolean hasHierarchyConstraints() {
    return !getDescendantOf().isEmpty()
        || !getNotDescendantOf().isEmpty()
        || (ancestorOf != null && !ancestorOf.isBlank())
        || (ancestor != null && !ancestor.isBlank())
        || (descendant != null && !descendant.isBlank());
  }

  /**
   * Compute include param.
   *
   * @return the include param
   */
  public IncludeParam computeIncludeParam() {
    return new IncludeParam(include);
  }

  /**
   * Check required fields.
   *
   * @return true, if successful
   */
  public boolean checkRequiredFields() {
    return true;
  }

  /**
   * Check required fields.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public void checkPagination() throws Exception {
    checkPagination(1000);
  }

  /**
   * Check pagination.
   *
   * @param upperPageSize the upper page size
   * @throws Exception the exception
   */
  public void checkPagination(final int upperPageSize) throws Exception {
    if (pageSize < 1 || pageSize > upperPageSize) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Parameter 'pageSize' must be between 1 and " + upperPageSize + " = " + pageSize);
    }

    // This rule is no longer required, non-aligned fromRecord/pageSize
    // supported
    // if (fromRecord % pageSize != 0) {
    // throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
    // "Parameter 'fromRecord' should be the first record of a page, e.g.
    // fromRecord % pageSize == 0");
    // }
  }

  /**
   * Compute missing required fields.
   *
   * @return the string
   */
  public String computeMissingRequiredFields() {
    return "term";
  }

  /**
   * Validate.
   *
   * @param terminology the terminology instance
   * @param metadataService the metadata service
   * @throws Exception the exception
   */
  public void validate(final Terminology terminology, final MetadataService metadataService)
      throws Exception {
    // if (getTerm() == null) {
    // throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
    // "Required parameter 'term' is missing");
    // }

    if (!TerminologyUtils.asSet(
            "AND", "OR", "phrase", "exact", "contains", "fuzzy", "match", "startsWith")
        .contains(getType())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Required parameter 'type' has an invalid value = " + type);
    }

    if (fromRecord < 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Parameter 'fromRecord' must be >= 0 = " + fromRecord);
    }

    if ((pageSize < 1) || (pageSize > 1000)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Parameter 'pageSize' must be between 1 and 1000 = " + pageSize);
    }

    // Restrict paging for license-restricted terminologies (unless term is set)
    if (terminology.getMetadata().getLicenseText() != null
        && (term == null || term.isEmpty())
        && pageSize > 10) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          terminology.getMetadata().getUiLabel().replaceFirst(":.*", "")
              + " has license restrictions and so bulk operations are limited to working on 10"
              + " things at a time (page size = "
              + pageSize
              + ")");
    }

    // Validate concept status
    if (getConceptStatus().size() > 0) {
      final Set<String> conceptStatuses =
          new HashSet<>(metadataService.getConceptStatuses(terminology.getTerminology()).get());
      for (final String cs : getConceptStatus()) {
        if (!conceptStatuses.contains(cs)) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Parameter 'conceptStatus' has an invalid value = " + cs);
        }
      }
    }

    // Validate synonym source - must be a valid synonym source
    if (getSynonymSource().size() > 0) {
      final Set<String> synonymSources =
          metadataService.getSynonymSources(terminology.getTerminology()).stream()
              .map(c -> c.getCode())
              .collect(Collectors.toSet());
      for (final String ss : getSynonymSource()) {
        if (!synonymSources.contains(ss)) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Parameter 'synonymSource' has an invalid value = " + ss);
        }
      }
    }

    // Validate definition source - must be a valid definition source
    if (getDefinitionSource().size() > 0) {
      final Set<String> definitionSources =
          metadataService.getDefinitionSources(terminology.getTerminology()).stream()
              .map(c -> c.getCode())
              .collect(Collectors.toSet());
      for (final String ss : getDefinitionSource()) {
        if (!definitionSources.contains(ss)) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Parameter 'definitionSource' has an invalid value = " + ss);
        }
      }
    }

    validateHierarchyConstraints(terminology);
  }

  /**
   * Validate hierarchy constraints that do not require concept lookups. Anchor code existence is
   * checked where the search is executed.
   *
   * @param terminology the terminology
   */
  private void validateHierarchyConstraints(final Terminology terminology) {
    if (maxLevel != null) {
      if (maxLevel < 1) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Parameter 'maxLevel' must be >= 1 = " + maxLevel);
      }
      if (getDescendantOf().isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Parameter 'maxLevel' requires 'descendantOf'");
      }
    }

    if (!hasHierarchyConstraints()) {
      return;
    }

    if (!Boolean.TRUE.equals(terminology.getMetadata().getHierarchy())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Terminology '"
              + terminology.getTerminology()
              + "' does not support hierarchy parameters");
    }

    checkHierarchyCodes("descendantOf", getDescendantOf());
    checkHierarchyCodes("notDescendantOf", getNotDescendantOf());
    if (ancestorOf != null && ancestorOf.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Parameter 'ancestorOf' has an invalid value = " + ancestorOf);
    }
    if (ancestor != null && ancestor.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Parameter 'ancestor' has an invalid value = " + ancestor);
    }
    if (descendant != null && descendant.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Parameter 'descendant' has an invalid value = " + descendant);
    }
  }

  /**
   * Check a hierarchy code list for blank values and size.
   *
   * @param name the parameter name
   * @param codes the codes
   */
  private void checkHierarchyCodes(final String name, final List<String> codes) {
    if (codes.size() > MAX_HIERARCHY_CODES) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Parameter '" + name + "' allows at most " + MAX_HIERARCHY_CODES + " codes");
    }
    for (final String code : codes) {
      if (code == null || code.isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Parameter '" + name + "' has an invalid value = " + code);
      }
    }
  }

  /**
   * Hash code.
   *
   * @return the int
   */
  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    // result =
    // prime * result + ((association == null) ? 0 :
    // association.hashCode());
    result = prime * result + ((conceptStatus == null) ? 0 : conceptStatus.hashCode());
    result = prime * result + ((definitionSource == null) ? 0 : definitionSource.hashCode());
    result = prime * result + ((fromRecord == null) ? 0 : fromRecord.hashCode());
    result = prime * result + ((include == null) ? 0 : include.hashCode());
    result = prime * result + ((sort == null) ? 0 : sort.hashCode());
    result = prime * result + ((ascending == null) ? 0 : ascending.hashCode());
    // result = prime * result + ((inverse == null) ? 0 : inverse.hashCode());
    result = prime * result + ((pageSize == null) ? 0 : pageSize.hashCode());
    result = prime * result + ((property == null) ? 0 : property.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    // result = prime * result + ((role == null) ? 0 : role.hashCode());
    result = prime * result + ((synonymSource == null) ? 0 : synonymSource.hashCode());
    result = prime * result + ((synonymTermType == null) ? 0 : synonymTermType.hashCode());
    result = prime * result + ((term == null) ? 0 : term.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((subset == null) ? 0 : subset.hashCode());
    result = prime * result + ((codeList == null) ? 0 : codeList.hashCode());
    result = prime * result + ((descendantOf == null) ? 0 : descendantOf.hashCode());
    result = prime * result + ((notDescendantOf == null) ? 0 : notDescendantOf.hashCode());
    result = prime * result + ((ancestorOf == null) ? 0 : ancestorOf.hashCode());
    result = prime * result + ((ancestor == null) ? 0 : ancestor.hashCode());
    result = prime * result + ((descendant == null) ? 0 : descendant.hashCode());
    result = prime * result + ((maxLevel == null) ? 0 : maxLevel.hashCode());
    result = prime * result + ((includeSelf == null) ? 0 : includeSelf.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
   */
  /* see superclass */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    final SearchCriteriaWithoutTerminology other = (SearchCriteriaWithoutTerminology) obj;
    // if (association == null) {
    // if (other.association != null) {
    // return false;
    // }
    // } else if (!association.equals(other.association)) {
    // return false;
    // }
    if (conceptStatus == null) {
      if (other.conceptStatus != null) {
        return false;
      }
    } else if (!conceptStatus.equals(other.conceptStatus)) {
      return false;
    }
    if (definitionSource == null) {
      if (other.definitionSource != null) {
        return false;
      }
    } else if (!definitionSource.equals(other.definitionSource)) {
      return false;
    }
    if (fromRecord == null) {
      if (other.fromRecord != null) {
        return false;
      }
    } else if (!fromRecord.equals(other.fromRecord)) {
      return false;
    }
    if (include == null) {
      if (other.include != null) {
        return false;
      }
    } else if (!include.equals(other.include)) {
      return false;
    }
    if (sort == null) {
      if (other.sort != null) {
        return false;
      }
    } else if (!sort.equals(other.sort)) {
      return false;
    }
    if (ascending == null) {
      if (other.ascending != null) {
        return false;
      }
    } else if (!ascending.equals(other.ascending)) {
      return false;
    }
    // if (inverse == null) {
    // if (other.inverse != null) {
    // return false;
    // }
    // } else if (!inverse.equals(other.inverse)) {
    // return false;
    // }
    if (pageSize == null) {
      if (other.pageSize != null) {
        return false;
      }
    } else if (!pageSize.equals(other.pageSize)) {
      return false;
    }
    if (property == null) {
      if (other.property != null) {
        return false;
      }
    } else if (!property.equals(other.property)) {
      return false;
    }
    if (value == null) {
      if (other.value != null) {
        return false;
      }
    } else if (!value.equals(other.value)) {
      return false;
    }
    // if (role == null) {
    // if (other.role != null) {
    // return false;
    // }
    // } else if (!role.equals(other.role)) {
    // return false;
    // }
    if (synonymSource == null) {
      if (other.synonymSource != null) {
        return false;
      }
    } else if (!synonymSource.equals(other.synonymSource)) {
      return false;
    }
    if (synonymTermType == null) {
      if (other.synonymTermType != null) {
        return false;
      }
    } else if (!synonymTermType.equals(other.synonymTermType)) {
      return false;
    }
    if (subset == null) {
      if (other.subset != null) {
        return false;
      }
    } else if (!subset.equals(other.subset)) {
      return false;
    }
    if (codeList == null) {
      if (other.codeList != null) {
        return false;
      }
    } else if (!codeList.equals(other.codeList)) {
      return false;
    }
    if (descendantOf == null) {
      if (other.descendantOf != null) {
        return false;
      }
    } else if (!descendantOf.equals(other.descendantOf)) {
      return false;
    }
    if (notDescendantOf == null) {
      if (other.notDescendantOf != null) {
        return false;
      }
    } else if (!notDescendantOf.equals(other.notDescendantOf)) {
      return false;
    }
    if (ancestorOf == null) {
      if (other.ancestorOf != null) {
        return false;
      }
    } else if (!ancestorOf.equals(other.ancestorOf)) {
      return false;
    }
    if (ancestor == null) {
      if (other.ancestor != null) {
        return false;
      }
    } else if (!ancestor.equals(other.ancestor)) {
      return false;
    }
    if (descendant == null) {
      if (other.descendant != null) {
        return false;
      }
    } else if (!descendant.equals(other.descendant)) {
      return false;
    }
    if (maxLevel == null) {
      if (other.maxLevel != null) {
        return false;
      }
    } else if (!maxLevel.equals(other.maxLevel)) {
      return false;
    }
    if (includeSelf == null) {
      if (other.includeSelf != null) {
        return false;
      }
    } else if (!includeSelf.equals(other.includeSelf)) {
      return false;
    }
    if (term == null) {
      if (other.term != null) {
        return false;
      }
    } else if (!term.equals(other.term)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    return true;
  }
}
