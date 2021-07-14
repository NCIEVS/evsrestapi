
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import gov.nih.nci.evs.api.service.ElasticOperationsService;

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
 *     "termGroup" : "PT",
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
@Document(indexName = "default", type = ElasticOperationsService.CONCEPT_TYPE)
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Concept extends ConceptMinimal {

  /** The highlight. */
  @Transient
  @JsonSerialize
  @JsonDeserialize
  private String highlight;

  /** The highlights. */
  @Transient
  @JsonSerialize
  @JsonDeserialize
  private java.util.Map<String, String> highlights;

  /** The normName. */
  @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Keyword)
  private String normName;

  /** The normName. */
  @Field(type = FieldType.Keyword)
  private String subsetLink;

  /** The concept status. */
  @JsonProperty(access = Access.READ_ONLY)
  @Field(type = FieldType.Keyword)
  private String conceptStatus;

  /** The level. */
  @Field(type = FieldType.Integer)
  private Integer level;

  /** The leaf. */
  @Field(type = FieldType.Boolean)
  private Boolean leaf;

  /** The synonyms. */
  @Field(type = FieldType.Nested)
  private List<Synonym> synonyms;

  /** The definitions. */
  @Field(type = FieldType.Nested)
  private List<Definition> definitions;

  /** The properties. */
  @Field(type = FieldType.Nested)
  private List<Property> properties;

  /** The children. */
  @Field(type = FieldType.Nested, ignoreFields = {
      "parents", "children", "descendants", "paths", "extensions"
  })
  private List<Concept> children;

  /** The parents. */
  @Field(type = FieldType.Nested, ignoreFields = {
      "parents", "children", "descendants", "paths", "extensions"
  })
  private List<Concept> parents;

  /** The descendants. */
  @Field(type = FieldType.Nested, ignoreFields = {
      "parents", "children", "descendants", "paths", "extensions"
  })
  private List<Concept> descendants;

  /** The associations. */
  @Field(type = FieldType.Nested)
  private List<Association> associations;

  /** The inverse associations. */
  @Field(type = FieldType.Nested)
  private List<Association> inverseAssociations;

  /** The roles. */
  @Field(type = FieldType.Nested)
  private List<Role> roles;

  /** The disjoint with. */
  @Field(type = FieldType.Nested)
  private List<DisjointWith> disjointWith;

  /** The inverse roles. */
  @Field(type = FieldType.Nested)
  private List<Role> inverseRoles;

  /** The maps. */
  @Field(type = FieldType.Nested)
  private List<Map> maps;

  /** The paths to root. */
  @Field(type = FieldType.Nested, ignoreFields = {
      "paths"
  })
  private Paths paths;

  /** The paths to root. */
  @Field(type = FieldType.Nested)
  private Extensions extensions;

  /**
   * Instantiates an empty {@link Concept}.
   */
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
  public Concept(final HierarchyNode other) {
    super(other.getCode());
    setName(other.getLabel());
    level = other.getLevel();
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
    super.populateFrom(other);
    highlight = other.getHighlight();
    highlights = new HashMap<>(other.getHighlights());
    level = other.getLevel();
    leaf = other.getLeaf();
    normName = other.getNormName();
    subsetLink = other.getSubsetLink();
    synonyms = new ArrayList<>(other.getSynonyms());
    definitions = new ArrayList<>(other.getDefinitions());
    properties = new ArrayList<>(other.getProperties());
    children = new ArrayList<>(other.getChildren());
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
  public java.util.Map<String, String> getHighlights() {
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
  public void setHighlights(final java.util.Map<String, String> highlights) {
    this.highlights = highlights;
  }

  /**
   * Returns the normName.
   *
   * @return the normName
   */
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
   * @return the subsetLink
   */
  public String getSubsetLink() {
    return subsetLink;
  }

  /**
   * @param subsetLink the subsetLink to set
   */
  public void setSubsetLink(String subsetLink) {
    this.subsetLink = subsetLink;
  }

  /**
   * @return the conceptStatus
   */
  public String getConceptStatus() {
    return conceptStatus;
  }

  /**
   * @param conceptStatus the conceptStatus to set
   */
  public void setConceptStatus(String conceptStatus) {
    this.conceptStatus = conceptStatus;
  }

  /**
   * Returns the level.
   *
   * @return the level
   */
  public Integer getLevel() {
    return level;
  }

  /**
   * Sets the level.
   *
   * @param level the level
   */
  public void setLevel(final Integer level) {
    this.level = level;
  }

  /**
   * Returns the leaf.
   *
   * @return the leaf
   */
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
   * Returns the synonyms.
   *
   * @return the synonyms
   */
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
   * Returns the properties.
   *
   * @return the properties
   */
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
   * Returns the children.
   *
   * @return the children
   */
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
  public List<Map> getMaps() {
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
  public void setMaps(final List<Map> maps) {
    this.maps = maps;
  }

  /**
   * Returns the paths.
   *
   * @return the paths
   */
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

  /**
   * Sort lists.
   */
  public void sortLists() {

    if (synonyms != null) {
      Collections.sort(synonyms);
    }
    if (definitions != null) {
      Collections.sort(definitions);
    }
    if (properties != null) {
      Collections.sort(properties);
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
   * stream children
   */
  public Stream<Concept> streamSelfAndChildren() {
    return Stream.concat(Stream.of(this),
        getChildren().stream().flatMap(Concept::streamSelfAndChildren));
  }

}
