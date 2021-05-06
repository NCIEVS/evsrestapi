
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Sets;

/**
 * Represents a list of paths.
 */
@JsonInclude(Include.NON_EMPTY)
public class Paths extends BaseModel {

  /** The Constant logger. */
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
  public List<Paths> rewritePaths(final Map<String, Paths> mainTypeHierarchy,
    final Set<String> mainTypeSet, final Set<String> broadCategorySet) {

    final Paths rewritePaths = new Paths();
    final Set<String> combined = Sets.union(mainTypeSet, broadCategorySet);
    final Map<String, Paths> map = new HashMap<>();
    final String code = getPaths().get(0).getConcepts().get(0).getCode();

    // If this is a main/broad code, return main type hierarchy
    if (combined.contains(code)) {
      final List<Paths> paths = new ArrayList<>();
      // Remove first concept from each path because this is just ancestors
      final Paths copy = new Paths();
      // Identify the skipped main type concepts
      if (!mainTypeHierarchy.containsKey(code)) {
        return paths;
      }
      for (final Path path : mainTypeHierarchy.get(code).getPaths()) {
        final Path removeFirstPath = new Path();
        removeFirstPath.setDirection(path.getDirection());
        removeFirstPath.setConcepts(new ArrayList<>(path.getConcepts()));
        removeFirstPath.getConcepts().removeIf(c -> c.getCode().equals(code));
        // if there are any concepts left in the path, add it
        if (removeFirstPath.getConcepts().size() > 0) {
          copy.getPaths().add(removeFirstPath);
        }
      }
      // Only add if there are actually any paths
      if (copy.getPaths().size() > 0) {
        paths.add(copy);
      }
      return paths;
    }

    // If not a main type/broad category concept

    // Determine if any paths go through main type concepts (or it is a main
    // type concept)
    boolean mtFlag = getPaths().stream().flatMap(p -> p.getConcepts().stream())
        .filter(c -> mainTypeSet.contains(c.getCode())).findFirst().orElse(null) != null;

    // Determine if there are paths that go directly to C2991 without going
    // through Neoplasm C3262
    boolean directFlag = getPaths().stream()
        .filter(p -> p.getConcepts().stream().filter(c -> c.getCode().equals("C3262")).count() == 0
            && p.getConcepts().stream().filter(c -> c.getCode().equals("C2991")).count() > 0)
        .findFirst().orElse(null) != null;

    // * Find all paths from the given concept to Disease or Disorder (Code
    // C2991) and concepts belonging to the broad category.
    long shortestLength = 100;
    final List<Path> shortestPathsRewritten = new ArrayList<>();
    for (final Path path : getPaths()
        .stream().filter(p -> p.getConcepts().stream()
            .filter(c -> broadCategorySet.contains(c.getCode())).count() > 0)
        .collect(Collectors.toList())) {

      // If there is a path through a main type concept and this
      // path doesn't have one, skip it
      if (mtFlag && path.getConcepts().stream().filter(c -> mainTypeSet.contains(c.getCode()))
          .findFirst().orElse(null) == null) {
        continue;
      }

      // If there are direct paths to C2991 and this goes
      // through Neoplasm C3262, skip
      if (directFlag && path.getConcepts().stream().filter(c -> c.getCode().equals("C3262"))
          .findFirst().orElse(null) != null) {
        continue;
      }

      logger.info("XXX path = "
          + path.getConcepts().stream().map(c -> c.getCode()).collect(Collectors.toList()));

      // * Find the length of the first mma concept to the top node (only
      // looking
      //      long length = path.getConcepts().stream().filter(c -> combined.contains(c.getCode())).count();
       long length = getDistance(path, mainTypeHierarchy.keySet());

      logger.info("XXX length = " + length);
      if (length <= shortestLength) {
        if (length < shortestLength) {
          shortestLength = length;
          shortestPathsRewritten.clear();
        }
        // * Trim each path by removing all concepts (or nodes) in the path that
        // are not main types (as well as this concept itself)
        final Path rewritePath = path.rewritePath(combined);
        if (rewritePath != null) {
          shortestPathsRewritten.add(rewritePath);
        }
      }

    }
    logger.info("XXX shortest length = " + shortestLength);
    logger.info("XXX shortest paths = " + shortestPathsRewritten);

    rewritePaths.getPaths().addAll(shortestPathsRewritten);

    // For each main menu ancestor, get the corresponding paths
    for (final String mma : rewritePaths.getPaths().stream()
        .map(p -> p.getConcepts().get(0).getCode()).collect(Collectors.toSet())) {
      // if mma has another concept that is a child, it does not qualify
      // if mma is in a path and isn't the first concept, it's a parent
      boolean isMmaAncestor = rewritePaths.getPaths().stream()
          .filter(p -> p.getConcepts().stream().filter(c -> c.getCode().equals(mma)).count() > 0
              && !p.getConcepts().get(0).getCode().equals(mma))
          .count() > 0;

      if (!isMmaAncestor && mainTypeHierarchy.containsKey(mma)) {
        logger.info("XXX mma = " + mma);
        map.put(mma, mainTypeHierarchy.get(mma));
      }
    }

    // Sort paths by name of first concept
    final List<Paths> sortedPaths = map.values().stream()
        .sorted((a, b) -> a.getPaths().get(0).getConcepts().get(0).getName()
            .compareTo(b.getPaths().get(0).getConcepts().get(0).getName()))
        .collect(Collectors.toList());

    return sortedPaths;
  }

  /**
   * Returns the distance.
   *
   * @param path the path
   * @param codes the codes
   * @return the distance
   */
  private long getDistance(final Path path, final Set<String> codes) {
    // ASSUMPTION, there is at least one code matching codes.
    long first = -1;
    long last = 0;
    long ct = 0;
    for (final Concept concept : path.getConcepts()) {
      if (codes.contains(concept.getCode())) {
        if (first == -1) {
          first = ct;
        }
        last = ct;
      }
      ct++;
    }
    return last - first;
  }
}
