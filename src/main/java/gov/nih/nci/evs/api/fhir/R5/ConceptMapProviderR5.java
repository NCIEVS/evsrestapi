package gov.nih.nci.evs.api.fhir.R5;

import static gov.nih.nci.evs.api.service.ElasticSearchServiceImpl.escape;

import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.model.api.annotation.Description;
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
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import gov.nih.nci.evs.api.util.FhirUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ConceptMap;
import org.hl7.fhir.r5.model.IdType;
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
  @Autowired ElasticQueryService esQueryService;

  /** the elastic search service. */
  @Autowired ElasticSearchService esSearchService;

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
          final NumberParam offset)
      throws Exception {
    try {
      FhirUtilityR5.notSupportedSearchParams(request);

      final List<Concept> mapsets = esQueryService.getMapsets(new IncludeParam("properties"));

      final List<ConceptMap> list = new ArrayList<>();
      for (final Concept mapset : mapsets) {
        final List<Property> props = mapset.getProperties();
        if (props.stream()
            .anyMatch(m -> m.getType().equals("downloadOnly") && m.getValue().equals("true"))) {
          continue;
        }
        final ConceptMap cm = FhirUtilityR5.toR5(mapset);
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
   * @param conceptMap The concept map is provided directly as part of the request.
   * @param conceptMapVersion The identifier that is used to identify a specific version of the
   *     concept map to be used
   * @param sourceCode The code that is to be translated. If a code is provided, a system must be
   *     provided.
   * @param system The system for the code that is to be translated.
   * @param version The version of the system, if one was provided in the source data.
   * @param sourceScope Limits the scope of the $translate operation to source codes that are
   *     members of this value set.
   * @param sourceCoding A coding to translate.
   * @param sourceCodeableConcept A full codeableConcept to validate.
   * @param targetCode The target code that is to be translated to. If a code is provided, a system
   *     must be provided.
   * @param targetCoding A target coding to translate to.
   * @param targetCodeableConcept A full codeableConcept to validate.
   * @param targetScope Limits the scope of the $translate operation to target codes that are
   *     members of this value set.
   * @param targetSystem Identifies a target code system in which a mapping is sought. This
   *     parameter is an alternative to the targetScope parameter - only one is required.
   * @param dependency The value for the dependency.
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
      // @OperationParam(name = "conceptMap") final ConceptMap conceptMap,
      @OperationParam(name = "conceptMapVersion") final StringType conceptMapVersion,
      @OperationParam(name = "sourceCode") final CodeType sourceCode,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "sourceScope") final UriType sourceScope,
      // @OperationParam(name = "sourceCoding") final Coding sourceCoding,
      // @OperationParam(name = "sourceCodeableConcept") final CodeableConcept
      // sourceCodeableConcept,
      @OperationParam(name = "targetCode") final UriType targetCode,
      // @OperationParam(name = "targetCoding") final UriType targetCoding,
      // @OperationParam(name = "targetCodeableConcept") final CodeableConcept
      // targetCodeableConcept,
      @OperationParam(name = "targetScope") final UriType targetScope,
      @OperationParam(name = "targetSystem") final UriType targetSystem
      // @OperationParam(name = "dependency") final String dependency
      ) throws Exception {
    // Check if request is POST, throw error as we don't support POST calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE,
          IssueType.NOTSUPPORTED,
          405);
    }

    try {
      FhirUtilityR5.mutuallyRequired(sourceCode, "sourceCode", system, "system");
      FhirUtilityR5.mutuallyRequired(targetCode, "targetCode", system, "system");
      FhirUtilityR5.mutuallyExclusive(targetScope, "targetScope", targetSystem, "targetSystem");

      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(null, null, system, url, version, targetSystem);
      // Extract the mapsetcode from cm build the query
      final List<String> mapsetCodes = cm.stream().map(m -> m.getTitle()).toList();

      // Build a string query to search for the source code and target code
      String query = buildFhirQueryString(sourceCode, targetCode, mapsetCodes, "AND");
      logger.debug("   Fhir query string = " + query);

      MappingResultList maps;

      SearchCriteria criteria = new SearchCriteria();
      // Set as high as we can, should not be more than 10000 in reality.
      // (Unlimited support?)
      criteria.setPageSize(10000);
      criteria.setFromRecord(0);

      maps = esSearchService.findConceptMappings(query, criteria);
      final List<Mapping> conceptMaps = maps.getMaps();

      if (!conceptMaps.isEmpty()) {
        final Mapping map = conceptMaps.get(0);
        params.addParameter("result", true);
        final Parameters.ParametersParameterComponent property =
            new Parameters.ParametersParameterComponent().setName("match");
        property.addPart().setName("equivalence").setValue(new StringType("equivalent"));
        params.addParameter(property);
        if (sourceCode != null) {
          property
              .addPart()
              .setName("concept")
              .setValue(
                  new Coding(map.getSourceTerminology(), map.getSourceCode(), map.getSourceName()));
        } else {
          property
              .addPart()
              .setName("concept")
              .setValue(
                  new Coding(map.getTargetTerminology(), map.getTargetCode(), map.getTargetName()));
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
   * @param conceptMap The concept map is provided directly as part of the request.
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
   * @param sourceCodeableConcept A full codeableConcept to validate.
   * @param targetCode The target code that is to be translated to. If a code is provided, a system
   *     must be provided.
   * @param targetCoding A target coding to translate to.
   * @param targetCodeableConcept A full codeableConcept to validate.
   * @param targetScope Limits the scope of the $translate operation to target codes that are
   *     members of this value set.
   * @param targetSystem Identifies a target code system in which a mapping is sought. This
   *     parameter is an alternative to the targetScope parameter - only one is required.
   * @param dependency The value for this dependency.
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
      // @OperationParam(name = "conceptMap") final ConceptMap conceptMap,
      @OperationParam(name = "conceptMapVersion") final StringType conceptMapVersion,
      @OperationParam(name = "sourceCode") final CodeType sourceCode,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "sourceScope") final UriType sourceScope,
      // @OperationParam(name = "sourceCoding") final Coding sourceCoding,
      // @OperationParam(name = "codeableConcept") final CodeableConcept
      // sourceCodeableConcept,
      @OperationParam(name = "targetCode") final UriType targetCode,
      // @OperationParam(name = "targetCoding") final UriType targetCoding,
      // @OperationParam(name = "targetCodeableConcept") final CodeableConcept
      // targetCodeableConcept,
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
      FhirUtilityR5.mutuallyRequired(sourceCode, "sourceCode", system, "system");
      FhirUtilityR5.mutuallyRequired(targetCode, "targetCode", system, "system");
      FhirUtilityR5.mutuallyExclusive(targetScope, "targetScope", targetSystem, "targetSystem");

      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(null, null, system, url, version, targetSystem);
      // Extract the mapsetcode from cm build the query
      final List<String> mapsetCodes = cm.stream().map(m -> m.getTitle()).toList();

      // Build a string query to search for the source code and target code
      String query = buildFhirQueryString(sourceCode, targetCode, mapsetCodes, "AND");
      logger.debug("   Fhir query string = " + query);

      MappingResultList maps;

      SearchCriteria criteria = new SearchCriteria();
      // Set as high as we can, should not be more than 10000 in reality.
      // (Unlimited support?)
      criteria.setPageSize(10000);
      criteria.setFromRecord(0);

      maps = esSearchService.findConceptMappings(query, criteria);
      final List<Mapping> conceptMaps = maps.getMaps();

      if (!conceptMaps.isEmpty()) {
        final Mapping map = conceptMaps.get(0);
        params.addParameter("result", true);
        final Parameters.ParametersParameterComponent property =
            new Parameters.ParametersParameterComponent().setName("match");
        property.addPart().setName("equivalence").setValue(new StringType("equivalent"));
        params.addParameter(property);
        if (sourceCode != null) {
          property
              .addPart()
              .setName("concept")
              .setValue(
                  new Coding(map.getSourceTerminology(), map.getSourceCode(), map.getSourceName()));
        } else {
          property
              .addPart()
              .setName("concept")
              .setValue(
                  new Coding(map.getTargetTerminology(), map.getTargetCode(), map.getTargetName()));
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
   * @param details the details
   * @param id the id
   * @return the concept map
   * @throws Exception the exception
   */
  @Read
  public ConceptMap getConceptMap(final ServletRequestDetails details, @IdParam final IdType id)
      throws Exception {
    try {

      final List<ConceptMap> candidates = findPossibleConceptMaps(id, null, null, null, null, null);
      for (final ConceptMap set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }

      throw FhirUtilityR5.exception(
          "Concept map not found = " + (id == null ? "null" : id.getIdPart()),
          IssueType.NOTFOUND,
          404);

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
      final List<Concept> mapsets = esQueryService.getMapsets(new IncludeParam("properties"));
      final List<ConceptMap> list = new ArrayList<>();
      // Find the matching mapsets
      for (final Concept mapset : mapsets) {
        final List<Property> props = mapset.getProperties();
        if (props.stream()
            .anyMatch(m -> m.getType().equals("downloadOnly") && m.getValue().equals("true"))) {
          continue;
        }
        final ConceptMap cm = FhirUtilityR5.toR5(mapset);
        // Skip non-matching
        if (url != null && !url.getValue().equals(cm.getUrl())) {
          logger.debug("  SKIP url mismatch = " + cm.getUrl());
          continue;
        }
        if (id != null && !id.getIdPart().equals(cm.getId())) {
          logger.debug("  SKIP id mismatch = " + cm.getName());
          continue;
        }
        if (system != null && !system.getValue().equals(cm.getUrl())) {
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
   * Helper method for building a query string for the source or target code for FHIR
   *
   * @param sourceCode the code being translated
   * @param targetCode the target value set to be used for translation. Extracted from the system
   *     uri
   * @param mapsetCodes the system for the code that is being translated, if provided
   * @param operator the operator to use for the query
   * @return the query string
   */
  private String buildFhirQueryString(
      CodeType sourceCode, UriType targetCode, List<String> mapsetCodes, String operator)
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
}
