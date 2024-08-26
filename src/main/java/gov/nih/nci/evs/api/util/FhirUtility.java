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

import static java.lang.String.format;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

/** Utility for fhir data building. */
public final class FhirUtility {

  /** Instantiates an empty {@link FhirUtility}. */
  private FhirUtility() {
    // n/a
  }

  /**
   * Gets the code.
   *
   * @param code the code
   * @param coding the coding
   * @return the code
   * @throws Exception the exception
   */
  public static String getCode(final CodeType code, final Coding coding) throws Exception {
    if (code != null) {
      return code.getCode();
    }
    if (coding != null) {
      return coding.getCode();
    }

    return null;
  }

  /**
   * Mutually exclusive.
   *
   * @param param1Name the param 1 name
   * @param param1 the param 1
   * @param param2Name the param 2 name
   * @param param2 the param 2
   */
  public static void mutuallyExclusive(
      final String param1Name, final Object param1, final String param2Name, final Object param2) {
    if (param1 != null && param2 != null) {
      throw exception(
          format("Use one of '%s' or '%s' parameters.", param1Name, param2Name),
          OperationOutcome.IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Not supported.
   *
   * @param paramName the param name
   * @param obj the obj
   */
  public static void notSupported(final String paramName, final Object obj) {
    notSupported(paramName, obj, null);
  }

  /**
   * Not supported.
   *
   * @param paramName the param name
   * @param obj the obj
   * @param additionalDetail the additional detail
   */
  public static void notSupported(
      final String paramName, final Object obj, final String additionalDetail) {
    if (obj != null) {
      final String message =
          format(
              "Input parameter '%s' is not supported%s",
              paramName, (additionalDetail == null ? "." : format(" %s", additionalDetail)));
      throw exception(message, OperationOutcome.IssueType.NOTSUPPORTED, 400);
    }
  }

  /**
   * Require exactly one of.
   *
   * @param param1Name the param 1 name
   * @param param1 the param 1
   * @param param2Name the param 2 name
   * @param param2 the param 2
   */
  public static void requireExactlyOneOf(
      final String param1Name, final Object param1, final String param2Name, final Object param2) {
    if (param1 == null && param2 == null) {
      throw exception(
          format("One of '%s' or '%s' parameters must be supplied.", param1Name, param2Name),
          IssueType.INVARIANT,
          400);
    } else {
      mutuallyExclusive(param1Name, param1, param2Name, param2);
    }
  }

  /**
   * Require exactly one of.
   *
   * @param param1Name the param 1 name
   * @param param1 the param 1
   * @param param2Name the param 2 name
   * @param param2 the param 2
   * @param param3Name the param 3 name
   * @param param3 the param 3
   */
  public static void requireExactlyOneOf(
      final String param1Name,
      final Object param1,
      final String param2Name,
      final Object param2,
      final String param3Name,
      final Object param3) {
    if (param1 == null && param2 == null && param3 == null) {
      throw exception(
          format(
              "One of '%s' or '%s' or '%s' parameters must be supplied.",
              param1Name, param2Name, param3Name),
          OperationOutcome.IssueType.INVARIANT,
          400);
    } else {
      mutuallyExclusive(param1Name, param1, param2Name, param2);
      mutuallyExclusive(param1Name, param1, param3Name, param3);
      mutuallyExclusive(param2Name, param2, param3Name, param3);
    }
  }

  /**
   * Mutually required.
   *
   * @param param1Name the param 1 name
   * @param param1 the param 1
   * @param param2Name the param 2 name
   * @param param2 the param 2
   */
  public static void mutuallyRequired(
      final String param1Name, final Object param1, final String param2Name, final Object param2) {
    if (param1 != null && param2 == null) {
      throw exception(
          format(
              "Input parameter '%s' can only be used in conjunction with parameter '%s'.",
              param1Name, param2Name),
          IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Mutually required.
   *
   * @param param1Name the param 1 name
   * @param param1 the param 1
   * @param param2Name the param 2 name
   * @param param2 the param 2
   * @param param3Name the param 3 name
   * @param param3 the param 3
   */
  public static void mutuallyRequired(
      final String param1Name,
      final Object param1,
      final String param2Name,
      final Object param2,
      final String param3Name,
      final Object param3) {
    if (param1 != null && param2 == null && param3 == null) {
      throw exception(
          format(
              "Use of input parameter '%s' only allowed if '%s' or '%s' is also present.",
              param1Name, param2Name, param3Name),
          IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Recover code.
   *
   * @param code the code
   * @param coding the coding
   * @return the string
   */
  public static String recoverCode(final CodeType code, final Coding coding) {
    if (code == null && coding == null) {
      throw exception(
          "Use either 'code' or 'coding' parameters, not both.",
          OperationOutcome.IssueType.INVARIANT,
          400);
    } else if (code != null) {
      if (code.getCode().contains("|")) {
        throw exception(
            "The 'code' parameter cannot supply a codeSystem. "
                + "Use 'coding' or provide CodeSystem in 'system' parameter.",
            OperationOutcome.IssueType.NOTSUPPORTED,
            400);
      }
      return code.getCode();
    }
    return coding.getCode();
  }

  /**
   * Exception not supported.
   *
   * @param message the message
   * @return the FHIR server response exception
   */
  public static FHIRServerResponseException exceptionNotSupported(final String message) {
    return exception(message, OperationOutcome.IssueType.NOTSUPPORTED, 501);
  }

  /**
   * Exception.
   *
   * @param message the message
   * @param issueType the issue type
   * @param theStatusCode the the status code
   * @return the FHIR server response exception
   */
  public static FHIRServerResponseException exception(
      final String message, final OperationOutcome.IssueType issueType, final int theStatusCode) {
    return exception(message, issueType, theStatusCode, null);
  }

  /**
   * Exception.
   *
   * @param message the message
   * @param issueType the issue type
   * @param theStatusCode the the status code
   * @param e the e
   * @return the FHIR server response exception
   */
  public static FHIRServerResponseException exception(
      final String message,
      final OperationOutcome.IssueType issueType,
      final int theStatusCode,
      final Throwable e) {
    final OperationOutcome outcome = new OperationOutcome();
    final OperationOutcome.OperationOutcomeIssueComponent component =
        new OperationOutcome.OperationOutcomeIssueComponent();
    component.setSeverity(OperationOutcome.IssueSeverity.ERROR);
    component.setCode(issueType);
    component.setDiagnostics(message);
    outcome.addIssue(component);
    return new FHIRServerResponseException(theStatusCode, message, outcome, e);
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
