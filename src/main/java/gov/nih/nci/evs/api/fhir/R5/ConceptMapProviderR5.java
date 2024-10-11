package gov.nih.nci.evs.api.fhir.R5;

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
import gov.nih.nci.evs.api.model.Mappings;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import gov.nih.nci.evs.api.util.FhirUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
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

  /** The code to translate. */
  String codeToTranslate = "";

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
   * @param system the system
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
      @OptionalParam(name = "system") final StringParam system,
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
          logger.info("  SKIP url mismatch = " + cm.getUrl());
          continue;
        }
        if (id != null && !id.getValue().equals(cm.getId())) {
          logger.info("  SKIP id mismatch = " + cm.getName());
          continue;
        }
        if (system != null && !system.getValue().equals(cm.getName())) {
          logger.info("  SKIP system mismatch = " + cm.getName());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cm.getDate())) {
          logger.info("  SKIP date mismatch = " + cm.getDate());
          continue;
        }
        if (version != null && !FhirUtility.compareString(version, cm.getVersion())) {
          logger.info("  SKIP version mismatch = " + cm.getVersion());
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
   *    <a href="https://hl7.org/fhir/R5/conceptmap-operation-translate.html">Conceptmap operation ref </a>"
   * </pre>
   * @param request the servlet request
   * @param response the servlet response
   * @param details the servlet request details
   * @param id the lookup id
   * @param url the canonical URL for a concept map
   * @param conceptMap The concept map is provided directly as part of the request.
   * @param conceptMapVersion The identifier that is used to identify a specific version of the concept map to be used
   * @param sourceCode The code that is to be translated. If a code is provided, a system must be provided.
   * @param system The system for the code that is to be translated.
   * @param version The version of the system, if one was provided in the source data.
   * @param sourceScope Limits the scope of the $translate operation to source codes that are members of this value set.
   * @param sourceCoding A coding to translate.
   * @param sourceCodeableConcept A full codeableConcept to validate.
   * @param targetCode The target code that is to be translated to. If a code is provided, a system must be provided.
   * @param targetSystem Identifies a target code system in which a mapping is sought. This parameter is an alternative
   *                    to the targetScope parameter - only one is required.
   * @param targetCoding A target coding to translate to.
   * @param targetScope Limits the scope of the $translate operation to target codes that are members of this value set.
   * @param targetCodeableConcept A full codeableConcept to validate.
   * @param dependencyAttribute The attribute for this dependency.
   * @param dependencyValue The value for this dependency.
   * @return the parameters
   * @throws Exception throws exception if error occurs.
   *      <p>no support for dependency parameter
   */
  @Operation(name = JpaConstants.OPERATION_TRANSLATE, idempotent = true)
  public Parameters translateInstance(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "url") final UriType url,
//      @OperationParam(name = "conceptMap") final ConceptMap conceptMap,
      @OperationParam(name = "conceptMapVersion") final StringType conceptMapVersion,
      @OperationParam(name = "sourceCode") final CodeType sourceCode,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "source") final UriType sourceScope,
//      @OperationParam(name = "coding") final Coding sourceCoding,
//      @OperationParam(name = "codeableConcept") final CodeableConcept sourceCodeableConcept,
      @OperationParam(name = "target") final UriType targetCode,
//      @OperationParam(name = "targetCoding") final UriType targetCoding,
//      @OperationParam(name = "targetCodeableConcept") final CodeableConcept targetCodeableConcept,
      @OperationParam(name = "targetScope") final UriType targetScope,
      @OperationParam(name = "targetSystem") final UriType targetSystem
//      @OperationParam(name = "dependency") final String dependency
        )
      throws Exception {
    // Check if request is POST, throw error as we don't support POST calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method nor supported for " + JpaConstants.OPERATION_TRANSLATE,
          IssueType.NOTSUPPORTED,
          405);
    }

    try {
      FhirUtilityR5.mutuallyRequired(sourceCode, "sourceCode", system, "system");
      FhirUtilityR5.mutuallyRequired(targetCode, "targetCode", system, "system");
      FhirUtilityR5.mutuallyExclusive(targetScope, "targetScope", targetSystem, "targetSystem");

      codeToTranslate = sourceCode.getCode().toLowerCase();
      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(id, null, system, url, version, sourceScope, targetCode);
      for (final ConceptMap mapping : cm) {
        final List<Mappings> maps =
            esQueryService.getMapset(mapping.getTitle(), new IncludeParam("maps")).get(0).getMaps();
        List<Mappings> filteredMaps = new ArrayList<>();
        if (reverse != null && reverse.getValue()) {
          filteredMaps =
              maps.stream()
                  .filter(
                      m ->
                          m.getTargetCode().toLowerCase().contains(codeToTranslate)
                              || m.getTargetName()
                                  .toLowerCase()
                                  .matches("^" + Pattern.quote(codeToTranslate) + ".*")
                              || m.getTargetName()
                                  .toLowerCase()
                                  .matches(".*\\b" + Pattern.quote(codeToTranslate) + ".*"))
                  .collect(Collectors.toList());
        } else {
          filteredMaps =
              maps.stream()
                  .filter(
                      m ->
                          m.getSourceCode().toLowerCase().contains(codeToTranslate)
                              || m.getSourceName()
                                  .toLowerCase()
                                  .matches("^" + Pattern.quote(codeToTranslate) + ".*")
                              || m.getSourceName()
                                  .toLowerCase()
                                  .matches(".*\\b" + Pattern.quote(codeToTranslate) + ".*"))
                  .collect(Collectors.toList());
        }

        if (!filteredMaps.isEmpty()) {
          final Mappings map = filteredMaps.get(0);
          params.addParameter("result", true);
          final Parameters.ParametersParameterComponent property =
              new Parameters.ParametersParameterComponent().setName("match");
          property.addPart().setName("equivalence").setValue(new StringType("equivalent"));
          if (reverse != null && reverse.getValue()) {
            property
                .addPart()
                .setName("concept")
                .setValue(
                    new Coding(
                        map.getSourceTerminology(), map.getSourceCode(), map.getSourceName()));
          } else {
            property
                .addPart()
                .setName("concept")
                .setValue(
                    new Coding(
                        map.getTargetTerminology(), map.getTargetCode(), map.getTargetName()));
          }
          params.addParameter(property);
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
   *  <a href="https://hl7.org/fhir/R5/conceptmap-operation-translate.html">Conceptmap operation ref</a>
   * </pre>
   * @param request the servlet request.
   * @param response the servlet response.
   * @param details the servlet request details.
   * @param url A canonical URL for a concept map. The server must know the concept map.
//   * @param conceptMap The concept map is provided directly as part of the request.
   * @param conceptMapVersion The identifier that is used to identify a specific version of the concept map to be used
   *                          for the translation. This is an arbitrary value managed by the concept map author and is
   *                          not expected to be globally unique.
   * @param sourceCode The code that is to be translated. If a code is provided, a system must be provided.
   * @param system The system for the code that is to be translated.
   * @param version The version of the system, if one was provided in the source data.
   * @param sourceScope Limits the scope of the $translate operation to source codes that are members of this value set.
//   * @param sourceCoding A coding to translate.
//   * @param sourceCodeableConcept A full codeableConcept to validate.
   * @param targetCode The target code that is to be translated to. If a code is provided, a system must be provided.
   * @param targetSystem Identifies a target code system in which a mapping is sought. This parameter is an
   *                     alternative to the targetScope parameter - only one is required.
//   * @param targetCoding A target coding to translate to.
   * @param targetScope Limits the scope of the $translate operation to target codes that are members of this value set.
//   * @param targetCodeableConcept A full codeableConcept to validate.
//   * @param dependencyAttribute The attribute for this dependency.
//   * @param dependencyValue The value for this dependency.
   * @return the parameters
   * @throws Exception throws exception if error occurs.
   *      <p>no support for dependency parameter
   */
  @Operation(name = JpaConstants.OPERATION_TRANSLATE, idempotent = true)
  public Parameters translateImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      //@OperationParam(name = "conceptMap") final ConceptMap conceptMap,
      @OperationParam(name = "conceptMapVersion") final StringType conceptMapVersion,
      @OperationParam(name = "sourceCode") final CodeType sourceCode,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "sourceScope") final UriType sourceScope,
//      @OperationParam(name = "sourceCoding") final Coding sourceCoding,
//      @OperationParam(name = "codeableConcept") final CodeableConcept sourceCodeableConcept,
      @OperationParam(name = "targetCode") final UriType targetCode,
//      @OperationParam(name = "targetCoding") final UriType targetCoding,
//      @OperationParam(name = "targetCodeableConcept") final CodeableConcept targetCodeableConcept,
      @OperationParam(name = "targetScope") final UriType targetScope,
      @OperationParam(name = "targetSystem") final UriType targetSystem
//      @OperationParam(name = "dependency") final UriType dependency
      )
      throws Exception {
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

      codeToTranslate = sourceCode.getCode().toLowerCase();

      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(null, null, system, url, version, sourceScope, targetCode);
      for (final ConceptMap mapping : cm) {
        final List<Mappings> maps =
            esQueryService.getMapset(mapping.getTitle(), new IncludeParam("maps")).get(0).getMaps();
        List<Mappings> filteredMaps = new ArrayList<>();
        if (reverse != null && reverse.getValue()) {
          filteredMaps =
              maps.stream()
                  .filter(
                      m ->
                          m.getTargetCode().toLowerCase().contains(codeToTranslate)
                              || m.getTargetName()
                                  .toLowerCase()
                                  .matches("^" + Pattern.quote(codeToTranslate) + ".*")
                              || m.getTargetName()
                                  .toLowerCase()
                                  .matches(".*\\b" + Pattern.quote(codeToTranslate) + ".*"))
                  .collect(Collectors.toList());
        } else {
          filteredMaps =
              maps.stream()
                  .filter(
                      m ->
                          m.getSourceCode().toLowerCase().contains(codeToTranslate)
                              || m.getSourceName()
                                  .toLowerCase()
                                  .matches("^" + Pattern.quote(codeToTranslate) + ".*")
                              || m.getSourceName()
                                  .toLowerCase()
                                  .matches(".*\\b" + Pattern.quote(codeToTranslate) + ".*"))
                  .collect(Collectors.toList());
        }

        if (!filteredMaps.isEmpty()) {
          final Mappings map = filteredMaps.get(0);
          params.addParameter("result", true);
          final Parameters.ParametersParameterComponent property =
              new Parameters.ParametersParameterComponent().setName("match");
          property.addPart().setName("equivalence").setValue(new StringType("equivalent"));
          if (reverse != null && reverse.getValue()) {
            property
                .addPart()
                .setName("concept")
                .setValue(
                    new Coding(
                        map.getSourceTerminology(), map.getSourceCode(), map.getSourceName()));
          } else {
            property
                .addPart()
                .setName("concept")
                .setValue(
                    new Coding(
                        map.getTargetTerminology(), map.getTargetCode(), map.getTargetName()));
          }

          params.addParameter(property);
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

      final List<ConceptMap> candidates =
          findPossibleConceptMaps(id, null, null, null, null, null, null);
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
   * @param source the source
   * @param target the target
   * @return the list
   * @throws Exception the exception
   */
  private List<ConceptMap> findPossibleConceptMaps(
      @OptionalParam(name = "_id") final IdType id,
      @OptionalParam(name = "date") final DateRangeParam date,
      @OptionalParam(name = "system") final UriType system,
      @OptionalParam(name = "url") final UriType url,
      @OptionalParam(name = "version") final StringType version,
      @OptionalParam(name = "version") final UriType source,
      @OptionalParam(name = "version") final UriType target)
      throws Exception {
    try {

      // If no ID and no url are specified, no code systems match
      if (id == null && url == null && system == null) {
        return new ArrayList<>(0);
      }

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
          logger.info("  SKIP url mismatch = " + cm.getUrl());
          continue;
        }
        if (id != null && !id.getIdPart().equals(cm.getId())) {
          logger.info("  SKIP id mismatch = " + cm.getName());
          continue;
        }
        if (system != null && !system.getValue().equals(cm.getName())) {
          logger.info("  SKIP system mismatch = " + cm.getName());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cm.getDate())) {
          logger.info("  SKIP date mismatch = " + cm.getDate());
          continue;
        }
        if (version != null && !version.getValue().equals(cm.getVersion())) {
          logger.info("  SKIP version mismatch = " + cm.getVersion());
          continue;
        }
        if (source != null
            && !source.getValue().equals(cm.getGroup().get(0).getSourceElement().getValue())) {
          logger.info("  SKIP source mismatch = " + cm.getVersion());
          continue;
        }
        if (target != null
            && !target.getValue().equals(cm.getGroup().get(0).getTargetElement().getValue())) {
          logger.info("  SKIP target mismatch = " + cm.getVersion());
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
}
