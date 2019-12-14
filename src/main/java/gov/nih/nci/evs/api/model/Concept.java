
package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;

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
 *   ],
 *   "contributingSources" : [ "CTRP", "CTEP", "Cellosaurus", "MedDRA", "CDISC", "NICHD", "CPTAC" ],
 * }
 * </pre>
 */
public class Concept extends BaseModel {

  /** The code. */
  private String code;

  /** The name. */
  private String name;

  /** The synonyms. */
  private List<Synonym> synonyms;

  /** The definitions. */
  private List<Definition> definitions;

  /** The properties. */
  private List<Property> properties;

  /** The associations. */
  private List<Association> associations;

  /** The inverse associations. */
  private List<Association> inverseAssociations;

  /** The roles. */
  private List<Role> roles;

  /** The inverse roles. */
  private List<Role> inverseRoles;

  /** The maps. */
  private List<Map> maps;

  /**
   * Instantiates an empty {@link Concept}.
   */
  public Concept() {
    // n/a
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
   * Populate from.
   *
   * @param other the other
   */
  public void populateFrom(final Concept other) {
    code = other.getCode();
    name = other.getName();
    synonyms = new ArrayList<>(other.getSynonyms());
    definitions = new ArrayList<>(other.getDefinitions());
    properties = new ArrayList<>(other.getProperties());
    associations = new ArrayList<>(other.getAssociations());
    inverseAssociations = new ArrayList<>(other.getAssociations());
    roles = new ArrayList<>(other.getRoles());
    inverseRoles = new ArrayList<>(other.getRoles());
    maps = new ArrayList<>(other.getMaps());
  }

  /**
   * Returns the code.
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Sets the code.
   *
   * @param code the code
   */
  public void setCode(final String code) {
    this.code = code;
  }

  /**
   * Returns the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Returns the synonyms.
   *
   * @return the synonyms
   */
  public List<Synonym> getSynonyms() {
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
   * Returns the associations.
   *
   * @return the associations
   */
  public List<Association> getAssociations() {
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
    return inverseAssociations;
  }

  /**
   * Sets the inverse associations.
   *
   * @param inverseAssociations the inverse associations
   */
  public void setInverseAssociations(
    final List<Association> inverseAssociations) {
    this.inverseAssociations = inverseAssociations;
  }

  /**
   * Returns the roles.
   *
   * @return the roles
   */
  public List<Role> getRoles() {
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
   * Returns the maps.
   *
   * @return the maps
   */
  public List<Map> getMaps() {
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

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((code == null) ? 0 : code.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Concept other = (Concept) obj;
    if (code == null) {
      if (other.code != null) {
        return false;
      }
    } else if (!code.equals(other.code)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

}
