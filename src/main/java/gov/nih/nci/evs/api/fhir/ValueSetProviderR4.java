
package gov.nih.nci.evs.api.fhir;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.controller.SubsetController;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.util.TerminologyUtils;

/**
 * The ValueSet provider.
 */
@Component
public class ValueSetProviderR4 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(ValueSetProviderR4.class);

  /** The search service. */
  @Autowired
  ElasticSearchService searchService;

  /** The search service. */
  @Autowired
  ElasticQueryService queryService;

  /** The metadata service. */
  @Autowired
  MetadataService metadataService;

  /** The subset controller. */
  @Autowired
  SubsetController subsetController;

  /** The term utils. */
  /* The terminology utils */
  @Autowired
  TerminologyUtils termUtils;

  /**
   * Expand implicit.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-expand.html
   * </pre>
   *
   * @param url the url
   * @param valueSet the value set
   * @param version the version
   * @param context the context
   * @param contextDirection the context direction
   * @param filter the filter
   * @param date the date
   * @param offset the offset
   * @param count the count
   * @param includeDesignations the include designations
   * @param designation the designation
   * @param includeDefinition the include definition
   * @param activeOnly the active only
   * @param excludeNested the exclude nested
   * @param excludeNotForUI the exclude not for UI
   * @param excludePostCoordinated the exclude post coordinated
   * @param displayLanguage the display language
   * @param exclude_system the exclude system
   * @param system_version the system version
   * @param check_system_version the check system version
   * @param force_system_version the force system version
   * @return the value set
   * @throws Exception the exception
   */
  @Operation(name = "$expand", idempotent = true)
  public ValueSet expandImplicit(@OperationParam(name = "url")
  final UriType url, @OperationParam(name = "valueSet")
  final ValueSet valueSet, @OperationParam(name = "valueSetVersion")
  final StringType version, @OperationParam(name = "context")
  final UriType context, @OperationParam(name = "contextDirection")
  final CodeType contextDirection, @OperationParam(name = "filter")
  final StringType filter, @OperationParam(name = "date")
  final DateTimeType date, @OperationParam(name = "offset")
  final IntegerType offset, @OperationParam(name = "count")
  final IntegerType count, @OperationParam(name = "includeDesignations")
  final BooleanType includeDesignations, @OperationParam(name = "designation")
  final StringType designation, @OperationParam(name = "includeDefinition")
  final BooleanType includeDefinition, @OperationParam(name = "activeOnly")
  final BooleanType activeOnly, @OperationParam(name = "excludeNested")
  final BooleanType excludeNested, @OperationParam(name = "excludeNotForUI")
  final BooleanType excludeNotForUI, @OperationParam(name = "excludePostCoordinated")
  final BooleanType excludePostCoordinated, @OperationParam(name = "displayLanguage")
  final StringType displayLanguage, @OperationParam(name = "exclude-system")
  final StringType exclude_system, @OperationParam(name = "system-version")
  final StringType system_version, @OperationParam(name = "check-system-version")
  final StringType check_system_version, @OperationParam(name = "force-system-version")
  final StringType force_system_version) throws Exception {

    try {
      FhirUtilityR4.required("url", url);
      FhirUtilityR4.notSupported("valueSet", valueSet);
      FhirUtilityR4.notSupported("context", context);
      FhirUtilityR4.notSupported("contextDirection", contextDirection);
      FhirUtilityR4.notSupported("date", date);
      FhirUtilityR4.notSupported("includeDesignations", includeDesignations);
      FhirUtilityR4.notSupported("designation", designation);
      FhirUtilityR4.notSupported("includeDefinition", includeDefinition);
      FhirUtilityR4.notSupported("excludeNested", excludeNested);
      FhirUtilityR4.notSupported("excludeNotForUI", excludeNotForUI);
      FhirUtilityR4.notSupported("excludePostCoordinated", excludePostCoordinated);
      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      FhirUtilityR4.notSupported("exclude-system", exclude_system);
      FhirUtilityR4.notSupported("system-version", system_version);
      FhirUtilityR4.notSupported("check-system-version", check_system_version);
      FhirUtilityR4.notSupported("force-system-version", force_system_version);
      final List<ValueSet> vsList = findPossibleValueSets(null, null, null, url, version);
      if (vsList.size() == 0) {
        throw FhirUtilityR4.exception("Value set " + url + " not found",
            OperationOutcome.IssueType.EXCEPTION, 500);
      }
      final ValueSet vs = vsList.get(0);
      List<Concept> subsetMembers = new ArrayList<Concept>();
      if (url.getValue().contains("?fhir_vs=$")) {
        final List<Association> invAssoc =
            queryService.getConcept(vs.getIdentifier().get(0).getValue(),
                termUtils.getTerminology(vs.getTitle(), true),
                new IncludeParam("inverseAssociations")).get().getInverseAssociations();
        for (final Association assn : invAssoc) {
          final Concept member = queryService.getConcept(assn.getRelatedCode(),
              termUtils.getTerminology(vs.getTitle(), true), new IncludeParam("minimal"))
              .orElse(null);
          if (member != null) {
            subsetMembers.add(member);
          }
        }
      } else {
        final List<Terminology> terminologies = new ArrayList<>();
        terminologies.add(termUtils.getTerminology(vs.getTitle(), true));
        final SearchCriteria sc = new SearchCriteria();
        sc.setPageSize(count != null ? count.getValue() : 10);
        sc.setFromRecord(offset != null ? offset.getValue() : 0);
        sc.setTerm(filter != null ? filter.getValue() : null);
        sc.setType("contains");
        sc.setTerminology(
            terminologies.stream().map(Terminology::getTerminology).collect(Collectors.toList()));
        subsetMembers = searchService.search(terminologies, sc).getConcepts();
      }
      final ValueSetExpansionComponent vsExpansion = new ValueSetExpansionComponent();
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset.getValue() : 0);
      vsExpansion.setTotal(subsetMembers.size());
      if (subsetMembers.size() > 0) {
        for (final Concept subset : subsetMembers) {
          if (activeOnly != null && activeOnly.getValue() && !subset.getActive()) {
            continue;
          }
          final ValueSetExpansionContainsComponent vsContains =
              new ValueSetExpansionContainsComponent();
          vsContains.setSystem(url.getValue());
          vsContains.setCode(subset.getCode());
          vsContains.setDisplay(subset.getName());
          vsExpansion.addContains(vsContains);
        }

      }
      vs.setExpansion(vsExpansion);
      return vs;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      e.printStackTrace();
      throw FhirUtilityR4.exception("Failed to load value set",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Expand instance.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-expand.html
   * </pre>
   *
   * @param id the id
   * @param url the url
   * @param valueSet the value set
   * @param version the version
   * @param context the context
   * @param contextDirection the context direction
   * @param filter the filter
   * @param date the date
   * @param offset the offset
   * @param count the count
   * @param includeDesignations the include designations
   * @param designation the designation
   * @param includeDefinition the include definition
   * @param activeOnly the active only
   * @param excludeNested the exclude nested
   * @param excludeNotForUI the exclude not for UI
   * @param excludePostCoordinated the exclude post coordinated
   * @param displayLanguage the display language
   * @param exclude_system the exclude system
   * @param system_version the system version
   * @param check_system_version the check system version
   * @param force_system_version the force system version
   * @return the value set
   * @throws Exception the exception
   */
  @Operation(name = "$expand", idempotent = true)
  public ValueSet expandInstance(@IdParam
  final IdType id, @OperationParam(name = "url")
  final UriType url, @OperationParam(name = "valueSet")
  final ValueSet valueSet, @OperationParam(name = "valueSetVersion")
  final StringType version, @OperationParam(name = "context")
  final UriType context, @OperationParam(name = "contextDirection")
  final CodeType contextDirection, @OperationParam(name = "filter")
  final StringType filter, @OperationParam(name = "date")
  final DateTimeType date, @OperationParam(name = "offset")
  final IntegerType offset, @OperationParam(name = "count")
  final IntegerType count, @OperationParam(name = "includeDesignations")
  final BooleanType includeDesignations, @OperationParam(name = "designation")
  final StringType designation, @OperationParam(name = "includeDefinition")
  final BooleanType includeDefinition, @OperationParam(name = "activeOnly")
  final BooleanType activeOnly, @OperationParam(name = "excludeNested")
  final BooleanType excludeNested, @OperationParam(name = "excludeNotForUI")
  final BooleanType excludeNotForUI, @OperationParam(name = "excludePostCoordinated")
  final BooleanType excludePostCoordinated, @OperationParam(name = "displayLanguage")
  final StringType displayLanguage, @OperationParam(name = "exclude-system")
  final StringType exclude_system, @OperationParam(name = "system-version")
  final StringType system_version, @OperationParam(name = "check-system-version")
  final StringType check_system_version, @OperationParam(name = "force-system-version")
  final StringType force_system_version) throws Exception {

    try {
      FhirUtilityR4.required("url", url);
      FhirUtilityR4.notSupported("valueSet", valueSet);
      FhirUtilityR4.notSupported("context", context);
      FhirUtilityR4.notSupported("contextDirection", contextDirection);
      FhirUtilityR4.notSupported("date", date);
      FhirUtilityR4.notSupported("includeDesignations", includeDesignations);
      FhirUtilityR4.notSupported("designation", designation);
      FhirUtilityR4.notSupported("includeDefinition", includeDefinition);
      FhirUtilityR4.notSupported("excludeNested", excludeNested);
      FhirUtilityR4.notSupported("excludeNotForUI", excludeNotForUI);
      FhirUtilityR4.notSupported("excludePostCoordinated", excludePostCoordinated);
      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      FhirUtilityR4.notSupported("exclude-system", exclude_system);
      FhirUtilityR4.notSupported("system-version", system_version);
      FhirUtilityR4.notSupported("check-system-version", check_system_version);
      FhirUtilityR4.notSupported("force-system-version", force_system_version);
      final List<ValueSet> vsList = findPossibleValueSets(id, null, null, url, version);
      if (vsList.size() == 0) {
        throw FhirUtilityR4.exception("Value set " + url + " not found",
            OperationOutcome.IssueType.EXCEPTION, 500);
      }
      final ValueSet vs = vsList.get(0);
      List<Concept> subsetMembers = new ArrayList<Concept>();
      if (url.getValue().contains("?fhir_vs=$")) {
        final List<Association> invAssoc =
            queryService.getConcept(vs.getIdentifier().get(0).getValue(),
                termUtils.getTerminology(vs.getTitle(), true),
                new IncludeParam("inverseAssociations")).get().getInverseAssociations();
        for (final Association assn : invAssoc) {
          final Concept member = queryService.getConcept(assn.getRelatedCode(),
              termUtils.getTerminology(vs.getTitle(), true), new IncludeParam("minimal"))
              .orElse(null);
          if (member != null) {
            subsetMembers.add(member);
          }
        }
      } else {
        final List<Terminology> terminologies = new ArrayList<>();
        terminologies.add(termUtils.getTerminology(vs.getTitle(), true));
        final SearchCriteria sc = new SearchCriteria();
        sc.setPageSize(count != null ? count.getValue() : 10);
        sc.setFromRecord(offset != null ? offset.getValue() : 0);
        sc.setTerm(filter != null ? filter.getValue() : null);
        sc.setType("contains");
        sc.setTerminology(
            terminologies.stream().map(Terminology::getTerminology).collect(Collectors.toList()));
        subsetMembers = searchService.search(terminologies, sc).getConcepts();
      }
      final ValueSetExpansionComponent vsExpansion = new ValueSetExpansionComponent();
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset.getValue() : 0);
      vsExpansion.setTotal(subsetMembers.size());
      if (subsetMembers.size() > 0) {
        for (final Concept subset : subsetMembers) {
          if (activeOnly != null && activeOnly.getValue() && !subset.getActive()) {
            continue;
          }
          final ValueSetExpansionContainsComponent vsContains =
              new ValueSetExpansionContainsComponent();
          vsContains.setSystem(url.getValue());
          vsContains.setCode(subset.getCode());
          vsContains.setDisplay(subset.getName());
          vsExpansion.addContains(vsContains);
        }

      }
      vs.setExpansion(vsExpansion);
      return vs;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to load value set",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code implicit.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-validate-code.html
   * </pre>
   *
   * @param url the url
   * @param context the context
   * @param valueSet the value set
   * @param valueSetVersion the value set version
   * @param code the code
   * @param system the system
   * @param systemVersion the system version
   * @param version the version
   * @param display the display
   * @param coding the coding
   * @param codeableConcept the codeable concept
   * @param date the date
   * @param abstractt the abstractt
   * @param displayLanguage the display language
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$validate-code", idempotent = true)
  public Parameters validateCodeImplicit(@OperationParam(name = "url")
  final UriType url, @OperationParam(name = "context")
  final UriType context, @OperationParam(name = "valueSet")
  final ValueSet valueSet, @OperationParam(name = "valueSetVersion")
  final StringType valueSetVersion, @OperationParam(name = "code")
  final CodeType code, @OperationParam(name = "system")
  final UriType system, @OperationParam(name = "systemVersion")
  final StringType systemVersion, @OperationParam(name = "version")
  final StringType version, @OperationParam(name = "display")
  final StringType display, @OperationParam(name = "coding")
  final Coding coding, @OperationParam(name = "codeableConcept")
  final CodeableConcept codeableConcept, @OperationParam(name = "date")
  final DateTimeType date, @OperationParam(name = "abstract")
  final BooleanType abstractt, @OperationParam(name = "displayLanguage")
  final StringType displayLanguage) throws Exception {

    try {
      FhirUtilityR4.requireAtLeastOneOf("code", code, "system", system, "systemVersion",
          systemVersion, "url", url);
      FhirUtilityR4.notSupported("codeableConcept", codeableConcept);
      FhirUtilityR4.notSupported("coding", coding);
      FhirUtilityR4.notSupported("context", context);
      FhirUtilityR4.notSupported("date", date);
      FhirUtilityR4.notSupported("abstract", abstractt);
      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      FhirUtilityR4.notSupported("version", version);
      FhirUtilityR4.notSupported("valueSet", valueSet);
      FhirUtilityR4.notSupported("valueSetVersion", valueSetVersion);
      final List<ValueSet> list = findPossibleValueSets(null, code, system, url, systemVersion);
      final Parameters params = new Parameters();

      if (list.size() > 0) {
        params.addParameter("result", true);
        params.addParameter("display", list.get(0).getName());
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "The value set was not found.");
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to load value set",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }

  }

  /**
   * Validate code instance.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-validate-code.html
   * </pre>
   *
   * @param id the id
   * @param url the url
   * @param context the context
   * @param valueSet the value set
   * @param valueSetVersion the value set version
   * @param code the code
   * @param system the system
   * @param systemVersion the system version
   * @param version the version
   * @param display the display
   * @param coding the coding
   * @param codeableConcept the codeable concept
   * @param date the date
   * @param abstractt the abstractt
   * @param displayLanguage the display language
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$validate-code", idempotent = true)
  public Parameters validateCodeInstance(@IdParam
  final IdType id, @OperationParam(name = "url")
  final UriType url, @OperationParam(name = "context")
  final UriType context, @OperationParam(name = "valueSet")
  final ValueSet valueSet, @OperationParam(name = "valueSetVersion")
  final StringType valueSetVersion, @OperationParam(name = "code")
  final CodeType code, @OperationParam(name = "system")
  final UriType system, @OperationParam(name = "systemVersion")
  final StringType systemVersion, @OperationParam(name = "version")
  final StringType version, @OperationParam(name = "display")
  final StringType display, @OperationParam(name = "coding")
  final Coding coding, @OperationParam(name = "codeableConcept")
  final CodeableConcept codeableConcept, @OperationParam(name = "date")
  final DateTimeType date, @OperationParam(name = "abstract")
  final BooleanType abstractt, @OperationParam(name = "displayLanguage")
  final StringType displayLanguage) throws Exception {

    try {
      FhirUtilityR4.requireAtLeastOneOf("code", code, "system", system, "systemVersion",
          systemVersion, "url", url);
      FhirUtilityR4.notSupported("codeableConcept", codeableConcept);
      FhirUtilityR4.notSupported("coding", coding);
      FhirUtilityR4.notSupported("context", context);
      FhirUtilityR4.notSupported("date", date);
      FhirUtilityR4.notSupported("abstract", abstractt);
      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      FhirUtilityR4.notSupported("version", version);
      FhirUtilityR4.notSupported("valueSet", valueSet);
      FhirUtilityR4.notSupported("valueSetVersion", valueSetVersion);
      final List<ValueSet> list = findPossibleValueSets(null, code, system, url, systemVersion);
      final Parameters params = new Parameters();
      if (list.size() > 0) {
        params.addParameter("result", true);
        params.addParameter("display", list.get(0).getName());
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "The value set was not found.");
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to load value set",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Find value sets.
   *
   * @param id the id
   * @param code the code
   * @param name the name
   * @param system the system
   * @param url the url
   * @param version the version
   * @return the list
   * @throws Exception the exception
   */
  @Search
  public List<ValueSet> findValueSets(@OptionalParam(name = "_id")
  final TokenParam id, @OptionalParam(name = "code")
  final StringParam code, @OptionalParam(name = "name")
  final StringParam name, @OptionalParam(name = "system")
  final UriType system, @OptionalParam(name = "url")
  final StringParam url, @OptionalParam(name = "version")
  final StringParam version) throws Exception {

    final List<Terminology> terms = termUtils.getTerminologies(true);

    final List<ValueSet> list = new ArrayList<ValueSet>();
    if (code == null) {
      for (final Terminology terminology : terms) {
        final ValueSet vs = FhirUtilityR4.toR4VS(terminology);
        // Skip non-matching
        if (id != null && !id.getValue().equals(vs.getId())) {
          logger.info("  SKIP id mismatch = " + vs.getId());
          continue;
        }
        if (url != null && !url.getValue().equals(vs.getUrl())) {
          logger.info("  SKIP url mismatch = " + vs.getUrl());
          continue;
        }
        if (system != null && !system.getValue().equals(vs.getTitle())) {
          logger.info("  SKIP system mismatch = " + vs.getTitle());
          continue;
        }
        if (name != null && !name.getValue().equals(vs.getName())) {
          logger.info("  SKIP name mismatch = " + vs.getName());
          continue;
        }
        if (version != null && !FhirUtility.compareString(version, vs.getVersion())) {
          logger.info("  SKIP version mismatch = " + vs.getVersion());
          continue;
        }

        list.add(vs);
      }
    }
    final List<Concept> subsets =
        metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.ofNullable(null));
    final Set<String> codes = subsets.stream().flatMap(Concept::streamSelfAndChildren)
        .map(c -> c.getCode()).collect(Collectors.toSet());
    final List<Concept> subsetsAsConcepts = queryService.getConcepts(codes,
        termUtils.getTerminology("ncit", true), new IncludeParam("minimal"));
    for (final Concept subset : subsetsAsConcepts) {
      final ValueSet vs = FhirUtilityR4.toR4VS(subset);
      // Skip non-matching
      if (id != null && !id.getValue().equals(vs.getId())) {
        logger.info("  SKIP id mismatch = " + vs.getUrl());
        continue;
      }
      if (url != null && !url.getValue().equals(vs.getUrl())) {
        logger.info("  SKIP url mismatch = " + vs.getUrl());
        continue;
      }
      if (system != null && !system.getValue().equals(vs.getTitle())) {
        logger.info("  SKIP system mismatch = " + vs.getTitle());
        continue;
      }
      if (name != null && !name.getValue().equals(vs.getName())) {
        logger.info("  SKIP name mismatch = " + vs.getName());
        continue;
      }
      if (code != null && !vs.getIdentifier().stream()
          .filter(i -> i.getValue().equals(code.getValue())).findAny().isPresent()) {
        logger.info("  SKIP code mismatch = " + vs.getTitle());
        continue;
      }
      list.add(vs);
    }

    return list;

  }

  /**
   * Returns the value set.
   *
   * @param details the details
   * @param id the id
   * @return the value set
   * @throws Exception the exception
   */
  @Read
  public ValueSet getValueSet(final ServletRequestDetails details, @IdParam
  final IdType id) throws Exception {
    try {

      final List<ValueSet> candidates = findPossibleValueSets(id, null, null, null, null);
      for (final ValueSet set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }

      throw FhirUtilityR4.exception(
          "Value set not found = " + (id == null ? "null" : id.getIdPart()), IssueType.NOTFOUND,
          404);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR4.exception("Failed to get value set", OperationOutcome.IssueType.EXCEPTION,
          500);
    }
  }

  /**
   * Find possible value sets.
   *
   * @param id the id
   * @param code the code
   * @param system the system
   * @param url the url
   * @param version the version
   * @return the list
   * @throws Exception the exception
   */
  public List<ValueSet> findPossibleValueSets(@OptionalParam(name = "_id")
  final IdType id, @OptionalParam(name = "code")
  final StringType code, @OptionalParam(name = "system")
  final UriType system, @OptionalParam(name = "url")
  final UriType url, @OptionalParam(name = "version")
  final StringType version) throws Exception {

    final List<Terminology> terms = termUtils.getTerminologies(true);

    final List<ValueSet> list = new ArrayList<ValueSet>();
    if (code == null) {
      for (final Terminology terminology : terms) {
        final ValueSet vs = FhirUtilityR4.toR4VS(terminology);
        // Skip non-matching
        if (id != null && !id.getIdPart().equals(vs.getId())) {
          logger.info("  SKIP id mismatch = " + vs.getId());
          continue;
        }
        if (url != null && !url.getValue().equals(vs.getUrl())) {
          logger.info("  SKIP url mismatch = " + vs.getUrl());
          continue;
        }
        if (system != null && !system.getValue().equals(vs.getTitle())) {
          logger.info("  SKIP system mismatch = " + vs.getTitle());
          continue;
        }
        if (version != null && !version.getValue().equals(vs.getVersion())) {
          logger.info("  SKIP version mismatch = " + vs.getVersion());
          continue;
        }

        list.add(vs);
      }
    }
    final List<Concept> subsets =
        metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.ofNullable(null));
    final Set<String> codes = subsets.stream().flatMap(Concept::streamSelfAndChildren)
        .map(c -> c.getCode()).collect(Collectors.toSet());
    final List<Concept> subsetsAsConcepts = queryService.getConcepts(codes,
        termUtils.getTerminology("ncit", true), new IncludeParam("minimal"));
    for (final Concept subset : subsetsAsConcepts) {
      final ValueSet vs = FhirUtilityR4.toR4VS(subset);
      // Skip non-matching
      if (id != null && !id.equals(vs.getId())) {
        logger.info("  SKIP id mismatch = " + vs.getUrl());
        continue;
      }
      if (url != null && !url.getValue().equals(vs.getUrl())) {
        logger.info("  SKIP url mismatch = " + vs.getUrl());
        continue;
      }
      if (system != null && !system.getValue().equals(vs.getTitle())) {
        logger.info("  SKIP system mismatch = " + vs.getTitle());
        continue;
      }
      if (code != null && !vs.getIdentifier().stream()
          .filter(i -> i.getValue().equals(code.getValue())).findAny().isPresent()) {
        logger.info("  SKIP code mismatch = " + vs.getTitle());
        continue;
      }
      list.add(vs);
    }

    return list;

  }

  /* see superclass */
  @Override
  public Class<ValueSet> getResourceType() {
    return ValueSet.class;
  }
}
