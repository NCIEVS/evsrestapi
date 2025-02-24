package gov.nih.nci.evs.api.fhir.R4;

import static java.lang.String.format;

import ca.uhn.fhir.rest.param.NumberParam;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility for FHIR R4. */
public final class FhirUtilityR4 {

  /** The logger. */
  @SuppressWarnings("unused")
  private static Logger logger = LoggerFactory.getLogger(FhirUtilityR4.class);

  /** The publishers. */
  private static HashMap<String, String> publishers = generatePublishers();

  /** The uris. */
  private static HashMap<String, String> uris = generateUris();

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

  /** Instantiates an empty {@link FhirUtilityR4}. */
  private FhirUtilityR4() {
    // n/a
  }

  /**
   * Generate publishers.
   *
   * @return the hash map
   */
  private static HashMap<String, String> generatePublishers() {
    final HashMap<String, String> publish = new HashMap<>();
    publish.put(
        "mdr",
        "MedDRA Maintenance and Support Services Organization (MedDRA MSSO); Mr. Patrick Revelle;"
            + " MSSO Director");
    publish.put("umlssemnet", "National Library of Medicine");
    publish.put("go", "GO Consortium");
    publish.put("icd10cm", "NCHS");
    publish.put("icd10", "World Health Organization");
    publish.put("hgnc", "HUGO Gene Nomenclature Committee");
    publish.put("duo", "Data Use Ontology");
    publish.put("obi", "Ontology for Biomedical Investigations");
    publish.put("obib", "Ontology for Biobanking");
    publish.put("ndfrt", "Veterans Health Administration");
    publish.put("snomedct_us", "National Library of Medicine");
    publish.put("ctcae5", "NCI");
    publish.put("lnc", "LOINC and Health Data Standards, Regenstrief Institute, Inc.");
    publish.put("ncit", "NCI");
    publish.put("icd9cm", "NCHS");
    publish.put("radlex", "RSNA (Radiological Society of North America)");
    publish.put("canmed", "National Cancer Institute Enterprise Vocabulary Services");
    publish.put("medrt", "National Library of Medicine");
    publish.put("chebi", "Open Biomedical Ontologies - European Bioinformatics Institute");
    publish.put("ncim", "National Cancer Institute Enterprise Vocabulary Services");
    publish.put("pdq", "National Cancer Institute");
    publish.put("ma", "The Jackson Laboratory");
    publish.put("hl7v30", "Health Level Seven International");
    publish.put("mged", "National Cancer Institute");
    publish.put("npo", "National Cancer Institute");
    publish.put("ma", "National Cancer Institute");
    publish.put("zfa", "National Cancer Institute");

    return publish;
  }

  /**
   * Generate uris.
   *
   * @return the hash map
   */
  private static HashMap<String, String> generateUris() {
    final HashMap<String, String> uri = new HashMap<>();
    uri.put("mdr", "https://www.meddra.org");
    uri.put("umlssemnet", "http://www.nlm.nih.gov/research/umls/umlssemnet.owl");
    uri.put("go", "http://purl.obolibrary.org/obo/go.owl");
    uri.put("icd10", "http://hl7.org/fhir/sid/icd-10");
    uri.put("icd10cm", "http://hl7.org/fhir/sid/icd-10-cm");
    uri.put("hgnc", "http://www.genenames.org");
    uri.put("duo", "https://obofoundry.org/ontology/duo.html");
    uri.put("obi", "https://obi-ontology.org/");
    uri.put("obib", "https://obofoundry.org/ontology/obib.html");
    uri.put("ndfrt", "https://bioportal.bioontology.org/ontologies/NDF-RT");
    uri.put("snomedct_us", "http://snomed.info/sct");
    uri.put("ctcae5", "http://hl7.org/fhir/us/ctcae");
    uri.put("lnc", "http://loinc.org");
    uri.put("ncit", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl");
    uri.put("icd9cm", "http://terminology.hl7.org/CodeSystem/icd9cm");
    uri.put("radlex", "http://radlex.org/");
    uri.put("canmed", "http://seer.nci.nih.gov/CanMED.owl");
    uri.put("medrt", "http://va.gov/terminology/medrt");
    uri.put("chebi", "http://www.ebi.ac.uk/chebi/");
    uri.put("ncim", "https://ncim.nci.nih.gov/ncimbrowser/");
    uri.put("pdq", "https://www.cancer.gov/publications/pdq");
    uri.put(
        "hl7v30",
        "https://www.nlm.nih.gov/research/umls/sourcereleasedocs/current/HL7V3.0/index.html");
    uri.put("mged", "http://mged.sourceforge.net/ontologies/MGEDOntology.owl");
    uri.put("npo", "http://purl.bioontology.org/ontology/npo");
    uri.put("ma", "http://purl.obolibrary.org/obo/emap.owl");
    uri.put("zfa", "http://purl.obolibrary.org/obo/zfa.owl");
    // Put reverse entries isnomedn
    for (final Map.Entry<String, String> entry : new HashSet<>(uri.entrySet())) {
      uri.put(entry.getValue(), entry.getKey());
    }
    return uri;
  }

  /**
   * Returns the publisher.
   *
   * @param terminology the terminology
   * @return the publisher
   */
  private static String getPublisher(final String terminology) {
    return publishers.get(terminology);
  }

  /**
   * Returns the uri.
   *
   * @param terminology the terminology
   * @return the uri
   */
  private static String getUri(final String terminology) {
    return uris.get(terminology.toLowerCase());
  }

  /**
   * To bool.
   *
   * @param bool the bool
   * @return true, if successful
   */
  public static boolean toBool(final BooleanType bool) {
    return bool != null && bool.booleanValue();
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
      return code.getValue();
    }
    if (coding != null) {
      return coding.getCode();
    }

    return null;
  }

  /**
   * To R 4.
   *
   * @param term the term
   * @return the code system
   */
  public static CodeSystem toR4(final Terminology term) {
    final CodeSystem cs = new CodeSystem();
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
   * To R 4.
   *
   * @param mapset the mapset
   * @return the concept map
   */
  public static ConceptMap toR4(final Concept mapset) {
    final ConceptMap cm = new ConceptMap();
    cm.setId((mapset.getCode() + "_" + mapset.getVersion()).toLowerCase());
    cm.setName(mapset.getName());
    cm.setTitle(mapset.getCode());
    cm.setExperimental(false);
    cm.setStatus(Enumerations.PublicationStatus.ACTIVE);
    cm.setVersion(mapset.getVersion());
    cm.setPublisher(
        getPublisher(
            mapset.getProperties().stream()
                .filter(m -> m.getType().equals("sourceTerminology"))
                .findFirst()
                .orElse(null)
                .getValue()));
    cm.setSource(
        new UriType(
            getUri(
                mapset.getProperties().stream()
                    .filter(m -> m.getType().equals("sourceTerminology"))
                    .findFirst()
                    .get()
                    .getValue())));
    cm.setTarget(
        new UriType(
            getUri(
                mapset.getProperties().stream()
                    .filter(m -> m.getType().equals("targetTerminology"))
                    .findFirst()
                    .get()
                    .getValue())));
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
   * To R 4 VS.
   *
   * @param term the term
   * @return the value set
   */
  public static ValueSet toR4VS(final Terminology term) {
    final ValueSet vs = new ValueSet();
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
   * To R 4 VS.
   *
   * @param subset the subset
   * @return the value set
   */
  public static ValueSet toR4VS(final Concept subset) {
    final ValueSet vs = new ValueSet();
    vs.setId((subset.getTerminology() + "_" + subset.getCode()).toLowerCase());
    vs.setUrl(getUri(subset.getTerminology()) + "?fhir_vs=" + subset.getCode());
    vs.setName(subset.getName());
    vs.setVersion(subset.getVersion());
    vs.setTitle(subset.getTerminology());
    vs.setPublisher(getPublisher(subset.getTerminology()));
    vs.setDescription(
        "Value set representing the " + subset.getTerminology() + " subset " + subset.getCode());
    vs.addIdentifier(new Identifier().setValue(subset.getCode()));
    vs.setExperimental(false);
    vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
    return vs;
  }

  /**
   * Required.
   *
   * @param param1Name the param 1 name
   * @param param1 the param 1
   */
  public static void required(final String param1Name, final Object param1) {
    if (param1 == null) {
      throw exception(format("Must use '%s' parameter.", param1Name), IssueType.INVARIANT, 400);
    }
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
          IssueType.INVARIANT,
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
      throw exception(message, IssueType.NOTSUPPORTED, 400);
    }
  }

  /**
   * Not supported.
   *
   * @param request the request
   * @param paramName the param name
   */
  public static void notSupported(final HttpServletRequest request, final String paramName) {
    if (request.getParameterMap().containsKey(paramName)) {
      final String message = format("Input parameter '%s' is not supported", paramName);
      throw exception(message, OperationOutcome.IssueType.NOTSUPPORTED, 400);
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
   * Require at least one of.
   *
   * @param param1Name the param 1 name
   * @param param1 the param 1
   * @param param2Name the param 2 name
   * @param param2 the param 2
   * @param param3Name the param 3 name
   * @param param3 the param 3
   * @param param4Name the param 4 name
   * @param param4 the param 4
   */
  public static void requireAtLeastOneOf(
      final String param1Name,
      final Object param1,
      final String param2Name,
      final Object param2,
      final String param3Name,
      final Object param3,
      final String param4Name,
      final Object param4) {

    if (param1 == null && param2 == null && param3 == null && param4 == null) {
      throw exception(
          format(
              "At least one of '%s', '%s', '%s', or '%s' parameters must be supplied.",
              param1Name, param2Name, param3Name, param4Name),
          IssueType.INVARIANT,
          400);
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
          IssueType.INVARIANT,
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
          "Use either 'code' or 'coding' parameters, not both.", IssueType.INVARIANT, 400);
    } else if (code != null) {
      if (code.getCode().contains("|")) {
        throw exception(
            "The 'code' parameter cannot supply a codeSystem. "
                + "Use 'coding' or provide CodeSystem in 'system' parameter.",
            IssueType.NOTSUPPORTED,
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
   * Exception.
   *
   * @param message the message
   * @param issueType the issue type
   * @param theStatusCode the status code
   * @param e the e
   * @return the FHIR server response exception
   */
  public static FHIRServerResponseException exception(
      final String message, final IssueType issueType, final int theStatusCode, final Throwable e) {
    final OperationOutcome outcome = new OperationOutcome();
    final OperationOutcomeIssueComponent component = new OperationOutcomeIssueComponent();
    component.setSeverity(IssueSeverity.ERROR);
    component.setCode(issueType);
    component.setDiagnostics(message);
    outcome.addIssue(component);
    return new FHIRServerResponseException(theStatusCode, message, outcome, e);
  }

  /**
   * Creates the property.
   *
   * @param propertyName the property name
   * @param propertyValue the property value
   * @param isCode the is code
   * @return the parameters. parameters parameter component
   */
  public static ParametersParameterComponent createProperty(
      final String propertyName, final Object propertyValue, final boolean isCode) {
    // Make a property with code as "valueCode"
    final ParametersParameterComponent property =
        new ParametersParameterComponent().setName("property");
    property.addPart().setName("code").setValue(new CodeType(propertyName));

    // Determine the value
    final String propertyValueString = propertyValue == null ? "" : propertyValue.toString();
    // Set code type
    if (isCode) {
      property.addPart().setName("value").setValue(new CodeType(propertyValueString));
    }

    // Set coding ntype
    else if (propertyValue instanceof Coding) {
      property.addPart().setName("value").setValue((Coding) propertyValue);
    }
    // Set boolean type
    else if (propertyValue instanceof Boolean) {
      property.addPart().setName("value").setValue(new BooleanType((Boolean) propertyValue));
    }
    // Set date type
    else if (propertyValue instanceof Date) {
      property.addPart().setName("value").setValue(new DateTimeType((Date) propertyValue));
    }
    // Otherwise use a string
    else {
      final StringType value = new StringType(propertyValueString);
      property.addPart().setName("value").setValue(value);
    }
    return property;
  }

  /**
   * Returns the next link.
   *
   * @param uri the uri
   * @param offset the offset
   * @param offsetInt the offset int
   * @param count the count
   * @param countInt the count int
   * @return the next link
   */
  public static BundleLinkComponent getNextLink(
      final String uri,
      final NumberParam offset,
      final int offsetInt,
      final NumberParam count,
      final int countInt) {
    final int nextOffset = offsetInt + countInt;
    String nextUri = uri;
    if (!uri.contains("?")) {
      nextUri = nextUri + "?";
    }
    if (offset != null) {
      nextUri = nextUri.replaceFirst("_offset=\\d+", "_offset=" + nextOffset);
    } else {
      nextUri += (nextUri.endsWith("?") ? "" : "&") + "_offset=" + nextOffset;
    }
    if (count != null) {
      nextUri = nextUri.replaceFirst("_count=\\d+", "_count=" + countInt);
    } else {
      nextUri += (nextUri.endsWith("?") ? "" : "&") + "_count=" + countInt;
    }

    return new BundleLinkComponent().setUrl(nextUri).setRelation("next");
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
    return new BundleLinkComponent().setUrl(prevUri).setRelation("previous");
  }

  /**
   * Make bundle.
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
    // This isn't adding the link relation, it's automatically being set. Commenting out
    // bundle.addLink(new BundleLinkComponent().setUrl(thisUrl).setRelation("self"));
    if (offsetInt + countInt < list.size()) {
      bundle.addLink(FhirUtilityR4.getNextLink(thisUrl, offset, offsetInt, count, countInt));
    }
    if (offsetInt - countInt >= list.size()) {
      bundle.addLink(FhirUtilityR4.getPrevLink(thisUrl, offset, offsetInt, count, countInt));
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
