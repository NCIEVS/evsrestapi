package gov.nih.nci.evs.api.fhir.R4;

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
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** FHIR R4 ConceptMap provider. */
@Component
public class ConceptMapProviderR4 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(ConceptMapProviderR4.class);

  /** the query service. */
  @Autowired ElasticQueryService esQueryService;

  /** the elastic search service. */
  @Autowired ElasticSearchService esSearchService;

  /** The code to translate. */
  String codeToTranslate = "";

  /**
   * Perform the lookup in the instance map.
   *
   * <p>see https://hl7.org/fhir/R4/conceptmap-operation-translate.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param url A canonical URL for a concept map. The server must know the concept map (
   * @param conceptMapVersion The identifier that is used to identify a specific version of the
   *     concept map to be used for the translation.
   * @param code The code that is to be translated. If a code is provided, a system must be provided
   * @param system The system for the code that is to be translated
   * @param version The version of the system, if one was provided in the source data
   * @param source Identifies the value set used when the concept (system/code pair) was chosen.
   *     Optional because user may not always know it
   * @param coding A coding to translate
   * @param target Identifies the value set in which a translation is sought. If there's no target
   *     specified, the server should return all known translations, along with their source
   * @param targetSystem identifies a target code system in which a mapping is sought. This
   *     parameter is an alternative to the target parameter.
   * @param dependency the element for the dependency, may help produce the correct mapping
   * @param reverse This parameter reverses the meaning of the source and target parameters
   * @return the parameters
   * @throws Exception the exception
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
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "source") final UriType source,
      @OperationParam(name = "coding") final Coding coding,
      // @OperationParam(name = "codeableConcept") final CodeableConcept
      // codeableConcept,
      @OperationParam(name = "target") final UriType target,
      @OperationParam(name = "targetSystem") final UriType targetSystem,
      // @OperationParam(name = "dependency") final UriType dependency,
      @OperationParam(name = "reverse", type = BooleanType.class) final BooleanType reverse)
      throws Exception {
    // check if request is a post, throw exception as we don't support post
    // calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.mutuallyExclusive("target", target, "targetSystem", targetSystem);
      for (final String param : new String[] {"codableConcept", "dependency"}) {
        FhirUtilityR4.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR4.notSupported(request, "_has");
      }
      
      UriType systemToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (coding != null) {
        systemToLookup = coding.getSystemElement();
      }
      
      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(null, null, systemToLookup, url, version, source, target, targetSystem);
      // Extract the mapsetcode from cm build the query
      final List<String> mapsetCodes = cm.stream().map(m -> m.getTitle()).toList();

      // Build a string query to search for the code/target
      CodeType sourceCodeToLookup = null;
      if (code != null) {
    	  sourceCodeToLookup = code;
      } else if (coding != null) {
    	  sourceCodeToLookup = coding.getCodeElement();
      }
      
      String query = buildFhirQueryString(sourceCodeToLookup, mapsetCodes, reverse, "AND");
      logger.debug("   Fhir query string = " + query);

      MappingResultList maps;

      SearchCriteria criteria = new SearchCriteria();
      // Set as high as we can, should not be more than 10000 in reality.
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
        if (reverse != null && reverse.getValue()) {
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
        params.addParameter(property);
      }
      if (!params.hasParameter()) {
        params.addParameter("result", false);
        params.addParameter("match", "none");
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception(
          "Failed to translate concept map", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Perform the lookup in the implicit map.
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param url A canonical URL for a concept map. The server must know the concept map.
   * @param conceptMapVersion The identifier that is used to identify a specific version of the
   *     concept map to be used for the translation.
   * @param code The code that is to be translated. If a code is provided, a system must be provided
   * @param system The system for the code that is to be translated
   * @param version The version of the system, if one was provided in the source data
   * @param source Identifies the value set used when the concept (system/code pair) was chosen.
   *     Optional because user may not always know it
   * @param coding A coding to translate
   * @param target Identifies the value set in which a translation is sought. If there's no target
   *     specified, the server should return all known translations, along with their source
   * @param targetSystem identifies a target code system in which a mapping is sought. This
   *     parameter is an alternative to the target parameter.
   * @param dependency the element for the dependency, may help produce the correct mapping
   * @param reverse This parameter reverses the meaning of the source and target parameters
   * @return the parameters
   * @throws Exception the exception
   *     <p>no support for dependency parameter
   * @see <a href= "https://hl7.org/fhir/R4/conceptmap-operation-translate.html">conceptmap
   *     operation translate</a>
   */
  @Operation(name = JpaConstants.OPERATION_TRANSLATE, idempotent = true)
  public Parameters translateImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "conceptMapVersion") final StringType conceptMapVersion,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "source") final UriType source,
      @OperationParam(name = "coding") final Coding coding,
      // @OperationParam(name = "codeableConcept") final CodeableConcept
      // codeableConcept,
      @OperationParam(name = "target") final UriType target,
      @OperationParam(name = "targetSystem") final UriType targetSystem,
      // @OperationParam(name = "dependency") final UriType dependency,
      @OperationParam(name = "reverse", type = BooleanType.class) final BooleanType reverse)
      throws Exception {
    // check if request is a post, throw exception as we don't support post
    // calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.mutuallyExclusive("target", target, "targetSystem", targetSystem);
      for (final String param : new String[] {"codableConcept", "dependency"}) {
        FhirUtilityR4.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR4.notSupported(request, "_has");
      }
      
      UriType systemToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (coding != null) {
        systemToLookup = coding.getSystemElement();
      }
      
      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(null, null, systemToLookup, url, version, source, target, targetSystem);
      // Extract the mapsetcode from cm build the query
      final List<String> mapsetCodes = cm.stream().map(m -> m.getTitle()).toList();

      // Build a string query to search for the code/target
      CodeType sourceCodeToLookup = null;
      if (code != null) {
    	  sourceCodeToLookup = code;
      } else if (coding != null) {
    	  sourceCodeToLookup = coding.getCodeElement();
      }
      
      String query = buildFhirQueryString(sourceCodeToLookup, mapsetCodes, reverse, "AND");
      logger.debug("   Fhir query string = " + query);

      // final List<ConceptMap> cm =
      // findPossibleConceptMaps(null, null, system, url, version, source,
      // target);
      MappingResultList maps;

      SearchCriteria criteria = new SearchCriteria();
      // Set as high as we can, should not be more than 10000 in reality.
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
        if (reverse != null && reverse.getValue()) {
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

        params.addParameter(property);
      }
      if (!params.hasParameter()) {
        params.addParameter("result", false);
        params.addParameter("match", "none");
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception(
          "Failed to translate concept map", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /* see superclass */
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
      FhirUtilityR4.notSupportedSearchParams(request);

      final List<Concept> mapsets = esQueryService.getMapsets(new IncludeParam("properties"));

      final List<ConceptMap> list = new ArrayList<>();
      for (final Concept mapset : mapsets) {
        final List<Property> props = mapset.getProperties();
        if (props.stream()
            .anyMatch(m -> m.getType().equals("downloadOnly") && m.getValue().equals("true"))) {
          continue;
        }
        final ConceptMap cm = FhirUtilityR4.toR4(mapset);
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

      return FhirUtilityR4.makeBundle(request, list, count, offset);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR4.exception(
          "Failed to find concept maps", OperationOutcome.IssueType.EXCEPTION, 500);
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
   * @param source the source
   * @param target the target
   * @return the list
   * @throws Exception the exception
   */
  private List<ConceptMap> findPossibleConceptMaps(
      final IdType id,
      final DateRangeParam date,
      final UriType system,
      final UriType url,
      final StringType version,
      final UriType source,
      final UriType target,
      final UriType targetSystem)
      throws Exception {
    try {
      // FhirUtilityR4.notSupportedSearchParams(request);

      // If no ID and no url are specified, no code systems match
      if (id == null && url == null && system == null) {
        return new ArrayList<>(0);
      }

      final List<Concept> mapsets = esQueryService.getMapsets(new IncludeParam("properties"));

      final List<ConceptMap> list = new ArrayList<>();
      for (final Concept mapset : mapsets) {
        final List<Property> props = mapset.getProperties();
        if (props.stream()
            .anyMatch(m -> m.getType().equals("downloadOnly") && m.getValue().equals("true"))) {
          continue;
        }
        final ConceptMap cm = FhirUtilityR4.toR4(mapset);
        // Skip non-matching
        if (url != null && !url.getValue().equals(cm.getUrl())) {
          logger.debug("  SKIP url mismatch = " + cm.getUrl());
          continue;
        }
        if (id != null && !id.getIdPart().equals(cm.getId())) {
          logger.debug("  SKIP id mismatch = " + cm.getName());
          continue;
        }
        if (system != null && !system.getValue().equals(cm.getSourceUriType().getValue())) {
          logger.debug("  SKIP system mismatch = " + cm.getName());
          continue;
        }
        if (targetSystem != null
            && !targetSystem.getValue().equals(cm.getTargetUriType().getValue())) {
          logger.debug("  SKIP targetSystem mismatch = " + cm.getName());
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
        if (source != null
            && !source.getValue().equals(cm.getSourceUriType().getValue() + "?fhir_vs")) {
          logger.debug("  SKIP source mismatch = " + cm.getVersion());
          continue;
        }
        if (target != null
            && !target.getValue().equals(cm.getTargetUriType().getValue() + "?fhir_vs")) {
          logger.debug("  SKIP target mismatch = " + cm.getVersion());
          continue;
        }

        list.add(cm);
      }
      return list;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR4.exception(
          "Failed to find concept maps", OperationOutcome.IssueType.EXCEPTION, 500);
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

      final List<ConceptMap> candidates =
          findPossibleConceptMaps(id, null, null, null, null, null, null, null);
      for (final ConceptMap set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }

      throw FhirUtilityR4.exception(
          "Concept map not found = " + (id == null ? "null" : id.getIdPart()),
          IssueType.NOTFOUND,
          404);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR4.exception(
          "Failed to get concept map", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Helper method for building a FHIR query string. If the code is not null, it will return a query
   *
   * @param code the code being translated
   * @param mapsetCodes target value set to be used for translations. Extracted from system uri
   * @param operator the operator to use for the query
   * @return
   */
  private String buildFhirQueryString(
      CodeType code, List<String> mapsetCodes, BooleanType reverse, String operator)
      throws Exception {
    // Check the required parameter is provided
    if (code == null) {
      throw FhirUtilityR4.exception(
          "Either code or target parameter is required", OperationOutcome.IssueType.REQUIRED, 400);
    }
    List<String> clauses = new ArrayList<>();
    if (!mapsetCodes.isEmpty()) {
      clauses.add(
          "mapsetCode:("
              + String.join(" ", mapsetCodes.stream().map(c -> escape(c)).toList())
              + ")");
    }
    if (reverse != null && reverse.booleanValue()) {
      // compose query string for source code and system uri
      clauses.add("targetCode:\"" + escape(code.getValue()) + "\"");
      return ConceptUtils.composeQuery(operator, clauses);
    } else {
      // compose query for target code
      clauses.add("sourceCode:\"" + escape(code.getValue()) + "\"");
      return ConceptUtils.composeQuery(operator, clauses);
    }
  }
}
