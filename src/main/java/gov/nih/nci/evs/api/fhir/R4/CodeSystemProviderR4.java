package gov.nih.nci.evs.api.fhir.R4;

import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.History;
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
import gov.nih.nci.evs.api.fhir.R5.FhirUtilityR5;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
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
   * <p>See https://hl7.org/fhir/R4/codesystem-operation-lookup.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param code the code that is to be located. If provided, a system must be provided.
   * @param system the system for the code that is be located.
   * @param version the version of the system
   * @param coding the coding to look up
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
      @OperationParam(name = "coding") final Coding coding
      // @OperationParam(name = "date") final DateRangeParam date
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      // @OperationParam(name = "property") final CodeType property
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
      // FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      // FhirUtilityR4.notSupported("property", property);
      for (final String param : new String[] {"displayLanguage", "property", "date"}) {
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

      final List<CodeSystem> cs = findPossibleCodeSystems(null, systemToLookup, version);
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
   * <p>See https://hl7.org/fhir/R4/codesystem-operation-lookup.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param code the code that is to be located. If provided, a system must be provided.
   * @param system the system for the code that is be located.
   * @param version the version of the system
   * @param coding the coding to look up
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
      @OperationParam(name = "coding") final Coding coding
      // @OperationParam(name = "date") final DateRangeParam date
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      // @OperationParam(name = "property") final CodeType property
      ) throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_LOOKUP,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR4.mutuallyExclusive("code", code, "coding", coding);
      // FhirUtilityR4.notSupported("displayLanguage", displayLanguage);
      // FhirUtilityR4.notSupported("property", property);
      for (final String param : new String[] {"displayLanguage", "property", "date"}) {
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

      final List<CodeSystem> cs = findPossibleCodeSystems(id, null, version);
      final Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        // if system is supplied, ensure it matches the url returned on the codeSys found by id
        if ((systemToLookup != null) && !codeSys.getUrl().equals(systemToLookup.getValue())) {
          throw FhirUtilityR5.exception(
              "Supplied url or system "
                  + systemToLookup
                  + " doesn't match the CodeSystem retrieved by the id "
                  + id
                  + " "
                  + codeSys.getUrl(),
              org.hl7.fhir.r5.model.OperationOutcome.IssueType.EXCEPTION,
              400);
        }
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
   * <p>See https://hl7.org/fhir/R4/codesystem-operation-validate-code.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param url the CodeSystem URL.
   * @param code the code that is to be validated.
   * @param version the version of the code system.
   * @param display the display associated with the code If provided, a code must be provided.
   * @param coding the coding to validate.
   * @return the parameters
   * @throws Exception the exception
   */
  @Operation(name = JpaConstants.OPERATION_VALIDATE_CODE, idempotent = true)
  public Parameters validateCodeImplicit(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final ServletRequestDetails details,
      @OperationParam(name = "url") final UriType url,
      // @OperationParam(name = "codeSystem") final CodeSystem codeSystem,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding
      // @OperationParam(name = "date") final DateTimeType date,
      // @OperationParam(name = "abstract") final BooleanType abstractt,
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage
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
      FhirUtilityR4.mutuallyRequired("code", code, "url", url);
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

      final List<CodeSystem> cs = findPossibleCodeSystems(null, systemToLookup, version);
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
   * <p>See https://hl7.org/fhir/R4/codesystem-operation-validate-code.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param url the CodeSystem URL.
   * @param code the code that is to be validated.
   * @param version the version of the code system.
   * @param display the display associated with the code If provided, a code must be provided.
   * @param coding the coding to validate.
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
      // @OperationParam(name = "codeSystem") final CodeSystem codeSystem,
      @OperationParam(name = "code") final CodeType code,
      @OperationParam(name = "version") final StringType version,
      @OperationParam(name = "display") final StringType display,
      @OperationParam(name = "coding") final Coding coding
      // @OperationParam(name = "date") final DateTimeType date,
      // @OperationParam(name = "abstract") final BooleanType abstractt,
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage
      ) throws Exception {
    // check if request is a post, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR4.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }

    // This is an instance call so the url is unnecessary
    // FhirUtilityR4.mutuallyRequired("code", code, "url", url);
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

      final List<CodeSystem> cs = findPossibleCodeSystems(id, null, version);
      final Parameters params = new Parameters();
      if (cs.size() > 0) {
        String codeToValidate = "";
        if (code != null) {
          codeToValidate = code.getCode();
        } else if (coding != null) {
          codeToValidate = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        // if url is supplied, ensure it matches the url returned on the codeSys found by id
        if ((systemToLookup != null) && !codeSys.getUrl().equals(systemToLookup.getValue())) {
          throw FhirUtilityR5.exception(
              "Supplied url or system "
                  + systemToLookup
                  + " doesn't match the CodeSystem retrieved by the id "
                  + id
                  + " "
                  + codeSys.getUrl(),
              org.hl7.fhir.r5.model.OperationOutcome.IssueType.EXCEPTION,
              400);
        }
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
   * <p>See https://hl7.org/fhir/R4/codesystem-operation-subsumes.html
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

      final List<CodeSystem> cs = findPossibleCodeSystems(null, systemToLookup, version);
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
   * <p>See https://hl7.org/fhir/R5/codesystem-operation-subsumes.html
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

      final List<CodeSystem> cs = findPossibleCodeSystems(id, systemToLookup, version);
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
   * <p>See https://hl7.org/fhir/R4/codesystem.html (find "search parameters")
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
          final NumberParam offset,
      @Description(shortDefinition = "Sort by field (name, title, publisher, date, url)")
          @OptionalParam(name = "_sort")
          final StringParam sort)
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

      // Apply sorting if requested
      applySorting(list, sort);

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
   * @param url the url
   * @param version the version
   * @return the list
   * @throws Exception the exception
   */
  private List<CodeSystem> findPossibleCodeSystems(
      @OptionalParam(name = "_id") final IdType id,
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
   * <p>See https://hl7.org/fhir/R4/codesystem.html
   *
   * @param id the id
   * @return the concept map
   * @throws Exception the exception
   */
  @Read
  public CodeSystem getCodeSystem(@IdParam final IdType id) throws Exception {
    logger.info("=== REGULAR READ called ===");
    logger.info("ID part: {}", id.getIdPart());
    logger.info("Version part: {}", id.getVersionIdPart());
    logger.info("Has version ID: {}", id.hasVersionIdPart());
    logger.info("Full ID: {}", id.getValue());

    try {
      if (id.hasVersionIdPart()) {
        // If someone somehow passes a versioned ID to read, delegate to vread
        return vread(id);
      }
      final List<CodeSystem> candidates = findPossibleCodeSystems(id, null, null);
      for (final CodeSystem set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }
      throw FhirUtilityR4.exception(
          "Code system not found = " + id.getIdPart(), IssueType.NOTFOUND, 404);

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

  /**
   * Gets the code system history.
   *
   * @param id the id
   * @return the code system history
   * @throws Exception the exception
   */
  @History(type = org.hl7.fhir.r4.model.CodeSystem.class)
  public List<org.hl7.fhir.r4.model.CodeSystem> getCodeSystemHistory(
      @IdParam org.hl7.fhir.r4.model.IdType id) throws Exception {
    List<org.hl7.fhir.r4.model.CodeSystem> history = new ArrayList<>();
    try {
      final List<org.hl7.fhir.r4.model.CodeSystem> candidates =
          findPossibleCodeSystems(id, null, null);
      for (final org.hl7.fhir.r4.model.CodeSystem cs : candidates) {
        if (id.getIdPart().equals(cs.getId())) {
          history.add(cs);
        }
      }
      if (history.isEmpty()) {
        throw FhirUtilityR4.exception(
            "Code system not found = " + (id == null ? "null" : id.getIdPart()),
            org.hl7.fhir.r4.model.OperationOutcome.IssueType.NOTFOUND,
            404);
      }
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR4.exception(
          "Failed to get code system",
          org.hl7.fhir.r4.model.OperationOutcome.IssueType.EXCEPTION,
          500);
    }

    // Make sure each CodeSystem has proper metadata for history
    for (org.hl7.fhir.r4.model.CodeSystem cs : history) {
      if (cs.getMeta() == null) {
        cs.setMeta(new Meta());
      }
      if (cs.getMeta().getVersionId() == null) {
        cs.getMeta().setVersionId("1"); // Set appropriate version
      }
      if (cs.getMeta().getLastUpdated() == null) {
        cs.getMeta().setLastUpdated(new Date());
      }
    }

    return history;
  }

  /**
   * Vread.
   *
   * @param versionedId the versioned id
   * @return the code system
   * @throws Exception the exception
   */
  @Read(version = true)
  public CodeSystem vread(@IdParam IdType versionedId) throws Exception {
    logger.info("=== VREAD called ===");
    logger.info("ID part: {}", versionedId.getIdPart());
    logger.info("Version part: {}", versionedId.getVersionIdPart());
    logger.info("Has version ID: {}", versionedId.hasVersionIdPart());
    logger.info("Full ID: {}", versionedId.getValue());

    String resourceId = versionedId.getIdPart(); // "canmed_202311"
    String versionId = versionedId.getVersionIdPart(); // "1"

    logger.info("Looking for resource: {} version: {}", resourceId, versionId);

    try {
      // If no version is specified in a vread call, this shouldn't happen
      // but if it does, delegate to regular read
      if (!versionedId.hasVersionIdPart()) {
        logger.warn("VRead called without version ID, delegating to regular read");
        return getCodeSystem(new IdType(versionedId.getIdPart()));
      }
      final List<CodeSystem> candidates = findPossibleCodeSystems(versionedId, null, null);
      logger.info("Found {} candidates", candidates.size());

      for (final CodeSystem cs : candidates) {
        String csId = cs.getId();
        String csVersionId = cs.getMeta() != null ? cs.getMeta().getVersionId() : null;

        logger.info("Checking candidate: id={}, versionId={}", csId, csVersionId);

        if (resourceId.equals(csId)) {
          // If the CodeSystem doesn't have a version ID, treat it as version "1"
          String effectiveVersionId = (csVersionId != null) ? csVersionId : "1";

          if (versionId.equals(effectiveVersionId)) {
            // Make sure the returned CodeSystem has the version ID set
            if (cs.getMeta() == null) {
              cs.setMeta(new Meta());
            }
            cs.getMeta().setVersionId("1");
            cs.getMeta().setLastUpdated(new Date()); // Optional: set timestamp

            logger.info("Found matching version!");
            return cs;
          }
        }
      }

      throw FhirUtilityR4.exception(
          "Code system version not found: " + resourceId + " version " + versionId,
          OperationOutcome.IssueType.NOTFOUND,
          404);
    } catch (final FHIRServerResponseException e) {
      throw e; // Re-throw FHIR exceptions as-is
    } catch (final Exception e) {
      logger.error("Unexpected exception in vread", e);
      throw FhirUtilityR4.exception(
          "Failed to get code system version", OperationOutcome.IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Apply sorting to the list of CodeSystems if requested.
   *
   * @param list the list to sort
   * @param sort the sort parameter
   */
  private void applySorting(final List<CodeSystem> list, final StringParam sort) {
    if (sort == null || sort.getValue() == null || sort.getValue().trim().isEmpty()) {
      return;
    }

    try {
      final String sortValue = sort.getValue().trim();
      final boolean descending = sortValue.startsWith("-");
      final String field = descending ? sortValue.substring(1) : sortValue;

      // Validate supported fields
      final List<String> supportedFields =
          Arrays.asList("name", "title", "publisher", "date", "url");
      if (!supportedFields.contains(field)) {
        throw FhirUtilityR4.exception(
            "Unsupported sort field: "
                + field
                + ". Supported fields: "
                + String.join(", ", supportedFields),
            OperationOutcome.IssueType.INVALID,
            400);
      }

      final Comparator<CodeSystem> comparator = getCodeSystemComparator(field);
      if (descending) {
        Collections.sort(list, comparator.reversed());
      } else {
        Collections.sort(list, comparator);
      }
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      throw FhirUtilityR4.exception(
          "Error processing sort parameter: " + e.getMessage(),
          OperationOutcome.IssueType.INVALID,
          400);
    }
  }

  /**
   * Get comparator for CodeSystem sorting.
   *
   * @param field the field to sort by
   * @return the comparator
   */
  private Comparator<CodeSystem> getCodeSystemComparator(final String field) {
    switch (field) {
      case "name":
        return Comparator.comparing(
            cs -> cs.getName() != null ? cs.getName().toLowerCase() : "",
            Comparator.nullsLast(String::compareTo));
      case "title":
        return Comparator.comparing(
            cs -> cs.getTitle() != null ? cs.getTitle().toLowerCase() : "",
            Comparator.nullsLast(String::compareTo));
      case "publisher":
        return Comparator.comparing(
            cs -> cs.getPublisher() != null ? cs.getPublisher().toLowerCase() : "",
            Comparator.nullsLast(String::compareTo));
      case "date":
        return Comparator.comparing(CodeSystem::getDate, Comparator.nullsLast(Date::compareTo));
      case "url":
        return Comparator.comparing(
            cs -> cs.getUrl() != null ? cs.getUrl().toLowerCase() : "",
            Comparator.nullsLast(String::compareTo));
      default:
        throw new IllegalArgumentException("Unsupported sort field: " + field);
    }
  }
}
