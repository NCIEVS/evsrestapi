package gov.nih.nci.evs.api.support.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import gov.nih.nci.evs.api.service.ElasticOperationsService;

@Document(indexName = "default_object", type = ElasticOperationsService.OBJECT_TYPE)
public class ElasticObject {
  
  @Id
  private String name;

  @Field(type = FieldType.Keyword)
  private String data;
  
//  @Field(type = FieldType.Object)
//  private HierarchyUtils hierarchy;
//
//  @Field(type = FieldType.Object)
//  private Paths paths;
//  
//  @Field(type = FieldType.Nested)
//  private List<Concept> concepts;
//
//  @Field(type = FieldType.Nested)
//  private List<ConceptMinimal> conceptMinimals;

  public ElasticObject() {
  }
  
  public ElasticObject(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }
  
//  public HierarchyUtils getHierarchy() {
//    return hierarchy;
//  }
//
//  public void setHierarchy(HierarchyUtils hierarchy) {
//    this.hierarchy = hierarchy;
//  }
//
//  public Paths getPaths() {
//    return paths;
//  }
//
//  public void setPaths(Paths paths) {
//    this.paths = paths;
//  }
//
//  public List<Concept> getConcepts() {
//    return concepts;
//  }
//
//  public void setConcepts(List<Concept> concepts) {
//    this.concepts = concepts;
//  }
//
//  public List<ConceptMinimal> getConceptMinimals() {
//    return conceptMinimals;
//  }
//
//  public void setConceptMinimals(List<ConceptMinimal> conceptMinimals) {
//    this.conceptMinimals = conceptMinimals;
//  }
}
