package gov.nih.nci.evs.api.fhir.R5;

import static java.lang.String.format;

import ca.uhn.fhir.rest.param.NumberParam;
import gov.nih.nci.evs.api.controller.StaticContextAccessor;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.Bundle.LinkRelationTypes;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Identifier;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility for FHIR R4. */
public class FhirUtilityR5 {
  /** The logger. */
  @SuppressWarnings("unused")
  private static Logger logger = LoggerFactory.getLogger(FhirUtilityR5.class);

  /** The publishers. */
  private static HashMap<String, String> publishers;

  /** The uris. */
  private static HashMap<String, String> uris;

  /** The unsupported params list for search. */
  private static final String[] unsupportedParams =
      new String[] {
        "_lastUpdated",
        "_tag",
        "_profile",
        "_security",
        "_text",
        "_list",
        "_type",
        "_include",
        "_revinclude",
        "_summary",
        "_total",
        "_elements",
        "_contained",
        "_containedType"
      };

  /** Instantiates an empty {@link FhirUtilityR5}. */
  private FhirUtilityR5() {
    // n/a
  }

  /**
   * Generate uris from the terminology metadata.
   *
   * @return the hash map
   */
  private static HashMap<String, String> generateUris() {
    final HashMap<String, String> uri = new HashMap<>();
    List<Terminology> terms;
    try {
      ElasticQueryService esQueryService = StaticContextAccessor.getBean(ElasticQueryService.class);
      TerminologyUtils termUtils = StaticContextAccessor.getBean(TerminologyUtils.class);

      terms = termUtils.getIndexedTerminologies(esQueryService);

      terms.forEach(
          terminology -> {
            uri.put(terminology.getTerminology(), terminology.getMetadata().getFhirUri());
          });
    } catch (Exception e) {
      return uri;
    }

    return uri;
  }

  /**
   * Generate publishers from the terminology metadata.
   *
   * @return the hash map
   */
  private static HashMap<String, String> generatePublishers() {
    final HashMap<String, String> publish = new HashMap<>();
    List<Terminology> terms;
    try {
      ElasticQueryService esQueryService = StaticContextAccessor.getBean(ElasticQueryService.class);
      TerminologyUtils termUtils = StaticContextAccessor.getBean(TerminologyUtils.class);

      terms = termUtils.getIndexedTerminologies(esQueryService);

      terms.forEach(
          terminology -> {
            publish.put(terminology.getTerminology(), terminology.getMetadata().getFhirPublisher());
          });
    } catch (Exception e) {
      return publish;
    }

    return publish;
  }

  /**
   * Gets the publisher.
   *
   * @param terminology the terminology
   * @return the publisher for a given terminology
   */
  private static String getPublisher(final String terminology) {
    if (publishers == null) {
      publishers = generatePublishers();
    }
    return publishers.get(terminology);
  }

  /**
   * Gets the uri.
   *
   * @param terminology the terminology
   * @return the uri for a given terminology
   */
  private static String getUri(final String terminology) {
    if (uris == null) {
      uris = generateUris();
    }
    return uris.get(terminology.toLowerCase());
  }

  /**
   * Gets the code.
   *
   * @param code the primitive fhir code type, not bound to an enumerated list
   * @param coding the reference to a code defined by a terminology system
   * @return the code
   * @throws Exception exception
   */
  public static String getCode(final CodeType code, final Coding coding) throws Exception {
    // if the code has a value, we want to return the code value only
    if (code != null) {
      return code.getValue();
    }
    // if the code is null, but the coding isn't, return the coding code
    if (coding != null) {
      return coding.getCode();
    }
    // Else return null
    return null;
  }

  /**
   * Convert a Terminology to r5 code system.
   *
   * @param term the terminology
   * @return the created code system
   */
  public static CodeSystem toR5(final Terminology term) {
    final CodeSystem cs = new CodeSystem();
    // populate the code system
    // This ensures our id values set internally are always lowercase.
    cs.setId((term.getTerminologyVersion()).toLowerCase());
    cs.setName(term.getName());
    cs.setTitle(term.getTerminology());
    cs.setExperimental(false);
    cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    cs.setHierarchyMeaning(CodeSystem.CodeSystemHierarchyMeaning.ISA);
    cs.setVersion(term.getVersion());
    cs.setPublisher(getPublisher(term.getTerminology()));
    cs.setUrl(getUri(term.getTerminology()));
    return cs;
  }

  /**
   * Convert a Concept to r5 concept map. since setXXXXVersion doesn't doesn't exist
   *
   * @param mapset the concept mapset
   * @return the concept map
   */
  public static ConceptMap toR5(final Concept mapset) {
    final ConceptMap cm = new ConceptMap();
    // populate the r5 concept map
    // This ensures our id values set internally are always lowercase.
    cm.setId((mapset.getCode() + "_" + mapset.getVersion()).toLowerCase());
    cm.setName(mapset.getName());
    cm.setTitle(mapset.getCode());
    cm.setExperimental(false);
    cm.setStatus(Enumerations.PublicationStatus.ACTIVE);
    cm.setVersion(mapset.getVersion());
    cm.setPublisher(
        getPublisher(
            Objects.requireNonNull(
                    mapset.getProperties().stream()
                        .filter(m -> m.getType().equals("sourceTerminology"))
                        .findFirst()
                        .orElse(null))
                .getValue()));
    cm.setSourceScope(
        new UriType(
            getUri(
                    mapset.getProperties().stream()
                        .filter(m -> m.getType().equals("sourceTerminology"))
                        .findFirst()
                        .get()
                        .getValue())
                + "?fhir_vs"));
    cm.setTargetScope(
        new UriType(
            getUri(
                    mapset.getProperties().stream()
                        .filter(m -> m.getType().equals("targetTerminology"))
                        .findFirst()
                        .get()
                        .getValue())
                + "?fhir_vs"));
    cm.setUrl(
        getUri(
                mapset.getProperties().stream()
                    .filter(m -> m.getType().equals("sourceTerminology"))
                    .findFirst()
                    .get()
                    .getValue())
            + "?fhir_cm="
            + mapset.getCode());
    return cm;
  }

  /**
   * Convert a Terminology to r5 value set.
   *
   * @param term the Terminology to convert
   * @return the value set
   */
  public static ValueSet toR5VS(final Terminology term) {
    final ValueSet vs = new ValueSet();
    // This ensures our id values set internally are always lowercase.
    vs.setId((term.getTerminology() + "_" + term.getVersion()).toLowerCase());
    vs.setName(term.getName());
    vs.setVersion(term.getVersion());
    vs.setTitle(term.getTerminology());
    vs.setUrl(getUri(term.getTerminology()) + "?fhir_vs");
    vs.setPublisher(getPublisher(term.getTerminology()));
    vs.setExperimental(false);
    vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    vs.setDescription(term.getDescription());
    return vs;
  }

  /**
   * Convert a Concept subset to r5 value set.
   *
   * @param subset the Concept subset to convert
   * @return the value set
   */
  public static ValueSet toR5VS(final Concept subset) {
    final ValueSet vs = new ValueSet();
    // This ensures our id values set internally are always lowercase.
    vs.setId((subset.getTerminology() + "_" + subset.getCode()).toLowerCase());
    vs.setUrl(getUri(subset.getTerminology()) + "?fhir_vs=" + subset.getCode());
    vs.setName(subset.getName());
    vs.setVersion(subset.getVersion());
    vs.setTitle(subset.getTerminology());
    vs.setPublisher(getPublisher(subset.getTerminology()));
    vs.setDescription(
        "Value set representing the " + subset.getTerminology() + "subset" + subset.getCode());
    vs.addIdentifier(new Identifier().setValue(subset.getCode()));
    vs.setExperimental(false);
    vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    return vs;
  }

  /**
   * Check if the required object is valid.
   *
   * @param obj the required object
   * @param objName the string name of the object
   */
  public static void required(final Object obj, final String objName) {
    if (obj == null) {
      throw exception(format("Must use '%s' parameter.", objName), IssueType.INVARIANT, 400);
    }
  }

  /**
   * Check if the object is mutually exclusive.
   *
   * @param obj1 first object
   * @param obj1Name first object string name
   * @param obj2 second object
   * @param obj2Name second object string name
   */
  public static void mutuallyExclusive(
      final Object obj1, final String obj1Name, final Object obj2, final String obj2Name) {
    if (obj1 != null && obj2 != null) {
      throw exception(
          format("Must use one of '%s' or '%s' parameters", obj1Name, obj2Name),
          IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Check if the object is not supported.
   *
   * @param obj the object
   * @param objName the string name of the object
   */
  public static void notSupported(final Object obj, final String objName) {
    notSupported(obj, objName, null);
  }

  /**
   * Check if the object is not supported with additional detail param.
   *
   * @param obj the object
   * @param objName the string name of the object
   * @param additionalDetail additional information and details
   */
  public static void notSupported(
      final Object obj, final String objName, final String additionalDetail) {
    if (obj != null) {
      final String message =
          format(
              "Input parameter '%s' is not supported '%s'",
              objName, (additionalDetail == null ? "." : format(" '%s'", additionalDetail)));
      throw exception(message, IssueType.NOTSUPPORTED, 400);
    }
  }

  /**
   * Check if the request param is not supported.
   *
   * @param request the request
   * @param paramName the parameter name
   */
  public static void notSupported(final HttpServletRequest request, final String paramName) {
    if (request.getParameterMap().containsKey(paramName)) {
      final String message = format("Input parameter '%s' is not supported.", paramName);
      throw exception(message, IssueType.NOTSUPPORTED, 400);
    }
  }

  /**
   * Not supported search params.
   *
   * @param request the request
   */
  public static void notSupportedSearchParams(final HttpServletRequest request) {
    for (final String param : unsupportedParams) {
      notSupported(request, param);
    }
    if (Collections.list(request.getParameterNames()).stream()
            .filter(k -> k.startsWith("_has"))
            .count()
        > 0) {
      notSupported(request, "_has");
    }
  }

  /**
   * Check we have at least one required object fopr 2 objects.
   *
   * @param obj1 the first object
   * @param obj1Name the first object name
   * @param obj2 the second object
   * @param obj2Name the second object name
   */
  public static void requireExactlyOneOf(
      final Object obj1, final String obj1Name, final Object obj2, final String obj2Name) {
    if (obj1 == null && obj2 == null) {
      throw exception(
          format("Must supply one of '%s' or '%s' parameters.", obj1Name, obj2Name),
          IssueType.INVARIANT,
          400);
    } else {
      mutuallyExclusive(obj1, obj1Name, obj2, obj2Name);
    }
  }

  /**
   * Check we have exactly one required object for 3 objects.
   *
   * @param obj1 first object
   * @param obj1Name first object name
   * @param obj2 second object
   * @param obj2Name second object name
   * @param obj3 third object
   * @param obj3Name third object name
   */
  public static void requireExactlyOneOf(
      final Object obj1,
      final String obj1Name,
      final Object obj2,
      final String obj2Name,
      final Object obj3,
      final String obj3Name) {
    if (obj1 == null && obj2 == null && obj3 == null) {
      throw exception(
          format(
              "Must supply at least one of '%s', '%s', or '%s' parameters.",
              obj1Name, obj2Name, obj3Name),
          IssueType.INVARIANT,
          400);
    } else {
      mutuallyExclusive(obj1, obj1Name, obj2, obj2Name);
      mutuallyExclusive(obj1, obj1Name, obj3, obj3Name);
      mutuallyExclusive(obj2, obj2Name, obj3, obj3Name);
    }
  }

  /**
   * Check we have at least one required object for 4 objects.
   *
   * @param obj1 first object
   * @param obj1Name first object name
   * @param obj2 second object
   * @param obj2Name second object name
   * @param obj3 third object
   * @param obj3Name third object name
   * @param obj4 fourth object
   * @param obj4Name fourth object name
   */
  public static void requireAtLeastOneOf(
      final Object obj1,
      final String obj1Name,
      final Object obj2,
      final String obj2Name,
      final Object obj3,
      final String obj3Name,
      final Object obj4,
      final String obj4Name) {
    if (obj1 == null && obj2 == null && obj3 == null && obj4 == null) {
      throw exception(
          format(
              "Must supply at least one of '%s', '%s', '%s', or '%s' parameters.",
              obj1Name, obj2Name, obj3Name, obj4Name),
          IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Check we have both required fields for 2 objects.
   *
   * @param obj1 first object
   * @param obj1Name first object name
   * @param obj2 second object
   * @param obj2Name second object name
   */
  public static void mutuallyRequired(
      final Object obj1, final String obj1Name, final Object obj2, final String obj2Name) {
    if (obj1 != null && obj2 == null) {
      throw exception(
          format(
              "Input parameter '%s' can only be used in conjunction with parameter '%s'.",
              obj1Name, obj2Name),
          IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Check we have all required fields for 3 objects.
   *
   * @param obj1 first object
   * @param obj1Name first object name
   * @param obj2 second object
   * @param obj2Name second object name
   * @param obj3 third object
   * @param obj3Name third object name
   */
  public static void mutuallyRequired(
      final Object obj1,
      final String obj1Name,
      final Object obj2,
      final String obj2Name,
      final Object obj3,
      final String obj3Name) {
    if (obj1 != null && obj2 == null && obj3 == null) {
      throw exception(
          format(
              "Use of input parameter '%s' only allowed if '%s' or '%s' is also present.",
              obj1Name, obj2Name, obj3Name),
          IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Get the recover code for a CodeType or Coding System.
   *
   * @param code CodeType code
   * @param coding Coding Type
   * @return a string code value
   */
  public static String recoverCode(final CodeType code, final Coding coding) {
    if (code == null && coding == null) {
      throw exception("User either code or coding parameters, not both", IssueType.INVARIANT, 400);
    } else if (code != null) {
      if (code.getCode().contains("|")) {
        throw exception(
            "The code parameter cannot supply a codeSystem. Use coding or provide CodeSystem in"
                + " system parameter.",
            IssueType.NOTSUPPORTED,
            400);
      }
      return code.getCode();
    }
    return coding.getCode();
  }

  /**
   * Method for reporting unsupported exception.
   *
   * @param message the message
   * @return the fhir server response exception
   */
  public static FHIRServerResponseException exceptionNotSupported(final String message) {
    return exception(message, IssueType.NOTSUPPORTED, 501);
  }

  /**
   * Exception.
   *
   * @param message the message
   * @param issueType the issue type
   * @param theStatusCode the status code
   * @return the FHIR server response exception
   */
  public static FHIRServerResponseException exception(
      final String message, final IssueType issueType, final int theStatusCode) {
    return exception(message, issueType, theStatusCode, null);
  }

  /**
   * Building FHIR server response exception.
   *
   * @param message the error message
   * @param issueType the issue type
   * @param theStatusCode the status code
   * @param error the error
   * @return the FHIR server response exception
   */
  public static FHIRServerResponseException exception(
      final String message,
      final IssueType issueType,
      final int theStatusCode,
      final Throwable error) {
    final OperationOutcome outcome = new OperationOutcome();
    final OperationOutcomeIssueComponent component = new OperationOutcomeIssueComponent();

    component.setSeverity(IssueSeverity.ERROR);
    component.setCode(issueType);
    component.setDiagnostics(message);
    outcome.addIssue(component);
    return new FHIRServerResponseException(theStatusCode, message, outcome, error);
  }

  /**
   * Create a property for a parameter.
   *
   * @param propertyValue the property value
   * @param propertyName the property name
   * @param isCode is the property a code
   * @return the parameter property component
   */
  public static ParametersParameterComponent createProperty(
      final Object propertyValue, final String propertyName, final boolean isCode) {
    final String valueName = "value";
    // Create a property and add the code as a valueCode
    final ParametersParameterComponent property =
        new ParametersParameterComponent().setName("property");
    property.addPart().setName("code").setValue(new CodeType(propertyName));

    // Define the value based on the property value, if present
    final String propertyValueString = propertyValue == null ? "" : propertyValue.toString();
    // if isCode, set code type, if not set other types, else use a string
    if (isCode) {
      property.addPart().setName(valueName).setValue(new CodeType(propertyValueString));
    } else if (propertyValue instanceof Coding) {
      property.addPart().setName(valueName).setName(valueName).setValue((Coding) propertyValue);
    } else if (propertyValue instanceof Boolean) {
      property.addPart().setName(valueName).setValue(new BooleanType((Boolean) propertyValue));
    } else if (propertyValue instanceof Date) {
      property.addPart().setName(valueName).setValue(new DateTimeType((Date) propertyValue));
    } else {
      final StringType value = new StringType(propertyValueString);
      property.addPart().setName(valueName).setValue(value);
    }
    return property;
  }

  /**
   * Get the next link component.
   *
   * @param uri the uri
   * @param offset the offset
   * @param offsetInt the offset int
   * @param count the count
   * @param countInt the count int
   * @return the next link component
   */
  public static BundleLinkComponent getNextLink(
      final String uri,
      final NumberParam offset,
      final int offsetInt,
      final NumberParam count,
      final int countInt) {
    final int nextOffset = offsetInt + countInt;
    String nextUri = uri;
    // append ? to url if missing
    if (!uri.contains("?")) {
      nextUri = nextUri + "?";
    }
    // replace offset if it exists
    if (offset != null) {
      nextUri = nextUri.replaceFirst("offset=\\d+", "_offset=" + nextOffset);
    } else {
      nextUri += (nextUri.endsWith("?") ? "" : "&") + "_offset=" + nextOffset;
    }
    // replace count if it exists
    if (count != null) {
      nextUri = nextUri.replaceFirst("_count=\\d+", "_count=" + countInt);
    } else {
      nextUri += (nextUri.endsWith("?") ? "" : "&") + "_count=" + countInt;
    }

    return new BundleLinkComponent().setUrl(nextUri).setRelation(LinkRelationTypes.NEXT);
  }

  /**
   * Get the previous link component.
   *
   * @param uri the uri
   * @param offset the offset
   * @param offsetInt the offset int
   * @param count the count
   * @param countInt the count int
   * @return the previous link component
   */
  public static BundleLinkComponent getPrevLink(
      final String uri,
      final NumberParam offset,
      final int offsetInt,
      final NumberParam count,
      final int countInt) {
    final int prevOffset = offsetInt - countInt;
    String prevUri = uri;
    // append ? to url if missing
    if (!uri.contains("?")) {
      prevUri = prevUri + "?";
    }
    // replace offset if it exists
    if (offset != null) {
      prevUri = prevUri.replaceFirst("_offset=\\d+", "_offset=" + prevOffset);
    } else {
      prevUri += (prevUri.endsWith("?") ? "" : "&") + "_offset=" + prevOffset;
    }
    // replace count if it exists
    if (count != null) {
      prevUri = prevUri.replaceFirst("_count=\\d+", "_count=" + countInt);
    } else {
      prevUri += (prevUri.endsWith("?") ? "" : "&") + "_count=" + countInt;
    }
    return new BundleLinkComponent().setUrl(prevUri).setRelation(LinkRelationTypes.PREVIOUS);
  }

  /**
   * Make a bundle.
   *
   * @param request the request
   * @param list the list
   * @param count the count
   * @param offset the offset
   * @return the bundle
   */
  public static Bundle makeBundle(
      final HttpServletRequest request,
      final List<? extends Resource> list,
      final NumberParam count,
      final NumberParam offset) {
    final int countInt = count == null ? 100 : count.getValue().intValue();
    final int offsetInt = offset == null ? 0 : offset.getValue().intValue();
    final String thisUrl =
        request.getQueryString() == null
            ? request.getRequestURL().toString()
            : request.getRequestURL().append('?').append(request.getQueryString()).toString();

    final Bundle bundle = new Bundle();
    bundle.setId(UUID.randomUUID().toString());
    bundle.setType(BundleType.SEARCHSET);
    bundle.setTotal(list.size());
    // Commenting out as causing duplication of the link relation, relation is set automatically
    // bundle.addLink(new
    // BundleLinkComponent().setUrl(thisUrl).setRelation(LinkRelationTypes.SELF));

    if (offsetInt + countInt < list.size()) {
      bundle.addLink(FhirUtilityR5.getNextLink(thisUrl, offset, offsetInt, count, countInt));
    }
    if (offsetInt - countInt >= list.size()) {
      bundle.addLink(FhirUtilityR5.getPrevLink(thisUrl, offset, offsetInt, count, countInt));
    }
    for (int i = offsetInt; i < offsetInt + countInt; i++) {
      if (i > list.size() - 1) {
        break;
      }
      final BundleEntryComponent component = new BundleEntryComponent();
      component.setResource(list.get(i));
      component.setFullUrl(request.getRequestURL() + "/" + list.get(i).getId());
      bundle.addEntry(component);
    }
    return bundle;
  }
}
