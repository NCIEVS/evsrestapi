package gov.nih.nci.evs.api.support.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import gov.nih.nci.evs.api.service.ElasticOperationsService;

@Document(
    indexName = ElasticOperationsService.METADATA_INDEX, 
    type = ElasticOperationsService.METADATA_TYPE)
@JsonInclude(Include.NON_EMPTY)
public class IndexMetadata {
  /** the index name **/
  @Id
  @Field(type = FieldType.Keyword)
  private String indexName;

  /** the object index name **/
  @Field(type = FieldType.Keyword)
  private String objectIndexName;
  
  /** the terminology version **/
  @Field(type = FieldType.Keyword)
  private String terminologyVersion;

  /** the total concepts to be indexed **/
  @Field(type = FieldType.Integer)
  private int totalConcepts;
  
  /** the status of indexing **/
  @Field(type = FieldType.Boolean)
  private boolean completed;

  /**
   * Get index name
   * 
   * @return the index name
   */
  public String getIndexName() {
    return indexName;
  }

  /**
   * Set the object index name
   * 
   * @param objectIndexName the object index name
   */
  public void setObjectIndexName(String objectIndexName) {
    this.objectIndexName = objectIndexName;
  }

  /**
   * Get object index name
   * 
   * @return the object index name
   */
  public String getObjectIndexName() {
    return objectIndexName;
  }

  /**
   * Set the index name
   * 
   * @param indexName the index name
   */
  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }
  
  /**
   * Get terminology version
   * 
   * @return the terminology version
   */
  public String getTerminologyVersion() {
    return terminologyVersion;
  }

  /**
   * Set the terminology version
   * 
   * @param terminologyVersion the terminology version
   */
  public void setTerminologyVersion(String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /**
   * Get the concepts total
   * 
   * @return the total
   */
  public int getTotalConcepts() {
    return totalConcepts;
  }

  /**
   * Set the concepts total
   * 
   * @param totalConcepts the concepts total
   */
  public void setTotalConcepts(int totalConcepts) {
    this.totalConcepts = totalConcepts;
  }

  /**
   * Get the boolean indicating status of indexing
   * 
   * @return the boolean indicating index completion status
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Set the status of indexing
   * 
   * @param completed the boolean indicating index completion status
   */
  public void setCompleted(boolean completed) {
    this.completed = completed;
  }
}
