package gov.nih.nci.evs.api.fhir;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.service.ElasticOperationsService;
import gov.nih.nci.evs.api.service.ElasticSearchService;

/**
 * The CodeSystem provider.
 */
@Component
public class CodeSystemProviderR4 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(CodeSystemProviderR4.class);

  /** The operations service. */
  @Autowired
  ElasticOperationsService operationsService;

  /** The search service. */
  @Autowired
  ElasticSearchService searchService;

  /**
   * Lookup implicit.
   * 
   * <pre>
   * https://build.fhir.org/codesystem-operation-lookup.html
   * All properties implemented.
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param code the code
   * @param system the system
   * @param version the version
   * @param coding the coding
   * @param date the date
   * @param displayLanguage the display language
   * @param properties the properties
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$lookup", idempotent = true)
  public Parameters lookupImplicit(final HttpServletRequest request,
    final HttpServletResponse response, final ServletRequestDetails details,
    @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "system")
    final String system, @OperationParam(name = "version")
    final String version, @OperationParam(name = "coding")
    final Coding coding, @OperationParam(name = "date")
    final DateTimeType date, @OperationParam(name = "displayLanguage")
    final String displayLanguage, @OperationParam(name = "property")
    final Set<String> properties) throws Exception {

    try {

      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      e.printStackTrace();
      throw FhirUtilityR4.exception("Failed to lookup code", OperationOutcome.IssueType.EXCEPTION,
          500);
    }
  }

  /**
   * Lookup instance. https://build.fhir.org/codesystem-operation-lookup.html
   * 
   * <pre>
   * https://build.fhir.org/codesystem-operation-lookup.html
   * All properties implemented.
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param code the code
   * @param system the system
   * @param version the version
   * @param coding the coding
   * @param date the date
   * @param displayLanguage the display language
   * @param properties the properties
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$lookup", idempotent = true)
  public Parameters lookupInstance(final HttpServletRequest request,
    final HttpServletResponse response, final ServletRequestDetails details, @IdParam
    final IdType id, @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "system")
    final String system, @OperationParam(name = "version")
    final String version, @OperationParam(name = "coding")
    final Coding coding, @OperationParam(name = "date")
    final DateTimeType date, @OperationParam(name = "displayLanguage")
    final String displayLanguage, @OperationParam(name = "property")
    final Set<String> properties) throws Exception {

    try {
      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to lookup code", OperationOutcome.IssueType.EXCEPTION,
          500);
    }
  }

  /**
   * Validate code implicit.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/codesystem-operation-validate-code.html
   * The following parameters are not used:
   * &#64;OperationParam(name = "codeSystem") CodeSystem codeSystem
   * &#64;OperationParam(name = "date") DateTimeType date, 
   * &#64;OperationParam(name = "codeableConcept") CodeableConcept codeableConcept
   * &#64;OperationParam(name = "abstract") BooleanType abstract
   * &#64;OperationParam(name = "displayLanguage") String displayLanguage
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param url the url
   * @param code the code
   * @param display the display
   * @param version the version
   * @param coding the coding
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$validate-code", idempotent = true)
  public Parameters validateCodeImplicit(final HttpServletRequest request,
    final HttpServletResponse response, final ServletRequestDetails details,
    @OperationParam(name = "url")
    final String url, @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "display")
    final String display, @OperationParam(name = "version")
    final String version, @OperationParam(name = "coding")
    final Coding coding) throws Exception {

    try {
      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to validate code", OperationOutcome.IssueType.EXCEPTION,
          500);
    }
  }

  /**
   * Validate code implicit.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/codesystem-operation-validate-code.html
   * The following parameters are not used:
   * &#64;OperationParam(name = "codeSystem") CodeSystem codeSystem
   * &#64;OperationParam(name = "date") DateTimeType date, 
   * &#64;OperationParam(name = "codeableConcept") CodeableConcept codeableConcept
   * &#64;OperationParam(name = "abstract") BooleanType abstract
   * &#64;OperationParam(name = "displayLanguage") String displayLanguage
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param url the url
   * @param code the code
   * @param display the display
   * @param version the version
   * @param coding the coding
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$validate-code", idempotent = true)
  public Parameters validateCodeInstance(final HttpServletRequest request,
    final HttpServletResponse response, final ServletRequestDetails details, @IdParam IdType id,
    @OperationParam(name = "url")
    final String url, @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "display")
    final String display, @OperationParam(name = "version")
    final String version, @OperationParam(name = "coding")
    final Coding coding) throws Exception {

    try {
      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to validate code", OperationOutcome.IssueType.EXCEPTION,
          500);
    }

  }

  /**
   * Subsumes implicit.
   *
   * <pre>
   * https://hl7.org/fhir/R4/codesystem-operation-subsumes.html All parameters supported. </pre
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param codeA the code A
   * @param codeB the code B
   * @param system the system
   * @param version the version
   * @param codingA the coding A
   * @param codingB the coding B
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$subsumes", idempotent = true)
  public Parameters subsumesImplicit(final HttpServletRequest request,
    final HttpServletResponse response, final ServletRequestDetails details,
    @OperationParam(name = "codeA")
    final CodeType codeA, @OperationParam(name = "codeB")
    final CodeType codeB, @OperationParam(name = "system")
    final String system, @OperationParam(name = "version")
    final String version, @OperationParam(name = "codingA")
    final Coding codingA, @OperationParam(name = "codingB")
    final Coding codingB) throws Exception {

    try {
      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to check if A subsumes B",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Subsumes instance.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/codesystem-operation-subsumes.html All parameters supported. </pre
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param codeA the code A
   * @param codeB the code B
   * @param system the system
   * @param version the version
   * @param codingA the coding A
   * @param codingB the coding B
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$subsumes", idempotent = true)
  public Parameters subsumesInstance(final HttpServletRequest request,
    final HttpServletResponse response, final ServletRequestDetails details, @IdParam IdType id,
    @OperationParam(name = "codeA")
    final CodeType codeA, @OperationParam(name = "codeB")
    final CodeType codeB, @OperationParam(name = "system")
    final String system, @OperationParam(name = "version")
    final String version, @OperationParam(name = "codingA")
    final Coding codingA, @OperationParam(name = "codingB")
    final Coding codingB) throws Exception {

    try {
      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to validate code", OperationOutcome.IssueType.EXCEPTION,
          500);
    }

  }

  /* see superclass */
  @Override
  public Class<CodeSystem> getResourceType() {
    return CodeSystem.class;
  }
}
