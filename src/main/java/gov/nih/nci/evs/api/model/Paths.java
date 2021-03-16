
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a list of paths.
 */
@JsonInclude(Include.NON_EMPTY)
public class Paths extends BaseModel {

  /** The paths. */
  private List<Path> paths = null;

  /**
   * Instantiates an empty {@link Paths}.
   */
  public Paths() {
    paths = new ArrayList<Path>();
  }

  /**
   * Instantiates a {@link Paths} from the specified parameters.
   *
   * @param paths the paths
   */
  public Paths(List<Path> paths) {
    this.paths = paths;
  }

  /**
   * Sets the path.
   *
   * @param paths the path
   */
  public void setPaths(List<Path> paths) {
    this.paths = paths;
  }

  /**
   * Adds the.
   *
   * @param path the path
   */
  public void add(Path path) {
    this.paths.add(path);
  }

  /**
   * Returns the paths.
   *
   * @return the paths
   */
  public List<Path> getPaths() {
    return this.paths;
  }

  /**
   * Returns the path count.
   *
   * @return the path count
   */
  @JsonIgnore
  public int getPathCount() {
    return this.paths.size();
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((paths == null) ? 0 : paths.hashCode());
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
    Paths other = (Paths) obj;
    if (paths == null) {
      if (other.paths != null)
        return false;
    } else if (!paths.equals(other.paths))
      return false;
    return true;
  }

  /**
   * Returns the paths with ancestor.
   *
   * @param ancestors the ancestors
   * @return the paths with ancestor
   */
  public List<Path> getPathsWithAncestors(final Set<String> ancestors) {
    // Return filtered paths that contain a reference to specified code
    return getPaths().stream().filter(p -> p.getConcepts().stream()
        .filter(c -> ancestors.contains(c.getCode())).findFirst().orElse(null) != null)
        .collect(Collectors.toList());
  }

  /**
   * Rewrite paths for ancestor.
   * This method is for the CTRP "main menu ancestors" computation.
   * @param ancestors the ancestors
   * @return the list
   */
  public List<Paths> rewritePathsForAncestors(final Set<String> ancestors) {

    final Map<String, List<Path>> map = new HashMap<>();

    for (final Path path : paths) {
      final Path rewritePath = new Path();
      rewritePath.setDirection(path.getDirection());
      String broadCode = null;
      for (final Concept concept : path.getConcepts()) {
        rewritePath.getConcepts().add(concept);
        if (ancestors.contains(concept.getCode())) {
          broadCode = concept.getCode();
          break;
        }
      }
      if (!map.containsKey(broadCode)) {
        map.put(broadCode, new ArrayList<>());
      }
      map.get(broadCode).add(rewritePath);
    }
    return map.values().stream().map(l -> new Paths(l)).collect(Collectors.toList());

  }

}
