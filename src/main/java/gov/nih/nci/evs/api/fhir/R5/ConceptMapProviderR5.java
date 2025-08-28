package gov.nih.nci.evs.api.fhir.R5;

import static gov.nih.nci.evs.api.service.OpenSearchServiceImpl.escape;

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
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Mapping;
import gov.nih.nci.evs.api.model.MappingResultList;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpenSearchService;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import gov.nih.nci.evs.api.util.FhirUtility;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** FHIR R5 ConceptMap provider. */
@Component
public class ConceptMapProviderR5 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(ConceptMapProviderR5.class);

  /** the query service. */
  @Autowired OpensearchQueryService osQueryService;

  /** the opensearch search service. */
  @Autowired OpenSearchService osSearchService;

  /** The term utils. */
  @Autowired TerminologyUtils termUtils;

  /**
   * Returns the type of resource for this provider.
   *
   * @return the resource
   */
  @Override
  public Class<ConceptMap> getResourceType() {
    return ConceptMap.class;
  }

  /**
   * Find concept maps.
   *
   * @param request the request
   * @param id the id
   * @param date the date
   * @param name the name
   * @param url the url
   * @param version the version
   * @param count the count
   * @param offset the offset
   * @return the list
   * @throws Exception the exception
   */
  @Search
  public Bundle findConceptMaps(
      final HttpServletRequest request,
      @OptionalParam(name = "_id") final TokenParam id,
      @OptionalParam(name = "date") final DateRangeParam date,
      @OptionalParam(name = "name") final StringParam name,
      @OptionalParam(name = "url") final StringParam url,
      @OptionalParam(name = "version") final StringParam version,
      @Description(shortDefinition = "Number of entries to return") @OptionalParam(name = "_count")
          final NumberParam count,
      @Description(shortDefinition = "Start offset, used when reading a next page")
          @OptionalParam(name = "_offset")
          final NumberParam offset,
      @Description(shortDefinition = "Sort by field (name, title, publisher, date, url)")
          @OptionalParam(name = "_sort")
          final StringParam sort)
      throws Exception {
    try {
      FhirUtilityR5.notSupportedSearchParams(request);

      final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);
      final Map<String, Terminology> map = new HashMap<>();
      for (final Terminology terminology : terms) {
        map.put(terminology.getTerminology(), terminology);
      }
      final List<Concept> mapsets = osQueryService.getMapsets(new IncludeParam("properties"));

      final List<ConceptMap> list = new ArrayList<>();
      for (final Concept mapset : mapsets) {
        final List<Property> props = mapset.getProperties();
        if (props.stream()
            .anyMatch(m -> m.getType().equals("downloadOnly") && m.getValue().equals("true"))) {
          continue;
        }
        final ConceptMap cm =
            FhirUtilityR5.toR5(
                map.get(mapset.getPropertyValue("sourceTerminology")),
                map.get(mapset.getPropertyValue("targetTerminology")),
                mapset);
        // Skip non-matching
        if (url != null && !url.getValue().equals(cm.getUrl())) {
          logger.debug("  SKIP url mismatch = " + cm.getUrl());
          continue;
        }
        if (id != null && !id.getValue().equals(cm.getId())) {
          logger.debug("  SKIP id mismatch = " + cm.getName());
          continue;
        }
        if (name != null && !FhirUtility.compareString(name, cm.getName())) {
          logger.debug("  SKIP name mismatch = " + cm.getName());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cm.getDate())) {
          logger.debug("  SKIP date mismatch = " + cm.getDate());
          continue;
        }
        if (version != null && !FhirUtility.compareString(version, cm.getVersion())) {
          logger.debug("  SKIP version mismatch = " + cm.getVersion());
          continue;
        }

        list.add(cm);
      }

      // Apply sorting if requested
      applySorting(list, sort);

      return FhirUtilityR5.makeBundle(request, list, count, offset);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR5.exception("Failed to find concept maps", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Perform the lookup in the instance map.
   *
   * <pre>
   *    <a href=
   * "https://hl7.org/fhir/R5/conceptmap-operation-translate.html">Conceptmap operation ref </a>"
   * </pre>
   *
   * @param request the servlet request
   * @param response the servlet response
   * @param details the servlet request details
   * @param id the lookup id
   * @param url the canonical URL for a concept map
   * @param conceptMapVersion The identifier that is used to identify a specific version of the
   *     concept map to be used
   * @param sourceCode The code that is to be translated. If a code is provided, a system must be
   *     provided.
   * @param system The system for the code that is to be translated.
   * @param version The version of the system, if one was provided in the source data.
   * @param sourceScope Limits the scope of the $translate operation to source codes that are
   *     members of this value set.
   * @param sourceCoding A coding to translate.
   * @param targetCode The target code that is to be translated to. If a code is provided, a system
   *     must be provided.
   * @param targetCoding A target coding to translate to.
   * @param targetScope Limits the scope of the $translate operation to target codes that are
   *     members of this value set.
   * @param targetSystem Identifies a target code system in which a mapping is sought. This
   *     parameter is an alternative to the targetScope parameter - only one is required.
   * @return the parameters
   * @throws Exception throws exception if error occurs.
   *     <p>no support for dependency parameter
   */
  @Operation(name = JpaConstants.OPERATION_TRANSLATE, idempotent = true)
  public Parameters translateInstance(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "conceptMapVersion") final StringType conceptMapVersion,
      @OperationParam(name = "sourceCode") final CodeType sourceCode,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "sourceScope") final UriType sourceScope,
      @OperationParam(name = "sourceCoding") final Coding sourceCoding,
      @OperationParam(name = "targetCode") final UriType targetCode,
      // TODO: support for targetCoding not provided due to API error; should be Coding
      @OperationParam(name = "targetCoding") final UriType targetCoding,
      @OperationParam(name = "targetScope") final UriType targetScope,
      @OperationParam(name = "targetSystem") final UriType targetSystem)
      throws Exception {
    // Check if request is POST, throw error as we don't support POST calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE,
          IssueType.NOTSUPPORTED,
          405);
    }

    try {
      FhirUtilityR5.mutuallyRequired("sourceCode", sourceCode, "system", system);
      FhirUtilityR5.mutuallyRequired("targetCode", targetCode, "system", system);
      FhirUtilityR5.mutuallyExclusive("targetScope", targetScope, "targetSystem", targetSystem);
      FhirUtilityR5.mutuallyExclusive("sourceCode", sourceCode, "targetCoding", targetCoding);
      FhirUtilityR5.mutuallyExclusive("sourceCode", sourceCode, "targetCode", targetCode);
      FhirUtilityR5.mutuallyExclusive("targetCode", sourceCode, "sourceCoding", targetCoding);
      FhirUtilityR5.mutuallyExclusive("sourceCoding", sourceCoding, "targetCoding", targetCoding);
      FhirUtilityR5.mutuallyExclusive("sourceCoding", sourceCoding, "system", system);
      FhirUtilityR5.mutuallyExclusive("targetCoding", targetCoding, "system", system);
      FhirUtilityR5.requireAtLeastOneOf(
          "sourceCode",
          sourceCode,
          "targetCode",
          targetCode,
          "sourceCoding",
          sourceCoding,
          "targetCoding",
          targetCoding);
      for (final String param :
          new String[] {"sourceCodableConcept", "targetCodableConcept", "dependency"}) {
        FhirUtilityR5.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR5.notSupported(request, "_has");
      }

      UriType systemToLookup = null;
      CodeType targetCodeToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (sourceCoding != null) {
        systemToLookup = sourceCoding.getSystemElement();
      }
      // targetCoding sent in as a UriType, so needs to be parsed to extract targetCode and system
      else if (targetCoding != null) {
        String urlPart = targetCoding.getValue().substring(0, targetCoding.getValue().indexOf("|"));
        targetCodeToLookup =
            new CodeType(
                targetCoding.getValue().substring(targetCoding.getValue().indexOf("|") + 1));
        systemToLookup = new UriType(urlPart);
      }

      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(null, null, systemToLookup, url, version, targetSystem);
      // Extract the mapsetcode from cm build the query
      final List<String> mapsetCodes = cm.stream().map(m -> m.getTitle()).toList();

      // Build a string query to search for the source code and target code
      CodeType sourceCodeToLookup = null;
      if (sourceCode != null) {
        sourceCodeToLookup = sourceCode;
      } else if (sourceCoding != null) {
        sourceCodeToLookup = sourceCoding.getCodeElement();
      }

      if (targetCode != null) {
        targetCodeToLookup = new CodeType(targetCode.getValue()); // Convert UriType to CodeType
      }
      String query =
          buildFhirQueryString(sourceCodeToLookup, targetCodeToLookup, mapsetCodes, "AND");
      logger.debug("   Fhir query string = " + query);

      MappingResultList maps;

      SearchCriteria criteria = new SearchCriteria();
      // Set as high as we can, should not be more than 10000 in reality.
      // (Unlimited support?)
      criteria.setPageSize(10000);
      criteria.setFromRecord(0);

      maps = osSearchService.findConceptMappings(query, criteria);
      final List<Mapping> conceptMaps = maps.getMaps();

      if (!conceptMaps.isEmpty()) {
        final Mapping map = conceptMaps.get(0);
        params.addParameter("result", true);
        final Parameters.ParametersParameterComponent property =
            new Parameters.ParametersParameterComponent().setName("match");
        property.addPart().setName("equivalence").setValue(new StringType("equivalent"));
        params.addParameter(property);

        // Return the opposite of what was searched for
        if (sourceCodeToLookup != null) {
          // User provided source code, return target code
          property
              .addPart()
              .setName("concept")
              .setValue(
                  new Coding(map.getTargetTerminology(), map.getTargetCode(), map.getTargetName()));
        } else if (targetCodeToLookup != null) {
          // User provided target code, return source code (reverse mapping)
          property
              .addPart()
              .setName("concept")
              .setValue(
                  new Coding(map.getSourceTerminology(), map.getSourceCode(), map.getSourceName()));
        }
      }
      if (!params.hasParameter()) {
        params.addParameter("result", false);
        params.addParameter("match", "none");
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred:", e);
      throw FhirUtilityR5.exception("Failed to translate concept map", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Perform the lookup in the implicit map.
   *
   * <pre>
   *  <a href=
   * "https://hl7.org/fhir/R5/conceptmap-operation-translate.html">Conceptmap operation ref</a>
   * </pre>
   *
   * @param request the servlet request.
   * @param response the servlet response.
   * @param details the servlet request details.
   * @param url A canonical URL for a concept map. The server must know the concept map.
   * @param conceptMapVersion The identifier that is used to identify a specific version of the
   *     concept map to be used for the translation. This is an arbitrary value managed by the
   *     concept map author and is not expected to be globally unique.
   * @param sourceCode The code that is to be translated. If a code is provided, a system must be
   *     provided.
   * @param system The system for the code that is to be translated.
   * @param version The version of the system, if one was provided in the source data.
   * @param sourceScope Limits the scope of the $translate operation to source codes that are
   *     members of this value set.
   * @param sourceCoding A coding to translate.
   * @param targetCode The target code that is to be translated to. If a code is provided, a system
   *     must be provided.
   * @param targetCoding A target coding to translate to.
   * @param targetScope Limits the scope of the $translate operation to target codes that are
   *     members of this value set.
   * @param targetSystem Identifies a target code system in which a mapping is sought. This
   *     parameter is an alternative to the targetScope parameter - only one is required.
   * @return the parameters
   * @throws Exception throws exception if error occurs.
   *     <p>no support for dependency parameter
   */
  @Operation(name = JpaConstants.OPERATION_TRANSLATE, idempotent = true)
  public Parameters translateImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "conceptMapVersion") final StringType conceptMapVersion,
      @OperationParam(name = "sourceCode") final CodeType sourceCode,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "sourceScope") final UriType sourceScope,
      @OperationParam(name = "sourceCoding") final Coding sourceCoding,
      // @OperationParam(name = "codeableConcept") final CodeableConcept
      // sourceCodeableConcept,
      @OperationParam(name = "targetCode") final UriType targetCode,
      // TODO: support for targetCoding not provided due to API error; should be Coding
      @OperationParam(name = "targetCoding") final UriType targetCoding,
      @OperationParam(name = "targetScope") final UriType targetScope,
      @OperationParam(name = "targetSystem") final UriType targetSystem
      // @OperationParam(name = "dependency") final UriType dependency
      ) throws Exception {
    // Check if request is post, throw error as we don't support POST calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyRequired("sourceCode", sourceCode, "system", system);
      FhirUtilityR5.mutuallyRequired("targetCode", targetCode, "system", system);
      FhirUtilityR5.mutuallyExclusive("sourceCode", sourceCode, "sourceCoding", sourceCoding);
      FhirUtilityR5.mutuallyExclusive("targetScope", targetScope, "targetSystem", targetSystem);
      FhirUtilityR5.mutuallyExclusive("sourceCode", sourceCode, "targetCoding", targetCoding);
      FhirUtilityR5.mutuallyExclusive("sourceCode", sourceCode, "targetCode", targetCode);
      FhirUtilityR5.mutuallyExclusive("targetCode", sourceCode, "sourceCoding", targetCoding);
      FhirUtilityR5.mutuallyExclusive("sourceCoding", sourceCoding, "targetCoding", targetCoding);
      FhirUtilityR5.mutuallyExclusive("sourceCoding", sourceCoding, "system", system);
      FhirUtilityR5.mutuallyExclusive("targetCoding", targetCoding, "system", system);
      FhirUtilityR5.requireAtLeastOneOf(
          "sourceCode",
          sourceCode,
          "targetCode",
          targetCode,
          "sourceCoding",
          sourceCoding,
          "targetCoding",
          targetCoding);

      for (final String param :
          new String[] {"sourceCodableConcept", "targetCodableConcept", "dependency"}) {
        FhirUtilityR5.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR5.notSupported(request, "_has");
      }

      UriType systemToLookup = null;
      CodeType targetCodeToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (sourceCoding != null) {
        systemToLookup = sourceCoding.getSystemElement();
      }
      // targetCoding sent in as a UriType, so needs to be parsed to extract targetCode and system
      else if (targetCoding != null) {
        String urlPart = targetCoding.getValue().substring(0, targetCoding.getValue().indexOf("|"));
        targetCodeToLookup =
            new CodeType(
                targetCoding.getValue().substring(targetCoding.getValue().indexOf("|") + 1));
        systemToLookup = new UriType(urlPart);
      }

      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(null, null, systemToLookup, url, version, targetSystem);
      // Extract the mapsetcode from cm build the query
      final List<String> mapsetCodes = cm.stream().map(m -> m.getTitle()).toList();

      CodeType sourceCodeToLookup = null;
      if (sourceCode != null) {
        sourceCodeToLookup = sourceCode;
      } else if (sourceCoding != null) {
        sourceCodeToLookup = sourceCoding.getCodeElement();
      }

      if (targetCode != null) {
        targetCodeToLookup = new CodeType(targetCode.getValue()); // Convert UriType to CodeType
      }

      String query =
          buildFhirQueryString(sourceCodeToLookup, targetCodeToLookup, mapsetCodes, "AND");
      logger.debug("   Fhir query string = " + query);

      MappingResultList maps;

      SearchCriteria criteria = new SearchCriteria();
      // Set as high as we can, should not be more than 10000 in reality.
      // (Unlimited support?)
      criteria.setPageSize(10000);
      criteria.setFromRecord(0);

      maps = osSearchService.findConceptMappings(query, criteria);
      final List<Mapping> conceptMaps = maps.getMaps();

      if (!conceptMaps.isEmpty()) {
        final Mapping map = conceptMaps.get(0);
        params.addParameter("result", true);
        final Parameters.ParametersParameterComponent property =
            new Parameters.ParametersParameterComponent().setName("match");
        property.addPart().setName("equivalence").setValue(new StringType("equivalent"));
        params.addParameter(property);
        // NEW LOGIC: Return the opposite of what was searched for
        if (sourceCodeToLookup != null) {
          // User provided source code, return target code
          property
              .addPart()
              .setName("concept")
              .setValue(
                  new Coding(map.getTargetTerminology(), map.getTargetCode(), map.getTargetName()));
        } else if (targetCodeToLookup != null) {
          // User provided target code, return source code (reverse mapping)
          property
              .addPart()
              .setName("concept")
              .setValue(
                  new Coding(map.getSourceTerminology(), map.getSourceCode(), map.getSourceName()));
        }
      }
      if (!params.hasParameter()) {
        params.addParameter("result", false);
        params.addParameter("match", "none");
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to translate concept map", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Returns the concept map.
   *
   * @param id the id
   * @return the concept map
   * @throws Exception the exception
   */
  @Read
  public ConceptMap getConceptMap(@IdParam final IdType id) throws Exception {
    try {
      if (id.hasVersionIdPart()) {
        // If someone somehow passes a versioned ID to read, delegate to vread
        return vread(id);
      }
      final List<ConceptMap> candidates = findPossibleConceptMaps(id, null, null, null, null, null);
      for (final ConceptMap set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }

      throw FhirUtilityR5.exception(
          "Concept map not found = " + (id.getIdPart()), IssueType.NOTFOUND, 404);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR5.exception("Failed to get concept map", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Helper method to find possible concept maps.
   *
   * @param id the id
   * @param date the date
   * @param system the system
   * @param url the url
   * @param version the version
   * @param targetSystem the target
   * @return the list
   * @throws Exception the exception
   */
  private List<ConceptMap> findPossibleConceptMaps(
      final IdType id,
      final DateRangeParam date,
      final UriType system,
      final UriType url,
      final StringType version,
      final UriType targetSystem)
      throws Exception {
    try {
      final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);
      final Map<String, Terminology> map = new HashMap<>();
      for (final Terminology terminology : terms) {
        map.put(terminology.getTerminology(), terminology);
      }
      final List<Concept> mapsets = osQueryService.getMapsets(new IncludeParam("properties"));

      final List<ConceptMap> list = new ArrayList<>();
      // Find the matching mapsets
      for (final Concept mapset : mapsets) {
        final List<Property> props = mapset.getProperties();
        if (props.stream()
            .anyMatch(m -> m.getType().equals("downloadOnly") && m.getValue().equals("true"))) {
          continue;
        }
        final ConceptMap cm =
            FhirUtilityR5.toR5(
                map.get(mapset.getPropertyValue("sourceTerminology")),
                map.get(mapset.getPropertyValue("targetTerminology")),
                mapset);
        // Skip non-matching
        if (url != null && !url.getValue().equals(cm.getUrl())) {
          logger.debug("  SKIP url mismatch = " + cm.getUrl());
          continue;
        }
        if (id != null && !id.getIdPart().equals(cm.getId())) {
          logger.debug("  SKIP id mismatch = " + cm.getName());
          continue;
        }
        if (system != null && !system.getValue().startsWith(cm.getUrl())) {
          logger.debug("  SKIP system mismatch = " + cm.getUrl());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cm.getDate())) {
          logger.debug("  SKIP date mismatch = " + cm.getDate());
          continue;
        }
        if (version != null && !version.getValue().equals(cm.getVersion())) {
          logger.debug("  SKIP version mismatch = " + cm.getVersion());
          continue;
        }
        if (targetSystem != null
            && !targetSystem
                .getValue()
                .equals(cm.getTargetScopeUriType().getValue().replaceFirst("\\?fhir_vs$", ""))) {
          logger.debug("  SKIP target mismatch = " + cm.getTargetScopeUriType().getValue());
          continue;
        }

        list.add(cm);
      }
      return list;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR5.exception("Failed to find concept maps", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Helper method for building a query string for the source or target code for FHIR.
   *
   * @param sourceCode the code being translated
   * @param targetCode the target value set to be used for translation. Extracted from the system
   *     uri
   * @param mapsetCodes the system for the code that is being translated, if provided
   * @param operator the operator to use for the query
   * @return the query string
   * @throws Exception the exception
   */
  private String buildFhirQueryString(
      CodeType sourceCode, CodeType targetCode, List<String> mapsetCodes, String operator)
      throws Exception {
    // Check our required parameters are provided
    if (sourceCode == null && targetCode == null) {
      throw FhirUtilityR5.exception(
          "Either sourceCode or targetCode must be provided", IssueType.INVALID, 400);
    }

    List<String> clauses = new ArrayList<>();
    if (!mapsetCodes.isEmpty()) {
      // compose query string for source code and system uri
      clauses.add(
          "mapsetCode:("
              + String.join(" ", mapsetCodes.stream().map(c -> escape(c)).toList())
              + ")");
    }
    if (sourceCode != null) {
      // compose query string for source code and system uri
      clauses.add("sourceCode:\"" + escape(sourceCode.getValue()) + "\"");
      return ConceptUtils.composeQuery(operator, clauses);
    } else {
      // compose query for target code
      clauses.add("targetCode:\"" + escape(targetCode.getValue()) + "\"");
      return ConceptUtils.composeQuery(operator, clauses);
    }
  }

  /**
   * Gets the concept map history.
   *
   * @param id the id
   * @return the concept map history
   */
  @History(type = ConceptMap.class)
  public List<ConceptMap> getConceptMapHistory(@IdParam IdType id) {
    List<ConceptMap> history = new ArrayList<>();
    try {
      final List<ConceptMap> candidates = findPossibleConceptMaps(id, null, null, null, null, null);
      for (final ConceptMap cs : candidates) {
        if (id.getIdPart().equals(cs.getId())) {
          history.add(cs);
        }
      }
      if (history.isEmpty()) {
        throw FhirUtilityR5.exception(
            "Concept map not found = " + (id == null ? "null" : id.getIdPart()),
            IssueType.NOTFOUND,
            404);
      }
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR5.exception("Failed to get concept map", IssueType.EXCEPTION, 500);
    }

    // Make sure each ConceptMap has proper metadata for history
    for (ConceptMap cs : history) {
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
   * @return the concept map
   */
  @Read(version = true)
  public ConceptMap vread(@IdParam IdType versionedId) {
    String resourceId = versionedId.getIdPart();
    String versionId = versionedId.getVersionIdPart(); // "1"

    logger.info("Looking for resource: {} version: {}", resourceId, versionId);

    try {
      // If no version is specified in a vread call, this shouldn't happen
      // but if it does, delegate to regular read
      if (!versionedId.hasVersionIdPart()) {
        logger.warn("VRead called without version ID, delegating to regular read");
        return getConceptMap(new org.hl7.fhir.r5.model.IdType(versionedId.getIdPart()));
      }

      final List<ConceptMap> candidates =
          findPossibleConceptMaps(versionedId, null, null, null, null, null);
      logger.info("Found {} candidates", candidates.size());

      for (final ConceptMap cs : candidates) {
        String csId = cs.getId();
        String csVersionId = cs.getMeta() != null ? cs.getMeta().getVersionId() : null;

        logger.info("Checking candidate: id={}, versionId={}", csId, csVersionId);

        if (resourceId.equals(csId)) {
          // If the ConceptMap doesn't have a version ID, treat it as version "1"
          String effectiveVersionId = (csVersionId != null) ? csVersionId : "1";

          if (versionId.equals(effectiveVersionId)) {
            // Make sure the returned ConceptMap has the version ID set
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
          "Concept map version not found: " + resourceId + " version " + versionId,
          IssueType.NOTFOUND,
          404);
    } catch (final FHIRServerResponseException e) {
      throw e; // Re-throw FHIR exceptions as-is
    } catch (final Exception e) {
      logger.error("Unexpected exception in vread", e);
      throw FhirUtilityR5.exception("Failed to get concept map version", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Apply sorting to the list of ConceptMaps if requested.
   *
   * @param list the list to sort
   * @param sort the sort parameter
   */
  private void applySorting(final List<ConceptMap> list, final StringParam sort) {
    if (sort == null || sort.getValue() == null || sort.getValue().trim().isEmpty()) {
      return;
    }

    try {
      final String sortValue = sort.getValue().trim();
      final boolean descending = sortValue.startsWith("-");
      final String field = descending ? sortValue.substring(1) : sortValue;

      // Validate supported fields
      final List<String> supportedFields =
          Arrays.asList("name", "title", "publisher", "date", "url");
      if (!supportedFields.contains(field)) {
        throw FhirUtilityR5.exception(
            "Unsupported sort field: "
                + field
                + ". Supported fields: "
                + String.join(", ", supportedFields),
            IssueType.INVALID,
            400);
      }

      final Comparator<ConceptMap> comparator = getConceptMapComparator(field);
      if (descending) {
        Collections.sort(list, comparator.reversed());
      } else {
        Collections.sort(list, comparator);
      }
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR5.exception(
          "Error processing sort parameter: " + e.getMessage(), IssueType.INVALID, 400);
    }
  }

  /**
   * Get comparator for ConceptMap sorting.
   *
   * @param field the field to sort by
   * @return the comparator
   */
  private Comparator<ConceptMap> getConceptMapComparator(final String field) {
    switch (field) {
      case "name":
        return Comparator.comparing(
            cm -> cm.getName() != null ? cm.getName().toLowerCase() : "",
            Comparator.nullsLast(String::compareTo));
      case "title":
        return Comparator.comparing(
            cm -> cm.getTitle() != null ? cm.getTitle().toLowerCase() : "",
            Comparator.nullsLast(String::compareTo));
      case "publisher":
        return Comparator.comparing(
            cm -> cm.getPublisher() != null ? cm.getPublisher().toLowerCase() : "",
            Comparator.nullsLast(String::compareTo));
      case "date":
        return Comparator.comparing(ConceptMap::getDate, Comparator.nullsLast(Date::compareTo));
      case "url":
        return Comparator.comparing(
            cm -> cm.getUrl() != null ? cm.getUrl().toLowerCase() : "",
            Comparator.nullsLast(String::compareTo));
      default:
        throw new IllegalArgumentException("Unsupported sort field: " + field);
    }
  }
}
