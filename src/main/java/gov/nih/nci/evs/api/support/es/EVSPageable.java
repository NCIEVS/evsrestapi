
package gov.nih.nci.evs.api.support.es;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Custom pageable implementation to support arbitrary offset (work in progress).
 *
 * @author Arun
 */
public class EVSPageable extends PageRequest {

  /** The offset. */
  private int offset;

  /**
   * Instantiates a {@link EVSPageable} from the specified parameters.
   *
   * @param page the page
   * @param size the size
   * @param offset the offset
   */
  public EVSPageable(int page, int size, int offset) {
    super(page, size, Sort.unsorted());
    this.offset = offset;
  }

  /* see superclass */
  @Override
  public long getOffset() {
    return this.offset;
  }
}
