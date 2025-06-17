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
import java.util.HashMap;
import java.util.Map;
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

  public static String convertToYYYYMMDD(String sdate) {
    if (sdate == null || sdate.trim().isEmpty()) {
      return sdate; // Return input unchanged instead of null
    }

    String cleaned = sdate.trim();

    // YYYY_MM_DD pattern (e.g., "2008_12_19")
    if (cleaned.matches("\\d{4}_\\d{1,2}_\\d{1,2}")) {
      String[] parts = cleaned.split("_");
      String year = parts[0];
      String month = String.format("%02d", Integer.parseInt(parts[1]));
      String day = String.format("%02d", Integer.parseInt(parts[2]));
      return year + "-" + month + "-" + day;
    }

    // YYYY pattern (e.g., "2008")
    if (cleaned.matches("\\d{4}")) {
      return cleaned + "-01-01";
    }

    // YYYY_MM pattern (e.g., "2011_02")
    if (cleaned.matches("\\d{4}_\\d{1,2}")) {
      String[] parts = cleaned.split("_");
      String year = parts[0];
      String month = String.format("%02d", Integer.parseInt(parts[1]));
      return year + "-" + month + "-01";
    }

    // YYYYMM pattern (e.g., "202311")
    if (cleaned.matches("\\d{6}")) {
      String year = cleaned.substring(0, 4);
      String month = cleaned.substring(4, 6);
      return year + "-" + month + "-01";
    }

    // YYYYMMDD pattern (e.g., "20231101")
    if (cleaned.matches("\\d{8}")) {
      String year = cleaned.substring(0, 4);
      String month = cleaned.substring(4, 6);
      String day = cleaned.substring(6, 8);
      return year + "-" + month + "-" + day;
    }

    // dd:MM:yyyy pattern (e.g., "14:11:2014")
    if (cleaned.matches("\\d{1,2}:\\d{1,2}:\\d{4}")) {
      String[] parts = cleaned.split(":");
      String day = String.format("%02d", Integer.parseInt(parts[0]));
      String month = String.format("%02d", Integer.parseInt(parts[1]));
      String year = parts[2];
      return year + "-" + month + "-" + day;
    }

    // yyyy.MM.dd pattern (e.g., "2018.02.05")
    if (cleaned.matches("\\d{4}\\.\\d{1,2}\\.\\d{1,2}")) {
      String[] parts = cleaned.split("\\.");
      String year = parts[0];
      String month = String.format("%02d", Integer.parseInt(parts[1]));
      String day = String.format("%02d", Integer.parseInt(parts[2]));
      return year + "-" + month + "-" + day;
    }

    // dd:MM:yyyy HH:mm pattern (e.g., "09:09:2022 07:26") - extract date part only
    if (cleaned.matches("\\d{1,2}:\\d{1,2}:\\d{4}\\s+\\d{1,2}:\\d{1,2}")) {
      String datePart = cleaned.split("\\s+")[0]; // Get the date part before space
      String[] parts = datePart.split(":");
      String day = String.format("%02d", Integer.parseInt(parts[0]));
      String month = String.format("%02d", Integer.parseInt(parts[1]));
      String year = parts[2];
      return year + "-" + month + "-" + day;
    }

    // yyyy-MM-dd pattern (e.g., "2021-02-23") - already in correct format
    if (cleaned.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
      String[] parts = cleaned.split("-");
      String year = parts[0];
      String month = String.format("%02d", Integer.parseInt(parts[1]));
      String day = String.format("%02d", Integer.parseInt(parts[2]));
      return year + "-" + month + "-" + day;
    }

    // MMMM d, yyyy pattern (e.g., "February 9, 2007")
    if (cleaned.matches("(?i)[a-z]+\\s+\\d{1,2},\\s*\\d{4}")) {
      try {
        // Create month name mapping
        Map<String, String> monthMap = new HashMap<>();
        monthMap.put("january", "01");
        monthMap.put("february", "02");
        monthMap.put("march", "03");
        monthMap.put("april", "04");
        monthMap.put("may", "05");
        monthMap.put("june", "06");
        monthMap.put("july", "07");
        monthMap.put("august", "08");
        monthMap.put("september", "09");
        monthMap.put("october", "10");
        monthMap.put("november", "11");
        monthMap.put("december", "12");

        // Parse the parts
        String[] parts = cleaned.toLowerCase().replaceAll(",", "").split("\\s+");
        String monthName = parts[0];
        String day = String.format("%02d", Integer.parseInt(parts[1]));
        String year = parts[2];

        String monthNum = monthMap.get(monthName);
        if (monthNum != null) {
          return year + "-" + monthNum + "-" + day;
        }
      } catch (Exception e) {
        // Fall through to return input unchanged
      }
    }

    // No pattern matched
    return "";
  }
}
