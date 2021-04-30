
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

    // IF this is a main/broad code, just look up the paths and return
    if (combined.contains(code)) {
      final List<Paths> paths = new ArrayList<>();
      // Remove first concept from each path because this is just ancestors
      final Paths copy = new Paths();
      for (final Path path : mainTypeHierarchy.get(code).getPaths()) {
        final Path removeFirstPath = new Path();
        removeFirstPath.setDirection(path.getDirection());
        removeFirstPath.setConcepts(new ArrayList<>(path.getConcepts()));
        removeFirstPath.getConcepts().removeIf(c -> c.getCode().equals(code));
        copy.getPaths().add(removeFirstPath);
      }
      paths.add(copy);
      return paths;
    }

    for (final Path path : getPaths()) {
      // logger.info("XXX = "
      // + path.getConcepts().stream().map(c ->
      // c.getCode()).collect(Collectors.toList()));

      // Eliminate all paths that visits any disease broad category concept
      // other than C2991 as an intermediate node (for example, the first path
      // above visits the broad category concept, Neoplasm (C3262), so it should
      // be eliminated.)
      boolean hasBroadCategory = path.getConcepts().stream()
          .filter(c -> broadCategorySet.contains(c.getCode()) && !c.getCode().equals("C2991"))
          .count() > 0;
      if (hasBroadCategory) {
        // logger.info(" SKIP (has broad category) = " +
        // path.getConcepts().stream()
        // .filter(c -> broadCategorySet.contains(c.getCode()) &&
        // !c.getCode().equals("C2991"))
        // .findFirst().get());
        continue;
      }

      //  Trim each path by removing all concepts (or nodes) in the path that
      // are not main types (as well as this concept itself)
      final Path rewritePath = path.rewritePath(combined);

      if (rewritePath != null) {
        // logger.info(" rewrite (combined set) = " +
        // rewritePath.getConcepts().stream()
        // .map(c -> c.getCode()).collect(Collectors.toList()));
        rewritePaths.getPaths().add(rewritePath);
      }
    }

    // For each main menu ancestor, get the corresponding paths
    for (final String mma : rewritePaths.getPaths().stream()
        .map(p -> p.getConcepts().get(0).getCode()).collect(Collectors.toSet())) {
      // if mma has another concept that is a child, it does not qualify
      // if mma is in a path and isn't the first concept, it's a parent
      boolean isMmaAncestor = rewritePaths.getPaths().stream()
          .filter(p -> p.getConcepts().stream().filter(c -> c.getCode().equals(mma)).count() > 0
              && !p.getConcepts().get(0).getCode().equals(mma))
          .count() > 0;
      // If the mma is a "broad category code" and the code is a "main type
      // code" then skip
      boolean flag = broadCategorySet.contains(mma) && mainTypeSet.contains(code);

      if (!isMmaAncestor && !flag) {
        // logger.info(" mma = " + mma);
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

}
