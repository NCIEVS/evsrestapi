
package gov.nih.nci.evs.api.model;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Represents a path in a hierarchy (as a list of concepts with a direction flag).
 */
public class Path extends BaseModel {

  /** The direction. */
  @Field(type = FieldType.Integer)
  private int direction;

  /** The concepts. */
  @Field(type = FieldType.Nested)
  private List<ConceptNode> concepts;

  /**
   * Instantiates an empty {@link Path}.
   */
  public Path() {
  }

  /**
   * Instantiates a {@link Path} from the specified parameters.
   *
   * @param direction the direction
   * @param concepts the concepts
   */
  public Path(int direction, List<ConceptNode> concepts) {
    this.direction = direction;
    this.concepts = concepts;
  }

  /**
   * Sets the direction.
   *
   * @param direction the direction
   */
  public void setDirection(int direction) {
    this.direction = direction;
  }

  /**
   * Sets the concepts.
   *
   * @param concepts the concepts
   */
  public void setConcepts(List<ConceptNode> concepts) {
    this.concepts = concepts;
  }

  /**
   * Returns the direction.
   *
   * @return the direction
   */
  public int getDirection() {
    return this.direction;
  }

  /**
   * Returns the concepts.
   *
   * @return the concepts
   */
  public List<ConceptNode> getConcepts() {
    return this.concepts;
  }
}