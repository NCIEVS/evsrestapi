package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Represents a path in a hierarchy (as a list of concepts with a direction flag). */
@Schema(description = "Represents a path (or partial path) in a hierarchy")
@JsonInclude(Include.NON_EMPTY)
public class Path extends BaseModel {

  /** The direction. 1 means from node to root. -1 means from root to node. */
  private int direction;

  /** The concepts. */
  private List<ConceptMinimal> concepts;

  /** Instantiates an empty {@link Path}. */
  public Path() {
    // n/a
  }

  /**
   * Instantiates a {@link Path} from the specified parameters.
   *
   * @param other the other
   */
  public Path(final Path other) {
    direction = other.getDirection();
    concepts = new ArrayList<>(other.getConcepts());
  }

  /**
   * Instantiates a {@link Path} from the specified parameters.
   *
   * @param direction the direction
   * @param concepts the concepts
   */
  public Path(int direction, List<ConceptMinimal> concepts) {
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
  public void setConcepts(List<ConceptMinimal> concepts) {
    this.concepts = concepts;
  }

  /**
   * Returns the direction. 1 means from node to root. -1 means from root to node.
   *
   * @return the direction
   */
  @Schema(description = "Direction of the map (1 means node-to-root, -1 means root-to-node)")
  public int getDirection() {
    return direction;
  }

  /**
   * Returns the concepts.
   *
   * @return the concepts
   */
  @Schema(description = "Concepts on the path")
  public List<ConceptMinimal> getConcepts() {
    if (concepts == null) {
      concepts = new ArrayList<>(5);
    }
    return concepts;
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
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Path other = (Path) obj;
    if (concepts == null) {
      if (other.concepts != null) return false;
    } else if (!concepts.equals(other.concepts)) return false;
    if (direction != other.direction) return false;
    return true;
  }

  /**
   * Rewrite path.
   *
   * @param include the include
   * @return the path
   */
  public Path rewritePath(final Set<String> include) {
    return rewritePath(include, false);
  }

  /**
   * Rewrite path.
   *
   * @param include the include
   * @param keepFirst the keep first
   * @return the path
   */
  public Path rewritePath(final Set<String> include, final boolean keepFirst) {
    final Path path = new Path();
    path.setDirection(getDirection());
    path.setConcepts(new ArrayList<>());
    path.setDirection(path.getDirection());
    int level = 0;
    boolean first = true;
    for (final ConceptMinimal concept : getConcepts()) {
      // Skip first concept
      if (!keepFirst && first) {
        first = false;
        continue;
      }
      if (include.contains(concept.getCode())) {
        final ConceptMinimal copy = new ConceptMinimal(concept);
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
    for (final ConceptMinimal concept : getConcepts()) {
      if (i++ == 0) {
        continue;
      }
      final ConceptMinimal parent = concept;
      final ConceptMinimal child = getConcepts().get(i - 1);
      final StringBuffer str = new StringBuffer();
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
    boolean found = false;
    for (final ConceptMinimal concept : getConcepts()) {
      // Skip the first one
      if (i > 0 && ancestors.contains(concept.getCode())) {
        found = true;
        break;
      }
      i++;
    }
    if (!found) {
      return -1;
    }
    return getConcepts().size() - i;
  }
}
