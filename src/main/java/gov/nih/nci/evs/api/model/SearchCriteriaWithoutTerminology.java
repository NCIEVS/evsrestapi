
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * Search criteria object for /concept/search implementation without a terminology field.
 */
public class SearchCriteriaWithoutTerminology extends BaseModel {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(SearchCriteriaWithoutTerminology.class);

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

  /** The inverse. */
  // private Boolean inverse = null;

  /** The association. */
  // private List<String> association;

  /** The role. */
  // private List<String> role;

  /**
   * Instantiates an empty {@link SearchCriteriaWithoutTerminology}.
   */
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
  }

  /**
   * Returns the term.
   *
   * @return the term
   */
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
    if (term == null) {
      return false;
    }
    return true;
  }

  /**
   * Check required fields.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public void checkPagination() throws Exception {
    if (pageSize < 1 || pageSize > 1000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Parameter 'pageSize' must be between 1 and 1000 = " + pageSize);

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
  public void validate(final Terminology terminology, final MetadataService metadataService) throws Exception {
    // if (getTerm() == null) {
    // throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
    // "Required parameter 'term' is missing");
    // }

    if (!TerminologyUtils.asSet("AND", "OR", "phrase", "exact", "contains", "fuzzy", "match", "startsWith")
        .contains(getType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Required parameter 'type' has an invalid value = " + type);
    }

    if (fromRecord < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter 'fromRecord' must be >= 0 = " + fromRecord);
    }

    if ((pageSize < 1) || (pageSize > 1000)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Parameter 'pageSize' must be between 1 and 1000 = " + pageSize);
    }

    // Restrict paging for license-restricted terminologies (unless term is set)
    if (terminology.getMetadata().getLicenseText() != null && (term == null || term.isEmpty()) && pageSize > 10) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          terminology.getMetadata().getUiLabel().replaceFirst(":.*", "")
              + " has license restrictions and so bulk operations are limited to working on 10 things at a time "
              + "(page size = " + pageSize + ")");
    }

    // Validate concept status
    if (getConceptStatus().size() > 0) {
      final Set<String> conceptStatuses =
          new HashSet<>(metadataService.getConceptStatuses(terminology.getTerminology()).get());
      for (final String cs : getConceptStatus()) {
        if (!conceptStatuses.contains(cs)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Parameter 'conceptStatus' has an invalid value = " + cs);
        }
      }
    }

    // Validate synonym source - must be a valid synonym source
    if (getSynonymSource().size() > 0) {
      final Set<String> synonymSources = metadataService.getSynonymSources(terminology.getTerminology()).stream()
          .map(c -> c.getCode()).collect(Collectors.toSet());
      for (final String ss : getSynonymSource()) {
        if (!synonymSources.contains(ss)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Parameter 'synonymSource' has an invalid value = " + ss);
        }
      }
    }

    // Validate definition source - must be a valid definition source
    if (getDefinitionSource().size() > 0) {
      final Set<String> definitionSources = metadataService.getDefinitionSources(terminology.getTerminology()).stream()
          .map(c -> c.getCode()).collect(Collectors.toSet());
      for (final String ss : getDefinitionSource()) {
        if (!definitionSources.contains(ss)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Parameter 'definitionSource' has an invalid value = " + ss);
        }
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
