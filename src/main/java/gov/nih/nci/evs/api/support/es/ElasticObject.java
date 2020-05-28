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
  
  @Field(type = FieldType.Object)
  private Object object;

  public ElasticObject(String name, Object object) {
    this.name = name;
    this.object = object;
  }

  //TODO: comments
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public Object getObject() {
    return object;
  }

  public void setObject(Object object) {
    this.object = object;
  }
}
