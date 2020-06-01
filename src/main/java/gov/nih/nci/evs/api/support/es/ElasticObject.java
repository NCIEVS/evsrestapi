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
  
  @Field(type = FieldType.Byte)
  private byte[] data;

  public ElasticObject(String name, byte[] data) {
    this.name = name;
    this.data = data;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }
}
