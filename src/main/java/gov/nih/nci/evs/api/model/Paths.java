
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
   * Rewrite paths for ancestor. This method is for the CTRP "main menu
   * ancestors" computation.
   * @param ancestors the ancestors
   * @return the list
   */
  public List<Paths> rewritePathsForAncestors(final Set<String> ancestors) {

    final Map<String, Path> map = new HashMap<>();

    for (final Path path : paths) {
      final Path rewritePath = new Path();
      rewritePath.setConcepts(new ArrayList<>());
      rewritePath.setDirection(path.getDirection());
      int level = 0;
      final StringBuffer keysb = new StringBuffer();
      for (final Concept concept : path.getConcepts()) {
        if (ancestors.contains(concept.getCode())) {
          final Concept copy = new Concept(concept);
          copy.setLevel(level++);
          rewritePath.getConcepts().add(copy);
          keysb.append(copy.getCode()).append(",");
        }
      }
      // Only if we found something do we add it
      final String key = keysb.toString().replaceFirst(",$", "");
      if (level > 0 && !map.containsKey(key)) {
        map.put(key, rewritePath);
      }
    }
    // Keep the longest key that starts with a particular concept
    // see C104034
    final Paths paths = new Paths();
    final Set<String> seen = new HashSet<>();
    // Sort keys from longest to shortest
    for (final String key : map.keySet().stream().sorted((a, b) -> b.length() - a.length())
        .collect(Collectors.toList())) {
      // Skip if this key is already accounted for
      if (seen.contains(key)) {
        continue;
      }
      // Skip if all parts have each been individually seen
      final String[] parts = key.split("\\,");
      if (Arrays.asList(parts).stream().filter(p -> seen.contains(p)).count() == parts.length) {
        continue;
      }

      paths.getPaths().add(map.get(key));

      // Add all parts and subkeys to seen (so longest paths cover matching
      // shorter ones) but different paths get their own entries
      final StringBuilder keyPartSb = new StringBuilder();
      for (final String part : parts) {
        seen.add(part);
        if (keyPartSb.length() > 0) {
          keyPartSb.append(",");
        }
        keyPartSb.append(part);
        seen.add(keyPartSb.toString());
      }
    }
    final List<Paths> wrapper = new ArrayList<>();
    wrapper.add(paths);
    return wrapper;
  }

}
