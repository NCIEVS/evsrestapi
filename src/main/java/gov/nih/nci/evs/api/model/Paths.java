
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a list of paths.
 */
@JsonInclude(Include.NON_EMPTY)
public class Paths extends BaseModel {

  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(Paths.class);

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
   * Rewrite paths for ancestor. This method is for the CTRP "main menu
   * ancestors" computation.
   *
   * @param mainTypeHierarchy the main type hierarchy
   * @param mainTypeSet the ancestors
   * @param broadCategorySet the broad category set
   * @return the list
   */
  public List<Paths> rewritePaths(final Set<String> mainTypeSet,
    final Set<String> broadCategorySet) {

    final Paths rewritePaths = new Paths();

    for (final Path path : getPaths()) {
      // Eliminate all paths that visits any disease broad category concept
      // other than C2991 as an intermediate node (for example, the first path
      // above visits the broad category concept, Neoplasm (C3262), so it should
      // be eliminated.)
      boolean hasBroadCategory = path.getConcepts().stream()
          .filter(c -> broadCategorySet.contains(c.getCode()) && !c.getCode().equals("C2991"))
          .count() > 0;
      if (hasBroadCategory) {
        continue;
      }

      //  Trim each path by removing all concepts (or nodes) in the path that
      // are not main types (as well as this concept itself)
      Path rewritePath = path.rewritePath(mainTypeSet);
      if (rewritePath != null) {
        rewritePaths.getPaths().add(rewritePath);
      } else {
        // If the path is null, rewrite just to broad category
        // paths to satisfy cases where the main menu ancestor just says
        // "disease"
        rewritePath = path.rewritePath(broadCategorySet);
        if (rewritePath != null) {
          rewritePaths.getPaths().add(rewritePath);
        }

      }
    }

    // Sort paths in length order (longest first)
    rewritePaths.setPaths(rewritePaths.getPaths().stream()
        .sorted((a, b) -> b.getConcepts().size() - a.getConcepts().size())
        .collect(Collectors.toList()));

    final Map<String, Paths> map = new HashMap<>();
    final Set<String> seen = new HashSet<>();
    // Iterate through sorted paths
    for (final Path path : rewritePaths.getPaths()) {

      final String mma = path.getConcepts().get(0).getCode();
      final Paths paths = map.containsKey(mma) ? map.get(mma) : new Paths();

      // Avoid keeping a path of just "C2991 Disease or Disorder" if there are
      // other paths. Longest paths are seen first, so this will always come
      // after a "real" one
      if (!seen.isEmpty() && path.getConcepts().size() == 1 && mma.contentEquals("C2991")) {
        continue;
      }
      // If not all concepts in the path have been seen, then add the path
      if (path.getConcepts().stream().filter(c -> !seen.contains(c.getCode())).count() > 0) {
        paths.getPaths().add(path);

        // Only if we've added a path do we add "paths" to the map.
        map.put(mma, paths);

        // Add all concepts to seen
        path.getConcepts().stream().peek(c -> seen.add(c.getCode())).count();
      }

    }

    // Sort paths by name of first concept
    final List<Paths> sortedPaths = map.values().stream()
        .sorted((a, b) -> a.getPaths().get(0).getConcepts().get(0).getName()
            .compareTo(b.getPaths().get(0).getConcepts().get(0).getName()))
        .collect(Collectors.toList());

    return sortedPaths;
  }

}
