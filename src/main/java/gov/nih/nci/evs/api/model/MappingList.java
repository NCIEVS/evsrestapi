
package gov.nih.nci.evs.api.model;

import java.util.List;

/**
 * Represents a list of results with paging parameters.
 */
public class MappingList {

  /** The total. */
  private Integer total;

  /** The maps. */
  private List<Map> maps;

  /**
   * Instantiates an empty {@link MappingList}.
   */
  public MappingList() {
    // n/a
  }

  /**
   * Instantiates a {@link MappingList} from the specified parameters.
   *
   * @param other the other
   */
  public MappingList(final MappingList other) {
    super.populateFrom(other);
    populateFrom(other);
  }

  public MappingList(Integer total, List<Map> mappings) {
    this.total = total;
    this.maps = mappings;
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final MappingList other) {
    super.populateFrom(other);
    total = other.getTotal();
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
   * @return the maps
   */
  public List<Map> getMaps() {
    return maps;
  }

  /**
   * @param maps the maps to set
   */
  public void setMaps(List<Map> maps) {
    this.maps = maps;
  }

  /**
   * Hash code.
   *
   * @return the int
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((maps == null) ? 0 : maps.hashCode());
    result = prime * result + ((total == null) ? 0 : total.hashCode());
    return result;
  }

  /**
   * Equals.
   *
   * @param obj the obj
   * @return true, if successful
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MappingList other = (MappingList) obj;
    if (maps == null) {
      if (other.maps != null)
        return false;
    } else if (!maps.equals(other.maps))
      return false;
    if (total == null) {
      if (other.total != null)
        return false;
    } else if (!total.equals(other.total))
      return false;
    return true;
  }

}