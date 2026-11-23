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

  /**
   * Returns the page of {@code pageSize} records starting at {@code fromRecord}, which need not be
   * a multiple of {@code pageSize}: OpenSearch starts the page at {@link #getOffset()}, not at the
   * page number times the size.
   *
   * @param fromRecord the index of the first record on the page
   * @param pageSize the number of records on the page
   * @return the page
   */
  public static EVSPageable fromRecord(final int fromRecord, final int pageSize) {
    return new EVSPageable(fromRecord / pageSize, pageSize, fromRecord);
  }

  /* see superclass */
  @Override
  public long getOffset() {
    return this.offset;
  }
}
