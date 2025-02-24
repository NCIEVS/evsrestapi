package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;

/** Represents an association between two concepts. */
@Schema(
    description =
        "Represents a non-defining (in a logical sense) relationship between two concepts")
@JsonInclude(Include.NON_EMPTY)
public class Association extends Relationship {
  /** Instantiates an empty {@link Association}. */
  public Association() {
    // n/a
  }

  /**
   * Instantiates a {@link Association} from the specified parameters.
   *
   * @param other the other
   */
  public Association(final Association other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Association other) {
    super.populateFrom(other);
  }
}
