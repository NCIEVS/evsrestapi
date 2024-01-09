
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a list of Map results with paging parameters.
 */
@Schema(description = "Represents a list of maps returned from a search or find call")
public class ConceptMapResultList extends ResultList {

  /** The maps. */
  private List<ConceptMap> maps;

  /**
   * Instantiates an empty {@link ConceptMapResultList}.
   */
  public ConceptMapResultList() {
    // n/a
  }

  /**
   * Instantiates a {@link ConceptMapResultList} from the specified parameters.
   *
   * @param other the other
   */
  public ConceptMapResultList(final ConceptMapResultList other) {
    populateFrom(other);
  }

  /**
   * Instantiates a {@link ConceptMapResultList} from the specified parameters.
   *
   * @param total the total
   * @param maps the maps
   */
  public ConceptMapResultList(final long total, final List<ConceptMap> maps) {
    super.setTotal(total);
    this.maps = new ArrayList<>(maps);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final ConceptMapResultList other) {
    super.populateFrom(other);
    maps = new ArrayList<>(other.getMaps());
  }

  /**
   * Returns the maps.
   *
   * @return the maps
   */
  @Schema(description = "List of maps")
  public List<ConceptMap> getMaps() {
    return maps;
  }

  /**
   * Sets the maps.
   *
   * @param maps the maps to set
   */
  public void setMaps(List<ConceptMap> maps) {
    this.maps = maps;
  }

}