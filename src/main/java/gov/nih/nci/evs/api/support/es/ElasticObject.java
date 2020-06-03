package gov.nih.nci.evs.api.support.es;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import gov.nih.nci.evs.api.service.ElasticOperationsService;

@Document(indexName = "default_object", type = ElasticOperationsService.OBJECT_TYPE)
public class ElasticObject<T> {
  
  @Id
  private String name;

  @Field(type = FieldType.Object)
  private T object;
  
  @Field(type = FieldType.Nested)
  private List<T> objects;

  public ElasticObject(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public T getObject() {
    return object;
  }

  public void setObject(T object) {
    this.object = object;
  }
  
  public List<T> getObjects() {
    return objects;
  }

  public void setObjects(List<T> objects) {
    this.objects = objects;
  }
}
