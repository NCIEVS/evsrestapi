package gov.nih.nci.evs.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Represents choices about how much data to include when reading content. */
@Schema(hidden = true)
public class IncludeParam extends BaseModel {

  /** Separates the values in an include parameter. */
  private static final String VALUE_SEPARATOR = ",";

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

  /** The ancestors. */
  private boolean ancestors;

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

  /** The extensions. */
  private boolean extensions;

  /** The history. */
  private boolean history;

  /** The subset links. */
  private boolean subsetLink;

  /** The mapset links. */
  private boolean mapsetLink;

  /** Instantiates an empty {@link IncludeParam}. */
  public IncludeParam() {
    // n/a
  }

  /**
   * Instantiates an {@link IncludeParam} from an include parameter as a client wrote it: values
   * separated by commas, e.g. "summary,roles", or null for minimal. Spaces around a value and empty
   * values are ignored.
   *
   * @param include the include parameter, or null
   * @throws ResponseStatusException BAD_REQUEST naming the first unknown value
   */
  public IncludeParam(final String include) {
    this(getIncludes(include));
  }

  /**
   * Instantiates an {@link IncludeParam} turning on every field any of the values turns on.
   *
   * @param includes the include values
   */
  public IncludeParam(final Collection<Include> includes) {
    final Set<Include> fields = EnumSet.noneOf(Include.class);
    for (final Include include : includes) {
      fields.addAll(include.getFields());
    }

    synonyms = fields.contains(Include.SYNONYMS);
    definitions = fields.contains(Include.DEFINITIONS);
    properties = fields.contains(Include.PROPERTIES);
    children = fields.contains(Include.CHILDREN);
    descendants = fields.contains(Include.DESCENDANTS);
    ancestors = fields.contains(Include.ANCESTORS);
    parents = fields.contains(Include.PARENTS);
    associations = fields.contains(Include.ASSOCIATIONS);
    inverseAssociations = fields.contains(Include.INVERSE_ASSOCIATIONS);
    roles = fields.contains(Include.ROLES);
    inverseRoles = fields.contains(Include.INVERSE_ROLES);
    maps = fields.contains(Include.MAPS);
    highlights = fields.contains(Include.HIGHLIGHTS);
    disjointWith = fields.contains(Include.DISJOINT_WITH);
    paths = fields.contains(Include.PATHS);
    extensions = fields.contains(Include.EXTENSIONS);
    history = fields.contains(Include.HISTORY);
    subsetLink = fields.contains(Include.SUBSET_LINK);
    mapsetLink = fields.contains(Include.MAPSET_LINK);
  }

  /**
   * Returns the include values in an include parameter as a client wrote it.
   *
   * @param include the include parameter, or null for minimal
   * @return the include values
   * @throws ResponseStatusException BAD_REQUEST naming the first unknown value
   */
  private static List<Include> getIncludes(final String include) {
    final boolean hasInclude = include != null;
    if (!hasInclude) {
      return List.of(Include.MINIMAL);
    }

    final List<Include> includes = new ArrayList<>();
    for (final String untrimmedValue : include.split(VALUE_SEPARATOR)) {
      final String value = untrimmedValue.trim();
      final boolean isEmptyValue = value.isEmpty();
      if (isEmptyValue) {
        continue;
      }
      includes.add(getInclude(value, include));
    }
    return includes;
  }

  /**
   * Returns the include value a client wrote.
   *
   * @param value one value, trimmed
   * @param include the whole include parameter, to show in the error
   * @return the include value
   * @throws ResponseStatusException BAD_REQUEST naming the value and the allowed values
   */
  private static Include getInclude(final String value, final String include) {
    try {
      return Include.getByParameterValue(value);
    } catch (final IllegalArgumentException e) {
      final boolean isWholeParameter = value.equals(include);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Invalid includes value = "
              + value
              + (isWholeParameter ? "" : "; " + include)
              + "; allowed: "
              + String.join(", ", Include.getParameterValues()),
          e);
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
    ancestors = other.isAncestors();
    parents = other.isParents();
    associations = other.isAssociations();
    inverseAssociations = other.isInverseAssociations();
    roles = other.isRoles();
    inverseRoles = other.isInverseRoles();
    maps = other.isMaps();
    highlights = other.isHighlights();
    disjointWith = other.isDisjointWith();
    paths = other.isPaths();
    extensions = other.isExtensions();
    history = other.isHistory();
    subsetLink = other.isSubsetLink();
    mapsetLink = other.isMapsetLink();
  }

  /**
   * Checks for anything set.
   *
   * @return true, if successful
   */
  public boolean hasAnyTrue() {
    return synonyms
        || definitions
        || properties
        || children
        || descendants
        || ancestors
        || parents
        || associations
        || inverseAssociations
        || roles
        || inverseRoles
        || maps
        || disjointWith
        || paths
        || extensions
        || history;
  }

  /**
   * Returns included fields.
   *
   * @return the included fields
   */
  public String[] getIncludedFields() {
    List<String> fields =
        new ArrayList<>(
            Arrays.asList(
                "name",
                "code",
                "terminology",
                "leaf",
                "version",
                "uri",
                "active",
                "conceptStatus"));
    if (synonyms) {
      fields.add("synonyms");
    }
    if (definitions) {
      fields.add("definitions");
    }
    if (history) {
      fields.add("history");
    }
    if (properties) {
      fields.add("properties");
    }
    if (children) {
      fields.add("children");
    }
    if (parents) {
      fields.add("parents");
    }
    if (associations) {
      fields.add("associations");
    }
    if (inverseAssociations) {
      fields.add("inverseAssociations");
    }
    if (roles) {
      fields.add("roles");
    }
    if (inverseRoles) {
      fields.add("inverseRoles");
    }
    if (maps) {
      fields.add("maps");
    }
    if (highlights) {
      fields.add("highlights");
    }
    if (disjointWith) {
      fields.add("disjointWith");
    }
    if (subsetLink) {
      fields.add("subsetLink");
    }
    if (mapsetLink) {
      fields.add("mapsetLink");
    }
    if (paths) {
      fields.add("paths");
    }
    if (descendants) {
      fields.add("descendants");
    }
    if (ancestors) {
      fields.add("ancestors");
    }
    if (extensions) {
      fields.add("extensions");
    }
    return fields.toArray(new String[fields.size()]);
  }

  /**
   * Returns excluded fields.
   *
   * @return the excluded fields
   */
  public String[] getExcludedFields() {
    List<String> fields = new ArrayList<>();
    //    fields.add("normName"); // normName always excluded
    //    fields.add("stemName"); // stemName always excluded
    if (!synonyms) {
      fields.add("synonyms");
      //    } else {
      //      fields.add("synonyms.normName");
      //      fields.add("synonyms.stemName");
      //      fields.add("synonyms.typeCode");
      //      fields.add("synonyms.qualifiers.code");
    }
    if (!definitions) {
      fields.add("definitions");
      //    } else {
      //      fields.add("definitions.code");
      //      fields.add("definitions.qualifiers.code");
    }

    if (!history) {
      fields.add("history");
    }
    if (!properties) {
      fields.add("properties");
      //    } else {
      //      fields.add("properties.code");
      //      fields.add("properties.qualifiers.code");
    }
    if (!children) {
      fields.add("children");
    }
    if (!parents) {
      fields.add("parents");
    }
    if (!associations) {
      fields.add("associations");
      //    } else {
      //      fields.add("associations.code");
      //      fields.add("associations.qualifiers.code");
    }
    if (!inverseAssociations) {
      fields.add("inverseAssociations");
      //    } else {
      //      fields.add("inverseAssociations.code");
      //      fields.add("inverseAssociations.qualifiers.code");
    }
    if (!roles) {
      fields.add("roles");
      //    } else {
      //      fields.add("roles.code");
      //      fields.add("roles.qualifiers.code");
    }
    if (!inverseRoles) {
      fields.add("inverseRoles");
      //    } else {
      //      fields.add("inverseRoles.code");
      //      fields.add("inverseRoles.qualifiers.code");
    }
    if (!maps) {
      fields.add("maps");
    }
    if (!highlights) {
      fields.add("highlights");
    }
    if (!disjointWith) {
      fields.add("disjointWith");
      //    } else {
      //      fields.add("disjointWith.code");
    }

    if (!subsetLink) {
      fields.add("subsetLink");
    }
    if (!mapsetLink) {
      fields.add("mapsetLink");
    }
    if (!paths) {
      fields.add("paths");
    }
    if (!descendants) {
      fields.add("descendants");
    }
    if (!ancestors) {
      fields.add("ancestors");
    }
    if (!extensions) {
      fields.add("extensions");
    }
    return fields.toArray(new String[fields.size()]);
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
   * Indicates whether or not history is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isHistory() {
    return history;
  }

  /**
   * Sets the history.
   *
   * @param history the history
   */
  public void setHistory(boolean history) {
    this.history = history;
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
   * Indicates whether or not ancestors is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isAncestors() {
    return ancestors;
  }

  /**
   * Sets the ancestors.
   *
   * @param ancestors the ancestors
   */
  public void setAncestors(boolean ancestors) {
    this.ancestors = ancestors;
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
   * @param paths the paths
   */
  public void setPaths(boolean paths) {
    this.paths = paths;
  }

  /**
   * Indicates whether or not extensions is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isExtensions() {
    return extensions;
  }

  /**
   * Sets the extensions.
   *
   * @param extensions the extensions
   */
  public void setExtensions(boolean extensions) {
    this.extensions = extensions;
  }

  /**
   * Indicates whether or not SubsetLink is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isSubsetLink() {
    return subsetLink;
  }

  /**
   * Sets the subsetLink.
   *
   * @param subsetLink the subsetLink
   */
  public void setSubsetLink(boolean subsetLink) {
    this.subsetLink = subsetLink;
  }

  /**
   * Indicates whether or not SubsetLink is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isMapsetLink() {
    return mapsetLink;
  }

  /**
   * Sets the subsetLink.
   *
   * @param mapsetLink the mapset link
   */
  public void setMapsetLink(boolean mapsetLink) {
    this.mapsetLink = mapsetLink;
  }
}
