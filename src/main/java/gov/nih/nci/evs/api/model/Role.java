package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

/** Represents a role relationship between two concepts. */
@Schema(description = "Represents a (logically) defining relationship between two concepts")
@JsonInclude(Include.NON_EMPTY)
public class Role extends Relationship {
  /** Instantiates an empty {@link Role}. */
  public Role() {
    // n/a
  }

  /**
   * Instantiates a {@link Role} from the specified parameters.
   *
   * @param other the other
   */
  public Role(final Role other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Role other) {
    super.populateFrom(other);
  }
}
