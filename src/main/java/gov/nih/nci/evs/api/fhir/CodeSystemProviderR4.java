package gov.nih.nci.evs.api.fhir;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
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

import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.controller.ConceptController;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticOperationsService;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.ElasticSearchService;
import gov.nih.nci.evs.api.util.TerminologyUtils;

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

  /** the query service */
  @Autowired
  ElasticQueryService queryService;

  /** The search service. */
  @Autowired
  ElasticSearchService searchService;

  /** The concept controller */
  @Autowired
  ConceptController conceptController;

  /* The terminology utils */
  @Autowired
  TerminologyUtils termUtils;

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
    final StringType system, @OperationParam(name = "version")
    final StringType version, @OperationParam(name = "coding")
    final Coding coding, @OperationParam(name = "date")
    final DateRangeParam date, @OperationParam(name = "displayLanguage")
    final StringType displayLanguage, @OperationParam(name = "property")
    final Set<String> properties) throws Exception {

    try {
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.mutuallyExclusive("code", code, "coding", coding);
      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      List<CodeSystem> cs = findPossibleCodeSystems(null, date, system, version);
      Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        CodeSystem codeSys = cs.get(0);
        Terminology term = termUtils.getTerminology(codeSys.getTitle(), true);
        Concept conc =
            queryService.getConcept(codeToLookup, term, new IncludeParam("children")).get();
        params.addParameter("code", "code");
        params.addParameter("system", codeSys.getUrl());
        params.addParameter("code", codeSys.getName());
        params.addParameter("version", codeSys.getVersion());
        params.addParameter("display", conc.getName());
        params.addParameter("active", codeSys.getStatus().toString());
        for (Concept parent : conc.getParents()) {
          params.addParameter(FhirUtilityR4.createProperty("parent", parent.getCode(), true));
        }
        for (Concept child : conc.getChildren()) {
          params.addParameter(FhirUtilityR4.createProperty("child", child.getCode(), true));
        }

      }
      return params;

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
    final StringType system, @OperationParam(name = "version")
    final StringType version, @OperationParam(name = "coding")
    final Coding coding, @OperationParam(name = "date")
    final DateRangeParam date, @OperationParam(name = "displayLanguage")
    final StringType displayLanguage, @OperationParam(name = "property")
    final Set<String> properties) throws Exception {

    try {
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.mutuallyExclusive("code", code, "coding", coding);
      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      List<CodeSystem> cs = findPossibleCodeSystems(id, date, system, version);
      Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        CodeSystem codeSys = cs.get(0);
        Terminology term = termUtils.getTerminology(codeSys.getTitle(), true);
        Concept conc =
            queryService.getConcept(codeToLookup, term, new IncludeParam("children")).get();
        params.addParameter("code", "code");
        params.addParameter("system", codeSys.getUrl());
        params.addParameter("code", codeSys.getName());
        params.addParameter("version", codeSys.getVersion());
        params.addParameter("display", conc.getName());
        params.addParameter("active", codeSys.getStatus().toString());
        for (Concept parent : conc.getParents()) {
          params.addParameter(FhirUtilityR4.createProperty("parent", parent.getCode(), true));
        }
        for (Concept child : conc.getChildren()) {
          params.addParameter(FhirUtilityR4.createProperty("child", child.getCode(), true));
        }

      }
      return params;

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
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeImplicit(final HttpServletRequest request,
    final HttpServletResponse response, final ServletRequestDetails details,
    @OperationParam(name = "url")
    final UriType url, @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "system")
    final StringType system, @OperationParam(name = "display")
    final StringType display, @OperationParam(name = "version")
    final StringType version, @OperationParam(name = "coding")
    final Coding coding) throws Exception {

    try {
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.mutuallyExclusive("code", code, "coding", coding);
      List<CodeSystem> cs = findPossibleCodeSystems(null, null, system, version);
      Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToValidate = "";
        if (code != null) {
          codeToValidate = code.getCode();
        } else if (coding != null) {
          codeToValidate = coding.getCode();
        }
        CodeSystem codeSys = cs.get(0);
        Terminology term = termUtils.getTerminology(codeSys.getTitle(), true);
        Optional<Concept> check =
            queryService.getConcept(codeToValidate, term, new IncludeParam("children"));
        if (check.get() != null) {
          Concept conc =
              queryService.getConcept(codeToValidate, term, new IncludeParam("children")).get();
          params.addParameter("result", "true");
          params.addParameter("code", "code");
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("code", codeSys.getName());
          params.addParameter("version", codeSys.getVersion());
          params.addParameter("display", conc.getName());
          params.addParameter("active", codeSys.getStatus().toString());
        } else {
          params.addParameter("result", "false");
          params.addParameter("message",
              "The code does not exist for the supplied code system and/or version");
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
        }
      }
      return params;

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
    final UriType url, @OperationParam(name = "code")
    final CodeType code, @OperationParam(name = "system")
    final StringType system, @OperationParam(name = "display")
    final StringType display, @OperationParam(name = "version")
    final StringType version, @OperationParam(name = "coding")
    final Coding coding) throws Exception {

    try {
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.mutuallyExclusive("code", code, "coding", coding);
      List<CodeSystem> cs = findPossibleCodeSystems(id, null, system, version);
      Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToValidate = "";
        if (code != null) {
          codeToValidate = code.getCode();
        } else if (coding != null) {
          codeToValidate = coding.getCode();
        }
        CodeSystem codeSys = cs.get(0);
        Terminology term = termUtils.getTerminology(codeSys.getTitle(), true);
        Optional<Concept> check =
            queryService.getConcept(codeToValidate, term, new IncludeParam("children"));
        if (check.get() != null) {
          Concept conc =
              queryService.getConcept(codeToValidate, term, new IncludeParam("children")).get();
          params.addParameter("result", "true");
          params.addParameter("code", "code");
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("code", codeSys.getName());
          params.addParameter("version", codeSys.getVersion());
          params.addParameter("display", conc.getName());
          params.addParameter("active", codeSys.getStatus().toString());
        } else {
          params.addParameter("result", "false");
          params.addParameter("message",
              "The code does not exist for the supplied code system and/or version");
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
        }

      }
      return params;

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
    final StringType system, @OperationParam(name = "version")
    final StringType version, @OperationParam(name = "codingA")
    final Coding codingA, @OperationParam(name = "codingB")
    final Coding codingB) throws Exception {

    try {
      FhirUtilityR4.mutuallyRequired("codeA", codeA, "system", system);
      FhirUtilityR4.mutuallyRequired("codeB", codeB, "system", system);
      FhirUtilityR4.mutuallyExclusive("codingB", codingB, "codeB", codeB);
      FhirUtilityR4.mutuallyExclusive("codingA", codingA, "codeA", codeA);
      List<CodeSystem> cs = findPossibleCodeSystems(null, null, system, version);
      Parameters params = new Parameters();
      if (cs.size() > 0) {
        String code1 = "";
        String code2 = "";
        if (codeA != null && codeB != null) {
          code1 = codeA.getCode();
          code2 = codeB.getCode();
        } else if (codingA != null && codingB != null) {
          code1 = codingA.getCode();
          code2 = codingB.getCode();
        }
        CodeSystem codeSys = cs.get(0);
        Terminology term = termUtils.getTerminology(codeSys.getTitle(), true);
        Optional<Concept> checkA =
            queryService.getConcept(code1, term, new IncludeParam("minimal"));
        Optional<Concept> checkB =
            queryService.getConcept(code2, term, new IncludeParam("minimal"));
        if (checkA.get() != null && checkB.get() != null) {
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          if (queryService.getPathsToParent(code1, code2, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumes");
          } else if (queryService.getPathsToParent(code2, code1, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumed-by");
          } else {
            params.addParameter("outcome", "no-subsumption-relationship");
          }
        }
      }
      return params;

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
    final StringType system, @OperationParam(name = "version")
    final StringType version, @OperationParam(name = "codingA")
    final Coding codingA, @OperationParam(name = "codingB")
    final Coding codingB) throws Exception {

    try {
      FhirUtilityR4.mutuallyRequired("codeA", codeA, "system", system);
      FhirUtilityR4.mutuallyRequired("codeB", codeB, "system", system);
      FhirUtilityR4.mutuallyExclusive("codingB", codingB, "codeB", codeB);
      FhirUtilityR4.mutuallyExclusive("codeA", codingA, "codeA", codeA);
      List<CodeSystem> cs = findPossibleCodeSystems(id, null, system, version);
      Parameters params = new Parameters();
      if (cs.size() > 0) {
        String code1 = "";
        String code2 = "";
        if (codeA != null && codeB != null) {
          code1 = codeA.getCode();
          code2 = codeB.getCode();
        } else if (codingA != null && codingB != null) {
          code1 = codingA.getCode();
          code2 = codingB.getCode();
        }
        CodeSystem codeSys = cs.get(0);
        Terminology term = termUtils.getTerminology(codeSys.getTitle(), true);
        Optional<Concept> checkA =
            queryService.getConcept(code1, term, new IncludeParam("minimal"));
        Optional<Concept> checkB =
            queryService.getConcept(code2, term, new IncludeParam("minimal"));
        if (checkA.get() != null && checkB.get() != null) {
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          if (queryService.getPathsToParent(code1, code2, term).getCt() > 0) {
            params.addParameter("outcome", "subsumes");
          } else if (queryService.getPathsToParent(code2, code1, term).getCt() > 0) {
            params.addParameter("outcome", "subsumed-by");
          } else {
            params.addParameter("outcome", "no-subsumption-relationship");
          }
        }
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception("Failed to validate code", OperationOutcome.IssueType.EXCEPTION,
          500);
    }

  }

  /**
   * Find code systems.
   * 
   * <pre>
   * Parameters for all resources 
   *   used: _id
   *   not used: _content, _filter, _has, _in, _language, _lastUpdated, 
   *             _list, _profile, _query, _security, _source, _tag, _text, _type
   * https://hl7.org/fhir/R4/codesystem.html (see Search Parameters)
   * The following parameters in the registry are not used
   * &#64;OptionalParam(name="code") String code,
   * &#64;OptionalParam(name="context") TokenParam context,
   * &#64;OptionalParam(name="context-quantity") QuantityParam contextQuantity,
   * &#64;OptionalParam(name="context-type") String contextType,
   * &#64;OptionalParam(name="context-type-quantity") QuantityParam contextTypeQuantity,
   * &#64;OptionalParam(name="context-type-value") String contextTypeValue,
   * &#64;OptionalParam(name="identifier") StringParam identifier,
   * &#64;OptionalParam(name="jurisdiction") StringParam jurisdiction,
   * &#64;OptionalParam(name="status") String status,
   * </pre>
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param date the date
   * @param description the description
   * @param name the name
   * @param publisher the publisher
   * @param title the title
   * @param url the url
   * @param version the version
   * @return the list
   * @throws Exception the exception
   */
  @Search
  public List<CodeSystem> findCodeSystems(@OptionalParam(name = "_id") TokenParam id,
    @OptionalParam(name = "date")
    final DateRangeParam date, @OptionalParam(name = "system")
    final StringParam system, @OptionalParam(name = "version")
    final StringParam version, @OptionalParam(name = "title")
    final StringParam title) throws Exception {
    try {
      final List<Terminology> terms = termUtils.getTerminologies(true);

      final List<CodeSystem> list = new ArrayList<>();
      for (final Terminology terminology : terms) {
        final CodeSystem cs = FhirUtilityR4.toR4(terminology);
        // Skip non-matching
        if ((id != null && !id.getValue().equals(cs.getId()))
            || (system != null && !system.getValue().equals(cs.getUrl()))) {
          logger.info("  SKIP url mismatch = " + cs.getUrl());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cs.getDate())) {
          logger.info("  SKIP date mismatch = " + cs.getDate());
          continue;
        }
        if (title != null && !FhirUtility.compareString(title, cs.getTitle())) {
          logger.info("  SKIP title mismatch = " + cs.getTitle());
          continue;
        }
        if (version != null && !FhirUtility.compareString(version, cs.getVersion())) {
          logger.info("  SKIP version mismatch = " + cs.getVersion());
          continue;
        }

        list.add(cs);
      }
      return list;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR4.exception("Failed to find code systems",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  public List<CodeSystem> findPossibleCodeSystems(@OptionalParam(name = "_id") IdType id,
    @OptionalParam(name = "date")
    final DateRangeParam date, @OptionalParam(name = "system")
    final StringType system, @OptionalParam(name = "version")
    final StringType version) throws Exception {
    try {
      final List<Terminology> terms = termUtils.getTerminologies(true);

      final List<CodeSystem> list = new ArrayList<>();
      for (final Terminology terminology : terms) {
        final CodeSystem cs = FhirUtilityR4.toR4(terminology);
        // Skip non-matching
        if ((id != null && !id.getIdPart().equals(cs.getId()))
            || (system != null && !system.getValue().equals(cs.getUrl()))) {
          logger.info("  SKIP url mismatch = " + cs.getUrl());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cs.getDate())) {
          logger.info("  SKIP date mismatch = " + cs.getDate());
          continue;
        }
        if (version != null && !version.getValue().equals(cs.getVersion())) {
          logger.info("  SKIP version mismatch = " + cs.getVersion());
          continue;
        }

        list.add(cs);
      }
      return list;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR4.exception("Failed to find code systems",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  @Read
  public CodeSystem getConceptMap(final ServletRequestDetails details, @IdParam
  final IdType id) throws Exception {
    try {

      final List<CodeSystem> candidates = findPossibleCodeSystems(id, null, null, null);
      for (final CodeSystem set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }

      throw FhirUtilityR4.exception(
          "Code system not found = " + (id == null ? "null" : id.getIdPart()), IssueType.NOTFOUND,
          404);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR4.exception("Failed to get code system",
          OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /* see superclass */
  @Override
  public Class<CodeSystem> getResourceType() {
    return CodeSystem.class;
  }
}
