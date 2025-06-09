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
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.nih.nci.evs.api.controller.ConceptController;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpenSearchService;
import gov.nih.nci.evs.api.service.OpensearchOperationsService;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import gov.nih.nci.evs.api.util.FhirUtility;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
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

/** FHIR R4 CodeSystem provider. */
@Component
public class CodeSystemProviderR4 implements IResourceProvider {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(CodeSystemProviderR4.class);

  /** The operations service. */
  @Autowired OpensearchOperationsService operationsService;

  /** the query service. */
  @Autowired OpensearchQueryService osQueryService;

  /** The search service. */
  @Autowired OpenSearchService searchService;

  /** The concept controller. */
  @Autowired ConceptController conceptController;

  /** The term utils. */
  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;

  /**
   * Lookup implicit.
   *
   * <pre>
   * https://hl7.org/fhir/R4/codesystem-operation-lookup.html
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param code the code that is to be located. If provided, a system must be provided.
   * @param system the system for the code that is be located.
   * @param version the version of the system
   * @param coding the coding to look up
   * @param date the date the information should be returned for.
   * @param displayLanguage the display language
   * @param property the property that we want to return. If not present, the system chooses what to
   *     return.
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_LOOKUP, idempotent = true)
  public Parameters lookupImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "date") final DateRangeParam date
      //      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      //      @OperationParam(name = "property") final CodeType property
      ) throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_LOOKUP,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.mutuallyExclusive("code", code, "coding", coding);
      //      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      //      FhirUtilityR4.notSupported("property", property);
      for (final String param : new String[] {"displayLanguage", "property"}) {
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

      final List<CodeSystem> cs = findPossibleCodeSystems(null, date, systemToLookup, version);
      final Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), osQueryService, true);
        final Concept conc =
            osQueryService.getConcept(codeToLookup, term, new IncludeParam("children")).get();
        // required in the specification
        params.addParameter("name", codeSys.getName());
        params.addParameter("display", conc.getName());
        // optional in the specification
        params.addParameter("version", codeSys.getVersion());
        // properties
        params.addParameter(FhirUtilityR4.createProperty("active", conc.getActive(), false));
        for (final Concept parent : conc.getParents()) {
          params.addParameter(FhirUtilityR4.createProperty("parent", parent.getCode(), true));
        }
        for (final Concept child : conc.getChildren()) {
          params.addParameter(FhirUtilityR4.createProperty("child", child.getCode(), true));
        }
      } else {
        throw FhirUtilityR4.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      e.printStackTrace();
      throw FhirUtilityR4.exception(
          "Failed to lookup code", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Lookup instance.
   *
   * <pre>
   * https://hl7.org/fhir/R4/codesystem-operation-lookup.html
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param code the code that is to be located. If provided, a system must be provided.
   * @param system the system for the code that is be located.
   * @param version the version of the system
   * @param coding the coding to look up
   * @param date the date the information should be returned for.
   * @param displayLanguage the display language
   * @param property the property that we want to return. If not present, the system chooses what to
   *     return.
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_LOOKUP, idempotent = true)
  public Parameters lookupInstance(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "coding") final Coding coding,
      @OperationParam(name = "date") final DateRangeParam date
      //      @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      //      @OperationParam(name = "property") final CodeType property
      ) throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_LOOKUP,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.mutuallyRequired("code", code, "system", system);
      FhirUtilityR4.mutuallyExclusive("code", code, "coding", coding);
      //      FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      //      FhirUtilityR4.notSupported("property", property);
      for (final String param : new String[] {"displayLanguage", "property"}) {
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

      final List<CodeSystem> cs = findPossibleCodeSystems(id, date, systemToLookup, version);
      final Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), osQueryService, true);
        final Concept conc =
            osQueryService.getConcept(codeToLookup, term, new IncludeParam("children")).get();
        // required in the specification
        params.addParameter("name", codeSys.getName());
        params.addParameter("display", conc.getName());
        // optional in the specification
        params.addParameter("version", codeSys.getVersion());
        params.addParameter(FhirUtilityR4.createProperty("active", conc.getActive(), false));
        for (final Concept parent : conc.getParents()) {
          params.addParameter(FhirUtilityR4.createProperty("parent", parent.getCode(), true));
        }
        for (final Concept child : conc.getChildren()) {
          params.addParameter(FhirUtilityR4.createProperty("child", child.getCode(), true));
        }
      } else {
        throw FhirUtilityR4.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      e.printStackTrace();
      throw FhirUtilityR4.exception(
          "Failed to lookup code", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code implicit.
   *
   * <pre>
   * https://hl7.org/fhir/R4/codesystem-operation-validate-code.html
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param url the CodeSystem URL.
   * @param codeSystem the code system provided directly as a part of the request.
   * @param code the code that is to be validated.
   * @param version the version of the code system.
   * @param display the display associated with the code If provided, a code must be provided.
   * @param coding the coding to validate.
   * @param date the date for when the validation should be checked
   * @param abstractt the abstractt indicates if the concept is a logical grouping concept. If True,
   *     the validation is being performed in a context where a concept designated as 'abstract' is
   *     appropriate/allowed to be used
   * @param displayLanguage the display language to be used for the description when validating the
   *     display property
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      //      @OperationParam(name = "codeSystem") final CodeSystem codeSystem,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding
      //      @OperationParam(name = "date") final DateTimeType date,
      //      @OperationParam(name = "abstract") final BooleanType abstractt,
      //      @OperationParam(name = "displayLanguage") final StringType displayLanguage
      ) throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {

      FhirUtilityR4.mutuallyExclusive("code", code, "coding", coding);
      FhirUtilityR4.mutuallyRequired("code", code, "system", url);
      for (final String param :
          new String[] {"codeSystem", "date", "abstract", "displayLanguage"}) {
        FhirUtilityR4.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR4.notSupported(request, "_has");
      }

      UriType systemToLookup = null;
      if (url != null) {
        systemToLookup = url;
      } else if (coding != null) {
        systemToLookup = coding.getSystemElement();
      }

      final List<CodeSystem> cs = findPossibleCodeSystems(null, null, systemToLookup, version);
      final Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToValidate = "";
        if (code != null) {
          codeToValidate = code.getCode();
        } else if (coding != null) {
          codeToValidate = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), osQueryService, true);
        final Optional<Concept> check =
            osQueryService.getConcept(codeToValidate, term, new IncludeParam("children"));
        if (check.isPresent()) {
          final Concept conc =
              osQueryService.getConcept(codeToValidate, term, new IncludeParam("children")).get();
          if (display == null || conc.getName().equals(display.getValue())) {
            params.addParameter("result", true);
            params.addParameter("code", conc.getCode());
          } else {
            params.addParameter("result", false);
            params.addParameter(
                "message",
                "The code "
                    + conc.getCode()
                    + " exists in this value set but the display is not valid");
          }
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          params.addParameter("display", conc.getName());
          params.addParameter("active", true);
        } else {
          params.addParameter("result", false);
          params.addParameter(
              "message", "The code does not exist for the supplied code system url and/or version");
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
        }
      } else {
        throw FhirUtilityR4.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception(
          "Failed to validate code", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code implicit.
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param url the CodeSystem URL.
   * @param codeSystem the code system provided directly as a part of the request.
   * @param code the code that is to be validated.
   * @param version the version of the code system.
   * @param display the display associated with the code If provided, a code must be provided.
   * @param coding the coding to validate.
   * @param date the date for when the validation should be checked
   * @param abstractt the abstractt indicates if the concept is a logical grouping concept. If True,
   *     the validation is being performed in a context where a concept designated as 'abstract' is
   *     appropriate/allowed to be used
   * @param displayLanguage the display language to be used for the description when validating the
   *     display property
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeInstance(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "url") final UriType url,
      //      @OperationParam(name = "codeSystem") final CodeSystem codeSystem,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding
      //      @OperationParam(name = "date") final DateTimeType date,
      //      @OperationParam(name = "abstract") final BooleanType abstractt,
      //      @OperationParam(name = "displayLanguage") final StringType displayLanguage
      ) throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }

    FhirUtilityR4.mutuallyRequired("code", code, "system", url);
    FhirUtilityR4.mutuallyExclusive("code", code, "coding", coding);
    try {
      for (final String param :
          new String[] {"codeSystem", "date", "abstract", "displayLanguage"}) {
        FhirUtilityR4.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR4.notSupported(request, "_has");
      }

      UriType systemToLookup = null;
      if (url != null) {
        systemToLookup = url;
      } else if (coding != null) {
        systemToLookup = coding.getSystemElement();
      }

      final List<CodeSystem> cs = findPossibleCodeSystems(id, null, systemToLookup, version);
      final Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToValidate = "";
        if (code != null) {
          codeToValidate = code.getCode();
        } else if (coding != null) {
          codeToValidate = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), osQueryService, true);
        final Optional<Concept> check =
            osQueryService.getConcept(codeToValidate, term, new IncludeParam("children"));
        if (check.isPresent()) {
          final Concept conc =
              osQueryService.getConcept(codeToValidate, term, new IncludeParam("children")).get();
          if (display == null || conc.getName().equals(display.getValue())) {
            params.addParameter("code", conc.getCode());
            params.addParameter("result", true);
          } else {
            params.addParameter("result", false);
            params.addParameter(
                "message",
                "The code "
                    + conc.getCode()
                    + " exists in this value set but the display is not valid");
          }
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          params.addParameter("display", conc.getName());
          params.addParameter("active", true);
        } else {
          params.addParameter("result", false);
          params.addParameter(
              "message", "The code does not exist for the supplied code system url and/or version");
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
        }
      } else {
        throw FhirUtilityR4.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception(
          "Failed to validate code", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Subsumes implicit.
   *
   * <pre>
   * https://hl7.org/fhir/R4/codesystem-operation-subsumes.html
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param codeA the code A to be tested. If provided, a system must be provided
   * @param codeB the code B to be tested. If provided, a system must be provided
   * @param system the code system in with the subsumption testing is to be performed. Must be
   *     provided unless the operation is invoked on a code system instance.
   * @param version the version of the code system.
   * @param codingA the coding A to be tested
   * @param codingB the coding B to be tested
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = "$subsumes", idempotent = true)
  public Parameters subsumesImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "codeA") final CodeType codeA,
      @OperationParam(name = "codeB") final CodeType codeB,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "codingA") final Coding codingA,
      @OperationParam(name = "codingB") final Coding codingB)
      throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.mutuallyRequired("codeA", codeA, "system", system);
      FhirUtilityR4.mutuallyRequired("codeB", codeB, "system", system);
      FhirUtilityR4.mutuallyExclusive("codingB", codingB, "codeB", codeB);
      FhirUtilityR4.mutuallyExclusive("codingA", codingA, "codeA", codeA);

      UriType systemToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (codingA != null) {
        systemToLookup = codingA.getSystemElement();
      }

      final List<CodeSystem> cs = findPossibleCodeSystems(null, null, systemToLookup, version);
      final Parameters params = new Parameters();
      if (cs.size() > 0) {
        String code1 = "";
        String code2 = "";
        if (codeA != null && codeB != null) {
          code1 = codeA.getCode();
          code2 = codeB.getCode();
        } else if (codingA != null && codingB != null) {
          code1 = codingA.getCode();
          code2 = codingB.getCode();
        } else if (codeA == null) {
          throw FhirUtilityR4.exception(
              "No codeA parameter provided in request", OperationOutcome.IssueType.EXCEPTION, 400);
        } else if (codeB == null) {
          throw FhirUtilityR4.exception(
              "No codeB parameter provided in request", OperationOutcome.IssueType.EXCEPTION, 400);
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), osQueryService, true);
        final Optional<Concept> checkA =
            osQueryService.getConcept(code1, term, new IncludeParam("minimal"));
        final Optional<Concept> checkB =
            osQueryService.getConcept(code2, term, new IncludeParam("minimal"));
        if (checkA.get() != null && checkB.get() != null) {
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          if (osQueryService.getPathsToParent(code1, code2, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumes");
          } else if (osQueryService.getPathsToParent(code2, code1, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumed-by");
          } else {
            params.addParameter("outcome", "no-subsumption-relationship");
          }
        }
      } else {
        throw FhirUtilityR4.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception(
          "Failed to check if A subsumes B", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Subsumes instance.
   *
   * <pre>
   * https://hl7.org/fhir/R4/codesystem-operation-subsumes.html
   * </pre>
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param codeA the code A to be tested. If not provided, a system must be provided
   * @param codeB the code B to be tested. If not provided, a system must be provided
   * @param system the code system in with the subsumption testing is to be performed. Must be
   *     provided unless the operation is invoked on a code system instance.
   * @param version the version of the code system.
   * @param codingA the coding A to be tested
   * @param codingB the coding B to be tested
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_SUBSUMES, idempotent = true)
  public Parameters subsumesInstance(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @IdParam final IdType id,
      @OperationParam(name = "codeA") final CodeType codeA,
      @OperationParam(name = "codeB") final CodeType codeB,
      @OperationParam(name = "system") final UriType system,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "codingA") final Coding codingA,
      @OperationParam(name = "codingB") final Coding codingB)
      throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.mutuallyRequired("codeA", codeA, "system", system);
      FhirUtilityR4.mutuallyRequired("codeB", codeB, "system", system);
      FhirUtilityR4.mutuallyExclusive("codingB", codingB, "codeB", codeB);
      FhirUtilityR4.mutuallyExclusive("codeA", codingA, "codeA", codeA);

      UriType systemToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (codingA != null) {
        systemToLookup = codingA.getSystemElement();
      }

      final List<CodeSystem> cs = findPossibleCodeSystems(id, null, systemToLookup, version);
      final Parameters params = new Parameters();
      if (cs.size() > 0) {
        String code1 = "";
        String code2 = "";
        if (codeA != null && codeB != null) {
          code1 = codeA.getCode();
          code2 = codeB.getCode();
        } else if (codingA != null && codingB != null) {
          code1 = codingA.getCode();
          code2 = codingB.getCode();
        } else if (codeA == null) {
          throw FhirUtilityR4.exception(
              "No codeA parameter provided in request", OperationOutcome.IssueType.EXCEPTION, 400);
        } else if (codeB == null) {
          throw FhirUtilityR4.exception(
              "No codeB parameter provided in request", OperationOutcome.IssueType.EXCEPTION, 400);
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), osQueryService, true);
        final Optional<Concept> checkA =
            osQueryService.getConcept(code1, term, new IncludeParam("minimal"));
        final Optional<Concept> checkB =
            osQueryService.getConcept(code2, term, new IncludeParam("minimal"));
        if (checkA.get() != null && checkB.get() != null) {
          params.addParameter("system", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          if (osQueryService.getPathsToParent(code1, code2, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumes");
          } else if (osQueryService.getPathsToParent(code2, code1, term).getPathCount() > 0) {
            params.addParameter("outcome", "subsumed-by");
          } else {
            params.addParameter("outcome", "no-subsumption-relationship");
          }
        }
      } else {
        throw FhirUtilityR4.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception(
          "Failed to validate code", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Find code systems.
   *
   * @param request the request
   * @param id the id
   * @param date the date
   * @param url the url
   * @param system the system
   * @param version the version
   * @param title the title
   * @param count the count
   * @param offset the offset
   * @return the list
   * @throws Exception the exception
   */
  @Search
  public Bundle findCodeSystems(
      final HttpServletRequest request,
      @OptionalParam(name = "_id") final TokenParam id,
      @OptionalParam(name = "date") final DateRangeParam date,
      @OptionalParam(name = "url") final UriParam url,
      @OptionalParam(name = "system") final UriParam system,
      @OptionalParam(name = "version") final StringParam version,
      @OptionalParam(name = "title") final StringParam title,
      @Description(shortDefinition = "Number of entries to return") @OptionalParam(name = "_count")
          final NumberParam count,
      @Description(shortDefinition = "Start offset, used when reading a next page")
          @OptionalParam(name = "_offset")
          final NumberParam offset)
      throws Exception {
    try {
      FhirUtilityR4.notSupportedSearchParams(request);
      FhirUtilityR4.mutuallyExclusive("url", url, "system", system);

      final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);

      final List<CodeSystem> list = new ArrayList<>();
      for (final Terminology terminology : terms) {
        final CodeSystem cs = FhirUtilityR4.toR4(terminology);
        // Skip non-matching
        if ((id != null && !id.getValue().equals(cs.getId()))
            || (system != null && !system.getValue().equals(cs.getUrl()))) {
          logger.debug("  SKIP system mismatch = " + cs.getUrl());
          continue;
        }
        if (url != null && !url.getValue().equals(cs.getUrl())) {
          logger.debug("  SKIP url mismatch = " + cs.getUrl());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cs.getDate())) {
          logger.debug("  SKIP date mismatch = " + cs.getDate());
          continue;
        }
        if (title != null && !FhirUtility.compareString(title, cs.getTitle())) {
          logger.debug("  SKIP title mismatch = " + cs.getTitle());
          continue;
        }
        if (version != null && !FhirUtility.compareString(version, cs.getVersion())) {
          logger.debug("  SKIP version mismatch = " + cs.getVersion());
          continue;
        }

        list.add(cs);
      }
      return FhirUtilityR4.makeBundle(request, list, count, offset);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR4.exception(
          "Failed to find code systems", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Helper method to find possible code systems.
   *
   * @param id the id
   * @param date the date
   * @param url the url
   * @param version the version
   * @return the list
   * @throws Exception the exception
   */
  private List<CodeSystem> findPossibleCodeSystems(
      @OptionalParam(name = "_id") final IdType id,
      @OptionalParam(name = "date") final DateRangeParam date,
      @OptionalParam(name = "url") final UriType url,
      @OptionalParam(name = "version") final StringType version)
      throws Exception {
    try {
      // If no ID and no url are specified, no code systems match
      if (id == null && url == null) {
        return new ArrayList<>(0);
      }

      final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);

      final List<CodeSystem> list = new ArrayList<>();
      for (final Terminology terminology : terms) {
        final CodeSystem cs = FhirUtilityR4.toR4(terminology);
        // Skip non-matching
        if ((id != null && !id.getIdPart().equals(cs.getId()))
            || (url != null && !url.getValue().equals(cs.getUrl()))) {
          logger.debug("  SKIP url mismatch = " + cs.getUrl());
          continue;
        }
        if (date != null && !FhirUtility.compareDateRange(date, cs.getDate())) {
          logger.debug("  SKIP date mismatch = " + cs.getDate());
          continue;
        }
        if (version != null && !version.getValue().equals(cs.getVersion())) {
          logger.debug("  SKIP version mismatch = " + cs.getVersion());
          continue;
        }

        list.add(cs);
      }
      return list;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR4.exception(
          "Failed to find code systems", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Returns the concept map for the specified details.
   *
   * @param details the details
   * @param id the id
   * @return the concept map
   * @throws Exception the exception
   */
  @Read
  public CodeSystem getCodeSystem(final ServletRequestDetails details, @IdParam final IdType id)
      throws Exception {
    try {
      final List<CodeSystem> candidates = findPossibleCodeSystems(id, null, null, null);
      for (final CodeSystem set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }
      throw FhirUtilityR4.exception(
          "Code system not found = " + (id == null ? "null" : id.getIdPart()),
          IssueType.NOTFOUND,
          404);

    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR4.exception(
          "Failed to get code system", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /* see superclass */
  @Override
  public Class<CodeSystem> getResourceType() {
    return CodeSystem.class;
  }
}
