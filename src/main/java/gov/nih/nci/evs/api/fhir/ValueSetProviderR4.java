package gov.nih.nci.evs.api.fhir;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
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
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.service.MetadataService;
import gov.nih.nci.evs.api.util.TerminologyUtils;

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

  /** The search service. */
  @Autowired
  ElasticQueryService queryService;

  /** The metadata service. */
  @Autowired
  MetadataService metadataService;

  /* The terminology utils */
  @Autowired
  TerminologyUtils termUtils;

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
    @OperationParam(name = "filter") String filter,
    @OperationParam(name = "offset") IntegerType offset,
    @OperationParam(name = "count") IntegerType count,
    @OperationParam(name = "includeDesignations") BooleanType includeDesignationsType,
    @OperationParam(name = "designation") Set<String> designations,
    @OperationParam(name = "activeOnly") BooleanType activeOnly) throws Exception {

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
    @OperationParam(name = "filter") String filter,
    @OperationParam(name = "offset") IntegerType offset,
    @OperationParam(name = "count") IntegerType count,
    @OperationParam(name = "includeDesignations") BooleanType includeDesignationsType,
    @OperationParam(name = "designation") Set<String> designations,
    @OperationParam(name = "activeOnly") BooleanType activeOnly

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
    final ServletRequestDetails details, @OperationParam(name = "code") CodeType codeType,
    @OperationParam(name = "system") StringParam system,
    @OperationParam(name = "systemVersion") StringParam systemVersion,
    @OperationParam(name = "url") StringParam url) throws Exception {

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
    @OperationParam(name = "code") CodeType codeType,
    @OperationParam(name = "system") StringParam system,
    @OperationParam(name = "systemVersion") StringParam systemVersion,
    @OperationParam(name = "url") StringParam url) throws Exception {

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
   * Find value sets.
   * 
   * <pre>
   * Parameters for all resources 
   *   used: _id
   *   not used: _content, _filter, _has, _in, _language, _lastUpdated, 
   *             _list, _profile, _query, _security, _source, _tag, _text, _type
   * https://hl7.org/fhir/R4/valueset.html (see Search Parameters)
   * The following parameters in the registry are not used
   * &#64;OptionalParam(name="context-quantity") QuantityParam contextQuantity,
   * &#64;OptionalParam(name="context-type") TokenParam contextType,
   * &#64;OptionalParam(name="context-type-quantity") QuantityParam contextTypeQuantity,
   * &#64;OptionalParam(name="context-type-value") CompositeParam contextTypeValue,
   * &#64;OptionalParam(name="date") DateParam date,
   * &#64;OptionalParam(name="expansion") String expansion,
   * &#64;OptionalParam(name="identifier") TokenParam identifier,
   * &#64;OptionalParam(name="jurisdiction") TokenParam jurisdiction,
   * &#64;OptionalParam(name="reference") String reference,
   * </pre>
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param date the date
   * @param code the code
   * @param description the description
   * @param identifier the identifier
   * @param name the name
   * @param publisher the publisher
   * @param title the title
   * @param url the url
   * @param version the version
   * @return the list
   * @throws Exception the exception
   * @OptionalParam(name="status") String status,
   *                               </pre>
   */
  @Search
  public List<ValueSet> findValueSets(final HttpServletRequest request,
    final ServletRequestDetails details, @OptionalParam(name = "_id") StringParam id,
    @OptionalParam(name = "code") StringParam code, @OptionalParam(name = "name") StringParam name,
    @OptionalParam(name = "system") StringParam system,
    @OptionalParam(name = "url") StringParam url,
    @OptionalParam(name = "version") StringParam version) throws Exception {

    final List<Terminology> terms = termUtils.getTerminologies(true);

    final List<ValueSet> list = new ArrayList<ValueSet>();
    for (final Terminology terminology : terms) {
      final ValueSet vs = FhirUtilityR4.toR4VS(terminology);
      // Skip non-matching
      if (id != null && !id.getValue().equals(vs.getId())) {
        logger.info("  SKIP id mismatch = " + vs.getUrl());
        continue;
      }
      if (system != null && !system.getValue().equals(vs.getTitle())) {
        logger.info("  SKIP system mismatch = " + vs.getTitle());
        continue;
      }
      if (name != null && !name.getValue().equals(vs.getName())) {
        logger.info("  SKIP name mismatch = " + vs.getName());
        continue;
      }
      if (version != null && !FhirUtility.compareString(version, vs.getVersion())) {
        logger.info("  SKIP version mismatch = " + vs.getVersion());
        continue;
      }

      list.add(vs);
    }
    logger.info("Hello");
    final List<Concept> subsets =
        metadataService.getSubsets("ncit", Optional.of("minimal"), Optional.ofNullable(null));
    final Set<String> codes = subsets.stream().flatMap(Concept::streamSelfAndChildren)
        .map(c -> c.getCode()).collect(Collectors.toSet());
    List<Concept> subsetsAsConcepts = queryService.getConcepts(codes,
        termUtils.getTerminology("ncit", true), new IncludeParam("minimal"));
    for (final Concept subset : subsetsAsConcepts) {
      final ValueSet vs = FhirUtilityR4.toR4VS(subset);
      // Skip non-matching
      if (id != null && !id.getValue().equals(vs.getId())) {
        logger.info("  SKIP id mismatch = " + vs.getUrl());
        continue;
      }
      if (system != null && !system.getValue().equals(vs.getTitle())) {
        logger.info("  SKIP system mismatch = " + vs.getTitle());
        continue;
      }
      if (name != null && !name.getValue().equals(vs.getName())) {
        logger.info("  SKIP name mismatch = " + vs.getName());
        continue;
      }
      if (code != null && !code.getValue().equals(vs.getIdentifier())) {
        logger.info("  SKIP code mismatch = " + vs.getIdentifier());
        continue;
      }
      list.add(vs);
    }

    return list;

  }

  /* see superclass */
  @Override
  public Class<ValueSet> getResourceType() {
    return ValueSet.class;
  }
}
