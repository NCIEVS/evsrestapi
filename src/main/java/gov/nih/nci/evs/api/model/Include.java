package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A value of the {@code include} parameter: a concept field to return, or a shortcut for several.
 * The single list of what {@code include} accepts; {@link IncludeParam} is built from it, and the
 * API documentation lists its values. Clients write the {@link #getParameterValue parameter value},
 * e.g. {@code inverseRoles}, comma-separated, and get every field any value turns on.
 */
public enum Include {

  /** No lists, just the fields every concept returns: code, name, terminology, status. */
  MINIMAL("minimal") {
    @Override
    public Set<Include> getFields() {
      return EnumSet.noneOf(Include.class);
    }
  },

  /** Synonyms, definitions, and properties. */
  SUMMARY("summary") {
    @Override
    public Set<Include> getFields() {
      return EnumSet.of(SYNONYMS, DEFINITIONS, PROPERTIES);
    }
  },

  /** Every field except the large lists: descendants, ancestors, paths, and extensions. */
  FULL("full") {
    @Override
    public Set<Include> getFields() {
      final Set<Include> fields = EVERYTHING.getFields();
      fields.removeAll(EnumSet.of(DESCENDANTS, ANCESTORS, PATHS, EXTENSIONS));
      return fields;
    }
  },

  /** Every field. */
  EVERYTHING("*") {
    @Override
    public Set<Include> getFields() {
      return Arrays.stream(values())
          .filter(Include::getIsField)
          .collect(Collectors.toCollection(() -> EnumSet.noneOf(Include.class)));
    }
  },

  /** The concept's synonyms. */
  SYNONYMS("synonyms"),

  /** The concept's definitions. */
  DEFINITIONS("definitions"),

  /** The concept's properties. */
  PROPERTIES("properties"),

  /** The concept's direct children. */
  CHILDREN("children"),

  /** The concept's direct parents. */
  PARENTS("parents"),

  /** Every concept below the concept. Large. */
  DESCENDANTS("descendants"),

  /** Every concept above the concept. Large. */
  ANCESTORS("ancestors"),

  /** The concept's roles. */
  ROLES("roles"),

  /** The roles of other concepts that point to the concept. */
  INVERSE_ROLES("inverseRoles"),

  /** The concept's associations. */
  ASSOCIATIONS("associations"),

  /** The associations of other concepts that point to the concept. */
  INVERSE_ASSOCIATIONS("inverseAssociations"),

  /** The concept's maps to other terminologies. */
  MAPS("maps"),

  /** The concept's history. */
  HISTORY("history"),

  /** Which part of each concept matched the search. */
  HIGHLIGHTS("highlights"),

  /** The concepts the concept is disjoint with. */
  DISJOINT_WITH("disjointWith"),

  /** The concept's paths to the root. Large. */
  PATHS("paths"),

  /** The concept's extensions. Large. */
  EXTENSIONS("extensions"),

  /** A link to the subset, for a subset concept. */
  SUBSET_LINK("subsetLink"),

  /** A link to the mapset, for a mapset concept. */
  MAPSET_LINK("mapsetLink");

  /** Separates the values in an include parameter. */
  private static final String SEPARATOR = ",";

  /** The values that stand for several fields rather than one. */
  private static final Set<Include> SHORTCUTS = EnumSet.of(MINIMAL, SUMMARY, FULL, EVERYTHING);

  /** The value as clients write it, e.g. "inverseRoles". */
  private final String parameterValue;

  /**
   * Instantiates an {@link Include}.
   *
   * @param parameterValue the value as clients write it
   */
  Include(final String parameterValue) {
    this.parameterValue = parameterValue;
  }

  /**
   * Returns the value as clients write it, e.g. "inverseRoles". Also how it's written in JSON and
   * listed in the API documentation.
   *
   * @return the parameter value
   */
  @JsonValue
  public String getParameterValue() {
    return parameterValue;
  }

  /**
   * Returns the fields this value turns on: a field turns on itself, a shortcut its set.
   *
   * @return the fields
   */
  public Set<Include> getFields() {
    return EnumSet.of(this);
  }

  /**
   * Indicates whether this value is one field rather than a shortcut for several.
   *
   * @return whether it's a field
   */
  public boolean getIsField() {
    return !SHORTCUTS.contains(this);
  }

  /**
   * Returns the value a client wrote.
   *
   * @param parameterValue the value as the client wrote it, e.g. "inverseRoles"
   * @return the include value
   * @throws IllegalArgumentException if no value is written that way, naming it and the allowed
   *     values
   */
  public static Include getByParameterValue(final String parameterValue) {
    return Arrays.stream(values())
        .filter(include -> include.parameterValue.equals(parameterValue))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unknown include value '"
                        + parameterValue
                        + "'; allowed: "
                        + String.join(", ", getParameterValues())));
  }

  /**
   * Returns every value as clients write it, in declaration order.
   *
   * @return the parameter values
   */
  public static List<String> getParameterValues() {
    return Arrays.stream(values()).map(Include::getParameterValue).toList();
  }

  /**
   * Returns the include values as one parameter, as a client would write it, e.g. "summary,roles".
   *
   * @param includes the include values
   * @return the parameter
   */
  public static String getParameter(final Collection<Include> includes) {
    return includes.stream().map(Include::getParameterValue).collect(Collectors.joining(SEPARATOR));
  }
}
