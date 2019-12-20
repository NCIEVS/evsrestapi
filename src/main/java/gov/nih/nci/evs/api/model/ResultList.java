
package gov.nih.nci.evs.api.model;

/**
 * Represents a list of results with paging parameters.
 */
public class ResultList extends BaseModel {

  /** The total. */
  private Integer total;

  /** The from record. */
  private Integer fromRecord;

  /** The page size. */
  private Integer pageSize;

  /**
   * Instantiates an empty {@link ResultList}.
   */
  public ResultList() {
    // n/a
  }

  /**
   * Instantiates a {@link ResultList} from the specified parameters.
   *
   * @param other the other
   */
  public ResultList(final ResultList other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final ResultList other) {
    total = other.getTotal();
    fromRecord = other.getFromRecord();
    pageSize = other.getPageSize();
  }

  /**
   * Returns the total.
   *
   * @return the total
   */
  public Integer getTotal() {
    return total;
  }

  /**
   * Sets the total.
   *
   * @param total the total
   */
  public void setTotal(final Integer total) {
    this.total = total;
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

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((fromRecord == null) ? 0 : fromRecord.hashCode());
    result = prime * result + ((pageSize == null) ? 0 : pageSize.hashCode());
    result = prime * result + ((total == null) ? 0 : total.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ResultList other = (ResultList) obj;
    if (fromRecord == null) {
      if (other.fromRecord != null) {
        return false;
      }
    } else if (!fromRecord.equals(other.fromRecord)) {
      return false;
    }
    if (pageSize == null) {
      if (other.pageSize != null) {
        return false;
      }
    } else if (!pageSize.equals(other.pageSize)) {
      return false;
    }
    if (total == null) {
      if (other.total != null) {
        return false;
      }
    } else if (!total.equals(other.total)) {
      return false;
    }
    return true;
  }

}