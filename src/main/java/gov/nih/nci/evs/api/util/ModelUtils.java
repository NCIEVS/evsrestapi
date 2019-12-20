
package gov.nih.nci.evs.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for interacting with model objects.
 */
public final class ModelUtils {

  /** The Constant logger. */
  @SuppressWarnings("unused")
  private static final Logger logger =
      LoggerFactory.getLogger(ModelUtils.class);

  /**
   * Instantiates an empty {@link ModelUtils}.
   */
  private ModelUtils() {
    // n/a
  }

  /**
   * Indicates whether or not code style is the case.
   *
   * @param code the code
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public static boolean isCodeStyle(final String code) {
    return code.matches("\\d+") || code.matches("[A-Z]\\d+");
  }
}
