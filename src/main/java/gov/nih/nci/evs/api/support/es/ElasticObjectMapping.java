
package gov.nih.nci.evs.api.support.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import gov.nih.nci.evs.api.model.BaseModel;
import gov.nih.nci.evs.api.service.ElasticOperationsService;

/**
 * Used just for putting a mapping for this object
 */
@Document(indexName = "default_object")
@JsonInclude(content = Include.NON_EMPTY)
public class ElasticObjectMapping extends BaseModel {

  /** The name. */
  @Id
  private String name;

  /**
   * Instantiates an empty {@link ElasticObjectMapping}.
   */
  public ElasticObjectMapping() {
  }

  /**
   * Instantiates a {@link ElasticObjectMapping} from the specified parameters.
   *
   * @param name the name
   */
  public ElasticObjectMapping(String name) {
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

}
