
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.common.util.set.Sets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import gov.nih.nci.evs.api.util.HierarchyUtils;

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
   *
   * @param mainTypeHierarchy the main type hierarchy
   * @param mainTypeSet the ancestors
   * @param broadCategorySet the broad category set
   * @return the list
   */
  public List<Paths> rewritePaths(final HierarchyUtils mainTypeHierarchy,
    final Set<String> mainTypeSet) {

    final Paths rewritePaths = null;
    
    
    Main Menu Ancestors
    Given a disease concept that is not a main type concept, it is desirable to find main menu ancestors of the concept that meet the following conditions.
       A main menu ancestor is a main type concept.
       A main menu ancestor is an ancestor of the given concept according to the NCI Thesaurus.
       A main menu ancestor of a concept cannot have a child concept that is a main menu ancestor of the same concept

    
    
    // Go through paths and find leaf node main type ancestors
    // From that, construct paths from the main type hierarchy
    mainTypeHierarchy.getsup

    final List<Paths> wrapper = new ArrayList<>();
    if (rewritePaths.getPaths().size() > 0) {
      wrapper.add(rewritePaths);
      return wrapper;
    }
    return null;
  }

}
