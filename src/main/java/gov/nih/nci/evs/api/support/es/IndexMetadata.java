
package gov.nih.nci.evs.api.support.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticOperationsService;

/**
 * The Class IndexMetadata.
 */
@Document(indexName = ElasticOperationsService.METADATA_INDEX)
@JsonInclude(Include.NON_EMPTY)
public class IndexMetadata {

  /** the index name *. */
  @Id
  @Field(type = FieldType.Keyword)
  private String indexName;

  /** the total concepts to be indexed *. */
  @Field(type = FieldType.Integer)
  private int totalConcepts;

  /** the status of indexing *. */
  @Field(type = FieldType.Boolean)
  private boolean completed;

  /** The terminology. */
  @Field(type = FieldType.Object)
  private Terminology terminology;

  /**
   * Get index name.
   *
   * @return the index name
   */
  public String getIndexName() {
    return indexName;
  }

  /**
   * Set the index name.
   *
   * @param indexName the index name
   */
  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  /**
   * Get the concepts total.
   *
   * @return the total
   */
  public int getTotalConcepts() {
    return totalConcepts;
  }

  /**
   * Set the concepts total.
   *
   * @param totalConcepts the concepts total
   */
  public void setTotalConcepts(int totalConcepts) {
    this.totalConcepts = totalConcepts;
  }

  /**
   * Get the boolean indicating status of indexing.
   *
   * @return the boolean indicating index completion status
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Set the status of indexing.
   *
   * @param completed the boolean indicating index completion status
   */
  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  /**
   * Get the terminology.
   *
   * @return the terminology
   */
  public Terminology getTerminology() {
    return terminology;
  }

  /**
   * Set the terminology.
   *
   * @param terminology the terminology
   */
  public void setTerminology(Terminology terminology) {
    this.terminology = terminology;
  }

  /**
   * Get object index name.
   *
   * @return the object index name
   */
  @JsonIgnore
  public String getObjectIndexName() {
    if (terminology == null)
      return null;
    return terminology.getObjectIndexName();
  }

  /**
   * Get terminology version.
   *
   * @return the terminology version
   */
  @JsonIgnore
  public String getTerminologyVersion() {
    if (terminology == null)
      return null;
    return terminology.getTerminologyVersion();
  }

  /* see superclass */
  @Override
  public String toString() {
    return String.format(
        "{indexName: %s, terminologyVersion: %s, totalConcepts: %d, completed: %s}", indexName,
        getTerminologyVersion(), totalConcepts, String.valueOf(completed));
  }
}
