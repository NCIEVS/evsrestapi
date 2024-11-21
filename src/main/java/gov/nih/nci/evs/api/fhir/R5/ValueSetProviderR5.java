package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import gov.nih.nci.evs.api.util.FhirUtility;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** FHIR R5 ValueSet provider. */
@Component
public class ValueSetProviderR5 implements IResourceProvider {
  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(ValueSetProviderR5.class);

  /** The search service. */
  @Autowired ElasticSearchService searchService;

  /** The search service. */
  @Autowired ElasticQueryService esQueryService;

  /** The metadata service. */
  @Autowired MetadataService metadataService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;

  /**
   * Returns the type of resource for this provider.
   *
   * @return the resource
   */
  @Override
  public Class<ValueSet> getResourceType() {
    return ValueSet.class;
  }

  /**
   * Find value sets.
   *
   * @param request the request
   * @param id the id
   * @param code the code
   * @param name the name
   * @param system the system
   * @param url the url
   * @param version the version
   * @param count the count
   * @param offset the offset
   * @return the list
   * @throws Exception the exception
   */
  @Search
  public Bundle findValueSets(
      final HttpServletRequest request,
      @OptionalParam(name = "_id") final TokenParam id,
      @OptionalParam(name = "code") final StringParam code,
      @OptionalParam(name = "name") final StringParam name,
      @OptionalParam(name = "system") final UriType system,
      @OptionalParam(name = "url") final StringParam url,
      @OptionalParam(name = "version") final StringParam version,
      @Description(shortDefinition = "Number of entries to return") @OptionalParam(name = "_count")
          final NumberParam count,
      @Description(shortDefinition = "Start offset, used when reading a next page")
          @OptionalParam(name = "_offset")
          final NumberParam offset)
      throws Exception {
    FhirUtilityR5.notSupportedSearchParams(request);
    final List<Terminology> terms = termUtils.getIndexedTerminologies(esQueryService);
    final List<ValueSet> list = new ArrayList<>();

    if (code == null) {
      for (final Terminology terminology : terms) {
        final ValueSet vs = FhirUtilityR5.toR5VS(terminology);
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
        metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.empty());
    final Set<String> codes =
        subsets.stream()
            .flatMap(Concept::streamSelfAndChildren)
            .map(c -> c.getCode())
            .collect(Collectors.toSet());
    final List<Concept> subsetsAsConcepts =
        esQueryService.getConcepts(
            codes,
            termUtils.getIndexedTerminology("ncit", esQueryService),
            new IncludeParam("minimal"));

    for (final Concept subset : subsetsAsConcepts) {
      final ValueSet vs = FhirUtilityR5.toR5VS(subset);
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
      if (code != null
          && !vs.getIdentifier().stream().anyMatch(i -> i.getValue().equals(code.getValue()))) {
        logger.info("  SKIP code mismatch = " + vs.getTitle());
        continue;
      }
      list.add(vs);
    }
    return FhirUtilityR5.makeBundle(request, list, count, offset);
  }

  /**
   * Expand implicit.
   *
   * <pre>
   * <a href="https://hl7.org/fhir/R5/valueset-operation-expand.html">valueset operation expand</a>
   * </pre>
   *
   * @param request the request
   * @param details the details
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
  @Operation(name = JpaConstants.OPERATION_EXPAND, idempotent = true)
  public ValueSet expandImplicit(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "valueSet") final ValueSet valueSet,
      @OperationParam(name = "valueSetVersion") final StringType version,
      @OperationParam(name = "context") final UriType context,
      @OperationParam(name = "contextDirection") final CodeType contextDirection,
      @OperationParam(name = "filter") final StringType filter,
      @OperationParam(name = "date") final DateTimeType date,
      @OperationParam(name = "offset") final IntegerType offset,
      @OperationParam(name = "count") final IntegerType count,
      @OperationParam(name = "includeDesignations") final BooleanType includeDesignations,
      @OperationParam(name = "designation") final StringType designation,
      @OperationParam(name = "includeDefinition") final BooleanType includeDefinition,
      @OperationParam(name = "activeOnly") final BooleanType activeOnly,
      @OperationParam(name = "excludeNested") final BooleanType excludeNested,
      @OperationParam(name = "excludeNotForUI") final BooleanType excludeNotForUI,
      @OperationParam(name = "excludePostCoordinated") final BooleanType excludePostCoordinated,
      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      @OperationParam(name = "exclude-system") final StringType exclude_system,
      @OperationParam(name = "system-version") final StringType system_version,
      @OperationParam(name = "check-system-version") final StringType check_system_version,
      @OperationParam(name = "force-system-version") final StringType force_system_version)
      throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_EXPAND,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.required(url, "url");
      FhirUtilityR5.notSupported(context, "context");
      FhirUtilityR5.notSupported(valueSet, "valueSet");
      FhirUtilityR5.notSupported(contextDirection, "contextDirection");
      FhirUtilityR5.notSupported(date, "date");
      FhirUtilityR5.notSupported(includeDesignations, "includeDesignations");
      FhirUtilityR5.notSupported(designation, "designation");
      FhirUtilityR5.notSupported(includeDefinition, "includeDefinition");
      FhirUtilityR5.notSupported(excludeNested, "excludeNested");
      FhirUtilityR5.notSupported(excludeNotForUI, "excludeNotForUI");
      FhirUtilityR5.notSupported(excludePostCoordinated, "excludePostCoordinated");
      FhirUtilityR5.notSupported(displayLanguage, "displayLanguage");
      FhirUtilityR5.notSupported(exclude_system, "exclude-system");
      FhirUtilityR5.notSupported(system_version, "system-version");
      FhirUtilityR5.notSupported(check_system_version, "check-system-version");
      FhirUtilityR5.notSupported(force_system_version, "force-system-version");
      final List<ValueSet> vsList = findPossibleValueSets(null, null, url, version);
      if (vsList.isEmpty()) {
        throw FhirUtilityR5.exception("Value set " + url + " not found", IssueType.EXCEPTION, 500);
      }
      final ValueSet vs = vsList.get(0);
      List<Concept> subsetMembers = new ArrayList<Concept>();
      if (url.getValue().contains("?fhir_vs=$")) {
        final List<Association> invAssoc =
            esQueryService
                .getConcept(
                    vs.getIdentifier().get(0).getValue(),
                    termUtils.getIndexedTerminology(vs.getTitle(), esQueryService),
                    new IncludeParam("inverseAssociations"))
                .get()
                .getInverseAssociations();
        for (final Association assn : invAssoc) {
          final Concept member =
              esQueryService
                  .getConcept(
                      assn.getRelatedCode(),
                      termUtils.getIndexedTerminology(vs.getTitle(), esQueryService),
                      new IncludeParam("minimal"))
                  .orElse(null);
          if (member != null) {
            subsetMembers.add(member);
          }
        }
      } else {
        final List<Terminology> terminologies = new ArrayList<>();
        terminologies.add(termUtils.getIndexedTerminology(vs.getTitle(), esQueryService));
        final SearchCriteria sc = new SearchCriteria();
        sc.setPageSize(count != null ? count.getValue() : 10);
        sc.setFromRecord(offset != null ? offset.getValue() : 0);
        sc.setTerm(filter != null ? filter.getValue() : null);
        sc.setType("contains");
        sc.setTerminology(
            terminologies.stream().map(Terminology::getTerminology).collect(Collectors.toList()));
        subsetMembers = searchService.search(terminologies, sc).getConcepts();
      }
      final ValueSet.ValueSetExpansionComponent vsExpansion =
          new ValueSet.ValueSetExpansionComponent();
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset.getValue() : 0);
      vsExpansion.setTotal(subsetMembers.size());
      if (!subsetMembers.isEmpty()) {
        for (final Concept subset : subsetMembers) {
          if (activeOnly != null && activeOnly.getValue() && !subset.getActive()) {
            continue;
          }
          final ValueSet.ValueSetExpansionContainsComponent vsContains =
              new ValueSet.ValueSetExpansionContainsComponent();
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
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to load value set", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Expand instance.
   *
   * <pre>
   * <a href="https://hl7.org/fhir/R5/valueset-operation-expand.html">valueset operation expand</a>
   * </pre>
   *
   * @param request the request
   * @param details the details
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
  @Operation(name = JpaConstants.OPERATION_EXPAND, idempotent = true)
  public ValueSet expandInstance(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "valueSet") final ValueSet valueSet,
      @OperationParam(name = "valueSetVersion") final StringType version,
      @OperationParam(name = "context") final UriType context,
      @OperationParam(name = "contextDirection") final CodeType contextDirection,
      @OperationParam(name = "filter") final StringType filter,
      @OperationParam(name = "date") final DateTimeType date,
      @OperationParam(name = "offset") final IntegerType offset,
      @OperationParam(name = "count") final IntegerType count,
      @OperationParam(name = "includeDesignations") final BooleanType includeDesignations,
      @OperationParam(name = "designation") final StringType designation,
      @OperationParam(name = "includeDefinition") final BooleanType includeDefinition,
      @OperationParam(name = "activeOnly") final BooleanType activeOnly,
      @OperationParam(name = "excludeNested") final BooleanType excludeNested,
      @OperationParam(name = "excludeNotForUI") final BooleanType excludeNotForUI,
      @OperationParam(name = "excludePostCoordinated") final BooleanType excludePostCoordinated,
      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      @OperationParam(name = "exclude-system") final StringType exclude_system,
      @OperationParam(name = "system-version") final StringType system_version,
      @OperationParam(name = "check-system-version") final StringType check_system_version,
      @OperationParam(name = "force-system-version") final StringType force_system_version)
      throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_EXPAND,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      // URL is not required because "id" is provided
      //      FhirUtilityR5.required(url, "url");
      FhirUtilityR5.notSupported(valueSet, "valueSet");
      FhirUtilityR5.notSupported(context, "context");
      FhirUtilityR5.notSupported(contextDirection, "contextDirection");
      FhirUtilityR5.notSupported(date, "date");
      FhirUtilityR5.notSupported(includeDesignations, "includeDesignations");
      FhirUtilityR5.notSupported(designation, "designation");
      FhirUtilityR5.notSupported(includeDefinition, "includeDefinition");
      FhirUtilityR5.notSupported(excludeNested, "excludeNested");
      FhirUtilityR5.notSupported(excludeNotForUI, "excludeNotForUI");
      FhirUtilityR5.notSupported(excludePostCoordinated, "excludePostCoordinated");
      FhirUtilityR5.notSupported(displayLanguage, "displayLanguage");
      FhirUtilityR5.notSupported(exclude_system, "exclude-system");
      FhirUtilityR5.notSupported(system_version, "system-version");
      FhirUtilityR5.notSupported(check_system_version, "check-system-version");
      FhirUtilityR5.notSupported(force_system_version, "force-system-version");
      final List<ValueSet> vsList = findPossibleValueSets(id, null, url, version);
      if (vsList.isEmpty()) {
        throw FhirUtilityR5.exception("Value set " + url + " not found", IssueType.EXCEPTION, 500);
      }
      final ValueSet vs = vsList.get(0);
      List<Concept> subsetMembers = new ArrayList<Concept>();
      if (url.getValue().contains("?fhir_vs=$")) {
        final List<Association> invAssoc =
            esQueryService
                .getConcept(
                    vs.getIdentifier().get(0).getValue(),
                    termUtils.getIndexedTerminology(vs.getTitle(), esQueryService),
                    new IncludeParam("inverseAssociations"))
                .get()
                .getInverseAssociations();
        for (final Association assn : invAssoc) {
          final Concept member =
              esQueryService
                  .getConcept(
                      assn.getRelatedCode(),
                      termUtils.getIndexedTerminology(vs.getTitle(), esQueryService),
                      new IncludeParam("minimal"))
                  .orElse(null);
          if (member != null) {
            subsetMembers.add(member);
          }
        }
      } else {
        final List<Terminology> terminologies = new ArrayList<>();
        terminologies.add(termUtils.getIndexedTerminology(vs.getTitle(), esQueryService));
        final SearchCriteria sc = new SearchCriteria();
        sc.setPageSize(count != null ? count.getValue() : 10);
        sc.setFromRecord(offset != null ? offset.getValue() : 0);
        sc.setTerm(filter != null ? filter.getValue() : null);
        sc.setType("contains");
        sc.setTerminology(
            terminologies.stream().map(Terminology::getTerminology).collect(Collectors.toList()));
        subsetMembers = searchService.search(terminologies, sc).getConcepts();
      }
      final ValueSet.ValueSetExpansionComponent vsExpansion =
          new ValueSet.ValueSetExpansionComponent();
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset.getValue() : 0);
      vsExpansion.setTotal(subsetMembers.size());
      if (!subsetMembers.isEmpty()) {
        for (final Concept subset : subsetMembers) {
          if (activeOnly != null && activeOnly.getValue() && !subset.getActive()) {
            continue;
          }
          final ValueSet.ValueSetExpansionContainsComponent vsContains =
              new ValueSet.ValueSetExpansionContainsComponent();
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
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to load value set", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code implicit.
   *
   * <pre>
   * <a href="https://hl7.org/fhir/R5/valueset-operation-validate-code.html">valueset operation validate code</a>
   * </pre>
   *
   * @param request the request
   * @param details the details
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
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeImplicit(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "context") final UriType context,
      @OperationParam(name = "valueSet") final ValueSet valueSet,
      @OperationParam(name = "valueSetVersion") final StringType valueSetVersion,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "systemVersion") final StringType systemVersion,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      @OperationParam(name = "date") final DateTimeType date,
      @OperationParam(name = "abstract") final BooleanType abstractt,
      @OperationParam(name = "displayLanguage") final StringType displayLanguage)
      throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.required(code, "code");
      FhirUtilityR5.mutuallyRequired(code, "code", system, "system", url, "url");
      FhirUtilityR5.mutuallyRequired(system, "system", systemVersion, "systemVersion");
      FhirUtilityR5.notSupported(codeableConcept, "codeableConcept");
      FhirUtilityR5.notSupported(coding, "coding");
      FhirUtilityR5.notSupported(context, "context");
      FhirUtilityR5.notSupported(date, "date");
      FhirUtilityR5.notSupported(abstractt, "abstract");
      FhirUtilityR5.notSupported(displayLanguage, "displayLanguage");
      FhirUtilityR5.notSupported(version, "version");
      FhirUtilityR5.notSupported(valueSet, "valueSet");
      FhirUtilityR5.notSupported(valueSetVersion, "valueSetVersion");
      final List<ValueSet> list = findPossibleValueSets(null, system, url, systemVersion);
      final Parameters params = new Parameters();

      if (!list.isEmpty()) {
        final ValueSet vs = list.get(0);
        final SearchCriteria sc = new SearchCriteria();
        sc.setTerm(code.getCode());
        sc.setInclude("minimal");
        sc.setType("exact");
        sc.setFromRecord(0);
        sc.setPageSize(1);
        if (vs.getIdentifier() != null && !vs.getIdentifier().isEmpty()) {
          sc.setSubset(Arrays.asList(vs.getIdentifier().get(0).getValue()));
        }
        final Terminology term = termUtils.getIndexedTerminology(vs.getTitle(), esQueryService);
        sc.setTerminology(Arrays.asList(vs.getTitle()));
        sc.validate(term, metadataService);
        final List<Terminology> terms = Arrays.asList(term);
        final List<Concept> conc = searchService.search(terms, sc).getConcepts();
        if (!conc.isEmpty()) {
          params.addParameter("result", true);
          params.addParameter("display", conc.get(0).getName());
          if (display != null && !display.getValue().equals(conc.get(0).getName())) {
            params.addParameter(
                "message",
                "The code '"
                    + code
                    + "' was found in this value set, however the display '"
                    + display
                    + "' did not match any designations.");
          }
        } else {
          params.addParameter("result", false);
          params.addParameter("message", "The code '" + code.getCode() + "' was not found.");
        }
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "Unable to find matching value set");
        params.addParameter("url", (url == null ? new UriType("<null>") : url));
        params.addParameter("version", version);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to load value set", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code instance.
   *
   * <pre>
   * <a href="https://hl7.org/fhir/R5/valueset-operation-validate-code.html">valueset operation validate code</a>
   * </pre>
   *
   * @param request the request
   * @param details the details
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
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeInstance(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "context") final UriType context,
      @OperationParam(name = "valueSet") final ValueSet valueSet,
      @OperationParam(name = "valueSetVersion") final StringType valueSetVersion,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "systemVersion") final StringType systemVersion,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      @OperationParam(name = "date") final DateTimeType date,
      @OperationParam(name = "abstract") final BooleanType abstractt,
      @OperationParam(name = "displayLanguage") final StringType displayLanguage)
      throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.requireAtLeastOneOf(
          code, "code", system, "system", systemVersion, "systemVersion", url, "url");
      FhirUtilityR5.notSupported(codeableConcept, "codeableConcept");
      FhirUtilityR5.notSupported(coding, "coding");
      FhirUtilityR5.notSupported(context, "context");
      FhirUtilityR5.notSupported(date, "date");
      FhirUtilityR5.notSupported(abstractt, "abstract");
      FhirUtilityR5.notSupported(displayLanguage, "displayLanguage");
      FhirUtilityR5.notSupported(version, "version");
      FhirUtilityR5.notSupported(valueSet, "valueSet");
      FhirUtilityR5.notSupported(valueSetVersion, "valueSetVersion");
      final List<ValueSet> list = findPossibleValueSets(id, system, url, systemVersion);
      final Parameters params = new Parameters();
      if (!list.isEmpty()) {
        final ValueSet vs = list.get(0);
        final SearchCriteria sc = new SearchCriteria();
        sc.setTerm(code.getCode());
        sc.setInclude("minimal");
        sc.setType("exact");
        if (vs.getIdentifier() != null && !vs.getIdentifier().isEmpty()) {
          sc.setSubset(Arrays.asList(vs.getIdentifier().get(0).getValue()));
        }

        final Terminology term = termUtils.getIndexedTerminology(vs.getTitle(), esQueryService);
        sc.validate(term, metadataService);
        final List<Terminology> terms = Arrays.asList(term);
        final List<Concept> conc = searchService.search(terms, sc).getConcepts();

        if (!conc.isEmpty()) {
          params.addParameter("result", true);
          params.addParameter("display", conc.get(0).getName());
          if (display != null && !display.getValue().equals(conc.get(0).getName())) {
            params.addParameter(
                "message",
                "The code '"
                    + code
                    + "' was found in this value set, however the display '"
                    + display
                    + "' did not match any designations.");
          }
        } else {
          params.addParameter("result", false);
          params.addParameter("message", "The code '" + code.getCode() + "' was not found.");
        }
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "Unable to find matching value set");
        params.addParameter("url", (url == null ? new UriType("<null>") : url));
        params.addParameter("version", version);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to load value set", IssueType.EXCEPTION, 500);
    }
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
  public ValueSet getValueSet(final ServletRequestDetails details, @IdParam final IdType id)
      throws Exception {
    try {
      final List<ValueSet> candidates = findPossibleValueSets(id, null, null, null);
      for (final ValueSet set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }
      throw FhirUtilityR5.exception(
          "Value set not found = " + (id == null ? "null" : id.getIdPart()),
          IssueType.NOTFOUND,
          404);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR5.exception("Failed to get value set", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Helper method to find possible value sets.
   *
   * @param id the id
   * @param system the system
   * @param url the url
   * @param version the version
   * @return the list
   * @throws Exception the exception
   */
  public List<ValueSet> findPossibleValueSets(
      @OptionalParam(name = "_id") final IdType id,
      @OptionalParam(name = "system") final UriType system,
      @OptionalParam(name = "url") final UriType url,
      @OptionalParam(name = "version") final StringType version)
      throws Exception {
    // If no ID and no url are specified, no code systems match
    if (id == null && url == null) {
      return new ArrayList<>(0);
    }

    final List<Terminology> terms = termUtils.getIndexedTerminologies(esQueryService);
    final List<ValueSet> list = new ArrayList<ValueSet>();

    for (final Terminology terminology : terms) {
      final ValueSet vs = FhirUtilityR5.toR5VS(terminology);
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
    final List<Concept> subsets =
        metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.empty());
    final Set<String> codes =
        subsets.stream()
            .flatMap(Concept::streamSelfAndChildren)
            .map(c -> c.getCode())
            .collect(Collectors.toSet());
    final List<Concept> subsetsAsConcepts =
        esQueryService.getConcepts(
            codes,
            termUtils.getIndexedTerminology("ncit", esQueryService),
            new IncludeParam("minimal"));

    for (final Concept subset : subsetsAsConcepts) {
      final ValueSet vs = FhirUtilityR5.toR5VS(subset);
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
      list.add(vs);
    }
    return list;
  }
}
