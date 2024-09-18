/*
 * Copyright 2021 West Coast Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of West Coast Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * West Coast Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package gov.nih.nci.evs.api.util;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;


/** Utility for fhir data building. */
public final class FhirUtility {

  /** Instantiates an empty {@link FhirUtility}. */
  private FhirUtility() {
    // n/a
  }

  /**
   * Gets the type name.
   *
   * @param obj the obj
   * @return the type name
   */
  @SuppressWarnings("unused")
  private static String getTypeName(final Object obj) {
    if (obj instanceof String) {
      return "valueString";
    } else if (obj instanceof Boolean) {
      return "valueBoolean";
    }
    return null;
  }

  /**
   * Compare string.
   *
   * @param s1 the s 1
   * @param s2 the s 2
   * @return true, if successful
   */
  public static boolean compareString(final StringParam s1, final String s2) {
    // If we've not specified a search term, then we pass through a match
    if (s1 == null || StringUtils.isEmpty(s1.getValue())) {
      return true;
    }

    // If we've specified a search term but the target element is not populated, that's not a
    // match
    if (s2 == null) {
      return false;
    }

    // What sort of matching are we doing? StartsWith by default
    if (s1.isExact()) {
      return s2.equalsIgnoreCase(s1.getValue());
    } else if (s1.isContains()) {
      return s2.toLowerCase().contains(s1.getValue().toLowerCase());
    } else {
      return s2.toLowerCase().startsWith(s1.getValue().toLowerCase());
    }
  }

  /**
   * Compare date.
   *
   * @param d1 the d 1
   * @param d2 the d 2
   * @return true, if successful
   */
  public static boolean compareDate(final DateParam d1, final Date d2) {

    // If we've not specified a search term, then we pass through a match
    if (d1 == null) {
      return true;
    }

    // If we've specified a search term but the target element is not populated, that's not a
    // match
    if (d2 == null) {
      return false;
    }

    // NO prefix is equals
    if (d1.getPrefix() == null) {
      return d1.getValue().equals(d2);
    }
    switch (d1.getPrefix()) {
      case APPROXIMATE:
        {
          return d1.getValue().equals(d2);
        }
      case ENDS_BEFORE:
        {
          // doesn't really make sense for a single date
          return d2.compareTo(d1.getValue()) < 0;
        }
      case EQUAL:
        {
          return d1.getValue().equals(d2);
        }
      case GREATERTHAN:
        {
          return d2.compareTo(d1.getValue()) > 0;
        }
      case GREATERTHAN_OR_EQUALS:
        {
          return d2.compareTo(d1.getValue()) >= 0;
        }
      case LESSTHAN:
        {
          return d2.compareTo(d1.getValue()) < 0;
        }
      case LESSTHAN_OR_EQUALS:
        {
          return d2.compareTo(d1.getValue()) <= 0;
        }
      case NOT_EQUAL:
        {
          return !d1.getValue().equals(d2);
        }
      case STARTS_AFTER:
        {
          // doesn't really make sense for a single date
          return d2.compareTo(d1.getValue()) > 0;
        }
      default:
        break;
    }
    return false;
  }

  /**
   * Compare date range.
   *
   * @param d1 the d 1
   * @param d2 the d 2
   * @return true, if successful
   */
  public static boolean compareDateRange(final DateRangeParam d1, final Date d2) {

    // If we've not specified a search term, then we pass through a match
    if (d1 == null) {
      return true;
    }

    // If we've specified a search term but the target element is not populated, that's not a
    // match
    if (d2 == null) {
      return false;
    }

    // Check that date is in range
    return compareDate(d1.getLowerBound(), d2) && compareDate(d1.getUpperBound(), d2);
  }
}
