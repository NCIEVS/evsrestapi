package gov.nih.nci.evs.api.fhir;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.service.ElasticQueryService;

/**
 * The ConceptMap provider.
 */
@Component
public class ConceptMapProviderR4 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(ConceptMapProviderR4.class);

  /** the query service */
  @Autowired
  ElasticQueryService queryService;

  String codeToTranslate = "";

  /**
   * Perform the lookup in the instance map.
   * 
   * <pre>
   * Parameters for all resources 
   *   used: _id
   *   not used: _content, _filter, _has, _in, _language, _lastUpdated, 
   *             _list, _profile, _query, _security, _source, _tag, _text, _type
   * https://hl7.org/fhir/R4/conceptmap-operation-translate.html
   * The following parameters in the operation are not used
   * &#64;OptionalParam(name="dependency") ?? dependency
   * &#64;OperationParam(name = "conceptMap") ConceptMap conceptMap, 
   * &#64;OperationParam(name = "conceptMapVersion") String conceptMapVersion,
   * &#64;OperationParam(name = "source") UriType source,
   * &#64;OperationParam(name = "target") String target,
   * &#64;OperationParam(name = "reverse") BooleanType reverse
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param url the url
   * @param code the code
   * @param system the system
   * @param version the version
   * @param coding the coding
   * @param codeableConcept the codeable concept
   * @param targetSystem the target system
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$translate", idempotent = true)
  public Parameters translateInstance(final HttpServletRequest request,
    final HttpServletResponse response, final ServletRequestDetails details, @IdParam
    final TokenParam id, @OperationParam(name = "url")
    final StringParam url, @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "system")
    final StringParam system, @OperationParam(name = "version")
    final StringParam version, @OperationParam(name = "reverse", type = BooleanType.class)
    final BooleanType reverse) throws Exception {

    try {
      FhirUtilityR4.required("code", code);
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.notSupported("version", version);
      codeToTranslate = code.getCode().toLowerCase();
      Parameters params = new Parameters();
      List<ConceptMap> cm = findConceptMaps(id, null, system, url, version);
      for (ConceptMap mapping : cm) {
        List<gov.nih.nci.evs.api.model.ConceptMap> maps =
            queryService.getMapset(mapping.getTitle(), new IncludeParam("maps")).get(0).getMaps();
        List<gov.nih.nci.evs.api.model.ConceptMap> filteredMaps = new ArrayList<>();
        if (reverse.getValue()) {
          filteredMaps = maps.stream()
              .filter(m -> m.getTargetCode().toLowerCase().contains(codeToTranslate)
                  || m.getTargetName().toLowerCase()
                      .matches("^" + Pattern.quote(codeToTranslate) + ".*")
                  || m.getTargetName().toLowerCase()
                      .matches(".*\\b" + Pattern.quote(codeToTranslate) + ".*"))
              .collect(Collectors.toList());
        } else {
          filteredMaps = maps.stream()
              .filter(m -> m.getSourceCode().toLowerCase().contains(codeToTranslate)
                  || m.getSourceName().toLowerCase()
                      .matches("^" + Pattern.quote(codeToTranslate) + ".*")
                  || m.getSourceName().toLowerCase()
                      .matches(".*\\b" + Pattern.quote(codeToTranslate) + ".*"))
              .collect(Collectors.toList());
        }

        if (filteredMaps.size() > 0) {
          gov.nih.nci.evs.api.model.ConceptMap map = filteredMaps.get(0);
          params.addParameter("result", true);
          Parameters.ParametersParameterComponent property =
              new Parameters.ParametersParameterComponent().setName("match");
          property.addPart().setName("equivalence").setValue(new StringType("equivalent"));
          property.addPart().setName("concept").setValue(
              new Coding(map.getTargetTerminology(), map.getTargetCode(), map.getTargetName()));
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
      throw FhirUtilityR4.exception("Failed to translate concept map",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Perform the lookup in the implicit map.
   * 
   * <pre>
   * Parameters for all resources 
   *   used: _id
   *   not used: _content, _filter, _has, _in, _language, _lastUpdated, 
   *             _list, _profile, _query, _security, _source, _tag, _text, _type
   * https://hl7.org/fhir/R4/conceptmap-operation-translate.html
   * The following parameters in the operation are not used
   * &#64;OptionalParam(name="dependency") ?? dependency
   * &#64;OperationParam(name = "conceptMap") ConceptMap conceptMap, 
   * &#64;OperationParam(name = "conceptMapVersion") String conceptMapVersion,
   * &#64;OperationParam(name = "source") String source,
   * &#64;OperationParam(name = "target") String target,
   * &#64;OperationParam(name = "reverse") BooleanType reverse
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param url the url
   * @param code the code
   * @param system the system
   * @param version the version
   * @param coding the coding
   * @param codeableConcept the codeable concept
   * @param targetSystem the target system
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$translate", idempotent = true)
  public Parameters translateImplicit(final HttpServletRequest request,
    final HttpServletResponse response, final ServletRequestDetails details,
    @OperationParam(name = "url")
    final StringParam url, @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "system")
    final StringParam system, @OperationParam(name = "version")
    final StringParam version, @OperationParam(name = "reverse", type = BooleanType.class)
    final BooleanType reverse) throws Exception {

    try {
      FhirUtilityR4.required("code", code);
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.notSupported("version", version);
      codeToTranslate = code.getCode().toLowerCase();
      Parameters params = new Parameters();
      List<ConceptMap> cm = findConceptMaps(null, null, system, url, version);
      for (ConceptMap mapping : cm) {
        List<gov.nih.nci.evs.api.model.ConceptMap> maps =
            queryService.getMapset(mapping.getTitle(), new IncludeParam("maps")).get(0).getMaps();
        List<gov.nih.nci.evs.api.model.ConceptMap> filteredMaps = new ArrayList<>();
        if (reverse.getValue()) {
          filteredMaps = maps.stream()
              .filter(m -> m.getTargetCode().toLowerCase().contains(codeToTranslate)
                  || m.getTargetName().toLowerCase()
                      .matches("^" + Pattern.quote(codeToTranslate) + ".*")
                  || m.getTargetName().toLowerCase()
                      .matches(".*\\b" + Pattern.quote(codeToTranslate) + ".*"))
              .collect(Collectors.toList());
        } else {
          filteredMaps = maps.stream()
              .filter(m -> m.getSourceCode().toLowerCase().contains(codeToTranslate)
                  || m.getSourceName().toLowerCase()
                      .matches("^" + Pattern.quote(codeToTranslate) + ".*")
                  || m.getSourceName().toLowerCase()
                      .matches(".*\\b" + Pattern.quote(codeToTranslate) + ".*"))
              .collect(Collectors.toList());
        }

        if (filteredMaps.size() > 0) {
          gov.nih.nci.evs.api.model.ConceptMap map = filteredMaps.get(0);
          params.addParameter("result", true);
          Parameters.ParametersParameterComponent property =
              new Parameters.ParametersParameterComponent().setName("match");
          property.addPart().setName("equivalence").setValue(new StringType("equivalent"));
          property.addPart().setName("concept").setValue(
              new Coding(map.getTargetTerminology(), map.getTargetCode(), map.getTargetName()));
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
      throw FhirUtilityR4.exception("Failed to translate concept map",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /* see superclass */
  @Override
  public Class<ConceptMap> getResourceType() {
    return ConceptMap.class;
  }

  @Search
  public List<ConceptMap> findConceptMaps(@OptionalParam(name = "_id") TokenParam id,
    @OptionalParam(name = "date")
    final DateRangeParam date, @OptionalParam(name = "system")
    final StringParam system, @OptionalParam(name = "url")
    final StringParam url, @OptionalParam(name = "version")
    final StringParam version) throws Exception {
    try {
      final List<Concept> mapsets = queryService.getMapsets(new IncludeParam("properties"));

      final List<ConceptMap> list = new ArrayList<>();
      for (final Concept mapset : mapsets) {
        List<Property> props = mapset.getProperties();
        if (props.stream()
            .filter(m -> m.getType().equals("downloadOnly") && m.getValue().equals("true"))
            .findAny().isPresent()) {
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
      return list;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR4.exception("Failed to find concept maps",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }
}
