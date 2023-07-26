
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a list of Map results with paging parameters.
 */
public class MapResultList extends ResultList {

  /** The maps. */
  private List<Map> maps;

  /**
   * Instantiates an empty {@link MapResultList}.
   */
  public MapResultList() {
    // n/a
  }

  /**
   * Instantiates a {@link MapResultList} from the specified parameters.
   *
   * @param other the other
   */
  public MapResultList(final MapResultList other) {
    populateFrom(other);
  }

  /**
   * Instantiates a {@link MapResultList} from the specified parameters.
   *
   * @param total the total
   * @param maps the maps
   */
  public MapResultList(final int total, final List<Map> maps) {
    super.setTotal(total);
    this.maps = new ArrayList<>(maps);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final MapResultList other) {
    super.populateFrom(other);
    maps = new ArrayList<>(other.getMaps());
  }

  /**
   * Returns the maps.
   *
   * @return the maps
   */
  public List<Map> getMaps() {
    return maps;
  }

  /**
   * Sets the maps.
   *
   * @param maps the maps to set
   */
  public void setMaps(List<Map> maps) {
    this.maps = maps;
  }

}