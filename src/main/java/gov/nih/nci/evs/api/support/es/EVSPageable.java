package gov.nih.nci.evs.api.support.es;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Custom pageable implementation to support arbitrary offset (work in progress)
 * 
 * @author Arun
 *
 */
@SuppressWarnings("serial")
public class EVSPageable extends PageRequest {
  private int offset;
  public EVSPageable(int page, int size, int offset){
    super(page, size, Sort.unsorted());
    this.offset=offset;
  }
  
  @Override
  public long getOffset(){
    return this.offset;
  }
}
