package gov.nih.nci.evs.api.fhir;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

/**
 * The ConceptMap provider.
 */
@Component
public class ConceptMapProviderR4 implements IResourceProvider {

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
    final IdType id, @OperationParam(name = "url")
    final String url, @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "system")
    final String system, @OperationParam(name = "version")
    final String version, @OperationParam(name = "coding")
    final Coding coding, @OperationParam(name = "codeableConcept")
    final CodeableConcept codeableConcept, @OperationParam(name = "targetsystem")
    final String targetSystem) throws Exception {

    try {
      return null;

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
    final String url, @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "system")
    final String system, @OperationParam(name = "version")
    final String version, @OperationParam(name = "coding")
    final Coding coding, @OperationParam(name = "codeableConcept")
    final CodeableConcept codeableConcept, @OperationParam(name = "targetsystem")
    final String targetSystem) throws Exception {

    try {
      return null;

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
}
