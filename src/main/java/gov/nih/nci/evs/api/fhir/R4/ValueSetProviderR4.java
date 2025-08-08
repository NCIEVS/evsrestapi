package gov.nih.nci.evs.api.fhir.R4;

import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.controller.SubsetController;
import gov.nih.nci.evs.api.fhir.R5.FhirUtilityR5;
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
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceDesignationComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/** FHIR R4 ValueSet provider. */
@Component
public class ValueSetProviderR4 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(ValueSetProviderR4.class);

  /** The search service. */
  @Autowired OpenSearchService searchService;

  /** The search service. */
  @Autowired OpensearchQueryService osQueryService;

  /** The metadata service. */
  @Autowired MetadataService metadataService;

  /** The subset controller. */
  @Autowired SubsetController subsetController;

  /** The term utils. */
  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;

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
   * <p>See https://hl7.org/fhir/R4/valueset-operation-expand.html
   *
   * @param request the request
   * @param details the details
   * @param url the canonical reference to the value set.
   * @param version the value set version to specify the version to be used when generating the
   *     expansion
   * @param filter the text filter applied to restrict code that are returned.
   * @param offset the offset for number of records.
   * @param count the count for codes that should be provided in the partial page view.
   * @param includeDesignations the include designations
   * @param activeOnly controls whether inactive concepts are included or excluded in value set
   *     expansions.
   * @return the value set
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_EXPAND, idempotent = true)
  public ValueSet expandImplicit(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @OperationParam(name = "valueSet") final ValueSet valueSet,
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
      @OperationParam(name = "activeOnly") final BooleanType activeOnly
      // @OperationParam(name = "excludeNested") final BooleanType excludeNested,
      // @OperationParam(name = "excludeNotForUI") final BooleanType excludeNotForUI,
      // @OperationParam(name = "excludePostCoordinated") final BooleanType
      // excludePostCoordinated,
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      // @OperationParam(name = "exclude-system") final StringType exclude_system,
      // @OperationParam(name = "system-version") final StringType system_version,
      // @OperationParam(name = "check-system-version") final StringType check_system_version,
      // @OperationParam(name = "force-system-version") final StringType force_system_version
      ) throws Exception {
    // Extract actual values - use @OperationParam if available, fallback to manual extraction
    String filterValue = getParameterValue(filter, request, "filter");
    boolean activeOnlyValue = getBooleanParameterValue(activeOnly, request, "activeOnly", false);
    boolean includeDesignationsValue =
        getBooleanParameterValue(includeDesignations, request, "includeDesignations", false);
    boolean includeDefinitionValue =
        getBooleanParameterValue(includeDefinition, request, "includeDefinition", false);
    int countValue = getIntParameterValue(count, request, "count", 1000);
    int offsetValue = getIntParameterValue(offset, request, "offset", 0);

    FhirUtilityR4.mutuallyExclusive("url", url, "valueSet", valueSet);

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

    // check if request is a post, throw exception as we don't support post
    // calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_EXPAND,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.required("url", url);

      // The params below should be commented out above
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
        FhirUtilityR4.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR4.notSupported(request, "_has");
      }

      final List<ValueSet> vsList = findPossibleValueSets(null, null, url, version);
      if (vsList.size() == 0) {
        throw FhirUtilityR4.exception(
            "Value set " + url.getValueAsString() + " not found",
            OperationOutcome.IssueType.EXCEPTION,
            500);
      }

      // If properties are indicated, retrieve the concept with all potentially
      // needed info
      IncludeParam includeParam = new IncludeParam("minimal");
      List<String> includeList = new ArrayList<>();

      if (includeDefinitionValue) {
        includeList.add("definitions");
      }
      if (includeDesignationsValue) {
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
      final ValueSetExpansionComponent vsExpansion = new ValueSetExpansionComponent();
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
        vsExpansion.setTotal(subsetMembers.size());
      } else {
        final List<Terminology> terminologies = new ArrayList<>();
        terminologies.add(termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true));
        final SearchCriteria sc = new SearchCriteria();
        sc.setPageSize(countValue);
        sc.setFromRecord(offsetValue);
        sc.setTerm(filterValue);
        sc.setType("contains");
        sc.setTerminology(
            terminologies.stream().map(Terminology::getTerminology).collect(Collectors.toList()));
        if (includeList.size() >= 1) {
          sc.setInclude(String.join(",", includeList));
        }

        ConceptResultList subsetMembersList = searchService.findConcepts(terminologies, sc);
        subsetMembers = subsetMembersList.getConcepts();
        vsExpansion.setTotal(Math.toIntExact(subsetMembersList.getTotal()));
      }
      ValueSetExpansionParameterComponent vsParameter;
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offsetValue);
      if (subsetMembers.size() > 0) {
        for (final Concept member : subsetMembers) {
          if (activeOnlyValue && !member.getActive()) {
            continue;
          }
          final ValueSetExpansionContainsComponent vsContains =
              new ValueSetExpansionContainsComponent();
          vsContains.setSystem(vs.getUrl());
          vsContains.setCode(member.getCode());
          vsContains.setDisplay(member.getName());
          vsExpansion.addContains(vsContains);

          // Add synonyms to the contains component if they were requested
          if (includeDesignationsValue && member.getSynonyms() != null) {
            for (Synonym term : member.getSynonyms()) {
              ConceptReferenceDesignationComponent designation =
                  new ConceptReferenceDesignationComponent()
                      .setLanguage("en")
                      .setUse(new Coding(term.getUri(), term.getTermType(), term.getName()))
                      .setValue(term.getName());

              vsContains.addDesignation(designation);
            }
          }

          // Add definitions if requested
          if (includeDefinitionValue && member.getDefinitions() != null) {
            for (Definition def : member.getDefinitions()) {
              // For R4, we add definitions as properties (this would be different in R5)
              // For now we'll add them as designations with a special use
              ConceptReferenceDesignationComponent defDesignation =
                  new ConceptReferenceDesignationComponent()
                      .setLanguage("en")
                      .setUse(
                          new Coding(
                              "http://terminology.hl7.org/CodeSystem/designation-usage",
                              "definition",
                              "Definition"))
                      .setValue(def.getDefinition());
              vsContains.addDesignation(defDesignation);
            }
          }
        }

        if (filterValue != null) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("filter");
          vsParameter.setValue(new StringType(filterValue));
          vsExpansion.addParameter(vsParameter);
        }

        if (countValue != 1000) { // Only add if different from default
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("count");
          vsParameter.setValue(new IntegerType(countValue));
          vsExpansion.addParameter(vsParameter);
        }

        if (includeDesignationsValue) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("includeDesignations");
          vsParameter.setValue(new BooleanType(includeDesignationsValue));
          vsExpansion.addParameter(vsParameter);
        }

        if (includeDefinitionValue) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("includeDefinition");
          vsParameter.setValue(new BooleanType(includeDefinitionValue));
          vsExpansion.addParameter(vsParameter);
        }

        if (activeOnlyValue) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("activeOnly");
          vsParameter.setValue(new BooleanType(activeOnlyValue));
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
   * Expand valueSet using compose definition.
   *
   * @param request the request
   * @param valueSet the value set to expand
   * @param version the version
   * @param filter the filter
   * @param count the count
   * @param offset the offset
   * @param includeDesignations the include designations flag
   * @param includeDefinition the include definition flag
   * @param activeOnly the active only flag
   * @return the expanded value set
   * @throws Exception the exception
   */
  private ValueSet expandValueSet(
      HttpServletRequest request,
      ValueSet valueSet,
      StringType version,
      String filter,
      int count,
      int offset,
      boolean includeDesignations,
      boolean includeDefinition,
      boolean activeOnly)
      throws Exception {

    logger.info("Expanding ValueSet using compose definition: {}", valueSet.getUrl());

    // Create expansion component
    ValueSetExpansionComponent expansion = new ValueSetExpansionComponent();
    expansion.setIdentifier(generateExpansionId());
    expansion.setTimestamp(new Date());
    expansion.setOffset(offset);

    // Process the compose definition
    List<Concept> allConcepts = new ArrayList<>();
    if (valueSet.hasCompose()) {
      allConcepts =
          expandCompose(valueSet.getCompose(), version, includeDesignations, includeDefinition);
    }

    // Apply text filter if specified
    if (filter != null && !filter.trim().isEmpty()) {
      allConcepts =
          allConcepts.stream()
              .filter(concept -> passesTextFilter(concept, filter))
              .collect(Collectors.toList());
    }

    // Apply active filter if specified
    if (activeOnly) {
      allConcepts =
          allConcepts.stream()
              .filter(concept -> passesActiveFilter(concept, activeOnly))
              .collect(Collectors.toList());
    }

    // Deduplicate and sort concepts
    allConcepts = deduplicateAndSort(allConcepts);

    // Set total before pagination
    expansion.setTotal(allConcepts.size());

    // Apply pagination
    List<Concept> paginatedConcepts = applyPagination(allConcepts, offset, count);

    // Convert concepts to expansion contains
    for (Concept concept : paginatedConcepts) {
      ValueSetExpansionContainsComponent contains = new ValueSetExpansionContainsComponent();
      contains.setSystem(
          concept.getTerminology() != null
              ? "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl"
              : valueSet.getUrl());
      contains.setCode(concept.getCode());
      contains.setDisplay(concept.getName());

      addDesignationsAndDefinitions(contains, concept, includeDesignations, includeDefinition);
      expansion.addContains(contains);
    }

    // Add parameters to expansion
    addExpansionParameters(
        expansion, filter, count, offset, includeDesignations, includeDefinition, activeOnly);

    // Set expansion on value set and return
    ValueSet result = valueSet.copy();
    result.setExpansion(expansion);

    logger.info("ValueSet expansion completed with {} concepts", expansion.getTotal());
    return result;
  }

  /**
   * Expand compose definition to get all included concepts.
   *
   * @param compose the compose component
   * @param version the version
   * @param includeDesignations include designations flag
   * @param includeDefinition include definition flag
   * @return list of concepts
   * @throws Exception on error
   */
  private List<Concept> expandCompose(
      ValueSet.ValueSetComposeComponent compose,
      StringType version,
      boolean includeDesignations,
      boolean includeDefinition)
      throws Exception {

    List<Concept> allConcepts = new ArrayList<>();

    // Process includes first
    if (compose.hasInclude()) {
      for (ValueSet.ConceptSetComponent include : compose.getInclude()) {
        List<Concept> includedConcepts =
            processInclude(include, version, includeDesignations, includeDefinition);
        allConcepts.addAll(includedConcepts);
      }
    }

    // Process excludes - remove concepts that should be excluded
    if (compose.hasExclude()) {
      Set<String> excludeCodes = new java.util.HashSet<>();

      for (ValueSet.ConceptSetComponent exclude : compose.getExclude()) {
        List<Concept> excludedConcepts =
            processExclude(exclude, version, includeDesignations, includeDefinition);
        excludeCodes.addAll(
            excludedConcepts.stream().map(Concept::getCode).collect(Collectors.toSet()));
      }

      // Filter out excluded concepts
      allConcepts = filterOutExcludedConcepts(allConcepts, excludeCodes);
    }

    return allConcepts;
  }

  /**
   * Process include component.
   *
   * @param include the include component
   * @param version the version
   * @param includeDesignations include designations flag
   * @param includeDefinition include definition flag
   * @return list of included concepts
   * @throws Exception on error
   */
  private List<Concept> processInclude(
      ValueSet.ConceptSetComponent include,
      StringType version,
      boolean includeDesignations,
      boolean includeDefinition)
      throws Exception {

    List<Concept> concepts = new ArrayList<>();

    // Determine terminology
    Terminology terminology = getTerminologyFromSystem(include.getSystem(), version);
    if (terminology == null) {
      logger.warn("Unknown system: {}", include.getSystem());
      return concepts;
    }

    // Set up include parameters
    List<String> includeList = new ArrayList<>();
    includeList.add("minimal");
    if (includeDesignations) {
      includeList.add("synonyms");
    }
    if (includeDefinition) {
      includeList.add("definitions");
    }
    IncludeParam includeParam = new IncludeParam(String.join(",", includeList));

    // Process direct concept references
    if (include.hasConcept()) {
      for (ValueSet.ConceptReferenceComponent concept : include.getConcept()) {
        Optional<Concept> foundConcept =
            osQueryService.getConcept(concept.getCode(), terminology, includeParam);
        if (foundConcept.isPresent()) {
          // Validate display if provided
          Concept c = foundConcept.get();
          if (concept.hasDisplay()) {
            String expectedDisplay = lookupConceptDisplay(c.getCode(), terminology);
            if (expectedDisplay != null && !expectedDisplay.equals(concept.getDisplay())) {
              logger.warn(
                  "Display mismatch for {}: expected '{}', got '{}'",
                  c.getCode(),
                  expectedDisplay,
                  concept.getDisplay());
            }
          }
          concepts.add(c);
        } else {
          logger.warn("Concept not found: {} in system {}", concept.getCode(), include.getSystem());
        }
      }
    }

    // Process filters
    if (include.hasFilter()) {
      // Separate inclusion filters from exclusion filters and property-based filters
      List<ValueSet.ConceptSetFilterComponent> inclusionFilters = new ArrayList<>();
      List<ValueSet.ConceptSetFilterComponent> exclusionFilters = new ArrayList<>();
      List<ValueSet.ConceptSetFilterComponent> propertyFilters = new ArrayList<>();

      for (ValueSet.ConceptSetFilterComponent filter : include.getFilter()) {
        String operation = filter.getOp().toCode();
        if ("concept".equals(filter.getProperty())) {
          // Classify concept-based filters
          if ("not-in".equals(operation) || "is-not-a".equals(operation)) {
            exclusionFilters.add(filter);
          } else {
            inclusionFilters.add(filter);
          }
        } else if ("=".equals(operation) || "exists".equals(operation)) {
          propertyFilters.add(filter);
        } else {
          inclusionFilters.add(
              filter); // Handle as concept filter for unsupported property operations
        }
      }

      // Apply inclusion concept-based filters first
      for (ValueSet.ConceptSetFilterComponent filter : inclusionFilters) {
        List<Concept> filteredConcepts = applyConceptFilter(filter, terminology, includeParam);
        concepts.addAll(filteredConcepts);
      }

      // Apply property-based filters to existing concepts
      if (!propertyFilters.isEmpty() && !concepts.isEmpty()) {
        for (ValueSet.ConceptSetFilterComponent filter : propertyFilters) {
          concepts = applyPropertyFilterToConcepts(concepts, filter, terminology);
        }
      } else if (!propertyFilters.isEmpty()) {
        logger.warn(
            "Property filters found but no concepts to filter. Property filters require explicit"
                + " concept list or concept-based filters.");
      }

      // Apply exclusion filters last to remove concepts from the final set
      if (!exclusionFilters.isEmpty() && !concepts.isEmpty()) {
        for (ValueSet.ConceptSetFilterComponent filter : exclusionFilters) {
          concepts = applyExclusionFilter(concepts, filter, terminology);
        }
      } else if (!exclusionFilters.isEmpty()) {
        logger.warn(
            "Exclusion filters found but no concepts to filter. Exclusion filters require explicit"
                + " concept list or inclusion filters.");
      }
    }

    // If no concepts or filters, include entire code system (may be impractical)
    if (!include.hasConcept() && !include.hasFilter()) {
      logger.warn(
          "Include has no concepts or filters - including entire code system may be impractical");
      // For now, we'll return empty list to avoid overwhelming results
      // In a production system, you might want to implement pagination or limits
    }

    return concepts;
  }

  /**
   * Process exclude component.
   *
   * @param exclude the exclude component
   * @param version the version
   * @param includeDesignations include designations flag
   * @param includeDefinition include definition flag
   * @return list of excluded concepts
   * @throws Exception on error
   */
  private List<Concept> processExclude(
      ValueSet.ConceptSetComponent exclude,
      StringType version,
      boolean includeDesignations,
      boolean includeDefinition)
      throws Exception {

    // Process excludes similar to includes, but return concepts to be excluded
    return processInclude(exclude, version, includeDesignations, includeDefinition);
  }

  /**
   * Expand instance.
   *
   * <p>See https://hl7.org/fhir/R4/valueset-operation-expand.html
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param url the canonical reference to the value set.
   * @param version the value set version to specify the version to be used when generating the
   *     expansion
   * @param filter the text filter applied to restrict code that are returned.
   * @param offset the offset for number of records.
   * @param count the count for codes that should be provided in the partial page view.
   * @param includeDesignations the include designations
   * @param activeOnly controls whether inactive concepts are included or excluded in value set
   *     expansions.
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
      @ca.uhn.fhir.rest.annotation.Offset final Integer offset,
      @ca.uhn.fhir.rest.annotation.Count final Integer count,
      @OperationParam(name = "includeDesignations") final BooleanType includeDesignations,
      // @OperationParam(name = "designation") final StringType designation,
      // @OperationParam(name = "includeDefinition") final BooleanType includeDefinition,
      @OperationParam(name = "activeOnly") final BooleanType activeOnly
      // @OperationParam(name = "excludeNested") final BooleanType excludeNested,
      // @OperationParam(name = "excludeNotForUI") final BooleanType excludeNotForUI,
      // @OperationParam(name = "excludePostCoordinated") final BooleanType
      // excludePostCoordinated,
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      // @OperationParam(name = "exclude-system") final StringType exclude_system,
      // @OperationParam(name = "system-version") final StringType system_version,
      // @OperationParam(name = "check-system-version") final StringType check_system_version,
      // @OperationParam(name = "force-system-version") final StringType force_system_version
      ) throws Exception {
    // check if request is a post, throw exception as we don't support post
    // calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_EXPAND,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      for (final String param :
          new String[] {
            "valueSet",
            "context",
            "contextDirection",
            "date",
            "designation",
            "includeDefinition",
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
        FhirUtilityR4.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR4.notSupported(request, "_has");
      }
      final List<ValueSet> vsList = findPossibleValueSets(id, null, null, version);
      if (vsList.size() == 0) {
        throw FhirUtilityR4.exception(
            "Value set " + id + " not found", OperationOutcome.IssueType.EXCEPTION, 500);
      }

      // If properties are indicated, retrieve the concept with all potentially
      // needed info
      IncludeParam includeParam = new IncludeParam("minimal");
      List<String> includeList = new ArrayList<>();

      if (includeDesignations != null && includeDesignations.getValue().booleanValue() == true) {
        includeList.add("synonyms");
      } else {
        includeList.add("minimal");
      }
      includeParam = new IncludeParam(String.join(",", includeList));

      final ValueSet vs = vsList.get(0);
      final ValueSetExpansionComponent vsExpansion = new ValueSetExpansionComponent();
      List<Concept> subsetMembers = new ArrayList<Concept>();

      if ((url != null) && !vs.getUrl().equals(url.getValue())) {
        throw FhirUtilityR4.exception(
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
        vsExpansion.setTotal(subsetMembers.size());
      } else {
        final List<Terminology> terminologies = new ArrayList<>();
        terminologies.add(termUtils.getIndexedTerminology(vs.getTitle(), osQueryService, true));
        final SearchCriteria sc = new SearchCriteria();
        sc.setPageSize(count != null ? count : 10);
        sc.setFromRecord(offset != null ? offset : 0);
        sc.setTerm(filter != null ? filter.getValue() : null);
        sc.setType("contains");
        sc.setTerminology(
            terminologies.stream().map(Terminology::getTerminology).collect(Collectors.toList()));
        if (includeList.size() >= 1) {
          sc.setInclude(String.join(",", includeList));
        }

        ConceptResultList subsetMembersList = searchService.findConcepts(terminologies, sc);
        subsetMembers = subsetMembersList.getConcepts();
        vsExpansion.setTotal(Math.toIntExact(subsetMembersList.getTotal()));
      }
      ValueSetExpansionParameterComponent vsParameter;
      vsExpansion.setTimestamp(new Date());
      vsExpansion.setOffset(offset != null ? offset : 0);
      if (subsetMembers.size() > 0) {
        for (final Concept member : subsetMembers) {
          if (activeOnly != null && activeOnly.getValue() && !member.getActive()) {
            continue;
          }
          final ValueSetExpansionContainsComponent vsContains =
              new ValueSetExpansionContainsComponent();
          vsContains.setSystem(vs.getUrl());
          vsContains.setCode(member.getCode());
          vsContains.setDisplay(member.getName());
          vsExpansion.addContains(vsContains);

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
        }

        if (filter != null) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("filter");
          vsParameter.setValue(filter);
          vsExpansion.addParameter(vsParameter);
        }

        if (count != null) {
          vsParameter = new ValueSetExpansionParameterComponent();
          vsParameter.setName("count");
          vsParameter.setValue(new IntegerType(count));
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
   * Validate code implicit.
   *
   * <p>See https://hl7.org/fhir/R4/valueset-operation-validate-code.html
   *
   * @param request the request
   * @param details the details
   * @param url the value set canonical url
   * @param code the code that is to be validated. If provided, systems or context must be provided
   * @param system the system for the code that is to be validated
   * @param systemVersion the version of the system
   * @param display the display associated with the code, if provided. If provided, a code must be
   *     provided
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
    // calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyExclusive("code", code, "coding", coding);
      FhirUtilityR4.mutuallyRequired("code", code, "system", system, "url", url);
      FhirUtilityR4.mutuallyRequired("system", system, "systemVersion", systemVersion);

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
        FhirUtilityR4.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR4.notSupported(request, "_has");
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

      if (list.size() > 0) {
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
      throw FhirUtilityR4.exception(
          "Failed to load value set", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code instance.
   *
   * <p>See https://hl7.org/fhir/R4/valueset-operation-validate-code.html
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param url the value set canonical url
   * @param code the code that is to be validated. If provided, systems or context must be provided
   * @param system the system for the code that is to be validated
   * @param systemVersion the version of the system
   * @param display the display associated with the code, if provided. If provided, a code must be
   *     provided
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
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyExclusive("code", code, "coding", coding);
      FhirUtilityR4.requireAtLeastOneOf(
          "code", code, "coding", coding, "systemVersion", systemVersion, "url", url);

      // TODO: not sure that "version" should be in this list
      for (final String param :
          new String[] {
            "context",
            "date",
            "abstractt",
            "displayLanguage",
            "version",
            "valueSet",
            "valueSetVersion"
          }) {
        FhirUtilityR4.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR4.notSupported(request, "_has");
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
      if (list.size() > 0) {
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
              org.hl7.fhir.r5.model.OperationOutcome.IssueType.EXCEPTION,
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
      throw FhirUtilityR4.exception(
          "Failed to load value set", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Find value sets.
   *
   * <p>See https://hl7.org/fhir/R4/valueset.html (find "search parameters")
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
    FhirUtilityR4.notSupportedSearchParams(request);
    final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);
    final List<ValueSet> list = new ArrayList<>();

    if (code == null) {
      for (final Terminology terminology : terms) {
        final ValueSet vs = FhirUtilityR4.toR4VS(terminology);
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
    final List<Concept> subsetsAsConcepts =
        subsets.stream().flatMap(Concept::streamSelfAndChildren).toList();
    for (final Concept subset : subsetsAsConcepts) {
      final ValueSet vs = FhirUtilityR4.toR4VS(subset);
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
   * <p>See https://hl7.org/fhir/R4/valueset.html
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

      throw FhirUtilityR4.exception(
          "Value set not found = " + id.getIdPart(), IssueType.NOTFOUND, 404);

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

    final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);

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
    final List<Concept> subsets = getNcitSubsets();
    final List<Concept> subsetsAsConcepts =
        subsets.stream().flatMap(Concept::streamSelfAndChildren).toList();

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

  @Cacheable("ncitsubsets")
  public List<Concept> getNcitSubsets() throws Exception {
    return metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.empty());
  }

  /* see superclass */
  @Override
  public Class<ValueSet> getResourceType() {
    return ValueSet.class;
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
        throw FhirUtilityR4.exception(
            "Value set not found = " + (id == null ? "null" : id.getIdPart()),
            OperationOutcome.IssueType.NOTFOUND,
            404);
      }
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR4.exception(
          "Failed to get value set", OperationOutcome.IssueType.EXCEPTION, 500);
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
        return getValueSet(new org.hl7.fhir.r4.model.IdType(versionedId.getIdPart()));
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

      throw FhirUtilityR4.exception(
          "Value set version not found: " + resourceId + " version " + versionId,
          OperationOutcome.IssueType.NOTFOUND,
          404);
    } catch (final FHIRServerResponseException e) {
      throw e; // Re-throw FHIR exceptions as-is
    } catch (final Exception e) {
      logger.error("Unexpected exception in vread", e);
      throw FhirUtilityR4.exception(
          "Failed to get value set version", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  // Utility methods for ValueSet expansion

  /** Generate unique expansion identifier. */
  private String generateExpansionId() {
    return UUID.randomUUID().toString();
  }

  /** Get terminology from system URL. */
  private Terminology getTerminologyFromSystem(String system, StringType version) throws Exception {
    if ("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl".equals(system)) {
      return termUtils.getIndexedTerminology("ncit", osQueryService, true);
    }
    // Add support for other terminologies as needed
    return null;
  }

  /** Apply concept filter (is-a, child-of, in, etc.). */
  private List<Concept> applyConceptFilter(
      ValueSet.ConceptSetFilterComponent filter, Terminology terminology, IncludeParam includeParam)
      throws Exception {

    List<Concept> concepts = new ArrayList<>();
    String property = filter.getProperty();
    String operation = filter.getOp().toCode();
    String value = filter.getValue();

    logger.info("Applying filter: property={}, op={}, value={}", property, operation, value);

    switch (operation.toLowerCase()) {
      case "=":
        // Property equals filter is now handled in the include processing logic
        // This case should not be reached for property filters
        if ("concept".equals(property)) {
          logger.warn("Equals operation not supported for 'concept' property: {}", operation);
        } else {
          logger.warn(
              "Property equals filter should be handled in include processing, not in"
                  + " applyConceptFilter: {}",
              operation);
        }
        break;
      case "is-a":
        concepts = processDescendants(value, terminology, includeParam, true);
        break;
      case "descendent-of":
        concepts = processDescendants(value, terminology, includeParam, false);
        break;
      case "is-not-a":
        // Exclusion filter - should be handled in include processing, not here
        logger.warn(
            "Is-not-a filter operation should be handled as exclusion filter in include processing:"
                + " {}",
            operation);
        break;
      case "regex":
        // Not implemented yet
        logger.warn("Regex filter operation not yet implemented: {}", operation);
        break;
      case "in":
        concepts = processInFilter(value, terminology, includeParam);
        break;
      case "not-in":
        // Exclusion filter - should be handled in include processing, not here
        logger.warn(
            "Not-in filter operation should be handled as exclusion filter in include processing:"
                + " {}",
            operation);
        break;
      case "generalizes":
        // Not implemented yet
        logger.warn("Generalizes filter operation not yet implemented: {}", operation);
        break;
      case "exists":
        // Property exists filter is now handled in the include processing logic
        // This case should not be reached for property filters
        if ("concept".equals(property)) {
          logger.warn("Exists operation not supported for 'concept' property: {}", operation);
        } else {
          logger.warn(
              "Property exists filter should be handled in include processing, not in"
                  + " applyConceptFilter: {}",
              operation);
        }
        break;
      default:
        logger.warn("Unsupported filter operation for FHIR R4: {}", operation);
    }

    return concepts;
  }

  /** Process descendants (for is-a and descendant-of filters). */
  private List<Concept> processDescendants(
      String conceptCode, Terminology terminology, IncludeParam includeParam, boolean includeSelf)
      throws Exception {

    List<Concept> concepts = new ArrayList<>();

    // Include the concept itself if is-a filter
    if (includeSelf) {
      Optional<Concept> concept = osQueryService.getConcept(conceptCode, terminology, includeParam);
      if (concept.isPresent()) {
        concepts.add(concept.get());
      }
    }

    // Get descendants using the query service
    List<Concept> descendants = osQueryService.getDescendants(conceptCode, terminology);
    concepts.addAll(descendants);

    return concepts;
  }

  /** Process "in" filter (comma-separated concept codes). */
  private List<Concept> processInFilter(
      String value, Terminology terminology, IncludeParam includeParam) throws Exception {

    List<Concept> concepts = new ArrayList<>();
    String[] codes = value.split(",");

    for (String code : codes) {
      code = code.trim();
      Optional<Concept> concept = osQueryService.getConcept(code, terminology, includeParam);
      if (concept.isPresent()) {
        concepts.add(concept.get());
      } else {
        logger.warn("Concept not found for 'in' filter: {}", code);
      }
    }

    return concepts;
  }

  /** Apply property filter to concept list. */
  private List<Concept> applyPropertyFilterToConcepts(
      List<Concept> concepts, ValueSet.ConceptSetFilterComponent filter, Terminology terminology)
      throws Exception {

    List<Concept> filteredConcepts = new ArrayList<>();
    String propertyName = filter.getProperty();
    String operation = filter.getOp().toCode();
    String value = filter.getValue();

    for (Concept concept : concepts) {
      boolean shouldInclude = false;

      try {
        if ("=".equals(operation)) {
          shouldInclude = conceptHasPropertyValue(concept, terminology, propertyName, value.trim());
        } else if ("exists".equals(operation)) {
          boolean shouldExist = "true".equalsIgnoreCase(value.trim());
          shouldInclude = conceptHasProperty(concept, terminology, propertyName, shouldExist);
        }
      } catch (Exception e) {
        logger.warn(
            "Error checking property filter '{}' {} '{}' for concept {}: {}",
            propertyName,
            operation,
            value,
            concept.getCode(),
            e.getMessage());
      }

      if (shouldInclude) {
        filteredConcepts.add(concept);
      }
    }

    logger.info(
        "Property filter '{}' {} '{}' filtered {} concepts to {} matches",
        propertyName,
        operation,
        value,
        concepts.size(),
        filteredConcepts.size());

    return filteredConcepts;
  }

  /** Check if concept has property value. */
  private boolean conceptHasPropertyValue(
      Concept concept, Terminology terminology, String propertyName, String propertyValue)
      throws Exception {

    // Get the full concept details including properties if not already loaded
    Concept fullConcept = concept;
    if (concept.getProperties() == null || concept.getProperties().isEmpty()) {
      Optional<Concept> conceptOpt =
          osQueryService.getConcept(concept.getCode(), terminology, new IncludeParam("properties"));
      if (conceptOpt.isPresent()) {
        fullConcept = conceptOpt.get();
      }
    }

    if (fullConcept.getProperties() == null) {
      return false;
    }

    // Check if any property matches the specified name and value
    return fullConcept.getProperties().stream()
        .anyMatch(
            prop -> propertyName.equals(prop.getType()) && propertyValue.equals(prop.getValue()));
  }

  /** Check if concept has property. */
  private boolean conceptHasProperty(
      Concept concept, Terminology terminology, String propertyName, boolean shouldExist)
      throws Exception {

    // Get the full concept details including properties if not already loaded
    Concept fullConcept = concept;
    if (concept.getProperties() == null || concept.getProperties().isEmpty()) {
      Optional<Concept> conceptOpt =
          osQueryService.getConcept(concept.getCode(), terminology, new IncludeParam("properties"));
      if (conceptOpt.isPresent()) {
        fullConcept = conceptOpt.get();
      }
    }

    if (fullConcept.getProperties() == null || fullConcept.getProperties().isEmpty()) {
      return !shouldExist; // No properties = property doesn't exist
    }

    // Check if the specific property exists (has at least one value)
    boolean hasProperty =
        fullConcept.getProperties().stream()
            .anyMatch(
                prop ->
                    propertyName.equals(prop.getType())
                        && prop.getValue() != null
                        && !prop.getValue().toString().trim().isEmpty());

    return hasProperty == shouldExist;
  }

  /** Apply exclusion filter to remove concepts from the list. */
  private List<Concept> applyExclusionFilter(
      List<Concept> concepts, ValueSet.ConceptSetFilterComponent filter, Terminology terminology)
      throws Exception {

    List<Concept> filteredConcepts = new ArrayList<>();
    String operation = filter.getOp().toCode();
    String value = filter.getValue();

    logger.info(
        "Applying exclusion filter: op={}, value={} to {} concepts",
        operation,
        value,
        concepts.size());

    for (Concept concept : concepts) {
      boolean shouldExclude = false;

      try {
        if ("not-in".equals(operation)) {
          shouldExclude = conceptIsInList(concept, value);
        } else if ("is-not-a".equals(operation)) {
          shouldExclude = conceptIsA(concept, value, terminology);
        }
      } catch (Exception e) {
        logger.warn(
            "Error checking exclusion filter {} '{}' for concept {}: {}",
            operation,
            value,
            concept.getCode(),
            e.getMessage());
      }

      if (!shouldExclude) {
        filteredConcepts.add(concept);
      } else {
        logger.debug("Excluding concept {} due to {} filter", concept.getCode(), operation);
      }
    }

    logger.info(
        "Exclusion filter {} '{}' filtered {} concepts to {} remaining",
        operation,
        value,
        concepts.size(),
        filteredConcepts.size());

    return filteredConcepts;
  }

  /** Check if concept is in comma-separated list. */
  private boolean conceptIsInList(Concept concept, String conceptList) {
    if (conceptList == null || conceptList.trim().isEmpty()) {
      return false;
    }

    String[] codes = conceptList.split(",");
    for (String code : codes) {
      if (concept.getCode().equals(code.trim())) {
        return true;
      }
    }
    return false;
  }

  /** Check if concept has is-a relationship with target concept. */
  private boolean conceptIsA(Concept concept, String targetConceptCode, Terminology terminology)
      throws Exception {
    // First check if it's the same concept
    if (concept.getCode().equals(targetConceptCode)) {
      return true;
    }

    // Check if concept is a descendant of the target concept
    // We use the ancestors to determine is-a relationships
    List<Concept> ancestors = osQueryService.getAncestors(concept.getCode(), terminology);

    return ancestors.stream().anyMatch(ancestor -> ancestor.getCode().equals(targetConceptCode));
  }

  /** Lookup concept display name. */
  private String lookupConceptDisplay(String code, Terminology terminology) throws Exception {
    Optional<Concept> concept =
        osQueryService.getConcept(code, terminology, new IncludeParam("minimal"));
    return concept.map(Concept::getName).orElse(null);
  }

  /** Add designations and definitions to expansion contains. */
  private void addDesignationsAndDefinitions(
      ValueSetExpansionContainsComponent contains,
      Concept concept,
      boolean includeDesignations,
      boolean includeDefinition) {

    if (includeDesignations && concept.getSynonyms() != null) {
      for (Synonym synonym : concept.getSynonyms()) {
        ConceptReferenceDesignationComponent designation =
            new ConceptReferenceDesignationComponent()
                .setLanguage("en")
                .setUse(new Coding(synonym.getUri(), synonym.getTermType(), synonym.getName()))
                .setValue(synonym.getName());
        contains.addDesignation(designation);
      }
    }

    if (includeDefinition && concept.getDefinitions() != null) {
      for (Definition definition : concept.getDefinitions()) {
        ConceptReferenceDesignationComponent defDesignation =
            new ConceptReferenceDesignationComponent()
                .setLanguage("en")
                .setUse(
                    new Coding(
                        "http://terminology.hl7.org/CodeSystem/designation-usage",
                        "definition",
                        "Definition"))
                .setValue(definition.getDefinition());
        contains.addDesignation(defDesignation);
      }
    }
  }

  /** Check if concept passes text filter. */
  private boolean passesTextFilter(Concept concept, String filter) {
    if (filter == null || filter.trim().isEmpty()) {
      return true;
    }

    String lowerFilter = filter.toLowerCase();

    // Check code and name
    if (concept.getCode().toLowerCase().contains(lowerFilter)
        || concept.getName().toLowerCase().contains(lowerFilter)) {
      return true;
    }

    // Check synonyms
    if (concept.getSynonyms() != null) {
      for (Synonym synonym : concept.getSynonyms()) {
        if (synonym.getName().toLowerCase().contains(lowerFilter)) {
          return true;
        }
      }
    }

    return false;
  }

  /** Check if concept passes active filter. */
  private boolean passesActiveFilter(Concept concept, boolean activeOnly) {
    if (!activeOnly) {
      return true;
    }
    return concept.getActive();
  }

  /** Filter out excluded concepts. */
  private List<Concept> filterOutExcludedConcepts(
      List<Concept> concepts, Set<String> excludeCodes) {
    return concepts.stream()
        .filter(concept -> !excludeCodes.contains(concept.getCode()))
        .collect(Collectors.toList());
  }

  /** Deduplicate and sort concepts by code. */
  private List<Concept> deduplicateAndSort(List<Concept> concepts) {
    Map<String, Concept> conceptMap = new LinkedHashMap<>();

    for (Concept concept : concepts) {
      // Keep first occurrence of each concept
      conceptMap.putIfAbsent(concept.getCode(), concept);
    }

    return conceptMap.values().stream()
        .sorted((c1, c2) -> c1.getCode().compareTo(c2.getCode()))
        .collect(Collectors.toList());
  }

  /** Apply pagination to concept list. */
  private List<Concept> applyPagination(List<Concept> concepts, int offset, int count) {
    if (offset >= concepts.size()) {
      return new ArrayList<>();
    }

    int endIndex = Math.min(offset + count, concepts.size());
    return concepts.subList(offset, endIndex);
  }

  /** Add expansion parameters. */
  private void addExpansionParameters(
      ValueSetExpansionComponent expansion,
      String filter,
      int count,
      int offset,
      boolean includeDesignations,
      boolean includeDefinition,
      boolean activeOnly) {

    if (filter != null && !filter.trim().isEmpty()) {
      expansion.addParameter().setName("filter").setValue(new StringType(filter));
    }

    if (count != 1000) {
      expansion.addParameter().setName("count").setValue(new IntegerType(count));
    }

    if (offset != 0) {
      expansion.addParameter().setName("offset").setValue(new IntegerType(offset));
    }

    if (includeDesignations) {
      expansion.addParameter().setName("includeDesignations").setValue(new BooleanType(true));
    }

    if (includeDefinition) {
      expansion.addParameter().setName("includeDefinition").setValue(new BooleanType(true));
    }

    if (activeOnly) {
      expansion.addParameter().setName("activeOnly").setValue(new BooleanType(true));
    }
  }
}
