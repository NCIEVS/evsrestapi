
package gov.nih.nci.evs.api.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Represents choices about how much data to include when reading content.
 */
public class IncludeParam extends BaseModel {

  /** The synonyms. */
  private boolean synonyms;

  /** The definitions. */
  private boolean definitions;

  /** The properties. */
  private boolean properties;

  /** The children. */
  private boolean children;

  /** The descendants. */
  private boolean descendants;

  /** The parents. */
  private boolean parents;

  /** The associations. */
  private boolean associations;

  /** The inverse associations. */
  private boolean inverseAssociations;

  /** The roles. */
  private boolean roles;

  /** The inverse roles. */
  private boolean inverseRoles;

  /** The maps. */
  private boolean maps;

  /** The disjoint classes. */
  private boolean disjointWith;

  /** The highlights. */
  private boolean highlights;

  /** The paths. */
  private boolean paths;
  
  /**
   * Instantiates an empty {@link IncludeParam}.
   */
  public IncludeParam() {
    // n/a
  }

  /**
   * Instantiates a {@link IncludeParam} from the specified parameters.
   *
   * @param include the include
   */
  public IncludeParam(final String include) {
    if (include == null) {
      populateMinimal();
    } else {
      for (final String partPreTrim : include.split(",")) {
        final String part = partPreTrim.trim();
        if (part.equals("minimal")) {
          populateMinimal();
        } else if (part.equals("summary")) {
          populateSummary();
        } else if (part.equals("full")) {
          populateFull();
        } else if (part.equals("synonyms")) {
          synonyms = true;
        } else if (part.equals("definitions")) {
          definitions = true;
        } else if (part.equals("properties")) {
          properties = true;
        } else if (part.equals("children")) {
          children = true;
        } else if (part.equals("descendants")) {
          descendants = true;
        } else if (part.equals("parents")) {
          parents = true;
        } else if (part.equals("associations")) {
          associations = true;
        } else if (part.equals("inverseAssociations")) {
          inverseAssociations = true;
        } else if (part.equals("roles")) {
          roles = true;
        } else if (part.equals("inverseRoles")) {
          inverseRoles = true;
        } else if (part.equals("maps")) {
          maps = true;
        } else if (part.equals("highlights")) {
          highlights = true;
        } else if (part.equals("disjointWith")) {
          disjointWith = true;
        } else if (part.equals("paths")) {
          paths = true;
        } else {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Invalid includes value = " + part
                  + (part.equals(include) ? "" : "; " + include));
        }
      }
    }
  }

  /**
   * Instantiates a {@link IncludeParam} from the specified parameters.
   *
   * @param other the other
   */
  public IncludeParam(final IncludeParam other) {
    populateFrom(other);
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final IncludeParam other) {
    synonyms = other.isSynonyms();
    definitions = other.isDefinitions();
    properties = other.isProperties();
    children = other.isChildren();
    descendants = other.isDescendants();
    parents = other.isParents();
    associations = other.isAssociations();
    inverseAssociations = other.isInverseAssociations();
    roles = other.isRoles();
    inverseRoles = other.isInverseRoles();
    maps = other.isMaps();
    highlights = other.isHighlights();
    disjointWith = other.isDisjointWith();
    paths = other.isPaths();

  }

  /**
   * Populate minimal.
   */
  public void populateMinimal() {
    // n/a - this involves setting actually nothing right now because code/name
    // are required.
  }

  /**
   * Populate summary.
   */
  public void populateSummary() {
    synonyms = true;
    definitions = true;
    properties = true;
  }

  /**
   * Populate full.
   */
  public void populateFull() {
    synonyms = true;
    definitions = true;
    properties = true;
    children = true;
    descendants = true;
    parents = true;
    associations = true;
    inverseAssociations = true;
    roles = true;
    inverseRoles = true;
    maps = true;
    highlights = true;
    disjointWith = true;
    paths = true;
  }

  /**
   * Checks for anything set.
   *
   * @return true, if successful
   */
  public boolean hasAnyTrue() {
    return synonyms || definitions || properties || children || descendants || parents
        || associations || inverseAssociations || roles || inverseRoles || maps
        || disjointWith || paths;
  }

  /**
   * Indicates whether or not synonyms is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isSynonyms() {
    return synonyms;
  }

  /**
   * Sets the synonyms.
   *
   * @param synonyms the synonyms
   */
  public void setSynonyms(boolean synonyms) {
    this.synonyms = synonyms;
  }

  /**
   * Indicates whether or not definitions is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isDefinitions() {
    return definitions;
  }

  /**
   * Sets the definitions.
   *
   * @param definitions the definitions
   */
  public void setDefinitions(boolean definitions) {
    this.definitions = definitions;
  }

  /**
   * Indicates whether or not properties is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isProperties() {
    return properties;
  }

  /**
   * Sets the properties.
   *
   * @param properties the properties
   */
  public void setProperties(boolean properties) {
    this.properties = properties;
  }

  /**
   * Indicates whether or not children is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isChildren() {
    return children;
  }

  /**
   * Sets the children.
   *
   * @param children the children
   */
  public void setChildren(boolean children) {
    this.children = children;
  }

  /**
   * Indicates whether or not descendant is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isDescendants() {
    return descendants;
  }

  /**
   * Sets the descendants.
   *
   * @param descendants the descendants
   */
  public void setDescendant(boolean descendants) {
    this.descendants = descendants;
  }

  /**
   * Indicates whether or not parents is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isParents() {
    return parents;
  }

  /**
   * Sets the parents.
   *
   * @param parents the parents
   */
  public void setParents(boolean parents) {
    this.parents = parents;
  }

  /**
   * Indicates whether or not associations is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isAssociations() {
    return associations;
  }

  /**
   * Sets the associations.
   *
   * @param associations the associations
   */
  public void setAssociations(boolean associations) {
    this.associations = associations;
  }

  /**
   * Indicates whether or not inverse associations is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isInverseAssociations() {
    return inverseAssociations;
  }

  /**
   * Sets the inverse associations.
   *
   * @param inverseAssociations the inverse associations
   */
  public void setInverseAssociations(boolean inverseAssociations) {
    this.inverseAssociations = inverseAssociations;
  }

  /**
   * Indicates whether or not roles is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isRoles() {
    return roles;
  }

  /**
   * Sets the roles.
   *
   * @param roles the roles
   */
  public void setRoles(boolean roles) {
    this.roles = roles;
  }

  /**
   * Indicates whether or not inverse roles is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isInverseRoles() {
    return inverseRoles;
  }

  /**
   * Sets the inverse roles.
   *
   * @param inverseRoles the inverse roles
   */
  public void setInverseRoles(boolean inverseRoles) {
    this.inverseRoles = inverseRoles;
  }

  /**
   * Indicates whether or not maps is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isMaps() {
    return maps;
  }

  /**
   * Sets the maps.
   *
   * @param maps the maps
   */
  public void setMaps(boolean maps) {
    this.maps = maps;
  }

  /**
   * Indicates whether or not highlights is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isHighlights() {
    return highlights;
  }

  /**
   * Sets the highlights.
   *
   * @param highlights the highlights
   */
  public void setHighlights(boolean highlights) {
    this.highlights = highlights;
  }

  /**
   * Indicates whether or not disjoint is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isDisjointWith() {
    return disjointWith;
  }

  /**
   * Sets the disjoint.
   *
   * @param disjoint the disjoint
   */
  public void setDisjointWith(boolean disjoint) {
    this.disjointWith = disjoint;
  }

  /**
   * Indicates whether or not paths is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isPaths() {
    return paths;
  }

  /**
   * Sets the disjoint.
   *
   * @param disjoint the disjoint
   */
  public void setPaths(boolean paths) {
    this.paths = paths;
  }
}
