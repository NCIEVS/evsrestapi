
package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents an disjointness association between two concepts.
 */
@Schema(description = "Represents an assertion of disjointness between two concepts")
@JsonInclude(Include.NON_EMPTY)
public class DisjointWith extends Relationship {
  /**
   * Instantiates an empty {@link DisjointWith}.
   */
  public DisjointWith() {
    // n/a
  }

  /**
   * Instantiates a {@link DisjointWith} from the specified parameters.
   *
   * @param other the other
   */
  public DisjointWith(final DisjointWith other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final DisjointWith other) {
    super.populateFrom(other);
  }

}
