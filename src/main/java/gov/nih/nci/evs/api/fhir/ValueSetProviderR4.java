package gov.nih.nci.evs.api.fhir;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.service.ElasticSearchService;

/**
 * The ValueSet provider.
 */
@Component
public class ValueSetProviderR4 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(ValueSetProviderR4.class);

  /** The search service. */
  @Autowired
  ElasticSearchService searchService;

  /**
   * Expand implicit.
   *
   * @param request the request
   * @param details the details
   * @param rawBody the raw body
   * @param url the url
   * @param version the version
   * @param filter the filter
   * @param offset the offset
   * @param count the count
   * @param includeDesignationsType the include designations type
   * @param designations the designations
   * @param activeOnly the active only
   * @return the value set
   * @throws Exception the exception
   */
  @Operation(name = "$expand", idempotent = true)
  public ValueSet expandImplicit(final HttpServletRequest request,
    final ServletRequestDetails details, @ResourceParam String rawBody,
    @OperationParam(name = "url") String url,
    @OperationParam(name = "valueSetVersion") StringParam version,
    // @OperationParam(name = "context") String context,
    // @OperationParam(name = "contextDirection") String contextDirection,
    @OperationParam(name = "filter") String filter,
    // @OperationParam(name = "date") String date,
    @OperationParam(name = "offset") IntegerType offset,
    @OperationParam(name = "count") IntegerType count,
    @OperationParam(name = "includeDesignations") BooleanType includeDesignationsType,
    @OperationParam(name = "designation") Set<String> designations,
    // @OperationParam(name = "includeDefinition") BooleanType includeDefinition,
    @OperationParam(name = "activeOnly") BooleanType activeOnly
  // @OperationParam(name = "excludeNested") BooleanType excludeNested,
  // @OperationParam(name = "excludeNotForUI") BooleanType excludeNotForUI,
  // @OperationParam(name = "excludePostCoordinated") BooleanType excludePostCoordinated,
  // @OperationParam(name = "displayLanguage") String displayLanguage,
  // @OperationParam(name = "exclude-system") StringType excludeSystem,
  // @OperationParam(name = "system-version") StringType systemVersion,
  // @OperationParam(name = "check-system-version") StringType checkSystemVersion,
  // @OperationParam(name = "force-system-version") StringType forceSystemVersion
  ) throws Exception {

    try {
      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      e.printStackTrace();
      throw FhirUtilityR4.exception("Failed to load value set",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Expand instance.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-expand.html
   * </pre>
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param rawBody the raw body
   * @param version the version
   * @param filter the filter
   * @param offset the offset
   * @param count the count
   * @param includeDesignationsType the include designations type
   * @param designations the designations
   * @param activeOnly the active only
   * @return the value set
   * @throws Exception the exception
   */
  @Operation(name = "$expand", idempotent = true)
  public ValueSet expandInstance(final HttpServletRequest request,
    final ServletRequestDetails details, @IdParam IdType id, @ResourceParam String rawBody,
    @OperationParam(name = "valueSetVersion") StringParam version,
    // @OperationParam(name = "context") String context,
    // @OperationParam(name = "contextDirection") String contextDirection,
    @OperationParam(name = "filter") String filter,
    // @OperationParam(name = "date") String date,
    @OperationParam(name = "offset") IntegerType offset,
    @OperationParam(name = "count") IntegerType count,
    @OperationParam(name = "includeDesignations") BooleanType includeDesignationsType,
    @OperationParam(name = "designation") Set<String> designations,
    // @OperationParam(name = "includeDefinition") BooleanType includeDefinition,
    @OperationParam(name = "activeOnly") BooleanType activeOnly
  // @OperationParam(name = "excludeNested") BooleanType excludeNested,
  // @OperationParam(name = "excludeNotForUI") BooleanType excludeNotForUI,
  // @OperationParam(name = "excludePostCoordinated") BooleanType excludePostCoordinated,
  // @OperationParam(name = "displayLanguage") String displayLanguage,
  // @OperationParam(name = "exclude-system") StringType excludeSystem,
  // @OperationParam(name = "system-version") StringType systemVersion,
  // @OperationParam(name = "check-system-version") StringType checkSystemVersion,
  // @OperationParam(name = "force-system-version") StringType forceSystemVersion
  ) throws Exception {

    try {
      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to load value set",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code implicit.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-validate-code.html
   * </pre>
   *
   * @param request the request
   * @param details the details
   * @param url the url
   * @param version the version
   * @param codeType the code type
   * @param system the system
   * @param systemVersion the system version
   * @param display the display
   * @param coding the coding
   * @param date the date
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$validate-code", idempotent = true)
  public Parameters validateCodeImplicit(final HttpServletRequest request,
    final ServletRequestDetails details, @OperationParam(name = "url") String url,
    @OperationParam(name = "valueSetVersion") StringParam version,
    @OperationParam(name = "code") CodeType codeType,
    @OperationParam(name = "system") String system,
    @OperationParam(name = "systemVersion") StringParam systemVersion,
    @OperationParam(name = "display") String display,
    @OperationParam(name = "coding") Coding coding,
    @OperationParam(name = "date") DateTimeType date)
    // @OperationParam(name = "displayLanguage") String displayLanguage)
    throws Exception {

    try {
      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to load value set",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }

  }

  /**
   * Validate code instance.
   * 
   * <pre>
   * https://hl7.org/fhir/R4/valueset-operation-validate-code.html
   * </pre>
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param version the version
   * @param codeType the code type
   * @param system the system
   * @param systemVersion the system version
   * @param display the display
   * @param coding the coding
   * @param date the date
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$validate-code", idempotent = true)
  public Parameters validateCodeInstance(final HttpServletRequest request,
    final ServletRequestDetails details, @IdParam IdType id,
    @OperationParam(name = "valueSetVersion") StringParam version,
    @OperationParam(name = "code") CodeType codeType,
    @OperationParam(name = "system") String system,
    @OperationParam(name = "systemVersion") StringParam systemVersion,
    @OperationParam(name = "display") String display,
    @OperationParam(name = "coding") Coding coding,
    @OperationParam(name = "date") DateTimeType date)
    // @OperationParam(name = "displayLanguage") String displayLanguage)
    throws Exception {

    try {
      return null;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to load value set",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /* see superclass */
  @Override
  public Class<ValueSet> getResourceType() {
    return ValueSet.class;
  }
}
