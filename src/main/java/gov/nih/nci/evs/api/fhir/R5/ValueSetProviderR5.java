package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.service.OpenSearchService;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import gov.nih.nci.evs.api.util.FhirUtility;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.ValueSet.ConceptPropertyComponent;
import org.hl7.fhir.r5.model.ValueSet.ConceptReferenceDesignationComponent;
import org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionParameterComponent;
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
  @Autowired OpenSearchService searchService;

  /** The search service. */
  @Autowired OpensearchQueryService osQueryService;

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
   * See https://hl7.org/fhir/R5/valueset.html (find "search parameters")
   *
   * @param request the request
   * @param id the id
   * @param code the code
   * @param name the name
   * @param title the title
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
      @OptionalParam(name = "title") final StringParam title,
      @OptionalParam(name = "url") final StringParam url,
      @OptionalParam(name = "version") final StringParam version,
      @Description(shortDefinition = "Number of entries to return") @OptionalParam(name = "_count")
          final NumberParam count,
      @Description(shortDefinition = "Start offset, used when reading a next page")
          @OptionalParam(name = "_offset")
          final NumberParam offset)
      throws Exception {
    FhirUtilityR5.notSupportedSearchParams(request);
    final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);
    final List<ValueSet> list = new ArrayList<>();

    if (code == null) {
      for (final Terminology terminology : terms) {
        final ValueSet vs = FhirUtilityR5.toR5VS(terminology);
        // Skip non-matching
        if (id != null && !id.getValue().equals(vs.getId())) {
          logger.debug("  SKIP id mismatch = " + vs.getId());
          continue;
        }
        if (url != null && !url.getValue().equals(vs.getUrl())) {
          logger.debug("  SKIP url mismatch = " + vs.getUrl());
          continue;
        }
        if (title != null && !FhirUtility.compareString(title, vs.getTitle())) {
          logger.debug("  SKIP title mismatch = " + vs.getTitle());
          continue;
        }
        if (name != null && !FhirUtility.compareString(name, vs.getName())) {
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
        metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.empty());
    final Set<String> codes =
        subsets.stream()
            .flatMap(Concept::streamSelfAndChildren)
            .map(c -> c.getCode())
            .collect(Collectors.toSet());
    final List<Concept> subsetsAsConcepts =
        osQueryService.getConcepts(
            codes,
            termUtils.getIndexedTerminology("ncit", osQueryService, true),
            new IncludeParam("minimal"));

    for (final Concept subset : subsetsAsConcepts) {
      final ValueSet vs = FhirUtilityR5.toR5VS(subset);
      // Skip non-matching
      if (id != null && !id.getValue().equals(vs.getId())) {
        logger.debug("  SKIP id mismatch = " + vs.getUrl());
        continue;
      }
      if (url != null && !url.getValue().equals(vs.getUrl())) {
        logger.debug("  SKIP url mismatch = " + vs.getUrl());
        continue;
      }
      if (title != null && !FhirUtility.compareString(title, vs.getTitle())) {
        logger.debug("  SKIP title mismatch = " + vs.getTitle());
        continue;
      }
      if (name != null && !FhirUtility.compareString(name, vs.getName())) {
        logger.debug("  SKIP name mismatch = " + vs.getName());
        continue;
      }
      if (code != null
          && !vs.getIdentifier().stream().anyMatch(i -> i.getValue().equals(code.getValue()))) {
        logger.debug("  SKIP code mismatch = " + vs.getTitle());
        continue;
      }
      list.add(vs);
    }
    return FhirUtilityR5.makeBundle(request, list, count, offset);
  }

  /**
   * Expand implicit.
   *
   * See https://hl7.org/fhir/R5/valueset-operation-expand.html
   *
   * @param request the request
   * @param details the details
   * @param url a canonical reference to the value set.
   * @param version the identifier used to identify the specific version of the value set to be used
   *     to generate expansion.
   * @param filter the text filter applied to the restrict codes that are returned.
   * @param offset the offset for the records.
   * @param count the count for how many codes should be returned in partial page view.
   * @param includeDesignations the include designations
   * @param includeDefinition the include definition
   * @param activeOnly controls whether the inactive concepts are include/excluded in the expansion.
   * @param properties the properties
   * @return the value set
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_EXPAND, idempotent = true)
  public ValueSet expandImplicit(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      //      @OperationParam(name = "valueSet") final ValueSet valueSet,
      @OperationParam(name = "valueSetVersion") final StringType version,
      //      @OperationParam(name = "context") final UriType context,
      //      @OperationParam(name = "contextDirection") final CodeType contextDirection,
      @OperationParam(name = "filter") final StringType filter,
      //      @OperationParam(name = "date") final DateTimeType date,
      @OperationParam(name = "offset") final IntegerType offset,
      @OperationParam(name = "count") final IntegerType count,
      @OperationParam(name = "includeDesignations") final BooleanType includeDesignations,
      //      @OperationParam(name = "designation") final StringType designation,
      @OperationParam(name = "includeDefinition") final BooleanType includeDefinition,
      @OperationParam(name = "activeOnly") final BooleanType activeOnly,
      //      @OperationParam(name = "excludeNested") final BooleanType excludeNested,
      //      @OperationParam(name = "excludeNotForUI") final BooleanType excludeNotForUI,
      //      @OperationParam(name = "excludePostCoordinated") final BooleanType
      // excludePostCoordinated,
      //      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      //      @OperationParam(name = "exclude-system") final StringType exclude_system,
      //      @OperationParam(name = "system-version") final StringType system_version,
      //      @OperationParam(name = "check-system-version") final StringType check_system_version,
      //      @OperationParam(name = "force-system-version") final StringType force_system_version,
      @OperationParam(name = "property") final List<StringType> properties)
      throws Exception {
    // check if request is a post, throw exception as we don't support post
    // calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_EXPAND,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.required("url", url);
      for (final String param :
          new String[] {
            "valueSet",
            "context",
            "contextDirection",
            "date",
            "designation",
            "excludeNested",
            "excludeNotForUI",
            "excludePostCoordinated",
            "displayLanguage",
            "exclude-system",
            "system-version",
            "check-system-version",
            "force-system-version",
            "_count",
            "_offset"
          }) {
        FhirUtilityR5.notSupported(request, param);
      }

      final List<ValueSet> vsList = findPossibleValueSets(null, null, url, version);
      if (vsList.isEmpty()) {
        throw FhirUtilityR5.exception(
            "Value set " + url.asStringValue() + " not found", IssueType.EXCEPTION, 500);
      }

      // Convert list of StringType properties to list of String property names
      // if provided
      List<String> propertyNames = null;
      if (properties != null && !properties.isEmpty()) {
        propertyNames = properties.stream().map(StringType::getValue).collect(Collectors.toList());
      }
      // If properties are indicated, retrieve the concept with all potentially
      // needed info
      IncludeParam includeParam = new IncludeParam("minimal");
      List<String> includeList = new ArrayList<>();
      if (propertyNames != null && !propertyNames.isEmpty()) {

        includeList.add("properties");
      }
      if (includeDefinition != null && includeDefinition.getValue().booleanValue() == true) {
        includeList.add("definitions");
      }
      if (includeDesignations != null && includeDesignations.getValue().booleanValue() == true) {
        includeList.add("synonyms");
      }
      if (includeList.size() >= 1) {
        includeList.add("parents");
        includeList.add("children");
      } else {
        includeList.add("minimal");
      }
      includeParam = new IncludeParam(String.join(",", includeList));

      final ValueSet vs = vsList.get(0);
      List<Concept> subsetMembers = new ArrayList<Concept>();
      if (url.getValue().contains("?fhir_vs=")) {
        final List<Association> invAssoc =
            osQueryService
                .getConcept(
                    vs.getIdentifier().get(0).getValue(),
                    termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true),
                    new IncludeParam("inverseAssociations"))
                .get()
                .getInverseAssociations();
        for (final Association assn : invAssoc) {
          final Concept member =
              osQueryService
                  .getConcept(
                      assn.getRelatedCode(),
                      termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true),
                      includeParam)
                  .orElse(null);
          if (member != null) {
            subsetMembers.add(member);
          }
        }
      } else {
        final List<Terminology> terminologies = new ArrayList<>();
        terminologies.add(termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true));
        final SearchCriteria sc = new SearchCriteria();
        sc.setPageSize(count != null ? count.getValue() : 10);
        sc.setFromRecord(offset != null ? offset.getValue() : 0);
        sc.setTerm(filter != null ? filter.getValue() : null);
        sc.setType("contains");
        sc.setTerminology(
            terminologies.stream().map(Terminology::getTerminology).collect(Collectors.toList()));
        // Add property names to search criteria if indicated
        if (includeList.size() > 1) {
          sc.setInclude(String.join(",", includeList));
        }
        subsetMembers = searchService.findConcepts(terminologies, sc).getConcepts();
      }
      final ValueSet.ValueSetExpansionComponent vsExpansion =
          new ValueSet.ValueSetExpansionComponent();
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset.getValue() : 0);
      vsExpansion.setTotal(subsetMembers.size());
      if (!subsetMembers.isEmpty()) {
        for (final Concept member : subsetMembers) {
          if (activeOnly != null && activeOnly.getValue() && !member.getActive()) {
            continue;
          }
          final ValueSet.ValueSetExpansionContainsComponent vsContains =
              new ValueSet.ValueSetExpansionContainsComponent();
          vsContains.setSystem(vs.getUrl());
          vsContains.setCode(member.getCode());
          vsContains.setDisplay(member.getName());

          // Add properties to the contains component if they were requested
          if (propertyNames != null && !propertyNames.isEmpty()) {
            for (String propertyName : propertyNames) {
              addConceptProperty(vsContains, member, propertyName);
            }
          }
          // Add definitions to the contains component if they were requested
          if (includeDefinition != null
              && includeDefinition.booleanValue()
              && member.getDefinitions() != null) {
            addConceptProperty(vsContains, member, "definition");
          }
          // Add synonyms to the contains component if they were requested
          if (includeDesignations != null
              && includeDesignations.booleanValue()
              && member.getSynonyms() != null) {
            for (Synonym term : member.getSynonyms()) {
              ConceptReferenceDesignationComponent designation =
                  new ConceptReferenceDesignationComponent()
                      .setLanguage("en")
                      .setUse(new Coding(term.getUri(), term.getTermType(), term.getName()))
                      .setValue(term.getName());

              vsContains.addDesignation(designation);
            }
          }
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
   * See https://hl7.org/fhir/R5/valueset-operation-expand.html
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param url a canonical reference to the value set.
   * @param version the identifier used to identify the specific version of the value set to be used
   *     to generate expansion.
   * @param filter the text filter applied to the restrict codes that are returned.
   * @param offset the offset for the records.
   * @param count the count for how many codes should be returned in partial page view.
   * @param includeDesignations the include designations
   * @param includeDefinition the include definition
   * @param activeOnly controls whether the inactive concepts are include/excluded in the expansion.
   * @param properties the properties
   * @return the value set
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_EXPAND, idempotent = true)
  public ValueSet expandInstance(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "url") final UriType url,
      //      @OperationParam(name = "valueSet") final ValueSet valueSet,
      @OperationParam(name = "valueSetVersion") final StringType version,
      //      @OperationParam(name = "context") final UriType context,
      //      @OperationParam(name = "contextDirection") final CodeType contextDirection,
      @OperationParam(name = "filter") final StringType filter,
      //      @OperationParam(name = "date") final DateTimeType date,
      @OperationParam(name = "offset") final IntegerType offset,
      @OperationParam(name = "count") final IntegerType count,
      @OperationParam(name = "includeDesignations") final BooleanType includeDesignations,
      //      @OperationParam(name = "designation") final StringType designation,
      @OperationParam(name = "includeDefinition") final BooleanType includeDefinition,
      @OperationParam(name = "activeOnly") final BooleanType activeOnly,
      //      @OperationParam(name = "excludeNested") final BooleanType excludeNested,
      //      @OperationParam(name = "excludeNotForUI") final BooleanType excludeNotForUI,
      //      @OperationParam(name = "excludePostCoordinated") final BooleanType
      // excludePostCoordinated,
      //      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      //      @OperationParam(name = "exclude-system") final StringType exclude_system,
      //      @OperationParam(name = "system-version") final StringType system_version,
      //      @OperationParam(name = "check-system-version") final StringType check_system_version,
      //      @OperationParam(name = "force-system-version") final StringType force_system_version,
      @OperationParam(name = "property") final List<StringType> properties)
      throws Exception {
    // check if request is a post, throw exception as we don't support post
    // calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_EXPAND,
          IssueType.NOTSUPPORTED,
          405);
    }

    for (final String param :
        new String[] {
          "valueSet",
          "context",
          "contextDirection",
          "date",
          "designation",
          "excludeNested",
          "excludeNotForUI",
          "excludePostCoordinated",
          "displayLanguage",
          "exclude_system",
          "system_version",
          "check_system_version",
          "force_system_version",
          "_count",
          "_offset"
        }) {
      FhirUtilityR5.notSupported(request, param);
    }
    if (Collections.list(request.getParameterNames()).stream()
            .filter(k -> k.startsWith("_has"))
            .count()
        > 0) {
      FhirUtilityR5.notSupported(request, "_has");
    }

    try {

      final List<ValueSet> vsList = findPossibleValueSets(id, null, null, version);
      if (vsList.isEmpty()) {
        throw FhirUtilityR5.exception("Value set " + id + " not found", IssueType.EXCEPTION, 500);
      }

      // Convert list of StringType properties to list of String property names
      // if provided
      List<String> propertyNames = null;
      if (properties != null && !properties.isEmpty()) {
        propertyNames = properties.stream().map(StringType::getValue).collect(Collectors.toList());
      }
      // If properties are indicated, retrieve the concept with all potentially
      // needed info
      IncludeParam includeParam = new IncludeParam("minimal");
      List<String> includeList = new ArrayList<>();
      if (propertyNames != null && !propertyNames.isEmpty()) {
        includeList.add("properties");
      }
      if (includeDefinition != null && includeDefinition.getValue().booleanValue() == true) {
        includeList.add("definitions");
      }
      if (includeDesignations != null && includeDesignations.getValue().booleanValue() == true) {
        includeList.add("synonyms");
      }
      if (includeList.size() >= 1) {
        includeList.add("parents");
        includeList.add("children");
      } else {
        includeList.add("minimal");
      }
      includeParam = new IncludeParam(String.join(",", includeList));

      final ValueSet vs = vsList.get(0);
      List<Concept> subsetMembers = new ArrayList<Concept>();
      if ((url != null) && !vs.getUrl().equals(url.getValue())) {
        throw FhirUtilityR5.exception(
            "Supplied url "
                + url.getValue()
                + " doesn't match the ValueSet retrieved by the id "
                + id
                + " "
                + vs.getUrl(),
            OperationOutcome.IssueType.EXCEPTION,
            400);
      }

      if (vs.getUrl() != null && vs.getUrl().contains("?fhir_vs=")) {
        final List<Association> invAssoc =
            osQueryService
                .getConcept(
                    vs.getIdentifier().get(0).getValue(),
                    termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true),
                    new IncludeParam("inverseAssociations"))
                .get()
                .getInverseAssociations();
        for (final Association assn : invAssoc) {
          final Concept member =
              osQueryService
                  .getConcept(
                      assn.getRelatedCode(),
                      termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true),
                      includeParam)
                  .orElse(null);
          if (member != null) {
            subsetMembers.add(member);
          }
        }
      } else {
        final List<Terminology> terminologies = new ArrayList<>();
        terminologies.add(termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true));
        final SearchCriteria sc = new SearchCriteria();
        sc.setPageSize(count != null ? count.getValue() : 10);
        sc.setFromRecord(offset != null ? offset.getValue() : 0);
        sc.setTerm(filter != null ? filter.getValue() : null);
        sc.setType("contains");
        sc.setTerminology(
            terminologies.stream().map(Terminology::getTerminology).collect(Collectors.toList()));
        // Add property names to search criteria if indicated
        if (propertyNames != null && !propertyNames.isEmpty()) {
          sc.setInclude(String.join(",", includeList));
        }
        subsetMembers = searchService.findConcepts(terminologies, sc).getConcepts();
      }
      final ValueSet.ValueSetExpansionComponent vsExpansion =
          new ValueSet.ValueSetExpansionComponent();
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset.getValue() : 0);
      vsExpansion.setTotal(subsetMembers.size());
      if (!subsetMembers.isEmpty()) {
        for (final Concept member : subsetMembers) {
          if (activeOnly != null && activeOnly.getValue() && !member.getActive()) {
            continue;
          }
          final ValueSet.ValueSetExpansionContainsComponent vsContains =
              new ValueSet.ValueSetExpansionContainsComponent();
          vsContains.setSystem(vs.getUrl());
          vsContains.setCode(member.getCode());
          vsContains.setDisplay(member.getName());

          // Add properties to the contains component if they were requested
          if (propertyNames != null && !propertyNames.isEmpty()) {
            for (String propertyName : propertyNames) {
              addConceptProperty(vsContains, member, propertyName);
            }
          }
          // Add definitions to the contains component if they were requested
          if (includeDefinition != null
              && includeDefinition.booleanValue()
              && member.getDefinitions() != null) {
            addConceptProperty(vsContains, member, "definition");
          }
          // Add synonyms to the contains component if they were requested
          if (includeDesignations != null
              && includeDesignations.booleanValue()
              && member.getSynonyms() != null) {
            for (Synonym term : member.getSynonyms()) {
              ConceptReferenceDesignationComponent designation =
                  new ConceptReferenceDesignationComponent()
                      .setLanguage("en")
                      .setUse(new Coding(term.getUri(), term.getTermType(), term.getName()))
                      .setValue(term.getName());

              vsContains.addDesignation(designation);
            }
          }
          vsExpansion.addContains(vsContains);
        }
      }
      // Add expansion parameters
      ValueSetExpansionParameterComponent vsParameter = new ValueSetExpansionParameterComponent();
      vsParameter.setName("url");
      vsParameter.setValue(url);
      vsExpansion.addParameter(vsParameter);

      vsParameter = new ValueSetExpansionParameterComponent();
      vsParameter.setName("version");
      vsParameter.setValue(version);
      vsExpansion.addParameter(vsParameter);

      // Add property parameter if properties were specified
      if (properties != null && !properties.isEmpty()) {
        for (StringType property : properties) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("property");
          vsParameter.setValue(property);
          vsExpansion.addParameter(vsParameter);
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
   * See https://hl7.org/fhir/R5/valueset-operation-validate-code.html
   *
   * @param request the request
   * @param details the details
   * @param url value set canonical URL.
   * @param code the code that is to be validated. If provided, a system or context must be
   *     provided.
   * @param system the system for the code that is to be validated.
   * @param systemVersion the version of the system, if one was provided.
   * @param display the display associated with the code. If provided, a code must be provided.
   * @param coding the coding
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeImplicit(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      //      @OperationParam(name = "context") final UriType context,
      //      @OperationParam(name = "valueSet") final ValueSet valueSet,
      //      @OperationParam(name = "valueSetVersion") final StringType valueSetVersion,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "systemVersion") final StringType systemVersion,
      //      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding
      //      @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      //      @OperationParam(name = "date") final DateTimeType date,
      //      @OperationParam(name = "abstract") final BooleanType abstractt,
      //      @OperationParam(name = "displayLanguage") final StringType displayLanguage
      ) throws Exception {
    // check if request is a post, throw exception as we don't support post
    // calls except for coding parameter
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyRequired("code", code, "system", system, "url", url);
      FhirUtilityR5.mutuallyExclusive("code", code, "coding", coding);
      FhirUtilityR5.mutuallyRequired("system", system, "systemVersion", systemVersion);
      FhirUtilityR5.mutuallyRequired("display", display, "code", code);

      // TODO: not sure that "version" should be in this list
      for (final String param :
          new String[] {
            "context",
            "date",
            "abstract",
            "displayLanguage",
            "version",
            "valueSet",
            "valueSetVersion"
          }) {
        FhirUtilityR5.notSupported(request, param);
      }

      UriType urlToLookup = null;
      if (url != null) {
        urlToLookup = url;
      }
      if (coding != null) {
        urlToLookup = coding.getSystemElement();
      }

      final List<ValueSet> list = findPossibleValueSets(null, system, urlToLookup, systemVersion);
      final Parameters params = new Parameters();

      if (!list.isEmpty()) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        final ValueSet vs = list.get(0);
        final SearchCriteria sc = new SearchCriteria();
        sc.setTerm(codeToLookup);
        sc.setInclude("minimal");
        sc.setType("exact");
        sc.setFromRecord(0);
        sc.setPageSize(1);
        if (vs.getIdentifier() != null && !vs.getIdentifier().isEmpty()) {
          sc.setSubset(Arrays.asList(vs.getIdentifier().get(0).getValue()));
        }
        final Terminology term =
            termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true);
        sc.setTerminology(Arrays.asList(vs.getTitle()));
        sc.validate(term, metadataService);
        final List<Terminology> terms = Arrays.asList(term);
        final List<Concept> conc = searchService.findConcepts(terms, sc).getConcepts();
        if (!conc.isEmpty()) {
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
          params.addParameter("message", "The code '" + codeToLookup + "' was not found.");
        }
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "Unable to find matching value set");
        params.addParameter("url", (url == null ? new UriType("<null>") : url));
        //        params.addParameter("version", version);
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
   * See https://hl7.org/fhir/R5/valueset-operation-validate-code.html
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param url value set canonical URL.
   * @param code the code that is to be validated. If provided, a system or context must be
   *     provided.
   * @param system the system for the code that is to be validated.
   * @param systemVersion the version of the system, if one was provided.
   * @param display the display associated with the code. If provided, a code must be provided.
   * @param coding the coding
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeInstance(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "url") final UriType url,
      //      @OperationParam(name = "context") final UriType context,
      //      @OperationParam(name = "valueSet") final ValueSet valueSet,
      //      @OperationParam(name = "valueSetVersion") final StringType valueSetVersion,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "systemVersion") final StringType systemVersion,
      //      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding
      //      @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      //      @OperationParam(name = "date") final DateTimeType date,
      //      @OperationParam(name = "abstract") final BooleanType abstractt,
      //      @OperationParam(name = "displayLanguage") final StringType displayLanguage
      ) throws Exception {
    // check if request is a post, throw exception as we don't support post
    // calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.requireAtLeastOneOf("id", id, "code", code, "system", system, "coding", coding);
      FhirUtilityR5.mutuallyExclusive("code", code, "coding", coding);
      FhirUtilityR5.mutuallyRequired("display", display, "code", code);

      // TODO: not sure that "version" should be in this list
      for (final String param :
          new String[] {
            "context",
            "date",
            "abstract",
            "displayLanguage",
            "version",
            "valueSet",
            "valueSetVersion"
          }) {
        FhirUtilityR5.notSupported(request, param);
      }

      UriType urlToLookup = null;
      if (url != null) {
        urlToLookup = url;
      }
      if (coding != null) {
        urlToLookup = coding.getSystemElement();
      }

      final List<ValueSet> list = findPossibleValueSets(id, system, null, systemVersion);
      final Parameters params = new Parameters();
      if (!list.isEmpty()) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        final ValueSet vs = list.get(0);
        if ((urlToLookup != null) && !vs.getUrl().equals(urlToLookup.getValue())) {
          throw FhirUtilityR5.exception(
              "Supplied url "
                  + urlToLookup
                  + " doesn't match the ValueSet retrieved by the id "
                  + id
                  + " "
                  + vs.getUrl(),
              OperationOutcome.IssueType.EXCEPTION,
              400);
        }
        final SearchCriteria sc = new SearchCriteria();
        sc.setTerm(codeToLookup);
        sc.setInclude("minimal");
        sc.setType("exact");
        if (vs.getIdentifier() != null && !vs.getIdentifier().isEmpty()) {
          sc.setSubset(Arrays.asList(vs.getIdentifier().get(0).getValue()));
        }

        final Terminology term =
            termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true);
        sc.validate(term, metadataService);
        final List<Terminology> terms = Arrays.asList(term);
        final List<Concept> conc = searchService.findConcepts(terms, sc).getConcepts();

        if (!conc.isEmpty()) {
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
          params.addParameter("message", "The code '" + codeToLookup + "' was not found.");
        }
      } else {
        params.addParameter("result", false);
        params.addParameter("message", "Unable to find matching value set");
        params.addParameter("url", (url == null ? new UriType("<null>") : url));
        //        params.addParameter("version", version);
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
   * See https://hl7.org/fhir/R5/valueset.html
   *
   * @param id the id
   * @return the value set
   * @throws Exception the exception
   */
  @Read
  public ValueSet getValueSet(@IdParam final IdType id) throws Exception {
    try {
      if (id.hasVersionIdPart()) {
        // If someone somehow passes a versioned ID to read, delegate to vread
        return vread(id);
      }
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

    final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);
    final List<ValueSet> list = new ArrayList<ValueSet>();

    for (final Terminology terminology : terms) {
      final ValueSet vs = FhirUtilityR5.toR5VS(terminology);
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
        metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.empty());
    final Set<String> codes =
        subsets.stream()
            .flatMap(Concept::streamSelfAndChildren)
            .map(c -> c.getCode())
            .collect(Collectors.toSet());
    final List<Concept> subsetsAsConcepts =
        osQueryService.getConcepts(
            codes,
            termUtils.getIndexedTerminology("ncit", osQueryService, true),
            new IncludeParam("minimal"));

    for (final Concept subset : subsetsAsConcepts) {
      final ValueSet vs = FhirUtilityR5.toR5VS(subset);
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

  /**
   * Helper method to extract a property value from a concept.
   *
   * @param vsContains the vs contains
   * @param concept The concept
   * @param propertyName The name of the property to retrieve
   * @return The property value, or null if not found
   */
  private void addConceptProperty(
      ValueSetExpansionContainsComponent vsContains, Concept concept, String propertyName) {

    if (propertyName.equals("active")) {
      vsContains.addProperty(
          new ConceptPropertyComponent()
              .setCode("active")
              .setValue(new BooleanType(concept.getActive())));
    } else if (propertyName.contains("parent")) {
      for (final Concept parent : concept.getParents()) {
        vsContains.addProperty(
            new ConceptPropertyComponent()
                .setCode("parent")
                .setValue(new Coding().setCode(parent.getCode())));
      }
    } else if (propertyName.contains("child")) {
      for (final Concept child : concept.getChildren()) {
        vsContains.addProperty(
            new ConceptPropertyComponent()
                .setCode("child")
                .setValue(new Coding().setCode(child.getCode())));
      }
    } else if (propertyName.contains("definition")) {
      for (final Definition def : concept.getDefinitions()) {
        vsContains.addProperty(
            new ConceptPropertyComponent()
                .setCode("definition")
                .setValue(new StringType(def.getDefinition())));
      }
    } else if (concept.getProperties().stream().anyMatch(p -> p.getType().equals(propertyName))) {
      concept.getProperties().stream()
          .filter(p -> p.getType().equals(propertyName))
          .forEach(
              p -> {
                vsContains.addProperty(
                    new ConceptPropertyComponent()
                        .setCode(propertyName)
                        .setValue(new StringType(p.getValue().toString())));
              });
    } else {
      // if (!notFoundSeen.contains(property)) {
      logger.warn("Requested property not found = " + propertyName);
      // notFoundSeen.add(property);
      // }
    }
  }

  @History(type = ValueSet.class)
  public List<ValueSet> getValueSetHistory(@IdParam IdType id) {
    List<ValueSet> history = new ArrayList<>();
    try {
      final List<ValueSet> candidates = findPossibleValueSets(id, null, null, null);
      for (final ValueSet cs : candidates) {
        if (id.getIdPart().equals(cs.getId())) {
          history.add(cs);
        }
      }
      if (history.isEmpty()) {
        throw FhirUtilityR5.exception(
            "Value set not found = " + (id == null ? "null" : id.getIdPart()),
            IssueType.NOTFOUND,
            404);
      }
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR5.exception("Failed to get value set", IssueType.EXCEPTION, 500);
    }

    // Make sure each ValueSet has proper metadata for history
    for (ValueSet cs : history) {
      if (cs.getMeta() == null) {
        cs.setMeta(new Meta());
      }
      if (cs.getMeta().getVersionId() == null) {
        cs.getMeta().setVersionId("1"); // Set appropriate version
      }
      if (cs.getMeta().getLastUpdated() == null) {
        cs.getMeta().setLastUpdated(new Date());
      }
    }

    return history;
  }

  @Read(version = true)
  public ValueSet vread(@IdParam IdType versionedId) {
    String resourceId = versionedId.getIdPart();
    String versionId = versionedId.getVersionIdPart(); // "1"

    logger.info("Looking for resource: {} version: {}", resourceId, versionId);

    try {
      // If no version is specified in a vread call, this shouldn't happen
      // but if it does, delegate to regular read
      if (!versionedId.hasVersionIdPart()) {
        logger.warn("VRead called without version ID, delegating to regular read");
        return getValueSet(new org.hl7.fhir.r5.model.IdType(versionedId.getIdPart()));
      }

      final List<ValueSet> candidates = findPossibleValueSets(versionedId, null, null, null);
      logger.info("Found {} candidates", candidates.size());

      for (final ValueSet cs : candidates) {
        String csId = cs.getId();
        String csVersionId = cs.getMeta() != null ? cs.getMeta().getVersionId() : null;

        logger.info("Checking candidate: id={}, versionId={}", csId, csVersionId);

        if (resourceId.equals(csId)) {
          // If the ValueSet doesn't have a version ID, treat it as version "1"
          String effectiveVersionId = (csVersionId != null) ? csVersionId : "1";

          if (versionId.equals(effectiveVersionId)) {
            // Make sure the returned ValueSet has the version ID set
            if (cs.getMeta() == null) {
              cs.setMeta(new Meta());
            }
            cs.getMeta().setVersionId("1");
            cs.getMeta().setLastUpdated(new Date()); // Optional: set timestamp

            logger.info("Found matching version!");
            return cs;
          }
        }
      }

      throw FhirUtilityR5.exception(
          "Value set version not found: " + resourceId + " version " + versionId,
          IssueType.NOTFOUND,
          404);
    } catch (final FHIRServerResponseException e) {
      throw e; // Re-throw FHIR exceptions as-is
    } catch (final Exception e) {
      logger.error("Unexpected exception in vread", e);
      throw FhirUtilityR5.exception("Failed to get value set version", IssueType.EXCEPTION, 500);
    }
  }
}
