
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
   * @param mthAncestors the mma ancestors
   * @return the list
   */
  public List<Paths> rewritePaths(final Map<String, Paths> mainTypeHierarchy,
    final Set<String> mainTypeSet, final Set<String> broadCategorySet) {

    final Set<String> combined = Sets.union(mainTypeSet, broadCategorySet);
    final Map<String, Paths> map = new HashMap<>();
    final String code = getPaths().get(0).getConcepts().get(0).getCode();

    // SPECIAL handling for main type and broad category codes themselves.
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

    // Acquire set of MMAs from the path.

    // If not a main type/broad category concept

    // Determine if any paths go through main type concepts (or it is a main
    // type concept)
    boolean mtFlag = getPaths().stream().flatMap(p -> p.getConcepts().stream())
        .filter(c -> mainTypeSet.contains(c.getCode())).findFirst().orElse(null) != null;

    // TODO:::
    // direct path available in paths to C2991 not through neoplasm (for this
    // mma)
    // boolean directFlag = getPaths().stream()
    // .filter(p -> p.getConcepts().stream().filter(c ->
    // c.getCode().equals("C3262")).count() == 0
    // && p.getConcepts().stream().filter(c ->
    // c.getCode().equals("C2991")).count() > 0)
    // .findFirst().orElse(null) != null;

    // Keep only qualifying candidate paths (starting with only paths that go
    // through broad category concepts)
    final List<Path> candidatePaths = new ArrayList<>();
    for (final Path path : getPaths()
        .stream().filter(p -> p.getConcepts().stream()
            .filter(c -> broadCategorySet.contains(c.getCode())).count() > 0)
        .collect(Collectors.toList())) {

      // logger.info("XXX path = "
      // + path.getConcepts().stream().map(c ->
      // c.getCode()).collect(Collectors.toList()));

      // Skip paths that don't go through a main type concept if there are paths
      // that do
      if (mtFlag && path.getConcepts().stream().filter(c -> mainTypeSet.contains(c.getCode()))
          .findFirst().orElse(null) == null) {
        // logger.info("XXX SKIP mtFlag ");
        continue;
      }

      // TODO:
      // ALSO: if the "mma" here has direct paths to C2991 and this path goes
      // through neoplasms, skip
      // final String mma = path.getConcepts().stream().filter(c ->
      // combined.contains(c.getCode()))
      // .findFirst().get().getCode();

      // direct path available in paths to C2991 not through neoplasm (for this
      // mma)
      // boolean directFlag = getPaths().stream()
      // .filter(
      // p -> p.getConcepts().stream().filter(c ->
      // c.getCode().equals("C3262")).count() == 0
      // && p.getConcepts().stream().filter(c ->
      // c.getCode().equals("C2991")).count() > 0
      // && p.getConcepts().stream().filter(c ->
      // c.getCode().equals(mma)).count() > 0)
      // .findFirst().orElse(null) != null;

      // Skip paths that go through neoplasm if there are direct paths to C2991
      // if (directFlag && path.getConcepts().stream().filter(c ->
      // c.getCode().equals("C3262"))
      // .findFirst().orElse(null) != null) {
      // logger.info("XXX SKIP neoplasm ");
      // continue;
      // }

      candidatePaths.add(path);
    }

    // * Find all paths from the given concept to Disease or Disorder (Code
    // C2991) and concepts belonging to the broad category.
    long shortestLength = 100;
    final List<Path> shortestPathsRewritten = new ArrayList<>();
    for (final Path path : candidatePaths) {

      // logger.info("YYY path = "
      // + path.getConcepts().stream().map(c ->
      // c.getCode()).collect(Collectors.toList()));

      // Find the first main menu ancestor concept
      final String mma = path.getConcepts().stream().filter(c -> combined.contains(c.getCode()))
          .findFirst().get().getCode();

      // if mma is an ancestor of another main type concept that's in one of the
      // candidate paths, then skip it
      if (isMthAncestor(mma, combined, candidatePaths)) {
        // logger.info("YYY SKIP mth ancestor = " + mma);
        continue;
      }

      // * Find the length of the first mma concept to the top node in the main
      // type hierarchy
      int length = getDistance(path, mma, broadCategorySet);

      // logger.info("YYY length = " + length);
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
    // logger.info("YYY shortest length = " + shortestLength);
    // logger.info("YYY shortest paths = " + shortestPathsRewritten);

    final Paths rewritePaths = new Paths();
    rewritePaths.getPaths().addAll(shortestPathsRewritten);

    // For each main menu ancestor, get the corresponding paths
    for (final String mma : rewritePaths.getPaths().stream()
        .map(p -> p.getConcepts().get(0).getCode()).collect(Collectors.toSet())) {

      if (mainTypeHierarchy.containsKey(mma)) {
        // logger.info("YYY mma = " + mma);
        map.put(mma, mainTypeHierarchy.get(mma));
      } else {
        throw new RuntimeException("Main type hierarchy missing expected code = " + mma);
      }
    }

    // Sort paths by name of first concept
    final List<Paths> sortedPaths = map.values().stream()
        .sorted((a, b) -> a.getPaths().get(0).getConcepts().get(0).getCode()
            .compareTo(b.getPaths().get(0).getConcepts().get(0).getCode()))
        .collect(Collectors.toList());

    return sortedPaths;
  }

  /**
   * Indicates whether or not mth ancestor is the case.
   *
   * @param mma the mma
   * @param codes the codes
   * @param paths the paths
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  private boolean isMthAncestor(final String mma, final Set<String> codes, final List<Path> paths) {
    for (final Path path : paths) {
      boolean flag = false;
      for (final Concept concept : path.getConcepts()) {
        if (concept.getCode().equals(mma)) {
          // if another code was encountered before this mma, then
          // this is an ancestor and return true
          if (flag) {
            return true;
          }
          // otherwise, try the next path
          break;
        }
        if (codes.contains(concept.getCode())) {
          flag = true;
        }
      }
    }
    // if we didn't find an ancestor case, return false, mma is not an ancestor
    return false;
  }

  /**
   * Returns the distance from the "code" to the top entry from "codes".
   *
   * @param path the path
   * @param codes the codes
   * @return the distance
   */
  private int getDistance(final Path path, final String code, final Set<String> codes) {
    // ASSUMPTION, there is at least one code matching code.
    // ASSUMPTION, there is at least one code matching codes.
    int first = -1;
    int last = 0;
    int ct = 0;
    for (final Concept concept : path.getConcepts()) {
      // First is the index where "code" is encountered
      if (concept.getCode().equals(code)) {
        first = ct;
      }
      // last is the last index containing one of "codes"
      if (codes.contains(concept.getCode())) {
        last = ct;
      }
      ct++;
    }
    if (last < first) {
      throw new RuntimeException("Unexpected condition = " + last + ", " + first);
    }
    return last - first;
  }
}
