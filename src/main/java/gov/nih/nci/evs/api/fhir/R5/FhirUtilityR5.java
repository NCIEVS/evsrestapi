package gov.nih.nci.evs.api.fhir.R5;

import static java.lang.String.format;

import ca.uhn.fhir.rest.param.NumberParam;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r5.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.Bundle.LinkRelationTypes;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.ConceptMap.ConceptMapGroupComponent;
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
import org.hl7.fhir.r5.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FhirUtilityR5 {
  /** The logger. */
  @SuppressWarnings("unused")
  private static Logger logger = LoggerFactory.getLogger(FhirUtilityR5.class);

  /** The publishers. */
  private static HashMap<String, String> publishers = generatePublishers();

  /** The URIs */
  private static HashMap<String, String> uris = generateUris();

  /** Instantiates an empty {@link FhirUtilityR5} */
  private FhirUtilityR5() {
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
    uri.put("snomedct_us", "http://terminology.hl7.org/CodeSystem/snomedct_us");
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
    return uri;
  }

  /**
   * Gets the publisher.
   *
   * @param terminology the terminology
   * @return the publisher for a given terminology
   */
  private static String getPublisher(final String terminology) {
    return publishers.get(terminology);
  }

  /**
   * Gets the uri.
   *
   * @param terminology the terminology
   * @return the uri for a given terminology
   */
  private static String getUri(final String terminology) {
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
    cs.setId(term.getTerminologyVersion());
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
    cm.setId(mapset.getCode() + "_" + mapset.getVersion());
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

    // populate the r5 concept map group
    final ConceptMapGroupComponent group = new ConceptMapGroupComponent();
    group.setSource(
        mapset.getProperties().stream()
            .filter(m -> m.getType().equals("sourceTerminology"))
            .findFirst()
            .get()
            .getValue());
    group.setTarget(
        mapset.getProperties().stream()
            .filter(m -> m.getType().equals("targetTerminology"))
            .findFirst()
            .get()
            .getValue());
    group.setSourceElement(
        new CanonicalType(
            getUri(
                mapset.getProperties().stream()
                    .filter(m -> m.getType().equals("sourceTerminology"))
                    .findFirst()
                    .get()
                    .getValue())));
    group.setTargetElement(
        new CanonicalType(
            getUri(
                mapset.getProperties().stream()
                    .filter(m -> m.getType().equals("targetTerminology"))
                    .findFirst()
                    .get()
                    .getValue())));
    // populate the r5 concept map group element
    cm.addGroup(group);
    cm.setUrl(group.getSourceElement().asStringValue() + "?fhir_cm=" + mapset.getCode());
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
    vs.setId(term.getTerminology() + "_" + term.getVersion());
    vs.setName(term.getName());
    vs.setVersion(term.getVersion());
    vs.setTitle(term.getTerminology());
    vs.setUrl(getUri(term.getTerminology()));
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
    vs.setId(subset.getTerminology() + "_" + subset.getCode());
    vs.setUrl(getUri(subset.getTerminology()) + "?fhir_vs=" + subset.getCode());
    vs.setName(subset.getName());
    vs.setVersion(subset.getVersion());
    vs.setTitle(subset.getTerminology());
    vs.setPublisher(getPublisher(subset.getTerminology()));
    vs.setDescription(
        "Value set reprenting the " + subset.getTerminology() + "subset" + subset.getCode());
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
      throw exception(format("Must use %s parameter.", objName), IssueType.INVARIANT, 400);
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
          format("Must use one of %s or %s parameters", obj1Name, obj2Name),
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
              "Input parameter %s is not supported%s",
              objName, (additionalDetail == null ? "." : format(" %s", additionalDetail)));
      throw exception(message, IssueType.NOTSUPPORTED, 400);
    }
  }

  /**
   * Check we have at least one required object
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
          format("Must supply one of %s or %s parameters.", obj1Name, obj2Name),
          IssueType.INVARIANT,
          400);
    } else {
      mutuallyExclusive(obj1, obj1Name, obj2, obj2Name);
    }
  }

  /**
   * Check we have at least one required object
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
              "Must supply at least one of %s, %s, %s, or %s parameters.",
              obj1Name, obj2Name, obj3Name, obj4Name),
          IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Check we have exactly one required object
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
              "Must supply at least one of %s, %s, or %s parameters.",
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
   * Check we have both required fields
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
              "Input parameter %s can only be used in conjunction with parameter %s.",
              obj1Name, obj2Name),
          IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Check we have all required fields
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
              "Use of input parameter %s only allowed if %s or %s is also present.",
              obj1Name, obj2Name, obj3Name),
          IssueType.INVARIANT,
          400);
    }
  }

  /**
   * Get the recover code for a CodeType or Coding System
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
   * Method for reporting unsupported exception
   *
   * @param message the message
   * @return the fhir server response exception
   */
  public static FHIRServerResponseException exceptionNotSupported(final String message) {
    return exception(message, IssueType.NOTSUPPORTED, 501);
  }

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
   * Create a property for a parameter
   *
   * @param propertyValue the property value
   * @param propertyName the property name
   * @param isCode is the property a code
   * @return the parameter property component
   */
  public static ParametersParameterComponent createProperty(
      final Object propertyValue, final String propertyName, final boolean isCode) {
    String valueName = "value";
    // Create a property and add the code as a valueCode
    final ParametersParameterComponent property = new ParametersParameterComponent();
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
   * Get the next link component
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

    if (!uri.contains("?")) {
      nextUri = nextUri + "?";
    }
    if (offset != null) {
      nextUri = nextUri.replaceFirst("offset=\\d+", "_offset=" + nextOffset);
    } else {
      nextUri += (nextUri.endsWith("?") ? "" : "&") + "_offset=" + nextOffset;
    }
    if (count != null) {
      nextUri = nextUri.replaceFirst("_count=\\d+", "_count=" + countInt);
    } else {
      nextUri += (nextUri.endsWith("?") ? "" : "&") + "_count=" + countInt;
    }

    return new BundleLinkComponent().setUrl(nextUri).setRelation(LinkRelationTypes.NEXT);
  }

  /**
   * Make a bundle
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
    int countInt = count == null ? 100 : count.getValue().intValue();
    int offsetInt = offset == null ? 0 : offset.getValue().intValue();
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
