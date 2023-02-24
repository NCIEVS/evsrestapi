
package gov.nih.nci.evs.api.support.es;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.AssociationEntry;
import gov.nih.nci.evs.api.model.BaseModel;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.service.ElasticOperationsService;
import gov.nih.nci.evs.api.util.HierarchyUtils;

/**
 * The elasticsearch wrapper object for cached objects.
 */
@Document(indexName = "default_object", type = ElasticOperationsService.OBJECT_TYPE)
@JsonInclude(content = Include.NON_EMPTY)
public class ElasticObject extends BaseModel {

  /** The name. */
  @Id
  private String name;

  /** The hierarchy. */
  private HierarchyUtils hierarchy;

  /** The paths. */
  private Paths paths;

  /** The concepts. */
  private List<Concept> concepts;

  /** The concept minimals. */
  private List<ConceptMinimal> conceptMinimals;

  /** The association entries. */
  private List<AssociationEntry> associationEntries;

  /** The map. Store this as a string to avoid complicated indexing */
  @Field(type = FieldType.Keyword, index = false)
  private String map;

  /**
   * Instantiates an empty {@link ElasticObject}.
   */
  public ElasticObject() {
    // n/a
  }

  /**
   * Instantiates a {@link ElasticObject} from the specified parameters.
   *
   * @param name the name
   */
  public ElasticObject(String name) {
    this.name = name;
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
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the hierarchy.
   *
   * @return the hierarchy
   */
  public HierarchyUtils getHierarchy() {
    return hierarchy;
  }

  /**
   * Sets the hierarchy.
   *
   * @param hierarchy the hierarchy
   */
  public void setHierarchy(HierarchyUtils hierarchy) {
    this.hierarchy = hierarchy;
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
   * @param paths the paths
   */
  public void setPaths(Paths paths) {
    this.paths = paths;
  }

  /**
   * Returns the concepts.
   *
   * @return the concepts
   */
  public List<Concept> getConcepts() {
    if (concepts == null) {
      concepts = new ArrayList<>();
    }
    return concepts;
  }

  /**
   * Sets the concepts.
   *
   * @param concepts the concepts
   */
  public void setConcepts(List<Concept> concepts) {
    this.concepts = concepts;
  }

  /**
   * Returns the concept minimals.
   *
   * @return the concept minimals
   */
  public List<ConceptMinimal> getConceptMinimals() {
    if (conceptMinimals == null) {
      conceptMinimals = new ArrayList<>();
    }
    return conceptMinimals;
  }

  /**
   * Sets the concept minimals.
   *
   * @param conceptMinimals the concept minimals
   */
  public void setConceptMinimals(List<ConceptMinimal> conceptMinimals) {
    this.conceptMinimals = conceptMinimals;
  }

  /**
   * Returns the association entries.
   *
   * @return the association entries
   */
  public List<AssociationEntry> getAssociationEntries() {
    return associationEntries;
  }

  /**
   * Sets the association entries.
   *
   * @param associationEntries the association entries
   */
  public void setAssociationEntries(List<AssociationEntry> associationEntries) {
    this.associationEntries = associationEntries;
  }

  /**
   * Returns the map.
   *
   * @return the map
   * @throws Exception the exception
   */
  public Map<String, Set<String>> getMap() throws Exception {

    // The X is to trick elasticsearch into avoiding trying to index this like a
    // map
    if (map == null || !map.startsWith("X")) {
      return new HashMap<>();
    }
    // Turn back into a map
    return new ObjectMapper().readValue(map.substring(1),
        new TypeReference<Map<String, Set<String>>>() {
          // n/a
        });
  }

  /**
   * Sets the map.
   *
   * @param map the map
   * @throws Exception the exception
   */
  public void setMap(Map<String, Set<String>> map) throws Exception {
    if (map == null) {
      this.map = null;
    } else {
      // The X is to trick elasticsearch into avoiding trying to index this like
      // a map
      this.map = new ObjectMapper().writeValueAsString("X" + map);
    }
  }

}
