
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a path in a hierarchy (as a list of concepts with a direction
 * flag).
 */
@JsonInclude(Include.NON_EMPTY)
public class Path extends BaseModel {

  /** The direction. */
  private int direction;

  /** The concepts. */
  private List<Concept> concepts;

  /**
   * Instantiates an empty {@link Path}.
   */
  public Path() {
    // n/a
  }

  /**
   * Instantiates a {@link Path} from the specified parameters.
   *
   * @param direction the direction
   * @param concepts the concepts
   */
  public Path(int direction, List<Concept> concepts) {
    this.direction = direction;
    this.concepts = new ArrayList<>(concepts);
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
  public void setConcepts(List<Concept> concepts) {
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
  public List<Concept> getConcepts() {
    return this.concepts;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((concepts == null) ? 0 : concepts.hashCode());
    result = prime * result + direction;
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Path other = (Path) obj;
    if (concepts == null) {
      if (other.concepts != null)
        return false;
    } else if (!concepts.equals(other.concepts))
      return false;
    if (direction != other.direction)
      return false;
    return true;
  }

  /**
   * Rewrite path.
   *
   * @param include the include
   * @return the path
   */
  public Path rewritePath(final Set<String> include) {
    final Path path = new Path();
    path.setDirection(getDirection());
    path.setConcepts(new ArrayList<>());
    path.setDirection(path.getDirection());
    int level = 0;
    for (final Concept concept : getConcepts()) {
      if (include.contains(concept.getCode())) {
        final Concept copy = new Concept(concept);
        copy.setLevel(level++);
        path.getConcepts().add(copy);
      }
    }
    if (path.getConcepts().size() > 0) {
      return path;
    }
    return null;
  }

  /**
   * Returns the parent child.
   *
   * @return the parent child
   */
  public List<String> toParentChild() {
    int i = 0;
    final List<String> parentchild = new ArrayList<>();
    for (final Concept concept : getConcepts()) {
      if (i++ == 0) {
        continue;
      }
      final Concept parent = concept;
      final Concept child = getConcepts().get(i - 1);
      StringBuffer str = new StringBuffer();
      str.append(parent.getCode());
      str.append("\t");
      str.append(parent.getName());
      str.append("\t");
      str.append(child.getCode());
      str.append("\t");
      str.append(child.getName());
      str.append("\n");
      parentchild.add(str.toString());
    }
    return parentchild;
  }

  /**
   * Returns the path length from ancestor.
   *
   * @param ancestors the ancestors
   * @return the path length from ancestor
   */
  public int getPathLengthFromAncestor(final Set<String> ancestors) {
    int i = 0;
    for (final Concept concept : getConcepts()) {
      // Skip the first one
      if (i > 0 && ancestors.contains(concept.getCode())) {
        break;
      }
      i++;
    }
    return getConcepts().size() - i;
  }
}