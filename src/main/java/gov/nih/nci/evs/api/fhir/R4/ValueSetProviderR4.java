package gov.nih.nci.evs.api.fhir.R4;

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
import gov.nih.nci.evs.api.controller.SubsetController;
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
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
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
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** FHIR R4 ValueSet provider. */
@Component
public class ValueSetProviderR4 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(ValueSetProviderR4.class);

  /** The search service. */
  @Autowired ElasticSearchService searchService;

  /** The search service. */
  @Autowired ElasticQueryService esQueryService;

  /** The metadata service. */
  @Autowired MetadataService metadataService;

  /** The subset controller. */
  @Autowired SubsetController subsetController;

  /** The term utils. */
  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;

  /**
   * Expand implicit.
   *
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-expand.html
   * </pre>
   *
   * @param request the request
   * @param details the details
   * @param url the canonical reference to the value set.
   * @param valueSet the value set
   * @param version the value set version to specify the version to be used when generating the
   *     expansion
   * @param context the context of the value set.
   * @param contextDirection the context direction, incoming or outgoing. Usually accompanied by
   *     context.
   * @param filter the text filter applied to restrict code that are returned.
   * @param date the date for which the expansion should be generated.
   * @param offset the offset for number of records.
   * @param count the count for codes that should be provided in the partial page view.
   * @param includeDesignations the include designations flag for included/excluded in value set
   *     expansions
   * @param designation the designation token that specifies the system + code that either a use or
   *     language.
   * @param includeDefinition controls whether the value set definition is included or excluded in
   *     value set expansions
   * @param activeOnly controls whether inactive concepts are included or excluded in value set
   *     expansions.
   * @param excludeNested controls whether the value set expansion nests codes or not
   * @param excludeNotForUI controls whether the value set expansion is assembled for a user
   *     interface use
   * @param excludePostCoordinated controls whether the value set expansion includes post
   *     coordinated codes
   * @param displayLanguage specifies the language to be used for description in teh expansions
   * @param exclude_system the code system or particular version of a code system to be excluded
   *     from the expansion
   * @param system_version the version to use for a system
   * @param check_system_version specifies a version to use for a system.
   * @param force_system_version specifies a version to use for a system. Overrides any specified
   *     version in the value set
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
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_EXPAND,
          IssueType.NOTSUPPORTED,
          405);
    }
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
      final List<ValueSet> vsList = findPossibleValueSets(null, null, url, version);
      if (vsList.size() == 0) {
        throw FhirUtilityR4.exception(
            "Value set " + url.getValueAsString() + " not found",
            OperationOutcome.IssueType.EXCEPTION,
            500);
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
        subsetMembers = searchService.findConcepts(terminologies, sc).getConcepts();
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
          ValueSetExpansionParameterComponent vsParameter =
              new ValueSetExpansionParameterComponent();
          vsParameter.setName("url");
          vsParameter.setValue(url);
          vsExpansion.addParameter(vsParameter);
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("version");
          vsParameter.setValue(version);
          vsExpansion.addParameter(vsParameter);
        }
      }
      vs.setExpansion(vsExpansion);
      return vs;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      e.printStackTrace();
      throw FhirUtilityR4.exception(
          "Failed to load value set", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Expand instance.
   *
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-expand.html
   * </pre>
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param url the canonical reference to the value set.
   * @param valueSet the value set
   * @param version the value set version to specify the version to be used when generating the
   *     expansion
   * @param context the context of the value set.
   * @param contextDirection the context direction, incoming or outgoing. Usually accompanied by
   *     context.
   * @param filter the text filter applied to restrict code that are returned.
   * @param date the date for which the expansion should be generated.
   * @param offset the offset for number of records.
   * @param count the count for codes that should be provided in the partial page view.
   * @param includeDesignations the include designations flag for included/excluded in value set
   *     expansions
   * @param designation the designation token that specifies the system + code that either a use or
   *     language.
   * @param includeDefinition controls whether the value set definition is included or excluded in
   *     value set expansions
   * @param activeOnly controls whether inactive concepts are included or excluded in value set
   *     expansions.
   * @param excludeNested controls whether the value set expansion nests codes or not
   * @param excludeNotForUI controls whether the value set expansion is assembled for a user
   *     interface use
   * @param excludePostCoordinated controls whether the value set expansion includes post
   *     coordinated codes
   * @param displayLanguage specifies the language to be used for description in teh expansions
   * @param exclude_system the code system or particular version of a code system to be excluded
   *     from the expansion
   * @param system_version the version to use for a system
   * @param check_system_version specifies a version to use for a system.
   * @param force_system_version specifies a version to use for a system. Overrides any specified
   *     version in the value set
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
      @ca.uhn.fhir.rest.annotation.Offset final Integer offset,
      @ca.uhn.fhir.rest.annotation.Count final Integer count,
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
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_EXPAND,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      // URL is not required because "id" is provided
      // FhirUtilityR4.required("url", url);
      FhirUtilityR4.notSupported("valueSet", null);
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
      final List<ValueSet> vsList = findPossibleValueSets(id, null, url, version);
      if (vsList.size() == 0) {
        throw FhirUtilityR4.exception(
            "Value set " + url.getValueAsString() + " not found",
            OperationOutcome.IssueType.EXCEPTION,
            500);
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
        sc.setPageSize(count != null ? count : 10);
        sc.setFromRecord(offset != null ? offset : 0);
        sc.setTerm(filter != null ? filter.getValue() : null);
        sc.setType("contains");
        sc.setTerminology(
            terminologies.stream().map(Terminology::getTerminology).collect(Collectors.toList()));
        subsetMembers = searchService.findConcepts(terminologies, sc).getConcepts();
      }
      final ValueSetExpansionComponent vsExpansion = new ValueSetExpansionComponent();
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset : 0);
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
          ValueSetExpansionParameterComponent vsParameter =
              new ValueSetExpansionParameterComponent();
          vsParameter.setName("url");
          vsParameter.setValue(url);
          vsExpansion.addParameter(vsParameter);
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("version");
          vsParameter.setValue(version);
          vsExpansion.addParameter(vsParameter);
        }
      }
      vs.setExpansion(vsExpansion);
      return vs;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception(
          "Failed to load value set", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code implicit.
   *
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-validate-code.html
   * </pre>
   *
   * @param request the request
   * @param details the details
   * @param url the value set canonical url
   * @param context the context of the value set, allows the server to resolve the value set to
   *     validate against
   * @param valueSet the value set
   * @param valueSetVersion the value set version identifier use to specify the version of the value
   *     set to be used when validating the code
   * @param code the code that is to be validated. If provided, systems or context must be provided
   * @param system the system for the code that is to be validated
   * @param systemVersion the version of the system
   * @param version the version
   * @param display the display associated with the code, if provided. If provided, a code must be
   *     provided
   * @param coding the coding to be validated
   * @param codeableConcept the codeable concept to validate
   * @param date the date that the validation should be checked.
   * @param abstractt the abstractt indicates if the concept is a logical grouping concept. If True,
   *     the validation is being performed in a context where a concept designated as 'abstract' is
   *     appropriate/allowed to be used
   * @param displayLanguage the display language for the description when validating a display
   *     property
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
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.required("code", code);
      FhirUtilityR4.mutuallyRequired("code", code, "system", system, "url", url);
      FhirUtilityR4.mutuallyRequired("system", system, "systemVersion", systemVersion);
      FhirUtilityR4.notSupported("codeableConcept", codeableConcept);
      FhirUtilityR4.notSupported("coding", coding);
      FhirUtilityR4.notSupported("context", context);
      FhirUtilityR4.notSupported("date", date);
      FhirUtilityR4.notSupported("abstract", abstractt);
      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      FhirUtilityR4.notSupported("version", version);
      FhirUtilityR4.notSupported("valueSet", valueSet);
      FhirUtilityR4.notSupported("valueSetVersion", valueSetVersion);
      final List<ValueSet> list = findPossibleValueSets(null, system, url, systemVersion);
      final Parameters params = new Parameters();

      if (list.size() > 0) {
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
        final List<Concept> conc = searchService.findConcepts(terms, sc).getConcepts();
        if (conc.size() > 0) {
          params.addParameter("display", conc.get(0).getName());
          if (display != null && !display.getValue().equals(conc.get(0).getName())) {
            params.addParameter("result", false);
            params.addParameter(
                "message",
                "The code '"
                    + code
                    + "' was found in this value set, however the display '"
                    + display
                    + "' did not match any designations.");
          } else {
            params.addParameter("result", true);
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
      throw FhirUtilityR4.exception(
          "Failed to load value set", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code instance.
   *
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-validate-code.html
   * </pre>
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param url the value set canonical url
   * @param context the context of the value set, allows the server to resolve the value set to
   *     validate against
   * @param valueSet the value set
   * @param valueSetVersion the value set version identifier use to specify the version of the value
   *     set to be used when validating the code
   * @param code the code that is to be validated. If provided, systems or context must be provided
   * @param system the system for the code that is to be validated
   * @param systemVersion the version of the system
   * @param version the version
   * @param display the display associated with the code, if provided. If provided, a code must be
   *     provided
   * @param coding the coding to be validated
   * @param codeableConcept the codeable concept to validate
   * @param date the date that the validation should be checked.
   * @param abstractt the abstractt indicates if the concept is a logical grouping concept. If True,
   *     the validation is being performed in a context where a concept designated as 'abstract' is
   *     appropriate/allowed to be used
   * @param displayLanguage the display language for the description when validating a display
   *     property
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
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.requireAtLeastOneOf(
          "code", code, "system", system, "systemVersion", systemVersion, "url", url);
      FhirUtilityR4.notSupported("codeableConcept", codeableConcept);
      FhirUtilityR4.notSupported("coding", coding);
      FhirUtilityR4.notSupported("context", context);
      FhirUtilityR4.notSupported("date", date);
      FhirUtilityR4.notSupported("abstract", abstractt);
      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      FhirUtilityR4.notSupported("version", version);
      FhirUtilityR4.notSupported("valueSet", valueSet);
      FhirUtilityR4.notSupported("valueSetVersion", valueSetVersion);
      final List<ValueSet> list = findPossibleValueSets(id, system, url, systemVersion);
      final Parameters params = new Parameters();
      if (list.size() > 0) {
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
        final List<Concept> conc = searchService.findConcepts(terms, sc).getConcepts();
        if (conc.size() > 0) {
          params.addParameter("display", conc.get(0).getName());
          if (display != null && !display.getValue().equals(conc.get(0).getName())) {
            params.addParameter("result", false);
            params.addParameter(
                "message",
                "The code '"
                    + code
                    + "' was found in this value set, however the display '"
                    + display
                    + "' did not match any designations.");
          } else {
            params.addParameter("result", true);
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
      throw FhirUtilityR4.exception(
          "Failed to load value set", OperationOutcome.IssueType.EXCEPTION, 500);
    }
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
    FhirUtilityR4.notSupportedSearchParams(request);
    final List<Terminology> terms = termUtils.getIndexedTerminologies(esQueryService);
    final List<ValueSet> list = new ArrayList<>();

    if (code == null) {
      for (final Terminology terminology : terms) {
        final ValueSet vs = FhirUtilityR4.toR4VS(terminology);
        // Skip non-matching
        if (id != null && !id.getValue().equals(vs.getId())) {
          logger.debug("  SKIP id mismatch = " + vs.getId());
          continue;
        }
        if (url != null && !url.getValue().equals(vs.getUrl())) {
          logger.debug("  SKIP url mismatch = " + vs.getUrl());
          continue;
        }
        if (system != null && !system.getValue().equals(vs.getTitle())) {
          logger.debug("  SKIP system mismatch = " + vs.getTitle());
          continue;
        }
        if (name != null && !name.getValue().equals(vs.getName())) {
          logger.debug("  SKIP name mismatch = " + vs.getName());
          continue;
        }
        if (version != null && !FhirUtility.compareString(version, vs.getVersion())) {
          logger.debug("  SKIP version mismatch = " + vs.getVersion());
          continue;
        }

        list.add(vs);
      }
    }
    final List<Concept> subsets =
        metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.ofNullable(null));
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
      final ValueSet vs = FhirUtilityR4.toR4VS(subset);
      // Skip non-matching
      if (id != null && !id.getValue().equals(vs.getId())) {
        logger.debug("  SKIP id mismatch = " + vs.getUrl());
        continue;
      }
      if (url != null && !url.getValue().equals(vs.getUrl())) {
        logger.debug("  SKIP url mismatch = " + vs.getUrl());
        continue;
      }
      if (system != null && !system.getValue().equals(vs.getTitle())) {
        logger.debug("  SKIP system mismatch = " + vs.getTitle());
        continue;
      }
      if (name != null && !name.getValue().equals(vs.getName())) {
        logger.debug("  SKIP name mismatch = " + vs.getName());
        continue;
      }
      if (code != null
          && !vs.getIdentifier().stream()
              .filter(i -> i.getValue().equals(code.getValue()))
              .findAny()
              .isPresent()) {
        logger.debug("  SKIP code mismatch = " + vs.getTitle());
        continue;
      }
      list.add(vs);
    }

    return FhirUtilityR4.makeBundle(request, list, count, offset);
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

      throw FhirUtilityR4.exception(
          "Value set not found = " + (id == null ? "null" : id.getIdPart()),
          IssueType.NOTFOUND,
          404);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR4.exception(
          "Failed to get value set", OperationOutcome.IssueType.EXCEPTION, 500);
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
  private List<ValueSet> findPossibleValueSets(
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
      final ValueSet vs = FhirUtilityR4.toR4VS(terminology);
      // Skip non-matching
      if (id != null && !id.getIdPart().equals(vs.getId())) {
        logger.debug("  SKIP id mismatch = " + vs.getId());
        continue;
      }
      if (url != null && !url.getValue().equals(vs.getUrl())) {
        logger.debug("  SKIP url mismatch = " + vs.getUrl());
        continue;
      }
      if (system != null && !system.getValue().equals(vs.getTitle())) {
        logger.debug("  SKIP system mismatch = " + vs.getTitle());
        continue;
      }
      if (version != null && !version.getValue().equals(vs.getVersion())) {
        logger.debug("  SKIP version mismatch = " + vs.getVersion());
        continue;
      }

      list.add(vs);
    }
    final List<Concept> subsets =
        metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.ofNullable(null));
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
      final ValueSet vs = FhirUtilityR4.toR4VS(subset);

      // Skip non-matching
      if (id != null && !id.getIdPart().equals(vs.getId())) {
        logger.debug("  SKIP id mismatch = " + vs.getId());
        continue;
      }
      if (url != null && !url.getValue().equals(vs.getUrl())) {
        logger.debug("  SKIP url mismatch = " + vs.getUrl());
        continue;
      }
      if (system != null && !system.getValue().equals(vs.getTitle())) {
        logger.debug("  SKIP system mismatch = " + vs.getTitle());
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
