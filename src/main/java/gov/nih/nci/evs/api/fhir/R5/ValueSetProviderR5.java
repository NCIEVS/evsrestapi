package gov.nih.nci.evs.api.fhir.R5;

import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ConceptPropertyComponent;
import org.hl7.fhir.r5.model.ValueSet.ConceptReferenceDesignationComponent;
import org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
   * <p>See https://hl7.org/fhir/R5/valueset.html (find "search parameters")
   *
   * @param request the request
   * @param id the id
   * @param date the date
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
      @OptionalParam(name = "date") final DateRangeParam date,
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
        if (date != null && !FhirUtility.compareDateRange(date, vs.getDate())) {
          logger.debug("  SKIP date mismatch = " + vs.getDate());
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
    final List<Concept> subsets = getNcitSubsets();
    final Set<Concept> subsetsAsConcepts =
        subsets.stream().flatMap(Concept::streamSelfAndChildren).collect(Collectors.toSet());

    for (final Concept subset : subsetsAsConcepts) {
      final ValueSet vs = FhirUtilityR5.toR5VS(subset);
      // Skip non-matching
      if (id != null && !id.getValue().equals(vs.getId())) {
        logger.debug("  SKIP id mismatch = " + vs.getUrl());
        continue;
      }
      if (date != null && !FhirUtility.compareDateRange(date, vs.getDate())) {
        logger.debug("  SKIP date mismatch = " + vs.getDate());
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
   * Gets the ncit subsets.
   *
   * @return the ncit subsets
   * @throws Exception the exception
   */
  @Cacheable("ncitsubsets")
  public List<Concept> getNcitSubsets() throws Exception {
    return metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.empty());
  }

  // Helper methods to extract parameters reliably
  private String getParameterValue(
      StringParam param, HttpServletRequest request, String paramName) {
    if (param != null && param.getValue() != null) {
      return param.getValue();
    }
    return request.getParameter(paramName);
  }

  private boolean getBooleanParameterValue(
      BooleanType param, HttpServletRequest request, String paramName, boolean defaultValue) {
    if (param != null && param.getValue() != null) {
      return param.getValue();
    }
    String requestValue = request.getParameter(paramName);
    return requestValue != null ? "true".equalsIgnoreCase(requestValue) : defaultValue;
  }

  private int getIntParameterValue(
      IntegerType param, HttpServletRequest request, String paramName, int defaultValue) {
    if (param != null && param.getValue() != null) {
      return param.getValue();
    }
    String requestValue = request.getParameter(paramName);
    return requestValue != null ? Integer.parseInt(requestValue) : defaultValue;
  }

  /**
   * Expand implicit.
   *
   * <p>See https://hl7.org/fhir/R5/valueset-operation-expand.html
   *
   * @param request the request
   * @param details the details
   * @param url a canonical reference to the value set.
   * @param valueSet the value set
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
  @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
  public ValueSet expandImplicit(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @OperationParam(name = "valueSet") final ValueSet valueSet,
      //@ResourceParam final ValueSet valueSet,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "valueSetVersion") final StringType version,
      // @OperationParam(name = "context") final UriType context,
      // @OperationParam(name = "contextDirection") final CodeType contextDirection,
      @OperationParam(name = "filter") final StringParam filter,
      // @OperationParam(name = "date") final DateTimeType date,
      @OperationParam(name = "offset") final IntegerType offset,
      @OperationParam(name = "count") final IntegerType count,
      @OperationParam(name = "includeDesignations") final BooleanType includeDesignations,
      // @OperationParam(name = "designation") final StringType designation,
      @OperationParam(name = "includeDefinition") final BooleanType includeDefinition,
      @OperationParam(name = "activeOnly") final BooleanType activeOnly,
      // @OperationParam(name = "excludeNested") final BooleanType excludeNested,
      // @OperationParam(name = "excludeNotForUI") final BooleanType excludeNotForUI,
      // @OperationParam(name = "excludePostCoordinated") final BooleanType
      // excludePostCoordinated,
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      // @OperationParam(name = "exclude-system") final StringType exclude_system,
      // @OperationParam(name = "system-version") final StringType system_version,
      // @OperationParam(name = "check-system-version") final StringType check_system_version,
      // @OperationParam(name = "force-system-version") final StringType force_system_version,
      @OperationParam(name = "property") final List<StringType> properties)
      throws Exception {
    // Extract actual values - use @OperationParam if available, fallback to manual extraction
    String filterValue = getParameterValue(filter, request, "filter");
    boolean activeOnlyValue = getBooleanParameterValue(activeOnly, request, "activeOnly", false);
    boolean includeDesignationsValue =
        getBooleanParameterValue(includeDesignations, request, "includeDesignations", false);
    boolean includeDefinitionValue =
        getBooleanParameterValue(includeDefinition, request, "includeDefinition", false);
    int countValue = getIntParameterValue(count, request, "count", 1000);
    int offsetValue = getIntParameterValue(offset, request, "offset", 0);

    FhirUtilityR5.mutuallyExclusive("url", url, "valueSet", valueSet);

    if (valueSet != null) {
      return expandValueSet(
          request,
          valueSet,
          version,
          filterValue,
          countValue,
          offsetValue,
          includeDesignationsValue,
          includeDefinitionValue,
          activeOnlyValue);
    }

    // TODO add more test cases for exclude, after adding filter is-a
    // TODO add remainder of parameters to expandValueSet
    // TODO add include.version processing (use latest if not specified)

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
      final ValueSet.ValueSetExpansionComponent vsExpansion =
          new ValueSet.ValueSetExpansionComponent();
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
        vsExpansion.setTotal(subsetMembers.size());
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
        ConceptResultList subsetMembersList = searchService.findConcepts(terminologies, sc);
        subsetMembers = subsetMembersList.getConcepts();
        vsExpansion.setTotal(Math.toIntExact(subsetMembersList.getTotal()));
      }

      ValueSetExpansionParameterComponent vsParameter;
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset.getValue() : 0);
      if (!subsetMembers.isEmpty()) {
        for (final Concept member : subsetMembers) {
          if (activeOnly != null && activeOnly.getValue() && !member.getActive()) {
            continue;
          }
          final ValueSetExpansionContainsComponent vsContains =
              new ValueSetExpansionContainsComponent();
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

        if (filter != null) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("filter");
          vsParameter.setValue(new StringType(filter.getValue()));
          vsExpansion.addParameter(vsParameter);
        }

        if (count != null) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("count");
          vsParameter.setValue(count);
          vsExpansion.addParameter(vsParameter);
        }

        if (includeDefinition != null) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("includeDefinition");
          vsParameter.setValue(includeDefinition);
          vsExpansion.addParameter(vsParameter);
        }

        if (includeDesignations != null) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("includeDesignations");
          vsParameter.setValue(includeDesignations);
          vsExpansion.addParameter(vsParameter);
        }

        if (activeOnly != null) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("activeOnly");
          vsParameter.setValue(activeOnly);
          vsExpansion.addParameter(vsParameter);
        }

        if (propertyNames != null && !propertyNames.isEmpty()) {
          for (String propertyName : propertyNames) {

            vsParameter = new ValueSetExpansionParameterComponent();
            vsParameter.setName("property");
            vsParameter.setValue(new StringType(propertyName));
            vsExpansion.addParameter(vsParameter);
          }
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
   * <p>See https://hl7.org/fhir/R5/valueset-operation-expand.html
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
      // @OperationParam(name = "valueSet") final ValueSet valueSet,
      @OperationParam(name = "valueSetVersion") final StringType version,
      // @OperationParam(name = "context") final UriType context,
      // @OperationParam(name = "contextDirection") final CodeType contextDirection,
      @OperationParam(name = "filter") final StringType filter,
      // @OperationParam(name = "date") final DateTimeType date,
      @OperationParam(name = "offset") final IntegerType offset,
      @OperationParam(name = "count") final IntegerType count,
      @OperationParam(name = "includeDesignations") final BooleanType includeDesignations,
      // @OperationParam(name = "designation") final StringType designation,
      @OperationParam(name = "includeDefinition") final BooleanType includeDefinition,
      @OperationParam(name = "activeOnly") final BooleanType activeOnly,
      // @OperationParam(name = "excludeNested") final BooleanType excludeNested,
      // @OperationParam(name = "excludeNotForUI") final BooleanType excludeNotForUI,
      // @OperationParam(name = "excludePostCoordinated") final BooleanType
      // excludePostCoordinated,
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      // @OperationParam(name = "exclude-system") final StringType exclude_system,
      // @OperationParam(name = "system-version") final StringType system_version,
      // @OperationParam(name = "check-system-version") final StringType check_system_version,
      // @OperationParam(name = "force-system-version") final StringType force_system_version,
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
      final ValueSet.ValueSetExpansionComponent vsExpansion =
          new ValueSet.ValueSetExpansionComponent();
      List<Concept> subsetMembers = new ArrayList<Concept>();
      if ((url != null) && !vs.getUrl().equals(url.getValue())) {
        throw FhirUtilityR5.exception(
            "Supplied url "
                + url.getValue()
                + " doesn't match the ValueSet retrieved by the id "
                + id
                + " "
                + vs.getUrl(),
            IssueType.EXCEPTION,
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
        vsExpansion.setTotal(subsetMembers.size());
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
        ConceptResultList subsetMembersList = searchService.findConcepts(terminologies, sc);
        subsetMembers = subsetMembersList.getConcepts();
        vsExpansion.setTotal(Math.toIntExact(subsetMembersList.getTotal()));
      }
      ValueSetExpansionParameterComponent vsParameter;
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset.getValue() : 0);
      if (!subsetMembers.isEmpty()) {
        for (final Concept member : subsetMembers) {
          if (activeOnly != null && activeOnly.getValue() && !member.getActive()) {
            continue;
          }
          final ValueSetExpansionContainsComponent vsContains =
              new ValueSetExpansionContainsComponent();
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

      if (filter != null) {
        vsParameter = new ValueSetExpansionParameterComponent();
        vsParameter.setName("filter");
        vsParameter.setValue(filter);
        vsExpansion.addParameter(vsParameter);
      }

      if (count != null) {
        vsParameter = new ValueSetExpansionParameterComponent();
        vsParameter.setName("count");
        vsParameter.setValue(count);
        vsExpansion.addParameter(vsParameter);
      }

      if (includeDefinition != null) {
        vsParameter = new ValueSetExpansionParameterComponent();
        vsParameter.setName("includeDefinition");
        vsParameter.setValue(includeDefinition);
        vsExpansion.addParameter(vsParameter);
      }

      if (includeDesignations != null) {
        vsParameter = new ValueSetExpansionParameterComponent();
        vsParameter.setName("includeDesignations");
        vsParameter.setValue(includeDesignations);
        vsExpansion.addParameter(vsParameter);
      }

      if (activeOnly != null) {
        vsParameter = new ValueSetExpansionParameterComponent();
        vsParameter.setName("activeOnly");
        vsParameter.setValue(activeOnly);
        vsExpansion.addParameter(vsParameter);
      }

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
   * <p>See https://hl7.org/fhir/R5/valueset-operation-validate-code.html
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
      // @OperationParam(name = "context") final UriType context,
      // @OperationParam(name = "valueSet") final ValueSet valueSet,
      // @OperationParam(name = "valueSetVersion") final StringType valueSetVersion,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "systemVersion") final StringType systemVersion,
      // @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding
      // @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      // @OperationParam(name = "date") final DateTimeType date,
      // @OperationParam(name = "abstract") final BooleanType abstractt,
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage
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
        // params.addParameter("version", version);
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
   * <p>See https://hl7.org/fhir/R5/valueset-operation-validate-code.html
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
      // @OperationParam(name = "context") final UriType context,
      // @OperationParam(name = "valueSet") final ValueSet valueSet,
      // @OperationParam(name = "valueSetVersion") final StringType valueSetVersion,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "systemVersion") final StringType systemVersion,
      // @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding
      // @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      // @OperationParam(name = "date") final DateTimeType date,
      // @OperationParam(name = "abstract") final BooleanType abstractt,
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage
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
              IssueType.EXCEPTION,
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
        // params.addParameter("version", version);
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
   * <p>See https://hl7.org/fhir/R5/valueset.html
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
          "Value set not found = " + (id.getIdPart()), IssueType.NOTFOUND, 404);

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
    final List<Concept> subsets = getNcitSubsets();
    final Set<Concept> subsetsAsConcepts =
        subsets.stream().flatMap(Concept::streamSelfAndChildren).collect(Collectors.toSet());

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

  /**
   * Gets the value set history.
   *
   * @param id the id
   * @return the value set history
   */
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

  /**
   * Vread.
   *
   * @param versionedId the versioned id
   * @return the value set
   */
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
        return getValueSet(new IdType(versionedId.getIdPart()));
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

  /**
   * Expand value set.
   *
   * @param request the request
   * @param valueSet the value set
   * @param version the version
   * @param filter the filter
   * @param count the count
   * @param offset the offset
   * @param includeDesignations the include designations
   * @param includeDefinition the include definition
   * @param activeOnly the active only
   * @return the value set
   * @throws Exception the exception
   */
  private ValueSet expandValueSet(
      final HttpServletRequest request,
      final ValueSet valueSet,
      final StringType version,
      final String filter,
      final int count,
      final int offset,
      final boolean includeDesignations,
      final boolean includeDefinition,
      final boolean activeOnly)
      throws Exception {

    FhirUtilityR5.notSupportedSearchParams(request);

    // Validate input ValueSet
    if (valueSet == null) {
      throw new InvalidRequestException("ValueSet resource is required for expansion");
    }

    if (valueSet.getCompose() == null || valueSet.getCompose().isEmpty()) {
      throw new InvalidRequestException("ValueSet must contain a compose definition for expansion");
    }

    try {
      // Create the expanded ValueSet
      ValueSet expandedValueSet = new ValueSet();
      expandedValueSet.setId(valueSet.getId());
      expandedValueSet.setUrl(valueSet.getUrl());
      expandedValueSet.setVersion(valueSet.getVersion());
      expandedValueSet.setName(valueSet.getName());
      expandedValueSet.setTitle(valueSet.getTitle());
      expandedValueSet.setStatus(valueSet.getStatus());
      expandedValueSet.setDescription(valueSet.getDescription());

      // Process expansion
      List<ValueSetExpansionContainsComponent> expandedConcepts =
          expandCompose(
              valueSet.getCompose(), filter, activeOnly, includeDesignations, includeDefinition);

      // Apply pagination
      int startIndex = offset;
      int maxResults = count;

      List<ValueSetExpansionContainsComponent> paginatedConcepts =
          applyPagination(expandedConcepts, startIndex, maxResults);

      // Build expansion
      ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
      expansion.setIdentifier(generateExpansionId());
      expansion.setTimestamp(new Date());
      expansion.setTotal(expandedConcepts.size());
      expansion.setContains(paginatedConcepts);

      expandedValueSet.setExpansion(expansion);

      logger.info(
          "Expanded ValueSet {} with {} concepts", valueSet.getUrl(), expandedConcepts.size());

      return expandedValueSet;

    } catch (Exception e) {
      logger.error("Error expanding ValueSet: " + valueSet.getUrl(), e);
      throw new InternalErrorException("Error during ValueSet expansion: " + e.getMessage());
    }
  }

  /**
   * Expand compose.
   *
   * @param compose the compose
   * @param textFilter the text filter
   * @param activeOnly the active only
   * @param includeDesignations the include designations
   * @param includeDefinitions the include definitions
   * @return the list
   * @throws Exception the exception
   */
  private List<ValueSetExpansionContainsComponent> expandCompose(
      ValueSet.ValueSetComposeComponent compose,
      String textFilter,
      boolean activeOnly,
      boolean includeDesignations,
      boolean includeDefinitions)
      throws Exception {

    List<ValueSetExpansionContainsComponent> allConcepts = new ArrayList<>();

    // Process includes
    if (compose.hasInclude()) {
      for (ValueSet.ConceptSetComponent include : compose.getInclude()) {
        List<ValueSetExpansionContainsComponent> includedConcepts =
            processInclude(
                include, textFilter, activeOnly, includeDesignations, includeDefinitions);
        allConcepts.addAll(includedConcepts);
      }
    }

    // Process excludes
    if (compose.hasExclude()) {
      for (ValueSet.ConceptSetComponent exclude : compose.getExclude()) {
        List<ValueSetExpansionContainsComponent> excludedConcepts =
            processExclude(exclude, activeOnly);
        allConcepts = filterOutExcludedConcepts(allConcepts, excludedConcepts);
      }
    }

    // Remove duplicates and sort
    return deduplicateAndSort(allConcepts);
  }

  /**
   * Process include.
   *
   * @param include the include
   * @param textFilter the text filter
   * @param activeOnly the active only
   * @param includeDesignations the include designations
   * @param includeDefinitions the include definitions
   * @return the list
   * @throws Exception the exception
   */
  private List<ValueSetExpansionContainsComponent> processInclude(
      ValueSet.ConceptSetComponent include,
      String textFilter,
      boolean activeOnly,
      boolean includeDesignations,
      boolean includeDefinitions)
      throws Exception {

    List<ValueSetExpansionContainsComponent> concepts = new ArrayList<>();

    // Handle direct concept inclusion
    if (include.hasConcept()) {
      // TODO consider version field on the include and match concepts accordingly
      for (ValueSet.ConceptReferenceComponent concept : include.getConcept()) {
        // Always look up the concept in the terminology service first
        String authoritativeDisplay = lookupConceptDisplay(include.getSystem(), concept.getCode());

        if (authoritativeDisplay == null) {
          logger.warn("Skipping invalid concept: {}#{}", include.getSystem(), concept.getCode());
          continue; // Skip invalid concepts - they don't exist in the terminology
        }

        // Check if input display matches authoritative display
        if (concept.hasDisplay() && !concept.getDisplay().equals(authoritativeDisplay)) {
          logger.warn(
              "Display mismatch for {}#{}: input='{}', authoritative='{}'",
              include.getSystem(),
              concept.getCode(),
              concept.getDisplay(),
              authoritativeDisplay);
          // Continue with authoritative display, but log the mismatch
        }

        ValueSetExpansionContainsComponent contains = new ValueSetExpansionContainsComponent();
        contains.setSystem(include.getSystem());
        contains.setCode(concept.getCode());
        contains.setDisplay(authoritativeDisplay); // Always use authoritative display

        // Apply filters
        if (passesTextFilter(contains, textFilter)
            && passesActiveFilter(contains, activeOnly)
            && passesVersion(contains, include.getVersion())) {
          // Add designations if requested
          if (includeDesignations || includeDefinitions) {
            addDesignationsAndDefinitions(
                contains,
                include.getSystem(),
                concept.getCode(),
                includeDesignations,
                includeDefinitions);
          }
          concepts.add(contains);
        }
      }
    }

    // Handle filter-based inclusion
    // if (include.hasFilter()) {
    // for (ValueSet.ConceptSetFilterComponent filter : include.getFilter()) {
    // List<ValueSetExpansionContainsComponent> filteredConcepts =
    // applyConceptFilter(include.getSystem(), filter, textFilter, activeOnly,
    // includeDesignations);
    // concepts.addAll(filteredConcepts);
    // }
    // }

    // Handle value set inclusion
    // if (include.hasValueSet()) {
    // for (CanonicalType valueSetUrl : include.getValueSet()) {
    // List<ValueSetExpansionContainsComponent> referencedConcepts =
    // expandReferencedValueSet(valueSetUrl.getValue(), textFilter, activeOnly,
    // includeDesignations);
    // concepts.addAll(referencedConcepts);
    // }
    // }

    return concepts;
  }

  /**
   * Process exclude.
   *
   * @param exclude the exclude
   * @param activeOnly the active only
   * @return the list
   * @throws Exception the exception
   */
  private List<ValueSetExpansionContainsComponent> processExclude(
      ValueSet.ConceptSetComponent exclude, boolean activeOnly) throws Exception {

    List<ValueSetExpansionContainsComponent> concepts = new ArrayList<>();

    if (exclude.hasConcept()) {
      for (ValueSet.ConceptReferenceComponent concept : exclude.getConcept()) {
        ValueSetExpansionContainsComponent contains = new ValueSetExpansionContainsComponent();
        contains.setSystem(exclude.getSystem());
        contains.setCode(concept.getCode());
        contains.setDisplay(
            concept.hasDisplay()
                ? concept.getDisplay()
                : lookupConceptDisplay(exclude.getSystem(), concept.getCode()));

        if (passesActiveFilter(contains, activeOnly)) {
          concepts.add(contains);
        }
      }
    }

    // if (exclude.hasFilter()) {
    // for (ValueSet.ConceptSetFilterComponent filter : exclude.getFilter()) {
    // List<ValueSetExpansionContainsComponent> filteredConcepts =
    // applyConceptFilter(exclude.getSystem(), filter, null, activeOnly, false);
    // concepts.addAll(filteredConcepts);
    // }
    // }

    return concepts;
  }

  /**
   * Lookup concept display.
   *
   * @param system the system
   * @param code the code
   * @return the string
   * @throws Exception the exception
   */
  private String lookupConceptDisplay(String system, String code) throws Exception {
    Terminology selectedTerminology =
        termUtils.getIndexedTerminologies(osQueryService).stream()
            .filter(term -> term.getMetadata().getFhirUri().equals(system))
            .findFirst()
            .orElse(null);

    if (selectedTerminology == null) {
      logger.warn("No terminology found for system: {}", system);
      return null; // This will cause the concept to be filtered out
    }

    Optional<Concept> conceptOpt =
        osQueryService.getConcept(code, selectedTerminology, new IncludeParam("minimal"));

    if (!conceptOpt.isPresent()) {
      logger.warn("Concept not found: {}#{}", system, code);
      return null; // This will cause the concept to be filtered out
    }

    Concept concept = conceptOpt.get();
    logger.debug("Looking up display for {}#{}", system, code);
    return concept.getName();
  }

  // private List<ValueSetExpansionContainsComponent> applyConceptFilter(
  // String system,
  // ValueSet.ConceptSetFilterComponent filter,
  // String textFilter,
  // boolean activeOnly,
  // boolean includeDesignations) throws Exception {
  //
  //
  // // TODO: Implement filter logic based on your terminology service
  // List<ValueSetExpansionContainsComponent> compList = new ArrayList<>();
  //
  // Terminology selectedTerminology = termUtils.getIndexedTerminologies(osQueryService)
  // .stream()
  // .filter(term -> term.getMetadata().getFhirUri().equals(system))
  // .findFirst()
  // .orElse(null);
  // Concept concept = osQueryService
  // .getConcept(
  // filter.getValue(),
  // selectedTerminology,
  // new IncludeParam("descendants"))
  // .get();
  // for (Concept desc : concept.getDescendants()) {
  // if (desc.getActive() != null && !desc.getActive()) {
  // continue;
  // }
  // final ValueSet.ValueSetExpansionContainsComponent vsContains =
  // new ValueSet.ValueSetExpansionContainsComponent();
  // vsContains.setSystem(system);
  // vsContains.setCode(desc.getCode());
  // vsContains.setDisplay(desc.getName());
  //
  // compList.add(vsContains);
  // }
  // // Example for SNOMED CT "is-a" filter:
  // // if ("concept".equals(filter.getProperty()) && "is-a".equals(filter.getOp())) {
  // // return snomedService.getDescendants(filter.getValue(), textFilter, activeOnly);
  // // }
  //
  // logger.debug("Applying filter: {} {} {} on {}",
  // filter.getProperty(), filter.getOp(), filter.getValue(), system);
  //
  // return compList;
  // }

  // private List<ValueSetExpansionContainsComponent> expandReferencedValueSet(
  // String valueSetUrl,
  // String textFilter,
  // boolean activeOnly,
  // boolean includeDesignations) throws Exception {
  //
  // // TODO: Implement recursive ValueSet expansion
  // // Example: return valueSetService.expand(valueSetUrl, textFilter, activeOnly,
  // includeDesignations);
  // logger.debug("Expanding referenced ValueSet: {}", valueSetUrl);
  // return new ArrayList<>();
  // }

  /**
   * Adds the designations and definitions.
   *
   * @param contains the contains
   * @param system the system
   * @param code the code
   * @param includeDesignations the include designations
   * @param includeDefinition the include definition
   * @throws Exception the exception
   */
  private void addDesignationsAndDefinitions(
      ValueSetExpansionContainsComponent contains,
      String system,
      String code,
      boolean includeDesignations,
      boolean includeDefinition)
      throws Exception {
    Terminology selectedTerminology =
        termUtils.getIndexedTerminologies(osQueryService).stream()
            .filter(term -> term.getMetadata().getFhirUri().equals(system))
            .findFirst()
            .orElse(null);
    IncludeParam includeParam = new IncludeParam();
    if (includeDesignations) {
      includeParam.setSynonyms(true);
    }
    if (includeDefinition) {
      includeParam.setDefinitions(true);
    }

    Concept concept = osQueryService.getConcept(code, selectedTerminology, includeParam).get();
    for (Synonym term : concept.getSynonyms()) {
      if (term.getTermType() != null && term.getName() != null) {
        ConceptReferenceDesignationComponent designation =
            new ConceptReferenceDesignationComponent()
                .setLanguage("en")
                .setUse(new Coding(term.getUri(), term.getTermType(), term.getName()))
                .setValue(term.getName());

        contains.addDesignation(designation);
      }
    }
    if (includeDefinition) {
      addConceptProperty(contains, concept, "definition");
    }
    logger.debug("Adding designations for {}#{}", system, code);
  }

  // Utility methods

  /**
   * Passes text filter.
   *
   * @param contains the contains
   * @param textFilter the text filter
   * @return true, if successful
   */
  private boolean passesTextFilter(ValueSetExpansionContainsComponent contains, String textFilter) {
    if (textFilter == null || textFilter.trim().isEmpty()) {
      return true;
    }

    String lowerFilter = textFilter.toLowerCase();
    return (contains.getCode() != null && contains.getCode().toLowerCase().contains(lowerFilter))
        || (contains.getDisplay() != null
            && contains.getDisplay().toLowerCase().contains(lowerFilter));
  }

  /**
   * Passes active filter.
   *
   * @param contains the contains
   * @param activeOnly the active only
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean passesActiveFilter(
      ValueSetExpansionContainsComponent contains, boolean activeOnly) throws Exception {
    if (!activeOnly) {
      return true;
    }

    Terminology selectedTerminology =
        termUtils.getIndexedTerminologies(osQueryService).stream()
            .filter(term -> term.getMetadata().getFhirUri().equals(contains.getSystem()))
            .findFirst()
            .orElse(null);

    if (selectedTerminology == null) {
      logger.warn("No terminology found for system: {}", contains.getSystem());
      return false; // Filter out concepts from unknown systems
    }

    Optional<Concept> conceptOpt =
        osQueryService.getConcept(
            contains.getCode(), selectedTerminology, new IncludeParam("minimal"));

    if (!conceptOpt.isPresent()) {
      logger.warn(
          "Concept not found during active filter: {}#{}",
          contains.getSystem(),
          contains.getCode());
      return false; // Filter out non-existent concepts
    }

    Concept concept = conceptOpt.get();
    Boolean active = concept.getActive();

    // If active status is null, treat as inactive when activeOnly is true
    return active != null && active;
  }

  /**
   * Passes version.
   *
   * @param contains the contains
   * @param version the version
   * @return true, if successful
   * @throws Exception the exception
   */
  private boolean passesVersion(ValueSetExpansionContainsComponent contains, String version)
      throws Exception {

    if (version == null || (version != null && version.isEmpty())) {
      return true;
    }
    Terminology selectedTerminology =
        termUtils.getIndexedTerminologies(osQueryService).stream()
            .filter(term -> term.getMetadata().getFhirUri().equals(contains.getSystem()))
            .findFirst()
            .orElse(null);

    if (selectedTerminology == null) {
      logger.warn("No terminology found for system: {}", contains.getSystem());
      return false; // Filter out concepts from unknown systems
    }

    Optional<Concept> conceptOpt =
        osQueryService.getConcept(
            contains.getCode(), selectedTerminology, new IncludeParam("minimal"));

    if (!conceptOpt.isPresent()) {
      logger.warn(
          "Concept not found during active filter: {}#{}",
          contains.getSystem(),
          contains.getCode());
      return false; // Filter out non-existent concepts
    }

    Concept concept = conceptOpt.get();
    String cptVersion = concept.getVersion();

    // If active status is null, treat as inactive when activeOnly is true
    return version != null && version.compareTo(cptVersion) >= 0;
  }

  /**
   * Filter out excluded concepts.
   *
   * @param allConcepts the all concepts
   * @param excludedConcepts the excluded concepts
   * @return the list
   */
  private List<ValueSetExpansionContainsComponent> filterOutExcludedConcepts(
      List<ValueSetExpansionContainsComponent> allConcepts,
      List<ValueSetExpansionContainsComponent> excludedConcepts) {

    Set<String> excludeSet =
        excludedConcepts.stream()
            .map(c -> c.getSystem() + "|" + c.getCode())
            .collect(Collectors.toSet());

    return allConcepts.stream()
        .filter(concept -> !excludeSet.contains(concept.getSystem() + "|" + concept.getCode()))
        .collect(Collectors.toList());
  }

  /**
   * Deduplicate and sort.
   *
   * @param concepts the concepts
   * @return the list
   */
  private List<ValueSetExpansionContainsComponent> deduplicateAndSort(
      List<ValueSetExpansionContainsComponent> concepts) {

    Map<String, ValueSetExpansionContainsComponent> uniqueConcepts = new LinkedHashMap<>();

    for (ValueSetExpansionContainsComponent concept : concepts) {
      String key = concept.getSystem() + "|" + concept.getCode();
      if (!uniqueConcepts.containsKey(key)) {
        uniqueConcepts.put(key, concept);
      }
    }

    return uniqueConcepts.values().stream()
        .sorted(
            (a, b) -> {
              int systemCompare = a.getSystem().compareTo(b.getSystem());
              if (systemCompare != 0) {
                return systemCompare;
              }
              return a.getCode().compareTo(b.getCode());
            })
        .collect(Collectors.toList());
  }

  /**
   * Apply pagination.
   *
   * @param concepts the concepts
   * @param startIndex the start index
   * @param maxResults the max results
   * @return the list
   */
  private List<ValueSetExpansionContainsComponent> applyPagination(
      List<ValueSetExpansionContainsComponent> concepts, int startIndex, int maxResults) {

    int endIndex = Math.min(startIndex + maxResults, concepts.size());
    if (startIndex >= concepts.size()) {
      return new ArrayList<>();
    }

    return concepts.subList(startIndex, endIndex);
  }

  /**
   * Generate expansion id.
   *
   * @return the string
   */
  private String generateExpansionId() {
    return "expansion-"
        + System.currentTimeMillis()
        + "-"
        + UUID.randomUUID().toString().substring(0, 8);
  }
}
