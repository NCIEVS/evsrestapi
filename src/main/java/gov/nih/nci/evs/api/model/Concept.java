package gov.nih.nci.evs.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.DynamicMapping;
import org.springframework.data.elasticsearch.annotations.DynamicMappingValue;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Represents a concept with a code from a terminology.
 *
 * <pre>
 * {
 *   "code" : "C3224",
 *   "name" : "Melanoma",
 *   "leaf" : false,
 *   "synonyms" :[ {
 *     "name" : "MELANOMA, MALIGNANT",
 *     "termType" : "PT",
 *     "source" : "CDISC",
 *     "type" : "FULL_SYN"
 *   }, ... ],
 *   "definitions": [ ...
 *     {
 *     "definition" : "A form of cancer that begins in melanocytes (cells that make the pigment melanin). It may begin in a mole (skin melanoma), but can also begin in other pigmented tissues, such as in the eye or in the intestines.",
 *     "source" : "NCI-GLOSS",
 *     "type": "ALT_DEFINITION"
 *   } ],
 *   "properties" : [
 *     { "type": "Neoplastic_Status", "value": "Malignant" },
 *     { "type": "UMLS_CUI", "value": "C0025202" }, ...
 *   ]
 * }
 * </pre>
 */
@Schema(description = "Represents a concept in a terminology")
@Document(indexName = "default")
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Concept extends ConceptMinimal {

  /** The highlight. */
  @Transient @JsonSerialize @JsonDeserialize private String highlight;

  /** The highlights. */
  @Transient @JsonSerialize @JsonDeserialize private Map<String, String> highlights;

  /** The normName. */
  // In the future we can use @WriteOnlyProperty
  // this does not work: @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Keyword)
  private String normName;

  /** The stemName. */
  // In the future we can use @WriteOnlyProperty
  // this does not work: @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Text)
  private String stemName;

  /** The subset Link. */
  @Field(type = FieldType.Keyword)
  private String subsetLink;

  /** The mapset Link. */
  @Field(type = FieldType.Keyword)
  private String mapsetLink;

  /** The concept status. */
  @Field(type = FieldType.Keyword)
  private String conceptStatus;

  /** The source - only used by parent/child references for NCIM. */
  @Field(type = FieldType.Keyword)
  private String source;

  /** The leaf. */
  @Field(type = FieldType.Boolean)
  private Boolean leaf;

  /**
   * The active flag. Intentionally not on ConceptMinimal to avoid parents/children having to have
   * flag maintained
   */
  @Field(type = FieldType.Boolean)
  private Boolean active;

  /** The synonyms. */
  @Field(type = FieldType.Nested)
  private List<Synonym> synonyms;

  /** The definitions. */
  @Field(type = FieldType.Nested)
  private List<Definition> definitions;

  /** The properties. */
  @Field(type = FieldType.Nested)
  private List<Property> properties;

  /**
   * The children. Given this is a List<Concept>, we will not set enabled = false, even though we
   * don't need most of the field data associated with descendant. Otherwise, we are creating a self
   * circular reference that results in a StackOverflowError
   */
  @Field(
      type = FieldType.Object,
      ignoreFields = {
        "associations",
        "children",
        "descendants",
        "definitions",
        "disjointWith",
        "extensions",
        "history",
        "inverseAssociations",
        "inverseRoles",
        "maps",
        "parents",
        "paths",
        "properties",
        "qualifiers",
        "roles",
        "synonyms",
      })
  private List<Concept> children;

  /**
   * The parents. Given this is a List<Concept>, we will not set enabled = false, even though we
   * don't need most of the field data associated with parents. Otherwise, we are creating a self
   * circular reference that results in a StackOverflowError
   */
  @Field(
      type = FieldType.Object,
      ignoreFields = {
        "associations",
        "children",
        "descendants",
        "definitions",
        "disjointWith",
        "extensions",
        "history",
        "inverseAssociations",
        "inverseRoles",
        "maps",
        "parents",
        "paths",
        "properties",
        "qualifiers",
        "roles",
        "synonyms",
      })
  private List<Concept> parents;

  /**
   * The descendants. Given this is a List<Concept>, we will not set enabled = false even though we
   * don't need most of the field data associated with descendant. Otherwise, we are creating a self
   * circular reference that results in a StackOverflowError
   */
  @Field(
      type = FieldType.Object,
      ignoreFields = {
        "associations",
        "children",
        "descendants",
        "definitions",
        "disjointWith",
        "extensions",
        "history",
        "inverseAssociations",
        "inverseRoles",
        "maps",
        "parents",
        "paths",
        "properties",
        "qualifiers",
        "roles",
        "synonyms",
      })
  private List<Concept> descendants;

  /**
   * The qualifiers - only used by parent/child references for NCIM. The enabled = false will set
   * the index = false, to avoid indexing the fields in this Concept model. The
   * DynamicMappingValue.False will prevent indexing fields from the Qualifiers object.
   */
  @Field(type = FieldType.Object, includeInParent = false, enabled = false)
  @DynamicMapping(DynamicMappingValue.False)
  private List<Qualifier> qualifiers;

  /**
   * The associations. enabled = false will set the index = false, to avoid indexing the fields in
   * this Concept model
   */
  @Field(type = FieldType.Object, enabled = false)
  private List<Association> associations;

  /**
   * The inverse associations. enabled = false will set the index = false, to avoid indexing the
   * fields in this Concept model
   */
  @Field(type = FieldType.Object, enabled = false)
  private List<Association> inverseAssociations;

  /**
   * The roles. enabled = false will set the index = false, to avoid indexing the fields in this
   * Concept model
   */
  @Field(type = FieldType.Object, enabled = false)
  private List<Role> roles;

  /**
   * The disjoint with. enabled = false will set the index = false, to avoid indexing the fields in
   * this Concept model
   */
  @Field(type = FieldType.Object, enabled = false)
  private List<DisjointWith> disjointWith;

  /**
   * The inverse roles. enabled = false will set the index = false, to avoid indexing the fields in
   * this Concept model
   */
  @Field(type = FieldType.Object, enabled = false)
  private List<Role> inverseRoles;

  /**
   * The history. enabled = false will set the index = false, to avoid indexing the fields in this
   * Concept model. The DynamicMappingValue.False will prevent indexing fields from the History
   * object.
   */
  @Field(type = FieldType.Object, enabled = false)
  @DynamicMapping(DynamicMappingValue.False)
  private List<History> history;

  /**
   * The maps. enabled = false will set the index = false, to avoid indexing the fields in this
   * Concept model. The DynamicMappingValue.False will prevent indexing fields from the ConceptMap
   * object.
   */
  @Field(type = FieldType.Object, enabled = false)
  @DynamicMapping(DynamicMappingValue.False)
  private List<ConceptMap> maps;

  /**
   * The paths to root. enabled = false will set the index = false, to avoid indexing the fields in
   * this Concept model. The DynamicMappingValue.False will prevent indexing fields from the Paths
   * object.
   */
  @Field(type = FieldType.Object, enabled = false)
  @DynamicMapping(DynamicMappingValue.False)
  private Paths paths;

  /**
   * The paths to root. enabled = false will set the index = false, to avoid indexing the fields in
   * this Concept model. The DynamicMappingValue.False will prevent indexing fields from the
   * Extensions object.
   */
  @Field(type = FieldType.Object, enabled = false)
  @DynamicMapping(DynamicMappingValue.False)
  private Extensions extensions;

  /** Instantiates an empty {@link Concept}. */
  public Concept() {
    // n/a
  }

  /**
   * Instantiates a {@link Concept} from the specified parameters.
   *
   * @param code the code
   */
  public Concept(final String code) {
    super(code);
  }

  /**
   * Instantiates a {@link Concept} from the specified parameters.
   *
   * @param terminology the terminology
   * @param code the code
   * @param name the name
   */
  public Concept(final String terminology, final String code, final String name) {
    super(terminology, code, name);
  }

  /**
   * Instantiates a {@link Concept} from the specified parameters.
   *
   * @param other the other
   */
  public Concept(final Concept other) {
    populateFrom(other);
  }

  /**
   * Instantiates a {@link Concept} from the specified parameters.
   *
   * @param other the other
   */
  public Concept(final ConceptMinimal other) {
    super.populateFrom(other);
  }

  /**
   * Instantiates a {@link Concept} from the specified parameters.
   *
   * @param other the other
   */
  public Concept(final HierarchyNode other) {
    super(other.getCode());
    setName(other.getLabel());
    setLevel(other.getLevel());
    if (other.getLeaf() != null && other.getLeaf()) {
      leaf = other.getLeaf();
    }
    for (final HierarchyNode child : other.getChildren()) {
      getChildren().add(new Concept(child));
    }
  }

  /**
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Concept other) {
    populateFrom(other, false);
  }

  /**
   * Populate from.
   *
   * @param other the other
   * @param subsetFlag the no children - used for subsets retrieval
   */
  public void populateFrom(final Concept other, final boolean subsetFlag) {
    super.populateFrom(other);
    highlight = other.getHighlight();
    highlights = new HashMap<>(other.getHighlights());
    conceptStatus = other.getConceptStatus();
    leaf = other.getLeaf();
    active = other.getActive();
    normName = other.getNormName();
    stemName = other.getStemName();
    if (!subsetFlag) {
      subsetLink = other.getSubsetLink();
    }
    mapsetLink = other.getMapsetLink();
    conceptStatus = other.getConceptStatus();
    synonyms = new ArrayList<>(other.getSynonyms());
    definitions = new ArrayList<>(other.getDefinitions());
    history = new ArrayList<>(other.getHistory());
    properties = new ArrayList<>(other.getProperties());
    qualifiers = new ArrayList<>(other.getQualifiers());
    source = other.getSource();
    if (!subsetFlag) {
      children = new ArrayList<>(other.getChildren());
    }
    parents = new ArrayList<>(other.getParents());
    associations = new ArrayList<>(other.getAssociations());
    inverseAssociations = new ArrayList<>(other.getInverseAssociations());
    roles = new ArrayList<>(other.getRoles());
    inverseRoles = new ArrayList<>(other.getInverseRoles());
    disjointWith = new ArrayList<>(other.getDisjointWith());
    maps = new ArrayList<>(other.getMaps());
    if (other.getPaths() != null) {
      paths = other.getPaths();
    }
    extensions = other.getExtensions();
  }

  /**
   * Returns the highlight.
   *
   * @return the highlight
   */
  @Schema(
      description =
          "Used by search calls to provide information for highlighting a view of results")
  public String getHighlight() {
    return highlight;
  }

  /**
   * Sets the highlight.
   *
   * @param highlight the highlight
   */
  public void setHighlight(final String highlight) {
    this.highlight = highlight;
  }

  /**
   * Returns the highlights.
   *
   * @return the highlights
   */
  @Schema(hidden = true)
  public Map<String, String> getHighlights() {
    if (highlights == null) {
      highlights = new HashMap<>();
    }
    return highlights;
  }

  /**
   * Sets the highlights.
   *
   * @param highlights the highlights
   */
  public void setHighlights(final Map<String, String> highlights) {
    this.highlights = highlights;
  }

  /**
   * Returns the normName.
   *
   * @return the normName
   */
  @Schema(hidden = true)
  public String getNormName() {
    return normName;
  }

  /**
   * Sets the normName.
   *
   * @param normName the normName
   */
  public void setNormName(String normName) {
    this.normName = normName;
  }

  /**
   * @return the stemName
   */
  @Schema(hidden = true)
  public String getStemName() {
    return stemName;
  }

  /**
   * @param stemName the stemName to set
   */
  public void setStemName(String stemName) {
    this.stemName = stemName;
  }

  /**
   * Returns the subset link.
   *
   * @return the subsetLink
   */
  @Schema(
      description =
          "Link to download data for a subset, used when the concept represents subset metadata")
  public String getSubsetLink() {
    return subsetLink;
  }

  /**
   * Sets the subset link.
   *
   * @param subsetLink the subsetLink to set
   */
  public void setSubsetLink(String subsetLink) {
    this.subsetLink = subsetLink;
  }

  /**
   * Returns the mapset link.
   *
   * @return the mapsetLink
   */
  @Schema(description = "Metadata for downloading a mapset")
  public String getMapsetLink() {
    return mapsetLink;
  }

  /**
   * Sets the mapset link.
   *
   * @param mapsetLink the mapsetLink to set
   */
  public void setMapsetLink(String mapsetLink) {
    this.mapsetLink = mapsetLink;
  }

  /**
   * Returns the concept status.
   *
   * @return the conceptStatus
   */
  @Schema(description = "Status value for the concept, e.g. Retired_Concept")
  public String getConceptStatus() {
    return conceptStatus;
  }

  /**
   * Sets the concept status.
   *
   * @param conceptStatus the conceptStatus to set
   */
  public void setConceptStatus(String conceptStatus) {
    this.conceptStatus = conceptStatus;
  }

  /**
   * Returns the leaf.
   *
   * @return the leaf
   */
  @Schema(description = "Indicates whether concept is a leaf node")
  public Boolean getLeaf() {
    return leaf;
  }

  /**
   * Sets the leaf.
   *
   * @param leaf the leaf
   */
  public void setLeaf(final Boolean leaf) {
    this.leaf = leaf;
  }

  /**
   * Returns the active flag.
   *
   * @return the active flag
   */
  @Schema(description = "Indicates whether the concept is active")
  public Boolean getActive() {
    return active;
  }

  /**
   * Sets the active flag.
   *
   * @param active the active flag
   */
  public void setActive(final Boolean active) {
    this.active = active;
  }

  /**
   * Returns the synonyms.
   *
   * @return the synonyms
   */
  @Schema(
      description = "Synonyms, or all of the names for this concept, including the preferred name")
  public List<Synonym> getSynonyms() {
    if (synonyms == null) {
      synonyms = new ArrayList<>();
    }
    return synonyms;
  }

  /**
   * Sets the synonyms.
   *
   * @param synonyms the synonyms
   */
  public void setSynonyms(final List<Synonym> synonyms) {
    this.synonyms = synonyms;
  }

  /**
   * Returns the definitions.
   *
   * @return the definitions
   */
  @Schema(description = "Text definitions")
  public List<Definition> getDefinitions() {
    if (definitions == null) {
      definitions = new ArrayList<>();
    }
    return definitions;
  }

  /**
   * Sets the definitions.
   *
   * @param definitions the definitions
   */
  public void setDefinitions(final List<Definition> definitions) {
    this.definitions = definitions;
  }

  /**
   * Returns the history.
   *
   * @return the history
   */
  @Schema(description = "History records")
  public List<History> getHistory() {
    if (history == null) {
      history = new ArrayList<>();
    }
    return history;
  }

  /**
   * Sets the history.
   *
   * @param history the history
   */
  public void setHistory(final List<History> history) {
    this.history = history;
  }

  /**
   * Returns the properties.
   *
   * @return the properties
   */
  @Schema(description = "Key/value properties")
  public List<Property> getProperties() {
    if (properties == null) {
      properties = new ArrayList<>();
    }
    return properties;
  }

  /**
   * Sets the properties.
   *
   * @param properties the properties
   */
  public void setProperties(final List<Property> properties) {
    this.properties = properties;
  }

  /**
   * Returns the qualifiers.
   *
   * @return the qualifiers
   */
  @Schema(hidden = true)
  public List<Qualifier> getQualifiers() {
    if (qualifiers == null) {
      qualifiers = new ArrayList<>();
    }
    return qualifiers;
  }

  /**
   * Sets the qualifiers.
   *
   * @param qualifiers the qualifiers
   */
  @Schema(
      description =
          "Qualifiers for use when a concept is used as a parent/child - to indicate RELA for"
              + " NCIm-derived content")
  public void setQualifiers(final List<Qualifier> qualifiers) {
    this.qualifiers = qualifiers;
  }

  /**
   * Returns the source.
   *
   * @return the source
   */
  @Schema(description = "Associations from this concept to other ones")
  public String getSource() {
    return source;
  }

  /**
   * Sets the source.
   *
   * @param source the source
   */
  public void setSource(final String source) {
    this.source = source;
  }

  /**
   * Returns the children.
   *
   * @return the children
   */
  @Schema(description = "Child concepts in the hierarchy")
  public List<Concept> getChildren() {
    if (children == null) {
      children = new ArrayList<>();
    }
    return children;
  }

  /**
   * Sets the children.
   *
   * @param children the children
   */
  public void setChildren(final List<Concept> children) {
    this.children = children;
  }

  /**
   * Returns the parents.
   *
   * @return the parents
   */
  @Schema(description = "Parent concepts in the hierarchy")
  public List<Concept> getParents() {
    if (parents == null) {
      parents = new ArrayList<>();
    }
    return parents;
  }

  /**
   * Sets the parents.
   *
   * @param parents the parents
   */
  public void setParents(final List<Concept> parents) {
    this.parents = parents;
  }

  /**
   * Returns the descendants.
   *
   * @return the descendants
   */
  @Schema(description = "Descendant concepts in the hierarchy")
  public List<Concept> getDescendants() {
    if (descendants == null) {
      descendants = new ArrayList<>();
    }
    return descendants;
  }

  /**
   * Sets the descendants.
   *
   * @param descendants the descendants
   */
  public void setDescendants(final List<Concept> descendants) {
    this.descendants = descendants;
  }

  /**
   * Returns the associations.
   *
   * @return the associations
   */
  @Schema(description = "Associations from this concept to other ones")
  public List<Association> getAssociations() {
    if (associations == null) {
      associations = new ArrayList<>();
    }
    return associations;
  }

  /**
   * Sets the associations.
   *
   * @param associations the associations
   */
  public void setAssociations(final List<Association> associations) {
    this.associations = associations;
  }

  /**
   * Returns the inverse associations.
   *
   * @return the inverse associations
   */
  @Schema(description = "Associations to this concept from other ones")
  public List<Association> getInverseAssociations() {
    if (inverseAssociations == null) {
      inverseAssociations = new ArrayList<>();
    }
    return inverseAssociations;
  }

  /**
   * Sets the inverse associations.
   *
   * @param inverseAssociations the inverse associations
   */
  public void setInverseAssociations(final List<Association> inverseAssociations) {
    this.inverseAssociations = inverseAssociations;
  }

  /**
   * Returns the roles.
   *
   * @return the roles
   */
  @Schema(description = "Roles from this concept to other ones")
  public List<Role> getRoles() {
    if (roles == null) {
      roles = new ArrayList<>();
    }
    return roles;
  }

  /**
   * Sets the roles.
   *
   * @param roles the roles
   */
  public void setRoles(final List<Role> roles) {
    this.roles = roles;
  }

  /**
   * Returns the inverse roles.
   *
   * @return the inverse roles
   */
  @Schema(description = "Roles to this concept from other ones")
  public List<Role> getInverseRoles() {
    if (inverseRoles == null) {
      inverseRoles = new ArrayList<>();
    }
    return inverseRoles;
  }

  /**
   * Sets the inverse roles.
   *
   * @param inverseRoles the inverse roles
   */
  public void setInverseRoles(final List<Role> inverseRoles) {
    this.inverseRoles = inverseRoles;
  }

  /**
   * Returns the disjoint with.
   *
   * @return the disjoint with
   */
  @Schema(description = "Assertions of disjointness with respect to other concepts")
  public List<DisjointWith> getDisjointWith() {
    if (disjointWith == null) {
      disjointWith = new ArrayList<>();
    }
    return disjointWith;
  }

  /**
   * Sets the disjoint with.
   *
   * @param disjointWith the disjoint with
   */
  public void setDisjointWith(final List<DisjointWith> disjointWith) {
    this.disjointWith = disjointWith;
  }

  /**
   * Returns the maps.
   *
   * @return the maps
   */
  @Schema(description = "Maps from this concept to concepts in other terminologies")
  public List<ConceptMap> getMaps() {
    if (maps == null) {
      maps = new ArrayList<>();
    }
    return maps;
  }

  /**
   * Sets the maps.
   *
   * @param maps the maps
   */
  public void setMaps(final List<ConceptMap> maps) {
    this.maps = maps;
  }

  /**
   * Returns the paths.
   *
   * @return the paths
   */
  @Schema(description = "Paths for this concept in the hierarchy")
  public Paths getPaths() {
    return paths;
  }

  /**
   * Sets the paths.
   *
   * @param paths the paths to root
   */
  public void setPaths(Paths paths) {
    this.paths = paths;
  }

  /**
   * Returns the extensions.
   *
   * @return the extensions
   */
  @Schema(hidden = true)
  public Extensions getExtensions() {
    return extensions;
  }

  /**
   * Sets the extensions.
   *
   * @param extensions the extensions
   */
  public void setExtensions(Extensions extensions) {
    this.extensions = extensions;
  }

  /** Sort lists. */
  public void sortLists() {

    if (synonyms != null) {
      Collections.sort(synonyms);
    }
    if (definitions != null) {
      Collections.sort(definitions);
    }
    if (history != null) {
      Collections.sort(history);
    }
    if (properties != null) {
      Collections.sort(properties);
    }
    if (qualifiers != null) {
      Collections.sort(qualifiers);
    }
    if (children != null) {
      Collections.sort(children);
    }
    if (parents != null) {
      Collections.sort(parents);
    }
    if (descendants != null) {
      Collections.sort(descendants);
    }
    if (associations != null) {
      Collections.sort(associations);
    }
    if (inverseAssociations != null) {
      Collections.sort(inverseAssociations);
    }
    if (roles != null) {
      Collections.sort(roles);
    }
    if (inverseRoles != null) {
      Collections.sort(inverseRoles);
    }
    if (disjointWith != null) {
      Collections.sort(disjointWith);
    }
    if (maps != null) {
      Collections.sort(maps);
    }
  }

  /**
   * stream children.
   *
   * @return the stream
   */
  public Stream<Concept> streamSelfAndChildren() {
    return Stream.concat(
        Stream.of(this), getChildren().stream().flatMap(Concept::streamSelfAndChildren));
  }
}
