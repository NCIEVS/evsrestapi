package gov.nih.nci.evs.api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import gov.nih.nci.evs.api.service.ElasticOperationsService;

@Document(indexName = "default", type = ElasticOperationsService.CONCEPT_TYPE)
public class ConceptPaths extends Paths {
  @Id
  @Field(type = FieldType.Text)
  private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
