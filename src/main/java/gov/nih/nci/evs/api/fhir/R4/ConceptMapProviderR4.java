package gov.nih.nci.evs.api.fhir.R4;

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
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
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

/** The ConceptMap provider. */
@Component
public class ConceptMapProviderR4 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(ConceptMapProviderR4.class);

  /** the query service. */
  @Autowired ElasticQueryService esQueryService;

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
   * @param url the url
   * @param conceptMap the concept map
   * @param conceptMapVersion the concept map version
   * @param code the code
   * @param system the system
   * @param version the version
   * @param source the source
   * @param coding the coding
   * @param codeableConcept the codeable concept
   * @param target the target
   * @param targetsystem the targetsystem
   * @param dependency_element the dependency element
   * @param dependency_concept the dependency concept
   * @param reverse the reverse
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
      @OperationParam(name = "conceptMap") final ConceptMap conceptMap,
      @OperationParam(name = "conceptMapVersion") final StringType conceptMapVersion,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "source") final UriType source,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      @OperationParam(name = "target") final UriType target,
      @OperationParam(name = "targetsystem") final UriType targetsystem,
      @OperationParam(name = "dependency.element") final UriType dependency_element,
      @OperationParam(name = "dependency.concept") final CodeableConcept dependency_concept,
      @OperationParam(name = "reverse", type = BooleanType.class) final BooleanType reverse)
      throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.required("code", code);
      FhirUtilityR4.notSupported("conceptMap", conceptMap);
      FhirUtilityR4.notSupported("conceptMapVersion", conceptMapVersion);
      FhirUtilityR4.notSupported("coding", coding);
      FhirUtilityR4.notSupported("codeableConcept", codeableConcept);
      FhirUtilityR4.notSupported("targetsystem", targetsystem);
      FhirUtilityR4.notSupported("dependency_element", dependency_element);
      FhirUtilityR4.notSupported("dependency_concept", dependency_concept);
      codeToTranslate = code.getCode().toLowerCase();
      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(id, null, system, url, version, source, target);
      for (final ConceptMap mapping : cm) {
        final List<gov.nih.nci.evs.api.model.ConceptMap> maps =
            esQueryService.getMapset(mapping.getTitle(), new IncludeParam("maps")).get(0).getMaps();
        List<gov.nih.nci.evs.api.model.ConceptMap> filteredMaps = new ArrayList<>();
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
          final gov.nih.nci.evs.api.model.ConceptMap map = filteredMaps.get(0);
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
      throw FhirUtilityR4.exception(
          "Failed to translate concept map", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Perform the lookup in the implicit map.
   *
   * @see <a href="https://hl7.org/fhir/R4/conceptmap-operation-translate.html">conceptmap operation
   *     translate</a>
   * @param request the request
   * @param response the response
   * @param details the details
   * @param url the url
   * @param conceptMap the concept map
   * @param conceptMapVersion the concept map version
   * @param code the code
   * @param system the system
   * @param version the version
   * @param source the source
   * @param coding the coding
   * @param codeableConcept the codeable concept
   * @param target the target
   * @param targetsystem the targetsystem
   * @param dependency_element the dependency element
   * @param dependency_concept the dependency concept
   * @param reverse the reverse
   * @return the parameters
   * @throws Exception the exception
   *     <p>no support for dependency parameter
   */
  @Operation(name = JpaConstants.OPERATION_TRANSLATE, idempotent = true)
  public Parameters translateImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "conceptMap") final ConceptMap conceptMap,
      @OperationParam(name = "conceptMapVersion") final StringType conceptMapVersion,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "source") final UriType source,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "codeableConcept") final CodeableConcept codeableConcept,
      @OperationParam(name = "target") final UriType target,
      @OperationParam(name = "targetsystem") final UriType targetsystem,
      @OperationParam(name = "dependency.element") final UriType dependency_element,
      @OperationParam(name = "dependency.concept") final CodeableConcept dependency_concept,
      @OperationParam(name = "reverse", type = BooleanType.class) final BooleanType reverse)
      throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_TRANSLATE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.required("code", code);
      FhirUtilityR4.notSupported("conceptMap", conceptMap);
      FhirUtilityR4.notSupported("conceptMapVersion", conceptMapVersion);
      FhirUtilityR4.notSupported("coding", coding);
      FhirUtilityR4.notSupported("codeableConcept", codeableConcept);
      FhirUtilityR4.notSupported("targetsystem", targetsystem);
      FhirUtilityR4.notSupported("dependency_element", dependency_element);
      FhirUtilityR4.notSupported("dependency_concept", dependency_concept);
      codeToTranslate = code.getCode().toLowerCase();
      final Parameters params = new Parameters();
      final List<ConceptMap> cm =
          findPossibleConceptMaps(
              null, null, system, url, version, source, target);
      for (final ConceptMap mapping : cm) {
        final List<gov.nih.nci.evs.api.model.ConceptMap> maps =
            esQueryService.getMapset(mapping.getTitle(), new IncludeParam("maps")).get(0).getMaps();
        List<gov.nih.nci.evs.api.model.ConceptMap> filteredMaps = new ArrayList<>();
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

        if (filteredMaps.size() > 0) {
          final gov.nih.nci.evs.api.model.ConceptMap map = filteredMaps.get(0);
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
   * @param id the id
   * @param date the date
   * @param system the system
   * @param url the url
   * @param version the version
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
          NumberParam count,
      @Description(shortDefinition = "Start offset, used when reading a next page")
          @OptionalParam(name = "_offset")
          NumberParam offset)
      throws Exception {
    try {
      FhirUtilityR4.notSupportedSearchParams(request);

      final List<Concept> mapsets = esQueryService.getMapsets(new IncludeParam("properties"));

      final List<ConceptMap> list = new ArrayList<>();
      for (final Concept mapset : mapsets) {
        final List<Property> props = mapset.getProperties();
        if (props.stream()
            .filter(m -> m.getType().equals("downloadOnly") && m.getValue().equals("true"))
            .findAny()
            .isPresent()) {
          continue;
        }
        final ConceptMap cm = FhirUtilityR4.toR4(mapset);
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
      @OptionalParam(name = "_id") final IdType id,
      @OptionalParam(name = "date") final DateRangeParam date,
      @OptionalParam(name = "system") final UriType system,
      @OptionalParam(name = "url") final UriType url,
      @OptionalParam(name = "version") final StringType version,
      @OptionalParam(name = "version") final UriType source,
      @OptionalParam(name = "version") final UriType target)
      throws Exception {
    try {
      //      FhirUtilityR4.notSupportedSearchParams(request);

      // If no ID and no url are specified, no code systems match
      if (id == null && url == null && system == null) {
        return new ArrayList<>(0);
      }

      final List<Concept> mapsets = esQueryService.getMapsets(new IncludeParam("properties"));

      final List<ConceptMap> list = new ArrayList<>();
      for (final Concept mapset : mapsets) {
        final List<Property> props = mapset.getProperties();
        if (props.stream()
            .filter(m -> m.getType().equals("downloadOnly") && m.getValue().equals("true"))
            .findAny()
            .isPresent()) {
          continue;
        }
        final ConceptMap cm = FhirUtilityR4.toR4(mapset);
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
          findPossibleConceptMaps(id, null, null, null, null, null, null);
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
}
