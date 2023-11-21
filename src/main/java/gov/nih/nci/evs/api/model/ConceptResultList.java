
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a list of results with paging parameters.
 */
@Schema(description = "Represents a list of concepts returned from a search or find call")
public class ConceptResultList extends ResultList {

  /** The concepts. */
  private List<Concept> concepts;

  /**
   * Instantiates an empty {@link ConceptResultList}.
   */
  public ConceptResultList() {
    // n/a
  }

  /**
   * Instantiates a {@link ConceptResultList} from the specified parameters.
   *
   * @param other the other
   */
  public ConceptResultList(final ConceptResultList other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final ConceptResultList other) {
    super.populateFrom(other);
    concepts = new ArrayList<>(other.getConcepts());
  }

  /**
   * Returns the concepts.
   *
   * @return the concepts
   */
  @Schema(description = "List of concepts")
  public List<Concept> getConcepts() {
    if (concepts == null) {
      concepts = new ArrayList<>();
    }
    return concepts;
  }

  /**
   * Sets the concepts.
   *
   * @param concepts the concepts
   */
  public void setConcepts(final List<Concept> concepts) {
    this.concepts = concepts;
  }

}