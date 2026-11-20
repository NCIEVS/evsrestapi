package gov.nih.nci.evs.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

/** Represents a list of Map results with paging parameters. */
@Schema(description = "Represents a list of maps returned from a search or find call")
public class MappingResultList extends ResultList {

  /** The maps. */
  private List<Mapping> maps;

  /** Instantiates an empty {@link MappingResultList}. */
  public MappingResultList() {
    // n/a
  }

  /**
   * Instantiates a {@link MappingResultList} from the specified parameters.
   *
   * @param other the other
   */
  public MappingResultList(final MappingResultList other) {
    populateFrom(other);
  }

  /**
   * Instantiates a {@link MappingResultList} from the specified parameters.
   *
   * @param total the total
   * @param maps the maps
   */
  public MappingResultList(final long total, final List<Mapping> maps) {
    super.setTotal(total);
    this.maps = new ArrayList<>(maps);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final MappingResultList other) {
    super.populateFrom(other);
    maps = new ArrayList<>(other.getMaps());
  }

  /**
   * Returns the maps.
   *
   * @return the maps
   */
  @Schema(description = "List of maps")
  public List<Mapping> getMaps() {
    return maps;
  }

  /**
   * Sets the maps.
   *
   * @param maps the maps to set
   */
  public void setMaps(List<Mapping> maps) {
    this.maps = maps;
  }
}
