package gov.nih.nci.evs.api.fhir.R5;

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
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.util.FHIRServerResponseException;
import gov.nih.nci.evs.api.util.FhirUtility;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** FHIR R5 CodeSystem provider. */
@Component
public class CodeSystemProviderR5 implements IResourceProvider {
  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(CodeSystemProviderR5.class);

  /** the query service. */
  @Autowired OpensearchQueryService osQueryService;

  /** The term utils. */
  /* The terminology utils */
  @Autowired TerminologyUtils termUtils;

  /**
   * Returns the type of resource for this provider.
   *
   * @return the resource
   */
  @Override
  public Class<CodeSystem> getResourceType() {
    return CodeSystem.class;
  }

  /**
   * Find code systems.
   *
   * <p>See https://hl7.org/fhir/R5/codesystem.html (find "search parameters")
   *
   * @param request the request
   * @param details the details
   * @param id the id
   * @param code the code
   * @param date the date
   * @param description the description
   * @param name the name
   * @param publisher the publisher
   * @param title the title
   * @param url the url (also known as system)
   * @param system the system
   * @param version the version
   * @param count the count
   * @param offset the offset
   * @return the bundle
   * @throws Exception exception
   */
  @Search
  public Bundle findCodeSystems(
      final HttpServletRequest request,
      final ServletRequestDetails details,
      @OptionalParam(name = "_id") final TokenParam id,
      @OptionalParam(name = "code") final TokenParam code,
      @OptionalParam(name = "date") final DateRangeParam date,
      @OptionalParam(name = "description") final StringParam description,
      @OptionalParam(name = "name") final StringParam name,
      @OptionalParam(name = "publisher") final StringParam publisher,
      @OptionalParam(name = "title") final StringParam title,
      @OptionalParam(name = "url") final UriParam url,
      @OptionalParam(name = "system") final UriParam system,
      @OptionalParam(name = "version") final StringParam version,
      @Description(shortDefinition = "Number of entries to return") @OptionalParam(name = "_count")
          final NumberParam count,
      @Description(shortDefinition = "Start offset, used when reading a next page")
          @OptionalParam(name = "_offset")
          final NumberParam offset)
      throws Exception {
    try {
      FhirUtilityR5.notSupportedSearchParams(request);
      FhirUtilityR5.mutuallyExclusive("url", url, "system", system);

      // Get the indexed terms
      final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);
      final List<CodeSystem> list = new ArrayList<>();

      // Find the matching code systems in the list of terms
      for (final Terminology terminology : terms) {
        final CodeSystem cs = FhirUtilityR5.toR5(terminology);

        // Skip non-matching
        if ((id != null && !id.getValue().equals(cs.getIdPart()))
            || (url != null && !url.getValue().equals(cs.getUrl()))) {
          logger.debug("  SKIP url mismatch = " + cs.getUrl());
          continue;
        }

        if (system != null && !system.getValue().equals(cs.getUrl())) {
          logger.debug("  SKIP system mismatch = " + cs.getUrl());
          continue;
        }

        if (date != null && !FhirUtility.compareDateRange(date, cs.getDate())) {
          logger.debug("  SKIP date mismatch = " + cs.getDate());
          continue;
        }
        if (description != null && !FhirUtility.compareString(description, cs.getDescription())) {
          logger.debug("  SKIP description mismatch = " + cs.getDescription());
          continue;
        }
        if (name != null && !FhirUtility.compareString(name, cs.getName())) {
          logger.debug("  SKIP name mismatch = " + cs.getName());
          continue;
        }
        if (publisher != null && !FhirUtility.compareString(publisher, cs.getPublisher())) {
          logger.debug("  SKIP publisher mismatch = " + cs.getPublisher());
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
      return FhirUtilityR5.makeBundle(request, list, count, offset);
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected error", e);
      throw FhirUtilityR5.exception("Failed to find code systesm", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Look up implicit
   *
   * <p>See https://hl7.org/fhir/R5/codesystem-operation-lookup.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param code the code to lookup
   * @param system the system for the code being looked up
   * @param version the version of the system, if provided.
   * @param coding the coding to look up
   * @param date the date that the information should be returned.
   * @return the parameters
   * @throws Exception exception
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
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      // @OperationParam(name = "property") final CodeType property
      ) throws Exception {
    // Check if the request is a POST, throw exception as we don't support post calls
    // exception for coding parameter
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_LOOKUP,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyRequired("code", code, "system", system);
      FhirUtilityR5.mutuallyExclusive("code", code, "coding", coding);
      // FhirUtilityR5.notSupported(displayLanguage, "displayLanguage");
      // FhirUtilityR5.notSupported(property, "property");
      for (final String param : new String[] {"displayLanguage", "property"}) {
        FhirUtilityR5.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR5.notSupported(request, "_has");
      }

      UriType systemToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (coding != null) {
        systemToLookup = coding.getSystemElement();
      }

      final List<CodeSystem> cs = findPossibleCodeSystems(null, date, systemToLookup, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), osQueryService, true);
        final Concept concept =
            osQueryService.getConcept(codeToLookup, term, new IncludeParam("children")).get();
        // required in the specification
        params.addParameter("name", codeSys.getName());
        params.addParameter("display", concept.getName());
        // optional in the specification
        params.addParameter("version", codeSys.getVersion());
        // properties
        params.addParameter(FhirUtilityR5.createProperty(concept.getActive(), "active", false));
        for (final Concept parent : concept.getParents()) {
          params.addParameter(FhirUtilityR5.createProperty("parent", parent.getCode(), true));
        }
        for (final Concept child : concept.getChildren()) {
          params.addParameter(FhirUtilityR5.createProperty("child", child.getCode(), true));
        }
      } else {
        throw FhirUtilityR5.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to lookup code", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Lookup instance.
   *
   * <p>See https://hl7.org/fhir/R5/codesystem-operation-lookup.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param code the code to lookup
   * @param system the system for the code being looked up
   * @param version the version of the system, if provided.
   * @param coding the coding to look up
   * @param date the date that the information should be returned.
   * @return the parameters
   * @throws Exception exception
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
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage,
      // @OperationParam(name = "property") final CodeType property
      ) throws Exception {
    // Check if the request is a POST, throw exception as we don't support post calls
    // exception for coding parameter
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_LOOKUP,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyExclusive("code", code, "coding", coding);
      for (final String param : new String[] {"displayLanguage", "property"}) {
        FhirUtilityR5.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR5.notSupported(request, "_has");
      }

      UriType systemToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (coding != null) {
        systemToLookup = coding.getSystemElement();
      }

      final List<CodeSystem> cs = findPossibleCodeSystems(id, date, null, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        String codeToLookup = "";
        if (code != null) {
          codeToLookup = code.getCode();
        } else if (coding != null) {
          codeToLookup = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        if ((systemToLookup != null) && !codeSys.getUrl().equals(systemToLookup.getValue())) {
          throw FhirUtilityR5.exception(
              "Supplied url or system "
                  + systemToLookup
                  + " doesn't match the CodeSystem retrieved by the id "
                  + id
                  + " "
                  + codeSys.getUrl(),
              OperationOutcome.IssueType.EXCEPTION,
              400);
        }
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), osQueryService, true);
        final Concept concept =
            osQueryService.getConcept(codeToLookup, term, new IncludeParam("children")).get();
        // required in the specification
        params.addParameter("name", codeSys.getName());
        params.addParameter("display", concept.getName());
        // optional in the specification
        params.addParameter("version", codeSys.getVersion());
        params.addParameter(FhirUtilityR5.createProperty(concept.getActive(), "active", false));
        for (final Concept parent : concept.getParents()) {
          params.addParameter(FhirUtilityR5.createProperty(parent.getCode(), "parent", true));
        }
        for (final Concept parent : concept.getParents()) {
          params.addParameter(FhirUtilityR5.createProperty(parent.getCode(), "parent", true));
        }
        for (final Concept child : concept.getChildren()) {
          params.addParameter(FhirUtilityR5.createProperty(child.getCode(), "child", true));
        }
      } else {
        throw FhirUtilityR5.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to lookup code", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate code implicit
   *
   * <p>See https://hl7.org/fhir/R5/codesystem-operation-validate-code.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param url the CodeSystem URL
   * @param code the code to be validated
   * @param version the version of the code system, if provided
   * @param display the display associated with the code. If provided, a code must be provided.
   * @param coding the coding to validate
   * @return the parameters
   * @throws Exception exception
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
      // @OperationParam(name = "date") final DateRangeParam date,
      // @OperationParam(name = "abstract") final BooleanType abstractt,
      // @OperationParam(name = "displayLanguage") final StringType displayLanguage
      ) throws Exception {
    // Check if the request is a POST, throw exception as we don't support post calls
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyExclusive("code", code, "coding", coding);
      FhirUtilityR5.mutuallyRequired("display", display, "code", code);
      FhirUtilityR5.mutuallyRequired("code", code, "url", url);
      for (final String param :
          new String[] {"codeSystem", "date", "abstract", "displayLanguage"}) {
        FhirUtilityR5.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR5.notSupported(request, "_has");
      }

      UriType systemToLookup = null;
      if (url != null) {
        systemToLookup = url;
      } else if (coding != null) {
        systemToLookup = coding.getSystemElement();
      }

      final List<CodeSystem> cs = findPossibleCodeSystems(null, null, systemToLookup, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
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
          final Concept concept =
              osQueryService.getConcept(codeToValidate, term, new IncludeParam("children")).get();
          params.addParameter("result", true);
          if (display == null || concept.getName().equals(display.getValue())) {
            params.addParameter("code", concept.getCode());
          } else {
            params.addParameter(
                "message",
                "The code"
                    + concept.getCode()
                    + " exists in this value set but the "
                    + "display is not valid");
          }
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          params.addParameter("display", concept.getName());
          params.addParameter("active", true);
        } else {
          params.addParameter("result", false);
          params.addParameter(
              "message", "The code does not exist for the supplied code system url and/or version");
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
        }
      } else {
        throw FhirUtilityR5.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to validate code", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Validate the code instance
   *
   * <p>See https://hl7.org/fhir/R5/codesystem-operation-validate-code.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param url the CodeSystem URL
   * @param code the code to be validated
   * @param version the version of the code system, if provided
   * @param display the display associated with the code. If provided, a code must be provided.
   * @param coding the coding to validate
   * @return the parameters
   * @throws Exception exception
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
    // Check if the request is a post, throw exception as we don't support post requests
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_VALIDATE_CODE,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyExclusive("code", code, "coding", coding);
      FhirUtilityR5.mutuallyRequired("display", display, "code", code);
      for (final String param :
          new String[] {"codeSystem", "date", "abstract", "displayLanguage"}) {
        FhirUtilityR5.notSupported(request, param);
      }
      if (Collections.list(request.getParameterNames()).stream()
              .filter(k -> k.startsWith("_has"))
              .count()
          > 0) {
        FhirUtilityR5.notSupported(request, "_has");
      }

      UriType systemToLookup = null;
      if (url != null) {
        systemToLookup = url;
      } else if (coding != null) {
        systemToLookup = coding.getSystemElement();
      }

      final List<CodeSystem> cs = findPossibleCodeSystems(id, null, null, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        String codeToValidate = "";
        if (code != null) {
          codeToValidate = code.getCode();
        } else if (coding != null) {
          codeToValidate = coding.getCode();
        }
        final CodeSystem codeSys = cs.get(0);
        if ((systemToLookup != null) && !codeSys.getUrl().equals(systemToLookup.getValue())) {
          throw FhirUtilityR5.exception(
              "Supplied url or system "
                  + systemToLookup
                  + " doesn't match the CodeSystem retrieved by the id "
                  + id
                  + " "
                  + codeSys.getUrl(),
              OperationOutcome.IssueType.EXCEPTION,
              400);
        }
        final Terminology term =
            termUtils.getIndexedTerminology(codeSys.getTitle(), osQueryService, true);
        final Optional<Concept> check =
            osQueryService.getConcept(codeToValidate, term, new IncludeParam("children"));
        if (check.isPresent()) {
          final Concept concept =
              osQueryService.getConcept(codeToValidate, term, new IncludeParam("children")).get();
          params.addParameter("result", true);
          if (display == null || concept.getName().equals(display.getValue())) {
            params.addParameter("code", concept.getCode());
          } else {
            params.addParameter(
                "message",
                "The code "
                    + concept.getCode()
                    + " exists in this value set but the "
                    + "display is not valid");
          }
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
          params.addParameter("display", concept.getName());
          params.addParameter("active", true);
        } else {
          params.addParameter("result", false);
          params.addParameter(
              "message", "The code does not exist for the supplied code system url and/or version");
          params.addParameter("url", codeSys.getUrl());
          params.addParameter("version", codeSys.getVersion());
        }
      } else {
        throw FhirUtilityR5.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to validate code", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Subsumes implicit
   *
   * <p>See https://hl7.org/fhir/R5/codesystem-operation-subsumes.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param codeA the code a to be tested. If provided, system must be provided.
   * @param codeB the code b to be tested. If provided, system must be provided.
   * @param system the code system the subsumption test is performed on.
   * @param version the version of the code system, if provided.
   * @param codingA the coding a to be tested.
   * @param codingB the coding b to be tested.
   * @return the parameters
   * @throws Exception exception
   */
  @Operation(name = JpaConstants.OPERATION_SUBSUMES, idempotent = true)
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
    // Check if the request is a post, throw exception as we don't support post requests
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyRequired("codeA", codeA, "system", system);
      FhirUtilityR5.mutuallyRequired("codeB", codeB, "system", system);
      FhirUtilityR5.mutuallyExclusive("codingB", codingB, "codeB", codeB);
      FhirUtilityR5.mutuallyExclusive("codingA", codingA, "codeA", codeA);

      UriType systemToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (codingA != null) {
        systemToLookup = codingA.getSystemElement();
      }
      if (codingA != null && codingB != null) {
        if (!codingA.getSystem().equals(codingB.getSystem())) {
          throw FhirUtilityR5.exception(
              "CodingA system and CodingB system are expected to match",
              OperationOutcome.IssueType.EXCEPTION,
              400);
        }
      }
      final List<CodeSystem> cs = findPossibleCodeSystems(null, null, systemToLookup, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        String code1 = "";
        String code2 = "";
        if (codeA != null && codeB != null) {
          code1 = codeA.getCode();
          code2 = codeB.getCode();
        } else if (codingA != null && codingB != null) {
          code1 = codingA.getCode();
          code2 = codingB.getCode();
        } else if (codeA == null) {
          throw FhirUtilityR5.exception(
              "No codeA parameter provided in request", OperationOutcome.IssueType.EXCEPTION, 400);
        } else if (codeB == null) {
          throw FhirUtilityR5.exception(
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
        throw FhirUtilityR5.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to check if A subsumes B", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Subsumes instance
   *
   * <p>See https://hl7.org/fhir/R5/codesystem-operation-subsumes.html
   *
   * @param request the request
   * @param response the response
   * @param details the details
   * @param id the id
   * @param codeA the code a to be tested. If provided, system must be provided.
   * @param codeB the code b to be tested. If provided, system must be provided.
   * @param system the code system the subsumption test is performed on.
   * @param version the version of the code system, if provided.
   * @param codingA the coding a to be tested.
   * @param codingB the coding b to be tested.
   * @return the parameters
   * @throws Exception exception
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
    // Check if the request is a post, throw exception as we don't support post requests
    if (request.getMethod().equals("POST")) {
      throw FhirUtilityR5.exception(
          "POST method not supported for " + JpaConstants.OPERATION_SUBSUMES,
          IssueType.NOTSUPPORTED,
          405);
    }
    try {
      FhirUtilityR5.mutuallyRequired("codeA", codeA, "system", system);
      FhirUtilityR5.mutuallyRequired("codeB", codeB, "system", system);
      FhirUtilityR5.mutuallyExclusive("codingB", codingB, "codeB", codeB);
      FhirUtilityR5.mutuallyExclusive("codingA", codingA, "codeA", codeA);

      UriType systemToLookup = null;
      if (system != null) {
        systemToLookup = system;
      } else if (codingA != null) {
        systemToLookup = codingA.getSystemElement();
      }
      if (codingA != null && codingB != null) {
        if (!codingA.getSystem().equals(codingB.getSystem())) {
          throw FhirUtilityR5.exception(
              "CodingA system and CodingB system are expected to match",
              OperationOutcome.IssueType.EXCEPTION,
              400);
        }
      }

      final List<CodeSystem> cs = findPossibleCodeSystems(null, null, systemToLookup, version);
      final Parameters params = new Parameters();
      if (!cs.isEmpty()) {
        String code1 = "";
        String code2 = "";
        if (codeA != null && codeB != null) {
          code1 = codeA.getCode();
          code2 = codeB.getCode();
        } else if (codingA != null && codingB != null) {
          code1 = codingA.getCode();
          code2 = codingB.getCode();
        } else if (codeA == null) {
          throw FhirUtilityR5.exception(
              "No codeA parameter provided in request", OperationOutcome.IssueType.EXCEPTION, 400);
        } else if (codeB == null) {
          throw FhirUtilityR5.exception(
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
        throw FhirUtilityR5.exception(
            "Unable to find matching code system", OperationOutcome.IssueType.NOTFOUND, 400);
      }
      return params;
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Error occurred: ", e);
      throw FhirUtilityR5.exception("Failed to check if A subsumes B", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Returns the concept map for the specified details.
   *
   * <p>See https://hl7.org/fhir/R5/codesystem.html
   *
   * @param id the id
   * @return the code system concept map
   * @throws Exception exception
   */
  @Read
  public CodeSystem getCodeSystem(@IdParam final IdType id) throws Exception {
    try {
      if (id.hasVersionIdPart()) {
        // If someone somehow passes a versioned ID to read, delegate to vread
        return vread(id);
      }
      final List<CodeSystem> candidates = findPossibleCodeSystems(id, null, null, null);
      for (final CodeSystem set : candidates) {
        if (id.getIdPart().equals(set.getId())) {
          return set;
        }
      }
      throw FhirUtilityR5.exception(
          "Code system not found = " + (id.getIdPart()), IssueType.NOTFOUND, 404);
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR5.exception("Failed to get code system", IssueType.EXCEPTION, 500);
    }
  }

  /**
   * Helper method to find possible code systems.
   *
   * @param id the id type
   * @param date the date range
   * @param url the uri type
   * @param version the string type version
   * @return the list of code systems
   * @throws Exception exception
   */
  private List<CodeSystem> findPossibleCodeSystems(
      @OperationParam(name = "_id") final IdType id,
      @OperationParam(name = "date") final DateRangeParam date,
      @OperationParam(name = "url") final UriType url,
      @OperationParam(name = "version") final StringType version)
      throws Exception {
    try {
      if (id == null && url == null) {
        return new ArrayList<>(0);
      }

      final List<Terminology> terms = termUtils.getIndexedTerminologies(osQueryService);
      final List<CodeSystem> list = new ArrayList<>();
      // Find the matching code systems
      for (final Terminology terminology : terms) {
        final CodeSystem cs = FhirUtilityR5.toR5(terminology);
        // Skip non-matching
        if ((id != null && !id.getIdPart().equals(cs.getIdPart()))
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
      throw FhirUtilityR5.exception("Failed to find code systems", IssueType.EXCEPTION, 500);
    }
  }

  @History(type = CodeSystem.class)
  public List<CodeSystem> getCodeSystemHistory(@IdParam IdType id) {
    List<CodeSystem> history = new ArrayList<>();
    try {
      final List<CodeSystem> candidates = findPossibleCodeSystems(id, null, null, null);
      for (final CodeSystem cs : candidates) {
        if (id.getIdPart().equals(cs.getId())) {
          history.add(cs);
        }
      }
      if (history.isEmpty()) {
        throw FhirUtilityR5.exception(
            "Code system not found = " + (id == null ? "null" : id.getIdPart()),
            IssueType.NOTFOUND,
            404);
      }
    } catch (final FHIRServerResponseException e) {
      throw e;
    } catch (final Exception e) {
      logger.error("Unexpected exception", e);
      throw FhirUtilityR5.exception("Failed to get code system", IssueType.EXCEPTION, 500);
    }

    // Make sure each CodeSystem has proper metadata for history
    for (CodeSystem cs : history) {
      if (cs.getMeta() == null) {
        cs.setMeta(new Meta());
      }
      if (cs.getMeta().getVersionId() == null) {
        // Set appropriate version
        cs.getMeta().setVersionId("1");
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
   */
  @Read(version = true)
  public CodeSystem vread(@IdParam IdType versionedId) {
    // "canmed_202311"
    String resourceId = versionedId.getIdPart();
    // "1"
    String versionId = versionedId.getVersionIdPart();

    logger.info("Looking for resource: {} version: {}", resourceId, versionId);

    try {
      // If no version is specified in a vread call, this shouldn't happen
      // but if it does, delegate to regular read
      if (!versionedId.hasVersionIdPart()) {
        logger.warn("VRead called without version ID, delegating to regular read");
        return getCodeSystem(new org.hl7.fhir.r5.model.IdType(versionedId.getIdPart()));
      }
      final List<CodeSystem> candidates = findPossibleCodeSystems(versionedId, null, null, null);
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
            // Optional: set timestamp
            cs.getMeta().setLastUpdated(new Date());

            logger.info("Found matching version!");
            return cs;
          }
        }
      }

      throw FhirUtilityR5.exception(
          "Code system version not found: " + resourceId + " version " + versionId,
          IssueType.NOTFOUND,
          404);
    } catch (final FHIRServerResponseException e) {
      throw e; // Re-throw FHIR exceptions as-is
    } catch (final Exception e) {
      logger.error("Unexpected exception in vread", e);
      throw FhirUtilityR5.exception("Failed to get code system version", IssueType.EXCEPTION, 500);
    }
  }
}
